package jtool.math.matrix;

import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

/**
 * 对于内部含有 int[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class IntArrayMatrixOperation extends AbstractIntMatrixOperation {
    @Override public void fill          (int aRHS) {IntArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapFill2This(rMatrix.internalData(), rMatrix.internalDataShift(), aRHS, rMatrix.internalDataSize());}
    @Override public void fill          (IIntMatrix aRHS) {
        IntArrayMatrix rThis = thisMatrix_();
        int[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    
    /** 方便内部使用，减少一些重复代码 */
    private IntArrayMatrix newMatrix_() {
        final IntArrayMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.rowNumber(), tThis.columnNumber());
    }
    
    /** stuff to override */
    @Override protected abstract IntArrayMatrix thisMatrix_();
    @Override protected abstract IntArrayMatrix newMatrix_(int aRowNum, int aColNum);
}
