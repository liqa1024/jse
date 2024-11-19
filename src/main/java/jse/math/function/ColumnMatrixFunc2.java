package jse.math.function;

import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.ColumnMatrix;
import jse.math.vector.AbstractVector;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleUnaryOperator;


/**
 * @author liqa
 * <p> 现在改为内部存储 {@link ColumnMatrix}，这里简单起见略去中间层，甚至不考虑 data 实际长度和使用长度不同的情况 </p>
 * <p> 并且都要求 x, y 是等间距排列的，主要是为了加速查找过程 </p>
 */
@ApiStatus.Experimental
public abstract class ColumnMatrixFunc2 extends AbstractFunc2 implements IEqualIntervalFunc2, IDataShell<ColumnMatrix> {
    protected ColumnMatrix mData;
    protected double mX0, mY0;
    protected final double mDx, mDy;
    protected ColumnMatrixFunc2(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData) {mX0 = aX0; mY0 = aY0; mDx = aDx; mDy = aDy; mData = aData;}
    
    
    /** DataShell stuffs */
    @Override public final void setInternalData(ColumnMatrix aData) {mData = aData;}
    @Override public final ColumnMatrix internalData() {return mData;}
    @Override public final int internalDataSize() {return mData.rowNumber() * mData.columnNumber();}
    @Override public final @Nullable ColumnMatrix getIfHasSameOrderData(Object aObj) {
        // 必须要求同样是 ColumnMatrixFunc2 并且开始位置，间距以及长度都相同，也就是只考虑完全一致的情况（因为 Func 在区域外依旧可以取值）
        if (aObj instanceof ColumnMatrixFunc2) {
            ColumnMatrixFunc2 tFunc = (ColumnMatrixFunc2)aObj;
            if (tFunc.Nx()==Nx() && tFunc.Ny()==Ny()
             && MathEX.Code.numericEqual(tFunc.mX0, mX0) && MathEX.Code.numericEqual(tFunc.mY0, mY0)
             && MathEX.Code.numericEqual(tFunc.mDy, mDy) && MathEX.Code.numericEqual(tFunc.mDy, mDy)) {
                return tFunc.mData;
            }
            else return null;
        }
        return null;
    }
    
    
    /** IFunc2 stuffs */
    @Override public final IVector x() {
        return new RefVector() {
            @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return getX(aIdx);}
            @Override public int size() {return Nx();}
        };
    }
    @Override public final IVector y() {
        return new RefVector() {
            @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return getY(aIdx);}
            @Override public int size() {return Ny();}
        };
    }
    @Override public final ColumnMatrix f() {return mData;}
    
    /** 不进行边界检测的版本，带入 x 的情况永远不会超过边界（周期边界或者固定值），因此只提供索引的情况 */
    @Override public final double get(int aI, int aJ) {return mData.get(aI, aJ);}
    @Override public final void set(int aI, int aJ, double aV) {mData.set(aI, aJ, aV);}
    @Override public int getINear(double aX) {return MathEX.Code.round2int((aX-mX0)/mDx);}
    @Override public int getJNear(double aY) {return MathEX.Code.round2int((aY-mY0)/mDy);}
    
    /** 索引和 x 相互转换的接口 */
    @Override public final int Nx() {return mData.rowNumber();}
    @Override public final int Ny() {return mData.columnNumber();}
    @Override public final double x0() {return mX0;}
    @Override public final double dx() {return mDx;}
    @Override public final double y0() {return mY0;}
    @Override public final double dy() {return mDy;}
    @Override public final double getX(int aI) {return mX0 + aI*mDx;}
    @Override public final double getY(int aJ) {return mY0 + aJ*mDy;}
    @Override public final void setX0(double aNewX0) {mX0 = aNewX0;}
    @Override public final void setY0(double aNewY0) {mY0 = aNewY0;}
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    @Override public final void update(int aI, int aJ, DoubleUnaryOperator aOpt) {
        mData.update(aI, aJ, aOpt);
    }
    @Override public final double getAndUpdate(int aI, int aJ, DoubleUnaryOperator aOpt) {
        return mData.getAndUpdate(aI, aJ, aOpt);
    }
    
    @Override public final ColumnMatrixFunc2 copy() {
        return newInstance_(mX0, mY0, mDx, mDy, mData.copy());
    }
    
    /** 还提供一个给函数专用的运算 */
    protected class ColumnMatrixFunc2Operation_ extends ColumnMatrixFunc2Operation {
        @Override protected ColumnMatrixFunc2 thisFunc2_() {return ColumnMatrixFunc2.this;}
        /** 边界外的结果不保证正确性，这里简单起见统一都使用 ConstBoundFunc2 来作为返回类型 */
        @Override protected ColumnMatrixFunc2 newFunc2_() {return ConstBoundFunc2.zeros(x0(), y0(), dx(), dy(), Nx(), Ny());}
    }
    @Override public IFunc2Operation operation() {return new ColumnMatrixFunc2Operation_();}
    
    /** stuff to override，重写表明 x, y 超出了界限的情况下如何处理 */
    public abstract double subs(double aX, double aY);
    public abstract ColumnMatrixFunc2 newShell();
    protected abstract ColumnMatrixFunc2 newInstance_(double aX0, double aY0, double aDx, double aDy, ColumnMatrix aData);
}
