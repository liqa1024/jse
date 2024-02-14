package jse.jobs;


import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * @author liqa
 * <p> 通用的任务池，使用可以独立提交任务和获取结果的写法 </p>
 * <p> 提交任务：putJob(key, args...) </p>
 * <p> 获取结果：getResult(key) </p>
 * <p> 获取 Future：getJob(key) </p>
 * <p> 不进行实现因此不强制要求采用这些写法，因此原本的 SystemExecutor
 * 也不需要扩展功能（毕竟错误码基本都不需要，长时任务下错误码也不好获取）</p>
 */
public interface IJobPool extends IAutoShutdown {
    /** 这些现在也是 IHasJobPool 的东西 */
    void waitUntilDone() throws InterruptedException;
    int jobNumber();
    /** 正常关闭的接口 */
    void shutdown();
    
    @VisibleForTesting default int njobs() {return jobNumber();}
    @Deprecated default int nJobs() {return jobNumber();}
}
