package jse.plot;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static jse.plot.Shapes.*;
import static jse.plot.Strokes.*;


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
    protected abstract void onMarkerEdgeWidthChange(double aOldEdgeWidth, double aNewEdgeWidth);
    
    protected IResizableStroke mLineStroke = toStroke(DEFAULT_LINE_TYPE, DEFAULT_LINE_WIDTH);
    protected IResizableShape mMarkerShape = toShape(DEFAULT_MARKER_TYPE, DEFAULT_MARKER_SIZE);
    protected IResizableStroke mMarkerStroke = new Solid(DEFAULT_LINE_WIDTH);
    
    protected LineType mLineType = DEFAULT_LINE_TYPE;
    protected MarkerType mMarkerType = DEFAULT_MARKER_TYPE;
    
    
    @Override public ILine markerEdgeWidth(double aEdgeWidth) {
        double oEdgeWidth = mMarkerStroke.getSize();
        mMarkerStroke.setSize(aEdgeWidth);
        onMarkerEdgeWidthChange(oEdgeWidth, aEdgeWidth);
        return this;
    }
    @Override public ILine lineWidth(double aLineWidth) {
        double oLineWidth = mLineStroke.getSize();
        mLineStroke.setSize(aLineWidth);
        // 线宽变化时还需要同步调整 Marker Stroke 的宽度
        markerEdgeWidth(aLineWidth);
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
                @Override protected @NotNull Stroke getStroke(double aSize) {return aLineStroke;}
            };
        }
        mLineStroke = tLineStroke;
        // 线宽变化时还需要同步调整 Marker Stroke 的宽度
        markerEdgeWidth(mLineStroke.getSize());
        onLineWidthChange(oLineWidth, mLineStroke.getSize());
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
                @Override protected @NotNull Shape getShape(double aSize) {
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
