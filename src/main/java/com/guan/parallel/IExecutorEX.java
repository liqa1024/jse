package com.guan.parallel;


import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> Extended ExecutorService for more flexible usage </p>
 */
public interface IExecutorEX extends IThreadPoolContainer {
    /** ExecutorService stuffs */
    void execute(Runnable aRun);
    Future<?> submit(Runnable aRun);
    <T> Future<T> submit(Callable<T> aCall);
    void shutdown();
    void shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    void awaitTermination() throws InterruptedException;
    
    
    /** Extended stuffs */
    void waitUntilDone() throws InterruptedException;
    int nTasks();
    int nThreads();
    
    @Deprecated int getTaskNumber();
}
