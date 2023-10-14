package com.jtool.math.function;

/**
 * 等间距的 IFunc1，等价于旧版本中原本的 IFunc1
 * @author liqa
 */
public interface IEqualIntervalFunc2 extends IFunc2 {
    @Override default double dx(int aI) {return dx();}
    @Override default double dy(int aJ) {return dy();}
    double dx();
    double dy();
}
