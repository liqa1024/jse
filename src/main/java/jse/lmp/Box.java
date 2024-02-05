package jse.lmp;

import jse.atom.IXYZ;
import jse.atom.XYZ;
import org.jetbrains.annotations.NotNull;

import static jse.code.CS.*;
import static jse.code.UT.Code.newBox;

/**
 * lammps 格式的模拟盒信息
 * @author liqa
 */
public class Box {
    private final @NotNull IXYZ mBoxLo, mBoxHi;
    
    public Box() {this(BOX_ONE);}
    public Box(double aSize) {this(aSize, aSize, aSize);}
    public Box(double aX, double aY, double aZ) {this(BOX_ZERO, new XYZ(aX, aY, aZ));}
    public Box(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi) {this(new XYZ(aXlo, aYlo, aZlo), new XYZ(aXhi, aYhi, aZhi));}
    public Box(@NotNull IXYZ aBox) {this(BOX_ZERO, aBox);}
    public Box(Box aBox) {this(aBox.mBoxLo, aBox.mBoxHi);}
    public Box(@NotNull IXYZ aBoxLo, @NotNull IXYZ aBoxHi) {mBoxLo = newBox(aBoxLo); mBoxHi = newBox(aBoxHi);}
    
    /// 获取属性
    public final double xlo() {return mBoxLo.x();}
    public final double xhi() {return mBoxHi.x();}
    public final double ylo() {return mBoxLo.y();}
    public final double yhi() {return mBoxHi.y();}
    public final double zlo() {return mBoxLo.z();}
    public final double zhi() {return mBoxHi.z();}
    public final IXYZ boxLo() {return mBoxLo;}
    public final IXYZ boxHi() {return mBoxHi;}
    public final boolean isShifted() {return mBoxLo==BOX_ZERO;}
    public final @NotNull IXYZ shiftedBox() {return mBoxLo==BOX_ZERO ? mBoxHi : mBoxHi.minus(mBoxLo);}
    
    public Box copy() {return new Box(this);}
    
    // stuff to override
    protected Type type() {return Type.NORMAL;}
    
    @Override public String toString() {
        return String.format("{boxlo: (%.4g, %.4g, %.4g), boxhi: (%.4g, %.4g, %.4g)}", mBoxLo.x(), mBoxLo.y(), mBoxLo.z(), mBoxHi.x(), mBoxHi.y(), mBoxHi.z());
    }
    
    public enum Type {
          NORMAL
        , PRISM
    }
}
