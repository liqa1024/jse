package jse.vasp;

import jse.atom.XYZ;

public final class VaspBoxPrism extends VaspBox {
    private final double mAy, mAz;
    private final double mBx, mBz;
    private final double mCx, mCy;
    
    public VaspBoxPrism(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz, double aScale) {
        super(aAx, aBy, aCz, aScale);
        mAy = aAy; mAz = aAz;
        mBx = aBx; mBz = aBz;
        mCx = aCx; mCy = aCy;
    }
    public VaspBoxPrism(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        super(aAx, aBy, aCz);
        mAy = aAy; mAz = aAz;
        mBx = aBx; mBz = aBz;
        mCx = aCx; mCy = aCy;
    }
    VaspBoxPrism(VaspBox aVaspBox, double aAy, double aAz, double aBx, double aBz, double aCx, double aCy) {
        super(aVaspBox);
        mAy = aAy; mAz = aAz;
        mBx = aBx; mBz = aBz;
        mCx = aCx; mCy = aCy;
    }
    @SuppressWarnings("CopyConstructorMissesField")
    VaspBoxPrism(VaspBoxPrism aVaspBoxPrism) {
        super(aVaspBoxPrism);
        mAy = aVaspBoxPrism.mAy; mAz = aVaspBoxPrism.mAz;
        mBx = aVaspBoxPrism.mBx; mBz = aVaspBoxPrism.mBz;
        mCx = aVaspBoxPrism.mCx; mCy = aVaspBoxPrism.mCy;
    }
    
    /** VaspBox stuffs */
    @Override public double iay() {return mAy;}
    @Override public double iaz() {return mAz;}
    @Override public double ibx() {return mBx;}
    @Override public double ibz() {return mBz;}
    @Override public double icx() {return mCx;}
    @Override public double icy() {return mCy;}
    
    /** IBox stuffs */
    @Override public boolean isLmpStyle() {return false;}
    @Override public boolean isPrism() {return true;}
    
    @Override public VaspBoxPrism copy() {return new VaspBoxPrism(this);}
    
    
    /** 为了加速运算，内部会缓存中间变量，再修改 scale 时会让这些缓存失效 */
    private XYZ mBC = null, mCA = null, mAB = null;
    private double mV = Double.NaN;
    @Override public void toDirect(XYZ rCartesian) {
        if (mBC == null) {
            XYZ tA = XYZ.toXYZ(a());
            XYZ tB = XYZ.toXYZ(b());
            XYZ tC = XYZ.toXYZ(c());
            mBC = tB.cross(tC);
            mCA = tC.cross(tA);
            mAB = tA.cross(tB);
            mV = tA.mixed(tB, tC);
        }
        rCartesian.setXYZ(
            mBC.dot(rCartesian) / mV,
            mCA.dot(rCartesian) / mV,
            mAB.dot(rCartesian) / mV
        );
    }
    @Override protected void onAnyChange_() {mBC = null;}
}
