package jse.code.collection;

/**
 * @author liqa
 * <p> STL style Triplet,
 * simple and more powerful </p>
 */
public class Triplet<A, B, C> implements ITriplet<A, B, C> {
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
    
    /** print */
    @Override public String toString() {return String.format("(%s, %s, %s)", mA, mB, mC);}
}
