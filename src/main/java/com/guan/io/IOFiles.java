package com.guan.io;


import com.guan.code.UT;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

/**
 * @author liqa
 * <p> IHasIOFiles 的默认实现 </p>
 */
public class IOFiles implements IHasIOFiles {
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
    
    /** 重写实现自定义的 AbstractFilePathList */
    protected List<String> toFilePathList(final String aFileKey, final String aFilePath) {return FilePaths.get(aFilePath);}
    protected List<String> toFilePathList(final String aFileKey, final String aFilePath, final int aMultiple) {return FilePaths.get(aFilePath, aMultiple);}
    protected List<String> toFilePathList(final String aFileKey, final String aFilePath, final int aStart, final int aEnd) {return FilePaths.get(aFilePath, aStart, aEnd);}
    
    
    private final Map<String, List<String>> mIFiles;
    private final Map<String, List<String>> mOFiles; // <FileKey, List<FilePath>>
    
    public IOFiles() {
        mIFiles = new HashMap<>();
        mOFiles = new HashMap<>();
    }
    
    
    @Override public List<String> getIFiles(String aIFileKey) {return mIFiles.get(aIFileKey);}
    @Override public List<String> getOFiles(String aOFileKey) {return mOFiles.get(aOFileKey);}
    @Override public Iterable<String> getIFiles() {return UT.Code.toIterable(mIFiles.values());}
    @Override public Iterable<String> getOFiles() {return UT.Code.toIterable(mOFiles.values());}
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
