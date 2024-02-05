package jse.math.function;

/**
 * @author liqa
 * <p> 一维数值函数的另一种实现，超出界限外使用 0 值，可以加速一些运算 </p>
 */
public final class ZeroBoundFunc1 extends DoubleArrayFunc1 implements IZeroBoundFunc1 {
    /** 在这里提供一些常用的构造 */
    public static ZeroBoundFunc1 zeros(double aX0, double aDx, int aNx) {return new ZeroBoundFunc1(aX0, aDx, new double[aNx]);}
    
    public ZeroBoundFunc1(double aX0, double aDx, double[] aF) {super(aX0, aDx, aF);}
    public ZeroBoundFunc1(double[] aX, double[] aF) {super(aX, aF);}
    
    /** DoubleArrayFunc1 stuffs */
    @Override protected double getOutL_(int aI) {return 0.0;}
    @Override protected double getOutR_(int aI) {return 0.0;}
    
    /** 提供额外的接口用于检测两端 */
    @Override public double zeroBoundL() {return mX0 - mDx;}
    @Override public double zeroBoundR() {return mX0 + Nx()*mDx;}
    
    @Override public ZeroBoundFunc1 newShell() {return new ZeroBoundFunc1(mX0, mDx, null);}
    @Override protected ZeroBoundFunc1 newInstance_(double aX0, double aDx, double[] aData) {return new ZeroBoundFunc1(aX0, aDx, aData);}
}
