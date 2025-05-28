package jsex.nnap.nn;

import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.math.IDataShell;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jsex.nnap.NNAP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final IDataShell<double[]> mHiddenWeights, mHiddenWeightsBackward;
    private final IDataShell<double[]> mHiddenBiases;
    private final IDataShell<double[]> mOutputWeight;
    private final double mOutputBias;
    private final int mHiddenNumber, mHiddenWeightsSize, mHiddenBiasesSize, mOutputWeightSize;
    
    /// 缓存中间变量
    private final IDataShell<double[]> mHiddenOutputs, mHiddenGrads;
    
    FeedForward(int aInputDim, int[] aHiddenDims, IDataShell<double[]> aHiddenWeights, IDataShell<double[]> aHiddenWeightsBackward, IDataShell<double[]> aHiddenBiases, IDataShell<double[]> aOutputWeight, double aOutputBias) {
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
        DoubleList aHiddenWeights = new DoubleList(tHiddenWeightsSize);
        DoubleList aHiddenWeightsBackward = new DoubleList(tHiddenWeightsSize);
        List<RowMatrix> tWeightList = new ArrayList<>(tHiddenNumber);
        tColNum = aInputDim;
        for (int i = 0; i < tHiddenNumber; ++i) {
            RowMatrix tWeight = Matrices.fromRows((List<?>)tHiddenWeights.get(i));
            if (tWeight.columnNumber() != tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
            if (tWeight.rowNumber() != aHiddenDims[i]) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
            aHiddenWeights.addAll(tWeight.asVecRow());
            tWeightList.add(tWeight);
            tColNum = aHiddenDims[i];
        }
        for (int i = tHiddenNumber-1; i >=0 ; --i) {
            aHiddenWeightsBackward.addAll(tWeightList.get(i).asVecCol());
        }
        List<?> tHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        if (tHiddenBiases.size() != tHiddenNumber) throw new IllegalArgumentException("The number of hidden biases mismatch");
        DoubleList aHiddenBiases = new DoubleList(tHiddenBiasesSize);
        for (int i = 0; i < tHiddenNumber; ++i) {
            Vector tBias = Vectors.from((List<? extends Number>)tHiddenBiases.get(i));
            if (tBias.size() != aHiddenDims[i]) throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch");
            aHiddenBiases.addAll(tBias);
        }
        Vector aOutputWeight = Vectors.from((List<? extends Number>)aMap.get("output_weight"));
        double aOutputBias = ((Number)aMap.get("output_bias")).doubleValue();
        if (aOutputWeight.size() != aHiddenDims[tHiddenNumber-1]) throw new IllegalArgumentException("Size of output weight mismatch");
        
        return new FeedForward(aInputDim, aHiddenDims, aHiddenWeights, aHiddenWeightsBackward, aHiddenBiases, aOutputWeight, aOutputBias);
    }
    
    @Override public double forward(DoubleArrayVector aX) {
        return forward0(aX);
    }
    double forward0(IDataShell<double[]> aX) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        int tShiftX = aX.internalDataShift();
        return forward1(aX.internalDataWithLengthCheck(mInputDim, tShiftX), tShiftX, mInputDim, mHiddenDims, mHiddenNumber,
                        mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0),
                        mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias,
                        mHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize, 0));
    }
    private static native double forward1(double[] aX, int aShiftX, int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                          double[] aHiddenWeights, double[] aHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                          double[] rHiddenOutputs);
    
    @Override public double backward(DoubleArrayVector aX, DoubleArrayVector rGradX) {
        return backward0(aX, rGradX);
    }
    double backward0(IDataShell<double[]> aX, IDataShell<double[]> rGradX) {
        if (mHiddenDims.length < mHiddenNumber) throw new IllegalArgumentException("data size mismatch");
        int tShiftX = aX.internalDataShift();
        int tShiftGradX = rGradX.internalDataShift();
        return backward1(aX.internalDataWithLengthCheck(mInputDim, tShiftX), tShiftX, rGradX.internalDataWithLengthCheck(mInputDim, tShiftGradX), tShiftGradX, mInputDim, mHiddenDims, mHiddenNumber,
                         mHiddenWeights.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenWeightsBackward.internalDataWithLengthCheck(mHiddenWeightsSize, 0), mHiddenBiases.internalDataWithLengthCheck(mHiddenBiasesSize, 0),
                         mOutputWeight.internalDataWithLengthCheck(mOutputWeightSize, 0), mOutputBias,
                         mHiddenOutputs.internalDataWithLengthCheck(mHiddenBiasesSize, 0), mHiddenGrads.internalDataWithLengthCheck(mHiddenBiasesSize, 0));
    }
    private static native double backward1(double[] aX, int aShiftX, double [] rGradX, int aShiftGradX, int aInputDim, int[] aHiddenDims, int aHiddenNumber,
                                           double[] aHiddenWeights, double[] aHiddenWeightsBackward, double[] aHiddenBiases, double[] aOutputWeight, double aOutputBias,
                                           double[] rHiddenOutputs, double[] rHiddenGrads);
}
