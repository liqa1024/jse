package jse.io;

import jse.code.UT;

import java.io.BufferedReader;
import java.io.IOException;

import static jse.code.CS.KEEP;
import static jse.code.CS.REMOVE;


/**
 * @author liqa
 * <p> 按照一行一个参数和属性格式的输入文件 </p>
 */
public abstract class AbstractInFileLines extends AbstractInFile {
    @Override public final void writeTo_(UT.IO.IWriteln aWriteln) throws IOException {
        try (BufferedReader tInFile = getInFileReader()) {
            String tLine;
            while ((tLine = tInFile.readLine()) != null) {
                String tKey = getKeyOfLine(tLine);
                if (tKey!=null && containsKey(tKey)) {
                    Object tValue = get(tKey);
                    if (tValue != REMOVE) {
                        if (tValue != KEEP) tLine = setValueOfLine(tLine, tValue);
                        aWriteln.writeln(tLine);
                    }
                } else {
                    aWriteln.writeln(tLine);
                }
            }
        }
    }
    
    
    /** stuff to override，提供一个获取 inFile 的 Reader 以及应用这些自定义设置的方法 */
    protected abstract BufferedReader getInFileReader() throws IOException;
    protected abstract String getKeyOfLine(String aLine); // 注意返回 null 表示获取 key 失败，不进行 value 设置
    protected abstract String setValueOfLine(String aLine, Object aValue);
}
