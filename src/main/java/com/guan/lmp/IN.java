package com.guan.lmp;

import com.guan.code.IHasIOFiles;
import com.guan.code.IOFiles;
import com.guan.code.UT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

import static com.guan.code.CS.SEP;

/**
 * @author liqa
 * <p> 一些预设的 lammps 输入文件，包含附加的额外输入文件以及输出文件 </p>
 */
@SuppressWarnings("SameParameterValue")
public enum IN implements IHasIOFiles {
      INIT_MELT_NPT_Cu   ("init-melt-NPT-Cu"        , IOFiles.get("vInDataPath"    , "lmp/data/CuFCC108.lmpdat"            , SEP, "vOutRestartPath", "lmp/.temp/restart/melt-Cu108-init"))
    , RESTART_MELT_NPT_Cu("restart-melt-NPT-Cu"     , IOFiles.get("vInRestartPath" , "lmp/.temp/restart/melt-Cu108-init"   , SEP, "vOutRestartPath", "lmp/.temp/restart/melt-Cu108", 5))
    ;
    
    
    private final @Nullable URL mLmpInFile;
    private final IHasIOFiles mIOFiles;
    
    public @NotNull URL lmpInFile() {if (mLmpInFile == null) throw new RuntimeException(String.format("Lammps IN file of %s is missing", this.name())); return mLmpInFile;}
    
    IN (String aLmpInFileName) {
        mLmpInFile = UT.IO.getResource("lmp/in/"+aLmpInFileName);
        mIOFiles = IOFiles.get();
    }
    IN (String aLmpInFileName, IHasIOFiles aIOFiles) {
        mLmpInFile = UT.IO.getResource("lmp/in/"+aLmpInFileName);
        mIOFiles = aIOFiles;
    }
    @Override public Iterable<String> inputFiles(String aInFileKey) {return mIOFiles.inputFiles(aInFileKey);}
    @Override public Iterable<String> outputFiles(String aOutFileKey) {return mIOFiles.outputFiles(aOutFileKey);}
    @Override public Iterable<String> inputFiles() {return mIOFiles.inputFiles();}
    @Override public Iterable<String> outputFiles() {return mIOFiles.outputFiles();}
}
