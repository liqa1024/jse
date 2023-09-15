package com.jtool.math.matrix;

import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.code.functional.IDoubleOperator2;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.math.operation.DATA;
import com.jtool.math.vector.IVector;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation implements IMatrixOperation {
    /** 通用的一些运算 */
    @Override public IMatrix plus       (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebePlus2Dest    (tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix minus      (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeMinus2Dest   (tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lminus     (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeMinus2Dest   (tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix multiply   (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeMultiply2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix div        (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeDiv2Dest     (tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix ldiv       (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeDiv2Dest     (tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix mod        (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeMod2Dest     (tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lmod       (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeMod2Dest     (tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix operate    (IMatrix aRHS, IDoubleOperator2 aOpt) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.ebeDo2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
    @Override public IMatrix plus       (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapPlus2Dest     (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix minus      (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapMinus2Dest    (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lminus     (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapLMinus2Dest   (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix multiply   (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapMultiply2Dest (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix div        (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapDiv2Dest      (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix ldiv       (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapLDiv2Dest     (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix mod        (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapMod2Dest      (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lmod       (double aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapLMod2Dest     (tThis::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix map        (IDoubleOperator1 aOpt) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.size()); DATA.mapDo2Dest(tThis::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
    @Override public void plus2this     (IMatrix aRHS) {DATA.ebePlus2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void minus2this    (IMatrix aRHS) {DATA.ebeMinus2This   (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lminus2this   (IMatrix aRHS) {DATA.ebeLMinus2This  (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void multiply2this (IMatrix aRHS) {DATA.ebeMultiply2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void div2this      (IMatrix aRHS) {DATA.ebeDiv2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void ldiv2this     (IMatrix aRHS) {DATA.ebeLDiv2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void mod2this      (IMatrix aRHS) {DATA.ebeMod2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lmod2this     (IMatrix aRHS) {DATA.ebeLMod2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void operate2this  (IMatrix aRHS, IDoubleOperator2 aOpt) {DATA.ebeDo2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol, aOpt);}
    
    @Override public void plus2this     (double aRHS) {DATA.mapPlus2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void minus2this    (double aRHS) {DATA.mapMinus2This   (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lminus2this   (double aRHS) {DATA.mapLMinus2This  (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void multiply2this (double aRHS) {DATA.mapMultiply2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void div2this      (double aRHS) {DATA.mapDiv2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void ldiv2this     (double aRHS) {DATA.mapLDiv2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void mod2this      (double aRHS) {DATA.mapMod2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lmod2this     (double aRHS) {DATA.mapLMod2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void map2this      (IDoubleOperator1 aOpt) {DATA.mapDo2This(thisMatrix_()::setIteratorCol, aOpt);}
    
    @Override public void fill          (double aRHS) {DATA.mapFill2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void fill          (IMatrix aRHS) {DATA.ebeFill2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void assignCol     (IDoubleSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorCol, aSup);}
    @Override public void assignRow     (IDoubleSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorRow, aSup);}
    @Override public void forEachCol    (IDoubleConsumer1 aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorCol, aCon);}
    @Override public void forEachRow    (IDoubleConsumer1 aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorRow, aCon);}
    @Override public void fill          (IMatrixGetter aRHS) {
        final IMatrix tThis = thisMatrix_();
        final IDoubleSetIterator si = tThis.setIteratorCol();
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            si.nextAndSet(aRHS.get(row, col));
        }
    }
    
    @Override public double sum () {return DATA.sumOfThis (thisMatrix_()::iteratorCol);}
    @Override public double mean() {return DATA.meanOfThis(thisMatrix_()::iteratorCol);}
    @Override public double max () {return DATA.maxOfThis (thisMatrix_()::iteratorCol);}
    @Override public double min () {return DATA.minOfThis (thisMatrix_()::iteratorCol);}
    
    
    /** 这里直接调用对应向量的运算，现在可以利用上所有的优化 */
    @Override public IVector sumOfCols() {
        final IMatrix tThis = thisMatrix_();
        
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            rVector.set_(col, tThis.col(col).sum());
        }
        return rVector;
    }
    @Override public IVector sumOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final int tRowNum = tThis.rowNumber();
        IVector rVector = tThis.newZerosVec(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            rVector.set_(row, tThis.row(row).sum());
        }
        return rVector;
    }
    
    @Override public IVector meanOfCols() {
        final IMatrix tThis = thisMatrix_();
        
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            rVector.set_(col, tThis.col(col).mean());
        }
        return rVector;
    }
    @Override public IVector meanOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final int tRowNum = tThis.rowNumber();
        IVector rVector = tThis.newZerosVec(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            rVector.set_(row, tThis.row(row).mean());
        }
        return rVector;
    }
    
    @Override public IMatrix transpose() {
        IMatrix tMatrix = refTranspose();
        IMatrix rMatrix = thisMatrix_().newZeros(tMatrix.rowNumber(), tMatrix.columnNumber());
        rMatrix.fill(rMatrix);
        return rMatrix;
    }
    @Override public IMatrix refTranspose() {
        return new RefMatrix() {
            private final IMatrix mThis = thisMatrix_();
            @Override public double get_(int aRow, int aCol) {return mThis.get_(aCol, aRow);}
            @Override public void set_(int aRow, int aCol, double aValue)  {mThis.set_(aCol, aRow, aValue);}
            @Override public double getAndSet_(int aRow, int aCol, double aValue) {return mThis.getAndSet_(aCol, aRow, aValue);}
            @Override public int rowNumber() {return mThis.columnNumber();}
            @Override public int columnNumber() {return mThis.rowNumber();}
        };
    }
    
    @Override public boolean isDiag() {
        final IMatrix tThis = thisMatrix_();
        
        final IDoubleIterator it = tThis.iteratorCol();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            double tValue = it.next();
            if (col != row) if (tValue != 0.0) return false;
        }
        return true;
    }
    
    
    /** stuff to override */
    protected abstract IMatrix thisMatrix_();
    protected abstract IMatrix newMatrix_(IMatrix.ISize aSize);
}
