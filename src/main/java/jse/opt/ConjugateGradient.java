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
    protected IVector mLastGrad = null, mLastStep = null;
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
    
    public ConjugateGradient setAdaptiveEta(boolean aAdaptiveEta) {
        mAdaptiveEta = aAdaptiveEta;
        return this;
    }
    
    /**
     * {@link ConjugateGradient} 需要使用更加精确的线搜索来保证收敛速度
     * @param aStep {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override protected int lineSearch(int aStep, double aLoss) {
        int tStep = 0;
        double tAlpha = 1.0;
        double tGradA = grad().operation().dot(mParameterStep);
        if (tGradA >= 0) throw new IllegalStateException("positive gradient");
        // CG 总是会执行一次线搜索，从而找到合适的长度
        mParameter.plus2this(mParameterStep);
        double tLoss = eval();
        mParameter.minus2this(mParameterStep);
        // 二次样条拟合
        double tA = (tLoss - aLoss - tGradA*tAlpha) / (tAlpha*tAlpha);
        // 若拟合得到 tA < 0，则说明此区域不是正定的，保留原本步长即可
        if (tA <= 0) return tStep;
        // 获取适合的 tAlpha
        tAlpha = Math.min(-tGradA / (2*tA), 3.0*tAlpha);
        ++tStep;
        while (true) {
            double tTarget = aLoss + mC1*tGradA*tAlpha;
            mParameter.operation().mplus2this(mParameterStep, tAlpha);
            tLoss = eval();
            mParameter.operation().mplus2this(mParameterStep, -tAlpha);
            if (tLoss <= tTarget) {
                mParameterStep.multiply2this(tAlpha);
                if (mAdaptiveEta) {
                    if (tAlpha > 2.0) mEta *= 2.0;
                    else if (tAlpha < 0.5) mEta *= 0.5;
                }
                return tStep;
            }
            // 不满足则再次二次样条拟合
            tA = (tLoss - aLoss - tGradA*tAlpha) / (tAlpha*tAlpha);
            // 若拟合得到 tA < 0，则说明此区域不是正定的，保留原本步长即可
            if (tA <= 0) {
                mParameterStep.multiply2this(tAlpha);
                if (mAdaptiveEta) {
                    if (tAlpha > 2.0) mEta *= 2.0;
                    else if (tAlpha < 0.5) mEta *= 0.5;
                }
                return tStep;
            }
            // 获取适合的 tAlpha
            tAlpha = Math.min(-tGradA / (2*tA), 3.0*tAlpha);
            ++tStep;
        }
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
