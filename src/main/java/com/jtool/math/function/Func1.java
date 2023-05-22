package com.jtool.math.function;

import com.jtool.math.MathEX;
import com.jtool.math.operator.IOperator1;

/**
 * @author liqa
 * <p> 数值函数，一维输入（f(x)），内部使用向量存储，对于中间值使用线性插值给出结果 </p>
 * <p> 要求 x 是等间距排列的，主要是为了加速查找过程 </p>
 */
public class Func1 {
    private final double mX0;
    private final double mDx;
    private double[] mData;
    
    
    /** 提供更加灵活的使用方法，主要用来方便快速在 double[] 和 Func 之间进行转换并且不创建多余的对象 */
    public Func1 setData(double[] aData) {mData = aData; return this;}
    public Func1 shell() {return new Func1(mX0, mDx, null);}
    
    public Func1(double aX0, double aDx, double[] aF) {
        mX0 = aX0; mDx = aDx; mData = aF;
    }
    /** 插值存储，因此不等间距排列也可以，精度会受到影响 */
    public Func1(double[] aX, double[] aF) {
        mX0 = aX[0];
        mDx = (aX[aX.length-1] - aX[0])/(aX.length-1);
        mData = new double[aX.length];
        mData[0] = aF[0];
        
        double tX = mX0;
        int tI = 1;
        for (int i = 1; i < aX.length; ++i) {
            tX += mDx;
            while (tI < aX.length-1 && tX >= aX[tI]) ++tI;
            mData[i] = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1], aF[tI], tX);
        }
    }
    public Func1(IOperator1<Double> aFunc, double aX0, double aDx, int aNx) {
        mX0 = aX0; mDx = aDx;
        mData = new double[aNx];
        for (int i = 0; i < aNx; ++i) mData[i] = aFunc.cal(mX0 + i*mDx);
    }
    
    public Func1 copy() {return new Func1(mX0, mDx, MathEX.Vec.copy(mData));}
    
    /** 获取结果 */
    public double subs(double aX) {
        int tI = (int)Math.ceil((aX-mX0)/mDx);
        int tImm = tI-1;
        
        double tX1 = mX0 + tImm*mDx;
        double tX2 = mX0 + tI*mDx;
        
        return MathEX.Func.interp1(tX1, tX2, get(tImm), get(tI), aX);
    }
    public double get_(int aI) {return mData[aI];}
    public void set_(int aI, double aV) {mData[aI] = aV;}
    public double get(int aI) {
        if (aI<0 || aI>=Nx()) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        return get_(aI);
    }
    public void set(int aI, double aV) {
        if (aI<0 || aI>=Nx()) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        set_(aI, aV);
    }
    
    /** 提供原生的 PBC 支持 */
    public double subsPBC(double aX) {
        int tI = (int)Math.ceil((aX-mX0)/mDx);
        int tImm = tI-1;
        
        double tX1 = mX0 + tImm*mDx;
        double tX2 = mX0 + tI*mDx;
        
        return MathEX.Func.interp1(tX1, tX2, getPBC(tImm), getPBC(tI), aX);
    }
    public double getPBC(int aI) {
        int tNx = Nx();
        if      (aI <  0  ) {while (aI <  0  ) aI += tNx;}
        else if (aI >= tNx) {while (aI >= tNx) aI -= tNx;}
        
        return get_(aI);
    }
    public void setPBC(int aI, double aV) {
        int tNx = Nx();
        if      (aI <  0  ) {while (aI <  0  ) aI += tNx;}
        else if (aI >= tNx) {while (aI >= tNx) aI -= tNx;}
        
        set_(aI, aV);
    }
    
    
    public double x0() {return mX0;}
    public double dx() {return mDx;}
    public int Nx() {return mData.length;}
    public double[] data() {return mData;}
}
