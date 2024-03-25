package jse.atom;

/**
 * 用于 Groovy 使用的扩展方法，
 * 用于对数字增加一些扩展运算
 * @author liqa
 */
public class XYZExtensions {
    public static XYZ plus    (Number aLHS, IXYZ aRHS) {return aRHS.plus    (aLHS.doubleValue());}
    public static XYZ minus   (Number aLHS, IXYZ aRHS) {return aRHS.lminus  (aLHS.doubleValue());}
    public static XYZ multiply(Number aLHS, IXYZ aRHS) {return aRHS.multiply(aLHS.doubleValue());}
    public static XYZ div     (Number aLHS, IXYZ aRHS) {return aRHS.ldiv    (aLHS.doubleValue());}
}
