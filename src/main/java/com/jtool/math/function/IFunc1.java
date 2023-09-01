package com.jtool.math.function;

import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;
import com.jtool.math.vector.IVectorSetter;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;

/**
 * @author liqa
 * <p> 通用的数值函数接口，一维输入（f(x)）</p>
 */
public interface IFunc1 extends IFunc1Subs, IVectorGetter, IVectorSetter {
    /** 获取所有数据方便外部使用或者进行运算 */
    IVector x();
    IVector f();
    
    /** 批量修改的接口 */
    void fill(double[] aData);
    default void fill(double aValue) {operation().fill(aValue);}
    default void fill(IFunc1Subs aFunc1Subs) {operation().fill(aFunc1Subs);}
    default void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        assign(() -> it.next().doubleValue());
    }
    default void assign(IDoubleSupplier aSup) {operation().assign(aSup);}
    default void forEach(IDoubleConsumer1 aCon) {operation().forEach(aCon);}
    
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
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    void update_(int aI, IDoubleOperator1 aOpt);
    double getAndUpdate_(int aI, IDoubleOperator1 aOpt);
    void update(int aI, IDoubleOperator1 aOpt);
    double getAndUpdate(int aI, IDoubleOperator1 aOpt);
    
    /** 还提供一个给函数专用的运算 */
    IFunc1Operation operation();
    @VisibleForTesting default IFunc1Operation opt() {return operation();}
    
    
    /** Groovy 的部分，增加向量基本的运算操作，现在也归入内部使用 */
    default IFunc1 plus     (double aRHS) {return operation().plus    (aRHS);}
    default IFunc1 minus    (double aRHS) {return operation().minus   (aRHS);}
    default IFunc1 multiply (double aRHS) {return operation().multiply(aRHS);}
    default IFunc1 div      (double aRHS) {return operation().div     (aRHS);}
    default IFunc1 mod      (double aRHS) {return operation().mod     (aRHS);}
    
    default IFunc1 plus     (IFunc1Subs aRHS) {return operation().plus    (aRHS);}
    default IFunc1 minus    (IFunc1Subs aRHS) {return operation().minus   (aRHS);}
    default IFunc1 multiply (IFunc1Subs aRHS) {return operation().multiply(aRHS);}
    default IFunc1 div      (IFunc1Subs aRHS) {return operation().div     (aRHS);}
    default IFunc1 mod      (IFunc1Subs aRHS) {return operation().mod     (aRHS);}
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    default void plus2this      (double aRHS) {operation().plus2this    (aRHS);}
    default void minus2this     (double aRHS) {operation().minus2this   (aRHS);}
    default void multiply2this  (double aRHS) {operation().multiply2this(aRHS);}
    default void div2this       (double aRHS) {operation().div2this     (aRHS);}
    default void mod2this       (double aRHS) {operation().mod2this     (aRHS);}
    
    default void plus2this      (IFunc1Subs aRHS) {operation().plus2this    (aRHS);}
    default void minus2this     (IFunc1Subs aRHS) {operation().minus2this   (aRHS);}
    default void multiply2this  (IFunc1Subs aRHS) {operation().multiply2this(aRHS);}
    default void div2this       (IFunc1Subs aRHS) {operation().div2this     (aRHS);}
    default void mod2this       (IFunc1Subs aRHS) {operation().mod2this     (aRHS);}
    
    /** Groovy 的部分，重载一些运算符方便操作；圆括号为 x 值查找，方括号为索引查找 */
    @VisibleForTesting default double call(double aX) {return subs(aX);}
    @VisibleForTesting default double getAt(int aI) {return get(aI);}
    @VisibleForTesting default void putAt(int aI, double aV) {set(aI, aV);}
}
