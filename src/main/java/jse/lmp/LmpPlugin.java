package jse.lmp;

import jse.clib.*;
import jse.code.IO;
import jse.code.OS;
import jse.code.SP;
import jse.code.UT;
import jse.parallel.IAutoShutdown;
import jse.parallel.MPI;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static jse.code.OS.*;

/**
 * 原生的 lammps jse 插件，提供 {@code LmpPair} 和
 * {@code LmpFix}，重写以实现自定义的 lammps {@code pair} 和 {@code fix} 指令
 * @author liqa
 */
public class LmpPlugin {
    /** 目前认为这些值永远都不会变，至少在 java 中这些操作肯定是可以达到目的 */
    public final static int SBBITS = 30, HISTBITS = 29, NEIGHMASK = 0x1FFFFFFF, HISTMASK = 0xDFFFFFFF, SPECIALMASK = 0x3FFFFFFF;
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    public final static class Conf {
        /** 启动的 jvm 的最大内存，默认为 1g 用来防止 mpi 运行 java 导致内存溢出 */
        public static String JVM_XMX = "1g";
        
        /** 插件依赖的 lammps 版本字符串，默认自动检测 */
        public static String LMP_VERSION = null;
        private final static String DEFAULT_LMP_VERSION = "22 Jul 2025";
    }
    
    public final static String LIB_DIR;
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_lmp_LmpPlugin_Pair.cpp"
        , "jse_lmp_LmpPlugin_Pair.h"
        , "jse_lmp_LmpPlugin_Fix.cpp"
        , "jse_lmp_LmpPlugin_Fix.h"
        , "jseplugin.cpp"
        , "LmpPair.cpp"
        , "LmpPair.h"
        , "LmpFix.cpp"
        , "LmpFix.h"
        , "LmpPlugin.cpp"
        , "LmpPlugin.h"
        , "pair_jse.cpp"
        , "pair_jse.h"
        , "fix_jse.cpp"
        , "fix_jse.h"
        , "lammpsplugin.h"
        , "neigh_request.h"
        , "STUBS/mpi.h"
        , "compat/json_fwd.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 依赖 cpointer
        CPointer.InitHelper.init();
        // 依赖 lmpjni
        NativeLmp.InitHelper.init();
        // 构建 lmpplugin
        LIB_DIR = LmpCore.ROOT+"plugin/" + UT.Code.uniqueID(LmpCore.LIB_PATH, Conf.JVM_XMX, LmpCore.Conf.CMAKE_SETTING_SHARE) + "/";
        final String[] fLmpVersion = new String[1];
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("lmpplugin", "LMPPLUGIN", LIB_DIR, LmpCore.Conf.CMAKE_SETTING_SHARE)
            .setEnvChecker(() -> {
                // 获取 lammps 版本字符串
                if (Conf.LMP_VERSION != null) {
                    fLmpVersion[0] = Conf.LMP_VERSION;
                } else {
                    try (NativeLmp tLmp = new NativeLmp("-log", "none", "-screen", "none")) {
                        String tLmpVersion = tLmp.versionStr();
                        fLmpVersion[0] = tLmpVersion==null ? Conf.DEFAULT_LMP_VERSION : tLmpVersion;
                    }
                }
            })
            .setSrc("lmp/plugin", SRC_NAME)
            .setCmakeCxxCompiler(LmpCore.Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(LmpCore.Conf.CMAKE_CXX_FLAGS)
            .setCmakeLineOp(line -> {
                // 替换其中的 lammps 库路径为设置好的路径
                line = line.replace("$ENV{JSE_LMP_INCLUDE_DIR}", LmpCore.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_LMP_LIB_PATH}"   , LmpCore.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
                // 替换 lammps 版本为设置值
                line = line.replace("$ENV{JSE_LMP_VERSION}", fLmpVersion[0]);
                // 替换其中的 jvm 库路径为自动检测到的路径
                line = line.replace("$ENV{JSE_JVM_LIB_PATH_DEF}", JVM.LIB_PATH.replace("\\", "\\\\\\\\")); // 注意反斜杠的转义问题
                // 替换 jvm 启动设置
                line = line.replace("$ENV{JSE_JAR_PATH_DEF}",  JAR_PATH.replace("\\", "\\\\\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_JVM_XMX}", Conf.JVM_XMX);
                return line;
            }).get();
        // 设置库路径，这里直接使用 System.load
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    
    public static abstract class Pair implements IAutoShutdown {
        static {
            // 确保 LmpPlugin 已经确实初始化
            LmpPlugin.InitHelper.init();
        }
        /** 目前认为这些值永远都不会变 */
        public final static int CENTROID_SAME = 0, CENTROID_AVAIL = 1, CENTROID_NOTAVAIL = 2;
        
        /**
         * 通过反射来获取类，可以是文件路径，也可以是类路径；
         * 这里简单处理，先尝试当文件路径，然后再尝试当类路径
         * @param aClassNameOrPath 类路径的名称或者是 groovy 的脚本文件路径
         * @param aPairPtr lammps jse pair 对应类的指针
         * @return 需要的对象
         */
        public static Pair of(String aClassNameOrPath, long aPairPtr) throws Exception {
            Class<?> tClazz;
            try {
                tClazz = SP.Groovy.parseClass(aClassNameOrPath);
            } catch (Exception e) {
                try {
                    tClazz = SP.Groovy.getClass(aClassNameOrPath);
                } catch (Exception ex) {
                    // 这里简单判断然后抛出合适的错误
                    if (aClassNameOrPath.endsWith(".groovy") || aClassNameOrPath.contains("/") || aClassNameOrPath.contains("\\") || aClassNameOrPath.contains(File.separator)) throw e;
                    else throw ex;
                }
            }
            return (Pair)InvokerHelper.invokeConstructorOf(tClazz, aPairPtr);
        }
        
        
        protected final long mPairPtr;
        /**
         * @param aPairPtr lammps jse pair 对应类的指针
         */
        protected Pair(long aPairPtr) {mPairPtr = aPairPtr;}
        
        /**
         * 在这里执行主要的 pair 的计算部分
         */
        public abstract void compute() throws Exception;
        
        /**
         * 在这里执行 pair 的每个原子对作用的计算部分
         */
        public double single(int i, int j, int itype, int jtype, double rsq, double factor_coul, double factor_lj, DoubleCPointer fforce) throws Exception {
            throw new UnsupportedOperationException("single");
        }
        private double single_(int i, int j, int itype, int jtype, double rsq, double factor_coul, double factor_lj, long fforce) throws Exception {
            return single(i, j, itype, jtype, rsq, factor_coul, factor_lj, new DoubleCPointer(fforce));
        }
        
        /**
         * lammps {@code pair_coeff} 会调用的方法，用于设置参数
         * @param aArgs 参数的字符串数组
         */
        public abstract void coeff(String... aArgs) throws Exception;
        
        /**
         * lammps {@code pair_style} 会调用的方法，主要用于在这里设置 pair 的各种性质
         * <p>
         * 默认情况下 pair jse 设置为：
         * <pre> {@code
         * single_enable = 0
         * restartinfo = 0
         * manybody_flag = 1
         * } </pre>
         * @param aArgs {@code pair_style} 传入的参数，第一个参数永远是 pair 的类名
         */
        public void settings(String... aArgs) throws Exception {
            if (aArgs.length > 1) throw new IllegalArgumentException("Illegal pair_style jse command");
        }
        
        /**
         * lammps pair 初始化调用，主要用于在这里设置需要的近邻列表样式
         */
        public void initStyle() throws Exception {neighborRequestDefault();}
        
        /**
         * lammps pair 初始化某两个种类 {@code i} {@code j}，用于获取这两个种类间的截断半径值
         * @param i 种类 {@code i}，从 {@code 1} 开始
         * @param j 种类 {@code j}，从 {@code 1} 开始
         * @return 种类 {@code i} {@code j} 之间的截断半径
         */
        public abstract double initOne(int i, int j) throws Exception;
        
        /**
         * lammps pair 析构时调用，用于手动释放一些资源
         */
        @Override public void shutdown() {/**/}
        
        /**
         * lammps pair 中提供的 {@code pack_forward_comm} 方法包装，用于将数据送出到 ghost 原子
         * <p>
         * 在这里打包需要向其他进程送出的数据
         */
        public int packForwardComm(int n, IntCPointer list, DoubleCPointer buf, int pbc_flag, IntCPointer pbc) throws Exception {return 0;}
        private int packForwardComm_(int n, long list, long buf, int pbc_flag, long pbc) throws Exception {
            return packForwardComm(n, new IntCPointer(list), new DoubleCPointer(buf), pbc_flag, new IntCPointer(pbc));
        }
        /**
         * lammps pair 中提供的 {@code unpack_forward_comm} 方法包装，用于将数据送出到 ghost 原子
         * <p>
         * 在这里解包其他进程送来的数据
         */
        public void unpackForwardComm(int n, int first, DoubleCPointer buf) throws Exception {}
        private void unpackForwardComm_(int n, int first, long buf) throws Exception {
            unpackForwardComm(n, first, new DoubleCPointer(buf));
        }
        /**
         * lammps pair 中提供的 {@code pack_reverse_comm} 方法包装，用于从 ghost 原子获取数据
         * <p>
         * 在这里打包需要向其他进程送出的数据
         */
        public int packReverseComm(int n, int first, DoubleCPointer buf) throws Exception {return 0;}
        private int packReverseComm_(int n, int first, long buf) throws Exception {
            return packReverseComm(n, first, new DoubleCPointer(buf));
        }
        /**
         * lammps pair 中提供的 {@code unpack_reverse_comm} 方法包装，用于从 ghost 原子获取数据
         * <p>
         * 在这里解包其他进程送来的数据
         */
        public void unpackReverseComm(int n, IntCPointer list, DoubleCPointer buf) throws Exception {}
        private void unpackReverseComm_(int n, long list, long buf) throws Exception {
            unpackReverseComm(n, new IntCPointer(list), new DoubleCPointer(buf));
        }

        
        /// lammps pair 提供的接口
        protected final int findVariable(String aName) {
            if (aName == null) throw new NullPointerException();
            return findVariable_(mPairPtr, aName);
        }
        private native static int findVariable_(long aPairPtr, @NotNull String name);
        
        protected final double computeVariable(int aIdx) {
            if (aIdx < 0) throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            return computeVariable_(mPairPtr, aIdx);
        }
        private native static double computeVariable_(long aPairPtr, int ivar);
        
        protected final void setSingleEnable(boolean aFlag) {setSingleEnable_(mPairPtr, aFlag);}
        private native static void setSingleEnable_(long aPairPtr, boolean aFlag);
        
        protected final void setOneCoeff(boolean aFlag) {setOneCoeff_(mPairPtr, aFlag);}
        private native static void setOneCoeff_(long aPairPtr, boolean aFlag);
        
        protected final void setManybodyFlag(boolean aFlag) {setManybodyFlag_(mPairPtr, aFlag);}
        private native static void setManybodyFlag_(long aPairPtr, boolean aFlag);
        
        protected final void setUnitConvertFlag(int aFlag) {setUnitConvertFlag_(mPairPtr, aFlag);}
        private native static void setUnitConvertFlag_(long aPairPtr, int aFlag);
        
        protected final void setNoVirialFdotrCompute(boolean aFlag) {setNoVirialFdotrCompute_(mPairPtr, aFlag);}
        protected final void noVirialFdotrCompute() {setNoVirialFdotrCompute_(mPairPtr, true);}
        private native static void setNoVirialFdotrCompute_(long aPairPtr, boolean aFlag);
        
        protected final void setFinitecutflag(boolean aFlag) {setFinitecutflag_(mPairPtr, aFlag);}
        private native static void setFinitecutflag_(long aPairPtr, boolean aFlag);
        
        protected final void setGhostneigh(boolean aFlag) {setGhostneigh_(mPairPtr, aFlag);}
        private native static void setGhostneigh_(long aPairPtr, boolean aFlag);
        
        protected final void setCentroidstressflag(int aFlag) {setCentroidstressflag_(mPairPtr, aFlag);}
        private native static void setCentroidstressflag_(long aPairPtr, int aFlag);
        
        protected final void setCommForward(int aSize) {setCommForward_(mPairPtr, aSize);}
        private native static void setCommForward_(long aPairPtr, int aSize);
        
        protected final void setCommReverse(int aSize) {setCommReverse_(mPairPtr, aSize);}
        private native static void setCommReverse_(long aPairPtr, int aSize);
        
        protected final void setCommReverseOff(int aSize) {setCommReverseOff_(mPairPtr, aSize);}
        private native static void setCommReverseOff_(long aPairPtr, int aSize);
        
        protected final void neighborRequestDefault() {neighborRequestDefault_(mPairPtr);}
        private native static void neighborRequestDefault_(long aPairPtr);
        
        protected final void neighborRequestFull() {neighborRequestFull_(mPairPtr);}
        private native static void neighborRequestFull_(long aPairPtr);
        
        protected final NestedDoubleCPointer atomX() {return new NestedDoubleCPointer(atomX_(mPairPtr));}
        private native static long atomX_(long aPairPtr);
        
        protected final NestedDoubleCPointer atomV() {return new NestedDoubleCPointer(atomV_(mPairPtr));}
        private native static long atomV_(long aPairPtr);
        
        protected final NestedDoubleCPointer atomF() {return new NestedDoubleCPointer(atomF_(mPairPtr));}
        private native static long atomF_(long aPairPtr);
        
        protected final IntCPointer atomTag() {
            if (NativeLmp.LAMMPS_BIGBIG) throw new UnsupportedOperationException("atomTag for LAMMPS_BIGBIG");
            return new IntCPointer(atomTag_(mPairPtr));
        }
        private native static long atomTag_(long aPairPtr);
        
        protected final IntCPointer atomType() {return new IntCPointer(atomType_(mPairPtr));}
        private native static long atomType_(long aPairPtr);
        
        protected final DoubleCPointer atomMass() {return new DoubleCPointer(atomMass_(mPairPtr));}
        private native static long atomMass_(long aPairPtr);
        
        protected final CPointer atomExtract(String aName) {return new CPointer(atomExtract_(mPairPtr, aName));}
        private native static long atomExtract_(long aPairPtr, String aName);
        
        protected final long atomNatoms() {return atomNatoms_(mPairPtr);}
        private native static long atomNatoms_(long aPairPtr);
        
        protected final int atomNtypes() {return atomNtypes_(mPairPtr);}
        private native static int atomNtypes_(long aPairPtr);
        
        protected final int atomNlocal() {return atomNlocal_(mPairPtr);}
        private native static int atomNlocal_(long aPairPtr);
        
        protected final int atomNghost() {return atomNghost_(mPairPtr);}
        private native static int atomNghost_(long aPairPtr);
        
        protected final int atomNmax() {return atomNmax_(mPairPtr);}
        private native static int atomNmax_(long aPairPtr);
        
        protected final DoubleCPointer forceSpecialLj() {return new DoubleCPointer(forceSpecialLj_(mPairPtr));}
        private native static long forceSpecialLj_(long aPairPtr);
        
        protected final boolean forceNewtonPair() {return forceNewtonPair_(mPairPtr);}
        private native static boolean forceNewtonPair_(long aPairPtr);
        
        protected final int listGnum() {return listGnum_(mPairPtr);}
        private native static int listGnum_(long aPairPtr);
        
        protected final int listInum() {return listInum_(mPairPtr);}
        private native static int listInum_(long aPairPtr);
        
        protected final IntCPointer listIlist() {return new IntCPointer(listIlist_(mPairPtr));}
        private native static long listIlist_(long aPairPtr);
        
        protected final IntCPointer listNumneigh() {return new IntCPointer(listNumneigh_(mPairPtr));}
        private native static long listNumneigh_(long aPairPtr);
        
        protected final NestedIntCPointer listFirstneigh() {return new NestedIntCPointer(listFirstneigh_(mPairPtr));}
        private native static long listFirstneigh_(long aPairPtr);
        
        protected static int sbmask(int j) {return (j >> SBBITS) & 3;}
        
        protected final double cutsq(int i, int j) {return cutsq_(mPairPtr, i, j);}
        private native static double cutsq_(long aPairPtr, int i, int j);
        
        protected final DoubleCPointer engVdwl() {return new DoubleCPointer(engVdwl_(mPairPtr));}
        private native static long engVdwl_(long aPairPtr);
        
        protected final DoubleCPointer engCoul() {return new DoubleCPointer(engCoul_(mPairPtr));}
        private native static long engCoul_(long aPairPtr);
        
        protected final DoubleCPointer eatom() {return new DoubleCPointer(eatom_(mPairPtr));}
        private native static long eatom_(long aPairPtr);
        
        protected final DoubleCPointer virial() {return new DoubleCPointer(virial_(mPairPtr));}
        private native static long virial_(long aPairPtr);
        
        protected final NestedDoubleCPointer vatom() {return new NestedDoubleCPointer(vatom_(mPairPtr));}
        private native static long vatom_(long aPairPtr);
        
        protected final NestedDoubleCPointer cvatom() {return new NestedDoubleCPointer(cvatom_(mPairPtr));}
        private native static long cvatom_(long aPairPtr);
        
        protected final void evTally(int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fpair, double delx, double dely, double delz) {evTally_(mPairPtr, i, j, nlocal, newtonPair, evdwl, ecoul, fpair, delx, dely, delz);}
        private native static void evTally_(long aPairPtr, int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fpair, double delx, double dely, double delz);
        
        protected final void evTallyFull(int i, double evdwl, double ecoul, double fpair, double delx, double dely, double delz) {evTallyFull_(mPairPtr, i, evdwl, ecoul, fpair, delx, dely, delz);}
        private native static void evTallyFull_(long aPairPtr, int i, double evdwl, double ecoul, double fpair, double delx, double dely, double delz);
        
        protected final void evTallyXYZ(int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz) {evTallyXYZ_(mPairPtr, i, j, nlocal, newtonPair, evdwl, ecoul, fx, fy, fz, delx, dely, delz);}
        private native static void evTallyXYZ_(long aPairPtr, int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz);
        
        protected final void evTallyXYZFull(int i, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz) {evTallyXYZFull_(mPairPtr, i, evdwl, ecoul, fx, fy, fz, delx, dely, delz);}
        private native static void evTallyXYZFull_(long aPairPtr, int i, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz);
        
        protected final boolean evflag() {return evflag_(mPairPtr);}
        private native static boolean evflag_(long aPairPtr);
        
        protected final boolean vflagEither() {return vflagEither_(mPairPtr);}
        private native static boolean vflagEither_(long aPairPtr);
        
        protected final boolean vflagGlobal() {return vflagGlobal_(mPairPtr);}
        private native static boolean vflagGlobal_(long aPairPtr);
        
        protected final boolean vflagAtom() {return vflagAtom_(mPairPtr);}
        private native static boolean vflagAtom_(long aPairPtr);
        
        protected final boolean cvflagAtom() {return cvflagAtom_(mPairPtr);}
        private native static boolean cvflagAtom_(long aPairPtr);
        
        protected final boolean eflagEither() {return eflagEither_(mPairPtr);}
        private native static boolean eflagEither_(long aPairPtr);
        
        protected final boolean eflagGlobal() {return eflagGlobal_(mPairPtr);}
        private native static boolean eflagGlobal_(long aPairPtr);
        
        protected final boolean eflagAtom() {return eflagAtom_(mPairPtr);}
        private native static boolean eflagAtom_(long aPairPtr);
        
        protected final boolean vflagFdotr() {return vflagFdotr_(mPairPtr);}
        private native static boolean vflagFdotr_(long aPairPtr);
        
        protected final void virialFdotrCompute() {virialFdotrCompute_(mPairPtr);}
        private native static void virialFdotrCompute_(long aPairPtr);
        
        protected final int commMe() {return commMe_(mPairPtr);}
        private native static int commMe_(long aPairPtr);
        
        protected final int commNprocs() {return commNprocs_(mPairPtr);}
        private native static int commNprocs_(long aPairPtr);
        
        protected final MPI.Comm commWorld() {return MPI.Comm.of(commWorld_(mPairPtr));}
        private native static long commWorld_(long aPairPtr);
        
        protected final void commForwardComm() {commForwardComm_(mPairPtr);}
        private native static void commForwardComm_(long aPairPtr);
        
        protected final void commReverseComm() {commReverseComm_(mPairPtr);}
        private native static void commReverseComm_(long aPairPtr);
        
        protected final String unitStyle() {return unitStyle_(mPairPtr);}
        private native static String unitStyle_(long aPairPtr);
    }
    
    
    public static abstract class Fix implements IAutoShutdown {
        static {
            // 确保 LmpPlugin 已经确实初始化
            LmpPlugin.InitHelper.init();
        }
        /** 目前认为这些值永远都不会变 */
        public final static int
              INITIAL_INTEGRATE         = 1
            , POST_INTEGRATE            = 1 << 1
            , PRE_EXCHANGE              = 1 << 2
            , PRE_NEIGHBOR              = 1 << 3
            , POST_NEIGHBOR             = 1 << 4
            , PRE_FORCE                 = 1 << 5
            , PRE_REVERSE               = 1 << 6
            , POST_FORCE                = 1 << 7
            , FINAL_INTEGRATE           = 1 << 8
            , END_OF_STEP               = 1 << 9
            , POST_RUN                  = 1 << 10
            , MIN_PRE_EXCHANGE          = 1 << 16
            , MIN_PRE_NEIGHBOR          = 1 << 17
            , MIN_POST_NEIGHBOR         = 1 << 18
            , MIN_PRE_FORCE             = 1 << 19
            , MIN_PRE_REVERSE           = 1 << 20
            , MIN_POST_FORCE            = 1 << 21
            ;
        public final static int
              NO_BOX_CHANGE     = 0
            , BOX_CHANGE_ANY    = 1
            , BOX_CHANGE_DOMAIN = 1 << 1
            , BOX_CHANGE_X      = 1 << 2
            , BOX_CHANGE_Y      = 1 << 3
            , BOX_CHANGE_Z      = 1 << 4
            , BOX_CHANGE_YZ     = 1 << 5
            , BOX_CHANGE_XZ     = 1 << 6
            , BOX_CHANGE_XY     = 1 << 7
            , BOX_CHANGE_SIZE   = BOX_CHANGE_X  | BOX_CHANGE_Y  | BOX_CHANGE_Z
            , BOX_CHANGE_SHAPE  = BOX_CHANGE_YZ | BOX_CHANGE_XZ | BOX_CHANGE_XY
            ;
        
        /**
         * 通过反射来获取类，可以是文件路径，也可以是类路径；
         * 这里简单处理，先尝试当文件路径，然后再尝试当类路径
         * @param aClassNameOrPath 类路径的名称或者是 groovy 的脚本文件路径
         * @param aFixPtr lammps jse fix 对应类的指针
         * @param aArgs 初始化 fix 的参数，这个参数和 lammps fix 参数保持一致为完整的参数
         * @return 需要的对象
         * @author liqa
         */
        public static Fix of(String aClassNameOrPath, long aFixPtr, String... aArgs) throws Exception {
            Class<?> tClazz;
            try {
                tClazz = SP.Groovy.parseClass(aClassNameOrPath);
            } catch (Exception e) {
                try {
                    tClazz = SP.Groovy.getClass(aClassNameOrPath);
                } catch (Exception ex) {
                    // 这里简单判断然后抛出合适的错误
                    if (aClassNameOrPath.endsWith(".groovy") || aClassNameOrPath.contains("/") || aClassNameOrPath.contains("\\") || aClassNameOrPath.contains(File.separator)) throw e;
                    else throw ex;
                }
            }
            return (Fix)InvokerHelper.invokeConstructorOf(tClazz, new Object[]{aFixPtr, aArgs});
        }
        
        
        protected final long mFixPtr;
        /**
         * @param aFixPtr lammps jse fix 对应类的指针
         * @param aArgs 初始化 fix 的参数，这个参数和 lammps fix 参数保持一致为完整的参数，因此一般来说需要从
         *        {@code aArgs[4]} 来获取后续参数
         */
        protected Fix(long aFixPtr, String... aArgs) {mFixPtr = aFixPtr;}
        
        /// lammps fix 初始化调用的接口
        public abstract int setMask() throws Exception;
        public void init() throws Exception {/**/}
        public void setup(int aVFlag) throws Exception {/**/}
        public void minSetup(int aVFlag) throws Exception {/**/}
        
        /**
         * lammps fix 析构时调用，用于手动释放一些资源
         */
        @Override public void shutdown() {/**/}
        
        /// lammps fix 固定步数回调的钩子
        public void initialIntegrate(int aVFlag) throws Exception {/**/}
        public void postIntegrate() throws Exception {/**/}
        public void preExchange() throws Exception {/**/}
        public void preNeighbor() throws Exception {/**/}
        public void postNeighbor() throws Exception {/**/}
        public void preForce(int aVFlag) throws Exception {/**/}
        public void preReverse(int aEFlag, int aVFlag) throws Exception {/**/}
        public void postForce(int aVFlag) throws Exception {/**/}
        public void finalIntegrate() throws Exception {/**/}
        public void endOfStep() throws Exception {/**/}
        public void postRun() throws Exception {/**/}
        public void minPreExchange() throws Exception {/**/}
        public void minPreNeighbor() throws Exception {/**/}
        public void minPostNeighbor() throws Exception {/**/}
        public void minPreForce(int aVFlag) throws Exception {/**/}
        public void minPreReverse(int aEFlag, int aVFlag) throws Exception {/**/}
        public void minPostForce(int aVFlag) throws Exception {/**/}
        
        /**
         * lammps fix 中提供的 {@code pack_forward_comm} 方法包装，用于将数据送出到 ghost 原子
         * <p>
         * 在这里打包需要向其他进程送出的数据
         */
        public int packForwardComm(int n, IntCPointer list, DoubleCPointer buf, int pbc_flag, IntCPointer pbc) throws Exception {return 0;}
        private int packForwardComm_(int n, long list, long buf, int pbc_flag, long pbc) throws Exception {
            return packForwardComm(n, new IntCPointer(list), new DoubleCPointer(buf), pbc_flag, new IntCPointer(pbc));
        }
        /**
         * lammps fix 中提供的 {@code unpack_forward_comm} 方法包装，用于将数据送出到 ghost 原子
         * <p>
         * 在这里解包其他进程送来的数据
         */
        public void unpackForwardComm(int n, int first, DoubleCPointer buf) throws Exception {}
        private void unpackForwardComm_(int n, int first, long buf) throws Exception {
            unpackForwardComm(n, first, new DoubleCPointer(buf));
        }
        /**
         * lammps fix 中提供的 {@code pack_reverse_comm} 方法包装，用于从 ghost 原子获取数据
         * <p>
         * 在这里打包需要向其他进程送出的数据
         */
        public int packReverseComm(int n, int first, DoubleCPointer buf) throws Exception {return 0;}
        private int packReverseComm_(int n, int first, long buf) throws Exception {
            return packReverseComm(n, first, new DoubleCPointer(buf));
        }
        /**
         * lammps fix 中提供的 {@code unpack_reverse_comm} 方法包装，用于从 ghost 原子获取数据
         * <p>
         * 在这里解包其他进程送来的数据
         */
        public void unpackReverseComm(int n, IntCPointer list, DoubleCPointer buf) throws Exception {}
        private void unpackReverseComm_(int n, long list, long buf) throws Exception  {
            unpackReverseComm(n, new IntCPointer(list), new DoubleCPointer(buf));
        }
        
        /// lammps fix 可以用来计算获取的变量
        public double computeScalar() throws Exception {return 0.0;}
        public double computeVector(int i) throws Exception {return 0.0;}
        public double computeArray(int i, int j) throws Exception {return 0.0;}
        
        
        /// lammps fix 提供的接口
        protected final void setBoxChange(int aFlag) {setBoxChange_(mFixPtr, aFlag);}
        private native static void setBoxChange_(long aFixPtr, int aFlag);
        
        protected final void setNoChangeBox(boolean aFlag) {setNoChangeBox_(mFixPtr, aFlag);}
        private native static void setNoChangeBox_(long aFixPtr, boolean aFlag);
        
        protected final void setForceReneighbor(boolean aFlag) {setForceReneighbor_(mFixPtr, aFlag);}
        private native static void setForceReneighbor_(long aFixPtr, boolean aFlag);
        
        protected final void setNextReneighbor(long aTimestep) {setNextReneighbor_(mFixPtr, aTimestep);}
        private native static void setNextReneighbor_(long aFixPtr, long aTimestep);
        
        protected final long nextReneighbor() {return nextReneighbor_(mFixPtr);}
        private native static long nextReneighbor_(long aFixPtr);
        
        protected final void setNevery(int aNevery) {setNevery_(mFixPtr, aNevery);}
        private native static void setNevery_(long aFixPtr, int aNevery);
        
        protected final void setEnergyGlobalFlag(boolean aFlag) {setEnergyGlobalFlag_(mFixPtr, aFlag);}
        private native static void setEnergyGlobalFlag_(long aFixPtr, boolean aFlag);
        
        protected final void setEnergyPeratomFlag(boolean aFlag) {setEnergyPeratomFlag_(mFixPtr, aFlag);}
        private native static void setEnergyPeratomFlag_(long aFixPtr, boolean aFlag);
        
        protected final void setVirialGlobalFlag(boolean aFlag) {setVirialGlobalFlag_(mFixPtr, aFlag);}
        private native static void setVirialGlobalFlag_(long aFixPtr, boolean aFlag);
        
        protected final void setVirialPeratomFlag(boolean aFlag) {setVirialPeratomFlag_(mFixPtr, aFlag);}
        private native static void setVirialPeratomFlag_(long aFixPtr, boolean aFlag);
        
        protected final void setTimeDepend(boolean aFlag) {setTimeDepend_(mFixPtr, aFlag);}
        private native static void setTimeDepend_(long aFixPtr, boolean aFlag);
        
        protected final void setDynamicGroupAllow(boolean aFlag) {setDynamicGroupAllow_(mFixPtr, aFlag);}
        private native static void setDynamicGroupAllow_(long aFixPtr, boolean aFlag);
        
        protected final void setScalarFlag(boolean aFlag) {setScalarFlag_(mFixPtr, aFlag);}
        private native static void setScalarFlag_(long aFixPtr, boolean aFlag);
        
        protected final void setVectorFlag(boolean aFlag) {setVectorFlag_(mFixPtr, aFlag);}
        private native static void setVectorFlag_(long aFixPtr, boolean aFlag);
        
        protected final void setArrayFlag(boolean aFlag) {setArrayFlag_(mFixPtr, aFlag);}
        private native static void setArrayFlag_(long aFixPtr, boolean aFlag);
        
        protected final void setSizeVector(int aSize) {setSizeVector_(mFixPtr, aSize);}
        private native static void setSizeVector_(long aFixPtr, int aSize);
        
        protected final void setSizeArrayRows(int aRowNum) {setSizeArrayRows_(mFixPtr, aRowNum);}
        private native static void setSizeArrayRows_(long aFixPtr, int aRowNum);
        
        protected final void setSizeArrayCols(int aColNum) {setSizeArrayCols_(mFixPtr, aColNum);}
        private native static void setSizeArrayCols_(long aFixPtr, int aColNum);
        
        protected final void setGlobalFreq(int aFreq) {setGlobalFreq_(mFixPtr, aFreq);}
        private native static void setGlobalFreq_(long aFixPtr, int aFreq);
        
        protected final void setExtscalar(boolean aFlag) {setExtscalar_(mFixPtr, aFlag);}
        private native static void setExtscalar_(long aFixPtr, boolean aFlag);
        
        protected final void setExtvector(boolean aFlag) {setExtvector_(mFixPtr, aFlag);}
        private native static void setExtvector_(long aFixPtr, boolean aFlag);
        
        protected final void setExtarray(boolean aFlag) {setExtarray_(mFixPtr, aFlag);}
        private native static void setExtarray_(long aFixPtr, boolean aFlag);
        
        protected final void setCommForward(int aSize) {setCommForward_(mFixPtr, aSize);}
        private native static void setCommForward_(long aFixPtr, int aSize);
        
        protected final void setCommReverse(int aSize) {setCommReverse_(mFixPtr, aSize);}
        private native static void setCommReverse_(long aFixPtr, int aSize);
        
        protected final int findVariable(String aName) {
            if (aName == null) throw new NullPointerException();
            return findVariable_(mFixPtr, aName);
        }
        private native static int findVariable_(long aFixPtr, @NotNull String aName);
        
        protected final double computeVariable(int aIdx) {
            if (aIdx < 0) throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            return computeVariable_(mFixPtr, aIdx);
        }
        private native static double computeVariable_(long aFixPtr, int aIdx);
        
        @SuppressWarnings("SameParameterValue")
        protected final void neighborRequestDefault(double aRCut) {neighborRequestDefault_(mFixPtr, aRCut);}
        protected final void neighborRequestDefault() {neighborRequestDefault(-1);}
        private native static void neighborRequestDefault_(long aFixPtr, double aRCut);
        
        @SuppressWarnings("SameParameterValue")
        protected final void neighborRequestFull(double aRCut) {neighborRequestFull_(mFixPtr, aRCut);}
        protected final void neighborRequestFull() {neighborRequestFull(-1);}
        private native static void neighborRequestFull_(long aFixPtr, double aRCut);
        
        @SuppressWarnings("SameParameterValue")
        protected final void neighborRequestOccasional(double aRCut) {neighborRequestOccasional_(mFixPtr, aRCut);}
        protected final void neighborRequestOccasional() {neighborRequestOccasional(-1);}
        private native static void neighborRequestOccasional_(long aFixPtr, double aRCut);
        
        @SuppressWarnings("SameParameterValue")
        protected final void neighborRequestOccasionalFull(double aRCut) {neighborRequestOccasionalFull_(mFixPtr, aRCut);}
        protected final void neighborRequestOccasionalFull() {neighborRequestOccasionalFull(-1);}
        private native static void neighborRequestOccasionalFull_(long aFixPtr, double aRCut);
        
        protected final void neighborBuildOne() {neighborBuildOne_(mFixPtr);}
        private native static void neighborBuildOne_(long aFixPtr);
        
        protected final double neighborCutneighmin() {return neighborCutneighmin_(mFixPtr);}
        private native static double neighborCutneighmin_(long aFixPtr);
        
        protected final double neighborCutneighmax() {return neighborCutneighmax_(mFixPtr);}
        private native static double neighborCutneighmax_(long aFixPtr);
        
        protected final double neighborCuttype(int aType) {return neighborCuttype_(mFixPtr, aType);}
        private native static double neighborCuttype_(long aFixPtr, int aType);
        
        protected final double neighborSkin() {return neighborSkin_(mFixPtr);}
        private native static double neighborSkin_(long aFixPtr);
        
        protected final int igroup() {return igroup_(mFixPtr);}
        private native static int igroup_(long aFixPtr);
        
        protected final int groupbit() {return groupbit_(mFixPtr);}
        private native static int groupbit_(long aFixPtr);
        
        protected final NestedDoubleCPointer atomX() {return new NestedDoubleCPointer(atomX_(mFixPtr));}
        private native static long atomX_(long aFixPtr);
        
        protected final NestedDoubleCPointer atomV() {return new NestedDoubleCPointer(atomV_(mFixPtr));}
        private native static long atomV_(long aFixPtr);
        
        protected final NestedDoubleCPointer atomF() {return new NestedDoubleCPointer(atomF_(mFixPtr));}
        private native static long atomF_(long aFixPtr);
        
        protected final IntCPointer atomMask() {return new IntCPointer(atomMask_(mFixPtr));}
        private native static long atomMask_(long aFixPtr);
        
        protected final IntCPointer atomTag() {
            if (NativeLmp.LAMMPS_BIGBIG) throw new UnsupportedOperationException("atomTag for LAMMPS_BIGBIG");
            return new IntCPointer(atomTag_(mFixPtr));
        }
        private native static long atomTag_(long aFixPtr);
        
        protected final IntCPointer atomType() {return new IntCPointer(atomType_(mFixPtr));}
        private native static long atomType_(long aFixPtr);
        
        protected final DoubleCPointer atomMass() {return new DoubleCPointer(atomMass_(mFixPtr));}
        private native static long atomMass_(long aFixPtr);
        
        protected final CPointer atomExtract(String aName) {return new CPointer(atomExtract_(mFixPtr, aName));}
        private native static long atomExtract_(long aFixPtr, String aName);
        
        protected final long atomNatoms() {return atomNatoms_(mFixPtr);}
        private native static long atomNatoms_(long aFixPtr);
        
        protected final int atomNtypes() {return atomNtypes_(mFixPtr);}
        private native static int atomNtypes_(long aFixPtr);
        
        protected final int atomNlocal() {return atomNlocal_(mFixPtr);}
        private native static int atomNlocal_(long aFixPtr);
        
        protected final int atomNmax() {return atomNmax_(mFixPtr);}
        private native static int atomNmax_(long aFixPtr);
        
        protected final int atomNghost() {return atomNghost_(mFixPtr);}
        private native static int atomNghost_(long aFixPtr);
        
        protected final boolean domainTriclinic() {return domainTriclinic_(mFixPtr);}
        private native static boolean domainTriclinic_(long aFixPtr);
        
        protected final DoubleCPointer domainXy() {return new DoubleCPointer(domainXy_(mFixPtr));}
        private native static long domainXy_(long aFixPtr);
        
        protected final DoubleCPointer domainXz() {return new DoubleCPointer(domainXz_(mFixPtr));}
        private native static long domainXz_(long aFixPtr);
        
        protected final DoubleCPointer domainYz() {return new DoubleCPointer(domainYz_(mFixPtr));}
        private native static long domainYz_(long aFixPtr);
        
        protected final double domainXprd() {return domainXprd_(mFixPtr);}
        private native static double domainXprd_(long aFixPtr);
        
        protected final double domainYprd() {return domainYprd_(mFixPtr);}
        private native static double domainYprd_(long aFixPtr);
        
        protected final double domainZprd() {return domainZprd_(mFixPtr);}
        private native static double domainZprd_(long aFixPtr);
        
        protected final DoubleCPointer domainH() {return new DoubleCPointer(domainH_(mFixPtr));}
        private native static long domainH_(long aFixPtr);
        
        protected final DoubleCPointer domainHInv() {return new DoubleCPointer(domainHInv_(mFixPtr));}
        private native static long domainHInv_(long aFixPtr);
        
        protected final DoubleCPointer domainBoxlo() {return new DoubleCPointer(domainBoxlo_(mFixPtr));}
        private native static long domainBoxlo_(long aFixPtr);
        
        protected final DoubleCPointer domainBoxhi() {return new DoubleCPointer(domainBoxhi_(mFixPtr));}
        private native static long domainBoxhi_(long aFixPtr);
        
        protected final DoubleCPointer domainBoxloLamda() {return new DoubleCPointer(domainBoxloLamda_(mFixPtr));}
        private native static long domainBoxloLamda_(long aFixPtr);
        
        protected final DoubleCPointer domainBoxhiLamda() {return new DoubleCPointer(domainBoxhiLamda_(mFixPtr));}
        private native static long domainBoxhiLamda_(long aFixPtr);
        
        protected final DoubleCPointer domainSublo() {return new DoubleCPointer(domainSublo_(mFixPtr));}
        private native static long domainSublo_(long aFixPtr);
        
        protected final DoubleCPointer domainSubhi() {return new DoubleCPointer(domainSubhi_(mFixPtr));}
        private native static long domainSubhi_(long aFixPtr);
        
        protected final DoubleCPointer domainSubloLamda() {return new DoubleCPointer(domainSubloLamda_(mFixPtr));}
        private native static long domainSubloLamda_(long aFixPtr);
        
        protected final DoubleCPointer domainSubhiLamda() {return new DoubleCPointer(domainSubhiLamda_(mFixPtr));}
        private native static long domainSubhiLamda_(long aFixPtr);
        
        protected final void domainX2lamda(int aN) {domainX2lamda_(mFixPtr, aN);}
        private native static void domainX2lamda_(long aFixPtr, int aN);
        
        protected final void domainX2lamda(DoubleCPointer aX, DoubleCPointer rLamda) {domainX2lamda_(mFixPtr, aX.ptr_(), rLamda.ptr_());}
        private native static void domainX2lamda_(long aFixPtr, long aX, long rLamda);
        
        protected final void domainLamda2x(int aN) {domainLamda2x_(mFixPtr, aN);}
        private native static void domainLamda2x_(long aFixPtr, int aN);
        
        protected final void domainLamda2x(DoubleCPointer aLamda, DoubleCPointer rX) {domainLamda2x_(mFixPtr, aLamda.ptr_(), rX.ptr_());}
        private native static void domainLamda2x_(long aFixPtr, long aLamda, long rX);
        
        protected final void domainSetGlobalBox() {domainSetGlobalBox_(mFixPtr);}
        private native static void domainSetGlobalBox_(long aFixPtr);
        
        protected final void domainSetLocalBox() {domainSetLocalBox_(mFixPtr);}
        private native static void domainSetLocalBox_(long aFixPtr);
        
        protected final int listGnum() {return listGnum_(mFixPtr);}
        private native static int listGnum_(long aFixPtr);
        
        protected final int listInum() {return listInum_(mFixPtr);}
        private native static int listInum_(long aFixPtr);
        
        protected final IntCPointer listIlist() {return new IntCPointer(listIlist_(mFixPtr));}
        private native static long listIlist_(long aFixPtr);
        
        protected final IntCPointer listNumneigh() {return new IntCPointer(listNumneigh_(mFixPtr));}
        private native static long listNumneigh_(long aFixPtr);
        
        protected final NestedIntCPointer listFirstneigh() {return new NestedIntCPointer(listFirstneigh_(mFixPtr));}
        private native static long listFirstneigh_(long aFixPtr);
        
        protected final double forceBoltz() {return forceBoltz_(mFixPtr);}
        private native static double forceBoltz_(long aFixPtr);
        
        protected final double forcePairCutforce() {return forcePairCutforce_(mFixPtr);}
        private native static double forcePairCutforce_(long aFixPtr);
        
        protected final double dt() {return dt_(mFixPtr);}
        private native static double dt_(long aFixPtr);
        
        protected final long ntimestep() {return ntimestep_(mFixPtr);}
        private native static long ntimestep_(long aFixPtr);
        
        protected final long firststep() {return firststep_(mFixPtr);}
        private native static long firststep_(long aFixPtr);
        
        protected final long laststep() {return laststep_(mFixPtr);}
        private native static long laststep_(long aFixPtr);
        
        protected final long beginstep() {return beginstep_(mFixPtr);}
        private native static long beginstep_(long aFixPtr);
        
        protected final long endstep() {return endstep_(mFixPtr);}
        private native static long endstep_(long aFixPtr);
        
        protected final int commMe() {return commMe_(mFixPtr);}
        private native static int commMe_(long aFixPtr);
        
        protected final int commNprocs() {return commNprocs_(mFixPtr);}
        private native static int commNprocs_(long aFixPtr);
        
        protected final MPI.Comm commWorld() {return MPI.Comm.of(commWorld_(mFixPtr));}
        private native static long commWorld_(long aFixPtr);
        
        protected final double commCutghostuser() {return commCutghostuser_(mFixPtr);}
        private native static double commCutghostuser_(long aFixPtr);
        
        protected final void commForwardComm() {commForwardComm_(mFixPtr);}
        private native static void commForwardComm_(long aFixPtr);
        
        protected final void commReverseComm() {commReverseComm_(mFixPtr);}
        private native static void commReverseComm_(long aFixPtr);
        
        protected final String unitStyle() {return unitStyle_(mFixPtr);}
        private native static String unitStyle_(long aFixPtr);
    }
}
