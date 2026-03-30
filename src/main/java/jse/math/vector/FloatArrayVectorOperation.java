package jse.math.vector;

import jse.code.functional.IFloatConsumer;
import jse.code.functional.IFloatSupplier;
import jse.math.IDataShell;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;

import static jse.math.vector.AbstractVectorOperation.ebeCheck;

public abstract class FloatArrayVectorOperation extends AbstractFloatVectorOperation {
    @Override public void fill          (float aRHS) {FloatArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (IFloatVector aRHS) {
        final FloatArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        float[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    @Override public void fill          (IFloatVectorGetter aRHS) {FloatArrayVector rThis = thisVector_(); ARRAY.vecFill2This (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aRHS);}
    @Override public void assign        (IFloatSupplier     aSup) {FloatArrayVector rThis = thisVector_(); ARRAY.assign2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aSup);}
    @Override public void forEach       (IFloatConsumer     aCon) {FloatArrayVector rThis = thisVector_(); ARRAY.forEachOfThis(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aCon);}
    
    @Override public IFloatVector reverse() {
        FloatArrayVector tThis = thisVector_();
        FloatArrayVector rVector = newVector_();
        float[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.reverse2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.reverse2Dest(tThis, rVector);
        return rVector;
    }
    
    /** 方便内部使用，减少一些重复代码 */
    private FloatArrayVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    @Override protected abstract FloatArrayVector thisVector_();
    @Override protected abstract FloatArrayVector newVector_(int aSize);
}
