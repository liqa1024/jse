package com.guan.parallel;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * @author liqa
 * <p> 用来统一管理包含 ThreadPool 的类 </p>
 */
public interface IHasThreadPool extends AutoCloseable {
    void shutdown();
    void shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    void awaitTermination() throws InterruptedException;
    
    void waitUntilDone() throws InterruptedException;
    int nJobs();
    int nThreads();
    
    @VisibleForTesting default int getTaskNumber() {return nJobs();}
    
    /** AutoClosable stuffs */
    @VisibleForTesting default void close() {shutdown();}
}
