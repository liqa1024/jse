package jse.math;

import jse.Main;
import jse.atom.IXYZ;
import jse.atom.XYZ;
import jse.code.collection.*;
import jse.code.functional.*;
import jse.code.iterator.IHasIntIterator;
import jse.math.function.Func2;
import jse.math.function.Func3;
import jse.math.vector.*;
import jse.cache.LogicalVectorCache;
import jse.parallel.ParforThreadPool;
import jse.cache.VectorCache;
import jsex.voronoi.Geometry;
import net.jafama.DoubleWrapper;
import net.jafama.FastMath;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.*;

import static jse.code.CS.ZL_MAT;


/**
 * @author liqa
 * <p> Extended mathematical methods </p>
 * <p> The method of using internal Thread Pool is not thread safe when {@code nThreads > 1} </p>
 */
@SuppressWarnings("DuplicatedCode")
public class MathEX {
    /** 数学常量以及用于运算的常量，现在都放在这里而不是 CS */
    public final static double PI = Math.PI;
    public final static double E = Math.E;
    public final static IComplexDouble i1 = new AbstractComplexDouble() {
        @Override public double real() {return 0.0;}
        @Override public double imag() {return 1.0;}
    };
    public final static double SQRT2 = Math.sqrt(2.0);
    public final static double SQRT2_INV = 1.0/SQRT2;
    public final static double SQRT3 = Math.sqrt(3.0);
    public final static double SQRT3_INV = 1.0/SQRT3;
    public final static double SQRT3DIV2 = Math.sqrt(3.0/2.0);
    
    private final static IVector SH_Alm, SH_Blm; // 不动 m 改变 l 递归公式的前系数，只计算必要的（l >= 2, m <= l-2）
    private final static IVector SH_Clm, SH_Dlm; // 不动 l 改变 m 递归公式的前系数，只计算必要的（l >= 2, m <= l-2）
    private final static IVector SH_Elm; // Ylm 相对于原始的 Plm 归一化前系数，计算所有值
    private final static IVector SH_FACTORIAL2_2L_PLUS_1; // (2l+1)!!
    private final static IVector SH_SQRT_2L; // sqrt(2l)
    private final static int SH_LARGEST_L = 1000;
    
    static {
        SH_FACTORIAL2_2L_PLUS_1 = Vectors.zeros(SH_LARGEST_L+1);
        for (int tL = 0; tL <= SH_LARGEST_L; ++tL) {
            double rFactorial2 = 1.0;
            for (int i = tL+tL-1; i > 1; i-=2) rFactorial2 *= i;
            SH_FACTORIAL2_2L_PLUS_1.set(tL, rFactorial2);
        }
        SH_SQRT_2L = Vectors.from(SH_LARGEST_L+1, l -> Fast.sqrt(l+l));
        
        final int tSize = (SH_LARGEST_L+2)*(SH_LARGEST_L+1)/2;
        SH_Alm = Vectors.NaN(tSize);
        SH_Blm = Vectors.NaN(tSize);
        SH_Clm = Vectors.NaN(tSize);
        SH_Dlm = Vectors.NaN(tSize);
        int tStart = 3;
        for (int tL = 2; tL <= SH_LARGEST_L; ++tL) {
            double tLL = tL*tL, tLmmLmm = (tL-1)*(tL-1);
            for (int tM = 0; tM < tL-1; ++tM) {
                double tMM = tM * tM;
                SH_Alm.set(tStart+tM,  Fast.sqrt((4.0*tLL - 1.0) / (tLL - tMM)));
                SH_Blm.set(tStart+tM, -Fast.sqrt((tLmmLmm - tMM) / (4.0*tLmmLmm - 1.0)));
                double tMul = 1.0 / (double)((tL+tM+1) * (tL-tM));
                SH_Clm.set(tStart+tM, -2.0 * (tM+1) * Fast.sqrt(tMul));
                SH_Dlm.set(tStart+tM, -Fast.sqrt((tL+tM+2) * (tL-tM-1) * tMul));
            }
            tStart += tL + 1;
        }
        SH_Elm = Vectors.zeros(tSize);
        tStart = 0;
        for (int tL = 0; tL <= SH_LARGEST_L; ++tL) {
            for (int tM = 0; tM <= tL; ++tM) {
                double rElm = 1.0;
                for (int i = tL+tM; i > tL-tM; --i) rElm *= i;
                rElm *= 4*PI;
                rElm = 1.0 / rElm;
                rElm *= tL+tL+1;
                rElm = Fast.sqrt(rElm);
                SH_Elm.set(tStart+tM, rElm);
            }
            tStart += tL + 1;
        }
    }
    
    
    @ApiStatus.Obsolete
    @SuppressWarnings("UnusedReturnValue")
    public static class Vec {
        /// Vector Merge
        public static double[] merge(double aFront, double aBack) {
            return new double[] {aFront, aBack};
        }
        public static double[] merge(double aFront, double[] aBack) {
            double[] tOut = new double[aBack.length+1];
            tOut[0] = aFront;
            System.arraycopy(aBack, 0, tOut, 1, aBack.length);
            return tOut;
        }
        public static double[] merge(double[] aFront, double aBack) {
            double[] tOut = new double[aFront.length+1];
            tOut[aFront.length] = aBack;
            System.arraycopy(aFront, 0, tOut, 0, aFront.length);
            return tOut;
        }
        public static double[] merge(double[] aFront, double[] aBack) {
            double[] tOut = new double[aFront.length+aBack.length];
            System.arraycopy(aFront, 0, tOut, 0, aFront.length);
            System.arraycopy(aBack, 0, tOut, aFront.length, aBack.length);
            return tOut;
        }
        
        /// Vector operations
        public static boolean[] mapDo(boolean[] aData, IBooleanUnaryOperator aOpt) {
            boolean[] tOut = new boolean[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aOpt.applyAsBoolean(aData[i]);
            return tOut;
        }
        public static boolean[] mapDo2Dest(boolean[] rDest, IBooleanUnaryOperator aOpt) {return mapDo2Dest(rDest, aOpt, 0, rDest.length);}
        public static boolean[] mapDo2Dest(boolean[] rDest, IBooleanUnaryOperator aOpt, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = aOpt.applyAsBoolean(rDest[i]);
            return rDest;
        }
        public static boolean[] ebeDo(boolean[] aData1, boolean[] aData2, IBooleanBinaryOperator aOpt) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            boolean[] tOut = new boolean[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aOpt.applyAsBoolean(aData1[i], aData2[i]);
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = aOpt.applyAsBoolean(aData1[i], false);}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = aOpt.applyAsBoolean(false, aData2[i]);}
            return tOut;
        }
        public static boolean[] ebeDo2Dest(boolean[] rDest, boolean[] aData, IBooleanBinaryOperator aOpt) {return ebeDo2Dest(rDest, aData, aOpt, 0, 0, Math.min(aData.length, rDest.length));}
        public static boolean[] ebeDo2Dest(boolean[] rDest, boolean[] aData, IBooleanBinaryOperator aOpt, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] = aOpt.applyAsBoolean(rDest[i], aData[j]); ++j;}
            return rDest;
        }
        
        public static double[] mapDo(double[] aData, DoubleUnaryOperator aOpt) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aOpt.applyAsDouble(aData[i]);
            return tOut;
        }
        public static double[] mapDo2Dest(double[] rDest, DoubleUnaryOperator aOpt) {return mapDo2Dest(rDest, aOpt, 0, rDest.length);}
        public static double[] mapDo2Dest(double[] rDest, DoubleUnaryOperator aOpt, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = aOpt.applyAsDouble(rDest[i]);
            return rDest;
        }
        public static double[] ebeDo(double[] aData1, double[] aData2, DoubleBinaryOperator aOpt) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            double[] tOut = new double[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aOpt.applyAsDouble(aData1[i], aData2[i]);
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = aOpt.applyAsDouble(aData1[i], Double.NaN);}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = aOpt.applyAsDouble(Double.NaN, aData2[i]);}
            return tOut;
        }
        public static double[] ebeDo2Dest(double[] rDest, double[] aData, DoubleBinaryOperator aOpt) {return ebeDo2Dest(rDest, aData, aOpt, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeDo2Dest(double[] rDest, double[] aData, DoubleBinaryOperator aOpt, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] = aOpt.applyAsDouble(rDest[i], aData[j]); ++j;}
            return rDest;
        }
        
        /**
         * @author liqa
         * <p> parallel support, while aData.length == aBlockSize*N + rest, and aBlockSize should be very large,
         * otherwise there will be no acceleration effect </p>
         * <p> For convenience, assume here aData1.length == aData2.length </p>
         */
        public static boolean[] parmapDo(final int aBlockSize, final boolean[] aData, final IBooleanUnaryOperator aOpt) {return parmapDo(Par.POOL, aBlockSize, aData, aOpt);}
        public static boolean[] parmapDo(ParforThreadPool aPool, final int aBlockSize, final boolean[] aData, final IBooleanUnaryOperator aOpt) {
            if (aPool.nThreads()==1) return mapDo(aData, aOpt);
            
            int tN = aData.length/aBlockSize;
            boolean[] tOut = new boolean[aData.length];
            aPool.parfor(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) tOut[j] = aOpt.applyAsBoolean(aData[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < aData.length; ++j) tOut[j] = aOpt.applyAsBoolean(aData[j]);
            return tOut;
        }
        public static boolean[] parmapDo2Dest(final int aBlockSize, final boolean[] rDest, final IBooleanUnaryOperator aOpt) {return parmapDo2Dest(Par.POOL, aBlockSize, rDest, aOpt);}
        public static boolean[] parmapDo2Dest(ParforThreadPool aPool, final int aBlockSize, final boolean[] rDest, final IBooleanUnaryOperator aOpt) {
            if (aPool.nThreads()==1) return mapDo2Dest(rDest, aOpt);
            
            int tN = rDest.length/aBlockSize;
            aPool.parfor(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) rDest[j] = aOpt.applyAsBoolean(rDest[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < rDest.length; ++j) rDest[j] = aOpt.applyAsBoolean(rDest[j]);
            return rDest;
        }
        public static double[] parebeDo(final int aBlockSize, final double[] aData1, final double[] aData2, DoubleBinaryOperator aOpt) {return parebeDo(Par.POOL, aBlockSize, aData1, aData2, aOpt);}
        public static double[] parebeDo(ParforThreadPool aPool, final int aBlockSize, final double[] aData1, final double[] aData2, DoubleBinaryOperator aOpt) {
            if (aPool.nThreads()==1) return ebeDo(aData1, aData2, aOpt);
            assert aData1.length == aData2.length;
            
            int tN = aData1.length/aBlockSize;
            final double[] tOut = new double[aData1.length];
            aPool.parfor(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) tOut[j] = aOpt.applyAsDouble(aData1[j], aData2[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < aData1.length; ++j) tOut[j] = aOpt.applyAsDouble(aData1[j], aData2[j]);
            return tOut;
        }
        public static double[] parebeDo2Dest(final int aBlockSize, final double[] rDest, final double[] aData, final DoubleBinaryOperator aOpt) {return parebeDo2Dest(Par.POOL, aBlockSize, rDest, aData, aOpt);}
        public static double[] parebeDo2Dest(ParforThreadPool aPool, final int aBlockSize, final double[] rDest, final double[] aData, final DoubleBinaryOperator aOpt) {
            if (aPool.nThreads()==1) return ebeDo2Dest(rDest, aData, aOpt);
            assert rDest.length == aData.length;
            
            int tN = aData.length/aBlockSize;
            aPool.parfor(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) rDest[j] = aOpt.applyAsDouble(rDest[j], aData[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < aData.length; ++j) rDest[j] = aOpt.applyAsDouble(rDest[j], aData[j]);
            return rDest;
        }
        
        
        /**
         * @author liqa
         * <p> Vector Logic Operations Similar to Matlab or Apache Commons Math </p>
         */
        public static boolean[] mapEqual(double[] aData, double aValue) {
            boolean[] tOut = new boolean[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] == aValue;
            return tOut;
        }
        public static boolean[] ebeEqual(double[] aData1, double[] aData2) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            boolean[] tOut = new boolean[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aData1[i] == aData2[i];
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = false;}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = false;}
            return tOut;
        }
        
        public static boolean[] mapAnd(boolean[] aData, boolean aValue) {
            boolean[] tOut = new boolean[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] && aValue;
            return tOut;
        }
        public static boolean[] mapAnd2Dest(boolean[] rDest, boolean aValue) {return mapAnd2Dest(rDest, aValue, 0, rDest.length);}
        public static boolean[] mapAnd2Dest(boolean[] rDest, boolean aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] &= aValue;
            return rDest;
        }
        public static boolean[] ebeAnd(boolean[] aData1, boolean[] aData2) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            boolean[] tOut = new boolean[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aData1[i] && aData2[i];
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = false;}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = false;}
            return tOut;
        }
        public static boolean[] ebeAnd2Dest(boolean[] rDest, boolean[] aData) {return ebeAnd2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static boolean[] ebeAnd2Dest(boolean[] rDest, boolean[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] &= aData[j]; ++j;}
            return rDest;
        }
        
        public static boolean[] mapOr(boolean[] aData, boolean aValue) {
            boolean[] tOut = new boolean[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] || aValue;
            return tOut;
        }
        public static boolean[] mapOr2Dest(boolean[] rDest, boolean aValue) {return mapOr2Dest(rDest, aValue, 0, rDest.length);}
        public static boolean[] mapOr2Dest(boolean[] rDest, boolean aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] |= aValue;
            return rDest;
        }
        public static boolean[] ebeOr(boolean[] aData1, boolean[] aData2) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            boolean[] tOut = new boolean[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aData1[i] || aData2[i];
            if (t1Bigger) {System.arraycopy(aData1, tMinSize, tOut, tMinSize, aData1.length - tMinSize);}
            else          {System.arraycopy(aData2, tMinSize, tOut, tMinSize, aData2.length - tMinSize);}
            return tOut;
        }
        public static boolean[] ebeOr2Dest(boolean[] rDest, boolean[] aData) {return ebeOr2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static boolean[] ebeOr2Dest(boolean[] rDest, boolean[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] |= aData[j]; ++j;}
            return rDest;
        }
        
        public static boolean[] mapXor(boolean[] aData, boolean aValue) {
            boolean[] tOut = new boolean[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] ^ aValue;
            return tOut;
        }
        public static boolean[] mapXor2Dest(boolean[] rDest, boolean aValue) {return mapXor2Dest(rDest, aValue, 0, rDest.length);}
        public static boolean[] mapXor2Dest(boolean[] rDest, boolean aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] ^= aValue;
            return rDest;
        }
        public static boolean[] ebeXor(boolean[] aData1, boolean[] aData2) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            boolean[] tOut = new boolean[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aData1[i] ^ aData2[i];
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = true;}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = true;}
            return tOut;
        }
        public static boolean[] ebeXor2Dest(boolean[] rDest, boolean[] aData) {return ebeXor2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static boolean[] ebeXor2Dest(boolean[] rDest, boolean[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] ^= aData[j]; ++j;}
            return rDest;
        }
        
        public static boolean[] not(boolean[] aData) {
            boolean[] tOut = new boolean[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = !aData[i];
            return tOut;
        }
        public static boolean[] not2Dest(boolean[] rDest) {return not2Dest(rDest, 0, rDest.length);}
        public static boolean[] not2Dest(boolean[] rDest, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = !rDest[i];
            return rDest;
        }
        
        public static boolean all(boolean[] aData) {
            for (boolean subData : aData) {
                if (!subData) return false;
            }
            return true;
        }
        public static boolean any(boolean[] aData) {
            for (boolean subData : aData) {
                if (subData) return true;
            }
            return false;
        }
        public static int count(boolean[] aData) {
            int tOut = 0;
            for (boolean subData : aData) if (subData) ++tOut;
            return tOut;
        }
        
        /**
         * @author liqa
         * <p> Vector Float Operations Similar to Matlab or Apache Commons Math </p>
         */
        public static double[] mapAdd(double[] aData, double aValue) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] + aValue;
            return tOut;
        }
        public static double[] mapAdd2Dest(double[] rDest, double aValue) {return mapAdd2Dest(rDest, aValue, 0, rDest.length);}
        public static double[] mapAdd2Dest(double[] rDest, double aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] += aValue;
            return rDest;
        }
        public static double[] ebeAdd(double[] aData1, double[] aData2) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            double[] tOut = new double[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aData1[i] + aData2[i];
            if (t1Bigger) {System.arraycopy(aData1, tMinSize, tOut, tMinSize, aData1.length - tMinSize);}
            else          {System.arraycopy(aData2, tMinSize, tOut, tMinSize, aData2.length - tMinSize);}
            return tOut;
        }
        public static double[] ebeAdd2Dest(double[] rDest, double[] aData) {return ebeAdd2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeAdd2Dest(double[] rDest, double[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] += aData[j]; ++j;}
            return rDest;
        }
        
        public static double[] mapMinus(double[] aData, double aValue) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] - aValue;
            return tOut;
        }
        public static double[] mapMinus2Dest(double[] rDest, double aValue) {return mapMinus2Dest(rDest, aValue, 0, rDest.length);}
        public static double[] mapMinus2Dest(double[] rDest, double aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] -= aValue;
            return rDest;
        }
        public static double[] ebeMinus(double[] aDataL, double[] aDataR) {
            boolean t1Bigger = aDataL.length > aDataR.length;
            int tMinSize = t1Bigger ? aDataR.length : aDataL.length;
            double[] tOut = new double[t1Bigger ? aDataL.length : aDataR.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aDataL[i] - aDataR[i];
            if (t1Bigger) {System.arraycopy(aDataL, tMinSize, tOut, tMinSize, aDataL.length - tMinSize);}
            else          {for (int i = tMinSize; i < aDataR.length; ++i) tOut[i] = -aDataR[i];}
            return tOut;
        }
        public static double[] ebeMinus2Dest(double[] rDest, double[] aData) {return ebeMinus2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeMinus2Dest(double[] rDest, double[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] -= aData[j]; ++j;}
            return rDest;
        }
        
        public static double[] mapLMinus(double[] aData, double aValue) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aValue - aData[i];
            return tOut;
        }
        public static double[] mapLMinus2Dest(double[] rDest, double aValue) {return mapLMinus2Dest(rDest, aValue, 0, rDest.length);}
        public static double[] mapLMinus2Dest(double[] rDest, double aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = aValue - rDest[i];
            return rDest;
        }
        public static double[] ebeLMinus2Dest(double[] rDest, double[] aData) {return ebeLMinus2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeLMinus2Dest(double[] rDest, double[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] = aData[j] - rDest[i]; ++j;}
            return rDest;
        }
        
        public static double[] opposite(double[] aData) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = -aData[i];
            return tOut;
        }
        public static double[] opposite2Dest(double[] rDest) {return opposite2Dest(rDest, 0, rDest.length);}
        public static double[] opposite2Dest(double[] rDest, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = -rDest[i];
            return rDest;
        }
        
        public static double[] mapMultiply(double[] aData, double aValue) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] * aValue;
            return tOut;
        }
        public static double[] mapMultiply2Dest(double[] rDest, double aValue) {return mapMultiply2Dest(rDest, aValue, 0, rDest.length);}
        public static double[] mapMultiply2Dest(double[] rDest, double aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] *= aValue;
            return rDest;
        }
        public static double[] ebeMultiply(double[] aData1, double[] aData2) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            double[] tOut = new double[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aData1[i] * aData2[i];
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = 0.0;}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = 0.0;}
            return tOut;
        }
        public static double[] ebeMultiply2Dest(double[] rDest, double[] aData) {return ebeMultiply2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeMultiply2Dest(double[] rDest, double[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] *= aData[j]; ++j;}
            return rDest;
        }
        
        public static double[] mapDivide(double[] aData, double aValue) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aData[i] / aValue;
            return tOut;
        }
        public static double[] mapDivide2Dest(double[] rDest, double aValue) {return mapDivide2Dest(rDest, aValue, 0, rDest.length);}
        public static double[] mapDivide2Dest(double[] rDest, double aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] /= aValue;
            return rDest;
        }
        @SuppressWarnings("divzero")
        public static double[] ebeDivide(double[] aDataL, double[] aDataR) {
            boolean t1Bigger = aDataL.length > aDataR.length;
            int tMinSize = t1Bigger ? aDataR.length : aDataL.length;
            double[] tOut = new double[t1Bigger ? aDataL.length : aDataR.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aDataL[i] / aDataR[i];
            if (t1Bigger) {for (int i = tMinSize; i < aDataL.length; ++i) tOut[i] = aDataL[i]/0.0;}
            else          {for (int i = tMinSize; i < aDataR.length; ++i) tOut[i] = 0.0/aDataR[i];}
            return tOut;
        }
        public static double[] ebeDivide2Dest(double[] rDest, double[] aData) {return ebeDivide2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeDivide2Dest(double[] rDest, double[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] /= aData[j]; ++j;}
            return rDest;
        }
        
        public static double[] mapLDivide(double[] aData, double aValue) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aValue / aData[i];
            return tOut;
        }
        public static double[] mapLDivide2Dest(double[] rDest, double aValue) {return mapLDivide2Dest(rDest, aValue, 0, rDest.length);}
        public static double[] mapLDivide2Dest(double[] rDest, double aValue, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = aValue / rDest[i];
            return rDest;
        }
        public static double[] ebeLDivide2Dest(double[] rDest, double[] aData) {return ebeLDivide2Dest(rDest, aData, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeLDivide2Dest(double[] rDest, double[] aData, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] = aData[j] / rDest[i]; ++j;}
            return rDest;
        }
        
        public static double[] inverse(double[] aData) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = 1.0/aData[i];
            return tOut;
        }
        public static double[] inverse2Dest(double[] rDest) {return inverse2Dest(rDest, 0, rDest.length);}
        public static double[] inverse2Dest(double[] rDest, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = 1.0/rDest[i];
            return rDest;
        }
        
        public static double statBy(double[] aData, DoubleBinaryOperator aOpt) {return statBy(aData, aOpt, 0, aData.length);}
        public static double statBy(double[] aData, DoubleBinaryOperator aOpt, int aBeginPos, int aLength) {
            double tOut = Double.NaN;
            for (int i = aBeginPos; i < aBeginPos+aLength; ++i) tOut = aOpt.applyAsDouble(tOut, aData[i]);
            return tOut;
        }
        public static double min(double[] aData) {
            double tOut = Double.POSITIVE_INFINITY;
            for (double subData : aData) if (subData < tOut) tOut = subData;
            return tOut;
        }
        public static double max(double[] aData) {
            double tOut = Double.NEGATIVE_INFINITY;
            for (double subData : aData) if (subData > tOut) tOut = subData;
            return tOut;
        }
        public static Pair<Double, Integer> minWithIdx(double[] aData) {
            Pair<Double, Integer> tOut = new Pair<>(Double.POSITIVE_INFINITY, -1);
            for (int i = 0; i < aData.length; ++i) if (aData[i] < tOut.mFirst) {
                tOut.mFirst = aData[i];
                tOut.mSecond = i;
            }
            return tOut;
        }
        public static Pair<Double, Integer> maxWithIdx(double[] aData) {
            Pair<Double, Integer> tOut = new Pair<>(Double.NEGATIVE_INFINITY, -1);
            for (int i = 0; i < aData.length; ++i) if (aData[i] > tOut.mFirst) {
                tOut.mFirst = aData[i];
                tOut.mSecond = i;
            }
            return tOut;
        }
        public static double sum(double[] aData) {
            double tOut = 0.0;
            for (double subData : aData) tOut += subData;
            return tOut;
        }
        public static double product(double[] aData) {
            double tOut = 1.0;
            for (double subData : aData) tOut *= subData;
            return tOut;
        }
        
        public static double[] copy(double[] aData) {
            double[] tOut = new double[aData.length];
            System.arraycopy(aData, 0, tOut, 0, aData.length);
            return tOut;
        }
    }
    
    
    /// a Parfor ThreadPool for MathEX usage
    @ApiStatus.Obsolete
    public static class Par {
        private static ParforThreadPool POOL = new ParforThreadPool(1);
        public static void setThreadNum(int aThreadNum) {POOL.shutdown(); POOL = new ParforThreadPool(aThreadNum);}
        public static void closeThreadPool() {if (POOL.nThreads() > 1) setThreadNum(1);}
        // 在程序结束时关闭 POOL
        static {Main.addGlobalAutoCloseable(Par::closeThreadPool);}
    }
    
    
    /// Methods of Graph usage
    public static class Graph {
        
        /**
         * 为了可读性以及减少重复代码，这里使用会创建临时变量 {@link XYZ} 的
         * @author CHanzy
         * @return A, B, C 三点组成的三角形的面积，永远为正数
         */
        public static double area(IXYZ aA, IXYZ aB, IXYZ aC) {
            XYZ tA = XYZ.toXYZ(aA);
            XYZ tAB = aB.minus(tA);
            XYZ tAC = aC.minus(tA);
            tAB.cross2this(tAC);
            return 0.5 * tAB.norm();
        }
        /**
         * 确定点 D 是否位于由点 A、B 和 C 定义的平面的左侧。假定从平面的右侧看，ABC 满足逆时针的顺序。
         * @return 如果在左边则为正，右边则为负，刚好在平面上则为 0
         */
        public static double leftOfPlane(IXYZ aA, IXYZ aB, IXYZ aC, IXYZ aD) {
            return Geometry.leftOfPlane(
                  aA.x(), aA.y(), aA.z()
                , aB.x(), aB.y(), aB.z()
                , aC.x(), aC.y(), aC.z()
                , aD.x(), aD.y(), aD.z());
        }
        /**
         * 确定点 E 是否位于由点 A、B、C 和 D 定义的球体的内部。假定 {@code leftOfPlane(A, B, C, D) > 0}。
         * @return 如果在内部则为正，外部则为负，刚好在球面上则为 0
         */
        public static double inSphere(IXYZ aA, IXYZ aB, IXYZ aC, IXYZ aD, IXYZ aE) {
            return Geometry.inSphere(
                  aA.x(), aA.y(), aA.z()
                , aB.x(), aB.y(), aB.z()
                , aC.x(), aC.y(), aC.z()
                , aD.x(), aD.y(), aD.z()
                , aE.x(), aE.y(), aE.z());
        }
        /**
         * 计算由点 A，B，C 和 D 定义的球的中心。假定 {@code leftOfPlane(A, B, C, D) > 0}。
         * @return 球心 XYZ 坐标
         */
        public static XYZ centerSphere(IXYZ aA, IXYZ aB, IXYZ aC, IXYZ aD) {
            XYZ rCenter = new XYZ(0.0, 0.0, 0.0);
            Geometry.centerSphere(
                  aA.x(), aA.y(), aA.z()
                , aB.x(), aB.y(), aB.z()
                , aC.x(), aC.y(), aC.z()
                , aD.x(), aD.y(), aD.z()
                , rCenter);
            return rCenter;
        }
        
        /**
         * Get all the Intersection Points of the ray between 2D box
         * @author liqa
         * @return list x y of the point, empty for no point and null for invalid input
         */
        public static double[][] interRayBox2D(double aBoxXMin, double aBoxYMin, double aBoxXMax, double aBoxYMax, double aRayXFrom, double aRayYFrom, double aRayXTo, double aRayYTo) {
            double tSizeX = aBoxXMax - aBoxXMin;
            double tSizeY = aBoxYMax - aBoxYMin;
            if (tSizeX < 0 || tSizeY < 0) return ZL_MAT;
            // 增加刚好在边界的处理，这里使用增加微扰的方法来简单处理，from 优先往盒内，to 优先往盒外
            if      (aRayXFrom == aBoxXMin) aRayXFrom += tSizeX*1.0e-12;
            else if (aRayXFrom == aBoxXMax) aRayXFrom -= tSizeX*1.0e-12;
            if      (aRayYFrom == aBoxYMin) aRayYFrom += tSizeY*1.0e-12;
            else if (aRayYFrom == aBoxYMax) aRayYFrom -= tSizeY*1.0e-12;
            if      (aRayXTo   == aBoxXMin) aRayXTo   -= tSizeX*1.0e-12;
            else if (aRayXTo   == aBoxXMax) aRayXTo   += tSizeX*1.0e-12;
            if      (aRayYTo   == aBoxYMin) aRayYTo   -= tSizeY*1.0e-12;
            else if (aRayYTo   == aBoxYMax) aRayYTo   += tSizeY*1.0e-12;
            // 获取结果
            return interRayBox2D_(aBoxXMin, aBoxYMin, aBoxXMax, aBoxYMax, aRayXFrom, aRayYFrom, aRayXTo, aRayYTo);
        }
        public static double[][] interRayBox2D_(double aBoxXMin, double aBoxYMin, double aBoxXMax, double aBoxYMax, double aRayXFrom, double aRayYFrom, double aRayXTo, double aRayYTo) {
            // 获取射线的源和终点在箱中的位置
            byte tPos1 = posBox2D(aRayXFrom, aRayYFrom, aBoxXMin, aBoxYMin, aBoxXMax, aBoxYMax);
            byte tPos2 = posBox2D(aRayXTo, aRayYTo, aBoxXMin, aBoxYMin, aBoxXMax, aBoxYMax);
            // 获取交点可能的位置的情况
            byte tInterPos = PosBox2D.INTER_POS[tPos1][tPos2];
            if (tInterPos == PosBox2D.N) return ZL_MAT;
            if (tInterPos == PosBox2D.E) return null; // 非法情况，输出 null，注意刚好在边界的情况也会输出 null
            double[] tInterPoint = _interRayBox2D_(tInterPos, aBoxXMin, aBoxYMin, aBoxXMax, aBoxYMax, aRayXFrom, aRayYFrom, aRayXTo, aRayYTo);
            // 获取另一个方向的结果
            byte tInterPosR = PosBox2D.INTER_POS[tPos2][tPos1];
            if (tInterPos == tInterPosR) return new double[][] {tInterPoint};
            double[] tInterPointR = _interRayBox2D_(tInterPosR, aBoxXMin, aBoxYMin, aBoxXMax, aBoxYMax, aRayXFrom, aRayYFrom, aRayXTo, aRayYTo);
            return new double[][] {tInterPoint, tInterPointR};
        }
        @SuppressWarnings("SuspiciousNameCombination")
        private static double[] _interRayBox2D_(byte aInterPos, double aBoxXMin, double aBoxYMin, double aBoxXMax, double aBoxYMax, double aRayXFrom, double aRayYFrom, double aRayXTo, double aRayYTo) {
            // 根据可能的位置来计算交点
            switch (aInterPos) {
            case PosBox2D.L: {
                double tY = Func.interp1(aRayXFrom, aRayXTo, aRayYFrom, aRayYTo, aBoxXMin);
                if (tY > aBoxYMax || tY < aBoxYMin) return null;
                return new double[] {aBoxXMin, tY};
            }
            case PosBox2D.R: {
                double tY = Func.interp1(aRayXFrom, aRayXTo, aRayYFrom, aRayYTo, aBoxXMax);
                if (tY > aBoxYMax || tY < aBoxYMin) return null;
                return new double[] {aBoxXMax, tY};
            }
            case PosBox2D.D: {
                double tX = Func.interp1(aRayYFrom, aRayYTo, aRayXFrom, aRayXTo, aBoxYMin);
                if (tX > aBoxXMax || tX < aBoxXMin) return null;
                return new double[] {tX, aBoxYMin};
            }
            case PosBox2D.U: {
                double tX = Func.interp1(aRayYFrom, aRayYTo, aRayXFrom, aRayXTo, aBoxYMax);
                if (tX > aBoxXMax || tX < aBoxXMin) return null;
                return new double[] {tX, aBoxYMax};
            }
            case PosBox2D.LD: {
                double tX = aBoxXMin;
                double tY = Func.interp1(aRayXFrom, aRayXTo, aRayYFrom, aRayYTo, tX);
                if (tY > aBoxYMax || tY < aBoxYMin) {
                    tY = aBoxYMin;
                    tX = Func.interp1(aRayYFrom, aRayYTo, aRayXFrom, aRayXTo, tY);
                    if (tX > aBoxXMax || tX < aBoxXMin) return null;
                }
                return new double[] {tX, tY};
            }
            case PosBox2D.LU: {
                double tX = aBoxXMin;
                double tY = Func.interp1(aRayXFrom, aRayXTo, aRayYFrom, aRayYTo, tX);
                if (tY > aBoxYMax || tY < aBoxYMin) {
                    tY = aBoxYMax;
                    tX = Func.interp1(aRayYFrom, aRayYTo, aRayXFrom, aRayXTo, tY);
                    if (tX > aBoxXMax || tX < aBoxXMin) return null;
                }
                return new double[] {tX, tY};
            }
            case PosBox2D.RD: {
                double tX = aBoxXMax;
                double tY = Func.interp1(aRayXFrom, aRayXTo, aRayYFrom, aRayYTo, tX);
                if (tY > aBoxYMax || tY < aBoxYMin) {
                    tY = aBoxYMin;
                    tX = Func.interp1(aRayYFrom, aRayYTo, aRayXFrom, aRayXTo, tY);
                    if (tX > aBoxXMax || tX < aBoxXMin) return null;
                }
                return new double[] {tX, tY};
            }
            case PosBox2D.RU: {
                double tX = aBoxXMax;
                double tY = Func.interp1(aRayXFrom, aRayXTo, aRayYFrom, aRayYTo, tX);
                if (tY > aBoxYMax || tY < aBoxYMin) {
                    tY = aBoxYMax;
                    tX = Func.interp1(aRayYFrom, aRayYTo, aRayXFrom, aRayXTo, tY);
                    if (tX > aBoxXMax || tX < aBoxXMin) return null;
                }
                return new double[] {tX, tY};
            }
            case PosBox2D.N: default: {
                // 没有的情况或者非法情况，注意刚好在边界的情况也会输出 null
                return null;
            }}
        }
        
        
        /**
         * Get the position type of input point in the 2D box
         * @author liqa
         */
        public static byte posBox2D(double aPointX, double aPointY, double aBoxXMin, double aBoxYMin, double aBoxXMax, double aBoxYMax) {
            if (aPointX < aBoxXMin) {
                if      (aPointY<aBoxYMin)                     return PosBox2D.XNYN;
                else if (aPointY>aBoxYMax)                     return PosBox2D.XNYP;
                else if (aPointY<aBoxYMax && aPointY>aBoxYMin) return PosBox2D.XNYM;
                else                                           return PosBox2D.ELSE; // edge or NaN
            } else
            if (aPointX > aBoxXMax) {
                if      (aPointY<aBoxYMin)                     return PosBox2D.XPYN;
                else if (aPointY>aBoxYMax)                     return PosBox2D.XPYP;
                else if (aPointY<aBoxYMax && aPointY>aBoxYMin) return PosBox2D.XPYM;
                else                                           return PosBox2D.ELSE; // edge or NaN
            } else
            if (aPointX < aBoxXMax && aPointX > aBoxXMin)
            {
                if      (aPointY<aBoxYMin)                     return PosBox2D.XMYN;
                else if (aPointY>aBoxYMax)                     return PosBox2D.XMYP;
                else if (aPointY<aBoxYMax && aPointY>aBoxYMin) return PosBox2D.IN;
                else                                           return PosBox2D.ELSE; // edge or NaN
            } else {
                return PosBox2D.ELSE; // edge or NaN
            }
        }
        
        private static class PosBox2D {
            public final static byte IN = 0, XNYM = 1, XPYM = 2, XMYN = 3, XMYP = 4, XNYN = 5, XNYP = 6, XPYN = 7, XPYP = 8, ELSE = 9;
            public final static byte N = 0, L = 1, R = 2, D = 3, U = 4, LD = 5, LU = 6, RD = 7, RU = 8, E = -1;
            
            /** INTER_POS[fromPos][toPos] */
            public final static byte[][] INTER_POS = new byte[][] {
                {N , L , R , D , U , LD, LU, RD, RU, E}, // IN
                {L , N , L , L , L , N , N , L , L , E}, // XNYM
                {R , R , N , R , R , R , R , N , N , E}, // XPYM
                {D , D , D , N , D , N , D , N , D , E}, // XMYN
                {U , U , U , U , N , U , N , U , N , E}, // XMYP
                {LD, N , LD, N , LD, N , N , N , LD, E}, // XNYN
                {LU, N , LU, LU, N , N , N , LU, N , E}, // XNYP
                {RD, RD, N , N , RD, N , RD, N , N , E}, // XPYN
                {RU, RU, N , RU, N , RU, N , N , N , E}, // XPYP
                {E , E , E , E , E , E , E , E , E , E}, // ELSE
            };
        }
    }
    
    
    /// Special functions
    public static class Func {
        /**
         * Linear Interpolation like in matlab
         * @author liqa
         * @param aX1 left x
         * @param aX2 right x
         * @param aF1 left f(x1)
         * @param aF2 right f(x2)
         * @param aXq query points
         * @return the Linear Interpolation result yq
         */
        public static double interp1(double aX1, double aX2, double aF1, double aF2, double aXq) {
            if (aX1 > aX2) return interp1(aX2, aX1, aF2, aF1, aXq);
            if (aXq <= aX1) return aF1;
            if (aXq >= aX2) return aF2;
            return aF1 + (aXq-aX1)/(aX2-aX1) * (aF2-aF1);
        }
        
        /**
         * 输出 Chebyshev 多项式函数，参见：
         * <a href="https://en.wikipedia.org/wiki/Chebyshev_polynomials">
         * Chebyshev polynomials </a>
         * @author liqa
         * @param aN Chebyshev 多项式阶数，有 {@code n >= 0}
         * @return 计算结果，实数
         */
        public static double chebyshev(@Range(from = 0, to = Integer.MAX_VALUE) int aN, double aX) {
            // 直接采用递推关系递归计算
            switch(aN) {
            case 0: {return 1.0;}
            case 1: {return aX;}
            default: {return 2.0 * aX * chebyshev(aN - 1, aX) - chebyshev(aN - 2, aX);}
            }
        }
        
        /**
         * 使用 <a href="https://arxiv.org/abs/1410.1748">
         * Taweetham Limpanuparb, Josh Milthorpe. 2014 </a>
         * 介绍的方法一次计算所有球谐函数，应该会快非常多
         * <p>
         * 注意原文存在许多错误在这里已经修复，并且修改了返回形式依旧为复数，可以方便使用
         * <p>
         * 一次直接计算小于等于 aLMax 的 l 以及对应的所有 m 的 Ylm 值
         * @author liqa
         * @param aLMax 球谐函数参数 l，非负整数
         * @param aTheta 球坐标下径向方向与 z 轴的角度
         * @param aPhi 球坐标下径向方向在 xy 平面投影下和 x 轴的角度
         * @return l = 0 ~ aLMax, m = -l ~ l 下所有球谐函数值组成的复向量，按照 l 从小到大排列，先遍历 m 后遍历 l
         */
        public static ComplexVector sphericalHarmonicsFull(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi) {
            ComplexVector rY = ComplexVector.zeros((aLMax+1)*(aLMax+1));
            sphericalHarmonicsFull2Dest_(aLMax, aTheta, aPhi, rY);
            return rY;
        }
        public static void sphericalHarmonicsFull2Dest(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi, IComplexVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < (aLMax+1)*(aLMax+1)) throw new IllegalArgumentException("Size of rDest MUST be GreaterOrEqual to (L+1)^2 ("+((aLMax+1)*(aLMax+1))+"), input: "+rDest.size());
            sphericalHarmonicsFull2Dest_(aLMax, aTheta, aPhi, rDest);
        }
        private static void sphericalHarmonicsFull2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi, IComplexVector rDest) {
            DoubleWrapper tJafamaDoubleWrapper = new DoubleWrapper(); // new 的损耗应该可以忽略掉
            double tSinTheta = FastMath.sinAndCos(aTheta, tJafamaDoubleWrapper);
            normalizedLegendreFull2Dest_(aLMax, tJafamaDoubleWrapper.value, tSinTheta, rDest);
            // 现在 m = 0 的情况不需要设置了
            double tSinMmmPhi = 0.0;
            double tCosMmmPhi = 1.0;
            double tSinMPhi = FastMath.sinAndCos(aPhi, tJafamaDoubleWrapper);
            double tCosMPhi = tJafamaDoubleWrapper.value;
            final double tCosPhi2 = tCosMPhi+tCosMPhi;
            for (int tM = 1; tM <= aLMax; ++tM) {
                int tStartL = tM*tM+tM;
                for (int tL = tM; tL <= aLMax; ++tL) {
                    int tIdxPos = tStartL+tM;
                    int tIdxNeg = tStartL-tM;
                    final double fCosMPhi = tCosMPhi;
                    final double fSinMPhi = tSinMPhi;
                    rDest.updateReal(tIdxPos, Plm -> fCosMPhi*Plm); rDest.updateImag(tIdxPos, Plm -> fSinMPhi*Plm);
                    rDest.updateReal(tIdxNeg, Plm -> fCosMPhi*Plm); rDest.updateImag(tIdxNeg, Plm -> fSinMPhi*Plm);
                    tStartL += tL+tL+2;
                }
                // 利用和差化积的递推公式来更新 tSinMPhi tCosMPhi
                double tSinMppPhi = tCosPhi2 * tSinMPhi - tSinMmmPhi;
                double tCosMppPhi = tCosPhi2 * tCosMPhi - tCosMmmPhi;
                tSinMmmPhi = tSinMPhi; tCosMmmPhi = tCosMPhi;
                tSinMPhi = tSinMppPhi; tCosMPhi = tCosMppPhi;
            }
        }
        private static void normalizedLegendreFull2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, IComplexVector rDest) {
            // 直接设置到 rY 上，可以减少缓存的使用，并且可以简化实现
            double tPll = 0.28209479177387814347403972578039; // = sqrt(1/(4*PI))
            rDest.set(0, tPll);
            if (aLMax > 0) {
                rDest.set(2, SQRT3 * aX * tPll);
                tPll *= (-SQRT3DIV2 * aY);
                setY_(rDest, 2, 1, tPll);
                int tStartL = 6, tStartLmm = 2, tStartLm2 = 0;
                int tStartAB = 3;
                for (int tL = 2; tL <= aLMax; ++tL) {
                    for (int tM = 0; tM < tL-1; ++tM) {
                        int tIdxAB = tStartAB+tM;
                        double tPlm = SH_Alm.get(tIdxAB) * (aX*rDest.getReal(tStartLmm+tM) + SH_Blm.get(tIdxAB)*rDest.getReal(tStartLm2+tM));
                        if (tM == 0) rDest.set(tStartL, tPlm);
                        else setY_(rDest, tStartL, tM, tPlm);
                    }
                    setY_(rDest, tStartL, tL-1, aX * Fast.sqrt(2.0*(tL-1) + 3.0) * tPll);
                    tPll *= (-Fast.sqrt(1.0 + 0.5/(double)tL) * aY);
                    setY_(rDest, tStartL, tL, tPll);
                    tStartLm2 = tStartLmm;
                    tStartLmm = tStartL;
                    tStartL += tL+tL+1+1;
                    tStartAB += tL+1;
                }
            }
        }
        private static void setY_(IComplexVector rY, int aIdxL0, int aM, double aValue) {
            int tIdxPos = aIdxL0+aM;
            int tIdxNeg = aIdxL0-aM;
            rY.set(tIdxPos, aValue, aValue);
            if ((aM&1)==1) {
                rY.set(tIdxNeg, -aValue, aValue);
            } else {
                rY.set(tIdxNeg, aValue, -aValue);
            }
        }
        
        
        /**
         * 参考 <a href="https://arxiv.org/abs/1410.1748">
         * Taweetham Limpanuparb, Josh Milthorpe. 2014 </a>
         * 介绍的方法一次计算所有 m 的球谐函数，应该会快非常多
         * <p>
         * 注意原文存在许多错误在这里已经修复，并且修改了返回形式依旧为复数，可以方便使用
         * <p>
         * 一次直接计算 l 对应的所有 m 的 Ylm 值
         * @author liqa
         * @param aL 球谐函数参数 l，非负整数
         * @param aTheta 球坐标下径向方向与 z 轴的角度
         * @param aPhi 球坐标下径向方向在 xy 平面投影下和 x 轴的角度
         * @return m = -l ~ l 下所有球谐函数值组成的复向量
         */
        public static ComplexVector sphericalHarmonics(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi) {
            ComplexVector rY = ComplexVector.zeros(aL+aL+1);
            sphericalHarmonics2Dest_(aL, aTheta, aPhi, rY);
            return rY;
        }
        public static void sphericalHarmonics2Dest(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi, IComplexVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < aL+aL+1) throw new IllegalArgumentException("Size of rY MUST be GreaterOrEqual to 2l+1 ("+(aL+aL+1)+"), input: "+rDest.size());
            sphericalHarmonics2Dest_(aL, aTheta, aPhi, rDest);
        }
        private static void sphericalHarmonics2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi, IComplexVector rDest) {
            DoubleWrapper tJafamaDoubleWrapper = new DoubleWrapper(); // new 的损耗应该可以忽略掉
            double tSinTheta = FastMath.sinAndCos(aTheta, tJafamaDoubleWrapper);
            normalizedLegendre2Dest_(aL, tJafamaDoubleWrapper.value, tSinTheta, rDest);
            // 现在 m = 0 的情况不需要设置了
            double tSinMmmPhi = 0.0;
            double tCosMmmPhi = 1.0;
            double tSinMPhi = FastMath.sinAndCos(aPhi, tJafamaDoubleWrapper);
            double tCosMPhi = tJafamaDoubleWrapper.value;
            final double tCosPhi2 = tCosMPhi+tCosMPhi;
            for (int tM = 1; tM <= aL; ++tM) {
                int tIdxPos =  tM+aL;
                int tIdxNeg = -tM+aL;
                final double fCosMPhi = tCosMPhi;
                final double fSinMPhi = tSinMPhi;
                rDest.updateReal(tIdxPos, Plm -> fCosMPhi*Plm); rDest.updateImag(tIdxPos, Plm -> fSinMPhi*Plm);
                rDest.updateReal(tIdxNeg, Plm -> fCosMPhi*Plm); rDest.updateImag(tIdxNeg, Plm -> fSinMPhi*Plm);
                // 利用和差化积的递推公式来更新 tSinMPhi tCosMPhi
                double tSinMppPhi = tCosPhi2 * tSinMPhi - tSinMmmPhi;
                double tCosMppPhi = tCosPhi2 * tCosMPhi - tCosMmmPhi;
                tSinMmmPhi = tSinMPhi; tCosMmmPhi = tCosMPhi;
                tSinMPhi = tSinMppPhi; tCosMPhi = tCosMppPhi;
            }
        }
        private static void normalizedLegendre2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, IComplexVector rDest) {
            // 直接设置到 rY 上，可以减少缓存的使用，并且可以简化实现
            // 对于固定 l 的，改为这种迭代顺序
            switch (aL) {
            case 0: {
                rDest.set(0, 0.28209479177387814347403972578039); // = sqrt(1/(4*PI))
                return;
            }
            case 1: {
                double tPll = 0.28209479177387814347403972578039;
                rDest.set(1, SQRT3 * aX * tPll);
                setY_(rDest, 1, 1, -SQRT3DIV2 * aY * tPll);
                return;
            }
            default: {
                int tStartCDE = (aL+1)*aL/2;
                double tPll = SH_Elm.get(tStartCDE + aL);
                tPll *= Fast.powFast(aY, aL);
                if ((aL&1)==1) tPll = -tPll;
                tPll *= SH_FACTORIAL2_2L_PLUS_1.get(aL);
                
                double tXDivY = aX/aY;
                setY_(rDest, aL, aL, tPll);
                setY_(rDest, aL, aL-1, -tXDivY * SH_SQRT_2L.get(aL) * tPll);
                
                for (int tM = aL-2; tM >= 0; --tM) {
                    int tIdxCD = tStartCDE+tM;
                    double tPlm = tXDivY*SH_Clm.get(tIdxCD)*rDest.getReal(aL+tM+1) + SH_Dlm.get(tIdxCD)*rDest.getReal(aL+tM+2);
                    if (tM == 0) rDest.set(aL, tPlm);
                    else setY_(rDest, aL, tM, tPlm);
                }
                return;
            }}
        }
        
        
        /**
         * 输出球谐（Spherical Harmonics）函数，定义同北大数理方法教材，可参考：
         * <a href="https://en.wikipedia.org/wiki/Spherical_harmonics">
         * Spherical harmonics </a> 中声学的定义
         * <p>
         * 标准的直接使用递推公式来计算的方式
         * @author liqa
         * @param aL 球谐函数参数 l，非负整数
         * @param aM 球谐函数参数 m，整数，-l ~ l
         * @param aTheta 球坐标下径向方向与 z 轴的角度
         * @param aPhi 球坐标下径向方向在 xy 平面投影下和 x 轴的角度
         * @return 球谐函数值，复数
         */
        public static ComplexDouble sphericalHarmonics(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aTheta, double aPhi) {
            // 判断输入是否合法
            if (Math.abs(aM) > aL) throw new IllegalArgumentException("Input m MUST be in range -l ~ l, input: "+aM);
            return sphericalHarmonics_(aL, aM, aTheta, aPhi);
        }
        public static void sphericalHarmonics2Dest(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aTheta, double aPhi, ISettableComplexDouble rDest) {
            // 判断输入是否合法
            if (Math.abs(aM) > aL) throw new IllegalArgumentException("Input m MUST be in range -l ~ l, input: "+aM);
            sphericalHarmonics2Dest_(aL, aM, aTheta, aPhi, rDest);
        }
        private static ComplexDouble sphericalHarmonics_(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aTheta, double aPhi) {
            ComplexDouble rY = new ComplexDouble();
            sphericalHarmonics2Dest_(aL, aM, aTheta, aPhi, rY);
            return rY;
        }
        private static void sphericalHarmonics2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aTheta, double aPhi, ISettableComplexDouble rDest) {
            if (aM < 0) {
                sphericalHarmonics2Dest_(aL, -aM, aTheta, aPhi, rDest);
                rDest.conj2this();
                if ((aM&1)==1) rDest.negative2this();
            }
            // 计算前系数（实数部分）
            double rFront = SH_Elm.get((aL+1)*aL/2 + aM);
            // 计算连带 Legendre 多项式部分
            rFront *= legendre_(aL, aM, Fast.cos(aTheta));
            // 返回结果，实部虚部分开计算
            rDest.setReal(rFront*Fast.cos(aM*aPhi));
            rDest.setImag(rFront*Fast.sin(aM*aPhi));
        }
        
        /**
         * 输出连带 Legendre 多项式函数，定义同 matlab 的 legendre，参见：
         * <a href="https://en.wikipedia.org/wiki/Associated_Legendre_polynomials">
         * Associated Legendre polynomials </a>
         * @author liqa
         * @param aL 连带 Legendre 多项式参数 l，非负整数
         * @param aM 连带 Legendre 多项式参数 m，非负整数，{@code m <= l}
         * @return 计算结果，实数
         */
        public static double legendre(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aX) {
            // 判断输入是否合法
            if (aM < 0 || aM > aL) throw new IllegalArgumentException("Input m MUST be in range 0 ~ l, input: "+aM);
            return legendre_(aL, aM, aX);
        }
        private static double legendre_(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aX) {
            // 直接采用递推关系递归计算
            int tGreater = aL - aM;
            switch(tGreater) {
            case 0: {
                if (aM == 0) return 1.0;
                double tPmm = Fast.sqrt(Fast.powFast(1.0 - aX*aX, aM));
                if ((aM&1)==1) tPmm = -tPmm;
                tPmm *= SH_FACTORIAL2_2L_PLUS_1.get(aL);
                return tPmm;
            }
            case 1: {
                return (aL+aL-1)*aX*legendre_(aL-1, aM, aX);
            }
            default: {
                return ((aL+aL-1)*aX*legendre_(aL-1, aM, aX) - (aL+aM-1)*legendre_(aL-2, aM, aX)) / (double)tGreater;
            }}
        }
        
        
        /**
         * 计算 Wigner 3-j 符号的结果，这里只考虑整数输入
         * <p>
         * Reference: <a href="https://en.wikipedia.org/wiki/3-j_symbol">
         * 3-j symbol - Wikipedia </a>
         * @author liqa
         * @return 计算结果，实数
         */
        public static double wigner3j(@Range(from = 0, to = SH_LARGEST_L) int aJ1, @Range(from = 0, to = SH_LARGEST_L) int aJ2, @Range(from = 0, to = SH_LARGEST_L) int aJ3, int aM1, int aM2, int aM3) {
            // 判断输入是否合法
            if (aM1 < 0 || aM1 > aJ1) throw new IllegalArgumentException("Input m1 MUST be in range 0 ~ j1, input: "+aM1);
            if (aM2 < 0 || aM2 > aJ2) throw new IllegalArgumentException("Input m2 MUST be in range 0 ~ j2, input: "+aM2);
            if (aM3 < 0 || aM3 > aJ3) throw new IllegalArgumentException("Input m3 MUST be in range 0 ~ j3, input: "+aM3);
            return wigner3j_(aJ1, aJ2, aJ3, aM1, aM2, aM3);
        }
        private static double wigner3j_(int aJ1, int aJ2, int aJ3, int aM1, int aM2, int aM3) {
            // 如果 mi 不满足和为 0 则返回 0
            if (aM1+aM2+aM3 != 0) return 0.0;
            
            // 先计算最后的求和，计算求和范围
            int tK = Math.max(Math.max(0, aJ2-aJ3-aM1), aJ1-aJ3+aM2);
            int tN = Math.min(Math.min(aJ1+aJ2-aJ3, aJ1-aM1), aJ2+aM2);
            // 范围不合法直接返回 0
            if (tN < tK) return 0.0;
            // 累加最后的求和
            double rBack = 0.0;
            for (int k = tK; k <= tN; ++k) {
                double tDiv = 1.0;
                tDiv *= factorial(k);
                tDiv *= factorial(aJ1+aJ2-aJ3-k);
                tDiv *= factorial(aJ1-aM1-k);
                tDiv *= factorial(aJ2+aM2-k);
                tDiv *= factorial(aJ3-aJ2+aM1+k);
                tDiv *= factorial(aJ3-aJ1-aM2+k);
                if ((k&1)==1) tDiv = -tDiv;
                rBack += (1.0/tDiv);
            }
            
            // 计算前面根式
            double rFront = 1.0;
            rFront *= factorial( aJ1+aJ2-aJ3);
            rFront *= factorial( aJ1-aJ2+aJ3);
            rFront *= factorial(-aJ1+aJ2+aJ3);
            rFront /= factorial( aJ1+aJ2+aJ3+1);
            rFront = Fast.sqrt(rFront);
            
            // 计算中间的根式
            double rMid = 1.0;
            rMid *= factorial(aJ1-aM1);
            rMid *= factorial(aJ1+aM1);
            rMid *= factorial(aJ2-aM2);
            rMid *= factorial(aJ2+aM2);
            rMid *= factorial(aJ3-aM3);
            rMid *= factorial(aJ3+aM3);
            rMid = Fast.sqrt(rMid);
            
            // 最终结果
            double tResult = rFront*rMid*rBack;
            if (((aJ1-aJ2-aM3)&1)==1) tResult = -tResult;
            return tResult;
        }
        
        /**
         * 计算阶乘，返回浮点数避免整型溢出
         * @author liqa
         * @return 1 * 2 * 3 * ... * (aN-1) * aN
         */
        public static double factorial(int aN) {
            if (aN < 0) return 0.0;
            if (aN==0 || aN==1) return 1.0;
            double rProd = 1.0;
            for (int i = 2; i <= aN; ++i) rProd *= i;
            return rProd;
        }
        
        
        
        /**
         * get the numerical laplacian of the input numerical function in PBC,
         * Δf(x_i) = (f(x_{i-1}) + f(x_{i+1}) - 2f(x_i)) / (δx^2)
         * @author liqa
         * @param aFunc2 input numerical function, can be double[], Func1, aFunc2 or aFunc3
         * @return the numerical laplacian of the function (in PBC)
         */
        @ApiStatus.Obsolete
        public static Func2 laplacian(Func2 aFunc2) {return laplacian2Dest(aFunc2, aFunc2.shell().setData(new double[aFunc2.data().length]));}
        @ApiStatus.Obsolete
        public static Func2 laplacian2Dest(Func2 aFunc2, Func2 rDest) {
            int tNx = aFunc2.Nx();
            int tNy = aFunc2.Ny();
            double tDx2 = aFunc2.dx()*aFunc2.dx();
            double tDy2 = aFunc2.dy()*aFunc2.dy();
            for (int j = 0; j < tNy; ++j) for (int i = 0; i < tNx; ++i) {
                int imm = i-1; if (imm <  0  ) imm += tNx;
                int ipp = i+1; if (ipp >= tNx) ipp -= tNx;
                int jmm = j-1; if (jmm <  0  ) jmm += tNy;
                int jpp = j+1; if (jpp >= tNy) jpp -= tNy;
                
                rDest.set(i, j, (aFunc2.get_(imm, j  ) + aFunc2.get_(ipp, j  ) - 2*aFunc2.get_(i, j)) / tDx2 +
                                (aFunc2.get_(i  , jmm) + aFunc2.get_(i  , jpp) - 2*aFunc2.get_(i, j)) / tDy2);
            }
            return rDest;
        }
        @ApiStatus.Obsolete
        public static Func3 laplacian(Func3 aFunc3) {return laplacian2Dest(aFunc3, aFunc3.shell().setData(new double[aFunc3.data().length]));}
        @ApiStatus.Obsolete
        public static Func3 laplacian2Dest(final Func3 aFunc3, final Func3 rDest) {
            final int tNx = aFunc3.Nx();
            final int tNy = aFunc3.Ny();
            final int tNz = aFunc3.Ny();
            final double tDx2 = aFunc3.dx()*aFunc3.dx();
            final double tDy2 = aFunc3.dy()*aFunc3.dy();
            final double tDz2 = aFunc3.dz()*aFunc3.dz();
            for (int k = 0; k < tNz; ++k) for (int j = 0; j < tNy; ++j) for (int i = 0; i < tNx; ++i) {
                int imm = i-1; if (imm <  0  ) imm += tNx;
                int ipp = i+1; if (ipp >= tNx) ipp -= tNx;
                int jmm = j-1; if (jmm <  0  ) jmm += tNy;
                int jpp = j+1; if (jpp >= tNy) jpp -= tNy;
                int kmm = k-1; if (kmm <  0  ) kmm += tNz;
                int kpp = k+1; if (kpp >= tNz) kpp -= tNz;
                
                rDest.set(i, j, k, (aFunc3.get_(imm, j  , k  ) + aFunc3.get_(ipp, j  , k  ) - 2*aFunc3.get_(i, j, k)) / tDx2 +
                                   (aFunc3.get_(i  , jmm, k  ) + aFunc3.get_(i  , jpp, k  ) - 2*aFunc3.get_(i, j, k)) / tDy2 +
                                   (aFunc3.get_(i  , j  , kmm) + aFunc3.get_(i  , j  , kpp) - 2*aFunc3.get_(i, j, k)) / tDz2);
            }
            return rDest;
        }
        
        /** parallel support for 3d laplacian */
        @ApiStatus.Obsolete
        public static Func3 parlaplacian2Dest(final Func3 aFunc3, final Func3 rDest) {return parlaplacian2Dest(Par.POOL, aFunc3, rDest);}
        @ApiStatus.Obsolete
        public static Func3 parlaplacian2Dest(ParforThreadPool aPool, final Func3 aFunc3, final Func3 rDest) {
            final int tNx = aFunc3.Nx();
            final int tNy = aFunc3.Ny();
            final int tNz = aFunc3.Ny();
            final double tDx2 = aFunc3.dx()*aFunc3.dx();
            final double tDy2 = aFunc3.dy()*aFunc3.dy();
            final double tDz2 = aFunc3.dz()*aFunc3.dz();
            aPool.parfor(tNz, k -> {
                int kmm = k-1; if (kmm <  0  ) kmm += tNz;
                int kpp = k+1; if (kpp >= tNz) kpp -= tNz;
                
                for (int j = 0; j < tNy; ++j) for (int i = 0; i < tNx; ++i) {
                    int imm = i-1; if (imm <  0  ) imm += tNx;
                    int ipp = i+1; if (ipp >= tNx) ipp -= tNx;
                    int jmm = j-1; if (jmm <  0  ) jmm += tNy;
                    int jpp = j+1; if (jpp >= tNy) jpp -= tNy;
                    
                    rDest.set(i, j, k, (aFunc3.get_(imm, j  , k  ) + aFunc3.get_(ipp, j  , k  ) - 2*aFunc3.get_(i, j, k)) / tDx2 +
                                       (aFunc3.get_(i  , jmm, k  ) + aFunc3.get_(i  , jpp, k  ) - 2*aFunc3.get_(i, j, k)) / tDy2 +
                                       (aFunc3.get_(i  , j  , kmm) + aFunc3.get_(i  , j  , kpp) - 2*aFunc3.get_(i, j, k)) / tDz2);
                }
            });
            return rDest;
        }
        
        
        /**
         * Get the numerical solution of the ordinary differential equation (ode):
         * y' = f(y, x). Note that y is in the front of the function f.
         * Here use the simplest Euler method (or Forward Difference Method)
         * @author liqa
         * @param aFunc2 Customized binary functions f(y, x)
         * @param aStartY the init value of y, can be an array for multivalued function
         * @param aStartX the init value of x, default is 0.0
         * @param aDx dx, the step length of x, smaller value is needed in Forward Difference Method to ensure convergence in iteration
         * @param aSteps the step number of this iteration
         * @return the array of solution y(x) in default, or the single value of y(last) = y(aStartX+aDx*aSteps) if use "odeLast"
         */
        @ApiStatus.Obsolete
        public static double[] odeEuler(DoubleBinaryOperator aFunc2, double aStartY, double aStartX, double aDx, int aSteps) {
            double[] tResult = new double[aSteps+1];
            tResult[0] = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult[i+1] = tResult[i] + aDx*aFunc2.applyAsDouble(tResult[i], tX);
                tX += aDx;
            }
            return tResult;
        }
        @ApiStatus.Obsolete
        public static double odeLastEuler(DoubleBinaryOperator aFunc2, double aStartY, double aStartX, double aDx, int aSteps) {
            double tResult = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult += aDx*aFunc2.applyAsDouble(tResult, tX);
                tX += aDx;
            }
            return tResult;
        }
        @ApiStatus.Obsolete
        public static double[][] odeEuler(IBinaryFullOperator<double[], double[], Double> aFunc2, double[] aStartY, double aStartX, final double aDx, int aSteps) {
            double[][] tResult = new double[aSteps+1][];
            tResult[0] = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult[i+1] = Vec.ebeDo(tResult[i], aFunc2.apply(tResult[i], tX), (y, fyx) -> y + aDx*fyx);
                tX += aDx;
            }
            return tResult;
        }
        /** 利用上 aDestFunc2 产生的临时变量 */
        @ApiStatus.Obsolete
        public static double[][] ode2DestEuler(IBinaryFullOperator<double[], double[], Double> aDestFunc2, double[] aStartY, double aStartX, final double aDx, int aSteps) {
            double[][] tResult = new double[aSteps+1][];
            tResult[0] = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult[i+1] = Vec.ebeDo2Dest(aDestFunc2.apply(tResult[i], tX), tResult[i], (fyx, y) -> y + aDx*fyx);
                tX += aDx;
            }
            return tResult;
        }
        @ApiStatus.Obsolete
        public static double[] odeLastEuler(IBinaryFullOperator<double[], double[], Double> aFunc2, double[] aStartY, double aStartX, final double aDx, int aSteps) {
            double[] tResult = Vec.copy(aStartY);
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                Vec.ebeDo2Dest(tResult, aFunc2.apply(tResult, tX), (y, fyx) -> y + aDx*fyx);
                tX += aDx;
            }
            return tResult;
        }
        @ApiStatus.Obsolete public static double[]   odeEuler         (DoubleBinaryOperator aFunc2, double aStartY, double aDx, int aSteps) {return odeEuler    (aFunc2, aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double     odeLastEuler     (DoubleBinaryOperator aFunc2, double aStartY, double aDx, int aSteps) {return odeLastEuler(aFunc2, aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double[][] odeEuler(IBinaryFullOperator<double[], double[], Double> aFunc2    , double[] aStartY, double aDx, int aSteps) {return odeEuler(aFunc2    , aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double[][] ode2DestEuler(IBinaryFullOperator<double[], double[], Double> aDestFunc2, double[] aStartY, double aDx, int aSteps) {return ode2DestEuler(aDestFunc2, aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double[]   odeLastEuler(IBinaryFullOperator<double[], double[], Double> aFunc2    , double[] aStartY, double aDx, int aSteps) {return odeLastEuler(aFunc2    , aStartY, 0.0, aDx, aSteps);}
    }
    
    
    /// operations in FastMath
    public static class Fast {
        public static double sqrt(double aValue) {return FastMath.sqrt(aValue);}
        public static double cbrt(double aValue) {return FastMath.cbrt(aValue);}
        public static double hypot(double aX, double aY) {return FastMath.hypot(aX, aY);}
        public static double hypot(double aX, double aY, double aZ) {return FastMath.hypot(aX, aY, aZ);}
        
        public static double exp(double aValue) {return FastMath.exp(aValue);}
        public static double log(double aValue) {return FastMath.log(aValue);}
        public static double log10(double aValue) {return FastMath.log10(aValue);}
        
        public static double pow(double aValue, double aPower) {return FastMath.pow(aValue, aPower);}
        public static double powFast(double aValue, int aPower) {return FastMath.powFast(aValue, aPower);}
        public static double pow2(double aValue) {return aValue*aValue;}
        public static double pow3(double aValue) {return aValue*aValue*aValue;}
        
        public static double sin(double aValue) {return FastMath.sin(aValue);}
        public static double cos(double aValue) {return FastMath.cos(aValue);}
        public static double tan(double aValue) {return FastMath.tan(aValue);}
        
        public static double asin(double aValue) {return FastMath.asin(aValue);}
        public static double acos(double aValue) {return FastMath.acos(aValue);}
        public static double atan(double aValue) {return FastMath.atan(aValue);}
        public static double atan2(double aY, double aX) {return FastMath.atan2(aY, aX);}
        
        public static double sinh(double aValue) {return FastMath.sinh(aValue);}
        public static double cosh(double aValue) {return FastMath.cosh(aValue);}
        public static double tanh(double aValue) {return FastMath.tanh(aValue);}
        
        public static double asinh(double aValue) {return FastMath.asinh(aValue);}
        public static double acosh(double aValue) {return FastMath.acosh(aValue);}
        public static double atanh(double aValue) {return FastMath.atanh(aValue);}
        
        public static double powQuick(double aValue, double aPower) {return FastMath.powQuick(aValue, aPower);}
        public static double sqrtQuick(double aValue) {return FastMath.sqrtQuick(aValue);}
        public static double sinQuick(double aValue) {return FastMath.sinQuick(aValue);}
        public static double cosQuick(double aValue) {return FastMath.cosQuick(aValue);}
    }
    
    
    /// advance operations
    public static class Adv {
        
        /**
         * 线性拟合得到 y = a + bx
         * @author liqa
         * @param aX x 数据组成的向量
         * @param aY y 数据组成的向量
         * @return {a, b} 组成的 pair
         */
        public static DoublePair lineFit(IVector aX, IVector aY) {
            int tNx = aX.size();
            int tNy = aY.size();
            if (tNx != tNy) throw new IllegalArgumentException("Input x, y MUST have same size, input: ("+tNx+", "+tNy+")");
            
            double tXMean = aX.mean();
            double tYMean = aY.mean();
            
            double tB = (aX.operation().dot(aY) - tNx*tXMean*tYMean) / (aX.operation().dot() - tNx*tXMean*tXMean);
            double tA = tYMean - tB*tXMean;
            
            return new DoublePair(tA, tB);
        }
        /**
         * 带有误差考虑的线性拟合得到 y = a + bx + e
         * @author liqa
         * @param aX x 数据组成的向量
         * @param aY y 数据组成的向量
         * @return {a, b, e} 组成的 triplet
         */
        public static DoubleTriplet lineFitSigma(IVector aX, IVector aY) {
            int tNx = aX.size();
            int tNy = aY.size();
            if (tNx != tNy) throw new IllegalArgumentException("Input x, y MUST have same size, input: ("+tNx+", "+tNy+")");
            
            double tXMean = aX.mean();
            double tYMean = aY.mean();
            
            double tB = (aX.operation().dot(aY) - tNx*tXMean*tYMean) / (aX.operation().dot() - tNx*tXMean*tXMean);
            double tA = tYMean - tB*tXMean;
            
            IVector rBuffer = VectorCache.getVec(tNx);
            rBuffer.fill(aX);
            rBuffer.multiply2this(tB);
            rBuffer.plus2this(tA);
            rBuffer.minus2this(aY);
            double tSSR = rBuffer.operation().dot();
            VectorCache.returnVec(rBuffer);
            
            double tSigma = Fast.sqrt(tSSR / (tNx-2.0));
            
            return new DoubleTriplet(tA, tB, tSigma);
        }
        
        
        /**
         * General method to get clusters by using Breadth-First Search
         * @author liqa
         * @param aSize number of the total points, aSize == max(points) + 1
         * @param aPoints all points should be considered
         * @param aNeighborListGetter get the neighbor list of the giving point
         * @return list of cluster
         */
        public static List<IntVector> getClustersBFS(int aSize, IHasIntIterator aPoints, final IListGetter<? extends IHasIntIterator> aNeighborListGetter) {
            final List<IntVector> rClusters = new ArrayList<>();
            final ILogicalVector tVisited = LogicalVectorCache.getZeros(aSize);
            
            final IntDeque tQueue = new IntDeque();
            aPoints.forEach(point -> {
                if (!tVisited.get(point)) {
                    IntVector.Builder subCluster = IntVector.builder();
//                  tQueue.clear(); // 由于后面会遍历移除，因此此时 tQueue 永远为空
                    
                    tQueue.addLast(point);
                    tVisited.set(point, true);
                    
                    while (!tQueue.isEmpty()) {
                        int currentPoint = tQueue.removeFirst();
                        subCluster.add(currentPoint);
                        
                        aNeighborListGetter.get(currentPoint).forEach(neighbor -> {
                            if (!tVisited.get(neighbor)) {
                                tQueue.addLast(neighbor);
                                tVisited.set(neighbor, true);
                            }
                        });
                    }
                    rClusters.add(subCluster.build());
                }
            });
            
            LogicalVectorCache.returnVec(tVisited);
            return rClusters;
        }
        
        /**
         * General method to get clusters by using Depth-First Search
         * @author liqa
         * @param aSize number of the total points, aSize == max(points) + 1
         * @param aPoints all points should be considered
         * @param aNeighborListGetter get the neighbor list of the giving point
         * @return list of cluster
         */
        public static List<IntVector> getClustersDFS(int aSize, IHasIntIterator aPoints, final IListGetter<? extends IHasIntIterator> aNeighborListGetter) {
            final List<IntVector> rClusters = new ArrayList<>();
            final ILogicalVector tVisited = LogicalVectorCache.getZeros(aSize);
            
            final IntDeque tStack = new IntDeque();
            aPoints.forEach(point -> {
                if (!tVisited.get(point)) {
                    IntVector.Builder subCluster = IntVector.builder();
//                  tStack.clear(); // 由于后面会遍历移除，因此此时 tStack 永远为空
                    
                    tStack.push(point);
                    tVisited.set(point, true);
                    
                    while (!tStack.isEmpty()) {
                        int currentPoint = tStack.pop();
                        subCluster.add(currentPoint);
                        
                        aNeighborListGetter.get(currentPoint).forEach(neighbor -> {
                            if (!tVisited.get(neighbor)) {
                                tStack.push(neighbor);
                                tVisited.set(neighbor, true);
                            }
                        });
                    }
                    rClusters.add(subCluster.build());
                }
            });
            
            LogicalVectorCache.returnVec(tVisited);
            return rClusters;
        }
    }
    
    
    /// utils operations
    public static class Code {
        public static double floor(double aValue) {return FastMath.floor(aValue);}
        public static double ceil(double aValue) {return FastMath.ceil(aValue);}
        public static long round(double aValue) {return FastMath.round(aValue);}
        
        public static double toRange(double aMin, double aMax, double aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        public static int toRange(int aMin, int aMax, int aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        public static long toRange(long aMin, long aMax, long aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        
        /** double compare */
        public static boolean numericEqual(double aLHS, double aRHS) {
            double tNorm = Math.abs(aLHS) + Math.abs(aRHS);
            if (tNorm < Double.MIN_NORMAL * EPS_MUL) return true; // 两个值都为零的情况，比这个值更小时乘以 epsilon() 会失效
            double tDiff = Math.abs(aLHS - aRHS);
            return tDiff <= tNorm * DBL_EPSILON * EPS_MUL;
        }
        public static boolean numericGreater(double aLHS, double aRHS) {
            double tNorm = Math.abs(aLHS) + Math.abs(aRHS);
            if (tNorm < Double.MIN_NORMAL * EPS_MUL) return false; // 两个值都为零的情况，比这个值更小时乘以 epsilon() 会失效
            return aLHS - aRHS > tNorm * DBL_EPSILON * EPS_MUL;
        }
        public static boolean numericLess(double aLHS, double aRHS) {
            double tNorm = Math.abs(aLHS) + Math.abs(aRHS);
            if (tNorm < Double.MIN_NORMAL * EPS_MUL) return false; // 两个值都为零的情况，比这个值更小时乘以 epsilon() 会失效
            return aRHS - aLHS > tNorm * DBL_EPSILON * EPS_MUL;
        }
        public final static int EPS_MUL = 8;
        /** {@link FastMath} will has lower accuracy */
        public final static double DBL_EPSILON = 1.0e-12;
        
        
        /** Translates Amount of aUnit1 to Amount of aUnit2. */
        public static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
            if (aTargetUnit == 0) return 0;
            if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
            if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
            if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
            return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
        }
        public static int units(int aAmount, int aOriginalUnit, int aTargetUnit, boolean aRoundUp) {
            if (aTargetUnit == 0) return 0;
            if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
            if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
            if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
            return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
        }
        
        
        /**
         * Divides but rounds up.
         */
        public static long divup(long aNumber, long aDivider) {return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);}
        public static int  divup(int  aNumber, int  aDivider) {return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);}
        
        /**
         * get the next power of 2 of aNum
         * @author liqa
         */
        public static int ceilPower2(int aNum) {
            --aNum;
            aNum |= aNum >> 1;
            aNum |= aNum >> 2;
            aNum |= aNum >> 4;
            aNum |= aNum >> 8;
            aNum |= aNum >> 16;
            ++aNum;
            return aNum;
        }
        /**
         * get the previous power of 2 of aNum
         * @author liqa
         */
        public static int floorPower2(int aNum) {
            aNum |= aNum >> 1;
            aNum |= aNum >> 2;
            aNum |= aNum >> 4;
            aNum |= aNum >> 8;
            aNum |= aNum >> 16;
            aNum = (aNum + 1) >> 1;
            return aNum;
        }
        /**
         * get the next power of aRoot of aNum, use the ceil value of power consistently
         * @author liqa
         */
        public static int ceilPower(int aNum, double aRoot) {
            if (aNum <= 1) return aNum;
            if (aRoot == 2.0) return ceilPower2(aNum);
            double tValue = 1.0;
            int out = 1;
            while (out < aNum) {
                tValue *= aRoot;
                out = (int) Code.ceil(tValue);
            }
            return out;
        }
        /**
         * get the previous power of aRoot of aNum, use the ceil value of power consistently
         * @author liqa
         */
        public static int floorPower(int aNum, double aRoot) {
            if (aNum <= 1) return aNum;
            if (aRoot == 2.0) return floorPower2(aNum);
            double tValue = 1.0;
            int out = 1;
            while (true) {
                tValue *= aRoot;
                int tOut = (int) Code.ceil(tValue);
                if (tOut > aNum) return out;
                out = tOut;
            }
        }
    }
}
