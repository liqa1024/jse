package com.jtool.io;


import com.google.common.collect.ImmutableMap;
import com.jtool.code.UT;
import com.jtool.lmp.LmpIn;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.Reader;

/**
 * @author liqa
 * <p> 提供一些常用的输入文件的获取 </p>
 */
public class InFiles {
    public static IInFile lmp(String aLmpInFilePath) {return LmpIn.custom(aLmpInFilePath);}
    public static IInFile json(final String aJsonFilePath) {return new AbstractInFileJson() {@Override protected Reader getInFileReader() throws IOException {return UT.IO.toReader(aJsonFilePath);}};}
    public static IInFile immutable(final String aInFilePath) {return new AbstractInFile(ImmutableMap.of()) {@Override public void write_(String aPath) throws IOException {UT.IO.copy(aInFilePath, aPath);}};}
    
    /** 默认行为 */
    @VisibleForTesting public static IInFile get(String aInFilePath) {return immutable(aInFilePath);}
}
