package jse.code.collection;

import java.util.Objects;

/**
 * @author liqa
 * <p> STL style Triplet,
 * simple and more powerful </p>
 */
public final class Triplet<A, B, C> implements ITriplet<A, B, C> {
    public A mA;
    public B mB;
    public C mC;
    public Triplet(A aA, B aB, C aC) {
        mA = aA;
        mB = aB;
        mC = aC;
    }
    
    @Override public A a() {return mA;}
    @Override public B b() {return mB;}
    @Override public C c() {return mC;}
    
    @Override public boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof ITriplet)) return false;
        
        ITriplet<?, ?, ?> tTriplet = (ITriplet<?, ?, ?>)aRHS;
        return Objects.equals(mA, tTriplet.a()) && Objects.equals(mB, tTriplet.b()) && Objects.equals(mC, tTriplet.c());
    }
    @Override public int hashCode() {
        int rHashCode = Objects.hashCode(mA);
        rHashCode = 31*rHashCode + Objects.hashCode(mB);
        rHashCode = 31*rHashCode + Objects.hashCode(mC);
        return rHashCode;
    }
    
    /** print */
    @Override public String toString() {return String.format("(%s, %s, %s)", mA, mB, mC);}
}
