package com.jtool.math.function;


import com.jtool.math.IDataShell;
import com.jtool.math.MathEX;
import com.jtool.math.vector.AbstractVector;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * @author liqa
 * <p> 内部存储 double[] 的数值函数，这里简单起见略去中间层，并且不考虑 shift 和 reverse 的情况 </p>
 * <p> 并且都要求 x 是等间距排列的，主要是为了加速查找过程 </p>
 * <p> 如果完全抽象的实现会非常复杂（包括新的专门用于函数的迭代器，相关运算之类，复杂度会超过矩阵向量库），这里暂不打算实现（至少等矩阵向量库完全成熟稳定） </p>
 */
public abstract class DoubleArrayFunc1 implements IFunc1, IDataShell<double[]> {
    protected double[] mData;
    protected double mX0;
    protected final double mDx;
    protected DoubleArrayFunc1(double aX0, double aDx, double[] aData) {mX0 = aX0; mDx = aDx; mData = aData;}
    
    /** 插值存储，因此不等间距排列也可以，精度会受到影响 */
    protected DoubleArrayFunc1(double[] aX, double[] aF) {
        this(aX[0], (aX[aX.length-1] - aX[0])/(aX.length-1), new double[aX.length]);
        
        mData[0] = aF[0];
        double tX = mX0;
        int tI = 1;
        for (int i = 1; i < aX.length; ++i) {
            tX += mDx;
            while (tI < aX.length-1 && tX >= aX[tI]) ++tI;
            mData[i] = MathEX.Func.interp1(aX[tI-1], aX[tI], aF[tI-1], aF[tI], tX);
        }
    }
    protected DoubleArrayFunc1(double aX0, double aDx, int aNx, IFunc1Subs aFunc) {
        this(aX0, aDx, new double[aNx]);
        
        for (int i = 0; i < aNx; ++i) mData[i] = aFunc.subs(mX0 + i*mDx);
    }
    
    
    /** DataShell stuffs */
    @Override public final void setData2this(double[] aData) {mData = aData;}
    @Override public final double[] getData() {return mData;}
    @Override public final int dataSize() {return Nx();}
    @Override public final int shiftSize() {return 0;}
    @Override public final double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 必须要求同样是 DoubleArrayFunc1 并且开始位置，间距以及长度都相同，也就是只考虑完全一致的情况（因为 Func 在区域外依旧可以取值）
        if (aObj instanceof DoubleArrayFunc1) {
            DoubleArrayFunc1 tFunc = (DoubleArrayFunc1)aObj;
            if (tFunc.Nx()==Nx() && MathEX.Code.numericEqual(tFunc.mX0, mX0) && MathEX.Code.numericEqual(tFunc.mDx, mDx)) return tFunc.mData;
            else return null;
        }
        return null;
    }
    
    
    /** IFunc1 stuffs */
    @Override public final IVector x() {
        return new AbstractVector() {
            @Override public double get_(int aIdx) {return getX(aIdx);}
            @Override public int size() {return Nx();}
        };
    }
    @Override public final IVector f() {return new Vector(mData);}
    
    
    /** 批量修改的接口 */
    @Override public final void fill(double aValue) {operation().mapFill2this(aValue);}
    @Override public final void fill(IFunc1Subs aFunc1Subs) {operation().ebeFill2this(aFunc1Subs);}
    @Override public final void fill(double[] aData) {System.arraycopy(aData, 0, mData, 0, Nx());}
    @Override public final void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        int tNx = Nx();
        for (int i = 0; i < tNx; ++i) mData[i] = it.next().doubleValue();
    }
    
    
    /** 获取结果，支持按照索引查找和按照 x 的值来查找 */
    @Override public final double subs(double aX) {
        int tI = (int)Math.ceil((aX-mX0)/mDx);
        int tImm = tI-1;
        
        double tX1 = getX(tImm);
        double tX2 = getX(tI);
        
        int tNx = Nx();
        if      (tI <= 0  ) return MathEX.Func.interp1(tX1, tX2, getOutL_(tImm), tI==0 ? mData[0] : getOutL_(tI), aX);
        else if (tI >= tNx) return MathEX.Func.interp1(tX1, tX2, tI==tNx ? mData[tNx-1] : getOutR_(tImm), getOutR_(tI), aX);
        else return MathEX.Func.interp1(tX1, tX2, mData[tImm], mData[tI], aX);
    }
    @Override public final double get(int aI) {
        if (aI<0 || aI>=Nx()) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        return get_(aI);
    }
    /** 设置结果，简单起见只允许按照索引来设置 */
    @Override public final void set(int aI, double aV) {
        if (aI<0 || aI>=Nx()) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        set_(aI, aV);
    }
    
    /** 不进行边界检测的版本，带入 x 的情况永远不会超过边界（周期边界或者固定值），因此只提供索引的情况 */
    @Override public final double get_(int aI) {return mData[aI];}
    @Override public final void set_(int aI, double aV) {mData[aI] = aV;}
    
    /** 索引和 x 相互转换的接口 */
    @Override public final int Nx() {return mData.length;}
    @Override public final double x0() {return mX0;}
    @Override public final double dx() {return mDx;}
    @Override public final double getX(int aI) {return mX0 + aI*mDx;}
    @Override public final void setX0(double aNewX0) {mX0 = aNewX0;}
    
    @Override public final DoubleArrayFunc1 copy() {
        double[] rData = new double[Nx()];
        System.arraycopy(mData, 0, rData, 0, rData.length);
        return newInstance_(mX0, mDx, rData);
    }
    
    
    
    
    /** 还提供一个给函数专用的运算 */
    protected class DoubleArrayFunc1Operation implements IFunc1Operation {
        @Override public void plus2this(IFunc1Subs aRHS) {
            final int tNx = Nx();
            final double[] tData = getIfHasSameOrderData(aRHS);
            if (tData != null) {
                // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
                final int tShift = IDataShell.shiftSize(aRHS);
                if (tShift == 0) for (int i = 0; i < tNx; ++i) mData[i] += tData[i];
                else for (int i = 0, j = tShift; i < tNx; ++i, ++j) mData[i] += tData[j];
            } else
            if (aRHS instanceof IZeroBoundFunc1) {
                // 对于零边界的特殊优化
                IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
                final int tStart = Math.max((int)Math.floor((tRHS.zeroBoundL() - mX0)/mDx), 0);
                final int tEnd = Math.min((int)Math.ceil((tRHS.zeroBoundR() - mX0)/mDx) + 1, tNx);
                for (int i = tStart; i < tEnd; ++i) mData[i] += aRHS.subs(getX(i));
            } else {
                for (int i = 0; i < tNx; ++i) mData[i] += aRHS.subs(getX(i));
            }
        }
        
        @Override public void mapFill2this(double aRHS) {
            final int tNx = Nx();
            for (int i = 0; i < tNx; ++i) mData[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
        }
        @Override public void ebeFill2this(IFunc1Subs aRHS) {
            final int tNx = Nx();
            final double[] tData = getIfHasSameOrderData(aRHS);
            if (tData != null) {
                // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
                System.arraycopy(tData, IDataShell.shiftSize(aRHS), mData, 0, tNx);
            } else {
                for (int i = 0; i < tNx; ++i) mData[i] = aRHS.subs(getX(i));
            }
        }
        
        
        /** 边界外的结果不保证正确性，这里简单起见统一都使用 ZeroBoundFunc1 来作为返回类型 */
        @Override public IFunc1 laplacian() {
            IFunc1 tOut = ZeroBoundFunc1.zeros(mX0, mDx, Nx());
            laplacian2Dest(tOut);
            return tOut;
        }
        public void laplacian2Dest(IFunc1 rDest) {
            int tNx = Nx();
            double tDx2 = mDx*mDx;
            for (int i = 0; i < tNx; ++i) {
                int imm = i-1;
                double tFmm = (imm < 0) ? getOutL_(imm) : mData[imm];
                int ipp = i+1;
                double tFpp = (ipp >= tNx) ? getOutR_(ipp) : mData[ipp];
                
                rDest.set_(i, (tFmm + tFpp - 2*mData[i]) / tDx2);
            }
        }
        
        /** 对于卷积以 refConvolve 为主 */
        @Override public IFunc1 convolve(IFunc2Subs aConv) {
            if (aConv instanceof IFunc2) {
                IFunc2 tConv = (IFunc2)aConv;
                return new ZeroBoundFunc1(tConv.y0(), tConv.dy(), tConv.Ny(), refConvolve(aConv));
            } else {
                return new ZeroBoundFunc1(mX0, mDx, Nx(), refConvolve(aConv));
            }
        }
        @Override public IFunc1Subs refConvolve(IFunc2Subs aConv) {
            return k -> {
                double pC = aConv.subs(mX0, k) * mData[0];
                double tResult = 0.0;
                double tDx2 = mDx/2.0;
                int tNx = Nx();
                for (int i = 1; i < tNx; ++i) {
                    double tC = aConv.subs(getX(i), k) * mData[i];
                    tResult += tDx2*(tC + pC);
                    pC = tC;
                }
                // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
                double tC = aConv.subs(getX(tNx), k) * getOutR_(tNx);
                tResult += tDx2*(tC + pC);
                
                return tResult;
            };
        }
        @Override public IFunc1 convolveFull(IFunc3Subs aConv) {
            if (aConv instanceof IFunc3) {
                IFunc3 tConv = (IFunc3)aConv;
                return new ZeroBoundFunc1(tConv.z0(), tConv.dz(), tConv.Nz(), refConvolveFull(aConv));
            } else {
                return new ZeroBoundFunc1(mX0, mDx, Nx(), refConvolveFull(aConv));
            }
        }
        @SuppressWarnings("SuspiciousNameCombination")
        @Override public IFunc1Subs refConvolveFull(IFunc3Subs aConv) {
            return k -> {
                double pC = aConv.subs(mData[0], mX0, k);
                double tResult = 0.0;
                double tDx2 = mDx/2.0;
                int tNx = Nx();
                for (int i = 1; i < tNx; ++i) {
                    double tC = aConv.subs(mData[i], getX(i), k);
                    tResult += tDx2*(tC + pC);
                    pC = tC;
                }
                // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
                double tC = aConv.subs(getOutR_(tNx), getX(tNx), k);
                tResult += tDx2*(tC + pC);
                
                return tResult;
            };
        }
        
        /** 由于是线性插值，因此最大的位置就是对应的 data 值 */
        @Override public double maxX() {
            int tMaxIdx = -1;
            double tMaxValue = Double.NEGATIVE_INFINITY;
            int tNx = Nx();
            for (int i = 0; i < tNx; ++i) {
                double tValue = mData[i];
                if (tValue > tMaxValue) {
                    tMaxValue = tValue;
                    tMaxIdx = i;
                }
            }
            return getX(tMaxIdx);
        }
        @Override public double minX() {
            int tMinIdx = -1;
            double tMinValue = Double.POSITIVE_INFINITY;
            int tNx = Nx();
            for (int i = 0; i < tNx; ++i) {
                double tValue = mData[i];
                if (tValue < tMinValue) {
                    tMinValue = tValue;
                    tMinIdx = i;
                }
            }
            return getX(tMinIdx);
        }
    }
    
    @Override public DoubleArrayFunc1Operation operation() {return new DoubleArrayFunc1Operation();}
    
    
    /** stuff to override，重写表明 x 超出了界限的情况下如何处理 */
    protected abstract double getOutL_(int aI);
    protected abstract double getOutR_(int aI);
    public abstract DoubleArrayFunc1 newShell();
    protected abstract DoubleArrayFunc1 newInstance_(double aX0, double aDx, double[] aData);
}
