package com.jtool.atom;

/** 现在认为原子无论怎样都会拥有这些属性 */
public interface IAtom extends IXYZ {
    double x();
    double y();
    double z();
    int id();
    int type();
    
    default double vx() {return 0.0;}
    default double vy() {return 0.0;}
    default double vz() {return 0.0;}
}
