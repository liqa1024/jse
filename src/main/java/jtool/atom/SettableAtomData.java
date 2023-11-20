package jtool.atom;

import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static jtool.code.CS.BOX_ONE;


/**
 * @author liqa
 * <p> 内部使用的通用的原子数据格式，直接使用 {@code List<IAtom>} 来存储数据 </p>
 * <p> 主要用于避免意义不大的匿名类的使用，并且也能减少意料之外的引用 </p>
 * <p> 这里所有的输入会直接作为成员，不进行值拷贝 </p>
 */
public final class SettableAtomData extends AbstractSettableAtomData {
    private final @Unmodifiable List<? extends ISettableAtom> mAtoms;
    private final IXYZ mBox;
    private int mAtomTypeNum;
    private final boolean mHasVelocities;
    
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IXYZ aBox, boolean aHasVelocities) {
        mAtoms = aAtoms;
        mBox = aBox;
        mAtomTypeNum = aAtomTypeNum;
        mHasVelocities = aHasVelocities;
    }
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, boolean aHasVelocities) {this(aAtoms, aAtomTypeNum, BOX_ONE, aHasVelocities);}
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, IXYZ aBox, boolean aHasVelocities) {this(aAtoms, 1, aBox, aHasVelocities);}
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, boolean aHasVelocities) {this(aAtoms, 1, aHasVelocities);}
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IXYZ aBox) {this(aAtoms, aAtomTypeNum, aBox, false);}
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum) {this(aAtoms, aAtomTypeNum, false);}
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, IXYZ aBox) {this(aAtoms, aBox, false);}
    public SettableAtomData(List<? extends ISettableAtom> aAtoms) {this(aAtoms, false);}
    
    @Override public ISettableAtom pickAtom(int aIdx) {
        // 需要包装一层，用于在更新种类时自动更新整体的种类计数
        final ISettableAtom tAtom = mAtoms.get(aIdx);
        return new ISettableAtom() {
            @Override public double x() {return tAtom.x();}
            @Override public double y() {return tAtom.y();}
            @Override public double z() {return tAtom.z();}
            @Override public int id() {return tAtom.id();}
            @Override public int type() {return tAtom.type();}
            /** 会复写掉内部的 index 数据 */
            @Override public int index() {return aIdx;}
            
            @Override public double vx() {return tAtom.vx();}
            @Override public double vy() {return tAtom.vy();}
            @Override public double vz() {return tAtom.vz();}
            
            /** 注意 return this 和 return tAtom 的区别 */
            @Override public ISettableAtom setX(double aX) {tAtom.setX(aX); return this;}
            @Override public ISettableAtom setY(double aY) {tAtom.setY(aY); return this;}
            @Override public ISettableAtom setZ(double aZ) {tAtom.setZ(aZ); return this;}
            @Override public ISettableAtom setID(int aID) {tAtom.setID(aID); return this;}
            @Override public ISettableAtom setType(int aType) {
                // 对于设置种类需要特殊处理，设置种类同时需要更新内部的原子种类计数
                tAtom.setType(aType);
                if (aType > atomTypeNum()) setAtomTypeNum(aType);
                return this;
            }
            
            @Override public ISettableAtom setVx(double aVx) {tAtom.setVx(aVx); return this;}
            @Override public ISettableAtom setVy(double aVy) {tAtom.setVy(aVy); return this;}
            @Override public ISettableAtom setVz(double aVz) {tAtom.setVz(aVz); return this;}
        };
    }
    @Override public IXYZ box() {return mBox;}
    @Override public int atomNum() {return mAtoms.size();}
    @Override public int atomTypeNum() {return mAtomTypeNum;}
    @Override public SettableAtomData setAtomTypeNum(int aAtomTypeNum) {mAtomTypeNum = aAtomTypeNum; return this;}
    @Override public boolean hasVelocities() {return mHasVelocities;}
}
