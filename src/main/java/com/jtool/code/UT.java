package com.jtool.code;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.jtool.atom.*;
import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.operator.IOperator1;
import com.jtool.code.task.TaskCall;
import com.jtool.code.task.TaskRun;
import com.jtool.math.function.IFunc1;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.RowMatrix;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import com.jtool.parallel.MergedFuture;
import com.jtool.parallel.ParforThreadPool;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import org.apache.groovy.json.internal.CharScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.jtool.code.CS.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author liqa
 * <p> utils of this project </p>
 */
public class UT {
    
    public static class Code {
        
        /**
         * Get the random seed for lammps usage
         * @author liqa
         */
        public synchronized static int randSeed() {return RANDOM.nextInt(MAX_SEED);}
        
        /**
         * Get the random id in URL and Filename safe Base64, 8 length
         * @author liqa
         */
        public synchronized static String randID() {
            byte[] rBytes = new byte[6];
            RANDOM.nextBytes(rBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rBytes);
        }
        
        
        /**
         * Get the value in the map according to the order of the keys
         * @author liqa
         */
        public static Object get(Map<?, ?> aMap, Object... aKeys) {
            if (aKeys == null) return null;
            for (Object tKey : aKeys) if (aMap.containsKey(tKey)) return aMap.get(tKey);
            return null;
        }
        /**
         * Get the value in the map according to the order of the keys and with default value
         * @author liqa
         */
        public static Object getWithDefault(Map<?, ?> aMap, Object aDefaultValue, Object... aKeys) {
            if (aKeys == null) return aDefaultValue;
            for (Object tKey : aKeys) if (aMap.containsKey(tKey)) return aMap.get(tKey);
            return aDefaultValue;
        }
        
        
        /**
         * merge two array into one List
         * @author liqa
         */
        public static List<Object> merge(final Object[] aBefore, Object... aAfter) {
            final Object[] fAfter = aAfter==null ? ZL_OBJ : aAfter;
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {return index<aBefore.length ? aBefore[index] : fAfter[index-aBefore.length];}
                @Override public int size() {return aBefore.length+fAfter.length;}
            };
        }
        public static List<Object> mergeBefore(final Object[] aAfter, Object... aBefore) {
            final Object[] fBefore = aBefore==null ? ZL_OBJ : aBefore;
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {return index<fBefore.length ? fBefore[index] : aAfter[index-fBefore.length];}
                @Override public int size() {return fBefore.length+aAfter.length;}
            };
        }
        public static List<Object> merge(final Object aBefore0, final Object[] aAfter) {
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {return index<1 ? aBefore0 : aAfter[index-1];}
                @Override public int size() {return aAfter.length+1;}
            };
        }
        public static List<Object> merge(final Object aBefore0, final Object aBefore1, final Object[] aAfter) {
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {
                    switch (index) {
                    case 0: return aBefore0;
                    case 1: return aBefore1;
                    default: return aAfter[index-2];
                    }
                }
                @Override public int size() {return aAfter.length+2;}
            };
        }
        public static List<Object> merge(final Object aBefore0, final Object aBefore1, final Object aBefore2, final Object[] aAfter) {
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {
                    switch (index) {
                    case 0: return aBefore0;
                    case 1: return aBefore1;
                    case 2: return aBefore2;
                    default: return aAfter[index-3];
                    }
                }
                @Override public int size() {return aAfter.length+3;}
            };
        }
        public static List<Object> merge(final Object[] aBefore, final Object aAfter0) {
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {
                    int tRest = index-aBefore.length;
                    return tRest==0 ? aAfter0 : aBefore[index];
                }
                @Override public int size() {return aBefore.length+1;}
            };
        }
        public static List<Object> merge(final Object[] aBefore, final Object aAfter0, final Object aAfter1) {
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {
                    int tRest = index-aBefore.length;
                    switch (tRest) {
                    case 0: return aAfter0;
                    case 1: return aAfter1;
                    default: return aBefore[index];
                    }
                }
                @Override public int size() {return aBefore.length+2;}
            };
        }
        public static List<Object> merge(final Object[] aBefore, final Object aAfter0, final Object aAfter1, final Object aAfter2) {
            return new AbstractRandomAccessList<Object>() {
                @Override public Object get(int index) {
                    int tRest = index-aBefore.length;
                    switch (tRest) {
                    case 0: return aAfter0;
                    case 1: return aAfter1;
                    case 2: return aAfter2;
                    default: return aBefore[index];
                    }
                }
                @Override public int size() {return aBefore.length+3;}
            };
        }
        public static <T> Iterable<T> merge(final T aBefore0, final Iterable<? extends T> aAfter) {
            return () -> new Iterator<T>() {
                private final Iterator<? extends T> mIt = aAfter.iterator();
                private boolean mFirst = true;
                @Override public boolean hasNext() {
                    if (mFirst) return true;
                    return mIt.hasNext();
                }
                @Override public T next() {
                    if (mFirst) {
                        mFirst = false;
                        return aBefore0;
                    } else {
                        return mIt.next();
                    }
                }
            };
        }
        
        
        /**
         * filter the input Iterable
         * @author liqa
         */
        public static <T> Iterable<T> filter(final Iterable<T> aIterable, final IOperator1<Boolean, ? super T> aFilter) {
            return () -> new Iterator<T>() {
                private final Iterator<T> mIt = aIterable.iterator();
                private T mNext = null;
                
                @Override public boolean hasNext() {
                    while (true) {
                        if (mNext != null) return true;
                        if (mIt.hasNext()) {
                            mNext = mIt.next();
                            // 过滤器不通过则设为 null 跳过
                            if (!aFilter.cal(mNext)) mNext = null;
                            continue;
                        }
                        return false;
                    }
                }
                @Override public T next() {
                    if (hasNext()) {
                        T tNext = mNext;
                        mNext = null; // 设置 mNext 非法表示此时不再有 Next
                        return tNext;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }
        /**
         * map {@code Iterable<T> to Iterable<R>} like {@link Stream}.map
         * @author liqa
         */
        public static <R, T> Iterable<R> map(final Iterable<T> aIterable, final IOperator1<? extends R, ? super T> aOpt) {
            return () -> map(aIterable.iterator(), aOpt);
        }
        public static <R, T> Iterator<R> map(final Iterator<T> aIterator, final IOperator1<? extends R, ? super T> aOpt) {
            return new Iterator<R>() {
                final Iterator<T> mIt = aIterator;
                @Override public boolean hasNext() {
                    return mIt.hasNext();
                }
                @Override public R next() {
                    if (hasNext()) {
                        return aOpt.cal(mIt.next());
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }
        public static <R, T> Collection<R> map(final Collection<T> aCollection, final IOperator1<? extends R, ? super T> aOpt) {
            return new AbstractCollection<R>() {
                @Override public Iterator<R> iterator() {return map(aCollection.iterator(), aOpt);}
                @Override public int size() {return aCollection.size();}
            };
        }
        public static <R, T> List<R> map(final List<T> aList, final IOperator1<? extends R, ? super T> aOpt) {
            return new AbstractRandomAccessList<R>() {
                @Override public R get(int index) {return aOpt.cal(aList.get(index));}
                @Override public int size() {return aList.size();}
                @Override public Iterator<R> iterator() {return map(aList.iterator(), aOpt);}
            };
        }
        public static <R, T> List<R> map(final T[] aArray, final IOperator1<? extends R, ? super T> aOpt) {
            return new AbstractRandomAccessList<R>() {
                @Override public R get(int index) {return aOpt.cal(aArray[index]);}
                @Override public int size() {return aArray.length;}
            };
        }
        
        
        
        /**
         * Convert nested Iterable to a single one
         * @author liqa
         */
        public static <T> Iterable<T> toIterable(final Iterable<? extends Iterable<? extends T>> aNestIterable) {
            return () -> new Iterator<T>() {
                private final Iterator<? extends Iterable<? extends T>> mParentIt = aNestIterable.iterator();
                private Iterator<? extends T> mIt = mParentIt.hasNext() ? mParentIt.next().iterator() : null;
                private T mNext = null;
                
                @Override public boolean hasNext() {
                    if (mIt == null) return false;
                    while (true) {
                        if (mNext != null) return true;
                        if (mIt.hasNext()) {
                            mNext = mIt.next();
                            continue;
                        }
                        if (mParentIt.hasNext()) {
                            mIt = mParentIt.next().iterator();
                            mNext = null;
                            continue;
                        }
                        return false;
                    }
                }
                @Override public T next() {
                    if (hasNext()) {
                        T tNext = mNext;
                        mNext = null; // 设置 mNext 非法表示此时不再有 Next
                        return tNext;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }
        
        /**
         * Convert Iterable to a Collection to use size()
         * @author liqa
         */
        public static <T> Collection<T> toCollection(final Iterable<T> aIterable) {
            if (aIterable instanceof Collection) return (Collection<T>)aIterable;
            List<T> rList = new ArrayList<>();
            for (T tEntry : aIterable) rList.add(tEntry);
            return rList;
        }
        /**
         * Convert Iterable to a List to use size() and get(i)
         * @author liqa
         */
        public static <T> List<T> toList(final Iterable<T> aIterable) {
            // 注意由于约定，需要能够随机访问才会直接转换，否则依旧会重新构造
            if ((aIterable instanceof List) & (aIterable instanceof RandomAccess)) return (List<T>)aIterable;
            List<T> rList = new ArrayList<>();
            for (T tEntry : aIterable) rList.add(tEntry);
            return rList;
        }
        
        /**
         * Convert IHasXYZ to XYZ to optimise
         * @author liqa
         */
        public static XYZ toXYZ(IHasXYZ aXYZ) {
            return (aXYZ instanceof XYZ) ? (XYZ)aXYZ : new XYZ(aXYZ);
        }
        /**
         * Convert IHasXYZ to XYZ for box usage
         * @author liqa
         */
        public static XYZ toBOX(IHasXYZ aXYZ) {
            if (aXYZ == BOX_ONE) return BOX_ONE;
            if (aXYZ == BOX_ZERO) return BOX_ZERO;
            return toXYZ(aXYZ);
        }
        
        
        /**
         * {@link Arrays}.asList for double[]
         * @author liqa
         * @param aData the input double[]
         * @return the list format of double[]
         */
        public static List<Double> asList(final double[] aData) {
            return new AbstractRandomAccessList<Double>() {
                @Override public Double get(int index) {return aData[index];}
                @Override public Double set(int index, Double element) {
                    double oValue = aData[index];
                    aData[index] = element;
                    return oValue;
                }
                @Override public int size() {return aData.length;}
            };
        }
        /**
         * {@link Arrays}.asList for int[]
         * @author liqa
         * @param aData the input int[]
         * @return the list format of int[]
         */
        public static List<Integer> asList(final int[] aData) {
            return new AbstractRandomAccessList<Integer>() {
                @Override public Integer get(int index) {return aData[index];}
                @Override public Integer set(int index, Integer element) {
                    int oValue = aData[index];
                    aData[index] = element;
                    return oValue;
                }
                @Override public int size() {return aData.length;}
            };
        }
        /**
         * {@link Arrays}.asList for boolean[]
         * @author liqa
         * @param aData the input boolean[]
         * @return the list format of boolean[]
         */
        public static List<Boolean> asList(final boolean[] aData) {
            return new AbstractRandomAccessList<Boolean>() {
                @Override public Boolean get(int index) {return aData[index];}
                @Override public Boolean set(int index, Boolean element) {
                    boolean oValue = aData[index];
                    aData[index] = element;
                    return oValue;
                }
                @Override public int size() {return aData.length;}
            };
        }
        
        /**
         * Convert {@code Collection<Number>} to double[]
         * @author liqa
         */
        public static double[] toData(Collection<? extends Number> aList) {
            double[] rData = new double[aList.size()];
            int tIdx = 0;
            for (Number tValue : aList) {
                rData[tIdx] = tValue.doubleValue();
                ++tIdx;
            }
            return rData;
        }
        public static double[] toData(int aSize, Iterable<? extends Number> aList) {
            double[] rData = new double[aSize];
            int tIdx = 0;
            for (Number tValue : aList) {
                rData[tIdx] = tValue.doubleValue();
                ++tIdx;
            }
            return rData;
        }
        /**
         * Convert {@code Collection<Collection<Number>>} to double[][]
         * @author liqa
         */
        public static double[][] toMat(Collection<? extends Collection<? extends Number>> aRows) {
            double[][] rMat = new double[aRows.size()][];
            int tIdx = 0;
            for (Collection<? extends Number> tRow : aRows) {
                rMat[tIdx] = toData(tRow);
                ++tIdx;
            }
            return rMat;
        }
        public static double[][] toMat(int aRowNum, int aColNum, Iterable<? extends Iterable<? extends Number>> aRows) {
            double[][] rMat = new double[aRowNum][];
            int tIdx = 0;
            for (Iterable<? extends Number> tRow : aRows) {
                rMat[tIdx] = toData(aColNum, tRow);
                ++tIdx;
            }
            return rMat;
        }
        
        
        /**
         * the range function similar to python
         * <p> only support in aStep > 0 for now </p>
         * @author liqa
         * @param aStart the start value, include
         * @param aStop the stop position, exclude
         * @param aStep the step of Iteration
         * @return A iterable container
         */
        public static Iterable<Integer> range_(final int aStart, final int aStop, final int aStep) {
            return () -> new Iterator<Integer>() {
                int mIdx = aStart;
                @Override public boolean hasNext() {
                    return mIdx < aStop;
                }
                @Override public Integer next() {
                    if (hasNext()) {
                        int tIdx = mIdx;
                        mIdx += aStep;
                        return tIdx;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }
        public static Iterable<Integer> range_(            int aSize           ) {return range_(0, aSize);}
        public static Iterable<Integer> range_(int aStart, int aStop           ) {return range_(aStart, aStop, 1);}
        public static Iterable<Integer> range (            int aSize           ) {return range(0, aSize);}
        public static Iterable<Integer> range (int aStart, int aStop           ) {return range(aStart, aStop, 1);}
        public static Iterable<Integer> range (int aStart, int aStop, int aStep) {
            aStep = Math.max(aStep, 1);
            aStop = Math.max(aStop, aStart);
            return range_(aStart, aStop, aStep);
        }
    }
    
    public static class Par {
        /**
         * parfor for groovy usage
         * @author liqa
         */
        @VisibleForTesting public static void parfor(int aSize, Closure<?> aGroovyTask) {parfor(aSize, DEFAULT_THREAD_NUM, aGroovyTask);}
        @VisibleForTesting public static void parfor(int aSize, int aThreadNum, final Closure<?> aGroovyTask) {
            try (ParforThreadPool tPool = new ParforThreadPool(aThreadNum)) {
                int tN = aGroovyTask.getMaximumNumberOfParameters();
                switch (tN) {
                case 0: {tPool.parfor(aSize, (i, threadID) -> aGroovyTask.call()); return;}
                case 1: {tPool.parfor(aSize, (i, threadID) -> aGroovyTask.call(i)); return;}
                case 2: {tPool.parfor(aSize, (i, threadID) -> aGroovyTask.call(i, threadID)); return;}
                default: throw new IllegalArgumentException("Parameters Number of parfor Task Must be 0, 1 or 2");
                }
            }
        }
        /**
         * parwhile for groovy usage
         * @author liqa
         */
        @VisibleForTesting public static void parwhile(ParforThreadPool.IParwhileChecker aChecker, Closure<?> aGroovyTask) {parwhile(aChecker, DEFAULT_THREAD_NUM, aGroovyTask);}
        @VisibleForTesting public static void parwhile(ParforThreadPool.IParwhileChecker aChecker, int aThreadNum, final Closure<?> aGroovyTask) {
            try (ParforThreadPool tPool = new ParforThreadPool(aThreadNum)) {
                int tN = aGroovyTask.getMaximumNumberOfParameters();
                switch (tN) {
                case 0: {tPool.parwhile(aChecker, (threadID) -> aGroovyTask.call()); return;}
                case 1: {tPool.parwhile(aChecker, (threadID) -> aGroovyTask.call(threadID)); return;}
                default: throw new IllegalArgumentException("Parameters Number of parwhile Task Must be 0 or 1");
                }
            }
        }
        
        /**
         * merge {@code Iterable<Future<T>> to single Future<List<T>>} in All logic
         * @author liqa
         */
        public static <T> Future<List<T>> mergeAll(Iterable<? extends Future<? extends T>> aFutures) {return new MergedFuture<>(aFutures);}
        
        /**
         * map {@code Future<T> to Future<R>} like {@link Stream}.map
         * @author liqa
         */
        public static <R, T> Future<R> map(final Future<T> aFuture, final IOperator1<? extends R, ? super T> aOpt) {
            return new Future<R>() {
                @Override public boolean cancel(boolean mayInterruptIfRunning) {return aFuture.cancel(mayInterruptIfRunning);}
                @Override public boolean isCancelled() {return aFuture.isCancelled();}
                @Override public boolean isDone() {return aFuture.isDone();}
                @Override public R get() throws InterruptedException, ExecutionException {return aOpt.cal(aFuture.get());}
                @Override public R get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {return aOpt.cal(aFuture.get(timeout, unit));}
            };
        }
        
        /**
         * 获取一个 Future 使用携程执行 aSupplier 获取结果；
         * 其中使用新建一个 Thread 而不是 {@link CompletableFuture} 中使用的 {@link ForkJoinPool}；
         * 这样可以避免线程数上限，主要用于执行需要长期监控但是不消耗资源的情况
         * @param aSupplier 计算返回值的函数
         * @param <U> 函数的返回类型
         * @return 新的 Future
         */
        public static <U> Future<U> supplyAsync(Supplier<U> aSupplier) {return new ThreadFuture<>(aSupplier);}
        /**
         * 获取一个 Future 使用携程执行 aRunnable；
         * 其中使用新建一个 Thread 而不是 {@link CompletableFuture} 中使用的 {@link ForkJoinPool}；
         * 这样可以避免线程数上限，主要用于执行需要长期监控但是不消耗资源的情况
         * @param aRunnable 需要运行的 Runnable
         * @return 新的 Future
         */
        public static Future<Void> runAsync(Runnable aRunnable) {return new ThreadFuture<>(aRunnable);}
        
        private final static class ThreadFuture<T> implements Future<T> {
            private final static int SLEEP_TIME = 10, TRY_TIMES = 100;
            
            /** 使用 volatile 或 final 保证不同线程的可见性 */
            private volatile T mResult = null;
            private volatile boolean mFinished = false, mCancelled = false;
            private final Thread mThread;
            private ThreadFuture(final Supplier<T> aSupplier) {
                mThread = new Thread(() -> {
                    mResult = aSupplier.get();
                    mFinished = true;
                });
                mThread.start();
            }
            private ThreadFuture(final Runnable aRunnable) {
                mThread = new Thread(() -> {
                    aRunnable.run();
                    mFinished = true;
                });
                mThread.start();
            }
            
            @Override public boolean cancel(boolean mayInterruptIfRunning) {
                if (mayInterruptIfRunning && mThread.isAlive()) {
                    mThread.interrupt();
                    for (int i = 0; i < TRY_TIMES; ++i) {
                        if (!mThread.isAlive()) {mCancelled = true; mFinished = true; return true;}
                        try {Thread.sleep(SLEEP_TIME);}
                        catch (InterruptedException e) {return false;}
                    }
                }
                return false;
            }
            @Override public boolean isCancelled() {return mCancelled;}
            @Override public boolean isDone() {return mFinished;}
            @SuppressWarnings("BusyWait")
            @Override public T get() throws InterruptedException {
                while (!mFinished) Thread.sleep(1);
                if (mCancelled) throw new CancellationException();
                return mResult;
            }
            @SuppressWarnings("BusyWait")
            @Override public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, TimeoutException {
                long tic = System.nanoTime();
                while (!mFinished) {
                    Thread.sleep(1);
                    if (System.nanoTime()-tic >= unit.toNanos(timeout)) throw new TimeoutException();
                }
                if (mCancelled) throw new CancellationException();
                return mResult;
            }
        }
    }
    
    public static class Hack {
        
        public static TaskCall<Object> getCallableOfMethod_(Class<?> aClazz, final @Nullable Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            final Method m = findMethod_(aClazz, aMethodName, fArgs);
            if (m == null) throw new NoSuchMethodException("No such method: " + aMethodName);
            convertArgs_(fArgs, m.getParameterTypes());
            return new TaskCall<>(() -> m.invoke(aInstance, fArgs));
        }
        public static TaskCall<Object> getCallableOfStaticMethod(String aClassName, String aMethodName, Object... aArgs) throws NoSuchMethodException, ClassNotFoundException {
            Class<?> tClass;
            tClass = Class.forName(aClassName);
            return getCallableOfMethod_(tClass, null, aMethodName, aArgs);
        }
        public static TaskRun          getRunnableOfStaticMethod(              String aClassName, String aMethodName, Object... aArgs) throws NoSuchMethodException, ClassNotFoundException {return getCallableOfStaticMethod(aClassName, aMethodName, aArgs).toRunnable();}
        public static TaskCall<Object> getCallableOfMethod      (final @NotNull Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {return getCallableOfMethod_(aInstance.getClass(), aInstance, aMethodName, aArgs);}
        public static TaskRun          getRunnableOfMethod      (final @NotNull Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {return getCallableOfMethod(aInstance, aMethodName, aArgs).toRunnable();}
        
        public static Method findMethod_(Class<?> aClazz, String aMethodName, Object @NotNull... aArgs) {
            Method[] tAllMethods = aClazz.getMethods();
            for (Method tMethod : tAllMethods) if (tMethod.getName().equals(aMethodName)) {
                if (compatible(aArgs, tMethod.getParameterTypes())) return tMethod;
            }
            return null;
        }
        public static Constructor<?> findConstructor_(Class<?> aClazz, Object @NotNull... aArgs) {
            Constructor<?>[] tAllConstructors = aClazz.getConstructors();
            for (Constructor<?> tConstructor : tAllConstructors) {
                if (compatible(aArgs, tConstructor.getParameterTypes())) return tConstructor;
            }
            return null;
        }
        private static boolean compatible(Object[] aArgs, Class<?>[] aMethodParameterTypes) {
            if (aArgs.length != aMethodParameterTypes.length) return false;
            for (int i = 0; i < aArgs.length; i++) if (!compatible(aArgs[i], aMethodParameterTypes[i])) return false;
            return true;
        }
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private static boolean compatible(Object aArg, Class<?> aMethodParameterType) {
            // 首先统一转换成 Wrapper 的类型
            aMethodParameterType = toWrapperType(aMethodParameterType);
            // 数字都相互兼容（仅限基本类型数字）
            if ((aArg instanceof Number) && (aMethodParameterType==Double.class || aMethodParameterType==Integer.class || aMethodParameterType==Long.class || aMethodParameterType==Float.class || aMethodParameterType==Byte.class || aMethodParameterType==Short.class)) return true;
            // 一般情况，参数 instanceof 参数种类即可
            return aMethodParameterType.isInstance(aArg);
        }
        public static void convertArgs_(Object[] rArgs, Class<?>[] aParameterTypes) {
            for (int i = 0; i < aParameterTypes.length; i++) {
                Class<?> tArgClazz = rArgs[i].getClass();
                Class<?> tParClazz = toWrapperType(aParameterTypes[i]); // 注意需要转换成 Wrapper 的类型
                if (tArgClazz == tParClazz) continue;
                // 做数字转换
                if (rArgs[i] instanceof Number) {
                    Number tArgValue = (Number)rArgs[i];
                    if      (tParClazz ==  Double.class) rArgs[i] = tArgValue.doubleValue();
                    else if (tParClazz == Integer.class) rArgs[i] = tArgValue.intValue();
                    else if (tParClazz ==    Long.class) rArgs[i] = tArgValue.longValue();
                    else if (tParClazz ==   Float.class) rArgs[i] = tArgValue.floatValue();
                    else if (tParClazz ==    Byte.class) rArgs[i] = tArgValue.byteValue();
                    else if (tParClazz ==   Short.class) rArgs[i] = tArgValue.shortValue();
                }
            }
        }
        
        private static Class<?> toWrapperType(Class<?> aClazz) {
            Class<?> tWrapper = PRIMITIVE_TO_WRAPPER.get(aClazz);
            return tWrapper == null ? aClazz : tWrapper;
        }
        public static final @Unmodifiable BiMap<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = (new ImmutableBiMap.Builder<Class<?>, Class<?>>())
            .put(boolean.class,   Boolean.class)
            .put(   byte.class,      Byte.class)
            .put(   char.class, Character.class)
            .put(  short.class,     Short.class)
            .put(    int.class,   Integer.class)
            .put(   long.class,      Long.class)
            .put(  float.class,     Float.class)
            .put( double.class,    Double.class)
            .build();
    }
    
    public static class Timer {
        private static Long sTime = null;
        
        public static void tic() {sTime = System.currentTimeMillis();}
        public static void toc() {toc("Total");}
        public static void toc(String aMsg) {
            double elapsedTime = (System.currentTimeMillis() - sTime) / 1000.0;
            sTime = null;
            
            int hours = (int) Math.floor(elapsedTime / 3600.0);
            int minutes = (int) Math.floor(elapsedTime % 3600.0) / 60;
            double seconds = elapsedTime % 60.0;
            System.out.printf("%s time: %02d hour %02d min %02.2f sec\n", aMsg, hours, minutes, seconds);
        }
    }
    
    /** 序列化和反序列化的一些方法 */
    public static class Serial {
        public final static int LONG_LEN = 8, DOUBLE_LEN = LONG_LEN;
        public final static int INT_LEN = 4;
        
        
        public static void int2bytes(int aI, byte[] rBytes, final int aPos) {
            for (int i = aPos; i < aPos+4; ++i) {
                rBytes[i] = (byte)(aI & 0xff);
                aI >>= 8;
            }
        }
        public static int bytes2int(byte[] aBytes, final int aPos) {
            int rI = 0;
            for (int i = aPos+3; i >= aPos; --i) {
                rI <<= 8;
                rI |= (aBytes[i] & 0xff);
            }
            return rI;
        }
        
        public static void long2bytes(long aL, byte[] rBytes, final int aPos) {
            for (int i = aPos; i < aPos+8; ++i) {
                rBytes[i] = (byte)(aL & 0xff);
                aL >>= 8;
            }
        }
        public static long bytes2long(byte[] aBytes, final int aPos) {
            long rL = 0;
            for (int i = aPos+7; i >= aPos; --i) {
                rL <<= 8;
                rL |= (aBytes[i] & 0xff);
            }
            return rL;
        }
        public static void double2bytes(double aD, byte[] rBytes, final int aPos) {long2bytes(Double.doubleToRawLongBits(aD), rBytes, aPos);}
        public static double bytes2double(byte[] aBytes, final int aPos) {return Double.longBitsToDouble(bytes2long(aBytes, aPos));}
        
        
        public static byte[] str2bytes(String aStr) {return aStr.getBytes(StandardCharsets.UTF_8);}
        public static String bytes2str(byte[] aBytes) {return new String(aBytes, StandardCharsets.UTF_8);}
        
        
        public static byte[] int2bytes(int aI) {
            byte[] rBytes = new byte[INT_LEN];
            int2bytes(aI, rBytes, 0);
            return rBytes;
        }
        public static int bytes2int(byte[] aBytes) {
            return bytes2int(aBytes, 0);
        }
        
        public static byte[] long2bytes(long aL) {
            byte[] rBytes = new byte[LONG_LEN];
            long2bytes(aL, rBytes, 0);
            return rBytes;
        }
        public static long bytes2long(byte[] aBytes) {
            return bytes2long(aBytes, 0);
        }
        public static byte[] double2bytes(double aD) {return long2bytes(Double.doubleToRawLongBits(aD));}
        public static double bytes2double(byte[] aBytes) {return Double.longBitsToDouble(bytes2long(aBytes));}
        
        /** {@link IHasAtomData} 的序列化和反序列化 */
        public static byte[] atomDataXYZ2bytes(IHasAtomData aAtomData) {
            byte[] rBytes = new byte[DOUBLE_LEN*3*2 + DOUBLE_LEN*3*aAtomData.atomNum()];
            int tIdx = 0;
            // 模拟盒数据
            IHasXYZ tBoxLo = aAtomData.boxLo();
            double2bytes(tBoxLo.x(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            double2bytes(tBoxLo.y(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            double2bytes(tBoxLo.z(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            IHasXYZ tBoxHi = aAtomData.boxHi();
            double2bytes(tBoxHi.x(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            double2bytes(tBoxHi.y(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            double2bytes(tBoxHi.z(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            // 原子数据
            for (IAtom tAtom : aAtomData.atoms()) {
                double2bytes(tAtom.x(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
                double2bytes(tAtom.y(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
                double2bytes(tAtom.z(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            }
            return rBytes;
        }
        public static IHasAtomData bytes2atomDataXYZ(byte[] aBytes) {
            double tX, tY, tZ;
            int tIdx = 0;
            // 获取模拟盒数据
            tX = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            tY = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            tZ = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            final XYZ tBoxLo = new XYZ(tX, tY, tZ);
            tX = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            tY = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            tZ = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            final XYZ tBoxHi = new XYZ(tX, tY, tZ);
            // 获取原子数据，这里只有 XYZ 数据
            final int tAtomNum = aBytes.length/(DOUBLE_LEN*3) - 2;
            final List<IAtom> rAtoms = new ArrayList<>(tAtomNum);
            for (int tID = 1; tID <= tAtomNum; ++tID) {
                tX = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
                tY = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
                tZ = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
                rAtoms.add(new Atom(tX, tY, tZ, tID));
            }
            // 返回结果
            return new AbstractAtomData() {
                @Override public List<IAtom> atoms() {return rAtoms;}
                @Override public IHasXYZ boxLo() {return tBoxLo;}
                @Override public IHasXYZ boxHi() {return tBoxHi;}
                @Override public int atomNum() {return tAtomNum;}
                @Override public int atomTypeNum() {return 1;}
            };
        }
    }
    
    public static class Texts {
        /**
         * 针对 lammps 等软件输出文件中存在的使用空格分割的数据专门优化的读取操作，
         * 使用 groovy-json 中现成的 parseDouble 等方法，总体比直接 split 并用 java 的 parseDouble 快一倍以上
         * @author liqa
         */
        public static IVector str2data(String aStr, int aLength) {
            // 先直接转 char[]，适配 groovy-json 的 CharScanner
            char[] tChar = aStr.toCharArray();
            // 直接遍历忽略空格（不可识别的也会跳过），获取开始和末尾，然后 parseDouble
            IVector rData = Vectors.zeros(aLength);
            int tFrom = CharScanner.skipWhiteSpace(tChar, 0, tChar.length);
            int tIdx = 0;
            for (int i = tFrom; i < tChar.length; ++i) {
                int tCharCode = tChar[i];
                if (tFrom < 0) {
                    if (tCharCode > 32) {
                        tFrom = i;
                    }
                } else {
                    if (tCharCode <= 32) {
                        rData.set_(tIdx, CharScanner.parseDouble(tChar, tFrom, i));
                        tFrom = -1;
                        ++tIdx;
                        if (tIdx == aLength) return rData;
                    }
                }
            }
            // 最后一个数据
            if (tFrom >= 0 && tFrom < tChar.length) rData.set_(tIdx, CharScanner.parseDouble(tChar, tFrom, tChar.length));
            return rData;
        }
        
        
        /**
         * Start from aStartIdx to find the first index containing aContainStr
         * @author liqa
         * @param aLines where to find the aContainStr
         * @param aStartIdx the start position, include
         * @param aContainStr a string to find in aLines
         * @return the idx of aLines which contains aContainStr, or aLines.length if not find
         */
        public static int findLineContaining(List<String> aLines, int aStartIdx, String aContainStr) {
            int tIdx = aStartIdx;
            while (tIdx < aLines.size()) {
                if (aLines.get(tIdx).contains(aContainStr)) break;
                ++tIdx;
            }
            return tIdx;
        }
        
        /**
         * Splits a string separated by blank characters into multiple strings
         * <p> will automatically ignores multiple spaces and the beginning and end spaces </p>
         * @author liqa
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitBlank(String aStr) {
            return BLANKS.split(aStr.trim());
        }
        
        
        /**
         * Splits a string separated by comma(",") characters into multiple strings
         * <p> will automatically ignores multiple spaces </p>
         * @author liqa
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitComma(String aStr) {
            return COMMA.split(BLANKS.matcher(aStr).replaceAll(""));
        }
        
        
        /**
         * Java version of splitNodeList
         * @author liqa
         * @param aRawNodeList input raw node list from $SLURM_JOB_NODELIST
         * @return split node list
         */
        public static List<String> splitNodeList(String aRawNodeList) {
            List<String> rOutput = new ArrayList<>();
            
            // Remove the "cn", "[" and "]" characters
            String tTrimmedStr = aRawNodeList.replace("cn", "").replace("[", "").replace("]", "");
            
            // Split the string by comma
            String[] tArray = tTrimmedStr.split(",");
            
            // Range of numbers
            Pattern tPattern = Pattern.compile("([0-9]+)-([0-9]+)");
            // Loop through each range and generate the numbers
            for (String tRange : tArray) {
                Matcher tMatcher = tPattern.matcher(tRange);
                if (tMatcher.find()) {
                    int tStart = Integer.parseInt(tMatcher.group(1));
                    int tEnd = Integer.parseInt(tMatcher.group(2));
                    for (int i = tStart; i <= tEnd; i++) {
                        rOutput.add("cn" + i);
                    }
                } else {
                    // Single number
                    rOutput.add("cn" + tRange);
                }
            }
            
            return rOutput;
        }
    }
    
    public static class IO {
        /**
         * Wrapper of {@link Files}.write
         * @author liqa
         * @param aFilePath File to write
         * @param aLines Iterable String or String[]
         * @throws IOException when fail
         */
        public static void write(String aFilePath, String[] aLines, OpenOption... aOptions)                         throws IOException  {write(aFilePath, Arrays.asList(aLines), aOptions);}
        public static void write(String aFilePath, String aText, OpenOption... aOptions)                            throws IOException  {write(aFilePath, Collections.singletonList(aText), aOptions);}
        public static void write(String aFilePath, byte[] aData, OpenOption... aOptions)                            throws IOException  {write(toAbsolutePath_(aFilePath), aData, aOptions);}
        public static void write(String aFilePath, Iterable<? extends CharSequence> aLines, OpenOption... aOptions) throws IOException  {write(toAbsolutePath_(aFilePath), aLines, aOptions);}
        public static void write(Path aPath, byte[] aData, OpenOption... aOptions)                                  throws IOException  {validPath(aPath); Files.write(aPath, aData, aOptions);}
        public static void write(Path aPath, Iterable<? extends CharSequence> aLines, OpenOption... aOptions)       throws IOException  {validPath(aPath); Files.write(aPath, aLines, aOptions);}
        /**
         * Wrapper of {@link Files}.readAllBytes
         * @author liqa
         * @param aFilePath File to read
         * @return array of byte
         * @throws IOException when fail
         */
        public static byte[] readAllBytes(String aFilePath) throws IOException {return Files.readAllBytes(toAbsolutePath_(aFilePath));}
        /**
         * read all lines of the File
         * @author liqa
         * @param aFilePath File to read
         * @return lines of String
         * @throws IOException when fail
         */
        public static List<String> readAllLines(String aFilePath) throws IOException {return Files.readAllLines(toAbsolutePath_(aFilePath));}
        /**
         * read all lines of the InputStream
         * @author liqa
         */
        public static List<String> readAllLines(InputStream aInputStream) throws IOException {
            try (BufferedReader tReader = toReader(aInputStream)) {
                List<String> rLines = new ArrayList<>();
                String tLine;
                while ((tLine = tReader.readLine()) != null) rLines.add(tLine);
                return rLines;
            }
        }
        
        /**
         * remove the Directory, will remove the subdirectories recursively
         * @author liqa
         * @param aDir the Directory will be removed
         */
        @VisibleForTesting public static void rmdir(String aDir) throws IOException {removeDir(aDir);}
        public static void removeDir(String aDir) throws IOException {
            if (!aDir.isEmpty() && !aDir.endsWith("/") && !aDir.endsWith("\\")) aDir += "/";
            if (!isDir(aDir)) return;
            removeDir_(aDir);
        }
        private static void removeDir_(String aDir) throws IOException {
            String[] tFiles = UT.IO.list(aDir);
            if (tFiles == null) return;
            for (String tName : tFiles) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tFileOrDir = aDir+tName;
                if (UT.IO.isDir(tFileOrDir)) {removeDir_(tFileOrDir+"/");}
                else if (UT.IO.isFile(tFileOrDir)) {delete(tFileOrDir);}
            }
            delete(aDir);
        }
        
        /** useful methods, wrapper of {@link Files} stuffs */
        @VisibleForTesting
        public static void      mkdir   (String aDir)                                   throws IOException  {makeDir(aDir);} // can mkdir nested
        public static void      makeDir (String aDir)                                   throws IOException  {makeDir(toAbsolutePath_(aDir));} // can mkdir nested
        public static boolean   isDir   (String aDir)                                                       {return Files.isDirectory(toAbsolutePath_(aDir));}
        public static boolean   isFile  (String aFilePath)                                                  {return Files.isRegularFile(toAbsolutePath_(aFilePath));}
        public static boolean   exists  (String aPath)                                                      {return Files.exists(toAbsolutePath_(aPath));}
        public static void      delete  (String aPath)                                  throws IOException  {Files.deleteIfExists(toAbsolutePath_(aPath));} // can delete not exist path
        public static void      copy    (String aSourcePath, String aTargetPath)        throws IOException  {copy(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath));}
        public static void      copy    (InputStream aSourceStream, String aTargetPath) throws IOException  {copy(aSourceStream, toAbsolutePath_(aTargetPath));}
        public static void      copy    (URL aSourceURL, String aTargetPath)            throws IOException  {copy(aSourceURL, toAbsolutePath_(aTargetPath));}
        public static void      move    (String aSourcePath, String aTargetPath)        throws IOException  {move(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath));}
        public static String[]  list    (String aDir)                                                       {return toFile(aDir).list();} // use the File.list not Files.list to get the simple result
        public static void      makeDir (Path aDir)                                     throws IOException  {Files.createDirectories(aDir);} // can mkdir nested
        public static void      copy    (Path aSourcePath, Path aTargetPath)            throws IOException  {validPath(aTargetPath); Files.copy(aSourcePath, aTargetPath, REPLACE_EXISTING);}
        public static void      copy    (InputStream aSourceStream, Path aTargetPath)   throws IOException  {validPath(aTargetPath); Files.copy(aSourceStream, aTargetPath, REPLACE_EXISTING);}
        public static void      copy    (URL aSourceURL, Path aTargetPath)              throws IOException  {try (InputStream tURLStream = aSourceURL.openStream()) {copy(tURLStream, aTargetPath);}}
        public static void      move    (Path aSourcePath, Path aTargetPath)            throws IOException  {validPath(aTargetPath); Files.move(aSourcePath, aTargetPath, REPLACE_EXISTING);}
        
        /** output stuffs */
        public static PrintStream    toPrintStream (String aFilePath, OpenOption... aOptions)   throws IOException  {return new PrintStream(toOutputStream(aFilePath, aOptions));}
        public static OutputStream   toOutputStream(String aFilePath, OpenOption... aOptions)   throws IOException  {return toOutputStream(UT.IO.toAbsolutePath_(aFilePath), aOptions);}
        public static BufferedWriter toWriter      (String aFilePath, OpenOption... aOptions)   throws IOException  {return toWriter(UT.IO.toAbsolutePath_(aFilePath), aOptions);}
        public static BufferedWriter toWriter      (OutputStream aOutputStream)                                     {return new BufferedWriter(new OutputStreamWriter(aOutputStream, StandardCharsets.UTF_8));}
        public static OutputStream   toOutputStream(Path aPath, OpenOption... aOptions)         throws IOException  {validPath(aPath); return Files.newOutputStream(aPath, aOptions);}
        public static BufferedWriter toWriter      (Path aPath, OpenOption... aOptions)         throws IOException  {validPath(aPath); return Files.newBufferedWriter(aPath, aOptions);}
        
        /** input stuffs */
        public static InputStream    toInputStream(String aFilePath)        throws IOException  {return toInputStream(UT.IO.toAbsolutePath_(aFilePath));}
        public static BufferedReader toReader     (String aFilePath)        throws IOException  {return toReader(UT.IO.toAbsolutePath_(aFilePath));}
        public static BufferedReader toReader     (InputStream aInputStream)                    {return new BufferedReader(new InputStreamReader(aInputStream, StandardCharsets.UTF_8));}
        public static BufferedReader toReader     (URL aFileURL)            throws IOException  {return toReader(aFileURL.openStream());}
        public static InputStream    toInputStream(Path aPath)              throws IOException  {return Files.newInputStream(aPath);}
        public static BufferedReader toReader     (Path aPath)              throws IOException  {return Files.newBufferedReader(aPath);}
        
        /** misc stuffs */
        public static File toFile(String aFilePath)                     {return toAbsolutePath_(aFilePath).toFile();}
        public static void validPath(String aPath)  throws IOException  {if (aPath.endsWith("/") || aPath.endsWith("\\")) UT.IO.makeDir(aPath); else validPath(toAbsolutePath_(aPath));}
        public static void validPath(Path aPath)    throws IOException  {Path tParent = aPath.getParent(); if (tParent != null) UT.IO.makeDir(tParent);}
        
        /**
         * extract zip file to directory
         * @author liqa
         */
        public static void zip2dir(String aZipFilePath, String aDir) throws IOException {
            if (!aDir.isEmpty() && !aDir.endsWith("/") && !aDir.endsWith("\\")) aDir += "/";
            makeDir(aDir);
            byte[] tBuffer = new byte[1024];
            try (ZipInputStream tZipInputStream = new ZipInputStream(toInputStream(aZipFilePath))) {
                ZipEntry tZipEntry = tZipInputStream.getNextEntry();
                while (tZipEntry != null) {
                    String tEntryPath = aDir + tZipEntry.getName();
                    if (tZipEntry.isDirectory()) {
                        makeDir(tEntryPath);
                    } else {
                        try (OutputStream tOutputStream = toOutputStream(tEntryPath)) {
                            int length;
                            while ((length = tZipInputStream.read(tBuffer)) > 0) {
                                tOutputStream.write(tBuffer, 0, length);
                            }
                        }
                    }
                    tZipEntry = tZipInputStream.getNextEntry();
                }
            }
        }
        
        
        /**
         * convert between json and map
         * @author liqa
         */
        public static Map<?, ?> json2map(String aFilePath) throws IOException {
            try (Reader tReader = IO.toReader(aFilePath)) {
                return (Map<?, ?>) (new JsonSlurper()).parse(tReader);
            }
        }
        public static void map2json(Map<?, ?> aMap, String aFilePath) throws IOException {
            try (Writer tWriter = IO.toWriter(aFilePath)) {
                (new JsonBuilder(aMap)).writeTo(tWriter);
            }
        }
        
        
        /**
         * save matrix data to csv file
         * @author liqa
         * @param aData the matrix form data to be saved
         * @param aFilePath csv file path to be saved
         * @param aHeads optional headers for the title
         */
        public static void data2csv(double[][] aData, String aFilePath, String... aHeads) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                if (aHeads!=null && aHeads.length>0) tPrinter.println(String.join(",", aHeads));
                for (double[] subData : aData) tPrinter.println(String.join(",", Code.map(Code.asList(subData), String::valueOf)));
            }
        }
        public static void data2csv(IMatrix aData, String aFilePath, String... aHeads) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                if (aHeads!=null && aHeads.length>0) tPrinter.println(String.join(",", aHeads));
                for (IVector subData : aData.rows()) tPrinter.println(String.join(",", Code.map(subData.iterable(), String::valueOf)));
            }
        }
        public static void data2csv(Iterable<? extends Number> aData, String aFilePath) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                for (Number subData : aData) tPrinter.println(subData);
            }
        }
        public static void data2csv(Iterable<? extends Number> aData, String aFilePath, String aHead) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                tPrinter.println(aHead);
                for (Number subData : aData) tPrinter.println(subData);
            }
        }
        public static void data2csv(IVector aData, String aFilePath) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                IDoubleIterator it = aData.iterator();
                while (it.hasNext()) tPrinter.println(it.next());
            }
        }
        public static void data2csv(IVector aData, String aFilePath, String aHead) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                tPrinter.println(aHead);
                IDoubleIterator it = aData.iterator();
                while (it.hasNext()) tPrinter.println(it.next());
            }
        }
        public static void data2csv(IFunc1 aFunc, String aFilePath, String... aHeads) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                if (aHeads!=null && aHeads.length>0) tPrinter.println(String.join(",", aHeads));
                else tPrinter.println(String.join(",", "f", "x"));
                
                IVector tX = aFunc.x();
                IVector tY = aFunc.f();
                int tN = aFunc.Nx();
                for (int i = 0; i < tN; ++i) {
                    tPrinter.println(String.join(",", String.valueOf(tY.get_(i)), String.valueOf(tX.get_(i))));
                }
            }
        }
        
        /**
         * read matrix data from csv file
         * @author liqa
         * @param aFilePath csv file path to read
         * @return a matrix
         */
        public static IMatrix csv2data(String aFilePath) throws IOException {return csv2table(aFilePath).matrix();}
        
        
        /**
         * save table to csv file
         * @author liqa
         * @param aTable the Table to be saved
         * @param aFilePath csv file path to be saved
         */
        public static void table2csv(ITable aTable, String aFilePath) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                if (!aTable.noHead()) tPrinter.println(String.join(",", aTable.heads()));
                for (IVector subData : aTable.rows()) tPrinter.println(String.join(",", Code.map(subData.iterable(), String::valueOf)));
            }
        }
        /**
         * read table from csv file
         * @author liqa
         * @param aFilePath csv file path to read
         * @return table with hand
         */
        public static ITable csv2table(String aFilePath) throws IOException {
            // 现在直接全部读取
            List<String> tLines = readAllLines(aFilePath);
            // 获取行数，末尾空行忽略
            int tLineNum = tLines.size();
            for (int i = tLines.size()-1; i >= 0; --i) {
                if (tLines.get(i).trim().isEmpty()) --tLineNum;
                else break;
            }
            // 需要的参数
            String[] tHand;
            IMatrix tData;
            String[] tTokens;
            // 读取第一行检测是否有头，直接看能否成功粘贴
            tTokens = Texts.splitComma(tLines.get(0));
            IVector tFirstData = null;
            try {tFirstData = Vectors.from(Code.map(tTokens, Double::parseDouble));} catch (Exception ignored) {} // 直接看能否成功粘贴
            if (tFirstData != null) {
                tHand = null;
                tData = RowMatrix.zeros(tLineNum, tFirstData.size());
                tData.row(0).fill(tFirstData);
            } else {
                tHand = tTokens;
                tData = RowMatrix.zeros(tLineNum-1, tHand.length);
            }
            // 遍历读取后续数据
            for (int i = 1; i < tLineNum; ++i) {
                tTokens = Texts.splitComma(tLines.get(i));
                tData.row(tHand==null ? i : i-1).fill(Code.map(tTokens, Double::parseDouble));
            }
            // 返回结果
            return tHand != null ? new Table(tHand, tData) : new Table(tData);
        }
        
        
        /**
         * get URL of the resource
         * @author liqa
         */
        @SuppressWarnings("rawtypes")
        public static URL getResource(Class aClass, String aPath) {
            return aClass.getClassLoader().getResource("assets/" + aClass.getName().replaceAll("\\.", "/") + "/" + aPath);
        }
        public static URL getResource(String aPath) {
            return IO.class.getClassLoader().getResource("assets/" + aPath);
        }
        
        /**
         * <p> use Runtime.exec() to get the working dir </p>
         * <p> it seems like the only way to get the correct working dir in matlab </p>
         * <p> return `System.getProperty("user.home")` if failed in exec </p>
         * @author liqa
         * @return the working dir
         */
        public static String pwd() {
            Process tProcess;
            try {
                tProcess = Runtime.getRuntime().exec(IS_WINDOWS ? "cmd /c cd" : "pwd");
            } catch (IOException e) {
                return System.getProperty("user.home");
            }
            
            String wd;
            try (BufferedReader tReader = new BufferedReader(new InputStreamReader(tProcess.getInputStream()))) {
                tProcess.waitFor();
                wd = tReader.readLine().trim();
            } catch (Exception e) {
                wd = System.getProperty("user.home");
            }
            return wd;
        }
        
        /**
         * check whether the two paths are actually same
         * @author liqa
         */
        public static boolean samePath(String aPath1, String aPath2) {return WORKING_PATH.resolve(aPath1).normalize().equals(WORKING_PATH.resolve(aPath2).normalize());}
        
        /**
         * Right `toAbsolutePath` method,
         * because `toAbsolutePath` in `Paths` will still not work even used `setProperty`
         * @author liqa
         * @param aPath string path, can be relative or absolute
         * @return the Right absolute path
         */
        public static String toAbsolutePath(String aPath) {return toAbsolutePath_(aPath).toString();}
        public static Path toAbsolutePath_(String aPath) {return WORKING_PATH.resolve(aPath);}
        
        // reset the working dir to correct value
        private static Path WORKING_PATH = null;
        private static boolean INITIALIZED = false;
        public static void init() {
            if (INITIALIZED) return;
            INITIALIZED = true;
            // 全局修改换行符为 LF
            System.setProperty("line.separator", "\n");
            // 全局修改工作目录为正确的目录
            String wd = pwd();
            System.setProperty("user.dir", wd);
            WORKING_PATH = Paths.get(wd);
        }
        static {init();}
    }
}
