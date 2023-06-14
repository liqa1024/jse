package com.jtool.math.matrix;

import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;
import com.jtool.math.operation.DATA;
import com.jtool.math.vector.IVector;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation implements IMatrixOperation {
    /** 通用的一些运算 */
    @Override public IMatrix ebePlus        (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebePlus2Dest_      (rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix ebeMinus       (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeMinus2Dest_     (rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix ebeMultiply    (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeMultiply2Dest_  (rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix ebeDiv         (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeDiv2Dest_       (rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix ebeMod         (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeMod2Dest_       (rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix ebeDo          (IMatrixGetter aLHS, IMatrixGetter aRHS, IDoubleOperator2 aOpt) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeDo2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator(), aOpt); return rMatrix;}
    
    @Override public IMatrix mapPlus        (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapPlus2Dest_       (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapMinus       (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapMinus2Dest_      (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapLMinus      (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapLMinus2Dest_     (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapMultiply    (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapMultiply2Dest_   (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapDiv         (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapDiv2Dest_        (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapLDiv        (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapLDiv2Dest_       (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapMod         (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapMod2Dest_        (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapLMod        (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapLMod2Dest_       (rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator()); return rMatrix;}
    @Override public IMatrix mapDo          (IMatrixGetter aLHS, IDoubleOperator1 aOpt) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapDo2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colSetIterator(), aOpt); return rMatrix;}
    
    @Override public void ebePlus2this      (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebePlus2this_       (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeMinus2this     (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeMinus2this_      (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeLMinus2this    (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeLMinus2this_     (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeMultiply2this  (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeMultiply2this_   (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeDiv2this       (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeDiv2this_        (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeLDiv2this      (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeLDiv2this_       (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeMod2this       (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeMod2this_        (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeLMod2this      (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeLMod2this_       (rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    @Override public void ebeDo2this        (IMatrixGetter aRHS, IDoubleOperator2 aOpt) {IMatrix rMatrix = thisMatrix_(); DATA.ebeDo2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS), aOpt);}
    
    @Override public void mapPlus2this      (double aRHS) {DATA.mapPlus2this_       (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapMinus2this     (double aRHS) {DATA.mapMinus2this_      (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapLMinus2this    (double aRHS) {DATA.mapLMinus2this_     (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapMultiply2this  (double aRHS) {DATA.mapMultiply2this_   (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapDiv2this       (double aRHS) {DATA.mapDiv2this_        (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapLDiv2this      (double aRHS) {DATA.mapLDiv2this_       (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapMod2this       (double aRHS) {DATA.mapMod2this_        (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapLMod2this      (double aRHS) {DATA.mapLMod2this_       (thisMatrix_().colSetIterator(), aRHS);}
    @Override public void mapDo2this        (IDoubleOperator1 aOpt) {DATA.mapDo2this_(thisMatrix_().colSetIterator(), aOpt);}
    
    @Override public void mapFill2this      (double aRHS) {DATA.mapFill2this_(thisMatrix_().colSetIterator(), aRHS);}
    @Override public void ebeFill2this      (IMatrixGetter aRHS) {IMatrix rMatrix = thisMatrix_(); DATA.ebeFill2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));}
    
    @Override public double sum () {return DATA.sumOfThis_ (thisMatrix_().colSetIterator());}
    @Override public double mean() {return DATA.meanOfThis_(thisMatrix_().colSetIterator());}
    @Override public double max () {return DATA.maxOfThis_ (thisMatrix_().colSetIterator());}
    @Override public double min () {return DATA.minOfThis_ (thisMatrix_().colSetIterator());}
    
    
    /** 这里直接调用对应向量的运算，现在可以利用上所有的优化 */
    @Override public IVector sumOfCols() {
        final IMatrix tThis = thisMatrix_();
        
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            rVector.set_(col, tThis.col(col).operation().sum());
        }
        return rVector;
    }
    @Override public IVector sumOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final int tRowNum = tThis.rowNumber();
        IVector rVector = tThis.newZerosVec(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            rVector.set_(row, tThis.row(row).operation().sum());
        }
        return rVector;
    }
    
    @Override public IVector meanOfCols() {
        final IMatrix tThis = thisMatrix_();
        
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            rVector.set_(col, tThis.col(col).operation().mean());
        }
        return rVector;
    }
    @Override public IVector meanOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final int tRowNum = tThis.rowNumber();
        IVector rVector = tThis.newZerosVec(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            rVector.set_(row, tThis.row(row).operation().mean());
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
        
        final IDoubleIterator it = tThis.colIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            double tValue = it.next();
            if (col != row) if (tValue != 0.0) return false;
        }
        return true;
    }
    
    
    /** 内部实用函数 */
    protected IMatrix.ISize newMatrixSize_(IMatrixGetter aData) {
        if (aData instanceof IMatrix) return ((IMatrix)aData).size();
        return thisMatrix_().size();
    }
    protected IMatrix.ISize newMatrixSize_(IMatrixGetter aData1, IMatrixGetter aData2) {
        if (aData1 instanceof IMatrix) return ((IMatrix)aData1).size();
        if (aData2 instanceof IMatrix) return ((IMatrix)aData2).size();
        return thisMatrix_().size();
    }
    
    /** stuff to override */
    protected abstract IMatrix thisMatrix_();
    protected abstract IMatrix newMatrix_(IMatrix.ISize aSize);
}
