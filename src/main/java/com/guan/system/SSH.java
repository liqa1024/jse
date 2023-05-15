package com.guan.system;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;

@VisibleForTesting
public final class SSH extends SSHSystemExecutor {
    public SSH(Map<?, ?> aArgs) throws Exception {super(aArgs);}
    public SSH(int aThreadNum, Map<?, ?> aArgs) throws Exception {super(aThreadNum, aArgs);}
    public SSH(int aThreadNum, int aIOThreadNum, Map<?, ?> aArgs) throws Exception {super(aThreadNum, aIOThreadNum, aArgs);}
}
