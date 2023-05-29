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
public final class ColumnMatrix extends DoubleArrayMatrix<ColumnMatrix, Vector> implements IMatrix {
    /** 提供默认的创建 */
    public static ColumnMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static ColumnMatrix ones(int aRowNum, int aColNum) {
        double[] tData = new double[aRowNum*aColNum];
        Arrays.fill(tData, 1.0);
        return new ColumnMatrix(aRowNum, aColNum, tData);
    }
    public static ColumnMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static ColumnMatrix zeros(int aRowNum, int aColNum) {return new ColumnMatrix(aRowNum, aColNum, new double[aRowNum*aColNum]);}
    
    
    private final int mRowNum;
    private final int mColNum;
    
    public ColumnMatrix(int aRowNum, int aColNum, double[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public ColumnMatrix(int aRowNum, double[] aData) {this(aRowNum, aData.length/aRowNum, aData);}
    
    
    /** IMatrix stuffs */
    @Override public double get_(int aRow, int aCol) {return mData[aRow + aCol*mRowNum];}
    @Override public void set_(int aRow, int aCol, double aValue) {mData[aRow + aCol*mRowNum] = aValue;}
    @Override public double getAndSet_(int aRow, int aCol, double aValue) {
        int tIdx = aRow + aCol*mRowNum;
        double oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mColNum;}
    
    @Override protected ColumnMatrix newZeros_(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    @Override protected Vector newZeros_(int aSize) {return Vector.zeros(aSize);}
    
    @Override public ColumnMatrix newShell() {return new ColumnMatrix(mRowNum, mColNum, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RealColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof ColumnMatrix && ((ColumnMatrix)aObj).mRowNum == mRowNum) return ((ColumnMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public IVector col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new ShiftVector(mRowNum, aCol*mRowNum, mData);
    }
    
    /** Optimize stuffs，重写 Vector 的 same 接口专门优化拷贝部分 */
    @Override public IVectorGenerator<Vector> generatorVec() {
        return new VectorGenerator() {
                @Override public Vector same() {
                Vector rVector = zeros();
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
                } else {
                    throw new NoSuchElementException();
                }
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
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> colSetIterator(final int aCol) {
        return new ISetIterator<Double>() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol*mRowNum, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(Double e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e;
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[oIdx] = e;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    double oValue = mData[oIdx];
                    mData[oIdx] = e;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> rowSetIterator(final int aRow) {
        return new ISetIterator<Double>() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = aRow;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(Double e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e;
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
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    mIdx += mRowNum;
                    if (mIdx >= mSize) {
                        ++mRow;
                        mIdx = mRow;
                    }
                    mData[oIdx] = e;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    mIdx += mRowNum;
                    if (mIdx >= mSize) {
                        ++mRow;
                        mIdx = mRow;
                    }
                    double oValue = mData[oIdx];
                    mData[oIdx] = e;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
