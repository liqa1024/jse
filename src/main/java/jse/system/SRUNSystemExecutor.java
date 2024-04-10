package jse.system;

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
import static jse.code.Conf.WORKING_DIR_OF;

/**
 * @author liqa
 * <p> 在 SLURM 内部提交子任务的提交器，主要用于 salloc 或者 sbatch 一个 jse 任务后，
 * 在提交的 jse 任务中提交子任务；因此认为此时已经有了 SLURM 的任务环境 </p>
 * <p> 由于是提交子任务的形式，这里依旧使用 java 线程池来提交后台任务 </p>
 */
public class SRUNSystemExecutor extends LocalSystemExecutor {
    private final String mWorkingDir;
    private final Map<Slurm.Resource, Boolean> mAssignedResources;
    
    /** 线程数现在由每个任务的并行数，申请到的节点数，以及每节点的核心数来确定 */
    public SRUNSystemExecutor(int aTaskNum, int aMaxParallelNum) throws Exception {
        super();
        
        mAssignedResources = new HashMap<>();
        // 设置一下工作目录；虽然非强制，这里还是使用相对路径保证 if (needSyncIOFiles()) removeDir(mWorkingDir); 合法
        mWorkingDir = UT.IO.toRelativePath(WORKING_DIR_OF("SRUN@"+UT.Code.randID()));
        // 由于是本地的，这里不需要创建文件夹
        // 仅 slurm 可用
        if (!IS_SLURM) {
            this.shutdown();
            throw new Exception("SRUN can Only be used in SLURM");
        }
        for (int i = 0; i < aMaxParallelNum; ++i) {
            Slurm.Resource tResource = aTaskNum>0 ? RESOURCES_MANAGER.assignResource(aTaskNum) : RESOURCES_MANAGER.assignResource();
            // 分配失败直接抛出错误
            if (tResource == null) {
                this.shutdown();
                throw new Exception("Not enough resource in SLURM to assign");
            }
            mAssignedResources.put(tResource, false);
        }
    }
    public SRUNSystemExecutor(int aTaskNum) throws Exception {this(aTaskNum, 1);}
    public SRUNSystemExecutor() throws Exception {this(-1, 1);}
    
    /** 内部使用的向任务分配节点的方法 */
    private synchronized @Nullable Slurm.Resource assignResource() {
        for (Map.Entry<Slurm.Resource, Boolean> tEntry : mAssignedResources.entrySet()) {
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
        mAssignedResources.put(aResource, false);
    }
    
    @Override protected Future<Integer> submitSystem__(String aCommand, @NotNull UT.IO.IWriteln aWriteln) {
        // 对于空指令专门优化，不执行操作
        if (aCommand == null || aCommand.isEmpty()) return SUC_FUTURE;
        // 先尝试获取节点
        Slurm.Resource tResource = assignResource();
        if (tResource == null && !noERROutput()) {
            System.err.println("WARNING: Can NOT to assign resource for this job temporarily, this job blocks until there are any free resource.");
            System.err.println("It may be caused by too large number of parallels.");
        }
        while (tResource == null) {
            try {Thread.sleep(FILE_SYSTEM_SLEEP_TIME);}
            catch (InterruptedException e) {printStackTrace(e); return ERR_FUTURE;}
            tResource = assignResource();
        }
        // 为了兼容性，需要将实际需要执行的脚本写入 bash 后再执行，从而可以支持复杂的逻辑输入（包含 cd 等操作或者多行命令）
        String tTempScriptPath = mWorkingDir+UT.Code.randID()+".sh";
        try {UT.IO.write(tTempScriptPath, "#!/bin/bash\n"+aCommand);}
        catch (Exception e) {printStackTrace(e); returnResource(tResource); return ERR_FUTURE;}
        // 获取提交指令
        String tCommand = RESOURCES_MANAGER.creatJobStep(tResource, "bash "+tTempScriptPath); // 使用 bash 执行不需要考虑权限的问题
        // 获取指令失败直接输出错误
        if (tCommand == null) {System.err.println("ERROR: Create SLURM job step Failed"); returnResource(tResource); return ERR_FUTURE;}
        // 任务完成后需要归还任务
        final Slurm.Resource fResource = tResource;
        return toSystemFuture(super.submitSystem__(tCommand, aWriteln), () -> returnResource(fResource));
    }
    
    /** 程序结束时删除自己的临时工作目录，并归还资源 */
    @Override protected void shutdownFinal() {
        try {
            UT.IO.removeDir(mWorkingDir);
            if (needSyncIOFiles()) removeDir(mWorkingDir);
        } catch (Exception ignored) {}
        for (Slurm.Resource tResource : mAssignedResources.keySet()) RESOURCES_MANAGER.returnResource(tResource);
    }
}
