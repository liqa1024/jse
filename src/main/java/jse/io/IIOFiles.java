package jse.io;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public interface IIOFiles {
    /** 提供一个拷贝方法解决引用返回值的问题 */
    IIOFiles copy();
    
    
    /** 获取输入输出的文件路径 */
    default String getIFile(String aIFileKey) {return getIFiles(aIFileKey).iterator().next();}
    default String getOFile(String aOFileKey) {return getOFiles(aOFileKey).iterator().next();}
    default String getIFile(String aIFileKey, int aIndex) {
        Iterable<String> tIFiles = getIFiles(aIFileKey);
        if (tIFiles instanceof List) return ((List<String>)tIFiles).get(aIndex);
        int tIdx = 0;
        for (String tIFile : tIFiles) {
            if (tIdx == aIndex) return tIFile;
            ++tIdx;
        }
        throw new IndexOutOfBoundsException("index: "+aIndex);
    }
    default String getOFile(String aOFileKey, int aIndex) {
        Iterable<String> tOFiles = getOFiles(aOFileKey);
        if (tOFiles instanceof List) return ((List<String>)tOFiles).get(aIndex);
        int tIdx = 0;
        for (String tOFile : tOFiles) {
            if (tIdx == aIndex) return tOFile;
            ++tIdx;
        }
        throw new IndexOutOfBoundsException("index: "+aIndex);
    }
    Iterable<String> getIFiles(String aIFileKey);
    Iterable<String> getOFiles(String aOFileKey);
    Iterable<String> getIFiles();
    Iterable<String> getOFiles();
    Iterable<String> getIFileKeys();
    Iterable<String> getOFileKeys();
    
    
    /**
     * 添加输入输出的文件路径，返回自身方便链式调用，输入参数格式为：
     * <p>
     * FileKey, FilePath, [start], [end]
     * <p>
     * 提供 [start], [end] 则认为 FilePath 有多个，名称为 ${InFilePath}-${i}, i 会从 start 依次增加到 end。
     * 注意这里由于是 java，约定默认 start 为 0，且 end 是不包含的，和其他的使用到 start 和 end 的操作保持一致
     * @author liqa
     */
    IIOFiles putIFiles(String aIFileKey, String aIFilePath                      );
    IIOFiles putIFiles(String aIFileKey, String aIFilePath, int aMultiple       );
    IIOFiles putIFiles(String aIFileKey, String aIFilePath, int aStart, int aEnd);
    IIOFiles putOFiles(String aOFileKey, String aOFilePath                      );
    IIOFiles putOFiles(String aOFileKey, String aOFilePath, int aMultiple       );
    IIOFiles putOFiles(String aOFileKey, String aOFilePath, int aStart, int aEnd);
    
    
    @VisibleForTesting default String i(String aIFileKey, int aIndex) {return getIFile(aIFileKey, aIndex);}
    @VisibleForTesting default String o(String aOFileKey, int aIndex) {return getOFile(aOFileKey, aIndex);}
    @VisibleForTesting default String i(String aIFileKey) {return getIFile(aIFileKey);}
    @VisibleForTesting default String o(String aOFileKey) {return getOFile(aOFileKey);}
    @VisibleForTesting default Iterable<String> i() {return getIFiles();}
    @VisibleForTesting default Iterable<String> o() {return getOFiles();}
    
    @VisibleForTesting IIOFiles i(String aIFileKey, String aIFilePath                      );
    @VisibleForTesting IIOFiles i(String aIFileKey, String aIFilePath, int aMultiple       );
    @VisibleForTesting IIOFiles i(String aIFileKey, String aIFilePath, int aStart, int aEnd);
    @VisibleForTesting IIOFiles o(String aOFileKey, String aOFilePath                      );
    @VisibleForTesting IIOFiles o(String aOFileKey, String aOFilePath, int aMultiple       );
    @VisibleForTesting IIOFiles o(String aOFileKey, String aOFilePath, int aStart, int aEnd);
}
