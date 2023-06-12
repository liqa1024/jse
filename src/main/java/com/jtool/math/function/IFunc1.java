package com.jtool.math.function;

import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;
import com.jtool.math.vector.IVectorSetter;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * @author liqa
 * <p> 通用的数值函数接口，一维输入（f(x)）</p>
 */
public interface IFunc1 extends IFunc1Subs, IVectorGetter, IVectorSetter {
    /** 获取所有数据方便外部使用或者进行运算 */
    IVector x();
    IVector f();
    
    /** 批量修改的接口 */
    void fill(double aValue);
    void fill(IFunc1Subs aFunc1Subs);
    void fill(double[] aData);
    void fill(Iterable<? extends Number> aList);
    
    /** 拷贝的接口 */
    IFunc1 copy();
    
    /** 获取结果，支持按照索引查找和按照 x 的值来查找 */
    double subs(double aX);
    double get(int aI);
    /** 设置结果，简单起见只允许按照索引来设置 */
    void set(int aI, double aV);
    
    /** 不进行边界检测的版本，带入 x 的情况永远不会超过边界（周期边界或者固定值），因此只提供索引的情况 */
    double get_(int aI);
    void set_(int aI, double aV);
    
    /** 索引和 x 相互转换的接口 */
    int Nx();
    double x0();
    double dx();
    double getX(int aI);
    void setX0(double aNewX0);
    
    /** 还提供一个给函数专用的运算 */
    IFunc1Operation operation();
    @VisibleForTesting default IFunc1Operation opt() {return operation();}
    
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    default void plus2this      (double aRHS)     {operation().mapPlus2this     (aRHS);}
    default void plus2this      (IFunc1Subs aRHS) {operation().ebePlus2this     (aRHS);}
    
    /** Groovy 的部分，重载一些运算符方便操作；圆括号为 x 值查找，方括号为索引查找 */
    @VisibleForTesting default double call(double aX) {return subs(aX);}
    @VisibleForTesting default double getAt(int aI) {return get(aI);}
    @VisibleForTesting default void putAt(int aI, double aV) {set(aI, aV);}
}
