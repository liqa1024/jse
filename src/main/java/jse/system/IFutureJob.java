package jse.system;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static jse.code.CS.SYNC_SLEEP_TIME_2;

/**
 * 为提交任务的 Future 提供一个统一的接口，可以获取到更多信息
 * @author liqa
 */
@SuppressWarnings("BusyWait")
public interface IFutureJob extends Future<Integer> {
    enum StateType {
          QUEUING
        , RUNNING
        , DONE
        , ELSE
    }
    StateType state();
    int jobID();
    
    
    @ApiStatus.Internal int getExitValue_();
    boolean cancel(boolean mayInterruptIfRunning);
    
    /** 简单实现，直接暴力 while 等待，注意不能在等待过程中加锁，会造成死锁 */
    @Override default Integer get() throws InterruptedException {
        while (!isDone()) Thread.sleep(SYNC_SLEEP_TIME_2);
        if (isCancelled()) throw new CancellationException();
        return getExitValue_();
    }
    @Override default Integer get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, TimeoutException {
        long tic = System.nanoTime();
        while (!isDone()) {
            Thread.sleep(SYNC_SLEEP_TIME_2);
            if (System.nanoTime()-tic >= unit.toNanos(timeout)) throw new TimeoutException();
        }
        if (isCancelled()) throw new CancellationException();
        return getExitValue_();
    }
}
