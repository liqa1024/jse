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
    public static ColumnLogicalMatrix ones(int aRowNum, int aColNum) {
        boolean[] tData = new boolean[aRowNum*aColNum];
        Arrays.fill(tData, true);
        return new ColumnLogicalMatrix(aRowNum, aColNum, tData);
    }
    public static ColumnLogicalMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static ColumnLogicalMatrix zeros(int aRowNum, int aColNum) {return new ColumnLogicalMatrix(aRowNum, aColNum, new boolean[aRowNum*aColNum]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aRowNum) {return new Builder(aRowNum);}
    public static Builder builder(int aRowNum, int aInitSize) {return new Builder(aRowNum, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private BooleanList mData;
        private final int mRowNum;
        private Builder(int aRowNum) {mRowNum = aRowNum; mData = new BooleanList(Math.max(aRowNum, DEFAULT_INIT_SIZE));}
        private Builder(int aRowNum, int aInitSize) {mRowNum = aRowNum; mData = new BooleanList(Math.max(aRowNum, aInitSize));}
        
        public void addCol(ILogicalVector aCol) {mData.addAll(aCol);}
        public void addCol(ILogicalVectorGetter aColGetter) {mData.addAll(mRowNum, aColGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public ColumnLogicalMatrix build() {
            BooleanList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new ColumnLogicalMatrix(mRowNum, tData.size()/mRowNum, tData.internalData());
        }
    }
    
    private final int mRowNum;
    private final int mColNum;
    
    public ColumnLogicalMatrix(int aRowNum, int aColNum, boolean[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public ColumnLogicalMatrix(int aRowNum, boolean[] aData) {this(aRowNum, aData.length/aRowNum, aData);}
    
    
    /** IMatrix stuffs */
    @Override public final boolean get(int aRow, int aCol) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        return mData[aRow + aCol*mRowNum];
    }
    @Override public final void set(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        mData[aRow + aCol*mRowNum] = aValue;
    }
    @Override public final boolean getAndSet(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        boolean oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mRowNum;}
    @Override public final int ncols() {return mColNum;}
    
    @Override protected ColumnLogicalMatrix newZeros_(int aRowNum, int aColNum) {return ColumnLogicalMatrix.zeros(aRowNum, aColNum);}
    @Override public ColumnLogicalMatrix copy() {return (ColumnLogicalMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mRowNum*mColNum;}
    
    @Override public boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if ((aObj instanceof ColumnLogicalMatrix) && ((ColumnLogicalMatrix)aObj).mRowNum == mRowNum) return ((ColumnLogicalMatrix)aObj).mData;
        return null;
    }
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public List<? extends ShiftLogicalVector> cols() {
        return new AbstractRandomAccessList<ShiftLogicalVector>() {
            @Override public int size() {return mColNum;}
            @Override public ShiftLogicalVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public ShiftLogicalVector col(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new ShiftLogicalVector(mRowNum, aCol*mRowNum, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public LogicalVector asVecCol() {return new LogicalVector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，引用转置直接返回 {@link RowLogicalMatrix} */
    @Override public final ILogicalMatrixOperation operation() {
        return new BooleanArrayMatrixOperation_() {
            @Override public void fill(ILogicalMatrixGetter aRHS) {
                int idx = 0;
                for (int col = 0; col < mColNum; ++col) for (int row = 0; row < mRowNum; ++row) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignCol(BooleanSupplier aSup) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsBoolean();
            }
            @Override public void forEachCol(IBooleanConsumer aCon) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public RowLogicalMatrix refTranspose() {
                return new RowLogicalMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        mData[tIdx] = aOpt.applyAsBoolean(mData[tIdx]);
    }
    @Override public final boolean getAndUpdate(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        boolean tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IBooleanIterator iteratorCol() {
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
    @Override public final IBooleanIterator iteratorRow() {
        return new IBooleanIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
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
            private final int mEnd = (aCol+1)*mRowNum;
            private int mIdx = aCol*mRowNum;
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
        rangeCheckRow(aRow, mRowNum);
        return new IBooleanIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    mIdx += mRowNum;
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
    @Override public final IBooleanSetIterator setIteratorRow() {
        return new IBooleanSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
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
    @Override public final IBooleanSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IBooleanSetIterator() {
            private final int mEnd = (aCol+1)*mRowNum;
            private int mIdx = aCol*mRowNum, oIdx = -1;
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
        rangeCheckRow(aRow, mRowNum);
        return new IBooleanSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
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
