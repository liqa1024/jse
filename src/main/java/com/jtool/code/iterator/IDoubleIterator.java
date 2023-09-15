package com.jtool.code.iterator;

import com.jtool.code.functional.IDoubleConsumer1;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * 返回 double 类型的迭代器，用来避免外套类型
 * @author liqa
 */
public interface IDoubleIterator {
    boolean hasNext();
    double next();
    
    /** Iterator default stuffs */
    default void remove() {throw new UnsupportedOperationException("remove");}
    default void forEachRemaining(IDoubleConsumer1 action) {
        Objects.requireNonNull(action);
        while (hasNext()) action.run(next());
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
}
