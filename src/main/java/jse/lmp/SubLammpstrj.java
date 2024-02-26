package jse.lmp;

import jse.atom.*;
import jse.cache.ThreadLocalObjectCachePool;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.table.ITable;
import jse.math.table.Tables;
import jse.math.vector.IVector;
import jse.parallel.MPI;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import static jse.code.CS.*;
import static jse.lmp.Lammpstrj.*;

/** 每个帧的子 Lammpstrj */
public class SubLammpstrj extends AbstractSettableAtomData {
    private long mTimeStep;
    private final String[] mBoxBounds;
    private final ITable mAtomData;
    private int mAtomTypeNum;
    private Box mBox;
    
    String mKeyX = null, mKeyY = null, mKeyZ = null;
    private final boolean mHasVelocities;
    
    /** 提供直接转为表格的接口 */
    public ITable asTable() {return mAtomData;}
    
    SubLammpstrj(long aTimeStep, String[] aBoxBounds, Box aBox, ITable aAtomData) {
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
    
    public SubLammpstrj setTimeStep(long aTimeStep) {mTimeStep = aTimeStep; return this;}
    /** Groovy stuffs */
    @VisibleForTesting public long getTimeStep() {return mTimeStep;}
    
    /** 密度归一化, 返回自身来支持链式调用 */
    public SubLammpstrj setDenseNormalized() {
        if (mKeyX==null) throw new RuntimeException("No X data in this Lammpstrj");
        if (mKeyY==null) throw new RuntimeException("No Y data in this Lammpstrj");
        if (mKeyZ==null) throw new RuntimeException("No Z data in this Lammpstrj");
        
        XYZ oShiftedBox = XYZ.toXYZ(mBox.shiftedBox());
        double tScale = MathEX.Fast.cbrt(oShiftedBox.prod() / this.atomNumber());
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
    @Override public ISettableAtom atom(final int aIdx) {
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
                if (aType > atomTypeNumber()) setAtomTypeNumber(aType);
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
    @Override public int atomNumber() {return mAtomData.rowNumber();}
    @Override public int atomTypeNumber() {return mAtomTypeNum;}
    @Override public SubLammpstrj setAtomTypeNumber(int aAtomTypeNum) {mAtomTypeNum = aAtomTypeNum; return this;}
    
    @Override public double volume() {return mBox.shiftedBox().prod();}
    
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
            // 一般的情况，现在需要手动拷贝一下
            final int tAtomNum = aAtomData.atomNumber();
            ITable rAtomData;
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
            return new SubLammpstrj(aTimeStep, BOX_BOUND, new Box(aAtomData.box()), rAtomData);
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
        Box aBox;
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
        aBoxBounds = new String[] {tTokens[3], tTokens[4], tTokens[5]};
        tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
        double aXlo = Double.parseDouble(tTokens[0]); double aXhi = Double.parseDouble(tTokens[1]);
        tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
        double aYlo = Double.parseDouble(tTokens[0]); double aYhi = Double.parseDouble(tTokens[1]);
        tLine=aReader.readLine(); tTokens = UT.Text.splitBlank(tLine);
        double aZlo = Double.parseDouble(tTokens[0]); double aZhi = Double.parseDouble(tTokens[1]);
        // 这里暂不考虑斜方模拟盒
        aBox = new Box(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi);
        
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
        aWriteln.writeln(String.format("ITEM: BOX BOUNDS %s", String.join(" ", boxBounds())));
        aWriteln.writeln(String.format("%f %f", mBox.xlo(), mBox.xhi()));
        aWriteln.writeln(String.format("%f %f", mBox.ylo(), mBox.yhi()));
        aWriteln.writeln(String.format("%f %f", mBox.zlo(), mBox.zhi()));
        aWriteln.writeln(String.format("ITEM: ATOMS %s", String.join(" ", mAtomData.heads())));
        for (IVector subAtomData : mAtomData.rows()) {
        aWriteln.writeln(String.join(" ", AbstractCollections.map(subAtomData, Object::toString)));
        }
    }
    
    
    /// MPI stuffs
    /** [AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep] */
    private final static int LAMMPSTRJ_INFO_LEN = 8;
    /** 为了使用简单并且避免 double 转 long 造成的信息损耗，这里统一用 long[] 来传输信息 */
    private final static ThreadLocalObjectCachePool<long[]> LAMMPSTRJ_INFO_CACHE = ThreadLocalObjectCachePool.withInitial(() -> new long[LAMMPSTRJ_INFO_LEN]);
    /** 专门的方法用来收发 SubLammpstrj */
    public static void send(SubLammpstrj aSubLammpstrj, int aDest, MPI.Comm aComm) throws MPI.Error {
        // 暂不支持周期边界以外的类型的发送
        if (!aSubLammpstrj.mBoxBounds[0].equals("pp") || !aSubLammpstrj.mBoxBounds[1].equals("pp") || !aSubLammpstrj.mBoxBounds[2].equals("pp")) {
            throw new RuntimeException("send is temporarily support `pp pp pp` BoxBounds only");
        }
        // 先发送 SubLammpstrj 的必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
        long[] tLammpstrjInfo = LAMMPSTRJ_INFO_CACHE.getObject();
        try {
            tLammpstrjInfo[0] = UT.Serial.combineI(aSubLammpstrj.atomNumber(), aSubLammpstrj.mAtomData.columnNumber());
            tLammpstrjInfo[1] = Double.doubleToLongBits(aSubLammpstrj.mBox.xlo());
            tLammpstrjInfo[2] = Double.doubleToLongBits(aSubLammpstrj.mBox.xhi());
            tLammpstrjInfo[3] = Double.doubleToLongBits(aSubLammpstrj.mBox.ylo());
            tLammpstrjInfo[4] = Double.doubleToLongBits(aSubLammpstrj.mBox.yhi());
            tLammpstrjInfo[5] = Double.doubleToLongBits(aSubLammpstrj.mBox.zlo());
            tLammpstrjInfo[6] = Double.doubleToLongBits(aSubLammpstrj.mBox.zhi());
            tLammpstrjInfo[7] = aSubLammpstrj.mTimeStep;
            aComm.send(tLammpstrjInfo, LAMMPSTRJ_INFO_LEN, aDest, LAMMPSTRJ_INFO);
        } finally {
            // 发送后归还临时数据
            LAMMPSTRJ_INFO_CACHE.returnObject(tLammpstrjInfo);
        }
        // 必要信息发送完成后分别发送 atomDataKeys 和 atomData，这里按列发送，先统一发送 key 再统一发送数据
        for (String subDataKey : aSubLammpstrj.mAtomData.heads()) aComm.sendStr(subDataKey, aDest, DATA_KEY);
        for (IVector subData : aSubLammpstrj.mAtomData.asMatrix().cols()) aComm.send(subData, aDest, DATA);
    }
    public static SubLammpstrj recv(int aSource, MPI.Comm aComm) throws MPI.Error {
        // 同样先接收必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
        long[] tLammpstrjInfo = LAMMPSTRJ_INFO_CACHE.getObject();
        try {
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
            return new SubLammpstrj(tTimeStep, BOX_BOUND, new Box(
                Double.longBitsToDouble(tLammpstrjInfo[1]), Double.longBitsToDouble(tLammpstrjInfo[2]),
                Double.longBitsToDouble(tLammpstrjInfo[3]), Double.longBitsToDouble(tLammpstrjInfo[4]),
                Double.longBitsToDouble(tLammpstrjInfo[5]), Double.longBitsToDouble(tLammpstrjInfo[6])
            ), rAtomData);
        } finally {
            // 完事归还临时数据
            LAMMPSTRJ_INFO_CACHE.returnObject(tLammpstrjInfo);
        }
    }
    public static SubLammpstrj bcast(SubLammpstrj aSubLammpstrj, int aRoot, MPI.Comm aComm) throws MPI.Error {
        if (aComm.rank() == aRoot) {
            // 暂不支持周期边界以外的类型的发送
            if (!aSubLammpstrj.mBoxBounds[0].equals("pp") || !aSubLammpstrj.mBoxBounds[1].equals("pp") || !aSubLammpstrj.mBoxBounds[2].equals("pp")) {
                throw new RuntimeException("bcast is temporarily support `pp pp pp` BoxBounds only");
            }
            // 先发送 SubLammpstrj 的必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
            long[] tLammpstrjInfo = LAMMPSTRJ_INFO_CACHE.getObject();
            try {
                tLammpstrjInfo[0] = UT.Serial.combineI(aSubLammpstrj.atomNumber(), aSubLammpstrj.mAtomData.columnNumber());
                tLammpstrjInfo[1] = Double.doubleToLongBits(aSubLammpstrj.mBox.xlo());
                tLammpstrjInfo[2] = Double.doubleToLongBits(aSubLammpstrj.mBox.xhi());
                tLammpstrjInfo[3] = Double.doubleToLongBits(aSubLammpstrj.mBox.ylo());
                tLammpstrjInfo[4] = Double.doubleToLongBits(aSubLammpstrj.mBox.yhi());
                tLammpstrjInfo[5] = Double.doubleToLongBits(aSubLammpstrj.mBox.zlo());
                tLammpstrjInfo[6] = Double.doubleToLongBits(aSubLammpstrj.mBox.zhi());
                tLammpstrjInfo[7] = aSubLammpstrj.mTimeStep;
                aComm.bcast(tLammpstrjInfo, LAMMPSTRJ_INFO_LEN, aRoot);
            } finally {
                // 发送后归还临时数据
                LAMMPSTRJ_INFO_CACHE.returnObject(tLammpstrjInfo);
            }
            // 必要信息发送完成后分别发送 atomDataKeys 和 atomData，这里按列发送，先统一发送 key 再统一发送数据
            for (String subDataKey : aSubLammpstrj.mAtomData.heads()) aComm.bcastStr(subDataKey, aRoot);
            for (IVector subData : aSubLammpstrj.mAtomData.asMatrix().cols()) aComm.bcast(subData, aRoot);
            return aSubLammpstrj;
        } else {
            // 同样先接收必要信息，[AtomNum | AtomDataKeyNum, Box.xlo, Box.xhi, Box.ylo, Box.yhi, Box.zlo, Box.zhi, TimeStep]
            long[] tLammpstrjInfo = LAMMPSTRJ_INFO_CACHE.getObject();
            try {
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
                return new SubLammpstrj(tTimeStep, BOX_BOUND, new Box(
                    Double.longBitsToDouble(tLammpstrjInfo[1]), Double.longBitsToDouble(tLammpstrjInfo[2]),
                    Double.longBitsToDouble(tLammpstrjInfo[3]), Double.longBitsToDouble(tLammpstrjInfo[4]),
                    Double.longBitsToDouble(tLammpstrjInfo[5]), Double.longBitsToDouble(tLammpstrjInfo[6])
                ), rAtomData);
            } finally {
                // 完事归还临时数据
                LAMMPSTRJ_INFO_CACHE.returnObject(tLammpstrjInfo);
            }
        }
    }
}
