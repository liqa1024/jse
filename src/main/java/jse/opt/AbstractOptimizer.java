package jse.opt;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static jse.math.MathEX.Code.DBL_EPSILON;

/**
 * 通用的优化器类，用于减少重复实现
 * @see IOptimizer
 * @author liqa
 */
public abstract class AbstractOptimizer implements IOptimizer {
    private IVector mParameter = null;
    private Vector mParameterStep = null;
    
    private ILossFunc mLossFunc = null;
    private ILossFuncGrad mLossFuncGrad = null;
    
    private ILogPrinter mLogPrinter = null;
    private IBreakChecker mBreakChecker = null;
    
    protected double mC1 = Double.NaN, mC2 = Double.NaN;
    protected int mMaxIter = -1;
    protected boolean mLineSearch = false;
    
    private Vector mGrad = null;
    private boolean mGradValid = false;
    private double mLoss = Double.NaN;
    private boolean mLossValid = false;
    
    
    /**
     * 调用此方法来进行损失函数值计算
     * @param aRequireGrad 是否要求顺便计算梯度值，默认为 {@code false}
     * @return 得到的损失函数值
     */
    protected double eval(boolean aRequireGrad) {
        if (aRequireGrad) {
            if (mLossFuncGrad == null) throw new IllegalStateException("no loss func gradient set");
            mGradValid = true;
            mLoss = mLossFuncGrad.call(mGrad);
            mLossValid = true;
            return mLoss;
        }
        if (mLossFunc == null) throw new IllegalStateException("no loss func set");
        mLoss = mLossFunc.call();
        mLossValid = true;
        return mLoss;
    }
    /**
     * 调用此方法来进行损失函数值计算
     * @return 得到的损失函数值
     */
    protected double eval() {
        return eval(false);
    }
    /**
     * 获取当前的梯度值，如果缓存不合法则自动重新计算
     * @return 当前的梯度值
     */
    protected Vector grad() {
        if (!mGradValid) eval(true);
        return mGrad;
    }
    /**
     * 获取当前的 loss 值，如果缓存不合法则自动重新计算
     * @return 当前的 loss 值
     */
    protected double loss() {
        if (!mLossValid) return eval();
        return mLoss;
    }
    /**
     * 清空当前缓存的梯度值，表明此时已经不合法；
     * 之后需要梯度时则会自动重新计算
     */
    protected void invalidGrad() {
        mGradValid = false;
    }
    /**
     * 清空当前缓存的 loss 值，会顺便清空梯度值，表明此时已经不合法；
     * 之后需要loss 或梯度时则会自动重新计算
     */
    protected void invalidLoss() {
        mGradValid = false;
        mLossValid = false;
    }
    
    /** {@inheritDoc} */
    @Override public void markLossFuncChanged() {
        setLossFunc(mLossFunc).setLossFuncGrad(mLossFuncGrad);
    }
    /** {@inheritDoc} */
    @Override public void markParameterChanged() {
        setParameter(mParameter);
    }
    
    /**
     * {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setParameter(IVector aParameter) {
        mParameter = aParameter;
        if (aParameter != null) {
            mParameterStep = Vectors.zeros(aParameter.size());
            mGrad = Vectors.zeros(aParameter.size());
        }
        invalidLoss();
        reset();
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aLossFunc {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setLossFunc(ILossFunc aLossFunc) {
        mLossFunc = aLossFunc;
        invalidLoss();
        reset();
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aLossFuncGrad {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setLossFuncGrad(ILossFuncGrad aLossFuncGrad) {
        mLossFuncGrad = aLossFuncGrad;
        invalidLoss();
        reset();
        return this;
    }
    /**
     * 设置需要使用
     * <a href="https://en.wikipedia.org/wiki/Wolfe_conditions">strong Wolfe 线搜索</a>
     * @param aC1 Armijo 线搜索中的参数 c1，默认为 {@code 0.0001}
     * @param aC2 Wolfe 线搜索中的参数 c2，默认为 {@code 0.1}
     * @param aMaxIter 限制的最大迭代次数，默认为 {@code 10}
     * @return 自身方便链式调用
     */
    public AbstractOptimizer setLineSearchStrongWolfe(double aC1, double aC2, int aMaxIter) {
        mC1 = aC1;
        mC2 = aC2;
        mMaxIter = aMaxIter;
        mLineSearch = true;
        return this;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setLineSearch() {
        return setLineSearchStrongWolfe(0.0001, 0.1, 10);
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setNoLineSearch() {
        mLineSearch = false;
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aLogPrinter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setLogPrinter(ILogPrinter aLogPrinter) {
        mLogPrinter = aLogPrinter;
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aBreakChecker {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AbstractOptimizer setBreakChecker(IBreakChecker aBreakChecker) {
        mBreakChecker = aBreakChecker;
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
            calStep(step, mParameter, mParameterStep);
            double tLoss = loss();
            int tLineSearchStep;
            if (mLineSearch) {
                tLineSearchStep = lineSearch(step, tLoss, mParameter, mParameterStep);
            } else {
                applyStep(step);
                tLineSearchStep = 0;
            }
            printLog(step, tLineSearchStep, tLoss, aPrintLog);
            if (checkBreak(step, tLoss, oLoss, mParameterStep)) break;
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
     * 计算参数需要的迭代长度，并将计算结果写入 {@code rParameterStep}。
     * 会借用内部缓存的梯度从而避免重复计算
     * @param aStep 当前的迭代步数
     * @param aParameter 当前的参数值
     * @param rParameterStep 需要写入的迭代步长
     */
    protected abstract void calStep(int aStep, IVector aParameter, Vector rParameterStep);
    /**
     * 应用线搜索，现在统一使用 strong Wolfe 条件的线搜索。
     * 将线搜索结果写入 {@code rParameterStep}，并且自动更新 {@code rParameter}
     * @param aStep 当前的迭代步数
     * @param rParameter 需要实时更新的参数列表
     * @param rParameterStep 需要写入的更新后的迭代步长
     * @return 进行线搜索的步数
     */
    protected int lineSearch(int aStep, double aLoss, IVector rParameter, Vector rParameterStep) {
        IVector tGrad = grad();
        double tGradA0 = tGrad.operation().dot(rParameterStep);
        if (tGradA0 >= 0) throw new IllegalStateException("positive gradient");
        int tStep = 0;
        double tAlpha = 1.0;
        double tAlphaL = 0.0, tAlphaR = Double.NaN;
        double tLossL = aLoss, tLossR = Double.NaN, tGradL = tGradA0;
        // 总是线搜索，开启后会在最开始总执行一次 Armijo 线搜索，从而找到合适的步长
        if (lineSearchAlways()) {
            // 这步由于总是会被抛弃，因此只计算能量
            rParameter.plus2this(rParameterStep);
            double tLoss = eval();
            rParameter.minus2this(rParameterStep);
            invalidLoss();
            // 二次样条拟合
            double tA = (tLoss - aLoss - tGradA0*tAlpha) / (tAlpha*tAlpha);
            // 若拟合得到 tA < 0，则说明此区域不是正定的，这里会直接中断
            if (tA <= 0) return tStep;
            // 获取适合的 tAlpha
            tAlpha = Math.min(-tGradA0 / (2*tA), 2.0*tAlpha);
            ++tStep;
        }
        while (true) {
            // 先简单判断中间分点是否满足 Armijo + strong Wolfe
            rParameter.operation().mplus2this(rParameterStep, tAlpha);
            double tLoss = eval(true);
            double tGradA = tGrad.operation().dot(rParameterStep);
            final boolean tArmijoOK = tLoss <= aLoss + mC1*tGradA0*tAlpha;
            if (tStep>=mMaxIter || (tArmijoOK && Math.abs(tGradA)<=-mC2*tGradA0)) {
                rParameterStep.multiply2this(tAlpha);
                updateLearningRate(tAlpha);
                return tStep;
            }
            // 此时不满足线搜索条件，需要重新设置参数
            rParameter.operation().mplus2this(rParameterStep, -tAlpha);
            // 判断中间点是可以作为左端还是右端
            if (!tArmijoOK || tGradA>0) {
                // 如果中间点不满足 Armijo 则一定为右端点
                // 如果中间点斜率为正则一定为右端点
                tAlphaR = tAlpha; tLossR = tLoss;
                tAlpha = lineSearchChoose(tAlphaL, tAlphaR, tLossL, tLossR, tGradL);
            } else
            if (tGradA < mC2*tGradA0) {
                // 如果中间点斜率过低则总是可以作为左端点
                tAlphaL = tAlpha; tLossL = tLoss; tGradL = tGradA;
                tAlpha = Double.isNaN(tAlphaR) ? tAlphaL*2.0 :
                    lineSearchChoose(tAlphaL, tAlphaR, tLossL, tLossR, tGradL);
            }
            ++tStep;
        }
    }
    protected double lineSearchChoose(double aAlphaL, double aAlphaR, double aLossL, double aLossR, double aGradL) {
        // 采用二次样条插值，例外情况这里简单回退到二分
        double tAlphaGap = aAlphaR - aAlphaL;
        double tA = (aLossR - aLossL - aGradL*tAlphaGap) / (tAlphaGap*tAlphaGap);
        if (tA <= 0) return (aAlphaL+aAlphaR) * 0.5;
        double tAlpha = aAlphaL - aGradL / (2*tA);
        if (tAlpha<aAlphaL || tAlpha>aAlphaR) return (aAlphaL+aAlphaR) * 0.5;
        return tAlpha;
    }
    protected boolean lineSearchAlways() {
        return false;
    }
    protected void updateLearningRate(double aLR) {/**/}
    
    protected final static DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 应用迭代步长，默认直接运算 {@code mParameter.plus2this(mParameterStep)}。
     * 重写来实现自定义的更新策略
     * @param aStep 当前的迭代步数
     */
    protected void applyStep(int aStep) {
        mParameter.plus2this(mParameterStep);
        invalidGrad();
    }
    /**
     * 打印输出，重写实现自定义的打印需求
     * @param aStep 当前的迭代步数
     * @param aLineSearchStep 当前步进行的线搜索步数
     * @param aLoss 当前的 loss 值
     * @param aPrintLog 是否进行打印，作为参数传入确保无论如何一定会调用
     */
    protected void printLog(int aStep, int aLineSearchStep, double aLoss, boolean aPrintLog) {
        if (mLogPrinter != null) {
            mLogPrinter.call(aStep, aLineSearchStep, aLoss, aPrintLog);
            return;
        }
        if (!aPrintLog) return;
        if (aStep == 0) System.out.printf("%12s %12s %18s %12s\n", "step", "time", "loss", "max_step");
        double tMaxStep = 0.0;
        final int tSize = mParameterStep.size();
        for (int i = 0; i < tSize; ++i) {
            double tStep = Math.abs(mParameterStep.get(i));
            if (tStep > tMaxStep) tMaxStep = tStep;
        }
        String tTime = LocalDateTime.now().format(DATE_FMT);
        if (aLineSearchStep > 0) {
            System.out.printf("%12d %12s %18.12g %12.6g  (line search: %d)\n", aStep, tTime, aLoss, tMaxStep, aLineSearchStep);
        } else {
            System.out.printf("%12d %12s %18.12g %12.6g\n", aStep, tTime, aLoss, tMaxStep);
        }
    }
    /**
     * 测试当前优化步是否可以终止，默认为优化到数值精度，即
     * {@link MathEX.Code#DBL_EPSILON}
     * @param aStep 当前的迭代步数
     * @param aLoss 当前的 loss 值
     * @param aLastLoss 上一步的 loss 值，如果是第一步则为 {@link Double#NaN}
     * @param aParameterStep 这次迭代的步长
     * @return 是否进行中断
     */
    protected boolean checkBreak(int aStep, double aLoss, double aLastLoss, Vector aParameterStep) {
        if (mBreakChecker != null) {
            return mBreakChecker.call(aStep, aLoss, aLastLoss, aParameterStep);
        }
        if (aStep==0 || Double.isNaN(aLastLoss)) return false;
        return Math.abs(aLastLoss-aLoss) < Math.abs(aLastLoss)*DBL_EPSILON;
    }
}
