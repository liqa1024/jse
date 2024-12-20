package jse.atom;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;

import static jse.code.CS.MASS;
import static jse.code.CS.ZL_STR;


/**
 * 一般的可以设置的原子数据实现，用于从头直接创建原子数据
 * <p>
 * 一般来说，需要创建一个原子列表 {@code List<Atom>}
 * 以及所在的模拟盒信息 {@link Box}，由此即可创建 {@link AtomData}：
 * <pre> {@code
 * def atoms = [
 *     new Atom(0.0, 0.0, 0.0),
 *     new Atom(0.5, 0.5, 0.0),
 *     new Atom(0.5, 0.0, 0.5),
 *     new Atom(0.0, 0.5, 0.5)
 * ]
 * def box = new Box(1.0, 1.0, 1.0)
 * def data = new SettableAtomData(atoms, box)
 * } </pre>
 * 由此来创建一个 fcc 的晶胞
 * <p>
 * 这里的 {@link SettableAtomData} 创建会直接将输入的原子列表 {@code atoms}
 * 以及模拟盒 {@code box} 作为内部的成员，不会进行值拷贝。
 * 因此此对象内部属性都是引用，相当于一种对于原子列表
 * {@code List<Atom>} 的包装
 *
 * @author liqa
 * @see Atom Atom: 一般的原子实现
 * @see Box Box: 一般的正交的模拟盒实现
 * @see BoxPrism BoxPrism: 一般的三斜的模拟盒实现
 * @see ISettableAtom ISettableAtom: 通用的可以设置的原子数据接口
 * @see AbstractSettableAtomData AbstractSettableAtomData: 一般的可以设置的原子数据抽象类
 * @see AtomData AtomData: 一般的原子数据实现
 */
public final class SettableAtomData extends AbstractSettableAtomData {
    private final @Unmodifiable List<? extends ISettableAtom> mAtoms;
    private final IBox mBox;
    private int mAtomTypeNum;
    private final boolean mHasID;
    private final boolean mHasVelocity;
    private String @Nullable[] mSymbols;
    
    /**
     * 创建一个一般的可以设置的原子数据，内部直接存储输入的引用
     * <p>
     * 此方法目前不会对输入进行合理性检测，直接存储到内部
     * @param aAtoms 原子数据的原子列表，这里需要是可以设置的原子 {@link ISettableAtom}
     * @param aAtomTypeNum 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     * @param aSymbols 原子数据的元素符号信息
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasID, boolean aHasVelocity, String... aSymbols) {
        mAtoms = aAtoms;
        mBox = aBox;
        mAtomTypeNum = aAtomTypeNum;
        mHasID = aHasID;
        mHasVelocity = aHasVelocity;
        mSymbols = (aSymbols==null || aSymbols.length==0) ? null : aSymbols;
    }
    /**
     * 创建一个一般的可以设置的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表自动检测生成元素符号信息
     * @param aAtoms 原子数据的原子列表，这里需要是可以设置的原子 {@link ISettableAtom}
     * @param aAtomTypeNum 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasID, boolean aHasVelocity) {
        this(aAtoms, aAtomTypeNum, aBox, aHasID, aHasVelocity, (!aAtoms.isEmpty() && aAtoms.get(0).hasSymbol()) ? new String[aAtomTypeNum] : ZL_STR);
        if (mSymbols != null) for (IAtom tAtom : aAtoms) {
            int tTypeMM = Math.min(tAtom.type(), mAtomTypeNum) - 1;
            if (mSymbols[tTypeMM] == null) mSymbols[tTypeMM] = tAtom.symbol();
        }
    }
    /**
     * 创建一个一般的可以设置的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表自动检测种类数目，并且生成元素符号信息
     * @param aAtoms 原子数据的原子列表，这里需要是可以设置的原子 {@link ISettableAtom}
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, IBox aBox, boolean aHasID, boolean aHasVelocity) {
        mAtoms = aAtoms;
        mBox = aBox;
        mHasID = aHasID;
        mHasVelocity = aHasVelocity;
        int tAtomTypeNum = 1;
        for (IAtom tAtom : aAtoms) {
            tAtomTypeNum = Math.max(tAtom.type(), tAtomTypeNum);
        }
        mAtomTypeNum = tAtomTypeNum;
        mSymbols = (!aAtoms.isEmpty() && aAtoms.get(0).hasSymbol()) ? new String[mAtomTypeNum] : null;
        if (mSymbols != null) for (IAtom tAtom : aAtoms) {
            int tTypeMM = tAtom.type() - 1;
            if (mSymbols[tTypeMM] == null) mSymbols[tTypeMM] = tAtom.symbol();
        }
    }
    /**
     * 创建一个一般的可以设置的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表检测是否包含 id 和速度信息，并且生成元素符号信息
     * @param aAtoms 原子数据的原子列表，这里需要是可以设置的原子 {@link ISettableAtom}
     * @param aAtomTypeNum 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox) {this(aAtoms, aAtomTypeNum, aBox, !aAtoms.isEmpty() && aAtoms.get(0).hasID(), !aAtoms.isEmpty() && aAtoms.get(0).hasVelocity());}
    /**
     * 创建一个一般的可以设置的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表补全其他信息，包括种类数目，元素符号信息，以及是否包含 id 和速度信息
     * @param aAtoms 原子数据的原子列表，这里需要是可以设置的原子 {@link ISettableAtom}
     * @param aBox 原子数目的模拟盒
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, IBox aBox) {this(aAtoms, aBox, !aAtoms.isEmpty() && aAtoms.get(0).hasID(), !aAtoms.isEmpty() && aAtoms.get(0).hasVelocity());}
    
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see ISettableAtom
     */
    @Override public ISettableAtom atom(int aIdx) {
        // 需要包装一层，用于在更新种类时自动更新整体的种类计数
        final ISettableAtom tAtom = mAtoms.get(aIdx);
        return new AbstractSettableAtom_() {
            @Override public int index() {return aIdx;}
            @Override public double x() {return tAtom.x();}
            @Override public double y() {return tAtom.y();}
            @Override public double z() {return tAtom.z();}
            @Override protected int id_() {return tAtom.id();}
            @Override protected int type_() {return tAtom.type();}
            @Override protected double vx_() {return tAtom.vx();}
            @Override protected double vy_() {return tAtom.vy();}
            @Override protected double vz_() {return tAtom.vz();}
            
            @Override protected void setX_(double aX) {tAtom.setX(aX);}
            @Override protected void setY_(double aY) {tAtom.setY(aY);}
            @Override protected void setZ_(double aZ) {tAtom.setZ(aZ);}
            @Override protected void setID_(int aID) {tAtom.setID(aID);}
            @Override protected void setType_(int aType) {tAtom.setType(aType);}
            @Override protected void setVx_(double aVx) {tAtom.setVx(aVx);}
            @Override protected void setVy_(double aVy) {tAtom.setVy(aVy);}
            @Override protected void setVz_(double aVz) {tAtom.setVz(aVz);}
        };
    }
    /**
     * @return {@inheritDoc}
     * @see IBox
     */
    @Override public IBox box() {return mBox;}
    /** @return {@inheritDoc} */
    @Override public int atomNumber() {return mAtoms.size();}
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return mAtomTypeNum;}
    /**
     * {@inheritDoc}
     * @param aAtomTypeNum {@inheritDoc}
     * @return {@inheritDoc}
     * @see #atomTypeNumber()
     * @see IAtom#type()
     */
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
    
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasID()
     */
    @Override public boolean hasID() {return mHasID;}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasVelocity()
     */
    @Override public boolean hasVelocity() {return mHasVelocity;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasSymbol()
     * @see #setSymbols(String...)
     */
    @Override public SettableAtomData setNoSymbol() {return setSymbols(ZL_STR);}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasSymbol()
     */
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     * @see IAtom#type()
     * @see #hasSymbol()
     */
    @Override public @Nullable String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    /**
     * {@inheritDoc}
     * @param aSymbols {@inheritDoc}
     * @return {@inheritDoc}
     * @see #symbols()
     * @see IAtom#symbol()
     */
    @Override public SettableAtomData setSymbols(String... aSymbols) {
        if (aSymbols==null || aSymbols.length==0) {
            mSymbols = null;
            return this;
        }
        if (mSymbols==null || aSymbols.length>mSymbols.length) mSymbols = Arrays.copyOf(aSymbols, aSymbols.length);
        else System.arraycopy(aSymbols, 0, mSymbols, 0, aSymbols.length);
        return this;
    }
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasMass()
     */
    @Override public boolean hasMass() {return hasSymbol();}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#mass()
     * @see IAtom#type()
     * @see #hasMass()
     */
    @Override public double mass(int aType) {
        @Nullable String tSymbol = symbol(aType);
        return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
    }
}
