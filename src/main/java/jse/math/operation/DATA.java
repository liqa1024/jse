package jse.math.operation;

import com.mastfrog.util.sort.Sort;
import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import groovy.transform.stc.SimpleType;
import jse.code.functional.*;
import jse.code.iterator.*;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.vector.*;

import java.util.function.*;

import static jse.code.UT.Code.toComplexDouble;

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
        rDest.assign(() -> aOpt.apply(li.next(), ri.next()));
    }
    
    public static void mapEqual2Dest            (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs == aRHS));}
    public static void mapGreater2Dest          (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs >  aRHS));}
    public static void mapGreaterOrEqual2Dest   (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs >= aRHS));}
    public static void mapLess2Dest             (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs <  aRHS));}
    public static void mapLessOrEqual2Dest      (IHasDoubleIterator aLHS, final double aRHS, IHasBooleanSetOnlyIterator rDest) {mapCheck2Dest(aLHS, rDest, lhs -> (lhs <= aRHS));}
    public static void mapCheck2Dest            (IHasDoubleIterator aData, IHasBooleanSetOnlyIterator rDest, final IChecker aOpt) {
        final IDoubleIterator it = aData.iterator();
        rDest.assign(() -> aOpt.apply(it.next()));
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
    
    /** int stuffs */
    @SuppressWarnings("Convert2MethodRef")
    public static void ebePlus2Dest     (IHasIntIterator aLHS, IHasIntIterator aRHS, IHasIntSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs + rhs));}
    public static void ebeMinus2Dest    (IHasIntIterator aLHS, IHasIntIterator aRHS, IHasIntSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs - rhs));}
    public static void ebeMultiply2Dest (IHasIntIterator aLHS, IHasIntIterator aRHS, IHasIntSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs * rhs));}
    public static void ebeDiv2Dest      (IHasIntIterator aLHS, IHasIntIterator aRHS, IHasIntSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs / rhs));}
    public static void ebeMod2Dest      (IHasIntIterator aLHS, IHasIntIterator aRHS, IHasIntSetOnlyIterator rDest) {ebeDo2Dest(aLHS, aRHS, rDest, (lhs, rhs) -> (lhs % rhs));}
    
    public static void mapPlus2Dest     (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs + aRHS));}
    public static void mapMinus2Dest    (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs - aRHS));}
    public static void mapLMinus2Dest   (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (aRHS - lhs));}
    public static void mapMultiply2Dest (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs * aRHS));}
    public static void mapDiv2Dest      (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs / aRHS));}
    public static void mapLDiv2Dest     (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (aRHS / lhs));}
    public static void mapMod2Dest      (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (lhs % aRHS));}
    public static void mapLMod2Dest     (IHasIntIterator aLHS, final int aRHS, IHasIntSetOnlyIterator rDest) {mapDo2Dest(aLHS, rDest, lhs -> (aRHS % lhs));}
    
    @SuppressWarnings("Convert2MethodRef")
    public static void ebePlus2This     (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs + rhs));}
    public static void ebeMinus2This    (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs - rhs));}
    public static void ebeLMinus2This   (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (rhs - lhs));}
    public static void ebeMultiply2This (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs * rhs));}
    public static void ebeDiv2This      (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs / rhs));}
    public static void ebeLDiv2This     (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (rhs / lhs));}
    public static void ebeMod2This      (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (lhs % rhs));}
    public static void ebeLMod2This     (IHasIntSetIterator rThis, IHasIntIterator aRHS) {ebeDo2This(rThis, aRHS, (lhs, rhs) -> (rhs % lhs));}
    
    public static void mapPlus2This     (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (lhs + aRHS));}
    public static void mapMinus2This    (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (lhs - aRHS));}
    public static void mapLMinus2This   (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (aRHS - lhs));}
    public static void mapMultiply2This (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (lhs * aRHS));}
    public static void mapDiv2This      (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (lhs / aRHS));}
    public static void mapLDiv2This     (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (aRHS / lhs));}
    public static void mapMod2This      (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (lhs % aRHS));}
    public static void mapLMod2This     (IHasIntSetIterator rThis, final int aRHS) {mapDo2This(rThis, lhs -> (aRHS % lhs));}
    
    /** do stuff */
    public static void ebeDo2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest, final IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.apply(li.next(), ri.next()));
    }
    public static void ebeDo2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest, final IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.apply(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleSetOnlyIterator rDest, final IUnaryFullOperator<? extends IComplexDouble, Double> aOpt) {
        final IDoubleIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.apply(it.next()));
    }
    public static void mapDo2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleSetOnlyIterator rDest, final IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.apply(it.next()));
    }
    public static void ebeDo2This(IHasComplexDoubleSetIterator rThis, IHasDoubleIterator aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.apply(si.next(), ri.next()));
    }
    public static void ebeDo2This(IHasComplexDoubleSetIterator rThis, IHasComplexDoubleIterator aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.apply(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasComplexDoubleSetIterator rThis, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.apply(si.next()));
    }
    
    public static void ebeDo2Dest(IHasDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasDoubleSetOnlyIterator rDest, final DoubleBinaryOperator aOpt) {
        final IDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.applyAsDouble(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasDoubleIterator aLHS, IHasDoubleSetOnlyIterator rDest, final DoubleUnaryOperator aOpt) {
        final IDoubleIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.applyAsDouble(it.next()));
    }
    public static void ebeDo2This(IHasDoubleSetIterator rThis, IHasDoubleIterator aRHS, DoubleBinaryOperator aOpt) {
        final IDoubleIterator ri = aRHS.iterator();
        final IDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.applyAsDouble(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasDoubleSetIterator rThis, DoubleUnaryOperator aOpt) {
        final IDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.applyAsDouble(si.next()));
    }
    
    public static void ebeDo2Dest(IHasBooleanIterator aLHS, IHasBooleanIterator aRHS, IHasBooleanSetOnlyIterator rDest, final IBooleanBinaryOperator aOpt) {
        final IBooleanIterator li = aLHS.iterator();
        final IBooleanIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.applyAsBoolean(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasBooleanIterator aLHS, IHasBooleanSetOnlyIterator rDest, final IBooleanUnaryOperator aOpt) {
        final IBooleanIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.applyAsBoolean(it.next()));
    }
    public static void ebeDo2This(IHasBooleanSetIterator rThis, IHasBooleanIterator aRHS, IBooleanBinaryOperator aOpt) {
        final IBooleanIterator ri = aRHS.iterator();
        final IBooleanSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.applyAsBoolean(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasBooleanSetIterator rThis, IBooleanUnaryOperator aOpt) {
        final IBooleanSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.applyAsBoolean(si.next()));
    }
    
    public static void ebeDo2Dest(IHasIntIterator aLHS, IHasIntIterator aRHS, IHasIntSetOnlyIterator rDest, final IntBinaryOperator aOpt) {
        final IIntIterator li = aLHS.iterator();
        final IIntIterator ri = aRHS.iterator();
        rDest.assign(() -> aOpt.applyAsInt(li.next(), ri.next()));
    }
    public static void mapDo2Dest(IHasIntIterator aLHS, IHasIntSetOnlyIterator rDest, final IntUnaryOperator aOpt) {
        final IIntIterator it = aLHS.iterator();
        rDest.assign(() -> aOpt.applyAsInt(it.next()));
    }
    public static void ebeDo2This(IHasIntSetIterator rThis, IHasIntIterator aRHS, IntBinaryOperator aOpt) {
        final IIntIterator ri = aRHS.iterator();
        final IIntSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.applyAsInt(si.next(), ri.next()));
    }
    public static void mapDo2This(IHasIntSetIterator rThis, IntUnaryOperator aOpt) {
        final IIntSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(aOpt.applyAsInt(si.next()));
    }
    
    /** negative stuffs */
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
            si.nextAndSet(-it.real(), -it.imag());
        }
    }
    public static void mapNegative2This(IHasComplexDoubleSetIterator rThis) {
        final IComplexDoubleSetIterator si = rThis.setIterator();
        while (si.hasNext()) {
            si.nextOnly();
            si.set(-si.real(), -si.imag());
        }
    }
    public static void mapNegative2Dest(IHasIntIterator aData, IHasIntSetOnlyIterator rDest) {
        final IIntIterator it = aData.iterator();
        rDest.assign(() -> -it.next());
    }
    public static void mapNegative2This(IHasIntSetIterator rThis) {
        final IIntSetIterator si = rThis.setIterator();
        while (si.hasNext()) si.set(-si.next());
    }
    
    
    /** complex double stuffs */
    public static void ebePlus2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() + ri.next(), li.imag());
        }
    }
    public static void ebeMinus2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() - ri.next(), li.imag());
        }
    }
    public static void ebeMultiply2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            double tRHS = ri.next();
            si.nextAndSet(li.real() * tRHS, li.imag() * tRHS);
        }
    }
    public static void ebeDiv2Dest(IHasComplexDoubleIterator aLHS, IHasDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            double tRHS = ri.next();
            si.nextAndSet(li.real() / tRHS, li.imag() / tRHS);
        }
    }
    public static void ebePlus2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextAndSet(li.next() + ri.real(), ri.imag());
        }
    }
    public static void ebeMinus2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            si.nextAndSet(li.next() - ri.real(), -ri.imag());
        }
    }
    public static void ebeMultiply2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            double tLHS = li.next();
            si.nextAndSet(tLHS * ri.real(), tLHS * ri.imag());
        }
    }
    public static void ebeDiv2Dest(IHasDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            ri.nextOnly();
            double tLHS = li.next();
            double rReal = ri.real(), rImag = ri.imag();
            double div = rReal*rReal + rImag*rImag;
            si.nextAndSet((tLHS*rReal)/div, (-tLHS*rImag)/div);
        }
    }
    public static void ebePlus2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            si.nextAndSet(li.real() + ri.real(), li.imag() + ri.imag());
        }
    }
    public static void ebeMinus2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            si.nextAndSet(li.real() - ri.real(), li.imag() - ri.imag());
        }
    }
    public static void ebeMultiply2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double rReal = ri.real(), rImag = ri.imag();
            si.nextAndSet(lReal*rReal - lImag*rImag, lImag*rReal + lReal*rImag);
        }
    }
    public static void ebeDiv2Dest(IHasComplexDoubleIterator aLHS, IHasComplexDoubleIterator aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double rReal = ri.real(), rImag = ri.imag();
            double div = rReal*rReal + rImag*rImag;
            si.nextAndSet((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
        }
    }
    
    public static void mapPlus2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() + aRHS, li.imag());
        }
    }
    public static void mapMinus2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() - aRHS, li.imag());
        }
    }
    public static void mapLMinus2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(aRHS - li.real(), -li.imag());
        }
    }
    public static void mapMultiply2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() * aRHS, li.imag() * aRHS);
        }
    }
    public static void mapDiv2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() / aRHS, li.imag() / aRHS);
        }
    }
    public static void mapLDiv2Dest(IHasComplexDoubleIterator aLHS, double aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        while (si.hasNext()) {
            li.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double div = lReal*lReal + lImag*lImag;
            si.nextAndSet((aRHS*lReal)/div, (-aRHS*lImag)/div);
        }
    }
    public static void mapPlus2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextAndSet(li.next() + rReal, rImag);
        }
    }
    public static void mapMinus2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextAndSet(li.next() - rReal, -rImag);
        }
    }
    public static void mapLMinus2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextAndSet(rReal - li.next(), rImag);
        }
    }
    public static void mapMultiply2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            double tLHS = li.next();
            si.nextAndSet(tLHS * rReal, tLHS * rImag);
        }
    }
    public static void mapDiv2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        final double div = rReal*rReal + rImag*rImag;
        while (si.hasNext()) {
            double tLHS = li.next();
            si.nextAndSet((tLHS*rReal)/div, (-tLHS*rImag)/div);
        }
    }
    public static void mapLDiv2Dest(IHasDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            double tLHS = li.next();
            si.nextAndSet(rReal / tLHS, rImag / tLHS);
        }
    }
    public static void mapPlus2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() + rReal, li.imag() + rImag);
        }
    }
    public static void mapMinus2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(li.real() - rReal, li.imag() - rImag);
        }
    }
    public static void mapLMinus2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            si.nextAndSet(rReal - li.real(), rImag - li.imag());
        }
    }
    public static void mapMultiply2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            si.nextAndSet(lReal*rReal - lImag*rImag, lImag*rReal + lReal*rImag);
        }
    }
    public static void mapDiv2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        final double div = rReal*rReal + rImag*rImag;
        while (si.hasNext()) {
            li.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            si.nextAndSet((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
        }
    }
    public static void mapLDiv2Dest(IHasComplexDoubleIterator aLHS, IComplexDouble aRHS, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleIterator li = aLHS.iterator();
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            li.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double div = lReal*lReal + lImag*lImag;
            si.nextAndSet((rReal*lReal + rImag*lImag)/div, (rImag*lReal - rReal*lImag)/div);
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
    
    
    /** fill, forEach, assign stuff */
    public static void mapFill2This(IHasComplexDoubleSetOnlyIterator rThis, IComplexDouble aRHS) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        final double rReal = aRHS.real(), rImag = aRHS.imag();
        while (si.hasNext()) {
            si.nextAndSet(rReal, rImag);
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
            si.nextAndSet(it.real(), it.imag());
        }
    }
    public static void ebeFill2This(IHasComplexDoubleSetOnlyIterator rThis, IHasDoubleIterator aRHS) {
        final IDoubleIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void vecFill2This(IHasComplexDoubleSetOnlyIterator rThis, IComplexVectorGetter aVec) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        for (int i = 0; si.hasNext(); ++i) si.nextAndSet(aVec.get(i));
    }
    public static void vecFill2This(IHasComplexDoubleSetOnlyIterator rThis, IVectorGetter aVec) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        for (int i = 0; si.hasNext(); ++i) si.nextAndSet(aVec.get(i));
    }
    public static void assign2This(IHasComplexDoubleSetOnlyIterator rThis, Supplier<? extends IComplexDouble> aSup) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
    public static void assign2This(IHasComplexDoubleSetOnlyIterator rThis, DoubleSupplier aSup) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsDouble());
    }
    public static void forEachOfThis(IHasComplexDoubleIterator aThis, Consumer<? super ComplexDouble> aCon) {
        final IComplexDoubleIterator it = aThis.iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
    public static void forEachOfThis(IHasComplexDoubleIterator aThis, IDoubleBinaryConsumer aCon) {
        final IComplexDoubleIterator it = aThis.iterator();
        while (it.hasNext()) {
            it.nextOnly();
            aCon.accept(it.real(), it.imag());
        }
    }
    /** Groovy stuffs */
    public static void vecFill2This(IHasComplexDoubleSetOnlyIterator rThis, @ClosureParams(value=SimpleType.class, options="int") Closure<?> aGroovyTask) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        for (int i = 0; si.hasNext(); ++i) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = aGroovyTask.call(i);
            if (tObj instanceof IComplexDouble) si.nextAndSet((IComplexDouble)tObj);
            else if (tObj instanceof Number) si.nextAndSet(((Number)tObj).doubleValue());
            else si.nextAndSet(Double.NaN);
        }
    }
    public static void assign2This(IHasComplexDoubleSetOnlyIterator rThis, Closure<?> aGroovyTask) {
        final IComplexDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = aGroovyTask.call();
            if (tObj instanceof IComplexDouble) si.nextAndSet((IComplexDouble)tObj);
            else if (tObj instanceof Number) si.nextAndSet(((Number)tObj).doubleValue());
            else si.nextAndSet(Double.NaN);
        }
    }
    public static void forEachOfThis(IHasComplexDoubleIterator aThis, @ClosureParams(value=FromString.class, options={"ComplexDouble", "double,double"}) Closure<?> aGroovyTask) {
        if (aGroovyTask.getMaximumNumberOfParameters() == 2) {
            forEachOfThis(aThis, (real, imag) -> aGroovyTask.call(real, imag));
            return;
        }
        forEachOfThis(aThis, value -> aGroovyTask.call(value));
    }
    
    public static void mapFill2This(IHasDoubleSetOnlyIterator rThis, final double aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasDoubleSetOnlyIterator rThis, IHasDoubleIterator aRHS) {
        final IDoubleIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void vecFill2This(IHasDoubleSetOnlyIterator rThis, IVectorGetter aVec) {
        final IDoubleSetOnlyIterator si = rThis.setIterator();
        for (int i = 0; si.hasNext(); ++i) si.nextAndSet(aVec.get(i));
    }
    public static void assign2This(IHasDoubleSetOnlyIterator rThis, DoubleSupplier aSup) {
        final IDoubleSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsDouble());
    }
    public static void forEachOfThis(IHasDoubleIterator aThis, DoubleConsumer aCon) {
        final IDoubleIterator it = aThis.iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
    
    public static void mapFill2This(IHasBooleanSetOnlyIterator rThis, final boolean aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasBooleanSetOnlyIterator rThis, IHasBooleanIterator aRHS) {
        final IBooleanIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void vecFill2This(IHasBooleanSetOnlyIterator rThis, ILogicalVectorGetter aVec) {
        final IBooleanSetOnlyIterator si = rThis.setIterator();
        for (int i = 0; si.hasNext(); ++i) si.nextAndSet(aVec.get(i));
    }
    public static void assign2This(IHasBooleanSetOnlyIterator rThis, BooleanSupplier aSup) {
        final IBooleanSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsBoolean());
    }
    public static void forEachOfThis(IHasBooleanIterator aThis, IBooleanConsumer aCon) {
        final IBooleanIterator it = aThis.iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
    
    public static void mapFill2This(IHasIntSetOnlyIterator rThis, final int aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasIntSetOnlyIterator rThis, IHasIntIterator aRHS) {
        final IIntIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void vecFill2This(IHasIntSetOnlyIterator rThis, IIntVectorGetter aVec) {
        final IIntSetOnlyIterator si = rThis.setIterator();
        for (int i = 0; si.hasNext(); ++i) si.nextAndSet(aVec.get(i));
    }
    public static void assign2This(IHasIntSetOnlyIterator rThis, IntSupplier aSup) {
        final IIntSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsInt());
    }
    public static void forEachOfThis(IHasIntIterator aThis, IntConsumer aCon) {
        final IIntIterator it = aThis.iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
    
    public static void mapFill2This(IHasLongSetOnlyIterator rThis, final long aRHS) {
        rThis.assign(() -> aRHS);
    }
    public static void ebeFill2This(IHasLongSetOnlyIterator rThis, IHasLongIterator aRHS) {
        final ILongIterator it = aRHS.iterator();
        rThis.assign(it::next);
    }
    /** 注意这几个方法不能替换成通用遍历方法，会造成无限递归 */
    public static void vecFill2This(IHasLongSetOnlyIterator rThis, ILongVectorGetter aVec) {
        final ILongSetOnlyIterator si = rThis.setIterator();
        for (int i = 0; si.hasNext(); ++i) si.nextAndSet(aVec.get(i));
    }
    public static void assign2This(IHasLongSetOnlyIterator rThis, LongSupplier aSup) {
        final ILongSetOnlyIterator si = rThis.setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsLong());
    }
    public static void forEachOfThis(IHasLongIterator aThis, LongConsumer aCon) {
        final ILongIterator it = aThis.iterator();
        while (it.hasNext()) aCon.accept(it.next());
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
    public static int sumOfThis(IHasIntIterator aThis) {
        final IIntIterator it = aThis.iterator();
        int rSum = 0;
        while (it.hasNext()) rSum += it.next();
        return rSum;
    }
    public static long exsumOfThis(IHasIntIterator aThis) {
        final IIntIterator it = aThis.iterator();
        long rSum = 0;
        while (it.hasNext()) rSum += it.next();
        return rSum;
    }
    public static long sumOfThis(IHasLongIterator aThis) {
        final ILongIterator it = aThis.iterator();
        long rSum = 0;
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
    public static int maxOfThis(IHasIntIterator aThis) {
        final IIntIterator it = aThis.iterator();
        int rMax = it.next();
        while (it.hasNext()) {
            int tValue = it.next();
            if (tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static int minOfThis(IHasIntIterator aThis) {
        final IIntIterator it = aThis.iterator();
        int rMin = it.next();
        while (it.hasNext()) {
            int tValue = it.next();
            if (tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    public static long maxOfThis(IHasLongIterator aThis) {
        final ILongIterator it = aThis.iterator();
        long rMax = it.next();
        while (it.hasNext()) {
            long tValue = it.next();
            if (tValue > rMax) rMax = tValue;
        }
        return rMax;
    }
    public static long minOfThis(IHasLongIterator aThis) {
        final ILongIterator it = aThis.iterator();
        long rMin = it.next();
        while (it.hasNext()) {
            long tValue = it.next();
            if (tValue < rMin) rMin = tValue;
        }
        return rMin;
    }
    public static double statOfThis(IHasDoubleIterator aThis, DoubleBinaryOperator aOpt) {
        final IDoubleIterator it = aThis.iterator();
        double rStat = Double.NaN;
        while (it.hasNext()) rStat = aOpt.applyAsDouble(rStat, it.next());
        return rStat;
    }
    public static ComplexDouble statOfThis(IHasComplexDoubleIterator aThis, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rStat = null;
        while (it.hasNext()) rStat = toComplexDouble(aOpt.apply(rStat, it.next()));
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
            rSum.plus2this(it);
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
    public static void cummean2Dest(IHasComplexDoubleIterator aThis, IHasComplexDoubleSetOnlyIterator rDest) {
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rSum = new ComplexDouble();
        double tNum = 0.0;
        while (it.hasNext()) {
            it.nextOnly();
            rSum.plus2this(it);
            ++tNum;
            si.nextAndSet(rSum.mReal / tNum, rSum.mImag / tNum);
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
            rProd.multiply2this(it);
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
    public static void cumstat2Dest(IHasDoubleIterator aThis, IHasDoubleSetOnlyIterator rDest, DoubleBinaryOperator aOpt) {
        final IDoubleSetOnlyIterator si = rDest.setIterator();
        final IDoubleIterator it = aThis.iterator();
        double rStat = Double.NaN;
        while (it.hasNext()) {
            rStat = aOpt.applyAsDouble(rStat, it.next());
            si.nextAndSet(rStat);
        }
    }
    public static void cumstat2Dest(IHasComplexDoubleIterator aThis, IHasComplexDoubleSetOnlyIterator rDest, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        final IComplexDoubleSetOnlyIterator si = rDest.setIterator();
        final IComplexDoubleIterator it = aThis.iterator();
        ComplexDouble rStat = null;
        while (it.hasNext()) {
            rStat = toComplexDouble(aOpt.apply(rStat, it.next()));
            si.nextAndSet(rStat);
        }
    }
    
    
    /** 排序会用到的算法，这里不自己实现 */
    public static void reverse2Dest(IHasDoubleIterator aThis, IVector rDest) {
        final int tSize = rDest.size();
        final IDoubleIterator it = aThis.iterator();
        for (int i = tSize-1; i >= 0; --i) {
            rDest.set(i, it.next());
        }
    }
    public static void reverse2Dest(IHasComplexDoubleIterator aThis, IComplexVector rDest) {
        final int tSize = rDest.size();
        final IComplexDoubleIterator it = aThis.iterator();
        for (int i = tSize-1; i >= 0; --i) {
            it.nextOnly();
            rDest.set(i, it);
        }
    }
    public static void reverse2Dest(IHasBooleanIterator aThis, ILogicalVector rDest) {
        final int tSize = rDest.size();
        final IBooleanIterator it = aThis.iterator();
        for (int i = tSize-1; i >= 0; --i) {
            rDest.set(i, it.next());
        }
    }
    public static void reverse2Dest(IHasIntIterator aThis, IIntVector rDest) {
        final int tSize = rDest.size();
        final IIntIterator it = aThis.iterator();
        for (int i = tSize-1; i >= 0; --i) {
            rDest.set(i, it.next());
        }
    }
    public static void reverse2Dest(IHasLongIterator aThis, ILongVector rDest) {
        final int tSize = rDest.size();
        final ILongIterator it = aThis.iterator();
        for (int i = tSize-1; i >= 0; --i) {
            rDest.set(i, it.next());
        }
    }
    public static void reverse2This(IVector rThis) {
        reverse2This(rThis, rThis.size());
    }
    public static void reverse2This(IComplexVector rThis) {
        reverse2This(rThis, rThis.size());
    }
    public static void reverse2This(ILogicalVector rThis) {
        reverse2This(rThis, rThis.size());
    }
    public static void reverse2This(IIntVector rThis) {
        reverse2This(rThis, rThis.size());
    }
    public static void reverse2This(ILongVector rThis) {
        reverse2This(rThis, rThis.size());
    }
    public static void reverse2This(ISwapper rThis, int aSize) {
        for (int i = 0, j = aSize-1; i < j; ++i, --j) {
            rThis.swap(i, j);
        }
    }
    public static void sort(final IVector rVec) {
        Sort.sortAdhoc(rVec, rVec.size(), (i, j) -> Double.compare(rVec.get(i), rVec.get(j)));
    }
    public static void sort(final IIntVector rVec) {
        Sort.sortAdhoc(rVec, rVec.size(), (i, j) -> Integer.compare(rVec.get(i), rVec.get(j)));
    }
    public static void sort(final ILongVector rVec) {
        Sort.sortAdhoc(rVec, rVec.size(), (i, j) -> Long.compare(rVec.get(i), rVec.get(j)));
    }
    public static void sort(IVector rVec, IntBinaryOperator aComp) {
        Sort.sortAdhoc(rVec, rVec.size(), aComp);
    }
    public static void sort(IIntVector rVec, IntBinaryOperator aComp) {
        Sort.sortAdhoc(rVec, rVec.size(), aComp);
    }
    public static void sort(ILongVector rVec, IntBinaryOperator aComp) {
        Sort.sortAdhoc(rVec, rVec.size(), aComp);
    }
    public static void biSort(final IVector rVec, ISwapper aSwapper) {
        Sort.sortAdhoc(rVec.merge(aSwapper), rVec.size(), (i, j) -> Double.compare(rVec.get(i), rVec.get(j)));
    }
    public static void biSort(final IIntVector rVec, ISwapper aSwapper) {
        Sort.sortAdhoc(rVec.merge(aSwapper), rVec.size(), (i, j) -> Integer.compare(rVec.get(i), rVec.get(j)));
    }
    public static void biSort(final ILongVector rVec, ISwapper aSwapper) {
        Sort.sortAdhoc(rVec.merge(aSwapper), rVec.size(), (i, j) -> Long.compare(rVec.get(i), rVec.get(j)));
    }
    public static void biSort(final IVector rVec, ISwapper aSwapper, IntBinaryOperator aComp) {
        Sort.sortAdhoc(rVec.merge(aSwapper), rVec.size(), aComp);
    }
    public static void biSort(final IIntVector rVec, ISwapper aSwapper, IntBinaryOperator aComp) {
        Sort.sortAdhoc(rVec.merge(aSwapper), rVec.size(), aComp);
    }
    public static void biSort(final ILongVector rVec, ISwapper aSwapper, IntBinaryOperator aComp) {
        Sort.sortAdhoc(rVec.merge(aSwapper), rVec.size(), aComp);
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
