package jse.system;

import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 执行 mpi 并行程序的执行器，会使用 mpiexec 来执行程序，并且强制设置资源分配器为 user 来绕开 slurm 的资源分配 </p>
 */
public class MPISystemExecutor extends LocalSystemExecutor {
    private final int mProcessNum;
    public MPISystemExecutor(int aProcessNum) {
        super();
        mProcessNum = aProcessNum;
    }
    
    @Override protected String @NotNull[] programAndArgs_() {return new String[]{"mpiexec", "-np", String.valueOf(mProcessNum)};}
}
