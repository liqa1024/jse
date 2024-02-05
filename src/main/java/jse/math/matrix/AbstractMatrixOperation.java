package jse.math.matrix;

import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetOnlyIterator;
import jse.math.operation.DATA;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;

import java.util.function.*;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation implements IMatrixOperation {
    /** 通用的一些运算 */
    @Override public IMatrix plus       (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebePlus2Dest    (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix minus      (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lminus     (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix multiply   (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebeMultiply2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix div        (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix ldiv       (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix mod        (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebeMod2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lmod       (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); DATA.ebeMod2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix operate    (IMatrix aRHS, DoubleBinaryOperator aOpt) {IMatrix rMatrix = newMatrix_(); DATA.ebeDo2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
    @Override public IMatrix plus       (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapPlus2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix minus      (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapMinus2Dest    (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lminus     (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapLMinus2Dest   (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix multiply   (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapMultiply2Dest (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix div        (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapDiv2Dest      (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix ldiv       (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapLDiv2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix mod        (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapMod2Dest      (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lmod       (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapLMod2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix map        (DoubleUnaryOperator aOpt) {IMatrix rMatrix = newMatrix_(); DATA.mapDo2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
    @Override public void plus2this     (IMatrix aRHS) {DATA.ebePlus2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void minus2this    (IMatrix aRHS) {DATA.ebeMinus2This   (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lminus2this   (IMatrix aRHS) {DATA.ebeLMinus2This  (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void multiply2this (IMatrix aRHS) {DATA.ebeMultiply2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void div2this      (IMatrix aRHS) {DATA.ebeDiv2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void ldiv2this     (IMatrix aRHS) {DATA.ebeLDiv2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void mod2this      (IMatrix aRHS) {DATA.ebeMod2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lmod2this     (IMatrix aRHS) {DATA.ebeLMod2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void operate2this  (IMatrix aRHS, DoubleBinaryOperator aOpt) {DATA.ebeDo2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol, aOpt);}
    
    @Override public void plus2this     (double aRHS) {DATA.mapPlus2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void minus2this    (double aRHS) {DATA.mapMinus2This   (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lminus2this   (double aRHS) {DATA.mapLMinus2This  (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void multiply2this (double aRHS) {DATA.mapMultiply2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void div2this      (double aRHS) {DATA.mapDiv2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void ldiv2this     (double aRHS) {DATA.mapLDiv2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void mod2this      (double aRHS) {DATA.mapMod2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lmod2this     (double aRHS) {DATA.mapLMod2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void map2this      (DoubleUnaryOperator aOpt) {DATA.mapDo2This(thisMatrix_()::setIteratorCol, aOpt);}
    
    @Override public IMatrix negative() {IMatrix rMatrix = newMatrix_(); DATA.mapNegative2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public void negative2this() {DATA.mapNegative2This(thisMatrix_()::setIteratorCol);}
    
    @Override public void fill          (double aRHS) {DATA.mapFill2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void fill          (IMatrix aRHS) {DATA.ebeFill2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void assignCol     (DoubleSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorCol, aSup);}
    @Override public void assignRow     (DoubleSupplier aSup) {DATA.assign2This(thisMatrix_()::setIteratorRow, aSup);}
    @Override public void forEachCol    (DoubleConsumer aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorCol, aCon);}
    @Override public void forEachRow    (DoubleConsumer aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorRow, aCon);}
    @Override public void fill          (IMatrixGetter aRHS) {
        final IMatrix tThis = thisMatrix_();
        final IDoubleSetOnlyIterator si = tThis.setIteratorCol();
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
        IVector rVector = newVector_(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            rVector.set(col, tThis.col(col).sum());
        }
        return rVector;
    }
    @Override public IVector sumOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final int tRowNum = tThis.rowNumber();
        IVector rVector = newVector_(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            rVector.set(row, tThis.row(row).sum());
        }
        return rVector;
    }
    
    @Override public IVector meanOfCols() {
        final IMatrix tThis = thisMatrix_();
        
        final int tColNum = tThis.columnNumber();
        IVector rVector = newVector_(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            rVector.set(col, tThis.col(col).mean());
        }
        return rVector;
    }
    @Override public IVector meanOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final int tRowNum = tThis.rowNumber();
        IVector rVector = newVector_(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            rVector.set(row, tThis.row(row).mean());
        }
        return rVector;
    }
    
    @Override public IMatrix transpose() {
        final IMatrix tThis = thisMatrix_();
        IMatrix rMatrix = newMatrix_(tThis.columnNumber(), tThis.rowNumber());
        final IDoubleIterator it = tThis.iteratorCol();
        final IDoubleSetOnlyIterator si = rMatrix.setIteratorRow();
        while (it.hasNext()) si.nextAndSet(it.next());
        return rMatrix;
    }
    @Override public IMatrix refTranspose() {
        return new RefMatrix() {
            private final IMatrix mThis = thisMatrix_();
            @Override public double get(int aRow, int aCol) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); return mThis.get(aCol, aRow);}
            @Override public void set(int aRow, int aCol, double aValue)  {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); mThis.set(aCol, aRow, aValue);}
            @Override public double getAndSet(int aRow, int aCol, double aValue) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); return mThis.getAndSet(aCol, aRow, aValue);}
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
            if (col!=row && tValue!=0.0) return false;
        }
        return true;
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private IMatrix newMatrix_() {
        final IMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.rowNumber(), tThis.columnNumber());
    }
    
    /** stuff to override */
    protected abstract IMatrix thisMatrix_();
    protected abstract IMatrix newMatrix_(int aRowNum, int aColNum);
    protected IVector newVector_(int aSize) {return Vectors.zeros(aSize);}
}
