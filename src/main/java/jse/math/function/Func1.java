package jse.math.function;

import jse.code.iterator.IHasDoubleIterator;
import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.IVectorGetter;

import java.util.Collection;

import static jse.math.MathEX.PI;

/**
 * @author liqa
 * <p> 现在修改为专门获取一维函数的类，默认获取 {@link PBCFunc1} </p>
 */
public class Func1 {
    private Func1() {}
    
    public static PBCFunc1 ones(double aX0, double aDx, int aNx) {PBCFunc1 rFunc = zeros(aX0, aDx, aNx); rFunc.fill(1.0); return rFunc;}
    public static PBCFunc1 zeros(double aX0, double aDx, int aNx) {return PBCFunc1.zeros(aX0, aDx, aNx);}
    public static UnequalIntervalFunc1 zeros(int aNx, IVectorGetter aXGetter) {return UnequalIntervalFunc1.zeros(aNx, aXGetter);}
    public static UnequalIntervalFunc1 zeros(IVector aX) {return zeros(aX.size(), aX);}
    
    public static PBCFunc1 from(double aX0, double aDx, int aNx, IFunc1Subs aFunc1Subs) {
        PBCFunc1 rFunc = zeros(aX0, aDx, aNx);
        rFunc.fill(aFunc1Subs);
        return rFunc;
    }
    public static PBCFunc1 from(double aX0, double aDx, int aNx, Iterable<? extends Number> aList) {
        PBCFunc1 rFunc = zeros(aX0, aDx, aNx);
        rFunc.fill(aList);
        return rFunc;
    }
    public static PBCFunc1 from(double aX0, double aDx, Collection<? extends Number> aList) {
        PBCFunc1 rFunc = zeros(aX0, aDx, aList.size());
        rFunc.fill(aList);
        return rFunc;
    }
    public static PBCFunc1 from(double aX0, double aDx, double[] aData) {
        PBCFunc1 rFunc = zeros(aX0, aDx, aData.length);
        rFunc.fill(aData);
        return rFunc;
    }
    public static PBCFunc1 from(double aX0, double aDx, IVector aVector) {
        PBCFunc1 rFunc = zeros(aX0, aDx, aVector.size());
        rFunc.fill(aVector);
        return rFunc;
    }
    /** 提供非均匀间距的构造 */
    public static UnequalIntervalFunc1 from(int aNx, IVectorGetter aXGetter, IFunc1Subs aFunc1Subs) {
        UnequalIntervalFunc1 rFunc = zeros(aNx, aXGetter);
        rFunc.fill(aFunc1Subs);
        return rFunc;
    }
    public static UnequalIntervalFunc1 from(IVector aX, IFunc1Subs aFunc1Subs) {
        UnequalIntervalFunc1 rFunc = zeros(aX);
        rFunc.fill(aFunc1Subs);
        return rFunc;
    }
    public static UnequalIntervalFunc1 from(IVector aX, IVector aF) {
        UnequalIntervalFunc1 rFunc = zeros(aX);
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
    public static ZeroBoundSymmetryFunc1 deltaG(double aSigma, final double aMu, double aResolution) {
        final double tXMul = -1.0 / (2.0*aSigma*aSigma);
        final double tYMul =  1.0 / (MathEX.Fast.sqrt(2.0*PI) * aSigma);
        
        ZeroBoundSymmetryFunc1 rFunc1 = ZeroBoundSymmetryFunc1.zeros(aMu, aSigma/aResolution, (int)Math.round(aResolution*G_RANG));
        rFunc1.fill(x -> {
            x -= aMu;
            return MathEX.Fast.exp(x * x * tXMul) * tYMul;
        });
        return rFunc1;
    }
    private final static int G_RANG = 6;
    
    
    /**
     * 根据指定数据生成此数据的分布，超出范围的值会忽略
     * @author liqa
     * @param aData 需要统计分布的数据
     * @param aStart 分布的下界
     * @param aEnd 分布的上界
     * @param aN 分划的份数
     * @return 得到的分布函数
     */
    public static ZeroBoundFunc1 distFrom(IHasDoubleIterator aData, double aStart, double aEnd, int aN) {
        final double tStep = (aEnd-aStart)/(double)(aN-1);
        final ZeroBoundFunc1 rFunc1 = ZeroBoundFunc1.zeros(aStart, tStep, aN);
        
        final double tLBound = aStart - tStep*0.5;
        final double tUBound = aEnd + tStep*0.5;
        final int[] rSize = {0};
        aData.forEach(v -> {
            ++rSize[0]; // 虽然说大部分输入数据都不用进行这个计数，不过保证简洁还是都做一下
            if (v>=tLBound && v<tUBound) rFunc1.updateNear(v, f->f+1);
        });
        
        rFunc1.div2this(rSize[0] * tStep);
        return rFunc1;
    }
    public static ZeroBoundFunc1 distFrom(Iterable<? extends Number> aDataList, double aStart, double aEnd, int aN) {return distFrom(IHasDoubleIterator.of(aDataList), aStart, aEnd, aN);}
    
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来统计分布，超出范围的值会忽略
     * @author liqa
     * @param aData 需要统计分布的数据
     * @param aStart 分布的下界
     * @param aEnd 分布的上界
     * @param aN 分划的份数
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return 得到的分布函数
     */
    public static ZeroBoundFunc1 distFrom_G(IHasDoubleIterator aData, double aStart, double aEnd, int aN, int aSigmaMul) {
        final double tStep = (aEnd-aStart)/(double)(aN-1);
        final ZeroBoundFunc1 rFunc1 = ZeroBoundFunc1.zeros(aStart, tStep, aN);
        // 用于累加的 DeltaG
        final IZeroBoundFunc1 tDeltaG = deltaG(tStep*aSigmaMul, 0.0, aSigmaMul);
        
        final double tLBound = aStart - tDeltaG.zeroBoundR();
        final double tUBound = aEnd - tDeltaG.zeroBoundL();
        final int[] rSize = {0};
        aData.forEach(v -> {
            ++rSize[0]; // 虽然说大部分输入数据都不用进行这个计数，不过保证简洁还是都做一下
            if (v>=tLBound && v<tUBound) {
                tDeltaG.setX0(v);
                rFunc1.plus2this(tDeltaG);
            }
        });
        
        rFunc1.div2this(rSize[0]);
        return rFunc1;
    }
    public static ZeroBoundFunc1 distFrom_G(IHasDoubleIterator aData, double aStart, double aEnd, int aN) {return distFrom_G(aData, aStart, aEnd, aN, 4);}
    public static ZeroBoundFunc1 distFrom_G(Iterable<? extends Number> aDataList, double aStart, double aEnd, int aN, int aSigmaMul) {return distFrom_G(IHasDoubleIterator.of(aDataList), aStart, aEnd, aN, aSigmaMul);}
    public static ZeroBoundFunc1 distFrom_G(Iterable<? extends Number> aDataList, double aStart, double aEnd, int aN) {return distFrom_G(IHasDoubleIterator.of(aDataList), aStart, aEnd, aN);}
}
