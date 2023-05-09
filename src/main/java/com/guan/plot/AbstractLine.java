package com.guan.plot;

import com.guan.math.MathEX;

import java.awt.*;
import java.awt.geom.*;


/**
 * @author liqa
 * <p> 一般的 ILine 的实现，将 LineType 和 MarkerType 转为 java 的 Stroke 和 Shape 的修改 </p>
 */
public abstract class AbstractLine implements ILine {
    public interface IResizable {
        double getSize();
        void setSize(double aSize);
    }
    public interface IResizableShape extends IResizable, Shape {}
    public interface IResizableStroke extends IResizable, Stroke {}
    public static abstract class AbstractResizableShape implements IResizableShape {
        protected Shape mShape;
        protected double mSize;
        
        protected AbstractResizableShape(double aSize) {mShape = getShape(aSize); mSize = aSize;}
        @Override public double getSize() {return mSize;}
        @Override public void setSize(double aSize) {mShape = getShape(aSize); mSize = aSize;}
        
        @Override public Rectangle getBounds() {return mShape.getBounds();}
        @Override public Rectangle2D getBounds2D() {return mShape.getBounds2D();}
        @Override public boolean contains(double x, double y) {return mShape.contains(x, y);}
        @Override public boolean contains(Point2D p) {return mShape.contains(p);}
        @Override public boolean intersects(double x, double y, double w, double h) {return mShape.intersects(x, y, w, h);}
        @Override public boolean intersects(Rectangle2D r) {return mShape.intersects(r);}
        @Override public boolean contains(double x, double y, double w, double h) {return mShape.contains(x, y, w, h);}
        @Override public boolean contains(Rectangle2D r) {return mShape.contains(r);}
        @Override public PathIterator getPathIterator(AffineTransform at) {return mShape.getPathIterator(at);}
        @Override public PathIterator getPathIterator(AffineTransform at, double flatness) {return mShape.getPathIterator(at, flatness);}
        
        /** stuff to override */
        protected abstract Shape getShape(double aSize);
    }
    public static abstract class AbstractResizableStroke implements IResizableStroke {
        protected Stroke mStroke;
        protected double mSize;
        
        protected AbstractResizableStroke(double aSize) {mStroke = getStroke(aSize); mSize = aSize;}
        @Override public double getSize() {return mSize;}
        @Override public void setSize(double aSize) {mStroke = getStroke(aSize); mSize = aSize;}
        
        @Override public Shape createStrokedShape(Shape p) {return mStroke.createStrokedShape(p);}
        
        /** stuff to override */
        protected abstract Stroke getStroke(double aSize);
    }
    
    /** 各种 marker 形状的实现 */
    public static class NullShape extends AbstractResizableShape {
        public NullShape(double aSize) {super(aSize);}
        @Override protected Shape getShape(double aSize) {return null;}
    }
    public static class Square extends Rectangle2D.Double implements IResizableShape {
        public Square(double aSize) {super(-aSize*0.5, -aSize*0.5, aSize, aSize);}
        @Override public double getSize() {return super.width;}
        @Override public void setSize(double aSize) {
            super.width = aSize;
            super.height = aSize;
            super.x = -aSize*0.5;
            super.y = -aSize*0.5;
        }
    }
    public static class Circle extends Ellipse2D.Double implements IResizableShape {
        public Circle(double aSize) {super(-aSize*0.5, -aSize*0.5, aSize, aSize);}
        @Override public double getSize() {return super.width;}
        @Override public void setSize(double aSize) {
            super.width = aSize;
            super.height = aSize;
            super.x = -aSize*0.5;
            super.y = -aSize*0.5;
        }
    }
    public static class Plus extends AbstractResizableShape {
        public Plus(double aSize) {super(aSize);}
        @Override protected Shape getShape(double aSize) {
            double tLen = aSize*0.5;
            Line2D hLine = new Line2D.Double(0-tLen, 0     , 0+tLen, 0     );
            Line2D vLine = new Line2D.Double(0     , 0-tLen, 0     , 0+tLen);
            GeneralPath tPlus = new GeneralPath();
            tPlus.append(hLine, false);
            tPlus.append(vLine, false);
            return tPlus;
        }
    }
    private final static double SQRT2 = MathEX.Fast.sqrt(2.0);
    private final static double SQRT2_INV = 1.0/SQRT2;
    public static class Asterisk extends AbstractResizableShape {
        public Asterisk(double aSize) {super(aSize);}
        @Override protected Shape getShape(double aSize) {
            double tLen = aSize*0.5;
            double dLen = tLen*SQRT2_INV;
            Line2D hLine  = new Line2D.Double(-tLen, 0.0  , +tLen, 0.0  );
            Line2D vLine  = new Line2D.Double(0.0  , -tLen, 0.0  , +tLen);
            Line2D dLine1 = new Line2D.Double(-dLen, -dLen, +dLen, +dLen);
            Line2D dLine2 = new Line2D.Double(-dLen, +dLen, +dLen, -dLen);
            GeneralPath tAsterisk = new GeneralPath();
            tAsterisk.append(hLine , false);
            tAsterisk.append(vLine , false);
            tAsterisk.append(dLine1, false);
            tAsterisk.append(dLine2, false);
            return tAsterisk;
        }
    }
    public static class Cross extends AbstractResizableShape {
        public Cross(double aSize) {super(aSize);}
        @Override protected Shape getShape(double aSize) {
            double tLen = aSize*0.5;
            double dLen = tLen*SQRT2_INV;
            Line2D dLine1 = new Line2D.Double(-dLen, -dLen, +dLen, +dLen);
            Line2D dLine2 = new Line2D.Double(-dLen, +dLen, +dLen, -dLen);
            GeneralPath tCross = new GeneralPath();
            tCross.append(dLine1, false);
            tCross.append(dLine2, false);
            return tCross;
        }
    }
    public static class Diamond extends AbstractResizableShape {
        public Diamond(double aSize) {super(aSize);}
        @Override protected Shape getShape(double aSize) {
            double tLen = aSize*0.5*SQRT2;
            GeneralPath tDiamond = new GeneralPath();
            tDiamond.moveTo(0.0  , +tLen);
            tDiamond.lineTo(+tLen, 0.0  );
            tDiamond.lineTo(0.0  , -tLen);
            tDiamond.lineTo(-tLen, 0.0  );
            tDiamond.closePath();
            return tDiamond;
        }
    }
    private final static double TRIANGLE_MUL_X = 0.66;
    private final static double TRIANGLE_MUL_Y = TRIANGLE_MUL_X/MathEX.Fast.sqrt(3.0);
    public static class Triangle extends AbstractResizableShape {
        public Triangle(double aSize) {super(aSize);}
        @Override protected Shape getShape(double aSize) {
            double tLenX = aSize*TRIANGLE_MUL_X;
            double tLenY = aSize*TRIANGLE_MUL_Y;
            GeneralPath tTriangle = new GeneralPath();
            tTriangle.moveTo(0.0   , -tLenY*2.0);
            tTriangle.lineTo(+tLenX, +tLenY    );
            tTriangle.lineTo(-tLenX, +tLenY    );
            tTriangle.closePath();
            return tTriangle;
        }
    }
    /** 各种 line 的形状的实现 */
    public static class NullStroke extends AbstractResizableStroke {
        public NullStroke(double aSize) {super(aSize);}
        @Override protected Stroke getStroke(double aSize) {return null;}
    }
    public static class Solid extends AbstractResizableStroke {
        public Solid(double aSize) {super(aSize);}
        @Override protected Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            // 圆角端点，圆角连接，斜接限制调整为 2.0f（波动会更加明显）
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f);
        }
    }
    public static class Dashed extends AbstractResizableStroke {
        public Dashed(double aSize) {super(aSize);}
        @Override protected Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*5.0f, tWidth*2.0f}, 0.0f);
        }
    }
    public static class Dotted extends AbstractResizableStroke {
        public Dotted(double aSize) {super(aSize);}
        @Override protected Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth, tWidth*2.0f}, 0.0f);
        }
    }
    public static class DashDotted extends AbstractResizableStroke {
        public DashDotted(double aSize) {super(aSize);}
        @Override protected Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*5.0f, tWidth*2.0f, tWidth, tWidth*2.0f}, 0.0f);
        }
    }
    public static class DashDotDotted extends AbstractResizableStroke {
        public DashDotDotted(double aSize) {super(aSize);}
        @Override protected Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*4.5f, tWidth*1.5f, tWidth, tWidth*1.5f, tWidth, tWidth*1.5f}, 0.0f);
        }
    }
    public static class DashDashDotted extends AbstractResizableStroke {
        public DashDashDotted(double aSize) {super(aSize);}
        @Override protected Stroke getStroke(double aSize) {
            float tWidth = (float)aSize;
            return new BasicStroke(tWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 2.0f, new float[] {tWidth*3.75f, tWidth*1.5f, tWidth*3.75f, tWidth*1.5f, tWidth, tWidth*1.5f}, 0.0f);
        }
    }
    
    
    /** 全局常量记录默认值 */
    public final static IResizableStroke NULL_STROKE = new NullStroke(Util.DEFAULT_LINE_WIDTH);
    public final static IResizableShape NULL_SHAPE = new NullShape(Util.DEFAULT_MARKER_SIZE);
    public final static IResizableStroke DEFAULT_STROKE = toStroke(Util.DEFAULT_LINE_TYPE, Util.DEFAULT_LINE_WIDTH);
    public final static IResizableShape DEFAULT_SHAPE = toShape(Util.DEFAULT_MARKER_TYPE, Util.DEFAULT_MARKER_SIZE);
    
    
    /** 内部的 Stroke 和 Shape 发生改变时的 call-back，用于子类重写后续行为 */
    protected abstract void onLineTypeChange(LineType aOldLineType, LineType aNewLineType);
    protected abstract void onMarkerTypeChange(MarkerType aOldMarkerType, MarkerType aNewMarkerType);
    protected abstract void onLineWidthChange(double aOldLineWidth, double aNewLineWidth);
    protected abstract void onMarkerSizeChange(double aOldMarkerSize, double aNewMarkerSize);
    
    protected IResizableStroke mStroke = DEFAULT_STROKE;
    protected IResizableShape mShape = DEFAULT_SHAPE;
    
    protected LineType mLineType = Util.DEFAULT_LINE_TYPE;
    protected MarkerType mMarkerType = Util.DEFAULT_MARKER_TYPE;
    
    
    protected static IResizableStroke toStroke(LineType aLineType, double aLineWidth) {
        switch (aLineType) {
        case NULL:              return NULL_STROKE;
        case SOLID:             return new Solid(aLineWidth);
        case DASHED:            return new Dashed(aLineWidth);
        case DOTTED:            return new Dotted(aLineWidth);
        case DASH_DOTTED:       return new DashDotted(aLineWidth);
        case DASH_DOT_DOTTED:   return new DashDotDotted(aLineWidth);
        case DASH_DASH_DOTTED:  return new DashDashDotted(aLineWidth);
        default:                return toStroke(Util.DEFAULT_LINE_TYPE, aLineWidth);
        }
    }
    protected static IResizableShape toShape(MarkerType aMarkerType, double aMarkerSize) {
        switch (aMarkerType) {
        case NULL:              return NULL_SHAPE;
        case CIRCLE:            return new Circle(aMarkerSize);
        case PLUS:              return new Plus(aMarkerSize);
        case ASTERISK:          return new Asterisk(aMarkerSize);
        case CROSS:             return new Cross(aMarkerSize);
        case SQUARE:            return new Square(aMarkerSize);
        case DIAMOND:           return new Diamond(aMarkerSize);
        case TRIANGLE:          return new Triangle(aMarkerSize);
        default:                return toShape(Util.DEFAULT_MARKER_TYPE, aMarkerSize);
        }
    }
    
    
    @Override public ILine lineWidth(double aLineWidth) {
        double oLineWidth = mStroke.getSize();
        mStroke.setSize(aLineWidth);
        onLineWidthChange(oLineWidth, aLineWidth);
        return this;
    }
    @Override public ILine lineType(LineType aLineType) {
        if (aLineType == mLineType) return this;
        LineType oLineType = mLineType;
        mLineType = aLineType;
        mStroke = toStroke(aLineType, mStroke.getSize());
        onLineTypeChange(oLineType, aLineType);
        return this;
    }
    @Override public ILine markerSize(double aMarkerSize) {
        double oMarkerSize = mShape.getSize();
        mShape.setSize(aMarkerSize);
        onMarkerSizeChange(oMarkerSize, aMarkerSize);
        return this;
    }
    @Override public ILine markerType(MarkerType aMarkerType) {
        if (aMarkerType == mMarkerType) return this;
        MarkerType oMarkerType = mMarkerType;
        mMarkerType = aMarkerType;
        mShape = toShape(aMarkerType, mShape.getSize());
        onMarkerTypeChange(oMarkerType, aMarkerType);
        return this;
    }
}
