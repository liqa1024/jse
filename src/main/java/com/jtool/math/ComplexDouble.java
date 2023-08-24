package com.jtool.math;


/**
 * 复数类，实际是类似结构体的设计，因此所有成员直接 public，
 * 为了使用方便这里不使用常规结构使用的命名规范
 * @author liqa
 */
public final class ComplexDouble {
    public double real;
    public double imag;
    public ComplexDouble(double real, double imag) {this.real = real; this.imag = imag;}
    public ComplexDouble(double real             ) {this(real, 0.0);}
    public ComplexDouble(                        ) {this(0.0, 0.0);}
    
    /** 提供一些常见的复数运算 */
    public ComplexDouble plus(ComplexDouble complex   ) {return new ComplexDouble(this.real + complex.real, this.imag + complex.imag);}
    public ComplexDouble plus(double real, double imag) {return new ComplexDouble(this.real + real        , this.imag + imag        );}
    public ComplexDouble plus(double real             ) {return new ComplexDouble(this.real + real        , this.imag               );}
    public void     plus2this(ComplexDouble complex   ) {this.real += complex.real; this.imag += complex.imag;}
    public void     plus2this(double real, double imag) {this.real += real        ; this.imag += imag        ;}
    public void     plus2this(double real             ) {this.real += real        ;                           }
    
    public ComplexDouble minus(ComplexDouble complex   ) {return new ComplexDouble(this.real - complex.real, this.imag - complex.imag);}
    public ComplexDouble minus(double real, double imag) {return new ComplexDouble(this.real - real        , this.imag - imag        );}
    public ComplexDouble minus(double real             ) {return new ComplexDouble(this.real - real        , this.imag               );}
    public void     minus2this(ComplexDouble complex   ) {this.real -= complex.real; this.imag -= complex.imag;}
    public void     minus2this(double real, double imag) {this.real -= real        ; this.imag -= imag        ;}
    public void     minus2this(double real             ) {this.real -= real        ;                           }
    
    public ComplexDouble multiply(ComplexDouble complex   ) {return new ComplexDouble(this.real*complex.real - this.imag*complex.imag, this.imag*complex.real + this.real*complex.imag);}
    public ComplexDouble multiply(double real, double imag) {return new ComplexDouble(this.real*real         - this.imag*imag        , this.imag*real         + this.real*imag        );}
    public ComplexDouble multiply(double real             ) {return new ComplexDouble(this.real*real                                 , this.imag*real                                 );}
    public void multiply2this(ComplexDouble complex) {
        double tReal = this.real*complex.real - this.imag*complex.imag;
        double tImag = this.imag*complex.real + this.real*complex.imag;
        this.real = tReal;
        this.imag = tImag;
    }
    public void multiply2this(double real, double imag) {
        double tReal = this.real*real - this.imag*imag;
        double tImag = this.imag*real + this.real*imag;
        this.real = tReal;
        this.imag = tImag;
    }
    public void multiply2this(double real) {
        this.real *= real;
        this.imag *= real;
    }
    
    public ComplexDouble div(ComplexDouble complex   ) {double tDiv = complex.real*complex.real + complex.imag*complex.imag; return new ComplexDouble((this.real*complex.real + this.imag*complex.imag)/tDiv, (this.imag*complex.real - this.real*complex.imag)/tDiv);}
    public ComplexDouble div(double real, double imag) {double tDiv = real*real                 + imag*imag                ; return new ComplexDouble((this.real*real         + this.imag*imag        )/tDiv, (this.real*imag         - this.imag*real        )/tDiv);}
    public ComplexDouble div(double real             ) {return new ComplexDouble(this.real/real, this.imag/real);}
    public void div2this(ComplexDouble complex) {
        double tDiv = complex.real*complex.real + complex.imag*complex.imag;
        double tReal = (this.real*complex.real + this.imag*complex.imag)/tDiv;
        double tImag = (this.imag*complex.real - this.real*complex.imag)/tDiv;
        this.real = tReal;
        this.imag = tImag;
    }
    public void div2this(double real, double imag) {
        double tDiv = real*real + imag*imag;
        double tReal = (this.real*real + this.imag*imag)/tDiv;
        double tImag = (this.imag*real - this.real*imag)/tDiv;
        this.real = tReal;
        this.imag = tImag;
    }
    public void div2this(double real) {
        this.real /= real;
        this.imag /= real;
    }
    
    public double abs() {return MathEX.Fast.sqrt(real*real + imag*imag);}
    public double norm() {return abs();}
    public double dot() {return real*real + imag*imag;}
}
