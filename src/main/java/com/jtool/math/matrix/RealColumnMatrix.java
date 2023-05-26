package com.jtool.math.matrix;

import com.jtool.code.ISetIterator;
import com.jtool.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


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
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RealColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof RealColumnMatrix && ((RealColumnMatrix)aObj).mRowNum == mRowNum) return ((RealColumnMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public IVector<Double> col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new ShiftRealVector(mRowNum, aCol*mRowNum, mData);
    }
    
    /** Optimize stuffs，重写 Vector 的 same 接口专门优化拷贝部分 */
    @Override public IVectorGenerator<RealVector> generatorVec() {
        return new VectorGenerator() {
                @Override public RealVector same() {
                RealVector rVector = zeros();
                System.arraycopy(mData, 0, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
                return rVector;
            }
        };
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public Iterator<Double> colIterator(final int aCol) {
        return new Iterator<Double>() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol*mRowNum;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public Double next() {
                if (hasNext()) {
                    Double tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public Iterator<Double> rowIterator(final int aRow) {
        return new Iterator<Double>() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = aRow;
            private int mIdx = mRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public Double next() {
                if (hasNext()) {
                    Double tNext = mData[mIdx];
                    mIdx += mRowNum;
                    if (mIdx >= mSize) {
                        ++mRow;
                        mIdx = mRow;
                    }
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public ISetIterator<Double, Number> colSetIterator(final int aCol) {
        return new ISetIterator<Double, Number>() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol*mRowNum, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(Number e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e.doubleValue();
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return mData[oIdx];
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public ISetIterator<Double, Number> rowSetIterator(final int aRow) {
        return new ISetIterator<Double, Number>() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = aRow;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(Number e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e.doubleValue();
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    mIdx += mRowNum;
                    if (mIdx >= mSize) {
                        ++mRow;
                        mIdx = mRow;
                    }
                    return mData[oIdx];
                }
                throw new NoSuchElementException();
            }
        };
    }
}
