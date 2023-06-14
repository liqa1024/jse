package com.jtool.math.matrix;

import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.ShiftVector;
import com.jtool.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;


/**
 * @author liqa
 * <p> 矩阵一般实现，按照列排序 </p>
 */
public final class ColumnMatrix extends DoubleArrayMatrix {
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
    
    @Override public ColumnMatrix newZeros(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    @Override public Vector newZerosVec(int aSize) {return Vector.zeros(aSize);}
    
    @Override public ColumnMatrix newShell() {return new ColumnMatrix(mRowNum, mColNum, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof ColumnMatrix && ((ColumnMatrix)aObj).mRowNum == mRowNum) return ((ColumnMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public IVector col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new ShiftVector(mRowNum, aCol*mRowNum, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public IVector asVecCol() {return new Vector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，引用转置直接返回 {@link RowMatrix} */
    @Override public IMatrixOperation operation() {
        return new DoubleArrayMatrixOperation_() {
            @Override public RowMatrix refTranspose() {
                return new RowMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void increment_(int aRow, int aCol) {++mData[aRow + aCol*mRowNum];}
    @Override public double getAndIncrement_(int aRow, int aCol) {return mData[aRow + aCol*mRowNum]++;}
    @Override public double incrementAndGet_(int aRow, int aCol) {return ++mData[aRow + aCol*mRowNum];}
    @Override public void decrement_(int aRow, int aCol) {--mData[aRow + aCol*mRowNum];}
    @Override public double getAndDecrement_(int aRow, int aCol) {return mData[aRow + aCol*mRowNum]--;}
    @Override public double decrementAndGet_(int aRow, int aCol) {return --mData[aRow + aCol*mRowNum];}
    
    @Override public void add_(int aRow, int aCol, double aDelta) {mData[aRow + aCol*mRowNum] += aDelta;}
    @Override public double getAndAdd_(int aRow, int aCol, double aDelta) {
        int tIdx = aRow + aCol*mRowNum;
        double tValue = mData[tIdx];
        mData[tIdx] += aDelta;
        return tValue;
    }
    @Override public double addAndGet_(int aRow, int aCol, double aDelta) {
        int tIdx = aRow + aCol*mRowNum;
        double tValue = mData[tIdx];
        tValue += aDelta;
        mData[tIdx] = tValue;
        return tValue;
    }
    @Override public void update_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        int tIdx = aRow + aCol*mRowNum;
        mData[tIdx] = aOpt.cal(mData[tIdx]);
    }
    @Override public double getAndUpdate_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        int tIdx = aRow + aCol*mRowNum;
        double tValue = mData[tIdx];
        mData[tIdx] = aOpt.cal(tValue);
        return tValue;
    }
    @Override public double updateAndGet_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        int tIdx = aRow + aCol*mRowNum;
        double tValue = mData[tIdx];
        tValue = aOpt.cal(tValue);
        mData[tIdx] = tValue;
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public IDoubleIterator colIterator() {
        return new IDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleIterator rowIterator() {
        return new IDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleIterator colIterator(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new IDoubleIterator() {
            private final int mEnd = (aCol+1)*mRowNum;
            private int mIdx = aCol*mRowNum;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleIterator rowIterator(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new IDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mRowNum;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator colSetIterator() {
        return new IDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    double oValue = mData[oIdx];
                    mData[oIdx] = aValue;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator rowSetIterator() {
        return new IDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    double oValue = mData[oIdx];
                    mData[oIdx] = aValue;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator colSetIterator(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new IDoubleSetIterator() {
            private final int mEnd = (aCol+1)*mColNum;
            private int mIdx = aCol*mRowNum, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    double oValue = mData[oIdx];
                    mData[oIdx] = aValue;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator rowSetIterator(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new IDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    double oValue = mData[oIdx];
                    mData[oIdx] = aValue;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
