package jse.code.collection;

public class DoublePair implements IDoublePair {
    public double mFirst;
    public double mSecond;
    public DoublePair(double aFirst, double aSecond) {
        mFirst = aFirst;
        mSecond = aSecond;
    }
    
    @Override public double first() {return mFirst;}
    @Override public double second() {return mSecond;}
    
    /** print */
    @Override public String toString() {return String.format("(%f, %f)", mFirst, mSecond);}
}
