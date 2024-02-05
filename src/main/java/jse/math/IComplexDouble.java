package jse.math;

/**
 * 通用的复数接口，使用此接口还有一重含义时此复数值是不建议修改的
 * <p>
 * 现在运算统一返回 {@link ComplexDouble} 方便使用，并且也暗示这些方法会返回新的对象
 * @author liqa
 */
public interface IComplexDouble {
    double real();
    double imag();
    
    /** 提供一些常见的复数运算 */
    default ComplexDouble plus(IComplexDouble aComplex) {return new ComplexDouble(real() + aComplex.real(), imag() + aComplex.imag());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default ComplexDouble plus(ComplexDouble aComplex ) {return new ComplexDouble(real() + aComplex.mReal , real() + aComplex.mImag );}
    default ComplexDouble plus(double aReal           ) {return new ComplexDouble(real() + aReal          , imag()                  );}
    
    default ComplexDouble minus(IComplexDouble aComplex) {return new ComplexDouble(real() - aComplex.real(), imag() - aComplex.imag());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default ComplexDouble minus(ComplexDouble aComplex ) {return new ComplexDouble(real() - aComplex.mReal , real() - aComplex.mImag );}
    default ComplexDouble minus(double aReal           ) {return new ComplexDouble(real() - aReal          , imag()                  );}
    
    default ComplexDouble lminus(IComplexDouble aComplex) {return new ComplexDouble(aComplex.real() - real(), aComplex.imag() - imag());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default ComplexDouble lminus(ComplexDouble aComplex ) {return new ComplexDouble(aComplex.mReal  - real(), aComplex.mImag  - real());}
    default ComplexDouble lminus(double aReal           ) {return new ComplexDouble(aReal - real()          , -imag()                 );}
    
    default ComplexDouble multiply(IComplexDouble aComplex) {
        final double lReal = real(),          lImag = imag();
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        return new ComplexDouble(lReal*rReal - lImag*rImag, lImag*rReal + lReal*rImag);
    }
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default ComplexDouble multiply(ComplexDouble aComplex) {
        final double lReal = real(),         lImag = imag();
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        return new ComplexDouble(lReal*rReal - lImag*rImag, lImag*rReal + lReal*rImag);
    }
    default ComplexDouble multiply(double aReal           ) {
        return new ComplexDouble(real()*aReal, imag()*aReal);
    }
    
    default ComplexDouble div(IComplexDouble aComplex) {
        final double lReal = real(),          lImag = imag();
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = rReal*rReal + rImag*rImag;
        return new ComplexDouble((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
    }
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default ComplexDouble div(ComplexDouble aComplex) {
        final double lReal = real(),         lImag = imag();
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = rReal*rReal + rImag*rImag;
        return new ComplexDouble((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
    }
    default ComplexDouble div(double aReal          ) {
        return new ComplexDouble(real()/aReal, imag()/aReal);
    }
    
    default ComplexDouble ldiv(IComplexDouble aComplex) {
        final double lReal = real(),          lImag = imag();
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = lReal*lReal + lImag*lImag;
        return new ComplexDouble((rReal*lReal + rImag*lImag)/div, (rImag*lReal - rReal*lImag)/div);
    }
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default ComplexDouble ldiv(ComplexDouble aComplex) {
        final double lReal = real(),         lImag = imag();
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = lReal*lReal + lImag*lImag;
        return new ComplexDouble((rReal*lReal + rImag*lImag)/div, (rImag*lReal - rReal*lImag)/div);
    }
    default ComplexDouble ldiv(double aReal          ) {
        final double lReal = real(), lImag = imag();
        final double div = lReal*lReal + lImag*lImag;
        return new ComplexDouble((aReal*lReal)/div, (-aReal*lImag)/div);
    }
    
    default ComplexDouble negative() {return new ComplexDouble(-real(), -imag());}
    
    default double norm() {return MathEX.Fast.hypot(real(), imag());}
    default double phase() {return MathEX.Fast.atan2(imag(), real());}
    /** matlab 的名称 */
    default double abs() {return norm();}
    default double angle() {return phase();}
    
    /** 获取复数的共轭值，conjugate */
    default ComplexDouble conj() {return new ComplexDouble(real(), -imag());}
}
