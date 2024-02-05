package jse.plot;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author liqa
 * <p> {@link java.awt.Stroke} 中的一些用到的实例 </p>
 * <p> 目前用于 plot 的 Line 来使用 </p>
 */
public class Strokes {
    /** 各种 line 的形状的实现 */
    private final static Stroke EPT_STROKE = new BasicStroke(0.0f);
    public static class NullStroke extends AbstractResizableStroke {
        public NullStroke(double aSize) {super(aSize);}
        @Override protected @NotNull Stroke getStroke(double aSize) {return EPT_STROKE;}
    }
    public static class Solid extends AbstractResizableStroke {
        public Solid(double aSize) {super(aSize);}
        @Override protected @NotNull Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            // 圆角端点，圆角连接，斜接限制调整为 2.0f（波动会更加明显）
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f);
        }
    }
    public static class Dashed extends AbstractResizableStroke {
        public Dashed(double aSize) {super(aSize);}
        @Override protected @NotNull Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*5.0f, tWidth*3.0f}, 0.0f);
        }
    }
    public static class Dotted extends AbstractResizableStroke {
        public Dotted(double aSize) {super(aSize);}
        @Override protected @NotNull Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth, tWidth*3.0f}, 0.0f);
        }
    }
    public static class DashDotted extends AbstractResizableStroke {
        public DashDotted(double aSize) {super(aSize);}
        @Override protected @NotNull Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*5.0f, tWidth*3.0f, tWidth, tWidth*3.0f}, 0.0f);
        }
    }
    public static class DashDotDotted extends AbstractResizableStroke {
        public DashDotDotted(double aSize) {super(aSize);}
        @Override protected @NotNull Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*4.5f, tWidth*2.3f, tWidth, tWidth*2.3f, tWidth, tWidth*2.3f}, 0.0f);
        }
    }
    public static class DashDashDotted extends AbstractResizableStroke {
        public DashDashDotted(double aSize) {super(aSize);}
        @Override protected @NotNull Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*3.75f, tWidth*2.3f, tWidth*3.75f, tWidth*2.3f, tWidth, tWidth*2.3f}, 0.0f);
        }
    }
    
    
    /** Line stuffs */
    public enum LineType {
          NULL
        , SOLID
        , DASHED
        , DOTTED
        , DASH_DOTTED
        , DASH_DOT_DOTTED
        , DASH_DASH_DOTTED
        , ELSE
    }
    public static LineType toLineType(String aLineType) {
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
    public static IResizableStroke toStroke(LineType aLineType, double aLineWidth) {
        switch (aLineType) {
        case NULL:              return new NullStroke(aLineWidth);
        case SOLID:             return new Solid(aLineWidth);
        case DASHED:            return new Dashed(aLineWidth);
        case DOTTED:            return new Dotted(aLineWidth);
        case DASH_DOTTED:       return new DashDotted(aLineWidth);
        case DASH_DOT_DOTTED:   return new DashDotDotted(aLineWidth);
        case DASH_DASH_DOTTED:  return new DashDashDotted(aLineWidth);
        default:                return toStroke(DEFAULT_LINE_TYPE, aLineWidth);
        }
    }
    
    /** 全局常量记录默认值 */
    public final static LineType DEFAULT_LINE_TYPE = LineType.SOLID;
    public final static double DEFAULT_LINE_WIDTH = 2.0;
}
