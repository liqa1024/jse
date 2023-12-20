package jtool.lmp;

import jtool.code.UT;
import jtool.code.collection.AbstractCollections;
import jtool.code.iterator.IDoubleIterator;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import jtool.parallel.DoubleArrayCache;
import jtool.parallel.IAutoShutdown;
import jtool.parallel.MPI;
import jtool.parallel.MatrixCache;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
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
 * References:
 * <a href="https://docs.lammps.org/Python_module.html">
 * The lammps Python module </a>,
 * <a href="https://docs.lammps.org/Library.html/">
 * LAMMPS Library Interfaces </a>,
 * @author liqa
 */
public class NativeLmp implements IAutoShutdown {
    
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
    }
    
    private final static String LMPLIB_DIR = JAR_DIR+"lmp/";
    private final static String LMPLIB_PATH = LMPLIB_DIR + (IS_WINDOWS ? "lmp.dll" : (IS_MAC ? "lmp.jnilib" : "lmp.so"));
    private final static String[] LMPSRC_NAME = {
          "jtool_lmp_NativeLmp.c"
        , "jtool_lmp_NativeLmp.h"
    };
    private final static String NATIVE_DIR_NAME = "native", BUILD_DIR_NAME = IS_WINDOWS ? "build-win" : (IS_MAC ? "build-mac" : "build");
    private final static String NATIVE_LMPLIB_NAME = IS_WINDOWS ? "liblammps.dll" : (IS_MAC ? "liblammps.dylib" : "liblammps.so");
    
    private static String initCmakeSettingCmd_(String aNativeLmpBuildDir) {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cd"); rCommand.add(aNativeLmpBuildDir); rCommand.add(";");
        rCommand.add("cmake");
        // 设置输出动态链接库
        rCommand.add("-D"); rCommand.add("BUILD_SHARED_LIBS=yes");
        // 设置构建输出目录为 lib
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY=lib");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY=lib");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY=lib");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE=lib");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE=lib");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE=lib");
        // 添加额外的设置参数
        for (Map.Entry<String, String> tEntry : Conf.CMAKE_SETTING.entrySet()) {
            rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
        }
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
            EXE.system(String.format("cd %s; cmake ../cmake", Conf.LMP_HOME));
            // 设置参数
            EXE.system(initCmakeSettingCmd_(Conf.LMP_HOME));
            // 如果设置 CLEAN 则进行 clean 操作
            if (Conf.CLEAN) EXE.system(String.format("cd %s; cmake --build . --target clean", Conf.LMP_HOME));
            // 最后进行构造操作
            EXE.system(String.format("cd %s; cmake --build . --config Release", Conf.LMP_HOME));
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
            EXE.system(String.format("cd %s; cmake ..; cmake --build . --config Release", tBuildDir));
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
     * @param aPtr pointer to a LAMMPS C++ class instance when called from an embedded Python interpreter.
     *             0 means load symbols from shared library.
     *
     * @author liqa
     */
    public NativeLmp(String[] aArgs, long aComm, long aPtr) {
        String[] tArgs = aArgs==null ? DEFAULT_ARGS : new String[aArgs.length+1];
        tArgs[0] = EXECUTABLE_NAME;
        if (aArgs != null) System.arraycopy(aArgs, 0, tArgs, 1, aArgs.length);
        mLmpPtr = aComm==0 ? lammpsOpen_(tArgs, aPtr) : lammpsOpen_(tArgs, aComm, aPtr);
    }
    public NativeLmp(String[] aArgs, long aComm) {this(aArgs, aComm, 0);}
    public NativeLmp(String[] aArgs, MPI.Comm aComm, long aPtr) {this(aArgs, aComm==null ? 0 : aComm.ptr_(), aPtr);}
    public NativeLmp(String[] aArgs, MPI.Comm aComm) {this(aArgs, aComm, 0);}
    public NativeLmp(String[] aArgs) {this(aArgs, 0);}
    public NativeLmp() {this(null);}
    private native static long lammpsOpen_(String[] aArgs, long aComm, long aPtr);
    private native static long lammpsOpen_(String[] aArgs, long aPtr);
    
    /**
     * Return a numerical representation of the LAMMPS version in use.
     * <p>
     * This is a wrapper around the {@code lammps_version()} function of the C-library interface.
     * @return version number
     */
    public int version() {
        return lammpsVersion_(mLmpPtr);
    }
    private native static int lammpsVersion_(long aLmpPtr);
    
    /**
     * Read LAMMPS commands from a file.
     * <p>
     * This is a wrapper around the {@code lammps_file()} function of the C-library interface.
     * It will open the file with the name/path file and process the LAMMPS commands line by line
     * until the end.
     * The function will return when the end of the file is reached.
     * @param aPath Name of the file/path with LAMMPS commands
     */
    public void file(String aPath) {
        lammpsFile_(mLmpPtr, aPath);
    }
    private native static void lammpsFile_(long aLmpPtr, String aPath);
    
    /**
     * Process a single LAMMPS input command from a string.
     * <p>
     * This is a wrapper around the {@code lammps_command()} function of the C-library interface.
     * @param aCmd a single lammps command
     */
    public void command(String aCmd) {
        lammpsCommand_(mLmpPtr, aCmd);
    }
    private native static void lammpsCommand_(long aLmpPtr, String aCmd);
    
    /**
     * Process multiple LAMMPS input commands from a list of strings.
     * <p>
     * This is a wrapper around the {@code lammps_commands_list()} function of the C-library interface.
     * @param aCmds a list of lammps commands
     */
    public void commands(String[] aCmds) {
        lammpsCommandsList_(mLmpPtr, aCmds);
    }
    private native static void lammpsCommandsList_(long aLmpPtr, String[] aCmds);
    
    /**
     * Process a block of LAMMPS input commands from a string.
     * <p>
     * This is a wrapper around the {@code lammps_commands_string()} function of the C-library interface.
     * @param aMultiCmd text block of lammps commands
     */
    public void commands(String aMultiCmd) {
        lammpsCommandsString_(mLmpPtr, aMultiCmd);
    }
    private native static void lammpsCommandsString_(long aLmpPtr, String aMultiCmd);
    
    /**
     * Get the total number of atoms in the LAMMPS instance.
     * <p>
     * This is a wrapper around the {@code lammps_get_natoms()} function of the C-library interface.
     * @return number of atoms
     */
    public int atomNum() {
        return (int)lammpsGetNatoms_(mLmpPtr);
    }
    public int natoms() {return atomNum();}
    private native static double lammpsGetNatoms_(long aLmpPtr);
    
    /**
     * Get the total number of atoms types in the LAMMPS instance.
     * @return number of atom types
     */
    public int atomTypeNum() {return settingOf("ntypes");}
    public int ntype() {return atomTypeNum();}
    
    /**
     * Extract simulation box parameters
     * <p>
     * This is a wrapper around the lammps_extract_box() function of the C-library interface.
     * Unlike in the C function, the result is returned a {@link BoxPrism} object.
     * @return a {@link BoxPrism} object.
     */
    public BoxPrism box() {
        double[] rBox = DoubleArrayCache.getArray(15);
        lammpsExtractBox_(mLmpPtr, rBox);
        BoxPrism tOut = new BoxPrism(rBox[0], rBox[3], rBox[1], rBox[4], rBox[2], rBox[5], rBox[6], rBox[8], rBox[7]);
        DoubleArrayCache.returnArray(rBox);
        return tOut;
    }
    /**
     * {@code [xlo, ylo, zlo, xhi, yhi, zhi, xy, yz, xz, px, py, pz, bx, by, bz]}
     * <p>
     * {@code [0  , 1  , 2  , 3  , 4  , 5  , 6 , 7 , 8 , 9 , 10, 11, 12, 13, 14]}
     */
    private native static void lammpsExtractBox_(long aLmpPtr, double[] rBox);
    
    /**
     * Query LAMMPS about global settings that can be expressed as an integer.
     * <p>
     * This is a wrapper around the {@code lammps_extract_setting()} function of the C-library interface.
     * <a href="https://docs.lammps.org/Library_properties.html#_CPPv422lammps_extract_settingPvPKc">
     * Its documentation </a> includes a list of the supported keywords.
     * @param aName name of the setting
     * @return value of the setting
     */
    public int settingOf(String aName) {
        return lammpsExtractSetting_(mLmpPtr, aName);
    }
    private native static int lammpsExtractSetting_(long aLmpPtr, String aName);
    
    /**
     * Retrieve per-atom properties from LAMMPS
     * <p>
     * This is a wrapper around the {@code lammps_gather_concat()} function of the C-library interface.
     * <a href="https://docs.lammps.org/Classes_atom.html#_CPPv4N9LAMMPS_NS4Atom7extractEPKc">
     * Its documentation </a> includes a list of the supported keywords and their data types.
     * This function will try to auto-detect the data type by asking the library.
     * This function returns null if either the keyword is not recognized.
     * @param aName name of the property
     * @return requested data or null
     * @see <a href="https://docs.lammps.org/Library_scatter.html#_CPPv420lammps_gather_concatPvPKciiPv">
     * lammps_gather_concat() </a>
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    public IMatrix atomDataOf(String aName) {
        switch(aName) {
        case "mass":        {return atomDataOf(aName, true , 1);}
        case "id":          {return atomDataOf(aName, false, 1);}
        case "type":        {return atomDataOf(aName, false, 1);}
        case "mask":        {return atomDataOf(aName, false, 1);}
        case "image":       {return atomDataOf(aName, false, 1);}
        case "x":           {return atomDataOf(aName, true , 3);}
        case "v":           {return atomDataOf(aName, true , 3);}
        case "f":           {return atomDataOf(aName, true , 3);}
        case "molecule":    {return atomDataOf(aName, false, 1);}
        case "q":           {return atomDataOf(aName, true , 1);}
        case "mu":          {return atomDataOf(aName, true , 3);}
        case "omega":       {return atomDataOf(aName, true , 3);}
        case "angmom":      {return atomDataOf(aName, true , 3);}
        case "torque":      {return atomDataOf(aName, true , 3);}
        case "radius":      {return atomDataOf(aName, true , 1);}
        case "rmass":       {return atomDataOf(aName, true , 1);}
        case "ellipsoid":   {return atomDataOf(aName, false, 1);}
        case "line":        {return atomDataOf(aName, false, 1);}
        case "tri":         {return atomDataOf(aName, false, 1);}
        case "body":        {return atomDataOf(aName, false, 1);}
        case "quat":        {return atomDataOf(aName, true , 4);}
        case "temperature": {return atomDataOf(aName, true , 1);}
        case "heatflow":    {return atomDataOf(aName, true , 1);}
        default: {
            if (aName.startsWith("i_")) {
                return atomDataOf(aName, false, 1);
            } else
            if (aName.startsWith("d_")) {
                return atomDataOf(aName, true , 1);
            } else {
                throw new IllegalArgumentException("Unexpected name: "+aName+", use atomDataOf(aName, aIsDouble, aCount) to gather this atom data.");
            }
        }}
    }
    public IMatrix atomDataOf(String aName, boolean aIsDouble, int aCount) {
        int tAtomNum = atomNum();
        double[] rData = DoubleArrayCache.getArray(tAtomNum*aCount);
        lammpsGatherConcat_(mLmpPtr, aName, aIsDouble, aCount, rData);
        return new RowMatrix(tAtomNum, aCount, rData);
    }
    private native static void lammpsGatherConcat_(long aLmpPtr, String aName, boolean aIsDouble, int aCount, double[] rData);
    
    
    /**
     * 通过 {@link #atomDataOf} 直接构造一个 {@link Lmpdat}，
     * 可以避免走文件管理系统
     * @param aNoVelocities 是否关闭速度信息，默认 false（包含速度信息）
     * @return 一个类似于读取 lammps data 文件后得到的 {@link Lmpdat}
     * @author liqa
     */
    public Lmpdat lmpdat(boolean aNoVelocities) {
        // 获取数据
        IMatrix tID = atomDataOf("id");
        IMatrix tType = atomDataOf("type");
        IMatrix tXYZ = atomDataOf("x");
        @Nullable IMatrix tVelocities = aNoVelocities ? null : atomDataOf("v");
        IMatrix tMasses = atomDataOf("mass");
        int tAtomNum = tXYZ.rowNumber();
        int tAtomTypeNum = atomTypeNum();
        // 设置 mass，按照 lammps 的设定只有这个范围内的才有意义（但是超出范围的依旧会进行访问，因此还是需要一个 atomNum 长的数组）
        IVector tMassesData = tMasses.asVecRow().slicer().get(AbstractCollections.range(1, tAtomTypeNum+1));
        MatrixCache.returnMat(tMasses);
        // 设置 atomData
        IMatrix tAtomData = RowMatrix.zeros(tAtomNum, STD_ATOM_DATA_KEYS.length);
        IDoubleIterator itID   = tID  .iteratorRow();
        IDoubleIterator itType = tType.iteratorRow();
        IDoubleIterator itXYZ  = tXYZ .iteratorRow();
        for (IVector tRow : tAtomData.rows()) {
            tRow.set(STD_ID_COL  , itID  .next());
            tRow.set(STD_TYPE_COL, itType.next());
            tRow.set(STD_X_COL   , itXYZ .next());
            tRow.set(STD_Y_COL   , itXYZ .next());
            tRow.set(STD_Z_COL   , itXYZ .next());
        }
        MatrixCache.returnMat(tID);
        MatrixCache.returnMat(tType);
        MatrixCache.returnMat(tXYZ);
        // 构造 Lmpdat，其余数据由于可以直接存在 Lmpdat 中，因此不用归还
        return new Lmpdat(tAtomTypeNum, box(), tMassesData, tAtomData, tVelocities);
    }
    public Lmpdat lmpdat() {
        return lmpdat(false);
    }
    
    /**
     * Explicitly delete a LAMMPS instance through the C-library interface.
     * <p>
     * This is a wrapper around the {@code lammps_close()} function of the C-library interface.
     */
    public void shutdown() {
        lammpsClose_(mLmpPtr);
    }
    private native static void lammpsClose_(long aLmpPtr);
}
