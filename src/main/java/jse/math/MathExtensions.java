package jse.math;

import jse.math.matrix.IComplexMatrix;
import jse.math.matrix.IMatrix;
import jse.math.vector.IComplexVector;
import jse.math.vector.IIntVector;
import jse.math.vector.ILogicalVector;
import jse.math.vector.IVector;

/**
 * 用于 Groovy 使用的扩展方法，
 * 用于对数字增加一些扩展运算
 * @author liqa
 */
public class MathExtensions {
    public static IVector plus    (Number aLHS, IVector aRHS) {return aRHS.plus              (aLHS.doubleValue());}
    public static IVector minus   (Number aLHS, IVector aRHS) {return aRHS.operation().lminus(aLHS.doubleValue());}
    public static IVector multiply(Number aLHS, IVector aRHS) {return aRHS.multiply          (aLHS.doubleValue());}
    public static IVector div     (Number aLHS, IVector aRHS) {return aRHS.operation().ldiv  (aLHS.doubleValue());}
    public static IVector mod     (Number aLHS, IVector aRHS) {return aRHS.operation().lmod  (aLHS.doubleValue());}
    
    public static IMatrix plus    (Number aLHS, IMatrix aRHS) {return aRHS.plus              (aLHS.doubleValue());}
    public static IMatrix minus   (Number aLHS, IMatrix aRHS) {return aRHS.operation().lminus(aLHS.doubleValue());}
    public static IMatrix multiply(Number aLHS, IMatrix aRHS) {return aRHS.multiply          (aLHS.doubleValue());}
    public static IMatrix div     (Number aLHS, IMatrix aRHS) {return aRHS.operation().ldiv  (aLHS.doubleValue());}
    public static IMatrix mod     (Number aLHS, IMatrix aRHS) {return aRHS.operation().lmod  (aLHS.doubleValue());}
    
    public static IComplexVector plus    (IComplexDouble aLHS, IComplexVector aRHS) {return aRHS.plus              (aLHS);}
    public static IComplexVector minus   (IComplexDouble aLHS, IComplexVector aRHS) {return aRHS.operation().lminus(aLHS);}
    public static IComplexVector multiply(IComplexDouble aLHS, IComplexVector aRHS) {return aRHS.multiply          (aLHS);}
    public static IComplexVector div     (IComplexDouble aLHS, IComplexVector aRHS) {return aRHS.operation().ldiv  (aLHS);}
    public static IComplexVector plus    (Number         aLHS, IComplexVector aRHS) {return aRHS.plus              (aLHS.doubleValue());}
    public static IComplexVector minus   (Number         aLHS, IComplexVector aRHS) {return aRHS.operation().lminus(aLHS.doubleValue());}
    public static IComplexVector multiply(Number         aLHS, IComplexVector aRHS) {return aRHS.multiply          (aLHS.doubleValue());}
    public static IComplexVector div     (Number         aLHS, IComplexVector aRHS) {return aRHS.operation().ldiv  (aLHS.doubleValue());}
    public static IComplexVector plus    (IVector        aLHS, IComplexVector aRHS) {return aRHS.plus              (aLHS);}
    public static IComplexVector minus   (IVector        aLHS, IComplexVector aRHS) {return aRHS.operation().lminus(aLHS);}
    public static IComplexVector multiply(IVector        aLHS, IComplexVector aRHS) {return aRHS.multiply          (aLHS);}
    public static IComplexVector div     (IVector        aLHS, IComplexVector aRHS) {return aRHS.operation().ldiv  (aLHS);}
    
    public static IComplexMatrix plus    (IComplexDouble aLHS, IComplexMatrix aRHS) {return aRHS.plus              (aLHS);}
    public static IComplexMatrix minus   (IComplexDouble aLHS, IComplexMatrix aRHS) {return aRHS.operation().lminus(aLHS);}
    public static IComplexMatrix multiply(IComplexDouble aLHS, IComplexMatrix aRHS) {return aRHS.multiply          (aLHS);}
    public static IComplexMatrix div     (IComplexDouble aLHS, IComplexMatrix aRHS) {return aRHS.operation().ldiv  (aLHS);}
    public static IComplexMatrix plus    (Number         aLHS, IComplexMatrix aRHS) {return aRHS.plus              (aLHS.doubleValue());}
    public static IComplexMatrix minus   (Number         aLHS, IComplexMatrix aRHS) {return aRHS.operation().lminus(aLHS.doubleValue());}
    public static IComplexMatrix multiply(Number         aLHS, IComplexMatrix aRHS) {return aRHS.multiply          (aLHS.doubleValue());}
    public static IComplexMatrix div     (Number         aLHS, IComplexMatrix aRHS) {return aRHS.operation().ldiv  (aLHS.doubleValue());}
    public static IComplexMatrix plus    (IMatrix        aLHS, IComplexMatrix aRHS) {return aRHS.plus              (aLHS);}
    public static IComplexMatrix minus   (IMatrix        aLHS, IComplexMatrix aRHS) {return aRHS.operation().lminus(aLHS);}
    public static IComplexMatrix multiply(IMatrix        aLHS, IComplexMatrix aRHS) {return aRHS.multiply          (aLHS);}
    public static IComplexMatrix div     (IMatrix        aLHS, IComplexMatrix aRHS) {return aRHS.operation().ldiv  (aLHS);}
    
    public static IIntVector plus    (Integer aLHS, IIntVector aRHS) {return aRHS.plus              (aLHS);}
    public static IIntVector minus   (Integer aLHS, IIntVector aRHS) {return aRHS.operation().lminus(aLHS);}
    public static IIntVector multiply(Integer aLHS, IIntVector aRHS) {return aRHS.multiply          (aLHS);}
    public static IIntVector div     (Integer aLHS, IIntVector aRHS) {return aRHS.operation().ldiv  (aLHS);}
    public static IIntVector mod     (Integer aLHS, IIntVector aRHS) {return aRHS.operation().lmod  (aLHS);}
    
    public static ILogicalVector and(Boolean aLHS, ILogicalVector aRHS) {return aRHS.and(aLHS);}
    public static ILogicalVector or (Boolean aLHS, ILogicalVector aRHS) {return aRHS.or (aLHS);}
    public static ILogicalVector xor(Boolean aLHS, ILogicalVector aRHS) {return aRHS.xor(aLHS);}
    
    public static ComplexDouble plus    (Number aLHS, IComplexDouble aRHS) {return aRHS.plus    (aLHS.doubleValue());}
    public static ComplexDouble minus   (Number aLHS, IComplexDouble aRHS) {return aRHS.lminus  (aLHS.doubleValue());}
    public static ComplexDouble multiply(Number aLHS, IComplexDouble aRHS) {return aRHS.multiply(aLHS.doubleValue());}
    public static ComplexDouble div     (Number aLHS, IComplexDouble aRHS) {return aRHS.ldiv    (aLHS.doubleValue());}
}
