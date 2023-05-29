package com.jtool.math.matrix;


import com.jtool.code.ISetIterator;
import com.jtool.math.vector.IVectorFull;
import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;

/**
 * 矩阵一些额外运算的默认实现，原则上应该使用抽象类来实现，
 * 但是由于多继承的问题，为了避免重复代码，依旧放在接口中
 * @author liqa
 */
public interface IDefaultMatrixOperation<M extends IMatrixFull<?, ?>, MS extends IMatrixFull<M, V>, V extends IVectorFull<?>> extends IMatrixOperation<M, V> {
    /** 这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    default V sumOfCols() {
        final MS tThis = thisMatrix_();
        
        final Iterator<Double> it = tThis.colIterator();
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
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
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
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
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
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
        final int tColNum = tThis.columnNumber();
        final int tRowNum = tThis.rowNumber();
        V rVector = tThis.generatorVec().zeros(tRowNum);
        for (int row = 0; row < tRowNum; ++row) {
            double rSum = 0.0;
            for (int col = 0; col < tColNum; ++col) rSum += it.next();
            rVector.set_(row, rSum/(double)tColNum);
        }
        return rVector;
    }
    
    default M transpose() {
        final MS tThis = thisMatrix_();
        final M rMatrix = tThis.generator().zeros(tThis.columnNumber(), tThis.rowNumber());
        
        final Iterator<Double> it = tThis.colIterator();
        final ISetIterator<Double> si = rMatrix.rowSetIterator();
        while(it.hasNext()) si.nextAndSet(it.next());
        return rMatrix;
    }
    
    /** stuff to override */
    @ApiStatus.Internal MS thisMatrix_();
}
