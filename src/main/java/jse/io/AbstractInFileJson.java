package jse.io;

import jse.code.UT;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import static jse.code.CS.KEEP;
import static jse.code.CS.REMOVE;


/**
 * @author liqa
 * <p> Json 格式的输入文件 </p>
 */
public abstract class AbstractInFileJson extends AbstractInFile {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public final void writeTo_(UT.IO.IWriteln aWriteln) throws IOException {
        Map tJson = UT.IO.json2map(getInFileReader());
        // 直接遍历修改
        for (Map.Entry<String, Object> subSetting : entrySet()) if (subSetting.getValue()!=KEEP && tJson.containsKey(subSetting.getKey())) {
            if (subSetting.getValue() == REMOVE) tJson.remove(subSetting.getKey());
            else tJson.put(subSetting.getKey(), subSetting.getValue());
        }
        aWriteln.writeln(UT.Text.map2json(tJson));
    }
    
    /** stuff to override，提供一个获取 inFile 的方法即可 */
    protected abstract Reader getInFileReader() throws IOException;
}
