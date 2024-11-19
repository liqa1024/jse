package jse.math.function;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;

/**
 * @author liqa
 * <p> 一维数值函数的另一种实现，x0 处对称并且超出边界后为零 </p>
 */
public final class ZeroBoundSymmetryFunc1 extends VectorFunc1 implements IZeroBoundFunc1 {
    /** 在这里提供一些常用的构造 */
    public static ZeroBoundSymmetryFunc1 zeros(double aX0, double aDx, int aNx) {return new ZeroBoundSymmetryFunc1(aX0, aDx, Vector.zeros(aNx));}
    
    public ZeroBoundSymmetryFunc1(double aX0, double aDx, Vector aF) {super(aX0, aDx, aF);}
    public ZeroBoundSymmetryFunc1(IVector aX, IVector aF) {super(aX, aF);}
    
    /** DoubleArrayFunc1 stuffs */
    @Override public double subs(double aX) {
        int tI = MathEX.Code.ceil2int((aX-mX0)/mDx);
        int tImm = tI-1;
        
        double tX1 = getX(tImm);
        double tX2 = getX(tI);
        
        int tNx = Nx();
        
        if (tImm < 0) tImm = -tImm;
        if (tI   < 0) tI   = -tI;
        
        double tF1 = tImm>=tNx ? 0.0 : mData.get(tImm);
        double tF2 = tI  >=tNx ? 0.0 : mData.get(tI);
        return MathEX.Func.interp1(tX1, tX2, tF1, tF2, aX);
    }
    @Override public int getINear(double aX) {
        int tI = super.getINear(aX);
        if (tI < 0) tI = -tI;
        return Math.min(tI, Nx()-1);
    }
    
    /** 提供额外的接口用于检测两端 */
    @Override public double zeroBoundL() {return mX0 - Nx()*mDx;}
    @Override public double zeroBoundR() {return mX0 + Nx()*mDx;}
    
    @Override public ZeroBoundSymmetryFunc1 newShell() {return new ZeroBoundSymmetryFunc1(mX0, mDx, null);}
    @Override protected ZeroBoundSymmetryFunc1 newInstance_(double aX0, double aDx, Vector aData) {return new ZeroBoundSymmetryFunc1(aX0, aDx, aData);}
    
    
    /** 对于对称的函数，这些运算需要重新考虑 */
    @Override public VectorFunc1Operation operation() {
        return new VectorFunc1Operation_() {
            /** 对称函数的 laplacian 依旧是对称的，可以直接用 */
            @Override public IFunc1 laplacian(boolean aConsiderBound) {
                IFunc1 rFunc1 = ZeroBoundSymmetryFunc1.zeros(mX0, mDx, Nx());
                laplacian2Dest_(rFunc1, aConsiderBound);
                return rFunc1;
            }
            
            /** 积分考虑对称性需要结果乘以 2 */
            @Override public double integral(boolean aConsiderBoundL, boolean aConsiderBoundR) {return super.integral(aConsiderBoundL, aConsiderBoundR) * 2.0;}
            
            /** 卷积考虑对称性需要结果乘以 2 */
            @Override public IFunc1Subs refConvolve(final IFunc2Subs aConv, boolean aConsiderBoundL, boolean aConsiderBoundR) {return k -> super.refConvolve(aConv, aConsiderBoundL, aConsiderBoundR).subs(k) * 2.0;}
            @Override public IFunc1Subs refConvolveFull(final IFunc3Subs aConv, boolean aConsiderBoundL, boolean aConsiderBoundR) {return k -> super.refConvolveFull(aConv, aConsiderBoundL, aConsiderBoundR).subs(k) * 2.0;}
        };
    }
}
