package jse.math;


/**
 * 复数类，实际是类似结构体的设计，因此所有成员直接 public，
 * 为了使用方便这里不使用常规结构使用的命名规范
 * @author liqa
 */
public interface ISettableComplexDouble extends IComplexDouble {
    /** 考虑到性能以及复数的简单性，不返回自身 */
    void setReal(double aReal);
    void setImag(double aImag);
    default void setComplexDouble(double aReal, double aImag) {setReal(aReal); setImag(aImag);}
    default void setNormPhase(double aNorm, double aPhase) {setComplexDouble(aNorm * MathEX.Fast.cos(aPhase), aNorm * MathEX.Fast.sin(aPhase));}
    
    
    default void plus2this(IComplexDouble aComplex) {setComplexDouble(real() + aComplex.real(), imag() + aComplex.imag());}
    default void plus2this(ComplexDouble aComplex ) {setComplexDouble(real() + aComplex.mReal , imag() + aComplex.mImag );}
    default void plus2this(double aReal           ) {setReal(real() + aReal          );                                   }
    
    default void minus2this(IComplexDouble aComplex) {setComplexDouble(real() - aComplex.real(), imag() - aComplex.imag());}
    default void minus2this(ComplexDouble aComplex ) {setComplexDouble(real() - aComplex.mReal , imag() - aComplex.mImag );}
    default void minus2this(double aReal           ) {setReal(real() - aReal          );                                   }
    
    default void lminus2this(IComplexDouble aComplex) {setComplexDouble(aComplex.real() - real(), aComplex.imag() - imag());}
    default void lminus2this(ComplexDouble aComplex ) {setComplexDouble(aComplex.mReal  - real(), aComplex.mImag  - imag());}
    default void lminus2this(double aReal           ) {setComplexDouble(aReal           - real(),                 - imag());}
    
    default void multiply2this(IComplexDouble aComplex) {
        final double lReal = real(),          lImag = imag();
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        setComplexDouble(lReal*rReal - lImag*rImag, lImag*rReal + lReal*rImag);
    }
    default void multiply2this(ComplexDouble aComplex ) {
        final double lReal = real(),         lImag = imag();
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        setComplexDouble(lReal*rReal - lImag*rImag, lImag*rReal + lReal*rImag);
    }
    default void multiply2this(double aReal           ) {
        setComplexDouble(real() * aReal, imag() * aReal);
    }
    
    default void div2this(IComplexDouble aComplex) {
        final double lReal = real(),          lImag = imag();
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = rReal*rReal + rImag*rImag;
        setComplexDouble((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
    }
    default void div2this(ComplexDouble aComplex ) {
        final double lReal = real(),         lImag = imag();
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = rReal*rReal + rImag*rImag;
        setComplexDouble((lReal*rReal + lImag*rImag)/div, (lImag*rReal - lReal*rImag)/div);
    }
    default void div2this(double aReal          ) {
        setComplexDouble(real() / aReal, imag() / aReal);
    }
    
    default void ldiv2this(IComplexDouble aComplex) {
        final double lReal = real(),          lImag = imag();
        final double rReal = aComplex.real(), rImag = aComplex.imag();
        final double div = lReal*lReal + lImag*lImag;
        setComplexDouble((rReal*lReal + rImag*lImag)/div, (rImag*lReal - rReal*lImag)/div);
    }
    default void ldiv2this(ComplexDouble aComplex ) {
        final double lReal = real(),         lImag = imag();
        final double rReal = aComplex.mReal, rImag = aComplex.mImag;
        final double div = lReal*lReal + lImag*lImag;
        setComplexDouble((rReal*lReal + rImag*lImag)/div, (rImag*lReal - rReal*lImag)/div);
    }
    default void ldiv2this(double aReal          ) {
        final double lReal = real(), lImag = imag();
        final double div = lReal*lReal + lImag*lImag;
        setComplexDouble((aReal*lReal)/div, (-aReal*lImag)/div);
    }
    
    default void negative2this() {setComplexDouble(-real(), -imag());}
    default void conj2this() {setImag(-imag());}
}
