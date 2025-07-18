package jsex.nnap.nn;

import jse.code.UT;
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
 * <p>
 * 由于内部会缓存中间结果，因此此类一般来说相同实例线程不安全，而不同实例之间线程安全；
 * 可以通过 {@link #threadSafeRef()} 来创建一个线程安全的引用（拷贝内部缓存）
 *
 * @author liqa
 */
public class FeedForward extends NeuralNetwork implements ISavable {
    private final int mInputDim;
    private final int[] mHiddenDims;
    private final Vector mHiddenWeights, mHiddenWeightsBackward;
    private final IntVector mIndexToBackward;
    private final Vector mHiddenBiases;
    private final Vector mOutputWeight;
    private final double[] mOutputBias;
    private final int mHiddenNumber, mHiddenWeightsSize, mHiddenBiasesSize, mOutputWeightSize;
    
    /// 缓存中间变量
    private final Vector mHiddenOutputs, mHiddenGrads, mHiddenGrads2, mHiddenGrads3;
    
    private FeedForward(int aInputDim, int[] aHiddenDims, Vector aHiddenWeights, Vector aHiddenWeightsBackward, IntVector aIndexToBackward, Vector aHiddenBiases, Vector aOutputWeight, double[] aOutputBias) {
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
        if (mOutputBias.length != 1) throw new IllegalArgumentException("The size of output biases mismatch");
        mHiddenOutputs = Vectors.zeros(mHiddenBiasesSize);
        mHiddenGrads = Vectors.zeros(mHiddenBiasesSize);
        mHiddenGrads2 = Vectors.zeros(mHiddenBiasesSize);
        mHiddenGrads3 = Vectors.zeros(mHiddenBiasesSize);
    }
    
    public FeedForward threadSafeRef() {
        return new FeedForward(mInputDim, mHiddenDims, mHiddenWeights, mHiddenWeightsBackward, mIndexToBackward, mHiddenBiases, mOutputWeight, mOutputBias);
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
        
        return new FeedForward(aInputDim, aHiddenDims, aHiddenWeights, aHiddenWeightsBackward, aIndexToBackward, aHiddenBiases, aOutputWeight, new double[]{aOutputBias});
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
        
        return new FeedForward(aInputDim, aHiddenDims, aHiddenWeights, aHiddenWeightsBackward, aIndexToBackward, aHiddenBiases, aOutputWeight, new double[]{aOutputBias});
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "feed_forward");
        rSaveTo.put("input_dim", mInputDim);
        rSaveTo.put("hidden_dims", mHiddenDims);
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
    
    @Override public int inputSize() {
        return mInputDim;
    }
    @Override public double eval(DoubleArrayVector aX) {
        return forward(aX, null, null);
    }
    public double forward(DoubleArrayVector aX, @Nullable DoubleArrayVector rHiddenOutputs, @Nullable DoubleArrayVector rHiddenGrads) {
        boolean aRequireGrad = rHiddenOutputs!=null;
        if (aRequireGrad && rHiddenGrads==null) throw new NullPointerException();
        return forward0(aX, aRequireGrad?rHiddenOutputs:mHiddenOutputs, aRequireGrad?rHiddenGrads:null);
    }
    double forward0(IDataShell<double[]> aX, @NotNull IDataShell<double[]> rHiddenOutputs, @Nullable IDataShell<double[]> rHiddenGrads) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        return forward1(aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), mInputDim, mHiddenDims, mHiddenNumber,
                        mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0),
                        mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias[0],
                        rHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize), rHiddenOutputs.internalDataShift(),
                        rHiddenGrads==null?null:rHiddenGrads.internalDataWithLengthCheck(mHiddenBiasesSize), rHiddenGrads==null?0:rHiddenGrads.internalDataShift());
    }
    private static native double forward1(double[] aX, int aShiftX, int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                          double[] aHiddenWeights, double[] aHiddenBiases, double[] aOutputWeight, double aOutputBias,
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
        return forwardGrad0(aX, rGradX, aRequireGradGrad?rHiddenOutputs:mHiddenOutputs, aRequireGradGrad?rHiddenGrads:mHiddenGrads,
                            aRequireGradGrad?rHiddenGrads2:mHiddenGrads2, aRequireGradGrad?rHiddenGrads3:mHiddenGrads3, aRequireGradGrad?rHiddenGradGrads:null);
    }
    double forwardGrad0(IDataShell<double[]> aX, IDataShell<double[]> rGradX, @NotNull IDataShell<double[]> rHiddenOutputs, @NotNull IDataShell<double[]> rHiddenGrads,
                        @NotNull IDataShell<double[]> rHiddenGrads2, @NotNull IDataShell<double[]> rHiddenGrads3, @Nullable IDataShell<double[]> rHiddenGradGrads) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        return forwardGrad1(aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), rGradX.internalDataWithLengthCheck(mInputDim), rGradX.internalDataShift(),
                            mInputDim, mHiddenDims, mHiddenNumber,
                            mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0),
                            mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias[0],
                            rHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize), rHiddenOutputs.internalDataShift(),
                            rHiddenGrads.internalDataWithLengthCheck(mHiddenBiasesSize), rHiddenGrads.internalDataShift(),
                            rHiddenGrads2.internalDataWithLengthCheck(mHiddenBiasesSize), rHiddenGrads2.internalDataShift(),
                            rHiddenGrads3.internalDataWithLengthCheck(mHiddenBiasesSize), rHiddenGrads3.internalDataShift(),
                            rHiddenGradGrads==null?null:rHiddenGradGrads.internalDataWithLengthCheck(mHiddenBiasesSize), rHiddenGradGrads==null?0:rHiddenGradGrads.internalDataShift());
    }
    private static native double forwardGrad1(double[] aX, int aShiftX, double[] rGradX, int aShiftGradX,
                                              int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                              double[] aHiddenWeights, double[] aHiddenWeightsBackward, double[] aHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                              double[] rHiddenOutputs, int aShiftOutputs, double[] rHiddenGrads, int aShiftGrads,
                                              double[] rHiddenGrads2, int aShiftGrads2, double[] rHiddenGrads3, int aShiftGrads3, double[] rHiddenGradGrads, int aShiftGradGrads);
    
    
    public void backward(double aYGrad, DoubleArrayVector aX, DoubleArrayVector rGradPara, DoubleArrayVector aHiddenOutputs, DoubleArrayVector aHiddenGrads) {
        backward0(aYGrad, aX, rGradPara, aHiddenOutputs, aHiddenGrads);
    }
    void backward0(double aYGrad, IDataShell<double[]> aX, IDataShell<double[]> rGradPara, IDataShell<double[]> aHiddenOutputs, IDataShell<double[]> aHiddenGrads) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        backward1(aYGrad, aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(mHiddenWeightsSize+mHiddenBiasesSize+mOutputWeightSize+1), rGradPara.internalDataShift(),
                  mInputDim, mHiddenDims, mHiddenNumber,
                  mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0),
                  aHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize), aHiddenOutputs.internalDataShift(),
                  aHiddenGrads.internalDataWithLengthCheck(mHiddenBiasesSize), aHiddenGrads.internalDataShift(),
                  mHiddenGrads2.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mHiddenGrads3.internalDataWithLengthCheck(mHiddenBiasesSize, 0));
    }
    private static native void backward1(double aYGrad, double[] aX, int aShiftX, double[] rGradPara, int aShiftGradPara,
                                         int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                         double[] aHiddenWeightsBackward, double[] aOutputWeight,
                                         double[] aHiddenOutputs, int aShiftOutputs, double[] aHiddenGrads, int aShiftGrads, double[] rHiddenGrads2, double[] rHiddenGrads3);
    
    public void gradBackward(DoubleArrayVector aGradXGrad, DoubleArrayVector aX, DoubleArrayVector rGradPara, DoubleArrayVector aHiddenOutputs, DoubleArrayVector aHiddenGrads,
                             DoubleArrayVector aHiddenGrads2, DoubleArrayVector aHiddenGrads3, DoubleArrayVector aHiddenGradGrads) {
        gradBackward0(aGradXGrad, aX, rGradPara, aHiddenOutputs, aHiddenGrads, aHiddenGrads2, aHiddenGrads3, aHiddenGradGrads);
    }
    void gradBackward0(IDataShell<double[]> aGradXGrad, IDataShell<double[]> aX, IDataShell<double[]> rGradPara, IDataShell<double[]> aHiddenOutputs, IDataShell<double[]> aHiddenGrads,
                       IDataShell<double[]> aHiddenGrads2, IDataShell<double[]> aHiddenGrads3, IDataShell<double[]> aHiddenGradGrads) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        gradBackward1(aGradXGrad.internalDataWithLengthCheck(mInputDim), aGradXGrad.internalDataShift(), aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(),
                      rGradPara.internalDataWithLengthCheck(mHiddenWeightsSize+mHiddenBiasesSize+mOutputWeightSize+1), rGradPara.internalDataShift(),
                      mInputDim, mHiddenDims, mHiddenNumber,
                      mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0),  mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0),
                      aHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize), aHiddenOutputs.internalDataShift(),
                      aHiddenGrads.internalDataWithLengthCheck(mHiddenBiasesSize), aHiddenGrads.internalDataShift(),
                      aHiddenGrads2.internalDataWithLengthCheck(mHiddenBiasesSize), aHiddenGrads2.internalDataShift(),
                      aHiddenGrads3.internalDataWithLengthCheck(mHiddenBiasesSize), aHiddenGrads3.internalDataShift(),
                      aHiddenGradGrads.internalDataWithLengthCheck(mHiddenBiasesSize), aHiddenGradGrads.internalDataShift(),
                      mHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mHiddenGrads2.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mHiddenGrads3.internalDataWithLengthCheck(mHiddenBiasesSize, 0));
    }
    private static native void gradBackward1(double[] aGradXGrad, int aShiftGradXGrad, double[] aX, int aShiftX, double[] rGradPara, int aShiftGradPara,
                                             int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                             double[] aHiddenWeights, double[] aHiddenWeightsBackward, double[] aOutputWeight,
                                             double[] aHiddenOutputs, int aShiftOutputs, double[] aHiddenGrads, int aShiftGrads,
                                             double[] aHiddenGrads2, int aShiftGrads2, double[] aHiddenGrads3, int aShiftGrads3, double[] aHiddenGradGrads, int aShiftGradGrads,
                                             double[] rHiddenOutputs2, double[] rHiddenGrads4, double[] rHiddenGrads5);
}
