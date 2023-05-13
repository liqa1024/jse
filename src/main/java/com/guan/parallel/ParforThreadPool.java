package com.guan.parallel;

import com.guan.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author liqa
 * <p> 可以像 matlab 的 parfor 一样直接使用的线程池 </p>
 * <p> 自动识别单线程的情况来直接进行串行 </p>
 * <p> 当然还是需要自己注意内部的线程安全问题 </p>
 * <p> 支持每个线程写入到独立的内存而不需要额外加锁 </p>
 * <p> 注意：当线程数大于 1 时此类线程不安全，但不同实例间线程安全；
 * 当线程数为 1 时线程安全，包括多个线程同时访问同一个实例 </p>
 */
public class ParforThreadPool extends AbstractHasThreadPool<IExecutorEX> {
    private final ThreadLocal<Integer> mThreadID = ThreadLocal.withInitial(()->null); // 使用 ThreadLocal 存储每个线程对应的 id，直接避免线程安全的问题
    private int mBatchSize;
    private final Lock @Nullable [] mLocks; // 用来在并行时给每个线程独立加锁，保证每个线程独立写入的操作的可见性
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {mDead = true; super.shutdown();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    
    public ParforThreadPool(int aThreadNum) {this(aThreadNum, 64);}
    public ParforThreadPool(int aThreadNum, int aBatchSize) {
        super(aThreadNum<=1 ? SERIAL_EXECUTOR : newPool(aThreadNum));
        int tThreadNum = Math.max(aThreadNum, 1);
        mBatchSize = Math.max(aBatchSize, 1);
        if (tThreadNum == 1) mLocks = null;
        else {
            mLocks = new Lock[tThreadNum];
            for (int i = 0; i < tThreadNum; ++i) mLocks[i] = new ReentrantLock();
        }
    }
    // 提供一些额外接口
    public final int currentThreadID() {return mThreadID.get();}
    
    /// 参数设置
    public ParforThreadPool setBatchSize(int aBatchSize) {mBatchSize = Math.max(aBatchSize, 1); return this;}
    
    /**
     * @author liqa
     * <p> 类似 for (int i = aStart; i < aEnd; i += aStep),
     * 包含 aStart 不包含 aEnd </p>
     * <p> 支持每个线程写入到独立的内存而不需要额外加锁 </p>
     */
    public void parfor(                       int aSize, final IParforTask       aTask      ) {parfor(0, 1, aSize, aTask);}
    public void parfor(int aStart,            int aEnd , final IParforTask       aTask      ) {parfor(aStart, 1, aEnd, aTask);}
    public void parfor(int aStart, int aStep, int aEnd , final IParforTask       aTask      ) {parfor(aStart, aStep, aEnd, (i, threadID) -> aTask.run(i));}
    public void parfor(                       int aSize, final IParforTaskWithID aTaskWithID) {parfor(0, 1, aSize, aTaskWithID);}
    public void parfor(int aStart,            int aEnd , final IParforTaskWithID aTaskWithID) {parfor(aStart, 1, aEnd, aTaskWithID);}
    public void parfor(int aStart, int aStep, int aEnd , final IParforTaskWithID aTaskWithID) {
        if (mDead) throw new RuntimeException("This ParforThreadPool is dead");
        // 只支持正方向的迭代输入
        aStep = Math.max(aStep, 1);
        aEnd = Math.max(aEnd, aStart);
        parfor_(aStart, aStep, aEnd, aTaskWithID);
    }
    public void parfor_(                                   final int aSize, final IParforTask       aTask      ) {parfor_(0, 1, aSize, aTask);}
    public void parfor_(final int aStart,                  final int aEnd , final IParforTask       aTask      ) {parfor_(aStart, 1, aEnd, aTask);}
    public void parfor_(final int aStart, final int aStep, final int aEnd , final IParforTask       aTask      ) {parfor_(aStart, aStep, aEnd, (i, threadID) -> aTask.run(i));}
    public void parfor_(                                   final int aSize, final IParforTaskWithID aTaskWithID) {parfor_(0, 1, aSize, aTaskWithID);}
    public void parfor_(final int aStart,                  final int aEnd , final IParforTaskWithID aTaskWithID) {parfor_(aStart, 1, aEnd, aTaskWithID);}
    public void parfor_(final int aStart, final int aStep, final int aEnd , final IParforTaskWithID aTaskWithID) {
        if (mDead) throw new RuntimeException("This ParforThreadPool is dead");
        // 串行的情况
        if (nThreads() <= 1) {
            for (int i = aStart; i < aEnd; i += aStep) aTaskWithID.run(i, 0);
        }
        // 并行的情况，将任务分组，使用竞争获取任务的思路来获取任务，保证实际创建的线程在 parfor 任务中不会提前结束，并且可控
        else {
            assert mLocks != null;
            int tThreadNum = nThreads();
            // 计算余下不能按照 mBatchSize 来分配的任务数
            int tRestNum = MathEX.Code.divup(aEnd - aStart, aStep) % (mBatchSize * tThreadNum);
            final int tRestBatchSize = MathEX.Code.divup(tRestNum, tThreadNum);
            // 竞争获取任务
            final int[] currentIdx = new int[] {aStart};
            final CountDownLatch tLatch = new CountDownLatch(tThreadNum);
            for (int id = 0; id < tThreadNum; ++id) {
                final int fId = id;
                pool().execute(() -> {
                    mThreadID.set(fId); // 注册 id
                    mLocks[fId].lock(); // 加锁在结束后进行数据同步
                    boolean tFirstRest = tRestBatchSize > 0; // 首先处理 rest 的情况，如果 tRestBatchSize 为零则是刚好整除分配完全
                    while (true) {
                        int tStart, tEnd;
                        synchronized (this) {
                            tStart = currentIdx[0];
                            currentIdx[0] += aStep*(tFirstRest ? tRestBatchSize : mBatchSize);
                            tEnd = Math.min(currentIdx[0], aEnd);
                        }
                        tFirstRest = false;
                        if (tStart >= aEnd) break;
                        for (int i = tStart; i < tEnd; i += aStep) aTaskWithID.run(i, fId);
                    }
                    mLocks[fId].unlock();
                    tLatch.countDown();
                });
            }
            try {tLatch.await();} catch (InterruptedException ignored) {} // 使用 CountDownLatch 来等待线程池工作完成
        }
    }
    
    /**
     * @author liqa
     * <p> 重写此类实现 parfor 的使用 </p>
     */
    @FunctionalInterface public interface IParforTask {void run(int i);}
    @FunctionalInterface public interface IParforTaskWithID {void run(int i, int threadID);}
}
