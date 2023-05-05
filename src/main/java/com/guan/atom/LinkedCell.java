package com.guan.atom;

import com.guan.math.MathEX;
import com.guan.code.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 更加通用易用的 LinkedCell 类 </p>
 * <p> 分区粒子的 cell，并且提供获取周围 cell 链接的 cell 的方法 </p>
 * <p> 目前认为所有边界都是周期边界条件，并且只考虑最近邻的 cell </p>
 * <p> 此类线程安全，包括多个线程同时访问同一个实例 </p>
 */
class LinkedCell<Atom extends IHasXYZ> {
    final @Unmodifiable List<List<Atom>> mCells;
    final int mSizeX, mSizeY, mSizeZ;
    final double[] mCellBox;
    final double[] mBox;
    
    final double mMaxDis; // 此 cell 能使用的最大的近邻距离
    
    // 指定三维的分划份数来初始化
    LinkedCell(Atom[] aAtoms, double[] aBox, int aSizeX, int aSizeY, int aSizeZ) {
        mSizeX = aSizeX; mSizeY = aSizeY; mSizeZ = aSizeZ;
        mBox = aBox;
        mCellBox = new double[]{mBox[0] / (double) mSizeX, mBox[1] / (double) mSizeY, mBox[2] / (double) mSizeZ};
        mMaxDis = MathEX.Vec.min(mCellBox);
        // 初始化 cell
        int tSize = aSizeX * aSizeY * aSizeZ;
        int tCellCap = (int) Math.ceil(2.0 * aAtoms.length / (double) tSize);
        mCells = new ArrayList<>(tSize);
        for (int i = 0; i < tSize; ++i) mCells.add(new ArrayList<>(tCellCap));
        // 遍历添加 Atom
        for (Atom tAtom : aAtoms) {
            double[] tXYZ = tAtom.xyz();
            int i = (int) Math.floor(tXYZ[0] / mCellBox[0]); if (i >= mSizeX) continue;
            int j = (int) Math.floor(tXYZ[1] / mCellBox[1]); if (j >= mSizeY) continue;
            int k = (int) Math.floor(tXYZ[2] / mCellBox[2]); if (k >= mSizeZ) continue;
            add(i, j, k, tAtom);
        }
    }
    int idx(int i, int j, int k) {
        if (i<0 || i>=mSizeX || j<0 || j>=mSizeY || k<0 || k>=mSizeZ) throw new IndexOutOfBoundsException(String.format("Index: (%d, %d, %d)", i, j, k));
        return (i + mSizeX*j + mSizeX*mSizeY*k);
    }
    void add(int i, int j, int k, Atom aAtom) {mCells.get(idx(i, j, k)).add(aAtom);}
    // 获取任意 ijk 的 link，自动判断是否是镜像的并计算镜像的附加值
    Link<Atom> link(int i, int j, int k) {
        double[] tDirection = new double[3];
        boolean tIsMirror = false;
        
        if (i >= mSizeX) {tIsMirror = true; i -= mSizeX; tDirection[0] =  mBox[0];}
        else if (i < 0)  {tIsMirror = true; i += mSizeX; tDirection[0] = -mBox[0];}
        
        if (j >= mSizeY) {tIsMirror = true; j -= mSizeY; tDirection[1] =  mBox[1];}
        else if (j < 0)  {tIsMirror = true; j += mSizeY; tDirection[1] = -mBox[1];}
        
        if (k >= mSizeZ) {tIsMirror = true; k -= mSizeZ; tDirection[2] =  mBox[2];}
        else if (k < 0)  {tIsMirror = true; k += mSizeZ; tDirection[2] = -mBox[2];}
        
        return tIsMirror ? new Link<>(cell(i, j, k), tDirection) : new Link<>(cell(i, j, k));
    }
    
    // 获取的接口
    public @Unmodifiable List<Atom> cell(int i, int j, int k) {return mCells.get(idx(i, j, k));}
    public @Unmodifiable List<Atom> cell(double[] aXYZ) {return cell((int) Math.floor(aXYZ[0] / mCellBox[0]), (int) Math.floor(aXYZ[1] / mCellBox[1]), (int) Math.floor(aXYZ[2] / mCellBox[2]));}
    
    // links 缓存
    private final ThreadLocal<Pair<Integer, List<Link<Atom>>>> mLinksTemp = ThreadLocal.withInitial(() -> new Pair<>(-1, null));
    public @Unmodifiable List<Link<Atom>> links(double[] aXYZ) {return links((int) Math.floor(aXYZ[0] / mCellBox[0]), (int) Math.floor(aXYZ[1] / mCellBox[1]), (int) Math.floor(aXYZ[2] / mCellBox[2]));}
    public @Unmodifiable List<Link<Atom>> links(int i, int j, int k) {
        int tIdx = idx(i, j, k);
        Pair<Integer, List<Link<Atom>>> tLinksTemp = mLinksTemp.get();
        if (tLinksTemp.first == tIdx) return tLinksTemp.second;
        tLinksTemp.first = tIdx;
        List<Link<Atom>> rLinkList = new ArrayList<>(27);
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
    
    // 最常见的实现，返回在 rMax 返回内的所有粒子对应的镜像的坐标 XYZ 以及距离 Dis 组成的四位数组
    public class NeighborListItr extends Itr<double[]> {
        final double[] aXYZ; final double aRMax;
        public NeighborListItr(final double[] aXYZ, final double aRMax) {
            super(aXYZ);
            this.aXYZ = aXYZ; this.aRMax = aRMax;
        }
        @Override public double[] getNext(Atom aNextAtom, Link<Atom> aLink) {
            double[] tXYZ = aNextAtom.xyz();
            double[] tMirrorXYZ_Dis = new double[4];
            tMirrorXYZ_Dis[0] = (aLink.mDirection == null || aLink.mDirection[0] == 0) ? tXYZ[0] : tXYZ[0] + aLink.mDirection[0];
            tMirrorXYZ_Dis[1] = (aLink.mDirection == null || aLink.mDirection[1] == 0) ? tXYZ[1] : tXYZ[1] + aLink.mDirection[1];
            tMirrorXYZ_Dis[2] = (aLink.mDirection == null || aLink.mDirection[2] == 0) ? tXYZ[2] : tXYZ[2] + aLink.mDirection[2];
            tMirrorXYZ_Dis[3] = MathEX.XYZ.distance(tMirrorXYZ_Dis, aXYZ);
            return tMirrorXYZ_Dis[3] > aRMax ? null : tMirrorXYZ_Dis;
        }
    }
    public Iterable<double[]> getNeighborList(final double[] aXYZ, final double aRMax) {
        if (aRMax > mMaxDis) throw new RuntimeException("This cell cannot be used for the RMax: "+aRMax+", max: "+mMaxDis);
        return getNeighborList_(aXYZ, aRMax);
    }
    public Iterable<double[]> getNeighborList_(final double[] aXYZ, final double aRMax) {
        return () -> new NeighborListItr(aXYZ, aRMax);
    }
    
    
    // Link 类，多存储一个 mDirection 来标记镜像偏移，避免重复创建对象
    public static class Link<Atom extends IHasXYZ>  {
        final @Unmodifiable List<Atom> mCell;
        final double @Nullable [] mDirection;
        Link(List<Atom> aSubCell) {this(aSubCell, null);}
        Link(List<Atom> aSubCell, double @Nullable [] aDirection) {
            mCell = aSubCell;
            mDirection = aDirection;
        }
        public @Unmodifiable List<Atom> cell() {return mCell;}
        public boolean isMirror() {return mDirection != null;}
        public double[] direction() {return mDirection;}
    }
    
    // 遍历 Links 的迭代器，重写实现自定义功能
    public abstract class Itr<ReturnType> implements Iterator<ReturnType> {
        private final Iterator<Link<Atom>> mLinksIt;
        private Link<Atom> mLink;
        private Iterator<Atom> mCellIt;
        
        public Itr(int i, int j, int k) {this(links(i, j, k));}
        public Itr(double[] aXYZ) {this(links(aXYZ));}
        private Itr(Iterable<Link<Atom>> aLinks) {
            mLinksIt = aLinks.iterator();
            mLink = mLinksIt.next(); // links 一定有 27 个元素，因此这个永远合法
            mCellIt = mLink.mCell.iterator();
        }
        
        private Atom mNextAtom = null;
        private ReturnType mNext = null;
        
        /**
         * stuff to override
         */
        public boolean isValid(Atom aNextAtom) {return true;}
        public abstract ReturnType getNext(Atom aNextAtom, Link<Atom> aLink); // 返回 null 表示获取失败，直接检测下一个
        
        // 让 mNext 合法，如果失败返回 false
        private boolean validNext() {
            if (mNext != null) return true;
            if (mNextAtom == null) return false;
            if (!isValid(mNextAtom)) {
                mNextAtom = null; // 设置 mNextXYZ_ID 非法保证只检测一次是否合法
                return false;
            }
            mNext = getNext(mNextAtom, mLink);
            mNextAtom = null; // 获取完成后，mNextXYZ_ID 非法
            return mNext != null;
        }
        
        @Override
        public final boolean hasNext() {
            while (true) {
                if (validNext()) return true;
                if (mCellIt.hasNext()) {
                    mNextAtom = mCellIt.next();
                    continue;
                }
                if (mLinksIt.hasNext()) {
                    mLink = mLinksIt.next();
                    mCellIt = mLink.mCell.iterator();
                    mNextAtom = null;
                    continue;
                }
                return false;
            }
        }
        @Override
        public final ReturnType next() {
            if (hasNext()) {
                ReturnType tNext = mNext;
                mNext = null; // 设置 mNext 非法表示此时不再有 Next
                return tNext;
            }
            throw new NoSuchElementException();
        }
    }
}
