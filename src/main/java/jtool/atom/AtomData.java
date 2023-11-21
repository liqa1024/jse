package jtool.atom;

import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static jtool.code.CS.*;


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
    
    @Override public IAtom pickAtom(int aIdx) {
        // 需要包装一层，用于自动复写内部原子的 index 信息
        final IAtom tAtom = mAtoms.get(aIdx);
        return new AbstractAtom() {
            @Override public double x() {return tAtom.x();}
            @Override public double y() {return tAtom.y();}
            @Override public double z() {return tAtom.z();}
            @Override public int id() {return tAtom.id();}
            @Override public int type() {return tAtom.type();}
            /** 会复写掉内部的 index 数据 */
            @Override public int index() {return aIdx;}
            
            /** 会复写掉内部的 hasVelocities 数据 */
            @Override public double vx() {return mHasVelocities ? tAtom.vx() : 0.0;}
            @Override public double vy() {return mHasVelocities ? tAtom.vy() : 0.0;}
            @Override public double vz() {return mHasVelocities ? tAtom.vz() : 0.0;}
            @Override public boolean hasVelocities() {return mHasVelocities;}
        };
    }
    @Override public IXYZ box() {return mBox;}
    @Override public int atomNum() {return mAtoms.size();}
    @Override public int atomTypeNum() {return mAtomTypeNum;}
    @Override public boolean hasVelocities() {return mHasVelocities;}
}
