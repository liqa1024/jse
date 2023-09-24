package com.jtool.code.collection;

import com.jtool.code.filter.IDoubleFilter;
import com.jtool.code.filter.IFilter;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IOperator1;
import com.jtool.code.iterator.IHasDoubleIterator;
import com.jtool.math.MathEX;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vector;

import java.util.*;

/**
 * 获取固定容器的类，这里获取的结果统一都进行了一次值拷贝
 * <p>
 * 为了区分 java 自带的 {@link Collections}，并且 Collections 意思比较抽象，这里起名 NewCollections；
 * 既表明这是创建新的实例，也表明这是新的工具类。
 * <p>
 * 注意一般的创建实例的工具类不会有 New 开头
 * @author liqa
 */
@SuppressWarnings({"UseBulkOperation", "ManualArrayToCollectionCopy"})
public class NewCollections {
    protected NewCollections() {}
    
    /** 为了统一格式同样提供一个 zl 方法，但是目前不强制要求使用（目前依旧倾向于直接用 new 创建而不是外套一个专门的静态函数） */
    public static <T> List<T> zl() {return new ArrayList<>();}
    
    /**
     * 提供一些常用的 List 初始化
     * <p>
     * 同样这里可能会存在一些 lambda 表达式的重载，
     * 使用 Groovy 调用遇到问题时再考虑提供专门的方法即可
     * @author liqa
     */
    public static <T> List<T> nulls(int aSize) {
        List<T> rOut = new ArrayList<>(aSize);
        for (int i = 0; i < aSize; ++i) rOut.add(null);
        return rOut;
    }
    public static <T> List<T> from(int aSize, IOperator1<? extends T, Integer> aListGetter) {
        List<T> rOut = new ArrayList<>(aSize);
        for (int i = 0; i < aSize; ++i) rOut.add(aListGetter.cal(i));
        return rOut;
    }
    public static <T> List<T> from(Iterable<? extends T> aIterable) {
        List<T> rOut = new ArrayList<>();
        for (T tValue : aIterable) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> from(Collection<? extends T> aList) {
        List<T> rOut = new ArrayList<>(aList.size());
        for (T tValue : aList) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> from(T[] aData) {
        List<T> rOut = new ArrayList<>(aData.length);
        for (T tValue : aData) rOut.add(tValue);
        return rOut;
    }
    public static List<Double> from(double[] aData) {
        List<Double> rOut = new ArrayList<>(aData.length);
        for (double tValue : aData) rOut.add(tValue);
        return rOut;
    }
    public static List<Integer> from(int[] aData) {
        List<Integer> rOut = new ArrayList<>(aData.length);
        for (int tValue : aData) rOut.add(tValue);
        return rOut;
    }
    public static List<Boolean> from(boolean[] aData) {
        List<Boolean> rOut = new ArrayList<>(aData.length);
        for (boolean tValue : aData) rOut.add(tValue);
        return rOut;
    }
    
    
    /**
     * the range function similar to python
     * <p> only support in {@code aStep > 0} for now </p>
     * @author liqa
     * @param aStart the start value, include
     * @param aStop the stop position, exclude
     * @param aStep the step of Iteration
     * @return A iterable container
     */
    public static List<Integer> range_(int aStart, int aStop, int aStep) {
       List<Integer> rRange = new ArrayList<>(MathEX.Code.divup(aStop-aStart, aStep));
       for (int i = aStart; i < aStop; i += aStep) rRange.add(i);
       return rRange;
    }
    public static List<Integer> range_(            int aSize           ) {return range_(0, aSize);}
    public static List<Integer> range_(int aStart, int aStop           ) {return range_(aStart, aStop, 1);}
    public static List<Integer> range (            int aSize           ) {return range(0, aSize);}
    public static List<Integer> range (int aStart, int aStop           ) {return range(aStart, aStop, 1);}
    public static List<Integer> range (int aStart, int aStop, int aStep) {
        aStep = Math.max(aStep, 1);
        aStop = Math.max(aStop, aStart);
        return range_(aStart, aStop, aStep);
    }
    
    /**
     * map {@code Iterable<T> to List<R>}
     * @author liqa
     */
    public static <R, T> List<R> map(final Iterable<T> aIterable, final IOperator1<? extends R, ? super T> aOpt) {
        List<R> rOut = new ArrayList<>();
        for (T tValue : aIterable) rOut.add(aOpt.cal(tValue));
        return rOut;
    }
    public static <R, T> List<R> map(final Collection<T> aCollection, final IOperator1<? extends R, ? super T> aOpt) {
        List<R> rOut = new ArrayList<>(aCollection.size());
        for (T tValue : aCollection) rOut.add(aOpt.cal(tValue));
        return rOut;
    }
    public static <R, T> List<R> map(final T[] aArray, final IOperator1<? extends R, ? super T> aOpt) {
        List<R> rOut = new ArrayList<>(aArray.length);
        for (T tValue : aArray) rOut.add(aOpt.cal(tValue));
        return rOut;
    }
    
    
    /**
     * 提供通用切片接口
     * @author liqa
     */
    public static <T> List<T> slice(List<? extends T> aList, Iterable<Integer> aIndices) {
        List<T> rList = new ArrayList<>();
        for (int tIdx : aIndices) rList.add(aList.get(tIdx));
        return rList;
    }
    public static <T> List<T> slice(List<? extends T> aList, Collection<Integer> aIndices) {
        List<T> rList = new ArrayList<>(aIndices.size());
        for (int tIdx : aIndices) rList.add(aList.get(tIdx));
        return rList;
    }
    public static <T> List<T> slice(List<? extends T> aList, int[] aIndices) {
        List<T> rList = new ArrayList<>(aIndices.length);
        for (int tIdx : aIndices) rList.add(aList.get(tIdx));
        return rList;
    }
    public static <T> List<T> slice(Iterable<? extends T> aIterable, IIndexFilter aIndices) {
        List<T> rList = new ArrayList<>();
        int tIdx = 0;
        for (T tValue : aIterable) {
            if (aIndices.accept(tIdx)) rList.add(tValue);
            ++tIdx;
        }
        return rList;
    }
    
    
    /**
     * 提供通用的执行过滤的接口
     * @author liqa
     */
    public static <T> List<T> filter(Iterable<? extends T> aIterable, IFilter<? super T> aFilter) {
        List<T> rList = new ArrayList<>();
        for (T tValue : aIterable) if (aFilter.accept(tValue)) rList.add(tValue);
        return rList;
    }
    public static List<Integer> filterInteger(Iterable<Integer> aIndices, final IIndexFilter aFilter) {
        return filter(aIndices, aFilter::accept);
    }
    public static List<Integer> filterInteger(int aSize, IIndexFilter aFilter) {
        List<Integer> rIndices = new ArrayList<>();
        for (int i = 0; i < aSize; ++i) if (aFilter.accept(i)) rIndices.add(i);
        return rIndices;
    }
    public static IVector filterDouble(Iterable<? extends Number> aIterable, IDoubleFilter aFilter) {
        Vector.Builder rBuilder = Vector.builder();
        for (Number tNumber : aIterable) {
            double tValue = tNumber.doubleValue();
            if (aFilter.accept(tValue)) rBuilder.add(tValue);
        }
        rBuilder.shrinkToFit();
        return rBuilder.build();
    }
    public static IVector filterDouble(IHasDoubleIterator aIterable, IDoubleFilter aFilter) {
        final Vector.Builder rBuilder = Vector.builder();
        aIterable.forEach(v -> {
            if (aFilter.accept(v)) rBuilder.add(v);
        });
        rBuilder.shrinkToFit();
        return rBuilder.build();
    }
    
    /**
     * merge two array into one List
     * @author liqa
     */
    public static <T> List<T> merge(T[] aBefore, T[] aAfter) {
        List<T> rOut = new ArrayList<>(aBefore.length+aAfter.length);
        for (T tValue : aBefore) rOut.add(tValue);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, T[] aAfter) {
        List<T> rOut = new ArrayList<>(1+aAfter.length);
        rOut.add(aBefore0);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, T aBefore1, T[] aAfter) {
        List<T> rOut = new ArrayList<>(2+aAfter.length);
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, T aBefore1, T aBefore2, T[] aAfter) {
        List<T> rOut = new ArrayList<>(3+aAfter.length);
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        rOut.add(aBefore2);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T[] aBefore, T aAfter0) {
        List<T> rOut = new ArrayList<>(aBefore.length+1);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        return rOut;
    }
    public static <T> List<T> merge(T[] aBefore, T aAfter0, T aAfter1) {
        List<T> rOut = new ArrayList<>(aBefore.length+2);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        return rOut;
    }
    public static <T> List<T> merge(T[] aBefore, T aAfter0, T aAfter1, T aAfter2) {
        List<T> rOut = new ArrayList<>(aBefore.length+3);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        rOut.add(aAfter2);
        return rOut;
    }
    public static <T> List<T> merge(Collection<? extends T> aBefore, Collection<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>(aBefore.size()+aAfter.size());
        for (T tValue : aBefore) rOut.add(tValue);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, Collection<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>(1+aAfter.size());
        rOut.add(aBefore0);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, T aBefore1, Collection<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>(2+aAfter.size());
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, T aBefore1, T aBefore2, Collection<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>(3+aAfter.size());
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        rOut.add(aBefore2);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(Collection<? extends T> aBefore, T aAfter0) {
        List<T> rOut = new ArrayList<>(aBefore.size()+1);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        return rOut;
    }
    public static <T> List<T> merge(Collection<? extends T> aBefore, T aAfter0, T aAfter1) {
        List<T> rOut = new ArrayList<>(aBefore.size()+2);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        return rOut;
    }
    public static <T> List<T> merge(Collection<? extends T> aBefore, T aAfter0, T aAfter1, T aAfter2) {
        List<T> rOut = new ArrayList<>(aBefore.size()+3);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        rOut.add(aAfter2);
        return rOut;
    }
    public static <T> List<T> merge(Iterable<? extends T> aBefore, Iterable<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, Iterable<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>();
        rOut.add(aBefore0);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, T aBefore1, Iterable<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>();
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(T aBefore0, T aBefore1, T aBefore2, Iterable<? extends T> aAfter) {
        List<T> rOut = new ArrayList<>();
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        rOut.add(aBefore2);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> List<T> merge(Iterable<? extends T> aBefore, T aAfter0) {
        List<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        return rOut;
    }
    public static <T> List<T> merge(Iterable<? extends T> aBefore, T aAfter0, T aAfter1) {
        List<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        return rOut;
    }
    public static <T> List<T> merge(Iterable<? extends T> aBefore, T aAfter0, T aAfter1, T aAfter2) {
        List<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        rOut.add(aAfter2);
        return rOut;
    }
    /**
     * Convert nested Iterable to a single one
     * @author liqa
     */
    public static <T> Iterable<T> merge(Iterable<? extends Iterable<? extends T>> aNestIterable) {
        List<T> rOut = new ArrayList<>();
        for (Iterable<? extends T> tIterable : aNestIterable) for (T tValue : tIterable) rOut.add(tValue);
        return rOut;
    }
}
