package com.guan.parallel;

import java.util.concurrent.*;

/**
 * @author liqa
 * <p> 一般的默认实现，这里仅支持 IExecutorEX，统一其他类的使用 </p>
 */
public abstract class AbstractHasThreadPool<TP extends IHasThreadPool> implements IHasThreadPool {
    /** 提供一个可选的线程池供子类选择 */
    protected static IExecutorEX newPool(int nThreads) {return ExecutorsEX.newFixedThreadPool(Math.max(nThreads, 1));}
    protected static IExecutorEX newSingle() {return ExecutorsEX.newSingleThreadExecutor();}
    /** 串行线程池的默认实现，设定上不能关闭，从而可以实现复用 */
    protected final static IExecutorEX SERIAL_EXECUTOR = new IExecutorEX() {
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
        @Override public int nJobs() {return 0;}
        @Override public int nThreads() {return 1;}
    };
    
    
    /** 子类通过 super 指定需要的线程池 */
    private TP mPool;
    protected AbstractHasThreadPool(TP aPool) {mPool = aPool;}
    protected void setPool(TP aPool) {mPool.shutdown(); mPool = aPool;}
    protected TP pool() {return mPool;}
    
    @Override public void shutdown() {mPool.shutdown();}
    @Override public void shutdownNow() {mPool.shutdownNow();}
    @Override public boolean isShutdown() {return mPool.isShutdown();}
    @Override public boolean isTerminated() {return mPool.isTerminated();}
    @Override public void awaitTermination() throws InterruptedException {mPool.awaitTermination();}
    
    @Override public void waitUntilDone() throws InterruptedException {mPool.waitUntilDone();}
    @Override public int nJobs() {return mPool.nJobs();}
    @Override public int nThreads() {return mPool.nThreads();}
}
