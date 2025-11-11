package jse.code.collection;

import java.util.Objects;

/**
 * STL style pair,
 * simple and more powerful
 * @author liqa
 */
public final class Pair<A, B> implements IPair<A, B> {
    public A mFirst;
    public B mSecond;
    public Pair(A aFirst, B aSecond) {
        mFirst = aFirst;
        mSecond = aSecond;
    }
    
    @Override public A first() {return mFirst;}
    @Override public B second() {return mSecond;}
    
    @Override public boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof IPair)) return false;
        
        IPair<?, ?> tPair = (IPair<?, ?>)aRHS;
        return Objects.equals(mFirst, tPair.first()) && Objects.equals(mSecond, tPair.second());
    }
    @Override public int hashCode() {
        return 31*Objects.hashCode(mFirst) + Objects.hashCode(mSecond);
    }
    
    /** print */
    @Override public String toString() {return String.format("(%s, %s)", mFirst, mSecond);}
}
