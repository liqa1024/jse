package com.jtool.math.function;


import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorAny;
import com.jtool.math.vector.IVectorGetter;
import com.jtool.math.vector.Vector;

import java.util.Collection;

/**
 * @author liqa
 * <p> 现在修改为专门获取一维函数的类，默认获取 {@link PBCFunc1} </p>
 */
public class Func1 {
    private Func1() {}
    
    public static IFunc1 ones(double aX0, double aDx, int aNx) {IFunc1 rFunc = zeros(aX0, aDx, aNx); rFunc.fill(1.0); return rFunc;}
    public static IFunc1 zeros(double aX0, double aDx, int aNx) {return PBCFunc1.zeros(aX0, aDx, aNx);}
    
    
    public static IFunc1 from(double aX0, double aDx, int aNx, IFunc1Subs aFunc1Subs) {
        IFunc1 rFunc = zeros(aX0, aDx, aNx);
        rFunc.fill(aFunc1Subs);
        return rFunc;
    }
    public static IFunc1 from(IFunc1 aFunc1) {
        IFunc1 rFunc = zeros(aFunc1.x0(), aFunc1.dx(), aFunc1.Nx());
        rFunc.fill(aFunc1);
        return rFunc;
    }
    
    public static IFunc1 from(double aX0, double aDx, int aNx, Iterable<? extends Number> aList) {
        IFunc1 rFunc = zeros(aX0, aDx, aNx);
        rFunc.fill(aList);
        return rFunc;
    }
    public static IFunc1 from(double aX0, double aDx, Collection<? extends Number> aList) {
        IFunc1 rFunc = zeros(aX0, aDx, aList.size());
        rFunc.fill(aList);
        return rFunc;
    }
    public static IFunc1 from(double aX0, double aDx, double[] aData) {
        IFunc1 rFunc = zeros(aX0, aDx, aData.length);
        rFunc.fill(aData);
        return rFunc;
    }
}
