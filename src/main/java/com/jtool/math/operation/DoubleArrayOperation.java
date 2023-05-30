package com.jtool.math.operation;

import com.jtool.code.IHasLotIterator;
import com.jtool.code.operator.IOperator1;
import com.jtool.code.operator.IOperator2;
import com.jtool.math.IDataShell;


/**
 * 对于内部含有 double[] 的数据的运算做专门优化，方便编译器做 SIMD 的相关优化
 * @author liqa
 */
public abstract class DoubleArrayOperation<RS extends IHasLotIterator<? super T, Double> & IDataShell<double[]>, R extends RS, S extends RS, T> extends AbstractDataOperation<RS, R, S, T> {
    /** add, minus, multiply, divide stuffs */
    @Override protected void ebeAdd2Dest_(T aLHS, T aRHS, RS rDest) {
        final double[] tData1 = rDest.getIfHasSameOrderData(aLHS);
        final double[] tData2 = rDest.getIfHasSameOrderData(aRHS);
        if (tData1 != null && tData2 != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift1 = IDataShell.shiftSize(aLHS);
            final int tShift2 = IDataShell.shiftSize(aRHS);
            if (rShift == tShift1) {
                if (rShift == tShift2) for (int i = rShift; i < rEnd; ++i) rData[i] = tData1[i] + tData2[i];
                else for (int i = rShift, k = tShift2; i < rEnd; ++i, ++k) rData[i] = tData1[i] + tData2[k];
            } else {
                if (rShift == tShift2) for (int i = rShift, j = tShift1; i < rEnd; ++i, ++j) rData[i] = tData1[j] + tData2[i];
                else for (int i = rShift, j = tShift1, k = tShift2; i < rEnd; ++i, ++j, ++k) rData[i] = tData1[j] + tData2[k];
            }
        } else {
            super.ebeAdd2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void ebeMinus2Dest_(T aLHS, T aRHS, RS rDest) {
        final double[] tData1 = rDest.getIfHasSameOrderData(aLHS);
        final double[] tData2 = rDest.getIfHasSameOrderData(aRHS);
        if (tData1 != null && tData2 != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift1 = IDataShell.shiftSize(aLHS);
            final int tShift2 = IDataShell.shiftSize(aRHS);
            if (rShift == tShift1) {
                if (rShift == tShift2) for (int i = rShift; i < rEnd; ++i) rData[i] = tData1[i] - tData2[i];
                else for (int i = rShift, k = tShift2; i < rEnd; ++i, ++k) rData[i] = tData1[i] - tData2[k];
            } else {
                if (rShift == tShift2) for (int i = rShift, j = tShift1; i < rEnd; ++i, ++j) rData[i] = tData1[j] - tData2[i];
                else for (int i = rShift, j = tShift1, k = tShift2; i < rEnd; ++i, ++j, ++k) rData[i] = tData1[j] - tData2[k];
            }
        } else {
            super.ebeMinus2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void ebeMultiply2Dest_(T aLHS, T aRHS, RS rDest) {
        final double[] tData1 = rDest.getIfHasSameOrderData(aLHS);
        final double[] tData2 = rDest.getIfHasSameOrderData(aRHS);
        if (tData1 != null && tData2 != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift1 = IDataShell.shiftSize(aLHS);
            final int tShift2 = IDataShell.shiftSize(aRHS);
            if (rShift == tShift1) {
                if (rShift == tShift2) for (int i = rShift; i < rEnd; ++i) rData[i] = tData1[i] * tData2[i];
                else for (int i = rShift, k = tShift2; i < rEnd; ++i, ++k) rData[i] = tData1[i] * tData2[k];
            } else {
                if (rShift == tShift2) for (int i = rShift, j = tShift1; i < rEnd; ++i, ++j) rData[i] = tData1[j] * tData2[i];
                else for (int i = rShift, j = tShift1, k = tShift2; i < rEnd; ++i, ++j, ++k) rData[i] = tData1[j] * tData2[k];
            }
        } else {
            super.ebeMultiply2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void ebeDivide2Dest_(T aLHS, T aRHS, RS rDest) {
        final double[] tData1 = rDest.getIfHasSameOrderData(aLHS);
        final double[] tData2 = rDest.getIfHasSameOrderData(aRHS);
        if (tData1 != null && tData2 != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift1 = IDataShell.shiftSize(aLHS);
            final int tShift2 = IDataShell.shiftSize(aRHS);
            if (rShift == tShift1) {
                if (rShift == tShift2) for (int i = rShift; i < rEnd; ++i) rData[i] = tData1[i] / tData2[i];
                else for (int i = rShift, k = tShift2; i < rEnd; ++i, ++k) rData[i] = tData1[i] / tData2[k];
            } else {
                if (rShift == tShift2) for (int i = rShift, j = tShift1; i < rEnd; ++i, ++j) rData[i] = tData1[j] / tData2[i];
                else for (int i = rShift, j = tShift1, k = tShift2; i < rEnd; ++i, ++j, ++k) rData[i] = tData1[j] / tData2[k];
            }
        } else {
            super.ebeDivide2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void ebeMod2Dest_(T aLHS, T aRHS, RS rDest) {
        final double[] tData1 = rDest.getIfHasSameOrderData(aLHS);
        final double[] tData2 = rDest.getIfHasSameOrderData(aRHS);
        if (tData1 != null && tData2 != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift1 = IDataShell.shiftSize(aLHS);
            final int tShift2 = IDataShell.shiftSize(aRHS);
            if (rShift == tShift1) {
                if (rShift == tShift2) for (int i = rShift; i < rEnd; ++i) rData[i] = tData1[i] % tData2[i];
                else for (int i = rShift, k = tShift2; i < rEnd; ++i, ++k) rData[i] = tData1[i] % tData2[k];
            } else {
                if (rShift == tShift2) for (int i = rShift, j = tShift1; i < rEnd; ++i, ++j) rData[i] = tData1[j] % tData2[i];
                else for (int i = rShift, j = tShift1, k = tShift2; i < rEnd; ++i, ++j, ++k) rData[i] = tData1[j] % tData2[k];
            }
        } else {
            super.ebeMod2Dest_(aLHS, aRHS, rDest);
        }
    }
    
    
    @Override protected void mapAdd2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] + aRHS;
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] + aRHS;
        } else {
            super.mapAdd2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void mapMinus2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] - aRHS;
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] - aRHS;
        } else {
            super.mapMinus2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void mapLMinus2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = aRHS - tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = aRHS - tData[j];
        } else {
            super.mapLMinus2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void mapMultiply2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] * aRHS;
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] * aRHS;
        } else {
            super.mapMultiply2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void mapDivide2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] / aRHS;
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] / aRHS;
        } else {
            super.mapDivide2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void mapLDivide2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = aRHS / tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = aRHS / tData[j];
        } else {
            super.mapLDivide2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void mapMod2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] % aRHS;
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] % aRHS;
        } else {
            super.mapMod2Dest_(aLHS, aRHS, rDest);
        }
    }
    @Override protected void mapLMod2Dest_(T aLHS, double aRHS, RS rDest) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = aRHS % tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = aRHS % tData[j];
        } else {
            super.mapLMod2Dest_(aLHS, aRHS, rDest);
        }
    }
    
    
    @Override protected void ebeAdd2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] += tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] += tData[j];
        } else {
            super.ebeAdd2this_(rThis, aRHS);
        }
    }
    @Override protected void ebeMinus2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] -= tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] -= tData[j];
        } else {
            super.ebeMinus2this_(rThis, aRHS);
        }
    }
    @Override protected void ebeLMinus2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] - rData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] - rData[i];
        } else {
            super.ebeLMinus2this_(rThis, aRHS);
        }
    }
    @Override protected void ebeMultiply2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] *= tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] *= tData[j];
        } else {
            super.ebeMultiply2this_(rThis, aRHS);
        }
    }
    @Override protected void ebeDivide2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] /= tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] /= tData[j];
        } else {
            super.ebeDivide2this_(rThis, aRHS);
        }
    }
    @Override protected void ebeLDivide2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] / rData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] / rData[i];
        } else {
            super.ebeLDivide2this_(rThis, aRHS);
        }
    }
    @Override protected void ebeMod2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] %= tData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] %= tData[j];
        } else {
            super.ebeMod2this_(rThis, aRHS);
        }
    }
    @Override protected void ebeLMod2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = tData[i] % rData[i];
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = tData[j] % rData[i];
        } else {
            super.ebeLMod2this_(rThis, aRHS);
        }
    }
    
    
    @Override protected void mapAdd2this_       (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] += aRHS;}
    @Override protected void mapMinus2this_     (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] -= aRHS;}
    @Override protected void mapLMinus2this_    (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] = aRHS - rData[i];}
    @Override protected void mapMultiply2this_  (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] *= aRHS;}
    @Override protected void mapDivide2this_    (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] /= aRHS;}
    @Override protected void mapLDivide2this_   (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] = aRHS / rData[i];}
    @Override protected void mapMod2this_       (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] %= aRHS;}
    @Override protected void mapLMod2this_      (RS rThis, double aRHS) {final double[] rData = rThis.getData(); final int rShift = rThis.shiftSize(); final int rEnd = rThis.dataSize() + rShift; for (int i = rShift; i < rEnd; ++i) rData[i] = aRHS % rData[i];}
    
    
    
    /** do stuff */
    @Override protected void ebeDo2Dest_(T aLHS, T aRHS, RS rDest, IOperator2<Double> aOpt) {
        final double[] tData1 = rDest.getIfHasSameOrderData(aLHS);
        final double[] tData2 = rDest.getIfHasSameOrderData(aRHS);
        if (tData1 != null && tData2 != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift1 = IDataShell.shiftSize(aLHS);
            final int tShift2 = IDataShell.shiftSize(aRHS);
            if (rShift == tShift1) {
                if (rShift == tShift2) for (int i = rShift; i < rEnd; ++i) rData[i] = aOpt.cal(tData1[i], tData2[i]);
                else for (int i = rShift, k = tShift2; i < rEnd; ++i, ++k) rData[i] = aOpt.cal(tData1[i], tData2[k]);
            } else {
                if (rShift == tShift2) for (int i = rShift, j = tShift1; i < rEnd; ++i, ++j) rData[i] = aOpt.cal(tData1[j], tData2[i]);
                else for (int i = rShift, j = tShift1, k = tShift2; i < rEnd; ++i, ++j, ++k) rData[i] = aOpt.cal(tData1[j], tData2[k]);
            }
        } else {
            super.ebeDo2Dest_(aLHS, aRHS, rDest, aOpt);
        }
    }
    @Override protected void mapDo2Dest_(T aLHS, RS rDest, IOperator1<Double> aOpt) {
        final double[] tData = rDest.getIfHasSameOrderData(aLHS);
        if (tData != null) {
            final double[] rData = rDest.getData();
            final int rShift = rDest.shiftSize();
            final int rEnd = rDest.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aLHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = aOpt.cal(tData[i]);
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = aOpt.cal(tData[j]);
        } else {
            super.mapDo2Dest_(aLHS, rDest, aOpt);
        }
    }
    @Override protected void ebeDo2this_(RS rThis, T aRHS, IOperator2<Double> aOpt) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rEnd = rThis.dataSize() + rShift;
            final int tShift = IDataShell.shiftSize(aRHS);
            if (rShift == tShift) for (int i = rShift; i < rEnd; ++i) rData[i] = aOpt.cal(rData[i], tData[i]);
            else for (int i = rShift, j = tShift; i < rEnd; ++i, ++j) rData[i] = aOpt.cal(rData[i], tData[j]);
        } else {
            super.ebeDo2this_(rThis, aRHS, aOpt);
        }
    }
    @Override protected void mapDo2this_(RS rThis, IOperator1<Double> aOpt) {
        final double[] rData = rThis.getData();
        final int rShift = rThis.shiftSize();
        final int rEnd = rThis.dataSize() + rShift;
        for (int i = rShift; i < rEnd; ++i) rData[i] = aOpt.cal(rData[i]);
    }
    
    
    @Override protected void mapFill2this_(RS rThis, double aRHS) {
        final double[] rData = rThis.getData();
        final int rShift = rThis.shiftSize();
        final int rEnd = rThis.dataSize() + rShift;
        for (int i = rShift; i < rEnd; ++i) rData[i] = aRHS; // 注意在指定区域外不能填充，因此不能使用 Arrays.fill
    }
    @Override protected void ebeFill2this_(RS rThis, T aRHS) {
        final double[] tData = rThis.getIfHasSameOrderData(aRHS);
        if (tData != null) {
            System.arraycopy(tData, IDataShell.shiftSize(aRHS), rThis.getData(), rThis.shiftSize(), rThis.dataSize());
        } else {
            super.ebeFill2this_(rThis, aRHS);
        }
    }
    
    
    
    /** stat stuff */
    protected double sumOfThis_(RS tThis) {
        final double[] tData = tThis.getData();
        final int tShift = tThis.shiftSize();
        final int tEnd = tThis.dataSize() + tShift;
        
        double rSum = 0.0;
        for (int i = tShift; i < tEnd; ++i) rSum = tData[i];
        return rSum;
    }
    protected double meanOfThis_(RS tThis) {
        final double[] tData = tThis.getData();
        final int tShift = tThis.shiftSize();
        final int tSize = tThis.dataSize();
        final int tEnd = tSize + tShift;
        
        double rSum = 0.0;
        for (int i = tShift; i < tEnd; ++i) rSum = tData[i];
        return rSum / (double)tSize;
    }
}
