package com.guan.io;

import java.io.IOException;
import java.util.Map;

/**
 * @author liqa
 * <p> 一般的输入文件接口，继承 IHasIOFiles 支持远程提交任务时自动上传下载文件，
 * 继承 Map 支持在 Groovy 中直接使用 . 索引来设置属性 </p>
 * <p> 提供了一些 hooks 来在修改设置时自动同步 IOFiles 的相关设置，
 * 原则上内部自带一个 setting 到 IOFiles 内部文件路径的 hook，
 * 即如果设置的是 String 格式则会自动检测 IOFiles 中对应的路径来同步修改 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public interface IInFile extends IHasIOFiles, Map<String, Object> {
    /** 设置 IOFile 的其他属性在 settings 中的 key，会添加一个 hook，在任何一方修改时检索 setting 的值来同步 */
    IInFile setIOFilesMultipleKey(String aSettingKey, String aIOFilesKey);
    IInFile setIOFilesStartKey(String aSettingKey, String aIOFilesKey);
    IInFile setIOFileEndKey(String aSettingKey, String aIOFilesKey);
    
    
    /** 提供将设置的属性应用到输入文件，然后写成文件的接口 */
    void write(String aPath) throws IOException;
    
    /** IOFile stuffs */
    IInFile setIFilePath    (String aIFileKey, String aIFilePath);
    IInFile setIFileSingle  (String aIFileKey                   );
    IInFile setIFileStart   (String aIFileKey, int aStart       );
    IInFile setIFileEnd     (String aIFileKey, int aEnd         );
    IInFile setIFileMultiple(String aIFileKey, int aMultiple    );
    IInFile setOFilePath    (String aOFileKey, String aOFilePath);
    IInFile setOFileSingle  (String aOFileKey                   );
    IInFile setOFileStart   (String aOFileKey, int aStart       );
    IInFile setOFileEnd     (String aOFileKey, int aEnd         );
    IInFile setOFileMultiple(String aOFileKey, int aMultiple    );
    
    IInFile putIFiles(String aIFileKey1, String aIFilePath1, Object... aElse       );
    IInFile putIFiles(String aIFileKey1, String aIFilePath1                        );
    IInFile putIFiles(String aIFileKey1, String aIFilePath1, int aMultiple1        );
    IInFile putIFiles(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1);
    IInFile putOFiles(String aOFileKey1, String aOFilePath1, Object... aElse       );
    IInFile putOFiles(String aOFileKey1, String aOFilePath1                        );
    IInFile putOFiles(String aOFileKey1, String aOFilePath1, int aMultiple1        );
    IInFile putOFiles(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1);
    
    @Deprecated IInFile i(String aIFileKey1, String aIFilePath1, Object... aElse       );
    @Deprecated IInFile i(String aIFileKey1, String aIFilePath1                        );
    @Deprecated IInFile i(String aIFileKey1, String aIFilePath1, int aMultiple1        );
    @Deprecated IInFile i(String aIFileKey1, String aIFilePath1, int aStart1, int aEnd1);
    @Deprecated IInFile o(String aOFileKey1, String aOFilePath1, Object... aElse       );
    @Deprecated IInFile o(String aOFileKey1, String aOFilePath1                        );
    @Deprecated IInFile o(String aOFileKey1, String aOFilePath1, int aMultiple1        );
    @Deprecated IInFile o(String aOFileKey1, String aOFilePath1, int aStart1, int aEnd1);
}
