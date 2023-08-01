package com.jtool.code.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@FunctionalInterface
public interface IFilter<T> {
    boolean accept(T aInput);
    
    /**
     * 提供通用的执行过滤的接口
     * @author liqa
     */
    static <T> Iterable<T> filter(final Iterable<T> aIterable, final IFilter<? super T> aFilter) {
        return () -> new Iterator<T>() {
            private final Iterator<T> mIt = aIterable.iterator();
            private T mNext = null;
            
            @Override public boolean hasNext() {
                while (true) {
                    if (mNext != null) return true;
                    if (mIt.hasNext()) {
                        mNext = mIt.next();
                        // 过滤器不通过则设为 null 跳过
                        if (!aFilter.accept(mNext)) mNext = null;
                        continue;
                    }
                    return false;
                }
            }
            @Override public T next() {
                if (hasNext()) {
                    T tNext = mNext;
                    mNext = null; // 设置 mNext 非法表示此时不再有 Next
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    static <T> List<T> fixedFilter(Iterable<? extends T> aList, IFilter<? super T> aFilter) {
        List<T> rList = new ArrayList<>();
        for (T tValue : aList) if (aFilter.accept(tValue)) rList.add(tValue);
        return rList;
    }
}
