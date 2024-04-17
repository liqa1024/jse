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
    
    /** 修改模拟盒类型 */
    public SubLammpstrj setBoxNormal() {
        if (mKeyX == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without x data");
        if (mKeyY == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without y data");
        if (mKeyZ == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without z data");
        
        if (!isPrism()) return this;
        LmpBox oBox = mBox;
        mBox = new LmpBox(mBox);
        // 如果原本的斜方模拟盒不存在斜方数据则直接返回
        if (MathEX.Code.numericEqual(oBox.xy(), 0.0) && MathEX.Code.numericEqual(oBox.xz(), 0.0) && MathEX.Code.numericEqual(oBox.yz(), 0.0)) return this;
        // 否则将原子进行线性变换
        XYZ tBuf = new XYZ();
        final int tAtomNum = atomNumber();
        for (int i = 0; i < tAtomNum; ++i) {
            ISettableAtom tAtom = atom(i);
            tBuf.setXYZ(tAtom);
            // 这样转换两次即可实现线性变换
            oBox.toDirect(tBuf);
            mBox.toCartesian(tBuf);
            tAtom.setXYZ(tBuf);
            // 如果存在速度，则速度也需要做一次这样的变换
            if (mHasVelocities) {
                tBuf.setXYZ(tAtom.vx(), tAtom.vy(), tAtom.vz());
                oBox.toDirect(tBuf);
                mBox.toCartesian(tBuf);
                if (mKeyVx != null) tAtom.setVx(tBuf.mX);
                if (mKeyVy != null) tAtom.setVy(tBuf.mY);
                if (mKeyVz != null) tAtom.setVz(tBuf.mZ);
            }
        }
        return this;
    }
    public SubLammpstrj setBoxPrism() {return setBoxPrism(0.0, 0.0, 0.0);}
    public SubLammpstrj setBoxPrism(double aXY, double aXZ, double aYZ) {
        if (mKeyX == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without x data");
        if (mKeyY == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without y data");
        if (mKeyZ == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without z data");
        
        LmpBox oBox = mBox;
        mBox = new LmpBoxPrism(mBox, aXY, aXZ, aYZ);
        // 现在必须要求三个倾斜因子相同才可以跳过设置
        if (MathEX.Code.numericEqual(oBox.xy(), aXY) && MathEX.Code.numericEqual(oBox.xz(), aXZ) && MathEX.Code.numericEqual(oBox.yz(), aYZ)) return this;
        // 否则将原子进行线性变换
        XYZ tBuf = new XYZ();
        final int tAtomNum = atomNumber();
        for (int i = 0; i < tAtomNum; ++i) {
            ISettableAtom tAtom = atom(i);
            tBuf.setXYZ(tAtom);
            // 这样转换两次即可实现线性变换
            oBox.toDirect(tBuf);
            mBox.toCartesian(tBuf);
            tAtom.setXYZ(tBuf);
            // 如果存在速度，则速度也需要做一次这样的变换
            if (mHasVelocities) {
                tBuf.setXYZ(tAtom.vx(), tAtom.vy(), tAtom.vz());
                oBox.toDirect(tBuf);
                mBox.toCartesian(tBuf);
                if (mKeyVx != null) tAtom.setVx(tBuf.mX);
                if (mKeyVy != null) tAtom.setVy(tBuf.mY);
                if (mKeyVz != null) tAtom.setVz(tBuf.mZ);
            }
        }
        return this;
    }
    /** 调整模拟盒的 xyz 长度 */
    public SubLammpstrj setBoxXYZ(double aX, double aY, double aZ) {
        if (mKeyX == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without x data");
        if (mKeyY == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without y data");
        if (mKeyZ == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without z data");
        
        LmpBox oBox = mBox;
        mBox = oBox.isPrism() ?
            new LmpBoxPrism(aX, aY, aZ, oBox.xy(), oBox.xz(), oBox.yz()) :
            new LmpBox(aX, aY, aZ);
        // 现在必须要求 xyz 长度相同才可以跳过设置，需要注意 lo 之类的也要为 0，这里通过比较 hi 也相等来实现，可以包含更多情况
        if (MathEX.Code.numericEqual(oBox.x(), aX) && MathEX.Code.numericEqual(oBox.y(), aY) && MathEX.Code.numericEqual(oBox.z(), aZ)
         && MathEX.Code.numericEqual(oBox.xhi(), aX) && MathEX.Code.numericEqual(oBox.yhi(), aY) && MathEX.Code.numericEqual(oBox.zhi(), aZ)) return this;
        // 这里顺便也会移除掉 boxlo 的数据，因此不使用 atom 修改
        switch (mXType) {
        case NORMAL: case UNWRAPPED: {
            IVector tCol = mAtomData.col(mKeyX);
            tCol.minus2this(mBox.xlo());
            break;
        }
        case SCALED: case SCALED_UNWRAPPED: {break;}
        default: throw new RuntimeException();
        }
        switch (mYType) {
        case NORMAL: case UNWRAPPED: {
            IVector tCol = mAtomData.col(mKeyY);
            tCol.minus2this(mBox.ylo());
            break;
        }
        case SCALED: case SCALED_UNWRAPPED: {break;}
        default: throw new RuntimeException();
        }
        switch (mZType) {
        case NORMAL: case UNWRAPPED: {
            IVector tCol = mAtomData.col(mKeyZ);
            tCol.minus2this(mBox.zlo());
            break;
        }
        case SCALED: case SCALED_UNWRAPPED: {break;}
        default: throw new RuntimeException();
        }
        if (oBox.isPrism()) {
            // 这里依旧使用 atom 来方便获取和修改数据
            XYZ tBuf = new XYZ();
            final int tAtomNum = atomNumber();
            for (int i = 0; i < tAtomNum; ++i) {
                ISettableAtom tAtom = atom(i);
                tBuf.setXYZ(tAtom);
                // 这样转换两次即可实现线性变换
                oBox.toDirect(tBuf);
                mBox.toCartesian(tBuf);
                tAtom.setXYZ(tBuf);
                // 如果存在速度，则速度也需要做一次这样的变换
                if (mHasVelocities) {
                    tBuf.setXYZ(tAtom.vx(), tAtom.vy(), tAtom.vz());
                    oBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    if (mKeyVx != null) tAtom.setVx(tBuf.mX);
                    if (mKeyVy != null) tAtom.setVy(tBuf.mY);
                    if (mKeyVz != null) tAtom.setVz(tBuf.mZ);
                }
            }
        } else {
            mAtomData.col(mKeyX).div2this(oBox.x());
            mAtomData.col(mKeyY).div2this(oBox.y());
            mAtomData.col(mKeyZ).div2this(oBox.z());
            mAtomData.col(mKeyX).multiply2this(mBox.x());
            mAtomData.col(mKeyY).multiply2this(mBox.y());
            mAtomData.col(mKeyZ).multiply2this(mBox.z());
        }
        return this;
    }
    
    /** 密度归一化, 返回自身来支持链式调用 */
    public SubLammpstrj setDenseNormalized() {
        if (mKeyX == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without x data");
        if (mKeyY == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without y data");
        if (mKeyZ == null) throw new UnsupportedOperationException("`setDenseNormalized` for Lammpstrj without z data");
        
        double tScale = MathEX.Fast.cbrt(volume() / atomNumber());
        tScale = 1.0 / tScale;
        
        // 从逻辑上考虑，这里不对原本数据做值拷贝，
        // 即使是斜方的也可以直接像这样进行缩放，
        // 这里顺便也会移除掉 boxlo 的数据，因此不使用 atom 修改
        switch (mXType) {
        case NORMAL: case UNWRAPPED: {
            IVector tCol = mAtomData.col(mKeyX);
            tCol.minus2this(mBox.xlo());
            tCol.multiply2this(tScale);
            break;
        }
        case SCALED: case SCALED_UNWRAPPED: {break;}
        default: throw new RuntimeException();
        }
        switch (mYType) {
        case NORMAL: case UNWRAPPED: {
            IVector tCol = mAtomData.col(mKeyY);
            tCol.minus2this(mBox.ylo());
            tCol.multiply2this(tScale);
            break;
        }
        case SCALED: case SCALED_UNWRAPPED: {break;}
        default: throw new RuntimeException();
        }
        switch (mZType) {
        case NORMAL: case UNWRAPPED: {
            IVector tCol = mAtomData.col(mKeyZ);
            tCol.minus2this(mBox.zlo());
            tCol.multiply2this(tScale);
            break;
        }
        case SCALED: case SCALED_UNWRAPPED: {break;}
        default: throw new RuntimeException();
        }
        
        if (mKeyVx != null) mAtomData.col(mKeyVx).multiply2this(tScale);
        if (mKeyVy != null) mAtomData.col(mKeyVy).multiply2this(tScale);
        if (mKeyVz != null) mAtomData.col(mKeyVz).multiply2this(tScale);
        
        // box 还是会重新创建，因为 box 的值这里约定是严格的常量，可以避免一些问题
        mBox = isPrism() ?
            new LmpBoxPrism(mBox.x()*tScale, mBox.y()*tScale, mBox.z()*tScale, mBox.xy()*tScale, mBox.xz()*tScale, mBox.yz()*tScale) :
            new LmpBox(mBox.x()*tScale, mBox.y()*tScale, mBox.z()*tScale);
        
        return this;
    }
    
    
    /** AbstractAtomData stuffs */
    @Override public boolean hasVelocities() {return mHasVelocities;}
    @Override public ISettableAtom atom(final int aIdx) {
        return new AbstractSettableAtom() {
            @Override public double x() {
                if (mKeyX == null) throw new UnsupportedOperationException("`x` for Lammpstrj without x data");
                double tX = mAtomData.get(aIdx, mKeyX);
                switch (mXType) {
                case NORMAL: {
                    return tX-mBox.xlo();
                }
                case UNWRAPPED: {
                    double tBoxLoX = mBox.xlo();
                    double tBoxHiX = mBox.xhi();
                    double tBoxX = tBoxHiX - tBoxLoX;
                    if      (tX <  tBoxLoX) {do {tX += tBoxX;} while (tX <  tBoxLoX);}
                    else if (tX >= tBoxHiX) {do {tX -= tBoxX;} while (tX >= tBoxHiX);}
                    return tX-tBoxLoX;
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (mXType == XYZType.SCALED_UNWRAPPED) {
                        if      (tX <  0.0) {do {++tX;} while (tX <  0.0);}
                        else if (tX >= 1.0) {do {--tX;} while (tX >= 1.0);}
                    }
                    if (!isPrism()) {
                        return tX*mBox.x();
                    } else {
                        if (mKeyY==null || !(mYType==XYZType.SCALED || mYType==XYZType.SCALED_UNWRAPPED)) throw new UnsupportedOperationException("`x` for SCALED x in prism Lammpstrj without SCALED y data");
                        if (mKeyZ==null || !(mZType==XYZType.SCALED || mZType==XYZType.SCALED_UNWRAPPED)) throw new UnsupportedOperationException("`x` for SCALED x in prism Lammpstrj without SCALED z data");
                        double tY = mAtomData.get(aIdx, mKeyY);
                        double tZ = mAtomData.get(aIdx, mKeyZ);
                        if (mYType == XYZType.SCALED_UNWRAPPED) {
                            if      (tY <  0.0) {do {++tY;} while (tY <  0.0);}
                            else if (tY >= 1.0) {do {--tY;} while (tY >= 1.0);}
                        }
                        if (mZType == XYZType.SCALED_UNWRAPPED) {
                            if      (tZ <  0.0) {do {++tZ;} while (tZ <  0.0);}
                            else if (tZ >= 1.0) {do {--tZ;} while (tZ >= 1.0);}
                        }
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
                case NORMAL: {
                    return tY-mBox.ylo();
                }
                case UNWRAPPED: {
                    double tBoxLoY = mBox.ylo();
                    double tBoxHiY = mBox.yhi();
                    double tBoxY = tBoxHiY - tBoxLoY;
                    if      (tY <  tBoxLoY) {do {tY += tBoxY;} while (tY <  tBoxLoY);}
                    else if (tY >= tBoxHiY) {do {tY -= tBoxY;} while (tY >= tBoxHiY);}
                    return tY-tBoxLoY;
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (mYType == XYZType.SCALED_UNWRAPPED) {
                        if      (tY <  0.0) {do {++tY;} while (tY <  0.0);}
                        else if (tY >= 1.0) {do {--tY;} while (tY >= 1.0);}
                    }
                    if (!isPrism()) {
                        return tY*mBox.y();
                    } else {
                        if (mKeyZ==null || !(mZType==XYZType.SCALED || mZType==XYZType.SCALED_UNWRAPPED)) throw new UnsupportedOperationException("`y` for SCALED y in prism Lammpstrj without SCALED z data");
                        double tZ = mAtomData.get(aIdx, mKeyZ);
                        if (mZType == XYZType.SCALED_UNWRAPPED) {
                            if      (tZ <  0.0) {do {++tZ;} while (tZ <  0.0);}
                            else if (tZ >= 1.0) {do {--tZ;} while (tZ >= 1.0);}
                        }
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
                case NORMAL: {
                    return tZ-mBox.zlo();
                }
                case UNWRAPPED: {
                    double tBoxLoZ = mBox.zlo();
                    double tBoxHiZ = mBox.zhi();
                    double tBoxZ = tBoxHiZ - tBoxLoZ;
                    if      (tZ <  tBoxLoZ) {do {tZ += tBoxZ;} while (tZ <  tBoxLoZ);}
                    else if (tZ >= tBoxHiZ) {do {tZ -= tBoxZ;} while (tZ >= tBoxHiZ);}
                    return tZ-tBoxLoZ;
                }
                case SCALED: case SCALED_UNWRAPPED: {
                    if (mZType == XYZType.SCALED_UNWRAPPED) {
                        if      (tZ <  0.0) {do {++tZ;} while (tZ <  0.0);}
                        else if (tZ >= 1.0) {do {--tZ;} while (tZ >= 1.0);}
                    }
                    return tZ*mBox.z();
                }
                default: throw new RuntimeException();
                }
            }
            
            /** 如果没有 id 数据则 id 为顺序位置 +1 */
            @Override public int id() {return mKeyID==null ? aIdx+1 : (int)mAtomData.get(aIdx, mKeyID);}
            /** 如果没有 type 数据则 type 都为 1 */
            @Override public int type() {return mKeyType==null ? 1 : (int)mAtomData.get(aIdx, mKeyType);}
            @Override public int index() {return aIdx;}
            
            @Override public double vx() {return mKeyVx==null ? 0.0 : mAtomData.get(aIdx, mKeyVx);}
            @Override public double vy() {return mKeyVy==null ? 0.0 : mAtomData.get(aIdx, mKeyVy);}
            @Override public double vz() {return mKeyVz==null ? 0.0 : mAtomData.get(aIdx, mKeyVz);}
            @Override public boolean hasVelocities() {return mHasVelocities;}
            
            private final XYZ mBuf = new XYZ();
            @Override public ISettableAtom setX(double aX) {
                if (mKeyX == null) throw new UnsupportedOperationException("`setX` for Lammpstrj without x data");
                switch (mXType) {
                case NORMAL: case UNWRAPPED: {
                    mAtomData.set(aIdx, mKeyX, aX+mBox.xlo());
                    return this;
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
                    return this;
                }
                default: throw new RuntimeException();
                }
            }
            @Override public ISettableAtom setY(double aY) {
                if (mKeyY == null) throw new UnsupportedOperationException("`setY` for Lammpstrj without y data");
                switch (mYType) {
                case NORMAL: case UNWRAPPED: {
                    mAtomData.set(aIdx, mKeyY, aY+mBox.ylo());
                    return this;
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
                    return this;
                }
                default: throw new RuntimeException();
                }
            }
            @Override public ISettableAtom setZ(double aZ) {
                if (mKeyZ == null) throw new UnsupportedOperationException("`setZ` for Lammpstrj without z data");
                switch (mZType) {
                case NORMAL: case UNWRAPPED: {
                    mAtomData.set(aIdx, mKeyZ, aZ+mBox.zlo());
                    return this;
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
                    return this;
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
                    case NORMAL: case UNWRAPPED:        {mAtomData.set(aIdx, mKeyX,      aX); break;}
                    case SCALED: case SCALED_UNWRAPPED: {mAtomData.set(aIdx, mKeyX, mBuf.mX); break;}
                    default: throw new RuntimeException();
                    }
                    switch(mYType) {
                    case NORMAL: case UNWRAPPED:        {mAtomData.set(aIdx, mKeyY,      aY); break;}
                    case SCALED: case SCALED_UNWRAPPED: {mAtomData.set(aIdx, mKeyY, mBuf.mY); break;}
                    default: throw new RuntimeException();
                    }
                    switch(mZType) {
                    case NORMAL: case UNWRAPPED:        {mAtomData.set(aIdx, mKeyZ,      aZ); break;}
                    case SCALED: case SCALED_UNWRAPPED: {mAtomData.set(aIdx, mKeyZ, mBuf.mZ); break;}
                    default: throw new RuntimeException();
                    }
                    return this;
                }
            }
            
            @Override public ISettableAtom setID(int aID) {
                if (mKeyID == null) throw new UnsupportedOperationException("`setID` for Lammpstrj without id");
                mAtomData.set(aIdx, mKeyID, aID); return this;
            }
            @Override public ISettableAtom setType(int aType) {
                if (mKeyType == null) throw new UnsupportedOperationException("`setType` for Lammpstrj without type");
                // 对于设置种类需要特殊处理，设置种类同时需要更新内部的原子种类计数
                mAtomData.set(aIdx, mKeyType, aType);
                if (aType > atomTypeNumber()) setAtomTypeNumber(aType);
                return this;
            }
            
            @Override public ISettableAtom setVx(double aVx) {
                if (mKeyVx == null) throw new UnsupportedOperationException("`setVx` for Lammpstrj without vx");
                mAtomData.set(aIdx, mKeyVx, aVx); return this;
            }
            @Override public ISettableAtom setVy(double aVy) {
                if (mKeyVy == null) throw new UnsupportedOperationException("`setVy` for Lammpstrj without vy");
                mAtomData.set(aIdx, mKeyVy, aVy); return this;
            }
            @Override public ISettableAtom setVz(double aVz) {
                if (mKeyVz == null) throw new UnsupportedOperationException("`setVz` for Lammpstrj without vy");
                mAtomData.set(aIdx, mKeyVz, aVz); return this;
            }
        };
    }
    @Override public LmpBox box() {return mBox;}
    @Override public int atomNumber() {return mAtomData.rowNumber();}
    @Override public int atomTypeNumber() {return mAtomTypeNum;}
    @Override public SubLammpstrj setAtomTypeNumber(int aAtomTypeNum) {mAtomTypeNum = aAtomTypeNum; return this;}
    
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
                if (aAtomData.hasVelocities()) {
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
                if (aAtomData.hasVelocities()) {
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
        aWriteln.writeln(String.join(" ", AbstractCollections.map(subAtomData, SubLammpstrj::toString_)));
        }
    }
    /** 保证整数时直接输出整数 */
    private static String toString_(double aValue) {
        String tStr = Double.toString(aValue);
        if (tStr.endsWith(".0")) tStr = tStr.substring(0, tStr.length()-2);
        return tStr;
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
