package com.jtool.lmp;

import com.jtool.atom.*;
import com.jtool.code.UT;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;
import com.jtool.math.vector.IVector;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
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
    private static final String[] BOX_BOUND = {"pp", "pp", "pp"};
    
    private final SubLammpstrj[] mData;
    
    public Lammpstrj(SubLammpstrj... aData) {mData = aData==null ? new SubLammpstrj[0] : aData;}
    public Lammpstrj(Collection<SubLammpstrj> aData) {mData = aData.toArray(new SubLammpstrj[0]);}
    
    /** AbstractList stuffs */
    @Override public int size() {return mData.length;}
    @Override public SubLammpstrj get(int index) {return mData[index];}
    
    // dump 额外的属性
    public long timeStep() {return defaultFrame().timeStep();}
    public String[] boxBounds() {return defaultFrame().boxBounds();}
    public Box box() {return defaultFrame().box();}
    
    /** 每个帧的子 Lammpstrj */
    public static class SubLammpstrj extends AbstractAtomData {
        private final long mTimeStep;
        private final String[] mBoxBounds;
        private final Box mBox;
        private final ITable mAtomData;
        private final int mAtomTypeNum;
        
        private int mTypeCol = -1;
        private int mIDCol = -1;
        private int mXCol = -1, mYCol = -1, mZCol = -1;
        private final XYZType mXType, mYType, mZType;
        
        
        public SubLammpstrj(long aTimeStep, String[] aBoxBounds, Box aBox, ITable aAtomData) {
            mTimeStep = aTimeStep;
            mBoxBounds = aBoxBounds;
            mBox = aBox;
            mAtomData = aAtomData;
            
            String tKeyX = "x";
            String tKeyY = "y";
            String tKeyZ = "z";
            for (int i = 0; i < aAtomData.columnNumber(); ++i) {
                String tKey = aAtomData.getHead(i);
                if (tKey.equals("type")) mTypeCol = i;
                if (tKey.equals("id")) mIDCol = i;
                if (tKey.equals("x") || tKey.equals("xs") || tKey.equals("xu") || tKey.equals("xsu")) {tKeyX = tKey; mXCol = i;}
                if (tKey.equals("y") || tKey.equals("ys") || tKey.equals("yu") || tKey.equals("ysu")) {tKeyY = tKey; mYCol = i;}
                if (tKey.equals("z") || tKey.equals("zs") || tKey.equals("zu") || tKey.equals("zsu")) {tKeyZ = tKey; mZCol = i;}
            }
            
            switch (tKeyX) {
            case "x"  : {mXType = XYZType.NORMAL;             break;}
            case "xs" : {mXType = XYZType.SCALED;             break;}
            case "xu" : {mXType = XYZType.UNWRAPPED;          break;}
            case "xsu": {mXType = XYZType.SCALED_UNWRAPPED;   break;}
            default: throw new RuntimeException();
            }
            switch (tKeyY) {
            case "y"  : {mYType = XYZType.NORMAL;             break;}
            case "ys" : {mYType = XYZType.SCALED;             break;}
            case "yu" : {mYType = XYZType.UNWRAPPED;          break;}
            case "ysu": {mYType = XYZType.SCALED_UNWRAPPED;   break;}
            default: throw new RuntimeException();
            }
            switch (tKeyZ) {
            case "z"  : {mZType = XYZType.NORMAL;             break;}
            case "zs" : {mZType = XYZType.SCALED;             break;}
            case "zu" : {mZType = XYZType.UNWRAPPED;          break;}
            case "zsu": {mZType = XYZType.SCALED_UNWRAPPED;   break;}
            default: throw new RuntimeException();
            }
            
            // 对于 dump，mAtomTypeNum 只能手动遍历统计
            int tAtomTypeNum = 1;
            if (mTypeCol >= 0) {
                tAtomTypeNum = (int)mAtomData.col(mTypeCol).operation().max();
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
        
        /** 内部方法，用于从一行的数据获取合适的 x，y，z 数据 */
        private double getX_(IVector aRow) {
            if (mXCol < 0) throw new RuntimeException("No X data in this Lammpstrj");
            double tX = aRow.get_(mXCol);
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
        private double getY_(IVector aRow) {
            if (mYCol < 0) throw new RuntimeException("No Y data in this Lammpstrj");
            double tY = aRow.get_(mYCol);
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
        private double getZ_(IVector aRow) {
            if (mZCol < 0) throw new RuntimeException("No Z data in this Lammpstrj");
            double tZ = aRow.get_(mZCol);
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
        @Override public List<IAtom> atoms() {
            return new AbstractList<IAtom>() {
                @Override public IAtom get(final int index) {
                    return new IAtom() {
                        private final IVector mRow = mAtomData.row(index);
                        @Override public double x() {return getX_(mRow);}
                        @Override public double y() {return getY_(mRow);}
                        @Override public double z() {return getZ_(mRow);}
                        
                        /** 如果没有 id 数据则 id 为顺序位置 +1 */
                        @Override public int id() {return (mIDCol < 0) ? index+1 : (int)mRow.get(mIDCol);}
                        /** 如果没有 type 数据则 type 都为 1 */
                        @Override public int type() {return (mTypeCol < 0) ? 1 : (int)mRow.get(mTypeCol);}
                    };
                }
                @Override public int size() {return mAtomData.rowNumber();}
            };
        }
        @Override public IHasXYZ boxLo() {return mBox.boxLo();}
        @Override public IHasXYZ boxHi() {return mBox.boxHi();}
        @Override public int atomNum() {return mAtomData.rowNumber();}
        @Override public int atomTypeNum() {return mAtomTypeNum;}
        
        @Override public double volume() {return mBox.shiftedBox().product();}
    }
    
    
    /// 创建 Lammpstrj
    /** 从 IHasAtomData 来创建，对于 Lammpstrj 可以支持容器的 aHasAtomData */
    public static Lammpstrj fromAtomData(IHasAtomData... aHasAtomDataArray) {
        if (aHasAtomDataArray == null || aHasAtomDataArray.length == 0) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int tTimeStep = 0;
        for (IHasAtomData subHasAtomData : aHasAtomDataArray) {
            rLammpstrj.add(fromAtomData_(subHasAtomData, tTimeStep));
            ++tTimeStep;
        }
        return new Lammpstrj(rLammpstrj);
    }
    public static Lammpstrj fromAtomData(Iterable<? extends IHasAtomData> aHasAtomDataList) {
        if (aHasAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int tTimeStep = 0;
        for (IHasAtomData subHasAtomData : aHasAtomDataList) {
            rLammpstrj.add(fromAtomData_(subHasAtomData, tTimeStep));
            ++tTimeStep;
        }
        return new Lammpstrj(rLammpstrj);
    }
    static SubLammpstrj fromAtomData_(IHasAtomData aHasAtomData, long aTimeStep) {
        // 根据输入的 aHasAtomData 类型来具体判断需要如何获取 rAtomData
        if (aHasAtomData instanceof Lammpstrj) {
            return fromAtomData_(((Lammpstrj)aHasAtomData).defaultFrame(), aTimeStep);
        } else
        if (aHasAtomData instanceof SubLammpstrj) {
            // SubLammpstrj 则直接获取即可（专门优化，保留排序，具体坐标的形式，对应的标签等，注意时间步会抹除）
            SubLammpstrj tSubLammpstrj = (SubLammpstrj)aHasAtomData;
            return new SubLammpstrj(aTimeStep, BOX_BOUND, tSubLammpstrj.mBox.copy(), tSubLammpstrj.mAtomData);
        } else {
            // 一般的情况，通过 dataSTD 来创建，注意这里认为获取时已经经过了值拷贝，因此不再需要 copy
            return new SubLammpstrj(aTimeStep, BOX_BOUND, new Box(aHasAtomData.boxLo(), aHasAtomData.boxHi()), aHasAtomData.dataSTD());
        }
    }
    /** 对于 matlab 调用的兼容 */
    public static Lammpstrj fromAtomData_compat(Object... aHasAtomDataArray) {
        if (aHasAtomDataArray == null || aHasAtomDataArray.length == 0) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int tTimeStep = 0;
        for (Object subHasAtomData : aHasAtomDataArray) if (subHasAtomData instanceof IHasAtomData) {
            rLammpstrj.add(fromAtomData_((IHasAtomData)subHasAtomData, tTimeStep));
            ++tTimeStep;
        }
        return new Lammpstrj(rLammpstrj);
    }
    
    
    /// 文件读写
    /**
     * 从文件 lammps 输出的 dump 文件中读取来实现初始化
     * @param aFilePath lammps 输出的 dump 文件路径
     * @return 读取得到的 Lammpstrj 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     */
    public static Lammpstrj read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    public static Lammpstrj read_(String[] aLines) {
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        
        int idx = 0;
        String[] tTokens;
        while (idx < aLines.length) {
            long aTimeStep;
            int tAtomNum;
            String[] aBoxBounds;
            Box aBox;
            String[] aAtomDataKeys;
            List<double[]> aAtomData;
            
            // 读取时间步数
            idx = UT.Texts.findLineContaining(aLines, idx, "ITEM: TIMESTEP"); ++idx;
            if (idx >= aLines.length) break;
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            aTimeStep = Long.parseLong(tTokens[0]);
            // 读取原子总数
            idx = UT.Texts.findLineContaining(aLines, idx, "ITEM: NUMBER OF ATOMS"); ++idx;
            if (idx >= aLines.length) break;
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            tAtomNum = Integer.parseInt(tTokens[0]);
            // 读取模拟盒信息
            idx = UT.Texts.findLineContaining(aLines, idx, "ITEM: BOX BOUNDS");
            if (idx >= aLines.length) break;
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            aBoxBounds = new String[] {tTokens[3], tTokens[4], tTokens[5]};
            ++idx; tTokens = UT.Texts.splitBlank(aLines[idx]);
            double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]);
            ++idx; tTokens = UT.Texts.splitBlank(aLines[idx]);
            double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]);
            ++idx; tTokens = UT.Texts.splitBlank(aLines[idx]);
            double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]);
            // 这里暂不考虑斜方模拟盒
            aBox = new Box(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi);
            
            // 读取原子信息
            idx = UT.Texts.findLineContaining(aLines, idx, "ITEM: ATOMS");
            if (idx >= aLines.length) break;
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            aAtomDataKeys = new String[tTokens.length-2];
            System.arraycopy(tTokens, 2, aAtomDataKeys, 0, aAtomDataKeys.length);
            ++idx;
            if (idx+tAtomNum > aLines.length) break;
            aAtomData = new ArrayList<>(tAtomNum);
            for (int i = 0; i < tAtomNum; ++i) {
                tTokens = UT.Texts.splitBlank(aLines[idx]);
                aAtomData.add(UT.IO.str2data(tTokens));
                ++idx;
            }
            
            // 创建 SubLammpstrj 并附加到 rLammpstrj 中
            rLammpstrj.add(new SubLammpstrj(aTimeStep, aBoxBounds, aBox, new Table(aAtomDataKeys, aAtomData)));
        }
        return new Lammpstrj(rLammpstrj);
    }
    
    /**
     * 输出成 lammps 格式的 dump 文件，可以供 OVITO 等软件读取
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    public void write(String aFilePath) throws IOException {
        List<String> lines = new ArrayList<>();
        
        for (SubLammpstrj tSubLammpstrj : this) {
            lines.add("ITEM: TIMESTEP");
            lines.add(String.format("%d", tSubLammpstrj.timeStep()));
            lines.add("ITEM: NUMBER OF ATOMS");
            lines.add(String.format("%d", tSubLammpstrj.atomNum()));
            lines.add(String.format("ITEM: BOX BOUNDS %s", String.join(" ", tSubLammpstrj.boxBounds())));
            lines.add(String.format("%f %f", tSubLammpstrj.box().xlo(), tSubLammpstrj.box().xhi()));
            lines.add(String.format("%f %f", tSubLammpstrj.box().ylo(), tSubLammpstrj.box().yhi()));
            lines.add(String.format("%f %f", tSubLammpstrj.box().zlo(), tSubLammpstrj.box().zhi()));
            lines.add(String.format("ITEM: ATOMS %s", String.join(" ", tSubLammpstrj.mAtomData.heads())));
            for (IVector subAtomData : tSubLammpstrj.mAtomData.rows())
            lines.add(String.join(" ", UT.Code.map(subAtomData.iterable(), Object::toString)));
        }
        lines.add("");
        
        UT.IO.write(aFilePath, lines);
    }
}
