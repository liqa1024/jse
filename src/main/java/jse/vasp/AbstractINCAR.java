package jse.vasp;

import jse.code.UT;
import jse.io.AbstractInFileLines;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;


/**
 * @author liqa
 * <p> 抽象的 vasp 输入文件 INCAR </p>
 */
public abstract class AbstractINCAR extends AbstractInFileLines {
    /** stuff to override */
    @Override protected abstract IReadLine getInFileReader() throws IOException;
    
    /** AbstractInFile stuffs，实现 INCAR 格式的设置参数 */
    @Override protected @Nullable String getKeyOfLine(String aLine) {
        // 对于 INCAR，就是简单的 key = value 写法
        if (!aLine.contains("=")) return null;
        return aLine.split("=")[0].trim();
    }
    @Override protected @NotNull String setValueOfLine(String aLine, @Nullable Object aValue) {
        // 对于 INCAR，就是简单的 key = value 写法
        if (aValue == null) return aLine;
        int tEqIdx = aLine.indexOf('=');
        if (tEqIdx < 0) return aLine;
        return aLine.substring(0, tEqIdx+1) + " " +  value2str(aValue);
    }
    
    protected static String value2str(@NotNull Object aValue) {
        if (aValue instanceof Boolean) return (Boolean)aValue ? ".TRUE." : ".FALSE.";
        return aValue.toString();
    }
    protected static Object str2value(String aStr) {
        switch(aStr) {
            case ".TRUE.":  return Boolean.TRUE;
            case ".FALSE.": return Boolean.FALSE;
            default: {
                @Nullable Number tNum = UT.Text.str2number(aStr);
                return tNum!=null ? tNum : aStr;
            }
        }
    }
    
    /** INCAR 转为 map */
    public LinkedHashMap<String, Object> toMap(boolean aToLowerCase) throws IOException {
        LinkedHashMap<String, Object> rMap = new LinkedHashMap<>();
        try (IReadLine tReader = getInFileReader()) {
            String tLine;
            while ((tLine = tReader.readLine()) != null) {
                if (!tLine.contains("=")) continue;
                String[] tParts = tLine.split("=");
                String tKey = tParts[0].trim();
                if (aToLowerCase) tKey = tKey.toLowerCase();
                rMap.put(tKey, str2value(tParts[1].trim()));
            }
        }
        return rMap;
    }
    public LinkedHashMap<String, Object> toMap() throws IOException {
        return toMap(false);
    }
}
