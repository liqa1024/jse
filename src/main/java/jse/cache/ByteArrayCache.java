package jse.cache;

import jse.code.collection.IListGetter;
import jse.code.collection.IListSetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static jse.code.CS.ZL_BYTE;
import static jse.code.Conf.NO_CACHE;

/**
 * 专门针对 {@code byte[]} 的全局线程独立缓存，
 * 返回大于等于要求长度的 {@code byte[]}
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class ByteArrayCache {
    private ByteArrayCache() {}
    
    private static final ThreadLocal<NavigableMap<Integer, IObjectPool<byte[]>>> CACHE = ThreadLocal.withInitial(TreeMap::new);
    
    /**
     * 归还数组，会根据数组长度自动选择缓存存放的位置
     * @param aArray 需要归还的数组
     * @author liqa
     */
    public static void returnArray(byte @NotNull[] aArray) {
        if (NO_CACHE) return;
        final int tSizeKey = aArray.length;
        if (tSizeKey == 0) return;
        CACHE.get().computeIfAbsent(tSizeKey, key -> new ObjectCachePool<>()).returnObject(aArray);
    }
    
    static boolean entryInvalid(@Nullable Map.Entry<Integer, ?> aEntry, int aSize) {
        if (aEntry == null) return true;
        int tSize = aEntry.getKey();
        return (tSize > 16) && (tSize+tSize > aSize+aSize+aSize);
    }
    
    
    /**
     * @param aMinSize 要求的最小长度
     * @return 大于等于要求长度的 {@code byte[]}，并且所有成员都为 0
     * @author liqa
     */
    public static byte @NotNull[] getZeros(int aMinSize) {
        if (aMinSize <= 0) return ZL_BYTE;
        if (NO_CACHE) return new byte[aMinSize];
        Map.Entry<Integer, IObjectPool<byte[]>> tEntry = CACHE.get().ceilingEntry(aMinSize);
        if (entryInvalid(tEntry, aMinSize)) return new byte[aMinSize];
        IObjectPool<byte[]> tPool = tEntry.getValue();
        // 如果是缓存值需要手动设置为 0.0
        byte @Nullable[] tOut = tPool.getObject();
        if (tOut == null) return new byte[aMinSize];
        Arrays.fill(tOut, (byte)0);
        return tOut;
    }
    
    /**
     * @param aMinSize 要求的最小长度
     * @return 大于等于要求长度的 {@code byte[]}，成员为任意值
     * @author liqa
     */
    public static byte @NotNull[] getArray(int aMinSize) {
        if (aMinSize <= 0) return ZL_BYTE;
        if (NO_CACHE) return new byte[aMinSize];
        Map.Entry<Integer, IObjectPool<byte[]>> tEntry = CACHE.get().ceilingEntry(aMinSize);
        if (entryInvalid(tEntry, aMinSize)) return new byte[aMinSize];
        IObjectPool<byte[]> tPool = tEntry.getValue();
        if (tPool == null) return new byte[aMinSize];
        byte @Nullable[] tOut = tPool.getObject();
        if (tOut == null) return new byte[aMinSize];
        return tOut;
    }
    
    
    
    /** 批量操作的接口，约定所有数组都等长 */
    public static void returnArrayFrom(int aMultiple, IListGetter<byte @NotNull[]> aArrayGetter) {
        if (NO_CACHE) return;
        if (aMultiple <= 0) return;
        byte[] tFirst = aArrayGetter.get(0);
        final int tSizeKey = tFirst.length;
        if (tSizeKey == 0) return;
        IObjectPool<byte[]> tPool = CACHE.get().computeIfAbsent(tSizeKey, key -> new ObjectCachePool<>());
        tPool.returnObject(tFirst);
        for (int i = 1; i < aMultiple; ++i) tPool.returnObject(aArrayGetter.get(i));
    }
    
    public static void getZerosTo(int aMinSize, int aMultiple, IListSetter<byte @NotNull[]> aZerosConsumer) {
        if (aMultiple <= 0) return;
        if (aMinSize <= 0) {
            for (int i = 0; i < aMultiple; ++i) aZerosConsumer.set(i, ZL_BYTE);
            return;
        }
        if (NO_CACHE) {
            for (int i = 0; i < aMultiple; ++i) aZerosConsumer.set(i, new byte[aMinSize]);
            return;
        }
        Map.Entry<Integer, IObjectPool<byte[]>> tEntry = CACHE.get().ceilingEntry(aMinSize);
        if (entryInvalid(tEntry, aMinSize)) {
            for (int i = 0; i < aMultiple; ++i) aZerosConsumer.set(i, new byte[aMinSize]);
            return;
        }
        IObjectPool<byte[]> tPool = tEntry.getValue();
        // 如果是缓存值需要手动设置为 0
        boolean tNoCache = false;
        for (int i = 0; i < aMultiple; ++i) {
            if (tNoCache) {
                aZerosConsumer.set(i, new byte[aMinSize]);
                continue;
            }
            byte @Nullable[] subOut = tPool.getObject();
            if (subOut == null) {
                tNoCache = true;
                subOut = new byte[aMinSize];
            } else {
                Arrays.fill(subOut, (byte)0);
            }
            aZerosConsumer.set(i, subOut);
        }
    }
    
    public static void getArrayTo(int aMinSize, int aMultiple, IListSetter<byte @NotNull[]> aArrayConsumer) {
        if (aMultiple <= 0) return;
        if (aMinSize <= 0) {
            for (int i = 0; i < aMultiple; ++i) aArrayConsumer.set(i, ZL_BYTE);
            return;
        }
        if (NO_CACHE) {
            for (int i = 0; i < aMultiple; ++i) aArrayConsumer.set(i, new byte[aMinSize]);
            return;
        }
        Map.Entry<Integer, IObjectPool<byte[]>> tEntry = CACHE.get().ceilingEntry(aMinSize);
        if (entryInvalid(tEntry, aMinSize)) {
            for (int i = 0; i < aMultiple; ++i) aArrayConsumer.set(i, new byte[aMinSize]);
            return;
        }
        IObjectPool<byte[]> tPool = tEntry.getValue();
        boolean tNoCache = false;
        for (int i = 0; i < aMultiple; ++i) {
            if (tNoCache) {
                aArrayConsumer.set(i, new byte[aMinSize]);
                continue;
            }
            byte @Nullable[] subOut = tPool.getObject();
            if (subOut == null) {
                tNoCache = true;
                subOut = new byte[aMinSize];
            }
            aArrayConsumer.set(i, subOut);
        }
    }
}
