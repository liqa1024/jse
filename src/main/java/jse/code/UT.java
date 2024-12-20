package jse.code;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import groovy.transform.stc.SimpleType;
import groovy.yaml.YamlBuilder;
import groovy.yaml.YamlSlurper;
import jse.Main;
import jse.atom.*;
import jse.cache.ByteArrayCache;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.NewCollections;
import jse.code.functional.IDoubleFilter;
import jse.code.functional.IFilter;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IHasDoubleIterator;
import jse.code.iterator.IHasIntIterator;
import jse.code.task.TaskCall;
import jse.code.task.TaskRun;
import jse.code.timer.FixedTimer;
import jse.io.UnicodeReader;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.MathEX;
import jse.math.function.IFunc1;
import jse.math.function.IFunc1Subs;
import jse.math.matrix.ColumnMatrix;
import jse.math.matrix.IMatrix;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.table.ITable;
import jse.math.table.Table;
import jse.math.table.Tables;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jse.parallel.*;
import jse.plot.*;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import me.tongfei.progressbar.wrapped.ProgressBarWrappedIterable;
import net.jafama.FastMath;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import jse.io.CharScanner;
import org.apache.groovy.util.Maps;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.codehaus.groovy.util.CharSequenceReader;
import org.jetbrains.annotations.*;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static jse.code.CS.*;
import static jse.code.Conf.*;
import static jse.code.OS.USER_HOME;
import static jse.code.OS.WORKING_DIR_PATH;

/**
 * @author liqa
 * <p> utils of this project </p>
 */
public class UT {
    
    public static class Code {
        
        /**
         * 打印警告以及栈信息
         * @author liqa
         */
        public static void warning(String aMsg) {
            Warning w = new Warning(aMsg);
            if (!DEBUG) Main.sanitize(w);
            w.printStackTrace(System.err);
        }
        /**
         * 打印简化后的栈信息
         * @author liqa
         */
        public static void printStackTrace(Throwable t) {
            if (!DEBUG) Main.deepSanitize(t);
            t.printStackTrace(System.err);
        }
        
        /**
         * 保留 null 的转换 String
         * @author liqa
         */
        @Contract(value = "null->null; !null -> !null", pure = true)
        public static @Nullable String toString(@Nullable Object aObj) {
            return aObj==null ? null : aObj.toString();
        }
        
        /**
         * 考虑 null 的转为 double
         * @author liqa
         */
        public static double doubleValue(@Nullable Number aNumber) {
            return aNumber==null ? Double.NaN : aNumber.doubleValue();
        }
        
        /**
         * Get the random seed for lammps usage
         * @author liqa
         */
        public static int randSeed() {return RANDOM.nextInt(MAX_SEED) + 1;}
        public static int randSeed(long aSeed) {return new LocalRandom(aSeed).nextInt(MAX_SEED) + 1;}
        
        /**
         * MPI version of {@link #randSeed},
         * will get the same rand seed for all process
         * @author liqa
         */
        public static int randSeed(MPI.Comm aComm, int aRoot) throws MPIException {
            return aComm.bcastI(aComm.rank()==aRoot ? Code.randSeed() : -1, aRoot);
        }
        public static int randSeed(MPI.Comm aComm, int aRoot, long aSeed) throws MPIException {
            return aComm.bcastI(aComm.rank()==aRoot ? Code.randSeed(aSeed) : -1, aRoot);
        }
        
        
        /**
         * 现在改为（小于等于） 8 长度的 Base16 的字符串，
         * 这样和 uniqueID 同步并且可以避免 windows 下不区分大小写的问题
         * <p>
         * 虽然现在长度更低了导致哈希碰撞概率较高，但是实际应该影响不大，
         * 并且也没有高效的方法实现返回 long 的 hashCode
         * <p>
         * 这个修改会修改随机流，导致部分结果会和和旧版本不同
         * @author liqa
         */
        public static String randID() {
            return Integer.toHexString(RANDOM.nextInt());
        }
        public static String randID(long aSeed) {
            return Integer.toHexString(new LocalRandom(aSeed).nextInt());
        }
        
        /**
         * MPI version of {@link #randID},
         * will get the same rand ID for all process
         * @author liqa
         */
        public static String randID(MPI.Comm aComm, int aRoot) throws MPIException {
            return Integer.toHexString(aComm.bcastI(aComm.rank()==aRoot ? RANDOM.nextInt() : 0, aRoot));
        }
        public static String randID(MPI.Comm aComm, int aRoot, long aSeed) throws MPIException {
            return Integer.toHexString(aComm.bcastI(aComm.rank()==aRoot ? new LocalRandom(aSeed).nextInt() : 0, aRoot));
        }
        
        /**
         * Get the unique id in Base16, 8 length
         * @author liqa
         */
        public static String uniqueID(Object... aObjects) {
            return Integer.toHexString(Arrays.hashCode(aObjects));
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
        public static <T> T max(Iterable<T> self) {return DefaultGroovyMethods.max(self);}
        public static <T> T max(T[] self) {return DefaultGroovyMethods.max(self);}
        public static <T> T min(Iterable<T> self) {return DefaultGroovyMethods.min(self);}
        public static <T> T min(T[] self) {return DefaultGroovyMethods.min(self);}
        public static <T> Object sum(Iterable<T> self) {return DefaultGroovyMethods.sum(self);}
        public static Object sum(Object[] self) {return DefaultGroovyMethods.sum(self);}
        
        
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
        @VisibleForTesting public static Iterable<Integer> filterInt(Iterable<Integer> aIndices, IIndexFilter aFilter) {return AbstractCollections.filterInt(aIndices, aFilter);}
        @VisibleForTesting public static IHasIntIterator filterInt(IHasIntIterator aIndices, IIndexFilter aFilter) {return AbstractCollections.filterInt(aIndices, aFilter);}
        @VisibleForTesting public static IHasIntIterator filterInt(int aSize, IIndexFilter aFilter) {return AbstractCollections.filterInt(aSize, aFilter);}
        @VisibleForTesting public static Iterable<? extends Number> filterDouble(Iterable<? extends Number> aIterable, final IDoubleFilter aFilter) {return AbstractCollections.filterDouble(aIterable, aFilter);}
        @VisibleForTesting public static IHasDoubleIterator filterDouble(IHasDoubleIterator aIterable, final IDoubleFilter aFilter) {return AbstractCollections.filterDouble(aIterable, aFilter);}
        
        /** 保留这些接口方便外部调用使用 */
        @VisibleForTesting public static <R, T> Iterable<R>   map(Iterable<T> aIterable,     IUnaryFullOperator<? extends R, ? super T> aOpt) {return AbstractCollections.map(aIterable, aOpt);}
        @VisibleForTesting public static <R, T> Iterator<R>   map(Iterator<T> aIterator,     IUnaryFullOperator<? extends R, ? super T> aOpt) {return AbstractCollections.map(aIterator, aOpt);}
        @VisibleForTesting public static <R, T> Collection<R> map(Collection<T> aCollection, IUnaryFullOperator<? extends R, ? super T> aOpt) {return AbstractCollections.map(aCollection, aOpt);}
        @VisibleForTesting public static <R, T> List<R>       map(List<T> aList,             IUnaryFullOperator<? extends R, ? super T> aOpt) {return AbstractCollections.map(aList, aOpt);}
        @VisibleForTesting public static <R, T> List<R>       map(T[] aArray,                IUnaryFullOperator<? extends R, ? super T> aOpt) {return AbstractCollections.map(aArray, aOpt);}
        
        /** 保留这些接口方便外部调用使用 */
        @VisibleForTesting public static List<Double> asList(double[] aData) {return AbstractCollections.from(aData);}
        @VisibleForTesting public static List<Integer> asList(int[] aData) {return AbstractCollections.from(aData);}
        @VisibleForTesting public static List<Boolean> asList(boolean[] aData) {return AbstractCollections.from(aData);}
        
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
        @VisibleForTesting public static XYZ toXYZ(IXYZ aXYZ) {return XYZ.toXYZ(aXYZ);}
        /**
         * Convert IComplexDouble to ComplexDouble to optimise, result should be read only!
         * @author liqa
         */
        public static ComplexDouble toComplexDouble(IComplexDouble aComplexDouble) {
            return (aComplexDouble instanceof ComplexDouble) ? (ComplexDouble)aComplexDouble : new ComplexDouble(aComplexDouble);
        }
    }
    
    public static class Par {
        /** 改为全局的 pool 缓存来避免总是创建 */
        private static @Nullable ParforThreadPool POOL = null;
        public static @NotNull ParforThreadPool pool(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {
            if (POOL==null || POOL.threadNumber()!=aThreadNum) {
                if (POOL != null) POOL.shutdown();
                POOL = new ParforThreadPool(aThreadNum);
            }
            return POOL;
        }
        private static void shutdownPool_() {
            if (POOL != null) {
                POOL.shutdown(); POOL = null;
            }
        }
        // 在程序结束时关闭 POOL
        static {Main.addGlobalAutoCloseable(UT.Par::shutdownPool_);}
        
        /**
         * parfor for groovy usage
         * @author liqa
         */
        @VisibleForTesting public static void parfor(int aSize, @ClosureParams(value=FromString.class, options={"int", "int,int"}) Closure<?> aGroovyTask) {parfor(aSize, PARFOR_THREAD_NUMBER, aGroovyTask);}
        @VisibleForTesting public static void parfor(int aSize, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, @ClosureParams(value=FromString.class, options={"int", "int,int"}) final Closure<?> aGroovyTask) {
            if (aGroovyTask.getMaximumNumberOfParameters() == 2) {
                pool(aThreadNum).parfor(aSize, (i, threadID) -> aGroovyTask.call(i, threadID));
                return;
            }
            pool(aThreadNum).parfor(aSize, (i, threadID) -> aGroovyTask.call(i));
        }
        /**
         * parwhile for groovy usage
         * @author liqa
         */
        @VisibleForTesting public static void parwhile(ParforThreadPool.IParwhileChecker aChecker, @ClosureParams(value=SimpleType.class, options="int") Closure<?> aGroovyTask) {parwhile(aChecker, PARFOR_THREAD_NUMBER, aGroovyTask);}
        @VisibleForTesting public static void parwhile(ParforThreadPool.IParwhileChecker aChecker, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, @ClosureParams(value=SimpleType.class, options="int") final Closure<?> aGroovyTask) {
            pool(aThreadNum).parwhile(aChecker, (threadID) -> aGroovyTask.call(threadID));
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
        public static <R, T> Future<R> map(final Future<T> aFuture, final IUnaryFullOperator<? extends R, ? super T> aOpt) {
            return new Future<R>() {
                @Override public boolean cancel(boolean mayInterruptIfRunning) {return aFuture.cancel(mayInterruptIfRunning);}
                @Override public boolean isCancelled() {return aFuture.isCancelled();}
                @Override public boolean isDone() {return aFuture.isDone();}
                @Override public R get() throws InterruptedException, ExecutionException {return aOpt.apply(aFuture.get());}
                @Override public R get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {return aOpt.apply(aFuture.get(timeout, unit));}
            };
        }
        
        /**
         * 获取一个 Future 使用携程执行 aSupplier 获取结果；
         * 其中使用新建一个 Thread 而不是 {@link CompletableFuture} 中使用的 {@link ForkJoinPool}；
         * 这样可以避免线程数上限，主要用于执行需要长期监控但是不消耗资源的情况
         * @param aCallable 计算返回值的函数
         * @param <U> 函数的返回类型
         * @return 新的 Future
         */
        public static <U> Future<U> callAsync(Callable<U> aCallable) {return new ThreadFuture<>(aCallable);}
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
            private volatile boolean mCancelled = false;
            private volatile Throwable mThrowable = null;
            private final Thread mThread;
            private ThreadFuture(final Callable<T> aCallable) {
                mThread = new Thread(() -> {
                    try {mResult = aCallable.call();}
                    catch (Throwable t) {mThrowable = t;}
                });
                mThread.start();
            }
            private ThreadFuture(final Supplier<T> aSupplier) {
                mThread = new Thread(() -> {
                    try {mResult = aSupplier.get();}
                    catch (Throwable t) {mThrowable = t;}
                });
                mThread.start();
            }
            private ThreadFuture(final Runnable aRunnable) {
                mThread = new Thread(() -> {
                    try {aRunnable.run();}
                    catch (Throwable t) {mThrowable = t;}
                });
                mThread.start();
            }
            
            @Override public boolean cancel(boolean mayInterruptIfRunning) {
                if (mayInterruptIfRunning && mThread.isAlive()) {
                    mThread.interrupt();
                    for (int i = 0; i < TRY_TIMES; ++i) {
                        if (!mThread.isAlive()) {mCancelled = true; return true;}
                        try {Thread.sleep(SYNC_SLEEP_TIME);}
                        catch (InterruptedException e) {return false;}
                    }
                }
                return false;
            }
            @Override public boolean isCancelled() {return mCancelled;}
            @Override public boolean isDone() {return !mThread.isAlive();}
            @SuppressWarnings("BusyWait")
            @Override public T get() throws InterruptedException, ExecutionException {
                while (mThread.isAlive()) Thread.sleep(INTERNAL_SLEEP_TIME);
                if (mCancelled) throw new CancellationException();
                if (mThrowable != null) throw new ExecutionException(mThrowable);
                return mResult;
            }
            @SuppressWarnings("BusyWait")
            @Override public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                long tic = System.nanoTime();
                while (mThread.isAlive()) {
                    Thread.sleep(INTERNAL_SLEEP_TIME);
                    if (System.nanoTime()-tic >= unit.toNanos(timeout)) throw new TimeoutException();
                }
                if (mCancelled) throw new CancellationException();
                if (mThrowable != null) throw new ExecutionException(mThrowable);
                return mResult;
            }
        }
        
        private final static int BUFFER_SIZE = 1024;
        /**
         * 重定向一个输入流的输出到需要的输出流中，
         * 例如将程序的错误输出重定向到 {@link System#err}
         * @param aFrom 输入流
         * @param aAutoCloseFrom 是否在结束后自动关闭输入流，默认不关闭
         * @param aTo 需要重定向的输出流
         * @param aAutoCloseTo 是否在结束后自动关闭输出流，默认不关闭
         * @return 管理此重定向任务的 {@link Future}
         */
        public static Future<Void> redirectStream(final InputStream aFrom, final boolean aAutoCloseFrom, final OutputStream aTo, final boolean aAutoCloseTo) {
            return callAsync(() -> {
                byte[] tBuffer = ByteArrayCache.getArray(BUFFER_SIZE);
                try {
                    int tLen;
                    while ((tLen = aFrom.read(tBuffer)) != -1) {
                        aTo.write(tBuffer, 0, tLen);
                    }
                } finally {
                    ByteArrayCache.returnArray(tBuffer);
                    if (aAutoCloseFrom) aFrom.close();
                    if (aAutoCloseTo) aTo.close();
                }
                return null;
            });
        }
        public static Future<Void> redirectStream(InputStream aFrom, boolean aAutoCloseFrom, OutputStream aTo) {return redirectStream(aFrom, aAutoCloseFrom, aTo, false);}
        public static Future<Void> redirectStream(InputStream aFrom, OutputStream aTo, boolean aAutoCloseTo) {return redirectStream(aFrom, false, aTo, aAutoCloseTo);}
        public static Future<Void> redirectStream(InputStream aFrom, OutputStream aTo) {return redirectStream(aFrom, false, aTo, false);}
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
            Class<?> aClazz = Class.forName(aClassName);
            return getCallableOfMethod_(aClazz, null, aMethodName, aArgs);
        }
        public static TaskRun          getRunnableOfStaticMethod(              String aClassName, String aMethodName, Object... aArgs) throws NoSuchMethodException, ClassNotFoundException {return getCallableOfStaticMethod(aClassName, aMethodName, aArgs).toRunnable();}
        public static TaskCall<Object> getCallableOfMethod      (final @NotNull Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {return getCallableOfMethod_(aInstance.getClass(), aInstance, aMethodName, aArgs);}
        public static TaskRun          getRunnableOfMethod      (final @NotNull Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {return getCallableOfMethod(aInstance, aMethodName, aArgs).toRunnable();}
        
        public static @Nullable Method findMethod_(Class<?> aClazz, String aMethodName, Object @NotNull... aArgs) {
            Method[] tAllMethods = aClazz.getMethods();
            for (Method tMethod : tAllMethods) if (tMethod.getName().equals(aMethodName)) {
                if (compatible(aArgs, tMethod.getParameterTypes())) return tMethod;
            }
            return null;
        }
        public static @Nullable Constructor<?> findConstructor_(Class<?> aClazz, Object @NotNull... aArgs) {
            Constructor<?>[] tAllConstructors = aClazz.getConstructors();
            for (Constructor<?> tConstructor : tAllConstructors) {
                if (compatible(aArgs, tConstructor.getParameterTypes())) return tConstructor;
            }
            return null;
        }
        private static boolean compatible(Object[] aArgs, Class<?>[] aMethodParameterTypes) {
            if (aArgs.length != aMethodParameterTypes.length) return false;
            for (int i = 0; i < aArgs.length; ++i) if (!compatible(aArgs[i], aMethodParameterTypes[i])) return false;
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
            for (int i = 0; i < aParameterTypes.length; ++i) {
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
        private final static FixedTimer TIMER = new FixedTimer();
        
        public static void tic() {TIMER.reset();}
        public static void toc() {toc("Total");}
        public static void toc(String aMsg) {
            double elapsedTime = TIMER.get();
            TIMER.reset();
            
            double seconds = elapsedTime % 60.0;
            if (elapsedTime < 3.0) {
                System.out.printf("%s time: %.4g sec\n", aMsg, seconds);
                return;
            }
            if (elapsedTime < 60.0) {
                System.out.printf("%s time: %02.2f sec\n", aMsg, seconds);
                return;
            }
            int minutes = MathEX.Code.floor2int(elapsedTime % 3600.0) / 60;
            if (elapsedTime < 3600.0) {
                System.out.printf("%s time: %02d min %02.2f sec\n", aMsg, minutes, seconds);
                return;
            }
            int hours = MathEX.Code.floor2int(elapsedTime / 3600.0);
            System.out.printf("%s time: %02d hour %02d min %02.2f sec\n", aMsg, hours, minutes, seconds);
        }
        public static double toc(boolean aFlag) {
            double elapsedTime = TIMER.get();
            TIMER.reset();
            return elapsedTime;
        }
        
        public static ProgressBarStyle FANCY_ASCII = ProgressBarStyle.builder().block('#').fractionSymbols(" 123456789").rightSideFractionSymbol(' ').build();
        private static @Nullable ProgressBar sProgressBar = null;
        private static synchronized void progressBar_(String aName, long aN, ProgressBarStyle aStyle, PrintStream aConsumer, int aUpdateIntervalMillis, String aUnitName, long aUnitSize, int aMaxRenderedLength, boolean aShowSpeed, ChronoUnit aSpeedUnit) {
            if (sProgressBar != null) {
                sProgressBar.close();
                Main.removeGlobalAutoCloseable(sProgressBar);
            }
            ProgressBarBuilder rBuilder = new ProgressBarBuilder()
                .setTaskName(aName).setInitialMax(aN)
                .setConsumer(new ConsoleProgressBarConsumer(aConsumer, aMaxRenderedLength)) // 这里总是需要重写一下避免乱码问题
                .setUpdateIntervalMillis(aUpdateIntervalMillis)
                .setStyle(aStyle)
                .setUnit(aUnitName, aUnitSize)
                .setMaxRenderedLength(aMaxRenderedLength);
            if (aShowSpeed) rBuilder.showSpeed().setSpeedUnit(aSpeedUnit);
            sProgressBar = rBuilder.build();
            Main.addGlobalAutoCloseable(sProgressBar);
        }
        public static synchronized void progressBar(Map<?, ?> aArgs) {
            progressBar_(Code.toString(Code.getWithDefault(aArgs, "", "TaskName", "taskname", "Name", "name")),
                         ((Number)Code.get(aArgs, "InitialMax", "initialmax", "Max", "max", "N", "n")).longValue(),
                         (ProgressBarStyle)Code.getWithDefault(aArgs, UNICODE_SUPPORT ? ProgressBarStyle.COLORFUL_UNICODE_BLOCK : FANCY_ASCII, "Style", "style", "s"),
                         (PrintStream)Code.getWithDefault(aArgs, PBAR_ERR_STREAM ? System.err : System.out, "Consumer", "consumer", "c"),
                         ((Number)Code.getWithDefault(aArgs, (int)FILE_SYSTEM_SLEEP_TIME_2, "UpdateIntervalMillis", "updateintervalmills", "Update", "update")).intValue(),
                         Code.toString(Code.getWithDefault(aArgs, "", "UnitName", "unitname", "uname")),
                         ((Number)Code.getWithDefault(aArgs, 1, "UnitSize", "unitsize", "usize")).longValue(),
                         ((Number)Code.getWithDefault(aArgs, -1, "MaxRenderedLength", "maxrenderlength", "Length", "length", "l")).intValue(),
                         (Boolean)Code.getWithDefault(aArgs, false, "ShowSpeed", "showspeed", "Speed", "speed"),
                         (ChronoUnit)Code.getWithDefault(aArgs, ChronoUnit.SECONDS, "SpeedUnit", "speedunit")
            );
        }
        public static synchronized void progressBar(String aName, long aN) {
            progressBar_(aName, aN,
                         UNICODE_SUPPORT ? ProgressBarStyle.COLORFUL_UNICODE_BLOCK : FANCY_ASCII,
                         PBAR_ERR_STREAM ? System.err : System.out, // 现在统一默认使用 err 流
                         (int)FILE_SYSTEM_SLEEP_TIME_2,
                         "", 1,
                         -1,
                         false, ChronoUnit.SECONDS
            );
        }
        public static synchronized void progressBar(long aN) {progressBar("", aN);}
        public static synchronized void progressBar(@Nullable String aExtraMsg) {
            if (sProgressBar == null) return;
            if (aExtraMsg != null) sProgressBar.setExtraMessage(aExtraMsg);
            sProgressBar.step();
            if (sProgressBar.getCurrent() >= sProgressBar.getMax()) {
                sProgressBar.close();
                Main.removeGlobalAutoCloseable(sProgressBar);
                sProgressBar = null;
            }
        }
        public static synchronized void progressBar() {progressBar((String)null);}
        @VisibleForTesting public static void pbar(Map<?, ?> aArgs) {progressBar(aArgs);}
        @VisibleForTesting public static void pbar(String aName, long aN) {progressBar(aName, aN);}
        @VisibleForTesting public static void pbar(long aN) {progressBar(aN);}
        @VisibleForTesting public static void pbar(@Nullable String aExtraMsg) {progressBar(aExtraMsg);}
        @VisibleForTesting public static void pbar() {progressBar();}
        
        
        private static <T> Iterable<T> progressBarWrapper_(String aName, Iterable<T> aUnderlying, ProgressBarStyle aStyle, PrintStream aConsumer, int aUpdateIntervalMillis, String aUnitName, long aUnitSize, int aMaxRenderedLength, boolean aShowSpeed, ChronoUnit aSpeedUnit) {
            ProgressBarBuilder rBuilder = new ProgressBarBuilder()
                .setTaskName(aName)
                .setConsumer(new ConsoleProgressBarConsumer(aConsumer, aMaxRenderedLength)) // 这里总是需要重写一下避免乱码问题
                .setUpdateIntervalMillis(aUpdateIntervalMillis)
                .setStyle(aStyle)
                .setUnit(aUnitName, aUnitSize)
                .setMaxRenderedLength(aMaxRenderedLength);
            if (aShowSpeed) rBuilder.showSpeed().setSpeedUnit(aSpeedUnit);
            return new ProgressBarWrappedIterable<>(aUnderlying, rBuilder);
        }
        public static Iterable<?> progressBarWrapper(Map<?, ?> aArgs) {
            return progressBarWrapper_(Code.toString(Code.getWithDefault(aArgs, "", "TaskName", "taskname", "Name", "name")),
                                       (Iterable<?>)Code.get(aArgs, "Underlying", "underlying", "u"),
                                       (ProgressBarStyle)Code.getWithDefault(aArgs, UNICODE_SUPPORT ? ProgressBarStyle.COLORFUL_UNICODE_BLOCK : FANCY_ASCII, "Style", "style", "s"),
                                       (PrintStream)Code.getWithDefault(aArgs, PBAR_ERR_STREAM ? System.err : System.out, "Consumer", "consumer", "c"),
                                       ((Number)Code.getWithDefault(aArgs, (int)FILE_SYSTEM_SLEEP_TIME_2, "UpdateIntervalMillis", "updateintervalmills", "Update", "update")).intValue(),
                                       Code.toString(Code.getWithDefault(aArgs, "", "UnitName", "unitname", "uname")),
                                       ((Number)Code.getWithDefault(aArgs, 1, "UnitSize", "unitsize", "usize")).longValue(),
                                       ((Number)Code.getWithDefault(aArgs, -1, "MaxRenderedLength", "maxrenderlength", "Length", "length", "l")).intValue(),
                                       (Boolean)Code.getWithDefault(aArgs, false, "ShowSpeed", "showspeed", "Speed", "speed"),
                                       (ChronoUnit)Code.getWithDefault(aArgs, ChronoUnit.SECONDS, "SpeedUnit", "speedunit")
            );
        }
        public static <T> Iterable<T> progressBarWrapper(String aName, Iterable<T> aUnderlying) {
            return progressBarWrapper_(aName, aUnderlying,
                                       UNICODE_SUPPORT ? ProgressBarStyle.COLORFUL_UNICODE_BLOCK : FANCY_ASCII,
                                       PBAR_ERR_STREAM ? System.err : System.out, // 现在统一默认使用 err 流
                                       (int)FILE_SYSTEM_SLEEP_TIME_2,
                                       "", 1,
                                       -1,
                                       false, ChronoUnit.SECONDS
            );
        }
        public static <T> Iterable<T> progressBarWrapper(Iterable<T> aUnderlying) {return progressBarWrapper("", aUnderlying);}
        @VisibleForTesting public static Iterable<?> pbarw(Map<?, ?> aArgs) {return progressBarWrapper(aArgs);}
        @VisibleForTesting public static <T> Iterable<T> pbarw(String aName, Iterable<T> aUnderlying) {return progressBarWrapper(aName, aUnderlying);}
        @VisibleForTesting public static <T> Iterable<T> pbarw(Iterable<T> aUnderlying) {return progressBarWrapper(aUnderlying);}
    }
    
    /** 序列化和反序列化的一些方法 */
    public static class Serial {
        /** 合并 int */
        public static long combineI(int aI1, int aI2) {return ((long)aI1 & 0xffffffffL) | (long)aI2 << 32;}
        /** 将 long 拆分成 int */
        public static int toIntL(long aL, @Range(from = 0, to = 1) int aIdx) {return (int)(aL >> (aIdx<<5));}
        
        /** 合并 boolean */
        public static byte combineZ(boolean aZ1)                                                                                            {return (byte)(aZ1?1:0);}
        public static byte combineZ(boolean aZ1, boolean aZ2)                                                                               {return (byte)((aZ1?1:0) | (aZ2?2:0));}
        public static byte combineZ(boolean aZ1, boolean aZ2, boolean aZ3)                                                                  {return (byte)((aZ1?1:0) | (aZ2?2:0) | (aZ3?4:0));}
        public static byte combineZ(boolean aZ1, boolean aZ2, boolean aZ3, boolean aZ4)                                                     {return (byte)((aZ1?1:0) | (aZ2?2:0) | (aZ3?4:0) | (aZ4?8:0));}
        public static byte combineZ(boolean aZ1, boolean aZ2, boolean aZ3, boolean aZ4, boolean aZ5)                                        {return (byte)((aZ1?1:0) | (aZ2?2:0) | (aZ3?4:0) | (aZ4?8:0) | (aZ5?16:0));}
        public static byte combineZ(boolean aZ1, boolean aZ2, boolean aZ3, boolean aZ4, boolean aZ5, boolean aZ6)                           {return (byte)((aZ1?1:0) | (aZ2?2:0) | (aZ3?4:0) | (aZ4?8:0) | (aZ5?16:0) | (aZ6?32:0));}
        public static byte combineZ(boolean aZ1, boolean aZ2, boolean aZ3, boolean aZ4, boolean aZ5, boolean aZ6, boolean aZ7)              {return (byte)((aZ1?1:0) | (aZ2?2:0) | (aZ3?4:0) | (aZ4?8:0) | (aZ5?16:0) | (aZ6?32:0) | (aZ7?64:0));}
        public static byte combineZ(boolean aZ1, boolean aZ2, boolean aZ3, boolean aZ4, boolean aZ5, boolean aZ6, boolean aZ7, boolean aZ8) {return (byte)((aZ1?1:0) | (aZ2?2:0) | (aZ3?4:0) | (aZ4?8:0) | (aZ5?16:0) | (aZ6?32:0) | (aZ7?64:0) | (aZ8?128:0));}
        /** 将 byte 拆分成 boolean */
        public static boolean toBooleanB(byte aB, @Range(from = 0, to = 7) int aIdx) {return (aB & (byte)(1<<aIdx)) != 0;}
        
        
        
        
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
        public static void double2bytes(double aD, byte[] rBytes, final int aPos) {long2bytes(Double.doubleToLongBits(aD), rBytes, aPos);}
        public static double bytes2double(byte[] aBytes, final int aPos) {return Double.longBitsToDouble(bytes2long(aBytes, aPos));}
        
        
        public static byte[] str2bytes(String aStr) {return aStr.getBytes(StandardCharsets.UTF_8);}
        public static String bytes2str(byte[] aBytes) {return new String(aBytes, StandardCharsets.UTF_8);}
        public static String bytes2str(byte[] aBytes, int aPos, int aLen) {return new String(aBytes, aPos, aLen, StandardCharsets.UTF_8);}
        
        
        public static byte[] int2bytes(int aI) {
            byte[] rBytes = new byte[Integer.BYTES];
            int2bytes(aI, rBytes, 0);
            return rBytes;
        }
        public static int bytes2int(byte[] aBytes) {
            return bytes2int(aBytes, 0);
        }
        
        public static byte[] long2bytes(long aL) {
            byte[] rBytes = new byte[Long.BYTES];
            long2bytes(aL, rBytes, 0);
            return rBytes;
        }
        public static long bytes2long(byte[] aBytes) {
            return bytes2long(aBytes, 0);
        }
        public static byte[] double2bytes(double aD) {return long2bytes(Double.doubleToLongBits(aD));}
        public static double bytes2double(byte[] aBytes) {return Double.longBitsToDouble(bytes2long(aBytes));}
        
        /** {@link IAtomData} 的序列化和反序列化 */
        @Deprecated public static byte[] atomDataXYZ2bytes(IAtomData aAtomData) {
            byte[] rBytes = new byte[Double.BYTES*3 + Double.BYTES*3*aAtomData.atomNumber()];
            int tIdx = 0;
            // 模拟盒数据
            IXYZ tBox = aAtomData.box();
            double2bytes(tBox.x(), rBytes, tIdx); tIdx+=Double.BYTES;
            double2bytes(tBox.y(), rBytes, tIdx); tIdx+=Double.BYTES;
            double2bytes(tBox.z(), rBytes, tIdx); tIdx+=Double.BYTES;
            // 原子数据
            for (IAtom tAtom : aAtomData.atoms()) {
                double2bytes(tAtom.x(), rBytes, tIdx); tIdx+=Double.BYTES;
                double2bytes(tAtom.y(), rBytes, tIdx); tIdx+=Double.BYTES;
                double2bytes(tAtom.z(), rBytes, tIdx); tIdx+=Double.BYTES;
            }
            return rBytes;
        }
        @Deprecated public static IAtomData bytes2atomDataXYZ(byte[] aBytes) {
            double tX, tY, tZ;
            int tIdx = 0;
            // 获取模拟盒数据
            tX = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
            tY = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
            tZ = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
            // 获取原子数据，这里只有 XYZ 数据
            int tAtomNum = (aBytes.length - Double.BYTES*3) / (Double.BYTES*3);
            List<Atom> rAtoms = new ArrayList<>(tAtomNum);
            for (int i = 0; i < tAtomNum; ++i) {
                tX = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
                tY = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
                tZ = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
                rAtoms.add(new Atom(tX, tY, tZ));
            }
            // 返回结果
            return new AtomData(rAtoms, 1, new Box(tX, tY, tZ));
        }
        
        /** {@link IAtomData} 的序列化和反序列化 */
        @Deprecated public static byte[] atomDataXYZType2bytes(IAtomData aAtomData) {
            byte[] rBytes = new byte[Double.BYTES*3 + Integer.BYTES + (Double.BYTES*3 + Integer.BYTES)*aAtomData.atomNumber()];
            int tIdx = 0;
            // 模拟盒数据
            IXYZ tBox = aAtomData.box();
            double2bytes(tBox.x(), rBytes, tIdx); tIdx+=Double.BYTES;
            double2bytes(tBox.y(), rBytes, tIdx); tIdx+=Double.BYTES;
            double2bytes(tBox.z(), rBytes, tIdx); tIdx+=Double.BYTES;
            // 原子种类数目信息
            int2bytes(aAtomData.atomTypeNumber(), rBytes, tIdx); tIdx+=Integer.BYTES;
            // 原子数据
            for (IAtom tAtom : aAtomData.atoms()) {
                double2bytes(tAtom.x(), rBytes, tIdx); tIdx+=Double.BYTES;
                double2bytes(tAtom.y(), rBytes, tIdx); tIdx+=Double.BYTES;
                double2bytes(tAtom.z(), rBytes, tIdx); tIdx+=Double.BYTES;
                int2bytes (tAtom.type(), rBytes, tIdx); tIdx+=Integer.BYTES;
            }
            return rBytes;
        }
        @Deprecated public static IAtomData bytes2atomDataXYZType(byte[] aBytes) {
            double tX, tY, tZ;
            int tType;
            int tIdx = 0;
            // 获取模拟盒数据
            tX = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
            tY = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
            tZ = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
            // 获取原子种类数目信息
            int tAtomTypeNum = bytes2int(aBytes, tIdx); tIdx+=Integer.BYTES;
            // 获取原子数据，这里只有 XYZ 数据
            int tAtomNum = (aBytes.length - Double.BYTES*3 - Integer.BYTES) / (Double.BYTES*3 + Integer.BYTES);
            List<Atom> rAtoms = new ArrayList<>(tAtomNum);
            for (int i = 0; i < tAtomNum; ++i) {
                tX = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
                tY = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
                tZ = bytes2double(aBytes, tIdx); tIdx+=Double.BYTES;
                tType = bytes2int(aBytes, tIdx); tIdx+=Integer.BYTES;
                rAtoms.add(new Atom(tX, tY, tZ, tType));
            }
            // 返回结果
            return new AtomData(rAtoms, tAtomTypeNum, new Box(tX, tY, tZ));
        }
    }
    
    public static class Text {
        
        public static Reader toReader_(CharSequence aStr) {return new CharSequenceReader(aStr);}
        public static BufferedReader toReader(CharSequence aStr) {return new BufferedReader(toReader_(aStr));}
        public static void eachLine(final CharSequence aStr, final Consumer<String> aCon) throws IOException {
            try (BufferedReader tReader = toReader(aStr)) {
                String tLine;
                while ((tLine = tReader.readLine()) != null) aCon.accept(tLine);
            }
        }
        public static void eachLine(final CharSequence aStr, final BiConsumer<String, Integer> aCon) throws IOException {
            try (BufferedReader tReader = toReader(aStr)) {
                int tLineNum = 0;
                String tLine;
                while ((tLine = tReader.readLine()) != null) {
                    aCon.accept(tLine, tLineNum);
                    ++tLineNum;
                }
            }
        }
        
        public static String[] toArray(Collection<? extends CharSequence> aLines) {
            String[] rArray = new String[aLines.size()];
            int i = 0;
            for (CharSequence tStr : aLines) {
                rArray[i] = Code.toString(tStr);
                ++i;
            }
            return rArray;
        }
        
        /**
         * Convert a prob value to percent String
         * @author liqa
         */
        public static String percent(double aProb) {
            return String.format("%.2f", aProb*100) + "%";
        }
        
        /**
         * 重复给定 char，照搬 {@code me.tongfei.progressbar.Util} 中的方法
         * @author Tongfei Chen, liqa
         */
        public static String repeat(char aChar, int aNum) {
            if (aNum <= 0) return "";
            char[] tChars = new char[aNum];
            Arrays.fill(tChars, aChar);
            return new String(tChars);
        }
        
        /**
         * 重复给定 String，使用类似 Groovy 中对于 String 的乘法的方法
         * @author liqa
         */
        public static String repeat(CharSequence aStr, int aNum) {
            if (aNum <= 0) return "";
            StringBuilder rStr = new StringBuilder(aStr);
            for (int i = 1; i < aNum; ++i) {
                rStr.append(aStr);
            }
            return rStr.toString();
        }
        
        
        public static boolean firstCharIsDigitDecimal(int aChar) {
            switch(aChar) {
            case '-': case '+': case '.': {
                return true;
            }
            default: {
                return CharScanner.isDigit(aChar);
            }}
        }
        /**
         * 将单个字符串转为 Number 值，如果转换失败则返回 {@code null}
         * @author liqa
         */
        public static @Nullable Number str2number(String aStr) {
            // 先直接转 char[]，适配 groovy-json 的 CharScanner
            char[] tChar = aStr.toCharArray();
            // 先判断开头，这样可以避免抛出错误带来的性能损失
            if (!firstCharIsDigitDecimal(tChar[0])) return null;
            // 但一般还是使用 try，这样避免意外的情况
            try {return CharScanner.parseJsonNumber(tChar);}
            catch (Exception ignored) {}
            return null;
        }
        
        public static int findNoBlankIndex(String aStr, int aStart) {
            final int tLen = aStr.length();
            int c;
            for (; aStart < tLen; ++aStart) {
                c = aStr.charAt(aStart);
                if (c > 32) {
                    return aStart;
                }
            }
            return -1;
        }
        public static int findBlankIndex(String aStr, int aStart) {
            final int tLen = aStr.length();
            int c;
            for (; aStart < tLen; ++aStart) {
                c = aStr.charAt(aStart);
                if (c <= 32) {
                    return aStart;
                }
            }
            return -1;
        }
        
        /**
         * 针对 lammps 等软件输出文件中存在的使用空格分割的数据专门优化的读取操作，
         * 使用 groovy-json 中现成的 parseDouble 等方法，总体比直接 split 并用 java 的 parseDouble 快一倍以上；
         * <p>
         * 现在兼容考虑逗号分割的情况，并同时运用在 csv 的读取上
         * <p>
         * 现在读取失败会存为 {@link Double#NaN} 而不是抛出错误
         * @author liqa
         */
        public static Vector str2data(String aStr, int aLength) {
            // 不足的数据现在默认为 NaN
            Vector rData = Vectors.NaN(aLength);
            // 先直接转 char[]，适配 groovy-json 的 CharScanner
            char[] tChar = aStr.toCharArray();
            // 直接遍历忽略空格，获取开始和末尾，然后 parseDouble
            int tFrom = CharScanner.skipWhiteSpace(tChar, 0, tChar.length);
            int tIdx = 0;
            boolean tHasComma = false;
            for (int i = tFrom; i < tChar.length; ++i) {
                int tCharCode = tChar[i];
                if (tFrom < 0) {
                    if (tCharCode > 32) {
                        if (tCharCode == 44) {
                            if (tHasComma) {
                                ++tIdx;
                                if (tIdx == aLength) return rData;
                            } else {
                                tHasComma = true;
                            }
                        } else {
                            tHasComma = false;
                            tFrom = i;
                        }
                    }
                } else {
                    if (tCharCode<=32 || tCharCode==44) {
                        if (tCharCode == 44) tHasComma = true;
                        try {rData.set(tIdx, CharScanner.parseDouble(tChar, tFrom, i));}
                        catch (Exception ignored) {}
                        tFrom = -1;
                        ++tIdx;
                        if (tIdx == aLength) return rData;
                    }
                }
            }
            // 最后一个数据
            if (tFrom >= 0 && tFrom < tChar.length) {
                try {rData.set(tIdx, CharScanner.parseDouble(tChar, tFrom, tChar.length));}
                catch (Exception ignored) {}
            }
            return rData;
        }
        
        /** useful methods, wrapper of {@link StringGroovyMethods} stuffs */
        @Contract("null -> true")
        public static boolean isBlank(final CharSequence self) {
            if (self == null) return true;
            return BLANKS_OR_EMPTY.matcher(self).matches();
        }
        public static boolean containsIgnoreCase(final CharSequence self, final CharSequence searchString) {return StringGroovyMethods.containsIgnoreCase(self, searchString);}
        
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
                    if (containsIgnoreCase(aLines.get(tIdx), aContainStr)) break;
                } else {
                    if (aLines.get(tIdx).contains(aContainStr)) break;
                }
                ++tIdx;
            }
            return tIdx;
        }
        public static int findLineContaining(List<String> aLines, int aStartIdx, String aContainStr) {return findLineContaining(aLines, aStartIdx, aContainStr, false);}
        
        public static @Nullable String findLineContaining(BufferedReader aReader, String aContainStr, boolean aIgnoreCase) throws IOException {
            String tLine;
            while ((tLine = aReader.readLine()) != null) {
                if (aIgnoreCase) {
                    if (containsIgnoreCase(tLine, aContainStr)) return tLine;
                } else {
                    if (tLine.contains(aContainStr)) return tLine;
                }
            }
            return null;
        }
        public static @Nullable String findLineContaining(BufferedReader aReader, String aContainStr) throws IOException {return findLineContaining(aReader, aContainStr, false);}
        
        
        /**
         * Splits a string separated by blank characters into multiple strings
         * <p> will automatically ignores multiple spaces and the beginning and end spaces </p>
         * @author liqa
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitBlank(String aStr) {
            return BLANKS.split(aStr.trim(), -1);
        }
        
        
        /**
         * Splits a string separated by comma(",") characters into multiple strings
         * <p> will automatically ignores multiple spaces </p>
         * @author liqa
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitComma(String aStr) {
            return COMMA.split(aStr.trim(), -1);
        }
        
        /**
         * 匹配使用空格分割或者逗号（{@code ,}）分割的字符串，可以出现混合
         * <p> 会自动忽略多余的空格 </p>
         * @author liqa
         * @param aStr input string
         * @return the split sting in array
         */
        @ApiStatus.Experimental
        public static String[] splitStr(String aStr) {
            return COMMA_OR_BLANKS.split(aStr.trim(), -1);
        }
        
        /**
         * Java version of splitNodeList
         * @author liqa
         * @param aRawNodeList input raw node list from $SLURM_JOB_NODELIST
         * @return split node list
         */
        public static List<String> splitNodeList(String aRawNodeList) {
            List<String> rOutput = new ArrayList<>();
            
            int tLen = aRawNodeList.length();
            boolean tInBlock = false;
            int tStart = 0;
            for (int i = 0; i < tLen; ++i) {
                if (tInBlock) {
                    if (aRawNodeList.charAt(i) == ']') tInBlock = false;
                    continue;
                }
                if (aRawNodeList.charAt(i) == '[') {
                    tInBlock = true;
                    continue;
                }
                if (aRawNodeList.charAt(i) == ',') {
                    splitNodeList_(aRawNodeList.substring(tStart, i), rOutput);
                    tStart = i+1;
                }
            }
            splitNodeList_(aRawNodeList.substring(tStart, tLen), rOutput);
            
            return rOutput;
        }
        private static void splitNodeList_(String aSubRawNodeList, List<String> rNodeList) {
            // check for "[", "]"
            int tListStart = aSubRawNodeList.indexOf("[");
            if (tListStart < 0) {
                rNodeList.add(aSubRawNodeList);
                return;
            }
            String tHeadStr = aSubRawNodeList.substring(0, tListStart);
            String tListStr = aSubRawNodeList.substring(tListStart+1, aSubRawNodeList.length()-1);
            
            // Split the string by comma
            String[] tArray = tListStr.split(",");
            
            // Range of numbers
            Pattern tPattern = Pattern.compile("([0-9]+)-([0-9]+)");
            // Loop through each range and generate the numbers
            for (String tRange : tArray) {
                Matcher tMatcher = tPattern.matcher(tRange);
                if (tMatcher.find()) {
                    String tStartStr = tMatcher.group(1);
                    int tMinLen = tStartStr.length();
                    int tStart = Integer.parseInt(tStartStr);
                    int tEnd = Integer.parseInt(tMatcher.group(2));
                    for (int i = tStart; i <= tEnd; ++i) {
                        rNodeList.add(String.format("%s%0"+tMinLen+"d", tHeadStr, i));
                    }
                } else {
                    // Single number
                    rNodeList.add(tHeadStr + tRange);
                }
            }
        }
        
        
        /**
         * convert between json and map
         * @author liqa
         */
        public static Map<?, ?> json2map(String aText) {
            return (Map<?, ?>) (new JsonSlurper()).parseText(aText);
        }
        public static String map2json(Map<?, ?> aMap) {
            return map2json(aMap, false);
        }
        public static String map2json(Map<?, ?> aMap, boolean aPretty) {
            JsonBuilder tBuilder = new JsonBuilder();
            tBuilder.call(aMap);
            return aPretty ? tBuilder.toPrettyString() : tBuilder.toString();
        }
        /**
         * convert between yaml and map
         * @author liqa
         */
        public static Map<?, ?> yaml2map(String aText) {
            return (Map<?, ?>) (new YamlSlurper()).parseText(aText);
        }
        public static String map2yaml(Map<?, ?> aMap) {
            YamlBuilder tBuilder = new YamlBuilder();
            tBuilder.call(aMap);
            return tBuilder.toString();
        }
    }
    
    public static class IO {
        private final static int BUFFER_SIZE = 8192;
        
        /**
         * 内部使用，判断一个文件夹路径 aDir 是否符合内部使用的格式，
         * 内部使用时需要 aDir 的结尾为 / 或者为空，保证可以直接统一通过 + 文件名
         * 的方式来获取路径
         * @author liqa
         */
        @ApiStatus.Internal @Contract(pure = true) public static boolean isInternalValidDir(@NotNull String aDir) {
            return aDir.isEmpty() || aDir.endsWith("/") || aDir.endsWith("\\");
        }
        /**
         * 内部使用，将一个文件夹路径 aDir 转换为符合内部使用的格式
         * @author liqa
         */
        @ApiStatus.Internal @CheckReturnValue @Contract(pure = true) public static String toInternalValidDir(@NotNull String aDir) {
            return isInternalValidDir(aDir) ? aDir : (aDir+"/");
        }
        
        
        /**
         * Wrapper of {@link Files#write}
         * @author liqa
         * @param aFilePath File to write
         * @param aLines Iterable String or String[] to be written
         * @throws IOException when fail
         */
        public static void write(String aFilePath, String...                        aLines)                         throws IOException {write(aFilePath, aLines, ZL_OO);}
        public static void write(String aFilePath, Iterable<? extends CharSequence> aLines)                         throws IOException {write(aFilePath, aLines, ZL_OO);}
        public static void write(String aFilePath, String                            aLine)                         throws IOException {write(aFilePath, aLine, ZL_OO);}
        public static void write(String aFilePath, byte[]                            aData)                         throws IOException {write(aFilePath, aData, ZL_OO);}
        public static void write(String aFilePath, String[]                         aLines, OpenOption... aOptions) throws IOException {write(aFilePath, AbstractCollections.from(aLines), aOptions);}
        public static void write(String aFilePath, Iterable<? extends CharSequence> aLines, OpenOption... aOptions) throws IOException {write(toAbsolutePath_(aFilePath), aLines, aOptions);}
        public static void write(String aFilePath, String                            aLine, OpenOption... aOptions) throws IOException {write(aFilePath, Collections.singletonList(aLine), aOptions);}
        public static void write(String aFilePath, byte[]                            aData, OpenOption... aOptions) throws IOException {write(toAbsolutePath_(aFilePath), aData, aOptions);}
        public static void write(Path       aPath, byte[]                            aData, OpenOption... aOptions) throws IOException {validPath(aPath); Files.write(aPath, aData, aOptions);}
        public static void write(Path       aPath, String                            aLine, OpenOption... aOptions) throws IOException {write(aPath, Collections.singletonList(aLine), aOptions);}
        public static void write(Path       aPath, Iterable<? extends CharSequence> aLines, OpenOption... aOptions) throws IOException {
            validPath(aPath);
            // 使用 UT.IO 中的 stream 统一使用 LF 换行符
            try (IWriteln tWriteln = toWriteln(aPath, aOptions)) {
                for (CharSequence tLine: aLines) {tWriteln.writeln(tLine);}
            }
        }
        /**
         * Wrapper of {@link Files#writeString}
         * @author liqa
         * @param aFilePath File to write
         * @param aText String to be written
         * @throws IOException when fail
         */
        public static void writeText(String aFilePath, String aText) throws IOException {writeText(aFilePath, aText, ZL_OO);}
        public static void writeText(String aFilePath, String aText, OpenOption... aOptions) throws IOException {writeText(toAbsolutePath_(aFilePath), aText, aOptions);}
        public static void writeText(Path aPath, String aText, OpenOption... aOptions) throws IOException {
            validPath(aPath);
            // 现在改为直接使用 BufferedWriter 写入整个 String，保证编码格式为 UTF-8，并且内部也会自动分 buffer 处理字符串
            try (BufferedWriter tWriter = toWriter(aPath, aOptions)) {
                tWriter.write(aText);
            }
        }
        
        /**
         * Wrapper of {@link Files#readAllBytes}
         * @author liqa
         * @param aFilePath File to read
         * @return array of byte
         * @throws IOException when fail
         */
        public static byte[] readAllBytes(String aFilePath) throws IOException {return Files.readAllBytes(toAbsolutePath_(aFilePath));}
        /**
         * read all lines from the File
         * @author liqa
         * @param aFilePath File to read
         * @return lines of String
         * @throws IOException when fail
         */
        public static List<String> readAllLines(String aFilePath) throws IOException {
            try (BufferedReader tReader = toReader(aFilePath)) {
                return readAllLines(tReader);
            }
        }
        /**
         * read the specified number of lines from the File
         * @author liqa
         */
        public static List<String> readLines(String aFilePath, int aNumber) throws IOException {
            try (BufferedReader tReader = toReader(aFilePath)) {
                return readLines(tReader, aNumber);
            }
        }
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
         * jdk8 version of {@link Files#readString}
         * @author liqa
         * @param aFilePath File to read
         * @return lines of String
         * @throws IOException when fail
         */
        public static String readAllText(String aFilePath) throws IOException {
            try (BufferedReader tReader = toReader(aFilePath)) {
                return IOGroovyMethods.getText(tReader);
            }
        }
        
        
        /**
         * remove the Directory, will remove the subdirectories recursively
         * @author liqa
         * @param aDir the Directory will be removed
         */
        @VisibleForTesting public static void rmdir(String aDir) throws IOException {removeDir(aDir);}
        public static void removeDir(String aDir) throws IOException {
            aDir = toInternalValidDir(aDir);
            if (!isDir(aDir)) return;
            removeDir_(aDir);
        }
        private static void removeDir_(String aDir) throws IOException {
            for (String tName : list(aDir)) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tFileOrDir = aDir+tName;
                if (isDir(tFileOrDir)) {removeDir_(tFileOrDir+"/");}
                else if (isFile(tFileOrDir)) {delete(tFileOrDir);}
            }
            delete(aDir);
        }
        
        /**
         * 递归的复制一个目录，这里使用字符串拼接的方法实现，而不是
         * {@link Files#walkFileTree}
         * @author liqa
         */
        @VisibleForTesting public static void cpdir(String aSourceDir, String aTargetDir) throws IOException {copyDir(aSourceDir, aTargetDir);}
        public static void copyDir(String aSourceDir, String aTargetDir) throws IOException {
            aSourceDir = toInternalValidDir(aSourceDir);
            aTargetDir = toInternalValidDir(aTargetDir);
            if (!exists(aSourceDir)) throw new NoSuchFileException(toAbsolutePath(aSourceDir));
            if (!isDir(aSourceDir)) throw new NotDirectoryException(toAbsolutePath(aSourceDir));
            copyDir_(aSourceDir, aTargetDir);
        }
        private static void copyDir_(String aSourceDir, String aTargetDir) throws IOException {
            makeDir(aTargetDir);
            for (String tName : list(aSourceDir)) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tSourceFileOrDir = aSourceDir+tName;
                String tTargetFileOrDir = aTargetDir+tName;
                if (isDir(tSourceFileOrDir)) {copyDir_(tSourceFileOrDir+"/", tTargetFileOrDir+"/");}
                else if (isFile(tSourceFileOrDir)) {copy(tSourceFileOrDir, tTargetFileOrDir);}
            }
        }
        
        
        /** useful methods, wrapper of {@link Files} stuffs */
        @VisibleForTesting
        public static void      mkdir   (String aDir)                                   throws IOException {makeDir(aDir);} // can mkdir nested
        public static void      makeDir (String aDir)                                   throws IOException {makeDir(toAbsolutePath_(aDir));} // can mkdir nested
        @VisibleForTesting
        public static boolean   isdir   (String aDir)                                                      {return isDir(aDir);}
        public static boolean   isDir   (String aDir)                                                      {return Files.isDirectory(toAbsolutePath_(aDir));}
        @VisibleForTesting
        public static boolean   isfile  (String aFilePath)                                                 {return isFile(aFilePath);}
        public static boolean   isFile  (String aFilePath)                                                 {return Files.isRegularFile(toAbsolutePath_(aFilePath));}
        
        public static boolean   exists  (String aPath)                                                     {return Files.exists(toAbsolutePath_(aPath));}
        public static void      delete  (String aPath)                                  throws IOException {Files.deleteIfExists(toAbsolutePath_(aPath));} // can delete not exist path
        public static void      copy    (String aSourcePath, String aTargetPath)        throws IOException {copy(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath));}
        public static void      copy    (InputStream aSourceStream, String aTargetPath) throws IOException {copy(aSourceStream, toAbsolutePath_(aTargetPath));}
        public static void      copy    (URL aSourceURL, String aTargetPath)            throws IOException {copy(aSourceURL, toAbsolutePath_(aTargetPath));}
        public static void      move    (String aSourcePath, String aTargetPath)        throws IOException {move(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath));}
        public static void      makeDir (Path aDir)                                     throws IOException {Files.createDirectories(aDir);} // can mkdir nested
        public static void      copy    (Path aSourcePath, Path aTargetPath)            throws IOException {validPath(aTargetPath); Files.copy(aSourcePath, aTargetPath, REPLACE_EXISTING);}
        public static void      copy    (InputStream aSourceStream, Path aTargetPath)   throws IOException {validPath(aTargetPath); Files.copy(aSourceStream, aTargetPath, REPLACE_EXISTING);}
        public static void      copy    (URL aSourceURL, Path aTargetPath)              throws IOException {try (InputStream tURLStream = aSourceURL.openStream()) {copy(tURLStream, aTargetPath);}}
        public static void      move    (Path aSourcePath, Path aTargetPath)            throws IOException {validPath(aTargetPath); Files.move(aSourcePath, aTargetPath, REPLACE_EXISTING);}
        /** use the {@link File#list} not {@link Files#list} to get the simple result */
        public static String @NotNull[] list(String aDir) throws IOException {
            String[] tList = toFile(aDir).list();
            if (tList==null) throw new IOException("Fail to det list of \""+aDir+"\"");
            return tList;
        }
        /** map (filterLines) */
        public static void      map     (String        aSourcePath, String aTargetPath, IUnaryFullOperator<? extends CharSequence, ? super String> aOpt) throws IOException  {try (BufferedReader tReader = toReader(aSourcePath); IWriteln tWriter = toWriteln(aTargetPath)) {map(tReader, tWriter, aOpt);}}
        public static void      map     (InputStream aSourceStream, String aTargetPath, IUnaryFullOperator<? extends CharSequence, ? super String> aOpt) throws IOException  {try (BufferedReader tReader = toReader(aSourceStream); IWriteln tWriter = toWriteln(aTargetPath)) {map(tReader, tWriter, aOpt);}}
        public static void      map     (URL            aSourceURL, String aTargetPath, IUnaryFullOperator<? extends CharSequence, ? super String> aOpt) throws IOException  {try (BufferedReader tReader = toReader(aSourceURL); IWriteln tWriter = toWriteln(aTargetPath)) {map(tReader, tWriter, aOpt);}}
        public static void      map     (BufferedReader    aReader, IWriteln   aWriter, IUnaryFullOperator<? extends CharSequence, ? super String> aOpt) throws IOException  {String tLine; while ((tLine = aReader.readLine()) != null) {aWriter.writeln(aOpt.apply(tLine));}}
        
        
        /** only support writeln */
        @FunctionalInterface public interface IWriteln extends AutoCloseable {
            void writeln(CharSequence aLine) throws IOException;
            default void close() throws IOException {/**/}
        }
        
        /** output stuffs */
        public static OutputStream   toOutputStream(String aFilePath)                           throws IOException {return toOutputStream(aFilePath, ZL_OO);}
        public static OutputStream   toOutputStream(String aFilePath, OpenOption... aOptions)   throws IOException {return toOutputStream(toAbsolutePath_(aFilePath), aOptions);}
        public static OutputStream   toOutputStream(Path aPath, OpenOption... aOptions)         throws IOException {validPath(aPath); return Files.newOutputStream(aPath, aOptions);}
        public static BufferedWriter toWriter      (String aFilePath)                           throws IOException {return toWriter(aFilePath, ZL_OO);}
        public static BufferedWriter toWriter      (String aFilePath, OpenOption... aOptions)   throws IOException {return toWriter(toAbsolutePath_(aFilePath), aOptions);}
        public static BufferedWriter toWriter      (Path aPath, OpenOption... aOptions)         throws IOException {validPath(aPath); return new BufferedWriter(new OutputStreamWriter(toOutputStream(aPath, aOptions), StandardCharsets.UTF_8)) {@Override public void newLine() throws IOException {write("\n");}};}
        public static BufferedWriter toWriter      (OutputStream aOutputStream)                                    {return toWriter(aOutputStream, StandardCharsets.UTF_8);}
        public static BufferedWriter toWriter      (OutputStream aOutputStream, Charset aCS)                       {return new BufferedWriter(new OutputStreamWriter(aOutputStream, aCS)) {@Override public void newLine() throws IOException {write("\n");}};}
        public static IWriteln       toWriteln     (String aFilePath)                           throws IOException {return toWriteln(aFilePath, ZL_OO);}
        public static IWriteln       toWriteln     (String aFilePath, OpenOption... aOptions)   throws IOException {return toWriteln(toWriter(aFilePath, aOptions));}
        public static IWriteln       toWriteln     (Path aPath, OpenOption... aOptions)         throws IOException {return toWriteln(toWriter(aPath, aOptions));}
        public static IWriteln       toWriteln     (OutputStream aOutputStream)                                    {return toWriteln(toWriter(aOutputStream));}
        public static IWriteln       toWriteln     (OutputStream aOutputStream, Charset aCS)                       {return toWriteln(toWriter(aOutputStream, aCS));}
        public static IWriteln       toWriteln     (BufferedWriter aWriter)                                        {
            return new IWriteln() {
                @Override public void writeln(CharSequence aLine) throws IOException {aWriter.append(aLine); aWriter.newLine();}
                @Override public void close() throws IOException {aWriter.close();}
            };
        }
        
        /** input stuffs */
        public static InputStream    toInputStream(String aFilePath)         throws IOException {return toInputStream(toAbsolutePath_(aFilePath));}
        public static InputStream    toInputStream(Path aPath)               throws IOException {return Files.newInputStream(aPath);}
        public static BufferedReader toReader     (String aFilePath)         throws IOException {return toReader(toAbsolutePath_(aFilePath));}
        public static BufferedReader toReader     (Path aPath)               throws IOException {return toReader(toInputStream(aPath));}
        public static BufferedReader toReader     (URL aFileURL)             throws IOException {return toReader(aFileURL.openStream());}
        public static BufferedReader toReader     (InputStream aInputStream) throws IOException {return toReader(aInputStream, StandardCharsets.UTF_8, true);}
        public static BufferedReader toReader     (InputStream aInputStream, Charset aCS) throws IOException {return toReader(aInputStream, aCS, false);}
        public static BufferedReader toReader     (InputStream aInputStream, Charset aCS, boolean aUseUnicodeReader) throws IOException {
            // 现在改为 UnicodeReader 实现，可以自动检测 UTF 的 BOM
            return new BufferedReader(aUseUnicodeReader ? new UnicodeReader(aInputStream, aCS.name()) : new InputStreamReader(aInputStream, aCS));
        }
        
        /** misc stuffs */
        public static File toFile(String aFilePath)                    {return toAbsolutePath_(aFilePath).toFile();}
        public static void validPath(String aPath)  throws IOException {if (aPath.endsWith("/") || aPath.endsWith("\\")) makeDir(aPath); else validPath(toAbsolutePath_(aPath));}
        public static void validPath(Path aPath)    throws IOException {Path tParent = aPath.getParent(); if (tParent != null) makeDir(tParent);}
        
        /**
         * extract zip file to directory
         * @author liqa
         */
        public static void zip2dir(String aZipFilePath, String aDir) throws IOException {
            aDir = toInternalValidDir(aDir);
            makeDir(aDir);
            byte[] tBuffer = ByteArrayCache.getArray(BUFFER_SIZE);
            try (ZipInputStream tZipInputStream = new ZipInputStream(toInputStream(aZipFilePath))) {
                ZipEntry tZipEntry = tZipInputStream.getNextEntry();
                while (tZipEntry != null) {
                    String tEntryPath = aDir + tZipEntry.getName();
                    if (tZipEntry.isDirectory()) {
                        makeDir(tEntryPath);
                    } else {
                        try (OutputStream tOutputStream = toOutputStream(tEntryPath)) {
                            int length;
                            while ((length = tZipInputStream.read(tBuffer, 0, BUFFER_SIZE)) > 0) {
                                tOutputStream.write(tBuffer, 0, length);
                            }
                        }
                    }
                    tZipEntry = tZipInputStream.getNextEntry();
                }
            } finally {
                ByteArrayCache.returnArray(tBuffer);
            }
        }
        
        /**
         * compress directory to zip file
         * @author liqa
         */
        public static void dir2zip(String aDir, String aZipFilePath, int aCompressLevel) throws IOException {
            aDir = toInternalValidDir(aDir);
            byte[] tBuffer =  ByteArrayCache.getArray(BUFFER_SIZE);
            try (ZipOutputStream tZipOutputStream = new ZipOutputStream(toOutputStream(aZipFilePath))) {
                tZipOutputStream.setLevel(aCompressLevel);
                for (String tName : list(aDir)) {
                    if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                    String tPath = aDir+tName;
                    if (isDir(tPath)) addDirToZip_("", tPath+"/", tName, tZipOutputStream, tBuffer);
                    else addFileToZip_("", tPath, tName, tZipOutputStream, tBuffer);
                }
            } finally {
                ByteArrayCache.returnArray(tBuffer);
            }
        }
        public static void dir2zip(String aDir, String aZipFilePath) throws IOException {dir2zip(aDir, aZipFilePath, Deflater.DEFAULT_COMPRESSION);}
        /**
         * compress files/directories to zip file
         * @author liqa
         */
        public static void files2zip(String[] aPaths, String aZipFilePath, int aCompressLevel) throws IOException {files2zip(AbstractCollections.from(aPaths), aZipFilePath, aCompressLevel);}
        public static void files2zip(String[] aPaths, String aZipFilePath) throws IOException {files2zip(AbstractCollections.from(aPaths), aZipFilePath);}
        /** Groovy stuff */
        public static void files2zip(Iterable<? extends CharSequence> aPaths, String aZipFilePath, int aCompressLevel) throws IOException {
            byte[] tBuffer = ByteArrayCache.getArray(BUFFER_SIZE);
            try (ZipOutputStream tZipOutputStream = new ZipOutputStream(toOutputStream(aZipFilePath))) {
                tZipOutputStream.setLevel(aCompressLevel);
                for (CharSequence tCS : aPaths) {
                    String tPath = tCS.toString();
                    File tFile = toFile(tPath);
                    if (tFile.isDirectory()) {
                        tPath = toInternalValidDir(tPath);
                        addDirToZip_("", tPath, tFile.getName(), tZipOutputStream, tBuffer);
                    } else {
                        addFileToZip_("", tPath, tFile.getName(), tZipOutputStream, tBuffer);
                    }
                }
            } finally {
                ByteArrayCache.returnArray(tBuffer);
            }
        }
        public static void files2zip(Iterable<? extends CharSequence> aPaths, String aZipFilePath) throws IOException {files2zip(aPaths, aZipFilePath, Deflater.DEFAULT_COMPRESSION);}
        
        private static void addFileToZip_(String aZipDir, String aFilePath, String aFileName, ZipOutputStream aZipOutputStream, byte[] rBuffer) throws IOException {
            try (InputStream tInputStream = toInputStream(aFilePath)) {
                aZipOutputStream.putNextEntry(new ZipEntry(aZipDir+aFileName));
                int length;
                while ((length = tInputStream.read(rBuffer, 0, BUFFER_SIZE)) > 0) {
                    aZipOutputStream.write(rBuffer, 0, length);
                }
                aZipOutputStream.closeEntry();
            }
        }
        private static void addDirToZip_(String aZipDir, String aDir, String aDirName, ZipOutputStream aZipOutputStream, byte[] rBuffer) throws IOException {
            String tZipDir = aZipDir+aDirName+"/";
            for (String tName : list(aDir)) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tPath = aDir+tName;
                if (isDir(tPath)) addDirToZip_(tZipDir, tPath+"/", tName, aZipOutputStream, rBuffer);
                else addFileToZip_(tZipDir, tPath, tName, aZipOutputStream, rBuffer);
            }
        }
        
        
        /**
         * convert between json and map
         * @author liqa
         */
        public static Map<?, ?> json2map(String aFilePath) throws IOException {
            try (Reader tReader = toReader(aFilePath)) {return json2map(tReader);}
        }
        public static Map<?, ?> json2map(Reader aReader) {
            return (Map<?, ?>) (new JsonSlurper()).parse(aReader);
        }
        public static void map2json(Map<?, ?> aMap, String aFilePath) throws IOException {
            map2json(aMap, aFilePath, false);
        }
        public static void map2json(Map<?, ?> aMap, String aFilePath, boolean aPretty) throws IOException {
            try (Writer tWriter = toWriter(aFilePath)) {
                JsonBuilder tBuilder = new JsonBuilder();
                tBuilder.call(aMap);
                if (aPretty) {
                    tWriter.append(tBuilder.toPrettyString());
                } else {
                    tBuilder.writeTo(tWriter);
                }
            }
        }
        /**
         * convert between yaml and map
         * @author liqa
         */
        public static Map<?, ?> yaml2map(String aFilePath) throws IOException {
            try (Reader tReader = toReader(aFilePath)) {return yaml2map(tReader);}
        }
        public static Map<?, ?> yaml2map(Reader aReader) {
            return (Map<?, ?>) (new YamlSlurper()).parse(aReader);
        }
        public static void map2yaml(Map<?, ?> aMap, String aFilePath) throws IOException {
            try (Writer tWriter = toWriter(aFilePath)) {
                YamlBuilder tBuilder = new YamlBuilder();
                tBuilder.call(aMap);
                tBuilder.writeTo(tWriter);
            }
        }
        
        
        /**
         * save matrix data to csv file
         * @author liqa
         * @param aData the matrix form data to save
         * @param aFilePath csv file path to save
         */
        public static void data2csv(double[][] aData, String aFilePath)                   throws IOException {data2csv(aData, aFilePath, ZL_STR);}
        public static void data2csv(double[][] aData, String aFilePath, String... aHeads) throws IOException {rows2csv(aData, aFilePath, aHeads);}
        public static void rows2csv(double[][] aData, String aFilePath)                   throws IOException {rows2csv(aData, aFilePath, ZL_STR);}
        public static void rows2csv(double[][] aRows, String aFilePath, String... aHeads) throws IOException {
            List<String> rLines = AbstractCollections.map(AbstractCollections.from(aRows), row -> String.join(",", AbstractCollections.map(row, Object::toString)));
            if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
            write(aFilePath, rLines);
        }
        public static void cols2csv(double[][] aData, String aFilePath)                   throws IOException {cols2csv(aData, aFilePath, ZL_STR);}
        public static void cols2csv(double[][] aCols, String aFilePath, String... aHeads) throws IOException {
            List<String> rLines = AbstractCollections.from(aCols[0].length, i ->  String.join(",", AbstractCollections.from(aCols.length, j -> String.valueOf(aCols[j][i]))));
            if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
            write(aFilePath, rLines);
        }
        public static void data2csv(double[] aData, String aFilePath) throws IOException {
            write(aFilePath, AbstractCollections.map(aData, Object::toString));
        }
        public static void data2csv(double[] aData, String aFilePath, String aHead) throws IOException {
            write(aFilePath, AbstractCollections.merge(aHead, AbstractCollections.map(aData, Object::toString)));
        }
        public static void data2csv(Iterable<?> aData, String aFilePath)                   throws IOException {data2csv(aData, aFilePath, ZL_STR);}
        public static void data2csv(Iterable<?> aData, String aFilePath, String... aHeads) throws IOException {rows2csv(aData, aFilePath, aHeads);}
        public static void rows2csv(Iterable<?> aData, String aFilePath)                   throws IOException {rows2csv(aData, aFilePath, ZL_STR);}
        public static void rows2csv(Iterable<?> aRows, String aFilePath, String... aHeads) throws IOException {
            Iterable<String> rLines = AbstractCollections.map(aRows, row -> {
                if (row instanceof IVector) {
                    return String.join(",", AbstractCollections.map(((IVector)row), Object::toString));
                } else
                if (row instanceof double[]) {
                    return String.join(",", AbstractCollections.map((double[])row, Object::toString));
                } else
                if (row instanceof Iterable) {
                    return String.join(",", AbstractCollections.map((Iterable<?>)row, String::valueOf));
                } else
                if (row instanceof Object[]) {
                    return String.join(",", AbstractCollections.map((Object[])row, String::valueOf));
                } else {
                    return String.valueOf(row);
                }
            });
            if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
            write(aFilePath, rLines);
        }
        public static void cols2csv(Iterable<?> aData, String aFilePath)                   throws IOException {cols2csv(aData, aFilePath, ZL_STR);}
        public static void cols2csv(Iterable<?> aCols, String aFilePath, String... aHeads) throws IOException {
            List<Iterator<String>> its = NewCollections.map(aCols, col -> {
                if (col instanceof IVector) {
                    return AbstractCollections.map((IVector)col, Object::toString).iterator();
                } else
                if (col instanceof double[]) {
                    return AbstractCollections.map((double[])col, Object::toString).iterator();
                } else
                if (col instanceof Iterable) {
                    return AbstractCollections.map((Iterable<?>)col, String::valueOf).iterator();
                } else
                if (col instanceof Object[]) {
                    return AbstractCollections.map((Object[])col, String::valueOf).iterator();
                } else {
                    return Collections.singletonList(String.valueOf(col)).iterator();
                }
            });
            validPath(aFilePath);
            try (IWriteln tWriteln = toWriteln(aFilePath)) {
                if (aHeads!=null && aHeads.length>0) tWriteln.writeln(String.join(",", aHeads));
                List<String> tTokens = new ArrayList<>(its.size());
                boolean tHasNext = true;
                while (true) {
                    for (Iterator<String> it : its) {
                        if (!it.hasNext()) {tHasNext = false; break;}
                        tTokens.add(it.next());
                    }
                    if (!tHasNext) break;
                    tWriteln.writeln(String.join(",", tTokens));
                    tTokens.clear();
                }
            }
        }
        public static void data2csv(IMatrix aData, String aFilePath)                   throws IOException {data2csv(aData, aFilePath, ZL_STR);}
        public static void data2csv(IMatrix aData, String aFilePath, String... aHeads) throws IOException {
            List<String> rLines = AbstractCollections.map(aData.rows(), subData -> String.join(",", AbstractCollections.map(subData, Object::toString)));
            if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
            write(aFilePath, rLines);
        }
        public static void data2csv(IVector aData, String aFilePath) throws IOException {
            write(aFilePath, AbstractCollections.map(aData, Object::toString));
        }
        public static void data2csv(IVector aData, String aFilePath, String aHead) throws IOException {
            write(aFilePath, AbstractCollections.merge(aHead, AbstractCollections.map(aData, Object::toString)));
        }
        public static void data2csv(IFunc1 aFunc, String aFilePath)                   throws IOException {data2csv(aFunc, aFilePath, ZL_STR);}
        public static void data2csv(IFunc1 aFunc, String aFilePath, String... aHeads) throws IOException {
            List<String> rLines = AbstractCollections.map(AbstractCollections.range(aFunc.Nx()), i -> aFunc.get(i)+","+aFunc.getX(i));
            rLines = AbstractCollections.merge((aHeads!=null && aHeads.length>0) ? String.join(",", aHeads) : "f,x", rLines);
            write(aFilePath, rLines);
        }
        
        /**
         * read matrix data from csv file
         * <p>
         * 现在也可以直接读取泛滥的空格分割的数据；
         * 现在也可以处理泛滥的 {@code #} 注释的情况，
         * 这里简单处理只考虑其在行首的情况
         * @author liqa
         * @param aFilePath csv file path to read
         * @return a matrix
         */
        public static RowMatrix csv2data(String aFilePath) throws IOException {
            try (BufferedReader tReader = toReader(aFilePath)) {
                // 需要的参数
                RowMatrix.Builder rBuilder;
                String tLine;
                int tColNum;
                // 跳过开头可能的注释行
                while ((tLine = tReader.readLine()) != null) {
                    if (!tLine.startsWith("#")) break;
                }
                if (tLine == null) return null;
                // 读取第一行来判断列数
                String[] tTokens = Text.splitStr(tLine);
                tColNum = tTokens.length;
                rBuilder = RowMatrix.builder(tColNum);
                // 读取第一行检测是否有头，直接看能否成功粘贴
                IVector tRow = null;
                try {tRow = Vectors.from(AbstractCollections.map(tTokens, Double::parseDouble));} catch (Exception ignored) {} // 直接看能否成功粘贴
                if (tRow != null) {
                    rBuilder.addRow(tRow);
                }
                // 遍历读取后续数据
                while ((tLine = tReader.readLine()) != null) {
                    // 跳过注释行
                    if (tLine.startsWith("#")) continue;
                    rBuilder.addRow(Text.str2data(tLine, tColNum));
                }
                // 返回结果
                rBuilder.trimToSize();
                return rBuilder.build();
            }
        }
        
        
        /**
         * save table to csv file
         * @author liqa
         * @param aTable the Table to be saved
         * @param aFilePath csv file path to save
         */
        public static void table2csv(ITable aTable, String aFilePath) throws IOException {
            List<String> rLines = AbstractCollections.map(aTable.rows(), subData -> String.join(",", AbstractCollections.map(subData, Object::toString)));
            rLines = AbstractCollections.merge(String.join(",", aTable.heads()), rLines);
            write(aFilePath, rLines);
        }
        /**
         * read table from csv file
         * <p>
         * 现在也可以直接读取泛滥的空格分割的数据；
         * 现在也可以处理泛滥的 {@code #} 注释的情况，
         * 这里简单处理只考虑其在行首的情况
         * @author liqa
         * @param aFilePath csv file path to read
         * @return table with head
         */
        public static Table csv2table(String aFilePath) throws IOException {
            try (BufferedReader tReader = toReader(aFilePath)) {
                // 需要的参数
                List<IVector> rRows = new ArrayList<>();
                String tLine;
                int tColNum;
                String[] tHeads = ZL_STR;
                // 跳过开头可能的注释行
                while ((tLine = tReader.readLine()) != null) {
                    if (!tLine.startsWith("#")) break;
                }
                if (tLine == null) return null;
                // 读取第一行来判断列数
                String[] tTokens = Text.splitStr(tLine);
                tColNum = tTokens.length;
                // 读取第一行检测是否有头，直接看能否成功粘贴
                IVector tRow = null;
                try {tRow = Vectors.from(AbstractCollections.map(tTokens, Double::parseDouble));} catch (Exception ignored) {} // 直接看能否成功粘贴
                if (tRow != null) {
                    rRows.add(tRow);
                } else {
                    tHeads = tTokens;
                }
                // 遍历读取后续数据
                while ((tLine = tReader.readLine()) != null) {
                    // 跳过注释行
                    if (tLine.startsWith("#")) continue;
                    rRows.add(Text.str2data(tLine, tColNum));
                }
                // 返回结果
                return Tables.fromRows(rRows, tHeads);
            }
        }
        
        /**
         * 保证兼容性的读取 csv 到 String，
         * 不假设 csv 是纯数字的，并且不识别 head
         * @author liqa
         * @param aFilePath csv file path to read
         * @param aFormat 自定义 {@link CSVFormat}
         * @return split 后的行组成的 List
         */
        public static List<String[]> csv2str(String aFilePath, CSVFormat aFormat) throws IOException {
            List<String[]> rLines = new ArrayList<>();
            try (CSVParser tParser = new CSVParser(toReader(aFilePath), aFormat)) {
                for (CSVRecord tRecord : tParser) rLines.add(tRecord.values());
            }
            return rLines;
        }
        /**
         * 保证兼容性的保存 String 到 csv，
         * 这里不提供额外 head 支持
         * @author liqa
         * @param aLines 需要保存的字符串数据，按行排列
         * @param aFilePath csv file path to save
         * @param aFormat 自定义 {@link CSVFormat}
         */
        public static void str2csv(Iterable<?> aLines, String aFilePath, CSVFormat aFormat) throws IOException {
            try (CSVPrinter tPrinter = new CSVPrinter(toWriter(aFilePath), aFormat)) {
                for (Object tLine : aLines) {
                    if (tLine instanceof Iterable) {
                        tPrinter.printRecord((Iterable<?>)tLine);
                    } else
                    if (tLine instanceof Object[]) {
                        tPrinter.printRecord((Object[])tLine);
                    } else {
                        tPrinter.printRecord(tLine);
                    }
                }
            }
        }
        private final static CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.DEFAULT.builder().setRecordSeparator('\n').setCommentMarker('#').setTrim(true).build();
        public static List<String[]> csv2str(String aFilePath) throws IOException {return csv2str(aFilePath, DEFAULT_CSV_FORMAT);}
        public static void str2csv(Iterable<?> aLines, String aFilePath) throws IOException {str2csv(aLines, aFilePath, DEFAULT_CSV_FORMAT);}
        
        /**
         * get URL of the resource
         * @author liqa
         */
        public static URL getResource(String aPath) {
            return IO.class.getClassLoader().getResource("assets/" + aPath);
        }
        
        /**
         * check whether the two paths are actually same
         * @author liqa
         */
        public static boolean samePath(String aPath1, String aPath2) {return WORKING_DIR_PATH.resolve(aPath1).normalize().equals(WORKING_DIR_PATH.resolve(aPath2).normalize());}
        
        
        /**
         * @author liqa
         * @param aPath 字符串路径
         * @return 给定路径的父路径
         */
        public static @Nullable String toParentPath(String aPath) {
            Path tPath = toParentPath_(aPath);
            return tPath==null ? null : tPath.toString();
        }
        public static @Nullable Path toParentPath_(String aPath) {return Paths.get(aPath).getParent();}
        
        /**
         * @author liqa
         * @param aPath 字符串路径
         * @return 给定路径的最后一项文件名（或目录名）
         */
        public static String toFileName(String aPath) {return toFileName_(aPath).toString();}
        public static Path toFileName_(String aPath) {return Paths.get(aPath).getFileName();}
        
        /**
         * 将输入路径转为相对路径，顺便会将 {@code `\`}
         * 转换为 {@code `/`}，保证可以用于 ssh 的文件路径；
         * 此项目依旧认为所有路径风格符都为 {@code `/`} 并要求至少兼容此种路径分隔符
         * @author liqa
         * @param aPath 字符串路径
         * @return 合理的相对路径
         */
        public static String toRelativePath(String aPath) {return toRelativePath_(aPath).toString().replace("\\", "/");}
        public static Path toRelativePath_(String aPath) {return WORKING_DIR_PATH.relativize(Paths.get(aPath));}
        
        /**
         * Right `toAbsolutePath` method,
         * because `toAbsolutePath` in `Paths` will still not work even used `setProperty`
         * @author liqa
         * @param aPath string path, can be relative or absolute
         * @return the Right absolute path
         */
        public static String toAbsolutePath(String aPath) {
            if (aPath.startsWith("~")) {
                // 默认不支持 ~
                return USER_HOME + aPath.substring(1); // user.home 这里统一认为 user.home 就是绝对路径
            }
            return WORKING_DIR_PATH.resolve(aPath).toString();
        }
        public static Path toAbsolutePath_(String aPath) {
            if (aPath.startsWith("~")) {
                // 默认不支持 ~
                return Paths.get(USER_HOME + aPath.substring(1)); // user.home 这里统一认为 user.home 就是绝对路径
            }
            return WORKING_DIR_PATH.resolve(aPath);
        }
        public static boolean isAbsolutePath(String aPath) {return isAbsolutePath(Paths.get(aPath));}
        public static boolean isAbsolutePath(Path aPath) {return aPath.isAbsolute();}
        
        static {jse.code.OS.InitHelper.init();}
    }
    
    /** @deprecated use {@link OS} */
    @Deprecated public final static class Exec extends OS {}
    
    /**
     * 实现类似 matlab 中可以直接使用 plot 函数进行绘图
     * <p>
     * 这里简单起见只维护一个窗口
     * @author liqa
     */
    @VisibleForTesting public static class Plot {
        private static final List<IPlotter> PLTS = new ArrayList<>();
        private static IPlotter PLT = null;
        
        /** 增加多图片支持 */
        public static IPlotter plotter() {PLT = Plotters.get(); PLTS.add(PLT); return PLT;}
        public static IPlotter plotter(int aIdx) {return PLTS.get(aIdx);}
        public static int plotterNumber() {return PLTS.size();}
        public static int nplotters() {return plotterNumber();}
        public static List<? extends IPlotter> plotters() {
            return new AbstractRandomAccessList<IPlotter>() {
                @Override public int size() {return plotterNumber();}
                @Override public IPlotter get(int index) {return plotter(index);}
            };
        }
        public static IFigure figure() {PLT = Plotters.get(); PLTS.add(PLT); return PLT.show();}
        public static IFigure figure(int aIdx) {return PLTS.get(aIdx).show();}
        public static int figureNumber() {return PLTS.size();}
        public static int nfigures() {return figureNumber();}
        public static List<? extends IFigure> figures() {
            return new AbstractRandomAccessList<IFigure>() {
                @Override public int size() {return figureNumber();}
                @Override public IFigure get(int index) {return figure(index);}
            };
        }
        public static void cla() {cla(false);}
        public static void clf() {clf(false);}
        public static void cla(boolean aAll) {
            if (aAll) {
                for (IPlotter tPlt : PLTS) tPlt.clear();
            } else {
                if (PLT != null) PLT.clear();
            }
        }
        public static void clf(boolean aAll) {
            if (aAll) {
                for (IPlotter tPlt : PLTS) tPlt.dispose();
                PLTS.clear();
                PLT = null;
            } else {
                if (PLT != null) {PLTS.remove(PLT); PLT.dispose(); PLT = null;}
            }
        }
        private static void validPlotter_() {
            if (PLT != null) return;
            if (PLTS.isEmpty()) {PLT = Plotters.get(); PLTS.add(PLT); return;}
            PLT = Code.last(PLTS);
        }
        
        
        public static ILine plot(                                                   double[] aY) {validPlotter_(); ILine tLine = PLT.plot(aY); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX,                     double[] aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX,                     double[] aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX,                     double[] aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                                                    IVector aY) {validPlotter_(); ILine tLine = PLT.plot(aY); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX,                      IVector aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX,                      IVector aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX,                      IVector aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                                                     IFunc1 aY) {validPlotter_(); ILine tLine = PLT.plot(aY); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX,                   IFunc1Subs aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX,                   IFunc1Subs aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX,                   IFunc1Subs aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                               Collection<? extends Number> aY) {validPlotter_(); ILine tLine = PLT.plot(aY); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX, Iterable  <? extends Number> aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX, Iterable  <? extends Number> aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {validPlotter_(); ILine tLine = PLT.plot(aX, aY); PLT.show(); return tLine;}
        public static ILine plot(                                                   double[] aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot( aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX,                     double[] aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX,                     double[] aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX,                     double[] aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                                                    IVector aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX,                      IVector aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX,                      IVector aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX,                      IVector aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                                                     IFunc1 aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX,                   IFunc1Subs aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX,                   IFunc1Subs aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX,                   IFunc1Subs aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                               Collection<? extends Number> aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                  double[] aX, Iterable  <? extends Number> aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(                   IVector aX, Iterable  <? extends Number> aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        public static ILine plot(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, @Nullable String aName) {validPlotter_(); ILine tLine = PLT.plot(aX, aY, aName); PLT.show(); return tLine;}
        
        public static ILine loglog(                                                   double[] aY) {xScaleLog(); yScaleLog(); return plot(aY);}
        public static ILine loglog(                  double[] aX,                     double[] aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                   IVector aX,                     double[] aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(Iterable<? extends Number> aX,                     double[] aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                                                    IVector aY) {xScaleLog(); yScaleLog(); return plot(aY);}
        public static ILine loglog(                  double[] aX,                      IVector aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                   IVector aX,                      IVector aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(Iterable<? extends Number> aX,                      IVector aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                                                     IFunc1 aY) {xScaleLog(); yScaleLog(); return plot(aY);}
        public static ILine loglog(                  double[] aX,                   IFunc1Subs aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                   IVector aX,                   IFunc1Subs aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(Iterable<? extends Number> aX,                   IFunc1Subs aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                               Collection<? extends Number> aY) {xScaleLog(); yScaleLog(); return plot(aY);}
        public static ILine loglog(                  double[] aX, Iterable  <? extends Number> aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                   IVector aX, Iterable  <? extends Number> aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {xScaleLog(); yScaleLog(); return plot(aX, aY);}
        public static ILine loglog(                                                   double[] aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aY, aName);}
        public static ILine loglog(                  double[] aX,                     double[] aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(                   IVector aX,                     double[] aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(Iterable<? extends Number> aX,                     double[] aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(                                                    IVector aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aY, aName);}
        public static ILine loglog(                  double[] aX,                      IVector aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(                   IVector aX,                      IVector aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(Iterable<? extends Number> aX,                      IVector aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(                                                     IFunc1 aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aY, aName);}
        public static ILine loglog(                  double[] aX,                   IFunc1Subs aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(                   IVector aX,                   IFunc1Subs aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(Iterable<? extends Number> aX,                   IFunc1Subs aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(                               Collection<? extends Number> aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aY, aName);}
        public static ILine loglog(                  double[] aX, Iterable  <? extends Number> aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(                   IVector aX, Iterable  <? extends Number> aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        public static ILine loglog(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, @Nullable String aName) {xScaleLog(); yScaleLog(); return plot(aX, aY, aName);}
        
        public static ILine semilogx(                                                   double[] aY) {xScaleLog(); return plot(aY);}
        public static ILine semilogx(                  double[] aX,                     double[] aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                   IVector aX,                     double[] aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(Iterable<? extends Number> aX,                     double[] aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                                                    IVector aY) {xScaleLog(); return plot(aY);}
        public static ILine semilogx(                  double[] aX,                      IVector aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                   IVector aX,                      IVector aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(Iterable<? extends Number> aX,                      IVector aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                                                     IFunc1 aY) {xScaleLog(); return plot(aY);}
        public static ILine semilogx(                  double[] aX,                   IFunc1Subs aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                   IVector aX,                   IFunc1Subs aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(Iterable<? extends Number> aX,                   IFunc1Subs aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                               Collection<? extends Number> aY) {xScaleLog(); return plot(aY);}
        public static ILine semilogx(                  double[] aX, Iterable  <? extends Number> aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                   IVector aX, Iterable  <? extends Number> aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {xScaleLog(); return plot(aX, aY);}
        public static ILine semilogx(                                                   double[] aY, @Nullable String aName) {xScaleLog(); return plot(aY, aName);}
        public static ILine semilogx(                  double[] aX,                     double[] aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(                   IVector aX,                     double[] aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(Iterable<? extends Number> aX,                     double[] aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(                                                    IVector aY, @Nullable String aName) {xScaleLog(); return plot(aY, aName);}
        public static ILine semilogx(                  double[] aX,                      IVector aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(                   IVector aX,                      IVector aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(Iterable<? extends Number> aX,                      IVector aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(                                                     IFunc1 aY, @Nullable String aName) {xScaleLog(); return plot(aY, aName);}
        public static ILine semilogx(                  double[] aX,                   IFunc1Subs aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(                   IVector aX,                   IFunc1Subs aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(Iterable<? extends Number> aX,                   IFunc1Subs aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(                               Collection<? extends Number> aY, @Nullable String aName) {xScaleLog(); return plot(aY, aName);}
        public static ILine semilogx(                  double[] aX, Iterable  <? extends Number> aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(                   IVector aX, Iterable  <? extends Number> aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogx(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, @Nullable String aName) {xScaleLog(); return plot(aX, aY, aName);}
        
        public static ILine semilogy(                                                   double[] aY) {yScaleLog(); return plot(aY);}
        public static ILine semilogy(                  double[] aX,                     double[] aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                   IVector aX,                     double[] aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(Iterable<? extends Number> aX,                     double[] aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                                                    IVector aY) {yScaleLog(); return plot(aY);}
        public static ILine semilogy(                  double[] aX,                      IVector aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                   IVector aX,                      IVector aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(Iterable<? extends Number> aX,                      IVector aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                                                     IFunc1 aY) {yScaleLog(); return plot(aY);}
        public static ILine semilogy(                  double[] aX,                   IFunc1Subs aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                   IVector aX,                   IFunc1Subs aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(Iterable<? extends Number> aX,                   IFunc1Subs aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                               Collection<? extends Number> aY) {yScaleLog(); return plot(aY);}
        public static ILine semilogy(                  double[] aX, Iterable  <? extends Number> aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                   IVector aX, Iterable  <? extends Number> aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {yScaleLog(); return plot(aX, aY);}
        public static ILine semilogy(                                                   double[] aY, @Nullable String aName) {yScaleLog(); return plot(aY, aName);}
        public static ILine semilogy(                  double[] aX,                     double[] aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(                   IVector aX,                     double[] aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(Iterable<? extends Number> aX,                     double[] aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(                                                    IVector aY, @Nullable String aName) {yScaleLog(); return plot(aY, aName);}
        public static ILine semilogy(                  double[] aX,                      IVector aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(                   IVector aX,                      IVector aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(Iterable<? extends Number> aX,                      IVector aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(                                                     IFunc1 aY, @Nullable String aName) {yScaleLog(); return plot(aY, aName);}
        public static ILine semilogy(                  double[] aX,                   IFunc1Subs aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(                   IVector aX,                   IFunc1Subs aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(Iterable<? extends Number> aX,                   IFunc1Subs aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(                               Collection<? extends Number> aY, @Nullable String aName) {yScaleLog(); return plot(aY, aName);}
        public static ILine semilogy(                  double[] aX, Iterable  <? extends Number> aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(                   IVector aX, Iterable  <? extends Number> aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        public static ILine semilogy(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, @Nullable String aName) {yScaleLog(); return plot(aX, aY, aName);}
        
        
        /**
         * 增加直接绘制 AtomData 的 plot，简单实现，由于参数较多这里使用 Map 作为输入
         * <p>
         * 格式为：
         * <pre><code>
         * {
         *   "Types": "${原子种类名称组成的数组}",
         *   "Colors": "${原子种类对应的颜色}",
         *   "Sizes": ${原子种类对应的大小},
         *   "Axis": "${视角轴，x,y,z}",
         * }
         * </code></pre>
         * @author liqa
         */
        @SuppressWarnings("unchecked")
        public static ILine[] plot(IAtomData aAtomData, Map<?, ?> aArgs) {
            validPlotter_();
            List<?> aTypes = (List<?>)Code.getWithDefault(aArgs, aAtomData.hasSymbol() ? AbstractCollections.from(aAtomData.atomTypeNumber(), i -> aAtomData.symbol(i+1)==null ? ("type "+(i+1)) : aAtomData.symbol(i+1)) : AbstractCollections.zl(), "Types", "types", "t");
            List<?> aColors = (List<?>)Code.getWithDefault(aArgs, AbstractCollections.from(aTypes.size(), i -> COLOR.getOrDefault(Code.toString(aTypes.get(i)), Colors.COLOR(i+1))), "Colors", "colors", "c");
            List<?> aSizes = (List<?>)Code.getWithDefault(aArgs, AbstractCollections.map(aTypes, type -> SIZE.getOrDefault(Code.toString(type), 1.0)), "Sizes", "sizes", "s");
            String aAxis = Code.toString(Code.getWithDefault(aArgs, "z", "Axis", "axis", "a"));
            if (!aAxis.equals("x") && !aAxis.equals("y") && !aAxis.equals("z")) aAxis = "z";
            
            double tScale;
            switch (aAxis) {
            case "x": {tScale = 200.0/Math.max(aAtomData.box().y(), aAtomData.box().z()); break;}
            case "y": {tScale = 200.0/Math.max(aAtomData.box().x(), aAtomData.box().z()); break;}
            case "z": {tScale = 200.0/Math.max(aAtomData.box().x(), aAtomData.box().y()); break;}
            default: throw new RuntimeException();
            }
            ILine[] rLines = new ILine[aAtomData.atomTypeNumber()];
            for (int i = 0; i < rLines.length; ++i) {
                final int tType = i + 1;
                Iterable<IAtom> tAtoms = AbstractCollections.filter(aAtomData.atoms(), atom->atom.type()==tType);
                switch (aAxis) {
                case "x": {rLines[i] = PLT.plot(AbstractCollections.map(tAtoms, IAtom::y), AbstractCollections.map(tAtoms, IAtom::z), aTypes.size()>i ? Code.toString(aTypes.get(i)) : "type "+tType); break;}
                case "y": {rLines[i] = PLT.plot(AbstractCollections.map(tAtoms, IAtom::x), AbstractCollections.map(tAtoms, IAtom::z), aTypes.size()>i ? Code.toString(aTypes.get(i)) : "type "+tType); break;}
                case "z": {rLines[i] = PLT.plot(AbstractCollections.map(tAtoms, IAtom::x), AbstractCollections.map(tAtoms, IAtom::y), aTypes.size()>i ? Code.toString(aTypes.get(i)) : "type "+tType); break;}
                }
                rLines[i].lineType(Strokes.LineType.NULL).markerType(Shapes.MarkerType.CIRCLE).fill();
                if (aColors.size() <= i) {
                    rLines[i].color(tType);
                } else {
                    Object tColor = aColors.get(i);
                    if (tColor instanceof Number) {
                        rLines[i].color(((Number)tColor).intValue());
                    } else
                    if (tColor instanceof List) {
                        List<? extends Number> tColorList = (List<? extends Number>)tColor;
                        rLines[i].color(tColorList.get(0).doubleValue(), tColorList.get(1).doubleValue(), tColorList.get(2).doubleValue());
                    } else
                    if (tColor instanceof Paint) {
                        rLines[i].color((Paint)tColor);
                    } else {
                        rLines[i].color(Code.toString(tColor));
                    }
                }
                rLines[i].markerEdgeColor(0);
                rLines[i].markerSize((aSizes.size()>i ? ((Number)aSizes.get(i)).doubleValue() : 1.0) * tScale);
            }
            switch (aAxis) {
            case "x": {PLT.xLabel("y").yLabel("z").axis(0.0, aAtomData.box().y(), 0.0, aAtomData.box().z()); break;}
            case "y": {PLT.xLabel("x").yLabel("z").axis(0.0, aAtomData.box().x(), 0.0, aAtomData.box().z()); break;}
            case "z": {PLT.xLabel("x").yLabel("y").axis(0.0, aAtomData.box().x(), 0.0, aAtomData.box().y()); break;}
            }
            PLT.xScaleLinear().yScaleLinear();
            PLT.show();
            return rLines;
        }
        public static ILine[] plot(IAtomData aAtomData, final String... aAtomTypes) {return plot(aAtomData, Maps.of("Types" , AbstractCollections.from(aAtomTypes)));}
        public static ILine[] plot(IAtomData aAtomData) {
            @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
            String[] rAtomTypes = tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR);
            for (int tType = 1; tType <= rAtomTypes.length; ++tType) if (rAtomTypes[tType-1]==null) rAtomTypes[tType-1] = "type "+tType;
            return plot(aAtomData, rAtomTypes);
        }
        
        
        public static void xScaleLog() {validPlotter_(); PLT.xScaleLog();}
        public static void yScaleLog() {validPlotter_(); PLT.yScaleLog();}
        public static void xScaleLinear() {validPlotter_(); PLT.xScaleLinear();}
        public static void yScaleLinear() {validPlotter_(); PLT.yScaleLinear();}
        
        public static void title(String aTitle) {validPlotter_(); PLT.title(aTitle);}
        public static void xLabel(String aXLabel) {validPlotter_(); PLT.xLabel(aXLabel);}
        public static void yLabel(String aYLabel) {validPlotter_(); PLT.yLabel(aYLabel);}
        public static void xlabel(String aXLabel) {validPlotter_(); PLT.xlabel(aXLabel);}
        public static void ylabel(String aYLabel) {validPlotter_(); PLT.ylabel(aYLabel);}
        
        public static void xRange(double aMin, double aMax) {validPlotter_(); PLT.xRange(aMin, aMax);}
        public static void yRange(double aMin, double aMax) {validPlotter_(); PLT.yRange(aMin, aMax);}
        public static void xrange(double aMin, double aMax) {validPlotter_(); PLT.xrange(aMin, aMax);}
        public static void yrange(double aMin, double aMax) {validPlotter_(); PLT.yrange(aMin, aMax);}
        public static void axis(double aMin, double aMax) {validPlotter_(); PLT.axis(aMin, aMax);}
        public static void axis(double aXMin, double aXMax, double aYMin, double aYMax) {validPlotter_(); PLT.axis(aXMin, aXMax, aYMin, aYMax);}
        public static void axis(double[] aAxis) {validPlotter_(); PLT.axis(aAxis);}
        
        public static void xTick(double aTick) {validPlotter_(); PLT.xTick(aTick);}
        public static void yTick(double aTick) {validPlotter_(); PLT.yTick(aTick);}
        public static void xtick(double aTick) {validPlotter_(); PLT.xtick(aTick);}
        public static void ytick(double aTick) {validPlotter_(); PLT.ytick(aTick);}
        public static void tick(double aTick) {validPlotter_(); PLT.tick(aTick);}
        public static void tick(double aXTick, double aYTick) {validPlotter_(); PLT.tick(aXTick, aYTick);}
        
        public static void figureSize(int aWidth, int aHeight) {validPlotter_(); PLT.size(aWidth, aHeight);}
        public static void saveFigure(@Nullable String aFilePath, int aWidth, int aHeight) throws IOException {validPlotter_(); PLT.save(aFilePath, aWidth, aHeight);}
        public static void saveFigure(@Nullable String aFilePath) throws IOException {validPlotter_(); PLT.save(aFilePath);}
        public static void saveFigure() throws IOException {validPlotter_(); PLT.save();}
        public static byte[] encodeFigure(int aWidth, int aHeight) throws IOException {validPlotter_(); return PLT.encode(aWidth, aHeight);}
        public static byte[] encodeFigure() throws IOException {validPlotter_(); return PLT.encode();}
        public static void figsize(int aWidth, int aHeight) {figureSize(aWidth, aHeight);}
        public static void savefig(@Nullable String aFilePath, int aWidth, int aHeight) throws IOException {saveFigure(aFilePath, aWidth, aHeight);}
        public static void savefig(@Nullable String aFilePath) throws IOException {saveFigure(aFilePath);}
        public static void savefig() throws IOException {saveFigure();}
        public static byte[] encodefig(int aWidth, int aHeight) throws IOException {return encodeFigure(aWidth, aHeight);}
        public static byte[] encodefig() throws IOException {return encodeFigure();}
        
        public static ILegend legend() {validPlotter_(); return PLT.legend();}
        public static List<? extends ILine> lines() {validPlotter_(); return PLT.lines();}
        public static ILine line(String aName) {validPlotter_(); return PLT.line(aName);}
        public static ILine line(int aIdx) {validPlotter_(); return PLT.line(aIdx);}
        public static int lineNumber() {validPlotter_(); return PLT.lineNumber();}
        @VisibleForTesting public static int nlines()  {validPlotter_(); return PLT.nlines();}
    }
    
    
    /**
     * 实现类似 matlab 或 numpy 中可以直接使用的数学运算，这里只提供实数运算
     * <p>
     * 这里再次使用独立的实现，保证效率的同时可以方便的复制过来实现，并且已有实现重构时不会受到影响
     * @author liqa
     */
    @VisibleForTesting public static class Math {
        public final static double PI = FastMath.PI, pi = PI;
        public final static double E = FastMath.E, e = E;
        public final static IComplexDouble i1 = MathEX.i1, j1 = i1, i = i1, j = j1;
        public final static double NaN = Double.NaN, nan = NaN;
        public final static double Inf = Double.POSITIVE_INFINITY, inf = Inf;
        
        
        public static double sqrt(double aValue) {return FastMath.sqrt(aValue);}
        public static double cbrt(double aValue) {return FastMath.cbrt(aValue);}
        public static double hypot(double aX, double aY) {return FastMath.hypot(aX, aY);}
        public static double hypot(double aX, double aY, double aZ) {return FastMath.hypot(aX, aY, aZ);}
        
        public static double exp(double aValue) {return FastMath.exp(aValue);}
        public static double log(double aValue) {return FastMath.log(aValue);}
        public static double log10(double aValue) {return FastMath.log10(aValue);}
        
        public static double pow(double aValue, double aPower) {return FastMath.pow(aValue, aPower);}
        public static double powFast(double aValue, int aPower) {return FastMath.powFast(aValue, aPower);}
        public static double pow2(double aValue) {return aValue*aValue;}
        public static double pow3(double aValue) {return aValue*aValue*aValue;}
        
        public static double sin(double aValue) {return FastMath.sin(aValue);}
        public static double cos(double aValue) {return FastMath.cos(aValue);}
        public static double tan(double aValue) {return FastMath.tan(aValue);}
        
        public static double asin(double aValue) {return FastMath.asin(aValue);}
        public static double acos(double aValue) {return FastMath.acos(aValue);}
        public static double atan(double aValue) {return FastMath.atan(aValue);}
        public static double atan2(double aY, double aX) {return FastMath.atan2(aY, aX);}
        
        public static double sinh(double aValue) {return FastMath.sinh(aValue);}
        public static double cosh(double aValue) {return FastMath.cosh(aValue);}
        public static double tanh(double aValue) {return FastMath.tanh(aValue);}
        
        public static double asinh(double aValue) {return FastMath.asinh(aValue);}
        public static double acosh(double aValue) {return FastMath.acosh(aValue);}
        public static double atanh(double aValue) {return FastMath.atanh(aValue);}
        
        public static double floor(double aValue) {return FastMath.floor(aValue);}
        public static double ceil(double aValue) {return FastMath.ceil(aValue);}
        public static long round(double aValue) {return FastMath.round(aValue);}
        
        public static double toRange(double aMin, double aMax, double aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        public static int toRange(int aMin, int aMax, int aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        public static long toRange(long aMin, long aMax, long aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        
        public static double abs(double aValue) {return java.lang.Math.abs(aValue);}
        public static double min(double aLHS, double aRHS) {return java.lang.Math.min(aLHS, aRHS);}
        public static double max(double aLHS, double aRHS) {return java.lang.Math.max(aLHS, aRHS);}
        public static double rand() {return RANDOM.nextDouble();}
        public static double randn() {return RANDOM.nextGaussian();}
        public static int randi(int aBound) {return RANDOM.nextInt(aBound);}
        public static Random rng(long aSeed) {RANDOM.setSeed(aSeed); return RANDOM;}
        public static Random rng() {return RANDOM;}
        
        
        /// vectors
        public static IVector sqrt(IVector aVec) {return aVec.operation().map(Math::sqrt);}
        public static IVector cbrt(IVector aVec) {return aVec.operation().map(Math::cbrt);}
        public static IVector hypot(IVector aX, final double aY) {return aX.operation().map(x -> hypot(x, aY));}
        public static IVector hypot(final double aX, IVector aY) {return aY.operation().map(y -> hypot(aX, y));}
        public static IVector hypot(IVector aX, IVector aY) {return aX.operation().operate(aY, Math::hypot);}
        public static IVector hypot(IVector aX, final double aY, final double aZ) {return aX.operation().map(x -> hypot(x, aY, aZ));}
        public static IVector hypot(final double aX, IVector aY, final double aZ) {return aY.operation().map(y -> hypot(aX, y, aZ));}
        public static IVector hypot(final double aX, final double aY, IVector aZ) {return aZ.operation().map(z -> hypot(aX, aY, z));}
        public static IVector hypot(IVector aX, IVector aY, final double aZ) {return aX.operation().operate(aY, (x, y) -> hypot(x, y, aZ));}
        public static IVector hypot(IVector aX, final double aY, IVector aZ) {return aX.operation().operate(aZ, (x, z) -> hypot(x, aY, z));}
        public static IVector hypot(final double aX, IVector aY, IVector aZ) {return aY.operation().operate(aZ, (y, z) -> hypot(aX, y, z));}
        /** IVector 不支持三元运算，这里不再考虑效率问题直接这样实现 */
        public static IVector hypot(IVector aX, IVector aY, IVector aZ) {return Vectors.from(aX.size(), i -> hypot(aX.get(i), aY.get(i), aZ.get(i)));}
        
        public static IVector exp(IVector aVec) {return aVec.operation().map(Math::exp);}
        public static IVector log(IVector aVec) {return aVec.operation().map(Math::log);}
        public static IVector log10(IVector aVec) {return aVec.operation().map(Math::log10);}
        
        public static IVector pow(IVector aVec, final double aPower) {return aVec.operation().map(v -> pow(v, aPower));}
        public static IVector powFast(IVector aVec, final int aPower) {return aVec.operation().map(v -> powFast(v, aPower));}
        public static IVector pow2(IVector aVec) {return aVec.operation().map(v -> v*v);}
        public static IVector pow3(IVector aVec) {return aVec.operation().map(v -> v*v*v);}
        
        public static IVector sin(IVector aVec) {return aVec.operation().map(Math::sin);}
        public static IVector cos(IVector aVec) {return aVec.operation().map(Math::cos);}
        public static IVector tan(IVector aVec) {return aVec.operation().map(Math::tan);}
        
        public static IVector asin(IVector aVec) {return aVec.operation().map(Math::asin);}
        public static IVector acos(IVector aVec) {return aVec.operation().map(Math::acos);}
        public static IVector atan(IVector aVec) {return aVec.operation().map(Math::atan);}
        public static IVector atan2(IVector aY, final double aX) {return aY.operation().map(y -> atan2(y, aX));}
        public static IVector atan2(final double aY, IVector aX) {return aX.operation().map(x -> atan2(aY, x));}
        public static IVector atan2(IVector aY, IVector aX) {return aY.operation().operate(aX, Math::atan2);}
        
        public static IVector sinh(IVector aVec) {return aVec.operation().map(Math::sinh);}
        public static IVector cosh(IVector aVec) {return aVec.operation().map(Math::cosh);}
        public static IVector tanh(IVector aVec) {return aVec.operation().map(Math::tanh);}
        
        public static IVector asinh(IVector aVec) {return aVec.operation().map(Math::asinh);}
        public static IVector acosh(IVector aVec) {return aVec.operation().map(Math::acosh);}
        public static IVector atanh(IVector aVec) {return aVec.operation().map(Math::atanh);}
        
        public static IVector floor(IVector aVec) {return aVec.operation().map(Math::floor);}
        public static IVector ceil(IVector aVec) {return aVec.operation().map(Math::ceil);}
        public static IVector round(IVector aVec) {return aVec.operation().map(Math::round);}
        
        public static IVector toRange(double aMin, double aMax, IVector aVec) {return aVec.operation().map(v -> toRange(aMin, aMax, v));}
        
        public static IVector abs(IVector aVec) {return aVec.operation().map(Math::abs);}
        public static double sum (IVector aVec) {return aVec.sum();}
        public static double mean(IVector aVec) {return aVec.mean();}
        public static double prod(IVector aVec) {return aVec.prod();}
        public static double min (IVector aVec) {return aVec.min();}
        public static double max (IVector aVec) {return aVec.max();}
        public static IVector cumsum (IVector aVec) {return aVec.operation().cumsum();}
        public static IVector cummean(IVector aVec) {return aVec.operation().cummean();}
        public static IVector cumprod(IVector aVec) {return aVec.operation().cumprod();}
        public static IVector cummin (IVector aVec) {return aVec.operation().cummin();}
        public static IVector cummax (IVector aVec) {return aVec.operation().cummax();}
        
        public static double dot(IVector aVec) {return aVec.operation().dot();}
        public static double dot(IVector aLHS, IVector aRHS) {return aLHS.operation().dot(aRHS);}
        public static double norm(IVector aVec) {return aVec.operation().norm();}
        public static double norm1(IVector aVec) {return aVec.operation().norm1();}
        
        public static Vector zeros(int aSize) {return Vectors.zeros(aSize);}
        public static Vector ones(int aSize) {return Vectors.ones(aSize);}
        public static Vector NaN(int aSize) {return Vectors.NaN(aSize);}
        public static Vector nan(int aSize) {return NaN(aSize);}
        public static Vector rand(int aSize) {Vector rVec = zeros(aSize); rVec.assign(Math::rand); return rVec;}
        public static Vector randn(int aSize) {Vector rVec = zeros(aSize); rVec.assign(Math::randn); return rVec;}
        public static Vector randi(final int aBound, int aSize) {Vector rVec = zeros(aSize); rVec.assign(()->randi(aBound)); return rVec;}
        public static Vector linsequence(double aStart, double aStep, int aN) {return Vectors.linsequence(aStart, aStep, aN);}
        public static Vector linspace(double aStart, double aEnd, int aN) {return Vectors.linspace(aStart, aEnd, aN);}
        public static Vector logsequence(double aStart, double aStep, int aN) {return Vectors.logsequence(aStart, aStep, aN);}
        public static Vector logspace(double aStart, double aEnd, int aN) {return Vectors.logspace(aStart, aEnd, aN);}
        
        
        /// matrices
        public static IMatrix sqrt(IMatrix aMat) {return aMat.operation().map(Math::sqrt);}
        public static IMatrix cbrt(IMatrix aVec) {return aVec.operation().map(Math::cbrt);}
        public static IMatrix hypot(IMatrix aX, final double aY) {return aX.operation().map(x -> hypot(x, aY));}
        public static IMatrix hypot(final double aX, IMatrix aY) {return aY.operation().map(y -> hypot(aX, y));}
        public static IMatrix hypot(IMatrix aX, IMatrix aY) {return aX.operation().operate(aY, Math::hypot);}
        public static IMatrix hypot(IMatrix aX, final double aY, final double aZ) {return aX.operation().map(x -> hypot(x, aY, aZ));}
        public static IMatrix hypot(final double aX, IMatrix aY, final double aZ) {return aY.operation().map(y -> hypot(aX, y, aZ));}
        public static IMatrix hypot(final double aX, final double aY, IMatrix aZ) {return aZ.operation().map(z -> hypot(aX, aY, z));}
        public static IMatrix hypot(IMatrix aX, IMatrix aY, final double aZ) {return aX.operation().operate(aY, (x, y) -> hypot(x, y, aZ));}
        public static IMatrix hypot(IMatrix aX, final double aY, IMatrix aZ) {return aX.operation().operate(aZ, (x, z) -> hypot(x, aY, z));}
        public static IMatrix hypot(final double aX, IMatrix aY, IMatrix aZ) {return aY.operation().operate(aZ, (y, z) -> hypot(aX, y, z));}
        /** IMatrix 不支持三元运算，这里不再考虑效率问题直接这样实现 */
        public static IMatrix hypot(IMatrix aX, IMatrix aY, IMatrix aZ) {return Matrices.from(aX.rowNumber(), aX.columnNumber(), (i, j) -> hypot(aX.get(i, j), aY.get(i, j), aZ.get(i, j)));}
        
        public static IMatrix exp(IMatrix aMat) {return aMat.operation().map(Math::exp);}
        public static IMatrix log(IMatrix aMat) {return aMat.operation().map(Math::log);}
        public static IMatrix log10(IMatrix aMat) {return aMat.operation().map(Math::log10);}
        
        public static IMatrix pow(IMatrix aMat, final double aPower) {return aMat.operation().map(v -> pow(v, aPower));}
        public static IMatrix powFast(IMatrix aMat, final int aPower) {return aMat.operation().map(v -> powFast(v, aPower));}
        public static IMatrix pow2(IMatrix aMat) {return aMat.operation().map(v -> v*v);}
        public static IMatrix pow3(IMatrix aMat) {return aMat.operation().map(v -> v*v*v);}
        
        public static IMatrix sin(IMatrix aMat) {return aMat.operation().map(Math::sin);}
        public static IMatrix cos(IMatrix aMat) {return aMat.operation().map(Math::cos);}
        public static IMatrix tan(IMatrix aMat) {return aMat.operation().map(Math::tan);}
        
        public static IMatrix asin(IMatrix aMat) {return aMat.operation().map(Math::asin);}
        public static IMatrix acos(IMatrix aMat) {return aMat.operation().map(Math::acos);}
        public static IMatrix atan(IMatrix aMat) {return aMat.operation().map(Math::atan);}
        public static IMatrix atan2(IMatrix aY, final double aX) {return aY.operation().map(y -> atan2(y, aX));}
        public static IMatrix atan2(final double aY, IMatrix aX) {return aX.operation().map(x -> atan2(aY, x));}
        public static IMatrix atan2(IMatrix aY, IMatrix aX) {return aY.operation().operate(aX, Math::atan2);}
        
        public static IMatrix sinh(IMatrix aMat) {return aMat.operation().map(Math::sinh);}
        public static IMatrix cosh(IMatrix aMat) {return aMat.operation().map(Math::cosh);}
        public static IMatrix tanh(IMatrix aMat) {return aMat.operation().map(Math::tanh);}
        
        public static IMatrix asinh(IMatrix aMat) {return aMat.operation().map(Math::asinh);}
        public static IMatrix acosh(IMatrix aMat) {return aMat.operation().map(Math::acosh);}
        public static IMatrix atanh(IMatrix aMat) {return aMat.operation().map(Math::atanh);}
        
        public static IMatrix floor(IMatrix aMat) {return aMat.operation().map(Math::floor);}
        public static IMatrix ceil(IMatrix aMat) {return aMat.operation().map(Math::ceil);}
        public static IMatrix round(IMatrix aMat) {return aMat.operation().map(Math::round);}
        
        public static IMatrix toRange(double aMin, double aMax, IMatrix aMat) {return aMat.operation().map(v -> toRange(aMin, aMax, v));}
        
        public static IMatrix abs(IMatrix aMat) {return aMat.operation().map(Math::abs);}
        public static double sum (IMatrix aMat) {return aMat.operation().sum ();}
        public static double mean(IMatrix aMat) {return aMat.operation().mean();}
        public static double min (IMatrix aMat) {return aMat.operation().min ();}
        public static double max (IMatrix aMat) {return aMat.operation().max ();}
        public static IVector sumOfCols (IMatrix aMat) {return aMat.operation().sumOfCols ();}
        public static IVector sumOfRows (IMatrix aMat) {return aMat.operation().sumOfRows ();}
        public static IVector meanOfCols(IMatrix aMat) {return aMat.operation().meanOfCols();}
        public static IVector meanOfRows(IMatrix aMat) {return aMat.operation().meanOfRows();}
        
        public static ColumnMatrix zeros(int aRowNum, int ColNum) {return Matrices.zeros(aRowNum, ColNum);}
        public static ColumnMatrix ones(int aRowNum, int ColNum) {return Matrices.ones(aRowNum, ColNum);}
        public static ColumnMatrix NaN(int aRowNum, int ColNum) {return Matrices.NaN(aRowNum, ColNum);}
        public static ColumnMatrix nan(int aRowNum, int ColNum) {return NaN(aRowNum, ColNum);}
        public static ColumnMatrix rand(int aRowNum, int ColNum) {ColumnMatrix rMat = zeros(aRowNum, ColNum); rMat.assignCol(Math::rand); return rMat;}
        public static ColumnMatrix randn(int aRowNum, int ColNum) {ColumnMatrix rMat = zeros(aRowNum, ColNum); rMat.assignCol(Math::randn); return rMat;}
        public static ColumnMatrix randi(final int aBound, int aRowNum, int ColNum) {ColumnMatrix rMat = zeros(aRowNum, ColNum); rMat.assignCol(()->randi(aBound)); return rMat;}
        public static ColumnMatrix diag(IVector aVec) {return Matrices.diag(aVec);}
        public static IVector diag(IMatrix aMat) {return aMat.slicer().diag();}
        public static IVector diag(IMatrix aMat, int aShift) {return aMat.slicer().diag(aShift);}
        
        
        /// 已经实现的特殊函数
        public static double legendre(int aL, int aM, double aX) {return MathEX.Func.legendre(aL, aM, aX);}
        public static IVector legendre(final int aL, final int aM, IVector aX) {return aX.operation().map(x -> legendre(aL, aM, x));}
        public static IMatrix legendre(final int aL, final int aM, IMatrix aX) {return aX.operation().map(x -> legendre(aL, aM, x));}
    }
}
