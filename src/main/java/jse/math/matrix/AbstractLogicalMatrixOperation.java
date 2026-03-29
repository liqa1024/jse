package jse.math.matrix;

import jse.code.functional.IBooleanConsumer;
import jse.code.iterator.IBooleanIterator;
import jse.code.iterator.IBooleanSetOnlyIterator;
import jse.math.operation.DATA;
import jse.math.vector.ILogicalVector;
import jse.math.vector.LogicalVector;

import java.util.function.BooleanSupplier;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;
import static jse.math.matrix.AbstractMatrixOperation.ebeCheck;

/**
 * 一般的逻辑矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractLogicalMatrixOperation implements ILogicalMatrixOperation {
    @Override public void fill          (boolean aRHS) {DATA.mapFill2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void fill          (ILogicalMatrix aRHS) {ebeCheck(thisMatrix_().nrows(), thisMatrix_().ncols(), aRHS.nrows(), aRHS.ncols()); DATA.ebeFill2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void assignCol     (BooleanSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorCol, aSup);}
    @Override public void assignRow     (BooleanSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorRow, aSup);}
    @Override public void forEachCol    (IBooleanConsumer aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorCol, aCon);}
    @Override public void forEachRow    (IBooleanConsumer aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorRow, aCon);}
    @Override public void fill          (ILogicalMatrixGetter aRHS) {
        final ILogicalMatrix tThis = thisMatrix_();
        final IBooleanSetOnlyIterator si = tThis.setIteratorCol();
        final int tColNum = tThis.ncols();
        final int tRowNum = tThis.nrows();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            si.nextAndSet(aRHS.get(row, col));
        }
    }
    
    @Override public ILogicalMatrix transpose() {
        final ILogicalMatrix tThis = thisMatrix_();
        ILogicalMatrix rMatrix = newMatrix_(tThis.ncols(), tThis.nrows());
        final IBooleanIterator it = tThis.iteratorCol();
        final IBooleanSetOnlyIterator si = rMatrix.setIteratorRow();
        while (it.hasNext()) si.nextAndSet(it.next());
        return rMatrix;
    }
    @Override public ILogicalMatrix refTranspose() {
        return new RefLogicalMatrix() {
            private final ILogicalMatrix mThis = thisMatrix_();
            @Override public boolean get(int aRow, int aCol) {rangeCheckRow(aRow, nrows()); rangeCheckCol(aCol, this.ncols()); return mThis.get(aCol, aRow);}
            @Override public void set(int aRow, int aCol, boolean aValue)  {rangeCheckRow(aRow, nrows()); rangeCheckCol(aCol, this.ncols()); mThis.set(aCol, aRow, aValue);}
            @Override public boolean getAndSet(int aRow, int aCol, boolean aValue) {rangeCheckRow(aRow, nrows()); rangeCheckCol(aCol, this.ncols()); return mThis.getAndSet(aCol, aRow, aValue);}
            @Override public int nrows() {return mThis.ncols();}
            @Override public int ncols() {return mThis.nrows();}
        };
    }
    
    @Override public boolean isDiag() {
        final ILogicalMatrix tThis = thisMatrix_();
        
        final IBooleanIterator it = tThis.iteratorCol();
        final int tRowNum = tThis.nrows();
        final int tColNum = tThis.ncols();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            boolean tValue = it.next();
            if (col != row) if (tValue) return false;
        }
        return true;
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private ILogicalMatrix newMatrix_() {
        final ILogicalMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.nrows(), tThis.ncols());
    }
    
    /** stuff to override */
    protected abstract ILogicalMatrix thisMatrix_();
    protected abstract ILogicalMatrix newMatrix_(int aRowNum, int aColNum);
    protected ILogicalVector newVector_(int aSize) {return LogicalVector.zeros(aSize);}
}
