package com.jtool.plot;

import org.jetbrains.annotations.VisibleForTesting;

import java.awt.*;

import static com.jtool.plot.Shapes.*;
import static com.jtool.plot.Strokes.*;
import static com.jtool.plot.Colors.*;


/**
 * @author liqa
 * <p> {@link IPlotter}.plot 得到的线的对象 </p>
 * <p> 主要用于方便的设置具体某个线的参数 </p>
 */
public interface ILine {
    /** 设置线的各种属性，返回自身方便链式调用 */
    default ILine color(double[] aColor) {return color(aColor[0], aColor[1], aColor[2]);}
    default ILine color(double aR, double aG, double aB) {return color(new Color(Math.round(aR*255), Math.round(aG*255), Math.round(aB*255)));}
    default ILine color(String aColor) {return color(toColor(aColor));}
    ILine color(Paint aPaint);
    
    @VisibleForTesting default ILine width(double aLineWidth) {return lineWidth(aLineWidth);}
    @VisibleForTesting default ILine lineSize(double aLineWidth) {return lineWidth(aLineWidth);}
    ILine lineWidth(double aLineWidth);
    
    @VisibleForTesting default ILine type(LineType aLineType) {return lineType(aLineType);}
    @VisibleForTesting default ILine type(String aLineType) {return lineType(aLineType);}
    default ILine lineType(String aLineType) {return lineType(toLineType(aLineType));}
    ILine lineType(LineType aLineType);
    ILine lineStroke(Stroke aLineStroke);
    
    
    default ILine markerColor(double[] aColor) {return markerColor(aColor[0], aColor[1], aColor[2]);}
    default ILine markerColor(double aR, double aG, double aB) {return markerColor(new Color(Math.round(aR*255), Math.round(aG*255), Math.round(aB*255)));}
    default ILine markerColor(String aColor) {return markerColor(toColor(aColor));}
    ILine markerColor(Paint aPaint);
    
    @VisibleForTesting default ILine size(double aSize) {return markerSize(aSize);}
    ILine markerSize(double aSize);
    
    @VisibleForTesting default ILine marker(MarkerType aMarkerType) {return markerType(aMarkerType);}
    @VisibleForTesting default ILine marker(String aMarkerType) {return markerType(aMarkerType);}
    default ILine markerType(String aMarkerType) {return markerType(toMarkerType(aMarkerType));}
    ILine markerType(MarkerType aMarkerType);
    ILine markerShape(Shape aMarkerShape);
}
