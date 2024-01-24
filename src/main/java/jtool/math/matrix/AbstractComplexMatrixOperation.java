package jtool.math.matrix;

import groovy.lang.Closure;
import jtool.code.functional.IBinaryFullOperator;
import jtool.code.functional.IDoubleBinaryConsumer;
import jtool.code.functional.IUnaryFullOperator;
import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetOnlyIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.operation.DATA;
import jtool.math.vector.ComplexVector;
import jtool.math.vector.IComplexVector;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import static jtool.math.matrix.AbstractMatrix.rangeCheckCol;
import static jtool.math.matrix.AbstractMatrix.rangeCheckRow;

public abstract class AbstractComplexMatrixOperation implements IComplexMatrixOperation {
    /** 通用的一些运算 */
    @Override public IComplexMatrix plus        (IComplexMatrix aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebePlus2Dest    (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix minus       (IComplexMatrix aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix lminus      (IComplexMatrix aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix multiply    (IComplexMatrix aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeMultiply2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix div         (IComplexMatrix aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix ldiv        (IComplexMatrix aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix operate     (IComplexMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeDo2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    @Override public IComplexMatrix plus        (IMatrix        aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebePlus2Dest    (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix minus       (IMatrix        aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix lminus      (IMatrix        aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix multiply    (IMatrix        aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeMultiply2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix div         (IMatrix        aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix ldiv        (IMatrix        aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix operate     (IMatrix        aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {IComplexMatrix rMatrix = newMatrix_(); DATA.ebeDo2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
    @Override public IComplexMatrix plus        (IComplexDouble aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapPlus2Dest    (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix minus       (IComplexDouble aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapMinus2Dest   (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix lminus      (IComplexDouble aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapLMinus2Dest  (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix multiply    (IComplexDouble aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapMultiply2Dest(thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix div         (IComplexDouble aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapDiv2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix ldiv        (IComplexDouble aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapLDiv2Dest    (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix plus        (double         aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapPlus2Dest    (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix minus       (double         aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapMinus2Dest   (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix lminus      (double         aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapLMinus2Dest  (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix multiply    (double         aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapMultiply2Dest(thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix div         (double         aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapDiv2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix ldiv        (double         aRHS) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapLDiv2Dest    (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IComplexMatrix map         (IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {IComplexMatrix rMatrix = newMatrix_(); DATA.mapDo2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
    @Override public void plus2this     (IComplexMatrix aRHS) {DATA.ebePlus2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void minus2this    (IComplexMatrix aRHS) {DATA.ebeMinus2This   (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lminus2this   (IComplexMatrix aRHS) {DATA.ebeLMinus2This  (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void multiply2this (IComplexMatrix aRHS) {DATA.ebeMultiply2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void div2this      (IComplexMatrix aRHS) {DATA.ebeDiv2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void ldiv2this     (IComplexMatrix aRHS) {DATA.ebeLDiv2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void operate2this  (IComplexMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {DATA.ebeDo2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol, aOpt);}
    @Override public void plus2this     (IMatrix        aRHS) {DATA.ebePlus2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void minus2this    (IMatrix        aRHS) {DATA.ebeMinus2This   (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lminus2this   (IMatrix        aRHS) {DATA.ebeLMinus2This  (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void multiply2this (IMatrix        aRHS) {DATA.ebeMultiply2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void div2this      (IMatrix        aRHS) {DATA.ebeDiv2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void ldiv2this     (IMatrix        aRHS) {DATA.ebeLDiv2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void operate2this  (IMatrix        aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {DATA.ebeDo2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol, aOpt);}
    
    @Override public void plus2this     (IComplexDouble aRHS) {DATA.mapPlus2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void minus2this    (IComplexDouble aRHS) {DATA.mapMinus2This   (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lminus2this   (IComplexDouble aRHS) {DATA.mapLMinus2This  (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void multiply2this (IComplexDouble aRHS) {DATA.mapMultiply2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void div2this      (IComplexDouble aRHS) {DATA.mapDiv2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void ldiv2this     (IComplexDouble aRHS) {DATA.mapLDiv2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void plus2this     (double         aRHS) {DATA.mapPlus2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void minus2this    (double         aRHS) {DATA.mapMinus2This   (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lminus2this   (double         aRHS) {DATA.mapLMinus2This  (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void multiply2this (double         aRHS) {DATA.mapMultiply2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void div2this      (double         aRHS) {DATA.mapDiv2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void ldiv2this     (double         aRHS) {DATA.mapLDiv2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void map2this      (IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {DATA.mapDo2This(thisMatrix_()::setIteratorCol, aOpt);}
    
    @Override public IComplexMatrix negative() {IComplexMatrix rMatrix = newMatrix_(); DATA.mapNegative2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public void negative2this() {DATA.mapNegative2This(thisMatrix_()::setIteratorCol);}
    
    @Override public void fill          (IComplexDouble                     aRHS) {DATA.mapFill2This (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void fill          (double                             aRHS) {DATA.mapFill2This (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void fill          (IComplexMatrix                     aRHS) {DATA.ebeFill2This (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void fill          (IMatrix                            aRHS) {DATA.ebeFill2This (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void assignCol     (Supplier<? extends IComplexDouble> aSup) {DATA.assign2This  (thisMatrix_()::setIteratorCol, aSup);}
    @Override public void assignCol     (DoubleSupplier                     aSup) {DATA.assign2This  (thisMatrix_()::setIteratorCol, aSup);}
    @Override public void assignRow     (Supplier<? extends IComplexDouble> aSup) {DATA.assign2This  (thisMatrix_()::setIteratorRow, aSup);}
    @Override public void assignRow     (DoubleSupplier                     aSup) {DATA.assign2This  (thisMatrix_()::setIteratorRow, aSup);}
    @Override public void forEachCol    (Consumer<? super ComplexDouble>    aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorCol, aCon);}
    @Override public void forEachCol    (IDoubleBinaryConsumer              aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorCol, aCon);}
    @Override public void forEachRow    (Consumer<? super ComplexDouble>    aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorRow, aCon);}
    @Override public void forEachRow    (IDoubleBinaryConsumer              aCon) {DATA.forEachOfThis(thisMatrix_()::iteratorRow, aCon);}
    @Override public void fill          (IComplexMatrixGetter               aRHS) {
        final IComplexMatrix tThis = thisMatrix_();
        final IComplexDoubleSetOnlyIterator si = tThis.setIteratorCol();
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            si.nextAndSet(aRHS.get(row, col));
        }
    }
    @Override public void fill          (IMatrixGetter                      aRHS) {
        final IComplexMatrix tThis = thisMatrix_();
        final IComplexDoubleSetOnlyIterator si = tThis.setIteratorCol();
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            si.nextAndSet(aRHS.get(row, col));
        }
    }
    /** Groovy stuffs */
    @Override public void fill          (Closure<?> aGroovyTask) {
        final IComplexMatrix tThis = thisMatrix_();
        final IComplexDoubleSetOnlyIterator si = tThis.setIteratorCol();
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = aGroovyTask.call(row, col);
            if (tObj instanceof IComplexDouble) si.nextAndSet((IComplexDouble)tObj);
            else if (tObj instanceof Number) si.nextAndSet(((Number)tObj).doubleValue());
            else si.nextAndSet(Double.NaN);
        }
    }
    @Override public void assignCol     (Closure<?> aGroovyTask) {DATA.assign2This  (thisMatrix_()::setIteratorCol, aGroovyTask);}
    @Override public void assignRow     (Closure<?> aGroovyTask) {DATA.assign2This  (thisMatrix_()::setIteratorRow, aGroovyTask);}
    @Override public void forEachCol    (Closure<?> aGroovyTask) {DATA.forEachOfThis(thisMatrix_()::iteratorCol, aGroovyTask);}
    @Override public void forEachRow    (Closure<?> aGroovyTask) {DATA.forEachOfThis(thisMatrix_()::iteratorRow, aGroovyTask);}
    
    @Override public IComplexMatrix transpose() {
        final IComplexMatrix tThis = thisMatrix_();
        IComplexMatrix rMatrix = newMatrix_(tThis.columnNumber(), tThis.rowNumber());
        final IComplexDoubleIterator it = tThis.iteratorCol();
        final IComplexDoubleSetOnlyIterator si = rMatrix.setIteratorRow();
        while (it.hasNext()) {
            it.nextOnly();
            si.nextAndSet(it);
        }
        return rMatrix;
    }
    @Override public IComplexMatrix refTranspose() {
        return new RefComplexMatrix() {
            private final IComplexMatrix mThis = thisMatrix_();
            @Override public double getReal(int aRow, int aCol) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); return mThis.getReal(aCol, aRow);}
            @Override public double getImag(int aRow, int aCol) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); return mThis.getImag(aCol, aRow);}
            @Override public void setReal(int aRow, int aCol, double aReal) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); mThis.setReal(aCol, aRow, aReal);}
            @Override public void setImag(int aRow, int aCol, double aImag) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); mThis.setImag(aCol, aRow, aImag);}
            @Override public double getAndSetReal(int aRow, int aCol, double aReal) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); return mThis.getAndSetReal(aCol, aRow, aReal);}
            @Override public double getAndSetImag(int aRow, int aCol, double aImag) {rangeCheckRow(aRow, rowNumber()); rangeCheckCol(aCol, columnNumber()); return mThis.getAndSetImag(aCol, aRow, aImag);}
            @Override public int rowNumber() {return mThis.columnNumber();}
            @Override public int columnNumber() {return mThis.rowNumber();}
        };
    }
    
    @Override public boolean isDiag() {
        final IComplexMatrix tThis = thisMatrix_();
        
        final IComplexDoubleIterator it = tThis.iteratorCol();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            it.nextOnly();
            if (col!=row && (it.real()!=0.0 || it.imag()!=0.0)) return false;
        }
        return true;
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private IComplexMatrix newMatrix_() {
        final IComplexMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.rowNumber(), tThis.columnNumber());
    }
    
    /** stuff to override */
    protected abstract IComplexMatrix thisMatrix_();
    protected abstract IComplexMatrix newMatrix_(int aRowNum, int aColNum);
    protected IComplexVector newVector_(int aSize) {return ComplexVector.zeros(aSize);}
}
