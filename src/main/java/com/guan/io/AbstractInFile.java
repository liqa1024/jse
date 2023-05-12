package com.guan.io;


import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;


/**
 * @author liqa
 * <p> 输入文件 IInFile 的默认实现，自身文件的 key 为 {@code "<self>"} </p>
 * <p> 由于 Map 需要的接口更多，因此继承 AbstractMap 来减少重复代码的数量 </p>
 */
public abstract class AbstractInFile extends AbstractMap<String, Object> implements IInFile {
    /** Wrapper of IOFile and Map */
    private final IHasIOFiles mIOFile;
    private final Map<String, Object> mSettings;
    public AbstractInFile() {
        mIOFile = new IOFiles();
        mSettings = new HashMap<>();
    }
    
    
    /** IOFile stuffs */
    @Override public final List<String> getIFiles(String aIFileKey) {return mIOFile.getIFiles(aIFileKey);}
    @Override public final List<String> getOFiles(String aOFileKey) {return mIOFile.getOFiles(aOFileKey);}
    @Override public final Iterable<String> getIFiles() {return mIOFile.getIFiles();}
    @Override public final Iterable<String> getOFiles() {return mIOFile.getOFiles();}
    @Override public final IHasIOFiles setIFiles(String aIFileKey1, String aIFilePath1, Object... aElse) {mIOFile.setIFiles(aIFileKey1, aIFilePath1, aElse); return this;}
    @Override public final IHasIOFiles setOFiles(String aOFileKey1, String aOFilePath1, Object... aElse) {mIOFile.setOFiles(aOFileKey1, aOFilePath1, aElse); return this;}
    
    /** Map stuffs */
    @NotNull @Override public final Set<Entry<String, Object>> entrySet() {return mSettings.entrySet();}
    @Override public final Object put(String key, Object value) {return mSettings.put(key, value);}
    @Override public final Object get(Object key) {return mSettings.get(key);}
    @Override public final Object remove(Object key) {return mSettings.remove(key);}
    @Override public final int size() {return mSettings.size();}
    @Override public final void clear() {mSettings.clear();}
    
    /** IInFile stuffs */
    public final void write(String aPath) throws IOException {
        write_(aPath);
        setIFiles("<self>", aPath);
    }
    
    /** stuff to override */
    protected abstract void write_(String aPath) throws IOException;
}
