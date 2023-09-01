package com.jtool.math.function;


import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.math.IDataShell;
import com.jtool.math.MathEX;
import com.jtool.math.vector.RefVector;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vector;
import org.jetbrains.annotations.Nullable;


/**
 * @author liqa
 * <p> 内部存储 double[] 的数值函数，这里简单起见略去中间层，并且不考虑 shift 和 reverse 的情况，甚至不考虑 data 实际长度和使用长度不同的情况 </p>
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
        return new RefVector() {
            @Override public double get_(int aIdx) {return getX(aIdx);}
            @Override public int size() {return Nx();}
        };
    }
    @Override public final IVector f() {return new Vector(Nx(), mData);}
    
    
    /** 批量修改的接口 */
    @Override public final void fill(double[] aData) {System.arraycopy(aData, 0, mData, 0, Nx());}
    
    
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
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    @Override public final void update_(int aI, IDoubleOperator1 aOpt) {
        mData[aI] = aOpt.cal(mData[aI]);
    }
    @Override public final double getAndUpdate_(int aI, IDoubleOperator1 aOpt) {
        double tV = mData[aI];
        mData[aI] = aOpt.cal(tV);
        return tV;
    }
    @Override public final void update(int aI, IDoubleOperator1 aOpt) {
        if (aI<0 || aI>=Nx()) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        update_(aI, aOpt);
    }
    @Override public final double getAndUpdate(int aI, IDoubleOperator1 aOpt) {
        if (aI<0 || aI>=Nx()) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        return getAndUpdate_(aI, aOpt);
    }
    
    
    @Override public final DoubleArrayFunc1 copy() {
        double[] rData = new double[Nx()];
        System.arraycopy(mData, 0, rData, 0, rData.length);
        return newInstance_(mX0, mDx, rData);
    }
    
    
    /** 还提供一个给函数专用的运算 */
    protected class DoubleArrayFunc1Operation_ extends DoubleArrayFunc1Operation {
        @Override protected DoubleArrayFunc1 thisFunc1_() {return DoubleArrayFunc1.this;}
        /** 边界外的结果不保证正确性，这里简单起见统一都使用 ZeroBoundFunc1 来作为返回类型 */
        @Override protected DoubleArrayFunc1 newFunc1_(double aX0, double aDx, int aNx) {return ZeroBoundFunc1.zeros(aX0, aDx, aNx);}
        
        /** 暂时将这个遍历优化统一放在这里，因为略去了中间类（主要是略去了 reverse 的情况）*/
        @Override public void assign(IDoubleSupplier aSup) {
            final int tN = Nx();
            for (int i = 0; i < tN; ++i) mData[i] = aSup.get();
        }
        @Override public void forEach(IDoubleConsumer1 aCon) {
            final int tN = Nx();
            for (int i = 0; i < tN; ++i) aCon.run(mData[i]);
        }
    }
    
    @Override public IFunc1Operation operation() {return new DoubleArrayFunc1Operation_();}
    
    
    /** stuff to override，重写表明 x 超出了界限的情况下如何处理 */
    protected abstract double getOutL_(int aI);
    protected abstract double getOutR_(int aI);
    public abstract DoubleArrayFunc1 newShell();
    protected abstract DoubleArrayFunc1 newInstance_(double aX0, double aDx, double[] aData);
}
