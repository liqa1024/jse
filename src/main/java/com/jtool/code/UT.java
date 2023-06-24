package com.jtool.code;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.jtool.atom.*;
import com.jtool.code.operator.IOperator1;
import com.jtool.code.task.TaskCall;
import com.jtool.code.task.TaskRun;
import com.jtool.math.function.IFunc1;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.Matrices;
import com.jtool.math.table.Table;
import com.jtool.math.vector.IVector;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
        
        
        /**
         * merge two array into one List
         * @author liqa
         */
        public static List<Object> merge(final Object[] aBefore, Object... aAfter) {
            final Object[] fAfter = aAfter==null ? ZL_OBJ : aAfter;
            return new AbstractList<Object>() {
                @Override public Object get(int index) {return index<aBefore.length ? aBefore[index] : fAfter[index-aBefore.length];}
                @Override public int size() {return aBefore.length+fAfter.length;}
            };
        }
        public static List<Object> mergeBefore(final Object[] aAfter, Object... aBefore) {
            final Object[] fBefore = aBefore==null ? ZL_OBJ : aBefore;
            return new AbstractList<Object>() {
                @Override public Object get(int index) {return index<fBefore.length ? fBefore[index] : aAfter[index-fBefore.length];}
                @Override public int size() {return fBefore.length+aAfter.length;}
            };
        }
        public static List<Object> merge(final Object aBefore0, final Object[] aAfter) {
            return new AbstractList<Object>() {
                @Override public Object get(int index) {return index<1 ? aBefore0 : aAfter[index-1];}
                @Override public int size() {return aAfter.length+1;}
            };
        }
        public static List<Object> merge(final Object aBefore0, final Object aBefore1, final Object[] aAfter) {
            return new AbstractList<Object>() {
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
            return new AbstractList<Object>() {
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
            return new AbstractList<Object>() {
                @Override public Object get(int index) {
                    int tRest = index-aBefore.length;
                    return tRest==0 ? aAfter0 : aBefore[index];
                }
                @Override public int size() {return aBefore.length+1;}
            };
        }
        public static List<Object> merge(final Object[] aBefore, final Object aAfter0, final Object aAfter1) {
            return new AbstractList<Object>() {
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
            return new AbstractList<Object>() {
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
        /**
         * merge {@code Iterable<Future<T>> to single Future<List<T>>} in All logic
         * @author liqa
         */
        public static <T> Future<List<T>> mergeAll(final Iterable<? extends Future<T>> aFutures) {
            return new Future<List<T>>() {
                @Override public boolean cancel(boolean mayInterruptIfRunning) {
                    for (Future<T> tFuture : aFutures) if (!tFuture.cancel(mayInterruptIfRunning)) return false;
                    return true;
                }
                @Override public boolean isCancelled() {
                    for (Future<T> tFuture : aFutures) if (!tFuture.isCancelled()) return false;
                    return true;
                }
                @Override public boolean isDone() {
                    for (Future<T> tFuture : aFutures) if (!tFuture.isDone()) return false;
                    return true;
                }
                @Override public List<T> get() throws InterruptedException, ExecutionException {
                    List<T> tOut = new ArrayList<>();
                    for (Future<T> tFuture : aFutures) tOut.add(tFuture.get());
                    return tOut;
                }
                @Override public List<T> get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    List<T> tOut = new ArrayList<>();
                    for (Future<T> tFuture : aFutures) tOut.add(tFuture.get(timeout, unit));
                    return tOut;
                }
            };
        }
        
        
        /**
         * filter the input Iterable
         * @author liqa
         */
        public static <T> Iterable<T> filter(final Iterable<? extends T> aIterable, final IOperator1<Boolean, ? super T> aFilter) {
            return () -> new Iterator<T>() {
                private final Iterator<? extends T> mIt = aIterable.iterator();
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
            return () -> new Iterator<R>() {
                final Iterator<T> mIt = aIterable.iterator();
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
        public static <R, T> List<R> map(final List<T> aList, final IOperator1<? extends R, ? super T> aOpt) {
            return new AbstractList<R>() {
                @Override public R get(int index) {return aOpt.cal(aList.get(index));}
                @Override public int size() {return aList.size();}
            };
        }
        public static <R, T> List<R> map(final T[] aArray, final IOperator1<? extends R, ? super T> aOpt) {
            return new AbstractList<R>() {
                @Override public R get(int index) {return aOpt.cal(aArray[index]);}
                @Override public int size() {return aArray.length;}
            };
        }
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
         * {@link Arrays}.asList for double[]
         * @author liqa
         * @param aData the input double[]
         * @return the list format of double[]
         */
        public static List<Double> asList(final double[] aData) {
            return new AbstractList<Double>() {
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
            return new AbstractList<Integer>() {
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
            return new AbstractList<Boolean>() {
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
        
        public static byte[] long2bytes(long aL) {
            byte[] rBytes = new byte[DOUBLE_LEN];
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
            return aStr.trim().split("\\s+");
        }
        
        
        /**
         * Splits a string separated by comma(",") characters into multiple strings
         * <p> will automatically ignores multiple spaces </p>
         * @author liqa
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitComma(String aStr) {
            return aStr.replaceAll("\\s+","").split(",");
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
         * <p>
         * Optimized for large file reads
         * @author liqa
         * @param aFilePath File to read
         * @return lines of String
         * @throws IOException when fail
         */
        public static List<String> readAllLines(String aFilePath) throws IOException {
            try (Stream<String> tLines = Files.lines(toAbsolutePath_(aFilePath))) {
                return tLines.parallel().collect(Collectors.toList());
            }
        }
        /**
         * read all lines of the InputStream
         * <p>
         * Optimized for large file reads
         * @author liqa
         */
        public static List<String> readAllLines(InputStream aInputStream) {
            try (Stream<String> tLines = toReader(aInputStream).lines()) {
                return tLines.parallel().collect(Collectors.toList());
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
        public static void      move    (String aSourcePath, String aTargetPath)        throws IOException  {move(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath));}
        public static String[]  list    (String aDir)                                                       {return toFile(aDir).list();} // use the File.list not Files.list to get the simple result
        public static void      makeDir (Path aDir)                                     throws IOException  {Files.createDirectories(aDir);} // can mkdir nested
        public static void      copy    (Path aSourcePath, Path aTargetPath)            throws IOException  {validPath(aTargetPath); Files.copy(aSourcePath, aTargetPath, REPLACE_EXISTING);}
        public static void      copy    (InputStream aSourceStream, Path aTargetPath)   throws IOException  {validPath(aTargetPath); Files.copy(aSourceStream, aTargetPath, REPLACE_EXISTING);}
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
         * convert double[] to String[] for printRecord usage
         * @author liqa
         */
        public static String[] data2str(double[] aData) {
            String[] tStr = new String[aData.length];
            for (int i = 0; i < aData.length; ++i) tStr[i] = String.valueOf(aData[i]);
            return tStr;
        }
        /**
         * convert String[] to double[] for csv2data usage
         * @author liqa
         */
        public static double[] str2data(String[] aStr) {
            double[] tData = new double[aStr.length];
            for (int i = 0; i < aStr.length; ++i) tData[i] = Double.parseDouble(aStr[i]);
            return tData;
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
                for (double[] subData : aData) tPrinter.println(String.join(",", data2str(subData)));
            }
        }
        public static void data2csv(IMatrix aData, String aFilePath, String... aHeads) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                if (aHeads!=null && aHeads.length>0) tPrinter.println(String.join(",", aHeads));
                for (IVector subData : aData.rows()) tPrinter.println(String.join(",", Code.map(subData.iterable(), String::valueOf)));
            }
        }
        public static void data2csv(IVector aData, String aFilePath) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                for (Double subData : aData.iterable()) tPrinter.println(subData);
            }
        }
        public static void data2csv(IVector aData, String aFilePath, String aHead) throws IOException {
            try (PrintStream tPrinter = toPrintStream(aFilePath)) {
                tPrinter.println(aHead);
                for (Double subData : aData.iterable()) tPrinter.println(subData);
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
        public static IMatrix csv2data(String aFilePath) throws IOException {return Matrices.from(csv2table(aFilePath).matrix());}
        
        
        /**
         * save table to csv file
         * @author liqa
         * @param aTable the Table to be saved
         * @param aFilePath csv file path to be saved
         */
        public static void table2csv(Table aTable, String aFilePath) throws IOException {
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
        public static Table csv2table(String aFilePath) throws IOException {
            try (BufferedReader tReader = toReader(aFilePath)) {
                List<double[]> tData = new ArrayList<>();
                String[] tHand = null;
                boolean tIsHead = true;
                String tLine;
                while ((tLine = tReader.readLine()) != null) {
                    String[] tTokens = Texts.splitComma(tLine);
                    if (tIsHead) {
                        double[] tFirstData = null;
                        try {tFirstData = str2data(tTokens);} catch (Exception ignored) {} // 直接看能否成功粘贴
                        if (tFirstData != null) tData.add(tFirstData);
                        else tHand = tTokens;
                        tIsHead = false;
                    } else {
                        tData.add(str2data(tTokens));
                    }
                }
                return tHand != null ? new Table(tHand, tData) : new Table(tData);
            }
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
