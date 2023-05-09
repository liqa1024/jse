package com.guan.plot;


/**
 * @author liqa
 * <p> 用来方便的获取 IPlot，可以不受具体实现的影响 </p>
 */
public class Plotters {
    /** 目前默认使用 JFreeChart 的实现 */
    public static IPlotter get() {return getJFree();}
    public static PlotterJFree getJFree() {return new PlotterJFree();}
}
