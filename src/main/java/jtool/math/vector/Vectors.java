package jtool.math.vector;


import jtool.code.collection.NewCollections;
import jtool.code.functional.IDoubleFilter;
import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IDoubleSetIterator;
import jtool.code.iterator.IHasDoubleIterator;
import jtool.math.MathEX;
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
    
    
    public static IntegerVector fromInteger(int aSize, IIntegerVectorGetter aVectorGetter) {
        IntegerVector rVector = IntegerVector.zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    public static IntegerVector fromInteger(IIntegerVector aVector) {
        IntegerVector rVector = IntegerVector.zeros(aVector.size());
        rVector.fill(aVector);
        return rVector;
    }
    /** Groovy stuff */
    public static IntegerVector fromInteger(int aSize, final Closure<Integer> aGroovyTask) {return fromInteger(aSize, aGroovyTask::call);}
    
    public static IntegerVector fromInteger(Iterable<Integer> aIterable) {
        final IntegerVector.Builder rBuilder = IntegerVector.builder();
        for (Integer tValue : aIterable) rBuilder.add(tValue);
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static IntegerVector fromInteger(Collection<Integer> aList) {
        IntegerVector rVector = IntegerVector.zeros(aList.size());
        rVector.fill(aList);
        return rVector;
    }
    public static IntegerVector fromInteger(int[] aData) {
        IntegerVector rVector = IntegerVector.zeros(aData.length);
        rVector.fill(aData);
        return rVector;
    }
    
    
    public static Vector merge(IVector aBefore, IVector aAfter) {
        Vector rVector = zeros(aBefore.size()+aAfter.size());
        // 原则上使用优化后的 refSlicer 会更快，但是优化需要的代码量较大，这里直接使用迭代器遍历，一个适中的优化效果
        final IDoubleSetIterator si = rVector.setIterator();
        final IDoubleIterator itB = aBefore.iterator();
        final IDoubleIterator itA = aAfter.iterator();
        while (itB.hasNext()) si.nextAndSet(itB.next());
        while (itA.hasNext()) si.nextAndSet(itA.next());
        return rVector;
    }
    
    /** 也提供过滤的接口，但是这里使用 vector 的写法，不涉及 {@link IHasDoubleIterator} */
    public static Vector filter(Iterable<? extends Number> aIterable, IDoubleFilter aFilter) {
        return NewCollections.filterDouble(aIterable, aFilter);
    }
    public static Vector filter(IVector aVector, IDoubleFilter aFilter) {
        return NewCollections.filterDouble(aVector, aFilter);
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
}
