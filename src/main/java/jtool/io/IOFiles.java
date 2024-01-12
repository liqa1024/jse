package jtool.io;


import com.google.common.collect.ImmutableMap;
import jtool.code.collection.AbstractCollections;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

/**
 * @author liqa
 * <p> IIOFiles 的默认实现 </p>
 */
public class IOFiles implements IIOFiles {
    /** 全部遍历一次保证一定会值拷贝，String 也不会被修改因此不用考虑进一步值拷贝 */
    @Override public final IOFiles copy() {
        IOFiles rIOFiles = new IOFiles();
        for (Map.Entry<String, List<String>> tEntry : mIFiles.entrySet()) {
            List<String> tFiles = tEntry.getValue();
            List<String> rFiles = new ArrayList<>(tFiles.size());
            rFiles.addAll(tFiles);
            rIOFiles.mIFiles.put(tEntry.getKey(), rFiles);
        }
        for (Map.Entry<String, List<String>> tEntry : mOFiles.entrySet()) {
            List<String> tFileList = tEntry.getValue();
            List<String> rFileList = new ArrayList<>(tFileList.size());
            rFileList.addAll(tFileList);
            rIOFiles.mOFiles.put(tEntry.getKey(), rFileList);
        }
        return rIOFiles;
    }
    
    /** 重写实现自定义的 AbstractFilePathList，注意要嵌套使用方便重写 */
    protected List<String> toFilePathList(final String aFileKey, final String aFilePath) {return toFilePathList(aFileKey, aFilePath, 0, -1);}
    protected List<String> toFilePathList(final String aFileKey, final String aFilePath, final int aMultiple) {return toFilePathList(aFileKey, aFilePath, 0, aMultiple);}
    protected List<String> toFilePathList(final String aFileKey, final String aFilePath, final int aStart, final int aEnd) {
        return new AbstractFilePathList() {
            @Override public int start() {return aStart;}
            @Override public int end() {return aEnd;}
            @Override public String filePath() {return aFilePath;}
        };
    }
    
    
    private final Map<String, List<String>> mIFiles;
    private final Map<String, List<String>> mOFiles; // <FileKey, List<FilePath>>
    
    IOFiles(Map<String, List<String>> aIFiles, Map<String, List<String>> aOFiles) {mIFiles = aIFiles; mOFiles = aOFiles;}
    IOFiles() {this(new HashMap<>(), new HashMap<>());}
    /**
     * 现在对于 iofile 统一使用这种方式来获取，这里约定：
     * 对于实例类内部的静态方法来构造自身的，如果有输入参数，默认行为使用 of；
     * 对于实例类内部的静态方法来构造自身的，如果没有输入参数，默认行为使用 create；
     * 对于 xxxs 之类的工具类中的静态方法统一构造的，如果有输入参数，对于默认行为使用 from；
     * 对于 xxxs 之类的工具类中的静态方法统一构造的，如果没有输入参数，对于默认行为使用 get
     */
    public static IOFiles immutable() {return new IOFiles(ImmutableMap.of(), ImmutableMap.of());}
    public static IOFiles create() {return new IOFiles();}
    
    @Override public List<String> getIFiles(String aIFileKey) {return mIFiles.get(aIFileKey);}
    @Override public List<String> getOFiles(String aOFileKey) {return mOFiles.get(aOFileKey);}
    @Override public Iterable<String> getIFiles() {return AbstractCollections.merge(mIFiles.values());}
    @Override public Iterable<String> getOFiles() {return AbstractCollections.merge(mOFiles.values());}
    @Override public Iterable<String> getIFileKeys() {return mIFiles.keySet();}
    @Override public Iterable<String> getOFileKeys() {return mOFiles.keySet();}
    
    
    
    @Override public final IOFiles putIFiles(String aIFileKey, String aIFilePath                      ) {mIFiles.put(aIFileKey, toFilePathList(aIFileKey, aIFilePath              )); return this;}
    @Override public final IOFiles putIFiles(String aIFileKey, String aIFilePath, int aMultiple       ) {mIFiles.put(aIFileKey, toFilePathList(aIFileKey, aIFilePath, aMultiple   )); return this;}
    @Override public final IOFiles putIFiles(String aIFileKey, String aIFilePath, int aStart, int aEnd) {mIFiles.put(aIFileKey, toFilePathList(aIFileKey, aIFilePath, aStart, aEnd)); return this;}
    @Override public final IOFiles putOFiles(String aOFileKey, String aOFilePath                      ) {mOFiles.put(aOFileKey, toFilePathList(aOFileKey, aOFilePath              )); return this;}
    @Override public final IOFiles putOFiles(String aOFileKey, String aOFilePath, int aMultiple       ) {mOFiles.put(aOFileKey, toFilePathList(aOFileKey, aOFilePath, aMultiple   )); return this;}
    @Override public final IOFiles putOFiles(String aOFileKey, String aOFilePath, int aStart, int aEnd) {mOFiles.put(aOFileKey, toFilePathList(aOFileKey, aOFilePath, aStart, aEnd)); return this;}
    
    
    
    @VisibleForTesting @Override public final IOFiles i(String aIFileKey, String aIFilePath                      ) {return putIFiles(aIFileKey, aIFilePath              );}
    @VisibleForTesting @Override public final IOFiles i(String aIFileKey, String aIFilePath, int aMultiple       ) {return putIFiles(aIFileKey, aIFilePath, aMultiple   );}
    @VisibleForTesting @Override public final IOFiles i(String aIFileKey, String aIFilePath, int aStart, int aEnd) {return putIFiles(aIFileKey, aIFilePath, aStart, aEnd);}
    @VisibleForTesting @Override public final IOFiles o(String aOFileKey, String aOFilePath                      ) {return putOFiles(aOFileKey, aOFilePath              );}
    @VisibleForTesting @Override public final IOFiles o(String aOFileKey, String aOFilePath, int aMultiple       ) {return putOFiles(aOFileKey, aOFilePath, aMultiple   );}
    @VisibleForTesting @Override public final IOFiles o(String aOFileKey, String aOFilePath, int aStart, int aEnd) {return putOFiles(aOFileKey, aOFilePath, aStart, aEnd);}
}
