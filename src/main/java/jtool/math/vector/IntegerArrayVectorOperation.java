package jtool.math.vector;

import jtool.code.functional.IIntegerOperator1;
import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

import java.util.Arrays;
import java.util.Comparator;

public abstract class IntegerArrayVectorOperation extends AbstractIntegerVectorOperation {
    @Override public void fill              (int aRHS) {IntegerArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill              (IIntegerVector aRHS) {
        final IntegerArrayVector rThis = thisVector_();
        int[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    
    /** 排序不自己实现 */
    @Override public void sort() {
        final IntegerArrayVector rThis = thisVector_();
        final int rShift = rThis.internalDataShift();
        final int tEnd = rThis.internalDataSize() + rShift;
        Arrays.sort(rThis.internalData(), rShift, tEnd);
    }
    @Override public void sort(Comparator<? super Integer> aComp) {
        final IntegerArrayVector rThis = thisVector_();
        final int[] rData = rThis.internalData();
        final int rShift = rThis.internalDataShift();
        final int tSize = rThis.internalDataSize();
        // TODO: 对于这个特殊情况似乎还是得自己实现一下
        Integer[] tObjData = new Integer[tSize];
        for (int i = 0, j = rShift; i < tSize; ++i, ++j) {
            tObjData[i] = rData[j];
        }
        Arrays.sort(tObjData, aComp);
        for (int i = 0, j = rShift; i < tSize; ++i, ++j) {
            rData[j] = tObjData[i];
        }
    }
    
    @Override public void shuffle(IIntegerOperator1 aRng) {
        final IntegerArrayVector rThis = thisVector_();
        final int[] rData = rThis.internalData();
        final int rShift = rThis.internalDataShift();
        final int tEnd = rThis.internalDataSize() + rShift;
        final int rShiftPP = rShift+1;
        for (int i = tEnd; i > rShiftPP; --i) {
            swap(rData, i-1, aRng.cal(i));
        }
    }
    
    static void swap(int[] rData, int i, int j) {
        int oValueJ = rData[j];
        rData[j] = rData[i];
        rData[i] = oValueJ;
    }
    
    /** stuff to override */
    @Override protected abstract IntegerArrayVector thisVector_();
}
