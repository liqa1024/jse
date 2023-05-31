package com.jtool.atom;

/** 现在认为原子无论怎样都会拥有这些属性 */
public interface IAtom extends IHasXYZID {
    double x();
    double y();
    double z();
    int id();
    int type();
}
