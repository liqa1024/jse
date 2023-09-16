package com.jtool.code.iterator;

import com.jtool.atom.IAtom;
import com.jtool.atom.IXYZ;
import com.jtool.code.functional.IConsumer1;
import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleConsumer2;
import com.jtool.math.ComplexDouble;
import groovy.lang.Closure;

import java.util.Objects;

/**
 * 和 {@link IXYZ} 或者 {@link IAtom} 部分的用法思路不同，
 * 这里直接返回 {@link ComplexDouble} 让其和基本类型一样的使用，不会有引用的属性在里面
 * @author liqa
 */
@FunctionalInterface
public interface IHasComplexDoubleIterator {
    IComplexDoubleIterator iterator();
    
    /** 同样采用 iterable() 的方法转为 {@link Iterable} 而不是继承 */
    default Iterable<ComplexDouble> iterable() {return () -> iterator().toIterator();}
    
    /** Iterable like stuffs */
    default void forEach(IDoubleConsumer2 aCon) {
        Objects.requireNonNull(aCon);
        final IComplexDoubleIterator it = iterator();
        while (it.hasNext()) {
            it.nextOnly();
            aCon.run(it.real(), it.imag());
        }
    }
    default void forEach(IConsumer1<? super ComplexDouble> aCon) {
        Objects.requireNonNull(aCon);
        final IComplexDoubleIterator it = iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
    default void forEachReal(IDoubleConsumer1 aRealCon) {
        Objects.requireNonNull(aRealCon);
        final IComplexDoubleIterator it = iterator();
        while (it.hasNext()) {
            it.nextOnly();
            aRealCon.run(it.real());
        }
    }
    default void forEachImag(IDoubleConsumer1 aImagCon) {
        Objects.requireNonNull(aImagCon);
        final IComplexDoubleIterator it = iterator();
        while (it.hasNext()) {
            it.nextOnly();
            aImagCon.run(it.imag());
        }
    }
    /** Groovy stuffs */
    default void forEach(Closure<?> aGroovyTask) {
        int tN = aGroovyTask.getMaximumNumberOfParameters();
        switch (tN) {
        case 1: {forEach(value -> aGroovyTask.call(value)); return;}
        case 2: {forEach((real, imag) -> aGroovyTask.call(real, imag)); return;}
        default: throw new IllegalArgumentException("Parameters Number of forEach in IHasComplexDoubleIterator Must be 1 or 2");
        }
    }
}
