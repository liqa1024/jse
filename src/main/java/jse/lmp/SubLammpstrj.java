package jse.lmp;

import jse.atom.*;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.table.ITable;
import jse.math.table.Tables;
import jse.math.vector.IVector;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import static jse.code.CS.*;
import static jse.lmp.Lammpstrj.*;

/** 每个帧的子 Lammpstrj */
public class SubLammpstrj extends AbstractSettableAtomData {
    final static String[] BOX_BOUND = {"pp", "pp", "pp"};
    
    private long mTimeStep;
    private final String[] mBoxBounds;
    private final ITable mAtomData;
    private int mAtomTypeNum;
    private LmpBox mBox;
    
    private enum XYZType {
        NORMAL, SCALED, UNWRAPPED, SCALED_UNWRAPPED
    }
    
    private XYZType mXType = null, mYType = null, mZType = null;
    private String mKeyX = null, mKeyY = null, mKeyZ = null;
    private String mKeyVx = null, mKeyVy = null, mKeyVz = null;
    private String mKeyType = null, mKeyID = null;
    private final boolean mHasVelocities;
    
    /** 提供直接转为表格的接口 */
    public ITable asTable() {return mAtomData;}
    
    SubLammpstrj(long aTimeStep, String[] aBoxBounds, LmpBox aBox, ITable aAtomData) {
        mTimeStep = aTimeStep;
        mBoxBounds = aBoxBounds;
        mBox = aBox;
        mAtomData = aAtomData;
        
        for (int i = 0; i < aAtomData.columnNumber(); ++i) {
            String tKey = aAtomData.getHead(i);
            if (mKeyX == null) {
                if (tKey.equalsIgnoreCase("x")) {
                    mKeyX = tKey;
                    mXType = XYZType.NORMAL;
                } else
                if (tKey.equalsIgnoreCase("xs")) {
                    mKeyX = tKey;
                    mXType = XYZType.SCALED;
                } else
                if (tKey.equalsIgnoreCase("xu")) {
                    mKeyX = tKey;
                    mXType = XYZType.UNWRAPPED;
                } else
                if (tKey.equalsIgnoreCase("xsu")) {
                    mKeyX = tKey;
                    mXType = XYZType.SCALED_UNWRAPPED;
                }
            }
            if (mKeyY == null) {
                if (tKey.equalsIgnoreCase("y")) {
                    mKeyY = tKey;
                    mYType = XYZType.NORMAL;
                } else
                if (tKey.equalsIgnoreCase("ys")) {
                    mKeyY = tKey;
                    mYType = XYZType.SCALED;
                } else
                if (tKey.equalsIgnoreCase("yu")) {
                    mKeyY = tKey;
                    mYType = XYZType.UNWRAPPED;
                } else
                if (tKey.equalsIgnoreCase("ysu")) {
                    mKeyY = tKey;
                    mYType = XYZType.SCALED_UNWRAPPED;
                }
            }
            if (mKeyZ == null) {
                if (tKey.equalsIgnoreCase("z")) {
                    mKeyZ = tKey;
                    mZType = XYZType.NORMAL;
                } else
                if (tKey.equalsIgnoreCase("zs")) {
                    mKeyZ = tKey;
                    mZType = XYZType.SCALED;
                } else
                if (tKey.equalsIgnoreCase("zu")) {
                    mKeyZ = tKey;
                    mZType = XYZType.UNWRAPPED;
                } else
                if (tKey.equalsIgnoreCase("zsu")) {
                    mKeyZ = tKey;
                    mZType = XYZType.SCALED_UNWRAPPED;
                }
            }
            if (mKeyVx==null && tKey.equalsIgnoreCase("vx")) {mKeyVx = tKey;}
            if (mKeyVy==null && tKey.equalsIgnoreCase("vy")) {mKeyVy = tKey;}
            if (mKeyVz==null && tKey.equalsIgnoreCase("vz")) {mKeyVz = tKey;}
            if (mKeyID==null && tKey.equalsIgnoreCase("id")) {mKeyID = tKey;}
            if (mKeyType==null && tKey.equalsIgnoreCase("type")) {mKeyType = tKey;}
        }
        mHasVelocities = mKeyVx!=null || mKeyVy!=null || mKeyVz!=null;
        
        // 对于 dump，mAtomTypeNum 只能手动遍历统计
        int tAtomTypeNum = 1;
        if (mKeyType != null) {
            tAtomTypeNum = (int)mAtomData.col(mKeyType).max();
        }
        mAtomTypeNum = tAtomTypeNum;
    }
    
    // dump 额外的属性
    public long timeStep() {return mTimeStep;}
    public String[] boxBounds() {return mBoxBounds;}
    /** @deprecated use {@link #box} */ @Deprecated public LmpBox lmpBox() {return box();}
    
    public SubLammpstrj setTimeStep(long aTimeStep) {mTimeStep = aTimeStep; return this;}
    /** Groovy stuffs */
    @VisibleForTesting public long getTimeStep() {return mTimeStep;}
    
    @Override protected void setBox_(double aX, double aY, double aZ) {
        // 这里统一移除掉 boxlo 的数据，保证新的 box 合法性
        removeBoxLo_();
        mBox = new LmpBox(aX, aY, aZ);
    }
    @Override protected void setBox_(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {
        // 这里统一移除掉 boxlo 的数据，保证新的 box 合法性
        removeBoxLo_();
        mBox = new LmpBoxPrism(aX, aY, aZ, aXY, aXZ, aYZ);
    }
    @Override protected void setBox_(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        // 这里统一移除掉 boxlo 的数据，保证新的 box 合法性
        removeBoxLo_();
        mBox = LmpBox.of(new XYZ(aAx, aAy, aAz), new XYZ(aBx, aBy, aBz), new XYZ(aCx, aCy, aCz));
    }
    private void removeBoxLo_() {
        if (mKeyX != null) {
            switch (mXType) {
            case NORMAL: case UNWRAPPED: {
                mAtomData.col(mKeyX).minus2this(mBox.xlo());
                break;
            }
            case SCALED: case SCALED_UNWRAPPED: {break;}
            default: throw new RuntimeException();
            }
        }
        if (mKeyY != null) {
            switch (mYType) {
            case NORMAL: case UNWRAPPED: {
                mAtomData.col(mKeyY).minus2this(mBox.ylo());
                break;
            }
            case SCALED: case SCALED_UNWRAPPED: {break;}
            default: throw new RuntimeException();
            }
        }
        if (mKeyZ != null) {
            switch (mZType) {
            case NORMAL: case UNWRAPPED: {
                mAtomData.col(mKeyZ).minus2this(mBox.zlo());
                break;
            }
            case SCALED: case SCALED_UNWRAPPED: {break;}
            default: throw new RuntimeException();
            }
        }
    }
    @Override protected void scaleAtomPosition_(boolean aKeepAtomPosition, double aScale) {
        if (mKeyX != null) {
            switch (mXType) {
            case NORMAL: case UNWRAPPED: {
                if (aKeepAtomPosition) break;
                mAtomData.col(mKeyX).multiply2this(aScale);
                break;
            }
            case SCALED: case SCALED_UNWRAPPED: {
                // 注意对于 scaled 的情况，保持位置反而需要调整具体值
                if (!aKeepAtomPosition) break;
                mAtomData.col(mKeyX).div2this(aScale);
                break;
            }
            default: throw new RuntimeException();
            }
        }
        if (mKeyY != null) {
            switch (mYType) {
            case NORMAL: case UNWRAPPED: {
                if (aKeepAtomPosition) break;
                mAtomData.col(mKeyY).multiply2this(aScale);
                break;
            }
            case SCALED: case SCALED_UNWRAPPED: {
                // 注意对于 scaled 的情况，保持位置反而需要调整具体值
                if (!aKeepAtomPosition) break;
                mAtomData.col(mKeyY).div2this(aScale);
                break;
            }
            default: throw new RuntimeException();
            }
        }
        if (mKeyZ != null) {
            switch (mZType) {
            case NORMAL: case UNWRAPPED: {
                if (aKeepAtomPosition) break;
                mAtomData.col(mKeyZ).multiply2this(aScale);
                break;
            }
            case SCALED: case SCALED_UNWRAPPED: {
                // 注意对于 scaled 的情况，保持位置反而需要调整具体值
                if (!aKeepAtomPosition) break;
                mAtomData.col(mKeyZ).div2this(aScale);
                break;
            }
            default: throw new RuntimeException();
            }
        }
        if (aKeepAtomPosition) return;
        if (mKeyVx != null) mAtomData.col(mKeyVx).multiply2this(aScale);
        if (mKeyVy != null) mAtomData.col(mKeyVy).multiply2this(aScale);
        if (mKeyVz != null) mAtomData.col(mKeyVz).multiply2this(aScale);
    }
    @Override protected void validAtomPosition_(boolean aKeepAtomPosition, IBox aOldBox) {
        if (mKeyX == null) throw new UnsupportedOperationException("`setBox` with valid atom position for Lammpstrj without x data");
        if (mKeyY == null) throw new UnsupportedOperationException("`setBox` with valid atom position for Lammpstrj without y data");
        if (mKeyZ == null) throw new UnsupportedOperationException("`setBox` with valid atom position for Lammpstrj without z data");
        
        boolean tIsScaled = (mXType==XYZType.SCALED || mXType==XYZType.SCALED_UNWRAPPED) && (mYType==XYZType.SCALED || mYType==XYZType.SCALED_UNWRAPPED) && (mZType==XYZType.SCALED || mZType==XYZType.SCALED_UNWRAPPED);
        boolean tIsUnscaled = (mXType==XYZType.NORMAL || mXType==XYZType.UNWRAPPED) && (mYType==XYZType.NORMAL || mYType==XYZType.UNWRAPPED) && (mZType==XYZType.NORMAL || mZType==XYZType.UNWRAPPED);
        if (!tIsScaled && !tIsUnscaled) throw new UnsupportedOperationException("`setBox` with valid atom position for mix scaled/unscaled xyz data");
        
        final int tAtomNum = atomNumber();
        XYZ tBuf = new XYZ();
        // 先统一调整速度，速度总是没有 scaled
        if (!aKeepAtomPosition && mHasVelocities) {
            if (mBox.isPrism() || aOldBox.isPrism()) {
                for (int i = 0; i < tAtomNum; ++i) {
                    tBuf.setXYZ(mKeyVx==null ? 0.0 : mAtomData.get(i, mKeyVx), mKeyVy==null ? 0.0 : mAtomData.get(i, mKeyVy), mKeyVz==null ? 0.0 : mAtomData.get(i, mKeyVz));
                    aOldBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    if (mKeyVx != null) mAtomData.set(i, mKeyVx, tBuf.mX);
                    if (mKeyVy != null) mAtomData.set(i, mKeyVy, tBuf.mY);
                    if (mKeyVz != null) mAtomData.set(i, mKeyVz, tBuf.mZ);
                }
            } else {
                tBuf.setXYZ(mBox);
                tBuf.div2this(aOldBox);
                if (mKeyVx != null) mAtomData.col(mKeyVx).multiply2this(tBuf.mX);
                if (mKeyVy != null) mAtomData.col(mKeyVy).multiply2this(tBuf.mY);
                if (mKeyVz != null) mAtomData.col(mKeyVz).multiply2this(tBuf.mZ);
            }
        }
        // 对于 unscaled，保持原子位置不会进行任何操作
        if (tIsUnscaled) {
            if (aKeepAtomPosition) return;
            if (mBox.isPrism() || aOldBox.isPrism()) {
                for (int i = 0; i < tAtomNum; ++i) {
                    tBuf.setXYZ(mAtomData.get(i, mKeyX), mAtomData.get(i, mKeyY), mAtomData.get(i, mKeyZ));
                    aOldBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    mAtomData.set(i, mKeyX, tBuf.mX);
                    mAtomData.set(i, mKeyY, tBuf.mY);
                    mAtomData.set(i, mKeyZ, tBuf.mZ);
                }
            } else {
                tBuf.setXYZ(mBox);
                tBuf.div2this(aOldBox);
                mAtomData.col(mKeyX).multiply2this(tBuf.mX);
                mAtomData.col(mKeyY).multiply2this(tBuf.mY);
                mAtomData.col(mKeyZ).multiply2this(tBuf.mZ);
            }
            return;
        }
        // 对于 scaled，拉伸原子不会进行任何操作
        if (!aKeepAtomPosition) return;
        if (mBox.isPrism() || aOldBox.isPrism()) {
            for (int i = 0; i < tAtomNum; ++i) {
                tBuf.setXYZ(mAtomData.get(i, mKeyX), mAtomData.get(i, mKeyY), mAtomData.get(i, mKeyZ));
                // scaled 需要这样反向操作来保证原子位置不变
                aOldBox.toCartesian(tBuf);
                mBox.toDirect(tBuf);
                mAtomData.set(i, mKeyX, tBuf.mX);
                mAtomData.set(i, mKeyY, tBuf.mY);
                mAtomData.set(i, mKeyZ, tBuf.mZ);
            }
        } else {
            tBuf.setXYZ(aOldBox);
            tBuf.div2this(mBox);
            mAtomData.col(mKeyX).multiply2this(tBuf.mX);
            mAtomData.col(mKeyY).multiply2this(tBuf.mY);
            mAtomData.col(mKeyZ).multiply2this(tBuf.mZ);
        }
    }
    
    /**
     * 密度归一化
     * @return 返回自身来支持链式调用
     */
    @ApiStatus.Obsolete
    public SubLammpstrj setDenseNormalized() {
        if (mKeyX == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without x data");
        if (mKeyY == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without y data");
        if (mKeyZ == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without z data");
        
        double tScale = MathEX.Fast.cbrt(volume() / atomNumber());
        tScale = 1.0 / tScale;
        return (SubLammpstrj)setBoxScale(tScale);
    }
    
    
    /// AbstractAtomData stuffs
    /** @return {@inheritDoc} */
    @Override public boolean hasID() {return mKeyID!=null;}
    @Override public boolean hasVelocity() {return mHasVelocities;}
    /**
     * {@inheritDoc}
     * <p>
     * 现在 Lammpstrj 统一不对 unwrap 的原子坐标进行 wrap
     * 处理，保证对于 unwrap 的数据也会获取到 unwrap 的坐标；
     * 如果需要进行 wrap，可以调用 {@link ISettableAtomDataOperation#wrap2this()}
     * 来手动 warp
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see ISettableAtom
     * @see #atoms()
     */
    @Override public ISettableAtom atom(final int aIdx) {
        return new AbstractSettableAtom_() {
            @Override public int index() {return aIdx;}
            @Override public double x() {
                if (mKeyX == null) throw new UnsupportedOperationException("`x` for Lammpstrj without x data");
                double tX = mAtomData.get(aIdx, mKeyX);
                switch (mXType) {
                case NORMAL: case UNWRAPPED: {
                    return tX-mBox.xlo();
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (!isPrism()) {
                        return tX*mBox.x();
                    } else {
                        if (mKeyY==null || !(mYType==XYZType.SCALED || mYType==XYZType.SCALED_UNWRAPPED)) throw new UnsupportedOperationException("`x` for SCALED x in prism Lammpstrj without SCALED y data");
                        if (mKeyZ==null || !(mZType==XYZType.SCALED || mZType==XYZType.SCALED_UNWRAPPED)) throw new UnsupportedOperationException("`x` for SCALED x in prism Lammpstrj without SCALED z data");
                        double tY = mAtomData.get(aIdx, mKeyY);
                        double tZ = mAtomData.get(aIdx, mKeyZ);
                        return mBox.x()*tX + mBox.xy()*tY + mBox.xz()*tZ;
                    }
                }
                default: throw new RuntimeException();
                }
            }
            @Override public double y() {
                if (mKeyY == null) throw new UnsupportedOperationException("`y` for Lammpstrj without y data");
                double tY = mAtomData.get(aIdx, mKeyY);
                switch (mYType) {
                case NORMAL: case UNWRAPPED: {
                    return tY-mBox.ylo();
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (!isPrism()) {
                        return tY*mBox.y();
                    } else {
                        if (mKeyZ==null || !(mZType==XYZType.SCALED || mZType==XYZType.SCALED_UNWRAPPED)) throw new UnsupportedOperationException("`y` for SCALED y in prism Lammpstrj without SCALED z data");
                        double tZ = mAtomData.get(aIdx, mKeyZ);
                        return mBox.y()*tY + mBox.yz()*tZ;
                    }
                }
                default: throw new RuntimeException();
                }
            }
            @Override public double z() {
                if (mKeyZ == null) throw new UnsupportedOperationException("`z` for Lammpstrj without z data");
                double tZ = mAtomData.get(aIdx, mKeyZ);
                switch (mZType) {
                case NORMAL: case UNWRAPPED: {
                    return tZ-mBox.zlo();
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    return tZ*mBox.z();
                }
                default: throw new RuntimeException();
                }
            }
            
            /** 如果没有 id 数据则 id 为顺序位置 +1 */
            @Override protected int id_() {return mKeyID==null ? (aIdx+1) : (int)mAtomData.get(aIdx, mKeyID);}
            /** 如果没有 type 数据则 type 都为 1 */
            @Override protected int type_() {return mKeyType==null ? 1 : (int)mAtomData.get(aIdx, mKeyType);}
            /** 这里的速度是每个方向分别存储的，因此都需要判断一下 */
            @Override protected double vx_() {return mKeyVx==null ? 0.0 : mAtomData.get(aIdx, mKeyVx);}
            @Override protected double vy_() {return mKeyVy==null ? 0.0 : mAtomData.get(aIdx, mKeyVy);}
            @Override protected double vz_() {return mKeyVz==null ? 0.0 : mAtomData.get(aIdx, mKeyVz);}
            
            private final XYZ mBuf = new XYZ();
            @Override protected void setX_(double aX) {
                if (mKeyX == null) throw new UnsupportedOperationException("`setX` for Lammpstrj without x data");
                switch (mXType) {
                case NORMAL: case UNWRAPPED: {
                    mAtomData.set(aIdx, mKeyX, aX+mBox.xlo());
                    return;
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (!isPrism()) {
                        mAtomData.set(aIdx, mKeyX, aX/mBox.x());
                    } else {
                        // 这种情况下性能开销较大，因此需要 setXYZ 这种方法；
                        // 这里保持一致不对 lammps 风格的 box 做特殊的优化
                        mBuf.setXYZ(aX, y(), z());
                        mBox.toDirect(mBuf);
                        mAtomData.set(aIdx, mKeyX, mBuf.mX);
                        // lammps 风格的 box 此时不用设置 mZ 和 mY
                    }
                    return;
                }
                default: throw new RuntimeException();
                }
            }
            @Override protected void setY_(double aY) {
                if (mKeyY == null) throw new UnsupportedOperationException("`setY` for Lammpstrj without y data");
                switch (mYType) {
                case NORMAL: case UNWRAPPED: {
                    mAtomData.set(aIdx, mKeyY, aY+mBox.ylo());
                    return;
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (!isPrism()) {
                        mAtomData.set(aIdx, mKeyY, aY/mBox.y());
                    } else {
                        // 这种情况下性能开销较大，因此需要 setXYZ 这种方法；
                        // 这里保持一致不对 lammps 风格的 box 做特殊的优化
                        mBuf.setXYZ(x(), aY, z());
                        mBox.toDirect(mBuf);
                        // 原则上只有 scaled 的 x 需要额外设置
                        if (mXType==XYZType.SCALED || mXType==XYZType.SCALED_UNWRAPPED) mAtomData.set(aIdx, mKeyX, mBuf.mX);
                        mAtomData.set(aIdx, mKeyY, mBuf.mY);
                        // lammps 风格的 box 此时不用设置 mZ
                    }
                    return;
                }
                default: throw new RuntimeException();
                }
            }
            @Override protected void setZ_(double aZ) {
                if (mKeyZ == null) throw new UnsupportedOperationException("`setZ` for Lammpstrj without z data");
                switch (mZType) {
                case NORMAL: case UNWRAPPED: {
                    mAtomData.set(aIdx, mKeyZ, aZ+mBox.zlo());
                    return;
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (!isPrism()) {
                        mAtomData.set(aIdx, mKeyZ, aZ/mBox.z());
                    } else {
                        // 这种情况下性能开销较大，因此需要 setXYZ 这种方法；
                        // 这里保持一致不对 lammps 风格的 box 做特殊的优化
                        mBuf.setXYZ(x(), y(), aZ);
                        mBox.toDirect(mBuf);
                        // 原则上只有 scaled 的 x, y 需要额外设置
                        if (mXType==XYZType.SCALED || mXType==XYZType.SCALED_UNWRAPPED) mAtomData.set(aIdx, mKeyX, mBuf.mX);
                        if (mYType==XYZType.SCALED || mYType==XYZType.SCALED_UNWRAPPED) mAtomData.set(aIdx, mKeyY, mBuf.mY);
                        mAtomData.set(aIdx, mKeyZ, mBuf.mZ);
                    }
                    return;
                }
                default: throw new RuntimeException();
                }
            }
            @Override public ISettableAtom setXYZ(double aX, double aY, double aZ) {
                if (!isPrism() ||
                      ((mXType==XYZType.NORMAL || mXType==XYZType.UNWRAPPED)
                    && (mYType==XYZType.NORMAL || mYType==XYZType.UNWRAPPED)
                    && (mZType==XYZType.NORMAL || mZType==XYZType.UNWRAPPED))) {
                    return setX(aX).setY(aY).setZ(aZ);
                } else {
                    // 此时 setXYZ 反而性能开销更小
                    mBuf.setXYZ(aX, aY, aZ);
                    mBox.toDirect(mBuf);
                    // 根据类型选择需要设置的值
                    switch(mXType) {
                    case NORMAL: case UNWRAPPED:        {mAtomData.set(aIdx, mKeyX, aX+mBox.xlo()); break;}
                    case SCALED: case SCALED_UNWRAPPED: {mAtomData.set(aIdx, mKeyX,       mBuf.mX); break;}
                    default: throw new RuntimeException();
                    }
                    switch(mYType) {
                    case NORMAL: case UNWRAPPED:        {mAtomData.set(aIdx, mKeyY, aY+mBox.ylo()); break;}
                    case SCALED: case SCALED_UNWRAPPED: {mAtomData.set(aIdx, mKeyY,       mBuf.mY); break;}
                    default: throw new RuntimeException();
                    }
                    switch(mZType) {
                    case NORMAL: case UNWRAPPED:        {mAtomData.set(aIdx, mKeyZ, aZ+mBox.zlo()); break;}
                    case SCALED: case SCALED_UNWRAPPED: {mAtomData.set(aIdx, mKeyZ,       mBuf.mZ); break;}
                    default: throw new RuntimeException();
                    }
                    return this;
                }
            }
            
            @Override protected void setID_(int aID) {
                if (mKeyID == null) throw new UnsupportedOperationException("`setID` for Lammpstrj without id");
                mAtomData.set(aIdx, mKeyID, aID);
            }
            @Override protected void setType_(int aType) {
                if (mKeyType == null) throw new UnsupportedOperationException("`setType` for Lammpstrj without type");
                mAtomData.set(aIdx, mKeyType, aType);
            }
            /** 这里的速度是每个方向分别存储的，因此都需要判断一下 */
            @Override protected void setVx_(double aVx) {
                if (mKeyVx == null) throw new UnsupportedOperationException("`setVx` for Lammpstrj without vx");
                mAtomData.set(aIdx, mKeyVx, aVx);
            }
            @Override protected void setVy_(double aVy) {
                if (mKeyVy == null) throw new UnsupportedOperationException("`setVy` for Lammpstrj without vy");
                mAtomData.set(aIdx, mKeyVy, aVy);
            }
            @Override protected void setVz_(double aVz) {
                if (mKeyVz == null) throw new UnsupportedOperationException("`setVz` for Lammpstrj without vy");
                mAtomData.set(aIdx, mKeyVz, aVz);
            }
        };
    }
    @Override public LmpBox box() {return mBox;}
    @Override public int atomNumber() {return mAtomData.rowNumber();}
    @Override public int atomTypeNumber() {return mAtomTypeNum;}
    @Override public SubLammpstrj setAtomTypeNumber(int aAtomTypeNum) {
        int oTypeNum = mAtomTypeNum;
        if (aAtomTypeNum == oTypeNum) return this;
        mAtomTypeNum = aAtomTypeNum;
        if (aAtomTypeNum<oTypeNum && mKeyType!=null) {
            // 现在支持设置更小的值，更大的种类会直接截断
            mAtomData.col(mKeyType).opt().map2this(v -> Math.min(v, aAtomTypeNum));
            return this;
        }
        return this;
    }
    
    @Override public SubLammpstrj copy() {return new SubLammpstrj(mTimeStep, Arrays.copyOf(mBoxBounds, mBoxBounds.length), mBox.copy(), mAtomData.copy());}
    // 由于 SubLammpstrj 不一定全都可以修改，因此不重写另外两个
    
    
    /// 创建 SubLammpstrj
    public static SubLammpstrj fromAtomData(IAtomData aAtomData) {
        return fromAtomData(aAtomData, getTimeStep(aAtomData, 0));
    }
    public static SubLammpstrj fromAtomData(IAtomData aAtomData, long aTimeStep) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof SubLammpstrj) {
            // SubLammpstrj 则直接获取即可（专门优化，保留排序，具体坐标的形式，对应的标签等）
            SubLammpstrj tSubLammpstrj = (SubLammpstrj)aAtomData;
            return new SubLammpstrj(aTimeStep, Arrays.copyOf(tSubLammpstrj.mBoxBounds, tSubLammpstrj.mBoxBounds.length), tSubLammpstrj.mBox.copy(), tSubLammpstrj.mAtomData.copy());
        } else {
            final int tAtomNum = aAtomData.atomNumber();
            ITable rAtomData;
            // 一般的情况，需要考虑斜方的模拟盒的情况
            IBox tBox = aAtomData.box();
            LmpBox rBox = LmpBox.of(tBox);
            if (tBox.isLmpStyle()) {
                // 模拟盒满足 lammps 种类下可以直接拷贝过来
                if (aAtomData.hasVelocity()) {
                    rAtomData = Tables.zeros(tAtomNum, ALL_ATOM_DATA_KEYS);
                    IMatrix rMat = rAtomData.asMatrix();
                    for (int i = 0; i < tAtomNum; ++i) {
                        IAtom tAtom = aAtomData.atom(i);
                        rMat.set(i, ALL_ID_COL, tAtom.id());
                        rMat.set(i, ALL_TYPE_COL, tAtom.type());
                        rMat.set(i, ALL_X_COL, tAtom.x());
                        rMat.set(i, ALL_Y_COL, tAtom.y());
                        rMat.set(i, ALL_Z_COL, tAtom.z());
                        rMat.set(i, ALL_VX_COL, tAtom.vx());
                        rMat.set(i, ALL_VY_COL, tAtom.vy());
                        rMat.set(i, ALL_VZ_COL, tAtom.vz());
                    }
                } else {
                    rAtomData = Tables.zeros(tAtomNum, STD_ATOM_DATA_KEYS);
                    IMatrix rMat = rAtomData.asMatrix();
                    for (int i = 0; i < tAtomNum; ++i) {
                        IAtom tAtom = aAtomData.atom(i);
                        rMat.set(i, STD_ID_COL, tAtom.id());
                        rMat.set(i, STD_TYPE_COL, tAtom.type());
                        rMat.set(i, STD_X_COL, tAtom.x());
                        rMat.set(i, STD_Y_COL, tAtom.y());
                        rMat.set(i, STD_Z_COL, tAtom.z());
                    }
                }
            } else {
                // 否则需要转换成 lammps 的种类
                XYZ tBuf = new XYZ();
                if (aAtomData.hasVelocity()) {
                    rAtomData = Tables.zeros(tAtomNum, ALL_ATOM_DATA_KEYS);
                    IMatrix rMat = rAtomData.asMatrix();
                    for (int i = 0; i < tAtomNum; ++i) {
                        IAtom tAtom = aAtomData.atom(i);
                        rMat.set(i, ALL_ID_COL, tAtom.id());
                        rMat.set(i, ALL_TYPE_COL, tAtom.type());
                        tBuf.setXYZ(tAtom);
                        tBox.toDirect(tBuf);
                        rBox.toCartesian(tBuf);
                        rMat.set(i, ALL_X_COL, tBuf.mX);
                        rMat.set(i, ALL_Y_COL, tBuf.mY);
                        rMat.set(i, ALL_Z_COL, tBuf.mZ);
                        // 对于速度也使用同样的变换
                        tBuf.setXYZ(tAtom.vx(), tAtom.vy(), tAtom.vz());
                        tBox.toDirect(tBuf);
                        rBox.toCartesian(tBuf);
                        rMat.set(i, ALL_VX_COL, tBuf.mX);
                        rMat.set(i, ALL_VY_COL, tBuf.mY);
                        rMat.set(i, ALL_VZ_COL, tBuf.mZ);
                    }
                } else {
                    rAtomData = Tables.zeros(tAtomNum, STD_ATOM_DATA_KEYS);
                    IMatrix rMat = rAtomData.asMatrix();
                    for (int i = 0; i < tAtomNum; ++i) {
                        IAtom tAtom = aAtomData.atom(i);
                        rMat.set(i, STD_ID_COL, tAtom.id());
                        rMat.set(i, STD_TYPE_COL, tAtom.type());
                        tBuf.setXYZ(tAtom);
                        tBox.toDirect(tBuf);
                        rBox.toCartesian(tBuf);
                        rMat.set(i, STD_X_COL, tBuf.mX);
                        rMat.set(i, STD_Y_COL, tBuf.mY);
                        rMat.set(i, STD_Z_COL, tBuf.mZ);
                    }
                }
            }
            return new SubLammpstrj(aTimeStep, BOX_BOUND, rBox, rAtomData);
        }
    }
    static long getTimeStep(IAtomData aAtomData, long aDefault) {
        if (aAtomData instanceof SubLammpstrj) return ((SubLammpstrj)aAtomData).mTimeStep;
        return aDefault;
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static SubLammpstrj of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static SubLammpstrj of(IAtomData aAtomData, long aTimeStep) {return fromAtomData(aAtomData, aTimeStep);}
    
    
    /// 文件读写
    /**
     * 从文件 lammps 输出的 dump 文件中读取来实现初始化
     * @author liqa
     * @param aFilePath lammps 输出的 dump 文件路径
     * @return 读取得到的 SubLammpstrj 对象，只会读取第一帧，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static SubLammpstrj read(String aFilePath) throws IOException {try (BufferedReader tReader = UT.IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用；不会自动关闭流，只读取一帧的数据然后停止读取 */
    static SubLammpstrj read_(BufferedReader aReader) throws IOException {
        String tLine;
        String[] tTokens;
        
        long aTimeStep;
        int tAtomNum;
        String[] aBoxBounds;
        LmpBox aBox;
        final ITable aAtomData;
        
        // 读取时间步数
        UT.Text.findLineContaining(aReader, "ITEM: TIMESTEP", true); tLine=aReader.readLine();
        if (tLine == null) return null;
        tTokens = UT.Text.splitBlank(tLine);
        aTimeStep = Long.parseLong(tTokens[0]);
        // 读取原子总数
        UT.Text.findLineContaining(aReader, "ITEM: NUMBER OF ATOMS", true); tLine=aReader.readLine();
        if (tLine == null) return null;
        tTokens = UT.Text.splitBlank(tLine);
        tAtomNum = Integer.parseInt(tTokens[0]);
        // 读取模拟盒信息
        tLine = UT.Text.findLineContaining(aReader, "ITEM: BOX BOUNDS", true);
        if (tLine == null) return null;
        tTokens = UT.Text.splitBlank(tLine);
        // 斜方支持
        if (tTokens[3].equalsIgnoreCase("xy")) {
            aBoxBounds = new String[] {tTokens[6], tTokens[7], tTokens[8]};
            tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
            double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]); double aXY = Double.parseDouble(tTokens[2]);
            tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
            double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]); double aXZ = Double.parseDouble(tTokens[2]);
            tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
            double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]); double aYZ = Double.parseDouble(tTokens[2]);
            // 注意 dump 和 data 斜方格式不同，需要转换
            aXlo -= Math.min(Math.min(0.0, aXY), Math.min(aXZ, aXY+aXZ));
            aXhi -= Math.max(Math.max(0.0, aXY), Math.max(aXZ, aXY+aXZ));
            aYlo -= Math.min(0.0, aYZ);
            aYhi -= Math.max(0.0, aYZ);
            aBox = new LmpBoxPrism(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi, aXY, aXZ, aYZ);
        } else {
            aBoxBounds = new String[] {tTokens[3], tTokens[4], tTokens[5]};
            tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
            double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]);
            tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
            double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]);
            tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
            double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]);
            aBox = new LmpBox(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi);
        }
        
        // 读取原子信息
        tLine = UT.Text.findLineContaining(aReader, "ITEM: ATOMS", true);
        if (tLine == null) return null;
        tTokens = UT.Text.splitBlank(tLine);
        String[] tAtomDataKeys = new String[tTokens.length-2];
        System.arraycopy(tTokens, 2, tAtomDataKeys, 0, tAtomDataKeys.length);
        boolean tIsAtomDataReadFull = true;
        aAtomData = Tables.zeros(tAtomNum, tAtomDataKeys);
        for (IVector tRow : aAtomData.rows()) {
            tLine = aReader.readLine();
            if (tLine == null) {tIsAtomDataReadFull = false; break;}
            tRow.fill(UT.Text.str2data(tLine, tAtomDataKeys.length));
        }
        if (!tIsAtomDataReadFull) return null;
        
        // 创建 SubLammpstrj 并返回
        return new SubLammpstrj(aTimeStep, aBoxBounds, aBox, aAtomData);
    }
    
    /**
     * 输出成 lammps 格式的 dump 文件，可以供 OVITO 等软件读取
     * <p>
     * 改为 {@link UT.IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {write_(tWriteln);}}
    /** 改为 {@link UT.IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用；不会自动关闭流，只写入一帧的数据然后停止写入 */
    void write_(UT.IO.IWriteln aWriteln) throws IOException {
        aWriteln.writeln("ITEM: TIMESTEP");
        aWriteln.writeln(String.format("%d", mTimeStep));
        aWriteln.writeln("ITEM: NUMBER OF ATOMS");
        aWriteln.writeln(String.format("%d", atomNumber()));
        if (!isPrism()) {
        aWriteln.writeln(String.format("ITEM: BOX BOUNDS %s", String.join(" ", boxBounds())));
        aWriteln.writeln(String.format("%f %f", mBox.xlo(), mBox.xhi()));
        aWriteln.writeln(String.format("%f %f", mBox.ylo(), mBox.yhi()));
        aWriteln.writeln(String.format("%f %f", mBox.zlo(), mBox.zhi()));
        } else {
        aWriteln.writeln(String.format("ITEM: BOX BOUNDS xy xz yz %s", String.join(" ", boxBounds())));
        double tXlo = mBox.xlo(), tYlo = mBox.ylo(), tZlo = mBox.zlo();
        double tXhi = mBox.xhi(), tYhi = mBox.yhi(), tZhi = mBox.zhi();
        double tXY  = mBox.xy() , tXZ  = mBox.xz() , tYZ  = mBox.yz() ;
        tXlo += Math.min(Math.min(0.0, tXY), Math.min(tXZ, tXY+tXZ));
        tXhi += Math.max(Math.max(0.0, tXY), Math.max(tXZ, tXY+tXZ));
        tYlo += Math.min(0.0, tYZ);
        tYhi += Math.max(0.0, tYZ);
        aWriteln.writeln(String.format("%f %f %f", tXlo, tXhi, tXY));
        aWriteln.writeln(String.format("%f %f %f", tYlo, tYhi, tXZ));
        aWriteln.writeln(String.format("%f %f %f", tZlo, tZhi, tYZ));
        }
        aWriteln.writeln(String.format("ITEM: ATOMS %s", String.join(" ", mAtomData.heads())));
        for (IVector subAtomData : mAtomData.rows()) {
        aWriteln.writeln(String.join(" ", AbstractCollections.map(subAtomData, SubLammpstrj::double2str_)));
        }
    }
    /** 保证整数时直接输出整数 */
    private static String double2str_(double aValue) {
        int tIntValue = (int)aValue;
        if (tIntValue == aValue) return String.valueOf(tIntValue);
        return Double.toString(aValue);
    }
    
    
    /// MPI stuffs
    /** [AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep] */
    private final static int LAMMPSTRJ_INFO_LEN = 8;
    /** 专门的方法用来收发 SubLammpstrj */
    public static void send(SubLammpstrj aSubLammpstrj, int aDest, MPI.Comm aComm) throws MPIException {
        // 暂不支持周期边界以外的类型的发送
        if (!aSubLammpstrj.mBoxBounds[0].equalsIgnoreCase("pp") || !aSubLammpstrj.mBoxBounds[1].equalsIgnoreCase("pp") || !aSubLammpstrj.mBoxBounds[2].equalsIgnoreCase("pp")) {
            throw new UnsupportedOperationException("send is temporarily support `pp pp pp` BoxBounds only");
        }
        // 暂不支持正交盒以外的类型的发送
        if (aSubLammpstrj.isPrism()) {
            throw new UnsupportedOperationException("send is temporarily NOT support Prism Lammpstrj");
        }
        // 先发送 SubLammpstrj 的必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
        // 为了使用简单并且避免 double 转 long 造成的信息损耗，这里统一用 long[] 来传输信息
        aComm.send(new long[] {
              UT.Serial.combineI(aSubLammpstrj.atomNumber(), aSubLammpstrj.mAtomData.columnNumber())
            , Double.doubleToLongBits(aSubLammpstrj.mBox.xlo())
            , Double.doubleToLongBits(aSubLammpstrj.mBox.xhi())
            , Double.doubleToLongBits(aSubLammpstrj.mBox.ylo())
            , Double.doubleToLongBits(aSubLammpstrj.mBox.yhi())
            , Double.doubleToLongBits(aSubLammpstrj.mBox.zlo())
            , Double.doubleToLongBits(aSubLammpstrj.mBox.zhi())
            , aSubLammpstrj.mTimeStep
        }, LAMMPSTRJ_INFO_LEN, aDest, LAMMPSTRJ_INFO);
        // 必要信息发送完成后分别发送 atomDataKeys 和 atomData，这里按列发送，先统一发送 key 再统一发送数据
        for (String subDataKey : aSubLammpstrj.mAtomData.heads()) aComm.sendStr(subDataKey, aDest, DATA_KEY);
        for (IVector subData : aSubLammpstrj.mAtomData.asMatrix().cols()) aComm.send(subData, aDest, DATA);
    }
    public static SubLammpstrj recv(int aSource, MPI.Comm aComm) throws MPIException {
        // 同样先接收必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
        long[] tLammpstrjInfo = new long[LAMMPSTRJ_INFO_LEN];
        aComm.recv(tLammpstrjInfo, LAMMPSTRJ_INFO_LEN, aSource, LAMMPSTRJ_INFO);
        long tData = tLammpstrjInfo[0];
        final int tAtomNum = UT.Serial.toIntL(tData, 0);
        final int tAtomDataKeyNum = UT.Serial.toIntL(tData, 1);
        final long tTimeStep = tLammpstrjInfo[7];
        // 由于 Table 可以扩容，并且要和和 read 保持一致，不使用缓存的数据
        String[] tAtomDataKeys = new String[tAtomDataKeyNum];
        for (int i = 0; i < tAtomDataKeyNum; ++i) tAtomDataKeys[i] = aComm.recvStr(aSource, DATA_KEY);
        ITable rAtomData = Tables.zeros(tAtomNum, tAtomDataKeys);
        for (IVector subData : rAtomData.asMatrix().cols()) aComm.recv(subData, aSource, DATA);
        // 创建 SubLammpstrj
        return new SubLammpstrj(tTimeStep, BOX_BOUND, new LmpBox(
            Double.longBitsToDouble(tLammpstrjInfo[1]), Double.longBitsToDouble(tLammpstrjInfo[2]),
            Double.longBitsToDouble(tLammpstrjInfo[3]), Double.longBitsToDouble(tLammpstrjInfo[4]),
            Double.longBitsToDouble(tLammpstrjInfo[5]), Double.longBitsToDouble(tLammpstrjInfo[6])
        ), rAtomData);
    }
    public static SubLammpstrj bcast(SubLammpstrj aSubLammpstrj, int aRoot, MPI.Comm aComm) throws MPIException {
        if (aComm.rank() == aRoot) {
            // 暂不支持周期边界以外的类型的发送
            if (!aSubLammpstrj.mBoxBounds[0].equalsIgnoreCase("pp") || !aSubLammpstrj.mBoxBounds[1].equalsIgnoreCase("pp") || !aSubLammpstrj.mBoxBounds[2].equalsIgnoreCase("pp")) {
                throw new UnsupportedOperationException("bcast is temporarily support `pp pp pp` BoxBounds only");
            }
            // 暂不支持正交盒以外的类型的发送
            if (aSubLammpstrj.isPrism()) {
                throw new UnsupportedOperationException("send is temporarily NOT support Prism Lammpstrj");
            }
            // 先发送 SubLammpstrj 的必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
            aComm.bcast(new long[] {
                  UT.Serial.combineI(aSubLammpstrj.atomNumber(), aSubLammpstrj.mAtomData.columnNumber())
                , Double.doubleToLongBits(aSubLammpstrj.mBox.xlo())
                , Double.doubleToLongBits(aSubLammpstrj.mBox.xhi())
                , Double.doubleToLongBits(aSubLammpstrj.mBox.ylo())
                , Double.doubleToLongBits(aSubLammpstrj.mBox.yhi())
                , Double.doubleToLongBits(aSubLammpstrj.mBox.zlo())
                , Double.doubleToLongBits(aSubLammpstrj.mBox.zhi())
                , aSubLammpstrj.mTimeStep
            }, LAMMPSTRJ_INFO_LEN, aRoot);
            // 必要信息发送完成后分别发送 atomDataKeys 和 atomData，这里按列发送，先统一发送 key 再统一发送数据
            for (String subDataKey : aSubLammpstrj.mAtomData.heads()) aComm.bcastStr(subDataKey, aRoot);
            for (IVector subData : aSubLammpstrj.mAtomData.asMatrix().cols()) aComm.bcast(subData, aRoot);
            return aSubLammpstrj;
        } else {
            // 同样先接收必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
            long[] tLammpstrjInfo = new long[LAMMPSTRJ_INFO_LEN];
            aComm.bcast(tLammpstrjInfo, LAMMPSTRJ_INFO_LEN, aRoot);
            long tData = tLammpstrjInfo[0];
            final int tAtomNum = UT.Serial.toIntL(tData, 0);
            final int tAtomDataKeyNum = UT.Serial.toIntL(tData, 1);
            final long tTimeStep = tLammpstrjInfo[7];
            // 由于 Table 可以扩容，并且要和和 read 保持一致，不使用缓存的数据
            String[] tAtomDataKeys = new String[tAtomDataKeyNum];
            for (int i = 0; i < tAtomDataKeyNum; ++i) tAtomDataKeys[i] = aComm.bcastStr(null, aRoot);
            ITable rAtomData = Tables.zeros(tAtomNum, tAtomDataKeys);
            for (IVector subData : rAtomData.asMatrix().cols()) aComm.bcast(subData, aRoot);
            // 创建 SubLammpstrj
            return new SubLammpstrj(tTimeStep, BOX_BOUND, new LmpBox(
                Double.longBitsToDouble(tLammpstrjInfo[1]), Double.longBitsToDouble(tLammpstrjInfo[2]),
                Double.longBitsToDouble(tLammpstrjInfo[3]), Double.longBitsToDouble(tLammpstrjInfo[4]),
                Double.longBitsToDouble(tLammpstrjInfo[5]), Double.longBitsToDouble(tLammpstrjInfo[6])
            ), rAtomData);
        }
    }
}
