package jsex.nnap.basis;

import jse.cache.VectorCache;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.vector.DoubleArrayVector;
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
public class SphericalChebyshev extends NNAPWTypeBasis {
    final static int[] L3NCOLS = {0, 0, 2, 4, 9}, L3NCOLS_NOCROSS = {0, 0, 1, 1, 2};
    
    public final static int DEFAULT_NMAX = 5;
    public final static int DEFAULT_LMAX = 6;
    public final static int DEFAULT_L3MAX = 0;
    public final static boolean DEFAULT_NORADIAL = false;
    public final static boolean DEFAULT_L3CROSS = true;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    final int mTypeNum;
    final String @Nullable[] mSymbols;
    final int mNMax, mLMax, mL3Max;
    final boolean mNoRadial, mL3Cross;
    final double mRCut;
    final int mWType;
    
    final int mSizeN, mSizeL, mSize;
    final int mLMaxMax, mLMAll;
    
    /** 一些缓存的中间变量，现在统一作为对象存储，对于这种大规模的缓存情况可以进一步提高效率 */
    private final Vector mCnlm;
    private final Vector mCnlmPx, mCnlmPy, mCnlmPz;
    private final Vector mRn, mRnPx, mRnPy, mRnPz, mCheby2;
    private final Vector mY, mYPx, mYPy, mYPz, mYPphi, mYPtheta;
    
    final DoubleList mNlY = new DoubleList(1024);
    final DoubleList mNlRn = new DoubleList(128);
    
    SphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, double aRCut, int aWType) {
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
        mNoRadial = aNoRadial;
        mL3Cross = aL3Cross;
        mRCut = aRCut;
        mWType = aWType;
        
        mSizeN = sizeN_(mNMax, mTypeNum, mWType);
        mSizeL = (mNoRadial?mLMax:(mLMax+1)) + (mL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[mL3Max];
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
        this(aSymbols, aSymbols.length, aNMax, aLMax, DEFAULT_NORADIAL, DEFAULT_L3MAX, DEFAULT_L3CROSS, aRCut, WTYPE_DEFAULT);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
        this(null, aTypeNum, aNMax, aLMax, DEFAULT_NORADIAL, DEFAULT_L3MAX, DEFAULT_L3CROSS, aRCut, WTYPE_DEFAULT);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("noradial", mNoRadial);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l3cross", mL3Cross);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
    }
    
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        return new SphericalChebyshev(
            aSymbols, aSymbols.length,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_NORADIAL, "noradial"),
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
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_NORADIAL, "noradial"),
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
    
    @Override protected void shutdown_() {
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
    
    @Override
    public void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        
        
        // 现在直接计算基组
        eval0(aNlDx, aNlDy, aNlDz, aNlType, rFp);
    }
    
    @Override
    public void evalPartial_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                             DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        final int tNN = aNlDx.size();
        // 确保 Rn Y 的长度
        validSize_(mNlY, tNN*mLMAll);
        validSize_(mNlRn, tNN*(mNMax+1));
        // 初始化偏导数相关值
        int tSizeAll = rFp.size() + rFp.internalDataShift();
        validSize_(rFpPx, tNN*tSizeAll);
        validSize_(rFpPy, tNN*tSizeAll);
        validSize_(rFpPz, tNN*tSizeAll);
        
        // 现在直接计算基组偏导
        evalPartial0(aNlDx, aNlDy, aNlDz, aNlType, rFp, rFpPx, rFpPy, rFpPz);
    }
    
    void eval0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp) {
        int tNN = aNlDx.internalDataSize();
        int tShiftFp = rFp.internalDataShift();
        eval1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
              mRn.internalDataWithLengthCheck(mNMax+1, 0), mY.internalDataWithLengthCheck(mLMAll, 0), mCnlm.internalDataWithLengthCheck(mSizeN*mLMAll, 0), rFp.internalDataWithLengthCheck(mSize, tShiftFp), tShiftFp,
              mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mWType);
    }
    private static native void eval1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                     double[] rRn, double[] rY, double[] rCnlm, double[] rFp, int aShiftFp,
                                     int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, int aWType);
    
    void evalPartial0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                      IDataShell<double[]> rFp, IDataShell<double[]> rFpPx, IDataShell<double[]> rFpPy, IDataShell<double[]> rFpPz) {
        int tNN = aNlDx.internalDataSize();
        int tShiftFp = rFp.internalDataShift();
        int tSizeFp = rFp.internalDataSize();
        evalPartial1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                     mNlRn.internalDataWithLengthCheck(tNN*(mNMax+1), 0), mRnPx.internalDataWithLengthCheck(mNMax+1, 0), mRnPy.internalDataWithLengthCheck(mNMax+1, 0), mRnPz.internalDataWithLengthCheck(mNMax+1, 0), mCheby2.internalDataWithLengthCheck(mNMax, 0),
                     mNlY.internalDataWithLengthCheck(tNN*mLMAll, 0), mYPtheta.internalDataWithLengthCheck(mLMAll, 0), mYPphi.internalDataWithLengthCheck(mLMAll, 0),
                     mYPx.internalDataWithLengthCheck(mLMAll, 0), mYPy.internalDataWithLengthCheck(mLMAll, 0), mYPz.internalDataWithLengthCheck(mLMAll, 0),
                     mCnlm.internalDataWithLengthCheck(mSizeN*mLMAll, 0), mCnlmPx.internalDataWithLengthCheck(mLMAll, 0), mCnlmPy.internalDataWithLengthCheck(mLMAll, 0), mCnlmPz.internalDataWithLengthCheck(mLMAll, 0),
                     rFp.internalDataWithLengthCheck(mSize, tShiftFp), tSizeFp, tShiftFp,
                     rFpPx.internalDataWithLengthCheck(tNN*(tSizeFp+tShiftFp), 0), rFpPy.internalDataWithLengthCheck(tNN*(tSizeFp+tShiftFp), 0), rFpPz.internalDataWithLengthCheck(tNN*(tSizeFp+tShiftFp), 0),
                     mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mWType);
    }
    private static native void evalPartial1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                            double[] rNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, double[] rCheby2,
                                            double[] rNlY, double[] rYPtheta, double[] rYPphi, double[] rYPx, double[] rYPy, double[] rYPz,
                                            double[] rCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz,
                                            double[] rFp, int aSizeFp, int aShiftFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                            int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, int aWType);
}
