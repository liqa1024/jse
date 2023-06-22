package com.jtool.rareevent.atom;

import com.jtool.atom.IHasAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.math.vector.IVector;
import com.jtool.rareevent.IParameterCalculator;


/**
 * 一种参数计算机，计算体系中的固体部分的尺寸
 * @author liqa
 */
public class SolidSizeCalculator implements IParameterCalculator<IHasAtomData> {
    @Override public double lambdaOf(IHasAtomData aPoint) {
        // 进行类固体判断
        IVector tIsSolid;
        try (MonatomicParameterCalculator tMPC = aPoint.getMPC()) {tIsSolid = tMPC.checkSolidQ6();}
        // 统计 lambda
        return tIsSolid.operation().sum();
    }
}
