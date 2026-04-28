package jsex.nnap.nn;

import jse.code.Conf;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;
import static jse.code.CS.ZL_VEC;

/**
 * 可以共享部分神经元的前馈神经网络 (FFNN) 实现，原生实现在非 batch 的情况下可以有最快的性能
 * <p>
 * 物理意义上来说，共享的部分代表通用的近邻信息，独立部分代表中心原子的信息
 * <p>
 * 仅支持输出单个数值的网络，且输出层不可共享
 *
 * @author liqa
 */
public class SharedFeedForward2 extends NeuralNetwork2 {
    private final FeedForward2 mBase;
    private final int mSharedType;
    public FeedForward2 sharedNeuralNetwork() {return mBase;}
    public int sharedType() {return mSharedType;}
    
    private final int mInputDim;
    private final int[] mSharedHiddenDims;
    private Vector mNoSharedHiddenWeights, mNoSharedHiddenBiases;
    private Vector mNoSharedOutputWeight, mNoSharedOutputBias;
    private Vector mGradNoSharedHiddenWeights = null, mGradNoSharedHiddenBiases = null;
    private Vector mGradNoSharedOutputWeight = null, mGradNoSharedOutputBias = null;
    private final int mNumLayers;
    private final int mNoSharedHiddenWeightsSize;
    private final int mNoSharedHiddenBiasesSize;
    private final int mNoSharedOutputWeightSize, mNoSharedOutputBiasSize;
    
    private IDoubleOrFloatCPointer mInternalHiddenWeights = null, mInternalHiddenBiases = null;
    private IDoubleOrFloatCPointer mInternalOutputWeight = null, mInternalOutputBias = null;
    private IDoubleOrFloatCPointer[] mInternalGradHiddenWeights = null, mInternalGradHiddenBiases = null;
    private IDoubleOrFloatCPointer[] mInternalGradOutputWeight = null, mInternalGradOutputBias = null;
    
    SharedFeedForward2(int aInputDim, FeedForward2 aBase, int aSharedType, int[] aSharedHiddenDims, Vector aNoSharedHiddenWeights, Vector aNoSharedHiddenBiases, Vector aNoSharedOutputWeight, Vector aNoSharedOutputBias) {
        mInputDim = aInputDim;
        mBase = aBase;
        mSharedType = aSharedType;
        // 最后值缺省支持
        if (aSharedHiddenDims.length == mBase.mNumLayers) {
            int[] oSharedHiddenDims = aSharedHiddenDims;
            aSharedHiddenDims = new int[oSharedHiddenDims.length+1];
            System.arraycopy(oSharedHiddenDims, 0, aSharedHiddenDims, 0, oSharedHiddenDims.length);
        }
        mSharedHiddenDims = aSharedHiddenDims;
        mNumLayers = mSharedHiddenDims.length-1;
        if (mNumLayers != mBase.mNumLayers) throw new IllegalArgumentException("Hidden number mismatch");
        if (mInputDim != mBase.mInputDim) throw new IllegalArgumentException("Input dimensions mismatch for shared nn");
        int tNoSharedHiddenWeightsSize = 0;
        int tNoSharedHiddenBiasesSize = 0;
        int tColNum = mInputDim;
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mBase.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            if (tSharedHiddenDim<0 || tSharedHiddenDim>tHiddenDim) throw new IllegalArgumentException("invalid shared dimension: "+tSharedHiddenDim+" vs "+tHiddenDim);
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            tNoSharedHiddenWeightsSize += tColNum * tNoSharedHiddenDim;
            tNoSharedHiddenBiasesSize += tNoSharedHiddenDim;
            tColNum = tHiddenDim;
        }
        mNoSharedHiddenWeightsSize = tNoSharedHiddenWeightsSize;
        mNoSharedHiddenBiasesSize = tNoSharedHiddenBiasesSize;
        mNoSharedHiddenWeights = aNoSharedHiddenWeights==null ? Vectors.zeros(mNoSharedHiddenWeightsSize) : aNoSharedHiddenWeights;
        mNoSharedHiddenBiases = aNoSharedHiddenBiases==null ? Vectors.zeros(mNoSharedHiddenBiasesSize) : aNoSharedHiddenBiases;
        if (mNoSharedHiddenWeights.size() != mNoSharedHiddenWeightsSize) throw new IllegalArgumentException("The size of hidden weights mismatch");
        if (mNoSharedHiddenBiases.size() != mNoSharedHiddenBiasesSize) throw new IllegalArgumentException("The size of hidden biases mismatch");
        int tSharedOutputDim = mSharedHiddenDims[mNumLayers];
        if (tSharedOutputDim<0 || tSharedOutputDim>1) throw new IllegalArgumentException("invalid shared dimension: "+tSharedOutputDim+" vs "+1);
        mNoSharedOutputWeightSize = tSharedOutputDim==1 ? 0 : mBase.mHiddenDims[mNumLayers -1];
        mNoSharedOutputBiasSize = tSharedOutputDim==1 ? 0 : 1;
        mNoSharedOutputWeight = aNoSharedOutputWeight==null ? Vectors.zeros(mNoSharedOutputWeightSize) : aNoSharedOutputWeight;
        mNoSharedOutputBias = aNoSharedOutputBias==null ?  Vectors.zeros(mNoSharedOutputBiasSize) : aNoSharedOutputBias;
        if (mNoSharedOutputWeight.size() != mNoSharedOutputWeightSize) throw new IllegalArgumentException("The size of output weight mismatch");
        if (mNoSharedOutputBias.size() != mNoSharedOutputBiasSize) throw new IllegalArgumentException("The size of output biases mismatch");
    }
    @Override public void requireGrad(int aNumThreads) {
        if (mInternalGradHiddenWeights!=null) return;
        mInternalGradHiddenWeights = new IDoubleOrFloatCPointer[aNumThreads];
        mInternalGradHiddenBiases = new IDoubleOrFloatCPointer[aNumThreads];
        mInternalGradOutputWeight = new IDoubleOrFloatCPointer[aNumThreads];
        mInternalGradOutputBias = new IDoubleOrFloatCPointer[aNumThreads];
    }
    
    @Override public void updateParameters() {
        updateParameters_();
    }
    @Override public void backwardParameter() {
        if (mInternalGradHiddenWeights==null) throw new IllegalStateException("invoke `requireGrad(nthreads)` first.");
        int tNumThreads = mInternalGradHiddenWeights.length;
        for (int ti = 0; ti < tNumThreads; ++ti) {
            IDoubleOrFloatCPointer tInternalGradHiddenWeights = mInternalGradHiddenWeights[ti];
            IDoubleOrFloatCPointer tInternalGradOutputWeight = mInternalGradOutputWeight[ti];
            IDoubleOrFloatCPointer tInternalGradHiddenBiases = mInternalGradHiddenBiases[ti];
            int tColNum = mInputDim;
            int tShift = 0, tNoSharedShift = 0;
            IDoubleOrFloatCPointer tPtr = tInternalGradHiddenWeights.copy();
            for (int i = 0; i < mNumLayers; ++i) {
                int tHiddenDim = mBase.mHiddenDims[i];
                int tSharedHiddenDim = mSharedHiddenDims[i];
                int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
                int tSize = tHiddenDim*tColNum;
                int tNoSharedSize = tNoSharedHiddenDim*tColNum;
                for (int j = 0; j < tNoSharedSize; ++j) {
                    mGradNoSharedHiddenWeights.add(j+tNoSharedShift, tPtr.getAtD(j));
                }
                // shared last
                for (int j = tNoSharedSize; j < tSize; ++j) {
                    mBase.mGradHiddenWeights.add(j+tShift, tPtr.getAtD(j));
                }
                tShift += tSize;
                tNoSharedShift += tNoSharedSize;
                tPtr.rightShift(tSize);
                tColNum = tHiddenDim;
            }
            tShift = 0; tNoSharedShift = 0;
            tPtr = tInternalGradHiddenBiases.copy();
            for (int i = 0; i < mNumLayers; ++i) {
                int tHiddenDim = mBase.mHiddenDims[i];
                int tSharedHiddenDim = mSharedHiddenDims[i];
                int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
                for (int j = 0; j < tNoSharedHiddenDim; ++j) {
                    mGradNoSharedHiddenBiases.add(j+tNoSharedShift, tPtr.getAtD(j));
                }
                // shared last
                for (int j = tNoSharedHiddenDim; j < tHiddenDim; ++j) {
                    mBase.mGradHiddenBiases.add(j+tShift, tPtr.getAtD(j));
                }
                tShift += tHiddenDim;
                tNoSharedShift += tNoSharedHiddenDim;
                tPtr.rightShift(tHiddenDim);
            }
            if (mSharedHiddenDims[mNumLayers]==0) {
                for (int j = 0; j < mBase.mOutputWeightSize; ++j) {
                    mGradNoSharedOutputWeight.add(j, tInternalGradOutputWeight.getAtD(j));
                }
                mGradNoSharedOutputBias.add(0, mInternalGradOutputBias[ti].getD());
            } else {
                for (int j = 0; j < mBase.mOutputWeightSize; ++j) {
                    mBase.mGradOutputWeight.add(j, tInternalGradOutputWeight.getAtD(j));
                }
                mBase.mGradOutputBias.add(0, mInternalGradOutputBias[ti].getD());
            }
        }
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    final void updateParameters_() {
        mInternalHiddenWeights.fillD(mBase.mHiddenWeights);
        mInternalHiddenBiases.fillD(mBase.mHiddenBiases);
        int tColNum = mInputDim;
        int tShift = 0;
        IDoubleOrFloatCPointer tPtr = mInternalHiddenWeights.copy();
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mBase.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            // shared last
            int tSize = tNoSharedHiddenDim*tColNum;
            tPtr.fillD(mNoSharedHiddenWeights.subVec(tShift, tShift+tSize));
            tShift += tSize;
            tPtr.rightShift(tHiddenDim*tColNum);
            tColNum = tHiddenDim;
        }
        tShift = 0;
        tPtr = mInternalHiddenBiases.copy();
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mBase.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            // shared last
            tPtr.fillD(mNoSharedHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim));
            tShift += tNoSharedHiddenDim;
            tPtr.rightShift(tHiddenDim);
        }
        if (mSharedHiddenDims[mNumLayers]==0) {
            mInternalOutputWeight.fillD(mNoSharedOutputWeight);
            mInternalOutputBias.setD(mNoSharedOutputBias.get(0));
        } else {
            mInternalOutputWeight.fillD(mBase.mOutputWeight);
            mInternalOutputBias.setD(mBase.mOutputBias.get(0));
        }
    }
    
    // 只需要初始化可拟合的参数部分
    @Override public void initParameters() {
        int tColNum = mInputDim;
        int tShift = 0;
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mBase.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            double tBound = MathEX.Fast.sqrt(6.0 / tColNum); // Kaiming 均匀初始化
            int tSize = tNoSharedHiddenDim*tColNum;
            mNoSharedHiddenWeights.subVec(tShift, tShift+tSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        tShift = 0;
        tColNum = mInputDim;
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mBase.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            double tBound = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
            mNoSharedHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tNoSharedHiddenDim;
            tColNum = tHiddenDim;
        }
        double tBound = MathEX.Fast.sqrt(3.0 / tColNum); // Kaiming 均匀初始化，注意输出层没有激活函数因此权重需要调整
        double tBoundB = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
        if (mSharedHiddenDims[mNumLayers]==0) {
            mNoSharedOutputWeight.assign(() -> RANDOM.nextDouble(-tBound, tBound));
            mNoSharedOutputBias.set(0, RANDOM.nextDouble(-tBoundB, tBoundB));
        }
        updateParameters_();
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SharedFeedForward2 load(int aInputDim, NeuralNetwork2[] aNN, Map aMap) {
        @Nullable Object tShare = aMap.get("share");
        if (tShare==null) {
            throw new IllegalArgumentException("Key `share` required for shared_feed_forward");
        }
        int tSharedType = ((Number)tShare).intValue();
        FeedForward2 tBase = (FeedForward2)aNN[tSharedType-1];
        // 旧版分层 share 情况现在不再支持
        if (aMap.get("shared_flags") != null) {
            throw new IllegalArgumentException("shared in layer nn is invalid now");
        }
        @Nullable Number tInputDim = (Number)UT.Code.get(aMap, "input_dim");
        if (tInputDim!=null && tInputDim.intValue()!=aInputDim) {
            throw new IllegalArgumentException("Input dimension mismatch: "+tInputDim.intValue()+" vs "+aInputDim);
        }
        int[] aSharedHiddenDims;
        @Nullable Object tSharedHiddenDims = UT.Code.get(aMap, "shared_hidden_dims", "shared_nnarch");
        if (tSharedHiddenDims == null) {
            throw new IllegalArgumentException("Key `shared_hidden_dims` required for shared_feed_forward");
        }
        if (tSharedHiddenDims instanceof int[]) {
            aSharedHiddenDims = Arrays.copyOf((int[])tSharedHiddenDims, ((int[])tSharedHiddenDims).length);
        } else
        if (tSharedHiddenDims instanceof List) {
            aSharedHiddenDims = new int[((List<?>)tSharedHiddenDims).size()];
            for (int i = 0; i < aSharedHiddenDims.length; ++i) {
                aSharedHiddenDims[i] = ((Number)((List<?>)tSharedHiddenDims).get(i)).intValue();
            }
        } else {
            throw new IllegalArgumentException("invalid type of shared_hidden_dims: " + tSharedHiddenDims.getClass().getName());
        }
        // 最后值缺省支持
        if (aSharedHiddenDims.length == tBase.mNumLayers) {
            int[] oSharedHiddenDims = aSharedHiddenDims;
            aSharedHiddenDims = new int[oSharedHiddenDims.length+1];
            System.arraycopy(oSharedHiddenDims, 0, aSharedHiddenDims, 0, oSharedHiddenDims.length);
        }
        int tNumLayers = aSharedHiddenDims.length-1;
        if (tNumLayers!=tBase.mNumLayers) {
            throw new IllegalArgumentException("The number of hidden layers mismatch: "+tNumLayers+" vs "+tBase.mNumLayers);
        }
        int tNoSharedHiddenWeightsSize = 0;
        int tNoSharedHiddenBiasesSize = 0;
        int tColNum = aInputDim;
        for (int i = 0; i < tNumLayers; ++i) {
            int tHiddenDim = tBase.mHiddenDims[i];
            int tSharedHiddenDim = aSharedHiddenDims[i];
            if (tSharedHiddenDim<0 || tSharedHiddenDim>tHiddenDim) throw new IllegalArgumentException("invalid shared dimension: "+tSharedHiddenDim+" vs "+tHiddenDim);
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            tNoSharedHiddenWeightsSize += tColNum * tNoSharedHiddenDim;
            tNoSharedHiddenBiasesSize += tNoSharedHiddenDim;
            tColNum = tHiddenDim;
        }
        Vector aNoSharedHiddenWeights = null;
        @Nullable List<?> tNoSharedHiddenWeights = (List<?>)UT.Code.get(aMap, "hidden_weights");
        if (tNoSharedHiddenWeights != null) {
            if (tNoSharedHiddenWeights.size()!=tNumLayers) {
                throw new IllegalArgumentException("The number of hidden weights mismatch: "+tNoSharedHiddenWeights.size()+" vs "+tNumLayers);
            }
            aNoSharedHiddenWeights = Vectors.zeros(tNoSharedHiddenWeightsSize);
            tColNum = aInputDim;
            int tShift = 0;
            for (int i = 0; i < tNumLayers; ++i) {
                int tHiddenDim = tBase.mHiddenDims[i];
                int tSharedHiddenDim = aSharedHiddenDims[i];
                int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
                int tSize = tNoSharedHiddenDim*tColNum;
                RowMatrix tWeight = Matrices.fromRows((List<?>)tNoSharedHiddenWeights.get(i));
                if (tWeight.nrows()!=tNoSharedHiddenDim) {
                    throw new IllegalArgumentException("nrows of hidden weight '"+i+"' mismatch: "+tWeight.nrows()+" vs "+tNoSharedHiddenDim);
                }
                if (tNoSharedHiddenDim>0 && tWeight.ncols()!=tColNum) {
                    throw new IllegalArgumentException("ncols of hidden weight '"+i+"' mismatch: "+tWeight.ncols()+" vs "+tColNum);
                }
                aNoSharedHiddenWeights.subVec(tShift, tShift+tSize).fill(tWeight.asVecRow());
                tShift += tSize;
                tColNum = tHiddenDim;
            }
        }
        Vector aNoSharedHiddenBiases = null;
        @Nullable List<?> tNoSharedHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        if (tNoSharedHiddenBiases != null) {
            if (tNoSharedHiddenBiases.size()!=tNumLayers) {
                throw new IllegalArgumentException("The number of hidden biases mismatch: "+tNoSharedHiddenBiases.size()+" vs "+tNumLayers);
            }
            aNoSharedHiddenBiases = Vectors.zeros(tNoSharedHiddenBiasesSize);
            int tShift = 0;
            for (int i = 0; i < tNumLayers; ++i) {
                int tHiddenDim = tBase.mHiddenDims[i];
                int tSharedHiddenDim = aSharedHiddenDims[i];
                int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
                Vector tBias = Vectors.from((List<? extends Number>)tNoSharedHiddenBiases.get(i));
                if (tBias.size()!=tNoSharedHiddenDim) {
                    throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch: "+tBias.size()+" vs "+tNoSharedHiddenDim);
                }
                aNoSharedHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim).fill(tBias);
                tShift += tNoSharedHiddenDim;
            }
        }
        Vector aNoSharedOutputWeight = null;
        Vector aNoSharedOutputBias = null;
        if (aSharedHiddenDims[tNumLayers]==1) {
            aNoSharedOutputWeight = ZL_VEC;
            aNoSharedOutputBias = ZL_VEC;
        } else {
            @Nullable Object tNoSharedOutputWeight = aMap.get("output_weight");
            if (tNoSharedOutputWeight != null) {
                aNoSharedOutputWeight = Vectors.from((List<? extends Number>)tNoSharedOutputWeight);
                if (aNoSharedOutputWeight.size()!=tBase.mHiddenDims[tNumLayers-1]) {
                    throw new IllegalArgumentException("Size of output weight mismatch: "+aNoSharedOutputWeight.size()+" vs "+tBase.mHiddenDims[tNumLayers-1]);
                }
            }
            @Nullable Object tNoSharedOutputBias = aMap.get("output_bias");
            if (tNoSharedOutputBias != null) {
                aNoSharedOutputBias = Vectors.zeros(1);
                aNoSharedOutputBias.set(0, ((Number)tNoSharedOutputBias).doubleValue());
            }
        }
        return new SharedFeedForward2(aInputDim, tBase, tSharedType, aSharedHiddenDims, aNoSharedHiddenWeights, aNoSharedHiddenBiases, aNoSharedOutputWeight, aNoSharedOutputBias);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "shared_feed_forward");
        rSaveTo.put("share", mSharedType);
        rSaveTo.put("shared_hidden_dims", AbstractCollections.from(mSharedHiddenDims));
        rSaveTo.put("input_dim", mInputDim);
        List<List<List<Double>>> rHiddenWeights = new ArrayList<>(mNumLayers);
        int tColNum = mInputDim;
        int tShift = 0;
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mBase.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            List<List<Double>> tWeights = new ArrayList<>(tNoSharedHiddenDim);
            rHiddenWeights.add(tWeights);
            for (int j = 0; j < tNoSharedHiddenDim; ++j) {
                tWeights.add(mNoSharedHiddenWeights.subVec(tShift, tShift+tColNum).asList());
                tShift += tColNum;
            }
            tColNum = tHiddenDim;
        }
        rSaveTo.put("hidden_weights", rHiddenWeights);
        List<List<Double>> rHiddenBiases = new ArrayList<>(mNumLayers);
        tShift = 0;
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mBase.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            rHiddenBiases.add(mNoSharedHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim).asList());
            tShift += tNoSharedHiddenDim;
        }
        rSaveTo.put("hidden_biases", rHiddenBiases);
        if (mSharedHiddenDims[mNumLayers]==0) {
            rSaveTo.put("output_weight", mNoSharedOutputWeight.asList());
            rSaveTo.put("output_bias", mNoSharedOutputBias.get(0));
        }
    }
    
    @Override public int parameterSize() {
        return mNoSharedHiddenWeightsSize+mNoSharedOutputWeightSize + mNoSharedHiddenBiasesSize+mNoSharedOutputBiasSize;
    }
    public int fittableParameterWeightSize() {
        return mNoSharedHiddenWeightsSize + mNoSharedOutputWeightSize;
    }
    @Override public void mountParameter(Vector aVec) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aVec.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aVec.size()) throw new IllegalArgumentException("data size mismatch");
        }
        int tShift = 0;
        
        IVector oVec = mNoSharedHiddenWeights;
        mNoSharedHiddenWeights = aVec.subVec(tShift, tShift+mNoSharedHiddenWeightsSize);
        tShift += mNoSharedHiddenWeightsSize;
        mNoSharedHiddenWeights.fill(oVec);
        
        oVec = mNoSharedOutputWeight;
        mNoSharedOutputWeight = aVec.subVec(tShift, tShift+mNoSharedOutputWeightSize);
        tShift += mNoSharedOutputWeightSize;
        mNoSharedOutputWeight.fill(oVec);
        
        oVec = mNoSharedHiddenBiases;
        mNoSharedHiddenBiases = aVec.subVec(tShift, tShift+mNoSharedHiddenBiasesSize);
        tShift += mNoSharedHiddenBiasesSize;
        mNoSharedHiddenBiases.fill(oVec);
        
        oVec = mNoSharedOutputBias;
        mNoSharedOutputBias = aVec.subVec(tShift, tShift+mNoSharedOutputBiasSize);
        mNoSharedOutputBias.fill(oVec);
    }
    @Override public void mountGradParameter(Vector aVec) {
        if (mInternalGradHiddenWeights==null) throw new IllegalStateException("invoke `requireGrad(nthreads)` first.");
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aVec.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aVec.size()) throw new IllegalArgumentException("data size mismatch");
        }
        int tShift = 0;
        mGradNoSharedHiddenWeights = aVec.subVec(tShift, tShift+mNoSharedHiddenWeightsSize);
        tShift += mNoSharedHiddenWeightsSize;
        mGradNoSharedOutputWeight = aVec.subVec(tShift, tShift+mNoSharedOutputWeightSize);
        tShift += mNoSharedOutputWeightSize;
        mGradNoSharedHiddenBiases = aVec.subVec(tShift, tShift+mNoSharedHiddenBiasesSize);
        tShift += mNoSharedHiddenBiasesSize;
        mGradNoSharedOutputBias = aVec.subVec(tShift, tShift+mNoSharedOutputBiasSize);
    }
    
    @Override public int cptrParameterSize() {
        return mBase.cptrParameterSize();
    }
    public int inputSize() {
        return mInputDim;
    }
    @Override public void mountCptrParameter(IDoubleOrFloatCPointer aPtr) {
        mInternalHiddenWeights = aPtr.copy();
        mInternalOutputWeight = mInternalHiddenWeights.plus(mBase.mHiddenWeightsSize);
        mInternalHiddenBiases = mInternalOutputWeight.plus(mBase.mOutputWeightSize);
        mInternalOutputBias = mInternalHiddenBiases.plus(mBase.mHiddenBiasesSize);
        updateParameters_();
    }
    @Override public void mountGradCptrParameter(int aThreadID, IDoubleOrFloatCPointer aPtr) {
        if (mInternalGradHiddenWeights==null) throw new IllegalStateException("invoke `requireGrad(nthreads)` first.");
        mInternalGradHiddenWeights[aThreadID] = aPtr.copy();
        mInternalGradOutputWeight[aThreadID] = mInternalGradHiddenWeights[aThreadID].plus(mBase.mHiddenWeightsSize);
        mInternalGradHiddenBiases[aThreadID] = mInternalGradOutputWeight[aThreadID].plus(mBase.mOutputWeightSize);
        mInternalGradOutputBias[aThreadID] = mInternalGradHiddenBiases[aThreadID].plus(mBase.mHiddenBiasesSize);
    }
    
    @Override public int forwardCacheSize() {
        return mBase.forwardCacheSize();
    }
    @Override public int backwardCacheSize() {
        return mBase.backwardCacheSize();
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        mBase.updateGenMap(rGenMap, aGenIdx);
    }
    @Override public boolean hasSameGenMap(NeuralNetwork2 aNN) {
        return mBase.hasSameGenMap(aNN);
    }
}
