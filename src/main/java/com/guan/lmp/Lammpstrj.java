package com.guan.lmp;

import com.google.common.collect.ImmutableMap;
import com.guan.atom.AbstractAtomData;
import com.guan.atom.AbstractMultiFrameAtomData;
import com.guan.atom.IHasAtomData;
import com.guan.code.UT;
import com.guan.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.guan.code.CS.STD_ATOM_DATA_KEYS;

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
        private final String[] mAtomDataKeys;
        private final double[][] mAtomData;
        private final int mAtomTypeNum;
        
        private int mTypeCol = -1;
        private final Map<String, Integer> mKey2Idx;
        private int mXCol = -1, mYCol = -1, mZCol = -1;
        private final XYZType mXYZType;
        private final XYZType @Nullable[] mXYZTypeDetail;
        
        public SubLammpstrj(long aTimeStep, String[] aBoxBounds, Box aBox, String[] aAtomDataKeys, double[][] aAtomData) {
            mTimeStep = aTimeStep;
            mBoxBounds = aBoxBounds;
            mBox = aBox;
            mAtomDataKeys = aAtomDataKeys;
            mAtomData = aAtomData;
            
            ImmutableMap.Builder<String, Integer> tMapBuilder = new ImmutableMap.Builder<>();
            String tKeyX = "x";
            String tKeyY = "y";
            String tKeyZ = "z";
            for (int i = 0; i < mAtomDataKeys.length; ++i) {
                String tKey = mAtomDataKeys[i];
                tMapBuilder.put(tKey, i);
                if (tKey.equals("type")) mTypeCol = i;
                if (tKey.equals("x") || tKey.equals("xs") || tKey.equals("xu") || tKey.equals("xsu")) {tKeyX = tKey; mXCol = i;}
                if (tKey.equals("y") || tKey.equals("ys") || tKey.equals("yu") || tKey.equals("ysu")) {tKeyY = tKey; mYCol = i;}
                if (tKey.equals("z") || tKey.equals("zs") || tKey.equals("zu") || tKey.equals("zsu")) {tKeyZ = tKey; mZCol = i;}
            }
            mKey2Idx = tMapBuilder.build();
            
            if      (tKeyX.equals("x"  ) && tKeyY.equals("y"  ) && tKeyZ.equals("z"  )) mXYZType = XYZType.NORMAL;
            else if (tKeyX.equals("xs" ) && tKeyY.equals("ys" ) && tKeyZ.equals("zs" )) mXYZType = XYZType.SCALED;
            else if (tKeyX.equals("xu" ) && tKeyY.equals("yu" ) && tKeyZ.equals("zu" )) mXYZType = XYZType.UNWRAPPED;
            else if (tKeyX.equals("xsu") && tKeyY.equals("ysu") && tKeyZ.equals("zsu")) mXYZType = XYZType.SCALED_UNWRAPPED;
            else mXYZType = XYZType.MIXED;
            
            if (mXYZType == XYZType.MIXED) {
                mXYZTypeDetail = new XYZType[3];
                switch (tKeyX) {
                case "x"  : {mXYZTypeDetail[0] = XYZType.NORMAL;             break;}
                case "xs" : {mXYZTypeDetail[0] = XYZType.SCALED;             break;}
                case "xu" : {mXYZTypeDetail[0] = XYZType.UNWRAPPED;          break;}
                case "xsu": {mXYZTypeDetail[0] = XYZType.SCALED_UNWRAPPED;   break;}
                }
                switch (tKeyY) {
                case "y"  : {mXYZTypeDetail[1] = XYZType.NORMAL;             break;}
                case "ys" : {mXYZTypeDetail[1] = XYZType.SCALED;             break;}
                case "yu" : {mXYZTypeDetail[1] = XYZType.UNWRAPPED;          break;}
                case "ysu": {mXYZTypeDetail[1] = XYZType.SCALED_UNWRAPPED;   break;}
                }
                switch (tKeyZ) {
                case "z"  : {mXYZTypeDetail[2] = XYZType.NORMAL;             break;}
                case "zs" : {mXYZTypeDetail[2] = XYZType.SCALED;             break;}
                case "zu" : {mXYZTypeDetail[2] = XYZType.UNWRAPPED;          break;}
                case "zsu": {mXYZTypeDetail[2] = XYZType.SCALED_UNWRAPPED;   break;}
                }
            } else {
                mXYZTypeDetail = null;
            }
            
            // 对于 dump，mAtomTypeNum 只能手动遍历统计
            int tAtomTypeNum = 1;
            if (mTypeCol >= 0) for (double[] subAtomData : mAtomData) {
                int tType = (int)subAtomData[mTypeCol];
                if (tType > tAtomTypeNum) tAtomTypeNum = tType;
            }
            mAtomTypeNum = tAtomTypeNum;
        }
        
        private enum XYZType {
              NORMAL
            , SCALED
            , UNWRAPPED
            , SCALED_UNWRAPPED
            , MIXED
        }
        
        // dump 额外的属性
        public long timeStep() {return mTimeStep;}
        public String[] boxBounds() {return mBoxBounds;}
        public Box box() {return mBox;}
        
        /** AbstractAtomData stuffs */
        @Override public int atomTypeNum() {return mAtomTypeNum;}
        @Override public String[] atomDataKeys() {return mAtomDataKeys;}
        @Override public double[][] atomData() {return mAtomData;}
        @Override public double[] boxLo() {return mBox.boxLo();}
        @Override public double[] boxHi() {return mBox.boxHi();}
        
        /** 重写这个方法来对于不同格式的 xyz dump 都能直接使用 */
        @Override public int xCol() {return mXCol;}
        @Override public int yCol() {return mYCol;}
        @Override public int zCol() {return mZCol;}
        @Override public double[][] toOrthogonalXYZ_(double[][] aAtomDataXYZ) {
            switch (mXYZType) {
            case NORMAL: {return aAtomDataXYZ;}
            case SCALED: {
                // 由于需要修改，根据约定先进行值拷贝
                aAtomDataXYZ = MathEX.Mat.copy(aAtomDataXYZ);
                // 根据 Box 重新缩放
                MathEX.XYZ.unscaleAtomDataXYZ(aAtomDataXYZ, mBox.boxLo(), mBox.boxHi());
                return aAtomDataXYZ;
            }
            case UNWRAPPED: {
                // 由于需要修改，根据约定先进行值拷贝
                aAtomDataXYZ = MathEX.Mat.copy(aAtomDataXYZ);
                // 根据 Box 进行 wrap
                MathEX.XYZ.wrapAtomDataXYZ(aAtomDataXYZ, mBox.boxLo(), mBox.boxHi());
                return aAtomDataXYZ;
            }
            case SCALED_UNWRAPPED: {
                // 由于需要修改，根据约定先进行值拷贝
                aAtomDataXYZ = MathEX.Mat.copy(aAtomDataXYZ);
                // 先进行 wrap
                MathEX.XYZ.wrapScaledAtomDataXYZ(aAtomDataXYZ);
                // 再根据 Box 重新缩放
                MathEX.XYZ.unscaleAtomDataXYZ(aAtomDataXYZ, mBox.boxLo(), mBox.boxHi());
                return aAtomDataXYZ;
            }
            case MIXED: {
                assert mXYZTypeDetail != null;
                // 由于需要修改，根据约定先进行值拷贝
                aAtomDataXYZ = MathEX.Mat.copy(aAtomDataXYZ);
                // 混合情况直接手动遍历实现
                double tBoxLoX = mBox.xlo(), tBoxLoY = mBox.ylo(), tBoxLoZ = mBox.zlo();
                double tBoxHiX = mBox.xhi(), tBoxHiY = mBox.yhi(), tBoxHiZ = mBox.zhi();
                double tBoxX = tBoxHiX-tBoxLoX, tBoxY = tBoxHiY-tBoxLoY, tBoxZ = tBoxHiZ-tBoxLoZ;
                XYZType tXType = mXYZTypeDetail[0], tYType = mXYZTypeDetail[1], tZType = mXYZTypeDetail[2];
                for (double[] rXYZ : aAtomDataXYZ) {
                    switch (tXType) {
                    case NORMAL: default: break;
                    case SCALED: {
                        rXYZ[0] *= tBoxX; rXYZ[0] += tBoxLoX;
                        break;
                    }
                    case UNWRAPPED: {
                        double tX = rXYZ[0];
                        if      (tX <  tBoxLoX) {while (rXYZ[0] <  tBoxLoX) rXYZ[0] += tBoxX;}
                        else if (tX >= tBoxHiX) {while (rXYZ[0] >= tBoxHiX) rXYZ[0] -= tBoxX;}
                        break;
                    }
                    case SCALED_UNWRAPPED: {
                        double tX = rXYZ[0];
                        if      (tX <  0.0) {while (rXYZ[0] <  0.0) ++rXYZ[0];}
                        else if (tX >= 1.0) {while (rXYZ[0] >= 1.0) --rXYZ[0];}
                        rXYZ[0] *= tBoxX; rXYZ[0] += tBoxLoX;
                        break;
                    }}
                    switch (tYType) {
                    case NORMAL: default: break;
                    case SCALED: {
                        rXYZ[1] *= tBoxY; rXYZ[1] += tBoxLoY;
                        break;
                    }
                    case UNWRAPPED: {
                        double tY = rXYZ[1];
                        if      (tY <  tBoxLoY) {while (rXYZ[1] <  tBoxLoY) rXYZ[1] += tBoxY;}
                        else if (tY >= tBoxHiY) {while (rXYZ[1] >= tBoxHiY) rXYZ[1] -= tBoxY;}
                        break;
                    }
                    case SCALED_UNWRAPPED: {
                        double tY = rXYZ[1];
                        if      (tY <  0.0) {while (rXYZ[1] <  0.0) ++rXYZ[1];}
                        else if (tY >= 1.0) {while (rXYZ[1] >= 1.0) --rXYZ[1];}
                        rXYZ[1] *= tBoxY; rXYZ[1] += tBoxLoY;
                        break;
                    }}
                    switch (tZType) {
                    case NORMAL: default: break;
                    case SCALED: {
                        rXYZ[2] *= tBoxZ; rXYZ[2] += tBoxLoZ;
                        break;
                    }
                    case UNWRAPPED: {
                        double tZ = rXYZ[2];
                        if      (tZ <  tBoxLoZ) {while (rXYZ[2] <  tBoxLoZ) rXYZ[2] += tBoxZ;}
                        else if (tZ >= tBoxHiZ) {while (rXYZ[2] >= tBoxHiZ) rXYZ[2] -= tBoxZ;}
                        break;
                    }
                    case SCALED_UNWRAPPED: {
                        double tZ = rXYZ[2];
                        if      (tZ <  0.0) {while (rXYZ[2] <  0.0) ++rXYZ[2];}
                        else if (tZ >= 1.0) {while (rXYZ[2] >= 1.0) --rXYZ[2];}
                        rXYZ[2] *= tBoxZ; rXYZ[2] += tBoxLoZ;
                        break;
                    }}
                }
                return aAtomDataXYZ;
            }
            default: throw new RuntimeException();
            }
        }
        @Override public double[][] toOrthogonalXYZID_(double[][] aAtomDataXYZID) {return toOrthogonalXYZ_(aAtomDataXYZID);} // 由于前 0-2 列一样都是 xyz，因此操作可以通用
        
        /** 重写来优化索引过程 */
        @Override public int typeCol() {return mTypeCol;}
        @Override public int key2idx(String aKey) {
            Integer tIdx = mKey2Idx.get(aKey);
            return tIdx==null ? -1 : tIdx;
        }
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
        // 粒子数据，可以是任意格式
        double[][] tAtomData;
        String[] tAtomDataKeys;
        int tAtomTypeNum = aHasAtomData.atomTypeNum();
        // 根据输入的 aHasAtomData 类型来具体判断需要如何获取 rAtomData
        if (aHasAtomData instanceof SubLammpstrj || aHasAtomData instanceof Lammpstrj) {
            // SubLammpstrj 则直接获取即可（专门优化，保留排序，具体坐标的形式，对应的标签等，注意时间步会抹除）
            tAtomData = aHasAtomData.atomData();
            tAtomDataKeys = aHasAtomData.atomDataKeys();
        } else {
            // 一般的情况，注意标签也要改为默认标签
            tAtomData = IHasAtomData.Util.toStandardAtomData(aHasAtomData);
            tAtomDataKeys = STD_ATOM_DATA_KEYS;
        }
        return new SubLammpstrj(aTimeStep, BOX_BOUND, new Box(aHasAtomData.boxLo(), aHasAtomData.boxHi()), tAtomDataKeys, tAtomData);
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
            double[][] aAtomData;
            
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
            aAtomData = new double[tAtomNum][aAtomDataKeys.length];
            for (int i = 0; i < tAtomNum; ++i) {
                tTokens = UT.Texts.splitBlank(aLines[idx]);
                for(int j = 0; j < aAtomDataKeys.length; ++j) aAtomData[i][j] = Double.parseDouble(tTokens[j]);
                ++idx;
            }
            
            // 创建 SubLammpstrj 并附加到 rLammpstrj 中
            rLammpstrj.add(new SubLammpstrj(aTimeStep, aBoxBounds, aBox, aAtomDataKeys, aAtomData));
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
            lines.add(String.format("ITEM: ATOMS %s", String.join(" ", tSubLammpstrj.atomDataKeys())));
            for (double[] subAtomData : tSubLammpstrj.atomData())
            lines.add(String.join(" ", UT.IO.data2str(subAtomData)));
        }
        lines.add("");
        
        UT.IO.write(aFilePath, lines);
    }
}
