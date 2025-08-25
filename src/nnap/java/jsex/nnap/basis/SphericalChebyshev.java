package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IntArrayVector;
import jse.math.vector.Vectors;
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
public class SphericalChebyshev extends WTypeBasis {
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
    private final IDataShell<double[]> mCnlm, mGradCnlm;
    private final IDataShell<double[]> mCnlmPx, mCnlmPy, mCnlmPz;
    private final IDataShell<double[]> mRnPx, mRnPy, mRnPz, mCheby2;
    private final IDataShell<double[]> mYPx, mYPy, mYPz, mYPphi, mYPtheta;
    
    final DoubleList mNlY = new DoubleList(1024);
    final DoubleList mNlRn = new DoubleList(128);
    
    SphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, double aRCut, int aWType) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax<0 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [0, 20], input: "+aNMax);
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
        
        mCnlm = Vectors.zeros(mSizeN*mLMAll);
        mGradCnlm = Vectors.zeros(mSizeN*mLMAll);
        mCnlmPx = Vectors.zeros(mLMAll);
        mCnlmPy = Vectors.zeros(mLMAll);
        mCnlmPz = Vectors.zeros(mLMAll);
        
        mRnPx = Vectors.zeros(mNMax+1);
        mRnPy = Vectors.zeros(mNMax+1);
        mRnPz = Vectors.zeros(mNMax+1);
        mCheby2 = Vectors.zeros(mNMax);
        
        mYPx = Vectors.zeros(mLMAll);
        mYPy = Vectors.zeros(mLMAll);
        mYPz = Vectors.zeros(mLMAll);
        mYPphi = Vectors.zeros(mLMAll);
        mYPtheta = Vectors.zeros(mLMAll);
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
    
    @Override
    protected void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, @Nullable IntArrayVector rFpGradNlSize, boolean aBufferNl) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        final int tNN = aNlDx.size();
        // 确保 Rn Y 的长度
        validSize_(mNlY, tNN*mLMAll);
        validSize_(mNlRn, tNN*(mNMax+1));
        
        // 现在直接计算基组
        eval0(aNlDx, aNlDy, aNlDz, aNlType, rFp, rFpGradNlSize, aBufferNl);
    }
    
    @Override
    protected void evalGrad_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                             IntArrayVector aFpGradNlSize, IntArrayVector rFpGradNlIndex, IntArrayVector rFpGradFpIndex,
                             DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算基组偏导
        evalGrad0(aNlDx, aNlDy, aNlDz, aNlType, rFpGradNlIndex, rFpGradFpIndex, rFpPx, rFpPy, rFpPz);
    }
    
    @Override
    protected void evalGradAndForceDotAccumulate_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                                                  DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算力
        evalGradAndForceDot0(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, rFx, rFy, rFz);
    }
    
    
    void eval0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp, @Nullable IDataShell<int[]> rFpNlSize, boolean aBufferNl) {
        int tNN = aNlDx.internalDataSize();
        eval1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
              mNlRn.internalDataWithLengthCheck(tNN*(mNMax+1), 0), mNlY.internalDataWithLengthCheck(tNN*mLMAll, 0), mCnlm.internalDataWithLengthCheck(mSizeN*mLMAll, 0),
              rFp.internalDataWithLengthCheck(mSize), rFp.internalDataShift(),
              rFpNlSize==null?null:rFpNlSize.internalDataWithLengthCheck(mSize), rFpNlSize==null?0:rFpNlSize.internalDataShift(),
              aBufferNl, mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mWType);
    }
    private static native void eval1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                     double[] rNlRn, double[] rNlY, double[] rCnlm, double[] rFp, int aShiftFp, int @Nullable[] rFpNlSize, int aShiftFpNlSize,
                                     boolean aBufferNl, int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, int aWType);
    
    void evalGrad0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                   IDataShell<int[]> rFpGradNlIndex, IDataShell<int[]> rFpGradFpIndex, IDataShell<double[]> rFpPx, IDataShell<double[]> rFpPy, IDataShell<double[]> rFpPz) {
        int tNN = aNlDx.internalDataSize();
        int tSizeAll = rFpGradNlIndex.internalDataSize();
        evalGrad1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  mNlRn.internalDataWithLengthCheck(tNN*(mNMax+1), 0), mRnPx.internalDataWithLengthCheck(mNMax+1, 0), mRnPy.internalDataWithLengthCheck(mNMax+1, 0), mRnPz.internalDataWithLengthCheck(mNMax+1, 0), mCheby2.internalDataWithLengthCheck(mNMax, 0),
                  mNlY.internalDataWithLengthCheck(tNN*mLMAll, 0), mYPtheta.internalDataWithLengthCheck(mLMAll, 0), mYPphi.internalDataWithLengthCheck(mLMAll, 0),
                  mYPx.internalDataWithLengthCheck(mLMAll, 0), mYPy.internalDataWithLengthCheck(mLMAll, 0), mYPz.internalDataWithLengthCheck(mLMAll, 0),
                  mCnlm.internalDataWithLengthCheck(mSizeN*mLMAll, 0), mCnlmPx.internalDataWithLengthCheck(mLMAll, 0), mCnlmPy.internalDataWithLengthCheck(mLMAll, 0), mCnlmPz.internalDataWithLengthCheck(mLMAll, 0),
                  rFpGradNlIndex.internalDataWithLengthCheck(tSizeAll), rFpGradNlIndex.internalDataShift(), rFpGradFpIndex.internalDataWithLengthCheck(tSizeAll), rFpGradFpIndex.internalDataShift(),
                  rFpPx.internalDataWithLengthCheck(tSizeAll), rFpPx.internalDataShift(), rFpPy.internalDataWithLengthCheck(tSizeAll), rFpPy.internalDataShift(), rFpPz.internalDataWithLengthCheck(tSizeAll), rFpPz.internalDataShift(),
                  mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mWType);
    }
    private static native void evalGrad1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, double[] rCheby2,
                                         double[] aNlY, double[] rYPtheta, double[] rYPphi, double[] rYPx, double[] rYPy, double[] rYPz,
                                         double[] aCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz,
                                         int[] rFpGradNlIndex, int aShiftFpGradNlIndex, int[] rFpGradFpIndex, int aShiftFpGradFpIndex,
                                         double[] rFpPx, int aShiftFpPx, double[] rFpPy, int aShiftFpPy, double[] rFpPz, int aShiftFpPz,
                                         int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, int aWType);
    
    void evalGradAndForceDot0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                              IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz) {
        int tNN = aNlDx.internalDataSize();
        evalGradAndForceDot1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                             mNlRn.internalDataWithLengthCheck(tNN*(mNMax+1), 0), mRnPx.internalDataWithLengthCheck(mNMax+1, 0), mRnPy.internalDataWithLengthCheck(mNMax+1, 0), mRnPz.internalDataWithLengthCheck(mNMax+1, 0), mCheby2.internalDataWithLengthCheck(mNMax, 0),
                             mNlY.internalDataWithLengthCheck(tNN*mLMAll, 0), mYPtheta.internalDataWithLengthCheck(mLMAll, 0), mYPphi.internalDataWithLengthCheck(mLMAll, 0),
                             mYPx.internalDataWithLengthCheck(mLMAll, 0), mYPy.internalDataWithLengthCheck(mLMAll, 0), mYPz.internalDataWithLengthCheck(mLMAll, 0),
                             mCnlm.internalDataWithLengthCheck(mSizeN*mLMAll, 0), mGradCnlm.internalDataWithLengthCheck(mSizeN*mLMAll, 0),
                             aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                             mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mWType);
    }
    private static native void evalGradAndForceDot1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                                    double[] aNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, double[] rCheby2,
                                                    double[] aNlY, double[] rYPtheta, double[] rYPphi, double[] rYPx, double[] rYPy, double[] rYPz,
                                                    double[] aCnlm, double[] rGradCnlm,
                                                    double[] aNNGrad, int aShiftFp, double[] rFx, double[] rFy, double[] rFz,
                                                    int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, int aWType);
    
    
    @Override @Deprecated
    protected void evalGradWithShift_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                                      int aShiftFp, int aRestFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算基组偏导
        evalGradWithShift0(aNlDx, aNlDy, aNlDz, aNlType, aShiftFp, aRestFp, rFpPx, rFpPy, rFpPz);
    }
    @Deprecated
    void evalGradWithShift0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                            int aShiftFp, int aRestFp, IDataShell<double[]> rFpPx, IDataShell<double[]> rFpPy, IDataShell<double[]> rFpPz) {
        int tNN = aNlDx.internalDataSize();
        int tSizeTot = aShiftFp + aRestFp + mSize;
        evalGradWithShift1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                           mNlRn.internalDataWithLengthCheck(tNN*(mNMax+1), 0), mRnPx.internalDataWithLengthCheck(mNMax+1, 0), mRnPy.internalDataWithLengthCheck(mNMax+1, 0), mRnPz.internalDataWithLengthCheck(mNMax+1, 0), mCheby2.internalDataWithLengthCheck(mNMax, 0),
                           mNlY.internalDataWithLengthCheck(tNN*mLMAll, 0), mYPtheta.internalDataWithLengthCheck(mLMAll, 0), mYPphi.internalDataWithLengthCheck(mLMAll, 0),
                           mYPx.internalDataWithLengthCheck(mLMAll, 0), mYPy.internalDataWithLengthCheck(mLMAll, 0), mYPz.internalDataWithLengthCheck(mLMAll, 0),
                           mCnlm.internalDataWithLengthCheck(mSizeN*mLMAll, 0), mCnlmPx.internalDataWithLengthCheck(mLMAll, 0), mCnlmPy.internalDataWithLengthCheck(mLMAll, 0), mCnlmPz.internalDataWithLengthCheck(mLMAll, 0),
                           aShiftFp, aRestFp, rFpPx.internalDataWithLengthCheck(tNN*tSizeTot, 0), rFpPy.internalDataWithLengthCheck(tNN*tSizeTot, 0), rFpPz.internalDataWithLengthCheck(tNN*tSizeTot, 0),
                           mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mWType);
    }
    @Deprecated
    private static native void evalGradWithShift1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                                  double[] aNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, double[] rCheby2,
                                                  double[] aNlY, double[] rYPtheta, double[] rYPphi, double[] rYPx, double[] rYPy, double[] rYPz,
                                                  double[] aCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz,
                                                  int aShiftFp, int aRestFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                                  int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, int aWType);
}
