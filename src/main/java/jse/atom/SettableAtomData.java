package jse.atom;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jse.code.CS.MASS;
import static jse.code.CS.ZL_STR;


/**
 * 一般的可以设置的原子数据实现，用于从头直接创建原子数据
 * <p>
 * 现在可以通过构造器 {@link #builder()} 来方便创建原子数据：
 * <pre> {@code
 * def data = SettableAtomData.builder()
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
 * @see ISettableAtom ISettableAtom: 通用的可以设置的原子数据接口
 * @see AbstractSettableAtomData AbstractSettableAtomData: 一般的可以设置的原子数据抽象类
 * @see AtomData AtomData: 一般的原子数据实现
 */
public final class SettableAtomData extends AbstractSettableAtomData {
    /**
     * 创建一个可以修改的原子数据 {@link SettableAtomData} 的构造器
     * {@code AtomDataBuilder<SettableAtomData>}，以此来快速构建原子数据：
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
     * @return {@link SettableAtomData} 的构造器
     * @see AtomDataBuilder
     */
    public static AtomDataBuilder<SettableAtomData> builder() {
        return new AtomDataBuilder<SettableAtomData>() {
            @Override SettableAtomData newAtomData(List<ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasID, boolean aHasVelocity, String[] aSymbols) {
                return new SettableAtomData(aAtoms, aAtomTypeNum, aBox, aHasID, aHasVelocity, aSymbols);
            }
        };
    }
    
    private final @Unmodifiable List<? extends ISettableAtom> mAtoms;
    private IBox mBox;
    private int mNumTypes;
    private final boolean mHasID;
    private boolean mHasVelocity;
    private String @Nullable[] mSymbols;
    
    /**
     * 创建一个一般的可以设置的原子数据，内部直接存储输入的引用
     * <p>
     * 此方法目前不会对输入进行合理性检测，直接存储到内部
     * @param aAtoms 原子数据的原子列表，这里需要是可以设置的原子 {@link ISettableAtom}
     * @param aNumTypes 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     * @param aSymbols 原子数据的元素符号信息
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aNumTypes, IBox aBox, boolean aHasID, boolean aHasVelocity, String... aSymbols) {
        mAtoms = aAtoms;
        mBox = aBox;
        mNumTypes = aNumTypes;
        mHasID = aHasID;
        mHasVelocity = aHasVelocity;
        mSymbols = (aSymbols==null || aSymbols.length==0) ? null : aSymbols;
    }
    /**
     * 创建一个一般的可以设置的原子数据，内部直接存储输入的引用
     * <p>
     * 现在会自动通过输入的原子列表自动检测生成元素符号信息
     * @param aAtoms 原子数据的原子列表，这里需要是可以设置的原子 {@link ISettableAtom}
     * @param aNumTypes 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     * @param aHasID 原子数据是否包含 id 信息
     * @param aHasVelocity 原子数据是否包含速度信息
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aNumTypes, IBox aBox, boolean aHasID, boolean aHasVelocity) {
        this(aAtoms, aNumTypes, aBox, aHasID, aHasVelocity, (!aAtoms.isEmpty() && aAtoms.get(0).hasSymbol()) ? new String[aNumTypes] : ZL_STR);
        if (mSymbols != null) for (IAtom tAtom : aAtoms) {
            int tTypeMM = Math.min(tAtom.type(), mNumTypes) - 1;
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
        mNumTypes = tAtomTypeNum;
        mSymbols = (!aAtoms.isEmpty() && aAtoms.get(0).hasSymbol()) ? new String[mNumTypes] : null;
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
     * @param aNumTypes 需要的原子种类数目，如果实际原子种类编号大于此值会被截断
     * @param aBox 原子数目的模拟盒
     */
    public SettableAtomData(List<? extends ISettableAtom> aAtoms, int aNumTypes, IBox aBox) {this(aAtoms, aNumTypes, aBox, !aAtoms.isEmpty() && aAtoms.get(0).hasID(), !aAtoms.isEmpty() && aAtoms.get(0).hasVelocity());}
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
    @Override public int natoms() {return mAtoms.size();}
    /** @return {@inheritDoc} */
    @Override public int ntypes() {return mNumTypes;}
    /**
     * {@inheritDoc}
     * @param aNumTypes {@inheritDoc}
     * @return {@inheritDoc}
     * @see #ntypes()
     * @see IAtom#type()
     */
    @Override public SettableAtomData setNtypes(int aNumTypes) {
        int oTypeNum = mNumTypes;
        if (aNumTypes == oTypeNum) return this;
        mNumTypes = aNumTypes;
        if (aNumTypes < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            for (ISettableAtom tAtom : mAtoms) if (tAtom.type() > aNumTypes){
                tAtom.setType(aNumTypes);
            }
            return this;
        }
        if (mSymbols!=null && mSymbols.length<aNumTypes) {
            String[] rSymbols = new String[aNumTypes];
            System.arraycopy(mSymbols, 0, rSymbols, 0, mSymbols.length);
            for (int tType = mSymbols.length+1; tType <= aNumTypes; ++tType) rSymbols[tType-1] = "T" + tType;
            mSymbols = rSymbols;
        }
        return this;
    }
    
    @Override protected void setBox_(double aX, double aY, double aZ) {
        mBox = new Box(aX, aY, aZ);
    }
    @Override protected void setBox_(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        mBox = new BoxPrism(aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);
    }
    
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasID()
     */
    @Override public boolean hasID() {return mHasID;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasVelocity()
     * @see #setHasVelocity()
     */
    @Override public SettableAtomData setNoVelocity() {mHasVelocity = false; return this;}
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
     * @param aSymbolOrder {@inheritDoc}
     * @return {@inheritDoc}
     * @see #setSymbols(String...)
     */
    @Override public SettableAtomData setSymbolOrder(String... aSymbolOrder) {
        if (mSymbols == null) throw new UnsupportedOperationException("`setSymbolOrder` for AtomData without symbols");
        if (aSymbolOrder==null || aSymbolOrder.length==0) return this;
        // 构造一个 tSymbol2Type
        final Map<String, Integer> tSymbol2Type = new HashMap<>();
        for (String tSymbol : aSymbolOrder) {
            // 注意这里需要考虑可能存在相同符号的情况
            if (!tSymbol2Type.containsKey(tSymbol)) {
                int tType = tSymbol2Type.size() + 1;
                tSymbol2Type.put(tSymbol, tType);
            }
        }
        // 遍历一次 mSymbols 确保 tSymbol2Type 全部覆盖
        for (int typeMM = 0; typeMM < mNumTypes; ++typeMM) {
            String tSymbol = mSymbols[typeMM];
            if (!tSymbol2Type.containsKey(tSymbol)) {
                int tType = tSymbol2Type.size() + 1;
                tSymbol2Type.put(tSymbol, tType);
            }
        }
        // 映射调整原子 types
        for (ISettableAtom tAtom : mAtoms) {
            tAtom.setType(tSymbol2Type.get(mSymbols[tAtom.type()-1]));
        }
        // 构建 mSymbols
        if (tSymbol2Type.size()>mSymbols.length) mSymbols = new String[tSymbol2Type.size()];
        for (Map.Entry<String, Integer> tEntry : tSymbol2Type.entrySet()) {
            mSymbols[tEntry.getValue()-1] = tEntry.getKey();
        }
        return this;
    }
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
