package jtool.math.function;

import jtool.math.MathEX;
import jtool.math.vector.IVector;
import jtool.math.vector.IVectorGetter;

import java.util.Collection;

import static jtool.math.MathEX.PI;

/**
 * @author liqa
 * <p> 现在修改为专门获取一维函数的类，默认获取 {@link PBCFunc1} </p>
 */
public class Func1 {
    private Func1() {}
    
    public static IFunc1 ones(double aX0, double aDx, int aNx) {IFunc1 rFunc = zeros(aX0, aDx, aNx); rFunc.fill(1.0); return rFunc;}
    public static IFunc1 zeros(double aX0, double aDx, int aNx) {return PBCFunc1.zeros(aX0, aDx, aNx);}
    public static IFunc1 zeros(int aNx, IVectorGetter aXGetter) {return UnequalIntervalFunc1.zeros(aNx, aXGetter);}
    public static IFunc1 zeros(IVector aX) {return zeros(aX.size(), aX);}
    
    public static IFunc1 from(double aX0, double aDx, int aNx, IFunc1Subs aFunc1Subs) {
        IFunc1 rFunc = zeros(aX0, aDx, aNx);
        rFunc.fill(aFunc1Subs);
        return rFunc;
    }
    public static IFunc1 from(double aX0, double aDx, int aNx, Iterable<? extends Number> aList) {
        IFunc1 rFunc = zeros(aX0, aDx, aNx);
        rFunc.fill(aList);
        return rFunc;
    }
    public static IFunc1 from(double aX0, double aDx, Collection<? extends Number> aList) {
        IFunc1 rFunc = zeros(aX0, aDx, aList.size());
        rFunc.fill(aList);
        return rFunc;
    }
    public static IFunc1 from(double aX0, double aDx, double[] aData) {
        IFunc1 rFunc = zeros(aX0, aDx, aData.length);
        rFunc.fill(aData);
        return rFunc;
    }
    public static IFunc1 from(double aX0, double aDx, IVector aVector) {
        IFunc1 rFunc = zeros(aX0, aDx, aVector.size());
        rFunc.fill(aVector);
        return rFunc;
    }
    /** 提供非均匀间距的构造 */
    public static IFunc1 from(int aNx, IVectorGetter aXGetter, IFunc1Subs aFunc1Subs) {
        IFunc1 rFunc = zeros(aNx, aXGetter);
        rFunc.fill(aFunc1Subs);
        return rFunc;
    }
    public static IFunc1 from(IVector aX, IFunc1Subs aFunc1Subs) {
        IFunc1 rFunc = zeros(aX);
        rFunc.fill(aFunc1Subs);
        return rFunc;
    }
    public static IFunc1 from(IVector aX, IVector aF) {
        IFunc1 rFunc = zeros(aX);
        rFunc.fill(aF);
        return rFunc;
    }
    
    
    
    /**
     * Get the Dirac Delta function δ(x-mu) in the Gaussian form,
     * result will in [-aDx*aN, aDx*aN], so out.length == 2*N+1
     * <p>
     * Optimized for vector operations
     * <p>
     * 为了保证 MathEX.Func 中都为直接返回函数值的特殊函数，直接获取数值函数的方法统一移动到这里
     * @author liqa
     * @param aSigma the standard deviation of the Gaussian distribution
     * @param aMu the mean value of the Gaussian distribution
     * @param aResolution the Resolution of the Function1, dx == aSigma/aResolution
     * @return the Dirac Delta function δ(x-mu) in the Gaussian form
     */
    public static IZeroBoundFunc1 deltaG(double aSigma, final double aMu, double aResolution) {
        final double tXMul = -1.0 / (2.0*aSigma*aSigma);
        final double tYMul =  1.0 / (MathEX.Fast.sqrt(2.0*PI) * aSigma);
        
        IZeroBoundFunc1 rFunc1 = ZeroBoundSymmetryFunc1.zeros(aMu, aSigma/aResolution, (int)Math.round(aResolution*G_RANG));
        rFunc1.fill(x -> {
            x -= aMu;
            return MathEX.Fast.exp(x * x * tXMul) * tYMul;
        });
        return rFunc1;
    }
    private final static int G_RANG = 6;
}
