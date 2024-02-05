package jse.code.collection;

public class DoubleTriplet implements IDoubleTriplet {
    public double mA;
    public double mB;
    public double mC;
    public DoubleTriplet(double aA, double aB, double aC) {
        mA = aA;
        mB = aB;
        mC = aC;
    }
    
    @Override public double a() {return mA;}
    @Override public double b() {return mB;}
    @Override public double c() {return mC;}
    
    /** print */
    @Override public String toString() {return String.format("(%f, %f, %f)", mA, mB, mC);}
}
