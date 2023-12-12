package jtool.math.function;

import jtool.code.functional.*;
import jtool.math.vector.IVector;
import org.jetbrains.annotations.ApiStatus;

/**
 * 通用的的函数运算。
 * @author liqa
 */
@ApiStatus.Experimental
public abstract class AbstractFunc1Operation implements IFunc1Operation {
    /** 通用的一些运算 */
    @SuppressWarnings("Convert2MethodRef")
    @Override public IFunc1 plus    (IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (lhs + rhs));}
    @Override public IFunc1 minus   (IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (lhs - rhs));}
    @Override public IFunc1 lminus  (IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (rhs - lhs));}
    @Override public IFunc1 multiply(IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (lhs * rhs));}
    @Override public IFunc1 div     (IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (lhs / rhs));}
    @Override public IFunc1 ldiv    (IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (rhs / lhs));}
    @Override public IFunc1 mod     (IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (lhs % rhs));}
    @Override public IFunc1 lmod    (IFunc1 aRHS) {return operate(aRHS, (lhs, rhs) -> (rhs % lhs));}
    @Override public IFunc1 operate (IFunc1 aRHS, IDoubleOperator2 aOpt) {
        IFunc1 tThis = thisFunc1_();
        IFunc1 rFunc1 = newFunc1_();
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(tThis.get_(i), aRHS.subs(rFunc1.getX(i))));
        return rFunc1;
    }
    
    @Override public IFunc1 plus    (final double aRHS) {return map(lhs -> (lhs + aRHS));}
    @Override public IFunc1 minus   (final double aRHS) {return map(lhs -> (lhs - aRHS));}
    @Override public IFunc1 lminus  (final double aRHS) {return map(lhs -> (aRHS - lhs));}
    @Override public IFunc1 multiply(final double aRHS) {return map(lhs -> (lhs * aRHS));}
    @Override public IFunc1 div     (final double aRHS) {return map(lhs -> (lhs / aRHS));}
    @Override public IFunc1 ldiv    (final double aRHS) {return map(lhs -> (aRHS / lhs));}
    @Override public IFunc1 mod     (final double aRHS) {return map(lhs -> (lhs % aRHS));}
    @Override public IFunc1 lmod    (final double aRHS) {return map(lhs -> (aRHS % lhs));}
    @Override public IFunc1 map     (IDoubleOperator1 aOpt) {
        IFunc1 tThis = thisFunc1_();
        IFunc1 rFunc1 = newFunc1_();
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(tThis.get_(i)));
        return rFunc1;
    }
    
    @SuppressWarnings("Convert2MethodRef")
    @Override public void plus2this     (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (lhs + rhs));}
    @Override public void minus2this    (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (lhs - rhs));}
    @Override public void lminus2this   (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (rhs - lhs));}
    @Override public void multiply2this (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (lhs * rhs));}
    @Override public void div2this      (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (lhs / rhs));}
    @Override public void ldiv2this     (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (rhs / lhs));}
    @Override public void mod2this      (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (lhs % rhs));}
    @Override public void lmod2this     (IFunc1 aRHS) {operate2this(aRHS, (lhs, rhs) -> (rhs % lhs));}
    @Override public void operate2this  (IFunc1 aRHS, IDoubleOperator2 aOpt) {
        IFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) rThis.set_(i, aOpt.cal(rThis.get_(i), aRHS.subs(rThis.getX(i))));
    }
    
    @Override public void plus2this     (double aRHS) {thisFunc1_().f().operation().plus2this    (aRHS);}
    @Override public void minus2this    (double aRHS) {thisFunc1_().f().operation().minus2this   (aRHS);}
    @Override public void lminus2this   (double aRHS) {thisFunc1_().f().operation().lminus2this  (aRHS);}
    @Override public void multiply2this (double aRHS) {thisFunc1_().f().operation().multiply2this(aRHS);}
    @Override public void div2this      (double aRHS) {thisFunc1_().f().operation().div2this     (aRHS);}
    @Override public void ldiv2this     (double aRHS) {thisFunc1_().f().operation().ldiv2this    (aRHS);}
    @Override public void mod2this      (double aRHS) {thisFunc1_().f().operation().mod2this     (aRHS);}
    @Override public void lmod2this     (double aRHS) {thisFunc1_().f().operation().lmod2this    (aRHS);}
    @Override public void map2this      (IDoubleOperator1 aOpt) {thisFunc1_().f().operation().map2this(aOpt);}
    
    @Override public void fill          (double aRHS) {thisFunc1_().f().operation().fill(aRHS);}
    @Override public void fill          (IVector aRHS) {thisFunc1_().f().operation().fill(aRHS);}
    @Override public void fill          (IFunc1 aRHS) {fill((IFunc1Subs)aRHS);}
    @Override public void fill          (IFunc1Subs aRHS) {
        IFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) rThis.set_(i, aRHS.subs(rThis.getX(i)));
    }
    @Override public void assign        (IDoubleSupplier aSup) {thisFunc1_().f().operation().assign(aSup);}
    @Override public void forEach       (IDoubleConsumer1 aCon) {thisFunc1_().f().operation().forEach(aCon);}
    
    /** 函数特有的运算 */
    @Override public IFunc1 operateFull(IFunc1 aRHS, IDoubleOperator3 aOpt) {
        IFunc1 tThis = thisFunc1_();
        IFunc1 rFunc1 = newFunc1_();
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tX = rFunc1.getX(i);
            rFunc1.set_(i, aOpt.cal(tThis.get_(i), aRHS.subs(tX), tX));
        }
        return rFunc1;
    }
    @Override public IFunc1 mapFull(IDoubleOperator2 aOpt) {
        IFunc1 tThis = thisFunc1_();
        IFunc1 rFunc1 = newFunc1_();
        final int tNx = rFunc1.Nx();
        for (int i = 0; i < tNx; ++i) rFunc1.set_(i, aOpt.cal(tThis.get_(i), rFunc1.getX(i)));
        return rFunc1;
    }
    @Override public void operateFull2this(IFunc1 aRHS, IDoubleOperator3 aOpt) {
        IFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) {
            double tX = rThis.getX(i);
            rThis.set_(i, aOpt.cal(rThis.get_(i), aRHS.subs(tX), tX));
        }
    }
    @Override public void mapFull2this(IDoubleOperator2 aOpt) {
        IFunc1 rThis = thisFunc1_();
        final int tNx = rThis.Nx();
        for (int i = 0; i < tNx; ++i) rThis.set_(i, aOpt.cal(rThis.get_(i), rThis.getX(i)));
    }
    
    
    /** 简单起见，对于一般非均匀的直接不支持此操作 */
    @Override public IFunc1 laplacian() {throw new UnsupportedOperationException("laplacian");}
    
    @Override public double integral() {
        final IFunc1 tThis = thisFunc1_();
        double pF = tThis.get_(0);
        double tResult = 0.0;
        int tNx = tThis.Nx();
        for (int i = 1; i < tNx; ++i) {
            double tF = tThis.get_(i);
            tResult += tThis.dx(i-1)*(tF + pF)*0.5;
            pF = tF;
        }
        return tResult;
    }
    
    /** 对于卷积以 refConvolve 为主 */
    @Override public IFunc1 convolve(IFunc2Subs aConv) {
        IFunc1 rFunc1 = newFunc1_();
        rFunc1.fill(refConvolve(aConv));
        return rFunc1;
    }
    @Override public IFunc1Subs refConvolve(IFunc2Subs aConv) {
        final IFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.x0(), k) * tThis.get_(0);
            double tResult = 0.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.getX(i), k) * tThis.get_(i);
                tResult += tThis.dx(i-1)*(tC + pC)*0.5;
                pC = tC;
            }
            // 一般情况不考虑边界外的值
            return tResult;
        };
    }
    
    @Override public IFunc1 convolveFull(IFunc3Subs aConv) {
        IFunc1 rFunc1 = newFunc1_();
        rFunc1.fill(refConvolveFull(aConv));
        return rFunc1;
    }
    @Override public IFunc1Subs refConvolveFull(IFunc3Subs aConv) {
        final IFunc1 tThis = thisFunc1_();
        return k -> {
            double pC = aConv.subs(tThis.get_(0), tThis.x0(), k);
            double tResult = 0.0;
            int tNx = tThis.Nx();
            for (int i = 1; i < tNx; ++i) {
                double tC = aConv.subs(tThis.get_(i), tThis.getX(i), k);
                tResult += tThis.dx(i-1)*(tC + pC)*0.5;
                pC = tC;
            }
            // 一般情况不考虑边界外的值
            return tResult;
        };
    }
    
    /** 由于是线性插值，因此最大的位置就是对应的 data 值 */
    @Override public double maxX() {
        final IFunc1 tThis = thisFunc1_();
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
        final IFunc1 tThis = thisFunc1_();
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
    protected abstract IFunc1 thisFunc1_();
    protected abstract IFunc1 newFunc1_();
}
