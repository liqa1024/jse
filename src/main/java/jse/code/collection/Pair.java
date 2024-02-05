package jse.code.collection;

/**
 * @author liqa
 * <p> STL style pair, 
 * simple and more powerful </p>
 */
public class Pair<A, B> implements IPair<A, B> {
    public A mFirst;
    public B mSecond;
    public Pair(A aFirst, B aSecond) {
        mFirst = aFirst;
        mSecond = aSecond;
    }
    
    @Override public A first() {return mFirst;}
    @Override public B second() {return mSecond;}
    
    /** print */
    @Override public String toString() {return String.format("(%s, %s)", mFirst, mSecond);}
}
