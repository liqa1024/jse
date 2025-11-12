package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

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
    public final static int DEFAULT_LMAX = 4;
    
    final Vector mEquWeight;
    final double[] mEquScale;
    final int[] mEquSize;
    final int mEquNumber, mEquWeightSize, mEquCacheSize;
    
    EquivariantSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aL3Max, int aL4Max, double aRCut,
                                  int aWType, int aFuseStyle, RowMatrix aFuseWeight, Vector aEquWeight, double[] aEquScale, int[] aEquSize) {
        super(aSymbols, aTypeNum, aNMax, checkLMax_(aLMax), false,
              checkL3Max_(aL3Max, aLMax), checkL4Max_(aL4Max, aLMax), aRCut,
              aWType, aFuseStyle, aFuseWeight, null, null);
        
        mEquWeight = aEquWeight;
        mEquSize = aEquSize;
        mEquScale = aEquScale;
        mEquNumber = aEquSize.length;
        if (mEquNumber < 2) throw new IllegalArgumentException("At least two equivariant layer is required");
        mSize = mEquSize[mEquNumber-1]*mSizeL;
        
        int tEquWeightSize = 0;
        int tColNum = mSizeN;
        for (int k = 0; k < mEquNumber; ++k) {
            int tRowNum = mEquSize[k];
            if (mFuseStyle==FUSE_STYLE_LIMITED) {
                tEquWeightSize += tRowNum*tColNum;
            } else {
                tEquWeightSize += tRowNum*tColNum*(mLMax+1);
            }
            tColNum = tRowNum*2;
        }
        mEquWeightSize = tEquWeightSize;
        int tEquCacheSize = mEquSize[0]*mLMAll;
        for (int k = 1; k < mEquNumber; ++k) {
            tEquCacheSize += mEquSize[k-1]*mLMAll;
            tEquCacheSize += mEquSize[k]*mLMAll;
        }
        mEquCacheSize = tEquCacheSize;
        if (mEquWeight.size()!=mEquWeightSize) throw new IllegalArgumentException("Size of equivariant weight mismatch");
        if (mEquScale.length!=mEquNumber) throw new IllegalArgumentException("Size of equivariant scale mismatch");
        if (mEquSize.length!=mEquNumber) throw new IllegalArgumentException("Size of equivariant size mismatch");
    }
    private static int checkLMax_(int aLMax) {
        if (aLMax<0 || aLMax>5) throw new IllegalArgumentException("Input lmax MUST be in [0, 5], input: "+aLMax);
        return aLMax;
    }
    private static int checkL3Max_(int aL3Max, int aLMax) {
        if (aL3Max>aLMax) throw new IllegalArgumentException("Input l3max MUST be less than lmax, input: "+aL3Max+" vs "+aLMax);
        return aL3Max;
    }
    private static int checkL4Max_(int aL4Max, int aLMax) {
        if (aL4Max>aLMax) throw new IllegalArgumentException("Input l4max MUST be less than lmax, input: "+aL4Max+" vs "+aLMax);
        return aL4Max;
    }
    
    @Override public EquivariantSphericalChebyshev threadSafeRef() {
        return new EquivariantSphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mL3Max, mL4Max, mRCut, mWType, mFuseStyle, mFuseWeight, mEquWeight, mEquScale, mEquSize);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "equivariant_spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l4max", mL4Max);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        rSaveTo.put("fuse_style", ALL_FUSE_STYLE.inverse().get(mFuseStyle));
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
        rSaveTo.put("equivariant_sizes", AbstractCollections.from(mEquSize));
        rSaveTo.put("equivariant_scales", AbstractCollections.from(mEquScale));
        List<List<Double>> rEquWeights = new ArrayList<>(mEquNumber);
        int tShift = 0;
        int tColNum = mSizeN;
        for (int k = 0; k < mEquNumber; ++k) {
            int tRowNum = mEquSize[k];
            int tSize;
            if (mFuseStyle==FUSE_STYLE_LIMITED) {
                tSize = tRowNum*tColNum;
            } else {
                tSize = tRowNum*tColNum*(mLMax+1);
            }
            rEquWeights.add(mEquWeight.subVec(tShift, tShift+tSize).asList());
            tShift += tSize;
            tColNum = tRowNum*2;
        }
        rSaveTo.put("equivariant_weights", rEquWeights);
    }
    
    @SuppressWarnings("rawtypes")
    public static EquivariantSphericalChebyshev load(String @NotNull [] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        int aNMax = ((Number) UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        int aWType = getWType_(aMap, WTYPE_EXFULL);
        int aFuseStyle = getFuseStyle_(aMap, FUSE_STYLE_EXTENSIVE);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax, aLMax);
        int tFuseSize = getFuseSize(aWType, aFuseStyle, aNMax, aLMax, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, tFuseSize);
        List<?> tEquSizes = (List<?>)UT.Code.get(aMap, "equivariant_sizes", "equ_sizes");
        int[] aEquSizes = new int[tEquSizes.size()];
        for (int i = 0; i < aEquSizes.length; ++i) {
            aEquSizes[i] = ((Number)tEquSizes.get(i)).intValue();
        }
        List<?> tEquScales = (List<?>)UT.Code.get(aMap, "equivariant_scales", "equ_scales");
        double[] aEquScales = new double[tEquScales==null ? tEquSizes.size() : tEquScales.size()];
        for (int i = 0; i < aEquScales.length; ++i) {
            aEquScales[i] = tEquScales==null ? 1.0 : ((Number)tEquScales.get(i)).doubleValue();
        }
        Vector aEquWeight = getEquWeight_(aMap, aFuseStyle, tSizeN, aLMax, aEquSizes);
        return new EquivariantSphericalChebyshev(
            aSymbols, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight, aEquWeight, aEquScales, aEquSizes
        );
    }
    @SuppressWarnings("rawtypes")
    public static EquivariantSphericalChebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        int aWType = getWType_(aMap, WTYPE_EXFULL);
        int aFuseStyle = getFuseStyle_(aMap, FUSE_STYLE_EXTENSIVE);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax, aLMax);
        int tFuseSize = getFuseSize(aWType, aFuseStyle, aNMax, aLMax, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, tFuseSize);
        List<?> tEquSizes = (List<?>)UT.Code.get(aMap, "equivariant_sizes", "equ_sizes");
        int[] aEquSizes = new int[tEquSizes.size()];
        for (int i = 0; i < aEquSizes.length; ++i) {
            aEquSizes[i] = ((Number)tEquSizes.get(i)).intValue();
        }
        List<?> tEquScales = (List<?>)UT.Code.get(aMap, "equivariant_scales", "equ_scales");
        double[] aEquScales = new double[tEquScales==null ? tEquSizes.size() : tEquScales.size()];
        for (int i = 0; i < aEquScales.length; ++i) {
            aEquScales[i] = tEquScales==null ? 1.0 : ((Number)tEquScales.get(i)).doubleValue();
        }
        Vector aEquWeight = getEquWeight_(aMap, aFuseStyle, tSizeN, aLMax, aEquSizes);
        return new EquivariantSphericalChebyshev(
            null, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight, aEquWeight, aEquScales, aEquSizes
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Vector getEquWeight_(Map aMap, int aFuseStyle, int aSizeN, int aLMax, int[] aEquSizes) {
        List<?> tEquWeights = (List<?>)UT.Code.get(aMap, "equivariant_weights", "equ_weights");
        if (tEquWeights!=null) {
            int tEquNumber = tEquWeights.size();
            if (aEquSizes.length!=tEquNumber) throw new IllegalArgumentException("Size of equivariant weights mismatch");
            Vector.Builder tOut = Vector.builder(16);
            int tColNum = aSizeN;
            for (int k = 0; k < tEquNumber; ++k) {
                int tRowNum = aEquSizes[k];
                Vector tVec = Vectors.from((List)tEquWeights.get(k));
                int tSize;
                if (aFuseStyle==FUSE_STYLE_LIMITED) {
                    tSize = tRowNum*tColNum;
                } else
                if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
                    tSize = tRowNum*tColNum*(aLMax+1);
                } else {
                    throw new IllegalStateException();
                }
                if (tVec.size()!=tSize) throw new IllegalArgumentException("Size of equivariant weight '"+k+"' mismatch");
                tOut.addAll(tVec);
                tColNum = tRowNum*2;
            }
            return tOut.build();
        }
        int tEquWeightSize = 0;
        int tColNum = aSizeN;
        for (int tRowNum : aEquSizes) {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                tEquWeightSize += tRowNum*tColNum;
            } else {
                tEquWeightSize += tRowNum*tColNum*(aLMax+1);
            }
            tColNum = tRowNum*2;
        }
        return Vector.zeros(tEquWeightSize);
    }
    
    
    @Override public void initParameters() {
        super.initParameters();
        // 补充对于 EquWeight 的初始化
        mEquWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这个可以有效加速训练；经验设定
        int tShift = 0;
        int tColNum = mSizeN;
        for (int k = 0; k < mEquNumber; ++k) {
            int tRowNum = mEquSize[k];
            for (int np = 0; np < tRowNum; ++np) {
                if (mFuseStyle==FUSE_STYLE_LIMITED) {
                    IVector tSubVec = mEquWeight.subVec(tShift, tShift + tColNum);
                    tSubVec.div2this(tSubVec.operation().norm1() / tColNum);
                    tShift += tColNum;
                } else {
                    // extensive 情况下会先遍历 l，因此归一化需要这样进行
                    for (int l = 0; l <= mLMax; ++l) {
                        double tNorm1 = 0.0;
                        for (int n = 0; n < tColNum; ++n) {
                            tNorm1 += Math.abs(mEquWeight.get(tShift + n*(mLMax+1) + l));
                        }
                        final double tDiv = tNorm1 / tColNum;
                        for (int n = 0; n < tColNum; ++n) {
                            mEquWeight.update(tShift + n*(mLMax+1) + l, v -> v/tDiv);
                        }
                    }
                    tShift += tColNum*(mLMax+1);
                }
            }
            // 在这个经验设定下，scale 设置为此值确保输出的基组值数量级一致
            mEquScale[k] = MathEX.Fast.sqrt(1.0 / tColNum);
            tColNum = tRowNum*2;
        }
    }
    @Override public IVector parameters() {
        final IVector tPara = super.parameters();
        // 补充对于 EquWeight 的参数
        if (tPara == null) return mEquWeight;
        final int tParaSize = tPara.size();
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx < tParaSize) {
                    return tPara.get(aIdx);
                }
                aIdx -= tParaSize;
                if (aIdx < mEquWeightSize) {
                    return mEquWeight.get(aIdx);
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                if (aIdx < tParaSize) {
                    tPara.set(aIdx, aValue);
                    return;
                }
                aIdx -= tParaSize;
                if (aIdx < mEquWeightSize) {
                    mEquWeight.set(aIdx, aValue);
                    return;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return tParaSize+mEquWeightSize;
            }
        };
    }
    @Override public boolean hasParameters() {
        return true;
    }
    
    @Override protected int forwardCacheSize_(int aNN, boolean aFullCache) {
        return super.forwardCacheSize_(aNN, aFullCache) + mEquCacheSize;
    }
    @Override protected int backwardCacheSize_(int aNN) {
        return super.backwardCacheSize_(aNN) + mEquCacheSize;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        return super.forwardForceCacheSize_(aNN, aFullCache) + mEquCacheSize;
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        return super.backwardForceCacheSize_(aNN) + mEquCacheSize;
    }
    
    
    @Override
    protected void backward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleArrayVector aForwardCache, DoubleArrayVector rBackwardCache, boolean aKeepCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 如果不保留旧值则在这里清空
        if (!aKeepCache) rBackwardCache.fill(0.0);
        backward0(aNlDx, aNlDy, aNlDz, aNlType, aGradFp, rGradPara, aForwardCache, rBackwardCache);
    }
    
    @Override
    void forward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp, IDataShell<double[]> rForwardCache, boolean aFullCache) {
        if (mEquSize.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        if (mEquScale.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        forward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                 rFp.internalDataWithLengthCheck(mSize), rFp.internalDataShift(),
                 rForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, aFullCache)), rForwardCache.internalDataShift(), aFullCache,
                 mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                 mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                 mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale, mEquNumber);
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                        int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize,
                                        double[] aEquWeight, int[] aEquSize, double[] aEquScale, int aEquNumber);
    
    @Override
    void backward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aGradFp, IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> rBackwardCache) {
        if (mEquSize.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        if (mEquScale.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        int tParaSize = mEquWeight.internalDataSize();
        if (mFuseWeight!=null) tParaSize += mFuseWeight.internalDataSize();
        backward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  aGradFp.internalDataWithLengthCheck(mSize), aGradFp.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(tParaSize), rGradPara.internalDataShift(),
                  aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                  rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                  mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                  mWType, mFuseStyle, mFuseSize,
                  mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale, mEquNumber);
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                         int aWType, int aFuseStyle, int aFuseSize,
                                         double[] aEquWeight, int[] aEquSize, double[] aEquScale, int aEquNumber);
    
    @Override
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        if (mEquSize.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        if (mEquScale.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                      mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                      mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale, mEquNumber);
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                             int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize,
                                             double[] aEquWeight, int[] aEquSize, double[] aEquScale, int aEquNumber);
    
    @Override
    void backwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                        IDataShell<double[]> aNNGrad, IDataShell<double[]> aGradFx, IDataShell<double[]> aGradFy, IDataShell<double[]> aGradFz,
                        IDataShell<double[]> rGradNNGrad, @Nullable IDataShell<double[]> rGradPara,
                        IDataShell<double[]> aForwardCache, IDataShell<double[]> aForwardForceCache,
                        IDataShell<double[]> rBackwardCache, IDataShell<double[]> rBackwardForceCache, boolean aFixBasis) {
        if (mEquSize.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        if (mEquScale.length < mEquNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        int tParaSize = mEquWeight.internalDataSize();
        if (mFuseWeight!=null) tParaSize += mFuseWeight.internalDataSize();
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
                       mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                       mWType, mFuseStyle, mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                       mEquWeight.internalDataWithLengthCheck(), mEquSize, mEquScale, mEquNumber);
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int aBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                              int aWType, int aFuseStyle, double[] aFuseWeight, int aFuseSize,
                                              double[] aEquWeight, int[] aEquSize, double[] aEquScale, int aEquNumber);
}
