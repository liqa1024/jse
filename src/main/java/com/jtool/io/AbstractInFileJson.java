package com.jtool.io;

import com.jtool.code.UT;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;


/**
 * @author liqa
 * <p> Json 格式的输入文件 </p>
 */
public abstract class AbstractInFileJson extends AbstractInFile {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public final void write_(String aPath) throws IOException {
        Map rJson;
        try (Reader tInFile = getInFileReader()) {
            rJson = (Map) (new JsonSlurper()).parse(tInFile);
            // 直接遍历修改
            for (Map.Entry<String, Object> subSetting : entrySet()) if (rJson.containsKey(subSetting.getKey())) {
                rJson.put(subSetting.getKey(), subSetting.getValue());
            }
        }
        try (Writer tWriter = UT.IO.toWriter(aPath)) {
            (new JsonBuilder(rJson)).writeTo(tWriter);
        }
    }
    
    /** stuff to override，提供一个获取 inFile 的方法即可 */
    protected abstract Reader getInFileReader() throws IOException;
}
