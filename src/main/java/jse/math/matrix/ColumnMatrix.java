package jse.math.matrix;

import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.DoubleList;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.math.vector.IVectorGetter;
import jse.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;


/**
 * 矩阵一般实现，按照列排序
 * @author liqa
 */
public class ColumnMatrix extends DoubleArrayMatrix {
    /** 提供默认的创建 */
    public static ColumnMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static ColumnMatrix ones(int aNumRows, int aNumCols) {
        double[] tData = new double[aNumRows*aNumCols];
        Arrays.fill(tData, 1.0);
        return new ColumnMatrix(aNumRows, aNumCols, tData);
    }
    public static ColumnMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static ColumnMatrix zeros(int aNumRows, int aNumCols) {return new ColumnMatrix(aNumRows, aNumCols, new double[aNumRows*aNumCols]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aNumRows) {return new Builder(aNumRows);}
    public static Builder builder(int aNumRows, int aInitSize) {return new Builder(aNumRows, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private DoubleList mData;
        private final int mNumRows;
        private Builder(int aNumRows) {mNumRows = aNumRows; mData = new DoubleList(Math.max(aNumRows, DEFAULT_INIT_SIZE));}
        private Builder(int aNumRows, int aInitSize) {mNumRows = aNumRows; mData = new DoubleList(Math.max(aNumRows, aInitSize));}
        
        public void addCol(IVectorGetter aColGetter) {mData.addAll(mNumRows, aColGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public ColumnMatrix build() {
            DoubleList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new ColumnMatrix(mNumRows, tData.size()/mNumRows, tData.internalData());
        }
    }
    
    private final int mNumRows;
    private final int mNumCols;
    private final int mShift;
    
    public ColumnMatrix(int aNumRows, int aNumCols, int aShift, double[] aData) {
        super(aData);
        mNumRows = aNumRows;
        mNumCols = aNumCols;
        mShift = aShift;
    }
    public ColumnMatrix(int aNumRows, int aNumCols, double[] aData) {
        this(aNumRows, aNumCols, 0, aData);
    }
    public ColumnMatrix(int aNumRows, double[] aData) {
        this(aNumRows, aData.length/aNumRows, aData);
    }
    
    
    /** IMatrix stuffs */
    @Override public final double get(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[aRow + aCol*mNumRows + mShift];
    }
    @Override public final void set(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[aRow + aCol*mNumRows + mShift] = aValue;
    }
    @Override public final double getAndSet(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows + mShift;
        double oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public final int nrows() {return mNumRows;}
    @Override public final int ncols() {return mNumCols;}
    
    @Override protected ColumnMatrix newZeros_(int aNumRows, int aNumCols) {return ColumnMatrix.zeros(aNumRows, aNumCols);}
    @Override public ColumnMatrix copy() {return (ColumnMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mNumRows*mNumCols;}
    @Override public int internalDataShift() {return mShift;}
    
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof ColumnMatrix && ((ColumnMatrix)aObj).mNumRows==mNumRows) return ((ColumnMatrix)aObj).mData;
        return null;
    }
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public List<? extends Vector> cols() {
        return new AbstractRandomAccessList<Vector>() {
            @Override public int size() {return mNumCols;}
            @Override public Vector get(int aCol) {return col(aCol);}
        };
    }
    @Override public Vector col(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new Vector(mNumRows, aCol*mNumRows + mShift, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public Vector asVecCol() {
        return new Vector(mNumRows*mNumCols, mShift, mData);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link RowMatrix} */
    @Override public final IMatrixOperation operation() {
        return new DoubleArrayMatrixOperation_() {
            @Override public void fill(IMatrixGetter aRHS) {
                int idx = mShift;
                for (int col = 0; col < mNumCols; ++col) for (int row = 0; row < mNumRows; ++row) {
                    mData[idx] = aRHS.get(row, col);
                    ++idx;
                }
            }
            @Override public void assignCol(DoubleSupplier aSup) {
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) mData[i] = aSup.getAsDouble();
            }
            @Override public void forEachCol(DoubleConsumer aCon) {
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) aCon.accept(mData[i]);
            }
            @Override public RowMatrix refTranspose() {
                return new RowMatrix(mNumCols, mNumRows, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows + mShift;
        mData[tIdx] = aOpt.applyAsDouble(mData[tIdx]);
    }
    @Override public final double getAndUpdate(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aRow + aCol*mNumRows + mShift;
        double tValue = mData[tIdx];
        mData[tIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public final IDoubleIterator iteratorCol() {
        return new IDoubleIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = mShift;
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
    @Override public final IDoubleIterator iteratorRow() {
        return new IDoubleIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mRow = 0;
            private int mIdx = mRow+mShift;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mNumRows;
                    if (mIdx >= mEnd) {++mRow; mIdx = mRow+mShift;}
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
            private final int mEnd = (aCol+1)*mNumRows + mShift;
            private int mIdx = aCol*mNumRows + mShift;
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
    @Override public final IDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IDoubleIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aRow+mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    mIdx += mNumRows;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IDoubleSetIterator setIteratorCol() {
        return new IDoubleSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = mShift, oIdx = -1;
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
    @Override public final IDoubleSetIterator setIteratorRow() {
        return new IDoubleSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mRow = 0;
            private int mIdx = mRow+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
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
            @Override public void nextAndSet(double aValue) {
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
    @Override public final IDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IDoubleSetIterator() {
            private final int mEnd = (aCol+1)*mNumRows + mShift;
            private int mIdx = aCol*mNumRows + mShift, oIdx = -1;
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
    @Override public final IDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IDoubleSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aRow+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
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
            @Override public void nextAndSet(double aValue) {
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
