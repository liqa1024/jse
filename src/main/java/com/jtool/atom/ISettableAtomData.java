package com.jtool.atom;


import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * 可以设置的原子数据
 * @author liqa
 */
public interface ISettableAtomData extends IAtomData {
    void setAtom(int aIdx, IAtom aAtom);
    ISettableAtomData setAtomTypeNum(int aAtomTypeNum);
    /** IAtomData stuffs*/
    List<? extends ISettableAtom> atoms();
    ISettableAtom pickAtom(int aIdx);
    ISettableAtomDataOperation operation();
    @VisibleForTesting default ISettableAtomDataOperation opt() {return operation();}
}
