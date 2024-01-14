package jtool.lmp;

import jtool.atom.IAtom;
import jtool.atom.IAtomData;
import jtool.atom.IXYZ;
import jtool.code.UT;
import jtool.io.IInFile;
import jtool.math.matrix.ColumnMatrix;
import jtool.math.matrix.DoubleArrayMatrix;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import jtool.parallel.DoubleArrayCache;
import jtool.parallel.IAutoShutdown;
import jtool.parallel.MPI;
import jtool.parallel.MatrixCache;
import jtool.vasp.IVaspCommonData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jtool.code.CS.Exec.EXE;
import static jtool.code.CS.Exec.JAR_DIR;
import static jtool.code.CS.*;

/**
 * 基于 jni 的调用本地原生 lammps 的类，
 * 使用类似 python 调用 lammps 的结构，
 * 使用 {@link MPI} 实现并行。
 * <p>
 * 由于 lammps 的特性，此类线程不安全，并且要求所有方法都由相同的线程调用
 * <p>
 * References:
 * <a href="https://docs.lammps.org/Python_module.html">
 * The lammps Python module </a>,
 * <a href="https://docs.lammps.org/Library.html/">
 * LAMMPS Library Interfaces </a>,
 * @author liqa
 */
public class NativeLmp implements IAutoShutdown {
    
    public final static class Error extends Exception {
        public Error(String aMessage) {
            super(aMessage);
        }
    }
    
    /**
     * 使用这个子类来进行一些配置参数的设置，
     * 使用子类的成员不会触发 {@link NativeLmp} 的静态初始化
     * @author liqa
     */
    public static class Conf {
        private Conf() {}
        /**
         * 设置 lammps 的 build 目录，
         * 应该包含一个 includes 目录其中有所有的头文件，
         * 然后包含一个 lib 目录，其中有所有的二进制库文件
         */
        public static String LMP_HOME = null;
        
        /**
         * 指定需要下载的 lammps 的 tag，
         * 只在下载时有用；
         * 这里简单实现，不使用 git 来自动识别最新稳定版本
         */
        public static String LMP_TAG = "stable_2Aug2023_update2";
        
        /**
         * 包含所有的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new HashMap<>();
        
        /**
         * 是否在检测到库文件时依旧重新编译 lammps，
         * 在需要修改 lammps 包时很有用
         */
        public static boolean REBUILD = false;
        
        /**
         * 在编译 lammps 之前是否进行 clean 操作，
         * 这会大大增加二次编译的时间
         */
        public static boolean CLEAN = false;
        
        /**
         * 是否是旧版本的 lammps，具体来说大致为 18Sep2020 之前版本的 lammps，
         * 开启后会使用更老的 api，兼容性会更高
         */
        public static boolean IS_OLD = false;
        
        /**
         * lammps 是否有 exception 相关接口，
         * 对于新版的 lammps 总是存在，但对于旧版可能不会存在，
         * 关闭后可以保证编译通过
         */
        public static boolean HAS_EXCEPTIONS = true;
        
        /**
         * lammps 的 lammps_has_error 接口是否有 NULL 支持，
         * 对于较旧的版本并不支持这个
         */
        public static boolean EXCEPTIONS_NULL_SUPPORT = true;
    }
    
    private final static String LMPLIB_DIR = JAR_DIR+"lmp/";
    private final static String LMPLIB_PATH = LMPLIB_DIR + (IS_WINDOWS ? "lmp.dll" : (IS_MAC ? "lmp.jnilib" : "lmp.so"));
    private final static String[] LMPSRC_NAME = {
          "jtool_lmp_NativeLmp.c"
        , "jtool_lmp_NativeLmp.h"
    };
    private final static String NATIVE_DIR_NAME = "native", BUILD_DIR_NAME = IS_WINDOWS ? "build-win" : (IS_MAC ? "build-mac" : "build");
    private final static String NATIVE_LMPLIB_NAME = IS_WINDOWS ? "liblammps.dll" : (IS_MAC ? "liblammps.dylib" : "liblammps.so");
    
    private static String initCmakeSettingCmdNativeLmp_(String aNativeLmpBuildDir) {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cd"); rCommand.add("\""+aNativeLmpBuildDir+"\""); rCommand.add(";");
        rCommand.add("cmake");
        // 设置输出动态链接库
        rCommand.add("-D"); rCommand.add("BUILD_SHARED_LIBS=ON");
        // 设置抛出错误
        rCommand.add("-D"); rCommand.add("LAMMPS_EXCEPTIONS:BOOL=ON");
        // 设置编译模式 Release
        rCommand.add("-D"); rCommand.add("CMAKE_BUILD_TYPE=Release");
        // 设置构建输出目录为 lib
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH=\""+aNativeLmpBuildDir+"lib\"");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH=\""+aNativeLmpBuildDir+"lib\"");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH=\""+aNativeLmpBuildDir+"lib\"");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH=\""+aNativeLmpBuildDir+"lib\"");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH=\""+aNativeLmpBuildDir+"lib\"");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH=\""+aNativeLmpBuildDir+"lib\"");
        // 添加额外的设置参数
        for (Map.Entry<String, String> tEntry : Conf.CMAKE_SETTING.entrySet()) {
            rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
        }
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    private static String initCmakeSettingCmdLmpJni_(String aLmpJniBuildDir) {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cd"); rCommand.add("\""+aLmpJniBuildDir+"\""); rCommand.add(";");
        rCommand.add("cmake");
        rCommand.add("-D"); rCommand.add("LAMMPS_IS_OLD="                 +(Conf.IS_OLD                 ?"ON":"OFF"));
        rCommand.add("-D"); rCommand.add("LAMMPS_HAS_EXCEPTIONS="         +(Conf.HAS_EXCEPTIONS         ?"ON":"OFF"));
        rCommand.add("-D"); rCommand.add("LAMMPS_EXCEPTIONS_NULL_SUPPORT="+(Conf.EXCEPTIONS_NULL_SUPPORT?"ON":"OFF"));
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    
    private static void initLmp_() throws Exception {
        // 检测 cmake，这里要求一定要有 cmake 环境
        EXE.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXE.system("cmake --version") != 0;
        EXE.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("NATIVE_LMP BUILD ERROR: No camke environment.");
        String tWorkingDir = WORKING_DIR.replaceAll("%n", "nativelmp");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        // LMP_HOME 不合法，需要重新指定
        if (!UT.IO.isDir(Conf.LMP_HOME)) {
            String tNativeLmpDir = LMPLIB_DIR+NATIVE_DIR_NAME+"/";
            // 如果 lammps 源码文件夹不存在则需要重新从 github 上下载
            if (!UT.IO.isDir(tNativeLmpDir)) {
                String tNativeLmpZipPath = tWorkingDir+"lammps-"+Conf.LMP_TAG+".zip";
                System.out.printf("NATIVE_LMP INIT INFO: No native lammps build dir in %s, downloading the source code...\n", Conf.LMP_HOME);
                UT.IO.copy(new URL(String.format("https://github.com/lammps/lammps/archive/refs/tags/%s.zip", Conf.LMP_TAG)), tNativeLmpZipPath);
                System.out.println("NATIVE_LMP INIT INFO: Lammps source code downloading finished.");
                // 解压 lammps 到临时目录，如果已经存在则直接清空此目录
                String tNativeLmpTempDir = tWorkingDir+"temp/";
                UT.IO.removeDir(tNativeLmpTempDir);
                UT.IO.zip2dir(tNativeLmpZipPath, tNativeLmpTempDir);
                String tNativeLmpTempDir2 = null;
                for (String tName : UT.IO.list(tNativeLmpTempDir)) {
                    if (tName!=null && !tName.isEmpty() && !tName.equals(".") && !tName.equals("..")) {
                        String tNativeLmpTempDir1 = tNativeLmpTempDir + tName + "/";
                        if (UT.IO.isDir(tNativeLmpTempDir1)) {
                            tNativeLmpTempDir2 = tNativeLmpTempDir1;
                            break;
                        }
                    }
                }
                if (tNativeLmpTempDir2 == null) throw new Exception("NATIVE_LMP INIT ERROR: No lammps dir in "+tNativeLmpTempDir);
                // 移动到需要的目录
                UT.IO.move(tNativeLmpTempDir2, tNativeLmpDir);
            }
            // 这里创建 build 目录即可
            String tNativeLmpBuildDir = tNativeLmpDir+BUILD_DIR_NAME+"/";
            UT.IO.makeDir(tNativeLmpBuildDir);
            // 设置新的 LMP_HOME
            Conf.LMP_HOME = UT.IO.toAbsolutePath(tNativeLmpBuildDir);
            if (!Conf.LMP_HOME.isEmpty() && !Conf.LMP_HOME.endsWith("/") && !Conf.LMP_HOME.endsWith("\\")) Conf.LMP_HOME += "/";
        }
        // 检测是否有需要的 lib，如果没有（或者强制要求重新编译），则需要进行编译
        String tNativeLmpLib = Conf.LMP_HOME+"lib/"+NATIVE_LMPLIB_NAME;
        if (Conf.REBUILD || !UT.IO.isFile(tNativeLmpLib)) {
            System.out.println("NATIVE_LMP INIT INFO: Building lammps from source code...");
            // 编译 lammps，直接通过系统指令来编译，关闭输出
            EXE.setNoSTDOutput();
            // 初始化 cmake
            EXE.system(String.format("cd \"%s\"; cmake ../cmake", Conf.LMP_HOME));
            // 设置参数
            EXE.system(initCmakeSettingCmdNativeLmp_(Conf.LMP_HOME));
            // 如果设置 CLEAN 则进行 clean 操作
            if (Conf.CLEAN) EXE.system(String.format("cd \"%s\"; cmake --build . --target clean", Conf.LMP_HOME));
            // 最后进行构造操作
            EXE.system(String.format("cd \"%s\"; cmake --build . --config Release", Conf.LMP_HOME));
            EXE.setNoSTDOutput(false);
        }
        // 如果依旧没有 tNativeLmpLib 则构建失败
        if (!UT.IO.isFile(tNativeLmpLib)) throw new Exception("NATIVE_LMP BUILD ERROR: Lammps build Failed, No liblammps in "+Conf.LMP_HOME+"lib/");
        // 注意只有 jni 的 lib 没有检测到才编译 jni 的部分
        if (!UT.IO.isFile(LMPLIB_PATH)) {
            // 从内部资源解压到临时目录
            String tSrcDir = tWorkingDir+"src/";
            for (String tName : LMPSRC_NAME) {
                UT.IO.copy(UT.IO.getResource("lmp/src/"+tName), tSrcDir+tName);
            }
            // 这里对 CMakeLists.txt 特殊处理，替换其中的 lammps 库路径为设置好的路径
            try (BufferedReader tReader = UT.IO.toReader(UT.IO.getResource("lmp/src/CMakeLists.txt")); UT.IO.IWriteln tWriter = UT.IO.toWriteln(tSrcDir+"CMakeLists.txt")) {
                String tLine;
                while ((tLine = tReader.readLine()) != null) {
                    tLine = tLine.replace("$ENV{LAMMPS_HOME}", Conf.LMP_HOME.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                    tWriter.writeln(tLine);
                }
            }
            System.out.println("NATIVE_LMP INIT INFO: Building lmpjni from source code...");
            String tBuildDir = tSrcDir+"build/";
            UT.IO.makeDir(tBuildDir);
            // 直接通过系统指令来编译 lmpjni 的库，关闭输出
            EXE.setNoSTDOutput();
            // 初始化 cmake
            EXE.system(String.format("cd \"%s\"; cmake ..", tBuildDir));
            // 设置参数
            EXE.system(initCmakeSettingCmdLmpJni_(tBuildDir));
            // 最后进行构造操作
            EXE.system(String.format("cd \"%s\"; cmake --build . --config Release", tBuildDir));
            EXE.setNoSTDOutput(false);
            // 获取 build 目录下的 lib 文件
            String tLibDir = tBuildDir+"lib/";
            if (!UT.IO.isDir(tLibDir)) throw new Exception("NATIVE_LMP BUILD ERROR: lmpjni build Failed, No lmpjni lib in "+tBuildDir);
            String[] tList = UT.IO.list(tLibDir);
            String tLibPath = null;
            for (String tName : tList) if (tName.contains("lmp") && (tName.endsWith(".dll") || tName.endsWith(".so") || tName.endsWith(".jnilib") || tName.endsWith(".dylib"))) {
                tLibPath = tName;
            }
            if (tLibPath == null) throw new Exception("NATIVE_LMP BUILD ERROR: lmpjni build Failed, No lmpjni lib in "+tLibDir);
            tLibPath = tLibDir+tLibPath;
            // 将 build 的输出拷贝到 lib 目录下
            UT.IO.copy(tLibPath, LMPLIB_PATH);
        }
        // 完事后移除临时解压得到的源码（以及可能存在的临时下载的 lammps 源码压缩包）
        UT.IO.removeDir(tWorkingDir);
        System.out.println("NATIVE_LMP INIT INFO: lammps libraries successfully installed.");
    }
    
    static {
        // 先规范化 LMP_HOME 的格式
        if (Conf.LMP_HOME == null) {
            Conf.LMP_HOME = UT.IO.toAbsolutePath(LMPLIB_DIR+NATIVE_DIR_NAME+"/"+BUILD_DIR_NAME+"/");
        } else {
            Conf.LMP_HOME = UT.IO.toAbsolutePath(Conf.LMP_HOME);
        }
        if (!Conf.LMP_HOME.isEmpty() && !Conf.LMP_HOME.endsWith("/") && !Conf.LMP_HOME.endsWith("\\")) Conf.LMP_HOME += "/";
        // 如果不存在 jni lib 则需要重新通过源码编译
        if (Conf.REBUILD || !UT.IO.isFile(LMPLIB_PATH) || !UT.IO.isFile(Conf.LMP_HOME+"lib/"+NATIVE_LMPLIB_NAME)) {
            System.out.println("NATIVE_LMP INIT INFO: lammps libraries not found. Reinstalling...");
            try {initLmp_();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        // 设置库路径（注意 LMP_HOME 在这期间会改变，因此需要重新获取）
        System.load(UT.IO.toAbsolutePath(Conf.LMP_HOME+"lib/"+NATIVE_LMPLIB_NAME));
        System.load(UT.IO.toAbsolutePath(LMPLIB_PATH));
    }
    
    
    private final static String EXECUTABLE_NAME = "liblammps";
    private final static String[] DEFAULT_ARGS = {EXECUTABLE_NAME, "-log", "none"};
    private final long mLmpPtr;
    private final @Nullable MPI.Comm mComm;
    private final long mInitTheadID; // lammps 需要保证初始化时的线程和调用时的是相同的
    private boolean mDead = false;
    /**
     * Create an instance of the LAMMPS Java class.
     * <p>
     * This is a Java wrapper class that exposes the LAMMPS C-library interface to Java.
     * It either requires that LAMMPS has been compiled as shared library which is then
     * dynamically loaded via the jni, for example through the java {@code <init>} method.
     * When the class is instantiated it calls the {@code lammps_open()} function of the LAMMPS
     * C-library interface, which in turn will create an instance of the LAMMPS C++ class.
     * The handle to this C++ class is stored internally and automatically passed to the
     * calls to the C library interface.
     *
     * @param aArgs  array of command line arguments to be passed to the {@code lammps_open()}
     *               (or {@code lammps_open_no_mpi()} when no mpi or no init mpi) function.
     *               The executable name is automatically added.
     *
     * @param aComm MPI communicator as provided by {@link MPI} (or {@link MPI.Native}).
     *              null (or 0) means use {@link MPI.Comm#WORLD} implicitly.
     *
     * @author liqa
     */
    public NativeLmp(String[] aArgs, long aComm) throws Error {
        String[] tArgs = aArgs==null ? DEFAULT_ARGS : new String[aArgs.length+1];
        tArgs[0] = EXECUTABLE_NAME;
        if (aArgs != null) System.arraycopy(aArgs, 0, tArgs, 1, aArgs.length);
        mLmpPtr = aComm==0 ? lammpsOpen_(tArgs) : lammpsOpen_(tArgs, aComm);
        mComm = aComm==0 ? null : MPI.Comm.of(aComm);
        mInitTheadID = Thread.currentThread().getId();
    }
    public NativeLmp(String[] aArgs, MPI.Comm aComm) throws Error {this(aArgs, aComm==null ? 0 : aComm.ptr_());}
    public NativeLmp(String[] aArgs) throws Error {this(aArgs, 0);}
    public NativeLmp() throws Error {this(null);}
    private native static long lammpsOpen_(String[] aArgs, long aComm) throws Error;
    private native static long lammpsOpen_(String[] aArgs) throws Error;
    
    public boolean threadValid() {
        return Thread.currentThread().getId() == mInitTheadID;
    }
    public void checkThread() throws Error {
        long tCurrentThreadID = Thread.currentThread().getId();
        if (tCurrentThreadID != mInitTheadID) throw new Error("Thread of NativeLmp MUST be SAME: "+tCurrentThreadID+" vs "+mInitTheadID);
    }
    
    /**
     * Return a numerical representation of the LAMMPS version in use.
     * <p>
     * This is a wrapper around the {@code lammps_version()} function of the C-library interface.
     * @return version number
     */
    public int version() throws Error {
        checkThread();
        return lammpsVersion_(mLmpPtr);
    }
    private native static int lammpsVersion_(long aLmpPtr) throws Error;
    
    /**
     * @return the {@link MPI.Comm} of this NativeLmp
     */
    public @NotNull MPI.Comm comm() {
        return mComm==null ? MPI.Comm.WORLD : mComm;
    }
    
    /**
     * Read LAMMPS commands from a file.
     * <p>
     * This is a wrapper around the {@code lammps_file()} function of the C-library interface.
     * It will open the file with the name/path file and process the LAMMPS commands line by line
     * until the end.
     * The function will return when the end of the file is reached.
     * @param aPath Name of the file/path with LAMMPS commands
     */
    public void file(String aPath) throws Error {
        checkThread();
        lammpsFile_(mLmpPtr, aPath);
    }
    private native static void lammpsFile_(long aLmpPtr, String aPath) throws Error;
    
    /**
     * 提供一个更加易用的直接使用 {@link IInFile}
     * 作为输入的 file 方法，底层会使用 {@link #commands}
     * 来执行多行命令
     * @param aLmpIn 需要读取的 lammps in 文件
     * @author liqa
     */
    public void file(IInFile aLmpIn) throws IOException, Error {
        checkThread();
        commands(aLmpIn.toLines().toArray(ZL_STR));
    }
    
    /**
     * Process a single LAMMPS input command from a string.
     * <p>
     * This is a wrapper around the {@code lammps_command()} function of the C-library interface.
     * @param aCmd a single lammps command
     */
    public void command(String aCmd) throws Error {
        checkThread();
        lammpsCommand_(mLmpPtr, aCmd);
    }
    private native static void lammpsCommand_(long aLmpPtr, String aCmd) throws Error;
    
    /**
     * Process multiple LAMMPS input commands from a list of strings.
     * <p>
     * This is a wrapper around the {@code lammps_commands_list()} function of the C-library interface.
     * @param aCmds a list of lammps commands
     */
    public void commands(String[] aCmds) throws Error {
        checkThread();
        lammpsCommandsList_(mLmpPtr, aCmds);
    }
    private native static void lammpsCommandsList_(long aLmpPtr, String[] aCmds) throws Error;
    
    /**
     * Process a block of LAMMPS input commands from a string.
     * <p>
     * This is a wrapper around the {@code lammps_commands_string()} function of the C-library interface.
     * @param aMultiCmd text block of lammps commands
     */
    public void commands(String aMultiCmd) throws Error {
        checkThread();
        lammpsCommandsString_(mLmpPtr, aMultiCmd);
    }
    private native static void lammpsCommandsString_(long aLmpPtr, String aMultiCmd) throws Error;
    
    /**
     * Get the total number of atoms in the LAMMPS instance.
     * <p>
     * This is a wrapper around the {@code lammps_get_natoms()} function of the C-library interface.
     * @return number of atoms
     */
    public int atomNum() throws Error {
        checkThread();
        return (int)lammpsGetNatoms_(mLmpPtr);
    }
    @VisibleForTesting public int natoms() throws Error {return atomNum();}
    private native static double lammpsGetNatoms_(long aLmpPtr) throws Error;
    
    /**
     * Get the total number of atoms types in the LAMMPS instance.
     * @return number of atom types
     */
    public int atomTypeNum() throws Error {return settingOf("ntypes");}
    @VisibleForTesting public int ntype() throws Error {return atomTypeNum();}
    
    /**
     * Get the local number of atoms in the LAMMPS instance.
     * @return number of “owned” atoms of the current MPI rank.
     */
    public int localAtomNum() throws Error {return settingOf("nlocal");}
    @VisibleForTesting public int nlocal() throws Error {return localAtomNum();}
    
    /**
     * Extract simulation box parameters
     * <p>
     * This is a wrapper around the lammps_extract_box() function of the C-library interface.
     * Unlike in the C function, the result is returned a {@link Box} object.
     * @return a {@link Box} object.
     */
    public Box box() throws Error {
        checkThread();
        double[] rBox = DoubleArrayCache.getArray(15);
        lammpsExtractBox_(mLmpPtr, rBox);
        Box tOut;
        if (settingOf("triclinic")==1) {
            tOut = new BoxPrism(rBox[0], rBox[3], rBox[1], rBox[4], rBox[2], rBox[5], rBox[6], rBox[8], rBox[7]);
        } else {
            tOut = new Box(rBox[0], rBox[3], rBox[1], rBox[4], rBox[2], rBox[5]);
        }
        DoubleArrayCache.returnArray(rBox);
        return tOut;
    }
    /**
     * {@code [xlo, ylo, zlo, xhi, yhi, zhi, xy, yz, xz, px, py, pz, bx, by, bz]}
     * <p>
     * {@code [0  , 1  , 2  , 3  , 4  , 5  , 6 , 7 , 8 , 9 , 10, 11, 12, 13, 14]}
     */
    private native static void lammpsExtractBox_(long aLmpPtr, double[] rBox) throws Error;
    
    /**
     * Reset simulation box parameters
     * <p>
     * This is a wrapper around the {@code lammps_reset_box()} function of the C-library interface,
     * but in {@link BoxPrism} order.
     */
    public void resetBox(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi, double aXY, double aXZ, double aYZ) throws Error {
        checkThread();
        lammpsResetBox_(mLmpPtr, aXlo, aYlo, aZlo, aXhi, aYhi, aZhi, aXY, aYZ, aXZ);
    }
    public void resetBox(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi) throws Error {
        resetBox(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi, 0.0, 0.0, 0.0);
    }
    public void resetBox(double aXhi, double aYhi, double aZhi) throws Error {
        resetBox(0.0, aXhi, 0.0, aYhi, 0.0, aZhi);
    }
    public void resetBox(BoxPrism aBoxPrism) throws Error {
        resetBox(aBoxPrism.xlo(), aBoxPrism.xhi(), aBoxPrism.ylo(), aBoxPrism.yhi(), aBoxPrism.zlo(), aBoxPrism.zhi(), aBoxPrism.xy(), aBoxPrism.xz(), aBoxPrism.yz());
    }
    public void resetBox(Box aBox) throws Error {
        if (aBox.type() == Box.Type.NORMAL) {
            resetBox(aBox.xlo(), aBox.xhi(), aBox.ylo(), aBox.yhi(), aBox.zlo(), aBox.zhi());
        } else {
            resetBox((BoxPrism)aBox);
        }
    }
    public void resetBox(IXYZ aBoxLo, IXYZ aBoxHi) throws Error {
        resetBox(aBoxLo.x(), aBoxHi.x(), aBoxLo.y(), aBoxHi.y(), aBoxLo.z(), aBoxHi.z());
    }
    public void resetBox(IXYZ aBox) throws Error {
        resetBox(aBox.x(), aBox.y(), aBox.z());
    }
    private native static void lammpsResetBox_(long aLmpPtr, double aXlo, double aYlo, double aZlo, double aXhi, double aYhi, double aZhi, double aXY, double aYZ, double aXZ) throws Error;
    
    /**
     * Get current value of a thermo keyword
     * <p>
     * This is a wrapper around the {@code lammps_get_thermo()} function of the C-library interface.
     * @param aName name of thermo keyword
     * @return value of thermo keyword
     */
    public double thermoOf(String aName) throws Error {
        checkThread();
        return lammpsGetThermo_(mLmpPtr, aName);
    }
    private native static double lammpsGetThermo_(long aLmpPtr, String aName) throws Error;
    
    /**
     * Query LAMMPS about global settings that can be expressed as an integer.
     * <p>
     * This is a wrapper around the {@code lammps_extract_setting()} function of the C-library interface.
     * <a href="https://docs.lammps.org/Library_properties.html#_CPPv422lammps_extract_settingPvPKc">
     * Its documentation </a> includes a list of the supported keywords.
     * @param aName name of the setting
     * @return value of the setting
     */
    public int settingOf(String aName) throws Error {
        checkThread();
        return lammpsExtractSetting_(mLmpPtr, aName);
    }
    private native static int lammpsExtractSetting_(long aLmpPtr, String aName) throws Error;
    
    /**
     * Gather the named per-atom, per-atom fix, per-atom compute,
     * or fix property/atom-based entities from all processes, unordered.
     * <p>
     * This is a wrapper around the {@code lammps_gather_concat()} function of the C-library interface.
     * <a href="https://docs.lammps.org/Classes_atom.html#_CPPv4N9LAMMPS_NS4Atom7extractEPKc">
     * Its documentation </a> includes a list of the supported keywords and their data types.
     * This function will try to auto-detect the data type by asking the library.
     * This function returns null if either the keyword is not recognized.
     * @param aName name of the property
     * @return RowMatrix of requested data
     * @see <a href="https://docs.lammps.org/Library_scatter.html#_CPPv420lammps_gather_concatPvPKciiPv">
     * lammps_gather_concat() </a>
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    public RowMatrix atomDataOf(String aName) throws Error {
        switch(aName) {
        case "mass":        {return localAtomDataOf(aName, 1, atomTypeNum()+1, 1);}
        case "id":          {return fullAtomDataOf(aName, false, 1);}
        case "type":        {return fullAtomDataOf(aName, false, 1);}
        case "mask":        {return fullAtomDataOf(aName, false, 1);}
        case "image":       {return fullAtomDataOf(aName, false, 1);}
        case "x":           {return fullAtomDataOf(aName, true , 3);}
        case "v":           {return fullAtomDataOf(aName, true , 3);}
        case "f":           {return fullAtomDataOf(aName, true , 3);}
        case "molecule":    {return fullAtomDataOf(aName, false, 1);}
        case "q":           {return fullAtomDataOf(aName, true , 1);}
        case "mu":          {return fullAtomDataOf(aName, true , 3);}
        case "omega":       {return fullAtomDataOf(aName, true , 3);}
        case "angmom":      {return fullAtomDataOf(aName, true , 3);}
        case "torque":      {return fullAtomDataOf(aName, true , 3);}
        case "radius":      {return fullAtomDataOf(aName, true , 1);}
        case "rmass":       {return fullAtomDataOf(aName, true , 1);}
        case "ellipsoid":   {return fullAtomDataOf(aName, false, 1);}
        case "line":        {return fullAtomDataOf(aName, false, 1);}
        case "tri":         {return fullAtomDataOf(aName, false, 1);}
        case "body":        {return fullAtomDataOf(aName, false, 1);}
        case "quat":        {return fullAtomDataOf(aName, true , 4);}
        case "temperature": {return fullAtomDataOf(aName, true , 1);}
        case "heatflow":    {return fullAtomDataOf(aName, true , 1);}
        default: {
            if (aName.startsWith("i_")) {
                return fullAtomDataOf(aName, false, 1);
            } else
            if (aName.startsWith("d_")) {
                return fullAtomDataOf(aName, true , 1);
            } else {
                throw new IllegalArgumentException("Unexpected name: "+aName+", use fullAtomDataOf(aName, aIsDouble, aRowNum, aColNum) to gather this atom data.");
            }
        }}
    }
    
    /**
     * Gather the named per-atom, per-atom fix, per-atom compute,
     * or fix property/atom-based entities from all processes, unordered.
     * <p>
     * This is a wrapper around the {@code lammps_extract_atom()} function of the C-library interface.
     * @param aName name of the property
     * @param aIsDouble false for int, true for double
     * @param aColNum column number of Matrix of requested data
     * @return RowMatrix of requested data, row number is always atomNum.
     */
    public RowMatrix fullAtomDataOf(String aName, boolean aIsDouble, int aColNum) throws Error {
        checkThread();
        RowMatrix rData = MatrixCache.getMatRow(atomNum(), aColNum);
        lammpsGatherConcat_(mLmpPtr, aName, aIsDouble, aColNum, rData.internalData());
        return rData;
    }
    /**
     * 获取此进程的原子数据而不进行收集操作，
     * 似乎 mass 需要使用此方法才能合法获取；
     * 这里支持 BigBig 包因此需要 int 类型的 aDataType
     * <p>
     * This is a wrapper around the {@code lammps_extract_atom()} function of the C-library interface.
     * @param aName name of the property
     * @param aDataType 0 for int, 1 for double, 2 for int64_t when LAMMPS_BIGBIG is defined, 3 for int64_t anyway
     * @param aColNum column number of Matrix of requested data
     * @param aRowNum row number of Matrix of requested data
     * @return RowMatrix of requested data
     */
    public RowMatrix localAtomDataOf(String aName, int aDataType, int aRowNum, int aColNum) throws Error {
        checkThread();
        RowMatrix rData = MatrixCache.getMatRow(aRowNum, aColNum);
        lammpsExtractAtom_(mLmpPtr, aName, aDataType, aRowNum, aColNum, rData.internalData());
        return rData;
    }
    private native static void lammpsGatherConcat_(long aLmpPtr, String aName, boolean aIsDouble, int aCount, double[] rData) throws Error;
    private native static void lammpsExtractAtom_(long aLmpPtr, String aName, int aDataType, int aAtomNum, int aCount, double[] rData) throws Error;
    
    /**
     * Scatter the named per-atom, per-atom fix, per-atom compute,
     * or fix property/atom-based entity in data to all processes.
     * <p>
     * This is a wrapper around the {@code lammps_scatter()} function of the C-library interface.
     * This subroutine takes data stored in a one-dimensional array supplied by the user and
     * scatters them to all atoms on all processes. The data must be ordered by atom ID,
     * with the requirement that the IDs be consecutive. Use lammps_scatter_subset() to
     * scatter data for some (or all) atoms, unordered.
     * @param aName name of the property
     * @param aData Matrix of data to set
     * @see <a href="https://docs.lammps.org/Library_scatter.html#_CPPv414lammps_scatterPvPKciiPv">
     * lammps_scatter() </a>
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    public void setAtomDataOf(String aName, IMatrix aData) throws Error {
        switch(aName) {
        case "id":          {setAtomDataOf(aName, aData, false); return;}
        case "type":        {setAtomDataOf(aName, aData, false); return;}
        case "mask":        {setAtomDataOf(aName, aData, false); return;}
        case "image":       {setAtomDataOf(aName, aData, false); return;}
        case "x":           {setAtomDataOf(aName, aData, true ); return;}
        case "v":           {setAtomDataOf(aName, aData, true ); return;}
        case "f":           {setAtomDataOf(aName, aData, true ); return;}
        case "molecule":    {setAtomDataOf(aName, aData, false); return;}
        case "q":           {setAtomDataOf(aName, aData, true ); return;}
        case "mu":          {setAtomDataOf(aName, aData, true ); return;}
        case "omega":       {setAtomDataOf(aName, aData, true ); return;}
        case "angmom":      {setAtomDataOf(aName, aData, true ); return;}
        case "torque":      {setAtomDataOf(aName, aData, true ); return;}
        case "radius":      {setAtomDataOf(aName, aData, true ); return;}
        case "rmass":       {setAtomDataOf(aName, aData, true ); return;}
        case "ellipsoid":   {setAtomDataOf(aName, aData, false); return;}
        case "line":        {setAtomDataOf(aName, aData, false); return;}
        case "tri":         {setAtomDataOf(aName, aData, false); return;}
        case "body":        {setAtomDataOf(aName, aData, false); return;}
        case "quat":        {setAtomDataOf(aName, aData, true ); return;}
        case "temperature": {setAtomDataOf(aName, aData, true ); return;}
        case "heatflow":    {setAtomDataOf(aName, aData, true ); return;}
        default: {
            if (aName.startsWith("i_")) {
                setAtomDataOf(aName, aData, false);
            } else
            if (aName.startsWith("d_")) {
                setAtomDataOf(aName, aData, true );
            } else {
                throw new IllegalArgumentException("Unexpected name: "+aName+", use setAtomDataOf(aName, aData, aIsDouble) to scatter this atom data.");
            }
        }}
    }
    public void setAtomDataOf(String aName, IMatrix aData, boolean aIsDouble) throws Error {
        checkThread();
        if ((aData instanceof RowMatrix) || ((aData instanceof ColumnMatrix) && aData.columnNumber()==1)) {
            lammpsScatter_(mLmpPtr, aName, aIsDouble, aData.rowNumber(), aData.columnNumber(), ((DoubleArrayMatrix)aData).internalData());
        } else {
            lammpsScatter_(mLmpPtr, aName, aIsDouble, aData.rowNumber(), aData.columnNumber(), aData.asVecRow().data());
        }
    }
    private native static void lammpsScatter_(long aLmpPtr, String aName, boolean aIsDouble, int aAtomNum, int aCount, double[] aData) throws Error;
    
    /** 提供 {@link Lmpdat} 格式的获取质量 */
    public IVector masses() throws Error {
        IVector tMasses = atomDataOf("mass").asVecRow();
        return tMasses.subVec(1, tMasses.size());
    }
    public double mass(int aType) throws Error {return atomDataOf("mass").get(aType, 1);}
    
    /**
     * 通过 {@link #atomDataOf} 直接构造一个 {@link Lmpdat}，
     * 可以避免走文件管理系统
     * @param aNoVelocities 是否关闭速度信息，默认 false（包含速度信息）
     * @return 一个类似于读取 lammps data 文件后得到的 {@link Lmpdat}
     * @author liqa
     */
    public Lmpdat lmpdat(boolean aNoVelocities) throws Error {
        // 获取数据
        RowMatrix tID = atomDataOf("id");
        RowMatrix tType = atomDataOf("type");
        RowMatrix tXYZ = atomDataOf("x");
        @Nullable RowMatrix tVelocities = aNoVelocities ? null : atomDataOf("v");
        IMatrix tMasses = atomDataOf("mass");
        int tAtomTypeNum = tMasses.rowNumber()-1;
        // 设置 mass，按照 lammps 的设定只有这个范围内的才有意义
        IVector tMassesData = tMasses.asVecRow().subVec(1, tAtomTypeNum+1);
        // 构造 Lmpdat，其余数据由于可以直接存在 Lmpdat 中，因此不用归还
        return new Lmpdat(tAtomTypeNum, box(), tMassesData, tID.asVecRow(), tType.asVecRow(), tXYZ, tVelocities);
    }
    public Lmpdat lmpdat() throws Error {
        return lmpdat(false);
    }
    @VisibleForTesting public Lmpdat data(boolean aNoVelocities) throws Error {return lmpdat(aNoVelocities);}
    @VisibleForTesting public Lmpdat data() throws Error {return lmpdat();}
    
    /**
     * 更加易用的方法，类似于 lammps 的 {@code read_data} 命令，但是不需要走文件管理器；
     * 实际实现过程有些区别
     * @param aLmpdat 作为输入的原子数据
     * @author liqa
     */
    public void loadLmpdat(final Lmpdat aLmpdat) throws Error {
        checkThread();
        Box tBox = aLmpdat.lmpBox();
        if (tBox.type() == Box.Type.NORMAL) {
        command(String.format("region          box block %f %f %f %f %f %f",          tBox.xlo(), tBox.xhi(), tBox.ylo(), tBox.yhi(), tBox.zlo(), tBox.zhi()));
        } else {
        BoxPrism pBox = (BoxPrism)tBox;
        command(String.format("region          box prism %f %f %f %f %f %f %f %f %f", pBox.xlo(), pBox.xhi(), pBox.ylo(), pBox.yhi(), pBox.zlo(), pBox.zhi(), pBox.xy(), pBox.xz(), pBox.yz()));
        }
        int tAtomTypeNum = aLmpdat.atomTypeNum();
        command(String.format("create_box      %d box", tAtomTypeNum));
        IVector tMasses = aLmpdat.masses();
        if (tMasses != null) for (int i = 0; i < tAtomTypeNum; ++i) {
        command(String.format("mass            %d %f", i+1, tMasses.get(i)));
        }
        @Nullable RowMatrix tVelocities = aLmpdat.velocities();
        lammpsCreateAtoms_(mLmpPtr, aLmpdat.ids().internalData(), aLmpdat.types().internalData(), aLmpdat.positions().internalData(), tVelocities==null ? null : tVelocities.internalData(), null, false);
    }
    public void loadData(IAtomData aAtomData) throws Error {
        checkThread();
        if (aAtomData instanceof Lmpdat) {loadLmpdat((Lmpdat)aAtomData); return;}
        IXYZ tBox = aAtomData.box();
        command(String.format("region          box block 0 %f 0 %f 0 %f", tBox.x(), tBox.y(), tBox.z()));
        int tAtomTypeNum = aAtomData.atomTypeNum();
        command(String.format("create_box      %d box", tAtomTypeNum));
        // IVaspCommonData 包含原子种类字符，可以自动获取到质量
        if (aAtomData instanceof IVaspCommonData) {
        String[] tAtomTypes = ((IVaspCommonData)aAtomData).atomTypes();
        if (tAtomTypes!=null && tAtomTypes.length>=tAtomTypeNum) for (int i = 0; i < tAtomTypeNum; ++i) {
        command(String.format("mass            %d %f", i+1, MASS.getOrDefault(tAtomTypes[i], -1.0)));
        }}
        creatAtoms(aAtomData.asList());
    }
    public void loadData(Lmpdat aLmpdat) throws Error {loadLmpdat(aLmpdat);}
    
    /**
     * Create N atoms from list of coordinates and properties
     * <p>
     * This function is a wrapper around the {@code lammps_create_atoms()} function of the C-library interface.
     * @param aAtoms List of Atoms
     * @param aShrinkExceed whether to expand shrink-wrap boundaries if atoms are outside the box (false in default)
     */
    public void creatAtoms(List<? extends IAtom> aAtoms, boolean aShrinkExceed) throws Error {
        checkThread();
        final boolean tHasVelocities = UT.Code.first(aAtoms).hasVelocities();
        final int tAtomNum = aAtoms.size();
        double[] rID = DoubleArrayCache.getArray(tAtomNum);
        double[] rType = DoubleArrayCache.getArray(tAtomNum);
        double[] rXYZ = DoubleArrayCache.getArray(tAtomNum*3);
        double[] rVelocities = tHasVelocities ? DoubleArrayCache.getArray(tAtomNum*3) : null;
        int i = 0, j1 = 0, j2 = 0;
        for (IAtom tAtom : aAtoms) {
            rID[i] = tAtom.id();
            rType[i] = tAtom.type();
            ++i;
            rXYZ[j1] = tAtom.x(); ++j1;
            rXYZ[j1] = tAtom.y(); ++j1;
            rXYZ[j1] = tAtom.z(); ++j1;
            if (tHasVelocities) {
                rVelocities[j2] = tAtom.vx(); ++j2;
                rVelocities[j2] = tAtom.vy(); ++j2;
                rVelocities[j2] = tAtom.vz(); ++j2;
            }
        }
        lammpsCreateAtoms_(mLmpPtr, rID, rType, rXYZ, rVelocities, null, aShrinkExceed);
        DoubleArrayCache.returnArray(rID);
        DoubleArrayCache.returnArray(rType);
        DoubleArrayCache.returnArray(rXYZ);
        if (tHasVelocities) DoubleArrayCache.returnArray(rVelocities);
    }
    public void creatAtoms(List<? extends IAtom> aAtoms) throws Error {
        creatAtoms(aAtoms, false);
    }
    private native static void lammpsCreateAtoms_(long aLmpPtr, double[] aID, double[] aType, double[] aXYZ, double[] aVelocities, double[] aImage, boolean aShrinkExceed) throws Error;
    
    /**
     * lammps clear 指令
     */
    public void clear() throws Error {
        command("clear");
    }
    
    /**
     * Explicitly delete a LAMMPS instance through the C-library interface.
     * <p>
     * This is a wrapper around the {@code lammps_close()} function of the C-library interface.
     */
    public void shutdown() {
        if (!mDead) {
            mDead = true;
            try {
                checkThread();
                try {lammpsClose_(mLmpPtr);} catch (Error ignored) {}
            } catch (Error e) {
                e.printStackTrace(System.err);
            }
        }
    }
    private native static void lammpsClose_(long aLmpPtr) throws Error;
}
