package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.DoubleArrayVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 一种仅使用 Chebyshev 多项式将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这不会包含角向序，但是速度可以很快。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * 现在统一通过调用 c 并借助 avx 指令优化来得到最佳的性能
 * @author liqa
 */
public class Chebyshev extends WTypeBasis {
    public final static int DEFAULT_NMAX = 5;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    /** 仅对不影响整体且有部分关键势还在使用的 Chebyshev 保留兼容 */
    public final static int FUSE_STYLE_LIMITED = 0, FUSE_STYLE_EXTENSIVE = 1;
    final static BiMap<String, Integer> ALL_FUSE_STYLE = ImmutableBiMap.<String, Integer>builder()
        .put("limited", FUSE_STYLE_LIMITED)
        .put("extensive", FUSE_STYLE_EXTENSIVE)
        .build();
    
    final int mFuseStyle;
    final String @Nullable[] mSymbols;
    final double mRCut;
    final int mSize;
    
    Chebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, double aRCut, int aWType, int aFuseStyle, @Nullable RowMatrix aFuseWeight) {
        super(aTypeNum, aNMax, aWType, aFuseWeight);
        if (!ALL_FUSE_STYLE.containsValue(aFuseStyle)) throw new IllegalArgumentException("Input fuse_style MUST be in {0, 1}, input: "+ aFuseStyle);
        mFuseStyle = aFuseStyle;
        // Chebyshev 对 extensive 保留兼容
        if (mFuseWeight!=null && mFuseStyle==FUSE_STYLE_EXTENSIVE) {
            mFuseSize = mFuseWeight.ncols()/(aNMax+1);
            if (mFuseWeight.ncols()!=mFuseSize*(aNMax+1)) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            mSizeN = getSizeN_(mWType, mTypeNum, mNMax, mFuseSize);
        }
        mSymbols = aSymbols;
        mRCut = aRCut;
        mSize = mSizeN;
    }
    @Override public Chebyshev threadSafeRef() {
        return new Chebyshev(mSymbols, mTypeNum, mNMax, mRCut, mWType, mFuseStyle, mFuseWeight);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        if (mFuseWeight!=null && mFuseStyle!=FUSE_STYLE_LIMITED) {
            rSaveTo.put("fuse_style", ALL_FUSE_STYLE.inverse().get(mFuseStyle));
        }
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
    }
    
    @SuppressWarnings({"rawtypes"})
    public static Chebyshev load(String @NotNull[] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        if (aMap.containsKey("rfunc_scales")) throw new IllegalArgumentException("rfunc_scales is invalid now.");
        if (aMap.containsKey("system_scales")) throw new IllegalArgumentException("system_scales is invalid now.");
        int aWType = getWType_(aMap);
        int aFuseStyle = getFuseStyle_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax);
        return new Chebyshev(
            aSymbols, aTypeNum, aNMax,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight
        );
    }
    @SuppressWarnings({"rawtypes"})
    public static Chebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        if (aMap.containsKey("rfunc_scales")) throw new IllegalArgumentException("rfunc_scales is invalid now.");
        if (aMap.containsKey("system_scales")) throw new IllegalArgumentException("system_scales is invalid now.");
        int aWType = getWType_(aMap);
        int aFuseStyle = getFuseStyle_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax);
        return new Chebyshev(
            null, aTypeNum, aNMax,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight
        );
    }
    
    @SuppressWarnings({"rawtypes"})
    static int getFuseStyle_(Map aMap) {
        @Nullable Object tStyle = UT.Code.get(aMap, "fuse_style");
        if (tStyle == null) return FUSE_STYLE_LIMITED;
        if (tStyle instanceof Number) return ((Number)tStyle).intValue();
        @Nullable Integer tOut = ALL_FUSE_STYLE.get(tStyle.toString());
        if (tOut == null) throw new IllegalArgumentException("Input wtype MUST be in {limited, extensive}, input: "+tStyle);
        return tOut;
    }
    @SuppressWarnings("rawtypes")
    static @Nullable RowMatrix getFuseWeight_(Map aMap, int aWType, int aFuseStyle, int aTypeNum, int aNMax) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) return null;
        Object tFuseSize = UT.Code.get(aMap, "fuse_size");
        Object tFuseWeight = aMap.get("fuse_weight");
        if (tFuseWeight!=null) {
            if (tFuseSize==null) {
                // 如果没有 fuse_size 则是旧版，按列读取
                return Matrices.fromCols((List<?>)tFuseWeight);
            }
            // 否则按行读取
            RowMatrix tMat = Matrices.fromRows((List<?>)tFuseWeight);
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                if (tMat.ncols()!=((Number)tFuseSize).intValue()) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            } else
            if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
                if (tMat.ncols()!=((Number)tFuseSize).intValue()*(aNMax+1)) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            } else {
                throw new IllegalStateException();
            }
            return tMat;
        }
        if (tFuseSize==null) throw new IllegalArgumentException("Key `fuse_weight` or `fuse_size` required for fuse wtype");
        int tColNum;
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            tColNum = ((Number)tFuseSize).intValue();
        } else
        if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
            tColNum = ((Number)tFuseSize).intValue()*(aNMax+1);
        } else {
            throw new IllegalStateException();
        }
        return RowMatrix.zeros(aTypeNum, tColNum);
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
    @Override public int ntypes() {return mTypeNum;}
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
        return aFullCache ? aNN*(mNMax+1 + 1) : (mNMax+1);
    }
    @Override protected int backwardCacheSize_(int aNN) {
        return 0;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        return aFullCache ? (3*aNN*(mNMax+1 + 1) + (mNMax+1)) : (4*(mNMax+1));
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        if (mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE) {
            return mNMax+1;
        }
        return 0;
    }
    
    @Override
    protected void forward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleArrayVector rForwardCache, boolean aFullCache) {
        if (isClosed()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算基组
        forward0(aNlDx, aNlDy, aNlDz, aNlType, rFp, rForwardCache, aFullCache);
    }
    @Override
    protected void backward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleArrayVector aForwardCache, DoubleArrayVector rBackwardCache, boolean aKeepCache) {
        if (isClosed()) throw new IllegalStateException("This Basis is dead");
        
        // 如果不是 fuse 直接返回不走 native
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_EXFUSE) return;
        // 如果不保留旧值则在这里清空
        if (!aKeepCache) rBackwardCache.fill(0.0);
        
        backward0(aNlDx, aNlDy, aNlDz, aNlType, aGradFp, rGradPara, aForwardCache, rBackwardCache);
    }
    @Override
    protected void forwardForceAccumulate_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleArrayVector aForwardCache, DoubleArrayVector rForwardForceCache, boolean aFullCache) {
        if (isClosed()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算力
        forwardForce0(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache);
    }
    @Override
    protected void backwardForce_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                                  DoubleArrayVector aForwardCache, DoubleArrayVector aForwardForceCache, DoubleArrayVector rBackwardCache, DoubleArrayVector rBackwardForceCache, boolean aKeepCache, boolean aFixBasis) {
        if (isClosed()) throw new IllegalStateException("This Basis is dead");
        
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
                 mTypeNum, mRCut, mNMax, mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize);
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCut, int aNMax, int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize);
    
    void backward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aGradFp, IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> rBackwardCache) {
        assert mFuseWeight != null;
        int tNN = aNlDx.internalDataSize();
        backward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  aGradFp.internalDataWithLengthCheck(mSize), aGradFp.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(mFuseWeight.internalDataSize()), rGradPara.internalDataShift(),
                  aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                  rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                  mTypeNum, mRCut, mNMax, mWType, mFuseStyle, mFuseSize);
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCut, int aNMax, int aWType, int aFuseStyle, int aFuseSize);
    
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCut, mNMax, mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize);
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCut, int aNMax, int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize);
    
    void backwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                        IDataShell<double[]> aNNGrad, IDataShell<double[]> aGradFx, IDataShell<double[]> aGradFy, IDataShell<double[]> aGradFz,
                        IDataShell<double[]> rGradNNGrad, @Nullable IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> aForwardForceCache,
                        IDataShell<double[]> rBackwardCache, IDataShell<double[]> rBackwardForceCache, boolean aFixBasis) {
        int tNN = aNlDx.internalDataSize();
        if (mFuseWeight!=null && !aFixBasis && rGradPara==null) throw new NullPointerException();
        boolean tNoPassGradPara = mFuseWeight==null || aFixBasis;
        backwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                       aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), aGradFx.internalDataWithLengthCheck(tNN, 0), aGradFy.internalDataWithLengthCheck(tNN, 0), aGradFz.internalDataWithLengthCheck(tNN, 0),
                       rGradNNGrad.internalDataWithLengthCheck(mSize), rGradNNGrad.internalDataShift(),
                       tNoPassGradPara?null:rGradPara.internalDataWithLengthCheck(mFuseWeight.internalDataSize()), tNoPassGradPara?0:rGradPara.internalDataShift(),
                       aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                       aForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, true)), aForwardForceCache.internalDataShift(),
                       rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                       rBackwardForceCache.internalDataWithLengthCheck(backwardForceCacheSize_(tNN)), rBackwardForceCache.internalDataShift(), aFixBasis,
                       mTypeNum, mRCut, mNMax, mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize);
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int rBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCut, int aNMax, int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize);
}
