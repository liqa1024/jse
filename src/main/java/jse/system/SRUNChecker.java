package jse.system;

import jse.code.IO;
import jse.code.OS.Slurm;
import jse.code.ReferenceChecker;
import jse.code.UT;

import java.io.IOException;
import java.util.Map;

import static jse.code.Conf.WORKING_DIR_OF;
import static jse.code.OS.Slurm.RESOURCES_MANAGER;

/**
 * 用来自动回收 {@link SRUNSystemExecutor} 创建的临时文件夹以及创建资源的检测器，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
class SRUNChecker extends ReferenceChecker {
    final String mWorkingDir;
    final Map<Slurm.Resource, Boolean> mAssignedResources;
    SRUNChecker(SRUNSystemExecutor aSRUN, Map<Slurm.Resource, Boolean> aAssignedResources) {
        super(aSRUN);
        mAssignedResources = aAssignedResources;
        // 设置一下工作目录；虽然非强制，这里还是使用相对路径保证 removeDir(mWorkingDir); 合法
        mWorkingDir = WORKING_DIR_OF("SRUN@"+ UT.Code.randID(), true);
    }
    
    @Override protected void dispose_() throws IOException {
        // 虽然这个需要 srun close 后并且等所有任务完成后才能合法调用；
        // 但实际调用此方法时 srun 已经被垃圾回收，此时一定所有任务已经完成，因此永远合法
        IO.removeDir(mWorkingDir);
        for (Slurm.Resource tResource : mAssignedResources.keySet()) RESOURCES_MANAGER.returnResource(tResource);
    }
}
