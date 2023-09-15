package com.jtool.math;


/**
 * 复数类，实际是类似结构体的设计，因此所有成员直接 public，
 * 为了使用方便这里不使用常规结构使用的命名规范
 * @author liqa
 */
public final class ComplexDouble implements IComplexDouble {
    public double mReal;
    public double mImag;
    public ComplexDouble(double aReal, double aImag) {mReal = aReal; mImag = aImag;}
    public ComplexDouble(double aReal              ) {this(aReal, 0.0);}
    public ComplexDouble(                          ) {this(0.0, 0.0);}
    public ComplexDouble(IComplexDouble aValue     ) {this(aValue.real(), aValue.imag());}
    
    @Override public double real() {return mReal;}
    @Override public double imag() {return mImag;}
    
    /** print */
    @Override public String toString() {return String.format("%8.4g + %8.4gi", mReal, mImag);}
    
    
    /** 提供一些常见的复数运算 */
    @Override public ComplexDouble plus(IComplexDouble aComplex   ) {return new ComplexDouble(mReal + aComplex.real(), mImag + aComplex.imag());}
    @Override public ComplexDouble plus(double aReal, double aImag) {return new ComplexDouble(mReal + aReal          , mImag + aImag          );}
    @Override public ComplexDouble plus(double aReal              ) {return new ComplexDouble(mReal + aReal          , mImag                  );}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public ComplexDouble plus(ComplexDouble aComplex    ) {return new ComplexDouble(mReal + aComplex.mReal, mImag + aComplex.mImag);}
    public void     plus2this(ComplexDouble aComplex    ) {mReal += aComplex.mReal; mImag += aComplex.mImag;}
    public void     plus2this(double aReal, double aImag) {mReal += aReal         ; mImag += aImag         ;}
    public void     plus2this(double aReal              ) {mReal += aReal         ;                         }
    
    @Override public ComplexDouble minus(IComplexDouble aComplex   ) {return new ComplexDouble(mReal - aComplex.real(), mImag - aComplex.imag());}
    @Override public ComplexDouble minus(double aReal, double aImag) {return new ComplexDouble(mReal - aReal          , mImag - aImag          );}
    @Override public ComplexDouble minus(double aReal              ) {return new ComplexDouble(mReal - aReal          , mImag                  );}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public ComplexDouble minus(ComplexDouble aComplex    ) {return new ComplexDouble(mReal - aComplex.mReal, mImag - aComplex.mImag);}
    public void     minus2this(ComplexDouble aComplex    ) {mReal -= aComplex.mReal; mImag -= aComplex.mImag;}
    public void     minus2this(double aReal, double aImag) {mReal -= aReal         ; mImag -= aImag         ;}
    public void     minus2this(double aReal              ) {mReal -= aReal         ;                         }
    
    @Override public ComplexDouble multiply(IComplexDouble aComplex   ) {return new ComplexDouble(mReal*aComplex.real() - mImag*aComplex.imag(), mImag*aComplex.real() + mReal*aComplex.imag());}
    @Override public ComplexDouble multiply(double aReal, double aImag) {return new ComplexDouble(mReal*aReal           - mImag*aImag          , mImag*aReal           + mReal*aImag          );}
    @Override public ComplexDouble multiply(double aReal              ) {return new ComplexDouble(mReal*aReal                                  , mImag*aReal                                  );}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public ComplexDouble multiply(ComplexDouble aComplex) {return new ComplexDouble(mReal*aComplex.mReal - mImag*aComplex.mImag, mImag*aComplex.mReal + mReal*aComplex.mImag);}
    public void multiply2this(ComplexDouble aComplex) {
        double tReal = mReal*aComplex.mReal - mImag*aComplex.mImag;
        double tImag = mImag*aComplex.mReal + mReal*aComplex.mImag;
        mReal = tReal;
        mImag = tImag;
    }
    public void multiply2this(double aReal, double aImag) {
        double tReal = mReal*aReal - mImag*aImag;
        double tImag = mImag*aReal + mReal*aImag;
        mReal = tReal;
        mImag = tImag;
    }
    public void multiply2this(double aReal) {
        mReal *= aReal;
        mImag *= aReal;
    }
    
    @Override public ComplexDouble div(IComplexDouble aComplex   ) {double tDiv = aComplex.dot()           ; return new ComplexDouble((mReal*aComplex.real() + mImag*aComplex.imag())/tDiv, (mImag*aComplex.real() - mReal*aComplex.imag())/tDiv);}
    @Override public ComplexDouble div(double aReal, double aImag) {double tDiv = aReal*aReal + aImag*aImag; return new ComplexDouble((mReal*aReal           + mImag*aImag          )/tDiv, (mImag*aReal           - mReal*aImag          )/tDiv);}
    @Override public ComplexDouble div(double aReal              ) {return new ComplexDouble(mReal/aReal, mImag/aReal);}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public ComplexDouble div(ComplexDouble aComplex) {double tDiv = aComplex.dot(); return new ComplexDouble((mReal*aComplex.mReal + mImag*aComplex.mImag)/tDiv, (mImag*aComplex.mReal - mReal*aComplex.mImag)/tDiv);}
    public void div2this(ComplexDouble aComplex) {
        double tDiv = aComplex.dot();
        double tReal = (mReal*aComplex.mReal + mImag*aComplex.mImag)/tDiv;
        double tImag = (mImag*aComplex.mReal - mReal*aComplex.mImag)/tDiv;
        mReal = tReal;
        mImag = tImag;
    }
    public void div2this(double aReal, double aImag) {
        double tDiv = aReal*aReal + aImag*aImag;
        double tReal = (mReal*aReal + mImag*aImag)/tDiv;
        double tImag = (mImag*aReal - mReal*aImag)/tDiv;
        mReal = tReal;
        mImag = tImag;
    }
    public void div2this(double aReal) {
        mReal /= aReal;
        mImag /= aReal;
    }
    
    @Override public double abs() {return MathEX.Fast.sqrt(mReal*mReal + mImag*mImag);}
    
    @Override public ComplexDouble dot(IComplexDouble aComplex   ) {return new ComplexDouble(mReal*aComplex.real() + mImag*aComplex.imag(), mImag*aComplex.real() - mReal*aComplex.imag());}
    @Override public ComplexDouble dot(double aReal, double aImag) {return new ComplexDouble(mReal*aReal           + mImag*aImag          , mImag*aReal           - mReal*aImag          );}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public ComplexDouble dot(ComplexDouble aComplex) {return new ComplexDouble(mReal*aComplex.mReal + mImag*aComplex.mImag, mImag*aComplex.mReal - mReal*aComplex.mImag);}
    public void dot2this(ComplexDouble aComplex) {
        double tReal = mReal*aComplex.mReal + mImag*aComplex.mImag;
        double tImag = mImag*aComplex.mReal - mReal*aComplex.mImag;
        mReal = tReal;
        mImag = tImag;
    }
    public void dot2this(double aReal, double aImag) {
        double tReal = mReal*aReal + mImag*aImag;
        double tImag = mImag*aReal - mReal*aImag;
        mReal = tReal;
        mImag = tImag;
    }
    @Override public double dot() {return mReal*mReal + mImag*mImag;}
}
