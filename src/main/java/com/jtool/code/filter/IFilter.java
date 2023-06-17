package com.jtool.code.filter;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface IFilter<T> {
    boolean accept(T aInput);
    
    /** 提供一个通用的执行过滤的接口 */
    static <T> List<T> filter(Iterable<? extends T> aList, IFilter<? super T> aFilter) {
        List<T> rList = new ArrayList<>();
        for (T tValue : aList) if (aFilter.accept(tValue)) rList.add(tValue);
        return rList;
    }
}
