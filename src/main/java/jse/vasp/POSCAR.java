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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jse.code.CS.MASS;

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
    /** POSCAR 使用晶格矢量组成的矩阵以及对应的晶格常数来作为边界 */
    private final IMatrix mBox;
    private double mBoxScale;
    /** 是否有 Selective dynamics 关键字 */
    public final boolean mSelectiveDynamics;
    /** 保存一份 id 列表，这样在 lmpdat 转为 poscar 时会继续保留 id 信息 */
    private @Nullable IIntVector mIDs;
    
    private final boolean mIsDiagBox;
    /** 用于标记此 POSCAR 是否是来源于 XDATCAR，从而不能进行激进的修改 */
    private final boolean mIsRef;
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final @NotNull Multimap<String, Integer> mKey2Type;
    
    POSCAR(@Nullable String aComment, IMatrix aBox, double aBoxScale, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, boolean aSelectiveDynamics, IMatrix aDirect, boolean aIsCartesian, @Nullable IIntVector aIDs, boolean aIsRef) {
        mDirect = aDirect;
        mIsCartesian = aIsCartesian;
        mComment = aComment;
        mTypeNames = aTypeNames;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mBoxScale = aBoxScale;
        mSelectiveDynamics = aSelectiveDynamics;
        mIDs = aIDs;
        
        mKey2Type = ArrayListMultimap.create();
        if (mTypeNames != null) {
            int rType = 0;
            for (String tKey : mTypeNames) {
                ++rType;
                mKey2Type.put(tKey, rType);
            }
        }
        
        mIsDiagBox = mBox.operation().isDiag();
        mIsRef = aIsRef;
    }
    POSCAR(@Nullable String aComment, IMatrix aBox, double aBoxScale, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, boolean aSelectiveDynamics, IMatrix aDirect, boolean aIsCartesian, @Nullable IIntVector aIDs) {
        this(aComment, aBox, aBoxScale, aTypeNames, aAtomNumbers, aSelectiveDynamics, aDirect, aIsCartesian, aIDs, false);
    }
    /** 用于方便构建，减少重复代码 */
    POSCAR(IVaspCommonData aVaspCommonData, boolean aSelectiveDynamics, IMatrix aDirect) {
        this(aVaspCommonData.comment(), aVaspCommonData.vaspBox(), aVaspCommonData.vaspBoxScale(), aVaspCommonData.typeNames(), aVaspCommonData.atomNumbers(), aSelectiveDynamics, aDirect, aVaspCommonData.isCartesian(), aVaspCommonData.ids(), true);
    }
    
    static String @Nullable[] copyTypeNames(String @Nullable[] aTypeNames) {
        return (aTypeNames==null || aTypeNames.length==0) ? null : Arrays.copyOf(aTypeNames, aTypeNames.length);
    }
    static @Nullable IIntVector copyIDs(@Nullable IIntVector aIDs) {
        return (aIDs==null || aIDs.isEmpty()) ? null : aIDs.copy();
    }
    
    /** 对于 POSCAR 提供额外的实用接口 */
    public @Nullable String typeName(int aType) {return mTypeNames==null ? null : mTypeNames[aType-1];}
    public double mass(int aType) {
        @Nullable String tTypeName = typeName(aType);
        return tTypeName==null ? Double.NaN : MASS.getOrDefault(tTypeName, Double.NaN);
    }
    public int atomNumber(String aType) {
        int rAtomNum = 0;
        for (int tType : mKey2Type.get(aType)) rAtomNum += atomNumber(tType);
        return rAtomNum;
    }
    public int atomNumber(int aType) {return mAtomNumbers.get(aType-1);}
    /** 保留旧名称兼容，当时起名太随意了，居然这么久都没发现 */
    @Deprecated public final int atomNum(String aType) {return atomNumber(aType);}
    @Deprecated public final int atomNum(int aType) {return atomNumber(aType);}
    /** 提供简写版本 */
    @VisibleForTesting public final int natoms(String aType) {return atomNumber(aType);}
    @VisibleForTesting public final int natoms(int aType) {return atomNumber(aType);}
    
    public @Override @Nullable String comment() {return mComment;}
    public @Override String @Nullable[] typeNames() {return mTypeNames;}
    public @Override IIntVector atomNumbers() {return mAtomNumbers;}
    public @Override IMatrix vaspBox() {return mBox;}
    public @Override double vaspBoxScale() {return mBoxScale;}
    public IMatrix direct() {return mDirect;}
    public @Override boolean isCartesian() {return mIsCartesian;}
    public @Override boolean isDiagBox() {return mIsDiagBox;}
    public @Override @Nullable IIntVector ids() {return mIDs;}
    
    /** Groovy stuffs */
    @VisibleForTesting public String @Nullable[] getTypeNames() {return mTypeNames;}
    @VisibleForTesting public String getComment() {return mComment;}
    @VisibleForTesting public double getBoxScale() {return mBoxScale;}
    
    
    /** 支持直接修改 TypeNames，只会增大种类数，不会减少 */
    public POSCAR setTypeNames(String... aTypeNames) {
        if (mIsRef) throw new RuntimeException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        if (aTypeNames==null || aTypeNames.length==0) {
            mTypeNames = null;
            mKey2Type.clear();
            return this;
        }
        setTypeNames_(aTypeNames, true);
        return this;
    }
    void setTypeNames_(String @NotNull[] aTypeNames, boolean aCopy) {
        if (mIsRef) throw new RuntimeException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        if (mTypeNames ==null || aTypeNames.length> mTypeNames.length) mTypeNames = aCopy ? Arrays.copyOf(aTypeNames, aTypeNames.length) : aTypeNames;
        else System.arraycopy(aTypeNames, 0, mTypeNames, 0, aTypeNames.length);
        if (aTypeNames.length > mAtomNumbers.size()) {
            IIntVector oAtomNumbers = mAtomNumbers;
            mAtomNumbers = IntVector.zeros(aTypeNames.length);
            mAtomNumbers.subVec(0, oAtomNumbers.size()).fill(oAtomNumbers);
        }
        mKey2Type.clear();
        int rType = 0;
        for (String tKey : mTypeNames) {
            ++rType;
            mKey2Type.put(tKey, rType);
        }
    }
    
    public POSCAR setComment(@Nullable String aComment) {mComment = aComment; return this;}
    public POSCAR setBoxScale(double aBoxScale) {mBoxScale = aBoxScale; return this;}
    
    /** Cartesian 和 Direct 来回转换 */
    public POSCAR setCartesian() {
        if (mIsCartesian) return this;
        if (mIsRef) throw new RuntimeException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        // 注意如果是斜方的模拟盒则不能进行转换
        if (!mIsDiagBox) throw new RuntimeException("converting between Cartesian and Direct is temporarily support Diagonal Box only");
        mDirect.col(0).multiply2this(mBox.get(0, 0));
        mDirect.col(1).multiply2this(mBox.get(1, 1));
        mDirect.col(2).multiply2this(mBox.get(2, 2));
        mIsCartesian = true;
        return this;
    }
    public POSCAR setDirect() {
        if (!mIsCartesian) return this;
        if (mIsRef) throw new RuntimeException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        // 注意如果是斜方的模拟盒则不能进行转换
        if (!mIsDiagBox) throw new RuntimeException("converting between Cartesian and Direct is temporarily support Diagonal Box only");
        mDirect.col(0).div2this(mBox.get(0, 0));
        mDirect.col(1).div2this(mBox.get(1, 1));
        mDirect.col(2).div2this(mBox.get(2, 2));
        mIsCartesian = false;
        return this;
    }
    /** 密度归一化 */
    public POSCAR setDenseNormalized() {
        if (mIsRef) throw new RuntimeException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
        // 注意如果是斜方的模拟盒则不能进行转换
        if (!mIsDiagBox) throw new RuntimeException("setDenseNormalized is temporarily support Diagonal Box only");
        
        double tScale = MathEX.Fast.cbrt(volume() / atomNumber());
        // 直接通过调整 boxScale 来实现
        mBoxScale /= tScale;
        
        return this;
    }
    
    /** AbstractAtomData stuffs */
    @Override public ISettableAtom pickAtom(final int aIdx) {
        // 暂时没功夫研究矩阵表示的晶格在斜方情况下原子坐标是怎么样的
        // 并且这里还需要将其转换成正交的情况，因为参数计算相关优化都需要在正交情况下实现
        if (!mIsDiagBox) throw new RuntimeException("atoms is temporarily support Diagonal Box only");
        return new AbstractSettableAtom() {
            private int mIdx = aIdx;
            @Override public double x() {return (mIsCartesian ? mDirect.get(mIdx, 0) : mDirect.get(mIdx, 0)*mBox.get(0, 0)) * mBoxScale;}
            @Override public double y() {return (mIsCartesian ? mDirect.get(mIdx, 1) : mDirect.get(mIdx, 1)*mBox.get(1, 1)) * mBoxScale;}
            @Override public double z() {return (mIsCartesian ? mDirect.get(mIdx, 2) : mDirect.get(mIdx, 2)*mBox.get(2, 2)) * mBoxScale;}
            
            /** 如果没有 id 数据则为顺序位置 +1 */
            @Override public int id() {return mIDs==null ? mIdx+1 : mIDs.get(mIdx);}
            /** type 直接遍历获取即可，根据名称顺序给 type 的数字 */
            @Override public int type() {
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
            /** poscar 的 atom 在 setType 后会改变位置 */
            @Override public int index() {return mIdx;}
            
            @Override public ISettableAtom setX(double aX) {mDirect.set(mIdx, 0, (mIsCartesian ? aX : aX/mBox.get(0, 0)) / mBoxScale); return this;}
            @Override public ISettableAtom setY(double aY) {mDirect.set(mIdx, 1, (mIsCartesian ? aY : aY/mBox.get(1, 1)) / mBoxScale); return this;}
            @Override public ISettableAtom setZ(double aZ) {mDirect.set(mIdx, 2, (mIsCartesian ? aZ : aZ/mBox.get(2, 2)) / mBoxScale); return this;}
            @Override public ISettableAtom setID(int aID) {
                if (id() == aID) return this;
                if (mIsRef) throw new RuntimeException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
                if (mIDs==null) mIDs = Vectors.range(1, atomNumber()+1);
                mIDs.set(mIdx, aID);
                return this;
            }
            /** poscar 的 atom 在 setType 后会改变位置，并且也会影响其他原子的位置，这里只同步当前原子的位置 */
            @Override public ISettableAtom setType(int aType) {
                int oType = type();
                if (oType == aType) return this;
                if (mIsRef) throw new RuntimeException("This POSCAR is reference from XDATCAR, use copy() to modify it.");
                // 超过原子种类数目则需要重新设置
                if (aType > mAtomNumbers.size()) {
                    String[] rTypeNames = new String[aType];
                    if (mTypeNames != null) System.arraycopy(mTypeNames, 0, rTypeNames, 0, mTypeNames.length);
                    for (int tType = mAtomNumbers.size()+1; tType <= aType; ++tType) rTypeNames[tType-1] = "T"+tType;
                    setTypeNames_(rTypeNames, false);
                }
                //noinspection IfStatementWithIdenticalBranches
                if (oType < aType) {
                    // 增大 type 的情况，这里使用高效的方式，
                    // 将所有中间的边界处原子都向上跳跃到正确位置，
                    // 然后将此原子移动到新的间隙中
                    int tID = id();
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
                        if (mIDs != null) mIDs.set(to, mIDs.get(from));
                        to = from;
                    }
                    // 更新 mIdx
                    mIdx = from;
                    mDirect.set(mIdx, 0, tX);
                    mDirect.set(mIdx, 1, tY);
                    mDirect.set(mIdx, 2, tZ);
                    if (mIDs != null) mIDs.set(mIdx, tID);
                    // 更新 type 计数
                    mAtomNumbers.decrement(oType-1);
                    mAtomNumbers.increment(aType-1);
                    return this;
                } else {
                    // 减小 type 的情况，这里简单处理，
                    // 将所有中间的边界处原子都向下跳跃到正确位置，
                    // 然后将此原子移动到新的间隙中
                    int tID = id();
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
                        if (mIDs != null) mIDs.set(to, mIDs.get(from));
                        to = from;
                    }
                    // 更新 mIdx
                    mIdx = from;
                    mDirect.set(mIdx, 0, tX);
                    mDirect.set(mIdx, 1, tY);
                    mDirect.set(mIdx, 2, tZ);
                    if (mIDs != null) mIDs.set(mIdx, tID);
                    // 更新 type 计数
                    mAtomNumbers.decrement(oType-1);
                    mAtomNumbers.increment(aType-1);
                    return this;
                }
            }
        };
    }
    @Override public IXYZ box() {
        if (!mIsDiagBox) throw new RuntimeException("box is temporarily support Diagonal Box only");
        XYZ tBox = new XYZ(mBox.refSlicer().diag());
        tBox.multiply2this(mBoxScale);
        return tBox;
    }
    @Override public int atomNumber() {return mDirect.rowNumber();}
    @Override public int atomTypeNumber() {return mAtomNumbers.size();}
    @Override public POSCAR setAtomTypeNumber(int aAtomTypeNum) {throw new UnsupportedOperationException("setAtomTypeNum");}
    @Override public double volume() {
        // 注意如果是斜方的模拟盒则不能获取到模拟盒体积
        if (!mIsDiagBox) throw new RuntimeException("volume is temporarily support Diagonal Box only");
        return mBox.refSlicer().diag().prod() * MathEX.Fast.pow3(mBoxScale);
    }
    
    
    /** 拷贝一份 POSCAR */
    @Override public POSCAR copy() {return new POSCAR(mComment, mBox.copy(), mBoxScale, copyTypeNames(mTypeNames), mAtomNumbers.copy(), mSelectiveDynamics, mDirect.copy(), mIsCartesian, copyIDs(mIDs));}
    // 由于 POSCAR 不是全都可以修改，因此不重写另外两个
    
    /** 从 IAtomData 来创建，POSCAR 需要额外的原子种类字符串以及额外的是否开启 SelectiveDynamics */
    public static POSCAR fromAtomData(IAtomData aAtomData) {return fromAtomData(aAtomData, (aAtomData instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomData).typeNames() : null);}
    public static POSCAR fromAtomData(IAtomData aAtomData, String... aTypeNames) {return fromAtomData(aAtomData, (aAtomData instanceof POSCAR) && ((POSCAR)aAtomData).mSelectiveDynamics, aTypeNames);}
    public static POSCAR fromAtomData(IAtomData aAtomData, boolean aSelectiveDynamics, String... aTypeNames) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            POSCAR tPOSCAR = (POSCAR)aAtomData;
            int tAtomTypeNum = Math.max(tPOSCAR.mAtomNumbers.size(), aTypeNames.length);
            IIntVector tAtomNumbers = IntVector.zeros(tAtomTypeNum);
            tAtomNumbers.subVec(0, tPOSCAR.mAtomNumbers.size()).fill(tPOSCAR.mAtomNumbers);
            return new POSCAR(tPOSCAR.mComment, tPOSCAR.mBox.copy(), tPOSCAR.mBoxScale, copyTypeNames(aTypeNames), tAtomNumbers, aSelectiveDynamics, tPOSCAR.mDirect.copy(), tPOSCAR.mIsCartesian, copyIDs(tPOSCAR.mIDs));
        } if (aAtomData instanceof XDATCAR) {
            // XDATCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            return fromAtomData(((XDATCAR)aAtomData).defaultFrame(), aSelectiveDynamics, aTypeNames);
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
            IIntVector rIDs = IntVector.zeros(aAtomData.atomNumber());
            int tAtomTypeNum = Math.max(aAtomData.atomTypeNumber(), aTypeNames.length);
            IIntVector rAtomNumbers = IntVector.zeros(tAtomTypeNum);
            IMatrix rDirect = Matrices.zeros(aAtomData.atomNumber(), 3);
            int tIdx = 0;
            for (int tTypeMM = 0; tTypeMM < tAtomTypeNum; ++tTypeMM) {
                for (IAtom tAtom : aAtomData.asList()) if (tAtom.type() == tTypeMM+1) {
                    rAtomNumbers.increment(tTypeMM);
                    rDirect.set(tIdx, 0, tAtom.x());
                    rDirect.set(tIdx, 1, tAtom.y());
                    rDirect.set(tIdx, 2, tAtom.z());
                    rIDs.set(tIdx, tAtom.id());
                    ++tIdx;
                }
            }
            // 现在转换会直接转成 Cartesian 来避免计算中的浮点误差
            return new POSCAR(null, Matrices.diag(aAtomData.box().data()), 1.0, copyTypeNames(aTypeNames), rAtomNumbers, aSelectiveDynamics, rDirect, true, rIDs);
        }
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static POSCAR of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static POSCAR of(IAtomData aAtomData, String... aTypeNames) {return fromAtomData(aAtomData, aTypeNames);}
    public static POSCAR of(IAtomData aAtomData, boolean aSelectiveDynamics, String... aTypeNames) {return fromAtomData(aAtomData, aSelectiveDynamics, aTypeNames);}
    
    
    /// 文件读写
    /**
     * 从 vasp 输出的 POSCAR 文件中读取来实现初始化
     * @param aFilePath vasp 输出的 POSCAR 文件路径
     * @return 读取得到的 POSCAR 对象，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static POSCAR read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    public static POSCAR read_(List<String> aLines) {
        if (aLines.isEmpty()) return null;
        
        String aComment;
        IMatrix aBox;
        double aBoxScale;
        String[] aTypeNames;
        IIntVector aAtomNumbers;
        boolean aSelectiveDynamics = false;
        IMatrix aDirect;
        boolean aIsCartesian;
        
        int idx = 0;
        String[] tTokens;
        // 第一行为 Comment
        aComment = aLines.get(idx);
        // 读取模拟盒信息
        ++idx; if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        aBoxScale = Double.parseDouble(tTokens[0]);
        aBox = Matrices.zeros(3);
        ++idx; if (idx >= aLines.size()) return null;
        aBox.row(0).fill(UT.Text.str2data(aLines.get(idx), 3));
        ++idx; if (idx >= aLines.size()) return null;
        aBox.row(1).fill(UT.Text.str2data(aLines.get(idx), 3));
        ++idx; if (idx >= aLines.size()) return null;
        aBox.row(2).fill(UT.Text.str2data(aLines.get(idx), 3));
        // 读取原子种类（可选）和对应数目的信息
        ++idx; if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        aTypeNames = tTokens;
        ++idx; if (idx >= aLines.size()) return null; tTokens = UT.Text.splitBlank(aLines.get(idx));
        try {
        final String[] fTokens = tTokens;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        } catch (Exception e) {
        --idx;
        final String[] fTokens = aTypeNames;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        aTypeNames = null;
        }
        // 可选的注释行
        ++idx; if (idx >= aLines.size()) return null;
        if (aLines.get(idx).equalsIgnoreCase("Selective dynamics")) {
        aSelectiveDynamics = true; ++idx; if (idx >= aLines.size()) return null;
        }
        // 只支持 Direct 和 Cartesian
        aIsCartesian = aLines.get(idx).equalsIgnoreCase("Cartesian");
        if (!aIsCartesian && !aLines.get(idx).equalsIgnoreCase("Direct")) {
        throw new RuntimeException("Can ONLY read Direct or Cartesian POSCAR");
        }
        // 读取原子数据
        ++idx; if (idx >= aLines.size()) return null;
        int tAtomNum = aAtomNumbers.sum();
        if (idx+tAtomNum > aLines.size()) return null;
        aDirect = RowMatrix.zeros(tAtomNum, 3);
        for (IVector tRow : aDirect.rows()) {
            tRow.fill(UT.Text.str2data(aLines.get(idx), 3));
            ++idx;
        }
        
        // 返回 POSCAR
        return new POSCAR(aComment, aBox, aBoxScale, aTypeNames, aAtomNumbers, aSelectiveDynamics, aDirect, aIsCartesian, null);
    }
    
    /**
     * 输出成 vasp 能够读取的 POSCAR 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    public void write(String aFilePath) throws IOException {
        List<String> lines = new ArrayList<>();
        
        lines.add(mComment==null ? DEFAULT_COMMENT : mComment);
        lines.add(String.valueOf(mBoxScale));
        lines.add(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(0, 0), mBox.get(0, 1), mBox.get(0, 2)));
        lines.add(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(1, 0), mBox.get(1, 1), mBox.get(1, 2)));
        lines.add(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(2, 0), mBox.get(2, 1), mBox.get(2, 2)));
        if (mTypeNames!=null && mTypeNames.length!=0)
        lines.add(String.join(" ", AbstractCollections.map(mTypeNames, type -> String.format("%6s", type))));
        lines.add(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number))));
        if (mSelectiveDynamics)
        lines.add("Selective dynamics");
        lines.add(mIsCartesian ? "Cartesian" : "Direct");
        for (IVector subDirect : mDirect.rows())
        lines.add(String.format("%16.10g    %16.10g    %16.10g", subDirect.get(0), subDirect.get(1), subDirect.get(2)));
        
        UT.IO.write(aFilePath, lines);
    }
}
