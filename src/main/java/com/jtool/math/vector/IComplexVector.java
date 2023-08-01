package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.iterator.IComplexDoubleIterator;
import com.jtool.code.iterator.IComplexDoubleSetIterator;
import com.jtool.code.iterator.IComplexDoubleSetOnlyIterator;
import com.jtool.code.operator.IComplexDoubleOperator1;
import com.jtool.math.ComplexDouble;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 专用的复数向量 </p>
 * <p> 会存在一些不可避免的 lambda 重载，为了内部使用方便这里不去刻意避免，
 * 如遇到 Groovy 脚本使用不方便问题则专门提供一个用于 Groovy 脚本使用的接口 </p>
 */
public interface IComplexVector extends IComplexVectorGetter, IComplexVectorSetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    IComplexDoubleIterator iterator();
    IComplexDoubleSetIterator setIterator();
    IComplexDoubleIterator iteratorOf(IComplexVectorGetter aContainer);
    IComplexDoubleSetOnlyIterator setIteratorOf(IComplexVectorSetter aContainer);
    
    default Iterable<ComplexDouble> iterable() {return this::iterator;}
    List<ComplexDouble> asList();
    
    /** 获取实部和虚部 */
    IVector real();
    IVector imag();
    
    /** 转为兼容性更好的 double[] */
    double[][] data();
    
    /** 批量修改的接口 */
    void fill(ComplexDouble aValue);
    void fill(double aReal);
    void fill(double aReal, double aImag);
    void fill(IComplexVectorGetter aVectorGetter);
    void fill(IVectorGetter aVectorGetter);
    void fill(IVectorGetter aRealGetter, IVectorGetter aImagGetter);
    void fill(double[] aData);
    void fill(double[] aRealData, double[] aImagData);
    void fill(Iterable<? extends Number> aList);
    void fill(Iterable<? extends Number> aRealList, Iterable<? extends Number> aImagList);
    
    /** 访问和修改部分，自带的接口 */
    ComplexDouble get_(int aIdx);
    double getReal_(int aIdx);
    double getImag_(int aIdx);
    void set_(int aIdx, ComplexDouble aValue);
    void set_(int aIdx, double aReal, double aImag);
    ComplexDouble getAndSet_(int aIdx, ComplexDouble aValue); // 返回修改前的值
    ComplexDouble getAndSet_(int aIdx, double aReal, double aImag);
    int size();
    
    ComplexDouble get(int aIdx);
    double getReal(int aIdx);
    double getImag(int aIdx);
    void set(int aIdx, ComplexDouble aValue);
    void set(int aIdx, double aValue);
    void set(int aIdx, double aReal, double aImag);
    ComplexDouble getAndSet(int aIdx, ComplexDouble aValue);
    ComplexDouble getAndSet(int aIdx, double aValue);
    ComplexDouble getAndSet(int aIdx, double aReal, double aImag);
    
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
    void update_(int aIdx, IComplexDoubleOperator1 aOpt);
    double getAndUpdate_(int aIdx, IComplexDoubleOperator1 aOpt);
    void update(int aIdx, IComplexDoubleOperator1 aOpt);
    double getAndUpdate(int aIdx, IComplexDoubleOperator1 aOpt);
    
    
    /** 现在不再提供生成器，只提供直接创建相同类型的全零的向量的接口，特殊向量的创建请使用 {@link Vectors} */
    default IComplexVector newZeros() {return newZeros(size());}
    IComplexVector newZeros(int aSize);
    
    IComplexVector copy();
    
//    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
//    IVectorSlicer slicer();
//    IVectorSlicer refSlicer();
//
//    /** 向量的运算操作，默认返回新的向量 */
//    IVectorOperation operation();
//    @VisibleForTesting default IVectorOperation opt() {return operation();}
    
    
    /** Groovy 的部分，增加向量基本的运算操作，现在也归入内部使用 */
    IComplexVector plus     (ComplexDouble aRHS);
    IComplexVector minus    (ComplexDouble aRHS);
    IComplexVector multiply (ComplexDouble aRHS);
    IComplexVector div      (ComplexDouble aRHS);
    IComplexVector mod      (ComplexDouble aRHS);
    IComplexVector plus     (double aRHS);
    IComplexVector minus    (double aRHS);
    IComplexVector multiply (double aRHS);
    IComplexVector div      (double aRHS);
    IComplexVector mod      (double aRHS);
    
    IComplexVector plus     (IComplexVectorGetter aRHS);
    IComplexVector minus    (IComplexVectorGetter aRHS);
    IComplexVector multiply (IComplexVectorGetter aRHS);
    IComplexVector div      (IComplexVectorGetter aRHS);
    IComplexVector mod      (IComplexVectorGetter aRHS);
    IComplexVector plus     (IVectorGetter aRHS);
    IComplexVector minus    (IVectorGetter aRHS);
    IComplexVector multiply (IVectorGetter aRHS);
    IComplexVector div      (IVectorGetter aRHS);
    IComplexVector mod      (IVectorGetter aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (ComplexDouble aRHS);
    void minus2this     (ComplexDouble aRHS);
    void multiply2this  (ComplexDouble aRHS);
    void div2this       (ComplexDouble aRHS);
    void mod2this       (ComplexDouble aRHS);
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void mod2this       (double aRHS);
    
    void plus2this      (IComplexVectorGetter aRHS);
    void minus2this     (IComplexVectorGetter aRHS);
    void multiply2this  (IComplexVectorGetter aRHS);
    void div2this       (IComplexVectorGetter aRHS);
    void mod2this       (IComplexVectorGetter aRHS);
    void plus2this      (IVectorGetter aRHS);
    void minus2this     (IVectorGetter aRHS);
    void multiply2this  (IVectorGetter aRHS);
    void div2this       (IVectorGetter aRHS);
    void mod2this       (IVectorGetter aRHS);
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting ComplexDouble call(int aIdx);
    @VisibleForTesting ComplexDouble getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, ComplexDouble aValue);
    @VisibleForTesting void putAt(int aIdx, double aValue);
    
    @VisibleForTesting IComplexVector call(List<Integer> aIndices);
    @VisibleForTesting IComplexVector call(SliceType     aIndices);
    
    @VisibleForTesting IComplexVector getAt(List<Integer> aIndices);
    @VisibleForTesting IComplexVector getAt(SliceType     aIndices);
    @VisibleForTesting IComplexVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(List<Integer> aIndices, ComplexDouble aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, double aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, IComplexVectorGetter aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, ComplexDouble aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, double aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, IComplexVectorGetter aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, ComplexDouble aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, double aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IComplexVectorGetter aVector);
}
