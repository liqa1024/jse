package jse.plot;

/**
 * @author liqa
 * <p> 用来方便的获取 IPlot，可以不受具体实现的影响 </p>
 */
public class Plotters {
    /**
     * 目前默认使用 JFreeChart 的实现，这里约定：
     * 对于实例类内部的静态方法来构造自身的，如果有输入参数，默认行为使用 of；
     * 对于实例类内部的静态方法来构造自身的，如果没有输入参数，默认行为使用 create；
     * 对于 xxxs 之类的工具类中的静态方法统一构造的，如果有输入参数，对于默认行为使用 from；
     * 对于 xxxs 之类的工具类中的静态方法统一构造的，如果没有输入参数，对于默认行为使用 get
     */
    public static IPlotter get() {return jFree();}
    public static PlotterJFree jFree() {return new PlotterJFree();}
}
