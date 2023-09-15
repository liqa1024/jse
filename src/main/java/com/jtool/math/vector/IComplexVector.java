package com.jtool.math.vector;

import com.jtool.atom.IAtom;
import com.jtool.atom.IXYZ;
import com.jtool.code.CS.SliceType;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.*;
import com.jtool.code.iterator.*;
import com.jtool.math.ComplexDouble;
import com.jtool.math.IComplexDouble;
import groovy.lang.Closure;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * @author liqa
 * <p> 专用的复数向量 </p>
 * <p> 会存在一些不可避免的 lambda 重载，为了内部使用方便这里不去刻意避免，
 * 如遇到 Groovy 脚本使用不方便问题则专门提供一个用于 Groovy 脚本使用的接口 </p>
 * <p> 和 {@link IXYZ} 或者 {@link IAtom} 部分的用法思路不同，
 * 这里直接返回 {@link ComplexDouble} 让其和基本类型一样的使用，不会有引用的属性在里面 </p>
 */
public interface IComplexVector extends IHasComplexDoubleIterator, IHasComplexDoubleSetIterator, IComplexVectorGetter, IComplexVectorSetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    IComplexDoubleIterator iterator();
    IComplexDoubleSetIterator setIterator();
    IComplexDoubleIterator iteratorOf(IComplexVectorGetter aContainer);
    IComplexDoubleSetOnlyIterator setIteratorOf(IComplexVectorSetter aContainer);
    
    List<ComplexDouble> asList();
    
    /** 获取实部和虚部 */
    IVector real();
    IVector imag();
    
    /** 转为兼容性更好的 double[][] */
    double[][] data();
    
    /** 批量修改的接口 */
    void fill(IComplexDouble aValue);
    void fill(double aValue);
    void fillReal(double aReal);
    void fillImag(double aImag);
    void fill(IComplexVectorGetter aVectorGetter);
    void fill(IVectorGetter aVectorGetter);
    void fillReal(IVectorGetter aRealGetter);
    void fillImag(IVectorGetter aImagGetter);
    void fill(double[] aData);
    void fillReal(double[] aRealData);
    void fillImag(double[] aImagData);
    void fill(Iterable<? extends Number> aList);
    void fillReal(Iterable<? extends Number> aRealList);
    void fillImag(Iterable<? extends Number> aImagList);
    void assign(Supplier<? extends IComplexDouble> aSup);
    void assign(IDoubleSupplier aSup);
    void assignReal(IDoubleSupplier aRealSup);
    void assignImag(IDoubleSupplier aImagSup);
    void forEach(IConsumer1<? super ComplexDouble> aCon);
    void forEach(IDoubleConsumer2 aCon);
    void forEachReal(IDoubleConsumer1 aCon);
    void forEachImag(IDoubleConsumer1 aCon);
    /** Groovy stuff */
    void assign(Closure<?> aGroovyTask);
    void forEach(Closure<?> aGroovyTask);
    
    /** 访问和修改部分，自带的接口 */
    ComplexDouble get_(int aIdx);
    double getReal_(int aIdx);
    double getImag_(int aIdx);
    void set_(int aIdx, IComplexDouble aValue);
    void set_(int aIdx, double aValue);
    void setReal_(int aIdx, double aReal);
    void setImag_(int aIdx, double aImag);
    ComplexDouble getAndSet_(int aIdx, IComplexDouble aValue); // 返回修改前的值
    ComplexDouble getAndSet_(int aIdx, double aValue);
    double getAndSetReal_(int aIdx, double aReal);
    double getAndSetImag_(int aIdx, double aImag);
    int size();
    
    ComplexDouble get(int aIdx);
    double getReal(int aIdx);
    double getImag(int aIdx);
    void set(int aIdx, IComplexDouble aValue);
    void set(int aIdx, double aValue);
    void setReal(int aIdx, double aReal);
    void setImag(int aIdx, double aImag);
    ComplexDouble getAndSet(int aIdx, IComplexDouble aValue);
    ComplexDouble getAndSet(int aIdx, double aValue);
    double getAndSetReal(int aIdx, double aReal);
    double getAndSetImag(int aIdx, double aImag);
    
    /** 用于方便访问 */
    default boolean isEmpty() {return size()==0;}
    default ComplexDouble last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty ComplexVector");
        else return get(size()-1);
    }
    default ComplexDouble first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty ComplexVector");
        else return get(0);
    }
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    void update_(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    void updateReal_(int aIdx, IDoubleOperator1 aRealOpt);
    void updateImag_(int aIdx, IDoubleOperator1 aImagOpt);
    ComplexDouble getAndUpdate_(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    double getAndUpdateReal_(int aIdx, IDoubleOperator1 aRealOpt);
    double getAndUpdateImag_(int aIdx, IDoubleOperator1 aImagOpt);
    void update(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    void updateReal(int aIdx, IDoubleOperator1 aRealOpt);
    void updateImag(int aIdx, IDoubleOperator1 aImagOpt);
    ComplexDouble getAndUpdate(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    double getAndUpdateReal(int aIdx, IDoubleOperator1 aRealOpt);
    double getAndUpdateImag(int aIdx, IDoubleOperator1 aImagOpt);
    
    
    /** 现在不再提供生成器，只提供直接创建相同类型的全零的向量的接口，特殊向量的创建请使用 {@link Vectors} */
    default IComplexVector newZeros() {return newZeros(size());}
    IComplexVector newZeros(int aSize);
    
    IComplexVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IComplexVectorSlicer slicer();
    IComplexVectorSlicer refSlicer();

    /** 向量的运算操作，默认返回新的向量 */
    IComplexVectorOperation operation();
    @VisibleForTesting default IComplexVectorOperation opt() {return operation();}
    
    
    /** Groovy 的部分，增加向量基本的运算操作，现在也归入内部使用 */
    IComplexVector plus     (IComplexDouble aRHS);
    IComplexVector minus    (IComplexDouble aRHS);
    IComplexVector multiply (IComplexDouble aRHS);
    IComplexVector div      (IComplexDouble aRHS);
    IComplexVector plus     (double aRHS);
    IComplexVector minus    (double aRHS);
    IComplexVector multiply (double aRHS);
    IComplexVector div      (double aRHS);
    
    IComplexVector plus     (IComplexVectorGetter aRHS);
    IComplexVector minus    (IComplexVectorGetter aRHS);
    IComplexVector multiply (IComplexVectorGetter aRHS);
    IComplexVector div      (IComplexVectorGetter aRHS);
    IComplexVector plus     (IVectorGetter aRHS);
    IComplexVector minus    (IVectorGetter aRHS);
    IComplexVector multiply (IVectorGetter aRHS);
    IComplexVector div      (IVectorGetter aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (IComplexDouble aRHS);
    void minus2this     (IComplexDouble aRHS);
    void multiply2this  (IComplexDouble aRHS);
    void div2this       (IComplexDouble aRHS);
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    
    void plus2this      (IComplexVectorGetter aRHS);
    void minus2this     (IComplexVectorGetter aRHS);
    void multiply2this  (IComplexVectorGetter aRHS);
    void div2this       (IComplexVectorGetter aRHS);
    void plus2this      (IVectorGetter aRHS);
    void minus2this     (IVectorGetter aRHS);
    void multiply2this  (IVectorGetter aRHS);
    void div2this       (IVectorGetter aRHS);
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting ComplexDouble call(int aIdx);
    @VisibleForTesting ComplexDouble getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, IComplexDouble aValue);
    @VisibleForTesting void putAt(int aIdx, double aValue);
    
    @VisibleForTesting IComplexVector call(List<Integer> aIndices);
    @VisibleForTesting IComplexVector call(SliceType     aIndices);
    
    @VisibleForTesting IComplexVector getAt(List<Integer> aIndices);
    @VisibleForTesting IComplexVector getAt(SliceType     aIndices);
    @VisibleForTesting IComplexVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(List<Integer> aIndices, IComplexDouble aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, double aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, IComplexVectorGetter aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, IComplexDouble aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, double aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, IComplexVectorGetter aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IComplexDouble aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, double aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IComplexVectorGetter aVector);
}
