package jse.math.function;

import jse.math.MathEX;
import jse.math.matrix.ColumnMatrix;

/**
 * @author liqa
 * <p> 一维数值函数默认实现，超出界限外认为是周期边界条件 </p>
 */
@SuppressWarnings("SuspiciousNameCombination")
public final class PBCFunc2 extends ColumnMatrixFunc2 {
    /** 在这里提供一些常用的构造 */
    public static PBCFunc2 zeros(double aX0, double aDx, int aN) {return new PBCFunc2(aX0, aX0, aDx, aDx, ColumnMatrix.zeros(aN));}
    public static PBCFunc2 zeros(double aX0, double aY0, double aDx, double aDy, int aNx, int aNy) {return new PBCFunc2(aX0, aY0, aDx, aDy, ColumnMatrix.zeros(aNx, aNy));}
    public PBCFunc2(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData) {super(aX0, aY0, aDx, aDy, aData);}
    
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
        
        if      (tI   <  0  ) {do {tI   += tNx;} while (tI   <  0  );}
        else if (tI   >= tNx) {do {tI   -= tNx;} while (tI   >= tNx);}
        if      (tJ   <  0  ) {do {tJ   += tNy;} while (tJ   <  0  );}
        else if (tJ   >= tNy) {do {tJ   -= tNy;} while (tJ   >= tNy);}
        if      (tImm <  0  ) {do {tImm += tNx;} while (tImm <  0  );}
        else if (tImm >= tNx) {do {tImm -= tNx;} while (tImm >= tNx);}
        if      (tJmm <  0  ) {do {tJmm += tNy;} while (tJmm <  0  );}
        else if (tJmm >= tNy) {do {tJmm -= tNy;} while (tJmm >= tNy);}
        return MathEX.Func.interp1(tY1, tY2,
                                   MathEX.Func.interp1(tX1, tX2, mData.get(tImm, tJmm), mData.get(tI, tJmm), aX),
                                   MathEX.Func.interp1(tX1, tX2, mData.get(tImm, tJ  ), mData.get(tI, tJ  ), aX),
                                   aY);
    }
    
    @Override public PBCFunc2 newShell() {return new PBCFunc2(mX0, mY0, mDx, mDy, null);}
    @Override protected PBCFunc2 newInstance_(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData) {return new PBCFunc2(aX0, aY0, aDx, aDy, aData);}
}
