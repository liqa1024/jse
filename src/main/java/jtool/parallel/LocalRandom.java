package jtool.parallel;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 完全使用局部变量的随机数生成器，应该可以改善并行下的性能问题；
 * 此类线程不安全，但不同实例间线程安全；
 * 所有实现均参考 {@link java.util.Random}
 */
public class LocalRandom {
    private static final String BAD_BOUND = "bound must be positive";
    
    private long seed;
    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;
    private static final double DOUBLE_UNIT = 0x1.0p-53; // 1.0 / (1L << 53)
    
    public LocalRandom() {
        this(seedUniquifier() ^ System.nanoTime());
    }
    
    private static long seedUniquifier() {
        // L'Ecuyer, "Tables of Linear Congruential Generators of
        // Different Sizes and Good Lattice Structure", 1999
        for (;;) {
            long current = seedUniquifier.get();
            long next = current * 1181783497276652981L;
            if (seedUniquifier.compareAndSet(current, next))
                return next;
        }
    }
    private static final AtomicLong seedUniquifier = new AtomicLong(8682522807148012L);
    
    public LocalRandom(long seed) {
        this.seed = 0;
        setSeed(seed);
    }
    private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;
    }
    
    
    public void setSeed(long seed) {
        this.seed = initialScramble(seed);
        haveNextNextGaussian = false;
    }
    
    protected int next(int bits) {
        // 现在不再需要 CAS 来更新 seed
        this.seed = (this.seed * multiplier + addend) & mask;
        return (int)(this.seed >>> (48 - bits));
    }
    
    public void nextBytes(byte[] bytes) {
        for (int i = 0, len = bytes.length; i < len; )
            for (int rnd = nextInt(), n = Math.min(len - i, Integer.SIZE/Byte.SIZE); n-- > 0; rnd >>= Byte.SIZE)
                bytes[i++] = (byte)rnd;
    }
    public int nextInt() {return next(32);}
    
    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BAD_BOUND);
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0)  // i.e., bound is a power of 2
            r = (int)((bound * (long)r) >> 31);
        else { // reject over-represented candidates
            //noinspection StatementWithEmptyBody
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31))
                ;
        }
        return r;
    }
    
    public long nextLong() {
        // it's okay that the bottom word remains signed.
        return ((long)(next(32)) << 32) + next(32);
    }
    
    public boolean nextBoolean() {
        return next(1) != 0;
    }
    
    public float nextFloat() {
        return next(24) / ((float)(1 << 24));
    }
    
    public double nextDouble() {
        return (((long)(next(26)) << 27) + next(27)) * DOUBLE_UNIT;
    }
    
    private double nextNextGaussian;
    private boolean haveNextNextGaussian = false;
    
    public double nextGaussian() {
        // See Knuth, TAOCP, Vol. 2, 3rd edition, Section 3.4.1 Algorithm C.
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s)/s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }
    
    
    /** 使用 LocalRandom 的 shuffle */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void shuffle(List<?> rList, LocalRandom aRNG) {
        int size = rList.size();
        if (size < SHUFFLE_THRESHOLD || rList instanceof RandomAccess) {
            for (int i=size; i>1; --i) Collections.swap(rList, i-1, aRNG.nextInt(i));
        } else {
            Object[] arr = rList.toArray();
            // Shuffle array
            for (int i=size; i>1; --i) swap(arr, i-1, aRNG.nextInt(i));
            ListIterator it = rList.listIterator();
            for (Object e : arr) {it.next(); it.set(e);}
        }
    }
    private static final int SHUFFLE_THRESHOLD = 5;
    private static void swap(Object[] arr, int i, int j) {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }
}

