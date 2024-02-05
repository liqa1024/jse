package jse.code.iterator;

import jse.math.IComplexDouble;
import groovy.lang.Closure;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

@FunctionalInterface
public interface IHasComplexDoubleSetOnlyIterator {
    IComplexDoubleSetOnlyIterator setIterator();
    
    /** Iterable like stuffs */
    default void assign(Supplier<? extends IComplexDouble> aSup) {
        Objects.requireNonNull(aSup);
        final IComplexDoubleSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    default void assign(DoubleSupplier aSup) {
        Objects.requireNonNull(aSup);
        final IComplexDoubleSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsDouble());
    }
    /** Groovy stuffs */
    default void assign(Closure<?> aGroovyTask) {
        final IComplexDoubleSetOnlyIterator si = setIterator();
        while (si.hasNext()) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = aGroovyTask.call();
            if (tObj instanceof IComplexDouble) si.nextAndSet((IComplexDouble)tObj);
            else if (tObj instanceof Number) si.nextAndSet(((Number)tObj).doubleValue());
        }
    }
}
