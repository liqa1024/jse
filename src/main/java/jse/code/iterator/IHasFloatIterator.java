package jse.code.iterator;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.functional.IFloatConsumer;

import java.util.Objects;

@FunctionalInterface
public interface IHasFloatIterator {
    IFloatIterator iterator();
    
    default Iterable<Float> iterable() {return () -> iterator().toIterator();}
    
    /** Iterable like stuffs */
    default void forEach(IFloatConsumer aCon) {
        Objects.requireNonNull(aCon);
        final IFloatIterator it = iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
    
    /** 通过 {@code Iterable<? extends Number>} 得到 */
    static IHasFloatIterator of(final Iterable<? extends Number> aIterable) {
        return new IHasFloatIterator() {
            @Override public IFloatIterator iterator() {return IFloatIterator.of(aIterable.iterator());}
            @Override public Iterable<Float> iterable() {return AbstractCollections.map(aIterable, UT.Code::floatValue);}
            @Override public void forEach(final IFloatConsumer aCon) {aIterable.forEach(v->aCon.accept(UT.Code.floatValue(v)));}
        };
    }
}
