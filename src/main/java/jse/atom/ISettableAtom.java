package jse.atom;

public interface ISettableAtom extends IAtom {
    /** 返回自身用于链式调用 */
    ISettableAtom setX(double aX);
    ISettableAtom setY(double aY);
    ISettableAtom setZ(double aZ);
    ISettableAtom setID(int aID);
    ISettableAtom setType(int aType);
    
    default ISettableAtom setVx(double aVx) {throw new UnsupportedOperationException("setVx");}
    default ISettableAtom setVy(double aVy) {throw new UnsupportedOperationException("setVy");}
    default ISettableAtom setVz(double aVz) {throw new UnsupportedOperationException("setVz");}
}
