package com.jtool.math.vector;


import com.jtool.code.UT;

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
    public static IVector from(IVectorAny<?> aVector) {
        if (aVector instanceof Vector) {
            return ((Vector)aVector).generator().same();
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
}
