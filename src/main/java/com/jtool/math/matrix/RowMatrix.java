package com.jtool.math.matrix;

import com.jtool.code.ISetIterator;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.ShiftVector;
import com.jtool.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * @author liqa
 * <p> 按照行排序的矩阵，{@link ColumnMatrix} 的对称实现 </p>
 */
public final class RowMatrix extends DoubleArrayMatrix {
    /** 提供默认的创建 */
    public static RowMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RowMatrix ones(int aRowNum, int aColNum) {
        double[] tData = new double[aRowNum*aColNum];
        Arrays.fill(tData, 1.0);
        return new RowMatrix(aRowNum, aColNum, tData);
    }
    public static RowMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowMatrix zeros(int aRowNum, int aColNum) {return new RowMatrix(aRowNum, aColNum, new double[aRowNum*aColNum]);}
    
    
    private final int mRowNum;
    private final int mColNum;
    
    public RowMatrix(int aRowNum, int aColNum, double[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public RowMatrix(int aColNum, double[] aData) {this(aData.length/aColNum, aColNum, aData);}
    
    
    /** IMatrix stuffs */
    @Override public double get_(int aRow, int aCol) {return mData[aCol + aRow*mColNum];}
    @Override public void set_(int aRow, int aCol, double aValue) {mData[aCol + aRow*mColNum] = aValue;}
    @Override public double getAndSet_(int aRow, int aCol, double aValue) {
        int tIdx = aCol + aRow*mColNum;
        double oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mColNum;}
    
    @Override public RowMatrix newZeros(int aRowNum, int aColNum) {return RowMatrix.zeros(aRowNum, aColNum);}
    @Override public Vector newZerosVec(int aSize) {return Vector.zeros(aSize);}
    
    @Override public RowMatrix newShell() {return new RowMatrix(mRowNum, mColNum, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if (aObj instanceof RowMatrix && ((RowMatrix)aObj).mColNum == mColNum) return ((RowMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public IVector row(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new ShiftVector(mColNum, aRow*mColNum, mData);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnMatrix} */
    @Override public IMatrixOperation operation() {
        return new DoubleArrayMatrixOperation_() {
            @Override public ColumnMatrix refTranspose() {
                return new ColumnMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void increment_(int aRow, int aCol) {++mData[aCol + aRow*mColNum];}
    @Override public double getAndIncrement_(int aRow, int aCol) {return mData[aCol + aRow*mColNum]++;}
    @Override public double incrementAndGet_(int aRow, int aCol) {return ++mData[aCol + aRow*mColNum];}
    @Override public void decrement_(int aRow, int aCol) {--mData[aCol + aRow*mColNum];}
    @Override public double getAndDecrement_(int aRow, int aCol) {return mData[aCol + aRow*mColNum]--;}
    @Override public double decrementAndGet_(int aRow, int aCol) {return --mData[aCol + aRow*mColNum];}
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public Iterator<Double> colIterator(final int aCol) {
        return new Iterator<Double>() {
            private final int mSize = mRowNum * mColNum;
            private int mCol = aCol;
            private int mIdx = mCol;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public Double next() {
                if (hasNext()) {
                    Double tNext = mData[mIdx];
                    mIdx += mColNum;
                    if (mIdx >= mSize) {
                        ++mCol;
                        mIdx = mCol;
                    }
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
            private int mIdx = aRow*mColNum;
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
    @Override public ISetIterator<Double> colSetIterator(final int aCol) {
        return new ISetIterator<Double>() {
            private final int mSize = mRowNum * mColNum;
            private int mCol = aCol;
            private int mIdx = mCol, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(Double e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e;
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    mIdx += mColNum;
                    if (mIdx >= mSize) {
                        ++mCol;
                        mIdx = mCol;
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
                    mIdx += mColNum;
                    if (mIdx >= mSize) {
                        ++mCol;
                        mIdx = mCol;
                    }
                    mData[oIdx] = e;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    mIdx += mColNum;
                    if (mIdx >= mSize) {
                        ++mCol;
                        mIdx = mCol;
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
    @Override public ISetIterator<Double> rowSetIterator(final int aRow) {
        return new ISetIterator<Double>() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow*mColNum, oIdx = -1;
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
}
