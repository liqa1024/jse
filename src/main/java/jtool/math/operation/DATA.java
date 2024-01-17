package jtool.math.operation;

import jtool.code.iterator.*;
import jtool.code.functional.*;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;

import java.util.function.Supplier;

import static jtool.code.UT.Code.toComplexDouble;

/**
 * 对于运算操作的一般实现，主要用于减少重复代码；
 * 直接使用 {@link IHasDoubleIterator} 避免泛型的使用。
 * <p>
 * 这里会在可以的时候使用一些通用遍历方法，保证所有遍历优化都能用上，注意避免意料外的无限递归
 * @author liqa
 */
public class DATA {
    private DATA() {}
    
    /** logical stuffs */
    public static void ebeAnd2Dest  (IHasBooleanIterator aLHS, IHasBooleanIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs && rhs));}
    public static void ebeOr2Dest   (IHasBooleanIterator aLHS, IHasBooleanIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs || rhs));}
    public static void ebeXor2Dest  (IHasBooleanIterator aLHS, IHasBooleanIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs ^  rhs));}
    
    public static void mapAnd2Dest  (IHasBooleanIterator aLHS, final boolean aRHS, IHasBooleanSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs && aRHS));}
    public static void mapOr2Dest   (IHasBooleanIterator aLHS, final boolean aRHS, IHasBooleanSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs || aRHS));}
    public static void mapXor2Dest  (IHasBooleanIterator aLHS, final boolean aRHS, IHasBooleanSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs ^  aRHS));}
    
    public static void ebeAnd2This  (IHasBooleanSetIterator rThis, IHasBooleanIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs && rhs));}
    public static void ebeOr2This   (IHasBooleanSetIterator rThis, IHasBooleanIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs || rhs));}
    public static void ebeXor2This  (IHasBooleanSetIterator rThis, IHasBooleanIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs ^  rhs));}
    
    public static void mapAnd2This  (IHasBooleanSetIterator rThis, final boolean aRHS) {mapDo2This(rThis, lhs -> (lhs && aRHS));}
    public static void mapOr2This   (IHasBooleanSetIterator rThis, final boolean aRHS) {mapDo2This(rThis, lhs -> (lhs || aRHS));}
    public static void mapXor2This  (IHasBooleanSetIterator rThis, final boolean aRHS) {mapDo2This(rThis, lhs -> (lhs ^  aRHS));}
    
    public static void not2Dest     (IHasBooleanIterator aData, IHasBooleanSetOnlyIterator rDest) {mapDo2Dest(aData, rDest, v -> !v);}
    public static void not2This     (IHasBooleanSetIterator rThis) {mapDo2This(rThis, v -> !v);}
    
    public static boolean allOfThis(IHasBooleanIterator aThis) {
        final IBooleanIterator it = aThis.iterator();
        while (it.hasNext()) {
            boolean tValue = it.next();
            if (!tValue) return false;
        }
        return true;
    }
    public static boolean anyOfThis(IHasBooleanIterator aThis) {
        final IBooleanIterator it = aThis.iterator();
        while (it.hasNext()) {
            boolean tValue = it.next();
            if (tValue) return true;
        }
        return false;
    }
    public static int countOfThis(IHasBooleanIterator aThis) {
        final IBooleanIterator it = aThis.iterator();
        int rCount = 0;
        while (it.hasNext()) {
            boolean tValue = it.next();
            if (tValue) ++rCount;
        }
        return rCount;
    }
    
    public static void cumall2Dest(IHasBooleanIterator aThis, IHasBooleanSetOnlyIterator rDest) {
        final IBooleanIterator it = aThis.iterator();
        final IBooleanSetOnlyIterator si = rDest.setIterator();
        boolean rAll = true;
        while (it.hasNext()) {
            rAll &= it.next();
            si.nextAndSet(rAll);
        }
    }
    public static void cumany2Dest(IHasBooleanIterator aThis, IHasBooleanSetOnlyIterator rDest) {
        final IBooleanIterator it = aThis.iterator();
        final IBooleanSetOnlyIterator si = rDest.setIterator();
        boolean rAny = false;
        while (it.hasNext()) {
            rAny |= it.next();
            si.nextAndSet(rAny);
        }
    }
    public static void cumcount2Dest(IHasBooleanIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IBooleanIterator it = aThis.iterator();
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        double rCount = 0.0;
        while (it.hasNext()) {
            boolean tValue = it.next();
            if (tValue) ++rCount;
            si.nextAndSet(rCount);
        }
    }
    
    public static void ebeEqual2Dest            (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeCompare2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs == rhs));}
    public static void ebeGreater2Dest          (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeCompare2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs >  rhs));}
    public static void ebeGreaterOrEqual2Dest   (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeCompare2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs >= rhs));}
    public static void ebeLess2Dest             (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeCompare2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs <  rhs));}
    public static void ebeLessOrEqual2Dest      (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasBooleanSetOnlyIterator rDest) {ebeCompare2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs <= rhs));}
    public static void ebeCompare2Dest          (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasBooleanSetOnlyIterator rDest, final IComparator aOpt) {
        final IDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.cal(li.next(), ri.next()));
    }
    
    public static void mapEqual2Dest            (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs == aRHS));}
    public static void mapGreater2Dest          (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs >  aRHS));}
    public static void mapGreaterOrEqual2Dest   (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs >= aRHS));}
    public static void mapLess2Dest             (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs <  aRHS));}
    public static void mapLessOrEqual2Dest      (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs <= aRHS));}
    public static void mapCheck2Dest            (IHasDoubleIterator aData, IHasBooleanSetOnlyIterator rDest, final IChecker aOpt) {
        final IDoubleIterator it = aData.iterator();
        rDest.assign(() -> aOpt.cal(it.next()));
    }
    
    
    /** add, minus, multiply, divide stuffs */
    @SuppressWarnings("Convert2MethodRef")
    public static void ebePlus2Dest     (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs + rhs));}
    public static void ebeMinus2Dest    (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs - rhs));}
    public static void ebeMultiply2Dest (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs * rhs));}
    public static void ebeDiv2Dest      (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs / rhs));}
    public static void ebeMod2Dest      (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs % rhs));}
    
    public static void mapPlus2Dest     (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs + aRHS));}
    public static void mapMinus2Dest    (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs - aRHS));}
    public static void mapLMinus2Dest   (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (aRHS - lhs));}
    public static void mapMultiply2Dest (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs * aRHS));}
    public static void mapDiv2Dest      (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs / aRHS));}
    public static void mapLDiv2Dest     (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (aRHS / lhs));}
    public static void mapMod2Dest      (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs % aRHS));}
    public static void mapLMod2Dest     (IHasDoubleIterator aLHS, final double aRHS, IHasDoubleSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (aRHS % lhs));}
    
    @SuppressWarnings("Convert2MethodRef")
    public static void ebePlus2This     (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs + rhs));}
    public static void ebeMinus2This    (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs - rhs));}
    public static void ebeLMinus2This   (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (rhs - lhs));}
    public static void ebeMultiply2This (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs * rhs));}
    public static void ebeDiv2This      (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs / rhs));}
    public static void ebeLDiv2This     (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (rhs / lhs));}
    public static void ebeMod2This      (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs % rhs));}
    public static void ebeLMod2This     (IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (rhs % lhs));}
    
    public static void mapPlus2This     (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (lhs + aRHS));}
    public static void mapMinus2This    (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (lhs - aRHS));}
    public static void mapLMinus2This   (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (aRHS - lhs));}
    public static void mapMultiply2This (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (lhs * aRHS));}
    public static void mapDiv2This      (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (lhs / aRHS));}
    public static void mapLDiv2This     (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (aRHS / lhs));}
    public static void mapMod2This      (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (lhs % aRHS));}
    public static void mapLMod2This     (IHasDoubleSetIterator rThis, final double aRHS) {mapDo2This(rThis, lhs -> (aRHS % lhs));}
    
    
    public static void mapNegative2Dest(IHasDoubleIterator aData, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleIterator it = aData.iterator();
        rDest.assign(() -> -it.next());
    }
    public static void mapNegative2This(IHasDoubleSetIterator rThis) {
        final IDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(-si.next());
    }
    public static void mapNegative2Dest(IHasComplexDoubleIterator aData, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator it = aData.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            it.nextOnly();
            si.nextOnly();
            si.setReal(-it.real());
            si.setImag(-it.imag());
        }
    }
    public static void mapNegative2This(IHasComplexDoubleSetIterator rThis) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.setReal(-si.real());
            si.setImag(-si.imag());
        }
    }
    
    
    /** complex double stuffs */
    public static void ebePlus2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() + ri.next());
            si.setImag(li.imag());
        }
    }
    public static void ebeMinus2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() + ri.next());
            si.setImag(li.imag());
        }
    }
    public static void ebeMultiply2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            double tRHS = ri.next();
            si.setReal(li.real() * tRHS);
            si.setImag(li.imag() * tRHS);
        }
    }
    public static void ebeDiv2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            double tRHS = ri.next();
            si.setReal(li.real() / tRHS);
            si.setImag(li.imag() / tRHS);
        }
    }
    public static void ebePlus2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.setReal(li.next() + ri.real());
            si.setImag(ri.imag());
        }
    }
    public static void ebeMinus2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.setReal(li.next() - ri.real());
            si.setImag(-ri.imag());
        }
    }
    public static void ebeMultiply2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            double tLHS = li.next();
            si.setReal(tLHS * ri.real());
            si.setImag(tLHS * ri.imag());
        }
    }
    public static void ebeDiv2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            double tLHS = li.next();
            si.setReal(tLHS / ri.real());
            si.setImag(tLHS / ri.imag());
        }
    }
    public static void ebePlus2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            si.nextOnly();
            si.setReal(li.real() + ri.real());
            si.setImag(li.imag() + ri.imag());
        }
    }
    public static void ebeMinus2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            si.nextOnly();
            si.setReal(li.real() - ri.real());
            si.setImag(li.imag() - ri.imag());
        }
    }
    public static void ebeMultiply2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            si.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double rReal = ri.real(), rImag = ri.imag();
            si.setReal(lReal*rReal - lImag*rImag);
            si.setImag(lImag*rReal + lReal*rImag);
        }
    }
    public static void ebeDiv2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            si.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double rReal = ri.real(), rImag = ri.imag();
            double div = rReal*rReal + rImag*rImag;
            si.setReal((lReal*rReal + lImag*rImag)/div);
            si.setImag((lImag*rReal - lReal*rImag)/div);
        }
    }
    
    public static void mapPlus2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() + aRHS);
            si.setImag(li.imag());
        }
    }
    public static void mapMinus2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() - aRHS);
            si.setImag(li.imag());
        }
    }
    public static void mapLMinus2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(aRHS - li.real());
            si.setImag(-li.imag());
        }
    }
    public static void mapMultiply2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() * aRHS);
            si.setImag(li.imag() * aRHS);
        }
    }
    public static void mapDiv2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() / aRHS);
            si.setImag(li.imag() / aRHS);
        }
    }
    public static void mapLDiv2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double div = lReal*lReal + lImag*lImag;
            si.setReal((aRHS*lReal)/div);
            si.setImag((-aRHS*lImag)/div);
        }
    }
    public static void mapPlus2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextOnly();
            si.setReal(li.next() + rReal);
            si.setImag(rImag);
        }
    }
    public static void mapMinus2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextOnly();
            si.setReal(li.next() - rReal);
            si.setImag(-rImag);
        }
    }
    public static void mapLMinus2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextOnly();
            si.setReal(rReal - li.next());
            si.setImag(rImag);
        }
    }
    public static void mapMultiply2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextOnly();
            double tLHS = li.next();
            si.setReal(tLHS * rReal);
            si.setImag(tLHS * rImag);
        }
    }
    public static void mapDiv2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        final double div = rReal*rReal + rImag*rImag;
        while (si.hasNext()) {
            si.nextOnly();
            double tLHS = li.next();
            si.setReal((tLHS*rReal)/div);
            si.setImag((-tLHS*rImag)/div);
        }
    }
    public static void mapLDiv2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextOnly();
            double tLHS = li.next();
            si.setReal(rReal / tLHS);
            si.setImag(rImag / tLHS);
        }
    }
    public static void mapPlus2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() + rReal);
            si.setImag(li.imag() + rImag);
        }
    }
    public static void mapMinus2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(li.real() - rReal);
            si.setImag(li.imag() - rImag);
        }
    }
    public static void mapLMinus2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            si.setReal(rReal - li.real());
            si.setImag(rImag - li.imag());
        }
    }
    public static void mapMultiply2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            si.setReal(lReal*rReal - lImag*rImag);
            si.setImag(lImag*rReal + lReal*rImag);
        }
    }
    public static void mapDiv2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        final double div = rReal*rReal + rImag*rImag;
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            si.setReal((lReal*rReal + lImag*rImag)/div);
            si.setImag((lImag*rReal - lReal*rImag)/div);
        }
    }
    public static void mapLDiv2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double div = lReal*lReal + lImag*lImag;
            si.setReal((rReal*lReal + rImag*lImag)/div);
            si.setImag((rImag*lReal - rReal*lImag)/div);
        }
    }
    
    public static void ebePlus2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.plus2this(ri.next());
        }
    }
    public static void ebeMinus2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.minus2this(ri.next());
        }
    }
    public static void ebeLMinus2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.lminus2this(ri.next());
        }
    }
    public static void ebeMultiply2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.multiply2this(ri.next());
        }
    }
    public static void ebeDiv2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.div2this(ri.next());
        }
    }
    public static void ebeLDiv2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.ldiv2this(ri.next());
        }
    }
    public static void ebePlus2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.plus2this(ri);
        }
    }
    public static void ebeMinus2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.minus2this(ri);
        }
    }
    public static void ebeLMinus2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.lminus2this(ri);
        }
    }
    public static void ebeMultiply2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.multiply2this(ri);
        }
    }
    public static void ebeDiv2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.div2this(ri);
        }
    }
    public static void ebeLDiv2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.ldiv2this(ri);
        }
    }
    
    public static void mapPlus2This(IHasComplexDoubleSetIterator rThis, double aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.plus2this(aRHS);
        }
    }
    public static void mapMinus2This(IHasComplexDoubleSetIterator rThis, double aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.minus2this(aRHS);
        }
    }
    public static void mapLMinus2This(IHasComplexDoubleSetIterator rThis, double aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.lminus2this(aRHS);
        }
    }
    public static void mapMultiply2This(IHasComplexDoubleSetIterator rThis, double aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.multiply2this(aRHS);
        }
    }
    public static void mapDiv2This(IHasComplexDoubleSetIterator rThis, double aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.div2this(aRHS);
        }
    }
    public static void mapLDiv2This(IHasComplexDoubleSetIterator rThis, double aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.ldiv2this(aRHS);
        }
    }
    public static void mapPlus2This(IHasComplexDoubleSetIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final ComplexDouble tRHS = toComplexDouble(aRHS);
        while (si.hasNext()) {
            si.nextOnly();
            si.plus2this(tRHS);
        }
    }
    public static void mapMinus2This(IHasComplexDoubleSetIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final ComplexDouble tRHS = toComplexDouble(aRHS);
        while (si.hasNext()) {
            si.nextOnly();
            si.minus2this(tRHS);
        }
    }
    public static void mapLMinus2This(IHasComplexDoubleSetIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final ComplexDouble tRHS = toComplexDouble(aRHS);
        while (si.hasNext()) {
            si.nextOnly();
            si.lminus2this(tRHS);
        }
    }
    public static void mapMultiply2This(IHasComplexDoubleSetIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final ComplexDouble tRHS = toComplexDouble(aRHS);
        while (si.hasNext()) {
            si.nextOnly();
            si.multiply2this(tRHS);
        }
    }
    public static void mapDiv2This(IHasComplexDoubleSetIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final ComplexDouble tRHS = toComplexDouble(aRHS);
        while (si.hasNext()) {
            si.nextOnly();
            si.div2this(tRHS);
        }
    }
    public static void mapLDiv2This(IHasComplexDoubleSetIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final ComplexDouble tRHS = toComplexDouble(aRHS);
        while (si.hasNext()) {
            si.nextOnly();
            si.ldiv2this(tRHS);
        }
    }
    
    /** do stuff */
    public static void ebeDo2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest, final IOperator2<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.cal(li.next(), ri.next()));
    }
    public static void ebeDo2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest, final IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.cal(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleSetOnlyIterator rDest, final IOperator1<? extends IComplexDouble, Double> aOpt) {
        final IDoubleIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.cal(it.next()));
    }
    public static void mapDo2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleSetOnlyIterator rDest, final IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.cal(it.next()));
    }
    public static void ebeDo2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next(), ri.next()));
    }
    public static void ebeDo2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasComplexDoubleSetIterator rThis, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next()));
    }
    
    public static void ebeDo2Dest(IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest, final IDoubleOperator2 aOpt) {
        final IDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.cal(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasDoubleIterator aLHS, IHasDoubleSetOnlyIterator rDest, final IDoubleOperator1 aOpt) {
        final IDoubleIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.cal(it.next()));
    }
    public static void ebeDo2This(IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS, IDoubleOperator2 aOpt) {
        final IDoubleIterator ri = aRHS.iterator();
        final IDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasDoubleSetIterator rThis, IDoubleOperator1 aOpt) {
        final IDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next()));
    }
    
    public static void ebeDo2Dest(IHasBooleanIterator aLHS, IHasBooleanIterator aRHS, IHasBooleanSetOnlyIterator rDest, final IBooleanOperator2 aOpt) {
        final IBooleanIterator li = aLHS.iterator();
        final IBooleanIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.cal(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasBooleanIterator aLHS, IHasBooleanSetOnlyIterator rDest, final IBooleanOperator1 aOpt) {
        final IBooleanIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.cal(it.next()));
    }
    public static void ebeDo2This(IHasBooleanSetIterator rThis, IHasBooleanIterator aRHS, IBooleanOperator2 aOpt) {
        final IBooleanIterator ri = aRHS.iterator();
        final IBooleanSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasBooleanSetIterator rThis, IBooleanOperator1 aOpt) {
        final IBooleanSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next()));
    }
    
    
    public static void mapFill2This(IHasComplexDoubleSetOnlyIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextOnly();
            si.setReal(rReal);
            si.setImag(rImag);
        }
    }
    public static void mapFill2This(IHasComplexDoubleSetOnlyIterator rThis, final double aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasComplexDoubleSetOnlyIterator rThis, IHasComplexDoubleIterator aRHS) {
        final IComplexDoubleIterator it = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) {
            it.nextOnly();
            si.nextOnly();
            si.setReal(it.real());
            si.setImag(it.imag());
        }
    }
    public static void ebeFill2This(IHasComplexDoubleSetOnlyIterator rThis, IHasDoubleIterator aRHS) {
        final IDoubleIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void assign2This(IHasComplexDoubleSetOnlyIterator rThis, Supplier<? extends IComplexDouble> aSup) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void assign2This(IHasComplexDoubleSetOnlyIterator rThis, IDoubleSupplier aSup) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void forEachOfThis(IHasComplexDoubleIterator aThis, IConsumer1<? super ComplexDouble> aCon) {
        final IComplexDoubleIterator it = aThis.iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
    public static void forEachOfThis(IHasComplexDoubleIterator aThis, IDoubleConsumer2 aCon) {
        final IComplexDoubleIterator it = aThis.iterator();
        while (it.hasNext()) {
            it.nextOnly();
            aCon.run(it.real(), it.imag());
        }
    }
    
    public static void mapFill2This(IHasDoubleSetOnlyIterator rThis, final double aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasDoubleSetOnlyIterator rThis, IHasDoubleIterator aRHS) {
        final IDoubleIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void assign2This(IHasDoubleSetOnlyIterator rThis, IDoubleSupplier aSup) {
        final IDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void forEachOfThis(IHasDoubleIterator aThis, IDoubleConsumer1 aCon) {
        final IDoubleIterator it = aThis.iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
    
    public static void mapFill2This(IHasBooleanSetOnlyIterator rThis, final boolean aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasBooleanSetOnlyIterator rThis, IHasBooleanIterator aRHS) {
        final IBooleanIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void assign2This(IHasBooleanSetOnlyIterator rThis, IBooleanSupplier aSup) {
        final IBooleanSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void forEachOfThis(IHasBooleanIterator aThis, IBooleanConsumer1 aCon) {
        final IBooleanIterator it = aThis.iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
    
    public static void mapFill2This(IHasIntegerSetOnlyIterator rThis, final int aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasIntegerSetOnlyIterator rThis, IHasIntegerIterator aRHS) {
        final IIntegerIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void assign2This(IHasIntegerSetOnlyIterator rThis, IIntegerSupplier aSup) {
        final IIntegerSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void forEachOfThis(IHasIntegerIterator aThis, IIntegerConsumer1 aCon) {
        final IIntegerIterator it = aThis.iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
    
    
    /** stat stuff */
    public static double sumOfThis(IHasDoubleIterator aThis) {
        final IDoubleIterator it = aThis.iterator();
        double rSum = 0.0;
        while (it.hasNext()) rSum += it.next();
        return rSum;
    }
    public static ComplexDouble sumOfThis(IHasComplexDoubleIterator aThis) {
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rSum = new ComplexDouble();
        while (it.hasNext()) {
            it.nextOnly();
            rSum.plus2this(it);
        }
        return rSum;
    }
    public static double meanOfThis(IHasDoubleIterator aThis) {
        final IDoubleIterator it = aThis.iterator();
        double rSum = 0.0;
        double tNum = 0.0;
        while (it.hasNext()) {
            rSum += it.next();
            ++tNum;
        }
        return rSum / tNum;
    }
    public static ComplexDouble meanOfThis(IHasComplexDoubleIterator aThis) {
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rMean = new ComplexDouble();
        double tNum = 0.0;
        while (it.hasNext()) {
            it.nextOnly();
            rMean.plus2this(it);
            ++tNum;
        }
        rMean.div2this(tNum);
        return rMean;
    }
    public static double prodOfThis(IHasDoubleIterator aThis) {
        final IDoubleIterator it = aThis.iterator();
        double rProd = 1.0;
        while (it.hasNext()) rProd *= it.next();
        return rProd;
    }
    public static ComplexDouble prodOfThis(IHasComplexDoubleIterator aThis) {
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rProd = new ComplexDouble(1.0);
        while (it.hasNext()) {
            it.nextOnly();
            rProd.multiply2this(it);
        }
        return rProd;
    }
    public static double maxOfThis(IHasDoubleIterator aThis) {
        final IDoubleIterator it = aThis.iterator();
        double rMax = Double.NaN;
        while (it.hasNext()) {
            double tValue = it.next();
            if (Double.isNaN(rMax) || tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static double minOfThis(IHasDoubleIterator aThis) {
        final IDoubleIterator it = aThis.iterator();
        double rMin = Double.NaN;
        while (it.hasNext()) {
            double tValue = it.next();
            if (Double.isNaN(rMin) || tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    public static double statOfThis(IHasDoubleIterator aThis, IDoubleOperator2 aOpt) {
        final IDoubleIterator it = aThis.iterator();
        double rStat = Double.NaN;
        while (it.hasNext()) rStat = aOpt.cal(rStat, it.next());
        return rStat;
    }
    public static ComplexDouble statOfThis(IHasComplexDoubleIterator aThis, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rStat = null;
        while (it.hasNext()) rStat = toComplexDouble(aOpt.cal(rStat, it.next()));
        return rStat;
    }
    
    public static void cumsum2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rSum = 0.0;
        while (it.hasNext()) {
            rSum += it.next();
            si.nextAndSet(rSum);
        }
    }
    public static void cumsum2Dest(IHasComplexDoubleIterator aThis, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rSum = new ComplexDouble();
        while (it.hasNext()) {
            it.nextOnly();
            si.nextOnly();
            rSum.plus2this(it);
            si.setReal(rSum.mReal);
            si.setImag(rSum.mImag);
        }
    }
    public static void cummean2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rSum = 0.0;
        double tNum = 0.0;
        while (it.hasNext()) {
            rSum += it.next();
            ++tNum;
            si.nextAndSet(rSum / tNum);
        }
    }
    public static void cummean2Dest(IHasComplexDoubleIterator aThis, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rSum = new ComplexDouble();
        double tNum = 0.0;
        while (it.hasNext()) {
            it.nextOnly();
            si.nextOnly();
            rSum.plus2this(it);
            ++tNum;
            si.setReal(rSum.mReal / tNum);
            si.setImag(rSum.mImag / tNum);
        }
    }
    public static void cumprod2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rProd = 1.0;
        while (it.hasNext()) {
            rProd *= it.next();
            si.nextAndSet(rProd);
        }
    }
    public static void cumprod2Dest(IHasComplexDoubleIterator aThis, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rProd = new ComplexDouble(1.0);
        while (it.hasNext()) {
            it.nextOnly();
            si.nextOnly();
            rProd.multiply2this(it);
            si.setReal(rProd.mReal);
            si.setImag(rProd.mImag);
        }
    }
    public static void cummax2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rMax = Double.NaN;
        while (it.hasNext()) {
            double tValue = it.next();
            if (Double.isNaN(rMax) || tValue > rMax) rMax = tValue;
            si.nextAndSet(rMax);
        }
    }
    public static void cummin2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rMin = Double.NaN;
        while (it.hasNext()) {
            double tValue = it.next();
            if (Double.isNaN(rMin) || tValue < rMin) rMin = tValue;
            si.nextAndSet(rMin);
        }
    }
    public static void cumstat2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest, IDoubleOperator2 aOpt) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rStat = Double.NaN;
        while (it.hasNext()) {
            rStat = aOpt.cal(rStat, it.next());
            si.nextAndSet(rStat);
        }
    }
    public static void cumstat2Dest(IHasComplexDoubleIterator aThis, IHasComplexDoubleSetOnlyIterator rDest, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rStat = null;
        while (it.hasNext()) {
            rStat = toComplexDouble(aOpt.cal(rStat, it.next()));
            si.nextOnly();
            si.setReal(rStat.mReal);
            si.setImag(rStat.mImag);
        }
    }
    
    
    /** 较为复杂的运算，只有遇到时专门增加，主要避免 IOperator2 使用需要新建 ComplexDouble */
    public static void mapMultiplyThenEbePlus2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS, double aMul) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            si.setComplexDouble(si.real() + aMul*ri.real(), si.imag() + aMul*ri.imag());
        }
    }
    public static void mapMultiplyThenEbePlus2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS, IComplexDouble aMul) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final double lReal = aMul.real(), lImag = aMul.imag();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextOnly();
            final double rReal = ri.real(),   rImag = ri.imag();
            si.setComplexDouble(si.real() + (lReal*rReal - lImag*rImag), si.imag() + (lImag*rReal + lReal*rImag));
        }
    }
}
