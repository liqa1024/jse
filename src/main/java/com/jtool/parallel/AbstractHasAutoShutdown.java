package com.jtool.parallel;


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
    
    /** stuff to override */
    protected void shutdown_() {/**/}
    protected abstract void shutdownInternal_();
}
