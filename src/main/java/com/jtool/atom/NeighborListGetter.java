package com.jtool.atom;

import com.jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;
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
        final XYZ mXYZ;
        final int mIDX;
        public XYZ_IDX(XYZ aXYZ, int aIDX) {mXYZ = aXYZ; mIDX = aIDX;}
        @Override public double x() {return mXYZ.mX;}
        @Override public double y() {return mXYZ.mY;}
        @Override public double z() {return mXYZ.mZ;}
    }
    
    
    private XYZ_IDX[] mAtomDataXYZ_IDX;
    private final XYZ mBox;
    private final int mAtomNum;
    private final double mMinBox;
    private final double mCellStep;
    
    private final TreeMap<Integer, LinkedCell<XYZ_IDX>> mLinkedCells = new TreeMap<>(); // 记录对应有效近邻半径的 LinkedCell，使用 Integer 只存储倍率（负值表示除法），避免 double 作为 key 的问题
    
    // 提供一个手动关闭的方法
    private volatile boolean mDead = false;
    public void shutdown() {shutdown_(); System.gc();}
    public void shutdown_() {mDead = true; mAtomDataXYZ_IDX = null; mLinkedCells.clear();}
    
    public double getCellStep() {return mCellStep;}
    
    // 使用读写锁来实现线程安全
    private final ReadWriteLock mRWL = new ReentrantReadWriteLock();
    private final Lock mRL = mRWL.readLock();
    private final Lock mWL = mRWL.writeLock();
    
    // NL 只支持已经经过平移的数据
    public NeighborListGetter(XYZ[] aAtomDataXYZ, XYZ aBox) {this(aAtomDataXYZ, aBox, 2.0);}
    public NeighborListGetter(XYZ[] aAtomDataXYZ, XYZ aBox, double aCellStep) {
        mAtomNum = aAtomDataXYZ.length;
        
        mAtomDataXYZ_IDX = new XYZ_IDX[mAtomNum];
        for (int i = 0; i < mAtomNum; ++i) mAtomDataXYZ_IDX[i] = new XYZ_IDX(aAtomDataXYZ[i], i);
        mBox = aBox;
        mMinBox = mBox.min();
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
            int aSizeX = Math.max((int)Math.floor(mBox.mX / tCellLength), tDiv); // 可以避免舍入误差的问题
            int aSizeY = Math.max((int)Math.floor(mBox.mY / tCellLength), tDiv);
            int aSizeZ = Math.max((int)Math.floor(mBox.mZ / tCellLength), tDiv);
            tLinkedCell = new LinkedCell<>(mAtomDataXYZ_IDX, mBox, aSizeX, aSizeY, aSizeZ);
            mLinkedCells.put(-tDiv, tLinkedCell);
        }
        // 再处理需要扩展的情况
        else {
            int tMul = MathEX.Code.ceilPower(tMinMulti, mCellStep);
            double tCellLength = mMinBox * tMul;
            int aSizeX = (int)Math.floor(mBox.mX / tCellLength);
            int aSizeY = (int)Math.floor(mBox.mY / tCellLength);
            int aSizeZ = (int)Math.floor(mBox.mZ / tCellLength);
            // 对于为 0 的则是需要扩展的，统计扩展数目
            int tMulX = 1, tMulY = 1, tMulZ = 1;
            if (aSizeX == 0) {aSizeX = 1; tMulX = (int)Math.ceil(tCellLength / mBox.mX);}
            if (aSizeY == 0) {aSizeY = 1; tMulY = (int)Math.ceil(tCellLength / mBox.mY);}
            if (aSizeZ == 0) {aSizeZ = 1; tMulZ = (int)Math.ceil(tCellLength / mBox.mZ);}
            int tExpendAtomNum = mAtomNum*tMulX*tMulY*tMulZ;
            if (tExpendAtomNum == mAtomNum) {
                tLinkedCell = new LinkedCell<>(mAtomDataXYZ_IDX, mBox, aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(tMul, tLinkedCell);
            } else {
                XYZ_IDX[] tExpendAtomDataXYZ_IDX = new XYZ_IDX[tExpendAtomNum];
                int tIdx = 0;
                for (int i = 0; i < tMulX; ++i) for (int j = 0; j < tMulY; ++j) for (int k = 0; k < tMulZ; ++k) for (int l = 0; l < mAtomNum; ++l) {
                    XYZ tXYZ = mAtomDataXYZ_IDX[l].mXYZ;
                    XYZ aXYZ = new XYZ(
                        i==0 ? tXYZ.mX : tXYZ.mX + mBox.mX*i,
                        j==0 ? tXYZ.mY : tXYZ.mY + mBox.mY*j,
                        k==0 ? tXYZ.mZ : tXYZ.mZ + mBox.mZ*k
                    );
                    tExpendAtomDataXYZ_IDX[tIdx] = new XYZ_IDX(aXYZ, mAtomDataXYZ_IDX[l].mIDX);
                    ++tIdx;
                }
                tLinkedCell = new LinkedCell<>(tExpendAtomDataXYZ_IDX, mBox.multiply(tMulX, tMulY, tMulZ), aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(tMul, tLinkedCell);
            }
        }
        mWL.unlock();
        
        // 最后返回近邻
        return tLinkedCell;
    }
    
    
    
    @FunctionalInterface public interface IXYZDo {void run(double aX, double aY, double aZ);}
    @FunctionalInterface public interface IDisDo {void run(double aDis);}
    
    /**
     * 现在统一改为 for-each 的形式，首先提供一个完全通用的方法遍历所有的近邻；
     * 注意输入的 aRMaxNeed 只保证在这个半径内所有近邻都会遍历到，不会保证这个半径外的原子不会被遍历
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMaxNeed 需要的近邻半径
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子
     */
    public void forEachNeighbor(final int aIDX, double aRMaxNeed, final boolean aHalf, final IXYZDo aXYZDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        getProperLinkedCell(aRMaxNeed).forEachNeighbor(mAtomDataXYZ_IDX[aIDX].mXYZ, (xyz_idx, link) -> {
            if (link.isMirror()) {
                // 如果是镜像的，则会保留相同的 idx 的情况
                if (aHalf) {
                    if (xyz_idx.mIDX <= aIDX) {
                        XYZ tDir = link.direction();
                        aXYZDo.run(xyz_idx.mXYZ.mX+tDir.mX, xyz_idx.mXYZ.mY+tDir.mY, xyz_idx.mXYZ.mZ+tDir.mZ);
                    }
                } else {
                    XYZ tDir = link.direction();
                    aXYZDo.run(xyz_idx.mXYZ.mX+tDir.mX, xyz_idx.mXYZ.mY+tDir.mY, xyz_idx.mXYZ.mZ+tDir.mZ);
                }
            } else {
                // 如果不是镜像的，则不会保留相同的 idx 的情况
                if (aHalf) {
                    if (xyz_idx.mIDX <  aIDX) aXYZDo.run(xyz_idx.mXYZ.mX, xyz_idx.mXYZ.mY, xyz_idx.mXYZ.mZ);
                } else {
                    if (xyz_idx.mIDX != aIDX) aXYZDo.run(xyz_idx.mXYZ.mX, xyz_idx.mXYZ.mY, xyz_idx.mXYZ.mZ);
                }
            }
        });
    }
    public void forEachNeighbor(XYZ aXYZ, double aRMaxNeed, final IXYZDo aXYZDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        getProperLinkedCell(aRMaxNeed).forEachNeighbor(aXYZ, (xyz_idx, link) -> {
            if (link.isMirror()) {
                XYZ tDir = link.direction();
                aXYZDo.run(xyz_idx.mXYZ.mX+tDir.mX, xyz_idx.mXYZ.mY+tDir.mY, xyz_idx.mXYZ.mZ+tDir.mZ);
            } else {
                aXYZDo.run(xyz_idx.mXYZ.mX, xyz_idx.mXYZ.mY, xyz_idx.mXYZ.mZ);
            }
        });
    }

    
    /**
     * 现在统一改为 for-each 的形式，再提供专门的遍历近邻距离的方法；
     * 注意这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 最大的近邻半径
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子
     */
    public void forEachNeighborDis(final int aIDX, final double aRMax, final boolean aHalf, final IDisDo aDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        final XYZ aXYZ = mAtomDataXYZ_IDX[aIDX].mXYZ;
        
        getProperLinkedCell(aRMax).forEachNeighbor(aXYZ, (xyz_idx, link) -> {
            if (link.isMirror()) {
                // 如果是镜像的，则会保留相同的 idx 的情况
                if (aHalf) {
                    if (xyz_idx.mIDX <= aIDX) {
                        double tDis = link.distance(aXYZ, xyz_idx.mXYZ);
                        if (tDis < aRMax) aDisDo.run(tDis);
                    }
                } else {
                    double tDis = link.distance(aXYZ, xyz_idx.mXYZ);
                    if (tDis < aRMax) aDisDo.run(tDis);
                }
            } else {
                // 如果不是镜像的，则不会保留相同的 idx 的情况
                if (aHalf) {
                    if (xyz_idx.mIDX < aIDX) {
                        double tDis = aXYZ.distance(xyz_idx.mXYZ);
                        if (tDis < aRMax) aDisDo.run(tDis);
                    }
                } else {
                    if (xyz_idx.mIDX != aIDX) {
                        double tDis = aXYZ.distance(xyz_idx.mXYZ);
                        if (tDis < aRMax) aDisDo.run(tDis);
                    }
                }
            }
        });
    }
    public void forEachNeighborDis(final XYZ aXYZ, final double aRMax, final IDisDo aDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        getProperLinkedCell(aRMax).forEachNeighbor(aXYZ, (xyz_idx, link) -> {
            double tDis = link.isMirror() ? link.distance(aXYZ, xyz_idx.mXYZ) : aXYZ.distance(xyz_idx.mXYZ);
            if (tDis < aRMax) aDisDo.run(tDis);
        });
    }
    
}
