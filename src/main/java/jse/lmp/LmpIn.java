package jse.lmp;

import jse.code.UT;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.util.CharSequenceReader;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;


/**
 * @author liqa
 * <p> lammps 的输入文件，提供一些默认的预设的函数，
 * 或者使用 String 的路径来使用自定义的输入文件 </p>
 */
public class LmpIn extends AbstractLmpIn {
    final String mInFilePath;
    final boolean mInternal;
    LmpIn(String aInFilePath) {this(aInFilePath, false);}
    LmpIn(String aInFilePath, boolean aInternal) {mInFilePath = aInFilePath; mInternal = aInternal;}
    
    /** AbstractInFile stuffs，实现 lammps 的输入文件格式的设置参数 */
    @Override protected IReadLine getInFileReader() throws IOException {
        //noinspection resource
        final BufferedReader tReader = mInternal ? UT.IO.toReader(UT.IO.getResource("lmp/in/" + mInFilePath)) : UT.IO.toReader(mInFilePath);
        return new IReadLine() {
            @Override public String readLine() throws IOException {return tReader.readLine();}
            @Override public void close() throws IOException {tReader.close();}
        };
    }
    
    /** 提供常用的获取 LmpIn 实例的接口 */
    public static LmpIn custom(String aLmpInPath) {return new LmpIn(aLmpInPath);}
    public static AbstractLmpIn text(final String aText) {
        return new AbstractLmpIn() {
            @Override protected IReadLine getInFileReader() {
                Iterator<String> it = IOGroovyMethods.iterator(new CharSequenceReader(aText));
                return () -> it.hasNext() ? it.next() : null;
            }
        };
    }
    
    static LmpIn CONSTANT() {return new LmpIn("constant", true);}
    public static LmpIn DATA2RESTART_MELT_NPT_Cu    () {LmpIn tLmpIn = new LmpIn("data-restart-melt-NPT-Cu"         , true); tLmpIn.putIFiles("vInDataPath"    , "lmp/data/CuFCC108.lmpdat"         ).putOFiles("vOutRestartPath", "lmp/.temp/restart/melt-Cu108-init"); return tLmpIn;}
    public static LmpIn DATA2DATA_MELT_NPT_Cu       () {LmpIn tLmpIn = new LmpIn("data-data-melt-NPT-Cu"            , true); tLmpIn.putIFiles("vInDataPath"    , "lmp/.temp/data-in-Cu108"          ).putOFiles("vOutDataPath"   , "lmp/.temp/data-out-Cu108"         ); return tLmpIn;}
    public static LmpIn DATA2DUMP_MELT_NPT_Cu       () {LmpIn tLmpIn = new LmpIn("data-dump-melt-NPT-Cu"            , true); tLmpIn.putIFiles("vInDataPath"    , "lmp/data/Cu108.lmpdat"            ).putOFiles("vDumpPath"      , "lmp/.temp/dump/Cu108.lammpstrj"   ); return tLmpIn;}
    public static LmpIn RESTART2DUMP_MELT_NPT_Cu    () {LmpIn tLmpIn = new LmpIn("restart-dump-melt-NPT-Cu"         , true); tLmpIn.putIFiles("vInRestartPath" , "lmp/.temp/restart/melt-Cu108-init").putOFiles("vDumpPath"      , "lmp/.temp/dump/Cu108.lammpstrj"   ); return tLmpIn;}
    public static LmpIn RESTART2MULTI_MELT_NPT_Cu   () {LmpIn tLmpIn = new LmpIn("restart-multirestart-melt-NPT-Cu" , true); tLmpIn.putIFiles("vInRestartPath" , "lmp/.temp/restart/melt-Cu108-init").putOFiles("vOutRestartPath", "lmp/.temp/restart/melt-Cu108", 5).setIOFilesStartKey("vBeginIdx", "vOutRestartPath").setIOFileEndKey("vEndIdx", "vOutRestartPath"); return tLmpIn;}
    public static LmpIn DATA2DATA_COOLDOWN_NPT_Cu   () {LmpIn tLmpIn = new LmpIn("data-data-cooldown-NPT-Cu"        , true); tLmpIn.putIFiles("vInDataPath"    , "lmp/.temp/data-in-Cu108"          ).putOFiles("vOutDataPath"   , "lmp/.temp/data-out-Cu108"         ); return tLmpIn;}
    
    /** 默认行为 */
    @VisibleForTesting public static LmpIn of(String aLmpInPath) {return custom(aLmpInPath);}
}
