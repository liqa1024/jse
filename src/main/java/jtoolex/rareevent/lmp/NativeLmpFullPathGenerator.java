package jtoolex.rareevent.lmp;

import jtool.atom.IAtomData;
import jtool.code.collection.NewCollections;
import jtool.lmp.Lmpdat;
import jtool.lmp.NativeLmp;
import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import jtool.parallel.*;
import jtoolex.rareevent.IFullPathGenerator;
import jtoolex.rareevent.IParameterCalculator;
import jtoolex.rareevent.ITimeAndParameterIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static jtool.code.CS.MAX_SEED;


/**
 * 一种路径生成器，通过原生运行 {@link NativeLmp} 来直接生成完整的路径
 * <p>
 * 和 {@link NativeLmp} 类似，每个进程都需要调用 new 来进行创建，
 * 后续所有信息也需要所有进程进行调用来进行 mpi 计算；
 * 除了特殊说明，输入参数都需要一致
 * <p>
 * 要求这些方法是线程安全的，可以同一个实例并行运行同一个方法，注意获取到的容器是线程不安全的（不同实例间线程安全）
 * <p>
 * 现在统一对于包含 {@link MPI.Comm} 不进行自动关闭的管理，和 {@link NativeLmp} 一致，
 * 这样可以简单处理很多情况
 * @author liqa
 */
public class NativeLmpFullPathGenerator implements IFullPathGenerator<IAtomData> {
    private final static String[] LMP_ARGS = {"-log", "none", "-screen", "none"};
    
    private final List<Lmpdat> mInitPoints;
    private final IVector mMesses;
    private final double mTemperature;
    private final String mPairStyle, mPairCoeff;
    private final double mTimestep;
    private final int mDumpStep;
    
    /** 现在固定只有一个 lammps 实例 */
    private final NativeLmp mLmp;
    private volatile boolean mUsing = false;
    private final IParameterCalculator<? super IAtomData> mParameterCalculator;
    
    private boolean mReturnLast = false;
    
    /**
     * 创建一个生成器；
     * 每个进程都需要调用来进行创建，除了特殊说明，输入参数都需要一致
     * @author liqa
     * @param aLmpComm 此进程需要运行 {@link NativeLmp} 的 {@link MPI.Comm}，默认为 {@link MPI.Comm#WORLD}
     * @param aParameterCalculator 计算对应 Lmpdat 参数的计算器，建议使用 MPI 版本的计算器，并使用相同的 {@link MPI.Comm}
     * @param aInitAtomDataList  用于初始的原子数据
     * @param aMesses 每个种类的原子对应的摩尔质量，且长度指定原子种类数目
     * @param aTemperature 创建路径的温度
     * @param aPairStyle lammps 输入文件使用的势场类型
     * @param aPairCoeff lammps 输入文件使用的势场参数
     * @param aTimestep 每步的实际时间步长，影响输入文件和统计使用的时间，默认为 0.002 (ps)
     * @param aDumpStep 每隔多少模拟步输出一个 dump，默认为 10
     */
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep) throws NativeLmp.Error {
        // 基本参数存储
        mInitPoints = NewCollections.map(aInitAtomDataList, data -> Lmpdat.fromAtomData(data).setNoVelocities()); // 初始点也需要移除速度，保证会从不同路径开始
        mMesses = aMesses.copy();
        mTemperature = aTemperature;
        mPairStyle = aPairStyle; mPairCoeff = aPairCoeff;
        mTimestep = aTimestep;
        mDumpStep = aDumpStep;
        // Lmp 相关参数
        mLmp = new NativeLmp(LMP_ARGS, aLmpComm);
        mParameterCalculator = aParameterCalculator;
    }
    
    NativeLmpFullPathGenerator setReturnLast() {mReturnLast = true; return this;}
    
    
    @Override public ITimeAndParameterIterator<Lmpdat> fullPathFrom(IAtomData aStart, long aSeed) {return new NativeLmpIterator(aStart, aSeed);}
    @Override public ITimeAndParameterIterator<Lmpdat> fullPathInit(long aSeed) {return new NativeLmpIterator(null, aSeed);}
    
    private class NativeLmpIterator implements ITimeAndParameterIterator<Lmpdat>, IAutoShutdown {
        /** 一些状态存储，专门优化第一次调用 */
        private boolean mIsFirst = true;
        private boolean mLmpNeedInit = true;
        private boolean mDead = false;
        /** 路径部分 */
        private @NotNull Lmpdat mNext;
        /** 此路径的局部随机数生成器，由于约定了相同实例线程不安全，并且考虑到可能存在的高并发需求，因此直接使用 {@link LocalRandom} */
        private final LocalRandom mRNG;
        
        /** 创建时进行初始化 */
        NativeLmpIterator(@Nullable IAtomData aStart, long aSeed) {
            if (mUsing) throw new RuntimeException("NativeLmpFullPathGenerator can ONLY have ONE active path.");
            mUsing = true;
            mRNG = new LocalRandom(aSeed);
            mNext = aStart==null ? mInitPoints.get(mRNG.nextInt(mInitPoints.size())) : (aStart instanceof Lmpdat) ? (Lmpdat)aStart : Lmpdat.fromAtomData(aStart);
        }
        
        /** 这里获取到的点需要是精简的 */
        @Override public Lmpdat next() {
            if (mDead) throw new RuntimeException("This NativeLmpIterator is dead");
            // 第一次调用特殊优化，直接返回
            if (mIsFirst) {
                mIsFirst = false;
                return mNext;
            }
            // 一般操作直接合法化后 next
            if (mLmpNeedInit) {
                mLmpNeedInit = false;
                try {
                    mLmp.command("units           metal");
                    mLmp.command("boundary        p p p");
                    mLmp.command("timestep        "+mTimestep);
                    mLmp.loadLmpdat(mNext.setMasses(mMesses)); // 在这里统一设置质量
                    mLmp.command("pair_style      "+mPairStyle);
                    mLmp.command("pair_coeff      "+mPairCoeff); // MARK: 好像卡在这里，但是不一定
                    // 虽然理论上永远都是没有速度并且重新分配速度，这里还是和原本保持逻辑一致
                    if (!mNext.hasVelocities()) {
                        mLmp.command(String.format("velocity        all create %f %d dist gaussian mom yes rot yes", mTemperature, mRNG.nextInt(MAX_SEED)));
                    }
                    mLmp.command(String.format("fix             1 all npt temp %f %f 0.2 iso 0.0 0.0 2", mTemperature, mTemperature));
                } catch (NativeLmp.Error e) {
                    throw new RuntimeException(e);
                }
            }
            try {mLmp.command("run "+mDumpStep);}
            catch (NativeLmp.Error e) {throw new RuntimeException(e);}
            // 由于这里底层的 NativeLmp 获取的 Lmpdat 也是使用了缓存，因此对于上一步的 mNext 可以归还；
            // 当然一般情况下获取的 next 会在外部保存，不能归还，因此默认关闭
            if (mReturnLast) {
                if (mNext.hasVelocities()) {
                    RowMatrix tVelocities = mNext.velocities();
                    assert tVelocities != null;
                    MatrixCache.returnMat(tVelocities);
                }
                MatrixCache.returnMat(mNext.positions());
                VectorCache.returnVec(mNext.types());
                VectorCache.returnVec(mNext.ids());
            }
            try {mNext = mLmp.lmpdat(true);}
            catch (NativeLmp.Error e) {throw new RuntimeException(e);}
            return mNext;
        }
        
        /** 获取当前位置点从初始开始消耗的时间，如果没有调用过 next 则会抛出错误 */
        @Override public double timeConsumed() {
            if (mDead) throw new RuntimeException("This NativeLmpIterator is dead");
            if (mIsFirst) throw new IllegalStateException();
            if (mLmpNeedInit) return 0.0;
            try {return mLmp.thermoOf("step") * mTimestep;}
            catch (NativeLmp.Error e) {throw new RuntimeException(e);}
        }
        
        /** 获取当前位置点的参数 λ */
        @Override public double lambda() {
            if (mDead) throw new RuntimeException("This NativeLmpIterator is dead");
            if (mIsFirst) throw new IllegalStateException();
            return mParameterCalculator.lambdaOf(mNext);
        }
        
        /** 完整路径永远都有 next */
        @Override public boolean hasNext() {return true;}
        
        @Override public void shutdown() {
            if (!mDead) {
                mDead = true;
                try {mLmp.clear();}
                catch (NativeLmp.Error ignored) {}
                mUsing = false;
            }
        }
    }
    
    @Override public void shutdown() {mLmp.shutdown();}
}
