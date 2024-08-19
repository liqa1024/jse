package jse.code.collection;

public final class DoubleTriplet implements IDoubleTriplet {
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
    
    @Override public boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof IDoubleTriplet)) return false;
        
        IDoubleTriplet tTriplet = (IDoubleTriplet)aRHS;
        return Double.compare(mA, tTriplet.a())==0 && Double.compare(mB, tTriplet.b())==0 && Double.compare(mC, tTriplet.c())==0;
    }
    @Override public int hashCode() {
        int rHashCode = Double.hashCode(mA);
        rHashCode = 31*rHashCode + Double.hashCode(mB);
        rHashCode = 31*rHashCode + Double.hashCode(mC);
        return rHashCode;
    }
    
    /** print */
    @Override public String toString() {return String.format("(%f, %f, %f)", mA, mB, mC);}
}
