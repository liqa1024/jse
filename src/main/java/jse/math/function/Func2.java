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
    public static PBCFunc2 distFrom(IHasDoubleIterator aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {
        double tStepX = (aEndX-aStartX)/(double)(aNx-1);
        double tStepY = (aEndY-aStartY)/(double)(aNy-1);
        PBCFunc2 rFunc2 = zeros(aStartX, aStartY, tStepX, tStepY, aNx, aNy);
        
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
    public static PBCFunc2 distFrom(IHasDoubleIterator aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom(aDataX, IHasDoubleIterator.of(aDataY), aStartX, aStartY, aEndX, aEndY, aNx, aNy);}
    public static PBCFunc2 distFrom(Iterable<? extends Number> aDataX, IHasDoubleIterator aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom(IHasDoubleIterator.of(aDataX), aDataY, aStartX, aStartY, aEndX, aEndY, aNx, aNy);}
    public static PBCFunc2 distFrom(Iterable<? extends Number> aDataX, Iterable<? extends Number> aDataY, double aStartX, double aStartY, double aEndX, double aEndY, int aNx, int aNy) {return distFrom(IHasDoubleIterator.of(aDataX), IHasDoubleIterator.of(aDataY), aStartX, aStartY, aEndX, aEndY, aNx, aNy);}
    
    public static PBCFunc2 distFrom(IHasDoubleIterator aDataX, IHasDoubleIterator aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static PBCFunc2 distFrom(IHasDoubleIterator aDataX, Iterable<? extends Number> aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static PBCFunc2 distFrom(Iterable<? extends Number> aDataX, IHasDoubleIterator aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
    public static PBCFunc2 distFrom(Iterable<? extends Number> aDataX, Iterable<? extends Number> aDataY, double aStart, double aEnd, int aN) {return distFrom(aDataX, aDataY, aStart, aStart, aEnd, aEnd, aN, aN);}
}
