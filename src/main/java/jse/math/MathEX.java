package jse.math;

import jse.atom.IXYZ;
import jse.atom.XYZ;
import jse.cache.LogicalVectorCache;
import jse.cache.VectorCache;
import jse.code.collection.DoublePair;
import jse.code.collection.DoubleTriplet;
import jse.code.collection.IListGetter;
import jse.code.collection.IntDeque;
import jse.code.iterator.IHasIntIterator;
import jse.math.vector.*;
import net.jafama.DoubleWrapper;
import net.jafama.FastMath;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;

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
    public final static int SH_LARGEST_L = 1000;
    
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
         * 输出第一类 Chebyshev 多项式函数（Tn），参见：
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
         * 输出第二类 Chebyshev 多项式函数（Un），参见：
         * <a href="https://en.wikipedia.org/wiki/Chebyshev_polynomials">
         * Chebyshev polynomials </a>
         * @author liqa
         * @param aN Chebyshev 多项式阶数，有 {@code n >= 0}
         * @return 计算结果，实数
         */
        public static double chebyshev2(@Range(from = 0, to = Integer.MAX_VALUE) int aN, double aX) {
            // 直接采用递推关系递归计算
            switch(aN) {
            case 0: {return 1.0;}
            case 1: {return 2.0 * aX;}
            default: {return 2.0 * aX * chebyshev2(aN - 1, aX) - chebyshev2(aN - 2, aX);}
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
         * <p>
         * 所有的球谐函数值 Ylm 排列成一个复向量，
         * 先遍历 m 后遍历 l，由于有
         * {@code m = -l ~ l}，也就是说，如果需要访问给定 {@code (l, m)}
         * 的 Ylm，需要使用：
         * <pre> {@code
         * def YlmFull = MathEx.Func.sphericalHarmonicsFull(...)
         * int idx = l*(l+1) + m
         * def Ylm = YlmFull[idx]
         * } </pre>
         * 来获取。
         * @author liqa
         * @param aLMax 球谐函数参数 l，非负整数
         * @param aTheta 球坐标下径向方向与 z 轴的角度
         * @param aPhi 球坐标下径向方向在 xy 平面投影下和 x 轴的角度
         * @return {@code l = 0 ~ aLMax}, {@code m = -l ~ l} 下所有球谐函数值组成的复向量，按照 l 从小到大排列，先遍历 m 后遍历 l
         */
        public static ComplexVector sphericalHarmonicsFull(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi) {
            ComplexVector rY = ComplexVector.zeros((aLMax+1)*(aLMax+1));
            sphericalHarmonicsFull2Dest_(aLMax, aTheta, aPhi, rY);
            return rY;
        }
        public static ComplexVector sphericalHarmonicsFull3(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ) {
            ComplexVector rY = ComplexVector.zeros((aLMax+1)*(aLMax+1));
            sphericalHarmonicsFull2Dest3_(aLMax, aX, aY, aZ, rY);
            return rY;
        }
        public static void sphericalHarmonicsFull2Dest(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi, IComplexVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < (aLMax+1)*(aLMax+1)) throw new IllegalArgumentException("Size of rDest MUST be GreaterOrEqual to (L+1)^2 ("+((aLMax+1)*(aLMax+1))+"), input: "+rDest.size());
            sphericalHarmonicsFull2Dest_(aLMax, aTheta, aPhi, rDest);
        }
        public static void sphericalHarmonicsFull2Dest3(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ, IComplexVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < (aLMax+1)*(aLMax+1)) throw new IllegalArgumentException("Size of rDest MUST be GreaterOrEqual to (L+1)^2 ("+((aLMax+1)*(aLMax+1))+"), input: "+rDest.size());
            sphericalHarmonicsFull2Dest3_(aLMax, aX, aY, aZ, rDest);
        }
        private static void sphericalHarmonicsFull2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi, IComplexVector rDest) {
            DoubleWrapper tJafamaDoubleWrapper = new DoubleWrapper(); // new 的损耗应该可以忽略掉
            double tSinTheta = FastMath.sinAndCos(aTheta, tJafamaDoubleWrapper);
            double tCosTheta = tJafamaDoubleWrapper.value;
            double tSinPhi = FastMath.sinAndCos(aPhi, tJafamaDoubleWrapper);
            double tCosPhi = tJafamaDoubleWrapper.value;
            sphericalHarmonicsFull2Dest4_(aLMax, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        private static void sphericalHarmonicsFull2Dest3_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ, IComplexVector rDest) {
            sphericalHarmonicsFull2DestXYZDis_(aLMax, aX, aY, aZ, Fast.hypot(aX, aY, aZ), rDest);
        }
        @ApiStatus.Internal
        public static void sphericalHarmonicsFull2DestXYZDis_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ, double aDis, IComplexVector rDest) {
            double tXY = Fast.hypot(aX, aY);
            double tCosTheta = aZ / aDis;
            double tSinTheta = tXY / aDis;
            double tCosPhi;
            double tSinPhi;
            // 注意避免 NaN 的情况
            if (Code.numericEqual(tXY, 0.0)) {
                tCosPhi = 1.0;
                tSinPhi = 0.0;
            } else {
                tCosPhi = aX / tXY;
                tSinPhi = aY / tXY;
            }
            sphericalHarmonicsFull2Dest4_(aLMax, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        @ApiStatus.Internal
        public static void sphericalHarmonicsFull2Dest4_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aCosTheta, double aSinTheta, double aCosPhi, double aSinPhi, IComplexVector rDest) {
            // 现在统一通过实球谐函数的结果设置
            realSphericalHarmonicsFull2Dest4_(aLMax, aCosTheta, aSinTheta, aCosPhi, aSinPhi, rDest.real());
            for (int tM = 1; tM <= aLMax; ++tM) {
                int tStartL = tM*tM+tM;
                for (int tL = tM; tL <= aLMax; ++tL) {
                    int tIdxPos = tStartL+tM;
                    int tIdxNeg = tStartL-tM;
                    double tRealYlmPos = rDest.getReal(tIdxPos);
                    double tRealYlmNeg = rDest.getReal(tIdxNeg);
                    rDest.setReal(tIdxPos, SQRT2_INV*tRealYlmPos); rDest.setImag(tIdxPos, SQRT2_INV*tRealYlmNeg);
                    if ((tM&1)==1) {
                    rDest.setReal(tIdxNeg, -SQRT2_INV*tRealYlmPos); rDest.setImag(tIdxNeg, SQRT2_INV*tRealYlmNeg);
                    } else {
                    rDest.setReal(tIdxNeg, SQRT2_INV*tRealYlmPos); rDest.setImag(tIdxNeg, -SQRT2_INV*tRealYlmNeg);
                    }
                    tStartL += tL+tL+2;
                }
            }
        }
        
        /**
         * 直接计算实球谐函数的结果，这样可以缩小数据的大小，并且减少后续进行 m 方向点乘需要的操作
         * <p>
         * 注意这里采用 <a href="https://arxiv.org/abs/1410.1748">
         * Taweetham Limpanuparb, Josh Milthorpe. 2014 </a>
         * 中定义的实球谐函数形式，相比 <a href="https://en.wikipedia.org/wiki/Spherical_harmonics">
         * wikipedia 中 real spherical harmonics 定义 </a>
         * 缺少一项 {@code (-1)^m}
         * @author liqa
         * @param aLMax 球谐函数参数 l，非负整数
         * @param aTheta 球坐标下径向方向与 z 轴的角度
         * @param aPhi 球坐标下径向方向在 xy 平面投影下和 x 轴的角度
         * @return {@code l = 0 ~ aLMax}, {@code m = -l ~ l} 下所有实球谐函数值，按照 l 从小到大排列，先遍历 m 后遍历 l
         */
        public static Vector realSphericalHarmonicsFull(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi) {
            Vector rY = Vector.zeros((aLMax+1)*(aLMax+1));
            realSphericalHarmonicsFull2Dest_(aLMax, aTheta, aPhi, rY);
            return rY;
        }
        public static Vector realSphericalHarmonicsFull3(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ) {
            Vector rY = Vector.zeros((aLMax+1)*(aLMax+1));
            realSphericalHarmonicsFull2Dest3_(aLMax, aX, aY, aZ, rY);
            return rY;
        }
        public static void realSphericalHarmonicsFull2Dest(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi, IVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < (aLMax+1)*(aLMax+1)) throw new IllegalArgumentException("Size of rDest MUST be GreaterOrEqual to (L+1)^2 ("+((aLMax+1)*(aLMax+1))+"), input: "+rDest.size());
            realSphericalHarmonicsFull2Dest_(aLMax, aTheta, aPhi, rDest);
        }
        public static void realSphericalHarmonicsFull2Dest3(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ, IVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < (aLMax+1)*(aLMax+1)) throw new IllegalArgumentException("Size of rDest MUST be GreaterOrEqual to (L+1)^2 ("+((aLMax+1)*(aLMax+1))+"), input: "+rDest.size());
            realSphericalHarmonicsFull2Dest3_(aLMax, aX, aY, aZ, rDest);
        }
        private static void realSphericalHarmonicsFull2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aTheta, double aPhi, IVector rDest) {
            DoubleWrapper tJafamaDoubleWrapper = new DoubleWrapper(); // new 的损耗应该可以忽略掉
            double tSinTheta = FastMath.sinAndCos(aTheta, tJafamaDoubleWrapper);
            double tCosTheta = tJafamaDoubleWrapper.value;
            double tSinPhi = FastMath.sinAndCos(aPhi, tJafamaDoubleWrapper);
            double tCosPhi = tJafamaDoubleWrapper.value;
            realSphericalHarmonicsFull2Dest4_(aLMax, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        private static void realSphericalHarmonicsFull2Dest3_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ, IVector rDest) {
            realSphericalHarmonicsFull2DestXYZDis_(aLMax, aX, aY, aZ, Fast.hypot(aX, aY, aZ), rDest);
        }
        @ApiStatus.Internal
        public static void realSphericalHarmonicsFull2DestXYZDis_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, double aZ, double aDis, IVector rDest) {
            double tXY = Fast.hypot(aX, aY);
            double tCosTheta = aZ / aDis;
            double tSinTheta = tXY / aDis;
            double tCosPhi;
            double tSinPhi;
            // 注意避免 NaN 的情况
            if (Code.numericEqual(tXY, 0.0)) {
                tCosPhi = 1.0;
                tSinPhi = 0.0;
            } else {
                tCosPhi = aX / tXY;
                tSinPhi = aY / tXY;
            }
            realSphericalHarmonicsFull2Dest4_(aLMax, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        @ApiStatus.Internal
        public static void realSphericalHarmonicsFull2Dest4_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aCosTheta, double aSinTheta, double aCosPhi, double aSinPhi, IVector rDest) {
            realNormalizedLegendreFull2Dest_(aLMax, aCosTheta, aSinTheta, rDest);
            // 现在 m = 0 的情况不需要设置了
            double tSinMmmPhi = 0.0;
            double tCosMmmPhi = 1.0;
            double tSinMPhi = aSinPhi;
            double tCosMPhi = aCosPhi;
            final double tCosPhi2 = tCosMPhi+tCosMPhi;
            for (int tM = 1; tM <= aLMax; ++tM) {
                int tStartL = tM*tM+tM;
                // 对于实球谐函数这里的处理下需要多乘一个 sqrt(2)
                final double fSqrt2CosMPhi = SQRT2*tCosMPhi;
                final double fSqrt2SinMPhi = SQRT2*tSinMPhi;
                for (int tL = tM; tL <= aLMax; ++tL) {
                    int tIdxPos = tStartL+tM;
                    int tIdxNeg = tStartL-tM;
                    rDest.update(tIdxPos, Plm -> fSqrt2CosMPhi*Plm);
                    rDest.update(tIdxNeg, Plm -> fSqrt2SinMPhi*Plm);
                    tStartL += tL+tL+2;
                }
                // 利用和差化积的递推公式来更新 tSinMPhi tCosMPhi
                double tSinMppPhi = tCosPhi2 * tSinMPhi - tSinMmmPhi;
                double tCosMppPhi = tCosPhi2 * tCosMPhi - tCosMmmPhi;
                tSinMmmPhi = tSinMPhi; tCosMmmPhi = tCosMPhi;
                tSinMPhi = tSinMppPhi; tCosMPhi = tCosMppPhi;
            }
        }
        private static void realNormalizedLegendreFull2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aLMax, double aX, double aY, IVector rDest) {
            // 直接设置到 rY 上，可以减少缓存的使用，并且可以简化实现
            double tPll = 0.28209479177387814347403972578039; // = sqrt(1/(4*PI))
            rDest.set(0, tPll);
            if (aLMax > 0) {
                rDest.set(2, SQRT3 * aX * tPll);
                tPll *= (-SQRT3DIV2 * aY);
                setRealY_(rDest, 2, 1, tPll);
                int tStartL = 6, tStartLmm = 2, tStartLm2 = 0;
                int tStartAB = 3;
                for (int tL = 2; tL <= aLMax; ++tL) {
                    for (int tM = 0; tM < tL-1; ++tM) {
                        int tIdxAB = tStartAB+tM;
                        double tPlm = SH_Alm.get(tIdxAB) * (aX*rDest.get(tStartLmm+tM) + SH_Blm.get(tIdxAB)*rDest.get(tStartLm2+tM));
                        if (tM == 0) rDest.set(tStartL, tPlm);
                        else setRealY_(rDest, tStartL, tM, tPlm);
                    }
                    setRealY_(rDest, tStartL, tL-1, aX * Fast.sqrt(2.0*(tL-1) + 3.0) * tPll);
                    tPll *= (-Fast.sqrt(1.0 + 0.5/(double)tL) * aY);
                    setRealY_(rDest, tStartL, tL, tPll);
                    tStartLm2 = tStartLmm;
                    tStartLmm = tStartL;
                    tStartL += tL+tL+1+1;
                    tStartAB += tL+1;
                }
            }
        }
        private static void setRealY_(IVector rY, int aIdxL0, int aM, double aValue) {
            int tIdxPos = aIdxL0+aM;
            int tIdxNeg = aIdxL0-aM;
            rY.set(tIdxPos, aValue);
            rY.set(tIdxNeg, aValue);
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
         * @return {@code m = -l ~ l} 下所有球谐函数值组成的复向量
         */
        public static ComplexVector sphericalHarmonics(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi) {
            ComplexVector rY = ComplexVector.zeros(aL+aL+1);
            sphericalHarmonics2Dest_(aL, aTheta, aPhi, rY);
            return rY;
        }
        public static ComplexVector sphericalHarmonics3(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ) {
            ComplexVector rY = ComplexVector.zeros(aL+aL+1);
            sphericalHarmonics2Dest3_(aL, aX, aY, aZ, rY);
            return rY;
        }
        public static void sphericalHarmonics2Dest(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi, IComplexVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < aL+aL+1) throw new IllegalArgumentException("Size of rY MUST be GreaterOrEqual to 2l+1 ("+(aL+aL+1)+"), input: "+rDest.size());
            sphericalHarmonics2Dest_(aL, aTheta, aPhi, rDest);
        }
        public static void sphericalHarmonics2Dest3(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ, IComplexVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < aL+aL+1) throw new IllegalArgumentException("Size of rY MUST be GreaterOrEqual to 2l+1 ("+(aL+aL+1)+"), input: "+rDest.size());
            sphericalHarmonics2Dest3_(aL, aX, aY, aZ, rDest);
        }
        private static void sphericalHarmonics2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi, IComplexVector rDest) {
            DoubleWrapper tJafamaDoubleWrapper = new DoubleWrapper(); // new 的损耗应该可以忽略掉
            double tSinTheta = FastMath.sinAndCos(aTheta, tJafamaDoubleWrapper);
            double tCosTheta = tJafamaDoubleWrapper.value;
            double tSinPhi = FastMath.sinAndCos(aPhi, tJafamaDoubleWrapper);
            double tCosPhi = tJafamaDoubleWrapper.value;
            sphericalHarmonics2Dest4_(aL, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        private static void sphericalHarmonics2Dest3_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ, IComplexVector rDest) {
            sphericalHarmonics2DestXYZDis_(aL, aX, aY, aZ, Fast.hypot(aX, aY, aZ), rDest);
        }
        @ApiStatus.Internal
        public static void sphericalHarmonics2DestXYZDis_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ, double aDis, IComplexVector rDest) {
            double tXY = Fast.hypot(aX, aY);
            double tCosTheta = aZ / aDis;
            double tSinTheta = tXY / aDis;
            double tCosPhi;
            double tSinPhi;
            // 注意避免 NaN 的情况
            if (Code.numericEqual(tXY, 0.0)) {
                tCosPhi = 1.0;
                tSinPhi = 0.0;
            } else {
                tCosPhi = aX / tXY;
                tSinPhi = aY / tXY;
            }
            sphericalHarmonics2Dest4_(aL, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        @ApiStatus.Internal
        public static void sphericalHarmonics2Dest4_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aCosTheta, double aSinTheta, double aCosPhi, double aSinPhi, IComplexVector rDest) {
            // 现在统一通过实球谐函数的结果设置
            realSphericalHarmonics2Dest4_(aL, aCosTheta, aSinTheta, aCosPhi, aSinPhi, rDest.real());
            for (int tM = 1; tM <= aL; ++tM) {
                int tIdxPos =  tM+aL;
                int tIdxNeg = -tM+aL;
                double tRealYlmPos = rDest.getReal(tIdxPos);
                double tRealYlmNeg = rDest.getReal(tIdxNeg);
                rDest.setReal(tIdxPos, SQRT2_INV*tRealYlmPos); rDest.setImag(tIdxPos, SQRT2_INV*tRealYlmNeg);
                if ((tM&1)==1) {
                rDest.setReal(tIdxNeg, -SQRT2_INV*tRealYlmPos); rDest.setImag(tIdxNeg, SQRT2_INV*tRealYlmNeg);
                } else {
                rDest.setReal(tIdxNeg, SQRT2_INV*tRealYlmPos); rDest.setImag(tIdxNeg, -SQRT2_INV*tRealYlmNeg);
                }
            }
        }
        
        /**
         * 直接计算实球谐函数的结果，这样可以缩小数据的大小，并且减少后续进行 m 方向点乘需要的操作
         * <p>
         * 注意这里采用 <a href="https://arxiv.org/abs/1410.1748">
         * Taweetham Limpanuparb, Josh Milthorpe. 2014 </a>
         * 中定义的实球谐函数形式，相比 <a href="https://en.wikipedia.org/wiki/Spherical_harmonics">
         * wikipedia 中 real spherical harmonics 定义 </a>
         * 缺少一项 {@code (-1)^m}
         * @author liqa
         * @param aL 球谐函数参数 l，非负整数
         * @param aTheta 球坐标下径向方向与 z 轴的角度
         * @param aPhi 球坐标下径向方向在 xy 平面投影下和 x 轴的角度
         * @return {@code m = -l ~ l} 下所有实球谐函数值组成的向量
         */
        public static Vector realSphericalHarmonics(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi) {
            Vector rY = Vector.zeros(aL+aL+1);
            realSphericalHarmonics2Dest_(aL, aTheta, aPhi, rY);
            return rY;
        }
        public static Vector realSphericalHarmonics3(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ) {
            Vector rY = Vector.zeros(aL+aL+1);
            realSphericalHarmonics2Dest3_(aL, aX, aY, aZ, rY);
            return rY;
        }
        public static void realSphericalHarmonics2Dest(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi, IVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < aL+aL+1) throw new IllegalArgumentException("Size of rY MUST be GreaterOrEqual to 2l+1 ("+(aL+aL+1)+"), input: "+rDest.size());
            realSphericalHarmonics2Dest_(aL, aTheta, aPhi, rDest);
        }
        public static void realSphericalHarmonics2Dest3(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ, IVector rDest) {
            // 判断输入是否合法
            if (rDest.size() < aL+aL+1) throw new IllegalArgumentException("Size of rY MUST be GreaterOrEqual to 2l+1 ("+(aL+aL+1)+"), input: "+rDest.size());
            realSphericalHarmonics2Dest3_(aL, aX, aY, aZ, rDest);
        }
        private static void realSphericalHarmonics2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aTheta, double aPhi, IVector rDest) {
            DoubleWrapper tJafamaDoubleWrapper = new DoubleWrapper(); // new 的损耗应该可以忽略掉
            double tSinTheta = FastMath.sinAndCos(aTheta, tJafamaDoubleWrapper);
            double tCosTheta = tJafamaDoubleWrapper.value;
            double tSinPhi = FastMath.sinAndCos(aPhi, tJafamaDoubleWrapper);
            double tCosPhi = tJafamaDoubleWrapper.value;
            realSphericalHarmonics2Dest4_(aL, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        private static void realSphericalHarmonics2Dest3_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ, IVector rDest) {
            realSphericalHarmonics2DestXYZDis_(aL, aX, aY, aZ, Fast.hypot(aX, aY, aZ), rDest);
        }
        @ApiStatus.Internal
        public static void realSphericalHarmonics2DestXYZDis_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, double aZ, double aDis, IVector rDest) {
            double tXY = Fast.hypot(aX, aY);
            double tCosTheta = aZ / aDis;
            double tSinTheta = tXY / aDis;
            double tCosPhi;
            double tSinPhi;
            // 注意避免 NaN 的情况
            if (Code.numericEqual(tXY, 0.0)) {
                tCosPhi = 1.0;
                tSinPhi = 0.0;
            } else {
                tCosPhi = aX / tXY;
                tSinPhi = aY / tXY;
            }
            realSphericalHarmonics2Dest4_(aL, tCosTheta, tSinTheta, tCosPhi, tSinPhi, rDest);
        }
        @ApiStatus.Internal
        public static void realSphericalHarmonics2Dest4_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aCosTheta, double aSinTheta, double aCosPhi, double aSinPhi, IVector rDest) {
            realNormalizedLegendre2Dest_(aL, aCosTheta, aSinTheta, rDest);
            // 现在 m = 0 的情况不需要设置了
            double tSinMmmPhi = 0.0;
            double tCosMmmPhi = 1.0;
            double tSinMPhi = aSinPhi;
            double tCosMPhi = aCosPhi;
            final double tCosPhi2 = tCosMPhi+tCosMPhi;
            for (int tM = 1; tM <= aL; ++tM) {
                // 对于实球谐函数这里的处理下需要多乘一个 sqrt(2)
                final double fSqrt2CosMPhi = SQRT2*tCosMPhi;
                final double fSqrt2SinMPhi = SQRT2*tSinMPhi;
                int tIdxPos =  tM+aL;
                int tIdxNeg = -tM+aL;
                rDest.update(tIdxPos, Plm -> fSqrt2CosMPhi*Plm);
                rDest.update(tIdxNeg, Plm -> fSqrt2SinMPhi*Plm);
                // 利用和差化积的递推公式来更新 tSinMPhi tCosMPhi
                double tSinMppPhi = tCosPhi2 * tSinMPhi - tSinMmmPhi;
                double tCosMppPhi = tCosPhi2 * tCosMPhi - tCosMmmPhi;
                tSinMmmPhi = tSinMPhi; tCosMmmPhi = tCosMPhi;
                tSinMPhi = tSinMppPhi; tCosMPhi = tCosMppPhi;
            }
        }
        private static void realNormalizedLegendre2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aL, double aX, double aY, IVector rDest) {
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
                setRealY_(rDest, 1, 1, -SQRT3DIV2 * aY * tPll);
                return;
            }
            default: {
                int tStartCDE = (aL+1)*aL/2;
                double tPll = SH_Elm.get(tStartCDE + aL);
                tPll *= Fast.powFast(aY, aL);
                
                // 特殊处理 aY == 0.0 的情况，避免出现 NaN，此时除了 Pl0 全部都会是 0.0，
                // 顺便也处理 aY ≈ 0 的情况，保证至少结果不会偏离太远，
                // 当然这样会导致结果出现较大误差，如果需要更高精度可以使用 Full 版本的算法
                if (Code.numericEqual(tPll, 0.0)) {
                    // 这里需要从 l 方向从 0 开始遍历，使用 rDest 暂存中间结果
                    double tP00 = 0.28209479177387814347403972578039;
                    rDest.set(0, tP00);
                    rDest.set(1, SQRT3 * aX * tP00);
                    int tLmm = 1, tLm2 = 0;
                    int tIdxAB = 3;
                    for (int tL = 2; tL <= aL; ++tL) {
                        double tPl0 = SH_Alm.get(tIdxAB) * (aX*rDest.get(tLmm) + SH_Blm.get(tIdxAB)*rDest.get(tLm2));
                        rDest.set(tL, tPl0);
                        tLm2 = tLmm;
                        tLmm = tL;
                        tIdxAB += tL+1;
                    }
                    // 然后清空前面暂存结果即可
                    for (int tM = 1; tM <= aL; ++tM) setRealY_(rDest, aL, tM, 0.0);
                    return;
                }
                
                if ((aL&1)==1) tPll = -tPll;
                tPll *= SH_FACTORIAL2_2L_PLUS_1.get(aL);
                
                double tXDivY = aX/aY;
                setRealY_(rDest, aL, aL, tPll);
                setRealY_(rDest, aL, aL-1, -tXDivY * SH_SQRT_2L.get(aL) * tPll);
                
                for (int tM = aL-2; tM >= 0; --tM) {
                    int tIdxCD = tStartCDE+tM;
                    double tPlm = tXDivY*SH_Clm.get(tIdxCD)*rDest.get(aL+tM+1) + SH_Dlm.get(tIdxCD)*rDest.get(aL+tM+2);
                    if (tM == 0) rDest.set(aL, tPlm);
                    else setRealY_(rDest, aL, tM, tPlm);
                }
                //noinspection UnnecessaryReturnStatement
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
        public static ComplexDouble sphericalHarmonics3(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aX, double aY, double aZ) {
            // 判断输入是否合法
            if (Math.abs(aM) > aL) throw new IllegalArgumentException("Input m MUST be in range -l ~ l, input: "+aM);
            return sphericalHarmonics3_(aL, aM, aX, aY, aZ);
        }
        public static void sphericalHarmonics2Dest(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aTheta, double aPhi, ISettableComplexDouble rDest) {
            // 判断输入是否合法
            if (Math.abs(aM) > aL) throw new IllegalArgumentException("Input m MUST be in range -l ~ l, input: "+aM);
            sphericalHarmonics2Dest_(aL, aM, Fast.cos(aTheta), aPhi, rDest);
        }
        public static void sphericalHarmonics2Dest3(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aX, double aY, double aZ, ISettableComplexDouble rDest) {
            // 判断输入是否合法
            if (Math.abs(aM) > aL) throw new IllegalArgumentException("Input m MUST be in range -l ~ l, input: "+aM);
            sphericalHarmonics2Dest3_(aL, aM, aX, aY, aZ, rDest);
        }
        private static ComplexDouble sphericalHarmonics_(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aTheta, double aPhi) {
            ComplexDouble rY = new ComplexDouble();
            sphericalHarmonics2Dest_(aL, aM, Fast.cos(aTheta), aPhi, rY);
            return rY;
        }
        private static ComplexDouble sphericalHarmonics3_(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aX, double aY, double aZ) {
            ComplexDouble rY = new ComplexDouble();
            sphericalHarmonics2Dest3_(aL, aM, aX, aY, aZ, rY);
            return rY;
        }
        private static void sphericalHarmonics2Dest3_(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aX, double aY, double aZ, ISettableComplexDouble rDest) {
            double tCosTheta = aZ / Fast.hypot(aX, aY, aZ);
            double tPhi = Fast.atan2(aY, aX);
            sphericalHarmonics2Dest_(aL, aM, tCosTheta, tPhi, rDest);
        }
        private static void sphericalHarmonics2Dest_(@Range(from = 0, to = SH_LARGEST_L) int aL, int aM, double aCosTheta, double aPhi, ISettableComplexDouble rDest) {
            if (aM < 0) {
                sphericalHarmonics2Dest_(aL, -aM, aCosTheta, aPhi, rDest);
                rDest.conj2this();
                if ((aM&1)==1) rDest.negative2this();
                return;
            }
            // 计算前系数（实数部分）
            double rFront = SH_Elm.get((aL+1)*aL/2 + aM);
            // 计算连带 Legendre 多项式部分
            rFront *= legendre_(aL, aM, aCosTheta);
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
            if (aM < -aL || aM > aL) throw new IllegalArgumentException("Input m MUST be in range -l ~ l, input: "+aM);
            if (aM >= 0) return legendre_(aL, aM, aX);
            aM = -aM;
            double rPlm = legendre_(aL, aM, aX);
            if ((aM&1)==1) rPlm = -rPlm;
            // 这里手动累乘而不去查表（因为这个累乘表初始化会很费时，并且 legendre_ 本身也在做这个）
            rPlm *= factorial(aL - aM);
            rPlm /= factorial(aL + aM);
            return rPlm;
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
            if (aM1 < -aJ1 || aM1 > aJ1) throw new IllegalArgumentException("Input m1 MUST be in range -j1 ~ j1, input: "+aM1);
            if (aM2 < -aJ1 || aM2 > aJ2) throw new IllegalArgumentException("Input m2 MUST be in range -j2 ~ j2, input: "+aM2);
            if (aM3 < -aJ1 || aM3 > aJ3) throw new IllegalArgumentException("Input m3 MUST be in range -j3 ~ j3, input: "+aM3);
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
         * @return {@code 1 * 2 * 3 * ... * (aN-1) * aN}
         */
        public static double factorial(int aN) {
            if (aN < 0) return 0.0;
            if (aN==0 || aN==1) return 1.0;
            double rProd = 1.0;
            for (int i = 2; i <= aN; ++i) rProd *= i;
            return rProd;
        }
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
         * 线性拟合得到 y = bx
         * @author liqa
         * @param aX x 数据组成的向量
         * @param aY y 数据组成的向量
         * @return 斜率 b
         */
        public static double lineFit0(IVector aX, IVector aY) {
            int tNx = aX.size();
            int tNy = aY.size();
            if (tNx != tNy) throw new IllegalArgumentException("Input x, y MUST have same size, input: ("+tNx+", "+tNy+")");
            return aX.operation().dot(aY) / aX.operation().dot();
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
        
        public static int floor2int(double aValue) {return FastMath.floorToInt(aValue);}
        public static int ceil2int(double aValue) {return FastMath.ceilToInt(aValue);}
        public static int round2int(double aValue) {return FastMath.roundToInt(aValue);}
        
        public static double toRange(double aMin, double aMax, double aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        public static int toRange(int aMin, int aMax, int aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        public static long toRange(long aMin, long aMax, long aValue) {return FastMath.toRange(aMin, aMax, aValue);}
        
        /** double compare */
        public static boolean numericEqual(double aLHS, double aRHS) {
            double tNorm = Math.abs(aLHS) + Math.abs(aRHS);
            if (tNorm < Double.MIN_NORMAL * EPS_MUL) return true; // 两个值都为零的情况，比这个值更小时乘以 epsilon() 会失效
            double tDiff = Math.abs(aLHS - aRHS);
            return tDiff <= tNorm * DBL_EPSILON;
        }
        public static boolean numericGreater(double aLHS, double aRHS) {
            double tNorm = Math.abs(aLHS) + Math.abs(aRHS);
            if (tNorm < Double.MIN_NORMAL * EPS_MUL) return false; // 两个值都为零的情况，比这个值更小时乘以 epsilon() 会失效
            return aLHS - aRHS > tNorm * DBL_EPSILON;
        }
        public static boolean numericLess(double aLHS, double aRHS) {
            double tNorm = Math.abs(aLHS) + Math.abs(aRHS);
            if (tNorm < Double.MIN_NORMAL * EPS_MUL) return false; // 两个值都为零的情况，比这个值更小时乘以 epsilon() 会失效
            return aRHS - aLHS > tNorm * DBL_EPSILON;
        }
        public final static int EPS_MUL = 8;
        /** {@link FastMath} will has lower accuracy */
        public final static double DBL_EPSILON = 1.0e-10;
        
        
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
                out = Code.ceil2int(tValue);
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
                int tOut = Code.ceil2int(tValue);
                if (tOut > aNum) return out;
                out = tOut;
            }
        }
    }
}
