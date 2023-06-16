package com.jtool.atom;

import com.jtool.code.collection.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liqa
 * <p> 更加通用易用的 LinkedCell 类 </p>
 * <p> 分区粒子的 cell，并且提供获取周围 cell 链接的 cell 的方法 </p>
 * <p> 目前认为所有边界都是周期边界条件，并且只考虑最近邻的 cell </p>
 * <p> 此类线程安全，包括多个线程同时访问同一个实例 </p>
 */
class LinkedCell<A extends IHasXYZ> {
    final @Unmodifiable List<List<A>> mCells;
    final int mSizeX, mSizeY, mSizeZ;
    final XYZ mCellBox;
    final XYZ mBox;
    
    final double mMaxDis; // 此 cell 能使用的最大的近邻距离
    
    // 指定三维的分划份数来初始化
    LinkedCell(A[] aAtoms, XYZ aBox, int aSizeX, int aSizeY, int aSizeZ) {
        mSizeX = aSizeX; mSizeY = aSizeY; mSizeZ = aSizeZ;
        mBox = aBox;
        mCellBox = mBox.div(mSizeX, mSizeY, mSizeZ);
        mMaxDis = mCellBox.min();
        // 初始化 cell
        int tSize = aSizeX * aSizeY * aSizeZ;
        int tCellCap = (int) Math.ceil(2.0 * aAtoms.length / (double) tSize);
        mCells = new ArrayList<>(tSize);
        for (int i = 0; i < tSize; ++i) mCells.add(new ArrayList<>(tCellCap));
        // 遍历添加 Atom
        for (A tAtom : aAtoms) {
            int i = (int) Math.floor(tAtom.x() / mCellBox.mX); if (i >= mSizeX) continue;
            int j = (int) Math.floor(tAtom.y() / mCellBox.mY); if (j >= mSizeY) continue;
            int k = (int) Math.floor(tAtom.z() / mCellBox.mZ); if (k >= mSizeZ) continue;
            add(i, j, k, tAtom);
        }
    }
    int idx(int i, int j, int k) {
        if (i<0 || i>=mSizeX || j<0 || j>=mSizeY || k<0 || k>=mSizeZ) throw new IndexOutOfBoundsException(String.format("Index: (%d, %d, %d)", i, j, k));
        return (i + mSizeX*j + mSizeX*mSizeY*k);
    }
    void add(int i, int j, int k, A aAtom) {mCells.get(idx(i, j, k)).add(aAtom);}
    // 获取任意 ijk 的 link，自动判断是否是镜像的并计算镜像的附加值
    Link<A> link(int i, int j, int k) {
        double tDirX = 0.0, tDirY = 0.0, tDirZ = 0.0;
        boolean tIsMirror = false;
        
        if (i >= mSizeX) {tIsMirror = true; i -= mSizeX; tDirX =  mBox.mX;}
        else if (i < 0)  {tIsMirror = true; i += mSizeX; tDirX = -mBox.mX;}
        
        if (j >= mSizeY) {tIsMirror = true; j -= mSizeY; tDirY =  mBox.mY;}
        else if (j < 0)  {tIsMirror = true; j += mSizeY; tDirY = -mBox.mY;}
        
        if (k >= mSizeZ) {tIsMirror = true; k -= mSizeZ; tDirZ =  mBox.mZ;}
        else if (k < 0)  {tIsMirror = true; k += mSizeZ; tDirZ = -mBox.mZ;}
        
        return tIsMirror ? new Link<>(cell(i, j, k), new XYZ(tDirX, tDirY, tDirZ)) : new Link<>(cell(i, j, k));
    }
    
    // 获取的接口
    public @Unmodifiable List<A> cell(int i, int j, int k) {return mCells.get(idx(i, j, k));}
    public @Unmodifiable List<A> cell(IHasXYZ aXYZ) {return cell((int) Math.floor(aXYZ.x() / mCellBox.mX), (int) Math.floor(aXYZ.y() / mCellBox.mY), (int) Math.floor(aXYZ.z() / mCellBox.mZ));}
    
    // links 缓存
    private final ThreadLocal<Pair<Integer, List<Link<A>>>> mLinksTemp = ThreadLocal.withInitial(() -> new Pair<>(-1, null));
    public @Unmodifiable List<Link<A>> links(IHasXYZ aXYZ) {return links((int) Math.floor(aXYZ.x() / mCellBox.mX), (int) Math.floor(aXYZ.y() / mCellBox.mY), (int) Math.floor(aXYZ.z() / mCellBox.mZ));}
    public @Unmodifiable List<Link<A>> links(int i, int j, int k) {
        int tIdx = idx(i, j, k);
        Pair<Integer, List<Link<A>>> tLinksTemp = mLinksTemp.get();
        if (tLinksTemp.first == tIdx) return tLinksTemp.second;
        tLinksTemp.first = tIdx;
        List<Link<A>> rLinkList = new ArrayList<>(27);
        tLinksTemp.second = rLinkList;
        rLinkList.add(link(i  , j  , k  ));
        rLinkList.add(link(i+1, j  , k  ));
        rLinkList.add(link(i-1, j  , k  ));
        rLinkList.add(link(i  , j+1, k  ));
        rLinkList.add(link(i  , j-1, k  ));
        rLinkList.add(link(i  , j  , k+1));
        rLinkList.add(link(i  , j  , k-1));
        rLinkList.add(link(i+1, j+1, k  ));
        rLinkList.add(link(i+1, j-1, k  ));
        rLinkList.add(link(i-1, j+1, k  ));
        rLinkList.add(link(i-1, j-1, k  ));
        rLinkList.add(link(i  , j+1, k+1));
        rLinkList.add(link(i  , j+1, k-1));
        rLinkList.add(link(i  , j-1, k+1));
        rLinkList.add(link(i  , j-1, k-1));
        rLinkList.add(link(i+1, j  , k+1));
        rLinkList.add(link(i-1, j  , k+1));
        rLinkList.add(link(i+1, j  , k-1));
        rLinkList.add(link(i-1, j  , k-1));
        rLinkList.add(link(i+1, j+1, k+1));
        rLinkList.add(link(i+1, j+1, k-1));
        rLinkList.add(link(i+1, j-1, k+1));
        rLinkList.add(link(i+1, j-1, k-1));
        rLinkList.add(link(i-1, j+1, k+1));
        rLinkList.add(link(i-1, j+1, k-1));
        rLinkList.add(link(i-1, j-1, k+1));
        rLinkList.add(link(i-1, j-1, k-1));
        return rLinkList;
    }
    
    
    // Link 类，多存储一个 mDirection 来标记镜像偏移，避免重复创建对象
    public static final class Link<A extends IHasXYZ>  {
        private final @Unmodifiable List<A> mCell;
        private final @Nullable XYZ mDirection;
        private Link(List<A> aSubCell) {this(aSubCell, null);}
        private Link(List<A> aSubCell, @Nullable XYZ aDirection) {
            mCell = aSubCell;
            mDirection = aDirection;
        }
        public boolean isMirror() {return mDirection!=null;}
        public XYZ direction() {return mDirection;}
    }
    
    @FunctionalInterface
    public interface ILinkedCellDo<A extends IHasXYZ> {
        void run(A aAtom, Link<A> aLink);
    }
    /** 现在改为 for-each 的形式来避免单一返回值的问题 */
    public void forEachNeighbor(IHasXYZ aXYZ, ILinkedCellDo<A> aLinkedCellDo) {
        for (Link<A> tLink : links(aXYZ)) for (A tAtom : tLink.mCell) aLinkedCellDo.run(tAtom, tLink);
    }
}
