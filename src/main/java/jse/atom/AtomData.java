package jse.atom;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static jse.code.CS.MASS;
import static jse.code.CS.ZL_STR;


/**
 * @author liqa
 * <p> 内部使用的通用的原子数据格式，直接使用 {@code List<IAtom>} 来存储数据 </p>
 * <p> 主要用于避免意义不大的匿名类的使用，并且也能减少意料之外的引用 </p>
 * <p> 这里所有的输入会直接作为成员，不进行值拷贝 </p>
 */
public final class AtomData extends AbstractAtomData {
    private final @Unmodifiable List<? extends IAtom> mAtoms;
    private final IBox mBox;
    private final int mAtomTypeNum;
    private final boolean mHasVelocity;
    private final String @Nullable[] mSymbols;
    
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasVelocity, String... aSymbols) {
        mAtoms = aAtoms;
        mBox = aBox;
        mAtomTypeNum = aAtomTypeNum;
        mHasVelocity = aHasVelocity;
        mSymbols = (aSymbols==null || aSymbols.length==0) ? null : aSymbols;
    }
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasVelocity) {
        this(aAtoms, aAtomTypeNum, aBox, aHasVelocity, aAtoms.get(0).hasSymbol() ? new String[aAtomTypeNum] : ZL_STR);
        if (mSymbols != null) for (IAtom tAtom : aAtoms) {
            int tTypeMM = Math.min(tAtom.type(), mAtomTypeNum) - 1;
            if (mSymbols[tTypeMM] == null) mSymbols[tTypeMM] = tAtom.symbol();
        }
    }
    public AtomData(List<? extends IAtom> aAtoms, IBox aBox, boolean aHasVelocity) {
        mAtoms = aAtoms;
        mBox = aBox;
        mHasVelocity = aHasVelocity;
        int tAtomTypeNum = 1;
        for (IAtom tAtom : aAtoms) {
            tAtomTypeNum = Math.max(tAtom.type(), tAtomTypeNum);
        }
        mAtomTypeNum = tAtomTypeNum;
        mSymbols = aAtoms.get(0).hasSymbol() ? new String[mAtomTypeNum] : null;
        if (mSymbols != null) for (IAtom tAtom : aAtoms) {
            int tTypeMM = tAtom.type() - 1;
            if (mSymbols[tTypeMM] == null) mSymbols[tTypeMM] = tAtom.symbol();
        }
    }
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IBox aBox) {this(aAtoms, aAtomTypeNum, aBox, aAtoms.get(0).hasVelocity());}
    public AtomData(List<? extends IAtom> aAtoms,                   IBox aBox) {this(aAtoms, aBox, aAtoms.get(0).hasVelocity());}
    
    
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    @Override public @Nullable String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    @Override public boolean hasMass() {return hasSymbol();}
    @Override public double mass(int aType) {
        @Nullable String tSymbol = symbol(aType);
        return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
    }
    @Override public IAtom atom(int aIdx) {
        // 需要包装一层，用于自动复写内部原子的 index 信息
        final IAtom tAtom = mAtoms.get(aIdx);
        return new AbstractAtom_() {
            @Override public double x() {return tAtom.x();}
            @Override public double y() {return tAtom.y();}
            @Override public double z() {return tAtom.z();}
            @Override public int id() {return tAtom.id();}
            @Override public int type() {return Math.min(tAtom.type(), mAtomTypeNum);}
            /** 会复写掉内部的 index 数据 */
            @Override public int index() {return aIdx;}
            
            /** 会复写掉内部的 hasVelocities 数据 */
            @Override public double vx() {return mHasVelocity ? tAtom.vx() : 0.0;}
            @Override public double vy() {return mHasVelocity ? tAtom.vy() : 0.0;}
            @Override public double vz() {return mHasVelocity ? tAtom.vz() : 0.0;}
        };
    }
    @Override public IBox box() {return mBox;}
    @Override public int atomNumber() {return mAtoms.size();}
    @Override public int atomTypeNumber() {return mAtomTypeNum;}
    @Override public boolean hasVelocity() {return mHasVelocity;}
}
