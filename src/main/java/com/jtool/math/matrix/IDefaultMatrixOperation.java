package com.jtool.math.matrix;


import com.jtool.math.vector.IVectorAny;
import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;

/**
 * 矩阵一些额外运算的默认实现，原则上应该使用抽象类来实现，
 * 但是由于多继承的问题，为了避免重复代码，依旧放在接口中
 * @author liqa
 */
public interface IDefaultMatrixOperation<M extends IMatrixAny<?, ?>, MS extends IMatrixAny<M, V>, V extends IVectorAny<?>> extends IMatrixOperation<M, V> {
    /** 这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    default V sumOfCols() {
        final MS tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.colIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        V rVector = tThis.generatorVec().zeros(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            double rSum = 0.0;
            for (int row = 0; row < tRowNum; ++row) rSum += it.next();
            rVector.set_(col, rSum);
        }
        return rVector;
    }
    default V sumOfRows() {
        final MS tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.rowIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        V rVector = tThis.generatorVec().zeros(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            double rSum = 0.0;
            for (int col = 0; col < tColNum; ++col) rSum += it.next();
            rVector.set_(row, rSum);
        }
        return rVector;
    }
    
    default V meanOfCols() {
        final MS tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.colIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        V rVector = tThis.generatorVec().zeros(tColNum);
        for (int col = 0; col < tColNum; ++col) {
            double rSum = 0.0;
            for (int row = 0; row < tRowNum; ++row) rSum += it.next();
            rVector.set_(col, rSum/(double)tRowNum);
        }
        return rVector;
    }
    default V meanOfRows() {
        final MS tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.rowIterator();
        final int tRowNum = tThis.rowNumber();
        final int tColNum = tThis.columnNumber();
        V rVector = tThis.generatorVec().zeros(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            double rSum = 0.0;
            for (int col = 0; col < tColNum; ++col) rSum += it.next();
            rVector.set_(row, rSum/(double)tColNum);
        }
        return rVector;
    }
    
    default M transpose() {return thisMatrix_().generator().from(refTranspose());}
    default IMatrixAny<?, ?> refTranspose() {
        return new AbstractMatrix() {
            private final MS mThis = thisMatrix_();
            @Override public double get_(int aRow, int aCol) {return mThis.get_(aCol, aRow);}
            @Override public void set_(int aRow, int aCol, double aValue)  {mThis.set_(aCol, aRow, aValue);}
            @Override public double getAndSet_(int aRow, int aCol, double aValue) {return mThis.getAndSet_(aCol, aRow, aValue);}
            @Override public int rowNumber() {return mThis.columnNumber();}
            @Override public int columnNumber() {return mThis.rowNumber();}
        };
    }
    
    /** stuff to override */
    @ApiStatus.Internal MS thisMatrix_();
}
