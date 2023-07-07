package com.jtool.lmp;

import com.jtool.atom.IHasXYZ;
import com.jtool.atom.XYZ;
import org.jetbrains.annotations.NotNull;

import static com.jtool.code.CS.*;
import static com.jtool.code.UT.Code.toBOX;

/**
 * lammps 格式的模拟盒信息
 * @author liqa
 */
public class Box {
    private final @NotNull XYZ mBoxLo, mBoxHi;
    
    public Box() {this(BOX_ONE);}
    public Box(double aSize) {this(aSize, aSize, aSize);}
    public Box(double aX, double aY, double aZ) {this(BOX_ZERO, new XYZ(aX, aY, aZ));}
    public Box(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi) {this(new XYZ(aXlo, aYlo, aZlo), new XYZ(aXhi, aYhi, aZhi));}
    public Box(@NotNull IHasXYZ aBox) {this(BOX_ZERO, aBox);}
    public Box(@NotNull IHasXYZ aBoxLo, @NotNull IHasXYZ aBoxHi) {this(toBOX(aBoxLo), toBOX(aBoxHi));}
    public Box(@NotNull XYZ aBoxLo, @NotNull XYZ aBoxHi) {mBoxLo = aBoxLo; mBoxHi = aBoxHi;}
    public Box(Box aBox) {this(toBOX(aBox.mBoxLo), toBOX(aBox.mBoxHi));}
    
    /// 获取属性
    public double xlo() {return mBoxLo.mX;}
    public double xhi() {return mBoxHi.mX;}
    public double ylo() {return mBoxLo.mY;}
    public double yhi() {return mBoxHi.mY;}
    public double zlo() {return mBoxLo.mZ;}
    public double zhi() {return mBoxHi.mZ;}
    public XYZ boxLo() {return mBoxLo;}
    public XYZ boxHi() {return mBoxHi;}
    public boolean isShifted() {return mBoxLo==BOX_ZERO;}
    public @NotNull XYZ shiftedBox() {return mBoxHi==BOX_ONE ? BOX_ONE : (mBoxLo==BOX_ZERO ? mBoxHi : mBoxHi.minus(mBoxLo));}
    
    public Box copy() {return new Box(this);}
    
    // stuff to override
    protected Type type() {return Type.NORMAL;}
    
    public enum Type {
          NORMAL
        , PRISM
    }
}
