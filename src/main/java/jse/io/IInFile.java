package jse.io;

import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author liqa
 * <p> 一般的输入文件接口，继承 IIOFiles 支持远程提交任务时自动上传下载文件，
 * 继承 Map 支持在 Groovy 中直接使用 . 索引来设置属性 </p>
 * <p> 提供了一些 hooks 来在修改设置时自动同步 IOFiles 的相关设置，
 * 原则上内部自带一个 setting 到 IOFiles 内部文件路径的 hook，
 * 即如果设置的是 String 格式则会自动检测 IOFiles 中对应的路径来同步修改 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public interface IInFile extends IIOFiles, Map<String, Object> {
    /** 设置 IOFile 的其他属性在 settings 中的 key，会添加一个 hook，在任何一方修改时检索 setting 的值来同步 */
    IInFile setIOFilesMultipleKey(String aSettingKey, String aIOFilesKey);
    IInFile setIOFilesStartKey(String aSettingKey, String aIOFilesKey);
    IInFile setIOFileEndKey(String aSettingKey, String aIOFilesKey);
    
    
    /** 提供将设置的属性应用到输入文件，然后写成文件的接口 */
    void write(String aPath) throws IOException;
    List<String> toLines() throws IOException;
    
    /** IOFile stuffs */
    IInFile putIFiles(String aIFileKey, String aIFilePath                      );
    IInFile putIFiles(String aIFileKey, String aIFilePath, int aMultiple       );
    IInFile putIFiles(String aIFileKey, String aIFilePath, int aStart, int aEnd);
    IInFile putOFiles(String aOFileKey, String aOFilePath                      );
    IInFile putOFiles(String aOFileKey, String aOFilePath, int aMultiple       );
    IInFile putOFiles(String aOFileKey, String aOFilePath, int aStart, int aEnd);
    
    @VisibleForTesting IInFile i(String aIFileKey, String aIFilePath                      );
    @VisibleForTesting IInFile i(String aIFileKey, String aIFilePath, int aMultiple       );
    @VisibleForTesting IInFile i(String aIFileKey, String aIFilePath, int aStart, int aEnd);
    @VisibleForTesting IInFile o(String aOFileKey, String aOFilePath                      );
    @VisibleForTesting IInFile o(String aOFileKey, String aOFilePath, int aMultiple       );
    @VisibleForTesting IInFile o(String aOFileKey, String aOFilePath, int aStart, int aEnd);
}
