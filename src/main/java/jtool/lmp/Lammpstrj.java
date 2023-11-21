package jtool.lmp;

import jtool.atom.*;
import jtool.code.UT;
import jtool.code.collection.AbstractCollections;
import jtool.code.collection.NewCollections;
import jtool.math.MathEX;
import jtool.math.table.AbstractMultiFrameTable;
import jtool.math.table.ITable;
import jtool.math.table.Tables;
import jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;


/**
 * @author liqa
 * <p> lammps 使用 dump 输出的数据格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 支持拥有多个时间帧的输出（可以当作 List 来使用），
 * 也可直接获取结果（则会直接获取第一个时间帧的结果） </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 */
public class Lammpstrj extends AbstractMultiFrameSettableAtomData<Lammpstrj.SubLammpstrj> {
    private final static String[] BOX_BOUND = {"pp", "pp", "pp"};
    
    private List<SubLammpstrj> mData;
    
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
    public Lammpstrj appendList(Iterable<? extends IAtomData> aAtomDataList) {
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
    public Box lmpBox() {return defaultFrame().lmpBox();}
    
    /** 提供直接转为表格的接口 */
    public AbstractMultiFrameTable<ITable> asTable() {
        return new AbstractMultiFrameTable<ITable>() {
            @Override public AbstractMultiFrameTable<ITable> copy() {return Lammpstrj.this.copy().asTable();}
            @Override public ITable get(int index) {return Lammpstrj.this.get(index).asTable();}
            @Override public int size() {return Lammpstrj.this.size();}
            @Override public ITable defaultFrame() {return Lammpstrj.this.defaultFrame().asTable();}
        };
    }
    
    /** 密度归一化, 返回自身来支持链式调用 */
    public Lammpstrj setDenseNormalized() {
        for (SubLammpstrj tSubLammpstrj : this) tSubLammpstrj.setDenseNormalized();
        return this;
    }
    /** 截断开头一部分, 返回自身来支持链式调用 */
    public Lammpstrj cutFront(int aLength) {
        if (aLength <= 0) return this;
        if (aLength > mData.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        List<SubLammpstrj> oData = mData;
        mData = new ArrayList<>(oData.size() - aLength);
        Iterator<SubLammpstrj> it = oData.listIterator(aLength);
        while (it.hasNext()) mData.add(it.next());
        return this;
    }
    /** 截断结尾一部分, 返回自身来支持链式调用 */
    public Lammpstrj cutBack(int aLength) {
        if (aLength <= 0) return this;
        if (aLength > mData.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        for (int i = 0; i < aLength; ++i) UT.Code.removeLast(mData);
        return this;
    }
    
    
    /** 每个帧的子 Lammpstrj */
    public static class SubLammpstrj extends AbstractSettableAtomData {
        private final long mTimeStep;
        private final String[] mBoxBounds;
        private final ITable mAtomData;
        private int mAtomTypeNum;
        private Box mBox;
        
        String mKeyX = null, mKeyY = null, mKeyZ = null;
        private final boolean mHasVelocities;
        
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
            mHasVelocities = (mAtomData.containsHead("vx") || mAtomData.containsHead("vy") || mAtomData.containsHead("vz"));
            
            // 对于 dump，mAtomTypeNum 只能手动遍历统计
            int tAtomTypeNum = 1;
            if (mAtomData.containsHead("type")) {
                tAtomTypeNum = (int)mAtomData.col("type").max();
            }
            mAtomTypeNum = tAtomTypeNum;
        }
        
        // dump 额外的属性
        public long timeStep() {return mTimeStep;}
        public String[] boxBounds() {return mBoxBounds;}
        public Box lmpBox() {return mBox;}
        
        /** 密度归一化, 返回自身来支持链式调用 */
        public SubLammpstrj setDenseNormalized() {
            if (mKeyX==null) throw new RuntimeException("No X data in this Lammpstrj");
            if (mKeyY==null) throw new RuntimeException("No Y data in this Lammpstrj");
            if (mKeyZ==null) throw new RuntimeException("No Z data in this Lammpstrj");
            
            XYZ oShiftedBox = XYZ.toXYZ(mBox.shiftedBox());
            double tScale = MathEX.Fast.cbrt(oShiftedBox.prod() / atomNum());
            tScale = 1.0 / tScale;
            
            // 从逻辑上考虑，这里不对原本数据做值拷贝
            switch (mKeyX) {
            case "x": case "xu": {
                IVector tCol = mAtomData.col(mKeyX);
                tCol.minus2this(mBox.xlo());
                tCol.multiply2this(tScale);
                break;
            }
            case "xs": case "xsu": {break;}
            default: throw new RuntimeException();
            }
            switch (mKeyY) {
            case "y": case "yu": {
                IVector tCol = mAtomData.col(mKeyY);
                tCol.minus2this(mBox.ylo());
                tCol.multiply2this(tScale);
                break;
            }
            case "ys": case "ysu": {break;}
            default: throw new RuntimeException();
            }
            switch (mKeyZ) {
            case "z": case "zu": {
                IVector tCol = mAtomData.col(mKeyZ);
                tCol.minus2this(mBox.zlo());
                tCol.multiply2this(tScale);
                break;
            }
            case "zs": case "zsu": {break;}
            default: throw new RuntimeException();
            }
            
            if (mAtomData.containsHead("vx")) mAtomData.col("vx").multiply2this(tScale);
            if (mAtomData.containsHead("vy")) mAtomData.col("vy").multiply2this(tScale);
            if (mAtomData.containsHead("vz")) mAtomData.col("vz").multiply2this(tScale);
            
            // box 还是会重新创建，因为 box 的值这里约定是严格的常量，可以避免一些问题
            mBox = new Box(oShiftedBox.multiply(tScale));
            
            return this;
        }
        
        /** 内部方法，用于从原始的数据获取合适的 x，y，z 数据 */
        private double getX_(int aIdx) {
            double tX = mAtomData.get(aIdx, mKeyX);
            switch (mKeyX) {
            case "x": {
                return tX-mBox.xlo();
            }
            case "xs": {
                return tX*(mBox.xhi()-mBox.xlo());
            }
            case "xu": {
                double tBoxLoX = mBox.xlo();
                double tBoxHiX = mBox.xhi();
                double tBoxX = tBoxHiX - tBoxLoX;
                if      (tX <  tBoxLoX) {while (tX <  tBoxLoX) tX += tBoxX;}
                else if (tX >= tBoxHiX) {while (tX >= tBoxHiX) tX -= tBoxX;}
                return tX-tBoxLoX;
            }
            case "xsu": {
                if      (tX <  0.0) {while (tX <  0.0) ++tX;}
                else if (tX >= 1.0) {while (tX >= 1.0) --tX;}
                return tX*(mBox.xhi()-mBox.xlo());
            }
            default: throw new RuntimeException();
            }
        }
        private double getY_(int aIdx) {
            double tY = mAtomData.get(aIdx, mKeyY);
            switch (mKeyY) {
            case "y": {
                return tY-mBox.ylo();
            }
            case "ys": {
                return tY*(mBox.yhi()-mBox.ylo());
            }
            case "yu": {
                double tBoxLoY = mBox.ylo();
                double tBoxHiY = mBox.yhi();
                double tBoxY = tBoxHiY - tBoxLoY;
                if      (tY <  tBoxLoY) {while (tY <  tBoxLoY) tY += tBoxY;}
                else if (tY >= tBoxHiY) {while (tY >= tBoxHiY) tY -= tBoxY;}
                return tY-tBoxLoY;
            }
            case "ysu": {
                if      (tY <  0.0) {while (tY <  0.0) ++tY;}
                else if (tY >= 1.0) {while (tY >= 1.0) --tY;}
                return tY*(mBox.yhi()-mBox.ylo());
            }
            default: throw new RuntimeException();
            }
        }
        private double getZ_(int aIdx) {
            double tZ = mAtomData.get(aIdx, mKeyZ);
            switch (mKeyZ) {
            case "z": {
                return tZ-mBox.zlo();
            }
            case "zs": {
                return tZ*(mBox.zhi()-mBox.zlo());
            }
            case "zu": {
                double tBoxLoZ = mBox.zlo();
                double tBoxHiZ = mBox.zhi();
                double tBoxZ = tBoxHiZ - tBoxLoZ;
                if      (tZ <  tBoxLoZ) {while (tZ <  tBoxLoZ) tZ += tBoxZ;}
                else if (tZ >= tBoxHiZ) {while (tZ >= tBoxHiZ) tZ -= tBoxZ;}
                return tZ-tBoxLoZ;
            }
            case "zsu": {
                if      (tZ <  0.0) {while (tZ <  0.0) ++tZ;}
                else if (tZ >= 1.0) {while (tZ >= 1.0) --tZ;}
                return tZ*(mBox.zhi()-mBox.zlo());
            }
            default: throw new RuntimeException();
            }
        }
        
        /** 内部方法，用于从原始的数据来设置内部 x，y，z 数据 */
        private void setX_(int aIdx, double aX) {
            switch (mKeyX) {
            case "x": case "xu": {
                mAtomData.set(aIdx, mKeyX, aX+mBox.xlo()); break;
            }
            case "xs": case "xsu": {
                mAtomData.set(aIdx, mKeyX, aX/(mBox.xhi()-mBox.xlo())); break;
            }
            default: throw new RuntimeException();
            }
        }
        private void setY_(int aIdx, double aY) {
            switch (mKeyY) {
            case "y": case "yu": {
                mAtomData.set(aIdx, mKeyY, aY+mBox.ylo()); break;
            }
            case "ys": case "ysu": {
                mAtomData.set(aIdx, mKeyY, aY/(mBox.yhi()-mBox.ylo())); break;
            }
            default: throw new RuntimeException();
            }
        }
        private void setZ_(int aIdx, double aZ) {
            switch (mKeyZ) {
            case "z": case "zu": {
                mAtomData.set(aIdx, mKeyZ, aZ+mBox.zlo()); break;
            }
            case "zs": case "zsu": {
                mAtomData.set(aIdx, mKeyZ, aZ/(mBox.zhi()-mBox.zlo())); break;
            }
            default: throw new RuntimeException();
            }
        }
        
        
        /** AbstractAtomData stuffs */
        @Override public boolean hasVelocities() {return mHasVelocities;}
        @Override public ISettableAtom pickAtom(final int aIdx) {
            return new AbstractSettableAtom() {
                @Override public double x() {if (mKeyX==null) throw new RuntimeException("No X data in this Lammpstrj"); return getX_(aIdx);}
                @Override public double y() {if (mKeyY==null) throw new RuntimeException("No Y data in this Lammpstrj"); return getY_(aIdx);}
                @Override public double z() {if (mKeyZ==null) throw new RuntimeException("No Z data in this Lammpstrj"); return getZ_(aIdx);}
                
                /** 如果没有 id 数据则 id 为顺序位置 +1 */
                @Override public int id() {return mAtomData.containsHead("id") ? (int)mAtomData.get(aIdx, "id") : aIdx+1;}
                /** 如果没有 type 数据则 type 都为 1 */
                @Override public int type() {return mAtomData.containsHead("type") ? (int)mAtomData.get(aIdx, "type") : 1;}
                @Override public int index() {return aIdx;}
                
                @Override public double vx() {return mAtomData.containsHead("vx") ? mAtomData.get(aIdx, "vx") : 0.0;}
                @Override public double vy() {return mAtomData.containsHead("vy") ? mAtomData.get(aIdx, "vy") : 0.0;}
                @Override public double vz() {return mAtomData.containsHead("vz") ? mAtomData.get(aIdx, "vz") : 0.0;}
                @Override public boolean hasVelocities() {return mHasVelocities;}
                
                @Override public ISettableAtom setX(double aX) {if (mKeyX==null) throw new RuntimeException("No X data in this Lammpstrj"); setX_(aIdx, aX); return this;}
                @Override public ISettableAtom setY(double aY) {if (mKeyY==null) throw new RuntimeException("No Y data in this Lammpstrj"); setY_(aIdx, aY); return this;}
                @Override public ISettableAtom setZ(double aZ) {if (mKeyZ==null) throw new RuntimeException("No Z data in this Lammpstrj"); setZ_(aIdx, aZ); return this;}
                @Override public ISettableAtom setID(int aID) {
                    if (!mAtomData.containsHead("id")) throw new UnsupportedOperationException("setID");
                    mAtomData.set(aIdx, "id", aID); return this;
                }
                @Override public ISettableAtom setType(int aType) {
                    if (!mAtomData.containsHead("type")) throw new UnsupportedOperationException("setType");
                    // 对于设置种类需要特殊处理，设置种类同时需要更新内部的原子种类计数
                    mAtomData.set(aIdx, "type", aType);
                    if (aType > atomTypeNum()) setAtomTypeNum(aType);
                    return this;
                }
                
                @Override public ISettableAtom setVx(double aVx) {
                    if (!mAtomData.containsHead("vx")) throw new UnsupportedOperationException("setVx");
                    mAtomData.set(aIdx, "vx", aVx); return this;
                }
                @Override public ISettableAtom setVy(double aVy) {
                    if (!mAtomData.containsHead("vy")) throw new UnsupportedOperationException("setVy");
                    mAtomData.set(aIdx, "vy", aVy); return this;
                }
                @Override public ISettableAtom setVz(double aVz) {
                    if (!mAtomData.containsHead("vz")) throw new UnsupportedOperationException("setVz");
                    mAtomData.set(aIdx, "vz", aVz); return this;
                }
            };
        }
        @Override public IXYZ box() {return mBox.shiftedBox();}
        @Override public int atomNum() {return mAtomData.rowNumber();}
        @Override public int atomTypeNum() {return mAtomTypeNum;}
        @Override public SubLammpstrj setAtomTypeNum(int aAtomTypeNum) {mAtomTypeNum = aAtomTypeNum; return this;}
        
        @Override public double volume() {return mBox.shiftedBox().prod();}
        
        @Override public SubLammpstrj copy() {return new SubLammpstrj(mTimeStep, Arrays.copyOf(mBoxBounds, mBoxBounds.length), mBox.copy(), mAtomData.copy());}
        // 由于 SubLammpstrj 不一定全都可以修改，因此不重写另外两个
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
    public static Lammpstrj fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>(aAtomDataList.size());
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
            return new SubLammpstrj(aTimeStep, BOX_BOUND, new Box(aAtomData.box()), aAtomData.hasVelocities()?aAtomData.dataAll():aAtomData.dataSTD());
        }
    }
    private static long getTimeStep(IAtomData aAtomData, long aDefault) {
        if (aAtomData instanceof Lammpstrj) return ((Lammpstrj)aAtomData).defaultFrame().mTimeStep;
        if (aAtomData instanceof SubLammpstrj) return ((SubLammpstrj)aAtomData).mTimeStep;
        return aDefault;
    }
    /** 对于 matlab 调用的兼容 */
    public static Lammpstrj fromAtomData_compat(Object[] aAtomDataArray) {
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
            final ITable aAtomData;
            
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
            String[] tAtomDataKeys = new String[tTokens.length-2];
            System.arraycopy(tTokens, 2, tAtomDataKeys, 0, tAtomDataKeys.length);
            boolean tIsAtomDataReadFull = true;
            aAtomData = Tables.zeros(tAtomNum, tAtomDataKeys);
            for (IVector tRow : aAtomData.rows()) {
                tLine = aReader.readLine();
                if (tLine == null) {tIsAtomDataReadFull = false; break;}
                tRow.fill(UT.Texts.str2data(tLine, tAtomDataKeys.length));
            }
            if (!tIsAtomDataReadFull) break;
            
            // 创建 SubLammpstrj 并附加到 rLammpstrj 中
            rLammpstrj.add(new SubLammpstrj(aTimeStep, aBoxBounds, aBox, aAtomData));
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
        try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {
            for (SubLammpstrj tSubLammpstrj : this) {
                tWriteln.writeln("ITEM: TIMESTEP");
                tWriteln.writeln(String.format("%d", tSubLammpstrj.timeStep()));
                tWriteln.writeln("ITEM: NUMBER OF ATOMS");
                tWriteln.writeln(String.format("%d", tSubLammpstrj.atomNum()));
                tWriteln.writeln(String.format("ITEM: BOX BOUNDS %s", String.join(" ", tSubLammpstrj.boxBounds())));
                tWriteln.writeln(String.format("%f %f", tSubLammpstrj.mBox.xlo(), tSubLammpstrj.mBox.xhi()));
                tWriteln.writeln(String.format("%f %f", tSubLammpstrj.mBox.ylo(), tSubLammpstrj.mBox.yhi()));
                tWriteln.writeln(String.format("%f %f", tSubLammpstrj.mBox.zlo(), tSubLammpstrj.mBox.zhi()));
                tWriteln.writeln(String.format("ITEM: ATOMS %s", String.join(" ", tSubLammpstrj.mAtomData.heads())));
                for (IVector subAtomData : tSubLammpstrj.mAtomData.rows()) {
                tWriteln.writeln(String.join(" ", AbstractCollections.map(subAtomData.iterable(), Object::toString)));
                }
            }
        }
    }
    
    
    
    /// 实用功能，这里依旧保留这种写法
    /**
     * 获取多帧原子参数的计算器，支持使用 MFPC 的简写来调用
     * @param aTimestep 每一个 step 之间的时间步长，实际每帧之间的时间步长会考虑 SubLammpstrj 的时间步；只考虑第一帧和第二帧之间的间距
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MFPC 的线程数目
     * @return 获取到的 MFPC
     */
    public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType, int aThreadNum) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
    public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep                           ) {return new MultiFrameParameterCalculator(this                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep,            int aThreadNum) {return new MultiFrameParameterCalculator(this                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
    public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType                ) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getMFPC          (double aTimestep                           ) {return new MultiFrameParameterCalculator(this                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getMFPC          (double aTimestep,            int aThreadNum) {return new MultiFrameParameterCalculator(this                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
    @VisibleForTesting public MultiFrameParameterCalculator getTypeMFPC      (double aTimestep, int aType                ) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getTypeMFPC      (double aTimestep, int aType, int aThreadNum) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
}
