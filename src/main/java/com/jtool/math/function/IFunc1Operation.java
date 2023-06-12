package com.jtool.math.function;


import com.jtool.math.vector.IVectorSetter;

/**
 * 任意一维数值函数的运算
 * @author liqa
 */
public interface IFunc1Operation {
    /** 通用的运算 */
    void mapPlus2this(double aRHS);
    void ebePlus2this(IFunc1Subs aRHS);
    
    void mapFill2this(double aRHS);
    void ebeFill2this(IFunc1Subs aRHS);
    
    
    /** 函数特有的运算 */
    IFunc1Subs laplacian();
    void laplacian2Dest(IVectorSetter rDest);
    
    /**
     * 卷积运算，通过输入的卷积核来对自身函数进行卷积运算，输出得到的结果
     * <p>
     * 注意这里卷积核输入格式为 (x, k) -> out，自身函数为 f(x)，经过卷积后得到函数 g(k)
     * <p>
     * 执行的卷积运算为：g(k) = int(conv(x, k) * f(x), x);
     */
    IFunc1Subs convolve(IFunc2Subs aConv);
    IFunc1Subs refConvolve(IFunc2Subs aConv);
    
    /**
     * 完整的卷积运算，通过输入的卷积核来对自身函数进行卷积运算，输出得到的结果
     * <p>
     * 注意这里卷积核输入格式为 (f(x), x, k) -> out，自身函数为 f(x)，经过卷积后得到函数 g(k)
     * <p>
     * 执行的卷积运算为：g(k) = int(conv(f(x), x, k), x);
     */
    IFunc1Subs convolveFull(IFunc3Subs aConv);
    IFunc1Subs refConvolveFull(IFunc3Subs aConv);
    
    /** 返回峰值所在的 x 坐标 */
    double maxX();
    double minX();
}
