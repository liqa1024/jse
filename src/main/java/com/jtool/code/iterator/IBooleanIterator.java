package com.jtool.code.iterator;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * 返回 double 类型的迭代器，用来避免外套类型
 * @author liqa
 */
public interface IBooleanIterator {
    boolean hasNext();
    boolean next();
    
    /** Iterator default stuffs */
    default void remove() {throw new UnsupportedOperationException("remove");}
    default void forEachRemaining(Consumer<? super Boolean> action) {
        Objects.requireNonNull(action);
        while (hasNext()) action.accept(next());
    }
    
    /** convert to Double */
    default Iterator<Boolean> toIterator() {
        return new Iterator<Boolean>() {
            @Override public boolean hasNext() {return IBooleanIterator.this.hasNext();}
            @Override public Boolean next() {return IBooleanIterator.this.next();}
            
            @Override public void remove() {IBooleanIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Boolean> action) {IBooleanIterator.this.forEachRemaining(action);}
        };
    }
}
