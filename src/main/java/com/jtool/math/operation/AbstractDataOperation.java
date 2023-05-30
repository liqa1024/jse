package com.jtool.math.operation;

import com.jtool.code.*;
import com.jtool.code.operator.IOperator1;
import com.jtool.code.operator.IOperator2;

import java.util.Iterator;

/**
 * 对于运算操作的一般实现，主要用于减少重复代码；
 * 输入仅支持 {@link IHasLotIterator} 而不是 {@link IFatIterable}，
 * 表明这个运算器仅用于计算内部的一些数据结构
 * @author liqa
 * @param <RS> 目标输出至少需要的类型，这里要求 {@code RM extends IHasLotIterator<? super T, Double>}
 * @param <R> 计算返回的数据类型
 * @param <S> 获取的自身的实例的类型，一般会有 S == R
 * @param <T> 计算输入的数据类型
 */
public abstract class AbstractDataOperation<RS extends IHasLotIterator<? super T, Double>, R extends RS, S extends RS, T> implements IDataOperation<R, T> {
    /** add, minus, multiply, divide stuffs */
    @Override final public R ebeAdd       (T aLHS, T aRHS) {R r = newInstance_(aLHS, aRHS); ebeAdd2Dest_        (aLHS, aRHS, r); return r;}
    @Override final public R ebeMinus     (T aLHS, T aRHS) {R r = newInstance_(aLHS, aRHS); ebeMinus2Dest_      (aLHS, aRHS, r); return r;}
    @Override final public R ebeMultiply  (T aLHS, T aRHS) {R r = newInstance_(aLHS, aRHS); ebeMultiply2Dest_   (aLHS, aRHS, r); return r;}
    @Override final public R ebeDivide    (T aLHS, T aRHS) {R r = newInstance_(aLHS, aRHS); ebeDivide2Dest_     (aLHS, aRHS, r); return r;}
    @Override final public R ebeMod       (T aLHS, T aRHS) {R r = newInstance_(aLHS, aRHS); ebeMod2Dest_        (aLHS, aRHS, r); return r;}
    
    @Override final public R mapAdd       (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapAdd2Dest_     (aLHS, aRHS, r); return r;}
    @Override final public R mapMinus     (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapMinus2Dest_   (aLHS, aRHS, r); return r;}
    @Override final public R mapLMinus    (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapLMinus2Dest_  (aLHS, aRHS, r); return r;}
    @Override final public R mapMultiply  (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapMultiply2Dest_(aLHS, aRHS, r); return r;}
    @Override final public R mapDivide    (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapDivide2Dest_  (aLHS, aRHS, r); return r;}
    @Override final public R mapLDivide   (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapLDivide2Dest_ (aLHS, aRHS, r); return r;}
    @Override final public R mapMod       (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapMod2Dest_     (aLHS, aRHS, r); return r;}
    @Override final public R mapLMod      (T aLHS, double aRHS) {R r = newInstance_(aLHS); mapLMod2Dest_    (aLHS, aRHS, r); return r;}
    
    @Override public final void ebeAdd2this       (T aRHS) {ebeAdd2this_      (thisInstance_(), aRHS);}
    @Override public final void ebeMinus2this     (T aRHS) {ebeMinus2this_    (thisInstance_(), aRHS);}
    @Override public final void ebeLMinus2this    (T aRHS) {ebeLMinus2this_   (thisInstance_(), aRHS);}
    @Override public final void ebeMultiply2this  (T aRHS) {ebeMultiply2this_ (thisInstance_(), aRHS);}
    @Override public final void ebeDivide2this    (T aRHS) {ebeDivide2this_   (thisInstance_(), aRHS);}
    @Override public final void ebeLDivide2this   (T aRHS) {ebeLDivide2this_  (thisInstance_(), aRHS);}
    @Override public final void ebeMod2this       (T aRHS) {ebeMod2this_      (thisInstance_(), aRHS);}
    @Override public final void ebeLMod2this      (T aRHS) {ebeLMod2this_     (thisInstance_(), aRHS);}
    
    @Override public final void mapAdd2this       (double aRHS) {mapAdd2this_      (thisInstance_(), aRHS);}
    @Override public final void mapMinus2this     (double aRHS) {mapMinus2this_    (thisInstance_(), aRHS);}
    @Override public final void mapLMinus2this    (double aRHS) {mapLMinus2this_   (thisInstance_(), aRHS);}
    @Override public final void mapMultiply2this  (double aRHS) {mapMultiply2this_ (thisInstance_(), aRHS);}
    @Override public final void mapDivide2this    (double aRHS) {mapDivide2this_   (thisInstance_(), aRHS);}
    @Override public final void mapLDivide2this   (double aRHS) {mapLDivide2this_  (thisInstance_(), aRHS);}
    @Override public final void mapMod2this       (double aRHS) {mapMod2this_      (thisInstance_(), aRHS);}
    @Override public final void mapLMod2this      (double aRHS) {mapLMod2this_     (thisInstance_(), aRHS);}
    
    
    /** do stuff */
    @Override public final R ebeDo(T aLHS, T aRHS, IOperator2<Double> aOpt) {R r = newInstance_(aLHS, aRHS); ebeDo2Dest_(aLHS, aRHS, r, aOpt); return r;}
    @Override public final R mapDo(T aLHS, IOperator1<Double> aOpt) {R r = newInstance_(aLHS); mapDo2Dest_(aLHS, r, aOpt); return r;}
    @Override public final void ebeDo2this(T aRHS, IOperator2<Double> aOpt) {ebeDo2this_(thisInstance_(), aRHS, aOpt);}
    @Override public final void mapDo2this(IOperator1<Double> aOpt) {mapDo2this_(thisInstance_(), aOpt);}
    
    @Override public final void mapFill2this(double aRHS) {mapFill2this_(thisInstance_(), aRHS);}
    @Override public final void ebeFill2this(T aRHS) {ebeFill2this_(thisInstance_(), aRHS);}
    
    /** stat stuff */
    @Override public final double sum() {return sumOfThis_(thisInstance_());}
    @Override public final double mean() {return meanOfThis_(thisInstance_());}
    
    
    /** 默认实现没做任何优化，重写来进行优化 */
    protected double sumOfThis_(RS tThis) {
        double rSum = 0.0;
        final Iterator<Double> it = tThis.iterator();
        while (it.hasNext()) rSum += it.next();
        return rSum;
    }
    protected double meanOfThis_(RS tThis) {
        double rSum = 0.0;
        double tNum = 0.0;
        final Iterator<Double> it = tThis.iterator();
        while (it.hasNext()) {
            rSum += it.next();
            ++tNum;
        }
        return rSum / tNum;
    }
    
    @SuppressWarnings("Convert2MethodRef")
    protected void ebeAdd2Dest_        (T aLHS, T aRHS, RS rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs + rhs));}
    protected void ebeMinus2Dest_      (T aLHS, T aRHS, RS rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs - rhs));}
    protected void ebeMultiply2Dest_   (T aLHS, T aRHS, RS rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs * rhs));}
    protected void ebeDivide2Dest_     (T aLHS, T aRHS, RS rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs / rhs));}
    protected void ebeMod2Dest_        (T aLHS, T aRHS, RS rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs % rhs));}
    
    protected void mapAdd2Dest_        (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs + aRHS));}
    protected void mapMinus2Dest_      (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs - aRHS));}
    protected void mapLMinus2Dest_     (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS - lhs));}
    protected void mapMultiply2Dest_   (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs * aRHS));}
    protected void mapDivide2Dest_     (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs / aRHS));}
    protected void mapLDivide2Dest_    (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS / lhs));}
    protected void mapMod2Dest_        (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs % aRHS));}
    protected void mapLMod2Dest_       (T aLHS, double aRHS, RS rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS % lhs));}
    
    @SuppressWarnings("Convert2MethodRef")
    protected void ebeAdd2this_        (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs + rhs));}
    protected void ebeMinus2this_      (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs - rhs));}
    protected void ebeLMinus2this_     (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs - lhs));}
    protected void ebeMultiply2this_   (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs * rhs));}
    protected void ebeDivide2this_     (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs / rhs));}
    protected void ebeLDivide2this_    (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs / lhs));}
    protected void ebeMod2this_        (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs % rhs));}
    protected void ebeLMod2this_       (RS rThis, T aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs % lhs));}
    
    protected void mapAdd2this_        (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs + aRHS));}
    protected void mapMinus2this_      (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs - aRHS));}
    protected void mapLMinus2this_     (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (aRHS - lhs));}
    protected void mapMultiply2this_   (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs * aRHS));}
    protected void mapDivide2this_     (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs / aRHS));}
    protected void mapLDivide2this_    (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (aRHS / lhs));}
    protected void mapMod2this_        (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs % aRHS));}
    protected void mapLMod2this_       (RS rThis, double aRHS) {mapDo2this_(rThis, lhs -> (aRHS % lhs));}
    
    
    protected void ebeDo2Dest_(T aLHS, T aRHS, RS rDest, IOperator2<Double> aOpt) {
        final ISetIterator<Double> si = rDest.setIterator();
        final Iterator<Double> li = rDest.iteratorOf(aLHS);
        final Iterator<Double> ri = rDest.iteratorOf(aRHS);
        while (si.hasNext()) si.nextAndSet(aOpt.cal(li.next(), ri.next()));
    }
    protected void mapDo2Dest_(T aLHS, RS rDest, IOperator1<Double> aOpt) {
        final ISetIterator<Double> si = rDest.setIterator();
        final Iterator<Double> li = rDest.iteratorOf(aLHS);
        while (si.hasNext()) si.nextAndSet(aOpt.cal(li.next()));
    }
    protected void ebeDo2this_(RS rThis, T aRHS, IOperator2<Double> aOpt) {
        final ISetIterator<Double> si = rThis.setIterator();
        final Iterator<Double> ri = rThis.iteratorOf(aRHS);
        while (si.hasNext()) si.set(aOpt.cal(si.next(), ri.next()));
    }
    protected void mapDo2this_(RS rThis, IOperator1<Double> aOpt) {
        final ISetIterator<Double> si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next()));
    }
    
    protected void mapFill2this_(RS rThis, double aRHS) {
        final ISetIterator<Double> si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aRHS);
    }
    protected void ebeFill2this_(RS rThis, T aRHS) {
        final ISetIterator<Double> si = rThis.setIterator();
        final Iterator<Double> ri = rThis.iteratorOf(aRHS);
        while (si.hasNext()) si.nextAndSet(ri.next());
    }
    
    // 方便起见这里直接认为自身类型就是 R，如果遇到不是的再考虑
    /** stuff to override */
    protected abstract S thisInstance_();
    protected abstract R newInstance_(T aData);
    protected abstract R newInstance_(T aData1, T aData2);
}
