package com.guan.parallel;

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
    int nTasks();
    int nThreads();
    
    @Deprecated default int getTaskNumber() {return nTasks();}
    
    /** AutoClosable stuffs */
    @Deprecated default void close() {shutdown();}
}
