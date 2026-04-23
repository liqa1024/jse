package jse.parallel;


import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Extended ExecutorService for more flexible usage
 * @author liqa
 */
public interface IExecutorEX extends IThreadPool {
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
    int njobs();
    int nthreads();
}
