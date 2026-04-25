package jse.atom.data;

import jse.ase.AseAtoms;
import jse.atom.*;
import jse.code.Conf;
import jse.code.FileEndException;
import jse.code.IO;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.*;
import jse.math.vector.*;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static jse.code.CS.*;

/**
 * <a href="https://en.wikipedia.org/wiki/XYZ_file_format">
 * XYZ 原子数据格式 </a> 支持，由于名称上和现有的代表坐标点的
 * {@link jse.atom.XYZ} 一致，因此这里统称 {@code DataXYZ}
 * <p>
 * 这里主要对
 * <a href="https://docs.ovito.org/reference/file_formats/input/xyz.html#file-formats-input-xyz-extended-format">
 * 扩展的 XYZ 格式 </a> 提供支持，一般的 XYZ 格式同样支持读取，而写入时会根据需要自动转为扩展的格式
 * <p>
 * 对于扩展的 XYZ 格式，通过：
 * <pre> {@code
 * def value = dataXYZ.parameter('param_key')
 * dataXYZ.setParameter('param_key', value)
 * } </pre>
 * 来读写对应的参量（整个原子数据公用的值），可以是
 * {@code double, int, boolean, String}；
 * 通过：
 * <pre> {@code
 * def values = dataXYZ.property('prop_key')
 * dataXYZ.setProperty('prop_key', values)
 * } </pre>
 * 来读写对应的属性（每个原子独立的值），可以是
 * {@code IVector, IIntVector, IMatrix, IIntMatrix, String[],
 * String[][]}，按照原子索引按行排列
 * <p>
 * 对于原始的 XYZ 格式，由于不存在模拟盒信息，会自动通过原子坐标自动生成最小的模拟盒
 *
 * @see IAtomData IAtomData: 原子数据类型通用接口
 * @see DumpXYZ DumpXYZ: 多帧的 XYZ 格式
 * @see #read(String) read(String): 读取指定路径的 XYZ 格式的原子数据
 * @see #write(String) write(String): 将此 XYZ 原子数据写入指定路径
 * @see #of(IAtomData) of(IAtomData): 将任意的原子数据转换成 XYZ 类型
 * @author liqa
 */
public class DataXYZ extends AbstractSettableAtomData {
    private final int mNumAtoms;
    private @Nullable String mComment;
    private final @NotNull Map<String, Object> mParameters;
    private final @NotNull Map<String, Object> mProperties;
    
    private @Nullable IBox mBox;
    private String @Nullable[] mSpecies = null;
    private @Nullable IMatrix mPositions = null;
    private @Nullable IMatrix mVelocities  = null;
    
    /** 用于通过原子序数获取每个种类的粒子数 */
    private final @NotNull Map<String, Integer> mSymbol2Type;
    private String @Nullable[] mType2Symbol; // 第 0 个直接制空
    
    DataXYZ(int aNumAtoms, @Nullable String aComment, @NotNull Map<String, Object> aParameters, @NotNull Map<String, Object> aProperties, @Nullable IBox aBox) {
        mNumAtoms = aNumAtoms;
        mComment = aComment;
        mParameters = aParameters;
        mProperties = aProperties;
        Object tSymbols = mProperties.get("species");
        if (tSymbols instanceof String[]) mSpecies = (String[])tSymbols;
        Object tPositions = mProperties.get("pos");
        if ((tPositions instanceof IMatrix) && ((IMatrix)tPositions).ncols()==3) mPositions = (IMatrix)tPositions;
        Object tVelocities = mProperties.get("velo");
        if ((tVelocities instanceof IMatrix) && ((IMatrix)tVelocities).ncols()==3) {
            mVelocities = (IMatrix)tVelocities;
        } else {
            tVelocities = mProperties.get("vel");
            if ((tVelocities instanceof IMatrix) && ((IMatrix)tVelocities).ncols()==3) mVelocities = (IMatrix)tVelocities;
        }
        if (aBox==null && mPositions!=null) {
            double tMaxX = mPositions.col(0).max(); double tMinX = Math.min(0.0, mPositions.col(0).min());
            double tMaxY = mPositions.col(1).max(); double tMinY = Math.min(0.0, mPositions.col(1).min());
            double tMaxZ = mPositions.col(2).max(); double tMinZ = Math.min(0.0, mPositions.col(2).min());
            aBox = new Box(tMaxX-tMinX, tMaxY-tMinY, tMaxZ-tMinZ);
        }
        mBox = aBox;
        
        mSymbol2Type = new HashMap<>();
        if (mSpecies == null) {
            mType2Symbol = null;
        } else {
            for (String tSymbol : mSpecies) {
                if (!mSymbol2Type.containsKey(tSymbol)) {
                    int tType = mSymbol2Type.size() + 1;
                    mSymbol2Type.put(tSymbol, tType);
                }
            }
            mType2Symbol = new String[mSymbol2Type.size()+1];
            for (Map.Entry<String, Integer> tEntry : mSymbol2Type.entrySet()) {
                mType2Symbol[tEntry.getValue()] = tEntry.getKey();
            }
        }
    }
    
    public static boolean isInvalidKey(String aKey) {
        return aKey.contains("\"") || IO.Text.findBlankIndex(aKey, 0)>=0;
    }
    public static boolean isInvalidPropertyKey(String aKey) {
        return aKey.contains(":") || isInvalidKey(aKey);
    }
    
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasVelocity()
     */
    @Override public boolean hasVelocity() {return mVelocities!=null;}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasSymbol()
     */
    @Override public boolean hasSymbol() {return mSpecies!=null;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     * @see IAtom#type()
     * @see #hasSymbol()
     */
    @Override public String symbol(int aType) {
        if (mType2Symbol == null) return null;
        return mType2Symbol[aType];
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#hasMass()
     */
    @Override public boolean hasMass() {return hasSymbol();}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IVector
     * @see IAtom#mass()
     * @see #hasMass()
     */
    @Override public double mass(int aType) {
        @Nullable String tSymbol = symbol(aType);
        return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
    }
    
    /// XYZ 特有的接口
    /**
     * @return 此 XYZ 数据的注释值，如果没有或者是扩展的 XYZ 格式则返回 {@code null}
     * @see #setComment(String)
     */
    public @Nullable String comment() {return mComment;}
    /** @see #comment() */
    @VisibleForTesting public @Nullable String getComment() {return mComment;}
    /**
     * 设置此 XYZ 数据的注释值，只能适用于原始且带有注释的 XYZ
     * 格式，因为没有注释值会自动视为扩展的 XYZ 格式
     * @param aComment 需要设置的注释值
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 当不存在注释值或者是扩展的 XYZ 格式时
     * @see #comment()
     */
    public DataXYZ setComment(String aComment) {
        if (mComment == null) throw new UnsupportedOperationException("`setComment` for DataXYZ without comment");
        mComment = aComment;
        return this;
    }
    private void validComment_() {
        if (mComment != null) {
            // 如果 comment 没有影响解析的字符则直接保留 comment
            if (!mComment.contains("\"")) mParameters.put("Comment", mComment);
            mComment = null;
        }
    }
    /**
     * 判断是否有某个参量（整个原子数据公用的值）
     * @param aKey 此参量的名称，区分大小写
     * @return 是否存在这个参量值
     * @see #parameter(String)
     * @see #setParameter(String, Object)
     * @see #removeParameter(String)
     */
    public boolean hasParameter(String aKey) {return mParameters.containsKey(aKey);}
    /**
     * 直接获取所有的参量组成的 {@link Map}，方便直接遍历，原则上不允许修改
     * @return 参量名称和具体参量值组成的 {@link Map}
     * @see #parameter(String)
     * @see #setParameter(String, Object)
     * @see #removeParameter(String)
     */
    public @Unmodifiable Map<String,Object> parameters() {return mParameters;}
    /**
     * 获取某个参量（整个原子数据公用的值）
     * @param aKey 需要的参量的名称，区分大小写
     * @return 具体参量值
     * @see #hasParameter(String)
     * @see #setParameter(String, Object)
     * @see #removeParameter(String)
     */
    public Object parameter(String aKey) {return mParameters.get(aKey);}
    /**
     * 设置某个参量（整个原子数据公用的值），不支持设置扩展
     * XYZ 格式中已经预定的参量名称 {@code Lattice} 和
     * {@code Properties}；如果是包含注释的原始 XYZ
     * 格式，则会尝试将注释设置成 {@code Comment} 参量并清除注释值。
     *
     * @param aKey 需要设置的参量的名称，区分大小写
     * @param aValue 需要设置的参量值
     * @return 自身方便链式调用
     * @see #hasParameter(String)
     * @see #parameter(String)
     * @see #removeParameter(String)
     */
    public DataXYZ setParameter(String aKey, Object aValue) {
        if (aKey.equals("Lattice")) throw new IllegalArgumentException("Lattice for DataXYZ parameter");
        if (aKey.equals("Properties")) throw new IllegalArgumentException("Properties for DataXYZ parameter");
        if (isInvalidKey(aKey)) throw new IllegalArgumentException("Invalid key '"+aKey+"'");
        validComment_();
        if (aValue == null) {
            mParameters.remove(aKey);
            return this;
        }
        mParameters.put(aKey, aValue);
        return this;
    }
    /**
     * 移除某个参量（整个原子数据公用的值）
     * @param aKey 需要移除的参量的名称，区分大小写
     * @return 移除的参量的值，如果没有则会返回 {@code null}
     * @see #hasParameter(String)
     * @see #parameter(String)
     * @see #setParameter(String, Object)
     * @see Map#remove(Object)
     */
    public Object removeParameter(String aKey) {
        if (aKey.equals("Lattice")) throw new IllegalArgumentException("Lattice for DataXYZ parameter");
        if (aKey.equals("Properties")) throw new IllegalArgumentException("Properties for DataXYZ parameter");
        return mParameters.remove(aKey);
    }
    /**
     * 判断是否有某个属性（每个原子独立的值）
     * @param aKey 此属性的名称，区分大小写
     * @return 是否存在这个属性值
     * @see #property(String)
     * @see #setProperty(String, Object)
     * @see #removeProperty(String)
     */
    public boolean hasProperty(String aKey) {return mProperties.containsKey(aKey);}
    /**
     * 直接获取所有的属性组成的 {@link Map}，方便直接遍历，原则上不允许修改
     * @return 属性名称和具体属性值组成的 {@link Map}
     * @see #property(String)
     * @see #setProperty(String, Object)
     * @see #removeProperty(String)
     */
    public @Unmodifiable Map<String,Object> properties() {return mProperties;}
    /**
     * 获取某个属性（每个原子独立的值），一般来说类型只能是
     * {@code IVector, IIntVector, IMatrix, IIntMatrix, String[],
     * String[][]}，按照原子索引按行排列
     *
     * @param aKey 需要的属性的名称，区分大小写
     * @return 具体属性值
     * @see #hasProperty(String)
     * @see #setProperty(String, Object)
     * @see #removeProperty(String)
     */
    public Object property(String aKey) {return mProperties.get(aKey);}
    /**
     * 设置某个属性（每个原子独立的值），一般来说类型只能是
     * {@code IVector, IIntVector, IMatrix, IIntMatrix, String[],
     * String[][]}，按照原子索引按行排列
     * <p>
     * 对于扩展 XYZ 格式中特定的参量名称 {@code pos, species, velo, vel}
     * 严格限制维数和类型；如果是包含注释的原始 XYZ 格式，则会尝试将注释设置成
     * {@code Comment} 参量并清除注释值。
     * <p>
     * 此设置不会进行值拷贝
     *
     * @param aKey 需要设置的属性的名称，区分大小写
     * @param aValue 需要设置的属性值
     * @return 自身方便链式调用
     * @see #hasProperty(String)
     * @see #property(String)
     * @see #removeProperty(String)
     */
    public DataXYZ setProperty(String aKey, Object aValue) {
        if (isInvalidPropertyKey(aKey)) throw new IllegalArgumentException("Invalid key '"+aKey+"'");
        validComment_();
        if (aValue == null) {
            removeProperty(aKey);
            return this;
        }
        // 这里简单处理，只对特定的参数做类型检测；总之后面遇到不识别的就不输出
        switch(aKey) {
        case "pos": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for pos, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            if (tValue.ncols() != ATOM_DATA_KEYS_XYZ.length) throw new IllegalArgumentException("Data ncols mismatch, need: "+ATOM_DATA_KEYS_XYZ.length+", input: "+tValue.ncols());
            mPositions = tValue;
            mProperties.put(aKey, tValue);
            return this;
        }
        case "species": {
            if (!(aValue instanceof String[])) throw new IllegalArgumentException("Value type MUST be String[] for species, input: "+aValue.getClass().getName());
            String[] tValue = (String[])aValue;
            if (tValue.length != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.length);
            mSpecies = tValue;
            // 这里直接重置原有的 symbol order
            mSymbol2Type.clear();
            for (String tSymbol : mSpecies) {
                if (!mSymbol2Type.containsKey(tSymbol)) {
                    int tType = mSymbol2Type.size() + 1;
                    mSymbol2Type.put(tSymbol, tType);
                }
            }
            mType2Symbol = new String[mSymbol2Type.size()+1];
            for (Map.Entry<String, Integer> tEntry : mSymbol2Type.entrySet()) {
                mType2Symbol[tEntry.getValue()] = tEntry.getKey();
            }
            mProperties.put(aKey, tValue);
            return this;
        }
        case "velo": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for velo, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            if (tValue.ncols() != ATOM_DATA_KEYS_VELOCITY.length) throw new IllegalArgumentException("Data ncols mismatch, need: "+ATOM_DATA_KEYS_VELOCITY.length+", input: "+tValue.ncols());
            mVelocities = tValue;
            mProperties.put(aKey, tValue);
            return this;
        }
        case "vel": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for vel, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            if (tValue.ncols() != ATOM_DATA_KEYS_VELOCITY.length) throw new IllegalArgumentException("Data ncols mismatch, need: "+ATOM_DATA_KEYS_VELOCITY.length+", input: "+tValue.ncols());
            Object tVelo = mProperties.get("velo");
            if (!(tVelo instanceof IMatrix) || ((IMatrix) tVelo).ncols()!=3) {
                mVelocities = tValue;
            }
            mProperties.put(aKey, tValue);
            return this;
        }}
        if (aValue instanceof IVector) {
            IVector tValue = (IVector)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mProperties.put(aKey, tValue);
        } else
        if (aValue instanceof IIntVector) {
            IIntVector tValue = (IIntVector)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mProperties.put(aKey, tValue);
        } else
        if (aValue instanceof ILogicalVector) {
            ILogicalVector tValue = (ILogicalVector)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mProperties.put(aKey, tValue);
        } else
        if (aValue instanceof IMatrix) {
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            mProperties.put(aKey, tValue);
        } else
        if (aValue instanceof IIntMatrix) {
            IIntMatrix tValue = (IIntMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            mProperties.put(aKey, tValue);
        } else
        if (aValue instanceof ILogicalMatrix) {
            ILogicalMatrix tValue = (ILogicalMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            mProperties.put(aKey, tValue);
        } else
        if (aValue instanceof String[]) {
            String[] tValue = (String[])aValue;
            if (tValue.length != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.length);
            mProperties.put(aKey, tValue);
        } else
        if (aValue instanceof String[][]) {
            String[][] tValue = (String[][])aValue;
            if (tValue.length != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.length);
            mProperties.put(aKey, tValue);
        } else {
            throw new IllegalArgumentException("Invalid ext-xyz property type, input: "+aValue.getClass().getName());
        }
        return this;
    }
    /**
     * 移除某个属性（每个原子独立的值）
     * @param aKey 需要移除的属性的名称，区分大小写
     * @return 移除的属性的值，如果没有则会返回 {@code null}
     * @see #hasProperty(String)
     * @see #property(String)
     * @see #setProperty(String, Object)
     * @see Map#remove(Object)
     */
    @SuppressWarnings("UnusedReturnValue")
    public Object removeProperty(String aKey) {
        switch(aKey) {
        case "pos": {
            mPositions = null;
            return mProperties.remove(aKey);
        }
        case "species": {
            mSpecies = null;
            mSymbol2Type.clear();
            mType2Symbol = null;
            return mProperties.remove(aKey);
        }
        case "velo": {
            Object tVel = mProperties.get("vel");
            if (tVel instanceof IMatrix && ((IMatrix)tVel).ncols()==3) {
                mVelocities = (IMatrix)tVel;
            } else {
                mVelocities = null;
            }
            return mProperties.remove(aKey);
        }
        case "vel": {
            Object tVelo = mProperties.get("velo");
            if (!(tVelo instanceof IMatrix) || ((IMatrix) tVelo).ncols()!=3) {
                mVelocities = null;
            }
            return mProperties.remove(aKey);
        }}
        return mProperties.remove(aKey);
    }
    
    /**
     * {@inheritDoc}
     * @param aSymbolOrder {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException 如果不包含元素符号信息
     * @see #setSymbols(String...)
     */
    @Override public DataXYZ setSymbolOrder(String... aSymbolOrder) {
        if (mSpecies == null) throw new UnsupportedOperationException("`setSymbolOrder` for DataXYZ without species data");
        assert mType2Symbol != null;
        if (aSymbolOrder==null || aSymbolOrder.length==0) return this;
        // 先更新 mSymbol2Type
        mSymbol2Type.clear();
        for (String tSymbol : aSymbolOrder) {
            // 注意这里需要考虑可能存在相同符号的情况
            if (!mSymbol2Type.containsKey(tSymbol)) {
                int tType = mSymbol2Type.size() + 1;
                mSymbol2Type.put(tSymbol, tType);
            }
        }
        // 遍历一次 mType2Symbol 确保 mSymbol2Type 全部覆盖
        for (int type = 1; type < mType2Symbol.length; ++type) {
            String tSymbol = mType2Symbol[type];
            if (!mSymbol2Type.containsKey(tSymbol)) {
                int tType = mSymbol2Type.size() + 1;
                mSymbol2Type.put(tSymbol, tType);
            }
        }
        // 总是重新构建 mType2Symbol
        if (mSymbol2Type.size() != mType2Symbol.length-1) {
            mType2Symbol = new String[mSymbol2Type.size()+1];
        }
        for (Map.Entry<String, Integer> tEntry : mSymbol2Type.entrySet()) {
            mType2Symbol[tEntry.getValue()] = tEntry.getKey();
        }
        return this;
    }
    
    /**
     * {@inheritDoc}
     * @param aSymbols {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException 如果不包含元素符号信息
     * @see #symbols()
     * @see IAtom#symbol()
     * @see #setSymbolOrder(String...)
     */
    @Override public DataXYZ setSymbols(String... aSymbols) {
        if (mSpecies == null) throw new UnsupportedOperationException("`setSymbols` for DataXYZ without species data");
        assert mType2Symbol != null;
        if (aSymbols==null || aSymbols.length==0) return this;
        // 先更新 mSymbols，此时需要旧的 mSymbol2Type
        for (int i = 0; i < mSpecies.length; ++i) {
            int tTypeMM = mSymbol2Type.get(mSpecies[i]) - 1;
            if (tTypeMM < aSymbols.length) {
                mSpecies[i] = aSymbols[tTypeMM];
            }
        }
        // 更新 mSymbol2Type
        mSymbol2Type.clear();
        for (String tSymbol : aSymbols) {
            // 注意这里需要考虑可能存在相同符号的情况
            if (!mSymbol2Type.containsKey(tSymbol)) {
                int tType = mSymbol2Type.size() + 1;
                mSymbol2Type.put(tSymbol, tType);
            }
        }
        // 遍历剩余 mType2Symbol 确保 mSymbol2Type 全部覆盖
        for (int type = aSymbols.length+1; type < mType2Symbol.length; ++type) {
            String tSymbol = mType2Symbol[type];
            if (!mSymbol2Type.containsKey(tSymbol)) {
                int tType = mSymbol2Type.size() + 1;
                mSymbol2Type.put(tSymbol, tType);
            }
        }
        // 总是重新构建 mType2Symbol
        if (mSymbol2Type.size() != mType2Symbol.length-1) {
            mType2Symbol = new String[mSymbol2Type.size()+1];
        }
        for (Map.Entry<String, Integer> tEntry : mSymbol2Type.entrySet()) {
            mType2Symbol[tEntry.getValue()] = tEntry.getKey();
        }
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aNumTypes {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException 如果不包含元素符号信息
     * @see #ntypes()
     * @see IAtom#type()
     */
    @Override public DataXYZ setNtypes(int aNumTypes) {
        if (mSpecies == null) throw new UnsupportedOperationException("`setNtypes` for DataXYZ without species data");
        assert mType2Symbol != null;
        int oTypeNum = ntypes();
        if (aNumTypes == oTypeNum) return this;
        if (aNumTypes < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            for (int i = 0; i < mSpecies.length; ++i) {
                int tType = mSymbol2Type.get(mSpecies[i]);
                if (tType > aNumTypes) {
                    mSpecies[i] = mType2Symbol[aNumTypes];
                }
            }
            String[] oType2Symbol = mType2Symbol;
            mType2Symbol = new String[aNumTypes+1];
            System.arraycopy(oType2Symbol, 0, mType2Symbol, 0, mType2Symbol.length);
            validSymbol2Type_();
            return this;
        }
        String[] oType2Symbol = mType2Symbol;
        mType2Symbol = new String[aNumTypes+1];
        System.arraycopy(oType2Symbol, 0, mType2Symbol, 0, oType2Symbol.length);
        for (int tType = oType2Symbol.length; tType <= aNumTypes; ++tType) {
            mType2Symbol[tType] = "T"+tType;
        }
        validSymbol2Type_();
        return this;
    }
    private void validSymbol2Type_() {
        assert mType2Symbol != null;
        mSymbol2Type.clear();
        for (int type = 1; type < mType2Symbol.length; ++type) {
            mSymbol2Type.put(mType2Symbol[type], type);
        }
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasVelocity()
     * @see #setHasVelocity()
     */
    @Override public DataXYZ setNoVelocity() {
        mVelocities = null;
        mProperties.remove("velo");
        mProperties.remove("vel");
        return this;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasVelocity()
     * @see #setNoVelocity()
     */
    @Override public DataXYZ setHasVelocity() {
        validComment_();
        if (mVelocities == null) {
            mVelocities = RowMatrix.zeros(natoms(), ATOM_DATA_KEYS_VELOCITY.length);
            mProperties.put("velo", mVelocities);
        }
        return this;
    }
    
    /// set box stuff
    @Override protected void setBox_(double aX, double aY, double aZ) {
        validComment_();
        mBox = new Box(aX, aY, aZ);
    }
    @Override protected void setBox_(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        validComment_();
        mBox = new BoxPrism(aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);
    }
    @Override protected void scaleAtomPosition_(boolean aKeepAtomPosition, double aScale) {
        if (aKeepAtomPosition) return;
        if (mPositions != null) {
            mPositions.multiply2this(aScale);
        }
        if (mVelocities != null) {
            mVelocities.multiply2this(aScale);
        }
    }
    @Override protected void validAtomPosition_(boolean aKeepAtomPosition, IBox aOldBox) {
        if (aKeepAtomPosition) return;
        final int tAtomNum = natoms();
        XYZ tBuf = new XYZ();
        assert mBox != null;
        if (mBox.isPrism() || aOldBox.isPrism()) {
            for (int i = 0; i < tAtomNum; ++i) {
                if (mPositions != null) {
                    tBuf.setXYZ(mPositions.get(i, XYZ_X_COL), mPositions.get(i, XYZ_Y_COL), mPositions.get(i, XYZ_Z_COL));
                    // 这样转换两次即可实现线性变换
                    aOldBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    mPositions.set(i, XYZ_X_COL, tBuf.mX);
                    mPositions.set(i, XYZ_Y_COL, tBuf.mY);
                    mPositions.set(i, XYZ_Z_COL, tBuf.mZ);
                }
                // 如果存在速度，则速度也需要做一次这样的变换
                if (mVelocities != null) {
                    tBuf.setXYZ(mVelocities.get(i, STD_VX_COL), mVelocities.get(i, STD_VY_COL), mVelocities.get(i, STD_VZ_COL));
                    aOldBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    mVelocities.set(i, STD_VX_COL, tBuf.mX);
                    mVelocities.set(i, STD_VY_COL, tBuf.mY);
                    mVelocities.set(i, STD_VZ_COL, tBuf.mZ);
                }
            }
        } else {
            tBuf.setXYZ(mBox);
            tBuf.div2this(aOldBox);
            if (mPositions != null) {
                mPositions.col(XYZ_X_COL).multiply2this(tBuf.mX);
                mPositions.col(XYZ_Y_COL).multiply2this(tBuf.mY);
                mPositions.col(XYZ_Z_COL).multiply2this(tBuf.mZ);
            }
            // 如果存在速度，则速度也需要做一次这样的变换
            if (mVelocities != null) {
                mVelocities.col(STD_VX_COL).multiply2this(tBuf.mX);
                mVelocities.col(STD_VY_COL).multiply2this(tBuf.mY);
                mVelocities.col(STD_VZ_COL).multiply2this(tBuf.mZ);
            }
        }
    }
    
    /// AbstractAtomData stuffs
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see ISettableAtom
     * @see #atom(int)
     * @see #setAtom(int, IAtom)
     */
    @Override public ISettableAtom atom(final int aIdx) {
        return new AbstractSettableAtom_() {
            @Override public int index() {return aIdx;}
            @Override public double x() {
                if (mPositions == null) throw new UnsupportedOperationException("`x` for DataXYZ without pos data");
                return mPositions.get(aIdx, XYZ_X_COL);
            }
            @Override public double y() {
                if (mPositions == null) throw new UnsupportedOperationException("`y` for DataXYZ without pos data");
                return mPositions.get(aIdx, XYZ_Y_COL);
            }
            @Override public double z() {
                if (mPositions == null) throw new UnsupportedOperationException("`z` for DataXYZ without pos data");
                return mPositions.get(aIdx, XYZ_Z_COL);
            }
            @Override protected int type_() {
                if (mSpecies == null) return 1;
                return mSymbol2Type.get(mSpecies[aIdx]);
            }
            @Override protected double vx_() {assert mVelocities!=null; return mVelocities.get(aIdx, STD_VX_COL);}
            @Override protected double vy_() {assert mVelocities!=null; return mVelocities.get(aIdx, STD_VY_COL);}
            @Override protected double vz_() {assert mVelocities!=null; return mVelocities.get(aIdx, STD_VZ_COL);}
            
            @Override protected void setX_(double aX) {
                if (mPositions == null) throw new UnsupportedOperationException("`setX` for DataXYZ without pos data");
                mPositions.set(aIdx, XYZ_X_COL, aX);
            }
            @Override protected void setY_(double aY) {
                if (mPositions == null) throw new UnsupportedOperationException("`setY` for DataXYZ without pos data");
                mPositions.set(aIdx, XYZ_Y_COL, aY);
            }
            @Override protected void setZ_(double aZ) {
                if (mPositions == null) throw new UnsupportedOperationException("`setZ` for DataXYZ without pos data");
                mPositions.set(aIdx, XYZ_Z_COL, aZ);
            }
            @Override protected void setType_(int aType) {
                if (mSpecies == null) throw new UnsupportedOperationException("`setType` for DataXYZ without species data");
                assert mType2Symbol != null;
                mSpecies[aIdx] = mType2Symbol[aType];
            }
            @Override protected void setVx_(double aVx) {assert mVelocities!=null; mVelocities.set(aIdx, STD_VX_COL, aVx);}
            @Override protected void setVy_(double aVy) {assert mVelocities!=null; mVelocities.set(aIdx, STD_VY_COL, aVy);}
            @Override protected void setVz_(double aVz) {assert mVelocities!=null; mVelocities.set(aIdx, STD_VZ_COL, aVz);}
            
            @Override public String symbol() {
                return mSpecies!=null ? mSpecies[aIdx] : null;
            }
            @Override public double mass() {
                @Nullable String tSymbol = symbol();
                return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
            }
        };
    }
    /**
     * @return {@inheritDoc}
     * @see IBox
     */
    @Override public IBox box() {
        return mBox;
    }
    /** @return {@inheritDoc} */
    @Override public int natoms() {
        return mNumAtoms;
    }
    /** @return {@inheritDoc} */
    @Override public int ntypes() {
        return mType2Symbol==null ? 1 : mType2Symbol.length-1;
    }
    
    /** @return {@inheritDoc} */
    @Override public DataXYZ copy() {
        Map<String, Object> tParameter = new LinkedHashMap<>(mParameters);
        Map<String, Object> tProperties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> oEntry : mProperties.entrySet()) {
            Object oValue = oEntry.getValue();
            if (oValue == null) continue;
            if (oValue instanceof String[]) {
                String[] oValueStr = (String[])oValue;
                String[] tValue = new String[oValueStr.length];
                System.arraycopy(oValueStr, 0, tValue, 0, tValue.length);
                tProperties.put(oEntry.getKey(), tValue);
            } else
            if (oValue instanceof String[][]) {
                String[][] oValueStr = (String[][])oValue;
                int nrows = oValueStr.length;
                int ncols = oValueStr[0].length;
                String[][] tValue = new String[nrows][ncols];
                for (int i = 0; i < nrows; ++i) {
                    System.arraycopy(oValueStr[i], 0, tValue[i], 0, ncols);
                }
                tProperties.put(oEntry.getKey(), tValue);
            } else
            if (oValue instanceof IVector) {
                tProperties.put(oEntry.getKey(), ((IVector)oValue).copy());
            } else
            if (oValue instanceof IMatrix) {
                tProperties.put(oEntry.getKey(), ((IMatrix)oValue).copy());
            } else
            if (oValue instanceof IIntVector) {
                tProperties.put(oEntry.getKey(), ((IIntVector)oValue).copy());
            } else
            if (oValue instanceof IIntMatrix) {
                tProperties.put(oEntry.getKey(), ((IIntMatrix)oValue).copy());
            } else
            if (oValue instanceof ILogicalVector) {
                tProperties.put(oEntry.getKey(), ((ILogicalVector)oValue).copy());
            } else
            if (oValue instanceof ILogicalMatrix) {
                tProperties.put(oEntry.getKey(), ((ILogicalMatrix)oValue).copy());
            }
        }
        DataXYZ tOut = new DataXYZ(mNumAtoms, mComment, tParameter, tProperties, mBox==null?null:mBox.copy());
        if (mType2Symbol != null) {
            String[] tSymbols = new String[mType2Symbol.length-1];
            System.arraycopy(mType2Symbol, 1, tSymbols, 0, tSymbols.length);
            tOut.setSymbolOrder(tSymbols);
        }
        return tOut;
    }
    
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 XYZ 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(IAtomData, String...)} 来手动指定元素符号信息
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的 XYZ 数据
     * @see #of(IAtomData, String...)
     */
    public static DataXYZ of(IAtomData aAtomData) {
        @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
        return of(aAtomData, tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 XYZ 数据
     * @param aAtomData 输入的原子数据
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XYZ 数据
     * @see #of(IAtomData)
     */
    @SuppressWarnings("unchecked")
    public static DataXYZ of(IAtomData aAtomData, String... aSymbols) {
        if (aSymbols == null) aSymbols = ZL_STR;
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof DataXYZ) {
            // DataXYZ 则直接获取即可（专门优化，保留完整模拟盒信息）
            return ((DataXYZ)aAtomData).copy().setSymbols(aSymbols);
        }
        // 直接遍历拷贝数据
        int tNumAtoms = aAtomData.natoms();
        String[] rSpecies = new String[tNumAtoms];
        RowMatrix rPositions = RowMatrix.zeros(tNumAtoms, ATOM_DATA_KEYS_XYZ.length);
        @Nullable RowMatrix rVelocities = aAtomData.hasVelocity() ? RowMatrix.zeros(tNumAtoms, ATOM_DATA_KEYS_VELOCITY.length) : null;
        for (int i = 0; i < tNumAtoms; ++i) {
            IAtom tAtom = aAtomData.atom(i);
            int tType = tAtom.type();
            rSpecies[i] = tType>aSymbols.length ? ("T"+tType) : aSymbols[tType-1];
            rPositions.set(i, XYZ_X_COL, tAtom.x());
            rPositions.set(i, XYZ_Y_COL, tAtom.y());
            rPositions.set(i, XYZ_Z_COL, tAtom.z());
            if (rVelocities != null) {
                rVelocities.set(i, STD_VX_COL, tAtom.vx());
                rVelocities.set(i, STD_VY_COL, tAtom.vy());
                rVelocities.set(i, STD_VZ_COL, tAtom.vz());
            }
        }
        Map<String, Object> rProperties = new LinkedHashMap<>();
        rProperties.put("species", rSpecies);
        rProperties.put("pos", rPositions);
        if (rVelocities != null) rProperties.put("velo", rVelocities);
        // 转换时默认增加 T T T 的 pbc
        Map<String, Object> rParameters = new LinkedHashMap<>();
        rParameters.put("pbc", "T T T");
        DataXYZ rDataXYZ = new DataXYZ(tNumAtoms, null,
                                       rParameters, rProperties, aAtomData.box().copy()).setSymbolOrder(aSymbols);
        // 针对 AseAtoms 特殊处理，这里简单直接补充添加额外属性
        if (aAtomData instanceof AseAtoms) {
            AseAtoms tAtoms = (AseAtoms)aAtomData;
            // 遍历添加，对于 calc results 的属性只考虑特殊已知名称，这里主要保证 xyz 的格式合法
            for (Map.Entry<String, Object> tEntry : tAtoms.calcResults().entrySet()) {
                String tKey = tEntry.getKey();
                Object tValue = tEntry.getValue();
                switch(tKey) {
                case "energy": case "free_energy": {
                    if (tValue instanceof Number) {
                        rDataXYZ.setParameter(tKey, ((Number)tValue).doubleValue());
                    }
                    break;
                }
                case "magmom": {
                    if (tValue instanceof IVector) {
                        rDataXYZ.setParameter(tKey, String.join(" ", AbstractCollections.map((IVector)tValue, Object::toString)));
                    } else
                    if (tValue instanceof Number) {
                        rDataXYZ.setParameter(tKey, ((Number)tValue).doubleValue());
                    }
                    break;
                }
                case "stress": {
                    if (tValue instanceof IVector) {
                        //  0,  1,  2,  3,  4,  5
                        // xx, yy, zz, yz, xz, xy
                        IVector tStressAse = (IVector)tValue;
                        //  0,  1,  2,  3,  4,  5,  6,  7,  8
                        // xx, xy, xz, yx, yy, yz, zx, zy, zz
                        IVector tStressXYZ = Vectors.zeros(9);
                        tStressXYZ.set(0, tStressAse.get(0)); tStressXYZ.set(1, tStressAse.get(5)); tStressXYZ.set(2, tStressAse.get(4));
                        tStressXYZ.set(3, tStressAse.get(5)); tStressXYZ.set(4, tStressAse.get(1)); tStressXYZ.set(5, tStressAse.get(3));
                        tStressXYZ.set(6, tStressAse.get(4)); tStressXYZ.set(7, tStressAse.get(3)); tStressXYZ.set(8, tStressAse.get(2));
                        rDataXYZ.setParameter(tKey, String.join(" ", AbstractCollections.map(tStressXYZ, Object::toString)));
                    }
                    break;
                }
                case "dipole": {
                    if (tValue instanceof IVector) {
                        rDataXYZ.setParameter(tKey, String.join(" ", AbstractCollections.map((IVector)tValue, Object::toString)));
                    }
                    break;
                }
                case "forces": case "stresses": {
                    if (tValue instanceof IMatrix) {
                        rDataXYZ.setProperty(tKey, ((IMatrix)tValue).copy());
                    }
                    break;
                }
                case "energies": case "charges": {
                    if (tValue instanceof IVector) {
                        rDataXYZ.setProperty(tKey, ((IVector)tValue).copy());
                    }
                    break;
                }
                case "magmoms": {
                    if (tValue instanceof IVector) {
                        rDataXYZ.setProperty(tKey, ((IVector)tValue).copy());
                    } else
                    if (tValue instanceof IMatrix) {
                        rDataXYZ.setProperty(tKey, ((IMatrix)tValue).copy());
                    }
                    break;
                }}
            }
            for (Map.Entry<String, Object> tEntry : tAtoms.infos().entrySet()) {
                String tKey = tEntry.getKey();
                Object tValue = tEntry.getValue();
                if (tValue instanceof CharSequence) {
                    rDataXYZ.setParameter(tKey, ((CharSequence)tValue).toString());
                } else
                if (tValue instanceof Number) {
                    rDataXYZ.setParameter(tKey, ((Number)tValue).doubleValue());
                } else
                if (tValue instanceof IVector) {
                    rDataXYZ.setParameter(tKey, String.join(" ", AbstractCollections.map((IVector)tValue, Object::toString)));
                } else
                if (tValue instanceof IIntVector) {
                    rDataXYZ.setParameter(tKey, String.join(" ", AbstractCollections.map((IIntVector)tValue, Object::toString)));
                } else
                if (tValue instanceof ILogicalVector) {
                    // 此类型不支持完美的反向转换，但目前 jse 协议下的 ext-xyz 不认为此类型合法，因此是额外扩充兼容
                    rDataXYZ.setParameter(tKey, String.join(" ", AbstractCollections.map(((ILogicalVector)tValue).asList(), v->(v?"T":"F"))));
                } else
                if (tValue instanceof List) {
                    // 此类型不支持完美的反向转换，但目前 jse 协议下的 ext-xyz 不认为此类型合法，因此是额外扩充兼容
                    rDataXYZ.setParameter(tKey, String.join(" ", AbstractCollections.map((List<?>)tValue, v -> {
                        if (v instanceof Boolean) {
                            return (Boolean)v ? "T" : "F";
                        }
                        return v.toString();
                    })));
                }
            }
            for (Map.Entry<String, Object> tEntry : tAtoms.arrays().entrySet()) {
                String tKey = tEntry.getKey();
                Object tValue = tEntry.getValue();
                switch(tKey) {
                case "numbers": case "positions": case "momenta": {
                    continue;
                }}
                if (tValue instanceof IVector) {
                    rDataXYZ.setProperty(tKey, ((IVector)tValue).copy());
                } else
                if (tValue instanceof IMatrix) {
                    rDataXYZ.setProperty(tKey, ((IMatrix)tValue).copy());
                } else
                if (tValue instanceof IIntVector) {
                    rDataXYZ.setProperty(tKey, ((IIntVector)tValue).copy());
                } else
                if (tValue instanceof IIntMatrix) {
                    rDataXYZ.setProperty(tKey, ((IIntMatrix)tValue).copy());
                } else
                if (tValue instanceof ILogicalVector) {
                    rDataXYZ.setProperty(tKey, ((ILogicalVector)tValue).copy());
                } else
                if (tValue instanceof ILogicalMatrix) {
                    rDataXYZ.setProperty(tKey, ((ILogicalMatrix)tValue).copy());
                } else
                if (tValue instanceof String[]) {
                    String[] tValue2 = new String[((String[])tValue).length];
                    System.arraycopy((String[])tValue, 0, tValue2, 0, tValue2.length);
                    rDataXYZ.setProperty(tKey, tValue2);
                } else
                if (tValue instanceof String[][]) {
                    int nrows = ((String[][])tValue).length;
                    int ncols = ((String[][])tValue)[0].length;
                    String[][] tValue2 = new String[nrows][ncols];
                    for (int i = 0; i < nrows; ++i) {
                        System.arraycopy(((String[][])tValue)[i], 0, tValue2[i], 0, ncols);
                    }
                    rDataXYZ.setProperty(tKey, tValue2);
                } else
                if (tValue instanceof List) {
                    int nrows = ((List<?>)tValue).size();
                    if (nrows == 0) {
                        rDataXYZ.setProperty(tKey, ZL_STR);
                    }
                    Object tObj = ((List<?>)tValue).get(0);
                    if (tObj instanceof List) {
                        int ncols = ((List<?>)tObj).size();
                        Object tObj2 = ((List<?>)tObj).get(0);
                        if (tObj2 instanceof Boolean) {
                            rDataXYZ.setProperty(tKey, Matrices.fromBoolean((List<?>)tValue));
                        } else
                        if (tObj2 instanceof Number) {
                            rDataXYZ.setProperty(tKey, Matrices.from((List<?>)tValue));
                        } else {
                            String[][] tValue2 = new String[nrows][ncols];
                            for (int i = 0; i < nrows; ++i) {
                                List<?> tRow = (List<?>)((List<?>)tValue).get(i);
                                for (int j = 0; j < ncols; ++j) {
                                    tValue2[i][j] = tRow.get(j).toString();
                                }
                            }
                            rDataXYZ.setProperty(tKey, tValue2);
                        }
                    } else
                    if (tObj instanceof Boolean) {
                        rDataXYZ.setProperty(tKey, Vectors.fromBoolean((List<Boolean>)tValue));
                    } else
                    if (tObj instanceof Number) {
                        rDataXYZ.setProperty(tKey, Vectors.from((List<? extends Number>)tValue));
                    } else {
                        String[] tValue2 = new String[nrows];
                        for (int i = 0; i < nrows; ++i) {
                            tValue2[i] = ((List<?>)tValue).get(i).toString();
                        }
                        rDataXYZ.setProperty(tKey, tValue2);
                    }
                }
            }
        }
        return rDataXYZ;
    }
    /**
     * 传入列表形式元素符号的转换
     * @see #of(IAtomData, String...)
     * @see Collection
     */
    public static DataXYZ of(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {
        return of(aAtomData, IO.Text.toArray(aSymbols));
    }
    
    
    /// 文件读写
    /**
     * 从 XYZ 原子数据格式或者扩展的 XYZ 格式文件读取来初始化
     * @param aFilePath XYZ 文件路径
     * @return 读取得到的 {@link DataXYZ} 对象
     * @throws IOException 如果读取失败
     * @see #write(String)
     */
    public static DataXYZ read(String aFilePath) throws IOException {
        try (BufferedReader tReader = IO.toReader(aFilePath)) {
            return read(tReader);
        }
    }
    /**
     * 提供使用 {@link BufferedReader} 的流式接口
     * @param aReader 需要的读取流
     * @return 读取得到的 {@link DataXYZ} 对象
     * @throws IOException 如果读取失败
     * @throws FileEndException 在发现文件似乎不完整时
     */
    public static DataXYZ read(BufferedReader aReader) throws IOException {
        String tLine;
        String[] tTokens;
        
        int aNumAtoms;
        String aComment;
        Map<String, Object> aParameters = new LinkedHashMap<>();
        IBox aBox = null;
        Map<String, Object> aProperties = null;
        
        // 第一行为原子数
        tLine = aReader.readLine(); if (tLine==null) {IO.fileEnd(); return null;}
        aNumAtoms = Integer.parseInt(tLine.trim());
        // 第二行为 comment
        tLine = aReader.readLine(); if (tLine==null) {IO.fileEnd(); return null;}
        aComment = tLine;
        // 对于扩展的 XYZ 格式，comment 会包含其余重要信息，需要解析 comment
        @Nullable String tParseErr = parseParameters_(aComment, aParameters);
        // 只要有 Properties 属性就认为是扩展的 XYZ，此时有任何解析错误就抛出错误
        if ((aParameters.containsKey("Properties") || aParameters.containsKey("properties")) && tParseErr!=null) {
            if (Conf.STRICT_IO) throw new IllegalArgumentException(tParseErr);
            // 为了避免意外的转换，这里还是返回 null，毕竟传统 xyz 格式是基本弃用的，只提供最低程度支持
            return null;
        }
        // 有任何错误回退到传统 xyz
        if (tParseErr!=null) {
            aParameters.clear();
        } else {
            aComment = null;
            // 读取模拟盒信息
            Object tLattice = aParameters.remove("Lattice");
            if (tLattice == null) {
                tLattice = aParameters.remove("lattice");
            }
            if (tLattice instanceof String) {
                Vector tVec = IO.Text.str2data((String)tLattice, 9);
                boolean tAnyNaN = false;
                for (int i = 0; i < 9; ++i) {
                    if (Double.isNaN(tVec.get(i))) {
                        tAnyNaN = true;
                        break;
                    }
                }
                if (!tAnyNaN) {
                    // 判断是否是 prism 并据此创建 Box
                    boolean tNotPrism =
                           MathEX.Code.numericEqual(tVec.get(1), 0.0) && MathEX.Code.numericEqual(tVec.get(2), 0.0)
                        && MathEX.Code.numericEqual(tVec.get(3), 0.0) && MathEX.Code.numericEqual(tVec.get(5), 0.0)
                        && MathEX.Code.numericEqual(tVec.get(6), 0.0) && MathEX.Code.numericEqual(tVec.get(7), 0.0)
                        ;
                    aBox = tNotPrism ?
                        new Box(tVec.get(0), tVec.get(4), tVec.get(8)) :
                        new BoxPrism(
                            tVec.get(0), tVec.get(1), tVec.get(2),
                            tVec.get(3), tVec.get(4), tVec.get(5),
                            tVec.get(6), tVec.get(7), tVec.get(8)
                        );
                }
            }
            // 读取参数信息
            Object tProperties = aParameters.remove("Properties");
            if (tProperties == null) {
                tProperties = aParameters.remove("properties");
            }
            if (tProperties instanceof String) {
                aProperties = new LinkedHashMap<>();
                String[] tPropertiesArr = ((String)tProperties).split(":");
                final int tEnd = tPropertiesArr.length-2;
                for (int i = 0; i < tEnd; i+=3) {
                    String tKey = tPropertiesArr[i];
                    String tType = tPropertiesArr[i+1];
                    int tCols = Integer.parseInt(tPropertiesArr[i+2]);
                    switch (tType) {
                    case "S": {
                        aProperties.put(tKey, tCols==1 ? new String[aNumAtoms] : new String[aNumAtoms][tCols]);
                        break;
                    }
                    case "R": {
                        aProperties.put(tKey, tCols==1 ? Vectors.zeros(aNumAtoms) : RowMatrix.zeros(aNumAtoms, tCols));
                        break;
                    }
                    case "I": {
                        aProperties.put(tKey, tCols==1 ? IntVector.zeros(aNumAtoms) : RowIntMatrix.zeros(aNumAtoms, tCols));
                        break;
                    }
                    case "L": {
                        aProperties.put(tKey, tCols==1 ? LogicalVector.zeros(aNumAtoms) : RowLogicalMatrix.zeros(aNumAtoms, tCols));
                        break;
                    }}
                }
            }
        }
        if (aProperties == null) {
            // 即使是非 Extended，也需要有 Properties 以及 Box
            aProperties = new LinkedHashMap<>();
            aProperties.put("species", new String[aNumAtoms]);
            aProperties.put("pos", RowMatrix.zeros(aNumAtoms, ATOM_DATA_KEYS_XYZ.length));
        }
        // 简单遍历后续数据
        for (int i = 0; i < aNumAtoms; ++i) {
            tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;}
            tTokens = IO.Text.splitBlank(tLine);
            // 基于 aProperties 的顺序解析，现在可以统一解析语法
            int j = 0;
            for (Object tValue : aProperties.values()) {
                if (tValue instanceof String[]) {
                    ((String[])tValue)[i] = tTokens[j];
                    ++j;
                } else
                if (tValue instanceof String[][]) {
                    String[] tRow = ((String[][])tValue)[i];
                    for (int k = 0; k < tRow.length; ++k) {
                        tRow[k] = tTokens[j];
                        ++j;
                    }
                } else
                if (tValue instanceof IVector) {
                    ((IVector)tValue).set(i, IO.Text.str2double(tTokens[j]));
                    ++j;
                } else
                if (tValue instanceof IMatrix) {
                    IVector tRow = ((IMatrix)tValue).row(i);
                    for (int k = 0; k < tRow.size(); ++k) {
                        tRow.set(k, IO.Text.str2double(tTokens[j]));
                        ++j;
                    }
                } else
                if (tValue instanceof IIntVector) {
                    ((IIntVector)tValue).set(i, IO.Text.str2int(tTokens[j]));
                    ++j;
                } else
                if (tValue instanceof IIntMatrix) {
                    IIntVector tRow = ((IIntMatrix)tValue).row(i);
                    for (int k = 0; k < tRow.size(); ++k) {
                        tRow.set(k, IO.Text.str2int(tTokens[j]));
                        ++j;
                    }
                } else
                if (tValue instanceof ILogicalVector) {
                    boolean tSub;
                    if (tTokens[j].equals("T")) {
                        tSub = true;
                    } else
                    if (tTokens[j].equals("F")) {
                        tSub = false;
                    } else {
                        throw new IllegalArgumentException("Illegal L token: " + tTokens[j]);
                    }
                    ((ILogicalVector)tValue).set(i, tSub);
                    ++j;
                } else
                if (tValue instanceof ILogicalMatrix) {
                    ILogicalVector tRow = ((ILogicalMatrix)tValue).row(i);
                    for (int k = 0; k < tRow.size(); ++k) {
                        boolean tSub;
                        if (tTokens[j].equals("T")) {
                            tSub = true;
                        } else
                        if (tTokens[j].equals("F")) {
                            tSub = false;
                        } else {
                            throw new IllegalArgumentException("Illegal L token: " + tTokens[j]);
                        }
                        tRow.set(k, tSub);
                        ++j;
                    }
                }
            }
        }
        // 返回 XYZ
        return new DataXYZ(aNumAtoms, aComment, aParameters, aProperties, aBox);
    }
    private static String subStringLimit_(String aStr, int aBegin) {
        int tSize = aStr.length();
        if (tSize-aBegin > 16) {
            return aStr.substring(aBegin, aBegin+12) + "...";
        }
        return aStr.substring(aBegin, tSize);
    }
    static @Nullable String parseParameters_(String aComment, Map<String, Object> rParameters) {
        aComment = aComment.trim();
        // 这个操作比较复杂，还需要处理双引号的情况
        final int tLen = aComment.length();
        int tKeyBegin = 0;
        while (tKeyBegin < tLen) {
            int tKeyEnd = aComment.indexOf('=', tKeyBegin);
            if (tKeyEnd < 0) {
                return "Miss `=` from: "+subStringLimit_(aComment, tKeyBegin);
            }
            int tValueBegin = tKeyEnd+1;
            boolean tHasQuote = aComment.charAt(tValueBegin)=='"';
            int tValueEnd;
            if (tHasQuote) {
                ++tValueBegin;
                tValueEnd = aComment.indexOf('"', tValueBegin);
                if (tValueEnd < 0) {
                    return "Miss `\"` from: "+subStringLimit_(aComment, tValueBegin-1);
                }
            } else {
                tValueEnd = IO.Text.findBlankIndex(aComment, tValueBegin);
                if (tValueEnd < 0) tValueEnd = tLen;
            }
            Object tValue = aComment.substring(tValueBegin, tValueEnd);
            if (!tHasQuote) {
                if (tValue.equals("T")) {
                    tValue = true;
                } else
                if (tValue.equals("F")) {
                    tValue = false;
                } else {
                    Number tNumberValue = IO.Text.str2number(true, (String)tValue);
                    if (tNumberValue != null) tValue = tNumberValue;
                }
            }
            String tKey = aComment.substring(tKeyBegin, tKeyEnd);
            if (isInvalidKey(tKey)) return "Invalid key: "+tKey;
            rParameters.put(tKey, tValue);
            if (tHasQuote) ++tValueEnd;
            tKeyBegin = IO.Text.findNoBlankIndex(aComment, tValueEnd);
            if (tKeyBegin < 0) break; // 注意这种情况是已经结束了
        }
        // 这里削弱要求，不一定需要特定的参数名称
        // null 表示没有任何解析错误
        return null;
    }
    
    
    /**
     * 输出成标准的 XYZ 文件，会根据需要自动选择原始的 XYZ 格式或者扩展的 XYZ 格式
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     * @see #read(String)
     */
    public void write(String aFilePath) throws IOException {
        try (IO.IWriteln tWriteln = IO.toWriteln(aFilePath)) {write(tWriteln);}
    }
    /**
     * 提供使用 {@link IO.IWriteln} 的流式接口
     * @param aWriteln 需要写入的流
     * @throws IOException 如果写入文件失败
     */
    public void write(IO.IWriteln aWriteln) throws IOException {
        aWriteln.writeln(String.valueOf(mNumAtoms));
        if (mComment != null) {
            aWriteln.writeln(mComment);
        } else {
            StringBuilder rLine = new StringBuilder();
            if (mBox != null) {
                rLine.append("Lattice=\"");
                rLine.append(mBox.ax()); rLine.append(" ");
                rLine.append(mBox.ay()); rLine.append(" ");
                rLine.append(mBox.az()); rLine.append(" ");
                rLine.append(mBox.bx()); rLine.append(" ");
                rLine.append(mBox.by()); rLine.append(" ");
                rLine.append(mBox.bz()); rLine.append(" ");
                rLine.append(mBox.cx()); rLine.append(" ");
                rLine.append(mBox.cy()); rLine.append(" ");
                rLine.append(mBox.cz()); rLine.append("\"");
                rLine.append(" ");
            }
            rLine.append("Properties=");
            boolean tFirst = true;
            for (Map.Entry<String, Object> tEntry : mProperties.entrySet()) {
                Object tValue = tEntry.getValue();
                if (tValue == null) continue;
                if (!tFirst) rLine.append(":");
                else tFirst = false;
                if (tValue instanceof String[]) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":S:1");
                } else
                if (tValue instanceof String[][]) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":S:");
                    rLine.append(((String[][])tValue)[0].length);
                } else
                if (tValue instanceof IVector) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":R:1");
                } else
                if (tValue instanceof IMatrix) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":R:");
                    rLine.append(((IMatrix)tValue).ncols());
                } else
                if (tValue instanceof IIntVector) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":I:1");
                } else
                if (tValue instanceof IIntMatrix) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":I:");
                    rLine.append(((IIntMatrix)tValue).ncols());
                } else
                if (tValue instanceof ILogicalVector) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":L:1");
                } else
                if (tValue instanceof ILogicalMatrix) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":L:");
                    rLine.append(((ILogicalMatrix)tValue).ncols());
                }
            }
            // 如果不存在 pbc 参数，则自动写入一个 T T T
            if (!mParameters.containsKey("pbc")) {
                rLine.append(" pbc=\"T T T\"");
            }
            for (Map.Entry<String, Object> tEntry : mParameters.entrySet()) {
                Object tValue = tEntry.getValue();
                if (tValue == null) continue;
                rLine.append(" ");
                rLine.append(tEntry.getKey()); rLine.append("=");
                if (tValue instanceof Boolean) {
                    rLine.append((Boolean)tValue ? "T" : "F");
                } else {
                    String tValueStr = tValue.toString();
                    boolean tHasBlank = IO.Text.findBlankIndex(tValueStr, 0)>=0;
                    if (tHasBlank) {
                        tValueStr = tValueStr.replace("\"", "");
                        rLine.append("\"");
                    }
                    rLine.append(tValueStr);
                    if (tHasBlank) {
                        rLine.append("\"");
                    }
                }
            }
            aWriteln.writeln(rLine.toString());
        }
        for (int i = 0; i < mNumAtoms; ++i) {
            // 基于 mProperties 的顺序写入
            List<String> rLine = new ArrayList<>();
            for (Object tValue : mProperties.values()) {
                if (tValue instanceof String[]) {
                    rLine.add(((String[])tValue)[i]);
                } else
                if (tValue instanceof String[][]) {
                    String[] tRow = ((String[][])tValue)[i];
                    rLine.addAll(AbstractCollections.from(tRow));
                } else
                if (tValue instanceof IVector) {
                    rLine.add(String.valueOf(((IVector)tValue).get(i)));
                } else
                if (tValue instanceof IMatrix) {
                    IVector tRow = ((IMatrix)tValue).row(i);
                    rLine.addAll(AbstractCollections.map(tRow, String::valueOf));
                } else
                if (tValue instanceof IIntVector) {
                    rLine.add(String.valueOf(((IIntVector)tValue).get(i)));
                } else
                if (tValue instanceof IIntMatrix) {
                    IIntVector tRow = ((IIntMatrix)tValue).row(i);
                    rLine.addAll(AbstractCollections.map(tRow, String::valueOf));
                } else
                if (tValue instanceof ILogicalVector) {
                    rLine.add(((ILogicalVector)tValue).get(i) ? "T" : "F");
                } else
                if (tValue instanceof ILogicalMatrix) {
                    ILogicalVector tRow = ((ILogicalMatrix)tValue).row(i);
                    rLine.addAll(AbstractCollections.map(tRow.asList(), v -> v ? "T" : "F"));
                }
            }
            aWriteln.writeln(String.join(" ", rLine));
        }
    }
}
