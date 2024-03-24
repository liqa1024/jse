package jse.lmp;

import jse.atom.IBox;
import jse.atom.IXYZ;
import jse.atom.XYZ;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jse.code.CS.XYZ_ZERO;

/**
 * lammps 格式的模拟盒信息
 * @author liqa
 */
public class LmpBox implements IBox {
    private final @Nullable XYZ mBoxLo;
    private final @NotNull XYZ mBoxHi;
    
    public LmpBox(double aSize) {this(aSize, aSize, aSize);}
    public LmpBox(double aX, double aY, double aZ) {mBoxLo = null; mBoxHi = new XYZ(aX, aY, aZ);}
    public LmpBox(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi) {mBoxLo = new XYZ(aXlo, aYlo, aZlo); mBoxHi = new XYZ(aXhi, aYhi, aZhi);}
    public LmpBox(@NotNull IXYZ aBox) {mBoxLo = null; mBoxHi = new XYZ(aBox);}
    public LmpBox(@NotNull IXYZ aBoxLo, @NotNull IXYZ aBoxHi) {mBoxLo = new XYZ(aBoxLo); mBoxHi = new XYZ(aBoxHi);}
    LmpBox(LmpBox aLmpBox) {mBoxLo = aLmpBox.mBoxLo==null ? null : new XYZ(aLmpBox.mBoxLo); mBoxHi = new XYZ(aLmpBox.mBoxHi);}
    
    /** IBox stuffs */
    @Override public LmpBox copy() {return new LmpBox(this);}
    
    @Override public final double ax() {return mBoxLo==null ? mBoxHi.mX : (mBoxHi.mX-mBoxLo.mX);}
    @Override public final double by() {return mBoxLo==null ? mBoxHi.mY : (mBoxHi.mY-mBoxLo.mY);}
    @Override public final double cz() {return mBoxLo==null ? mBoxHi.mZ : (mBoxHi.mZ-mBoxLo.mZ);}
    
    @Override public String toString() {
        return String.format("{boxlo: (%.4g, %.4g, %.4g), boxhi: (%.4g, %.4g, %.4g)}", xlo(), ylo(), zlo(), xhi(), yhi(), zhi());
    }
    
    /** LmpBox 特有属性 */
    public final double xlo() {return mBoxLo==null ? 0.0 : mBoxLo.mX;}
    public final double xhi() {return mBoxHi.mX;}
    public final double ylo() {return mBoxLo==null ? 0.0 : mBoxLo.mY;}
    public final double yhi() {return mBoxHi.mY;}
    public final double zlo() {return mBoxLo==null ? 0.0 : mBoxLo.mZ;}
    public final double zhi() {return mBoxHi.mZ;}
    
    /** 现在这些会引起混淆，只是保留兼容 */
    @Deprecated public final IXYZ boxLo() {return mBoxLo==null ? XYZ_ZERO : mBoxLo;}
    @Deprecated public final IXYZ boxHi() {return mBoxLo==null ? new XYZ(this) : plus(mBoxLo);}
    @Deprecated public final boolean isShifted() {return mBoxLo == null;}
    @Deprecated public final @NotNull IXYZ shiftedBox() {return new XYZ(this);}
}
