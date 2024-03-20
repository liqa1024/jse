package jse.math.matrix;

import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.IntList;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.math.vector.IIntVector;
import jse.math.vector.IIntVectorGetter;
import jse.math.vector.IntVector;
import jse.math.vector.ShiftIntVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;


/**
 * @author liqa
 * <p> 按照行排序的整数矩阵 </p>
 */
public class RowIntMatrix extends IntArrayMatrix {
    /** 提供默认的创建 */
    public static RowIntMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RowIntMatrix ones(int aRowNum, int aColNum) {
        int[] tData = new int[aRowNum*aColNum];
        Arrays.fill(tData, 1);
        return new RowIntMatrix(aRowNum, aColNum, tData);
    }
    public static RowIntMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowIntMatrix zeros(int aRowNum, int aColNum) {return new RowIntMatrix(aRowNum, aColNum, new int[aRowNum*aColNum]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aColNum) {return new Builder(aColNum);}
    public static Builder builder(int aColNum, int aInitSize) {return new Builder(aColNum, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private IntList mData;
        private final int mColNum;
        private Builder(int aColNum) {mColNum = aColNum; mData = new IntList(Math.max(aColNum, DEFAULT_INIT_SIZE));}
        private Builder(int aColNum, int aInitSize) {mColNum = aColNum; mData = new IntList(Math.max(aColNum, aInitSize));}
        
        public void addRow(IIntVector aRow) {mData.addAll(aRow);}
        public void addRow(IIntVectorGetter aRowGetter) {mData.addAll(mColNum, aRowGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public RowIntMatrix build() {
            IntList tData = mData;
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new RowIntMatrix(tData.size()/mColNum, mColNum, tData.internalData());
        }
    }
    
    private final int mRowNum;
    private final int mColNum;
    
    public RowIntMatrix(int aRowNum, int aColNum, int[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public RowIntMatrix(int aColNum, int[] aData) {this(aData.length/aColNum, aColNum, aData);}
    
    
    /** IComplexMatrix stuffs */
    @Override public final int get(int aRow, int aCol) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        return mData[aCol + aRow*mColNum];
    }
    @Override public final void set(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        mData[aCol + aRow*mColNum] = aValue;
    }
    @Override public final int getAndSet(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        int oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int rowNumber() {return mRowNum;}
    @Override public final int columnNumber() {return mColNum;}
    
    @Override protected RowIntMatrix newZeros_(int aRowNum, int aColNum) {return RowIntMatrix.zeros(aRowNum, aColNum);}
    @Override public RowIntMatrix copy() {return (RowIntMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mRowNum*mColNum;}
    @Override public RowIntMatrix newShell() {return new RowIntMatrix(mRowNum, mColNum, null);}
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if (aObj instanceof RowIntMatrix && ((RowIntMatrix)aObj).mColNum == mColNum) return ((RowIntMatrix)aObj).mData;
        return null;
    }
    
    @Override public final RowIntMatrix toBufRow(boolean aAbort) {return this;}
    @Override public final void releaseBuf(@NotNull IIntMatrix aBuf, boolean aAbort) {if (aBuf != this) super.releaseBuf(aBuf, aAbort);}
    
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public IntVector asVecRow() {return new IntVector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public List<? extends ShiftIntVector> rows() {
        return new AbstractRandomAccessList<ShiftIntVector>() {
            @Override public int size() {return mRowNum;}
            @Override public ShiftIntVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public final ShiftIntVector row(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new ShiftIntVector(mColNum, aRow*mColNum, mData);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnIntMatrix} */
    @Override public final IIntMatrixOperation operation() {
        return new IntArrayMatrixOperation_() {
            @Override public void fill(IIntMatrixGetter aRHS) {
                int idx = 0;
                for (int row = 0; row < mRowNum; ++row) for (int col = 0; col < mColNum; ++col) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignRow(IntSupplier aSup) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsInt();
            }
            @Override public void forEachRow(IntConsumer aCon) {
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public ColumnIntMatrix refTranspose() {
                return new ColumnIntMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        mData[tIdx] = aOpt.applyAsInt(mData[tIdx]);
    }
    @Override public final int getAndUpdate(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        int tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IIntIterator iteratorCol() {
        return new IIntIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mCol = 0;
            private int mIdx = mCol;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mColNum;
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
    @Override public final IIntIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IIntIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    mIdx += mColNum;
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
            private final int mEnd = (aRow+1)*mColNum;
            private int mIdx = aRow*mColNum;
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
            private final int mSize = mRowNum * mColNum;
            private int mCol = 0;
            private int mIdx = mCol, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
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
    @Override public final IIntSetIterator setIteratorRow() {
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
    @Override public final IIntSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IIntSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aCol, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mColNum;
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
            private final int mEnd = (aRow+1)*mColNum;
            private int mIdx = aRow*mColNum, oIdx = -1;
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
