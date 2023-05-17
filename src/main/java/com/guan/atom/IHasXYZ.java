package com.guan.atom;

public interface IHasXYZ {
    double[] xyz();
    default double x() {return xyz()[0];}
    default double y() {return xyz()[1];}
    default double z() {return xyz()[2];}
}
