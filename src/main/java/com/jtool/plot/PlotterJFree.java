package com.jtool.plot;

import com.jtool.code.UT;
import com.jtool.math.MathEX;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import static com.jtool.plot.Shapes.*;
import static com.jtool.plot.Strokes.*;
import static com.jtool.plot.Colors.COLOR_;
import static java.awt.Color.*;


/**
 * @author liqa
 * <p> 基于 JFreeChart 实现的 plot 类 </p>
 * <p> 暂不考虑对数坐标的情况 </p>
 */
public class PlotterJFree implements IPlotter {
    protected class LineJFree extends AbstractLine {
        final int mID;
        final String mName;
        final Iterable<Double> mX, mY;
        Paint mPaint;
        Shape mLegendLine;
        
        LineJFree(int aID, Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName) {
            mID = aID;
            mName = aName;
            mX = UT.Code.map(aX, Number::doubleValue);
            mY = UT.Code.map(aY, Number::doubleValue);
            mPaint = COLOR_(aID);
            mLineRender.setSeriesPaint(mID, mPaint);
            mLegendLine = new Line2D.Double(-super.mLineStroke.getSize()*LEGEND_SIZE, 0.0, super.mLineStroke.getSize()*LEGEND_SIZE, 0.0);
            
            // 设置默认的线型，为了避免一些问题不在构造函数中调用自己的一些多态的方法
            if (mLineType == LineType.NULL) {mLineRender.setSeriesLinesVisible(mID, false);}
            else {mLineRender.setSeriesStroke(mID, super.mLineStroke); mLineRender.setSeriesLinesVisible(mID, true);}
            if (mMarkerType == MarkerType.NULL) {mLineRender.setSeriesShapesVisible(mID, false);}
            else {mLineRender.setSeriesShape(mID, super.mMarkerShape); mLineRender.setSeriesOutlineStroke(mID, DEFAULT_MARKER_STROKE); mLineRender.setSeriesShapesVisible(mID, true);}
        }
        
        @Override public ILine color(Paint aPaint) {mPaint = aPaint; mLineRender.setSeriesPaint(mID, aPaint); return this;} // 会覆盖掉 markerColor 的颜色设置
        @Override public ILine markerColor(Paint aPaint) {mLineRender.setSeriesPaint(mID, aPaint); return this;}
        
        @Override protected void onLineTypeChange(LineType aOldLineType, LineType aNewLineType) {
            if (aNewLineType == LineType.NULL) {mLineRender.setSeriesLinesVisible(mID, false);}
            else {mLineRender.setSeriesStroke(mID, super.mLineStroke); mLineRender.setSeriesLinesVisible(mID, true);}
        }
        @Override protected void onMarkerTypeChange(MarkerType aOldMarkerType, MarkerType aNewMarkerType) {
            if (aNewMarkerType == MarkerType.NULL) {mLineRender.setSeriesShapesVisible(mID, false);}
            else {mLineRender.setSeriesShape(mID, super.mMarkerShape); mLineRender.setSeriesOutlineStroke(mID, getMarkerStroke(aNewMarkerType, super.mMarkerShape.getSize())); mLineRender.setSeriesShapesVisible(mID, true);}
        }
        @Override protected void onLineWidthChange(double aOldLineWidth, double aNewLineWidth) {
            // 线宽变化时需要同步调整 Legend 的长度
            mLegendLine = new Line2D.Double(-aNewLineWidth*LEGEND_SIZE, 0.0, aNewLineWidth*LEGEND_SIZE, 0.0);
            mLineRender.notifyListeners(new RendererChangeEvent(mLineRender));
        }
        @Override protected void onMarkerSizeChange(double aOldMarkerSize, double aNewMarkerSize) {
            // Marker 大小变化时需要同步调整 Marker Stroke 的宽度
            mLineRender.setSeriesOutlineStroke(mID, getMarkerStroke(super.mMarkerType, aNewMarkerSize));
        }
    }
    
    
    /** 全局常量记录默认值 */
    public final static String TITLE = null, X_LABEL = null, Y_LABEL = null;
    public final static Font
          TITLE_FONT  = new Font("Times New Roman", Font.BOLD , 24)
        , LABEL_FONT  = new Font("Times New Roman", Font.PLAIN, 20)
        , LEGEND_FONT = new Font("Times New Roman", Font.PLAIN, 20)
        , TICK_FONT   = new Font("Times New Roman", Font.PLAIN, 18)
        ;
    public final static double LEGEND_SIZE = 16.0;
    
    /** 内部成员 */
    final XYSeriesCollection mLinesData;
    final JFreeChart mChart;
    final XYPlot mPlot;
    final ValueAxis mXAxis, mYAxis;
    final XYLineAndShapeRenderer mLineRender;
    final List<LineJFree> mLines;
    
    
    /** 一些默认的初始设定 */
    public PlotterJFree() {
        // 存储绘制的线
        mLines = new ArrayList<>();
        // 内部使用的成员
        mLinesData = new XYSeriesCollection();
        mChart = ChartFactory.createXYLineChart(TITLE, X_LABEL, Y_LABEL, mLinesData);
        mChart.setBackgroundPaint(WHITE); // 调整背景颜色
        mPlot = mChart.getXYPlot(); // 认定内部的 Plot 是 final 的
        // 默认线型
        mLineRender = new XYLineAndShapeRenderer() {
            @Override protected void drawFirstPassShape(Graphics2D g2, int pass, int series, int item, Shape shape) {
                g2.setStroke(getItemStroke(series, item));
                g2.setPaint(mLines.get(series).mPaint); // 重写绘制曲线获取颜色的部分
                g2.draw(shape);
            }
            // 重写获取 legendItem 的部分，修改线的颜色为正确颜色
            @Override public LegendItem getLegendItem(int datasetIndex, int series) {
                LegendItem tLegendItem = super.getLegendItem(datasetIndex, series);
                tLegendItem.setLinePaint(mLines.get(series).mPaint);
                tLegendItem.setLine(mLines.get(series).mLegendLine);
                return tLegendItem;
            }
        };
        mLineRender.setDrawSeriesLineAsPath(true); // 指定按照路径的方式绘制
        mPlot.setRenderer(mLineRender); // 指定修改后的 renderer
        mPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD); // 修改绘制顺序为常见的顺序
        mPlot.setBackgroundPaint(WHITE); // 调整背景颜色
        mPlot.setDomainGridlinePaint(GRAY); // 调整刻度线颜色
        mPlot.setRangeGridlinePaint(GRAY); // 调整刻度线颜色
        // x，y 轴
        mXAxis = mPlot.getDomainAxis();
        mYAxis = mPlot.getRangeAxis();
        
        // 设置标题字体
        fontTitle(TITLE_FONT);
        // 设置轴标签字体
        fontLabel(LABEL_FONT);
        // 设置图例字体
        fontLegend(LEGEND_FONT);
        // 设置轴刻度字体
        fontTick(TICK_FONT);
    }
    
    
    /** IPlotter stuffs */
    @Override public String defaultLineName_() {return "data-"+mLines.size();}
    
    /** 字体设置，由于 mChart 不一定会存这些信息，需要手动存这些 */
    public Font mTitleFont;
    @Override public IPlotter fontTitle(Font aFont) {
        mTitleFont = aFont;
        TextTitle tTitle = mChart.getTitle();
        if (tTitle != null) tTitle.setFont(mTitleFont);
        return this;
    }
    @Override public IPlotter fontLabel(Font aFont) {
        mXAxis.setLabelFont(aFont);
        mYAxis.setLabelFont(aFont);
        return this;
    }
    @Override public IPlotter fontLegend(Font aFont) {
        mLineRender.setDefaultLegendTextFont(aFont);
        return this;
    }
    @Override public IPlotter fontTick(Font aFont) {
        mXAxis.setTickLabelFont(aFont);
        mYAxis.setTickLabelFont(aFont);
        return this;
    }
    
    /** 设置标题等 */
    @Override public IPlotter title(String aTitle) {mChart.setTitle(aTitle); fontTitle(mTitleFont); return this;}
    @Override public IPlotter xLabel(String aXLabel) {mXAxis.setLabel(aXLabel); return this;}
    @Override public IPlotter yLabel(String aYLabel) {mYAxis.setLabel(aYLabel); return this;}
    
    /** 设置绘制范围 */
    @Override public IPlotter xRange(double aMin, double aMax) {
        // 先进行设置范围，非法返回会自动报错
        mXAxis.setRange(aMin, aMax);
        // 设置绘制范围后，需要手动调整绘制区域外的数据，因为 JFree 不会对此优化，从而可能会导致卡死的问题
        updateSeries_(aMin, Double.NEGATIVE_INFINITY, aMax, Double.POSITIVE_INFINITY);
        return this;
    }
    @Override public IPlotter yRange(double aMin, double aMax) {
        // 先进行设置范围，非法返回会自动报错
        mYAxis.setRange(aMin, aMax);
        // 设置绘制范围后，需要手动调整绘制区域外的数据，因为 JFree 不会对此优化，从而可能会导致卡死的问题
        updateSeries_(Double.NEGATIVE_INFINITY, aMin, Double.POSITIVE_INFINITY, aMax);
        return this;
    }
    @Override public IPlotter axis(double aXMin, double aXMax, double aYMin, double aYMax) {
        // 先进行设置范围，非法返回会自动报错
        mXAxis.setRange(aXMin, aXMax);
        mYAxis.setRange(aYMin, aYMax);
        // 设置绘制范围后，需要手动调整绘制区域外的数据，因为 JFree 不会对此优化，从而可能会导致卡死的问题
        updateSeries_(aXMin, aYMin, aXMax, aYMax);
        return this;
    }
    /** 内部使用，根据输入的 box 边界来更新数据，因为 JFree 不会对此优化，从而可能会导致卡死的问题 */
    private void updateSeries_(double aBoxXMin, double aBoxYMin, double aBoxXMax, double aBoxYMax) {
        // 用来保证不会出现刚好在边界情况
        double tSizeX = aBoxXMax - aBoxXMin; tSizeX *= 0.9975432489542136;
        double tSizeY = aBoxYMax - aBoxYMin; tSizeY *= 0.9986214753294532;
        aBoxXMin -= tSizeX; aBoxXMax += tSizeX;
        aBoxYMin -= tSizeY; aBoxYMax += tSizeY;
        // 直接遍历全部重设数据，因为可能会有多次调整范围的问题；直接清空旧的序列然后重新覆盖，操作会比较直接
        mLinesData.removeAllSeries();
        for (LineJFree tLine : mLines) {
            // 创建新的序列
            XYSeries tSeries = new XYSeries(tLine.mName, false, true);
            // 遍历获取新的值
            double tX0 = Double.NaN;
            double tY0 = Double.NaN;
            Iterator<Double> itx = tLine.mX.iterator();
            Iterator<Double> ity = tLine.mY.iterator();
            while (itx.hasNext() && ity.hasNext()) {
                double tX = itx.next();
                double tY = ity.next();
                // 检测位置是否合法，合法则直接添加，不合法则添加边界值或者 NaN
                if (tX>aBoxXMin && tX<aBoxXMax && tY>aBoxYMin && tY<aBoxYMax) {
                    tSeries.add(tX, tY);
                } else //noinspection StatementWithEmptyBody
                if (Double.isNaN(tX0) || Double.isNaN(tY0)) {
                    // 没有上一个值 NaN 则这个值不进行添加
                } else {
                    // 上一个值在范围内则只需要添加一个边界点
                    double[][] tInterPoints = MathEX.Graph.interRayBox2D_(aBoxXMin, aBoxYMin,  aBoxXMax, aBoxYMax, tX0, tY0, tX, tY);
                    // 遍历添加，不添加非法值
                    if (tInterPoints != null) for (double[] tXY : tInterPoints) tSeries.add(tXY[0], tXY[1]);
                }
                tX0 = tX;
                tY0 = tY;
            }
            // 添加修改绘制数据后的序列
            mLinesData.addSeries(tSeries);
        }
    }
    
    
    /** 设置 tick 间隔 */
    @Override public IPlotter xTick(double aTick) {((NumberAxis)mXAxis).setTickUnit(new NumberTickUnit(aTick)); return this;}
    @Override public IPlotter yTick(double aTick) {((NumberAxis)mYAxis).setTickUnit(new NumberTickUnit(aTick)); return this;}
    
    /** 添加绘制数据 */
    @Override
    public ILine plot(Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName) {
        // 创建数据
        XYSeries tSeries = new XYSeries(aName, false, true); // 不做排序以及允许重复数值
        Iterator<? extends Number> itx = aX.iterator();
        Iterator<? extends Number> ity = aY.iterator();
        while (itx.hasNext() && ity.hasNext()) tSeries.add(itx.next(), ity.next());
        // 添加数据
        mLinesData.addSeries(tSeries);
        // 创建曲线
        LineJFree tLine = new LineJFree(mLines.size(), aX, aY, aName);
        mLines.add(tLine);
        
        // 返回 LineJFree
        return tLine;
    }
    
    @Override
    public IFigure show(String aName) {
        // 显示图像
        final ChartFrame tFrame = new ChartFrame(aName, mChart);
        tFrame.pack();
        tFrame.setVisible(true);
        
        // 返回 IFigure
        return new IFigure() {
            @Override public IFigure name(String aName) {tFrame.setName(aName); return this;}
            @Override public IFigure size(int aWidth, int aHeight) {tFrame.setSize(aWidth, aHeight); return this;}
            @Override public IFigure location(int aX, int aY) {tFrame.setLocation(aX, aY); return this;}
        };
    }
}
