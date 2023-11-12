package jtool.plot;

import jtool.code.UT;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import static jtool.plot.Colors.COLOR;
import static jtool.plot.Shapes.*;
import static jtool.plot.Strokes.*;
import static java.awt.Color.*;


/**
 * @author liqa
 * <p> 基于 JFreeChart 实现的 plot 类 </p>
 * <p> 暂不考虑对数坐标的情况 </p>
 */
public final class PlotterJFree implements IPlotter {
    protected class LineJFree extends AbstractLine {
        final int mID;
        final String mName;
        final Iterable<? extends Number> mX, mY;
        Shape mLegendLine;
        
        LineJFree(int aID, Iterable<? extends Number> aX, Iterable  <? extends Number> aY, String aName) {
            mID = aID;
            mName = aName;
            mX = aX;
            mY = aY;
            // 设置颜色，为了避免一些问题不在构造函数中调用自己的一些多态的方法
            Paint tPaint = COLOR(aID);
            mLineRender.setSeriesPaint(mID, tPaint);
            mLineRender.setSeriesFillPaint(mID, tPaint);
            mLineRender.setSeriesOutlinePaint(mID, tPaint);
            // 设置 mLegendLine，为了避免一些问题不在构造函数中调用自己的一些多态的方法
            mLegendLine = new Line2D.Double(-super.mLineStroke.getSize()*LEGEND_SIZE, 0.0, super.mLineStroke.getSize()*LEGEND_SIZE, 0.0);
            // 设置默认的线型，为了避免一些问题不在构造函数中调用自己的一些多态的方法
            mLineRender.setSeriesStroke(mID, super.mLineStroke);
            mLineRender.setSeriesShape(mID, super.mMarkerShape);
            mLineRender.setSeriesOutlineStroke(mID, super.mMarkerStroke);
            mLineRender.setSeriesLinesVisible(mID, mLineType!=LineType.NULL);
            mLineRender.setSeriesShapesVisible(mID, mMarkerType!=MarkerType.NULL);
        }
        
        @Override public ILine filled(boolean aFilled) {mLineRender.setSeriesShapesFilled(mID, aFilled); return this;}
        
        @Override public ILine color(Paint aPaint) {mLineRender.setSeriesPaint(mID, aPaint); mLineRender.setSeriesFillPaint(mID, aPaint); mLineRender.setSeriesOutlinePaint(mID, aPaint); return this;} // 会覆盖掉 markerColor 的颜色设置
        @Override public ILine lineColor(Paint aPaint) {mLineRender.setSeriesPaint(mID, aPaint); return this;} // 不会覆盖掉 markerColor 的颜色设置
        @Override public ILine markerColor(Paint aPaint) {mLineRender.setSeriesOutlinePaint(mID, aPaint); mLineRender.setSeriesFillPaint(mID, aPaint); return this;} // 会覆盖掉 markerFaceColor 的颜色设置
        @Override public ILine markerFaceColor(Paint aPaint) {filled(); mLineRender.setSeriesFillPaint(mID, aPaint); return this;}
        @Override public ILine markerEdgeColor(Paint aPaint) {mLineRender.setSeriesOutlinePaint(mID, aPaint); return this;}
        
        @Override protected void onLineTypeChange(LineType aOldLineType, LineType aNewLineType) {
            mLineRender.setSeriesStroke(mID, super.mLineStroke);
            mLineRender.setSeriesLinesVisible(mID, aNewLineType!=LineType.NULL);
        }
        @Override protected void onMarkerTypeChange(MarkerType aOldMarkerType, MarkerType aNewMarkerType) {
            mLineRender.setSeriesShape(mID, super.mMarkerShape);
            mLineRender.setSeriesShapesVisible(mID, aNewMarkerType!=MarkerType.NULL);
        }
        @Override protected void onLineWidthChange(double aOldLineWidth, double aNewLineWidth) {
            // 线宽变化时需要同步调整 Legend 的长度
            mLegendLine = new Line2D.Double(-aNewLineWidth*LEGEND_SIZE, 0.0, aNewLineWidth*LEGEND_SIZE, 0.0);
            mLineRender.notifyListeners(new RendererChangeEvent(mLineRender));
        }
        @Override protected void onMarkerSizeChange(double aOldMarkerSize, double aNewMarkerSize) {
            mLineRender.notifyListeners(new RendererChangeEvent(mLineRender));
        }
        @Override protected void onMarkerEdgeWidthChange(double aOldEdgeWidth, double aNewEdgeWidth) {
            mLineRender.notifyListeners(new RendererChangeEvent(mLineRender));
        }
        
        @Override public ILine noLegend() {mLineRender.setSeriesVisibleInLegend(mID, false); return this;}
        @Override public ILine showLegend() {mLineRender.setSeriesVisibleInLegend(mID, true); return this;}
    }
    
    /** 重写 NumberAxis 的方式修改默认的 tick 间距 */
    private final static class LargerTickUnitNumberAxis extends NumberAxis {
        public LargerTickUnitNumberAxis(String label) {super(label);}
        
        @Override protected void selectHorizontalAutoTickUnit(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
            TickUnit unit = getTickUnit();
            TickUnitSource tickUnitSource = getStandardTickUnits();
            
            double length = getRange().getLength();
            int count = (int) (length / unit.getSize());
            if (count < 3 || count > 40) {
                unit = tickUnitSource.getCeilingTickUnit(length / 10);
            }
            
            TickUnit unit1 = tickUnitSource.getCeilingTickUnit(unit);
            double tickLabelWidth = estimateMaximumTickLabelWidth(g2, unit1);
            double unit1Width = lengthToJava2D(unit1.getSize(), dataArea, edge);
            NumberTickUnit unit2 = (NumberTickUnit) unit1;
            double guess = ((tickLabelWidth+10.0) / unit1Width) * unit1.getSize(); // 修改这个来增大间距
            
            // 直接在这里限制 tick 的数目
            if (getRange().getLength() > 15.0*guess) guess = getRange().getLength()/15.0;
            
            if (Double.isFinite(guess)) {
                unit2 = (NumberTickUnit) tickUnitSource.getCeilingTickUnit(guess);
                double unit2Width = lengthToJava2D(unit2.getSize(), dataArea, edge);
                tickLabelWidth = estimateMaximumTickLabelWidth(g2, unit2);
                if (tickLabelWidth > unit2Width) {
                    unit2 = (NumberTickUnit) tickUnitSource.getLargerTickUnit(unit2);
                }
            }
            setTickUnit(unit2, false, false);
        }
        
        @Override protected void selectVerticalAutoTickUnit(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
            double tickLabelHeight = estimateMaximumTickLabelHeight(g2);
            
            TickUnitSource tickUnits = getStandardTickUnits();
            TickUnit unit1 = tickUnits.getCeilingTickUnit(getTickUnit());
            double unitHeight = lengthToJava2D(unit1.getSize(), dataArea, edge);
            double guess;
            if (unitHeight > 0) {
                guess = ((tickLabelHeight+20.0) / unitHeight) * unit1.getSize(); // 修改这个来增大间距
            } else {
                guess = getRange().getLength() / 20.0;
            }
            // 直接在这里限制 tick 的数目
            if (getRange().getLength() > 15.0*guess) guess = getRange().getLength()/15.0;
            
            NumberTickUnit unit2 = (NumberTickUnit) tickUnits.getCeilingTickUnit(guess);
            double unit2Height = lengthToJava2D(unit2.getSize(), dataArea, edge);
            
            tickLabelHeight = estimateMaximumTickLabelHeight(g2);
            if (tickLabelHeight > unit2Height) {
                unit2 = (NumberTickUnit) tickUnits.getLargerTickUnit(unit2);
            }
            setTickUnit(unit2, false, false);
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
    private final XYSeriesCollection mLinesData;
    private final JFreeChart mChart;
    private final XYPlot mPlot;
    private NumberAxis mXAxis, mYAxis;
    private final XYLineAndShapeRenderer mLineRender;
    private final List<LineJFree> mLines;
    
    private static volatile boolean INITIALIZED = false;
    /** 一些默认的初始设定 */
    public PlotterJFree() {
        // 先进行通用的初始化
        if (!INITIALIZED) {
            INITIALIZED = true;
            // 禁用自动缩放保证绘制结果一致
            System.setProperty("sun.java2d.uiScale", "1");
        }
        
        // 存储绘制的线
        mLines = new ArrayList<>();
        // 内部使用的成员
        mLinesData = new XYSeriesCollection();
        
        // x，y 轴
        mXAxis = new LargerTickUnitNumberAxis(X_LABEL);
        mYAxis = new LargerTickUnitNumberAxis(Y_LABEL);
        // 关闭 autoRange 包含零
        mXAxis.setAutoRangeIncludesZero(false);
        mYAxis.setAutoRangeIncludesZero(false);
        // 线渲染器，默认线型
        mLineRender = new XYLineAndShapeRenderer(true, true) {
            // 重写获取 legendItem 的部分，修改线长度
            @Override public LegendItem getLegendItem(int datasetIndex, int series) {
                LegendItem tLegendItem = super.getLegendItem(datasetIndex, series);
                tLegendItem.setLine(mLines.get(series).mLegendLine);
                return tLegendItem;
            }
        };
        mLineRender.setUseFillPaint(true); // 指定填充颜色独立
        mLineRender.setDefaultShapesFilled(false); // 现在默认不开启 fill
        mLineRender.setUseOutlinePaint(true); // 使用独立的 outline 颜色
        mLineRender.setDrawOutlines(true); // 默认绘制 outline
        mLineRender.setDrawSeriesLineAsPath(true); // 指定按照路径的方式绘制
        // 绘制器
        mPlot = new XYPlot(mLinesData, mXAxis, mYAxis, mLineRender);
        mPlot.setOrientation(PlotOrientation.VERTICAL);
        mPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD); // 修改绘制顺序为常见的顺序
        // 默认开启 tooltips
        mLineRender.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
        // 整个图表
        mChart = new JFreeChart(TITLE, TITLE_FONT, mPlot, true);
        
        // 目前是在默认的主题上直接修改
        ChartFactory.getChartTheme().apply(mChart);
        mChart.setBackgroundPaint(WHITE); // 调整背景颜色
        mPlot.setBackgroundPaint(WHITE); // 调整背景颜色
        mPlot.setDomainGridlinePaint(GRAY); // 调整刻度线颜色
        mPlot.setRangeGridlinePaint(GRAY); // 调整刻度线颜色
        mPlot.setInsets(new RectangleInsets(10.0, 20.0, 10.0, 40.0)); // 增大默认边框
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
        updateSeries_();
        return this;
    }
    @Override public IPlotter yRange(double aMin, double aMax) {
        // 先进行设置范围，非法返回会自动报错
        mYAxis.setRange(aMin, aMax);
        // 设置绘制范围后，需要手动调整绘制区域外的数据，因为 JFree 不会对此优化，从而可能会导致卡死的问题
        updateSeries_();
        return this;
    }
    @Override public IPlotter axis(double aXMin, double aXMax, double aYMin, double aYMax) {
        // 先进行设置范围，非法返回会自动报错
        mXAxis.setRange(aXMin, aXMax);
        mYAxis.setRange(aYMin, aYMax);
        // 设置绘制范围后，需要手动调整绘制区域外的数据，因为 JFree 不会对此优化，从而可能会导致卡死的问题
        updateSeries_();
        return this;
    }
    /** 内部使用，根据输入的 box 边界来更新数据，因为 JFree 不会对此优化，从而可能会导致卡死的问题 */
    private void updateSeries_() {
        // 直接遍历全部重设数据，因为可能会有多次调整范围的问题；直接清空旧的序列然后重新覆盖，操作会比较直接
        mLinesData.removeAllSeries();
        // 添加修改绘制数据后的序列
        for (LineJFree tLine : mLines) mLinesData.addSeries(getValidXYSeries_(tLine.mX, tLine.mY, tLine.mName));
    }
    private XYSeries getValidXYSeries_(Iterable<? extends Number> aX, Iterable<? extends Number> aY, String aName) {
        double tBoxXMin, tBoxXMax;
        if (mXAxis.isAutoRange()) {
            tBoxXMin = Double.NEGATIVE_INFINITY;
            tBoxXMax = Double.POSITIVE_INFINITY;
        } else {
            Range tRange = mXAxis.getRange();
            tBoxXMin = tRange.getLowerBound();
            tBoxXMax = tRange.getUpperBound();
        }
        double tBoxYMin, tBoxYMax;
        if (mYAxis.isAutoRange()) {
            tBoxYMin = Double.NEGATIVE_INFINITY;
            tBoxYMax = Double.POSITIVE_INFINITY;
        } else {
            Range tRange = mYAxis.getRange();
            tBoxYMin = tRange.getLowerBound();
            tBoxYMax = tRange.getUpperBound();
        }
        // 用来保证不会出现刚好在边界情况
        double tSizeX = tBoxXMax - tBoxXMin; tSizeX *= 0.9975432489542136;
        double tSizeY = tBoxYMax - tBoxYMin; tSizeY *= 0.9986214753294532;
        tBoxXMin -= tSizeX; tBoxXMax += tSizeX;
        tBoxYMin -= tSizeY; tBoxYMax += tSizeY;
        // 创建新的序列
        XYSeries rSeries = new XYSeries(aName, false, true); // 不做排序以及允许重复数值
        // 遍历获取新的值
        double tX0 = Double.NaN;
        double tY0 = Double.NaN;
        Iterator<? extends Number> itx = aX.iterator();
        Iterator<? extends Number> ity = aY.iterator();
        boolean tNeedInterPoint = false;
        while (itx.hasNext() && ity.hasNext()) {
            double tX = itx.next().doubleValue();
            double tY = ity.next().doubleValue();
            // 检测位置是否合法，合法则直接添加
            if (tX>tBoxXMin && tX<tBoxXMax && tY>tBoxYMin && tY<tBoxYMax) {
                if (tNeedInterPoint) {
                    // 如果标记需要插入则需要利用上一个点的数据来插入
                    double[][] tInterPoints = MathEX.Graph.interRayBox2D_(tBoxXMin, tBoxYMin, tBoxXMax, tBoxYMax, tX0, tY0, tX, tY);
                    // 遍历添加，不添加非法值
                    if (tInterPoints != null) for (double[] tXY : tInterPoints) if (tXY != null) rSeries.add(tXY[0], tXY[1]);
                }
                // 先插入边界点再插入自身
                rSeries.add(tX, tY);
                // 这个为合法的点，下一个点不需要插入
                tNeedInterPoint = false;
            } else {
                // 没有上一个值 NaN 则这个值不进行添加
                if (!Double.isNaN(tX0) && !Double.isNaN(tY0)) {
                    // 这个点不在氛围内直接添加边界点
                    double[][] tInterPoints = MathEX.Graph.interRayBox2D_(tBoxXMin, tBoxYMin, tBoxXMax, tBoxYMax, tX0, tY0, tX, tY);
                    // 遍历添加，不添加非法值
                    if (tInterPoints != null) for (double[] tXY : tInterPoints) if (tXY != null) rSeries.add(tXY[0], tXY[1]);
                }
                // 这个为超出边界的点，标记下一个点需要插入
                tNeedInterPoint = true;
            }
            tX0 = tX;
            tY0 = tY;
        }
        return rSeries;
    }
    
    
    /** 设置 tick 间隔 */
    @Override public IPlotter xTick(double aTick) {mXAxis.setTickUnit(new NumberTickUnit(aTick)); return this;}
    @Override public IPlotter yTick(double aTick) {mYAxis.setTickUnit(new NumberTickUnit(aTick)); return this;}
    
    /** 设置绘图的边距 */
    @Override public IPlotter insets(double aTop, double aLeft, double aBottom, double aRight) {mPlot.setInsets(new RectangleInsets(aTop, aLeft, aBottom, aRight)); return this;}
    @Override public IPlotter insetsTop(double aTop) {RectangleInsets oInsets = mPlot.getInsets(); mPlot.setInsets(new RectangleInsets(aTop, oInsets.getLeft(), oInsets.getBottom(), oInsets.getRight())); return this;}
    @Override public IPlotter insetsLeft(double aLeft) {RectangleInsets oInsets = mPlot.getInsets(); mPlot.setInsets(new RectangleInsets(oInsets.getTop(), aLeft, oInsets.getBottom(), oInsets.getRight())); return this;}
    @Override public IPlotter insetsBottom(double aBottom) {RectangleInsets oInsets = mPlot.getInsets(); mPlot.setInsets(new RectangleInsets(oInsets.getTop(), oInsets.getLeft(), aBottom, oInsets.getRight())); return this;}
    @Override public IPlotter insetsRight(double aRight) {RectangleInsets oInsets = mPlot.getInsets(); mPlot.setInsets(new RectangleInsets(oInsets.getTop(), oInsets.getLeft(), oInsets.getBottom(), aRight)); return this;}
    
    /** 直接保存结果 */
    @Override public void save(@Nullable String aFilePath, int aWidth, int aHeight) throws IOException {
        if (aFilePath==null || aFilePath.isEmpty()) aFilePath = IPlotter.DEFAULT_FIGURE_NAME;
        if (!aFilePath.endsWith(".png")) aFilePath = aFilePath+".png";
        UT.IO.validPath(aFilePath); // 注意这里是调用外部接口保存，需要手动合法化路径
        ChartUtils.saveChartAsPNG(UT.IO.toFile(aFilePath), mChart, aWidth, aHeight);
    }
    
    /** 添加绘制数据 */
    @Override public ILine plot(Iterable<? extends Number> aX, Iterable<? extends Number> aY, String aName) {
        // 添加数据
        mLinesData.addSeries(getValidXYSeries_(aX, aY, aName));
        // 创建曲线
        LineJFree tLine = new LineJFree(mLines.size(), aX, aY, aName);
        mLines.add(tLine);
        // 返回 LineJFree
        return tLine;
    }
    
    @Override public IPlotter clear() {
        mLinesData.removeAllSeries();
        mLines.clear();
        return this;
    }
    @Override public void dispose() {
        if (mCurrentFigure != null) mCurrentFigure.dispose();
        clear();
    }
    
    
    /** 设置轴的类型 */
    @Override public IPlotter xScaleLog() {
        if (!(mXAxis instanceof LogarithmicAxis)) {
            NumberAxis oXAxis = mXAxis;
            mXAxis = new LogarithmicAxis(oXAxis.getLabel()); // log 轴的默认 tick 逻辑有点复杂，修改麻烦，这里懒得去弄了
            mXAxis.setAutoRangeIncludesZero(false);
            mXAxis.setLabelFont(oXAxis.getLabelFont());
            mXAxis.setTickLabelFont(oXAxis.getTickLabelFont());
            if (!oXAxis.isAutoRange()) mXAxis.setRange(oXAxis.getRange());
            mPlot.setDomainAxis(mXAxis);
        }
        return this;
    }
    @Override public IPlotter yScaleLog() {
        if (!(mYAxis instanceof LogarithmicAxis)) {
            NumberAxis oYAxis = mYAxis;
            mYAxis = new LogarithmicAxis(oYAxis.getLabel()); // log 轴的默认 tick 逻辑有点复杂，修改麻烦，这里懒得去弄了
            mYAxis.setAutoRangeIncludesZero(false);
            mYAxis.setLabelFont(oYAxis.getLabelFont());
            mYAxis.setTickLabelFont(oYAxis.getTickLabelFont());
            if (!oYAxis.isAutoRange()) mYAxis.setRange(oYAxis.getRange());
            mPlot.setRangeAxis(mYAxis);
        }
        return this;
    }
    @Override public IPlotter xScaleLinear() {
        if (!(mXAxis instanceof LargerTickUnitNumberAxis)) {
            NumberAxis oXAxis = mXAxis;
            mXAxis = new LargerTickUnitNumberAxis(oXAxis.getLabel());
            mXAxis.setAutoRangeIncludesZero(false);
            mXAxis.setLabelFont(oXAxis.getLabelFont());
            mXAxis.setTickLabelFont(oXAxis.getTickLabelFont());
            if (!oXAxis.isAutoTickUnitSelection()) mXAxis.setTickUnit(oXAxis.getTickUnit()); // 一般轴可以有 tickUnit
            if (!oXAxis.isAutoRange()) mXAxis.setRange(oXAxis.getRange());
            mPlot.setDomainAxis(mXAxis);
        }
        return this;
    }
    @Override public IPlotter yScaleLinear() {
        if (!(mYAxis instanceof LargerTickUnitNumberAxis)) {
            NumberAxis oYAxis = mYAxis;
            mYAxis = new LargerTickUnitNumberAxis(oYAxis.getLabel());
            mYAxis.setAutoRangeIncludesZero(false);
            mYAxis.setLabelFont(oYAxis.getLabelFont());
            mYAxis.setTickLabelFont(oYAxis.getTickLabelFont());
            if (!oYAxis.isAutoTickUnitSelection()) mYAxis.setTickUnit(oYAxis.getTickUnit()); // 一般轴可以有 tickUnit
            if (!oYAxis.isAutoRange()) mYAxis.setRange(oYAxis.getRange());
            mPlot.setRangeAxis(mYAxis);
        }
        return this;
    }
    
    private @Nullable IFigure mCurrentFigure = null;
    
    @Override public IFigure show(String aName) {
        // 如果已经有窗口则不再显示
        if (mCurrentFigure != null) {
            // 无论怎样都需要设置名称
            mCurrentFigure.name(aName);
            // 如果没有被关闭则直接结束
            if (mCurrentFigure.isShowing()) return mCurrentFigure;
        }
        
        // 显示图像
        final JFrame tFrame = new JFrame(aName);
        tFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final Insets tInset = new Insets(20, 20, 20, 20);
        final JPanel tPanel = new ChartPanel(mChart) {
            @Override public Insets getInsets() {return tInset;}
        };
        tPanel.setBackground(WHITE);
        tFrame.setContentPane(tPanel);
        tFrame.pack();
        tFrame.setVisible(true);
        
        // 返回 IFigure
        mCurrentFigure = new IFigure() {
            @Override public boolean isShowing() {return tFrame.isShowing();}
            @Override public void dispose() {tFrame.dispose();}
            
            @Override public IFigure name(String aName) {tFrame.setTitle(aName); return this;}
            @Override public IFigure size(int aWidth, int aHeight) {synchronized (tFrame.getTreeLock()) {tPanel.setSize(aWidth-tInset.left-tInset.right, aHeight-tInset.top-tInset.bottom); tFrame.setSize(aWidth, aHeight);} return this;}
            @Override public IFigure location(int aX, int aY) {tFrame.setLocation(aX, aY); return this;}
            @Override public IFigure insets(double aTop, double aLeft, double aBottom, double aRight) {
                synchronized (tFrame.getTreeLock()) {
                    tInset.top = (int) Math.round(aTop);
                    tInset.left = (int) Math.round(aLeft);
                    tInset.bottom = (int) Math.round(aBottom);
                    tInset.right = (int) Math.round(aRight);
                }
                return this;
            }
            @Override public IFigure insetsTop(double aTop) {synchronized (tFrame.getTreeLock()) {tInset.top = (int)Math.round(aTop);} return this;}
            @Override public IFigure insetsLeft(double aLeft) {synchronized (tFrame.getTreeLock()) {tInset.left = (int)Math.round(aLeft);} return this;}
            @Override public IFigure insetsBottom(double aBottom) {synchronized (tFrame.getTreeLock()) {tInset.bottom = (int)Math.round(aBottom);} return this;}
            @Override public IFigure insetsRight(double aRight) {synchronized (tFrame.getTreeLock()) {tInset.right = (int)Math.round(aRight);} return this;}
            
            @Override public void save(@Nullable String aFilePath) throws IOException {
                synchronized (tFrame.getTreeLock()) {
                    if (aFilePath==null || aFilePath.isEmpty()) aFilePath = tFrame.getTitle();
                    if (!aFilePath.endsWith(".png")) aFilePath = aFilePath+".png";
                    UT.IO.validPath(aFilePath); // 注意这里是调用外部接口保存，需要手动合法化路径
                    ChartUtils.saveChartAsPNG(UT.IO.toFile(aFilePath), mChart, tPanel.getWidth()-tInset.left-tInset.right, tPanel.getHeight()-tInset.top-tInset.bottom);
                }
            }
        };
        
        return mCurrentFigure;
    }
}
