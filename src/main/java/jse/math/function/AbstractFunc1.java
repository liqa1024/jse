package jse.math.function;

import jse.code.UT;
import jse.math.vector.IVector;

import java.util.Iterator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

public abstract class AbstractFunc1 implements IFunc1 {
    /** 批量修改的接口 */
    @Override public final void fill(double aValue) {operation().fill(aValue);}
    @Override public final void fill(IVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IFunc1 aFunc1) {operation().fill(aFunc1);}
    @Override public final void fill(IFunc1Subs aFunc1Subs) {operation().fill(aFunc1Subs);}
    @Override public final void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        assign(() -> UT.Code.doubleValue(it.next()));
    }
    @Override public final void assign(DoubleSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(DoubleConsumer aCon) {operation().forEach(aCon);}
    
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    @Override public void update(int aI, DoubleUnaryOperator aOpt) {
        rangeCheck(aI, Nx());
        set(aI, aOpt.applyAsDouble(get(aI)));
    }
    @Override public double getAndUpdate(int aI, DoubleUnaryOperator aOpt) {
        rangeCheck(aI, Nx());
        double tValue = get(aI);
        set(aI, aOpt.applyAsDouble(tValue));
        return tValue;
    }
    static void rangeCheck(int aIdx, int aSize) {
        if (aIdx<0 || aIdx>=aSize) throw new IndexOutOfBoundsException("Index = " + aIdx + ", Size = " + aSize);
    }
    
    /** near stuffs */
    @Override public final double getNear(double aX) {return get(getINear(aX));}
    @Override public final void setNear(double aX, double aV) {set(getINear(aX), aV);}
    @Override public final void updateNear(double aX, DoubleUnaryOperator aOpt) {update(getINear(aX), aOpt);}
    @Override public final double getAndUpdateNear(double aX, DoubleUnaryOperator aOpt) {return getAndUpdate(getINear(aX), aOpt);}
    
    /** stuff to override */
    public abstract double get(int aI);
    public abstract void set(int aI, double aV);
    public abstract int getINear(double aX);
}
