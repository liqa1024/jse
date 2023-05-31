package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.ISetIterator;
import com.jtool.code.UT;
import com.jtool.math.vector.*;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * @author liqa
 * <p> 通用的矩阵类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractMatrixAny<M extends IMatrixAny<?, ?>, V extends IVectorAny<?>> implements IMatrixAny<M, V> {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        List<IVector> tRows = rows();
        boolean tFirst = true;
        for (IVector tRow : tRows) {
            if (!tFirst) rStr.append("\n");
            for (double tValue : tRow.iterable()) rStr.append(toString_(tValue));
            tFirst = false;
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public Iterator<Double> colIterator(final int aCol) {
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = get_(mRow, mCol);
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public Iterator<Double> rowIterator(final int aRow) {
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = get_(mRow, mCol);
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> colSetIterator(final int aCol) {
        return new ISetIterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol, oCol = aCol;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(Double e) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oRow, oCol, e);
            }
            @Override public Double next() {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return get_(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    set_(oRow, oCol, e);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    double oValue = get_(oRow, oCol);
                    set_(oRow, oCol, e);
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> rowSetIterator(final int aRow) {
        return new ISetIterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = aRow, oRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(Double e) {
                if (oCol < 0) throw new IllegalStateException();
                set_(oRow, oCol, e);
            }
            @Override public Double next() {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return get_(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    set_(oRow, oCol, e);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    double oValue = get_(oRow, oCol);
                    set_(oRow, oCol, e);
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public Iterator<Double> colIteratorOf(final int aCol, final IMatrixGetter aContainer) {
        if (aContainer instanceof IMatrixAny) return ((IMatrixAny<?, ?>)aContainer).colIterator(aCol);
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = aContainer.get(mRow, mCol);
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public Iterator<Double> rowIteratorOf(final int aRow, final IMatrixGetter aContainer) {
        if (aContainer instanceof IMatrixAny) return ((IMatrixAny<?, ?>)aContainer).rowIterator(aRow);
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = aContainer.get(mRow, mCol);
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    
    /** 转为兼容性更好的 double[][]，默认直接使用 asList 转为 double[][] */
    @Override public double[][] data() {return UT.Code.toMat(asList());}
    
    /** 批量修改的接口，现在统一使用迭代器来填充 */
    @Override public final void fill(double aValue) {operation().mapFill2this(aValue);}
    @Override public final void fill(IMatrixGetter aMatrixGetter) {operation().ebeFill2this(aMatrixGetter);}
    
    /** 同样这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public void fill(final double[][] aData) {
        final ISetIterator<Double> si = rowSetIterator();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final double[] tRow = aData[row];
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRow[col]);
        }
    }
    @Override public void fillWithRows(Iterable<? extends Iterable<? extends Number>> aRows) {
        final Iterator<? extends Iterable<? extends Number>> tRowsIt = aRows.iterator();
        final ISetIterator<Double> si = rowSetIterator();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final Iterator<? extends Number> tRowIt = tRowsIt.next().iterator();
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRowIt.next().doubleValue());
        }
    }
    @Override public void fillWithCols(Iterable<? extends Iterable<? extends Number>> aCols) {
        final Iterator<? extends Iterable<? extends Number>> tColsIt = aCols.iterator();
        final ISetIterator<Double> si = colSetIterator();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int col = 0; col < tColNum; ++col) {
            final Iterator<? extends Number> tColIt = tColsIt.next().iterator();
            for (int row = 0; row < tRowNum; ++row) si.nextAndSet(tColIt.next().doubleValue());
        }
    }
    
    
    @Override public double get(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return get_(aRow, aCol);
    }
    @Override public double getAndSet(int aRow, int aCol, double aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSet_(aRow, aCol, aValue);
    }
    @Override public void set(int aRow, int aCol, double aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        set_(aRow, aCol, aValue);
    }
    @Override public ISize size() {
        return new ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    
    
    @Override public List<IVector> rows() {
        return new AbstractList<IVector>() {
            @Override public int size() {return rowNumber();}
            @Override public IVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IVector row(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new AbstractVector() {
            @Override public double get_(int aIdx) {return AbstractMatrixAny.this.get_(aRow, aIdx);}
            @Override public void set_(int aIdx, double aValue) {AbstractMatrixAny.this.set_(aRow, aIdx, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrixAny.this.getAndSet_(aRow, aIdx, aValue);}
            @Override public int size() {return columnNumber();}
        };
    }
    @Override public List<IVector> cols() {
        return new AbstractList<IVector>() {
            @Override public int size() {return columnNumber();}
            @Override public IVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IVector col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new AbstractVector() {
            @Override public double get_(int aIdx) {return AbstractMatrixAny.this.get_(aIdx, aCol);}
            @Override public void set_(int aIdx, double aValue) {AbstractMatrixAny.this.set_(aIdx, aCol, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrixAny.this.getAndSet_(aIdx, aCol, aValue);}
            @Override public int size() {return rowNumber();}
        };
    }
    
    
    
    /** 矩阵生成器的一般实现，主要实现一些重复的接口 */
    protected class MatrixGenerator extends AbstractMatrixGenerator<M> {
        @Override protected Iterator<Double> thisIterator_() {return iterator();}
        @Override protected int thisRowNumber_() {return rowNumber();}
        @Override protected int thisColumnNumber_() {return columnNumber();}
        @Override public M zeros(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
    }
    protected class VectorGenerator extends AbstractVectorGenerator<V> {
        @Override protected Iterator<Double> thisIterator_() {return iterator();}
        @Override protected int thisSize_() {return rowNumber()*columnNumber();}
        @Override public V zeros(int aSize) {return newZeros_(aSize);}
    }
    
    @Override public IVectorGenerator<V> generatorVec() {return new VectorGenerator();}
    @Override public IMatrixGenerator<M> generator() {return new MatrixGenerator();}
    
    @Override public final M copy() {return generator().same();}
    
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    @Override public IMatrixSlicer<M, V> slicer() {
        return new AbstractMatrixSlicer<M, V>() {
            @Override protected V getIL(final int aSelectedRow, final List<Integer> aSelectedCols) {return generatorVec().from(aSelectedCols.size(), i -> AbstractMatrixAny.this.get(aSelectedRow, aSelectedCols.get(i)));}
            @Override protected V getLI(final List<Integer> aSelectedRows, final int aSelectedCol) {return generatorVec().from(aSelectedRows.size(), i -> AbstractMatrixAny.this.get(aSelectedRows.get(i), aSelectedCol));}
            @Override protected V getIA(final int aSelectedRow) {return generatorVec().from(columnNumber(), col -> AbstractMatrixAny.this.get(aSelectedRow, col));}
            @Override protected V getAI(final int aSelectedCol) {return generatorVec().from(rowNumber()   , row -> AbstractMatrixAny.this.get(row, aSelectedCol));}
            
            @Override protected M getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {return generator().from(aSelectedRows.size(), aSelectedCols.size(), (row, col) -> AbstractMatrixAny.this.get(aSelectedRows.get(row), aSelectedCols.get(col)));}
            @Override protected M getLA(final List<Integer> aSelectedRows) {return generator().from(aSelectedRows.size(), columnNumber()      , (row, col) -> AbstractMatrixAny.this.get(aSelectedRows.get(row), col));}
            @Override protected M getAL(final List<Integer> aSelectedCols) {return generator().from(rowNumber()         , aSelectedCols.size(), (row, col) -> AbstractMatrixAny.this.get(row, aSelectedCols.get(col)));}
            @Override protected M getAA() {return generator().same();}
            
            @Override protected List<IVector> thisRows_() {return rows();}
            @Override protected List<IVector> thisCols_() {return cols();}
        };
    }
    @Override public IMatrixSlicer<IMatrix, IVector> refSlicer() {
        return new AbstractMatrixSlicer<IMatrix, IVector>() {
            @Override protected IVector getIL(final int aSelectedRow, final List<Integer> aSelectedCols) {
                if (aSelectedRow<0 || aSelectedRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aSelectedRow);
                return new AbstractVector() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aIdx) {return AbstractMatrixAny.this.get(aSelectedRow, aSelectedCols.get(aIdx));}
                    @Override public void set_(int aIdx, double aValue) {AbstractMatrixAny.this.set(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrixAny.this.getAndSet(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public int size() {return aSelectedCols.size();}
                };
            }
            @Override protected IVector getLI(final List<Integer> aSelectedRows, final int aSelectedCol) {
                if (aSelectedCol<0 || aSelectedCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aSelectedCol);
                return new AbstractVector() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aIdx) {return AbstractMatrixAny.this.get(aSelectedRows.get(aIdx), aSelectedCol);}
                    @Override public void set_(int aIdx, double aValue) {AbstractMatrixAny.this.set(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrixAny.this.getAndSet(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public int size() {return aSelectedRows.size();}
                };
            }
            @Override protected IVector getIA(int aSelectedRow) {return row(aSelectedRow);}
            @Override protected IVector getAI(int aSelectedCol) {return col(aSelectedCol);}
            
            @Override protected IMatrix getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {
                return new AbstractMatrix() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrixAny.this.get(aSelectedRows.get(aRow), aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrixAny.this.set(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrixAny.this.getAndSet(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override protected IMatrix getLA(final List<Integer> aSelectedRows) {
                return new AbstractMatrix() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrixAny.this.get(aSelectedRows.get(aRow), aCol);}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrixAny.this.set(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrixAny.this.getAndSet(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return AbstractMatrixAny.this.columnNumber();}
                };
            }
            @Override protected IMatrix getAL(final List<Integer> aSelectedCols) {
                return new AbstractMatrix() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrixAny.this.get(aRow, aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrixAny.this.set(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrixAny.this.getAndSet(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return AbstractMatrixAny.this.rowNumber();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override protected IMatrix getAA() {
                return new AbstractMatrix() {
                    /** 对于全部切片，则不再需要二次边界检查 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrixAny.this.get_(aRow, aCol);}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrixAny.this.set_(aRow, aCol, aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrixAny.this.getAndSet_(aRow, aCol, aValue);}
                    @Override public int rowNumber() {return AbstractMatrixAny.this.rowNumber();}
                    @Override public int columnNumber() {return AbstractMatrixAny.this.columnNumber();}
                };
            }
            
            @Override protected List<IVector> thisRows_() {return rows();}
            @Override protected List<IVector> thisCols_() {return cols();}
        };
    }
    
    
    
    /** Groovy 的部分，增加矩阵基本的运算操作，由于不能重载 += 之类的变成向自身操作，因此会充斥着值拷贝，因此不推荐重性能的场景使用 */
    @VisibleForTesting @Override public M plus       (double aRHS) {return operation().mapAdd       (this, aRHS);}
    @VisibleForTesting @Override public M minus      (double aRHS) {return operation().mapMinus     (this, aRHS);}
    @VisibleForTesting @Override public M multiply   (double aRHS) {return operation().mapMultiply  (this, aRHS);}
    @VisibleForTesting @Override public M div        (double aRHS) {return operation().mapDivide    (this, aRHS);}
    @VisibleForTesting @Override public M mod        (double aRHS) {return operation().mapMod       (this, aRHS);}
    
    @VisibleForTesting @Override public M plus      (IMatrixGetter aRHS) {return operation().ebeAdd       (this, aRHS);}
    @VisibleForTesting @Override public M minus     (IMatrixGetter aRHS) {return operation().ebeMinus     (this, aRHS);}
    @VisibleForTesting @Override public M multiply  (IMatrixGetter aRHS) {return operation().ebeMultiply  (this, aRHS);}
    @VisibleForTesting @Override public M div       (IMatrixGetter aRHS) {return operation().ebeDivide    (this, aRHS);}
    @VisibleForTesting @Override public M mod       (IMatrixGetter aRHS) {return operation().ebeMod       (this, aRHS);}
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public double call(int aRow, int aCol) {return get(aRow, aCol);}
    @VisibleForTesting @Override public M call(List<Integer> aSelectedRows, List<Integer> aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public M call(SliceType     aSelectedRows, List<Integer> aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public M call(List<Integer> aSelectedRows, SliceType     aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public M call(SliceType     aSelectedRows, SliceType     aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public V call(int           aSelectedRow , List<Integer> aSelectedCols) {return slicer().get(aSelectedRow , aSelectedCols);}
    @VisibleForTesting @Override public V call(int           aSelectedRow , SliceType     aSelectedCols) {return slicer().get(aSelectedRow , aSelectedCols);}
    @VisibleForTesting @Override public V call(List<Integer> aSelectedRows, int           aSelectedCol ) {return slicer().get(aSelectedRows, aSelectedCol );}
    @VisibleForTesting @Override public V call(SliceType     aSelectedRows, int           aSelectedCol ) {return slicer().get(aSelectedRows, aSelectedCol );}
    
    @VisibleForTesting @Override public IMatrixRows_<V, M> getAt(SliceType aSelectedRows) {return new MatrixRowsA_(aSelectedRows);}
    @VisibleForTesting @Override public IMatrixRows_<V, M> getAt(List<Integer> aSelectedRows)  {return new MatrixRowsL_(aSelectedRows);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public IMatrixRow_<V> getAt(int aRow) {return new MatrixRow_((aRow < 0) ? (rowNumber()+aRow) : aRow);}
    
    protected class MatrixRow_ implements IMatrixRow_<V> {
        protected final int mRow;
        protected MatrixRow_(int aRow) {mRow = aRow;}
        
        @Override public V getAt(SliceType aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        @Override public V getAt(List<Integer> aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Number> aList) {refSlicer().get(mRow, aSelectedCols).fill(aList);}
        @Override public void putAt(SliceType aSelectedCols, IVectorGetter aVector) {refSlicer().get(mRow, aSelectedCols).fill(aVector);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aList) {refSlicer().get(mRow, aSelectedCols).fill(aList);}
        @Override public void putAt(List<Integer> aSelectedCols, IVectorGetter aVector) {refSlicer().get(mRow, aSelectedCols).fill(aVector);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public double getAt(int aCol) {return get(mRow, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {set(mRow, (aCol < 0) ? (columnNumber()+aCol) : aCol, aValue);}
    }
    protected class MatrixRowsA_ implements IMatrixRows_<V, M> {
        protected final SliceType mSelectedRows;
        protected MatrixRowsA_(SliceType aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public M getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public M getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Iterable<? extends Number> aList) {refSlicer().get(mSelectedRows, aCol).fill(aList);}
        @Override public void putAt(int aCol, IVectorGetter aVector) {refSlicer().get(mSelectedRows, aCol).fill(aVector);}
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public V getAt(int aCol) {return slicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {refSlicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol).fill(aValue);}
    }
    protected class MatrixRowsL_ implements IMatrixRows_<V, M> {
        protected final List<Integer> mSelectedRows;
        protected MatrixRowsL_(List<Integer> aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public M getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public M getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Iterable<? extends Number> aList) {refSlicer().get(mSelectedRows, aCol).fill(aList);}
        @Override public void putAt(int aCol, IVectorGetter aVector) {refSlicer().get(mSelectedRows, aCol).fill(aVector);}
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public V getAt(int aCol) {return slicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {refSlicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol).fill(aValue);}
    }
    
    /** stuff to override */
    public abstract double get_(int aRow, int aCol);
    public abstract void set_(int aRow, int aCol, double aValue);
    public abstract double getAndSet_(int aRow, int aCol, double aValue); // 返回修改前的值
    public abstract int rowNumber();
    public abstract int columnNumber();
    
    protected String toString_(double aValue) {return String.format(" %8.4g", aValue);}
    
    public abstract IMatrixOperation<M, V> operation();
    
    protected abstract M newZeros_(int aRowNum, int aColNum);
    protected abstract V newZeros_(int aSize);
}
