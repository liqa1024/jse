package com.guan.io;

import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public interface IHasIOFiles {
    /** 提供一个拷贝方法解决引用返回值的问题 */
    IHasIOFiles copy();
    
    
    /** 获取输入输出的文件路径 */
    String getIFile(String aIFileKey);
    String getOFile(String aOFileKey);
    String getIFile(String aIFileKey, int aIndex);
    String getOFile(String aOFileKey, int aIndex);
    List<String> getIFiles(String aIFileKey);
    List<String> getOFiles(String aOFileKey);
    Iterable<String> getIFiles();
    Iterable<String> getOFiles();
    Iterable<String> getIFileKeys();
    Iterable<String> getOFileKeys();
    
    
    /**
     * 添加输入输出的文件路径，返回自身方便链式调用，
     * 输入参数是序列化的，方便使用，格式为：
     * <p>
     * FileKey1, FilePath1, [start1], [end1], FileKey2, FilePath2, [start2], [end2], ...
     * <p>
     * 提供 [start], [end] 则认为 FilePath 有多个，名称为 ${InFilePath}-${i}, i 会从 start 依次增加到 end。
     * 注意这里由于是 java，约定默认 start 为 0，且 end 是不包含的，和其他的使用到 start 和 end 的操作保持一致
     * @author liqa
     */
    IHasIOFiles putIFiles(String aIFileKey1, String aIFilePath1, Object... aElse       );
    IHasIOFiles putIFiles(String aIFileKey1, String aIFilePath1                        );
    IHasIOFiles putIFiles(String aIFileKey1, String aIFilePath1, int aMultiple1        );
    IHasIOFiles putIFiles(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1);
    IHasIOFiles putOFiles(String aOFileKey1, String aOFilePath1, Object... aElse       );
    IHasIOFiles putOFiles(String aOFileKey1, String aOFilePath1                        );
    IHasIOFiles putOFiles(String aOFileKey1, String aOFilePath1, int aMultiple1        );
    IHasIOFiles putOFiles(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1);
    
    
    @Deprecated default String i(String aIFileKey, int aIndex) {return getIFile(aIFileKey, aIndex);}
    @Deprecated default String o(String aOFileKey, int aIndex) {return getOFile(aOFileKey, aIndex);}
    @Deprecated default String i(String aIFileKey) {return getIFile(aIFileKey);}
    @Deprecated default String o(String aOFileKey) {return getOFile(aOFileKey);}
    @Deprecated default Iterable<String> i() {return getIFiles();}
    @Deprecated default Iterable<String> o() {return getOFiles();}
    
    @Deprecated IHasIOFiles i(String aIFileKey1, String aIFilePath1, Object... aElse       );
    @Deprecated IHasIOFiles i(String aIFileKey1, String aIFilePath1                        );
    @Deprecated IHasIOFiles i(String aIFileKey1, String aIFilePath1, int aMultiple1        );
    @Deprecated IHasIOFiles i(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1);
    @Deprecated IHasIOFiles o(String aOFileKey1, String aOFilePath1, Object... aElse       );
    @Deprecated IHasIOFiles o(String aOFileKey1, String aOFilePath1                        );
    @Deprecated IHasIOFiles o(String aOFileKey1, String aOFilePath1, int aMultiple1        );
    @Deprecated IHasIOFiles o(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1);
}
