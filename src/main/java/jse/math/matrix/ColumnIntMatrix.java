package jse.math.matrix;

import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.IntList;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.math.vector.IIntVector;
import jse.math.vector.IIntVectorGetter;
import jse.math.vector.IntVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;


/**
 * 整数矩阵一般实现，按照列排序
 * @author liqa
 */
public class ColumnIntMatrix extends IntArrayMatrix {
    /** 提供默认的创建 */
    public static ColumnIntMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static ColumnIntMatrix ones(int aNumRows, int aNumCols) {
        int[] tData = new int[aNumRows*aNumCols];
        Arrays.fill(tData, 1);
        return new ColumnIntMatrix(aNumRows, aNumCols, tData);
    }
    public static ColumnIntMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static ColumnIntMatrix zeros(int aNumRows, int aNumCols) {return new ColumnIntMatrix(aNumRows, aNumCols, new int[aNumRows*aNumCols]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aNumRows) {return new Builder(aNumRows);}
    public static Builder builder(int aNumRows, int aInitSize) {return new Builder(aNumRows, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private IntList mData;
        private final int mNumRows;
        private Builder(int aNumRows) {mNumRows = aNumRows; mData = new IntList(Math.max(aNumRows, DEFAULT_INIT_SIZE));}
        private Builder(int aNumRows, int aInitSize) {mNumRows = aNumRows; mData = new IntList(Math.max(aNumRows, aInitSize));}
        
        public void addCol(IIntVector aCol) {mData.addAll(aCol);}
        public void addCol(IIntVectorGetter aColGetter) {mData.addAll(mNumRows, aColGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public ColumnIntMatrix build() {
            IntList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new ColumnIntMatrix(mNumRows, tData.size()/mNumRows, tData.internalData());
        }
    }
    
    private final int mNumRows;
    private final int mNumCols;
    
    public ColumnIntMatrix(int aNumRows, int aNumCols, int[] aData) {
        super(aData);
        mNumRows = aNumRows;
        mNumCols = aNumCols;
    }
    public ColumnIntMatrix(int aNumRows, int[] aData) {this(aNumRows, aData.length/aNumRows, aData);}
    
    
    /** IMatrix stuffs */
    @Override public final int get(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[aRow + aCol*mNumRows];
    }
    @Override public final void set(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[aRow + aCol*mNumRows] = aValue;
    }
    @Override public final int getAndSet(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows;
        int oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mNumRows;}
    @Override public final int ncols() {return mNumCols;}
    
    @Override protected ColumnIntMatrix newZeros_(int aNumRows, int aNumCols) {return ColumnIntMatrix.zeros(aNumRows, aNumCols);}
    @Override public ColumnIntMatrix copy() {return (ColumnIntMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mNumRows*mNumCols;}
    
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof ColumnIntMatrix && ((ColumnIntMatrix)aObj).mNumRows == mNumRows) return ((ColumnIntMatrix)aObj).mData;
        return null;
    }
    
    @Override public final ColumnIntMatrix toBufCol(boolean aAbort) {return this;}
    @Override public final void releaseBuf(@NotNull IIntMatrix aBuf, boolean aAbort) {if (aBuf != this) super.releaseBuf(aBuf, aAbort);}
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public List<? extends IntVector> cols() {
        return new AbstractRandomAccessList<IntVector>() {
            @Override public int size() {return mNumCols;}
            @Override public IntVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IntVector col(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IntVector(mNumRows, aCol*mNumRows, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public IntVector asVecCol() {return new IntVector(mNumRows*mNumCols, mData);}
    
    /** Optimize stuffs，引用转置直接返回 {@link RowIntMatrix} */
    @Override public final IIntMatrixOperation operation() {
        return new IntArrayMatrixOperation_() {
            @Override public void fill(IIntMatrixGetter aRHS) {
                int idx = 0;
                for (int col = 0; col < mNumCols; ++col) for (int row = 0; row < mNumRows; ++row) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignCol(IntSupplier aSup) {
                int rEnd = mNumRows*mNumCols;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsInt();
            }
            @Override public void forEachCol(IntConsumer aCon) {
                int rEnd = mNumRows*mNumCols;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public RowIntMatrix refTranspose() {
                return new RowIntMatrix(mNumRows, mNumCols, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows;
        mData[tIdx] = aOpt.applyAsInt(mData[tIdx]);
    }
    @Override public final int getAndUpdate(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows;
        int tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IIntIterator iteratorCol() {
        return new IIntIterator() {
            private final int mSize = mNumRows * mNumCols;
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
            private final int mSize = mNumRows * mNumCols;
            private int mRow = 0;
            private int mIdx = mRow;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mNumRows;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IIntIterator() {
            private final int mEnd = (aCol+1)*mNumRows;
            private int mIdx = aCol*mNumRows;
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
        rangeCheckRow(aRow, mNumRows);
        return new IIntIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mIdx = aRow;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mNumRows;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntSetIterator setIteratorCol() {
        return new IIntSetIterator() {
            private final int mSize = mNumRows * mNumCols;
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
            private final int mSize = mNumRows * mNumCols;
            private int mRow = 0;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IIntSetIterator() {
            private final int mEnd = (aCol+1)*mNumRows;
            private int mIdx = aCol*mNumRows, oIdx = -1;
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
        rangeCheckRow(aRow, mNumRows);
        return new IIntSetIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mIdx = aRow, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
