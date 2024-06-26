package jse.math.function;

import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IHasDoubleIterator;
import jse.math.MathEX;

import static jse.math.MathEX.PI;

/**
 * @author liqa
 * <p> 现在修改为专门获取一维函数的类，默认获取 {@link PBCFunc2} </p>
 */
public class Func2 {
    private Func2() {}
    
    public static PBCFunc2 zeros(double aX0, double aDx, int aNx) {return PBCFunc2.zeros(aX0, aDx, aNx);}
    public static PBCFunc2 zeros(double aX0, double aY0, double aDx, double aDy, int aNx, int aNy) {return PBCFunc2.zeros(aX0, aY0, aDx, aDy, aNx, aNy);}
    
    
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
    public static ZeroBoundSymmetryFunc2 deltaG(double aSigma, final double aMu, double aResolution) {
        final double tMul = -1.0 / (2.0*aSigma*aSigma);
        final double tFMul =  1.0 / (MathEX.Fast.sqrt(2.0*PI) * aSigma * aSigma);
        
        ZeroBoundSymmetryFunc2 rFunc1 = ZeroBoundSymmetryFunc2.zeros(aMu, aSigma/aResolution, MathEX.Code.round2int(aResolution*G_RANG));
        rFunc1.fill((x, y) -> {
            x -= aMu;
            y -= aMu;
            return MathEX.Fast.exp(x*x*tMul + y*y*tMul) * tFMul;
        });
        return rFunc1;
    }
    private final static int G_RANG = 6;
    
    
    /**
     * 根据指定数据生成此数据的分布，超出范围的值会忽略
     * @author liqa
     * @param aDataX 需要统计分布的 x 数据
     * @param aDataY 需要统计分布的 y 数据
     * @param aStartX 分布的 x 下界
     * @param aStartY 分布的 y 下界
     * @param aEndX 分布的 x 上界
     * @param aEndY 分布的 y 上界
     * @param aNx 分划的 x 份数
     * @param aNy 分划的 y 份数
     * @return 得到的二维分布函数
     */
    public static ZeroBoundFunc2 distFrom(IHasDoubleIterator aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {
        double tStepX = (aEndX-aStartX)/(double)(aNx-1);
        double tStepY = (aEndY-aStartY)/(double)(aNy-1);
        ZeroBoundFunc2 rFunc2 = ZeroBoundFunc2.zeros(aStartX, aStartY, tStepX, tStepY, aNx, aNy);
        
        double tLBoundX = aStartX - tStepX*0.5;
        double tLBoundY = aStartY - tStepY*0.5;
        double tUBoundX = aEndX + tStepX*0.5;
        double tUBoundY = aEndY + tStepY*0.5;
        int rSize = 0;
        
        IDoubleIterator itX = aDataX.iterator(), itY = aDataY.iterator();
        while (itX.hasNext() && itY.hasNext()) {
            double tX = itX.next(), tY = itY.next();
            ++rSize; // 虽然说大部分输入数据都不用进行这个计数，不过保证简洁还是都做一下
            if (tX>=tLBoundX && tY>=tLBoundY
             && tX< tUBoundX && tY< tUBoundY) {
                rFunc2.updateNear(tX, tY, f->f+1);
            }
        }
        rFunc2.div2this(rSize * tStepX * tStepY);
        return rFunc2;
    }
    public static ZeroBoundFunc2 distFrom(IHasDoubleIterator aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom(aDataX, IHasDoubleIterator.of(aDataY), aStartX, aStartY, aEndX, aEndY, aNx, aNy);}
    public static ZeroBoundFunc2 distFrom(Iterable<? extends Number> aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom(IHasDoubleIterator.of(aDataX), aDataY, aStartX, aStartY, aEndX, aEndY, aNx, aNy);}
    public static ZeroBoundFunc2 distFrom(Iterable<? extends Number> aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom(IHasDoubleIterator.of(aDataX), IHasDoubleIterator.of(aDataY), aStartX, aStartY, aEndX, aEndY, aNx, aNy);}
    
    public static ZeroBoundFunc2 distFrom(IHasDoubleIterator aDataX, IHasDoubleIterator aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static ZeroBoundFunc2 distFrom(IHasDoubleIterator aDataX, Iterable<? extends Number> aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static ZeroBoundFunc2 distFrom(Iterable<? extends Number> aDataX, IHasDoubleIterator aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static ZeroBoundFunc2 distFrom(Iterable<? extends Number> aDataX, Iterable<? extends Number> aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    
    
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来统计分布，超出范围的值会忽略
     * @author liqa
     * @param aDataX 需要统计分布的 x 数据
     * @param aDataY 需要统计分布的 y 数据
     * @param aStartX 分布的 x 下界
     * @param aStartY 分布的 y 下界
     * @param aEndX 分布的 x 上界
     * @param aEndY 分布的 y 上界
     * @param aNx 分划的 x 份数
     * @param aNy 分划的 y 份数
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return 得到的分布函数
     */
    public static ZeroBoundFunc2 distFrom_G(IHasDoubleIterator aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy, int aSigmaMul) {
        double tStepX = (aEndX-aStartX)/(double)(aNx-1);
        double tStepY = (aEndY-aStartY)/(double)(aNy-1);
        ZeroBoundFunc2 rFunc2 = ZeroBoundFunc2.zeros(aStartX, aStartY, tStepX, tStepY, aNx, aNy);
        // 用于累加的 DeltaG
        final IZeroBoundFunc2 tDeltaG = deltaG(MathEX.Fast.sqrt(tStepX*tStepY)*aSigmaMul, 0.0, aSigmaMul);
        
        final double tLBoundX = aStartX - tDeltaG.zeroBoundNegX();
        final double tLBoundY = aStartY - tDeltaG.zeroBoundNegY();
        final double tUBoundX = aEndX - tDeltaG.zeroBoundPosX();
        final double tUBoundY = aEndY - tDeltaG.zeroBoundPosY();
        int rSize = 0;
        
        IDoubleIterator itX = aDataX.iterator(), itY = aDataY.iterator();
        while (itX.hasNext() && itY.hasNext()) {
            double tX = itX.next(), tY = itY.next();
            ++rSize; // 虽然说大部分输入数据都不用进行这个计数，不过保证简洁还是都做一下
            if (tX>=tLBoundX && tY>=tLBoundY
             && tX< tUBoundX && tY< tUBoundY) {
                tDeltaG.setX0(tX);
                tDeltaG.setY0(tY);
                rFunc2.plus2this(tDeltaG);
            }
        }
        rFunc2.div2this(rSize);
        return rFunc2;
    }
    public static ZeroBoundFunc2 distFrom_G(IHasDoubleIterator aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy, int aSigmaMul) {return distFrom_G(aDataX, IHasDoubleIterator.of(aDataY), aStartX, aStartY, aEndX, aEndY, aNx, aNy, aSigmaMul);}
    public static ZeroBoundFunc2 distFrom_G(Iterable<? extends Number> aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy, int aSigmaMul) {return distFrom_G(IHasDoubleIterator.of(aDataX), aDataY, aStartX, aStartY, aEndX, aEndY, aNx, aNy, aSigmaMul);}
    public static ZeroBoundFunc2 distFrom_G(Iterable<? extends Number> aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy, int aSigmaMul) {return distFrom_G(IHasDoubleIterator.of(aDataX), IHasDoubleIterator.of(aDataY), aStartX, aStartY, aEndX, aEndY, aNx, aNy, aSigmaMul);}
    
    public static ZeroBoundFunc2 distFrom_G(IHasDoubleIterator aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom_G(aDataX, aDataY, aStartX, aStartY, aEndX, aEndY, aNx, aNy, 4);}
    public static ZeroBoundFunc2 distFrom_G(IHasDoubleIterator aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom_G(aDataX, aDataY, aStartX, aStartY, aEndX, aEndY, aNx, aNy, 4);}
    public static ZeroBoundFunc2 distFrom_G(Iterable<? extends Number> aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom_G(aDataX, aDataY, aStartX, aStartY, aEndX, aEndY, aNx, aNy, 4);}
    public static ZeroBoundFunc2 distFrom_G(Iterable<? extends Number> aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom_G(aDataX, aDataY, aStartX, aStartY, aEndX, aEndY, aNx, aNy, 4);}
    
    public static ZeroBoundFunc2 distFrom_G(IHasDoubleIterator aDataX, IHasDoubleIterator aDataY, double aStart, double aEnd, int aN) {return distFrom_G(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static ZeroBoundFunc2 distFrom_G(IHasDoubleIterator aDataX, Iterable<? extends Number> aDataY, double aStart, double aEnd, int aN) {return distFrom_G(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static ZeroBoundFunc2 distFrom_G(Iterable<? extends Number> aDataX, IHasDoubleIterator aDataY, double aStart, double aEnd, int aN) {return distFrom_G(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static ZeroBoundFunc2 distFrom_G(Iterable<? extends Number> aDataX, Iterable<? extends Number> aDataY, double aStart, double aEnd, int aN) {return distFrom_G(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
}
