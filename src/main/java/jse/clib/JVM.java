package jse.clib;

import static jse.code.Conf.LIB_NAME_IN;
import static jse.code.Conf.LLIB_NAME_IN;
import static jse.code.OS.IS_WINDOWS;
import static jse.code.OS.JAVA_HOME_DIR;

public class JVM {
    public final static String INCLUDE_DIR;
    public final static String LIB_DIR;
    public final static String LIB_PATH;
    public final static String LLIB_DIR;
    public final static String LLIB_PATH;
    
    static {
        INCLUDE_DIR = JAVA_HOME_DIR + "include/";
        LLIB_DIR = JAVA_HOME_DIR + "lib/";
        String tLLibName = LLIB_NAME_IN(LLIB_DIR, "jvm");
        if (tLLibName == null) throw new RuntimeException("No jvm llib in '$JAVA_HOME/lib/'");
        LLIB_PATH = LLIB_DIR + tLLibName;
        if (!IS_WINDOWS) {
            LIB_DIR = LLIB_DIR;
            LIB_PATH = LLIB_PATH;
        } else {
            String tLibDir = JAVA_HOME_DIR + "bin/server/";
            String tLibName = LIB_NAME_IN(tLibDir, "jvm");
            if (tLibName == null) {
                tLibDir = JAVA_HOME_DIR + "bin/client/";
                tLibName = LIB_NAME_IN(tLibDir, "jvm");
            }
            if (tLibName == null) throw new RuntimeException("No jvm lib in '$JAVA_HOME/bin/server/' or '$JAVA_HOME/bin/client/'");
            LIB_DIR = tLibDir;
            LIB_PATH = tLibDir + tLibName;
        }
    }
}
