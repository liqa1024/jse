package jse.clib;

import jse.code.UT;

import static jse.code.OS.JAR_DIR;
import static jse.code.CS.VERSION;

/**
 * 其他 jni 库或者此项目需要依赖的 c 库；
 * 包含编写 jni 库需要的一些通用方法。
 * <p>
 * 使用 header only 的写法来简化编译，并且提高效率
 * @author liqa
 */
public class JNIUtil {
    private JNIUtil() {}
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(HOME);
        }
    }
    
    public final static String HOME = JAR_DIR+"jniutil/" + UT.Code.uniqueID(VERSION) + "/";
    public final static String INCLUDE_DIR = HOME+"include/";
    public final static String HEADER_NAME = "jniutil.h";
    public final static String HEADER_PATH = INCLUDE_DIR+HEADER_NAME;
    
    /** jniutil 内部定义的常量，这里重新定义一次从而避免交互 */
    public final static int
      JTYPE_NULL    = 0
    , JTYPE_BYTE    = 1
    , JTYPE_DOUBLE  = 2
    , JTYPE_BOOLEAN = 3
    , JTYPE_CHAR    = 4
    , JTYPE_SHORT   = 5
    , JTYPE_INT     = 6
    , JTYPE_LONG    = 7
    , JTYPE_FLOAT   = 8
    ;
    
    private static void initJNIUtil_() throws Exception {
        // 直接从内部资源解压到需要目录，如果已经存在则先删除
        UT.IO.removeDir(INCLUDE_DIR);
        UT.IO.copy(UT.IO.getResource("jniutil/src/"+ HEADER_NAME), HEADER_PATH);
        System.out.println("JNIUTIL INIT INFO: jniutil successfully installed.");
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 如果不存在 jniutil.h 则需要重新通过源码编译
        if (!UT.IO.isFile(HEADER_PATH)) {
            System.out.println("JNIUTIL INIT INFO: jniutil.h not found. Reinstalling...");
            try {initJNIUtil_();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        // header only 库不需要设置库路径
    }
}
