package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

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
    
    final Vector mEmbWeights, mEmbBiases;
    final int[] mEmbDims;
    final int mEmbNumber, mEmbInputDim, mEmbOutputDim;
    final int mEmbCacheSize;
    
    final Vector mEquFuseWeight;
    final int mEquFuseSize;
    final double[] mEquFuseScale;
    
    final Vector mRFuncScale;
    final Vector mSystemScale;
    
    MultiLayerSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aL3Max, int aL4Max, double aRCut,
                                 Vector aEmbWeights, Vector aEmbBiases, int[] aEmbDims, Vector aEquFuseWeight,
                                 double[] aEquFuseScale, Vector aRFuncScale, Vector aSystemScale) {
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
        mRCut = aRCut;
        mLMAll = (mLMax+1)*(mLMax+1);
        mSizeL = mLMax+1 + SphericalChebyshev.L3NCOLS[mL3Max] + SphericalChebyshev.L4NCOLS[mL4Max];
        
        mEmbDims = aEmbDims;
        mEmbNumber = mEmbDims.length;
        if (mEmbNumber == 0) throw new IllegalArgumentException("At least one embedding layer is required");
        mEmbInputDim = mNMax; // TODO
        mEmbOutputDim = mEmbDims[mEmbNumber-1];
        
        int tEmbWeightsSize = 0;
        int tEmbBiasesSize = 0;
        int tColNum = mEmbInputDim;
        for (int tEmbDim : mEmbDims) {
            tEmbWeightsSize += tColNum * tEmbDim;
            tEmbBiasesSize += tEmbDim;
            tColNum = tEmbDim;
        }
        mEmbCacheSize = tEmbBiasesSize-mEmbOutputDim;
        mEmbWeights = aEmbWeights==null ? Vector.zeros(tEmbWeightsSize) : aEmbWeights;
        mEmbBiases = aEmbBiases==null ? Vector.zeros(tEmbBiasesSize) : aEmbBiases;
        if (mEmbWeights.size() != tEmbWeightsSize) throw new IllegalArgumentException("The size of embedding weights mismatch");
        if (mEmbBiases.size() != tEmbBiasesSize) throw new IllegalArgumentException("The size of embedding biases mismatch");
        
        mEquFuseWeight = aEquFuseWeight;
        mEquFuseSize = mEquFuseWeight.size()/mEmbOutputDim;
        mEquFuseScale = aEquFuseScale;
        if (mEquFuseScale.length!=1) throw new IllegalArgumentException("Size of equfuse scale mismatch");
        if (mEquFuseWeight.size()!=mEquFuseSize*mEmbOutputDim) throw new IllegalArgumentException("Size of equfuse weight mismatch");
        mSize = mEquFuseSize*mSizeL;
        
        mRFuncScale = aRFuncScale==null ? Vector.ones(mNMax) : aRFuncScale;
        mSystemScale = aSystemScale==null ? Vector.ones(mEmbOutputDim*(mLMax+1)) : aSystemScale;
        if (mRFuncScale.size()!=mNMax) throw new IllegalArgumentException("Size of rfunc scale mismatch");
        if (mSystemScale.size()!=mEmbOutputDim*(mLMax+1)) throw new IllegalArgumentException("Size of system scale mismatch");
    }
    
    @Override public MultiLayerSphericalChebyshev threadSafeRef() {
        return new MultiLayerSphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mL3Max, mL4Max, mRCut, mEmbWeights, mEmbBiases, mEmbDims, mEquFuseWeight, mEquFuseScale, mRFuncScale, mSystemScale);
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
        rSaveTo.put("embedding_dims", AbstractCollections.from(mEmbDims));
        rSaveTo.put("embedding_weights", mEmbWeights.asList());
        rSaveTo.put("embedding_biases", mEmbBiases.asList());
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
        List<?> tEmbDims = (List<?>)UT.Code.get(aMap, "embedding_dims");
        if (tEmbDims==null) throw new IllegalArgumentException("Key `embedding_dims` required for multi-type spherical chebyshev");
        int[] aEmbDims = new int[tEmbDims.size()];
        for (int i = 0; i < aEmbDims.length; ++i) {
            aEmbDims[i] = ((Number)tEmbDims.get(i)).intValue();
        }
        List<? extends Number> tEmbWeights = (List<? extends Number>)UT.Code.get(aMap, "embedding_weights");
        Vector aEmbWeights = tEmbWeights==null ? null : Vectors.from(tEmbWeights);
        List<? extends Number> tEmbBiases = (List<? extends Number>)UT.Code.get(aMap, "embedding_biases");
        Vector aEmbBiases = tEmbBiases==null ? null : Vectors.from(tEmbBiases);
        Object tEquFuseSize = UT.Code.get(aMap, "equfuse_size");
        if (tEquFuseSize==null) throw new IllegalArgumentException("Key `equfuse_size` required for multi-type spherical chebyshev");
        int aEquFuseSize = ((Number)tEquFuseSize).intValue();
        double aEquFuseScale = ((Number)UT.Code.getWithDefault(aMap, 1.0, "equfuse_scale")).doubleValue();
        Vector aEquFuseWeight = getEquFuseWeight_(aMap, aEmbDims[aEmbDims.length-1], aEquFuseSize);
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        return new MultiLayerSphericalChebyshev(
            aSymbols, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aEmbWeights, aEmbBiases, aEmbDims, aEquFuseWeight,
            new double[]{aEquFuseScale}, aRFuncScales, aSystemScale
        );
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MultiLayerSphericalChebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L4MAX, "l4max")).intValue();
        List<?> tEmbDims = (List<?>)UT.Code.get(aMap, "embedding_dims");
        if (tEmbDims==null) throw new IllegalArgumentException("Key `embedding_dims` required for multi-type spherical chebyshev");
        int[] aEmbDims = new int[tEmbDims.size()];
        for (int i = 0; i < aEmbDims.length; ++i) {
            aEmbDims[i] = ((Number)tEmbDims.get(i)).intValue();
        }
        List<? extends Number> tEmbWeights = (List<? extends Number>)UT.Code.get(aMap, "embedding_weights");
        Vector aEmbWeights = tEmbWeights==null ? null : Vectors.from(tEmbWeights);
        List<? extends Number> tEmbBiases = (List<? extends Number>)UT.Code.get(aMap, "embedding_biases");
        Vector aEmbBiases = tEmbBiases==null ? null : Vectors.from(tEmbBiases);
        Object tEquFuseSize = UT.Code.get(aMap, "equfuse_size");
        if (tEquFuseSize==null) throw new IllegalArgumentException("Key `equfuse_size` required for multi-type spherical chebyshev");
        int aEquFuseSize = ((Number)tEquFuseSize).intValue();
        double aEquFuseScale = ((Number)UT.Code.getWithDefault(aMap, 1.0, "equfuse_scale")).doubleValue();
        Vector aEquFuseWeight = getEquFuseWeight_(aMap, aEmbDims[aEmbDims.length-1], aEquFuseSize);
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        return new MultiLayerSphericalChebyshev(
            null, aTypeNum, aNMax, aLMax, aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aEmbWeights, aEmbBiases, aEmbDims, aEquFuseWeight,
            new double[]{aEquFuseScale}, aRFuncScales, aSystemScale
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Vector getEquFuseWeight_(Map aMap, int aRFuseSize, int aEquFuseSize) {
        List tEquFuseWeight = (List)UT.Code.get(aMap, "equfuse_weight");
        if (tEquFuseWeight == null) return Vectors.zeros(aRFuseSize*aEquFuseSize);
        if (tEquFuseWeight.size() != aRFuseSize*aEquFuseSize) throw new IllegalArgumentException("Size of equfuse weight mismatch");
        return Vectors.from((List<? extends Number>)tEquFuseWeight);
    }
    
    @Override public void initParameters() {
        /// embedding 网络参数初始化，采用 Kaiming 均匀初始化
        int tColNum = mEmbInputDim;
        int tShift = 0;
        final int tEnd = mEmbNumber-1;
        for (int i = 0; i < mEmbNumber; ++i) {
            int tEmbDim = mEmbDims[i];
            int tSize = tEmbDim*tColNum;
            double tBound = MathEX.Fast.sqrt((i==tEnd?3.0:6.0) / tColNum); // Kaiming 均匀初始化，注意最后线性层采用 3.0 权重
            mEmbWeights.subVec(tShift, tShift+tSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tSize;
            tColNum = tEmbDim;
        }
        tShift = 0;
        tColNum = mEmbInputDim;
        for (int tEmbDim : mEmbDims) {
            double tBound = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
            mEmbBiases.subVec(tShift, tShift+tEmbDim).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tEmbDim;
            tColNum = tEmbDim;
        }
        /// 等变混合权重初始化
        mEquFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这个可以有效加速训练；经验设定
        tShift = 0;
        for (int np = 0; np < mEquFuseSize; ++np) {
            IVector tSubVec = mEquFuseWeight.subVec(tShift, tShift + mEmbOutputDim);
            tSubVec.div2this(tSubVec.operation().norm1() / mEmbOutputDim);
            tShift += mEmbOutputDim;
        }
        // 在这个经验设定下，scale 设置为此值确保输出的基组值数量级一致
        mEquFuseScale[0] = MathEX.Fast.sqrt(1.0 / mEmbOutputDim);
    }
    @Override public IVector parameters() {
        final int tEmbWSize = mEmbWeights.size();
        final int tEmbBSize = mEmbBiases.size();
        final int tEquWSize = mEquFuseWeight.size();
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx < tEmbWSize) {
                    return mEmbWeights.get(aIdx);
                }
                aIdx -= tEmbWSize;
                if (aIdx < tEmbBSize) {
                    return mEmbBiases.get(aIdx);
                }
                aIdx -= tEmbBSize;
                if (aIdx < tEquWSize) {
                    return mEquFuseWeight.get(aIdx);
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                if (aIdx < tEmbWSize) {
                    mEmbWeights.set(aIdx, aValue);
                    return;
                }
                aIdx -= tEmbWSize;
                if (aIdx < tEmbBSize) {
                    mEmbBiases.set(aIdx, aValue);
                    return;
                }
                aIdx -= tEmbBSize;
                if (aIdx < tEquWSize) {
                    mEquFuseWeight.set(aIdx, aValue);
                    return;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return tEmbWSize+tEmbBSize+tEquWSize;
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
        return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll + mEmbCacheSize*2) + mEmbOutputDim + mEmbOutputDim*mLMAll + mEquFuseSize*mLMAll)
                          : (mNMax+1 + mLMAll + mEmbCacheSize + mEmbOutputDim + mEmbOutputDim*mLMAll + mEquFuseSize*mLMAll);
    }
    @Override protected int backwardCacheSize_(int aNN) {
        return aNN*(mEmbCacheSize + mEmbOutputDim) + mEmbOutputDim*mLMAll + mEquFuseSize*mLMAll;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll) + (mNMax+1) + 2*mLMAll + mEmbOutputDim + mEmbOutputDim*mLMAll + mEquFuseSize*mLMAll)
                          : (4*(mNMax+1) + 5*mLMAll + mEmbOutputDim + mEmbOutputDim*mLMAll + mEquFuseSize*mLMAll);
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        return mLMAll + mEmbOutputDim + mEmbOutputDim*mLMAll + mEquFuseSize*mLMAll;
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
    
    @TestOnly
    public void forwardEmb(Vector aInputX, Vector rOutputEmb) {
        if (mEmbDims.length < mEmbNumber) throw new IllegalArgumentException("data size mismatch");
        forwardEmb0(aInputX.internalDataWithLengthCheck(mNMax), rOutputEmb.internalDataWithLengthCheck(mEmbOutputDim), mNMax,
                    mEmbWeights.internalDataWithLengthCheck(), mEmbBiases.internalDataWithLengthCheck(), mEmbDims, mEmbNumber);
    }
    private static native void forwardEmb0(double[] aInputX, double[] rOutputEmb, int aNMax, double[] aEmbWeights, double[] aEmbBiases, int[] aEmbDims, int aEmbNumber);
    
    void forward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp, IDataShell<double[]> rForwardCache, boolean aFullCache) {
        if (mEmbDims.length < mEmbNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        forward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                 rFp.internalDataWithLengthCheck(mSize), rFp.internalDataShift(),
                 rForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, aFullCache)), rForwardCache.internalDataShift(), aFullCache,
                 mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                 mEmbWeights.internalDataWithLengthCheck(), mEmbBiases.internalDataWithLengthCheck(), mEmbDims, mEmbNumber,
                 mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                 mRFuncScale.internalDataWithLengthCheck(), mSystemScale.internalDataWithLengthCheck());
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                        double[] aEmbWeights, double[] aEmbBiases, int[] aEmbDims, int aEmbNumber,
                                        double[] aEquFuseWeight, int aEquFuseSize, double aEquFuseScale,
                                        double[] aRFuncScale, double[] aSystemScale);
    
    void backward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aGradFp, IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> rBackwardCache) {
        if (mEmbDims.length < mEmbNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        int tParaSize = mEmbWeights.internalDataSize() + mEmbBiases.internalDataSize() + mEquFuseWeight.internalDataSize();
        backward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  aGradFp.internalDataWithLengthCheck(mSize), aGradFp.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(tParaSize), rGradPara.internalDataShift(),
                  aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                  rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                  mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                  mEmbWeights.internalDataWithLengthCheck(), mEmbDims, mEmbNumber,
                  mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                  mSystemScale.internalDataWithLengthCheck());
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                         double[] aEmbWeights, int[] aEmbDims, int aEmbNumber,
                                         double[] aEquFuseWeight, int aEquFuseSize, double aEquFuseScale,
                                         double[] aSystemScale);
    
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        if (mEmbDims.length < mEmbNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCut, mNMax, mLMax, mL3Max, mL4Max,
                      mEmbWeights.internalDataWithLengthCheck(), mEmbDims, mEmbNumber,
                      mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                      mRFuncScale.internalDataWithLengthCheck(), mSystemScale.internalDataWithLengthCheck());
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                             double[] aEmbWeights, int[] aEmbDims, int aEmbNumber,
                                             double[] aEquFuseWeight, int aEquFuseSize, double aEquFuseScale,
                                             double[] aRFuncScale, double[] aSystemScale);
    
    void backwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                        IDataShell<double[]> aNNGrad, IDataShell<double[]> aGradFx, IDataShell<double[]> aGradFy, IDataShell<double[]> aGradFz,
                        IDataShell<double[]> rGradNNGrad, @Nullable IDataShell<double[]> rGradPara,
                        IDataShell<double[]> aForwardCache, IDataShell<double[]> aForwardForceCache,
                        IDataShell<double[]> rBackwardCache, IDataShell<double[]> rBackwardForceCache, boolean aFixBasis) {
        if (mEmbDims.length < mEmbNumber) throw new IllegalArgumentException("data size mismatch");
        int tNN = aNlDx.internalDataSize();
        int tParaSize = mEmbWeights.internalDataSize() + mEmbBiases.internalDataSize() + mEquFuseWeight.internalDataSize();
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
                       mEmbWeights.internalDataWithLengthCheck(), mEmbDims, mEmbNumber,
                       mEquFuseWeight.internalDataWithLengthCheck(), mEquFuseSize, mEquFuseScale[0],
                       mSystemScale.internalDataWithLengthCheck());
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int aBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCut, int aNMax, int aLMax, int aL3Max, int aL4Max,
                                              double[] aEmbWeights, int[] aEmbDims, int aEmbNumber,
                                              double[] aEquFuseWeight, int aEquFuseSize, double aEquFuseScale,
                                              double[] aSystemScale);
}
