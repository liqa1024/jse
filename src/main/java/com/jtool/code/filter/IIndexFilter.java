package com.jtool.code.filter;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface IIndexFilter {
    boolean accept(int aIdx);
    
    /** 提供一个通用的执行过滤的接口 */
    static List<Integer> filter(int aSize, IIndexFilter aFilter) {
        List<Integer> rIndices = new ArrayList<>();
        for (int i = 0; i < aSize; ++i) if (aFilter.accept(i)) rIndices.add(i);
        return rIndices;
    }
    static List<Integer> filter(Iterable<Integer> aIndices, IIndexFilter aFilter) {
        List<Integer> rIndices = new ArrayList<>();
        for (int tIdx : aIndices) if (aFilter.accept(tIdx)) rIndices.add(tIdx);
        return rIndices;
    }
}
