package jse.math.function;

/**
 * @author liqa
 * <p> 一维数值函数的另一种实现，x0 处对称并且超出边界后为零 </p>
 */
public final class ZeroBoundSymmetryFunc1 extends DoubleArrayFunc1 implements IZeroBoundFunc1 {
    /** 在这里提供一些常用的构造 */
    public static ZeroBoundSymmetryFunc1 zeros(double aX0, double aDx, int aNx) {return new ZeroBoundSymmetryFunc1(aX0, aDx, new double[aNx]);}
    
    public ZeroBoundSymmetryFunc1(double aX0, double aDx, double[] aF) {super(aX0, aDx, aF);}
    public ZeroBoundSymmetryFunc1(double[] aX, double[] aF) {super(aX, aF);}
    
    /** DoubleArrayFunc1 stuffs */
    @Override protected double getOutL_(int aI) {
        aI = -aI;
        return (aI < Nx()) ? mData[aI] : 0.0;
    }
    @Override protected double getOutR_(int aI) {return 0.0;}
    
    /** 提供额外的接口用于检测两端 */
    @Override public double zeroBoundL() {return mX0 - Nx()*mDx;}
    @Override public double zeroBoundR() {return mX0 + Nx()*mDx;}
    
    @Override public ZeroBoundSymmetryFunc1 newShell() {return new ZeroBoundSymmetryFunc1(mX0, mDx, null);}
    @Override protected ZeroBoundSymmetryFunc1 newInstance_(double aX0, double aDx, double[] aData) {return new ZeroBoundSymmetryFunc1(aX0, aDx, aData);}
    
    
    /** 对于对称的函数，这些运算需要重新考虑 */
    @Override public DoubleArrayFunc1Operation operation() {
        return new DoubleArrayFunc1Operation_() {
            /** 对称函数的 laplacian 依旧是对称的，可以直接用 */
            @Override public IFunc1 laplacian() {
                IFunc1 rFunc1 = ZeroBoundSymmetryFunc1.zeros(mX0, mDx, Nx());
                laplacian2Dest_(rFunc1);
                return rFunc1;
            }
            
            /** 积分考虑对称性需要结果乘以 2 */
            @Override public double integral() {return super.integral() * 2.0;}
            
            /** 卷积考虑对称性需要结果乘以 2 */
            @Override public IFunc1Subs refConvolve(final IFunc2Subs aConv) {return k -> super.refConvolve(aConv).subs(k) * 2.0;}
            @Override public IFunc1Subs refConvolveFull(final IFunc3Subs aConv) {return k -> super.refConvolveFull(aConv).subs(k) * 2.0;}
        };
    }
}
