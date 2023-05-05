package com.guan.math.numerical;

import com.guan.math.MathEX;
import com.guan.math.functional.IOperator2;

/**
 * @author liqa
 * <p> 数值函数，二维输入（f(x,y)），内部使用向量存储，对于中间值使用线性插值给出结果 </p>
 * <p> 要求 x，y 是等间距排列的，主要是为了加速查找过程 </p>
 */
public class Func2 {
    private final double mX0, mY0;
    private final double mDx, mDy;
    private final int mNx;
    private double[] mData;
    
    /** 提供更加灵活的使用方法，主要用来方便快速在 double[] 和 Func 之间进行转换并且不创建多余的对象 */
    public Func2 setData(double[] aData) {mData = aData; return this;}
    public Func2 shell() {return new Func2(mX0, mDx, mNx, mY0, mDy, null);}
    
    
    public Func2(double aX0, double aDx, int aNx, double aY0, double aDy, double[] aF) {
        mX0 = aX0; mDx = aDx;
        mY0 = aY0; mDy = aDy;
        mNx = aNx;
        mData = aF;
    }
    /** aF[index of x][index of y] */
    public Func2(double aX0, double aDx, double aY0, double aDy, double[][] aF) {
        mX0 = aX0; mDx = aDx;
        mY0 = aY0; mDy = aDy;
        mNx = aF.length;
        int tNy = aF[0].length;
        mData = new double[mNx*tNy];
        int tIdx = 0;
        for (int i = 0; i < mNx; ++i) {
            double[] tRows = aF[i];
            for (int j = 0; j < tNy; ++j) {
                mData[tIdx] = tRows[j];
                ++tIdx;
            }
        }
    }
    /** 插值存储，因此不等间距排列也可以，精度会受到影响 */
    public Func2(double[] aX, double[] aY, double[][] aF) {
        mX0 = aX[0];
        mDx = (aX[aX.length-1] - aX[0])/(aX.length-1);
        mNx = aX.length;
        mY0 = aY[0];
        mDy = (aY[aY.length-1] - aY[0])/(aY.length-1);
        int tNy = aY.length;
        
        mData = new double[mNx*tNy];
        for (int i = 0; i < mNx; ++i) mData[i] = aF[i][0];
        for (int j = 1; j < tNy; ++j) mData[j*mNx] = aF[0][j];
        
        double tX = mX0;
        int tI = 1;
        for (int i = 1; i < mNx; ++i) {
            tX += mDx;
            while (tI < mNx-1 && tX >= aX[tI]) ++tI;
            
            double tY = mY0;
            int tJ = 1;
            for (int j = 1; j < tNy; ++j) {
                tY += mDy;
                while (tJ < tNy-1 && tY >= aY[tJ]) ++tJ;
                
                double tF1 = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1][tJ-1], aF[tI][tJ-1], tX);
                double tF2 = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1][tJ]  , aF[tI][tJ]  , tX);
                mData[i + j*mNx] = MathEX.Func.interp1(aY[tJ-1], aY[tJ], tF1, tF2, tY);
            }
        }
    }
    public Func2(IOperator2<Double> aFunc, double aX0, double aDx, int aNx, double aY0, double aDy, int aNy) {
        mX0 = aX0; mDx = aDx;
        mY0 = aY0; mDy = aDy;
        mNx = aNx;
        
        mData = new double[aNx*aNy];
        for (int j = 0; j < aNy; ++j) for (int i = 0; i < aNx; ++i) mData[i + j*mNx] = aFunc.cal(mX0 + i*mDx, mY0 + j*mDy);
    }
    
    public Func2 copy() {return new Func2(mX0, mDx, mNx, mY0, mDy, MathEX.Vec.copy(mData));}
    
    /** 获取结果 */
    public double subs(double aX, double aY) {
        int tI = (int)Math.ceil((aX-mX0)/mDx);
        int tJ = (int)Math.ceil((aY-mY0)/mDy);
        int tImm = tI-1, tJmm = tJ-1;
        
        double tX1 = mX0 + tImm*mDx;
        double tX2 = mX0 + tI*mDx;
        double tY1 = mY0 + tJmm*mDy;
        double tY2 = mY0 + tJ*mDy;
        
        double tF1 = MathEX.Func.interp1(tX1, tX2, get(tImm, tJmm), get(tI, tJmm), aX);
        double tF2 = MathEX.Func.interp1(tX1, tX2, get(tImm, tJ  ), get(tI, tJ  ), aX);
        return MathEX.Func.interp1(tY1, tY2, tF1, tF2, aY);
    }
    public double get_(int aI, int aJ) {return mData[aI + aJ*mNx];}
    public void set_(int aI, int aJ, double aV) {mData[aI + aJ*mNx] = aV;}
    public double get(int aI, int aJ) {
        if (aI<0 || aI>=Nx() || aJ<0 || aJ>=Ny()) throw new IndexOutOfBoundsException(String.format("Index: (%d, %d)", aI, aJ));
        return get_(aI, aJ);
    }
    public void set(int aI, int aJ, double aV) {
        if (aI<0 || aI>=Nx() || aJ<0 || aJ>=Ny()) throw new IndexOutOfBoundsException(String.format("Index: (%d, %d)", aI, aJ));
        set_(aI, aJ, aV);
    }
    
    /** 提供原生的 PBC 支持 */
    public double subsPBC(double aX, double aY) {
        int tI = (int)Math.ceil((aX-mX0)/mDx);
        int tJ = (int)Math.ceil((aY-mY0)/mDy);
        int tImm = tI-1, tJmm = tJ-1;
        
        double tX1 = mX0 + tImm*mDx;
        double tX2 = mX0 + tI*mDx;
        double tY1 = mY0 + tJmm*mDy;
        double tY2 = mY0 + tJ*mDy;
        
        double tF1 = MathEX.Func.interp1(tX1, tX2, getPBC(tImm, tJmm), getPBC(tI, tJmm), aX);
        double tF2 = MathEX.Func.interp1(tX1, tX2, getPBC(tImm, tJ  ), getPBC(tI, tJ  ), aX);
        return MathEX.Func.interp1(tY1, tY2, tF1, tF2, aY);
    }
    public double getPBC(int aI, int aJ) {
        int tNx = Nx();
        if      (aI <  0  ) {while (aI <  0  ) aI += tNx;}
        else if (aI >= tNx) {while (aI >= tNx) aI -= tNx;}
        int tNy = Ny();
        if      (aJ <  0  ) {while (aJ <  0  ) aJ += tNy;}
        else if (aJ >= tNy) {while (aJ >= tNy) aJ -= tNy;}
        
        return get_(aI, aJ);
    }
    public void setPBC(int aI, int aJ, double aV) {
        int tNx = Nx();
        if      (aI <  0  ) {while (aI <  0  ) aI += tNx;}
        else if (aI >= tNx) {while (aI >= tNx) aI -= tNx;}
        int tNy = Ny();
        if      (aJ <  0  ) {while (aJ <  0  ) aJ += tNy;}
        else if (aJ >= tNy) {while (aJ >= tNy) aJ -= tNy;}
        
        set_(aI, aJ, aV);
    }
    
    public double x0() {return mX0;}
    public double dx() {return mDx;}
    public double y0() {return mY0;}
    public double dy() {return mDy;}
    public int Nx() {return mNx;}
    public int Ny() {return mData.length/mNx;}
    public double[] data() {return mData;}
}
