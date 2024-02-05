package jse.math.function;

import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.IVectorGetter;
import jse.math.vector.RefVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

/**
 * 不等间距的 Func1 实例，这里只提供一个专门的实现，
 * 简单起见这里只提供零边界的实现，size 为 mF.length
 * @author liqa
 */
@ApiStatus.Experimental
public final class UnequalIntervalFunc1 extends AbstractFunc1 implements IZeroBoundFunc1 {
    /** 在这里提供一些常用的构造 */
    public static UnequalIntervalFunc1 zeros(int aNx, IVectorGetter aXGetter) {
        double[] rX = new double[aNx];
        for (int i = 0; i < aNx; ++i) {
            rX[i] = aXGetter.get(i);
            // 在这里检测输入一定要求增加
            if (i>0 && rX[i]<rX[i-1]) throw new IllegalArgumentException("Input X Must be increasing");
        }
        return new UnequalIntervalFunc1(rX, new double[aNx]);
    }
    
    private final double[] mF;
    private final double[] mX;
    public UnequalIntervalFunc1(double[] aX, double[] aF) {mX = aX; mF = aF;}
    
    /** 对等间距的输入也进行支持 */
    public UnequalIntervalFunc1(double aX0, double aDx, double[] aData) {
        this(new double[aData.length], aData);
        double tValue = aX0;
        for (int i = 0; i < mX.length; ++i){
            mX[i] = tValue;
            tValue += aDx;
        }
    }
    
    /** IFunc1 stuffs */
    @Override public IVector x() {
        // 这样防止外部修改
        return new RefVector() {
            @Override public double get(int aIdx) {return mX[aIdx];}
            @Override public int size() {return Nx();}
        };
    }
    @Override public IVector f() {return new Vector(Nx(), mF);}
    
    
    /** 批量修改的接口 */
    @Override public void fill(double[] aData) {System.arraycopy(aData, 0, mF, 0, Nx());}
    
    
    /** 获取结果，支持按照索引查找和按照 x 的值来查找 */
    @Override public double subs(double aX) {
        int tNx = Nx();
        // 由于 X 是递增的，使用二分法查找
        int tI = Arrays.binarySearch(mX, 0, tNx, aX);
        // java 二分法查找库的约定，没有找到时索引为负，恰好找到时为正
        if (tI >= 0) return mF[tI];
        tI = (-tI) - 1;
        int tImm = tI-1;
        
        // 超过边界直接为 0
        if (tImm < 0) return 0.0;
        if (tI >= tNx) return 0.0;
        
        return MathEX.Func.interp1(mX[tImm], mX[tI], mF[tImm], mF[tI], aX);
    }
    
    /** 不进行边界检测的版本，带入 x 的情况永远不会超过边界（周期边界或者固定值），因此只提供索引的情况 */
    @Override public double get(int aI) {rangeCheck(aI, Nx()); return mF[aI];}
    @Override public void set(int aI, double aV) {rangeCheck(aI, Nx()); mF[aI] = aV;}
    
    /** 索引和 x 相互转换的接口 */
    @Override public int Nx() {return mF.length;}
    @Override public double x0() {return mX[0];}
    @Override public double dx(int aI) {
        if (aI<0 || aI>=Nx()-1) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        return mX[aI+1] - mX[aI];
    }
    @Override public double getX(int aI) {
        if (aI<0 || aI>=Nx()) throw new IndexOutOfBoundsException(String.format("Index: %d", aI));
        return mX[aI];
    }
    @Override public void setX0(double aNewX0) {
        double tShift = aNewX0 - mX[0];
        int tNx = Nx();
        for (int i = 0; i < tNx; ++i) mX[i] += tShift;
    }
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    @Override public void update(int aI, DoubleUnaryOperator aOpt) {
        rangeCheck(aI, Nx());
        mF[aI] = aOpt.applyAsDouble(mF[aI]);
    }
    @Override public double getAndUpdate(int aI, DoubleUnaryOperator aOpt) {
        rangeCheck(aI, Nx());
        double tV = mF[aI];
        mF[aI] = aOpt.applyAsDouble(tV);
        return tV;
    }
    
    
    @Override public UnequalIntervalFunc1 copy() {
        int tNx = Nx();
        double[] rX = new double[tNx];
        double[] rF = new double[tNx];
        System.arraycopy(mX, 0, rX, 0, tNx);
        System.arraycopy(mF, 0, rF, 0, tNx);
        return new UnequalIntervalFunc1(rX, rF);
    }
    
    
    /** 还提供一个给函数专用的运算 */
    @Override public IFunc1Operation operation() {return new AbstractFunc1Operation() {
        @Override protected IFunc1 thisFunc1_() {return UnequalIntervalFunc1.this;}
        @Override protected IFunc1 newFunc1_() {return UnequalIntervalFunc1.zeros(Nx(), x());}
    };}
    
    
    /** 提供额外的接口用于检测两端 */
    @Override public double zeroBoundL() {return mX[0];}
    @Override public double zeroBoundR() {return mX[Nx()-1];}
}
