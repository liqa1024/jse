package jse.vasp;

import jse.code.UT;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.util.CharSequenceReader;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author liqa
 * <p> vasp 的输入文件 INCAR，提供一些默认的预设的函数，
 * 或者使用 String 的路径来使用自定义的输入文件 </p>
 */
public class INCAR extends AbstractINCAR {
    final String mIncarPath;
    final boolean mInternal;
    INCAR(String aIncarPath) {this(aIncarPath, false);}
    INCAR(String aIncarPath, boolean aInternal) {mIncarPath = aIncarPath; mInternal = aInternal;}
    
    /** AbstractInFile stuffs，实现 INCAR 格式的设置参数 */
    @Override protected IReadLine getInFileReader() throws IOException {
        //noinspection resource
        final BufferedReader tReader = mInternal ? UT.IO.toReader(UT.IO.getResource("vasp/incar/" + mIncarPath)) : UT.IO.toReader(mIncarPath);
        return new IReadLine() {
            @Override public String readLine() throws IOException {return tReader.readLine();}
            @Override public void close() throws IOException {tReader.close();}
        };
    }
    
    /** 提供常用的获取 INCAR 实例的接口 */
    public static INCAR custom(String aIncarPath) {return new INCAR(aIncarPath);}
    public static AbstractINCAR text(final String aText) {
        return new AbstractINCAR() {
            @Override protected IReadLine getInFileReader() {
                Iterator<String> it = IOGroovyMethods.iterator(new CharSequenceReader(aText));
                return () -> it.hasNext() ? it.next() : null;
            }
        };
    }
    public static AbstractINCAR map(Map<String, Object> aMap) {
        final List<String> tLines = new ArrayList<>(aMap.size() + 1);
        tLines.add("INCAR created by JSE");
        for (Map.Entry<String, Object> tEntry : aMap.entrySet()) {
            tLines.add(" "+tEntry.getKey().toUpperCase()+" = "+value2str(tEntry.getValue()));
        }
        return new AbstractINCAR() {
            @Override protected IReadLine getInFileReader() {
                Iterator<String> it = tLines.iterator();
                return () -> it.hasNext() ? it.next() : null;
            }
        };
    }
    
    public static INCAR JNN_DEFAULT() {return new INCAR("jnn-default", true);}
    
    /** 默认行为 */
    @VisibleForTesting public static INCAR of(String aIncarPath) {return custom(aIncarPath);}
}
