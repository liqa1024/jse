package com.jtool.lmp;

import com.jtool.atom.AbstractAtomData;
import com.jtool.atom.IAtom;
import com.jtool.atom.IHasAtomData;
import com.jtool.atom.XYZ;
import com.jtool.code.UT;
import com.jtool.math.MathEX;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorOperation;
import com.jtool.math.vector.Vectors;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.jtool.code.CS.*;

/**
 * @author liqa
 * <p> lammps 使用 write_data 写出的数据格式，头的顺序也是本程序的标准原子格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持键和键角等信息 </p>
 * <p> 暂时不支持速度信息 </p>
 * <p> 直接获取到的所有数据都是引用，因此外部可以直接进行修改 </p>
 */
public class Lmpdat extends AbstractAtomData {
    private int mAtomTypeNum;
    private Box mBox;
    private @Nullable IVector mMasses;
    private final ITable mAtomData;
    
    /**
     * 直接根据数据创建 Lmpdat
     * @param aAtomTypeNum 原子类型数目（必须）
     * @param aBox 模拟盒，可以接收 double[] 的模拟盒，则认为所有数据已经经过了 shift
     * @param aMasses 原子的质量
     * @param aAtomData 原子数据组成的矩阵（必须）
     */
    Lmpdat(int aAtomTypeNum, Box aBox, @Nullable IVector aMasses, ITable aAtomData) {
        mBox = aBox;
        mMasses = aMasses;
        mAtomData = aAtomData;
        // 会根据 aMasses 的长度自适应调整原子种类数目
        mAtomTypeNum = aMasses==null ? aAtomTypeNum : Math.max(aAtomTypeNum, aMasses.size());
    }
    
    
    /// 参数修改
    /**
     * 修改一些属性来方便调整最终输出的 data 文件
     * @return 返回自身来支持链式调用
     */
    public Lmpdat setMasses(double[] aMasses) {return setMasses(Vectors.from(aMasses));}
    public Lmpdat setMasses(Collection<? extends Number> aMasses) {return setMasses(Vectors.from(aMasses));}
    public Lmpdat setMasses(IVector aMasses) {mMasses = aMasses; return this;}
    public Lmpdat setAtomTypeNum(int aAtomTypeNum) {mAtomTypeNum = aAtomTypeNum; return this;}
    
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
        
        XYZ oShiftedBox = mBox.shiftedBox();
        double tScale = MathEX.Fast.cbrt(oShiftedBox.product() / atomNum());
        tScale = 1.0 / tScale;
        
        // 从逻辑上考虑，这里不对原本数据做值拷贝
        double tXlo = mBox.xlo(), tYlo = mBox.ylo(), tZlo = mBox.zlo();
        IVectorOperation
        tOpt = mAtomData.col(STD_X_COL).operation();
        tOpt.mapMinus2this(tXlo);
        tOpt.mapMultiply2this(tScale);
        tOpt = mAtomData.col(STD_Y_COL).operation();
        tOpt.mapMinus2this(tYlo);
        tOpt.mapMultiply2this(tScale);
        tOpt = mAtomData.col(STD_Z_COL).operation();
        tOpt.mapMinus2this(tZlo);
        tOpt.mapMultiply2this(tScale);
        
        // box 还是会重新创建，因为 box 的值这里约定是严格的常量，可以避免一些问题
        mBox = new Box(oShiftedBox.multiply(tScale));
        
        return this;
    }
    
    
    /// 获取属性
    public Box box() {return mBox;}
    public IVector masses() {return mMasses;}
    public double mass(int aType) {return mMasses!=null ? mMasses.get(aType-1) : Double.NaN;}
    
    
    /** AbstractAtomData stuffs */
    @Override public List<IAtom> atoms() {
        // 注意如果是斜方的模拟盒则不能获取到正交的原子数据
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("Atoms is temporarily support NORMAL Box only");
        return new TableAtoms(mAtomData);
    }
    @Override public XYZ boxLo() {return mBox.boxLo();}
    @Override public XYZ boxHi() {return mBox.boxHi();}
    @Override public int atomNum() {return mAtomData.rowNumber();}
    @Override public int atomTypeNum() {return mAtomTypeNum;}
    @Override public double volume() {
        // 注意如果是斜方的模拟盒则不能获取到模拟盒体积
        if (mBox.type() != Box.Type.NORMAL) throw new RuntimeException("Volume is temporarily support NORMAL Box only");
        return mBox.shiftedBox().product();
    }
    
    
    /// 创建 Lmpdat
    /** 拷贝一份 Lmpdat，为了简洁还是只保留 copy 一种方法 */
    public Lmpdat copy() {return new Lmpdat(mAtomTypeNum, mBox.copy(), mMasses==null?null:mMasses.copy(), mAtomData.copy());}
    
    /** 从 IHasAtomData 来创建，一般来说 Lmpdat 需要一个额外的质量信息 */
    public static Lmpdat fromAtomData(IHasAtomData aHasAtomData) {return fromAtomData(aHasAtomData, (IVector)null);}
    public static Lmpdat fromAtomData(IHasAtomData aHasAtomData, IVector aMasses) {
        // 根据输入的 aHasAtomData 类型来具体判断需要如何获取 rAtomData
        if (aHasAtomData instanceof Lmpdat) {
            // Lmpdat 则直接获取即可（专门优化，保留完整模拟盒信息）
            Lmpdat tLmpdat = (Lmpdat)aHasAtomData;
            return new Lmpdat(tLmpdat.atomTypeNum(), tLmpdat.mBox.copy(), aMasses, tLmpdat.mAtomData.copy());
        } else {
            // 一般的情况，通过 dataSTD 来创建，注意这里认为获取时已经经过了值拷贝，因此不再需要 copy
            return new Lmpdat(aHasAtomData.atomTypeNum(), new Box(aHasAtomData.boxLo(), aHasAtomData.boxHi()), aMasses, aHasAtomData.dataSTD());
        }
    }
    /** 兼容 Groovy 的乱七八糟的数字数组 */
    public static Lmpdat fromAtomData(IHasAtomData aHasAtomData, Collection<? extends Number> aMasses) {return fromAtomData(aHasAtomData, Vectors.from(aMasses));}
    public static Lmpdat fromAtomData(IHasAtomData aHasAtomData, double[] aMasses) {return fromAtomData(aHasAtomData, Vectors.from(aMasses));}
    
    
    /// 文件读写
    /**
     * 从 lammps 输出的 data 文件中读取来实现初始化
     * @param aFilePath lammps 输出的 data 文件路径
     * @return 读取得到的 Lmpdat 对象，如果文件不完整会直接返回 null
     * @throws IOException 如果读取失败
     */
    public static Lmpdat read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    public static Lmpdat read_(String[] aLines) {
        
        int tAtomNum;
        int aAtomTypeNum;
        Box aBox;
        IVector aMasses;
        List<double[]> aAtomData;
        
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
            aMasses = Vectors.zeros(aAtomTypeNum);
            for (int i = 0; i < aAtomTypeNum; ++i) {
                tTokens = UT.Texts.splitBlank(aLines[idx]);
                aMasses.set(Integer.parseInt(tTokens[0])-1, Double.parseDouble(tTokens[1]));
                ++idx;
            }
        } else {
            aMasses = null;
        }
        
        // 获取原子坐标信息
        idx = UT.Texts.findLineContaining(aLines, idx, "Atoms"); ++idx;
        ++idx; // 中间有一个空行
        if (idx+tAtomNum > aLines.length) return null;
        aAtomData = new ArrayList<>(tAtomNum);
        for (int i = 0; i < tAtomNum; ++i) {
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            aAtomData.add(UT.IO.str2data(tTokens));
            ++idx;
        }
        
        // 返回 lmpdat
        return new Lmpdat(aAtomTypeNum, aBox, aMasses, new Table(STD_ATOM_DATA_KEYS, aAtomData));
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
        lines.add(String.format("%6d atoms", atomNum()));
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
        lines.add(String.format("%6d %10.5g", i+1, mMasses.get(i)));
        }
        lines.add("");
        lines.add("Atoms");
        lines.add("");
        for (IVector subAtomData : mAtomData.rows())
        lines.add(String.format("%6d %6d %10.5g %10.5g %10.5g", (int)subAtomData.get(0), (int)subAtomData.get(1), subAtomData.get(2), subAtomData.get(3), subAtomData.get(4)));
        lines.add("");
        
        UT.IO.write(aFilePath, lines);
    }
}
