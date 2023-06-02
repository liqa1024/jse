package com.jtool.math.function;

/**
 * @author liqa
 * <p> 一维数值函数默认实现，超出界限外认为是周期边界条件 </p>
 */
public final class PBCFunc1 extends DoubleArrayFunc1 {
    /** 在这里提供一些常用的构造 */
    public static PBCFunc1 zeros(double aX0, double aDx, int aNx) {return new PBCFunc1(aX0, aDx, new double[aNx]);}
    
    public PBCFunc1(double aX0, double aDx, double[] aF) {super(aX0, aDx, aF);}
    public PBCFunc1(double[] aX, double[] aF) {super(aX, aF);}
    public PBCFunc1(double aX0, double aDx, int aNx, IFunc1Subs aFunc) {super(aX0, aDx, aNx, aFunc);}
    
    
    /** DoubleArrayFunc1 stuffs */
    @Override protected double getOutL_(int aI) {
        int tNx = Nx();
        while (aI < 0) aI += tNx;
        return mData[aI];
    }
    @Override protected double getOutR_(int aI) {
        int tNx = Nx();
        while (aI >= tNx) aI -= tNx;
        return mData[aI];
    }
    
    @Override public PBCFunc1 newShell() {return new PBCFunc1(mX0, mDx, null);}
    @Override protected PBCFunc1 newInstance_(double aX0, double aDx, double[] aData) {return new PBCFunc1(aX0, aDx, aData);}
}
