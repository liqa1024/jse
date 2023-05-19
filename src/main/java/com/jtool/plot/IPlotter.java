package com.jtool.plot;


import com.jtool.code.UT;
import org.jetbrains.annotations.VisibleForTesting;

import java.awt.*;
import java.util.Collection;

/**
 * @author liqa
 * <p> 抽象的绘制器，实现类似 matlab 的 plot 或者 python 的 matplotlib 中较为简单的绘制方法 </p>
 * <p> 目前这个仅用于绘制二维的折线图 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public interface IPlotter {
    /** 内部使用 */
    default String defaultLineName_() {return null;}
    
    /** 绘制图像；这里和 matlab 不同，当不指定 x 时会从 0 开始 */
    default ILine plot(                                                   double[] aY) {return plot(aY, defaultLineName_());}
    default ILine plot(                  double[] aX,                     double[] aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(Iterable<? extends Number> aX,                     double[] aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                  double[] aX, Iterable  <? extends Number> aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                               Collection<? extends Number> aY) {return plot(aY, defaultLineName_());}
    default ILine plot(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                                                   double[] aY, String aName) {return plot(UT.Code.range_(aY.length), aY, aName);}
    default ILine plot(                  double[] aX,                     double[] aY, String aName) {return plot(UT.Code.asList(aX), UT.Code.asList(aY), aName);}
    default ILine plot(Iterable<? extends Number> aX,                     double[] aY, String aName) {return plot(aX, UT.Code.asList(aY), aName);}
    default ILine plot(                  double[] aX, Iterable  <? extends Number> aY, String aName) {return plot(UT.Code.asList(aX), aY, aName);}
    default ILine plot(                               Collection<? extends Number> aY, String aName) {return plot(UT.Code.range_(aY.size()), aY, aName);}
            ILine plot(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName);
    
    
    /** 显示结果 */
    default IFigure show() {return show("figure");}
    IFigure show(String aName);
    
    /** 设置标题等 */
    IPlotter title(String aTitle);
    IPlotter xLabel(String aXLabel);
    IPlotter yLabel(String aYLabel);
    @VisibleForTesting default IPlotter xlabel(String aXLabel) {return xLabel(aXLabel);}
    @VisibleForTesting default IPlotter ylabel(String aYLabel) {return yLabel(aYLabel);}
    
    IPlotter fontTitle(Font aFont);
    IPlotter fontLabel(Font aFont);
    IPlotter fontLegend(Font aFont);
    IPlotter fontTick(Font aFont);
    
    /** 设置绘制范围 */
    IPlotter xRange(double aMin, double aMax);
    IPlotter yRange(double aMin, double aMax);
    default IPlotter axis(double aMin, double aMax) {return axis(aMin, aMax, aMin, aMax);}
    default IPlotter axis(double aXMin, double aXMax, double aYMin, double aYMax) {return xRange(aXMin, aXMax).yRange(aYMin, aYMax);}
    @VisibleForTesting default IPlotter axis(double[] aAxis) {return axis(aAxis[0], aAxis[1], aAxis[2], aAxis[3]);}
    
    /** 设置 tick 间隔 */
    IPlotter xTick(double aTick);
    IPlotter yTick(double aTick);
    default IPlotter tick(double aTick) {return tick(aTick, aTick);}
    default IPlotter tick(double aXTick, double aYTick) {return xTick(aXTick).yTick(aYTick);}
}
