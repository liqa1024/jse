package jse.code.io;

import static org.apache.groovy.json.internal.Exceptions.die;

/**
 * 此类扩展自 {@link org.apache.groovy.json.internal.CharScanner}，
 * 主要用于修复其中不能处理 {@code .123} 这种字符的 bug；
 * 这里直接继承使用旧的 CharScanner，而不是直接复写，减少重复的代码量，从而保留版本更新后的兼容性
 * @author liqa
 */
public class CharScanner extends org.apache.groovy.json.internal.CharScanner {
    
    private static final double[] powersOf10 = {
        1.0,
        10.0,
        100.0,
        1000.0,
        10000.0,
        100000.0,
        1000000.0,
        10000000.0,
        100000000.0,
        1000000000.0,
        10000000000.0,
        100000000000.0,
        1000000000000.0,
        10000000000000.0,
        100000000000000.0,
        1000000000000000.0,
        10000000000000000.0,
        100000000000000000.0,
        1000000000000000000.0,
    };
    
    public static int parseIntFromToIgnoreDot(char[] digitChars, int offset, int to) {
        int num = 0;
        boolean negative = false;
        char c = digitChars[offset];
        if (c == '-') {
            offset++;
            negative = true;
        }
        if (offset >= to) {
            die();
        }
        
        for (; offset < to; offset++) {
            c = digitChars[offset];
            if (c != '.') {
                num = (num * 10) + (c - '0');
            }
        }
        
        return negative ? num * -1 : num;
    }
    
    public static long parseLongFromToIgnoreDot(char[] digitChars, int offset, int to) {
        long num = 0;
        boolean negative = false;
        char c = digitChars[offset];
        if (c == '-') {
            offset++;
            negative = true;
        }
        if (offset >= to) {
            die();
        }
        
        for (; offset < to; offset++) {
            c = digitChars[offset];
            if (c != '.') {
                num = (num * 10) + (c - '0');
            }
        }
        
        return negative ? num * -1 : num;
    }
    
    public static float parseFloat(char[] buffer, int from, int to) {
        return (float) parseDouble(buffer, from, to);
    }
    
    public static double parseDouble(char[] buffer, int from, int to) {
        double value;
        boolean simple = true;
        int digitsPastPoint = 0;
        
        int index = from;
        
        if (buffer[index] == '-') {
            index++;
        }
        
        boolean foundDot = false;
        for (; index < to; index++) {
            char ch = buffer[index];
            if (isNumberDigit(ch)) {
                if (foundDot) {
                    digitsPastPoint++;
                }
            } else if (ch == '.') {
                if (foundDot) {
                    die("unexpected character " + ch);
                }
                foundDot = true;
            } else if (ch == 'E' || ch == 'e' || ch == '-' || ch == '+') {
                simple = false;
            } else {
                die("unexpected character " + ch);
            }
        }
        
        if (digitsPastPoint >= powersOf10.length - 1) {
            simple = false;
        }
        
        final int length = index - from;
        
        if (!foundDot && simple) {
            if (isInteger(buffer, from, length)) {
                value = parseIntFromTo(buffer, from, index);
            } else {
                value = parseLongFromTo(buffer, from, index);
            }
        } else if (foundDot && simple) {
            long lvalue;
            
            if (length < powersOf10.length) {
                if (isInteger(buffer, from, length)) {
                    lvalue = parseIntFromToIgnoreDot(buffer, from, index);
                } else {
                    lvalue = parseLongFromToIgnoreDot(buffer, from, index);
                }
                
                double power = powersOf10[digitsPastPoint];
                value = lvalue / power;
            } else {
                value = Double.parseDouble(new String(buffer, from, length));
            }
        } else {
            value = Double.parseDouble(new String(buffer, from, index - from));
        }
        
        return value;
    }
    
    public static Number parseNumber(char[] buffer, int from, int to) {
        Number value;
        boolean simple = true;
        int digitsPastPoint = 0;
        
        int index = from;
        
        if (buffer[index] == '-') {
            index++;
        }
        
        boolean foundDot = false;
        for (; index < to; index++) {
            char ch = buffer[index];
            if (isNumberDigit(ch)) {
                if (foundDot) {
                    digitsPastPoint++;
                }
            } else if (ch == '.') {
                if (foundDot) {
                    die("unexpected character " + ch);
                }
                foundDot = true;
            } else if (ch == 'E' || ch == 'e' || ch == '-' || ch == '+') {
                simple = false;
            } else {
                die("unexpected character " + ch);
            }
        }
        
        if (digitsPastPoint >= powersOf10.length - 1) {
            simple = false;
        }
        
        final int length = index - from;
        
        if (!foundDot && simple) {
            if (isInteger(buffer, from, length)) {
                value = parseIntFromTo(buffer, from, index);
            } else {
                value = parseLongFromTo(buffer, from, index);
            }
        } else if (foundDot && simple) {
            long lvalue;
            
            if (length < powersOf10.length) {
                if (isInteger(buffer, from, length)) {
                    lvalue = parseIntFromToIgnoreDot(buffer, from, index);
                } else {
                    lvalue = parseLongFromToIgnoreDot(buffer, from, index);
                }
                
                double power = powersOf10[digitsPastPoint];
                value = lvalue / power;
            } else {
                value = Double.parseDouble(new String(buffer, from, length));
            }
        } else {
            value = Double.parseDouble(new String(buffer, from, index - from));
        }
        
        return value;
    }
}
