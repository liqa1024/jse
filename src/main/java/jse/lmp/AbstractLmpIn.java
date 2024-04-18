package jse.lmp;

import jse.code.UT;
import jse.io.AbstractInFileLines;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


/**
 * @author liqa
 * <p> 抽象的 lammps 输入文件 </p>
 */
public abstract class AbstractLmpIn extends AbstractInFileLines {
    
    /** stuff to override */
    @Override protected abstract IReadLine getInFileReader() throws IOException;
    
    /** AbstractInFile stuffs，实现 lammps 的输入文件格式的设置参数 */
    @Override protected String getKeyOfLine(String aLine) {
        // 对于 lammps 的输入文件，这里的 key 检测 variable 定义的变量名或者每行的第一个值（如果是 variable 则会变成 variable 定义的变量名）
        String[] tTokens = UT.Text.splitBlank(aLine);
        if (tTokens.length == 0) return null;
        if (tTokens[0].equals("variable") && tTokens.length>1) return tTokens[1];
        return tTokens[0];
    }
    @Override protected String setValueOfLine(String aLine, Object aValue) {
        // 同样，对于 lammps 的输入文件，检测第一个为 variable 时则修改后续定义的变量值，否则修改后续所有
        if (aValue == null) return aLine;
        String[] tTokens = UT.Text.splitBlank(aLine);
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
}
