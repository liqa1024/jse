package jsex.rareevent.atom;

import jse.atom.IAtomData;
import jse.atom.MonatomicParameterCalculator;
import jse.math.vector.ILogicalVector;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * <p>
 * 可自定义固体判据的团簇计算器，可以输入多个 checker 一起判断然后取并集
 * @author liqa
 */
public class CustomClusterSizeCalculator extends AbstractClusterSizeCalculator {
    private final static ISolidChecker[] ZL_CHECKERS = new ISolidChecker[0];
    
    private final ISolidChecker mSolidChecker;
    private final ISolidChecker[] mRestSolidCheckers;
    public CustomClusterSizeCalculator(ISolidChecker aSolidChecker, ISolidChecker... aRestSolidCheckers) {
        if (aRestSolidCheckers == null) aRestSolidCheckers = ZL_CHECKERS;
        mSolidChecker = aSolidChecker;
        mRestSolidCheckers = aRestSolidCheckers;
    }
    /** 主要用于兼容外部调用 */
    public CustomClusterSizeCalculator(ISolidChecker aSolidChecker) {this(aSolidChecker, ZL_CHECKERS);}
    
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {
        ILogicalVector rIsSolid = mSolidChecker.checkSolid(aMPC);
        for (ISolidChecker tChecker : mRestSolidCheckers) rIsSolid.or2this(tChecker.checkSolid(aMPC));
        return rIsSolid;
    }
}
