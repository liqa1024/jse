package jse.math.vector;

import jse.code.functional.IFloatConsumer;
import jse.code.functional.IFloatSupplier;
import jse.math.operation.DATA;

import static jse.math.vector.AbstractVector.rangeCheck;
import static jse.math.vector.AbstractVectorOperation.ebeCheck;

public abstract class AbstractFloatVectorOperation implements IFloatVectorOperation {
    @Override public void fill          (float              aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IFloatVector       aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign        (IFloatSupplier     aSup) {DATA.assign2This(thisVector_(), aSup);}
    @Override public void forEach       (IFloatConsumer     aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (IFloatVectorGetter aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    
    /** 向量的一些额外的运算 */
    @Override public IFloatVector reverse() {IFloatVector rVector = newVector_(); DATA.reverse2Dest(thisVector_(), rVector); return rVector;}
    @Override public IFloatVector refReverse() {
        return new RefFloatVector() {
            private final IFloatVector mThis = thisVector_();
            @Override public float get(int aIdx) {rangeCheck(aIdx, size()); return mThis.get(mThis.size()-1-aIdx);}
            @Override public void set(int aIdx, float aValue) {rangeCheck(aIdx, size()); mThis.set(mThis.size()-1-aIdx, aValue);}
            @Override public float getAndSet(int aIdx, float aValue) {rangeCheck(aIdx, size()); return mThis.getAndSet(mThis.size()-1-aIdx, aValue);}
            @Override public int size() {return mThis.size();}
        };
    }
    @Override public void reverse2this() {DATA.reverse2This(thisVector_());}
    
    /** 方便内部使用，减少一些重复代码 */
    private IFloatVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    protected abstract IFloatVector thisVector_();
    protected abstract IFloatVector newVector_(int aSize);
}
