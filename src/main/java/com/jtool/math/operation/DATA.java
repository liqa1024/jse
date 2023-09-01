package com.jtool.math.operation;

import com.jtool.code.iterator.*;
import com.jtool.code.functional.*;

/**
 * 对于运算操作的一般实现，主要用于减少重复代码；
 * 直接使用 {@link IHasDoubleIterator} 避免泛型的使用。
 * <p>
 * 为了避免意料外的无限递归，这里直接使用迭代器遍历而不是通用的遍历方法
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
    public static void ebeCompare2Dest          (IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasBooleanSetOnlyIterator rDest, IComparator aOpt) {
        final IDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IBooleanSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) si.nextAndSet(aOpt.cal(li.next(), ri.next()));
    }
    
    public static void mapEqual2Dest            (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs == aRHS));}
    public static void mapGreater2Dest          (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs >  aRHS));}
    public static void mapGreaterOrEqual2Dest   (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs >= aRHS));}
    public static void mapLess2Dest             (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs <  aRHS));}
    public static void mapLessOrEqual2Dest      (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs <= aRHS));}
    public static void mapCheck2Dest            (IHasDoubleIterator aData, IHasBooleanSetOnlyIterator rDest, IChecker aOpt) {
        final IDoubleIterator it = aData.iterator();
        final IBooleanSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) si.nextAndSet(aOpt.cal(it.next()));
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
    
    
    /** do stuff */
    public static void ebeDo2Dest(IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest, IDoubleOperator2 aOpt) {
        final IDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) si.nextAndSet(aOpt.cal(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasDoubleIterator aLHS, IHasDoubleSetOnlyIterator rDest, IDoubleOperator1 aOpt) {
        final IDoubleIterator it = aLHS.iterator();
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) si.nextAndSet(aOpt.cal(it.next()));
    }
    public static void ebeDo2This(IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS, IDoubleOperator2 aOpt) {
        final IDoubleIterator ri = aRHS.iterator();
        final IDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasDoubleSetIterator rThis, final IDoubleOperator1 aOpt) {
        final IDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.cal(si.next()));
    }
    
    public static void ebeDo2Dest(IHasBooleanIterator aLHS, IHasBooleanIterator aRHS, IHasBooleanSetOnlyIterator rDest, IBooleanOperator2 aOpt) {
        final IBooleanIterator li = aLHS.iterator();
        final IBooleanIterator ri = aRHS.iterator();
        final IBooleanSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) si.nextAndSet(aOpt.cal(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasBooleanIterator aLHS, IHasBooleanSetOnlyIterator rDest, IBooleanOperator1 aOpt) {
        final IBooleanIterator it = aLHS.iterator();
        final IBooleanSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) si.nextAndSet(aOpt.cal(it.next()));
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
    
    
    public static void mapFill2This(IHasDoubleSetOnlyIterator rThis, double aRHS) {
        final IDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aRHS);
    }
    public static void ebeFill2This(IHasDoubleSetOnlyIterator rThis, IHasDoubleIterator aRHS) {
        final IDoubleIterator it = aRHS.iterator();
        final IDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(it.next());
    }
    public static void assign2This(IHasDoubleSetOnlyIterator rThis, IDoubleSupplier aSup) {
        final IDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void forEachOfThis(IHasDoubleIterator aThis, IDoubleConsumer1 aCon) {
        final IDoubleIterator it = aThis.iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
    
    public static void mapFill2This(IHasBooleanSetOnlyIterator rThis, boolean aRHS) {
        final IBooleanSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aRHS);
    }
    public static void ebeFill2This(IHasBooleanSetOnlyIterator rThis, IHasBooleanIterator aRHS) {
        final IBooleanIterator it = aRHS.iterator();
        final IBooleanSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(it.next());
    }
    public static void assign2This(IHasBooleanSetOnlyIterator rThis, IBooleanSupplier aSup) {
        final IBooleanSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void forEachOfThis(IHasBooleanIterator aThis, IBooleanConsumer1 aCon) {
        final IBooleanIterator it = aThis.iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
    
    
    /** stat stuff */
    public static double sumOfThis(IHasDoubleIterator aThis) {
        final IDoubleIterator it = aThis.iterator();
        double rSum = 0.0;
        while (it.hasNext()) rSum += it.next();
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
    public static double prodOfThis(IHasDoubleIterator aThis) {
        final IDoubleIterator it = aThis.iterator();
        double rProd = 1.0;
        while (it.hasNext()) rProd *= it.next();
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
    
    public static void cumsum2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rSum = 0.0;
        while (it.hasNext()) {
            rSum += it.next();
            si.nextAndSet(rSum);
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
    public static void cumprod2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rProd = 1.0;
        while (it.hasNext()) {
            rProd *= it.next();
            si.nextAndSet(rProd);
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
}
