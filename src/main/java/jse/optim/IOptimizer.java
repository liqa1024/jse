package jse.optim;

import jse.math.vector.IVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.ApiStatus;

/**
 * 优化器的通用接口，除了原子结构优化还可以用于其他任意优化。
 * @author liqa
 */
@ApiStatus.Experimental
public interface IOptimizer {
    @FunctionalInterface interface ILossFunc {double call();}
    @FunctionalInterface interface ILossFuncGrad {double call(Vector rGrad);}
    @FunctionalInterface interface ILogPrinter {void call(int aStep, int aLineSearchStep, double aLoss, boolean aPrintLog);}
    @FunctionalInterface interface IBreakChecker {boolean call(int aStep, double aLoss, double aLastLoss, Vector aParameterStep);}
    
    /**
     * 设置需要优化的参数
     * @param aParameter 需要优化的参数引用
     * @return 自身方便链式调用
     */
    IOptimizer setParameter(IVector aParameter);
    
    /**
     * 设置需要优化的目标函数
     * @param aLossFunc 自定义的目标函数，输入可优化参数 {@link IVector}，输出目标值
     * @return 自身方便链式调用
     */
    IOptimizer setLossFunc(ILossFunc aLossFunc);
    
    /**
     * 设置需要优化的目标函数关于参数的梯度
     * @param aLossFuncGrad 自定义的目标函数的梯度值，输入可优化参数以及输出的梯度值应用 {@link IVector}，并顺便输出目标值
     * @return 自身方便链式调用
     */
    IOptimizer setLossFuncGrad(ILossFuncGrad aLossFuncGrad);
    
    /**
     * 设置需要线搜索，这里默认使用
     * <a href="https://en.wikipedia.org/wiki/Wolfe_conditions">strong Wolfe 线搜索</a>
     * @return 自身方便链式调用
     */
    IOptimizer setLineSearch();
    /**
     * 关闭线搜索
     * @return 自身方便链式调用
     */
    IOptimizer setNoLineSearch();
    
    /**
     * 设置优化过程中间变量打印器
     * @param aLogPrinter 自定义的中间变量打印器 {@link ILogPrinter}
     * @return 自身方便链式调用
     */
    IOptimizer setLogPrinter(ILogPrinter aLogPrinter);
    /**
     * 设置优化中断的检测器
     * @param aBreakChecker 自定义的中断的检测器 {@link IBreakChecker}
     * @return 自身方便链式调用
     */
    IOptimizer setBreakChecker(IBreakChecker aBreakChecker);
    
    /**
     * 设置学习率
     * @param aLearningRate 新的学习率
     * @return 自身方便链式调用
     */
    IOptimizer setLearningRate(double aLearningRate);
    
    
    /**
     * 执行优化，并更新输入的参数
     * @param aMaxStep 最多的优化步数
     * @param aPrintLog 是否在优化过程中输出中间信息，默认为 {@code true}
     */
    void run(int aMaxStep, boolean aPrintLog);
    /**
     * 执行优化，并更新输入的参数
     * @param aMaxStep 最多的优化步数
     */
    default void run(int aMaxStep) {run(aMaxStep, true);}
    
    /**
     * 重置优化器状态，仅对于部分优化器有效，
     * 可以使其忘记之前优化过程的信息
     */
    default void reset() {}
    
    /**
     * 标记损失函数状态发生变化；
     * 会清空档期缓存的 loss 值以及梯度，防止算法出现问题。
     */
    void markLossFuncChanged();
    
    /**
     * 标记参数状态发生变化，这里还包括参数长度改变的情况；
     * 会清空所有缓存来避免出现问题
     */
    void markParameterChanged();
}
