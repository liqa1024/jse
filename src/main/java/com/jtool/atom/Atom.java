package com.jtool.atom;

/**
 * 用于一般使用的原子，不含有速度属性减少一定的内存占用
 * @author liqa
 */
public class Atom implements ISettableAtom {
    public double mX, mY, mZ;
    public int mID, mType;
    
    public Atom(double aX, double aY, double aZ, int aID, int aType) {
        mX = aX; mY = aY; mZ = aZ;
        mID = aID; mType = aType;
    }
    public Atom(IXYZ aXYZ, int aID, int aType) {
        mX = aXYZ.x(); mY = aXYZ.y(); mZ = aXYZ.z();
        mID = aID; mType = aType;
    }
    public Atom(IAtom aAtom) {
        mX = aAtom.x(); mY = aAtom.y(); mZ = aAtom.z();
        mID = aAtom.id(); mType = aAtom.type();
    }
    public Atom(double aX, double aY, double aZ, int aID) {this(aX, aY, aZ, aID, 1);}
    public Atom(IXYZ aXYZ, int aID) {this(aXYZ, aID, 1);}
    public Atom(IXYZ aXYZ) {this(aXYZ, 1, 1);}
    public Atom() {this(0.0, 0.0, 0.0, 1, 1);}
    
    @Override public double x() {return mX;}
    @Override public double y() {return mY;}
    @Override public double z() {return mZ;}
    
    @Override public int id() {return mID;}
    @Override public int type() {return mType;}
    
    
    /** ISettableAtom stuffs */
    @Override public Atom setX(double aX) {mX = aX; return this;}
    @Override public Atom setY(double aY) {mY = aY; return this;}
    @Override public Atom setZ(double aZ) {mZ = aZ; return this;}
    @Override public Atom setID(int aID) {mID = aID; return this;}
    @Override public Atom setType(int aType) {mType = aType; return this;}
}
