package jse.opt;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;

import static jse.math.MathEX.Code.DBL_EPSILON;

/**
 * 通用的优化器类，用于减少重复实现
 * @see IOptimizer
 * @author liqa
 */
public abstract class AbstractOptimizer implements IOptimizer {
    protected IVector mParameter = null, mParameterStep = null;
    protected ILossFunc mLossFunc = null;
    protected ILossFuncGrad mLossFuncGrad = null;
    protected double mGamma = Double.NaN, mC1 = Double.NaN;
    protected boolean mLineSearch = false;
    
    private IVector mGrad = null;
    private boolean mGradValid = false;
    
    /**
     * 调用此方法设置当前 {@link #mParameter}
     * 下对应的梯度，可用于线搜索之类的算法直接使用
     * @param aGrad 需要设置的梯度值
     */
    protected void setGrad(IVector aGrad) {
        mGrad.fill(aGrad);
        mGradValid = true;
    }
    
    /**
     * {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setParameter(IVector aParameter) {
        mParameter = aParameter;
        if (mParameter != null) mParameterStep = Vectors.zeros(aParameter.size());
        if (mParameter != null) mGrad = Vectors.zeros(aParameter.size());
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aLossFunc {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setLossFunc(ILossFunc aLossFunc) {
        mLossFunc = aLossFunc;
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aLossFuncGrad {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setLossFuncGrad(ILossFuncGrad aLossFuncGrad) {
        mLossFuncGrad = aLossFuncGrad;
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aGamma {@inheritDoc}
     * @param aC1 {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setLineSearch(double aGamma, double aC1) {
        mGamma = aGamma;
        mC1 = aC1;
        mLineSearch = true;
        return this;
    }
    
    /**
     * {@inheritDoc}
     * @param aMaxStep {@inheritDoc}
     * @param aPrintLog {@inheritDoc}
     */
    @Override public void run(int aMaxStep, boolean aPrintLog) {
        // 通用的优化器执行步骤，重写来实现特殊的迭代步骤
        checkSetting();
        double oLoss = Double.NaN;
        for (int step = 0; step < aMaxStep; ++step) {
            double tLoss = calStep();
            if (mLineSearch) lineSearch(tLoss);
            if (checkBreak(tLoss, oLoss)) break;
            applyStep();
            if (mLineSearch) mGradValid = false;
            printLog(step, tLoss);
            oLoss = tLoss;
        }
    }
    
    /**
     * 简单测试是否设置完全
     */
    protected void checkSetting() {
        if (mParameter == null) throw new IllegalStateException("no parameter set");
    }
    /**
     * 计算参数需要的迭代长度，并将计算结果写入 {@link #mParameterStep}。
     * 如果计算了梯度，应当调用 {@link #setGrad(IVector)} 设置梯度从而避免重复计算
     * @return 顺便返回得到的 loss 值
     */
    protected abstract double calStep();
    /**
     * 应用线搜索，这里使用 Armijo 线搜索。
     * 将线搜索结果写入 {@link #mParameterStep}
     */
    protected void lineSearch(double aLoss) {
        if (!mGradValid) {
            if (mLossFuncGrad == null) throw new IllegalStateException("no loss func gradient set");
            mLossFuncGrad.call(mParameter, mGrad);
        }
        while (true) {
            if (mLossFunc == null) throw new IllegalStateException("no loss func set");
            double tTarget = aLoss + mC1*mGrad.operation().dot(mParameterStep);
            mParameter.plus2this(mParameterStep);
            double tLoss = mLossFunc.call(mParameter);
            mParameter.minus2this(mParameterStep);
            if (tLoss <= tTarget) return;
            mParameterStep.multiply2this(mGamma);
        }
    }
    /**
     * 应用迭代步长，默认直接运算 {@code mParameter.plus2this(mParameterStep)}。
     * 重写来实现自定义的更新策略
     */
    protected void applyStep() {
        mParameter.plus2this(mParameterStep);
    }
    /**
     * 打印输出，重写实现自定义的打印需求
     * @param aStep 当前的迭代步数
     * @param aLoss 当前的 loss 值
     */
    protected void printLog(int aStep, double aLoss) {
        if (aStep == 0) System.out.printf("%12s  %24s  %12s\n", "step", "loss", "max_step");
        double tMaxStep = 0.0;
        final int tSize = mParameterStep.size();
        for (int i = 0; i < tSize; ++i) {
            double tStep = Math.abs(mParameterStep.get(i));
            if (tStep > tMaxStep) tMaxStep = tStep;
        }
        System.out.printf("%12d  %24.18g  %12.6g\n", aStep, aLoss, tMaxStep);
    }
    /**
     * 测试当前优化步是否可以终止，默认为优化到数值精度，即
     * {@link MathEX.Code#DBL_EPSILON}
     * @param aLoss 当前的 loss 值
     * @param aLastLoss 上一步的 loss 值，如果是第一步则为 {@link Double#NaN}
     * @return 是否进行中断
     */
    protected boolean checkBreak(double aLoss, double aLastLoss) {
        if (Double.isNaN(aLastLoss)) return false;
        return Math.abs(aLastLoss-aLoss) < Math.abs(aLastLoss)*DBL_EPSILON;
    }
}
