package jse.math.matrix;

import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetOnlyIterator;
import jse.math.operation.DATA;
import jse.math.vector.IIntVector;
import jse.math.vector.IntVector;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;
import static jse.math.matrix.AbstractMatrixOperation.ebeCheck;

/**
 * 一般的整数矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractIntMatrixOperation implements IIntMatrixOperation {
    @Override public void fill          (int aRHS) {DATA.mapFill2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void fill          (IIntMatrix aRHS) {ebeCheck(thisMatrix_().nrows(), thisMatrix_().ncols(), aRHS.nrows(), aRHS.ncols()); DATA.ebeFill2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void assignCol     (IntSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorCol, aSup);}
    @Override public void assignRow     (IntSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorRow, aSup);}
    @Override public void forEachCol    (IntConsumer aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorCol, aCon);}
    @Override public void forEachRow    (IntConsumer aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorRow, aCon);}
    @Override public void fill          (IIntMatrixGetter aRHS) {
        final IIntMatrix tThis = thisMatrix_();
        final IIntSetOnlyIterator si = tThis.setIteratorCol();
        final int tColNum = tThis.ncols();
        final int tRowNum = tThis.nrows();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            si.nextAndSet(aRHS.get(row, col));
        }
    }
    
    @Override public IIntMatrix transpose() {
        final IIntMatrix tThis = thisMatrix_();
        IIntMatrix rMatrix = newMatrix_(tThis.ncols(), tThis.nrows());
        final IIntIterator it = tThis.iteratorCol();
        final IIntSetOnlyIterator si = rMatrix.setIteratorRow();
        while (it.hasNext()) si.nextAndSet(it.next());
        return rMatrix;
    }
    @Override public IIntMatrix refTranspose() {
        return new RefIntMatrix() {
            private final IIntMatrix mThis = thisMatrix_();
            @Override public int get(int aRow, int aCol) {rangeCheckRow(aRow, nrows()); rangeCheckCol(aCol, this.ncols()); return mThis.get(aCol, aRow);}
            @Override public void set(int aRow, int aCol, int aValue)  {rangeCheckRow(aRow, nrows()); rangeCheckCol(aCol, this.ncols()); mThis.set(aCol, aRow, aValue);}
            @Override public int getAndSet(int aRow, int aCol, int aValue) {rangeCheckRow(aRow, nrows()); rangeCheckCol(aCol, this.ncols()); return mThis.getAndSet(aCol, aRow, aValue);}
            @Override public int nrows() {return mThis.ncols();}
            @Override public int ncols() {return mThis.nrows();}
        };
    }
    
    @Override public boolean isDiag() {
        final IIntMatrix tThis = thisMatrix_();
        
        final IIntIterator it = tThis.iteratorCol();
        final int tRowNum = tThis.nrows();
        final int tColNum = tThis.ncols();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            double tValue = it.next();
            if (col != row) if (tValue != 0.0) return false;
        }
        return true;
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private IIntMatrix newMatrix_() {
        final IIntMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.nrows(), tThis.ncols());
    }
    
    /** stuff to override */
    protected abstract IIntMatrix thisMatrix_();
    protected abstract IIntMatrix newMatrix_(int aRowNum, int aColNum);
    protected IIntVector newVector_(int aSize) {return IntVector.zeros(aSize);}
}
