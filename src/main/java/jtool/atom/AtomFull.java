package jtool.atom;

/**
 * 包含所有属性的原子
 * @author liqa
 */
public class AtomFull extends Atom {
    public double mVx, mVy, mVz;
    public AtomFull(IAtom aAtom) {
        super(aAtom);
        mVx = aAtom.vx();
        mVy = aAtom.vy();
        mVz = aAtom.vz();
    }
    public AtomFull() {
        super();
        mVx = 0.0; mVy = 0.0; mVz = 0.0;
    }
    
    @Override public double vx() {return mVx;}
    @Override public double vy() {return mVy;}
    @Override public double vz() {return mVz;}
    
    @Override public AtomFull setVx(double aVx) {mVx = aVx; return this;}
    @Override public AtomFull setVy(double aVy) {mVy = aVy; return this;}
    @Override public AtomFull setVz(double aVz) {mVz = aVz; return this;}
}
