package jtool.math.matrix;

import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IDoubleSetIterator;
import jtool.math.vector.ShiftVector;
import jtool.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.*;

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
    @Override public double get(int aRow, int aCol) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        return mData[aCol + aRow*mColNum];
    }
    @Override public void set(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        mData[aCol + aRow*mColNum] = aValue;
    }
    @Override public double getAndSet(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        double oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mColNum;}
    
    @Override protected RowMatrix newZeros_(int aRowNum, int aColNum) {return RowMatrix.zeros(aRowNum, aColNum);}
    @Override public RowMatrix copy() {
        RowMatrix rMatrix = RowMatrix.zeros(mRowNum, mColNum);
        rMatrix.fill(this);
        return rMatrix;
    }
    
    @Override public RowMatrix newShell() {return new RowMatrix(mRowNum, mColNum, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if (aObj instanceof RowMatrix && ((RowMatrix)aObj).mColNum == mColNum) return ((RowMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public ShiftVector row(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new ShiftVector(mColNum, aRow*mColNum, mData);
    }
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public Vector asVecRow() {return new Vector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnMatrix} */
    @Override public IMatrixOperation operation() {
        return new DoubleArrayMatrixOperation_() {
            @Override public void fill(IMatrixGetter aRHS) {
                int idx = 0;
                for (int row = 0; row < mRowNum; ++row) for (int col = 0; col < mColNum; ++col) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignRow(DoubleSupplier aSup) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsDouble();
            }
            @Override public void forEachRow(DoubleConsumer aCon) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public ColumnMatrix refTranspose() {
                return new ColumnMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void update(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        mData[tIdx] = aOpt.applyAsDouble(mData[tIdx]);
    }
    @Override public double getAndUpdate(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        double tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public IDoubleIterator iteratorCol() {
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
    @Override public IDoubleIterator iteratorRow() {
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
    @Override public IDoubleIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
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
    @Override public IDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
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
    @Override public IDoubleSetIterator setIteratorCol() {
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
        };
    }
    @Override public IDoubleSetIterator setIteratorRow() {
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
        };
    }
    @Override public IDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
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
        };
    }
    @Override public IDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
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
        };
    }
}
