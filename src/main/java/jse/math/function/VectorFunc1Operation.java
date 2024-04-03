package jse.math.function;


import jse.math.MathEX;
import jse.math.vector.Vector;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * 针对包含 {@link Vector} 的函数的运算。
 * @author liqa
 */
@ApiStatus.Experimental
public abstract class VectorFunc1Operation extends AbstractFunc1Operation {
    /** 通用的一些运算 */
    @Override public IFunc1 plus(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().plus2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) + aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 minus(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().minus2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) - aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 lminus(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().lminus2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aRHS.subs(rFunc1.getX(i)) - tThis.get(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 multiply(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().multiply2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) * aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 div(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().div2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) / aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 ldiv(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().ldiv2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aRHS.subs(rFunc1.getX(i)) / tThis.get(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 mod(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().mod2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) % aRHS.subs(rFunc1.getX(i)));
        }
        return rFunc1;
    }
    @Override public IFunc1 lmod(IFunc1 aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().lmod2dest(tDataR, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aRHS.subs(rFunc1.getX(i)) % tThis.get(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 operate(IFunc1 aRHS, DoubleBinaryOperator aOpt) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        Vector tDataR = rFunc1.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().operate2dest(tDataR, rFunc1.internalData(), aOpt);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aOpt.applyAsDouble(tThis.get(i), aRHS.subs(rFunc1.getX(i))));
        }
        return rFunc1;
    }
    
    @Override public IFunc1 plus(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().plus2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) + aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 minus(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().minus2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，并且考虑到代码量这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) - aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 lminus(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().lminus2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aRHS - tThis.get(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 multiply(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().multiply2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) * aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 div(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().div2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) / aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 ldiv(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().ldiv2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aRHS / tThis.get(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 mod(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().mod2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, tThis.get(i) % aRHS);
        }
        return rFunc1;
    }
    @Override public IFunc1 lmod(double aRHS) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().lmod2dest(aRHS, rFunc1.internalData());
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aRHS % tThis.get(i));
        }
        return rFunc1;
    }
    @Override public IFunc1 map(DoubleUnaryOperator aOpt) {
        VectorFunc1 tThis = thisFunc1_();
        VectorFunc1 rFunc1 = newFunc1_();
        Vector tDataL = rFunc1.getIfHasSameOrderData(tThis);
        if (tDataL != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            tDataL.operation().map2dest(rFunc1.internalData(), aOpt);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rFunc1.Nx();
            for (int i = 0; i < tNx; ++i) rFunc1.set(i, aOpt.applyAsDouble(tThis.get(i)));
        }
        return rFunc1;
    }
    
    @Override public void plus2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().plus2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构
            final int tNx = rThis.Nx();
            final int tStart, tEnd;
            if (aRHS instanceof IZeroBoundFunc1) {
                // 对于零边界的特殊优化，只需要运算一部分
                IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
                tStart = Math.max(MathEX.Code.floor2int((tRHS.zeroBoundL() - rThis.x0())/rThis.dx()), 0);
                tEnd = Math.min(MathEX.Code.ceil2int((tRHS.zeroBoundR() - rThis.x0())/rThis.dx()) + 1, tNx);
            } else {
                tStart = 0; tEnd = tNx;
            }
            for (int i = tStart; i < tEnd; ++i) rThis.set(i, rThis.get(i) + aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void minus2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().minus2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构
            final int tNx = rThis.Nx();
            final int tStart, tEnd;
            if (aRHS instanceof IZeroBoundFunc1) {
                // 对于零边界的特殊优化，只需要运算一部分
                IZeroBoundFunc1 tRHS = (IZeroBoundFunc1)aRHS;
                tStart = Math.max(MathEX.Code.floor2int((tRHS.zeroBoundL() - rThis.x0())/rThis.dx()), 0);
                tEnd = Math.min(MathEX.Code.ceil2int((tRHS.zeroBoundR() - rThis.x0())/rThis.dx()) + 1, tNx);
            } else {
                tStart = 0; tEnd = tNx;
            }
            for (int i = tStart; i < tEnd; ++i) rThis.set(i, rThis.get(i) - aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void lminus2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().lminus2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set(i, aRHS.subs(rThis.getX(i)) - rThis.get(i));
        }
    }
    @Override public void multiply2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().multiply2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set(i, rThis.get(i) * aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void div2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().div2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set(i, rThis.get(i) / aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void ldiv2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().ldiv2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set(i, aRHS.subs(rThis.getX(i)) / rThis.get(i));
        }
    }
    @Override public void mod2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().mod2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set(i, rThis.get(i) % aRHS.subs(rThis.getX(i)));
        }
    }
    @Override public void lmod2this(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().lmod2this(tDataR);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set(i, aRHS.subs(rThis.getX(i)) % rThis.get(i));
        }
    }
    @Override public void operate2this(IFunc1 aRHS, DoubleBinaryOperator aOpt) {
        VectorFunc1 rThis = thisFunc1_();
        Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().operate2this(tDataR, aOpt);
        } else {
            // 其余情况不考虑 Vector 的结构，这里不对零边界的情况做优化
            final int tNx = rThis.Nx();
            for (int i = 0; i < tNx; ++i) rThis.set(i, aOpt.applyAsDouble(rThis.get(i), aRHS.subs(rThis.getX(i))));
        }
    }
    
    @Override public void fill(IFunc1 aRHS) {
        VectorFunc1 rThis = thisFunc1_();
        final Vector tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            // 对于完全相同排列的特殊优化，简单起见这里不考虑零边界的情况，只考虑完全一致的情况
            rThis.internalData().operation().fill(tDataR);
        } else {
            rThis.internalData().fill(i -> aRHS.subs(rThis.getX(i)));
        }
    }
    
    
    
    
    /** 边界外的结果不保证正确性 */
    @Override public IFunc1 laplacian() {
        IFunc1 rFunc1 = newFunc1_();
        laplacian2Dest_(rFunc1);
        return rFunc1;
    }
    /** 严格来说获取到的 data 顺序没有相关性，因此不能根据这个来做 laplacian */
    protected void laplacian2Dest_(IFunc1 rDest) {
        VectorFunc1 tFunc1 = thisFunc1_();
        int tNx = tFunc1.Nx();
        double tDx2 = tFunc1.dx() * tFunc1.dx();
        for (int i = 0; i < tNx; ++i) {
            int imm = i-1;
            double tFmm = (imm < 0) ? tFunc1.getOutL_(imm) : tFunc1.get(imm);
            int ipp = i+1;
            double tFpp = (ipp >= tNx) ? tFunc1.getOutR_(ipp) : tFunc1.get(ipp);
            
            rDest.set(i, (tFmm + tFpp - 2*tFunc1.get(i)) / tDx2);
        }
    }
    
    @Override public double integral() {
        VectorFunc1 tThis = thisFunc1_();
        double pF = tThis.get(0);
        double tDx2 = tThis.dx()/2.0;
        double tResult = 0.0;
        int tNx = tThis.Nx();
        for (int i = 1; i < tNx; ++i) {
            double tF = tThis.get(i);
            tResult += tDx2*(tF + pF);
            pF = tF;
        }
        // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
        double tF = tThis.getOutR_(tNx);
        tResult += tDx2*(tF + pF);
        
        return tResult;
    }
    
    /** 对于卷积以 refConvolve 为主 */
    @Override public IFunc1 convolve(IFunc2Subs aConv) {
        VectorFunc1 tThis = thisFunc1_();
        IFunc1 rFunc1 = ZeroBoundFunc1.zeros(tThis.x0(), tThis.dx(), tThis.Nx());
        rFunc1.fill(refConvolve(aConv));
        return rFunc1;
    }
    @Override public IFunc1Subs refConvolve(IFunc2Subs aConv) {
        final VectorFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.x0(), k) * tThis.get(0);
            double tResult = 0.0;
            double tDx2 = tThis.dx()/2.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.getX(i), k) * tThis.get(i);
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
        VectorFunc1 tThis = thisFunc1_();
        IFunc1 rFunc1 = ZeroBoundFunc1.zeros(tThis.x0(), tThis.dx(), tThis.Nx());
        rFunc1.fill(refConvolveFull(aConv));
        return rFunc1;
    }
    @Override public IFunc1Subs refConvolveFull(IFunc3Subs aConv) {
        final VectorFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.get(0), tThis.x0(), k);
            double tResult = 0.0;
            double tDx2 = tThis.dx()/2.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.get(i), tThis.getX(i), k);
                tResult += tDx2*(tC + pC);
                pC = tC;
            }
            // 还需要增加 i == tNx 的一项，这样对于周期边界条件会全部都积分
            double tC = aConv.subs(tThis.getOutR_(tNx), tThis.getX(tNx), k);
            tResult += tDx2*(tC + pC);
            
            return tResult;
        };
    }
    
    /** stuff to override */
    protected abstract VectorFunc1 thisFunc1_();
    protected abstract VectorFunc1 newFunc1_();
}
