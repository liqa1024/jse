package jtool.atom;

import jtool.code.functional.IIndexFilter;
import jtool.code.functional.IIntegerConsumer1;
import jtool.math.MathEX;
import jtool.math.matrix.IMatrix;
import jtool.parallel.IObjectPool;
import jtool.parallel.IShutdownable;
import jtool.parallel.ThreadLocalObjectCachePool;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static jtool.code.CS.ZL_INT;


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
    
    private IMatrix mAtomDataXYZ;  // 现在改为 Matrix 存储，每行为一个原子的 xyz 数据
    private final XYZ mBox;
    private final int mAtomNum;
    private final double mMinBox;
    private final double mCellStep;
    
    private final TreeMap<Integer, ILinkedCell> mLinkedCells = new TreeMap<>(); // 记录对应有效近邻半径的 LinkedCell，使用 Integer 只存储倍率（负值表示除法），避免 double 作为 key 的问题
    private final long mInitThreadID;
    
    /** NL 只支持已经经过平移的数据，目前暂不支持外部创建 */
    NeighborListGetter(IMatrix aAtomDataXYZ, int aAtomNum, IXYZ aBox, double aCellStep) {
        mAtomDataXYZ = aAtomDataXYZ;
        mAtomNum = aAtomNum;
        mBox = XYZ.toXYZ(aBox); // 仅用于计算，直接转为 XYZ 即可
        mMinBox = mBox.min();
        mCellStep = Math.max(aCellStep, 1.1);
        mAllCellsAlloc = sAllCellsAllocCache.getObject();
        mInitThreadID = Thread.currentThread().getId();
    }
    
    /** 直接使用 ObjectCachePool 避免重复创建临时变量 */
    private final static IObjectPool<Map<Integer, List<Cell>>> sAllCellsAllocCache = ThreadLocalObjectCachePool.withInitial(HashMap::new);
    /** 缓存所有的 Cells 的内存空间 */
    private Map<Integer, List<Cell>> mAllCellsAlloc;
    
    /** 方便使用的获取 Cells 内存空间的方法，线程不安全 */
    private List<Cell> getCellsAlloc_(int aMul, int aSizeX, int aSizeY, int aSizeZ) {
        int tSize = aSizeX * aSizeY * aSizeZ;
        List<Cell> tCellsAlloc = mAllCellsAlloc.computeIfAbsent(aMul, key -> new ArrayList<>(tSize));
        while (tCellsAlloc.size() < tSize) tCellsAlloc.add(new Cell());
        return tCellsAlloc;
    }
    
    /** 专用的 Cell 类，内部只存储下标来减少内存占用 */
    private interface ICell {
        void forEach(IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo);
        void forEach(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo);
    }
    
    private final static class Cell implements ICell {
        private int[] mData;
        private int mSize;
        private Cell(int aInitDataLength) {mData = new int[aInitDataLength]; mSize = 0;}
        private Cell() {mData = ZL_INT; mSize = 0;}
        public void forEach(IIntegerConsumer1 aIdxDo) {
            final int tSize = mSize;
            for (int i = 0; i < tSize; ++i) aIdxDo.run(mData[i]);
        }
        @Override public void forEach(IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo) {
            final int tSize = mSize;
            for (int i = 0; i < tSize; ++i) {
                int tIdx = mData[i];
                aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0), aAtomDataXYZ.get(tIdx, 1), aAtomDataXYZ.get(tIdx, 2), tIdx);
            }
        }
        @Override public void forEach(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo) {
            final int tSize = mSize;
            for (int i = 0; i < tSize; ++i) {
                int tIdx = mData[i];
                if (aHalf) {
                    // 由于有区域限制，因此一半优化时不在区域内的也需要进行统计
                    if (tIdx < aIdx || (aRegion!=null && !aRegion.accept(tIdx))) {
                        aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0), aAtomDataXYZ.get(tIdx, 1), aAtomDataXYZ.get(tIdx, 2), tIdx);
                    }
                } else {
                    if (tIdx != aIdx) {
                        aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0), aAtomDataXYZ.get(tIdx, 1), aAtomDataXYZ.get(tIdx, 2), tIdx);
                    }
                }
            }
        }
        private void add(int aValue) {
            if (mData.length == 0) {
                mData = new int[1];
            } else
            if (mData.length <= mSize) {
                int[] oData = mData;
                mData = new int[oData.length * 2];
                System.arraycopy(oData, 0, mData, 0, oData.length);
            }
            mData[mSize] = aValue;
            ++mSize;
        }
        private void clear() {mSize = 0;}
    }
    
    private final static class MirrorCell implements ICell {
        private final Cell mCell;
        private final double mDirX, mDirY, mDirZ;
        private MirrorCell(Cell aCell, double aDirX, double aDirY, double aDirZ) {
            mCell = aCell;
            mDirX = aDirX; mDirY = aDirY; mDirZ = aDirZ;
        }
        @Override public void forEach(IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo) {
            final int tSize = mCell.mSize;
            for (int i = 0; i < tSize; ++i) {
                int tIdx = mCell.mData[i];
                aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0) + mDirX, aAtomDataXYZ.get(tIdx, 1) + mDirY, aAtomDataXYZ.get(tIdx, 2) + mDirZ, tIdx);
            }
        }
        /** 对于镜像的不能排除 idx 相同的，而对于 Half 的情况要仔细分析 */
        @Override public void forEach(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo) {
            final int tSize = mCell.mSize;
            for (int i = 0; i < tSize; ++i) {
                int tIdx = mCell.mData[i];
                if (aHalf) {
                    // 由于有区域限制，因此一半优化时不在区域内的也需要进行统计
                    if (tIdx < aIdx || (aRegion!=null && !aRegion.accept(tIdx))) {
                        aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0) + mDirX, aAtomDataXYZ.get(tIdx, 1) + mDirY, aAtomDataXYZ.get(tIdx, 2) + mDirZ, tIdx);
                    } else
                    if (tIdx == aIdx) {
                        // 使用这个方法只遍历一半的镜像相等 idx 对象
                        if ((mDirX>0.0) || (mDirX==0.0 && (mDirY>0.0 || (mDirY==0.0 && mDirZ>0.0)))) {
                            aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0) + mDirX, aAtomDataXYZ.get(tIdx, 1) + mDirY, aAtomDataXYZ.get(tIdx, 2) + mDirZ, tIdx);
                        }
                    }
                } else {
                    aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0) + mDirX, aAtomDataXYZ.get(tIdx, 1) + mDirY, aAtomDataXYZ.get(tIdx, 2) + mDirZ, tIdx);
                }
            }
        }
    }
    
    /** 现在 Linked 直接放在内部 */
    private interface ILinkedCell {
        /** 现在改为 for-each 的形式来避免单一返回值的问题 */
        void forEachNeighbor(IXYZ aXYZ, IXYZIdxDo aXYZIdxDo);
        void forEachNeighbor(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDo aXYZIdxDo);
        void forEachCell(IIntegerConsumer1 aIdxDo);
        void forEachMirrorCell(IXYZIdxDo aXYZIdxDo);
    }
    
    /** 一般的 LinkedCell 实现 */
    private final class LinkedCell implements ILinkedCell {
        private final List<Cell> mCells;
        private final int mSizeX, mSizeY, mSizeZ;
        private final XYZ mCellBox;
        private LinkedCell(int aMul, int aSizeX, int aSizeY, int aSizeZ) {
            mSizeX = aSizeX; mSizeY = aSizeY; mSizeZ = aSizeZ;
            mCellBox = mBox.div(mSizeX, mSizeY, mSizeZ);
            // 初始化 cell
            mCells = getCellsAlloc_(aMul, aSizeX, aSizeY, aSizeZ);
            for (Cell tCell : mCells) tCell.clear(); // 直接清空旧数据即可
            // 遍历添加 XYZ
            for (int idx = 0; idx < mAtomNum; ++idx) {
                int i = (int) Math.floor(mAtomDataXYZ.get(idx, 0) / mCellBox.mX); if (i >= mSizeX) continue;
                int j = (int) Math.floor(mAtomDataXYZ.get(idx, 1) / mCellBox.mY); if (j >= mSizeY) continue;
                int k = (int) Math.floor(mAtomDataXYZ.get(idx, 2) / mCellBox.mZ); if (k >= mSizeZ) continue;
                mCells.get(idx(i, j, k)).add(idx);
            }
        }
        private int idx(int i, int j, int k) {
            if (i<0 || i>=mSizeX || j<0 || j>=mSizeY || k<0 || k>=mSizeZ) throw new IndexOutOfBoundsException(String.format("Index: (%d, %d, %d)", i, j, k));
            return (i + mSizeX*j + mSizeX*mSizeY*k);
        }
        // 获取任意 ijk 的 Cell，自动判断是否是镜像的并计算镜像的附加值
        private ICell cell(int i, int j, int k) {
            double tDirX = 0.0, tDirY = 0.0, tDirZ = 0.0;
            boolean tIsMirror = false;
            if (i >= mSizeX) {tIsMirror = true; i -= mSizeX; tDirX =  mBox.mX;}
            else if (i < 0)  {tIsMirror = true; i += mSizeX; tDirX = -mBox.mX;}
            if (j >= mSizeY) {tIsMirror = true; j -= mSizeY; tDirY =  mBox.mY;}
            else if (j < 0)  {tIsMirror = true; j += mSizeY; tDirY = -mBox.mY;}
            if (k >= mSizeZ) {tIsMirror = true; k -= mSizeZ; tDirZ =  mBox.mZ;}
            else if (k < 0)  {tIsMirror = true; k += mSizeZ; tDirZ = -mBox.mZ;}
            return tIsMirror ? new MirrorCell(mCells.get(idx(i, j, k)), tDirX, tDirY, tDirZ) : mCells.get(idx(i, j, k));
        }
        /** 现在改为 for-each 的形式来避免单一返回值的问题 */
        @Override public void forEachNeighbor(IXYZ aXYZ, IXYZIdxDo aXYZIdxDo) {
            final int i = (int) Math.floor(aXYZ.x() / mCellBox.mX);
            final int j = (int) Math.floor(aXYZ.y() / mCellBox.mY);
            final int k = (int) Math.floor(aXYZ.z() / mCellBox.mZ);
            cell(i  , j  , k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j  , k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j  , k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j+1, k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j-1, k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j  , k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j  , k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j+1, k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j-1, k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j+1, k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j-1, k  ).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j+1, k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j+1, k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j-1, k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j-1, k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j  , k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j  , k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j  , k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j  , k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j+1, k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j+1, k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j-1, k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j-1, k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j+1, k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j+1, k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j-1, k+1).forEach(mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j-1, k-1).forEach(mAtomDataXYZ, aXYZIdxDo);
        }
        @Override public void forEachNeighbor(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDo aXYZIdxDo) {
            if (aIdx >= mAtomNum) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
            final int i = (int) Math.floor(mAtomDataXYZ.get(aIdx, 0) / mCellBox.mX);
            final int j = (int) Math.floor(mAtomDataXYZ.get(aIdx, 1) / mCellBox.mY);
            final int k = (int) Math.floor(mAtomDataXYZ.get(aIdx, 2) / mCellBox.mZ);
            cell(i  , j  , k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j  , k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j  , k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j+1, k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j-1, k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j  , k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j  , k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j+1, k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j-1, k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j+1, k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j-1, k  ).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j+1, k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j+1, k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j-1, k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i  , j-1, k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j  , k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j  , k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j  , k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j  , k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j+1, k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j+1, k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j-1, k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i+1, j-1, k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j+1, k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j+1, k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j-1, k+1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
            cell(i-1, j-1, k-1).forEach(aIdx, aHalf, aRegion, mAtomDataXYZ, aXYZIdxDo);
        }
        @Override public void forEachCell(IIntegerConsumer1 aIdxDo) {
            for (Cell tCell : mCells) tCell.forEach(aIdxDo);
        }
        @Override public void forEachMirrorCell(IXYZIdxDo aXYZIdxDo) {
            // 先遍历 6 个面，这里的顺序不是最优的，不过不重要
            for (int j = 0; j < mSizeY; ++j) for (int i = 0; i < mSizeX; ++i) cell(i     , j     , -1    ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k = 0; k < mSizeZ; ++k) for (int i = 0; i < mSizeX; ++i) cell(i     , -1    , k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k = 0; k < mSizeZ; ++k) for (int j = 0; j < mSizeY; ++j) cell(-1    , j     , k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int j = 0; j < mSizeY; ++j) for (int i = 0; i < mSizeX; ++i) cell(i     , j     , mSizeZ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k = 0; k < mSizeZ; ++k) for (int i = 0; i < mSizeX; ++i) cell(i     , mSizeY, k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k = 0; k < mSizeZ; ++k) for (int j = 0; j < mSizeY; ++j) cell(mSizeX, j     , k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
            // 再按照这个顺序先遍历 8 个棱，会包含所有顶点
            for (int i = -1; i < mSizeX; ++i) cell(i     , -1    , -1    ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int j = -1; j < mSizeY; ++j) cell(mSizeX, j     , -1    ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int i = mSizeX; i >= 0; --i) cell(i     , mSizeY, -1    ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k = -1; k < mSizeZ; ++k) cell(-1    , mSizeY, k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int i = -1; i < mSizeX; ++i) cell(i     , mSizeY, mSizeZ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int j = mSizeY; j >= 0; --j) cell(mSizeX, j     , mSizeZ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int i = mSizeX; i >= 0; --i) cell(i     , -1    , mSizeZ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k = mSizeZ; k >= 0; --k) cell(-1    , -1    , k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
            // 最后遍历 4 个剩下的棱
            for (int j =  0; j < mSizeY; ++j) cell(-1    , j     , -1    ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int j =  0; j < mSizeY; ++j) cell(-1    , j     , mSizeZ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k =  0; k < mSizeZ; ++k) cell(mSizeX, -1    , k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
            for (int k =  0; k < mSizeZ; ++k) cell(mSizeX, mSizeY, k     ).forEach(mAtomDataXYZ, aXYZIdxDo);
        }
    }
    
    /** 为了保证 LinkedCell 内部的简洁和一致，对于恰好不需要分割以及需要扩展的情况单独讨论 */
    private final class SingleLinkedCell implements ILinkedCell {
        /** 调整了遍历顺序让速度更快 */
        @Override public void forEachNeighbor(IXYZ aXYZ, IXYZIdxDo aXYZIdxDo) {
            for (int idx = 0; idx < mAtomNum; ++idx) {
                double tX = mAtomDataXYZ.get(idx, 0);
                double tY = mAtomDataXYZ.get(idx, 1);
                double tZ = mAtomDataXYZ.get(idx, 2);
                aXYZIdxDo.run(tX        , tY        , tZ        , idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ        , idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ-mBox.mZ, idx);
            }
        }
        @Override public void forEachNeighbor(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDo aXYZIdxDo) {
            if (aIdx >= mAtomNum) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
            // 先统一处理一般情况
            final int tEnd = aHalf ? aIdx : mAtomNum;
            for (int idx = 0; idx < tEnd; ++idx) {
                double tX = mAtomDataXYZ.get(idx, 0);
                double tY = mAtomDataXYZ.get(idx, 1);
                double tZ = mAtomDataXYZ.get(idx, 2);
                aXYZIdxDo.run(tX        , tY        , tZ        , idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ        , idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ-mBox.mZ, idx);
            }
            if (aHalf) {
                // Half 时需要这样排除一半 idx 相同的情况
                double tX = mAtomDataXYZ.get(aIdx, 0);
                double tY = mAtomDataXYZ.get(aIdx, 1);
                double tZ = mAtomDataXYZ.get(aIdx, 2);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ        , aIdx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ        , aIdx);
                aXYZIdxDo.run(tX        , tY        , tZ+mBox.mZ, aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ        , aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ        , aIdx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ+mBox.mZ, aIdx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ-mBox.mZ, aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ+mBox.mZ, aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ-mBox.mZ, aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ+mBox.mZ, aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ-mBox.mZ, aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ+mBox.mZ, aIdx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ-mBox.mZ, aIdx);
            }
            // half 且有 region 时还需要考虑另外一半
            if (aHalf && aRegion!=null) for (int idx = aIdx+1; idx < mAtomNum; ++idx) if (!aRegion.accept(idx)) {
                double tX = mAtomDataXYZ.get(idx, 0);
                double tY = mAtomDataXYZ.get(idx, 1);
                double tZ = mAtomDataXYZ.get(idx, 2);
                aXYZIdxDo.run(tX        , tY        , tZ        , idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ        , idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ        , idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX        , tY-mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY        , tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX+mBox.mX, tY-mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY+mBox.mY, tZ-mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ+mBox.mZ, idx);
                aXYZIdxDo.run(tX-mBox.mX, tY-mBox.mY, tZ-mBox.mZ, idx);
            }
        }
        @Override public void forEachCell(IIntegerConsumer1 aIdxDo) {
            for (int idx = 0; idx < mAtomNum; ++idx) aIdxDo.run(idx);
        }
        @Override public void forEachMirrorCell(IXYZIdxDo aXYZIdxDo) {
            // 不是最优顺序，不过不重要
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)        , idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)        , mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)        , mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBox.mX, mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)+mBox.mY, mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)+mBox.mZ, idx);}
            for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBox.mX, mAtomDataXYZ.get(idx, 1)-mBox.mY, mAtomDataXYZ.get(idx, 2)-mBox.mZ, idx);}
        }
    }
    private final class ExpandLinkedCell implements ILinkedCell {
        private final int mMulX, mMulY, mMulZ;
        private ExpandLinkedCell(int aMulX, int aMulY, int aMulZ) {mMulX = aMulX; mMulY = aMulY; mMulZ = aMulZ;}
        /** 调整了遍历顺序让速度更快 */
        @Override public void forEachNeighbor(IXYZ aXYZ, IXYZIdxDo aXYZIdxDo) {
            for (int idx = 0; idx < mAtomNum; ++idx) {
                double tX = mAtomDataXYZ.get(idx, 0);
                double tY = mAtomDataXYZ.get(idx, 1);
                double tZ = mAtomDataXYZ.get(idx, 2);
                for (int i = -mMulX; i <= mMulX; ++i) for (int j = -mMulY; j <= mMulY; ++j) for (int k = -mMulZ; k <= mMulZ; ++k) {
                    aXYZIdxDo.run(
                        i==0 ? tX : tX + mBox.mX*i,
                        j==0 ? tY : tY + mBox.mY*j,
                        k==0 ? tZ : tZ + mBox.mZ*k,
                        idx);
                }
            }
        }
        @Override public void forEachNeighbor(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDo aXYZIdxDo) {
            if (aIdx >= mAtomNum) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
            // 先统一处理一般情况
            final int tEnd = aHalf ? aIdx : mAtomNum;
            for (int idx = 0; idx < tEnd; ++idx) {
                double tX = mAtomDataXYZ.get(idx, 0);
                double tY = mAtomDataXYZ.get(idx, 1);
                double tZ = mAtomDataXYZ.get(idx, 2);
                for (int i = -mMulX; i <= mMulX; ++i) for (int j = -mMulY; j <= mMulY; ++j) for (int k = -mMulZ; k <= mMulZ; ++k) {
                    aXYZIdxDo.run(
                        i==0 ? tX : tX + mBox.mX*i,
                        j==0 ? tY : tY + mBox.mY*j,
                        k==0 ? tZ : tZ + mBox.mZ*k,
                        idx);
                }
            }
            if (aHalf) {
                // 扩展情况的 Half 又有所不同，这里需要这样处理 idx 相同的情况
                double tX = mAtomDataXYZ.get(aIdx, 0);
                double tY = mAtomDataXYZ.get(aIdx, 1);
                double tZ = mAtomDataXYZ.get(aIdx, 2);
                // 通过这样的遍历方式排除掉对称的一半
                for (int i = 1; i <= mMulX; ++i) for (int j = -mMulY; j <= mMulY; ++j) for (int k = -mMulZ; k <= mMulZ; ++k) {
                    aXYZIdxDo.run(
                        i==0 ? tX : tX + mBox.mX*i,
                        j==0 ? tY : tY + mBox.mY*j,
                        k==0 ? tZ : tZ + mBox.mZ*k,
                        aIdx);
                }
                for (int j = 1; j <= mMulY; ++j) for (int k = -mMulZ; k <= mMulZ; ++k) {
                    aXYZIdxDo.run(
                        tX,
                        j==0 ? tY : tY + mBox.mY*j,
                        k==0 ? tZ : tZ + mBox.mZ*k,
                        aIdx);
                }
                for (int k = 1; k <= mMulZ; ++k) {
                    aXYZIdxDo.run(
                        tX,
                        tY,
                        k==0 ? tZ : tZ + mBox.mZ*k,
                        aIdx);
                }
            }
            // half 且有 region 时还需要考虑另外一半
            if (aHalf && aRegion!=null) for (int idx = aIdx+1; idx < mAtomNum; ++idx) if (!aRegion.accept(idx)) {
                double tX = mAtomDataXYZ.get(idx, 0);
                double tY = mAtomDataXYZ.get(idx, 1);
                double tZ = mAtomDataXYZ.get(idx, 2);
                for (int i = -mMulX; i <= mMulX; ++i) for (int j = -mMulY; j <= mMulY; ++j) for (int k = -mMulZ; k <= mMulZ; ++k) {
                    aXYZIdxDo.run(
                        i==0 ? tX : tX + mBox.mX*i,
                        j==0 ? tY : tY + mBox.mY*j,
                        k==0 ? tZ : tZ + mBox.mZ*k,
                        idx);
                }
            }
        }
        @Override public void forEachCell(IIntegerConsumer1 aIdxDo) {
            for (int idx = 0; idx < mAtomNum; ++idx) aIdxDo.run(idx);
        }
        @Override public void forEachMirrorCell(IXYZIdxDo aXYZIdxDo) {
            // 不是最优顺序，不过不重要
            for (int i = -mMulX; i <= mMulX; ++i) for (int j = -mMulY; j <= mMulY; ++j) for (int k = -mMulZ; k <= mMulZ; ++k) if (!(i==0 && j==0 && k==0)) {
                for (int idx = 0; idx < mAtomNum; ++idx) {
                    aXYZIdxDo.run(
                        i==0 ? mAtomDataXYZ.get(idx, 0) : mAtomDataXYZ.get(idx, 0) + mBox.mX*i,
                        j==0 ? mAtomDataXYZ.get(idx, 1) : mAtomDataXYZ.get(idx, 1) + mBox.mY*j,
                        k==0 ? mAtomDataXYZ.get(idx, 2) : mAtomDataXYZ.get(idx, 2) + mBox.mZ*k,
                        idx);
                }
            }
        }
    }
    
    
    
    // 提供一个手动关闭的方法
    private volatile boolean mDead = false;
    @Override public void shutdown() {
        mDead = true; mLinkedCells.clear(); mAtomDataXYZ = null;
        // 归还 Cells 的内存到缓存，这种写法保证永远能获取到 mAllCellsAlloc 时都是合法的
        // 只有相同线程关闭才会归还
        if (Thread.currentThread().getId() == mInitThreadID) {
            Map<Integer, List<Cell>> oAllCellsAlloc = mAllCellsAlloc;
            mAllCellsAlloc = null;
            sAllCellsAllocCache.returnObject(oAllCellsAlloc);
        } else {
            System.err.println("WARNING: ThreadID of shutdown() and init should be SAME in NeighborListGetter");
        }
    }
    
    public double getCellStep() {return mCellStep;}
    
    // 使用读写锁来实现线程安全
    private final ReadWriteLock mRWL = new ReentrantReadWriteLock();
    private final Lock mRL = mRWL.readLock();
    private final Lock mWL = mRWL.writeLock();
    
    
    boolean isCeilEntryValid(@Nullable Map.Entry<Integer, ILinkedCell> aCeilEntry, int aMinMulti) {
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
    ILinkedCell getProperLinkedCell(double aRMax) {
        // 获取需要的最小的 cell 长度倍率
        int tMinMulti = aRMax>mMinBox ? (int)Math.ceil(aRMax/mMinBox) : -(int)Math.floor(mMinBox/aRMax);
        // 尝试获取 LinkedCell
        mRL.lock();
        Map.Entry<Integer, ILinkedCell>
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
        ILinkedCell tLinkedCell;
        // 计算对应 LinkedCell 的参数
        if (tMinMulti < 0) {
            // 先处理不需要扩展的情况
            int tDiv = MathEX.Code.floorPower(-tMinMulti, mCellStep);
            double tCellLength = mMinBox / (double)tDiv;
            int aSizeX = Math.max((int)Math.floor(mBox.mX / tCellLength), tDiv); // 可以避免舍入误差的问题
            int aSizeY = Math.max((int)Math.floor(mBox.mY / tCellLength), tDiv);
            int aSizeZ = Math.max((int)Math.floor(mBox.mZ / tCellLength), tDiv);
            // 对于所有方向都不需要分割的情况特殊考虑，使用专门的 linkedCell 避免缓存的使用
            if (aSizeX==1 && aSizeY==1 && aSizeZ==1) {
                tLinkedCell = new SingleLinkedCell();
                mLinkedCells.put(-tDiv, tLinkedCell);
            } else {
                tLinkedCell = new LinkedCell(-tDiv, aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(-tDiv, tLinkedCell);
            }
        } else {
            // 再处理需要扩展的情况
            int tMul = MathEX.Code.ceilPower(tMinMulti, mCellStep);
            double tCellLength = mMinBox * tMul;
            // 统计扩展数目
            int aMulX = (int)Math.ceil(tCellLength / mBox.mX);
            int aMulY = (int)Math.ceil(tCellLength / mBox.mY);
            int aMulZ = (int)Math.ceil(tCellLength / mBox.mZ);
            // 这里简单起见，统一采用 ExpandLinkedCell 来管理，即使有非常长的边可以进一步分划
            tLinkedCell = new ExpandLinkedCell(aMulX, aMulY, aMulZ);
            mLinkedCells.put(tMul, tLinkedCell);
        }
        mWL.unlock();
        
        // 最后返回近邻
        return tLinkedCell;
    }
    
    
    @FunctionalInterface public interface IXYZIdxDisDo {void run(double aX, double aY, double aZ, int aIdx, double aDis);}
    @FunctionalInterface public interface IXYZIdxDo {void run(double aX, double aY, double aZ, int aIdx);}
    
    /**
     * 现在统一改为 for-each 的形式，提供两个通用的方法遍历所有的近邻；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 最大的近邻半径
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据；
     *             开启后会直接输出 MHT 距离，默认则会输出几何距离的平方
     */
     void forEachNeighbor_(final int aIDX, final double aRMax, boolean aHalf, boolean aMHT, @Nullable IIndexFilter aRegion, final IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(aIDX));
        if (aMHT) {
            getProperLinkedCell(aRMax).forEachNeighbor(aIDX, aHalf, aRegion, (x, y, z, idx) -> {
                // 内部会自动处理 idx 相同以及 half 的情况
                double tDisMHT = cXYZ.distanceMHT(x, y, z);
                if (tDisMHT < aRMax) aXYZIdxDisDo.run(x, y, z, idx, tDisMHT);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(aIDX, aHalf, aRegion, (x, y, z, idx) -> {
                // 内部会自动处理 idx 相同以及 half 的情况
                double tDis2 = cXYZ.distance2(x, y, z);
                if (tDis2 < tRMax2) aXYZIdxDisDo.run(x, y, z, idx, tDis2);
            });
        }
    }
    void forEachNeighbor_(int aIDX, double aRMax, boolean aHalf, boolean aMHT, IXYZIdxDisDo aXYZIdxDisDo) {
        forEachNeighbor_(aIDX, aRMax, aHalf, aMHT, null, aXYZIdxDisDo);
    }
    
    /**
     * 现在统一改为 for-each 的形式，提供两个通用的方法遍历所有的近邻；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子
     * @author liqa
     * @param aXYZ 中心粒子的位置
     * @param aRMax 最大的近邻半径
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据；
     *             开启后会直接输出 MHT 距离，默认则会输出几何距离的平方
     */
    void forEachNeighbor_(IXYZ aXYZ, final double aRMax, boolean aMHT, final IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        final XYZ cXYZ = XYZ.toXYZ(aXYZ);
        if (aMHT) {
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDisMHT = cXYZ.distanceMHT(x, y, z);
                if (tDisMHT < aRMax) aXYZIdxDisDo.run(x, y, z, idx, tDisMHT);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDis2 = cXYZ.distance2(x, y, z);
                if (tDis2 < tRMax2) aXYZIdxDisDo.run(x, y, z, idx, tDis2);
            });
        }
    }
    
    /** 使用这个统一的类来管理，可以限制最大元素数目，并专门处理距离完全相同的情况不会抹去 */
    private static class NearestNeighborList {
        private static class XYZIdxDis {
            final double mX, mY, mZ;
            final int mIdx;
            final double mDis;
            XYZIdxDis(double aX, double aY, double aZ, int aIdx, double aDis) {mX = aX; mY = aY; mZ = aZ; mIdx = aIdx; mDis = aDis;}
            XYZIdxDis(double aDis, double aX, double aY, double aZ, int aIdx) {mX = aX; mY = aY; mZ = aZ; mIdx = aIdx; mDis = aDis;}
        }
        /** 直接使用 LinkedList 存储来避免距离完全相同的情况 */
        private final LinkedList<XYZIdxDis> mNNList = new LinkedList<>();
        private final int mNnn;
        NearestNeighborList(int aNnn) {mNnn = aNnn;}
        
        void put(double aDis, double aX, double aY, double aZ, int aIdx) {
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
            li.add(new XYZIdxDis(aDis, aX, aY, aZ, aIdx));
            // 如果容量超过限制，则移除最后的元素
            if (mNNList.size() > mNnn) mNNList.removeLast();
        }
        
        /** 直接使用 for-each 的形式来遍历，并且全部交给这里来实现避免多重转发 */
        void forEachNeighbor(int aIDX, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDisDo aXYZIdxDisDo) {
            for (XYZIdxDis tXYZIdxDis : mNNList) {
                if (aHalf) {
                    int tIDX = tXYZIdxDis.mIdx;
                    // 这里对 idx 相同的情况简单处理，因为精确处理较为麻烦且即使精确处理结果也是不对的
                    // 由于有区域限制，因此一半优化时不在区域内的也需要进行统计
                    if (tIDX <= aIDX || (aRegion!=null && !aRegion.accept(tIDX))) {
                        aXYZIdxDisDo.run(tXYZIdxDis.mX, tXYZIdxDis.mY, tXYZIdxDis.mZ, tIDX, tXYZIdxDis.mDis);
                    }
                } else {
                    aXYZIdxDisDo.run(tXYZIdxDis.mX, tXYZIdxDis.mY, tXYZIdxDis.mZ, tXYZIdxDis.mIdx, tXYZIdxDis.mDis);
                }
            }
        }
        void forEachNeighbor(IXYZIdxDisDo aXYZIdxDisDo) {
            for (XYZIdxDis tXYZIdxDis : mNNList) {
                aXYZIdxDisDo.run(tXYZIdxDis.mX, tXYZIdxDis.mY, tXYZIdxDis.mZ, tXYZIdxDis.mIdx, tXYZIdxDis.mDis);
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
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据；
     *             开启后会直接输出 MHT 距离，默认则会输出几何距离的平方
     */
    void forEachNeighbor_(final int aIDX, final double aRMax, int aNnn, boolean aHalf, boolean aMHT, @Nullable IIndexFilter aRegion, IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 特殊输入处理，直接回到没有限制的情况
        if (aNnn <= 0) {
            forEachNeighbor_(aIDX, aRMax, aHalf, aMHT, aRegion, aXYZIdxDisDo);
            return;
        }
        // 如果有限制 aNnn 则 aHalf 会有意外的结果，因此会警告建议关闭
        if (aHalf) System.err.println("WARNING: Half will cause Unexpected Results when Nnn>0, although it remains open here to avoid excessive deviation in the results");
        
        // 先遍历所有经历统计出最近的列表
        final NearestNeighborList rNN = new NearestNeighborList(aNnn);
        final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(aIDX));
        // 这里需要先强制关闭 half 来获取限制最近邻数目的列表
        if (aMHT) {
            getProperLinkedCell(aRMax).forEachNeighbor(aIDX, false, aRegion, (x, y, z, idx) -> {
                // 内部会自动处理 idx 相同的情况
                double tDisMHT = cXYZ.distanceMHT(x, y, z);
                if (tDisMHT < aRMax) rNN.put(tDisMHT, x, y, z, idx);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(aIDX, false, aRegion, (x, y, z, idx) -> {
                // 内部会自动处理 idx 相同的情况
                double tDis2 = cXYZ.distance2(x, y, z);
                if (tDis2 < tRMax2) rNN.put(tDis2, x, y, z, idx);
            });
        }
        // 然后直接遍历得到的近邻列表，这里再手动处理 half 的情况
        rNN.forEachNeighbor(aIDX, aHalf, aRegion, aXYZIdxDisDo);
    }
    void forEachNeighbor_(int aIDX, double aRMax, int aNnn, boolean aHalf, boolean aMHT, IXYZIdxDisDo aXYZIdxDisDo) {
        forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, aMHT, null, aXYZIdxDisDo);
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
     * @param aMHT 是否采用曼哈顿距离（MHT: ManHaTtan distance）来作为距离的判据；
     *             开启后会直接输出 MHT 距离，默认则会输出几何距离的平方
     */
    void forEachNeighbor_(IXYZ aXYZ, final double aRMax, int aNnn, boolean aMHT, IXYZIdxDisDo aXYZIdxDisDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 特殊输入处理，直接回到没有限制的情况
        if (aNnn <= 0) {
            forEachNeighbor_(aXYZ, aRMax, aMHT, aXYZIdxDisDo);
            return;
        }
        
        // 先遍历所有经历统计出最近的列表
        final NearestNeighborList rNN = new NearestNeighborList(aNnn);
        final XYZ cXYZ = XYZ.toXYZ(aXYZ);
        if (aMHT) {
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDisMHT = cXYZ.distanceMHT(x, y, z);
                if (tDisMHT < aRMax) rNN.put(tDisMHT, x, y, z, idx);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDis2 = cXYZ.distance2(x, y, z);
                if (tDis2 < tRMax2) rNN.put(tDis2, x, y, z, idx);
            });
        }
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
     * 使用给定区域限制下遍历时，合法 half 遍历的方法
     * @author liqa
     */
    public void forEachNeighbor(int aIDX, double aRMax, int aNnn, boolean aHalf, IIndexFilter aRegion, IXYZIdxDisDo aXYZIdxDisDo) {forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, false, aRegion, aXYZIdxDisDo);}
    
    
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
    
    
    /**
     * 根据 cell 的顺序来遍历原子，让原子遍历顺序会按照一定的几何位置，
     * 这对于 voronoi 分析很有用
     * @author liqa
     * @param aRCell 需要的 cell 半径
     * @param aIdxDo 由于不涉及镜像，这里直接返回原子的 index
     */
    public void forEachCell(double aRCell, IIntegerConsumer1 aIdxDo) {
        getProperLinkedCell(aRCell).forEachCell(aIdxDo);
    }
    
    /**
     * 根据镜像 cell 的顺序遍历镜像原子，让原子遍历顺序会按照一定的几何位置，
     * 这对于 voronoi 分析很有用
     * @author liqa
     * @param aRCell 需要的 cell 半径
     * @param aXYZIdxDo 由于不涉及中间原子，这里不需要计算 dis
     */
    public void forEachMirrorCell(double aRCell, IXYZIdxDo aXYZIdxDo) {
        getProperLinkedCell(aRCell).forEachMirrorCell(aXYZIdxDo);
    }
}
