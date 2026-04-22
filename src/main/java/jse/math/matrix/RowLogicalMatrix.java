package jse.math.matrix;

import jep.NDArray;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.BooleanList;
import jse.code.functional.IBooleanConsumer;
import jse.code.functional.IBooleanUnaryOperator;
import jse.code.iterator.IBooleanIterator;
import jse.code.iterator.IBooleanSetIterator;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;


/**
 * 按照行排序的逻辑矩阵
 * @author liqa
 */
public class RowLogicalMatrix extends BooleanArrayMatrix {
    /** 提供默认的创建 */
    public static RowLogicalMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RowLogicalMatrix ones(int aNumRows, int aNumCols) {
        boolean[] tData = new boolean[aNumRows*aNumCols];
        Arrays.fill(tData, true);
        return new RowLogicalMatrix(aNumRows, aNumCols, tData);
    }
    public static RowLogicalMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowLogicalMatrix zeros(int aNumRows, int aNumCols) {return new RowLogicalMatrix(aNumRows, aNumCols, new boolean[aNumRows*aNumCols]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aNumCols) {return new Builder(aNumCols);}
    public static Builder builder(int aNumCols, int aInitSize) {return new Builder(aNumCols, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private BooleanList mData;
        private final int mNumCols;
        private Builder(int aNumCols) {mNumCols = aNumCols; mData = new BooleanList(Math.max(aNumCols, DEFAULT_INIT_SIZE));}
        private Builder(int aNumCols, int aInitSize) {mNumCols = aNumCols; mData = new BooleanList(Math.max(aNumCols, aInitSize));}
        
        public void addRow(ILogicalVector aRow) {mData.addAll(aRow);}
        public void addRow(ILogicalVectorGetter aRowGetter) {mData.addAll(mNumCols, aRowGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public RowLogicalMatrix build() {
            BooleanList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new RowLogicalMatrix(tData.size()/mNumCols, mNumCols, tData.internalData());
        }
    }
    
    private final int mNumRows;
    private final int mNumCols;
    private final int mShift;
    
    public RowLogicalMatrix(int aNumRows, int aNumCols, int aShift, boolean[] aData) {
        super(aData);
        mNumRows = aNumRows;
        mNumCols = aNumCols;
        mShift = aShift;
    }
    public RowLogicalMatrix(int aNumRows, int aNumCols, boolean[] aData) {
        this(aNumRows, aNumCols, 0, aData);
    }
    public RowLogicalMatrix(int aNumCols, boolean[] aData) {
        this(aData.length/aNumCols, aNumCols, aData);
    }
    
    
    /** IComplexMatrix stuffs */
    @Override public final boolean get(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[aCol + aRow*mNumCols + mShift];
    }
    @Override public final void set(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[aCol + aRow*mNumCols + mShift] = aValue;
    }
    @Override public final boolean getAndSet(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        boolean oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mNumRows;}
    @Override public final int ncols() {return mNumCols;}
    
    @Override protected RowLogicalMatrix newZeros_(int aNumRows, int aNumCols) {return RowLogicalMatrix.zeros(aNumRows, aNumCols);}
    @Override public RowLogicalMatrix copy() {return (RowLogicalMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mNumRows*mNumCols;}
    @Override public int internalDataShift() {return mShift;}
    
    @Override public boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if ((aObj instanceof RowLogicalMatrix) && ((RowLogicalMatrix)aObj).mNumCols==mNumCols) return ((RowLogicalMatrix)aObj).mData;
        return null;
    }
    @Override public final NDArray<boolean[]> numpy() {
        return mShift==0 ? new NDArray<>(mData, mNumRows, mNumCols) : super.numpy();
    }
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public List<? extends LogicalVector> rows() {
        return new AbstractRandomAccessList<LogicalVector>() {
            @Override public int size() {return mNumRows;}
            @Override public LogicalVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public final LogicalVector row(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new LogicalVector(mNumCols, aRow*mNumCols + mShift, mData);
    }
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public LogicalVector asVecRow() {
        return new LogicalVector(mNumRows*mNumCols, mShift, mData);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnLogicalMatrix} */
    @Override public final ILogicalMatrixOperation operation() {
        return new BooleanArrayMatrixOperation_() {
            @Override public void fill(ILogicalMatrixGetter aRHS) {
                int idx = mShift;
                for (int row = 0; row < mNumRows; ++row) for (int col = 0; col < mNumCols; ++col) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignRow(BooleanSupplier aSup) {
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) mData[i] = aSup.getAsBoolean();
            }
            @Override public void forEachRow(IBooleanConsumer aCon) {
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public ColumnLogicalMatrix refTranspose() {
                return new ColumnLogicalMatrix(mNumRows, mNumCols, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        mData[tIdx] = aOpt.applyAsBoolean(mData[tIdx]);
    }
    @Override public final boolean getAndUpdate(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        boolean tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IBooleanIterator iteratorCol() {
        return new IBooleanIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mCol = 0;
            private int mIdx = mCol+mShift;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanIterator iteratorRow() {
        return new IBooleanIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IBooleanIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aCol+mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mNumCols;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IBooleanIterator() {
            private final int mEnd = (aRow+1)*mNumCols + mShift;
            private int mIdx = aRow*mNumCols + mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanSetIterator setIteratorCol() {
        return new IBooleanSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mCol = 0;
            private int mIdx = mCol+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanSetIterator setIteratorRow() {
        return new IBooleanSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IBooleanSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aCol+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IBooleanSetIterator() {
            private final int mEnd = (aRow+1)*mNumCols + mShift;
            private int mIdx = aRow*mNumCols + mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
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
