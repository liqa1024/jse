package com.jtool.math.function;

import com.jtool.math.MathEX;
import com.jtool.math.operator.IOperator3;

/**
 * @author liqa
 * <p> 数值函数，三维输入（f(x,y,z)），内部使用向量存储，对于中间值使用线性插值给出结果 </p>
 * <p> 要求 x，y，z 是等间距排列的，主要是为了加速查找过程 </p>
 */
public class Func3 {
    private final double mX0, mY0, mZ0;
    private final double mDx, mDy, mDz;
    private final int mNx, mNy;
    private double[] mData;
    
    /** 提供更加灵活的使用方法，主要用来方便快速在 double[] 和 Func 之间进行转换并且不创建多余的对象 */
    public Func3 setData(double[] aData) {mData = aData; return this;}
    public Func3 shell() {return new Func3(mX0, mDx, mNx, mY0, mDy, mNy, mZ0, mDz, null);}
    
    public Func3(double aX0, double aDx, int aNx, double aY0, double aDy, int aNy, double aZ0, double aDz, double[] aF) {
        mX0 = aX0; mDx = aDx;
        mY0 = aY0; mDy = aDy;
        mZ0 = aZ0; mDz = aDz;
        mNx = aNx;
        mNy = aNy;
        mData = aF;
    }
    /** aF[index of x][index of y][index of z] */
    public Func3(double aX0, double aDx, double aY0, double aDy, double aZ0, double aDz, double[][][] aF) {
        mX0 = aX0; mDx = aDx;
        mY0 = aY0; mDy = aDy;
        mZ0 = aZ0; mDz = aDz;
        mNx = aF.length;
        mNy = aF[0].length;
        int tNz = aF[0][0].length;
        mData = new double[mNx*mNy*tNz];
        int tIdx = 0;
        for (int i = 0; i < mNx; ++i) {
            double[][] tFyz = aF[i];
            for (int j = 0; j < mNy; ++j) {
                double[] tFz = tFyz[j];
                for (int k = 0; k < mNy; ++k) {
                    mData[tIdx] = tFz[k];
                    ++tIdx;
                }
            }
        }
    }
    /** 插值存储，因此不等间距排列也可以，精度会受到影响 */
    public Func3(double[] aX, double[] aY, double[] aZ, double[][][] aF) {
        mX0 = aX[0];
        mDx = (aX[aX.length-1] - aX[0])/(aX.length-1);
        mNx = aX.length;
        mY0 = aY[0];
        mDy = (aY[aY.length-1] - aY[0])/(aY.length-1);
        mNy = aY.length;
        mZ0 = aZ[0];
        mDz = (aZ[aZ.length-1] - aZ[0])/(aZ.length-1);
        int tNz = aZ.length;
        
        mData = new double[mNx*mNy*tNz];
        for (int i = 0; i < mNx; ++i) for (int j = 0; j < mNy; ++j) mData[i + j*mNx] = aF[i][j][0];
        for (int i = 0; i < mNx; ++i) for (int k = 1; k < tNz; ++k) mData[i + k*mNx*mNy] = aF[i][0][k];
        for (int j = 1; j < mNy; ++j) for (int k = 1; k < tNz; ++k) mData[j*mNx + k*mNx*mNy] = aF[0][j][k];
        
        double tX = mX0;
        int tI = 1;
        for (int i = 1; i < mNx; ++i) {
            tX += mDx;
            while (tI < mNx-1 && tX >= aX[tI]) ++tI;
            
            double tY = mY0;
            int tJ = 1;
            for (int j = 1; j < mNy; ++j) {
                tY += mDy;
                while (tJ < mNy-1 && tY >= aY[tJ]) ++tJ;
                
                double tZ = mZ0;
                int tK = 1;
                for (int k = 1; k < tNz; ++k) {
                    tZ += mDz;
                    while (tK < tNz-1 && tK >= aZ[tK]) ++tK;
                    
                    double tF11 = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1][tJ-1][tK-1], aF[tI][tJ-1][tK-1], tX);
                    double tF21 = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1][tJ]  [tK-1], aF[tI][tJ]  [tK-1], tX);
                    double tF12 = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1][tJ-1][tK]  , aF[tI][tJ-1][tK]  , tX);
                    double tF22 = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1][tJ]  [tK]  , aF[tI][tJ]  [tK]  , tX);
                    double tF1 = MathEX.Func.interp1(aY[tJ-1], aY[tJ], tF11, tF21, tY);
                    double tF2 = MathEX.Func.interp1(aY[tJ-1], aY[tJ], tF12, tF22, tY);
                    mData[i + j*mNx + k*mNx*mNy] = MathEX.Func.interp1(aZ[tK-1], aZ[tK], tF1, tF2, tZ);
                }
            }
        }
    }
    public Func3(IOperator3<Double> aFunc, double aX0, double aDx, int aNx, double aY0, double aDy, int aNy, double aZ0, double aDz, int aNz) {
        mX0 = aX0; mDx = aDx;
        mY0 = aY0; mDy = aDy;
        mZ0 = aZ0; mDz = aDz;
        mNx = aNx;
        mNy = aNy;
        
        mData = new double[aNx*aNy*aNz];
        for (int k = 0; k < aNz; ++k) for (int j = 0; j < aNy; ++j) for (int i = 0; i < aNx; ++i) mData[i + j*mNx + k*mNx*mNy] = aFunc.cal(mX0 + i*mDx, mY0 + j*mDy, mZ0 + k*mDz);
    }
    
    public Func3 copy() {return new Func3(mX0, mDx, mNx, mY0, mDy, mNy, mZ0, mDz, MathEX.Vec.copy(mData));}
    
    /** 获取结果 */
    public double subs(double aX, double aY, double aZ) {
        int tI = (int)Math.ceil((aX-mX0)/mDx);
        int tJ = (int)Math.ceil((aY-mY0)/mDy);
        int tK = (int)Math.ceil((aZ-mZ0)/mDz);
        int tImm = tI-1, tJmm = tJ-1, tKmm = tK-1;
        
        double tX1 = mX0 + tImm*mDx;
        double tX2 = mX0 + tI*mDx;
        double tY1 = mY0 + tJmm*mDy;
        double tY2 = mY0 + tJ*mDy;
        double tZ1 = mZ0 + tKmm*mDz;
        double tZ2 = mZ0 + tK*mDz;
        
        double tF11 = MathEX.Func.interp1(tX1, tX2, get(tImm, tJmm, tKmm), get(tI, tJmm, tKmm), aX);
        double tF21 = MathEX.Func.interp1(tX1, tX2, get(tImm, tJ  , tKmm), get(tI, tJ  , tKmm), aX);
        double tF12 = MathEX.Func.interp1(tX1, tX2, get(tImm, tJmm, tK  ), get(tI, tJmm, tK  ), aX);
        double tF22 = MathEX.Func.interp1(tX1, tX2, get(tImm, tJ  , tK  ), get(tI, tJ  , tK  ), aX);
        double tF1 = MathEX.Func.interp1(tY1, tY2, tF11, tF21, aY);
        double tF2 = MathEX.Func.interp1(tY1, tY2, tF12, tF22, aY);
        
        return MathEX.Func.interp1(tZ1, tZ2, tF1, tF2, aZ);
    }
    public double get_(int aI, int aJ, int aK) {return mData[aI + aJ*mNx + aK*mNx*mNy];}
    public void set_(int aI, int aJ, int aK, double aV) {mData[aI + aJ*mNx + aK*mNx*mNy] = aV;}
    public double get(int aI, int aJ, int aK) {
        if (aI<0 || aI>=Nx() || aJ<0 || aJ>=Ny() || aK<0 || aK>=Nz()) throw new IndexOutOfBoundsException(String.format("Index: (%d, %d, %d)", aI, aJ, aK));
        return get_(aI, aJ, aK);
    }
    public void set(int aI, int aJ, int aK, double aV) {
        if (aI<0 || aI>=Nx() || aJ<0 || aJ>=Ny() || aK<0 || aK>=Nz()) throw new IndexOutOfBoundsException(String.format("Index: (%d, %d, %d)", aI, aJ, aK));
        set_(aI, aJ, aK, aV);
    }
    
    /** 提供原生的 PBC 支持 */
    public double subsPBC(double aX, double aY, double aZ) {
        int tI = (int)Math.ceil((aX-mX0)/mDx);
        int tJ = (int)Math.ceil((aY-mY0)/mDy);
        int tK = (int)Math.ceil((aZ-mZ0)/mDz);
        int tImm = tI-1, tJmm = tJ-1, tKmm = tK-1;
        
        double tX1 = mX0 + tImm*mDx;
        double tX2 = mX0 + tI*mDx;
        double tY1 = mY0 + tJmm*mDy;
        double tY2 = mY0 + tJ*mDy;
        double tZ1 = mZ0 + tKmm*mDz;
        double tZ2 = mZ0 + tK*mDz;
        
        double tF11 = MathEX.Func.interp1(tX1, tX2, getPBC(tImm, tJmm, tKmm), getPBC(tI, tJmm, tKmm), aX);
        double tF21 = MathEX.Func.interp1(tX1, tX2, getPBC(tImm, tJ  , tKmm), getPBC(tI, tJ  , tKmm), aX);
        double tF12 = MathEX.Func.interp1(tX1, tX2, getPBC(tImm, tJmm, tK  ), getPBC(tI, tJmm, tK  ), aX);
        double tF22 = MathEX.Func.interp1(tX1, tX2, getPBC(tImm, tJ  , tK  ), getPBC(tI, tJ  , tK  ), aX);
        double tF1 = MathEX.Func.interp1(tY1, tY2, tF11, tF21, aY);
        double tF2 = MathEX.Func.interp1(tY1, tY2, tF12, tF22, aY);
        
        return MathEX.Func.interp1(tZ1, tZ2, tF1, tF2, aZ);
    }
    public double getPBC(int aI, int aJ, int aK) {
        int tNx = Nx();
        if      (aI <  0  ) {while (aI <  0  ) aI += tNx;}
        else if (aI >= tNx) {while (aI >= tNx) aI -= tNx;}
        int tNy = Ny();
        if      (aJ <  0  ) {while (aJ <  0  ) aJ += tNy;}
        else if (aJ >= tNy) {while (aJ >= tNy) aJ -= tNy;}
        int tNz = Nz();
        if      (aK <  0  ) {while (aK <  0  ) aK += tNz;}
        else if (aK >= tNz) {while (aK >= tNz) aK -= tNz;}
        
        return get_(aI, aJ, aK);
    }
    public void setPBC(int aI, int aJ, int aK, double aV) {
        int tNx = Nx();
        if      (aI <  0  ) {while (aI <  0  ) aI += tNx;}
        else if (aI >= tNx) {while (aI >= tNx) aI -= tNx;}
        int tNy = Ny();
        if      (aJ <  0  ) {while (aJ <  0  ) aJ += tNy;}
        else if (aJ >= tNy) {while (aJ >= tNy) aJ -= tNy;}
        int tNz = Nz();
        if      (aK <  0  ) {while (aK <  0  ) aK += tNz;}
        else if (aK >= tNz) {while (aK >= tNz) aK -= tNz;}
        
        set_(aI, aJ, aK, aV);
    }
    
    public double x0() {return mX0;}
    public double dx() {return mDx;}
    public double y0() {return mY0;}
    public double dy() {return mDy;}
    public double z0() {return mZ0;}
    public double dz() {return mDz;}
    public int Nx() {return mNx;}
    public int Ny() {return mNy;}
    public int Nz() {return mData.length/(mNx*mNy);}
    public double[] data() {return mData;}
}
