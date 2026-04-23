package jse.parallel;

/**
 * 用来统一管理包含 ThreadPool 或类似功能的类
 * <p>
 * 除了规范接口外，还提供了针对 {@link AutoCloseable}
 * 的支持，从而可以通过 try-with-resources 自动关闭
 * @author liqa
 */
public interface IThreadPool extends AutoCloseable {
    void shutdown();
    void shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    void awaitTermination() throws InterruptedException;
    
    void waitUntilDone() throws InterruptedException;
    int njobs();
    int nthreads();
    
    /** 注意线程池的 try-with-resources 自动关闭还需要等待线程池完全关闭（注意需要忽略 InterruptedException 让后续可能的资源正常释放） */
    @Override default void close() throws Exception {
        shutdown();
        awaitTermination();
    }
}
