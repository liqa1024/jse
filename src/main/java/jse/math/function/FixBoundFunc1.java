package jse.math.function;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;

/**
 * @author liqa
 * <p> 一维数值函数的另一种实现，超出界限外使用固定的值 </p>
 */
public final class FixBoundFunc1 extends VectorFunc1 {
    /** 在这里提供一些常用的构造 */
    public static FixBoundFunc1 zeros(double aX0, double aDx, int aNx) {return new FixBoundFunc1(aX0, aDx, Vector.zeros(aNx));}
    
    private double mBoundL, mBoundR;
    
    public FixBoundFunc1(double aX0, double aDx, Vector aF) {super(aX0, aDx, aF); mBoundL = mBoundR = 0.0;}
    public FixBoundFunc1(IVector aX, IVector aF) {super(aX, aF); mBoundL = mBoundR = 0.0;}
    
    /** 通过链式调用来设置边界 */
    public FixBoundFunc1 setBound(double aBound) {
        mBoundL = mBoundR = aBound;
        return this;
    }
    public FixBoundFunc1 setBound(double aBoundL, double aBoundR) {
        mBoundL = aBoundL;
        mBoundR = aBoundR;
        return this;
    }
    
    /** DoubleArrayFunc1 stuffs */
    @Override public double subs(double aX) {
        int tI = MathEX.Code.ceil2int((aX-mX0)/mDx);
        int tImm = tI-1;
        
        double tX1 = getX(tImm);
        double tX2 = getX(tI);
        
        int tNx = Nx();
        
        double tF1 = tImm<0 ? mBoundL : (tImm>=tNx ? mBoundR : mData.get(tImm));
        double tF2 = tI  <0 ? mBoundL : (tI  >=tNx ? mBoundR : mData.get(tI));
        return MathEX.Func.interp1(tX1, tX2, tF1, tF2, aX);
    }
    @Override public int getINear(double aX) {return MathEX.Code.toRange(0, Nx()-1, super.getINear(aX));}
    
    @Override public FixBoundFunc1 newShell() {return new FixBoundFunc1(mX0, mDx, null).setBound(mBoundL, mBoundR);}
    @Override protected FixBoundFunc1 newInstance_(double aX0, double aDx, Vector aData) {return new FixBoundFunc1(aX0, aDx, aData).setBound(mBoundL, mBoundR);}
}
