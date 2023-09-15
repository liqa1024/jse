package com.jtool.code.iterator;

import com.jtool.atom.IAtom;
import com.jtool.atom.IXYZ;
import com.jtool.code.functional.IConsumer1;
import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleConsumer2;
import com.jtool.math.ComplexDouble;
import groovy.lang.Closure;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 支持将下一步和获取数据分开，然后分别获取实部和虚部，用来减少外套类型的使用
 * <p>
 * 和 {@link IXYZ} 或者 {@link IAtom} 部分的用法思路不同，
 * 这里直接返回 {@link ComplexDouble} 让其和基本类型一样的使用，不会有引用的属性在里面
 * @author liqa
 */
public interface IComplexDoubleIterator {
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
    default void forEachRemaining(IConsumer1<? super ComplexDouble> aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) aCon.run(next());
    }
    default void forEachRemaining(IDoubleConsumer2 aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) {
            nextOnly();
            aCon.run(real(), imag());
        }
    }
    default void forEachRemainingReal(IDoubleConsumer1 aRealCon) {
        Objects.requireNonNull(aRealCon);
        while (hasNext()) {
            nextOnly();
            aRealCon.run(real());
        }
    }
    default void forEachRemainingImag(IDoubleConsumer1 aImagCon) {
        Objects.requireNonNull(aImagCon);
        while (hasNext()) {
            nextOnly();
            aImagCon.run(imag());
        }
    }
    /** Groovy stuffs */
    default void forEachRemaining(Closure<?> aGroovyTask) {
        int tN = aGroovyTask.getMaximumNumberOfParameters();
        switch (tN) {
        case 1: forEachRemaining(value -> aGroovyTask.call(value));
        case 2: forEachRemaining((real, imag) -> aGroovyTask.call(real, imag));
        default: throw new IllegalArgumentException("Parameters Number of forEachRemaining in IComplexDoubleIterator Must be 1 or 2");
        }
    }
    
    /** 同样采用 toIterator() 的方法转为 {@link Iterator} 而不是继承 */
    default Iterator<ComplexDouble> toIterator() {
        return new Iterator<ComplexDouble>() {
            @Override public boolean hasNext() {return IComplexDoubleIterator.this.hasNext();}
            @Override public ComplexDouble next() {return IComplexDoubleIterator.this.next();}
            
            @Override public void remove() {IComplexDoubleIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super ComplexDouble> action) {IComplexDoubleIterator.this.forEachRemaining(action::accept);}
        };
    }
}
