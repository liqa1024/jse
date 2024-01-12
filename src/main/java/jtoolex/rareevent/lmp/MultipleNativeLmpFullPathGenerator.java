package jtoolex.rareevent.lmp;

import jtool.atom.IAtomData;
import jtool.code.UT;
import jtool.lmp.Box;
import jtool.lmp.Lmpdat;
import jtool.lmp.NativeLmp;
import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import jtool.math.vector.Vector;
import jtool.math.vector.Vectors;
import jtool.parallel.*;
import jtoolex.rareevent.IFullPathGenerator;
import jtoolex.rareevent.IParameterCalculator;
import jtoolex.rareevent.ITimeAndParameterIterator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static jtool.code.CS.*;

/**
 * 一种路径生成器，通过原生运行 {@link NativeLmp} 来直接生成完整的路径，
 * 内部包含多个 {@link NativeLmp} 并通过 MPI 来交流数据，
 * 需要使用 {@code MPI.initThread(args, MPI.Thread.MULTIPLE);}
 * 来初始化 MPI 从而保证对多线程的支持
 * <p>
 * 和 {@link NativeLmp} 类似，每个进程都需要调用 new 来进行创建，
 * 但是后续参数只需要 aWorldRoot 进程调用，内部会自动传输数据；
 * 除了特殊说明，输入参数都需要一致
 * <p>
 * 要求这些方法是线程安全的，可以同一个实例并行运行同一个方法，注意获取到的容器是线程不安全的（不同实例间线程安全）
 * <p>
 * 现在统一对于包含 {@link MPI.Comm} 不进行自动关闭的管理，和 {@link NativeLmp} 一致，
 * 这样可以简单处理很多情况
 * @author liqa
 */
@ApiStatus.Experimental
public class MultipleNativeLmpFullPathGenerator implements IFullPathGenerator<IAtomData> {
    
    private final IVector mMesses; // 主要用于收发数据时创建 Lmpdat 使用
    
    private final MPI.Comm mWorldComm;
    private final int mWorldRoot;
    private final int mWorldMe;
    private final MPI.Comm mLmpComm;
    private final Deque<Integer> mLmpRoots;
    private final int mLmpMe;
    
    private final NativeLmpFullPathGenerator mPathGen;
    
    private volatile boolean mDead = false;
    
    /**
     * 创建一个生成器；
     * 每个进程都需要调用来进行创建，除了特殊说明，输入参数都需要一致
     * @author liqa
     * @param aWorldComm 此进程用于和主进程通讯的 {@link MPI.Comm}，默认为 {@link MPI.Comm#WORLD}
     * @param aWorldRoot 主进程编号，默认为 0
     * @param aLmpComm 此进程需要运行 {@link NativeLmp} 的 {@link MPI.Comm}，不同分组（color）的进程可以不同
     * @param aLmpRoots aLmpComm 对应的主进程 0 在 aWorldComm 中的编号列表，一般为 n * aLmpComm.size()，可以只有主进程 aWorldRoot 传入
     * @param aParameterCalculator 计算对应 Lmpdat 参数的计算器，建议使用 MPI 版本的计算器，并使用和 aLmpComm 相同的 {@link MPI.Comm}
     * @param aInitAtomDataList  用于初始的原子数据
     * @param aMesses 每个种类的原子对应的摩尔质量，且长度指定原子种类数目
     * @param aTemperature 创建路径的温度
     * @param aPairStyle lammps 输入文件使用的势场类型
     * @param aPairCoeff lammps 输入文件使用的势场参数
     * @param aTimestep 每步的实际时间步长，影响输入文件和统计使用的时间，默认为 0.002 (ps)
     * @param aDumpStep 每隔多少模拟步输出一个 dump，默认为 10
     */
    private MultipleNativeLmpFullPathGenerator(MPI.Comm aWorldComm, int aWorldRoot, MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep) throws MPI.Error, NativeLmp.Error {
        // 基本参数存储
        mMesses = aMesses;
        // MPI 相关参数
        mWorldComm = aWorldComm;
        mWorldRoot = aWorldRoot;
        mWorldMe = mWorldComm.rank();
        mLmpComm = aLmpComm.copy(); // 对于数据传输使用的 Comm 要拷贝一份，和 lammps 使用的 Comm 进行区分避免相互干扰
        mLmpMe = mLmpComm.rank();
        if (aLmpRoots == null) {
            if (mWorldMe == mWorldRoot) throw new IllegalArgumentException("aLmpRoots of WorldRoot ("+mWorldRoot+") can NOT be null");
            mLmpRoots = null;
        } else {
            // 直接使用 ConcurrentLinkedDeque 来简单处理并行访问的情况
            mLmpRoots = new ConcurrentLinkedDeque<>(aLmpRoots);
        }
        
        mPathGen = new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep).setReturnLast();
    }
    
    /** 这里改为 static 方法来构造，从而避免一些问题，顺便实现自动资源释放 */
    private static void withOf_(MPI.Comm aWorldComm, int aWorldRoot, MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {
        try (MultipleNativeLmpFullPathGenerator tPathGen = new MultipleNativeLmpFullPathGenerator(aWorldComm, aWorldRoot, aLmpComm, aLmpRoots,  aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep)) {
            // 在另一个线程执行后续操作
            Future<Void> tLaterTask = null;
            if (tPathGen.mWorldMe == aWorldRoot) tLaterTask = UT.Par.runAsync(() -> aDoLater.accept(tPathGen));
            // 主线程开启服务器，现在所有进程都会阻塞，保证线程为主线程可以避免一些问题
            try (PathGenServer tServer = tPathGen.new PathGenServer()) {
                tServer.run();
            }
            // 最后需要等待一下 tLaterTask 完成
            if (tPathGen.mWorldMe == aWorldRoot) {
                assert tLaterTask != null;
                tLaterTask.get();
            }
        }
    }
    public static void withOf(MPI.Comm aWorldComm, int aWorldRoot, MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {
        withOf_(aWorldComm, aWorldRoot, aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, Vectors.from(aMesses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aDoLater);
    }
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, Vectors.from(aMesses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep,                Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf(aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff,                                  Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf(aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, Vectors.from(aMesses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep,                Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf(aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff,                                  Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf(aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, Vectors.from(aMesses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep,                Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf(aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10, aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff,                                  Consumer<MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf(aLmpComm, aLmpRoots, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002, aDoLater);}
    
    
    @Override public ITimeAndParameterIterator<Lmpdat> fullPathFrom(IAtomData aStart, long aSeed) {
        if (mWorldMe != mWorldRoot) throw new RuntimeException("fullPathFrom can ONLY be called from WorldRoot ("+mWorldRoot+")");
        if (mDead) throw new RuntimeException("This MultipleNativeLmpFullPathGenerator is dead");
        return new RemotePathIterator(aStart, aSeed);
    }
    @Override public ITimeAndParameterIterator<Lmpdat> fullPathInit(long aSeed) {
        if (mWorldMe != mWorldRoot) throw new RuntimeException("fullPathInit can ONLY be called from WorldRoot ("+mWorldRoot+")");
        if (mDead) throw new RuntimeException("This MultipleNativeLmpFullPathGenerator is dead");
        return new RemotePathIterator(null, aSeed);
    }
    
    
    /** [AtomNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities] */
    private final static int LMP_INFO_LEN = 8;
    /** 为了使用简单并且避免 double 转 long 造成的信息损耗，这里统一用 long[] 来传输信息 */
    private final static ThreadLocalObjectCachePool<long[]> LMP_INFO_CACHE = ThreadLocalObjectCachePool.withInitial(() -> new long[LMP_INFO_LEN]);
    /** 专门的方法用来收发 Lmpdat */
    private void sendLmpdat_(Lmpdat aLmpdat, int aDest, MPI.Comm aComm) throws MPI.Error {
        // 获取必要信息
        final int tAtomNum = aLmpdat.atomNum();
        final Vector tAtomID = aLmpdat.ids();
        final Vector tAtomType = aLmpdat.types();
        final RowMatrix tAtomXYZ = aLmpdat.positions();
        final @Nullable RowMatrix tVelocities = aLmpdat.velocities();
        final boolean tHasVelocities = aLmpdat.hasVelocities();
        // 先发送 Lmpdat 的必要信息，[AtomNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities]
        long[] rLmpdatInfo = LMP_INFO_CACHE.getObject();
        rLmpdatInfo[0] = tAtomNum;
        rLmpdatInfo[1] = Double.doubleToLongBits(aLmpdat.lmpBox().xlo());
        rLmpdatInfo[2] = Double.doubleToLongBits(aLmpdat.lmpBox().xhi());
        rLmpdatInfo[3] = Double.doubleToLongBits(aLmpdat.lmpBox().ylo());
        rLmpdatInfo[4] = Double.doubleToLongBits(aLmpdat.lmpBox().yhi());
        rLmpdatInfo[5] = Double.doubleToLongBits(aLmpdat.lmpBox().zlo());
        rLmpdatInfo[6] = Double.doubleToLongBits(aLmpdat.lmpBox().zhi());
        rLmpdatInfo[7] = tHasVelocities ? 1 : 0;
        aComm.send(rLmpdatInfo, LMP_INFO_LEN, aDest, LMPDAT_INFO);
        // 发送后归还临时数据
        LMP_INFO_CACHE.returnObject(rLmpdatInfo);
        // 必要信息发送完成后分别发送 atomData 和 velocities
        aComm.send(tAtomID  .getData(), tAtomID  .dataSize(), aDest, DATA_ID  );
        aComm.send(tAtomType.getData(), tAtomType.dataSize(), aDest, DATA_Type);
        aComm.send(tAtomXYZ .getData(), tAtomXYZ .dataSize(), aDest, DATA_XYZ );
        // 如果有速度信息则需要再发送一次速度信息
        if (tHasVelocities) {
            assert tVelocities != null;
            aComm.send(tVelocities.getData(), tVelocities.dataSize(), aDest, DATA_VELOCITIES);
        }
    }
    private Lmpdat recvLmpdat_(int aSource, MPI.Comm aComm) throws MPI.Error {
        // 同样先接收必要信息，[AtomNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities]
        long[] tLmpdatInfo = LMP_INFO_CACHE.getObject();
        aComm.recv(tLmpdatInfo, LMP_INFO_LEN, aSource, LMPDAT_INFO);
        // 还是使用缓存的数据
        Vector tAtomID = VectorCache.getVec((int)tLmpdatInfo[0]);
        Vector tAtomType = VectorCache.getVec((int)tLmpdatInfo[0]);
        RowMatrix tAtomXYZ = MatrixCache.getMatRow((int)tLmpdatInfo[0], ATOM_DATA_KEYS_XYZ.length);
        @Nullable RowMatrix tVelocities = null;
        // 先是基本信息，后是速度信息
        aComm.recv(tAtomID  .getData(), tAtomID  .dataSize(), aSource, DATA_ID  );
        aComm.recv(tAtomType.getData(), tAtomType.dataSize(), aSource, DATA_Type);
        aComm.recv(tAtomXYZ .getData(), tAtomXYZ .dataSize(), aSource, DATA_XYZ );
        if (tLmpdatInfo[7] == 1) {
            tVelocities = MatrixCache.getMatRow((int)tLmpdatInfo[0], ATOM_DATA_KEYS_VELOCITY.length);
            aComm.recv(tVelocities.getData(), tVelocities.dataSize(), aSource, DATA_VELOCITIES);
        }
        // 创建 Lmpdat
        Lmpdat tOut = new Lmpdat(mMesses.size(), new Box(
            Double.longBitsToDouble(tLmpdatInfo[1]), Double.longBitsToDouble(tLmpdatInfo[2]),
            Double.longBitsToDouble(tLmpdatInfo[3]), Double.longBitsToDouble(tLmpdatInfo[4]),
            Double.longBitsToDouble(tLmpdatInfo[5]), Double.longBitsToDouble(tLmpdatInfo[6])
        ), mMesses.copy(), tAtomID, tAtomType, tAtomXYZ, tVelocities);
        // 完事归还临时数据
        LMP_INFO_CACHE.returnObject(tLmpdatInfo);
        return tOut;
    }
    @SuppressWarnings("SameParameterValue")
    private Lmpdat bcastLmpdat_(Lmpdat aLmpdat, int aRoot, MPI.Comm aComm) throws MPI.Error {
        final int tMe = aComm.rank();
        if (tMe == aRoot) {
            // 获取必要信息
            final int tAtomNum = aLmpdat.atomNum();
            final Vector tAtomID = aLmpdat.ids();
            final Vector tAtomType = aLmpdat.types();
            final RowMatrix tAtomXYZ = aLmpdat.positions();
            final @Nullable RowMatrix tVelocities = aLmpdat.velocities();
            final boolean tHasVelocities = aLmpdat.hasVelocities();
            // 先发送 Lmpdat 的必要信息，[AtomNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities]
            long[] rLmpdatInfo = LMP_INFO_CACHE.getObject();
            rLmpdatInfo[0] = tAtomNum;
            rLmpdatInfo[1] = Double.doubleToLongBits(aLmpdat.lmpBox().xlo());
            rLmpdatInfo[2] = Double.doubleToLongBits(aLmpdat.lmpBox().xhi());
            rLmpdatInfo[3] = Double.doubleToLongBits(aLmpdat.lmpBox().ylo());
            rLmpdatInfo[4] = Double.doubleToLongBits(aLmpdat.lmpBox().yhi());
            rLmpdatInfo[5] = Double.doubleToLongBits(aLmpdat.lmpBox().zlo());
            rLmpdatInfo[6] = Double.doubleToLongBits(aLmpdat.lmpBox().zhi());
            rLmpdatInfo[7] = tHasVelocities ? 1 : 0;
            aComm.bcast(rLmpdatInfo, LMP_INFO_LEN, aRoot);
            // 发送后归还临时数据
            LMP_INFO_CACHE.returnObject(rLmpdatInfo);
            // 必要信息发送完成后分别发送 atomData 和 velocities
            aComm.bcast(tAtomID  .getData(), tAtomID  .dataSize(), aRoot);
            aComm.bcast(tAtomType.getData(), tAtomType.dataSize(), aRoot);
            aComm.bcast(tAtomXYZ .getData(), tAtomXYZ .dataSize(), aRoot);
            // 如果有速度信息则需要再发送一次速度信息
            if (tHasVelocities) {
                assert tVelocities != null;
                aComm.bcast(tVelocities.getData(), tVelocities.dataSize(), aRoot);
            }
            return aLmpdat;
        } else {
            // 同样先接收必要信息，[AtomNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities]
            long[] tLmpdatInfo = LMP_INFO_CACHE.getObject();
            aComm.bcast(tLmpdatInfo, LMP_INFO_LEN, aRoot);
            // 还是使用缓存的数据
            Vector tAtomID = VectorCache.getVec((int)tLmpdatInfo[0]);
            Vector tAtomType = VectorCache.getVec((int)tLmpdatInfo[0]);
            RowMatrix tAtomXYZ = MatrixCache.getMatRow((int)tLmpdatInfo[0], ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix tVelocities = null;
            // 先是基本信息，后是速度信息
            aComm.bcast(tAtomID  .getData(), tAtomID  .dataSize(), aRoot);
            aComm.bcast(tAtomType.getData(), tAtomType.dataSize(), aRoot);
            aComm.bcast(tAtomXYZ .getData(), tAtomXYZ .dataSize(), aRoot);
            if (tLmpdatInfo[7] == 1) {
                tVelocities = MatrixCache.getMatRow((int)tLmpdatInfo[0], ATOM_DATA_KEYS_VELOCITY.length);
                aComm.bcast(tVelocities.getData(), tVelocities.dataSize(), aRoot);
            }
            // 创建 Lmpdat
            Lmpdat tOut = new Lmpdat(mMesses.size(), new Box(
                Double.longBitsToDouble(tLmpdatInfo[1]), Double.longBitsToDouble(tLmpdatInfo[2]),
                Double.longBitsToDouble(tLmpdatInfo[3]), Double.longBitsToDouble(tLmpdatInfo[4]),
                Double.longBitsToDouble(tLmpdatInfo[5]), Double.longBitsToDouble(tLmpdatInfo[6])
            ), mMesses.copy(), tAtomID, tAtomType, tAtomXYZ, tVelocities);
            // 完事归还临时数据
            LMP_INFO_CACHE.returnObject(tLmpdatInfo);
            return tOut;
        }
    }
    
    
    private final static ThreadLocalObjectCachePool<double[]> DOUBLE1_CACHE = ThreadLocalObjectCachePool.withInitial(() -> new double[1]);
    private final static ThreadLocalObjectCachePool<long[]> LONG1_CACHE = ThreadLocalObjectCachePool.withInitial(() -> new long[1]);
    private final static ThreadLocalObjectCachePool<byte[]> BYTE1_CACHE = ThreadLocalObjectCachePool.withInitial(() -> new byte[1]);
    /** 用于 MPI 收发信息的 tags */
    private final static int
          LMPDAT_INFO = 100
        , DATA_XYZ = 101, DATA_ID = 103, DATA_Type = 104, DATA_VELOCITIES = 102
        , JOB_TYPE = 109
        , LONG1_INFO = 108, DOUBLE1_INFO = 107
        ;
    /** 各种任务完成的信息 tag */
    private final static int
          SHUTDOWN_FINISHED = 119
        , PATH_INIT_FINISHED = 110, PATH_FROM_FINISHED = 111
        , PATH_NEXT_FINISHED = 112, PATH_TIME_FINISHED = 113, PATH_LAMBDA_FINISHED = 114
        , PATH_SHUTDOWN_FINISHED = 118
        ;
    /** 各种任务的种类 */
    private final static byte
          SHUTDOWN = -1
        , PATH_INIT = 0, PATH_FROM = 1
        , PATH_NEXT = 2, PATH_TIME = 3, PATH_LAMBDA = 4
        , PATH_SHUTDOWN = -2
        ;
    private final static byte[]
          SHUTDOWN_BUF = {SHUTDOWN}
        , PATH_INIT_BUF = {PATH_INIT}, PATH_FROM_BUF = {PATH_FROM}
        , PATH_NEXT_BUF = {PATH_NEXT}, PATH_TIME_BUF = {PATH_TIME}, PATH_LAMBDA_BUF = {PATH_LAMBDA}
        , PATH_SHUTDOWN_BUF = {PATH_SHUTDOWN}
        ;
    
    
    private class PathGenServer implements IAutoShutdown, Runnable {
        private ITimeAndParameterIterator<Lmpdat> mIt = null;
        
        @Override public void run() {
            try {mPathGen.checkThread();}
            catch (NativeLmp.Error e) {throw new RuntimeException(e);}
            try {while (true) {
                // 获取任务种类，mLmpComm 主进程接收后使用 bcast 转发给所有进程
                byte[] rJobBuf = BYTE1_CACHE.getObject();
                if (mLmpMe == 0) {
                    mWorldComm.recv(rJobBuf, 1, mWorldRoot, JOB_TYPE);
                }
                mLmpComm.bcast(rJobBuf, 1, 0);
                byte tJob = rJobBuf[0];
                BYTE1_CACHE.returnObject(rJobBuf);
                // 根据获取到的任务种类执行操作
                switch (tJob) {
                case SHUTDOWN: {
                    shutdown();
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, SHUTDOWN_FINISHED);
                    }
                    mLmpComm.barrier();
                    return;
                }
                case PATH_INIT: {
                    long[] rSeedBuf = LONG1_CACHE.getObject();
                    if (mLmpMe == 0) {
                        mWorldComm.recv(rSeedBuf, 1, mWorldRoot, LONG1_INFO);
                    }
                    mLmpComm.bcast(rSeedBuf, 1, 0);
                    long tSeed = rSeedBuf[0];
                    LONG1_CACHE.returnObject(rSeedBuf);
                    if (mIt != null) mIt.shutdown();
                    mIt = mPathGen.fullPathInit(tSeed);
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_INIT_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_FROM: {
                    long[] rSeedBuf = LONG1_CACHE.getObject();
                    if (mLmpMe == 0) {
                        mWorldComm.recv(rSeedBuf, 1, mWorldRoot, LONG1_INFO);
                    }
                    mLmpComm.bcast(rSeedBuf, 1, 0);
                    long tSeed = rSeedBuf[0];
                    LONG1_CACHE.returnObject(rSeedBuf);
                    Lmpdat tStart = null;
                    if (mLmpMe == 0) {
                        tStart = recvLmpdat_(mWorldRoot, mWorldComm);
                    }
                    tStart = bcastLmpdat_(tStart, 0, mLmpComm);
                    if (mIt != null) mIt.shutdown();
                    mIt = mPathGen.fullPathFrom(tStart, tSeed);
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_FROM_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_NEXT: {
                    Lmpdat tNext = mIt.next();
                    if (mLmpMe == 0) {
                        sendLmpdat_(tNext, mWorldRoot, mWorldComm);
                    }
                    // 当然在这里直接 return Lmpdat 是非法的，
                    // 因为获取 lambda 时也需要这个 Lmpdat 值；
                    // 并且下一步的 lammps 运行也需要这个 Lmpdat 作为输入，
                    // 直接闪退并且不报任何错误应该也是 lammps 运行的原因
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_NEXT_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_TIME: {
                    double tTimeConsumed = mIt.timeConsumed();
                    if (mLmpMe == 0) {
                        double[] tTimeConsumedBuf = DOUBLE1_CACHE.getObject();
                        tTimeConsumedBuf[0] = tTimeConsumed;
                        mWorldComm.send(tTimeConsumedBuf, 1, mWorldRoot, DOUBLE1_INFO);
                        DOUBLE1_CACHE.returnObject(tTimeConsumedBuf);
                    }
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_TIME_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_LAMBDA: {
                    double tLambda = mIt.lambda();
                    if (mLmpMe == 0) {
                        double[] tLambdaBuf = DOUBLE1_CACHE.getObject();
                        tLambdaBuf[0] = tLambda;
                        mWorldComm.send(tLambdaBuf, 1, mWorldRoot, DOUBLE1_INFO);
                        DOUBLE1_CACHE.returnObject(tLambdaBuf);
                    }
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_LAMBDA_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_SHUTDOWN: {
                    if (mIt != null) {
                        mIt.shutdown();
                        mIt = null;
                    }
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_SHUTDOWN_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                default: {
                    throw new IllegalArgumentException("job type: "+tJob);
                }}
            }} catch (MPI.Error e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override public void shutdown() {
            if (mIt != null) {
                mIt.shutdown();
                mIt = null;
            }
            mPathGen.shutdown();
            // 由于是 copy 的，可以并且应该直接 shutdown，注意线程安全的问题
            synchronized (mLmpComm) {mLmpComm.shutdown();}
        }
    }
    
    
    private class RemotePathIterator implements ITimeAndParameterIterator<Lmpdat>, IAutoShutdown {
        private int mLmpRoot;
        /** 创建时进行初始化 */
        RemotePathIterator(@Nullable IAtomData aStart, long aSeed) {
            // 尝试获取对应 lmp 根节点发送数据
            @Nullable Integer tLmpRoot = mLmpRoots.pollLast();
            if (tLmpRoot == null) {
                System.err.println("WARNING: Can NOT to get LmpRoot for this path gen temporarily, this path gen blocks until there are any free LmpRoot.");
                System.err.println("It may be caused by too large number of parallels.");
            }
            while (tLmpRoot == null) {
                try {Thread.sleep(FILE_SYSTEM_SLEEP_TIME);}
                catch (InterruptedException e) {throw new RuntimeException(e);}
                tLmpRoot = mLmpRoots.pollLast();
            }
            mLmpRoot = tLmpRoot;
            try {
                // 根据输入发送创建一个 path 的任务
                mWorldComm.send(aStart==null ? PATH_INIT_BUF : PATH_FROM_BUF, 1, mLmpRoot, JOB_TYPE);
                // 无论怎样都先发送种子
                long[] tSeedBuf = LONG1_CACHE.getObject();
                tSeedBuf[0] = aSeed;
                mWorldComm.send(tSeedBuf, 1, mLmpRoot, LONG1_INFO);
                LONG1_CACHE.returnObject(tSeedBuf);
                if (aStart != null) {
                    // from 需要发送整个 Lmpdat
                    Lmpdat tStart = (aStart instanceof Lmpdat) ? (Lmpdat)aStart : Lmpdat.fromAtomData(aStart);
                    sendLmpdat_(tStart, mLmpRoot, mWorldComm);
                }
                // 接收任务完成信息
                mWorldComm.recv(mLmpRoot, aStart==null ? PATH_INIT_FINISHED : PATH_FROM_FINISHED);
            } catch (Throwable t) {
                this.shutdown();
                throw new RuntimeException(t);
            }
        }
        
        @Override public Lmpdat next() {
            if (mLmpRoot < 0) throw new RuntimeException("This RemotePathIterator is dead");
            try {
                // 发送 next 任务
                mWorldComm.send(PATH_NEXT_BUF, 1, mLmpRoot, JOB_TYPE);
                // 之后获取 Lmpdat 即可
                Lmpdat tNext = recvLmpdat_(mLmpRoot, mWorldComm);
                // 接收任务完成信息
                mWorldComm.recv(mLmpRoot, PATH_NEXT_FINISHED);
                return tNext;
            } catch (MPI.Error e) {
                throw new RuntimeException(e);
            }
        }
        @Override public double timeConsumed() {
            if (mLmpRoot < 0) throw new RuntimeException("This RemotePathIterator is dead");
            try {
                // 发送获取时间任务
                mWorldComm.send(PATH_TIME_BUF, 1, mLmpRoot, JOB_TYPE);
                // 之后获取时间即可
                double[] rTimeConsumedBuf = DOUBLE1_CACHE.getObject();
                mWorldComm.recv(rTimeConsumedBuf, 1, mLmpRoot, DOUBLE1_INFO);
                double tTimeConsumed = rTimeConsumedBuf[0];
                DOUBLE1_CACHE.returnObject(rTimeConsumedBuf);
                // 接收任务完成信息
                mWorldComm.recv(mLmpRoot, PATH_TIME_FINISHED);
                return tTimeConsumed;
            } catch (MPI.Error e) {
                throw new RuntimeException(e);
            }
        }
        @Override public double lambda() {
            if (mLmpRoot < 0) throw new RuntimeException("This RemotePathIterator is dead");
            try {
                // 发送获取 lambda 任务
                mWorldComm.send(PATH_LAMBDA_BUF, 1, mLmpRoot, JOB_TYPE);
                // 之后获取 lambda
                double[] rLambdaBuf = DOUBLE1_CACHE.getObject();
                mWorldComm.recv(rLambdaBuf, 1, mLmpRoot, DOUBLE1_INFO);
                double tLambda = rLambdaBuf[0];
                DOUBLE1_CACHE.returnObject(rLambdaBuf);
                // 接收任务完成信息
                mWorldComm.recv(mLmpRoot, PATH_LAMBDA_FINISHED);
                return tLambda;
            } catch (MPI.Error e) {
                throw new RuntimeException(e);
            }
        }
        /** 完整路径永远都有 next */
        @Override public boolean hasNext() {return true;}
        
        /** 关闭时归还 mLmpRoot */
        @Override public void shutdown() {
            if (mLmpRoot >= 0) {
                try {
                    mWorldComm.send(PATH_SHUTDOWN_BUF, 1, mLmpRoot, JOB_TYPE);
                    mWorldComm.recv(mLmpRoot, PATH_SHUTDOWN_FINISHED);
                } catch (MPI.Error e) {
                    e.printStackTrace(System.err);
                }
                mLmpRoots.addLast(mLmpRoot);
                mLmpRoot = -1;
            }
        }
    }
    
    
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        if (mPathGen.threadValid()) {
            mPathGen.shutdown(); // 这样会存在重复关闭的问题，不过不重要就是
        }
        if (mWorldMe==mWorldRoot) {
            for (int tLmpRoot : mLmpRoots) {
                try {
                    mWorldComm.send(SHUTDOWN_BUF, 1, tLmpRoot, JOB_TYPE);
                    mWorldComm.recv(tLmpRoot, SHUTDOWN_FINISHED);
                } catch (MPI.Error e) {
                    e.printStackTrace(System.err);
                }
            }
            mLmpRoots.clear();
        }
        // 由于是 copy 的，可以并且应该直接 shutdown，注意线程安全的问题
        synchronized (mLmpComm) {mLmpComm.shutdown();} // 这样会存在重复关闭的问题，不过不重要就是
    }
}
