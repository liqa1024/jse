package jse.math.matrix;

import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.math.vector.IntVector;
import jse.math.vector.ShiftIntVector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;


/**
 * @author liqa
 * <p> 矩阵一般实现，按照列排序 </p>
 */
public class ColumnIntMatrix extends IntArrayMatrix {
    /** 提供默认的创建 */
    public static ColumnIntMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static ColumnIntMatrix ones(int aRowNum, int aColNum) {
        int[] tData = new int[aRowNum*aColNum];
        Arrays.fill(tData, 1);
        return new ColumnIntMatrix(aRowNum, aColNum, tData);
    }
    public static ColumnIntMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static ColumnIntMatrix zeros(int aRowNum, int aColNum) {return new ColumnIntMatrix(aRowNum, aColNum, new int[aRowNum*aColNum]);}
    
    
    private final int mRowNum;
    private final int mColNum;
    
    public ColumnIntMatrix(int aRowNum, int aColNum, int[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public ColumnIntMatrix(int aRowNum, int[] aData) {this(aRowNum, aData.length/aRowNum, aData);}
    
    
    /** IMatrix stuffs */
    @Override public final int get(int aRow, int aCol) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        return mData[aRow + aCol*mRowNum];
    }
    @Override public final void set(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        mData[aRow + aCol*mRowNum] = aValue;
    }
    @Override public final int getAndSet(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        int oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int rowNumber() {return mRowNum;}
    @Override public final int columnNumber() {return mColNum;}
    
    @Override protected ColumnIntMatrix newZeros_(int aRowNum, int aColNum) {return ColumnIntMatrix.zeros(aRowNum, aColNum);}
    @Override public ColumnIntMatrix copy() {return (ColumnIntMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mRowNum*mColNum;}
    @Override public ColumnIntMatrix newShell() {return new ColumnIntMatrix(mRowNum, mColNum, null);}
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof ColumnIntMatrix && ((ColumnIntMatrix)aObj).mRowNum == mRowNum) return ((ColumnIntMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public ShiftIntVector col(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new ShiftIntVector(mRowNum, aCol*mRowNum, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public IntVector asVecCol() {return new IntVector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，引用转置直接返回 {@link RowIntMatrix} */
    @Override public final IIntMatrixOperation operation() {
        return new IntArrayMatrixOperation_() {
            @Override public void fill(IIntMatrixGetter aRHS) {
                int idx = 0;
                for (int col = 0; col < mColNum; ++col) for (int row = 0; row < mRowNum; ++row) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignCol(IntSupplier aSup) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsInt();
            }
            @Override public void forEachCol(IntConsumer aCon) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public RowIntMatrix refTranspose() {
                return new RowIntMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        mData[tIdx] = aOpt.applyAsInt(mData[tIdx]);
    }
    @Override public final int getAndUpdate(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        int tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IIntIterator iteratorCol() {
        return new IIntIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntIterator iteratorRow() {
        return new IIntIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IIntIterator() {
            private final int mEnd = (aCol+1)*mRowNum;
            private int mIdx = aCol*mRowNum;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new IIntIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mRowNum;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntSetIterator setIteratorCol() {
        return new IIntSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntSetIterator setIteratorRow() {
        return new IIntSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
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
    @Override public final IIntSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IIntSetIterator() {
            private final int mEnd = (aCol+1)*mRowNum;
            private int mIdx = aCol*mRowNum, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new IIntSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
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
