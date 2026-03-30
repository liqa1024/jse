package jse.math.vector;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import jep.NDArray;
import jse.code.collection.ISlice;
import jse.code.functional.*;
import jse.code.iterator.*;
import jse.math.SliceType;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 单精度浮点向量，返回类型 {@code float}
 * @author liqa
 */
public interface IFloatVector extends ISwapper, IHasFloatIterator, IHasFloatSetIterator, IFloatVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    @Override IFloatIterator iterator();
    @Override IFloatSetIterator setIterator();
    
    @Override default Iterable<Float> iterable() {return () -> iterator().toIterator();}
    List<Float> asList();
    IVector asVec();
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    NDArray<float[]> numpy();
    /** 转为兼容性更好的 {@code float[]} */
    float[] data();
    
    /** ISwapper stuffs */
    @Override void swap(int aIdx1, int aIdx2);
    
    /** 批量修改的接口 */
    void fill(float aValue);
    void fill(IFloatVector aVector);
    void fill(IFloatVectorGetter aVectorGetter);
    void fill(float[] aData);
    void fill(Iterable<? extends Number> aList);
    @Override void assign(IFloatSupplier aSup);
    @Override void forEach(IFloatConsumer aCon);
    /** Groovy stuff */
    default void fill(@ClosureParams(value=SimpleType.class, options="int") final Closure<? extends Number> aGroovyTask) {fill(i -> aGroovyTask.call(i).floatValue());}
    default void assign(final Closure<? extends Number> aGroovyTask) {assign(() -> aGroovyTask.call().floatValue());}
    
    /** 访问和修改部分，自带的接口 */
    int size();
    @Override float get(int aIdx);
    float getAndSet(int aIdx, float aValue); // 返回修改前的值
    void set(int aIdx, float aValue);
    
    /** 用于方便访问 */
    default boolean isEmpty() {return size()==0;}
    default float last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty FloatVector");
        return get(size()-1);
    }
    default float first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty FloatVector");
        return get(0);
    }
    
    /** 附加一些额外的单元素操作 */
    void update(int aIdx, IFloatUnaryOperator aOpt);
    float getAndUpdate(int aIdx, IFloatUnaryOperator aOpt);
    
    IFloatVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IFloatVectorSlicer slicer();
    IFloatVectorSlicer refSlicer();
    IFloatVector subVec(int aFromIdx, int aToIdx);
    
    /** 向量的运算操作，默认返回新的向量 */
    IFloatVectorOperation operation();
    @VisibleForTesting default IFloatVectorOperation op() {return operation();}
    
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting float call(int aIdx);
    @VisibleForTesting float getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, float aValue);
    
    @VisibleForTesting IFloatVector call(ISlice        aIndices);
    @VisibleForTesting IFloatVector call(List<Integer> aIndices);
    @VisibleForTesting IFloatVector call(SliceType     aIndices);
    @VisibleForTesting IFloatVector call(IIndexFilter  aIndices);
    
    @VisibleForTesting IFloatVector getAt(ISlice        aIndices);
    @VisibleForTesting IFloatVector getAt(List<Integer> aIndices);
    @VisibleForTesting IFloatVector getAt(SliceType     aIndices);
    @VisibleForTesting IFloatVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(ISlice        aIndices, float aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(ISlice        aIndices, IFloatVector aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, float aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IFloatVector aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, float aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IFloatVector aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, float aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IFloatVector aVector);
}
