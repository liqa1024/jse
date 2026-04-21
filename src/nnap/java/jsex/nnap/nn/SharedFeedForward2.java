package jsex.nnap.nn;

import jse.code.Conf;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

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
    private ShiftVector mNoSharedHiddenWeights, mNoSharedHiddenBiases;
    private ShiftVector mNoSharedOutputWeight, mNoSharedOutputBias;
    private ShiftVector mGradNoSharedHiddenWeights = null, mGradNoSharedHiddenBiases = null;
    private ShiftVector mGradNoSharedOutputWeight = null, mGradNoSharedOutputBias = null;
    private final int mNumLayers;
    private final int mNoSharedHiddenWeightsSize;
    private final int mNoSharedHiddenBiasesSize;
    private final int mNoSharedOutputWeightSize, mNoSharedOutputBiasSize;
    
    private IDoubleOrFloatCPointer mInternalHiddenWeights = null, mInternalHiddenBiases = null;
    private IDoubleOrFloatCPointer mInternalOutputWeight = null, mInternalOutputBias = null;
    private IDoubleOrFloatCPointer mInternalGradHiddenWeights = null, mInternalGradHiddenBiases = null;
    private IDoubleOrFloatCPointer mInternalGradOutputWeight = null, mInternalGradOutputBias = null;
    
    SharedFeedForward2(int aInputDim, FeedForward2 aBase, int aSharedType, int[] aSharedHiddenDims, ShiftVector aNoSharedHiddenWeights, ShiftVector aNoSharedHiddenBiases, ShiftVector aNoSharedOutputWeight, ShiftVector aNoSharedOutputBias) {
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
        mNoSharedHiddenWeights = aNoSharedHiddenWeights==null ? new ShiftVector(0, new double[mNoSharedHiddenWeightsSize]) : aNoSharedHiddenWeights;
        mNoSharedHiddenBiases = aNoSharedHiddenBiases==null ? new ShiftVector(0, new double[mNoSharedHiddenBiasesSize]) : aNoSharedHiddenBiases;
        if (mNoSharedHiddenWeights.size() != mNoSharedHiddenWeightsSize) throw new IllegalArgumentException("The size of hidden weights mismatch");
        if (mNoSharedHiddenBiases.size() != mNoSharedHiddenBiasesSize) throw new IllegalArgumentException("The size of hidden biases mismatch");
        int tSharedOutputDim = mSharedHiddenDims[mNumLayers];
        if (tSharedOutputDim<0 || tSharedOutputDim>1) throw new IllegalArgumentException("invalid shared dimension: "+tSharedOutputDim+" vs "+1);
        mNoSharedOutputWeightSize = tSharedOutputDim==1 ? 0 : mBase.mHiddenDims[mNumLayers -1];
        mNoSharedOutputBiasSize = tSharedOutputDim==1 ? 0 : 1;
        mNoSharedOutputWeight = aNoSharedOutputWeight==null ? new ShiftVector(0, new double[mNoSharedOutputWeightSize]) : aNoSharedOutputWeight;
        mNoSharedOutputBias = aNoSharedOutputBias==null ?  new ShiftVector(0, new double[mNoSharedOutputBiasSize]) : aNoSharedOutputBias;
        if (mNoSharedOutputWeight.size() != mNoSharedOutputWeightSize) throw new IllegalArgumentException("The size of output weight mismatch");
        if (mNoSharedOutputBias.size() != mNoSharedOutputBiasSize) throw new IllegalArgumentException("The size of output biases mismatch");
    }
    
    @Override public void updateParameters() {
        updateParameters_();
    }
    @Override public void backwardParameter() {
        if (mGradNoSharedHiddenWeights==null) throw new IllegalStateException();
        int tColNum = mInputDim;
        int tShift = 0, tNoSharedShift = 0;
        IDoubleOrFloatCPointer tPtr = mInternalGradHiddenWeights.copy();
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
        tPtr = mInternalGradHiddenBiases.copy();
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
                mGradNoSharedOutputWeight.add(j, mInternalGradOutputWeight.getAtD(j));
            }
            mGradNoSharedOutputBias.add(0, mInternalGradOutputBias.getD());
        } else {
            for (int j = 0; j < mBase.mOutputWeightSize; ++j) {
                mBase.mGradOutputWeight.add(j, mInternalGradOutputWeight.getAtD(j));
            }
            mBase.mGradOutputBias.add(0, mInternalGradOutputBias.getD());
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
    static SharedFeedForward2 load_(NeuralNetwork2[] aNN, Map aMap) {
        Object tShare = aMap.get("share");
        if (tShare == null) throw new IllegalArgumentException("Key `share` required for shared_feed_forward");
        int tSharedType = ((Number)tShare).intValue();
        FeedForward2 tBase = (FeedForward2)aNN[tSharedType-1];
        // 旧版分层 share 情况现在不再支持
        if (aMap.get("shared_flags") != null) {
            throw new IllegalArgumentException("shared in layer nn is invalid now");
        }
        int aInputDim = ((Number)UT.Code.get(aMap, "input_dim")).intValue();
        List<?> tSharedHiddenDims = (List<?>)UT.Code.get(aMap, "shared_hidden_dims");
        int[] aSharedHiddenDims = new int[tSharedHiddenDims.size()];
        for (int i = 0; i < aSharedHiddenDims.length; ++i) {
            aSharedHiddenDims[i] = ((Number)tSharedHiddenDims.get(i)).intValue();
        }
        // 最后值缺省支持
        if (aSharedHiddenDims.length == tBase.mNumLayers) {
            int[] oSharedHiddenDims = aSharedHiddenDims;
            aSharedHiddenDims = new int[oSharedHiddenDims.length+1];
            System.arraycopy(oSharedHiddenDims, 0, aSharedHiddenDims, 0, oSharedHiddenDims.length);
        }
        int tNumLayers = aSharedHiddenDims.length-1;
        if (tNumLayers != tBase.mNumLayers) throw new IllegalArgumentException("Hidden number mismatch");
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
        List<?> tNoSharedHiddenWeights = (List<?>)UT.Code.get(aMap, "hidden_weights");
        if (tNoSharedHiddenWeights.size() != tNumLayers) throw new IllegalArgumentException("The number of hidden weights mismatch");
        ShiftVector aNoSharedHiddenWeights = new ShiftVector(0, new double[tNoSharedHiddenWeightsSize]);
        tColNum = aInputDim;
        int tShift = 0;
        for (int i = 0; i < tNumLayers; ++i) {
            int tHiddenDim = tBase.mHiddenDims[i];
            int tSharedHiddenDim = aSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            int tSize = tNoSharedHiddenDim*tColNum;
            RowMatrix tWeight = Matrices.fromRows((List<?>)tNoSharedHiddenWeights.get(i));
            if (tWeight.nrows() != tNoSharedHiddenDim) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
            if (tNoSharedHiddenDim>0 && tWeight.ncols()!=tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
            aNoSharedHiddenWeights.subVec(tShift, tShift+tSize).fill(tWeight.asVecRow());
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        List<?> tNoSharedHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        if (tNoSharedHiddenBiases.size() != tNumLayers) throw new IllegalArgumentException("The number of hidden biases mismatch");
        ShiftVector aNoSharedHiddenBiases = new ShiftVector(0, new double[tNoSharedHiddenBiasesSize]);
        tShift = 0;
        for (int i = 0; i < tNumLayers; ++i) {
            int tHiddenDim = tBase.mHiddenDims[i];
            int tSharedHiddenDim = aSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            Vector tBias = Vectors.from((List<? extends Number>)tNoSharedHiddenBiases.get(i));
            if (tBias.size() != tNoSharedHiddenDim) throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch");
            aNoSharedHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim).fill(tBias);
            tShift += tNoSharedHiddenDim;
        }
        ShiftVector aNoSharedOutputWeight;
        ShiftVector aNoSharedOutputBias;
        if (aSharedHiddenDims[tNumLayers]==1) {
            aNoSharedOutputWeight = new ShiftVector(0, new double[0]);
            aNoSharedOutputBias = new ShiftVector(0, new double[0]);
        } else {
            aNoSharedOutputWeight = new ShiftVector(0, new double[tBase.mHiddenDims[tNumLayers-1]]);
            aNoSharedOutputBias = new ShiftVector(0, new double[1]);
            aNoSharedOutputWeight.fill((List<? extends Number>)aMap.get("output_weight"));
            aNoSharedOutputBias.set(0, ((Number)aMap.get("output_bias")).doubleValue());
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
    @Override public void mountParameter(IDataShell<double[]> aData) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        }
        double[] tData = aData.internalData();
        int tShift = aData.internalDataShift();
        
        IVector oVec = mNoSharedHiddenWeights;
        mNoSharedHiddenWeights = new ShiftVector(mNoSharedHiddenWeightsSize, tShift, tData);
        tShift += mNoSharedHiddenWeightsSize;
        mNoSharedHiddenWeights.fill(oVec);
        
        oVec = mNoSharedOutputWeight;
        mNoSharedOutputWeight = new ShiftVector(mNoSharedOutputWeightSize, tShift, tData);
        tShift += mNoSharedOutputWeightSize;
        mNoSharedOutputWeight.fill(oVec);
        
        oVec = mNoSharedHiddenBiases;
        mNoSharedHiddenBiases = new ShiftVector(mNoSharedHiddenBiasesSize, tShift, tData);
        tShift += mNoSharedHiddenBiasesSize;
        mNoSharedHiddenBiases.fill(oVec);
        
        oVec = mNoSharedOutputBias;
        mNoSharedOutputBias = new ShiftVector(mNoSharedOutputBiasSize, tShift, tData);
        mNoSharedOutputBias.fill(oVec);
    }
    @Override public void mountGradParameter(IDataShell<double[]> aData) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        }
        double[] tData = aData.internalData();
        int tShift = aData.internalDataShift();
        mGradNoSharedHiddenWeights = new ShiftVector(mNoSharedHiddenWeightsSize, tShift, tData);
        tShift += mNoSharedHiddenWeightsSize;
        mGradNoSharedOutputWeight = new ShiftVector(mNoSharedOutputWeightSize, tShift, tData);
        tShift += mNoSharedOutputWeightSize;
        mGradNoSharedHiddenBiases = new ShiftVector(mNoSharedHiddenBiasesSize, tShift, tData);
        tShift += mNoSharedHiddenBiasesSize;
        mGradNoSharedOutputBias = new ShiftVector(mNoSharedOutputBiasSize, tShift, tData);
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
    @Override public void mountGradCptrParameter(IDoubleOrFloatCPointer aPtr) {
        mInternalGradHiddenWeights = aPtr.copy();
        mInternalGradOutputWeight = mInternalGradHiddenWeights.plus(mBase.mHiddenWeightsSize);
        mInternalGradHiddenBiases = mInternalGradOutputWeight.plus(mBase.mOutputWeightSize);
        mInternalGradOutputBias = mInternalGradHiddenBiases.plus(mBase.mHiddenBiasesSize);
    }
    
    @Override public int forwardCacheSize() {
        return mBase.forwardCacheSize();
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        mBase.updateGenMap(rGenMap, aGenIdx);
    }
    @Override public boolean hasSameGenMap(NeuralNetwork2 aNN) {
        return mBase.hasSameGenMap(aNN);
    }
}
