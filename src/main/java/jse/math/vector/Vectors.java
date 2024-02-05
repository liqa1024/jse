package jse.math.vector;


import jse.code.collection.NewCollections;
import jse.code.functional.IDoubleFilter;
import jse.code.iterator.IDoubleSetIterator;
import jse.code.iterator.IHasDoubleIterator;
import jse.math.MathEX;
import groovy.lang.Closure;

import java.util.Collection;

/**
 * @author liqa
 * <p> 获取向量的类，默认获取 {@link Vector} </p>
 */
public class Vectors {
    private Vectors() {}
    
    public static Vector ones(int aSize) {return Vector.ones(aSize);}
    public static Vector zeros(int aSize) {return Vector.zeros(aSize);}
    public static Vector NaN(int aSize) {
        Vector rVector = zeros(aSize);
        rVector.fill(Double.NaN);
        return rVector;
    }
    
    public static Vector from(int aSize, IVectorGetter aVectorGetter) {
        Vector rVector = zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    public static Vector from(IVector aVector) {
        Vector rVector = zeros(aVector.size());
        rVector.fill(aVector);
        return rVector;
    }
    /** Groovy stuff */
    public static Vector from(int aSize, final Closure<? extends Number> aGroovyTask) {return from(aSize, i -> aGroovyTask.call(i).doubleValue());}
    
    public static Vector from(Iterable<? extends Number> aIterable) {
        final Vector.Builder rBuilder = Vector.builder();
        for (Number tValue : aIterable) rBuilder.add(tValue.doubleValue());
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static Vector from(Collection<? extends Number> aList) {
        Vector rVector = zeros(aList.size());
        rVector.fill(aList);
        return rVector;
    }
    public static Vector from(double[] aData) {
        Vector rVector = zeros(aData.length);
        rVector.fill(aData);
        return rVector;
    }
    
    /** Vector 特有的构造 */
    public static Vector linsequence(double aStart, double aStep, int aN) {
        final Vector rVector = zeros(aN);
        final IDoubleSetIterator si = rVector.setIterator();
        double tValue = aStart;
        while (si.hasNext()) {
            si.nextAndSet(tValue);
            tValue += aStep;
        }
        return rVector;
    }
    public static Vector linspace(double aStart, double aEnd, int aN) {
        double tStep = (aEnd-aStart)/(double)(aN-1);
        return linsequence(aStart, tStep, aN);
    }
    
    public static Vector logsequence(double aStart, double aStep, int aN) {
        final Vector rVector = zeros(aN);
        final IDoubleSetIterator si = rVector.setIterator();
        double tValue = aStart;
        while (si.hasNext()) {
            si.nextAndSet(tValue);
            tValue *= aStep;
        }
        return rVector;
    }
    public static Vector logspace(double aStart, double aEnd, int aN) {
        double tStep = MathEX.Fast.pow(aEnd/aStart, 1.0/(double)(aN-1));
        return logsequence(aStart, tStep, aN);
    }
    
    
    public static LogicalVector fromBoolean(int aSize, ILogicalVectorGetter aVectorGetter) {
        LogicalVector rVector = LogicalVector.zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    public static LogicalVector fromBoolean(ILogicalVector aVector) {
        LogicalVector rVector = LogicalVector.zeros(aVector.size());
        rVector.fill(aVector);
        return rVector;
    }
    /** Groovy stuff */
    public static LogicalVector fromBoolean(int aSize, final Closure<Boolean> aGroovyTask) {return fromBoolean(aSize, aGroovyTask::call);}
    
    public static LogicalVector fromBoolean(Iterable<Boolean> aIterable) {
        final LogicalVector.Builder rBuilder = LogicalVector.builder();
        for (Boolean tValue : aIterable) rBuilder.add(tValue);
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static LogicalVector fromBoolean(Collection<Boolean> aList) {
        LogicalVector rVector = LogicalVector.zeros(aList.size());
        rVector.fill(aList);
        return rVector;
    }
    public static LogicalVector fromBoolean(boolean[] aData) {
        LogicalVector rVector = LogicalVector.zeros(aData.length);
        rVector.fill(aData);
        return rVector;
    }
    
    
    public static IntVector fromInteger(int aSize, IIntVectorGetter aVectorGetter) {
        IntVector rVector = IntVector.zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    public static IntVector fromInteger(IIntVector aVector) {
        IntVector rVector = IntVector.zeros(aVector.size());
        rVector.fill(aVector);
        return rVector;
    }
    /** Groovy stuff */
    public static IntVector fromInteger(int aSize, final Closure<Integer> aGroovyTask) {return fromInteger(aSize, aGroovyTask::call);}
    
    public static IntVector fromInteger(Iterable<Integer> aIterable) {
        final IntVector.Builder rBuilder = IntVector.builder();
        for (Integer tValue : aIterable) rBuilder.add(tValue);
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static IntVector fromInteger(Collection<Integer> aList) {
        IntVector rVector = IntVector.zeros(aList.size());
        rVector.fill(aList);
        return rVector;
    }
    public static IntVector fromInteger(int[] aData) {
        IntVector rVector = IntVector.zeros(aData.length);
        rVector.fill(aData);
        return rVector;
    }
    
    /** IntegerVector 特有的构造 */
    public static IntVector range(int aSize) {
        IntVector rVector = IntVector.zeros(aSize);
        rVector.fill(i->i);
        return rVector;
    }
    public static IntVector range(final int aStart, int aStop) {
        IntVector rVector = IntVector.zeros(aStop-aStart);
        rVector.fill(i->i+aStart);
        return rVector;
    }
    public static IntVector range(final int aStart, int aStop, int aStep) {
        IntVector rVector = IntVector.zeros(MathEX.Code.divup(aStop-aStart, aStep));
        rVector.fill(i -> (i*aStep + aStart));
        return rVector;
    }
    
    
    public static Vector merge(IVector aBefore, IVector aAfter) {
        final int tBSize = aBefore.size();
        final int tASize = aAfter.size();
        final int tTotSize = tBSize+tASize;
        Vector rVector = Vector.zeros(tTotSize);
        rVector.subVec(0, tBSize).fill(aBefore);
        rVector.subVec(tBSize, tTotSize).fill(aAfter);
        return rVector;
    }
    public static LogicalVector merge(ILogicalVector aBefore, ILogicalVector aAfter) {
        final int tBSize = aBefore.size();
        final int tASize = aAfter.size();
        final int tTotSize = tBSize+tASize;
        LogicalVector rVector = LogicalVector.zeros(aBefore.size()+aAfter.size());
        rVector.subVec(0, tBSize).fill(aBefore);
        rVector.subVec(tBSize, tTotSize).fill(aAfter);
        return rVector;
    }
    public static IntVector merge(IIntVector aBefore, IIntVector aAfter) {
        final int tBSize = aBefore.size();
        final int tASize = aAfter.size();
        final int tTotSize = tBSize+tASize;
        IntVector rVector = IntVector.zeros(aBefore.size()+aAfter.size());
        rVector.subVec(0, tBSize).fill(aBefore);
        rVector.subVec(tBSize, tTotSize).fill(aAfter);
        return rVector;
    }
    public static ComplexVector merge(IComplexVector aBefore, IComplexVector aAfter) {
        final int tBSize = aBefore.size();
        final int tASize = aAfter.size();
        final int tTotSize = tBSize+tASize;
        ComplexVector rVector = ComplexVector.zeros(aBefore.size()+aAfter.size());
        rVector.subVec(0, tBSize).fill(aBefore);
        rVector.subVec(tBSize, tTotSize).fill(aAfter);
        return rVector;
    }
    
    /** 也提供过滤的接口，但是这里使用 vector 的写法，不涉及 {@link IHasDoubleIterator} */
    public static Vector filter(Iterable<? extends Number> aIterable, IDoubleFilter aFilter) {
        return NewCollections.filterDouble(aIterable, aFilter);
    }
    public static Vector filter(IVector aVector, IDoubleFilter aFilter) {
        return NewCollections.filterDouble(aVector, aFilter);
    }
}
