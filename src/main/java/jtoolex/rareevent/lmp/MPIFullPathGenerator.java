package jtoolex.rareevent.lmp;

import jtool.atom.IAtomData;
import jtool.code.collection.NewCollections;
import jtool.code.iterator.IDoubleIterator;
import jtool.lmp.Box;
import jtool.lmp.Lmpdat;
import jtool.lmp.NativeLmp;
import jtool.math.matrix.ColumnMatrix;
import jtool.math.matrix.DoubleArrayMatrix;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import jtool.parallel.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static jtool.code.CS.*;

/**
 * 一种路径生成器，通过原生运行 {@link NativeLmp} 来直接生成路径
 * <p>
 * 每个进程都需要调用 new 来进行创建，除了特殊说明，输入参数都需要一致；
 * 而后续使用只需要主线程 （aWorldRoot）来进行调用获取信息
 * <p>
 * 主要用于可行性验证，效率虽然高于原本的实现，但是为了符合原本 api
 * 结构依旧存在许多通讯的损失
 * <p>
 * 特别注意方法的线程安全要求
 * @author liqa
 */
@ApiStatus.Experimental
public class MPIFullPathGenerator {
    
    /** 用于 MPI 收发信息的 tags */
    private final static int LMPDAT_INFO = 100, DATA_STD = 101, DATA_VELOCITIES = 102;
    /** 用于获取 Matrix 的形式从而决定如何排列数据 */
    private final static int ROW_ROW = 0, ROW_COL = 1, COL_ROW = 2, COL_COL = 3, ELSE = -1;
    private static int matrixFromOf(IMatrix aAtomData, @Nullable IMatrix aVelocities) {
        if (aVelocities == null) {
            if (aAtomData instanceof RowMatrix) return ROW_ROW;
            else if (aAtomData instanceof ColumnMatrix) return COL_COL;
            else return ELSE;
        } else {
            if (aAtomData instanceof RowMatrix) {
                if (aVelocities instanceof RowMatrix) return ROW_ROW;
                else if (aVelocities instanceof ColumnMatrix) return ROW_COL;
                return ELSE;
            } else
            if (aAtomData instanceof ColumnMatrix) {
                if (aVelocities instanceof RowMatrix) return COL_ROW;
                else if (aVelocities instanceof ColumnMatrix) return COL_COL;
                return ELSE;
            } else {
                return ELSE;
            }
        }
    }
    
    private final List<Lmpdat> mInitPoints;
    private final IVector mMesses;
    private final double mTemperature;
    private final String mPairStyle, mPairCoeff;
    private final double mTimestep;
    private final int mDumpStep, mPathLength;
    
    private final MPI.Comm mWorldComm;
    private final int mWorldRoot;
    private final MPI.Comm mLmpComm;
    private final Deque<Integer> mLmpRoots;
    
    /**
     * 创建一个生成器；
     * 每个进程都需要调用来进行创建，除了特殊说明，输入参数都需要一致
     * @author liqa
     * @param aWorldComm 此进程用于和主进程通讯的 {@link MPI.Comm}，默认为 {@link MPI.Comm#WORLD}
     * @param aWorldRoot 主进程编号，一般为 0
     * @param aLmpComm 此进程需要运行 {@link NativeLmp} 的 {@link MPI.Comm}，不同分组（color）的进程可以不同
     * @param aLmpRoots aLmpComm 对应的主进程编号列表，一般为 n * aLmpComm.size()，可以只有主进程 aWorldRoot 传入
     * @param aInitAtomDataList  用于初始的原子数据
     * @param aMesses 每个种类的原子对应的摩尔质量，且长度指定原子种类数目
     * @param aTemperature 创建路径的温度
     * @param aPairStyle lammps 输入文件使用的势场类型
     * @param aPairCoeff lammps 输入文件使用的势场参数
     * @param aTimestep 每步的实际时间步长，影响输入文件和统计使用的时间，默认为 0.002 (ps)
     * @param aDumpStep 每隔多少模拟步输出一个 dump，默认为 10
     * @param aPathLength 一次创建的路径的长度，默认为 20
     */
    public MPIFullPathGenerator(MPI.Comm aWorldComm, int aWorldRoot, MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {
        // 基本参数存储
        mInitPoints = NewCollections.map(aInitAtomDataList, data -> Lmpdat.fromAtomData(data).setNoVelocities()); // 初始点也需要移除速度，保证会从不同路径开始
        mMesses = aMesses.copy();
        mTemperature = aTemperature;
        mPairStyle = aPairStyle; mPairCoeff = aPairCoeff;
        mTimestep = aTimestep;
        mDumpStep = aDumpStep; mPathLength = aPathLength;
        // MPI 相关参数
        mWorldComm = aWorldComm;
        mWorldRoot = aWorldRoot;
        mLmpComm = aLmpComm;
        if (aLmpRoots == null) {
            if (mWorldComm.rank() == mWorldRoot) throw new IllegalArgumentException("aLmpRoots of WorldRoot ("+mWorldRoot+") can NOT be null");
            mLmpRoots = null;
        } else {
            // 直接使用 ConcurrentLinkedDeque 来简单处理并行访问的情况
            mLmpRoots = new ConcurrentLinkedDeque<>(aLmpRoots);
        }
    }
    
    
    /** [AtomNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, MatrixFrom, HasVelocities, aSeed] */
    private final static int LMP_INFO_LEN = 10, LMP_INFO_LEN_RECV = 7;
    /** 为了使用简单并且避免 double 转 long 造成的信息损耗，这里统一用 long[] 来传输信息 */
    private final static ThreadLocalObjectCachePool<long[]> LMP_INFO_CACHE = ThreadLocalObjectCachePool.withInitial(() -> new long[LMP_INFO_LEN]);
    
    
    /** IPathGenerator stuff */
    public IAtomData initPoint(long aSeed) {
        if (mWorldComm.rank() != mWorldRoot) throw new RuntimeException("initPoint can ONLY be called from WorldRoot ("+mWorldRoot+")");
        return mInitPoints.get(new LocalRandom(aSeed).nextInt(mInitPoints.size()));
    }
//    public List<? extends IAtomData> pathFrom(IAtomData aStart, long aSeed) {
//        if (mWorldComm.rank() != mWorldRoot) throw new RuntimeException("pathFrom can ONLY be called from WorldRoot ("+mWorldRoot+")");
//        // 无论如何先转为 Lmpdat，这里统一不设置 mass 因为后续发送接收时同样会设置一次
//        Lmpdat tStart = (aStart instanceof Lmpdat) ? (Lmpdat)aStart : Lmpdat.fromAtomData(aStart);
//        // 尝试获取对应 lmp 根节点发送数据
//        @Nullable Integer tLmpRoot = mLmpRoots.pollLast();
//        if (tLmpRoot == null) {
//            System.err.println("WARNING: Can NOT to get LmpRoot for this path gen temporarily, this path gen blocks until there are any free LmpRoot.");
//            System.err.println("It may be caused by too large number of parallels.");
//        }
//        while (tLmpRoot == null) {
//            try {Thread.sleep(FILE_SYSTEM_SLEEP_TIME);}
//            catch (InterruptedException e) {throw new RuntimeException(e);}
//            tLmpRoot = mLmpRoots.pollLast();
//        }
//        // 获取必要信息
//        final int tAtomNum = tStart.atomNum();
//        final IMatrix tAtomData = tStart.atomData();
//        final @Nullable IMatrix tVelocities = tStart.velocities();
//        final int tMatrixFrom = matrixFromOf(tAtomData, tVelocities);
//        final boolean tHasVelocities = tStart.hasVelocities();
//        // 先发送 Lmpdat 的必要信息，[AtomNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, MatrixFrom, HasVelocities, aSeed]
//        long[] rLmpdatInfo = LMP_INFO_CACHE.getObject();
//        rLmpdatInfo[0] = tAtomNum;
//        rLmpdatInfo[1] = Double.doubleToLongBits(tStart.lmpBox().xlo());
//        rLmpdatInfo[2] = Double.doubleToLongBits(tStart.lmpBox().xhi());
//        rLmpdatInfo[3] = Double.doubleToLongBits(tStart.lmpBox().ylo());
//        rLmpdatInfo[4] = Double.doubleToLongBits(tStart.lmpBox().yhi());
//        rLmpdatInfo[5] = Double.doubleToLongBits(tStart.lmpBox().zlo());
//        rLmpdatInfo[6] = Double.doubleToLongBits(tStart.lmpBox().zhi());
//        rLmpdatInfo[7] = tMatrixFrom;
//        rLmpdatInfo[8] = tHasVelocities ? 1 : 0;
//        rLmpdatInfo[9] = aSeed;
//        mWorldComm.send(rLmpdatInfo, LMP_INFO_LEN, tLmpRoot, LMPDAT_INFO);
//        // 发送后归还临时数据
//        LMP_INFO_CACHE.returnObject(rLmpdatInfo);
//        // 必要信息发送完成后分别发送 atomData 和 velocities
//        if (tMatrixFrom != ELSE) {
//            mWorldComm.send(((DoubleArrayMatrix)tAtomData).getData(), ((DoubleArrayMatrix)tAtomData).dataSize(), tLmpRoot, DATA_STD);
//        } else {
//            // 这里对于一般清空统一使用横向 Matrix
//            final int tCount = tAtomNum*STD_ATOM_DATA_KEYS.length;
//            double[] rAtomDataBuf = DoubleArrayCache.getArray(tCount);
//            IDoubleIterator it = tAtomData.iteratorRow();
//            for (int i = 0; i < tCount; ++i) rAtomDataBuf[i] = it.next();
//            mWorldComm.send(rAtomDataBuf, tCount, tLmpRoot, DATA_STD);
//            // 发送后归还临时数据
//            DoubleArrayCache.returnArray(rAtomDataBuf);
//        }
//        // 如果有速度信息则需要再发送一次速度信息
//        if (tHasVelocities) {
//            assert tVelocities != null;
//            if (tMatrixFrom != ELSE) {
//                mWorldComm.send(((DoubleArrayMatrix)tVelocities).getData(), ((DoubleArrayMatrix)tVelocities).dataSize(), tLmpRoot, DATA_VELOCITIES);
//            } else {
//                // 这里对于一般清空统一使用横向 Matrix
//                final int tCount = tAtomNum*ATOM_DATA_KEYS_VELOCITY.length;
//                double[] rVelocitiesBuf = DoubleArrayCache.getArray(tCount);
//                IDoubleIterator it = tVelocities.iteratorRow();
//                for (int i = 0; i < tCount; ++i) rVelocitiesBuf[i] = it.next();
//                mWorldComm.send(rVelocitiesBuf, tCount, tLmpRoot, DATA_VELOCITIES);
//                // 发送后归还临时数据
//                DoubleArrayCache.returnArray(rVelocitiesBuf);
//            }
//        }
//        // 接收计算完成后的数据
//        List<Lmpdat> rPath = new ArrayList<>(mPathLength+1); // 根据约定，第一个为传入的 aStart，为了减少信息传输这里不去获取第一帧随机分配的速度（如果有的话）
//        rPath.add(tStart);
//        for (int i = 0; i < mPathLength; ++i) {
//            // 同样先接收必要信息
//            long[] tLmpdatInfo = LMP_INFO_CACHE.getObject();
//            mWorldComm.recv(tLmpdatInfo, LMP_INFO_LEN_RECV, tLmpRoot, LMPDAT_INFO);
//            // 由于这个不会归还缓存，为了不扰乱缓存的使用，这里直接创建新的矩阵
//            // 这里约定好了接收时一定是 RowMatrix
//            RowMatrix tAtomDataRecv = RowMatrix.zeros((int)tLmpdatInfo[0], STD_ATOM_DATA_KEYS.length);
//            // 接收时一定有速度信息
//            RowMatrix tVelocitiesRecv = RowMatrix.zeros((int)tLmpdatInfo[0], ATOM_DATA_KEYS_VELOCITY.length);
//            // 先是基本信息，后是速度信息
//            mWorldComm.recv(tAtomDataRecv.getData(), tAtomDataRecv.dataSize(), tLmpRoot, DATA_STD);
//            mWorldComm.recv(tVelocitiesRecv.getData(), tVelocitiesRecv.dataSize(), tLmpRoot, DATA_VELOCITIES);
//            // 添加到 path
//            rPath.add(new Lmpdat(mMesses.size(), new Box(
//                Double.longBitsToDouble(tLmpdatInfo[1]), Double.longBitsToDouble(tLmpdatInfo[2]),
//                Double.longBitsToDouble(tLmpdatInfo[3]), Double.longBitsToDouble(tLmpdatInfo[4]),
//                Double.longBitsToDouble(tLmpdatInfo[5]), Double.longBitsToDouble(tLmpdatInfo[6])
//                ), mMesses.copy(), tAtomDataRecv, tVelocitiesRecv));
//            // 完事归还临时数据
//            LMP_INFO_CACHE.returnObject(tLmpdatInfo);
//        }
//        return rPath;
//    }
}
