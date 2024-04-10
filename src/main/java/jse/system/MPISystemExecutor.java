package jse.system;

import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 执行 mpi 并行程序的执行器，会使用 mpiexec 来执行程序 </p>
 * <p> 目前不将指令写入 bash 之类的脚本保证全平台，但是同时也会导致不能执行复杂脚本 </p>
 */
public class MPISystemExecutor extends LocalSystemExecutor {
    private final int mProcessNum;
    public MPISystemExecutor(int aProcessNum) {
        super();
        mProcessNum = aProcessNum;
    }
    
    @Override protected String @NotNull[] programAndArgs_() {return new String[]{"mpiexec", "-np", String.valueOf(mProcessNum)};}
}
