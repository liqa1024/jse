package com.jtool.atom;

import com.jtool.math.MathEX;
import com.jtool.parallel.IShutdownable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.jtool.code.UT.Code.toXYZ;

/**
 * @author liqa
 * <p> 获取 atomDataXYZ 的近邻列表 </p>
 * <p> 暂存 cell 来在重复调用时进行加速，高度优化 </p>
 * <p> 这里考虑已经经过平移的数据，可以避免一些不必要的交叉引用以及重复平移的问题 </p>
 * <p> 认为所有边界都是周期边界条件 </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 * <p> 此类线程安全，包括多个线程同时访问同一个实例 </p>
 */
public class NeighborListGetter implements IShutdownable {
    final static double DEFAULT_CELL_STEP = 1.26; // 1.26*1.26*1.26 = 2.00
    
    // 用于 LinkedCell 使用
    private static class XYZ_IDX implements IXYZ {
        final int mIDX;
        final XYZ mXYZ;
        public XYZ_IDX(XYZ aXYZ, int aIDX) {mXYZ = aXYZ; mIDX = aIDX;}
        @Override public double x() {return mXYZ.mX;}
        @Override public double y() {return mXYZ.mY;}
        @Override public double z() {return mXYZ.mZ;}
    }
    
    
    private final XYZ[] mAtomDataXYZ;
    private final XYZ mBox;
    private final int mAtomNum;
    private final double mMinBox;
    private final double mCellStep;
    
    private final TreeMap<Integer, LinkedCell<XYZ_IDX>> mLinkedCells = new TreeMap<>(); // 记录对应有效近邻半径的 LinkedCell，使用 Integer 只存储倍率（负值表示除法），避免 double 作为 key 的问题
    
    // 提供一个手动关闭的方法
    private volatile boolean mDead = false;
    @Override public void shutdown() {mDead = true; mLinkedCells.clear();}
    
    public double getCellStep() {return mCellStep;}
    
    // 使用读写锁来实现线程安全
    private final ReadWriteLock mRWL = new ReentrantReadWriteLock();
    private final Lock mRL = mRWL.readLock();
    private final Lock mWL = mRWL.writeLock();
    
    // NL 只支持已经经过平移的数据
    public NeighborListGetter(XYZ[] aAtomDataXYZ, IXYZ aBox) {this(aAtomDataXYZ, aBox, DEFAULT_CELL_STEP);}
    public NeighborListGetter(XYZ[] aAtomDataXYZ, IXYZ aBox, double aCellStep) {
        mAtomNum = aAtomDataXYZ.length;
        mAtomDataXYZ = aAtomDataXYZ;
        mBox = toXYZ(aBox); // 仅用于计算，直接转为 XYZ 即可
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
            tLinkedCell = new LinkedCell<>(toXYZ_IDX(mAtomDataXYZ), mBox, aSizeX, aSizeY, aSizeZ);
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
            int aMulX = 1, aMulY = 1, aMulZ = 1;
            if (aSizeX == 0) {aSizeX = 1; aMulX = (int)Math.ceil(tCellLength / mBox.mX);}
            if (aSizeY == 0) {aSizeY = 1; aMulY = (int)Math.ceil(tCellLength / mBox.mY);}
            if (aSizeZ == 0) {aSizeZ = 1; aMulZ = (int)Math.ceil(tCellLength / mBox.mZ);}
            int tExpendAtomNum = mAtomNum*aMulX*aMulY*aMulZ;
            if (tExpendAtomNum == mAtomNum) {
                tLinkedCell = new LinkedCell<>(toXYZ_IDX(mAtomDataXYZ), mBox, aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(tMul, tLinkedCell);
            } else {
                tLinkedCell = new LinkedCell<>(toXYZ_IDX(mAtomDataXYZ, mBox, aMulX, aMulY, aMulZ), mBox.multiply(aMulX, aMulY, aMulZ), aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(tMul, tLinkedCell);
            }
        }
        mWL.unlock();
        
        // 最后返回近邻
        return tLinkedCell;
    }
    
    /** 内部实用方法 */
    private static Iterable<XYZ_IDX> toXYZ_IDX(final XYZ[] aAtomDataXYZ) {
        return () -> new Iterator<XYZ_IDX>() {
            private final int mSize = aAtomDataXYZ.length;
            int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public XYZ_IDX next() {
                if (hasNext()) {
                    XYZ_IDX tNext = new XYZ_IDX(aAtomDataXYZ[mIdx], mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    private static Iterable<XYZ_IDX> toXYZ_IDX(final XYZ[] aAtomDataXYZ, final XYZ aBox, final int aMulX, final int aMulY, final int aMulZ) {
        return () -> new Iterator<XYZ_IDX>() {
            private final int mSize = aAtomDataXYZ.length;
            int mIdx = 0;
            int i = 0, j = 0, k = 0;
            @Override public boolean hasNext() {return k < aMulZ;}
            @Override public XYZ_IDX next() {
                if (hasNext()) {
                    final XYZ tXYZ = aAtomDataXYZ[mIdx];
                    XYZ aXYZ = new XYZ(
                        i==0 ? tXYZ.mX : tXYZ.mX + aBox.mX*i,
                        j==0 ? tXYZ.mY : tXYZ.mY + aBox.mY*j,
                        k==0 ? tXYZ.mZ : tXYZ.mZ + aBox.mZ*k
                    );
                    XYZ_IDX tNext = new XYZ_IDX(aXYZ, mIdx);
                    ++mIdx;
                    if (mIdx == mSize) {
                        mIdx = 0;
                        ++i;
                        if (i == aMulX) {
                            i = 0;
                            ++j;
                            if (j == aMulY) {
                                j = 0;
                                ++k;
                            }
                        }
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    
    
    @FunctionalInterface public interface IXYZIdxDisDo {void run(double aX, double aY, double aZ, int aIdx, double aDis);}
    
    
    /**
     * 现在统一改为 for-each 的形式，提供两个通用的方法遍历所有的近邻；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 最大的近邻半径
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据
     */
     void forEachNeighbor_(final int aIDX, final double aRMax, final boolean aHalf, final boolean aMHT, final IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        final XYZ cXYZ = mAtomDataXYZ[aIDX];
        getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (xyz_idx, link) -> {
            if (link.isMirror()) {
                // 如果是镜像的，则会保留相同的 idx 的情况
                if (aHalf) {
                    if (xyz_idx.mIDX <= aIDX) {
                        XYZ tDir = link.direction();
                        double tX = xyz_idx.mXYZ.mX + tDir.mX;
                        double tY = xyz_idx.mXYZ.mY + tDir.mY;
                        double tZ = xyz_idx.mXYZ.mZ + tDir.mZ;
                        double tDis = aMHT ? cXYZ.distanceMHT(tX, tY, tZ) : cXYZ.distance(tX, tY, tZ);
                        if (tDis < aRMax) aXYZIdxDisDo.run(tX, tY, tZ, xyz_idx.mIDX, tDis);
                    }
                } else {
                    XYZ tDir = link.direction();
                    double tX = xyz_idx.mXYZ.mX + tDir.mX;
                    double tY = xyz_idx.mXYZ.mY + tDir.mY;
                    double tZ = xyz_idx.mXYZ.mZ + tDir.mZ;
                    double tDis = aMHT ? cXYZ.distanceMHT(tX, tY, tZ) : cXYZ.distance(tX, tY, tZ);
                    if (tDis < aRMax) aXYZIdxDisDo.run(tX, tY, tZ, xyz_idx.mIDX, tDis);
                }
            } else {
                // 如果不是镜像的，则不会保留相同的 idx 的情况
                if (aHalf) {
                    if (xyz_idx.mIDX <  aIDX) {
                        double tX = xyz_idx.mXYZ.mX;
                        double tY = xyz_idx.mXYZ.mY;
                        double tZ = xyz_idx.mXYZ.mZ;
                        double tDis = aMHT ? cXYZ.distanceMHT(tX, tY, tZ) : cXYZ.distance(tX, tY, tZ);
                        if (tDis < aRMax) aXYZIdxDisDo.run(tX, tY, tZ, xyz_idx.mIDX, tDis);
                    }
                } else {
                    if (xyz_idx.mIDX != aIDX) {
                        double tX = xyz_idx.mXYZ.mX;
                        double tY = xyz_idx.mXYZ.mY;
                        double tZ = xyz_idx.mXYZ.mZ;
                        double tDis = aMHT ? cXYZ.distanceMHT(tX, tY, tZ) : cXYZ.distance(tX, tY, tZ);
                        if (tDis < aRMax) aXYZIdxDisDo.run(tX, tY, tZ, xyz_idx.mIDX, tDis);
                    }
                }
            }
        });
    }
    
    /**
     * 现在统一改为 for-each 的形式，提供两个通用的方法遍历所有的近邻；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子
     * @author liqa
     * @param aXYZ 中心粒子的位置
     * @param aRMax 最大的近邻半径
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据
     */
    void forEachNeighbor_(IXYZ aXYZ, final double aRMax, final boolean aMHT, final IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        final XYZ cXYZ = toXYZ(aXYZ);
        getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (xyz_idx, link) -> {
            if (link.isMirror()) {
                XYZ tDir = link.direction();
                double tX = xyz_idx.mXYZ.mX + tDir.mX;
                double tY = xyz_idx.mXYZ.mY + tDir.mY;
                double tZ = xyz_idx.mXYZ.mZ + tDir.mZ;
                double tDis = aMHT ? cXYZ.distanceMHT(tX, tY, tZ) : cXYZ.distance(tX, tY, tZ);
                if (tDis < aRMax) aXYZIdxDisDo.run(tX, tY, tZ, xyz_idx.mIDX, tDis);
            } else {
                double tX = xyz_idx.mXYZ.mX;
                double tY = xyz_idx.mXYZ.mY;
                double tZ = xyz_idx.mXYZ.mZ;
                double tDis = aMHT ? cXYZ.distanceMHT(tX, tY, tZ) : cXYZ.distance(tX, tY, tZ);
                if (tDis < aRMax) aXYZIdxDisDo.run(tX, tY, tZ, xyz_idx.mIDX, tDis);
            }
        });
    }
    
    /** 使用这个统一的类来管理，可以限制最大元素数目，并专门处理距离完全相同的情况不会抹去 */
    private static class NearestNeighborList {
        private static class XYZIdxDis {
            final XYZ_IDX mXYZ_IDX;
            final double mDis;
            XYZIdxDis(XYZ_IDX aXYZ_IDX, double aDis) {mXYZ_IDX = aXYZ_IDX; mDis = aDis;}
            XYZIdxDis(double aDis, XYZ_IDX aXYZ_IDX) {mXYZ_IDX = aXYZ_IDX; mDis = aDis;}
        }
        /** 直接使用 LinkedList 存储来避免距离完全相同的情况 */
        private final LinkedList<XYZIdxDis> mNNList = new LinkedList<>();
        private final int mNnn;
        NearestNeighborList(int aNnn) {mNnn = aNnn;}
        
        void put(double aDis, XYZ_IDX aXYZ_IDX) {
            // 获取迭代器
            ListIterator<XYZIdxDis> li = mNNList.listIterator();
            // 跳转到距离大于或等于 aDis 之前
            while (li.hasNext()) {
                double tDis = li.next().mDis;
                if (tDis >= aDis) {
                    li.previous(); // 回到这个位置之前，在前面插入
                    break;
                }
            }
            // 然后直接进行添加即可
            li.add(new XYZIdxDis(aDis, aXYZ_IDX));
            // 如果容量超过限制，则移除最后的元素
            if (mNNList.size() > mNnn) mNNList.removeLast();
        }
        
        /** 直接使用 for-each 的形式来遍历，并且全部交给这里来实现避免多重转发 */
        void forEachNeighbor(int aIDX, boolean aHalf, IXYZIdxDisDo aXYZIdxDisDo) {
            for (XYZIdxDis tXYZIdxDis : mNNList) {
                if (aHalf) {
                    int tIDX = tXYZIdxDis.mXYZ_IDX.mIDX;
                    if (tIDX <= aIDX) {
                        XYZ tXYZ = tXYZIdxDis.mXYZ_IDX.mXYZ;
                        aXYZIdxDisDo.run(tXYZ.mX, tXYZ.mY, tXYZ.mZ, tIDX, tXYZIdxDis.mDis);
                    }
                } else {
                    XYZ tXYZ = tXYZIdxDis.mXYZ_IDX.mXYZ;
                    aXYZIdxDisDo.run(tXYZ.mX, tXYZ.mY, tXYZ.mZ, tXYZIdxDis.mXYZ_IDX.mIDX, tXYZIdxDis.mDis);
                }
            }
        }
        void forEachNeighbor(IXYZIdxDisDo aXYZIdxDisDo) {
            for (XYZIdxDis tXYZIdxDis : mNNList) {
                XYZ tXYZ = tXYZIdxDis.mXYZ_IDX.mXYZ;
                aXYZIdxDisDo.run(tXYZ.mX, tXYZ.mY, tXYZ.mZ, tXYZIdxDis.mXYZ_IDX.mIDX, tXYZIdxDis.mDis);
            }
        }
    }
    
    /**
     * 现在统一改为 for-each 的形式，再提供两个通用方法来限制最大近邻数目；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子；
     * 这里输入的 aNnn 会选取最多这个数目的最近的原子（如果开启了 aHalf 则实际的结果会小于此值，顺序为先限制 aNnn 再取 aHalf）
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 最大的近邻半径
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子（当设置了最大近邻后建议关闭，否则会爆出警告）
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据
     */
    void forEachNeighbor_(final int aIDX, final double aRMax, int aNnn, boolean aHalf, final boolean aMHT, IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 特殊输入处理，直接回到没有限制的情况
        if (aNnn <= 0) {
            forEachNeighbor_(aIDX, aRMax, aHalf, aMHT, aXYZIdxDisDo);
            return;
        }
        // 如果有限制 aNnn 则 aHalf 会有意外的结果，因此会警告建议关闭
        if (aHalf) System.err.println("WARNING: Half will cause Unexpected Results when Nnn>0, although it remains open here to avoid excessive deviation in the results");
        
        // 先遍历所有经历统计出最近的列表
        final NearestNeighborList rNN = new NearestNeighborList(aNnn);
        final XYZ cXYZ = mAtomDataXYZ[aIDX];
        getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (xyz_idx, link) -> {
            if (link.isMirror()) {
                // 如果是镜像的，则会保留相同的 idx 的情况
                XYZ tXYZ = xyz_idx.mXYZ.plus(link.direction());
                double tDis = aMHT ? cXYZ.distanceMHT(tXYZ) : cXYZ.distance(tXYZ);
                if (tDis < aRMax) rNN.put(tDis, new XYZ_IDX(tXYZ, xyz_idx.mIDX));
            } else {
                // 如果不是镜像的，则不会保留相同的 idx 的情况
                if (xyz_idx.mIDX != aIDX) {
                    double tDis = aMHT ? cXYZ.distanceMHT(xyz_idx.mXYZ) : cXYZ.distance(xyz_idx.mXYZ);
                    if (tDis < aRMax) rNN.put(tDis, xyz_idx);
                }
            }
            
        });
        // 然后直接遍历得到的近邻列表，由于上面已经处理好了相同 idx 的情况，这里可以直接保留相同 idx 即可
        rNN.forEachNeighbor(aIDX, aHalf, aXYZIdxDisDo);
    }
    
    /**
     * 现在统一改为 for-each 的形式，再提供两个通用方法来限制最大近邻数目；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子；
     * 这里输入的 aNnn 会选取最多这个数目的最近的原子（如果开启了 aHalf 则实际的结果会小于此值，顺序为先限制 aNnn 再取 aHalf）
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * @author liqa
     * @param aXYZ 中心粒子的位置
     * @param aRMax 最大的近邻半径
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据
     */
    void forEachNeighbor_(IXYZ aXYZ, final double aRMax, int aNnn, final boolean aMHT, IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 特殊输入处理，直接回到没有限制的情况
        if (aNnn <= 0) {
            forEachNeighbor_(aXYZ, aRMax, aMHT, aXYZIdxDisDo);
            return;
        }
        
        // 先遍历所有经历统计出最近的列表
        final NearestNeighborList rNN = new NearestNeighborList(aNnn);
        final XYZ cXYZ = toXYZ(aXYZ);
        getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (xyz_idx, link) -> {
            if (link.isMirror()) {
                XYZ tXYZ = xyz_idx.mXYZ.plus(link.direction());
                double tDis = aMHT ? cXYZ.distanceMHT(tXYZ) : cXYZ.distance(tXYZ);
                if (tDis < aRMax) rNN.put(tDis, new XYZ_IDX(tXYZ, xyz_idx.mIDX));
            } else {
                double tDis = aMHT ? cXYZ.distanceMHT(xyz_idx.mXYZ) : cXYZ.distance(xyz_idx.mXYZ);
                if (tDis < aRMax) rNN.put(tDis, xyz_idx);
            }
        });
        // 然后直接遍历得到的近邻列表
        rNN.forEachNeighbor(aXYZIdxDisDo);
    }
    
    
    /**
     * 现在统一改为 for-each 的形式，一般的使用欧几里得距离作为判据的方法；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 最大的近邻半径
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子（默认为 false）
     */
    public void forEachNeighbor(int  aIDX, double aRMax, boolean aHalf, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aIDX, aRMax, aHalf, false, aXYZIdxDisDo);}
    public void forEachNeighbor(int  aIDX, double aRMax, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor(aIDX, aRMax, false, aXYZIdxDisDo);}
    public void forEachNeighbor(IXYZ aXYZ, double aRMax, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aXYZ, aRMax, false, aXYZIdxDisDo);}
    /**
     * 增加的限制最大近邻数目的遍历方法
     * @author liqa
     */
    public void forEachNeighbor(int  aIDX, double aRMax, int aNnn, boolean aHalf, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, false, aXYZIdxDisDo);}
    public void forEachNeighbor(int  aIDX, double aRMax, int aNnn, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor(aIDX, aRMax, aNnn, false, aXYZIdxDisDo);}
    public void forEachNeighbor(IXYZ aXYZ, double aRMax, int aNnn, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aXYZ, aRMax, aNnn, false, aXYZIdxDisDo);}
    
    
    /**
     * 现在统一改为 for-each 的形式，专门使用曼哈顿距离作为判据的方法；
     * 这里输入的 aRMaxMHT 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子（曼哈顿距离）
     * <p>
     * MHT: ManHaTtan distance
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMaxMHT 最大的近邻半径，曼哈顿距离
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子（默认为 false）
     */
    public void forEachNeighborMHT(int  aIDX, double aRMaxMHT, boolean aHalf, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aIDX, aRMaxMHT, aHalf, true, aXYZIdxDisDo);}
    public void forEachNeighborMHT(int  aIDX, double aRMaxMHT, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighborMHT(aIDX, aRMaxMHT, false, aXYZIdxDisDo);}
    public void forEachNeighborMHT(IXYZ aXYZ, double aRMaxMHT, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aXYZ, aRMaxMHT, true, aXYZIdxDisDo);}
    /**
     * 增加的限制最大近邻数目的遍历方法
     * @author liqa
     */
    public void forEachNeighborMHT(int  aIDX, double aRMax, int aNnn, boolean aHalf, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, true, aXYZIdxDisDo);}
    public void forEachNeighborMHT(int  aIDX, double aRMax, int aNnn, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighborMHT(aIDX, aRMax, aNnn, false, aXYZIdxDisDo);}
    public void forEachNeighborMHT(IXYZ aXYZ, double aRMax, int aNnn, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aXYZ, aRMax, aNnn, true, aXYZIdxDisDo);}
}
