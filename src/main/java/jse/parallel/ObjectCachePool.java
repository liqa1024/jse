package jse.parallel;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

import static jse.code.CS.NO_CACHE;

/**
 * 用于作为缓存的对象池，会在内存不足时自动回收缓存
 * <p>
 * 此类线程不安全，但不同实例间线程安全
 * <p>
 * 注意为了实现简洁和性能，此类对于 return 相同对象不进行检测
 * <p>
 * 现在统一不设置缓存数目上限
 * @author liqa
 */
public class ObjectCachePool<T> implements IObjectPool<T> {
    private final Deque<SoftReference<T>> mCache;
    
    public ObjectCachePool() {
        mCache = new ArrayDeque<>();
    }
    public static <S> ObjectCachePool<S> withInitial(final Supplier<? extends S> aSupplier) {
        return new ObjectCachePool<S>() {
            @Override protected S initialValue() {return aSupplier.get();}
        };
    }
    T initialValue() {return null;}
    
    @Override public T getObject() {
        if (NO_CACHE) return initialValue();
        T tObj = null;
        while (!mCache.isEmpty()) {
            tObj = mCache.pollLast().get();
            if (tObj != null) break;
        }
        return tObj==null ? initialValue() : tObj;
    }
    @Override public void returnObject(@NotNull T aObject) {
        if (NO_CACHE) return;
        mCache.addLast(new SoftReference<>(aObject));
    }
}
