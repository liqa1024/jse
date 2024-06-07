package jsex.rareevent.lmp;

import jse.atom.IAtomData;
import jse.code.UT;
import jse.code.timer.AccumulatedTimer;
import jse.code.timer.FixedTimer;
import jse.lmp.LmpException;
import jse.lmp.Lmpdat;
import jse.lmp.NativeLmp;
import jse.math.vector.IVector;
import jse.parallel.IAutoShutdown;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import jsex.rareevent.IFullPathGenerator;
import jsex.rareevent.IParameterCalculator;
import jsex.rareevent.ITimeAndParameterIterator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static jse.code.CS.FILE_SYSTEM_SLEEP_TIME;

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
    /** 控制是否在包含 mWorldRoot 的 mLmpComm 上运行 lammps，对于资源受限的超算系统可能有用 */
    public static boolean NO_LMP_IN_WORLD_ROOT = false;
    
    private final MPI.Comm mWorldComm;
    private final int mWorldRoot;
    private final int mWorldMe;
    private final MPI.Comm mLmpComm;
    private final Deque<Integer> mLmpRoots;
    private final int mLmpMe;
    
    private final NativeLmpFullPathGenerator mPathGen;
    
    private volatile boolean mDead = false;
    
    private MultipleNativeLmpFullPathGenerator(MPI.Comm aWorldComm, int aWorldRoot, MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, NativeLmpFullPathGenerator aPathGen) throws MPIException {
        // MPI 相关参数
        mWorldComm = aWorldComm;
        mWorldRoot = aWorldRoot;
        mWorldMe = mWorldComm.rank();
        mLmpComm = aLmpComm.copy(); // 对于数据传输使用的 Comm 要拷贝一份，和 lammps 使用的 Comm 进行区分避免相互干扰
        try {
            mLmpMe = mLmpComm.rank();
            if (aLmpRoots == null) {
                if (mWorldMe == mWorldRoot) throw new IllegalArgumentException("aLmpRoots of WorldRoot ("+mWorldRoot+") can NOT be null");
                mLmpRoots = null;
            } else {
                // 直接使用 ConcurrentLinkedDeque 来简单处理并行访问的情况
                mLmpRoots = new ConcurrentLinkedDeque<>(aLmpRoots);
            }
            mPathGen = aPathGen.setReturnLast();
        } catch (Exception e) {
            mLmpComm.shutdown();
            throw e;
        }
    }
    
    /** 这里改为 static 方法来构造，从而避免一些问题，顺便实现自动资源释放 */
    private static void withOf_(MPI.Comm aWorldComm, int aWorldRoot, MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, NativeLmpFullPathGenerator aPathGen, Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {
        try (MultipleNativeLmpFullPathGenerator tPathGen = new MultipleNativeLmpFullPathGenerator(aWorldComm, aWorldRoot, aLmpComm, aLmpRoots, aPathGen)) {
            // 在另一个线程执行后续操作
            Future<Void> tLaterTask = null;
            if (tPathGen.mWorldMe == aWorldRoot) {
                tLaterTask = UT.Par.runAsync(() -> {
                    try {aDoLater.accept(tPathGen);}
                    finally {tPathGen.shutdown();} // 需要在结束时手动关闭 tPathGen 让主线程 tServer 自动退出
                });
            }
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
     * @param aThermostat 选择的热浴，默认为 {@link NativeLmpFullPathGenerator#NOSE_HOOVER}
     */
    public static void withOf(MPI.Comm aWorldComm, int aWorldRoot, MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, byte aThermostat, Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {
        withOf_(aWorldComm, aWorldRoot, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aThermostat), aDoLater);
    }
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, byte aThermostat, Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aThermostat), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep,                   Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep,                                  Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff,                                                    Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, byte aThermostat, Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aThermostat), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep,                   Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep,                                  Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff,                                                    Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, byte aThermostat, Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aThermostat), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep,                   Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep,                                  Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep), aDoLater);}
    public static void withOf(MPI.Comm aLmpComm, @Nullable List<Integer> aLmpRoots, IParameterCalculator<? super IAtomData> aParameterCalculator, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff,                                                    Consumer<? super MultipleNativeLmpFullPathGenerator> aDoLater) throws Exception {withOf_(MPI.Comm.WORLD, 0, aLmpComm, aLmpRoots, new NativeLmpFullPathGenerator(aLmpComm, aParameterCalculator, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff), aDoLater);}
    
    
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
    
    /** 用于 MPI 收发信息的 tags */
    private final static int
          JOB_TYPE = 109
        , SEED = 101
        , TIME_CONSUMED = 102, LAMBDA = 103
        , TIMER_INFO = 106
        ;
    /** 各种任务完成的信息 tag */
    private final static int
          SHUTDOWN_FINISHED = 119
        , PATH_INIT_FINISHED = 110, PATH_FROM_FINISHED = 111
        , PATH_NEXT_FINISHED = 112, PATH_TIME_FINISHED = 113, PATH_LAMBDA_FINISHED = 114
        , PATH_SHUTDOWN_FINISHED = 118
        , TIMER_INIT_FINISHED = 115, TIMER_GET_FINISHED = 116, TIMER_RESET_FINISHED = 117
        ;
    /** 各种任务的种类 */
    private final static byte
          SHUTDOWN = -1
        , PATH_INIT = 0, PATH_FROM = 1
        , PATH_NEXT = 2, PATH_TIME = 3, PATH_LAMBDA = 4
        , PATH_SHUTDOWN = -2
        , TIMER_INIT = 5, TIMER_GET = 6, TIMER_RESET = 7
        , JOBID_NULL = -9
        ;
    
    
    private class PathGenServer implements IAutoShutdown, Runnable {
        private ITimeAndParameterIterator<Lmpdat> mIt = null;
        /** 一些用来统计效率的计时器 */
        private @Nullable FixedTimer mTotTimer = null;
        private @Nullable AccumulatedTimer mLmpTimer = null, mCalTimer = null, mWaitTimer = null;
        
        private byte getJobID_() throws MPIException {
            byte tJobID = JOBID_NULL;
            if (mLmpMe == 0) {
                if (mWaitTimer != null) mWaitTimer.from();
                tJobID = mWorldComm.recvB(mWorldRoot, JOB_TYPE);
                if (mWaitTimer != null) mWaitTimer.to();
            }
            return mLmpComm.bcastB(tJobID, 0);
        }
        private long getSeed_() throws MPIException {
            long tSeed = 0;
            if (mLmpMe == 0) {
                tSeed = mWorldComm.recvL(mWorldRoot, SEED);
            }
            return mLmpComm.bcastL(tSeed, 0);
        }
        
        @Override public void run() {
            try {mPathGen.checkThread();}
            catch (LmpException e) {throw new RuntimeException(e);}
            try {while (true) {
                // 获取任务种类，mLmpComm 主进程接收后使用 bcast 转发给所有进程
                byte tJob = getJobID_();
                // 根据获取到的任务种类执行操作
                switch (tJob) {
                case SHUTDOWN: {
                    shutdown();
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, SHUTDOWN_FINISHED);
                    }
                    mWorldComm.barrier(); // 注意这个 shutdown 是全局操作，并且此时 mLmpComm 已经失效
                    return;
                }
                case PATH_INIT: {
                    long tSeed = getSeed_();
                    if (mIt != null) mIt.shutdown();
                    mIt = mPathGen.fullPathInit(tSeed);
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_INIT_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_FROM: {
                    // 注意顺序，约定一定是先接受 seed 后接受 lmpdat
                    long tSeed = getSeed_();
                    Lmpdat tStart = null;
                    if (mLmpMe == 0) {
                        tStart = Lmpdat.recv(mWorldRoot, mWorldComm);
                    }
                    tStart = Lmpdat.bcast(tStart, 0, mLmpComm);
                    if (mIt != null) mIt.shutdown();
                    mIt = mPathGen.fullPathFrom(tStart, tSeed);
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_FROM_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_NEXT: {
                    if (mLmpMe==0 && mLmpTimer!=null) mLmpTimer.from();
                    Lmpdat tNext = mIt.next();
                    if (mLmpMe==0 && mLmpTimer!=null) mLmpTimer.to();
                    if (mLmpMe == 0) {
                        Lmpdat.send(tNext, mWorldRoot, mWorldComm);
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
                        mWorldComm.sendD(tTimeConsumed, mWorldRoot, TIME_CONSUMED);
                    }
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, PATH_TIME_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case PATH_LAMBDA: {
                    if (mLmpMe==0 && mCalTimer!=null) mCalTimer.from();
                    double tLambda = mIt.lambda();
                    if (mLmpMe==0 && mCalTimer!=null) mCalTimer.to();
                    if (mLmpMe == 0) {
                        mWorldComm.sendD(tLambda, mWorldRoot, LAMBDA);
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
                case TIMER_INIT: {
                    // 这里简单处理，只需要 mLmpMe == 0 的统计耗时
                    if (mLmpMe == 0) {
                        mTotTimer = new FixedTimer();
                        mLmpTimer = new AccumulatedTimer();
                        mCalTimer = new AccumulatedTimer();
                        mWaitTimer = new AccumulatedTimer();
                    }
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, TIMER_INIT_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case TIMER_GET: {
                    if (mLmpMe == 0) {
                        assert mTotTimer!=null && mLmpTimer!=null && mCalTimer!=null && mWaitTimer!=null;
                        mWorldComm.send(new double[] {
                              mTotTimer.get()
                            , mLmpTimer.get()
                            , mCalTimer.get()
                            , mWaitTimer.get()
                        }, 4, mWorldRoot, TIMER_INFO);
                    }
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, TIMER_GET_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                case TIMER_RESET: {
                    if (mLmpMe == 0) {
                        assert mTotTimer!=null && mLmpTimer!=null && mCalTimer!=null && mWaitTimer!=null;
                        mTotTimer.reset();
                        mLmpTimer.reset();
                        mCalTimer.reset();
                        mWaitTimer.reset();
                    }
                    if (mLmpMe == 0) {
                        mWorldComm.send(mWorldRoot, TIMER_RESET_FINISHED);
                    }
                    mLmpComm.barrier();
                    break;
                }
                default: {
                    throw new IllegalArgumentException("job type: "+tJob);
                }}
            }} catch (MPIException e) {
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
    
    /** 实用接口，获取可用的并行数，目前在设置 NO_LMP_IN_WORLD_ROOT 后不一定为输入的并行数 */
    public int parallelNum() {
        if (mWorldMe != mWorldRoot) throw new RuntimeException("parallelNum can ONLY be called from WorldRoot ("+mWorldRoot+")");
        if (!NO_LMP_IN_WORLD_ROOT) return mLmpRoots.size();
        int rParallelNum = 0;
        for (int tLmpRoot : mLmpRoots) {
            if (tLmpRoot != mWorldMe) ++rParallelNum;
        }
        return rParallelNum;
    }
    
    /** 获取统计时间的接口 */
    public void initTimer() throws MPIException {
        if (mWorldMe != mWorldRoot) throw new RuntimeException("initTimer can ONLY be called from WorldRoot ("+mWorldRoot+")");
        if (mDead) throw new RuntimeException("This MultipleNativeLmpFullPathGenerator is dead");
        for (int tLmpRoot : mLmpRoots) {
            mWorldComm.sendB(TIMER_INIT, tLmpRoot, JOB_TYPE);
            mWorldComm.recv(tLmpRoot, TIMER_INIT_FINISHED);
        }
    }
    public void resetTimer() throws MPIException {
        if (mWorldMe != mWorldRoot) throw new RuntimeException("resetTimer can ONLY be called from WorldRoot ("+mWorldRoot+")");
        if (mDead) throw new RuntimeException("This MultipleNativeLmpFullPathGenerator is dead");
        for (int tLmpRoot : mLmpRoots) {
            mWorldComm.sendB(TIMER_RESET, tLmpRoot, JOB_TYPE);
            mWorldComm.recv(tLmpRoot, TIMER_RESET_FINISHED);
        }
    }
    public static class TimerInfo {
        public final double total, lmp, lambda, wait;
        public final double other;
        public TimerInfo(double total, double lmp, double lambda, double wait) {
            this.total = total; this.lmp = lmp; this.lambda = lambda; this.wait = wait;
            this.other = this.total - this.lmp - this.lambda - this.wait;
        }
    }
    public TimerInfo getTimerInfo() throws MPIException {return getTimerInfo(NO_LMP_IN_WORLD_ROOT);} // 如果关闭了 worldRoot 的 lammps 运行，则当然默认关闭其效率统计
    @SuppressWarnings("RedundantCast")
    public TimerInfo getTimerInfo(boolean aExcludeWorldRoot) throws MPIException {
        if (mWorldMe != mWorldRoot) throw new RuntimeException("getTimerInfo can ONLY be called from WorldRoot ("+mWorldRoot+")");
        if (mDead) throw new RuntimeException("This MultipleNativeLmpFullPathGenerator is dead");
        double rTotal = 0.0, rLmp = 0.0, rLambda = 0.0, rWait = 0.0;
        int tStatTimes = 0;
        double[] rTimeBuf = new double[4];
        for (int tLmpRoot : mLmpRoots) {
            if (aExcludeWorldRoot && tLmpRoot==mWorldRoot) continue;
            mWorldComm.sendB(TIMER_GET, tLmpRoot, JOB_TYPE);
            mWorldComm.recv(rTimeBuf, 4, tLmpRoot, TIMER_INFO);
            mWorldComm.recv(tLmpRoot, TIMER_GET_FINISHED);
            rTotal  += rTimeBuf[0];
            rLmp    += rTimeBuf[1];
            rLambda += rTimeBuf[2];
            rWait   += rTimeBuf[3];
            ++tStatTimes;
        }
        rTotal  /= (double)tStatTimes;
        rLmp    /= (double)tStatTimes;
        rLambda /= (double)tStatTimes;
        rWait   /= (double)tStatTimes;
        return new TimerInfo(rTotal, rLmp, rLambda, rWait);
    }
    
    
    private class RemotePathIterator implements ITimeAndParameterIterator<Lmpdat>, IAutoShutdown {
        private int mLmpRoot;
        /** 创建时进行初始化 */
        RemotePathIterator(@Nullable IAtomData aStart, long aSeed) {
            // 尝试获取对应 lmp 根节点发送数据
            @Nullable Integer tLmpRoot = mLmpRoots.pollLast();
            if (NO_LMP_IN_WORLD_ROOT && tLmpRoot!=null && tLmpRoot==mWorldRoot) {mLmpRoots.addLast(tLmpRoot); tLmpRoot = null;}
            if (tLmpRoot == null) {
                System.err.println("WARNING: Can NOT to get LmpRoot for this path gen temporarily, this path gen blocks until there are any free LmpRoot.");
                System.err.println("It may be caused by too large number of parallels.");
            }
            while (tLmpRoot == null) {
                try {Thread.sleep(FILE_SYSTEM_SLEEP_TIME);}
                catch (InterruptedException e) {throw new RuntimeException(e);}
                tLmpRoot = mLmpRoots.pollLast();
                if (NO_LMP_IN_WORLD_ROOT && tLmpRoot!=null && tLmpRoot==mWorldRoot) {mLmpRoots.addLast(tLmpRoot); tLmpRoot = null;}
            }
            mLmpRoot = tLmpRoot;
            try {
                // 根据输入发送创建一个 path 的任务
                mWorldComm.sendB(aStart==null ? PATH_INIT : PATH_FROM, mLmpRoot, JOB_TYPE);
                // 无论怎样都先发送种子
                mWorldComm.sendL(aSeed, mLmpRoot, SEED);
                if (aStart != null) {
                    // from 需要发送整个 Lmpdat
                    Lmpdat tStart = (aStart instanceof Lmpdat) ? (Lmpdat)aStart : Lmpdat.fromAtomData(aStart);
                    Lmpdat.send(tStart, mLmpRoot, mWorldComm);
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
                mWorldComm.sendB(PATH_NEXT, mLmpRoot, JOB_TYPE);
                // 之后获取 Lmpdat 即可
                Lmpdat tNext = Lmpdat.recv(mLmpRoot, mWorldComm);
                // 接收任务完成信息
                mWorldComm.recv(mLmpRoot, PATH_NEXT_FINISHED);
                return tNext;
            } catch (MPIException e) {
                throw new RuntimeException(e);
            }
        }
        @Override public double timeConsumed() {
            if (mLmpRoot < 0) throw new RuntimeException("This RemotePathIterator is dead");
            try {
                // 发送获取时间任务
                mWorldComm.sendB(PATH_TIME, mLmpRoot, JOB_TYPE);
                // 之后获取时间即可
                double tTimeConsumed = mWorldComm.recvD(mLmpRoot, TIME_CONSUMED);
                // 接收任务完成信息
                mWorldComm.recv(mLmpRoot, PATH_TIME_FINISHED);
                return tTimeConsumed;
            } catch (MPIException e) {
                throw new RuntimeException(e);
            }
        }
        @Override public double lambda() {
            if (mLmpRoot < 0) throw new RuntimeException("This RemotePathIterator is dead");
            try {
                // 发送获取 lambda 任务
                mWorldComm.sendB(PATH_LAMBDA, mLmpRoot, JOB_TYPE);
                // 之后获取 lambda
                double tLambda = mWorldComm.recvD(mLmpRoot, LAMBDA);
                // 接收任务完成信息
                mWorldComm.recv(mLmpRoot, PATH_LAMBDA_FINISHED);
                return tLambda;
            } catch (MPIException e) {
                throw new RuntimeException(e);
            }
        }
        /** 完整路径永远都有 next */
        @Override public boolean hasNext() {return true;}
        
        /** 关闭时归还 mLmpRoot */
        @Override public void shutdown() {
            if (mLmpRoot >= 0) {
                try {
                    mWorldComm.sendB(PATH_SHUTDOWN, mLmpRoot, JOB_TYPE);
                    mWorldComm.recv(mLmpRoot, PATH_SHUTDOWN_FINISHED);
                } catch (MPIException e) {
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
                    mWorldComm.sendB(SHUTDOWN, tLmpRoot, JOB_TYPE);
                    mWorldComm.recv(tLmpRoot, SHUTDOWN_FINISHED);
                } catch (MPIException e) {
                    e.printStackTrace(System.err);
                }
            }
            mLmpRoots.clear();
        }
        // 由于是 copy 的，可以并且应该直接 shutdown，注意线程安全的问题
        synchronized (mLmpComm) {mLmpComm.shutdown();} // 这样会存在重复关闭的问题，不过不重要就是
    }
}
