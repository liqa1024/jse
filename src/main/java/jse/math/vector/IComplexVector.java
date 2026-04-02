package jse.math.vector;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import groovy.transform.stc.SimpleType;
import jse.math.SliceType;
import jse.code.collection.ISlice;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.functional.IIndexFilter;
import jse.code.functional.ISwapper;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IComplexDoubleSetIterator;
import jse.code.iterator.IHasComplexDoubleIterator;
import jse.code.iterator.IHasComplexDoubleSetIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.*;

/**
 * 复数向量，返回类型 {@link ComplexDouble}
 * @author liqa
 */
public interface IComplexVector extends ISwapper, IHasComplexDoubleIterator, IHasComplexDoubleSetIterator, IComplexVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    @Override IComplexDoubleIterator iterator();
    @Override IComplexDoubleSetIterator setIterator();
    
    @Override default Iterable<ComplexDouble> iterable() {return () -> iterator().toIterator();}
    List<ComplexDouble> asList();
    
    /** 获取实部和虚部 */
    IVector real();
    IVector imag();
    
    /** 转为兼容性更好的 double[][] */
    double[][] data();
    
    /** ISwapper stuffs */
    @Override void swap(int aIdx1, int aIdx2);
    
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
    @Override void assign(Supplier<? extends IComplexDouble> aSup);
    @Override void assign(DoubleSupplier aSup);
    @Override void forEach(Consumer<? super ComplexDouble> aCon);
    @Override void forEach(IDoubleBinaryConsumer aCon);
    /** Groovy stuff */
    void fill(@ClosureParams(value=SimpleType.class, options="int") Closure<?> aGroovyTask);
    @Override void assign(Closure<?> aGroovyTask);
    @Override void forEach(@ClosureParams(value=FromString.class, options={"ComplexDouble", "double,double"}) Closure<?> aGroovyTask);
    
    /** 访问和修改部分，自带的接口 */
    int size();
    @Override ComplexDouble get(int aIdx);
    @Override double getReal(int aIdx);
    @Override double getImag(int aIdx);
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
    @VisibleForTesting default IComplexVectorOperation op() {return operation();}
    
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
    
    /** 对于 ComplexVector，现在也将这些统计接口也直接放在这里方便使用 */
    ComplexDouble sum ();
    ComplexDouble mean();
    
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
