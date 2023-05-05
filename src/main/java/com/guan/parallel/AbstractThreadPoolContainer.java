package com.guan.parallel;

import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 一般的默认实现，这里仅支持 IExecutorEX，统一其他类的使用 </p>
 */
public abstract class AbstractThreadPoolContainer<TP extends IThreadPoolContainer> implements IThreadPoolContainer {
    protected @Nullable TP mPool;
    protected AbstractThreadPoolContainer() {mPool = null;}
    protected AbstractThreadPoolContainer(@Nullable TP aPool) {mPool = aPool;}
    
    @Override public void shutdown() {if (mPool!=null) mPool.shutdown();}
    @Override public void shutdownNow() {if (mPool!=null) mPool.shutdownNow();}
    @Override public boolean isShutdown() {return mPool!=null && mPool.isShutdown();}
    @Override public boolean isTerminated() {return mPool!=null && mPool.isTerminated();}
    @Override public void awaitTermination() throws InterruptedException {if (mPool!=null) mPool.awaitTermination();}
    
    @Override public void waitUntilDone() throws InterruptedException {if (mPool!=null) mPool.waitUntilDone();}
    @Override public int nTasks() {return mPool==null ? 0 : mPool.nTasks();}
    @Override public int nThreads() {return mPool==null ? 1 : mPool.nThreads();}
    
    @Deprecated @Override public int getTaskNumber() {return nTasks();}
}
