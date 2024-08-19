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
    
    boolean equals(Object other);
    int hashCode();
    
    /** 提供一些常见的复数运算；使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default ComplexDouble plus(IComplexDouble aComplex) {return plus(aComplex.real(), aComplex.imag());}
    default ComplexDouble plus(ComplexDouble aComplex) {return plus(aComplex.mReal, aComplex.mImag);}
    default ComplexDouble plus(double aReal, double aImag) {return new ComplexDouble(real()+aReal, imag()+aImag);}
    default ComplexDouble plus(double aReal) {return new ComplexDouble(real()+aReal, imag());}
    
    default ComplexDouble minus(IComplexDouble aComplex) {return minus(aComplex.real(), aComplex.imag());}
    default ComplexDouble minus(ComplexDouble aComplex) {return minus(aComplex.mReal, aComplex.mImag);}
    default ComplexDouble minus(double aReal, double aImag) {return new ComplexDouble(real()-aReal, imag()-aImag);}
    default ComplexDouble minus(double aReal) {return new ComplexDouble(real()-aReal, imag());}
    
    default ComplexDouble lminus(IComplexDouble aComplex) {return lminus(aComplex.real(), aComplex.imag());}
    default ComplexDouble lminus(ComplexDouble aComplex) {return lminus(aComplex.mReal, aComplex.mImag);}
    default ComplexDouble lminus(double aReal, double aImag) {return new ComplexDouble(aReal-real(), aImag-imag());}
    default ComplexDouble lminus(double aReal) {return new ComplexDouble(aReal-real(), -imag());}
    
    default ComplexDouble multiply(IComplexDouble aComplex) {return multiply(aComplex.real(), aComplex.imag());}
    default ComplexDouble multiply(ComplexDouble aComplex) {return multiply(aComplex.mReal, aComplex.mImag);}
    default ComplexDouble multiply(double aReal, double aImag) {
        final double lReal = real(), lImag = imag();
        return new ComplexDouble(lReal*aReal - lImag*aImag, lImag*aReal + lReal*aImag);
    }
    default ComplexDouble multiply(double aReal) {
        return new ComplexDouble(real()*aReal, imag()*aReal);
    }
    
    default ComplexDouble div(IComplexDouble aComplex) {return div(aComplex.real(), aComplex.imag());}
    default ComplexDouble div(ComplexDouble aComplex) {return div(aComplex.mReal, aComplex.mImag);}
    default ComplexDouble div(double aReal, double aImag) {
        final double lReal = real(), lImag = imag();
        final double div = aReal*aReal + aImag*aImag;
        return new ComplexDouble((lReal*aReal + lImag*aImag)/div, (lImag*aReal - lReal*aImag)/div);
    }
    default ComplexDouble div(double aReal) {
        return new ComplexDouble(real()/aReal, imag()/aReal);
    }
    
    default ComplexDouble ldiv(IComplexDouble aComplex) {return ldiv(aComplex.real(), aComplex.imag());}
    default ComplexDouble ldiv(ComplexDouble aComplex) {return ldiv(aComplex.mReal, aComplex.mImag);}
    default ComplexDouble ldiv(double aReal, double aImag) {
        final double lReal = real(), lImag = imag();
        final double div = lReal*lReal + lImag*lImag;
        return new ComplexDouble((aReal*lReal + aImag*lImag)/div, (aImag*lReal - aReal*lImag)/div);
    }
    default ComplexDouble ldiv(double aReal) {
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
