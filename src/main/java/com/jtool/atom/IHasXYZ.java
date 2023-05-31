package com.jtool.atom;

public interface IHasXYZ {
    double x();
    double y();
    double z();
    
    /** 提供一些运算，由于 XYZ 本身就很轻量，为了避免方法调用的损失，并且让实现起来比较简单，这里不增加中间层 operation */
    default double product() {
        return x() * y() * z();
    }
    default IHasXYZ minus(IHasXYZ aRHS) {
        return new XYZ(x()-aRHS.x(), y()-aRHS.y(), z()-aRHS.z());
    }
    default IHasXYZ minus(double aX, double aY, double aZ) {
        return new XYZ(x()-aX, y()-aY, z()-aZ);
    }
    default IHasXYZ minus(double aRHS) {
        return new XYZ(x()-aRHS, y()-aRHS, z()-aRHS);
    }
    
    default IHasXYZ multiply(double aRHS) {
        return new XYZ(x()*aRHS, y()*aRHS, z()*aRHS);
    }
}
