package com.jtool.plot;

import com.jtool.code.collection.AbstractCollections;
import com.jtool.math.function.IFunc1;
import com.jtool.math.function.IFunc1Subs;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.awt.*;
import java.io.IOException;
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
    default ILine plot(                   IVector aX,                     double[] aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(Iterable<? extends Number> aX,                     double[] aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                                                    IVector aY) {return plot(aY, defaultLineName_());}
    default ILine plot(                  double[] aX,                      IVector aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                   IVector aX,                      IVector aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(Iterable<? extends Number> aX,                      IVector aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                                                     IFunc1 aY) {return plot(aY, defaultLineName_());}
    default ILine plot(                  double[] aX,                   IFunc1Subs aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                   IVector aX,                   IFunc1Subs aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(Iterable<? extends Number> aX,                   IFunc1Subs aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                               Collection<? extends Number> aY) {return plot(aY, defaultLineName_());}
    default ILine plot(                  double[] aX, Iterable  <? extends Number> aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                   IVector aX, Iterable  <? extends Number> aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {return plot(aX, aY, defaultLineName_());}
    default ILine plot(                                                   double[] aY, String aName) {return plot(AbstractCollections.range_(aY.length), aY, aName);}
    default ILine plot(                  double[] aX,                     double[] aY, String aName) {return plot(AbstractCollections.from(aX), AbstractCollections.from(aY), aName);}
    default ILine plot(                   IVector aX,                     double[] aY, String aName) {return plot(aX.iterable(), AbstractCollections.from(aY), aName);}
    default ILine plot(Iterable<? extends Number> aX,                     double[] aY, String aName) {return plot(aX, AbstractCollections.from(aY), aName);}
    default ILine plot(                                                    IVector aY, String aName) {return plot(AbstractCollections.range_(aY.size()), aY, aName);}
    default ILine plot(                  double[] aX,                      IVector aY, String aName) {return plot(AbstractCollections.from(aX), aY.iterable(), aName);}
    default ILine plot(                   IVector aX,                      IVector aY, String aName) {return plot(aX.iterable(), aY.iterable(), aName);}
    default ILine plot(Iterable<? extends Number> aX,                      IVector aY, String aName) {return plot(aX, aY.iterable(), aName);}
    default ILine plot(                                                     IFunc1 aY, String aName) {return plot(aY.x(), aY.f(), aName);}
    default ILine plot(                  double[] aX,                   IFunc1Subs aY, String aName) {return plot(AbstractCollections.from(aX), aY, aName);}
    default ILine plot(                   IVector aX,                   IFunc1Subs aY, String aName) {return plot(aX.iterable(), aY, aName);}
    default ILine plot(Iterable<? extends Number> aX,                   IFunc1Subs aY, String aName) {return plot(aX, AbstractCollections.map(aX, x -> aY.subs(x.doubleValue())), aName);}
    default ILine plot(                               Collection<? extends Number> aY, String aName) {return plot(AbstractCollections.range_(aY.size()), aY, aName);}
    default ILine plot(                  double[] aX, Iterable  <? extends Number> aY, String aName) {return plot(AbstractCollections.from(aX), aY, aName);}
    default ILine plot(                   IVector aX, Iterable  <? extends Number> aY, String aName) {return plot(aX.iterable(), aY, aName);}
            ILine plot(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName);
    
    default ILine loglog(                                                   double[] aY) {return xScaleLog().yScaleLog().plot(aY);}
    default ILine loglog(                  double[] aX,                     double[] aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                   IVector aX,                     double[] aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(Iterable<? extends Number> aX,                     double[] aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                                                    IVector aY) {return xScaleLog().yScaleLog().plot(aY);}
    default ILine loglog(                  double[] aX,                      IVector aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                   IVector aX,                      IVector aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(Iterable<? extends Number> aX,                      IVector aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                                                     IFunc1 aY) {return xScaleLog().yScaleLog().plot(aY);}
    default ILine loglog(                  double[] aX,                   IFunc1Subs aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                   IVector aX,                   IFunc1Subs aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(Iterable<? extends Number> aX,                   IFunc1Subs aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                               Collection<? extends Number> aY) {return xScaleLog().yScaleLog().plot(aY);}
    default ILine loglog(                  double[] aX, Iterable  <? extends Number> aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                   IVector aX, Iterable  <? extends Number> aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {return xScaleLog().yScaleLog().plot(aX, aY);}
    default ILine loglog(                                                   double[] aY, String aName) {return xScaleLog().yScaleLog().plot(aY, aName);}
    default ILine loglog(                  double[] aX,                     double[] aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(                   IVector aX,                     double[] aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(Iterable<? extends Number> aX,                     double[] aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(                                                    IVector aY, String aName) {return xScaleLog().yScaleLog().plot(aY, aName);}
    default ILine loglog(                  double[] aX,                      IVector aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(                   IVector aX,                      IVector aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(Iterable<? extends Number> aX,                      IVector aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(                                                     IFunc1 aY, String aName) {return xScaleLog().yScaleLog().plot(aY, aName);}
    default ILine loglog(                  double[] aX,                   IFunc1Subs aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(                   IVector aX,                   IFunc1Subs aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(Iterable<? extends Number> aX,                   IFunc1Subs aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(                               Collection<? extends Number> aY, String aName) {return xScaleLog().yScaleLog().plot(aY, aName);}
    default ILine loglog(                  double[] aX, Iterable  <? extends Number> aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(                   IVector aX, Iterable  <? extends Number> aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    default ILine loglog(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName) {return xScaleLog().yScaleLog().plot(aX, aY, aName);}
    
    default ILine semilogx(                                                   double[] aY) {return xScaleLog().plot(aY);}
    default ILine semilogx(                  double[] aX,                     double[] aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                   IVector aX,                     double[] aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(Iterable<? extends Number> aX,                     double[] aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                                                    IVector aY) {return xScaleLog().plot(aY);}
    default ILine semilogx(                  double[] aX,                      IVector aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                   IVector aX,                      IVector aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(Iterable<? extends Number> aX,                      IVector aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                                                     IFunc1 aY) {return xScaleLog().plot(aY);}
    default ILine semilogx(                  double[] aX,                   IFunc1Subs aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                   IVector aX,                   IFunc1Subs aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(Iterable<? extends Number> aX,                   IFunc1Subs aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                               Collection<? extends Number> aY) {return xScaleLog().plot(aY);}
    default ILine semilogx(                  double[] aX, Iterable  <? extends Number> aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                   IVector aX, Iterable  <? extends Number> aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {return xScaleLog().plot(aX, aY);}
    default ILine semilogx(                                                   double[] aY, String aName) {return xScaleLog().plot(aY, aName);}
    default ILine semilogx(                  double[] aX,                     double[] aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(                   IVector aX,                     double[] aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(Iterable<? extends Number> aX,                     double[] aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(                                                    IVector aY, String aName) {return xScaleLog().plot(aY, aName);}
    default ILine semilogx(                  double[] aX,                      IVector aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(                   IVector aX,                      IVector aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(Iterable<? extends Number> aX,                      IVector aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(                                                     IFunc1 aY, String aName) {return xScaleLog().plot(aY, aName);}
    default ILine semilogx(                  double[] aX,                   IFunc1Subs aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(                   IVector aX,                   IFunc1Subs aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(Iterable<? extends Number> aX,                   IFunc1Subs aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(                               Collection<? extends Number> aY, String aName) {return xScaleLog().plot(aY, aName);}
    default ILine semilogx(                  double[] aX, Iterable  <? extends Number> aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(                   IVector aX, Iterable  <? extends Number> aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    default ILine semilogx(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName) {return xScaleLog().plot(aX, aY, aName);}
    
    default ILine semilogy(                                                   double[] aY) {return yScaleLog().plot(aY);}
    default ILine semilogy(                  double[] aX,                     double[] aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                   IVector aX,                     double[] aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(Iterable<? extends Number> aX,                     double[] aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                                                    IVector aY) {return yScaleLog().plot(aY);}
    default ILine semilogy(                  double[] aX,                      IVector aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                   IVector aX,                      IVector aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(Iterable<? extends Number> aX,                      IVector aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                                                     IFunc1 aY) {return yScaleLog().plot(aY);}
    default ILine semilogy(                  double[] aX,                   IFunc1Subs aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                   IVector aX,                   IFunc1Subs aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(Iterable<? extends Number> aX,                   IFunc1Subs aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                               Collection<? extends Number> aY) {return yScaleLog().plot(aY);}
    default ILine semilogy(                  double[] aX, Iterable  <? extends Number> aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                   IVector aX, Iterable  <? extends Number> aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(Iterable<? extends Number> aX, Iterable  <? extends Number> aY) {return yScaleLog().plot(aX, aY);}
    default ILine semilogy(                                                   double[] aY, String aName) {return yScaleLog().plot(aY, aName);}
    default ILine semilogy(                  double[] aX,                     double[] aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(                   IVector aX,                     double[] aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(Iterable<? extends Number> aX,                     double[] aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(                                                    IVector aY, String aName) {return yScaleLog().plot(aY, aName);}
    default ILine semilogy(                  double[] aX,                      IVector aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(                   IVector aX,                      IVector aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(Iterable<? extends Number> aX,                      IVector aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(                                                     IFunc1 aY, String aName) {return yScaleLog().plot(aY, aName);}
    default ILine semilogy(                  double[] aX,                   IFunc1Subs aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(                   IVector aX,                   IFunc1Subs aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(Iterable<? extends Number> aX,                   IFunc1Subs aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(                               Collection<? extends Number> aY, String aName) {return yScaleLog().plot(aY, aName);}
    default ILine semilogy(                  double[] aX, Iterable  <? extends Number> aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(                   IVector aX, Iterable  <? extends Number> aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    default ILine semilogy(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName) {return yScaleLog().plot(aX, aY, aName);}
    
    String DEFAULT_FIGURE_NAME = "figure";
    /** 显示结果 */
    default IFigure show() {return show(DEFAULT_FIGURE_NAME);}
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
    
    /** 设置绘图的边距 */
    IPlotter insets(double aTop, double aLeft, double aBottom, double aRight);
    IPlotter insetsTop(double aTop);
    IPlotter insetsLeft(double aLeft);
    IPlotter insetsBottom(double aBottom);
    IPlotter insetsRight(double aRight);
    
    /** 设置轴的类型 */
    IPlotter xScaleLog();
    IPlotter yScaleLog();
    
    /** 直接保存结果 */
    void save(@Nullable String aFilePath, int aWidth, int aHeight) throws IOException;
    default void save(@Nullable String aFilePath) throws IOException {save(aFilePath, 1024, 768);}
    default void save() throws IOException {save(null);}
}
