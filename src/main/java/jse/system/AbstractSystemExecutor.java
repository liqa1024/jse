package jse.system;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.parallel.AbstractThreadPool;
import jse.parallel.IExecutorEX;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static jse.code.CS.*;


/**
 * @author liqa
 * <p> 将一般实现放入抽象类中，因为 submit 一定需要在 pool 中使用，如果直接嵌套文件的输入流会在写入前就关闭，默认输出在 System.out </p>
 */
public abstract class AbstractSystemExecutor extends AbstractThreadPool<IExecutorEX> implements ISystemExecutor {
    /** 增加 hook，保证 jvm 结束时将所有提交的程序都主动结束 */
    protected AbstractSystemExecutor() {
        super(newSingle());
        // 提交长期任务
        pool().execute(this::keepCheckingSystemList_);
    }
    
    private boolean mNoSTDOutput = false, mNoERROutput = false;
    @Override public final ISystemExecutor setNoSTDOutput(boolean aNoSTDOutput) {mNoSTDOutput = aNoSTDOutput; return this;}
    @Override public final boolean noSTDOutput() {return mNoSTDOutput;}
    @Override public final ISystemExecutor setNoERROutput(boolean aNoERROutput) {mNoERROutput = aNoERROutput; return this;}
    @Override public final boolean noERROutput() {return mNoERROutput;}
    
    protected final void printStackTrace(Throwable aThrowable) {if (!mNoERROutput) UT.Code.printStackTrace(aThrowable);}
    
    
    @Override public final int system(String aCommand                           ) {return system_(aCommand, STD_OUT_WRITELN);}
    @Override public final int system(String aCommand, final String aOutFilePath) {return system_(aCommand, fileWriteln(aOutFilePath));}
    
    @Override public final List<String> system_str(String aCommand) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return AbstractCollections.zl();
        final List<String> rList = new ArrayList<>();
        system_(aCommand, line->rList.add(line.toString()));
        return rList;
    }
    
    @Override public final Future<Integer> submitSystem(String aCommand                           ) {return submitSystem_(aCommand, STD_OUT_WRITELN);}
    @Override public final Future<Integer> submitSystem(String aCommand, final String aOutFilePath) {return submitSystem_(aCommand, fileWriteln(aOutFilePath));}
    
    @Override public final Future<List<String>> submitSystem_str(String aCommand) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return EPT_STR_FUTURE;
        final List<String> rList = new ArrayList<>();
        final Future<Integer> tSystemTask = submitSystem_(aCommand, line->rList.add(line.toString()));
        return UT.Par.map(tSystemTask, v -> rList);
    }
    
    protected final static UT.IO.IWriteln NUL_WRITELN = line -> {/**/};
    protected final static UT.IO.IWriteln STD_OUT_WRITELN = System.out::println;
    private UT.IO.IWriteln fileWriteln(String aFilePath) {
        try {
            return UT.IO.toWriteln(aFilePath);
        } catch (IOException e) {
            printStackTrace(e);
            return NUL_WRITELN;
        }
    }
    
    
    /** 内部使用的 Future，增加一个完成时的额外操作 */
    protected class SystemFuture<T> implements Future<T> {
        private final Future<T> mFuture;
        private SystemFuture(Future<T> aFuture) {mFuture = aFuture;}
        
        @Override public boolean cancel(boolean mayInterruptIfRunning) {return mFuture.cancel(mayInterruptIfRunning);}
        @Override public boolean isCancelled() {return mFuture.isCancelled();}
        @Override public boolean isDone() {return mFuture.isDone();}
        @Override public T get() throws InterruptedException, ExecutionException {
            if (!mValidOut) {
                mOut = mFuture.get();
                mValidOut = true;
            }
            doFinal();
            return mOut;
        }
        @Override public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (!mValidOut) {
                mOut = mFuture.get(timeout, unit);
                mValidOut = true;
            }
            doFinal();
            return mOut;
        }
        
        private volatile List<Runnable> mDoFinal = null;
        private volatile T mOut = null;
        private volatile boolean mValidOut = false;
        /** 加入同步保证最终操作是串行执行的 */
        private void doFinal() {
            if (mDoFinal!=null && !isCancelled() && isDone()) synchronized (AbstractSystemExecutor.this) {
                if (mDoFinal == null) return;
                if (!mValidOut) {
                    try {mOut = mFuture.get();} catch (Exception ignored) {}
                    mValidOut = true;
                }
                for (Runnable tDo : mDoFinal) tDo.run();
                mDoFinal = null;
            }
        }
    }
    protected <T> SystemFuture<T> toSystemFuture(Future<T> aFuture) {
        return (aFuture instanceof SystemFuture) ? (SystemFuture<T>)aFuture : new SystemFuture<>(aFuture);
    }
    protected <T> SystemFuture<T> toSystemFuture(Future<T> aFuture, Runnable aDoFinal) {
        SystemFuture<T> tSystemFuture = (aFuture instanceof SystemFuture) ? (SystemFuture<T>)aFuture : new SystemFuture<>(aFuture);
        if (tSystemFuture.mDoFinal == null) tSystemFuture.mDoFinal = new ArrayList<>();
        tSystemFuture.mDoFinal.add(aDoFinal);
        return tSystemFuture;
    }
    
    
    protected long sleepTime() {return SYNC_SLEEP_TIME_2;}
    @SuppressWarnings("BusyWait")
    private void keepCheckingSystemList_() {
        try {
            while (true) {
                Thread.sleep(sleepTime());
                // 开始检测任务完成情况，这一段直接全部加锁
                synchronized(this) {
                    // 如果没有正在执行的任务则需要考虑关闭线程
                    if (mRunningSystem.isEmpty()) {if (mDead) break; else continue;}
                    // 遍历移除已经完成的任务
                    int tIdx = 0;
                    while (tIdx < mRunningSystem.size()) {
                        SystemFuture<?> tSystem = mRunningSystem.get(tIdx);
                        if (tSystem.isDone()) {
                            mRunningSystem.set(tIdx, UT.Code.last(mRunningSystem));
                            UT.Code.removeLast(mRunningSystem);
                            // 这里执行一次保证完成时一定执行 final 语句
                            tSystem.doFinal();
                        } else {
                            ++tIdx;
                        }
                    }
                }
            }
        } catch (Exception e) {
            printStackTrace(e);
        } finally {
            // 如果还有正在执行的任务直接全部取消
            for (SystemFuture<?> tSystem : mRunningSystem) tSystem.cancel(true);
            mRunningSystem.clear();
            // 在这里执行最后的关闭
            shutdownFinal();
        }
    }
    /** IHasThreadPool stuffs */
    private final List<SystemFuture<?>> mRunningSystem = new ArrayList<>();
    private volatile boolean mDead = false;
    @Override public final void shutdown() {if (!isShutdown()) {mDead = true; super.shutdown();}}
    @Override public final void shutdownNow() {
        // 会直接强制取消所有任务，然后回到一般 shutdown
        synchronized (this) {for (Future<?> tSystem : mRunningSystem) tSystem.cancel(true);}
        shutdown();
    }
    @Override public final boolean isShutdown() {return mDead;}
    @Override public final synchronized int jobNumber() {return mRunningSystem.size();}
    @Override public final int threadNumber() {return 1;}
    @SuppressWarnings("BusyWait")
    @Override public void waitUntilDone() throws InterruptedException {while (this.jobNumber() > 0) Thread.sleep(SYNC_SLEEP_TIME_2);}
    
    
    /** 保证提交的指令都在内部有记录 */
    private int system_(String aCommand, @NotNull UT.IO.IWriteln aWriteln) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return 0;
        int tExitValue;
        Future<Integer> tSystemTask = null;
        try {
            tSystemTask = submitSystem_(aCommand, aWriteln);
            tExitValue = tSystemTask.get();
        } catch (ExecutionException e) {
            printStackTrace(e.getCause());
            tExitValue = -1;
        } catch (Exception e) {
            printStackTrace(e);
            tExitValue = -1;
        } finally {
            if (tSystemTask != null) tSystemTask.cancel(true);
        }
        return tExitValue;
    }
    private Future<Integer> submitSystem_(String aCommand, @NotNull UT.IO.IWriteln aWriteln) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return SUC_FUTURE;
        SystemFuture<Integer> tSystem = toSystemFuture(submitSystem__(aCommand, aWriteln));
        synchronized (this) {mRunningSystem.add(tSystem);}
        return tSystem;
    }
    
    @Override public final void putFiles(Iterable<? extends CharSequence> aFiles) throws Exception {putFiles_(AbstractCollections.map(aFiles, UT.Code::toString));}
    @Override public final void getFiles(Iterable<? extends CharSequence> aFiles) throws Exception {getFiles_(AbstractCollections.map(aFiles, UT.Code::toString));}
    @Override public final void putFiles(String... aFiles) throws Exception {putFiles_(AbstractCollections.from(aFiles));}
    @Override public final void getFiles(String... aFiles) throws Exception {getFiles_(AbstractCollections.from(aFiles));}
    
    /** stuff to override */
    protected void shutdownFinal() {/**/}
    protected abstract Future<Integer> submitSystem__(String aCommand, @NotNull UT.IO.IWriteln aWriteln);
    protected abstract void putFiles_(Iterable<String> aFiles) throws Exception;
    protected abstract void getFiles_(Iterable<String> aFiles) throws Exception;
}
