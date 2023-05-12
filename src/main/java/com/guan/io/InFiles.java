package com.guan.io;


import com.google.common.collect.ImmutableMap;
import com.guan.code.UT;
import com.guan.lmp.LmpIn;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * @author liqa
 * <p> 提供一些常用的输入文件的获取 </p>
 */
public class InFiles {
    private static abstract class ImmutableInFile extends AbstractMap<String, Object> implements IInFile {
        private final static Map<String, Object> ZL_SETTING = ImmutableMap.of();
        private final IHasIOFiles mIOFile;
        public ImmutableInFile() {mIOFile = new IOFiles();}
        
        /** IOFile stuffs */
        @Override public final Iterable<String> getIFiles(String aIFileKey) {return mIOFile.getIFiles(aIFileKey);}
        @Override public final Iterable<String> getOFiles(String aOFileKey) {return mIOFile.getOFiles(aOFileKey);}
        @Override public final Iterable<String> getIFiles() {return mIOFile.getIFiles();}
        @Override public final Iterable<String> getOFiles() {return mIOFile.getOFiles();}
        @Override public final IHasIOFiles setIFiles(String aIFileKey1, String aIFilePath1, Object... aElse) {mIOFile.setIFiles(aIFileKey1, aIFilePath1, aElse); return this;}
        @Override public final IHasIOFiles setOFiles(String aOFileKey1, String aOFilePath1, Object... aElse) {mIOFile.setOFiles(aOFileKey1, aOFilePath1, aElse); return this;}
        
        /** Map stuffs */
        @NotNull @Override public final Set<Entry<String, Object>> entrySet() {return ZL_SETTING.entrySet();}
    }
    
    
    public static IInFile lmp(String aLmpInFilePath) {return LmpIn.custom(aLmpInFilePath);}
    public static IInFile json(final String aJsonFilePath) {return new AbstractInFileJson() {@Override protected Reader getInFileReader() throws IOException {return UT.IO.toReader(aJsonFilePath);}};}
    public static IInFile immutable(final String aInFilePath) {return new ImmutableInFile() {@Override public void write(String aPath) throws IOException {UT.IO.copy(aInFilePath, aPath);}};}
    
    /** 默认行为 */
    @Deprecated public static IInFile get(String aInFilePath) {return immutable(aInFilePath);}
}
