package com.jtool.system;

import com.google.common.collect.ImmutableList;
import com.jtool.code.UT;
import com.jtool.code.operator.IOperator1;
import com.jtool.iofile.IHasIOFiles;
import com.jtool.iofile.MergedIOFiles;
import com.jtool.parallel.AbstractHasThreadPool;
import com.jtool.parallel.IExecutorEX;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.jtool.code.CS.*;


/**
 * @author liqa
 * <p> 将一般实现放入抽象类中，因为 submit 一定需要在 pool 中使用，如果直接嵌套文件的输入流会在写入前就关闭，默认输出在 System.out </p>
 */
public abstract class AbstractSystemExecutor extends AbstractHasThreadPool<IExecutorEX> implements ISystemExecutor {
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
    
    
    @Override public final int system(String aCommand                                                 ) {return system_(aCommand, this::outPrintln);}
    @Override public final int system(String aCommand, final String aOutFilePath                      ) {return system_(aCommand, () -> filePrintln(aOutFilePath));}
    @Override public final int system(String aCommand                           , IHasIOFiles aIOFiles) {return system_(aCommand, this::outPrintln, aIOFiles);}
    @Override public final int system(String aCommand, final String aOutFilePath, IHasIOFiles aIOFiles) {return system_(aCommand, () -> filePrintln(aOutFilePath), aIOFiles);}
    
    @Override public final List<String> system_str(String aCommand) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return ImmutableList.of();
        final List<String> rList = new ArrayList<>();
        system_(aCommand, () -> listPrintln(rList));
        return rList;
    }
    @Override public final List<String> system_str(String aCommand, IHasIOFiles aIOFiles) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) {
            if (needSyncIOFiles()) synchronized (this) {
                try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return ImmutableList.of();}
                try {getFiles(aIOFiles.getOFiles());} catch (Exception e) {e.printStackTrace(); return ImmutableList.of();}
            }
            return ImmutableList.of();
        }
        final List<String> rList = new ArrayList<>();
        system_(aCommand, () -> listPrintln(rList), aIOFiles);
        return rList;
    }
    
    @Override public final Future<Integer> submitSystem(String aCommand                                                 ) {return submitSystem_(aCommand, this::outPrintln);}
    @Override public final Future<Integer> submitSystem(String aCommand, final String aOutFilePath                      ) {return submitSystem_(aCommand, () -> filePrintln(aOutFilePath));}
    @Override public final Future<Integer> submitSystem(String aCommand                           , IHasIOFiles aIOFiles) {return submitSystem_(aCommand, this::outPrintln, aIOFiles);}
    @Override public final Future<Integer> submitSystem(String aCommand, final String aOutFilePath, IHasIOFiles aIOFiles) {return submitSystem_(aCommand, () -> filePrintln(aOutFilePath), aIOFiles);}
    
    @Override public final Future<List<String>> submitSystem_str(String aCommand) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return EPT_STR_FUTURE;
        final List<String> rList = new ArrayList<>();
        final Future<Integer> tSystemTask = submitSystem_(aCommand, () -> listPrintln(rList));
        return UT.Par.map(tSystemTask, v -> rList);
    }
    @Override public final Future<List<String>> submitSystem_str(String aCommand, IHasIOFiles aIOFiles) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) {
            if (needSyncIOFiles()) synchronized (this) {
                try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return EPT_STR_FUTURE;}
                try {getFiles(aIOFiles.getOFiles());} catch (Exception e) {e.printStackTrace(); return EPT_STR_FUTURE;}
            }
            return EPT_STR_FUTURE;
        }
        final List<String> rList = new ArrayList<>();
        final Future<Integer> tSystemTask = submitSystem_(aCommand, () -> listPrintln(rList), aIOFiles);
        return UT.Par.map(tSystemTask, v -> rList);
    }
    
    /** 批量任务直接遍历提交，使用 UT.Code.mergeAll 来管理 Future */
    private List<String> mBatchCommands = new ArrayList<>();
    private MergedIOFiles mBatchIOFiles = new MergedIOFiles();
    @Override public final synchronized Future<List<Integer>> submitBatchSystem() {
        if (needSyncIOFiles()) {
            try {putFiles(mBatchIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return ERR_FUTURES;}
        }
        List<Future<Integer>> rSystems = new ArrayList<>();
        for (String tCommand : mBatchCommands) rSystems.add(submitSystem__(tCommand, this::outPrintln));
        SystemFuture<List<Integer>> tBatchSystem = toSystemFuture(UT.Par.mergeAll(rSystems));
        // 如果下载文件是必要的，使用这个方法来在 tFuture 完成时自动下载附加文件
        if (needSyncIOFiles()) {
            // 注意 mBatchIOFiles 是易失的，因此需要拷贝一份输出文件列表；这里不考虑并行的情况，仅考虑传入的 aIOFiles 在函数结束时会被修改
            final List<String> tOFiles = new ArrayList<>();
            for (String tFile : mBatchIOFiles.getOFiles()) tOFiles.add(tFile);
            tBatchSystem = toSystemFuture(tBatchSystem, exitValues -> {
                if (exitValues == null) return null;
                try {getFiles(tOFiles);}
                catch (Exception e) {e.printStackTrace();}
                return exitValues;
            });
        }
        synchronized (this) {mRunningSystem.add(tBatchSystem);}
        
        mBatchCommands = new ArrayList<>();
        mBatchIOFiles = new MergedIOFiles();
        return tBatchSystem;
    }
    @Override public final synchronized void putBatchSystem(String aCommand) {
        // 对于空指令专门优化，不添加到队列
        if (aCommand != null && !aCommand.isEmpty()) mBatchCommands.add(aCommand);
    }
    @Override public final synchronized void putBatchSystem(String aCommand, IHasIOFiles aIOFiles) {
        // 对于空指令专门优化，不添加到队列
        if (aCommand != null && !aCommand.isEmpty()) mBatchCommands.add(aCommand);
        mBatchIOFiles.merge(aIOFiles);
    }
    
    
    /** only support println */
    protected interface IPrintln extends AutoCloseable {void println(String aLine); void close();}
    /** submit 相关需要使用 supplier，只在需要输入的时候进行创建 */
    @FunctionalInterface protected interface IPrintlnSupplier extends Supplier<IPrintln> {@NotNull IPrintln get();}
    /** 不使用静态方法方便子类重写 */
    protected IPrintln outPrintln() {
        return new IPrintln() {
            @Override public void println(String aLine) {System.out.println(aLine);}
            @Override public void close() {/**/}
        };
    }
    @SuppressWarnings("resource")
    protected IPrintln filePrintln(String aFilePath) {
        final PrintStream tFilePS;
        try {
           tFilePS = UT.IO.toPrintStream(aFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new IPrintln() {
            @Override public void println(String aLine) {tFilePS.println(aLine);}
            @Override public void close() {tFilePS.close();}
        };
    }
    protected IPrintln listPrintln(final List<String> aList) {
        return new IPrintln() {
            @Override public void println(String aLine) {aList.add(aLine);}
            @Override public void close() {/**/}
        };
    }
    
    
    /** 内部使用的 Future，增加一个完成时的额外操作 */
    private class SystemFuture<T> implements Future<T> {
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
        
        private volatile List<IOperator1<T, T>> mDoFinal = null;
        private volatile T mOut = null;
        private volatile boolean mValidOut = false;
        /** 加入同步保证最终操作（下载文件）是串行执行的 */
        private void doFinal() {
            if (mDoFinal != null && !isCancelled() && isDone()) synchronized (AbstractSystemExecutor.this) {
                if (mDoFinal == null) return;
                if (!mValidOut) {
                    try {mOut = mFuture.get();} catch (Exception ignored) {}
                    mValidOut = true;
                }
                for (IOperator1<T, T> tDo : mDoFinal) mOut = tDo.cal(mOut);
                mDoFinal = null;
            }
        }
    }
    protected <T> SystemFuture<T> toSystemFuture(Future<T> aFuture) {
        return (aFuture instanceof SystemFuture) ? (SystemFuture<T>)aFuture : new SystemFuture<>(aFuture);
    }
    protected <T> SystemFuture<T> toSystemFuture(Future<T> aFuture, IOperator1<T, T> aDoFinal) {
        SystemFuture<T> tSystemFuture = (aFuture instanceof SystemFuture) ? (SystemFuture<T>)aFuture : new SystemFuture<>(aFuture);
        if (tSystemFuture.mDoFinal == null) tSystemFuture.mDoFinal = new ArrayList<>();
        tSystemFuture.mDoFinal.add(aDoFinal);
        return tSystemFuture;
    }
    protected <T> SystemFuture<T> toSystemFuture(Future<T> aFuture, Runnable aDoFinal) {
        return toSystemFuture(aFuture, out -> {aDoFinal.run(); return out;});
    }
    
    
    protected long sleepTime() {return 20;}
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
                            int tLastIdx = mRunningSystem.size()-1;
                            mRunningSystem.set(tIdx, mRunningSystem.get(tLastIdx));
                            mRunningSystem.remove(tLastIdx);
                            // 这里执行一次保证完成时一定执行 final 语句，包括下载输出文件等
                            tSystem.doFinal();
                        } else {
                            ++tIdx;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 在这里执行最后的关闭
            shutdownFinal();
        }
    }
    /** IHasThreadPool stuffs */
    private final List<SystemFuture<?>> mRunningSystem = new ArrayList<>();
    private volatile boolean mDead = false;
    @Override public final void shutdown() {if (!isShutdown()) {mDead = true; pool().shutdown();}}
    @Override public final void shutdownNow() {
        // 会直接强制取消所有任务，然后回到一般 shutdown
        synchronized (this) {for (Future<?> tSystem : mRunningSystem) tSystem.cancel(true);}
        shutdown();
    }
    @Override public final boolean isShutdown() {return mDead;}
    @Override public final synchronized int nJobs() {return mRunningSystem.size();}
    @Override public final int nThreads() {return 1;}
    @SuppressWarnings("BusyWait")
    @Override public void waitUntilDone() throws InterruptedException {while (nJobs() > 0) Thread.sleep(20);}
    
    
    /** 保证提交的指令都在内部有记录 */
    private int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return 0;
        int tExitValue;
        try {
            tExitValue = submitSystem_(aCommand, aPrintln).get();
        } catch (Exception e) {
            e.printStackTrace();
            tExitValue = -1;
        }
        return tExitValue;
    }
    private int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) {
            if (needSyncIOFiles()) synchronized (this) {
                try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return -1;}
                try {getFiles(aIOFiles.getOFiles());} catch (Exception e) {e.printStackTrace(); return -1;}
            }
            return 0;
        }
        int tExitValue = 0;
        try {
            if (needSyncIOFiles()) synchronized (this) {putFiles(aIOFiles.getIFiles());}
            tExitValue = submitSystem_(aCommand, aPrintln).get();
            if (needSyncIOFiles()) synchronized (this) {getFiles(aIOFiles.getOFiles());}
        } catch (Exception e) {
            e.printStackTrace();
            tExitValue = tExitValue==0 ? -1 : tExitValue;
        }
        return tExitValue;
    }
    private Future<Integer> submitSystem_(String aCommand, @NotNull IPrintlnSupplier aPrintln) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return SUC_FUTURE;
        SystemFuture<Integer> tSystem = toSystemFuture(submitSystem__(aCommand, aPrintln));
        synchronized (this) {mRunningSystem.add(tSystem);}
        return tSystem;
    }
    private Future<Integer> submitSystem_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) {
            if (needSyncIOFiles()) synchronized (this) {
                try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return ERR_FUTURE;}
                try {getFiles(aIOFiles.getOFiles());} catch (Exception e) {e.printStackTrace(); return ERR_FUTURE;}
            }
            return SUC_FUTURE;
        }
        if (needSyncIOFiles()) synchronized (this) {
            try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return ERR_FUTURE;}
        }
        SystemFuture<Integer> tSystem = toSystemFuture(submitSystem__(aCommand, aPrintln));
        // 如果下载文件是必要的，使用这个方法来在 tFuture 完成时自动下载附加文件
        if (needSyncIOFiles()) {
            // 注意 aIOFiles 是易失的，因此需要拷贝一份输出文件列表；这里不考虑并行的情况，仅考虑传入的 aIOFiles 在函数结束时会被修改
            final List<String> tOFiles = new ArrayList<>();
            for (String tFile : aIOFiles.getOFiles()) tOFiles.add(tFile);
            tSystem = toSystemFuture(tSystem, exitValue -> {
                if (exitValue==null || exitValue!=0) return exitValue;
                try {
                    getFiles(tOFiles);
                } catch (Exception e) {
                    e.printStackTrace();
                    exitValue = -1;
                }
                return exitValue;
            });
        }
        synchronized (this) {mRunningSystem.add(tSystem);}
        return tSystem;
    }
    
    
    /** stuff to override */
    protected void shutdownFinal() {/**/}
    protected abstract Future<Integer> submitSystem__(String aCommand, @NotNull IPrintlnSupplier aPrintln);
    public abstract void putFiles(Iterable<String> aFiles) throws Exception;
    public abstract void getFiles(Iterable<String> aFiles) throws Exception;
    public abstract boolean needSyncIOFiles();
}
