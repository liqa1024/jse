package com.jtool.plot;

import java.awt.*;

import static com.jtool.plot.Shapes.*;
import static com.jtool.plot.Strokes.*;


/**
 * @author liqa
 * <p> 一般的 ILine 的实现，将 LineType 和 MarkerType 转为 java 的 Stroke 和 Shape 的修改 </p>
 */
public abstract class AbstractLine implements ILine {
    /** 内部的 Stroke 和 Shape 发生改变时的 call-back，用于子类重写后续行为 */
    protected abstract void onLineTypeChange(LineType aOldLineType, LineType aNewLineType);
    protected abstract void onMarkerTypeChange(MarkerType aOldMarkerType, MarkerType aNewMarkerType);
    protected abstract void onLineWidthChange(double aOldLineWidth, double aNewLineWidth);
    protected abstract void onMarkerSizeChange(double aOldMarkerSize, double aNewMarkerSize);
    
    protected IResizableStroke mLineStroke = DEFAULT_LINE_STROKE;
    protected IResizableShape mMarkerShape = DEFAULT_MARKER_SHAPE;
    
    protected LineType mLineType = DEFAULT_LINE_TYPE;
    protected MarkerType mMarkerType = DEFAULT_MARKER_TYPE;
    
    
    @Override public ILine lineWidth(double aLineWidth) {
        double oLineWidth = mLineStroke.getSize();
        mLineStroke.setSize(aLineWidth);
        onLineWidthChange(oLineWidth, aLineWidth);
        return this;
    }
    @Override public ILine lineType(LineType aLineType) {
        if (aLineType == mLineType) return this;
        LineType oLineType = mLineType;
        mLineType = aLineType;
        mLineStroke = toStroke(aLineType, mLineStroke.getSize());
        onLineTypeChange(oLineType, aLineType);
        return this;
    }
    @Override public ILine lineStroke(final Stroke aLineStroke) {
        double oLineWidth = mLineStroke.getSize();
        LineType oLineType = mLineType;
        mLineType = LineType.ELSE;
        IResizableStroke tLineStroke;
        if (aLineStroke instanceof IResizableStroke) {
            tLineStroke = (IResizableStroke)aLineStroke;
        } else {
            tLineStroke = new AbstractResizableStroke(oLineWidth) {
                @Override protected Stroke getStroke(double aSize) {return aLineStroke;}
            };
        }
        mLineStroke = tLineStroke;
        onLineWidthChange(oLineWidth, tLineStroke.getSize());
        onLineTypeChange(oLineType, mLineType);
        return this;
    }
    
    @Override public ILine markerSize(double aMarkerSize) {
        double oMarkerSize = mMarkerShape.getSize();
        mMarkerShape.setSize(aMarkerSize);
        onMarkerSizeChange(oMarkerSize, aMarkerSize);
        return this;
    }
    @Override public ILine markerType(MarkerType aMarkerType) {
        if (aMarkerType == mMarkerType) return this;
        MarkerType oMarkerType = mMarkerType;
        mMarkerType = aMarkerType;
        mMarkerShape = toShape(aMarkerType, mMarkerShape.getSize());
        onMarkerTypeChange(oMarkerType, aMarkerType);
        return this;
    }
    @Override public ILine markerShape(final Shape aMarkerShape) {
        double oMarkerSize = mMarkerShape.getSize();
        MarkerType oMarkerType = mMarkerType;
        mMarkerType = MarkerType.ELSE;
        IResizableShape tMarkerShape;
        if (aMarkerShape instanceof IResizableShape) {
            tMarkerShape = (IResizableShape)aMarkerShape;
        } else {
            tMarkerShape = new AbstractResizableShape(oMarkerSize) {
                @Override protected Shape getShape(double aSize) {
                    return aMarkerShape;
                }
            };
        }
        mMarkerShape = tMarkerShape;
        onMarkerSizeChange(oMarkerSize, tMarkerShape.getSize());
        onMarkerTypeChange(oMarkerType, mMarkerType);
        return this;
    }
}
