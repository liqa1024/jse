package jse.math.matrix;

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
 * 逻辑矩阵一般实现，按照列排序
 * @author liqa
 */
public class ColumnLogicalMatrix extends BooleanArrayMatrix {
    /** 提供默认的创建 */
    public static ColumnLogicalMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static ColumnLogicalMatrix ones(int aNumRows, int aNumCols) {
        boolean[] tData = new boolean[aNumRows*aNumCols];
        Arrays.fill(tData, true);
        return new ColumnLogicalMatrix(aNumRows, aNumCols, tData);
    }
    public static ColumnLogicalMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static ColumnLogicalMatrix zeros(int aNumRows, int aNumCols) {return new ColumnLogicalMatrix(aNumRows, aNumCols, new boolean[aNumRows*aNumCols]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aNumRows) {return new Builder(aNumRows);}
    public static Builder builder(int aNumRows, int aInitSize) {return new Builder(aNumRows, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private BooleanList mData;
        private final int mNumRows;
        private Builder(int aNumRows) {mNumRows = aNumRows; mData = new BooleanList(Math.max(aNumRows, DEFAULT_INIT_SIZE));}
        private Builder(int aNumRows, int aInitSize) {mNumRows = aNumRows; mData = new BooleanList(Math.max(aNumRows, aInitSize));}
        
        public void addCol(ILogicalVector aCol) {mData.addAll(aCol);}
        public void addCol(ILogicalVectorGetter aColGetter) {mData.addAll(mNumRows, aColGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public ColumnLogicalMatrix build() {
            BooleanList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new ColumnLogicalMatrix(mNumRows, tData.size()/mNumRows, tData.internalData());
        }
    }
    
    private final int mNumRows;
    private final int mNumCols;
    private final int mShift;
    
    public ColumnLogicalMatrix(int aNumRows, int aNumCols, int aShift, boolean[] aData) {
        super(aData);
        mNumRows = aNumRows;
        mNumCols = aNumCols;
        mShift = aShift;
    }
    public ColumnLogicalMatrix(int aNumRows, int aNumCols, boolean[] aData) {
        this(aNumRows, aNumCols, 0, aData);
    }
    public ColumnLogicalMatrix(int aNumRows, boolean[] aData) {
        this(aNumRows, aData.length/aNumRows, aData);
    }
    
    
    /** IMatrix stuffs */
    @Override public final boolean get(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[aRow + aCol*mNumRows + mShift];
    }
    @Override public final void set(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[aRow + aCol*mNumRows + mShift] = aValue;
    }
    @Override public final boolean getAndSet(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows + mShift;
        boolean oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mNumRows;}
    @Override public final int ncols() {return mNumCols;}
    
    @Override protected ColumnLogicalMatrix newZeros_(int aNumRows, int aNumCols) {return ColumnLogicalMatrix.zeros(aNumRows, aNumCols);}
    @Override public ColumnLogicalMatrix copy() {return (ColumnLogicalMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mNumRows*mNumCols;}
    @Override public int internalDataShift() {return mShift;}
    
    @Override public boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if ((aObj instanceof ColumnLogicalMatrix) && ((ColumnLogicalMatrix)aObj).mNumRows==mNumRows) return ((ColumnLogicalMatrix)aObj).mData;
        return null;
    }
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public List<? extends LogicalVector> cols() {
        return new AbstractRandomAccessList<LogicalVector>() {
            @Override public int size() {return mNumCols;}
            @Override public LogicalVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public LogicalVector col(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new LogicalVector(mNumRows, aCol*mNumRows + mShift, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public LogicalVector asVecCol() {
        return new LogicalVector(mNumRows*mNumCols, mShift, mData);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link RowLogicalMatrix} */
    @Override public final ILogicalMatrixOperation operation() {
        return new BooleanArrayMatrixOperation_() {
            @Override public void fill(ILogicalMatrixGetter aRHS) {
                int idx = mShift;
                for (int col = 0; col < mNumCols; ++col) for (int row = 0; row < mNumRows; ++row) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignCol(BooleanSupplier aSup) {
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) mData[i] = aSup.getAsBoolean();
            }
            @Override public void forEachCol(IBooleanConsumer aCon) {
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public RowLogicalMatrix refTranspose() {
                return new RowLogicalMatrix(mNumRows, mNumCols, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows + mShift;
        mData[tIdx] = aOpt.applyAsBoolean(mData[tIdx]);
    }
    @Override public final boolean getAndUpdate(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows + mShift;
        boolean tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IBooleanIterator iteratorCol() {
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
    @Override public final IBooleanIterator iteratorRow() {
        return new IBooleanIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mRow = 0;
            private int mIdx = mRow+mShift;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mNumRows;
                    if (mIdx >= mEnd) {++mRow; mIdx = mRow+mShift;}
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
            private final int mEnd = (aCol+1)*mNumRows + mShift;
            private int mIdx = aCol*mNumRows + mShift;
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
    @Override public final IBooleanIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IBooleanIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aRow+mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mNumRows;
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
    @Override public final IBooleanSetIterator setIteratorRow() {
        return new IBooleanSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mRow = 0;
            private int mIdx = mRow+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    if (mIdx >= mEnd) {++mRow; mIdx = mRow+mShift;}
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    if (mIdx >= mEnd) {++mRow; mIdx = mRow+mShift;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumRows;
                    if (mIdx >= mEnd) {++mRow; mIdx = mRow+mShift;}
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
            private final int mEnd = (aCol+1)*mNumRows + mShift;
            private int mIdx = aCol*mNumRows + mShift, oIdx = -1;
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
    @Override public final IBooleanSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IBooleanSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aRow+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
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
