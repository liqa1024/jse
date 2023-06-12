package com.jtool.code.iterator;

/**
 * 返回 double 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface IDoubleSetOnlyIterator {
    boolean hasNext();
    void nextOnly();
    void set(double e);
    /** 高性能接口，一次完成下一步和设置过程 */
    default void nextAndSet(double e) {
        nextOnly();
        set(e);
    }
    
    /** convert to Double */
    default ISetOnlyIterator<Double> toSetOnlyIterator() {
        return new ISetOnlyIterator<Double>() {
            @Override public boolean hasNext() {return IDoubleSetOnlyIterator.this.hasNext();}
            @Override public void nextOnly() {IDoubleSetOnlyIterator.this.nextOnly();}
            @Override public void set(Double aValue) {IDoubleSetOnlyIterator.this.set(aValue);}
            @Override public void nextAndSet(Double aValue) {IDoubleSetOnlyIterator.this.nextAndSet(aValue);}
        };
    }
}
