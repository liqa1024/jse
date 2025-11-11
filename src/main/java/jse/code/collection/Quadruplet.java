package jse.code.collection;

import java.util.Objects;

/**
 * STL style Quadruplet,
 * simple and more powerful
 * @author liqa
 */
public final class Quadruplet<A, B, C, D> implements IQuadruplet<A, B, C, D> {
    public A mA;
    public B mB;
    public C mC;
    public D mD;
    public Quadruplet(A aA, B aB, C aC, D aD) {
        mA = aA;
        mB = aB;
        mC = aC;
        mD = aD;
    }
    
    @Override public A a() {return mA;}
    @Override public B b() {return mB;}
    @Override public C c() {return mC;}
    @Override public D d() {return mD;}
    
    @Override public boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof IQuadruplet)) return false;
        
        IQuadruplet<?, ?, ?, ?> tQuadruplet = (IQuadruplet<?, ?, ?, ?>)aRHS;
        return Objects.equals(mA, tQuadruplet.a()) && Objects.equals(mB, tQuadruplet.b()) &&
               Objects.equals(mC, tQuadruplet.c()) && Objects.equals(mD, tQuadruplet.d());
    }
    @Override public int hashCode() {
        int rHashCode = Objects.hashCode(mA);
        rHashCode = 31*rHashCode + Objects.hashCode(mB);
        rHashCode = 31*rHashCode + Objects.hashCode(mC);
        rHashCode = 31*rHashCode + Objects.hashCode(mD);
        return rHashCode;
    }
    
    /** print */
    @Override public String toString() {return String.format("(%s, %s, %s, %s)", mA, mB, mC, mD);}
}
