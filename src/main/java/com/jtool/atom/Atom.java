package com.jtool.atom;

public class Atom implements IAtom {
    private final double[] mXYZ;
    private final int mType;
    private final int mID;
    
    public Atom(double[] aXYZ, int aType, int aID) {
        mXYZ = aXYZ;
        mType = aType;
        mID = aID;
    }
    public Atom(double[] aXYZ, int aType) {this(aXYZ, aType, -1);}
    public Atom(double[] aXYZ) {this(aXYZ, -1, -1);}
    
    
    @Override public double[] xyz() {return mXYZ;}
    @Override public int type() {
        if (mType <= 0) throw new RuntimeException("This Atom do NOT have type");
        return mType;
    }
    @Override public int id() {
        if (mID <= 0) throw new RuntimeException("This Atom do NOT have id");
        return mID;
    }
    
    
    @Override public boolean hasType() {return mType > 0;}
    @Override public boolean hasID() {return mID > 0;}
}
