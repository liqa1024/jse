package jse.io;

import jse.code.UT;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

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
        Map rJson;
        try (Reader tInFile = getInFileReader()) {
            rJson = (Map) (new JsonSlurper()).parse(tInFile);
            // 直接遍历修改
            for (Map.Entry<String, Object> subSetting : entrySet()) if (subSetting.getValue()!=KEEP && rJson.containsKey(subSetting.getKey())) {
                if (subSetting.getValue() == REMOVE) rJson.remove(subSetting.getKey());
                else rJson.put(subSetting.getKey(), subSetting.getValue());
            }
        }
        aWriteln.writeln((new JsonBuilder(rJson)).toString());
    }
    
    /** stuff to override，提供一个获取 inFile 的方法即可 */
    protected abstract Reader getInFileReader() throws IOException;
}
