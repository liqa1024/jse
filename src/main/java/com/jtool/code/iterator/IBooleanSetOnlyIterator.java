package com.jtool.code.iterator;

/**
 * 返回 double 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface IBooleanSetOnlyIterator {
    boolean hasNext();
    void nextOnly();
    void set(boolean aValue);
    /** 高性能接口，一次完成下一步和设置过程 */
    default void nextAndSet(boolean aValue) {
        nextOnly();
        set(aValue);
    }
    
    /** convert to Boolean */
    default ISetOnlyIterator<Boolean> toSetOnlyIterator() {
        return new ISetOnlyIterator<Boolean>() {
            @Override public boolean hasNext() {return IBooleanSetOnlyIterator.this.hasNext();}
            @Override public void nextOnly() {IBooleanSetOnlyIterator.this.nextOnly();}
            @Override public void set(Boolean aValue) {IBooleanSetOnlyIterator.this.set(aValue);}
            @Override public void nextAndSet(Boolean aValue) {IBooleanSetOnlyIterator.this.nextAndSet(aValue);}
        };
    }
}
