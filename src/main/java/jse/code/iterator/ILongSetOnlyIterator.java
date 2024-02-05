package jse.code.iterator;

/**
 * 返回 int 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface ILongSetOnlyIterator {
    boolean hasNext();
    void nextOnly();
    void set(long aValue);
    /** 高性能接口，一次完成下一步和设置过程 */
    default void nextAndSet(long aValue) {
        nextOnly();
        set(aValue);
    }
    
    /** convert to Integer */
    default ISetOnlyIterator<Long> toSetOnlyIterator() {
        return new ISetOnlyIterator<Long>() {
            @Override public boolean hasNext() {return ILongSetOnlyIterator.this.hasNext();}
            @Override public void nextOnly() {ILongSetOnlyIterator.this.nextOnly();}
            @Override public void set(Long aValue) {ILongSetOnlyIterator.this.set(aValue);}
            @Override public void nextAndSet(Long aValue) {ILongSetOnlyIterator.this.nextAndSet(aValue);}
        };
    }
}
