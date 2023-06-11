package com.jtool.math.matrix;

import com.jtool.code.operator.IOperator1;
import com.jtool.code.operator.IOperator2;
import com.jtool.math.MathEX;
import com.jtool.math.operation.DATA;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.RefVector;

import java.util.Iterator;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation implements IMatrixOperation {
    /** 通用的一些运算 */
    @Override public IMatrix ebePlus        (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebePlus2Dest_      (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix ebeMinus       (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeMinus2Dest_     (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix ebeMultiply    (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeMultiply2Dest_  (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix ebeDiv         (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeDiv2Dest_       (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix ebeMod         (IMatrixGetter aLHS, IMatrixGetter aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeMod2Dest_       (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix ebeDo          (IMatrixGetter aLHS, IMatrixGetter aRHS, IOperator2<Double> aOpt) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS)); DATA.ebeDo2Dest_(aLHS, aRHS, rMatrix, aOpt); return rMatrix;}
    
    @Override public IMatrix mapPlus        (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapPlus2Dest_       (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapMinus       (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapMinus2Dest_      (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapLMinus      (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapLMinus2Dest_     (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapMultiply    (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapMultiply2Dest_   (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapDiv         (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapDiv2Dest_        (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapLDiv        (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapLDiv2Dest_       (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapMod         (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapMod2Dest_        (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapLMod        (IMatrixGetter aLHS, double aRHS) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapLMod2Dest_       (aLHS, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix mapDo          (IMatrixGetter aLHS, IOperator1<Double> aOpt) {IMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS)); DATA.mapDo2Dest_(aLHS, rMatrix, aOpt); return rMatrix;}
    
    @Override public void ebePlus2this      (IMatrixGetter aRHS) {DATA.ebePlus2this_    (thisMatrix_(), aRHS);}
    @Override public void ebeMinus2this     (IMatrixGetter aRHS) {DATA.ebeMinus2this_   (thisMatrix_(), aRHS);}
    @Override public void ebeLMinus2this    (IMatrixGetter aRHS) {DATA.ebeLMinus2this_  (thisMatrix_(), aRHS);}
    @Override public void ebeMultiply2this  (IMatrixGetter aRHS) {DATA.ebeMultiply2this_(thisMatrix_(), aRHS);}
    @Override public void ebeDiv2this       (IMatrixGetter aRHS) {DATA.ebeDiv2this_     (thisMatrix_(), aRHS);}
    @Override public void ebeLDiv2this      (IMatrixGetter aRHS) {DATA.ebeLDiv2this_    (thisMatrix_(), aRHS);}
    @Override public void ebeMod2this       (IMatrixGetter aRHS) {DATA.ebeMod2this_     (thisMatrix_(), aRHS);}
    @Override public void ebeLMod2this      (IMatrixGetter aRHS) {DATA.ebeLMod2this_    (thisMatrix_(), aRHS);}
    @Override public void ebeDo2this        (IMatrixGetter aRHS, IOperator2<Double> aOpt) {DATA.ebeDo2this_(thisMatrix_(), aRHS, aOpt);}
    
    @Override public void mapPlus2this      (double aRHS) {DATA.mapPlus2this_       (thisMatrix_(), aRHS);}
    @Override public void mapMinus2this     (double aRHS) {DATA.mapMinus2this_      (thisMatrix_(), aRHS);}
    @Override public void mapLMinus2this    (double aRHS) {DATA.mapLMinus2this_     (thisMatrix_(), aRHS);}
    @Override public void mapMultiply2this  (double aRHS) {DATA.mapMultiply2this_   (thisMatrix_(), aRHS);}
    @Override public void mapDiv2this       (double aRHS) {DATA.mapDiv2this_        (thisMatrix_(), aRHS);}
    @Override public void mapLDiv2this      (double aRHS) {DATA.mapLDiv2this_       (thisMatrix_(), aRHS);}
    @Override public void mapMod2this       (double aRHS) {DATA.mapMod2this_        (thisMatrix_(), aRHS);}
    @Override public void mapLMod2this      (double aRHS) {DATA.mapLMod2this_       (thisMatrix_(), aRHS);}
    @Override public void mapDo2this        (IOperator1<Double> aOpt) {DATA.mapDo2this_(thisMatrix_(), aOpt);}
    
    @Override public void mapFill2this      (double aRHS) {DATA.mapFill2this_(thisMatrix_(), aRHS);}
    @Override public void ebeFill2this      (IMatrixGetter aRHS) {DATA.ebeFill2this_(thisMatrix_(), aRHS);}
    
    @Override public double sum () {return DATA.sumOfThis_ (thisMatrix_());}
    @Override public double mean() {return DATA.meanOfThis_(thisMatrix_());}
    @Override public double max () {return DATA.maxOfThis_ (thisMatrix_());}
    @Override public double min () {return DATA.minOfThis_ (thisMatrix_());}
    
    
    /** 这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public IVector sumOfCols() {
        final IMatrix tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.colIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            double rSum = 0.0;
            for (int row = 0; row < tRowNum; ++row) rSum += it.next();
            rVector.set_(col, rSum);
        }
        return rVector;
    }
    @Override public IVector sumOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.rowIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            double rSum = 0.0;
            for (int col = 0; col < tColNum; ++col) rSum += it.next();
            rVector.set_(row, rSum);
        }
        return rVector;
    }
    
    @Override public IVector meanOfCols() {
        final IMatrix tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.colIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            double rSum = 0.0;
            for (int row = 0; row < tRowNum; ++row) rSum += it.next();
            rVector.set_(col, rSum/(double)tRowNum);
        }
        return rVector;
    }
    @Override public IVector meanOfRows() {
        final IMatrix tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.rowIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        IVector rVector = tThis.newZerosVec(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            double rSum = 0.0;
            for (int col = 0; col < tColNum; ++col) rSum += it.next();
            rVector.set_(row, rSum/(double)tColNum);
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
        
        final Iterator<Double> it = tThis.colIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            double tValue = it.next();
            if (col != row) if (tValue != 0.0) return false;
        }
        return true;
    }
    
    @Override public IVector diag() {
        IVector tVector = refDiag();
        IVector rVector = thisMatrix_().newZerosVec(tVector.size());
        rVector.fill(tVector);
        return rVector;
    }
    @Override public IVector refDiag() {
        return new RefVector() {
            private final IMatrix mThis = thisMatrix_();
            @Override public double get_(int aIdx) {return mThis.get_(aIdx, aIdx);}
            @Override public void set_(int aIdx, double aValue)  {mThis.set_(aIdx, aIdx, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return mThis.getAndSet_(aIdx, aIdx, aValue);}
            @Override public int size() {return Math.min(mThis.columnNumber(), mThis.rowNumber());}
        };
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
