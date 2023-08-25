package com.jtool.rareevent.atom;

import com.jtool.atom.IAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.rareevent.IParameterCalculator;

import static com.jtool.code.CS.R_NEAREST_MUL;


/**
 * 一种参数计算器，计算体系中的固体部分的尺寸
 * @author liqa
 */
public class SolidSizeCalculator implements IParameterCalculator<IAtomData> {
    private final double mQ6CutoffMul;
    private final int mNnn;
    public SolidSizeCalculator(double aQ6CutoffMul, int aNnn) {mQ6CutoffMul = aQ6CutoffMul; mNnn = aNnn;}
    public SolidSizeCalculator(double aQ6CutoffMul) {this(aQ6CutoffMul, -1);}
    public SolidSizeCalculator() {this(R_NEAREST_MUL);}
    
    @Override public double lambdaOf(IAtomData aPoint) {
        try (MonatomicParameterCalculator tMPC = aPoint.getMonatomicParameterCalculator()) {
            return tMPC.checkSolidQ6(tMPC.unitLen()*mQ6CutoffMul, mNnn).count();
        }
    }
}
