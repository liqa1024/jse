package jse.atom.data;

import jse.atom.*;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.IIntMatrix;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowIntMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static jse.code.CS.*;

/**
 * <a href="https://en.wikipedia.org/wiki/XYZ_file_format">
 * XYZ 文件格式 </a> 的读写支持，由于名称上和现有的代表坐标点的
 * {@link jse.atom.XYZ} 一致，因此这里统称 {@code DataXYZ}
 * <p>
 * 这里主要对
 * <a href="https://docs.ovito.org/reference/file_formats/input/xyz.html#file-formats-input-xyz-extended-format">
 * 扩展的 XYZ 格式 </a> 提供支持
 * @author liqa
 */
public class DataXYZ extends AbstractSettableAtomData {
    private final int mAtomNum;
    private @Nullable String mComment;
    private final @NotNull Map<String, Object> mParameters;
    private final @NotNull Map<String, Object> mProperties;
    
    private final @Nullable IBox mBox;
    private String @Nullable[] mSpecies = null;
    private @Nullable IMatrix mPositions = null;
    private @Nullable IMatrix mVelocities  = null;
    
    /** 用于通过原子序数获取每个种类的粒子数 */
    private final @NotNull Map<String, Integer> mSymbol2Type;
    private String @Nullable[] mType2Symbol; // 第 0 个直接制空
    
    DataXYZ(int aAtomNum, @Nullable String aComment, @NotNull Map<String, Object> aParameters, @NotNull Map<String, Object> aProperties, @Nullable IBox aBox) {
        mAtomNum = aAtomNum;
        mComment = aComment;
        mParameters = aParameters;
        mProperties = aProperties;
        Object tSymbols = mProperties.get("species");
        if (tSymbols instanceof String[]) mSpecies = (String[])tSymbols;
        Object tPositions = mProperties.get("pos");
        if (tPositions instanceof IMatrix && ((IMatrix)tPositions).columnNumber()==3) mPositions = (IMatrix)tPositions;
        Object tVelocities = mProperties.get("velo");
        if (tVelocities instanceof IMatrix && ((IMatrix)tVelocities).columnNumber()==3) {
            mVelocities = (IMatrix)tVelocities;
        } else {
            tVelocities = mProperties.get("vel");
            if (tVelocities instanceof IMatrix && ((IMatrix)tVelocities).columnNumber()==3) mVelocities = (IMatrix)tVelocities;
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
    
    @Override public boolean hasVelocity() {return mVelocities!=null;}
    @Override public boolean hasSymbol() {return mSpecies!=null;}
    @Override public String symbol(int aType) {
        if (mType2Symbol == null) return null;
        return mType2Symbol[aType];
    }
    @Override public boolean hasMass() {return hasSymbol();}
    @Override public double mass(int aType) {
        if (!hasSymbol()) return Double.NaN;
        return MASS.get(symbol(aType));
    }
    /** XYZ 特有的接口 */
    public @Nullable String comment() {return mComment;}
    @VisibleForTesting public @Nullable String getComment() {return mComment;}
    public DataXYZ setComment(String aComment) {
        if (mComment == null) throw new UnsupportedOperationException("`setComment` for DataXYZ without comment");
        mComment = aComment;
        return this;
    }
    public Object parameter(String aKey) {return mParameters.get(aKey);}
    public DataXYZ setParameter(String aKey, Object aValue) {
        if (aKey.equals("Lattice")) throw new IllegalArgumentException("Lattice for DataXYZ parameter");
        if (aKey.equals("Properties")) throw new IllegalArgumentException("Properties for DataXYZ parameter");
        if (mComment != null) {
            // 如果 comment 没有影响解析的字符则直接保留 comment
            if (!mComment.contains("\"")) mParameters.put("Comment", mComment);
            mComment = null;
        }
        if (aValue == null) {
            mParameters.remove(aKey);
            return this;
        }
        mParameters.put(aKey, aValue);
        return this;
    }
    public Object removeParameter(String aKey) {
        if (aKey.equals("Lattice")) throw new IllegalArgumentException("Lattice for DataXYZ parameter");
        if (aKey.equals("Properties")) throw new IllegalArgumentException("Properties for DataXYZ parameter");
        return mParameters.remove(aKey);
    }
    public Object property(String aKey) {return mProperties.get(aKey);}
    public DataXYZ setProperty(String aKey, Object aValue) {
        if (mComment != null) {
            // 如果 comment 没有影响解析的字符则直接保留 comment
            if (!mComment.contains("\"")) mParameters.put("Comment", mComment);
            mComment = null;
        }
        if (aValue == null) {
            removeProperty(aKey);
            return this;
        }
        // 这里简单处理，只对特定的参数做类型检测；总之后面遇到不识别的就不输出
        switch(aKey) {
        case "pos": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for pos, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.rowNumber() < mAtomNum) throw new IndexOutOfBoundsException("Need: "+mAtomNum+", Size: "+tValue.rowNumber());
            if (tValue.columnNumber() < ATOM_DATA_KEYS_XYZ.length) throw new IndexOutOfBoundsException("Need: "+ATOM_DATA_KEYS_XYZ.length+", Size: "+tValue.columnNumber());
            mPositions = tValue;
            mProperties.put("pos", tValue);
            return this;
        }
        case "species": {
            if (!(aValue instanceof String[])) throw new IllegalArgumentException("Value type MUST be String[] for species, input: "+aValue.getClass().getName());
            String[] tValue = (String[])aValue;
            if (tValue.length < mAtomNum) throw new IndexOutOfBoundsException("Need: "+mAtomNum+", Size: "+tValue.length);
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
            mProperties.put("species", tValue);
            return this;
        }
        case "velo": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for velo, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.rowNumber() < mAtomNum) throw new IndexOutOfBoundsException("Need: "+mAtomNum+", Size: "+tValue.rowNumber());
            if (tValue.columnNumber() < ATOM_DATA_KEYS_VELOCITY.length) throw new IndexOutOfBoundsException("Need: "+ATOM_DATA_KEYS_VELOCITY.length+", Size: "+tValue.columnNumber());
            mVelocities = tValue;
            mProperties.put("velo", tValue);
            return this;
        }
        case "vel": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for vel, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.rowNumber() < mAtomNum) throw new IndexOutOfBoundsException("Need: "+mAtomNum+", Size: "+tValue.rowNumber());
            if (tValue.columnNumber() < ATOM_DATA_KEYS_VELOCITY.length) throw new IndexOutOfBoundsException("Need: "+ATOM_DATA_KEYS_VELOCITY.length+", Size: "+tValue.columnNumber());
            Object tVelo = mProperties.remove("velo");
            if (!(tVelo instanceof IMatrix) || ((IMatrix) tVelo).columnNumber()!=3) {
                mVelocities = tValue;
            }
            mProperties.put("vel", tValue);
            return this;
        }}
        mProperties.put(aKey, aValue);
        return this;
    }
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
            Object tVel = mProperties.remove("vel");
            if (tVel instanceof IMatrix && ((IMatrix)tVel).columnNumber()==3) {
                mVelocities = (IMatrix)tVel;
            } else {
                mVelocities = null;
            }
            return mProperties.remove("velo");
        }
        case "vel": {
            Object tVelo = mProperties.remove("velo");
            if (!(tVelo instanceof IMatrix) || ((IMatrix) tVelo).columnNumber()!=3) {
                mVelocities = null;
            }
            return mProperties.remove("vel");
        }}
        return mProperties.remove(aKey);
    }
    
    
    /** 支持调整种类的顺序，这对于 xyz 比较重要 */
    public DataXYZ setSymbolOrder(String... aSymbolOrder) {
        if (mSpecies == null) throw new UnsupportedOperationException("`setSymbolOrder` for DataXYZ without species data");
        assert mType2Symbol != null;
        if (aSymbolOrder == null) aSymbolOrder = ZL_STR;
        if (aSymbolOrder.length > atomTypeNumber()) {
            mType2Symbol = new String[aSymbolOrder.length+1];
        }
        System.arraycopy(aSymbolOrder, 0, mType2Symbol, 1, aSymbolOrder.length);
        // 先更新 mSymbol2Type
        validSymbol2Type_();
        // 遍历一次 mSymbols 确保 mType2Symbol 全部覆盖
        for (String tSymbol : mSpecies) {
            if (!mSymbol2Type.containsKey(tSymbol)) {
                int tType = mSymbol2Type.size() + 1;
                mSymbol2Type.put(tSymbol, tType);
            }
        }
        // 如果有缺失需要补充
        if (mSymbol2Type.size() > mType2Symbol.length-1) {
            mType2Symbol = new String[mSymbol2Type.size()+1];
            for (Map.Entry<String, Integer> tEntry : mSymbol2Type.entrySet()) {
                mType2Symbol[tEntry.getValue()] = tEntry.getKey();
            }
        }
        return this;
    }
    
    /** 支持直接修改 symbols，只会增大种类数，不会减少；少于种类数时会保留旧值 */
    @Override public DataXYZ setSymbols(String... aSymbols) {
        if (mSpecies == null) throw new UnsupportedOperationException("`setSymbols` for DataXYZ without species data");
        assert mType2Symbol != null;
        if (aSymbols==null || aSymbols.length==0) return this;
        if (aSymbols.length > atomTypeNumber()) {
            mType2Symbol = new String[aSymbols.length+1];
        }
        System.arraycopy(aSymbols, 0, mType2Symbol, 1, aSymbols.length);
        // 更新 mSymbols，此时需要旧的 mSymbol2Type
        for (int i = 0; i < mSpecies.length; ++i) {
            mSpecies[i] = mType2Symbol[mSymbol2Type.get(mSpecies[i])];
        }
        validSymbol2Type_();
        return this;
    }
    /** 设置原子种类数目 */
    @Override public DataXYZ setAtomTypeNumber(int aAtomTypeNum) {
        if (mSpecies == null) throw new UnsupportedOperationException("`setAtomTypeNumber` for DataXYZ without species data");
        assert mType2Symbol != null;
        int oTypeNum = atomTypeNumber();
        if (aAtomTypeNum == oTypeNum) return this;
        if (aAtomTypeNum < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            for (int i = 0; i < mSpecies.length; ++i) {
                int tType = mSymbol2Type.get(mSpecies[i]);
                if (tType > aAtomTypeNum) {
                    mSpecies[i] = mType2Symbol[aAtomTypeNum];
                }
            }
            String[] oType2Symbol = mType2Symbol;
            mType2Symbol = new String[aAtomTypeNum+1];
            System.arraycopy(oType2Symbol, 0, mType2Symbol, 0, mType2Symbol.length);
            validSymbol2Type_();
            return this;
        }
        String[] oType2Symbol = mType2Symbol;
        mType2Symbol = new String[aAtomTypeNum+1];
        System.arraycopy(oType2Symbol, 0, mType2Symbol, 0, oType2Symbol.length);
        for (int tType = oType2Symbol.length; tType <= aAtomTypeNum; ++tType) {
            mType2Symbol[tType] = "T"+tType;
        }
        validSymbol2Type_();
        return this;
    }
    void validSymbol2Type_() {
        assert mType2Symbol != null;
        mSymbol2Type.clear();
        int tAtomTypeNumber = atomTypeNumber();
        for (int tType = 1; tType <= tAtomTypeNumber; ++tType) {
            mSymbol2Type.put(mType2Symbol[tType], tType);
        }
    }
    @Override public DataXYZ setNoVelocity() {
        mVelocities = null;
        mProperties.remove("velo");
        mProperties.remove("vel");
        return this;
    }
    @Override public DataXYZ setHasVelocity() {
        if (mVelocities == null) {
            mVelocities = RowMatrix.zeros(atomNumber(), ATOM_DATA_KEYS_VELOCITY.length);
            mProperties.put("velo", mVelocities);
        }
        return this;
    }
    
    /** AbstractAtomData stuffs */
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
                return hasSymbol() ? MASS.get(symbol()) : Double.NaN;
            }
        };
    }
    @Override public IBox box() {return mBox;}
    @Override public int atomNumber() {return mAtomNum;}
    @Override public int atomTypeNumber() {return mType2Symbol==null ? 1 : mType2Symbol.length-1;}
    
    /** 拷贝一份 XYZ */
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
                String[][] tValue = new String[oValueStr.length][oValueStr[0].length];
                System.arraycopy(oValueStr, 0, tValue, 0, tValue.length);
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
            }
        }
        DataXYZ tOut = new DataXYZ(mAtomNum, mComment, tParameter, tProperties, mBox==null?null:mBox.copy());
        if (mType2Symbol != null) {
            String[] tSymbols = new String[mType2Symbol.length-1];
            System.arraycopy(mType2Symbol, 1, tSymbols, 0, tSymbols.length);
            tOut.setSymbolOrder(tSymbols);
        }
        return tOut;
    }
    
    /** 从 IAtomData 来创建，一般来说 XYZ 需要一个额外的 aSymbols 信息 */
    public static DataXYZ fromAtomData(IAtomData aAtomData) {
        @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
        return fromAtomData(aAtomData, tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    public static DataXYZ fromAtomData(IAtomData aAtomData, String... aSymbols) {
        if (aSymbols == null) aSymbols = ZL_STR;
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof DataXYZ) {
            // DataXYZ 则直接获取即可（专门优化，保留完整模拟盒信息）
            return ((DataXYZ)aAtomData).copy().setSymbols(aSymbols);
        } else {
            // 直接遍历拷贝数据
            int tAtomNum = aAtomData.atomNumber();
            String[] rSpecies = new String[tAtomNum];
            RowMatrix rPositions = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix rVelocities = aAtomData.hasVelocity() ? RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length) : null;
            for (int i = 0; i < tAtomNum; ++i) {
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
            return new DataXYZ(tAtomNum, null, new LinkedHashMap<>(), rProperties, aAtomData.box().copy()).setSymbols(aSymbols);
        }
    }
    public static DataXYZ fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {return fromAtomData(aAtomData, UT.Text.toArray(aSymbols));}
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static DataXYZ of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static DataXYZ of(IAtomData aAtomData, String... aSymbols) {return fromAtomData(aAtomData, aSymbols);}
    public static DataXYZ of(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {return fromAtomData(aAtomData, aSymbols);}
    
    
    /// 文件读写
    /**
     * 从标准的 XYZ 文件中读取来实现初始化
     * @param aFilePath XYZ 文件路径
     * @return 读取得到的 DataXYZ 对象，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static DataXYZ read(String aFilePath) throws IOException {try (BufferedReader tReader = UT.IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static DataXYZ read_(BufferedReader aReader) throws IOException {
        String tLine;
        String[] tTokens;
        
        int aAtomNum;
        String aComment;
        Map<String, Object> aParameters = new LinkedHashMap<>();
        IBox aBox = null;
        Map<String, Object> aProperties = null;
        
        // 第一行为原子数
        tLine = aReader.readLine(); if (tLine == null) return null;
        aAtomNum = Integer.parseInt(tLine.trim());
        // 第二行为 comment
        tLine = aReader.readLine(); if (tLine == null) return null;
        aComment = tLine;
        // 对于扩展的 XYZ 格式，comment 会包含其余重要信息，需要解析 comment
        boolean tIsExtended = parseParameters_(aComment, aParameters);
        if (!tIsExtended) {
            aParameters.clear();
        } else {
            aComment = null;
            // 读取模拟盒信息
            Object tLattice = aParameters.remove("Lattice");
            if (tLattice == null) {
                tLattice = aParameters.remove("lattice");
            }
            if (tLattice instanceof String) {
                Vector tVec = UT.Text.str2data((String)tLattice, 9);
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
                        aProperties.put(tKey, tCols==1 ? new String[aAtomNum] : new String[aAtomNum][tCols]);
                        break;
                    }
                    case "R": {
                        aProperties.put(tKey, tCols==1 ? Vectors.zeros(aAtomNum) : RowMatrix.zeros(aAtomNum, tCols));
                        break;
                    }
                    case "I": {
                        aProperties.put(tKey, tCols==1 ? IntVector.zeros(aAtomNum) : RowIntMatrix.zeros(aAtomNum, tCols));
                        break;
                    }}
                }
            }
        }
        if (aProperties == null) {
            // 即使是非 Extended，也需要有 Properties 以及 Box
            aProperties = new LinkedHashMap<>();
            aProperties.put("species", new String[aAtomNum]);
            aProperties.put("pos", RowMatrix.zeros(aAtomNum, ATOM_DATA_KEYS_XYZ.length));
        }
        // 简单遍历后续数据
        for (int i = 0; i < aAtomNum; ++i) {
            tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
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
                    ((IVector)tValue).set(i, Double.parseDouble(tTokens[j]));
                    ++j;
                } else
                if (tValue instanceof IMatrix) {
                    IVector tRow = ((IMatrix)tValue).row(i);
                    for (int k = 0; k < tRow.size(); ++k) {
                        tRow.set(k, Double.parseDouble(tTokens[j]));
                        ++j;
                    }
                } else
                if (tValue instanceof IIntVector) {
                    ((IIntVector)tValue).set(i, Integer.parseInt(tTokens[j]));
                    ++j;
                } else
                if (tValue instanceof IIntMatrix) {
                    IIntVector tRow = ((IIntMatrix)tValue).row(i);
                    for (int k = 0; k < tRow.size(); ++k) {
                        tRow.set(k, Integer.parseInt(tTokens[j]));
                        ++j;
                    }
                }
            }
        }
        // 返回 XYZ
        return new DataXYZ(aAtomNum, aComment, aParameters, aProperties, aBox);
    }
    static boolean parseParameters_(String aComment, Map<String, Object> rParameters) {
        aComment = aComment.trim();
        // 这个操作比较复杂，还需要处理双引号的情况
        final int tLen = aComment.length();
        int tKeyBegin = 0;
        while (tKeyBegin < tLen) {
            int tKeyEnd = aComment.indexOf('=', tKeyBegin);
            if (tKeyEnd < 0) return false;
            int tValueBegin = tKeyEnd+1;
            boolean tHasQuote = aComment.charAt(tValueBegin)=='"';
            int tValueEnd;
            if (tHasQuote) {
                ++tValueBegin;
                tValueEnd = aComment.indexOf('"', tValueBegin);
            } else {
                tValueEnd = UT.Text.findBlankIndex(aComment, tValueBegin);
            }
            if (tValueEnd < 0) return false;
            Object tValue = aComment.substring(tValueBegin, tValueEnd);
            if (tValue.equals("T")) {
                tValue = true;
            } else
            if (tValue.equals("F")) {
                tValue = false;
            } else {
                Number tNumberValue = UT.Text.str2number((String)tValue);
                if (tNumberValue != null) tValue = tNumberValue;
            }
            rParameters.put(aComment.substring(tKeyBegin, tKeyEnd), tValue);
            if (tHasQuote) ++tValueEnd;
            tKeyBegin = UT.Text.findNoBlankIndex(aComment, tValueEnd);
            if (tKeyBegin < 0) break; // 注意这种情况是已经结束了
        }
        // 这里削弱要求，不一定需要特定的参数名称
        return true;
    }
    
    
    /**
     * 输出成标准的 xyz 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {write_(tWriteln);}}
    /** 改为 {@link UT.IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用 */
    void write_(UT.IO.IWriteln aWriteln) throws IOException {
        aWriteln.writeln(String.valueOf(mAtomNum));
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
                    rLine.append(((IMatrix)tValue).columnNumber());
                } else
                if (tValue instanceof IIntVector) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":I:1");
                } else
                if (tValue instanceof IIntMatrix) {
                    rLine.append(tEntry.getKey());
                    rLine.append(":I:");
                    rLine.append(((IIntMatrix)tValue).columnNumber());
                }
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
                    boolean tHasBlank = UT.Text.findBlankIndex(tValueStr, 0)>=0;
                    if (tHasBlank) rLine.append("\"");
                    rLine.append(tValueStr);
                    if (tHasBlank) rLine.append("\"");
                }
            }
            aWriteln.writeln(rLine.toString());
        }
        for (int i = 0; i < mAtomNum; ++i) {
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
                }
            }
            aWriteln.writeln(String.join(" ", rLine));
        }
    }
}
