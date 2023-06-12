package com.jtool.math.operation;

import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;


/**
 * 对于内部含有 double[] 的数据的运算做专门优化，方便编译器做 SIMD 的相关优化
 * @author liqa
 */
public class ARRAY {
    private ARRAY() {}
    
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
    
    
    public static void mapFill2this_(double[] rThis, int rShift, double aRHS, int aLength) {
        final int rEnd = aLength + rShift;
        for (int i = rShift; i < rEnd; ++i) rThis[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    public static void ebeFill2this_(double[] rThis, int rShift, double[] aDataR, int aShiftR, int aLength) {
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
    public static double productOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rProduct = 1.0;
        for (int i = aShift; i < tEnd; ++i) rProduct *= aThis[i];
        return rProduct;
    }
    public static double maxOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rMax = Double.NEGATIVE_INFINITY;
        for (int i = aShift; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static double minOfThis_(double[] aThis, int aShift, int aLength) {
        final int tEnd = aLength + aShift;
        
        double rMin = Double.POSITIVE_INFINITY;
        for (int i = aShift; i < tEnd; ++i) {
            double tValue = aThis[i];
            if (tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
}
