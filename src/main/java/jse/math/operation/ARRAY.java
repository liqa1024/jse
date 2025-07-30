package jse.math.operation;

import com.mastfrog.util.sort.Sort;
import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import jse.clib.JNIUtil;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.functional.*;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.*;

import static jse.code.CS.VERSION;
import static jse.code.Conf.NATIVE_OPERATION;
import static jse.code.OS.JAR_DIR;
import static jse.code.OS.JAVA_HOME;
import static jse.code.UT.Code.toComplexDouble;


/**
 * 对于内部含有 double[] 的数据的运算做专门优化，方便编译器做 SIMD 的相关优化
 * @author liqa
 */
public class ARRAY {
    private ARRAY() {}
    
    /** logical stuffs */
    public static void ebeAnd2Dest(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] && aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] && aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] && aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] && aDataR[k];
        }
    }
    public static void ebeOr2Dest(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] || aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] || aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] || aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] || aDataR[k];
        }
    }
    public static void ebeXor2Dest(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] ^ aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] ^ aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] ^ aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] ^ aDataR[k];
        }
    }
    
    
    public static void mapAnd2Dest(boolean[] aDataL, int aShiftL, boolean aRHS, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] && aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] && aRHS;
    }
    public static void mapOr2Dest(boolean[] aDataL, int aShiftL, boolean aRHS, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] || aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] || aRHS;
    }
    public static void mapXor2Dest(boolean[] aDataL, int aShiftL, boolean aRHS, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] ^ aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] ^ aRHS;
    }
    
    
    public static void ebeAnd2This(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] &= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] &= aDataR[j];
    }
    public static void ebeOr2This(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] |= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] |= aDataR[j];
    }
    public static void ebeXor2This(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] ^= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] ^= aDataR[j];
    }
    
    
    public static void mapAnd2This  (boolean[] rThis, int rShift, boolean aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] &= aRHS;}
    public static void mapOr2This   (boolean[] rThis, int rShift, boolean aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] |= aRHS;}
    public static void mapXor2This  (boolean[] rThis, int rShift, boolean aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] ^= aRHS;}
    
    
    public static void not2Dest(boolean[] aData, int aShift, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShift) for (int i = rShift; i < rEnd; ++i) rDest[i] = !aData[i];
        else for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) rDest[i] = !aData[j];
    }
    public static void not2This(boolean[] rThis, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = !rThis[i];
    }
    
    public static boolean allOfThis(boolean[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        for (int i = aShift; i < tEnd; ++i) {
            if (!aThis[i]) return false;
        }
        return true;
    }
    public static boolean anyOfThis(boolean[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        for (int i = aShift; i < tEnd; ++i) {
            if (aThis[i]) return true;
        }
        return false;
    }
    public static int countOfThis(boolean[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        int rCount = 0;
        for (int i = aShift; i < tEnd; ++i) {
            if (aThis[i]) ++rCount;
        }
        return rCount;
    }
    
    
    
    /** add, minus, multiply, divide stuffs */
    public static void ebePlus2Dest(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] + aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] + aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] + aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] + aDataR[k];
        }
    }
    public static void ebeMinus2Dest(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] - aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] - aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] - aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] - aDataR[k];
        }
    }
    public static void ebeMultiply2Dest(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] * aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] * aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] * aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] * aDataR[k];
        }
    }
    public static void ebeDiv2Dest(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] / aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] / aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] / aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] / aDataR[k];
        }
    }
    public static void ebeMod2Dest(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] % aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] % aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] % aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] % aDataR[k];
        }
    }
    
    
    public static void mapPlus2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] + aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] + aRHS;
    }
    public static void mapMinus2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] - aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] - aRHS;
    }
    public static void mapLMinus2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS - aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS - aDataL[j];
    }
    public static void mapMultiply2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] * aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] * aRHS;
    }
    public static void mapDiv2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] / aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] / aRHS;
    }
    public static void mapLDiv2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS / aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS / aDataL[j];
    }
    public static void mapMod2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] % aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] % aRHS;
    }
    public static void mapLMod2Dest(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS % aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS % aDataL[j];
    }
    
    
    public static void ebePlus2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] += aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] += aDataR[j];
    }
    public static void ebeMinus2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] -= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] -= aDataR[j];
    }
    public static void ebeLMinus2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] - rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] - rThis[i];
    }
    public static void ebeMultiply2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] *= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] *= aDataR[j];
    }
    public static void ebeDiv2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] /= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] /= aDataR[j];
    }
    public static void ebeLDiv2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] / rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] / rThis[i];
    }
    public static void ebeMod2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] %= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] %= aDataR[j];
    }
    public static void ebeLMod2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] % rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] % rThis[i];
    }
    
    
    public static void mapPlus2This     (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] += aRHS          ;}
    public static void mapMinus2This    (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] -= aRHS          ;}
    public static void mapLMinus2This   (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS - rThis[i];}
    public static void mapMultiply2This (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] *= aRHS          ;}
    public static void mapDiv2This      (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] /= aRHS          ;}
    public static void mapLDiv2This     (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS / rThis[i];}
    public static void mapMod2This      (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] %= aRHS          ;}
    public static void mapLMod2This     (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS % rThis[i];}
    
    
    /** int stuffs */
    public static void ebePlus2Dest(int[] aDataL, int aShiftL, int[] aDataR, int aShiftR, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] + aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] + aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] + aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] + aDataR[k];
        }
    }
    public static void ebeMinus2Dest(int[] aDataL, int aShiftL, int[] aDataR, int aShiftR, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] - aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] - aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] - aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] - aDataR[k];
        }
    }
    public static void ebeMultiply2Dest(int[] aDataL, int aShiftL, int[] aDataR, int aShiftR, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] * aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] * aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] * aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] * aDataR[k];
        }
    }
    public static void ebeDiv2Dest(int[] aDataL, int aShiftL, int[] aDataR, int aShiftR, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] / aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] / aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] / aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] / aDataR[k];
        }
    }
    public static void ebeMod2Dest(int[] aDataL, int aShiftL, int[] aDataR, int aShiftR, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] % aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] % aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] % aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] % aDataR[k];
        }
    }
    
    
    public static void mapPlus2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] + aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] + aRHS;
    }
    public static void mapMinus2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] - aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] - aRHS;
    }
    public static void mapLMinus2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS - aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS - aDataL[j];
    }
    public static void mapMultiply2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] * aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] * aRHS;
    }
    public static void mapDiv2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] / aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] / aRHS;
    }
    public static void mapLDiv2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS / aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS / aDataL[j];
    }
    public static void mapMod2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] % aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] % aRHS;
    }
    public static void mapLMod2Dest(int[] aDataL, int aShiftL, int aRHS, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS % aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS % aDataL[j];
    }
    
    
    public static void ebePlus2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] += aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] += aDataR[j];
    }
    public static void ebeMinus2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] -= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] -= aDataR[j];
    }
    public static void ebeLMinus2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] - rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] - rThis[i];
    }
    public static void ebeMultiply2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] *= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] *= aDataR[j];
    }
    public static void ebeDiv2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] /= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] /= aDataR[j];
    }
    public static void ebeLDiv2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] / rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] / rThis[i];
    }
    public static void ebeMod2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] %= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] %= aDataR[j];
    }
    public static void ebeLMod2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] % rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] % rThis[i];
    }
    
    
    public static void mapPlus2This     (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] += aRHS          ;}
    public static void mapMinus2This    (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] -= aRHS          ;}
    public static void mapLMinus2This   (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS - rThis[i];}
    public static void mapMultiply2This (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] *= aRHS          ;}
    public static void mapDiv2This      (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] /= aRHS          ;}
    public static void mapLDiv2This     (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS / rThis[i];}
    public static void mapMod2This      (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] %= aRHS          ;}
    public static void mapLMod2This     (int[] rThis, int rShift, int aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS % rThis[i];}
    
    
    /** do stuff */
    public static void ebeDo2Dest(double[][] aDataL, int aShiftL, double[][] aDataR, int aShiftR, double[][] rDest, int rShift, int aLength, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) {
                for (int i = rShift; i < rEnd; ++i) {
                    IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealDataL[i], tImagDataL[i]), new ComplexDouble(tRealDataR[i], tImagDataR[i]));
                    rRealDest[i] = tValue.real(); rImagDest[i] = tValue.imag();
                }
            } else {
                for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) {
                    IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealDataL[i], tImagDataL[i]), new ComplexDouble(tRealDataR[k], tImagDataR[k]));
                    rRealDest[i] = tValue.real(); rImagDest[i] = tValue.imag();
                }
            }
        } else {
            if (rShift == aShiftR) {
                for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                    IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealDataL[j], tImagDataL[j]), new ComplexDouble(tRealDataR[i], tImagDataR[i]));
                    rRealDest[i] = tValue.real(); rImagDest[i] = tValue.imag();
                }
            } else {
                for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) {
                    IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealDataL[j], tImagDataL[j]), new ComplexDouble(tRealDataR[k], tImagDataR[k]));
                    rRealDest[i] = tValue.real(); rImagDest[i] = tValue.imag();
                }
            }
        }
    }
    public static void mapDo2Dest(double[][] aDataL, int aShiftL, double[][] rDest, int rShift, int aLength, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            for (int i = rShift; i < rEnd; ++i) {
                IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealDataL[i], tImagDataL[i]));
                rRealDest[i] = tValue.real(); rImagDest[i] = tValue.imag();
            }
        } else {
            for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealDataL[j], tImagDataL[j]));
                rRealDest[i] = tValue.real(); rImagDest[i] = tValue.imag();
            }
        }
    }
    public static void ebeDo2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] rRealThis  = rThis [0], rImagThis  = rThis [1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) {
            for (int i = rShift; i < rEnd; ++i) {
                IComplexDouble tValue = aOpt.apply(new ComplexDouble(rRealThis[i], rImagThis[i]), new ComplexDouble(tRealDataR[i], tImagDataR[i]));
                rRealThis[i] = tValue.real(); rImagThis[i] = tValue.imag();
            }
        } else {
            for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) {
                IComplexDouble tValue = aOpt.apply(new ComplexDouble(rRealThis[i], rImagThis[i]), new ComplexDouble(tRealDataR[j], tImagDataR[j]));
                rRealThis[i] = tValue.real(); rImagThis[i] = tValue.imag();
            }
        }
    }
    public static void mapDo2This(double[][] rThis, int rShift, int aLength, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] rRealThis = rThis[0], rImagThis = rThis[1];
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) {
            IComplexDouble tValue = aOpt.apply(new ComplexDouble(rRealThis[i], rImagThis[i]));
            rRealThis[i] = tValue.real(); rImagThis[i] = tValue.imag();
        }
    }
    
    public static void ebeDo2Dest(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength, DoubleBinaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.applyAsDouble(aDataL[i], aDataR[i]);
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aOpt.applyAsDouble(aDataL[i], aDataR[k]);
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.applyAsDouble(aDataL[j], aDataR[i]);
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aOpt.applyAsDouble(aDataL[j], aDataR[k]);
        }
    }
    public static void mapDo2Dest(double[] aDataL, int aShiftL, double[] rDest, int rShift, int aLength, DoubleUnaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.applyAsDouble(aDataL[i]);
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.applyAsDouble(aDataL[j]);
    }
    public static void ebeDo2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength, DoubleBinaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.applyAsDouble(rThis[i], aDataR[i]);
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aOpt.applyAsDouble(rThis[i], aDataR[j]);
    }
    public static void mapDo2This(double[] rThis, int rShift, int aLength, DoubleUnaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.applyAsDouble(rThis[i]);
    }
    
    public static void ebeDo2Dest(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength, IBooleanBinaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.applyAsBoolean(aDataL[i], aDataR[i]);
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aOpt.applyAsBoolean(aDataL[i], aDataR[k]);
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.applyAsBoolean(aDataL[j], aDataR[i]);
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aOpt.applyAsBoolean(aDataL[j], aDataR[k]);
        }
    }
    public static void mapDo2Dest(boolean[] aDataL, int aShiftL, boolean[] rDest, int rShift, int aLength, IBooleanUnaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.applyAsBoolean(aDataL[i]);
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.applyAsBoolean(aDataL[j]);
    }
    public static void ebeDo2This(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength, IBooleanBinaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.applyAsBoolean(rThis[i], aDataR[i]);
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aOpt.applyAsBoolean(rThis[i], aDataR[j]);
    }
    public static void mapDo2This(boolean[] rThis, int rShift, int aLength, IBooleanUnaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.applyAsBoolean(rThis[i]);
    }
    
    public static void ebeDo2Dest(int[] aDataL, int aShiftL, int[] aDataR, int aShiftR, int[] rDest, int rShift, int aLength, IntBinaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.applyAsInt(aDataL[i], aDataR[i]);
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aOpt.applyAsInt(aDataL[i], aDataR[k]);
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.applyAsInt(aDataL[j], aDataR[i]);
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aOpt.applyAsInt(aDataL[j], aDataR[k]);
        }
    }
    public static void mapDo2Dest(int[] aDataL, int aShiftL, int[] rDest, int rShift, int aLength, IntUnaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.applyAsInt(aDataL[i]);
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.applyAsInt(aDataL[j]);
    }
    public static void ebeDo2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength, IntBinaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.applyAsInt(rThis[i], aDataR[i]);
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aOpt.applyAsInt(rThis[i], aDataR[j]);
    }
    public static void mapDo2This(int[] rThis, int rShift, int aLength, IntUnaryOperator aOpt) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.applyAsInt(rThis[i]);
    }
    
    
    /** abs stuffs */
    public static void mapAbs2Dest(double[] aData, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShift) for (int i = rShift; i < rEnd; ++i) rDest[i] = Math.abs(aData[i]);
        else for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) rDest[i] = Math.abs(aData[j]);
    }
    public static void mapAbs2This(double[] rThis, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = Math.abs(rThis[i]);
    }
    public static void mapAbs2Dest(int[] aData, int aShift, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShift) for (int i = rShift; i < rEnd; ++i) rDest[i] = Math.abs(aData[i]);
        else for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) rDest[i] = Math.abs(aData[j]);
    }
    public static void mapAbs2This(int[] rThis, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = Math.abs(rThis[i]);
    }
    /** negative stuffs */
    public static void mapNegative2Dest(double[] aData, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShift) for (int i = rShift; i < rEnd; ++i) rDest[i] = -aData[i];
        else for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) rDest[i] = -aData[j];
    }
    public static void mapNegative2This(double[] rThis, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = -rThis[i];
    }
    public static void mapNegative2Dest(double[][] aData, int aShift, double[][] rDest, int rShift, int aLength) {
        mapNegative2Dest(aData[0], aShift, rDest[0], rShift, aLength);
        mapNegative2Dest(aData[1], aShift, rDest[1], rShift, aLength);
    }
    public static void mapNegative2This(double[][] rThis, int rShift, int aLength) {
        mapNegative2This(rThis[0], rShift, aLength);
        mapNegative2This(rThis[1], rShift, aLength);
    }
    public static void mapNegative2Dest(int[] aData, int aShift, int[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShift) for (int i = rShift; i < rEnd; ++i) rDest[i] = -aData[i];
        else for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) rDest[i] = -aData[j];
    }
    public static void mapNegative2This(int[] rThis, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = -rThis[i];
    }
    
    
    /** complex double stuffs */
    public static void ebePlus2Dest(double[][] aDataL, int aShiftL, double[][] aDataR, int aShiftR, double[][] rDest, int rShift, int aLength) {
        ebePlus2Dest(aDataL[0], aShiftL, aDataR[0], aShiftR, rDest[0], rShift, aLength);
        ebePlus2Dest(aDataL[1], aShiftL, aDataR[1], aShiftR, rDest[1], rShift, aLength);
    }
    public static void ebeMinus2Dest(double[][] aDataL, int aShiftL, double[][] aDataR, int aShiftR, double[][] rDest, int rShift, int aLength) {
        ebeMinus2Dest(aDataL[0], aShiftL, aDataR[0], aShiftR, rDest[0], rShift, aLength);
        ebeMinus2Dest(aDataL[1], aShiftL, aDataR[1], aShiftR, rDest[1], rShift, aLength);
    }
    public static void ebeMultiply2Dest(double[][] aDataL, int aShiftL, double[][] aDataR, int aShiftR, double[][] rDest, int rShift, int aLength) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) {
                for (int i = rShift; i < rEnd; ++i) {
                    final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                    final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                    rRealDest[i] = lReal*rReal - lImag*rImag;
                    rImagDest[i] = lImag*rReal + lReal*rImag;
                }
            } else {
                for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) {
                    final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                    final double rReal = tRealDataR[k], rImag = tImagDataR[k];
                    rRealDest[i] = lReal*rReal - lImag*rImag;
                    rImagDest[i] = lImag*rReal + lReal*rImag;
                }
            }
        } else {
            if (rShift == aShiftR) {
                for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                    final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                    final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                    rRealDest[i] = lReal*rReal - lImag*rImag;
                    rImagDest[i] = lImag*rReal + lReal*rImag;
                }
            } else {
                for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) {
                    final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                    final double rReal = tRealDataR[k], rImag = tImagDataR[k];
                    rRealDest[i] = lReal*rReal - lImag*rImag;
                    rImagDest[i] = lImag*rReal + lReal*rImag;
                }
            }
        }
    }
    public static void ebeDiv2Dest(double[][] aDataL, int aShiftL, double[][] aDataR, int aShiftR, double[][] rDest, int rShift, int aLength) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) {
                for (int i = rShift; i < rEnd; ++i) {
                    final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                    final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                    final double div = rReal*rReal + rImag*rImag;
                    rRealDest[i] = (lReal*rReal + lImag*rImag)/div;
                    rImagDest[i] = (lImag*rReal - lReal*rImag)/div;
                }
            } else {
                for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) {
                    final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                    final double rReal = tRealDataR[k], rImag = tImagDataR[k];
                    final double div = rReal*rReal + rImag*rImag;
                    rRealDest[i] = (lReal*rReal + lImag*rImag)/div;
                    rImagDest[i] = (lImag*rReal - lReal*rImag)/div;
                }
            }
        } else {
            if (rShift == aShiftR) {
                for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                    final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                    final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                    final double div = rReal*rReal + rImag*rImag;
                    rRealDest[i] = (lReal*rReal + lImag*rImag)/div;
                    rImagDest[i] = (lImag*rReal - lReal*rImag)/div;
                }
            } else {
                for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) {
                    final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                    final double rReal = tRealDataR[k], rImag = tImagDataR[k];
                    final double div = rReal*rReal + rImag*rImag;
                    rRealDest[i] = (lReal*rReal + lImag*rImag)/div;
                    rImagDest[i] = (lImag*rReal - lReal*rImag)/div;
                }
            }
        }
    }
    
    
    public static void mapPlus2Dest(double[][] aDataL, int aShiftL, IComplexDouble aRHS, double[][] rDest, int rShift, int aLength) {
        mapPlus2Dest(aDataL[0], aShiftL, aRHS.real(), rDest[0], rShift, aLength);
        mapPlus2Dest(aDataL[1], aShiftL, aRHS.imag(), rDest[1], rShift, aLength);
    }
    public static void mapMinus2Dest(double[][] aDataL, int aShiftL, IComplexDouble aRHS, double[][] rDest, int rShift, int aLength) {
        mapMinus2Dest(aDataL[0], aShiftL, aRHS.real(), rDest[0], rShift, aLength);
        mapMinus2Dest(aDataL[1], aShiftL, aRHS.imag(), rDest[1], rShift, aLength);
    }
    public static void mapLMinus2Dest(double[][] aDataL, int aShiftL, IComplexDouble aRHS, double[][] rDest, int rShift, int aLength) {
        mapLMinus2Dest(aDataL[0], aShiftL, aRHS.real(), rDest[0], rShift, aLength);
        mapLMinus2Dest(aDataL[1], aShiftL, aRHS.imag(), rDest[1], rShift, aLength);
    }
    public static void mapMultiply2Dest(double[][] aDataL, int aShiftL, IComplexDouble aRHS, double[][] rDest, int rShift, int aLength) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double   rReal =    aRHS.real(), rImag    = aRHS.imag();
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            for (int i = rShift; i < rEnd; ++i) {
                final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                rRealDest[i] = lReal*rReal - lImag*rImag;
                rImagDest[i] = lImag*rReal + lReal*rImag;
            }
        } else {
            for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                rRealDest[i] = lReal*rReal - lImag*rImag;
                rImagDest[i] = lImag*rReal + lReal*rImag;
            }
        }
    }
    public static void mapDiv2Dest(double[][] aDataL, int aShiftL, IComplexDouble aRHS, double[][] rDest, int rShift, int aLength) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double   rReal =    aRHS.real(), rImag    = aRHS.imag();
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final double div = rReal*rReal + rImag*rImag;
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            for (int i = rShift; i < rEnd; ++i) {
                final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                rRealDest[i] = (lReal*rReal + lImag*rImag)/div;
                rImagDest[i] = (lImag*rReal - lReal*rImag)/div;
            }
        } else {
            for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                rRealDest[i] = (lReal*rReal + lImag*rImag)/div;
                rImagDest[i] = (lImag*rReal - lReal*rImag)/div;
            }
        }
    }
    public static void mapLDiv2Dest(double[][] aDataL, int aShiftL, IComplexDouble aRHS, double[][] rDest, int rShift, int aLength) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double   rReal =    aRHS.real(), rImag    = aRHS.imag();
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            for (int i = rShift; i < rEnd; ++i) {
                final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                final double div = lReal*lReal + lImag*lImag;
                rRealDest[i] = (rReal*lReal + rImag*lImag)/div;
                rImagDest[i] = (rImag*lReal - rReal*lImag)/div;
            }
        } else {
            for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                final double div = lReal*lReal + lImag*lImag;
                rRealDest[i] = (rReal*lReal + rImag*lImag)/div;
                rImagDest[i] = (rImag*lReal - rReal*lImag)/div;
            }
        }
    }
    public static void mapPlus2Dest(double[][] aDataL, int aShiftL, double aRHS, double[][] rDest, int rShift, int aLength) {
        mapPlus2Dest(aDataL[0], aShiftL, aRHS, rDest[0], rShift, aLength);
        ebeFill2This(rDest[1], rShift, aDataL[1], aShiftL, aLength);
    }
    public static void mapMinus2Dest(double[][] aDataL, int aShiftL, double aRHS, double[][] rDest, int rShift, int aLength) {
        mapMinus2Dest(aDataL[0], aShiftL, aRHS, rDest[0], rShift, aLength);
        ebeFill2This(rDest[1], rShift, aDataL[1], aShiftL, aLength);
    }
    public static void mapLMinus2Dest(double[][] aDataL, int aShiftL, double aRHS, double[][] rDest, int rShift, int aLength) {
        mapLMinus2Dest(aDataL[0], aShiftL, aRHS, rDest[0], rShift, aLength);
        mapNegative2Dest(aDataL[1], aShiftL, rDest[1], rShift, aLength);
    }
    public static void mapMultiply2Dest(double[][] aDataL, int aShiftL, double aRHS, double[][] rDest, int rShift, int aLength) {
        mapMultiply2Dest(aDataL[0], aShiftL, aRHS, rDest[0], rShift, aLength);
        mapMultiply2Dest(aDataL[1], aShiftL, aRHS, rDest[1], rShift, aLength);
    }
    public static void mapDiv2Dest(double[][] aDataL, int aShiftL, double aRHS, double[][] rDest, int rShift, int aLength) {
        mapDiv2Dest(aDataL[0], aShiftL, aRHS, rDest[0], rShift, aLength);
        mapDiv2Dest(aDataL[1], aShiftL, aRHS, rDest[1], rShift, aLength);
    }
    public static void mapLDiv2Dest(double[][] aDataL, int aShiftL, double aRHS, double[][] rDest, int rShift, int aLength) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double[] rRealDest  = rDest [0], rImagDest  = rDest [1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            for (int i = rShift; i < rEnd; ++i) {
                final double lReal = tRealDataL[i], lImag = tImagDataL[i];
                final double div = lReal*lReal + lImag*lImag;
                rRealDest[i] = (aRHS*lReal)/div;
                rImagDest[i] = (-aRHS*lImag)/div;
            }
        } else {
            for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) {
                final double lReal = tRealDataL[j], lImag = tImagDataL[j];
                final double div = lReal*lReal + lImag*lImag;
                rRealDest[i] = (aRHS*lReal)/div;
                rImagDest[i] = (-aRHS*lImag)/div;
            }
        }
    }
    
    
    public static void ebePlus2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength) {
        ebePlus2This(rThis[0], rShift, aDataR[0], aShiftR, aLength);
        ebePlus2This(rThis[1], rShift, aDataR[1], aShiftR, aLength);
    }
    public static void ebeMinus2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength) {
        ebeMinus2This(rThis[0], rShift, aDataR[0], aShiftR, aLength);
        ebeMinus2This(rThis[1], rShift, aDataR[1], aShiftR, aLength);
    }
    public static void ebeLMinus2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength) {
        ebeLMinus2This(rThis[0], rShift, aDataR[0], aShiftR, aLength);
        ebeLMinus2This(rThis[1], rShift, aDataR[1], aShiftR, aLength);
    }
    public static void ebeMultiply2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength) {
        final double[] rRealThis  = rThis [0], rImagThis  = rThis [1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) {
            for (int i = rShift; i < rEnd; ++i) {
                final double lReal = rRealThis [i], lImag = rImagThis [i];
                final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                rRealThis[i] = lReal*rReal - lImag*rImag;
                rImagThis[i] = lImag*rReal + lReal*rImag;
            }
        } else {
            for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) {
                final double lReal = rRealThis [i], lImag = rImagThis [i];
                final double rReal = tRealDataR[j], rImag = tImagDataR[j];
                rRealThis[i] = lReal*rReal - lImag*rImag;
                rImagThis[i] = lImag*rReal + lReal*rImag;
            }
        }
    }
    public static void ebeDiv2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength) {
        final double[] rRealThis  = rThis [0], rImagThis  = rThis [1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) {
            for (int i = rShift; i < rEnd; ++i) {
                final double lReal = rRealThis [i], lImag = rImagThis [i];
                final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                final double div = rReal*rReal + rImag*rImag;
                rRealThis[i] = (lReal*rReal + lImag*rImag)/div;
                rImagThis[i] = (lImag*rReal - lReal*rImag)/div;
            }
        } else {
            for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) {
                final double lReal = rRealThis [i], lImag = rImagThis [i];
                final double rReal = tRealDataR[j], rImag = tImagDataR[j];
                final double div = rReal*rReal + rImag*rImag;
                rRealThis[i] = (lReal*rReal + lImag*rImag)/div;
                rImagThis[i] = (lImag*rReal - lReal*rImag)/div;
            }
        }
    }
    public static void ebeLDiv2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength) {
        final double[] rRealThis  = rThis [0], rImagThis  = rThis [1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) {
            for (int i = rShift; i < rEnd; ++i) {
                final double lReal = rRealThis [i], lImag = rImagThis [i];
                final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                final double div = lReal*lReal + lImag*lImag;
                rRealThis[i] = (rReal*lReal + rImag*lImag)/div;
                rImagThis[i] = (rImag*lReal - rReal*lImag)/div;
            }
        } else {
            for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) {
                final double lReal = rRealThis [i], lImag = rImagThis [i];
                final double rReal = tRealDataR[j], rImag = tImagDataR[j];
                final double div = lReal*lReal + lImag*lImag;
                rRealThis[i] = (rReal*lReal + rImag*lImag)/div;
                rImagThis[i] = (rImag*lReal - rReal*lImag)/div;
            }
        }
    }
    
    
    public static void mapPlus2This     (double[][] rThis, int rShift, IComplexDouble aRHS, int aLength) {mapPlus2This    (rThis[0], rShift, aRHS.real(), aLength); mapPlus2This    (rThis[1], rShift, aRHS.imag(), aLength);}
    public static void mapMinus2This    (double[][] rThis, int rShift, IComplexDouble aRHS, int aLength) {mapMinus2This   (rThis[0], rShift, aRHS.real(), aLength); mapMinus2This   (rThis[1], rShift, aRHS.imag(), aLength);}
    public static void mapLMinus2This   (double[][] rThis, int rShift, IComplexDouble aRHS, int aLength) {mapLMinus2This  (rThis[0], rShift, aRHS.real(), aLength); mapLMinus2This  (rThis[1], rShift, aRHS.imag(), aLength);}
    public static void mapMultiply2This (double[][] rThis, int rShift, IComplexDouble aRHS, int aLength) {
        final double[] rRealThis = rThis[0], rImagThis = rThis[1];
        final double   rReal  = aRHS.real(), rImag  = aRHS.imag();
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) {
            final double lReal = rRealThis[i], lImag = rImagThis[i];
            rRealThis[i] = lReal*rReal - lImag*rImag;
            rImagThis[i] = lImag*rReal + lReal*rImag;
        }
    }
    public static void mapDiv2This      (double[][] rThis, int rShift, IComplexDouble aRHS, int aLength) {
        final double[] rRealThis = rThis[0], rImagThis = rThis[1];
        final double   rReal  = aRHS.real(), rImag  = aRHS.imag();
        final double div = rReal*rReal + rImag*rImag;
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) {
            final double lReal = rRealThis[i], lImag = rImagThis[i];
            rRealThis[i] = (lReal*rReal + lImag*rImag)/div;
            rImagThis[i] = (lImag*rReal - lReal*rImag)/div;
        }
    }
    public static void mapLDiv2This     (double[][] rThis, int rShift, IComplexDouble aRHS, int aLength) {
        final double[] rRealThis = rThis[0], rImagThis = rThis[1];
        final double   rReal  = aRHS.real(), rImag  = aRHS.imag();
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) {
            final double lReal = rRealThis[i], lImag = rImagThis[i];
            final double div = lReal*lReal + lImag*lImag;
            rRealThis[i] = (rReal*lReal + rImag*lImag)/div;
            rImagThis[i] = (rImag*lReal - rReal*lImag)/div;
        }
    }
    public static void mapPlus2This     (double[][] rThis, int rShift, double aRHS, int aLength) {mapPlus2This    (rThis[0], rShift, aRHS, aLength);}
    public static void mapMinus2This    (double[][] rThis, int rShift, double aRHS, int aLength) {mapMinus2This   (rThis[0], rShift, aRHS, aLength);}
    public static void mapLMinus2This   (double[][] rThis, int rShift, double aRHS, int aLength) {mapLMinus2This  (rThis[0], rShift, aRHS, aLength); mapNegative2This(rThis[1], rShift,       aLength);}
    public static void mapMultiply2This (double[][] rThis, int rShift, double aRHS, int aLength) {mapMultiply2This(rThis[0], rShift, aRHS, aLength); mapMultiply2This(rThis[1], rShift, aRHS, aLength);}
    public static void mapDiv2This      (double[][] rThis, int rShift, double aRHS, int aLength) {mapDiv2This     (rThis[0], rShift, aRHS, aLength); mapDiv2This     (rThis[1], rShift, aRHS, aLength);}
    public static void mapLDiv2This     (double[][] rThis, int rShift, double aRHS, int aLength) {
        final double[] rRealThis = rThis[0], rImagThis = rThis[1];
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) {
            final double lReal = rRealThis[i], lImag = rImagThis[i];
            final double div = lReal*lReal + lImag*lImag;
            rRealThis[i] = (aRHS*lReal)/div;
            rImagThis[i] = (-aRHS*lImag)/div;
        }
    }
    
    
    
    /** fill, forEach, assign stuff */
    public static void mapFill2This(double[][] rThis, int rShift, IComplexDouble aRHS, int aLength) {
        mapFill2This(rThis[0], rShift, aRHS.real(), aLength);
        mapFill2This(rThis[1], rShift, aRHS.imag(), aLength);
    }
    public static void mapFill2This(double[][] rThis, int rShift, double aRHS, int aLength) {
        mapFill2This(rThis[0], rShift, aRHS, aLength);
        mapFill2This(rThis[1], rShift, 0.0, aLength);
    }
    public static void ebeFill2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, int aLength) {
        ebeFill2This(rThis[0], rShift, aDataR[0], aShiftR, aLength);
        ebeFill2This(rThis[1], rShift, aDataR[1], aShiftR, aLength);
    }
    public static void vecFill2This(double[][] rThis, int rShift, int aLength, IComplexVectorGetter aVec) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (rShift == 0) {
            for (int i = 0; i < aLength; ++i) {
                IComplexDouble tValue = aVec.get(i);
                tRealData[i] = tValue.real();
                tImagData[i] = tValue.imag();
            }
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift, j = 0; i < tEnd; ++i, ++j) {
                IComplexDouble tValue = aVec.get(j);
                tRealData[i] = tValue.real();
                tImagData[i] = tValue.imag();
            }
        }
    }
    public static void vecFill2This(double[][] rThis, int rShift, int aLength, IVectorGetter aVec) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (rShift == 0) {
            for (int i = 0; i < aLength; ++i) {
                tRealData[i] = aVec.get(i);
                tImagData[i] = 0.0;
            }
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift, j = 0; i < tEnd; ++i, ++j) {
                tRealData[i] = aVec.get(j);
                tImagData[i] = 0.0;
            }
        }
    }
    public static void assign2This(double[][] rThis, int rShift, int aLength, Supplier<? extends IComplexDouble> aSup) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        final int tEnd = aLength + rShift;
        for (int i = rShift; i < tEnd; ++i) {
            IComplexDouble tValue = aSup.get();
            tRealData[i] = tValue.real();
            tImagData[i] = tValue.imag();
        }
    }
    public static void assign2This(double[][] rThis, int rShift, int aLength, DoubleSupplier aSup) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        final int tEnd = aLength + rShift;
        for (int i = rShift; i < tEnd; ++i) {
            tRealData[i] = aSup.getAsDouble();
            tImagData[i] = 0.0;
        }
    }
    public static void forEachOfThis(double[][] aThis, int aShift, int aLength, Consumer<? super ComplexDouble> aCon) {
        final double[] tRealData = aThis[0];
        final double[] tImagData = aThis[1];
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; ++i) {
            aCon.accept(new ComplexDouble(tRealData[i], tImagData[i]));
        }
    }
    public static void forEachOfThis(double[][] aThis, int aShift, int aLength, IDoubleBinaryConsumer aCon) {
        final double[] tRealData = aThis[0];
        final double[] tImagData = aThis[1];
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; ++i) {
            aCon.accept(tRealData[i], tImagData[i]);
        }
    }
    /** Groovy stuffs */
    public static void vecFill2This(double[][] rThis, int rShift, int aLength, @ClosureParams(value=SimpleType.class, options="int") Closure<?> aGroovyTask) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (rShift == 0) {
            for (int i = 0; i < aLength; ++i) {
                // 直接先执行然后检测类型决定如何设置
                Object tObj = aGroovyTask.call(i);
                if (tObj instanceof IComplexDouble) {
                    IComplexDouble tValue = (IComplexDouble)tObj;
                    tRealData[i] = tValue.real();
                    tImagData[i] = tValue.imag();
                } else
                if (tObj instanceof Number) {
                    tRealData[i] = ((Number)tObj).doubleValue();
                    tImagData[i] = 0.0;
                } else {
                    tRealData[i] = Double.NaN;
                    tImagData[i] = 0.0;
                }
            }
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift, j = 0; i < tEnd; ++i, ++j) {
                // 直接先执行然后检测类型决定如何设置
                Object tObj = aGroovyTask.call(j);
                if (tObj instanceof IComplexDouble) {
                    IComplexDouble tValue = (IComplexDouble)tObj;
                    tRealData[i] = tValue.real();
                    tImagData[i] = tValue.imag();
                } else
                if (tObj instanceof Number) {
                    tRealData[i] = ((Number)tObj).doubleValue();
                    tImagData[i] = 0.0;
                } else {
                    tRealData[i] = Double.NaN;
                    tImagData[i] = 0.0;
                }
            }
        }
    }
    public static void assign2This(double[][] rThis, int rShift, int aLength, Closure<?> aGroovyTask) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        final int tEnd = aLength + rShift;
        for (int i = rShift; i < tEnd; ++i) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = aGroovyTask.call();
            if (tObj instanceof IComplexDouble) {
                IComplexDouble tValue = (IComplexDouble)tObj;
                tRealData[i] = tValue.real();
                tImagData[i] = tValue.imag();
            } else
            if (tObj instanceof Number) {
                tRealData[i] = ((Number)tObj).doubleValue();
                tImagData[i] = 0.0;
            } else {
                tRealData[i] = Double.NaN;
                tImagData[i] = 0.0;
            }
        }
    }
    
    public static void mapFill2This(double[] rThis, int rShift, double aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(double[] rThis, int rShift, int aLength, IVectorGetter aVec) {
        final int tEnd = aLength + rShift;
        if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
        else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
    }
    public static void assign2This(double[] rThis, int rShift, int aLength, DoubleSupplier aSup) {
        final int tEnd = aLength + rShift;
        for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsDouble();
    }
    public static void forEachOfThis(double[] aThis, int aShift, int aLength, DoubleConsumer aCon) {
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
    }
    
    public static void mapFill2This(boolean[] rThis, int rShift, boolean aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(boolean[] rThis, int rShift, int aLength, ILogicalVectorGetter aVec) {
        final int tEnd = aLength + rShift;
        if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
        else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
    }
    public static void assign2This(boolean[] rThis, int rShift, int aLength, BooleanSupplier aSup) {
        final int tEnd = aLength + rShift;
        for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsBoolean();
    }
    public static void forEachOfThis(boolean[] aThis, int aShift, int aLength, IBooleanConsumer aCon) {
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
    }
    
    public static void mapFill2This(int[] rThis, int rShift, int aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(int[] rThis, int rShift, int aLength, IIntVectorGetter aVec) {
        final int tEnd = aLength + rShift;
        if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
        else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
    }
    public static void assign2This(int[] rThis, int rShift, int aLength, IntSupplier aSup) {
        final int tEnd = aLength + rShift;
        for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsInt();
    }
    public static void forEachOfThis(int[] aThis, int aShift, int aLength, IntConsumer aCon) {
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
    }
    
    public static void mapFill2This(long[] rThis, int rShift, long aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(long[] rThis, int rShift, long[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(long[] rThis, int rShift, int aLength, ILongVectorGetter aVec) {
        final int tEnd = aLength + rShift;
        if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
        else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
    }
    public static void assign2This(long[] rThis, int rShift, int aLength, LongSupplier aSup) {
        final int tEnd = aLength + rShift;
        for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsLong();
    }
    public static void forEachOfThis(long[] aThis, int aShift, int aLength, LongConsumer aCon) {
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
    }
    
    /** stat stuff */
    public static double sumOfThis(double[] aThis, int aShift, int aLength) {
        if (NATIVE_OPERATION) return Native.sumOfThis(aThis, aShift, aLength);
        switch(aLength) {
        case 0:  {return 0.0;}
        case 1:  {return sumOfThis1(aThis, aShift);}
        case 2:  {return sumOfThis2(aThis, aShift);}
        case 3:  {return sumOfThis3(aThis, aShift);}
        case 4:  {return sumOfThis4(aThis, aShift);}
        case 5:  {return sumOfThis5(aThis, aShift);}
        case 6:  {return sumOfThis6(aThis, aShift);}
        case 7:  {return sumOfThis7(aThis, aShift);}
        case 8:  {return sumOfThis8(aThis, aShift);}
        default: {return sumOfThisN(aThis, aShift, aLength);}
        }
    }
    public static double sumOfThis1(double[] aThis, int aShift) {
        return aThis[aShift];
    }
    public static double sumOfThis2(double[] aThis, int aShift) {
        return aThis[aShift] + aThis[aShift+1];
    }
    public static double sumOfThis3(double[] aThis, int aShift) {
        return aThis[aShift] + aThis[aShift+1] + aThis[aShift+2];
    }
    public static double sumOfThis4(double[] aThis, int aShift) {
        return aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3];
    }
    public static double sumOfThis5(double[] aThis, int aShift) {
        return aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3] + aThis[aShift+4];
    }
    public static double sumOfThis6(double[] aThis, int aShift) {
        return aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3] + aThis[aShift+4] + aThis[aShift+5];
    }
    public static double sumOfThis7(double[] aThis, int aShift) {
        return aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3] + aThis[aShift+4] + aThis[aShift+5] + aThis[aShift+6];
    }
    public static double sumOfThis8(double[] aThis, int aShift) {
        return aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3] + aThis[aShift+4] + aThis[aShift+5] + aThis[aShift+6] + aThis[aShift+7];
    }
    public static double sumOfThisN(double[] aThis, int aShift, int aLength) {
        // 对于求和类型运算，JIT 不会自动做 SIMD 优化，因此这里需要手动做
        final int tRest = aLength % 8;
        double rSum = 0.0;
        // 先做 rest 的计算
        switch(tRest) {
        case 0: {
            break;
        }
        case 1: {
            rSum += aThis[aShift];
            ++aShift;
            break;
        }
        case 2: {
            rSum += (aThis[aShift] + aThis[aShift+1]);
            aShift+=2;
            break;
        }
        case 3: {
            rSum += (aThis[aShift] + aThis[aShift+1] + aThis[aShift+2]);
            aShift+=3;
            break;
        }
        case 4: {
            rSum += (aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3]);
            aShift+=4;
            break;
        }
        case 5: {
            rSum += (aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3] + aThis[aShift+4]);
            aShift+=5;
            break;
        }
        case 6: {
            rSum += (aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3] + aThis[aShift+4] + aThis[aShift+5]);
            aShift+=6;
            break;
        }
        case 7: {
            rSum += (aThis[aShift] + aThis[aShift+1] + aThis[aShift+2] + aThis[aShift+3] + aThis[aShift+4] + aThis[aShift+5] + aThis[aShift+6]);
            aShift+=7;
            break;
        }}
        aLength -= tRest;
        // 再做 simd 的计算
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; i+=8) {
            rSum += (aThis[i] + aThis[i+1] + aThis[i+2] + aThis[i+3] + aThis[i+4] + aThis[i+5] + aThis[i+6] + aThis[i+7]);
        }
        return rSum;
    }
    
    public static ComplexDouble sumOfThis(double[][] aThis, int aShift, int aLength) {
        // 这样可以简化重复代码，并且保证部分情况下和实数的 sum 结果一致
        return new ComplexDouble(sumOfThis(aThis[0], aShift, aLength), sumOfThis(aThis[1], aShift, aLength));
    }
    public static int sumOfThis(int[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        int rSum = 0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum;
    }
    public static long exsumOfThis(int[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        long rSum = 0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum;
    }
    public static long sumOfThis(long[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        long rSum = 0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum;
    }
    
    public static double meanOfThis(double[] aThis, int aShift, int aLength) {
        return sumOfThis(aThis, aShift, aLength) / (double)aLength;
    }
    public static ComplexDouble meanOfThis(double[][] aThis, int aShift, int aLength) {
        ComplexDouble rMean = sumOfThis(aThis, aShift, aLength);
        rMean.div2this(aLength);
        return rMean;
    }
    public static double meanOfThis(int[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rSum = 0.0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum / (double)aLength;
    }
    public static double meanOfThis(long[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rSum = 0.0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum / (double)aLength;
    }
    
    public static double prodOfThis(double[] aThis, int aShift, int aLength) {
        if (NATIVE_OPERATION) return Native.prodOfThis(aThis, aShift, aLength);
        switch(aLength) {
        case 0:  {return 0.0;}
        case 1:  {return prodOfThis1(aThis, aShift);}
        case 2:  {return prodOfThis2(aThis, aShift);}
        case 3:  {return prodOfThis3(aThis, aShift);}
        case 4:  {return prodOfThis4(aThis, aShift);}
        case 5:  {return prodOfThis5(aThis, aShift);}
        case 6:  {return prodOfThis6(aThis, aShift);}
        case 7:  {return prodOfThis7(aThis, aShift);}
        case 8:  {return prodOfThis8(aThis, aShift);}
        default: {return prodOfThisN(aThis, aShift, aLength);}
        }
    }
    public static double prodOfThis1(double[] aThis, int aShift) {
        return aThis[aShift];
    }
    public static double prodOfThis2(double[] aThis, int aShift) {
        return aThis[aShift] * aThis[aShift+1];
    }
    public static double prodOfThis3(double[] aThis, int aShift) {
        return aThis[aShift] * aThis[aShift+1] * aThis[aShift+2];
    }
    public static double prodOfThis4(double[] aThis, int aShift) {
        return aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3];
    }
    public static double prodOfThis5(double[] aThis, int aShift) {
        return aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3] * aThis[aShift+4];
    }
    public static double prodOfThis6(double[] aThis, int aShift) {
        return aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3] * aThis[aShift+4] * aThis[aShift+5];
    }
    public static double prodOfThis7(double[] aThis, int aShift) {
        return aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3] * aThis[aShift+4] * aThis[aShift+5] * aThis[aShift+6];
    }
    public static double prodOfThis8(double[] aThis, int aShift) {
        return aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3] * aThis[aShift+4] * aThis[aShift+5] * aThis[aShift+6] * aThis[aShift+7];
    }
    public static double prodOfThisN(double[] aThis, int aShift, int aLength) {
        // 对于求和类型运算，JIT 不会自动做 SIMD 优化，因此这里需要手动做
        final int tRest = aLength % 8;
        double rProd = 1.0;
        // 先做 rest 的计算
        switch(tRest) {
        case 0: {
            break;
        }
        case 1: {
            rProd *= aThis[aShift];
            ++aShift;
            break;
        }
        case 2: {
            rProd *= (aThis[aShift] * aThis[aShift+1]);
            aShift+=2;
            break;
        }
        case 3: {
            rProd *= (aThis[aShift] * aThis[aShift+1] * aThis[aShift+2]);
            aShift+=3;
            break;
        }
        case 4: {
            rProd *= (aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3]);
            aShift+=4;
            break;
        }
        case 5: {
            rProd *= (aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3] * aThis[aShift+4]);
            aShift+=5;
            break;
        }
        case 6: {
            rProd *= (aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3] * aThis[aShift+4] * aThis[aShift+5]);
            aShift+=6;
            break;
        }
        case 7: {
            rProd *= (aThis[aShift] * aThis[aShift+1] * aThis[aShift+2] * aThis[aShift+3] * aThis[aShift+4] * aThis[aShift+5] * aThis[aShift+6]);
            aShift+=7;
            break;
        }}
        aLength -= tRest;
        // 再做 simd 的计算
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; i+=8) {
            rProd *= (aThis[i] * aThis[i+1] * aThis[i+2] * aThis[i+3] * aThis[i+4] * aThis[i+5] * aThis[i+6] * aThis[i+7]);
        }
        return rProd;
    }
    
    public static ComplexDouble prodOfThis(double[][] aThis, int aShift, int aLength) {
        final double[] tRealThis = aThis[0], tImagThis = aThis[1];
        final int tEnd = aLength + aShift;
        
        // 由于复数 prod 迭代的性质，没有简单的方法做 SIMD 优化，这里暂时不做
        ComplexDouble rProd = new ComplexDouble(1.0);
        for (int i = aShift; i < tEnd; ++i) {
            final double lReal = rProd.mReal,  lImag = rProd.mImag;
            final double rReal = tRealThis[i], rImag = tImagThis[i];
            rProd.mReal = lReal*rReal - lImag*rImag;
            rProd.mImag = lImag*rReal + lReal*rImag;
        }
        return rProd;
    }
    public static double prodOfThis(int[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rProd = 1.0;
        for (int i = aShift; i < tEnd; ++i) rProd *= aThis[i];
        return rProd;
    }
    public static double prodOfThis(long[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rProd = 1.0;
        for (int i = aShift; i < tEnd; ++i) rProd *= aThis[i];
        return rProd;
    }
    
    public static double maxOfThis(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rMax = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static int maxOfThis(int[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        int rMax = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) {
            int tValue = aThis[i];
            if (tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static long maxOfThis(long[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        long rMax = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) {
            long tValue = aThis[i];
            if (tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    
    public static double minOfThis(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rMin = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    public static int minOfThis(int[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        int rMin = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) {
            int tValue = aThis[i];
            if (tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    public static long minOfThis(long[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        long rMin = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) {
            long tValue = aThis[i];
            if (tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    
    public static double statOfThis(double[] aThis, int aShift, int aLength, DoubleBinaryOperator aOpt) {
        final int tEnd = aLength + aShift;
        
        double rStat = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) rStat = aOpt.applyAsDouble(rStat, aThis[i]);
        return rStat;
    }
    public static ComplexDouble statOfThis(double[][] aThis, int aShift, int aLength, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] tRealThis = aThis[0], tImagThis = aThis[1];
        final int tEnd = aLength + aShift;
        
        ComplexDouble rStat = new ComplexDouble(tRealThis[aShift], tImagThis[aShift]);
        for (int i = aShift+1; i < tEnd; ++i) rStat = toComplexDouble(aOpt.apply(rStat, new ComplexDouble(tRealThis[i], tImagThis[i])));
        return rStat;
    }
    public static double statOfThis(int[] aThis, int aShift, int aLength, DoubleBinaryOperator aOpt) {
        final int tEnd = aLength + aShift;
        
        double rStat = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) rStat = aOpt.applyAsDouble(rStat, aThis[i]);
        return rStat;
    }
    public static double statOfThis(long[] aThis, int aShift, int aLength, DoubleBinaryOperator aOpt) {
        final int tEnd = aLength + aShift;
        
        double rStat = aThis[aShift];
        for (int i = aShift+1; i < tEnd; ++i) rStat = aOpt.applyAsDouble(rStat, aThis[i]);
        return rStat;
    }
    
    
    /** 排序会用到的算法，这里不自己实现 */
    public static void reverse2Dest(double[][] aData, int aShift, double[][] rDest, int rShift, int aLength) {
        reverse2Dest(aData[0], aShift, rDest[0], rShift, aLength);
        reverse2Dest(aData[1], aShift, rDest[1], rShift, aLength);
    }
    public static void reverse2Dest(double[] aData, int aShift, double[] rDest, int rShift, int aLength) {
        final int tEnd = aShift + aLength;
        for (int i = aShift, j = rShift+aLength-1; i < tEnd; ++i, --j) {
            rDest[j] = aData[i];
        }
    }
    public static void reverse2Dest(boolean[] aData, int aShift, boolean[] rDest, int rShift, int aLength) {
        final int tEnd = aShift + aLength;
        for (int i = aShift, j = rShift+aLength-1; i < tEnd; ++i, --j) {
            rDest[j] = aData[i];
        }
    }
    public static void reverse2Dest(int[] aData, int aShift, int[] rDest, int rShift, int aLength) {
        final int tEnd = aShift + aLength;
        for (int i = aShift, j = rShift+aLength-1; i < tEnd; ++i, --j) {
            rDest[j] = aData[i];
        }
    }
    public static void reverse2Dest(long[] aData, int aShift, long[] rDest, int rShift, int aLength) {
        final int tEnd = aShift + aLength;
        for (int i = aShift, j = rShift+aLength-1; i < tEnd; ++i, --j) {
            rDest[j] = aData[i];
        }
    }
    
    public static void sort(double[] rData, int rShift, int aLength) {
        Arrays.sort(rData, rShift, aLength+rShift);
    }
    public static void sort(int[] rData, int rShift, int aLength) {
        Arrays.sort(rData, rShift, aLength+rShift);
    }
    public static void sort(long[] rData, int rShift, int aLength) {
        Arrays.sort(rData, rShift, aLength+rShift);
    }
    public static void biSort(double[] rData, int rShift, int aLength, ISwapper aSwapper) {
        Sort.multiSort(rData, rShift, aLength+rShift, aSwapper);
    }
    public static void biSort(int[] rData, int rShift, int aLength, ISwapper aSwapper) {
        Sort.multiSort(rData, rShift, aLength+rShift, aSwapper);
    }
    public static void biSort(long[] rData, int rShift, int aLength, ISwapper aSwapper) {
        Sort.multiSort(rData, rShift, aLength+rShift, aSwapper);
    }
    
    public static double dot(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, int aLength) {
        if (NATIVE_OPERATION) return Native.dot(aDataL, aShiftL, aDataR, aShiftR, aLength);
        switch(aLength) {
        case 0:  {return 0.0;}
        case 1:  {return dot1(aDataL, aShiftL, aDataR, aShiftR);}
        case 2:  {return dot2(aDataL, aShiftL, aDataR, aShiftR);}
        case 3:  {return dot3(aDataL, aShiftL, aDataR, aShiftR);}
        case 4:  {return dot4(aDataL, aShiftL, aDataR, aShiftR);}
        case 5:  {return dot5(aDataL, aShiftL, aDataR, aShiftR);}
        case 6:  {return dot6(aDataL, aShiftL, aDataR, aShiftR);}
        case 7:  {return dot7(aDataL, aShiftL, aDataR, aShiftR);}
        case 8:  {return dot8(aDataL, aShiftL, aDataR, aShiftR);}
        default: {return dotN(aDataL, aShiftL, aDataR, aShiftR, aLength);}
        }
    }
    
    public static double dot1(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL]*aDataR[aShiftR];
    }
    public static double dot2(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL  ]*aDataR[aShiftR  ]
             + aDataL[aShiftL+1]*aDataR[aShiftR+1]
             ;
    }
    public static double dot3(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL  ]*aDataR[aShiftR  ]
             + aDataL[aShiftL+1]*aDataR[aShiftR+1]
             + aDataL[aShiftL+2]*aDataR[aShiftR+2]
             ;
    }
    public static double dot4(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL  ]*aDataR[aShiftR  ]
             + aDataL[aShiftL+1]*aDataR[aShiftR+1]
             + aDataL[aShiftL+2]*aDataR[aShiftR+2]
             + aDataL[aShiftL+3]*aDataR[aShiftR+3]
             ;
    }
    public static double dot5(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL  ]*aDataR[aShiftR  ]
             + aDataL[aShiftL+1]*aDataR[aShiftR+1]
             + aDataL[aShiftL+2]*aDataR[aShiftR+2]
             + aDataL[aShiftL+3]*aDataR[aShiftR+3]
             + aDataL[aShiftL+4]*aDataR[aShiftR+4]
             ;
    }
    public static double dot6(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL  ]*aDataR[aShiftR  ]
             + aDataL[aShiftL+1]*aDataR[aShiftR+1]
             + aDataL[aShiftL+2]*aDataR[aShiftR+2]
             + aDataL[aShiftL+3]*aDataR[aShiftR+3]
             + aDataL[aShiftL+4]*aDataR[aShiftR+4]
             + aDataL[aShiftL+5]*aDataR[aShiftR+5]
             ;
    }
    public static double dot7(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL  ]*aDataR[aShiftR  ]
             + aDataL[aShiftL+1]*aDataR[aShiftR+1]
             + aDataL[aShiftL+2]*aDataR[aShiftR+2]
             + aDataL[aShiftL+3]*aDataR[aShiftR+3]
             + aDataL[aShiftL+4]*aDataR[aShiftR+4]
             + aDataL[aShiftL+5]*aDataR[aShiftR+5]
             + aDataL[aShiftL+6]*aDataR[aShiftR+6]
             ;
    }
    public static double dot8(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR) {
        return aDataL[aShiftL  ]*aDataR[aShiftR  ]
             + aDataL[aShiftL+1]*aDataR[aShiftR+1]
             + aDataL[aShiftL+2]*aDataR[aShiftR+2]
             + aDataL[aShiftL+3]*aDataR[aShiftR+3]
             + aDataL[aShiftL+4]*aDataR[aShiftR+4]
             + aDataL[aShiftL+5]*aDataR[aShiftR+5]
             + aDataL[aShiftL+6]*aDataR[aShiftR+6]
             + aDataL[aShiftL+7]*aDataR[aShiftR+7]
             ;
    }
    public static double dotN(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, int aLength) {
        // 对于求和类型运算，JIT 不会自动做 SIMD 优化，因此这里需要手动做
        double rDot = 0.0;
        // 先做 rest 的计算
        final int tRest = aLength % 8;
        switch(tRest) {
        case 0: {
            break;
        }
        case 1: {
            rDot += aDataL[aShiftL]*aDataR[aShiftR];
            ++aShiftL; ++aShiftR;
            break;
        }
        case 2: {
            rDot += (
                  aDataL[aShiftL  ]*aDataR[aShiftR  ]
                + aDataL[aShiftL+1]*aDataR[aShiftR+1]
            );
            aShiftL+=2; aShiftR+=2;
            break;
        }
        case 3: {
            rDot += (
                  aDataL[aShiftL  ]*aDataR[aShiftR  ]
                + aDataL[aShiftL+1]*aDataR[aShiftR+1]
                + aDataL[aShiftL+2]*aDataR[aShiftR+2]
            );
            aShiftL+=3; aShiftR+=3;
            break;
        }
        case 4: {
            rDot += (
                  aDataL[aShiftL  ]*aDataR[aShiftR  ]
                + aDataL[aShiftL+1]*aDataR[aShiftR+1]
                + aDataL[aShiftL+2]*aDataR[aShiftR+2]
                + aDataL[aShiftL+3]*aDataR[aShiftR+3]
            );
            aShiftL+=4; aShiftR+=4;
            break;
        }
        case 5: {
            rDot += (
                  aDataL[aShiftL  ]*aDataR[aShiftR  ]
                + aDataL[aShiftL+1]*aDataR[aShiftR+1]
                + aDataL[aShiftL+2]*aDataR[aShiftR+2]
                + aDataL[aShiftL+3]*aDataR[aShiftR+3]
                + aDataL[aShiftL+4]*aDataR[aShiftR+4]
            );
            aShiftL+=5; aShiftR+=5;
            break;
        }
        case 6: {
            rDot += (
                  aDataL[aShiftL  ]*aDataR[aShiftR  ]
                + aDataL[aShiftL+1]*aDataR[aShiftR+1]
                + aDataL[aShiftL+2]*aDataR[aShiftR+2]
                + aDataL[aShiftL+3]*aDataR[aShiftR+3]
                + aDataL[aShiftL+4]*aDataR[aShiftR+4]
                + aDataL[aShiftL+5]*aDataR[aShiftR+5]
            );
            aShiftL+=6; aShiftR+=6;
            break;
        }
        case 7: {
            rDot += (
                  aDataL[aShiftL  ]*aDataR[aShiftR  ]
                + aDataL[aShiftL+1]*aDataR[aShiftR+1]
                + aDataL[aShiftL+2]*aDataR[aShiftR+2]
                + aDataL[aShiftL+3]*aDataR[aShiftR+3]
                + aDataL[aShiftL+4]*aDataR[aShiftR+4]
                + aDataL[aShiftL+5]*aDataR[aShiftR+5]
                + aDataL[aShiftL+6]*aDataR[aShiftR+6]
            );
            aShiftL+=7; aShiftR+=7;
            break;
        }}
        aLength -= tRest;
        // 再做 simd 的计算
        final int tEndL = aShiftL + aLength;
        if (aShiftL == aShiftR) {
            for (int i = aShiftL; i < tEndL; i+=8) {
                // 这样分两批计算更快，即使在支持 avx512 的机器上也是如此（支持的是否这样做影响都不大）
                rDot += (
                      aDataL[i  ]*aDataR[i  ]
                    + aDataL[i+1]*aDataR[i+1]
                    + aDataL[i+2]*aDataR[i+2]
                    + aDataL[i+3]*aDataR[i+3]
                );
                // 不使用两个 dot 尽量保证逻辑一致性
                rDot += (
                      aDataL[i+4]*aDataR[i+4]
                    + aDataL[i+5]*aDataR[i+5]
                    + aDataL[i+6]*aDataR[i+6]
                    + aDataL[i+7]*aDataR[i+7]
                );
            }
        } else {
            for (int i = aShiftL, j = aShiftR; i < tEndL; i+=8, j+=8) {
                // 这样分两批计算更快，即使在支持 avx512 的机器上也是如此（支持的是否这样做影响都不大）
                rDot += (
                      aDataL[i  ]*aDataR[j  ]
                    + aDataL[i+1]*aDataR[j+1]
                    + aDataL[i+2]*aDataR[j+2]
                    + aDataL[i+3]*aDataR[j+3]
                );
                // 不使用两个 dot 尽量保证逻辑一致性
                rDot += (
                      aDataL[i+4]*aDataR[j+4]
                    + aDataL[i+5]*aDataR[j+5]
                    + aDataL[i+6]*aDataR[j+6]
                    + aDataL[i+7]*aDataR[j+7]
                );
            }
        }
        return rDot;
    }
    
    /** 现在这个方法更快了 */
    public static double dotOfThis(double[] aThis, int aShift, int aLength) {
        if (NATIVE_OPERATION) return Native.dotOfThis(aThis, aShift, aLength);
        switch(aLength) {
        case 0:  {return 0.0;}
        case 1:  {return dotOfThis1(aThis, aShift);}
        case 2:  {return dotOfThis2(aThis, aShift);}
        case 3:  {return dotOfThis3(aThis, aShift);}
        case 4:  {return dotOfThis4(aThis, aShift);}
        case 5:  {return dotOfThis5(aThis, aShift);}
        case 6:  {return dotOfThis6(aThis, aShift);}
        case 7:  {return dotOfThis7(aThis, aShift);}
        case 8:  {return dotOfThis8(aThis, aShift);}
        default: {return dotOfThisN(aThis, aShift, aLength);}
        }
    }
    public static double dotOfThis1(double[] aThis, int aShift) {
        double tData = aThis[aShift];
        return tData*tData;
    }
    public static double dotOfThis2(double[] aThis, int aShift) {
        double tData0 = aThis[aShift  ];
        double tData1 = aThis[aShift+1];
        return tData0*tData0 + tData1*tData1;
    }
    public static double dotOfThis3(double[] aThis, int aShift) {
        double tData0 = aThis[aShift  ];
        double tData1 = aThis[aShift+1];
        double tData2 = aThis[aShift+2];
        return tData0*tData0 + tData1*tData1 + tData2*tData2;
    }
    public static double dotOfThis4(double[] aThis, int aShift) {
        double tData0 = aThis[aShift  ];
        double tData1 = aThis[aShift+1];
        double tData2 = aThis[aShift+2];
        double tData3 = aThis[aShift+3];
        return tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3;
    }
    public static double dotOfThis5(double[] aThis, int aShift) {
        double tData0 = aThis[aShift  ];
        double tData1 = aThis[aShift+1];
        double tData2 = aThis[aShift+2];
        double tData3 = aThis[aShift+3];
        double tData4 = aThis[aShift+4];
        return tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3 + tData4*tData4;
    }
    public static double dotOfThis6(double[] aThis, int aShift) {
        double tData0 = aThis[aShift  ];
        double tData1 = aThis[aShift+1];
        double tData2 = aThis[aShift+2];
        double tData3 = aThis[aShift+3];
        double tData4 = aThis[aShift+4];
        double tData5 = aThis[aShift+5];
        return tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3 + tData4*tData4 + tData5*tData5;
    }
    public static double dotOfThis7(double[] aThis, int aShift) {
        double tData0 = aThis[aShift  ];
        double tData1 = aThis[aShift+1];
        double tData2 = aThis[aShift+2];
        double tData3 = aThis[aShift+3];
        double tData4 = aThis[aShift+4];
        double tData5 = aThis[aShift+5];
        double tData6 = aThis[aShift+6];
        return tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3 + tData4*tData4 + tData5*tData5 + tData6*tData6;
    }
    public static double dotOfThis8(double[] aThis, int aShift) {
        double tData0 = aThis[aShift  ];
        double tData1 = aThis[aShift+1];
        double tData2 = aThis[aShift+2];
        double tData3 = aThis[aShift+3];
        double tData4 = aThis[aShift+4];
        double tData5 = aThis[aShift+5];
        double tData6 = aThis[aShift+6];
        double tData7 = aThis[aShift+7];
        return tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3 + tData4*tData4 + tData5*tData5 + tData6*tData6 + tData7*tData7;
    }
    public static double dotOfThisN(double[] aThis, int aShift, int aLength) {
        // 对于求和类型运算，JIT 不会自动做 SIMD 优化，因此这里需要手动做
        final int tRest = aLength % 8;
        double rDot = 0.0;
        // 先做 rest 的计算
        switch(tRest) {
        case 0: {
            break;
        }
        case 1: {
            double tData = aThis[aShift];
            rDot += tData*tData;
            ++aShift;
            break;
        }
        case 2: {
            double tData0 = aThis[aShift  ];
            double tData1 = aThis[aShift+1];
            rDot += (tData0*tData0 + tData1*tData1);
            aShift+=2;
            break;
        }
        case 3: {
            double tData0 = aThis[aShift  ];
            double tData1 = aThis[aShift+1];
            double tData2 = aThis[aShift+2];
            rDot += (tData0*tData0 + tData1*tData1 + tData2*tData2);
            aShift+=3;
            break;
        }
        case 4: {
            double tData0 = aThis[aShift  ];
            double tData1 = aThis[aShift+1];
            double tData2 = aThis[aShift+2];
            double tData3 = aThis[aShift+3];
            rDot += (tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3);
            aShift+=4;
            break;
        }
        case 5: {
            double tData0 = aThis[aShift  ];
            double tData1 = aThis[aShift+1];
            double tData2 = aThis[aShift+2];
            double tData3 = aThis[aShift+3];
            double tData4 = aThis[aShift+4];
            rDot += (tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3 + tData4*tData4);
            aShift+=5;
            break;
        }
        case 6: {
            double tData0 = aThis[aShift  ];
            double tData1 = aThis[aShift+1];
            double tData2 = aThis[aShift+2];
            double tData3 = aThis[aShift+3];
            double tData4 = aThis[aShift+4];
            double tData5 = aThis[aShift+5];
            rDot += (tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3 + tData4*tData4 + tData5*tData5);
            aShift+=6;
            break;
        }
        case 7: {
            double tData0 = aThis[aShift  ];
            double tData1 = aThis[aShift+1];
            double tData2 = aThis[aShift+2];
            double tData3 = aThis[aShift+3];
            double tData4 = aThis[aShift+4];
            double tData5 = aThis[aShift+5];
            double tData6 = aThis[aShift+6];
            rDot += (tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3 + tData4*tData4 + tData5*tData5 + tData6*tData6);
            aShift+=7;
            break;
        }}
        aLength -= tRest;
        // 再做 simd 的计算
        final int tEndL = aShift + aLength;
        for (int i = aShift; i < tEndL; i+=8) {
            double tData0 = aThis[i  ];
            double tData1 = aThis[i+1];
            double tData2 = aThis[i+2];
            double tData3 = aThis[i+3];
            double tData4 = aThis[i+4];
            double tData5 = aThis[i+5];
            double tData6 = aThis[i+6];
            double tData7 = aThis[i+7];
            // 这样分两批计算更快，即使在支持 avx512 的机器上也是如此（支持的是否这样做影响都不大）
            rDot += (tData0*tData0 + tData1*tData1 + tData2*tData2 + tData3*tData3);
            rDot += (tData4*tData4 + tData5*tData5 + tData6*tData6 + tData7*tData7);
        }
        return rDot;
    }
    
    public static ComplexDouble dot(double[][] aDataL, int aShiftL, double[][] aDataR, int aShiftR, int aLength) {
        final double[] tRealDataL = aDataL[0], tImagDataL = aDataL[1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final int tEndL = aLength + aShiftL;
        
        ComplexDouble rDot = new ComplexDouble();
        if (aShiftL == aShiftR) {
            for (int i = aShiftL; i < tEndL; ++i) {
                double lReal0 = tRealDataL[i], lImag0 = tImagDataL[i];
                double rReal0 = tRealDataR[i], rImag0 = tImagDataR[i];
                rDot.mReal += (lReal0*rReal0 + lImag0*rImag0);
                rDot.mImag += (lImag0*rReal0 - lReal0*rImag0);
            }
        } else {
            for (int i = aShiftL, j = aShiftR; i < tEndL; ++i, ++j) {
                double lReal = tRealDataL[i], lImag = tImagDataL[i];
                double rReal = tRealDataR[j], rImag = tImagDataR[j];
                rDot.mReal += (lReal*rReal + lImag*rImag);
                rDot.mImag += (lImag*rReal - lReal*rImag);
            }
        }
        return rDot;
    }
    public static double dotOfThis(double[][] aThis, int aShift, int aLength) {
        // 这样可以简化重复代码，并且保证部分情况下和实数的 dot 结果一致
        return dotOfThis(aThis[0], aShift, aLength) + dotOfThis(aThis[1], aShift, aLength);
    }
    
    public static double norm1OfThis(double[] aThis, int aShift, int aLength) {
        if (NATIVE_OPERATION) return Native.norm1OfThis(aThis, aShift, aLength);
        switch(aLength) {
        case 0:  {return 0.0;}
        case 1:  {return norm1OfThis1(aThis, aShift);}
        case 2:  {return norm1OfThis2(aThis, aShift);}
        case 3:  {return norm1OfThis3(aThis, aShift);}
        case 4:  {return norm1OfThis4(aThis, aShift);}
        case 5:  {return norm1OfThis5(aThis, aShift);}
        case 6:  {return norm1OfThis6(aThis, aShift);}
        case 7:  {return norm1OfThis7(aThis, aShift);}
        case 8:  {return norm1OfThis8(aThis, aShift);}
        default: {return norm1OfThisN(aThis, aShift, aLength);}
        }
    }
    public static double norm1OfThis1(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]);
    }
    public static double norm1OfThis2(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]);
    }
    public static double norm1OfThis3(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]);
    }
    public static double norm1OfThis4(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]);
    }
    public static double norm1OfThis5(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]) + Math.abs(aThis[aShift+4]);
    }
    public static double norm1OfThis6(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]) + Math.abs(aThis[aShift+4]) + Math.abs(aThis[aShift+5]);
    }
    public static double norm1OfThis7(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]) + Math.abs(aThis[aShift+4]) + Math.abs(aThis[aShift+5]) + Math.abs(aThis[aShift+6]);
    }
    public static double norm1OfThis8(double[] aThis, int aShift) {
        return Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]) + Math.abs(aThis[aShift+4]) + Math.abs(aThis[aShift+5]) + Math.abs(aThis[aShift+6]) + Math.abs(aThis[aShift+7]);
    }
    public static double norm1OfThisN(double[] aThis, int aShift, int aLength) {
        // 对于求和类型运算，JIT 不会自动做 SIMD 优化，因此这里需要手动做
        final int tRest = aLength % 8;
        double rSum = 0.0;
        // 先做 rest 的计算
        switch(tRest) {
        case 0: {
            break;
        }
        case 1: {
            rSum += Math.abs(aThis[aShift]);
            ++aShift;
            break;
        }
        case 2: {
            rSum += (Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]));
            aShift+=2;
            break;
        }
        case 3: {
            rSum += (Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]));
            aShift+=3;
            break;
        }
        case 4: {
            rSum += (Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]));
            aShift+=4;
            break;
        }
        case 5: {
            rSum += (Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]) + Math.abs(aThis[aShift+4]));
            aShift+=5;
            break;
        }
        case 6: {
            rSum += (Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]) + Math.abs(aThis[aShift+4]) + Math.abs(aThis[aShift+5]));
            aShift+=6;
            break;
        }
        case 7: {
            rSum += (Math.abs(aThis[aShift]) + Math.abs(aThis[aShift+1]) + Math.abs(aThis[aShift+2]) + Math.abs(aThis[aShift+3]) + Math.abs(aThis[aShift+4]) + Math.abs(aThis[aShift+5]) + Math.abs(aThis[aShift+6]));
            aShift+=7;
            break;
        }}
        aLength -= tRest;
        // 再做 simd 的计算
        final int tEnd = aLength + aShift;
        for (int i = aShift; i < tEnd; i+=8) {
            rSum += (Math.abs(aThis[i]) + Math.abs(aThis[i+1]) + Math.abs(aThis[i+2]) + Math.abs(aThis[i+3]) + Math.abs(aThis[i+4]) + Math.abs(aThis[i+5]) + Math.abs(aThis[i+6]) + Math.abs(aThis[i+7]));
        }
        return rSum;
    }
    
    
    /** 较为复杂的运算，只有遇到时专门增加，主要避免 IOperator2 使用需要新建 ComplexDouble */
    public static void mapMultiplyThenEbePlus2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, double aMul, int aLength) {
        final double[] rRealThis  = rThis [0], rImagThis  = rThis [1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) {
            for (int i = rShift; i < rEnd; ++i) {
                rRealThis[i] += aMul*tRealDataR[i];
                rImagThis[i] += aMul*tImagDataR[i];
            }
        } else {
            for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) {
                rRealThis[i] += aMul*tRealDataR[j];
                rImagThis[i] += aMul*tImagDataR[j];
            }
        }
    }
    public static void mapMultiplyThenEbePlus2This(double[][] rThis, int rShift, double[][] aDataR, int aShiftR, IComplexDouble aMul, int aLength) {
        final double[] rRealThis  = rThis [0], rImagThis  = rThis [1];
        final double[] tRealDataR = aDataR[0], tImagDataR = aDataR[1];
        final double lReal = aMul.real(), lImag = aMul.imag();
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) {
            for (int i = rShift; i < rEnd; ++i) {
                final double rReal = tRealDataR[i], rImag = tImagDataR[i];
                rRealThis[i] += lReal*rReal - lImag*rImag;
                rImagThis[i] += lImag*rReal + lReal*rImag;
            }
        } else {
            for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) {
                final double rReal = tRealDataR[j], rImag = tImagDataR[j];
                rRealThis[i] += lReal*rReal - lImag*rImag;
                rImagThis[i] += lImag*rReal + lReal*rImag;
            }
        }
    }
    /** 一般向量也添加这个接口来保持一致 */
    public static void mapMultiplyThenEbePlus2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, double aMul, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] += aMul*aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] += aMul*aDataR[j];
    }
    
    
    public static class Native {
        public final static class InitHelper {
            private static volatile boolean INITIALIZED = false;
            /** @return {@link ARRAY} 相关的 JNI 库是否已经初始化完成 */
            public static boolean initialized() {return INITIALIZED;}
            /** 初始化 {@link ARRAY} 相关的 JNI 库 */
            @SuppressWarnings("ResultOfMethodCallIgnored")
            public static void init() {
                if (!INITIALIZED) String.valueOf(LIB_PATH);
            }
        }
        
        public final static class Conf {
            /**
             * 自定义构建 math 的 cmake 参数设置，
             * 会在构建时使用 -D ${key}=${value} 传入
             */
            public final static Map<String, String> CMAKE_SETTING = new LinkedHashMap<>();
            
            public static final int NONE = -1;
            public static final int COMPAT = 0;
            public static final int BASE = 1;
            public static final int MAX = 2;
            /**
             * 自定义 math 需要采用的优化等级，默认为 1（基础优化），
             * 会开启 AVX2 指令集，在大多数现代处理器上能兼容运行
             */
            public static int OPT_LEVEL = OS.envI("JSE_MATH_OPT_LEVEL", BASE);
            
            /**
             * 自定义 match 内部循环使用的 batch size，
             * 一般来说不需要专门调整
             */
            public static int BATCH_SIZE = OS.envI("JSE_MATH_BATCH_SIZE", 64);
            
            /**
             * 自定义构建 math 时使用的编译器，
             * cmake 有时不能自动检测到希望使用的编译器
             * <p>
             * 也可使用环境变量 {@code JSE_CMAKE_C_COMPILER_MATH} 来设置
             */
            public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_MATH"  , jse.code.Conf.CMAKE_C_COMPILER);
            /**
             * 自定义构建 math 时使用的编译器，
             * cmake 有时不能自动检测到希望使用的编译器
             * <p>
             * 也可使用环境变量 {@code JSE_CMAKE_CXX_COMPILER_MATH} 来设置
             */
            public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_MATH", jse.code.Conf.CMAKE_CXX_COMPILER);
            /**
             * 自定义构建 math 时使用的编译器，
             * cmake 有时不能自动检测到希望使用的编译器
             * <p>
             * 也可使用环境变量 {@code JSE_CMAKE_C_FLAGS_MATH} 来设置
             */
            public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_MATH"     , jse.code.Conf.CMAKE_C_FLAGS);
            /**
             * 自定义构建 math 时使用的编译器，
             * cmake 有时不能自动检测到希望使用的编译器
             * <p>
             * 也可使用环境变量 {@code JSE_CMAKE_CXX_FLAGS_MATH} 来设置
             */
            public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_MATH"   , jse.code.Conf.CMAKE_CXX_FLAGS);
            
            /**
             * 重定向 math 动态库的路径，用于自定义编译这个库的过程，或者重新实现 math 的接口
             * <p>
             * 也可使用环境变量 {@code JSE_REDIRECT_MATH_LIB} 来设置
             */
            public static @Nullable String REDIRECT_MATH_LIB = OS.env("JSE_REDIRECT_MATH_LIB");
        }
        
        /** 当前 {@link ARRAY} JNI 库所在的文件夹路径，结尾一定存在 {@code '/'} */
        public final static String LIB_DIR = JAR_DIR+"math/" + UT.Code.uniqueID(JAVA_HOME, VERSION, Conf.OPT_LEVEL, Conf.BATCH_SIZE, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
        /** 当前 {@link ARRAY} JNI 库的路径 */
        public final static String LIB_PATH;
        private final static String[] SRC_NAME = {
              "jse_math_operation_ARRAY_Native.c"
            , "jse_math_operation_ARRAY_Native.h"
            , "math_util.h"
        };
        
        static {
            InitHelper.INITIALIZED = true;
            // 依赖 jniutil
            JNIUtil.InitHelper.init();
            // 这里不直接依赖 LmpPlugin
            
            // 先添加 Conf.CMAKE_SETTING，这样保证确定的优先级
            Map<String, String> rCmakeSetting = new LinkedHashMap<>(Conf.CMAKE_SETTING);
            switch(Conf.OPT_LEVEL) {
            case Conf.MAX: {
                rCmakeSetting.put("JSE_OPT_MAX",    "ON");
                rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
                rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
                break;
            }
            case Conf.BASE: {
                rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
                rCmakeSetting.put("JSE_OPT_BASE",   "ON");
                rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
                break;
            }
            case Conf.COMPAT: {
                rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
                rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
                rCmakeSetting.put("JSE_OPT_COMPAT", "ON");
                break;
            }
            case Conf.NONE: {
                rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
                rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
                rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
                break;
            }}
            rCmakeSetting.put("JSE_BATCH_SIZE", String.valueOf(Conf.BATCH_SIZE));
            // 现在直接使用 JNIUtil.buildLib 来统一初始化
            LIB_PATH = new JNIUtil.LibBuilder("math", "MATH", LIB_DIR, rCmakeSetting)
                .setSrc("math", SRC_NAME)
                .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS)
                .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
                .setRedirectLibPath(Conf.REDIRECT_MATH_LIB)
                .get();
            // 设置库路径
            System.load(IO.toAbsolutePath(LIB_PATH));
        }
        
        static void lengthCheck(int aNeed, int aLength) {
            if (aNeed<0 || aNeed>aLength) throw new IndexOutOfBoundsException("need = " + aNeed + ", length = " + aLength);
        }
        
        public static double sumOfThis(double[] aThis, int aShift, int aLength) {
            lengthCheck(aLength+aShift, aThis.length);
            return sumOfThis_(aThis, aShift, aLength);
        }
        private native static double sumOfThis_(double[] aThis, int aShift, int aLength);
        
        public static double prodOfThis(double[] aThis, int aShift, int aLength) {
            lengthCheck(aLength+aShift, aThis.length);
            return prodOfThis_(aThis, aShift, aLength);
        }
        private native static double prodOfThis_(double[] aThis, int aShift, int aLength);
        
        public static double dot(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, int aLength) {
            lengthCheck(aLength+aShiftL, aDataL.length);
            lengthCheck(aLength+aShiftR, aDataR.length);
            return dot_(aDataL, aShiftL, aDataR, aShiftR, aLength);
        }
        private native static double dot_(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, int aLength);
        
        public static double dotOfThis(double[] aThis, int aShift, int aLength) {
            lengthCheck(aLength+aShift, aThis.length);
            return dotOfThis_(aThis, aShift, aLength);
        }
        private native static double dotOfThis_(double[] aThis, int aShift, int aLength);
        
        public static double norm1OfThis(double[] aThis, int aShift, int aLength) {
            lengthCheck(aLength+aShift, aThis.length);
            return norm1OfThis_(aThis, aShift, aLength);
        }
        private native static double norm1OfThis_(double[] aThis, int aShift, int aLength);
        
        public static void matmulRC2Dest(double[] aDataRowL, int aShiftL, double[] aDataColR, int aShiftR,
                                         double[] rDestRow, int rShift, int aRowNum, int aColNum, int aMidNum) {
            lengthCheck(aRowNum*aMidNum + aShiftL, aDataRowL.length);
            lengthCheck(aMidNum*aColNum + aShiftR, aDataColR.length);
            lengthCheck(aRowNum*aColNum + rShift, rDestRow.length);
            matmulRC2Dest_(aDataRowL, aShiftL, aDataColR, aShiftR, rDestRow, rShift, aRowNum, aColNum, aMidNum);
        }
        private native static void matmulRC2Dest_(double[] aDataRowL, int aShiftL, double[] aDataColR, int aShiftR,
                                                  double[] rDestRow, int rShift, int aRowNum, int aColNum, int aMidNum);
    }
}
