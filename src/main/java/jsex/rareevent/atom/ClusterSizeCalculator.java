package jsex.rareevent.atom;

import jse.atom.IAtomData;
import jse.atom.MonatomicParameterCalculator;
import jse.math.vector.ILogicalVector;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * <p>
 * 此接口可能随着版本的更新使用改进的算法，从而提高判断准确率，当然也会导致结果不一致；
 * 换而言之其他接口会尽可能保证结果一致
 * @author liqa
 */
public class ClusterSizeCalculator extends AbstractClusterSizeCalculator {
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {return aMPC.checkSolidConnectRatio6();}
}
