package jtool.code.collection;

import jtool.code.functional.IDoubleFilter;
import jtool.code.functional.IFilter;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.IOperator1;
import jtool.code.iterator.IHasDoubleIterator;
import jtool.code.iterator.IHasIntegerIterator;
import jtool.math.MathEX;
import jtool.math.vector.IIntegerVector;
import jtool.math.vector.IVector;
import jtool.math.vector.IntegerVector;
import jtool.math.vector.Vector;

import java.lang.reflect.Array;
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
    public static <T> ArrayList<T> zl() {return new ArrayList<>();}
    
    /**
     * 提供一些常用的 List 初始化
     * <p>
     * 同样这里可能会存在一些 lambda 表达式的重载，
     * 使用 Groovy 调用遇到问题时再考虑提供专门的方法即可
     * @author liqa
     */
    public static <T> ArrayList<T> nulls(int aSize) {
        ArrayList<T> rOut = new ArrayList<>(aSize);
        for (int i = 0; i < aSize; ++i) rOut.add(null);
        return rOut;
    }
    public static <T> ArrayList<T> from(int aSize, IListGetter<? extends T> aListGetter) {
        ArrayList<T> rOut = new ArrayList<>(aSize);
        for (int i = 0; i < aSize; ++i) rOut.add(aListGetter.get(i));
        return rOut;
    }
    public static <T> ArrayList<T> from(Iterable<? extends T> aIterable) {
        ArrayList<T> rOut = new ArrayList<>();
        for (T tValue : aIterable) rOut.add(tValue);
        return rOut;
    }
    public static <T> ArrayList<T> from(Collection<? extends T> aList) {
        ArrayList<T> rOut = new ArrayList<>(aList.size());
        for (T tValue : aList) rOut.add(tValue);
        return rOut;
    }
    public static <T> ArrayList<T> from(T[] aData) {
        ArrayList<T> rOut = new ArrayList<>(aData.length);
        for (T tValue : aData) rOut.add(tValue);
        return rOut;
    }
    public static ArrayList<Double> from(double[] aData) {
        ArrayList<Double> rOut = new ArrayList<>(aData.length);
        for (double tValue : aData) rOut.add(tValue);
        return rOut;
    }
    public static ArrayList<Integer> from(int[] aData) {
        ArrayList<Integer> rOut = new ArrayList<>(aData.length);
        for (int tValue : aData) rOut.add(tValue);
        return rOut;
    }
    public static ArrayList<Boolean> from(boolean[] aData) {
        ArrayList<Boolean> rOut = new ArrayList<>(aData.length);
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
    public static ArrayList<Integer> range_(int aStart, int aStop, int aStep) {
        ArrayList<Integer> rRange = new ArrayList<>(MathEX.Code.divup(aStop-aStart, aStep));
       for (int i = aStart; i < aStop; i += aStep) rRange.add(i);
       return rRange;
    }
    public static ArrayList<Integer> range_(            int aSize           ) {return range_(0, aSize);}
    public static ArrayList<Integer> range_(int aStart, int aStop           ) {return range_(aStart, aStop, 1);}
    public static ArrayList<Integer> range (            int aSize           ) {return range(0, aSize);}
    public static ArrayList<Integer> range (int aStart, int aStop           ) {return range(aStart, aStop, 1);}
    public static ArrayList<Integer> range (int aStart, int aStop, int aStep) {
        aStep = Math.max(aStep, 1);
        aStop = Math.max(aStop, aStart);
        return range_(aStart, aStop, aStep);
    }
    
    /**
     * map {@code Iterable<T> to List<R>}
     * @author liqa
     */
    public static <R, T> ArrayList<R> map(final Iterable<T> aIterable, final IOperator1<? extends R, ? super T> aOpt) {
        ArrayList<R> rOut = new ArrayList<>();
        for (T tValue : aIterable) rOut.add(aOpt.cal(tValue));
        return rOut;
    }
    public static <R, T> ArrayList<R> map(final Collection<T> aCollection, final IOperator1<? extends R, ? super T> aOpt) {
        ArrayList<R> rOut = new ArrayList<>(aCollection.size());
        for (T tValue : aCollection) rOut.add(aOpt.cal(tValue));
        return rOut;
    }
    public static <R, T> ArrayList<R> map(final T[] aArray, final IOperator1<? extends R, ? super T> aOpt) {
        ArrayList<R> rOut = new ArrayList<>(aArray.length);
        for (T tValue : aArray) rOut.add(aOpt.cal(tValue));
        return rOut;
    }
    
    
    /**
     * 提供通用切片接口
     * @author liqa
     */
    public static <T> ArrayList<T> slice(List<? extends T> aList, ISlice aIndices) {
        final int tSize = aIndices.size();
        ArrayList<T> rList = new ArrayList<>(tSize);
        for (int i = 0; i < tSize; ++i) rList.add(aList.get(aIndices.get(i)));
        return rList;
    }
    public static <T> ArrayList<T> slice(List<? extends T> aList, Iterable<Integer> aIndices) {
        ArrayList<T> rList = new ArrayList<>();
        for (int tIdx : aIndices) rList.add(aList.get(tIdx));
        rList.trimToSize();
        return rList;
    }
    public static <T> ArrayList<T> slice(List<? extends T> aList, Collection<Integer> aIndices) {
        ArrayList<T> rList = new ArrayList<>(aIndices.size());
        for (int tIdx : aIndices) rList.add(aList.get(tIdx));
        return rList;
    }
    public static <T> ArrayList<T> slice(List<? extends T> aList, int[] aIndices) {
        ArrayList<T> rList = new ArrayList<>(aIndices.length);
        for (int tIdx : aIndices) rList.add(aList.get(tIdx));
        return rList;
    }
    public static <T> ArrayList<T> slice(Collection<? extends T> aIterable, IIndexFilter aIndices) {
        ArrayList<T> rList = new ArrayList<>(aIterable.size());
        int tIdx = 0;
        for (T tValue : aIterable) {
            if (aIndices.accept(tIdx)) rList.add(tValue);
            ++tIdx;
        }
        rList.trimToSize();
        return rList;
    }
    public static <T> ArrayList<T> slice(Iterable<? extends T> aIterable, IIndexFilter aIndices) {
        ArrayList<T> rList = new ArrayList<>();
        int tIdx = 0;
        for (T tValue : aIterable) {
            if (aIndices.accept(tIdx)) rList.add(tValue);
            ++tIdx;
        }
        rList.trimToSize();
        return rList;
    }
    
    
    /**
     * 提供通用的执行过滤的接口
     * @author liqa
     */
    public static <T> ArrayList<T> filter(Iterable<? extends T> aIterable, IFilter<? super T> aFilter) {
        ArrayList<T> rList = new ArrayList<>();
        for (T tValue : aIterable) if (aFilter.accept(tValue)) rList.add(tValue);
        rList.trimToSize();
        return rList;
    }
    public static <T> ArrayList<T> filter(Collection<? extends T> aCollection, IFilter<? super T> aFilter) {
        ArrayList<T> rList = new ArrayList<>(aCollection.size());
        for (T tValue : aCollection) if (aFilter.accept(tValue)) rList.add(tValue);
        rList.trimToSize();
        return rList;
    }
    public static IntegerVector filterInteger(Iterable<Integer> aIndices, final IIndexFilter aFilter) {
        IntegerVector.Builder rBuilder = IntegerVector.builder();
        for (int tValue : aIndices) if (aFilter.accept(tValue)) rBuilder.add(tValue);
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static IntegerVector filterInteger(Collection<Integer> aIndices, final IIndexFilter aFilter) {
        IntegerVector.Builder rBuilder = IntegerVector.builder(aIndices.size());
        for (int tValue : aIndices) if (aFilter.accept(tValue)) rBuilder.add(tValue);
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static IntegerVector filterInteger(int aSize, IIndexFilter aFilter) {
        IntegerVector.Builder rBuilder = IntegerVector.builder(aSize);
        for (int i = 0; i < aSize; ++i) if (aFilter.accept(i)) rBuilder.add(i);
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static IntegerVector filterInteger(IHasIntegerIterator aIterable, IIndexFilter aFilter) {
        IntegerVector.Builder rBuilder = IntegerVector.builder();
        aIterable.forEach(i -> {
            if (aFilter.accept(i)) rBuilder.add(i);
        });
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static IntegerVector filterInteger(IIntegerVector aVector, IIndexFilter aFilter) {
        IntegerVector.Builder rBuilder = IntegerVector.builder(aVector.size());
        aVector.forEach(i -> {
            if (aFilter.accept(i)) rBuilder.add(i);
        });
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static Vector filterDouble(Iterable<? extends Number> aIterable, IDoubleFilter aFilter) {
        Vector.Builder rBuilder = Vector.builder();
        for (Number tNumber : aIterable) {
            double tValue = tNumber.doubleValue();
            if (aFilter.accept(tValue)) rBuilder.add(tValue);
        }
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static Vector filterDouble(Collection<? extends Number> aCollection, IDoubleFilter aFilter) {
        Vector.Builder rBuilder = Vector.builder(aCollection.size());
        for (Number tNumber : aCollection) {
            double tValue = tNumber.doubleValue();
            if (aFilter.accept(tValue)) rBuilder.add(tValue);
        }
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static Vector filterDouble(IHasDoubleIterator aIterable, IDoubleFilter aFilter) {
        final Vector.Builder rBuilder = Vector.builder();
        aIterable.forEach(v -> {
            if (aFilter.accept(v)) rBuilder.add(v);
        });
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static Vector filterDouble(IVector aVector, IDoubleFilter aFilter) {
        final Vector.Builder rBuilder = Vector.builder(aVector.size());
        aVector.forEach(v -> {
            if (aFilter.accept(v)) rBuilder.add(v);
        });
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    
    /**
     * merge two array into one array
     * @author liqa
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[] aBefore, T[] aAfter) {
        T[] rOut = (T[]) Array.newInstance(aBefore.getClass().getComponentType(), aBefore.length+aAfter.length);
        System.arraycopy(aBefore, 0, rOut, 0, aBefore.length);
        System.arraycopy(aAfter, 0, rOut, aBefore.length, aAfter.length);
        return rOut;
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T aBefore0, T[] aAfter) {
        T[] rOut = (T[]) Array.newInstance(aAfter.getClass().getComponentType(), 1+aAfter.length);
        rOut[0] = aBefore0;
        System.arraycopy(aAfter, 0, rOut, 1, aAfter.length);
        return rOut;
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T aBefore0, T aBefore1, T[] aAfter) {
        T[] rOut = (T[]) Array.newInstance(aAfter.getClass().getComponentType(), 2+aAfter.length);
        rOut[0] = aBefore0;
        rOut[1] = aBefore1;
        System.arraycopy(aAfter, 0, rOut, 2, aAfter.length);
        return rOut;
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T aBefore0, T aBefore1, T aBefore2, T[] aAfter) {
        T[] rOut = (T[]) Array.newInstance(aAfter.getClass().getComponentType(), 3+aAfter.length);
        rOut[0] = aBefore0;
        rOut[1] = aBefore1;
        rOut[2] = aBefore2;
        System.arraycopy(aAfter, 0, rOut, 3, aAfter.length);
        return rOut;
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[] aBefore, T aAfter0) {
        T[] rOut = (T[]) Array.newInstance(aBefore.getClass().getComponentType(), aBefore.length+1);
        System.arraycopy(aBefore, 0, rOut, 0, aBefore.length);
        rOut[aBefore.length] = aAfter0;
        return rOut;
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[] aBefore, T aAfter0, T aAfter1) {
        T[] rOut = (T[]) Array.newInstance(aBefore.getClass().getComponentType(), aBefore.length+2);
        System.arraycopy(aBefore, 0, rOut, 0, aBefore.length);
        int i = aBefore.length;
        rOut[i] = aAfter0; ++i;
        rOut[i] = aAfter1;
        return rOut;
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[] aBefore, T aAfter0, T aAfter1, T aAfter2) {
        T[] rOut = (T[]) Array.newInstance(aBefore.getClass().getComponentType(), aBefore.length+3);
        System.arraycopy(aBefore, 0, rOut, 0, aBefore.length);
        int i = aBefore.length;
        rOut[i] = aAfter0; ++i;
        rOut[i] = aAfter1; ++i;
        rOut[i] = aAfter2;
        return rOut;
    }
    public static <T> ArrayList<T> merge(Collection<? extends T> aBefore, Collection<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>(aBefore.size()+aAfter.size());
        for (T tValue : aBefore) rOut.add(tValue);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> ArrayList<T> merge(T aBefore0, Collection<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>(1+aAfter.size());
        rOut.add(aBefore0);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> ArrayList<T> merge(T aBefore0, T aBefore1, Collection<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>(2+aAfter.size());
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> ArrayList<T> merge(T aBefore0, T aBefore1, T aBefore2, Collection<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>(3+aAfter.size());
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        rOut.add(aBefore2);
        for (T tValue : aAfter) rOut.add(tValue);
        return rOut;
    }
    public static <T> ArrayList<T> merge(Collection<? extends T> aBefore, T aAfter0) {
        ArrayList<T> rOut = new ArrayList<>(aBefore.size()+1);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        return rOut;
    }
    public static <T> ArrayList<T> merge(Collection<? extends T> aBefore, T aAfter0, T aAfter1) {
        ArrayList<T> rOut = new ArrayList<>(aBefore.size()+2);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        return rOut;
    }
    public static <T> ArrayList<T> merge(Collection<? extends T> aBefore, T aAfter0, T aAfter1, T aAfter2) {
        ArrayList<T> rOut = new ArrayList<>(aBefore.size()+3);
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        rOut.add(aAfter2);
        return rOut;
    }
    public static <T> ArrayList<T> merge(Iterable<? extends T> aBefore, Iterable<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        for (T tValue : aAfter) rOut.add(tValue);
        rOut.trimToSize();
        return rOut;
    }
    public static <T> ArrayList<T> merge(T aBefore0, Iterable<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>();
        rOut.add(aBefore0);
        for (T tValue : aAfter) rOut.add(tValue);
        rOut.trimToSize();
        return rOut;
    }
    public static <T> ArrayList<T> merge(T aBefore0, T aBefore1, Iterable<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>();
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        for (T tValue : aAfter) rOut.add(tValue);
        rOut.trimToSize();
        return rOut;
    }
    public static <T> ArrayList<T> merge(T aBefore0, T aBefore1, T aBefore2, Iterable<? extends T> aAfter) {
        ArrayList<T> rOut = new ArrayList<>();
        rOut.add(aBefore0);
        rOut.add(aBefore1);
        rOut.add(aBefore2);
        for (T tValue : aAfter) rOut.add(tValue);
        rOut.trimToSize();
        return rOut;
    }
    public static <T> ArrayList<T> merge(Iterable<? extends T> aBefore, T aAfter0) {
        ArrayList<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.trimToSize();
        return rOut;
    }
    public static <T> ArrayList<T> merge(Iterable<? extends T> aBefore, T aAfter0, T aAfter1) {
        ArrayList<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        rOut.trimToSize();
        return rOut;
    }
    public static <T> ArrayList<T> merge(Iterable<? extends T> aBefore, T aAfter0, T aAfter1, T aAfter2) {
        ArrayList<T> rOut = new ArrayList<>();
        for (T tValue : aBefore) rOut.add(tValue);
        rOut.add(aAfter0);
        rOut.add(aAfter1);
        rOut.add(aAfter2);
        rOut.trimToSize();
        return rOut;
    }
    /**
     * Convert nested Iterable to a single one
     * @author liqa
     */
    public static <T> ArrayList<T> merge(Iterable<? extends Iterable<? extends T>> aNestIterable) {
        ArrayList<T> rOut = new ArrayList<>();
        for (Iterable<? extends T> tIterable : aNestIterable) for (T tValue : tIterable) rOut.add(tValue);
        rOut.trimToSize();
        return rOut;
    }
}
