package jse.code.iterator;

import java.util.function.Consumer;

/**
 * 返回 int 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface IIntSetIterator extends IIntIterator, IIntSetOnlyIterator {
    /** convert to Integer */
    default ISetIterator<Integer> toSetIterator() {
        return new ISetIterator<Integer>() {
            @Override public boolean hasNext() {return IIntSetIterator.this.hasNext();}
            @Override public Integer next() {return IIntSetIterator.this.next();}
            
            @Override public void remove() {IIntSetIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Integer> action) {IIntSetIterator.this.forEachRemaining(action::accept);}
            
            @Override public void nextOnly() {IIntSetIterator.this.nextOnly();}
            @Override public void set(Integer aValue) {IIntSetIterator.this.set(aValue);}
            @Override public void nextAndSet(Integer aValue) {IIntSetIterator.this.nextAndSet(aValue);}
        };
    }
}
