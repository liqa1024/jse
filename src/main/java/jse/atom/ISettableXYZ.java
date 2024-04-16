package jse.atom;

import org.jetbrains.annotations.VisibleForTesting;

public interface ISettableXYZ extends IXYZ {
    /** 批量设置的接口，返回自身方便链式调用 */
    ISettableXYZ setX(double aX);
    ISettableXYZ setY(double aY);
    ISettableXYZ setZ(double aZ);
    default ISettableXYZ setXYZ(double aX, double aY, double aZ) {return setX(aX).setY(aY).setZ(aZ);}
    default ISettableXYZ setXYZ(IXYZ aXYZ) {return setXYZ(aXYZ.x(), aXYZ.y(), aXYZ.z());}
    
    /** Groovy stuffs */
    @VisibleForTesting default double getX() {return x();}
    @VisibleForTesting default double getY() {return y();}
    @VisibleForTesting default double getZ() {return z();}
    
    default void cross2this(IXYZ aRHS) {cross2this(aRHS.x(), aRHS.y(), aRHS.z());}
    default void cross2this(XYZ aRHS) {cross2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default void cross2this(double aX, double aY, double aZ) {
        double tX = x();
        double tY = y();
        double tZ = z();
        setXYZ(
            tY*aZ - aY*tZ,
            tZ*aX - aZ*tX,
            tX*aY - aX*tY
        );
    }
    
    default void negative2this() {setXYZ(-x(), -y(), -z());}
    
    default void plus2this(IXYZ aRHS) {plus2this(aRHS.x(), aRHS.y(), aRHS.z());}
    default void plus2this(XYZ aRHS) {plus2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default void plus2this(double aX, double aY, double aZ) {setXYZ(x()+aX, y()+aY, z()+aZ);}
    default void plus2this(double aRHS) {setXYZ(x()+aRHS, y()+aRHS, z()+aRHS);}
    
    /** 也增加这个运算方便使用 */
    default void mplus2this(IXYZ aRHS, double aMul) {mplus2this(aRHS.x(), aRHS.y(), aRHS.z(), aMul);}
    default void mplus2this(XYZ aRHS, double aMul) {mplus2this(aRHS.mX, aRHS.mY, aRHS.mZ, aMul);}
    default void mplus2this(double aX, double aY, double aZ, double aMul) {setXYZ(x() + aMul*aX, y() + aMul*aY, z() + aMul*aZ);}
    
    default void minus2this(IXYZ aRHS) {minus2this(aRHS.x(), aRHS.y(), aRHS.z());}
    default void minus2this(XYZ aRHS) {minus2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default void minus2this(double aX, double aY, double aZ) {setXYZ(x()-aX, y()-aY, z()-aZ);}
    default void minus2this(double aRHS) {setXYZ(x()-aRHS, y()-aRHS, z()-aRHS);}
    
    default void lminus2this(IXYZ aRHS) {lminus2this(aRHS.x(), aRHS.y(), aRHS.z());}
    default void lminus2this(XYZ aRHS) {lminus2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default void lminus2this(double aX, double aY, double aZ) {setXYZ(aX-x(), aY-y(), aZ-z());}
    default void lminus2this(double aRHS) {setXYZ(aRHS-x(), aRHS-y(), aRHS-z());}
    
    default void multiply2this(IXYZ aRHS) {multiply2this(aRHS.x(), aRHS.y(), aRHS.z());}
    default void multiply2this(XYZ aRHS) {multiply2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default void multiply2this(double aX, double aY, double aZ) {setXYZ(x()*aX, y()*aY, z()*aZ);}
    default void multiply2this(double aRHS) {setXYZ(x()*aRHS, y()*aRHS, z()*aRHS);}
    
    default void div2this(IXYZ aRHS) {div2this(aRHS.x(), aRHS.y(), aRHS.z());}
    default void div2this(XYZ aRHS) {div2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default void div2this(double aX, double aY, double aZ) {setXYZ(x()/aX, y()/aY, z()/aZ);}
    default void div2this(double aRHS) {setXYZ(x()/aRHS, y()/aRHS, z()/aRHS);}
    
    default void ldiv2this(IXYZ aRHS) {ldiv2this(aRHS.x(), aRHS.y(), aRHS.z());}
    default void ldiv2this(XYZ aRHS) {ldiv2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default void ldiv2this(double aX, double aY, double aZ) {setXYZ(aX/x(), aY/y(), aZ/z());}
    default void ldiv2this(double aRHS) {setXYZ(aRHS/x(), aRHS/y(), aRHS/z());}
}
