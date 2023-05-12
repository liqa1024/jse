package com.guan.code;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.guan.io.Decryptor;
import com.guan.io.Encryptor;
import com.guan.math.functional.IOperator1Full;
import com.guan.ssh.SerializableTask;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static com.guan.code.CS.ZL_OBJ;

/**
 * @author liqa
 * <p> utils of this project </p>
 */
public class UT {
    
    public static class Code {
        
        public static void printStackTrace(Exception e) {e.printStackTrace(System.out);}
        
        
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
         * map Iterable< T > to Iterable< R > like {@link Stream}.map
         * @author liqa
         */
        public static <R, T> Iterable<R> map(final Iterable<T> aIterable, final IOperator1Full<? extends R, ? super T> aOpt) {
            return () -> new Iterator<R>() {
                final Iterator<T> mIt = aIterable.iterator();
                @Override public boolean hasNext() {
                    return mIt.hasNext();
                }
                @Override public R next() {
                    if (hasNext()) {
                        return aOpt.cal(mIt.next());
                    }
                    throw new NoSuchElementException();
                }
            };
        }
        public static <R, T> List<R> map(final T[] aArray, final IOperator1Full<? extends R, ? super T> aOpt) {
            return new AbstractList<R>() {
                @Override public R get(int index) {return aOpt.cal(aArray[index]);}
                @Override public int size() {return aArray.length;}
            };
        }
        
        
        /**
         * Convert nested Iterable to a single one
         * @author liqa
         */
        public static <T> Iterable<T> toIterable(final Iterable<? extends Iterable<T>> aNestIterable) {
            return () -> new Iterator<T>() {
                private final Iterator<? extends Iterable<T>> mParentIt = aNestIterable.iterator();
                private Iterator<T> mIt = mParentIt.hasNext() ? mParentIt.next().iterator() : null;
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
                    }
                    throw new NoSuchElementException();
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
                @Override public Double set(int index, Double element) {return aData[index] = element;}
                @Override public int size() {return aData.length;}
            };
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
                    }
                    throw new NoSuchElementException();
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
    
    public static class Tasks {
        /**
         * Merge two tasks into one task
         * @author liqa
         * @param aTask1 the first Task will call
         * @param aTask2 the second Task will call
         * @return the Merged (serializable) Task
         */
        public static Task mergeTask(final @Nullable Task aTask1, final @Nullable Task aTask2) {
            if (aTask1 != null) {
                if (aTask2 == null) return aTask1;
                return new SerializableTask(() -> aTask1.call() && aTask2.call()) {
                    @Override public String toString() {return String.format("%s{%s:%s}", Type.MERGE.name(), (aTask1 instanceof SerializableTask) ? aTask1 : Type.NULL.name(), (aTask2 instanceof SerializableTask) ? aTask2 : Type.NULL.name());}
                };
            }
            return aTask2;
        }
        
        /**
         * Try to call a task
         * @author liqa
         * @param aTask the Task to call
         * @return true if it runs successfully, false otherwise
         */
        public static boolean tryTask(Task aTask) {
            if (aTask == null) return false;
            boolean tSuc;
            try {tSuc = aTask.call();} catch (Exception e) {return false;}
            return tSuc;
        }
        
        /**
         * Try to run a task with tolerant
         * @author liqa
         * @param aTask the Task to call
         * @param aTolerant tolerant number
         * @return true if it runs successfully, false otherwise
         */
        public static boolean tryTask(Task aTask, int aTolerant) {
            if (aTask == null) return false;
            boolean tSuc = false;
            for (int i = 0; i < aTolerant; ++i) {
                try {tSuc = aTask.call();} catch (Exception e) {continue;}
                if (tSuc) break;
            }
            return tSuc;
        }
    }
    
    public static class Hack {
        
        public static TaskCall<Object> getTaskCallOfMethod_(Class<?> aClazz, final @Nullable Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            final Method m = findMethod_(aClazz, aMethodName, fArgs);
            if (m == null) throw new NoSuchMethodException("No such method: " + aMethodName);
            convertArgs_(fArgs, m.getParameterTypes());
            return new TaskCall<>(() -> m.invoke(aInstance, fArgs));
        }
        public static TaskCall<Object> getTaskCallOfStaticMethod(String aClassName, String aMethodName, Object... aArgs) throws NoSuchMethodException {
            Class<?> tClass;
            try {tClass = Class.forName(aClassName);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}
            return getTaskCallOfMethod_(tClass, null, aMethodName, aArgs);
        }
        public static TaskRun          getTaskRunOfStaticMethod (              String aClassName, String aMethodName, Object... aArgs) throws NoSuchMethodException {return TaskRun.get(getTaskCallOfStaticMethod(aClassName, aMethodName, aArgs));}
        public static Task             getTaskOfStaticMethod    (              String aClassName, String aMethodName, Object... aArgs) throws NoSuchMethodException {return Task   .get(getTaskCallOfStaticMethod(aClassName, aMethodName, aArgs));}
        public static TaskCall<Object> getTaskCallOfMethod      (final @NotNull Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {return getTaskCallOfMethod_(aInstance.getClass(), aInstance, aMethodName, aArgs);}
        public static TaskRun          getTaskRunOfMethod       (final @NotNull Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {return TaskRun.get(getTaskCallOfMethod(aInstance, aMethodName, aArgs));}
        public static Task             getTaskOfMethod          (final @NotNull Object aInstance, String aMethodName, Object... aArgs) throws NoSuchMethodException {return Task   .get(getTaskCallOfMethod(aInstance, aMethodName, aArgs));}
        
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
    
    
    public static class Texts {
        /**
         * Start from aStartIdx to find the first index containing aContainStr
         * @author liqa
         * @param aLines where to find the aContainStr
         * @param aStartIdx the start position, include
         * @param aContainStr a string to find in aLines
         * @return the idx of aLines which contains aContainStr, or aLines.length if not find
         */
        public static int findLineContaining(String[] aLines, int aStartIdx, String aContainStr) {
            int tIdx = aStartIdx;
            while (tIdx < aLines.length) {
                if (aLines[tIdx].contains(aContainStr)) break;
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
        
        
    }
    
    public static class IO {
        /**
         * Wrapper of {@link Files}.write
         * @author liqa
         * @param aFilePath File to write
         * @param aLines Iterable String or String[]
         * @throws IOException when fail
         */
        public static void write(String aFilePath, String[] aLines, OpenOption... aOptions) throws IOException {Files.write(toAbsolutePath_(aFilePath), Arrays.asList(aLines), aOptions);}
        public static void write(String aFilePath, Iterable<? extends CharSequence> aLines, OpenOption... aOptions) throws IOException {Files.write(toAbsolutePath_(aFilePath), aLines, aOptions);}
        public static void write(String aFilePath, byte[] aData, OpenOption... aOptions) throws IOException {Files.write(toAbsolutePath_(aFilePath), aData, aOptions);}
        public static void write(String aFilePath, String aText, OpenOption... aOptions) throws IOException {Files.write(toAbsolutePath_(aFilePath), Collections.singletonList(aText), aOptions);}
        /**
         * Wrapper of {@link Files}.readAllBytes
         * @author liqa
         * @param aFilePath File to read
         * @return array of byte
         * @throws IOException when fail
         */
        public static byte[] readAllBytes(String aFilePath) throws IOException {return Files.readAllBytes(toAbsolutePath_(aFilePath));}
        /**
         * Wrapper of {@link Files}.readAllLines
         * @author liqa
         * @param aFilePath File to read
         * @return lines of String
         * @throws IOException when fail
         */
        public static String[] readAllLines(String aFilePath) throws IOException {return readAllLines_(aFilePath).toArray(new String[0]);}
        public static List<String> readAllLines_(String aFilePath) throws IOException {return Files.readAllLines(toAbsolutePath_(aFilePath));}
        /**
         * read all lines of the InputStream
         * @author liqa
         */
        public static String[] readAllLines(InputStream aInputStream) throws IOException {return readAllLines_(aInputStream).toArray(new String[0]);}
        public static List<String> readAllLines_(InputStream aInputStream) throws IOException {
            try (BufferedReader tReader = toReader(aInputStream)) {
                List<String> lines = new ArrayList<>();
                String tLine;
                while ((tLine = tReader.readLine()) != null) lines.add(tLine);
                return lines;
            }
        }
        
        /**
         * remove the Directory, will remove the subdirectories recursively
         * @author liqa
         * @param aDir the Directory will be removed
         */
        public static void rmdir(String aDir) throws IOException {removeDir(aDir);}
        public static void removeDir(String aDir) throws IOException {
            if (!aDir.isEmpty() && !aDir.endsWith("/") && !aDir.endsWith("\\")) aDir += "/";
            if (!exists(aDir)) return;
            removeDir_(aDir);
        }
        public static void removeDir_(String aDir) throws IOException {
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
        public static boolean mkdir(String aDir) {return makeDir(aDir);} // can mkdir nested
        public static boolean makeDir(String aDir) {try {Files.createDirectories(toAbsolutePath_(aDir)); return true;} catch (IOException e) {return false;}} // can mkdir nested
        public static boolean isDir(String aDir) {return Files.isDirectory(toAbsolutePath_(aDir));}
        public static boolean isFile(String aFilePath) {return Files.isRegularFile(toAbsolutePath_(aFilePath));}
        public static boolean exists(String aPath) {return Files.exists(toAbsolutePath_(aPath));}
        public static void delete(String aPath) throws IOException {Files.deleteIfExists(toAbsolutePath_(aPath));} // can delete not exist path
        public static void copy(String aSourcePath, String aTargetPath, CopyOption... aOptions) throws IOException {Files.copy(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath), aOptions);}
        public static void move(String aSourcePath, String aTargetPath, CopyOption... aOptions) throws IOException {Files.move(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath), aOptions);}
        public static String[] list(String aDir) {return toFile(aDir).list();} // use the File.list not Files.list to get the simple result
        
        /** output stuffs */
        public static PrintStream    toPrintStream (String aFilePath, OpenOption... aOptions) throws IOException {return new PrintStream(toOutputStream(aFilePath, aOptions));}
        public static OutputStream   toOutputStream(String aFilePath, OpenOption... aOptions) throws IOException {return Files.newOutputStream(UT.IO.toAbsolutePath_(aFilePath), aOptions);}
        public static BufferedWriter toWriter      (Path aPath, OpenOption... aOptions) throws IOException {return Files.newBufferedWriter(aPath, aOptions);}
        public static BufferedWriter toWriter      (String aFilePath, OpenOption... aOptions) throws IOException {return toWriter(UT.IO.toAbsolutePath_(aFilePath), aOptions);}
        public static BufferedWriter toWriter      (OutputStream aOutputStream) {return new BufferedWriter(new OutputStreamWriter(aOutputStream));}
        
        /** input stuffs */
        public static InputStream    toInputStream(String aFilePath) throws IOException {return Files.newInputStream(UT.IO.toAbsolutePath_(aFilePath));}
        public static BufferedReader toReader     (Path aPath) throws IOException {return Files.newBufferedReader(aPath);}
        public static BufferedReader toReader     (String aFilePath) throws IOException {return toReader(UT.IO.toAbsolutePath_(aFilePath));}
        public static BufferedReader toReader     (InputStream aInputStream) {return new BufferedReader(new InputStreamReader(aInputStream));}
        public static BufferedReader toReader     (URL aFileURL) throws IOException {return toReader(aFileURL.openStream());}
        
        /** misc stuffs */
        public static File toFile(String aFilePath) {return toAbsolutePath_(aFilePath).toFile();}
        
        
        /**
         * convert between json and map, Encryption support
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
        public static Map<?, ?> json2map(String aFilePath, String aKey) throws Exception {
            Decryptor tDecryptor = new Decryptor(aKey);
            return (Map<?, ?>) (new JsonSlurper()).parseText(tDecryptor.get(UT.IO.readAllBytes(aFilePath)));
        }
        public static void map2json(Map<?, ?> aMap, String aFilePath, String aKey) throws Exception {
            Encryptor tEncryptor = new Encryptor(aKey);
            UT.IO.write(aFilePath, tEncryptor.getData((new JsonBuilder(aMap)).toString()));
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
            CSVFormat tCSVFormat = (aHeads != null && aHeads.length == aData[0].length) ? CSVFormat.DEFAULT.builder().setHeader(aHeads).build() : CSVFormat.DEFAULT;
            try (CSVPrinter tPrinter = new CSVPrinter(toWriter(aFilePath), tCSVFormat)) {
                for (double[] subData : aData) tPrinter.printRecord((Object[]) data2str(subData));
            }
        }
        /**
         * read matrix data from csv file
         * @author liqa
         * @param aFilePath csv file path to read
         * @return matrix data (or with String[] head in Pair if use csv2dataWithHand)
         */
        public static double[][] csv2data(String aFilePath) throws IOException {
            try (CSVParser tParser = CSVParser.parse(toAbsolutePath_(aFilePath), StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
                List<double[]> tData = new ArrayList<>();
                boolean tIsHead = true;
                for (CSVRecord tCSVRecord : tParser) {
                    if (tIsHead) {
                        double[] tFirstData = null;
                        try {tFirstData = str2data(tCSVRecord.values());} catch (Exception ignored) {} // 直接看能否成功粘贴
                        if (tFirstData != null) tData.add(str2data(tCSVRecord.values()));
                        tIsHead = false;
                    } else {
                        tData.add(str2data(tCSVRecord.values()));
                    }
                }
                return tData.toArray(new double[0][]);
            }
        }
        public static Pair<double[][], String[]> csv2dataWithHand(String aFilePath) {
            aFilePath = toAbsolutePath(aFilePath);
            try (CSVParser tParser = CSVParser.parse(new File(aFilePath), StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
                List<double[]> tData = new ArrayList<>();
                String[] tHand = null;
                boolean tIsHead = true;
                for (CSVRecord tCSVRecord : tParser) {
                    if (tIsHead) {
                        tHand = tCSVRecord.values();
                        double[] tFirstData = null;
                        try {tFirstData = str2data(tHand);} catch (Exception ignored) {} // 直接看能否成功粘贴
                        if (tFirstData != null) {
                            tData.add(str2data(tCSVRecord.values()));
                            tHand = null;
                        }
                        tIsHead = false;
                    } else {
                        tData.add(str2data(tCSVRecord.values()));
                    }
                }
                return new Pair<>(tData.toArray(new double[0][]), tHand);
            } catch (IOException e) {
                throw new RuntimeException(e);
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
                tProcess = Runtime.getRuntime().exec(System.getProperty("os.name").toLowerCase().contains("windows") ? "cmd /c cd" : "pwd");
            } catch (IOException e) {
                return System.getProperty("user.home");
            }
            
            String wd;
            try (BufferedReader tReader = new BufferedReader(new InputStreamReader(tProcess.getInputStream()))) {
                tProcess.waitFor();
                wd = tReader.readLine().trim();
            } catch (IOException | InterruptedException e) {
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
            String wd = pwd();
            System.setProperty("user.dir", wd);
            WORKING_PATH = Paths.get(wd);
        }
        static {init();}
    }
}
