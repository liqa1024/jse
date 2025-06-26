package jse.code;

import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.yaml.YamlBuilder;
import groovy.yaml.YamlSlurper;
import jse.cache.ByteArrayCache;
import jse.code.collection.AbstractCollections;
import jse.code.collection.NewCollections;
import jse.code.functional.IUnaryFullOperator;
import jse.code.io.CharScanner;
import jse.code.io.UnicodeReader;
import jse.math.function.IFunc1;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.table.ITable;
import jse.math.table.Table;
import jse.math.table.Tables;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.codehaus.groovy.util.CharSequenceReader;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static jse.code.CS.*;
import static jse.code.OS.USER_HOME;
import static jse.code.OS.WORKING_DIR_PATH;


/**
 * 通用的文件操作工具类；现在变为独立的类而不是放在 {@link UT} 中。
 * <p>
 * 相比 java 自带的 {@link java.nio.file.Files} 以及
 * <a href="https://www.groovy-lang.org/groovy-dev-kit.html#_working_with_io">
 * groovy 的 IO 接口 </a>，主要的区别有：<ol>
 * <li> 直接基于字符串 {@link String} 表示的路径进行操作，而不需要转为 {@link java.nio.file.Path}
 * <li> 在创建文件或目录时，对应目录不存在时会自动创建，并且会自动递归创建子目录
 * <li> 文本文件编码格式统一为 {@code UTF-8}，换行符统一为 {@code \n}（即 {@code LF}），不用考虑文件格式的问题
 * </ol>
 * 因此在绝大部分时候都建议统一使用此工具类中的方法，而不需要再去使用原始的接口。
 * <p>
 * 在通过 matlab 调用 jar 使用时，获取到的工作目录会出错，{@link OS}
 * 的初始化中专门修复了这个问题，但使用 java 内部的接口获取绝对路径时依旧会出错；
 * 为了避免这个问题应当尽量使用本类中的接口进行操作，至少应当使用 {@link IO#toAbsolutePath(String)}
 * 来获取到正确的绝对路径。
 * <p>
 * 在 jse 内部对于文件夹的路径都会统一保留结尾的斜杠 {@code "/"}（或者对于完全为空的路径则保持为空），
 * 从而可以保证在需要拼接得到文件路径时可以直接相加。而对于外部输入的文件夹路径则没有强制要求，
 * 如果有拼接需求则也可以通过 {@link IO#toInternalValidDir(String)} 来为结尾补上这个斜杠 {@code "/"}
 * <p>
 * 对于 java 的读写流，jse 主要使用 {@link BufferedReader} 作为读取流，使用
 * {@link BufferedWriter} 并包装成通用的 {@link IO.IWriteln} 来作为写入流；
 * 可以通过 {@link IO#toReader} 和 {@link IO#toWriteln} 方法来获取这些流。
 *
 * @see IO.Text IO.Text: 文件操作中的字符串操作工具类
 * @see UT UT: 通用方法工具类
 * @see CS CS: 全局常量工具类
 * @see OS OS: 系统操作工具类
 * @author liqa
 */
public class IO {
    static {jse.code.OS.InitHelper.init();}
    private final static int BUFFER_SIZE = 8192;
    
    /**
     * 文本操作的工具类，这里包含只进行文本操作，但不进行文件读写的一些方法
     * <p>
     * 例如常见的字符串按照空格或者逗号切分（切分后忽略空格）{@link Text#splitStr(String)}，
     * 以及专门对纯数字情况进行优化的 {@link Text#str2data(String, int)} 等通用方法。
     * @author liqa
     */
    public static class Text {
        /**
         * 将输入字符串转换为读取此字符串的一个 {@link Reader}，主要用于内部使用
         * @param aStr 需要读取的字符串
         * @return 读取输入字符串的 {@link Reader}
         * @see #toReader(CharSequence)
         * @see CharSequenceReader
         */
        public static Reader toReader_(CharSequence aStr) {return new CharSequenceReader(aStr);}
        /**
         * 将输入字符串转换为读取此字符串的一个 {@link BufferedReader}，主要用于内部使用
         * @param aStr 需要读取的字符串
         * @return 读取输入字符串的 {@link BufferedReader}
         * @see CharSequenceReader
         */
        public static BufferedReader toReader(CharSequence aStr) {return new BufferedReader(toReader_(aStr));}
        public static void eachLine(final CharSequence aStr, final Consumer<String> aCon) throws IOException {
            try (BufferedReader tReader = toReader(aStr)) {
                String tLine;
                while ((tLine = tReader.readLine()) != null) aCon.accept(tLine);
            }
        }
        /**
         * 按行遍历输入的字符串，并且可以提供对应的行号（从 0 开始）；
         * 主要用于内部使用，在 groovy 脚本中可以直接通过：
         * <pre> {@code
         * def str = 'aaa\nbbb\nccc'
         * str.eachLine {line ->
         *     //
         * }
         * } </pre>
         * 来按行遍历字符串
         *
         * @param aStr 需要遍历的字符串
         * @param aCon 传入的遍历行的相关代码块，提供两个参数，第一个为当前行的字符串，第二个为当前的行号（从 0 开始）
         * @see StringGroovyMethods#eachLine(CharSequence, Closure)
         */
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
        
        /**
         * 将一个字符串组成的列表转换成 {@code String[]}
         * 的数组形式；主要用于兼容 groovy 中的字符串列表输入
         * <p>
         * 在 groovy 脚本中可以直接通过 {@code as String[]} 进行转换
         *
         * @param aLines 字符串组成的列表
         * @return 转换后的字符串数组
         */
        public static String[] toArray(Collection<? extends CharSequence> aLines) {
            String[] rArray = new String[aLines.size()];
            int i = 0;
            for (CharSequence tStr : aLines) {
                rArray[i] = UT.Code.toString(tStr);
                ++i;
            }
            return rArray;
        }
        
        /**
         * 将一个概率值（一般为 0~1）转换为百分数字符串（带有百分号
         * {@code %}），主要用于各种输出显示
         * @param aProb 概率值
         * @return 百分比数字符串
         */
        public static String percent(double aProb) {
            return String.format("%.2f", aProb*100) + "%";
        }
        
        /**
         * 重复给定 {@code char} 指定次数，照搬 {@code me.tongfei.progressbar.Util#repeat(char, int)}
         * @param aChar 需要重复的 {@code char}
         * @param aNum 需要重复的次数
         * @return 重复后得到的字符串
         * @author Tongfei Chen, liqa
         */
        public static String repeat(char aChar, int aNum) {
            if (aNum <= 0) return "";
            char[] tChars = new char[aNum];
            Arrays.fill(tChars, aChar);
            return new String(tChars);
        }
        
        /**
         * 重复给定字符串，使用类似 Groovy 中对于 {@link String} 的乘法的方法
         * @param aStr 需要重复的字符串
         * @param aNum 需要重复的次数
         * @return 重复后得到的字符串
         * @see StringGroovyMethods#multiply(CharSequence, Number)
         */
        public static String repeat(CharSequence aStr, int aNum) {
            if (aNum <= 0) return "";
            StringBuilder rStr = new StringBuilder(aStr);
            for (int i = 1; i < aNum; ++i) {
                rStr.append(aStr);
            }
            return rStr.toString();
        }
        
        
        private static boolean charIsDigitDecimal_(int aChar) {
            switch(aChar) {
            case '-': case '+': case '.': {
                return true;
            }
            default: {
                return CharScanner.isDigit(aChar);
            }}
        }
        /**
         * 将单个字符串转为 Number 值，要求前后不能含有任何空格；
         * 自动检测整数类型和小数类型，对于小数会返回{@link Double}，整数会根据大小返回
         * {@link Integer} 或 {@link Long}
         * @param aStr 需要进行转换的字符串
         * @return 转换得到的数字，如果转换失败则返回 {@code null}
         */
        public static @Nullable Number str2number(String aStr) {
            // 先直接转 char[]，适配 groovy-json 的 CharScanner
            char[] tChar = aStr.toCharArray();
            // 先判断开头，这样可以避免抛出错误带来的性能损失
            if (!charIsDigitDecimal_(tChar[0])) return null;
            // 但一般还是使用 try，这样避免意外的情况
            try {return CharScanner.parseNumber(tChar, 0, tChar.length);}
            catch (Exception ignored) {}
            return null;
        }
        
        /**
         * 从开始索引找到字符串中首个非空字符的索引，有：
         * <pre> {@code
         * import jse.code.IO
         *
         * assert IO.Text.findNoBlankIndex('  cde cf   ', 0) == 2
         * assert IO.Text.findNoBlankIndex('  cde cf   ', 3) == 3
         * assert IO.Text.findNoBlankIndex('  cde cf   ', 5) == 6
         * assert IO.Text.findNoBlankIndex('  cde cf   ', 8) == -1
         * } </pre>
         * @param aStr 需要查找的字符串
         * @param aStart 开始位置的索引
         * @return 找到的第一个非空字符串索引，如果没有找到则返回 {@code -1}
         */
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
        /**
         * 从开始索引找到字符串中首个空字符的索引，有：
         * <pre> {@code
         * import jse.code.IO
         *
         * assert IO.Text.findBlankIndex('ab  cde cf', 0) == 2
         * assert IO.Text.findBlankIndex('ab  cde cf', 3) == 3
         * assert IO.Text.findBlankIndex('ab  cde cf', 5) == 7
         * assert IO.Text.findBlankIndex('ab  cde cf', 8) == -1
         * } </pre>
         * @param aStr 需要查找的字符串
         * @param aStart 开始位置的索引
         * @return 找到的第一个空字符串索引，如果没有找到则返回 {@code -1}
         */
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
         * 将字符串转换成 jse 的向量数据 {@link Vector}，认为这个字符串是按照逗号
         * {@code ","} 或者空格 {@code " "} 分割的数字组成的，会忽略每个数据开头和结尾的任意数量空格，
         * 任何读取失败的数字都会存为 {@link Double#NaN} 而不是抛出错误。
         * <p>
         * 这样设计主要确保支持 lammps 或其他软件的输出文件中使用的空格分割的数据，并也能兼容一般的逗号分割的 csv 文件。
         * <p>
         * 此操作进行了专门优化，使用了 groovy-json 中的 {@link CharScanner#parseDouble(char[], int, int)}
         * 等方法，总体比直接 {@code split} 并用 java 的 {@link Double#parseDouble(String)} 快一倍以上。
         *
         * @param aStr 需要进行转换的字符串
         * @param aLength 期望的向量长度，字符串超出的数据会忽略，不足的会填充 {@link Double#NaN}
         * @return 转换得到的向量 {@link Vector}
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
        
        /**
         * 判断输入字符串是否为空或者是空格，此方法等效
         * {@link StringGroovyMethods#isBlank(CharSequence)}
         * @param self 需要判断的字符串
         * @return 是否为空
         * @see CS#BLANKS_OR_EMPTY
         */
        @Contract("null -> true")
        public static boolean isBlank(final CharSequence self) {
            if (self == null) return true;
            return BLANKS_OR_EMPTY.matcher(self).matches();
        }
        /**
         * 不考虑大小写的判断字符串是否包含给定字符串，此方法等效
         * {@link StringGroovyMethods#containsIgnoreCase(CharSequence, CharSequence)}
         * @param self 需要判断的主字符串
         * @param searchString 需要用于搜索的字符串
         * @return 是否包含给定字符串
         */
        public static boolean containsIgnoreCase(final CharSequence self, final CharSequence searchString) {return StringGroovyMethods.containsIgnoreCase(self, searchString);}
        
        /**
         * Start from aStart to find the first index containing aContainStr
         * @param aLines where to find the aContainStr
         * @param aStart the start index, include
         * @param aContainStr a string to find in aLines
         * @param aIgnoreCase if true, ignore case when comparing characters (default: {@code false})
         * @return the idx of aLines which contains aContainStr, or {@code -1} if not find
         */
        public static int findLineContaining(List<String> aLines, int aStart, String aContainStr, boolean aIgnoreCase) {
            final int tSize = aLines.size();
            for (; aStart < tSize; ++aStart) {
                if (aIgnoreCase) {
                    if (containsIgnoreCase(aLines.get(aStart), aContainStr)) return aStart;
                } else {
                    if (aLines.get(aStart).contains(aContainStr)) return aStart;
                }
            }
            return -1;
        }
        /**
         * Start from aStart to find the first index containing aContainStr
         * @param aLines where to find the aContainStr
         * @param aStart the start index, include
         * @param aContainStr a string to find in aLines
         * @return the idx of aLines which contains aContainStr, or {@code -1} if not find
         */
        public static int findLineContaining(List<String> aLines, int aStart, String aContainStr) {return findLineContaining(aLines, aStart, aContainStr, false);}
        
        /**
         * 读取 aReader 直到包含 aContainStr 的行
         * @param aReader 用来读取的 {@link BufferedReader}
         * @param aContainStr 需要查找的字符串
         * @param aIgnoreCase 是否忽略大小写，默认为 {@code false}
         * @return 找到的包含指定字符串的行，如果没有找到则返回 {@code null}
         */
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
        /**
         * 读取 aReader 直到包含 aContainStr 的行
         * @param aReader 用来读取的 {@link BufferedReader}
         * @param aContainStr 需要查找的字符串
         * @return 找到的包含指定字符串的行，如果没有找到则返回 {@code null}
         */
        public static @Nullable String findLineContaining(BufferedReader aReader, String aContainStr) throws IOException {return findLineContaining(aReader, aContainStr, false);}
        
        /**
         * 读取 aReader 直到非空行
         * @param aReader 用来读取的 {@link BufferedReader}
         * @return 找到非空的行，如果没有找到则返回 {@code null}
         */
        public static @Nullable String findLineNoBlank(BufferedReader aReader) throws IOException {
            String tLine;
            while ((tLine = aReader.readLine()) != null) {
                if (!isBlank(tLine)) return tLine;
            }
            return null;
        }
        
        /**
         * Splits a string separated by blank characters into multiple strings
         * <p>
         * will automatically ignore multiple spaces and the beginning and end spaces, we have:
         * <pre> {@code
         * import jse.code.IO
         *
         * assert IO.Text.splitBlank(' ab  cde cf  ') == ['ab', 'cde', 'cf']
         * } </pre>
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitBlank(String aStr) {
            return BLANKS.split(aStr.trim(), -1);
        }
        /**
         * Splits a string separated by comma(",") characters into multiple strings
         * <p>
         * will automatically ignore multiple spaces and the beginning and end spaces, we have:
         * <pre> {@code
         * import jse.code.IO
         *
         * assert IO.Text.splitComma(' ab , cde, cf  ') == ['ab', 'cde', 'cf']
         * } </pre>
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitComma(String aStr) {
            return COMMA.split(aStr.trim(), -1);
        }
        /**
         * 匹配使用空格分割或者逗号（{@code ","}）分割的字符串，可以出现混合
         * <p>
         * 会自动忽略多余的空格，有：
         * <pre> {@code
         * import jse.code.IO
         *
         * assert IO.Text.splitStr(' ab  cde , cf  ') == ['ab', 'cde', 'cf']
         * } </pre>
         * @param aStr input string
         * @return the split sting in array
         */
        public static String[] splitStr(String aStr) {
            return COMMA_OR_BLANKS.split(aStr.trim(), -1);
        }
        
        /**
         * 拆分 SLURM 系统中使用的环境变量 {@code SLURM_NODELIST} 成为可以直接使用的列表形式
         * <p>
         * 这里考虑了 SLURM 系统中各种神奇的格式，例如不一定是 {@code "cn"} 开头，节点数可能存在
         * 开头填充 {@code "0"} 的情况等
         *
         * @param aRawNodeList 原始 SLURM 环境变量 {@code SLURM_NODELIST} 字符串
         * @return 拆分后的列表形式的节点列表
         * @see OS.Slurm#NODE_LIST
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
         * 将一个 json 字符串转换成 {@link Map}，这里直接调用了
         * {@link JsonSlurper#parseText(String)}
         * @param aText 需要解析的 json 字符串
         * @return 解析得到的 {@link Map}
         * @see IO#json2map(String)
         * @see IO.Text#map2json(Map)
         */
        public static Map<?, ?> json2map(String aText) {
            return (Map<?, ?>) (new JsonSlurper()).parseText(aText);
        }
        /**
         * 将一个 {@link Map} 转换成 json 格式的字符串，这里直接调用了
         * {@link JsonBuilder#toString()}
         * <p>
         * 调用 {@link IO.Text#map2json(Map, boolean)} 来输出格式化后更加易读的
         * json 字符串。
         * @param aMap 需要编码成 json 的 {@link Map}
         * @return 编码得到的 json 字符串
         * @see IO#map2json(Map, String)
         * @see IO.Text#json2map(String)
         */
        public static String map2json(Map<?, ?> aMap) {
            return map2json(aMap, false);
        }
        /**
         * 将一个 {@link Map} 转换成 json 格式的字符串，这里直接调用了
         * {@link JsonBuilder#toPrettyString()} 或 {@link JsonBuilder#toString()}
         * @param aMap 需要编码成 json 的 {@link Map}
         * @param aPretty 是否自动格式化字符串保证较易读的形式，默认为 {@code false}
         * @return 编码得到的 json 字符串
         * @see IO#map2json(Map, String, boolean)
         * @see IO.Text#json2map(String)
         */
        public static String map2json(Map<?, ?> aMap, boolean aPretty) {
            JsonBuilder tBuilder = new JsonBuilder();
            tBuilder.call(aMap);
            return aPretty ? tBuilder.toPrettyString() : tBuilder.toString();
        }
        /**
         * 将一个 yaml 字符串转换成 {@link Map}，这里直接调用了
         * {@link YamlSlurper#parseText(String)}
         * @param aText 需要解析的 yaml 字符串
         * @return 解析得到的 {@link Map}
         * @see IO#yaml2map(String)
         * @see IO.Text#map2yaml(Map)
         */
        public static Map<?, ?> yaml2map(String aText) {
            return (Map<?, ?>) (new YamlSlurper()).parseText(aText);
        }
        /**
         * 将一个 {@link Map} 转换成 yaml 格式的字符串，这里直接调用了
         * {@link YamlBuilder#toString()}
         * @param aMap 需要编码成 yaml 的 {@link Map}
         * @return 编码得到的 yaml 字符串
         * @see IO#map2yaml(Map, String)
         * @see IO.Text#yaml2map(String)
         */
        public static String map2yaml(Map<?, ?> aMap) {
            YamlBuilder tBuilder = new YamlBuilder();
            tBuilder.call(aMap);
            return tBuilder.toString();
        }
    }
    
    /**
     * 判断一个文件夹路径 aDir 是否符合 jse 内部使用的格式，内部使用时需要 aDir
     * 的结尾为斜杠 {@code "/"} 或者本身为空，从而保证可以直接拼接文件名；
     * 主要用于内部使用
     * @param aDir 需要判断的文件夹路径字符串
     * @return 是否符合内部使用的格式要求
     * @see IO#toInternalValidDir(String)
     */
    @Contract(pure = true) public static boolean isInternalValidDir(@NotNull String aDir) {
        return aDir.isEmpty() || aDir.endsWith("/") || aDir.endsWith("\\");
    }
    /**
     * 将一个文件夹路径 aDir 转换为符合 jse 内部使用的格式，即 aDir
     * 的结尾为斜杠 {@code "/"} 或者本身为空，从而保证可以直接拼接文件名；
     * 主要用于内部使用
     * @param aDir 需要合法化的文件夹路径字符串
     * @return 合法化后的文件夹路径字符串
     * @see IO#isInternalValidDir(String)
     */
    @CheckReturnValue @Contract(pure = true) public static String toInternalValidDir(@NotNull String aDir) {
        return isInternalValidDir(aDir) ? aDir : (aDir+"/");
    }
    
    /**
     * 向指定路径的文件写入多行字符串，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 会统一在最末尾添加一个换行符 {@code "\n"}，这不会影响 {@link IO#readAllLines(String)}
     * 的结果（自动忽略最末尾的单个换行符）
     * <p>
     * 如果希望边操作边写入从而节省内存占用，可以通过 {@link IO#toWriteln(String)}
     * 来创建一个写入流
     *
     * @param aFilePath 需要写入的文件路径
     * @param aLines 需要写入的多行文本
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see Files#write(Path, Iterable, OpenOption...)
     * @see IO#readAllLines(String)
     * @see IO#write(String, String[], OpenOption...)
     */
    public static void write(String aFilePath, String... aLines) throws IOException {write(aFilePath, aLines, ZL_OO);}
    /**
     * 向指定路径的文件写入多行字符串，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 会统一在最末尾添加一个换行符 {@code "\n"}，这不会影响 {@link IO#readAllLines(String)}
     * 的结果（自动忽略最末尾的单个换行符）
     * <p>
     * 如果希望边操作边写入从而节省内存占用，可以通过 {@link IO#toWriteln(String)}
     * 来创建一个写入流
     *
     * @param aFilePath 需要写入的文件路径
     * @param aLines 需要写入的多行文本
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see Files#write(Path, Iterable, OpenOption...)
     * @see IO#readAllLines(String)
     * @see IO#write(String, Iterable, OpenOption...)
     */
    public static void write(String aFilePath, Iterable<? extends CharSequence> aLines) throws IOException {write(aFilePath, aLines, ZL_OO);}
    /**
     * 向指定路径的文件写入单行字符串，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 会在最末尾添加一个换行符 {@code "\n"}，如果希望和 {@link IO#readAllText(String)}
     * 行为一致，则应当使用 {@link IO#writeText(String, String)} 写入文件
     *
     * @param aFilePath 需要写入的文件路径
     * @param aLine 需要写入的单行文本
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see Files#write(Path, Iterable, OpenOption...)
     * @see IO#readAllLines(String)
     * @see IO#writeText(String, String)
     * @see IO#write(String, String, OpenOption...)
     */
    public static void write(String aFilePath, String aLine) throws IOException {write(aFilePath, aLine, ZL_OO);}
    /**
     * 向指定路径的文件直接写入二进制数据，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     *
     * @param aFilePath 需要写入的文件路径
     * @param aData 需要写入的二进制数据
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see Files#write(Path, byte[], OpenOption...)
     * @see IO#readAllBytes(String)
     * @see IO#write(String, byte[], OpenOption...)
     */
    public static void write(String aFilePath, byte[] aData) throws IOException {write(aFilePath, aData, ZL_OO);}
    /**
     * 向指定路径的文件写入多行字符串，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 会统一在最末尾添加一个换行符 {@code "\n"}，这不会影响 {@link IO#readAllLines(String)}
     * 的结果（自动忽略最末尾的单个换行符）
     * <p>
     * 如果希望边操作边写入从而节省内存占用，可以通过 {@link IO#toWriteln(String)}
     * 来创建一个写入流
     *
     * @param aFilePath 需要写入的文件路径
     * @param aLines 需要写入的多行文本
     * @param aOptions 可选的额外写入设置
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see StandardOpenOption
     * @see Files#write(Path, Iterable, OpenOption...)
     * @see IO#readAllLines(String)
     */
    public static void write(String aFilePath, String[] aLines, OpenOption... aOptions) throws IOException {write(aFilePath, AbstractCollections.from(aLines), aOptions);}
    /**
     * 向指定路径的文件写入多行字符串，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 会统一在最末尾添加一个换行符 {@code "\n"}，这不会影响 {@link IO#readAllLines(String)}
     * 的结果（自动忽略最末尾的单个换行符）
     * <p>
     * 如果希望边操作边写入从而节省内存占用，可以通过 {@link IO#toWriteln(String)}
     * 来创建一个写入流
     *
     * @param aFilePath 需要写入的文件路径
     * @param aLines 需要写入的多行文本
     * @param aOptions 可选的额外写入设置
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see StandardOpenOption
     * @see Files#write(Path, Iterable, OpenOption...)
     * @see IO#readAllLines(String)
     */
    public static void write(String aFilePath, Iterable<? extends CharSequence> aLines, OpenOption... aOptions) throws IOException {write(toAbsolutePath_(aFilePath), aLines, aOptions);}
    /**
     * 向指定路径的文件写入单行字符串，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 会在最末尾添加一个换行符 {@code "\n"}，如果希望和 {@link IO#readAllText(String)}
     * 行为一致，则应当使用 {@link IO#writeText(String, String)} 写入文件
     *
     * @param aFilePath 需要写入的文件路径
     * @param aLine 需要写入的单行文本
     * @param aOptions 可选的额外写入设置
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see StandardOpenOption
     * @see Files#write(Path, Iterable, OpenOption...)
     * @see IO#readAllLines(String)
     * @see IO#writeText(String, String, OpenOption...)
     */
    public static void write(String aFilePath, String aLine, OpenOption... aOptions) throws IOException {write(aFilePath, Collections.singletonList(aLine), aOptions);}
    /**
     * 向指定路径的文件直接写入二进制数据，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     *
     * @param aFilePath 需要写入的文件路径
     * @param aData 需要写入的二进制数据
     * @param aOptions 可选的额外写入设置
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see StandardOpenOption
     * @see Files#write(Path, byte[], OpenOption...)
     * @see IO#readAllBytes(String)
     */
    public static void write(String aFilePath, byte[] aData, OpenOption... aOptions) throws IOException {write(toAbsolutePath_(aFilePath), aData, aOptions);}
    /** {@link IO#write(String, byte[], OpenOption...)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void write(Path aPath, byte[] aData, OpenOption... aOptions) throws IOException {validPath(aPath); Files.write(aPath, aData, aOptions);}
    /** {@link IO#write(String, String, OpenOption...)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void write(Path aPath, String aLine, OpenOption... aOptions) throws IOException {write(aPath, Collections.singletonList(aLine), aOptions);}
    /** {@link IO#write(String, Iterable, OpenOption...)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void write(Path aPath, Iterable<? extends CharSequence> aLines, OpenOption... aOptions) throws IOException {
        validPath(aPath);
        // 使用 UT.IO 中的 stream 统一使用 LF 换行符
        try (IWriteln tWriteln = toWriteln(aPath, aOptions)) {
            for (CharSequence tLine: aLines) {tWriteln.writeln(tLine);}
        }
    }
    /**
     * 向指定路径的文件写入字符串文本，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 不会在最后添加换行符，进而保证和 {@link IO#readAllText(String)} 的结果一致
     *
     * @param aFilePath 需要写入的文件路径
     * @param aText 需要写入的文本
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see Files#writeString(Path, CharSequence, OpenOption...)
     * @see IO#readAllText(String)
     * @see IO#writeText(String, String, OpenOption...)
     */
    public static void writeText(String aFilePath, String aText) throws IOException {writeText(aFilePath, aText, ZL_OO);}
    /**
     * 向指定路径的文件写入字符串文本，默认情况下会覆盖已有文件，
     * 如果文件不存在会创建，如果目录不存在会递归创建，给定的路径有同名的目录时会抛出异常；
     * 触发权限不够时会抛出异常。
     * <p>
     * 不会在最后添加换行符，进而保证和 {@link IO#readAllText(String)} 的结果一致
     *
     * @param aFilePath 需要写入的文件路径
     * @param aText 需要写入的文本
     * @param aOptions 可选的额外写入设置
     * @throws IOException 给定的路径有同名的目录或者权限不够
     * @see StandardOpenOption
     * @see Files#writeString(Path, CharSequence, OpenOption...)
     * @see IO#readAllText(String)
     */
    public static void writeText(String aFilePath, String aText, OpenOption... aOptions) throws IOException {writeText(toAbsolutePath_(aFilePath), aText, aOptions);}
    /** {@link IO#writeText(String, String, OpenOption...)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void writeText(Path aPath, String aText, OpenOption... aOptions) throws IOException {
        validPath(aPath);
        // 现在改为直接使用 BufferedWriter 写入整个 String，保证编码格式为 UTF-8，并且内部也会自动分 buffer 处理字符串
        try (BufferedWriter tWriter = toWriter(aPath, aOptions)) {
            tWriter.write(aText);
        }
    }
    
    /**
     * 读取输入路径对应二进制文件，得到一个字节数组 {@code byte[]}
     * @param aFilePath 需要读取的二进制文件路径
     * @return 读取得到的二进制数据 {@code byte[]}
     * @throws IOException 文件不存在或者权限不够
     * @see Files#readAllBytes(Path)
     * @see IO#write(String, byte[])
     */
    public static byte[] readAllBytes(String aFilePath) throws IOException {return Files.readAllBytes(toAbsolutePath_(aFilePath));}
    /**
     * 读取输入路径对应文件中的所有行，得到由每行字符串组成的列表 {@code List<String>}
     * <p>
     * 会自动忽略文件最末尾的单个换行符（如果存在）
     * <p>
     * 如果希望边操作边读取从而节省内存占用，可以通过 {@link IO#toReader(String)}
     * 来创建一个读取流
     *
     * @param aFilePath 需要读取的文件路径
     * @return 读取得到的所有行组成的列表
     * @throws IOException 文件不存在或者权限不够
     * @see Files#readAllLines(Path)
     * @see IO#write(String, String[])
     * @see IO#readAllLines(BufferedReader)
     */
    public static List<String> readAllLines(String aFilePath) throws IOException {
        try (BufferedReader tReader = toReader(aFilePath)) {
            return readAllLines(tReader);
        }
    }
    /**
     * 读取输入路径对应文件指定的行数目，得到由每行字符串组成的列表 {@code List<String>}
     * <p>
     * 会自动忽略文件最末尾的单个换行符（如果存在）
     * <p>
     * 如果文件剩余行数不够会自动截断，即输出列表长度 {@code <=} aNumber
     *
     * @param aFilePath 需要读取的文件路径
     * @param aNumber 希望读取的行数
     * @return 读取得到的指定行数组成的列表
     * @throws IOException 文件不存在或者权限不够
     * @see IO#readAllLines(String)
     * @see IO#readLines(BufferedReader, int)
     */
    public static List<String> readLines(String aFilePath, int aNumber) throws IOException {
        try (BufferedReader tReader = toReader(aFilePath)) {
            return readLines(tReader, aNumber);
        }
    }
    /**
     * 读取 {@link BufferedReader} 中的所有行，得到由每行字符串组成的列表
     * {@code List<String>}，不会关闭输入的 aReader
     *
     * @param aReader 需要读取的输入流，调用后不会关闭
     * @return 读取得到的所有行组成的列表
     * @see IO#toReader(String)
     * @see IO#readAllLines(String)
     */
    public static List<String> readAllLines(BufferedReader aReader) throws IOException {
        List<String> rLines = new ArrayList<>();
        String tLine;
        while ((tLine = aReader.readLine()) != null) rLines.add(tLine);
        return rLines;
    }
    /**
     * 读取 {@link BufferedReader} 指定的行数目，得到由每行字符串组成的列表
     * {@code List<String>}，不会关闭输入的 aReader
     * <p>
     * 如果文件剩余行数不够会自动截断，即输出列表长度 {@code <=} aNumber
     *
     * @param aReader 需要读取的输入流，调用后不会关闭
     * @param aNumber 希望读取的行数
     * @return 读取得到的指定行数组成的列表
     * @see IO#toReader(String)
     * @see IO#readLines(String, int)
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
     * 读取输入路径对应文件中的所有文本，得到字符串 {@link String}
     * <p>
     * 会完全保证文件文本一致，不会忽略文件最后的换行符（如果存在的话），进而保证和
     * {@link IO#writeText(String, String)} 的结果一致
     *
     * @param aFilePath 需要读取的文件路径
     * @return 读取得到的文本字符串
     * @throws IOException 文件不存在或者权限不够
     * @see Files#readString(Path)
     * @see IO#writeText(String, String)
     */
    public static String readAllText(String aFilePath) throws IOException {
        try (BufferedReader tReader = toReader(aFilePath)) {
            return IOGroovyMethods.getText(tReader);
        }
    }
    
    /** @see IO#removeDir(String) */
    @VisibleForTesting public static void rmdir(String aDir) throws IOException {removeDir(aDir);}
    /**
     * 删除指定文件夹，会递归删除嵌套的文件夹
     * <p>
     * 如果给定目录不存在则不会指定任何操作
     * <p>
     * 效率可能不如系统原生的 {@code rm -rf} 命令
     *
     * @param aDir 需要删除的文件夹路径
     * @throws IOException 触发权限不够时
     * @see IO#delete(String)
     * @see IO#copyDir(String, String)
     * @see IO#move(String, String)
     * @see IO#makeDir(String)
     */
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
    
    /** @see IO#copyDir(String, String) */
    @VisibleForTesting public static void cpdir(String aSourceDir, String aTargetDir) throws IOException {copyDir(aSourceDir, aTargetDir);}
    /**
     * 复制一个文件夹，会递归复制嵌套的子文件夹到目标文件夹；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建
     * <p>
     * 效率可能不如系统原生的 {@code cp -r} 命令
     *
     * @param aSourceDir 需要复制的源文件夹路径
     * @param aTargetDir 需要复制的目标文件夹路径
     * @throws IOException 源文件夹不存在，目标路径有同名的文件夹或者触发权限不够时
     * @see IO#removeDir(String)
     * @see IO#move(String, String)
     * @see IO#makeDir(String)
     */
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
    
    
    /** @see IO#makeDir(String) */
    @VisibleForTesting public static void mkdir(String aDir) throws IOException {makeDir(aDir);}
    /**
     * 创建一个文件夹，会自动递归创建子文件夹
     * @param aDir 需要创建的文件夹路径
     * @throws IOException 已经存在同名的文件，或者触发权限不够时
     */
    public static void makeDir(String aDir) throws IOException {makeDir(toAbsolutePath_(aDir));}
    /** @see IO#isDir(String) */
    @VisibleForTesting public static boolean isdir(String aDir) {return isDir(aDir);}
    /**
     * 判断输入路径是否是一个文件夹
     * @param aDir 需要判断的路径
     * @return 是文件夹则返回 {@code true}，其余任意情况都返回 {@code false}（文件，不存在，或者检测失败）
     * @see Files#isDirectory(Path, LinkOption...)
     * @see IO#isFile(String)
     * @see IO#exists(String)
     */
    public static boolean isDir(String aDir){return Files.isDirectory(toAbsolutePath_(aDir));}
    /** @see IO#isFile(String) */
    @VisibleForTesting public static boolean isfile(String aFilePath) {return isFile(aFilePath);}
    /**
     * 判断输入路径是否是一个文件（而不是文件夹）
     * @param aFilePath 需要判断的路径
     * @return 是文件则返回 {@code true}，其余任意情况都返回 {@code false}（文件夹，不存在，或者检测失败）
     * @see Files#isRegularFile(Path, LinkOption...)
     * @see IO#isDir(String)
     * @see IO#exists(String)
     */
    public static boolean isFile(String aFilePath) {return Files.isRegularFile(toAbsolutePath_(aFilePath));}
    /**
     * 判断输入路径是否存在
     * @param aPath 需要判断的路径
     * @return 是文件或文件夹或链接等则返回 {@code true}，其余任意情况都返回 {@code false}（不存在，或者检测失败）
     * @see Files#exists(Path, LinkOption...)
     * @see IO#isDir(String)
     * @see IO#isFile(String)
     */
    public static boolean exists(String aPath) {return Files.exists(toAbsolutePath_(aPath));}
    /**
     * 删除指定的文件或空文件夹
     * <p>
     * 如果路径不存在则不执行任何操作
     * <p>
     * 如果需要删除有内容的文件夹，则使用 {@link IO#removeDir(String)}
     *
     * @param aPath 需要删除的路径
     * @throws IOException 触发权限不够时
     * @see Files#deleteIfExists(Path)
     * @see IO#removeDir(String)
     */
    public static void delete(String aPath) throws IOException {Files.deleteIfExists(toAbsolutePath_(aPath));}
    /**
     * 复制指定文件到另一个位置；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建
     * <p>
     * 不支持复制有内容的文件夹，如果需要则使用 {@link IO#copyDir(String, String)}
     *
     * @param aSourcePath 源文件路径
     * @param aTargetPath 目标文件路径
     * @throws IOException 源文件夹不存在，目标路径有同名的文件夹或者触发权限不够时
     * @see Files#copy(Path, Path, CopyOption...)
     * @see IO#copyDir(String, String)
     */
    public static void copy(String aSourcePath, String aTargetPath) throws IOException {copy(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath));}
    /**
     * 复制指定的输入流 {@link InputStream} 到目标路径；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建
     *
     * @param aSourceStream 源输入流，调用后不会关闭
     * @param aTargetPath 目标文件路径
     * @throws IOException 源输入流报错，目标路径有同名的文件夹或者触发权限不够时
     * @see Files#copy(InputStream, Path, CopyOption...)
     * @see IO#copy(String, String)
     */
    public static void copy(InputStream aSourceStream, String aTargetPath) throws IOException {copy(aSourceStream, toAbsolutePath_(aTargetPath));}
    /**
     * 复制指定的 {@link URL} 到目标路径；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建
     * <p>
     * 主要用于和 {@link IO#getResource(String)} 配合使用获取
     * jar 包内的资源
     *
     * @param aSourceURL 源 URL
     * @param aTargetPath 目标文件路径
     * @throws IOException 源 URL 报错，目标路径有同名的文件夹或者触发权限不够时
     * @see IO#getResource(String)
     * @see IO#copy(String, String)
     */
    public static void copy(URL aSourceURL, String aTargetPath) throws IOException {copy(aSourceURL, toAbsolutePath_(aTargetPath));}
    /**
     * 移动指定文件或文件夹到另一个位置；
     * 会覆盖已有文件，如果目录不存在会递归创建。
     * <p>
     * 在处理相同硬盘硬盘下的移动时，也支持移动有内容的文件夹，
     * 而需要跨硬盘操作时则需要先通过 {@link IO#copyDir(String, String)}
     * 复制文件夹，然后通过 {@link IO#removeDir(String)} 来删除旧的文件夹；
     * 为了实现两者都支持的情况，可能需要采用下面这种写法：
     * <pre> {@code
     * import jse.code.IO
     *
     * def src = 'path/to/src/dir'
     * def target = 'path/to/target/dir'
     * try {
     *     IO.move(src, target)
     * } catch (any) {
     *     IO.cpdir(src, target)
     *     IO.rmdir(src)
     * }
     * } </pre>
     * @param aSourcePath 源文件或文件夹路径
     * @param aTargetPath 目标文件或文件夹路径
     * @throws IOException 源路径不存在，目标路径有同名的文件夹或者触发权限不够时
     * @see IO#copyDir(String, String)
     * @see IO#removeDir(String)
     */
    public static void move(String aSourcePath, String aTargetPath) throws IOException {move(toAbsolutePath_(aSourcePath), toAbsolutePath_(aTargetPath));}
    /** {@link IO#makeDir(String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void makeDir(Path aDir) throws IOException {Files.createDirectories(aDir);}
    /** {@link IO#copy(String, String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void copy(Path aSourcePath, Path aTargetPath) throws IOException {validPath(aTargetPath); Files.copy(aSourcePath, aTargetPath, REPLACE_EXISTING);}
    /** {@link IO#copy(InputStream, String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void copy(InputStream aSourceStream, Path aTargetPath) throws IOException {validPath(aTargetPath); Files.copy(aSourceStream, aTargetPath, REPLACE_EXISTING);}
    /** {@link IO#copy(URL, String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void copy(URL aSourceURL, Path aTargetPath) throws IOException {try (InputStream tURLStream = aSourceURL.openStream()) {copy(tURLStream, aTargetPath);}}
    /** {@link IO#move(String, String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void move(Path aSourcePath, Path aTargetPath) throws IOException {validPath(aTargetPath); Files.move(aSourcePath, aTargetPath, REPLACE_EXISTING);}
    /**
     * 列出输入文件夹下的所有文件或目录的名称；
     * 注意不同平台下顺序会不一样，为了避免这个问题，可以通过
     * {@code list(dir).sort()} 来将输出的名称数组排序
     * <p>
     * 内部直接使用了 {@link File#list()} 而非 {@link Files#list(Path)}
     * 来直接获取简单的结果；注意这里当文件夹不存在时会直接抛出错误而不是返回 {@code null}。
     * <p>
     * 为了能够直接拼接获取到的文件或问价夹名称，可以通过 {@link IO#toInternalValidDir(String)}
     * 来将输入的文件夹名称合法化。
     *
     * @param aDir 需要列出内容的文件夹路径
     * @return 内部包含的文件或文件夹名称组成的字符串数组 {@code String[]}
     * @throws IOException 文件夹不存在或者触发权限不够时
     * @see File#list()
     */
    public static String @NotNull[] list(String aDir) throws IOException {
        String[] tList = toFile(aDir).list();
        if (tList==null) throw new IOException("Fail to det list of \""+aDir+"\"");
        return tList;
    }
    /**
     * 通过一个每行的变换操作 aOpt 来映射源文件到目标文件，主要用于将给定文本文件特定字符串批量替换；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 由于是边操作边读写，因此处理大文件时不会有过多的内存占用
     * <p>
     * 在 groovy 脚本中也可以直接通过 {@code filterLine} 来实现类似效果：
     * <pre> {@code
     * import jse.code.IO
     *
     * IO.toReader('path/to/src/dir').filterLine(IO.toWriter('path/to/target/dir')) {line -> line.replace('dog', 'cat')}
     * // equivalent to:
     * IO.map('path/to/src/dir', 'path/to/target/dir') {line -> line.replace('dog', 'cat')}
     * } </pre>
     * 即会将源文件中所有的 {@code 'dog'} 替换成 {@code 'cat'}；
     * 注意 groovy 的 {@code filterLine} 会自动在调用后关闭读取流和写入流，而
     * {@link IO#map(BufferedReader, IWriteln, IUnaryFullOperator)} 不会。
     *
     * @param aSourcePath 源文件路径
     * @param aTargetPath 目标文件路径
     * @param aOpt 一行源文件的字符串到一行目标文件字符串的映射
     * @throws IOException 源文件夹不存在，目标路径有同名的文件夹或者触发权限不够时
     * @see IOGroovyMethods#filterLine(Reader, Writer, Closure)
     * @see IO#copy(String, String)
     */
    public static void map(String aSourcePath, String aTargetPath, IUnaryFullOperator<? extends CharSequence, ? super String> aOpt) throws IOException  {try (BufferedReader tReader = toReader(aSourcePath); IWriteln tWriter = toWriteln(aTargetPath)) {map(tReader, tWriter, aOpt);}}
    /**
     * 通过一个每行的变换操作 aOpt 来映射源 {@link URL} 到目标文件，主要用于将给定文本文件特定字符串批量替换；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 由于是边操作边读写，因此处理大文件时不会有过多的内存占用
     * <p>
     * 主要用于和 {@link IO#getResource(String)} 配合使用获取
     * jar 包内的资源
     *
     * @param aSourceURL 源 URL
     * @param aTargetPath 目标文件路径
     * @param aOpt 一行源文件的字符串到一行目标文件字符串的映射
     * @throws IOException 源 URL 报错，目标路径有同名的文件夹或者触发权限不够时
     * @see IO#map(String, String, IUnaryFullOperator)
     * @see IO#copy(URL, String)
     */
    public static void map(URL aSourceURL, String aTargetPath, IUnaryFullOperator<? extends CharSequence, ? super String> aOpt) throws IOException  {try (BufferedReader tReader = toReader(aSourceURL); IWriteln tWriter = toWriteln(aTargetPath)) {map(tReader, tWriter, aOpt);}}
    /**
     * 通过一个每行的变换操作 aOpt 来映射读取流到目标写入流，主要用于将给定文本文件特定字符串批量替换；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 由于是边操作边读写，因此处理大文件时不会有过多的内存占用
     * <p>
     * 在 groovy 脚本中也可以直接通过 {@code filterLine} 来实现类似效果：
     * <pre> {@code
     * import jse.code.IO
     *
     * IO.toReader('path/to/src/dir').filterLine(IO.toWriter('path/to/target/dir')) {line -> line.replace('dog', 'cat')}
     * } </pre>
     * 即会将源文件中所有的 {@code 'dog'} 替换成 {@code 'cat'}；
     * 注意 groovy 的 {@code filterLine} 会自动在调用后关闭读取流和写入流，而此方法不会。
     *
     * @param aReader 源读取流，调用后不会关闭
     * @param aWriter 目标写入流，调用后不会关闭
     * @param aOpt 一行源文件的字符串到一行目标文件字符串的映射
     * @throws IOException 源文件夹不存在，目标路径有同名的文件夹或者触发权限不够时
     * @see IO#map(String, String, IUnaryFullOperator)
     * @see IOGroovyMethods#filterLine(Reader, Writer, Closure)
     */
    public static void map(BufferedReader aReader, IWriteln aWriter, IUnaryFullOperator<? extends CharSequence, ? super String> aOpt) throws IOException {String tLine; while ((tLine = aReader.readLine()) != null) {aWriter.writeln(aOpt.apply(tLine));}}
    
    
    /**
     * 只使用 {@code writeln} 方法写入的一个写入流，主要用来包装
     * {@link BufferedWriter} 实现方便简单按行写入需求的操作。
     */
    @FunctionalInterface public interface IWriteln extends AutoCloseable {
        /**
         * 向流中写入一行，并且换行
         * @param aLine 写入此行的内容
         */
        void writeln(CharSequence aLine) throws IOException;
        /** 关闭此写入流 */
        default void close() throws IOException {/**/}
    }
    
    /// output stuffs
    /**
     * 写入输入路径的文件并转为输出流 {@link OutputStream}；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 主要用于需要 {@link OutputStream} 作为输入的方法，不会进行
     * buffer，如果需要实现边操作边写入建议使用 {@link IO#toWriter(String)}
     * 或 {@link IO#toWriteln(String)}
     *
     * @param aFilePath 需要写入的文件路径
     * @return 创建得到的输出流 {@link OutputStream}
     * @throws IOException 给定的路径有同名的目录或者权限不够时
     * @see Files#newOutputStream(Path, OpenOption...)
     * @see IO#toWriter(String)
     * @see IO#toWriteln(String)
     */
    public static OutputStream toOutputStream(String aFilePath) throws IOException {return toOutputStream(aFilePath, ZL_OO);}
    /**
     * 写入输入路径的文件并转为输出流 {@link OutputStream}；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 主要用于需要 {@link OutputStream} 作为输入的方法，不会进行
     * buffer，如果需要实现边操作边写入建议使用 {@link IO#toWriter(String, OpenOption...)}
     * 或 {@link IO#toWriteln(String, OpenOption...)}
     *
     * @param aFilePath 需要写入的文件路径
     * @param aOptions 可选的额外写入设置
     * @return 创建得到的输出流 {@link OutputStream}
     * @throws IOException 给定的路径有同名的目录或者权限不够时
     * @see StandardOpenOption
     * @see Files#newOutputStream(Path, OpenOption...)
     * @see IO#toWriter(String, OpenOption...)
     * @see IO#toWriteln(String, OpenOption...)
     */
    public static OutputStream toOutputStream(String aFilePath, OpenOption... aOptions) throws IOException {return toOutputStream(toAbsolutePath_(aFilePath), aOptions);}
    /** {@link IO#toOutputStream(String, OpenOption...)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static OutputStream toOutputStream(Path aPath, OpenOption... aOptions) throws IOException {validPath(aPath); return Files.newOutputStream(aPath, aOptions);}
    /**
     * 写入输入路径的文件并转为写入流 {@link BufferedWriter}；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 已经重写了 {@link BufferedWriter#newLine()} 方法，确保写入文件换行符永远都是
     * {@code "\n"}，即 {@code LF} 格式
     * <p>
     * 已经对写入流自动 buffer，主要用于实现高效的边操作边写入行为，也可以使用
     * {@link IO#toWriteln(String)} 来获取更加简洁的写入流接口 {@link IO.IWriteln}
     *
     * @param aFilePath 需要写入的文件路径
     * @return 创建得到的写入流 {@link BufferedWriter}
     * @throws IOException 给定的路径有同名的目录或者权限不够时
     * @see Files#newBufferedWriter(Path, OpenOption...)
     * @see IO#toWriteln(String)
     */
    public static BufferedWriter toWriter(String aFilePath) throws IOException {return toWriter(aFilePath, ZL_OO);}
    /**
     * 写入输入路径的文件并转为写入流 {@link BufferedWriter}；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 已经重写了 {@link BufferedWriter#newLine()} 方法，确保写入文件换行符永远都是
     * {@code "\n"}，即 {@code LF} 格式
     * <p>
     * 已经对写入流自动 buffer，主要用于实现高效的边操作边写入行为，也可以使用
     * {@link IO#toWriteln(String, OpenOption...)} 来获取更加简洁的写入流接口 {@link IO.IWriteln}
     *
     * @param aFilePath 需要写入的文件路径
     * @param aOptions 可选的额外写入设置
     * @return 创建得到的写入流 {@link BufferedWriter}
     * @throws IOException 给定的路径有同名的目录或者权限不够时
     * @see StandardOpenOption
     * @see Files#newBufferedWriter(Path, OpenOption...)
     * @see IO#toWriteln(String, OpenOption...)
     */
    public static BufferedWriter toWriter(String aFilePath, OpenOption... aOptions) throws IOException {return toWriter(toAbsolutePath_(aFilePath), aOptions);}
    /** {@link IO#toWriter(String, OpenOption...)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static BufferedWriter toWriter(Path aPath, OpenOption... aOptions) throws IOException {validPath(aPath); return new BufferedWriter(new OutputStreamWriter(toOutputStream(aPath, aOptions), StandardCharsets.UTF_8)) {@Override public void newLine() throws IOException {write("\n");}};}
    /**
     * 将输出流 {@link OutputStream} 转换为写入流 {@link BufferedWriter}，并增加 buffer。
     * <p>
     * 只需要关闭转换后的写入流，原本的输出流也会同步关闭
     *
     * @param aOutputStream 需要转换的输出流
     * @return 转换后得到的写入流
     */
    public static BufferedWriter toWriter(OutputStream aOutputStream) {return toWriter(aOutputStream, StandardCharsets.UTF_8);}
    /**
     * 将输出流 {@link OutputStream} 转换为写入流 {@link BufferedWriter}，并增加 buffer。
     * <p>
     * 只需要关闭转换后的写入流，原本的输出流也会同步关闭
     *
     * @param aOutputStream 需要转换的输出流
     * @param aCS 需要使用的编码格式，默认使用 UTF-8
     * @return 转换后得到的写入流
     * @see StandardCharsets
     */
    public static BufferedWriter toWriter(OutputStream aOutputStream, Charset aCS) {return new BufferedWriter(new OutputStreamWriter(aOutputStream, aCS)) {@Override public void newLine() throws IOException {write("\n");}};}
    /**
     * 写入输入路径的文件并转为 jse 中使用的写入接口 {@link IO.IWriteln}；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 这里返回的接口为 {@link BufferedWriter} 的包装，并且重写了 {@link BufferedWriter#newLine()}
     * 方法，确保写入文件换行符永远都是 {@code "\n"}，即 {@code LF} 格式
     * <p>
     * 已经对写入流自动 buffer，主要用于实现高效的边操作边写入行为，也可以使用
     * {@link IO#toWriter(String)} 来获取原始的 {@link BufferedWriter}
     * 从而进行更加复杂的写入操作。
     *
     * @param aFilePath 需要写入的文件路径
     * @return 创建得到的写入流 {@link IO.IWriteln}
     * @throws IOException 给定的路径有同名的目录或者权限不够时
     * @see IO#toWriter(String)
     */
    public static IWriteln toWriteln(String aFilePath) throws IOException {return toWriteln(aFilePath, ZL_OO);}
    /**
     * 写入输入路径的文件并转为 jse 中使用的写入流 {@link IO.IWriteln}；
     * 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建。
     * <p>
     * 这里返回的接口为 {@link BufferedWriter} 的包装，并且重写了 {@link BufferedWriter#newLine()}
     * 方法，确保写入文件换行符永远都是 {@code "\n"}，即 {@code LF} 格式
     * <p>
     * 已经对写入流自动 buffer，主要用于实现高效的边操作边写入行为，也可以使用
     * {@link IO#toWriter(String, OpenOption...)} 来获取原始的 {@link BufferedWriter}
     * 从而进行更加复杂的写入操作。
     *
     * @param aFilePath 需要写入的文件路径
     * @param aOptions 可选的额外写入设置
     * @return 创建得到的写入流 {@link IO.IWriteln}
     * @throws IOException 给定的路径有同名的目录或者权限不够时
     * @see StandardOpenOption
     * @see IO#toWriter(String, OpenOption...)
     */
    public static IWriteln toWriteln(String aFilePath, OpenOption... aOptions) throws IOException {return toWriteln(toWriter(aFilePath, aOptions));}
    /** {@link IO#toWriteln(String, OpenOption...)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static IWriteln toWriteln(Path aPath, OpenOption... aOptions) throws IOException {return toWriteln(toWriter(aPath, aOptions));}
    /**
     * 将输出流 {@link OutputStream} 转换为 jse 中使用的写入流 {@link IO.IWriteln}，并增加 buffer。
     * <p>
     * 只需要关闭转换后的写入流，原本的输出流也会同步关闭
     *
     * @param aOutputStream 需要转换的输出流
     * @return 转换后得到的写入流
     */
    public static IWriteln toWriteln(OutputStream aOutputStream) {return toWriteln(toWriter(aOutputStream));}
    /**
     * 将输出流 {@link OutputStream} 转换为 jse 中使用的写入流 {@link IO.IWriteln}，并增加 buffer。
     * <p>
     * 只需要关闭转换后的写入流，原本的输出流也会同步关闭
     *
     * @param aOutputStream 需要转换的输出流
     * @param aCS 需要使用的编码格式，默认使用 UTF-8
     * @return 转换后得到的写入流
     * @see StandardCharsets
     */
    public static IWriteln toWriteln(OutputStream aOutputStream, Charset aCS) {return toWriteln(toWriter(aOutputStream, aCS));}
    /**
     * 将 java 写入流 {@link BufferedWriter} 转换为 jse 中使用的写入流 {@link IO.IWriteln}；
     * 主要用来提供写完一行后自动换行的操作 {@link IO.IWriteln#writeln(CharSequence)}
     * <p>
     * 只需要关闭转换后的写入流，原本的写入流也会同步关闭
     *
     * @param aWriter 需要转换的写入流
     * @return 转换后得到的 jse 写入流
     */
    public static IWriteln toWriteln(BufferedWriter aWriter) {
        return new IWriteln() {
            @Override public void writeln(CharSequence aLine) throws IOException {aWriter.append(aLine); aWriter.newLine();}
            @Override public void close() throws IOException {aWriter.close();}
        };
    }
    
    /// input stuffs
    /**
     * 读取输入路径的文件并转为输入流 {@link InputStream}。
     * <p>
     * 主要用于需要 {@link InputStream} 作为输入的方法，不会进行
     * buffer，如果需要实现边操作边写入建议使用 {@link IO#toReader(String)}
     *
     * @param aFilePath 需要读取的文件路径
     * @return 创建得到的输入流 {@link InputStream}
     * @throws IOException 给定路径文件不存在或者权限不够时
     * @see Files#newInputStream(Path, OpenOption...)
     * @see IO#toReader(String)
     */
    public static InputStream toInputStream(String aFilePath) throws IOException {return toInputStream(toAbsolutePath_(aFilePath));}
    /** {@link IO#toInputStream(String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static InputStream toInputStream(Path aPath) throws IOException {return Files.newInputStream(aPath);}
    /**
     * 读取输入路径的文件并转为读取流 {@link BufferedReader}。
     * <p>
     * 已经对读取流自动 buffer，主要用于实现高效的边操作边读取行为
     *
     * @param aFilePath 需要读取的文件路径
     * @return 创建得到的读取流 {@link BufferedReader}
     * @throws IOException 给定路径文件不存在或者权限不够时
     * @see Files#newBufferedReader(Path)
     */
    public static BufferedReader toReader(String aFilePath) throws IOException {return toReader(toAbsolutePath_(aFilePath));}
    /** {@link IO#toReader(String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static BufferedReader toReader(Path aPath) throws IOException {return toReader(toInputStream(aPath));}
    /**
     * 将指定 {@link URL} 转为读取流 {@link BufferedReader}。
     * <p>
     * 已经对读取流自动 buffer，主要用于实现高效的边操作边读取行为
     * <p>
     * 主要用于和 {@link IO#getResource(String)} 配合使用获取
     * jar 包内的资源
     *
     * @param aFileURL 需要读取的 URL
     * @return 创建得到的读取流 {@link BufferedReader}
     * @throws IOException 给定 URL 文件不存在或者权限不够时
     * @see IO#getResource(String)
     */
    public static BufferedReader toReader(URL aFileURL) throws IOException {return toReader(aFileURL.openStream());}
    /**
     * 将输入流 {@link InputStream} 转换为读取流 {@link BufferedReader}，并增加 buffer。
     * <p>
     * 只需要关闭转换后的读取流，原本的输入流也会同步关闭
     *
     * @param aInputStream 需要转换的输入流
     * @return 转换后得到的读取流
     */
    public static BufferedReader toReader(InputStream aInputStream) throws IOException {return toReader(aInputStream, StandardCharsets.UTF_8, true);}
    /**
     * 将输入流 {@link InputStream} 转换为读取流 {@link BufferedReader}，并增加 buffer。
     * <p>
     * 只需要关闭转换后的读取流，原本的输入流也会同步关闭
     *
     * @param aInputStream 需要转换的输入流
     * @param aCS 需要使用的编码格式，默认使用 UTF-8
     * @return 转换后得到的读取流
     */
    public static BufferedReader toReader(InputStream aInputStream, Charset aCS) throws IOException {return toReader(aInputStream, aCS, false);}
    /**
     * 将输入流 {@link InputStream} 转换为读取流 {@link BufferedReader}，并增加 buffer。
     * <p>
     * 只需要关闭转换后的读取流，原本的输入流也会同步关闭
     *
     * @param aInputStream 需要转换的输入流
     * @param aCS 需要使用的编码格式，默认使用 UTF-8
     * @param aUseUnicodeReader 是否使用 {@link UnicodeReader}，会自动根据 BOM 来检测
     * unicode 类型，在不指定 aCS 时默认为 {@code true}，而在指定 aCS 后默认为 {@code false}
     * @return 转换后得到的读取流
     */
    public static BufferedReader toReader(InputStream aInputStream, Charset aCS, boolean aUseUnicodeReader) throws IOException {
        // 现在改为 UnicodeReader 实现，可以自动检测 UTF 的 BOM
        return new BufferedReader(aUseUnicodeReader ? new UnicodeReader(aInputStream, aCS.name()) : new InputStreamReader(aInputStream, aCS));
    }
    
    /// misc stuffs
    /**
     * 将字符串文件路径转换为 java 的 {@link File}，主要用于内部使用
     * @param aFilePath 需要转换的文件路径
     * @return 得到的对应的 {@link File}
     */
    public static File toFile(String aFilePath) {return toAbsolutePath_(aFilePath).toFile();}
    /**
     * 合法化输入的文件路径，具体操作为创建文件所在的文件夹（包括可能嵌套的文件夹）；
     * 如果输入路径结尾为 {@code "/"} 或 {@code "\"} 则认为输入为文件夹，
     * 则会直接创建这个目录（包括可能嵌套的文件夹），即：
     * <pre> {@code
     * if (aPath.endsWith("/") || aPath.endsWith("\\")) {
     *     makeDir(aPath)
     * } else {
     *     def parent = toParentPath(aPath)
     *     if (parent != null) makeDir(parent)
     * }
     * } </pre>
     * 主要用于确保输出文件路径合法，从而可以调用一些外部程序进行写入而不会报错
     *
     * @param aPath 需要合法化的路径
     * @throws IOException 创建相关文件夹时失败
     */
    public static void validPath(String aPath) throws IOException {
        if (aPath.endsWith("/") || aPath.endsWith("\\")) makeDir(aPath);
        else validPath(toAbsolutePath_(aPath));
    }
    /** {@link IO#validPath(String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static void validPath(Path aPath) throws IOException {
        Path tParent = aPath.getParent();
        if (tParent != null) makeDir(tParent);
    }
    
    /**
     * 解压一个 zip 压缩包到指定目录下；
     * 会覆盖已有文件，如果输出目录不存在会递归创建
     * <p>
     * 类似直接转换一个 zip 文件为指定文件夹，不会创建一层 zip
     * 文件名的文件夹，例如位于 {@code a.zip/x} 的 zip
     * 包内文件，调用 {@code IO.zip2dir('a.zip', 'b')} 后，则会解压到
     * {'b/x'} 位置。
     * <p>
     * 基于 java 自带的 zip 包实现，因此一般会比常用的压缩软件更慢。
     *
     * @param aZipFilePath zip 压缩包文件路径
     * @param aDir 需要解压的输出文件夹
     * @throws IOException zip 文件不存在，输出文件夹有同名的文件或者触发权限不够时
     * @see IO#dir2zip(String, String)
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
     * 压缩输入的文件夹到 zip 压缩文件；
     * 会覆盖已有文件，如果输出目录不存在会递归创建
     * <p>
     * 类似直接转换指定文件夹为 zip 压缩文件，不会在 zip
     * 文件内创建一层输入的文件夹，例如位于 {@code a/x} 的文件夹，调用
     * {@code IO.dir2zip('a', 'b.zip')} 后，则内部文件会压缩到
     * {'b.zip/x'}。
     * <p>
     * 如果希望在 zip 文件内创建一层文件夹，可以使用
     * {@link IO#files2zip(String[], String, int)}
     * <p>
     * 基于 java 自带的 zip 包实现，因此一般会比常用的压缩软件更慢。
     *
     * @param aDir 需要压缩的文件夹
     * @param aZipFilePath 输出的 zip 压缩文件路径
     * @param aCompressLevel 压缩等级（0-9），默认为 {@link Deflater#DEFAULT_COMPRESSION}
     * @throws IOException 输入文件夹不存在，输出目录有同名的文件夹或者触发权限不够时
     * @see IO#zip2dir(String, String)
     * @see IO#dir2zip(String, String)
     * @see IO#files2zip(String[], String, int)
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
    /**
     * 压缩输入的文件夹到 zip 压缩文件；
     * 会覆盖已有文件，如果输出目录不存在会递归创建
     * <p>
     * 类似直接转换指定文件夹为 zip 压缩文件，不会在 zip
     * 文件内创建一层输入的文件夹，例如位于 {@code a/x} 的文件夹，调用
     * {@code IO.dir2zip('a', 'b.zip')} 后，则内部文件会压缩到
     * {'b.zip/x'}。
     * <p>
     * 如果希望在 zip 文件内创建一层文件夹，可以使用
     * {@link IO#files2zip(String[], String)}
     * <p>
     * 基于 java 自带的 zip 包实现，因此一般会比常用的压缩软件更慢。
     *
     * @param aDir 需要压缩的文件夹
     * @param aZipFilePath 输出的 zip 压缩文件路径
     * @throws IOException 输入文件夹不存在，输出目录有同名的文件夹或者触发权限不够时
     * @see IO#zip2dir(String, String)
     * @see IO#dir2zip(String, String, int)
     * @see IO#files2zip(String[], String)
     */
    public static void dir2zip(String aDir, String aZipFilePath) throws IOException {dir2zip(aDir, aZipFilePath, Deflater.DEFAULT_COMPRESSION);}
    
    /**
     * 压缩多个文件（或文件夹）到 zip 压缩文件；
     * 会覆盖已有文件，如果输出目录不存在会递归创建
     * <p>
     * 类似添加指定目录到 zip 压缩文件内部，例如位于 {@code a/x, y}
     * 两个文件，调用 {@code IO.files2zip(['a', 'y'], 'b.zip')}
     * 后，则会压缩为：
     * <pre> {@code
     * └─b.zip
     *   ├─a
     *   │ └─x
     *   └─y
     * } </pre>
     * <p>
     * 基于 java 自带的 zip 包实现，因此一般会比常用的压缩软件更慢。
     *
     * @param aPaths 需要压缩的文件（或文件夹）组成的字符串数组
     * @param aZipFilePath 输出的 zip 压缩文件路径
     * @param aCompressLevel 压缩等级（0-9），默认为 {@link Deflater#DEFAULT_COMPRESSION}
     * @throws IOException 输入文件（或文件夹）不存在，输出目录有同名的文件夹或者触发权限不够时
     * @see IO#zip2dir(String, String)
     * @see IO#dir2zip(String, String, int)
     * @see IO#files2zip(String[], String)
     */
    public static void files2zip(String[] aPaths, String aZipFilePath, int aCompressLevel) throws IOException {files2zip(AbstractCollections.from(aPaths), aZipFilePath, aCompressLevel);}
    /**
     * 压缩多个文件（或文件夹）到 zip 压缩文件；
     * 会覆盖已有文件，如果输出目录不存在会递归创建
     * <p>
     * 类似添加指定目录到 zip 压缩文件内部，例如位于 {@code a/x, y}
     * 两个文件，调用 {@code IO.files2zip(['a', 'y'], 'b.zip')}
     * 后，则会压缩为：
     * <pre> {@code
     * └─b.zip
     *   ├─a
     *   │ └─x
     *   └─y
     * } </pre>
     * <p>
     * 基于 java 自带的 zip 包实现，因此一般会比常用的压缩软件更慢。
     *
     * @param aPaths 需要压缩的文件（或文件夹）组成的字符串数组
     * @param aZipFilePath 输出的 zip 压缩文件路径
     * @throws IOException 输入文件（或文件夹）不存在，输出目录有同名的文件夹或者触发权限不够时
     * @see IO#zip2dir(String, String)
     * @see IO#dir2zip(String, String)
     * @see IO#files2zip(String[], String, int)
     */
    public static void files2zip(String[] aPaths, String aZipFilePath) throws IOException {files2zip(AbstractCollections.from(aPaths), aZipFilePath);}
    /**
     * 传入列表形式文件路径的 {@code files2zip} 实现，用于方便在 groovy 中直接使用
     * @see IO#files2zip(String[], String, int)
     * @see Iterable
     */
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
    /**
     * 传入列表形式文件路径的 {@code files2zip} 实现，用于方便在 groovy 中直接使用
     * @see IO#files2zip(String[], String)
     * @see Iterable
     */
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
     * 将一个 json 文件转换成 {@link Map}，这里直接调用了
     * {@link JsonSlurper#parse(Reader)} 实现
     * @param aFilePath 需要读取并解析的 json 文件路径
     * @return 解析得到的 {@link Map}
     * @throws IOException 输入文件不存在或触发权限不够时
     * @see IO.Text#json2map(String)
     * @see IO#map2json(Map, String)
     */
    public static Map<?, ?> json2map(String aFilePath) throws IOException {
        try (Reader tReader = toReader(aFilePath)) {return json2map(tReader);}
    }
    /**
     * 将一个 json 文件的读取流 {@link Reader} 转换成 {@link Map}，这里直接调用了
     * {@link JsonSlurper#parse(Reader)} 实现；主要用于内部使用
     * @param aReader 需要解析的 json 文件读取流，不会自动关闭
     * @return 解析得到的 {@link Map}
     * @see IO#json2map(String)
     */
    public static Map<?, ?> json2map(Reader aReader) {
        return (Map<?, ?>) (new JsonSlurper()).parse(aReader);
    }
    /**
     * 将一个 {@link Map} 保存成 json 格式的文本文件，这里直接调用了
     * {@link JsonBuilder#writeTo(Writer)}；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 调用 {@link IO#map2json(Map, String, boolean)} 来输出格式化后更加易读的
     * json 文件。
     *
     * @param aMap 需要编码成 json 的 {@link Map}
     * @param aFilePath 需要保存的 json 文件路径
     * @throws IOException 目标路径有同名的文件夹或触发权限不够时
     * @see IO.Text#map2json(Map)
     * @see IO#json2map(String)
     */
    public static void map2json(Map<?, ?> aMap, String aFilePath) throws IOException {
        map2json(aMap, aFilePath, false);
    }
    /**
     * 将一个 {@link Map} 保存成 json 格式的文本文件，这里直接调用了
     * {@link JsonBuilder#writeTo(Writer)}；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * @param aMap 需要编码成 json 的 {@link Map}
     * @param aFilePath 需要保存的 json 文件路径
     * @param aPretty 是否进行格式化来得到较易读的形式，默认为 {@code false}
     * @throws IOException 目标路径有同名的文件夹或触发权限不够时
     * @see IO.Text#map2json(Map, boolean)
     * @see IO#json2map(String)
     */
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
     * 将一个 yaml 文件转换成 {@link Map}，这里直接调用了
     * {@link YamlSlurper#parse(Reader)} 实现
     * @param aFilePath 需要读取并解析的 yaml 文件
     * @return 解析得到的 {@link Map}
     * @throws IOException 输入文件不存在或触发权限不够时
     * @see IO.Text#yaml2map(String)
     * @see IO#map2yaml(Map, String)
     */
    public static Map<?, ?> yaml2map(String aFilePath) throws IOException {
        try (Reader tReader = toReader(aFilePath)) {return yaml2map(tReader);}
    }
    /**
     * 将一个 yaml 文件的读取流 {@link Reader} 转换成 {@link Map}，这里直接调用了
     * {@link YamlSlurper#parse(Reader)} 实现；主要用于内部使用
     * @param aReader 需要解析的 yaml 文件读取流，不会自动关闭
     * @return 解析得到的 {@link Map}
     * @see IO#yaml2map(String)
     */
    public static Map<?, ?> yaml2map(Reader aReader) {
        return (Map<?, ?>) (new YamlSlurper()).parse(aReader);
    }
    /**
     * 将一个 {@link Map} 保存成 yaml 格式的文本文件，这里直接调用了
     * {@link YamlBuilder#writeTo(Writer)}；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * @param aMap 需要编码成 yaml 的 {@link Map}
     * @param aFilePath 需要保存的 yaml 文件路径
     * @throws IOException 目标路径有同名的文件夹或触发权限不够时
     * @see IO.Text#map2yaml(Map)
     * @see IO#yaml2map(String)
     */
    public static void map2yaml(Map<?, ?> aMap, String aFilePath) throws IOException {
        try (Writer tWriter = toWriter(aFilePath)) {
            YamlBuilder tBuilder = new YamlBuilder();
            tBuilder.call(aMap);
            tBuilder.writeTo(tWriter);
        }
    }
    
    
    /**
     * 保存输入的二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按行排列，即：
     * <pre> {@code
     * IO.data2csv([[1, 2], [3, 4]], 'path/to/data.csv')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(double[][], String)}
     * <p>
     * 通过 {@link IO#data2csv(double[][], String, String...)}
     * 来指定每列数据的名称（heads）
     *
     * @param aData 需要保存的二维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(double[][], String)
     * @see IO#cols2csv(double[][], String)
     * @see IO#csv2data(String)
     */
    public static void data2csv(double[][] aData, String aFilePath) throws IOException {data2csv(aData, aFilePath, ZL_STR);}
    /**
     * 保存输入的二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按行排列，第一行为每列的名称，即：
     * <pre> {@code
     * IO.data2csv([[1, 2], [3, 4]], 'path/to/data.csv', 'a', 'b')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * a,b
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(double[][], String, String...)}
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aData 需要保存的二维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(double[][], String, String...)
     * @see IO#cols2csv(double[][], String, String...)
     * @see IO#csv2table(String)
     */
    public static void data2csv(double[][] aData, String aFilePath, String... aHeads) throws IOException {rows2csv(aData, aFilePath, aHeads);}
    /**
     * 保存输入的按行排列的二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按行排列，即：
     * <pre> {@code
     * IO.rows2csv([[1, 2], [3, 4]], 'path/to/data.csv')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(double[][], String)}
     * <p>
     * 通过 {@link IO#rows2csv(double[][], String, String...)}
     * 来指定每列数据的名称（heads）
     *
     * @param aRows 需要保存的按行排列的二维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#cols2csv(double[][], String)
     * @see IO#csv2data(String)
     */
    public static void rows2csv(double[][] aRows, String aFilePath) throws IOException {rows2csv(aRows, aFilePath, ZL_STR);}
    /**
     * 保存输入的按行排列的二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按行排列，第一行为每列的名称，即：
     * <pre> {@code
     * IO.rows2csv([[1, 2], [3, 4]], 'path/to/data.csv', 'a', 'b')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * a,b
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(double[][], String, String...)}
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aRows 需要保存的按行排列的二维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#cols2csv(double[][], String, String...)
     * @see IO#csv2table(String)
     */
    public static void rows2csv(double[][] aRows, String aFilePath, String... aHeads) throws IOException {
        List<String> rLines = AbstractCollections.map(AbstractCollections.from(aRows), row -> String.join(",", AbstractCollections.map(row, Object::toString)));
        if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
        write(aFilePath, rLines);
    }
    /**
     * 保存输入的按列排列的二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按列排列，即：
     * <pre> {@code
     * IO.cols2csv([[1, 2], [3, 4]], 'path/to/data.csv')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * 1,3
     * 2,4
     * } </pre>
     * 如果希望按行保存，可以使用 {@link IO#rows2csv(double[][], String)}
     * <p>
     * 通过 {@link IO#cols2csv(double[][], String, String...)}
     * 来指定每列数据的名称（heads）
     *
     * @param aCols 需要保存的按列排列的二维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(double[][], String)
     * @see IO#csv2data(String)
     */
    public static void cols2csv(double[][] aCols, String aFilePath) throws IOException {cols2csv(aCols, aFilePath, ZL_STR);}
    /**
     * 保存输入的按列排列的二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按列排列，第一行为每列的名称，即：
     * <pre> {@code
     * IO.cols2csv([[1, 2], [3, 4]], 'path/to/data.csv', 'a', 'b')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * a,b
     * 1,3
     * 2,4
     * } </pre>
     * 如果希望按行保存，可以使用 {@link IO#rows2csv(double[][], String, String...)}
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aCols 需要保存的按列排列的二维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(double[][], String, String...)
     * @see IO#csv2table(String)
     */
    public static void cols2csv(double[][] aCols, String aFilePath, String... aHeads) throws IOException {
        List<String> rLines = AbstractCollections.from(aCols[0].length, i ->  String.join(",", AbstractCollections.from(aCols.length, j -> String.valueOf(aCols[j][i]))));
        if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
        write(aFilePath, rLines);
    }
    /**
     * 保存输入的一维数据为单列的 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 会将数据保存为单列的文件，即：
     * <pre> {@code
     * IO.data2csv([1, 2, 3, 4], 'path/to/data.csv')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * 1
     * 2
     * 3
     * 4
     * } </pre>
     * 如果希望保存为单行数据，可以使用 {@link IO#cols2csv(Iterable, String)}
     * 或者直接使用 {@link IO#data2csv(IMatrix, String)}
     * <p>
     * 通过 {@link IO#data2csv(double[], String, String)}
     * 来指定此列数据的名称（head）
     *
     * @param aData 需要保存的一维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#csv2data(String)
     */
    public static void data2csv(double[] aData, String aFilePath) throws IOException {
        write(aFilePath, AbstractCollections.map(aData, Object::toString));
    }
    /**
     * 保存输入的一维数据为单列的 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 会将数据保存为单列的文件，第一行为此列的名称，即：
     * <pre> {@code
     * IO.data2csv([1, 2, 3, 4], 'path/to/data.csv', 'a')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * a
     * 1
     * 2
     * 3
     * 4
     * } </pre>
     * 如果希望保存为单行数据，可以使用 {@link IO#cols2csv(Iterable, String, String...)}
     * 或者直接使用 {@link IO#data2csv(IMatrix, String, String...)}
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aData 需要保存的一维数据
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHead 自定义的此列数据的名称
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#csv2table(String)
     */
    public static void data2csv(double[] aData, String aFilePath, String aHead) throws IOException {
        write(aFilePath, AbstractCollections.merge(aHead, AbstractCollections.map(aData, Object::toString)));
    }
    /**
     * 保存输入的一维或者二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 总是认为数据按行排列，即：
     * <pre> {@code
     * IO.data2csv([[1, 2], [3, 4]], 'path/to/data.csv')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(Iterable, String)}
     * <p>
     * 通过 {@link IO#data2csv(Iterable, String, String...)}
     * 来指定每列数据的名称（heads）
     * <p>
     * 输入数据为向量数据或单个数值组成的列表 {@link Iterable}，
     * 目前支持的类型有 {@link IVector}, {@code double[]},
     * {@link Iterable}, {@code Object[]}, 其余类型会当作单个数值直接转为字符串。
     *
     * @param aData 需要保存的一维或者二维数据，即由向量数据或单个数值组成的列表
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(Iterable, String)
     * @see IO#cols2csv(Iterable, String)
     * @see IO#csv2data(String)
     */
    public static void data2csv(Iterable<?> aData, String aFilePath) throws IOException {data2csv(aData, aFilePath, ZL_STR);}
    /**
     * 保存输入的一维或者二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 总是认为数据按行排列，第一行为每列的名称，即：
     * <pre> {@code
     * IO.data2csv([[1, 2], [3, 4]], 'path/to/data.csv', 'a', 'b')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * a,b
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(Iterable, String, String...)}
     * <p>
     * 输入数据为向量数据或单个数值组成的列表 {@link Iterable}，
     * 目前支持的类型有 {@link IVector}, {@code double[]},
     * {@link Iterable}, {@code Object[]}, 其余类型会当作单个数值直接转为字符串。
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aData 需要保存的一维或者二维数据，即由向量数据或单个数值组成的列表
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(Iterable, String, String...)
     * @see IO#cols2csv(Iterable, String, String...)
     * @see IO#csv2table(String)
     */
    public static void data2csv(Iterable<?> aData, String aFilePath, String... aHeads) throws IOException {rows2csv(aData, aFilePath, aHeads);}
    /**
     * 保存输入的按行排列的一维或者二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按行排列，即：
     * <pre> {@code
     * IO.rows2csv([[1, 2], [3, 4]], 'path/to/data.csv')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(Iterable, String)}
     * <p>
     * 通过 {@link IO#rows2csv(Iterable, String, String...)}
     * 来指定每列数据的名称（heads）
     * <p>
     * 输入数据为向量数据或单个数值组成的列表 {@link Iterable}，
     * 目前支持的类型有 {@link IVector}, {@code double[]},
     * {@link Iterable}, {@code Object[]}, 其余类型会当作单个数值直接转为字符串。
     *
     * @param aRows 需要保存的按行排列的一维或者二维数据，即由向量数据或单个数值组成的列表
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#cols2csv(Iterable, String)
     * @see IO#csv2data(String)
     */
    public static void rows2csv(Iterable<?> aRows, String aFilePath) throws IOException {rows2csv(aRows, aFilePath, ZL_STR);}
    /**
     * 保存输入的按行排列的一维或者二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按行排列，第一行为每列的名称，即：
     * <pre> {@code
     * IO.rows2csv([[1, 2], [3, 4]], 'path/to/data.csv', 'a', 'b')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * a,b
     * 1,2
     * 3,4
     * } </pre>
     * 如果希望按列保存，可以使用 {@link IO#cols2csv(Iterable, String, String...)}
     * <p>
     * 输入数据为向量数据或单个数值组成的列表 {@link Iterable}，
     * 目前支持的类型有 {@link IVector}, {@code double[]},
     * {@link Iterable}, {@code Object[]}, 其余类型会当作单个数值直接转为字符串。
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aRows 需要保存的按行排列的一维或者二维数据，即由向量数据或单个数值组成的列表
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#cols2csv(Iterable, String, String...)
     * @see IO#csv2table(String)
     */
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
    /**
     * 保存输入的按列排列的一维或者二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按列排列，即：
     * <pre> {@code
     * IO.cols2csv([[1, 2], [3, 4]], 'path/to/data.csv')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * 1,3
     * 2,4
     * } </pre>
     * 如果希望按行保存，可以使用 {@link IO#rows2csv(Iterable, String)}
     * <p>
     * 通过 {@link IO#cols2csv(Iterable, String, String...)}
     * 来指定每列数据的名称（heads）
     * <p>
     * 输入数据为向量数据或单个数值组成的列表 {@link Iterable}，
     * 目前支持的类型有 {@link IVector}, {@code double[]},
     * {@link Iterable}, {@code Object[]}, 其余类型会当作单个数值直接转为字符串。
     *
     * @param aCols 需要保存的按列排列的一维或者二维数据，即由向量数据或单个数值组成的列表
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(Iterable, String)
     * @see IO#csv2data(String)
     */
    public static void cols2csv(Iterable<?> aCols, String aFilePath) throws IOException {cols2csv(aCols, aFilePath, ZL_STR);}
    /**
     * 保存输入的按列排列的一维或者二维数据为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 认为数据按列排列，第一行为每列的名称，即：
     * <pre> {@code
     * IO.cols2csv([[1, 2], [3, 4]], 'path/to/data.csv', 'a', 'b')
     * } </pre>
     * 会得到文件：
     * <pre> {@code
     * a,b
     * 1,3
     * 2,4
     * } </pre>
     * 如果希望按行保存，可以使用 {@link IO#rows2csv(Iterable, String, String...)}
     * <p>
     * 输入数据为向量数据或单个数值组成的列表 {@link Iterable}，
     * 目前支持的类型有 {@link IVector}, {@code double[]},
     * {@link Iterable}, {@code Object[]}, 其余类型会当作单个数值直接转为字符串。
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aCols 需要保存的按列排列的一维或者二维数据，即由向量数据或单个数值组成的列表
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IO#rows2csv(Iterable, String, String...)
     * @see IO#csv2table(String)
     */
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
    /**
     * 保存输入的矩阵 {@link IMatrix} 为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 通过 {@link IO#data2csv(IMatrix, String, String...)}
     * 来指定每列数据的名称（heads）
     *
     * @param aData 需要保存的矩阵数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IMatrix
     * @see IO#csv2data(String)
     */
    public static void data2csv(IMatrix aData, String aFilePath) throws IOException {data2csv(aData, aFilePath, ZL_STR);}
    /**
     * 保存输入的矩阵 {@link IMatrix} 为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aData 需要保存的矩阵数据
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IMatrix
     * @see IO#csv2table(String)
     */
    public static void data2csv(IMatrix aData, String aFilePath, String... aHeads) throws IOException {
        List<String> rLines = AbstractCollections.map(aData.rows(), subData -> String.join(",", AbstractCollections.map(subData, Object::toString)));
        if (aHeads!=null && aHeads.length>0) rLines = AbstractCollections.merge(String.join(",", aHeads), rLines);
        write(aFilePath, rLines);
    }
    /**
     * 保存输入的向量 {@link IVector} 为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 会保存成单列 csv 文件，如果希望按行排列可以通过 {@code IO.data2csv(data.asMatRow(), filePath)}
     * 来将向量按行转为单行的矩阵，再调用 {@link IO#data2csv(IMatrix, String)} 保存。
     * <p>
     * 通过 {@link IO#data2csv(IVector, String, String)}
     * 来指定此列数据的名称（head）
     *
     * @param aData 需要保存的向量数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IVector
     * @see IO#csv2data(String)
     */
    public static void data2csv(IVector aData, String aFilePath) throws IOException {
        write(aFilePath, AbstractCollections.map(aData, Object::toString));
    }
    /**
     * 保存输入的向量 {@link IVector} 为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 会保存成单列 csv 文件，如果希望按行排列可以通过 {@code IO.data2csv(data.asMatRow(), filePath)}
     * 来将向量按行转为单行的矩阵，再调用 {@link IO#data2csv(IMatrix, String)} 保存。
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aData 需要保存的向量数据
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHead 自定义的此列数据的名称
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IVector
     * @see IO#csv2table(String)
     */
    public static void data2csv(IVector aData, String aFilePath, String aHead) throws IOException {
        write(aFilePath, AbstractCollections.merge(aHead, AbstractCollections.map(aData, Object::toString)));
    }
    /**
     * 保存输入的一维数值函数 {@link IFunc1} 为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 会保存成带有头的两列 csv 文件，第一列为函数值 {@code "f"}，第二列为自变量值 {@code "x"}；
     * 如果希望调转顺序可以使用 {@code IO.cols2csv([func.x(), func.f()], filePath, 'x', 'f')}
     * 来手动按列保存数据。
     * <p>
     * 通过 {@link IO#data2csv(IFunc1, String, String...)}
     * 来手动指定两列数据的名称（heads）
     *
     * @param aFunc 需要保存的一维数值函数数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IFunc1
     * @see IO#csv2table(String)
     */
    public static void data2csv(IFunc1 aFunc, String aFilePath) throws IOException {data2csv(aFunc, aFilePath, ZL_STR);}
    /**
     * 保存输入的一维数值函数 {@link IFunc1} 为 csv 格式的文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 会保存成带有头的两列 csv 文件，第一列为函数值 {@code aHeads[0]}，第二列为自变量值 {@code aHeads[1]}；
     * 如果希望调转顺序可以使用 {@code IO.cols2csv([func.x(), func.f()], filePath, 'x', 'f')}
     * 来手动按列保存数据。
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aFunc 需要保存的一维数值函数数据
     * @param aFilePath 输出的 csv 文件的路径
     * @param aHeads 自定义的每列数据的名称，需要和列数相同
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see IFunc1
     * @see IO#csv2table(String)
     */
    public static void data2csv(IFunc1 aFunc, String aFilePath, String... aHeads) throws IOException {
        List<String> rLines = AbstractCollections.map(AbstractCollections.range(aFunc.Nx()), i -> aFunc.get(i)+","+aFunc.getX(i));
        rLines = AbstractCollections.merge((aHeads!=null && aHeads.length>0) ? String.join(",", aHeads) : "f,x", rLines);
        write(aFilePath, rLines);
    }
    
    /**
     * 读取输入的 csv 文件路径，存储其中的数据为 jse 矩阵 {@link IMatrix}。
     * <p>
     * 会自动检测第一行是否是可能的头，如果是头则自动忽略此行数据，从而保证和
     * {@link IO#data2csv(IMatrix, String, String...)} 一致；
     * 如果希望保留头的数据可以使用 {@link IO#csv2table(String)} 进行读取。
     * <p>
     * 支持逗号 {@code ","} 或者空格分割的数据，会自动忽略多余的空格，
     * 无法识别的内容会读取为 {@link Double#NaN}
     * <p>
     * 支持忽略井号 {@code "#"} 开头注释掉的行
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#csv2str(String)} 来读取。
     *
     * @param aFilePath 需要读取的 csv 文件路径
     * @return 读取得到的数据，存储为 jse 矩阵
     * @throws IOException 文件不存在或者触发权限不够时
     * @see IO#csv2table(String)
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
     * 保存输入的表格 {@link ITable} 为 csv 格式的文件，使用表格的来作为 csv 文件的第一行头；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#str2csv(Iterable, String)} 来写入。
     *
     * @param aTable 需要保存的表格数据
     * @param aFilePath 输出的 csv 文件的路径
     * @throws IOException 目标路径有同名的文件夹或者触发权限不够时
     * @see ITable
     * @see IO#csv2table(String)
     */
    public static void table2csv(ITable aTable, String aFilePath) throws IOException {
        List<String> rLines = AbstractCollections.map(aTable.rows(), subData -> String.join(",", AbstractCollections.map(subData, Object::toString)));
        rLines = AbstractCollections.merge(String.join(",", aTable.heads()), rLines);
        write(aFilePath, rLines);
    }
    /**
     * 读取输入的 csv 文件路径，存储其中的数据为 jse 表格 {@link ITable}。
     * <p>
     * 会自动检测第一行是否是可能的头，如果是头则读取此头作为 {@link ITable}
     * 的头，否则使用 {@link ITable} 的默认头（{@code "C0", "C1", "C2", ...}）；
     * 如果希望忽略头的信息，可以通过 {@link IO#csv2data(String)} 直接读取得到矩阵。
     * <p>
     * 支持逗号 {@code ","} 或者空格分割的数据，会自动忽略多余的空格，
     * 无法识别的内容会读取为 {@link Double#NaN}
     * <p>
     * 支持忽略井号 {@code "#"} 开头注释掉的行
     * <p>
     * 没有提供完整的 csv 文件读取支持，例如对于头目前并不支持双引号
     * ({@code "}) 包围的字符串的形式，如果需要完整 csv 支持可以使用
     * {@link IO#csv2str(String)} 来读取。
     *
     * @param aFilePath 需要读取的 csv 文件路径
     * @return 读取得到的数据，存储为 jse 表格
     * @throws IOException 文件不存在或者触发权限不够时
     * @see IO#csv2data(String)
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
     * 读取输入的 csv 文件路径，每行分割为字符串数组
     * {@code String[]}，并按行组装成列表 {@link List}
     * <p>
     * 内部采用
     * <a href="https://commons.apache.org/proper/commons-csv/">
     * Apache Commons CSV </a> 实现，因此理论上有完整的 csv 支持。
     *
     * @param aFilePath 需要读取的 csv 文件路径
     * @param aFormat 自定义的 csv 格式 {@link CSVFormat}，默认为 {@link IO#DEFAULT_CSV_FORMAT}
     * @return 拆分每后的行组成的 {@code List<String[]>}
     * @see IO#str2csv(Iterable, String, CSVFormat)
     */
    public static List<String[]> csv2str(String aFilePath, CSVFormat aFormat) throws IOException {
        List<String[]> rLines = new ArrayList<>();
        try (CSVParser tParser = CSVParser.builder().setReader(toReader(aFilePath)).setFormat(aFormat).get()) {
            for (CSVRecord tRecord : tParser) rLines.add(tRecord.values());
        }
        return rLines;
    }
    /**
     * 将输入的按行排列的一维或二维字符串数据写入 csv 文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 输入数据为一维字符串数据或字符串组成的列表 {@link Iterable}，
     * 支持的类型有 {@link Iterable}, {@code Object[]}, 其余类型会当作单个字符串解析。
     * <p>
     * 内部采用
     * <a href="https://commons.apache.org/proper/commons-csv/">
     * Apache Commons CSV </a> 实现，因此理论上有完整的 csv 支持。
     *
     * @param aLines 需要保存的一维或二维字符串数据，按行排列
     * @param aFilePath 输出的 csv 文件的路径
     * @param aFormat 自定义的 csv 格式 {@link CSVFormat}，默认为 {@link IO#DEFAULT_CSV_FORMAT}
     * @see IO#csv2str(String, CSVFormat)
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
    private final static CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.DEFAULT.builder().setRecordSeparator('\n').setCommentMarker('#').setTrim(true).get();
    /**
     * 读取输入的 csv 文件路径，每行分割为字符串数组
     * {@code String[]}，并按行组装成列表 {@link List}
     * <p>
     * 内部采用
     * <a href="https://commons.apache.org/proper/commons-csv/">
     * Apache Commons CSV </a> 实现，因此理论上有完整的 csv 支持。
     * <p>
     * 默认解析认为使用逗号 {@code ","} 分割（不支持空格分割），移除多余空格，忽略井号
     * {@code "#"} 开头注释的行；可以调用 {@link IO#csv2str(String, CSVFormat)}
     * 来实现自定义的解析规则
     *
     * @param aFilePath 需要读取的 csv 文件路径
     * @return 拆分每后的行组成的 {@code List<String[]>}
     * @see IO#str2csv(Iterable, String)
     */
    public static List<String[]> csv2str(String aFilePath) throws IOException {return csv2str(aFilePath, DEFAULT_CSV_FORMAT);}
    /**
     * 将输入的按行排列的一维或二维字符串数据写入 csv 文件；
     * 会覆盖已有文件，如果文件不存在会创建，如果输出目录不存在会递归创建。
     * <p>
     * 输入数据为一维字符串数据或字符串组成的列表 {@link Iterable}，
     * 支持的类型有 {@link Iterable}, {@code Object[]}, 其余类型会当作单个字符串解析。
     * <p>
     * 内部采用
     * <a href="https://commons.apache.org/proper/commons-csv/">
     * Apache Commons CSV </a> 实现，因此理论上有完整的 csv 支持。
     * <p>
     * 默认保存使用逗号 {@code ","} 分割，统一使用 {@code "\n"}
     * 换行；可以调用 {@link IO#str2csv(Iterable, String, CSVFormat)}
     * 来实现自定义的保存规则
     *
     * @param aLines 需要保存的一维或二维字符串数据，按行排列
     * @param aFilePath 输出的 csv 文件的路径
     * @see IO#csv2str(String)
     */
    public static void str2csv(Iterable<?> aLines, String aFilePath) throws IOException {str2csv(aLines, aFilePath, DEFAULT_CSV_FORMAT);}
    
    
    /**
     * 获取指定位置的 jar 包中的资源，返回对应的 {@link URL}，可以通过
     * {@link IO#copy(URL, String)} 输出，或者 {@link IO#toReader(URL)}
     * 转换为读取流；主要用于内部使用。
     * <p>
     * 这里只会获取 {@code assets/} 路径下的资源，并且自动拼接路径，即
     * {@code IO.getResource('a.txt')} 会获取到 jar 包中 {@code assets/a.txt}
     * 路径下的资源。
     *
     * @param aPath 需要查找的资源路径
     * @return 获取到的资源对应的 {@link URL}
     */
    public static URL getResource(String aPath) {
        return IO.class.getClassLoader().getResource("assets/" + aPath);
    }
    
    
    /**
     * 检查输入的两个路径是否指向相同的位置
     * <p>
     * 只进行基于字符串相关的检查，不会实际进入文件系统
     *
     * @param aPath1 字符串表示的路径 1
     * @param aPath2 字符串表示的路径 2
     * @return 是否指向相同位置，{@code true} 表示相同
     */
    public static boolean samePath(String aPath1, String aPath2) {
        return WORKING_DIR_PATH.resolve(aPath1).normalize().equals(WORKING_DIR_PATH.resolve(aPath2).normalize());
    }
    
    /**
     * 获取指定路径的父路径（上级目录）
     * <p>
     * 只进行基于字符串相关的运算，不会实际进入文件系统
     *
     * @param aPath 输入的路径
     * @return 输入路径的父路径，如果不存在父路径则会返回 {@code null}
     */
    public static @Nullable String toParentPath(String aPath) {
        Path tPath = toParentPath_(aPath);
        return tPath==null ? null : tPath.toString();
    }
    /** {@link IO#toParentPath(String)} 返回 {@link Path} 的接口，主要用于内部使用 */
    public static @Nullable Path toParentPath_(String aPath) {return Paths.get(aPath).getParent();}
    
    /**
     * 获取指定路径的文件（或文件夹）名称（最后一项的名称）
     * <p>
     * 只进行基于字符串相关的运算，不会实际进入文件系统
     *
     * @param aPath 输入的路径
     * @return 输入路径的文件名称
     */
    public static String toFileName(String aPath) {return toFileName_(aPath).toString();}
    /** {@link IO#toFileName(String)} 返回 {@link Path} 的接口，主要用于内部使用 */
    public static Path toFileName_(String aPath) {return Paths.get(aPath).getFileName();}
    
    /**
     * 将输入路径转为相对路径，如果不是处于工作目录的相对路径则不会转换；
     * 在跨平台时保持相对路径可以提高一些参数的兼容性。
     * <p>
     * 顺便会将可能存在的 {@code `\`} 转换为 {@code `/`}，以此保证跨平台时的兼容性
     * <p>
     * 只进行基于字符串相关的运算，不会实际进入文件系统
     *
     * @param aPath 输入的路径
     * @return 输入路径的相对工作目录的相对路径
     * @see OS#WORKING_DIR
     */
    public static String toRelativePath(String aPath) {return toRelativePath_(aPath).toString().replace("\\", "/");}
    /** {@link IO#toRelativePath(String)} 返回 {@link Path} 的接口，主要用于内部使用 */
    public static Path toRelativePath_(String aPath) {return WORKING_DIR_PATH.relativize(Paths.get(aPath));}
    
    /**
     * 将输入路径转换为绝对路径，如果已经是绝对路径则不会转换
     * <p>
     * 现在支持将 {@code "~"} 开头的路径替换为用户目录下的绝对路径
     * <p>
     * 只进行基于字符串相关的运算，不会实际进入文件系统
     *
     * @param aPath 输入的路径
     * @return 此路径对应的绝对路径
     * @see OS#WORKING_DIR
     */
    public static String toAbsolutePath(String aPath) {
        if (aPath.startsWith("~")) {
            // 默认不支持 ~
            return USER_HOME + aPath.substring(1); // user.home 这里统一认为 user.home 就是绝对路径
        }
        return WORKING_DIR_PATH.resolve(aPath).toString();
    }
    /** {@link IO#toAbsolutePath(String)} 返回 {@link Path} 的接口，主要用于内部使用 */
    public static Path toAbsolutePath_(String aPath) {
        if (aPath.startsWith("~")) {
            // 默认不支持 ~
            return Paths.get(USER_HOME + aPath.substring(1)); // user.home 这里统一认为 user.home 就是绝对路径
        }
        return WORKING_DIR_PATH.resolve(aPath);
    }
    
    /**
     * 判断输入路径是否是绝对路径
     * <p>
     * 只进行基于字符串相关的运算，不会实际进入文件系统
     *
     * @param aPath 输入的路径
     * @return 此路径是否是绝对路径，{@code true} 表示是绝对路径
     */
    public static boolean isAbsolutePath(String aPath) {return isAbsolutePath(Paths.get(aPath));}
    /** {@link IO#isAbsolutePath(String)} 的 {@link Path} 形式接口，主要用于内部使用 */
    public static boolean isAbsolutePath(Path aPath) {return aPath.isAbsolute();}
}
