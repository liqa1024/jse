package jse.code.collection;

public class DoubleWrapper implements IDoubleWrapper {
    public double mValue;
    public DoubleWrapper(double aValue) {
        mValue = aValue;
    }
    @Override public double value() {return mValue;}
    
    /** print */
    @Override public String toString() {return String.format("(%f)", mValue);}
}
