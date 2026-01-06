package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 在 {@link SphericalChebyshev} 基础上引入多重截断半径函数，
 * 作为 NNAP 下一代基组实现。
 * <p>
 * 去除了大部分 {@link SphericalChebyshev} 的参数选择，只保留最优的选择从而保证代码简洁。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
@ApiStatus.Experimental
public class MultiSphericalChebyshev extends MergeableBasis {
    final int mTypeNum;
    final String @Nullable[] mSymbols;
    
    final int mNMax;
    final int mLMax, mL3Max, mL4Max;
    final double mRCutMax;
    final Vector mRCutsL, mRCutsR;
    final int mRCutsSize;
    
    final int mSizeL;
    final int mLMAll;
    final int mSize;
    
    final int mEquFuseInDim;
    
    final Vector mRFuncScale;
    
    MultiSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aL3Max, int aL4Max, Vector aRCutsL, Vector aRCutsR, Vector aRFuncScale) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax<1 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [1, 20], input: "+aNMax);
        if (aLMax<0 || aLMax>12) throw new IllegalArgumentException("Input lmax MUST be in [0, 12], input: "+aLMax);
        if (aL3Max<0 || aL3Max>6) throw new IllegalArgumentException("Input l3max MUST be in [0, 6], input: "+aL3Max);
        if (aL4Max<0 || aL4Max>3) throw new IllegalArgumentException("Input l4max MUST be in [0, 3], input: "+aL3Max);
        if (aL3Max > aLMax) throw new IllegalArgumentException("Input l3max MUST be <= lmax, input: "+aL3Max+" vs "+aLMax);
        if (aL4Max > aLMax) throw new IllegalArgumentException("Input l4max MUST be <= lmax, input: "+aL4Max+" vs "+aLMax);
        
        mTypeNum = aTypeNum;
        mSymbols = aSymbols;
        mNMax = aNMax;
        mLMax = aLMax;
        mL3Max = aL3Max;
        mL4Max = aL4Max;
        mLMAll = (mLMax+1)*(mLMax+1);
        mSizeL = mLMax+1 + SphericalChebyshev.L3NCOLS[mL3Max] + SphericalChebyshev.L4NCOLS[mL4Max];
        
        mRCutsL = aRCutsL;
        mRCutsR = aRCutsR;
        if (mRCutsL.size()!=mRCutsR.size()) throw new IllegalArgumentException("Size of rcuts mismatch");
        mRCutMax = mRCutsR.max();
        mRCutsSize = mRCutsR.size();
        for (int i = 0; i < mRCutsSize; ++i) {
            if (mRCutsL.get(i) >= mRCutsR.get(i)) throw new IllegalArgumentException("rcut_l must be less than rcut_r: "+mRCutsL+" vs "+mRCutsR);
        }
        mEquFuseInDim = getEquFuseInDim(mNMax, mRCutsSize);
        mSize = mEquFuseInDim*mSizeL;
        
        mRFuncScale = aRFuncScale;
        if (mRFuncScale.size()!=mEquFuseInDim) throw new IllegalArgumentException("Size of rfunc scale mismatch");
    }
    
    @Override public MultiSphericalChebyshev threadSafeRef() {
        return new MultiSphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mL3Max, mL4Max, mRCutsL, mRCutsR, mRFuncScale);
    }
    
    static int getEquFuseInDim(int aNMax, int aRCutsSize) {
        return aRCutsSize *(aNMax+1);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "multi_spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l4max", mL4Max);
        rSaveTo.put("rcuts_l", mRCutsL.asList());
        rSaveTo.put("rcuts_r", mRCutsR.asList());
        rSaveTo.put("rfunc_scales", mRFuncScale.asList());
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MultiSphericalChebyshev load(String @NotNull [] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L4MAX, "l4max")).intValue();
        List<? extends Number> tRCutsR = (List<? extends Number>)UT.Code.get(aMap, "rcuts_r", "rcuts");
        if (tRCutsR==null) throw new IllegalArgumentException("Key `rcuts`/`rcuts_r` required for multi_spherical_chebyshev");
        Vector aRCutsR = Vectors.from(tRCutsR);
        List<? extends Number> tRCutsL = (List<? extends Number>)UT.Code.get(aMap, "rcuts_l");
        Vector aRCutsL = tRCutsL==null ? null : Vectors.from(tRCutsL);
        if (aRCutsL==null) {
            aRCutsL = aRCutsR.copy(); aRCutsL.negative2this();
        }
        int tEquFuseInDim = getEquFuseInDim(aNMax, tRCutsR.size());
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale==null ? Vectors.ones(tEquFuseInDim) : Vectors.from(tRFuncScale);
        return new MultiSphericalChebyshev(
            aSymbols, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            aRCutsL, aRCutsR, aRFuncScales
        );
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MultiSphericalChebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L4MAX, "l4max")).intValue();
        List<? extends Number> tRCutsR = (List<? extends Number>)UT.Code.get(aMap, "rcuts_r", "rcuts");
        if (tRCutsR==null) throw new IllegalArgumentException("Key `rcuts`/`rcuts_r` required for multi_spherical_chebyshev");
        Vector aRCutsR = Vectors.from(tRCutsR);
        List<? extends Number> tRCutsL = (List<? extends Number>)UT.Code.get(aMap, "rcuts_l");
        Vector aRCutsL = tRCutsL==null ? null : Vectors.from(tRCutsL);
        if (aRCutsL==null) {
            aRCutsL = aRCutsR.copy(); aRCutsL.negative2this();
        }
        int tEquFuseInDim = getEquFuseInDim(aNMax, tRCutsR.size());
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale ==null ? Vectors.ones(tEquFuseInDim) : Vectors.from(tRFuncScale);
        return new MultiSphericalChebyshev(
            null, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            aRCutsL, aRCutsR, aRFuncScales
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Vector getEquFuseWeight_(Map aMap, int aEquFuseInDim, int aEquFuseOutDim) {
        List tEquFuseWeight = (List)UT.Code.get(aMap, "equfuse_weight");
        if (tEquFuseWeight == null) return Vectors.zeros(aEquFuseInDim*aEquFuseOutDim);
        if (tEquFuseWeight.size() != aEquFuseInDim*aEquFuseOutDim) throw new IllegalArgumentException("Size of equfuse weight mismatch");
        return Vectors.from((List<? extends Number>)tEquFuseWeight);
    }
    
    @Override public void initParameters() {
        // 此时也经验设置 rfunc scale
        double tBase = MathEX.Adv.integral(0.0, mRCutMax, 1000, r -> {
            double x = r / mRCutMax;
            x = 1 - x*x;
            x = x*x;
            return r*r * x*x;
        });
        for (int k = 0, i = 0; k < mRCutsSize; ++k) {
            final double rcutL = mRCutsL.get(k);
            final double rcutR = mRCutsR.get(k);
            for (int n = 0; n <= mNMax; ++n, ++i) {
                final int fn = n;
                double tScale = MathEX.Adv.integral(Math.max(rcutL, 0.0), rcutR, 1000, r -> {
                    double x = (r-rcutL) / (rcutR-rcutL);
                    x = x+x - 1;
                    double x2 = 1 - x*x;
                    x2 = x2*x2;
                    double fcut = x2*x2;
                    double cheby = Math.abs(MathEX.Func.chebyshev(fn, -x));
                    return r*r * fcut * cheby;
                });
                mRFuncScale.set(i, tBase / tScale);
            }
        }
    }
    @Override public IVector parameters() {
        return null;
    }
    @Override public boolean hasParameters() {
        return false;
    }
    
    
    /** @return {@inheritDoc} */
    @Override public double rcut() {return mRCutMax;}
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
    
    
    @Override protected int forwardCacheSize_(int aNN, boolean aFullCache) {
        return aFullCache ? (aNN*((mNMax+1)*mRCutsSize + mRCutsSize + mLMAll) + mEquFuseInDim*mLMAll)
                          : ((mNMax+1)*mRCutsSize + mRCutsSize + mLMAll + mEquFuseInDim*mLMAll);
    }
    @Override protected int backwardCacheSize_(int aNN) {
        return aNN*(mEquFuseInDim) + mEquFuseInDim*mLMAll;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        return aFullCache ? (3*aNN*((mNMax+1)*mRCutsSize + mRCutsSize + mLMAll) + (mNMax+1)*mRCutsSize + (mNMax+1) + mRCutsSize + 3*mLMAll + mEquFuseInDim*mLMAll)
                          : (4*(mNMax+1)*mRCutsSize + (mNMax+1) + 4*mRCutsSize + 6*mLMAll + mEquFuseInDim*mLMAll);
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        return mLMAll + (mNMax+1)*mRCutsSize + mRCutsSize + mEquFuseInDim*mLMAll;
    }
    
    
    @Override
    protected void forward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleArrayVector rForwardCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算基组
        forward0(aNlDx, aNlDy, aNlDz, aNlType, rFp, rForwardCache, aFullCache);
    }
    @Override
    protected void backward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleArrayVector aForwardCache, DoubleArrayVector rBackwardCache, boolean aKeepCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 如果不保留旧值则在这里清空
        if (!aKeepCache) rBackwardCache.fill(0.0);
        
        backward0(aNlDx, aNlDy, aNlDz, aNlType, aGradFp, rGradPara, aForwardCache, rBackwardCache);
    }
    @Override
    protected void forwardForceAccumulate_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleArrayVector aForwardCache, DoubleArrayVector rForwardForceCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算力
        forwardForce0(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache);
    }
    @Override
    protected void backwardForce_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                                  DoubleArrayVector aForwardCache, DoubleArrayVector aForwardForceCache, DoubleArrayVector rBackwardCache, DoubleArrayVector rBackwardForceCache, boolean aKeepCache, boolean aFixBasis) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 如果不保留旧值则在这里清空
        if (!aKeepCache) {
            rBackwardCache.fill(0.0);
            rBackwardForceCache.fill(0.0);
        }
        
        backwardForce0(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis);
    }
    
    
    void forward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp, IDataShell<double[]> rForwardCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                 rFp.internalDataWithLengthCheck(mSize), rFp.internalDataShift(),
                 rForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, aFullCache)), rForwardCache.internalDataShift(), aFullCache,
                 mTypeNum, mRCutMax, mRCutsL.internalDataWithLengthCheck(), mRCutsR.internalDataWithLengthCheck(), mRCutsSize,
                 mNMax, mLMax, mL3Max, mL4Max,
                 mRFuncScale.internalDataWithLengthCheck());
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCutMax, double[] aRCutsL, double[] aRCutsR, int aRCutsSize,
                                        int aNMax, int aLMax, int aL3Max, int aL4Max,
                                        double[] aRFuncScale);
    
    void backward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aGradFp, IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> rBackwardCache) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = 0;
        backward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  aGradFp.internalDataWithLengthCheck(mSize), aGradFp.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(tParaSize), rGradPara.internalDataShift(),
                  aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                  rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                  mTypeNum, mRCutMax, mRCutsL.internalDataWithLengthCheck(), mRCutsR.internalDataWithLengthCheck(), mRCutsSize,
                  mNMax, mLMax, mL3Max, mL4Max,
                  mRFuncScale.internalDataWithLengthCheck());
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCutMax, double[] aRCutsL, double[] aRCutsR, int aRCutsSize,
                                         int aNMax, int aLMax, int aL3Max, int aL4Max,
                                         double[] aRFuncScale);
    
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCutMax, mRCutsL.internalDataWithLengthCheck(), mRCutsR.internalDataWithLengthCheck(), mRCutsSize,
                      mNMax, mLMax, mL3Max, mL4Max,
                      mRFuncScale.internalDataWithLengthCheck());
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCutMax, double[] aRCutsL, double[] aRCutsR, int aRCutsSize,
                                             int aNMax, int aLMax, int aL3Max, int aL4Max,
                                             double[] aRFuncScale);
    
    void backwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                        IDataShell<double[]> aNNGrad, IDataShell<double[]> aGradFx, IDataShell<double[]> aGradFy, IDataShell<double[]> aGradFz,
                        IDataShell<double[]> rGradNNGrad, @Nullable IDataShell<double[]> rGradPara,
                        IDataShell<double[]> aForwardCache, IDataShell<double[]> aForwardForceCache,
                        IDataShell<double[]> rBackwardCache, IDataShell<double[]> rBackwardForceCache, boolean aFixBasis) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = 0;
        if (!aFixBasis && rGradPara==null) throw new NullPointerException();
        backwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                       aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), aGradFx.internalDataWithLengthCheck(tNN, 0), aGradFy.internalDataWithLengthCheck(tNN, 0), aGradFz.internalDataWithLengthCheck(tNN, 0),
                       rGradNNGrad.internalDataWithLengthCheck(mSize), rGradNNGrad.internalDataShift(),
                       aFixBasis?null:rGradPara.internalDataWithLengthCheck(tParaSize), aFixBasis?0:rGradPara.internalDataShift(),
                       aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                       aForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, true)), aForwardForceCache.internalDataShift(),
                       rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                       rBackwardForceCache.internalDataWithLengthCheck(backwardForceCacheSize_(tNN)), rBackwardForceCache.internalDataShift(), aFixBasis,
                       mTypeNum, mRCutMax, mRCutsL.internalDataWithLengthCheck(), mRCutsR.internalDataWithLengthCheck(), mRCutsSize,
                       mNMax, mLMax, mL3Max, mL4Max,
                       mRFuncScale.internalDataWithLengthCheck());
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int aBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCutMax, double[] aRCutsL, double[] aRCutsR, int aRCutsSize,
                                              int aNMax, int aLMax, int aL3Max, int aL4Max,
                                              double[] aRFuncScale);
}
