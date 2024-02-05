package jse.code.iterator;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;


/**
 * 返回 int 类型的迭代器，用来避免外套类型
 * @author liqa
 */
public interface IIntIterator {
    boolean hasNext();
    int next();
    
    /** Iterator default stuffs */
    default void remove() {throw new UnsupportedOperationException("remove");}
    default void forEachRemaining(IntConsumer aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) aCon.accept(next());
    }
    
    /** convert to Integer */
    default Iterator<Integer> toIterator() {
        return new Iterator<Integer>() {
            @Override public boolean hasNext() {return IIntIterator.this.hasNext();}
            @Override public Integer next() {return IIntIterator.this.next();}
            
            @Override public void remove() {IIntIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Integer> action) {IIntIterator.this.forEachRemaining(action::accept);}
        };
    }
    
    default IDoubleIterator asDouble() {
        return new IDoubleIterator() {
            @Override public boolean hasNext() {return IIntIterator.this.hasNext();}
            @Override public double next() {return IIntIterator.this.next();}
        };
    }
}
