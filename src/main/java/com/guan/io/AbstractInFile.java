package com.guan.io;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.*;

import static com.guan.code.CS.INFILE_SELF_KEY;


/**
 * @author liqa
 * <p> 输入文件 IInFile 的默认实现，自身文件的 key 为 {@code "<self>"} </p>
 * <p> 由于 Map 需要的接口更多，因此继承 AbstractMap 来减少重复代码的数量 </p>
 */
public abstract class AbstractInFile extends AbstractMap<String, Object> implements IInFile {
    /** Wrapper of IOFile and Map */
    private final IHasIOFiles mIOFiles;
    private final Map<String, Object> mSettings;
    /** Hooks, {FileKey, SettingKey} */
    private final BiMap<String, String> mIOFileMultipleKeys;
    private final BiMap<String, String> mIOFileStartKeys;
    private final BiMap<String, String> mIOFileEndKeys;
    
    public AbstractInFile() {this(new HashMap<>());}
    protected AbstractInFile(Map<String, Object> aSettings) {
        mSettings = aSettings;
        mIOFileMultipleKeys = HashBiMap.create();
        mIOFileStartKeys = HashBiMap.create();
        mIOFileEndKeys = HashBiMap.create();
        
        mIOFiles = new IOFiles() {
            /** 重写这这个方法来增加参数同步的 hook */
            @Override protected List<String> toFilePathList(final String aFileKey, final String aFilePath, final int aStart, final int aEnd) {
                return new AbstractFilePathList() {
                    /** start 和 end 的优先度高于 multiple */
                    @Override public int start() {
                        int tStart = aStart;
                        if (mIOFileMultipleKeys.containsKey(aFileKey)) tStart = 0;
                        if (mIOFileStartKeys.containsKey(aFileKey)) {
                            Object tValue = mSettings.get(mIOFileStartKeys.get(aFileKey));
                            if (tValue instanceof Number) tStart = ((Number)tValue).intValue();
                        }
                        return tStart;
                    }
                    @Override public int end() {
                        int tEnd = aEnd;
                        if (mIOFileMultipleKeys.containsKey(aFileKey)) {
                            Object tValue = mSettings.get(mIOFileMultipleKeys.get(aFileKey));
                            if (tValue instanceof Number) tEnd = ((Number)tValue).intValue();
                        }
                        if (mIOFileEndKeys.containsKey(aFileKey)) {
                            Object tValue = mSettings.get(mIOFileEndKeys.get(aFileKey));
                            if (tValue instanceof Number) tEnd = ((Number)tValue).intValue();
                        }
                        return tEnd;
                    }
                    @Override public String filePath() {
                        String tFilePath = aFilePath;
                        if (mSettings.containsKey(aFileKey)) {
                            // 注意 String 输入不一定真的是 String，这样可以兼容任何输入
                            tFilePath = String.valueOf(mSettings.get(aFileKey));
                        }
                        return tFilePath;
                    }
                };
            }
        };
    }
    
    
    /** IOFile stuffs */
    @Override public final IHasIOFiles copy() {return mIOFiles.copy();}
    
    
    @Override public final Iterable<String> getIFiles(String aIFileKey) {return mIOFiles.getIFiles(aIFileKey);}
    @Override public final Iterable<String> getOFiles(String aOFileKey) {return mIOFiles.getOFiles(aOFileKey);}
    @Override public final Iterable<String> getIFiles() {return mIOFiles.getIFiles();}
    @Override public final Iterable<String> getOFiles() {return mIOFiles.getOFiles();}
    @Override public final Iterable<String> getIFileKeys() {return mIOFiles.getIFileKeys();}
    @Override public final Iterable<String> getOFileKeys() {return mIOFiles.getOFileKeys();}
    
    @Override public final AbstractInFile putIFiles(String aIFileKey, String aIFilePath                      ) {mIOFiles.putIFiles(aIFileKey, aIFilePath              ); return this;}
    @Override public final AbstractInFile putIFiles(String aIFileKey, String aIFilePath, int aMultiple       ) {mIOFiles.putIFiles(aIFileKey, aIFilePath, aMultiple   ); return this;}
    @Override public final AbstractInFile putIFiles(String aIFileKey, String aIFilePath, int aStart, int aEnd) {mIOFiles.putIFiles(aIFileKey, aIFilePath, aStart, aEnd); return this;}
    @Override public final AbstractInFile putOFiles(String aOFileKey, String aOFilePath                      ) {mIOFiles.putOFiles(aOFileKey, aOFilePath              ); return this;}
    @Override public final AbstractInFile putOFiles(String aOFileKey, String aOFilePath, int aMultiple       ) {mIOFiles.putOFiles(aOFileKey, aOFilePath, aMultiple   ); return this;}
    @Override public final AbstractInFile putOFiles(String aOFileKey, String aOFilePath, int aStart, int aEnd) {mIOFiles.putOFiles(aOFileKey, aOFilePath, aStart, aEnd); return this;}
    
    @VisibleForTesting @Override public final AbstractInFile i(String aIFileKey, String aIFilePath                      ) {return putIFiles(aIFileKey, aIFilePath              );}
    @VisibleForTesting @Override public final AbstractInFile i(String aIFileKey, String aIFilePath, int aMultiple       ) {return putIFiles(aIFileKey, aIFilePath, aMultiple   );}
    @VisibleForTesting @Override public final AbstractInFile i(String aIFileKey, String aIFilePath, int aStart, int aEnd) {return putIFiles(aIFileKey, aIFilePath, aStart, aEnd);}
    @VisibleForTesting @Override public final AbstractInFile o(String aOFileKey, String aOFilePath                      ) {return putOFiles(aOFileKey, aOFilePath              );}
    @VisibleForTesting @Override public final AbstractInFile o(String aOFileKey, String aOFilePath, int aMultiple       ) {return putOFiles(aOFileKey, aOFilePath, aMultiple   );}
    @VisibleForTesting @Override public final AbstractInFile o(String aOFileKey, String aOFilePath, int aStart, int aEnd) {return putOFiles(aOFileKey, aOFilePath, aStart, aEnd);}
    
    
    /** Map stuffs */
    @NotNull @Override public final Set<Entry<String, Object>> entrySet() {return mSettings.entrySet();}
    @Override public final Object get(Object key) {return mSettings.get(key);}
    @Override public final boolean containsKey(Object key) {return mSettings.containsKey(key);}
    @Override public final Object remove(Object key) {return mSettings.remove(key);}
    @Override public final int size() {return mSettings.size();}
    @Override public final void clear() {mSettings.clear();}
    
    @Override public final Object put(String key, Object value) {return mSettings.put(key, value);}
    
    
    /** IInFile stuffs */
    @Override public final AbstractInFile setIOFilesMultipleKey(String aSettingKey, String aIOFilesKey) {mIOFileMultipleKeys.put(aIOFilesKey, aSettingKey); return this;}
    @Override public final AbstractInFile setIOFilesStartKey(String aSettingKey, String aIOFilesKey) {mIOFileStartKeys.put(aIOFilesKey, aSettingKey); return this;}
    @Override public final AbstractInFile setIOFileEndKey(String aSettingKey, String aIOFilesKey) {mIOFileEndKeys.put(aIOFilesKey, aSettingKey); return this;}
    
    
    @Override public final void write(String aPath) throws IOException {
        write_(aPath);
        putIFiles(INFILE_SELF_KEY, aPath);
    }
    
    /** stuff to override */
    protected abstract void write_(String aPath) throws IOException;
}
