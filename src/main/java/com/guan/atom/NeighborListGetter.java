package com.guan.atom;

import com.guan.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author liqa
 * <p> 获取 atomDataXYZ 的近邻列表 </p>
 * <p> 暂存 cell 来在重复调用时进行加速，高度优化 </p>
 * <p> 这里考虑已经经过平移的数据，可以避免一些不必要的交叉引用以及重复平移的问题 </p>
 * <p> 认为所有边界都是周期边界条件 </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 * <p> 此类线程安全，包括多个线程同时访问同一个实例 </p>
 */
public class NeighborListGetter {
    // 用于 LinkedCell 使用
    private static class XYZ_IDX implements IHasXYZ {
        final double[] mXYZ;
        final int mIDX;
        public XYZ_IDX(double[] aXYZ, int aIDX) {mXYZ = aXYZ; mIDX = aIDX;}
        @Override public double[] xyz() {return mXYZ;}
    }
    
    
    private XYZ_IDX[] mAtomDataXYZ_IDX;
    private final double[] mBox;
    private final int mAtomNum;
    private final double mMinBox;
    private final double mCellStep;
    
    private final TreeMap<Integer, LinkedCell<XYZ_IDX>> mLinkedCells = new TreeMap<>(); // 记录对应有效近邻半径的 LinkedCell，使用 Integer 只存储倍率（负值表示除法），避免 double 作为 key 的问题
    
    // 提供一个手动关闭的方法
    private volatile boolean mDead = false;
    public void shutdown() {shutdown_(); System.gc();}
    public void shutdown_() {mDead = true;mAtomDataXYZ_IDX = null; mLinkedCells.clear();}
    
    public double getCellStep() {return mCellStep;}
    
    // 使用读写锁来实现线程安全
    private final ReadWriteLock mRWL = new ReentrantReadWriteLock();
    private final Lock mRL = mRWL.readLock();
    private final Lock mWL = mRWL.writeLock();
    
    // NL 只支持已经经过平移的数据
    public NeighborListGetter(double[][] aAtomDataXYZ, double[] aBox) {this(aAtomDataXYZ, aBox, 2.0);}
    public NeighborListGetter(double[][] aAtomDataXYZ, double[] aBox, double aCellStep) {
        mAtomNum = aAtomDataXYZ.length;
        
        mAtomDataXYZ_IDX = new XYZ_IDX[mAtomNum];
        for (int i = 0; i < mAtomNum; ++i) mAtomDataXYZ_IDX[i] = new XYZ_IDX(aAtomDataXYZ[i], i);
        mBox = aBox;
        mMinBox = MathEX.Vec.min(mBox);
        mCellStep = Math.max(aCellStep, 1.1);
    }
    
    
    boolean isCeilEntryValid(@Nullable Map.Entry<Integer, LinkedCell<XYZ_IDX>> aCeilEntry, int aMinMulti) {
        if (aCeilEntry == null) return false;
        return (aMinMulti > 0 && aCeilEntry.getKey() < Math.ceil(aMinMulti*mCellStep)) || (aMinMulti < 0 && Math.floor(aCeilEntry.getKey()*mCellStep) < aMinMulti);
    }
    /**
     * 获取覆盖 aRMax 的 LinkedCell，会自动选择和创建合适的 LinkedCell
     * <p> 线程安全 </p>
     * @author liqa
     * @param aRMax 这个 LinkedCell 需要考虑的最大半径
     * @return 合适的 LinkedCell
     */
    LinkedCell<XYZ_IDX> getProperLinkedCell(double aRMax) {
        // 获取需要的最小的 cell 长度倍率
        int tMinMulti = aRMax>mMinBox ? (int)Math.ceil(aRMax/mMinBox) : -(int)Math.floor(mMinBox/aRMax);
        // 尝试获取 LinkedCell
        mRL.lock();
        Map.Entry<Integer, LinkedCell<XYZ_IDX>>
        tCeilEntry = mLinkedCells.ceilingEntry(tMinMulti);
        mRL.unlock();
        // 检测是否合适，如果合适则直接返回
        if (isCeilEntryValid(tCeilEntry, tMinMulti)) return tCeilEntry.getValue();
        // 否则则需要添加 LinkedCell，整个过程加上写入锁
        mWL.lock();
        // 获取到写入锁后可能之前已经被修改，此时需要再次重新检测是否合适（并行特有的两次检测）
        tCeilEntry = mLinkedCells.ceilingEntry(tMinMulti);
        if (isCeilEntryValid(tCeilEntry, tMinMulti)) {mWL.unlock(); return tCeilEntry.getValue();} // 注意跳出前记得释放锁
        
        // 没有则需要开始手动添加
        LinkedCell<XYZ_IDX> tLinkedCell;
        // 计算对应 LinkedCell 的参数，先处理不需要扩展的情况
        if (tMinMulti < 0) {
            int tDiv = MathEX.Code.floorPower(-tMinMulti, mCellStep);
            double tCellLength = mMinBox / (double)tDiv;
            int aSizeX = Math.max((int)Math.floor(mBox[0] / tCellLength), tDiv); // 可以避免舍入误差的问题
            int aSizeY = Math.max((int)Math.floor(mBox[1] / tCellLength), tDiv);
            int aSizeZ = Math.max((int)Math.floor(mBox[2] / tCellLength), tDiv);
            tLinkedCell = new LinkedCell<>(mAtomDataXYZ_IDX, mBox, aSizeX, aSizeY, aSizeZ);
            mLinkedCells.put(-tDiv, tLinkedCell);
        }
        // 再处理需要扩展的情况
        else {
            int tMul = MathEX.Code.ceilPower(tMinMulti, mCellStep);
            double tCellLength = mMinBox * tMul;
            int aSizeX = (int)Math.floor(mBox[0] / tCellLength);
            int aSizeY = (int)Math.floor(mBox[1] / tCellLength);
            int aSizeZ = (int)Math.floor(mBox[2] / tCellLength);
            // 对于为 0 的则是需要扩展的，统计扩展数目
            int tMulX = 1, tMulY = 1, tMulZ = 1;
            if (aSizeX == 0) {aSizeX = 1; tMulX = (int)Math.ceil(tCellLength / mBox[0]);}
            if (aSizeY == 0) {aSizeY = 1; tMulY = (int)Math.ceil(tCellLength / mBox[1]);}
            if (aSizeZ == 0) {aSizeZ = 1; tMulZ = (int)Math.ceil(tCellLength / mBox[2]);}
            int tExpendAtomNum = mAtomNum*tMulX*tMulY*tMulZ;
            if (tExpendAtomNum == mAtomNum) {
                tLinkedCell = new LinkedCell<>(mAtomDataXYZ_IDX, mBox, aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(tMul, tLinkedCell);
            } else {
                XYZ_IDX[] tExpendAtomDataXYZ_IDX = new XYZ_IDX[tExpendAtomNum];
                int tIdx = 0;
                for (int i = 0; i < tMulX; ++i) for (int j = 0; j < tMulY; ++j) for (int k = 0; k < tMulZ; ++k) for (int l = 0; l < mAtomNum; ++l) {
                    double[] aXYZ = new double[3];
                    double[] tXYZ = mAtomDataXYZ_IDX[l].mXYZ;
                    aXYZ[0] = i==0 ? tXYZ[0] : tXYZ[0] + mBox[0]*i;
                    aXYZ[1] = j==0 ? tXYZ[1] : tXYZ[1] + mBox[1]*j;
                    aXYZ[2] = k==0 ? tXYZ[2] : tXYZ[2] + mBox[2]*k;
                    tExpendAtomDataXYZ_IDX[tIdx] = new XYZ_IDX(aXYZ, mAtomDataXYZ_IDX[l].mIDX);
                    ++tIdx;
                }
                tLinkedCell = new LinkedCell<>(tExpendAtomDataXYZ_IDX, new double[] {mBox[0] * tMulX, mBox[1] * tMulY, mBox[2] * tMulZ}, aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(tMul, tLinkedCell);
            }
        }
        mWL.unlock();
        
        // 最后返回近邻
        return tLinkedCell;
    }
    
    /**
     * 获取位于 aIDX 的 XYZ 在 aRMax 半径范围内的近邻粒子列表
     * <p> 指定 aHalf 则只会考虑一半的粒子（index > aIDX） </p>
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 近邻半径
     * @return Iterable 的容器，支持 for-each 遍历并且避免重复值拷贝
     */
    public Iterable<double[]> get_IDX(final int aIDX, final double aRMax                     ) {return get_IDX(aIDX, aRMax, false);}
    public Iterable<double[]> get_IDX(final int aIDX, final double aRMax, final boolean aHalf) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 获取 XYZ
        final double[] aXYZ = mAtomDataXYZ_IDX[aIDX].mXYZ;
        // 返回满足要求的容器
        return () -> getProperLinkedCell(aRMax).new NeighborListItr(aXYZ, aRMax) {
            // 重写这部分实现排除相同 id 或者只考虑一半的 id
            @Override public boolean isValid(XYZ_IDX aNextAtom) {
                if (aHalf) {return aNextAtom.mIDX >  aIDX;}
                else       {return aNextAtom.mIDX != aIDX;}
            }
        };
    }
    
    /**
     * 获取位于 aXYZ 在 aRMax 半径范围内的近邻粒子列表
     * <p> 由于没有 ID 信息，不会排除相同粒子 </p>
     * @author liqa
     * @param aXYZ 中心的坐标
     * @param aRMax 近邻半径
     * @return Iterable 的容器，支持 for-each 遍历并且避免重复值拷贝
     */
    public Iterable<double[]> get_XYZ(final double[] aXYZ, final double aRMax) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        // 返回满足要求的容器，这里直接返回即可
        return getProperLinkedCell(aRMax).getNeighborList_(aXYZ, aRMax);
    }
    
    
    /**
     * 获取位于 aIDX 的 XYZ 在曼哈顿距离为 aRMax 范围内的近邻粒子列表
     * <p> 指定 aHalf 则只会考虑一半的粒子（index > aIDX） </p>
     * <p> MHT: ManHaTtan distance </p>
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 近邻半径
     * @return Iterable 的容器，支持 for-each 遍历并且避免重复值拷贝
     */
    public Iterable<double[]> getMHT_IDX(final int aIDX, final double aRMax                     ) {return getMHT_IDX(aIDX, aRMax, false);}
    public Iterable<double[]> getMHT_IDX(final int aIDX, final double aRMax, final boolean aHalf) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 获取 XYZ
        final double[] aXYZ = mAtomDataXYZ_IDX[aIDX].mXYZ;
        // 返回满足要求的容器
        return () -> getProperLinkedCell(aRMax).new Itr<double[]>(aXYZ) {
            // 重写这部分实现排除相同 id 或者只考虑一半的 id
            @Override public boolean isValid(XYZ_IDX aNextAtom) {
                if (aHalf) {return aNextAtom.mIDX >  aIDX;}
                else       {return aNextAtom.mIDX != aIDX;}
            }
            @Override public double[] getNext(XYZ_IDX aNextAtom, LinkedCell.Link<XYZ_IDX> aLink) {
                double[] tXYZ = aNextAtom.xyz();
                double[] tMirrorXYZ = new double[3];
                tMirrorXYZ[0] = (aLink.mDirection == null || aLink.mDirection[0] == 0) ? tXYZ[0] : tXYZ[0] + aLink.mDirection[0];
                tMirrorXYZ[1] = (aLink.mDirection == null || aLink.mDirection[1] == 0) ? tXYZ[1] : tXYZ[1] + aLink.mDirection[1];
                tMirrorXYZ[2] = (aLink.mDirection == null || aLink.mDirection[2] == 0) ? tXYZ[2] : tXYZ[2] + aLink.mDirection[2];
                double tMHT = Math.abs(tMirrorXYZ[0] - aXYZ[0]) + Math.abs(tMirrorXYZ[1] - aXYZ[1]) + Math.abs(tMirrorXYZ[2] - aXYZ[2]);
                return tMHT > aRMax ? null : tMirrorXYZ;
            }
        };
    }
    
    /**
     * 获取位于 aXYZ 在曼哈顿距离为 aRMax 范围内的近邻粒子列表
     * <p> 由于没有 ID 信息，不会排除相同粒子 </p>
     * <p> MHT: ManHaTtan distance </p>
     * @author liqa
     * @param aXYZ 中心的坐标
     * @param aRMax 近邻半径
     * @return Iterable 的容器，支持 for-each 遍历并且避免重复值拷贝
     */
    public Iterable<double[]> getMHT_XYZ(final double[] aXYZ, final double aRMax) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        // 返回满足要求的容器
        return () -> getProperLinkedCell(aRMax).new Itr<double[]>(aXYZ) {
            @Override public double[] getNext(XYZ_IDX aNextAtom, LinkedCell.Link<XYZ_IDX> aLink) {
                double[] tXYZ = aNextAtom.xyz();
                double[] tMirrorXYZ = new double[3];
                tMirrorXYZ[0] = (aLink.mDirection == null || aLink.mDirection[0] == 0) ? tXYZ[0] : tXYZ[0] + aLink.mDirection[0];
                tMirrorXYZ[1] = (aLink.mDirection == null || aLink.mDirection[1] == 0) ? tXYZ[1] : tXYZ[1] + aLink.mDirection[1];
                tMirrorXYZ[2] = (aLink.mDirection == null || aLink.mDirection[2] == 0) ? tXYZ[2] : tXYZ[2] + aLink.mDirection[2];
                double tMHT = Math.abs(tMirrorXYZ[0] - aXYZ[0]) + Math.abs(tMirrorXYZ[1] - aXYZ[1]) + Math.abs(tMirrorXYZ[2] - aXYZ[2]);
                return tMHT > aRMax ? null : tMirrorXYZ;
            }
        };
    }
    
}
