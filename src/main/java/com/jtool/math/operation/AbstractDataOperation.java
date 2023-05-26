package com.jtool.math.operation;


import com.jtool.code.IFatIterable;
import com.jtool.code.ISetIterator;
import com.jtool.code.operator.IOperator1;
import com.jtool.code.operator.IOperator2;

import java.util.Iterator;

/**
 * 对于运算操作的一般实现，主要用于减少重复代码
 * @author liqa
 */
public abstract class AbstractDataOperation<R extends IFatIterable<? super T, N, E, N>, T, E extends N, N> implements IDataOperation<R, T, E, N> {
    /** add, minus, multiply, divide stuffs */
    @Override final public R ebeAdd       (T aLHS, T aRHS) {R r = newInstance_(); ebeAdd2Dest_        (aLHS, aRHS, r); return r;}
    @Override final public R ebeMinus     (T aLHS, T aRHS) {R r = newInstance_(); ebeMinus2Dest_      (aLHS, aRHS, r); return r;}
    @Override final public R ebeMultiply  (T aLHS, T aRHS) {R r = newInstance_(); ebeMultiply2Dest_   (aLHS, aRHS, r); return r;}
    @Override final public R ebeDivide    (T aLHS, T aRHS) {R r = newInstance_(); ebeDivide2Dest_     (aLHS, aRHS, r); return r;}
    @Override final public R ebeMod       (T aLHS, T aRHS) {R r = newInstance_(); ebeMod2Dest_        (aLHS, aRHS, r); return r;}
    
    @Override final public R mapAdd       (T aLHS, N aRHS) {R r = newInstance_(); mapAdd2Dest_        (aLHS, upcast_(aRHS), r); return r;}
    @Override final public R mapMinus     (T aLHS, N aRHS) {R r = newInstance_(); mapMinus2Dest_      (aLHS, upcast_(aRHS), r); return r;}
    @Override final public R mapLMinus    (T aLHS, N aRHS) {R r = newInstance_(); mapLMinus2Dest_     (aLHS, upcast_(aRHS), r); return r;}
    @Override final public R mapMultiply  (T aLHS, N aRHS) {R r = newInstance_(); mapMultiply2Dest_   (aLHS, upcast_(aRHS), r); return r;}
    @Override final public R mapDivide    (T aLHS, N aRHS) {R r = newInstance_(); mapDivide2Dest_     (aLHS, upcast_(aRHS), r); return r;}
    @Override final public R mapLDivide   (T aLHS, N aRHS) {R r = newInstance_(); mapLDivide2Dest_    (aLHS, upcast_(aRHS), r); return r;}
    @Override final public R mapMod       (T aLHS, N aRHS) {R r = newInstance_(); mapMod2Dest_        (aLHS, upcast_(aRHS), r); return r;}
    @Override final public R mapLMod      (T aLHS, N aRHS) {R r = newInstance_(); mapLMod2Dest_       (aLHS, upcast_(aRHS), r); return r;}
    
    @Override public final void ebeAdd2this       (T aRHS) {ebeAdd2this_      (thisInstance_(), aRHS);}
    @Override public final void ebeMinus2this     (T aRHS) {ebeMinus2this_    (thisInstance_(), aRHS);}
    @Override public final void ebeLMinus2this    (T aRHS) {ebeLMinus2this_   (thisInstance_(), aRHS);}
    @Override public final void ebeMultiply2this  (T aRHS) {ebeMultiply2this_ (thisInstance_(), aRHS);}
    @Override public final void ebeDivide2this    (T aRHS) {ebeDivide2this_   (thisInstance_(), aRHS);}
    @Override public final void ebeLDivide2this   (T aRHS) {ebeLDivide2this_  (thisInstance_(), aRHS);}
    @Override public final void ebeMod2this       (T aRHS) {ebeMod2this_      (thisInstance_(), aRHS);}
    @Override public final void ebeLMod2this      (T aRHS) {ebeLMod2this_     (thisInstance_(), aRHS);}
    
    @Override public final void mapAdd2this       (N aRHS) {mapAdd2this_      (thisInstance_(), upcast_(aRHS));}
    @Override public final void mapMinus2this     (N aRHS) {mapMinus2this_    (thisInstance_(), upcast_(aRHS));}
    @Override public final void mapLMinus2this    (N aRHS) {mapLMinus2this_   (thisInstance_(), upcast_(aRHS));}
    @Override public final void mapMultiply2this  (N aRHS) {mapMultiply2this_ (thisInstance_(), upcast_(aRHS));}
    @Override public final void mapDivide2this    (N aRHS) {mapDivide2this_   (thisInstance_(), upcast_(aRHS));}
    @Override public final void mapLDivide2this   (N aRHS) {mapLDivide2this_  (thisInstance_(), upcast_(aRHS));}
    @Override public final void mapMod2this       (N aRHS) {mapMod2this_      (thisInstance_(), upcast_(aRHS));}
    @Override public final void mapLMod2this      (N aRHS) {mapLMod2this_     (thisInstance_(), upcast_(aRHS));}
    
    
    /** do stuff */
    @Override public final R ebeDo(T aLHS, T aRHS, IOperator2<E> aOpt) {R r = newInstance_(); ebeDo2Dest_(aLHS, aRHS, r, aOpt); return r;}
    @Override public final R mapDo(T aLHS, IOperator1<E> aOpt) {R r = newInstance_(); mapDo2Dest_(aLHS, r, aOpt); return r;}
    @Override public final void ebeDo2this(T aRHS, IOperator2<E> aOpt) {ebeDo2this_(thisInstance_(), aRHS, aOpt);}
    @Override public final void mapDo2this(IOperator1<E> aOpt) {mapDo2this_(thisInstance_(), aOpt);}
    
    
    /** stuff to override */
    protected abstract void ebeAdd2Dest_        (T aLHS, T aRHS, R rDest);
    protected abstract void ebeMinus2Dest_      (T aLHS, T aRHS, R rDest);
    protected abstract void ebeMultiply2Dest_   (T aLHS, T aRHS, R rDest);
    protected abstract void ebeDivide2Dest_     (T aLHS, T aRHS, R rDest);
    protected abstract void ebeMod2Dest_        (T aLHS, T aRHS, R rDest);
    
    protected abstract void mapAdd2Dest_        (T aLHS, E aRHS, R rDest);
    protected abstract void mapMinus2Dest_      (T aLHS, E aRHS, R rDest);
    protected abstract void mapLMinus2Dest_     (T aLHS, E aRHS, R rDest);
    protected abstract void mapMultiply2Dest_   (T aLHS, E aRHS, R rDest);
    protected abstract void mapDivide2Dest_     (T aLHS, E aRHS, R rDest);
    protected abstract void mapLDivide2Dest_    (T aLHS, E aRHS, R rDest);
    protected abstract void mapMod2Dest_        (T aLHS, E aRHS, R rDest);
    protected abstract void mapLMod2Dest_       (T aLHS, E aRHS, R rDest);
    
    protected abstract void ebeAdd2this_        (R rThis, T aRHS);
    protected abstract void ebeMinus2this_      (R rThis, T aRHS);
    protected abstract void ebeLMinus2this_     (R rThis, T aRHS);
    protected abstract void ebeMultiply2this_   (R rThis, T aRHS);
    protected abstract void ebeDivide2this_     (R rThis, T aRHS);
    protected abstract void ebeLDivide2this_    (R rThis, T aRHS);
    protected abstract void ebeMod2this_        (R rThis, T aRHS);
    protected abstract void ebeLMod2this_       (R rThis, T aRHS);
    
    protected abstract void mapAdd2this_        (R rThis, E aRHS);
    protected abstract void mapMinus2this_      (R rThis, E aRHS);
    protected abstract void mapLMinus2this_     (R rThis, E aRHS);
    protected abstract void mapMultiply2this_   (R rThis, E aRHS);
    protected abstract void mapDivide2this_     (R rThis, E aRHS);
    protected abstract void mapLDivide2this_    (R rThis, E aRHS);
    protected abstract void mapMod2this_        (R rThis, E aRHS);
    protected abstract void mapLMod2this_       (R rThis, E aRHS);
    
    
    protected void ebeDo2Dest_(T aLHS, T aRHS, R rDest, IOperator2<E> aOpt) {
        final ISetIterator<E, N> si = rDest.setIterator();
        final Iterator<? extends N> li = rDest.iteratorOf(aLHS);
        final Iterator<? extends N> ri = rDest.iteratorOf(aRHS);
        while (si.hasNext()) {
            si.next();
            si.set(aOpt.cal(upcast_(li.next()), upcast_(ri.next())));
        }
    }
    protected void mapDo2Dest_(T aLHS, R rDest, IOperator1<E> aOpt) {
        final ISetIterator<E, N> si = rDest.setIterator();
        final Iterator<? extends N> li = rDest.iteratorOf(aLHS);
        while (si.hasNext()) {
            si.next();
            si.set(aOpt.cal(upcast_(li.next())));
        }
    }
    protected void ebeDo2this_(R rThis, T aRHS, IOperator2<E> aOpt) {
        final ISetIterator<E, N> si = rThis.setIterator();
        final Iterator<? extends N> ri = rThis.iteratorOf(aRHS);
        while (si.hasNext()) {
            si.set(aOpt.cal(si.next(), upcast_(ri.next())));
        }
    }
    protected void mapDo2this_(R rThis, IOperator1<E> aOpt) {
        final ISetIterator<E, N> si = rThis.setIterator();
        while (si.hasNext()) {
            si.set(aOpt.cal(si.next()));
        }
    }
    
    // 方便起见这里直接认为自身类型就是 R，如果遇到不是的再考虑
    protected abstract R thisInstance_();
    protected abstract R newInstance_();
    protected abstract E upcast_(N aValue);
}
