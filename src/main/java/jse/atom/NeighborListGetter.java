package jse.atom;

import jse.cache.IObjectPool;
import jse.cache.ThreadLocalObjectCachePool;
import jse.code.collection.IntList;
import jse.code.functional.IIndexFilter;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.parallel.IShutdownable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntConsumer;


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
    private IMatrix mAtomDataXYZ;  // 现在改为 Matrix 存储，每行为一个原子的 xyz 数据
    private final IBox mBox;
    private final @Nullable XYZ mBoxA, mBoxB, mBoxC; // null for normal
    private final XYZ mBoxXYZ; // 表示三个方向的模拟盒平面之间的距离，用于确定 cell 需要分划的份数
    private final int mAtomNum;
    private final double mMinBox;
    
    private final TreeMap<Integer, ILinkedCell> mLinkedCells = new TreeMap<>(); // 记录对应有效近邻半径的 LinkedCell，使用 Integer 只存储倍率（负值表示除法），避免 double 作为 key 的问题
    private final long mInitThreadID;
    
    /** NL 只支持已经经过平移的数据，目前暂不支持外部创建 */
    NeighborListGetter(IMatrix aAtomDataXYZ, int aAtomNum, IBox aBox) {
        mAtomDataXYZ = aAtomDataXYZ;
        mAtomNum = aAtomNum;
        mBox = aBox;
        if (mBox.isPrism()) {
            // 计算距离，这里涉及一些重复计算，不过不关键就是
            mBoxA = XYZ.toXYZ(aBox.a());
            mBoxB = XYZ.toXYZ(aBox.b());
            mBoxC = XYZ.toXYZ(aBox.c());
            XYZ tBoxBC = mBoxB.cross(mBoxC);
            XYZ tBoxCA = mBoxC.cross(mBoxA);
            XYZ tBoxAB = mBoxA.cross(mBoxB);
            mBoxXYZ = new XYZ(
                mBoxA.dot(tBoxBC) / tBoxBC.norm(),
                mBoxB.dot(tBoxCA) / tBoxCA.norm(),
                mBoxC.dot(tBoxAB) / tBoxAB.norm()
            );
        } else {
            mBoxA = mBoxB = mBoxC = null;
            mBoxXYZ = XYZ.toXYZ(aBox);
        }
        mMinBox = mBoxXYZ.min();
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
    interface ICell {
        void forEach(IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo);
        void forEach(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo);
    }
    
    private final static class Cell extends IntList implements ICell {
        private Cell(int aInitDataLength) {super(aInitDataLength);}
        private Cell() {super();}
        
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
    }
    
    private final static class MirrorCell implements ICell {
        private final Cell mCell;
        private final double mDirX, mDirY, mDirZ;
        private MirrorCell(Cell aCell, double aDirX, double aDirY, double aDirZ) {
            mCell = aCell;
            mDirX = aDirX; mDirY = aDirY; mDirZ = aDirZ;
        }
        @Override public void forEach(IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo) {
            final int tSize = mCell.size();
            final int[] tData = mCell.internalData();
            for (int i = 0; i < tSize; ++i) {
                int tIdx = tData[i];
                aXYZIdxDo.run(aAtomDataXYZ.get(tIdx, 0) + mDirX, aAtomDataXYZ.get(tIdx, 1) + mDirY, aAtomDataXYZ.get(tIdx, 2) + mDirZ, tIdx);
            }
        }
        /** 对于镜像的不能排除 idx 相同的，而对于 Half 的情况要仔细分析 */
        @Override public void forEach(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IMatrix aAtomDataXYZ, IXYZIdxDo aXYZIdxDo) {
            final int tSize = mCell.size();
            final int[] tData = mCell.internalData();
            for (int i = 0; i < tSize; ++i) {
                int tIdx = tData[i];
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
    interface ILinkedCell {
        /** 现在改为 for-each 的形式来避免单一返回值的问题 */
        void forEachNeighbor(IXYZ aXYZ, IXYZIdxDo aXYZIdxDo);
        void forEachNeighbor(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDo aXYZIdxDo);
        void forEachCell(IntConsumer aIdxDo);
        void forEachMirrorCell(IXYZIdxDo aXYZIdxDo);
    }
    
    /** 一般的 LinkedCell 实现 */
    private final class LinkedCell implements ILinkedCell {
        private final List<Cell> mCells;
        private final int mSizeX, mSizeY, mSizeZ;
        private final @Nullable XYZ mCellBoxXYZ; // null for prism
        private LinkedCell(int aMul, int aSizeX, int aSizeY, int aSizeZ) {
            mSizeX = aSizeX; mSizeY = aSizeY; mSizeZ = aSizeZ;
            // 初始化 cell
            mCells = getCellsAlloc_(aMul, aSizeX, aSizeY, aSizeZ);
            for (Cell tCell : mCells) tCell.clear(); // 直接清空旧数据即可
            // 遍历添加 XYZ
            if (mBox.isPrism()) {
                mCellBoxXYZ = null;
                XYZ tBuf = new XYZ();
                for (int idx = 0; idx < mAtomNum; ++idx) {
                    tBuf.setXYZ(mAtomDataXYZ.get(idx, 0), mAtomDataXYZ.get(idx, 1), mAtomDataXYZ.get(idx, 2));
                    mBox.toDirect(tBuf);
                    int i = MathEX.Code.floor2int(tBuf.mX * mSizeX);
                    int j = MathEX.Code.floor2int(tBuf.mY * mSizeY);
                    int k = MathEX.Code.floor2int(tBuf.mZ * mSizeZ);
                    mCells.get(idx(i, j, k)).add(idx);
                }
            } else {
                mCellBoxXYZ = mBoxXYZ.div(mSizeX, mSizeY, mSizeZ);
                for (int idx = 0; idx < mAtomNum; ++idx) {
                    int i = MathEX.Code.floor2int(mAtomDataXYZ.get(idx, 0) / mCellBoxXYZ.mX);
                    int j = MathEX.Code.floor2int(mAtomDataXYZ.get(idx, 1) / mCellBoxXYZ.mY);
                    int k = MathEX.Code.floor2int(mAtomDataXYZ.get(idx, 2) / mCellBoxXYZ.mZ);
                    mCells.get(idx(i, j, k)).add(idx);
                }
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
            if (mBox.isPrism()) {
                assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                if (i >= mSizeX) {tIsMirror = true; i -= mSizeX; tDirX += mBoxA.mX; tDirY += mBoxA.mY; tDirZ += mBoxA.mZ;}
                else if (i < 0)  {tIsMirror = true; i += mSizeX; tDirX -= mBoxA.mX; tDirY -= mBoxA.mY; tDirZ -= mBoxA.mZ;}
                if (j >= mSizeY) {tIsMirror = true; j -= mSizeY; tDirX += mBoxB.mX; tDirY += mBoxB.mY; tDirZ += mBoxB.mZ;}
                else if (j < 0)  {tIsMirror = true; j += mSizeY; tDirX -= mBoxB.mX; tDirY -= mBoxB.mY; tDirZ -= mBoxB.mZ;}
                if (k >= mSizeZ) {tIsMirror = true; k -= mSizeZ; tDirX += mBoxC.mX; tDirY += mBoxC.mY; tDirZ += mBoxC.mZ;}
                else if (k < 0)  {tIsMirror = true; k += mSizeZ; tDirX -= mBoxC.mX; tDirY -= mBoxC.mY; tDirZ -= mBoxC.mZ;}
            } else {
                if (i >= mSizeX) {tIsMirror = true; i -= mSizeX; tDirX =  mBoxXYZ.mX;}
                else if (i < 0)  {tIsMirror = true; i += mSizeX; tDirX = -mBoxXYZ.mX;}
                if (j >= mSizeY) {tIsMirror = true; j -= mSizeY; tDirY =  mBoxXYZ.mY;}
                else if (j < 0)  {tIsMirror = true; j += mSizeY; tDirY = -mBoxXYZ.mY;}
                if (k >= mSizeZ) {tIsMirror = true; k -= mSizeZ; tDirZ =  mBoxXYZ.mZ;}
                else if (k < 0)  {tIsMirror = true; k += mSizeZ; tDirZ = -mBoxXYZ.mZ;}
            }
            return tIsMirror ? new MirrorCell(mCells.get(idx(i, j, k)), tDirX, tDirY, tDirZ) : mCells.get(idx(i, j, k));
        }
        /** 现在改为 for-each 的形式来避免单一返回值的问题 */
        @Override public void forEachNeighbor(IXYZ aXYZ, IXYZIdxDo aXYZIdxDo) {
            final int i, j, k;
            if (mBox.isPrism()) {
                XYZ tBuf = new XYZ(aXYZ);
                mBox.toDirect(tBuf);
                i = MathEX.Code.floor2int(tBuf.mX * mSizeX);
                j = MathEX.Code.floor2int(tBuf.mY * mSizeY);
                k = MathEX.Code.floor2int(tBuf.mZ * mSizeZ);
            } else {
                assert mCellBoxXYZ != null;
                i = MathEX.Code.floor2int(aXYZ.x() / mCellBoxXYZ.mX);
                j = MathEX.Code.floor2int(aXYZ.y() / mCellBoxXYZ.mY);
                k = MathEX.Code.floor2int(aXYZ.z() / mCellBoxXYZ.mZ);
            }
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
            final int i, j, k;
            if (mBox.isPrism()) {
                XYZ tBuf = new XYZ(mAtomDataXYZ.get(aIdx, 0), mAtomDataXYZ.get(aIdx, 1), mAtomDataXYZ.get(aIdx, 2));
                mBox.toDirect(tBuf);
                i = MathEX.Code.floor2int(tBuf.mX * mSizeX);
                j = MathEX.Code.floor2int(tBuf.mY * mSizeY);
                k = MathEX.Code.floor2int(tBuf.mZ * mSizeZ);
            } else {
                assert mCellBoxXYZ != null;
                i = MathEX.Code.floor2int(mAtomDataXYZ.get(aIdx, 0) / mCellBoxXYZ.mX);
                j = MathEX.Code.floor2int(mAtomDataXYZ.get(aIdx, 1) / mCellBoxXYZ.mY);
                k = MathEX.Code.floor2int(mAtomDataXYZ.get(aIdx, 2) / mCellBoxXYZ.mZ);
            }
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
        @Override public void forEachCell(IntConsumer aIdxDo) {
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
                final double tX = mAtomDataXYZ.get(idx, 0);
                final double tY = mAtomDataXYZ.get(idx, 1);
                final double tZ = mAtomDataXYZ.get(idx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    aXYZIdxDo.run(tX                           , tY                           , tZ                           , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX                  , tY+mBoxA.mY                  , tZ+mBoxA.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX                  , tY-mBoxA.mY                  , tZ-mBoxA.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX                  , tY+mBoxB.mY                  , tZ+mBoxB.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX                  , tY-mBoxB.mY                  , tZ-mBoxB.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxC.mX                  , tY+mBoxC.mY                  , tZ+mBoxC.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxC.mX                  , tY-mBoxC.mY                  , tZ-mBoxC.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX         , tY+mBoxA.mY+mBoxB.mY         , tZ+mBoxA.mZ+mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX         , tY+mBoxA.mY-mBoxB.mY         , tZ+mBoxA.mZ-mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX         , tY-mBoxA.mY+mBoxB.mY         , tZ-mBoxA.mZ+mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX         , tY-mBoxA.mY-mBoxB.mY         , tZ-mBoxA.mZ-mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX+mBoxC.mX         , tY+mBoxB.mY+mBoxC.mY         , tZ+mBoxB.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX-mBoxC.mX         , tY+mBoxB.mY-mBoxC.mY         , tZ+mBoxB.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX+mBoxC.mX         , tY-mBoxB.mY+mBoxC.mY         , tZ-mBoxB.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX-mBoxC.mX         , tY-mBoxB.mY-mBoxC.mY         , tZ-mBoxB.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxC.mX         , tY+mBoxA.mY+mBoxC.mY         , tZ+mBoxA.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxC.mX         , tY+mBoxA.mY-mBoxC.mY         , tZ+mBoxA.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxC.mX         , tY-mBoxA.mY+mBoxC.mY         , tZ-mBoxA.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxC.mX         , tY-mBoxA.mY-mBoxC.mY         , tZ-mBoxA.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX+mBoxC.mX, tY+mBoxA.mY+mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX-mBoxC.mX, tY+mBoxA.mY+mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX+mBoxC.mX, tY+mBoxA.mY-mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX-mBoxC.mX, tY+mBoxA.mY-mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX+mBoxC.mX, tY-mBoxA.mY+mBoxB.mY+mBoxC.mY, tZ-mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX-mBoxC.mX, tY-mBoxA.mY+mBoxB.mY-mBoxC.mY, tZ-mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX+mBoxC.mX, tY-mBoxA.mY-mBoxB.mY+mBoxC.mY, tZ-mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX-mBoxC.mX, tY-mBoxA.mY-mBoxB.mY-mBoxC.mY, tZ-mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);
                } else {
                    aXYZIdxDo.run(tX           , tY           , tZ           , idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ           , idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                }
            }
        }
        @Override public void forEachNeighbor(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDo aXYZIdxDo) {
            if (aIdx >= mAtomNum) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
            // 先统一处理一般情况
            final int tEnd = aHalf ? aIdx : mAtomNum;
            for (int idx = 0; idx < tEnd; ++idx) if (idx != aIdx) {
                final double tX = mAtomDataXYZ.get(idx, 0);
                final double tY = mAtomDataXYZ.get(idx, 1);
                final double tZ = mAtomDataXYZ.get(idx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    aXYZIdxDo.run(tX                           , tY                           , tZ                           , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX                  , tY+mBoxA.mY                  , tZ+mBoxA.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX                  , tY-mBoxA.mY                  , tZ-mBoxA.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX                  , tY+mBoxB.mY                  , tZ+mBoxB.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX                  , tY-mBoxB.mY                  , tZ-mBoxB.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxC.mX                  , tY+mBoxC.mY                  , tZ+mBoxC.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxC.mX                  , tY-mBoxC.mY                  , tZ-mBoxC.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX         , tY+mBoxA.mY+mBoxB.mY         , tZ+mBoxA.mZ+mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX         , tY+mBoxA.mY-mBoxB.mY         , tZ+mBoxA.mZ-mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX         , tY-mBoxA.mY+mBoxB.mY         , tZ-mBoxA.mZ+mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX         , tY-mBoxA.mY-mBoxB.mY         , tZ-mBoxA.mZ-mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX+mBoxC.mX         , tY+mBoxB.mY+mBoxC.mY         , tZ+mBoxB.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX-mBoxC.mX         , tY+mBoxB.mY-mBoxC.mY         , tZ+mBoxB.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX+mBoxC.mX         , tY-mBoxB.mY+mBoxC.mY         , tZ-mBoxB.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX-mBoxC.mX         , tY-mBoxB.mY-mBoxC.mY         , tZ-mBoxB.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxC.mX         , tY+mBoxA.mY+mBoxC.mY         , tZ+mBoxA.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxC.mX         , tY+mBoxA.mY-mBoxC.mY         , tZ+mBoxA.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxC.mX         , tY-mBoxA.mY+mBoxC.mY         , tZ-mBoxA.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxC.mX         , tY-mBoxA.mY-mBoxC.mY         , tZ-mBoxA.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX+mBoxC.mX, tY+mBoxA.mY+mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX-mBoxC.mX, tY+mBoxA.mY+mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX+mBoxC.mX, tY+mBoxA.mY-mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX-mBoxC.mX, tY+mBoxA.mY-mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX+mBoxC.mX, tY-mBoxA.mY+mBoxB.mY+mBoxC.mY, tZ-mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX-mBoxC.mX, tY-mBoxA.mY+mBoxB.mY-mBoxC.mY, tZ-mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX+mBoxC.mX, tY-mBoxA.mY-mBoxB.mY+mBoxC.mY, tZ-mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX-mBoxC.mX, tY-mBoxA.mY-mBoxB.mY-mBoxC.mY, tZ-mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);
                } else {
                    aXYZIdxDo.run(tX           , tY           , tZ           , idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ           , idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                }
            }
            if (aHalf) {
                // Half 时需要这样排除一半 idx 相同的情况
                final double tX = mAtomDataXYZ.get(aIdx, 0);
                final double tY = mAtomDataXYZ.get(aIdx, 1);
                final double tZ = mAtomDataXYZ.get(aIdx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    aXYZIdxDo.run(tX+mBoxA.mX                  , tY+mBoxA.mY                  , tZ+mBoxA.mZ                  , aIdx);
                    aXYZIdxDo.run(tX+mBoxB.mX                  , tY+mBoxB.mY                  , tZ+mBoxB.mZ                  , aIdx);
                    aXYZIdxDo.run(tX+mBoxC.mX                  , tY+mBoxC.mY                  , tZ+mBoxC.mZ                  , aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX         , tY+mBoxA.mY+mBoxB.mY         , tZ+mBoxA.mZ+mBoxB.mZ         , aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX         , tY+mBoxA.mY-mBoxB.mY         , tZ+mBoxA.mZ-mBoxB.mZ         , aIdx);
                    aXYZIdxDo.run(tX+mBoxB.mX+mBoxC.mX         , tY+mBoxB.mY+mBoxC.mY         , tZ+mBoxB.mZ+mBoxC.mZ         , aIdx);
                    aXYZIdxDo.run(tX+mBoxB.mX-mBoxC.mX         , tY+mBoxB.mY-mBoxC.mY         , tZ+mBoxB.mZ-mBoxC.mZ         , aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxC.mX         , tY+mBoxA.mY+mBoxC.mY         , tZ+mBoxA.mZ+mBoxC.mZ         , aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxC.mX         , tY+mBoxA.mY-mBoxC.mY         , tZ+mBoxA.mZ-mBoxC.mZ         , aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX+mBoxC.mX, tY+mBoxA.mY+mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX-mBoxC.mX, tY+mBoxA.mY+mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX+mBoxC.mX, tY+mBoxA.mY-mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX-mBoxC.mX, tY+mBoxA.mY-mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, aIdx);
                } else {
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ           , aIdx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ           , aIdx);
                    aXYZIdxDo.run(tX           , tY           , tZ+mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ           , aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ           , aIdx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ+mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ-mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, aIdx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, aIdx);
                }
            }
            // half 且有 region 时还需要考虑另外一半
            if (aHalf && aRegion!=null) for (int idx = aIdx+1; idx < mAtomNum; ++idx) if (!aRegion.accept(idx)) {
                final double tX = mAtomDataXYZ.get(idx, 0);
                final double tY = mAtomDataXYZ.get(idx, 1);
                final double tZ = mAtomDataXYZ.get(idx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    aXYZIdxDo.run(tX                           , tY                           , tZ                           , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX                  , tY+mBoxA.mY                  , tZ+mBoxA.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX                  , tY-mBoxA.mY                  , tZ-mBoxA.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX                  , tY+mBoxB.mY                  , tZ+mBoxB.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX                  , tY-mBoxB.mY                  , tZ-mBoxB.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxC.mX                  , tY+mBoxC.mY                  , tZ+mBoxC.mZ                  , idx);
                    aXYZIdxDo.run(tX-mBoxC.mX                  , tY-mBoxC.mY                  , tZ-mBoxC.mZ                  , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX         , tY+mBoxA.mY+mBoxB.mY         , tZ+mBoxA.mZ+mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX         , tY+mBoxA.mY-mBoxB.mY         , tZ+mBoxA.mZ-mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX         , tY-mBoxA.mY+mBoxB.mY         , tZ-mBoxA.mZ+mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX         , tY-mBoxA.mY-mBoxB.mY         , tZ-mBoxA.mZ-mBoxB.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX+mBoxC.mX         , tY+mBoxB.mY+mBoxC.mY         , tZ+mBoxB.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxB.mX-mBoxC.mX         , tY+mBoxB.mY-mBoxC.mY         , tZ+mBoxB.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX+mBoxC.mX         , tY-mBoxB.mY+mBoxC.mY         , tZ-mBoxB.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxB.mX-mBoxC.mX         , tY-mBoxB.mY-mBoxC.mY         , tZ-mBoxB.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxC.mX         , tY+mBoxA.mY+mBoxC.mY         , tZ+mBoxA.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxC.mX         , tY+mBoxA.mY-mBoxC.mY         , tZ+mBoxA.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxC.mX         , tY-mBoxA.mY+mBoxC.mY         , tZ-mBoxA.mZ+mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxC.mX         , tY-mBoxA.mY-mBoxC.mY         , tZ-mBoxA.mZ-mBoxC.mZ         , idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX+mBoxC.mX, tY+mBoxA.mY+mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX+mBoxB.mX-mBoxC.mX, tY+mBoxA.mY+mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX+mBoxC.mX, tY+mBoxA.mY-mBoxB.mY+mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxA.mX-mBoxB.mX-mBoxC.mX, tY+mBoxA.mY-mBoxB.mY-mBoxC.mY, tZ+mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX+mBoxC.mX, tY-mBoxA.mY+mBoxB.mY+mBoxC.mY, tZ-mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX+mBoxB.mX-mBoxC.mX, tY-mBoxA.mY+mBoxB.mY-mBoxC.mY, tZ-mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX+mBoxC.mX, tY-mBoxA.mY-mBoxB.mY+mBoxC.mY, tZ-mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxA.mX-mBoxB.mX-mBoxC.mX, tY-mBoxA.mY-mBoxB.mY-mBoxC.mY, tZ-mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);
                } else {
                    aXYZIdxDo.run(tX           , tY           , tZ           , idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ           , idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ           , idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX           , tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY           , tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX+mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY+mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ+mBoxXYZ.mZ, idx);
                    aXYZIdxDo.run(tX-mBoxXYZ.mX, tY-mBoxXYZ.mY, tZ-mBoxXYZ.mZ, idx);
                }
            }
        }
        @Override public void forEachCell(IntConsumer aIdxDo) {
            for (int idx = 0; idx < mAtomNum; ++idx) aIdxDo.accept(idx);
        }
        @Override public void forEachMirrorCell(IXYZIdxDo aXYZIdxDo) {
            // 不是最优顺序，不过不重要
            if (mBox.isPrism()) {
                assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX                  , mAtomDataXYZ.get(idx, 1)+mBoxA.mY                  , mAtomDataXYZ.get(idx, 2)+mBoxA.mZ                  , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX                  , mAtomDataXYZ.get(idx, 1)-mBoxA.mY                  , mAtomDataXYZ.get(idx, 2)-mBoxA.mZ                  , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxB.mX                  , mAtomDataXYZ.get(idx, 1)+mBoxB.mY                  , mAtomDataXYZ.get(idx, 2)+mBoxB.mZ                  , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxB.mX                  , mAtomDataXYZ.get(idx, 1)-mBoxB.mY                  , mAtomDataXYZ.get(idx, 2)-mBoxB.mZ                  , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxC.mX                  , mAtomDataXYZ.get(idx, 1)+mBoxC.mY                  , mAtomDataXYZ.get(idx, 2)+mBoxC.mZ                  , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxC.mX                  , mAtomDataXYZ.get(idx, 1)-mBoxC.mY                  , mAtomDataXYZ.get(idx, 2)-mBoxC.mZ                  , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX+mBoxB.mX         , mAtomDataXYZ.get(idx, 1)+mBoxA.mY+mBoxB.mY         , mAtomDataXYZ.get(idx, 2)+mBoxA.mZ+mBoxB.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX-mBoxB.mX         , mAtomDataXYZ.get(idx, 1)+mBoxA.mY-mBoxB.mY         , mAtomDataXYZ.get(idx, 2)+mBoxA.mZ-mBoxB.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX+mBoxB.mX         , mAtomDataXYZ.get(idx, 1)-mBoxA.mY+mBoxB.mY         , mAtomDataXYZ.get(idx, 2)-mBoxA.mZ+mBoxB.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX-mBoxB.mX         , mAtomDataXYZ.get(idx, 1)-mBoxA.mY-mBoxB.mY         , mAtomDataXYZ.get(idx, 2)-mBoxA.mZ-mBoxB.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxB.mX+mBoxC.mX         , mAtomDataXYZ.get(idx, 1)+mBoxB.mY+mBoxC.mY         , mAtomDataXYZ.get(idx, 2)+mBoxB.mZ+mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxB.mX-mBoxC.mX         , mAtomDataXYZ.get(idx, 1)+mBoxB.mY-mBoxC.mY         , mAtomDataXYZ.get(idx, 2)+mBoxB.mZ-mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxB.mX+mBoxC.mX         , mAtomDataXYZ.get(idx, 1)-mBoxB.mY+mBoxC.mY         , mAtomDataXYZ.get(idx, 2)-mBoxB.mZ+mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxB.mX-mBoxC.mX         , mAtomDataXYZ.get(idx, 1)-mBoxB.mY-mBoxC.mY         , mAtomDataXYZ.get(idx, 2)-mBoxB.mZ-mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX+mBoxC.mX         , mAtomDataXYZ.get(idx, 1)+mBoxA.mY+mBoxC.mY         , mAtomDataXYZ.get(idx, 2)+mBoxA.mZ+mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX-mBoxC.mX         , mAtomDataXYZ.get(idx, 1)+mBoxA.mY-mBoxC.mY         , mAtomDataXYZ.get(idx, 2)+mBoxA.mZ-mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX+mBoxC.mX         , mAtomDataXYZ.get(idx, 1)-mBoxA.mY+mBoxC.mY         , mAtomDataXYZ.get(idx, 2)-mBoxA.mZ+mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX-mBoxC.mX         , mAtomDataXYZ.get(idx, 1)-mBoxA.mY-mBoxC.mY         , mAtomDataXYZ.get(idx, 2)-mBoxA.mZ-mBoxC.mZ         , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX+mBoxB.mX+mBoxC.mX, mAtomDataXYZ.get(idx, 1)+mBoxA.mY+mBoxB.mY+mBoxC.mY, mAtomDataXYZ.get(idx, 2)+mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX+mBoxB.mX-mBoxC.mX, mAtomDataXYZ.get(idx, 1)+mBoxA.mY+mBoxB.mY-mBoxC.mY, mAtomDataXYZ.get(idx, 2)+mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX-mBoxB.mX+mBoxC.mX, mAtomDataXYZ.get(idx, 1)+mBoxA.mY-mBoxB.mY+mBoxC.mY, mAtomDataXYZ.get(idx, 2)+mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxA.mX-mBoxB.mX-mBoxC.mX, mAtomDataXYZ.get(idx, 1)+mBoxA.mY-mBoxB.mY-mBoxC.mY, mAtomDataXYZ.get(idx, 2)+mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX+mBoxB.mX+mBoxC.mX, mAtomDataXYZ.get(idx, 1)-mBoxA.mY+mBoxB.mY+mBoxC.mY, mAtomDataXYZ.get(idx, 2)-mBoxA.mZ+mBoxB.mZ+mBoxC.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX+mBoxB.mX-mBoxC.mX, mAtomDataXYZ.get(idx, 1)-mBoxA.mY+mBoxB.mY-mBoxC.mY, mAtomDataXYZ.get(idx, 2)-mBoxA.mZ+mBoxB.mZ-mBoxC.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX-mBoxB.mX+mBoxC.mX, mAtomDataXYZ.get(idx, 1)-mBoxA.mY-mBoxB.mY+mBoxC.mY, mAtomDataXYZ.get(idx, 2)-mBoxA.mZ-mBoxB.mZ+mBoxC.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxA.mX-mBoxB.mX-mBoxC.mX, mAtomDataXYZ.get(idx, 1)-mBoxA.mY-mBoxB.mY-mBoxC.mY, mAtomDataXYZ.get(idx, 2)-mBoxA.mZ-mBoxB.mZ-mBoxC.mZ, idx);}
            } else {
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)           , idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)           , mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)           , mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)+mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)+mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)+mBoxXYZ.mZ, idx);}
                for (int idx = 0; idx < mAtomNum; ++idx) {aXYZIdxDo.run(mAtomDataXYZ.get(idx, 0)-mBoxXYZ.mX, mAtomDataXYZ.get(idx, 1)-mBoxXYZ.mY, mAtomDataXYZ.get(idx, 2)-mBoxXYZ.mZ, idx);}
            }
        }
    }
    private final class ExpandLinkedCell implements ILinkedCell {
        private final int mMulX, mMulY, mMulZ;
        private ExpandLinkedCell(int aMulX, int aMulY, int aMulZ) {mMulX = aMulX; mMulY = aMulY; mMulZ = aMulZ;}
        /** 调整了遍历顺序让速度更快 */
        @Override public void forEachNeighbor(IXYZ aXYZ, IXYZIdxDo aXYZIdxDo) {
            for (int idx = 0; idx < mAtomNum; ++idx) {
                final double tX = mAtomDataXYZ.get(idx, 0);
                final double tY = mAtomDataXYZ.get(idx, 1);
                final double tZ = mAtomDataXYZ.get(idx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    for (int i = -mMulX; i <= mMulX; ++i) {
                    double tDirAX = mBoxA.mX*i, tDirAY = mBoxA.mY*i, tDirAZ = mBoxA.mZ*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirBX = mBoxB.mX*j, tDirBY = mBoxB.mY*j, tDirBZ = mBoxB.mZ*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirAX + tDirBX + mBoxC.mX*k,
                            tY + tDirAY + tDirBY + mBoxC.mY*k,
                            tZ + tDirAZ + tDirBZ + mBoxC.mZ*k,
                            idx);
                    }}}
                } else {
                    for (int i = -mMulX; i <= mMulX; ++i) {
                    double tDirX = mBoxXYZ.mX*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirY = mBoxXYZ.mY*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirX,
                            tY + tDirY,
                            tZ + mBoxXYZ.mZ*k,
                            idx);
                    }}}
                }
            }
        }
        @Override public void forEachNeighbor(int aIdx, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDo aXYZIdxDo) {
            if (aIdx >= mAtomNum) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
            // 先统一处理一般情况
            final int tEnd = aHalf ? aIdx : mAtomNum;
            for (int idx = 0; idx < tEnd; ++idx) if (idx != aIdx) {
                final double tX = mAtomDataXYZ.get(idx, 0);
                final double tY = mAtomDataXYZ.get(idx, 1);
                final double tZ = mAtomDataXYZ.get(idx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    for (int i = -mMulX; i <= mMulX; ++i) {
                    double tDirAX = mBoxA.mX*i, tDirAY = mBoxA.mY*i, tDirAZ = mBoxA.mZ*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirBX = mBoxB.mX*j, tDirBY = mBoxB.mY*j, tDirBZ = mBoxB.mZ*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirAX + tDirBX + mBoxC.mX*k,
                            tY + tDirAY + tDirBY + mBoxC.mY*k,
                            tZ + tDirAZ + tDirBZ + mBoxC.mZ*k,
                            idx);
                    }}}
                } else {
                    for (int i = -mMulX; i <= mMulX; ++i) {
                    double tDirX = mBoxXYZ.mX*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirY = mBoxXYZ.mY*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirX,
                            tY + tDirY,
                            tZ + mBoxXYZ.mZ*k,
                            idx);
                    }}}
                }
            }
            if (aHalf) {
                // 扩展情况的 Half 又有所不同，这里需要这样处理 idx 相同的情况
                double tX = mAtomDataXYZ.get(aIdx, 0);
                double tY = mAtomDataXYZ.get(aIdx, 1);
                double tZ = mAtomDataXYZ.get(aIdx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    // 通过这样的遍历方式排除掉对称的一半
                    for (int i = 1; i <= mMulX; ++i) {
                    double tDirAX = mBoxA.mX*i, tDirAY = mBoxA.mY*i, tDirAZ = mBoxA.mZ*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirBX = mBoxB.mX*j, tDirBY = mBoxB.mY*j, tDirBZ = mBoxB.mZ*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirAX + tDirBX + mBoxC.mX*k,
                            tY + tDirAY + tDirBY + mBoxC.mY*k,
                            tZ + tDirAZ + tDirBZ + mBoxC.mZ*k,
                            aIdx);
                    }}}
                    for (int j = 1; j <= mMulY; ++j) {
                    double tDirBX = mBoxB.mX*j, tDirBY = mBoxB.mY*j, tDirBZ = mBoxB.mZ*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirBX + mBoxC.mX*k,
                            tY + tDirBY + mBoxC.mY*k,
                            tZ + tDirBZ + mBoxC.mZ*k,
                            aIdx);
                    }}
                    for (int k = 1; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + mBoxC.mX*k,
                            tY + mBoxC.mY*k,
                            tZ + mBoxC.mZ*k,
                            aIdx);
                    }
                } else {
                    // 通过这样的遍历方式排除掉对称的一半
                    for (int i = 1; i <= mMulX; ++i) {
                    double tDirX = mBoxXYZ.mX*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirY = mBoxXYZ.mY*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirX,
                            tY + tDirY,
                            tZ + mBoxXYZ.mZ*k,
                            aIdx);
                    }}}
                    for (int j = 1; j <= mMulY; ++j) {
                    double tDirY = mBoxXYZ.mY*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX,
                            tY + tDirY,
                            tZ + mBoxXYZ.mZ*k,
                            aIdx);
                    }}
                    for (int k = 1; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX,
                            tY,
                            tZ + mBoxXYZ.mZ*k,
                            aIdx);
                    }
                }
            }
            // half 且有 region 时还需要考虑另外一半
            if (aHalf && aRegion!=null) for (int idx = aIdx+1; idx < mAtomNum; ++idx) if (!aRegion.accept(idx)) {
                double tX = mAtomDataXYZ.get(idx, 0);
                double tY = mAtomDataXYZ.get(idx, 1);
                double tZ = mAtomDataXYZ.get(idx, 2);
                if (mBox.isPrism()) {
                    assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                    for (int i = -mMulX; i <= mMulX; ++i) {
                    double tDirAX = mBoxA.mX*i, tDirAY = mBoxA.mY*i, tDirAZ = mBoxA.mZ*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirBX = mBoxB.mX*j, tDirBY = mBoxB.mY*j, tDirBZ = mBoxB.mZ*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirAX + tDirBX + mBoxC.mX*k,
                            tY + tDirAY + tDirBY + mBoxC.mY*k,
                            tZ + tDirAZ + tDirBZ + mBoxC.mZ*k,
                            idx);
                    }}}
                } else {
                    for (int i = -mMulX; i <= mMulX; ++i) {
                    double tDirX = mBoxXYZ.mX*i;
                    for (int j = -mMulY; j <= mMulY; ++j) {
                    double tDirY = mBoxXYZ.mY*j;
                    for (int k = -mMulZ; k <= mMulZ; ++k) {
                        aXYZIdxDo.run(
                            tX + tDirX,
                            tY + tDirY,
                            tZ + mBoxXYZ.mZ*k,
                            idx);
                    }}}
                }
            }
        }
        @Override public void forEachCell(IntConsumer aIdxDo) {
            for (int idx = 0; idx < mAtomNum; ++idx) aIdxDo.accept(idx);
        }
        @Override public void forEachMirrorCell(IXYZIdxDo aXYZIdxDo) {
            // 不是最优顺序，不过不重要
            if (mBox.isPrism()) {
                assert mBoxA!=null && mBoxB!=null && mBoxC!=null;
                for (int i = -mMulX; i <= mMulX; ++i) {
                double tDirAX = mBoxA.mX*i, tDirAY = mBoxA.mY*i, tDirAZ = mBoxA.mZ*i;
                for (int j = -mMulY; j <= mMulY; ++j) {
                double tDirBX = mBoxB.mX*j, tDirBY = mBoxB.mY*j, tDirBZ = mBoxB.mZ*j;
                for (int k = -mMulZ; k <= mMulZ; ++k) {
                    if (i==0 && j==0 && k==0) continue;
                    for (int idx = 0; idx < mAtomNum; ++idx) {
                        aXYZIdxDo.run(
                            mAtomDataXYZ.get(idx, 0) + tDirAX + tDirBX + mBoxC.mX*k,
                            mAtomDataXYZ.get(idx, 1) + tDirAY + tDirBY + mBoxC.mY*k,
                            mAtomDataXYZ.get(idx, 2) + tDirAZ + tDirBZ + mBoxC.mZ*k,
                            idx);
                    }
                }}}
            } else {
                for (int i = -mMulX; i <= mMulX; ++i) {
                double tDirX = mBoxXYZ.mX*i;
                for (int j = -mMulY; j <= mMulY; ++j) {
                double tDirY = mBoxXYZ.mY*j;
                for (int k = -mMulZ; k <= mMulZ; ++k) {
                    if (i==0 && j==0 && k==0) continue;
                    for (int idx = 0; idx < mAtomNum; ++idx) {
                        aXYZIdxDo.run(
                            mAtomDataXYZ.get(idx, 0) + tDirX,
                            mAtomDataXYZ.get(idx, 1) + tDirY,
                            mAtomDataXYZ.get(idx, 2) + mBoxXYZ.mZ*k,
                            idx);
                    }
                }}}
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
    
    // 使用读写锁来实现线程安全
    private final ReadWriteLock mRWL = new ReentrantReadWriteLock();
    private final Lock mRL = mRWL.readLock();
    private final Lock mWL = mRWL.writeLock();
    
    
    boolean isCeilEntryValid(@Nullable Map.Entry<Integer, ILinkedCell> aCeilEntry, int aMinMulti) {
        if (aCeilEntry == null) return false;
        int tMinMulti = aCeilEntry.getKey();
        return (aMinMulti > 0 && tMinMulti+tMinMulti <= aMinMulti+aMinMulti+aMinMulti) || (aMinMulti < 0 && tMinMulti+tMinMulti+tMinMulti <= aMinMulti+aMinMulti);
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
        int tMinMulti = aRMax>mMinBox ? MathEX.Code.ceil2int(aRMax/mMinBox) : -MathEX.Code.floor2int(mMinBox/aRMax);
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
            int tDiv = -tMinMulti;
            double tCellLength = mMinBox / (double)tDiv;
            int aSizeX = Math.max(MathEX.Code.floor2int(mBoxXYZ.mX / tCellLength), tDiv); // 可以避免舍入误差的问题
            int aSizeY = Math.max(MathEX.Code.floor2int(mBoxXYZ.mY / tCellLength), tDiv);
            int aSizeZ = Math.max(MathEX.Code.floor2int(mBoxXYZ.mZ / tCellLength), tDiv);
            // 对于所有方向都不需要分割的情况特殊考虑，使用专门的 linkedCell 避免缓存的使用
            if (aSizeX==1 && aSizeY==1 && aSizeZ==1) {
                tLinkedCell = new SingleLinkedCell();
                mLinkedCells.put(tMinMulti, tLinkedCell);
            } else {
                tLinkedCell = new LinkedCell(tMinMulti, aSizeX, aSizeY, aSizeZ);
                mLinkedCells.put(tMinMulti, tLinkedCell);
            }
        } else {
            // 再处理需要扩展的情况
            double tCellLength = mMinBox * tMinMulti;
            // 统计扩展数目
            int aMulX = MathEX.Code.ceil2int(tCellLength / mBoxXYZ.mX);
            int aMulY = MathEX.Code.ceil2int(tCellLength / mBoxXYZ.mY);
            int aMulZ = MathEX.Code.ceil2int(tCellLength / mBoxXYZ.mZ);
            // 这里简单起见，统一采用 ExpandLinkedCell 来管理，即使有非常长的边可以进一步分划
            tLinkedCell = new ExpandLinkedCell(aMulX, aMulY, aMulZ);
            mLinkedCells.put(tMinMulti, tLinkedCell);
        }
        mWL.unlock();
        
        // 最后返回近邻
        return tLinkedCell;
    }
    
    
    @FunctionalInterface public interface IXYZIdxDxyzDo {void run(double aX, double aY, double aZ, int aIdx, double aDx, double aDy, double aDz);}
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
     void forEachNeighbor_(final int aIDX, final double aRMax, boolean aHalf, boolean aMHT, @Nullable IIndexFilter aRegion, final IXYZIdxDxyzDo aXYZIdxDxyzDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(aIDX));
        if (aMHT) {
            getProperLinkedCell(aRMax).forEachNeighbor(aIDX, aHalf, aRegion, (x, y, z, idx) -> {
                // 内部会自动处理 idx 相同以及 half 的情况
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDisMHT = Math.abs(tDx) + Math.abs(tDy) + Math.abs(tDz);
                if (tDisMHT < aRMax) aXYZIdxDxyzDo.run(x, y, z, idx, tDx, tDy, tDz);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(aIDX, aHalf, aRegion, (x, y, z, idx) -> {
                // 内部会自动处理 idx 相同以及 half 的情况
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDis2 = tDx*tDx + tDy*tDy + tDz*tDz;
                if (tDis2 < tRMax2) aXYZIdxDxyzDo.run(x, y, z, idx, tDx, tDy, tDz);
            });
        }
    }
    void forEachNeighbor_(int aIDX, double aRMax, boolean aHalf, boolean aMHT, IXYZIdxDxyzDo aXYZIdxDxyzDo) {
        forEachNeighbor_(aIDX, aRMax, aHalf, aMHT, null, aXYZIdxDxyzDo);
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
    void forEachNeighbor_(IXYZ aXYZ, final double aRMax, boolean aMHT, final IXYZIdxDxyzDo aXYZIdxDxyzDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        final XYZ cXYZ = XYZ.toXYZ(aXYZ);
        if (aMHT) {
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDisMHT = Math.abs(tDx) + Math.abs(tDy) + Math.abs(tDz);
                if (tDisMHT < aRMax) aXYZIdxDxyzDo.run(x, y, z, idx, tDx, tDy, tDz);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDis2 = tDx*tDx + tDy*tDy + tDz*tDz;
                if (tDis2 < tRMax2) aXYZIdxDxyzDo.run(x, y, z, idx, tDx, tDy, tDz);
            });
        }
    }
    
    /** 使用这个统一的类来管理，可以限制最大元素数目，并专门处理距离完全相同的情况不会抹去 */
    private static class NearestNeighborList {
        private static class XYZIdxDxyz {
            final double mX, mY, mZ;
            final int mIdx;
            final double mDx, mDy, mDz;
            private final double mDis;
            private XYZIdxDxyz(double aDis, double aX, double aY, double aZ, int aIdx, double aDx, double aDy, double aDz) {
                mDis = aDis;
                mX = aX; mY = aY; mZ = aZ;
                mIdx = aIdx;
                mDx = aDx; mDy = aDy; mDz = aDz;
            }
        }
        /** 直接使用 LinkedList 存储来避免距离完全相同的情况 */
        private final LinkedList<XYZIdxDxyz> mNNList = new LinkedList<>();
        private final int mNnn;
        NearestNeighborList(int aNnn) {mNnn = aNnn;}
        
        void put(double aDis, double aX, double aY, double aZ, int aIdx, double aDx, double aDy, double aDz) {
            // 获取迭代器
            ListIterator<XYZIdxDxyz> li = mNNList.listIterator();
            // 跳转到距离大于或等于 aDis 之前
            while (li.hasNext()) {
                double tDis = li.next().mDis;
                if (tDis >= aDis) {
                    li.previous(); // 回到这个位置之前，在前面插入
                    break;
                }
            }
            // 然后直接进行添加即可
            li.add(new XYZIdxDxyz(aDis, aX, aY, aZ, aIdx, aDx, aDy, aDz));
            // 如果容量超过限制，则移除最后的元素
            if (mNNList.size() > mNnn) mNNList.removeLast();
        }
        
        /** 直接使用 for-each 的形式来遍历，并且全部交给这里来实现避免多重转发 */
        void forEachNeighbor(int aIDX, boolean aHalf, @Nullable IIndexFilter aRegion, IXYZIdxDxyzDo aXYZIdxDxyzDo) {
            for (XYZIdxDxyz tXYZIdxDxyz : mNNList) {
                if (aHalf) {
                    int tIDX = tXYZIdxDxyz.mIdx;
                    // 这里对 idx 相同的情况简单处理，因为精确处理较为麻烦且即使精确处理结果也是不对的
                    // 由于有区域限制，因此一半优化时不在区域内的也需要进行统计
                    if (tIDX <= aIDX || (aRegion!=null && !aRegion.accept(tIDX))) {
                        aXYZIdxDxyzDo.run(tXYZIdxDxyz.mX, tXYZIdxDxyz.mY, tXYZIdxDxyz.mZ, tIDX, tXYZIdxDxyz.mDx, tXYZIdxDxyz.mDy, tXYZIdxDxyz.mDz);
                    }
                } else {
                    aXYZIdxDxyzDo.run(tXYZIdxDxyz.mX, tXYZIdxDxyz.mY, tXYZIdxDxyz.mZ, tXYZIdxDxyz.mIdx, tXYZIdxDxyz.mDx, tXYZIdxDxyz.mDy, tXYZIdxDxyz.mDz);
                }
            }
        }
        void forEachNeighbor(IXYZIdxDxyzDo aXYZIdxDxyzDo) {
            for (XYZIdxDxyz tXYZIdxDxyz : mNNList) {
                aXYZIdxDxyzDo.run(tXYZIdxDxyz.mX, tXYZIdxDxyz.mY, tXYZIdxDxyz.mZ, tXYZIdxDxyz.mIdx, tXYZIdxDxyz.mDx, tXYZIdxDxyz.mDy, tXYZIdxDxyz.mDz);
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
    void forEachNeighbor_(final int aIDX, final double aRMax, int aNnn, boolean aHalf, boolean aMHT, @Nullable IIndexFilter aRegion, IXYZIdxDxyzDo aXYZIdxDxyzDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 特殊输入处理，直接回到没有限制的情况
        if (aNnn <= 0) {
            forEachNeighbor_(aIDX, aRMax, aHalf, aMHT, aRegion, aXYZIdxDxyzDo);
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
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDisMHT = Math.abs(tDx) + Math.abs(tDy) + Math.abs(tDz);
                if (tDisMHT < aRMax) rNN.put(tDisMHT, x, y, z, idx, tDx, tDy, tDz);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(aIDX, false, aRegion, (x, y, z, idx) -> {
                // 内部会自动处理 idx 相同的情况
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDis2 = tDx*tDx + tDy*tDy + tDz*tDz;
                if (tDis2 < tRMax2) rNN.put(tDis2, x, y, z, idx, tDx, tDy, tDz);
            });
        }
        // 然后直接遍历得到的近邻列表，这里再手动处理 half 的情况
        rNN.forEachNeighbor(aIDX, aHalf, aRegion, aXYZIdxDxyzDo);
    }
    void forEachNeighbor_(int aIDX, double aRMax, int aNnn, boolean aHalf, boolean aMHT, IXYZIdxDxyzDo aXYZIdxDxyzDo) {
        forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, aMHT, null, aXYZIdxDxyzDo);
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
    void forEachNeighbor_(IXYZ aXYZ, final double aRMax, int aNnn, boolean aMHT, IXYZIdxDxyzDo aXYZIdxDxyzDo) {
        if (mDead) throw new RuntimeException("This NeighborListGetter is dead");
        
        // 特殊输入处理，直接回到没有限制的情况
        if (aNnn <= 0) {
            forEachNeighbor_(aXYZ, aRMax, aMHT, aXYZIdxDxyzDo);
            return;
        }
        
        // 先遍历所有经历统计出最近的列表
        final NearestNeighborList rNN = new NearestNeighborList(aNnn);
        final XYZ cXYZ = XYZ.toXYZ(aXYZ);
        if (aMHT) {
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDisMHT = Math.abs(tDx) + Math.abs(tDy) + Math.abs(tDz);
                if (tDisMHT < aRMax) rNN.put(tDisMHT, x, y, z, idx, tDx, tDy, tDz);
            });
        } else {
            final double tRMax2 = aRMax*aRMax;
            getProperLinkedCell(aRMax).forEachNeighbor(cXYZ, (x, y, z, idx) -> {
                double tDx = x - cXYZ.mX;
                double tDy = y - cXYZ.mY;
                double tDz = z - cXYZ.mZ;
                double tDis2 = tDx*tDx + tDy*tDy + tDz*tDz;
                if (tDis2 < tRMax2) rNN.put(tDis2, x, y, z, idx, tDx, tDy, tDz);
            });
        }
        // 然后直接遍历得到的近邻列表
        rNN.forEachNeighbor(aXYZIdxDxyzDo);
    }
    
    
    /**
     * 现在统一改为 for-each 的形式，一般的使用欧几里得距离作为判据的方法；
     * 这里输入的 aRMax 会保证完全遍历所有在这个距离内的粒子，并且不会遍历到超过此距离的粒子
     * @author liqa
     * @param aIDX 中心粒子的 index
     * @param aRMax 最大的近邻半径
     * @param aHalf 是否考虑 index 对易后一致的情况，只遍历一半的原子（默认为 false）
     */
    public void forEachNeighbor(int  aIDX, double aRMax, boolean aHalf, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aIDX, aRMax, aHalf, false, aXYZIdxDxyzDo);}
    public void forEachNeighbor(int  aIDX, double aRMax, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor(aIDX, aRMax, false, aXYZIdxDxyzDo);}
    public void forEachNeighbor(IXYZ aXYZ, double aRMax, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aXYZ, aRMax, false, aXYZIdxDxyzDo);}
    /**
     * 增加的限制最大近邻数目的遍历方法
     * @author liqa
     */
    public void forEachNeighbor(int  aIDX, double aRMax, int aNnn, boolean aHalf, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, false, aXYZIdxDxyzDo);}
    public void forEachNeighbor(int  aIDX, double aRMax, int aNnn, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor(aIDX, aRMax, aNnn, false, aXYZIdxDxyzDo);}
    public void forEachNeighbor(IXYZ aXYZ, double aRMax, int aNnn, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aXYZ, aRMax, aNnn, false, aXYZIdxDxyzDo);}
    /**
     * 使用给定区域限制下遍历时，合法 half 遍历的方法
     * @author liqa
     */
    public void forEachNeighbor(int aIDX, double aRMax, int aNnn, boolean aHalf, IIndexFilter aRegion, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, false, aRegion, aXYZIdxDxyzDo);}
    
    
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
    public void forEachNeighborMHT(int  aIDX, double aRMaxMHT, boolean aHalf, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aIDX, aRMaxMHT, aHalf, true, aXYZIdxDxyzDo);}
    public void forEachNeighborMHT(int  aIDX, double aRMaxMHT, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighborMHT(aIDX, aRMaxMHT, false, aXYZIdxDxyzDo);}
    public void forEachNeighborMHT(IXYZ aXYZ, double aRMaxMHT, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aXYZ, aRMaxMHT, true, aXYZIdxDxyzDo);}
    /**
     * 增加的限制最大近邻数目的遍历方法
     * @author liqa
     */
    public void forEachNeighborMHT(int  aIDX, double aRMax, int aNnn, boolean aHalf, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aIDX, aRMax, aNnn, aHalf, true, aXYZIdxDxyzDo);}
    public void forEachNeighborMHT(int  aIDX, double aRMax, int aNnn, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighborMHT(aIDX, aRMax, aNnn, false, aXYZIdxDxyzDo);}
    public void forEachNeighborMHT(IXYZ aXYZ, double aRMax, int aNnn, IXYZIdxDxyzDo aXYZIdxDxyzDo) {forEachNeighbor_(aXYZ, aRMax, aNnn, true, aXYZIdxDxyzDo);}
    
    
    /**
     * 根据 cell 的顺序来遍历原子，让原子遍历顺序会按照一定的几何位置，
     * 这对于 voronoi 分析很有用
     * @author liqa
     * @param aRCell 需要的 cell 半径
     * @param aIdxDo 由于不涉及镜像，这里直接返回原子的 index
     */
    public void forEachCell(double aRCell, IntConsumer aIdxDo) {
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
