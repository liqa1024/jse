package com.guan.lmp;

import com.guan.math.MathEX;
import org.jetbrains.annotations.NotNull;

import static com.guan.code.CS.BOX_ONE;
import static com.guan.code.CS.BOX_ZERO;

/**
 * @author liqa
 * lammps 格式的模拟盒信息
 */
public class Box {
    final double @NotNull[] mBoxLo, mBoxHi;
    
    public Box() {this(BOX_ONE);}
    public Box(double aSize) {this(aSize, aSize, aSize);}
    public Box(double aX, double aY, double aZ) {this(BOX_ZERO, new double[] {aX, aY, aZ});}
    public Box(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi) {this(new double[] {aXlo, aYlo, aZlo}, new double[] {aXhi, aYhi, aZhi});}
    public Box(double[] aBox) {this(BOX_ZERO, aBox);}
    public Box(double @NotNull[] aBoxLo, double @NotNull[] aBoxHi) {
        mBoxLo = aBoxLo; mBoxHi = aBoxHi;
    }
    
    /// 获取属性
    public double xlo() {return mBoxLo[0];}
    public double xhi() {return mBoxHi[0];}
    public double ylo() {return mBoxLo[1];}
    public double yhi() {return mBoxHi[1];}
    public double zlo() {return mBoxLo[2];}
    public double zhi() {return mBoxHi[2];}
    public double[] boxLo() {return mBoxLo;}
    public double[] boxHi() {return mBoxHi;}
    public boolean isShifted() {return mBoxLo==BOX_ZERO;}
    public double @NotNull[] shiftedBox() {return mBoxHi==BOX_ONE ? BOX_ONE : (mBoxLo==BOX_ZERO ? mBoxHi : new double[]{mBoxHi[0]-mBoxLo[0], mBoxHi[1]-mBoxLo[1], mBoxHi[2]-mBoxLo[2]});}
    
    public Box copy() {return new Box(mBoxLo==BOX_ZERO?BOX_ZERO:MathEX.Vec.copy(mBoxLo), mBoxHi==BOX_ONE?BOX_ONE:MathEX.Vec.copy(mBoxHi));}
    
    // stuff to override
    protected Type type() {return Type.NORMAL;}
    
    public enum Type {
          NORMAL
        , PRISM
    }
}
