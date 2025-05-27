package jsex.nnap.basis;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IHasSymbol;
import jse.clib.JNIUtil;
import jse.code.CS;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.io.ISavable;
import jse.math.IDataShell;
import jse.math.vector.DoubleArrayVector;
import jse.parallel.IAutoShutdown;
import jsex.nnap.NNAP;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import static jse.code.OS.JAR_DIR;

/**
 * 通用的 nnap 基组/描述符实现
 * <p>
 * 由于内部会缓存近邻列表，因此此类相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
@ApiStatus.Experimental
public abstract class Basis implements IHasSymbol, ISavable, IAutoShutdown {
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
         * 自定义构建 native nnap basis 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new LinkedHashMap<>();
        
        public static final int NONE = -1;
        public static final int COMPAT = 0;
        public static final int BASE = 1;
        public static final int MAX = 2;
        /**
         * 自定义 native nnap basis 需要采用的优化等级，默认为 1（基础优化），
         * 会开启 AVX2 指令集，在大多数现代处理器上能兼容运行
         */
        public static int OPT_LEVEL = OS.envI("JSE_NNAPBASIS_OPT_LEVEL", BASE);
        
        /**
         * 自定义构建 native nnap basis 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_NNAPBASIS"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_NNAPBASIS"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NNAPBASIS", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NNAPBASIS"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /** 重定向 native nnap basis 动态库的路径 */
        public static @Nullable String REDIRECT_NNAPBASIS_LIB = OS.env("JSE_REDIRECT_NNAPBASIS_LIB");
    }
    
    public final static String LIB_DIR = JAR_DIR+"nnap/basis/" + UT.Code.uniqueID(CS.VERSION, NNAP.VERSION, Conf.OPT_LEVEL, Conf.CMAKE_C_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "basis_util.h"
        , "jsex_nnap_basis_Basis.c"
        , "jsex_nnap_basis_Basis.h"
        , "jsex_nnap_basis_SphericalChebyshev.c"
        , "jsex_nnap_basis_SphericalChebyshev.h"
        , "jsex_nnap_basis_Chebyshev.c"
        , "jsex_nnap_basis_Chebyshev.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        // 不直接依赖 nnap
        
        // 先添加 Conf.CMAKE_SETTING，这样保证确定的优先级
        Map<String, String> rCmakeSetting = new LinkedHashMap<>(Conf.CMAKE_SETTING);
        switch(Conf.OPT_LEVEL) {
        case Conf.MAX: {
            rCmakeSetting.put("JSE_OPT_MAX",    "ON");
            rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
            rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
            break;
        }
        case Conf.BASE: {
            rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
            rCmakeSetting.put("JSE_OPT_BASE",   "ON");
            rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
            break;
        }
        case Conf.COMPAT: {
            rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
            rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
            rCmakeSetting.put("JSE_OPT_COMPAT", "ON");
            break;
        }
        case Conf.NONE: {
            rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
            rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
            rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
            break;
        }}
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("nnapbasis", "NATIVE_NNAP_BASIS", LIB_DIR, rCmakeSetting)
            .setSrc("nnap/basis", SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS)
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setRedirectLibPath(Conf.REDIRECT_NNAPBASIS_LIB)
            .get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    @ApiStatus.Internal
    public static void forceDot(IDataShell<double[]> aXGrad, IDataShell<double[]> aFpPx, IDataShell<double[]> aFpPy, IDataShell<double[]> aFpPz,
                                IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, int aNN) {
        int tLength = aXGrad.internalDataSize();
        int tShift = aXGrad.internalDataShift();
        forceDot1(aXGrad.internalDataWithLengthCheck(tLength, tShift), tShift, tLength,
                  aFpPx.internalDataWithLengthCheck(tLength*aNN), aFpPy.internalDataWithLengthCheck(tLength*aNN), aFpPz.internalDataWithLengthCheck(tLength*aNN),
                  rFx.internalDataWithLengthCheck(aNN), rFy.internalDataWithLengthCheck(aNN), rFz.internalDataWithLengthCheck(aNN), aNN);
    }
    private static native void forceDot1(double[] aXGrad, int aShift, int aLength, double[] aFpPx, double[] aFpPy, double[] aFpPz, double[] rFx, double[] rFy, double[] rFz, int aNN);
    
    
    /** @return 基组需要的近邻截断半径 */
    public abstract double rcut();
    /** @return 基组的长度 */
    public abstract int size();
    
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return 1;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override public boolean hasSymbol() {return false;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类
     */
    @Override public @Nullable String symbol(int aType) {return null;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override public final @Nullable List<@Nullable String> symbols() {return IHasSymbol.super.symbols();}
    
    private boolean mDead = false;
    /** @return 此基组是否已经关闭 */
    public final boolean isShutdown() {return mDead;}
    @Override public final void shutdown() {
        if (mDead) return;
        mDead = true;
        shutdown_();
    }
    protected void shutdown_() {/**/}
    
    
    @FunctionalInterface public interface IDxyzTypeIterable {void forEachDxyzType(IDxyzTypeDo aDxyzTypeDo);}
    @FunctionalInterface public interface IDxyzTypeDo {void run(double aDx, double aDy, double aDz, int aType);}
    
    private final DoubleList mNlDx = new DoubleList(16), mNlDy = new DoubleList(16), mNlDz = new DoubleList(16);
    private final IntList mNlType = new IntList(16);
    
    private void buildNL_(IDxyzTypeIterable aNL) {
        final int tTypeNum = atomTypeNumber();
        // 缓存情况需要先清空这些
        mNlDx.clear(); mNlDy.clear(); mNlDz.clear();
        mNlType.clear();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            // 现在不再检测距离，因为需要处理合并情况下截断不一致的情况
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            mNlDx.add(dx); mNlDy.add(dy); mNlDz.add(dz);
            mNlType.add(type);
        });
    }
    
    /**
     * 内部使用的计算基组接口，现在统一采用外部预先构造的近邻列表，从而可以避免重复遍历近邻
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param rFp 计算输出的原子描述符向量
     */
    @ApiStatus.Internal
    public abstract void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp);
    /**
     * 通用的计算基组的接口，可以自定义任何近邻列表获取器来实现
     * @param aNL 近邻列表遍历器
     * @param rFp 计算输出的原子描述符向量
     */
    public final void eval(IDxyzTypeIterable aNL, DoubleArrayVector rFp) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        buildNL_(aNL);
        eval_(mNlDx, mNlDy, mNlDz, mNlType, rFp);
    }
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @param rFp 计算输出的原子描述符向量
     */
    public final void eval(final AtomicParameterCalculator aAPC, final int aIdx, final IntUnaryOperator aTypeMap, DoubleArrayVector rFp) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        eval(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        }, rFp);
    }
    public final void eval(AtomicParameterCalculator aAPC, int aIdx, DoubleArrayVector rFp) {eval(aAPC, aIdx, type->type, rFp);}
    
    /**
     * 内部使用的计算基组以及偏导数接口，现在统一采用外部预先构造的近邻列表，从而可以避免重复遍历近邻
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPy 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPz 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，会自动清空旧值并根据近邻列表扩容
     */
    @ApiStatus.Internal
    public abstract void evalPartial_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                                      DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz);
    /**
     * 基组结果对于 {@code xyz} 偏微分的计算结果，主要用于力的计算；会同时计算基组值本身
     * @param aNL 近邻列表遍历器
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPy 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPz 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，会自动清空旧值并根据近邻列表扩容
     */
    public final void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        buildNL_(aNL);
        evalPartial_(mNlDx, mNlDy, mNlDz, mNlType, rFp, rFpPx, rFpPy, rFpPz);
    }
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组偏导数功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPy 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPz 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，会自动清空旧值并根据近邻列表扩容
     */
    public final void evalPartial(AtomicParameterCalculator aAPC, int aIdx, IntUnaryOperator aTypeMap, DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        evalPartial(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        }, rFp, rFpPx, rFpPy, rFpPz);
    }
    public final void evalPartial(AtomicParameterCalculator aAPC, int aIdx, DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        evalPartial(aAPC, aIdx, type->type, rFp, rFpPx, rFpPy, rFpPz);
    }
}
