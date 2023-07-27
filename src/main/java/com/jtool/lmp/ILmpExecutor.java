package com.jtool.lmp;

import com.jtool.iofile.IHasIOFiles;
import com.jtool.iofile.IInFile;
import com.jtool.parallel.IHasAutoShutdown;
import com.jtool.system.ISystemExecutor;

/**
 * 通用的 lammps 运行器
 * @author liqa
 */
public interface ILmpExecutor extends IHasAutoShutdown {
    ISystemExecutor exec();
    int run(IInFile aInFile);
    int run(String aInFile, IHasIOFiles aIOFiles);
}
