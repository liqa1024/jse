package jse.code.iterator;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import jse.atom.IAtom;
import jse.atom.IXYZ;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.math.ComplexDouble;

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
    default void forEach(@ClosureParams(value=FromString.class, options={"ComplexDouble", "double,double"}) Closure<?> aGroovyTask) {
        if (aGroovyTask.getMaximumNumberOfParameters() == 2) {
            forEach((real, imag) -> aGroovyTask.call(real, imag));
            return;
        }
        forEach(value -> aGroovyTask.call(value));
    }
}
