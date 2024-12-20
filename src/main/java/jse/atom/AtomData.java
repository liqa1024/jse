package jse.atom;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static jse.code.CS.MASS;
import static jse.code.CS.ZL_STR;


/**
 * 一般的原子数据实现，用于从头直接创建原子数据
 * <p>
 * 现在可以通过构造器 {@link #builder()} 来方便创建原子数据：
 * <pre> {@code
 * def data = AtomData.builder()
 *     .add(0.0, 0.0, 0.0)
 *     .add(0.5, 0.5, 0.0)
 *     .add(0.5, 0.0, 0.5)
 *     .add(0.0, 0.5, 0.5)
 *     .setBox(1.0, 1.0, 1.0)
 *     .build()
 * } </pre>
 * 来直接创建一个 fcc 晶胞
 *
 * @author liqa
 * @see IAtomData IAtomData: 通用的原子数据接口
 * @see SettableAtomData SettableAtomData: 一般的可以修改的原子数据实现
 * @see AbstractAtomData AbstractAtomData: 一般的原子数据抽象类
 */
public final class AtomData extends AbstractAtomData {
    /**
     * 创建一个原子数据 {@link AtomData} 的构造器
     * {@code AtomDataBuilder<AtomData>}，以此来快速构建原子数据：
     * <pre> {@code
     * def data = AtomData.builder()
     *     .add(0.0, 0.0, 0.0)
     *     .add(0.5, 0.5, 0.0)
     *     .add(0.5, 0.0, 0.5)
     *     .add(0.0, 0.5, 0.5)
     *     .setBox(1.0, 1.0, 1.0)
     *     .build()
     * } </pre>
     * 即可创建一个 fcc 晶胞
     *
     * @return {@link AtomData} 的构造器
     * @see AtomDataBuilder
     */
    public static AtomDataBuilder<AtomData> builder() {
        return new AtomDataBuilder<AtomData>() {
            @Override AtomData newAtomData(List<ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasID, boolean aHasVelocity, String[] aSymbols) {
                return new AtomData(aAtoms, aAtomTypeNum, aBox, aHasID, aHasVelocity, aSymbols);
            }
        };
    }
    
    private final @Unmodifiable List<? extends IAtom> mAtoms;
    private final IBox mBox;
    private final int mAtomTypeNum;
    private final boolean mHasID;
    private final boolean mHasVelocity;
    private final String @Nullable[] mSymbols;
    
    /**
     * 创建一个一般的原子数据，内部直接存储输入的引用
     * <p>
     * 此方法目前不会对输入进行合理性检测，直接存储到内部
     * @param aAtoms 原子数据的原子列表
     * @param aAtomTypeNum 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     * @param aSymbols 原子数据的元素符号信息
     */
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasID, boolean aHasVelocity, String... aSymbols) {
        mAtoms = aAtoms;
        mBox = aBox;
        mAtomTypeNum = aAtomTypeNum;
        mHasID = aHasID;
        mHasVelocity = aHasVelocity;
        mSymbols = (aSymbols==null || aSymbols.length==0) ? null : aSymbols;
    }
    /**
     * 创建一个一般的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表自动检测生成元素符号信息
     * @param aAtoms 原子数据的原子列表
     * @param aAtomTypeNum 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     */
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasID, boolean aHasVelocity) {
        this(aAtoms, aAtomTypeNum, aBox, aHasID, aHasVelocity, (!aAtoms.isEmpty() && aAtoms.get(0).hasSymbol()) ? new String[aAtomTypeNum] : ZL_STR);
        if (mSymbols != null) for (IAtom tAtom : aAtoms) {
            int tTypeMM = Math.min(tAtom.type(), mAtomTypeNum) - 1;
            if (mSymbols[tTypeMM] == null) mSymbols[tTypeMM] = tAtom.symbol();
        }
    }
    /**
     * 创建一个一般的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表自动检测种类数目，并且生成元素符号信息
     * @param aAtoms 原子数据的原子列表
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     */
    public AtomData(List<? extends IAtom> aAtoms, IBox aBox, boolean aHasID, boolean aHasVelocity) {
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
     * 创建一个一般的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表检测是否包含 id 和速度信息，并且生成元素符号信息
     * @param aAtoms 原子数据的原子列表
     * @param aAtomTypeNum 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     */
    public AtomData(List<? extends IAtom> aAtoms, int aAtomTypeNum, IBox aBox) {this(aAtoms, aAtomTypeNum, aBox, !aAtoms.isEmpty() && aAtoms.get(0).hasID(), !aAtoms.isEmpty() && aAtoms.get(0).hasVelocity());}
    /**
     * 创建一个一般的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表补全其他信息，包括种类数目，元素符号信息，以及是否包含 id 和速度信息
     * @param aAtoms 原子数据的原子列表
     * @param aBox 原子数目的模拟盒
     */
    public AtomData(List<? extends IAtom> aAtoms, IBox aBox) {this(aAtoms, aBox, !aAtoms.isEmpty() && aAtoms.get(0).hasID(), !aAtoms.isEmpty() && aAtoms.get(0).hasVelocity());}
    
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom
     */
    @Override public IAtom atom(int aIdx) {
        // 需要包装一层，用于自动复写内部原子的 index 信息
        final IAtom tAtom = mAtoms.get(aIdx);
        return new AbstractAtom_() {
            @Override public int index() {return aIdx;}
            @Override public double x() {return tAtom.x();}
            @Override public double y() {return tAtom.y();}
            @Override public double z() {return tAtom.z();}
            @Override protected int id_() {return tAtom.id();}
            @Override protected int type_() {return tAtom.type();}
            @Override protected double vx_() {return tAtom.vx();}
            @Override protected double vy_() {return tAtom.vy();}
            @Override protected double vz_() {return tAtom.vz();}
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
