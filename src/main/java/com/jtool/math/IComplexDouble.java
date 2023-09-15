package com.jtool.math;

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
    default ComplexDouble plus(IComplexDouble aComplex   ) {return new ComplexDouble(real() + aComplex.real(), imag() + aComplex.imag());}
    default ComplexDouble plus(double aReal, double aImag) {return new ComplexDouble(real() + aReal          , imag() + aImag          );}
    default ComplexDouble plus(double aReal              ) {return new ComplexDouble(real() + aReal          , imag()                  );}
    
    default ComplexDouble minus(IComplexDouble aComplex   ) {return new ComplexDouble(real() - aComplex.real(), imag() - aComplex.imag());}
    default ComplexDouble minus(double aReal, double aImag) {return new ComplexDouble(real() - aReal          , imag() - aImag          );}
    default ComplexDouble minus(double aReal              ) {return new ComplexDouble(real() - aReal          , imag()                  );}
    
    default ComplexDouble multiply(IComplexDouble aComplex   ) {return new ComplexDouble(real()*aComplex.real() - imag()*aComplex.imag(), imag()*aComplex.real() + real()*aComplex.imag());}
    default ComplexDouble multiply(double aReal, double aImag) {return new ComplexDouble(real()*aReal           - imag()*aImag          , imag()*aReal           + real()*aImag          );}
    default ComplexDouble multiply(double aReal              ) {return new ComplexDouble(real()*aReal                                   , imag()*aReal                                   );}
    
    default ComplexDouble div(IComplexDouble aComplex   ) {double tDiv = aComplex.dot()           ; return new ComplexDouble((real()*aComplex.real() + imag()*aComplex.imag())/tDiv, (imag()*aComplex.real() - real()*aComplex.imag())/tDiv);}
    default ComplexDouble div(double aReal, double aImag) {double tDiv = aReal*aReal + aImag*aImag; return new ComplexDouble((real()*aReal           + imag()*aImag          )/tDiv, (imag()*aReal           - real()*aImag          )/tDiv);}
    default ComplexDouble div(double aReal              ) {return new ComplexDouble(real()/aReal, imag()/aReal);}
    
    default double abs() {return MathEX.Fast.sqrt(real()*real() + imag()*imag());}
    
    /** 这里定义 a.dot(b) = a × b* */
    default ComplexDouble dot(IComplexDouble aComplex   ) {return new ComplexDouble(real()*aComplex.real() + imag()*aComplex.imag(), imag()*aComplex.real() - real()*aComplex.imag());}
    default ComplexDouble dot(double aReal, double aImag) {return new ComplexDouble(real()*aReal           + imag()*aImag          , imag()*aReal           - real()*aImag          );}
    default double dot() {return real()*real() + imag()*imag();}
}
