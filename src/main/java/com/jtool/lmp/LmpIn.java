package com.jtool.lmp;

import com.jtool.iofile.AbstractInFileLines;
import com.jtool.code.UT;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


/**
 * @author liqa
 * <p> lammps 的输入文件，提供一些默认的预设的函数，
 * 或者使用 String 的路径来使用自定义的输入文件 </p>
 */
public class LmpIn extends AbstractInFileLines {
    final String mInFilePath;
    final boolean mInternal;
    LmpIn(String aInFilePath) {this(aInFilePath, false);}
    LmpIn(String aInFilePath, boolean aInternal) {mInFilePath = aInFilePath; mInternal = aInternal;}
    
    /** AbstractInFile stuffs，实现 lammps 的输入文件格式的设置参数 */
    @Override protected BufferedReader getInFileReader() throws IOException {return mInternal ? UT.IO.toReader(UT.IO.getResource("lmp/in/" + mInFilePath)) : UT.IO.toReader(mInFilePath);}
    @Override protected String getKeyOfLine(String aLine) {
        // 对于 lammps 的输入文件，这里的 key 检测 variable 定义的变量名或者每行的第一个值（如果是 variable 则会变成 variable 定义的变量名）
        String[] tTokens = UT.Texts.splitBlank(aLine);
        if (tTokens.length == 0) return null;
        if (tTokens[0].equals("variable") && tTokens.length>1) return tTokens[1];
        return tTokens[0];
    }
    @Override protected String setValueOfLine(String aLine, Object aValue) {
        // 同样，对于 lammps 的输入文件，检测第一个为 variable 时则修改后续定义的变量值，否则修改后续所有
        if (aValue == null) return aLine;
        String[] tTokens = UT.Texts.splitBlank(aLine);
        if (tTokens.length == 0) return aLine;
        if (tTokens[0].equals("variable") && tTokens.length>1) {
            tTokens[3] = aValue.toString();
            return String.join(" ", tTokens);
        }
        // 一般情况，组装输出
        List<String> rTokens = new ArrayList<>();
        rTokens.add(tTokens[0]);
        // 一般情况可以兼容 Object[] 或者 Iterable 的 value 值
        if (aValue.getClass().isArray()) {
            int tSize = Array.getLength(aValue);
            for (int i = 0; i < tSize; ++i) rTokens.add(Array.get(aValue, i).toString());
        } else
        if (aValue instanceof Iterable) {
            Iterable<?> tValues = (Iterable<?>)aValue;
            for (Object tValue : tValues) rTokens.add(tValue.toString());
        } else {
            rTokens.add(aValue.toString());
        }
        return String.join(" ", rTokens);
    }
    
    
    /** 提供常用的获取 LmpIn 实例的接口 */
    public static LmpIn custom(String aLmpInPath) {return new LmpIn(aLmpInPath);}
    
    public static LmpIn LONG_TIME          () {return new LmpIn("long-time", true);}
    public static LmpIn INIT_MELT_NPT_Cu   () {LmpIn tLmpIn = new LmpIn("init-melt-NPT-Cu"   , true); tLmpIn.putIFiles("vInDataPath"    , "lmp/data/CuFCC108.lmpdat"         ).putOFiles("vOutRestartPath", "lmp/.temp/restart/melt-Cu108-init"); return tLmpIn;}
    public static LmpIn DATA_MELT_NPT_Cu   () {LmpIn tLmpIn = new LmpIn("data-melt-NPT-Cu"   , true); tLmpIn.putIFiles("vInDataPath"    , "lmp/.temp/data-in-Cu108"          ).putOFiles("vOutDataPath"   , "lmp/.temp/data-out-Cu108"         ); return tLmpIn;}
    public static LmpIn DUMP_MELT_NPT_Cu   () {LmpIn tLmpIn = new LmpIn("dump-melt-NPT-Cu"   , true); tLmpIn.putIFiles("vInDataPath"    , "lmp/data/Cu108.lmpdat"            ).putOFiles("vDumpPath"      , "lmp/.temp/dump/Cu108.lammpstrj"   ); return tLmpIn;}
    public static LmpIn RESTART_MELT_NPT_Cu() {LmpIn tLmpIn = new LmpIn("restart-melt-NPT-Cu", true); tLmpIn.putIFiles("vInRestartPath" , "lmp/.temp/restart/melt-Cu108-init").putOFiles("vOutRestartPath", "lmp/.temp/restart/melt-Cu108", 5).setIOFilesStartKey("vBeginIdx", "vOutRestartPath").setIOFileEndKey("vEndIdx", "vOutRestartPath"); return tLmpIn;}
    
    /** 默认行为 */
    @VisibleForTesting public static LmpIn of(String aLmpInPath) {return custom(aLmpInPath);}
}
