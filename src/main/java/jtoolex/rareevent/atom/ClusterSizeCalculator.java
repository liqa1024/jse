package jtoolex.rareevent.atom;

import jtool.atom.IAtomData;
import jtool.atom.MonatomicParameterCalculator;
import jtool.math.vector.ILogicalVector;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * @author liqa
 */
public class ClusterSizeCalculator extends AbstractClusterSizeCalculator {
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {return aMPC.checkSolidQ6();}
}
