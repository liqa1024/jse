package com.jtool.rareevent.atom;

import com.jtool.atom.IAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.math.vector.ILogicalVector;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * @author liqa
 */
public class ClusterSizeCalculator extends AbstractClusterSizeCalculator {
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {return aMPC.checkSolidQ6();}
}
