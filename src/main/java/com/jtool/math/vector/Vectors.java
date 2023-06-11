package com.jtool.math.vector;


import com.jtool.code.ISetIterator;

import java.util.Collection;

/**
 * @author liqa
 * <p> 获取向量的类，默认获取 {@link Vector} </p>
 */
public class Vectors {
    private Vectors() {}
    
    public static IVector ones(int aSize) {return Vector.ones(aSize);}
    public static IVector zeros(int aSize) {return Vector.zeros(aSize);}
    
    
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
    
    public static IVector from(int aSize, Iterable<? extends Number> aList) {
        IVector rVector = zeros(aSize);
        rVector.fill(aList);
        return rVector;
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
    
    
    /** Vector 特有的构造 */
    public static IVector sequence(double aStart, double aStep, double aEnd) {
        int tSize = (int)Math.floor((aEnd-aStart)/aStep) + 1;
        return sequenceByStep(aStart, aStep, tSize);
    }
    public static IVector sequenceByStep(double aStart, double aStep, int aN) {
        final IVector rVector = zeros(aN);
        final ISetIterator<Double> si = rVector.setIterator();
        double tValue = aStart;
        while (si.hasNext()) {
            si.nextAndSet(tValue);
            tValue += aStep;
        }
        return rVector;
    }
}
