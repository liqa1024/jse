package jse.system;

import jse.code.IO;
import jse.code.OS.Slurm;
import jse.code.UT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static jse.code.CS.*;
import static jse.code.OS.Slurm.IS_SLURM;
import static jse.code.OS.Slurm.RESOURCES_MANAGER;

/**
 * @author liqa
 * <p> 在 SLURM 内部提交子任务的提交器，主要用于 salloc 或者 sbatch 一个 jse 任务后，
 * 在提交的 jse 任务中提交子任务；因此认为此时已经有了 SLURM 的任务环境 </p>
 * <p> 由于是提交子任务的形式，这里依旧使用 java 线程池来提交后台任务 </p>
 */
public class SRUNSystemExecutor extends LocalSystemExecutor {
    private final SRUNChecker mChecker;
    /** 线程数现在由每个任务的并行数，申请到的节点数，以及每节点的核心数来确定 */
    public SRUNSystemExecutor(int aTaskNum, int aMaxParallelNum) {
        super();
        // 由于是本地的，这里不需要创建文件夹
        // 仅 slurm 可用
        if (!IS_SLURM) {
            throw new IllegalStateException("SRUN can Only be used in SLURM");
        }
        Map<Slurm.Resource, Boolean> tAssignedResources = new HashMap<>();
        for (int i = 0; i < aMaxParallelNum; ++i) {
            Slurm.Resource tResource = aTaskNum>0 ? RESOURCES_MANAGER.assignResource(aTaskNum) : RESOURCES_MANAGER.assignResource();
            // 分配失败直接抛出错误
            if (tResource == null) {
                throw new IllegalArgumentException("Not enough resource in SLURM to assign");
            }
            tAssignedResources.put(tResource, false);
        }
        mChecker = new SRUNChecker(this, tAssignedResources);
    }
    public SRUNSystemExecutor(int aTaskNum) {this(aTaskNum, 1);}
    public SRUNSystemExecutor() {this(-1, 1);}
    
    /** 内部使用的向任务分配节点的方法 */
    private synchronized @Nullable Slurm.Resource assignResource() {
        for (Map.Entry<Slurm.Resource, Boolean> tEntry : mChecker.mAssignedResources.entrySet()) {
            if (!tEntry.getValue()) {
                tEntry.setValue(true);
                return tEntry.getKey();
            }
        }
        // 所有节点的任务都分配满了，输出 null
        return null;
    }
    /** 内部使用的任务完成归还节点的方法 */
    private synchronized void returnResource(Slurm.Resource aResource) {
        mChecker.mAssignedResources.put(aResource, false);
    }
    
    @Override protected Future<Integer> submitSystem__(String aCommand, @NotNull IO.IWriteln aWriteln) {
        // 对于空指令专门优化，不执行操作
        if (aCommand == null || aCommand.isEmpty()) return SUC_FUTURE;
        // 先尝试获取节点
        Slurm.Resource tResource = assignResource();
        if (tResource == null && !noERROutput()) {
            UT.Code.warning("Can NOT to assign resource for this job temporarily, this job blocks until there are any free resource.\n" +
                            "It may be caused by too large number of parallels.");
        }
        while (tResource == null) {
            try {Thread.sleep(FILE_SYSTEM_SLEEP_TIME);}
            catch (InterruptedException e) {printStackTrace(e); return ERR_FUTURE;}
            tResource = assignResource();
        }
        // 为了兼容性，需要将实际需要执行的脚本写入 bash 后再执行，从而可以支持复杂的逻辑输入（包含 cd 等操作或者多行命令）
        String tTempScriptPath = mChecker.mWorkingDir+UT.Code.randID()+".sh";
        try {IO.write(tTempScriptPath, "#!/bin/bash\n"+aCommand);}
        catch (Exception e) {printStackTrace(e); returnResource(tResource); return ERR_FUTURE;}
        // 获取提交指令
        String tCommand = RESOURCES_MANAGER.creatJobStep(tResource, "bash "+tTempScriptPath); // 使用 bash 执行不需要考虑权限的问题
        // 获取指令失败直接输出错误
        if (tCommand == null) {UT.Code.warning("Create SLURM job step Failed"); returnResource(tResource); return ERR_FUTURE;}
        // 任务完成后需要归还资源
        return new SRUNSystemFuture(tCommand, aWriteln, tResource);
    }
    private final class SRUNSystemFuture extends LocalSystemFuture implements IDoFinalFuture<Integer> {
        private final Slurm.Resource mResource;
        private SRUNSystemFuture(String aCommand, @NotNull IO.IWriteln aWriteln, Slurm.Resource aResource) {
            super(aCommand, aWriteln);
            mResource = aResource;
        }
        private volatile boolean mFinalDone = false;
        @Override public synchronized void doFinal() {
            if (mFinalDone) return;
            mFinalDone = true;
            returnResource(mResource);
        }
    }
    
    @Override protected void shutdownFinal() {mChecker.dispose();}
}
