package com.guan.math;

import com.guan.atom.MonatomicParameterCalculator;
import com.guan.code.Pair;
import com.guan.math.functional.IOperator1;
import com.guan.math.functional.IOperator2;
import com.guan.math.functional.IOperator2Full;
import com.guan.math.numerical.Func1;
import com.guan.math.numerical.Func2;
import com.guan.math.numerical.Func3;
import com.guan.parallel.ParforThreadPool;
import net.jafama.FastMath;

import java.util.Arrays;


/**
 * @author liqa
 * <p> Extended mathematical methods </p>
 * <p> The method of using internal Thread Pool is not thread safe when nThreads > 1 </p>
 */
@SuppressWarnings("DuplicatedCode")
public class MathEX {
    
    public static final double PI = Math.PI;
    
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
        public static boolean[] mapDo(boolean[] aData, IOperator1<Boolean> aOpt) {
            boolean[] tOut = new boolean[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aOpt.cal(aData[i]);
            return tOut;
        }
        public static boolean[] mapDo2Dest(boolean[] rDest, IOperator1<Boolean> aOpt) {return mapDo2Dest(rDest, aOpt, 0, rDest.length);}
        public static boolean[] mapDo2Dest(boolean[] rDest, IOperator1<Boolean> aOpt, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = aOpt.cal(rDest[i]);
            return rDest;
        }
        public static boolean[] ebeDo(boolean[] aData1, boolean[] aData2, IOperator2<Boolean> aOpt) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            boolean[] tOut = new boolean[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aOpt.cal(aData1[i], aData2[i]);
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = aOpt.cal(aData1[i], null);}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = aOpt.cal(null, aData2[i]);}
            return tOut;
        }
        public static boolean[] ebeDo2Dest(boolean[] rDest, boolean[] aData, IOperator2<Boolean> aOpt) {return ebeDo2Dest(rDest, aData, aOpt, 0, 0, Math.min(aData.length, rDest.length));}
        public static boolean[] ebeDo2Dest(boolean[] rDest, boolean[] aData, IOperator2<Boolean> aOpt, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] = aOpt.cal(rDest[i], aData[j]); ++j;}
            return rDest;
        }
        
        public static double[] mapDo(double[] aData, IOperator1<Double> aOpt) {
            double[] tOut = new double[aData.length];
            for (int i = 0; i < aData.length; ++i) tOut[i] = aOpt.cal(aData[i]);
            return tOut;
        }
        public static double[] mapDo2Dest(double[] rDest, IOperator1<Double> aOpt) {return mapDo2Dest(rDest, aOpt, 0, rDest.length);}
        public static double[] mapDo2Dest(double[] rDest, IOperator1<Double> aOpt, int aDestPos, int aLength) {
            for (int i = aDestPos; i < aDestPos+aLength; ++i) rDest[i] = aOpt.cal(rDest[i]);
            return rDest;
        }
        public static double[] ebeDo(double[] aData1, double[] aData2, IOperator2<Double> aOpt) {
            boolean t1Bigger = aData1.length > aData2.length;
            int tMinSize = t1Bigger ? aData2.length : aData1.length;
            double[] tOut = new double[t1Bigger ? aData1.length : aData2.length];
            for (int i = 0; i < tMinSize; ++i) tOut[i] = aOpt.cal(aData1[i], aData2[i]);
            if (t1Bigger) {for (int i = tMinSize; i < aData1.length; ++i) tOut[i] = aOpt.cal(aData1[i], null);}
            else          {for (int i = tMinSize; i < aData2.length; ++i) tOut[i] = aOpt.cal(null, aData2[i]);}
            return tOut;
        }
        public static double[] ebeDo2Dest(double[] rDest, double[] aData, IOperator2<Double> aOpt) {return ebeDo2Dest(rDest, aData, aOpt, 0, 0, Math.min(aData.length, rDest.length));}
        public static double[] ebeDo2Dest(double[] rDest, double[] aData, IOperator2<Double> aOpt, int aDestPos, int aDataPos, int aLength) {
            int j = aDataPos;
            for (int i = aDestPos; i < aDestPos+aLength; ++i) {rDest[i] = aOpt.cal(rDest[i], aData[j]); ++j;}
            return rDest;
        }
        
        /**
         * @author liqa
         * <p> parallel support, while aData.length == aBlockSize*N + rest, and aBlockSize should be very large,
         * otherwise there will be no acceleration effect </p>
         * <p> For convenience, assume here aData1.length == aData2.length </p>
         */
        public static boolean[] parmapDo(final int aBlockSize, final boolean[] aData, final IOperator1<Boolean> aOpt) {return parmapDo(Par.POOL, aBlockSize, aData, aOpt);}
        public static boolean[] parmapDo(ParforThreadPool aPool, final int aBlockSize, final boolean[] aData, final IOperator1<Boolean> aOpt) {
            if (aPool.nThreads()==1) return mapDo(aData, aOpt);
            
            int tN = aData.length/aBlockSize;
            boolean[] tOut = new boolean[aData.length];
            aPool.parfor_(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) tOut[j] = aOpt.cal(aData[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < aData.length; ++j) tOut[j] = aOpt.cal(aData[j]);
            return tOut;
        }
        public static boolean[] parmapDo2Dest(final int aBlockSize, final boolean[] rDest, final IOperator1<Boolean> aOpt) {return parmapDo2Dest(Par.POOL, aBlockSize, rDest, aOpt);}
        public static boolean[] parmapDo2Dest(ParforThreadPool aPool, final int aBlockSize, final boolean[] rDest, final IOperator1<Boolean> aOpt) {
            if (aPool.nThreads()==1) return mapDo2Dest(rDest, aOpt);
            
            int tN = rDest.length/aBlockSize;
            aPool.parfor_(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) rDest[j] = aOpt.cal(rDest[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < rDest.length; ++j) rDest[j] = aOpt.cal(rDest[j]);
            return rDest;
        }
        public static double[] parebeDo(final int aBlockSize, final double[] aData1, final double[] aData2, IOperator2<Double> aOpt) {return parebeDo(Par.POOL, aBlockSize, aData1, aData2, aOpt);}
        public static double[] parebeDo(ParforThreadPool aPool, final int aBlockSize, final double[] aData1, final double[] aData2, IOperator2<Double> aOpt) {
            if (aPool.nThreads()==1) return ebeDo(aData1, aData2, aOpt);
            assert aData1.length == aData2.length;
            
            int tN = aData1.length/aBlockSize;
            final double[] tOut = new double[aData1.length];
            aPool.parfor_(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) tOut[j] = aOpt.cal(aData1[j], aData2[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < aData1.length; ++j) tOut[j] = aOpt.cal(aData1[j], aData2[j]);
            return tOut;
        }
        public static double[] parebeDo2Dest(final int aBlockSize, final double[] rDest, final double[] aData, final IOperator2<Double> aOpt) {return parebeDo2Dest(Par.POOL, aBlockSize, rDest, aData, aOpt);}
        public static double[] parebeDo2Dest(ParforThreadPool aPool, final int aBlockSize, final double[] rDest, final double[] aData, final IOperator2<Double> aOpt) {
            if (aPool.nThreads()==1) return ebeDo2Dest(rDest, aData, aOpt);
            assert rDest.length == aData.length;
            
            int tN = aData.length/aBlockSize;
            aPool.parfor_(tN, i -> {
                int tStart = i*aBlockSize, tEnd = tStart+aBlockSize;
                for (int j = tStart; j < tEnd; ++j) rDest[j] = aOpt.cal(rDest[j], aData[j]);
            });
            int tParEnd = tN*aBlockSize;
            for (int j = tParEnd; j < aData.length; ++j) rDest[j] = aOpt.cal(rDest[j], aData[j]);
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
        
        public static double statBy(double[] aData, IOperator2<Double> aOpt) {return statBy(aData, aOpt, 0, aData.length);}
        public static double statBy(double[] aData, IOperator2<Double> aOpt, int aBeginPos, int aLength) {
            double tOut = Double.NaN;
            for (int i = aBeginPos; i < aBeginPos+aLength; ++i) tOut = aOpt.cal(tOut, aData[i]);
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
            for (int i = 0; i < aData.length; ++i) if (aData[i] < tOut.first) {
                tOut.first = aData[i];
                tOut.second = i;
            }
            return tOut;
        }
        public static Pair<Double, Integer> maxWithIdx(double[] aData) {
            Pair<Double, Integer> tOut = new Pair<>(Double.NEGATIVE_INFINITY, -1);
            for (int i = 0; i < aData.length; ++i) if (aData[i] > tOut.first) {
                tOut.first = aData[i];
                tOut.second = i;
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
    
    
    public static class Mat {
        /// Matrix operations
        public static double sum(double[][] aMatrix) {
            double tOut = 0.0;
            for (double[] tRows : aMatrix) for (double tData : tRows) tOut += tData;
            return tOut;
        }
        public static double[] sum(double[][] aMatrix, int aDimension) {
            if (aDimension == 0) {
                double[] tOut = new double[aMatrix[0].length];
                for (double[] tRows : aMatrix) for (int i = 0; i < tOut.length; ++i) tOut[i] += tRows[i];
                return tOut;
            } else
            if (aDimension == 1) {
                double[] tOut = new double[aMatrix.length];
                for (int i = 0; i < tOut.length; ++i) for (double tData : aMatrix[i]) tOut[i] += tData;
                return tOut;
            } else {
                throw new RuntimeException("Invalid Dimension: "+aDimension);
            }
        }
        public static double[] sum2Dest(double[][] rDest) {
            double[] tOut = rDest[0];
            for (int i = 1; i < rDest.length; ++i) {
                double[] tRows = rDest[i];
                for (int j = 0; j < tOut.length; ++j) tOut[j] += tRows[j];
            }
            return tOut;
        }
        
        public static double[][] transpose(double[][] aMatrix) {
            int oRowNum = aMatrix.length;
            int oColNum = aMatrix[0].length;
            double[][] tTransposed = new double[oColNum][oRowNum];
            for (int i = 0; i < oRowNum; ++i) {
                double[] tRows = aMatrix[i];
                for (int j = 0; j < oColNum; ++j) tTransposed[j][i] = tRows[j];
            }
            return tTransposed;
        }
        
        public static double[][] copy(double[][] aMatrix) {
            double[][] tOut = new double[aMatrix.length][aMatrix[0].length];
            for (int i = 0; i < aMatrix.length; ++i) System.arraycopy(aMatrix[i], 0, tOut[i], 0, aMatrix[i].length);
            return tOut;
        }
        
        
        /// Matrix Slice
        private enum SliceType {
              ALL
            , NONE
        }
        public final static SliceType ALL = SliceType.ALL;
        public final static double[][] ZL_MAT = new double[0][];
        public final static double[]   ZL_VEC = new double[0];
        /**
         * @author liqa
         * <p> Vector Slice Similar to Matlab </p>
         */
        @FunctionalInterface public interface IRowFilter {boolean accept(final double[] aRow);}
        @FunctionalInterface public interface IColFilter {boolean accept(final RealMatrixColumn aCol);}
        private static boolean[] filter2bool(double[][] aMatrix, IRowFilter aSelectedRows) {
            boolean[] tSelectedRows = new boolean[aMatrix.length];
            for (int i = 0; i < tSelectedRows.length; ++i) tSelectedRows[i] = aSelectedRows.accept(aMatrix[i]);
            return tSelectedRows;
        }
        private static boolean[] filter2bool(double[][] aMatrix, IColFilter aSelectedColumns) {
            boolean[] tSelectedColumns = new boolean[aMatrix[0].length];
            for (int i = 0; i < tSelectedColumns.length; ++i) tSelectedColumns[i] = aSelectedColumns.accept(new RealMatrixColumn(aMatrix, i));
            return tSelectedColumns;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, SliceType  aSelectedRows, IColFilter aSelectedColumns) {return getSubMatrix(aMatrix, aSelectedRows, filter2bool(aMatrix, aSelectedColumns));}
        public static double[][] getSubMatrix(double[][] aMatrix, boolean[]  aSelectedRows, IColFilter aSelectedColumns) {return getSubMatrix(aMatrix, aSelectedRows, filter2bool(aMatrix, aSelectedColumns));}
        public static double[][] getSubMatrix(double[][] aMatrix, int[]      aSelectedRows, IColFilter aSelectedColumns) {return getSubMatrix(aMatrix, aSelectedRows, filter2bool(aMatrix, aSelectedColumns));}
        public static double[][] getSubMatrix(double[][] aMatrix, IRowFilter aSelectedRows, SliceType  aSelectedColumns) {return getSubMatrix(aMatrix, filter2bool(aMatrix, aSelectedRows), aSelectedColumns);}
        public static double[][] getSubMatrix(double[][] aMatrix, IRowFilter aSelectedRows, boolean[]  aSelectedColumns) {return getSubMatrix(aMatrix, filter2bool(aMatrix, aSelectedRows), aSelectedColumns);}
        public static double[][] getSubMatrix(double[][] aMatrix, IRowFilter aSelectedRows, int[]      aSelectedColumns) {return getSubMatrix(aMatrix, filter2bool(aMatrix, aSelectedRows), aSelectedColumns);}
        public static double[][] getSubMatrix(double[][] aMatrix, IRowFilter aSelectedRows, IColFilter aSelectedColumns) {return getSubMatrix(aMatrix, filter2bool(aMatrix, aSelectedRows), filter2bool(aMatrix, aSelectedColumns));}
        public static double[][] getSubMatrix(double[][] aMatrix, SliceType aSelectedRows, SliceType aSelectedColumns) {
            if (aSelectedRows != ALL) return ZL_MAT;
            int subRowNum = aMatrix.length;
            if (aSelectedColumns != ALL) return new double[subRowNum][0];
            int subColNum = aMatrix[0].length;
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            for (int subRow = 0; subRow < subColNum; ++subRow) {
                System.arraycopy(aMatrix[subRow], 0, subMatrix[subRow], 0, subColNum);
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, SliceType aSelectedRows, boolean[] aSelectedColumns) {
            if (aSelectedRows != ALL) return ZL_MAT;
            int subRowNum = aMatrix.length;
            int tColNum = aMatrix[0].length;
            int subColNum = Math.min(Vec.count(aSelectedColumns), tColNum);
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            for (int subRow = 0; subRow < subRowNum; ++subRow) {
                double[] tRows = aMatrix[subRow];
                double[] subRows = subMatrix[subRow];
                
                int subCol = 0;
                for (int col = 0; col < tColNum; ++col) if (aSelectedColumns[col]) {
                    subRows[subCol] = tRows[col];
                    ++subCol;
                }
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, boolean[] aSelectedRows, SliceType aSelectedColumns) {
            int tRowNum = aMatrix.length;
            int subRowNum = Math.min(Vec.count(aSelectedRows), tRowNum);
            if (aSelectedColumns != ALL) return new double[subRowNum][0];
            int subColNum = aMatrix[0].length;
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            int subRow = 0;
            for (int row = 0; row < tRowNum; ++row) if (aSelectedRows[row]) {
                double[] tRows = aMatrix[row];
                double[] subRows = subMatrix[subRow];
                ++subRow;
                
                System.arraycopy(tRows, 0, subRows, 0, subColNum);
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, boolean[] aSelectedRows, boolean[] aSelectedColumns) {
            int tRowNum = aMatrix.length;
            int tColNum = aMatrix[0].length;
            int subRowNum = Math.min(Vec.count(aSelectedRows   ), tRowNum);
            int subColNum = Math.min(Vec.count(aSelectedColumns), tColNum);
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            int subRow = 0;
            for (int row = 0; row < tRowNum; ++row) if (aSelectedRows[row]) {
                double[] tRows = aMatrix[row];
                double[] subRows = subMatrix[subRow];
                ++subRow;
                
                int subCol = 0;
                for (int col = 0; col < tColNum; ++col) if (aSelectedColumns[col]) {
                    subRows[subCol] = tRows[col];
                    ++subCol;
                }
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, SliceType aSelectedRows, int[] aSelectedColumns) {
            if (aSelectedRows != ALL) return ZL_MAT;
            int subRowNum = aMatrix.length;
            int subColNum = Math.min(aSelectedColumns.length, aMatrix[0].length);
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            for (int subRow = 0; subRow < subRowNum; ++subRow) {
                double[] tRows = aMatrix[subRow];
                double[] subRows = subMatrix[subRow];
                
                for (int subCol = 0; subCol < subColNum; ++subCol) subRows[subCol] = tRows[aSelectedColumns[subCol]];
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, int[] aSelectedRows, SliceType aSelectedColumns) {
            int subRowNum = Math.min(aSelectedRows.length, aMatrix.length);
            if (aSelectedColumns != ALL) return new double[subRowNum][0];
            int subColNum = aMatrix[0].length;
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            for (int subRow = 0; subRow < subRowNum; ++subRow) {
                double[] tRows = aMatrix[aSelectedRows[subRow]];
                double[] subRows = subMatrix[subRow];
                
                System.arraycopy(tRows, 0, subRows, 0, subColNum);
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, boolean[] aSelectedRows, int[] aSelectedColumns) {
            int tRowNum = aMatrix.length;
            int subRowNum = Math.min(Vec.count(aSelectedRows), tRowNum);
            int subColNum = Math.min(aSelectedColumns.length, aMatrix[0].length);
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            int subRow = 0;
            for (int row = 0; row < tRowNum; ++row) if (aSelectedRows[row]) {
                double[] tRows = aMatrix[row];
                double[] subRows = subMatrix[subRow];
                ++subRow;
                
                for (int subCol = 0; subCol < subColNum; ++subCol) subRows[subCol] = tRows[aSelectedColumns[subCol]];
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, int[] aSelectedRows, boolean[] aSelectedColumns) {
            int subRowNum = Math.min(aSelectedRows   .length, aMatrix   .length);
            int tColNum = aMatrix[0].length;
            int subColNum = Math.min(Vec.count(aSelectedColumns), tColNum);
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            for (int subRow = 0; subRow < subRowNum; ++subRow) {
                double[] tRows = aMatrix[aSelectedRows[subRow]];
                double[] subRows = subMatrix[subRow];
                
                int subCol = 0;
                for (int col = 0; col < tColNum; ++col) if (aSelectedColumns[col]) {
                    subRows[subCol] = tRows[col];
                    ++subCol;
                }
            }
            return subMatrix;
        }
        public static double[][] getSubMatrix(double[][] aMatrix, int[] aSelectedRows, int[] aSelectedColumns) {
            int subRowNum = Math.min(aSelectedRows   .length, aMatrix   .length);
            int subColNum = Math.min(aSelectedColumns.length, aMatrix[0].length);
            
            double[][] subMatrix = new double[subRowNum][subColNum];
            
            for (int subRow = 0; subRow < subRowNum; ++subRow) {
                double[] tRows = aMatrix[aSelectedRows[subRow]];
                double[] subRows = subMatrix[subRow];
                
                for (int subCol = 0; subCol < subColNum; ++subCol) subRows[subCol] = tRows[aSelectedColumns[subCol]];
            }
            return subMatrix;
        }
        
        public static double[] getColumn(double[][] aMatrix, int aSelectedColumn) {return getColumn(aMatrix, ALL, aSelectedColumn);}
        public static double[] getColumn(double[][] aMatrix, SliceType aSelectedRows, int aSelectedColumn) {
            if (aSelectedRows != ALL) return ZL_VEC;
            int subRowNum = aMatrix.length;
            
            double[] subColumn = new double[subRowNum];
            
            for (int subRow = 0; subRow < subRowNum; ++subRow) subColumn[subRow] = aMatrix[subRow][aSelectedColumn];
            return subColumn;
        }
        public static double[] getColumn(double[][] aMatrix, boolean[] aSelectedRows, int aSelectedColumn) {
            int tRowNum = aMatrix.length;
            int subRowNum = Math.min(Vec.count(aSelectedRows), tRowNum);
            
            double[] subColumn = new double[subRowNum];
            
            int subRow = 0;
            for (int row = 0; row < tRowNum; ++row) if (aSelectedRows[row]) {
                subColumn[subRow] = aMatrix[row][aSelectedColumn];
                ++subRow;
            }
            return subColumn;
        }
        public static double[] getColumn(double[][] aMatrix, int[] aSelectedRows, int aSelectedColumn) {
            int subRowNum = Math.min(aSelectedRows.length, aMatrix.length);
            
            double[] subColumn = new double[subRowNum];
            
            for (int subRow = 0; subRow < subRowNum; ++subRow) subColumn[subRow] = aMatrix[aSelectedRows[subRow]][aSelectedColumn];
            return subColumn;
        }
        public static double[] getRow(double[][] aMatrix, int aSelectedRow) {return getRow(aMatrix, aSelectedRow, ALL);}
        public static double[] getRow(double[][] aMatrix, int aSelectedRow, SliceType aSelectedColumns) {
            if (aSelectedColumns != ALL) return ZL_VEC;
            int subColNum = aMatrix[0].length;
            
            double[] subRow = new double[subColNum];
            
            double[] tRows = aMatrix[aSelectedRow];
            System.arraycopy(tRows, 0, subRow, 0, subColNum);
            return subRow;
        }
        public static double[] getRow(double[][] aMatrix, int aSelectedRow, boolean[] aSelectedColumns) {
            int tColNum = aMatrix[0].length;
            int subColNum = Math.min(Vec.count(aSelectedColumns), tColNum);
            
            double[] subRow = new double[subColNum];
            
            double[] tRows = aMatrix[aSelectedRow];
            int subCol = 0;
            for (int col = 0; col < tColNum; ++col) if (aSelectedColumns[col]) {
                subRow[subCol] = tRows[col];
                ++subCol;
            }
            return subRow;
        }
        public static double[] getRow(double[][] aMatrix, int aSelectedRow, int[] aSelectedColumns) {
            int subColNum = Math.min(aSelectedColumns.length, aMatrix[0].length);
            
            double[] subRow = new double[subColNum];
            
            double[] tRows = aMatrix[aSelectedRow];
            
            for (int subCol = 0; subCol < subColNum; ++subCol) subRow[subCol] = tRows[aSelectedColumns[subCol]];
            return subRow;
        }
    }
    
    public static class XYZ {
        /**
         * Get distance of two double[3] with no objects creation in the heap
         * @author liqa
         * @param aXYZ1 fist double[3]
         * @param aXYZ2 second double[3]
         * @return the distance of aXYZ1 and aXYZ2
         */
        public static double distance(double[] aXYZ1, double[] aXYZ2) {
            double tX = aXYZ1[0] - aXYZ2[0];
            double tY = aXYZ1[1] - aXYZ2[1];
            double tZ = aXYZ1[2] - aXYZ2[2];
            return Fast.sqrt(tX * tX + tY * tY + tZ * tZ);
        }
        /**
         * Get dotProduct of two double[3] with no objects creation in the heap
         * @author liqa
         * @param aXYZ1 fist double[3]
         * @param aXYZ2 second double[3]
         * @return the dotProduct of aXYZ1 and aXYZ2
         */
        public static double dot(double[] aXYZ1, double[] aXYZ2) {return aXYZ1[0] * aXYZ2[0] + aXYZ1[1] * aXYZ2[1] + aXYZ1[2] * aXYZ2[2];}
        
        /**
         * Shift AtomDataXYZ by using aBoxLo
         * @author liqa
         * @param rAtomDataXYZ input AtomDataXYZ that will be shifted
         * @param aBoxLo the lower bound of box to shift
         */
        public static void shiftAtomDataXYZ(double[][] rAtomDataXYZ, double[] aBoxLo) {shiftAtomDataXYZ(rAtomDataXYZ, aBoxLo[0], aBoxLo[1], aBoxLo[2]);}
        public static void shiftAtomDataXYZ(double[][] rAtomDataXYZ, double aXlo, double aYlo, double aZlo) {
            for (int i = 0; i < rAtomDataXYZ.length; ++i) {
                rAtomDataXYZ[i][0] = rAtomDataXYZ[i][0] - aXlo;
                rAtomDataXYZ[i][1] = rAtomDataXYZ[i][1] - aYlo;
                rAtomDataXYZ[i][2] = rAtomDataXYZ[i][2] - aZlo;
            }
        }
        
        /**
         * Wrap ONCE XYZ in rAtomDataXYZ that exceeds aBox (caused by the calculation accuracy of LAMMPS)
         * @param rAtomDataXYZ input AtomDataXYZ that will be wrapped
         * @param aBox the size of the box (so the data and box need to be shifted)
         */
        public static void wrapOnceAtomDataXYZ(double[][] rAtomDataXYZ, double[] aBox) {
            double tBoxX = aBox[0], tBoxY = aBox[1], tBoxZ = aBox[2];
            for (double[] rXYZ : rAtomDataXYZ) {
                double tX = rXYZ[0];
                if      (tX <  0.0  ) rXYZ[0] += tBoxX;
                else if (tX >= tBoxX) rXYZ[0] -= tBoxX;
                double tY = rXYZ[1];
                if      (tY <  0.0  ) rXYZ[1] += tBoxY;
                else if (tY >= tBoxY) rXYZ[1] -= tBoxY;
                double tZ = rXYZ[2];
                if      (tZ <  0.0  ) rXYZ[2] += tBoxZ;
                else if (tZ >= tBoxZ) rXYZ[2] -= tBoxZ;
            }
        }
        
        /**
         * Wrap XYZ in rUnwrappedAtomDataXYZ that exceeds aBox (Used specifically unwrapped data)
         * @param rUnwrappedAtomDataXYZ input AtomDataXYZ that will be wrapped
         * @param aBoxLo the lower bound of the box
         * @param aBoxHi the lower bound of the box
         */
        public static void wrapAtomDataXYZ(double[][] rUnwrappedAtomDataXYZ, double[] aBoxLo, double[] aBoxHi) {
            double tBoxLoX = aBoxLo[0], tBoxLoY = aBoxLo[1], tBoxLoZ = aBoxLo[2];
            double tBoxHiX = aBoxHi[0], tBoxHiY = aBoxHi[1], tBoxHiZ = aBoxHi[2];
            double tBoxX = tBoxHiX-tBoxLoX, tBoxY = tBoxHiY-tBoxLoY, tBoxZ = tBoxHiZ-tBoxLoZ;
            for (double[] rXYZ : rUnwrappedAtomDataXYZ) {
                double tX = rXYZ[0];
                if      (tX <  tBoxLoX) {while (rXYZ[0] <  tBoxLoX) rXYZ[0] += tBoxX;}
                else if (tX >= tBoxHiX) {while (rXYZ[0] >= tBoxHiX) rXYZ[0] -= tBoxX;}
                double tY = rXYZ[1];
                if      (tY <  tBoxLoY) {while (rXYZ[1] <  tBoxLoY) rXYZ[1] += tBoxY;}
                else if (tY >= tBoxHiY) {while (rXYZ[1] >= tBoxHiY) rXYZ[1] -= tBoxY;}
                double tZ = rXYZ[2];
                if      (tZ <  tBoxLoZ) {while (rXYZ[2] <  tBoxLoZ) rXYZ[2] += tBoxZ;}
                else if (tZ >= tBoxHiZ) {while (rXYZ[2] >= tBoxHiZ) rXYZ[2] -= tBoxZ;}
            }
        }
        
        /**
         * Wrap XYZ in rUnwrappedScaledAtomDataXYZ that exceeds aBox (Used specifically unwrapped-scaled data)
         * @param rUnwrappedScaledAtomDataXYZ input AtomDataXYZ that will be wrapped
         */
        public static void wrapScaledAtomDataXYZ(double[][] rUnwrappedScaledAtomDataXYZ) {
            for (double[] rXYZ : rUnwrappedScaledAtomDataXYZ) {
                double tX = rXYZ[0];
                if      (tX <  0.0) {while (rXYZ[0] <  0.0) ++rXYZ[0];}
                else if (tX >= 1.0) {while (rXYZ[0] >= 1.0) --rXYZ[0];}
                double tY = rXYZ[1];
                if      (tY <  0.0) {while (rXYZ[1] <  0.0) ++rXYZ[1];}
                else if (tY >= 1.0) {while (rXYZ[1] >= 1.0) --rXYZ[1];}
                double tZ = rXYZ[2];
                if      (tZ <  0.0) {while (rXYZ[2] <  0.0) ++rXYZ[2];}
                else if (tZ >= 1.0) {while (rXYZ[2] >= 1.0) --rXYZ[2];}
            }
        }
        
        /**
         * Unscale XYZ in rScaledAtomDataXYZ  (Used specifically scaled data)
         * @param rScaledAtomDataXYZ input AtomDataXYZ that will be unscaled
         * @param aBoxLo the lower bound of the box
         * @param aBoxHi the lower bound of the box
         */
        public static void unscaleAtomDataXYZ(double[][] rScaledAtomDataXYZ, double[] aBoxLo, double[] aBoxHi) {
            double tBoxLoX = aBoxLo[0], tBoxLoY = aBoxLo[1], tBoxLoZ = aBoxLo[2];
            double tBoxX = aBoxHi[0]-tBoxLoX, tBoxY = aBoxHi[1]-tBoxLoY, tBoxZ = aBoxHi[2]-tBoxLoZ;
            for (double[] rXYZ : rScaledAtomDataXYZ) {
                rXYZ[0] *= tBoxX; rXYZ[0] += tBoxLoX;
                rXYZ[1] *= tBoxY; rXYZ[1] += tBoxLoY;
                rXYZ[2] *= tBoxZ; rXYZ[2] += tBoxLoZ;
            }
        }
    }
    
    
    
    /** a Parfor ThreadPool for MathEX usage */
    public static class Par {
        private static ParforThreadPool POOL = new ParforThreadPool(1);
        public static void setThreadNum(int aThreadNum) {POOL.shutdown(); POOL = new ParforThreadPool(aThreadNum);}
        public static void closeThreadPool() {if (POOL.nThreads() > 1) setThreadNum(1);}
        // 在 JVM 关闭时时关闭 POOL
        static {Runtime.getRuntime().addShutdownHook(new Thread(() -> POOL.shutdown()));}
    }
    
    /// Special functions (in vector) or its operations
    public static class Func {
        /**
         * Get the aN length sequence from aStart in aStep,
         * the result will like start:step:end in matlab
         * and end = start + aStep*(aN-1).
         * @author liqa
         * @param aStart the start position, include
         * @param aStep step of the sequence
         * @param aN length of the sequence
         * @return the sequence
         */
        public static double[] sequence     (double aStart, double aStep, int aN) {return sequence2Dest(aStart, aStep, new double[aN]);}
        public static double[] sequence2Dest(double aStart, double aStep, double[] rDest) {return sequence2Dest(aStart, aStep, rDest, 0, rDest.length);}
        public static double[] sequence2Dest(double aStart, double aStep, double[] rDest, int aDestPos, int aLength) {
            rDest[aDestPos] = aStart;
            for (int i = aDestPos+1; i < aDestPos+aLength; ++i) {
                rDest[i] = rDest[i-1] + aStep;
            }
            return rDest;
        }
        
        /**
         * Linear Interpolation like in matlab
         * @param aX1 left x
         * @param aX2 right x
         * @param aF1 left f(x1)
         * @param aF2 right f(x2)
         * @param aXq query points
         * @return the Linear Interpolation result yq
         */
        public static double interp1(double aX1, double aX2, double aF1, double aF2, double aXq) {
            if (aXq <= aX1) return aF1;
            if (aXq >= aX2) return aF2;
            return aF1 + (aXq-aX1)/(aX2-aX1) * (aF2-aF1);
        }
        
        /**
         * Get the Dirac Delta function δ(x-mu) in the Gaussian form,
         * result will in [-aDx*aN, aDx*aN], so out.length == 2*N+1
         * <p> Optimized for vector operations </p>
         * @author liqa
         * @param aSigma the standard deviation of the Gaussian distribution
         * @param aMu the mean value of the Gaussian distribution
         * @param aX the input x vector, or use deltaG2Dest then rDest will be replaced by the output
         * @return the Dirac Delta function δ(x-mu) in the Gaussian form
         */
        public static double[] deltaG     (double aSigma, final double aMu, double[] aX) {return deltaG2Dest(aSigma, aMu, Arrays.copyOf(aX, aX.length));}
        public static double[] deltaG2Dest(double aSigma, final double aMu, double[] rDest) {return deltaG2Dest(aSigma, aMu, rDest, 0, rDest.length);}
        public static double[] deltaG2Dest(double aSigma, final double aMu, double[] rDest, int aDestPos, int aLength) {
            final double tXMul = -1.0 / (2.0*aSigma*aSigma);
            final double tYMul =  1.0 / (Fast.sqrt(2.0*PI) * aSigma);
            
            return Vec.mapDo2Dest(rDest, x -> {
                x = x-aMu;
                return Fast.exp(x * x * tXMul) * tYMul;
            }, aDestPos, aLength);
        }
        
        /**
         * convert RDF (radial distribution function, g(r))
         * to SF (structural factor, S(q)), same format in {@link MonatomicParameterCalculator}
         * @author liqa
         * @param aGr the matrix form of g(r)
         * @param aRou the atom number density
         * @param aN the split number of output
         * @param aQMax the max q of output S(q)
         * @param aQMin the min q of output S(q)
         * @return the structural factor, S(q)
         */
        public static double[][] RDF2SF (double[][] aGr,              double aRou, int aN, double aQMax, double aQMin) {aGr = Mat.transpose(aGr); return RDF2SF_(aGr[0], aGr[1], aRou, aN, aQMax, aQMin);}
        public static double[][] RDF2SF (double[][] aGr,              double aRou                                    ) {aGr = Mat.transpose(aGr); return RDF2SF_(aGr[0], aGr[1], aRou);}
        public static double[][] RDF2SF (double[][] aGr,              double aRou, int aN                            ) {aGr = Mat.transpose(aGr); return RDF2SF_(aGr[0], aGr[1], aRou, aN);}
        public static double[][] RDF2SF (double[][] aGr,              double aRou, int aN, double aQMax              ) {aGr = Mat.transpose(aGr); return RDF2SF_(aGr[0], aGr[1], aRou, aN, aQMax);}
        public static double[][] RDF2SF_(double[]   aGr, double[] aR, double aRou                                    ) {return RDF2SF_(aGr, aR, aRou, 100);}
        public static double[][] RDF2SF_(double[]   aGr, double[] aR, double aRou, int aN                            ) {double tRPeek = aR[Vec.maxWithIdx(aGr).second]; return RDF2SF_(aGr, aR, aRou, aN, 2.0*PI/tRPeek * 6.0, 2.0*PI/tRPeek * 0.4);}
        public static double[][] RDF2SF_(double[]   aGr, double[] aR, double aRou, int aN, double aQMax              ) {double tRPeek = aR[Vec.maxWithIdx(aGr).second]; return RDF2SF_(aGr, aR, aRou, aN, aQMax, 2.0*PI/tRPeek * 0.4);}
        public static double[][] RDF2SF_(double[]   aGr, double[] aR, double aRou, int aN, double aQMax, double aQMin) {
            double dq = (aQMax-aQMin)/aN;
            double[] q = Func.sequence(aQMin+dq, dq, aN);
            
            double[] Sq = new double[aN];
            
            double tFrontMul = 4.0*PI*aRou;
            for (int i = 0; i < aN; ++i) {
                final double tQ = q[i];
                Sq[i] = 1.0 + tFrontMul * convolve((gr, r) -> (r * (gr-1.0) * Fast.sin(tQ*r)), aGr, aR) / tQ;
            }
            
            return Mat.transpose(new double[][] {Vec.merge(0.0, Sq), Vec.merge(aQMin, q)});
        }
        /**
         * convert SF to RDF, same format in {@link MonatomicParameterCalculator}
         * @author liqa
         * @param aSq the matrix form of S(q)
         * @param aRou the atom number density
         * @param aN the split number of output
         * @param aRMax the max r of output g(r)
         * @param aRMin the min r of output g(r)
         * @return the radial distribution function, g(r)
         */
        public static double[][] SF2RDF (double[][] aSq,              double aRou, int aN, double aRMax, double aRMin) {aSq = Mat.transpose(aSq); return SF2RDF_(aSq[0], aSq[1], aRou, aN, aRMax, aRMin);}
        public static double[][] SF2RDF (double[][] aSq,              double aRou                                    ) {aSq = Mat.transpose(aSq); return SF2RDF_(aSq[0], aSq[1], aRou);}
        public static double[][] SF2RDF (double[][] aSq,              double aRou, int aN                            ) {aSq = Mat.transpose(aSq); return SF2RDF_(aSq[0], aSq[1], aRou, aN);}
        public static double[][] SF2RDF (double[][] aSq,              double aRou, int aN, double aRMax              ) {aSq = Mat.transpose(aSq); return SF2RDF_(aSq[0], aSq[1], aRou, aN, aRMax);}
        public static double[][] SF2RDF_(double[]   aSq, double[] aQ, double aRou                                    ) {return SF2RDF_(aSq, aQ, aRou, 100);}
        public static double[][] SF2RDF_(double[]   aSq, double[] aQ, double aRou, int aN                            ) {double tQPeek = aQ[Vec.maxWithIdx(aSq).second]; return SF2RDF_(aSq, aQ, aRou, aN, 2.0*PI/tQPeek * 6.0, 2.0*PI/tQPeek * 0.4);}
        public static double[][] SF2RDF_(double[]   aSq, double[] aQ, double aRou, int aN, double aRMax              ) {double tQPeek = aQ[Vec.maxWithIdx(aSq).second]; return SF2RDF_(aSq, aQ, aRou, aN, aRMax, 2.0*PI/tQPeek * 0.4);}
        public static double[][] SF2RDF_(double[]   aSq, double[] aQ, double aRou, int aN, double aRMax, double aRMin) {
            double dr = (aRMax-aRMin)/aN;
            double[] r = Func.sequence(aRMin+dr, dr, aN);
            
            double[] gr = new double[aN];
            
            double tFrontMul = 1.0/(2.0*PI*PI*aRou);
            for (int i = 0; i < aN; ++i) {
                final double tR = r[i];
                gr[i] = 1.0 + tFrontMul * convolve((Sq, q) -> (q * (Sq-1.0) * Fast.sin(q*tR)), aSq, aQ) / tR;
            }
            
            return Mat.transpose(new double[][] {Vec.merge(0.0, gr), Vec.merge(aRMin, r)});
        }
        
        
        /**
         * get the numerical laplacian of the input numerical function in PBC,
         * Δf(x_i) = (f(x_{i-1}) + f(x_{i+1}) - 2f(x_i)) / (δx^2)
         * @author liqa
         * @param aFunc input numerical function, can be double[], Func1, aFunc2 or aFunc3
         * @return the numerical laplacian of the function (in PBC)
         */
        public static double[] laplacian(double[] aFunc, double aDx) {return laplacian2Dest(aFunc, aDx, new double[aFunc.length]);}
        public static double[] laplacian2Dest(double[] aFunc, double aDx, double[] rDest) {
            int tNx = aFunc.length;
            double tDx2 = aDx*aDx;
            for (int i = 0; i < tNx; ++i) {
                int imm = i-1; if (imm <  0  ) imm += tNx;
                int ipp = i+1; if (ipp >= tNx) ipp -= tNx;
                
                rDest[i] = (aFunc[imm] + aFunc[ipp] - 2*aFunc[i]) / tDx2;
            }
            return rDest;
        }
        public static Func1 laplacian(Func1 aFunc1) {return laplacian2Dest(aFunc1, aFunc1.shell().setData(new double[aFunc1.data().length]));}
        public static Func1 laplacian2Dest(Func1 aFunc1, Func1 rDest) {
            int tNx = aFunc1.Nx();
            double tDx2 = aFunc1.dx()*aFunc1.dx();
            for (int i = 0; i < tNx; ++i) {
                int imm = i-1; if (imm <  0  ) imm += tNx;
                int ipp = i+1; if (ipp >= tNx) ipp -= tNx;
                
                rDest.set(i, (aFunc1.get_(imm) + aFunc1.get_(ipp) - 2*aFunc1.get_(i)) / tDx2);
            }
            return rDest;
        }
        public static Func2 laplacian(Func2 aFunc2) {return laplacian2Dest(aFunc2, aFunc2.shell().setData(new double[aFunc2.data().length]));}
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
        public static Func3 laplacian(Func3 aFunc3) {return laplacian2Dest(aFunc3, aFunc3.shell().setData(new double[aFunc3.data().length]));}
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
        public static Func3 parlaplacian2Dest(final Func3 aFunc3, final Func3 rDest) {return parlaplacian2Dest(Par.POOL, aFunc3, rDest);}
        public static Func3 parlaplacian2Dest(ParforThreadPool aPool, final Func3 aFunc3, final Func3 rDest) {
            final int tNx = aFunc3.Nx();
            final int tNy = aFunc3.Ny();
            final int tNz = aFunc3.Ny();
            final double tDx2 = aFunc3.dx()*aFunc3.dx();
            final double tDy2 = aFunc3.dy()*aFunc3.dy();
            final double tDz2 = aFunc3.dz()*aFunc3.dz();
            aPool.parfor_(tNz, k -> {
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
        public static double[] odeEuler(IOperator2<Double> aFunc2, double aStartY, double aStartX, double aDx, int aSteps) {
            double[] tResult = new double[aSteps+1];
            tResult[0] = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult[i+1] = tResult[i] + aDx*aFunc2.cal(tResult[i], tX);
                tX += aDx;
            }
            return tResult;
        }
        public static double odeLastEuler(IOperator2<Double> aFunc2, double aStartY, double aStartX, double aDx, int aSteps) {
            double tResult = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult += aDx*aFunc2.cal(tResult, tX);
                tX += aDx;
            }
            return tResult;
        }
        public static double[][] odeEuler(IOperator2Full<double[], double[], Double> aFunc2, double[] aStartY, double aStartX, final double aDx, int aSteps) {
            double[][] tResult = new double[aSteps+1][];
            tResult[0] = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult[i+1] = Vec.ebeDo(tResult[i], aFunc2.cal(tResult[i], tX), (y, fyx) -> y + aDx*fyx);
                tX += aDx;
            }
            return tResult;
        }
        /** 利用上 aDestFunc2 产生的临时变量 */
        public static double[][] ode2DestEuler(IOperator2Full<double[], double[], Double> aDestFunc2, double[] aStartY, double aStartX, final double aDx, int aSteps) {
            double[][] tResult = new double[aSteps+1][];
            tResult[0] = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult[i+1] = Vec.ebeDo2Dest(aDestFunc2.cal(tResult[i], tX), tResult[i], (fyx, y) -> y + aDx*fyx);
                tX += aDx;
            }
            return tResult;
        }
        public static double[] odeLastEuler(IOperator2Full<double[], double[], Double> aFunc2, double[] aStartY, double aStartX, final double aDx, int aSteps) {
            double[] tResult = Vec.copy(aStartY);
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                Vec.ebeDo2Dest(tResult, aFunc2.cal(tResult, tX), (y, fyx) -> y + aDx*fyx);
                tX += aDx;
            }
            return tResult;
        }
        public static double[]   odeEuler         (IOperator2<Double> aFunc2, double aStartY, double aDx, int aSteps) {return odeEuler    (aFunc2, aStartY, 0.0, aDx, aSteps);}
        public static double     odeLastEuler     (IOperator2<Double> aFunc2, double aStartY, double aDx, int aSteps) {return odeLastEuler(aFunc2, aStartY, 0.0, aDx, aSteps);}
        public static double[][] odeEuler         (IOperator2Full<double[], double[], Double> aFunc2    , double[] aStartY, double aDx, int aSteps) {return odeEuler         (aFunc2    , aStartY, 0.0, aDx, aSteps);}
        public static double[][] ode2DestEuler    (IOperator2Full<double[], double[], Double> aDestFunc2, double[] aStartY, double aDx, int aSteps) {return ode2DestEuler    (aDestFunc2, aStartY, 0.0, aDx, aSteps);}
        public static double[]   odeLastEuler     (IOperator2Full<double[], double[], Double> aFunc2    , double[] aStartY, double aDx, int aSteps) {return odeLastEuler     (aFunc2    , aStartY, 0.0, aDx, aSteps);}
        
        
        /**
         * General Convolve methods, use the vector format y, x and function format conv core,
         * will do the integral like: integral(aConv(y, x), x)
         * @author liqa
         * @param aConv binary operator aConv(y, x), Override to customize convolution form
         * @param aY the vector of y
         * @param aX the vector of x
         * @return convolution result
         */
        public static double convolve(IOperator2<Double> aConv, double[] aY, double[] aX) {
            double pX = aX[0];
            double pC = aConv.cal(aY[0], pX); // note that y is before x
            double tResult = 0.0;
            for (int i = 1; i < aX.length; ++i) {
                double tX = aX[i];
                double tC = aConv.cal(aY[i], tX);
                tResult += ((tX-pX)/2.0)*(tC + pC);
                pC = tC;
                pX = tX;
            }
            return tResult;
        }
        
        
        /**
         * General integration methods, use the function or vector format y and the dx(equal interval) / x(any)
         * @author liqa
         * @return the integral result
         */
        public static double integral(IOperator1<Double> aFunc, double aStartX, double aDx, int aN) {
            double tX = aStartX;
            double pY = aFunc.cal(tX);
            double tResult = 0.0;
            for (int i = 1; i < aN; ++i) {
                tX += aDx;
                double tY = aFunc.cal(tX);
                tResult += (aDx/2.0)*(tY + pY);
                pY = tY;
            }
            return tResult;
        }
        public static double integral(IOperator1<Double> aFunc, double[] aX) {
            double pX = aX[0];
            double pY = aFunc.cal(pX);
            double tResult = 0.0;
            for (int i = 1; i < aX.length; ++i) {
                double tX = aX[i];
                double tY = aFunc.cal(tX);
                tResult += ((tX-pX)/2.0)*(tY + pY);
                pY = tY;
                pX = tX;
            }
            return tResult;
        }
        public static double integral(double[] aY, double aDx) {
            double tResult = 0.0;
            for (int i = 1; i < aY.length; ++i) tResult += (aDx/2.0)*(aY[i] + aY[i-1]);
            return tResult;
        }
        public static double integral(double[] aY, double[] aX) {
            double tResult = 0.0;
            for (int i = 1; i < aY.length; ++i) tResult += ((aX[i]-aX[i-1])/2.0)*(aY[i] + aY[i-1]);
            return tResult;
        }
    }
    
    
    /// operations in FastMath
    public static class Fast {
        public static double sqrt(double aValue) {return FastMath.sqrt(aValue);}
        public static double cbrt(double aValue) {return FastMath.cbrt(aValue);}
        
        public static double exp(double aValue) {return FastMath.exp(aValue);}
        public static double log(double aValue) {return FastMath.log(aValue);}
        
        public static double sin(double aValue) {return FastMath.sin(aValue);}
        public static double cos(double aValue) {return FastMath.cos(aValue);}
        public static double tan(double aValue) {return FastMath.tan(aValue);}
        
        public static double pow(double aValue, double aPower) {return FastMath.pow(aValue, aPower);}
    }
    
    
    /// utils operations
    public static class Code {
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
                out = (int) Math.ceil(tValue);
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
                int tOut = (int) Math.ceil(tValue);
                if (tOut > aNum) return out;
                out = tOut;
            }
        }
    }
    
}
