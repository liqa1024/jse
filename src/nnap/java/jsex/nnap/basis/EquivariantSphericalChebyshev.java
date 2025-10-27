package jsex.nnap.basis;

import jse.code.UT;
import jse.math.IDataShell;
import jse.math.matrix.RowMatrix;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 在 {@link SphericalChebyshev} 基础上引入广义 Clebsch-Gordan
 * 系数实现等变网络的基组；参考了 MACE 的等变思路，理论上有着更好的泛化性能
 * <p>
 * 去除了部分 {@link SphericalChebyshev} 上的分支选项来保证简洁
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * Reference:
 * <a href="https://arxiv.org/abs/2206.07697">
 * MACE: Higher Order Equivariant Message Passing Neural Networks for Fast and Accurate Force Fields </a>
 * @author liqa
 */
@ApiStatus.Experimental
public class EquivariantSphericalChebyshev extends SphericalChebyshev {
    public final static int DEFAULT_LMAX = 3;
    public final static int DEFAULT_LLMAX = 2;
    
    final int mSizeLL;
    final int mLLMax;
    final int mLLMMAll;
    
    final Vector mEquWeight;
    final int mEquSize;
    final double[] mEquScale;
    
    EquivariantSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aL3Max, int aL4Max, int aLLMax, double aRCut,
                                  int aWType, int aFuseStyle, RowMatrix aFuseWeight, Vector aEquWeight, double[] aEquScale) {
        super(aSymbols, aTypeNum, aNMax, aLMax, false, aL3Max, true, aL4Max, true, aRCut, aWType, aFuseStyle, aFuseWeight, aEquWeight, aEquScale);
        if (aLMax<0 || aLMax>4) throw new IllegalArgumentException("Input lmax MUST be in [0, 4], input: "+aLMax);
        if (aLLMax<0 || aLLMax>4) throw new IllegalArgumentException("Input llmax MUST be in [0, 4], input: "+aLLMax);
        if (aL3Max>aLLMax) throw new IllegalArgumentException("Input l3max MUST be less than llmax, input: "+aL3Max+" vs "+aLLMax);
        if (aL4Max>aLLMax) throw new IllegalArgumentException("Input l4max MUST be less than llmax, input: "+aL4Max+" vs "+aLLMax);
        
        mEquWeight = mPostFuseWeight;
        mEquSize = mPostFuseSize;
        mEquScale = mPostFuseScale;
        if (mEquWeight==null) throw new IllegalStateException();
        
        mLLMax = aLLMax;
        mLLMMAll = (mLLMax+1)*(mLLMax+1);
        mSizeLL = (mLLMax+1) + L3NCOLS[mL3Max] + L4NCOLS[mL4Max];
        mSize = mEquSize*2*mSizeLL;
    }
    
    @Override public EquivariantSphericalChebyshev threadSafeRef() {
        return new EquivariantSphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mL3Max, mL4Max, mLLMax, mRCut, mWType, mFuseStyle, mFuseWeight, mEquWeight, mEquScale);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "equivariant_spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l4max", mL4Max);
        rSaveTo.put("llmax", mLLMax);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        rSaveTo.put("fuse_style", ALL_FUSE_STYLE.inverse().get(mFuseStyle));
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
        rSaveTo.put("equivariant_size", mPostFuseSize);
        rSaveTo.put("equivariant_scale", mPostFuseScale[0]);
        rSaveTo.put("equivariant_weight", mPostFuseWeight.asList());
    }
    
    @SuppressWarnings("rawtypes")
    public static EquivariantSphericalChebyshev load(String @NotNull [] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        int aNMax = ((Number) UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        int tLMaxMax = Math.max(Math.max(aLMax, aL3Max), aL4Max);
        int aWType = getWType_(aMap);
        int aFuseStyle = getFuseStyle_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax, tLMaxMax);
        int tFuseSize = getFuseSize(aWType, aFuseStyle, aNMax, tLMaxMax, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, tFuseSize);
        Vector aEquWeight = getEquWeight_(aMap, aFuseStyle, tSizeN, tLMaxMax);
        double tEquScale = ((Number)UT.Code.getWithDefault(aMap, 1.0, "equivariant_scale", "equ_scale")).doubleValue();
        return new EquivariantSphericalChebyshev(
            aSymbols, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LLMAX, "llmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight, aEquWeight, new double[]{tEquScale}
        );
    }
    @SuppressWarnings("rawtypes")
    public static EquivariantSphericalChebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        int tLMaxMax = Math.max(Math.max(aLMax, aL3Max), aL4Max);
        int aWType = getWType_(aMap);
        int aFuseStyle = getFuseStyle_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax, tLMaxMax);
        int tFuseSize = getFuseSize(aWType, aFuseStyle, aNMax, tLMaxMax, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, tFuseSize);
        Vector aEquWeight = getEquWeight_(aMap, aFuseStyle, tSizeN, tLMaxMax);
        double tEquScale = ((Number)UT.Code.getWithDefault(aMap, 1.0, "equivariant_scale", "equ_scale")).doubleValue();
        return new EquivariantSphericalChebyshev(
            null, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LLMAX, "llmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight, aEquWeight, new double[]{tEquScale}
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Vector getEquWeight_(Map aMap, int aFuseStyle, int aSizeN, int aLMaxMax) {
        Object tEquSize = UT.Code.get(aMap, "equivariant_size", "equ_size");
        Object tEquWeight = UT.Code.get(aMap, "equivariant_weight", "equ_weight");
        if (tEquWeight!=null) {
            Vector tVec = Vectors.from((List)tEquWeight);
            if (tEquSize!=null) {
                if (aFuseStyle==FUSE_STYLE_LIMITED) {
                    if (tVec.size()!=((Number)tEquSize).intValue()*aSizeN) throw new IllegalArgumentException("Size of equivariant weight mismatch");
                } else
                if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
                    if (tVec.size()!=((Number)tEquSize).intValue()*aSizeN*(aLMaxMax+1)) throw new IllegalArgumentException("Size of equivariant weight mismatch");
                } else {
                    throw new IllegalStateException();
                }
            }
            return tVec;
        }
        if (tEquSize==null) throw new IllegalArgumentException("Key `equivariant_size` or `equivariant_weight` required for equivariant_spherical_chebyshev");
        int tSize;
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            tSize = ((Number)tEquSize).intValue()*aSizeN;
        } else
        if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
            tSize = ((Number)tEquSize).intValue()*aSizeN*(aLMaxMax+1);
        } else {
            throw new IllegalStateException();
        }
        return Vector.zeros(tSize);
    }
    
    @Override protected int forwardCacheSize_(int aNN, boolean aFullCache) {
        return super.forwardCacheSize_(aNN, aFullCache) + mEquSize*2*mLLMMAll;
    }
    @Override protected int backwardCacheSize_(int aNN) {
        return super.backwardCacheSize_(aNN) + mEquSize*2*mLLMMAll;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        return super.forwardForceCacheSize_(aNN, aFullCache) + mEquSize*2*mLLMMAll;
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        return super.backwardForceCacheSize_(aNN) + mEquSize*2*mLLMMAll;
    }
    
    
    @Override
    void forward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp, IDataShell<double[]> rForwardCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                 rFp.internalDataWithLengthCheck(mSize), rFp.internalDataShift(),
                 rForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, aFullCache)), rForwardCache.internalDataShift(), aFullCache,
                 mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max, mLLMax,
                 mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                 mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale[0]);
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max, int aLLMax,
                                        int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize,
                                        double[] aEquWeight, int aEquSize, double aEquScale);
    
    @Override
    void backward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aGradFp, IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> rBackwardCache) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = 0;
        if (mFuseWeight!=null) tParaSize += mFuseWeight.internalDataSize();
        if (mPostFuseWeight!=null) tParaSize += mPostFuseWeight.internalDataSize();
        backward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  aGradFp.internalDataWithLengthCheck(mSize), aGradFp.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(tParaSize), rGradPara.internalDataShift(),
                  aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                  rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                  mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max, mLLMax,
                  mWType, mFuseStyle, mFuseSize,
                  mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale[0]);
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max, int aLLMax,
                                         int aWType, int aFuseStyle, int aFuseSize,
                                         double[] aEquWeight, int aEquSize, double aEquScale);
    
    @Override
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max, mLLMax,
                      mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                      mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale[0]);
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max, int aLLMax,
                                             int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize,
                                             double[] aEquWeight, int aEquSize, double aEquScale);
    
    @Override
    void backwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                        IDataShell<double[]> aNNGrad, IDataShell<double[]> aGradFx, IDataShell<double[]> aGradFy, IDataShell<double[]> aGradFz,
                        IDataShell<double[]> rGradNNGrad, @Nullable IDataShell<double[]> rGradPara,
                        IDataShell<double[]> aForwardCache, IDataShell<double[]> aForwardForceCache,
                        IDataShell<double[]> rBackwardCache, IDataShell<double[]> rBackwardForceCache, boolean aFixBasis) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = 0;
        if (mFuseWeight!=null) tParaSize += mFuseWeight.internalDataSize();
        if (mPostFuseWeight!=null) tParaSize += mPostFuseWeight.internalDataSize();
        boolean tNoPassGradPara = tParaSize==0 || aFixBasis;
        if (!tNoPassGradPara && rGradPara==null) throw new NullPointerException();
        backwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                       aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), aGradFx.internalDataWithLengthCheck(tNN, 0), aGradFy.internalDataWithLengthCheck(tNN, 0), aGradFz.internalDataWithLengthCheck(tNN, 0),
                       rGradNNGrad.internalDataWithLengthCheck(mSize), rGradNNGrad.internalDataShift(),
                       tNoPassGradPara?null:rGradPara.internalDataWithLengthCheck(tParaSize), tNoPassGradPara?0:rGradPara.internalDataShift(),
                       aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                       aForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, true)), aForwardForceCache.internalDataShift(),
                       rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                       rBackwardForceCache.internalDataWithLengthCheck(backwardForceCacheSize_(tNN)), rBackwardForceCache.internalDataShift(), aFixBasis,
                       mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max, mLLMax,
                       mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                       mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale[0]);
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int aBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max, int aLLMax,
                                              int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize,
                                              double[] aEquWeight, int aEquSize, double aEquScale);
}
