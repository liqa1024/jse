package com.guan.io;

import java.util.List;

public interface IHasIOFiles {
    /** 获取输入输出的文件路径 */
    List<String> getIFiles(String aIFileKey);
    List<String> getOFiles(String aOFileKey);
    Iterable<String> getIFiles();
    Iterable<String> getOFiles();
    
    /**
     * 设置输入输出的文件路径，返回自身方便链式调用，
     * 输入参数是序列化的，方便使用，格式为：
     * <p>
     * FileKey1, FilePath1, [start1], [end1], FileKey2, FilePath2, [start2], [end2], ...
     * <p>
     * 提供 [start], [end] 则认为 FilePath 有多个，名称为 ${InFilePath}-${i}, i 会从 start 依次增加到 end。
     * 注意这里由于是 java，约定默认 start 为 0，且 end 是不包含的，和其他的使用到 start 和 end 的操作保持一致
     * @author liqa
     */
            IHasIOFiles setIFiles(String aIFileKey1, String aIFilePath1, Object... aElse       );
    default IHasIOFiles setIFiles(String aIFileKey1, String aIFilePath1                        ) {return setIFiles(aIFileKey1, aIFilePath1, new Object[0]                );}
    default IHasIOFiles setIFiles(String aIFileKey1, String aIFilePath1, int aMultiple1        ) {return setIFiles(aIFileKey1, aIFilePath1, new Object[] {aMultiple1    });}
    default IHasIOFiles setIFiles(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1) {return setIFiles(aIFileKey1, aIFilePath1, new Object[] {aStart1, aEnd1});}
            IHasIOFiles setOFiles(String aOFileKey1, String aOFilePath1, Object... aElse       );
    default IHasIOFiles setOFiles(String aOFileKey1, String aOFilePath1                        ) {return setOFiles(aOFileKey1, aOFilePath1, new Object[0]                );}
    default IHasIOFiles setOFiles(String aOFileKey1, String aOFilePath1, int aMultiple1        ) {return setOFiles(aOFileKey1, aOFilePath1, new Object[] {aMultiple1    });}
    default IHasIOFiles setOFiles(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1) {return setOFiles(aOFileKey1, aOFilePath1, new Object[] {aStart1, aEnd1});}
    
    
    @Deprecated default List<String> i(String aIFileKey) {return getIFiles(aIFileKey);}
    @Deprecated default List<String> o(String aOFileKey) {return getOFiles(aOFileKey);}
    @Deprecated default Iterable<String> i() {return getIFiles();}
    @Deprecated default Iterable<String> o() {return getOFiles();}
    
    @Deprecated default IHasIOFiles i(String aIFileKey1, String aIFilePath1, Object... aElse       ) {return setIFiles(aIFileKey1, aIFilePath1, aElse);}
    @Deprecated default IHasIOFiles i(String aIFileKey1, String aIFilePath1                        ) {return setIFiles(aIFileKey1, aIFilePath1                );}
    @Deprecated default IHasIOFiles i(String aIFileKey1, String aIFilePath1, int aMultiple1        ) {return setIFiles(aIFileKey1, aIFilePath1, aMultiple1    );}
    @Deprecated default IHasIOFiles i(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1) {return setIFiles(aIFileKey1, aIFilePath1, aStart1, aEnd1);}
    @Deprecated default IHasIOFiles o(String aOFileKey1, String aOFilePath1, Object... aElse       ) {return setOFiles(aOFileKey1, aOFilePath1, aElse);}
    @Deprecated default IHasIOFiles o(String aOFileKey1, String aOFilePath1                        ) {return setOFiles(aOFileKey1, aOFilePath1                );}
    @Deprecated default IHasIOFiles o(String aOFileKey1, String aOFilePath1, int aMultiple1        ) {return setOFiles(aOFileKey1, aOFilePath1, aMultiple1    );}
    @Deprecated default IHasIOFiles o(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1) {return setOFiles(aOFileKey1, aOFilePath1, aStart1, aEnd1);}
}
