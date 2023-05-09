package com.guan.plot;

import java.awt.*;

import static com.guan.code.CS.COLOR_NULL;
import static com.guan.code.CS.color_;
import static java.awt.Color.*;

/**
 * @author liqa
 * <p> {@link IPlotter}.plot 得到的线的对象 </p>
 * <p> 主要用于方便的设置具体某个线的参数 </p>
 */
public interface ILine {
    /** 通用方法，用来减少重复代码 */
    class Util {
        /** 全局常量记录默认值 */
        public final static Color DEFAULT_COLOR = color_(0);
        public final static LineType DEFAULT_LINE_TYPE = LineType.SOLID;
        public final static MarkerType DEFAULT_MARKER_TYPE = MarkerType.NULL;
        public final static double DEFAULT_LINE_WIDTH = 2.0;
        public final static double DEFAULT_MARKER_SIZE = 12.0;
        
        
        protected static Color toColor(String aColor) {
            switch (aColor) {
            case "w": case "white": case "WHITE":
                return WHITE;
            case "lightGray": case "LIGHT_GRAY":
                return LIGHT_GRAY;
            case "gray": case "GRAY":
                return GRAY;
            case "darkGray": case "DARK_GRAY":
                return DARK_GRAY;
            case "k": case "black": case "BLACK":
                return BLACK;
            case "r": case "red": case "RED":
                return RED;
            case "pink": case "PINK":
                return PINK;
            case "orange": case "ORANGE":
                return ORANGE;
            case "y": case "yellow": case "YELLOW":
                return YELLOW;
            case "g": case "green": case "GREEN":
                return GREEN;
            case "m": case "magenta": case "MAGENTA":
                return MAGENTA;
            case "c": case "cyan": case "CYAN":
                return CYAN;
            case "b": case "blue": case "BLUE":
                return BLUE;
            case "none": case "null": case "NULL":
                return COLOR_NULL;
            default:
                return DEFAULT_COLOR;
            }
        }
        
        protected static LineType toLineType(String aLineType) {
            switch (aLineType) {
            case "-": case "solid": case "SOLID":
                return LineType.SOLID;
            case "--": case "dashed": case "DASHED":
                return LineType.DASHED;
            case ":": case "..": case "dotted": case "DOTTED":
                return LineType.DOTTED;
            case "-.": case "dash-dotted": case "DASH_DOTTED":
                return LineType.DASH_DOTTED;
            case "-..": case "dash-dot-dotted": case "DASH_DOT_DOTTED":
                return LineType.DASH_DOT_DOTTED;
            case "--.": case "dash-dash-dotted": case "DASH_DASH_DOTTED":
                return LineType.DASH_DASH_DOTTED;
            case "none": case "null": case "NULL":
                return LineType.NULL;
            default:
                return DEFAULT_LINE_TYPE;
            }
        }
        
        protected static MarkerType toMarkerType(String aMarkerType) {
            switch (aMarkerType) {
            case "o": case "circle": case "CIRCLE":
                return MarkerType.CIRCLE;
            case "+": case "plus": case "PLUS":
                return MarkerType.PLUS;
            case "*": case "asterisk": case "ASTERISK":
                return MarkerType.ASTERISK;
            case "x": case "cross": case "CROSS":
                return MarkerType.CROSS;
            case "s": case "square": case "SQUARE":
                return MarkerType.SQUARE;
            case "d": case "diamond": case "DIAMOND":
                return MarkerType.DIAMOND;
            case "^": case "triangle": case "TRIANGLE":
                return MarkerType.TRIANGLE;
            case "none": case "null": case "NULL":
                return MarkerType.NULL;
            default:
                return DEFAULT_MARKER_TYPE;
            }
        }
    }
    
    
    /** 设置线的各种属性，返回自身方便链式调用 */
    default ILine color(double[] aColor) {return color(aColor[0], aColor[1], aColor[2]);}
    default ILine color(double aR, double aG, double aB) {return color(new Color(Math.round(aR*255), Math.round(aG*255), Math.round(aB*255)));}
    default ILine color(String aColor) {return color(Util.toColor(aColor));}
    ILine color(Paint aPaint);
    
    @Deprecated default ILine width(double aLineWidth) {return lineWidth(aLineWidth);}
    @Deprecated default ILine lineSize(double aLineWidth) {return lineWidth(aLineWidth);}
    ILine lineWidth(double aLineWidth);
    
    @Deprecated default ILine type(LineType aLineType) {return lineType(aLineType);}
    @Deprecated default ILine type(String aLineType) {return lineType(aLineType);}
    default ILine lineType(String aLineType) {return lineType(Util.toLineType(aLineType));}
    ILine lineType(LineType aLineType);
    
    enum LineType {
          NULL
        , SOLID
        , DASHED
        , DOTTED
        , DASH_DOTTED
        , DASH_DOT_DOTTED
        , DASH_DASH_DOTTED
    }
    
    @Deprecated default ILine marker(MarkerType aMarkerType) {return markerType(aMarkerType);}
    @Deprecated default ILine marker(String aMarkerType) {return markerType(aMarkerType);}
    default ILine markerType(String aMarkerType) {return markerType(Util.toMarkerType(aMarkerType));}
    ILine markerType(MarkerType aMarkerType);
    
    enum MarkerType {
          NULL
        , CIRCLE
        , PLUS
        , ASTERISK
        , CROSS
        , SQUARE
        , DIAMOND
        , TRIANGLE
    }
    
    
    @Deprecated default ILine size(double aSize) {return markerSize(aSize);}
    ILine markerSize(double aSize);
    
    default ILine markerColor(double[] aColor) {return markerColor(aColor[0], aColor[1], aColor[2]);}
    default ILine markerColor(double aR, double aG, double aB) {return markerColor(new Color(Math.round(aR*255), Math.round(aG*255), Math.round(aB*255)));}
    default ILine markerColor(String aColor) {return markerColor(Util.toColor(aColor));}
    ILine markerColor(Paint aPaint);
}
