package com.jtool.system;

import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class InternalSLURM extends InternalSLURMSystemExecutor {
    public InternalSLURM(int aTaskNum, int aParallelNum) throws Exception {super(aTaskNum, aParallelNum);}
    public InternalSLURM(int aTaskNum) throws Exception {super(aTaskNum);}
}
