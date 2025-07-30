package jse.math.matrix;

import jse.cache.VectorCache;
import jse.code.Conf;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetOnlyIterator;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

import static jse.code.Conf.OPERATION_CHECK;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation implements IMatrixOperation {
    /** 通用的一些运算 */
    @Override public final IMatrix plus       (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); plus2dest    (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix minus      (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); minus2dest   (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix lminus     (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); lminus2dest  (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix multiply   (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); multiply2dest(aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix div        (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); div2dest     (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix ldiv       (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); ldiv2dest    (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix mod        (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); mod2dest     (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix lmod       (IMatrix aRHS) {IMatrix rMatrix = newMatrix_(); lmod2dest    (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix operate    (IMatrix aRHS, DoubleBinaryOperator aOpt) {IMatrix rMatrix = newMatrix_(); operate2dest(aRHS, rMatrix, aOpt); return rMatrix;}
    
    @Override public final IMatrix plus       (double aRHS) {IMatrix rMatrix = newMatrix_(); plus2dest    (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix minus      (double aRHS) {IMatrix rMatrix = newMatrix_(); minus2dest   (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix lminus     (double aRHS) {IMatrix rMatrix = newMatrix_(); lminus2dest  (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix multiply   (double aRHS) {IMatrix rMatrix = newMatrix_(); multiply2dest(aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix div        (double aRHS) {IMatrix rMatrix = newMatrix_(); div2dest     (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix ldiv       (double aRHS) {IMatrix rMatrix = newMatrix_(); ldiv2dest    (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix mod        (double aRHS) {IMatrix rMatrix = newMatrix_(); mod2dest     (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix lmod       (double aRHS) {IMatrix rMatrix = newMatrix_(); lmod2dest    (aRHS, rMatrix); return rMatrix;}
    @Override public final IMatrix map        (DoubleUnaryOperator aOpt) {IMatrix rMatrix = newMatrix_(); map2dest(rMatrix, aOpt); return rMatrix;}
    
    @Override public void plus2this     (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebePlus2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void minus2this    (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeMinus2This   (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lminus2this   (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeLMinus2This  (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void multiply2this (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeMultiply2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void div2this      (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeDiv2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void ldiv2this     (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeLDiv2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void mod2this      (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeMod2This     (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void lmod2this     (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeLMod2This    (thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    @Override public void operate2this  (IMatrix aRHS, DoubleBinaryOperator aOpt) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeDo2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol, aOpt);}
    
    @Override public void plus2this     (double aRHS) {DATA.mapPlus2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void minus2this    (double aRHS) {DATA.mapMinus2This   (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lminus2this   (double aRHS) {DATA.mapLMinus2This  (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void multiply2this (double aRHS) {DATA.mapMultiply2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void div2this      (double aRHS) {DATA.mapDiv2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void ldiv2this     (double aRHS) {DATA.mapLDiv2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void mod2this      (double aRHS) {DATA.mapMod2This     (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void lmod2this     (double aRHS) {DATA.mapLMod2This    (thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void map2this      (DoubleUnaryOperator aOpt) {DATA.mapDo2This(thisMatrix_()::setIteratorCol, aOpt);}
    
    @Override public IMatrix abs() {IMatrix rMatrix = newMatrix_(); DATA.mapAbs2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public void abs2this() {DATA.mapAbs2This(thisMatrix_()::setIteratorCol);}
    @Override public IMatrix negative() {IMatrix rMatrix = newMatrix_(); DATA.mapNegative2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public void negative2this() {DATA.mapNegative2This(thisMatrix_()::setIteratorCol);}
    
    /** 补充的一些运算 */
    @Override public void plus2dest     (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebePlus2Dest    (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);}
    @Override public void minus2dest    (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeMinus2Dest   (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);}
    @Override public void lminus2dest   (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeMinus2Dest   (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rDest::setIteratorCol);}
    @Override public void multiply2dest (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeMultiply2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);}
    @Override public void div2dest      (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeDiv2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);}
    @Override public void ldiv2dest     (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeDiv2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rDest::setIteratorCol);}
    @Override public void mod2dest      (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeMod2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);}
    @Override public void lmod2dest     (IMatrix aRHS, IMatrix rDest) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeMod2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rDest::setIteratorCol);}
    @Override public void operate2dest  (IMatrix aRHS, IMatrix rDest, DoubleBinaryOperator aOpt) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.ebeDo2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol, aOpt);}
    
    @Override public void plus2dest     (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapPlus2Dest     (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void minus2dest    (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapMinus2Dest    (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void lminus2dest   (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapLMinus2Dest   (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void multiply2dest (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapMultiply2Dest (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void div2dest      (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapDiv2Dest      (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void ldiv2dest     (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapLDiv2Dest     (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void mod2dest      (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapMod2Dest      (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void lmod2dest     (double aRHS, IMatrix rDest) {mapCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), rDest.rowNumber(), rDest.columnNumber()); DATA.mapLMod2Dest     (thisMatrix_()::iteratorCol, aRHS, rDest::setIteratorCol);}
    @Override public void map2dest      (IMatrix rDest, DoubleUnaryOperator aOpt) {DATA.mapDo2Dest(thisMatrix_()::iteratorCol, rDest::setIteratorCol, aOpt);}
    
    @Override public void fill          (double aRHS) {DATA.mapFill2This(thisMatrix_()::setIteratorCol, aRHS);}
    @Override public void fill          (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); DATA.ebeFill2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
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
    
    
    @Override public IMatrix  matmul(IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.rowNumber(), aRHS.columnNumber()); matmul2Dest_(tThis, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix lmatmul(IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(aRHS.rowNumber(), tThis.columnNumber()); matmul2Dest_(aRHS, tThis, rMatrix); return rMatrix;}
    @Override public void  matmul2this(IMatrix aRHS) {matmul2This_(thisMatrix_(), aRHS);}
    @Override public void lmatmul2this(IMatrix aRHS) {lmatmul2This_(thisMatrix_(), aRHS);}
    @Override public void  matmul2dest(IMatrix aRHS, IMatrix rDest) {matmul2Dest_(thisMatrix_(), aRHS, rDest);}
    @Override public void lmatmul2dest(IMatrix aRHS, IMatrix rDest) {matmul2Dest_(aRHS, thisMatrix_(), rDest);}
    
    
    /**
     * 计算矩阵乘法实现；
     * 现在对于串行版本不进行分块，因为大部分情况效率更低；
     * 不使用复杂度更低的神奇算法，因为实现麻烦且会降低精度
     */
    private static void matmul2This_(IMatrix rThis, IMatrix aRHS) {
        // 由于逻辑存在些许不同，这里简单起见重新实现，不去考虑重复代码的问题
        // 先判断大小是否合适
        matmul2thisCheck(rThis.rowNumber(), rThis.columnNumber(), aRHS.rowNumber(), aRHS.columnNumber());
        // 获取必要数据（mid == col）
        final int tRowNum = rThis.rowNumber();
        final int tColNum = rThis.columnNumber();
        // 特殊情况，这里是不去处理
        if (tColNum == 0) return;
        // 尝试使用 native 接口
        if (Conf.NATIVE_OPERATION) {
            Vector tRow = VectorCache.getVec(tColNum);
            RowMatrix tThis = rThis.toBufRow();
            ColumnMatrix tRHS = aRHS.toBufCol();
            ARRAY.Native.matmulRC2This(tThis.internalData(), 0, tRHS.internalData(), 0, tRow.internalData(), tRowNum, tColNum);
            aRHS.releaseBuf(tRHS, true);
            rThis.releaseBuf(tThis);
            VectorCache.returnVec(tRow);
            return;
        }
        // 这里简单处理，强制列优先遍历，让逻辑一致
        ColumnMatrix tRHS = aRHS.toBufCol();
        double[] rData = tRHS.internalData();
        // 注意 mid == col，可以直接展开
        switch(tColNum) {
        case 1: {
            rThis.multiply2this(rData[0]);
            break;
        }
        case 2: {
            for (int row = 0; row < tRowNum; ++row) {
                double tRow0 = rThis.get(row, 0);
                double tRow1 = rThis.get(row, 1);
                rThis.set(row, 0, tRow0*rData[0] + tRow1*rData[1]);
                rThis.set(row, 1, tRow0*rData[2] + tRow1*rData[3]);
            }
            break;
        }
        case 3: {
            for (int row = 0; row < tRowNum; ++row) {
                double tRow0 = rThis.get(row, 0);
                double tRow1 = rThis.get(row, 1);
                double tRow2 = rThis.get(row, 2);
                rThis.set(row, 0, tRow0*rData[0] + tRow1*rData[1] + tRow2*rData[2]);
                rThis.set(row, 1, tRow0*rData[3] + tRow1*rData[4] + tRow2*rData[5]);
                rThis.set(row, 2, tRow0*rData[6] + tRow1*rData[7] + tRow2*rData[8]);
            }
            break;
        }
        case 4: {
            for (int row = 0; row < tRowNum; ++row) {
                double tRow0 = rThis.get(row, 0);
                double tRow1 = rThis.get(row, 1);
                double tRow2 = rThis.get(row, 2);
                double tRow3 = rThis.get(row, 3);
                rThis.set(row, 0, tRow0*rData[ 0] + tRow1*rData[ 1] + tRow2*rData[ 2] + tRow3*rData[ 3]);
                rThis.set(row, 1, tRow0*rData[ 4] + tRow1*rData[ 5] + tRow2*rData[ 6] + tRow3*rData[ 7]);
                rThis.set(row, 2, tRow0*rData[ 8] + tRow1*rData[ 9] + tRow2*rData[10] + tRow3*rData[11]);
                rThis.set(row, 3, tRow0*rData[12] + tRow1*rData[13] + tRow2*rData[14] + tRow3*rData[15]);
            }
            break;
        }
        case 5: {
            for (int row = 0; row < tRowNum; ++row) {
                double tRow0 = rThis.get(row, 0);
                double tRow1 = rThis.get(row, 1);
                double tRow2 = rThis.get(row, 2);
                double tRow3 = rThis.get(row, 3);
                double tRow4 = rThis.get(row, 4);
                rThis.set(row, 0, tRow0*rData[ 0] + tRow1*rData[ 1] + tRow2*rData[ 2] + tRow3*rData[ 3] + tRow4*rData[ 4]);
                rThis.set(row, 1, tRow0*rData[ 5] + tRow1*rData[ 6] + tRow2*rData[ 7] + tRow3*rData[ 8] + tRow4*rData[ 9]);
                rThis.set(row, 2, tRow0*rData[10] + tRow1*rData[11] + tRow2*rData[12] + tRow3*rData[13] + tRow4*rData[14]);
                rThis.set(row, 3, tRow0*rData[15] + tRow1*rData[16] + tRow2*rData[17] + tRow3*rData[18] + tRow4*rData[19]);
                rThis.set(row, 4, tRow0*rData[20] + tRow1*rData[21] + tRow2*rData[22] + tRow3*rData[23] + tRow4*rData[24]);
            }
            break;
        }
        case 6: {
            for (int row = 0; row < tRowNum; ++row) {
                double tRow0 = rThis.get(row, 0);
                double tRow1 = rThis.get(row, 1);
                double tRow2 = rThis.get(row, 2);
                double tRow3 = rThis.get(row, 3);
                double tRow4 = rThis.get(row, 4);
                double tRow5 = rThis.get(row, 5);
                rThis.set(row, 0, tRow0*rData[ 0] + tRow1*rData[ 1] + tRow2*rData[ 2] + tRow3*rData[ 3] + tRow4*rData[ 4] + tRow5*rData[ 5]);
                rThis.set(row, 1, tRow0*rData[ 6] + tRow1*rData[ 7] + tRow2*rData[ 8] + tRow3*rData[ 9] + tRow4*rData[10] + tRow5*rData[11]);
                rThis.set(row, 2, tRow0*rData[12] + tRow1*rData[13] + tRow2*rData[14] + tRow3*rData[15] + tRow4*rData[16] + tRow5*rData[17]);
                rThis.set(row, 3, tRow0*rData[18] + tRow1*rData[19] + tRow2*rData[20] + tRow3*rData[21] + tRow4*rData[22] + tRow5*rData[23]);
                rThis.set(row, 4, tRow0*rData[24] + tRow1*rData[25] + tRow2*rData[26] + tRow3*rData[27] + tRow4*rData[28] + tRow5*rData[29]);
                rThis.set(row, 5, tRow0*rData[30] + tRow1*rData[31] + tRow2*rData[32] + tRow3*rData[33] + tRow4*rData[34] + tRow5*rData[35]);
            }
            break;
        }
        case 7: {
            for (int row = 0; row < tRowNum; ++row) {
                double tRow0 = rThis.get(row, 0);
                double tRow1 = rThis.get(row, 1);
                double tRow2 = rThis.get(row, 2);
                double tRow3 = rThis.get(row, 3);
                double tRow4 = rThis.get(row, 4);
                double tRow5 = rThis.get(row, 5);
                double tRow6 = rThis.get(row, 6);
                rThis.set(row, 0, tRow0*rData[ 0] + tRow1*rData[ 1] + tRow2*rData[ 2] + tRow3*rData[ 3] + tRow4*rData[ 4] + tRow5*rData[ 5] + tRow6*rData[ 6]);
                rThis.set(row, 1, tRow0*rData[ 7] + tRow1*rData[ 8] + tRow2*rData[ 9] + tRow3*rData[10] + tRow4*rData[11] + tRow5*rData[12] + tRow6*rData[13]);
                rThis.set(row, 2, tRow0*rData[14] + tRow1*rData[15] + tRow2*rData[16] + tRow3*rData[17] + tRow4*rData[18] + tRow5*rData[19] + tRow6*rData[20]);
                rThis.set(row, 3, tRow0*rData[21] + tRow1*rData[22] + tRow2*rData[23] + tRow3*rData[24] + tRow4*rData[25] + tRow5*rData[26] + tRow6*rData[27]);
                rThis.set(row, 4, tRow0*rData[28] + tRow1*rData[29] + tRow2*rData[30] + tRow3*rData[31] + tRow4*rData[32] + tRow5*rData[33] + tRow6*rData[34]);
                rThis.set(row, 5, tRow0*rData[35] + tRow1*rData[36] + tRow2*rData[37] + tRow3*rData[38] + tRow4*rData[39] + tRow5*rData[40] + tRow6*rData[41]);
                rThis.set(row, 6, tRow0*rData[42] + tRow1*rData[43] + tRow2*rData[44] + tRow3*rData[45] + tRow4*rData[46] + tRow5*rData[47] + tRow6*rData[48]);
            }
            break;
        }
        case 8: {
            for (int row = 0; row < tRowNum; ++row) {
                double tRow0 = rThis.get(row, 0);
                double tRow1 = rThis.get(row, 1);
                double tRow2 = rThis.get(row, 2);
                double tRow3 = rThis.get(row, 3);
                double tRow4 = rThis.get(row, 4);
                double tRow5 = rThis.get(row, 5);
                double tRow6 = rThis.get(row, 6);
                double tRow7 = rThis.get(row, 7);
                rThis.set(row, 0, tRow0*rData[ 0] + tRow1*rData[ 1] + tRow2*rData[ 2] + tRow3*rData[ 3] + tRow4*rData[ 4] + tRow5*rData[ 5] + tRow6*rData[ 6] + tRow7*rData[ 7]);
                rThis.set(row, 1, tRow0*rData[ 8] + tRow1*rData[ 9] + tRow2*rData[10] + tRow3*rData[11] + tRow4*rData[12] + tRow5*rData[13] + tRow6*rData[14] + tRow7*rData[15]);
                rThis.set(row, 2, tRow0*rData[16] + tRow1*rData[17] + tRow2*rData[18] + tRow3*rData[19] + tRow4*rData[20] + tRow5*rData[21] + tRow6*rData[22] + tRow7*rData[23]);
                rThis.set(row, 3, tRow0*rData[24] + tRow1*rData[25] + tRow2*rData[26] + tRow3*rData[27] + tRow4*rData[28] + tRow5*rData[29] + tRow6*rData[30] + tRow7*rData[31]);
                rThis.set(row, 4, tRow0*rData[32] + tRow1*rData[33] + tRow2*rData[34] + tRow3*rData[35] + tRow4*rData[36] + tRow5*rData[37] + tRow6*rData[38] + tRow7*rData[39]);
                rThis.set(row, 5, tRow0*rData[40] + tRow1*rData[41] + tRow2*rData[42] + tRow3*rData[43] + tRow4*rData[44] + tRow5*rData[45] + tRow6*rData[46] + tRow7*rData[47]);
                rThis.set(row, 6, tRow0*rData[48] + tRow1*rData[49] + tRow2*rData[50] + tRow3*rData[51] + tRow4*rData[52] + tRow5*rData[53] + tRow6*rData[54] + tRow7*rData[55]);
                rThis.set(row, 7, tRow0*rData[56] + tRow1*rData[57] + tRow2*rData[58] + tRow3*rData[59] + tRow4*rData[60] + tRow5*rData[61] + tRow6*rData[62] + tRow7*rData[63]);
            }
            break;
        }
        default: {
            Vector tRow = VectorCache.getVec(tColNum);
            double[] rowData = tRow.internalData();
            for (int row = 0; row < tRowNum; ++row) {
                tRow.fill(rThis.row(row));
                for (int col = 0, rs = 0; col < tColNum; ++col, rs+=tColNum) {
                    rThis.set(row, col, ARRAY.dot(rowData, 0, rData, rs, tColNum));
                }
            }
            VectorCache.returnVec(tRow);
            break;
        }}
        aRHS.releaseBuf(tRHS, true);
    }
    private static void lmatmul2This_(IMatrix rThis, IMatrix aRHS) {
        // 由于逻辑存在些许不同，这里简单起见重新实现，不去考虑重复代码的问题
        // 先判断大小是否合适
        lmatmul2thisCheck(rThis.rowNumber(), rThis.columnNumber(), aRHS.rowNumber(), aRHS.columnNumber());
        // 获取必要数据（mid == row）
        final int tRowNum = rThis.rowNumber();
        final int tColNum = rThis.columnNumber();
        // 特殊情况，这里是不去处理
        if (tRowNum == 0) return;
        // 尝试使用 native 接口
        if (Conf.NATIVE_OPERATION) {
            ColumnMatrix tThis = rThis.toBufCol();
            RowMatrix tRHS = aRHS.toBufRow();
            Vector tCol = VectorCache.getVec(tRowNum);
            ARRAY.Native.lmatmulCR2This(tThis.internalData(), 0, tRHS.internalData(), 0, tCol.internalData(), tRowNum, tColNum);
            VectorCache.returnVec(tCol);
            aRHS.releaseBuf(tRHS, true);
            rThis.releaseBuf(tThis);
            return;
        }
        // 这里简单处理，强制行优先遍历，让逻辑一致
        RowMatrix tRHS = aRHS.toBufRow();
        double[] rData = tRHS.internalData();
        // 注意 mid == row，可以直接展开
        switch(tRowNum) {
        case 1: {
            // 可以直接这样处理，因为两个 double 相乘满足交换律
            rThis.multiply2this(rData[0]);
            break;
        }
        case 2: {
            for (int col = 0; col < tColNum; ++col) {
                double tCol0 = rThis.get(0, col);
                double tCol1 = rThis.get(1, col);
                rThis.set(0, col, rData[0]*tCol0 + rData[1]*tCol1);
                rThis.set(1, col, rData[2]*tCol0 + rData[3]*tCol1);
            }
            break;
        }
        case 3: {
            for (int col = 0; col < tColNum; ++col) {
                double tCol0 = rThis.get(0, col);
                double tCol1 = rThis.get(1, col);
                double tCol2 = rThis.get(2, col);
                rThis.set(0, col, rData[0]*tCol0 + rData[1]*tCol1 + rData[2]*tCol2);
                rThis.set(1, col, rData[3]*tCol0 + rData[4]*tCol1 + rData[5]*tCol2);
                rThis.set(2, col, rData[6]*tCol0 + rData[7]*tCol1 + rData[8]*tCol2);
            }
            break;
        }
        case 4: {
            for (int col = 0; col < tColNum; ++col) {
                double tCol0 = rThis.get(0, col);
                double tCol1 = rThis.get(1, col);
                double tCol2 = rThis.get(2, col);
                double tCol3 = rThis.get(3, col);
                rThis.set(0, col, rData[ 0]*tCol0 + rData[ 1]*tCol1 + rData[ 2]*tCol2 + rData[ 3]*tCol3);
                rThis.set(1, col, rData[ 4]*tCol0 + rData[ 5]*tCol1 + rData[ 6]*tCol2 + rData[ 7]*tCol3);
                rThis.set(2, col, rData[ 8]*tCol0 + rData[ 9]*tCol1 + rData[10]*tCol2 + rData[11]*tCol3);
                rThis.set(3, col, rData[12]*tCol0 + rData[13]*tCol1 + rData[14]*tCol2 + rData[15]*tCol3);
            }
            break;
        }
        case 5: {
            for (int col = 0; col < tColNum; ++col) {
                double tCol0 = rThis.get(0, col);
                double tCol1 = rThis.get(1, col);
                double tCol2 = rThis.get(2, col);
                double tCol3 = rThis.get(3, col);
                double tCol4 = rThis.get(4, col);
                rThis.set(0, col, rData[ 0]*tCol0 + rData[ 1]*tCol1 + rData[ 2]*tCol2 + rData[ 3]*tCol3 + rData[ 4]*tCol4);
                rThis.set(1, col, rData[ 5]*tCol0 + rData[ 6]*tCol1 + rData[ 7]*tCol2 + rData[ 8]*tCol3 + rData[ 9]*tCol4);
                rThis.set(2, col, rData[10]*tCol0 + rData[11]*tCol1 + rData[12]*tCol2 + rData[13]*tCol3 + rData[14]*tCol4);
                rThis.set(3, col, rData[15]*tCol0 + rData[16]*tCol1 + rData[17]*tCol2 + rData[18]*tCol3 + rData[19]*tCol4);
                rThis.set(4, col, rData[20]*tCol0 + rData[21]*tCol1 + rData[22]*tCol2 + rData[23]*tCol3 + rData[24]*tCol4);
            }
            break;
        }
        case 6: {
            for (int col = 0; col < tColNum; ++col) {
                double tCol0 = rThis.get(0, col);
                double tCol1 = rThis.get(1, col);
                double tCol2 = rThis.get(2, col);
                double tCol3 = rThis.get(3, col);
                double tCol4 = rThis.get(4, col);
                double tCol5 = rThis.get(5, col);
                rThis.set(0, col, rData[ 0]*tCol0 + rData[ 1]*tCol1 + rData[ 2]*tCol2 + rData[ 3]*tCol3 + rData[ 4]*tCol4 + rData[ 5]*tCol5);
                rThis.set(1, col, rData[ 6]*tCol0 + rData[ 7]*tCol1 + rData[ 8]*tCol2 + rData[ 9]*tCol3 + rData[10]*tCol4 + rData[11]*tCol5);
                rThis.set(2, col, rData[12]*tCol0 + rData[13]*tCol1 + rData[14]*tCol2 + rData[15]*tCol3 + rData[16]*tCol4 + rData[17]*tCol5);
                rThis.set(3, col, rData[18]*tCol0 + rData[19]*tCol1 + rData[20]*tCol2 + rData[21]*tCol3 + rData[22]*tCol4 + rData[23]*tCol5);
                rThis.set(4, col, rData[24]*tCol0 + rData[25]*tCol1 + rData[26]*tCol2 + rData[27]*tCol3 + rData[28]*tCol4 + rData[29]*tCol5);
                rThis.set(5, col, rData[30]*tCol0 + rData[31]*tCol1 + rData[32]*tCol2 + rData[33]*tCol3 + rData[34]*tCol4 + rData[35]*tCol5);
            }
            break;
        }
        case 7: {
            for (int col = 0; col < tColNum; ++col) {
                double tCol0 = rThis.get(0, col);
                double tCol1 = rThis.get(1, col);
                double tCol2 = rThis.get(2, col);
                double tCol3 = rThis.get(3, col);
                double tCol4 = rThis.get(4, col);
                double tCol5 = rThis.get(5, col);
                double tCol6 = rThis.get(6, col);
                rThis.set(0, col, rData[ 0]*tCol0 + rData[ 1]*tCol1 + rData[ 2]*tCol2 + rData[ 3]*tCol3 + rData[ 4]*tCol4 + rData[ 5]*tCol5 + rData[ 6]*tCol6);
                rThis.set(1, col, rData[ 7]*tCol0 + rData[ 8]*tCol1 + rData[ 9]*tCol2 + rData[10]*tCol3 + rData[11]*tCol4 + rData[12]*tCol5 + rData[13]*tCol6);
                rThis.set(2, col, rData[14]*tCol0 + rData[15]*tCol1 + rData[16]*tCol2 + rData[17]*tCol3 + rData[18]*tCol4 + rData[19]*tCol5 + rData[20]*tCol6);
                rThis.set(3, col, rData[21]*tCol0 + rData[22]*tCol1 + rData[23]*tCol2 + rData[24]*tCol3 + rData[25]*tCol4 + rData[26]*tCol5 + rData[27]*tCol6);
                rThis.set(4, col, rData[28]*tCol0 + rData[29]*tCol1 + rData[30]*tCol2 + rData[31]*tCol3 + rData[32]*tCol4 + rData[33]*tCol5 + rData[34]*tCol6);
                rThis.set(5, col, rData[35]*tCol0 + rData[36]*tCol1 + rData[37]*tCol2 + rData[38]*tCol3 + rData[39]*tCol4 + rData[40]*tCol5 + rData[41]*tCol6);
                rThis.set(6, col, rData[42]*tCol0 + rData[43]*tCol1 + rData[44]*tCol2 + rData[45]*tCol3 + rData[46]*tCol4 + rData[47]*tCol5 + rData[48]*tCol6);
            }
            break;
        }
        case 8: {
            for (int col = 0; col < tColNum; ++col) {
                double tCol0 = rThis.get(0, col);
                double tCol1 = rThis.get(1, col);
                double tCol2 = rThis.get(2, col);
                double tCol3 = rThis.get(3, col);
                double tCol4 = rThis.get(4, col);
                double tCol5 = rThis.get(5, col);
                double tCol6 = rThis.get(6, col);
                double tCol7 = rThis.get(7, col);
                rThis.set(0, col, rData[ 0]*tCol0 + rData[ 1]*tCol1 + rData[ 2]*tCol2 + rData[ 3]*tCol3 + rData[ 4]*tCol4 + rData[ 5]*tCol5 + rData[ 6]*tCol6 + rData[ 7]*tCol7);
                rThis.set(1, col, rData[ 8]*tCol0 + rData[ 9]*tCol1 + rData[10]*tCol2 + rData[11]*tCol3 + rData[12]*tCol4 + rData[13]*tCol5 + rData[14]*tCol6 + rData[15]*tCol7);
                rThis.set(2, col, rData[16]*tCol0 + rData[17]*tCol1 + rData[18]*tCol2 + rData[19]*tCol3 + rData[20]*tCol4 + rData[21]*tCol5 + rData[22]*tCol6 + rData[23]*tCol7);
                rThis.set(3, col, rData[24]*tCol0 + rData[25]*tCol1 + rData[26]*tCol2 + rData[27]*tCol3 + rData[28]*tCol4 + rData[29]*tCol5 + rData[30]*tCol6 + rData[31]*tCol7);
                rThis.set(4, col, rData[32]*tCol0 + rData[33]*tCol1 + rData[34]*tCol2 + rData[35]*tCol3 + rData[36]*tCol4 + rData[37]*tCol5 + rData[38]*tCol6 + rData[39]*tCol7);
                rThis.set(5, col, rData[40]*tCol0 + rData[41]*tCol1 + rData[42]*tCol2 + rData[43]*tCol3 + rData[44]*tCol4 + rData[45]*tCol5 + rData[46]*tCol6 + rData[47]*tCol7);
                rThis.set(6, col, rData[48]*tCol0 + rData[49]*tCol1 + rData[50]*tCol2 + rData[51]*tCol3 + rData[52]*tCol4 + rData[53]*tCol5 + rData[54]*tCol6 + rData[55]*tCol7);
                rThis.set(7, col, rData[56]*tCol0 + rData[57]*tCol1 + rData[58]*tCol2 + rData[59]*tCol3 + rData[60]*tCol4 + rData[61]*tCol5 + rData[62]*tCol6 + rData[63]*tCol7);
            }
            break;
        }
        default: {
            Vector tCol = VectorCache.getVec(tRowNum);
            double[] colData = tCol.internalData();
            for (int col = 0; col < tColNum; ++col) {
                tCol.fill(rThis.col(col));
                for (int row = 0, rs = 0; row < tRowNum; ++row, rs+=tRowNum) {
                    rThis.set(row, col, ARRAY.dot(rData, rs, colData, 0, tRowNum));
                }
            }
            VectorCache.returnVec(tCol);
            break;
        }}
        aRHS.releaseBuf(tRHS, true);
    }
    private static void matmul2Dest_(IMatrix aLHS, IMatrix aRHS, IMatrix rDest) {
        // 先判断大小是否合适
        matmul2destCheck(aLHS.rowNumber(), aLHS.columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber());
        // 获取必要数据
        final int tRowNum = aLHS.rowNumber();
        final int tColNum = aRHS.columnNumber();
        final int tMidNum = aLHS.columnNumber();
        // 特殊情况处理
        if (tMidNum == 0) return;
        // 现在对于串行的版本默认都不进行分块，更加简洁且很多情况下都更快
        // 判断行列顺序优先，这个问题没有那么简单
        // 一般情况下，行和列更短的一方对应矩阵更小，应该优先全部遍历（内存友好）
        boolean tRowFirst = tColNum > tRowNum;
        // 尝试使用 native 接口
        if (Conf.NATIVE_OPERATION) {
            RowMatrix tLHS = aLHS.toBufRow();
            ColumnMatrix tRHS = aRHS.toBufCol();
            if (tRowFirst) {
                ColumnMatrix rBuf = rDest.toBufCol(true);
                ARRAY.Native.matmulRCC2Dest(tLHS.internalData(), 0, tRHS.internalData(), 0,
                                            rBuf.internalData(), 0, tRowNum, tColNum, tMidNum);
                rDest.releaseBuf(rBuf);
            } else {
                RowMatrix rBuf = rDest.toBufRow(true);
                ARRAY.Native.matmulRCR2Dest(tLHS.internalData(), 0, tRHS.internalData(), 0,
                                            rBuf.internalData(), 0, tRowNum, tColNum, tMidNum);
                rDest.releaseBuf(rBuf);
            }
            aRHS.releaseBuf(tRHS, true);
            aLHS.releaseBuf(tLHS, true);
            return;
        }
        // 在 jse 实现中，中间很短的情况下应该翻转这个操作，从而更好利用上 SIMD 加速
        if (tMidNum <= 4) tRowFirst = !tRowFirst;
        // 根据上述判断决定遍历顺序
        if (tRowFirst) {
            // 先遍历行，因此左边需要是行矩阵
            RowMatrix tLHS = aLHS.toBufRow();
            double[] lData = tLHS.internalData();
            switch(tMidNum) {
            case 1: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    for (int row = 0; row < tRowNum; ++row) {
                        rDest.set(row, col, lData[row]*tCol0);
                    }
                }
                break;
            }
            case 2: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    double tCol1 = aRHS.get(1, col);
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=2) {
                        rDest.set(row, col, lData[ls]*tCol0 + lData[ls+1]*tCol1);
                    }
                }
                break;
            }
            case 3: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    double tCol1 = aRHS.get(1, col);
                    double tCol2 = aRHS.get(2, col);
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=3) {
                        rDest.set(row, col, lData[ls]*tCol0 + lData[ls+1]*tCol1 + lData[ls+2]*tCol2);
                    }
                }
                break;
            }
            case 4: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    double tCol1 = aRHS.get(1, col);
                    double tCol2 = aRHS.get(2, col);
                    double tCol3 = aRHS.get(3, col);
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=4) {
                        rDest.set(row, col, lData[ls]*tCol0 + lData[ls+1]*tCol1 + lData[ls+2]*tCol2 + lData[ls+3]*tCol3);
                    }
                }
                break;
            }
            case 5: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    double tCol1 = aRHS.get(1, col);
                    double tCol2 = aRHS.get(2, col);
                    double tCol3 = aRHS.get(3, col);
                    double tCol4 = aRHS.get(4, col);
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=5) {
                        rDest.set(row, col, lData[ls]*tCol0 + lData[ls+1]*tCol1 + lData[ls+2]*tCol2 + lData[ls+3]*tCol3 + lData[ls+4]*tCol4);
                    }
                }
                break;
            }
            case 6: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    double tCol1 = aRHS.get(1, col);
                    double tCol2 = aRHS.get(2, col);
                    double tCol3 = aRHS.get(3, col);
                    double tCol4 = aRHS.get(4, col);
                    double tCol5 = aRHS.get(5, col);
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=6) {
                        rDest.set(row, col, lData[ls]*tCol0 + lData[ls+1]*tCol1 + lData[ls+2]*tCol2 + lData[ls+3]*tCol3 + lData[ls+4]*tCol4 + lData[ls+5]*tCol5);
                    }
                }
                break;
            }
            case 7: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    double tCol1 = aRHS.get(1, col);
                    double tCol2 = aRHS.get(2, col);
                    double tCol3 = aRHS.get(3, col);
                    double tCol4 = aRHS.get(4, col);
                    double tCol5 = aRHS.get(5, col);
                    double tCol6 = aRHS.get(6, col);
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=7) {
                        rDest.set(row, col, lData[ls]*tCol0 + lData[ls+1]*tCol1 + lData[ls+2]*tCol2 + lData[ls+3]*tCol3 + lData[ls+4]*tCol4 + lData[ls+5]*tCol5 + lData[ls+6]*tCol6);
                    }
                }
                break;
            }
            case 8: {
                for (int col = 0; col < tColNum; ++col) {
                    double tCol0 = aRHS.get(0, col);
                    double tCol1 = aRHS.get(1, col);
                    double tCol2 = aRHS.get(2, col);
                    double tCol3 = aRHS.get(3, col);
                    double tCol4 = aRHS.get(4, col);
                    double tCol5 = aRHS.get(5, col);
                    double tCol6 = aRHS.get(6, col);
                    double tCol7 = aRHS.get(7, col);
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=8) {
                        rDest.set(row, col, lData[ls]*tCol0 + lData[ls+1]*tCol1 + lData[ls+2]*tCol2 + lData[ls+3]*tCol3 + lData[ls+4]*tCol4 + lData[ls+5]*tCol5 + lData[ls+6]*tCol6 + lData[ls+7]*tCol7);
                    }
                }
                break;
            }
            default: {
                Vector tCol = VectorCache.getVec(tMidNum);
                double[] colData = tCol.internalData();
                for (int col = 0; col < tColNum; ++col) {
                    tCol.fill(aRHS.col(col));
                    for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=tMidNum) {
                        rDest.set(row, col, ARRAY.dot(lData, ls, colData, 0, tMidNum));
                    }
                }
                VectorCache.returnVec(tCol);
                break;
            }}
            aLHS.releaseBuf(tLHS, true);
        } else {
            // 先遍历列，因此右边需要是列矩阵
            ColumnMatrix tRHS = aRHS.toBufCol();
            double[] rData = tRHS.internalData();
            switch(tMidNum) {
            case 1: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    for (int col = 0; col < tColNum; ++col) {
                        rDest.set(row, col, tRow0*rData[col]);
                    }
                }
                break;
            }
            case 2: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    double tRow1 = aLHS.get(row, 1);
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=2) {
                        rDest.set(row, col, tRow0*rData[rs] + tRow1*rData[rs+1]);
                    }
                }
                break;
            }
            case 3: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    double tRow1 = aLHS.get(row, 1);
                    double tRow2 = aLHS.get(row, 2);
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=3) {
                        rDest.set(row, col, tRow0*rData[rs] + tRow1*rData[rs+1] + tRow2*rData[rs+2]);
                    }
                }
                break;
            }
            case 4: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    double tRow1 = aLHS.get(row, 1);
                    double tRow2 = aLHS.get(row, 2);
                    double tRow3 = aLHS.get(row, 3);
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=4) {
                        rDest.set(row, col, tRow0*rData[rs] + tRow1*rData[rs+1] + tRow2*rData[rs+2] + tRow3*rData[rs+3]);
                    }
                }
                break;
            }
            case 5: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    double tRow1 = aLHS.get(row, 1);
                    double tRow2 = aLHS.get(row, 2);
                    double tRow3 = aLHS.get(row, 3);
                    double tRow4 = aLHS.get(row, 4);
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=5) {
                        rDest.set(row, col, tRow0*rData[rs] + tRow1*rData[rs+1] + tRow2*rData[rs+2] + tRow3*rData[rs+3] + tRow4*rData[rs+4]);
                    }
                }
                break;
            }
            case 6: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    double tRow1 = aLHS.get(row, 1);
                    double tRow2 = aLHS.get(row, 2);
                    double tRow3 = aLHS.get(row, 3);
                    double tRow4 = aLHS.get(row, 4);
                    double tRow5 = aLHS.get(row, 5);
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=6) {
                        rDest.set(row, col, tRow0*rData[rs] + tRow1*rData[rs+1] + tRow2*rData[rs+2] + tRow3*rData[rs+3] + tRow4*rData[rs+4] + tRow5*rData[rs+5]);
                    }
                }
                break;
            }
            case 7: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    double tRow1 = aLHS.get(row, 1);
                    double tRow2 = aLHS.get(row, 2);
                    double tRow3 = aLHS.get(row, 3);
                    double tRow4 = aLHS.get(row, 4);
                    double tRow5 = aLHS.get(row, 5);
                    double tRow6 = aLHS.get(row, 6);
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=7) {
                        rDest.set(row, col, tRow0*rData[rs] + tRow1*rData[rs+1] + tRow2*rData[rs+2] + tRow3*rData[rs+3] + tRow4*rData[rs+4] + tRow5*rData[rs+5] + tRow6*rData[rs+6]);
                    }
                }
                break;
            }
            case 8: {
                for (int row = 0; row < tRowNum; ++row) {
                    double tRow0 = aLHS.get(row, 0);
                    double tRow1 = aLHS.get(row, 1);
                    double tRow2 = aLHS.get(row, 2);
                    double tRow3 = aLHS.get(row, 3);
                    double tRow4 = aLHS.get(row, 4);
                    double tRow5 = aLHS.get(row, 5);
                    double tRow6 = aLHS.get(row, 6);
                    double tRow7 = aLHS.get(row, 7);
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=8) {
                        rDest.set(row, col, tRow0*rData[rs] + tRow1*rData[rs+1] + tRow2*rData[rs+2] + tRow3*rData[rs+3] + tRow4*rData[rs+4] + tRow5*rData[rs+5] + tRow6*rData[rs+6] + tRow7*rData[rs+7]);
                    }
                }
                break;
            }
            default: {
                Vector tRow = VectorCache.getVec(tMidNum);
                double[] rowData = tRow.internalData();
                for (int row = 0; row < tRowNum; ++row) {
                    tRow.fill(aLHS.row(row));
                    for (int col = 0, rs = 0; col < tColNum; ++col, rs+=tMidNum) {
                        rDest.set(row, col, ARRAY.dot(rowData, 0, rData, rs, tMidNum));
                    }
                }
                VectorCache.returnVec(tRow);
                break;
            }}
            aRHS.releaseBuf(tRHS, true);
        }
    }
    
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
    static void ebeCheck(int lRowNum, int lColNum, int rRowNum, int rColNum) {
        if (!OPERATION_CHECK) return;
        if (lRowNum!=rRowNum || lColNum!=rColNum) throw new IllegalArgumentException(
            "The dimensions of two matrices are not match: ("+lRowNum+" x "+lColNum+") vs ("+rRowNum+" x "+rColNum+")"
        );
    }
    static void ebeCheck(int lRowNum, int lColNum, int rRowNum, int rColNum, int dRowNum, int dColNum) {
        if (!OPERATION_CHECK) return;
        ebeCheck(lRowNum, lColNum, rRowNum, rColNum);
        if (lRowNum!=dRowNum || lColNum!=dColNum) throw new IllegalArgumentException(
            "The dimensions of input and output matrices are not match: ("+lRowNum+" x "+lColNum+") vs ("+dRowNum+" x "+dColNum+")"
        );
    }
    static void mapCheck(int lRowNum, int lColNum, int dRowNum, int dColNum) {
        if (!OPERATION_CHECK) return;
        if (lRowNum!=dRowNum || lColNum!=dColNum) throw new IllegalArgumentException(
            "The dimensions of input and output matrices are not match: ("+lRowNum+" x "+lColNum+") vs ("+dRowNum+" x "+dColNum+")"
        );
    }
    static void matmulCheck(int lRowNum, int lColNum, int rRowNum, int rColNum) {
        if (!OPERATION_CHECK) return;
        if (lColNum != rRowNum) throw new IllegalArgumentException(
            "The dimension used for matrix multiplication is incorrect: ("+lRowNum+" x "+lColNum+") vs ("+rRowNum+" x "+rColNum+").\n" +
            "Please ensure that the ncols in the first matrix ("+lColNum+") matches the nrows in the second matrix ("+rRowNum+")"
        );
    }
    static void matmul2thisCheck(int lRowNum, int lColNum, int rRowNum, int rColNum) {
        if (!OPERATION_CHECK) return;
        matmulCheck(lRowNum, lColNum, rRowNum, rColNum);
        if (rRowNum != rColNum) throw new IllegalArgumentException(
            "Input matrix for `matmul2this` MUST be square: ("+rRowNum+" x "+rColNum+")"
        );
    }
    static void lmatmul2thisCheck(int lRowNum, int lColNum, int rRowNum, int rColNum) {
        if (!OPERATION_CHECK) return;
        matmulCheck(rRowNum, rColNum, lRowNum, lColNum);
        if (rRowNum != rColNum) throw new IllegalArgumentException(
            "Input matrix for `lmatmul2this` MUST be square: ("+rRowNum+" x "+rColNum+")"
        );
    }
    static void matmul2destCheck(int lRowNum, int lColNum, int rRowNum, int rColNum, int dRowNum, int dColNum) {
        if (!OPERATION_CHECK) return;
        matmulCheck(lRowNum, lColNum, rRowNum, rColNum);
        if (lRowNum!=dRowNum || rColNum!=dColNum) throw new IllegalArgumentException(
            "The dimensions of input and output matrix are not match: ("+lRowNum+" x "+rColNum+") vs ("+dRowNum+" x "+dColNum+")"
        );
    }
    
    /** stuff to override */
    protected abstract IMatrix thisMatrix_();
    protected abstract IMatrix newMatrix_(int aRowNum, int aColNum);
    protected IVector newVector_(int aSize) {return Vectors.zeros(aSize);}
}
