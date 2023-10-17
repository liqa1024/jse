package jtool.code;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jtool.atom.*;
import jtool.code.collection.AbstractCollections;
import jtool.code.filter.IDoubleFilter;
import jtool.code.filter.IFilter;
import jtool.code.filter.IIndexFilter;
import jtool.code.functional.IOperator1;
import jtool.code.iterator.IHasDoubleIterator;
import jtool.code.task.TaskCall;
import jtool.code.task.TaskRun;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.function.IFunc1;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.Matrices;
import jtool.math.table.ITable;
import jtool.math.table.Tables;
import jtool.math.vector.IVector;
import jtool.math.vector.Vectors;
import jtool.parallel.MergedFuture;
import jtool.parallel.ParforThreadPool;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.yaml.YamlBuilder;
import groovy.yaml.YamlSlurper;
import org.apache.groovy.json.internal.CharScanner;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
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

import static jtool.code.CS.*;
import static jtool.code.CS.Exec.EXE;
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
        public static int randSeed() {return RANDOM.nextInt(MAX_SEED);}
        
        /**
         * Get the random id in URL and Filename safe Base64, 8 length
         * @author liqa
         */
        public static String randID() {
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
        
        
        /** useful methods, wrapper of {@link DefaultGroovyMethods} stuffs */
        @SuppressWarnings("UnusedReturnValue")
        public static <T> T removeLast(List<T> self) {return DefaultGroovyMethods.removeLast(self);}
        public static <T> T last(Deque<T> self) {return DefaultGroovyMethods.last(self);}
        public static <T> T last(List<T> self) {return DefaultGroovyMethods.last(self);}
        public static <T> T last(Iterable<T> self) {return DefaultGroovyMethods.last(self);}
        public static <T> T last(T[] self) {return DefaultGroovyMethods.last(self);}
        public static <T> T first(List<T> self) {return DefaultGroovyMethods.first(self);}
        public static <T> T first(Iterable<T> self) {return DefaultGroovyMethods.first(self);}
        public static <T> T first(T[] self) {return DefaultGroovyMethods.first(self);}
        
        
        /** 保留这些接口方便外部调用使用 */
        @VisibleForTesting public static <T> List<T> merge(T[] aBefore, T[] aAfter) {return AbstractCollections.merge(aBefore, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(T aBefore0,                         T[] aAfter) {return AbstractCollections.merge(aBefore0, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(T aBefore0, T aBefore1,             T[] aAfter) {return AbstractCollections.merge(aBefore0, aBefore1, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(T aBefore0, T aBefore1, T aBefore2, T[] aAfter) {return AbstractCollections.merge(aBefore0, aBefore1, aBefore2, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(T[] aBefore, T aAfter0                      ) {return AbstractCollections.merge(aBefore, aAfter0);}
        @VisibleForTesting public static <T> List<T> merge(T[] aBefore, T aAfter0, T aAfter1           ) {return AbstractCollections.merge(aBefore, aAfter0, aAfter1);}
        @VisibleForTesting public static <T> List<T> merge(T[] aBefore, T aAfter0, T aAfter1, T aAfter2) {return AbstractCollections.merge(aBefore, aAfter0, aAfter1, aAfter2);}
        @VisibleForTesting public static <T> List<T> merge(List<? extends T> aBefore, List<? extends T> aAfter) {return AbstractCollections.merge(aBefore, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(T aBefore0,                         List<? extends T> aAfter) {return AbstractCollections.merge(aBefore0, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(T aBefore0, T aBefore1,             List<? extends T> aAfter) {return AbstractCollections.merge(aBefore0, aBefore1, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(T aBefore0, T aBefore1, T aBefore2, List<? extends T> aAfter) {return AbstractCollections.merge(aBefore0, aBefore1, aBefore2, aAfter);}
        @VisibleForTesting public static <T> List<T> merge(List<? extends T> aBefore, T aAfter0                      ) {return AbstractCollections.merge(aBefore, aAfter0);}
        @VisibleForTesting public static <T> List<T> merge(List<? extends T> aBefore, T aAfter0, T aAfter1           ) {return AbstractCollections.merge(aBefore, aAfter0, aAfter1);}
        @VisibleForTesting public static <T> List<T> merge(List<? extends T> aBefore, T aAfter0, T aAfter1, T aAfter2) {return AbstractCollections.merge(aBefore, aAfter0, aAfter1, aAfter2);}
        @VisibleForTesting public static <T> Iterable<T> merge(Iterable<? extends T> aBefore, Iterable<? extends T> aAfter) {return AbstractCollections.merge(aBefore, aAfter);}
        @VisibleForTesting public static <T> Iterable<T> merge(T aBefore0,                         Iterable<? extends T> aAfter) {return AbstractCollections.merge(aBefore0, aAfter);}
        @VisibleForTesting public static <T> Iterable<T> merge(T aBefore0, T aBefore1,             Iterable<? extends T> aAfter) {return AbstractCollections.merge(aBefore0, aBefore1, aAfter);}
        @VisibleForTesting public static <T> Iterable<T> merge(T aBefore0, T aBefore1, T aBefore2, Iterable<? extends T> aAfter) {return AbstractCollections.merge(aBefore0, aBefore1, aBefore2, aAfter);}
        @VisibleForTesting public static <T> Iterable<T> merge(Iterable<? extends T> aBefore, T aAfter0                      ) {return AbstractCollections.merge(aBefore, aAfter0);}
        @VisibleForTesting public static <T> Iterable<T> merge(Iterable<? extends T> aBefore, T aAfter0, T aAfter1           ) {return AbstractCollections.merge(aBefore, aAfter0, aAfter1);}
        @VisibleForTesting public static <T> Iterable<T> merge(Iterable<? extends T> aBefore, T aAfter0, T aAfter1, T aAfter2) {return AbstractCollections.merge(aBefore, aAfter0, aAfter1, aAfter2);}
        @VisibleForTesting public static <T> Iterable<T> merge(Iterable<? extends Iterable<? extends T>> aNestIterable) {return AbstractCollections.merge(aNestIterable);}
        
        /** 保留这些接口方便外部调用使用 */
        @VisibleForTesting public static <T> Iterable<T> filter(Iterable<? extends T> aIterable, IFilter<? super T> aFilter) {return AbstractCollections.filter(aIterable, aFilter);}
        @VisibleForTesting public static Iterable<Integer> filterInteger(Iterable<Integer> aIndices, IIndexFilter aFilter) {return AbstractCollections.filterInteger(aIndices, aFilter);}
        @VisibleForTesting public static Iterable<Integer> filterInteger(int aSize, IIndexFilter aFilter) {return AbstractCollections.filterInteger(aSize, aFilter);}
        @VisibleForTesting public static Iterable<? extends Number> filterDouble(Iterable<? extends Number> aIterable, final IDoubleFilter aFilter) {return AbstractCollections.filterDouble(aIterable, aFilter);}
        @VisibleForTesting public static IHasDoubleIterator filterDouble(final IHasDoubleIterator aIterable, final IDoubleFilter aFilter) {return AbstractCollections.filterDouble(aIterable, aFilter);}
        
        /** 保留这些接口方便外部调用使用 */
        @VisibleForTesting public static <R, T> Iterable<R>   map(Iterable<T> aIterable,     IOperator1<? extends R, ? super T> aOpt) {return AbstractCollections.map(aIterable, aOpt);}
        @VisibleForTesting public static <R, T> Iterator<R>   map(Iterator<T> aIterator,     IOperator1<? extends R, ? super T> aOpt) {return AbstractCollections.map(aIterator, aOpt);}
        @VisibleForTesting public static <R, T> Collection<R> map(Collection<T> aCollection, IOperator1<? extends R, ? super T> aOpt) {return AbstractCollections.map(aCollection, aOpt);}
        @VisibleForTesting public static <R, T> List<R>       map(List<T> aList,             IOperator1<? extends R, ? super T> aOpt) {return AbstractCollections.map(aList, aOpt);}
        @VisibleForTesting public static <R, T> List<R>       map(T[] aArray,                IOperator1<? extends R, ? super T> aOpt) {return AbstractCollections.map(aArray, aOpt);}
        
        /** 保留这些接口方便外部调用使用 */
        @VisibleForTesting public static List<Double> asList(final double[] aData) {return AbstractCollections.from(aData);}
        @VisibleForTesting public static List<Integer> asList(final int[] aData) {return AbstractCollections.from(aData);}
        @VisibleForTesting public static List<Boolean> asList(final boolean[] aData) {return AbstractCollections.from(aData);}
        
        /** 保留这些接口方便外部调用使用 */
        @VisibleForTesting public static List<Integer> range_(            int aSize           ) {return AbstractCollections.range_(aSize);}
        @VisibleForTesting public static List<Integer> range_(int aStart, int aStop           ) {return AbstractCollections.range_(aStart, aStop);}
        @VisibleForTesting public static List<Integer> range_(int aStart, int aStop, int aStep) {return AbstractCollections.range_(aStart, aStop, aStep);}
        @VisibleForTesting public static List<Integer> range (            int aSize           ) {return AbstractCollections.range (aSize);}
        @VisibleForTesting public static List<Integer> range (int aStart, int aStop           ) {return AbstractCollections.range (aStart, aStop);}
        @VisibleForTesting public static List<Integer> range (int aStart, int aStop, int aStep) {return AbstractCollections.range (aStart, aStop, aStep);}
        
        
        /**
         * Convert IXYZ to XYZ to optimise, result should be read only!
         * @author liqa
         */
        public static XYZ toXYZ(IXYZ aXYZ) {
            return (aXYZ instanceof XYZ) ? (XYZ)aXYZ : new XYZ(aXYZ);
        }
        /**
         * Return new IXYZ for Box usage, consider the constant
         * @author liqa
         */
        public static IXYZ newBox(IXYZ aXYZ) {
            if (aXYZ == BOX_ONE) return BOX_ONE;
            if (aXYZ == BOX_ZERO) return BOX_ZERO;
            return new XYZ(aXYZ);
        }
        /**
         * Convert IComplexDouble to ComplexDouble to optimise, result should be read only!
         * @author liqa
         */
        public static ComplexDouble toComplexDouble(IComplexDouble aComplexDouble) {
            return (aComplexDouble instanceof ComplexDouble) ? (ComplexDouble)aComplexDouble : new ComplexDouble(aComplexDouble);
        }
    }
    
    public static class Exec {
        /** 提供这些接口方便外部调用使用 */
        @VisibleForTesting public static int system(String aCommand) {return EXE.system(aCommand);}
        @VisibleForTesting public static Future<Integer> submitSystem(String aCommand) {return EXE.submitSystem(aCommand);}
        @VisibleForTesting public static List<String> system_str(String aCommand) {return EXE.system_str(aCommand);}
        @VisibleForTesting public static Future<List<String>> submitSystem_str(String aCommand) {return EXE.submitSystem_str(aCommand);}
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
            private final static int TRY_TIMES = 100;
            
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
                        try {Thread.sleep(SYNC_SLEEP_TIME);}
                        catch (InterruptedException e) {return false;}
                    }
                }
                return false;
            }
            @Override public boolean isCancelled() {return mCancelled;}
            @Override public boolean isDone() {return mFinished;}
            @SuppressWarnings("BusyWait")
            @Override public T get() throws InterruptedException {
                while (!mFinished) Thread.sleep(INTERNAL_SLEEP_TIME);
                if (mCancelled) throw new CancellationException();
                return mResult;
            }
            @SuppressWarnings("BusyWait")
            @Override public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, TimeoutException {
                long tic = System.nanoTime();
                while (!mFinished) {
                    Thread.sleep(INTERNAL_SLEEP_TIME);
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
        
        /** {@link IAtomData} 的序列化和反序列化 */
        public static byte[] atomDataXYZ2bytes(IAtomData aAtomData) {
            byte[] rBytes = new byte[DOUBLE_LEN*3 + DOUBLE_LEN*3*aAtomData.atomNum()];
            int tIdx = 0;
            // 模拟盒数据
            IXYZ tBox = aAtomData.box();
            double2bytes(tBox.x(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            double2bytes(tBox.y(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            double2bytes(tBox.z(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            // 原子数据
            for (IAtom tAtom : aAtomData.asList()) {
                double2bytes(tAtom.x(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
                double2bytes(tAtom.y(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
                double2bytes(tAtom.z(), rBytes, tIdx); tIdx+=DOUBLE_LEN;
            }
            return rBytes;
        }
        public static IAtomData bytes2atomDataXYZ(byte[] aBytes) {
            double tX, tY, tZ;
            int tIdx = 0;
            // 获取模拟盒数据
            tX = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            tY = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            tZ = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
            XYZ tBox = new XYZ(tX, tY, tZ);
            // 获取原子数据，这里只有 XYZ 数据
            int tAtomNum = aBytes.length/(DOUBLE_LEN*3) - 2;
            List<Atom> rAtoms = new ArrayList<>(tAtomNum);
            for (int tID = 1; tID <= tAtomNum; ++tID) {
                tX = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
                tY = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
                tZ = bytes2double(aBytes, tIdx); tIdx+=DOUBLE_LEN;
                rAtoms.add(new Atom(tX, tY, tZ, tID));
            }
            // 返回结果
            return new AtomData(rAtoms, tBox);
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
         * @param aIgnoreCase if true, ignore case when comparing characters
         * @return the idx of aLines which contains aContainStr, or aLines.length if not find
         */
        public static int findLineContaining(List<String> aLines, int aStartIdx, String aContainStr, boolean aIgnoreCase) {
            int tIdx = aStartIdx;
            while (tIdx < aLines.size()) {
                if (aIgnoreCase) {
                    if (StringGroovyMethods.containsIgnoreCase(aLines.get(tIdx), aContainStr)) break;
                } else {
                    if (aLines.get(tIdx).contains(aContainStr)) break;
                }
                ++tIdx;
            }
            return tIdx;
        }
        public static int findLineContaining(List<String> aLines, int aStartIdx, String aContainStr) {return findLineContaining(aLines, aStartIdx, aContainStr, false);}
        
        public static String findLineContaining(BufferedReader aReader, String aContainStr, boolean aIgnoreCase) throws IOException {
            String tLine;
            while ((tLine = aReader.readLine()) != null) {
                if (aIgnoreCase) {
                    if (StringGroovyMethods.containsIgnoreCase(tLine, aContainStr)) return tLine;
                } else {
                    if (tLine.contains(aContainStr)) return tLine;
                }
            }
            return null;
        }
        public static String findLineContaining(BufferedReader aReader, String aContainStr) throws IOException {return findLineContaining(aReader, aContainStr, false);}
        
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
         * read all lines from the BufferedReader
         * @author liqa
         */
        public static List<String> readAllLines(BufferedReader aReader) throws IOException {
            List<String> rLines = new ArrayList<>();
            String tLine;
            while ((tLine = aReader.readLine()) != null) rLines.add(tLine);
            return rLines;
        }
        /**
         * read the specified number of lines from the BufferedReader
         * @author liqa
         */
        public static List<String> readLines(BufferedReader aReader, int aNumber) throws IOException {
            List<String> rLines = new ArrayList<>();
            for (int i = 0; i < aNumber; ++i) {
                String tLine = aReader.readLine();
                if (tLine == null) break;
                rLines.add(tLine);
            }
            return rLines;
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
        public static PrintStream    toPrintStream (String aFilePath, OpenOption... aOptions)   throws IOException  {return new PrintStream(new BufferedOutputStream(toOutputStream(aFilePath, aOptions)), false, "UTF-8");}
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
         * convert between yaml and map
         * @author liqa
         */
        public static Map<?, ?> yaml2map(String aFilePath) throws Exception {
            try (Reader tReader = IO.toReader(aFilePath)) {
                return (Map<?, ?>) (new YamlSlurper()).parse(tReader);
            }
        }
        public static void map2yaml(Map<?, ?> aMap, String aFilePath) throws IOException {
            try (Writer tWriter = IO.toWriter(aFilePath)) {
                YamlBuilder tBuilder = new YamlBuilder();
                tBuilder.call(aMap);
                tBuilder.writeTo(tWriter);
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
            List<String> rLines = AbstractCollections.map(Arrays.asList(aData), subData -> String.join(",", AbstractCollections.map(AbstractCollections.from(subData), String::valueOf)));
            if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
            write(aFilePath, rLines);
        }
        public static void data2csv(IMatrix aData, String aFilePath, String... aHeads) throws IOException {
            List<String> rLines = AbstractCollections.map(aData.rows(), subData -> String.join(",", AbstractCollections.map(subData.iterable(), String::valueOf)));
            if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
            write(aFilePath, rLines);
        }
        public static void data2csv(Iterable<? extends Number> aData, String aFilePath) throws IOException {
            write(aFilePath, AbstractCollections.map(aData, String::valueOf));
        }
        public static void data2csv(Iterable<? extends Number> aData, String aFilePath, String aHead) throws IOException {
            write(aFilePath, AbstractCollections.merge(aHead, AbstractCollections.map(aData, String::valueOf)));
        }
        public static void data2csv(IVector aData, String aFilePath) throws IOException {
            write(aFilePath, AbstractCollections.map(aData.iterable(), String::valueOf));
        }
        public static void data2csv(IVector aData, String aFilePath, String aHead) throws IOException {
            write(aFilePath, AbstractCollections.merge(aHead, AbstractCollections.map(aData.iterable(), String::valueOf)));
        }
        public static void data2csv(IFunc1 aFunc, String aFilePath, String... aHeads) throws IOException {
            List<String> rLines = AbstractCollections.map(AbstractCollections.range(aFunc.Nx()), i -> aFunc.get_(i)+","+aFunc.getX(i));
            rLines = AbstractCollections.merge((aHeads!=null && aHeads.length>0) ? String.join(",", aHeads) : "f,x", rLines);
            write(aFilePath, rLines);
        }
        
        /**
         * read matrix data from csv file
         * @author liqa
         * @param aFilePath csv file path to read
         * @return a matrix
         */
        public static IMatrix csv2data(String aFilePath) throws IOException {
            // 现在直接全部读取
            List<String> tLines = readAllLines(aFilePath);
            // 获取行数，末尾空行忽略
            int tLineNum = tLines.size();
            for (int i = tLines.size()-1; i >= 0; --i) {
                if (tLines.get(i).trim().isEmpty()) --tLineNum;
                else break;
            }
            // 需要的参数
            IMatrix rMatrix;
            Iterator<IVector> itRow;
            String[] tTokens;
            // 读取第一行检测是否有头，直接看能否成功粘贴
            tTokens = Texts.splitComma(tLines.get(0));
            IVector tFirstData = null;
            try {tFirstData = Vectors.from(AbstractCollections.map(tTokens, Double::parseDouble));} catch (Exception ignored) {} // 直接看能否成功粘贴
            if (tFirstData != null) {
                rMatrix = Matrices.zeros(tLineNum, tFirstData.size());
                itRow = rMatrix.rows().iterator();
                itRow.next().fill(tFirstData);
            } else {
                rMatrix = Matrices.zeros(tLineNum-1, tTokens.length);
                itRow = rMatrix.rows().iterator();
            }
            // 遍历读取后续数据
            for (int i = 1; i < tLineNum; ++i) {
                tTokens = Texts.splitComma(tLines.get(i));
                itRow.next().fill(AbstractCollections.map(tTokens, Double::parseDouble));
            }
            // 返回结果
            return rMatrix;
        }
        
        
        /**
         * save table to csv file
         * @author liqa
         * @param aTable the Table to be saved
         * @param aFilePath csv file path to be saved
         */
        public static void table2csv(ITable aTable, String aFilePath) throws IOException {
            List<String> rLines = AbstractCollections.map(aTable.rows(), subData -> String.join(",", AbstractCollections.map(subData.iterable(), String::valueOf)));
            rLines = AbstractCollections.merge(String.join(",", aTable.heads()), rLines);
            write(aFilePath, rLines);
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
            ITable rTable;
            Iterator<IVector> itRow;
            String[] tTokens;
            // 读取第一行检测是否有头，直接看能否成功粘贴
            tTokens = Texts.splitComma(tLines.get(0));
            IVector tFirstData = null;
            try {tFirstData = Vectors.from(AbstractCollections.map(tTokens, Double::parseDouble));} catch (Exception ignored) {} // 直接看能否成功粘贴
            if (tFirstData != null) {
                rTable = Tables.zeros(tLineNum, tFirstData.size());
                itRow = rTable.rows().iterator();
                itRow.next().fill(tFirstData);
            } else {
                rTable = Tables.zeros(tLineNum-1, tTokens);
                itRow = rTable.rows().iterator();
            }
            // 遍历读取后续数据
            for (int i = 1; i < tLineNum; ++i) {
                tTokens = Texts.splitComma(tLines.get(i));
                itRow.next().fill(AbstractCollections.map(tTokens, Double::parseDouble));
            }
            // 返回结果
            return rTable;
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
        private static volatile boolean INITIALIZED = false;
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
