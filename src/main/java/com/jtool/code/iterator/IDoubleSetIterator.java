package com.jtool.code.iterator;

import java.util.function.Consumer;

/**
 * 返回 double 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface IDoubleSetIterator extends IDoubleIterator, IDoubleSetOnlyIterator {
    default void nextOnly() {next();}
    /** 高性能接口，一次完成下一步和设置过程，并且会返回旧的值 */
    default double getNextAndSet(double aValue) {
        double oValue = next();
        set(aValue);
        return oValue;
    }
    
    /** convert to Double */
    default ISetIterator<Double> toSetIterator() {
        return new ISetIterator<Double>() {
            @Override public boolean hasNext() {return IDoubleSetIterator.this.hasNext();}
            @Override public Double next() {return IDoubleSetIterator.this.next();}
            
            @Override public void remove() {IDoubleSetIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super Double> action) {IDoubleSetIterator.this.forEachRemaining(action);}
            
            @Override public void nextOnly() {IDoubleSetIterator.this.nextOnly();}
            @Override public void set(Double aValue) {IDoubleSetIterator.this.set(aValue);}
            @Override public void nextAndSet(Double aValue) {IDoubleSetIterator.this.nextAndSet(aValue);}
            @Override public Double getNextAndSet(Double aValue) {return IDoubleSetIterator.this.getNextAndSet(aValue);}
        };
    }
}
