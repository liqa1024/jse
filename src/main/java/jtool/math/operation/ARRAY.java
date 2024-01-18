package jtool.math.operation;

import com.mastfrog.util.sort.Sort;
import jtool.code.functional.*;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;

import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import static jtool.code.UT.Code.toComplexDouble;


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
    
    public static void mapFill2This(double[] rThis, int rShift, double aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    
    public static void mapFill2This(boolean[] rThis, int rShift, boolean aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    
    public static void mapFill2This(int[] rThis, int rShift, int aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2This(int[] rThis, int rShift, int[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
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
    public static double maxOfThis(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rMax = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (Double.isNaN(rMax) || tValue > rMax) rMax = tValue;
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
    
    
    /** 排序会用到的算法，这里不自己实现 */
    public static void sort(double[] rData, int rShift, int aLength) {
        Arrays.sort(rData, rShift, aLength+rShift);
    }
    public static void sort(int[] rData, int rShift, int aLength) {
        Arrays.sort(rData, rShift, aLength+rShift);
    }
    public static void biSort(double[] rData, int rShift, int aLength, ISwapper aSwapper) {
        Sort.multiSort(rData, rShift, aLength+rShift, aSwapper);
    }
    public static void biSort(int[] rData, int rShift, int aLength, ISwapper aSwapper) {
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
