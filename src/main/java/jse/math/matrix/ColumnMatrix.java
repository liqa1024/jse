package jse.math.matrix;

import jse.code.collection.AbstractRandomAccessList;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.math.vector.ShiftVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;


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
    @Override public double get(int aRow, int aCol) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        return mData[aRow + aCol*mRowNum];
    }
    @Override public void set(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        mData[aRow + aCol*mRowNum] = aValue;
    }
    @Override public double getAndSet(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        double oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mColNum;}
    
    @Override protected ColumnMatrix newZeros_(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    @Override public ColumnMatrix copy() {return (ColumnMatrix)super.copy();}
    
    @Override public ColumnMatrix newShell() {return new ColumnMatrix(mRowNum, mColNum, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof ColumnMatrix && ((ColumnMatrix)aObj).mRowNum == mRowNum) return ((ColumnMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public List<ShiftVector> cols() {
        return new AbstractRandomAccessList<ShiftVector>() {
            @Override public int size() {return mColNum;}
            @Override public ShiftVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public ShiftVector col(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new ShiftVector(mRowNum, aCol*mRowNum, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public Vector asVecCol() {return new Vector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，引用转置直接返回 {@link RowMatrix} */
    @Override public IMatrixOperation operation() {
        return new DoubleArrayMatrixOperation_() {
            @Override public void fill(IMatrixGetter aRHS) {
                int idx = 0;
                for (int col = 0; col < mColNum; ++col) for (int row = 0; row < mRowNum; ++row) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignCol(DoubleSupplier aSup) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsDouble();
            }
            @Override public void forEachCol(DoubleConsumer aCon) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public RowMatrix refTranspose() {
                return new RowMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void update(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        mData[tIdx] = aOpt.applyAsDouble(mData[tIdx]);
    }
    @Override public double getAndUpdate(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        double tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public IDoubleIterator iteratorCol() {
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
    @Override public IDoubleIterator iteratorRow() {
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
    @Override public IDoubleIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
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
    @Override public IDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
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
    @Override public IDoubleSetIterator setIteratorCol() {
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
    @Override public IDoubleSetIterator setIteratorRow() {
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
        };
    }
    @Override public IDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IDoubleSetIterator() {
            private final int mEnd = (aCol+1)*mRowNum;
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
        };
    }
    @Override public IDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
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
        };
    }
}
