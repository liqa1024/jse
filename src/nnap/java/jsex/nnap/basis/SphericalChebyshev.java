package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.cache.VectorCache;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.MathEX;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这是 NNAP 中默认使用的原子基组。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * 现在统一通过调用 c 并借助 avx 指令优化来得到最佳的性能
 * <p>
 * References:
 * <a href="https://link.springer.com/article/10.1007/s40843-024-2953-9">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * @author Su Rui, liqa
 */
public class SphericalChebyshev implements IBasis {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(_INIT_FLAG);
        }
    }
    
    private final static boolean _INIT_FLAG;
    static {
        InitHelper.INITIALIZED = true;
        // 确保 BASIS 已经确实初始化
        BASIS.InitHelper.init();
        _INIT_FLAG = false;
    }
    
    final static int[] L3NCOLS = {0, 0, 2, 4, 9}, L3NCOLS_NOCROSS = {0, 0, 1, 1, 2};
    
    public final static int DEFAULT_NMAX = 5;
    public final static int DEFAULT_LMAX = 6;
    public final static int DEFAULT_L3MAX = 0;
    public final static boolean DEFAULT_L3CROSS = true;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_SINGLE = 1, WTYPE_FULL = 2, WTYPE_EXFULL = 3;
    private final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("single", WTYPE_SINGLE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .build();
    
    final int mTypeNum;
    final String @Nullable[] mSymbols;
    final int mNMax, mLMax, mL3Max;
    final boolean mL3Cross;
    final double mRCut;
    final int mWType;
    
    final int mSizeN, mSizeL, mSize;
    final int mLMaxMax, mLMAll;
    
    /** 一些缓存的中间变量，现在统一作为对象存储，对于这种大规模的缓存情况可以进一步提高效率 */
    private final Vector mCnlm;
    private final Vector mCnlmPx, mCnlmPy, mCnlmPz;
    private final Vector mRn, mRnPx, mRnPy, mRnPz, mCheby2;
    private final Vector mY, mYPx, mYPy, mYPz, mYPphi, mYPtheta;
    
    final DoubleList mNlDx = new DoubleList(16), mNlDy = new DoubleList(16), mNlDz = new DoubleList(16);
    final IntList mNlType = new IntList(16);
    final DoubleList mNlY = new DoubleList(1024);
    final DoubleList mNlRn = new DoubleList(128);
    
    SphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aL3Max, boolean aL3Cross, double aRCut, int aWType) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax < 0) throw new IllegalArgumentException("Input nmax MUST be Non-Negative, input: "+aNMax);
        if (aLMax<0 || aLMax>20) throw new IllegalArgumentException("Input lmax MUST be in [0, 20], input: "+aLMax);
        if (aL3Max<0 || aL3Max>4) throw new IllegalArgumentException("Input l3max MUST be in [0, 4], input: "+aL3Max);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 1, 2, 3}, input: "+ aWType);
        mSymbols = aSymbols;
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mLMax = aLMax;
        mL3Max = aL3Max;
        mL3Cross = aL3Cross;
        mRCut = aRCut;
        mWType = aWType;
        
        switch(mWType) {
        case WTYPE_EXFULL: {
            mSizeN = mTypeNum>1 ? (mTypeNum+1)*(mNMax+1) : (mNMax+1);
            break;
        }
        case WTYPE_FULL: {
            mSizeN = mTypeNum*(mNMax+1);
            break;
        }
        case WTYPE_NONE:
        case WTYPE_SINGLE: {
            mSizeN = mNMax+1;
            break;
        }
        case WTYPE_DEFAULT: {
            mSizeN = mTypeNum>1 ? (mNMax+mNMax+2) : (mNMax+1);
            break;
        }
        default: {
            throw new IllegalStateException();
        }}
        mSizeL = mLMax+1 + (mL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[mL3Max];
        mSize = mSizeN*mSizeL;
        mLMaxMax = Math.max(mLMax, mL3Max);
        mLMAll = (mLMaxMax+1)*(mLMaxMax+1);
        
        mCnlm = VectorCache.getVec(mSizeN*mLMAll);
        mCnlmPx = VectorCache.getVec(mLMAll);
        mCnlmPy = VectorCache.getVec(mLMAll);
        mCnlmPz = VectorCache.getVec(mLMAll);
        
        mRn = VectorCache.getVec(mNMax+1);
        mRnPx = VectorCache.getVec(mNMax+1);
        mRnPy = VectorCache.getVec(mNMax+1);
        mRnPz = VectorCache.getVec(mNMax+1);
        mCheby2 = VectorCache.getVec(mNMax);
        
        mY = VectorCache.getVec(mLMAll);
        mYPx = VectorCache.getVec(mLMAll);
        mYPy = VectorCache.getVec(mLMAll);
        mYPz = VectorCache.getVec(mLMAll);
        mYPphi = VectorCache.getVec(mLMAll);
        mYPtheta = VectorCache.getVec(mLMAll);
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(String @NotNull[] aSymbols, int aNMax, int aLMax, double aRCut) {
        this(aSymbols, aSymbols.length, aNMax, aLMax, DEFAULT_L3MAX, DEFAULT_L3CROSS, aRCut, WTYPE_DEFAULT);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
        this(null, aTypeNum, aNMax, aLMax, DEFAULT_L3MAX, DEFAULT_L3CROSS, aRCut, WTYPE_DEFAULT);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l3cross", mL3Cross);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
    }
    
    private static int getWType_(@Nullable Object aType) {
        if (aType == null) return WTYPE_DEFAULT;
        if (aType instanceof Number) return ((Number)aType).intValue();
        return ALL_WTYPE.get(aType.toString());
    }
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        return new SphericalChebyshev(
            aSymbols, aSymbols.length,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            getWType_(UT.Code.get(aMap, "wtype"))
        );
    }
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(int aTypeNum, Map aMap) {
        return new SphericalChebyshev(
            null, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            getWType_(UT.Code.get(aMap, "wtype"))
        );
    }
    
    
    /** @return {@inheritDoc} */
    @Override public double rcut() {return mRCut;}
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)(lmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)(lmax+1)}
     */
    @Override public int size() {return mSize;}
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return mTypeNum;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    /**
     * {@inheritDoc}
     * @param aType
     * @return {@inheritDoc}
     */
    @Override public @Nullable String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    
    
    private void validYAllSize(int aNN) {
        int tMinSize = aNN * mLMAll;
        int tSize = mNlY.size();
        if (tSize >= tMinSize) return;
        mNlY.addZeros(tMinSize-tSize);
    }
    private void validRnAllSize(int aNN) {
        int tMinSize = aNN * (mNMax+1);
        int tSize = mNlRn.size();
        if (tSize >= tMinSize) return;
        mNlRn.addZeros(tMinSize-tSize);
    }
    
    boolean mDead = false;
    @Override public boolean isShutdown() {return mDead;}
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        VectorCache.returnVec(mCnlm);
        VectorCache.returnVec(mCnlmPx);
        VectorCache.returnVec(mCnlmPy);
        VectorCache.returnVec(mCnlmPz);
        VectorCache.returnVec(mRn);
        VectorCache.returnVec(mRnPx);
        VectorCache.returnVec(mRnPy);
        VectorCache.returnVec(mRnPz);
        VectorCache.returnVec(mCheby2);
        VectorCache.returnVec(mY);
        VectorCache.returnVec(mYPx);
        VectorCache.returnVec(mYPy);
        VectorCache.returnVec(mYPz);
        VectorCache.returnVec(mYPphi);
        VectorCache.returnVec(mYPtheta);
    }
    
    @Override public void eval(IDxyzTypeIterable aNL, Vector rFp) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        
        // 统一缓存近邻列表
        buildNL(aNL);
        final int tNN = mNlDx.size();
        
        // cnlm 需要累加，因此需要事先清空
        mCnlm.fill(0.0);
        
        // 现在直接计算基组
        eval0(tNN, rFp);
    }
    
    @Override public final void evalPartial(IDxyzTypeIterable aNL, Vector rFp, Vector rFpPx, Vector rFpPy, Vector rFpPz) {
        evalPartial(aNL, rFp, rFpPx, rFpPy, rFpPz, null, null, null);
    }
    @Override public void evalPartial(IDxyzTypeIterable aNL, Vector rFp, Vector rFpPx, Vector rFpPy, Vector rFpPz, @Nullable DoubleList rFpPxCross, @Nullable DoubleList rFpPyCross, @Nullable DoubleList rFpPzCross) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        
        // 统一缓存近邻列表
        buildNL(aNL);
        final int tNN = mNlDx.size();
        
        // 确保 Rn Y 的长度
        validYAllSize(tNN);
        validRnAllSize(tNN);
        
        // cnlm 需要累加，因此需要事先清空
        mCnlm.fill(0.0);
        
        // 初始化偏导数相关值
        rFpPx.fill(0.0);
        rFpPy.fill(0.0);
        rFpPz.fill(0.0);
        if (rFpPxCross != null) {
            assert rFpPyCross!=null && rFpPzCross!=null;
            rFpPxCross.clear(); rFpPxCross.addZeros(tNN*mSize);
            rFpPyCross.clear(); rFpPyCross.addZeros(tNN*mSize);
            rFpPzCross.clear(); rFpPzCross.addZeros(tNN*mSize);
        }
        
        // 现在直接计算基组偏导
        evalPartial0(tNN, rFp, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross);
    }
    
    void buildNL(IDxyzTypeIterable aNL) {
        // 缓存情况需要先清空这些
        mNlDx.clear(); mNlDy.clear(); mNlDz.clear();
        mNlType.clear();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= mRCut) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            // 简单缓存近邻列表
            mNlDx.add(dx); mNlDy.add(dy); mNlDz.add(dz);
            mNlType.add(type);
        });
    }
    
    void eval0(int aNN, Vector rFp) {
        BASIS.rangeCheck(mNlDx.size(), aNN);
        BASIS.rangeCheck(mNlDy.size(), aNN);
        BASIS.rangeCheck(mNlDz.size(), aNN);
        BASIS.rangeCheck(mNlType.size(), aNN);
        BASIS.rangeCheck(mRn.size(), mNMax+1);
        BASIS.rangeCheck(mY.size(), mLMAll);
        BASIS.rangeCheck(mCnlm.size(), mSizeN*mLMAll);
        BASIS.rangeCheck(rFp.size(), mSize);
        eval1(mNlDx.internalData(), mNlDy.internalData(), mNlDz.internalData(), mNlType.internalData(), aNN,
              mRn.internalData(), mY.internalData(), mCnlm.internalData(), rFp.internalData(),
              mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL3Cross, mWType);
    }
    private static native void eval1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                     double[] rRn, double[] rY, double[] rCnlm, double[] rFingerPrint,
                                     int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, boolean aL3Cross, int aWType);
    
    void evalPartial0(int aNN, Vector rFp, Vector rFpPx, Vector rFpPy, Vector rFpPz,
                      @Nullable DoubleList rFpPxCross, @Nullable DoubleList rFpPyCross, @Nullable DoubleList rFpPzCross) {
        BASIS.rangeCheck(mNlDx.size(), aNN);
        BASIS.rangeCheck(mNlDy.size(), aNN);
        BASIS.rangeCheck(mNlDz.size(), aNN);
        BASIS.rangeCheck(mNlType.size(), aNN);
        BASIS.rangeCheck(mNlRn.size(), aNN*(mNMax+1));
        BASIS.rangeCheck(mRnPx.size(), mNMax+1);
        BASIS.rangeCheck(mRnPy.size(), mNMax+1);
        BASIS.rangeCheck(mRnPz.size(), mNMax+1);
        BASIS.rangeCheck(mCheby2.size(), mNMax);
        BASIS.rangeCheck(mNlY.size(), aNN*mLMAll);
        BASIS.rangeCheck(mYPtheta.size(), mLMAll);
        BASIS.rangeCheck(mYPphi.size(), mLMAll);
        BASIS.rangeCheck(mYPx.size(), mLMAll);
        BASIS.rangeCheck(mYPy.size(), mLMAll);
        BASIS.rangeCheck(mYPz.size(), mLMAll);
        BASIS.rangeCheck(mCnlm.size(), mSizeN*mLMAll);
        BASIS.rangeCheck(mCnlmPx.size(), mLMAll);
        BASIS.rangeCheck(mCnlmPy.size(), mLMAll);
        BASIS.rangeCheck(mCnlmPz.size(), mLMAll);
        BASIS.rangeCheck(rFp.size(), mSize);
        BASIS.rangeCheck(rFpPx.size(), mSize);
        BASIS.rangeCheck(rFpPy.size(), mSize);
        BASIS.rangeCheck(rFpPz.size(), mSize);
        if (rFpPxCross != null) BASIS.rangeCheck(rFpPxCross.size(), aNN*mSize);
        if (rFpPyCross != null) BASIS.rangeCheck(rFpPyCross.size(), aNN*mSize);
        if (rFpPzCross != null) BASIS.rangeCheck(rFpPzCross.size(), aNN*mSize);
        evalPartial1(mNlDx.internalData(), mNlDy.internalData(), mNlDz.internalData(), mNlType.internalData(), aNN,
                     mNlRn.internalData(), mRnPx.internalData(), mRnPy.internalData(), mRnPz.internalData(), mCheby2.internalData(),
                     mNlY.internalData(), mYPtheta.internalData(), mYPphi.internalData(), mYPx.internalData(), mYPy.internalData(), mYPz.internalData(),
                     mCnlm.internalData(), mCnlmPx.internalData(), mCnlmPy.internalData(), mCnlmPz.internalData(),
                     rFp.internalData(), rFpPx.internalData(), rFpPy.internalData(), rFpPz.internalData(),
                     rFpPxCross!=null?rFpPxCross.internalData():null,
                     rFpPyCross!=null?rFpPyCross.internalData():null,
                     rFpPzCross!=null?rFpPzCross.internalData():null,
                     mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL3Cross, mWType);
    }
    private static native void evalPartial1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                            double[] rNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, double[] rCheby2,
                                            double[] rNlY, double[] rYPtheta, double[] rYPphi, double[] rYPx, double[] rYPy, double[] rYPz,
                                            double[] rCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz,
                                            double[] rFingerPrint, double[] rFingerPrintPx, double[] rFingerPrintPy, double[] rFingerPrintPz,
                                            double @Nullable[] rFingerPrintPxCross, double @Nullable[] rFingerPrintPyCross, double @Nullable[] rFingerPrintPzCross,
                                            int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, boolean aL3Cross, int aWType);
}
