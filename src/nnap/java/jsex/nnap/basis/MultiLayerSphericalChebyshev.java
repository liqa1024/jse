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

import static jse.code.CS.RANDOM;

/**
 * 在 {@link SphericalChebyshev} 基础上引入多次对于近邻的遍历，可以通过近邻环境来学习近邻权重。
 * 作为 NNAP 下一代基组实现，思路上参考了 Allegro 的结构。
 * <p>
 * 去除了大部分 {@link SphericalChebyshev} 的参数选择，只保留最优的选择从而保证代码简洁。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * Reference:
 * <a href="https://arxiv.org/abs/2204.05249">
 * Learning Local Equivariant Representations for Large-Scale Atomistic Dynamics </a>
 * @author liqa
 */
@ApiStatus.Experimental
public class MultiLayerSphericalChebyshev extends MergeableBasis {
    final int mTypeNum;
    final String @Nullable[] mSymbols;
    
    final int mNMax;
    final int mLMax, mL3Max, mL4Max;
    final double mRCut;
    
    final int mSizeL;
    final int mLMAll;
    final int mSize;
    
    final Vector mRFuseWeight;
    final int mRFuseSize;
    
    final Vector mEquFuseWeight;
    final int mEquFuseSize;
    final double[] mEquFuseScale;
    
    final Vector mRFuncScale;
    final Vector mSystemScale;
    
    MultiLayerSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aL3Max, int aL4Max, double aRCut,
                                 Vector aRFuseWeight, Vector aEquFuseWeight, double[] aEquFuseScale,
                                 Vector aRFuncScale, Vector aSystemScale) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax<0 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [0, 20], input: "+aNMax);
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
        mRCut = aRCut;
        mLMAll = (mLMax+1)*(mLMax+1);
        
        mRFuseWeight = aRFuseWeight;
        mRFuseSize = mRFuseWeight.size()/(mNMax+1);
        if (mRFuseWeight.size()!=mRFuseSize*(mNMax+1)) throw new IllegalArgumentException("Size of rfuse weight mismatch");
        
        mSizeL = mLMax+1 + SphericalChebyshev.L3NCOLS[mL3Max] + SphericalChebyshev.L4NCOLS[mL4Max];
        
        mEquFuseWeight = aEquFuseWeight;
        mEquFuseSize = mEquFuseWeight.size()/mRFuseSize;
        mEquFuseScale = aEquFuseScale;
        if (mEquFuseScale.length!=1) throw new IllegalArgumentException("Size of equfuse scale mismatch");
        if (mEquFuseWeight.size()!=mEquFuseSize*mRFuseSize) throw new IllegalArgumentException("Size of equfuse weight mismatch");
        mSize = mEquFuseSize*mSizeL;
        
        mRFuncScale = aRFuncScale==null ? Vector.ones(mNMax+1) : aRFuncScale;
        mSystemScale = aSystemScale==null ? Vector.ones(mRFuseSize*(mLMax+1)) : aSystemScale;
        if (mRFuncScale.size()!=mNMax+1) throw new IllegalArgumentException("Size of rfunc scale mismatch");
        if (mSystemScale.size()!=mRFuseSize*(mLMax+1)) throw new IllegalArgumentException("Size of system scale mismatch");
    }
    
    @Override public MultiLayerSphericalChebyshev threadSafeRef() {
        return new MultiLayerSphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mL3Max, mL4Max, mRCut, mRFuseWeight, mEquFuseWeight, mEquFuseScale, mRFuncScale, mSystemScale);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "multi_layer_spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l4max", mL4Max);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("rfunc_scales", mRFuncScale.asList());
        rSaveTo.put("system_scales", mSystemScale.asList());
        rSaveTo.put("rfuse_size", mRFuseSize);
        rSaveTo.put("rfuse_weight", mRFuseWeight.asList());
        rSaveTo.put("equfuse_size", mEquFuseSize);
        rSaveTo.put("equfuse_weight", mEquFuseWeight.asList());
        rSaveTo.put("equfuse_scale", mEquFuseScale[0]);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MultiLayerSphericalChebyshev load(String @NotNull [] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L4MAX, "l4max")).intValue();
        Object tRFuseSize = UT.Code.get(aMap, "rfuse_size");
        if (tRFuseSize==null) throw new IllegalArgumentException("Key `rfuse_size` required for multi-type spherical chebyshev");
        int aRFuseSize = ((Number)tRFuseSize).intValue();
        Vector aRFuseWeight = getRFuseWeight_(aMap, aNMax, aRFuseSize);
        Object tEquFuseSize = UT.Code.get(aMap, "equfuse_size");
        if (tEquFuseSize==null) throw new IllegalArgumentException("Key `equfuse_size` required for multi-type spherical chebyshev");
        int aEquFuseSize = ((Number)tEquFuseSize).intValue();
        double aEquFuseScale = ((Number)UT.Code.getWithDefault(aMap, 1.0, "equfuse_scale")).doubleValue();
        Vector aEquFuseWeight = getEquFuseWeight_(aMap, aRFuseSize, aEquFuseSize);
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        return new MultiLayerSphericalChebyshev(
            aSymbols, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aRFuseWeight, aEquFuseWeight, new double[]{aEquFuseScale},
            aRFuncScales, aSystemScale
        );
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MultiLayerSphericalChebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L4MAX, "l4max")).intValue();
        Object tRFuseSize = UT.Code.get(aMap, "rfuse_size");
        if (tRFuseSize==null) throw new IllegalArgumentException("Key `rfuse_size` required for multi-type spherical chebyshev");
        int aRFuseSize = ((Number)tRFuseSize).intValue();
        Vector aRFuseWeight = getRFuseWeight_(aMap, aNMax, aRFuseSize);
        Object tEquFuseSize = UT.Code.get(aMap, "equfuse_size");
        if (tEquFuseSize==null) throw new IllegalArgumentException("Key `equfuse_size` required for multi-type spherical chebyshev");
        int aEquFuseSize = ((Number)tEquFuseSize).intValue();
        double aEquFuseScale = ((Number)UT.Code.getWithDefault(aMap, 1.0, "equfuse_scale")).doubleValue();
        Vector aEquFuseWeight = getEquFuseWeight_(aMap, aRFuseSize, aEquFuseSize);
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        return new MultiLayerSphericalChebyshev(
            null, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aRFuseWeight, aEquFuseWeight, new double[]{aEquFuseScale},
            aRFuncScales, aSystemScale
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Vector getRFuseWeight_(Map aMap, int aNMax, int aRFuseSize) {
        List tRFuseWeight = (List)UT.Code.get(aMap, "rfuse_weight");
        if (tRFuseWeight == null) return Vectors.zeros((aNMax+1)*aRFuseSize);
        if (tRFuseWeight.size() != (aNMax+1)*aRFuseSize) throw new IllegalArgumentException("Size of rfuse weight mismatch");
        return Vectors.from((List<? extends Number>)tRFuseWeight);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Vector getEquFuseWeight_(Map aMap, int aRFuseSize, int aEquFuseSize) {
        List tEquFuseWeight = (List)UT.Code.get(aMap, "equfuse_weight");
        if (tEquFuseWeight == null) return Vectors.zeros(aRFuseSize*aEquFuseSize);
        if (tEquFuseWeight.size() != aRFuseSize*aEquFuseSize) throw new IllegalArgumentException("Size of equfuse weight mismatch");
        return Vectors.from((List<? extends Number>)tEquFuseWeight);
    }
    
    @Override public void initParameters() {
        /// 径向函数混合权重初始化
        mRFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这个可以有效加速训练；经验设定
        int tShift = 0;
        for (int np = 0; np < mRFuseSize; ++np) {
            IVector tSubVec = mRFuseWeight.subVec(tShift, tShift + mNMax+1);
            tSubVec.div2this(tSubVec.operation().norm1() / (mNMax+1));
            tShift += mNMax+1;
        }
        // 在这个经验设定下，scale 设置为此值确保输出的基组值数量级一致
        mRFuseWeight.div2this(MathEX.Fast.sqrt(mNMax+1));
        /// 等变混合权重初始化
        mEquFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这个可以有效加速训练；经验设定
        tShift = 0;
        for (int np = 0; np < mEquFuseSize; ++np) {
            IVector tSubVec = mEquFuseWeight.subVec(tShift, tShift + mRFuseSize);
            tSubVec.div2this(tSubVec.operation().norm1() / mRFuseSize);
            tShift += mRFuseSize;
        }
        // 在这个经验设定下，scale 设置为此值确保输出的基组值数量级一致
        mEquFuseScale[0] = MathEX.Fast.sqrt(1.0 / mRFuseSize);
    }
    @Override public IVector parameters() {
        final int tRParaSize = mRFuseWeight.size();
        final int tEquParaSize = mEquFuseWeight.size();
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx < tRParaSize) {
                    return mRFuseWeight.get(aIdx);
                }
                aIdx -= tRParaSize;
                if (aIdx < tEquParaSize) {
                    return mEquFuseWeight.get(aIdx);
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                if (aIdx < tRParaSize) {
                    mRFuseWeight.set(aIdx, aValue);
                    return;
                }
                aIdx -= tRParaSize;
                if (aIdx < tEquParaSize) {
                    mEquFuseWeight.set(aIdx, aValue);
                    return;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return tRParaSize+tEquParaSize;
            }
        };
    }
    @Override public boolean hasParameters() {
        return true;
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
    
    
    @Override protected int forwardCacheSize_(int aNN, boolean aFullCache) {
        return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll) + mRFuseSize + mRFuseSize*mLMAll + mEquFuseSize*mLMAll)
                          : (mNMax+1 + mLMAll + mRFuseSize + mRFuseSize*mLMAll + mEquFuseSize*mLMAll);
    }
    @Override protected int backwardCacheSize_(int aNN) {
        return aNN*mRFuseSize + mRFuseSize*mLMAll + mEquFuseSize*mLMAll;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll) + (mNMax+1) + 2*mLMAll + mRFuseSize + mRFuseSize*mLMAll + mEquFuseSize*mLMAll)
                          : (4*(mNMax+1) + 5*mLMAll + mRFuseSize + mRFuseSize*mLMAll + mEquFuseSize*mLMAll);
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        return mLMAll + mRFuseSize + mRFuseSize*mLMAll + mEquFuseSize*mLMAll;
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
                 mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                 mRFuseWeight.internalDataWithLengthCheck(), mRFuseSize,
                 mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                 mRFuncScale.internalDataWithLengthCheck(), mSystemScale.internalDataWithLengthCheck());
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                        double[] aRFuseWeight, int aRFuseSize, double[] aEquFuseWeight, int aEquFuseSize,
                                        double aEquFuseScale, double[] aRFuncScale, double[] aSystemScale);
    
    void backward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aGradFp, IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> rBackwardCache) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = mRFuseWeight.internalDataSize() + mEquFuseWeight.internalDataSize();
        backward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  aGradFp.internalDataWithLengthCheck(mSize), aGradFp.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(tParaSize), rGradPara.internalDataShift(),
                  aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                  rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                  mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                  mRFuseSize, mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                  mSystemScale.internalDataWithLengthCheck());
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                         int aRFuseSize, double[] aEquFuseWeight, int aEquFuseSize,
                                         double aEquFuseScale, double[] aSystemScale);
    
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                      mRFuseWeight.internalDataWithLengthCheck(), mRFuseSize,
                      mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                      mRFuncScale.internalDataWithLengthCheck(), mSystemScale.internalDataWithLengthCheck());
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                             double[] aRFuseWeight, int aRFuseSize, double[] aEquFuseWeight, int aEquFuseSize,
                                             double aEquFuseScale, double[] aRFuncScale, double[] aSystemScale);
    
    void backwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                        IDataShell<double[]> aNNGrad, IDataShell<double[]> aGradFx, IDataShell<double[]> aGradFy, IDataShell<double[]> aGradFz,
                        IDataShell<double[]> rGradNNGrad, @Nullable IDataShell<double[]> rGradPara,
                        IDataShell<double[]> aForwardCache, IDataShell<double[]> aForwardForceCache,
                        IDataShell<double[]> rBackwardCache, IDataShell<double[]> rBackwardForceCache, boolean aFixBasis) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = mRFuseWeight.internalDataSize() + mEquFuseWeight.internalDataSize();
        if (!aFixBasis && rGradPara==null) throw new NullPointerException();
        backwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                       aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), aGradFx.internalDataWithLengthCheck(tNN, 0), aGradFy.internalDataWithLengthCheck(tNN, 0), aGradFz.internalDataWithLengthCheck(tNN, 0),
                       rGradNNGrad.internalDataWithLengthCheck(mSize), rGradNNGrad.internalDataShift(),
                       aFixBasis ?null:rGradPara.internalDataWithLengthCheck(tParaSize), aFixBasis?0:rGradPara.internalDataShift(),
                       aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                       aForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, true)), aForwardForceCache.internalDataShift(),
                       rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                       rBackwardForceCache.internalDataWithLengthCheck(backwardForceCacheSize_(tNN)), rBackwardForceCache.internalDataShift(), aFixBasis,
                       mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                       mRFuseWeight.internalDataWithLengthCheck(), mRFuseSize,
                       mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                       mSystemScale.internalDataWithLengthCheck());
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int aBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                              double[] aRFuseWeight, int aRFuseSize, double[] aEquFuseWeight, int aEquFuseSize,
                                              double aEquFuseScale, double[] aSystemScale);
}
