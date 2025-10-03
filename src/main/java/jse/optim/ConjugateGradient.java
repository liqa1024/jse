package jse.optim;

import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;

/**
 * 共轭梯度算法实现优化器，这里采用
 * Polak-Ribiere 的变体实现（和 lammps 一致）：
 * <pre> {@code
 * r_0 <- df/dx_0
 * p_0 <- -r_0
 * for-each k:
 *   x_k <- x_k + eta * p_k
 *   r_(k+1) <- df/dx_k
 *   b_(k+1) <- (r_(k+1) · (r_(k+1) - r_k)) / (r_k · r_k)
 *   p_(k+1) <- -r_k+1 + b_k+1 p_k
 * } </pre>
 * 主要可以避免 Fletcher-Reeves 实现中收敛性差的问题
 * <p>
 * 默认会打开 {@link #setLineSearch()} 来达到 cg 应有的收敛速度
 *
 * @see IOptimizer
 * @author liqa
 */
public class ConjugateGradient extends AbstractOptimizer {
    protected Vector mLastGrad = null, mLastStep = null;
    protected boolean mIsFirst = true;
    protected boolean mAdaptiveEta = true;
    protected double mEta;
    
    /**
     * 创建一个共轭梯度优化器
     * @param aEta 使用的初始迭代步长，默认为 {@code 0.1}
     */
    public ConjugateGradient(double aEta) {
        mEta = aEta;
        setLineSearch();
    }
    /**
     * 创建一个共轭梯度优化器
     * @see #ConjugateGradient(double)
     */
    public ConjugateGradient() {this(0.1);}
    
    /**
     * {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public ConjugateGradient setParameter(IVector aParameter) {
        super.setParameter(aParameter);
        if (aParameter != null) mLastGrad = Vectors.zeros(aParameter.size());
        if (aParameter != null) mLastStep = Vectors.zeros(aParameter.size());
        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override public void reset() {
        mIsFirst = true;
    }
    
    public ConjugateGradient setAdaptiveEta(boolean aAdaptiveEta) {
        mAdaptiveEta = aAdaptiveEta;
        return this;
    }
    
    @Override protected boolean lineSearchAlways() {
        return true;
    }
    @Override protected void updateLearningRate(double aLR) {
        if (mAdaptiveEta) {
            if (aLR > 1.5) mEta *= 1.5;
            else if (aLR < 0.7) mEta *= 0.7;
        }
    }
    
    /**
     * {@inheritDoc}
     * @param aStep {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @param rParameterStep {@inheritDoc}
     */
    @Override protected void calStep(int aStep, IVector aParameter, Vector rParameterStep) {
        double tLoss = eval(true);
        IVector tGrad = grad();
        if (mIsFirst) {
            mIsFirst = false;
            tGrad.operation().multiply2dest(-1, mLastStep);
            tGrad.operation().multiply2dest(-mEta, rParameterStep);
            mLastGrad.fill(tGrad);
            return;
        }
        double tDiv = mLastGrad.operation().dot();
        mLastGrad.operation().lminus2this(tGrad);
        final double tBeta = tGrad.operation().dot(mLastGrad) / tDiv;
        if (tBeta < 0) {
            tGrad.operation().multiply2dest(-1, mLastStep);
            tGrad.operation().multiply2dest(-mEta, rParameterStep);
            mLastGrad.fill(tGrad);
            return;
        }
        mLastStep.operation().operate2this(tGrad, (p, r) -> -r + tBeta*p);
        mLastStep.operation().multiply2dest(mEta, rParameterStep);
        mLastGrad.fill(tGrad);
    }
}
