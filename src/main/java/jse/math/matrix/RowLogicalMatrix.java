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
    public static RowLogicalMatrix ones(int aRowNum, int aColNum) {
        boolean[] tData = new boolean[aRowNum*aColNum];
        Arrays.fill(tData, true);
        return new RowLogicalMatrix(aRowNum, aColNum, tData);
    }
    public static RowLogicalMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowLogicalMatrix zeros(int aRowNum, int aColNum) {return new RowLogicalMatrix(aRowNum, aColNum, new boolean[aRowNum*aColNum]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aColNum) {return new Builder(aColNum);}
    public static Builder builder(int aColNum, int aInitSize) {return new Builder(aColNum, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private BooleanList mData;
        private final int mColNum;
        private Builder(int aColNum) {mColNum = aColNum; mData = new BooleanList(Math.max(aColNum, DEFAULT_INIT_SIZE));}
        private Builder(int aColNum, int aInitSize) {mColNum = aColNum; mData = new BooleanList(Math.max(aColNum, aInitSize));}
        
        public void addRow(ILogicalVector aRow) {mData.addAll(aRow);}
        public void addRow(ILogicalVectorGetter aRowGetter) {mData.addAll(mColNum, aRowGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public RowLogicalMatrix build() {
            BooleanList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new RowLogicalMatrix(tData.size()/mColNum, mColNum, tData.internalData());
        }
    }
    
    private final int mRowNum;
    private final int mColNum;
    
    public RowLogicalMatrix(int aRowNum, int aColNum, boolean[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public RowLogicalMatrix(int aColNum, boolean[] aData) {this(aData.length/aColNum, aColNum, aData);}
    
    
    /** IComplexMatrix stuffs */
    @Override public final boolean get(int aRow, int aCol) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        return mData[aCol + aRow*mColNum];
    }
    @Override public final void set(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        mData[aCol + aRow*mColNum] = aValue;
    }
    @Override public final boolean getAndSet(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        boolean oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mRowNum;}
    @Override public final int ncols() {return mColNum;}
    
    @Override protected RowLogicalMatrix newZeros_(int aRowNum, int aColNum) {return RowLogicalMatrix.zeros(aRowNum, aColNum);}
    @Override public RowLogicalMatrix copy() {return (RowLogicalMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mRowNum*mColNum;}
    
    @Override public boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if ((aObj instanceof RowLogicalMatrix) && ((RowLogicalMatrix)aObj).mColNum == mColNum) return ((RowLogicalMatrix)aObj).mData;
        return null;
    }
    
    @Override public final NDArray<boolean[]> numpy() {return new NDArray<>(mData, mRowNum, mColNum);}
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public LogicalVector asVecRow() {return new LogicalVector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public List<? extends LogicalVector> rows() {
        return new AbstractRandomAccessList<LogicalVector>() {
            @Override public int size() {return mRowNum;}
            @Override public LogicalVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public final LogicalVector row(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new LogicalVector(mColNum, aRow*mColNum, mData);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnLogicalMatrix} */
    @Override public final ILogicalMatrixOperation operation() {
        return new BooleanArrayMatrixOperation_() {
            @Override public void fill(ILogicalMatrixGetter aRHS) {
                int idx = 0;
                for (int row = 0; row < mRowNum; ++row) for (int col = 0; col < mColNum; ++col) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignRow(BooleanSupplier aSup) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsBoolean();
            }
            @Override public void forEachRow(IBooleanConsumer aCon) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public ColumnLogicalMatrix refTranspose() {
                return new ColumnLogicalMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        mData[tIdx] = aOpt.applyAsBoolean(mData[tIdx]);
    }
    @Override public final boolean getAndUpdate(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        boolean tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IBooleanIterator iteratorCol() {
        return new IBooleanIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mCol = 0;
            private int mIdx = mCol;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mColNum;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanIterator iteratorRow() {
        return new IBooleanIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
        rangeCheckCol(aCol, mColNum);
        return new IBooleanIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mColNum;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new IBooleanIterator() {
            private final int mEnd = (aRow+1)*mColNum;
            private int mIdx = aRow*mColNum;
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
            private final int mSize = mRowNum * mColNum;
            private int mCol = 0;
            private int mIdx = mCol, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
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
    @Override public final IBooleanSetIterator setIteratorRow() {
        return new IBooleanSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
        rangeCheckCol(aCol, mColNum);
        return new IBooleanSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new IBooleanSetIterator() {
            private final int mEnd = (aRow+1)*mColNum;
            private int mIdx = aRow*mColNum, oIdx = -1;
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
