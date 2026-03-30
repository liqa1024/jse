package jse.math.vector;


import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import jep.NDArray;
import jse.code.UT;
import jse.code.collection.NewCollections;
import jse.code.functional.IDoubleFilter;
import jse.code.iterator.IDoubleSetIterator;
import jse.code.iterator.IHasDoubleIterator;
import jse.math.MathEX;
import groovy.lang.Closure;

import java.util.Arrays;
import java.util.Collection;

/**
 * 方便创建向量的工具类，默认获取 {@link Vector}
 * @author liqa
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
    
    /**
     * 从 numpy 的 {@link NDArray} 创建向量，自动检测类型
     * <p>
     * 和下面 {@code from} 相关方法不同的是，这里在 java
     * 侧不会进行值拷贝，考虑到 {@link NDArray}
     * 实际总是会经过一次值拷贝，因此在 python
     * 中使用不会有引用的问题
     *
     * @param aNDArray 输入的 numpy 数组
     * @param aUnsignedWarning 是否开启检测无符号的警告，如果这不是问题则可以关闭，默认为 {@code true}
     * @return 从 {@link NDArray} 创建的向量
     * @throws UnsupportedOperationException 当输入的 {@link NDArray}
     * 类型还不存在对应的 jse 向量类型
     * @throws IllegalArgumentException 当输入的 {@link NDArray} 不是一维的
     */
    public static Object fromNumpy(NDArray<?> aNDArray, boolean aUnsignedWarning) {
        int[] tDims = aNDArray.getDimensions();
        if (tDims.length != 1) throw new IllegalArgumentException("Invalid numpy shape: " + Arrays.toString(tDims));
        if (aUnsignedWarning && aNDArray.isUnsigned()) {
            UT.Code.warning("Input numpy is unsigned, which is not supported in jse, so the original signed value will be directly obtained");
        }
        final int tSize = tDims[0];
        Object tData = aNDArray.getData();
        if (tData instanceof double[]) {
            return new Vector(tSize, (double[])tData);
        } else
        if (tData instanceof float[]) {
            return new FloatVector(tSize, (float[])tData);
        } else
        if (tData instanceof int[]) {
            return new IntVector(tSize, (int[])tData);
        } else
        if (tData instanceof long[]) {
            return new LongVector(tSize, (long[])tData);
        } else
        if (tData instanceof boolean[]) {
            return new LogicalVector(tSize, (boolean[])tData);
        } else {
            throw new UnsupportedOperationException("Invalid numpy dtype: " + tData.getClass().getName());
        }
    }
    /**
     * 从 numpy 的 {@link NDArray} 创建向量，自动检测类型
     * <p>
     * 和下面 {@code from} 相关方法不同的是，这里在 java
     * 侧不会进行值拷贝，考虑到 {@link NDArray}
     * 实际总是会经过一次值拷贝，因此在 python
     * 中使用不会有引用的问题
     *
     * @param aNDArray 输入的 numpy 数组
     * @return 从 {@link NDArray} 创建的向量
     * @throws UnsupportedOperationException 当输入的 {@link NDArray}
     * 类型还不存在对应的 jse 向量类型
     * @throws IllegalArgumentException 当输入的 {@link NDArray} 不是一维的
     */
    public static Object fromNumpy(NDArray<?> aNDArray) {return fromNumpy(aNDArray, true);}
    
    public static Vector from(int aSize, IVectorGetter aVectorGetter) {return fromDouble(aSize, aVectorGetter);}
    public static Vector from(IVector aVector) {return fromDouble(aVector);}
    public static Vector fromDouble(int aSize, IVectorGetter aVectorGetter) {
        Vector rVector = zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    public static Vector fromDouble(IVector aVector) {
        Vector rVector = zeros(aVector.size());
        rVector.fill(aVector);
        return rVector;
    }
    /// Groovy stuff
    public static Vector from(int aSize, @ClosureParams(value=SimpleType.class, options="int") final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aSize, aGroovyTask);
    }
    public static Vector fromDouble(int aSize, @ClosureParams(value=SimpleType.class, options="int") final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aSize, i -> UT.Code.doubleValue(aGroovyTask.call(i)));
    }
    
    public static Vector from(Iterable<? extends Number> aIterable) {return fromDouble(aIterable);}
    public static Vector from(Collection<? extends Number> aList) {return fromDouble(aList);}
    public static Vector from(double[] aData) {return fromDouble(aData);}
    public static Vector fromDouble(Iterable<? extends Number> aIterable) {
        final Vector.Builder rBuilder = Vector.builder();
        for (Number tValue : aIterable) rBuilder.add(UT.Code.doubleValue(tValue));
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static Vector fromDouble(Collection<? extends Number> aList) {
        Vector rVector = zeros(aList.size());
        rVector.fill(aList);
        return rVector;
    }
    public static Vector fromDouble(double[] aData) {
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
    public static LogicalVector fromBoolean(int aSize, @ClosureParams(value=SimpleType.class, options="int") final Closure<Boolean> aGroovyTask) {
        return fromBoolean(aSize, aGroovyTask::call);
    }
    
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
    
    
    public static IntVector fromInt(int aSize, IIntVectorGetter aVectorGetter) {
        IntVector rVector = IntVector.zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    public static IntVector fromInt(IIntVector aVector) {
        IntVector rVector = IntVector.zeros(aVector.size());
        rVector.fill(aVector);
        return rVector;
    }
    /** Groovy stuff */
    public static IntVector fromInt(int aSize, @ClosureParams(value=SimpleType.class, options="int") final Closure<Integer> aGroovyTask) {
        return fromInt(aSize, aGroovyTask::call);
    }
    
    public static IntVector fromInt(Iterable<Integer> aIterable) {
        final IntVector.Builder rBuilder = IntVector.builder();
        for (Integer tValue : aIterable) rBuilder.add(tValue);
        rBuilder.trimToSize();
        return rBuilder.build();
    }
    public static IntVector fromInt(Collection<Integer> aList) {
        IntVector rVector = IntVector.zeros(aList.size());
        rVector.fill(aList);
        return rVector;
    }
    public static IntVector fromInt(int[] aData) {
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
