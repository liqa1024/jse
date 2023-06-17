package com.jtool.code.filter;

import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface IDoubleFilter {
    boolean accept(double aD);
    
    /** 提供一个通用的执行过滤的接口 */
    static List<Double> filter(Iterable<? extends Number> aList, IDoubleFilter aFilter) {
        List<Double> rList = new ArrayList<>();
        for (Number tNumber : aList) {
            double tValue = tNumber.doubleValue();
            if (aFilter.accept(tValue)) rList.add(tValue);
        }
        return rList;
    }
    static IVector filter(IVector aVector, IDoubleFilter aFilter) {
        List<Double> rList = new ArrayList<>();
        for (double tValue : aVector.iterable()) if (aFilter.accept(tValue)) rList.add(tValue);
        return Vectors.from(rList);
    }
}
