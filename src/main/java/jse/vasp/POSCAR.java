package jse.vasp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jse.atom.*;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static jse.code.CS.MASS;
import static jse.code.CS.ZL_STR;

/**
 * @author liqa
 * <p> vasp 读取的原子位置格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持边界条件设置 </p>
 */
public class POSCAR extends AbstractSettableAtomData implements IVaspCommonData {
    public final static String DEFAULT_COMMENT = "VASP_POSCAR_FROM_JSE";
    
    /** POSCAR 只存储每个原子的 xyz 缩放后的矢量 */
    private final IMatrix mDirect;
    private boolean mIsCartesian; // 如果是 Cartesian 则是没有进行缩放的
    /** POSCAR 特有的属性，系统名称以及每个种类的原子名称 */
    private @Nullable String mComment;
    private String @Nullable[] mTypeNames;
    private IIntVector mAtomNumbers;
    /** POSCAR 使用晶格矢量组成的矩阵以及对应的晶格常数来作为边界，现在这里直接转为内置的 Box 来存储 */
    private VaspBox mBox;
    /** 是否有 Selective dynamics 关键字 */
    private boolean mSelectiveDynamics;
    
    /** 用于标记此 POSCAR 是否是来源于 XDATCAR，从而不能进行激进的修改 */
    private final boolean mIsRef;
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final @NotNull Multimap<String, Integer> mKey2Type;
    
    POSCAR(@Nullable String aComment, VaspBox aBox, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, boolean aSelectiveDynamics, IMatrix aDirect, boolean aIsCartesian, boolean aIsRef) {
        mDirect = aDirect;
        mIsCartesian = aIsCartesian;
        mComment = aComment;
        mTypeNames = aTypeNames;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mSelectiveDynamics = aSelectiveDynamics;
        
        mKey2Type = ArrayListMultimap.create();
        if (mTypeNames != null) {
            int rType = 0;
            for (String tKey : mTypeNames) {
                ++rType;
                mKey2Type.put(tKey, rType);
            }
        }
        mIsRef = aIsRef;
    }
    POSCAR(@Nullable String aComment, VaspBox aBox, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, boolean aSelectiveDynamics, IMatrix aDirect, boolean aIsCartesian) {
        this(aComment, aBox, aTypeNames, aAtomNumbers, aSelectiveDynamics, aDirect, aIsCartesian, false);
    }
    /** 用于方便构建，减少重复代码 */
    POSCAR(IVaspCommonData aVaspCommonData, boolean aSelectiveDynamics, IMatrix aDirect) {
        this(aVaspCommonData.comment(), aVaspCommonData.box(), aVaspCommonData.typeNames(), aVaspCommonData.atomNumbers(), aSelectiveDynamics, aDirect, aVaspCommonData.isCartesian(), true);
    }
    
    static String @Nullable[] copyTypeNames(String @Nullable[] aTypeNames) {
        return (aTypeNames==null || aTypeNames.length==0) ? null : Arrays.copyOf(aTypeNames, aTypeNames.length);
    }
    
    /// 获取属性
    @Override public boolean hasSymbol() {return mTypeNames!=null;}
    @Override public @Nullable String symbol(int aType) {return mTypeNames==null ? null : mTypeNames[aType-1];}
    @Override public boolean hasMass() {return hasSymbol();}
    @Override public double mass(int aType) {
        @Nullable String tSymbol = symbol(aType);
        return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
    }
    /** @deprecated use {@link #symbol} */
    @Deprecated public @Nullable String typeName(int aType) {return symbol(aType);}
    public int atomNumber(String aType) {
        int rAtomNum = 0;
        for (int tType : mKey2Type.get(aType)) rAtomNum += atomNumber(tType);
        return rAtomNum;
    }
    public int atomNumber(int aType) {return mAtomNumbers.get(aType-1);}
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final int atomNum(String aType) {return atomNumber(aType);}
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final int atomNum(int aType) {return atomNumber(aType);}
    /** 提供简写版本 */
    @VisibleForTesting public final int natoms(String aType) {return atomNumber(aType);}
    @VisibleForTesting public final int natoms(int aType) {return atomNumber(aType);}
    
    public @Override @Nullable String comment() {return mComment;}
    public @Override String @Nullable[] typeNames() {return mTypeNames;}
    public @Override IIntVector atomNumbers() {return mAtomNumbers;}
    public IMatrix direct() {return mDirect;}
    public @Override boolean isCartesian() {return mIsCartesian;}
    public boolean isSelectiveDynamics() {return mSelectiveDynamics;}
    public POSCAR setSelectiveDynamics(boolean aSelectiveDynamics) {mSelectiveDynamics = aSelectiveDynamics; return this;}
    
    /** @deprecated use {@link #box} */ @Deprecated public VaspBox vaspBox() {return box();}
    /** @deprecated use {@link VaspBox#scale} */ @Deprecated public double vaspBoxScale() {return mBox.scale();}
    /** @deprecated use {@code !}{@link #isPrism} */ @Deprecated public boolean isDiagBox() {return !isPrism();}
    
    /** boxScale stuffs */
    public double boxScale() {return mBox.scale();}
    public POSCAR setBoxScale(double aBoxScale) {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        mBox.setScale(aBoxScale);
        return this;
    }
    /** Groovy stuffs */
    @VisibleForTesting public double getBoxScale() {return mBox.getScale();}
    
    
    /** 支持直接修改 TypeNames，只会增大种类数，不会减少 */
    @Override public POSCAR setSymbols(String... aTypeNames) {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        if (aTypeNames==null || aTypeNames.length==0) {
            mTypeNames = null;
            validKey2Type_();
            return this;
        }
        if (mTypeNames==null || aTypeNames.length>mTypeNames.length) mTypeNames = Arrays.copyOf(aTypeNames, aTypeNames.length);
        else System.arraycopy(aTypeNames, 0, mTypeNames, 0, aTypeNames.length);
        if (aTypeNames.length > mAtomNumbers.size()) {
            IIntVector oAtomNumbers = mAtomNumbers;
            mAtomNumbers = IntVector.zeros(aTypeNames.length);
            mAtomNumbers.subVec(0, oAtomNumbers.size()).fill(oAtomNumbers);
        }
        validKey2Type_();
        return this;
    }
    @Override public POSCAR setNoSymbol() {return setSymbols(ZL_STR);}
    /** 设置原子种类数目 */
    @Override public POSCAR setAtomTypeNumber(int aAtomTypeNum) {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        int oTypeNum = mAtomNumbers.size();
        if (aAtomTypeNum == oTypeNum) return this;
        if (aAtomTypeNum < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            mAtomNumbers.set(aAtomTypeNum-1, mAtomNumbers.subVec(aAtomTypeNum-1, mAtomNumbers.size()).sum());
            mAtomNumbers = mAtomNumbers.subVec(0, aAtomTypeNum).copy();
            validKey2Type_();
            return this;
        }
        if (mTypeNames!=null && mTypeNames.length<aAtomTypeNum) {
            String[] rTypeNames = new String[aAtomTypeNum];
            System.arraycopy(mTypeNames, 0, rTypeNames, 0, mTypeNames.length);
            for (int tType = mTypeNames.length+1; tType <= aAtomTypeNum; ++tType) rTypeNames[tType-1] = "T" + tType;
            mTypeNames = rTypeNames;
        }
        IIntVector oAtomNumbers = mAtomNumbers;
        mAtomNumbers = IntVector.zeros(aAtomTypeNum);
        mAtomNumbers.subVec(0, oTypeNum).fill(oAtomNumbers);
        validKey2Type_();
        return this;
    }
    
    void validKey2Type_() {
        mKey2Type.clear();
        if (mTypeNames != null) {
            int rType = 0;
            for (String tKey : mTypeNames) {
                ++rType;
                mKey2Type.put(tKey, rType);
            }
        }
    }
    /** @deprecated use {@link #setSymbols} */ @Deprecated public POSCAR setTypeNames(String... aSymbols) {return setSymbols(aSymbols);}
    /** @deprecated use {@link #setNoSymbol} */ @Deprecated public POSCAR setNoTypeName() {return setNoSymbol();}
    
    public POSCAR setComment(@Nullable String aComment) {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        mComment = aComment;
        return this;
    }
    
    /** Groovy stuffs */
    @VisibleForTesting public String @Nullable[] getTypeNames() {return mTypeNames;}
    @VisibleForTesting public @Nullable String getComment() {return mComment;}
    
    
    /** Cartesian 和 Direct 来回转换 */
    public POSCAR setCartesian() {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        if (mIsCartesian) return this;
        // 这里绕过 scale 直接处理
        if (isPrism()) {
            IMatrix tIABC = mBox.iabc();
            mDirect.operation().matmul2this(tIABC);
            // cartesian 其实也需要考虑计算误差带来的出边界的问题（当然此时在另一端的就不好修复了）
            final double tNorm = tIABC.asVecCol().operation().stat((norm, v) -> norm+Math.abs(v));
            mDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON*tNorm ? 0.0 : v);
        } else {
            mDirect.col(0).multiply2this(mBox.iax());
            mDirect.col(1).multiply2this(mBox.iby());
            mDirect.col(2).multiply2this(mBox.icz());
        }
        mIsCartesian = true;
        return this;
    }
    public POSCAR setDirect() {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        if (!mIsCartesian) return this;
        // 这里绕过 scale 直接处理
        if (isPrism()) {
            mDirect.operation().matmul2this(mBox.inviabc());
        } else {
            mDirect.col(0).div2this(mBox.iax());
            mDirect.col(1).div2this(mBox.iby());
            mDirect.col(2).div2this(mBox.icz());
        }
        // direct 现在无论任何情况都会自动靠近所有接近的整数值
        mDirect.operation().map2this(v -> {
            if (Math.abs(v) < MathEX.Code.DBL_EPSILON) return 0.0;
            int tIntV = MathEX.Code.round2int(v);
            if (MathEX.Code.numericEqual(v, tIntV)) return tIntV;
            return v;
        });
        mIsCartesian = false;
        return this;
    }
    
    /** 修改模拟盒类型 */
    public POSCAR setBoxNormal() {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        if (!isPrism()) return this;
        VaspBox oBox = mBox;
        mBox = new VaspBox(mBox);
        // 如果是 direct 则不用转换数据
        if (!mIsCartesian) return this;
        // 如果原本的斜方模拟盒不存在斜方数据则直接返回
        if (MathEX.Code.numericEqual(oBox.iay(), 0.0) && MathEX.Code.numericEqual(oBox.iaz(), 0.0)
         && MathEX.Code.numericEqual(oBox.ibx(), 0.0) && MathEX.Code.numericEqual(oBox.ibz(), 0.0)
         && MathEX.Code.numericEqual(oBox.icx(), 0.0) && MathEX.Code.numericEqual(oBox.icy(), 0.0)) return this;
        // 否则将原子进行线性变换，这里绕过 scale 直接处理
        mDirect.operation().matmul2this(oBox.inviabc());
        // 考虑计算误差带来的出边界的问题，现在会自动靠近所有接近的整数值
        mDirect.operation().map2this(v -> {
            if (Math.abs(v) < MathEX.Code.DBL_EPSILON) return 0.0;
            int tIntV = MathEX.Code.round2int(v);
            if (MathEX.Code.numericEqual(v, tIntV)) return tIntV;
            return v;
        });
        // 手动转换回到 cartesian
        mDirect.col(0).multiply2this(mBox.iax());
        mDirect.col(1).multiply2this(mBox.iby());
        mDirect.col(2).multiply2this(mBox.icz());
        return this;
    }
    public POSCAR setBoxPrism() {
        if (isPrism()) return this;
        return setBoxPrism(0.0, 0.0, 0.0);
    }
    public POSCAR setBoxPrism(double aIXY, double aIXZ, double aIYZ) {return setBoxPrism(0.0, 0.0, aIXY, 0.0, aIXZ, aIYZ);}
    public POSCAR setBoxPrism(double aIAy, double aIAz, double aIBx, double aIBz, double aICx, double aICy) {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        VaspBox oBox = mBox;
        mBox = new VaspBoxPrism(mBox, aIAy, aIAz, aIBx, aIBz, aICx, aICy);
        // 如果是 direct 则不用转换数据
        if (!mIsCartesian) return this;
        // 现在必须要求倾斜因子相同才可以跳过设置
        if (MathEX.Code.numericEqual(oBox.iay(), aIAy) && MathEX.Code.numericEqual(oBox.iaz(), aIAz)
         && MathEX.Code.numericEqual(oBox.ibx(), aIBx) && MathEX.Code.numericEqual(oBox.ibz(), aIBz)
         && MathEX.Code.numericEqual(oBox.icx(), aICx) && MathEX.Code.numericEqual(oBox.icy(), aICy)) return this;
        // 否则将原子进行线性变换，这里绕过 scale 直接处理
        if (oBox.isPrism()) {
            mDirect.operation().matmul2this(oBox.inviabc());
            // 由于后续存在处理，这里不处理计算误差带来的出边界的问题
        } else {
            mDirect.col(0).div2this(oBox.iax());
            mDirect.col(1).div2this(oBox.iby());
            mDirect.col(2).div2this(oBox.icz());
        }
        // 手动转换回到 cartesian
        IMatrix tIABC = mBox.iabc();
        mDirect.operation().matmul2this(tIABC);
        // cartesian 其实也需要考虑计算误差带来的出边界的问题
        final double tNorm = tIABC.asVecCol().operation().stat((norm, v) -> norm+Math.abs(v));
        mDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON*tNorm ? 0.0 : v);
        return this;
    }
    /** 调整模拟盒的 xyz 长度 */
    public POSCAR setBoxXYZ(double aIX, double aIY, double aIZ) {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        VaspBox oBox = mBox;
        mBox = oBox.isPrism() ?
            new VaspBoxPrism(aIX, oBox.iay(), oBox.iaz(), oBox.ibx(), aIY, oBox.ibz(), oBox.icx(), oBox.icy(), aIZ, oBox.scale()) :
            new VaspBox(aIX, aIY, aIZ, oBox.scale());
        // 如果是 direct 则不用转换数据
        if (!mIsCartesian) return this;
        // 现在必须要求 xyz 长度相同才可以跳过设置
        if (MathEX.Code.numericEqual(oBox.iax(), aIX) && MathEX.Code.numericEqual(oBox.iby(), aIY) && MathEX.Code.numericEqual(oBox.icz(), aIZ)) return this;
        // 否则将原子进行线性变换，这里绕过 scale 直接处理
        if (oBox.isPrism()) {
            mDirect.operation().matmul2this(oBox.inviabc());
            // 由于后续存在处理，这里不处理计算误差带来的出边界的问题
            // 手动转换回到 cartesian
            IMatrix tIABC = mBox.iabc();
            mDirect.operation().matmul2this(tIABC);
            // cartesian 其实也需要考虑计算误差带来的出边界的问题
            final double tNorm = tIABC.asVecCol().operation().stat((norm, v) -> norm+Math.abs(v));
            mDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON*tNorm ? 0.0 : v);
        } else {
            mDirect.col(0).div2this(oBox.iax());
            mDirect.col(1).div2this(oBox.iby());
            mDirect.col(2).div2this(oBox.icz());
            // 手动转换回到 cartesian
            mDirect.col(0).multiply2this(mBox.iax());
            mDirect.col(1).multiply2this(mBox.iby());
            mDirect.col(2).multiply2this(mBox.icz());
        }
        return this;
    }
    
    /** 密度归一化 */
    public POSCAR setDenseNormalized() {
        if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        double tScale = MathEX.Fast.cbrt(volume() / atomNumber());
        // 直接通过调整 boxScale 来实现
        mBox.setScale(mBox.scale() / tScale);
        return this;
    }
    
    /** AbstractAtomData stuffs */
    @Override public ISettableAtom atom(final int aIdx) {
        return new AbstractSettableAtom_() {
            private int mIdx = aIdx;
            /** poscar 的 atom 在 setType 后会改变位置 */
            @Override public int index() {return mIdx;}
            @Override public double x() {
                if (mIsCartesian) {
                    return mDirect.get(mIdx, 0)*mBox.scale();
                } else
                if (!isPrism()) {
                    return mBox.x()*mDirect.get(mIdx, 0);
                } else {
                    return (mBox.iax()*mDirect.get(mIdx, 0) + mBox.ibx()*mDirect.get(mIdx, 1) + mBox.icx()*mDirect.get(mIdx, 2))*mBox.scale();
                }
            }
            @Override public double y() {
                if (mIsCartesian) {
                    return mDirect.get(mIdx, 1)*mBox.scale();
                } else
                if (!isPrism()) {
                    return mBox.y()*mDirect.get(mIdx, 1);
                } else {
                    return (mBox.iay()*mDirect.get(mIdx, 0) + mBox.iby()*mDirect.get(mIdx, 1) + mBox.icy()*mDirect.get(mIdx, 2))*mBox.scale();
                }
            }
            @Override public double z() {
                if (mIsCartesian) {
                    return mDirect.get(mIdx, 2)*mBox.scale();
                } else
                if (!isPrism()) {
                    return mBox.z()*mDirect.get(mIdx, 2);
                } else {
                    return (mBox.iaz()*mDirect.get(mIdx, 0) + mBox.ibz()*mDirect.get(mIdx, 1) + mBox.icz()*mDirect.get(mIdx, 2))*mBox.scale();
                }
            }
            /** type 直接遍历获取即可，根据名称顺序给 type 的数字 */
            @Override protected int type_() {
                int rType = 0;
                int rNumber = 0;
                final int tAtomTypeNum = mAtomNumbers.size();
                for (int i = 0; i < tAtomTypeNum; ++i) {
                    rNumber += mAtomNumbers.get(i);
                    ++rType;
                    if (rNumber > mIdx) return rType;
                }
                throw new RuntimeException();
            }
            
            private final XYZ mBuf = new XYZ();
            @Override protected void setX_(double aX) {
                if (mIsCartesian) {
                    mDirect.set(mIdx, 0, aX/mBox.scale());
                } else
                if (!isPrism()) {
                    mDirect.set(mIdx, 0, aX/mBox.x());
                } else {
                    // 这种情况下性能开销较大，因此需要 setXYZ 这种方法
                    mBuf.setXYZ(aX, y(), z());
                    mBox.toDirect(mBuf);
                    mDirect.set(mIdx, 0, mBuf.mX);
                    mDirect.set(mIdx, 1, mBuf.mY);
                    mDirect.set(mIdx, 2, mBuf.mZ);
                }
            }
            @Override protected void setY_(double aY) {
                if (mIsCartesian) {
                    mDirect.set(mIdx, 1, aY/mBox.scale());
                } else
                if (!isPrism()) {
                    mDirect.set(mIdx, 1, aY/mBox.y());
                } else {
                    // 这种情况下性能开销较大，因此需要 setXYZ 这种方法
                    mBuf.setXYZ(x(), aY, z());
                    mBox.toDirect(mBuf);
                    mDirect.set(mIdx, 0, mBuf.mX);
                    mDirect.set(mIdx, 1, mBuf.mY);
                    mDirect.set(mIdx, 2, mBuf.mZ);
                }
            }
            @Override protected void setZ_(double aZ) {
                if (mIsCartesian) {
                    mDirect.set(mIdx, 2, aZ/mBox.scale());
                } else
                if (!isPrism()) {
                    mDirect.set(mIdx, 2, aZ/mBox.z());
                } else {
                    // 这种情况下性能开销较大，因此需要 setXYZ 这种方法
                    mBuf.setXYZ(x(), y(), aZ);
                    mBox.toDirect(mBuf);
                    mDirect.set(mIdx, 0, mBuf.mX);
                    mDirect.set(mIdx, 1, mBuf.mY);
                    mDirect.set(mIdx, 2, mBuf.mZ);
                }
            }
            @Override public ISettableAtom setXYZ(double aX, double aY, double aZ) {
                if (mIsCartesian) {
                    mDirect.set(mIdx, 0, aX/mBox.scale());
                    mDirect.set(mIdx, 1, aY/mBox.scale());
                    mDirect.set(mIdx, 2, aZ/mBox.scale());
                } else
                if (!isPrism()) {
                    mDirect.set(mIdx, 0, aX/mBox.x());
                    mDirect.set(mIdx, 1, aY/mBox.y());
                    mDirect.set(mIdx, 2, aZ/mBox.z());
                } else {
                    // 此时 setXYZ 反而性能开销更小
                    mBuf.setXYZ(aX, aY, aZ);
                    mBox.toDirect(mBuf);
                    mDirect.set(mIdx, 0, mBuf.mX);
                    mDirect.set(mIdx, 1, mBuf.mY);
                    mDirect.set(mIdx, 2, mBuf.mZ);
                }
                return this;
            }
            /** poscar 的 atom 在 setType 后会改变位置，并且也会影响其他原子的位置，这里只同步当前原子的位置 */
            @Override protected void setType_(int aType) {
                if (mIsRef) throw new UnsupportedOperationException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
                int oType = type();
                if (oType == aType) return;
                //noinspection IfStatementWithIdenticalBranches
                if (oType < aType) {
                    // 增大 type 的情况，这里使用高效的方式，
                    // 将所有中间的边界处原子都向上跳跃到正确位置，
                    // 然后将此原子移动到新的间隙中
                    double tX = mDirect.get(mIdx, 0);
                    double tY = mDirect.get(mIdx, 1);
                    double tZ = mDirect.get(mIdx, 2);
                    int from = -1;
                    for (int typeMM = 0; typeMM < oType-1; ++typeMM) {
                        from += mAtomNumbers.get(typeMM);
                    }
                    int to = mIdx;
                    for (int typeMM = oType-1; typeMM < aType-1; ++typeMM) {
                        from += mAtomNumbers.get(typeMM);
                        mDirect.set(to, 0, mDirect.get(from, 0));
                        mDirect.set(to, 1, mDirect.get(from, 1));
                        mDirect.set(to, 2, mDirect.get(from, 2));
                        to = from;
                    }
                    // 更新 mIdx
                    mIdx = from;
                    mDirect.set(mIdx, 0, tX);
                    mDirect.set(mIdx, 1, tY);
                    mDirect.set(mIdx, 2, tZ);
                    // 更新 type 计数
                    mAtomNumbers.decrement(oType-1);
                    mAtomNumbers.increment(aType-1);
                } else {
                    // 减小 type 的情况，这里简单处理，
                    // 将所有中间的边界处原子都向下跳跃到正确位置，
                    // 然后将此原子移动到新的间隙中
                    double tX = mDirect.get(mIdx, 0);
                    double tY = mDirect.get(mIdx, 1);
                    double tZ = mDirect.get(mIdx, 2);
                    int from = atomNumber();
                    for (int typeMM = mAtomNumbers.size()-1; typeMM >= oType; --typeMM) {
                        from -= mAtomNumbers.get(typeMM);
                    }
                    int to = mIdx;
                    for (int typeMM = oType-1; typeMM >= aType; --typeMM) {
                        from -= mAtomNumbers.get(typeMM);
                        mDirect.set(to, 0, mDirect.get(from, 0));
                        mDirect.set(to, 1, mDirect.get(from, 1));
                        mDirect.set(to, 2, mDirect.get(from, 2));
                        to = from;
                    }
                    // 更新 mIdx
                    mIdx = from;
                    mDirect.set(mIdx, 0, tX);
                    mDirect.set(mIdx, 1, tY);
                    mDirect.set(mIdx, 2, tZ);
                    // 更新 type 计数
                    mAtomNumbers.decrement(oType-1);
                    mAtomNumbers.increment(aType-1);
                }
            }
        };
    }
    @Override public VaspBox box() {return mBox;}
    @Override public int atomNumber() {return mDirect.rowNumber();}
    @Override public int atomTypeNumber() {return mAtomNumbers.size();}
    
    
    /** 拷贝一份 POSCAR */
    @Override public POSCAR copy() {return new POSCAR(mComment, mBox.copy(), copyTypeNames(mTypeNames), mAtomNumbers.copy(), mSelectiveDynamics, mDirect.copy(), mIsCartesian);}
    // 由于 POSCAR 不是全都可以修改，因此不重写另外两个
    
    /** 从 IAtomData 来创建，POSCAR 需要额外的原子种类字符串以及额外的是否开启 SelectiveDynamics */
    public static POSCAR fromAtomData(IAtomData aAtomData) {
        @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
        return fromAtomData(aAtomData, tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    public static POSCAR fromAtomData(IAtomData aAtomData, String... aTypeNames) {return fromAtomData(aAtomData, (aAtomData instanceof POSCAR) && ((POSCAR)aAtomData).mSelectiveDynamics, aTypeNames);}
    public static POSCAR fromAtomData(IAtomData aAtomData, boolean aSelectiveDynamics, String... aTypeNames) {
        if (aTypeNames == null) aTypeNames = ZL_STR;
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            return ((POSCAR)aAtomData).copy().setSelectiveDynamics(aSelectiveDynamics).setSymbols(aTypeNames);
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
            int tAtomTypeNum = Math.max(aAtomData.atomTypeNumber(), aTypeNames.length);
            IIntVector rAtomNumbers = IntVector.zeros(tAtomTypeNum);
            IMatrix rDirect = Matrices.zeros(aAtomData.atomNumber(), 3);
            int tIdx = 0;
            for (int tTypeMM = 0; tTypeMM < tAtomTypeNum; ++tTypeMM) {
                for (IAtom tAtom : aAtomData.atoms()) if (tAtom.type() == tTypeMM+1) {
                    rAtomNumbers.increment(tTypeMM);
                    rDirect.set(tIdx, 0, tAtom.x());
                    rDirect.set(tIdx, 1, tAtom.y());
                    rDirect.set(tIdx, 2, tAtom.z());
                    ++tIdx;
                }
            }
            // 现在转换会直接转成 Cartesian 来避免计算中的浮点误差
            IBox tBox = aAtomData.box();
            VaspBox rBox = tBox.isPrism() ?
                new VaspBoxPrism(tBox.ax(), tBox.ay(), tBox.az(), tBox.bx(), tBox.by(), tBox.bz(), tBox.cx(), tBox.cy(), tBox.cz()) :
                new VaspBox(tBox.x(), tBox.y(), tBox.z());
            if (aTypeNames.length!=0 && aTypeNames.length<tAtomTypeNum) {
                String[] rTypeNames = new String[tAtomTypeNum];
                System.arraycopy(aTypeNames, 0, rTypeNames, 0, aTypeNames.length);
                for (int tType = aTypeNames.length+1; tType <= tAtomTypeNum; ++tType) rTypeNames[tType-1] = "T" + tType;
                aTypeNames = rTypeNames;
            }
            return new POSCAR(null, rBox, copyTypeNames(aTypeNames), rAtomNumbers, aSelectiveDynamics, rDirect, true);
        }
    }
    public static POSCAR fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {return fromAtomData(aAtomData, UT.Text.toArray(aTypeNames));}
    public static POSCAR fromAtomData(IAtomData aAtomData, boolean aSelectiveDynamics, Collection<? extends CharSequence> aTypeNames) {return fromAtomData(aAtomData, aSelectiveDynamics, UT.Text.toArray(aTypeNames));}
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static POSCAR of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static POSCAR of(IAtomData aAtomData, String... aTypeNames) {return fromAtomData(aAtomData, aTypeNames);}
    public static POSCAR of(IAtomData aAtomData, boolean aSelectiveDynamics, String... aTypeNames) {return fromAtomData(aAtomData, aSelectiveDynamics, aTypeNames);}
    public static POSCAR of(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {return fromAtomData(aAtomData, aTypeNames);}
    public static POSCAR of(IAtomData aAtomData, boolean aSelectiveDynamics, Collection<? extends CharSequence> aTypeNames) {return fromAtomData(aAtomData, aSelectiveDynamics, aTypeNames);}
    
    
    /// 文件读写
    /**
     * 从 vasp 输出的 POSCAR 文件中读取来实现初始化
     * @param aFilePath vasp 输出的 POSCAR 文件路径
     * @return 读取得到的 POSCAR 对象，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static POSCAR read(String aFilePath) throws IOException {try (BufferedReader tReader = UT.IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static POSCAR read_(BufferedReader aReader) throws IOException {
        String tLine;
        String[] tTokens;
        
        String aComment;
        VaspBox aBox;
        IVector aBoxA, aBoxB, aBoxC;
        double aBoxScale;
        String[] aTypeNames;
        IIntVector aAtomNumbers;
        boolean aSelectiveDynamics = false;
        IMatrix aDirect;
        boolean aIsCartesian;
        
        // 第一行为 Comment
        tLine = aReader.readLine();
        aComment = tLine;
        // 读取模拟盒信息
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        aBoxScale = Double.parseDouble(tTokens[0]);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBoxA = UT.Text.str2data(tLine, 3);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBoxB = UT.Text.str2data(tLine, 3);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBoxC = UT.Text.str2data(tLine, 3);
        // 读取原子种类（可选）和对应数目的信息
        boolean tNoAtomType = false;
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        aTypeNames = tTokens;
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        try {
        final String[] fTokens = tTokens;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        } catch (Exception e) {
        tNoAtomType = true;
        final String[] fTokens = aTypeNames;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        aTypeNames = null;
        }
        if (!tNoAtomType) {
        tLine = aReader.readLine(); if (tLine == null) return null;
        }
        // 可选的注释行
        if (tLine.equalsIgnoreCase("Selective dynamics")) {
        aSelectiveDynamics = true; tLine = aReader.readLine(); if (tLine == null) return null;
        }
        // 只支持 Direct 和 Cartesian，改为 contains 从而可以支持直接读取 XDATCAR
        aIsCartesian = UT.Text.containsIgnoreCase(tLine, "Cartesian");
        if (!aIsCartesian && !UT.Text.containsIgnoreCase(tLine, "Direct")) {
        throw new RuntimeException("Can ONLY read Direct or Cartesian POSCAR");
        }
        // 读取原子数据
        int tAtomNum = aAtomNumbers.sum();
        aDirect = RowMatrix.zeros(tAtomNum, 3);
        for (IVector tRow : aDirect.rows()) {
            tLine = aReader.readLine();
            if (tLine == null) return null;
            tRow.fill(UT.Text.str2data(tLine, 3));
        }
        
        // 判断是否是 prism 并据此创建 VaspBox
        boolean tNotPrism =
               MathEX.Code.numericEqual(aBoxA.get(1), 0.0) && MathEX.Code.numericEqual(aBoxA.get(2), 0.0)
            && MathEX.Code.numericEqual(aBoxB.get(0), 0.0) && MathEX.Code.numericEqual(aBoxB.get(2), 0.0)
            && MathEX.Code.numericEqual(aBoxC.get(0), 0.0) && MathEX.Code.numericEqual(aBoxC.get(1), 0.0)
            ;
        aBox = tNotPrism ?
            new VaspBox(aBoxA.get(0), aBoxB.get(1), aBoxC.get(2), aBoxScale) :
            new VaspBoxPrism(
                aBoxA.get(0), aBoxA.get(1), aBoxA.get(2),
                aBoxB.get(0), aBoxB.get(1), aBoxB.get(2),
                aBoxC.get(0), aBoxC.get(1), aBoxC.get(2),
                aBoxScale
            );
        // 返回 POSCAR
        return new POSCAR(aComment, aBox, aTypeNames, aAtomNumbers, aSelectiveDynamics, aDirect, aIsCartesian);
    }
    
    /**
     * 输出成 vasp 能够读取的 POSCAR 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {write_(tWriteln);}}
    /** 改为 {@link UT.IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用 */
    void write_(UT.IO.IWriteln aWriteln) throws IOException {
        aWriteln.writeln(mComment==null ? DEFAULT_COMMENT : mComment);
        aWriteln.writeln(String.valueOf(mBox.scale()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.iax(), mBox.iay(), mBox.iaz()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.ibx(), mBox.iby(), mBox.ibz()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.icx(), mBox.icy(), mBox.icz()));
        if (mTypeNames!=null && mTypeNames.length!=0) {
        aWriteln.writeln(String.join(" ", AbstractCollections.slice(AbstractCollections.map(mTypeNames, type -> String.format("%6s", type)), AbstractCollections.range(mAtomNumbers.size()))));
        }
        aWriteln.writeln(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number))));
        if (mSelectiveDynamics) {
        aWriteln.writeln("Selective dynamics");
        }
        aWriteln.writeln(mIsCartesian ? "Cartesian" : "Direct");
        for (IVector subDirect : mDirect.rows()) {
        aWriteln.writeln(String.format("%24.18g  %24.18g  %24.18g", subDirect.get(0), subDirect.get(1), subDirect.get(2)));
        }
    }
}
