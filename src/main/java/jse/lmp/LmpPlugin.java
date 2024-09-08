package jse.lmp;

import jse.cache.MatrixCache;
import jse.clib.*;
import jse.code.OS;
import jse.code.SP;
import jse.code.UT;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jse.code.CS.VERSION;
import static jse.code.Conf.*;
import static jse.code.OS.EXEC;
import static jse.code.OS.JAR_DIR;

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
        /**
         * 自定义构建 lmpplugin 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new HashMap<>();
        
        /**
         * 自定义构建 lmpplugin 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_LMPPLUGIN", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_LMPPLUGIN"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 lmpplugin，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_LMPPLUGIN", jse.code.Conf.USE_MIMALLOC);
        
        /** 重定向 lmpplugin 动态库的路径，用于自定义编译这个库的过程，或者重新实现 lmpplugin 的接口 */
        public static @Nullable String REDIRECT_LMPPLUGIN_LIB = OS.env("JSE_REDIRECT_LMPPLUGIN_LIB");
    }
    
    private final static String LIB_DIR = JAR_DIR+"lmpplugin/" + UT.Code.uniqueID(VERSION, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    private final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_lmp_LmpPlugin_LmpPair.cpp"
        , "jse_lmp_LmpPlugin_LmpPair.h"
        , "jseplugin.cpp"
        , "LmpPair.cpp"
        , "LmpPair.h"
        , "pair_jse.cpp"
        , "pair_jse.h"
        , "lammpsplugin.h"
        , "version.h"
    };
    
    private static String cmakeInitCmd_() {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 这里设置 C/C++ 编译器（如果有）
        if (Conf.CMAKE_CXX_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ Conf.CMAKE_CXX_COMPILER);}
        if (Conf.CMAKE_CXX_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + Conf.CMAKE_CXX_FLAGS +"'");}
        // 初始化使用上一个目录的 CMakeList.txt
        rCommand.add("..");
        return String.join(" ", rCommand);
    }
    private static String cmakeSettingCmd_() throws IOException {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 设置构建输出目录为 lib
        UT.IO.makeDir(LIB_DIR); // 初始化一下这个目录避免意料外的问题
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LIB_DIR +"'");
        // 添加额外的设置参数
        for (Map.Entry<String, String> tEntry : Conf.CMAKE_SETTING.entrySet()) {
        rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
        }
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    
    private static @NotNull String initLmpPlugin_() throws Exception {
        // 检测 cmake，为了简洁并避免问题，现在要求一定要有 cmake 环境
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXEC.system("cmake --version") != 0;
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("LMPPLUGIN BUILD ERROR: No cmake environment.");
        // 从内部资源解压到临时目录
        String tWorkingDir = WORKING_DIR_OF("lmpplugin");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        for (String tName : SRC_NAME) {
            UT.IO.copy(UT.IO.getResource("lmpplugin/src/"+tName), tWorkingDir+tName);
        }
        // 这里对 CMakeLists.txt 特殊处理
        UT.IO.map(UT.IO.getResource("lmpplugin/src/CMakeLists.txt"), tWorkingDir+"CMakeLists.txt", line -> {
            // 替换其中的 jniutil 库路径为设置好的路径
            line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
            // 替换其中的 lammps 库路径为设置好的路径
            line = line.replace("$ENV{JSE_LMP_INCLUDE_DIR}", NativeLmp.NATIVELMP_INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                       .replace("$ENV{JSE_LMP_LIB_PATH}"   , NativeLmp.NATIVELMP_LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
            // 替换其中的 mimalloc 库路径为设置好的路径
            if (Conf.USE_MIMALLOC) {
            line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                       .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
            }
            return line;
        });
        System.out.println("LMPPLUGIN INIT INFO: Building lmpplugin from source code...");
        String tBuildDir = tWorkingDir+"build/";
        UT.IO.makeDir(tBuildDir);
        // 直接通过系统指令来编译 lmpplugin 的库，关闭输出
        EXEC.setNoSTDOutput().setWorkingDir(tBuildDir);
        // 初始化 cmake
        EXEC.system(cmakeInitCmd_());
        // 设置参数
        EXEC.system(cmakeSettingCmd_());
        // 最后进行构造操作
        EXEC.system("cmake --build . --config Release");
        EXEC.setNoSTDOutput(false).setWorkingDir(null);
        // 简单检测一下是否编译成功
        @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "lmpplugin");
        if (tLibName == null) throw new Exception("LMPPLUGIN BUILD ERROR: Build Failed, No lmpplugin lib in '"+LIB_DIR+"'");
        // 完事后移除临时解压得到的源码
        UT.IO.removeDir(tWorkingDir);
        System.out.println("LMPPLUGIN INIT INFO: lmpplugin successfully installed.");
        System.out.println("LMPPLUGIN PATH: " + LIB_DIR+tLibName);
        // 输出安装完成后的库名称
        return tLibName;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        // 依赖 cpointer
        CPointer.InitHelper.init();
        // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
        if (NativeLmp.Conf.USE_MIMALLOC) MiMalloc.InitHelper.init();
        // 依赖 lmpjni，且需要开启了 PLUGIN 插件
        NativeLmp.Conf.CMAKE_SETTING.put("PKG_PLUGIN", "ON");
        NativeLmp.InitHelper.init();
        
        if (Conf.REDIRECT_LMPPLUGIN_LIB == null) {
            @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "lmpplugin");
            // 如果不存在 jni lib 则需要重新通过源码编译
            if (tLibName == null) {
                System.out.println("LMPPLUGIN INIT INFO: lmpplugin libraries not found. Reinstalling...");
                try {tLibName = initLmpPlugin_();} catch (Exception e) {throw new RuntimeException(e);}
            }
            LIB_PATH = LIB_DIR + tLibName;
        } else {
            if (DEBUG) System.out.println("LMPPLUGIN INIT INFO: lmpplugin libraries are redirected to '" + Conf.REDIRECT_LMPPLUGIN_LIB + "'");
            LIB_PATH = Conf.REDIRECT_LMPPLUGIN_LIB;
        }
        // 设置库路径，这里直接使用 System.load
        System.load(UT.IO.toAbsolutePath(LIB_PATH));
    }
    
    
    public static class LmpPair {
        static {
            // 确保 LmpPlugin 已经确实初始化
            LmpPlugin.InitHelper.init();
        }
        
        
        /**
         * 通过反射来获取类，可以是文件路径，也可以是类路径；
         * 这里简单处理，先尝试当文件路径，然后再尝试当类路径
         * @param aClassNameOrPath 类路径的名称或者是 groovy 的脚本文件路径
         * @param aPairPtr lammps jse pair 对应类的指针
         * @return 需要的对象
         * @author liqa
         */
        public static LmpPair of(String aClassNameOrPath, long aPairPtr) throws Exception {
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
            return (LmpPair)InvokerHelper.invokeConstructorOf(tClazz, aPairPtr);
        }
        
        
        protected final long mPairPtr;
        /**
         * @param aPairPtr lammps jse pair 对应类的指针
         * @author liqa
         */
        protected LmpPair(long aPairPtr) {mPairPtr = aPairPtr;}
        
        /**
         * 在这里执行主要的 pair 的计算部分
         * @param aEFlag 是否需要计算能量的 flag，一般只需要直接传递给 lammps 相关接口即可
         * @param aVFlag 是否需要计算位力（virial）的 flag，一般只需要直接传递给 lammps 相关接口即可
         */
        public void compute(boolean aEFlag, boolean aVFlag) {
            double evdwl = 0.0;
            evInit(aEFlag, aVFlag);
            
            NestedDoubleCPointer x = atomX();
            NestedDoubleCPointer f = atomF();
            
            IntCPointer type = atomType();
            int nlocal = atomNlocal(); // 用于判断是否是 ghost 原子
            DoubleCPointer special_lj = forceSpecialLj();
            boolean newton_pair = forceNewtonPair();
            
            int inum = listInum();
            IntCPointer ilist = listIlist();
            IntCPointer numneigh = listNumneigh();
            NestedCPointer firstneigh = listFirstneigh();
            
            // loop over neighbors of atoms
            for (int ii = 0; ii < inum; ++ii) {
                int i = ilist.getAt(ii);
                double xtmp = x.getAt(i, 0);
                double ytmp = x.getAt(i, 1);
                double ztmp = x.getAt(i, 2);
                int itype = type.getAt(i);
                IntCPointer jlist = firstneigh.getAsIntCPointerAt(i);
                int jnum = numneigh.getAt(i);
                
                for (int jj = 0; jj < jnum; ++jj) {
                    int j = jlist.getAt(jj);
                    double factor_lj = special_lj.getAt(sbmask(j));
                    j &= NEIGHMASK;
                    
                    double delx = xtmp - x.getAt(j, 0);
                    double dely = ytmp - x.getAt(j, 1);
                    double delz = ztmp - x.getAt(j, 2);
                    double rsq = delx * delx + dely * dely + delz * delz;
                    int jtype = type.getAt(j);
                    
                    if (rsq < cutsq(itype, jtype)) {
                        double r2inv = 1.0 / rsq;
                        double r6inv = r2inv * r2inv * r2inv;
                        double forcelj = r6inv * (lj1 * r6inv - lj2);
                        double fpair = factor_lj * forcelj * r2inv;
                        
                        f.putAt(i, 0, f.getAt(i, 0) + delx*fpair);
                        f.putAt(i, 1, f.getAt(i, 1) + dely*fpair);
                        f.putAt(i, 2, f.getAt(i, 2) + delz*fpair);
                        if (newton_pair || j < nlocal) {
                            f.putAt(j, 0, f.getAt(j, 0) - delx*fpair);
                            f.putAt(j, 1, f.getAt(j, 1) - dely*fpair);
                            f.putAt(j, 2, f.getAt(j, 2) - delz*fpair);
                        }
                        
                        // ev stuffs
                        if (aEFlag) {
                            evdwl = r6inv * (lj3 * r6inv - lj4);
                            evdwl *= factor_lj;
                        }
                        if (aVFlag) evTally(i, j, nlocal, newton_pair, evdwl, 0.0, fpair, delx, dely, delz);
                    }
                }
            }
            
            // ev stuffs
            if (vflagFdotr()) virialFdotrCompute();
        }
        
        
        /**
         * lammps {@code pair_coeff} 会调用的方法，用于设置参数
         * @param aArgs 参数的字符串数组
         * @author liqa
         */
        public void coeff(String... aArgs) {
            double epsilon = Double.parseDouble(aArgs[0]);
            double sigma = Double.parseDouble(aArgs[1]);
            cutoff = Double.parseDouble(aArgs[2]);
            lj1 = 48.0 * epsilon * MathEX.Fast.powFast(sigma, 12);
            lj2 = 24.0 * epsilon * MathEX.Fast.powFast(sigma,  6);
            lj3 =  4.0 * epsilon * MathEX.Fast.powFast(sigma, 12);
            lj4 =  4.0 * epsilon * MathEX.Fast.powFast(sigma,  6);
        }
        private double cutoff = 0.0;
        private double lj1 = Double.NaN, lj2 = Double.NaN, lj3 = Double.NaN, lj4 = Double.NaN;
        
        /**
         * lammps pair 初始化调用，主要用于在这里设置需要的近邻列表样式
         * @author liqa
         */
        public void initStyle() {
            neighborRequestDefault();
        }
        
        /**
         * lammps pair 初始化某两个种类 {@code i} {@code j}，用于获取这两个种类间的截断半径值
         * @param i 种类 {@code i}，从 {@code 1} 开始
         * @param j 种类 {@code j}，从 {@code 1} 开始
         * @return 种类 {@code i} {@code j} 之间的截断半径
         * @author liqa
         */
        public double initOne(int i, int j) {
            return cutoff;
        }
        
        
        /// lammps pair 提供的接口
        protected final void neighborRequestDefault() {neighborRequestDefault_(mPairPtr);}
        private native static void neighborRequestDefault_(long aPairPtr);
        
        protected final void neighborRequestFull() {neighborRequestFull_(mPairPtr);}
        private native static void neighborRequestFull_(long aPairPtr);
        
        protected final void evInit(boolean eflag, boolean vflag) {evInit_(mPairPtr, eflag, vflag);}
        private native static void evInit_(long aPairPtr, boolean eflag, boolean vflag);
        
        protected final NestedDoubleCPointer atomX() {return new NestedDoubleCPointer(atomX_(mPairPtr));}
        private native static long atomX_(long aPairPtr);
        
        protected final NestedDoubleCPointer atomF() {return new NestedDoubleCPointer(atomF_(mPairPtr));}
        private native static long atomF_(long aPairPtr);
        
        protected final IntCPointer atomType() {return new IntCPointer(atomType_(mPairPtr));}
        private native static long atomType_(long aPairPtr);
        
        protected final int atomNlocal() {return atomNlocal_(mPairPtr);}
        private native static int atomNlocal_(long aPairPtr);
        
        protected final DoubleCPointer forceSpecialLj() {return new DoubleCPointer(forceSpecialLj_(mPairPtr));}
        private native static long forceSpecialLj_(long aPairPtr);
        
        protected final boolean forceNewtonPair() {return forceNewtonPair_(mPairPtr);}
        private native static boolean forceNewtonPair_(long aPairPtr);
        
        protected final int listInum() {return listInum_(mPairPtr);}
        private native static int listInum_(long aPairPtr);
        
        protected final IntCPointer listIlist() {return new IntCPointer(listIlist_(mPairPtr));}
        private native static long listIlist_(long aPairPtr);
        
        protected final IntCPointer listNumneigh() {return new IntCPointer(listNumneigh_(mPairPtr));}
        private native static long listNumneigh_(long aPairPtr);
        
        protected final NestedCPointer listFirstneigh() {return new NestedCPointer(listFirstneigh_(mPairPtr));}
        private native static long listFirstneigh_(long aPairPtr);
        
        protected static int sbmask(int j) {return (j >> SBBITS) & 3;}
        
        protected final double cutsq(int i, int j) {return cutsq_(mPairPtr, i, j);}
        private native static double cutsq_(long aPairPtr, int i, int j);
        
        @SuppressWarnings("SameParameterValue")
        protected final void evTally(int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fpair, double delx, double dely, double delz) {evTally_(mPairPtr, i, j, nlocal, newtonPair, evdwl, ecoul, fpair, delx, dely, delz);}
        private native static void evTally_(long aPairPtr, int i, int j, int nlocal, boolean newtonPair, double evdwl, double ecoul, double fpair, double delx, double dely, double delz);
        
        protected final boolean vflagFdotr() {return vflagFdotr_(mPairPtr);}
        private native static boolean vflagFdotr_(long aPairPtr);
        
        protected final void virialFdotrCompute() {virialFdotrCompute_(mPairPtr);}
        private native static void virialFdotrCompute_(long aPairPtr);
    }
}
