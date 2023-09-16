package com.jtool.lmp;

import com.jtool.atom.*;
import com.jtool.code.UT;
import com.jtool.code.collection.AbstractCollections;
import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.code.collection.NewCollections;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.RowMatrix;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author liqa
 * <p> lammps 使用 dump 输出的数据格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 支持拥有多个时间帧的输出（可以当作 List 来使用），
 * 也可直接获取结果（则会直接获取第一个时间帧的结果） </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 */
public class Lammpstrj extends AbstractMultiFrameAtomData<Lammpstrj.SubLammpstrj> {
    private final static String[] BOX_BOUND = {"pp", "pp", "pp"};
    
    private final List<SubLammpstrj> mData;
    
    Lammpstrj(SubLammpstrj... aData) {mData = NewCollections.from(aData);}
    Lammpstrj(List<SubLammpstrj> aData) {mData = aData;}
    
    /** AbstractList stuffs */
    @Override public int size() {return mData.size();}
    @Override public SubLammpstrj get(int index) {return mData.get(index);}
    @Override public SubLammpstrj set(int index, SubLammpstrj aSubLammpstrj) {return mData.set(index, aSubLammpstrj);}
    @Override public boolean add(SubLammpstrj aSubLammpstrj) {return mData.add(aSubLammpstrj);}
    @Override public SubLammpstrj remove(int aIndex) {return mData.remove(aIndex);}
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    public Lammpstrj append(IAtomData aAtomData, long aTimeStep) {
        mData.add(fromAtomData_(aAtomData, aTimeStep));
        return this;
    }
    public Lammpstrj append(IAtomData aAtomData) {
        mData.add(fromAtomData_(aAtomData, getTimeStep(aAtomData, mData.size())));
        return this;
    }
    public Lammpstrj appendList(Iterable<IAtomData> aAtomDataList) {
        for (IAtomData tAtomData : aAtomDataList) mData.add(fromAtomData_(tAtomData, getTimeStep(tAtomData, mData.size())));
        return this;
    }
    public Lammpstrj appendFile(String aFilePath) throws IOException {
        mData.addAll(read(aFilePath).mData);
        return this;
    }
    /** Groovy stuffs，用于支持传入 IAtomData 来设置 */
    void set(int aIdx, IAtomData aAtomData) {mData.set(aIdx, fromAtomData_(aAtomData, getTimeStep(aAtomData, aIdx)));}
    @VisibleForTesting void putAt(int aIdx, IAtomData aAtomData) {set(aIdx, aAtomData);}
    
    // dump 额外的属性
    public long timeStep() {return defaultFrame().timeStep();}
    public String[] boxBounds() {return defaultFrame().boxBounds();}
    public Box box() {return defaultFrame().box();}
    
    /** 提供直接转为表格的接口 */
    public ITable asTable() {return defaultFrame().asTable();}
    
    /** 每个帧的子 Lammpstrj */
    public static class SubLammpstrj extends AbstractAtomData {
        private final long mTimeStep;
        private final String[] mBoxBounds;
        private final Box mBox;
        private final ITable mAtomData;
        private final int mAtomTypeNum;
        
        String mKeyX = null, mKeyY = null, mKeyZ = null;
        private final boolean mHasVelocities;
        private final XYZType mXType, mYType, mZType;
        
        /** 提供直接转为表格的接口 */
        public ITable asTable() {return mAtomData;}
        
        public SubLammpstrj(long aTimeStep, String[] aBoxBounds, Box aBox, ITable aAtomData) {
            mTimeStep = aTimeStep;
            mBoxBounds = aBoxBounds;
            mBox = aBox;
            mAtomData = aAtomData;
            
            for (int i = 0; i < aAtomData.columnNumber(); ++i) {
                String tKey = aAtomData.getHead(i);
                if (tKey.equals("x") || tKey.equals("xs") || tKey.equals("xu") || tKey.equals("xsu")) {mKeyX = tKey;}
                if (tKey.equals("y") || tKey.equals("ys") || tKey.equals("yu") || tKey.equals("ysu")) {mKeyY = tKey;}
                if (tKey.equals("z") || tKey.equals("zs") || tKey.equals("zu") || tKey.equals("zsu")) {mKeyZ = tKey;}
            }
            mHasVelocities = (mAtomData.containsHead("vx") && mAtomData.containsHead("vy") && mAtomData.containsHead("vz"));
            
            switch (mKeyX) {
            case "x"  : {mXType = XYZType.NORMAL;             break;}
            case "xs" : {mXType = XYZType.SCALED;             break;}
            case "xu" : {mXType = XYZType.UNWRAPPED;          break;}
            case "xsu": {mXType = XYZType.SCALED_UNWRAPPED;   break;}
            default: throw new RuntimeException();
            }
            switch (mKeyY) {
            case "y"  : {mYType = XYZType.NORMAL;             break;}
            case "ys" : {mYType = XYZType.SCALED;             break;}
            case "yu" : {mYType = XYZType.UNWRAPPED;          break;}
            case "ysu": {mYType = XYZType.SCALED_UNWRAPPED;   break;}
            default: throw new RuntimeException();
            }
            switch (mKeyZ) {
            case "z"  : {mZType = XYZType.NORMAL;             break;}
            case "zs" : {mZType = XYZType.SCALED;             break;}
            case "zu" : {mZType = XYZType.UNWRAPPED;          break;}
            case "zsu": {mZType = XYZType.SCALED_UNWRAPPED;   break;}
            default: throw new RuntimeException();
            }
            
            // 对于 dump，mAtomTypeNum 只能手动遍历统计
            int tAtomTypeNum = 1;
            if (mAtomData.containsHead("type")) {
                tAtomTypeNum = (int)mAtomData.col("type").max();
            }
            mAtomTypeNum = tAtomTypeNum;
        }
        
        private enum XYZType {
              NORMAL
            , SCALED
            , UNWRAPPED
            , SCALED_UNWRAPPED
        }
        
        // dump 额外的属性
        public long timeStep() {return mTimeStep;}
        public String[] boxBounds() {return mBoxBounds;}
        public Box box() {return mBox;}
        
        /** 内部方法，用于从原始的数据获取合适的 x，y，z 数据 */
        private double getX_(double aRawX) {
            double tX = aRawX;
            switch (mXType) {
            case NORMAL: {
                return tX;
            }
            case SCALED: {
                double tBoxLoX = mBox.xlo();
                double tBoxX = mBox.xhi() - tBoxLoX;
                return tBoxLoX + tX*tBoxX;
            }
            case UNWRAPPED: {
                double tBoxLoX = mBox.xlo();
                double tBoxHiX = mBox.xhi();
                double tBoxX = tBoxHiX - tBoxLoX;
                if      (tX <  tBoxLoX) {while (tX <  tBoxLoX) tX += tBoxX;}
                else if (tX >= tBoxHiX) {while (tX >= tBoxHiX) tX -= tBoxX;}
                return tX;
            }
            case SCALED_UNWRAPPED: {
                if      (tX <  0.0) {while (tX <  0.0) ++tX;}
                else if (tX >= 1.0) {while (tX >= 1.0) --tX;}
                double tBoxLoX = mBox.xlo();
                double tBoxX = mBox.xhi() - tBoxLoX;
                return tBoxLoX + tX*tBoxX;
            }
            default: throw new RuntimeException();
            }
        }
        private double getY_(double aRawY) {
            double tY = aRawY;
            switch (mYType) {
            case NORMAL: {
                return tY;
            }
            case SCALED: {
                double tBoxLoY = mBox.ylo();
                double tBoxY = mBox.yhi() - tBoxLoY;
                return tBoxLoY + tY*tBoxY;
            }
            case UNWRAPPED: {
                double tBoxLoY = mBox.ylo();
                double tBoxHiY = mBox.yhi();
                double tBoxY = tBoxHiY - tBoxLoY;
                if      (tY <  tBoxLoY) {while (tY <  tBoxLoY) tY += tBoxY;}
                else if (tY >= tBoxHiY) {while (tY >= tBoxHiY) tY -= tBoxY;}
                return tY;
            }
            case SCALED_UNWRAPPED: {
                if      (tY <  0.0) {while (tY <  0.0) ++tY;}
                else if (tY >= 1.0) {while (tY >= 1.0) --tY;}
                double tBoxLoY = mBox.ylo();
                double tBoxY = mBox.yhi() - tBoxLoY;
                return tBoxLoY + tY*tBoxY;
            }
            default: throw new RuntimeException();
            }
        }
        private double getZ_(double aRawZ) {
            double tZ = aRawZ;
            switch (mZType) {
            case NORMAL: {
                return tZ;
            }
            case SCALED: {
                double tBoxLoZ = mBox.zlo();
                double tBoxZ = mBox.zhi() - tBoxLoZ;
                return tBoxLoZ + tZ*tBoxZ;
            }
            case UNWRAPPED: {
                double tBoxLoZ = mBox.zlo();
                double tBoxHiZ = mBox.zhi();
                double tBoxZ = tBoxHiZ - tBoxLoZ;
                if      (tZ <  tBoxLoZ) {while (tZ <  tBoxLoZ) tZ += tBoxZ;}
                else if (tZ >= tBoxHiZ) {while (tZ >= tBoxHiZ) tZ -= tBoxZ;}
                return tZ;
            }
            case SCALED_UNWRAPPED: {
                if      (tZ <  0.0) {while (tZ <  0.0) ++tZ;}
                else if (tZ >= 1.0) {while (tZ >= 1.0) --tZ;}
                double tBoxLoZ = mBox.zlo();
                double tBoxZ = mBox.zhi() - tBoxLoZ;
                return tBoxLoZ + tZ*tBoxZ;
            }
            default: throw new RuntimeException();
            }
        }
        
        /** AbstractAtomData stuffs */
        @Override public boolean hasVelocities() {return mHasVelocities;}
        @Override public List<IAtom> atoms() {
            return new AbstractRandomAccessList<IAtom>() {
                @Override public IAtom get(final int index) {
                    return new IAtom() {
                        @Override public double x() {if (mKeyX==null) throw new RuntimeException("No X data in this Lammpstrj"); return getX_(mAtomData.get(index, mKeyX));}
                        @Override public double y() {if (mKeyY==null) throw new RuntimeException("No Y data in this Lammpstrj"); return getY_(mAtomData.get(index, mKeyY));}
                        @Override public double z() {if (mKeyZ==null) throw new RuntimeException("No Z data in this Lammpstrj"); return getZ_(mAtomData.get(index, mKeyZ));}
                        
                        /** 如果没有 id 数据则 id 为顺序位置 +1 */
                        @Override public int id() {return mAtomData.containsHead("id") ? (int)mAtomData.get(index, "id") : index+1;}
                        /** 如果没有 type 数据则 type 都为 1 */
                        @Override public int type() {return mAtomData.containsHead("type") ? (int)mAtomData.get(index, "type") : 1;}
                        
                        @Override public double vx() {return mAtomData.containsHead("vx") ? mAtomData.get(index, "vx") : 0.0;}
                        @Override public double vy() {return mAtomData.containsHead("vy") ? mAtomData.get(index, "vy") : 0.0;}
                        @Override public double vz() {return mAtomData.containsHead("vz") ? mAtomData.get(index, "vz") : 0.0;}
                    };
                }
                @Override public int size() {return mAtomData.rowNumber();}
            };
        }
        @Override public IXYZ boxLo() {return mBox.boxLo();}
        @Override public IXYZ boxHi() {return mBox.boxHi();}
        @Override public int atomNum() {return mAtomData.rowNumber();}
        @Override public int atomTypeNum() {return mAtomTypeNum;}
        
        @Override public double volume() {return mBox.shiftedBox().prod();}
        
        @Override public SubLammpstrj copy() {return new SubLammpstrj(mTimeStep, Arrays.copyOf(mBoxBounds, mBoxBounds.length), mBox.copy(), mAtomData.copy());}
    }
    
    
    /** 拷贝一份 Lammpstrj */
    @Override public Lammpstrj copy() {
        List<SubLammpstrj> rData = new ArrayList<>(mData.size());
        for (SubLammpstrj subData : mData) rData.add(subData.copy());
        return new Lammpstrj(rData);
    }
    
    /// 创建 Lammpstrj
    /** 从 IAtomData 来创建，对于 Lammpstrj 可以支持容器的 aAtomData */
    public static Lammpstrj fromAtomData(IAtomData aAtomData, long aTimeStep) {
        return new Lammpstrj(fromAtomData_(aAtomData, aTimeStep));
    }
    public static Lammpstrj fromAtomData(IAtomData aAtomData) {
        return new Lammpstrj(fromAtomData_(aAtomData, getTimeStep(aAtomData, 0)));
    }
    public static Lammpstrj fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(fromAtomData_(subAtomData, getTimeStep(subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    static SubLammpstrj fromAtomData_(IAtomData aAtomData, long aTimeStep) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof Lammpstrj) {
            return fromAtomData_(((Lammpstrj)aAtomData).defaultFrame(), aTimeStep);
        } else
        if (aAtomData instanceof SubLammpstrj) {
            // SubLammpstrj 则直接获取即可（专门优化，保留排序，具体坐标的形式，对应的标签等）
            SubLammpstrj tSubLammpstrj = (SubLammpstrj)aAtomData;
            return new SubLammpstrj(aTimeStep, Arrays.copyOf(tSubLammpstrj.mBoxBounds, tSubLammpstrj.mBoxBounds.length), tSubLammpstrj.mBox.copy(), tSubLammpstrj.mAtomData.copy());
        } else {
            // 一般的情况，通过 dataXXX 来创建，注意这里认为获取时已经经过了值拷贝，因此不再需要 copy
            return new SubLammpstrj(aTimeStep, BOX_BOUND, new Box(aAtomData.boxLo(), aAtomData.boxHi()), aAtomData.hasVelocities()?aAtomData.dataAll():aAtomData.dataSTD());
        }
    }
    private static long getTimeStep(IAtomData aAtomData, long aDefault) {
        if (aAtomData instanceof Lammpstrj) return ((Lammpstrj)aAtomData).defaultFrame().mTimeStep;
        if (aAtomData instanceof SubLammpstrj) return ((SubLammpstrj)aAtomData).mTimeStep;
        return aDefault;
    }
    /** 对于 matlab 调用的兼容 */
    public static Lammpstrj fromAtomData_compat(Object... aAtomDataArray) {
        if (aAtomDataArray == null || aAtomDataArray.length == 0) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rLammpstrj.add(fromAtomData_((IAtomData)subAtomData, getTimeStep((IAtomData)subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    
    
    /// 文件读写
    /**
     * 从文件 lammps 输出的 dump 文件中读取来实现初始化
     * @author liqa
     * @param aFilePath lammps 输出的 dump 文件路径
     * @return 读取得到的 Lammpstrj 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     */
    public static Lammpstrj read(String aFilePath) throws IOException {try (BufferedReader tReader = UT.IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    public static Lammpstrj read_(BufferedReader aReader) throws IOException {
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        
        String tLine;
        String[] tTokens;
        while (true) {
            long aTimeStep;
            int tAtomNum;
            String[] aBoxBounds;
            Box aBox;
            String[] aAtomDataKeys;
            IMatrix aAtomData;
            
            // 读取时间步数
            UT.Texts.findLineContaining(aReader, "ITEM: TIMESTEP", true); tLine=aReader.readLine();
            if (tLine == null) break;
            tTokens = UT.Texts.splitBlank(tLine);
            aTimeStep = Long.parseLong(tTokens[0]);
            // 读取原子总数
            UT.Texts.findLineContaining(aReader, "ITEM: NUMBER OF ATOMS", true); tLine=aReader.readLine();
            if (tLine == null) break;
            tTokens = UT.Texts.splitBlank(tLine);
            tAtomNum = Integer.parseInt(tTokens[0]);
            // 读取模拟盒信息
            tLine = UT.Texts.findLineContaining(aReader, "ITEM: BOX BOUNDS", true);
            if (tLine == null) break;
            tTokens = UT.Texts.splitBlank(tLine);
            aBoxBounds = new String[] {tTokens[3], tTokens[4], tTokens[5]};
            tLine=aReader.readLine(); tTokens = UT.Texts.splitBlank(tLine);
            double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]);
            tLine=aReader.readLine(); tTokens = UT.Texts.splitBlank(tLine);
            double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]);
            tLine=aReader.readLine(); tTokens = UT.Texts.splitBlank(tLine);
            double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]);
            // 这里暂不考虑斜方模拟盒
            aBox = new Box(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi);
            
            // 读取原子信息
            tLine = UT.Texts.findLineContaining(aReader, "ITEM: ATOMS", true);
            if (tLine == null) break;
            tTokens = UT.Texts.splitBlank(tLine);
            aAtomDataKeys = new String[tTokens.length-2];
            System.arraycopy(tTokens, 2, aAtomDataKeys, 0, aAtomDataKeys.length);
            boolean tAtomDataReadFull = true;
            aAtomData = RowMatrix.zeros(tAtomNum, aAtomDataKeys.length);
            for (IVector tRow : aAtomData.rows()) {
                tLine = aReader.readLine();
                if (tLine == null) {tAtomDataReadFull = false; break;}
                tRow.fill(UT.Texts.str2data(tLine, aAtomDataKeys.length));
            }
            if (!tAtomDataReadFull) break;
            
            // 创建 SubLammpstrj 并附加到 rLammpstrj 中
            rLammpstrj.add(new SubLammpstrj(aTimeStep, aBoxBounds, aBox, new Table(aAtomDataKeys, aAtomData)));
        }
        return new Lammpstrj(rLammpstrj);
    }
    
    /**
     * 输出成 lammps 格式的 dump 文件，可以供 OVITO 等软件读取
     * <p>
     * 改为 {@link BufferedWriter} 而不是 {@code List<String>} 来避免过多内存占用
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {
        try (BufferedWriter tWriter = UT.IO.toWriter(aFilePath)) {
            for (SubLammpstrj tSubLammpstrj : this) {
                tWriter.write("ITEM: TIMESTEP"); tWriter.newLine();
                tWriter.write(String.format("%d", tSubLammpstrj.timeStep())); tWriter.newLine();
                tWriter.write("ITEM: NUMBER OF ATOMS"); tWriter.newLine();
                tWriter.write(String.format("%d", tSubLammpstrj.atomNum())); tWriter.newLine();
                tWriter.write(String.format("ITEM: BOX BOUNDS %s", String.join(" ", tSubLammpstrj.boxBounds()))); tWriter.newLine();
                tWriter.write(String.format("%f %f", tSubLammpstrj.box().xlo(), tSubLammpstrj.box().xhi())); tWriter.newLine();
                tWriter.write(String.format("%f %f", tSubLammpstrj.box().ylo(), tSubLammpstrj.box().yhi())); tWriter.newLine();
                tWriter.write(String.format("%f %f", tSubLammpstrj.box().zlo(), tSubLammpstrj.box().zhi())); tWriter.newLine();
                tWriter.write(String.format("ITEM: ATOMS %s", String.join(" ", tSubLammpstrj.mAtomData.heads()))); tWriter.newLine();
                for (IVector subAtomData : tSubLammpstrj.mAtomData.rows()) {
                    tWriter.write(String.join(" ", AbstractCollections.map(subAtomData.iterable(), Object::toString))); tWriter.newLine();
                }
            }
        }
    }
}
