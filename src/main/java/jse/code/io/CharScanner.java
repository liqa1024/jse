package jse.code.io;

/**
 * 此类扩展自 {@link org.apache.groovy.json.internal.CharScanner}，
 * 主要用于修复其中不能处理 {@code .123} 这种字符的 bug
 * <p>
 * 现在不再继承 CharScanner 从而保证独立性，因此逻辑上和传统 json
 * 扫描进行区分，并且可以进行更加完整的优化
 *
 * @author liqa
 */
public class CharScanner {
    protected static final int COMMA = ',';
    protected static final int CLOSED_CURLY = '}';
    protected static final int CLOSED_BRACKET = ']';
    protected static final int LETTER_E = 'e';
    protected static final int LETTER_BIG_E = 'E';
    protected static final int DECIMAL_POINT = '.';
    protected static final int ALPHA_0 = '0';
    protected static final int ALPHA_9 = '9';
    protected static final int MINUS = '-';
    protected static final int PLUS = '+';
    
    static final String MIN_LONG_STR_NO_SIGN = String.valueOf(Long.MIN_VALUE);
    static final String MAX_LONG_STR = String.valueOf(Long.MAX_VALUE);
    static final String MIN_INT_STR_NO_SIGN = String.valueOf(Integer.MIN_VALUE);
    static final String MAX_INT_STR = String.valueOf(Integer.MAX_VALUE);
    
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
    
    
    protected static String toStringFromTo(char[] chars, int from, int to) {
        return new String(chars, from, to-from);
    }
    
    public static boolean isDigit(int c) {
        return c >= ALPHA_0 && c <= ALPHA_9;
    }
    public static boolean isNumberDigit(int c) {
        return c >= ALPHA_0 && c <= ALPHA_9;
    }
    protected static boolean isDelimiter(int c) {
        return c == COMMA || c == CLOSED_CURLY || c == CLOSED_BRACKET;
    }
    public static boolean isDecimalDigit(int c) {
        return isDigit(c) || isDecimalChar(c);
    }
    
    public static boolean isDecimalChar(int currentChar) {
        switch (currentChar) {
        case MINUS:
        case PLUS:
        case LETTER_E:
        case LETTER_BIG_E:
        case DECIMAL_POINT:
            return true;
        }
        return false;
    }
    public static boolean hasDecimalChar(char[] chars, boolean negative) {
        int index = 0;
        
        if (negative) index++;
        
        for (; index < chars.length; index++) {
            switch (chars[index]) {
            case MINUS:
            case PLUS:
            case LETTER_E:
            case LETTER_BIG_E:
            case DECIMAL_POINT:
                return true;
            }
        }
        return false;
    }
    
    public static boolean isLong(char[] digitChars) {
        return isLong(digitChars, 0, digitChars.length);
    }
    public static boolean isLong(char[] digitChars, int offset, int len) {
        String cmpStr = digitChars[offset] == '-' ? MIN_LONG_STR_NO_SIGN : MAX_LONG_STR;
        int cmpLen = cmpStr.length();
        if (len < cmpLen) return true;
        if (len > cmpLen) return false;
        
        for (int i = 0; i < cmpLen; ++i) {
            int diff = digitChars[offset + i] - cmpStr.charAt(i);
            if (diff != 0) {
                return (diff < 0);
            }
        }
        return true;
    }
    
    public static boolean isInteger(char[] digitChars) {
        return isInteger(digitChars, 0, digitChars.length);
    }
    public static boolean isInteger(char[] digitChars, int offset, int len) {
        String cmpStr = (digitChars[offset] == '-') ? MIN_INT_STR_NO_SIGN : MAX_INT_STR;
        int cmpLen = cmpStr.length();
        if (len < cmpLen) return true;
        if (len > cmpLen) return false;
        
        for (int i = 0; i < cmpLen; ++i) {
            int diff = digitChars[offset + i] - cmpStr.charAt(i);
            if (diff != 0) {
                return (diff < 0);
            }
        }
        return true;
    }
    
    public static int parseInt(boolean ignoreErr, boolean[] anyErr, char[] digitChars) {
        return parseIntFromTo(ignoreErr, anyErr, digitChars, 0, digitChars.length);
    }
    
    public static int parseIntFromTo(boolean ignoreErr, boolean[] anyErr, char[] digitChars, int from, int to) {
        int num;
        boolean negative = false;
        int offset = from;
        char c = digitChars[offset];
        if (c == '-') {
            offset++;
            negative = true;
        }
        if (offset >= to) {
            if (ignoreErr) {anyErr[0] = true; return 0;}
            throw new NumberFormatException("For input chars: '"+toStringFromTo(digitChars, from, to)+"'");
        }
        num = (digitChars[offset] - '0');
        if (++offset < to) {
            num = (num * 10) + (digitChars[offset] - '0');
            if (++offset < to) {
                num = (num * 10) + (digitChars[offset] - '0');
                if (++offset < to) {
                    num = (num * 10) + (digitChars[offset] - '0');
                    if (++offset < to) {
                        num = (num * 10) + (digitChars[offset] - '0');
                        if (++offset < to) {
                            num = (num * 10) + (digitChars[offset] - '0');
                            if (++offset < to) {
                                num = (num * 10) + (digitChars[offset] - '0');
                                if (++offset < to) {
                                    num = (num * 10) + (digitChars[offset] - '0');
                                    if (++offset < to) {
                                        num = (num * 10) + (digitChars[offset] - '0');
                                        if (++offset < to) {
                                            num = (num * 10) + (digitChars[offset] - '0');
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return negative ? num * -1 : num;
    }
    
    public static int parseIntFromToIgnoreDot(boolean ignoreErr, boolean[] anyErr, char[] digitChars, int from, int to) {
        int num = 0;
        boolean negative = false;
        int offset = from;
        char c = digitChars[offset];
        if (c == '-') {
            offset++;
            negative = true;
        }
        if (offset >= to) {
            if (ignoreErr) {anyErr[0] = true; return 0;}
            throw new NumberFormatException("For input chars: '"+toStringFromTo(digitChars, from, to)+"'");
        }
        c = digitChars[offset];
        if (c == '.') {
            offset++;
        }
        if (offset >= to) {
            if (ignoreErr) {anyErr[0] = true; return 0;}
            throw new NumberFormatException("For input chars: '"+toStringFromTo(digitChars, from, to)+"'");
        }
        
        for (; offset < to; offset++) {
            c = digitChars[offset];
            if (c != '.') {
                num = (num * 10) + (c - '0');
            }
        }
        
        return negative ? num * -1 : num;
    }
    
    public static long parseLongFromToIgnoreDot(boolean ignoreErr, boolean[] anyErr, char[] digitChars, int from, int to) {
        long num = 0;
        boolean negative = false;
        int offset = from;
        char c = digitChars[offset];
        if (c == '-') {
            offset++;
            negative = true;
        }
        if (offset >= to) {
            if (ignoreErr) {anyErr[0] = true; return 0;}
            throw new NumberFormatException("For input chars: '"+toStringFromTo(digitChars, from, to)+"'");
        }
        c = digitChars[offset];
        if (c == '.') {
            offset++;
        }
        if (offset >= to) {
            if (ignoreErr) {anyErr[0] = true; return 0;}
            throw new NumberFormatException("For input chars: '"+toStringFromTo(digitChars, from, to)+"'");
        }
        
        for (; offset < to; offset++) {
            c = digitChars[offset];
            if (c != '.') {
                num = (num * 10) + (c - '0');
            }
        }
        
        return negative ? num * -1 : num;
    }
    
    public static long parseLongFromTo(boolean ignoreErr, boolean[] anyErr, char[] digitChars, int from, int to) {
        long num;
        boolean negative = false;
        int offset = from;
        char c = digitChars[offset];
        if (c == '-') {
            offset++;
            negative = true;
        }
        if (offset >= to) {
            if (ignoreErr) {anyErr[0] = true; return 0;}
            throw new NumberFormatException("For input chars: '"+toStringFromTo(digitChars, from, to)+"'");
        }
        c = digitChars[offset];
        num = (c - '0');
        offset++;
        
        long digit;
        
        for (; offset < to; offset++) {
            c = digitChars[offset];
            digit = (c - '0');
            num = (num * 10) + digit;
        }
        
        return negative ? num * -1 : num;
    }
    
    public static long parseLong(boolean ignoreErr, boolean[] anyErr, char[] digitChars) {
        return parseLongFromTo(ignoreErr, anyErr, digitChars, 0, digitChars.length);
    }
    
    public static float parseFloat(boolean ignoreErr, boolean[] anyErr, char[] digitChars) {
        return parseFloatFromTo(ignoreErr, anyErr, digitChars, 0, digitChars.length);
    }
    public static float parseFloatFromTo(boolean ignoreErr, boolean[] anyErr, char[] buffer, int from, int to) {
        return (float) parseDoubleFromTo(ignoreErr, anyErr, buffer, from, to);
    }
    
    public static double parseDouble(boolean ignoreErr, boolean[] anyErr, char[] digitChars) {
        return parseDoubleFromTo(ignoreErr, anyErr, digitChars, 0, digitChars.length);
    }
    public static double parseDoubleFromTo(boolean ignoreErr, boolean[] anyErr, char[] buffer, int from, int to) {
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
                    if (ignoreErr) {anyErr[0] = true; return Double.NaN;}
                    throw new NumberFormatException("Unexpected character '"+ch+"' for input chars: '"+toStringFromTo(buffer, from, to)+"'");
                }
                foundDot = true;
            } else if (ch == 'E' || ch == 'e' || ch == '-' || ch == '+') {
                simple = false;
            } else {
                if (ignoreErr) {anyErr[0] = true; return Double.NaN;}
                throw new NumberFormatException("Unexpected character '"+ch+"' for input chars: '"+toStringFromTo(buffer, from, to)+"'");
            }
        }
        
        if (digitsPastPoint >= powersOf10.length - 1) {
            simple = false;
        }
        
        final int length = index - from;
        
        if (!foundDot && simple) {
            if (isInteger(buffer, from, length)) {
                value = parseIntFromTo(ignoreErr, anyErr, buffer, from, index);
            } else {
                value = parseLongFromTo(ignoreErr, anyErr, buffer, from, index);
            }
            if (ignoreErr && anyErr[0]) return Double.NaN;
        } else if (foundDot && simple) {
            long lvalue;
            
            if (length < powersOf10.length) {
                if (isInteger(buffer, from, length)) {
                    lvalue = parseIntFromToIgnoreDot(ignoreErr, anyErr, buffer, from, index);
                } else {
                    lvalue = parseLongFromToIgnoreDot(ignoreErr, anyErr, buffer, from, index);
                }
                if (ignoreErr && anyErr[0]) return Double.NaN;
                
                double power = powersOf10[digitsPastPoint];
                value = lvalue / power;
            } else {
                if (ignoreErr) {
                    try {value = Double.parseDouble(new String(buffer, from, length));}
                    catch (Exception any) {anyErr[0] = true; return Double.NaN;}
                } else {
                    value = Double.parseDouble(new String(buffer, from, length));
                }
            }
        } else {
            if (ignoreErr) {
                try {value = Double.parseDouble(new String(buffer, from, index - from));}
                catch (Exception any) {anyErr[0] = true; return Double.NaN;}
            } else {
                value = Double.parseDouble(new String(buffer, from, index - from));
            }
        }
        
        return value;
    }
    
    public static Number parseNumber(boolean ignoreErr, boolean[] anyErr, char[] digitChars) {
        return parseNumberFromTo(ignoreErr, anyErr, digitChars, 0, digitChars.length);
    }
    public static Number parseNumberFromTo(boolean ignoreErr, boolean[] anyErr, char[] buffer, int from, int to) {
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
                    if (ignoreErr) {anyErr[0] = true; return null;}
                    throw new NumberFormatException("Unexpected character '"+ch+"' for input chars: '"+toStringFromTo(buffer, from, to)+"'");
                }
                foundDot = true;
            } else if (ch == 'E' || ch == 'e' || ch == '-' || ch == '+') {
                simple = false;
            } else {
                if (ignoreErr) {anyErr[0] = true; return null;}
                throw new NumberFormatException("Unexpected character '"+ch+"' for input chars: '"+toStringFromTo(buffer, from, to)+"'");
            }
        }
        
        if (digitsPastPoint >= powersOf10.length - 1) {
            simple = false;
        }
        
        final int length = index - from;
        
        if (!foundDot && simple) {
            if (isInteger(buffer, from, length)) {
                value = parseIntFromTo(ignoreErr, anyErr, buffer, from, index);
            } else {
                value = parseLongFromTo(ignoreErr, anyErr, buffer, from, index);
            }
            if (ignoreErr && anyErr[0]) return null;
        } else if (foundDot && simple) {
            long lvalue;
            
            if (length < powersOf10.length) {
                if (isInteger(buffer, from, length)) {
                    lvalue = parseIntFromToIgnoreDot(ignoreErr, anyErr, buffer, from, index);
                } else {
                    lvalue = parseLongFromToIgnoreDot(ignoreErr, anyErr, buffer, from, index);
                }
                if (ignoreErr && anyErr[0]) return null;
                
                double power = powersOf10[digitsPastPoint];
                value = lvalue / power;
            } else {
                if (ignoreErr) {
                    try {value = Double.parseDouble(new String(buffer, from, length));}
                    catch (Exception any) {anyErr[0] = true; return null;}
                } else {
                    value = Double.parseDouble(new String(buffer, from, length));
                }
            }
        } else {
            if (ignoreErr) {
                try {value = Double.parseDouble(new String(buffer, from, index - from));}
                catch (Exception any) {anyErr[0] = true; return null;}
            } else {
                value = Double.parseDouble(new String(buffer, from, index - from));
            }
        }
        
        return value;
    }
    
    public static int skipWhiteSpace(char[] array, int index, final int length) {
        int c;
        for (; index < length; index++) {
            c = array[index];
            if (c > 32) {
                return index;
            }
        }
        return index;
    }
}
