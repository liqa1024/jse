package jsex.nnap.model;

import jse.cache.VectorCache;
import jse.code.UT;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;

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
public class FeedForward extends Model {
    final int mInputDim;
    final int[] mHiddenDims;
    final int mHiddenNumber;
    final RowMatrix[] mHiddenWeights;
    final Vector[] mHiddenBiases;
    final Vector mOutputWeight;
    final double mOutputBias;
    
    public FeedForward(int aInputDim, int[] aHiddenDims, RowMatrix[] aHiddenWeights, Vector[] aHiddenBiases, Vector aOutputWeight, double aOutputBias) {
        mInputDim = aInputDim;
        mHiddenDims = aHiddenDims;
        mHiddenNumber = aHiddenDims.length;
        if (mHiddenNumber == 0) throw new IllegalArgumentException("At least one hidden layer is required");
        if (aHiddenWeights.length != mHiddenNumber) throw new IllegalArgumentException("The number of hidden weights mismatch");
        if (aHiddenBiases.length != mHiddenNumber) throw new IllegalArgumentException("The number of hidden biases mismatch");
        mHiddenWeights = aHiddenWeights;
        mHiddenBiases = aHiddenBiases;
        mOutputWeight = aOutputWeight;
        mOutputBias = aOutputBias;
        int tColNum = mInputDim;
        for (int i = 0; i < mHiddenNumber; ++i) {
            if (mHiddenWeights[i].columnNumber() != tColNum) throw new IllegalArgumentException("Column number of hidden weight '"+i+"' mismatch");
            if (mHiddenWeights[i].rowNumber() != mHiddenDims[i]) throw new IllegalArgumentException("Row number of hidden weight '"+i+"' mismatch");
            if (mHiddenBiases[i].size() != mHiddenDims[i]) throw new IllegalArgumentException("Size of hidden bias '"+i+"' mismatch");
            tColNum = mHiddenDims[i];
        }
        if (mOutputWeight.size() != mHiddenDims[mHiddenNumber-1]) throw new IllegalArgumentException("Size of output weight mismatch");
        
        mHiddenOutputs = new Vector[mHiddenNumber-1];
        mHiddenGrads = new Vector[mHiddenNumber];
        for (int i = 0; i < mHiddenNumber; i++) {
            if (i < mHiddenOutputs.length) mHiddenOutputs[i] = VectorCache.getVec(mHiddenDims[i]);
            mHiddenGrads[i] = VectorCache.getVec(mHiddenDims[i]);
        }
        
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static FeedForward load(Map aMap) {
        int aInputDim = ((Number)UT.Code.get(aMap, "input_dim")).intValue();
        List<?> tHiddenDims = (List<?>)UT.Code.get(aMap, "hidden_dims");
        int[] aHiddenDims = new int[tHiddenDims.size()];
        for (int i = 0; i < aHiddenDims.length; ++i) {
            aHiddenDims[i] = ((Number)tHiddenDims.get(i)).intValue();
        }
        List<?> tHiddenWeights = (List<?>)UT.Code.get(aMap, "hidden_weights");
        RowMatrix[] aHiddenWeights = new RowMatrix[tHiddenWeights.size()];
        for (int i = 0; i < aHiddenWeights.length; ++i) {
            aHiddenWeights[i] = Matrices.fromRows((List<?>)tHiddenWeights.get(i));
        }
        List<?> tHiddenBiases = (List<?>)UT.Code.get(aMap, "hidden_biases");
        Vector[] aHiddenBiases = new Vector[tHiddenBiases.size()];
        for (int i = 0; i < aHiddenBiases.length; ++i) {
            aHiddenBiases[i] = Vectors.from((List<? extends Number>)tHiddenBiases.get(i));
        }
        Vector aOutputWeight = Vectors.from((List<? extends Number>)aMap.get("output_weight"));
        double aOutputBias = ((Number)aMap.get("output_bias")).doubleValue();
        
        return new FeedForward(aInputDim, aHiddenDims, aHiddenWeights, aHiddenBiases, aOutputWeight, aOutputBias);
    }
    
    
    /// 缓存中间变量
    private final Vector[] mHiddenOutputs, mHiddenGrads;
    
    @Override protected void shutdown_() {
        for (int i = 0; i < mHiddenNumber; ++i) {
            if (i < mHiddenOutputs.length) VectorCache.returnVec(mHiddenOutputs[i]);
            VectorCache.returnVec(mHiddenGrads[i]);
        }
    }
    static double silu(double aX) {
        return aX / (1.0 + MathEX.Fast.exp(-aX));
    }
    static double siluGrad(double aX) {
        double tSigmoid = 1.0 / (1.0 + MathEX.Fast.exp(-aX));
        return tSigmoid * (1 + aX * (1 - tSigmoid));
    }
    
    double forward_(DoubleArrayVector aX, boolean aRequireGrad) {
        IVector tInput = aX;
        int tInSize = mInputDim;
        final int tEnd = mHiddenNumber - 1;
        for (int i = 0; i < tEnd; ++i) {
            IVector tOutput = mHiddenOutputs[i];
            IVector tGrad = aRequireGrad ? mHiddenGrads[i] : null;
            RowMatrix tWeights = mHiddenWeights[i];
            Vector tBiases = mHiddenBiases[i];
            int tOutSize = mHiddenDims[i];
            for (int j = 0; j < tOutSize; ++j) {
                IVector tRow = tWeights.row(j);
                double rSum = tBiases.get(j);
                for (int k = 0; k < tInSize; ++k) {
                    rSum += tInput.get(k) * tRow.get(k);
                }
                tOutput.set(j, silu(rSum));
                if (aRequireGrad) tGrad.set(j, siluGrad(rSum));
            }
            tInput = tOutput;
            tInSize = tOutSize;
        }
        // 最后一层特殊优化
        double rOut = mOutputBias;
        IVector tGrad = aRequireGrad ? mHiddenGrads[tEnd] : null;
        RowMatrix tWeights = mHiddenWeights[tEnd];
        Vector tBiases = mHiddenBiases[tEnd];
        int tOutSize = mHiddenDims[tEnd];
        for (int j = 0; j < tOutSize; ++j) {
            IVector tRow = tWeights.row(j);
            double rSum = tBiases.get(j);
            for (int k = 0; k < tInSize; ++k) {
                rSum += tInput.get(k) * tRow.get(k);
            }
            double tWeight = mOutputWeight.get(j);
            rOut += silu(rSum) * tWeight;
            if (aRequireGrad) tGrad.set(j, siluGrad(rSum) * tWeight);
        }
        return rOut;
    }
    
    @Override public double forward(DoubleArrayVector aX) {
        return forward_(aX, false);
    }
    
    @Override public double backward(DoubleArrayVector aX, DoubleArrayVector rGradX) {
        double tY = forward_(aX, true);
        // 开始反向传播，最后一层已经经过特殊优化
        final int tEnd = mHiddenNumber - 1;
        IVector tInput = mHiddenGrads[tEnd];
        int tInputSize = mHiddenDims[tEnd];
        for (int i = tEnd-1; i >= 0; --i) {
            IVector tGrad = mHiddenGrads[i];
            IVector tOutput = mHiddenOutputs[i];
            RowMatrix tWeights = mHiddenWeights[i+1];
            int tOutSize = mHiddenDims[i];
            for (int j = 0; j < tOutSize; ++j) {
                IVector tCol = tWeights.col(j);
                double rSum = 0.0;
                for (int k = 0; k < tInputSize; ++k) {
                    rSum += tInput.get(k) * tCol.get(k);
                }
                tOutput.set(j, rSum * tGrad.get(j));
            }
            tInput = tOutput;
            tInputSize = tOutSize;
        }
        // 输入层赋值
        RowMatrix tWeights = mHiddenWeights[0];
        for (int j = 0; j < mInputDim; ++j) {
            IVector tCol = tWeights.col(j);
            double rSum = 0.0;
            for (int k = 0; k < tInputSize; ++k) {
                rSum += tInput.get(k) * tCol.get(k);
            }
            rGradX.set(j, rSum);
        }
        return tY;
    }
}
