package com.jtool.math.vector;


import com.jtool.code.collection.NewCollections;
import com.jtool.code.filter.IDoubleFilter;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.iterator.IHasDoubleIterator;

import java.util.Collection;

/**
 * @author liqa
 * <p> 获取向量的类，默认获取 {@link Vector} </p>
 */
public class Vectors {
    private Vectors() {}
    
    public static IVector ones(int aSize) {return Vector.ones(aSize);}
    public static IVector zeros(int aSize) {return Vector.zeros(aSize);}
    public static IVector NaN(int aSize) {
        IVector rVector = zeros(aSize);
        rVector.fill(Double.NaN);
        return rVector;
    }
    
    public static IVector from(int aSize, IVectorGetter aVectorGetter) {
        IVector rVector = zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    public static IVector from(IVector aVector) {
        if (aVector instanceof Vector) {
            return aVector.copy();
        } else {
            IVector rVector = zeros(aVector.size());
            rVector.fill(aVector);
            return rVector;
        }
    }
    
    public static IVector from(Iterable<? extends Number> aIterable) {
        final Vector.Builder rBuilder = Vector.builder();
        for (Number tValue : aIterable) rBuilder.add(tValue.doubleValue());
        rBuilder.shrinkToFit();
        return rBuilder.build();
    }
    public static IVector from(Collection<? extends Number> aList) {
        IVector rVector = zeros(aList.size());
        rVector.fill(aList);
        return rVector;
    }
    public static IVector from(double[] aData) {
        IVector rVector = zeros(aData.length);
        rVector.fill(aData);
        return rVector;
    }
    
    public static IVector merge(IVector aBefore, IVector aAfter) {
        IVector rVector = zeros(aBefore.size()+aAfter.size());
        // 原则上使用优化后的 refSlicer 会更快，但是优化需要的代码量较大，这里直接使用迭代器遍历，一个适中的优化效果
        final IDoubleSetIterator si = rVector.setIterator();
        final IDoubleIterator itB = aBefore.iterator();
        final IDoubleIterator itA = aAfter.iterator();
        while (itB.hasNext()) si.nextAndSet(itB.next());
        while (itA.hasNext()) si.nextAndSet(itA.next());
        return rVector;
    }
    
    /** 也提供过滤的接口，但是这里使用 vector 的写法，不涉及 {@link IHasDoubleIterator} */
    public static IVector filter(Iterable<? extends Number> aIterable, IDoubleFilter aFilter) {
        return NewCollections.filterDouble(aIterable, aFilter);
    }
    public static IVector filter(IVector aVector, IDoubleFilter aFilter) {
        return NewCollections.filterDouble(aVector, aFilter);
    }
    
    
    /** Vector 特有的构造 */
    public static IVector sequence(double aStart, double aStep, double aEnd) {
        int tSize = (int)Math.floor((aEnd-aStart)/aStep) + 1;
        return linspace(aStart, aStep, tSize);
    }
    public static IVector linspace(double aStart, double aStep, int aN) {
        final IVector rVector = zeros(aN);
        final IDoubleSetIterator si = rVector.setIterator();
        double tValue = aStart;
        while (si.hasNext()) {
            si.nextAndSet(tValue);
            tValue += aStep;
        }
        return rVector;
    }
}
