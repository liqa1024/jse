package com.jtool.math.matrix;

import com.jtool.math.IDataGenerator2;
import com.jtool.math.IDataSlicer2;
import com.jtool.math.operator.IOperator2Full;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


/**
 * @author liqa
 * <p> 通用的矩阵类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractMatrix<T extends Number, M extends IMatrixFull<T, M>> implements IMatrixFull<T, M> {
    /** print */
    @Override public String toString() {return IMatrix.Util.toString_(this, this::toString_);}
    
    /** 矩阵生成器的一般实现，主要实现一些重复的接口 */
    protected abstract class AbstractGenerator implements IDataGenerator2<M> {
        @Override public M ones() {return ones(rowNumber(), columnNumber());}
        @Override public M zeros() {return zeros(rowNumber(), columnNumber());}
        @Override public M from(Callable<? extends Number> aCall) {return from(rowNumber(), columnNumber(), aCall);}
        @Override public M from(IOperator2Full<? extends Number, Integer, Integer> aOpt) {return from(rowNumber(), columnNumber(), aOpt);}
    }
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    @Override public IDataSlicer2<M> slicer() {
        return new IDataSlicer2<M>() {
            private final IDataGenerator2<M> mGen = generator();
            @Override public M getII(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {
                return mGen.from(aSelectedRows.size(), aSelectedCols.size(), (row, col) -> AbstractMatrix.this.get(aSelectedRows.get(row), aSelectedCols.get(col)));
            }
            @Override public M getIB(List<Integer> aSelectedRows, List<Boolean> aSelectedCols) {return getII(aSelectedRows, B2I(aSelectedCols));}
            @Override public M getBI(List<Boolean> aSelectedRows, List<Integer> aSelectedCols) {return getII(B2I(aSelectedRows), aSelectedCols);}
            @Override public M getBB(List<Boolean> aSelectedRows, List<Boolean> aSelectedCols) {return getII(B2I(aSelectedRows), B2I(aSelectedCols));}
            
            @Override public M getAI(final List<Integer> aSelectedCols) {
                return mGen.from(rowNumber(), aSelectedCols.size(), (row, col) -> AbstractMatrix.this.get(row, aSelectedCols.get(col)));
            }
            @Override public M getAB(List<Boolean> aSelectedCols) {return getAI(B2I(aSelectedCols));}
            @Override public M getIA(List<Integer> aSelectedRows) {
                return mGen.from(aSelectedRows.size(), columnNumber(), (row, col) -> AbstractMatrix.this.get(aSelectedRows.get(row), col));
            }
            @Override public M getBA(List<Boolean> aSelectedRows) {return getIA(B2I(aSelectedRows));}
            @Override public M getAA() {return mGen.same();}
        };
    }
    @Override public IDataSlicer2<IMatrix<T>> refSlicer() {
        return new IDataSlicer2<IMatrix<T>>() {
            @Override public IMatrix<T> getII(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {
                return new IMatrix<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aRow, int aCol) {return AbstractMatrix.this.get(aSelectedRows.get(aRow), aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, Number aValue) {AbstractMatrix.this.set(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public T getAndSet_(int aRow, int aCol, Number aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override public IMatrix<T> getIB(List<Integer> aSelectedRows, List<Boolean> aSelectedCols) {return getII(aSelectedRows, B2I(aSelectedCols));}
            @Override public IMatrix<T> getBI(List<Boolean> aSelectedRows, List<Integer> aSelectedCols) {return getII(B2I(aSelectedRows), aSelectedCols);}
            @Override public IMatrix<T> getBB(List<Boolean> aSelectedRows, List<Boolean> aSelectedCols) {return getII(B2I(aSelectedRows), B2I(aSelectedCols));}
            
            @Override public IMatrix<T> getAI(final List<Integer> aSelectedCols) {
                return new IMatrix<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aRow, int aCol) {return AbstractMatrix.this.get(aRow, aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, Number aValue) {AbstractMatrix.this.set(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public T getAndSet_(int aRow, int aCol, Number aValue) {return AbstractMatrix.this.getAndSet(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return AbstractMatrix.this.rowNumber();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override public IMatrix<T> getAB(List<Boolean> aSelectedCols) {return getAI(B2I(aSelectedCols));}
            @Override public IMatrix<T> getIA(final List<Integer> aSelectedRows) {
                return new IMatrix<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aRow, int aCol) {return AbstractMatrix.this.get(aSelectedRows.get(aRow), aCol);}
                    @Override public void set_(int aRow, int aCol, Number aValue) {AbstractMatrix.this.set(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public T getAndSet_(int aRow, int aCol, Number aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return AbstractMatrix.this.columnNumber();}
                };
            }
            @Override public IMatrix<T> getBA(List<Boolean> aSelectedRows) {return getIA(B2I(aSelectedRows));}
            @Override public IMatrix<T> getAA() {return AbstractMatrix.this;}
        };
    }
    
    /**
     * 将 {@code List<Boolean>} 值拷贝转为 {@code List<Integer>} 方便使用；
     * 为了代码简洁并且保证可读性，统一都使用 B2I 来处理 Boolean 的输入，对于超大的矩阵切片可能会有一定的性能影响
     */
    private static List<Integer> B2I(List<Boolean> aSelectedB) {
        List<Integer> rSelectedI = new ArrayList<>();
        int tCol = 0;
        for (boolean select : aSelectedB) {
            if (select) rSelectedI.add(tCol);
            ++tCol;
        }
        return rSelectedI;
    }
    
    
    /** stuff to override */
    public abstract T get_(int aRow, int aCol);
    public abstract void set_(int aRow, int aCol, Number aValue);
    public abstract T getAndSet_(int aRow, int aCol, Number aValue);
    public abstract int rowNumber();
    public abstract int columnNumber();
    
    public abstract IDataGenerator2<M> generator();
    
    protected String toString_(T aValue) {return String.format(" %8.4g", aValue.doubleValue());}
}
