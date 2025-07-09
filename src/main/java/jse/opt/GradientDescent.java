package jse.opt;

/**
 * 最简单的梯度下降算法实现优化器：
 * <pre> {@code
 * x <- x - eta * df/dx
 * } </pre>
 * @see IOptimizer
 * @author liqa
 */
public class GradientDescent extends AbstractOptimizer {
    protected double mEta;
    /**
     * 创建一个梯度下降优化器
     * @param aEta 使用的迭代步长，默认为 {@code 0.1}
     */
    public GradientDescent(double aEta) {
        mEta = aEta;
    }
    /**
     * 创建一个梯度下降优化器
     * @see #GradientDescent(double)
     */
    public GradientDescent() {this(0.1);}
    
    /**
     * {@inheritDoc}
     * @param aStep {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override protected double calStep(int aStep) {
        double tLoss = eval(true);
        grad().operation().multiply2dest(-mEta, mParameterStep);
        return tLoss;
    }
}
