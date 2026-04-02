package jse.math.matrix;

import jse.math.IDataShell;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;

import static jse.math.matrix.AbstractMatrixOperation.ebeCheck;

/**
 * 对于内部含有 boolean[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class BooleanArrayMatrixOperation extends AbstractLogicalMatrixOperation {
    @Override public void fill          (boolean aRHS) {BooleanArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapFill2This(rMatrix.internalData(), rMatrix.internalDataShift(), aRHS, rMatrix.internalDataSize());}
    @Override public void fill          (ILogicalMatrix aRHS) {
        BooleanArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    
    /** 方便内部使用，减少一些重复代码 */
    private BooleanArrayMatrix newMatrix_() {
        final BooleanArrayMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.nrows(), tThis.ncols());
    }
    
    /** stuff to override */
    @Override protected abstract BooleanArrayMatrix thisMatrix_();
    @Override protected abstract BooleanArrayMatrix newMatrix_(int aRowNum, int aColNum);
}
