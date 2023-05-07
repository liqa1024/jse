package com.guan.lmp;

import com.google.common.collect.ImmutableMapBuilder;
import com.google.common.collect.Maps;
import com.guan.code.UT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.URL;
import java.util.Map;

import static com.guan.code.CS.SP;

/**
 * @author liqa
 * <p> 一些预设的 lammps 输入文件，包含附加的额外输入文件以及输出文件 </p>
 */
@SuppressWarnings("SameParameterValue")
public enum IN {
      INIT_MELT_NPT_Cu   ("init-melt-NPT-Cu"             , SP, "vInDataPath", "lmp/data/CuFCC108.lmpdat", SP, "vOutRestartPath", ".temp/lmp/restart/melt-init-Cu108"   )
    , INIT_MELT_SC_NPT_Cu("init-melt-supercooling-NPT-Cu", SP, "vInDataPath", "lmp/data/CuFCC108.lmpdat", SP, "vOutRestartPath", ".temp/lmp/restart/melt-sc-init-Cu108")
    ;
    
    
    
    private final @Unmodifiable Map<String, String> mInputFiles;
    private final @Unmodifiable Map<String, String> mOutputFiles; // <FileKey, FilePath>
    private final @Nullable URL mLmpInFile;
    
    public @Unmodifiable Map<String, String> inputFiles() {return mInputFiles;}
    public @Unmodifiable Map<String, String> outputFiles() {return mOutputFiles;}
    public @NotNull URL lmpInFile() {if (mLmpInFile == null) throw new RuntimeException(String.format("Lammps IN file of %s is missing", this.name())); return mLmpInFile;}
    
    
    IN(String aLmpInFileName) {
        mLmpInFile = UT.IO.getResource("lmp/in/"+aLmpInFileName);
        mInputFiles = Maps.immutableMap();
        mOutputFiles = Maps.immutableMap();
    }
    IN(String aLmpInFileName, @Nullable Object aSeparator, String aFirstInFileKey, String aFirstInFilePath, Object... aElse) {
        mLmpInFile = UT.IO.getResource("lmp/in/"+aLmpInFileName);
        
        ImmutableMapBuilder<String, String> tInFileMapBuilder = new ImmutableMapBuilder<>();
        tInFileMapBuilder.put(aFirstInFileKey, aFirstInFilePath);
        int tOutFileIdx = -1;
        for (int i=1; i < aElse.length; i+=2) {
            Object tKey = aElse[i-1];
            if (!(tKey instanceof String)) {
                tOutFileIdx = i;
                break;
            }
            tInFileMapBuilder.put((String)tKey, (String)aElse[i]);
        }
        mInputFiles = tInFileMapBuilder.getMap();
        if (tOutFileIdx < 0) {
            mOutputFiles = Maps.immutableMap();
            return;
        }
        ImmutableMapBuilder<String, String> tOutFileMapBuilder = new ImmutableMapBuilder<>();
        for (int i=tOutFileIdx+1; i < aElse.length; i+=2) {
            tOutFileMapBuilder.put((String)aElse[i-1], (String)aElse[i]);
        }
        mOutputFiles = tOutFileMapBuilder.getMap();
    }
    IN(String aLmpInFileName, @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFileKey, String aFirstOutFilePath, Object... aElse) {
        mLmpInFile = UT.IO.getResource("lmp/in/"+aLmpInFileName);
        
        mInputFiles = Maps.immutableMap();
        ImmutableMapBuilder<String, String> tOutFileMapBuilder = new ImmutableMapBuilder<>();
        tOutFileMapBuilder.put(aFirstOutFileKey, aFirstOutFilePath);
        for (int i=1; i < aElse.length; i+=2) {
            tOutFileMapBuilder.put((String)aElse[i-1], (String)aElse[i]);
        }
        mOutputFiles = tOutFileMapBuilder.getMap();
    }
}
