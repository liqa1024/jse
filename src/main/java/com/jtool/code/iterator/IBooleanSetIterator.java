package com.jtool.code.iterator;

import java.util.function.Consumer;

/**
 * 返回 double 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface IBooleanSetIterator extends IBooleanIterator, IBooleanSetOnlyIterator {
    default void nextOnly() {next();}
    
    /** convert to Double */
    default ISetIterator<Boolean> toSetIterator() {
        return new ISetIterator<Boolean>() {
            @Override public boolean hasNext() {return IBooleanSetIterator.this.hasNext();}
            @Override public Boolean next() {return IBooleanSetIterator.this.next();}
            
            @Override public void remove() {IBooleanSetIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Boolean> action) {IBooleanSetIterator.this.forEachRemaining(action);}
            
            @Override public void nextOnly() {IBooleanSetIterator.this.nextOnly();}
            @Override public void set(Boolean aValue) {IBooleanSetIterator.this.set(aValue);}
            @Override public void nextAndSet(Boolean aValue) {IBooleanSetIterator.this.nextAndSet(aValue);}
        };
    }
}
