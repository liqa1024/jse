package jse.compat.CS;

import jse.code.CS;
import jse.system.ISystemExecutor;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class Exec extends CS.Exec {
    public final static boolean IS_WINDOWS = CS.Exec.IS_WINDOWS;
    public final static boolean IS_MAC = CS.Exec.IS_MAC;
    public final static String NO_LOG_LINUX = CS.Exec.NO_LOG_LINUX;
    public final static String NO_LOG_WIN = CS.Exec.NO_LOG_WIN;
    public final static String NO_LOG = CS.Exec.NO_LOG;
    
    public final static ISystemExecutor EXE = CS.Exec.EXE;
    public final static String JAR_PATH = CS.Exec.JAR_PATH;
    public final static String JAR_DIR = CS.Exec.JAR_DIR;
    public final static String USER_HOME = CS.Exec.USER_HOME;
    public final static String USER_HOME_DIR = CS.Exec.USER_HOME_DIR;
    public final static String WORKING_DIR = CS.Exec.WORKING_DIR;
}
