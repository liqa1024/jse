package jsex.nnap.nn;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;
import static jse.code.CS.ZL_DOUBLE;

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
    private final FeedForward2 mShare;
    private final int mSharedType;
    public FeedForward2 sharedNeuralNetwork() {return mShare;}
    public int sharedType() {return mSharedType;}
    
    private final int mInputDim;
    private final int[] mSharedHiddenDims;
    private final Vector mHiddenWeights;
    private final Vector mHiddenBiases;
    private final Vector mOutputWeight;
    private final double[] mOutputBias;
    private final int mHiddenNumber, mHiddenWeightsSize, mSharedHiddenWeightsSize, mHiddenBiasesSize, mSharedHiddenBiasesSize, mOutputWeightSize, mOutputBiasSize;
    
    SharedFeedForward2(int aInputDim, FeedForward2 aSharedFeedForward, int aSharedType, int[] aSharedHiddenDims, Vector aHiddenWeights, Vector aHiddenBiases, Vector aOutputWeight, double[] aOutputBias) {
        mInputDim = aInputDim;
        mShare = aSharedFeedForward;
        mSharedType = aSharedType;
        // 最后值缺省支持
        if (aSharedHiddenDims.length == mShare.mHiddenNumber) {
            int[] oSharedHiddenDims = aSharedHiddenDims;
            aSharedHiddenDims = new int[oSharedHiddenDims.length+1];
            System.arraycopy(oSharedHiddenDims, 0, aSharedHiddenDims, 0, oSharedHiddenDims.length);
        }
        mSharedHiddenDims = aSharedHiddenDims;
        mHiddenNumber = mSharedHiddenDims.length-1;
        if (mHiddenNumber != mShare.mHiddenNumber) throw new IllegalArgumentException("Hidden number mismatch");
        if (mInputDim != mShare.mInputDim) throw new IllegalArgumentException("Input dimensions mismatch for shared nn");
        int tHiddenWeightsSize = 0, tSharedHiddenWeightsSize = 0;
        int tHiddenBiasesSize = 0, tSharedHiddenBiasesSize = 0;
        int tColNum = mInputDim;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            if (tSharedHiddenDim<0 || tSharedHiddenDim>tHiddenDim) throw new IllegalArgumentException("invalid shared dimension: "+tSharedHiddenDim+" vs "+tHiddenDim);
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            tHiddenWeightsSize += tColNum * tNoSharedHiddenDim;
            tHiddenBiasesSize += tNoSharedHiddenDim;
            tSharedHiddenWeightsSize += tColNum * tSharedHiddenDim;
            tSharedHiddenBiasesSize += tSharedHiddenDim;
            tColNum = tHiddenDim;
        }
        mHiddenWeightsSize = tHiddenWeightsSize; mSharedHiddenWeightsSize = tSharedHiddenWeightsSize;
        mHiddenBiasesSize = tHiddenBiasesSize; mSharedHiddenBiasesSize = tSharedHiddenBiasesSize;
        mHiddenWeights = aHiddenWeights==null ? Vectors.zeros(mHiddenWeightsSize) : aHiddenWeights;
        mHiddenBiases = aHiddenBiases==null ? Vectors.zeros(mHiddenBiasesSize) : aHiddenBiases;
        if (mHiddenWeights.internalDataSize() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of hidden weights mismatch");
        if (mHiddenBiases.internalDataSize() != mHiddenBiasesSize) throw new IllegalArgumentException("The size of hidden biases mismatch");
        int tSharedOutputDim = mSharedHiddenDims[mHiddenNumber];
        if (tSharedOutputDim<0 || tSharedOutputDim>1) throw new IllegalArgumentException("invalid shared dimension: "+tSharedOutputDim+" vs "+1);
        mOutputWeightSize = tSharedOutputDim==1 ? 0 : mShare.mHiddenDims[mHiddenNumber-1];
        mOutputBiasSize = tSharedOutputDim==1 ? 0 : 1;
        mOutputWeight = aOutputWeight==null ? Vectors.zeros(mOutputWeightSize) : aOutputWeight;
        mOutputBias = aOutputBias==null ? new double[mOutputBiasSize] : aOutputBias;
        if (mOutputWeight.internalDataSize() != mOutputWeightSize) throw new IllegalArgumentException("The size of output weight mismatch");
        if (mOutputBias.length != mOutputBiasSize) throw new IllegalArgumentException("The size of output biases mismatch");
    }
    
    /** 拷贝参数从而转换成简单的 FeedForward 形式，从而可以加速神经网络的推理部分；会存在部分参数的引用因此进行参数修改是未定义的行为 */
    public FeedForward2 toFeedForward() {
        Vector aHiddenWeights = mShare.mHiddenWeights.copy();
        Vector aHiddenBiases = mShare.mHiddenBiases.copy();
        Vector aOutputWeight = mShare.mOutputWeight.copy();
        double aOutputBias = mShare.mOutputBias[0];
        int tColNum = mInputDim;
        int tShift = 0, tShiftShare = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            // shared last
            int tSize = tNoSharedHiddenDim*tColNum;
            aHiddenWeights.subVec(tShiftShare, tShiftShare+tSize).fill(mHiddenWeights.subVec(tShift, tShift+tSize));
            tShift += tSize;
            tShiftShare += tHiddenDim*tColNum;
            tColNum = tHiddenDim;
        }
        tShift = 0; tShiftShare = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            // shared last
            aHiddenBiases.subVec(tShiftShare, tShiftShare+tNoSharedHiddenDim).fill(mHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim));
            tShift += tNoSharedHiddenDim;
            tShiftShare += tHiddenDim;
        }
        if (mSharedHiddenDims[mHiddenNumber]==0) {
            aOutputWeight.fill(mOutputWeight);
            aOutputBias = mOutputBias[0];
        }
        return new FeedForward2(mInputDim, mShare.mHiddenDims, aHiddenWeights, aHiddenBiases, aOutputWeight, new double[]{aOutputBias});
    }
    
    // 这里包含时只处理实际计算流中涉及的参数部分
    public void initParameters(boolean aIncludeShare) {
        int tColNum = mInputDim;
        int tShift = 0, tShiftShare = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            double tBound = MathEX.Fast.sqrt(6.0 / tColNum); // Kaiming 均匀初始化
            int tSize = tNoSharedHiddenDim*tColNum;
            mHiddenWeights.subVec(tShift, tShift+tSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tSize;
            if (aIncludeShare) {
                tShiftShare += tSize;
                int tSharedSize = tSharedHiddenDim*tColNum;
                mShare.mHiddenWeights.subVec(tShiftShare, tShiftShare+tSharedSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
                tShiftShare += tSharedSize; // shared last
            }
            tColNum = tHiddenDim;
        }
        tShift = 0; tShiftShare = 0;
        tColNum = mInputDim;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            double tBound = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
            mHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tNoSharedHiddenDim;
            if (aIncludeShare) {
                tShiftShare += tNoSharedHiddenDim;
                mShare.mHiddenBiases.subVec(tShiftShare, tShiftShare+tSharedHiddenDim).assign(() -> RANDOM.nextDouble(-tBound, tBound));
                tShiftShare += tSharedHiddenDim; // shared last
            }
            tColNum = tHiddenDim;
        }
        double tBound = MathEX.Fast.sqrt(3.0 / tColNum); // Kaiming 均匀初始化，注意输出层没有激活函数因此权重需要调整
        double tBoundB = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
        if (mSharedHiddenDims[mHiddenNumber]==0) {
            mOutputWeight.assign(() -> RANDOM.nextDouble(-tBound, tBound));
            mOutputBias[0] = RANDOM.nextDouble(-tBoundB, tBoundB);
        } else
        if (aIncludeShare) {
            mShare.mOutputWeight.assign(() -> RANDOM.nextDouble(-tBound, tBound));
            mShare.mOutputBias[0] = RANDOM.nextDouble(-tBoundB, tBoundB);
        }
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static SharedFeedForward2 load_(NeuralNetwork2[] aNN, Map aMap) {
        Object tShare = aMap.get("share");
        if (tShare == null) throw new IllegalArgumentException("Key `share` required for shared_feed_forward");
        int tSharedType = ((Number)tShare).intValue();
        FeedForward2 tSharedNN = (FeedForward2)aNN[tSharedType-1];
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
        if (aSharedHiddenDims.length == tSharedNN.mHiddenNumber) {
            int[] oSharedHiddenDims = aSharedHiddenDims;
            aSharedHiddenDims = new int[oSharedHiddenDims.length+1];
            System.arraycopy(oSharedHiddenDims, 0, aSharedHiddenDims, 0, oSharedHiddenDims.length);
        }
        int tHiddenNumber = aSharedHiddenDims.length-1;
        if (tHiddenNumber != tSharedNN.mHiddenNumber) throw new IllegalArgumentException("Hidden number mismatch");
        int tHiddenWeightsSize = 0;
        int tHiddenBiasesSize = 0;
        int tColNum = aInputDim;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = tSharedNN.mHiddenDims[i];
            int tSharedHiddenDim = aSharedHiddenDims[i];
            if (tSharedHiddenDim<0 || tSharedHiddenDim>tHiddenDim) throw new IllegalArgumentException("invalid shared dimension: "+tSharedHiddenDim+" vs "+tHiddenDim);
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            tHiddenWeightsSize += tColNum * tNoSharedHiddenDim;
            tHiddenBiasesSize += tNoSharedHiddenDim;
            tColNum = tHiddenDim;
        }
        List<?> tHiddenWeights = (List<?>)UT.Code.get(aMap, "hidden_weights");
        if (tHiddenWeights.size() != tHiddenNumber) throw new IllegalArgumentException("The number of hidden weights mismatch");
        Vector aHiddenWeights = Vectors.zeros(tHiddenWeightsSize);
        tColNum = aInputDim;
        int tShift = 0;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = tSharedNN.mHiddenDims[i];
            int tSharedHiddenDim = aSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            int tSize = tNoSharedHiddenDim*tColNum;
            RowMatrix tWeight = Matrices.fromRows((List<?>)tHiddenWeights.get(i));
            if (tWeight.rowNumber() != tNoSharedHiddenDim) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
            if (tNoSharedHiddenDim>0 && tWeight.columnNumber()!=tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
            aHiddenWeights.subVec(tShift, tShift+tSize).fill(tWeight.asVecRow());
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        List<?> tHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        if (tHiddenBiases.size() != tHiddenNumber) throw new IllegalArgumentException("The number of hidden biases mismatch");
        Vector aHiddenBiases = Vectors.zeros(tHiddenBiasesSize);
        tShift = 0;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = tSharedNN.mHiddenDims[i];
            int tSharedHiddenDim = aSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            Vector tBias = Vectors.from((List<? extends Number>)tHiddenBiases.get(i));
            if (tBias.size() != tNoSharedHiddenDim) throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch");
            aHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim).fill(tBias);
            tShift += tNoSharedHiddenDim;
        }
        Vector aOutputWeight;
        double[] aOutputBias;
        if (aSharedHiddenDims[tHiddenNumber]==1) {
            aOutputWeight = Vectors.zeros(0);
            aOutputBias = ZL_DOUBLE;
        } else {
            aOutputWeight = Vectors.from((List<? extends Number>)aMap.get("output_weight"));
            aOutputBias = new double[] {((Number)aMap.get("output_bias")).doubleValue()};
            if (aOutputWeight.size() != tSharedNN.mHiddenDims[tHiddenNumber-1]) throw new IllegalArgumentException("Size of output weight mismatch");
        }
        return new SharedFeedForward2(aInputDim, tSharedNN, tSharedType, aSharedHiddenDims, aHiddenWeights, aHiddenBiases, aOutputWeight, aOutputBias);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "shared_feed_forward");
        rSaveTo.put("share", mSharedType);
        rSaveTo.put("shared_hidden_dims", AbstractCollections.from(mSharedHiddenDims));
        rSaveTo.put("input_dim", mInputDim);
        List<List<List<Double>>> rHiddenWeights = new ArrayList<>(mHiddenNumber);
        int tColNum = mInputDim;
        int tShift = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            List<List<Double>> tWeights = new ArrayList<>(tNoSharedHiddenDim);
            rHiddenWeights.add(tWeights);
            for (int j = 0; j < tNoSharedHiddenDim; ++j) {
                tWeights.add(mHiddenWeights.subVec(tShift, tShift+tColNum).asList());
                tShift += tColNum;
            }
            tColNum = tHiddenDim;
        }
        rSaveTo.put("hidden_weights", rHiddenWeights);
        List<List<Double>> rHiddenBiases = new ArrayList<>(mHiddenNumber);
        tShift = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSharedHiddenDim = mSharedHiddenDims[i];
            int tNoSharedHiddenDim = tHiddenDim-tSharedHiddenDim;
            rHiddenBiases.add(mHiddenBiases.subVec(tShift, tShift+tNoSharedHiddenDim).asList());
            tShift += tNoSharedHiddenDim;
        }
        rSaveTo.put("hidden_biases", rHiddenBiases);
        if (mSharedHiddenDims[mHiddenNumber]==0) {
            rSaveTo.put("output_weight", mOutputWeight.asList());
            rSaveTo.put("output_bias", mOutputBias[0]);
        }
    }
    
    /// 统一都只考虑独立的参数，不再考虑 shared 部分的参数
    @Override public int parameterSize() {
        return mHiddenWeightsSize+mOutputWeightSize + mHiddenBiasesSize+mOutputBiasSize;
    }
    public int parameterWeightSize() {
        return mHiddenWeightsSize+mOutputWeightSize;
    }
    public int hiddenSize() {
        return mHiddenBiasesSize;
    }
    public int inputSize() {
        return mInputDim;
    }
    
    @Override public IVector parameters() {
        final int tEndHW = mHiddenWeightsSize;
        final int tEndOW = tEndHW + mOutputWeightSize;
        final int tEndHB = tEndOW + mHiddenBiasesSize;
        final int tEndOB = tEndHB + mOutputBiasSize;
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
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put("[NN USE "+aGenIdx+"]", "shared_feed_forward");
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SHARED_TYPE", mSharedType);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_IN", mInputDim);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_HW", mHiddenWeightsSize);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_SHW", mSharedHiddenWeightsSize);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_HB", mHiddenBiasesSize);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_SHB", mSharedHiddenBiasesSize);
        rGenMap.put(aGenIdx+":NNAPGEN_NN_SIZE_OW", mOutputWeightSize);
        rGenMap.put("[NN HIDDEN LAYERS "+aGenIdx+"]", mHiddenNumber);
        int tInSize = mInputDim;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tOutSize = mShare.mHiddenDims[i];
            rGenMap.put(aGenIdx+":"+i+":NNAPGEN_NN_IN_SIZE", tInSize);
            rGenMap.put(aGenIdx+":"+i+":NNAPGEN_NN_OUT_SIZE", tOutSize);
            rGenMap.put(aGenIdx+":"+i+":NNAPGEN_NN_SHARE_SIZE", mSharedHiddenDims[i]);
            tInSize = tOutSize;
        }
    }
    
    @Override public boolean hasSameGenMap(NeuralNetwork2 aNN) {
        if (!(aNN instanceof SharedFeedForward2)) return false;
        SharedFeedForward2 tNN = (SharedFeedForward2)aNN;
        if (mHiddenNumber!=tNN.mHiddenNumber || mInputDim!=tNN.mInputDim) return false;
        for (int i = 0; i < mHiddenNumber; ++i) {
            if (mSharedHiddenDims[i]!=tNN.mSharedHiddenDims[i]) return false;
        }
        return mShare.hasSameGenMap(tNN.mShare);
    }
    
    @Override public int forwardCacheSize() {
        return mInputDim + (mHiddenBiasesSize+mSharedHiddenBiasesSize)*3;
    }
    @Override public int backwardCacheSize() {
        return mInputDim + (mHiddenBiasesSize+mSharedHiddenBiasesSize);
    }
}
