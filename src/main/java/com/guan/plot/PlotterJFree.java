package com.guan.plot;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.RendererChangeEvent;
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

import static com.guan.code.CS.COLOR_;

/**
 * @author liqa
 * <p> 基于 JFreeChart 实现的 plot 类 </p>
 */
public class PlotterJFree implements IPlotter {
    protected class LineJFree extends AbstractLine {
        final int mID;
        Paint mPaint;
        LineJFree(int aID) {
            mID = aID;
            mPaint = COLOR_(aID);
            mLineRender.setSeriesPaint(mID, mPaint);
            
            // 设置默认的线型，为了避免一些问题不在构造函数中调用自己的一些多态的方法
            mLineRender.setLegendLine(new Line2D.Double(-super.mStroke.getSize()*LEGEND_SIZE, 0.0, super.mStroke.getSize()*LEGEND_SIZE, 0.0));
            if (mLineType == LineType.NULL) {mLineRender.setSeriesLinesVisible(mID, false);}
            else {mLineRender.setSeriesStroke(mID, super.mStroke); mLineRender.setSeriesLinesVisible(mID, true);}
            if (mMarkerType == MarkerType.NULL) {mLineRender.setSeriesShapesVisible(mID, false);}
            else {mLineRender.setSeriesShape(mID, super.mShape); mLineRender.setSeriesShapesVisible(mID, true);}
        }
        
        @Override public ILine color(Paint aPaint) {mPaint = aPaint; mLineRender.notifyListeners(new RendererChangeEvent(mLineRender)); return this;}
        @Override public ILine markerColor(Paint aPaint) {mLineRender.setSeriesPaint(mID, aPaint); return this;}
        
        @Override protected void onLineTypeChange(LineType aOldLineType, LineType aNewLineType) {
            if (aNewLineType == LineType.NULL) {mLineRender.setSeriesLinesVisible(mID, false);}
            else {mLineRender.setSeriesStroke(mID, super.mStroke); mLineRender.setSeriesLinesVisible(mID, true);}
        }
        @Override protected void onMarkerTypeChange(MarkerType aOldMarkerType, MarkerType aNewMarkerType) {
            if (aNewMarkerType == MarkerType.NULL) {mLineRender.setSeriesShapesVisible(mID, false);}
            else {mLineRender.setSeriesShape(mID, super.mShape); mLineRender.setSeriesShapesVisible(mID, true);}
        }
        @Override protected void onLineWidthChange(double aOldLineWidth, double aNewLineWidth) {
            // 线宽变化时需要同步调整 Legend 的长度
            mLineRender.setLegendLine(new Line2D.Double(-aNewLineWidth*LEGEND_SIZE, 0.0, aNewLineWidth*LEGEND_SIZE, 0.0));
        }
        @Override protected void onMarkerSizeChange(double aOldMarkerSize, double aNewMarkerSize) {/**/}
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
                return tLegendItem;
            }
        };
        mLineRender.setDrawSeriesLineAsPath(true); // 指定按照路径的方式绘制
        mPlot.setRenderer(mLineRender); // 指定修改后的 renderer
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
    @Override public IPlotter xRange(double aMin, double aMax) {mXAxis.setRange(aMin, aMax); return this;}
    @Override public IPlotter yRange(double aMin, double aMax) {mYAxis.setRange(aMin, aMax); return this;}
    
    /** 设置 tick 间隔 */
    @Override public IPlotter xTick(double aTick) {((NumberAxis)mXAxis).setTickUnit(new NumberTickUnit(aTick)); return this;}
    @Override public IPlotter yTick(double aTick) {((NumberAxis)mYAxis).setTickUnit(new NumberTickUnit(aTick)); return this;}
    
    /** 添加绘制数据 */
    @Override
    public ILine plot(Iterable<? extends Number> aX, Iterable<? extends Number> aY, String aName) {
        // 创建数据
        XYSeries tSeries = new XYSeries(aName, false, true); // 不做排序以及允许重复数值
        Iterator<? extends Number> itx = aX.iterator();
        Iterator<? extends Number> ity = aY.iterator();
        while (itx.hasNext() && ity.hasNext()) {
            // 需要将过大的值改为 NaN
            Number tX = itx.next();
            Number tY = ity.next();
            tSeries.add(tX, tY);
        }
        // 添加数据
        mLinesData.addSeries(tSeries);
        // 创建曲线
        LineJFree tLine = new LineJFree(mLines.size());
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
