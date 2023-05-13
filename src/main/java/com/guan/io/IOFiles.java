package com.guan.io;


import com.guan.code.UT;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liqa
 * <p> IHasIOFiles 的默认实现 </p>
 */
public class IOFiles implements IHasIOFiles {
    
    @SuppressWarnings("UnusedReturnValue")
    protected static class FileList extends AbstractList<String> {
        protected int getStart() {return mStart;}
        protected int getEnd() {return mEnd;}
        protected String getFilePath() {return mFilePath;}
        protected FileList setStart(int aStart) {mStart = aStart; return this;}
        protected FileList setEnd(int aEnd) {mEnd = aEnd; return this;}
        protected FileList setFilePath(String aFilePath) {mFilePath = aFilePath; return this;}
        protected FileList setSingle() {mStart = 0; mEnd = -1; return this;}
        protected FileList setMultiple(int aMultiple) {mStart = 0; mEnd = aMultiple; return this;}
        
        private int mStart, mEnd;
        private String mFilePath;
        
        protected FileList(String aSinglePath) {
            mFilePath = aSinglePath;
            mStart = 0; mEnd = -1;
        }
        protected FileList(String aFilePath, int aMultiple) {
            mFilePath = aFilePath;
            mStart = 0; mEnd = aMultiple;
        }
        protected FileList(String aFilePath, int aStart, int aEnd) {
            mFilePath = aFilePath;
            mStart = aStart; mEnd = aEnd;
        }
        // 根据 end 来决定文件路径名称格式
        protected boolean isSinglePath() {return mEnd < 0;}
        protected boolean isMultiple() {return mEnd >= 0 && mStart == 0;}
        protected boolean isStartEnd() {return mStart > 0 && mEnd > 0;}
        
        
        /** List stuffs */
        @Override public String get(int index) {return isSinglePath() ? mFilePath : mFilePath+"-"+(mStart+index);}
        @Override public int size() {return isSinglePath() ? 1 : mEnd-mStart;}
    }
    
    
    private final Map<String, FileList> mIFiles;
    private final Map<String, FileList> mOFiles; // <FileKey, List<FilePath>>
    
    public IOFiles() {
        mIFiles = new HashMap<>();
        mOFiles = new HashMap<>();
    }
    
    @Override public final String getIFile(String aIFileKey) {return getIFiles(aIFileKey).get(0);}
    @Override public final String getOFile(String aOFileKey) {return getOFiles(aOFileKey).get(0);}
    @Override public final String getIFile(String aIFileKey, int aIndex) {return getIFiles(aIFileKey).get(aIndex);}
    @Override public final String getOFile(String aOFileKey, int aIndex) {return getOFiles(aOFileKey).get(aIndex);}
    
    @Override public FileList getIFiles(String aIFileKey) {return mIFiles.get(aIFileKey);}
    @Override public FileList getOFiles(String aOFileKey) {return mOFiles.get(aOFileKey);}
    @Override public Iterable<String> getIFiles() {return UT.Code.toIterable(mIFiles.values());}
    @Override public Iterable<String> getOFiles() {return UT.Code.toIterable(mOFiles.values());}
    @Override public Iterable<String> getIFileKeys() {return mIFiles.keySet();}
    @Override public Iterable<String> getOFileKeys() {return mOFiles.keySet();}
    
    
    @Override public IOFiles setIFilePath    (String aIFileKey, String aIFilePath) {if (mIFiles.containsKey(aIFileKey)) mIFiles.get(aIFileKey).setFilePath(aIFilePath); return this;}
    @Override public IOFiles setIFileSingle  (String aIFileKey                   ) {if (mIFiles.containsKey(aIFileKey)) mIFiles.get(aIFileKey).setSingle(); return this;}
    @Override public IOFiles setIFileStart   (String aIFileKey, int aStart       ) {if (mIFiles.containsKey(aIFileKey)) mIFiles.get(aIFileKey).setStart(aStart); return this;}
    @Override public IOFiles setIFileEnd     (String aIFileKey, int aEnd         ) {if (mIFiles.containsKey(aIFileKey)) mIFiles.get(aIFileKey).setEnd(aEnd); return this;}
    @Override public IOFiles setIFileMultiple(String aIFileKey, int aMultiple    ) {if (mIFiles.containsKey(aIFileKey)) mIFiles.get(aIFileKey).setMultiple(aMultiple); return this;}
    @Override public IOFiles setOFilePath    (String aOFileKey, String aOFilePath) {if (mOFiles.containsKey(aOFileKey)) mOFiles.get(aOFileKey).setFilePath(aOFilePath); return this;}
    @Override public IOFiles setOFileSingle  (String aOFileKey                   ) {if (mOFiles.containsKey(aOFileKey)) mOFiles.get(aOFileKey).setSingle(); return this;}
    @Override public IOFiles setOFileStart   (String aOFileKey, int aStart       ) {if (mOFiles.containsKey(aOFileKey)) mOFiles.get(aOFileKey).setStart(aStart); return this;}
    @Override public IOFiles setOFileEnd     (String aOFileKey, int aEnd         ) {if (mOFiles.containsKey(aOFileKey)) mOFiles.get(aOFileKey).setEnd(aEnd); return this;}
    @Override public IOFiles setOFileMultiple(String aOFileKey, int aMultiple    ) {if (mOFiles.containsKey(aOFileKey)) mOFiles.get(aOFileKey).setMultiple(aMultiple); return this;}
    
    
    @Override public final IOFiles putIFiles(String aIFileKey1, String aIFilePath1                        ) {return putIFiles(aIFileKey1, aIFilePath1, new Object[0]                );}
    @Override public final IOFiles putIFiles(String aIFileKey1, String aIFilePath1, int aMultiple1        ) {return putIFiles(aIFileKey1, aIFilePath1, new Object[] {aMultiple1    });}
    @Override public final IOFiles putIFiles(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1) {return putIFiles(aIFileKey1, aIFilePath1, new Object[] {aStart1, aEnd1});}
    @Override public final IOFiles putOFiles(String aOFileKey1, String aOFilePath1                        ) {return putOFiles(aOFileKey1, aOFilePath1, new Object[0]                );}
    @Override public final IOFiles putOFiles(String aOFileKey1, String aOFilePath1, int aMultiple1        ) {return putOFiles(aOFileKey1, aOFilePath1, new Object[] {aMultiple1    });}
    @Override public final IOFiles putOFiles(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1) {return putOFiles(aOFileKey1, aOFilePath1, new Object[] {aStart1, aEnd1});}
    
    
    @Override public IOFiles putIFiles(String aIFileKey1, String aIFilePath1, Object... aElse) {scanAndAddFiles2Dest(mIFiles, UT.Code.merge(aIFileKey1, aIFilePath1, aElse)); return this;}
    @Override public IOFiles putOFiles(String aOFileKey1, String aOFilePath1, Object... aElse) {scanAndAddFiles2Dest(mOFiles, UT.Code.merge(aOFileKey1, aOFilePath1, aElse)); return this;}
    
    
    private static void scanAndAddFiles2Dest(Map<String, FileList> rDest, List<Object> aFiles) {
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
                rDest.put(tKey, new FileList(tPath));
            } else if (tStart == null) {
                rDest.put(tKey, new FileList(tPath, tEnd));
            } else {
                rDest.put(tKey, new FileList(tPath, tStart, tEnd));
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
