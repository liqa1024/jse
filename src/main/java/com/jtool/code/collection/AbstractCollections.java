package com.jtool.code.collection;

import com.google.common.collect.ImmutableList;
import com.jtool.code.filter.IDoubleFilter;
import com.jtool.code.filter.IFilter;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IOperator1;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IHasDoubleIterator;
import com.jtool.math.MathEX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Stream;

/**
 * 获取抽象容器的类，这里获取的结果统一为引用
 * @author liqa
 */
public class AbstractCollections {
    protected AbstractCollections() {}
    
    public static <T> @Unmodifiable List<T> zl() {return ImmutableList.of();}
    
    /** 通用的转换方法 */
    public static <T> List<T> from(final int aSize, final IOperator1<? extends T, Integer> aListGetter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {return aListGetter.cal(index);}
            @Override public int size() {return aSize;}
        };
    }
    public static <T> Collection<T> from(final int aSize, final Iterable<T> aIterable) {
        return new AbstractCollection<T>() {
            @Override public @NotNull Iterator<T> iterator() {return aIterable.iterator();}
            @Override public int size() {return aSize;}
        };
    }
    
    /**
     * {@link Arrays#asList}
     * @author liqa
     * @param aData the input T[]
     * @return the list format of T[]
     */
    public static <T> List<T> from(T[] aData) {return Arrays.asList(aData);}
    /**
     * {@link Arrays#asList} for double[]
     * @author liqa
     * @param aData the input double[]
     * @return the list format of double[]
     */
    public static List<Double> from(final double[] aData) {
        return new AbstractRandomAccessList<Double>() {
            @Override public Double get(int index) {return aData[index];}
            @Override public Double set(int index, Double element) {
                double oValue = aData[index];
                aData[index] = element;
                return oValue;
            }
            @Override public int size() {return aData.length;}
        };
    }
    /**
     * {@link Arrays#asList} for int[]
     * @author liqa
     * @param aData the input int[]
     * @return the list format of int[]
     */
    public static List<Integer> from(final int[] aData) {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return aData[index];}
            @Override public Integer set(int index, Integer element) {
                int oValue = aData[index];
                aData[index] = element;
                return oValue;
            }
            @Override public int size() {return aData.length;}
        };
    }
    /**
     * {@link Arrays#asList} for boolean[]
     * @author liqa
     * @param aData the input boolean[]
     * @return the list format of boolean[]
     */
    public static List<Boolean> from(final boolean[] aData) {
        return new AbstractRandomAccessList<Boolean>() {
            @Override public Boolean get(int index) {return aData[index];}
            @Override public Boolean set(int index, Boolean element) {
                boolean oValue = aData[index];
                aData[index] = element;
                return oValue;
            }
            @Override public int size() {return aData.length;}
        };
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
    public static List<Integer> range_(int aStart, int aStop, int aStep) {return new Range(aStart, aStop, aStep);}
    public static List<Integer> range_(            int aSize           ) {return range_(0, aSize);}
    public static List<Integer> range_(int aStart, int aStop           ) {return range_(aStart, aStop, 1);}
    public static List<Integer> range (            int aSize           ) {return range(0, aSize);}
    public static List<Integer> range (int aStart, int aStop           ) {return range(aStart, aStop, 1);}
    public static List<Integer> range (int aStart, int aStop, int aStep) {
        aStep = Math.max(aStep, 1);
        aStop = Math.max(aStop, aStart);
        return range_(aStart, aStop, aStep);
    }
    private final static class Range extends AbstractRandomAccessList<Integer> {
        private final int mStart, mStop, mStep;
        private final int mSize;
        Range(int aStart, int aStop, int aStep) {
            mStart = aStart;
            mStop = aStop;
            mStep = aStep;
            mSize = MathEX.Code.divup(aStop-aStart, aStep);
        }
        
        @Override public Integer get(int index) {
            if (index >= mSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+mSize);
            return mStart + index*mStep;
        }
        @Override public int size() {return mSize;}
        
        /** 重写一下 Iterator 加速遍历的速度 */
        private class RangeItr implements Iterator<Integer> {
            protected int mIdx;
            RangeItr() {mIdx = mStart;}
            @Override public boolean hasNext() {return mIdx < mStop;}
            @Override public Integer next() {
                if (hasNext()) {
                    int tIdx = mIdx;
                    mIdx += mStep;
                    return tIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }
        private class RangeListItr extends RangeItr implements ListIterator<Integer> {
            RangeListItr(int aIndex) {super(); mIdx += aIndex*mStep;}
            
            @Override public boolean hasPrevious() {return mIdx > mStart;}
            @Override public Integer previous() {
                if (hasPrevious()) {
                    mIdx -= mStep;
                    return mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public int nextIndex() {return (mIdx-mStart)/mStep;}
            @Override public int previousIndex() {return (mIdx-mStart)/mStep - 1;}
            
            @Override public void remove() {throw new UnsupportedOperationException("remove");}
            @Override public void set(Integer integer) {throw new UnsupportedOperationException("set");}
            @Override public void add(Integer integer) {throw new UnsupportedOperationException("add");}
        }
        
        @Override public @NotNull Iterator<Integer> iterator() {return new RangeItr();}
        @Override public @NotNull ListIterator<Integer> listIterator(int index) {
            if (index >= mSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+mSize);
            return new RangeListItr(index);
        }
    }
    
    /**
     * map {@code Iterable<T> to Iterable<R>} like {@link Stream}.map
     * @author liqa
     */
    public static <R, T> Iterable<R> map(final Iterable<T> aIterable, final IOperator1<? extends R, ? super T> aOpt) {
        return () -> map(aIterable.iterator(), aOpt);
    }
    public static <R, T> Iterator<R> map(final Iterator<T> aIterator, final IOperator1<? extends R, ? super T> aOpt) {
        return new Iterator<R>() {
            final Iterator<T> mIt = aIterator;
            @Override public boolean hasNext() {
                return mIt.hasNext();
            }
            @Override public R next() {
                if (hasNext()) {
                    return aOpt.cal(mIt.next());
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    public static <R, T> Collection<R> map(final Collection<T> aCollection, final IOperator1<? extends R, ? super T> aOpt) {
        return new AbstractCollection<R>() {
            @Override public @NotNull Iterator<R> iterator() {return map(aCollection.iterator(), aOpt);}
            @Override public int size() {return aCollection.size();}
        };
    }
    public static <R, T> List<R> map(final List<T> aList, final IOperator1<? extends R, ? super T> aOpt) {
        return new AbstractRandomAccessList<R>() {
            @Override public R get(int index) {return aOpt.cal(aList.get(index));}
            @Override public int size() {return aList.size();}
            @Override public @NotNull Iterator<R> iterator() {return map(aList.iterator(), aOpt);}
        };
    }
    public static <R, T> List<R> map(final T[] aArray, final IOperator1<? extends R, ? super T> aOpt) {
        return new AbstractRandomAccessList<R>() {
            @Override public R get(int index) {return aOpt.cal(aArray[index]);}
            @Override public int size() {return aArray.length;}
        };
    }
    
    
    /**
     * 提供通用切片接口
     * @author liqa
     */
    public static <T> List<T> slice(final List<? extends T> aList, final List<Integer> aIndices) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {return aList.get(aIndices.get(index));}
            @Override public int size() {return aIndices.size();}
        };
    }
    public static <T> List<T> slice(List<? extends T> aList, Iterable<Integer> aIndices) {
        return slice(aList, NewCollections.from(aIndices));
    }
    public static <T> List<T> slice(List<? extends T> aList, Collection<Integer> aIndices) {
        return slice(aList, NewCollections.from(aIndices));
    }
    public static <T> List<T> slice(final List<? extends T> aList, final int[] aIndices) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {return aList.get(aIndices[index]);}
            @Override public int size() {return aIndices.length;}
        };
    }
    public static <T> List<T> slice(final List<? extends T> aList, IIndexFilter aIndices) {
        return slice(aList, NewCollections.filterInteger(aList.size(), aIndices));
    }
    
    
    /**
     * 提供通用的执行过滤的接口
     * @author liqa
     */
    public static <T> Iterable<T> filter(final Iterable<? extends T> aIterable, final IFilter<? super T> aFilter) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mIt = aIterable.iterator();
            private boolean mNextValid = false;
            private @Nullable T mNext = null;
            
            @Override public boolean hasNext() {
                while (true) {
                    if (mNextValid) return true;
                    if (mIt.hasNext()) {
                        mNext = mIt.next();
                        // 过滤器通过则设为合法跳过
                        if (aFilter.accept(mNext)) {
                            mNextValid = true;
                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            }
            @Override public T next() {
                if (hasNext()) {
                    T tNext = mNext;
                    mNext = null;
                    mNextValid = false; // 设置非法表示此时不再有 Next
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    public static Iterable<Integer> filterInteger(Iterable<Integer> aIndices, IIndexFilter aFilter) {
        return filter(aIndices, aFilter::accept);
    }
    public static Iterable<Integer> filterInteger(final int aSize, final IIndexFilter aFilter) {
        return () -> new Iterator<Integer>() {
            private int mIdx = 0, mNext = -1;
            
            @Override public boolean hasNext() {
                while (true) {
                    if (mNext >= 0) return true;
                    if (mIdx < aSize) {
                        // 过滤器通过设置 mNext 合法
                        if (aFilter.accept(mIdx)) mNext = mIdx;
                        ++mIdx;
                    } else {
                        return false;
                    }
                }
            }
            @Override public Integer next() {
                if (hasNext()) {
                    int tNext = mNext;
                    mNext = -1; // 设置 mNext 非法表示此时不再有 Next
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    public static Iterable<? extends Number> filterDouble(Iterable<? extends Number> aIterable, final IDoubleFilter aFilter) {
        return filter(aIterable, v -> aFilter.accept(v.doubleValue()));
    }
    public static IHasDoubleIterator filterDouble(final IHasDoubleIterator aIterable, final IDoubleFilter aFilter) {
        return () -> new IDoubleIterator() {
            private final IDoubleIterator mIt = aIterable.iterator();
            private boolean mNextValid = false;
            private double mNext = Double.NaN;
            
            @Override public boolean hasNext() {
                while (true) {
                    if (mNextValid) return true;
                    if (mIt.hasNext()) {
                        mNext = mIt.next();
                        // 过滤器通过则设为合法跳过
                        if (aFilter.accept(mNext)) {
                            mNextValid = true;
                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            }
            @Override public double next() {
                if (hasNext()) {
                    mNextValid = false; // 设置非法表示此时不再有 Next
                    return mNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    /**
     * merge two array into one List
     * @author liqa
     */
    public static <T> List<T> merge(final T[] aBefore, final T[] aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {return index<aBefore.length ? aBefore[index] : aAfter[index-aBefore.length];}
            @Override public int size() {return aBefore.length+aAfter.length;}
        };
    }
    public static <T> List<T> merge(final T aBefore0, final T[] aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {return index<1 ? aBefore0 : aAfter[index-1];}
            @Override public int size() {return aAfter.length+1;}
        };
    }
    public static <T> List<T> merge(final T aBefore0, final T aBefore1, final T[] aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                switch (index) {
                case 0: return aBefore0;
                case 1: return aBefore1;
                default: return aAfter[index-2];
                }
            }
            @Override public int size() {return aAfter.length+2;}
        };
    }
    public static <T> List<T> merge(final T aBefore0, final T aBefore1, final T aBefore2, final T[] aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                switch (index) {
                case 0: return aBefore0;
                case 1: return aBefore1;
                case 2: return aBefore2;
                default: return aAfter[index-3];
                }
            }
            @Override public int size() {return aAfter.length+3;}
        };
    }
    public static <T> List<T> merge(final T[] aBefore, final T aAfter0) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                int tRest = index-aBefore.length;
                return tRest==0 ? aAfter0 : aBefore[index];
            }
            @Override public int size() {return aBefore.length+1;}
        };
    }
    public static <T> List<T> merge(final T[] aBefore, final T aAfter0, final T aAfter1) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                int tRest = index-aBefore.length;
                switch (tRest) {
                case 0: return aAfter0;
                case 1: return aAfter1;
                default: return aBefore[index];
                }
            }
            @Override public int size() {return aBefore.length+2;}
        };
    }
    public static <T> List<T> merge(final T[] aBefore, final T aAfter0, final T aAfter1, final T aAfter2) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                int tRest = index-aBefore.length;
                switch (tRest) {
                case 0: return aAfter0;
                case 1: return aAfter1;
                case 2: return aAfter2;
                default: return aBefore[index];
                }
            }
            @Override public int size() {return aBefore.length+3;}
        };
    }
    public static <T> List<T> merge(final List<? extends T> aBefore, final List<? extends T> aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {return index<aBefore.size() ? aBefore.get(index) : aAfter.get(index-aBefore.size());}
            @Override public int size() {return aBefore.size()+aAfter.size();}
        };
    }
    public static <T> List<T> merge(final T aBefore0, final List<? extends T> aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {return index<1 ? aBefore0 : aAfter.get(index-1);}
            @Override public int size() {return aAfter.size()+1;}
        };
    }
    public static <T> List<T> merge(final T aBefore0, final T aBefore1, final List<? extends T> aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                switch (index) {
                case 0: return aBefore0;
                case 1: return aBefore1;
                default: return aAfter.get(index-2);
                }
            }
            @Override public int size() {return aAfter.size()+2;}
        };
    }
    public static <T> List<T> merge(final T aBefore0, final T aBefore1, final T aBefore2, final List<? extends T> aAfter) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                switch (index) {
                case 0: return aBefore0;
                case 1: return aBefore1;
                case 2: return aBefore2;
                default: return aAfter.get(index-3);
                }
            }
            @Override public int size() {return aAfter.size()+3;}
        };
    }
    public static <T> List<T> merge(final List<? extends T> aBefore, final T aAfter0) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                int tRest = index-aBefore.size();
                return tRest==0 ? aAfter0 : aBefore.get(index);
            }
            @Override public int size() {return aBefore.size()+1;}
        };
    }
    public static <T> List<T> merge(final List<? extends T> aBefore, final T aAfter0, final T aAfter1) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                int tRest = index-aBefore.size();
                switch (tRest) {
                case 0: return aAfter0;
                case 1: return aAfter1;
                default: return aBefore.get(index);
                }
            }
            @Override public int size() {return aBefore.size()+2;}
        };
    }
    public static <T> List<T> merge(final List<? extends T> aBefore, final T aAfter0, final T aAfter1, final T aAfter2) {
        return new AbstractRandomAccessList<T>() {
            @Override public T get(int index) {
                int tRest = index-aBefore.size();
                switch (tRest) {
                case 0: return aAfter0;
                case 1: return aAfter1;
                case 2: return aAfter2;
                default: return aBefore.get(index);
                }
            }
            @Override public int size() {return aBefore.size()+3;}
        };
    }
    public static <T> Iterable<T> merge(final Iterable<? extends T> aBefore, final Iterable<? extends T> aAfter) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mItB = aBefore.iterator();
            private final Iterator<? extends T> mItA = aAfter.iterator();
            
            @Override public boolean hasNext() {
                return mItB.hasNext() || mItA.hasNext();
            }
            @Override public T next() {
                return mItB.hasNext() ? mItB.next() : mItA.next();
            }
        };
    }
    public static <T> Iterable<T> merge(final T aBefore0, final Iterable<? extends T> aAfter) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mIt = aAfter.iterator();
            private boolean mFirst = true;
            
            @Override public boolean hasNext() {
                if (mFirst) return true;
                else return mIt.hasNext();
            }
            @Override public T next() {
                if (mFirst) {
                    mFirst = false;
                    return aBefore0;
                } else {
                    return mIt.next();
                }
            }
        };
    }
    public static <T> Iterable<T> merge(final T aBefore0, final T aBefore1, final Iterable<? extends T> aAfter) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mIt = aAfter.iterator();
            private int mIndex = -1;
            
            @Override public boolean hasNext() {
                if (mIndex < 1) return true;
                else return mIt.hasNext();
            }
            @Override public T next() {
                if (mIndex < 1) {
                    ++mIndex;
                    switch (mIndex) {
                    case 0: return aBefore0;
                    case 1: return aBefore1;
                    default: throw new RuntimeException();
                    }
                } else {
                    return mIt.next();
                }
            }
        };
    }
    public static <T> Iterable<T> merge(final T aBefore0, final T aBefore1, final T aBefore2, final Iterable<? extends T> aAfter) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mIt = aAfter.iterator();
            private int mIndex = -1;
            
            @Override public boolean hasNext() {
                if (mIndex < 2) return true;
                return mIt.hasNext();
            }
            @Override public T next() {
                if (mIndex < 2) {
                    ++mIndex;
                    switch (mIndex) {
                    case 0: return aBefore0;
                    case 1: return aBefore1;
                    case 2: return aBefore2;
                    default: throw new RuntimeException();
                    }
                } else {
                    return mIt.next();
                }
            }
        };
    }
    public static <T> Iterable<T> merge(final Iterable<? extends T> aBefore, final T aAfter0) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mIt = aBefore.iterator();
            private boolean mLast = false;
            @Override public boolean hasNext() {
                return !mLast;
            }
            @Override public T next() {
                if (mLast) throw new NoSuchElementException();
                if (mIt.hasNext()) {
                    return mIt.next();
                } else {
                    mLast = true;
                    return aAfter0;
                }
            }
        };
    }
    public static <T> Iterable<T> merge(final Iterable<? extends T> aBefore, final T aAfter0, final T aAfter1) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mIt = aBefore.iterator();
            private int mIndex = -1;
            
            @Override public boolean hasNext() {
                return mIndex < 1;
            }
            @Override public T next() {
                if (mIndex == 1) throw new NoSuchElementException();
                if (mIt.hasNext()) {
                    return mIt.next();
                } else {
                    ++mIndex;
                    switch (mIndex) {
                    case 0: return aAfter0;
                    case 1: return aAfter1;
                    default: throw new RuntimeException();
                    }
                }
            }
        };
    }
    public static <T> Iterable<T> merge(final Iterable<? extends T> aBefore, final T aAfter0, final T aAfter1, final T aAfter2) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends T> mIt = aBefore.iterator();
            private int mIndex = -1;
            
            @Override public boolean hasNext() {
                return mIndex < 2;
            }
            @Override public T next() {
                if (mIndex == 2) throw new NoSuchElementException();
                if (mIt.hasNext()) {
                    return mIt.next();
                } else {
                    ++mIndex;
                    switch (mIndex) {
                    case 0: return aAfter0;
                    case 1: return aAfter1;
                    case 2: return aAfter2;
                    default: throw new RuntimeException();
                    }
                }
            }
        };
    }
    /**
     * Convert nested Iterable to a single one
     * @author liqa
     */
    public static <T> Iterable<T> merge(final Iterable<? extends Iterable<? extends T>> aNestIterable) {
        return () -> new Iterator<T>() {
            private final Iterator<? extends Iterable<? extends T>> mParentIt = aNestIterable.iterator();
            private @Nullable Iterator<? extends T> mIt = mParentIt.hasNext() ? mParentIt.next().iterator() : null;
            private boolean mNextValid = false;
            private @Nullable T mNext = null;
            
            @Override public boolean hasNext() {
                if (mIt == null) return false;
                while (true) {
                    if (mNextValid) return true;
                    if (mIt.hasNext()) {
                        mNext = mIt.next();
                        mNextValid = true;
                        return true;
                    } else
                    if (mParentIt.hasNext()) {
                        mIt = mParentIt.next().iterator();
                        mNext = null;
                        mNextValid = false;
                    } else {
                        mIt = null;
                        return false;
                    }
                }
            }
            @Override public T next() {
                if (hasNext()) {
                    T tNext = mNext;
                    mNext = null;
                    mNextValid = false; // 设置非法表示此时不再有 Next
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
