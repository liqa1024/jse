package jtool.math.vector;

import jtool.code.CS.SliceType;
import jtool.code.collection.ISlice;
import jtool.code.collection.NewCollections;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.*;
import jtool.code.iterator.*;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 专用的逻辑值向量 </p>
 */
public interface ILogicalVector extends IHasBooleanIterator, IHasBooleanSetIterator, ILogicalVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    IBooleanIterator iterator();
    IBooleanSetIterator setIterator();
    
    default Iterable<Boolean> iterable() {return () -> iterator().toIterator();}
    List<Boolean> asList();
    IVector asVec();
    
    /** 转为兼容性更好的 boolean[] */
    boolean[] data();
    
    /** 批量修改的接口 */
    void fill(boolean aValue);
    void fill(ILogicalVector aVector);
    void fill(ILogicalVectorGetter aVectorGetter);
    void fill(boolean[] aData);
    void fill(Iterable<Boolean> aList);
    void assign(IBooleanSupplier aSup);
    void forEach(IBooleanConsumer1 aCon);
    
    /** 访问和修改部分，自带的接口 */
    boolean get_(int aIdx);
    void set_(int aIdx, boolean aValue);
    boolean getAndSet_(int aIdx, boolean aValue); // 返回修改前的值
    int size();
    
    boolean get(int aIdx);
    boolean getAndSet(int aIdx, boolean aValue);
    void set(int aIdx, boolean aValue);
    
    /** 用于方便访问 */
    default boolean isEmpty() {return size()==0;}
    default boolean last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LogicalVector");
        return get(size()-1);
    }
    default boolean first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LogicalVector");
        return get(0);
    }
    
    /** 附加一些额外的单元素操作，对于逻辑向量再增加一个翻转特定元素的操作 */
    void flip_(int aIdx);
    boolean getAndFlip_(int aIdx);
    void update_(int aIdx, IBooleanOperator1 aOpt);
    boolean getAndUpdate_(int aIdx, IBooleanOperator1 aOpt);
    
    void flip(int aIdx);
    boolean getAndFlip(int aIdx);
    void update(int aIdx, IBooleanOperator1 aOpt);
    boolean getAndUpdate(int aIdx, IBooleanOperator1 aOpt);
    
    
    ILogicalVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    ILogicalVectorSlicer slicer();
    ILogicalVectorSlicer refSlicer();
    ILogicalVector subVec(int aFromIdx, int aToIdx);
    
    /** 向量的运算操作，默认返回新的向量 */
    ILogicalVectorOperation operation();
    @VisibleForTesting default ILogicalVectorOperation opt() {return operation();}
    
    /** 提供一个调用过滤的方法简化使用 */
    default IIntegerVector where() {return NewCollections.filterInteger(size(), this);}
    
    /** Groovy 的部分，增加向量基本的运算操作，现在也归入内部使用 */
    ILogicalVector and  (boolean aRHS);
    ILogicalVector or   (boolean aRHS);
    ILogicalVector xor  (boolean aRHS);
    
    ILogicalVector and  (ILogicalVector aRHS);
    ILogicalVector or   (ILogicalVector aRHS);
    ILogicalVector xor  (ILogicalVector aRHS);
    ILogicalVector not  ();
    @VisibleForTesting default ILogicalVector bitwiseNegate() {return not();}
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 |= 之类的运算符 */
    void and2this   (boolean aRHS);
    void or2this    (boolean aRHS);
    void xor2this   (boolean aRHS);
    
    void and2this   (ILogicalVector aRHS);
    void or2this    (ILogicalVector aRHS);
    void xor2this   (ILogicalVector aRHS);
    void not2this   ();
    
    /** 对于 LogicalVector 将这些统计接口也直接放在这里方便使用 */
    boolean all  ();
    boolean any  ();
    int     count();
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting boolean call(int aIdx);
    @VisibleForTesting boolean getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, boolean aValue);
    
    @VisibleForTesting ILogicalVector call(ISlice        aIndices);
    @VisibleForTesting ILogicalVector call(List<Integer> aIndices);
    @VisibleForTesting ILogicalVector call(SliceType     aIndices);
    @VisibleForTesting ILogicalVector call(IIndexFilter  aIndices);
    
    @VisibleForTesting ILogicalVector getAt(ISlice        aIndices);
    @VisibleForTesting ILogicalVector getAt(List<Integer> aIndices);
    @VisibleForTesting ILogicalVector getAt(SliceType     aIndices);
    @VisibleForTesting ILogicalVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(ISlice        aIndices, boolean aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, Iterable<Boolean> aList);
    @VisibleForTesting void putAt(ISlice        aIndices, ILogicalVector aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, boolean aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<Boolean> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, ILogicalVector aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, boolean aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<Boolean> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, ILogicalVector aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, boolean aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<Boolean> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, ILogicalVector aVector);
}
