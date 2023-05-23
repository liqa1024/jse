package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.math.vector.AbstractVector;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGenerator;
import com.jtool.math.vector.IVectorGetter;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.Callable;


/**
 * @author liqa
 * <p> 通用的矩阵类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractMatrixFull<T extends Number, M extends IMatrix<T>, V extends IVector<T>> extends AbstractMatrix<T> implements IMatrixFull<T, M, V> {
    /** 矩阵生成器的一般实现，主要实现一些重复的接口 */
    protected class MatrixGenerator implements IMatrixGenerator<M> {
        @Override public M ones() {return ones(rowNumber(), columnNumber());}
        @Override public M zeros() {return zeros(rowNumber(), columnNumber());}
        @Override public M from(Callable<? extends Number> aCall) {return from(rowNumber(), columnNumber(), aCall);}
        @Override public M from(IMatrixGetter<? extends Number> aMatrixGetter) {return from(rowNumber(), columnNumber(), aMatrixGetter);}
        
        @Override public M same() {return from(AbstractMatrixFull.this::get_);}
        @Override public M ones(int aRowNum, int aColNum) {
            M rMatrix = newZeros(aRowNum, aColNum);
            rMatrix.fill(1);
            return rMatrix;
        }
        @Override public M zeros(int aRowNum, int aColNum) {
            return newZeros(aRowNum, aColNum);
        }
        @Override public M from(int aRowNum, int aColNum, Callable<? extends Number> aCall) {
            return from(aRowNum, aColNum, (row, col) -> {try {return aCall.call();} catch (Exception e) {throw new RuntimeException(e);}});
        }
        @Override public M from(int aRowNum, int aColNum, IMatrixGetter<? extends Number> aMatrixGetter) {
            M rMatrix = newZeros(aRowNum, aColNum);
            rMatrix.fillWith(aMatrixGetter);
            return rMatrix;
        }
    }
    protected class VectorGenerator implements IVectorGenerator<V> {
        @Override public V ones() {return ones(rowNumber()*columnNumber());}
        @Override public V zeros() {return zeros(rowNumber()*columnNumber());}
        @Override public V from(Callable<? extends Number> aCall) {return from(rowNumber()*columnNumber(), aCall);}
        @Override public V from(IVectorGetter<? extends Number> aVectorGetter) {return from(rowNumber()*columnNumber(), aVectorGetter);}
        /** 转为 Vector 需要按照列排列 */
        @Override public V same() {int tRowNum = rowNumber(); return from(i -> get_(i%tRowNum, i/tRowNum));}
        
        @Override public V ones(int aSize) {
            V rVector = newZeros(aSize);
            rVector.fill(1);
            return rVector;
        }
        @Override public V zeros(int aSize) {
            return newZeros(aSize);
        }
        @Override public V from(int aSize, Callable<? extends Number> aCall) {
            return from(aSize, i -> {try {return aCall.call();} catch (Exception e) {throw new RuntimeException(e);}});
        }
        @Override public V from(int aSize, IVectorGetter<? extends Number> aVectorGetter) {
            V rVector = newZeros(aSize);
            rVector.fillWith(aVectorGetter);
            return rVector;
        }
    }
    
    @Override public IVectorGenerator<V> generatorVec() {return new VectorGenerator();}
    @Override public IMatrixGenerator<M> generatorMat() {return new MatrixGenerator();}
    
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    @Override public IMatrixSlicer<M, V> slicer() {
        return new IMatrixSlicer<M, V>() {
            @Override public V getIL(final int aSelectedRow, final List<Integer> aSelectedCols) {return generatorVec().from(aSelectedCols.size(), i -> AbstractMatrixFull.this.get(aSelectedRow, aSelectedCols.get(i)));}
            @Override public V getLI(final List<Integer> aSelectedRows, final int aSelectedCol) {return generatorVec().from(aSelectedRows.size(), i -> AbstractMatrixFull.this.get(aSelectedRows.get(i), aSelectedCol));}
            @Override public V getIA(final int aSelectedRow) {return generatorVec().from(columnNumber(), col -> AbstractMatrixFull.this.get(aSelectedRow, col));}
            @Override public V getAI(final int aSelectedCol) {return generatorVec().from(rowNumber()   , row -> AbstractMatrixFull.this.get(row, aSelectedCol));}
            
            @Override public M getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {return generatorMat().from(aSelectedRows.size(), aSelectedCols.size(), (row, col) -> AbstractMatrixFull.this.get(aSelectedRows.get(row), aSelectedCols.get(col)));}
            @Override public M getLA(final List<Integer> aSelectedRows) {return generatorMat().from(aSelectedRows.size(), columnNumber()      , (row, col) -> AbstractMatrixFull.this.get(aSelectedRows.get(row), col));}
            @Override public M getAL(final List<Integer> aSelectedCols) {return generatorMat().from(rowNumber()         , aSelectedCols.size(), (row, col) -> AbstractMatrixFull.this.get(row, aSelectedCols.get(col)));}
            @Override public M getAA() {return generatorMat().same();}
        };
    }
    @Override public IMatrixSlicer<IMatrix<T>, IVector<T>> refSlicer() {
        return new IMatrixSlicer<IMatrix<T>, IVector<T>>() {
            @Override public IVector<T> getIL(final int aSelectedRow, final List<Integer> aSelectedCols) {
                return new AbstractVector<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aIdx) {return AbstractMatrixFull.this.get(aSelectedRow, aSelectedCols.get(aIdx));}
                    @Override public void set_(int aIdx, Number aValue) {AbstractMatrixFull.this.set(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public T getAndSet_(int aIdx, Number aValue) {return AbstractMatrixFull.this.getAndSet(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public int size() {return aSelectedCols.size();}
                };
            }
            @Override public IVector<T> getLI(final List<Integer> aSelectedRows, final int aSelectedCol) {
                return new AbstractVector<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aIdx) {return AbstractMatrixFull.this.get(aSelectedRows.get(aIdx), aSelectedCol);}
                    @Override public void set_(int aIdx, Number aValue) {AbstractMatrixFull.this.set(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public T getAndSet_(int aIdx, Number aValue) {return AbstractMatrixFull.this.getAndSet(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public int size() {return aSelectedRows.size();}
                };
            }
            @Override public IVector<T> getIA(final int aSelectedRow) {
                return new AbstractVector<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aCol) {return AbstractMatrixFull.this.get(aSelectedRow, aCol);}
                    @Override public void set_(int aCol, Number aValue) {AbstractMatrixFull.this.set(aSelectedRow, aCol, aValue);}
                    @Override public T getAndSet_(int aCol, Number aValue) {return AbstractMatrixFull.this.getAndSet(aSelectedRow, aCol, aValue);}
                    @Override public int size() {return columnNumber();}
                };
            }
            @Override public IVector<T> getAI(final int aSelectedCol) {
                return new AbstractVector<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aRow) {return AbstractMatrixFull.this.get(aRow, aSelectedCol);}
                    @Override public void set_(int aRow, Number aValue) {AbstractMatrixFull.this.set(aRow, aSelectedCol, aValue);}
                    @Override public T getAndSet_(int aRow, Number aValue) {return AbstractMatrixFull.this.getAndSet(aRow, aSelectedCol, aValue);}
                    @Override public int size() {return rowNumber();}
                };
            }
            
            @Override public IMatrix<T> getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {
                return new AbstractMatrix<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aRow, int aCol) {return AbstractMatrixFull.this.get(aSelectedRows.get(aRow), aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, Number aValue) {AbstractMatrixFull.this.set(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public T getAndSet_(int aRow, int aCol, Number aValue) {return AbstractMatrixFull.this.getAndSet(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override public IMatrix<T> getLA(final List<Integer> aSelectedRows) {
                return new AbstractMatrix<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aRow, int aCol) {return AbstractMatrixFull.this.get(aSelectedRows.get(aRow), aCol);}
                    @Override public void set_(int aRow, int aCol, Number aValue) {AbstractMatrixFull.this.set(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public T getAndSet_(int aRow, int aCol, Number aValue) {return AbstractMatrixFull.this.getAndSet(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return AbstractMatrixFull.this.columnNumber();}
                };
            }
            @Override public IMatrix<T> getAL(final List<Integer> aSelectedCols) {
                return new AbstractMatrix<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aRow, int aCol) {return AbstractMatrixFull.this.get(aRow, aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, Number aValue) {AbstractMatrixFull.this.set(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public T getAndSet_(int aRow, int aCol, Number aValue) {return AbstractMatrixFull.this.getAndSet(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return AbstractMatrixFull.this.rowNumber();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override public IMatrix<T> getAA() {return AbstractMatrixFull.this;}
        };
    }
    
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public M call(List<Integer> aSelectedRows, List<Integer> aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public M call(SliceType     aSelectedRows, List<Integer> aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public M call(List<Integer> aSelectedRows, SliceType     aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public M call(SliceType     aSelectedRows, SliceType     aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public V call(int           aSelectedRow , List<Integer> aSelectedCols) {return slicer().get(aSelectedRow , aSelectedCols);}
    @VisibleForTesting @Override public V call(int           aSelectedRow , SliceType     aSelectedCols) {return slicer().get(aSelectedRow , aSelectedCols);}
    @VisibleForTesting @Override public V call(List<Integer> aSelectedRows, int           aSelectedCol ) {return slicer().get(aSelectedRows, aSelectedCol );}
    @VisibleForTesting @Override public V call(SliceType     aSelectedRows, int           aSelectedCol ) {return slicer().get(aSelectedRows, aSelectedCol);}
    
    @VisibleForTesting @Override public IMatrixRowFull_<T, V> getAt(int aRow) {return new MatrixRowFull_(aRow);}
    @VisibleForTesting @Override public IMatrixRows_<T, V, M> getAt(SliceType aSelectedRows) {return new MatrixRowsA_(aSelectedRows);}
    @VisibleForTesting @Override public IMatrixRows_<T, V, M> getAt(List<Integer> aSelectedRows)  {return new MatrixRowsL_(aSelectedRows);}
    
    protected class MatrixRowFull_ extends MatrixRow_ implements IMatrixRowFull_<T, V> {
        protected MatrixRowFull_(int aRow) {super(aRow);}
        
        @Override public V getAt(SliceType aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        @Override public V getAt(List<Integer> aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        
        @Override public void putAt(SliceType aSelectedCols, Number aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Number> aVec) {refSlicer().get(mRow, aSelectedCols).fillWith(aVec);}
        @Override public void putAt(List<Integer> aSelectedCols, Number aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aVec) {refSlicer().get(mRow, aSelectedCols).fillWith(aVec);}
    }
    protected class MatrixRowsA_ implements IMatrixRows_<T, V, M> {
        protected final SliceType mSelectedRows;
        protected MatrixRowsA_(SliceType aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public V getAt(int aCol) {return slicer().get(mSelectedRows, aCol);}
        @Override public M getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public M getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Number aValue) {refSlicer().get(mSelectedRows, aCol).fill(aValue);}
        @Override public void putAt(int aCol, Iterable<? extends Number> aVec) {refSlicer().get(mSelectedRows, aCol).fillWith(aVec);}
        @Override public void putAt(SliceType aSelectedCols, Number aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrix<? extends Number> aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, Number aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrix<? extends Number> aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aMatrix);}
    }
    protected class MatrixRowsL_ implements IMatrixRows_<T, V, M> {
        protected final List<Integer> mSelectedRows;
        protected MatrixRowsL_(List<Integer> aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public V getAt(int aCol) {return slicer().get(mSelectedRows, aCol);}
        @Override public M getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public M getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Number aValue) {refSlicer().get(mSelectedRows, aCol).fill(aValue);}
        @Override public void putAt(int aCol, Iterable<? extends Number> aVec) {refSlicer().get(mSelectedRows, aCol).fillWith(aVec);}
        @Override public void putAt(SliceType aSelectedCols, Number aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrix<? extends Number> aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, Number aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrix<? extends Number> aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fillWith(aMatrix);}
    }
    
    
    /** stuff to override */
    public abstract T get_(int aRow, int aCol);
    public abstract void set_(int aRow, int aCol, Number aValue);
    public abstract T getAndSet_(int aRow, int aCol, Number aValue);
    public abstract int rowNumber();
    public abstract int columnNumber();
    
    protected abstract M newZeros(int aRowNum, int aColNum);
    protected abstract V newZeros(int aSize);
}
