package jse.atom;


import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * 可以设置的原子数据
 * @author liqa
 */
public interface ISettableAtomData extends IAtomData {
    void setAtom(int aIdx, IAtom aAtom);
    ISettableAtomData setAtomTypeNumber(int aAtomTypeNum);
    @Deprecated default ISettableAtomData setAtomTypeNum(int aAtomTypeNum) {return setAtomTypeNumber(aAtomTypeNum);}
    
    /** IAtomData stuffs*/
    List<? extends ISettableAtom> asList();
    ISettableAtom pickAtom(int aIdx);
    ISettableAtomDataOperation operation();
    @VisibleForTesting default ISettableAtomDataOperation opt() {return operation();}
}
