package jse.parallel;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
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
public class ParforThreadPool extends AbstractThreadPool<IExecutorEX> {
    /** 控制 parfor 的模式，在竞争模式下不同线程分配到的任务是不一定的，而关闭后是一定的，有时需要保证重复运行结果一致 */
    @VisibleForTesting public static boolean DEFAULT_NO_COMPETITIVE = false;
    
    private final Lock @Nullable[] mLocks; // 用来在并行时给每个线程独立加锁，保证每个线程独立写入的操作的可见性
    private final boolean mNoCompetitive;
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {mDead = true; super.shutdown();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    
    public ParforThreadPool(int aThreadNum, boolean aNoCompetitive) {
        super(aThreadNum<=1 ? SERIAL_EXECUTOR : newPool(aThreadNum));
        int tThreadNum = Math.max(aThreadNum, 1);
        if (tThreadNum == 1) mLocks = null;
        else {
            mLocks = new Lock[tThreadNum];
            for (int i = 0; i < tThreadNum; ++i) mLocks[i] = new ReentrantLock();
        }
        mNoCompetitive = aNoCompetitive;
    }
    public ParforThreadPool(int aThreadNum) {this(aThreadNum, DEFAULT_NO_COMPETITIVE);}
    
    
    /**
     * @author liqa
     * <p> 类似 {@code for (int i = 0; i < aSize; ++i)},
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
            int tThreadNum = nThreads();
            // 获取错误，保留执行中的错误，并在任意一个线程发生错误时中断
            final AtomicReference<Throwable> tThrowable = new AtomicReference<>(null);
            final CountDownLatch tLatch = new CountDownLatch(tThreadNum);
            if (mNoCompetitive) {
                // 非竞争获取任务
                for (int id = 0; id < tThreadNum; ++id) {
                    final int fId = id;
                    pool().execute(() -> {
                        assert mLocks != null;
                        mLocks[fId].lock(); // 加锁在结束后进行数据同步
                        for (int i = fId; i < aSize; i += tThreadNum) {
                            try {aTaskWithID.run(i, fId);}
                            catch (Throwable e) {tThrowable.set(e); break;}
                        }
                        // 认为不会在其他地方抛出错误，因此不做额外的 try-finally 操作
                        mLocks[fId].unlock();
                        tLatch.countDown();
                    });
                }
            } else {
                // 竞争获取任务
                final AtomicInteger currentIdx = new AtomicInteger(0);
                for (int id = 0; id < tThreadNum; ++id) {
                    final int fId = id;
                    pool().execute(() -> {
                        assert mLocks != null;
                        mLocks[fId].lock(); // 加锁在结束后进行数据同步
                        while (true) {
                            if (tThrowable.get() != null) break;
                            int i, ipp;
                            // 采用 CAS 自旋锁来避免同步（虽然简单测试下来都没有什么性能损失）
                            do {
                                i = currentIdx.get();
                                ipp = i + 1;
                            } while (!currentIdx.compareAndSet(i, ipp));
                            if (i >= aSize) break;
                            try {aTaskWithID.run(i, fId);}
                            catch (Throwable e) {tThrowable.set(e); break;}
                        }
                        // 认为不会在其他地方抛出错误，因此不做额外的 try-finally 操作
                        mLocks[fId].unlock();
                        tLatch.countDown();
                    });
                }
            }
            // 使用 CountDownLatch 来等待线程池工作完成
            try {tLatch.await();}
            catch (InterruptedException e) {if (tThrowable.get()==null) tThrowable.set(e);}
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
        // 特殊情况直接退出
        if (!aChecker.noBreak()) return;
        // 串行的情况
        if (nThreads() <= 1) {
            while (aChecker.noBreak()) aTaskWithID.run(0);
        }
        // 并行的情况，现在默认不进行分组，使用竞争获取任务的思路来获取任务，保证实际创建的线程在 parwhile 任务中不会提前结束，并且可控
        else synchronized (this) {
            int tThreadNum = nThreads();
            // 获取错误，保留执行中的错误，并在任意一个线程发生错误时中断
            final AtomicReference<Throwable> tThrowable = new AtomicReference<>(null);
            final CountDownLatch tLatch = new CountDownLatch(tThreadNum);
            // parwhile 不存在不竞争的情况
            for (int id = 0; id < tThreadNum; ++id) {
                final int fId = id;
                pool().execute(() -> {
                    assert mLocks != null;
                    mLocks[fId].lock(); // 加锁在结束后进行数据同步
                    while (true) {
                        if (tThrowable.get() != null) break;
                        try {
                            synchronized (aChecker) {if (!aChecker.noBreak()) break;}
                            aTaskWithID.run(fId);
                        } catch (Throwable e) {
                            tThrowable.set(e); break;
                        }
                    }
                    // 认为不会在其他地方抛出错误，因此不做额外的 try-finally 操作
                    mLocks[fId].unlock();
                    tLatch.countDown();
                });
            }
            // 使用 CountDownLatch 来等待线程池工作完成
            try {tLatch.await();}
            catch (InterruptedException e) {if (tThrowable.get()==null) tThrowable.set(e);}
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
