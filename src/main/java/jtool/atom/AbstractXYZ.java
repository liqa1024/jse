package jtool.atom;

/**
 * 增加一个中间层来统一 {@code toString()} 方法的实现
 * @author liqa
 */
public abstract class AbstractXYZ implements IXYZ {
    /** print */
    @Override public String toString() {return String.format("(%.4g, %.4g, %.4g)", x(), y(), z());}
}
