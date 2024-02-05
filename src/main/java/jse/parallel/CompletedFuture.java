package jse.parallel;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * 已经完成的 Future，用于在获取 Future 的一些方法在还没提交任务之前就退出时返回，但是又不希望返回 null 的情况
 * @author liqa
 */
public final class CompletedFuture<T> implements Future<T> {
    private final T mOut;
    public CompletedFuture(T aOut) {mOut = aOut;}
    
    @Override public boolean cancel(boolean mayInterruptIfRunning) {return false;}
    @Override public boolean isCancelled() {return false;}
    @Override public boolean isDone() {return true;}
    @Override public T get() {return mOut;}
    @Override public T get(long timeout, @NotNull TimeUnit unit) {return mOut;}
}
