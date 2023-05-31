package com.jtool.atom;

public class Atom implements IAtom {
    public double mX, mY, mZ;
    public int mID, mType;
    
    public Atom(double aX, double aY, double aZ, int aID, int aType) {
        mX = aX; mY = aY; mZ = aZ;
        mID = aID; mType = aType;
    }
    public Atom(IHasXYZ aXYZ, int aID, int aType) {
        mX = aXYZ.x(); mY = aXYZ.y(); mZ = aXYZ.z();
        mID = aID; mType = aType;
    }
    public Atom(IHasXYZID aXYZID, int aType) {
        mX = aXYZID.x(); mY = aXYZID.y(); mZ = aXYZID.z();
        mID = aXYZID.id(); mType = aType;
    }
    public Atom(IAtom aAtom) {
        mX = aAtom.x(); mY = aAtom.y(); mZ = aAtom.z();
        mID = aAtom.id(); mType = aAtom.type();
    }
    public Atom(double aX, double aY, double aZ, int aID) {this(aX, aY, aZ, aID, 1);}
    public Atom(IHasXYZ aXYZ, int aID) {this(aXYZ, aID, 1);}
    public Atom(IHasXYZID aXYZID) {this(aXYZID, 1);}
    
    @Override public double x() {return mX;}
    @Override public double y() {return mY;}
    @Override public double z() {return mZ;}
    
    @Override public int id() {return mID;}
    @Override public int type() {return mType;}
}
