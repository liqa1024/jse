package jsex.rareevent.atom;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * {@link MultiTypeClusterSizeCalculator} 的 solid 版本
 * @author liqa
 */
public class MultiTypeSolidSizeCalculator extends MultiTypeClusterSizeCalculator {
    public MultiTypeSolidSizeCalculator(ISolidChecker aAllSolidChecker, ISolidChecker @Nullable[] aTypeSolidCheckers) {super(aAllSolidChecker, aTypeSolidCheckers);}
    public MultiTypeSolidSizeCalculator(ISolidChecker aAllSolidChecker) {super(aAllSolidChecker);}
    public MultiTypeSolidSizeCalculator(ISolidChecker aAllSolidChecker, Collection<? extends ISolidChecker> aTypeSolidCheckers) {super(aAllSolidChecker, aTypeSolidCheckers);}
    
    @Override protected boolean countAll() {return true;}
}
