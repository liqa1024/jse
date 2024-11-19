package jse.math.function;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;

/**
 * @author liqa
 * <p> 一维数值函数的另一种实现，超出界限外使用 0 值，可以加速一些运算 </p>
 */
public final class ZeroBoundFunc1 extends VectorFunc1 implements IZeroBoundFunc1 {
    /** 在这里提供一些常用的构造 */
    public static ZeroBoundFunc1 zeros(double aX0, double aDx, int aNx) {return new ZeroBoundFunc1(aX0, aDx, Vector.zeros(aNx));}
    
    public ZeroBoundFunc1(double aX0, double aDx, Vector aF) {super(aX0, aDx, aF);}
    public ZeroBoundFunc1(IVector aX, IVector aF) {super(aX, aF);}
    
    /** DoubleArrayFunc1 stuffs */
    @Override public double subs(double aX) {
        int tI = MathEX.Code.ceil2int((aX-mX0)/mDx);
        int tImm = tI-1;
        
        double tX1 = getX(tImm);
        double tX2 = getX(tI);
        
        int tNx = Nx();
        
        double tF1 = (tImm<0 || tImm>=tNx) ? 0.0 : mData.get(tImm);
        double tF2 = (tI  <0 || tI  >=tNx) ? 0.0 : mData.get(tI);
        return MathEX.Func.interp1(tX1, tX2, tF1, tF2, aX);
    }
    @Override public int getINear(double aX) {return MathEX.Code.toRange(0, Nx()-1, super.getINear(aX));}
    
    /** 提供额外的接口用于检测两端 */
    @Override public double zeroBoundL() {return mX0 - mDx;}
    @Override public double zeroBoundR() {return mX0 + Nx()*mDx;}
    
    @Override public ZeroBoundFunc1 newShell() {return new ZeroBoundFunc1(mX0, mDx, null);}
    @Override protected ZeroBoundFunc1 newInstance_(double aX0, double aDx, Vector aData) {return new ZeroBoundFunc1(aX0, aDx, aData);}
}
