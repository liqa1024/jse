package jse.math.operation;

import com.mastfrog.util.sort.Sort;
import groovy.lang.Closure;
import jse.code.functional.*;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.vector.*;

import java.util.Arrays;
import java.util.function.*;

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
    public static void vecFill2This(double[][] rThis, int rShift, int aLength, boolean aReverse, IComplexVectorGetter aVec) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM, j = 0; i >= rShift; --i, ++j) {
                IComplexDouble tValue = aVec.get(j);
                tRealData[i] = tValue.real();
                tImagData[i] = tValue.imag();
            }
        } else {
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
    }
    public static void vecFill2This(double[][] rThis, int rShift, int aLength, boolean aReverse, IVectorGetter aVec) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM, j = 0; i >= rShift; --i, ++j) {
                tRealData[i] = aVec.get(j);
                tImagData[i] = 0.0;
            }
        } else {
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
    }
    public static void assign2This(double[][] rThis, int rShift, int aLength, boolean aReverse, Supplier<? extends IComplexDouble> aSup) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM; i >= rShift; --i) {
                IComplexDouble tValue = aSup.get();
                tRealData[i] = tValue.real();
                tImagData[i] = tValue.imag();
            }
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift; i < tEnd; ++i) {
                IComplexDouble tValue = aSup.get();
                tRealData[i] = tValue.real();
                tImagData[i] = tValue.imag();
            }
        }
    }
    public static void assign2This(double[][] rThis, int rShift, int aLength, boolean aReverse, DoubleSupplier aSup) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM; i >= rShift; --i) {
                tRealData[i] = aSup.getAsDouble();
                tImagData[i] = 0.0;
            }
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift; i < tEnd; ++i) {
                tRealData[i] = aSup.getAsDouble();
                tImagData[i] = 0.0;
            }
        }
    }
    public static void forEachOfThis(double[][] aThis, int aShift, int aLength, boolean aReverse, Consumer<? super ComplexDouble> aCon) {
        final double[] tRealData = aThis[0];
        final double[] tImagData = aThis[1];
        if (aReverse) {
            final int tEndMM = aLength + aShift - 1;
            for (int i = tEndMM; i >= aShift; --i) {
                aCon.accept(new ComplexDouble(tRealData[i], tImagData[i]));
            }
        } else {
            final int tEnd = aLength + aShift;
            for (int i = aShift; i < tEnd; ++i) {
                aCon.accept(new ComplexDouble(tRealData[i], tImagData[i]));
            }
        }
    }
    public static void forEachOfThis(double[][] aThis, int aShift, int aLength, boolean aReverse, IDoubleBinaryConsumer aCon) {
        final double[] tRealData = aThis[0];
        final double[] tImagData = aThis[1];
        if (aReverse) {
            final int tEndMM = aLength + aShift - 1;
            for (int i = tEndMM; i >= aShift; --i) {
                aCon.accept(tRealData[i], tImagData[i]);
            }
        } else {
            final int tEnd = aLength + aShift;
            for (int i = aShift; i < tEnd; ++i) {
                aCon.accept(tRealData[i], tImagData[i]);
            }
        }
    }
    /** Groovy stuffs */
    public static void vecFill2This(double[][] rThis, int rShift, int aLength, boolean aReverse, Closure<?> aGroovyTask) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM, j = 0; i >= rShift; --i, ++j) {
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
        } else {
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
    }
    public static void assign2This(double[][] rThis, int rShift, int aLength, boolean aReverse, Closure<?> aGroovyTask) {
        final double[] tRealData = rThis[0];
        final double[] tImagData = rThis[1];
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM; i >= rShift; --i) {
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
        } else {
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
    }
    
    public static void mapFill2This(double[] rThis, int rShift, double aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(double[] rThis, int rShift, int aLength, boolean aReverse, IVectorGetter aVec) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM, j = 0; i >= rShift; --i, ++j) rThis[i] = aVec.get(j);
        } else {
            final int tEnd = aLength + rShift;
            if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
            else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
        }
    }
    public static void assign2This(double[] rThis, int rShift, int aLength, boolean aReverse, DoubleSupplier aSup) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM; i >= rShift; --i) rThis[i] = aSup.getAsDouble();
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsDouble();
        }
    }
    public static void forEachOfThis(double[] aThis, int aShift, int aLength, boolean aReverse, DoubleConsumer aCon) {
        if (aReverse) {
            final int tEndMM = aLength + aShift - 1;
            for (int i = tEndMM; i >= aShift; --i) aCon.accept(aThis[i]);
        } else {
            final int tEnd = aLength + aShift;
            for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
        }
    }
    
    public static void mapFill2This(boolean[] rThis, int rShift, boolean aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(boolean[] rThis, int rShift, int aLength, boolean aReverse, ILogicalVectorGetter aVec) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM, j = 0; i >= rShift; --i, ++j) rThis[i] = aVec.get(j);
        } else {
            final int tEnd = aLength + rShift;
            if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
            else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
        }
    }
    public static void assign2This(boolean[] rThis, int rShift, int aLength, boolean aReverse, BooleanSupplier aSup) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM; i >= rShift; --i) rThis[i] = aSup.getAsBoolean();
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsBoolean();
        }
    }
    public static void forEachOfThis(boolean[] aThis, int aShift, int aLength, boolean aReverse, IBooleanConsumer aCon) {
        if (aReverse) {
            final int tEndMM = aLength + aShift - 1;
            for (int i = tEndMM; i >= aShift; --i) aCon.accept(aThis[i]);
        } else {
            final int tEnd = aLength + aShift;
            for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
        }
    }
    
    public static void mapFill2This(int[] rThis, int rShift, int aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(int[] rThis, int rShift, int aLength, boolean aReverse, IIntVectorGetter aVec) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM, j = 0; i >= rShift; --i, ++j) rThis[i] = aVec.get(j);
        } else {
            final int tEnd = aLength + rShift;
            if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
            else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
        }
    }
    public static void assign2This(int[] rThis, int rShift, int aLength, boolean aReverse, IntSupplier aSup) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM; i >= rShift; --i) rThis[i] = aSup.getAsInt();
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsInt();
        }
    }
    public static void forEachOfThis(int[] aThis, int aShift, int aLength, boolean aReverse, IntConsumer aCon) {
        if (aReverse) {
            final int tEndMM = aLength + aShift - 1;
            for (int i = tEndMM; i >= aShift; --i) aCon.accept(aThis[i]);
        } else {
            final int tEnd = aLength + aShift;
            for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
        }
    }
    
    public static void mapFill2This(long[] rThis, int rShift, long aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(long[] rThis, int rShift, long[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    public static void vecFill2This(long[] rThis, int rShift, int aLength, boolean aReverse, ILongVectorGetter aVec) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM, j = 0; i >= rShift; --i, ++j) rThis[i] = aVec.get(j);
        } else {
            final int tEnd = aLength + rShift;
            if (rShift == 0) {for (int i = rShift; i < tEnd; ++i) rThis[i] = aVec.get(i);}
            else {for (int i = rShift, j = 0; i < tEnd; ++i, ++j) rThis[i] = aVec.get(j);}
        }
    }
    public static void assign2This(long[] rThis, int rShift, int aLength, boolean aReverse, LongSupplier aSup) {
        if (aReverse) {
            final int tEndMM = aLength + rShift - 1;
            for (int i = tEndMM; i >= rShift; --i) rThis[i] = aSup.getAsLong();
        } else {
            final int tEnd = aLength + rShift;
            for (int i = rShift; i < tEnd; ++i) rThis[i] = aSup.getAsLong();
        }
    }
    public static void forEachOfThis(long[] aThis, int aShift, int aLength, boolean aReverse, LongConsumer aCon) {
        if (aReverse) {
            final int tEndMM = aLength + aShift - 1;
            for (int i = tEndMM; i >= aShift; --i) aCon.accept(aThis[i]);
        } else {
            final int tEnd = aLength + aShift;
            for (int i = aShift; i < tEnd; ++i) aCon.accept(aThis[i]);
        }
    }
    
    /** stat stuff */
    public static double sumOfThis(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rSum = 0.0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum;
    }
    public static ComplexDouble sumOfThis(double[][] aThis, int aShift, int aLength) {
        final double[] tRealThis = aThis[0], tImagThis = aThis[1];
        final int tEnd = aLength + aShift;
        
        ComplexDouble rSum = new ComplexDouble();
        for (int i = aShift; i < tEnd; ++i) {
            rSum.mReal += tRealThis[i];
            rSum.mImag += tImagThis[i];
        }
        return rSum;
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
        final int tEnd = aLength + aShift;
        
        double rSum = 0.0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum / (double)aLength;
    }
    public static ComplexDouble meanOfThis(double[][] aThis, int aShift, int aLength) {
        final double[] tRealThis = aThis[0], tImagThis = aThis[1];
        final int tEnd = aLength + aShift;
        
        ComplexDouble rMean = new ComplexDouble();
        for (int i = aShift; i < tEnd; ++i) {
            rMean.mReal += tRealThis[i];
            rMean.mImag += tImagThis[i];
        }
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
        final int tEnd = aLength + aShift;
        
        double rProd = 1.0;
        for (int i = aShift; i < tEnd; ++i) rProd *= aThis[i];
        return rProd;
    }
    public static ComplexDouble prodOfThis(double[][] aThis, int aShift, int aLength) {
        final double[] tRealThis = aThis[0], tImagThis = aThis[1];
        final int tEnd = aLength + aShift;
        
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
        
        double rMax = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (Double.isNaN(rMax) || tValue > rMax) rMax = tValue;
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
        
        double rMin = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (Double.isNaN(rMin) || tValue < rMin) rMin = tValue;
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
        
        double rStat = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) rStat = aOpt.applyAsDouble(rStat, aThis[i]);
        return rStat;
    }
    public static ComplexDouble statOfThis(double[][] aThis, int aShift, int aLength, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] tRealThis = aThis[0], tImagThis = aThis[1];
        final int tEnd = aLength + aShift;
        
        ComplexDouble rStat = null;
        for (int i = aShift; i < tEnd; ++i) rStat = toComplexDouble(aOpt.apply(rStat, new ComplexDouble(tRealThis[i], tImagThis[i])));
        return rStat;
    }
    public static double statOfThis(int[] aThis, int aShift, int aLength, DoubleBinaryOperator aOpt) {
        final int tEnd = aLength + aShift;
        
        double rStat = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) rStat = aOpt.applyAsDouble(rStat, aThis[i]);
        return rStat;
    }
    public static double statOfThis(long[] aThis, int aShift, int aLength, DoubleBinaryOperator aOpt) {
        final int tEnd = aLength + aShift;
        
        double rStat = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) rStat = aOpt.applyAsDouble(rStat, aThis[i]);
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
}
