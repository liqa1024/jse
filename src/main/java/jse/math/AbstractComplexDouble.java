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
}
