package com.jtool.plot;

import java.awt.*;

public abstract class AbstractResizableStroke implements IResizableStroke {
    protected Stroke mStroke;
    protected double mSize;
    
    protected AbstractResizableStroke(double aSize) {mStroke = getStroke(aSize); mSize = aSize;}
    @Override public double getSize() {return mSize;}
    @Override public void setSize(double aSize) {mStroke = getStroke(aSize); mSize = aSize;}
    
    @Override public Shape createStrokedShape(Shape p) {return mStroke.createStrokedShape(p);}
    
    /** stuff to override */
    protected abstract Stroke getStroke(double aSize);
}
