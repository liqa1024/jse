package jse.atom;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;

import static jse.code.CS.MASS;
import static jse.code.CS.ZL_STR;


/**
 * @author liqa
 * <p> 内部使用的通用的原子数据格式，直接使用 {@code List<IAtom>} 来存储数据 </p>
 * <p> 主要用于避免意义不大的匿名类的使用，并且也能减少意料之外的引用 </p>
 * <p> 这里所有的输入会直接作为成员，不进行值拷贝 </p>
 */
public final class SettableAtomData extends AbstractSettableAtomData {
    private final @Unmodifiable List<? extends ISettableAtom> mAtoms;
    private final IBox mBox;
    private int mAtomTypeNum;
    private final boolean mHasVelocity;
    private String @Nullable[] mSymbols;
    
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasVelocity, String... aSymbols) {
        mAtoms = aAtoms;
        mBox = aBox;
        mAtomTypeNum = aAtomTypeNum;
        mHasVelocity = aHasVelocity;
        mSymbols = (aSymbols==null || aSymbols.length==0) ? null : aSymbols;
    }
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasVelocity) {
        this(aAtoms, aAtomTypeNum, aBox, aHasVelocity, aAtoms.get(0).hasSymbol() ? new String[aAtomTypeNum] : ZL_STR);
        if (mSymbols != null) for (IAtom tAtom : aAtoms) {
            int tTypeMM = Math.min(tAtom.type(), mAtomTypeNum) - 1;
            if (mSymbols[tTypeMM] == null) mSymbols[tTypeMM] = tAtom.symbol();
        }
    }
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, IBox aBox, boolean aHasVelocity) {
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
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox) {this(aAtoms, aAtomTypeNum, aBox, aAtoms.get(0).hasVelocity());}
    public SettableAtomData(List<? extends ISettableAtom> aAtoms,                   IBox aBox) {this(aAtoms, aBox, aAtoms.get(0).hasVelocity());}
    
    
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    @Override public @Nullable String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    @Override public boolean hasMass() {return hasSymbol();}
    @Override public double mass(int aType) {
        @Nullable String tSymbol = symbol(aType);
        return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
    }
    @Override public SettableAtomData setSymbols(String... aSymbols) {
        if (aSymbols==null || aSymbols.length==0) {
            mSymbols = null;
            return this;
        }
        if (mSymbols==null || aSymbols.length>mSymbols.length) mSymbols = Arrays.copyOf(aSymbols, aSymbols.length);
        else System.arraycopy(aSymbols, 0, mSymbols, 0, aSymbols.length);
        return this;
    }
    @Override public SettableAtomData setNoSymbol() {return setSymbols(ZL_STR);}
    @Override public ISettableAtom atom(int aIdx) {
        // 需要包装一层，用于在更新种类时自动更新整体的种类计数
        final ISettableAtom tAtom = mAtoms.get(aIdx);
        return new AbstractSettableAtom_() {
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
            
            /** 注意 return this 和 return tAtom 的区别 */
            @Override public ISettableAtom setX(double aX) {tAtom.setX(aX); return this;}
            @Override public ISettableAtom setY(double aY) {tAtom.setY(aY); return this;}
            @Override public ISettableAtom setZ(double aZ) {tAtom.setZ(aZ); return this;}
            @Override public ISettableAtom setID(int aID) {tAtom.setID(aID); return this;}
            @Override public ISettableAtom setType(int aType) {
                // 对于设置种类需要特殊处理，设置种类同时需要更新内部的原子种类计数
                tAtom.setType(aType);
                if (aType > atomTypeNumber()) setAtomTypeNumber(aType);
                return this;
            }
            
            /** 会复写掉内部的 hasVelocities 数据 */
            @Override public ISettableAtom setVx(double aVx) {if (!mHasVelocity) throw new UnsupportedOperationException("setVx"); tAtom.setVx(aVx); return this;}
            @Override public ISettableAtom setVy(double aVy) {if (!mHasVelocity) throw new UnsupportedOperationException("setVy"); tAtom.setVy(aVy); return this;}
            @Override public ISettableAtom setVz(double aVz) {if (!mHasVelocity) throw new UnsupportedOperationException("setVz"); tAtom.setVz(aVz); return this;}
        };
    }
    @Override public IBox box() {return mBox;}
    @Override public int atomNumber() {return mAtoms.size();}
    @Override public int atomTypeNumber() {return mAtomTypeNum;}
    @Override public SettableAtomData setAtomTypeNumber(int aAtomTypeNum) {
        int oTypeNum = mAtomTypeNum;
        if (aAtomTypeNum == oTypeNum) return this;
        mAtomTypeNum = aAtomTypeNum;
        if (aAtomTypeNum < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            for (ISettableAtom tAtom : mAtoms) if (tAtom.type() > aAtomTypeNum){
                tAtom.setType(aAtomTypeNum);
            }
            return this;
        }
        if (mSymbols!=null && mSymbols.length<aAtomTypeNum) {
            String[] rSymbols = new String[aAtomTypeNum];
            System.arraycopy(mSymbols, 0, rSymbols, 0, mSymbols.length);
            for (int tType = mSymbols.length+1; tType <= aAtomTypeNum; ++tType) rSymbols[tType-1] = "T" + tType;
            mSymbols = rSymbols;
        }
        return this;
    }
    @Override public boolean hasVelocity() {return mHasVelocity;}
}
