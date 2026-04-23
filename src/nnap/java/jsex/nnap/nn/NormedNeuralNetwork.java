package jsex.nnap.nn;

import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.Nullable;

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
    public NormedNeuralNetwork(NeuralNetwork aNN, @Nullable IVector aNormMu, @Nullable IVector aNormSigma, double aNormMuEng, double aNormSigmaEng) {
        mNN = aNN;
        final boolean tNoBasisNorm = aNormMu==null && aNormSigma==null;
        mNormMu = tNoBasisNorm ? null : (aNormMu==null ? Vectors.zeros(mNN.inputSize()) : aNormMu);
        mNormSigma = tNoBasisNorm ? null : (aNormSigma==null ? Vectors.ones(mNN.inputSize()) : aNormSigma);
        mNormMuEng = aNormMuEng;
        mNormSigmaEng = aNormSigmaEng;
        mNormedX = tNoBasisNorm ? null : Vectors.zeros(mNN.inputSize());
    }
    @Override public NormedNeuralNetwork threadSafeRef() throws Exception {
        return new NormedNeuralNetwork(mNN.threadSafeRef(), mNormMu,  mNormSigma, mNormMuEng, mNormSigmaEng);
    }
    
    @Override public int inputSize() {
        return mNN.inputSize();
    }
    @Override public double eval(DoubleArrayVector aX) throws Exception {
        if (mNormedX == null) {
            double tPred = mNN.eval(aX);
            return tPred*mNormSigmaEng + mNormMuEng;
        }
        mNormedX.fill(i -> (aX.get(i) - mNormMu.get(i)) / mNormSigma.get(i));
        double tPred = mNN.eval(mNormedX);
        return tPred*mNormSigmaEng + mNormMuEng;
    }
    @Override public double evalGrad(DoubleArrayVector aX, DoubleArrayVector rGradX) throws Exception {
        if (mNormedX == null) {
            double tPred = mNN.evalGrad(aX, rGradX);
            rGradX.multiply2this(mNormSigmaEng);
            return tPred*mNormSigmaEng + mNormMuEng;
        }
        mNormedX.fill(i -> (aX.get(i) - mNormMu.get(i)) / mNormSigma.get(i));
        double tPred = mNN.evalGrad(mNormedX, rGradX);
        rGradX.div2this(mNormSigma);
        rGradX.multiply2this(mNormSigmaEng);
        return tPred*mNormSigmaEng + mNormMuEng;
    }
    
    @Override protected void close_() {mNN.close();}
}
