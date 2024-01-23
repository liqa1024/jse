package jtool.math.matrix;

import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.vector.IComplexVector;
import jtool.math.vector.RefComplexVector;

import java.util.List;
import java.util.NoSuchElementException;

import static jtool.math.matrix.AbstractMatrix.rangeCheckCol;
import static jtool.math.matrix.AbstractMatrix.rangeCheckRow;

public abstract class AbstractComplexMatrix implements IComplexMatrix {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Complex Matrix:", rowNumber(), columnNumber()));
        List<IComplexVector> tRows = rows();
        for (IComplexVector tRow : tRows) {
            rStr.append("\n");
            tRow.forEach((real, imag) -> rStr.append(toString_(real, imag)));
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IComplexDoubleIterator iteratorCol() {
        return new IComplexDoubleIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, oCol);}
        };
    }
    @Override public IComplexDoubleSetIterator setIteratorCol() {
        return new IComplexDoubleSetIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void setReal(double aReal) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setReal(oRow, oCol, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setImag(oRow, oCol, aImag);
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, oCol);}
            @Override public void set(IComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
        };
    }
    
    
    @Override public IMatrix.ISize size() {
        return new IMatrix.ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    
    
    @Override public List<IComplexVector> rows() {
        return new AbstractRandomAccessList<IComplexVector>() {
            @Override public int size() {return rowNumber();}
            @Override public IComplexVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IComplexVector row(final int aRow) {
        rangeCheckRow(aRow, rowNumber());
        return new RefComplexVector() {
            /** 对于全部切片，则不再需要二次边界检查 */
            @Override public double getReal(int aIdx) {return AbstractComplexMatrix.this.getReal(aRow, aIdx);}
            @Override public double getImag(int aIdx) {return AbstractComplexMatrix.this.getImag(aRow, aIdx);}
            @Override public void setReal(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal(aRow, aIdx, aReal);}
            @Override public void setImag(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag(aRow, aIdx, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal(aRow, aIdx, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag(aRow, aIdx, aImag);}
            @Override public int size() {return columnNumber();}
//            @Override public IDoubleIterator iterator() {return iteratorRowAt(aRow);}
//            @Override public IDoubleSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<IComplexVector> cols() {
        return new AbstractRandomAccessList<IComplexVector>() {
            @Override public int size() {return columnNumber();}
            @Override public IComplexVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IComplexVector col(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new RefComplexVector() {
            /** 对于全部切片，则不再需要二次边界检查 */
            @Override public double getReal(int aIdx) {return AbstractComplexMatrix.this.getReal(aIdx, aCol);}
            @Override public double getImag(int aIdx) {return AbstractComplexMatrix.this.getImag(aIdx, aCol);}
            @Override public void setReal(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal(aIdx, aCol, aReal);}
            @Override public void setImag(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag(aIdx, aCol, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal(aIdx, aCol, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag(aIdx, aCol, aImag);}
            @Override public int size() {return columnNumber();}
//            @Override public IDoubleIterator iterator() {return iteratorColAt(aCol);}
//            @Override public IDoubleSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    
    /** 矩阵的运算器 */
    @Override public IComplexMatrixOperation operation() {
        return new AbstractComplexMatrixOperation() {
            @Override protected IComplexMatrix thisMatrix_() {return AbstractComplexMatrix.this;}
        };
    }
    
    @Override public final void plus2this       (IComplexMatrix aRHS) {operation().plus2this    (aRHS);}
    
    
    
    /** stuff to override */
    public ComplexDouble get(int aRow, int aCol) {return new ComplexDouble(getReal(aRow, aCol), getImag(aRow, aCol));}
    public abstract double getReal(int aRow, int aCol);
    public abstract double getImag(int aRow, int aCol);
    public void set(int aRow, int aCol, IComplexDouble aValue) {setReal(aRow, aCol, aValue.real()); setImag(aRow, aCol, aValue.imag());}
    public void set(int aRow, int aCol, ComplexDouble aValue) {setReal(aRow, aCol, aValue.mReal); setImag(aRow, aCol, aValue.mImag);}
    public void set(int aRow, int aCol, double aValue) {setReal(aRow, aCol, aValue); setImag(aRow, aCol, 0.0);}
    public abstract void setReal(int aRow, int aCol, double aReal);
    public abstract void setImag(int aRow, int aCol, double aImag);
    public ComplexDouble getAndSet(int aRow, int aCol, IComplexDouble aValue) {return new ComplexDouble(getAndSetReal(aRow, aCol, aValue.real()), getAndSetImag(aRow, aCol, aValue.imag()));}
    public ComplexDouble getAndSet(int aRow, int aCol, ComplexDouble aValue) {return new ComplexDouble(getAndSetReal(aRow, aCol, aValue.mReal), getAndSetImag(aRow, aCol, aValue.mImag));}
    public ComplexDouble getAndSet(int aRow, int aCol, double aValue) {return new ComplexDouble(getAndSetReal(aRow, aCol, aValue), getAndSetImag(aRow, aCol, 0.0));}
    public abstract double getAndSetReal(int aRow, int aCol, double aReal);
    public abstract double getAndSetImag(int aRow, int aCol, double aImag);
    public abstract int rowNumber();
    public abstract int columnNumber();
    
    protected String toString_(double aReal, double aImag) {return Double.compare(aImag, 0.0)>=0 ? String.format("   %.4g + %.4gi", aReal, aImag) : String.format("   %.4g - %.4gi", aReal, -aImag);}
}
