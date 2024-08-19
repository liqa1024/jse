package jse.code.collection;

public final class DoublePair implements IDoublePair {
    public double mFirst;
    public double mSecond;
    public DoublePair(double aFirst, double aSecond) {
        mFirst = aFirst;
        mSecond = aSecond;
    }
    
    @Override public double first() {return mFirst;}
    @Override public double second() {return mSecond;}
    
    @Override public boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof IDoublePair)) return false;
        
        IDoublePair tPair = (IDoublePair)aRHS;
        return Double.compare(mFirst, tPair.first())==0 && Double.compare(mSecond, tPair.second())==0;
    }
    @Override public int hashCode() {
        return 31*Double.hashCode(mFirst) + Double.hashCode(mSecond);
    }
    
    /** print */
    @Override public String toString() {return String.format("(%f, %f)", mFirst, mSecond);}
}
