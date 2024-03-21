package jse.math.matrix;

import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetOnlyIterator;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jse.parallel.ParforThreadPool;

import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

import static jse.code.Conf.OPERATION_CHECK;
import static jse.code.Conf.PARFOR_THREAD_NUMBER;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation implements IMatrixOperation {
    /** 通用的一些运算 */
    @Override public IMatrix plus       (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebePlus2Dest    (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix minus      (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lminus     (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeMinus2Dest   (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix multiply   (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeMultiply2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix div        (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix ldiv       (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeDiv2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix mod        (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeMod2Dest     (thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lmod       (IMatrix aRHS) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeMod2Dest     (aRHS::iteratorCol, thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix operate    (IMatrix aRHS, DoubleBinaryOperator aOpt) {ebeCheck(thisMatrix_().rowNumber(), thisMatrix_().columnNumber(), aRHS.rowNumber(), aRHS.columnNumber()); IMatrix rMatrix = newMatrix_(); DATA.ebeDo2Dest(thisMatrix_()::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
    @Override public IMatrix plus       (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapPlus2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix minus      (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapMinus2Dest    (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lminus     (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapLMinus2Dest   (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix multiply   (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapMultiply2Dest (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix div        (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapDiv2Dest      (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix ldiv       (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapLDiv2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix mod        (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapMod2Dest      (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix lmod       (double aRHS) {IMatrix rMatrix = newMatrix_(); DATA.mapLMod2Dest     (thisMatrix_()::iteratorCol, aRHS, rMatrix::setIteratorCol); return rMatrix;}
    @Override public IMatrix map        (DoubleUnaryOperator aOpt) {IMatrix rMatrix = newMatrix_(); DATA.mapDo2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol, aOpt); return rMatrix;}
    
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
    
    @Override public IMatrix negative() {IMatrix rMatrix = newMatrix_(); DATA.mapNegative2Dest(thisMatrix_()::iteratorCol, rMatrix::setIteratorCol); return rMatrix;}
    @Override public void negative2this() {DATA.mapNegative2This(thisMatrix_()::setIteratorCol);}
    
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
//    @Override public void lmatmul2this(IMatrix aRHS) {lmatmul2This_(thisMatrix_(), aRHS);}
    @Override public void  matmul2dest(IMatrix aRHS, IMatrix rDest) {matmul2Dest_(thisMatrix_(), aRHS, rDest);}
    @Override public void lmatmul2dest(IMatrix aRHS, IMatrix rDest) {matmul2Dest_(aRHS, thisMatrix_(), rDest);}
    
    @Override public IMatrix  matmul_par(IMatrix aRHS) {return  matmul_par(aRHS, PARFOR_THREAD_NUMBER);}
    @Override public IMatrix lmatmul_par(IMatrix aRHS) {return lmatmul_par(aRHS, PARFOR_THREAD_NUMBER);}
    @Override public IMatrix  matmul_par(IMatrix aRHS, int aTreadNum) {try (ParforThreadPool tPool = new ParforThreadPool(aTreadNum)) {return  matmul_par(aRHS, tPool);}}
    @Override public IMatrix lmatmul_par(IMatrix aRHS, int aTreadNum) {try (ParforThreadPool tPool = new ParforThreadPool(aTreadNum)) {return lmatmul_par(aRHS, tPool);}}
    @Override public void  matmul2dest_par(IMatrix aRHS, IMatrix rDest) { matmul2dest_par(aRHS, rDest, PARFOR_THREAD_NUMBER);}
    @Override public void lmatmul2dest_par(IMatrix aRHS, IMatrix rDest) {lmatmul2dest_par(aRHS, rDest, PARFOR_THREAD_NUMBER);}
    @Override public void  matmul2dest_par(IMatrix aRHS, IMatrix rDest, int aTreadNum) {try (ParforThreadPool tPool = new ParforThreadPool(aTreadNum)) { matmul2dest_par(aRHS, rDest, tPool);}}
    @Override public void lmatmul2dest_par(IMatrix aRHS, IMatrix rDest, int aTreadNum) {try (ParforThreadPool tPool = new ParforThreadPool(aTreadNum)) {lmatmul2dest_par(aRHS, rDest, tPool);}}
    @Override public IMatrix  matmul_par(IMatrix aRHS, ParforThreadPool aPool) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.rowNumber(), aRHS.columnNumber()); matmul2Dest_par_(newMatrixIsZeros(), tThis, aRHS, rMatrix, aPool); return rMatrix;}
    @Override public IMatrix lmatmul_par(IMatrix aRHS, ParforThreadPool aPool) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(aRHS.rowNumber(), tThis.columnNumber()); matmul2Dest_par_(newMatrixIsZeros(), aRHS, tThis, rMatrix, aPool); return rMatrix;}
    @Override public void  matmul2dest_par(IMatrix aRHS, IMatrix rDest, ParforThreadPool aPool) {matmul2Dest_par_(thisMatrix_(), aRHS, rDest, aPool);}
    @Override public void lmatmul2dest_par(IMatrix aRHS, IMatrix rDest, ParforThreadPool aPool) {matmul2Dest_par_(aRHS, thisMatrix_(), rDest, aPool);}
    
    
    private final static int BLOCK_SIZE = 128; // 现在是并行分块的大小，大一些可以排除并行的损耗
    /**
     * 计算矩阵乘法实现；
     * 现在对于串行版本不进行分块，因为大部分情况效率更低；
     * 不使用复杂度更低的神奇算法，因为实现麻烦且会降低精度
     */
    private static void matmul2This_(IMatrix rThis, IMatrix aRHS) {
        // 由于逻辑存在些许不同，这里简单起见重新实现，不去考虑重复代码的问题
        // 先判断大小是否合适
        matmulCheck(rThis.rowNumber(), rThis.columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rThis.rowNumber(), rThis.columnNumber());
        // 获取必要数据（mid == col）
        final int tRowNum = rThis.rowNumber();
        final int tColNum = rThis.columnNumber();
        // 特殊情况，这里是不去处理
        if (tColNum == 0) return;
        // 这里简单处理，强制列优先遍历，让逻辑一致
        ColumnMatrix tRHS = aRHS.toBufCol();
        double[] rData = tRHS.internalData();
        try {
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
                    rThis.set(row, 3, tRow0*rData[12] + tRow1*rData[13] + tRow2*rData[14] + tRow3*rData[16]);
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
                try {
                    for (int row = 0; row < tRowNum; ++row) {
                        tRow.fill(rThis.row(row));
                        for (int col = 0, rs = 0; col < tColNum; ++col, rs+=tColNum) {
                            rThis.set(row, col, ARRAY.dot(rowData, 0, rData, rs, tColNum));
                        }
                    }
                } finally {
                    VectorCache.returnVec(tRow);
                }
                break;
            }}
        } finally {
            aRHS.releaseBuf(tRHS, true);
        }
    }
    private static void matmul2Dest_(IMatrix aLHS, IMatrix aRHS, IMatrix rDest) {
        // 先判断大小是否合适
        matmulCheck(aLHS.rowNumber(), aLHS.columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber());
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
        // 但是在中间很短的情况下，应该翻转这个操作，从而更好利用上 SIMD 加速
        if (tMidNum <= 4) tRowFirst = !tRowFirst;
        // 根据上述判断决定遍历顺序
        if (tRowFirst) {
            // 先遍历行，因此左边需要是行矩阵
            RowMatrix tLHS = aLHS.toBufRow();
            double[] lData = tLHS.internalData();
            try {
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
                    try {
                        for (int col = 0; col < tColNum; ++col) {
                            tCol.fill(aRHS.col(col));
                            for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=tMidNum) {
                                rDest.set(row, col, ARRAY.dot(lData, ls, colData, 0, tMidNum));
                            }
                        }
                    } finally {
                        VectorCache.returnVec(tCol);
                    }
                    break;
                }}
            } finally {
                aLHS.releaseBuf(tLHS, true);
            }
        } else {
            // 先遍历列，因此右边需要是列矩阵
            ColumnMatrix tRHS = aRHS.toBufCol();
            double[] rData = tRHS.internalData();
            try {
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
                    try {
                        for (int row = 0; row < tRowNum; ++row) {
                            tRow.fill(aLHS.row(row));
                            for (int col = 0, rs = 0; col < tColNum; ++col, rs+=tMidNum) {
                                rDest.set(row, col, ARRAY.dot(rowData, 0, rData, rs, tMidNum));
                            }
                        }
                    } finally {
                        VectorCache.returnVec(tRow);
                    }
                    break;
                }}
            } finally {
                aRHS.releaseBuf(tRHS, true);
            }
        }
    }
    private static void matmul2Dest_par_(IMatrix aLHS, IMatrix aRHS, IMatrix rDest, ParforThreadPool aPool) {matmul2Dest_par_(false, aLHS, aRHS, rDest, aPool);}
    private static void matmul2Dest_par_(boolean aDestIsZeros, IMatrix aLHS, IMatrix aRHS, IMatrix rDest, ParforThreadPool aPool) {
        // 先判断大小是否合适
        matmulCheck(aLHS.rowNumber(), aLHS.columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber());
        // 获取必要数据
        int tRowNum = aLHS.rowNumber();
        int tColNum = aRHS.columnNumber();
        int tMidNum = aLHS.columnNumber();
        // 过小情况不进行并行
        if (tRowNum<BLOCK_SIZE+BLOCK_SIZE && tColNum<BLOCK_SIZE+BLOCK_SIZE && tMidNum<BLOCK_SIZE+BLOCK_SIZE) {
            matmul2Dest_(aLHS, aRHS, rDest);
            return;
        }
        // 如果 Dest 不为零则需要手动设为 0
        if (!aDestIsZeros) rDest.fill(0.0);
        // 获取分块数目和剩余数目（需要考虑非整除的情况）
        final int blockRowNum = tRowNum / BLOCK_SIZE;
        final int blockColNum = tColNum / BLOCK_SIZE;
        final int blockMidNum = tMidNum / BLOCK_SIZE;
        final int restRowNum = tRowNum % BLOCK_SIZE;
        final int restColNum = tColNum % BLOCK_SIZE;
        final int restMidNum = tMidNum % BLOCK_SIZE;
        // 获取缓存块矩阵
        final List<RowMatrix>    lBlockPar = MatrixCache.getMatRow(BLOCK_SIZE, BLOCK_SIZE, aPool.threadNumber());
        final List<ColumnMatrix> rBlockPar = MatrixCache.getMatCol(BLOCK_SIZE, BLOCK_SIZE, aPool.threadNumber());
        try {
            // 先遍历 block
            aPool.parfor((blockRowNum+1) * (blockColNum+1), (k, threadID) -> {
                // 获取此线程的 block
                RowMatrix    lBlock = lBlockPar.get(threadID);
                ColumnMatrix rBlock = rBlockPar.get(threadID);
                // 获取 rowB 和 colB
                int rowB = k / (blockColNum+1);
                int colB = k % (blockColNum+1);
                // 开始一般的计算
                final int rowS = rowB*BLOCK_SIZE, colS = colB*BLOCK_SIZE;
                final int blockSizeRow = rowB==blockRowNum ? restRowNum : BLOCK_SIZE;
                final int blockSizeCol = colB==blockColNum ? restColNum : BLOCK_SIZE;
                // 需要遍历行列的 block 将结果累加
                for (int midB = 0; midB <= blockMidNum; ++midB) {
                    final int minS = midB*BLOCK_SIZE;
                    final int blockSizeMid = midB==blockMidNum ? restMidNum : BLOCK_SIZE;
                    // 手动拷贝数据到 block 中，这里直接随机访问（现在会占用整体时间了，不过也没有很好的办法）
                    for (int i = 0; i < blockSizeRow; ++i) for (int j = 0; j < blockSizeMid; ++j) {
                        lBlock.set(i, j, aLHS.get(rowS+i, minS+j));
                    }
                    for (int j = 0; j < blockSizeCol; ++j) for (int i = 0; i < blockSizeMid; ++i) {
                        rBlock.set(i, j, aRHS.get(minS+i, colS+j));
                    }
                    // 计算块矩阵的乘法并累加
                    addBlockMatmul2Dest_(blockSizeMid, lBlock.internalData(), blockSizeRow, rBlock.internalData(), blockSizeCol, rDest, rowS, colS);
                }
            });
        } finally {
            MatrixCache.returnMat(rBlockPar);
            MatrixCache.returnMat(lBlockPar);
        }
    }
    private static void addBlockMatmul2Dest_(int aBlockSizeMid, double[] aLHS, int aBlockSizeRow, double[] aRHS, int aBlockSizeCol, IMatrix rDest, int aRowStart, int aColStart) {
        if (aBlockSizeMid == BLOCK_SIZE) {
            for (int row = 0, ls = 0; row < aBlockSizeRow; ++row, ls+=BLOCK_SIZE) for (int col = 0, rs = 0; col < aBlockSizeCol; ++col, rs+=BLOCK_SIZE) {
                double rSum = 0.0;
                // 定长循环更快，因此这里手动实现一下这个点乘
                for (int i = 0; i < BLOCK_SIZE; i+=8) {
                    // 这样分两批计算更快，可以在支持 avx512 的机器上再测试
                    rSum += (
                          aLHS[ls+i  ]*aRHS[rs+i  ]
                        + aLHS[ls+i+1]*aRHS[rs+i+1]
                        + aLHS[ls+i+2]*aRHS[rs+i+2]
                        + aLHS[ls+i+3]*aRHS[rs+i+3]
                    );
                    // 不使用两个 sum 尽量保证逻辑一致性
                    rSum += (
                          aLHS[ls+i+4]*aRHS[rs+i+4]
                        + aLHS[ls+i+5]*aRHS[rs+i+5]
                        + aLHS[ls+i+6]*aRHS[rs+i+6]
                        + aLHS[ls+i+7]*aRHS[rs+i+7]
                    );
                }
                final double fSum = rSum;
                rDest.update(aRowStart+row, aColStart+col, v -> v+fSum);
            }
        } else {
            for (int row = 0, ls = 0; row < aBlockSizeRow; ++row, ls+=BLOCK_SIZE) for (int col = 0, rs = 0; col < aBlockSizeCol; ++col, rs+=BLOCK_SIZE) {
                final double tDot = ARRAY.dot(aLHS, ls, aRHS, rs, aBlockSizeMid);
                rDest.update(aRowStart+row, aColStart+col, v -> v+tDot);
            }
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
    static void matmulCheck(int lRowNum, int lColNum, int rRowNum, int rColNum) {
        if (!OPERATION_CHECK) return;
        if (lColNum != rRowNum) throw new IllegalArgumentException(
            "The dimension used for matrix multiplication is incorrect: ("+lRowNum+" x "+lColNum+") vs ("+rRowNum+" x "+rColNum+").\n" +
            "Please ensure that the ncols in the first matrix ("+lColNum+") matches the nrows in the second matrix ("+rRowNum+")"
        );
    }
    static void matmulCheck(int lRowNum, int lColNum, int rRowNum, int rColNum, int dRowNum, int dColNum) {
        if (!OPERATION_CHECK) return;
        if (lColNum != rRowNum) throw new IllegalArgumentException(
            "The dimension used for matrix multiplication is incorrect: ("+lRowNum+" x "+lColNum+") vs ("+rRowNum+" x "+rColNum+").\n" +
            "Please ensure that the ncols in the first matrix ("+lColNum+") matches the nrows in the second matrix ("+rRowNum+")"
        );
        if (lRowNum!=dRowNum || rColNum!=dColNum) throw new IllegalArgumentException(
            "The dimensions of input and output matrix are not match: ("+lRowNum+" x "+rColNum+") vs ("+dRowNum+" x "+dColNum+")"
        );
    }
    
    /** stuff to override */
    protected abstract IMatrix thisMatrix_();
    protected abstract IMatrix newMatrix_(int aRowNum, int aColNum);
    protected IVector newVector_(int aSize) {return Vectors.zeros(aSize);}
    protected boolean newMatrixIsZeros() {return true;}
}
