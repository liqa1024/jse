package com.jtool.plot;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class AbstractResizableShape implements IResizableShape {
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
