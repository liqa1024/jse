package com.jtool.math.function;


import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;
import com.jtool.code.operator.IDoubleOperator3;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.vector.IVectorSetter;

/**
 * 针对包含 double[] 的函数的运算，这里更进一步是针对 {@link DoubleArrayFunc1} 的运算。
 * @author liqa
 */
public abstract class DoubleArrayFunc1Operation implements IFunc1Operation {
    /** 通用的一些运算 */
    @Override public IFunc1 ebePlus(IFunc1Subs aLHS, IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebePlus2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) {
                double tX = rFunc1.getX(i);
                rFunc1.set_(i, aLHS.subs(tX) + aRHS.subs(tX));
            }
        }
        return rFunc1;
    }
    @Override public IFunc1 ebeMinus(IFunc1Subs aLHS, IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) {
                double tX = rFunc1.getX(i);
                rFunc1.set_(i, aLHS.subs(tX) - aRHS.subs(tX));
            }
        }
        return rFunc1;
    }
    @Override public IFunc1 ebeMultiply(IFunc1Subs aLHS, IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMultiply2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) {
                double tX = rFunc1.getX(i);
                rFunc1.set_(i, aLHS.subs(tX) * aRHS.subs(tX));
            }
        }
        return rFunc1;
    }
    @Override public IFunc1 ebeDiv(IFunc1Subs aLHS, IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) {
                double tX = rFunc1.getX(i);
                rFunc1.set_(i, aLHS.subs(tX) / aRHS.subs(tX));
            }
        }
        return rFunc1;
    }
    @Override public IFunc1 ebeMod(IFunc1Subs aLHS, IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) {
                double tX = rFunc1.getX(i);
                rFunc1.set_(i, aLHS.subs(tX) % aRHS.subs(tX));
            }
        }
        return rFunc1;
    }
    @Override public IFunc1 ebeDo(IFunc1Subs aLHS, IFunc1Subs aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize(), aOpt);
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) {
                double tX = rFunc1.getX(i);
                rFunc1.set_(i, aOpt.cal(aLHS.subs(tX), aRHS.subs(tX)));
            }
        }
        return rFunc1;
    }
    
    @Override public IFunc1 mapPlus(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapPlus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aLHS.subs(rFunc1.getX(i)) + aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 mapMinus(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aLHS.subs(rFunc1.getX(i)) - aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 mapLMinus(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapLMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS - aLHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 mapMultiply(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapMultiply2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aLHS.subs(rFunc1.getX(i)) * aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 mapDiv(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aLHS.subs(rFunc1.getX(i)) / aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 mapLDiv(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapLDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS / aLHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 mapMod(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aLHS.subs(rFunc1.getX(i)) % aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 mapLMod(IFunc1Subs aLHS, double aRHS) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapLMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS % aLHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 mapDo(IFunc1Subs aLHS, IDoubleOperator1 aOpt) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        double[] tDataL = rFunc1.getIfHasSameOrderData(aLHS);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize(), aOpt);
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(aLHS.subs(rFunc1.getX(i))));
        }
        return rFunc1;
    }
    
    @Override public void ebePlus2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebePlus2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构
            final int tNx = rFunc1.Nx();
            final int tStart, tEnd;
            if (aRHS instanceof IZeroBoundFunc1) {
                // 对于零边界的特殊优化，只需要运算一部分
                IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
                tStart = Math.max((int)Math.floor((tRHS.zeroBoundL() - rFunc1.x0())/rFunc1.dx()), 0);
                tEnd = Math.min((int)Math.ceil((tRHS.zeroBoundR() - rFunc1.x0())/rFunc1.dx()) + 1, tNx);
            } else {
                tStart = 0; tEnd = tNx;
            }
            for (int i = tStart; i < tEnd; ++i) rFunc1.set_(i, rFunc1.get_(i) + aRHS.subs(rFunc1.getX(i)));
        }
    }
    @Override public void ebeMinus2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMinus2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构
            final int tNx = rFunc1.Nx();
            final int tStart, tEnd;
            if (aRHS instanceof IZeroBoundFunc1) {
                // 对于零边界的特殊优化，只需要运算一部分
                IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
                tStart = Math.max((int)Math.floor((tRHS.zeroBoundL() - rFunc1.x0())/rFunc1.dx()), 0);
                tEnd = Math.min((int)Math.ceil((tRHS.zeroBoundR() - rFunc1.x0())/rFunc1.dx()) + 1, tNx);
            } else {
                tStart = 0; tEnd = tNx;
            }
            for (int i = tStart; i < tEnd; ++i) rFunc1.set_(i, rFunc1.get_(i) - aRHS.subs(rFunc1.getX(i)));
        }
    }
    @Override public void ebeLMinus2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeLMinus2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS.subs(rFunc1.getX(i)) - rFunc1.get_(i));
        }
    }
    @Override public void ebeMultiply2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMultiply2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, rFunc1.get_(i) * aRHS.subs(rFunc1.getX(i)));
        }
    }
    @Override public void ebeDiv2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDiv2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, rFunc1.get_(i) / aRHS.subs(rFunc1.getX(i)));
        }
    }
    @Override public void ebeLDiv2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeLDiv2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS.subs(rFunc1.getX(i)) / rFunc1.get_(i));
        }
    }
    @Override public void ebeMod2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMod2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, rFunc1.get_(i) % aRHS.subs(rFunc1.getX(i)));
        }
    }
    @Override public void ebeLMod2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeLMod2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS.subs(rFunc1.getX(i)) % rFunc1.get_(i));
        }
    }
    @Override public void ebeDo2this(IFunc1Subs aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDo2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize(), aOpt);
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(rFunc1.get_(i), aRHS.subs(rFunc1.getX(i))));
        }
    }
    
    @Override public void mapPlus2this      (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapPlus2this_       (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapMinus2this     (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapMinus2this_      (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapLMinus2this    (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapLMinus2this_     (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapMultiply2this  (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapMultiply2this_   (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapDiv2this       (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapDiv2this_        (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapLDiv2this      (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapLDiv2this_       (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapMod2this       (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapMod2this_        (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapLMod2this      (double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapLMod2this_       (rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void mapDo2this        (IDoubleOperator1 aOpt) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapDo2this_(rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize(), aOpt);}
    
    @Override public void mapFill2this(double aRHS) {DoubleArrayFunc1 rFunc1 = thisFunc1_(); ARRAY.mapFill2this_(rFunc1.getData(), rFunc1.shiftSize(), aRHS, rFunc1.dataSize());}
    @Override public void ebeFill2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        final double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeFill2this_(rFunc1.getData(), rFunc1.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.dataSize());
        } else {
            final double[] rData = rFunc1.getData();
            final int rShift = rFunc1.shiftSize();
            final int rSize = rFunc1.dataSize();
            if (rShift == 0) for (int i = 0; i < rSize; ++i) rData[i] += aRHS.subs(rFunc1.getX(i));
            else for (int i = 0, j = rShift; i < rSize; ++i, ++j) rData[j] += aRHS.subs(rFunc1.getX(i));
        }
    }
    
    
    
    /** 函数特有的运算 */
    @Override public IFunc1 ebeDoFull(IFunc1Subs aLHS, IFunc1Subs aRHS, IDoubleOperator3 aOpt) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tX = rFunc1.getX(i);
            rFunc1.set_(i, aOpt.cal(aLHS.subs(tX), aRHS.subs(tX), tX));
        }
        return rFunc1;
    }
    @Override public IFunc1 mapDoFull(IFunc1Subs aLHS, IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 rFunc1 = newFunc1_();
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tX = rFunc1.getX(i);
            rFunc1.set_(i, aOpt.cal(aLHS.subs(tX), tX));
        }
        return rFunc1;
    }
    @Override public void ebeDoFull2this(IFunc1Subs aRHS, IDoubleOperator3 aOpt) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tX = rFunc1.getX(i);
            rFunc1.set_(i, aOpt.cal(rFunc1.get_(i), aRHS.subs(tX), tX));
        }
    }
    @Override public void mapDoFull2this(IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 rFunc1 = thisFunc1_();
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(rFunc1.get_(i), rFunc1.getX(i)));
    }
    
    
    /** 边界外的结果不保证正确性 */
    @Override public IFunc1 laplacian() {
        DoubleArrayFunc1 tFunc1 = thisFunc1_();
        IFunc1 rFunc1 = newFunc1_(tFunc1.x0(), tFunc1.dx(), tFunc1.Nx());
        laplacian2Dest(rFunc1);
        return rFunc1;
    }
    /** 严格来说获取到的 data 顺序没有相关性，因此不能根据这个来做 laplacian */
    @Override public void laplacian2Dest(IVectorSetter rDest) {
        DoubleArrayFunc1 tFunc1 = thisFunc1_();
        int tNx = tFunc1.Nx();
        double tDx2 = tFunc1.dx() * tFunc1.dx();
        for (int i = 0; i < tNx; ++i) {
            int imm = i-1;
            double tFmm = (imm < 0) ? tFunc1.getOutL_(imm) : tFunc1.get_(imm);
            int ipp = i+1;
            double tFpp = (ipp >= tNx) ? tFunc1.getOutR_(ipp) : tFunc1.get_(ipp);
            
            rDest.set(i, (tFmm + tFpp - 2*tFunc1.get_(i)) / tDx2);
        }
    }
    
    /** 对于卷积以 refConvolve 为主 */
    @Override public IFunc1 convolve(IFunc2Subs aConv) {
        if (aConv instanceof IFunc2) {
            IFunc2 tConv = (IFunc2)aConv;
            return new ZeroBoundFunc1(tConv.y0(), tConv.dy(), tConv.Ny(), refConvolve(aConv));
        } else {
            DoubleArrayFunc1 tFunc1 = thisFunc1_();
            return new ZeroBoundFunc1(tFunc1.x0(), tFunc1.dx(), tFunc1.Nx(), refConvolve(aConv));
        }
    }
    @Override public IFunc1Subs refConvolve(IFunc2Subs aConv) {
        final DoubleArrayFunc1 tFunc1 = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tFunc1.x0(), k) * tFunc1.get_(0);
            double tResult = 0.0;
            double tDx2 = tFunc1.dx()/2.0;
            int tNx = tFunc1.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tFunc1.getX(i), k) * tFunc1.get_(i);
                tResult += tDx2*(tC + pC);
                pC = tC;
            }
            // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
            double tC = aConv.subs(tFunc1.getX(tNx), k) * tFunc1.getOutR_(tNx);
            tResult += tDx2*(tC + pC);
            
            return tResult;
        };
    }
    @Override public IFunc1 convolveFull(IFunc3Subs aConv) {
        if (aConv instanceof IFunc3) {
            IFunc3 tConv = (IFunc3)aConv;
            return new ZeroBoundFunc1(tConv.z0(), tConv.dz(), tConv.Nz(), refConvolveFull(aConv));
        } else {
            DoubleArrayFunc1 tFunc1 = thisFunc1_();
            return new ZeroBoundFunc1(tFunc1.x0(), tFunc1.dx(), tFunc1.Nx(), refConvolveFull(aConv));
        }
    }
    @SuppressWarnings("SuspiciousNameCombination")
    @Override public IFunc1Subs refConvolveFull(IFunc3Subs aConv) {
        final DoubleArrayFunc1 tFunc1 = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tFunc1.get_(0), tFunc1.mX0, k);
            double tResult = 0.0;
            double tDx2 = tFunc1.dx()/2.0;
            int tNx = tFunc1.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tFunc1.get_(i), tFunc1.getX(i), k);
                tResult += tDx2*(tC + pC);
                pC = tC;
            }
            // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
            double tC = aConv.subs(tFunc1.getOutR_(tNx), tFunc1.getX(tNx), k);
            tResult += tDx2*(tC + pC);
            
            return tResult;
        };
    }
    
    /** 由于是线性插值，因此最大的位置就是对应的 data 值 */
    @Override public double maxX() {
        final DoubleArrayFunc1 tFunc1 = thisFunc1_();
        int tMaxIdx = -1;
        double tMaxValue = Double.NEGATIVE_INFINITY;
        int tNx = tFunc1.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tValue = tFunc1.get_(i);
            if (tValue > tMaxValue) {
                tMaxValue = tValue;
                tMaxIdx = i;
            }
        }
        return tFunc1.getX(tMaxIdx);
    }
    @Override public double minX() {
        final DoubleArrayFunc1 tFunc1 = thisFunc1_();
        int tMinIdx = -1;
        double tMinValue = Double.POSITIVE_INFINITY;
        int tNx = tFunc1.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tValue = tFunc1.get_(i);
            if (tValue < tMinValue) {
                tMinValue = tValue;
                tMinIdx = i;
            }
        }
        return tFunc1.getX(tMinIdx);
    }
    
    
    /** 由于函数实际是没有边界的，因此默认的精度根据自身来而不会考虑输入 */
    private DoubleArrayFunc1 newFunc1_() {
        final DoubleArrayFunc1 tFunc1 = thisFunc1_();
        return newFunc1_(tFunc1.x0(), tFunc1.dx(), tFunc1.Nx());
    }
    
    /** stuff to override */
    protected abstract DoubleArrayFunc1 thisFunc1_();
    protected abstract DoubleArrayFunc1 newFunc1_(double aX0, double aDx, int aNx);
}
