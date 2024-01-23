package jtool.math.matrix;

import jtool.code.collection.AbstractCollections;
import jtool.code.collection.AbstractRandomAccessList;
import jtool.math.vector.IIntVector;
import jtool.math.vector.RefIntVector;

import java.util.List;

import static jtool.math.matrix.AbstractMatrix.rangeCheckCol;
import static jtool.math.matrix.AbstractMatrix.rangeCheckRow;

public abstract class AbstractIntMatrix implements IIntMatrix {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Complex Matrix:", rowNumber(), columnNumber()));
        List<IIntVector> tRows = rows();
        for (IIntVector tRow : tRows) {
            rStr.append("\n");
            tRow.forEach(v -> rStr.append(toString_(v)));
        }
        return rStr.toString();
    }
    
    /** 转换为其他类型 */
    @Override public List<List<Integer>> asListCols() {return AbstractCollections.map(cols(), IIntVector::asList);}
    @Override public List<List<Integer>> asListRows() {return AbstractCollections.map(rows(), IIntVector::asList);}
    @Override public IIntVector asVecCol() {
        return new RefIntVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
//            @Override public IDoubleIterator iterator() {return iteratorCol();}
//            @Override public IDoubleSetIterator setIterator() {return setIteratorCol();}
        };
    }
    @Override public IIntVector asVecRow() {
        return new RefIntVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aIdx/mColNum, aIdx%mColNum);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
//            @Override public IDoubleIterator iterator() {return iteratorRow();}
//            @Override public IDoubleSetIterator setIterator() {return setIteratorRow();}
        };
    }
    
    
    @Override public IMatrix.ISize size() {
        return new IMatrix.ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    
    
    @Override public List<IIntVector> rows() {
        return new AbstractRandomAccessList<IIntVector>() {
            @Override public int size() {return rowNumber();}
            @Override public IIntVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IIntVector row(final int aRow) {
        rangeCheckRow(aRow, rowNumber());
        return new RefIntVector() {
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aRow, aIdx);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aRow, aIdx, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aRow, aIdx, aValue);}
            @Override public int size() {return columnNumber();}
//            @Override public IDoubleIterator iterator() {return iteratorRowAt(aRow);}
//            @Override public IDoubleSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<IIntVector> cols() {
        return new AbstractRandomAccessList<IIntVector>() {
            @Override public int size() {return columnNumber();}
            @Override public IIntVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IIntVector col(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new RefIntVector() {
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aIdx, aCol);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aIdx, aCol, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aIdx, aCol, aValue);}
            @Override public int size() {return rowNumber();}
//            @Override public IDoubleIterator iterator() {return iteratorColAt(aCol);}
//            @Override public IDoubleSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    
    /** stuff to override */
    public abstract int get(int aRow, int aCol);
    public abstract void set(int aRow, int aCol, int aValue);
    public abstract int getAndSet(int aRow, int aCol, int aValue); // 返回修改前的值
    public abstract int rowNumber();
    public abstract int columnNumber();
    
    protected String toString_(int aValue) {return " "+aValue;}
}
