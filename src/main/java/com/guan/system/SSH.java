package com.guan.system;

import java.util.Map;

@Deprecated
public final class SSH extends SSHSystemExecutor {
    public SSH(Map<?, ?> aArgs) throws Exception {super(aArgs);}
    public SSH(String aFilePath) throws Exception {super(aFilePath);}
    public SSH(String aFilePath, String aKey) throws Exception {super(aFilePath, aKey);}
    public SSH(int aThreadNum, Map<?, ?> aArgs) throws Exception {super(aThreadNum, aArgs);}
    public SSH(int aThreadNum, String aFilePath) throws Exception {super(aThreadNum, aFilePath);}
    public SSH(int aThreadNum, String aFilePath, String aKey) throws Exception {super(aThreadNum, aFilePath, aKey);}
    public SSH(int aThreadNum, int aIOThreadNum, Map<?, ?> aArgs) throws Exception {super(aThreadNum, aIOThreadNum, aArgs);}
    public SSH(int aThreadNum, int aIOThreadNum, String aFilePath) throws Exception {super(aThreadNum, aIOThreadNum, aFilePath);}
    public SSH(int aThreadNum, int aIOThreadNum, String aFilePath, String aKey) throws Exception {super(aThreadNum, aIOThreadNum, aFilePath, aKey);}
}
