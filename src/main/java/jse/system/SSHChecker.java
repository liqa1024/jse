package jse.system;

import jse.code.ReferenceChecker;

/**
 * 用来自动回收 {@link SSHSystemExecutor} 内部 {@link SSHCore} 的检测器，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
class SSHChecker extends ReferenceChecker {
    final SSHCore mCore;
    SSHChecker(SSHSystemExecutor aSSH, SSHCore aCore) {
        super(aSSH);
        mCore = aCore;
    }
    
    @Override protected void dispose_() {
        // 虽然这个需要 ssh close 后并且等所有任务完成后才能合法调用；
        // 但实际调用此方法时 ssh 已经被垃圾回收，此时一定所有任务已经完成，因此永远合法
        mCore.close();
    }
}
