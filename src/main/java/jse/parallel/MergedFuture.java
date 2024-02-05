package jse.parallel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MergedFuture<T, F extends Future<? extends T>> implements Future<List<T>> {
    protected final Iterable<F> mFutures;
    public MergedFuture(Iterable<F> aFutures) {mFutures = aFutures;}
    
    @Override public boolean cancel(boolean mayInterruptIfRunning) {
        boolean tOut = true;
        for (Future<? extends T> tFutureJob : mFutures) tOut &= tFutureJob.cancel(mayInterruptIfRunning);
        return tOut;
    }
    @Override public boolean isCancelled() {
        for (Future<? extends T> tFuture : mFutures) if (!tFuture.isCancelled()) return false;
        return true;
    }
    @Override public boolean isDone() {
        for (Future<? extends T> tFuture : mFutures) if (!tFuture.isDone()) return false;
        return true;
    }
    @Override public List<T> get() throws InterruptedException, ExecutionException {
        List<T> tOut = new ArrayList<>();
        for (Future<? extends T> tFuture : mFutures) tOut.add(tFuture.get());
        return tOut;
    }
    @Override public List<T> get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<T> tOut = new ArrayList<>();
        long tic = System.nanoTime();
        long tRestTime = unit.toNanos(timeout);
        for (Future<? extends T> tFuture : mFutures) {
            tOut.add(tFuture.get(tRestTime, TimeUnit.NANOSECONDS));
            long toc = System.nanoTime();
            tRestTime -= toc - tic;
            tic = toc;
            if (tRestTime <= 0) throw new TimeoutException();
        }
        return tOut;
    }
}
