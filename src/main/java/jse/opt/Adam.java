package jse.opt;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;

/**
 * <a href="https://en.wikipedia.org/wiki/Stochastic_gradient_descent#Adam">Adam 优化器</a>
 * 实现：
 * <pre> {@code
 * m_k <- b1 m_(k-1) + (1 - b1) df/dx_k
 * v_k <- b2 v_(k-1) + (1 - b2) (df/dx_k)^2
 * _m_k <- m_k / (1 - b1^k)
 * _v_k <- v_k / (1 - b2^k)
 * x_k <- x_(k-1) - eta * _m_k / (sqrt(_v_k) + eps)
 * } </pre>
 * 主要针对有一定随机性的目标函数优化
 *
 * @see IOptimizer
 * @author liqa
 */
public class Adam extends AbstractOptimizer {
    protected double mEta;
    protected double mBeta1, mBeta2;
    protected double mEps;
    
    protected Vector mMomentum = null, mVariance = null;
    protected double mBeta1Prod = 1.0, mBeta2Prod = 1.0;
    
    /**
     * 创建一个 Adam 优化器
     * @param aEta 使用的学习率，默认为 {@code 0.001}
     * @param aBeta1 计算梯度平均值的系数，默认为 {@code 0.9}
     * @param aBeta2 计算梯度平方平均值的系数，默认为 {@code 0.999}
     * @param aEps 在分母中添加项以提高数值稳定性，默认为 {@code 1e-8}
     */
    public Adam(double aEta, double aBeta1, double aBeta2, double aEps) {
        mEta = aEta;
        mBeta1 = aBeta1;
        mBeta2 = aBeta2;
        mEps = aEps;
    }
    /**
     * 创建一个 Adam 优化器
     * @param aEta 使用的学习率，默认为 {@code 0.001}
     * @see #Adam(double, double, double, double)
     */
    public Adam(double aEta) {this(aEta, 0.9, 0.999, 1e-8);}
    /**
     * 创建一个 Adam 优化器
     * @see #Adam(double, double, double, double)
     */
    public Adam() {this(0.001);}
    
    
    /**
     * 设置学习率
     * @param aLearningRate 新的学习率
     * @return 自身方便链式调用
     */
    public Adam setLearningRate(double aLearningRate) {
        mEta = aLearningRate;
        return this;
    }
    
    /**
     * {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Adam setParameter(IVector aParameter) {
        super.setParameter(aParameter);
        if (aParameter != null) mMomentum = Vectors.zeros(aParameter.size());
        if (aParameter != null) mVariance = Vectors.zeros(aParameter.size());
        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override public void reset() {
        if (mMomentum != null) mMomentum.fill(0.0);
        if (mVariance != null) mVariance.fill(0.0);
        mBeta1Prod = 1.0;
        mBeta2Prod = 1.0;
    }
    
    /**
     * {@inheritDoc}
     * @param aStep {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @param rParameterStep {@inheritDoc}
     */
    @Override protected void calStep(int aStep, IVector aParameter, Vector rParameterStep) {
        IVector tGrad = grad();
        mMomentum.operation().operate2this(tGrad, (l, r) -> mBeta1*l + (1-mBeta1)*r);
        mVariance.operation().operate2this(tGrad, (l, r) -> mBeta2*l + (1-mBeta2)*r*r);
        mBeta1Prod *= mBeta1;
        mBeta2Prod *= mBeta2;
        mMomentum.operation().operate2dest(mVariance, rParameterStep,
                                           (m, v) -> -mEta * m/(1-mBeta1Prod) / (MathEX.Fast.sqrt(v/(1-mBeta2Prod)) + mEps));
    }
}
