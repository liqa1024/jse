package jse.parallel;

import java.util.concurrent.*;

import static jse.code.CS.SYNC_SLEEP_TIME;
import static jse.code.CS.SYNC_TIMEOUT;

/**
 * 类似 Executors 一样的类直接获取 IExecutorEX 线程池
 * @author liqa
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
        @Override public int njobs() {return mPool.getActiveCount() + mPool.getQueue().size();}
    }
    
    
    public static IExecutorEX newFixedThreadPool(final int nThreads) {
        ThreadPoolExecutor tPool = new ThreadPoolExecutor(nThreads, nThreads, SYNC_TIMEOUT, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        tPool.allowCoreThreadTimeOut(true);
        return new AbstractExecutorEX(tPool) {
            @Override public int nthreads() {return nThreads;}
        };
    }
    /** 串行线程池的默认实现，设定上不能关闭，从而可以实现复用 */
    public final static IExecutorEX SERIAL_EXECUTOR = new IExecutorEX() {
        @Override public void execute(Runnable aRun) {aRun.run();}
        @Override public Future<?> submit(Runnable aRun) {
            if (aRun == null) throw new NullPointerException();
            RunnableFuture<Void> tFTask = new FutureTask<>(aRun, null);
            execute(tFTask); return tFTask;
        }
        @Override public <T> Future<T> submit(Callable<T> aCall) {
            if (aCall == null) throw new NullPointerException();
            RunnableFuture<T> tFTask = new FutureTask<>(aCall);
            execute(tFTask); return tFTask;
        }
        @Override public void shutdown() {/**/}
        @Override public void shutdownNow() {/**/}
        @Override public boolean isShutdown() {return false;}
        @Override public boolean isTerminated() {return false;}
        @Override public void awaitTermination() {/**/}
        @Override public void waitUntilDone() {/**/}
        @Override public int njobs() {return 0;}
        @Override public int nthreads() {return 1;}
    };
}
