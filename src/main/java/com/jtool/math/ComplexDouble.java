package com.jtool.math;


/**
 * 复数类，实际是类似结构体的设计，因此所有成员直接 public，
 * 为了使用方便这里不使用常规结构使用的命名规范
 * @author liqa
 */
public final class ComplexDouble implements ISettableComplexDouble {
    public double mReal;
    public double mImag;
    public ComplexDouble(double aReal, double aImag) {mReal = aReal; mImag = aImag;}
    public ComplexDouble(double aReal              ) {this(aReal, 0.0);}
    public ComplexDouble(                          ) {this(0.0, 0.0);}
    public ComplexDouble(IComplexDouble aValue     ) {this(aValue.real(), aValue.imag());}
    public ComplexDouble(ComplexDouble aValue      ) {this(aValue.mReal, aValue.mImag);}
    
    @Override public double real() {return mReal;}
    @Override public double imag() {return mImag;}
    @Override public void setReal(double aReal) {mReal = aReal;}
    @Override public void setImag(double aImag) {mImag = aImag;}
    
    /** print */
    @Override public String toString() {return String.format("%.4g + %.4gi", mReal, mImag);}
    
    
    /** 提供一些常见的复数运算 */
    @Override public ComplexDouble plus(IComplexDouble aComplex) {return new ComplexDouble(mReal + aComplex.real(), mImag + aComplex.imag());}
    @Override public ComplexDouble plus(ComplexDouble aComplex ) {return new ComplexDouble(mReal + aComplex.mReal , mImag + aComplex.mImag );}
    @Override public ComplexDouble plus(double aReal           ) {return new ComplexDouble(mReal + aReal          , mImag                  );}
    @Override public void plus2this(IComplexDouble aComplex) {mReal += aComplex.real(); mImag += aComplex.imag();}
    @Override public void plus2this(ComplexDouble aComplex ) {mReal += aComplex.mReal ; mImag += aComplex.mImag ;}
    @Override public void plus2this(double aReal           ) {mReal += aReal          ;                          }
    
    @Override public ComplexDouble minus(IComplexDouble aComplex) {return new ComplexDouble(mReal - aComplex.real(), mImag - aComplex.imag());}
    @Override public ComplexDouble minus(ComplexDouble aComplex ) {return new ComplexDouble(mReal - aComplex.mReal , mImag - aComplex.mImag );}
    @Override public ComplexDouble minus(double aReal           ) {return new ComplexDouble(mReal - aReal          , mImag                  );}
    @Override public void minus2this(IComplexDouble aComplex) {mReal -= aComplex.real(); mImag -= aComplex.imag();}
    @Override public void minus2this(ComplexDouble aComplex ) {mReal -= aComplex.mReal ; mImag -= aComplex.mImag ;}
    @Override public void minus2this(double aReal           ) {mReal -= aReal          ;                          }
    
    @Override public ComplexDouble lminus(IComplexDouble aComplex) {return new ComplexDouble(aComplex.real() - mReal, aComplex.imag() - mImag);}
    @Override public ComplexDouble lminus(ComplexDouble aComplex ) {return new ComplexDouble(aComplex.mReal  - mReal, aComplex.mImag  - mImag);}
    @Override public ComplexDouble lminus(double aReal           ) {return new ComplexDouble(aReal           - mReal,                 - mImag);}
    @Override public void lminus2this(IComplexDouble aComplex) {mReal = aComplex.real() - mReal; mImag = aComplex.imag() - mImag;}
    @Override public void lminus2this(ComplexDouble aComplex ) {mReal = aComplex.mReal  - mReal; mImag = aComplex.mImag  - mImag;}
    @Override public void lminus2this(double aReal           ) {mReal = aReal           - mReal; mImag =                 - mImag;}
    
    @Override public ComplexDouble multiply(IComplexDouble aComplex) {
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        return new ComplexDouble(mReal*rReal - mImag*rImag, mImag*rReal + mReal*rImag);
    }
    @Override public ComplexDouble multiply(ComplexDouble aComplex ) {
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        return new ComplexDouble(mReal*rReal - mImag*rImag, mImag*rReal + mReal*rImag);
    }
    @Override public ComplexDouble multiply(double aReal           ) {
        return new ComplexDouble(mReal*aReal, mImag*aReal);
    }
    @Override public void multiply2this(IComplexDouble aComplex) {
        final double lReal = mReal,           lImag = mImag;
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        mReal = lReal*rReal - lImag*rImag;
        mImag = lImag*rReal + lReal*rImag;
    }
    @Override public void multiply2this(ComplexDouble aComplex) {
        final double lReal = mReal,          lImag = mImag;
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        mReal = lReal*rReal - lImag*rImag;
        mImag = lImag*rReal + lReal*rImag;
    }
    @Override public void multiply2this(double aReal) {
        mReal *= aReal; mImag *= aReal;
    }
    
    @Override public ComplexDouble div(IComplexDouble aComplex) {
        final double lReal = mReal,           lImag = mImag;
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = rReal*rReal + rImag*rImag;
        return new ComplexDouble((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
    }
    @Override public ComplexDouble div(ComplexDouble aComplex ) {
        final double lReal = mReal,          lImag = mImag;
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = rReal*rReal + rImag*rImag;
        return new ComplexDouble((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
    }
    @Override public ComplexDouble div(double aReal           ) {
        return new ComplexDouble(mReal/aReal, mImag/aReal);
    }
    @Override public void div2this(IComplexDouble aComplex) {
        final double lReal = mReal,           lImag = mImag;
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = rReal*rReal + rImag*rImag;
        mReal = (lReal*rReal + lImag*rImag)/div;
        mImag = (lImag*rReal - lReal*rImag)/div;
    }
    @Override public void div2this(ComplexDouble aComplex ) {
        final double lReal = mReal,          lImag = mImag;
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = rReal*rReal + rImag*rImag;
        mReal = (lReal*rReal + lImag*rImag)/div;
        mImag = (lImag*rReal - lReal*rImag)/div;
    }
    @Override public void div2this(double aReal           ) {
        mReal /= aReal; mImag /= aReal;
    }
    
    @Override public ComplexDouble ldiv(IComplexDouble aComplex) {
        final double lReal = mReal,           lImag = mImag;
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = lReal*lReal + lImag*lImag;
        return new ComplexDouble((rReal*lReal + rImag*lImag)/div, (rImag*lReal - rReal*lImag)/div);
    }
    @Override public ComplexDouble ldiv(ComplexDouble aComplex ) {
        final double lReal = mReal,          lImag = mImag;
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = lReal*lReal + lImag*lImag;
        return new ComplexDouble((rReal*lReal + rImag*lImag)/div, (rImag*lReal - rReal*lImag)/div);
    }
    @Override public ComplexDouble ldiv(double aReal           ) {
        final double lReal = mReal, lImag = mImag;
        final double div = lReal*lReal + lImag*lImag;
        return new ComplexDouble((aReal*lReal)/div, (-aReal*lImag)/div);
    }
    @Override public void ldiv2this(IComplexDouble aComplex) {
        final double lReal = mReal,           lImag = mImag;
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = lReal*lReal + lImag*lImag;
        mReal = (rReal*lReal + rImag*lImag)/div;
        mImag = (rImag*lReal - rReal*lImag)/div;
    }
    @Override public void ldiv2this(ComplexDouble aComplex ) {
        final double lReal = mReal,          lImag = mImag;
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = lReal*lReal + lImag*lImag;
        mReal = (rReal*lReal + rImag*lImag)/div;
        mImag = (rImag*lReal - rReal*lImag)/div;
    }
    @Override public void ldiv2this(double aReal           ) {
        final double lReal = mReal, lImag = mImag;
        final double div = lReal*lReal + lImag*lImag;
        mReal = (aReal*lReal)/div;
        mImag = (-aReal*lImag)/div;
    }
    
    @Override public double abs() {return MathEX.Fast.sqrt(mReal*mReal + mImag*mImag);}
    @Override public ComplexDouble conj() {return new ComplexDouble(mReal, -mImag);}
}
