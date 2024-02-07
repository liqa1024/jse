package jse.lmp;

import jse.atom.*;
import jse.code.UT;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vectors;
import jse.vasp.IVaspCommonData;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

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
    public final static int LMPDAT_VELOCITY_LENGTH = 4;
    public final static int LMPDAT_ID_COL = 0, LMPDAT_VX_COL = 1, LMPDAT_VY_COL = 2, LMPDAT_VZ_COL = 3;
    
    private int mAtomTypeNum;
    private Box mBox;
    private @Nullable IVector mMasses;
    /** 固定类型方便 MPI 传输 */
    private final IntVector mAtomID;
    private final IntVector mAtomType;
    private final RowMatrix mAtomXYZ;
    private @Nullable RowMatrix mVelocities;
    
    /**
     * 直接根据数据创建 Lmpdat
     * @param aAtomTypeNum 原子类型数目（必须）
     * @param aBox 模拟盒，可以接收 double[] 的模拟盒，则认为所有数据已经经过了 shift
     * @param aMasses 原子的质量
     * @param aAtomID 原子数据组成的矩阵（必须）
     * @param aAtomType 原子数据组成的矩阵（必须）
     * @param aAtomXYZ 原子数据组成的矩阵（必须）
     * @param aVelocities 原子速度组成的矩阵
     */
    public Lmpdat(int aAtomTypeNum, Box aBox, @Nullable IVector aMasses, IntVector aAtomID, IntVector aAtomType, RowMatrix aAtomXYZ, @Nullable RowMatrix aVelocities) {
        mBox = aBox;
        mMasses = aMasses;
        mAtomID = aAtomID;
        mAtomType = aAtomType;
        mAtomXYZ = aAtomXYZ;
        // 会根据 aMasses 的长度自适应调整原子种类数目
        mAtomTypeNum = aMasses==null ? aAtomTypeNum : Math.max(aAtomTypeNum, aMasses.size());
        mVelocities = aVelocities;
    }
    
    
    /// 参数修改
    /**
     * 修改一些属性来方便调整最终输出的 data 文件
     * @return 返回自身来支持链式调用
     */
    public Lmpdat setMasses(double[] aMasses) {return setMasses(Vectors.from(aMasses));}
    public Lmpdat setMasses(Collection<? extends Number> aMasses) {return setMasses(Vectors.from(aMasses));}
    public Lmpdat setMasses(IVector aMasses) {mMasses = aMasses; return this;}
    @Override public Lmpdat setAtomTypeNum(int aAtomTypeNum) {mAtomTypeNum = aAtomTypeNum; return this;}
    public Lmpdat setNoVelocities() {mVelocities = null; return this;}
    public Lmpdat setHasVelocities() {if (mVelocities == null) {mVelocities = RowMatrix.zeros(atomNum(), ATOM_DATA_KEYS_VELOCITY.length);} return this;}
    
    /**
     * 修改模拟盒类型
     * @return 返回自身来支持链式调用
     */
    public Lmpdat setBoxNormal() {
        if (mBox.type() != Box.Type.NORMAL) mBox = new Box(mBox);
        return this;
    }
    public Lmpdat setBoxPrism() {
        if (mBox.type() != Box.Type.PRISM) {
            if (mBox instanceof BoxPrism) {
                BoxPrism oBox = (BoxPrism)mBox;
                mBox = new BoxPrism(oBox);
            } else {
                mBox = new BoxPrism(mBox, 0.0, 0.0, 0.0);
            }
        }
        return this;
    }
    /**
     * 密度归一化
     * @return 返回自身来支持链式调用
     */
    public Lmpdat setDenseNormalized() {
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("setDenseNormalized is temporarily support NORMAL Box only");
        
        XYZ oShiftedBox = XYZ.toXYZ(mBox.shiftedBox());
        double tScale = MathEX.Fast.cbrt(oShiftedBox.prod() / atomNum());
        tScale = 1.0 / tScale;
        
        // 从逻辑上考虑，这里不对原本数据做值拷贝
        double tXlo = mBox.xlo(), tYlo = mBox.ylo(), tZlo = mBox.zlo();
        IVector
        tCol = mAtomXYZ.col(XYZ_X_COL);
        tCol.minus2this(tXlo);
        tCol.multiply2this(tScale);
        tCol = mAtomXYZ.col(XYZ_Y_COL);
        tCol.minus2this(tYlo);
        tCol.multiply2this(tScale);
        tCol = mAtomXYZ.col(XYZ_Z_COL);
        tCol.minus2this(tZlo);
        tCol.multiply2this(tScale);
        if (mVelocities != null) {
        tCol = mVelocities.col(STD_VX_COL);
        tCol.multiply2this(tScale);
        tCol = mVelocities.col(STD_VY_COL);
        tCol.multiply2this(tScale);
        tCol = mVelocities.col(STD_VZ_COL);
        tCol.multiply2this(tScale);
        }
        
        // box 还是会重新创建，因为 box 的值这里约定是严格的常量，可以避免一些问题
        mBox = new Box(oShiftedBox.multiply(tScale));
        
        return this;
    }
    
    
    /// 获取属性
    public Box lmpBox() {return mBox;}
    public IntVector ids() {return mAtomID;}
    public IntVector types() {return mAtomType;}
    public RowMatrix positions() {return mAtomXYZ;}
    public @Nullable RowMatrix velocities() {return mVelocities;}
    public @Nullable IVector masses() {return mMasses;}
    public double mass(int aType) {return mMasses!=null ? mMasses.get(aType-1) : Double.NaN;}
    public ISettableAtom pickAtomInternal(final int aIdx) {
        return new AbstractSettableAtom() {
            @Override public double x() {return mAtomXYZ.get(aIdx, XYZ_X_COL);}
            @Override public double y() {return mAtomXYZ.get(aIdx, XYZ_Y_COL);}
            @Override public double z() {return mAtomXYZ.get(aIdx, XYZ_Z_COL);}
            @Override public int id() {return mAtomID.get(aIdx);}
            @Override public int type() {return mAtomType.get(aIdx);}
            @Override public int index() {return aIdx;}
            
            @Override public double vx() {return mVelocities==null?0.0:mVelocities.get(aIdx, STD_VX_COL);}
            @Override public double vy() {return mVelocities==null?0.0:mVelocities.get(aIdx, STD_VY_COL);}
            @Override public double vz() {return mVelocities==null?0.0:mVelocities.get(aIdx, STD_VZ_COL);}
            @Override public boolean hasVelocities() {return mVelocities!=null;}
            
            @Override public ISettableAtom setX(double aX) {mAtomXYZ.set(aIdx, XYZ_X_COL, aX); return this;}
            @Override public ISettableAtom setY(double aY) {mAtomXYZ.set(aIdx, XYZ_Y_COL, aY); return this;}
            @Override public ISettableAtom setZ(double aZ) {mAtomXYZ.set(aIdx, XYZ_Z_COL, aZ); return this;}
            @Override public ISettableAtom setID(int aID) {mAtomID.set(aIdx, aID); return this;}
            @Override public ISettableAtom setType(int aType) {
                // 对于设置种类需要特殊处理，设置种类同时需要更新内部的原子种类计数
                mAtomType.set(aIdx, aType);
                if (aType > atomTypeNum()) setAtomTypeNum(aType);
                return this;
            }
            @Override public ISettableAtom setVx(double aVx) {
                if (mVelocities == null) throw new UnsupportedOperationException("setVx");
                mVelocities.set(aIdx, STD_VX_COL, aVx); return this;
            }
            @Override public ISettableAtom setVy(double aVy) {
                if (mVelocities == null) throw new UnsupportedOperationException("setVy");
                mVelocities.set(aIdx, STD_VY_COL, aVy); return this;
            }
            @Override public ISettableAtom setVz(double aVz) {
                if (mVelocities == null) throw new UnsupportedOperationException("setVz");
                mVelocities.set(aIdx, STD_VZ_COL, aVz); return this;
            }
        };
    }
    
    
    /** AbstractAtomData stuffs */
    @Override public boolean hasVelocities() {return mVelocities!=null;}
    @Override public ISettableAtom pickAtom(final int aIdx) {
        // 注意如果是斜方的模拟盒则不能获取到正交的原子数据
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("atoms is temporarily support NORMAL Box only");
        return new AbstractSettableAtom() {
            @Override public double x() {return mAtomXYZ.get(aIdx, XYZ_X_COL)-mBox.xlo();}
            @Override public double y() {return mAtomXYZ.get(aIdx, XYZ_Y_COL)-mBox.ylo();}
            @Override public double z() {return mAtomXYZ.get(aIdx, XYZ_Z_COL)-mBox.zlo();}
            @Override public int id() {return mAtomID.get(aIdx);}
            @Override public int type() {return mAtomType.get(aIdx);}
            @Override public int index() {return aIdx;}
            
            @Override public double vx() {return mVelocities==null?0.0:mVelocities.get(aIdx, STD_VX_COL);}
            @Override public double vy() {return mVelocities==null?0.0:mVelocities.get(aIdx, STD_VY_COL);}
            @Override public double vz() {return mVelocities==null?0.0:mVelocities.get(aIdx, STD_VZ_COL);}
            @Override public boolean hasVelocities() {return mVelocities!=null;}
            
            @Override public ISettableAtom setX(double aX) {mAtomXYZ.set(aIdx, XYZ_X_COL, aX+mBox.xlo()); return this;}
            @Override public ISettableAtom setY(double aY) {mAtomXYZ.set(aIdx, XYZ_Y_COL, aY+mBox.ylo()); return this;}
            @Override public ISettableAtom setZ(double aZ) {mAtomXYZ.set(aIdx, XYZ_Z_COL, aZ+mBox.zlo()); return this;}
            @Override public ISettableAtom setID(int aID) {mAtomID.set(aIdx, aID); return this;}
            @Override public ISettableAtom setType(int aType) {
                // 对于设置种类需要特殊处理，设置种类同时需要更新内部的原子种类计数
                mAtomType.set(aIdx, aType);
                if (aType > atomTypeNum()) setAtomTypeNum(aType);
                return this;
            }
            @Override public ISettableAtom setVx(double aVx) {
                if (mVelocities == null) throw new UnsupportedOperationException("setVx");
                mVelocities.set(aIdx, STD_VX_COL, aVx); return this;
            }
            @Override public ISettableAtom setVy(double aVy) {
                if (mVelocities == null) throw new UnsupportedOperationException("setVy");
                mVelocities.set(aIdx, STD_VY_COL, aVy); return this;
            }
            @Override public ISettableAtom setVz(double aVz) {
                if (mVelocities == null) throw new UnsupportedOperationException("setVz");
                mVelocities.set(aIdx, STD_VZ_COL, aVz); return this;
            }
        };
    }
    @Override public IXYZ box() {
        // 注意如果是斜方的模拟盒则不能获取到正交的模拟盒数据
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("box is temporarily support NORMAL Box only");
        return mBox.shiftedBox();
    }
    @Override public int atomNum() {return mAtomID.size();}
    @Override public int atomTypeNum() {return mAtomTypeNum;}
    @Override public double volume() {
        // 注意如果是斜方的模拟盒则不能获取到模拟盒体积
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("volume is temporarily support NORMAL Box only");
        return mBox.shiftedBox().prod();
    }
    
    
    /// 创建 Lmpdat
    /** 拷贝一份 Lmpdat，为了简洁还是只保留 copy 一种方法 */
    @Override public Lmpdat copy() {return new Lmpdat(mAtomTypeNum, mBox.copy(), mMasses==null?null:mMasses.copy(), mAtomID.copy(), mAtomType.copy(), mAtomXYZ.copy(), mVelocities==null?null:mVelocities.copy());}
    @Override protected Lmpdat newSame_() {return new Lmpdat(mAtomTypeNum, mBox.copy(), mMasses==null?null:mMasses.copy(), mAtomID.copy(), mAtomType.copy(), mAtomXYZ.copy(), mVelocities==null?null:mVelocities.copy());}
    @Override protected Lmpdat newZeros_(int aAtomNum) {return new Lmpdat(mAtomTypeNum, mBox.copy(), mMasses==null?null:mMasses.copy(), IntVector.zeros(aAtomNum), IntVector.zeros(aAtomNum), RowMatrix.zeros(aAtomNum, mAtomXYZ.columnNumber()), mVelocities==null?null:RowMatrix.zeros(aAtomNum, mVelocities.columnNumber()));}
    
    /** 从 IAtomData 来创建，一般来说 Lmpdat 需要一个额外的质量信息 */
    public static Lmpdat fromAtomData(IAtomData aAtomData) {
        @Nullable IVector aMasses = null;
        if (aAtomData instanceof Lmpdat) {
            aMasses = ((Lmpdat)aAtomData).mMasses;
            if (aMasses != null) aMasses = aMasses.copy();
        } else
        if (aAtomData instanceof IVaspCommonData) {
            String[] tAtomTypes = ((IVaspCommonData)aAtomData).atomTypes();
            int tAtomTypeNum = aAtomData.atomTypeNum();
            if (tAtomTypes!=null && tAtomTypes.length>=tAtomTypeNum) {
                aMasses = Vectors.zeros(tAtomTypeNum);
                for (int i = 0; i < tAtomTypeNum; ++i) aMasses.set(i, MASS.getOrDefault(tAtomTypes[i], -1.0));
            }
        }
        return fromAtomData_(aAtomData, aMasses);
    }
    public static Lmpdat fromAtomData(IAtomData aAtomData, IVector aMasses) {return fromAtomData_(aAtomData, Vectors.from(aMasses));}
    public static Lmpdat fromAtomData(IAtomData aAtomData, Collection<? extends Number> aMasses) {return fromAtomData_(aAtomData, Vectors.from(aMasses));}
    public static Lmpdat fromAtomData(IAtomData aAtomData, double[] aMasses) {return fromAtomData_(aAtomData, Vectors.from(aMasses));}
    
    static Lmpdat fromAtomData_(IAtomData aAtomData, @Nullable IVector aMasses) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof Lmpdat) {
            // Lmpdat 则直接获取即可（专门优化，保留完整模拟盒信息）
            Lmpdat tLmpdat = (Lmpdat)aAtomData;
            return new Lmpdat(tLmpdat.atomTypeNum(), tLmpdat.mBox.copy(), aMasses, tLmpdat.mAtomID.copy(), tLmpdat.mAtomType.copy(), tLmpdat.mAtomXYZ.copy(), tLmpdat.mVelocities==null?null:tLmpdat.mVelocities.copy());
        } else {
            // 一般的情况
            int tAtomNum = aAtomData.atomNum();
            IntVector rAtomID = IntVector.zeros(tAtomNum);
            IntVector rAtomType = IntVector.zeros(tAtomNum);
            RowMatrix rAtomXYZ = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix rVelocities = aAtomData.hasVelocities() ? RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length) : null;
            int row = 0;
            for (IAtom tAtom : aAtomData.asList()) {
                rAtomID.set(row, tAtom.id());
                rAtomType.set(row, tAtom.type());
                rAtomXYZ.set(row, XYZ_X_COL, tAtom.x());
                rAtomXYZ.set(row, XYZ_Y_COL, tAtom.y());
                rAtomXYZ.set(row, XYZ_Z_COL, tAtom.z());
                if (rVelocities != null) {
                    rVelocities.set(row, STD_VX_COL, tAtom.vx());
                    rVelocities.set(row, STD_VY_COL, tAtom.vy());
                    rVelocities.set(row, STD_VZ_COL, tAtom.vz());
                }
                ++row;
            }
            return new Lmpdat(aAtomData.atomTypeNum(), new Box(aAtomData.box()), aMasses, rAtomID, rAtomType, rAtomXYZ, rVelocities);
        }
    }
    
    /// 文件读写
    /**
     * 从 lammps 输出的 data 文件中读取来实现初始化
     * @param aFilePath lammps 输出的 data 文件路径
     * @return 读取得到的 Lmpdat 对象，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static Lmpdat read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    public static Lmpdat read_(List<String> aLines) {
        if (aLines.isEmpty()) return null;
        
        int tAtomNum;
        int aAtomTypeNum;
        Box aBox;
        IVector aMasses;
        IntVector aAtomID;
        IntVector aAtomType;
        RowMatrix aAtomXYZ;
        RowMatrix aVelocities;
        
        int idx = 0; int end;
        int tIdx; String[] tTokens;
        // 跳过第一行
        ++idx;
        // 读取原子数目
        idx = UT.Text.findLineContaining(aLines, idx, "atoms", true); if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        tAtomNum = Integer.parseInt(tTokens[0]);
        // 读取原子种类数目
        idx = UT.Text.findLineContaining(aLines, idx, "atom types", true); if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        aAtomTypeNum = Integer.parseInt(tTokens[0]);
        // 读取模拟盒信息
        idx = UT.Text.findLineContaining(aLines, idx, "xlo xhi", true); if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]);
        idx = UT.Text.findLineContaining(aLines, idx, "ylo yhi", true); if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]);
        idx = UT.Text.findLineContaining(aLines, idx, "zlo zhi", true); if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]);
        // 兼容可能的斜方模拟盒
        tIdx = UT.Text.findLineContaining(aLines, idx, "xy xz yz", true);
        if (tIdx < aLines.size()) {
            idx = tIdx;
            tTokens = UT.Text.splitBlank(aLines.get(idx));
            aBox = new BoxPrism(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi, Double.parseDouble(tTokens[0]), Double.parseDouble(tTokens[1]), Double.parseDouble(tTokens[2]));
        } else {
            aBox = new Box(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi);
        }
        
        // 读取可能的质量信息
        tIdx = UT.Text.findLineContaining(aLines, idx, "Masses", true); ++tIdx;
        if (tIdx < aLines.size()) {
            idx = tIdx;
            ++idx; // 中间有一个空行
            end = idx+aAtomTypeNum;
            if (end > aLines.size()) return null;
            aMasses = Vectors.zeros(aAtomTypeNum);
            for (; idx < end; ++idx) {
                tTokens = UT.Text.splitBlank(aLines.get(idx));
                aMasses.set(Integer.parseInt(tTokens[0])-1, Double.parseDouble(tTokens[1]));
            }
        } else {
            aMasses = null;
        }
        
        // 获取原子坐标信息
        idx = UT.Text.findLineContaining(aLines, idx, "Atoms", true); ++idx;
        ++idx; // 中间有一个空行
        end = idx+tAtomNum;
        if (end > aLines.size()) return null;
        aAtomID = IntVector.zeros(tAtomNum);
        aAtomType = IntVector.zeros(tAtomNum);
        aAtomXYZ = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
        // 和坐标排序一致的顺序来存储
        for (int row = 0; row < tAtomNum; ++row) {
            IVector tIDTypeXYZ = UT.Text.str2data(aLines.get(idx), STD_ATOM_DATA_KEYS.length);
            aAtomID.set(row, (int)tIDTypeXYZ.get(STD_ID_COL));
            aAtomType.set(row, (int)tIDTypeXYZ.get(STD_TYPE_COL));
            aAtomXYZ.set(row, XYZ_X_COL, tIDTypeXYZ.get(STD_X_COL));
            aAtomXYZ.set(row, XYZ_Y_COL, tIDTypeXYZ.get(STD_Y_COL));
            aAtomXYZ.set(row, XYZ_Z_COL, tIDTypeXYZ.get(STD_Z_COL));
            ++idx;
        }
        
        // 读取可能的速度信息
        tIdx = UT.Text.findLineContaining(aLines, idx, "Velocities", true); ++tIdx;
        if (tIdx < aLines.size()) {
            idx = tIdx;
            // 统计 id 和对应行的映射，用于保证速度顺序和坐标排序一致
            Map<Integer, Integer> tId2Row = new HashMap<>(tAtomNum);
            for (int row = 0; row < tAtomNum; ++row) tId2Row.put(aAtomID.get(row), row);
            // 读取速率
            ++idx; // 中间有一个空行
            end = idx+tAtomNum;
            if (end > aLines.size()) return null;
            aVelocities = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length);
            // 和坐标排序一致的顺序来存储
            for (; idx < end; ++idx) {
                IVector tVelocity = UT.Text.str2data(aLines.get(idx), LMPDAT_VELOCITY_LENGTH);
                int tRow = tId2Row.get((int)tVelocity.get(LMPDAT_ID_COL));
                aVelocities.set(tRow, STD_VX_COL, tVelocity.get(LMPDAT_VX_COL));
                aVelocities.set(tRow, STD_VY_COL, tVelocity.get(LMPDAT_VY_COL));
                aVelocities.set(tRow, STD_VZ_COL, tVelocity.get(LMPDAT_VZ_COL));
            }
        } else {
            aVelocities = null;
        }
        
        // 返回 lmpdat
        return new Lmpdat(aAtomTypeNum, aBox, aMasses, aAtomID, aAtomType, aAtomXYZ, aVelocities);
    }
    
    /**
     * 输出成 lammps 能够读取的 data 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    public void write(String aFilePath) throws IOException {
        List<String> lines = new ArrayList<>();
        
        lines.add("LAMMPS data file generated by jse");
        lines.add("");
        lines.add(String.format("%6d atoms", atomNum()));
        lines.add("");
        lines.add(String.format("%6d atom types", mAtomTypeNum));
        lines.add("");
        lines.add(String.format("%15.10g %15.10g xlo xhi", mBox.xlo(), mBox.xhi()));
        lines.add(String.format("%15.10g %15.10g ylo yhi", mBox.ylo(), mBox.yhi()));
        lines.add(String.format("%15.10g %15.10g zlo zhi", mBox.zlo(), mBox.zhi()));
        if (mBox instanceof BoxPrism) {
        BoxPrism tBox = (BoxPrism)mBox;
        lines.add(String.format("%15.10g %15.10g %15.10g xy xz yz", tBox.xy(), tBox.xz(), tBox.yz()));
        }
        if (mMasses != null) {
        lines.add("");
        lines.add("Masses");
        lines.add("");
        for (int i = 0; i < mAtomTypeNum; ++i)
        lines.add(String.format("%6d %15.10g", i+1, mMasses.get(i)));
        }
        lines.add("");
        lines.add("Atoms");
        lines.add("");
        for (int i = 0; i < atomNum(); ++i)
        lines.add(String.format("%6d %6d %15.10g %15.10g %15.10g", mAtomID.get(i), mAtomType.get(i), mAtomXYZ.get(i, XYZ_X_COL), mAtomXYZ.get(i, XYZ_Y_COL), mAtomXYZ.get(i, XYZ_Z_COL)));
        if (mVelocities != null) {
        lines.add("");
        lines.add("Velocities");
        lines.add("");
        for (int i = 0; i < atomNum(); ++i)
        lines.add(String.format("%6d %15.10g %15.10g %15.10g", mAtomID.get(i), mVelocities.get(i, STD_VX_COL), mVelocities.get(i, STD_VY_COL), mVelocities.get(i, STD_VZ_COL)));
        }
        
        UT.IO.write(aFilePath, lines);
    }
}
