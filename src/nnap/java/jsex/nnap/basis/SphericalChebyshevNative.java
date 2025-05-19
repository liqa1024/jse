package jsex.nnap.basis;

import com.google.common.collect.Lists;
import jse.cache.VectorCache;
import jse.clib.JNIUtil;
import jse.code.CS;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.ArrayLists;
import jse.code.collection.DoubleList;
import jse.code.timer.AccumulatedTimer;
import jse.math.MathEX;
import jse.math.vector.ShiftVector;
import jse.math.vector.Vector;
import jsex.nnap.NNAP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static jse.code.OS.JAR_DIR;

/**
 * 使用 JNI 来调用 c 来加速部分运算的 {@link SphericalChebyshev}
 *
 * @see SphericalChebyshev
 * @author liqa
 */
public class SphericalChebyshevNative extends SphericalChebyshev {
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
          "jsex_nnap_basis_SphericalChebyshevNative.c"
        , "jsex_nnap_basis_SphericalChebyshevNative.h"
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
            .get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    
    public SphericalChebyshevNative(String @NotNull[] aSymbols, int aNMax, int aLMax, int aL3Max, boolean aL3Cross, double aRCut) {super(aSymbols, aNMax, aLMax, aL3Max, aL3Cross, aRCut);}
    public SphericalChebyshevNative(String @NotNull[] aSymbols, int aNMax, int aLMax, int aL3Max, double aRCut) {super(aSymbols, aNMax, aLMax, aL3Max, aRCut);}
    public SphericalChebyshevNative(String @NotNull[] aSymbols, int aNMax, int aLMax, double aRCut) {super(aSymbols, aNMax, aLMax, aRCut);}
    public SphericalChebyshevNative(int aTypeNum, int aNMax, int aLMax, int aL3Max, boolean aL3Cross, double aRCut) {super(aTypeNum, aNMax, aLMax, aL3Max, aL3Cross, aRCut);}
    public SphericalChebyshevNative(int aTypeNum, int aNMax, int aLMax, int aL3Max, double aRCut) {super(aTypeNum, aNMax, aLMax, aL3Max, aRCut);}
    public SphericalChebyshevNative(int aTypeNum, int aNMax, int aLMax, double aRCut) {super(aTypeNum, aNMax, aLMax, aRCut);}
    
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshevNative load(String @NotNull[] aSymbols, Map aMap) {
        return new SphericalChebyshevNative(
            aSymbols,
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue()
        );
    }
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshevNative load(int aTypeNum, Map aMap) {
        return new SphericalChebyshevNative(
            aTypeNum,
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue()
        );
    }
    
    
    public static final AccumulatedTimer sNativeTimer = new AccumulatedTimer();
    
    @Override public void eval(IDxyzTypeIterable aNL, Vector rFp) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        // 统一缓存近邻列表
        buildNL(aNL);
        final int tNN = mDxAll.size();
        // 现在直接计算基组
        eval0(mDxAll.internalData(), mDyAll.internalData(), mDzAll.internalData(), mTypeAll.internalData(), tNN,
              bufRn(false).internalData(), bufY(false).internalData(), bufCnlm(true).internalData(), rFp.internalData(),
              mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL3Cross);
    }
    
    @Override public void evalPartial(IDxyzTypeIterable aNL, Vector rFp, Vector rFpPx, Vector rFpPy, Vector rFpPz, @Nullable DoubleList rFpPxCross, @Nullable DoubleList rFpPyCross, @Nullable DoubleList rFpPzCross) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        
        final int tSizeN = sizeN();
        final int tSizeL = sizeL();
        final int tSizeFP = tSizeN*tSizeL;
        // 统一缓存近邻列表
        buildNL(aNL);
        final int tNN = mDxAll.size();
        
        // 确保 Rn Y 的长度
        int tMinSize = tNN * (mNMax+1);
        mRnAll.ensureCapacity(tMinSize);
        while (mRnAll.size()<tMinSize) mRnAll.add(0.0);
        int tLMax = Math.max(mLMax, mL3Max);
        int tLMAll = (tLMax+1)*(tLMax+1);
        tMinSize = tNN * tLMAll;
        mYAll.ensureCapacity(tMinSize);
        while (mYAll.size()<tMinSize) mYAll.add(0.0);
        
        // 初始化偏导数相关值
        rFpPx.fill(0.0);
        rFpPy.fill(0.0);
        rFpPz.fill(0.0);
        if (rFpPxCross != null) {
            assert rFpPyCross!=null && rFpPzCross!=null;
            rFpPxCross.clear(); rFpPxCross.addZeros(tNN*tSizeFP);
            rFpPyCross.clear(); rFpPyCross.addZeros(tNN*tSizeFP);
            rFpPzCross.clear(); rFpPzCross.addZeros(tNN*tSizeFP);
        }
        
        // 现在直接计算基组偏导
        evalPartial0(mDxAll.internalData(), mDyAll.internalData(), mDzAll.internalData(), mTypeAll.internalData(), tNN,
                     mRnAll.internalData(), bufRnPx(false).internalData(), bufRnPy(false).internalData(), bufRnPz(false).internalData(),
                     mYAll.internalData(), bufYPtheta(false).internalData(), bufYPphi(false).internalData(), bufYPx(false).internalData(), bufYPy(false).internalData(), bufYPz(false).internalData(),
                     bufCnlm(true).internalData(), bufCnlmPx(false).internalData(), bufCnlmPy(false).internalData(), bufCnlmPz(false).internalData(),
                     rFp.internalData(), rFpPx.internalData(), rFpPy.internalData(), rFpPz.internalData(),
                     rFpPxCross!=null?rFpPxCross.internalData():null,
                     rFpPyCross!=null?rFpPyCross.internalData():null,
                     rFpPzCross!=null?rFpPzCross.internalData():null,
                     mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL3Cross);
    }
    
    void buildNL(IDxyzTypeIterable aNL) {
        // 缓存情况需要先清空这些
        mDxAll.clear(); mDyAll.clear(); mDzAll.clear();
        mTypeAll.clear();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= mRCut) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            // 简单缓存近邻列表
            mDxAll.add(dx); mDyAll.add(dy); mDzAll.add(dz);
            mTypeAll.add(type);
        });
    }
    
    static void eval0(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                      double[] rRn, double[] rY, double[] rCnlm, double[] rFingerPrint,
                      int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, boolean aL3Cross) {
        if (aL3Max > 4) throw new IllegalArgumentException("l3max > 4 for native SphericalChebyshev");
        final int tSizeN = aTypeNum>1 ? aNMax+aNMax+2 : aNMax+1;
        final int tSizeL = aLMax+1 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
        final int tLMax = Math.max(aLMax, aL3Max);
        final int tLMAll = (tLMax+1)*(tLMax+1);
        if (tLMax > 20) throw new IllegalArgumentException("lmax > 20 for native SphericalChebyshev");
        rangeCheck(aNlDx.length, aNN);
        rangeCheck(aNlDy.length, aNN);
        rangeCheck(aNlDz.length, aNN);
        rangeCheck(aNlType.length, aNN);
        rangeCheck(rRn.length, aNMax+1);
        rangeCheck(rY.length, tLMAll);
        rangeCheck(rCnlm.length, tSizeN*tLMAll);
        rangeCheck(rFingerPrint.length, tSizeN*tSizeL);
        sNativeTimer.from();
        eval1(aNlDx, aNlDy, aNlDz, aNlType, aNN,
                 rRn, rY, rCnlm, rFingerPrint,
                 aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL3Cross);
        sNativeTimer.to();
    }
    private static native void eval1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                     double[] rRn, double[] rY, double[] rCnlm, double[] rFingerPrint,
                                     int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, boolean aL3Cross);
    
    static void evalPartial0(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                             double[] rNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz,
                             double[] rNlY, double[] rYPtheta, double[] rYPphi, double[] rYPx, double[] rYPy, double[] rYPz,
                             double[] rCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz,
                             double[] rFingerPrint, double[] rFingerPrintPx, double[] rFingerPrintPy, double[] rFingerPrintPz,
                             double @Nullable[] rFingerPrintPxCross, double @Nullable[] rFingerPrintPyCross, double @Nullable[] rFingerPrintPzCross,
                             int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, boolean aL3Cross) {
        if (aL3Max > 4) throw new IllegalArgumentException("l3max > 4 for native SphericalChebyshev");
        final int tSizeN = aTypeNum>1 ? aNMax+aNMax+2 : aNMax+1;
        final int tSizeL = aLMax+1 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
        final int tLMax = Math.max(aLMax, aL3Max);
        final int tLMAll = (tLMax+1)*(tLMax+1);
        if (tLMax > 20) throw new IllegalArgumentException("lmax > 20 for native SphericalChebyshev");
        rangeCheck(aNlDx.length, aNN);
        rangeCheck(aNlDy.length, aNN);
        rangeCheck(aNlDz.length, aNN);
        rangeCheck(aNlType.length, aNN);
        rangeCheck(rNlRn.length, aNN*(aNMax+1));
        rangeCheck(rRnPx.length, aNMax+1);
        rangeCheck(rRnPy.length, aNMax+1);
        rangeCheck(rRnPz.length, aNMax+1);
        rangeCheck(rNlY.length, aNN*tLMAll);
        rangeCheck(rYPtheta.length, tLMAll);
        rangeCheck(rYPphi.length, tLMAll);
        rangeCheck(rYPx.length, tLMAll);
        rangeCheck(rYPy.length, tLMAll);
        rangeCheck(rYPz.length, tLMAll);
        rangeCheck(rCnlm.length, tSizeN*tLMAll);
        rangeCheck(rCnlmPx.length, tLMAll);
        rangeCheck(rCnlmPy.length, tLMAll);
        rangeCheck(rCnlmPz.length, tLMAll);
        rangeCheck(rFingerPrint.length, tSizeN*tSizeL);
        rangeCheck(rFingerPrintPx.length, tSizeN*tSizeL);
        rangeCheck(rFingerPrintPy.length, tSizeN*tSizeL);
        rangeCheck(rFingerPrintPz.length, tSizeN*tSizeL);
        if (rFingerPrintPxCross != null) rangeCheck(rFingerPrintPxCross.length, aNN*tSizeN*tSizeL);
        if (rFingerPrintPyCross != null) rangeCheck(rFingerPrintPyCross.length, aNN*tSizeN*tSizeL);
        if (rFingerPrintPzCross != null) rangeCheck(rFingerPrintPzCross.length, aNN*tSizeN*tSizeL);
        sNativeTimer.from();
        evalPartial1(aNlDx, aNlDy, aNlDz, aNlType, aNN,
                     rNlRn, rRnPx, rRnPy, rRnPz,
                     rNlY, rYPtheta, rYPphi, rYPx, rYPy, rYPz,
                     rCnlm, rCnlmPx, rCnlmPy, rCnlmPz,
                     rFingerPrint, rFingerPrintPx, rFingerPrintPy, rFingerPrintPz,
                     rFingerPrintPxCross, rFingerPrintPyCross, rFingerPrintPzCross,
                     aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL3Cross);
        sNativeTimer.to();
    }
    private static native void evalPartial1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                            double[] rNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz,
                                            double[] rNlY, double[] rYPtheta, double[] rYPphi, double[] rYPx, double[] rYPy, double[] rYPz,
                                            double[] rCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz,
                                            double[] rFingerPrint, double[] rFingerPrintPx, double[] rFingerPrintPy, double[] rFingerPrintPz,
                                            double @Nullable[] rFingerPrintPxCross, double @Nullable[] rFingerPrintPyCross, double @Nullable[] rFingerPrintPzCross,
                                            int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, boolean aL3Cross);
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
