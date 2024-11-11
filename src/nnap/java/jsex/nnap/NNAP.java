package jsex.nnap;

import com.google.common.collect.Lists;
import jse.atom.*;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.clib.*;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.ISlice;
import jse.code.collection.IntList;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.*;

import static jse.code.CS.VERSION;
import static jse.code.Conf.*;
import static jse.code.OS.EXEC;
import static jse.code.OS.JAR_DIR;

/**
 * jse 实现的 nnap 计算器，所有
 * nnap 相关能量和力的计算都在此实现
 * <p>
 * 考虑到 Torch 本身的内存安全性，此类设计时确保不同对象之间线程安全，
 * 而不同线程访问相同的对象线程不安全
 * <p>
 * 由于需要并行来绕开 GIL，并且考虑到效率问题，这里需要使用原生的 pytorch
 * <p>
 * 和其他的 java 中调用 pytorch 库不同的是，这里不会做内存管理，主要考虑了以下原因：
 * <pre>
 *    1. 借助 gc 来回收非 java 内存很低效，实际往往还是需要手动关闭
 *    2. 和 {@link jse.lmp.NativeLmp} 保持一致，而 lammps 库内部依旧还是需要手动管理内存
 *    3. 方便外部扩展，而不需要担心破坏自动回收的部分
 * </pre>
 * @author liqa
 */
public class NNAP implements IAutoShutdown {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    public final static class Conf {
        /**
         * 自定义构建 nnap 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new HashMap<>();
        
        /**
         * 自定义构建 nnap 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_NNAP"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NNAP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_NNAP"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NNAP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 nnap，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_NNAP", jse.code.Conf.USE_MIMALLOC);
        
        /** 重定向 nnap 动态库的路径 */
        public static @Nullable String REDIRECT_NNAP_LIB = OS.env("JSE_REDIRECT_NNAP_LIB");
    }
    
    private final static String LIB_DIR = JAR_DIR+"nnap/" + UT.Code.uniqueID(VERSION, Torch.HOME, Conf.USE_MIMALLOC, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    private final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jsex_nnap_NNAP.cpp"
        , "jsex_nnap_NNAP.h"
    };
    
    
    private static String cmakeInitCmd_() {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 这里设置 C/C++ 编译器（如果有）
        if (Conf.CMAKE_C_COMPILER   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + Conf.CMAKE_C_COMPILER);}
        if (Conf.CMAKE_CXX_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ Conf.CMAKE_CXX_COMPILER);}
        if (Conf.CMAKE_C_FLAGS      != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"    + Conf.CMAKE_C_FLAGS  +"'");}
        if (Conf.CMAKE_CXX_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + Conf.CMAKE_CXX_FLAGS+"'");}
        // 初始化使用上一个目录的 CMakeList.txt
        rCommand.add("..");
        return String.join(" ", rCommand);
    }
    private static String cmakeSettingCmd_() throws IOException {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="+(Conf.USE_MIMALLOC?"ON":"OFF"));
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
    
    private static @NotNull String initNNAP_() throws Exception {
        // 检测 cmake，为了简洁并避免问题，现在要求一定要有 cmake 环境
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXEC.system("cmake --version") != 0;
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("NNAP BUILD ERROR: No cmake environment.");
        // 从内部资源解压到临时目录
        String tWorkingDir = WORKING_DIR_OF("nnap");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        for (String tName : SRC_NAME) {
            UT.IO.copy(UT.IO.getResource("nnap/src/"+tName), tWorkingDir+tName);
        }
        // 这里对 CMakeLists.txt 特殊处理
        UT.IO.map(UT.IO.getResource("nnap/src/CMakeLists.txt"), tWorkingDir+"CMakeLists.txt", line -> {
            // 替换其中的 jniutil 库路径为设置好的路径
            line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
            // 替换其中的 mimalloc 库路径为设置好的路径
            if (Conf.USE_MIMALLOC) {
                line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
            }
            // 替换其中的 torch 库路径为设置好的路径
            line = line.replace("$ENV{JSE_TORCH_CMAKE_DIR}", Torch.CMAKE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
            return line;
        });
        System.out.println("NNAP INIT INFO: Building nnap from source code...");
        String tBuildDir = tWorkingDir+"build/";
        UT.IO.makeDir(tBuildDir);
        // 直接通过系统指令来编译 nnap 的库，关闭输出
        EXEC.setNoSTDOutput().setWorkingDir(tBuildDir);
        // 初始化 cmake
        EXEC.system(cmakeInitCmd_());
        // 设置参数
        EXEC.system(cmakeSettingCmd_());
        // 最后进行构造操作
        EXEC.system("cmake --build . --config Release");
        EXEC.setNoSTDOutput(false).setWorkingDir(null);
        // 简单检测一下是否编译成功
        @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "nnap");
        if (tLibName == null) throw new Exception("NNAP BUILD ERROR: Build Failed, No nnap lib in '"+LIB_DIR+"'");
        // 完事后移除临时解压得到的源码
        UT.IO.removeDir(tWorkingDir);
        System.out.println("NNAP INIT INFO: nnap successfully installed.");
        // 输出安装完成后的库名称
        return tLibName;
    }
    
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 torch
        Torch.InitHelper.init();
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
        if (Conf.USE_MIMALLOC) MiMalloc.InitHelper.init();
        // 这里不直接依赖 LmpPlugin
        
        if (Conf.REDIRECT_NNAP_LIB == null) {
            @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "nnap");
            // 如果不存在 jni lib 则需要重新通过源码编译
            if (tLibName == null) {
                System.out.println("NNAP INIT INFO: nnap libraries not found. Reinstalling...");
                try {tLibName = initNNAP_();} catch (Exception e) {throw new RuntimeException(e);}
            }
            LIB_PATH = LIB_DIR + tLibName;
        } else {
            if (DEBUG) System.out.println("NNAP INIT INFO: nnap libraries are redirected to '" + Conf.REDIRECT_NNAP_LIB + "'");
            LIB_PATH = Conf.REDIRECT_NNAP_LIB;
        }
        // 设置库路径
        System.load(UT.IO.toAbsolutePath(LIB_PATH));
        
        // 这里需要 torch 单线程
        try {setSingleThread0();}
        catch (TorchException ignored) {/* 可能已经设置过，这里就不考虑 */}
    }
    private static native void setSingleThread0() throws TorchException;
    
    public class SingleNNAP {
        private final long[] mModelPtrs;
        private final String mSymbol;
        private final double mRefEng;
        private final IVector mNormVec;
        private final Basis.IBasis mBasis;
        public String symbol() {return mSymbol;}
        public double refEng() {return mRefEng;}
        public IVector normVec() {return mNormVec;}
        public Basis.IBasis basis() {return mBasis;}
        
        @SuppressWarnings("unchecked")
        private SingleNNAP(int aTypeNum, Map<String, ?> aModelInfo) throws TorchException {
            Object tSymbol = aModelInfo.get("symbol");
            if (tSymbol == null) throw new IllegalArgumentException("No symbol in ModelInfo");
            mSymbol = tSymbol.toString();
            Number tRefEng = (Number)aModelInfo.get("ref_eng");
            if (tRefEng == null) throw new IllegalArgumentException("No ref_eng in ModelInfo");
            mRefEng = tRefEng.doubleValue();
            List<? extends Number> tNormVec = (List<? extends Number>)aModelInfo.get("norm_vec");
            if (tNormVec == null) throw new IllegalArgumentException("No norm_vec in ModelInfo");
            mNormVec = Vectors.from(tNormVec);
            
            Map<String, ?> tBasis = (Map<String, ?>)aModelInfo.get("basis");
            if (tBasis == null) {
                tBasis = Maps.of(
                    "type", "spherical_chebyshev",
                    "nmax", Basis.SphericalChebyshev.DEFAULT_NMAX,
                    "lmax", Basis.SphericalChebyshev.DEFAULT_LMAX,
                    "rcut", Basis.SphericalChebyshev.DEFAULT_RCUT
                );
            }
            Object tBasisType = tBasis.get("type");
            if (tBasisType == null) {
                tBasisType = "spherical_chebyshev";
            }
            if (!tBasisType.equals("spherical_chebyshev")) throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
            mBasis = new Basis.SphericalChebyshev(
                aTypeNum,
                ((Number)UT.Code.getWithDefault(tBasis, Basis.SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue(),
                ((Number)UT.Code.getWithDefault(tBasis, Basis.SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue(),
                ((Number)UT.Code.getWithDefault(tBasis, Basis.SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue()
            );
            Object tModel = aModelInfo.get("torch");
            if (tModel == null) throw new IllegalArgumentException("No torch data in ModelInfo");
            byte[] tModelBytes = Base64.getDecoder().decode(tModel.toString());
            mModelPtrs = new long[mThreadNumber];
            for (int i = 0; i < mThreadNumber; ++i) {
                long tModelPtr = load1(tModelBytes, tModelBytes.length);
                if (tModelPtr==0 || tModelPtr==-1) throw new TorchException("Failed to load Torch Model");
                mModelPtrs[i] = tModelPtr;
            }
        }
        
        protected double forward(int aThreadID, double[] aX, int aStart, int aCount) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            return forward0(mModelPtrs[aThreadID], aX, aStart, aCount);
        }
        protected double forward(double[] aX, int aStart, int aCount) throws TorchException {return forward(0, aX, aStart, aCount);}
        protected double forward(double[] aX, int aCount) throws TorchException {return forward(aX, 0, aCount);}
        protected double forward(double[] aX) throws TorchException {return forward(aX, aX.length);}
        protected double forward(int aThreadID, DoubleCPointer aX, int aCount) throws TorchException {return forward1(mModelPtrs[aThreadID], aX.ptr_(), aCount);}
        protected double forward(DoubleCPointer aX, int aCount) throws TorchException {return forward(0, aX, aCount);}
        
        protected double backward(int aThreadID, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            rangeCheck(rGradX.length, rStart+aCount);
            return backward0(mModelPtrs[aThreadID], aX, aStart, rGradX, rStart, aCount);
        }
        protected double backward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {return backward(0, aX, aStart, rGradX, rStart, aCount);}
        protected double backward(double[] aX, double[] rGradX, int aCount) throws TorchException {return backward(aX, 0, rGradX, 0, aCount);}
        protected double backward(double[] aX, double[] rGradX) throws TorchException {return backward(aX, rGradX, aX.length);}
        protected double backward(int aThreadID, DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward1(mModelPtrs[aThreadID], aX.ptr_(), rGradX.ptr_(), aCount);}
        protected double backward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward(0, aX, rGradX, aCount);}
    }
    private final List<SingleNNAP> mModels;
    private final @Nullable String mUnits;
    private boolean mDead = false;
    private final int mThreadNumber;
    public int atomTypeNumber() {return mModels.size();}
    public SingleNNAP model(int aType) {return mModels.get(aType-1);}
    public String units() {return mUnits;}
    
    @SuppressWarnings("unchecked")
    public NNAP(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException {
        mThreadNumber = aThreadNumber;
        Number tVersion = (Number)aModelInfo.get("version");
        if (tVersion != null) {
            int tVersionValue = tVersion.intValue();
            if (tVersionValue != 1) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        mUnits = UT.Code.toString(aModelInfo.get("units"));
        List<? extends Map<String, ?>> tModelInfos = (List<? extends Map<String, ?>>)aModelInfo.get("models");
        if (tModelInfos == null) throw new IllegalArgumentException("No models in ModelInfo");
        mModels = new ArrayList<>(tModelInfos.size());
        for (Map<String, ?> tModelInfo : tModelInfos) {
            mModels.add(new SingleNNAP(tModelInfos.size(), tModelInfo));
        }
    }
    public NNAP(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException, IOException {
        this(aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? UT.IO.yaml2map(aModelPath) : UT.IO.json2map(aModelPath), aThreadNumber);
    }
    public NNAP(Map<?, ?> aModelInfo) throws TorchException {this(aModelInfo, 1);}
    public NNAP(String aModelPath) throws TorchException, IOException {this(aModelPath, 1);}
    private static native long load0(String aModelPath) throws TorchException;
    private static native long load1(byte[] aModelBytes, int aSize) throws TorchException;
    
    @Override public void shutdown() {
        if (!mDead) {
            mDead = true;
            for (SingleNNAP tModel : mModels) for (int i = 0; i < mThreadNumber; ++i) {
                shutdown0(tModel.mModelPtrs[i]);
            }
        }
    }
    private static native void shutdown0(long aModelPtr);
    public boolean isShutdown() {return mDead;}
    public int threadNumber() {return mThreadNumber;}
    @VisibleForTesting public int nthreads() {return threadNumber();}
    
    
    protected IAtomData reorderSymbols(IAtomData aAtomData) {
        if (mModels.size() < aAtomData.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of AtomData: " + aAtomData.atomTypeNumber() + ", model: " + mModels.size());
        List<String> tAtomDataSymbols = aAtomData.symbols();
        if (tAtomDataSymbols==null || sameOrder(tAtomDataSymbols)) return aAtomData;
        int[] tAtomDataType2newType = new int[tAtomDataSymbols.size()+1];
        for (int i = 0; i < tAtomDataSymbols.size(); ++i) {
            String tElem = tAtomDataSymbols.get(i);
            int idx = indexOf(tElem);
            if (idx < 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in AtomData");
            tAtomDataType2newType[i+1] = idx+1;
        }
        return aAtomData.opt().mapType(atom -> tAtomDataType2newType[atom.type()]);
    }
    public boolean sameOrder(List<String> aSymbols) {
        for (int i = 0; i < aSymbols.size(); ++i) {
            if (!aSymbols.get(i).equals(mModels.get(i).mSymbol)) {
                return false;
            }
        }
        return true;
    }
    public int indexOf(String aSymbol) {
        for (int i = 0; i < mModels.size(); ++i) {
            if (aSymbol.equals(mModels.get(i).mSymbol)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 使用 nnap 计算给定原子结构每个原子的能量值
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 每个原子能量组成的向量
     * @author liqa
     */
    public Vector calEnergies(final MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tAtomNumber = aMPC.atomNumber();
        Vector rEngs = VectorCache.getVec(tAtomNumber);
        try {
            aMPC.pool_().parforWithException(tAtomNumber, null, null, (i, threadID) -> {
                final SingleNNAP tModel = model(aMPC.atomType_().get(i));
                RowMatrix tBasis = tModel.mBasis.eval(dxyzTypeDo -> {
                    aMPC.nl_().forEachNeighbor(i, tModel.mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, aMPC.atomType_().get(idx));
                    });
                });
                tBasis.asVecRow().div2this(tModel.mNormVec);
                double tPred = tModel.forward(threadID, tBasis.internalData(), tBasis.internalDataShift(), tBasis.internalDataSize());
                MatrixCache.returnMat(tBasis);
                tPred += tModel.mRefEng;
                rEngs.set(i, tPred);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergies(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergies(tMPC);}
    }
    /**
     * 使用 nnap 计算给定原子结构指定原子的能量
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @return 此原子的能量组成的向量
     * @author liqa
     */
    public Vector calEnergiesAt(final MonatomicParameterCalculator aMPC, ISlice aIndices) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tSize = aIndices.size();
        Vector rEngs = VectorCache.getVec(tSize);
        try {
            aMPC.pool_().parforWithException(tSize, null, null, (i, threadID) -> {
                final int cIdx = aIndices.get(i);
                final SingleNNAP tModel = model(aMPC.atomType_().get(cIdx));
                RowMatrix tBasis = tModel.mBasis.eval(dxyzTypeDo -> {
                    aMPC.nl_().forEachNeighbor(cIdx, tModel.mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, aMPC.atomType_().get(idx));
                    });
                });
                tBasis.asVecRow().div2this(tModel.mNormVec);
                double tPred = tModel.forward(threadID, tBasis.internalData(), tBasis.internalDataShift(), tBasis.internalDataSize());
                MatrixCache.returnMat(tBasis);
                tPred += tModel.mRefEng;
                rEngs.set(i, tPred);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergiesAt(IAtomData aAtomData, ISlice aIndices) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergiesAt(tMPC, aIndices);}
    }
    /**
     * 使用 nnap 计算给定原子结构的总能量
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 总能量
     * @author liqa
     */
    public double calEnergy(MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector tEngs = calEnergies(aMPC);
        double tTotEng = tEngs.sum();
        VectorCache.returnVec(tEngs);
        return tTotEng;
    }
    public double calEnergy(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergy(tMPC);}
    }
    
    /**
     * 计算移动前后的能量差，只计算移动影响的近邻列表中的原子，可以加快计算速度
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aRestoreMPC 计算完成后是否还原 MPC 的状态，默认为 {@code true}
     * @return 移动后能量 - 移动前能量
     */
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        SingleNNAP tModel = model(aMPC.atomType_().get(aI));
        XYZ oXYZ = new XYZ(aMPC.atomDataXYZ_().row(aI));
        IIntVector oNL = aMPC.getNeighborList(oXYZ, tModel.mBasis.rcut());
        XYZ nXYZ = oXYZ.plus(aDx, aDy, aDz);
        IIntVector nNL = aMPC.getNeighborList(nXYZ, tModel.mBasis.rcut());
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(oNL.size());
        oNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        nNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aMPC, tNL);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aMPC.setAtomXYZ(aI, nXYZ), tNL);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreMPC) aMPC.setAtomXYZ(aI, oXYZ);
        return nEng - oEng;
    }
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDx, aDy, aDz, true);}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, double aDx, double aDy, double aDz) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffMove(tMPC, aI, aDx, aDy, aDz, false);}
    }
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    /**
     * 计算交换种类前后的能量差，只计算交换影响的近邻列表中的原子，可以加快计算速度
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aRestoreMPC 计算完成后是否还原 MPC 的状态，默认为 {@code true}
     * @return 交换后能量 - 交换前能量
     */
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ, boolean aRestoreMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        int oTypeI = aMPC.atomType_().get(aI);
        int oTypeJ = aMPC.atomType_().get(aJ);
        if (oTypeI == oTypeJ) return 0.0;
        IIntVector iNL = aMPC.getNeighborList(aI, model(oTypeI).mBasis.rcut());
        IIntVector jNL = aMPC.getNeighborList(aJ, model(oTypeJ).mBasis.rcut());
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(iNL.size()+1);
        tNL.add(aI);
        iNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        if (!tNL.contains(aJ)) tNL.add(aJ);
        jNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aMPC, tNL);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aMPC.setAtomType(aI, oTypeJ).setAtomType(aJ, oTypeI), tNL);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreMPC) aMPC.setAtomType(aI, oTypeI).setAtomType(aJ, oTypeJ);
        return nEng - oEng;
    }
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ) throws TorchException {return calEnergyDiffSwap(aMPC, aI, aJ, true);}
    public double calEnergyDiffSwap(IAtomData aAtomData, int aI, int aJ) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffSwap(tMPC, aI, aJ, false);}
    }
    
    
    /**
     * 使用 nnap 计算给定原子结构每个原子和受力
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 每个原子力组成的矩阵，按行排列
     * @author liqa
     */
    public RowMatrix calForces(MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        RowMatrix rForces = MatrixCache.getMatRow(aMPC.atomNumber(), 3);
        calEnergyForcesVirials(aMPC, null, rForces.col(0), rForces.col(1), rForces.col(2), null, null, null, null, null, null);
        return rForces;
    }
    public RowMatrix calForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calForces(tMPC);}
    }
    
    /**
     * 使用 nnap 计算给定原子结构的应力具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 原子结构本身或者 MPC，使用 MPC 时统一不考虑速度部分
     * @param aIdealGas 是否考虑理想气体部分（速度效应部分），默认为 {@code false}
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力值
     * @author liqa
     */
    public List<Double> calStress(IAtomData aAtomData, boolean aIdealGas) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        final int tAtomNum = aAtomData.atomNumber();
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {
            List<Double> tStress = calStress(tMPC);
            if (!aIdealGas || !aAtomData.hasMass() || !aAtomData.hasVelocity()) return tStress;
            // 累加速度项，这里需要消去整体的平动
            double vxTot = 0.0, vyTot = 0.0, vzTot = 0.0;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                vxTot += tAtom.vx(); vyTot += tAtom.vy(); vzTot += tAtom.vz();
            }
            vxTot /= (double)tAtomNum;
            vyTot /= (double)tAtomNum;
            vzTot /= (double)tAtomNum;
            double tStressXX = 0.0, tStressYY = 0.0, tStressZZ = 0.0, tStressXY = 0.0, tStressXZ = 0.0, tStressYZ = 0.0;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                if (!tAtom.hasMass()) continue;
                double vx = tAtom.vx() - vxTot, vy = tAtom.vy() - vyTot, vz = tAtom.vz() - vzTot;
                double tMass = tAtom.mass();
                tStressXX -= tMass * vx*vx;
                tStressYY -= tMass * vy*vy;
                tStressZZ -= tMass * vz*vz;
                tStressXY -= tMass * vx*vy;
                tStressXZ -= tMass * vx*vz;
                tStressYZ -= tMass * vy*vz;
            }
            double tVolume = tMPC.volume();
            tStressXX /= tVolume;
            tStressYY /= tVolume;
            tStressZZ /= tVolume;
            tStressXY /= tVolume;
            tStressXZ /= tVolume;
            tStressYZ /= tVolume;
            tStress.set(0, tStress.get(0) + tStressXX);
            tStress.set(1, tStress.get(1) + tStressYY);
            tStress.set(2, tStress.get(2) + tStressZZ);
            tStress.set(3, tStress.get(3) + tStressXY);
            tStress.set(4, tStress.get(4) + tStressXZ);
            tStress.set(5, tStress.get(5) + tStressYZ);
            return tStress;
        }
    }
    public List<Double> calStress(IAtomData aAtomData) throws TorchException {return calStress(aAtomData, false);}
    public List<Double> calStress(MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        final int tAtomNum = aMPC.atomNumber();
        Vector rVirialsXX = VectorCache.getVec(tAtomNum);
        Vector rVirialsYY = VectorCache.getVec(tAtomNum);
        Vector rVirialsZZ = VectorCache.getVec(tAtomNum);
        Vector rVirialsXY = VectorCache.getVec(tAtomNum);
        Vector rVirialsXZ = VectorCache.getVec(tAtomNum);
        Vector rVirialsYZ = VectorCache.getVec(tAtomNum);
        calEnergyForcesVirials(aMPC, null, null, null, null, rVirialsXX, rVirialsYY, rVirialsZZ, rVirialsXY, rVirialsXZ, rVirialsYZ);
        double tStressXX = -rVirialsXX.sum();
        double tStressYY = -rVirialsYY.sum();
        double tStressZZ = -rVirialsZZ.sum();
        double tStressXY = -rVirialsXY.sum();
        double tStressXZ = -rVirialsXZ.sum();
        double tStressYZ = -rVirialsYZ.sum();
        VectorCache.returnVec(rVirialsYZ);
        VectorCache.returnVec(rVirialsXZ);
        VectorCache.returnVec(rVirialsXY);
        VectorCache.returnVec(rVirialsZZ);
        VectorCache.returnVec(rVirialsYY);
        VectorCache.returnVec(rVirialsXX);
        double tVolume = aMPC.volume();
        tStressXX /= tVolume;
        tStressYY /= tVolume;
        tStressZZ /= tVolume;
        tStressXY /= tVolume;
        tStressXZ /= tVolume;
        tStressYZ /= tVolume;
        return Lists.newArrayList(tStressXX, tStressYY, tStressZZ, tStressXY, tStressXZ, tStressYZ);
    }
    
    /**
     * 使用 nnap 同时计算给定原子结构每个原子的能量和受力
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 按照 {@code [eng, fx, fy, fz]} 的顺序返回结果
     * @author liqa
     */
    public List<Vector> calEnergyForces(MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector rEngs = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesX = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesY = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesZ = VectorCache.getVec(aMPC.atomNumber());
        calEnergyForcesVirials(aMPC, rEngs, rForcesX, rForcesY, rForcesZ, null, null, null, null, null, null);
        return Lists.newArrayList(rEngs, rForcesX, rForcesY, rForcesZ);
    }
    public List<Vector> calEnergyForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyForces(tMPC);}
    }
    
    /**
     * 使用 nnap 计算所有需要的热力学量，需要注意的是，这里位力采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @author liqa
     */
    public void calEnergyForcesVirials(final MonatomicParameterCalculator aMPC, final @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tAtomNumber = aMPC.atomNumber();
        final int tThreadNumber = aMPC.threadNumber();
        // 力需要累加统计，所以统一设置为 0
        if (rForcesX != null) rForcesX.fill(0.0);
        if (rForcesY != null) rForcesY.fill(0.0);
        if (rForcesZ != null) rForcesZ.fill(0.0);
        // 位力需要累加统计，所以统一设置为 0
        if (rVirialsXX != null) rVirialsXX.fill(0.0);
        if (rVirialsYY != null) rVirialsYY.fill(0.0);
        if (rVirialsZZ != null) rVirialsZZ.fill(0.0);
        if (rVirialsXY != null) rVirialsXY.fill(0.0);
        if (rVirialsXZ != null) rVirialsXZ.fill(0.0);
        if (rVirialsYZ != null) rVirialsYZ.fill(0.0);
        // 并行情况下存在并行写入的问题，因此需要这样操作
        IVector @Nullable[] rForcesXPar = rForcesX!=null ? new IVector[tThreadNumber] : null; if (rForcesX != null) {rForcesXPar[0] = rForcesX; for (int i = 1; i < tThreadNumber; ++i) {rForcesXPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rForcesYPar = rForcesY!=null ? new IVector[tThreadNumber] : null; if (rForcesY != null) {rForcesYPar[0] = rForcesY; for (int i = 1; i < tThreadNumber; ++i) {rForcesYPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rForcesZPar = rForcesZ!=null ? new IVector[tThreadNumber] : null; if (rForcesZ != null) {rForcesZPar[0] = rForcesZ; for (int i = 1; i < tThreadNumber; ++i) {rForcesZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsXXPar = rVirialsXX!=null ? new IVector[tThreadNumber] : null; if (rVirialsXX != null) {rVirialsXXPar[0] = rVirialsXX; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXXPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsYYPar = rVirialsYY!=null ? new IVector[tThreadNumber] : null; if (rVirialsYY != null) {rVirialsYYPar[0] = rVirialsYY; for (int i = 1; i < tThreadNumber; ++i) {rVirialsYYPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsZZPar = rVirialsZZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsZZ != null) {rVirialsZZPar[0] = rVirialsZZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsZZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsXYPar = rVirialsXY!=null ? new IVector[tThreadNumber] : null; if (rVirialsXY != null) {rVirialsXYPar[0] = rVirialsXY; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXYPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsXZPar = rVirialsXZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsXZ != null) {rVirialsXZPar[0] = rVirialsXZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsYZPar = rVirialsYZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsYZ != null) {rVirialsYZPar[0] = rVirialsYZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsYZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        try {
            aMPC.pool_().parforWithException(tAtomNumber, null, null, (i, threadID) -> {
                final @Nullable IVector tForcesX = rForcesX!=null ? rForcesXPar[threadID] : null;
                final @Nullable IVector tForcesY = rForcesY!=null ? rForcesYPar[threadID] : null;
                final @Nullable IVector tForcesZ = rForcesZ!=null ? rForcesZPar[threadID] : null;
                final @Nullable IVector tVirialsXX = rVirialsXX!=null ? rVirialsXXPar[threadID] : null;
                final @Nullable IVector tVirialsYY = rVirialsYY!=null ? rVirialsYYPar[threadID] : null;
                final @Nullable IVector tVirialsZZ = rVirialsZZ!=null ? rVirialsZZPar[threadID] : null;
                final @Nullable IVector tVirialsXY = rVirialsXY!=null ? rVirialsXYPar[threadID] : null;
                final @Nullable IVector tVirialsXZ = rVirialsXZ!=null ? rVirialsXZPar[threadID] : null;
                final @Nullable IVector tVirialsYZ = rVirialsYZ!=null ? rVirialsYZPar[threadID] : null;
                final SingleNNAP tModel = model(aMPC.atomType_().get(i));
                final List<@NotNull RowMatrix> tOut = tModel.mBasis.evalPartial(true, true, dxyzTypeDo -> {
                    aMPC.nl_().forEachNeighbor(i, tModel.mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, aMPC.atomType_().get(idx));
                    });
                });
                RowMatrix tBasis = tOut.get(0); tBasis.asVecRow().div2this(tModel.mNormVec);
                final Vector tPredPartial = VectorCache.getVec(tBasis.rowNumber()*tBasis.columnNumber());
                double tPred = tModel.backward(threadID, tBasis.internalData(), tBasis.internalDataShift(), tPredPartial.internalData(), tPredPartial.internalDataShift(), tBasis.internalDataSize());
                tPredPartial.div2this(tModel.mNormVec);
                if (rEnergies != null) {
                    tPred += tModel.mRefEng;
                    rEnergies.set(i, tPred);
                }
                if (tForcesX != null) tForcesX.add(i, -tPredPartial.opt().dot(tOut.get(1).asVecRow()));
                if (tForcesY != null) tForcesY.add(i, -tPredPartial.opt().dot(tOut.get(2).asVecRow()));
                if (tForcesZ != null) tForcesZ.add(i, -tPredPartial.opt().dot(tOut.get(3).asVecRow()));
                // 累加交叉项到近邻
                final int tNN = (tOut.size()-4)/3;
                final int[] j = {0};
                aMPC.nl_().forEachNeighbor(i, tModel.mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                    double fx = -tPredPartial.opt().dot(tOut.get(4+j[0]).asVecRow());
                    double fy = -tPredPartial.opt().dot(tOut.get(4+tNN+j[0]).asVecRow());
                    double fz = -tPredPartial.opt().dot(tOut.get(4+tNN+tNN+j[0]).asVecRow());
                    if (tForcesX != null) tForcesX.add(idx, fx);
                    if (tForcesY != null) tForcesY.add(idx, fy);
                    if (tForcesZ != null) tForcesZ.add(idx, fz);
                    if (tVirialsXX != null) tVirialsXX.add(idx, dx*fx);
                    if (tVirialsYY != null) tVirialsYY.add(idx, dy*fy);
                    if (tVirialsZZ != null) tVirialsZZ.add(idx, dz*fz);
                    if (tVirialsXY != null) tVirialsXY.add(idx, dx*fy);
                    if (tVirialsXZ != null) tVirialsXZ.add(idx, dx*fz);
                    if (tVirialsYZ != null) tVirialsYZ.add(idx, dy*fz);
                    ++j[0];
                });
                VectorCache.returnVec(tPredPartial);
                MatrixCache.returnMat(tOut);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 累加其余线程的数据然后归还临时变量
        if (rForcesZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesZ.plus2this(rForcesZPar[i]); VectorCache.returnVec(rForcesZPar[i]);}}
        if (rForcesY != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesY.plus2this(rForcesYPar[i]); VectorCache.returnVec(rForcesYPar[i]);}}
        if (rForcesX != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesX.plus2this(rForcesXPar[i]); VectorCache.returnVec(rForcesXPar[i]);}}
        if (rVirialsYZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsYZ.plus2this(rVirialsYZPar[i]); VectorCache.returnVec(rVirialsYZPar[i]);}}
        if (rVirialsXZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXZ.plus2this(rVirialsXZPar[i]); VectorCache.returnVec(rVirialsXZPar[i]);}}
        if (rVirialsXY != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXY.plus2this(rVirialsXYPar[i]); VectorCache.returnVec(rVirialsXYPar[i]);}}
        if (rVirialsZZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsZZ.plus2this(rVirialsZZPar[i]); VectorCache.returnVec(rVirialsZZPar[i]);}}
        if (rVirialsYY != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsYY.plus2this(rVirialsYYPar[i]); VectorCache.returnVec(rVirialsYYPar[i]);}}
        if (rVirialsXX != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXX.plus2this(rVirialsXXPar[i]); VectorCache.returnVec(rVirialsXXPar[i]);}}
    }
    
    
    private static native double forward0(long aModelPtr, double[] aX, int aStart, int aCount) throws TorchException;
    private static native double forward1(long aModelPtr, long aXPtr, int aCount) throws TorchException;
    private static native double backward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException;
    private static native double backward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount) throws TorchException;
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
