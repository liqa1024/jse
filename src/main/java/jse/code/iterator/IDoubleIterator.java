package jse.code.iterator;

import jse.code.UT;
import jse.code.collection.AbstractCollections;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

import java.util.function.DoubleConsumer;

/**
 * 返回 double 类型的迭代器，用来避免外套类型
 * @author liqa
 */
public interface IDoubleIterator {
    boolean hasNext();
    double next();
    
    /** Iterator default stuffs */
    default void remove() {throw new UnsupportedOperationException("remove");}
    default void forEachRemaining(DoubleConsumer aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) aCon.accept(next());
    }
    
    /** convert to Double */
    default Iterator<Double> toIterator() {
        return new Iterator<Double>() {
            @Override public boolean hasNext() {return IDoubleIterator.this.hasNext();}
            @Override public Double next() {return IDoubleIterator.this.next();}
            
            @Override public void remove() {IDoubleIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Double> action) {IDoubleIterator.this.forEachRemaining(action::accept);}
        };
    }
    
    /** 通过 {@code Iterator<? extends Number>} 得到 */
    static IDoubleIterator of(final Iterator<? extends Number> aIterator) {
        return new IDoubleIterator() {
            @Override public boolean hasNext() {return aIterator.hasNext();}
            @Override public double next() {return UT.Code.doubleValue(aIterator.next());}
            @Override public void remove() {aIterator.remove();}
            @Override public void forEachRemaining(DoubleConsumer aCon) {aIterator.forEachRemaining(v->aCon.accept(UT.Code.doubleValue(v)));}
            @Override public Iterator<Double> toIterator() {return AbstractCollections.map(aIterator, UT.Code::doubleValue);}
        };
    }
}
