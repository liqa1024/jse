package com.jtool.math.operation;

import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.iterator.IDoubleSetOnlyIterator;
import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;
import org.jetbrains.annotations.ApiStatus;

/**
 * 对于运算操作的一般实现，主要用于减少重复代码；
 * 直接使用 {@link IDoubleIterator} 避免泛型的使用。
 * <p>
 * 由于传入都是迭代器，因此调用后输入的迭代器都会失效
 * @author liqa
 */
public class DATA {
    private DATA() {}
    
    
    /** add, minus, multiply, divide stuffs */
    @SuppressWarnings("Convert2MethodRef")
    @ApiStatus.Internal public static void ebePlus2Dest_    (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs + rhs));}
    @ApiStatus.Internal public static void ebeMinus2Dest_   (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs - rhs));}
    @ApiStatus.Internal public static void ebeMultiply2Dest_(IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs * rhs));}
    @ApiStatus.Internal public static void ebeDiv2Dest_     (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs / rhs));}
    @ApiStatus.Internal public static void ebeMod2Dest_     (IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest) {ebeDo2Dest_(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs % rhs));}
    
    @ApiStatus.Internal public static void mapPlus2Dest_    (IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs + aRHS));}
    @ApiStatus.Internal public static void mapMinus2Dest_   (IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs - aRHS));}
    @ApiStatus.Internal public static void mapLMinus2Dest_  (IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS - lhs));}
    @ApiStatus.Internal public static void mapMultiply2Dest_(IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs * aRHS));}
    @ApiStatus.Internal public static void mapDiv2Dest_     (IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs / aRHS));}
    @ApiStatus.Internal public static void mapLDiv2Dest_    (IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS / lhs));}
    @ApiStatus.Internal public static void mapMod2Dest_     (IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (lhs % aRHS));}
    @ApiStatus.Internal public static void mapLMod2Dest_    (IDoubleIterator aLHS, double aRHS, IDoubleSetOnlyIterator rDest) {mapDo2Dest_(aLHS, rDest, lhs -> (aRHS % lhs));}
    
    @SuppressWarnings("Convert2MethodRef")
    @ApiStatus.Internal public static void ebePlus2this_    (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs + rhs));}
    @ApiStatus.Internal public static void ebeMinus2this_   (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs - rhs));}
    @ApiStatus.Internal public static void ebeLMinus2this_  (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs - lhs));}
    @ApiStatus.Internal public static void ebeMultiply2this_(IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs * rhs));}
    @ApiStatus.Internal public static void ebeDiv2this_     (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs / rhs));}
    @ApiStatus.Internal public static void ebeLDiv2this_    (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs / lhs));}
    @ApiStatus.Internal public static void ebeMod2this_     (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (lhs % rhs));}
    @ApiStatus.Internal public static void ebeLMod2this_    (IDoubleSetIterator rThis, IDoubleIterator aRHS) {ebeDo2this_(rThis, aRHS, (lhs, rhs) -> (rhs % lhs));}
    
    @ApiStatus.Internal public static void mapPlus2this_    (IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs + aRHS));}
    @ApiStatus.Internal public static void mapMinus2this_   (IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs - aRHS));}
    @ApiStatus.Internal public static void mapLMinus2this_  (IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (aRHS - lhs));}
    @ApiStatus.Internal public static void mapMultiply2this_(IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs * aRHS));}
    @ApiStatus.Internal public static void mapDiv2this_     (IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs / aRHS));}
    @ApiStatus.Internal public static void mapLDiv2this_    (IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (aRHS / lhs));}
    @ApiStatus.Internal public static void mapMod2this_     (IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (lhs % aRHS));}
    @ApiStatus.Internal public static void mapLMod2this_    (IDoubleSetIterator rThis, double aRHS) {mapDo2this_(rThis, lhs -> (aRHS % lhs));}
    
    
    /** do stuff */
    @ApiStatus.Internal public static void ebeDo2Dest_(IDoubleIterator aLHS, IDoubleIterator aRHS, IDoubleSetOnlyIterator rDest, IDoubleOperator2 aOpt) {
        while (rDest.hasNext()) rDest.nextAndSet(aOpt.cal(aLHS.next(), aRHS.next()));
    }
    @ApiStatus.Internal public static void mapDo2Dest_(IDoubleIterator aLHS, IDoubleSetOnlyIterator rDest, IDoubleOperator1 aOpt) {
        while (rDest.hasNext()) rDest.nextAndSet(aOpt.cal(aLHS.next()));
    }
    @ApiStatus.Internal public static void ebeDo2this_(IDoubleSetIterator rThis, IDoubleIterator aRHS, IDoubleOperator2 aOpt) {
        while (rThis.hasNext()) rThis.set(aOpt.cal(rThis.next(), aRHS.next()));
    }
    @ApiStatus.Internal public static void mapDo2this_(IDoubleSetIterator rThis, IDoubleOperator1 aOpt) {
        while (rThis.hasNext()) rThis.set(aOpt.cal(rThis.next()));
    }
    
    @ApiStatus.Internal public static void mapFill2this_(IDoubleSetOnlyIterator rThis, double aRHS) {
        while (rThis.hasNext()) rThis.nextAndSet(aRHS);
    }
    @ApiStatus.Internal public static void ebeFill2this_(IDoubleSetOnlyIterator rThis, IDoubleIterator aRHS) {
        while (rThis.hasNext()) rThis.nextAndSet(aRHS.next());
    }
    
    
    /** stat stuff */
    @ApiStatus.Internal public static double sumOfThis_(IDoubleIterator tThis) {
        double rSum = 0.0;
        while (tThis.hasNext()) rSum += tThis.next();
        return rSum;
    }
    @ApiStatus.Internal public static double meanOfThis_(IDoubleIterator tThis) {
        double rSum = 0.0;
        double tNum = 0.0;
        while (tThis.hasNext()) {
            rSum += tThis.next();
            ++tNum;
        }
        return rSum / tNum;
    }
    @ApiStatus.Internal public static double productOfThis_(IDoubleIterator tThis) {
        double rProduct = 1.0;
        while (tThis.hasNext()) rProduct *= tThis.next();
        return rProduct;
    }
    @ApiStatus.Internal public static double maxOfThis_(IDoubleIterator tThis) {
        double rMax = Double.NEGATIVE_INFINITY;
        while (tThis.hasNext()) {
            double tValue = tThis.next();
            if (tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    @ApiStatus.Internal public static double minOfThis_(IDoubleIterator tThis) {
        double rMin = Double.POSITIVE_INFINITY;
        while (tThis.hasNext()) {
            double tValue = tThis.next();
            if (tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
}
