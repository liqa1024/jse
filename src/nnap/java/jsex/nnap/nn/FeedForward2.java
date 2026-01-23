package jsex.nnap.nn;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.io.ISavable;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class FeedForward2 implements ISavable {
    
    final int mThisType;
    final int mInputDim;
    final int[] mHiddenDims;
    final Vector mHiddenWeights, mHiddenWeightsBackward;
    final IntVector mIndexToBackward;
    final Vector mHiddenBiases;
    final Vector mOutputWeight;
    final double[] mOutputBias;
    final int mHiddenNumber, mHiddenWeightsSize, mHiddenBiasesSize, mOutputWeightSize;
    
    private FeedForward2(int aThisType, int aInputDim, int[] aHiddenDims, Vector aHiddenWeights, Vector aHiddenWeightsBackward, IntVector aIndexToBackward, Vector aHiddenBiases, Vector aOutputWeight, double[] aOutputBias) {
        mThisType = aThisType;
        mInputDim = aInputDim;
        mHiddenDims = aHiddenDims;
        mHiddenNumber = aHiddenDims.length;
        if (mHiddenNumber == 0) throw new IllegalArgumentException("At least one hidden layer is required");
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
        mHiddenWeightsBackward = aHiddenWeightsBackward==null ? Vectors.zeros(mHiddenWeightsSize) : aHiddenWeightsBackward;
        mIndexToBackward = aIndexToBackward==null ? IntVector.zeros(mHiddenWeightsSize) : aIndexToBackward;
        mHiddenBiases = aHiddenBiases==null ? Vectors.zeros(mHiddenBiasesSize) : aHiddenBiases;
        if (mHiddenWeights.internalDataSize() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of hidden weights mismatch");
        if (mHiddenWeightsBackward.internalDataSize() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of backward hidden weights mismatch");
        if (mHiddenBiases.internalDataSize() != mHiddenBiasesSize) throw new IllegalArgumentException("The size of hidden biases mismatch");
        mOutputWeightSize = mHiddenDims[mHiddenNumber-1];
        mOutputWeight = aOutputWeight==null ? Vectors.zeros(mOutputWeightSize) : aOutputWeight;
        mOutputBias = aOutputBias==null ? new double[1] : aOutputBias;
        if (mOutputWeight.internalDataSize() != mOutputWeightSize) throw new IllegalArgumentException("The size of output weight mismatch");
        if (mOutputBias.length != 1) throw new IllegalArgumentException("The size of output biases mismatch");
    }
    public FeedForward2(int aThisType, int aInputDim, int[] aHiddenDims, Vector aHiddenWeights, Vector aHiddenBiases, Vector aOutputWeight, double[] aOutputBias) {
        this(aThisType, aInputDim, aHiddenDims, aHiddenWeights, null, null, aHiddenBiases, aOutputWeight, aOutputBias);
        int tColNum = mInputDim;
        int tShift = 0;
        for (int tHiddenDim : mHiddenDims) {
            int tSize = tHiddenDim*tColNum;
            final int fColNum = tColNum;
            final int tShiftB = mHiddenWeightsSize-tShift-tSize;
            mIndexToBackward.subVec(tShift, tShift+tSize).fill(ii -> {
                int row = ii / fColNum;
                int col = ii % fColNum;
                return col*tHiddenDim + row + tShiftB;
            });
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        for (int i = 0; i < mHiddenWeightsSize; ++i) {
            mHiddenWeightsBackward.set(mIndexToBackward.get(i), mHiddenWeights.get(i));
        }
    }
    
    
    public void initParameters() {
        int tColNum = mInputDim;
        int tShift = 0;
        for (int tHiddenDim : mHiddenDims) {
            int tSize = tHiddenDim*tColNum;
            double tBound = MathEX.Fast.sqrt(6.0 / tColNum); // Kaiming 均匀初始化
            mHiddenWeights.subVec(tShift, tShift+tSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        for (int i = 0; i < mHiddenWeightsSize; ++i) {
            mHiddenWeightsBackward.set(mIndexToBackward.get(i), mHiddenWeights.get(i));
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
        mOutputBias[0] = RANDOM.nextDouble(-tBoundB, tBoundB);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static FeedForward2 load(int aThisType, Map aMap) {
        int aInputDim = ((Number)UT.Code.get(aMap, "input_dim")).intValue();
        List<?> tHiddenDims = (List<?>)UT.Code.get(aMap, "hidden_dims");
        int[] aHiddenDims = new int[tHiddenDims.size()];
        for (int i = 0; i < aHiddenDims.length; ++i) {
            aHiddenDims[i] = ((Number)tHiddenDims.get(i)).intValue();
        }
        int tHiddenNumber = aHiddenDims.length;
        if (tHiddenNumber == 0) throw new IllegalArgumentException("At least one hidden layer is required");
        int tHiddenWeightsSize = 0;
        int tHiddenBiasesSize = 0;
        int tColNum = aInputDim;
        for (int tHiddenDim : aHiddenDims) {
            tHiddenWeightsSize += tColNum * tHiddenDim;
            tHiddenBiasesSize += tHiddenDim;
            tColNum = tHiddenDim;
        }
        List<?> tHiddenWeights = (List<?>)UT.Code.get(aMap, "hidden_weights");
        if (tHiddenWeights.size() != tHiddenNumber) throw new IllegalArgumentException("The number of hidden weights mismatch");
        Vector aHiddenWeights = Vectors.zeros(tHiddenWeightsSize);
        tColNum = aInputDim;
        int tShift = 0;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = aHiddenDims[i];
            int tSize = tHiddenDim*tColNum;
            RowMatrix tWeight = Matrices.fromRows((List<?>)tHiddenWeights.get(i));
            if (tWeight.columnNumber() != tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
            if (tWeight.rowNumber() != tHiddenDim) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
            aHiddenWeights.subVec(tShift, tShift+tSize).fill(tWeight.asVecRow());
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        List<?> tHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        if (tHiddenBiases.size() != tHiddenNumber) throw new IllegalArgumentException("The number of hidden biases mismatch");
        Vector aHiddenBiases = Vectors.zeros(tHiddenBiasesSize);
        tShift = 0;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = aHiddenDims[i];
            Vector tBias = Vectors.from((List<? extends Number>)tHiddenBiases.get(i));
            if (tBias.size() != tHiddenDim) throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch");
            aHiddenBiases.subVec(tShift, tShift+tHiddenDim).fill(tBias);
            tShift += tHiddenDim;
        }
        Vector aOutputWeight = Vectors.from((List<? extends Number>)aMap.get("output_weight"));
        double aOutputBias = ((Number)aMap.get("output_bias")).doubleValue();
        if (aOutputWeight.size() != aHiddenDims[tHiddenNumber-1]) throw new IllegalArgumentException("Size of output weight mismatch");
        
        return new FeedForward2(aThisType, aInputDim, aHiddenDims, aHiddenWeights, aHiddenBiases, aOutputWeight, new double[]{aOutputBias});
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "feed_forward");
        rSaveTo.put("input_dim", mInputDim);
        rSaveTo.put("hidden_dims", AbstractCollections.from(mHiddenDims));
        List<List<List<Double>>> rHiddenWeights = new ArrayList<>(mHiddenNumber);
        int tColNum = mInputDim;
        int tShift = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
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
        List<List<Double>> rHiddenBiases = new ArrayList<>(mHiddenNumber);
        tShift = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mHiddenDims[i];
            rHiddenBiases.add(mHiddenBiases.subVec(tShift, tShift+tHiddenDim).asList());
            tShift += tHiddenDim;
        }
        rSaveTo.put("hidden_biases", rHiddenBiases);
        rSaveTo.put("output_weight", mOutputWeight.asList());
        rSaveTo.put("output_bias", mOutputBias[0]);
    }
    
    public int parameterSize() {
        return mHiddenWeightsSize+mOutputWeightSize + mHiddenBiasesSize+1;
    }
    public int parameterWeightSize() {
        return mHiddenWeightsSize+mOutputWeightSize;
    }
    public int hiddenSize() {
        return mHiddenBiasesSize;
    }
    
    public IVector parameters() {
        final int tEndHW = mHiddenWeightsSize;
        final int tEndOW = tEndHW + mOutputWeightSize;
        final int tEndHB = tEndOW + mHiddenBiasesSize;
        final int tEndOB = tEndHB + 1;
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx < tEndHW) {
                    return mHiddenWeights.get(aIdx);
                } else
                if (aIdx < tEndOW) {
                    return mOutputWeight.get(aIdx-tEndHW);
                } else
                if (aIdx < tEndHB) {
                    return mHiddenBiases.get(aIdx-tEndOW);
                } else
                if (aIdx < tEndOB) {
                    return mOutputBias[0];
                } else {
                    throw new IndexOutOfBoundsException(String.valueOf(aIdx));
                }
            }
            @Override public void set(int aIdx, double aValue) {
                if (aIdx < tEndHW) {
                    mHiddenWeights.set(aIdx, aValue);
                    mHiddenWeightsBackward.set(mIndexToBackward.get(aIdx), aValue);
                } else
                if (aIdx < tEndOW) {
                    mOutputWeight.set(aIdx-tEndHW, aValue);
                } else
                if (aIdx < tEndHB) {
                    mHiddenBiases.set(aIdx-tEndOW, aValue);
                } else
                if (aIdx < tEndOB) {
                    mOutputBias[0] = aValue;
                } else {
                    throw new IndexOutOfBoundsException(String.valueOf(aIdx));
                }
            }
            @Override public int size() {
                return tEndOB;
            }
        };
    }
    
    public int inputSize() {
        return mInputDim;
    }
    
    public int forwardCacheSize(int aCacheLevel) {
        switch(aCacheLevel) {
        case 0: {return mHiddenBiasesSize;}
        case 1: {return mHiddenBiasesSize + mHiddenBiasesSize;}
        case 2: {return mHiddenBiasesSize + mHiddenBiasesSize + mHiddenBiasesSize;}
        default: {throw new IllegalStateException();}
        }
    }
    
    public void updateGenMap(Map<String, Object> rGenMap) {
        int ti = mThisType-1;
        rGenMap.put(ti+":NNAPGEN_NN_SIZE_HW", mHiddenWeightsSize);
        rGenMap.put(ti+":NNAPGEN_NN_SIZE_HB", mHiddenBiasesSize);
        rGenMap.put(ti+":NNAPGEN_NN_SIZE_OW", mOutputWeightSize);
        rGenMap.put("[NN HIDDEN LAYERS "+mThisType+"]", mHiddenNumber-1);
        int tInSize = mInputDim;
        for (int i = 0; i < mHiddenNumber-1; ++i) {
            int tOutSize = mHiddenDims[i];
            rGenMap.put(ti+":"+i+":NNAPGEN_NN_IN_SIZE_H", tInSize);
            rGenMap.put(ti+":"+i+":NNAPGEN_NN_OUT_SIZE_H", tOutSize);
            tInSize = tOutSize;
        }
        rGenMap.put(ti+":NNAPGEN_NN_IN_SIZE_O", tInSize);
        rGenMap.put(ti+":NNAPGEN_NN_OUT_SIZE_O", mHiddenDims[mHiddenNumber-1]);
    }
}
