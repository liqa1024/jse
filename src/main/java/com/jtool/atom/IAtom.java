package com.jtool.atom;

public interface IAtom extends IHasXYZ {
    double[] xyz();
    int type();
    int id();
    
    boolean hasType();
    boolean hasID();
}
