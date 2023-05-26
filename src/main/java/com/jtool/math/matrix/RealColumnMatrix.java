package com.jtool.math.matrix;

import com.jtool.math.vector.AbstractVector;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGenerator;
import com.jtool.math.vector.RealVector;

import java.util.Arrays;


/**
 * @author liqa
 * <p> 矩阵一般实现，按照列排序 </p>
 */
public final class RealColumnMatrix extends DoubleArrayMatrix<RealColumnMatrix, RealVector> {
    /** 提供默认的创建 */
    public static RealColumnMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RealColumnMatrix ones(int aRowNum, int aColNum) {
        double[] tData = new double[aRowNum*aColNum];
        Arrays.fill(tData, 1.0);
        return new RealColumnMatrix(aRowNum, aColNum, tData);
    }
    public static RealColumnMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RealColumnMatrix zeros(int aRowNum, int aColNum) {return new RealColumnMatrix(aRowNum, aColNum, new double[aRowNum*aColNum]);}
    
    
    private final int mRowNum;
    private final int mColNum;
    
    public RealColumnMatrix(int aRowNum, int aColNum, double[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public RealColumnMatrix(int aRowNum, double[] aData) {this(aRowNum, aData.length/aRowNum, aData);}
    
    
    /** IMatrix stuffs */
    @Override public Double get_(int aRow, int aCol) {return mData[aRow + aCol*mRowNum];}
    @Override public void set_(int aRow, int aCol, Number aValue) {mData[aRow + aCol*mRowNum] = aValue.doubleValue();}
    @Override public Double getAndSet_(int aRow, int aCol, Number aValue) {
        int tIdx = aRow + aCol*mRowNum;
        Double oValue = mData[tIdx];
        mData[tIdx] = aValue.doubleValue();
        return oValue;
    }
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mColNum;}
    
    @Override protected RealColumnMatrix newZeros(int aRowNum, int aColNum) {return RealColumnMatrix.zeros(aRowNum, aColNum);}
    @Override protected RealVector newZeros(int aSize) {return RealVector.zeros(aSize);}
    
    @Override protected RealColumnMatrix this_() {return this;}
    @Override public RealColumnMatrix newShell() {return new RealColumnMatrix(mRowNum, mColNum, null);}
    @Override public double[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RealColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof RealColumnMatrix && ((RealColumnMatrix)aObj).mRowNum == mRowNum) return ((RealColumnMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public IVector<Double> col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new AbstractVector<Double>() {
            private final int mShift = aCol*mRowNum;
            @Override public Double get_(int aIdx) {return mData[aIdx + mShift];}
            @Override public void set_(int aIdx, Number aValue) {mData[aIdx + mShift] = aValue.doubleValue();}
            @Override public Double getAndSet_(int aIdx, Number aValue) {
                int tIdx = aIdx + mShift;
                Double oValue = mData[tIdx];
                mData[tIdx] = aValue.doubleValue();
                return oValue;
            }
            @Override public int size() {return mRowNum;}
        };
    }
    
    /** Optimize stuffs，重写 same 接口专门优化拷贝部分 */
    @Override public IVectorGenerator<RealVector> generatorVec() {
        return new VectorGenerator() {
                @Override public RealVector same() {
                RealVector rVector = zeros();
                System.arraycopy(mData, 0, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
                return rVector;
            }
        };
    }
    
    /** TODO Optimize stuffs，重写迭代器来提高遍历速度 */
}
