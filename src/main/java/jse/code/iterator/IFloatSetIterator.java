package jse.code.iterator;

import java.util.function.Consumer;

/**
 * 返回 float 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface IFloatSetIterator extends IFloatIterator, IFloatSetOnlyIterator {
    /** convert to Double */
    default ISetIterator<Float> toSetIterator() {
        return new ISetIterator<Float>() {
            @Override public boolean hasNext() {return IFloatSetIterator.this.hasNext();}
            @Override public Float next() {return IFloatSetIterator.this.next();}
            
            @Override public void remove() {IFloatSetIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Float> action) {IFloatSetIterator.this.forEachRemaining(action::accept);}
            
            @Override public void nextOnly() {IFloatSetIterator.this.nextOnly();}
            @Override public void set(Float aValue) {IFloatSetIterator.this.set(aValue);}
            @Override public void nextAndSet(Float aValue) {IFloatSetIterator.this.nextAndSet(aValue);}
        };
    }
}
