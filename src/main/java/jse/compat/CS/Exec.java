package jse.compat.CS;

import jse.code.OS;
import jse.system.ISystemExecutor;
import org.jetbrains.annotations.VisibleForTesting;

/** @deprecated use {@link OS} */
@VisibleForTesting @Deprecated
public final class Exec extends OS {
    public final static boolean IS_WINDOWS = OS.IS_WINDOWS;
    public final static boolean IS_MAC = OS.IS_MAC;
    public final static String NO_LOG_LINUX = OS.NO_LOG_LINUX;
    public final static String NO_LOG_WIN = OS.NO_LOG_WIN;
    public final static String NO_LOG = OS.NO_LOG;
    
    public final static ISystemExecutor EXE = OS.EXE;
    public final static String JAR_PATH = OS.JAR_PATH;
    public final static String JAR_DIR = OS.JAR_DIR;
    public final static String USER_HOME = OS.USER_HOME;
    public final static String USER_HOME_DIR = OS.USER_HOME_DIR;
    public final static String WORKING_DIR = OS.WORKING_DIR;
}
