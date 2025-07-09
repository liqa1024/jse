package jse.opt;

import jse.math.vector.IVector;
import jse.math.vector.Vectors;

/**
 * 共轭梯度算法实现优化器，这里采用
 * Polak-Ribiere 的变体实现（和 lammps 一致）：
 * <pre> {@code
 * r_0 <- df/dx
 * p_0 <- -r_0
 * for each k:
 *   x_k <- x_k + eta * p_k
 *   r_k+1 <- df/dx_k
 *   b_k+1 <- (r_(k+1) · (r_(k+1) - r_k)) / (r_k · r_k)
 *   p_k+1 <- -r_k+1 + b_k+1 p_k
 * } </pre>
 * 主要可以避免 Fletcher-Reeves 实现中收敛性差的问题
 *
 * @see IOptimizer
 * @author liqa
 */
public class ConjugateGradient extends AbstractOptimizer {
    protected IVector mLastGrad = null, mLastStep = null;
    protected boolean mIsFirst = true;
    protected double mEta;
    
    /**
     * 创建一个共轭梯度优化器
     * @param aEta 使用的迭代步长，默认为 {@code 0.1}
     */
    public ConjugateGradient(double aEta) {
        mEta = aEta;
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
        mLastGrad = Vectors.zeros(aParameter.size());
        mLastStep = Vectors.zeros(aParameter.size());
        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override public void reset() {
        mIsFirst = true;
    }
    
    /**
     * {@inheritDoc}
     * @param aStep {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override protected double calStep(int aStep) {
        double tLoss = eval(true);
        IVector tGrad = grad();
        if (mIsFirst) {
            mIsFirst = false;
            tGrad.operation().multiply2dest(-1, mLastStep);
            tGrad.operation().multiply2dest(-mEta, mParameterStep);
            mLastGrad.fill(tGrad);
            return tLoss;
        }
        double tDiv = mLastGrad.operation().dot();
        mLastGrad.operation().lminus2this(tGrad);
        final double tBeta = tGrad.operation().dot(mLastGrad) / tDiv;
        if (tBeta < 0) {
            tGrad.operation().multiply2dest(-1, mLastStep);
            tGrad.operation().multiply2dest(-mEta, mParameterStep);
            mLastGrad.fill(tGrad);
            return tLoss;
        }
        mLastStep.operation().operate2this(tGrad, (p, r) -> -r + tBeta*p);
        mLastStep.operation().multiply2dest(mEta, mParameterStep);
        mLastGrad.fill(tGrad);
        return tLoss;
    }
}
