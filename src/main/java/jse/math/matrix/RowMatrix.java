package jse.math.matrix;

import jep.NDArray;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.DoubleList;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.math.vector.IVectorGetter;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * 按照行排序的矩阵，{@link ColumnMatrix} 的对称实现
 * @author liqa
 */
public class RowMatrix extends DoubleArrayMatrix {
    /** 提供默认的创建 */
    public static RowMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RowMatrix ones(int aNumRows, int aNumCols) {
        double[] tData = new double[aNumRows*aNumCols];
        Arrays.fill(tData, 1.0);
        return new RowMatrix(aNumRows, aNumCols, tData);
    }
    public static RowMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowMatrix zeros(int aNumRows, int aNumCols) {return new RowMatrix(aNumRows, aNumCols, new double[aNumRows*aNumCols]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aNumCols) {return new Builder(aNumCols);}
    public static Builder builder(int aNumCols, int aInitSize) {return new Builder(aNumCols, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private DoubleList mData;
        private final int mNumCols;
        private Builder(int aNumCols) {mNumCols = aNumCols; mData = new DoubleList(Math.max(aNumCols, DEFAULT_INIT_SIZE));}
        private Builder(int aNumCols, int aInitSize) {mNumCols = aNumCols; mData = new DoubleList(Math.max(aNumCols, aInitSize));}
        
        public void addRow(IVectorGetter aRowGetter) {mData.addAll(mNumCols, aRowGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public RowMatrix build() {
            DoubleList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new RowMatrix(tData.size()/mNumCols, mNumCols, tData.internalData());
        }
    }
    
    private final int mNumRows;
    private final int mNumCols;
    
    public RowMatrix(int aNumRows, int aNumCols, double[] aData) {
        super(aData);
        mNumRows = aNumRows;
        mNumCols = aNumCols;
    }
    public RowMatrix(int aNumCols, double[] aData) {this(aData.length/aNumCols, aNumCols, aData);}
    
    
    /** IMatrix stuffs */
    @Override public final double get(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[aCol + aRow* mNumCols];
    }
    @Override public final void set(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[aCol + aRow* mNumCols] = aValue;
    }
    @Override public final double getAndSet(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow* mNumCols;
        double oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mNumRows;}
    @Override public final int ncols() {return mNumCols;}
    
    @Override protected RowMatrix newZeros_(int aNumRows, int aNumCols) {return RowMatrix.zeros(aNumRows, aNumCols);}
    @Override public RowMatrix copy() {return (RowMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mNumRows * mNumCols;}
    
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if (aObj instanceof RowMatrix && ((RowMatrix)aObj).mNumCols == mNumCols) return ((RowMatrix)aObj).mData;
        return null;
    }
    
    @Override public final NDArray<double[]> numpy() {return new NDArray<>(mData, mNumRows, mNumCols);}
    @Override public final RowMatrix toBufRow(boolean aAbort) {return this;}
    @Override public final void releaseBuf(@NotNull IMatrix aBuf, boolean aAbort) {if (aBuf != this) super.releaseBuf(aBuf, aAbort);}
    
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public List<? extends Vector> rows() {
        return new AbstractRandomAccessList<Vector>() {
            @Override public int size() {return mNumRows;}
            @Override public Vector get(int aRow) {return row(aRow);}
        };
    }
    @Override public Vector row(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new Vector(mNumCols, aRow* mNumCols, mData);
    }
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public Vector asVecRow() {return new Vector(mNumRows * mNumCols, mData);}
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnMatrix} */
    @Override public final IMatrixOperation operation() {
        return new DoubleArrayMatrixOperation_() {
            @Override public void fill(IMatrixGetter aRHS) {
                int idx = 0;
                for (int row = 0; row < mNumRows; ++row) for (int col = 0; col < mNumCols; ++col) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignRow(DoubleSupplier aSup) {
                int rEnd = mNumRows * mNumCols;
                for (int i = 0; i < rEnd; ++i) mData[i] = aSup.getAsDouble();
            }
            @Override public void forEachRow(DoubleConsumer aCon) {
                int rEnd = mNumRows * mNumCols;
                for (int i = 0; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public ColumnMatrix refTranspose() {
                return new ColumnMatrix(mNumCols, mNumRows, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow* mNumCols;
        mData[tIdx] = aOpt.applyAsDouble(mData[tIdx]);
    }
    @Override public final double getAndUpdate(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow* mNumCols;
        double tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IDoubleIterator iteratorCol() {
        return new IDoubleIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mCol = 0;
            private int mIdx = mCol;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mNumCols;
                    if (mIdx >= mSize) {++mCol; mIdx = mCol;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IDoubleIterator iteratorRow() {
        return new IDoubleIterator() {
            private final int mSize = mNumRows * mNumCols;
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
    @Override public final IDoubleIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IDoubleIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mIdx = aCol;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mNumCols;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IDoubleIterator() {
            private final int mEnd = (aRow+1)* mNumCols;
            private int mIdx = aRow* mNumCols;
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
    @Override public final IDoubleSetIterator setIteratorCol() {
        return new IDoubleSetIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mCol = 0;
            private int mIdx = mCol, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
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
            @Override public void nextAndSet(double aValue) {
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
    @Override public final IDoubleSetIterator setIteratorRow() {
        return new IDoubleSetIterator() {
            private final int mSize = mNumRows * mNumCols;
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
    @Override public final IDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IDoubleSetIterator() {
            private final int mSize = mNumRows * mNumCols;
            private int mIdx = aCol, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
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
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IDoubleSetIterator() {
            private final int mEnd = (aRow+1)* mNumCols;
            private int mIdx = aRow* mNumCols, oIdx = -1;
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
