package jsex.nnap.nn;

import jse.code.UT;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.DoubleArrayMatrix;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

/**
 * 简单的前馈神经网络 (FFNN) 实现，原生实现在非 batch 的情况下可以有最快的性能
 * <p>
 * 仅支持输出单个数值的网络
 * <p>
 * 由于内部会缓存中间结果，因此此类一般来说相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
public class FeedForward extends NeuralNetwork {
    private final int mInputDim;
    private final int[] mHiddenDims;
    private final Vector mHiddenWeights, mHiddenWeightsBackward;
    private final IntVector mIndexToBackward;
    private final Vector mHiddenBiases;
    private final Vector mOutputWeight;
    private double mOutputBias;
    private final int mHiddenNumber, mHiddenWeightsSize, mHiddenBiasesSize, mOutputWeightSize;
    
    /// 缓存中间变量
    private final Vector mHiddenOutputs, mHiddenGrads;
    
    FeedForward(int aInputDim, int[] aHiddenDims, Vector aHiddenWeights, Vector aHiddenWeightsBackward, IntVector aIndexToBackward, Vector aHiddenBiases, Vector aOutputWeight, double aOutputBias) {
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
        mHiddenWeights = aHiddenWeights;
        mHiddenWeightsBackward = aHiddenWeightsBackward;
        mIndexToBackward = aIndexToBackward;
        mHiddenBiases = aHiddenBiases;
        if (mHiddenWeights.internalDataSize() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of hidden weights mismatch");
        if (mHiddenWeightsBackward.internalDataSize() != mHiddenWeightsSize) throw new IllegalArgumentException("The size of backward hidden weights mismatch");
        if (mHiddenBiases.internalDataSize() != mHiddenBiasesSize) throw new IllegalArgumentException("The size of hidden biases mismatch");
        mOutputWeight = aOutputWeight;
        mOutputBias = aOutputBias;
        mOutputWeightSize = mHiddenDims[mHiddenNumber-1];
        if (mOutputWeight.internalDataSize() != mOutputWeightSize) throw new IllegalArgumentException("The size of output weight mismatch");
        mHiddenOutputs = Vectors.zeros(mHiddenBiasesSize);
        mHiddenGrads = Vectors.zeros(mHiddenBiasesSize);
    }
    
    public static FeedForward init(int aInputDim, int[] aHiddenDims) {
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
        Vector aHiddenWeights = Vectors.zeros(tHiddenWeightsSize);
        Vector aHiddenWeightsBackward = Vectors.zeros(tHiddenWeightsSize);
        IntVector aIndexToBackward = IntVector.zeros(tHiddenWeightsSize);
        tColNum = aInputDim;
        int tShift = 0;
        for (int tHiddenDim : aHiddenDims) {
            int tSize = tHiddenDim*tColNum;
            double tBound = MathEX.Fast.sqrt(6.0 / tColNum); // Kaiming 均匀初始化
            aHiddenWeights.subVec(tShift, tShift+tSize).fill(i -> RANDOM.nextDouble(-tBound, tBound));
            final int fColNum = tColNum;
            final int tShiftB = tHiddenWeightsSize-tShift-tSize;
            aIndexToBackward.subVec(tShift, tShift+tSize).fill(ii -> {
                int row = ii / fColNum;
                int col = ii % fColNum;
                return col*tHiddenDim + row + tShiftB;
            });
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        for (int i = 0; i < tHiddenWeightsSize; ++i) {
            aHiddenWeightsBackward.set(aIndexToBackward.get(i), aHiddenWeights.get(i));
        }
        Vector aHiddenBiases = Vectors.zeros(tHiddenBiasesSize);
        tShift = 0;
        tColNum = aInputDim;
        for (int tHiddenDim : aHiddenDims) {
            double tBound = MathEX.Fast.sqrt(1.0 / tColNum); // Kaiming 均匀初始化
            aHiddenBiases.subVec(tShift, tShift+tHiddenDim).fill(i -> RANDOM.nextDouble(-tBound, tBound));
            tShift += tHiddenDim;
            tColNum = tHiddenDim;
        }
        double tBound = MathEX.Fast.sqrt(6.0 / tColNum); // Kaiming 均匀初始化
        Vector aOutputWeight = Vectors.from(aHiddenDims[tHiddenNumber-1], i -> RANDOM.nextDouble(-tBound, tBound));
        double tBoundB = MathEX.Fast.sqrt(1.0 / tColNum); // Kaiming 均匀初始化
        double aOutputBias = RANDOM.nextDouble(-tBoundB, tBoundB);
        
        return new FeedForward(aInputDim, aHiddenDims, aHiddenWeights, aHiddenWeightsBackward, aIndexToBackward, aHiddenBiases, aOutputWeight, aOutputBias);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static FeedForward load(Map aMap) {
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
        Vector aHiddenWeightsBackward = Vectors.zeros(tHiddenWeightsSize);
        IntVector aIndexToBackward = IntVector.zeros(tHiddenWeightsSize);
        tColNum = aInputDim;
        int tShift = 0;
        for (int i = 0; i < tHiddenNumber; ++i) {
            int tHiddenDim = aHiddenDims[i];
            int tSize = tHiddenDim*tColNum;
            RowMatrix tWeight = Matrices.fromRows((List<?>)tHiddenWeights.get(i));
            if (tWeight.columnNumber() != tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
            if (tWeight.rowNumber() != tHiddenDim) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
            aHiddenWeights.subVec(tShift, tShift+tSize).fill(tWeight.asVecRow());
            final int fColNum = tColNum;
            final int tShiftB = tHiddenWeightsSize-tShift-tSize;
            aIndexToBackward.subVec(tShift, tShift+tSize).fill(ii -> {
                int row = ii / fColNum;
                int col = ii % fColNum;
                return col*tHiddenDim + row + tShiftB;
            });
            tShift += tSize;
            tColNum = tHiddenDim;
        }
        for (int i = 0; i < tHiddenWeightsSize; ++i) {
            aHiddenWeightsBackward.set(aIndexToBackward.get(i), aHiddenWeights.get(i));
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
        
        return new FeedForward(aInputDim, aHiddenDims, aHiddenWeights, aHiddenWeightsBackward, aIndexToBackward, aHiddenBiases, aOutputWeight, aOutputBias);
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
                    return mOutputBias;
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
                    mOutputBias = aValue;
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
    @Override public double forward(DoubleArrayVector aX) {
        return forward0(aX);
    }
    double forward0(IDataShell<double[]> aX) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        return forward1(aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), mInputDim, mHiddenDims, mHiddenNumber,
                        mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0),
                        mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias,
                        mHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize, 0));
    }
    private static native double forward1(double[] aX, int aShiftX, int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                          double[] aHiddenWeights, double[] aHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                          double[] rHiddenOutputs);
    
    @Override public double backward(DoubleArrayVector aX, DoubleArrayVector rGradX) {
        return backwardFull(aX, rGradX, null);
    }
    public double backwardFull(DoubleArrayVector aX, @Nullable DoubleArrayVector rGradX, @Nullable DoubleArrayVector rGradPara) {
        return backwardFull0(aX, rGradX, rGradPara);
    }
    double backwardFull0(IDataShell<double[]> aX, @Nullable IDataShell<double[]> rGradX, @Nullable IDataShell<double[]> rGradPara) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        return backwardFull1(aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), rGradX==null ? null : rGradX.internalDataWithLengthCheck(mInputDim), rGradX==null ? 0 : rGradX.internalDataShift(), rGradPara==null ? null : rGradPara.internalDataWithLengthCheck(mHiddenWeightsSize+mHiddenBiasesSize+mOutputWeightSize+1), rGradPara==null ? 0 : rGradPara.internalDataShift(),
                             mInputDim, mHiddenDims, mHiddenNumber,
                             mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0),
                             mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias,
                             mHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mHiddenGrads.internalDataWithLengthCheck(mHiddenBiasesSize, 0));
    }
    private static native double backwardFull1(double[] aX, int aShiftX, double[] rGradX, int aShiftGradX, double[] rGradPara, int aShiftGradPara,
                                               int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                               double[] aHiddenWeights, double[] aHiddenWeightsBackward, double[] aHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                               double[] rHiddenOutputs, double[] rHiddenGrads);
    
    public double backwardDoubleFull(DoubleArrayVector aX, @Nullable DoubleArrayVector rGradX, @Nullable DoubleArrayVector rGradPara, @Nullable DoubleArrayVector rGradXGradPara) {
        return backwardDoubleFull0(aX, rGradX, rGradPara, rGradXGradPara);
    }
    double backwardDoubleFull0(IDataShell<double[]> aX, @Nullable IDataShell<double[]> rGradX, @Nullable IDataShell<double[]> rGradPara, @Nullable IDataShell<double[]> rGradXGradPara) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        int tParaSize = mHiddenWeightsSize+mHiddenBiasesSize+mOutputWeightSize+1;
        return backwardDoubleFull1(aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), rGradX==null ? null : rGradX.internalDataWithLengthCheck(mInputDim), rGradX==null ? 0 : rGradX.internalDataShift(), rGradPara==null ? null : rGradPara.internalDataWithLengthCheck(tParaSize), rGradPara==null ? 0 : rGradPara.internalDataShift(),
                                   rGradXGradPara==null ? null : rGradXGradPara.internalDataWithLengthCheck(mInputDim*tParaSize), rGradXGradPara==null ? 0 : rGradXGradPara.internalDataShift(),
                                   mInputDim, mHiddenDims, mHiddenNumber,
                                   mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0),
                                   mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias,
                                   mHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mHiddenGrads.internalDataWithLengthCheck(mHiddenBiasesSize, 0));
    }
    private static native double backwardDoubleFull1(double[] aX, int aShiftX, double[] rGradX, int aShiftGradX, double[] rGradPara, int aShiftGradPara, double[] rGradXGradPara, int aShiftGradXGradPara,
                                                     int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                                     double[] aHiddenWeights, double[] aHiddenWeightsBackward, double[] aHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                                     double[] rHiddenOutputs, double[] rHiddenGrads);
}
