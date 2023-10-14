package com.jtool.math.function;

/**
 * @author liqa
 * <p> 一维数值函数的另一种实现，超出界限外使用固定的值 </p>
 */
public final class FixBoundFunc1 extends DoubleArrayFunc1 {
    /** 在这里提供一些常用的构造 */
    public static FixBoundFunc1 zeros(double aX0, double aDx, int aNx) {return new FixBoundFunc1(aX0, aDx, new double[aNx]);}
    
    private double mBoundL, mBoundR;
    
    public FixBoundFunc1(double aX0, double aDx, double[] aF) {super(aX0, aDx, aF); mBoundL = mBoundR = 0.0;}
    public FixBoundFunc1(double[] aX, double[] aF) {super(aX, aF); mBoundL = mBoundR = 0.0;}
    
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
    @Override protected double getOutL_(int aI) {return mBoundL;}
    @Override protected double getOutR_(int aI) {return mBoundR;}
    
    @Override public FixBoundFunc1 newShell() {return new FixBoundFunc1(mX0, mDx, null).setBound(mBoundL, mBoundR);}
    @Override protected FixBoundFunc1 newInstance_(double aX0, double aDx, double[] aData) {return new FixBoundFunc1(aX0, aDx, aData).setBound(mBoundL, mBoundR);}
}
