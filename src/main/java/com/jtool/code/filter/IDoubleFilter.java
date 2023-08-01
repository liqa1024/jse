package com.jtool.code.filter;

import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface IDoubleFilter {
    boolean accept(double aD);
    
    /**
     * 提供通用的执行过滤的接口
     * @author liqa
     */
    static Iterable<? extends Number> filter(Iterable<? extends Number> aList, IDoubleFilter aFilter) {
        return IFilter.filter(aList, v -> aFilter.accept(v.doubleValue()));
    }
    static IVector fixedFilter(IVector aVector, IDoubleFilter aFilter) {
        List<Double> rList = new ArrayList<>();
        for (double tValue : aVector.iterable()) if (aFilter.accept(tValue)) rList.add(tValue);
        return Vectors.from(rList);
    }
}
