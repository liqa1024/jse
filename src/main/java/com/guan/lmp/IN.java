package com.guan.lmp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.guan.code.Pair;
import com.guan.code.UT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.guan.code.CS.SEP;

/**
 * @author liqa
 * <p> 一些预设的 lammps 输入文件，包含附加的额外输入文件以及输出文件 </p>
 */
@SuppressWarnings("SameParameterValue")
public enum IN {
      INIT_MELT_NPT_Cu   ("init-melt-NPT-Cu"        , SEP, "vInDataPath"    , "lmp/data/CuFCC108.lmpdat"            , SEP, "vOutRestartPath", "lmp/.temp/restart/melt-Cu108-init")
    , RESTART_MELT_NPT_Cu("restart-melt-NPT-Cu"     , SEP, "vInRestartPath" , "lmp/.temp/restart/melt-Cu108-init"   , SEP, "vOutRestartPath", "lmp/.temp/restart/melt-Cu108", 5)
    ;
    
    
    
    private final Map<String, List<String>> mInputFiles;
    private final Map<String, List<String>> mOutputFiles; // <FileKey, List<FilePath>>
    private final @Nullable URL mLmpInFile;
    
    public Map<String, List<String>> inputFiles() {return mInputFiles;}
    public Map<String, List<String>> outputFiles() {return mOutputFiles;}
    public @NotNull URL lmpInFile() {if (mLmpInFile == null) throw new RuntimeException(String.format("Lammps IN file of %s is missing", this.name())); return mLmpInFile;}
    
    
    IN (String aLmpInFileName) {
        mLmpInFile = UT.IO.getResource("lmp/in/"+aLmpInFileName);
        mInputFiles = ImmutableMap.of();
        mOutputFiles = ImmutableMap.of();
    }
    IN (String aLmpInFileName, Pair<Map<String, List<String>>, Map<String, List<String>>> aPair) {
        mLmpInFile = UT.IO.getResource("lmp/in/"+aLmpInFileName);
        mInputFiles = aPair.first;
        mOutputFiles = aPair.second;
    }
    IN (String aLmpInFileName, @Nullable Object aSeparator , Object... aFiles) {this(aLmpInFileName, ioFiles(aFiles));}
    IN (String aLmpInFileName, @Nullable Object aSeparator , String aInFileKey1, String aInFilePath1                                                                                                                         ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1                                                                          ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator , String aInFileKey1, String aInFilePath1, int aMultiple1                                                                                                         ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aMultiple1                                                              ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator , String aInFileKey1, String aInFilePath1, int aStart1, int aEnd1                                                                                                 ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aStart1, aEnd1                                                          ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1,                                                                  @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1                        ) {this(aLmpInFileName, ioFiles(                                           aSeparator2, aOutFileKey1, aOutFilePath1                ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1,                                                                  @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aMultiple2        ) {this(aLmpInFileName, ioFiles(                                           aSeparator2, aOutFileKey1, aOutFilePath1, aMultiple2    ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1,                                                                  @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aStart2, int aEnd2) {this(aLmpInFileName, ioFiles(                                           aSeparator2, aOutFileKey1, aOutFilePath1, aStart2, aEnd2));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1,                         @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1                        ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1,                 aSeparator2, aOutFileKey1, aOutFilePath1                ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1,                         @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aMultiple2        ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1,                 aSeparator2, aOutFileKey1, aOutFilePath1, aMultiple2    ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1,                         @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aStart2, int aEnd2) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1,                 aSeparator2, aOutFileKey1, aOutFilePath1, aStart2, aEnd2));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1, int aMultiple1,         @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1                        ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aMultiple1,     aSeparator2, aOutFileKey1, aOutFilePath1                ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1, int aMultiple1,         @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aMultiple2        ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aMultiple1,     aSeparator2, aOutFileKey1, aOutFilePath1, aMultiple2    ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1, int aMultiple1,         @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aStart2, int aEnd2) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aMultiple1,     aSeparator2, aOutFileKey1, aOutFilePath1, aStart2, aEnd2));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1, int aStart1, int aEnd1, @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1                        ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aStart1, aEnd1, aSeparator2, aOutFileKey1, aOutFilePath1                ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1, int aStart1, int aEnd1, @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aMultiple2        ) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aStart1, aEnd1, aSeparator2, aOutFileKey1, aOutFilePath1, aMultiple2    ));}
    IN (String aLmpInFileName, @Nullable Object aSeparator1, String aInFileKey1, String aInFilePath1, int aStart1, int aEnd1, @Nullable Object aSeparator2, String aOutFileKey1, String aOutFilePath1, int aStart2, int aEnd2) {this(aLmpInFileName, ioFiles(aInFileKey1, aInFilePath1, aStart1, aEnd1, aSeparator2, aOutFileKey1, aOutFilePath1, aStart2, aEnd2));}
    
    
    /**
     * 通用的获取输入输出文件的方法，输出为 {inFiles{key, pathList}, outFiles{key, pathList}}，
     * 输入是序列化的，方便使用，格式为：
     * <p>
     * InFileKey1, InFilePath1, [start1], [end1], InFileKey2, InFilePath2, ..., SEPARATOR, OutFileKey1, OutFilePath1, [start1], [end1], OutFileKey2, OutFilePath2, ...
     * <p>
     * 提供 [start], [end] 则认为 InFilePath 有多个，名称为 ${InFilePath}-${i}, i 会从 start 依次增加到 end。
     * 注意这里由于是 java 书写的，约定默认 start 为 0，且 end 是不包含的，和其他的使用到 start 和 end 的操作保持一致
     * @author liqa
     */
    public static Pair<Map<String, List<String>>, Map<String, List<String>>> ioFiles(Object... aFiles) {
        // 组装输入部分
        Pair<Map<String, List<String>>, Integer>
        tPair = scanAndBuildFiles_(0, aFiles);
        Map<String, List<String>> iFiles = tPair.first;
        // 组装输出部分
        tPair = scanAndBuildFiles_(tPair.second, aFiles);
        Map<String, List<String>> oFiles = tPair.first;
        // 输出
        return new Pair<>(iFiles, oFiles);
    }
    public static Pair<Map<String, List<String>>, Integer> scanAndBuildFiles_(int aIdx, Object... aFiles) {
        ImmutableMap.Builder<String, List<String>> fileBuilder = new ImmutableMap.Builder<>();
        while (aIdx < aFiles.length) {
            Object tKey = aFiles[aIdx]; ++aIdx;
            if (!(tKey instanceof String)) break;
            String tPath = (String)aFiles[aIdx]; ++aIdx;
            // 通过检测后两个来获取可选的 start 和 end
            int tStart = 0, tEnd = -1;
            Object tNext = aFiles[aIdx];
            if (tNext instanceof Number) {
                tEnd = ((Number)tNext).intValue();
                ++aIdx;
                tNext = aFiles[aIdx];
                if (tNext instanceof Number) {
                    tStart = tEnd;
                    tEnd = ((Number)tNext).intValue();
                    ++aIdx;
                }
            }
            // 根据 end 来决定文件路径名称格式
            List<String> tPaths;
            if (tEnd <= 0) tPaths = Collections.singletonList(tPath);
            else {
                ImmutableList.Builder<String> tBuilder = new ImmutableList.Builder<>();
                for (int i = tStart; i < tEnd; ++i) tBuilder.add(tPath+"-"+i);
                tPaths = tBuilder.build();
            }
            fileBuilder.put((String)tKey, tPaths);
        }
        // 输出
        return new Pair<>(fileBuilder.build(), aIdx);
    }
    
}
