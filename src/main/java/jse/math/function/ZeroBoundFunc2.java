package jse.math.function;

import jse.math.MathEX;
import jse.math.matrix.ColumnMatrix;

/**
 * @author liqa
 * <p> 二维维数值函数的另一种实现，超出界限外使用 0 值，可以加速一些运算 </p>
 */
@SuppressWarnings("SuspiciousNameCombination")
public final class ZeroBoundFunc2 extends ColumnMatrixFunc2 implements IZeroBoundFunc2 {
    /** 在这里提供一些常用的构造 */
    public static ZeroBoundFunc2 zeros(double aX0, double aDx, int aN) {return new ZeroBoundFunc2(aX0, aX0, aDx, aDx, ColumnMatrix.zeros(aN));}
    public static ZeroBoundFunc2 zeros(double aX0, double aY0, double aDx, double aDy, int aNx, int aNy) {return new ZeroBoundFunc2(aX0, aY0, aDx, aDy, ColumnMatrix.zeros(aNx, aNy));}
    public ZeroBoundFunc2(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData) {super(aX0, aY0, aDx, aDy, aData);}
    
    
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
        
        double tF11 = (tImm<0 || tImm>=tNx || tJmm<0 || tJmm>=tNy) ? 0.0 : mData.get(tImm, tJmm);
        double tF12 = (tImm<0 || tImm>=tNx || tJ  <0 || tJ  >=tNy) ? 0.0 : mData.get(tImm, tJ  );
        double tF21 = (tI  <0 || tI  >=tNx || tJmm<0 || tJmm>=tNy) ? 0.0 : mData.get(tI  , tJmm);
        double tF22 = (tI  <0 || tI  >=tNx || tJ  <0 || tJ  >=tNy) ? 0.0 : mData.get(tI  , tJ  );
        return MathEX.Func.interp1(tY1, tY2,
                                   MathEX.Func.interp1(tX1, tX2, tF11, tF21, aX),
                                   MathEX.Func.interp1(tX1, tX2, tF12, tF22, aX),
                                   aY);
    }
    
    @Override public int getINear(double aX) {return MathEX.Code.toRange(0, Nx()-1, super.getINear(aX));}
    @Override public int getJNear(double aY) {return MathEX.Code.toRange(0, Ny()-1, super.getJNear(aY));}
    
    /** 提供额外的接口用于检测两端 */
    @Override public double zeroBoundNegX() {return mX0 - mDx;}
    @Override public double zeroBoundPosX() {return mX0 + Nx()*mDx;}
    @Override public double zeroBoundNegY() {return mY0 - mDy;}
    @Override public double zeroBoundPosY() {return mY0 + Ny()*mDy;}
    
    @Override public ZeroBoundFunc2 newShell() {return new ZeroBoundFunc2(mX0, mY0, mDx, mDy, null);}
    @Override protected ZeroBoundFunc2 newInstance_(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData) {return new ZeroBoundFunc2(aX0, aY0, aDx, aDy, aData);}
}
