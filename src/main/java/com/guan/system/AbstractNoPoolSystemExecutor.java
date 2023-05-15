package com.guan.system;


import com.google.common.collect.ImmutableList;
import com.guan.code.UT;
import com.guan.io.IHasIOFiles;
import com.guan.io.IOFiles;
import com.guan.parallel.AbstractHasThreadPool;
import com.guan.parallel.IExecutorEX;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> 不使用 java 线程池管理并行任务的 SystemExecutor，即需要使用所在系统自带的任务提交系统 </p>
 * <p> 实际为了监控任务完成情况，依旧会使用一个单线程的线程池 </p>
 * <p> 与一般的实现不同的是，指令输出只会输出到文件中，而输出到 List 的会从这个文件中来读取，
 * 输出文件的 key 为 {@code "<out>"}（不会让外部获取到修改后的 IOFiles，因此只是用于内部使用）</p>
 */
public abstract class AbstractNoPoolSystemExecutor<T extends ISystemExecutor> extends AbstractHasThreadPool<IExecutorEX> implements ISystemExecutor {
    protected final static String OUT_FILE_KEY = "<out>";
    
    /** 包装一个任意的 mSystemExecutor 来执行指令，注意只会使用其中的最简单的 system 相关操作，因此不需要包含线程池 */
    protected final T mEXE;
    protected final int mParallelNum;
    protected final LinkedList<FutureJob> mQueuedJobList;
    protected final Map<FutureJob, Integer> mJobList;
    private final Thread mHook; // ShutdownHook
    
    protected AbstractNoPoolSystemExecutor(T aSystemExecutor, int aParallelNum) {
        super(newSingle());
        mEXE = aSystemExecutor;
        mParallelNum = aParallelNum;
        mJobList = new LinkedHashMap<>();
        mQueuedJobList = new LinkedList<>();
        
        // 提交长期任务
        pool().execute(this::keepSubmitFromList_);
        // 在 JVM 意外关闭时手动执行杀死 kill
        mHook = new Thread(this::kill);
        Runtime.getRuntime().addShutdownHook(mHook);
    }
    
    
    /** HasThreadPool stuffs */
    private volatile boolean mDead = false;
    private volatile boolean mPause = false; // 可以暂停任务的提交
    private volatile boolean mKilled = false; // 直接强制杀死提交进程
    
    @Override public void shutdown() {shutdown_();}
    @Override public void shutdownNow() {cancelThis(); shutdown_();}
    @Override public boolean isShutdown() {return mDead;}
    
    @Override public synchronized int nTasks() {return mQueuedJobList.size() + mJobList.size();}
    @Override public int nThreads() {return mParallelNum;}
    
    private void shutdown_() {
        mDead = true;
        pool().shutdown();
        Runtime.getRuntime().removeShutdownHook(mHook);
    }
    protected synchronized void cancelThis() {
        mQueuedJobList.clear();
        cancelThis_();
        mJobList.clear();
    }
    
    /// TODO 额外添加的一些接口，用于后续设置镜像的实现
    /** 设置暂停，会挂起直到获得这个对象的锁，这样在外部调用后确实已经暂停，而在内部使用时不容易出现死锁 */
    public synchronized void pause() {mPause = true;}
    public void unpause() {mPause = false;}
    /** 直接杀死这个对象，类似于系统层面的杀死进程，会直接关闭提交任务并且放弃监管远程服务器的任务而不是取消这些任务，从而使得 mirror 的内容冻结 */
    public void kill() {
        // 会先暂停保证正在进行的任务已经完成提交，保证镜像文件不会被这个对象再次修改
        pause();
        mKilled = true;
        mDead = true;
        pool().shutdown();
    }
    
    /** 还是采用一样的写法，提价一个长期任务不断手动监控任务完成情况 */
    @SuppressWarnings("BusyWait")
    private void keepSubmitFromList_() {
        while (true) {
            // 如果被杀死则直接结束（优先级最高）
            if (mKilled) break;
            try {Thread.sleep(sleepTime());} catch (InterruptedException e) {e.printStackTrace(); break;}
            // 如果已经暂停则直接跳过
            if (mPause) continue;
            // 开始检测任务完成情况，这一段直接全部加锁
            synchronized(this) {
                // 如果已经暂停则直接跳过，并行特有的两次检测
                if (mPause) continue;
                // 如果没有指令需要提交，并且没有正在执行的任务则需要考虑关闭线程
                if (mQueuedJobList.isEmpty() && mJobList.isEmpty()) {if (mDead) break; else continue;}
                // 获取实际还在运行的任务
                Set<Integer> tJobIDs = getRunningJobIDsFromSystem();
                // 因为各种原因获取不到 JobIDs，直接跳过
                if (tJobIDs == null) continue;
                // 遍历自身移除所有不在 JobIDs 中的任务
                final Iterator<Map.Entry<FutureJob, Integer>> tIt = mJobList.entrySet().iterator();
                while (tIt.hasNext()) {
                    Map.Entry<FutureJob, Integer> tEntry = tIt.next();
                    Integer tJobID = tEntry.getValue();
                    FutureJob tFutureJob = tEntry.getKey();
                    // 判断是否已经完成从而需要移除
                    if (!tJobIDs.contains(tJobID)) {
                        // 进行移除
                        tIt.remove();
                        // 指定对应的 Future 已经完成
                        tFutureJob.done(0); // 我也不知道退出码是多少，没有别的问题直接统一认为是 0
                    }
                }
                // 移除完成后检测并行数目是否合适，合适则继续添加任务
                while (!mQueuedJobList.isEmpty() && mJobList.size() < mParallelNum) {
                    // 获取第一个元素
                    FutureJob tFutureJob = mQueuedJobList.pollFirst();
                    // 尝试提交任务获取 JobID，这里都放在 FutureJob 内部执行
                    int tJobID = tFutureJob.submitSystemCommand();
                    if (tJobID > 0) {
                        // 如果提交成功则注册任务
                        mJobList.put(tFutureJob, tJobID);
                    } else {
                        // 否则直接设置任务完成
                        tFutureJob.done(-1); // 此时依旧会尝试下载输出文件
                    }
                }
            }
        }
        // 在这里关闭内部的 EXE
        mEXE.shutdown();
    }
    
    
    /** 排队指令的一个结构体 */
    private static class SystemCommand {
        protected final String mSubmitCommand;
        protected final boolean mNoOutput;
        private SystemCommand(String aSubmitCommand, boolean aNoOutput) {
            mSubmitCommand = aSubmitCommand; mNoOutput = aNoOutput;
        }
    }
    /** 需要专门自定义实现一个 Future，返回的是这个指令最终的退出代码，注意保持只有一个锁来防止死锁 */
    protected class FutureJob implements IFutureJob {
        private SystemCommand mSystemCommand;
        private Iterable<String> mOFiles;
        protected FutureJob(String aSubmitCommand, Iterable<String> aOFiles, boolean aNoOutput) {
            mSystemCommand = new SystemCommand(aSubmitCommand, aNoOutput);
            mOFiles = aOFiles;
        }
        
        /** 获取这个任务的状态 */
        @Override public StateType state() {synchronized (AbstractNoPoolSystemExecutor.this) {
            if (isDone()) return StateType.DONE;
            if (mQueuedJobList.contains(this)) return StateType.QUEUING;
            if (mJobList.containsKey(this)) return StateType.RUNNING;
            return StateType.ELSE;
        }}
        /** 获取 jobID */
        @Override public int jobID() {synchronized (AbstractNoPoolSystemExecutor.this) {
            if (mJobList.containsKey(this)) return mJobList.get(this);
            return -1;
        }}
        
        
        /** 尝试提交任务并且获取 ID */
        protected int submitSystemCommand() {synchronized (AbstractNoPoolSystemExecutor.this) {
            if (mSystemCommand == null) return -1;
            // 获取提交任务的 ID，提交任务不需要附加 aIOFiles
            List<String> tOutList = mEXE.system_str(mSystemCommand.mSubmitCommand);
            int tJobID = getJobIDFromSystem(tOutList);
            if (tJobID > 0) {
                // 如果提交成功则输出到 out
                if (!mSystemCommand.mNoOutput) for (String tLine : tOutList) System.out.println(tLine);
            } else {
                // 提交不成功则输出到 err，并且不考虑 NoOutput
                for (String tLine : tOutList) System.err.println(tLine);
            }
            // 提交完成后设置 mSystemCommand 为 null，避免重复提交
            mSystemCommand = null;
            return tJobID;
        }}
        
        private boolean mIsCancelled = false;
        private void setCancelled_() {synchronized (AbstractNoPoolSystemExecutor.this) {
            mIsCancelled = true; done(130);
        }}
        
        private boolean mIsDone = false;
        private int mExitValue = -1;
        /** 设置完成后会自动执行下载任务，注意如果退出码是 130（手动取消）则不会下载 */
        protected void done(int aExitValue) {synchronized (AbstractNoPoolSystemExecutor.this) {
            if (isDone()) return;
            mIsDone = true;
            mExitValue = aExitValue;
            if (aExitValue != 130 && mOFiles != null) {
                try {getFiles(mOFiles);} catch (Exception e) {e.printStackTrace(); mExitValue = mExitValue==0 ? -1 : aExitValue;}
            }
            // 结束后将内部附加属性置空
            mSystemCommand = null;
            mOFiles = null;
        }}
        
        /** 尝试取消这个任务 */
        @Override public boolean cancel() {synchronized (AbstractNoPoolSystemExecutor.this) {
            if (isCancelled()) return false;
            // 先检测是否在排队，如果还在排队则直接取消
            if (mQueuedJobList.remove(this)) {
                setCancelled_();
                return true;
            }
            // 如果已经在 JobList 中，则需要通过远程指令来取消
            if (mJobList.containsKey(this)) {
                int tJobID = mJobList.get(this);
                // 尝试取消这个任务
                boolean tOut = cancelJobFromSystem(tJobID);
                // 取消成功则也移除内部的
                if (tOut) {
                    mJobList.remove(this);
                    setCancelled_();
                }
                return tOut;
            }
            // 没有这个任务，取消失败
            return false;
        }}
        @Override public boolean isCancelled() {synchronized (AbstractNoPoolSystemExecutor.this) {
            return mIsCancelled;
        }}
        @Override public boolean isDone() {synchronized (AbstractNoPoolSystemExecutor.this) {
            return mIsDone;
        }}
        @ApiStatus.Internal @Override public int getExitValue_() {synchronized (AbstractNoPoolSystemExecutor.this) {
            return mExitValue;
        }}
    }
    
    
    
    /** ISystemExecutor stuffs */
    @Override public final int system_NO(String aCommand                                           ) {return system_(aCommand, null, EPT_IOF, true);}
    @Override public final int system   (String aCommand, String aOutFilePath                      ) {aOutFilePath = toRealOutFilePath(aOutFilePath); return system_(aCommand, aOutFilePath, new IOFiles().putOFiles(OUT_FILE_KEY, aOutFilePath), noConsoleOutput());}
    @Override public final int system_NO(String aCommand                     , IHasIOFiles aIOFiles) {return system_(aCommand, null, aIOFiles, true);}
    @Override public final int system   (String aCommand, String aOutFilePath, IHasIOFiles aIOFiles) {aOutFilePath = toRealOutFilePath(aOutFilePath); return system_(aCommand, aOutFilePath, aIOFiles.copy().putOFiles(OUT_FILE_KEY, aOutFilePath), noConsoleOutput());}
    
    @Override public final int system(String aCommand) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath == null) return system_(aCommand, null, EPT_IOF, noConsoleOutput());
        tFilePath = toRealOutFilePath(tFilePath);
        return system_(aCommand, tFilePath, new IOFiles().putOFiles(OUT_FILE_KEY, tFilePath), noConsoleOutput());
    }
    @Override public final int system(String aCommand, IHasIOFiles aIOFiles) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath == null) return system_(aCommand, null, aIOFiles, noConsoleOutput());
        tFilePath = toRealOutFilePath(tFilePath);
        return system_(aCommand, tFilePath, aIOFiles.copy().putOFiles(OUT_FILE_KEY, tFilePath), noConsoleOutput());
    }
    @Override public final List<String> system_str(String aCommand) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath==null) {
            system_NO(aCommand);
            return ImmutableList.of();
        } else {
            tFilePath = toRealOutFilePath(tFilePath);
            system_(aCommand, tFilePath, new IOFiles().putOFiles(OUT_FILE_KEY, tFilePath), true);
            try {
                return UT.IO.readAllLines_(tFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return ImmutableList.of();
            }
        }
    }
    @Override public final List<String> system_str(String aCommand, IHasIOFiles aIOFiles) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath==null) {
            system_NO(aCommand, aIOFiles);
            return ImmutableList.of();
        } else {
            tFilePath = toRealOutFilePath(tFilePath);
            system_(aCommand, tFilePath, aIOFiles.copy().putOFiles(OUT_FILE_KEY, tFilePath), true);
            try {
                return UT.IO.readAllLines_(tFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return ImmutableList.of();
            }
        }
    }
    
    
    @Override public final IFutureJob submitSystem_NO(String aCommand                                           ) {return submitSystem_(aCommand, null, EPT_IOF, true);}
    @Override public final IFutureJob submitSystem   (String aCommand, String aOutFilePath                      ) {aOutFilePath = toRealOutFilePath(aOutFilePath); return submitSystem_(aCommand, aOutFilePath, new IOFiles().putOFiles(OUT_FILE_KEY, aOutFilePath), noConsoleOutput());}
    @Override public final IFutureJob submitSystem_NO(String aCommand                     , IHasIOFiles aIOFiles) {return submitSystem_(aCommand, null, aIOFiles.copy(), true);}
    @Override public final IFutureJob submitSystem   (String aCommand, String aOutFilePath, IHasIOFiles aIOFiles) {aOutFilePath = toRealOutFilePath(aOutFilePath); return submitSystem_(aCommand, aOutFilePath, aIOFiles.copy().putOFiles(OUT_FILE_KEY, aOutFilePath), noConsoleOutput());}
    
    @Override public final IFutureJob submitSystem(String aCommand) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath == null) return submitSystem_(aCommand, null, EPT_IOF, noConsoleOutput());
        tFilePath = toRealOutFilePath(tFilePath);
        return submitSystem_(aCommand, tFilePath, new IOFiles().putOFiles(OUT_FILE_KEY, tFilePath), noConsoleOutput());
    }
    @Override public final IFutureJob submitSystem(String aCommand, IHasIOFiles aIOFiles) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath == null) return submitSystem_(aCommand, null, aIOFiles.copy(), noConsoleOutput());
        tFilePath = toRealOutFilePath(tFilePath);
        return submitSystem_(aCommand, tFilePath, aIOFiles.copy().putOFiles(OUT_FILE_KEY, tFilePath), noConsoleOutput());
    }
    /** 使用 CompletableFuture 来实现 Future 的转换 */
    @Override public final Future<List<String>> submitSystem_str(String aCommand) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath==null) {
            final Future<Integer> tSystemTask = submitSystem_NO(aCommand);
            return CompletableFuture.supplyAsync(() -> {
                try {tSystemTask.get();} catch (Exception e) {e.printStackTrace();}
                return ImmutableList.of();
            });
        } else {
            final String fFilePath = toRealOutFilePath(tFilePath);
            final Future<Integer> tSystemTask = submitSystem_(aCommand, fFilePath, new IOFiles().putOFiles(OUT_FILE_KEY, fFilePath), true);
            return CompletableFuture.supplyAsync(() -> {
                try {tSystemTask.get();} catch (Exception e) {e.printStackTrace();}
                try {
                    return UT.IO.readAllLines_(fFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return ImmutableList.of();
                }
            });
        }
    }
    @Override public final Future<List<String>> submitSystem_str(String aCommand, IHasIOFiles aIOFiles) {
        String tFilePath = defaultOutFilePath();
        if (tFilePath==null) {
            final Future<Integer> tSystemTask = submitSystem_NO(aCommand, aIOFiles);
            return CompletableFuture.supplyAsync(() -> {
                try {tSystemTask.get();} catch (Exception e) {e.printStackTrace();}
                return ImmutableList.of();
            });
        } else {
            final String fFilePath = toRealOutFilePath(tFilePath);
            final Future<Integer> tSystemTask = submitSystem_(aCommand, fFilePath, aIOFiles.copy().putOFiles(OUT_FILE_KEY, fFilePath), true);
            return CompletableFuture.supplyAsync(() -> {
                try {tSystemTask.get();} catch (Exception e) {e.printStackTrace();}
                try {
                    return UT.IO.readAllLines_(fFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return ImmutableList.of();
                }
            });
        }
    }
    
    
    
    /** 用于减少重复代码，这里 aIOFiles 包含了指令本身输出的文件，并且认为已经考虑了 aIOFiles 易失的问题，aNoOutput 指定是否会将提交结果信息输出到控制台 */
    protected final int system_(String aCommand, @Nullable String aOutFilePath, IHasIOFiles aIOFiles, boolean aNoOutput) {
        if (mDead) throw new RuntimeException("Can NOT do system from this Dead Executor.");
        // 先上传输入文件，上传文件部分是串行的
        try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return -1;}
        // 由于已经上传，提交任务不需要附加 aIOFiles
        int tExitValue = aNoOutput ? mEXE.system_NO(getRunCommand(aCommand, aOutFilePath)) : mEXE.system(getRunCommand(aCommand, aOutFilePath));
        // 直接下载输入文件
        try {getFiles(aIOFiles.getOFiles());} catch (Exception e) {e.printStackTrace(); return tExitValue == 0 ? -1 : tExitValue;}
        return tExitValue;
    }
    /** 用于减少重复代码，这里 aIOFiles 包含了指令本身输出的文件，并且认为已经考虑了 aIOFiles 易失的问题，aNoOutput 指定是否会将提交结果信息输出到控制台 */
    protected final IFutureJob submitSystem_(String aCommand, @Nullable String aOutFilePath, IHasIOFiles aIOFiles, boolean aNoOutput) {
        if (mDead) throw new RuntimeException("Can NOT submitSystem from this Dead Executor.");
        // 先上传输入文件，上传文件部分是串行的
        try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return ERR_FUTURE;}
        // 获取 FutureJob
        FutureJob tFutureJob = new FutureJob(getSubmitCommand(aCommand, aOutFilePath), aIOFiles.getOFiles(), aNoOutput);
        // 为了逻辑上简单，这里统一先加入排队
        synchronized (this) {mQueuedJobList.addLast(tFutureJob);}
        return tFutureJob;
    }
    private final static IHasIOFiles EPT_IOF = new IOFiles();
    private final static IFutureJob ERR_FUTURE = new IFutureJob() {
        @Override public boolean isCancelled() {return false;}
        @Override public boolean isDone() {return true;}
        @Override public StateType state() {return StateType.DONE;}
        @ApiStatus.Internal @Override public int getExitValue_() {return -1;}
        @Override public int jobID() {return -1;}
        @Override public boolean cancel() {return false;}
    };
    
    
    
    /// stuff to override
    /** 用来控制一般的提交是否会在控制台输出提交的结果 */
    protected boolean noConsoleOutput() {return false;}
    /** 用来控制检测频率，ms */
    protected long sleepTime() {return 500;}
    
    /** 因为上传和下载已经分开，因此需要子类重写这两个函数来实现提交任务部分的文件上传下载（而对于本地不需要同步的则可以不重写）*/
    protected void putFiles(Iterable<String> aFiles) throws Exception {/**/}
    protected void getFiles(Iterable<String> aFiles) throws Exception {/**/}
    
    /** 提供向系统提交任务所需要的指令, null 表示没有输出文件 */
    protected abstract @Nullable String defaultOutFilePath();
    protected abstract @Nullable String toRealOutFilePath(String aOutFilePath);
    protected abstract String getRunCommand(String aCommand, @Nullable String aOutFilePath);
    protected abstract String getSubmitCommand(String aCommand, @Nullable String aOutFilePath);
    
    /** 使用 submit 指令后系统会给出输出，需要使用这个输出来获取对应任务的 ID 用于监控任务是否完成，返回 <= 0 的值代表提交任务失败 */
    protected abstract int getJobIDFromSystem(List<String> aOutList);
    /** 获取这个对象提交的任务中，正在执行的任务 id 列表，用来监控任务是否完成 */
    protected abstract @Nullable Set<Integer> getRunningJobIDsFromSystem();
    /** 取消指定任务 ID 的任务，返回是否取消成功（已经完成的也会返回 false）*/
    protected abstract boolean cancelJobFromSystem(int aJobID);
    /** 取消这个对象提交的所有任务，子类重写来优化避免重复的提交指令 */
    protected synchronized void cancelThis_() {
        for (int tJobID : mJobList.values()) cancelJobFromSystem(tJobID);
    }
}
