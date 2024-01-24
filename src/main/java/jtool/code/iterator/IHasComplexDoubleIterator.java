package jtool.code.iterator;

import groovy.lang.Closure;
import jtool.atom.IAtom;
import jtool.atom.IXYZ;
import jtool.code.functional.IDoubleBinaryConsumer;
import jtool.math.ComplexDouble;

import java.util.Objects;
import java.util.function.Consumer;

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
    default void forEach(IDoubleBinaryConsumer aCon) {
        Objects.requireNonNull(aCon);
        final IComplexDoubleIterator it = iterator();
        while (it.hasNext()) {
            it.nextOnly();
            aCon.accept(it.real(), it.imag());
        }
    }
    default void forEach(Consumer<? super ComplexDouble> aCon) {
        Objects.requireNonNull(aCon);
        final IComplexDoubleIterator it = iterator();
        while (it.hasNext()) aCon.accept(it.next());
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
