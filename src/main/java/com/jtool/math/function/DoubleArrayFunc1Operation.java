package com.jtool.math.function;


import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.vector.IVectorSetter;

/**
 * 针对包含 double[] 的函数的运算，这里更进一步是针对 {@link DoubleArrayFunc1} 的运算。
 * 同样这里简单起见暂时略去中间层，并且不考虑 shift 和 reverse 的情况
 * @author liqa
 */
public abstract class DoubleArrayFunc1Operation implements IFunc1Operation {
    /** 通用的运算 */
    @Override public void mapPlus2this(double aRHS) {ARRAY.mapPlus2this_(thisFunc1_(), aRHS);}
    
    @Override public void ebePlus2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            final int tShift = IDataShell.shiftSize(aRHS);
            if (tShift == 0) for (int i = 0; i < tNx; ++i) rThis.mData[i] += tData[i];
            else for (int i = 0, j = tShift; i < tNx; ++i, ++j) rThis.mData[i] += tData[j];
        } else
        if (aRHS instanceof IZeroBoundFunc1) {
            // 对于零边界的特殊优化
            IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
            final int tStart = Math.max((int)Math.floor((tRHS.zeroBoundL() - rThis.mX0)/rThis.mDx), 0);
            final int tEnd = Math.min((int)Math.ceil((tRHS.zeroBoundR() - rThis.mX0)/rThis.mDx) + 1, tNx);
            for (int i = tStart; i < tEnd; ++i) rThis.mData[i] += aRHS.subs(rThis.getX(i));
        } else {
            for (int i = 0; i < tNx; ++i) rThis.mData[i] += aRHS.subs(rThis.getX(i));
        }
    }
    
    @Override public void mapFill2this(double aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) rThis.mData[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    @Override public void ebeFill2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            System.arraycopy(tData, IDataShell.shiftSize(aRHS), rThis.mData, 0, tNx);
        } else {
            for (int i = 0; i < tNx; ++i) rThis.mData[i] = aRHS.subs(rThis.getX(i));
        }
    }
    
    
    /** 边界外的结果不保证正确性 */
    @Override public IFunc1 laplacian() {
        DoubleArrayFunc1 tThis = thisFunc1_();
        IFunc1 tOut = newFunc1_(tThis.mX0, tThis.mDx, tThis.Nx());
        laplacian2Dest(tOut);
        return tOut;
    }
    @Override public void laplacian2Dest(IVectorSetter rDest) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        int tNx = rThis.Nx();
        double tDx2 = rThis.mDx*rThis.mDx;
        for (int i = 0; i < tNx; ++i) {
            int imm = i-1;
            double tFmm = (imm < 0) ? rThis.getOutL_(imm) : rThis.mData[imm];
            int ipp = i+1;
            double tFpp = (ipp >= tNx) ? rThis.getOutR_(ipp) : rThis.mData[ipp];
            
            rDest.set(i, (tFmm + tFpp - 2*rThis.mData[i]) / tDx2);
        }
    }
    
    /** 对于卷积以 refConvolve 为主 */
    @Override public IFunc1 convolve(IFunc2Subs aConv) {
        if (aConv instanceof IFunc2) {
            IFunc2 tConv = (IFunc2)aConv;
            return new ZeroBoundFunc1(tConv.y0(), tConv.dy(), tConv.Ny(), refConvolve(aConv));
        } else {
            DoubleArrayFunc1 tThis = thisFunc1_();
            return new ZeroBoundFunc1(tThis.mX0, tThis.mDx, tThis.Nx(), refConvolve(aConv));
        }
    }
    @Override public IFunc1Subs refConvolve(IFunc2Subs aConv) {
        final DoubleArrayFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.mX0, k) * tThis.mData[0];
            double tResult = 0.0;
            double tDx2 = tThis.mDx/2.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.getX(i), k) * tThis.mData[i];
                tResult += tDx2*(tC + pC);
                pC = tC;
            }
            // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
            double tC = aConv.subs(tThis.getX(tNx), k) * tThis.getOutR_(tNx);
            tResult += tDx2*(tC + pC);
            
            return tResult;
        };
    }
    @Override public IFunc1 convolveFull(IFunc3Subs aConv) {
        if (aConv instanceof IFunc3) {
            IFunc3 tConv = (IFunc3)aConv;
            return new ZeroBoundFunc1(tConv.z0(), tConv.dz(), tConv.Nz(), refConvolveFull(aConv));
        } else {
            DoubleArrayFunc1 tThis = thisFunc1_();
            return new ZeroBoundFunc1(tThis.mX0, tThis.mDx, tThis.Nx(), refConvolveFull(aConv));
        }
    }
    @SuppressWarnings("SuspiciousNameCombination")
    @Override public IFunc1Subs refConvolveFull(IFunc3Subs aConv) {
        final DoubleArrayFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.mData[0], tThis.mX0, k);
            double tResult = 0.0;
            double tDx2 = tThis.mDx/2.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.mData[i], tThis.getX(i), k);
                tResult += tDx2*(tC + pC);
                pC = tC;
            }
            // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
            double tC = aConv.subs(tThis.getOutR_(tNx), tThis.getX(tNx), k);
            tResult += tDx2*(tC + pC);
            
            return tResult;
        };
    }
    
    /** 由于是线性插值，因此最大的位置就是对应的 data 值 */
    @Override public double maxX() {
        final DoubleArrayFunc1 tThis = thisFunc1_();
        int tMaxIdx = -1;
        double tMaxValue = Double.NEGATIVE_INFINITY;
        int tNx = tThis.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tValue = tThis.mData[i];
            if (tValue > tMaxValue) {
                tMaxValue = tValue;
                tMaxIdx = i;
            }
        }
        return tThis.getX(tMaxIdx);
    }
    @Override public double minX() {
        final DoubleArrayFunc1 tThis = thisFunc1_();
        int tMinIdx = -1;
        double tMinValue = Double.POSITIVE_INFINITY;
        int tNx = tThis.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tValue = tThis.mData[i];
            if (tValue < tMinValue) {
                tMinValue = tValue;
                tMinIdx = i;
            }
        }
        return tThis.getX(tMinIdx);
    }
    
    
    /** stuff to override */
    protected abstract DoubleArrayFunc1 thisFunc1_();
    protected abstract DoubleArrayFunc1 newFunc1_(double aX0, double aDx, int aNx);
}
