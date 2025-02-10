package jse.vasp;

import jse.atom.XYZ;
import jse.math.MathEX;

public final class VaspBoxPrism extends VaspBox {
    private final double mIAy, mIAz;
    private final double mIBx, mIBz;
    private final double mICx, mICy;
    public VaspBoxPrism(double aIAx, double aIAy, double aIAz, double aIBx, double aIBy, double aIBz, double aICx, double aICy, double aICz, double aScale) {
        super(aIAx, aIBy, aICz, aScale);
        mIAy = aIAy; mIAz = aIAz;
        mIBx = aIBx; mIBz = aIBz;
        mICx = aICx; mICy = aICy;
        // 现在直接在创建时初始化缓存，从根本上杜绝线程读取不安全的问题
        initCache_();
    }
    public VaspBoxPrism(double aIAx, double aIAy, double aIAz, double aIBx, double aIBy, double aIBz, double aICx, double aICy, double aICz) {
        super(aIAx, aIBy, aICz);
        mIAy = aIAy; mIAz = aIAz;
        mIBx = aIBx; mIBz = aIBz;
        mICx = aICx; mICy = aICy;
        // 现在直接在创建时初始化缓存，从根本上杜绝线程读取不安全的问题
        initCache_();
    }
    VaspBoxPrism(VaspBox aVaspBox, double aIAy, double aIAz, double aIBx, double aIBz, double aICx, double aICy) {
        super(aVaspBox);
        mIAy = aIAy; mIAz = aIAz;
        mIBx = aIBx; mIBz = aIBz;
        mICx = aICx; mICy = aICy;
        // 现在直接在创建时初始化缓存，从根本上杜绝线程读取不安全的问题
        initCache_();
    }
    VaspBoxPrism(VaspBoxPrism aVaspBoxPrism) {
        super(aVaspBoxPrism);
        mIAy = aVaspBoxPrism.mIAy; mIAz = aVaspBoxPrism.mIAz;
        mIBx = aVaspBoxPrism.mIBx; mIBz = aVaspBoxPrism.mIBz;
        mICx = aVaspBoxPrism.mICx; mICy = aVaspBoxPrism.mICy;
        // 现在直接在创建时初始化缓存，从根本上杜绝线程读取不安全的问题
        initCache_();
    }
    
    /** VaspBox stuffs */
    @Override public double iay() {return mIAy;}
    @Override public double iaz() {return mIAz;}
    @Override public double ibx() {return mIBx;}
    @Override public double ibz() {return mIBz;}
    @Override public double icx() {return mICx;}
    @Override public double icy() {return mICy;}
    
    /** IBox stuffs */
    @Override public boolean isLmpStyle() {return false;}
    @Override public boolean isPrism() {return true;}
    
    @Override public VaspBoxPrism copy() {return new VaspBoxPrism(this);}
    
    
    /** 为了加速运算，内部会缓存中间变量，再修改 scale 时会让这些缓存失效 */
    private XYZ mBC = null, mCA = null, mAB = null;
    private double mV = Double.NaN;
    private void initCache_() {
        XYZ tA = XYZ.toXYZ(a());
        XYZ tB = XYZ.toXYZ(b());
        XYZ tC = XYZ.toXYZ(c());
        mBC = tB.cross(tC);
        mCA = tC.cross(tA);
        mAB = tA.cross(tB);
        mV = tA.mixed(tB, tC);
    }
    @Override protected void onAnyChange_() {initCache_();}
    @Override public void toDirect(XYZ rCartesian) {
        rCartesian.setXYZ(
            mBC.dot(rCartesian) / mV,
            mCA.dot(rCartesian) / mV,
            mAB.dot(rCartesian) / mV
        );
        // direct 需要考虑计算误差带来的出边界的问题，现在支持自动靠近所有整数值
        if (Math.abs(rCartesian.mX) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mX = 0.0;
        } else {
            int tIntX = MathEX.Code.round2int(rCartesian.mX);
            if (tIntX!=0 && MathEX.Code.numericEqual(rCartesian.mX, tIntX)) rCartesian.mX = tIntX;
        }
        if (Math.abs(rCartesian.mY) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mY = 0.0;
        } else {
            int tIntY = MathEX.Code.round2int(rCartesian.mY);
            if (tIntY!=0 && MathEX.Code.numericEqual(rCartesian.mY, tIntY)) rCartesian.mY = tIntY;
        }
        if (Math.abs(rCartesian.mZ) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mZ = 0.0;
        } else {
            int tIntZ = MathEX.Code.round2int(rCartesian.mZ);
            if (tIntZ!=0 && MathEX.Code.numericEqual(rCartesian.mZ, tIntZ)) rCartesian.mZ = tIntZ;
        }
    }
}
