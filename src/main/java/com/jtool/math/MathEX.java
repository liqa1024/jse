package com.jtool.math;

import com.jtool.code.Pair;
import com.jtool.code.operator.IOperator1;
import com.jtool.code.operator.IOperator2;
import com.jtool.code.operator.IOperator2Full;
import com.jtool.math.function.Func2;
import com.jtool.math.function.Func3;
import com.jtool.math.function.IFunc1;
import com.jtool.math.function.ZeroBoundSymmetryFunc1;
import com.jtool.parallel.ParforThreadPool;
import net.jafama.FastMath;
import org.jetbrains.annotations.ApiStatus;

import static com.jtool.code.CS.ZL_MAT;


/**
 * @author liqa
 * <p> Extended mathematical methods </p>
 * <p> The method of using internal Thread Pool is not thread safe when nThreads > 1 </p>
 */
@SuppressWarnings("DuplicatedCode")
public class MathEX {
    
    public static final double PI = Math.PI;
    
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
            aPool.parfor(tN, i -> {
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
            aPool.parfor(tN, i -> {
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
            aPool.parfor(tN, i -> {
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
            aPool.parfor(tN, i -> {
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
    
    
    /** a Parfor ThreadPool for MathEX usage */
    public static class Par {
        private static ParforThreadPool POOL = new ParforThreadPool(1);
        public static void setThreadNum(int aThreadNum) {POOL.shutdown(); POOL = new ParforThreadPool(aThreadNum);}
        public static void closeThreadPool() {if (POOL.nThreads() > 1) setThreadNum(1);}
        // 在 JVM 关闭时时关闭 POOL
        static {Runtime.getRuntime().addShutdownHook(new Thread(() -> POOL.shutdown()));}
    }
    
    
    /// Methods of Graph usage
    public static class Graph {
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
        
        public static class PosBox2D {
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
    
    /// Special functions (in vector) or its operations
    public static class Func {
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
            if (aX1 > aX2) return interp1(aX2, aX1, aF2, aF1, aXq);
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
         * @param aResolution the Resolution of the Function1, dx == aSigma/aResolution
         * @return the Dirac Delta function δ(x-mu) in the Gaussian form
         */
        public static IFunc1 deltaG(double aSigma, final double aMu, double aResolution) {
            final double tXMul = -1.0 / (2.0*aSigma*aSigma);
            final double tYMul =  1.0 / (Fast.sqrt(2.0*PI) * aSigma);
            
            return new ZeroBoundSymmetryFunc1(aMu, aSigma/aResolution, (int)Math.round(aResolution*G_RANG), x -> {
                x -= aMu;
                return Fast.exp(x * x * tXMul) * tYMul;
            });
        }
        public final static int G_RANG = 6;
        
        
        
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
        @ApiStatus.Obsolete
        public static double odeLastEuler(IOperator2<Double> aFunc2, double aStartY, double aStartX, double aDx, int aSteps) {
            double tResult = aStartY;
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                tResult += aDx*aFunc2.cal(tResult, tX);
                tX += aDx;
            }
            return tResult;
        }
        @ApiStatus.Obsolete
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
        @ApiStatus.Obsolete
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
        @ApiStatus.Obsolete
        public static double[] odeLastEuler(IOperator2Full<double[], double[], Double> aFunc2, double[] aStartY, double aStartX, final double aDx, int aSteps) {
            double[] tResult = Vec.copy(aStartY);
            double tX = aStartX;
            for (int i = 0; i < aSteps; ++i) {
                Vec.ebeDo2Dest(tResult, aFunc2.cal(tResult, tX), (y, fyx) -> y + aDx*fyx);
                tX += aDx;
            }
            return tResult;
        }
        @ApiStatus.Obsolete public static double[]   odeEuler         (IOperator2<Double> aFunc2, double aStartY, double aDx, int aSteps) {return odeEuler    (aFunc2, aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double     odeLastEuler     (IOperator2<Double> aFunc2, double aStartY, double aDx, int aSteps) {return odeLastEuler(aFunc2, aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double[][] odeEuler         (IOperator2Full<double[], double[], Double> aFunc2    , double[] aStartY, double aDx, int aSteps) {return odeEuler         (aFunc2    , aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double[][] ode2DestEuler    (IOperator2Full<double[], double[], Double> aDestFunc2, double[] aStartY, double aDx, int aSteps) {return ode2DestEuler    (aDestFunc2, aStartY, 0.0, aDx, aSteps);}
        @ApiStatus.Obsolete public static double[]   odeLastEuler     (IOperator2Full<double[], double[], Double> aFunc2    , double[] aStartY, double aDx, int aSteps) {return odeLastEuler     (aFunc2    , aStartY, 0.0, aDx, aSteps);}
        
        
        /**
         * General Convolve methods, use the vector format y, x and function format conv core,
         * will do the integral like: integral(aConv(y, x), x)
         * @author liqa
         * @param aConv binary operator aConv(y, x), Override to customize convolution form
         * @param aY the vector of y
         * @param aX the vector of x
         * @return convolution result
         */
        @Deprecated @ApiStatus.ScheduledForRemoval
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
        @Deprecated @ApiStatus.ScheduledForRemoval
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
        @Deprecated @ApiStatus.ScheduledForRemoval
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
        @Deprecated @ApiStatus.ScheduledForRemoval
        public static double integral(double[] aY, double aDx) {
            double tResult = 0.0;
            for (int i = 1; i < aY.length; ++i) tResult += (aDx/2.0)*(aY[i] + aY[i-1]);
            return tResult;
        }
        @Deprecated @ApiStatus.ScheduledForRemoval
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
