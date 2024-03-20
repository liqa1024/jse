package jse.math.matrix;

import jse.cache.MatrixCache;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetOnlyIterator;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

import static jse.code.Conf.MATMUL_BLOCK;
import static jse.code.Conf.OPERATION_CHECK;

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
    
    
    @Override public IMatrix matmul (IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(tThis.rowNumber(), aRHS.columnNumber()); addMatmul2dest_(tThis, aRHS, rMatrix); return rMatrix;}
    @Override public IMatrix lmatmul(IMatrix aRHS) {IMatrix tThis = thisMatrix_(); IMatrix rMatrix = newMatrix_(aRHS.rowNumber(), tThis.columnNumber()); addMatmul2dest_(aRHS, tThis, rMatrix); return rMatrix;}
    @Override public void matmul2dest (IMatrix aRHS, IMatrix rDest) {rDest.fill(0.0); addMatmul2dest_(thisMatrix_(), aRHS, rDest);}
    @Override public void lmatmul2dest(IMatrix aRHS, IMatrix rDest) {rDest.fill(0.0); addMatmul2dest_(aRHS, thisMatrix_(), rDest);}
    
    private final static int BLOCK_SIZE = 64; // 64 x 64 = 4096，这个值应该是最快的
    private final static int BLOCK_SIZE_MIN = 8; // 长宽以及中间层需要都超过此值才会进行 block（简单处理，没有效率最优，除了内存应当不怎么影响）
    /**
     * 计算矩阵乘法实现，这里使用分块缓存的方法来进行优化，
     * 不使用复杂度更低的神奇算法，因为实现麻烦且会降低精度
     */
    private static void addMatmul2dest_(IMatrix aLHS, IMatrix aRHS, IMatrix rDest) {
        // 先判断大小是否合适
        matmulCheck(aLHS.rowNumber(), aLHS.columnNumber(), aRHS.rowNumber(), aRHS.columnNumber(), rDest.rowNumber(), rDest.columnNumber());
        // 获取必要数据
        int tRowNum = aLHS.rowNumber();
        int tColNum = aRHS.columnNumber();
        int tMidNum = aLHS.columnNumber();
        // 不用分块的情况
        if (!MATMUL_BLOCK || tRowNum<BLOCK_SIZE_MIN || tColNum<BLOCK_SIZE_MIN || tMidNum<BLOCK_SIZE_MIN || (tRowNum<BLOCK_SIZE+BLOCK_SIZE && tColNum<BLOCK_SIZE+BLOCK_SIZE && tMidNum<BLOCK_SIZE+BLOCK_SIZE)) {
            // 还是会先转为行列的形式，这样永远都最快
            RowMatrix    tLHS = aLHS.toBufRow();
            ColumnMatrix tRHS = aRHS.toBufCol();
            try {
                for (int row = 0, ls = 0; row < tRowNum; ++row, ls+=tMidNum) for (int col = 0, rs = 0; col < tColNum; ++col, rs+=tMidNum) {
                    final double tDot = ARRAY.dot(tLHS.internalData(), ls, tRHS.internalData(), rs, tMidNum);
                    rDest.update(row, col, v -> v+tDot);
                }
            } finally {
                aLHS.releaseBuf(tLHS, true);
                aRHS.releaseBuf(tRHS, true);
            }
            return;
        }
        // 获取分块数目和剩余数目（需要考虑非整除的情况）
        final int blockRowNum = tRowNum / BLOCK_SIZE;
        final int blockColNum = tColNum / BLOCK_SIZE;
        final int blockMidNum = tMidNum / BLOCK_SIZE;
        final int restRowNum = tRowNum % BLOCK_SIZE;
        final int restColNum = tColNum % BLOCK_SIZE;
        final int restMidNum = tMidNum % BLOCK_SIZE;
        // 获取缓存块矩阵
        RowMatrix    lBlock = MatrixCache.getMatRow(BLOCK_SIZE, BLOCK_SIZE);
        ColumnMatrix rBlock = MatrixCache.getMatCol(BLOCK_SIZE, BLOCK_SIZE);
        try {
            // 先遍历 block
            for (int rowB = 0; rowB <= blockRowNum; ++rowB) for (int colB = 0; colB <= blockColNum; ++colB) {
                final int rowS = rowB*BLOCK_SIZE, colS = colB*BLOCK_SIZE;
                final int blockSizeRow = rowB==blockRowNum ? restRowNum : BLOCK_SIZE;
                final int blockSizeCol = colB==blockColNum ? restColNum : BLOCK_SIZE;
                // 需要遍历行列的 block 将结果累加
                for (int midB = 0; midB <= blockMidNum; ++midB) {
                    final int minS = midB*BLOCK_SIZE;
                    final int blockSizeMid = midB==blockMidNum ? restMidNum : BLOCK_SIZE;
                    // 手动拷贝数据到 block 中，这里直接随机访问（几乎不占用整体时间）
                    for (int i = 0; i < blockSizeRow; ++i) for (int j = 0; j < blockSizeMid; ++j) {
                        lBlock.set(i, j, aLHS.get(rowS+i, minS+j));
                    }
                    for (int j = 0; j < blockSizeCol; ++j) for (int i = 0; i < blockSizeMid; ++i) {
                        rBlock.set(i, j, aRHS.get(minS+i, colS+j));
                    }
                    // 计算块矩阵的乘法并累加
                    addBlockMatmul2Dest_(blockSizeMid, lBlock.internalData(), blockSizeRow, rBlock.internalData(), blockSizeCol, rDest, rowS, colS);
                }
            }
        } finally {
            MatrixCache.returnMat(rBlock);
            MatrixCache.returnMat(lBlock);
        }
    }
    private static void addBlockMatmul2Dest_(int aBlockSizeMid, double[] aLHS, int aBlockSizeRow, double[] aRHS, int aBlockSizeCol, IMatrix rDest, int aRowStart, int aColStart) {
        if (aBlockSizeMid == BLOCK_SIZE) {
            for (int row = 0, ls = 0; row < aBlockSizeRow; ++row, ls+=BLOCK_SIZE) for (int col = 0, rs = 0; col < aBlockSizeCol; ++col, rs+=BLOCK_SIZE) {
                double rSum0 = 0.0;
                double rSum1 = 0.0;
                double rSum2 = 0.0;
                double rSum3 = 0.0;
                // 定长循环更快，因此这里手动实现一下这个点乘
                for (int i = 0; i < BLOCK_SIZE; i+=4) {
                    rSum0 += aLHS[ls+i  ]*aRHS[rs+i  ];
                    rSum1 += aLHS[ls+i+1]*aRHS[rs+i+1];
                    rSum2 += aLHS[ls+i+2]*aRHS[rs+i+2];
                    rSum3 += aLHS[ls+i+3]*aRHS[rs+i+3];
                }
                final double fSum = rSum0+rSum1+rSum2+rSum3;
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
}
