package jse.code.iterator;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;


/**
 * 返回 int 类型的迭代器，用来避免外套类型
 * @author liqa
 */
public interface ILongIterator {
    boolean hasNext();
    long next();
    
    /** Iterator default stuffs */
    default void remove() {throw new UnsupportedOperationException("remove");}
    default void forEachRemaining(LongConsumer aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) aCon.accept(next());
    }
    
    /** convert to Integer */
    default Iterator<Long> toIterator() {
        return new Iterator<Long>() {
            @Override public boolean hasNext() {return ILongIterator.this.hasNext();}
            @Override public Long next() {return ILongIterator.this.next();}
            
            @Override public void remove() {ILongIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Long> action) {ILongIterator.this.forEachRemaining(action::accept);}
        };
    }
    
    default IDoubleIterator asDouble() {
        return new IDoubleIterator() {
            @Override public boolean hasNext() {return ILongIterator.this.hasNext();}
            @Override public double next() {return ILongIterator.this.next();}
        };
    }
}
