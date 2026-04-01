package jse.vasp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jse.atom.*;
import jse.code.Conf;
import jse.code.FileEndException;
import jse.code.IO;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static jse.code.CS.*;

/**
 * vasp 读取的原子位置格式，保留 poscar
 * 原本的格式，以及记录 Cartesian 属性
 * <p>
 * 目前支持种类的修改，但是通过调整原子的坐标存储位置来实现，
 * 因此遍历修改是未定义行为，例如：
 * <pre> {@code
 * poscar.op().mapType2this {...}
 * } </pre>
 * 操作是非法的，应当使用：
 * <pre> {@code
 * def data = poscar.op().mapType {...}
 * } </pre>
 * 或者转为 {@link jse.lmp.Lmpdat}
 *
 * @author liqa
 */
public class POSCAR extends AbstractSettableAtomData {
    public final static String DEFAULT_COMMENT = "VASP_POSCAR_FROM_JSE";
    
    /** POSCAR 只存储每个原子的 xyz 缩放后的矢量 */
    private final IMatrix mDirect;
    private boolean mIsCartesian; // 如果是 Cartesian 则是没有进行缩放的
    /** POSCAR 特有的属性，系统名称以及每个种类的原子名称 */
    private @Nullable String mComment;
    private String @Nullable[] mTypeNames;
    private IIntVector mNumAtomsVec;
    /** POSCAR 使用晶格矢量组成的矩阵以及对应的晶格常数来作为边界，现在这里直接转为内置的 Box 来存储 */
    private VaspBox mBox;
    /** 是否有 Selective dynamics 关键字 */
    private boolean mSelectiveDynamics;
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final @NotNull Multimap<String, Integer> mKey2Type;
    
    POSCAR(@Nullable String aComment, VaspBox aBox, String @Nullable[] aTypeNames, IIntVector aNumAtomsVec, boolean aSelectiveDynamics, IMatrix aDirect, boolean aIsCartesian) {
        mDirect = aDirect;
        mIsCartesian = aIsCartesian;
        mComment = aComment;
        mTypeNames = aTypeNames;
        mNumAtomsVec = aNumAtomsVec;
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
    /** @deprecated use {@link #symbol(int)} */
    @Deprecated public @Nullable String typeName(int aType) {return symbol(aType);}
    public int natoms(String aType) {
        int rNumAtoms = 0;
        for (int tType : mKey2Type.get(aType)) rNumAtoms += natoms(tType);
        return rNumAtoms;
    }
    public int natoms(int aType) {return mNumAtomsVec.get(aType-1);}
    
    public @Nullable String comment() {return mComment;}
    public String @Nullable[] typeNames() {return mTypeNames;}
    public IMatrix direct() {return mDirect;}
    public boolean isCartesian() {return mIsCartesian;}
    public boolean isSelectiveDynamics() {return mSelectiveDynamics;}
    public POSCAR setSelectiveDynamics(boolean aSelectiveDynamics) {mSelectiveDynamics = aSelectiveDynamics; return this;}
    
    
    /** 支持直接修改 TypeNames，只会增大种类数，不会减少 */
    @Override public POSCAR setSymbols(String... aTypeNames) {
        if (aTypeNames==null || aTypeNames.length==0) {
            mTypeNames = null;
            validKey2Type_();
            return this;
        }
        if (mTypeNames==null || aTypeNames.length>mTypeNames.length) mTypeNames = Arrays.copyOf(aTypeNames, aTypeNames.length);
        else System.arraycopy(aTypeNames, 0, mTypeNames, 0, aTypeNames.length);
        if (aTypeNames.length > mNumAtomsVec.size()) {
            IIntVector oAtomNumbers = mNumAtomsVec;
            mNumAtomsVec = IntVector.zeros(aTypeNames.length);
            mNumAtomsVec.subVec(0, oAtomNumbers.size()).fill(oAtomNumbers);
        }
        validKey2Type_();
        return this;
    }
    @Override public POSCAR setNoSymbol() {return setSymbols(ZL_STR);}
    /** 设置原子种类数目 */
    @Override public POSCAR setNtypes(int aNumTypes) {
        int oNumTypes = mNumAtomsVec.size();
        if (aNumTypes == oNumTypes) return this;
        if (aNumTypes < oNumTypes) {
            // 现在支持设置更小的值，更大的种类会直接截断
            mNumAtomsVec.set(aNumTypes -1, mNumAtomsVec.subVec(aNumTypes -1, mNumAtomsVec.size()).sum());
            mNumAtomsVec = mNumAtomsVec.subVec(0, aNumTypes).copy();
            validKey2Type_();
            return this;
        }
        if (mTypeNames!=null && mTypeNames.length< aNumTypes) {
            String[] rTypeNames = new String[aNumTypes];
            System.arraycopy(mTypeNames, 0, rTypeNames, 0, mTypeNames.length);
            for (int tType = mTypeNames.length+1; tType <= aNumTypes; ++tType) rTypeNames[tType-1] = "T" + tType;
            mTypeNames = rTypeNames;
        }
        IIntVector oAtomNumbers = mNumAtomsVec;
        mNumAtomsVec = IntVector.zeros(aNumTypes);
        mNumAtomsVec.subVec(0, oNumTypes).fill(oAtomNumbers);
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
    /** @deprecated use {@link #setSymbols(String...)} */ @Deprecated public POSCAR setTypeNames(String... aSymbols) {return setSymbols(aSymbols);}
    /** @deprecated use {@link #setNoSymbol()} */ @Deprecated public POSCAR setNoTypeName() {return setNoSymbol();}
    
    public POSCAR setComment(@Nullable String aComment) {
        mComment = aComment;
        return this;
    }
    
    /** Groovy stuffs */
    @VisibleForTesting public String @Nullable[] getTypeNames() {return mTypeNames;}
    @VisibleForTesting public @Nullable String getComment() {return mComment;}
    
    
    /** Cartesian 和 Direct 来回转换 */
    public POSCAR setCartesian() {
        if (mIsCartesian) return this;
        // 这里绕过 scale 直接处理
        if (isPrism()) {
            IMatrix tIABC = mBox.iabc();
            mDirect.operation().matmul2this(tIABC);
        } else {
            mDirect.col(0).multiply2this(mBox.iax());
            mDirect.col(1).multiply2this(mBox.iby());
            mDirect.col(2).multiply2this(mBox.icz());
        }
        mIsCartesian = true;
        return this;
    }
    public POSCAR setDirect() {
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
            int tIntV = MathEX.Code.round2int(v);
            if (Math.abs(v-tIntV) < MathEX.Code.DBL_EPSILON) return tIntV;
            return v;
        });
        mIsCartesian = false;
        return this;
    }
    
    @Override protected void setBox_(double aX, double aY, double aZ) {
        // 这里统一移除掉 scale 的数据，保证新的 box 合法性
        if (mIsCartesian) {
            mDirect.multiply2this(mBox.scale());
        }
        mBox = new VaspBox(aX, aY, aZ);
    }
    @Override protected void setBox_(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        // 这里统一移除掉 scale 的数据，保证新的 box 合法性
        if (mIsCartesian) {
            mDirect.multiply2this(mBox.scale());
        }
        mBox = new VaspBoxPrism(aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);
    }
    @Override protected void scaleAtomPosition_(boolean aKeepAtomPosition, double aScale) {
        // 对于 Cartesian 和 Direct 要分开讨论
        if (mIsCartesian) {
            if (aKeepAtomPosition) return;
            mDirect.multiply2this(aScale);
            return;
        }
        if (!aKeepAtomPosition) return;
        mDirect.div2this(aScale);
    }
    @Override protected void validAtomPosition_(boolean aKeepAtomPosition, IBox aOldBox) {
        // 对于 Cartesian 和 Direct 要分开讨论
        final int tNumAtoms = this.natoms();
        XYZ tBuf = new XYZ();
        if (mIsCartesian) {
            if (aKeepAtomPosition) return;
            if (mBox.isPrism() || aOldBox.isPrism()) {
                for (int i = 0; i < tNumAtoms; ++i) {
                    tBuf.setXYZ(mDirect.get(i, 0), mDirect.get(i, 1), mDirect.get(i, 2));
                    // 这样转换两次即可实现线性变换
                    aOldBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    mDirect.set(i, 0, tBuf.mX);
                    mDirect.set(i, 1, tBuf.mY);
                    mDirect.set(i, 2, tBuf.mZ);
                }
            } else {
                tBuf.setXYZ(mBox);
                tBuf.div2this(aOldBox);
                mDirect.col(0).multiply2this(tBuf.mX);
                mDirect.col(1).multiply2this(tBuf.mY);
                mDirect.col(2).multiply2this(tBuf.mZ);
            }
            return;
        }
        if (!aKeepAtomPosition) return;
        if (mBox.isPrism() || aOldBox.isPrism()) {
            for (int i = 0; i < tNumAtoms; ++i) {
                tBuf.setXYZ(mDirect.get(i, 0), mDirect.get(i, 1), mDirect.get(i, 2));
                // 对于 Direct 需要这样反向变换
                aOldBox.toCartesian(tBuf);
                mBox.toDirect(tBuf);
                mDirect.set(i, 0, tBuf.mX);
                mDirect.set(i, 1, tBuf.mY);
                mDirect.set(i, 2, tBuf.mZ);
            }
        } else {
            tBuf.setXYZ(aOldBox);
            tBuf.div2this(mBox);
            mDirect.col(0).multiply2this(tBuf.mX);
            mDirect.col(1).multiply2this(tBuf.mY);
            mDirect.col(2).multiply2this(tBuf.mZ);
        }
    }
    
    /**
     * 密度归一化
     * @return 返回自身来支持链式调用
     */
    @ApiStatus.Obsolete
    public POSCAR setDenseNormalized() {
        double tScale = MathEX.Fast.cbrt(volume() / this.natoms());
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
                final int tNumTypes = mNumAtomsVec.size();
                for (int i = 0; i < tNumTypes; ++i) {
                    rNumber += mNumAtomsVec.get(i);
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
                        from += mNumAtomsVec.get(typeMM);
                    }
                    int to = mIdx;
                    for (int typeMM = oType-1; typeMM < aType-1; ++typeMM) {
                        from += mNumAtomsVec.get(typeMM);
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
                    mNumAtomsVec.decrement(oType-1);
                    mNumAtomsVec.increment(aType-1);
                } else {
                    // 减小 type 的情况，这里简单处理，
                    // 将所有中间的边界处原子都向下跳跃到正确位置，
                    // 然后将此原子移动到新的间隙中
                    double tX = mDirect.get(mIdx, 0);
                    double tY = mDirect.get(mIdx, 1);
                    double tZ = mDirect.get(mIdx, 2);
                    int from = POSCAR.this.natoms();
                    for (int typeMM = mNumAtomsVec.size()-1; typeMM >= oType; --typeMM) {
                        from -= mNumAtomsVec.get(typeMM);
                    }
                    int to = mIdx;
                    for (int typeMM = oType-1; typeMM >= aType; --typeMM) {
                        from -= mNumAtomsVec.get(typeMM);
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
                    mNumAtomsVec.decrement(oType-1);
                    mNumAtomsVec.increment(aType-1);
                }
            }
        };
    }
    @Override public VaspBox box() {return mBox;}
    @Override public int natoms() {return mDirect.nrows();}
    @Override public int ntypes() {return mNumAtomsVec.size();}
    
    
    /** 拷贝一份 POSCAR */
    @Override public POSCAR copy() {
        return new POSCAR(mComment, mBox.copy(), copyTypeNames(mTypeNames), mNumAtomsVec.copy(), mSelectiveDynamics, mDirect.copy(), mIsCartesian);
    }
    // 由于 POSCAR 不是全都可以修改，因此不重写另外两个
    
    /** 从 IAtomData 来创建，POSCAR 需要额外的原子种类字符串以及额外的是否开启 SelectiveDynamics */
    public static POSCAR fromAtomData(IAtomData aAtomData) {
        @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
        return fromAtomData(aAtomData, tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    public static POSCAR fromAtomData(IAtomData aAtomData, String... aTypeNames) {
        return fromAtomData(aAtomData, (aAtomData instanceof POSCAR) && ((POSCAR)aAtomData).mSelectiveDynamics, aTypeNames);
    }
    public static POSCAR fromAtomData(IAtomData aAtomData, boolean aSelectiveDynamics, String... aTypeNames) {
        if (aTypeNames == null) aTypeNames = ZL_STR;
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            return ((POSCAR)aAtomData).copy().setSelectiveDynamics(aSelectiveDynamics).setSymbols(aTypeNames);
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
            int tAtomTypeNum = Math.max(aAtomData.ntypes(), aTypeNames.length);
            IIntVector rNumAtomsVec = IntVector.zeros(tAtomTypeNum);
            IMatrix rDirect = Matrices.zeros(aAtomData.natoms(), 3);
            int tIdx = 0;
            for (int tTypeMM = 0; tTypeMM < tAtomTypeNum; ++tTypeMM) {
                for (IAtom tAtom : aAtomData.atoms()) if (tAtom.type() == tTypeMM+1) {
                    rNumAtomsVec.increment(tTypeMM);
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
            return new POSCAR(null, rBox, copyTypeNames(aTypeNames), rNumAtomsVec, aSelectiveDynamics, rDirect, true);
        }
    }
    public static POSCAR fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomData(aAtomData, IO.Text.toArray(aTypeNames));
    }
    public static POSCAR fromAtomData(IAtomData aAtomData, boolean aSelectiveDynamics, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomData(aAtomData, aSelectiveDynamics, IO.Text.toArray(aTypeNames));
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static POSCAR of(IAtomData aAtomData) {
        return fromAtomData(aAtomData);
    }
    public static POSCAR of(IAtomData aAtomData, String... aTypeNames) {
        return fromAtomData(aAtomData, aTypeNames);
    }
    public static POSCAR of(IAtomData aAtomData, boolean aSelectiveDynamics, String... aTypeNames) {
        return fromAtomData(aAtomData, aSelectiveDynamics, aTypeNames);
    }
    public static POSCAR of(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomData(aAtomData, aTypeNames);
    }
    public static POSCAR of(IAtomData aAtomData, boolean aSelectiveDynamics, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomData(aAtomData, aSelectiveDynamics, aTypeNames);
    }
    
    
    
    static class Header {
        boolean full = false;
        String comment;
        double boxScale;
        IVector boxA, boxB, boxC;
        String[] typeNames = null;
        IIntVector numAtomsVec;
        boolean selectiveDynamics = false;
        boolean isCartesian;
        
        String lastLine = null;
        
        void fill(Header aHeader) {
            full = aHeader.full;
            comment = aHeader.comment;
            boxScale = aHeader.boxScale;
            boxA = aHeader.boxA;
            boxB = aHeader.boxB;
            boxC = aHeader.boxC;
            typeNames = aHeader.typeNames;
            numAtomsVec = aHeader.numAtomsVec;
            selectiveDynamics = aHeader.selectiveDynamics;
            isCartesian = aHeader.isCartesian;
            lastLine = aHeader.lastLine;
        }
    }
    
    /// 文件读写
    /**
     * 从 vasp 输出的 POSCAR 文件中读取来实现初始化
     * @param aFilePath vasp 输出的 POSCAR 文件路径
     * @return 读取得到的 {@link POSCAR} 对象
     * @throws IOException 如果读取失败
     */
    public static POSCAR read(String aFilePath) throws IOException {
        try (BufferedReader tReader = IO.toReader(aFilePath)) {
            return read(tReader);
        }
    }
    /**
     * 提供使用 {@link BufferedReader} 的流式接口
     * @param aReader 需要的读取流
     * @return 读取得到的 {@link POSCAR} 对象
     * @throws IOException 如果读取失败
     * @throws FileEndException 在发现文件似乎不完整时
     */
    public static POSCAR read(BufferedReader aReader) throws IOException {
        return read_(aReader, new Header());
    }
    static @Nullable Header readHeader_(BufferedReader aReader) throws IOException {
        String tLine;
        String[] tTokens;
        
        Header rHeader = new Header();
        // 第一行为 Comment 或者是 Direct configuration = 1，这里暂时不能确定
        tLine = aReader.readLine();
        rHeader.comment = tLine;
        // 此行只能是读取模拟盒信息或者是 Direct 矩阵，通过长度来判断是否有头
        tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;}
        tTokens = IO.Text.splitBlank(tLine);
        if (tTokens.length >= 3) {
            // 记录预读取值，并读取能读取的
            rHeader.lastLine = tLine;
            tLine = rHeader.comment;
            rHeader.comment = null;
            rHeader.isCartesian = IO.Text.containsIgnoreCase(tLine, "Cartesian");
            if (!rHeader.isCartesian && !IO.Text.containsIgnoreCase(tLine, "Direct")) {
                if (Conf.STRICT_IO) throw new IllegalArgumentException("Can ONLY read Direct or Cartesian POSCAR");
                return null;
            }
            return rHeader;
        }
        rHeader.boxScale = Double.parseDouble(tTokens[0]);
        tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;}
        rHeader.boxA = IO.Text.str2data(tLine, 3);
        tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;}
        rHeader.boxB = IO.Text.str2data(tLine, 3);
        tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;}
        rHeader.boxC = IO.Text.str2data(tLine, 3);
        // 读取原子种类（可选）和对应数目的信息
        boolean tNoAtomType = false;
        tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;} tTokens = IO.Text.splitBlank(tLine);
        rHeader.typeNames = tTokens;
        tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;} tTokens = IO.Text.splitBlank(tLine);
        try {
            final String[] fTokens = tTokens;
            rHeader.numAtomsVec = Vectors.fromInt(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        } catch (Exception e) {
            tNoAtomType = true;
            final String[] fTokens = rHeader.typeNames;
            rHeader.numAtomsVec = Vectors.fromInt(fTokens.length, i -> Integer.parseInt(fTokens[i]));
            rHeader.typeNames = null;
        }
        if (!tNoAtomType) {
            tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;}
        }
        // 可选的注释行
        if (tLine.equalsIgnoreCase("Selective dynamics")) {
            rHeader.selectiveDynamics = true; tLine = aReader.readLine(); if (tLine == null) {IO.fileEnd(); return null;}
        }
        // 只支持 Direct 和 Cartesian，改为 contains 从而可以支持直接读取 XDATCAR
        rHeader.isCartesian = IO.Text.containsIgnoreCase(tLine, "Cartesian");
        if (!rHeader.isCartesian && !IO.Text.containsIgnoreCase(tLine, "Direct")) {
            if (Conf.STRICT_IO) throw new IllegalArgumentException("Can ONLY read Direct or Cartesian POSCAR");
            return null;
        }
        rHeader.full = true;
        return rHeader;
    }
    static POSCAR read_(BufferedReader aReader, @NotNull Header rHeader) throws IOException {
        String tLine, tLastLine = null;
        
        // 无论怎样先尝试读取头
        Header tHeader = readHeader_(aReader);
        // 总是优先用新的值
        if (tHeader!=null && tHeader.full) {
            rHeader.fill(tHeader);
        }
        // 至少要有一个头
        if (!rHeader.full) {IO.fileEnd(); return null;} // 这里获取 header 失败总是由于文件结束（否则会在上面直接抛出错误）
        
        // 特殊情况，尝试读取头存储了一个旧的 line
        if (tHeader!=null && tHeader.lastLine!=null) {
            rHeader.isCartesian = tHeader.isCartesian;
            tLastLine = tHeader.lastLine;
        }
        
        // 读取原子数据
        int tNumAtoms = rHeader.numAtomsVec.sum();
        IMatrix aDirect = RowMatrix.zeros(tNumAtoms, 3);
        for (IVector tRow : aDirect.rows()) {
            if (tLastLine == null) {
                tLine = aReader.readLine();
            } else {
                tLine = tLastLine;
                tLastLine = null;
            }
            if (tLine == null) {IO.fileEnd(); return null;}
            tRow.fill(IO.Text.str2data(tLine, 3));
        }
        
        // 判断是否是 prism 并据此创建 VaspBox
        IVector aBoxA = rHeader.boxA;
        IVector aBoxB = rHeader.boxB;
        IVector aBoxC = rHeader.boxC;
        boolean tNotPrism =
               MathEX.Code.numericEqual(aBoxA.get(1), 0.0) && MathEX.Code.numericEqual(aBoxA.get(2), 0.0)
            && MathEX.Code.numericEqual(aBoxB.get(0), 0.0) && MathEX.Code.numericEqual(aBoxB.get(2), 0.0)
            && MathEX.Code.numericEqual(aBoxC.get(0), 0.0) && MathEX.Code.numericEqual(aBoxC.get(1), 0.0)
            ;
        VaspBox aBox = tNotPrism ?
            new VaspBox(aBoxA.get(0), aBoxB.get(1), aBoxC.get(2), rHeader.boxScale) :
            new VaspBoxPrism(
                aBoxA.get(0), aBoxA.get(1), aBoxA.get(2),
                aBoxB.get(0), aBoxB.get(1), aBoxB.get(2),
                aBoxC.get(0), aBoxC.get(1), aBoxC.get(2),
                rHeader.boxScale
            );
        // 返回 POSCAR
        return new POSCAR(rHeader.comment, aBox, rHeader.typeNames, rHeader.numAtomsVec, rHeader.selectiveDynamics, aDirect, rHeader.isCartesian);
    }
    
    /**
     * 输出成 vasp 能够读取的 POSCAR 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
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
        write_(aWriteln, -1, DEFAULT_COMMENT);
    }
    void write_(IO.IWriteln aWriteln, int aConf, String aDefaultComment) throws IOException {
        aWriteln.writeln(mComment==null ? aDefaultComment : mComment);
        aWriteln.writeln(String.valueOf(mBox.scale()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.iax(), mBox.iay(), mBox.iaz()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.ibx(), mBox.iby(), mBox.ibz()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.icx(), mBox.icy(), mBox.icz()));
        if (mTypeNames!=null && mTypeNames.length!=0) {
        aWriteln.writeln(String.join(" ", AbstractCollections.slice(AbstractCollections.map(mTypeNames, type -> String.format("%6s", type)), AbstractCollections.range(mNumAtomsVec.size()))));
        }
        aWriteln.writeln(String.join(" ", AbstractCollections.map(mNumAtomsVec.iterable(), number -> String.format("%6d", number))));
        if (mSelectiveDynamics) {
        aWriteln.writeln("Selective dynamics");
        }
        String tLine = mIsCartesian ? "Cartesian" : "Direct";
        if (aConf > 0) tLine += (" configuration= " + aConf); // for XDATCAR
        aWriteln.writeln(tLine);
        for (IVector subDirect : mDirect.rows()) {
        aWriteln.writeln(String.format("%24.18g  %24.18g  %24.18g", subDirect.get(0), subDirect.get(1), subDirect.get(2)));
        }
    }
}
