package jse.lmp;

import jse.io.IIOFiles;
import jse.io.IInFile;
import jse.parallel.IHasAutoShutdown;
import jse.system.ISystemExecutor;

/**
 * 通用的 lammps 运行器
 * @author liqa
 */
public interface ILmpExecutor extends IHasAutoShutdown {
    ISystemExecutor exec();
    int run(IInFile aInFile);
    int run(String aInFile, IIOFiles aIOFiles);
    
    /** IHasAutoShutdown stuffs */
    ILmpExecutor setDoNotShutdown(boolean aDoNotShutdown);
}
