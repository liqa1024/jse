package com.jtool.atom;

import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static com.jtool.code.CS.*;


/**
 * @author liqa
 * <p> 内部使用的通用的原子数据格式，直接使用 {@code List<IAtom>} 来存储数据 </p>
 * <p> 主要用于避免意义不大的匿名类的使用，并且也能减少意料之外的引用 </p>
 * <p> 这里所有的输入会直接作为成员，不进行值拷贝 </p>
 */
public final class AtomData extends AbstractAtomData {
    private final @Unmodifiable List<? extends IAtom> mAtoms;
    private final IXYZ mBox;
    private final int mAtomTypeNum;
    private final boolean mHasVelocities;
    
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IXYZ aBox, boolean aHasVelocities) {
        mAtoms = aAtoms;
        mBox = aBox;
        mAtomTypeNum = aAtomTypeNum;
        mHasVelocities = aHasVelocities;
    }
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum,            boolean aHasVelocities) {this(aAtoms, aAtomTypeNum, BOX_ONE, aHasVelocities);}
    public AtomData(List<? extends IAtom> aAtoms,                   IXYZ aBox, boolean aHasVelocities) {this(aAtoms, 1, aBox, aHasVelocities);}
    public AtomData(List<? extends IAtom> aAtoms,                              boolean aHasVelocities) {this(aAtoms, 1, aHasVelocities);}
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IXYZ aBox                        ) {this(aAtoms, aAtomTypeNum, aBox, false);}
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum                                   ) {this(aAtoms, aAtomTypeNum, false);}
    public AtomData(List<? extends IAtom> aAtoms,                   IXYZ aBox                        ) {this(aAtoms, aBox, false);}
    public AtomData(List<? extends IAtom> aAtoms                                                     ) {this(aAtoms, false);}
    
    @Override public IAtom pickAtom(int aIdx) {return mAtoms.get(aIdx);}
    @Override public IXYZ box() {return mBox;}
    @Override public int atomNum() {return mAtoms.size();}
    @Override public int atomTypeNum() {return mAtomTypeNum;}
    @Override public boolean hasVelocities() {return mHasVelocities;}
}
