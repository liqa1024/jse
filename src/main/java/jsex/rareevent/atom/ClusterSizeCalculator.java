package jsex.rareevent.atom;

import jse.atom.IAtomData;
import jse.atom.MonatomicParameterCalculator;
import jse.math.vector.ILogicalVector;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * @author liqa
 */
public class ClusterSizeCalculator extends AbstractClusterSizeCalculator {
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {return aMPC.checkSolidConnectRatio6();}
}
