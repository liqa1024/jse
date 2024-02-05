package jse.code.iterator;

import java.util.function.Consumer;

/**
 * 返回 int 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface ILongSetIterator extends ILongIterator, ILongSetOnlyIterator {
    /** convert to Integer */
    default ISetIterator<Long> toSetIterator() {
        return new ISetIterator<Long>() {
            @Override public boolean hasNext() {return ILongSetIterator.this.hasNext();}
            @Override public Long next() {return ILongSetIterator.this.next();}
            
            @Override public void remove() {ILongSetIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Long> action) {ILongSetIterator.this.forEachRemaining(action::accept);}
            
            @Override public void nextOnly() {ILongSetIterator.this.nextOnly();}
            @Override public void set(Long aValue) {ILongSetIterator.this.set(aValue);}
            @Override public void nextAndSet(Long aValue) {ILongSetIterator.this.nextAndSet(aValue);}
        };
    }
}
