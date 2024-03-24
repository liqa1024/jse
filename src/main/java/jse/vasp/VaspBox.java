package jse.vasp;

import jse.atom.IBox;
import jse.atom.IXYZ;
import jse.atom.XYZ;
import jse.math.matrix.IMatrix;
import jse.math.matrix.Matrices;
import org.jetbrains.annotations.VisibleForTesting;

public class VaspBox implements IBox {
    private final double mAx, mBy, mCz;
    private double mScale;
    public VaspBox(double aAx, double aBy, double aCz, double aScale) {mAx = aAx; mBy = aBy; mCz = aCz; mScale = aScale;}
    public VaspBox(double aAx, double aBy, double aCz) {this(aAx, aBy, aCz, 1.0);}
    VaspBox(VaspBox aVaspBox) {mAx = aVaspBox.mAx; mBy = aVaspBox.mBy; mCz = aVaspBox.mCz; mScale = aVaspBox.mScale;}
    
    @Override public VaspBox copy() {return new VaspBox(this);}
    
    @Override public final double ax() {return iax()*mScale;}
    @Override public final double ay() {return iay()*mScale;}
    @Override public final double az() {return iaz()*mScale;}
    @Override public final double bx() {return ibx()*mScale;}
    @Override public final double by() {return iby()*mScale;}
    @Override public final double bz() {return ibz()*mScale;}
    @Override public final double cx() {return icx()*mScale;}
    @Override public final double cy() {return icy()*mScale;}
    @Override public final double cz() {return icz()*mScale;}
    
    @Override public final String toString() {
        return String.format("scale: %.4g\n", mScale)
             + String.format("ia: (%.4g, %.4g, %.4g)\n", iax(), iay(), iaz())
             + String.format("ib: (%.4g, %.4g, %.4g)\n", ibx(), iby(), ibz())
             + String.format("ic: (%.4g, %.4g, %.4g)"  , icx(), icy(), icz());
    }
    
    /** VaspBox 特有属性，i 代表 internal */
    public final double iax() {return mAx;}
    public double iay() {return 0.0;}
    public double iaz() {return 0.0;}
    public double ibx() {return 0.0;}
    public final double iby() {return mBy;}
    public double ibz() {return 0.0;}
    public double icx() {return 0.0;}
    public double icy() {return 0.0;}
    public final double icz() {return mCz;}
    public final IXYZ ia() {return new XYZ(iax(), iay(), iaz());}
    public final IXYZ ib() {return new XYZ(ibx(), iby(), ibz());}
    public final IXYZ ic() {return new XYZ(icx(), icy(), icz());}
    /** 返回 internal 的 a, b, c 按行排列组成的矩阵，就像 POSCAR 文件中那样 */
    public final IMatrix iabc() {
        IMatrix rMat = Matrices.zeros(3);
        rMat.set(0, 0, iax()); rMat.set(0, 1, iay()); rMat.set(0, 2, iaz());
        rMat.set(1, 0, ibx()); rMat.set(1, 1, iby()); rMat.set(1, 2, ibz());
        rMat.set(2, 0, icx()); rMat.set(0, 1, icy()); rMat.set(2, 2, icz());
        return rMat;
    }
    /** 返回 internal 的 a, b, c 按行排列组成的矩阵的逆矩阵，主要用于方便 Cartesian 转为 Direct */
    public final IMatrix inviabc() {
        XYZ tA = XYZ.toXYZ(ia());
        XYZ tB = XYZ.toXYZ(ib());
        XYZ tC = XYZ.toXYZ(ic());
        XYZ tBC = tB.cross(tC);
        XYZ tCA = tC.cross(tA);
        XYZ tAB = tA.cross(tB);
        double tV = tA.mixed(tB, tC);
        
        IMatrix rMat = Matrices.zeros(3);
        rMat.set(0, 0, tBC.mX/tV); rMat.set(0, 1, tCA.mX/tV); rMat.set(0, 2, tAB.mX/tV);
        rMat.set(1, 0, tBC.mY/tV); rMat.set(1, 1, tCA.mY/tV); rMat.set(1, 2, tAB.mY/tV);
        rMat.set(2, 0, tBC.mZ/tV); rMat.set(0, 1, tCA.mZ/tV); rMat.set(2, 2, tAB.mZ/tV);
        return rMat;
    }
    
    public final VaspBox setScale(double aScale) {mScale = aScale; onAnyChange_(); return this;}
    public final double scale() {return mScale;}
    protected void onAnyChange_() {/**/}
    
    /** Groovy stuffs */
    @VisibleForTesting public final double getScale() {return mScale;}
}
