package jse.io;

import jse.code.UT;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import static jse.code.CS.KEEP;
import static jse.code.CS.REMOVE;


/**
 * @author liqa
 * <p> Yaml 格式的输入文件 </p>
 */
public abstract class AbstractInFileYaml extends AbstractInFile {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public final void writeTo_(UT.IO.IWriteln aWriteln) throws IOException {
        Map tYaml = UT.IO.yaml2map(getInFileReader());
        // 直接遍历修改
        for (Entry<String, Object> subSetting : entrySet()) if (subSetting.getValue()!=KEEP && tYaml.containsKey(subSetting.getKey())) {
            if (subSetting.getValue() == REMOVE) tYaml.remove(subSetting.getKey());
            else tYaml.put(subSetting.getKey(), subSetting.getValue());
        }
        aWriteln.writeln(UT.Text.map2yaml(tYaml));
    }
    
    /** stuff to override，提供一个获取 inFile 的方法即可 */
    protected abstract Reader getInFileReader() throws IOException;
}
