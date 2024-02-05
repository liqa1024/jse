package jse.plot;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract class AbstractResizableStroke implements IResizableStroke {
    protected @NotNull Stroke mStroke;
    protected double mSize;
    
    protected AbstractResizableStroke(double aSize) {mStroke = getStroke(aSize); mSize = aSize;}
    @Override public double getSize() {return mSize;}
    @Override public void setSize(double aSize) {mStroke = getStroke(aSize); mSize = aSize;}
    
    @Override public Shape createStrokedShape(Shape p) {return mStroke.createStrokedShape(p);}
    
    /** stuff to override */
    protected abstract @NotNull Stroke getStroke(double aSize);
}
