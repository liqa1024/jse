package jtool.math.matrix;

import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.vector.IComplexVector;
import jtool.math.vector.IVector;
import jtool.math.vector.RefComplexVector;

import java.util.List;
import java.util.NoSuchElementException;

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
                return getReal_(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag_(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get_(oRow, oCol);}
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
                setReal_(oRow, oCol, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                setImag_(oRow, oCol, aImag);
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
                return getReal_(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag_(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get_(oRow, oCol);}
            @Override public void set(IComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oRow, oCol, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oRow, oCol, aValue);
            }
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oRow, oCol, aValue);
            }
        };
    }
    
    
    @Override public ComplexDouble get(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return get_(aRow, aCol);
    }
    @Override public double getReal(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getReal_(aRow, aCol);
    }
    @Override public double getImag(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getImag_(aRow, aCol);
    }
    @Override public void set(int aRow, int aCol, IComplexDouble aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        set_(aRow, aCol, aValue);
    }
    @Override public void set(int aRow, int aCol, ComplexDouble aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        set_(aRow, aCol, aValue);
    }
    @Override public void set(int aRow, int aCol, double aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        set_(aRow, aCol, aValue);
    }
    @Override public void setReal(int aRow, int aCol, double aReal) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        setReal_(aRow, aCol, aReal);
    }
    @Override public void setImag(int aRow, int aCol, double aImag) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        setImag_(aRow, aCol, aImag);
    }
    @Override public ComplexDouble getAndSet(int aRow, int aCol, IComplexDouble aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSet_(aRow, aCol, aValue);
    }
    @Override public ComplexDouble getAndSet(int aRow, int aCol, ComplexDouble aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSet_(aRow, aCol, aValue);
    }
    @Override public ComplexDouble getAndSet(int aRow, int aCol, double aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSet_(aRow, aCol, aValue);
    }
    @Override public double getAndSetReal(int aRow, int aCol, double aReal) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSetReal_(aRow, aCol, aReal);
    }
    @Override public double getAndSetImag(int aRow, int aCol, double aImag) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSetImag_(aRow, aCol, aImag);
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
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new RefComplexVector() {
            /** 对于全部切片，则不再需要二次边界检查 */
            @Override public double getReal_(int aIdx) {return AbstractComplexMatrix.this.getReal_(aRow, aIdx);}
            @Override public double getImag_(int aIdx) {return AbstractComplexMatrix.this.getImag_(aRow, aIdx);}
            @Override public void setReal_(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal_(aRow, aIdx, aReal);}
            @Override public void setImag_(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag_(aRow, aIdx, aImag);}
            @Override public double getAndSetReal_(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal_(aRow, aIdx, aReal);}
            @Override public double getAndSetImag_(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag_(aRow, aIdx, aImag);}
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
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new RefComplexVector() {
            /** 对于全部切片，则不再需要二次边界检查 */
            @Override public double getReal_(int aIdx) {return AbstractComplexMatrix.this.getReal_(aIdx, aCol);}
            @Override public double getImag_(int aIdx) {return AbstractComplexMatrix.this.getImag_(aIdx, aCol);}
            @Override public void setReal_(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal_(aIdx, aCol, aReal);}
            @Override public void setImag_(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag_(aIdx, aCol, aImag);}
            @Override public double getAndSetReal_(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal_(aIdx, aCol, aReal);}
            @Override public double getAndSetImag_(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag_(aIdx, aCol, aImag);}
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
    public ComplexDouble get_(int aRow, int aCol) {return new ComplexDouble(getReal_(aRow, aCol), getImag_(aRow, aCol));}
    public abstract double getReal_(int aRow, int aCol);
    public abstract double getImag_(int aRow, int aCol);
    public void set_(int aRow, int aCol, IComplexDouble aValue) {setReal_(aRow, aCol, aValue.real()); setImag_(aRow, aCol, aValue.imag());}
    public void set_(int aRow, int aCol, ComplexDouble aValue) {setReal_(aRow, aCol, aValue.mReal); setImag_(aRow, aCol, aValue.mImag);}
    public void set_(int aRow, int aCol, double aValue) {setReal_(aRow, aCol, aValue); setImag_(aRow, aCol, 0.0);}
    public abstract void setReal_(int aRow, int aCol, double aReal);
    public abstract void setImag_(int aRow, int aCol, double aImag);
    public ComplexDouble getAndSet_(int aRow, int aCol, IComplexDouble aValue) {return new ComplexDouble(getAndSetReal_(aRow, aCol, aValue.real()), getAndSetImag_(aRow, aCol, aValue.imag()));}
    public ComplexDouble getAndSet_(int aRow, int aCol, ComplexDouble aValue) {return new ComplexDouble(getAndSetReal_(aRow, aCol, aValue.mReal), getAndSetImag_(aRow, aCol, aValue.mImag));}
    public ComplexDouble getAndSet_(int aRow, int aCol, double aValue) {return new ComplexDouble(getAndSetReal_(aRow, aCol, aValue), getAndSetImag_(aRow, aCol, 0.0));}
    public abstract double getAndSetReal_(int aRow, int aCol, double aReal);
    public abstract double getAndSetImag_(int aRow, int aCol, double aImag);
    public abstract int rowNumber();
    public abstract int columnNumber();
    
    protected String toString_(double aReal, double aImag) {return Double.compare(aImag, 0.0)>=0 ? String.format("   %.4g + %.4gi", aReal, aImag) : String.format("   %.4g - %.4gi", aReal, -aImag);}
}
