package jse.math;

/**
 * 增加一个中间层来统一 {@code toString()} 方法的实现
 * @author liqa
 */
public abstract class AbstractComplexDouble implements IComplexDouble {
    /** print */
    @Override public String toString() {
        double tReal = real();
        double tImag = imag();
        return Double.compare(tImag, 0.0)>=0 ? String.format("%.4g + %.4gi", tReal, tImag) : String.format("%.4g - %.4gi", tReal, -tImag);
    }
    
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof IComplexDouble)) return false;
        
        IComplexDouble tComplexDouble = (IComplexDouble)aRHS;
        return Double.compare(real(), tComplexDouble.real())==0 && Double.compare(imag(), tComplexDouble.imag())==0;
    }
    @Override public final int hashCode() {
        return hashCode(real(), imag());
    }
    public static int hashCode(double aReal, double aImag) {
        return 31*Double.hashCode(aReal) + Double.hashCode(aImag);
    }
}
