package jse.math.matrix;

import jep.NDArray;
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
 * 按照行排序的整数矩阵
 * @author liqa
 */
public class RowIntMatrix extends IntArrayMatrix {
    /** 提供默认的创建 */
    public static RowIntMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RowIntMatrix ones(int aNumRows, int aNumCols) {
        int[] tData = new int[aNumRows*aNumCols];
        Arrays.fill(tData, 1);
        return new RowIntMatrix(aNumRows, aNumCols, tData);
    }
    public static RowIntMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowIntMatrix zeros(int aNumRows, int aNumCols) {return new RowIntMatrix(aNumRows, aNumCols, new int[aNumRows*aNumCols]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aNumCols) {return new Builder(aNumCols);}
    public static Builder builder(int aNumCols, int aInitSize) {return new Builder(aNumCols, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private IntList mData;
        private final int mNumCols;
        private Builder(int aNumCols) {mNumCols = aNumCols; mData = new IntList(Math.max(aNumCols, DEFAULT_INIT_SIZE));}
        private Builder(int aNumCols, int aInitSize) {mNumCols = aNumCols; mData = new IntList(Math.max(aNumCols, aInitSize));}
        
        public void addRow(IIntVector aRow) {mData.addAll(aRow);}
        public void addRow(IIntVectorGetter aRowGetter) {mData.addAll(mNumCols, aRowGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public RowIntMatrix build() {
            IntList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new RowIntMatrix(tData.size()/mNumCols, mNumCols, tData.internalData());
        }
    }
    
    private final int mNumRows;
    private final int mNumCols;
    
    public RowIntMatrix(int aNumRows, int aNumCols, int[] aData) {
        super(aData);
        mNumRows = aNumRows;
        mNumCols = aNumCols;
    }
    public RowIntMatrix(int aNumCols, int[] aData) {this(aData.length/aNumCols, aNumCols, aData);}
    
    
    /** IComplexMatrix stuffs */
    @Override public final int get(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[aCol + aRow*mNumCols];
    }
    @Override public final void set(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[aCol + aRow*mNumCols] = aValue;
    }
    @Override public final int getAndSet(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols;
        int oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mNumRows;}
    @Override public final int ncols() {return mNumCols;}
    
    @Override protected RowIntMatrix newZeros_(int aNumRows, int aNumCols) {return RowIntMatrix.zeros(aNumRows, aNumCols);}
    @Override public RowIntMatrix copy() {return (RowIntMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mNumRows*mNumCols;}
    
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if (aObj instanceof RowIntMatrix && ((RowIntMatrix)aObj).mNumCols == mNumCols) return ((RowIntMatrix)aObj).mData;
        return null;
    }
    
    @Override public final NDArray<int[]> numpy() {return new NDArray<>(mData, mNumRows, mNumCols);}
    @Override public final RowIntMatrix toBufRow(boolean aAbort) {return this;}
    @Override public final void releaseBuf(@NotNull IIntMatrix aBuf, boolean aAbort) {if (aBuf != this) super.releaseBuf(aBuf, aAbort);}
    
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public IntVector asVecRow() {return new IntVector(mNumRows*mNumCols, mData);}
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public List<? extends IntVector> rows() {
        return new AbstractRandomAccessList<IntVector>() {
            @Override public int size() {return mNumRows;}
            @Override public IntVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public final IntVector row(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IntVector(mNumCols, aRow*mNumCols, mData);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnIntMatrix} */
    @Override public final IIntMatrixOperation operation() {
        return new IntArrayMatrixOperation_() {
            @Override public void fill(IIntMatrixGetter aRHS) {
                int idx = 0;
                for (int row = 0; row < mNumRows; ++row) for (int col = 0; col < mNumCols; ++col) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignRow(IntSupplier aSup) {
                int rEnd = mNumRows*mNumCols;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsInt();
            }
            @Override public void forEachRow(IntConsumer aCon) {
                int rEnd = mNumRows*mNumCols;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public ColumnIntMatrix refTranspose() {
                return new ColumnIntMatrix(mNumRows, mNumCols, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols;
        mData[tIdx] = aOpt.applyAsInt(mData[tIdx]);
    }
    @Override public final int getAndUpdate(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols;
        int tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IIntIterator iteratorCol() {
        return new IIntIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mCol = 0;
            private int mIdx = mCol;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mNumCols;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
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
    @Override public final IIntIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IIntIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mIdx = aCol;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mNumCols;
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
            private final int mEnd = (aRow+1)*mNumCols;
            private int mIdx = aRow*mNumCols;
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
    @Override public final IIntSetIterator setIteratorCol() {
        return new IIntSetIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mCol = 0;
            private int mIdx = mCol, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
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
    @Override public final IIntSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IIntSetIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mIdx = aCol, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
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
            private final int mEnd = (aRow+1)*mNumCols;
            private int mIdx = aRow*mNumCols, oIdx = -1;
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
}
