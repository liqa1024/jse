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
 * 可以共享部分层的前馈神经网络 (FFNN) 实现，原生实现在非 batch 的情况下可以有最快的性能
 * <p>
 * 仅支持输出单个数值的网络，且输出层不可共享
 * <p>
 * 由于内部会缓存中间结果，因此此类一般来说相同实例线程不安全，而不同实例之间线程安全；
 * 可以通过 {@link #threadSafeRef()} 来创建一个线程安全的引用（拷贝内部缓存）
 *
 * @author liqa
 */
public class SharedFeedForward extends NeuralNetwork implements ISavable {
    private final FeedForward mShare;
    private final int mSharedType;
    public FeedForward sharedNeuralNetwork() {return mShare;}
    public int sharedType() {return mSharedType;}
    
    private final int mInputDim;
    private final boolean[] mSharedFlags;
    private final Vector mHiddenWeights, mHiddenWeightsBackward;
    private final IntVector mIndexToBackward;
    private final Vector mHiddenBiases;
    private final Vector mOutputWeight;
    private final double[] mOutputBias;
    private final int mHiddenNumber, mHiddenWeightsSize, mSharedHiddenWeightsSize, mHiddenBiasesSize, mSharedHiddenBiasesSize, mOutputWeightSize;
    
    private SharedFeedForward(int aInputDim, FeedForward aSharedFeedForward, int aSharedType, boolean[] aSharedFlags, Vector aHiddenWeights, Vector aHiddenWeightsBackward, IntVector aIndexToBackward, Vector aHiddenBiases, Vector aOutputWeight, double[] aOutputBias) {
        mInputDim = aInputDim;
        mShare = aSharedFeedForward;
        mSharedType = aSharedType;
        mSharedFlags = aSharedFlags;
        mHiddenNumber = mSharedFlags.length;
        if (mHiddenNumber != mShare.mHiddenNumber) throw new IllegalArgumentException("Hidden number mismatch");
        if (mSharedFlags[0] && mInputDim!=mShare.mInputDim) throw new IllegalArgumentException("Input dimensions mismatch for shared first layer");
        int tHiddenWeightsSize = 0, tSharedHiddenWeightsSize = 0;
        int tHiddenBiasesSize = 0, tSharedHiddenBiasesSize = 0;
        int tColNum = mInputDim;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            if (!mSharedFlags[i]) {
                tHiddenWeightsSize += tColNum * tHiddenDim;
                tHiddenBiasesSize += tHiddenDim;
            } else {
                tSharedHiddenWeightsSize += tColNum * tHiddenDim;
                tSharedHiddenBiasesSize += tHiddenDim;
            }
            tColNum = tHiddenDim;
        }
        mHiddenWeightsSize = tHiddenWeightsSize; mSharedHiddenWeightsSize = tSharedHiddenWeightsSize;
        mHiddenBiasesSize = tHiddenBiasesSize; mSharedHiddenBiasesSize = tSharedHiddenBiasesSize;
        mHiddenWeights = aHiddenWeights==null ? Vectors.zeros(mHiddenWeightsSize) : aHiddenWeights;
        mHiddenWeightsBackward = aHiddenWeightsBackward==null ? Vectors.zeros(mHiddenWeightsSize) : aHiddenWeightsBackward;
        mIndexToBackward = aIndexToBackward==null ? IntVector.zeros(mHiddenWeightsSize) : aIndexToBackward;
        mHiddenBiases = aHiddenBiases==null ? Vectors.zeros(mHiddenBiasesSize) : aHiddenBiases;
        if (mHiddenWeights.internalDataSize() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of hidden weights mismatch");
        if (mHiddenWeightsBackward.internalDataSize() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of backward hidden weights mismatch");
        if (mHiddenBiases.internalDataSize() != mHiddenBiasesSize) throw new IllegalArgumentException("The size of hidden biases mismatch");
        mOutputWeightSize = mShare.mHiddenDims[mHiddenNumber-1];
        mOutputWeight = aOutputWeight==null ? Vectors.zeros(mOutputWeightSize) : aOutputWeight;
        mOutputBias = aOutputBias==null ? new double[1] : aOutputBias;
        if (mOutputWeight.internalDataSize() != mOutputWeightSize) throw new IllegalArgumentException("The size of output weight mismatch");
        if (mOutputBias.length != 1) throw new IllegalArgumentException("The size of output biases mismatch");
    }
    public SharedFeedForward(int aInputDim, FeedForward aSharedFeedForward, int aSharedType, boolean[] aSharedFlags, Vector aHiddenWeights, Vector aHiddenBiases, Vector aOutputWeight, double[] aOutputBias) {
        this(aInputDim, aSharedFeedForward, aSharedType, aSharedFlags, aHiddenWeights, null, null, aHiddenBiases, aOutputWeight, aOutputBias);
        int tColNum = mInputDim;
        int tShift = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            if (!mSharedFlags[i]) {
                int tSize = tHiddenDim*tColNum;
                final int fColNum = tColNum;
                final int tShiftB = mHiddenWeightsSize-tShift-tSize;
                mIndexToBackward.subVec(tShift, tShift+tSize).fill(ii -> {
                    int row = ii / fColNum;
                    int col = ii % fColNum;
                    return col*tHiddenDim + row + tShiftB;
                });
                tShift += tSize;
            }
            tColNum = tHiddenDim;
        }
        for (int i = 0; i < mHiddenWeightsSize; ++i) {
            mHiddenWeightsBackward.set(mIndexToBackward.get(i), mHiddenWeights.get(i));
        }
    }
    public SharedFeedForward(int aInputDim, FeedForward aSharedFeedForward, int aSharedType, boolean[] aSharedFlags) {
        this(aInputDim, aSharedFeedForward, aSharedType, aSharedFlags, null, null, null, null);
    }
    
    public SharedFeedForward threadSafeRef() {
        return new SharedFeedForward(mInputDim, mShare.threadSafeRef(), mSharedType, mSharedFlags, mHiddenWeights, mHiddenWeightsBackward, mIndexToBackward, mHiddenBiases, mOutputWeight, mOutputBias);
    }
    
    // 这里包含时只处理实际计算流中涉及的参数部分
    public void initParameters(boolean aIncludeShare) {
        int tColNum = mInputDim, tSharedColNum = mShare.mInputDim;
        int tShift = 0, tShiftShare = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            int tSize = tHiddenDim*tColNum;
            int tSharedSize = tHiddenDim*tSharedColNum;
            double tBound = MathEX.Fast.sqrt(6.0 / tColNum); // Kaiming 均匀初始化
            if (!mSharedFlags[i]) {
                mHiddenWeights.subVec(tShift, tShift+tSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
                tShift += tSize;
            } else
            if (aIncludeShare) {
                mShare.mHiddenWeights.subVec(tShiftShare, tShiftShare+tSharedSize).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            }
            tShiftShare += tSharedSize;
            tSharedColNum = tColNum = tHiddenDim;
        }
        for (int i = 0; i < mHiddenWeightsSize; ++i) {
            mHiddenWeightsBackward.set(mIndexToBackward.get(i), mHiddenWeights.get(i));
        }
        if (aIncludeShare) for (int i = 0; i < mShare.mHiddenWeightsSize; ++i) {
            mShare.mHiddenWeightsBackward.set(mShare.mIndexToBackward.get(i), mShare.mHiddenWeights.get(i));
        }
        tShift = 0; tShiftShare = 0;
        tColNum = mInputDim;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            double tBound = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
            if (!mSharedFlags[i]) {
                mHiddenBiases.subVec(tShift, tShift+tHiddenDim).assign(() -> RANDOM.nextDouble(-tBound, tBound));
                tShift += tHiddenDim;
            } else
            if (aIncludeShare) {
                mShare.mHiddenBiases.subVec(tShiftShare, tShiftShare+tHiddenDim).assign(() -> RANDOM.nextDouble(-tBound, tBound));
            }
            tShiftShare += tHiddenDim;
            tColNum = tHiddenDim;
        }
        double tBound = MathEX.Fast.sqrt(3.0 / tColNum); // Kaiming 均匀初始化，注意输出层没有激活函数因此权重需要调整
        mOutputWeight.assign(() -> RANDOM.nextDouble(-tBound, tBound));
        double tBoundB = MathEX.Fast.sqrt(1.0 / tColNum); // 偏置也使用 Kaiming 均匀初始化，和 pytorch 默认保持一致
        mOutputBias[0] = RANDOM.nextDouble(-tBoundB, tBoundB);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static SharedFeedForward load_(FeedForward aSharedFeedForward, int aSharedType, Map aMap) {
        aSharedFeedForward = aSharedFeedForward.threadSafeRef(); // 这里统一进行线程安全引用获取，确保加载后的 nn 和传入的 nn 之间线程安全，这是所有 load 的语义
        int aInputDim = ((Number)UT.Code.get(aMap, "input_dim")).intValue();
        List<?> tSharedFlags = (List<?>)UT.Code.get(aMap, "shared_flags");
        boolean[] aSharedFlags = new boolean[tSharedFlags.size()];
        for (int i = 0; i < aSharedFlags.length; ++i) {
            aSharedFlags[i] = (Boolean)tSharedFlags.get(i);
        }
        int tHiddenNumber = aSharedFlags.length;
        if (tHiddenNumber != aSharedFeedForward.mHiddenNumber) throw new IllegalArgumentException("Hidden number mismatch");
        int tHiddenWeightsSize = 0;
        int tHiddenBiasesSize = 0;
        int tNoSharedHiddenNumber = 0;
        int tColNum = aInputDim;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = aSharedFeedForward.mHiddenDims[i];
            if (!aSharedFlags[i]) {
                tHiddenWeightsSize += tColNum * tHiddenDim;
                tHiddenBiasesSize += tHiddenDim;
                ++tNoSharedHiddenNumber;
            }
            tColNum = tHiddenDim;
        }
        List<?> tHiddenWeights = (List<?>)UT.Code.get(aMap, "hidden_weights");
        if (tHiddenWeights.size() != tNoSharedHiddenNumber) throw new IllegalArgumentException("The number of hidden weights mismatch");
        Vector aHiddenWeights = Vectors.zeros(tHiddenWeightsSize);
        tColNum = aInputDim;
        int tShift = 0;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = aSharedFeedForward.mHiddenDims[i];
            if (!aSharedFlags[i]) {
                int tSize = tHiddenDim*tColNum;
                RowMatrix tWeight = Matrices.fromRows((List<?>)tHiddenWeights.get(i));
                if (tWeight.columnNumber() != tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
                if (tWeight.rowNumber() != tHiddenDim) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
                aHiddenWeights.subVec(tShift, tShift+tSize).fill(tWeight.asVecRow());
                tShift += tSize;
            }
            tColNum = tHiddenDim;
        }
        List<?> tHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        if (tHiddenBiases.size() != tNoSharedHiddenNumber) throw new IllegalArgumentException("The number of hidden biases mismatch");
        Vector aHiddenBiases = Vectors.zeros(tHiddenBiasesSize);
        tShift = 0;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = aSharedFeedForward.mHiddenDims[i];
            if (!aSharedFlags[i]) {
                Vector tBias = Vectors.from((List<? extends Number>)tHiddenBiases.get(i));
                if (tBias.size() != tHiddenDim) throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch");
                aHiddenBiases.subVec(tShift, tShift+tHiddenDim).fill(tBias);
                tShift += tHiddenDim;
            }
        }
        Vector aOutputWeight = Vectors.from((List<? extends Number>)aMap.get("output_weight"));
        double aOutputBias = ((Number)aMap.get("output_bias")).doubleValue();
        if (aOutputWeight.size() != aSharedFeedForward.mHiddenDims[tHiddenNumber-1]) throw new IllegalArgumentException("Size of output weight mismatch");
        
        return new SharedFeedForward(aInputDim, aSharedFeedForward, aSharedType, aSharedFlags, aHiddenWeights, aHiddenBiases, aOutputWeight, new double[]{aOutputBias});
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "shared_feed_forward");
        rSaveTo.put("share", mSharedType);
        rSaveTo.put("shared_flags", AbstractCollections.from(mSharedFlags));
        rSaveTo.put("input_dim", mInputDim);
        List<List<List<Double>>> rHiddenWeights = new ArrayList<>(mHiddenNumber);
        int tColNum = mInputDim;
        int tShift = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            if (!mSharedFlags[i]) {
                List<List<Double>> tWeights = new ArrayList<>(tHiddenDim);
                rHiddenWeights.add(tWeights);
                for (int j = 0; j < tHiddenDim; ++j) {
                    tWeights.add(mHiddenWeights.subVec(tShift, tShift+tColNum).asList());
                    tShift += tColNum;
                }
            }
            tColNum = tHiddenDim;
        }
        rSaveTo.put("hidden_weights", rHiddenWeights);
        List<List<Double>> rHiddenBiases = new ArrayList<>(mHiddenNumber);
        tShift = 0;
        for (int i = 0; i < mHiddenNumber; ++i) {
            int tHiddenDim = mShare.mHiddenDims[i];
            if (!mSharedFlags[i]) {
                rHiddenBiases.add(mHiddenBiases.subVec(tShift, tShift+tHiddenDim).asList());
                tShift += tHiddenDim;
            }
        }
        rSaveTo.put("hidden_biases", rHiddenBiases);
        rSaveTo.put("output_weight", mOutputWeight.asList());
        rSaveTo.put("output_bias", mOutputBias[0]);
    }
    
    // 这里包含时只返回实际计算流中涉及的参数部分
    public int parameterSize(boolean aIncludeShare) {
        int tParamSize = mHiddenWeightsSize+mOutputWeightSize + mHiddenBiasesSize+1;
        if (!aIncludeShare) return tParamSize;
        tParamSize += mSharedHiddenWeightsSize + mSharedHiddenBiasesSize;
        return tParamSize;
    }
    public int parameterWeightSize(boolean aIncludeShare) {
        int tParamSize = mHiddenWeightsSize+mOutputWeightSize;
        if (!aIncludeShare) return tParamSize;
        tParamSize += mSharedHiddenWeightsSize;
        return tParamSize;
    }
    public int hiddenSize(boolean aIncludeShare) {
        int tParamSize = mHiddenBiasesSize;
        if (!aIncludeShare) return tParamSize;
        tParamSize += mSharedHiddenBiasesSize;
        return tParamSize;
    }
    /// 由于准确的实现复杂，且内部使用完全没有使用到，因此这里直接不支持 includeShare 的情况
    public IVector parameters(final boolean aIncludeShare) {
        if (aIncludeShare) throw new UnsupportedOperationException("parameters with share is invalid now.");
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
    
    @Override public int inputSize() {
        return mInputDim;
    }
    @Override public double eval(DoubleArrayVector aX) {
        return forward(aX, null, null);
    }
    public double forward(DoubleArrayVector aX, @Nullable DoubleArrayVector rHiddenOutputs, @Nullable DoubleArrayVector rHiddenGrads) {
        boolean aRequireGrad = rHiddenOutputs!=null;
        if (aRequireGrad && rHiddenGrads==null) throw new NullPointerException();
        return forward0(aX, aRequireGrad?rHiddenOutputs:mShare.mHiddenOutputs, aRequireGrad?rHiddenGrads:null);
    }
    double forward0(IDataShell<double[]> aX, @NotNull IDataShell<double[]> rHiddenOutputs, @Nullable IDataShell<double[]> rHiddenGrads) {
        if (mShare.mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags[0] && mInputDim!=mShare.mInputDim) throw new IllegalArgumentException("Input dimensions mismatch for shared first layer");
        final int tHiddenSize = hiddenSize(true);
        return forward1(aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), mInputDim, mShare.mInputDim, mShare.mHiddenDims, mSharedFlags, mHiddenNumber,
                        mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mShare.mHiddenWeights.internalDataWithLengthCheck(mShare.mHiddenWeightsSize, 0),
                        mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mShare.mHiddenBiases.internalDataWithLengthCheck(mShare.mHiddenBiasesSize, 0),
                        mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias[0],
                        rHiddenOutputs.internalDataWithLengthCheck(tHiddenSize), rHiddenOutputs.internalDataShift(),
                        rHiddenGrads==null?null:rHiddenGrads.internalDataWithLengthCheck(tHiddenSize), rHiddenGrads==null?0:rHiddenGrads.internalDataShift());
    }
    private static native double forward1(double[] aX, int aShiftX, int aInputDim, int aSharedInputDim, int[] aHiddenDims, boolean[] aSharedFlags, int aHiddenNumber,
                                          double[] aHiddenWeights, double[] aSharedHiddenWeights, double[] aHiddenBiases, double[] aSharedHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                          double[] rHiddenOutputs, int aShiftOutputs, double[] rHiddenGrads, int aShiftGrads);
    
    @Override public double evalGrad(DoubleArrayVector aX, DoubleArrayVector rGradX) {
        return forwardGrad(aX, rGradX, null, null, null, null, null);
    }
    public double forwardGrad(DoubleArrayVector aX, DoubleArrayVector rGradX, @Nullable DoubleArrayVector rHiddenOutputs, @Nullable DoubleArrayVector rHiddenGrads,
                              @Nullable DoubleArrayVector rHiddenGrads2, @Nullable DoubleArrayVector rHiddenGrads3, @Nullable DoubleArrayVector rHiddenGradGrads) {
        boolean aRequireGradGrad = rHiddenOutputs!=null;
        if (aRequireGradGrad) {
            if (rHiddenGrads==null) throw new NullPointerException();
            if (rHiddenGrads2==null) throw new NullPointerException();
            if (rHiddenGrads3==null) throw new NullPointerException();
            if (rHiddenGradGrads==null) throw new NullPointerException();
        }
        return forwardGrad0(aX, rGradX, aRequireGradGrad?rHiddenOutputs:mShare.mHiddenOutputs, aRequireGradGrad?rHiddenGrads:mShare.mHiddenGrads,
                            aRequireGradGrad?rHiddenGrads2:mShare.mHiddenGrads2, aRequireGradGrad?rHiddenGrads3:mShare.mHiddenGrads3, aRequireGradGrad?rHiddenGradGrads:null);
    }
    double forwardGrad0(IDataShell<double[]> aX, IDataShell<double[]> rGradX, @NotNull IDataShell<double[]> rHiddenOutputs, @NotNull IDataShell<double[]> rHiddenGrads,
                        @NotNull IDataShell<double[]> rHiddenGrads2, @NotNull IDataShell<double[]> rHiddenGrads3, @Nullable IDataShell<double[]> rHiddenGradGrads) {
        if (mShare.mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags[0] && mInputDim!=mShare.mInputDim) throw new IllegalArgumentException("Input dimensions mismatch for shared first layer");
        final int tHiddenSize = hiddenSize(true);
        return forwardGrad1(aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), rGradX.internalDataWithLengthCheck(mInputDim), rGradX.internalDataShift(),
                            mInputDim, mShare.mInputDim, mShare.mHiddenDims, mSharedFlags, mHiddenNumber,
                            mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mShare.mHiddenWeights.internalDataWithLengthCheck(mShare.mHiddenWeightsSize, 0),
                            mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mShare.mHiddenWeightsBackward.internalDataWithLengthCheck(mShare.mHiddenWeightsSize, 0),
                            mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mShare.mHiddenBiases.internalDataWithLengthCheck(mShare.mHiddenBiasesSize, 0),
                            mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias[0],
                            rHiddenOutputs.internalDataWithLengthCheck(tHiddenSize), rHiddenOutputs.internalDataShift(),
                            rHiddenGrads.internalDataWithLengthCheck(tHiddenSize), rHiddenGrads.internalDataShift(),
                            rHiddenGrads2.internalDataWithLengthCheck(tHiddenSize), rHiddenGrads2.internalDataShift(),
                            rHiddenGrads3.internalDataWithLengthCheck(tHiddenSize), rHiddenGrads3.internalDataShift(),
                            rHiddenGradGrads==null?null:rHiddenGradGrads.internalDataWithLengthCheck(tHiddenSize), rHiddenGradGrads==null?0:rHiddenGradGrads.internalDataShift());
    }
    private static native double forwardGrad1(double[] aX, int aShiftX, double[] rGradX, int aShiftGradX,
                                              int aInputDim, int aSharedInputDim, int[] aHiddenDims, boolean[] aSharedFlags, int aHiddenNumber,
                                              double[] aHiddenWeights, double[] aSharedHiddenWeights, double[] aHiddenWeightsBackward, double[] aSharedHiddenWeightsBackward,
                                              double[] aHiddenBiases, double[] aSharedHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                              double[] rHiddenOutputs, int aShiftOutputs, double[] rHiddenGrads, int aShiftGrads,
                                              double[] rHiddenGrads2, int aShiftGrads2, double[] rHiddenGrads3, int aShiftGrads3, double[] rHiddenGradGrads, int aShiftGradGrads);
    
    
    public void backward(double aYGrad, DoubleArrayVector aX, DoubleArrayVector rGradPara, DoubleArrayVector rGradSharedPara, DoubleArrayVector aHiddenOutputs, DoubleArrayVector aHiddenGrads) {
        backward0(aYGrad, aX, null, rGradPara, rGradSharedPara, aHiddenOutputs, aHiddenGrads);
    }
    public void backward(double aYGrad, DoubleArrayVector aX, @Nullable DoubleArrayVector rGradX, DoubleArrayVector rGradPara, DoubleArrayVector rGradSharedPara, DoubleArrayVector aHiddenOutputs, DoubleArrayVector aHiddenGrads) {
        backward0(aYGrad, aX, rGradX, rGradPara, rGradSharedPara, aHiddenOutputs, aHiddenGrads);
    }
    void backward0(double aYGrad, IDataShell<double[]> aX, @Nullable IDataShell<double[]> rGradX, IDataShell<double[]> rGradPara, IDataShell<double[]> rGradSharedPara, IDataShell<double[]> aHiddenOutputs, IDataShell<double[]> aHiddenGrads) {
        if (mShare.mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags[0] && mInputDim!=mShare.mInputDim) throw new IllegalArgumentException("Input dimensions mismatch for shared first layer");
        final int tHiddenSize = hiddenSize(true);
        backward1(aYGrad, aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(),
                  rGradX==null?null:rGradX.internalDataWithLengthCheck(mInputDim), rGradX==null?0:rGradX.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(parameterSize(false)), rGradPara.internalDataShift(),
                  rGradSharedPara.internalDataWithLengthCheck(mShare.parameterSize()), rGradSharedPara.internalDataShift(),
                  mInputDim, mShare.mInputDim, mShare.mHiddenDims, mSharedFlags, mHiddenNumber,
                  mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mShare.mHiddenWeightsBackward.internalDataWithLengthCheck(mShare.mHiddenWeightsSize, 0),
                  mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0),
                  aHiddenOutputs.internalDataWithLengthCheck(tHiddenSize), aHiddenOutputs.internalDataShift(),
                  aHiddenGrads.internalDataWithLengthCheck(tHiddenSize), aHiddenGrads.internalDataShift(),
                  mShare.mHiddenGrads2.internalDataWithLengthCheck(tHiddenSize, 0), mShare.mHiddenGrads3.internalDataWithLengthCheck(tHiddenSize, 0));
    }
    private static native void backward1(double aYGrad, double[] aX, int aShiftX,  double[] rGradX, int aShiftGradX, double[] rGradPara, int aShiftGradPara, double[] rGradSharedPara, int aShiftGradSharedPara,
                                         int aInputDim, int aSharedInputDim, int[] aHiddenDims, boolean[] aSharedFlags, int aHiddenNumber,
                                         double[] aHiddenWeightsBackward, double[] aSharedHiddenWeightsBackward, double[] aOutputWeight,
                                         double[] aHiddenOutputs, int aShiftOutputs, double[] aHiddenGrads, int aShiftGrads, double[] rHiddenGrads2, double[] rHiddenGrads3);
    
    public void gradBackward(DoubleArrayVector aGradXGrad, DoubleArrayVector aX, DoubleArrayVector rGradPara, DoubleArrayVector rGradSharedPara, DoubleArrayVector aHiddenOutputs,
                             DoubleArrayVector aHiddenGrads, DoubleArrayVector aHiddenGrads2, DoubleArrayVector aHiddenGrads3, DoubleArrayVector aHiddenGradGrads) {
        gradBackward0(aGradXGrad, aX, null, rGradPara, rGradSharedPara, aHiddenOutputs, aHiddenGrads, aHiddenGrads2, aHiddenGrads3, aHiddenGradGrads);
    }
    public void gradBackward(DoubleArrayVector aGradXGrad, DoubleArrayVector aX, @Nullable DoubleArrayVector rGradX, DoubleArrayVector rGradPara, DoubleArrayVector rGradSharedPara, DoubleArrayVector aHiddenOutputs,
                             DoubleArrayVector aHiddenGrads, DoubleArrayVector aHiddenGrads2, DoubleArrayVector aHiddenGrads3, DoubleArrayVector aHiddenGradGrads) {
        gradBackward0(aGradXGrad, aX, rGradX, rGradPara, rGradSharedPara, aHiddenOutputs, aHiddenGrads, aHiddenGrads2, aHiddenGrads3, aHiddenGradGrads);
    }
    void gradBackward0(IDataShell<double[]> aGradXGrad, IDataShell<double[]> aX, @Nullable IDataShell<double[]> rGradX, IDataShell<double[]> rGradPara, IDataShell<double[]> rGradSharedPara, IDataShell<double[]> aHiddenOutputs,
                       IDataShell<double[]> aHiddenGrads, IDataShell<double[]> aHiddenGrads2, IDataShell<double[]> aHiddenGrads3, IDataShell<double[]> aHiddenGradGrads) {
        if (mShare.mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        if (mSharedFlags[0] && mInputDim!=mShare.mInputDim) throw new IllegalArgumentException("Input dimensions mismatch for shared first layer");
        final int tHiddenSize = hiddenSize(true);
        gradBackward1(aGradXGrad.internalDataWithLengthCheck(mInputDim), aGradXGrad.internalDataShift(), aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(),
                      rGradX==null?null:rGradX.internalDataWithLengthCheck(mInputDim), rGradX==null?0:rGradX.internalDataShift(),
                      rGradPara.internalDataWithLengthCheck(parameterSize(false)), rGradPara.internalDataShift(),
                      rGradSharedPara.internalDataWithLengthCheck(mShare.parameterSize()), rGradSharedPara.internalDataShift(),
                      mInputDim, mShare.mInputDim, mShare.mHiddenDims, mSharedFlags, mHiddenNumber,
                      mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mShare.mHiddenWeights.internalDataWithLengthCheck(mShare.mHiddenWeightsSize, 0),
                      mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mShare.mHiddenWeightsBackward.internalDataWithLengthCheck(mShare.mHiddenWeightsSize, 0),
                      mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0),
                      aHiddenOutputs.internalDataWithLengthCheck(tHiddenSize), aHiddenOutputs.internalDataShift(),
                      aHiddenGrads.internalDataWithLengthCheck(tHiddenSize), aHiddenGrads.internalDataShift(),
                      aHiddenGrads2.internalDataWithLengthCheck(tHiddenSize), aHiddenGrads2.internalDataShift(),
                      aHiddenGrads3.internalDataWithLengthCheck(tHiddenSize), aHiddenGrads3.internalDataShift(),
                      aHiddenGradGrads.internalDataWithLengthCheck(tHiddenSize), aHiddenGradGrads.internalDataShift(),
                      mShare.mHiddenOutputs.internalDataWithLengthCheck(tHiddenSize, 0), mShare.mHiddenGrads2.internalDataWithLengthCheck(tHiddenSize, 0), mShare.mHiddenGrads3.internalDataWithLengthCheck(tHiddenSize, 0));
    }
    private static native void gradBackward1(double[] aGradXGrad, int aShiftGradXGrad, double[] aX, int aShiftX, double[] rGradX, int aShiftGradX, double[] rGradPara, int aShiftGradPara, double[] rGradSharedPara, int aShiftGradSharedPara,
                                             int aInputDim, int aSharedInputDim, int[] aHiddenDims, boolean[] aSharedFlags, int aHiddenNumber,
                                             double[] aHiddenWeights, double[] aSharedHiddenWeights, double[] aHiddenWeightsBackward, double[] aSharedHiddenWeightsBackward, double[] aOutputWeight,
                                             double[] aHiddenOutputs, int aShiftOutputs, double[] aHiddenGrads, int aShiftGrads,
                                             double[] aHiddenGrads2, int aShiftGrads2, double[] aHiddenGrads3, int aShiftGrads3, double[] aHiddenGradGrads, int aShiftGradGrads,
                                             double[] rHiddenOutputs2, double[] rHiddenGrads4, double[] rHiddenGrads5);
}
