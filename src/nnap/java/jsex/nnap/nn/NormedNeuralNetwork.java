package jsex.nnap.nn;

import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;

/**
 * 通用的归一化后的神经网络实现
 * <p>
 * 通过内部缓存一个输入向量来避免对输入变量的直接修改，即使目前的使用可能不需要这个功能
 * @author liqa
 */
public class NormedNeuralNetwork extends NeuralNetwork {
    private final NeuralNetwork mNN;
    private final IVector mNormMu, mNormSigma;
    private final double mNormMuEng, mNormSigmaEng;
    private final Vector mNormedX;
    private final int mInputDim;
    public NormedNeuralNetwork(NeuralNetwork aNN, IVector aNormMu, IVector aNormSigma, double aNormMuEng, double aNormSigmaEng) {
        mNN = aNN;
        mNormMu = aNormMu;
        mNormSigma = aNormSigma;
        mNormMuEng = aNormMuEng;
        mNormSigmaEng = aNormSigmaEng;
        mInputDim = mNN.inputSize();
        mNormedX = Vectors.zeros(mInputDim);
    }
    
    @Override public int inputSize() {
        return mNN.inputSize();
    }
    @Override public double forward(DoubleArrayVector aX) throws Exception {
        for (int i = 0; i < mInputDim; ++i) {
            mNormedX.set(i, (aX.get(i) - mNormMu.get(i)) / mNormSigma.get(i));
        }
        double tPred = mNN.forward(mNormedX);
        return tPred*mNormSigmaEng + mNormMuEng;
    }
    @Override public double backward(DoubleArrayVector aX, DoubleArrayVector rGradX) throws Exception {
        for (int i = 0; i < mInputDim; ++i) {
            mNormedX.set(i, (aX.get(i) - mNormMu.get(i)) / mNormSigma.get(i));
        }
        double tPred = mNN.backward(mNormedX, rGradX);
        rGradX.div2this(mNormSigma);
        rGradX.multiply2this(mNormSigmaEng);
        return tPred*mNormSigmaEng + mNormMuEng;
    }
    
    @Override protected void shutdown_() {mNN.shutdown();}
}
