package jse.parallel;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static jse.code.Conf.PARFOR_NO_COMPETITIVE;

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
public final class ParforThreadPool extends AbstractThreadPool<IExecutorEX> {
    private final Lock @Nullable[] mLocks; // 用来在并行时给每个线程独立加锁，保证每个线程独立写入的操作的可见性
    private final boolean mNoCompetitive;
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {mDead = true; super.shutdown();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    /** ParforThreadPool close 时不需要 awaitTermination */
    @ApiStatus.Internal @Override public void close() {shutdown();}
    
    public ParforThreadPool(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, boolean aNoCompetitive) {
        super(aThreadNum==1 ? SERIAL_EXECUTOR : newPool(aThreadNum));
        if (aThreadNum == 1) {
            mLocks = null;
        } else {
            mLocks = new Lock[aThreadNum];
            for (int i = 0; i < aThreadNum; ++i) mLocks[i] = new ReentrantLock();
        }
        mNoCompetitive = aNoCompetitive;
    }
    public ParforThreadPool(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {this(aThreadNum, PARFOR_NO_COMPETITIVE);}
    
    
    /**
     * @author liqa
     * <p> 类似 {@code for (int i = 0; i < aSize; ++i)},
     * 现在不再支持设置具体的 start 和 end </p>
     * <p> 支持每个线程写入到独立的内存而不需要额外加锁 </p>
     */
    public void parfor(final int aSize, final Runnable          aTask      ) {parfor(aSize, (i, threadID) -> aTask.run());}
    public void parfor(final int aSize, final IParforTask       aTask      ) {parfor(aSize, (i, threadID) -> aTask.run(i));}
    public void parfor(final int aSize, final IParforTaskWithID aTaskWithID) {parfor(aSize, null, null, aTaskWithID);}
    public void parfor(final int aSize, final @Nullable ITaskWithID aInitDo, final @Nullable ITaskWithID aFinalDo, final IParforTaskWithID aTaskWithID) {
        try {parforWithException(aSize, aInitDo==null ? null : aInitDo::run, aFinalDo==null ? null : aFinalDo::run, aTaskWithID::run);}
        catch (Exception e) {throw new RuntimeException(e);}
    }
    @ApiStatus.Experimental
    public void parforWithException(final int aSize, final @Nullable ITaskWithIDAndException aInitDo, final @Nullable ITaskWithIDAndException aFinalDo, final IParforTaskWithIDAndException aTaskWithIDAndException) throws Exception {
        if (mDead) throw new RuntimeException("This ParforThreadPool is dead");
        if (aSize <= 0) return;
        // 串行的情况
        if (threadNumber() <= 1) {
            if (aInitDo != null) aInitDo.run(0);
            for (int i = 0; i < aSize; ++i) aTaskWithIDAndException.run(i, 0);
            if (aFinalDo != null) aFinalDo.run(0);
        }
        // 并行的情况，现在默认不进行分组，使用竞争获取任务的思路来获取任务，保证实际创建的线程在 parfor 任务中不会提前结束，并且可控
        else synchronized (this) {
            int tThreadNum = threadNumber();
            // 获取错误，保留执行中的错误，并在任意一个线程发生错误时中断
            final AtomicReference<Exception> tException = new AtomicReference<>(null);
            final CountDownLatch tLatch = new CountDownLatch(tThreadNum);
            if (mNoCompetitive) {
                // 非竞争获取任务
                for (int id = 0; id < tThreadNum; ++id) {
                    final int fId = id;
                    pool().execute(() -> {
                        assert mLocks != null;
                        mLocks[fId].lock(); // 加锁在结束后进行数据同步
                        try {
                            if (aInitDo != null) aInitDo.run(fId);
                            for (int i = fId; i < aSize; i += tThreadNum) {
                                aTaskWithIDAndException.run(i, fId);
                            }
                            if (aFinalDo != null) aFinalDo.run(fId);
                        } catch (Exception e) {
                            tException.set(e);
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
                        try {
                            if (aInitDo != null) aInitDo.run(fId);
                            while (true) {
                                if (tException.get() != null) break;
                                int i, ipp;
                                // 采用 CAS 自旋锁来避免同步（虽然简单测试下来都没有什么性能损失）
                                do {
                                    i = currentIdx.get();
                                    ipp = i + 1;
                                } while (!currentIdx.compareAndSet(i, ipp));
                                if (i >= aSize) break;
                                aTaskWithIDAndException.run(i, fId);
                            }
                            if (aFinalDo != null) aFinalDo.run(fId);
                        } catch (Exception e) {
                            tException.set(e);
                        }
                        // 认为不会在其他地方抛出错误，因此不做额外的 try-finally 操作
                        mLocks[fId].unlock();
                        tLatch.countDown();
                    });
                }
            }
            // 使用 CountDownLatch 来等待线程池工作完成
            try {tLatch.await();}
            catch (InterruptedException e) {if (tException.get()==null) tException.set(e);}
            // 如果有错误需要抛出
            if (tException.get() != null) throw tException.get();
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
    public void parwhile(final IParwhileChecker aChecker, final IParwhileTaskWithID aTaskWithID) {parwhile(aChecker, null, null, aTaskWithID);}
    public void parwhile(final IParwhileChecker aChecker, final @Nullable ITaskWithID aInitDo, final @Nullable ITaskWithID aFinalDo, final IParwhileTaskWithID aTaskWithID) {
        if (mDead) throw new RuntimeException("This ParforThreadPool is dead");
        // 特殊情况直接退出
        if (!aChecker.noBreak()) return;
        // 串行的情况
        if (threadNumber() <= 1) {
            if (aInitDo != null) aInitDo.run(0);
            while (aChecker.noBreak()) aTaskWithID.run(0);
            if (aFinalDo != null) aFinalDo.run(0);
        }
        // 并行的情况，现在默认不进行分组，使用竞争获取任务的思路来获取任务，保证实际创建的线程在 parwhile 任务中不会提前结束，并且可控
        else synchronized (this) {
            int tThreadNum = threadNumber();
            // 获取错误，保留执行中的错误，并在任意一个线程发生错误时中断
            final AtomicReference<Throwable> tThrowable = new AtomicReference<>(null);
            final CountDownLatch tLatch = new CountDownLatch(tThreadNum);
            // parwhile 不存在不竞争的情况
            for (int id = 0; id < tThreadNum; ++id) {
                final int fId = id;
                pool().execute(() -> {
                    assert mLocks != null;
                    mLocks[fId].lock(); // 加锁在结束后进行数据同步
                    if (aInitDo != null) aInitDo.run(fId);
                    while (true) {
                        if (tThrowable.get() != null) break;
                        try {
                            synchronized (aChecker) {if (!aChecker.noBreak()) break;}
                            aTaskWithID.run(fId);
                        } catch (Throwable e) {
                            tThrowable.set(e); break;
                        }
                    }
                    if (aFinalDo != null) aFinalDo.run(fId);
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
    @FunctionalInterface public interface ITaskWithID {void run(int threadID);}
    @FunctionalInterface public interface IParforTask {void run(int i);}
    @FunctionalInterface public interface IParforTaskWithID {void run(int i, int threadID);}
    @FunctionalInterface public interface ITaskWithIDAndException {void run(int threadID) throws Exception;}
    @FunctionalInterface public interface IParforTaskWithIDAndException {void run(int i, int threadID) throws Exception;}
    @FunctionalInterface public interface IParwhileChecker {boolean noBreak();}
    @FunctionalInterface public interface IParwhileTaskWithID {void run(int threadID);}
}
