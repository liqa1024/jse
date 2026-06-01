package jse.lmp;

import jse.clib.*;
import jse.code.*;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import jse.cptr.*;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

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
        private final static String DEFAULT_LMP_VERSION = LibVer.LMP;
        
        /** 插件是否开启 debug 模式，此时会让所有 rank 都输出异常 */
        public static boolean DEBUG = OS.envZ("JSE_DEBUG_LMPPLUGIN", jse.code.Conf.DEBUG);
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
        Map<String, String> rCmakeSetting = new LinkedHashMap<>(LmpCore.Conf.CMAKE_SETTING_SHARE);
        if (Conf.DEBUG) rCmakeSetting.put("JSE_DEBUG_MODE", "ON");
        LIB_DIR = LmpCore.ROOT+"plugin/" + UT.Code.uniqueID(LmpCore.LIB_PATH, Conf.JVM_XMX, rCmakeSetting) + "/";
        final String[] fLmpVersion = new String[1];
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("lmpplugin", "LMPPLUGIN", LIB_DIR, rCmakeSetting)
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
    
    
    public static abstract class Pair implements AutoCloseable {
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
        @Override public void close() throws Exception {/**/}
        
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
        public final int findVariable(String aName) {
            if (aName == null) throw new NullPointerException();
            return findVariable0(mPairPtr, aName);
        }
        private native static int findVariable0(long aPairPtr, @NotNull String aName);
        
        @UnsafeJNI("Illegal values will directly result in JVM SIGSEGV")
        public final double computeVariable(int aIdx) {
            if (aIdx < 0) throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            return computeVariable0(mPairPtr, aIdx);
        }
        private native static double computeVariable0(long aPairPtr, int ivar);
        
        public final void citemeAdd(String aCite) {
            if (aCite == null) throw new NullPointerException();
            citemeAdd0(mPairPtr, aCite);
        }
        private native static void citemeAdd0(long aPairPtr, @NotNull String aCite);
        
        public final void setSingleEnable(boolean aFlag) {setSingleEnable0(mPairPtr, aFlag);}
        private native static void setSingleEnable0(long aPairPtr, boolean aFlag);
        
        public final void setOneCoeff(boolean aFlag) {setOneCoeff0(mPairPtr, aFlag);}
        private native static void setOneCoeff0(long aPairPtr, boolean aFlag);
        
        public final void setManybodyFlag(boolean aFlag) {setManybodyFlag0(mPairPtr, aFlag);}
        private native static void setManybodyFlag0(long aPairPtr, boolean aFlag);
        
        public final void setUnitConvertFlag(int aFlag) {setUnitConvertFlag0(mPairPtr, aFlag);}
        private native static void setUnitConvertFlag0(long aPairPtr, int aFlag);
        
        public final void setNoVirialFdotrCompute(boolean aFlag) {setNoVirialFdotrCompute0(mPairPtr, aFlag);}
        public final void noVirialFdotrCompute() {setNoVirialFdotrCompute0(mPairPtr, true);}
        private native static void setNoVirialFdotrCompute0(long aPairPtr, boolean aFlag);
        
        public final void setFinitecutflag(boolean aFlag) {setFinitecutflag0(mPairPtr, aFlag);}
        private native static void setFinitecutflag0(long aPairPtr, boolean aFlag);
        
        public final void setGhostneigh(boolean aFlag) {setGhostneigh0(mPairPtr, aFlag);}
        private native static void setGhostneigh0(long aPairPtr, boolean aFlag);
        
        public final void setCentroidstressflag(int aFlag) {setCentroidstressflag0(mPairPtr, aFlag);}
        private native static void setCentroidstressflag0(long aPairPtr, int aFlag);
        
        public final void setCommForward(int aSize) {setCommForward0(mPairPtr, aSize);}
        private native static void setCommForward0(long aPairPtr, int aSize);
        
        public final void setCommReverse(int aSize) {setCommReverse0(mPairPtr, aSize);}
        private native static void setCommReverse0(long aPairPtr, int aSize);
        
        public final void setCommReverseOff(int aSize) {setCommReverseOff0(mPairPtr, aSize);}
        private native static void setCommReverseOff0(long aPairPtr, int aSize);
        
        public final void neighborRequestDefault() {neighborRequestDefault0(mPairPtr);}
        private native static void neighborRequestDefault0(long aPairPtr);
        
        public final void neighborRequestFull() {neighborRequestFull0(mPairPtr);}
        private native static void neighborRequestFull0(long aPairPtr);
        
        public final int neighborAgo() {return neighborAgo0(mPairPtr);}
        private native static int neighborAgo0(long aPairPtr);
        
        public final NestedDoubleCPointer atomX() {return new NestedDoubleCPointer(atomX0(mPairPtr));}
        private native static long atomX0(long aPairPtr);
        
        public final NestedDoubleCPointer atomV() {return new NestedDoubleCPointer(atomV0(mPairPtr));}
        private native static long atomV0(long aPairPtr);
        
        public final NestedDoubleCPointer atomF() {return new NestedDoubleCPointer(atomF0(mPairPtr));}
        private native static long atomF0(long aPairPtr);
        
        public final IntCPointer atomTag() {
            if (NativeLmp.LAMMPS_BIGBIG) throw new UnsupportedOperationException("atomTag for LAMMPS_BIGBIG");
            return new IntCPointer(atomTag0(mPairPtr));
        }
        private native static long atomTag0(long aPairPtr);
        
        public final IntCPointer atomType() {return new IntCPointer(atomType0(mPairPtr));}
        private native static long atomType0(long aPairPtr);
        
        public final DoubleCPointer atomMass() {return new DoubleCPointer(atomMass0(mPairPtr));}
        private native static long atomMass0(long aPairPtr);
        
        public final CPointer atomExtract(String aName) {return new CPointer(atomExtract0(mPairPtr, aName));}
        private native static long atomExtract0(long aPairPtr, String aName);
        
        public final long atomNatoms() {return atomNatoms0(mPairPtr);}
        private native static long atomNatoms0(long aPairPtr);
        
        public final int atomNtypes() {return atomNtypes0(mPairPtr);}
        private native static int atomNtypes0(long aPairPtr);
        
        public final int atomNlocal() {return atomNlocal0(mPairPtr);}
        private native static int atomNlocal0(long aPairPtr);
        
        public final int atomNghost() {return atomNghost0(mPairPtr);}
        private native static int atomNghost0(long aPairPtr);
        
        public final int atomNmax() {return atomNmax0(mPairPtr);}
        private native static int atomNmax0(long aPairPtr);
        
        public final DoubleCPointer forceSpecialLj() {return new DoubleCPointer(forceSpecialLj0(mPairPtr));}
        private native static long forceSpecialLj0(long aPairPtr);
        
        public final boolean forceNewtonPair() {return forceNewtonPair0(mPairPtr);}
        private native static boolean forceNewtonPair0(long aPairPtr);
        
        public final int listGnum() {return listGnum0(mPairPtr);}
        private native static int listGnum0(long aPairPtr);
        
        public final int listInum() {return listInum0(mPairPtr);}
        private native static int listInum0(long aPairPtr);
        
        public final IntCPointer listIlist() {return new IntCPointer(listIlist0(mPairPtr));}
        private native static long listIlist0(long aPairPtr);
        
        public final IntCPointer listNumneigh() {return new IntCPointer(listNumneigh0(mPairPtr));}
        private native static long listNumneigh0(long aPairPtr);
        
        public final NestedIntCPointer listFirstneigh() {return new NestedIntCPointer(listFirstneigh0(mPairPtr));}
        private native static long listFirstneigh0(long aPairPtr);
        
        public static int sbmask(int j) {return (j >> SBBITS) & 3;}
        
        public final double cutsq(int i, int j) {return cutsq0(mPairPtr, i, j);}
        private native static double cutsq0(long aPairPtr, int i, int j);
        
        public final DoubleCPointer engVdwl() {return new DoubleCPointer(engVdwl0(mPairPtr));}
        private native static long engVdwl0(long aPairPtr);
        
        public final DoubleCPointer engCoul() {return new DoubleCPointer(engCoul0(mPairPtr));}
        private native static long engCoul0(long aPairPtr);
        
        public final DoubleCPointer eatom() {return new DoubleCPointer(eatom0(mPairPtr));}
        private native static long eatom0(long aPairPtr);
        
        public final DoubleCPointer virial() {return new DoubleCPointer(virial0(mPairPtr));}
        private native static long virial0(long aPairPtr);
        
        public final NestedDoubleCPointer vatom() {return new NestedDoubleCPointer(vatom0(mPairPtr));}
        private native static long vatom0(long aPairPtr);
        
        public final NestedDoubleCPointer cvatom() {return new NestedDoubleCPointer(cvatom0(mPairPtr));}
        private native static long cvatom0(long aPairPtr);
        
        public final void evTally(int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fpair, double delx, double dely, double delz) {evTally0(mPairPtr, i, j, nlocal, newtonPair, evdwl, ecoul, fpair, delx, dely, delz);}
        private native static void evTally0(long aPairPtr, int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fpair, double delx, double dely, double delz);
        
        public final void evTallyFull(int i, double evdwl, double ecoul, double fpair, double delx, double dely, double delz) {evTallyFull0(mPairPtr, i, evdwl, ecoul, fpair, delx, dely, delz);}
        private native static void evTallyFull0(long aPairPtr, int i, double evdwl, double ecoul, double fpair, double delx, double dely, double delz);
        
        public final void evTallyXYZ(int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz) {evTallyXYZ0(mPairPtr, i, j, nlocal, newtonPair, evdwl, ecoul, fx, fy, fz, delx, dely, delz);}
        private native static void evTallyXYZ0(long aPairPtr, int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz);
        
        public final void evTallyXYZFull(int i, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz) {evTallyXYZFull0(mPairPtr, i, evdwl, ecoul, fx, fy, fz, delx, dely, delz);}
        private native static void evTallyXYZFull0(long aPairPtr, int i, double evdwl, double ecoul, double fx, double fy, double fz, double delx, double dely, double delz);
        
        public final boolean evflag() {return evflag0(mPairPtr);}
        private native static boolean evflag0(long aPairPtr);
        
        public final boolean vflagEither() {return vflagEither0(mPairPtr);}
        private native static boolean vflagEither0(long aPairPtr);
        
        public final boolean vflagGlobal() {return vflagGlobal0(mPairPtr);}
        private native static boolean vflagGlobal0(long aPairPtr);
        
        public final boolean vflagAtom() {return vflagAtom0(mPairPtr);}
        private native static boolean vflagAtom0(long aPairPtr);
        
        public final boolean cvflagAtom() {return cvflagAtom0(mPairPtr);}
        private native static boolean cvflagAtom0(long aPairPtr);
        
        public final boolean eflagEither() {return eflagEither0(mPairPtr);}
        private native static boolean eflagEither0(long aPairPtr);
        
        public final boolean eflagGlobal() {return eflagGlobal0(mPairPtr);}
        private native static boolean eflagGlobal0(long aPairPtr);
        
        public final boolean eflagAtom() {return eflagAtom0(mPairPtr);}
        private native static boolean eflagAtom0(long aPairPtr);
        
        public final boolean vflagFdotr() {return vflagFdotr0(mPairPtr);}
        private native static boolean vflagFdotr0(long aPairPtr);
        
        public final void virialFdotrCompute() {virialFdotrCompute0(mPairPtr);}
        private native static void virialFdotrCompute0(long aPairPtr);
        
        public final int commMe() {return commMe0(mPairPtr);}
        private native static int commMe0(long aPairPtr);
        
        public final int commNprocs() {return commNprocs0(mPairPtr);}
        private native static int commNprocs0(long aPairPtr);
        
        public final void commBarrier() throws MPIException {commBarrier0(mPairPtr);}
        private native static void commBarrier0(long aPairPtr) throws MPIException;
        
        public final MPI.Comm commWorld() {return MPI.Comm.of(commWorld0(mPairPtr));}
        private native static long commWorld0(long aPairPtr);
        
        public final void commForwardComm() {commForwardComm0(mPairPtr);}
        private native static void commForwardComm0(long aPairPtr);
        
        public final void commForwardCommThis() {commForwardCommThis0(mPairPtr);}
        private native static void commForwardCommThis0(long aPairPtr);
        
        public final void commReverseComm() {commReverseComm0(mPairPtr);}
        private native static void commReverseComm0(long aPairPtr);
        
        public final void commReverseCommThis() {commReverseCommThis0(mPairPtr);}
        private native static void commReverseCommThis0(long aPairPtr);
        
        public final String unitStyle() {return unitStyle0(mPairPtr);}
        private native static String unitStyle0(long aPairPtr);
    }
    
    
    public static abstract class Fix implements AutoCloseable {
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
        @Override public void close() throws Exception {/**/}
        
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
        public final void setBoxChange(int aFlag) {setBoxChange0(mFixPtr, aFlag);}
        private native static void setBoxChange0(long aFixPtr, int aFlag);
        
        public final void setNoChangeBox(boolean aFlag) {setNoChangeBox0(mFixPtr, aFlag);}
        private native static void setNoChangeBox0(long aFixPtr, boolean aFlag);
        
        public final void setForceReneighbor(boolean aFlag) {setForceReneighbor0(mFixPtr, aFlag);}
        private native static void setForceReneighbor0(long aFixPtr, boolean aFlag);
        
        public final void setNextReneighbor(long aTimestep) {setNextReneighbor0(mFixPtr, aTimestep);}
        private native static void setNextReneighbor0(long aFixPtr, long aTimestep);
        
        public final long nextReneighbor() {return nextReneighbor0(mFixPtr);}
        private native static long nextReneighbor0(long aFixPtr);
        
        public final void setNevery(int aNevery) {setNevery0(mFixPtr, aNevery);}
        private native static void setNevery0(long aFixPtr, int aNevery);
        
        public final void setEnergyGlobalFlag(boolean aFlag) {setEnergyGlobalFlag0(mFixPtr, aFlag);}
        private native static void setEnergyGlobalFlag0(long aFixPtr, boolean aFlag);
        
        public final void setEnergyPeratomFlag(boolean aFlag) {setEnergyPeratomFlag0(mFixPtr, aFlag);}
        private native static void setEnergyPeratomFlag0(long aFixPtr, boolean aFlag);
        
        public final void setVirialGlobalFlag(boolean aFlag) {setVirialGlobalFlag0(mFixPtr, aFlag);}
        private native static void setVirialGlobalFlag0(long aFixPtr, boolean aFlag);
        
        public final void setVirialPeratomFlag(boolean aFlag) {setVirialPeratomFlag0(mFixPtr, aFlag);}
        private native static void setVirialPeratomFlag0(long aFixPtr, boolean aFlag);
        
        public final void setTimeDepend(boolean aFlag) {setTimeDepend0(mFixPtr, aFlag);}
        private native static void setTimeDepend0(long aFixPtr, boolean aFlag);
        
        public final void setDynamicGroupAllow(boolean aFlag) {setDynamicGroupAllow0(mFixPtr, aFlag);}
        private native static void setDynamicGroupAllow0(long aFixPtr, boolean aFlag);
        
        public final void setScalarFlag(boolean aFlag) {setScalarFlag0(mFixPtr, aFlag);}
        private native static void setScalarFlag0(long aFixPtr, boolean aFlag);
        
        public final void setVectorFlag(boolean aFlag) {setVectorFlag0(mFixPtr, aFlag);}
        private native static void setVectorFlag0(long aFixPtr, boolean aFlag);
        
        public final void setArrayFlag(boolean aFlag) {setArrayFlag0(mFixPtr, aFlag);}
        private native static void setArrayFlag0(long aFixPtr, boolean aFlag);
        
        public final void setSizeVector(int aSize) {setSizeVector0(mFixPtr, aSize);}
        private native static void setSizeVector0(long aFixPtr, int aSize);
        
        public final void setSizeArrayRows(int aRows) {setSizeArrayRows0(mFixPtr, aRows);}
        private native static void setSizeArrayRows0(long aFixPtr, int aRows);
        
        public final void setSizeArrayCols(int aCols) {setSizeArrayCols0(mFixPtr, aCols);}
        private native static void setSizeArrayCols0(long aFixPtr, int aCols);
        
        public final void setGlobalFreq(int aFreq) {setGlobalFreq0(mFixPtr, aFreq);}
        private native static void setGlobalFreq0(long aFixPtr, int aFreq);
        
        public final void setExtscalar(boolean aFlag) {setExtscalar0(mFixPtr, aFlag);}
        private native static void setExtscalar0(long aFixPtr, boolean aFlag);
        
        public final void setExtvector(boolean aFlag) {setExtvector0(mFixPtr, aFlag);}
        private native static void setExtvector0(long aFixPtr, boolean aFlag);
        
        public final void setExtarray(boolean aFlag) {setExtarray0(mFixPtr, aFlag);}
        private native static void setExtarray0(long aFixPtr, boolean aFlag);
        
        public final void setPeratomFlag(boolean aFlag) {setPeratomFlag0(mFixPtr, aFlag);}
        private native static void setPeratomFlag0(long aFixPtr, boolean aFlag);
        
        public final void setSizePeratomCols(int aCols) {setSizePeratomCols0(mFixPtr, aCols);}
        private native static void setSizePeratomCols0(long aFixPtr, int aCols);
        
        public final void setPeratomFreq(int aFreq) {setPeratomFreq0(mFixPtr, aFreq);}
        private native static void setPeratomFreq0(long aFixPtr, int aFreq);
        
        public final void setVectorAtom(DoubleCPointer aPtr) {setVectorAtom0(mFixPtr, aPtr.ptr_());}
        private native static void setVectorAtom0(long aFixPtr, long aPtr);
        
        public final void setArrayAtom(NestedDoubleCPointer aPtr) {setArrayAtom0(mFixPtr, aPtr.ptr_());}
        private native static void setArrayAtom0(long aFixPtr, long aPtr);
        
        public final void setCommForward(int aSize) {setCommForward0(mFixPtr, aSize);}
        private native static void setCommForward0(long aFixPtr, int aSize);
        
        public final void setCommReverse(int aSize) {setCommReverse0(mFixPtr, aSize);}
        private native static void setCommReverse0(long aFixPtr, int aSize);
        
        public final int findVariable(String aName) {
            if (aName == null) throw new NullPointerException();
            return findVariable0(mFixPtr, aName);
        }
        private native static int findVariable0(long aFixPtr, @NotNull String aName);
        
        @UnsafeJNI("Illegal values will directly result in JVM SIGSEGV")
        public final double computeVariable(int aIdx) {
            if (aIdx < 0) throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            return computeVariable0(mFixPtr, aIdx);
        }
        private native static double computeVariable0(long aFixPtr, int aIdx);
        
        public final void citemeAdd(String aCite) {
            if (aCite == null) throw new NullPointerException();
            citemeAdd0(mFixPtr, aCite);
        }
        private native static void citemeAdd0(long aFixPtr, @NotNull String aCite);
        
        @SuppressWarnings("SameParameterValue")
        public final void neighborRequestDefault(double aRCut) {neighborRequestDefault0(mFixPtr, aRCut);}
        public final void neighborRequestDefault() {neighborRequestDefault(-1);}
        private native static void neighborRequestDefault0(long aFixPtr, double aRCut);
        
        @SuppressWarnings("SameParameterValue")
        public final void neighborRequestFull(double aRCut) {neighborRequestFull0(mFixPtr, aRCut);}
        public final void neighborRequestFull() {neighborRequestFull(-1);}
        private native static void neighborRequestFull0(long aFixPtr, double aRCut);
        
        @SuppressWarnings("SameParameterValue")
        public final void neighborRequestOccasional(double aRCut) {neighborRequestOccasional0(mFixPtr, aRCut);}
        public final void neighborRequestOccasional() {neighborRequestOccasional(-1);}
        private native static void neighborRequestOccasional0(long aFixPtr, double aRCut);
        
        @SuppressWarnings("SameParameterValue")
        public final void neighborRequestOccasionalFull(double aRCut) {neighborRequestOccasionalFull0(mFixPtr, aRCut);}
        public final void neighborRequestOccasionalFull() {neighborRequestOccasionalFull(-1);}
        private native static void neighborRequestOccasionalFull0(long aFixPtr, double aRCut);
        
        public final void neighborBuildOne() throws LmpException {neighborBuildOne0(mFixPtr);}
        private native static void neighborBuildOne0(long aFixPtr) throws LmpException;
        
        public final int neighborAgo() {return neighborAgo0(mFixPtr);}
        private native static int neighborAgo0(long aFixPtr);
        
        public final double neighborCutneighmin() {return neighborCutneighmin0(mFixPtr);}
        private native static double neighborCutneighmin0(long aFixPtr);
        
        public final double neighborCutneighmax() {return neighborCutneighmax0(mFixPtr);}
        private native static double neighborCutneighmax0(long aFixPtr);
        
        public final double neighborCuttype(int aType) {return neighborCuttype0(mFixPtr, aType);}
        private native static double neighborCuttype0(long aFixPtr, int aType);
        
        public final double neighborSkin() {return neighborSkin0(mFixPtr);}
        private native static double neighborSkin0(long aFixPtr);
        
        public final int igroup() {return igroup0(mFixPtr);}
        private native static int igroup0(long aFixPtr);
        
        public final int groupbit() {return groupbit0(mFixPtr);}
        private native static int groupbit0(long aFixPtr);
        
        public final NestedDoubleCPointer atomX() {return new NestedDoubleCPointer(atomX0(mFixPtr));}
        private native static long atomX0(long aFixPtr);
        
        public final NestedDoubleCPointer atomV() {return new NestedDoubleCPointer(atomV0(mFixPtr));}
        private native static long atomV0(long aFixPtr);
        
        public final NestedDoubleCPointer atomF() {return new NestedDoubleCPointer(atomF0(mFixPtr));}
        private native static long atomF0(long aFixPtr);
        
        public final IntCPointer atomMask() {return new IntCPointer(atomMask0(mFixPtr));}
        private native static long atomMask0(long aFixPtr);
        
        public final IntCPointer atomTag() {
            if (NativeLmp.LAMMPS_BIGBIG) throw new UnsupportedOperationException("atomTag for LAMMPS_BIGBIG");
            return new IntCPointer(atomTag0(mFixPtr));
        }
        private native static long atomTag0(long aFixPtr);
        
        public final IntCPointer atomType() {return new IntCPointer(atomType0(mFixPtr));}
        private native static long atomType0(long aFixPtr);
        
        public final DoubleCPointer atomMass() {return new DoubleCPointer(atomMass0(mFixPtr));}
        private native static long atomMass0(long aFixPtr);
        
        public final CPointer atomExtract(String aName) {return new CPointer(atomExtract0(mFixPtr, aName));}
        private native static long atomExtract0(long aFixPtr, String aName);
        
        public final long atomNatoms() {return atomNatoms0(mFixPtr);}
        private native static long atomNatoms0(long aFixPtr);
        
        public final int atomNtypes() {return atomNtypes0(mFixPtr);}
        private native static int atomNtypes0(long aFixPtr);
        
        public final int atomNlocal() {return atomNlocal0(mFixPtr);}
        private native static int atomNlocal0(long aFixPtr);
        
        public final int atomNmax() {return atomNmax0(mFixPtr);}
        private native static int atomNmax0(long aFixPtr);
        
        public final int atomNghost() {return atomNghost0(mFixPtr);}
        private native static int atomNghost0(long aFixPtr);
        
        public final boolean domainTriclinic() {return domainTriclinic0(mFixPtr);}
        private native static boolean domainTriclinic0(long aFixPtr);
        
        public final DoubleCPointer domainXy() {return new DoubleCPointer(domainXy0(mFixPtr));}
        private native static long domainXy0(long aFixPtr);
        
        public final DoubleCPointer domainXz() {return new DoubleCPointer(domainXz0(mFixPtr));}
        private native static long domainXz0(long aFixPtr);
        
        public final DoubleCPointer domainYz() {return new DoubleCPointer(domainYz0(mFixPtr));}
        private native static long domainYz0(long aFixPtr);
        
        public final double domainXprd() {return domainXprd0(mFixPtr);}
        private native static double domainXprd0(long aFixPtr);
        
        public final double domainYprd() {return domainYprd0(mFixPtr);}
        private native static double domainYprd0(long aFixPtr);
        
        public final double domainZprd() {return domainZprd0(mFixPtr);}
        private native static double domainZprd0(long aFixPtr);
        
        public final DoubleCPointer domainH() {return new DoubleCPointer(domainH0(mFixPtr));}
        private native static long domainH0(long aFixPtr);
        
        public final DoubleCPointer domainHInv() {return new DoubleCPointer(domainHInv0(mFixPtr));}
        private native static long domainHInv0(long aFixPtr);
        
        public final DoubleCPointer domainBoxlo() {return new DoubleCPointer(domainBoxlo0(mFixPtr));}
        private native static long domainBoxlo0(long aFixPtr);
        
        public final DoubleCPointer domainBoxhi() {return new DoubleCPointer(domainBoxhi0(mFixPtr));}
        private native static long domainBoxhi0(long aFixPtr);
        
        public final DoubleCPointer domainBoxloLamda() {return new DoubleCPointer(domainBoxloLamda0(mFixPtr));}
        private native static long domainBoxloLamda0(long aFixPtr);
        
        public final DoubleCPointer domainBoxhiLamda() {return new DoubleCPointer(domainBoxhiLamda0(mFixPtr));}
        private native static long domainBoxhiLamda0(long aFixPtr);
        
        public final DoubleCPointer domainSublo() {return new DoubleCPointer(domainSublo0(mFixPtr));}
        private native static long domainSublo0(long aFixPtr);
        
        public final DoubleCPointer domainSubhi() {return new DoubleCPointer(domainSubhi0(mFixPtr));}
        private native static long domainSubhi0(long aFixPtr);
        
        public final DoubleCPointer domainSubloLamda() {return new DoubleCPointer(domainSubloLamda0(mFixPtr));}
        private native static long domainSubloLamda0(long aFixPtr);
        
        public final DoubleCPointer domainSubhiLamda() {return new DoubleCPointer(domainSubhiLamda0(mFixPtr));}
        private native static long domainSubhiLamda0(long aFixPtr);
        
        public final void domainX2lamda(int aN) {domainX2lamda0(mFixPtr, aN);}
        private native static void domainX2lamda0(long aFixPtr, int aN);
        
        public final void domainX2lamda(DoubleCPointer aX, DoubleCPointer rLamda) {domainX2lamda0(mFixPtr, aX.ptr_(), rLamda.ptr_());}
        private native static void domainX2lamda0(long aFixPtr, long aX, long rLamda);
        
        public final void domainLamda2x(int aN) {domainLamda2x0(mFixPtr, aN);}
        private native static void domainLamda2x0(long aFixPtr, int aN);
        
        public final void domainLamda2x(DoubleCPointer aLamda, DoubleCPointer rX) {domainLamda2x0(mFixPtr, aLamda.ptr_(), rX.ptr_());}
        private native static void domainLamda2x0(long aFixPtr, long aLamda, long rX);
        
        public final void domainSetGlobalBox() {domainSetGlobalBox0(mFixPtr);}
        private native static void domainSetGlobalBox0(long aFixPtr);
        
        public final void domainSetLocalBox() {domainSetLocalBox0(mFixPtr);}
        private native static void domainSetLocalBox0(long aFixPtr);
        
        public final int listGnum() throws LmpException {return listGnum0(mFixPtr);}
        private native static int listGnum0(long aFixPtr) throws LmpException;
        
        public final int listInum() throws LmpException {return listInum0(mFixPtr);}
        private native static int listInum0(long aFixPtr) throws LmpException;
        
        public final IntCPointer listIlist() throws LmpException {return new IntCPointer(listIlist0(mFixPtr));}
        private native static long listIlist0(long aFixPtr) throws LmpException;
        
        public final IntCPointer listNumneigh() throws LmpException {return new IntCPointer(listNumneigh0(mFixPtr));}
        private native static long listNumneigh0(long aFixPtr) throws LmpException;
        
        public final NestedIntCPointer listFirstneigh() throws LmpException {return new NestedIntCPointer(listFirstneigh0(mFixPtr));}
        private native static long listFirstneigh0(long aFixPtr) throws LmpException;
        
        public final double forceBoltz() {return forceBoltz0(mFixPtr);}
        private native static double forceBoltz0(long aFixPtr);
        
        public final double forcePairCutforce() {return forcePairCutforce0(mFixPtr);}
        private native static double forcePairCutforce0(long aFixPtr);
        
        public final double dt() {return dt0(mFixPtr);}
        private native static double dt0(long aFixPtr);
        
        public final long ntimestep() {return ntimestep0(mFixPtr);}
        private native static long ntimestep0(long aFixPtr);
        
        public final long firststep() {return firststep0(mFixPtr);}
        private native static long firststep0(long aFixPtr);
        
        public final long laststep() {return laststep0(mFixPtr);}
        private native static long laststep0(long aFixPtr);
        
        public final long beginstep() {return beginstep0(mFixPtr);}
        private native static long beginstep0(long aFixPtr);
        
        public final long endstep() {return endstep0(mFixPtr);}
        private native static long endstep0(long aFixPtr);
        
        public final int commMe() {return commMe0(mFixPtr);}
        private native static int commMe0(long aFixPtr);
        
        public final int commNprocs() {return commNprocs0(mFixPtr);}
        private native static int commNprocs0(long aFixPtr);
        
        public final void commBarrier() throws MPIException {commBarrier0(mFixPtr);}
        private native static void commBarrier0(long aFixPtr) throws MPIException;
        
        public final MPI.Comm commWorld() {return MPI.Comm.of(commWorld0(mFixPtr));}
        private native static long commWorld0(long aFixPtr);
        
        public final double commCutghostuser() {return commCutghostuser0(mFixPtr);}
        private native static double commCutghostuser0(long aFixPtr);
        
        public final void commForwardComm() {commForwardComm0(mFixPtr);}
        private native static void commForwardComm0(long aFixPtr);
        
        public final void commForwardCommThis() {commForwardCommThis0(mFixPtr);}
        private native static void commForwardCommThis0(long aFixPtr);
        
        public final void commReverseComm() {commReverseComm0(mFixPtr);}
        private native static void commReverseComm0(long aFixPtr);
        
        public final void commReverseCommThis() {commReverseCommThis0(mFixPtr);}
        private native static void commReverseCommThis0(long aFixPtr);
        
        public final String unitStyle() {return unitStyle0(mFixPtr);}
        private native static String unitStyle0(long aFixPtr);
    }
}
