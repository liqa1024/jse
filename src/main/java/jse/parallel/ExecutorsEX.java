package jse.parallel;

import java.util.concurrent.*;

import static jse.code.CS.SYNC_SLEEP_TIME;

/**
 * @author liqa
 * <p> 类似 Executors 一样的类直接获取 IExecutorEX 线程池 </p>
 */
public class ExecutorsEX {
    protected abstract static class AbstractExecutorEX implements IExecutorEX {
        private final ThreadPoolExecutor mPool;
        protected AbstractExecutorEX(ThreadPoolExecutor aPool) {mPool = aPool;}
        
        @Override public void execute(Runnable aRun) {mPool.execute(aRun);}
        @Override public Future<?> submit(Runnable aRun) {return mPool.submit(aRun);}
        @Override public <T> Future<T> submit(Callable<T> aCall) {return mPool.submit(aCall);}
        @Override public void shutdown() {mPool.shutdown();}
        @Override public void shutdownNow() {mPool.shutdownNow();}
        @Override public boolean isShutdown() {return mPool.isShutdown();}
        @Override public boolean isTerminated() {return mPool.isTerminated();}
        @SuppressWarnings("ResultOfMethodCallIgnored") @Override public void awaitTermination() throws InterruptedException {mPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);}
        @SuppressWarnings("BusyWait") @Override public void waitUntilDone() throws InterruptedException {while (mPool.getActiveCount() > 0 || !mPool.getQueue().isEmpty()) Thread.sleep(SYNC_SLEEP_TIME);}
        @Override public int nJobs() {return mPool.getActiveCount() + mPool.getQueue().size();}
    }
    
    
    public static IExecutorEX newFixedThreadPool(final int nThreads) {
        return new AbstractExecutorEX(new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>())) {
            @Override public int nThreads() {return nThreads;}
        };
    }
    public static IExecutorEX newSingleThreadExecutor() {
        return new AbstractExecutorEX(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>())) {
            @Override public int nThreads() {return 1;}
        };
    }
}
