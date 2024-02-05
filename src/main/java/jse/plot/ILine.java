package jse.plot;

import org.jetbrains.annotations.VisibleForTesting;

import java.awt.*;

import static jse.plot.Shapes.*;
import static jse.plot.Strokes.*;
import static jse.plot.Colors.*;


/**
 * @author liqa
 * <p> {@link IPlotter}.plot 得到的线的对象 </p>
 * <p> 主要用于方便的设置具体某个线的参数 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public interface ILine {
    /** 设置线的各种属性，返回自身方便链式调用 */
    default ILine color(int aIdx) {return color(COLOR(aIdx));}
    default ILine color(double aR, double aG, double aB) {return color(new Color((int)Math.round(aR*255), (int)Math.round(aG*255), (int)Math.round(aB*255)));}
    default ILine color(String aColor) {return color(toColor(aColor));}
    ILine color(Paint aPaint);
    
    default ILine lineColor(int aIdx) {return lineColor(COLOR(aIdx));}
    default ILine lineColor(double aR, double aG, double aB) {return lineColor(new Color((int)Math.round(aR*255), (int)Math.round(aG*255), (int)Math.round(aB*255)));}
    default ILine lineColor(String aColor) {return lineColor(toColor(aColor));}
    ILine lineColor(Paint aPaint);
    
    @VisibleForTesting default ILine width(double aLineWidth) {return lineWidth(aLineWidth);}
    @VisibleForTesting default ILine lineSize(double aLineWidth) {return lineWidth(aLineWidth);}
    ILine lineWidth(double aLineWidth);
    
    @VisibleForTesting default ILine type(LineType aLineType) {return lineType(aLineType);}
    @VisibleForTesting default ILine type(String aLineType) {return lineType(aLineType);}
    default ILine lineType(String aLineType) {return lineType(toLineType(aLineType));}
    ILine lineType(LineType aLineType);
    ILine lineStroke(Stroke aLineStroke);
    
    default ILine filled() {return filled(true);}
    ILine filled(boolean aFilled);
    
    default ILine markerColor(int aIdx) {return markerColor(COLOR(aIdx));}
    default ILine markerColor(double aR, double aG, double aB) {return markerColor(new Color(Math.round(aR*255), Math.round(aG*255), Math.round(aB*255)));}
    default ILine markerColor(String aColor) {return markerColor(toColor(aColor));}
    ILine markerColor(Paint aPaint);
    
    default ILine markerFaceColor(int aIdx) {return markerFaceColor(COLOR(aIdx));}
    default ILine markerFaceColor(double aR, double aG, double aB) {return markerFaceColor(new Color(Math.round(aR*255), Math.round(aG*255), Math.round(aB*255)));}
    default ILine markerFaceColor(String aColor) {return markerFaceColor(toColor(aColor));}
    ILine markerFaceColor(Paint aPaint);
    
    default ILine markerEdgeColor(int aIdx) {return markerEdgeColor(COLOR(aIdx));}
    default ILine markerEdgeColor(double aR, double aG, double aB) {return markerEdgeColor(new Color(Math.round(aR*255), Math.round(aG*255), Math.round(aB*255)));}
    default ILine markerEdgeColor(String aColor) {return markerEdgeColor(toColor(aColor));}
    ILine markerEdgeColor(Paint aPaint);
    
    @VisibleForTesting default ILine edgeSize(double aEdgeWidth) {return markerEdgeWidth(aEdgeWidth);}
    @VisibleForTesting default ILine markerEdgeSize(double aEdgeWidth) {return markerEdgeWidth(aEdgeWidth);}
    @VisibleForTesting default ILine edgeWidth(double aEdgeWidth) {return markerEdgeWidth(aEdgeWidth);}
    ILine markerEdgeWidth(double aEdgeWidth);
    
    @VisibleForTesting default ILine size(double aSize) {return markerSize(aSize);}
    ILine markerSize(double aSize);
    
    @VisibleForTesting default ILine marker(MarkerType aMarkerType) {return markerType(aMarkerType);}
    @VisibleForTesting default ILine marker(String aMarkerType) {return markerType(aMarkerType);}
    default ILine markerType(String aMarkerType) {return markerType(toMarkerType(aMarkerType));}
    ILine markerType(MarkerType aMarkerType);
    ILine markerShape(Shape aMarkerShape);
    
    /** 是否显示 legend */
    ILine noLegend();
    ILine showLegend();
}
