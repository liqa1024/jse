package com.jtool.parallel;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author liqa
 * <p> 可以像 matlab 的 parfor 一样直接使用的线程池 </p>
 * <p> 自动识别单线程的情况来直接进行串行 </p>
 * <p> 当然还是需要自己注意内部的线程安全问题 </p>
 * <p> 支持每个线程写入到独立的内存而不需要额外加锁 </p>
 * <p> 注意：此类线程安全（包括不同实例间以及多个线程同时访问同一个实例）。
 * 当线程数大于 1 时同一个实例的操作加锁保证线程安全；
 * 当线程数为 1 时同一个实例的操作不会加锁 </p>
 */
public class ParforThreadPool extends AbstractHasThreadPool<IExecutorEX> {
    private final ThreadLocal<Integer> mThreadID = ThreadLocal.withInitial(()->null); // 使用 ThreadLocal 存储每个线程对应的 id，直接避免线程安全的问题
    private final Lock @Nullable[] mLocks; // 用来在并行时给每个线程独立加锁，保证每个线程独立写入的操作的可见性
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {mDead = true; super.shutdown();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    
    public ParforThreadPool(int aThreadNum) {
        super(aThreadNum<=1 ? SERIAL_EXECUTOR : newPool(aThreadNum));
        int tThreadNum = Math.max(aThreadNum, 1);
        if (tThreadNum == 1) mLocks = null;
        else {
            mLocks = new Lock[tThreadNum];
            for (int i = 0; i < tThreadNum; ++i) mLocks[i] = new ReentrantLock();
        }
    }
    // 提供一些额外接口
    public final int currentThreadID() {return mThreadID.get();}
    
    /**
     * @author liqa
     * <p> 类似 for (int i = 0; i < aSize; ++i),
     * 现在不再支持设置具体的 start 和 end </p>
     * <p> 支持每个线程写入到独立的内存而不需要额外加锁 </p>
     */
    public void parfor(final int aSize, final Runnable          aTask      ) {parfor(aSize, (i, threadID) -> aTask.run());}
    public void parfor(final int aSize, final IParforTask       aTask      ) {parfor(aSize, (i, threadID) -> aTask.run(i));}
    public void parfor(final int aSize, final IParforTaskWithID aTaskWithID) {
        if (mDead) throw new RuntimeException("This ParforThreadPool is dead");
        if (aSize <= 0) return;
        // 串行的情况
        if (nThreads() <= 1) {
            for (int i = 0; i < aSize; ++i) aTaskWithID.run(i, 0);
        }
        // 并行的情况，现在默认不进行分组，使用竞争获取任务的思路来获取任务，保证实际创建的线程在 parfor 任务中不会提前结束，并且可控
        else synchronized (this) {
            assert mLocks != null;
            int tThreadNum = nThreads();
            // 获取错误，保留执行中的错误，并在任意一个线程发生错误时中断
            final AtomicReference<Throwable> tThrowable = new AtomicReference<>(null);
            // 竞争获取任务
            final int[] currentIdx = new int[] {0};
            final CountDownLatch tLatch = new CountDownLatch(tThreadNum);
            for (int id = 0; id < tThreadNum; ++id) {
                final int fId = id;
                pool().execute(() -> {
                    mThreadID.set(fId); // 注册 id
                    mLocks[fId].lock(); // 加锁在结束后进行数据同步
                    while (true) {
                        if (tThrowable.get() != null) break;
                        int i;
                        synchronized (currentIdx) {
                            i = currentIdx[0];
                            ++currentIdx[0];
                        }
                        if (i >= aSize) break;
                        try {aTaskWithID.run(i, fId);}
                        catch (Throwable e) {tThrowable.set(e); break;}
                    }
                    mLocks[fId].unlock();
                    tLatch.countDown();
                });
            }
            try {tLatch.await();} catch (InterruptedException ignored) {} // 使用 CountDownLatch 来等待线程池工作完成
            // 如果有错误需要抛出
            if (tThrowable.get() != null) throw new RuntimeException(tThrowable.get());
        }
    }
    
    /**
     * @author liqa
     * <p> 类似 while (aChecker.noBreak()) </p>
     * <p> 内部已经对 aChecker 进行了加锁，因此不再需要对其再次加锁 </p>
     * <p> 由于并行的特性不能保证 while 会立刻停止 </p>
     * <p> 支持每个线程写入到独立的内存而不需要额外加锁 </p>
     */
    public void parwhile(final IParwhileChecker aChecker, final Runnable            aTask      ) {parwhile(aChecker, (threadID) -> aTask.run());}
    public void parwhile(final IParwhileChecker aChecker, final IParwhileTaskWithID aTaskWithID) {
        if (mDead) throw new RuntimeException("This ParforThreadPool is dead");
        // 串行的情况
        if (nThreads() <= 1) {
            while (aChecker.noBreak()) aTaskWithID.run(0);
        }
        // 并行的情况，现在默认不进行分组，使用竞争获取任务的思路来获取任务，保证实际创建的线程在 parwhile 任务中不会提前结束，并且可控
        else synchronized (this) {
            assert mLocks != null;
            int tThreadNum = nThreads();
            // 获取错误，保留执行中的错误，并在任意一个线程发生错误时中断
            final AtomicReference<Throwable> tThrowable = new AtomicReference<>(null);
            final CountDownLatch tLatch = new CountDownLatch(tThreadNum);
            for (int id = 0; id < tThreadNum; ++id) {
                final int fId = id;
                pool().execute(() -> {
                    mThreadID.set(fId); // 注册 id
                    mLocks[fId].lock(); // 加锁在结束后进行数据同步
                    while (true) {
                        if (tThrowable.get() != null) break;
                        synchronized (aChecker) {if (!aChecker.noBreak()) break;}
                        try {aTaskWithID.run(fId);}
                        catch (Throwable e) {tThrowable.set(e); break;}
                    }
                    mLocks[fId].unlock();
                    tLatch.countDown();
                });
            }
            try {tLatch.await();} catch (InterruptedException ignored) {} // 使用 CountDownLatch 来等待线程池工作完成
            // 如果有错误需要抛出
            if (tThrowable.get() != null) throw new RuntimeException(tThrowable.get());
        }
    }
    
    
    /**
     * @author liqa
     * <p> 重写此类实现 parfor 的使用 </p>
     */
    @FunctionalInterface public interface IParforTask {void run(int i);}
    @FunctionalInterface public interface IParforTaskWithID {void run(int i, int threadID);}
    @FunctionalInterface public interface IParwhileChecker {boolean noBreak();}
    @FunctionalInterface public interface IParwhileTaskWithID {void run(int threadID);}
}
