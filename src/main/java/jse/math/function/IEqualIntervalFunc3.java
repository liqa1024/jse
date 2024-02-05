package jse.math.function;

/**
 * 等间距的 IFunc1，等价于旧版本中原本的 IFunc1
 * @author liqa
 */
public interface IEqualIntervalFunc3 extends IFunc3 {
    @Override default double dx(int aI) {return dx();}
    @Override default double dy(int aJ) {return dy();}
    @Override default double dz(int aK) {return dz();}
    double dx();
    double dy();
    double dz();
}
