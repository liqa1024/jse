package jse.system;

import jse.code.IO;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static jse.code.CS.*;


/**
 * @author liqa
 * <p> 将一般实现放入抽象类中，因为 submit 一定需要在 pool 中使用，如果直接嵌套文件的输入流会在写入前就关闭，默认输出在 System.out </p>
 */
public abstract class AbstractSystemExecutor implements ISystemExecutor {
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
    
    @Override public final Future<Integer> submitSystem(String aCommand) {return submitSystem_(aCommand, STD_OUT_WRITELN);}
    @Override public final Future<Integer> submitSystem(String aCommand, final String aOutFilePath) {return submitSystem_(aCommand, fileWriteln(aOutFilePath));}
    @Override public final Future<List<String>> submitSystem_str(String aCommand) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return EPT_STR_FUTURE;
        final List<String> rList = new ArrayList<>();
        final Future<Integer> tSystemTask = submitSystem_(aCommand, line->rList.add(line.toString()));
        return UT.Par.map(tSystemTask, v -> rList);
    }
    
    protected final static IO.IWriteln NUL_WRITELN = line -> {/**/};
    protected final static IO.IWriteln STD_OUT_WRITELN = System.out::println;
    private IO.IWriteln fileWriteln(String aFilePath) {
        try {
            return IO.toWriteln(aFilePath);
        } catch (IOException e) {
            printStackTrace(e);
            return NUL_WRITELN;
        }
    }
    
    /** 子类重写增加自定义结束任务 */
    interface IDoFinalFuture<T> extends Future<T> {void doFinal();}
    
    /// IHasThreadPool stuffs
    protected long sleepTime() {return SYNC_SLEEP_TIME_2;}
    private volatile boolean mDead = false;
    private final List<Future<?>> mRunningSystem = new ArrayList<>();
    @Override public final void shutdown() {
        if (!mDead) {
            mDead = true;
            // 在这里添加携程等待完成后执行 shutdownFinal()
            UT.Par.runAsync(() -> {
                try {
                    awaitTermination();
                } catch (InterruptedException e) {
                    printStackTrace(e);
                } finally {
                    shutdownFinal();
                }
            });
        }
    }
    @Override public final void shutdownNow() {
        // 会直接强制取消所有任务，然后回到一般 shutdown
        synchronized (this) {
            for (Future<?> tSystem : mRunningSystem) {
                if (!tSystem.isDone()) tSystem.cancel(true);
                // cancel 不会执行 doFinal
            }
            mRunningSystem.clear();
        }
        shutdown();
    }
    @Override public final boolean isShutdown() {return mDead;}
    @Override public final synchronized int jobNumber() {
        final Iterator<Future<?>> it = mRunningSystem.iterator();
        while (it.hasNext()) {
            final Future<?> tSystem = it.next();
            if (tSystem.isDone()) {
                it.remove();
                if (tSystem instanceof IDoFinalFuture) ((IDoFinalFuture<?>)tSystem).doFinal();
            }
        }
        return mRunningSystem.size();
    }
    @Override public final int threadNumber() {return 1;}
    @SuppressWarnings("BusyWait")
    @Override public void waitUntilDone() throws InterruptedException {
        while (this.jobNumber() > 0) Thread.sleep(sleepTime());
    }
    @Override public boolean isTerminated() {
        return isShutdown() && jobNumber()==0;
    }
    @SuppressWarnings("BusyWait")
    @Override public void awaitTermination() throws InterruptedException {
        while (!isTerminated()) Thread.sleep(sleepTime());
    }
    
    
    /** 保证提交的指令都在内部有记录 */
    private int system_(String aCommand, @NotNull IO.IWriteln aWriteln) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return 0;
        int tExitValue;
        Future<Integer> tSystemTask = null;
        try {
            tSystemTask = submitSystem__(aCommand, aWriteln);
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
    @SuppressWarnings("BusyWait")
    private Future<Integer> submitSystem_(String aCommand, @NotNull IO.IWriteln aWriteln) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        if (aCommand==null || aCommand.isEmpty()) return SUC_FUTURE;
        Future<Integer> tTask = submitSystem__(aCommand, aWriteln);
        synchronized (this) {mRunningSystem.add(tTask);}
        if (tTask instanceof IDoFinalFuture) {
            IDoFinalFuture<?> fTask = (IDoFinalFuture<?>)tTask;
            UT.Par.runAsync(() -> {
                try {
                    while (!fTask.isDone()) Thread.sleep(sleepTime());
                } catch (InterruptedException e) {
                    printStackTrace(e);
                } finally {
                    fTask.doFinal();
                }
            });
        }
        return tTask;
    }
    
    @Override public final void putFiles(Iterable<? extends CharSequence> aFiles) throws Exception {putFiles_(AbstractCollections.map(aFiles, UT.Code::toString));}
    @Override public final void getFiles(Iterable<? extends CharSequence> aFiles) throws Exception {getFiles_(AbstractCollections.map(aFiles, UT.Code::toString));}
    @Override public final void putFiles(String... aFiles) throws Exception {putFiles_(AbstractCollections.from(aFiles));}
    @Override public final void getFiles(String... aFiles) throws Exception {getFiles_(AbstractCollections.from(aFiles));}
    
    /** stuff to override */
    protected void shutdownFinal() {/**/}
    protected abstract Future<Integer> submitSystem__(String aCommand, @NotNull IO.IWriteln aWriteln);
    protected abstract void putFiles_(Iterable<String> aFiles) throws Exception;
    protected abstract void getFiles_(Iterable<String> aFiles) throws Exception;
}
