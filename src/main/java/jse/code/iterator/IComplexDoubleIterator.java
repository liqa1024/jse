package jse.code.iterator;

import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import jse.atom.IAtom;
import jse.atom.IXYZ;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import groovy.lang.Closure;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.*;

/**
 * 支持将下一步和获取数据分开，然后分别获取实部和虚部，用来减少外套类型的使用
 * <p>
 * 和 {@link IXYZ} 或者 {@link IAtom} 部分的用法思路不同，
 * 这里直接返回 {@link ComplexDouble} 让其和基本类型一样的使用，不会有引用的属性在里面
 * <p>
 * 继承 {@link IComplexDouble} 来方便直接使用复数运算而无需创建对象
 * @author liqa
 */
public interface IComplexDoubleIterator extends IComplexDouble {
    void nextOnly();
    double real();
    double imag();
    
    /** Iterator stuffs */
    boolean hasNext();
    default ComplexDouble next() {
        nextOnly();
        return new ComplexDouble(real(), imag());
    }
    /** Iterator default stuffs */
    default void remove() {throw new UnsupportedOperationException("remove");}
    default void forEachRemaining(Consumer<? super ComplexDouble> aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) aCon.accept(next());
    }
    default void forEachRemaining(IDoubleBinaryConsumer aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) {
            nextOnly();
            aCon.accept(real(), imag());
        }
    }
    default void forEachRemainingReal(DoubleConsumer aRealCon) {
        Objects.requireNonNull(aRealCon);
        while (hasNext()) {
            nextOnly();
            aRealCon.accept(real());
        }
    }
    default void forEachRemainingImag(DoubleConsumer aImagCon) {
        Objects.requireNonNull(aImagCon);
        while (hasNext()) {
            nextOnly();
            aImagCon.accept(imag());
        }
    }
    /** Groovy stuffs */
    default void forEachRemaining(@ClosureParams(value=FromString.class, options={"ComplexDouble", "double,double"}) Closure<?> aGroovyTask) {
        if (aGroovyTask.getMaximumNumberOfParameters() == 2) {
            forEachRemaining((real, imag) -> aGroovyTask.call(real, imag));
            return;
        }
        forEachRemaining(value -> aGroovyTask.call(value));
    }
    
    /** 同样采用 toIterator() 的方法转为 {@link Iterator} 而不是继承 */
    default Iterator<ComplexDouble> toIterator() {
        return new Iterator<ComplexDouble>() {
            @Override public boolean hasNext() {return IComplexDoubleIterator.this.hasNext();}
            @Override public ComplexDouble next() {return IComplexDoubleIterator.this.next();}
            
            @Override public void remove() {IComplexDoubleIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super ComplexDouble> action) {IComplexDoubleIterator.this.forEachRemaining(action);}
        };
    }
}
