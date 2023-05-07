package com.guan.lmp;

import com.google.common.collect.Maps;
import com.guan.atom.AbstractAtomData;
import com.guan.atom.IHasAtomData;
import com.guan.code.UT;
import com.guan.math.MathEX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author liqa
 * <p> lammps 使用 write_data 写出的数据格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持键和键角等信息 </p>
 * <p> 暂时不支持速度信息 </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 */
public class Lmpdat extends AbstractAtomData {
    private final static String[] ATOM_DATA_KEYS = IHasAtomData.Util.STD_ATOM_DATA_KEYS;
    private final static Map<String, Integer> KEY2IDX = Maps.immutableMap("id", 0, "type", 1, "x", 2, "y", 3, "z", 4);
    private final static int[] ATOM_DATA_XYZ = new int[] {2, 3, 4};
    private final static int[] ATOM_DATA_XYZID = new int[] {2, 3, 4, 0};
    private final static int TYPE_COL = 1;
    
    private final int mAtomTypeNum;
    private Box mBox;
    private final double @Nullable[] mMasses;
    private final double[][] mAtomData;
    
    /**
     * 直接根据数据创建 Lmpdat
     * @param aAtomTypeNum 原子类型数目（必须）
     * @param aBox 模拟盒，可以接收 double[] 的模拟盒，则认为所有数据已经经过了 shift
     * @param aMasses 原子的质量
     * @param aAtomData 原子数据组成的矩阵（必须）
     */
    public Lmpdat(int aAtomTypeNum, Box aBox, double @Nullable[] aMasses, double[][] aAtomData) {
        mAtomTypeNum = aAtomTypeNum;
        mBox = aBox;
        mMasses = aMasses;
        mAtomData = aAtomData;
    }
    public Lmpdat(int aAtomTypeNum, Box aBox, double[][] aAtomData) {this(aAtomTypeNum, aBox, null, aAtomData);}
    public Lmpdat(int aAtomTypeNum, double[] aBox, double @Nullable[] aMasses, double[][] aAtomData) {this(aAtomTypeNum, new Box(aBox), aMasses, aAtomData);}
    public Lmpdat(int aAtomTypeNum, double[] aBoxLo, double[] aBoxHi, double @Nullable[] aMasses, double[][] aAtomData) {this(aAtomTypeNum, new Box(aBoxLo, aBoxHi), aMasses, aAtomData);}
    
    
    /// 参数修改
    /**
     * 修改模拟盒类型
     * @return 返回自身来支持链式调用
     */
    public Lmpdat setBoxNormal() {
        if (mBox.type() != Box.Type.NORMAL) mBox = new Box(mBox.mBoxLo, mBox.mBoxHi);
        return this;
    }
    public Lmpdat setBoxPrism() {
        if (mBox.type() != Box.Type.PRISM) {
            if (mBox instanceof BoxPrism) {
                BoxPrism oBox = (BoxPrism)mBox;
                mBox = new BoxPrism(oBox.mBoxLo, oBox.mBoxHi, oBox.mXY, oBox.mXZ, oBox.mYZ);
            } else {
                mBox = new BoxPrism(mBox.mBoxLo, mBox.mBoxHi, 0.0, 0.0, 0.0);
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
        
        double[] oShiftedBox = mBox.shiftedBox();
        double tScale = MathEX.Fast.cbrt(MathEX.Vec.product(oShiftedBox) / mAtomData.length);
        tScale = 1.0 / tScale;
        
        // 从逻辑上考虑，这里不对原本数据做值拷贝
        double tXlo = mBox.xlo(), tYlo = mBox.ylo(), tZlo = mBox.zlo();
        for (int i = 0; i < mAtomData.length; ++i) {
            mAtomData[i][2] -= tXlo; mAtomData[i][2] *= tScale;
            mAtomData[i][3] -= tYlo; mAtomData[i][3] *= tScale;
            mAtomData[i][4] -= tZlo; mAtomData[i][4] *= tScale;
        }
        // box 还是会重新创建，因为 box 的值这里约定是严格的常量，可以避免一些问题
        mBox = new Box(oShiftedBox[0]*tScale, oShiftedBox[1]*tScale, oShiftedBox[2]*tScale);
        
        return this;
    }
    
    
    /// 获取属性
    public Box box() {return mBox;}
    public double[] masses() {return mMasses;}
    public double   mass(int aType) {return mMasses!=null ? mMasses[aType-1] : Double.NaN;}
    
    /** 重写来获取各种属性 */
    @Override public int atomTypeNum() {return mAtomTypeNum;}
    @Override public String[] atomDataKeys() {return ATOM_DATA_KEYS;}
    @Override public double[][] atomData() {return mAtomData;}
    
    // 重写表明对于斜的 box 不支持正交 XYZ 的相关操作
    @Override public double[][] toOrthogonalXYZ_(double[][] aAtomDataXYZ) {
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("OrthogonalXYZ temporarily support NORMAL Box only");
        return super.toOrthogonalXYZ_(aAtomDataXYZ);
    }
    @Override public double[][] toOrthogonalXYZID_(double[][] aAtomDataXYZID) {
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("OrthogonalXYZID temporarily support NORMAL Box only");
        return super.toOrthogonalXYZID_(aAtomDataXYZID);
    }
    @Override public double[] boxLo() {
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("OrthogonalXYZ temporarily support NORMAL Box only");
        return mBox.boxLo();
    }
    @Override public double[] boxHi() {
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("OrthogonalXYZ temporarily support NORMAL Box only");
        return mBox.boxHi();
    }
    /** 重写来优化查找过程 */
    public int typeCol() {return TYPE_COL;}
    public int key2idx(String aKey) {Integer tIdx = KEY2IDX.get(aKey); return tIdx != null ? tIdx : -1;}
    public int @NotNull[] xyzCol() {return ATOM_DATA_XYZ;}
    public int @NotNull[] xyzidCol() {return ATOM_DATA_XYZID;}
    
    
    /// 创建 Lmpdat
    /** 拷贝一份 Lmpdat，为了简洁还是只保留 copy 一种方法 */
    public Lmpdat copy() {return new Lmpdat(mAtomTypeNum, mBox.copy(), mMasses==null?null:MathEX.Vec.copy(mMasses), MathEX.Mat.copy(mAtomData));}
    
    /** 从 IHasAtomData 来创建，一般来说 Lmpdat 需要一个额外的质量信息 */
    public static Lmpdat fromAtomData(IHasAtomData aHasAtomData) {return fromAtomData(aHasAtomData, (double[])null);}
    public static Lmpdat fromAtomData(IHasAtomData aHasAtomData, double[] aMasses) {
        // 粒子数据，需要符合 lmpdat 的格式
        double[][] tAtomData;
        int tAtomTypeNum = aHasAtomData.atomTypeNum();
        // 根据输入的 aHasAtomData 类型来具体判断需要如何获取 rAtomData
        if (aHasAtomData instanceof Lmpdat) {
            // Lmpdat 则直接获取即可（专门优化，保留排序）
            if (((Lmpdat)aHasAtomData).box().type() != Box.Type.NORMAL) throw new RuntimeException("fromAtomData for Lmpdat temporarily support NORMAL Box only");
            tAtomData = aHasAtomData.atomData();
        } else {
            // 一般的情况
            tAtomData = IHasAtomData.Util.toStandardAtomData(aHasAtomData);
        }
        return new Lmpdat(tAtomTypeNum, aHasAtomData.boxLo(), aHasAtomData.boxHi(), aMasses, tAtomData);
    }
    /** 兼容 Groovy 的乱七八糟的数字数组 */
    public static Lmpdat fromAtomData(IHasAtomData aHasAtomData, Collection<? extends Number> aMasses) {
        double[] tMasses = new double[aMasses.size()];
        int tIdx = 0;
        for (Number tMass : aMasses) {
            tMasses[tIdx] = tMass.doubleValue();
            ++tIdx;
        }
        return fromAtomData(aHasAtomData, tMasses);
    }
    
    
    /// 文件读写
    /**
     * 从文件 lammps 输出的文件中读取来实现初始化
     * @param aFilePath lammps 输出的 data 文件路径
     * @return 读取得到的 Lmpdat 对象，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static Lmpdat read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    public static Lmpdat read_(String[] aLines) {
        
        int tAtomNum;
        int aAtomTypeNum;
        Box aBox;
        double[] aMasses;
        double[][] aAtomData;
        
        int idx = 0;
        int tIdx; String[] tTokens;
        // 跳过第一行
        ++idx;
        // 读取原子数目
        idx = UT.Texts.findLineContaining(aLines, idx, "atoms"); if (idx >= aLines.length) return null; tTokens = UT.Texts.splitBlank(aLines[idx]);
        tAtomNum = Integer.parseInt(tTokens[0]);
        // 读取原子种类数目
        idx = UT.Texts.findLineContaining(aLines, idx, "atom types"); if (idx >= aLines.length) return null; tTokens = UT.Texts.splitBlank(aLines[idx]);
        aAtomTypeNum = Integer.parseInt(tTokens[0]);
        // 读取模拟盒信息
        idx = UT.Texts.findLineContaining(aLines, idx, "xlo xhi"); if (idx >= aLines.length) return null; tTokens = UT.Texts.splitBlank(aLines[idx]);
        double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]);
        idx = UT.Texts.findLineContaining(aLines, idx, "ylo yhi"); if (idx >= aLines.length) return null; tTokens = UT.Texts.splitBlank(aLines[idx]);
        double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]);
        idx = UT.Texts.findLineContaining(aLines, idx, "zlo zhi"); if (idx >= aLines.length) return null; tTokens = UT.Texts.splitBlank(aLines[idx]);
        double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]);
        // 兼容可能的斜方模拟盒
        tIdx = UT.Texts.findLineContaining(aLines, idx, "xy xz yz");
        if (tIdx < aLines.length) {
            idx = tIdx;
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            aBox = new BoxPrism(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi, Double.parseDouble(tTokens[0]), Double.parseDouble(tTokens[1]), Double.parseDouble(tTokens[2]));
        } else {
            aBox = new Box(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi);
        }
        
        // 读取可能的质量信息
        tIdx = UT.Texts.findLineContaining(aLines, idx, "Masses"); ++tIdx;
        if (tIdx < aLines.length) {
            idx = tIdx;
            ++idx; // 中间有一个空行
            if (idx+aAtomTypeNum > aLines.length) return null;
            aMasses = new double[aAtomTypeNum];
            for (int i = 0; i < aAtomTypeNum; ++i) {
                tTokens = UT.Texts.splitBlank(aLines[idx]);
                aMasses[Integer.parseInt(tTokens[0])-1] = Double.parseDouble(tTokens[1]);
                ++idx;
            }
        } else {
            aMasses = null;
        }
        
        // 获取原子坐标信息
        idx = UT.Texts.findLineContaining(aLines, idx, "Atoms"); ++idx;
        ++idx; // 中间有一个空行
        if (idx+tAtomNum > aLines.length) return null;
        aAtomData = new double[tAtomNum][ATOM_DATA_KEYS.length];
        for (int i = 0; i < tAtomNum; ++i) {
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            for(int j = 0; j < ATOM_DATA_KEYS.length; ++j) aAtomData[i][j] = Double.parseDouble(tTokens[j]);
            ++idx;
        }
        
        // 返回 lmpdat
        return new Lmpdat(aAtomTypeNum, aBox, aMasses, aAtomData);
    }
    
    /**
     * 输出成 lammps 能够读取的 data 文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    public void write(String aFilePath) throws IOException {
        List<String> lines = new ArrayList<>();
        
        lines.add("LAMMPS data file generated by jTool");
        lines.add("");
        lines.add(String.format("%6d atoms", mAtomData.length));
        lines.add("");
        lines.add(String.format("%6d atom types", mAtomTypeNum));
        lines.add("");
        lines.add(String.format("%10.5g %10.5g xlo xhi", mBox.xlo(), mBox.xhi()));
        lines.add(String.format("%10.5g %10.5g ylo yhi", mBox.ylo(), mBox.yhi()));
        lines.add(String.format("%10.5g %10.5g zlo zhi", mBox.zlo(), mBox.zhi()));
        if (mBox instanceof BoxPrism) {
        BoxPrism tBox = (BoxPrism)mBox;
        lines.add(String.format("%10.5g %10.5g %10.5g xy xz yz", tBox.mXY, tBox.mXZ, tBox.mYZ));
        }
        if (mMasses != null) {
        lines.add("");
        lines.add("Masses");
        lines.add("");
        for (int i = 0; i < mAtomTypeNum; ++i)
        lines.add(String.format("%6d %10.5g", i, mMasses[i]));
        }
        lines.add("");
        lines.add("Atoms");
        lines.add("");
        for (double[] subAtomData : mAtomData)
        lines.add(String.format("%6d %6d %10.5g %10.5g %10.5g", (int)subAtomData[0], (int)subAtomData[1], subAtomData[2], subAtomData[3], subAtomData[4]));
        lines.add("");
        
        UT.IO.write(aFilePath, lines);
    }
}
