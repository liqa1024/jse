package jse.lmp;

import jse.atom.*;
import jse.cache.IntVectorCache;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.IO;
import jse.code.UT;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jse.code.CS.*;


/**
 * @author liqa
 * <p> lammps 使用 write_data 写出的数据格式，头的顺序也是本程序的标准原子格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持键和键角等信息 </p>
 * <p> 直接获取到的所有数据都是引用，因此外部可以直接进行修改 </p>
 * <p> 修改了格式从而匹配 {@link NativeLmp} 的使用 </p>
 */
public class Lmpdat extends AbstractSettableAtomData {
    public final static int LMPDAT_VELOCITY_LENGTH = 4, LMPDAT_BOND_LENGTH = 4;
    public final static int LMPDAT_ID_COL = 0, LMPDAT_VX_COL = 1, LMPDAT_VY_COL = 2, LMPDAT_VZ_COL = 3;
    public final static int LMPDAT_TYPE_COL = 1, LMPDAT_X_COL = 2, LMPDAT_Y_COL = 3, LMPDAT_Z_COL = 4;
    public final static int LMPDAT_FULL_ID_COL = 0, LMPDAT_FULL_TYPE_COL = 1, LMPDAT_FULL_MOL_COL = 2, LMPDAT_FULL_CHARGE_COL = 3, LMPDAT_FULL_X_COL = 4, LMPDAT_FULL_Y_COL = 5, LMPDAT_FULL_Z_COL = 6;
    public final static int LMPDAT_BOND_ID_COL = 0, LMPDAT_BOND_TYPE_COL = 1, LMPDAT_BOND_ID1_COL = 2, LMPDAT_BOND_ID2_COL = 3;
    
    private final int mAtomNum;
    private final int mBondNum;
    private int mAtomTypeNum, mBondTypeNum;
    private LmpBox mBox;
    private @Nullable IVector mMasses;
    private final IntVector mAtomID;
    private final IntVector mAtomType;
    private final RowMatrix mAtomXYZ;
    private @Nullable RowMatrix mVelocities;
    private final IntList @Nullable[] mBondID;
    private final IntList @Nullable[] mBondType;
    private final IntList @Nullable[] mBondIndex;
    /** 缓存的 symbols，主要用来加速获取到 symbol 值 */
    private String @Nullable[] mSymbols;
    
    Lmpdat(int aAtomTypeNum, int aBondTypeNum, int aBondNum, LmpBox aBox, @Nullable IVector aMasses, IntVector aAtomID, IntVector aAtomType, RowMatrix aAtomXYZ, @Nullable RowMatrix aVelocities, IntList @Nullable[] aBondID, IntList @Nullable[] aBondType, IntList @Nullable[] aBondIndex) {
        mBox = aBox;
        mMasses = aMasses;
        mAtomID = aAtomID;
        mAtomType = aAtomType;
        mAtomXYZ = aAtomXYZ;
        // 根据 atomID 获取原子数目
        mAtomNum = aAtomID.size();
        mBondNum = aBondNum;
        mAtomTypeNum = aAtomTypeNum;
        mBondTypeNum = aBondTypeNum;
        mVelocities = aVelocities;
        mBondID = aBondID;
        mBondType = aBondType;
        mBondIndex = aBondIndex;
        // 初始化 mSymbols
        validSymbols_();
    }
    Lmpdat(int aAtomTypeNum, LmpBox aBox, @Nullable IVector aMasses, IntVector aAtomID, IntVector aAtomType, RowMatrix aAtomXYZ, @Nullable RowMatrix aVelocities) {
        this(aAtomTypeNum, 0, 0, aBox, aMasses, aAtomID, aAtomType, aAtomXYZ, aVelocities, null, null, null);
    }
    
    
    /// 缓存部分
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isFromCache() {
        return IntVectorCache.isFromCache(mAtomID) && IntVectorCache.isFromCache(mAtomType)
            && MatrixCache.isFromCache(mAtomXYZ) && (mVelocities==null || MatrixCache.isFromCache(mVelocities));
    }
    public void returnToCache() {
        if (!isFromCache()) throw new IllegalArgumentException("Return Lmpdat MUST be from cache");
        IntVectorCache.returnVec(mAtomID);
        IntVectorCache.returnVec(mAtomType);
        MatrixCache.returnMat(mAtomXYZ);
        if (mVelocities != null) MatrixCache.returnMat(mVelocities);
        // 现在不再归还 masses，虽然 NativeLmp 获取的 Lmpdat 的 masses
        // 是来自 cache 的，但是由于经过了一个 subVec 因此不能直接归还
    }
    
    
    /// 参数修改
    /**
     * 修改一些属性来方便调整最终输出的 data 文件
     * @return 返回自身来支持链式调用
     */
    @Override public Lmpdat setMasses(double... aMasses) {return setMasses_(Vectors.from(aMasses), false);}
    @Override public Lmpdat setMasses(Collection<? extends Number> aMasses) {return setMasses_(Vectors.from(aMasses), false);}
    @Override public Lmpdat setMasses(@Nullable IVector aMasses) {return setMasses_(aMasses, true);}
    @Override public Lmpdat setNoMass() {return setMasses((IVector)null);}
    Lmpdat setMasses_(@Nullable IVector aMasses, boolean aCopy) {
        if (aMasses==null || aMasses.isEmpty()) {
            mMasses = null; return this;
        }
        if (mMasses==null || aMasses.size()>mMasses.size()) mMasses = aCopy ? aMasses.copy() : aMasses;
        else mMasses.subVec(0, aMasses.size()).fill(aMasses);
        mAtomTypeNum = Math.max(mAtomTypeNum, aMasses.size());
        validSymbols_();
        return this;
    }
    /** 设置原子种类数目 */
    @Override public Lmpdat setAtomTypeNumber(int aAtomTypeNum) {
        int oTypeNum = mAtomTypeNum;
        if (aAtomTypeNum == oTypeNum) return this;
        mAtomTypeNum = aAtomTypeNum;
        if (aAtomTypeNum < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            mAtomType.op().map2this(v -> Math.min(v, aAtomTypeNum));
            return this;
        }
        // 现在理论上不需要更新 Masses 长度
        return this;
    }
    /** 设置键种类数目 */
    public Lmpdat setBondTypeNumber(int aBondTypeNum) {
        if (mBondType == null) throw new UnsupportedOperationException("setBondTypeNumber for Lmpdat has no bond");
        int oTypeNum = mBondTypeNum;
        if (aBondTypeNum == oTypeNum) return this;
        mBondTypeNum = aBondTypeNum;
        if (aBondTypeNum < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            for (IntList tBondType : mBondType) {
                int tSize = tBondType.size();
                for (int j = 0; j < tSize; ++j) {
                    tBondType.set(j, Math.min(tBondType.get(j), aBondTypeNum));
                }
            }
        }
        return this;
    }
    @Override public Lmpdat setNoVelocity() {mVelocities = null; return this;}
    @Override public Lmpdat setHasVelocity() {if (mVelocities == null) {mVelocities = RowMatrix.zeros(mAtomNum, ATOM_DATA_KEYS_VELOCITY.length);} return this;}
    @Override public Lmpdat setSymbols(String... aSymbols) {
        IVector rMasses = Vectors.NaN(aSymbols.length);
        for (int i = 0; i < aSymbols.length; ++i) {
            rMasses.set(i, MASS.getOrDefault(aSymbols[i], mass(i+1)));
        }
        return setMasses_(rMasses, false);
    }
    void validSymbols_() {
        if (mMasses == null) {
            mSymbols = null;
            return;
        }
        if (mSymbols==null || mSymbols.length<mMasses.size()) {
            mSymbols = new String[mMasses.size()];
        }
        for (int tType = 1; tType <= mAtomTypeNum; ++tType) {
            mSymbols[tType-1] = symbol_(tType);
        }
    }
    @Nullable String symbol_(int aType) {
        // 直接通过质量来猜测元素种类，直接遍历，误差在 0.1 内即可
        double tMass = mass(aType);
        if (Double.isNaN(tMass)) return null;
        for (Map.Entry<String, Double> tEntry : MASS.entrySet()) {
            if (Math.abs(tMass - tEntry.getValue()) < 0.1) return tEntry.getKey();
        }
        return null;
    }
    
    /// set box stuff
    @Override protected void setBox_(double aX, double aY, double aZ) {
        // 这里统一移除掉 boxlo 的数据，保证新的 box 合法性
        mAtomXYZ.col(XYZ_X_COL).minus2this(mBox.xlo());
        mAtomXYZ.col(XYZ_Y_COL).minus2this(mBox.ylo());
        mAtomXYZ.col(XYZ_Z_COL).minus2this(mBox.zlo());
        mBox = new LmpBox(aX, aY, aZ);
    }
    @Override protected void setBox_(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {
        // 这里统一移除掉 boxlo 的数据，保证新的 box 合法性
        mAtomXYZ.col(XYZ_X_COL).minus2this(mBox.xlo());
        mAtomXYZ.col(XYZ_Y_COL).minus2this(mBox.ylo());
        mAtomXYZ.col(XYZ_Z_COL).minus2this(mBox.zlo());
        mBox = new LmpBoxPrism(aX, aY, aZ, aXY, aXZ, aYZ);
    }
    @Override protected void setBox_(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        // 这里统一移除掉 boxlo 的数据，保证新的 box 合法性
        mAtomXYZ.col(XYZ_X_COL).minus2this(mBox.xlo());
        mAtomXYZ.col(XYZ_Y_COL).minus2this(mBox.ylo());
        mAtomXYZ.col(XYZ_Z_COL).minus2this(mBox.zlo());
        mBox = LmpBox.of(new XYZ(aAx, aAy, aAz), new XYZ(aBx, aBy, aBz), new XYZ(aCx, aCy, aCz));
    }
    @Override protected void scaleAtomPosition_(boolean aKeepAtomPosition, double aScale) {
        if (aKeepAtomPosition) return;
        mAtomXYZ.multiply2this(aScale);
        if (mVelocities != null) {
            mVelocities.multiply2this(aScale);
        }
    }
    @Override protected void validAtomPosition_(boolean aKeepAtomPosition, IBox aOldBox) {
        if (aKeepAtomPosition) return;
        final int tAtomNum = atomNumber();
        XYZ tBuf = new XYZ();
        if (mBox.isPrism() || aOldBox.isPrism()) {
            for (int i = 0; i < tAtomNum; ++i) {
                tBuf.setXYZ(mAtomXYZ.get(i, XYZ_X_COL), mAtomXYZ.get(i, XYZ_Y_COL), mAtomXYZ.get(i, XYZ_Z_COL));
                // 这样转换两次即可实现线性变换
                aOldBox.toDirect(tBuf);
                mBox.toCartesian(tBuf);
                mAtomXYZ.set(i, XYZ_X_COL, tBuf.mX);
                mAtomXYZ.set(i, XYZ_Y_COL, tBuf.mY);
                mAtomXYZ.set(i, XYZ_Z_COL, tBuf.mZ);
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
            mAtomXYZ.col(XYZ_X_COL).multiply2this(tBuf.mX);
            mAtomXYZ.col(XYZ_Y_COL).multiply2this(tBuf.mY);
            mAtomXYZ.col(XYZ_Z_COL).multiply2this(tBuf.mZ);
            // 如果存在速度，则速度也需要做一次这样的变换
            if (mVelocities != null) {
                mVelocities.col(STD_VX_COL).multiply2this(tBuf.mX);
                mVelocities.col(STD_VY_COL).multiply2this(tBuf.mY);
                mVelocities.col(STD_VZ_COL).multiply2this(tBuf.mZ);
            }
        }
    }
    
    /**
     * 密度归一化
     * @return 返回自身来支持链式调用
     */
    @ApiStatus.Obsolete
    public Lmpdat setDenseNormalized() {
        double tScale = MathEX.Fast.cbrt(volume() / mAtomNum);
        tScale = 1.0 / tScale;
        return (Lmpdat)setBoxScale(tScale);
    }
    
    
    /// 获取属性
    /** @deprecated use {@link #box} */ @Deprecated public LmpBox lmpBox() {return box();}
    public IIntVector ids() {return mAtomID;}
    public IIntVector types() {return mAtomType;}
    public IMatrix positions() {return mAtomXYZ;}
    /** @return {@inheritDoc} */
    @Override public boolean hasID() {return true;}
    @Override public boolean hasBond() {return mBondIndex!=null;}
    @Override public boolean hasBondID() {return true;}
    public @Nullable IMatrix velocities() {return mVelocities;}
    @Override public boolean hasVelocity() {return mVelocities != null;}
    @Override public boolean hasMass() {return mMasses!=null;}
    @Override public double mass(int aType) {return (mMasses==null || aType>mMasses.size()) ? Double.NaN : mMasses.get(aType-1);}
    @Override public boolean hasSymbol() {return this.hasMass();}
    @Override public @Nullable String symbol(int aType) {return (mSymbols==null || aType>mSymbols.length) ? null : mSymbols[aType-1];}
    
    /** AbstractAtomData stuffs */
    @Override public ISettableAtom atom(final int aIdx) {
        return new AbstractSettableAtom_() {
            @Override public int index() {return aIdx;}
            @Override public double x() {return mAtomXYZ.get(aIdx, XYZ_X_COL)-mBox.xlo();}
            @Override public double y() {return mAtomXYZ.get(aIdx, XYZ_Y_COL)-mBox.ylo();}
            @Override public double z() {return mAtomXYZ.get(aIdx, XYZ_Z_COL)-mBox.zlo();}
            @Override protected int id_() {return mAtomID.get(aIdx);}
            @Override protected int type_() {return mAtomType.get(aIdx);}
            @Override protected double vx_() {assert mVelocities!=null; return mVelocities.get(aIdx, STD_VX_COL);}
            @Override protected double vy_() {assert mVelocities!=null; return mVelocities.get(aIdx, STD_VY_COL);}
            @Override protected double vz_() {assert mVelocities!=null; return mVelocities.get(aIdx, STD_VZ_COL);}
            
            @Override protected void setX_(double aX) {mAtomXYZ.set(aIdx, XYZ_X_COL, aX+mBox.xlo());}
            @Override protected void setY_(double aY) {mAtomXYZ.set(aIdx, XYZ_Y_COL, aY+mBox.ylo());}
            @Override protected void setZ_(double aZ) {mAtomXYZ.set(aIdx, XYZ_Z_COL, aZ+mBox.zlo());}
            @Override protected void setID_(int aID) {mAtomID.set(aIdx, aID);}
            @Override protected void setType_(int aType) {mAtomType.set(aIdx, aType);}
            @Override protected void setVx_(double aVx) {assert mVelocities!=null; mVelocities.set(aIdx, STD_VX_COL, aVx);}
            @Override protected void setVy_(double aVy) {assert mVelocities!=null; mVelocities.set(aIdx, STD_VY_COL, aVy);}
            @Override protected void setVz_(double aVz) {assert mVelocities!=null; mVelocities.set(aIdx, STD_VZ_COL, aVz);}
            
            @Override public List<? extends IBond> bonds() {
                if (mBondIndex==null) return null;
                return new AbstractRandomAccessList<IBond>() {
                    @Override public IBond get(int index) {
                        return new AbstractBond_() {
                            @Override public int id() {
                                assert mBondID != null;
                                return mBondID[aIdx].get(index);
                            }
                            @Override protected int type_() {
                                assert mBondType != null;
                                return mBondType[aIdx].get(index);
                            }
                            @Override public int bondIndex() {
                                return mBondIndex[aIdx].get(index);
                            }
                        };
                    }
                    @Override public int size() {
                        return mBondIndex[aIdx].size();
                    }
                };
            }
        };
    }
    @Override public LmpBox box() {return mBox;}
    @Override public int atomNumber() {return mAtomNum;}
    @Override public int atomTypeNumber() {return mAtomTypeNum;}
    @Override public int bondTypeNumber() {return mBondTypeNum;}
    
    
    private static IntList @Nullable[] copyBondInfo_(IntList @Nullable[] aBondInfo) {
        if (aBondInfo == null) return null;
        IntList[] tOut = new IntList[aBondInfo.length];
        for (int i = 0; i < aBondInfo.length; ++i) {
            tOut[i] = aBondInfo[i].copy();
        }
        return tOut;
    }
    /// 创建 Lmpdat
    /** 拷贝一份 Lmpdat，为了简洁还是只保留 copy 一种方法 */
    @Override public Lmpdat copy() {return new Lmpdat(mAtomTypeNum, mBondTypeNum, mBondNum, mBox.copy(), mMasses==null?null:mMasses.copy(), mAtomID.copy(), mAtomType.copy(), mAtomXYZ.copy(), mVelocities==null?null:mVelocities.copy(), copyBondInfo_(mBondID), copyBondInfo_(mBondType), copyBondInfo_(mBondIndex));}
    @Override protected Lmpdat newSame_() {return new Lmpdat(mAtomTypeNum, mBondTypeNum, mBondNum, mBox.copy(), mMasses==null?null:mMasses.copy(), mAtomID.copy(), mAtomType.copy(), mAtomXYZ.copy(), mVelocities==null?null:mVelocities.copy(), copyBondInfo_(mBondID), copyBondInfo_(mBondType), copyBondInfo_(mBondIndex));}
    @Override protected Lmpdat newZeros_(int aAtomNum) {return new Lmpdat(mAtomTypeNum, mBondTypeNum, mBondNum, mBox.copy(), mMasses==null?null:mMasses.copy(), IntVector.zeros(aAtomNum), IntVector.zeros(aAtomNum), RowMatrix.zeros(aAtomNum, mAtomXYZ.columnNumber()), mVelocities==null?null:RowMatrix.zeros(aAtomNum, mVelocities.columnNumber()), copyBondInfo_(mBondID), copyBondInfo_(mBondType), copyBondInfo_(mBondIndex));}
    @Override protected Lmpdat newZeros_(int aAtomNum, IBox aBox) {return new Lmpdat(mAtomTypeNum, mBondTypeNum, mBondNum, LmpBox.of(aBox), mMasses==null?null:mMasses.copy(), IntVector.zeros(aAtomNum), IntVector.zeros(aAtomNum), RowMatrix.zeros(aAtomNum, mAtomXYZ.columnNumber()), mVelocities==null?null:RowMatrix.zeros(aAtomNum, mVelocities.columnNumber()), copyBondInfo_(mBondID), copyBondInfo_(mBondType), copyBondInfo_(mBondIndex));}
    
    /** 从 IAtomData 来创建，一般来说 Lmpdat 需要一个额外的质量信息 */
    public static Lmpdat fromAtomData(IAtomData aAtomData) {return fromAtomData_(aAtomData, Vectors.from(aAtomData.masses()));}
    public static Lmpdat fromAtomData(IAtomData aAtomData, IVector aMasses) {return fromAtomData_(aAtomData, Vectors.from(aMasses));}
    public static Lmpdat fromAtomData(IAtomData aAtomData, Collection<? extends Number> aMasses) {return fromAtomData_(aAtomData, Vectors.from(aMasses));}
    public static Lmpdat fromAtomData(IAtomData aAtomData, double... aMasses) {return fromAtomData_(aAtomData, Vectors.from(aMasses));}
    
    static Lmpdat fromAtomData_(IAtomData aAtomData, @Nullable IVector aMasses) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof Lmpdat) {
            // Lmpdat 则直接获取即可（专门优化，保留完整模拟盒信息）
            return ((Lmpdat)aAtomData).copy().setMasses_(aMasses, false);
        } else {
            int tAtomNum = aAtomData.atomNumber();
            IntVector rAtomID = IntVector.zeros(tAtomNum);
            IntVector rAtomType = IntVector.zeros(tAtomNum);
            RowMatrix rAtomXYZ = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix rVelocities = aAtomData.hasVelocity() ? RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length) : null;
            boolean tHasBond = aAtomData.hasBond();
            int tBondTypeNumber = tHasBond ? aAtomData.bondTypeNumber() : 0;
            IntList @Nullable[] rBondID = tHasBond ? new IntList[tAtomNum] : null;
            IntList @Nullable[] rBondType = tHasBond ? new IntList[tAtomNum] : null;
            IntList @Nullable[] rBondIndex = tHasBond ? new IntList[tAtomNum] : null;
            // 一般的情况，需要考虑斜方的模拟盒的情况
            IBox tBox = aAtomData.box();
            LmpBox rBox = LmpBox.of(tBox);
            if (tBox.isLmpStyle()) {
                // 模拟盒满足 lammps 种类下可以直接拷贝过来
                for (int i = 0; i < tAtomNum; ++i) {
                    IAtom tAtom = aAtomData.atom(i);
                    rAtomID.set(i, tAtom.id());
                    rAtomType.set(i, tAtom.type());
                    rAtomXYZ.set(i, XYZ_X_COL, tAtom.x());
                    rAtomXYZ.set(i, XYZ_Y_COL, tAtom.y());
                    rAtomXYZ.set(i, XYZ_Z_COL, tAtom.z());
                    if (rVelocities != null) {
                        rVelocities.set(i, STD_VX_COL, tAtom.vx());
                        rVelocities.set(i, STD_VY_COL, tAtom.vy());
                        rVelocities.set(i, STD_VZ_COL, tAtom.vz());
                    }
                }
            } else {
                // 否则需要转换成 lammps 的种类
                XYZ tBuf = new XYZ();
                for (int i = 0; i < tAtomNum; ++i) {
                    IAtom tAtom = aAtomData.atom(i);
                    rAtomID.set(i, tAtom.id());
                    rAtomType.set(i, tAtom.type());
                    tBuf.setXYZ(tAtom);
                    tBox.toDirect(tBuf);
                    rBox.toCartesian(tBuf);
                    rAtomXYZ.set(i, XYZ_X_COL, tBuf.mX);
                    rAtomXYZ.set(i, XYZ_Y_COL, tBuf.mY);
                    rAtomXYZ.set(i, XYZ_Z_COL, tBuf.mZ);
                    // 对于速度也使用同样的变换
                    if (rVelocities != null) {
                        tBuf.setXYZ(tAtom.vx(), tAtom.vy(), tAtom.vz());
                        tBox.toDirect(tBuf);
                        rBox.toCartesian(tBuf);
                        rVelocities.set(i, STD_VX_COL, tBuf.mX);
                        rVelocities.set(i, STD_VY_COL, tBuf.mY);
                        rVelocities.set(i, STD_VZ_COL, tBuf.mZ);
                    }
                }
            }
            int tAtomTypeNum = aAtomData.atomTypeNumber();
            if (aMasses != null && aMasses.isEmpty()) aMasses = null;
            if (aMasses != null) {
                if (aMasses.size() > tAtomTypeNum) {
                    tAtomTypeNum = aMasses.size();
                } else
                if (aMasses.size() < tAtomTypeNum) {
                    IVector rMasses = Vectors.NaN(tAtomTypeNum);
                    rMasses.subVec(0, aMasses.size()).fill(aMasses);
                    aMasses = rMasses;
                }
            }
            int tBondNum = 0;
            if (tHasBond) {
                boolean tHasBondID = aAtomData.hasBondID();
                for (int i = 0; i < tAtomNum; ++i) {
                    rBondID[i] = new IntList();
                    rBondType[i] = new IntList();
                    rBondIndex[i] = new IntList();
                    for (IBond tBond : aAtomData.atom(i).bonds()) {
                        ++tBondNum;
                        rBondID[i].add(tHasBondID ? tBond.id() : tBondNum);
                        rBondType[i].add(tBond.type());
                        rBondIndex[i].add(tBond.bondIndex());
                    }
                }
            }
            return new Lmpdat(tAtomTypeNum, tBondTypeNumber, tBondNum/2, rBox, aMasses, rAtomID, rAtomType, rAtomXYZ, rVelocities, rBondID, rBondType, rBondIndex);
        }
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static Lmpdat of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static Lmpdat of(IAtomData aAtomData, IVector aMasses) {return fromAtomData(aAtomData, aMasses);}
    public static Lmpdat of(IAtomData aAtomData, Collection<? extends Number> aMasses) {return fromAtomData(aAtomData, aMasses);}
    public static Lmpdat of(IAtomData aAtomData, double... aMasses) {return fromAtomData(aAtomData, aMasses);}
    
    
    /// 文件读写
    /**
     * 从 lammps 输出的 data 文件中读取来实现初始化
     * <p>
     * 目前只支持单原子数据
     * @param aFilePath lammps 输出的 data 文件路径
     * @return 读取得到的 Lmpdat 对象，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static Lmpdat read(String aFilePath) throws IOException {try (BufferedReader tReader = IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static Lmpdat read_(BufferedReader aReader) throws IOException {
        String tLine;
        String[] tTokens;
        
        int tAtomNum;
        int aBondNum = 0;
        int aAtomTypeNum;
        int aBondTypeNum = 0;
        LmpBox aBox;
        @Nullable IVector aMasses = null;
        IntVector aAtomID = null;
        IntVector aAtomType = null;
        RowMatrix aAtomXYZ = null;
        @Nullable RowMatrix aVelocities = null;
        IntList @Nullable[] aBondID = null;
        IntList @Nullable[] aBondType = null;
        IntList @Nullable[] aBondIndex = null;
        // 统计 id 和对应行的映射
        Map<Integer, Integer> tId2Row = null;
        
        // 跳过第一行描述
        aReader.readLine();
        // 读取原子数目（中间存在空行以及可能存在的不支持的信息）
        tLine = IO.Text.findLineContaining(aReader, "atoms", true); if (tLine == null) return null; tTokens = IO.Text.splitBlank(tLine);
        tAtomNum = Integer.parseInt(tTokens[0]);
        // 读取可选键数目
        tLine = IO.Text.findLineNoBlank(aReader); if (tLine == null) return null;
        if (IO.Text.containsIgnoreCase(tLine, "bonds")) {
            tTokens = IO.Text.splitBlank(tLine);
            aBondNum = Integer.parseInt(tTokens[0]);
        }
        // 读取原子种类数目（中间存在可选空行以及可能存在的不支持的信息）
        if (!IO.Text.containsIgnoreCase(tLine, "atom types")) {
            tLine = IO.Text.findLineContaining(aReader, "atom types", true); if (tLine == null) return null;
        }
        tTokens = IO.Text.splitBlank(tLine);
        aAtomTypeNum = Integer.parseInt(tTokens[0]);
        // 读取可选键种类数
        tLine = IO.Text.findLineNoBlank(aReader); if (tLine == null) return null;
        if (IO.Text.containsIgnoreCase(tLine, "bond types")) {
            tTokens = IO.Text.splitBlank(tLine);
            aBondTypeNum = Integer.parseInt(tTokens[0]);
        }
        // 读取模拟盒信息（中间存在空行以及可能存在的不支持的信息）
        if (!IO.Text.containsIgnoreCase(tLine, "xlo xhi")) {
            tLine = IO.Text.findLineContaining(aReader, "xlo xhi", true); if (tLine == null) return null;
        }
        tTokens = IO.Text.splitBlank(tLine);
        double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]);
        tLine = IO.Text.findLineContaining(aReader, "ylo yhi", true); if (tLine == null) return null; tTokens = IO.Text.splitBlank(tLine);
        double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]);
        tLine = IO.Text.findLineContaining(aReader, "zlo zhi", true); if (tLine == null) return null; tTokens = IO.Text.splitBlank(tLine);
        double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]);
        // 兼容可能的斜方模拟盒，直接在下一行
        tLine = aReader.readLine();
        if (IO.Text.containsIgnoreCase(tLine, "xy xz yz")) {
            tTokens = IO.Text.splitBlank(tLine);
            aBox = new LmpBoxPrism(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi, Double.parseDouble(tTokens[0]), Double.parseDouble(tTokens[1]), Double.parseDouble(tTokens[2]));
        } else {
            aBox = new LmpBox(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi);
        }
        // 读取任意属性直到结束
        while ((tLine = findLineNonBlank_(aReader)) != null) {
            // 各种情况分别处理
            if (aMasses==null && IO.Text.containsIgnoreCase(tLine, "Masses")) {
                aMasses = Vectors.zeros(aAtomTypeNum);
                readMasses_(aReader, aMasses);
            } else
            if (aAtomID==null && IO.Text.containsIgnoreCase(tLine, "Atoms")) {
                boolean aIsFull = IO.Text.containsIgnoreCase(tLine, "# full");
                aAtomID = IntVector.zeros(tAtomNum);
                aAtomType = IntVector.zeros(tAtomNum);
                aAtomXYZ = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
                readAtoms_(aReader, aAtomID, aAtomType, aAtomXYZ, aIsFull);
                // 统计 id 到对应行号的映射，用于其余属性的快速赋值
                tId2Row = new HashMap<>(tAtomNum);
                for (int i = 0; i < tAtomNum; ++i) tId2Row.put(aAtomID.get(i), i);
            } else
            if (tId2Row!=null && aVelocities==null && IO.Text.containsIgnoreCase(tLine, "Velocities")) {
                aVelocities = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length);
                readVelocities_(aReader, tId2Row, aVelocities);
            } else
            if (tId2Row!=null && aBondNum >=0 && aBondID==null && IO.Text.containsIgnoreCase(tLine, "Bonds")) {
                aBondID = new IntList[tAtomNum];
                aBondType = new IntList[tAtomNum];
                aBondIndex = new IntList[tAtomNum];
                for (int i = 0; i < tAtomNum; ++i) {
                    aBondID[i] = new IntList();
                    aBondType[i] = new IntList();
                    aBondIndex[i] = new IntList();
                }
                readBonds_(aReader, tId2Row, aBondNum, aBondID, aBondType, aBondIndex);
            } else {
                readElse_(aReader);
            }
        }
        if (aAtomID == null) return null;
        // 返回 lmpdat
        return new Lmpdat(aAtomTypeNum, aBondTypeNum, aBondNum, aBox, aMasses, aAtomID, aAtomType, aAtomXYZ, aVelocities, aBondID, aBondType, aBondIndex);
    }
    
    /** 跳过空行的通用方法，这样可以处理神奇的各种空行数目的情况 */
    private static String findLineNonBlank_(BufferedReader aReader) throws IOException {
        String tLine;
        while ((tLine = aReader.readLine()) != null) {
            if (!IO.Text.isBlank(tLine)) return tLine;
        }
        return null;
    }
    /**
     * 读取特定信息，此时的 aReader 应该在最开头，也就是 {@code aReader.readLine()} 会得到一个空行，并且下一行是数据；
     * 读取完成后会跳过末尾的空行，也就是 {@code aReader.readLine()} 会得到下一个属性的字符串
     */
    private static void readMasses_(BufferedReader aReader, IVector rMasses) throws IOException {
        String tLine = findLineNonBlank_(aReader); if (tLine == null) return; // 跳过开头空行
        final int tAtomTypeNum = rMasses.size();
        for (int i = 0; i < tAtomTypeNum; ++i) {
            String[] tTokens = IO.Text.splitBlank(tLine);
            rMasses.set(Integer.parseInt(tTokens[0])-1, Double.parseDouble(tTokens[1]));
            tLine = aReader.readLine(); if (tLine == null) return;
        }
    }
    private static void readAtoms_(BufferedReader aReader, IIntVector rAtomID, IIntVector rAtomType, IMatrix rAtomXYZ, boolean aIsFull) throws IOException {
        String tLine = findLineNonBlank_(aReader); if (tLine == null) return; // 跳过开头空行
        final int tAtomNum = rAtomID.size();
        // 和坐标排序一致的顺序来存储，暂不存储 molecule-tag，q，nx，ny，nz
        for (int i = 0; i < tAtomNum; ++i) {
            IVector tIDTypeXYZ = IO.Text.str2data(tLine, aIsFull ? ATOM_DATA_KEYS_ID_TYPE_MOL_CHARGE_XYZ.length : ATOM_DATA_KEYS_ID_TYPE_XYZ.length);
            rAtomID.set(i, (int)tIDTypeXYZ.get(aIsFull ? LMPDAT_FULL_ID_COL : LMPDAT_ID_COL));
            rAtomType.set(i, (int)tIDTypeXYZ.get(aIsFull ? LMPDAT_FULL_TYPE_COL : LMPDAT_TYPE_COL));
            rAtomXYZ.set(i, XYZ_X_COL, tIDTypeXYZ.get(aIsFull ? LMPDAT_FULL_X_COL : LMPDAT_X_COL));
            rAtomXYZ.set(i, XYZ_Y_COL, tIDTypeXYZ.get(aIsFull ? LMPDAT_FULL_Y_COL : LMPDAT_Y_COL));
            rAtomXYZ.set(i, XYZ_Z_COL, tIDTypeXYZ.get(aIsFull ? LMPDAT_FULL_Z_COL : LMPDAT_Z_COL));
            tLine = aReader.readLine(); if (tLine == null) return;
        }
    }
    private static void readVelocities_(BufferedReader aReader, Map<Integer, Integer> aId2Row, IMatrix rVelocities) throws IOException {
        String tLine = findLineNonBlank_(aReader); if (tLine == null) return; // 跳过开头空行
        final int tAtomNum = rVelocities.rowNumber();
        // 和坐标排序一致的顺序来存储
        for (int i = 0; i < tAtomNum; ++i) {
            IVector tVelocity = IO.Text.str2data(tLine, LMPDAT_VELOCITY_LENGTH);
            int tRow = aId2Row.get((int)tVelocity.get(LMPDAT_ID_COL));
            rVelocities.set(tRow, STD_VX_COL, tVelocity.get(LMPDAT_VX_COL));
            rVelocities.set(tRow, STD_VY_COL, tVelocity.get(LMPDAT_VY_COL));
            rVelocities.set(tRow, STD_VZ_COL, tVelocity.get(LMPDAT_VZ_COL));
            tLine = aReader.readLine(); if (tLine == null) return;
        }
    }
    private static void readBonds_(BufferedReader aReader, Map<Integer, Integer> aId2Row, int aBondNum, IntList[] rBondID, IntList[] rBondType, IntList[] rBondIndex) throws IOException {
        String tLine = findLineNonBlank_(aReader); if (tLine == null) return; // 跳过开头空行
        // 和坐标排序一致的顺序来存储
        for (int i = 0; i < aBondNum; ++i) {
            IVector tIdTypeId1Id2 = IO.Text.str2data(tLine, LMPDAT_BOND_LENGTH);
            int tId = (int)tIdTypeId1Id2.get(LMPDAT_BOND_ID_COL);
            int tType = (int)tIdTypeId1Id2.get(LMPDAT_BOND_TYPE_COL);
            int tRow1 = aId2Row.get((int)tIdTypeId1Id2.get(LMPDAT_BOND_ID1_COL));
            int tRow2 = aId2Row.get((int)tIdTypeId1Id2.get(LMPDAT_BOND_ID2_COL));
            rBondID[tRow1].add(tId); rBondID[tRow2].add(tId);
            rBondType[tRow1].add(tType); rBondType[tRow2].add(tType);
            rBondIndex[tRow1].add(tRow2); rBondIndex[tRow2].add(tRow1);
            tLine = aReader.readLine(); if (tLine == null) return;
        }
    }
    private static void readElse_(BufferedReader aReader) throws IOException {
        String tLine = findLineNonBlank_(aReader); if (tLine == null) return; // 跳过开头空行
        // 其余不支持的情况直接跳过中间的非空行即可
        while ((tLine = aReader.readLine()) != null) {
            if (IO.Text.isBlank(tLine)) return;
        }
    }
    
    
    /**
     * 输出成 lammps 能够读取的 data 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {try (IO.IWriteln tWriteln = IO.toWriteln(aFilePath)) {write_(tWriteln);}}
    /** 改为 {@link IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用 */
    void write_(IO.IWriteln aWriteln) throws IOException {
        final boolean tHasBond = hasBond();
        aWriteln.writeln("LAMMPS data file generated by jse");
        aWriteln.writeln("");
        aWriteln.writeln(String.format("%6d atoms", mAtomNum));
        if (tHasBond) {
        aWriteln.writeln(String.format("%6d bonds", mBondNum));
        }
        aWriteln.writeln("");
        aWriteln.writeln(String.format("%6d atom types", mAtomTypeNum));
        if (tHasBond) {
        aWriteln.writeln(String.format("%6d bond types", mBondTypeNum));
        }
        aWriteln.writeln("");
        aWriteln.writeln(String.format("%24.18g  %24.18g xlo xhi", mBox.xlo(), mBox.xhi()));
        aWriteln.writeln(String.format("%24.18g  %24.18g ylo yhi", mBox.ylo(), mBox.yhi()));
        aWriteln.writeln(String.format("%24.18g  %24.18g zlo zhi", mBox.zlo(), mBox.zhi()));
        if (isPrism()) {
        aWriteln.writeln(String.format("%24.18g  %24.18g  %24.18g xy xz yz", mBox.xy(), mBox.xz(), mBox.yz()));
        }
        if (mMasses != null) {
        aWriteln.writeln("");
        aWriteln.writeln("Masses");
        aWriteln.writeln("");
        for (int type = 1; type <= mAtomTypeNum; ++type) {
        aWriteln.writeln(String.format("%6d  %24.18g", type, mass(type)));
        }}
        aWriteln.writeln("");
        aWriteln.writeln("Atoms # atomic");
        aWriteln.writeln("");
        for (int i = 0; i < mAtomNum; ++i) {
        aWriteln.writeln(String.format("%6d  %6d  %24.18g  %24.18g  %24.18g", mAtomID.get(i), mAtomType.get(i), mAtomXYZ.get(i, XYZ_X_COL), mAtomXYZ.get(i, XYZ_Y_COL), mAtomXYZ.get(i, XYZ_Z_COL)));
        }
        if (mVelocities != null) {
        aWriteln.writeln("");
        aWriteln.writeln("Velocities");
        aWriteln.writeln("");
        for (int i = 0; i < mAtomNum; ++i) {
        aWriteln.writeln(String.format("%6d  %24.18g  %24.18g  %24.18g", mAtomID.get(i), mVelocities.get(i, STD_VX_COL), mVelocities.get(i, STD_VY_COL), mVelocities.get(i, STD_VZ_COL)));
        }}
        if (tHasBond) {
        assert mBondIndex!=null && mBondID!=null && mBondType!=null;
        aWriteln.writeln("");
        aWriteln.writeln("Bonds");
        aWriteln.writeln("");
        for (int i = 0; i < mAtomNum; ++i) {
        int tID1 = mAtomID.get(i);
        int tNBonds = mBondIndex[i].size();
        for (int j = 0; j < tNBonds; ++j) {
        int tID2 = mAtomID.get(mBondIndex[i].get(j));
        if (tID1 <= tID2) {
        aWriteln.writeln(String.format("%6d  %6d  %6d  %6d", mBondID[i].get(j), mBondType[i].get(j), tID1, tID2));
        }}
        }}
    }
    
    
    
    /// MPI stuffs
    /** 用于 MPI 收发信息的 tags */
    private final static int
          LMPDAT_INFO = 200
        , DATA_MASSES = 205
        , DATA_XYZ = 201
        , DATA_ID = 203
        , DATA_TYPE = 204
        , DATA_VELOCITIES = 202
        ;
    /** [AtomNum | AtomTypeNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities | HasMass | PositionsIsCol | VelocitiesIsCol] */
    private final static int LMP_INFO_LEN = 8;
    /** 专门的方法用来收发 Lmpdat */
    public static void send(Lmpdat aLmpdat, int aDest, MPI.Comm aComm) throws MPIException {
        // 暂不支持正交盒以外的类型的发送
        if (aLmpdat.isPrism()) throw new UnsupportedOperationException("send is temporarily NOT support Prism Lmpdat");
        // 暂不支持 bond 的发送
        if (aLmpdat.hasBond()) throw new UnsupportedOperationException("send is temporarily NOT support Lmpdat with bonds");
        // 获取必要信息
        final boolean tHasVelocities = (aLmpdat.mVelocities != null);
        final boolean tHasMass = (aLmpdat.mMasses != null);
        // 先发送 Lmpdat 的必要信息，[AtomNum | AtomTypeNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities | HasMass | PositionsIsCol | VelocitiesIsCol]
        // 为了使用简单并且避免 double 转 long 造成的信息损耗，这里统一用 long[] 来传输信息
        aComm.send(new long[] {
              UT.Serial.combineI(aLmpdat.mAtomNum, aLmpdat.mAtomTypeNum)
            , Double.doubleToLongBits(aLmpdat.mBox.xlo())
            , Double.doubleToLongBits(aLmpdat.mBox.xhi())
            , Double.doubleToLongBits(aLmpdat.mBox.ylo())
            , Double.doubleToLongBits(aLmpdat.mBox.yhi())
            , Double.doubleToLongBits(aLmpdat.mBox.zlo())
            , Double.doubleToLongBits(aLmpdat.mBox.zhi())
            , UT.Serial.combineZ(tHasVelocities, tHasMass)
        }, 0, LMP_INFO_LEN, aDest, LMPDAT_INFO);
        // 必要信息发送完成后分别发送 masses, atomData 和 velocities
        if (tHasMass) {
            if (aLmpdat.mMasses instanceof IDataShell) {
                aComm.send((IDataShell<?>)aLmpdat.mMasses, aDest, DATA_MASSES);
            } else {
                Vector tBuf = VectorCache.getVec(aLmpdat.mMasses.size());
                tBuf.fill(aLmpdat.mMasses);
                aComm.send(tBuf, aDest, DATA_MASSES);
                VectorCache.returnVec(tBuf);
            }
        }
        aComm.send(aLmpdat.mAtomID  , aDest, DATA_ID  );
        aComm.send(aLmpdat.mAtomType, aDest, DATA_TYPE);
        aComm.send(aLmpdat.mAtomXYZ.asVecRow(), aDest, DATA_XYZ);
        // 如果有速度信息则需要再发送一次速度信息
        if (tHasVelocities) {
            aComm.send(aLmpdat.mVelocities.asVecRow(), aDest, DATA_VELOCITIES);
        }
    }
    public static Lmpdat recv(int aSource, MPI.Comm aComm) throws MPIException {
        // 同样先接收必要信息，[AtomNum | AtomTypeNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities | HasMass]
        long[] tLmpdatInfo = new long[LMP_INFO_LEN];
        aComm.recv(tLmpdatInfo, 0, LMP_INFO_LEN, aSource, LMPDAT_INFO);
        long tData = tLmpdatInfo[0];
        final int tAtomNum = UT.Serial.toIntL(tData, 0);
        final int tAtomTypeNum = UT.Serial.toIntL(tData, 1);
        tData = tLmpdatInfo[7];
        final boolean tHasVelocities = UT.Serial.toBooleanB((byte)tData, 0);
        final boolean tHasMasses = UT.Serial.toBooleanB((byte)tData, 1);
        // 现在和 read 保持一致不再使用缓存的数据
        final IntVector tAtomID = IntVector.zeros(tAtomNum);
        final IntVector tAtomType = IntVector.zeros(tAtomNum);
        final RowMatrix tAtomXYZ = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
        @Nullable RowMatrix tVelocities = null;
        @Nullable Vector tMasses = null;
        // 先是质量信息，基本信息，后是速度信息
        if (tHasMasses) {
            tMasses = Vectors.zeros(tAtomTypeNum);
            aComm.recv(tMasses, aSource, DATA_MASSES);
        }
        aComm.recv(tAtomID  , aSource, DATA_ID  );
        aComm.recv(tAtomType, aSource, DATA_TYPE);
        aComm.recv(tAtomXYZ.asVecRow(), aSource, DATA_XYZ);
        if (tHasVelocities) {
            tVelocities = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length);
            aComm.recv(tVelocities.asVecRow(), aSource, DATA_VELOCITIES);
        }
        // 创建 Lmpdat
        return new Lmpdat(tAtomTypeNum, new LmpBox(
            Double.longBitsToDouble(tLmpdatInfo[1]), Double.longBitsToDouble(tLmpdatInfo[2]),
            Double.longBitsToDouble(tLmpdatInfo[3]), Double.longBitsToDouble(tLmpdatInfo[4]),
            Double.longBitsToDouble(tLmpdatInfo[5]), Double.longBitsToDouble(tLmpdatInfo[6])
        ), tMasses, tAtomID, tAtomType, tAtomXYZ, tVelocities);
    }
    public static Lmpdat bcast(Lmpdat aLmpdat, int aRoot, MPI.Comm aComm) throws MPIException {
        if (aComm.rank() == aRoot) {
            // 暂不支持正交盒以外的类型的发送
            if (aLmpdat.isPrism()) throw new UnsupportedOperationException("bcast is temporarily NOT support Prism Lmpdat");
            // 暂不支持 bond 的发送
            if (aLmpdat.hasBond()) throw new UnsupportedOperationException("bcast is temporarily NOT support Lmpdat with bonds");
            // 获取必要信息
            final boolean tHasVelocities = aLmpdat.mVelocities != null;
            final boolean tHasMass = aLmpdat.mMasses != null;
            // 先发送 Lmpdat 的必要信息，[AtomNum | AtomTypeNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities | HasMass]
            aComm.bcast(new long[] {
                UT.Serial.combineI(aLmpdat.mAtomNum, aLmpdat.mAtomTypeNum)
                , Double.doubleToLongBits(aLmpdat.mBox.xlo())
                , Double.doubleToLongBits(aLmpdat.mBox.xhi())
                , Double.doubleToLongBits(aLmpdat.mBox.ylo())
                , Double.doubleToLongBits(aLmpdat.mBox.yhi())
                , Double.doubleToLongBits(aLmpdat.mBox.zlo())
                , Double.doubleToLongBits(aLmpdat.mBox.zhi())
                , UT.Serial.combineZ(tHasVelocities, tHasMass)
            }, 0, LMP_INFO_LEN, aRoot);
            // 必要信息发送完成后分别发送 masses, atomData 和 velocities
            if (tHasMass) {
                if (aLmpdat.mMasses instanceof IDataShell) {
                    aComm.bcast((IDataShell<?>)aLmpdat.mMasses, aRoot);
                } else {
                    Vector tBuf = VectorCache.getVec(aLmpdat.mMasses.size());
                    tBuf.fill(aLmpdat.mMasses);
                    aComm.bcast(tBuf, aRoot);
                    VectorCache.returnVec(tBuf);
                }
            }
            aComm.bcast(aLmpdat.mAtomID  , aRoot);
            aComm.bcast(aLmpdat.mAtomType, aRoot);
            aComm.bcast(aLmpdat.mAtomXYZ.asVecRow(), aRoot);
            // 如果有速度信息则需要再发送一次速度信息
            if (tHasVelocities) {
                aComm.bcast(aLmpdat.mVelocities.asVecRow(), aRoot);
            }
            return aLmpdat;
        } else {
            // 同样先接收必要信息，[AtomNum | AtomTypeNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, HasVelocities | HasMass]
            long[] tLmpdatInfo = new long[LMP_INFO_LEN];
            aComm.bcast(tLmpdatInfo, 0, LMP_INFO_LEN, aRoot);
            long tData = tLmpdatInfo[0];
            final int tAtomNum = UT.Serial.toIntL(tData, 0);
            final int tAtomTypeNum = UT.Serial.toIntL(tData, 1);
            tData = tLmpdatInfo[7];
            final boolean tHasVelocities = UT.Serial.toBooleanB((byte)tData, 0);
            final boolean tHasMasses = UT.Serial.toBooleanB((byte)tData, 1);
            // 现在和 read 保持一致不再使用缓存的数据
            final IntVector tAtomID = IntVector.zeros(tAtomNum);
            final IntVector tAtomType = IntVector.zeros(tAtomNum);
            final RowMatrix tAtomXYZ = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix tVelocities = null;
            @Nullable Vector tMasses = null;
            // 先是质量信息，基本信息，后是速度信息
            if (tHasMasses) {
                tMasses = Vectors.zeros(tAtomTypeNum);
                aComm.bcast(tMasses, aRoot);
            }
            aComm.bcast(tAtomID  , aRoot);
            aComm.bcast(tAtomType, aRoot);
            aComm.bcast(tAtomXYZ.asVecRow(), aRoot);
            if (tHasVelocities) {
                tVelocities = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length);
                aComm.bcast(tVelocities.asVecRow(), aRoot);
            }
            // 创建 Lmpdat
            return new Lmpdat(tAtomTypeNum, new LmpBox(
                Double.longBitsToDouble(tLmpdatInfo[1]), Double.longBitsToDouble(tLmpdatInfo[2]),
                Double.longBitsToDouble(tLmpdatInfo[3]), Double.longBitsToDouble(tLmpdatInfo[4]),
                Double.longBitsToDouble(tLmpdatInfo[5]), Double.longBitsToDouble(tLmpdatInfo[6])
            ), tMasses, tAtomID, tAtomType, tAtomXYZ, tVelocities);
        }
    }
}
