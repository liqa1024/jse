package com.jtool.math.function;

/**
 * 等间距的 IFunc1，等价于旧版本中原本的 IFunc1
 * @author liqa
 */
public interface IEqualIntervalFunc1 extends IFunc1 {
    @Override default double dx(int aI) {return dx();}
    double dx();
}
