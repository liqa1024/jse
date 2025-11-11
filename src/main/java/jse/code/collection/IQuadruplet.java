package jse.code.collection;

public interface IQuadruplet<A, B, C, D> {
    A a();
    B b();
    C c();
    D d();
    
    boolean equals(Object other);
    int hashCode();
}
