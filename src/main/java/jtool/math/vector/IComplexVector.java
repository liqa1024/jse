package jtool.math.vector;

import groovy.lang.Closure;
import jtool.atom.IAtom;
import jtool.atom.IXYZ;
import jtool.code.CS.SliceType;
import jtool.code.collection.ISlice;
import jtool.code.functional.IDoubleBinaryConsumer;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.ISwapper;
import jtool.code.functional.IUnaryFullOperator;
import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetIterator;
import jtool.code.iterator.IHasComplexDoubleIterator;
import jtool.code.iterator.IHasComplexDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.*;

/**
 * @author liqa
 * <p> 专用的复数向量 </p>
 * <p> 会存在一些不可避免的 lambda 重载，为了内部使用方便这里不去刻意避免，
 * 如遇到 Groovy 脚本使用不方便问题则专门提供一个用于 Groovy 脚本使用的接口 </p>
 * <p> 和 {@link IXYZ} 或者 {@link IAtom} 部分的用法思路不同，
 * 这里直接返回 {@link ComplexDouble} 让其和基本类型一样的使用，不会有引用的属性在里面 </p>
 */
public interface IComplexVector extends ISwapper, IHasComplexDoubleIterator, IHasComplexDoubleSetIterator, IComplexVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    IComplexDoubleIterator iterator();
    IComplexDoubleSetIterator setIterator();
    
    default Iterable<ComplexDouble> iterable() {return () -> iterator().toIterator();}
    List<ComplexDouble> asList();
    
    /** 获取实部和虚部 */
    IVector real();
    IVector imag();
    
    /** 转为兼容性更好的 double[][] */
    double[][] data();
    
    /** ISwapper stuffs */
    void swap(int aIdx1, int aIdx2);
    
    /** 批量修改的接口 */
    void fill(IComplexDouble aValue);
    void fill(double aValue);
    void fill(IComplexVector aVector);
    void fill(IVector aVector);
    void fill(IComplexVectorGetter aVectorGetter);
    void fill(IVectorGetter aVectorGetter);
    void fill(double[][] aData);
    void fill(double[] aData);
    void fill(Iterable<?> aList);
    void assign(Supplier<? extends IComplexDouble> aSup);
    void assign(DoubleSupplier aSup);
    void forEach(Consumer<? super ComplexDouble> aCon);
    void forEach(IDoubleBinaryConsumer aCon);
    /** Groovy stuff */
    void fill(Closure<?> aGroovyTask);
    void assign(Closure<?> aGroovyTask);
    void forEach(Closure<?> aGroovyTask);
    
    /** 访问和修改部分，自带的接口 */
    int size();
    ComplexDouble get(int aIdx);
    double getReal(int aIdx);
    double getImag(int aIdx);
    void set(int aIdx, IComplexDouble aValue);
    void set(int aIdx, ComplexDouble aValue);
    void set(int aIdx, double aValue);
    void set(int aIdx, double aReal, double aImag);
    void setReal(int aIdx, double aReal);
    void setImag(int aIdx, double aImag);
    ComplexDouble getAndSet(int aIdx, IComplexDouble aValue); // 返回修改前的值
    ComplexDouble getAndSet(int aIdx, ComplexDouble aValue);
    ComplexDouble getAndSet(int aIdx, double aValue);
    ComplexDouble getAndSet(int aIdx, double aReal, double aImag);
    double getAndSetReal(int aIdx, double aReal);
    double getAndSetImag(int aIdx, double aImag);
    
    /** 用于方便访问 */
    default boolean isEmpty() {return size()==0;}
    default ComplexDouble last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty ComplexVector");
        return get(size()-1);
    }
    default ComplexDouble first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty ComplexVector");
        return get(0);
    }
    
    /** 附加一些额外的单元素操作，对于 ComplexVector 也提供略多的接口 */
    void add(int aIdx, IComplexDouble aDelta);
    void add(int aIdx, ComplexDouble aDelta);
    void add(int aIdx, double aDelta);
    void addImag(int aIdx, double aImag);
    void update(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    void updateReal(int aIdx, DoubleUnaryOperator aRealOpt);
    void updateImag(int aIdx, DoubleUnaryOperator aImagOpt);
    ComplexDouble getAndUpdate(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    double getAndUpdateReal(int aIdx, DoubleUnaryOperator aRealOpt);
    double getAndUpdateImag(int aIdx, DoubleUnaryOperator aImagOpt);
    
    
    IComplexVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IComplexVectorSlicer slicer();
    IComplexVectorSlicer refSlicer();
    IComplexVector subVec(int aFromIdx, int aToIdx);

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
    
    IComplexVector plus     (IComplexVector aRHS);
    IComplexVector minus    (IComplexVector aRHS);
    IComplexVector multiply (IComplexVector aRHS);
    IComplexVector div      (IComplexVector aRHS);
    IComplexVector plus     (IVector aRHS);
    IComplexVector minus    (IVector aRHS);
    IComplexVector multiply (IVector aRHS);
    IComplexVector div      (IVector aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (IComplexDouble aRHS);
    void minus2this     (IComplexDouble aRHS);
    void multiply2this  (IComplexDouble aRHS);
    void div2this       (IComplexDouble aRHS);
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    
    void plus2this      (IComplexVector aRHS);
    void minus2this     (IComplexVector aRHS);
    void multiply2this  (IComplexVector aRHS);
    void div2this       (IComplexVector aRHS);
    void plus2this      (IVector aRHS);
    void minus2this     (IVector aRHS);
    void multiply2this  (IVector aRHS);
    void div2this       (IVector aRHS);
    
    IComplexVector negative();
    void negative2this();
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting ComplexDouble call(int aIdx);
    @VisibleForTesting ComplexDouble getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, IComplexDouble aValue);
    @VisibleForTesting void putAt(int aIdx, ComplexDouble aValue);
    @VisibleForTesting void putAt(int aIdx, double aValue);
    
    @VisibleForTesting IComplexVector call(ISlice        aIndices);
    @VisibleForTesting IComplexVector call(List<Integer> aIndices);
    @VisibleForTesting IComplexVector call(SliceType     aIndices);
    @VisibleForTesting IComplexVector call(IIndexFilter  aIndices);
    
    @VisibleForTesting IComplexVector getAt(ISlice        aIndices);
    @VisibleForTesting IComplexVector getAt(List<Integer> aIndices);
    @VisibleForTesting IComplexVector getAt(SliceType     aIndices);
    @VisibleForTesting IComplexVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(ISlice        aIndices, IComplexDouble aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, ComplexDouble aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, double aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(ISlice        aIndices, IVector aVector);
    @VisibleForTesting void putAt(ISlice        aIndices, IComplexVector aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, IComplexDouble aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, ComplexDouble aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, double aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IVector aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, IComplexVector aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, IComplexDouble aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, ComplexDouble aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, double aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IVector aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, IComplexVector aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IComplexDouble aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, ComplexDouble aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, double aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IVector aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IComplexVector aVector);
}
