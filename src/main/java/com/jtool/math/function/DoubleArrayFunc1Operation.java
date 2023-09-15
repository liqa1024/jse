package com.jtool.math.function;


import com.jtool.code.functional.*;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;

/**
 * 针对包含 double[] 的函数的运算，这里更进一步是针对 {@link DoubleArrayFunc1} 的运算。
 * @author liqa
 */
public abstract class DoubleArrayFunc1Operation implements IFunc1Operation {
    /** 通用的一些运算 */
    @Override public IFunc1 plus(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebePlus2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) + aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 minus(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMinus2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) - aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 lminus(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMinus2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS.subs(rFunc1.getX(i)) - tThis.get_(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 multiply(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMultiply2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) * aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 div(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDiv2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) / aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 ldiv(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDiv2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS.subs(rFunc1.getX(i)) / tThis.get_(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 mod(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMod2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) % aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 lmod(IFunc1Subs aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMod2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS.subs(rFunc1.getX(i)) % tThis.get_(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 operate(IFunc1Subs aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        double[] tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDo2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize(), aOpt);
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(tThis.get_(i), aRHS.subs(rFunc1.getX(i))));
        }
        return rFunc1;
    }
    
    @Override public IFunc1 plus(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapPlus2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) + aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 minus(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapMinus2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) - aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 lminus(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapLMinus2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS - tThis.get_(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 multiply(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapMultiply2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) * aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 div(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapDiv2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) / aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 ldiv(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapLDiv2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS / tThis.get_(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 mod(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapMod2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, tThis.get_(i) % aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 lmod(double aRHS) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapLMod2Dest(tDataL, tThis.shiftSize(), aRHS, rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aRHS % tThis.get_(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 map(IDoubleOperator1 aOpt) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        double[] tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.mapDo2Dest(tDataL, tThis.shiftSize(), rFunc1.getData(), rFunc1.shiftSize(), rFunc1.dataSize(), aOpt);
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(tThis.get_(i)));
        }
        return rFunc1;
    }
    
    @Override public void plus2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebePlus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构
            final int tNx = rThis.Nx();
            final int tStart, tEnd;
            if (aRHS instanceof IZeroBoundFunc1) {
                // 对于零边界的特殊优化，只需要运算一部分
                IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
                tStart = Math.max((int)Math.floor((tRHS.zeroBoundL() - rThis.x0())/rThis.dx()), 0);
                tEnd = Math.min((int)Math.ceil((tRHS.zeroBoundR() - rThis.x0())/rThis.dx()) + 1, tNx);
            } else {
                tStart = 0; tEnd = tNx;
            }
            for (int i = tStart; i < tEnd; ++i) rThis.set_(i, rThis.get_(i) + aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void minus2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMinus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构
            final int tNx = rThis.Nx();
            final int tStart, tEnd;
            if (aRHS instanceof IZeroBoundFunc1) {
                // 对于零边界的特殊优化，只需要运算一部分
                IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
                tStart = Math.max((int)Math.floor((tRHS.zeroBoundL() - rThis.x0())/rThis.dx()), 0);
                tEnd = Math.min((int)Math.ceil((tRHS.zeroBoundR() - rThis.x0())/rThis.dx()) + 1, tNx);
            } else {
                tStart = 0; tEnd = tNx;
            }
            for (int i = tStart; i < tEnd; ++i) rThis.set_(i, rThis.get_(i) - aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void lminus2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeLMinus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set_(i, aRHS.subs(rThis.getX(i)) - rThis.get_(i));
        }
    }
    @Override public void multiply2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMultiply2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set_(i, rThis.get_(i) * aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void div2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDiv2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set_(i, rThis.get_(i) / aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void ldiv2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeLDiv2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set_(i, aRHS.subs(rThis.getX(i)) / rThis.get_(i));
        }
    }
    @Override public void mod2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeMod2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set_(i, rThis.get_(i) % aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void lmod2this(IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeLMod2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set_(i, aRHS.subs(rThis.getX(i)) % rThis.get_(i));
        }
    }
    @Override public void operate2this(IFunc1Subs aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeDo2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize(), aOpt);
        } else {
            // 其余情况不考虑 double[] 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set_(i, aOpt.cal(rThis.get_(i), aRHS.subs(rThis.getX(i))));
        }
    }
    
    @Override public void plus2this     (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapPlus2This       (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void minus2this    (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapMinus2This      (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void lminus2this   (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapLMinus2This     (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void multiply2this (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapMultiply2This   (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void div2this      (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapDiv2This        (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void ldiv2this     (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapLDiv2This       (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void mod2this      (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapMod2This        (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void lmod2this     (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapLMod2This       (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void map2this      (IDoubleOperator1 aOpt) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapDo2This(rThis.getData(), rThis.shiftSize(), rThis.dataSize(), aOpt);}
    
    @Override public void fill          (double aRHS) {DoubleArrayFunc1 rThis = thisFunc1_(); ARRAY.mapFill2This(rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void fill          (IFunc1Subs aRHS) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        final double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            ARRAY.ebeFill2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        } else {
            final double[] rData = rThis.getData();
            final int rShift = rThis.shiftSize();
            final int rSize = rThis.dataSize();
            if (rShift == 0) for (int i = 0; i < rSize; ++i) rData[i] += aRHS.subs(rThis.getX(i));
            else for (int i = 0, j = rShift; i < rSize; ++i, ++j) rData[j] += aRHS.subs(rThis.getX(i));
        }
    }
    @Override public void assign        (IDoubleSupplier aSup) {
        // 注意不能改变顺序，因此这里不能考虑 double[] 的情况
        DoubleArrayFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) rThis.set_(i, aSup.get());
    }
    @Override public void forEach       (IDoubleConsumer1 aCon) {
        // 注意不能改变顺序，因此这里不能考虑 double[] 的情况
        DoubleArrayFunc1 tThis = thisFunc1_();
        final int tNx = tThis.Nx();
        for (int i = 0; i < tNx; ++i) aCon.run(tThis.get_(i));
    }
    
    
    
    /** 函数特有的运算 */
    @Override public IFunc1 operateFull(IFunc1Subs aRHS, IDoubleOperator3 aOpt) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tX = rFunc1.getX(i);
            rFunc1.set_(i, aOpt.cal(tThis.get_(i), aRHS.subs(tX), tX));
        }
        return rFunc1;
    }
    @Override public IFunc1 mapFull(IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 tThis = thisFunc1_();
        DoubleArrayFunc1 rFunc1 = newFunc1_(tThis.x0(), tThis.dx(), tThis.Nx());
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(tThis.get_(i), rFunc1.getX(i)));
        return rFunc1;
    }
    @Override public void operateFull2this(IFunc1Subs aRHS, IDoubleOperator3 aOpt) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tX = rThis.getX(i);
            rThis.set_(i, aOpt.cal(rThis.get_(i), aRHS.subs(tX), tX));
        }
    }
    @Override public void mapFull2this(IDoubleOperator2 aOpt) {
        DoubleArrayFunc1 rThis = thisFunc1_();
        // 此时不考虑 double[] 的结构，并且这里不对零边界的情况做优化
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) rThis.set_(i, aOpt.cal(rThis.get_(i), rThis.getX(i)));
    }
    
    
    /** 边界外的结果不保证正确性 */
    @Override public IFunc1 laplacian() {
        DoubleArrayFunc1 tFunc1 = thisFunc1_();
        IFunc1 rFunc1 = newFunc1_(tFunc1.x0(), tFunc1.dx(), tFunc1.Nx());
        laplacian2Dest_(rFunc1);
        return rFunc1;
    }
    /** 严格来说获取到的 data 顺序没有相关性，因此不能根据这个来做 laplacian */
    protected void laplacian2Dest_(IFunc1 rDest) {
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
        final DoubleArrayFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.x0(), k) * tThis.get_(0);
            double tResult = 0.0;
            double tDx2 = tThis.dx()/2.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.getX(i), k) * tThis.get_(i);
                tResult += tDx2*(tC + pC);
                pC = tC;
            }
            // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
            double tC = aConv.subs(tThis.getX(tNx), k) * tThis.getOutR_(tNx);
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
        final DoubleArrayFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.get_(0), tThis.mX0, k);
            double tResult = 0.0;
            double tDx2 = tThis.dx()/2.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.get_(i), tThis.getX(i), k);
                tResult += tDx2*(tC + pC);
                pC = tC;
            }
            // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
            double tC = aConv.subs(tThis.getOutR_(tNx), tThis.getX(tNx), k);
            tResult += tDx2*(tC + pC);
            
            return tResult;
        };
    }
    
    /** 由于是线性插值，因此最大的位置就是对应的 data 值 */
    @Override public double maxX() {
        final DoubleArrayFunc1 tThis = thisFunc1_();
        int tMaxIdx = -1;
        double tMaxValue = Double.NEGATIVE_INFINITY;
        int tNx = tThis.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tValue = tThis.get_(i);
            if (tValue > tMaxValue) {
                tMaxValue = tValue;
                tMaxIdx = i;
            }
        }
        return tThis.getX(tMaxIdx);
    }
    @Override public double minX() {
        final DoubleArrayFunc1 tThis = thisFunc1_();
        int tMinIdx = -1;
        double tMinValue = Double.POSITIVE_INFINITY;
        int tNx = tThis.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tValue = tThis.get_(i);
            if (tValue < tMinValue) {
                tMinValue = tValue;
                tMinIdx = i;
            }
        }
        return tThis.getX(tMinIdx);
    }
    
    
    /** stuff to override */
    protected abstract DoubleArrayFunc1 thisFunc1_();
    protected abstract DoubleArrayFunc1 newFunc1_(double aX0, double aDx, int aNx);
}
