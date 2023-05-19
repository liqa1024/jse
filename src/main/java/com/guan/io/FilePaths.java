package com.guan.io;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/** 用来方便获取批量的文件路径 */
public class FilePaths {
    @VisibleForTesting public static List<String> get(final String aFilePath) {return get(aFilePath, 0, -1);}
    @VisibleForTesting public static List<String> get(final String aFilePath, final int aMultiple) {return get(aFilePath, 0, aMultiple);}
    public static List<String> get(final String aFilePath, final int aStart, final int aEnd) {
        return new AbstractFilePathList() {
            @Override public int start() {return aStart;}
            @Override public int end() {return aEnd;}
            @Override public String filePath() {return aFilePath;}
        };
    }
}
