package com.guan.parallel;

import java.util.concurrent.*;

/**
 * @author liqa
 * <p> 类似 Executors 一样的类直接获取 IExecutorEX 线程池 </p>
 */
public class ExecutorsEX {
    
    private abstract static class WrappedExecutorEX implements IExecutorEX {
        private final ThreadPoolExecutor mPool;
        public WrappedExecutorEX(ThreadPoolExecutor aPool) {mPool = aPool;}
        
        
        @Override public void execute(Runnable aRun) {mPool.execute(aRun);}
        @Override public Future<?> submit(Runnable aRun) {return mPool.submit(aRun);}
        @Override public <T> Future<T> submit(Callable<T> aCall) {return mPool.submit(aCall);}
        @Override public void shutdown() {mPool.shutdown();}
        @Override public void shutdownNow() {mPool.shutdownNow();}
        @Override public boolean isShutdown() {return mPool.isShutdown();}
        @Override public boolean isTerminated() {return mPool.isTerminated();}
        @SuppressWarnings("ResultOfMethodCallIgnored") @Override public void awaitTermination() throws InterruptedException {mPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);}
        @SuppressWarnings("BusyWait") @Override public void waitUntilDone() throws InterruptedException {while (mPool.getActiveCount() > 0 || mPool.getQueue().size() > 0) Thread.sleep(100);}
        @Override public int nTasks() {return mPool.getActiveCount() + mPool.getQueue().size();}
        @Deprecated @Override public int getTaskNumber() {return nTasks();}
    }
    
    
    public static IExecutorEX newFixedThreadPool(final int nThreads) {
        return new WrappedExecutorEX(new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>())) {
            @Override public int nThreads() {return nThreads;}
        };
    }
    public static IExecutorEX newSingleThreadExecutor() {
        return new WrappedExecutorEX(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>())) {
            @Override public int nThreads() {return 1;}
        };
    }
}
