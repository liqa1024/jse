package com.jtool.math.operation;

import com.jtool.code.operator.IBooleanOperator1;
import com.jtool.code.operator.IBooleanOperator2;
import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;


/**
 * 对于内部含有 double[] 的数据的运算做专门优化，方便编译器做 SIMD 的相关优化
 * @author liqa
 */
public class ARRAY {
    private ARRAY() {}
    
    /** logical stuffs */
    public static void ebeAnd2Dest_(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] && aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] && aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] && aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] && aDataR[k];
        }
    }
    public static void ebeOr2Dest_(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] || aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] || aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] || aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] || aDataR[k];
        }
    }
    public static void ebeXor2Dest_(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] ^ aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] ^ aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] ^ aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] ^ aDataR[k];
        }
    }
    
    
    public static void mapAnd2Dest_(boolean[] aDataL, int aShiftL, boolean aRHS, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] && aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] && aRHS;
    }
    public static void mapOr2Dest_(boolean[] aDataL, int aShiftL, boolean aRHS, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] || aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] || aRHS;
    }
    public static void mapXor2Dest_(boolean[] aDataL, int aShiftL, boolean aRHS, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] ^ aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] ^ aRHS;
    }
    
    
    public static void ebeAnd2this_(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] &= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] &= aDataR[j];
    }
    public static void ebeOr2this_(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] |= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] |= aDataR[j];
    }
    public static void ebeXor2this_(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] ^= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] ^= aDataR[j];
    }
    
    
    public static void mapAnd2this_     (boolean[] rThis, int rShift, boolean aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] &= aRHS;}
    public static void mapOr2this_      (boolean[] rThis, int rShift, boolean aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] |= aRHS;}
    public static void mapXor2this_     (boolean[] rThis, int rShift, boolean aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] ^= aRHS;}
    
    
    public static void not2Dest_(boolean[] aData, int aShift, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShift) for (int i = rShift; i < rEnd; ++i) rDest[i] = !aData[i];
        else for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) rDest[i] = !aData[j];
    }
    public static void not2this_(boolean[] rThis, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = !rThis[i];
    }
    
    public static boolean allOfThis_(boolean[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        for (int i = aShift; i < tEnd; ++i) {
            if (!aThis[i]) return false;
        }
        return true;
    }
    public static boolean anyOfThis_(boolean[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        for (int i = aShift; i < tEnd; ++i) {
            if (aThis[i]) return true;
        }
        return false;
    }
    public static int countOfThis_(boolean[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        int rCount = 0;
        for (int i = aShift; i < tEnd; ++i) {
            if (aThis[i]) ++rCount;
        }
        return rCount;
    }
    
    public static void cumall2Dest_(boolean[] aThis, int aShift, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        boolean rAll = true;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {rAll &= aThis[i]; rDest[i] = rAll;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {rAll &= aThis[j]; rDest[i] = rAll;}}
    }
    public static void cumany2Dest_(boolean[] aThis, int aShift, boolean[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        boolean rAny = false;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {rAny |= aThis[i]; rDest[i] = rAny;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {rAny |= aThis[j]; rDest[i] = rAny;}}
    }
    public static void cumcount2Dest_(boolean[] aThis, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        int rCount = 0;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {if (aThis[i]) ++rCount; rDest[i] = rCount;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {if (aThis[j]) ++rCount; rDest[i] = rCount;}}
    }
    
    
    
    /** add, minus, multiply, divide stuffs */
    public static void ebePlus2Dest_(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] + aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] + aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] + aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] + aDataR[k];
        }
    }
    public static void ebeMinus2Dest_(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] - aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] - aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] - aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] - aDataR[k];
        }
    }
    public static void ebeMultiply2Dest_(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] * aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] * aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] * aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] * aDataR[k];
        }
    }
    public static void ebeDiv2Dest_(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] / aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] / aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] / aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] / aDataR[k];
        }
    }
    public static void ebeMod2Dest_(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] % aDataR[i];
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aDataL[i] % aDataR[k];
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] % aDataR[i];
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aDataL[j] % aDataR[k];
        }
    }
    
    
    public static void mapPlus2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] + aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] + aRHS;
    }
    public static void mapMinus2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] - aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] - aRHS;
    }
    public static void mapLMinus2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS - aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS - aDataL[j];
    }
    public static void mapMultiply2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] * aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] * aRHS;
    }
    public static void mapDiv2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] / aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] / aRHS;
    }
    public static void mapLDiv2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS / aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS / aDataL[j];
    }
    public static void mapMod2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aDataL[i] % aRHS;
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aDataL[j] % aRHS;
    }
    public static void mapLMod2Dest_(double[] aDataL, int aShiftL, double aRHS, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aRHS % aDataL[i];
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aRHS % aDataL[j];
    }
    
    
    public static void ebePlus2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] += aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] += aDataR[j];
    }
    public static void ebeMinus2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] -= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] -= aDataR[j];
    }
    public static void ebeLMinus2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] - rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] - rThis[i];
    }
    public static void ebeMultiply2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] *= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] *= aDataR[j];
    }
    public static void ebeDiv2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] /= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] /= aDataR[j];
    }
    public static void ebeLDiv2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] / rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] / rThis[i];
    }
    public static void ebeMod2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] %= aDataR[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] %= aDataR[j];
    }
    public static void ebeLMod2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aDataR[i] % rThis[i];
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aDataR[j] % rThis[i];
    }
    
    
    public static void mapPlus2this_    (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] += aRHS          ;}
    public static void mapMinus2this_   (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] -= aRHS          ;}
    public static void mapLMinus2this_  (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS - rThis[i];}
    public static void mapMultiply2this_(double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] *= aRHS          ;}
    public static void mapDiv2this_     (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] /= aRHS          ;}
    public static void mapLDiv2this_    (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS / rThis[i];}
    public static void mapMod2this_     (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] %= aRHS          ;}
    public static void mapLMod2this_    (double[] rThis, int rShift, double aRHS, int aLength) {final int rEnd = aLength + rShift; for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS % rThis[i];}
    
    
    
    /** do stuff */
    public static void ebeDo2Dest_(double[] aDataL, int aShiftL, double[] aDataR, int aShiftR, double[] rDest, int rShift, int aLength, IDoubleOperator2 aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.cal(aDataL[i], aDataR[i]);
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aOpt.cal(aDataL[i], aDataR[k]);
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.cal(aDataL[j], aDataR[i]);
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aOpt.cal(aDataL[j], aDataR[k]);
        }
    }
    public static void mapDo2Dest_(double[] aDataL, int aShiftL, double[] rDest, int rShift, int aLength, IDoubleOperator1 aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.cal(aDataL[i]);
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.cal(aDataL[j]);
    }
    public static void ebeDo2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength, IDoubleOperator2 aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.cal(rThis[i], aDataR[i]);
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aOpt.cal(rThis[i], aDataR[j]);
    }
    public static void mapDo2this_(double[] rThis, int rShift, int aLength, IDoubleOperator1 aOpt) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.cal(rThis[i]);
    }
    
    public static void ebeDo2Dest_(boolean[] aDataL, int aShiftL, boolean[] aDataR, int aShiftR, boolean[] rDest, int rShift, int aLength, IBooleanOperator2 aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) {
            if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.cal(aDataL[i], aDataR[i]);
            else for (int i = rShift, k = aShiftR; i < rEnd; ++i, ++k) rDest[i] = aOpt.cal(aDataL[i], aDataR[k]);
        } else {
            if (rShift == aShiftR) for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.cal(aDataL[j], aDataR[i]);
            else for (int i = rShift, j = aShiftL, k = aShiftR; i < rEnd; ++i, ++j, ++k) rDest[i] = aOpt.cal(aDataL[j], aDataR[k]);
        }
    }
    public static void mapDo2Dest_(boolean[] aDataL, int aShiftL, boolean[] rDest, int rShift, int aLength, IBooleanOperator1 aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftL) for (int i = rShift; i < rEnd; ++i) rDest[i] = aOpt.cal(aDataL[i]);
        else for (int i = rShift, j = aShiftL; i < rEnd; ++i, ++j) rDest[i] = aOpt.cal(aDataL[j]);
    }
    public static void ebeDo2this_(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength, IBooleanOperator2 aOpt) {
        final int rEnd = aLength + rShift;
        if (rShift == aShiftR) for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.cal(rThis[i], aDataR[i]);
        else for (int i = rShift, j = aShiftR; i < rEnd; ++i, ++j) rThis[i] = aOpt.cal(rThis[i], aDataR[j]);
    }
    public static void mapDo2this_(boolean[] rThis, int rShift, int aLength, IBooleanOperator1 aOpt) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aOpt.cal(rThis[i]);
    }
    
    
    public static void mapFill2this_(double[] rThis, int rShift, double aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    
    public static void mapFill2this_(boolean[] rThis, int rShift, boolean aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2this_(boolean[] rThis, int rShift, boolean[] aDataR, int aShiftR, int aLength) {
        System.arraycopy(aDataR, aShiftR, rThis, rShift, aLength);
    }
    
    
    /** stat stuff */
    public static double sumOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rSum = 0.0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum;
    }
    public static double meanOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rSum = 0.0;
        for (int i = aShift; i < tEnd; ++i) rSum += aThis[i];
        return rSum / (double)aLength;
    }
    public static double prodOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rProd = 1.0;
        for (int i = aShift; i < tEnd; ++i) rProd *= aThis[i];
        return rProd;
    }
    public static double maxOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rMax = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (Double.isNaN(rMax) || tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static double minOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rMin = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (Double.isNaN(rMin) || tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    public static double statOfThis_(double[] aThis, int aShift, int aLength, IDoubleOperator2 aOpt) {
        final int tEnd = aLength + aShift;
        
        double rStat = Double.NaN;
        for (int i = aShift; i < tEnd; ++i) rStat = aOpt.cal(rStat, aThis[i]);
        return rStat;
    }
    
    public static void cumsum2Dest_(double[] aThis, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        double rSum = 0.0;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {rSum += aThis[i]; rDest[i] = rSum;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {rSum += aThis[j]; rDest[i] = rSum;}}
    }
    public static void cummean2Dest_(double[] aThis, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        double rSum = 0.0;
        double tNum = 0.0;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {rSum += aThis[i]; ++tNum; rDest[i] = rSum/tNum;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {rSum += aThis[j]; ++tNum; rDest[i] = rSum/tNum;}}
    }
    public static void cumprod2Dest_(double[] aThis, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        double rProd= 1.0;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {rProd *= aThis[i]; rDest[i] = rProd;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {rProd *= aThis[j]; rDest[i] = rProd;}}
    }
    public static void cummax2Dest_(double[] aThis, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        double rMax = Double.NaN;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {double tValue = aThis[i]; if (Double.isNaN(rMax) || tValue>rMax) rMax = tValue; rDest[i] = rMax;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {double tValue = aThis[j]; if (Double.isNaN(rMax) || tValue>rMax) rMax = tValue; rDest[i] = rMax;}}
    }
    public static void cummin2Dest_(double[] aThis, int aShift, double[] rDest, int rShift, int aLength) {
        final int rEnd = aLength + rShift;
        
        double rMin = Double.NaN;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {double tValue = aThis[i]; if (Double.isNaN(rMin) || tValue<rMin) rMin = tValue; rDest[i] = rMin;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {double tValue = aThis[j]; if (Double.isNaN(rMin) || tValue<rMin) rMin = tValue; rDest[i] = rMin;}}
    }
    public static void cumstat2Dest_(double[] aThis, int aShift, double[] rDest, int rShift, int aLength, IDoubleOperator2 aOpt) {
        final int rEnd = aLength + rShift;
        
        double rStat = Double.NaN;
        if (rShift == aShift) {for (int i = rShift; i < rEnd; ++i) {rStat = aOpt.cal(rStat, aThis[i]); rDest[i] = rStat;}}
        else {for (int i = rShift, j = aShift; i < rEnd; ++i, ++j) {rStat = aOpt.cal(rStat, aThis[j]); rDest[i] = rStat;}}
    }
}
