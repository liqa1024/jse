package com.jtool.code.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@FunctionalInterface
public interface IIndexFilter {
    boolean accept(int aIdx);
    
    /**
     * 提供通用的执行过滤的接口
     * @author liqa
     */
    static Iterable<Integer> filter(Iterable<Integer> aIndices, IIndexFilter aFilter) {
        return IFilter.filter(aIndices, aFilter::accept);
    }
    static Iterable<Integer> filter(final int aSize, final IIndexFilter aFilter) {
        return () -> new Iterator<Integer>() {
            private int mIdx = 0, mNext = -1;
            
            @Override public boolean hasNext() {
                while (true) {
                    if (mNext >= 0) return true;
                    if (mIdx < aSize) {
                        // 过滤器通过设置 mNext 合法
                        if (!aFilter.accept(mIdx)) mNext = mIdx;
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
    static List<Integer> fixedFilter(int aSize, IIndexFilter aFilter) {
        List<Integer> rIndices = new ArrayList<>();
        for (int i = 0; i < aSize; ++i) if (aFilter.accept(i)) rIndices.add(i);
        return rIndices;
    }
}
