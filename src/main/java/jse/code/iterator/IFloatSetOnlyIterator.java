package jse.code.iterator;

/**
 * 返回 float 类型的设置迭代器，用来避免外套类型
 * @author liqa
 */
public interface IFloatSetOnlyIterator {
    boolean hasNext();
    void nextOnly();
    void set(float aValue);
    /** 高性能接口，一次完成下一步和设置过程 */
    default void nextAndSet(float aValue) {
        nextOnly();
        set(aValue);
    }
    
    /** convert to Float */
    default ISetOnlyIterator<Float> toSetOnlyIterator() {
        return new ISetOnlyIterator<Float>() {
            @Override public boolean hasNext() {return IFloatSetOnlyIterator.this.hasNext();}
            @Override public void nextOnly() {IFloatSetOnlyIterator.this.nextOnly();}
            @Override public void set(Float aValue) {IFloatSetOnlyIterator.this.set(aValue);}
            @Override public void nextAndSet(Float aValue) {IFloatSetOnlyIterator.this.nextAndSet(aValue);}
        };
    }
}
