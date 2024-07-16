package jse.atom;


import jse.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;

/**
 * 可以设置的原子数据
 * @author liqa
 */
public interface ISettableAtomData extends IAtomData {
    void setAtom(int aIdx, IAtom aAtom);
    ISettableAtomData setAtomTypeNumber(int aAtomTypeNum);
    /** @deprecated use {@link #setAtomTypeNumber} */ @Deprecated default ISettableAtomData setAtomTypeNum(int aAtomTypeNum) {return setAtomTypeNumber(aAtomTypeNum);}
    ISettableAtomData setSymbols(String... aSymbols);
    ISettableAtomData setMasses(double... aMasses);
    ISettableAtomData setMasses(Collection<? extends Number> aMasses);
    ISettableAtomData setMasses(IVector aMasses);
    
    /** IAtomData stuffs*/
    List<? extends ISettableAtom> atoms();
    ISettableAtom atom(int aIdx);
    ISettableAtomDataOperation operation();
    @VisibleForTesting default ISettableAtomDataOperation opt() {return operation();}
    /** asList 接口保留兼容 */
    @Override @Deprecated @SuppressWarnings("deprecation") default List<? extends ISettableAtom> asList() {return atoms();}
    
    /** Groovy stuffs */
    @VisibleForTesting default int getAtomTypeNumber() {return atomTypeNumber();}
}
