package jse.math.function;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;

/**
 * @author liqa
 * <p> 一维数值函数的另一种实现，超出界限外使用边界的常量值，作为默认实现可以避免一些超出边界的问题 </p>
 */
public final class ConstBoundFunc1 extends VectorFunc1 {
    /** 在这里提供一些常用的构造 */
    public static ConstBoundFunc1 zeros(double aX0, double aDx, int aNx) {return new ConstBoundFunc1(aX0, aDx, Vector.zeros(aNx));}
    
    public ConstBoundFunc1(double aX0, double aDx, Vector aF) {super(aX0, aDx, aF);}
    public ConstBoundFunc1(IVector aX, IVector aF) {super(aX, aF);}
    
    /** DoubleArrayFunc1 stuffs */
    @Override public double subs(double aX) {
        int tI = MathEX.Code.ceil2int((aX-mX0)/mDx);
        int tImm = tI-1;
        
        double tX1 = getX(tImm);
        double tX2 = getX(tI);
        
        int tNx = Nx();
        
        tImm = MathEX.Code.toRange(0, tNx-1, tImm);
        tI   = MathEX.Code.toRange(0, tNx-1, tI  );
        return MathEX.Func.interp1(tX1, tX2, mData.get(tImm), mData.get(tI), aX);
    }
    @Override public int getINear(double aX) {return MathEX.Code.toRange(0, Nx()-1, super.getINear(aX));}
    
    @Override public ConstBoundFunc1 newShell() {return new ConstBoundFunc1(mX0, mDx, null);}
    @Override protected ConstBoundFunc1 newInstance_(double aX0, double aDx, Vector aData) {return new ConstBoundFunc1(aX0, aDx, aData);}
}
