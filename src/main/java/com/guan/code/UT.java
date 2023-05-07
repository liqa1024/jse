package com.guan.code;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMapBuilder;
import com.guan.ssh.SerializableTask;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> utils of this project </p>
 */
public class UT {
    
    public static class Code {
        /**
         * the range function similar to python
         * <p> only support in aStep > 0 for now </p>
         * @author liqa
         * @param aStart the start value, include
         * @param aStop the stop position, exclude
         * @return A iterable container
         */
        public static Iterable<Integer> range(int aStart, int aStop           ) {return range(aStart, aStop, 1);}
        public static Iterable<Integer> range(int aStart, int aStop, int aStep) {
            aStep = Math.max(aStep, 1);
            aStop = Math.max(aStop, aStart);
            return range_(aStart, aStop, aStep);
        }
        public static Iterable<Integer> range_(final int aStart, final int aStop) {return range_(aStart, aStop, 1);}
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
        
        public static TaskCall<Object> getTaskCallOfMethod_(Class<?> aClazz, final @Nullable Object aInstance, String aMethodName, Object... aArgs) {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            final Method m = findMethod_(aClazz, aMethodName, fArgs);
            if (m == null) throw new RuntimeException("No such method: " + aMethodName);
            convertArgs_(fArgs, m.getParameterTypes());
            return new TaskCall<>(() -> m.invoke(aInstance, fArgs));
        }
        public static TaskCall<Object> getTaskCallOfStaticMethod(String aClassName, String aMethodName, Object... aArgs) {
            Class<?> tClass;
            try {tClass = Class.forName(aClassName);} catch (ClassNotFoundException e) {throw new RuntimeException(e);}
            return getTaskCallOfMethod_(tClass, null, aMethodName, aArgs);
        }
        public static TaskRun          getTaskRunOfStaticMethod (              String aClassName, String aMethodName, Object... aArgs) {return TaskRun.get(getTaskCallOfStaticMethod(aClassName, aMethodName, aArgs));}
        public static Task             getTaskOfStaticMethod    (              String aClassName, String aMethodName, Object... aArgs) {return Task   .get(getTaskCallOfStaticMethod(aClassName, aMethodName, aArgs));}
        public static TaskCall<Object> getTaskCallOfMethod      (final @NotNull Object aInstance, String aMethodName, Object... aArgs) {return getTaskCallOfMethod_(aInstance.getClass(), aInstance, aMethodName, aArgs);}
        public static TaskRun          getTaskRunOfMethod       (final @NotNull Object aInstance, String aMethodName, Object... aArgs) {return TaskRun.get(getTaskCallOfMethod(aInstance, aMethodName, aArgs));}
        public static Task             getTaskOfMethod          (final @NotNull Object aInstance, String aMethodName, Object... aArgs) {return Task   .get(getTaskCallOfMethod(aInstance, aMethodName, aArgs));}
        
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
        public static final @Unmodifiable BiMap<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = (new ImmutableBiMapBuilder<Class<?>, Class<?>>())
            .put(boolean.class,   Boolean.class)
            .put(   byte.class,      Byte.class)
            .put(   char.class, Character.class)
            .put(  short.class,     Short.class)
            .put(    int.class,   Integer.class)
            .put(   long.class,      Long.class)
            .put(  float.class,     Float.class)
            .put( double.class,    Double.class)
            .getBiMap();
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
         * Make a directory and support create nested directories.
         * @author liqa
         * @param aDir the directory will be made
         * @return true if the directory already exists or the creation is successful, and false if it fails
         */
        public static boolean mkdir(String aDir) {
            File tFile = new File(toAbsolutePath(aDir)); return tFile.isDirectory() || tFile.mkdirs();
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
        public static void data2csv(double[][] aData, String aFilePath, String... aHeads) {
            aFilePath = toAbsolutePath(aFilePath);
            CSVFormat tCSVFormat = (aHeads != null && aHeads.length == aData[0].length) ? CSVFormat.DEFAULT.builder().setHeader(aHeads).build() : CSVFormat.DEFAULT;
            try (CSVPrinter tPrinter = new CSVPrinter(new FileWriter(aFilePath), tCSVFormat)) {
                for (double[] subData : aData) tPrinter.printRecord((Object[]) data2str(subData));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        /**
         * read matrix data from csv file
         * @author liqa
         * @param aFilePath csv file path to read
         * @return matrix data (or with String[] head in Pair if use csv2dataWithHand)
         */
        public static double[][] csv2data(String aFilePath) {
            aFilePath = toAbsolutePath(aFilePath);
            try (CSVParser tParser = CSVParser.parse(new File(aFilePath), StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
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
            } catch (IOException e) {
                throw new RuntimeException(e);
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
         * read all lines of the InputStream
         * @author liqa
         */
        public static List<String> readAllLines(InputStream aInputStream) throws IOException {
            try (BufferedReader tReader = new BufferedReader(new InputStreamReader(aInputStream))) {
                List<String> lines = new ArrayList<>();
                String tLine;
                while ((tLine = tReader.readLine()) != null) lines.add(tLine);
                return lines;
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
        public static String toAbsolutePath(String aPath) {return WORKING_PATH.resolve(aPath).toString();}
        
        
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
