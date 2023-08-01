package com.jtool.math.operation;

import com.jtool.code.iterator.*;
import com.jtool.code.operator.IBooleanOperator1;
import com.jtool.code.operator.IBooleanOperator2;
import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;

/**
 * 对于运算操作的一般实现，主要用于减少重复代码；
 * 直接使用 {@link IDoubleIterator} 避免泛型的使用。
 * <p>
 * 由于传入都是迭代器，因此调用后输入的迭代器都会失效
 * @author liqa
 */
public class DATA {
    private DATA() {}
    
    /** logical stuffs */
    public static void ebeAnd2Dest_     (IBooleanIterator aLHS, IBooleanIterator aRHS, IBooleanSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs && rhs));}
    public static void ebeOr2Dest_      (IBooleanIterator aLHS, IBooleanIterator aRHS, IBooleanSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs || rhs));}
    public static void ebeXor2Dest_     (IBooleanIterator aLHS, IBooleanIterator aRHS, IBooleanSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs ^  rhs));}
    
    public static void mapAnd2Dest_     (IBooleanIterator aLHS, final boolean aRHS, IBooleanSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs && aRHS));}
    public static void mapOr2Dest_      (IBooleanIterator aLHS, final boolean aRHS, IBooleanSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs || aRHS));}
    public static void mapXor2Dest_     (IBooleanIterator aLHS, final boolean aRHS, IBooleanSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs ^  aRHS));}
    
    public static void ebeAnd2this_     (IBooleanSetIterator rThis, IBooleanIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs && rhs));}
    public static void ebeOr2this_      (IBooleanSetIterator rThis, IBooleanIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs || rhs));}
    public static void ebeXor2this_     (IBooleanSetIterator rThis, IBooleanIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs ^  rhs));}
    
    public static void mapAnd2this_     (IBooleanSetIterator rThis, final boolean aRHS) {mapDo2this_(rThis, lhs -> (lhs && aRHS));}
    public static void mapOr2this_      (IBooleanSetIterator rThis, final boolean aRHS) {mapDo2this_(rThis, lhs -> (lhs || aRHS));}
    public static void mapXor2this_     (IBooleanSetIterator rThis, final boolean aRHS) {mapDo2this_(rThis, lhs -> (lhs ^  aRHS));}
    
    public static void not2Dest_        (IBooleanIterator aData, IBooleanSetOnlyIterator rDest) {mapDo2Dest_(aData, rDest, v -> !v);}
    public static void not2this_        (IBooleanSetIterator rThis) {mapDo2this_(rThis, v -> !v);}
    
    public static boolean allOfThis_(IBooleanIterator aThis) {
        while (aThis.hasNext()) {
            boolean tValue = aThis.next();
            if (!tValue) return false;
        }
        return true;
    }
    public static boolean anyOfThis_(IBooleanIterator aThis) {
        while (aThis.hasNext()) {
            boolean tValue = aThis.next();
            if (tValue) return true;
        }
        return false;
    }
    public static int countOfThis_(IBooleanIterator aThis) {
        int rCount = 0;
        while (aThis.hasNext()) {
            boolean tValue = aThis.next();
            if (tValue) ++rCount;
        }
        return rCount;
    }
    
    public static void cumall2Dest_(IBooleanIterator aThis, IBooleanSetOnlyIterator rDest) {
        boolean rAll = true;
        while (aThis.hasNext()) {
            rAll &= aThis.next();
            rDest.nextAndSet(rAll);
        }
    }
    public static void cumany2Dest_(IBooleanIterator aThis, IBooleanSetOnlyIterator rDest) {
        boolean rAny = false;
        while (aThis.hasNext()) {
            rAny |= aThis.next();
            rDest.nextAndSet(rAny);
        }
    }
    public static void cumcount2Dest_(IBooleanIterator aThis, IDoubleSetOnlyIterator rDest) {
        int rCount = 0;
        while (aThis.hasNext()) {
            boolean tValue = aThis.next();
            if (tValue) ++rCount;
            rDest.nextAndSet(rCount);
        }
    }
    
    /** add, minus, multiply, divide stuffs */
    @SuppressWarnings("Convert2MethodRef")
    public static void ebePlus2Dest_    (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs + rhs));}
    public static void ebeMinus2Dest_   (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs - rhs));}
    public static void ebeMultiply2Dest_(IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs * rhs));}
    public static void ebeDiv2Dest_     (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs / rhs));}
    public static void ebeMod2Dest_     (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs % rhs));}
    
    public static void mapPlus2Dest_    (IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs + aRHS));}
    public static void mapMinus2Dest_   (IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs - aRHS));}
    public static void mapLMinus2Dest_  (IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS - lhs));}
    public static void mapMultiply2Dest_(IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs * aRHS));}
    public static void mapDiv2Dest_     (IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs / aRHS));}
    public static void mapLDiv2Dest_    (IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS / lhs));}
    public static void mapMod2Dest_     (IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs % aRHS));}
    public static void mapLMod2Dest_    (IDoubleIterator aLHS, final double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS % lhs));}
    
    @SuppressWarnings("Convert2MethodRef")
    public static void ebePlus2this_    (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs + rhs));}
    public static void ebeMinus2this_   (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs - rhs));}
    public static void ebeLMinus2this_  (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs - lhs));}
    public static void ebeMultiply2this_(IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs * rhs));}
    public static void ebeDiv2this_     (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs / rhs));}
    public static void ebeLDiv2this_    (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs / lhs));}
    public static void ebeMod2this_     (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs % rhs));}
    public static void ebeLMod2this_    (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs % lhs));}
    
    public static void mapPlus2this_    (IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (lhs + aRHS));}
    public static void mapMinus2this_   (IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (lhs - aRHS));}
    public static void mapLMinus2this_  (IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (aRHS - lhs));}
    public static void mapMultiply2this_(IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (lhs * aRHS));}
    public static void mapDiv2this_     (IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (lhs / aRHS));}
    public static void mapLDiv2this_    (IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (aRHS / lhs));}
    public static void mapMod2this_     (IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (lhs % aRHS));}
    public static void mapLMod2this_    (IDoubleSetIterator rThis, final double aRHS) {mapDo2this_(rThis, lhs -> (aRHS % lhs));}
    
    
    /** do stuff */
    public static void ebeDo2Dest_(IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest, IDoubleOperator2 aOpt) {
        while (rDest.hasNext()) rDest.nextAndSet(aOpt.cal(aLHS.next(), aRHS.next()));
    }
    public static void mapDo2Dest_(IDoubleIterator aLHS, IDoubleSetOnlyIterator rDest, IDoubleOperator1 aOpt) {
        while (rDest.hasNext()) rDest.nextAndSet(aOpt.cal(aLHS.next()));
    }
    public static void ebeDo2this_(IDoubleSetIterator rThis, IDoubleIterator aRHS, IDoubleOperator2 aOpt) {
        while (rThis.hasNext()) rThis.set(aOpt.cal(rThis.next(), aRHS.next()));
    }
    public static void mapDo2this_(IDoubleSetIterator rThis, IDoubleOperator1 aOpt) {
        while (rThis.hasNext()) rThis.set(aOpt.cal(rThis.next()));
    }
    
    public static void ebeDo2Dest_(IBooleanIterator aLHS, IBooleanIterator aRHS, IBooleanSetOnlyIterator rDest, IBooleanOperator2 aOpt) {
        while (rDest.hasNext()) rDest.nextAndSet(aOpt.cal(aLHS.next(), aRHS.next()));
    }
    public static void mapDo2Dest_(IBooleanIterator aLHS, IBooleanSetOnlyIterator rDest, IBooleanOperator1 aOpt) {
        while (rDest.hasNext()) rDest.nextAndSet(aOpt.cal(aLHS.next()));
    }
    public static void ebeDo2this_(IBooleanSetIterator rThis, IBooleanIterator aRHS, IBooleanOperator2 aOpt) {
        while (rThis.hasNext()) rThis.set(aOpt.cal(rThis.next(), aRHS.next()));
    }
    public static void mapDo2this_(IBooleanSetIterator rThis, IBooleanOperator1 aOpt) {
        while (rThis.hasNext()) rThis.set(aOpt.cal(rThis.next()));
    }
    
    
    public static void mapFill2this_(IDoubleSetOnlyIterator rThis, double aRHS) {
        while (rThis.hasNext()) rThis.nextAndSet(aRHS);
    }
    public static void ebeFill2this_(IDoubleSetOnlyIterator rThis, IDoubleIterator aRHS) {
        while (rThis.hasNext()) rThis.nextAndSet(aRHS.next());
    }
    
    public static void mapFill2this_(IBooleanSetOnlyIterator rThis, boolean aRHS) {
        while (rThis.hasNext()) rThis.nextAndSet(aRHS);
    }
    public static void ebeFill2this_(IBooleanSetOnlyIterator rThis, IBooleanIterator aRHS) {
        while (rThis.hasNext()) rThis.nextAndSet(aRHS.next());
    }
    
    
    /** stat stuff */
    public static double sumOfThis_(IDoubleIterator aThis) {
        double rSum = 0.0;
        while (aThis.hasNext()) rSum += aThis.next();
        return rSum;
    }
    public static double meanOfThis_(IDoubleIterator aThis) {
        double rSum = 0.0;
        double tNum = 0.0;
        while (aThis.hasNext()) {
            rSum += aThis.next();
            ++tNum;
        }
        return rSum / tNum;
    }
    public static double prodOfThis_(IDoubleIterator aThis) {
        double rProd = 1.0;
        while (aThis.hasNext()) rProd *= aThis.next();
        return rProd;
    }
    public static double maxOfThis_(IDoubleIterator aThis) {
        double rMax = Double.NaN;
        while (aThis.hasNext()) {
            double tValue = aThis.next();
            if (Double.isNaN(rMax) || tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static double minOfThis_(IDoubleIterator aThis) {
        double rMin = Double.NaN;
        while (aThis.hasNext()) {
            double tValue = aThis.next();
            if (Double.isNaN(rMin) || tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    public static double statOfThis_(IDoubleIterator aThis, IDoubleOperator2 aOpt) {
        double rStat = Double.NaN;
        while (aThis.hasNext()) rStat = aOpt.cal(rStat, aThis.next());
        return rStat;
    }
    
    public static void cumsum2Dest_(IDoubleIterator aThis, IDoubleSetOnlyIterator rDest) {
        double rSum = 0.0;
        while (aThis.hasNext()) {
            rSum += aThis.next();
            rDest.nextAndSet(rSum);
        }
    }
    public static void cummean2Dest_(IDoubleIterator aThis, IDoubleSetOnlyIterator rDest) {
        double rSum = 0.0;
        double tNum = 0.0;
        while (aThis.hasNext()) {
            rSum += aThis.next();
            ++tNum;
            rDest.nextAndSet(rSum / tNum);
        }
    }
    public static void cumprod2Dest_(IDoubleIterator aThis, IDoubleSetOnlyIterator rDest) {
        double rProd = 1.0;
        while (aThis.hasNext()) {
            rProd *= aThis.next();
            rDest.nextAndSet(rProd);
        }
    }
    public static void cummax2Dest_(IDoubleIterator aThis, IDoubleSetOnlyIterator rDest) {
        double rMax = Double.NaN;
        while (aThis.hasNext()) {
            double tValue = aThis.next();
            if (Double.isNaN(rMax) || tValue > rMax) rMax = tValue;
            rDest.nextAndSet(rMax);
        }
    }
    public static void cummin2Dest_(IDoubleIterator aThis, IDoubleSetOnlyIterator rDest) {
        double rMin = Double.NaN;
        while (aThis.hasNext()) {
            double tValue = aThis.next();
            if (Double.isNaN(rMin) || tValue < rMin) rMin = tValue;
            rDest.nextAndSet(rMin);
        }
    }
    public static void cumstat2Dest_(IDoubleIterator aThis, IDoubleSetOnlyIterator rDest, IDoubleOperator2 aOpt) {
        double rStat = Double.NaN;
        while (aThis.hasNext()) {
            rStat = aOpt.cal(rStat, aThis.next());
            rDest.nextAndSet(rStat);
        }
    }
}
