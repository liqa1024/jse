package com.jtool.io;

import com.jtool.code.UT;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author liqa
 * <p> 按照一行一个参数和属性格式的输入文件 </p>
 */
public abstract class AbstractInFileLines extends AbstractInFile {
    @Override public final void write_(String aPath) throws IOException {
        List<String> rLines = new ArrayList<>();
        try (BufferedReader tInFile = getInFileReader()) {
            String tLine;
            while ((tLine = tInFile.readLine()) != null) {
                String tKey = getKeyOfLine(tLine);
                if (tKey!=null && containsKey(tKey)) tLine = setValueOfLine(tLine, get(tKey));
                rLines.add(tLine);
            }
        }
        UT.IO.write(aPath, rLines);
    }
    
    
    /** stuff to override，提供一个获取 inFile 的 Reader 以及应用这些自定义设置的方法 */
    protected abstract BufferedReader getInFileReader() throws IOException;
    protected abstract String getKeyOfLine(String aLine); // 注意返回 null 表示获取 key 失败，不进行 value 设置
    protected abstract String setValueOfLine(String aLine, Object aValue);
}
