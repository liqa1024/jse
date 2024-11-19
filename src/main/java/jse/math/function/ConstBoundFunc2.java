package jse.math.function;

import jse.math.MathEX;
import jse.math.matrix.ColumnMatrix;

/**
 * @author liqa
 * <p> 二维维数值函数的另一种实现，超出界限外使用边界的常量值，作为默认实现可以避免一些超出边界的问题 </p>
 */
@SuppressWarnings("SuspiciousNameCombination")
public final class ConstBoundFunc2 extends ColumnMatrixFunc2 {
    /** 在这里提供一些常用的构造 */
    public static ConstBoundFunc2 zeros(double aX0, double aDx, int aN) {return new ConstBoundFunc2(aX0, aX0, aDx, aDx, ColumnMatrix.zeros(aN));}
    public static ConstBoundFunc2 zeros(double aX0, double aY0, double aDx, double aDy, int aNx, int aNy) {return new ConstBoundFunc2(aX0, aY0, aDx, aDy, ColumnMatrix.zeros(aNx, aNy));}
    public ConstBoundFunc2(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData) {super(aX0, aY0, aDx, aDy, aData);}
    
    
    /** 获取结果，支持按照索引查找和按照 x 的值来查找 */
    @Override public double subs(double aX, double aY) {
        int tI = MathEX.Code.ceil2int((aX-mX0)/mDx);
        int tJ = MathEX.Code.ceil2int((aY-mY0)/mDy);
        int tImm = tI-1;
        int tJmm = tJ-1;
        
        double tX1 = getX(tImm);
        double tX2 = getX(tI);
        double tY1 = getY(tJmm);
        double tY2 = getY(tJ);
        
        int tNx = Nx();
        int tNy = Ny();
        
        tImm = MathEX.Code.toRange(0, tNx-1, tImm);
        tJmm = MathEX.Code.toRange(0, tNy-1, tJmm);
        tI   = MathEX.Code.toRange(0, tNx-1, tI  );
        tJ   = MathEX.Code.toRange(0, tNy-1, tJ  );
        return MathEX.Func.interp1(tY1, tY2,
                                   MathEX.Func.interp1(tX1, tX2, mData.get(tImm, tJmm), mData.get(tI, tJmm), aX),
                                   MathEX.Func.interp1(tX1, tX2, mData.get(tImm, tJ  ), mData.get(tI, tJ  ), aX),
                                   aY);
    }
    
    @Override public int getINear(double aX) {return MathEX.Code.toRange(0, Nx()-1, super.getINear(aX));}
    @Override public int getJNear(double aY) {return MathEX.Code.toRange(0, Ny()-1, super.getJNear(aY));}
    
    @Override public ConstBoundFunc2 newShell() {return new ConstBoundFunc2(mX0, mY0, mDx, mDy, null);}
    @Override protected ConstBoundFunc2 newInstance_(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData) {return new ConstBoundFunc2(aX0, aY0, aDx, aDy, aData);}
}
