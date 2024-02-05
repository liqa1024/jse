package jse.parallel;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

import static jse.code.CS.NO_CACHE;

/**
 * 线程独立对象池的缓存形式，会在内存不足时自动回收缓存
 * <p>
 * 此类线程安全，借助 {@link ThreadLocal} 来实现高性能的多个线程同时访问同一个实例
 * <p>
 * 注意为了实现简洁和性能，此类对于 return 相同对象不进行检测
 * <p>
 * 现在统一不设置缓存数目上限
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class ThreadLocalObjectCachePool<T> implements IObjectPool<T> {
    private final ThreadLocal<Deque<SoftReference<T>>> mCache;
    
    public ThreadLocalObjectCachePool() {
        mCache = ThreadLocal.withInitial(ArrayDeque::new);
    }
    public static <S> ThreadLocalObjectCachePool<S> withInitial(final Supplier<? extends S> aSupplier) {
        return new ThreadLocalObjectCachePool<S>() {
            @Override protected S initialValue() {return aSupplier.get();}
        };
    }
    T initialValue() {return null;}
    
    
    @Override public T getObject() {
        if (NO_CACHE) return initialValue();
        Deque<SoftReference<T>> tCache = mCache.get();
        T tObj = null;
        while (!tCache.isEmpty()) {
            tObj = tCache.pollLast().get();
            if (tObj != null) break;
        }
        return tObj==null ? initialValue() : tObj;
    }
    @Override public void returnObject(@NotNull T aObject) {
        if (NO_CACHE) return;
        mCache.get().addLast(new SoftReference<>(aObject));
    }
}
