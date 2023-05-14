package com.guan.io;


import com.guan.code.UT;

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
            List<String> tFileList = tEntry.getValue();
            List<String> rFileList = new ArrayList<>(tFileList.size());
            rFileList.addAll(tFileList);
            rIOFiles.mIFiles.put(tEntry.getKey(), rFileList);
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
    
    public IOFiles() {
        mIFiles = new HashMap<>();
        mOFiles = new HashMap<>();
    }
    
    @Override public final String getIFile(String aIFileKey) {return getIFiles(aIFileKey).get(0);}
    @Override public final String getOFile(String aOFileKey) {return getOFiles(aOFileKey).get(0);}
    @Override public final String getIFile(String aIFileKey, int aIndex) {return getIFiles(aIFileKey).get(aIndex);}
    @Override public final String getOFile(String aOFileKey, int aIndex) {return getOFiles(aOFileKey).get(aIndex);}
    
    @Override public List<String> getIFiles(String aIFileKey) {return mIFiles.get(aIFileKey);}
    @Override public List<String> getOFiles(String aOFileKey) {return mOFiles.get(aOFileKey);}
    @Override public Iterable<String> getIFiles() {return UT.Code.toIterable(mIFiles.values());}
    @Override public Iterable<String> getOFiles() {return UT.Code.toIterable(mOFiles.values());}
    @Override public Iterable<String> getIFileKeys() {return mIFiles.keySet();}
    @Override public Iterable<String> getOFileKeys() {return mOFiles.keySet();}
    
    
    
    @Override public final IOFiles putIFiles(String aIFileKey1, String aIFilePath1                        ) {return putIFiles(aIFileKey1, aIFilePath1, new Object[0]                );}
    @Override public final IOFiles putIFiles(String aIFileKey1, String aIFilePath1, int aMultiple1        ) {return putIFiles(aIFileKey1, aIFilePath1, new Object[] {aMultiple1    });}
    @Override public final IOFiles putIFiles(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1) {return putIFiles(aIFileKey1, aIFilePath1, new Object[] {aStart1, aEnd1});}
    @Override public final IOFiles putOFiles(String aOFileKey1, String aOFilePath1                        ) {return putOFiles(aOFileKey1, aOFilePath1, new Object[0]                );}
    @Override public final IOFiles putOFiles(String aOFileKey1, String aOFilePath1, int aMultiple1        ) {return putOFiles(aOFileKey1, aOFilePath1, new Object[] {aMultiple1    });}
    @Override public final IOFiles putOFiles(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1) {return putOFiles(aOFileKey1, aOFilePath1, new Object[] {aStart1, aEnd1});}
    
    
    @Override public IOFiles putIFiles(String aIFileKey1, String aIFilePath1, Object... aElse) {scanAndAddFiles2Dest(mIFiles, UT.Code.merge(aIFileKey1, aIFilePath1, aElse)); return this;}
    @Override public IOFiles putOFiles(String aOFileKey1, String aOFilePath1, Object... aElse) {scanAndAddFiles2Dest(mOFiles, UT.Code.merge(aOFileKey1, aOFilePath1, aElse)); return this;}
    
    
    private void scanAndAddFiles2Dest(Map<String, List<String>> rDest, List<Object> aFiles) {
        int idx = 0;
        int tSize = aFiles.size();
        while (idx < tSize) {
            // 获取 key
            Object
            tNext = aFiles.get(idx); ++idx;
            if (!(tNext instanceof String)) continue;
            String tKey = (String)tNext;
            // 获取 path
            tNext = idx<tSize ? aFiles.get(idx) : null; ++idx;
            if (!(tNext instanceof String)) continue;
            String tPath = (String)tNext;
            // 通过检测后两个来获取可选的 start 和 end
            Integer tStart = null, tEnd = null;
            tNext = idx<tSize ? aFiles.get(idx) : null;
            if (tNext instanceof Number) {
                tEnd = ((Number)tNext).intValue();
                ++idx;
                tNext = idx<tSize ? aFiles.get(idx) : null;
                if (tNext instanceof Number) {
                    tStart = tEnd;
                    tEnd = ((Number)tNext).intValue();
                    ++idx;
                }
            }
            // 不在这里判断 End 和类型的关系
            if (tEnd == null) {
                rDest.put(tKey, toFilePathList(tKey, tPath));
            } else if (tStart == null) {
                rDest.put(tKey, toFilePathList(tKey, tPath, tEnd));
            } else {
                rDest.put(tKey, toFilePathList(tKey, tPath, tStart, tEnd));
            }
        }
    }
    
    
    @Deprecated @Override public final IOFiles i(String aIFileKey1, String aIFilePath1, Object... aElse       ) {return putIFiles(aIFileKey1, aIFilePath1, aElse);}
    @Deprecated @Override public final IOFiles i(String aIFileKey1, String aIFilePath1                        ) {return putIFiles(aIFileKey1, aIFilePath1                );}
    @Deprecated @Override public final IOFiles i(String aIFileKey1, String aIFilePath1, int aMultiple1        ) {return putIFiles(aIFileKey1, aIFilePath1, aMultiple1    );}
    @Deprecated @Override public final IOFiles i(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1) {return putIFiles(aIFileKey1, aIFilePath1, aStart1, aEnd1);}
    @Deprecated @Override public final IOFiles o(String aOFileKey1, String aOFilePath1, Object... aElse       ) {return putOFiles(aOFileKey1, aOFilePath1, aElse);}
    @Deprecated @Override public final IOFiles o(String aOFileKey1, String aOFilePath1                        ) {return putOFiles(aOFileKey1, aOFilePath1                );}
    @Deprecated @Override public final IOFiles o(String aOFileKey1, String aOFilePath1, int aMultiple1        ) {return putOFiles(aOFileKey1, aOFilePath1, aMultiple1    );}
    @Deprecated @Override public final IOFiles o(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1) {return putOFiles(aOFileKey1, aOFilePath1, aStart1, aEnd1);}
}
