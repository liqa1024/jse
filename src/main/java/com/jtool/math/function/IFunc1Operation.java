package com.jtool.math.function;


import com.jtool.code.functional.*;

/**
 * 任意一维数值函数的运算
 * @author liqa
 */
public interface IFunc1Operation {
    /** 通用的运算 */
    IFunc1 plus         (IFunc1Subs aRHS);
    IFunc1 minus        (IFunc1Subs aRHS);
    IFunc1 lminus       (IFunc1Subs aRHS);
    IFunc1 multiply     (IFunc1Subs aRHS);
    IFunc1 div          (IFunc1Subs aRHS);
    IFunc1 ldiv         (IFunc1Subs aRHS);
    IFunc1 mod          (IFunc1Subs aRHS);
    IFunc1 lmod         (IFunc1Subs aRHS);
    IFunc1 operate      (IFunc1Subs aRHS, IDoubleOperator2 aOpt);
    
    IFunc1 plus         (double aRHS);
    IFunc1 minus        (double aRHS);
    IFunc1 lminus       (double aRHS);
    IFunc1 multiply     (double aRHS);
    IFunc1 div          (double aRHS);
    IFunc1 ldiv         (double aRHS);
    IFunc1 mod          (double aRHS);
    IFunc1 lmod         (double aRHS);
    IFunc1 map          (IDoubleOperator1 aOpt);
    
    void plus2this      (IFunc1Subs aRHS);
    void minus2this     (IFunc1Subs aRHS);
    void lminus2this    (IFunc1Subs aRHS);
    void multiply2this  (IFunc1Subs aRHS);
    void div2this       (IFunc1Subs aRHS);
    void ldiv2this      (IFunc1Subs aRHS);
    void mod2this       (IFunc1Subs aRHS);
    void lmod2this      (IFunc1Subs aRHS);
    void operate2this   (IFunc1Subs aRHS, IDoubleOperator2 aOpt);
    
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void lminus2this    (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void ldiv2this      (double aRHS);
    void mod2this       (double aRHS);
    void lmod2this      (double aRHS);
    void map2this       (IDoubleOperator1 aOpt);
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (double aRHS);
    void fill           (IFunc1Subs aRHS);
    void assign         (IDoubleSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach        (IDoubleConsumer1 aCon);
    
    /** 函数特有的运算，最后增加一项 x 的值传入 */
    IFunc1 operateFull      (IFunc1Subs aRHS, IDoubleOperator3 aOpt);
    IFunc1 mapFull          (IDoubleOperator2 aOpt);
    void operateFull2this   (IFunc1Subs aRHS, IDoubleOperator3 aOpt);
    void mapFull2this       (IDoubleOperator2 aOpt);
    
    /** 微分积分运算 */
    IFunc1 laplacian();
    
    /**
     * 卷积运算，通过输入的卷积核来对自身函数进行卷积运算，输出得到的结果
     * <p>
     * 注意这里卷积核输入格式为 (x, k) -> out，自身函数为 f(x)，经过卷积后得到函数 g(k)
     * <p>
     * 执行的卷积运算为：g(k) = int(conv(x, k) * f(x), x);
     */
    IFunc1 convolve(IFunc2Subs aConv);
    IFunc1Subs refConvolve(IFunc2Subs aConv);
    
    /**
     * 完整的卷积运算，通过输入的卷积核来对自身函数进行卷积运算，输出得到的结果
     * <p>
     * 注意这里卷积核输入格式为 (f(x), x, k) -> out，自身函数为 f(x)，经过卷积后得到函数 g(k)
     * <p>
     * 执行的卷积运算为：g(k) = int(conv(f(x), x, k), x);
     */
    IFunc1 convolveFull(IFunc3Subs aConv);
    IFunc1Subs refConvolveFull(IFunc3Subs aConv);
    
    /** 返回峰值所在的 x 坐标 */
    double maxX();
    double minX();
}
