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
 * 简单的前馈神经网络 (FFNN) 实现，原生实现在非 batch 的情况下可以有最快的性能
 * <p>
 * 仅支持输出单个数值的网络
 *
 * @author liqa
 */
public class FeedForward2 extends NeuralNetwork2 {
    final int mInputDim;
    final int[] mHiddenDims;
    DoubleArrayVector mHiddenWeights, mHiddenBiases;
    DoubleArrayVector mOutputWeight, mOutputBias;
    DoubleArrayVector mGradHiddenWeights = null, mGradHiddenBiases = null;
    DoubleArrayVector mGradOutputWeight = null, mGradOutputBias = null;
    final int mNumLayers, mHiddenWeightsSize, mHiddenBiasesSize, mOutputWeightSize;
    
    private IDoubleOrFloatCPointer mInternalHiddenWeights = null, mInternalHiddenBiases = null;
    private IDoubleOrFloatCPointer mInternalOutputWeight = null, mInternalOutputBias = null;
    private IDoubleOrFloatCPointer mInternalGradHiddenWeights = null, mInternalGradHiddenBiases = null;
    private IDoubleOrFloatCPointer mInternalGradOutputWeight = null, mInternalGradOutputBias = null;
    
    FeedForward2(int aInputDim, int[] aHiddenDims, Vector aHiddenWeights, Vector aHiddenBiases, Vector aOutputWeight, Vector aOutputBias) {
        mInputDim = aInputDim;
        mHiddenDims = aHiddenDims;
        mNumLayers = aHiddenDims.length;
        if (mNumLayers == 0) throw new IllegalArgumentException("At least one hidden layer is required");
        int tHiddenWeightsSize = 0;
        int tHiddenBiasesSize = 0;
        int tColNum = aInputDim;
        for (int tHiddenDim : aHiddenDims) {
            tHiddenWeightsSize += tColNum * tHiddenDim;
            tHiddenBiasesSize += tHiddenDim;
            tColNum = tHiddenDim;
        }
        mHiddenWeightsSize = tHiddenWeightsSize;
        mHiddenBiasesSize = tHiddenBiasesSize;
        mHiddenWeights = aHiddenWeights==null ? Vectors.zeros(mHiddenWeightsSize) : aHiddenWeights;
        mHiddenBiases = aHiddenBiases==null ? Vectors.zeros(mHiddenBiasesSize) : aHiddenBiases;
        if (mHiddenWeights.size() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of hidden weights mismatch");
        if (mHiddenBiases.size() != mHiddenBiasesSize) throw new IllegalArgumentException("The size of hidden biases mismatch");
        mOutputWeightSize = mHiddenDims[mNumLayers -1];
        mOutputWeight = aOutputWeight==null ? Vectors.zeros(mOutputWeightSize) : aOutputWeight;
        mOutputBias = aOutputBias==null ? Vectors.zeros(1) : aOutputBias;
        if (mOutputWeight.size() != mOutputWeightSize) throw new IllegalArgumentException("The size of output weight mismatch");
        if (mOutputBias.size() != 1) throw new IllegalArgumentException("The size of output biases mismatch");
    }
    
    @Override public void updateParameters() {
        updateParameters_();
    }
    @Override public void backwardParameter() {
        if (mGradHiddenWeights==null) throw new IllegalStateException();
        for (int i = 0; i < mHiddenWeightsSize; ++i) {
            mGradHiddenWeights.add(i, mInternalGradHiddenWeights.getAtD(i));
        }
        for (int i = 0; i < mOutputWeightSize; ++i) {
            mGradOutputWeight.add(i, mInternalGradOutputWeight.getAtD(i));
        }
        for (int i = 0; i < mHiddenBiasesSize; ++i) {
            mGradHiddenBiases.add(i, mInternalGradHiddenBiases.getAtD(i));
        }
        mGradOutputBias.add(0, mInternalGradOutputBias.getD());
    }
    final void updateParameters_() {
        mInternalHiddenWeights.fillD(mHiddenWeights);
        mInternalOutputWeight.fillD(mOutputWeight);
        mInternalHiddenBiases.fillD(mHiddenBiases);
        mInternalOutputBias.setD(mOutputBias.get(0));
    }
    
    @Override public void initParameters() {
        int tColNum = mInputDim;
        int tShift = 0;
        for (int tHiddenDim : mHiddenDims) {
            int tSize = tHiddenDim*tColNum;
            double tBound = MathEX.Fast.sqrt(6.0 / tColNum); // Kaiming 均匀初始化
            mHiddenWeights.subVec(tShift, tShift+tSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        tShift = 0;
        tColNum = mInputDim;
        for (int tHiddenDim : mHiddenDims) {
            double tBound = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
            mHiddenBiases.subVec(tShift, tShift+tHiddenDim).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tHiddenDim;
            tColNum = tHiddenDim;
        }
        double tBound = MathEX.Fast.sqrt(3.0 / tColNum); // Kaiming 均匀初始化，注意输出层没有激活函数因此权重需要调整
        mOutputWeight.assign(() -> RANDOM.nextDouble(-tBound, tBound));
        double tBoundB = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
        mOutputBias.set(0, RANDOM.nextDouble(-tBoundB, tBoundB));
        updateParameters_();
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static FeedForward2 load(Map aMap) {
        int aInputDim = ((Number)UT.Code.get(aMap, "input_dim")).intValue();
        List<?> tHiddenDims = (List<?>)UT.Code.get(aMap, "hidden_dims");
        int[] aHiddenDims = new int[tHiddenDims.size()];
        for (int i = 0; i < aHiddenDims.length; ++i) {
            aHiddenDims[i] = ((Number)tHiddenDims.get(i)).intValue();
        }
        int tNumLayers = aHiddenDims.length;
        if (tNumLayers == 0) throw new IllegalArgumentException("At least one hidden layer is required");
        int tHiddenWeightsSize = 0;
        int tHiddenBiasesSize = 0;
        int tColNum = aInputDim;
        for (int tHiddenDim : aHiddenDims) {
            tHiddenWeightsSize += tColNum * tHiddenDim;
            tHiddenBiasesSize += tHiddenDim;
            tColNum = tHiddenDim;
        }
        List<?> tHiddenWeights = (List<?>)UT.Code.get(aMap, "hidden_weights");
        if (tHiddenWeights.size() != tNumLayers) throw new IllegalArgumentException("The number of hidden weights mismatch");
        Vector aHiddenWeights = Vectors.zeros(tHiddenWeightsSize);
        tColNum = aInputDim;
        int tShift = 0;
        for (int i = 0; i < tNumLayers; ++i) {
            int tHiddenDim = aHiddenDims[i];
            int tSize = tHiddenDim*tColNum;
            RowMatrix tWeight = Matrices.fromRows((List<?>)tHiddenWeights.get(i));
            if (tWeight.ncols() != tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
            if (tWeight.nrows() != tHiddenDim) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
            aHiddenWeights.subVec(tShift, tShift+tSize).fill(tWeight.asVecRow());
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        List<?> tHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        if (tHiddenBiases.size() != tNumLayers) throw new IllegalArgumentException("The number of hidden biases mismatch");
        Vector aHiddenBiases = Vectors.zeros(tHiddenBiasesSize);
        tShift = 0;
        for (int i = 0; i < tNumLayers; ++i) {
            int tHiddenDim = aHiddenDims[i];
            Vector tBias = Vectors.from((List<? extends Number>)tHiddenBiases.get(i));
            if (tBias.size() != tHiddenDim) throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch");
            aHiddenBiases.subVec(tShift, tShift+tHiddenDim).fill(tBias);
            tShift += tHiddenDim;
        }
        Vector aOutputWeight = Vectors.from((List<? extends Number>)aMap.get("output_weight"));
        Vector aOutputBias = Vectors.zeros(1);
        aOutputBias.set(0, ((Number)aMap.get("output_bias")).doubleValue());
        if (aOutputWeight.size() != aHiddenDims[tNumLayers-1]) throw new IllegalArgumentException("Size of output weight mismatch");
        
        return new FeedForward2(aInputDim, aHiddenDims, aHiddenWeights, aHiddenBiases, aOutputWeight, aOutputBias);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "feed_forward");
        rSaveTo.put("input_dim", mInputDim);
        rSaveTo.put("hidden_dims", AbstractCollections.from(mHiddenDims));
        List<List<List<Double>>> rHiddenWeights = new ArrayList<>(mNumLayers);
        int tColNum = mInputDim;
        int tShift = 0;
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mHiddenDims[i];
            List<List<Double>> tWeights = new ArrayList<>(tHiddenDim);
            rHiddenWeights.add(tWeights);
            for (int j = 0; j < tHiddenDim; ++j) {
                tWeights.add(mHiddenWeights.subVec(tShift, tShift+tColNum).asList());
                tShift += tColNum;
            }
            tColNum = tHiddenDim;
        }
        rSaveTo.put("hidden_weights", rHiddenWeights);
        List<List<Double>> rHiddenBiases = new ArrayList<>(mNumLayers);
        tShift = 0;
        for (int i = 0; i < mNumLayers; ++i) {
            int tHiddenDim = mHiddenDims[i];
            rHiddenBiases.add(mHiddenBiases.subVec(tShift, tShift+tHiddenDim).asList());
            tShift += tHiddenDim;
        }
        rSaveTo.put("hidden_biases", rHiddenBiases);
        rSaveTo.put("output_weight", mOutputWeight.asList());
        rSaveTo.put("output_bias", mOutputBias.get(0));
    }
    
    @Override public int parameterSize() {
        return mHiddenWeightsSize+mOutputWeightSize + mHiddenBiasesSize+1;
    }
    public int fittableParameterWeightSize() {
        return mHiddenWeightsSize+mOutputWeightSize;
    }
    @Override public void mountParameter(IDataShell<double[]> aData) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        }
        double[] tData = aData.internalData();
        int tShift = aData.internalDataShift();
        
        IVector oVec = mHiddenWeights;
        mHiddenWeights = new ShiftVector(mHiddenWeightsSize, tShift, tData);
        tShift += mHiddenWeightsSize;
        mHiddenWeights.fill(oVec);
        
        oVec = mOutputWeight;
        mOutputWeight = new ShiftVector(mOutputWeightSize, tShift, tData);
        tShift += mOutputWeightSize;
        mOutputWeight.fill(oVec);
        
        oVec = mHiddenBiases;
        mHiddenBiases = new ShiftVector(mHiddenBiasesSize, tShift, tData);
        tShift += mHiddenBiasesSize;
        mHiddenBiases.fill(oVec);
        
        double tOutputBias = mOutputBias.get(0);
        mOutputBias = new ShiftVector(1, tShift, tData);
        mOutputBias.set(0, tOutputBias);
    }
    @Override public void mountGradParameter(IDataShell<double[]> aData) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        }
        double[] tData = aData.internalData();
        int tShift = aData.internalDataShift();
        mGradHiddenWeights = new ShiftVector(mHiddenWeightsSize, tShift, tData);
        tShift += mHiddenWeightsSize;
        mGradOutputWeight = new ShiftVector(mOutputWeightSize, tShift, tData);
        tShift += mOutputWeightSize;
        mGradHiddenBiases = new ShiftVector(mHiddenBiasesSize, tShift, tData);
        tShift += mHiddenBiasesSize;
        mGradOutputBias = new ShiftVector(1, tShift, tData);
    }
    
    @Override public int cptrParameterSize() {
        return parameterSize();
    }
    public int inputSize() {
        return mInputDim;
    }
    @Override public void mountCptrParameter(IDoubleOrFloatCPointer aPtr) {
        mInternalHiddenWeights = aPtr.copy();
        mInternalOutputWeight = mInternalHiddenWeights.plus(mHiddenWeightsSize);
        mInternalHiddenBiases = mInternalOutputWeight.plus(mOutputWeightSize);
        mInternalOutputBias = mInternalHiddenBiases.plus(mHiddenBiasesSize);
        updateParameters_();
    }
    @Override public void mountGradCptrParameter(IDoubleOrFloatCPointer aPtr) {
        mInternalGradHiddenWeights = aPtr.copy();
        mInternalGradOutputWeight = mInternalGradHiddenWeights.plus(mHiddenWeightsSize);
        mInternalGradHiddenBiases = mInternalGradOutputWeight.plus(mOutputWeightSize);
        mInternalGradOutputBias = mInternalGradHiddenBiases.plus(mHiddenBiasesSize);
    }
    
    @Override public int forwardCacheSize() {
        return mInputDim+mHiddenBiasesSize + mHiddenBiasesSize;
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put("[NN USE "+aGenIdx+"]", "feed_forward");
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_IN", mInputDim);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_HW", mHiddenWeightsSize);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_HB", mHiddenBiasesSize);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_OW", mOutputWeightSize);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_NLAYERS", mNumLayers);
        rGenMap.put("[NN HIDDEN LAYERS "+aGenIdx+"]", mNumLayers);
        int tInSize = mInputDim;
        for (int i = 0; i < mNumLayers; ++i) {
            int tOutSize = mHiddenDims[i];
            rGenMap.put(aGenIdx+":"+i+":NNAPGEN_NN_IN_SIZE", tInSize);
            rGenMap.put(aGenIdx+":"+i+":NNAPGEN_NN_OUT_SIZE", tOutSize);
            tInSize = tOutSize;
        }
    }
    
    @Override public boolean hasSameGenMap(NeuralNetwork2 aNN) {
        if (aNN instanceof SharedFeedForward2) return hasSameGenMap(((SharedFeedForward2)aNN).sharedNeuralNetwork());
        if (!(aNN instanceof FeedForward2)) return false;
        FeedForward2 tNN = (FeedForward2)aNN;
        if (mNumLayers !=tNN.mNumLayers || mInputDim!=tNN.mInputDim) return false;
        for (int i = 0; i < mNumLayers; ++i) {
            if (mHiddenDims[i]!=tNN.mHiddenDims[i]) return false;
        }
        return true;
    }
}
