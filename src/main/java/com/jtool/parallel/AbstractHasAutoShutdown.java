package com.jtool.parallel;


import org.jetbrains.annotations.VisibleForTesting;

/**
 * 内部包含可以关闭的类，提供接口来是否在关闭时同时关闭内部的 IAutoShutdown
 * @author liqa
 */
public abstract class AbstractHasAutoShutdown implements IHasAutoShutdown {
    private boolean mDoNotShutdown = false;
    protected final void setDoNotShutdown_(boolean aDoNotShutdown) {mDoNotShutdown = aDoNotShutdown;}
    @Override public final void shutdown() {
        shutdown_();
        if (!mDoNotShutdown) shutdownInternal_();
    }
    
    /** 注意有些类（如线程池）的 close 逻辑不完全和 shutdown 相同，这里需要专门使用内部的 close */
    @VisibleForTesting @Override public final void close() {
        close_();
        if (!mDoNotShutdown) closeInternal_();
    }
    
    /** stuff to override */
    protected void shutdown_() {/**/}
    protected abstract void shutdownInternal_();
    protected void close_() {shutdown_();}
    protected abstract void closeInternal_();
}
