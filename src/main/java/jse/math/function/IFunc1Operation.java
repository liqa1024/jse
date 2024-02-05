package jse.math.function;


import jse.code.functional.IDoubleTernaryOperator;
import jse.math.vector.IVector;
import java.util.function.*;

/**
 * 任意一维数值函数的运算
 * @author liqa
 */
public interface IFunc1Operation {
    /** 通用的运算 */
    IFunc1 plus         (IFunc1 aRHS);
    IFunc1 minus        (IFunc1 aRHS);
    IFunc1 lminus       (IFunc1 aRHS);
    IFunc1 multiply     (IFunc1 aRHS);
    IFunc1 div          (IFunc1 aRHS);
    IFunc1 ldiv         (IFunc1 aRHS);
    IFunc1 mod          (IFunc1 aRHS);
    IFunc1 lmod         (IFunc1 aRHS);
    IFunc1 operate      (IFunc1 aRHS, DoubleBinaryOperator aOpt);
    
    IFunc1 plus         (double aRHS);
    IFunc1 minus        (double aRHS);
    IFunc1 lminus       (double aRHS);
    IFunc1 multiply     (double aRHS);
    IFunc1 div          (double aRHS);
    IFunc1 ldiv         (double aRHS);
    IFunc1 mod          (double aRHS);
    IFunc1 lmod         (double aRHS);
    IFunc1 map          (DoubleUnaryOperator aOpt);
    
    void plus2this      (IFunc1 aRHS);
    void minus2this     (IFunc1 aRHS);
    void lminus2this    (IFunc1 aRHS);
    void multiply2this  (IFunc1 aRHS);
    void div2this       (IFunc1 aRHS);
    void ldiv2this      (IFunc1 aRHS);
    void mod2this       (IFunc1 aRHS);
    void lmod2this      (IFunc1 aRHS);
    void operate2this   (IFunc1 aRHS, DoubleBinaryOperator aOpt);
    
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void lminus2this    (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void ldiv2this      (double aRHS);
    void mod2this       (double aRHS);
    void lmod2this      (double aRHS);
    void map2this       (DoubleUnaryOperator aOpt);
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (double aRHS);
    void fill           (IVector aRHS);
    void fill           (IFunc1 aRHS);
    void fill           (IFunc1Subs aRHS);
    void assign         (DoubleSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach        (DoubleConsumer aCon);
    
    /** 函数特有的运算，最后增加一项 x 的值传入 */
    IFunc1 operateFull(IFunc1 aRHS, IDoubleTernaryOperator aOpt);
    IFunc1 mapFull          (DoubleBinaryOperator aOpt);
    void operateFull2this(IFunc1 aRHS, IDoubleTernaryOperator aOpt);
    void mapFull2this       (DoubleBinaryOperator aOpt);
    
    /** 微分积分运算 */
    IFunc1 laplacian();
    
    /** 积分运算 */
    double integral();
    
    /**
     * 卷积运算，通过输入的卷积核来对自身函数进行卷积运算，输出得到的结果
     * <p>
     * 注意这里卷积核输入格式为 {@code (x, k) -> out}，自身函数为 f(x)，经过卷积后得到函数 g(k)
     * <p>
     * 执行的卷积运算为：{@code g(k) = int(conv(x, k) * f(x), x)};
     */
    IFunc1 convolve(IFunc2Subs aConv);
    IFunc1Subs refConvolve(IFunc2Subs aConv);
    
    /**
     * 完整的卷积运算，通过输入的卷积核来对自身函数进行卷积运算，输出得到的结果
     * <p>
     * 注意这里卷积核输入格式为 {@code (f(x), x, k) -> out}，自身函数为 f(x)，经过卷积后得到函数 g(k)
     * <p>
     * 执行的卷积运算为：{@code g(k) = int(conv(f(x), x, k), x)};
     */
    IFunc1 convolveFull(IFunc3Subs aConv);
    IFunc1Subs refConvolveFull(IFunc3Subs aConv);
    
    /** 返回峰值所在的 x 坐标 */
    double maxX();
    double minX();
}
