package jse.code.random;


import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * 通用随机数生成接口，用于为各种随机算法自定义随机数生成器提供输入
 * <p>
 * 解决在低版本 jdk 下没有 {@link java.util.random.RandomGenerator}
 * 接口的问题
 * @author liqa
 */
public interface IRandom {
    String BAD_BOUND = "bound must be positive";
    String BAD_FLOATING_BOUND = "bound must be finite and positive";
    String BAD_RANGE = "bound must be greater than origin";
    
    void nextBytes(byte[] bytes);
    int nextInt();
    boolean nextBoolean();
    float nextFloat();
    double nextDouble();
    double nextGaussian();
    long nextLong();
    
    default int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        // Specialize boundedNextInt for origin == 0, bound > 0
        final int m = bound - 1;
        int r = nextInt();
        if ((bound & m) == 0) {
            // The bound is a power of 2.
            r &= m;
        } else {
            // Must reject over-represented candidates
            //noinspection StatementWithEmptyBody
            for (int u = r >>> 1;
                 u + m - (r = u % bound) < 0;
                 u = nextInt() >>> 1)
                ;
        }
        return r;
    }
    default int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        int r = nextInt();
        // It's not case (1).
        final int n = bound - origin;
        final int m = n - 1;
        if ((n & m) == 0) {
            // It is case (2): length of range is a power of 2.
            r = (r & m) + origin;
        } else if (n > 0) {
            // It is case (3): need to reject over-represented candidates.
            //noinspection StatementWithEmptyBody
            for (int u = r >>> 1;
                 u + m - (r = u % n) < 0;
                 u = nextInt() >>> 1)
                ;
            r += origin;
        }
        else {
            // It is case (4): length of range not representable as long.
            while (r < origin || r >= bound) {
                r = nextInt();
            }
        }
        return r;
    }
    default long nextLong(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        // Specialize boundedNextLong for origin == 0, bound > 0
        final long m = bound - 1;
        long r = nextLong();
        if ((bound & m) == 0L) {
            // The bound is a power of 2.
            r &= m;
        } else {
            // Must reject over-represented candidates
            /* This loop takes an unlovable form (but it works):
               because the first candidate is already available,
               we need a break-in-the-middle construction,
               which is concisely but cryptically performed
               within the while-condition of a body-less for loop. */
            //noinspection StatementWithEmptyBody
            for (long u = r >>> 1;
                 u + m - (r = u % bound) < 0L;
                 u = nextLong() >>> 1)
                ;
        }
        return r;
    }
    default long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        long r = nextLong();
        // It's not case (1).
        final long n = bound - origin;
        final long m = n - 1;
        if ((n & m) == 0L) {
            // It is case (2): length of range is a power of 2.
            r = (r & m) + origin;
        } else if (n > 0L) {
            // It is case (3): need to reject over-represented candidates.
            /* This loop takes an unlovable form (but it works):
               because the first candidate is already available,
               we need a break-in-the-middle construction,
               which is concisely but cryptically performed
               within the while-condition of a body-less for loop. */
            //noinspection StatementWithEmptyBody
            for (long u = r >>> 1;            // ensure nonnegative
                 u + m - (r = u % n) < 0L;    // rejection check
                 u = nextLong() >>> 1) // retry
                ;
            r += origin;
        }
        else {
            // It is case (4): length of range not representable as long.
            while (r < origin || r >= bound)
                r = nextLong();
        }
        return r;
    }
    default double nextGaussian(double mean, double stddev) {
        if (stddev < 0.0) {
            throw new IllegalArgumentException("standard deviation must be non-negative");
        }
        return mean + stddev*nextGaussian();
    }
    default float nextFloat(float bound) {
        if (!(0.0f < bound && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
        // Specialize boundedNextFloat for origin == 0, bound > 0
        float r = nextFloat();
        r = r * bound;
        if (r >= bound) // may need to correct a rounding problem
            r = Math.nextDown(bound);
        return r;
    }
    default float nextFloat(float origin, float bound) {
        if (!(Float.NEGATIVE_INFINITY < origin && origin < bound &&
                  bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        float r = nextFloat();
        if (bound - origin < Float.POSITIVE_INFINITY) {
            r = r * (bound - origin) + origin;
        } else {
            /* avoids overflow at the cost of 3 more multiplications */
            float halfOrigin = 0.5f * origin;
            r = (r * (0.5f * bound - halfOrigin) + halfOrigin) * 2.0f;
        }
        if (r >= bound) // may need to correct a rounding problem
            r = Math.nextDown(bound);
        return r;
    }
    default double nextDouble(double bound) {
        if (!(0.0 < bound && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
        // Specialize boundedNextDouble for origin == 0, bound > 0
        double r = nextDouble();
        r = r * bound;
        if (r >= bound)  // may need to correct a rounding problem
            r = Math.nextDown(bound);
        return r;
    }
    default double nextDouble(double origin, double bound) {
        if (!(Double.NEGATIVE_INFINITY < origin && origin < bound &&
                  bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        double r = nextDouble();
        if (bound - origin < Double.POSITIVE_INFINITY) {
            r = r * (bound - origin) + origin;
        } else {
            /* avoids overflow at the cost of 3 more multiplications */
            double halfOrigin = 0.5 * origin;
            r = (r * (0.5 * bound - halfOrigin) + halfOrigin) * 2.0;
        }
        if (r >= bound)  // may need to correct a rounding problem
            r = Math.nextDown(bound);
        return r;
    }
    
    
    default Random asRandom() {
        if (this instanceof Random) return ((Random)this);
        return new Random() {
            @Override public void nextBytes(byte[] bytes) {IRandom.this.nextBytes(bytes);}
            @Override public int nextInt() {return IRandom.this.nextInt();}
            @Override public int nextInt(int bound) {return IRandom.this.nextInt(bound);}
            @Override public int nextInt(int origin, int bound) {return IRandom.this.nextInt(origin, bound);}
            @Override public long nextLong() {return IRandom.this.nextLong();}
            @Override public long nextLong(long bound) {return IRandom.this.nextLong(bound);}
            @Override public long nextLong(long origin, long bound) {return IRandom.this.nextLong(origin, bound);}
            @Override public boolean nextBoolean() {return IRandom.this.nextBoolean();}
            @Override public float nextFloat() {return IRandom.this.nextFloat();}
            @Override public float nextFloat(float bound) {return IRandom.this.nextFloat(bound);}
            @Override public float nextFloat(float origin, float bound) {return IRandom.this.nextFloat(origin, bound);}
            @Override public double nextDouble() {return IRandom.this.nextDouble();}
            @Override public double nextDouble(double bound) {return IRandom.this.nextDouble(bound);}
            @Override public double nextDouble(double origin, double bound) {return IRandom.this.nextDouble(origin, bound);}
            @Override public double nextGaussian() {return IRandom.this.nextGaussian();}
            @Override public double nextGaussian(double mean, double stddev) {return IRandom.this.nextGaussian(mean, stddev);}
            
            @Override public void setSeed(long seed) {throw new UnsupportedOperationException();}
            @Override public boolean isDeprecated() {throw new UnsupportedOperationException();}
            @Override public double nextExponential() {throw new UnsupportedOperationException();}
            @Override public IntStream ints(long streamSize) {throw new UnsupportedOperationException();}
            @Override public IntStream ints() {throw new UnsupportedOperationException();}
            @Override public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {throw new UnsupportedOperationException();}
            @Override public IntStream ints(int randomNumberOrigin, int randomNumberBound) {throw new UnsupportedOperationException();}
            @Override public LongStream longs(long streamSize) {throw new UnsupportedOperationException();}
            @Override public LongStream longs() {throw new UnsupportedOperationException();}
            @Override public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {throw new UnsupportedOperationException();}
            @Override public LongStream longs(long randomNumberOrigin, long randomNumberBound) {throw new UnsupportedOperationException();}
            @Override public DoubleStream doubles(long streamSize) {throw new UnsupportedOperationException();}
            @Override public DoubleStream doubles() {throw new UnsupportedOperationException();}
            @Override public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {throw new UnsupportedOperationException();}
            @Override public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {throw new UnsupportedOperationException();}
            
            //This method should never be reached unless done by reflection so we should throw when tried
            @Override protected int next(int bits) {throw new UnsupportedOperationException();}
        };
    }
    
    /// 提供包装方法来快速转换
    static IRandom of(final Random aRng) {
        return new IRandom() {
            @Override public void nextBytes(byte[] bytes) {aRng.nextBytes(bytes);}
            @Override public int nextInt() {return aRng.nextInt();}
            @Override public int nextInt(int bound) {return aRng.nextInt(bound);}
            @Override public long nextLong() {return aRng.nextLong();}
            @Override public boolean nextBoolean() {return aRng.nextBoolean();}
            @Override public float nextFloat() {return aRng.nextFloat();}
            @Override public double nextDouble() {return aRng.nextDouble();}
            @Override public double nextGaussian() {return aRng.nextGaussian();}
        };
    }
    static IRandom of(final SplittableRandom aRng) {
        return new IRandom() {
            @Override public void nextBytes(byte[] bytes) {aRng.nextBytes(bytes);}
            @Override public int nextInt() {return aRng.nextInt();}
            @Override public int nextInt(int bound) {return aRng.nextInt(bound);}
            @Override public long nextLong() {return aRng.nextLong();}
            @Override public boolean nextBoolean() {return aRng.nextBoolean();}
            @Override public float nextFloat() {return aRng.nextFloat();}
            @Override public double nextDouble() {return aRng.nextDouble();}
            @Override public double nextGaussian() {return aRng.nextGaussian();}
        };
    }
    @SuppressWarnings("Since15")
    static IRandom of(Object aRng) {
        if (aRng instanceof IRandom) return (IRandom)aRng;
        if (aRng instanceof Random) return of((Random)aRng);
        if (aRng instanceof SplittableRandom) return of((SplittableRandom)aRng);
        if (aRng instanceof java.util.random.RandomGenerator) {
            final java.util.random.RandomGenerator tRng = (java.util.random.RandomGenerator)aRng;
            return new IRandom() {
                @Override public void nextBytes(byte[] bytes) {tRng.nextBytes(bytes);}
                @Override public int nextInt() {return tRng.nextInt();}
                @Override public int nextInt(int bound) {return tRng.nextInt(bound);}
                @Override public long nextLong() {return tRng.nextLong();}
                @Override public boolean nextBoolean() {return tRng.nextBoolean();}
                @Override public float nextFloat() {return tRng.nextFloat();}
                @Override public double nextDouble() {return tRng.nextDouble();}
                @Override public double nextGaussian() {return tRng.nextGaussian();}
            };
        }
        throw new IllegalArgumentException("Unsupported random generator class: " + aRng.getClass().getName());
    }
}
