package jsex.rareevent.lmp;

import jse.atom.IAtomData;
import jse.code.collection.NewCollections;
import jse.code.random.IRandom;
import jse.lmp.LmpException;
import jse.lmp.Lmpdat;
import jse.lmp.NativeLmp;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import jse.parallel.IAutoShutdown;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import jsex.rareevent.IFullPathGenerator;
import jsex.rareevent.IParameterCalculator;
import jsex.rareevent.ITimeAndParameterIterator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static jse.code.CS.MAX_SEED;


/**
 * 一种路径生成器，通过原生运行 {@link NativeLmp} 来直接生成完整的路径
 * <p>
 * 和 {@link NativeLmp} 类似，每个进程都需要调用 new 来进行创建，
 * 后续所有信息也需要所有进程进行调用来进行 mpi 计算；
 * 除了特殊说明，输入参数都需要一致
 * <p>
 * 由于 lammps 的特性，此类线程不安全，并且要求所有方法都由相同的线程调用
 * <p>
 * 现在统一对于包含 {@link MPI.Comm} 不进行自动关闭的管理，
 * 这样可以简单处理很多情况
 * @author liqa
 */
@ApiStatus.Experimental
public class NativeLmpFullPathGenerator implements IFullPathGenerator<IAtomData> {
    private final static String[] LMP_ARGS = {"-log", "none", "-screen", "none"};
    /** 热浴的策略，默认为 NOSE_HOOVER */
    public final static byte NOSE_HOOVER = 0, LANGEVIN = 1;
    
    private final List<Lmpdat> mInitPoints;
    private final IVector mMasses;
    private final double mTemperature;
    private final String mPairStyle, mPairCoeff;
    private final double mTimestep;
    private final int mDumpStep;
    private final byte mThermostat;
    
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
     * @param aParameterCalculator 计算对应 Lmpdat 参数的计算器，建议使用 MPI 版本的计算器，并使用相同范围的 {@link MPI.Comm}
     * @param aInitAtomDataList  用于初始的原子数据
     * @param aMasses 每个种类的原子对应的摩尔质量，且长度指定原子种类数目
     * @param aTemperature 创建路径的温度
     * @param aPairStyle lammps 输入文件使用的势场类型
     * @param aPairCoeff lammps 输入文件使用的势场参数
     * @param aTimestep 每步的实际时间步长，影响输入文件和统计使用的时间，默认为 0.002 (ps)
     * @param aDumpStep 每隔多少模拟步输出一个 dump，默认为 10
     * @param aThermostat 选择的热浴，默认为 {@link #NOSE_HOOVER}
     */
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, byte aThermostat) throws LmpException {
        // 基本参数存储
        mInitPoints = NewCollections.map(aInitAtomDataList, data -> Lmpdat.fromAtomData(data).setNoVelocity()); // 初始点也需要移除速度，保证会从不同路径开始
        mMasses = aMasses.copy();
        mTemperature = aTemperature;
        mPairStyle = aPairStyle; mPairCoeff = aPairCoeff;
        mTimestep = aTimestep;
        mDumpStep = aDumpStep;
        mThermostat = aThermostat;
        // Lmp 相关参数
        mLmp = new NativeLmp(LMP_ARGS, aLmpComm);
        mParameterCalculator = aParameterCalculator;
    }
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                  ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, aMasses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, NOSE_HOOVER);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                 ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, aMasses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMasses, double aTemperature, String aPairStyle, String aPairCoeff                                                   ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, aMasses, aTemperature, aPairStyle, aPairCoeff, 0.002);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, byte aThermostat) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aThermostat);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                  ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                 ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff, aTimestep);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMasses, double aTemperature, String aPairStyle, String aPairCoeff                                                   ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, byte aThermostat) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aThermostat);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                  ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMasses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                 ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff, aTimestep);}
    public NativeLmpFullPathGenerator(MPI.Comm aLmpComm, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMasses, double aTemperature, String aPairStyle, String aPairCoeff                                                   ) throws LmpException {this(aLmpComm, aParameterCalculator, aInitAtomDataList, Vectors.from(aMasses), aTemperature, aPairStyle, aPairCoeff);}
    
    NativeLmpFullPathGenerator setReturnLast() {mReturnLast = true; return this;}
    
    
    @Override public ITimeAndParameterIterator<Lmpdat> fullPathFrom(IAtomData aStart, IRandom aRNG) {return new NativeLmpIterator(aStart, aRNG);}
    @Override public ITimeAndParameterIterator<Lmpdat> fullPathInit(IRandom aRNG) {return new NativeLmpIterator(null, aRNG);}
    
    private class NativeLmpIterator implements ITimeAndParameterIterator<Lmpdat>, IAutoShutdown {
        /** 一些状态存储，专门优化第一次调用 */
        private boolean mIsFirst = true;
        private boolean mLmpNeedInit = true;
        private boolean mDead = false;
        private boolean mNextIsFromNativeLmp = false;
        /** 路径部分 */
        private @NotNull Lmpdat mNext;
        private final IRandom mRNG;
        
        /** 创建时进行初始化 */
        NativeLmpIterator(@Nullable IAtomData aStart, IRandom aRNG) {
            if (mUsing) throw new RuntimeException("NativeLmpFullPathGenerator can ONLY have ONE active path.");
            mUsing = true;
            mRNG = aRNG;
            mNext = aStart==null ? mInitPoints.get(mRNG.nextInt(mInitPoints.size())) : ((aStart instanceof Lmpdat) ? (Lmpdat)aStart : Lmpdat.fromAtomData(aStart));
        }
        
        /** 用于方便子类重写从而实现自定义的策略 */
        protected void initLmp_() throws LmpException {
            mLmp.command("units           metal");
            mLmp.command("boundary        p p p");
            mLmp.command("timestep        "+mTimestep);
            mLmp.loadLmpdat(mNext.setMasses(mMasses)); // 在这里统一设置质量
            mLmp.command("pair_style      "+mPairStyle);
            mLmp.command("pair_coeff      "+mPairCoeff); // MARK: 好像卡在这里，但是不一定
            // 逻辑上只要没有速度还是需要重新分配速度
            if (!mNext.hasVelocity()) {
                mLmp.command(String.format("velocity        all create %f %d dist gaussian mom yes rot yes", mTemperature, mRNG.nextInt(MAX_SEED)+1));
            }
            switch(mThermostat) {
            case LANGEVIN: {
                mLmp.command("fix             1 all nve");
                mLmp.command(String.format("fix             2 all langevin %f %f %f %d", mTemperature, mTemperature, mTimestep*100.0*1000.0, mRNG.nextInt(MAX_SEED)+1));
                mLmp.command(String.format("fix             3 all press/berendsen iso 0.0 0.0 %f", mTimestep*1000.0*1000.0));
                break;
            }
            case NOSE_HOOVER: default: {
                mLmp.command(String.format("fix             1 all npt temp %f %f %f iso 0.0 0.0 %f", mTemperature, mTemperature, mTimestep*100.0, mTimestep*1000.0));
                break;
            }}
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
                    initLmp_();
                } catch (LmpException e) {
                    throw new RuntimeException(e);
                }
            }
            try {mLmp.command("run "+mDumpStep);}
            catch (LmpException e) {throw new RuntimeException(e);}
            // 由于这里底层的 NativeLmp 获取的 Lmpdat 也是使用了缓存，因此对于上一步的 mNext 可以归还；
            // 当然一般情况下获取的 next 会在外部保存，不能归还，因此默认关闭
            if (mReturnLast && mNextIsFromNativeLmp) {
                mNext.returnToCache();
            }
            try {mNext = mLmp.lmpdat(mThermostat!=LANGEVIN); mNextIsFromNativeLmp = true;}
            catch (LmpException | MPIException e) {throw new RuntimeException(e);}
            return mNext;
        }
        
        /** 获取当前位置点从初始开始消耗的时间，如果没有调用过 next 则会抛出错误 */
        @Override public double timeConsumed() {
            if (mDead) throw new RuntimeException("This NativeLmpIterator is dead");
            if (mIsFirst) throw new IllegalStateException();
            if (mLmpNeedInit) return 0.0;
            try {return mLmp.thermoOf("step") * mTimestep;}
            catch (LmpException e) {throw new RuntimeException(e);}
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
                catch (LmpException ignored) {}
                mUsing = false;
            }
        }
    }
    
    @Override public void shutdown() {mLmp.shutdown();}
    
    
    public boolean threadValid() {return mLmp.threadValid();}
    public void checkThread() throws LmpException {mLmp.checkThread();}
}
