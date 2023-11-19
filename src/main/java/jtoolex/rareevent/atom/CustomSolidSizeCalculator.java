package jtoolex.rareevent.atom;

/**
 * {@link CustomClusterSizeCalculator} 的 solid 版本
 * @author liqa
 */
public class CustomSolidSizeCalculator extends CustomClusterSizeCalculator {
    public CustomSolidSizeCalculator(ISolidChecker aSolidChecker, ISolidChecker... aRestSolidCheckers) {super(aSolidChecker, aRestSolidCheckers);}
    public CustomSolidSizeCalculator(ISolidChecker aSolidChecker) {super(aSolidChecker);}
    
    @Override protected boolean countAll() {return true;}
}
