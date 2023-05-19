package com.jtool.system;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;

@VisibleForTesting
public final class SLURM extends SLURMSystemExecutor {
    public SLURM(Map<?, ?> aArgs) throws Exception {super(aArgs);}
    public SLURM(int aParallelNum, Map<?, ?> aArgs) throws Exception {super(aParallelNum, aArgs);}
    public SLURM(int aParallelNum, int aIOThreadNum, Map<?, ?> aArgs) throws Exception {super(aParallelNum, aIOThreadNum, aArgs);}
    public SLURM(int aParallelNum, int aIOThreadNum, String aPartition, Map<?, ?> aArgs) throws Exception {super(aParallelNum, aIOThreadNum, aPartition, aArgs);}
    public SLURM(int aParallelNum, int aIOThreadNum, String aPartition, int aTaskNum, int aMaxTaskNumPerNode, Map<?, ?> aArgs) throws Exception {super(aParallelNum, aIOThreadNum, aPartition, aTaskNum, aMaxTaskNumPerNode, aArgs);}
    public SLURM(String aPartition, int aTaskNum, int aMaxTaskNumPerNode, Map<?, ?> aArgs) throws Exception {super(aPartition, aTaskNum, aMaxTaskNumPerNode, aArgs);}
    public SLURM(String aPartition, Map<?, ?> aArgs) throws Exception {super(aPartition, aArgs);}
}
