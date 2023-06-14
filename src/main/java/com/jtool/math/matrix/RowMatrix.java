package com.jtool.math.matrix;

import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.iterator.ISetIterator;
import com.jtool.code.operator.IDoubleOperator1;
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
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public IVector asVecRow() {return new Vector(mRowNum*mColNum, mData);}
    
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
    
    @Override public void add_(int aRow, int aCol, double aDelta) {mData[aCol + aRow*mColNum] += aDelta;}
    @Override public double getAndAdd_(int aRow, int aCol, double aDelta) {
        int tIdx = aCol + aRow*mColNum;
        double tValue = mData[tIdx];
        mData[tIdx] += aDelta;
        return tValue;
    }
    @Override public double addAndGet_(int aRow, int aCol, double aDelta) {
        int tIdx = aCol + aRow*mColNum;
        double tValue = mData[tIdx];
        tValue += aDelta;
        mData[tIdx] = tValue;
        return tValue;
    }
    @Override public void update_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        int tIdx = aCol + aRow*mColNum;
        mData[tIdx] = aOpt.cal(mData[tIdx]);
    }
    @Override public double getAndUpdate_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        int tIdx = aCol + aRow*mColNum;
        double tValue = mData[tIdx];
        mData[tIdx] = aOpt.cal(tValue);
        return tValue;
    }
    @Override public double updateAndGet_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        int tIdx = aCol + aRow*mColNum;
        double tValue = mData[tIdx];
        tValue = aOpt.cal(tValue);
        mData[tIdx] = tValue;
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public IDoubleIterator colIterator() {
        return new IDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mCol = 0;
            private int mIdx = mCol;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mColNum;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
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
    @Override public IDoubleIterator colIterator(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new IDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mColNum;
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
            private final int mEnd = (aRow+1)*mColNum;
            private int mIdx = aRow*mColNum;
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
    @Override public IDoubleSetIterator colSetIterator() {
        return new IDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mCol = 0;
            private int mIdx = mCol, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
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
    @Override public IDoubleSetIterator colSetIterator(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new IDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
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
            private final int mEnd = (aRow+1)*mColNum;
            private int mIdx = aRow*mColNum, oIdx = -1;
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
}
