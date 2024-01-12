package jtool.jobs;

import jtool.code.UT;
import jtool.io.ILoader;
import jtool.parallel.IShutdownable;

import java.util.*;
import java.util.function.Supplier;

import static jtool.code.CS.FILE_SYSTEM_SLEEP_TIME;
import static jtool.code.CS.WORKING_DIR;

/**
 * @author liqa
 * <p> 长时任务的管理器，可以规范化超长时间的，可以中断的，任务的写法 </p>
 */
public class LongTimeJobManager<T extends ILongTimeJobPool> implements IShutdownable {
    private final String mStepFile;
    private final String mJobPoolFile;
    private final String mShutdownFile;
    
    private final List<Supplier<T>>   mJobPoolList;
    private final List<IInputTask<T>> mJobsDoneList;
    private final List<Runnable>      mConnectorList;
    
    private final ILoader<T> mLongTimeJobPoolLoader;
    private final int mForceStep;
    private final boolean mWaitUntilDone;
    
    /** 需要给定一个加载 TimeJobSupplier 的反序列化器 */
    public LongTimeJobManager(String aUniqueName, ILoader<T> aLongTimeJobPoolLoader) {this(aUniqueName, aLongTimeJobPoolLoader, false);}
    public LongTimeJobManager(String aUniqueName, ILoader<T> aLongTimeJobPoolLoader, boolean aWaitUntilDone) {this(aUniqueName, aLongTimeJobPoolLoader, aWaitUntilDone, -1);}
    public LongTimeJobManager(String aUniqueName, ILoader<T> aLongTimeJobPoolLoader, boolean aWaitUntilDone, int aForceStep) {
        String tWorkingDir = WORKING_DIR.replaceAll("%n", "LTJM@" + aUniqueName);
        mStepFile = tWorkingDir +"step";
        mJobPoolFile = tWorkingDir + "jobpool";
        mShutdownFile = tWorkingDir + "shutdown";
        
        mJobPoolList = new ArrayList<>();
        mJobsDoneList = new ArrayList<>();
        mConnectorList = new ArrayList<>();
        
        mLongTimeJobPoolLoader = aLongTimeJobPoolLoader;
        mForceStep = aForceStep;
        mWaitUntilDone = aWaitUntilDone;
    }
    
    @FunctionalInterface public interface IInputTask<V> {void run(V in);}
    
    /** 使用这个结构让使用更加规范 */
    private final InitJobPool  INIT_JOB_POOL  = new InitJobPool();
    private final WhenJobsDone WHEN_JOBS_DONE = new WhenJobsDone();
    private final Connector    CONNECTOR      = new Connector();
    protected class InitJobPool {
        public WhenJobsDone initJobPool(Supplier<T> aJobPool) {
            mJobPoolList.add(aJobPool);
            return WHEN_JOBS_DONE;
        }
    }
    protected class WhenJobsDone {
        public Connector whenJobsDone(IInputTask<T> aJobDone) {
            mJobsDoneList.add(aJobDone);
            return CONNECTOR;
        }
    }
    protected class Connector {
        public InitJobPool then(Runnable aThenDo) {
            mConnectorList.add(aThenDo);
            return INIT_JOB_POOL;
        }
        /** 结束组装，并且开始任务 */
        @SuppressWarnings({"BusyWait", "rawtypes"})
        public void finish(Runnable aFinishDo) throws Exception {
            // 检测记录工作进度的文件是否存在，如果不存在说明需要从头开始
            int tStep;
            if (mForceStep >= 0) {
                tStep = mForceStep;
            } else
            if (UT.IO.isFile(mStepFile)) {
                tStep = Integer.parseInt(UT.IO.readAllLines(mStepFile).get(0));
            } else {
                tStep = 0;
            }
            if (tStep == mConnectorList.size()) {
                // 步骤是最终步，执行最终的任务，并删除文件
                aFinishDo.run();
                UT.IO.delete(mStepFile);
                return;
            }
            // 否则执行此步骤的任务
            mConnectorList.get(tStep).run();
            // 查看是否有存储 JobPool 的序列化的文件，如果有则不需要获取而是直接反序列化
            T tJobPool;
            if (UT.IO.isFile(mJobPoolFile)) {
                tJobPool = mLongTimeJobPoolLoader.load(UT.IO.json2map(mJobPoolFile));
            } else {
                // 否则则通过 supplier 获取
                tJobPool = mJobPoolList.get(tStep).get();
            }
            // 等待直到建议终止或者任务完成
            int idx = 0;
            while (true) {
                // 任务完成则可以删除旧的 json 文件，并且执行后续操作
                if (tJobPool.nJobs() == 0) {
                    // 为了避免一些问题，这里需要先删除旧的 json 文件
                    UT.IO.delete(mJobPoolFile);
                    // 执行后续操作
                    mJobsDoneList.get(tStep).run(tJobPool);
                    // 增加步骤，更新步骤文件
                    ++tStep;
                    UT.IO.write(mStepFile, String.valueOf(tStep));
                    // 正常退出则会执行 shutdown
                    tJobPool.shutdown();
                    // 直接退出
                    return;
                }
                // 建议终止则进行保存操作，然后终止监控
                if (mDead || (!mWaitUntilDone && tJobPool.killRecommended())) {
                    Map rJobPoolArgs = new LinkedHashMap();
                    tJobPool.save(rJobPoolArgs);
                    UT.IO.map2json(rJobPoolArgs, mJobPoolFile);
                    // 杀死则会执行 kill
                    tJobPool.kill();
                    // 直接退出
                    return;
                }
                // 每秒检测一次 shutdown 文件看是否手动关闭
                if (idx % 10 == 9) {
                    if (!UT.IO.isFile(mShutdownFile)) {
                        UT.IO.write(mShutdownFile, "0");
                    } else {
                        int tValue = Integer.parseInt(UT.IO.readAllLines(mShutdownFile).get(0));
                        if (tValue != 0) {
                            UT.IO.delete(mShutdownFile); // 关闭前记得先删掉这个文件
                            shutdown();
                        }
                    }
                }
                Thread.sleep(FILE_SYSTEM_SLEEP_TIME);
                ++idx;
            }
        }
    }
    
    /** 开始组装 */
    public InitJobPool init(Runnable aInitDo) {
        mConnectorList.add(aInitDo);
        return INIT_JOB_POOL;
    }
    
    
    /** 外部用来手动关闭这个 Manager 的接口 */
    private volatile boolean mDead = false;
    public boolean isShutdown() {return mDead;}
    @Override public void shutdown() {
        if (isShutdown()) return;
        mDead = true;
    }
}
