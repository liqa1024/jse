package jse.system;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class SRUN extends SRUNSystemExecutor {
    public SRUN(int aTaskNum, int aParallelNum) throws Exception {super(aTaskNum, aParallelNum);}
    public SRUN(int aTaskNum) throws Exception {super(aTaskNum);}
    public SRUN() throws Exception {super();}
}
