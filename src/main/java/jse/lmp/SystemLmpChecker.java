package jse.lmp;

import jse.code.IO;
import jse.code.ReferenceChecker;
import jse.code.UT;

import static jse.code.Conf.WORKING_DIR_OF;

/**
 * 用来自动回收 {@link SystemLmpPotential} 创建的临时文件夹的检测器，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
class SystemLmpChecker extends ReferenceChecker {
    final String mWorkingDir;
    SystemLmpChecker(SystemLmpPotential aLmp) {
        super(aLmp);
        // 使用相对路径提高 exec 的兼容性
        mWorkingDir = WORKING_DIR_OF("LMP@"+ UT.Code.randID(), true);
    }
    
    @Override protected void dispose_() {
        try {
            IO.removeDir(mWorkingDir);
        } catch (Exception ignored) {}
    }
}
