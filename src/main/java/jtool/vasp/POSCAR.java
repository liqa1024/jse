package jtool.vasp;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jtool.atom.*;
import jtool.code.UT;
import jtool.code.collection.AbstractCollections;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.Matrices;
import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import jtool.math.vector.Vectors;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

import static jtool.code.CS.ZL_STR;
import static jtool.code.UT.Code.toXYZ;

/**
 * @author liqa
 * <p> vasp 读取的原子位置格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持边界条件设置 </p>
 */
public class POSCAR extends AbstractSettableAtomData implements IVaspCommonData {
    public final static String DEFAULT_DATA_NAME = "VASP_POSCAR_FROM_JTOOL";
    
    /** POSCAR 只存储每个原子的 xyz 缩放后的矢量 */
    private final IMatrix mDirect;
    /** POSCAR 特有的属性，系统名称以及每个种类的原子名称 */
    private final String mDataName;
    private final String @NotNull[] mAtomTypes;
    private final IVector mAtomNumbers;
    /** POSCAR 使用晶格矢量组成的矩阵以及对应的晶格常数来作为边界 */
    private final IMatrix mBox;
    private final double mBoxScale;
    /** 是否有 Selective dynamics 关键字 */
    public final boolean mSelectiveDynamics;
    
    /** 内部使用，用于获取每个粒子的种类 index */
    private final NavigableMap<Integer, Integer> mIdx2Type;
    private final boolean mIsDiagBox;
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final Multimap<String, Integer> mKey2Type;
    
    POSCAR(String aDataName, IMatrix aBox, double aBoxScale, String @NotNull[] aAtomTypes, IVector aAtomNumbers, boolean aSelectiveDynamics, IMatrix aDirect) {
        mDirect = aDirect;
        mDataName = aDataName;
        mAtomTypes = aAtomTypes;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mBoxScale = aBoxScale;
        mSelectiveDynamics = aSelectiveDynamics;
        
        mIdx2Type = new TreeMap<>();
        int rNumber = 0;
        int rType = 0;
        for (double tNumber : mAtomNumbers.iterable()) {
            rNumber += (int)tNumber;
            ++rType;
            mIdx2Type.put(rNumber, rType);
        }
        
        mKey2Type = ArrayListMultimap.create();
        rType = 0;
        for (String tKey : mAtomTypes) {
            ++rType;
            mKey2Type.put(tKey, rType);
        }
        
        mIsDiagBox = mBox.operation().isDiag();
    }
    /** 用于方便构建，减少重复代码 */
    POSCAR(IVaspCommonData aVaspCommonData, boolean aSelectiveDynamics, IMatrix aDirect) {
        this(aVaspCommonData.dataName(), aVaspCommonData.vaspBox(), aVaspCommonData.vaspBoxScale(), aVaspCommonData.atomTypes(), aVaspCommonData.atomNumbers(), aSelectiveDynamics, aDirect);
    }
    
    /** 对于 POSCAR 提供额外的实用接口 */
    public int atomNum(String aKey) {
        int rAtomNum = 0;
        for (int tType : mKey2Type.get(aKey)) rAtomNum += atomNum(tType);
        return rAtomNum;
    }
    public int atomNum(int aType) {return (int)mAtomNumbers.get(aType-1);}
    
    public @Override String dataName() {return mDataName;}
    public @Override String @NotNull[] atomTypes() {return mAtomTypes;}
    public @Override IVector atomNumbers() {return mAtomNumbers;}
    public @Override IMatrix vaspBox() {return mBox;}
    public @Override double vaspBoxScale() {return mBoxScale;}
    public IMatrix direct() {return mDirect;}
    
    /** AbstractAtomData stuffs */
    @Override public ISettableAtom pickAtom(final int aIdx) {
        // 暂时没功夫研究矩阵表示的晶格在斜方情况下原子坐标是怎么样的
        // 并且这里还需要将其转换成正交的情况，因为参数计算相关优化都需要在正交情况下实现
        if (!mIsDiagBox) throw new RuntimeException("atoms is temporarily support Diagonal Box only");
        return new ISettableAtom() {
            @Override public double x() {return mDirect.get(aIdx, 0)*mBox.get(0, 0)*mBoxScale;}
            @Override public double y() {return mDirect.get(aIdx, 1)*mBox.get(1, 1)*mBoxScale;}
            @Override public double z() {return mDirect.get(aIdx, 2)*mBox.get(2, 2)*mBoxScale;}
            
            /** 没有 id 数据，id 为顺序位置 +1 */
            @Override public int id() {return aIdx+1;}
            /** type 需要根据 mIdx2Type 来获取，根据名称顺序给 type 的数字 */
            @Override public int type() {return mIdx2Type.higherEntry(aIdx).getValue();}
            
            @Override public ISettableAtom setX(double aX) {mDirect.set(aIdx, 0, aX/(mBox.get(0, 0)*mBoxScale)); return this;}
            @Override public ISettableAtom setY(double aY) {mDirect.set(aIdx, 1, aY/(mBox.get(1, 1)*mBoxScale)); return this;}
            @Override public ISettableAtom setZ(double aZ) {mDirect.set(aIdx, 2, aZ/(mBox.get(2, 2)*mBoxScale)); return this;}
            /** POSCAR 直接不支持设置这两者 */
            @Override public ISettableAtom setID(int aID) {throw new UnsupportedOperationException("setID");}
            @Override public ISettableAtom setType(int aType) {throw new UnsupportedOperationException("setType");}
        };
    }
    @Override public IXYZ box() {
        if (!mIsDiagBox) throw new RuntimeException("box is temporarily support Diagonal Box only");
        XYZ tBox = new XYZ(mBox.refSlicer().diag());
        tBox.multiply2this(mBoxScale);
        return tBox;
    }
    @Override public int atomNum() {return mDirect.rowNumber();}
    @Override public int atomTypeNum() {return mAtomTypes.length;}
    @Override public POSCAR setAtomTypeNum(int aAtomTypeNum) {throw new UnsupportedOperationException("setAtomTypeNum");}
    @Override public double volume() {
        // 注意如果是斜方的模拟盒则不能获取到模拟盒体积
        if (!mIsDiagBox) throw new RuntimeException("volume is temporarily support Diagonal Box only");
        return mBox.refSlicer().diag().prod();
    }
    
    
    /** 拷贝一份 POSCAR */
    @Override public POSCAR copy() {return new POSCAR(mDataName, mBox.copy(), mBoxScale, Arrays.copyOf(mAtomTypes, mAtomTypes.length), mAtomNumbers, mSelectiveDynamics, mDirect.copy());}
    // 由于 POSCAR 不是全都可以修改，因此不重写另外两个
    
    /** 从 IAtomData 来创建，POSCAR 需要额外的原子种类字符串以及额外的是否开启 SelectiveDynamics */
    public static POSCAR fromAtomData(IAtomData aAtomData) {return fromAtomData(aAtomData, (aAtomData instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomData).atomTypes() : ZL_STR);}
    public static POSCAR fromAtomData(IAtomData aAtomData, String... aAtomTypes) {return fromAtomData(aAtomData, (aAtomData instanceof POSCAR) && ((POSCAR)aAtomData).mSelectiveDynamics, aAtomTypes);}
    public static POSCAR fromAtomData(IAtomData aAtomData, boolean aSelectiveDynamics, String... aAtomTypes) {
        if (aAtomTypes == null) aAtomTypes = ZL_STR;
        if (aAtomTypes.length > 0) aAtomTypes = Arrays.copyOf(aAtomTypes, aAtomTypes.length);
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            POSCAR tPOSCAR = (POSCAR)aAtomData;
            return new POSCAR(tPOSCAR.mDataName, tPOSCAR.mBox.copy(), tPOSCAR.mBoxScale, aAtomTypes, tPOSCAR.mAtomNumbers.copy(), aSelectiveDynamics, tPOSCAR.mDirect.copy());
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
            XYZ tBox = toXYZ(aAtomData.box());
            
            int tAtomTypeNum = aAtomData.atomTypeNum();
            IVector rAtomNumbers = Vectors.zeros(tAtomTypeNum);
            IMatrix rDirect = Matrices.zeros(aAtomData.atomNum(), 3);
            int tIdx = 0;
            for (int tTypeMM = 0; tTypeMM < tAtomTypeNum; ++tTypeMM) {
                for (IAtom tAtom : aAtomData.asList()) if (tAtom.type() == tTypeMM+1) {
                    rAtomNumbers.increment_(tTypeMM);
                    rDirect.set(tIdx, 0, tAtom.x()/tBox.mX);
                    rDirect.set(tIdx, 1, tAtom.y()/tBox.mY);
                    rDirect.set(tIdx, 2, tAtom.z()/tBox.mZ);
                    ++tIdx;
                }
            }
            return new POSCAR(DEFAULT_DATA_NAME, Matrices.diag(tBox.data()), 1.0, aAtomTypes, rAtomNumbers, aSelectiveDynamics, rDirect);
        }
    }
    
    
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
        
        String aDataName;
        IMatrix aBox;
        double aBoxScale;
        String[] aAtomTypes;
        IVector aAtomNumbers;
        boolean aSelectiveDynamics = false;
        IMatrix aDirect;
        
        int idx = 0;
        String[] tTokens;
        // 第一行为 DataName
        aDataName = aLines.get(idx);
        // 读取模拟盒信息
        ++idx; if (idx >= aLines.size()) return null; tTokens = UT.Texts.splitBlank(aLines.get(idx));
        aBoxScale = Double.parseDouble(tTokens[0]);
        aBox = Matrices.zeros(3);
        ++idx; if (idx >= aLines.size()) return null;
        aBox.row(0).fill(UT.Texts.str2data(aLines.get(idx), 3));
        ++idx; if (idx >= aLines.size()) return null;
        aBox.row(1).fill(UT.Texts.str2data(aLines.get(idx), 3));
        ++idx; if (idx >= aLines.size()) return null;
        aBox.row(2).fill(UT.Texts.str2data(aLines.get(idx), 3));
        // 读取原子种类（可选）和对应数目的信息
        ++idx; if (idx >= aLines.size()) return null; tTokens = UT.Texts.splitBlank(aLines.get(idx));
        aAtomTypes = tTokens;
        ++idx; if (idx >= aLines.size()) return null; tTokens = UT.Texts.splitBlank(aLines.get(idx));
        try {
        aAtomNumbers = Vectors.from(AbstractCollections.map(tTokens, Integer::parseInt));
        } catch (Exception e) {
        --idx;
        aAtomNumbers = Vectors.from(AbstractCollections.map(aAtomTypes, Integer::parseInt));
        aAtomTypes = ZL_STR;
        }
        // 可选的注释行
        ++idx; if (idx >= aLines.size()) return null;
        if (aLines.get(idx).equalsIgnoreCase("Selective dynamics")) {
        aSelectiveDynamics = true; ++idx; if (idx >= aLines.size()) return null;
        }
        // 目前只支持 Direct
        if (!aLines.get(idx).equalsIgnoreCase("Direct")) {
        throw new RuntimeException("Can ONLY read Direct POSCAR temporarily");
        }
        // 读取原子数据
        ++idx; if (idx >= aLines.size()) return null;
        int tAtomNum = (int)aAtomNumbers.sum();
        if (idx+tAtomNum > aLines.size()) return null;
        aDirect = RowMatrix.zeros(tAtomNum, 3);
        for (IVector tRow : aDirect.rows()) {
            tRow.fill(UT.Texts.str2data(aLines.get(idx), 3));
            ++idx;
        }
        
        // 返回 POSCAR
        return new POSCAR(aDataName, aBox, aBoxScale, aAtomTypes, aAtomNumbers, aSelectiveDynamics, aDirect);
    }
    
    /**
     * 输出成 vasp 能够读取的 POSCAR 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    public void write(String aFilePath) throws IOException {
        List<String> lines = new ArrayList<>();
        
        lines.add(mDataName);
        lines.add(String.valueOf(mBoxScale));
        lines.add(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(0, 0), mBox.get(0, 1), mBox.get(0, 2)));
        lines.add(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(1, 0), mBox.get(1, 1), mBox.get(1, 2)));
        lines.add(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(2, 0), mBox.get(2, 1), mBox.get(2, 2)));
        if (mAtomTypes.length!=0)
        lines.add(String.join(" ", AbstractCollections.map(mAtomTypes, type -> String.format("%6s", type))));
        lines.add(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number.intValue()))));
        if (mSelectiveDynamics)
        lines.add("Selective dynamics");
        lines.add("Direct");
        for (IVector subDirect : mDirect.rows())
        lines.add(String.format("%16.10g    %16.10g    %16.10g", subDirect.get(0), subDirect.get(1), subDirect.get(2)));
        
        UT.IO.write(aFilePath, lines);
    }
}
