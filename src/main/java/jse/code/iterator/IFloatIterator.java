package jse.code.iterator;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.functional.IFloatConsumer;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 返回 float 类型的迭代器，用来避免外套类型
 * @author liqa
 */
public interface IFloatIterator {
    boolean hasNext();
    float next();
    
    /** Iterator default stuffs */
    default void remove() {throw new UnsupportedOperationException("remove");}
    default void forEachRemaining(IFloatConsumer aCon) {
        Objects.requireNonNull(aCon);
        while (hasNext()) aCon.accept(next());
    }
    
    /** convert to float */
    default Iterator<Float> toIterator() {
        return new Iterator<Float>() {
            @Override public boolean hasNext() {return IFloatIterator.this.hasNext();}
            @Override public Float next() {return IFloatIterator.this.next();}
            
            @Override public void remove() {IFloatIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Float> action) {IFloatIterator.this.forEachRemaining(action::accept);}
        };
    }
    
    default IDoubleIterator asDouble() {
        return new IDoubleIterator() {
            @Override public boolean hasNext() {return IFloatIterator.this.hasNext();}
            @Override public double next() {return IFloatIterator.this.next();}
        };
    }
    
    /** 通过 {@code Iterator<? extends Number>} 得到 */
    static IFloatIterator of(final Iterator<? extends Number> aIterator) {
        return new IFloatIterator() {
            @Override public boolean hasNext() {return aIterator.hasNext();}
            @Override public float next() {return UT.Code.floatValue(aIterator.next());}
            @Override public void remove() {aIterator.remove();}
            @Override public void forEachRemaining(IFloatConsumer aCon) {aIterator.forEachRemaining(v->aCon.accept(UT.Code.floatValue(v)));}
            @Override public Iterator<Float> toIterator() {return AbstractCollections.map(aIterator, UT.Code::floatValue);}
        };
    }
}
