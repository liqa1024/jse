package com.guan.io;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;


/**
 * @author liqa
 * <p> 输入文件 IInFile 的默认实现，自身文件的 key 为 {@code "<self>"} </p>
 * <p> 由于 Map 需要的接口更多，因此继承 AbstractMap 来减少重复代码的数量 </p>
 * <p> 为了实现方便，只支持添加和修改设置，而不支持移除已有的设置，如有需求可以直接创建新的 InFile </p>
 */
public abstract class AbstractInFile extends AbstractMap<String, Object> implements IInFile {
    /** Wrapper of IOFile and Map */
    private final IHasIOFiles mIOFiles;
    private final Map<String, Object> mSettings;
    /** Hooks */
    private final BiMap<String, String> mIOFileMultipleKeys;
    private final BiMap<String, String> mIOFileStartKeys;
    private final BiMap<String, String> mIOFileEndKeys;
    
    protected AbstractInFile(Map<String, Object> aSettings) {
        mIOFiles = new IOFiles();
        mSettings = aSettings;
        mIOFileMultipleKeys = HashBiMap.create();
        mIOFileStartKeys = HashBiMap.create();
        mIOFileEndKeys = HashBiMap.create();
    }
    public AbstractInFile() {this(new HashMap<>());}
    
    
    /** IOFile stuffs */
    @Override public final AbstractInFile putIFiles(String aIFileKey1, String aIFilePath1, Object... aElse) {mIOFiles.putIFiles(aIFileKey1, aIFilePath1, aElse); updateIFile_(); return this;}
    @Override public final AbstractInFile putOFiles(String aOFileKey1, String aOFilePath1, Object... aElse) {mIOFiles.putOFiles(aOFileKey1, aOFilePath1, aElse); updateOFile_(); return this;}
    
    @Override public final List<String> getIFiles(String aIFileKey) {return mIOFiles.getIFiles(aIFileKey);}
    @Override public final List<String> getOFiles(String aOFileKey) {return mIOFiles.getOFiles(aOFileKey);}
    @Override public final Iterable<String> getIFiles() {return mIOFiles.getIFiles();}
    @Override public final Iterable<String> getOFiles() {return mIOFiles.getOFiles();}
    @Override public final Iterable<String> getIFileKeys() {return mIOFiles.getIFileKeys();}
    @Override public final Iterable<String> getOFileKeys() {return mIOFiles.getOFileKeys();}
    
    @Override public final String getIFile(String aIFileKey) {return getIFiles(aIFileKey).get(0);}
    @Override public final String getOFile(String aOFileKey) {return getOFiles(aOFileKey).get(0);}
    @Override public final String getIFile(String aIFileKey, int aIndex) {return getIFiles(aIFileKey).get(aIndex);}
    @Override public final String getOFile(String aOFileKey, int aIndex) {return getOFiles(aOFileKey).get(aIndex);}
    
    @Override public final AbstractInFile setIFilePath    (String aIFileKey, String aIFilePath) {mIOFiles.setIFilePath    (aIFileKey, aIFilePath); return this;}
    @Override public final AbstractInFile setIFileSingle  (String aIFileKey                   ) {mIOFiles.setIFileSingle  (aIFileKey            ); return this;}
    @Override public final AbstractInFile setIFileStart   (String aIFileKey, int aStart       ) {mIOFiles.setIFileStart   (aIFileKey, aStart    ); return this;}
    @Override public final AbstractInFile setIFileEnd     (String aIFileKey, int aEnd         ) {mIOFiles.setIFileEnd     (aIFileKey, aEnd      ); return this;}
    @Override public final AbstractInFile setIFileMultiple(String aIFileKey, int aMultiple    ) {mIOFiles.setIFileMultiple(aIFileKey, aMultiple ); return this;}
    @Override public final AbstractInFile setOFilePath    (String aOFileKey, String aOFilePath) {mIOFiles.setOFilePath    (aOFileKey, aOFilePath); return this;}
    @Override public final AbstractInFile setOFileSingle  (String aOFileKey                   ) {mIOFiles.setOFileSingle  (aOFileKey            ); return this;}
    @Override public final AbstractInFile setOFileStart   (String aOFileKey, int aStart       ) {mIOFiles.setOFileStart   (aOFileKey, aStart    ); return this;}
    @Override public final AbstractInFile setOFileEnd     (String aOFileKey, int aEnd         ) {mIOFiles.setOFileEnd     (aOFileKey, aEnd      ); return this;}
    @Override public final AbstractInFile setOFileMultiple(String aOFileKey, int aMultiple    ) {mIOFiles.setOFileMultiple(aOFileKey, aMultiple ); return this;}
    
    @Override public final AbstractInFile putIFiles(String aIFileKey1, String aIFilePath1                        ) {return putIFiles(aIFileKey1, aIFilePath1, new Object[0]                );}
    @Override public final AbstractInFile putIFiles(String aIFileKey1, String aIFilePath1, int aMultiple1        ) {return putIFiles(aIFileKey1, aIFilePath1, new Object[] {aMultiple1    });}
    @Override public final AbstractInFile putIFiles(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1) {return putIFiles(aIFileKey1, aIFilePath1, new Object[] {aStart1, aEnd1});}
    @Override public final AbstractInFile putOFiles(String aOFileKey1, String aOFilePath1                        ) {return putOFiles(aOFileKey1, aOFilePath1, new Object[0]                );}
    @Override public final AbstractInFile putOFiles(String aOFileKey1, String aOFilePath1, int aMultiple1        ) {return putOFiles(aOFileKey1, aOFilePath1, new Object[] {aMultiple1    });}
    @Override public final AbstractInFile putOFiles(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1) {return putOFiles(aOFileKey1, aOFilePath1, new Object[] {aStart1, aEnd1});}
    
    @Deprecated @Override public final AbstractInFile i(String aIFileKey1, String aIFilePath1, Object... aElse       ) {return putIFiles(aIFileKey1, aIFilePath1, aElse);}
    @Deprecated @Override public final AbstractInFile i(String aIFileKey1, String aIFilePath1                        ) {return putIFiles(aIFileKey1, aIFilePath1                );}
    @Deprecated @Override public final AbstractInFile i(String aIFileKey1, String aIFilePath1, int aMultiple1        ) {return putIFiles(aIFileKey1, aIFilePath1, aMultiple1    );}
    @Deprecated @Override public final AbstractInFile i(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1) {return putIFiles(aIFileKey1, aIFilePath1, aStart1, aEnd1);}
    @Deprecated @Override public final AbstractInFile o(String aOFileKey1, String aOFilePath1, Object... aElse       ) {return putOFiles(aOFileKey1, aOFilePath1, aElse);}
    @Deprecated @Override public final AbstractInFile o(String aOFileKey1, String aOFilePath1                        ) {return putOFiles(aOFileKey1, aOFilePath1                );}
    @Deprecated @Override public final AbstractInFile o(String aOFileKey1, String aOFilePath1, int aMultiple1        ) {return putOFiles(aOFileKey1, aOFilePath1, aMultiple1    );}
    @Deprecated @Override public final AbstractInFile o(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1) {return putOFiles(aOFileKey1, aOFilePath1, aStart1, aEnd1);}
    
    
    /** Map stuffs */
    @NotNull @Override public final Set<Entry<String, Object>> entrySet() {
        // 使用这个方法来获取不能删除元素的 entrySet
        return new AbstractSet<Entry<String, Object>>() {
            private final Set<Entry<String, Object>> mSet = mSettings.entrySet();
            @Override public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<Entry<String, Object>>() {
                    private final Iterator<Entry<String, Object>> mIt = mSet.iterator();
                    @Override public boolean hasNext() {return mIt.hasNext();}
                    @Override public Entry<String, Object> next() {return mIt.next();}
                };
            }
            @Override public int size() {return mSet.size();}
            @Override public void clear() {throw new UnsupportedOperationException("clear");}
        };
    }
    @Override public final Object get(Object key) {return mSettings.get(key);}
    @Override public final boolean containsKey(Object key) {return mSettings.containsKey(key);}
    @Override public final Object remove(Object key) {throw new UnsupportedOperationException("remove");}
    @Override public final int size() {return mSettings.size();}
    @Override public final void clear() {throw new UnsupportedOperationException("clear");}
    
    @Override public final Object put(String key, Object value) {Object tObj = mSettings.put(key, value); updateSetting_(key); return tObj;}
    
    
    /** IInFile stuffs */
    @Override public final AbstractInFile setIOFilesMultipleKey(String aSettingKey, String aIOFilesKey) {mIOFileMultipleKeys.put(aSettingKey, aIOFilesKey); updateSetting_(aSettingKey); return this;}
    @Override public final AbstractInFile setIOFilesStartKey(String aSettingKey, String aIOFilesKey) {mIOFileStartKeys.put(aSettingKey, aIOFilesKey); updateSetting_(aSettingKey); return this;}
    @Override public final AbstractInFile setIOFileEndKey(String aSettingKey, String aIOFilesKey) {mIOFileEndKeys.put(aSettingKey, aIOFilesKey); updateSetting_(aSettingKey); return this;}
    
    
    
    /** 更新指定 key 的设定，会应用对应所有的 hook */
    protected void updateSetting_(String aSettingKey) {
        if (!containsKey(aSettingKey)) return;
        Object tValue = get(aSettingKey);
        // 如果是路径则更新对应路径
        if (tValue instanceof String) {
            setIFilePath(aSettingKey, (String)tValue);
            setOFilePath(aSettingKey, (String)tValue);
        }
        // 获取对应的 hook 设定，冲突时不做优先级保证
        if (tValue instanceof Number) if (mIOFileMultipleKeys.containsKey(aSettingKey)) {
            String tIOKey = mIOFileMultipleKeys.get(aSettingKey);
            setIFileMultiple(tIOKey, ((Number)tValue).intValue());
            setOFileMultiple(tIOKey, ((Number)tValue).intValue());
        }
        if (tValue instanceof Number) if (mIOFileStartKeys.containsKey(aSettingKey)) {
            String tIOKey = mIOFileStartKeys.get(aSettingKey);
            setIFileStart(tIOKey, ((Number)tValue).intValue());
            setOFileStart(tIOKey, ((Number)tValue).intValue());
        }
        if (tValue instanceof Number) if (mIOFileEndKeys.containsKey(aSettingKey)) {
            String tIOKey = mIOFileEndKeys.get(aSettingKey);
            setIFileEnd(tIOKey, ((Number)tValue).intValue());
            setOFileEnd(tIOKey, ((Number)tValue).intValue());
        }
    }
    protected void updateIFile_(String aIFileKey) {
        // 首先判断是否需要更新路径
        if (containsKey(aIFileKey)) {
            Object tValue = get(aIFileKey);
            if (tValue instanceof String) setIFilePath(aIFileKey, (String)tValue);
        }
        // 然后更新其他的 hooks
        if (mIOFileMultipleKeys.containsValue(aIFileKey)) {
            String tSettingKey = mIOFileMultipleKeys.inverse().get(aIFileKey);
            Object tValue = get(tSettingKey);
            if (tValue instanceof Number) setIFileMultiple(aIFileKey, ((Number)tValue).intValue());
        }
        if (mIOFileStartKeys.containsValue(aIFileKey)) {
            String tSettingKey = mIOFileStartKeys.inverse().get(aIFileKey);
            Object tValue = get(tSettingKey);
            if (tValue instanceof Number) setIFileStart(aIFileKey, ((Number)tValue).intValue());
        }
        if (mIOFileEndKeys.containsValue(aIFileKey)) {
            String tSettingKey = mIOFileEndKeys.inverse().get(aIFileKey);
            Object tValue = get(tSettingKey);
            if (tValue instanceof Number) setIFileEnd(aIFileKey, ((Number)tValue).intValue());
        }
    }
    protected void updateOFile_(String aOFileKey) {
        // 首先判断是否需要更新路径
        if (containsKey(aOFileKey)) {
            Object tValue = get(aOFileKey);
            if (tValue instanceof String) setOFilePath(aOFileKey, (String)tValue);
        }
        // 然后更新其他的 hooks
        if (mIOFileMultipleKeys.containsValue(aOFileKey)) {
            String tSettingKey = mIOFileMultipleKeys.inverse().get(aOFileKey);
            Object tValue = get(tSettingKey);
            if (tValue instanceof Number) setOFileMultiple(aOFileKey, ((Number)tValue).intValue());
        }
        if (mIOFileStartKeys.containsValue(aOFileKey)) {
            String tSettingKey = mIOFileStartKeys.inverse().get(aOFileKey);
            Object tValue = get(tSettingKey);
            if (tValue instanceof Number) setOFileStart(aOFileKey, ((Number)tValue).intValue());
        }
        if (mIOFileEndKeys.containsValue(aOFileKey)) {
            String tSettingKey = mIOFileEndKeys.inverse().get(aOFileKey);
            Object tValue = get(tSettingKey);
            if (tValue instanceof Number) setOFileEnd(aOFileKey, ((Number)tValue).intValue());
        }
    }
    protected void updateIFile_() {for (String tIFileKey : getIFileKeys()) updateIFile_(tIFileKey);}
    protected void updateOFile_() {for (String tOFileKey : getOFileKeys()) updateOFile_(tOFileKey);}
    
    
    @Override public final void write(String aPath) throws IOException {
        write_(aPath);
        putIFiles("<self>", aPath);
    }
    
    /** stuff to override */
    protected abstract void write_(String aPath) throws IOException;
}
