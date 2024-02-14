package jse.parallel;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * @author liqa
 * <p> 用来统一管理包含 ThreadPool 的类 </p>
 */
public interface IThreadPool extends IAutoShutdown {
    void shutdown();
    void shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    void awaitTermination() throws InterruptedException;
    
    void waitUntilDone() throws InterruptedException;
    int jobNumber();
    int threadNumber();
    
    @VisibleForTesting default int njobs() {return jobNumber();}
    @VisibleForTesting default int nthreads() {return threadNumber();}
    
    /** 注意线程池的 try-with-resources 自动关闭还需要等待线程池完全关闭（注意需要忽略 InterruptedException 让后续可能的资源正常释放） */
    @ApiStatus.Internal @Override default void close() {shutdown(); try {awaitTermination();} catch (InterruptedException ignored) {}}
    
    
    @Deprecated default int nJobs() {return jobNumber();}
    @Deprecated default int nThreads() {return threadNumber();}
    @Deprecated default int getTaskNumber() {return jobNumber();}
}
