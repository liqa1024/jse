package jse.code.collection;

public interface IPair<A, B> {
    A first();
    B second();
    
    boolean equals(Object other);
    int hashCode();
}
