/**
 * Copyright (C) 2009 Hal Hildebrand. All rights reserved.
 * <p>
 * This file is part of the 3D Incremental Voronoi system
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Modifications:
 * - Copyright (C) 2023 Qing'an Li. All rights reserved.
 * - Simplify for project usage and add voronoi parameter calculation
 */
package jsex.voronoi;

import jse.atom.AbstractXYZ;
import jse.atom.XYZ;
import jse.atom.IXYZ;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.random.IRandom;
import jse.code.random.LocalRandom;
import jse.math.MathEX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

import static jse.code.CS.RANDOM;


/**
 * 简化使用的 3D Voronoi 构造器，基于：
 * <a href="https://ieeexplore.ieee.org/document/4276112">
 * Computing the 3D Voronoi Diagram Robustly: An Easy Explanation </a>
 * <p>
 * 代码实现参考：
 * <a href="https://github.com/Hellblazer/Voronoi-3D">
 * Hellblazer/Voronoi-3D </a>
 * <p>
 * 此类线程不安全，但不同实例间线程安全
 * @author CHanzy
 */
@SuppressWarnings("UnusedReturnValue")
public final class VoronoiBuilder {
    /** 内部的节点类，存储自身位置和一个近邻的四面体即可 */
    abstract class AbstractVertex implements IVertex {
        final @NotNull XYZ mXYZ;
        /*@NotNull*/ Tetrahedron mAdj;
        AbstractVertex(@NotNull XYZ aXYZ, /*@NotNull*/ Tetrahedron aAdj) {mXYZ = aXYZ; mAdj = aAdj;}
        
        final int orient(Vertex aA, Vertex aB, Vertex aC) {return VoronoiBuilder.orient(mXYZ, aA.mXYZ, aB.mXYZ, aC.mXYZ);}
        final void freshenAdjacent(Tetrahedron aTet) {if (!mAdj.valid()) mAdj = aTet;}
    }
    
    /** 内部的定向面类 */
    class OrientedFace {
        /** 这里多存储一些成员变量，可能性能会有所下降，但是可以减少一些重复代码，并且保证代码的一致性 */
        final @NotNull Tetrahedron mIncident;
        final byte mFace;
        final byte mAdjFace;
        OrientedFace(@NotNull Tetrahedron aIncident, byte aFace) {
            mIncident = aIncident; mFace = aFace;
            Tetrahedron tAdj = adjacent();
            mAdjFace = tAdj==null ? PosTet.NULL : tAdj.ordinalOf(mIncident);
        }
        
        boolean hasAdjacent() {return mAdjFace!=PosTet.NULL;}
        @NotNull Tetrahedron incident() {return mIncident;}
        @Nullable Tetrahedron adjacent() {return mIncident.getNeighbor(mFace);}
        @NotNull Vertex incidentVertex() {return mIncident.getVertex(mFace);}
        @Nullable Vertex adjacentVertex() {return mAdjFace==PosTet.NULL ? null : Objects.requireNonNull(adjacent()).getVertex(mAdjFace);}
        
        boolean valid() {
            if (!incident().valid()) return false;
            @Nullable Tetrahedron tAdjacent = adjacent();
            return tAdjacent!=null && tAdjacent.valid();
        }
        
        /** 如果相邻四面体中的顶点包含在入射四面体的外球面中，则返回 true */
        boolean notRegular() {return hasAdjacent() && mIncident.inSphere(Objects.requireNonNull(adjacentVertex()).mXYZ) > 0;}
        
        Vertex getVertex(int aIdx) {
            switch(mFace) {
            case PosTet.A: {
                switch (aIdx) {
                case 0: {return mIncident.mC;}
                case 1: {return mIncident.mB;}
                case 2: {return mIncident.mD;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.B: {
                switch (aIdx) {
                case 0: {return mIncident.mD;}
                case 1: {return mIncident.mA;}
                case 2: {return mIncident.mC;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.C: {
                switch (aIdx) {
                case 0: {return mIncident.mA;}
                case 1: {return mIncident.mD;}
                case 2: {return mIncident.mB;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.D: {
                switch (aIdx) {
                case 0: {return mIncident.mB;}
                case 1: {return mIncident.mC;}
                case 2: {return mIncident.mA;}
                default: throw new RuntimeException();
                }
            }
            default: throw new RuntimeException();
            }
        }
        
        
        /**
         * 尝试翻转此面保证 delaunay condition
         * <p>
         * 很乱的写法，为了避免出现问题，这里保持原本写法
         * @param rEars 缓存此操作需要优化的面
         * @return null 如果尝试翻转失败，如果成功则返回新的四面体中的一个
         */
        Tetrahedron tryFlip(Deque<OrientedFace> rEars) {
            if (!valid()) return null;
            Vertex tIncidentVertex = incidentVertex();
            
            int tReflexEdge = 0;
            int tReflexEdgeNum = 0;
            // Determine how many faces are visible from the tetrahedron formed by the inserted point and the popped facet
            // 这应该是一种优化操作
            for (int i = 0; tReflexEdgeNum < 2 && i < 3; i++) {
                if (isReflex(i)) {
                    tReflexEdge = i;
                    ++tReflexEdgeNum;
                }
            }
            
            Tetrahedron tOut = null;
            if (tReflexEdgeNum == 0 && notRegular()) {
                // Only one face of the opposing tetrahedron is visible
                for (Tetrahedron tTet : flip2to3()) {
                    OrientedFace tFace = tTet.getFace(tIncidentVertex);
                    if (tFace.hasAdjacent()) rEars.add(tFace);
                    tOut = tTet;
                }
            } else if (tReflexEdgeNum == 1 && notRegular()) {
                // Two faces of the opposing tetrahedron are visible
                Vertex opposingVertex = getVertex(tReflexEdge);
                Tetrahedron tTet1 = incident().getNeighbor(opposingVertex);
                Tetrahedron tTet2 = Objects.requireNonNull(adjacent()).getNeighbor(opposingVertex);
                if (tTet1 != null && tTet1 == tTet2) {
                    for (Tetrahedron tTet : flip3to2(tReflexEdge)) {
                        OrientedFace tFace = tTet.getFace(tIncidentVertex);
                        if (tFace.hasAdjacent()) rEars.add(tFace);
                        tOut = tTet;
                    }
                }
            }
            // all three faces are visible, no action taken
            return tOut;
        }
        
        boolean isReflex(int aIdx) {
            Vertex tAdjVertex = adjacentVertex();
            if (tAdjVertex == null) return false;
            
            switch(mFace) {
            case PosTet.A: {
                switch (aIdx) {
                case 0: {return tAdjVertex.orient(mIncident.mA, mIncident.mB, mIncident.mD) == 1;}
                case 1: {return tAdjVertex.orient(mIncident.mC, mIncident.mA, mIncident.mD) == 1;}
                case 2: {return tAdjVertex.orient(mIncident.mC, mIncident.mB, mIncident.mA) == 1;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.B: {
                switch (aIdx) {
                case 0: {return tAdjVertex.orient(mIncident.mB, mIncident.mA, mIncident.mC) == 1;}
                case 1: {return tAdjVertex.orient(mIncident.mD, mIncident.mB, mIncident.mC) == 1;}
                case 2: {return tAdjVertex.orient(mIncident.mD, mIncident.mA, mIncident.mB) == 1;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.C: {
                switch (aIdx) {
                case 0: {return tAdjVertex.orient(mIncident.mC, mIncident.mD, mIncident.mB) == 1;}
                case 1: {return tAdjVertex.orient(mIncident.mA, mIncident.mC, mIncident.mB) == 1;}
                case 2: {return tAdjVertex.orient(mIncident.mA, mIncident.mD, mIncident.mC) == 1;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.D: {
                switch (aIdx) {
                case 0: {return tAdjVertex.orient(mIncident.mD, mIncident.mC, mIncident.mA) == 1;}
                case 1: {return tAdjVertex.orient(mIncident.mB, mIncident.mD, mIncident.mA) == 1;}
                case 2: {return tAdjVertex.orient(mIncident.mB, mIncident.mC, mIncident.mD) == 1;}
                default: throw new RuntimeException();
                }
            }
            default: throw new RuntimeException();
            }
        }
        
        Tetrahedron[] flip2to3() {
            Vertex tOpposingVertex = adjacentVertex(); assert tOpposingVertex != null;
            Vertex tIncidentVertex = incidentVertex();
            Tetrahedron rTet0 = new Tetrahedron(getVertex(0), tIncidentVertex, getVertex(1), tOpposingVertex);
            Tetrahedron rTet1 = new Tetrahedron(getVertex(1), tIncidentVertex, getVertex(2), tOpposingVertex);
            Tetrahedron rTet2 = new Tetrahedron(getVertex(0), getVertex(2), tIncidentVertex, tOpposingVertex);
            
            rTet0.mTetA = rTet1;
            rTet0.mTetC = rTet2;
            
            rTet1.mTetA = rTet2;
            rTet1.mTetC = rTet0;
            
            rTet2.mTetA = rTet1;
            rTet2.mTetB = rTet0;
            
            mIncident.patch(getVertex(2), rTet0, PosTet.D);
            mIncident.patch(getVertex(0), rTet1, PosTet.D);
            mIncident.patch(getVertex(1), rTet2, PosTet.D);
            
            Tetrahedron tAdjacent = adjacent(); assert tAdjacent != null;
            
            tAdjacent.patch(getVertex(0), rTet1, PosTet.B);
            tAdjacent.patch(getVertex(1), rTet2, PosTet.C);
            tAdjacent.patch(getVertex(2), rTet0, PosTet.B);
            
            mIncident.delete();
            tAdjacent.delete();
            
            rTet0.removeAnyDegenerateTetrahedronPair();
            rTet1.removeAnyDegenerateTetrahedronPair();
            rTet2.removeAnyDegenerateTetrahedronPair();
            
            if (rTet0.valid()) {
                if (rTet1.valid()) {
                    if (rTet2.valid()) return new Tetrahedron[]{rTet0, rTet1, rTet2};
                    else return new Tetrahedron[]{rTet0, rTet1};
                } else {
                    if (rTet2.valid()) return new Tetrahedron[]{rTet0, rTet2};
                    else return new Tetrahedron[]{rTet0};
                }
            } else {
                if (rTet1.valid()) {
                    if (rTet2.valid()) return new Tetrahedron[]{rTet1, rTet2};
                    else return new Tetrahedron[]{rTet1};
                } else {
                    if (rTet2.valid()) return new Tetrahedron[]{rTet2};
                    else return ZL_TET;
                }
            }
        }
        
        Tetrahedron[] flip3to2(int reflexEdge) {
            Tetrahedron oTet2 = mIncident.getNeighbor(getVertex(reflexEdge));
            
            Vertex tTop0, tTop1;
            
            switch(reflexEdge) {
            case 0: {tTop0 = getVertex(1); tTop1 = getVertex(2); break;}
            case 1: {tTop0 = getVertex(0); tTop1 = getVertex(2); break;}
            case 2: {tTop0 = getVertex(0); tTop1 = getVertex(1); break;}
            default: throw new RuntimeException();
            }
            
            Vertex tX = getVertex(reflexEdge);
            Vertex tY = incidentVertex();
            Vertex tZ = adjacentVertex(); assert tZ != null;
            
            Tetrahedron rTet0, rTet1;
            if (tTop0.orient(tX, tY, tZ) > 0) {
                rTet0 = new Tetrahedron(tX, tY, tZ, tTop0);
                rTet1 = new Tetrahedron(tY, tX, tZ, tTop1);
            } else {
                rTet0 = new Tetrahedron(tX, tY, tZ, tTop1);
                rTet1 = new Tetrahedron(tY, tX, tZ, tTop0);
            }
            
            rTet0.mTetD = rTet1;
            rTet1.mTetD = rTet0;
            
            mIncident.patch(rTet0.mD, rTet1, rTet1.ordinalOf(adjacentVertex()));
            mIncident.patch(rTet1.mD, rTet0, rTet0.ordinalOf(adjacentVertex()));
            
            Tetrahedron tAdjacent = adjacent(); assert tAdjacent != null;
            
            tAdjacent.patch(rTet0.mD, rTet1, rTet1.ordinalOf(incidentVertex()));
            tAdjacent.patch(rTet1.mD, rTet0, rTet0.ordinalOf(incidentVertex()));
            
            oTet2.patch(rTet0.mD, rTet1, rTet1.ordinalOf(getVertex(reflexEdge)));
            oTet2.patch(rTet1.mD, rTet0, rTet0.ordinalOf(getVertex(reflexEdge)));
            
            mIncident.delete();
            tAdjacent.delete();
            oTet2.delete();
            
            return new Tetrahedron[]{rTet0, rTet1};
        }
    }
    
    /** 内部的四面体类，存储周围四个面对应的近邻 */
    final static Tetrahedron[] ZL_TET = new Tetrahedron[0];
    abstract class AbstractTetrahedron implements ITetrahedron {
        /** 此四面体的四个顶点 */
        final @NotNull Vertex mA, mB, mC, mD;
        /** 此四面体的四个面对应的四面体，如果没有则为 null */
        @Nullable Tetrahedron mTetA = null, mTetB = null, mTetC = null, mTetD = null;
        /** 通过四个点来构造这个四面体 */
        AbstractTetrahedron(@NotNull Vertex aA, @NotNull Vertex aB, @NotNull Vertex aC, @NotNull Vertex aD) {
            mA = aA; mB = aB; mC = aC; mD = aD;
        }
        
        /** 直接从 mAllTet 来检测此四面体是否已经被删除 */
        @Override public boolean valid() {return mAllTet.contains((Tetrahedron)this);}
        /**  是否含有输入的节点 */
        boolean containsVertex(Vertex aVertex) {return mA==aVertex || mB==aVertex || mC==aVertex || mD==aVertex;}
        
        /**
         * 获取给定两个节点组成的棱两侧的近邻四面体，并返回和指定四面体不同的那一个，
         * 用于沿着棱绕圈
         * @author CHanzy
         * @param aVertex1 棱上第一个节点
         * @param aVertex2 棱上第二个节点
         * @param aTetFrom 绕行方向来的四面体，输入 null 表明可以返回任意方向的四面体
         * @return 下一个方向的四面体，null 表明没有这个四面体
         */
        @Nullable Tetrahedron getNeighbor(Vertex aVertex1, Vertex aVertex2, @Nullable Tetrahedron aTetFrom) {
            switch (ordinalOf(aVertex1)) {
            case PosTet.A: {
                switch(ordinalOf(aVertex2)) {
                case PosTet.B: {return mTetC==aTetFrom ? mTetD : mTetC;}
                case PosTet.C: {return mTetB==aTetFrom ? mTetD : mTetB;}
                case PosTet.D: {return mTetB==aTetFrom ? mTetC : mTetB;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.B: {
                switch(ordinalOf(aVertex2)) {
                case PosTet.A: {return mTetC==aTetFrom ? mTetD : mTetC;}
                case PosTet.C: {return mTetA==aTetFrom ? mTetD : mTetA;}
                case PosTet.D: {return mTetA==aTetFrom ? mTetC : mTetA;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.C: {
                switch(ordinalOf(aVertex2)) {
                case PosTet.B: {return mTetA==aTetFrom ? mTetD : mTetA;}
                case PosTet.A: {return mTetB==aTetFrom ? mTetD : mTetB;}
                case PosTet.D: {return mTetB==aTetFrom ? mTetA : mTetB;}
                default: throw new RuntimeException();
                }
            }
            case PosTet.D: {
                switch(ordinalOf(aVertex2)) {
                case PosTet.B: {return mTetC==aTetFrom ? mTetA : mTetC;}
                case PosTet.C: {return mTetB==aTetFrom ? mTetA : mTetB;}
                case PosTet.A: {return mTetB==aTetFrom ? mTetC : mTetB;}
                default: throw new RuntimeException();
                }
            }
            default: throw new RuntimeException();
            }
        }
        
        /** 获取对应面对应的节点 */
        @NotNull Vertex getVertex(byte aFace) {
            switch (aFace) {
            case PosTet.A: {return mA;}
            case PosTet.B: {return mB;}
            case PosTet.C: {return mC;}
            case PosTet.D: {return mD;}
            default: throw new RuntimeException();
            }
        }
        /** 获取对应面的近邻四面体 */
        @Nullable Tetrahedron getNeighbor(byte aFace) {
            switch (aFace) {
            case PosTet.A: {return mTetA;}
            case PosTet.B: {return mTetB;}
            case PosTet.C: {return mTetC;}
            case PosTet.D: {return mTetD;}
            default: throw new RuntimeException();
            }
        }
        Tetrahedron getNeighbor(Vertex vertex) {return getNeighbor(ordinalOf(vertex));}
        /** 设置对应面的近邻四面体 */
        void setNeighbor(byte aFace, @Nullable Tetrahedron aNeighbor) {
            switch (aFace) {
            case PosTet.A: {mTetA = aNeighbor; break;}
            case PosTet.B: {mTetB = aNeighbor; break;}
            case PosTet.C: {mTetC = aNeighbor; break;}
            case PosTet.D: {mTetD = aNeighbor; break;}
            default: throw new RuntimeException();
            }
        }
        /** 获取指定近邻对应的方向 */
        byte ordinalOf(Tetrahedron aNeighbor) {
            if (aNeighbor == null) return PosTet.NULL;
            if (mTetA == aNeighbor) return PosTet.A;
            if (mTetB == aNeighbor) return PosTet.B;
            if (mTetC == aNeighbor) return PosTet.C;
            if (mTetD == aNeighbor) return PosTet.D;
            return PosTet.NULL;
        }
        /** 获取指定节点对应的方向 */
        byte ordinalOf(Vertex aVertex) {
            if (aVertex == null) return PosTet.NULL;
            if (mA == aVertex) return PosTet.A;
            if (mB == aVertex) return PosTet.B;
            if (mC == aVertex) return PosTet.C;
            if (mD == aVertex) return PosTet.D;
            return PosTet.NULL;
        }
        
        int orient(IXYZ aXYZ, byte aFace) {
            switch (aFace) {
            case PosTet.A: {return VoronoiBuilder.orient(aXYZ, mC.mXYZ, mB.mXYZ, mD.mXYZ);}
            case PosTet.B: {return VoronoiBuilder.orient(aXYZ, mD.mXYZ, mA.mXYZ, mC.mXYZ);}
            case PosTet.C: {return VoronoiBuilder.orient(aXYZ, mA.mXYZ, mD.mXYZ, mB.mXYZ);}
            case PosTet.D: {return VoronoiBuilder.orient(aXYZ, mB.mXYZ, mC.mXYZ, mA.mXYZ);}
            default: throw new RuntimeException();
            }
        }
        int inSphere(IXYZ aXYZ) {return VoronoiBuilder.inSphere(aXYZ, mA.mXYZ, mB.mXYZ, mC.mXYZ, mD.mXYZ);}
        
        /** 获取指定界面 */
        OrientedFace getFace(byte aFace) {return new OrientedFace((Tetrahedron)this, aFace);}
        OrientedFace getFace(Vertex aVertex) {return getFace(ordinalOf(aVertex));}
        
        /**
         * 通过在中间插入一个节点点的方式，将一个四面体拆分成四个，
         * 会自动修改所有的 adj 信息使其合法化
         * @param aVertex 插入的节点
         * @param rEars 缓存此操作需要优化的四个面
         * @return 新得到的四个四面体的其中一个
         */
        Tetrahedron flip1to4(Vertex aVertex, Deque<OrientedFace> rEars) {
            // 这个顺序应该是有讲究的，不过我不确定具体要求
            Tetrahedron rTet0 = new Tetrahedron(mA, mB, mC, aVertex);
            Tetrahedron rTet1 = new Tetrahedron(mA, mD, mB, aVertex);
            Tetrahedron rTet2 = new Tetrahedron(mA, mC, mD, aVertex);
            Tetrahedron rTet3 = new Tetrahedron(mB, mD, mC, aVertex);
            
            // 设置近邻
            rTet0.mTetA = rTet3;
            rTet0.mTetB = rTet2;
            rTet0.mTetC = rTet1;
            
            rTet1.mTetA = rTet3;
            rTet1.mTetB = rTet0;
            rTet1.mTetC = rTet2;
            
            rTet2.mTetA = rTet3;
            rTet2.mTetB = rTet1;
            rTet2.mTetC = rTet0;
            
            rTet3.mTetA = rTet2;
            rTet3.mTetB = rTet0;
            rTet3.mTetC = rTet1;
            
            // 将自身近邻对应的近邻合法化
            patch(PosTet.D, rTet0, PosTet.D);
            patch(PosTet.C, rTet1, PosTet.D);
            patch(PosTet.B, rTet2, PosTet.D);
            patch(PosTet.A, rTet3, PosTet.D);
            
            // 移除自身
            delete();
            
            // 设置需要考虑翻转的界面
            OrientedFace
            tFace = rTet0.getFace(PosTet.D);
            if (tFace.hasAdjacent()) rEars.addLast(tFace);
            tFace = rTet1.getFace(PosTet.D);
            if (tFace.hasAdjacent()) rEars.addLast(tFace);
            tFace = rTet2.getFace(PosTet.D);
            if (tFace.hasAdjacent()) rEars.addLast(tFace);
            tFace = rTet3.getFace(PosTet.D);
            if (tFace.hasAdjacent()) rEars.addLast(tFace);
            
            // 返回任意的四面体，这里保持一致
            return rTet1;
        }
        
        void delete() {
            mTetA = mTetB = mTetC = mTetD = null;
            mAllTet.remove((Tetrahedron)this);
        }
        void patch(byte aOld, Tetrahedron aNewTet, byte aNew) {
            Tetrahedron tNeighbor = getNeighbor(aOld);
            if (tNeighbor != null) {
                tNeighbor.setNeighbor(tNeighbor.ordinalOf((Tetrahedron)this), aNewTet);
                aNewTet.setNeighbor(aNew, tNeighbor);
            }
        }
        void patch(Vertex aOld, Tetrahedron aNewTet, byte aNew) {patch(ordinalOf(aOld), aNewTet, aNew);}
        
        @SuppressWarnings("UnnecessaryReturnStatement")
        void removeAnyDegenerateTetrahedronPair() {
            if (mTetA != null) {
                if (mTetA == mTetB) {removeDegenerateTetrahedronPair_(PosTet.A, PosTet.B, PosTet.C, PosTet.D); return;}
                if (mTetA == mTetC) {removeDegenerateTetrahedronPair_(PosTet.A, PosTet.C, PosTet.B, PosTet.D); return;}
                if (mTetA == mTetD) {removeDegenerateTetrahedronPair_(PosTet.A, PosTet.D, PosTet.B, PosTet.C); return;}
            }
            if (mTetB != null) {
                if (mTetB == mTetC) {removeDegenerateTetrahedronPair_(PosTet.B, PosTet.C, PosTet.A, PosTet.D); return;}
                if (mTetB == mTetD) {removeDegenerateTetrahedronPair_(PosTet.B, PosTet.D, PosTet.A, PosTet.C); return;}
            }
            if (mTetC != null) {
                if (mTetC == mTetD) {removeDegenerateTetrahedronPair_(PosTet.C, PosTet.D, PosTet.A, PosTet.B); return;}
            }
        }
        private void removeDegenerateTetrahedronPair_(byte ve1, byte ve2, byte vf1, byte vf2) {
            Tetrahedron nE = getNeighbor(ve1); assert nE != null;
            Tetrahedron nF1_that = nE.getNeighbor(getVertex(vf1));
            Tetrahedron nF2_that = nE.getNeighbor(getVertex(vf2));
            
            patch(vf1, nF1_that, nF1_that.ordinalOf(nE));
            patch(vf2, nF2_that, nF2_that.ordinalOf(nE));
            
            Vertex e1 = getVertex(ve1);
            Vertex e2 = getVertex(ve2);
            Vertex f1 = getVertex(vf1);
            Vertex f2 = getVertex(vf2);
            
            delete();
            nE.delete();
            
            e1.freshenAdjacent(nF1_that);
            f2.freshenAdjacent(nF1_that);
            e2.freshenAdjacent(nF2_that);
            f1.freshenAdjacent(nF2_that);
        }
    }
    
    
    /** 此项目的风格，对于几何中使用的枚举常量，并且为私有情况下，使用 byte 而不是 enum */
    static class PosTet {
        final static byte A = 0, B = 1, C = 2, D = 3, NULL = -1;
        final static byte[] VERTICES = {A, B, C, D}, FACES = VERTICES;
        /** 随机方向的预选值 */
        static final byte[][][] ORDER = {
              {{B, C, D}, {C, B, D}, {C, D, B}, {B, D, C}, {D, B, C}, {D, C, B}}
            , {{A, C, D}, {C, A, D}, {C, D, A}, {A, D, C}, {D, A, C}, {D, C, A}}
            , {{B, A, D}, {A, B, D}, {A, D, B}, {B, D, A}, {D, B, A}, {D, A, B}}
            , {{B, C, A}, {C, B, A}, {C, A, B}, {B, A, C}, {A, B, C}, {A, C, B}}
        };
    }
    private final static double SCALE = Math.pow(2.0, 30);
    
    /**
     * 检测点 aXYZ 是否在 aFace 指定面的正向，如果是则 > 0，否则 < 0，如果恰好在面上则 = 0
     * 相关定义可以参考：
     * <a href="https://ieeexplore.ieee.org/document/4276112">
     * Computing the 3D Voronoi Diagram Robustly: An Easy Explanation </a>
     */
    static int orient(IXYZ aXYZ, IXYZ aA, IXYZ aB, IXYZ aC) {
        double result = VoronoiStaticExtensions.leftOfPlane(null, aA, aB, aC, aXYZ);
        if (result > 0.0) return 1;
        else if (result < 0.0) return -1;
        return 0;
    }
    /**
     * 检测点 aXYZ 是否在此四面体四个顶点组成的球形的内部，如果是则 > 0，否则 < 0，如果恰好在面上则 = 0
     * 相关定义可以参考：
     * <a href="https://ieeexplore.ieee.org/document/4276112">
     * Computing the 3D Voronoi Diagram Robustly: An Easy Explanation </a>
     */
    static int inSphere(IXYZ aXYZ, IXYZ aA, IXYZ aB, IXYZ aC, IXYZ aD) {
        double result = VoronoiStaticExtensions.inSphere(null, aA, aB, aC, aD, aXYZ);
        if (result > 0.0) return 1;
        else if (result < 0.0) return -1;
        return 0;
    }
    
    
    
    /** 初始的极大四面体，保证所有点都会在其内部 */
    private final Vertex mInitVertexA, mInitVertexB, mInitVertexC, mInitVertexD;
    /** 上一步的四面体，用于进行加速搜索过程 */
    private Tetrahedron mLast;
    
    /** 独立的随机数生成器 */
    final IRandom mRNG;
    /** 存储所有的插入的节点，按照插入顺序保留方便使用 */
    final List<Vertex> mAllVertex = new ArrayList<>();
    /** 存储所有的创建的四面体，这个是无序的 */
    final Set<Tetrahedron> mAllTet = new HashSet<>();
    /** 此值用于检验统计值是否有效 */
    int mCheck;
    
    /** 是否需要输出警告 */
    boolean mNoWarning = false;
    public VoronoiBuilder setNoWarning(boolean aNoWarning) {mNoWarning = aNoWarning; return this;}
    public VoronoiBuilder setNoWarning() {return setNoWarning(true);}
    
    /** 边长和面积的截断比例，用于处理退化情况 */
    private double mAreaThreshold = 0.0;
    private double mLengthThreshold = 0.0;
    private double mAreaThresholdAbs = Double.NaN; // 默认用相对值
    private double mLengthThresholdAbs = Double.NaN; // 默认用相对值
    public VoronoiBuilder setAreaThreshold(double aAreaThreshold) {
        double oAreaThreshold = mAreaThreshold;
        mAreaThreshold = Math.max(0.0, aAreaThreshold);
        mAreaThresholdAbs = Double.NaN;
        if (oAreaThreshold != mAreaThreshold) mCheck = mRNG.nextInt();
        return this;
    }
    public VoronoiBuilder setLengthThreshold(double aLengthThreshold) {
        double oLengthThreshold = mLengthThreshold;
        mLengthThreshold = Math.max(0.0, aLengthThreshold);
        mLengthThresholdAbs = Double.NaN;
        if (oLengthThreshold != mLengthThreshold) mCheck = mRNG.nextInt();
        return this;
    }
    public VoronoiBuilder setAreaThresholdAbs(double aAreaThresholdAbs) {
        double oAreaThresholdAbs = mAreaThresholdAbs;
        mAreaThresholdAbs = Math.max(0.0, aAreaThresholdAbs);
        mAreaThreshold = Double.NaN;
        if (oAreaThresholdAbs != mAreaThresholdAbs) mCheck = mRNG.nextInt();
        return this;
    }
    public VoronoiBuilder setLengthThresholdAbs(double aLengthThresholdAbs) {
        double oLengthThresholdAbs = mLengthThresholdAbs;
        mLengthThresholdAbs = Math.max(0.0, aLengthThresholdAbs);
        mLengthThreshold = Double.NaN;
        if (oLengthThresholdAbs != mLengthThresholdAbs) mCheck = mRNG.nextInt();
        return this;
    }
    boolean areaValid(double aArea, double aRefArea) {
        if (!Double.isNaN(mAreaThreshold)) {
            return mAreaThreshold==0.0 || aArea>mAreaThreshold*aRefArea;
        } else
        if (!Double.isNaN(mAreaThresholdAbs)) {
            return mAreaThresholdAbs==0.0 || aArea>mAreaThresholdAbs;
        } else {
            throw new RuntimeException();
        }
    }
    boolean lengthValid(double aLength, double aRefLength) {
        if (!Double.isNaN(mLengthThreshold)) {
            return mLengthThreshold==0.0 || aLength>mLengthThreshold*aRefLength;
        } else
        if (!Double.isNaN(mLengthThresholdAbs)) {
            return mLengthThresholdAbs==0.0 || aLength>mLengthThresholdAbs;
        } else {
            throw new RuntimeException();
        }
    }
    boolean areaCutoff() {
        // NaN 值同样返回 false
        return (mAreaThreshold > 0.0) || (mAreaThresholdAbs > 0.0);
    }
    
    /** Voronor Index 长度 */
    int mIndexLength = 9;
    public VoronoiBuilder setIndexLength(int aIndexLength) {mIndexLength = Math.max(1, aIndexLength); return this;}
    
    
    /** 构造函数 */
    public VoronoiBuilder() {this(RANDOM);}
    public VoronoiBuilder(long aSeed) {this(new LocalRandom(aSeed));}
    VoronoiBuilder(IRandom aRNG) {
        mRNG = aRNG;
        mCheck = mRNG.nextInt();
        // 初始的极大四面体，保证所有点都会在其内部；这样降低对称性，让 2D 情况更好处理
        Tetrahedron tInitTetrahedron = new Tetrahedron(
              new Vertex(-SCALE*1.1, SCALE*1.6,-SCALE*2.3)
            , new Vertex( SCALE*1.5, SCALE*1.9, SCALE*1.8)
            , new Vertex( SCALE*2.2,-SCALE*1.4,-SCALE*1.7)
            , new Vertex(-SCALE*1.2,-SCALE*2.1, SCALE*1.3)
        );
        mLast = tInitTetrahedron;
        mInitVertexA = tInitTetrahedron.mA;
        mInitVertexB = tInitTetrahedron.mB;
        mInitVertexC = tInitTetrahedron.mC;
        mInitVertexD = tInitTetrahedron.mD;
    }
    
    /** 返回是否是虚构的巨大四面体的顶点 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isUniverse(Vertex aVertex) {return aVertex==mInitVertexA || aVertex==mInitVertexB || aVertex==mInitVertexC || aVertex==mInitVertexD;}
    /** 返回是否是虚构的巨大四面体 */
    boolean isUniverse(Tetrahedron aTet) {return aTet.containsVertex(mInitVertexA) || aTet.containsVertex(mInitVertexB) || aTet.containsVertex(mInitVertexC) || aTet.containsVertex(mInitVertexD);}
    
    
    
    /**
     * 此 builder 的构造方法，插入一个 xyz 点，
     * 按照几何的顺序来进行插入可以更快的找到对应的四面体；
     * 为了避免位置被外部意外修改，需要统一进行一次值拷贝
     */
    public void insert(IXYZ aXYZ) {insert(aXYZ, -1);}
    public void insert(double aX, double aY, double aZ) {insert(aX, aY, aZ, -1);}
    public void insert(IXYZ aXYZ, int atomIndex) {insert_(new XYZ(aXYZ), atomIndex);}
    public void insert(double aX, double aY, double aZ, int atomIndex) {insert_(new XYZ(aX, aY, aZ), atomIndex);}
    
    private void insert_(XYZ aXYZ, int atomIndex) {
        mCheck = mRNG.nextInt();
        // 先使用这个寻路算法找到包围输入位置的四面体
        mLast = locate_(aXYZ, mLast);
        // 指定此 XYZ 对应的 adj，并且创建节点
        Vertex tVertex = new Vertex(aXYZ, mLast, atomIndex);
        // 然后将其分成四份，并存储需要考虑翻转的面
        Deque<OrientedFace> tEars = new ArrayDeque<>();
        mLast = mLast.flip1to4(tVertex, tEars);
        // 考虑所有翻转的情况
        while (!tEars.isEmpty()) {
            Tetrahedron tLast = tEars.removeLast().tryFlip(tEars);
            if (tLast != null) mLast = tLast;
        }
        // 存储此节点
        mAllVertex.add(tVertex);
    }
    
    /** 从起始四面体开始，定位到能够包含 aXYZ 的四面体 */
    private Tetrahedron locate_(XYZ aXYZ, Tetrahedron aStart) {
        byte nFace = PosTet.NULL;
        for (byte tFace : PosTet.FACES) {
            if (aStart.orient(aXYZ, tFace) < 0) {nFace = tFace; break;}
        }
        Tetrahedron tCurrent = aStart;
        while (true) {
            // 如果没有下一个界面则表明 aXYZ 就在 tCurrent 四面体内部，终止
            if (nFace == PosTet.NULL) {return tCurrent;}
            // 获取当前四面体对应面的四面体
            Tetrahedron tNext = tCurrent.getNeighbor(nFace);
            assert tNext != null;
            // 下一个界面待定
            nFace = PosTet.NULL;
            // 按照随机的顺序检查 tNext 四面体的三个面，确定下一个面
            for (byte tFace : PosTet.ORDER[tNext.ordinalOf(tCurrent)][mRNG.nextInt(6)]) {
                if (tNext.orient(aXYZ, tFace) < 0) {
                    // 此界面方向点在外侧，选取此方向继续迭代
                    nFace = tFace;
                    break;
                }
            }
            // 更新当前考虑的四面体
            tCurrent = tNext;
            // 所有方向都在内部，表明 aXYZ 就在 tCurrent 四面体内部，此时 nFace == PosTet.NULL
        }
    }
    
    /** 用于外部访问的包含 voronoi 参数信息的节点，用于实时进行计算分析，减少内存占用 */
    public interface IVertex extends IXYZ {
        int coordination();
        double atomicVolume();
        double cavityRadius();
        int[] index();
        /** 其他可能有用信息 */
        int atomIndex();
        double x();
        double y();
        double z();
        @Unmodifiable Collection<IVertex> neighborVertex();
        @Unmodifiable Collection<ITetrahedron> neighborTetrahedron();
    }
    /** 用于外部访问的包含 voronoi 参数信息的四面体，用于实时进行计算分析，减少内存占用 */
    public interface ITetrahedron {
        IXYZ centerSphere();
        @Unmodifiable List<IVertex> neighborVertex();
        @Unmodifiable List<ITetrahedron> neighborTetrahedron();
        boolean valid();
    }
    
    /**
     * 获取位置节点，支持随机访问，节点按照添加顺序排列
     * @author CHanzy
     * @param aIdx 需要获取的节点索引
     * @return 包含 voronoi 多面体参数的节点
     */
    public IVertex getVertex(int aIdx) {return mAllVertex.get(aIdx);}
    public int sizeVertex() {return mAllVertex.size();}
    public @Unmodifiable List<IVertex> allVertex() {return AbstractCollections.map(mAllVertex, v->v);}
    /**
     * 获取一个四面体，不支持随机访问，会获取最近创建的四面体
     * @author CHanzy
     * @return 包含 voronoi 多面体参数的四面体
     */
    public ITetrahedron getTetrahedron() {return mLast;}
    public @Unmodifiable Collection<ITetrahedron> allTetrahedron() {return AbstractCollections.map(mAllTet, v->v);}
    @VisibleForTesting public ITetrahedron getTet() {return getTetrahedron();}
    @VisibleForTesting public @Unmodifiable Collection<ITetrahedron> allTet() {return allTetrahedron();}
    
    
    /** 带有统计信息的完整四面体，会自动更新统计信息 */
    class Tetrahedron extends AbstractTetrahedron {
        Tetrahedron(Vertex aA, Vertex aB, Vertex aC, Vertex aD) {
            super(aA, aB, aC, aD);
            mA.mAdj = this;
            mB.mAdj = this;
            mC.mAdj = this;
            mD.mAdj = this;
            mAllTet.add(this);
        }
        
        /** 此值用于验证是否需要更新统计信息 */
        private int oCheck = -1;
        /** 近邻信息 */
        final List<Vertex> mNeighborVertex = new ArrayList<>(4);
        final List<Tetrahedron> mNeighborTet = new ArrayList<>(4); // 这里会保留边界四面体保证近邻都会获取到
        private void updateStat_() {
            if (oCheck == mCheck) return;
            oCheck = mCheck;
            // 清空旧的数据
            mNeighborVertex.clear();
            mNeighborTet.clear();
            // 统计近邻的节点和四面体，只需要排除边界结构即可
            if (!isUniverse(mA)) mNeighborVertex.add(mA);
            if (!isUniverse(mB)) mNeighborVertex.add(mB);
            if (!isUniverse(mC)) mNeighborVertex.add(mC);
            if (!isUniverse(mD)) mNeighborVertex.add(mD);
            if (mTetA!=null) {mNeighborTet.add(mTetA);}
            if (mTetB!=null) {mNeighborTet.add(mTetB);}
            if (mTetC!=null) {mNeighborTet.add(mTetC);}
            if (mTetD!=null) {mNeighborTet.add(mTetD);}
        }
        
        private XYZ mCenterSphere = null;
        /** 返回此四面体外接球的球心，这个值是恒定的，但是不需要总是计算 */
        XYZ centerSphere_() {
            if (mCenterSphere == null) {
                if (!mNoWarning && isUniverse(this)) UT.Code.warning("This Tetrahedron is Universe, centerSphere may be wrong.");
                mCenterSphere = VoronoiStaticExtensions.centerSphere(null, mA.mXYZ, mB.mXYZ, mC.mXYZ, mD.mXYZ);
            }
            return mCenterSphere;
        }
        public IXYZ centerSphere() {
            final XYZ tCenterSphere = centerSphere_();
            return tCenterSphere==null ? null : new AbstractXYZ() {
                @Override public double x() {return tCenterSphere.mX;}
                @Override public double y() {return tCenterSphere.mY;}
                @Override public double z() {return tCenterSphere.mZ;}
            };
        }
        /** voronoi 统计信息，现在只有需要时才会进行统计 */
        @Override public @Unmodifiable List<IVertex> neighborVertex() {
            updateStat_();
            return AbstractCollections.map(mNeighborVertex, v->v);
        }
        @Override public @Unmodifiable List<ITetrahedron> neighborTetrahedron() {
            updateStat_();
            return AbstractCollections.map(mNeighborTet, t->t);
        }
    }
    
    /** 暂存节点信息类 */
    static class VertexInfo {
        int mTetNum;
        final double mArea, mDis;
        VertexInfo(int aTetNum, double aArea, double aDis) {mTetNum = aTetNum; mArea = aArea; mDis = aDis;}
    }
    
    /** 带有统计信息的完整节点，会自动更新统计信息 */
    class Vertex extends AbstractVertex {
        final int mAtomIndex;
        Vertex(XYZ aXYZ, Tetrahedron aAdj, int atomIndex) {super(aXYZ, aAdj); mAtomIndex = atomIndex;}
        Vertex(double aX, double aY, double aZ) {super(new XYZ(aX, aY, aZ), null); mAtomIndex = -1;}
        
        /** 此值用于验证是否需要更新统计信息 */
        private int oCheck = -1;
        /** 近邻信息 */
        final Map<Vertex, @Nullable VertexInfo> mNeighborVertex = new LinkedHashMap<>(); // <节点，对应 voronoi 面的信息>
        final Set<Tetrahedron> mNeighborTet = new LinkedHashSet<>(); // 这里会保留边界四面体保证近邻都会获取到
        
        private void updateStat_() {
            if (oCheck == mCheck) return;
            oCheck = mCheck;
            // 清空旧的数据
            mNeighborVertex.clear();
            mNeighborTet.clear();
            double tSurfaceArea = 0.0;
            // 缓存需要处理的四面体
            Deque<Tetrahedron> tStack = new ArrayDeque<>();
            tStack.addLast(mAdj);
            while (!tStack.isEmpty()) {
                // 获取一个近邻四面体，这样获取则为 DFS
                Tetrahedron tTet = tStack.removeLast();
                // 如果已经处理过则跳过
                if (mNeighborTet.contains(tTet)) continue;
                // 根据中心节点所在的位置来添加周围近邻以及节点
                switch (tTet.ordinalOf(this)) {
                case PosTet.A: {
                    // 先添加另外三个节点
                    if (!isUniverse(tTet.mB)) mNeighborVertex.put(tTet.mB, null);
                    if (!isUniverse(tTet.mC)) mNeighborVertex.put(tTet.mC, null);
                    if (!isUniverse(tTet.mD)) mNeighborVertex.put(tTet.mD, null);
                    // 再添加三个近邻面的四面体到缓存等待下一步处理，需要判断是否处理过以及是否为 null；这里需要保留巨大西面体因为还保存着合法点
                    if (tTet.mTetB!=null && !mNeighborTet.contains(tTet.mTetB)) tStack.addLast(tTet.mTetB);
                    if (tTet.mTetC!=null && !mNeighborTet.contains(tTet.mTetC)) tStack.addLast(tTet.mTetC);
                    if (tTet.mTetD!=null && !mNeighborTet.contains(tTet.mTetD)) tStack.addLast(tTet.mTetD);
                    break;
                }
                case PosTet.B: {
                    // 先添加另外三个节点
                    if (!isUniverse(tTet.mA)) mNeighborVertex.put(tTet.mA, null);
                    if (!isUniverse(tTet.mC)) mNeighborVertex.put(tTet.mC, null);
                    if (!isUniverse(tTet.mD)) mNeighborVertex.put(tTet.mD, null);
                    // 再添加三个近邻面的四面体到缓存等待下一步处理，需要判断是否处理过以及是否为 null；这里需要保留巨大西面体因为还保存着合法点
                    if (tTet.mTetA!=null && !mNeighborTet.contains(tTet.mTetA)) tStack.addLast(tTet.mTetA);
                    if (tTet.mTetC!=null && !mNeighborTet.contains(tTet.mTetC)) tStack.addLast(tTet.mTetC);
                    if (tTet.mTetD!=null && !mNeighborTet.contains(tTet.mTetD)) tStack.addLast(tTet.mTetD);
                    break;
                }
                case PosTet.C: {
                    // 先添加另外三个节点
                    if (!isUniverse(tTet.mB)) mNeighborVertex.put(tTet.mB, null);
                    if (!isUniverse(tTet.mA)) mNeighborVertex.put(tTet.mA, null);
                    if (!isUniverse(tTet.mD)) mNeighborVertex.put(tTet.mD, null);
                    // 再添加三个近邻面的四面体到缓存等待下一步处理，需要判断是否处理过以及是否为 null；这里需要保留巨大西面体因为还保存着合法点
                    if (tTet.mTetB!=null && !mNeighborTet.contains(tTet.mTetB)) tStack.addLast(tTet.mTetB);
                    if (tTet.mTetA!=null && !mNeighborTet.contains(tTet.mTetA)) tStack.addLast(tTet.mTetA);
                    if (tTet.mTetD!=null && !mNeighborTet.contains(tTet.mTetD)) tStack.addLast(tTet.mTetD);
                    break;
                }
                case PosTet.D: {
                    // 先添加另外三个节点
                    if (!isUniverse(tTet.mB)) mNeighborVertex.put(tTet.mB, null);
                    if (!isUniverse(tTet.mC)) mNeighborVertex.put(tTet.mC, null);
                    if (!isUniverse(tTet.mA)) mNeighborVertex.put(tTet.mA, null);
                    // 再添加三个近邻面的四面体到缓存等待下一步处理，需要判断是否处理过以及是否为 null；这里需要保留巨大西面体因为还保存着合法点
                    if (tTet.mTetB!=null && !mNeighborTet.contains(tTet.mTetB)) tStack.addLast(tTet.mTetB);
                    if (tTet.mTetC!=null && !mNeighborTet.contains(tTet.mTetC)) tStack.addLast(tTet.mTetC);
                    if (tTet.mTetA!=null && !mNeighborTet.contains(tTet.mTetA)) tStack.addLast(tTet.mTetA);
                    break;
                }
                default: throw new RuntimeException();
                }
                // 此四面体处理完成
                mNeighborTet.add(tTet);
            }
            // 根据每个近邻节点计算每个 voronoi 面的顶点数（共棱的四面体数目）和面积（过小要进行截断）
            for (Map.Entry<Vertex, @Nullable VertexInfo> tVertexEntry : mNeighborVertex.entrySet()) {
                Vertex tVertex = tVertexEntry.getKey();
                double tDis = mXYZ.distance(tVertex.mXYZ);
                // 这里直接遍历所有的近邻四面体来得到第一个共棱的四面体
                Tetrahedron tTet0 = null; XYZ tA = null;
                for (Tetrahedron tTet : mNeighborTet) if (!isUniverse(tTet)) {
                    if (tTet.containsVertex(tVertex)) {
                        tTet0 = tTet; tA = tTet.centerSphere_();
                        break;
                    }
                }
                // 非常奇异的情况，此棱全由边界四面体构成，直接跳过即可
                if (tTet0==null || tA==null) {
                    if (!mNoWarning) UT.Code.warning("Voronoi of this node is Incomplete, voronoi parameters may be wrong.");
                    continue;
                }
                // 绕棱方向计算面积并统计四面体数目
                int rTetNum = 1;
                double rArea = 0.0;
                // 绕棱获取下一个四面体
                Tetrahedron tTet2 = tTet0.getNeighbor(this, tVertex, null);
                XYZ tB = (tTet2!=null && !isUniverse(tTet2) && mNeighborTet.contains(tTet2)) ? tTet2.centerSphere_() : null;
                // 如果没有获取到（没有近邻，不包含在近邻中，边界四面体），则输出警告，结束环绕
                if (tB == null) {
                    if (!mNoWarning) UT.Code.warning("Voronoi of this node is Incomplete, voronoi parameters may be wrong.");
                    continue;
                }
                // 如果 AB 距离过小需要进行截断
                if (lengthValid(tA.distance(tB), tDis)) ++rTetNum;
                Tetrahedron tTet1 = tTet0;
                while (true) {
                    // 绕棱获取下一个四面体
                    Tetrahedron tTet3 = tTet2.getNeighbor(this, tVertex, tTet1);
                    XYZ tC = (tTet3!=null && !isUniverse(tTet3) && mNeighborTet.contains(tTet3)) ? tTet3.centerSphere_() : null;
                    // 如果没有获取到（没有近邻，不包含在近邻中，边界四面体），则输出警告，结束环绕
                    if (tC == null) {
                        if (!mNoWarning) UT.Code.warning("Voronoi of this node is Incomplete, voronoi parameters may be wrong.");
                        break;
                    }
                    // 如果为初始四面体同样结束环绕
                    if (tTet3 == tTet0) break;
                    // 成功获取到下一个四面体，更新数据；
                    // 如果 BC 距离过小需要进行截断
                    if (lengthValid(tB.distance(tC), tDis)) ++rTetNum;
                    rArea += MathEX.Graph.area(tA, tB, tC);
                    tB = tC;
                    tTet1 = tTet2;
                    tTet2 = tTet3;
                }
                // 统计完成，设置此节点的信息并累加总面积
                tSurfaceArea += rArea;
                tVertexEntry.setValue(new VertexInfo(rTetNum, rArea, tDis));
            }
            // 然后再遍历一次截断过小的面积
            if (areaCutoff()) for (@Nullable VertexInfo tInfo : mNeighborVertex.values()) if (tInfo != null) {
                // 直接将 mTetNum 设为 0 标记为界面被截断，保留 mArea 的值保证体积计算正确
                if (!areaValid(tInfo.mArea, tSurfaceArea)) {
                    tInfo.mTetNum = 0;
                }
            }
        }
        /** voronoi 统计信息，现在只有需要时才会进行统计 */
        @Override public int coordination() {
            updateStat_();
            int rCoordination = 0;
            // 在这里对面积较小的面进行截断，现在可以直接检测 mTetNum
            for (@Nullable VertexInfo tInfo : mNeighborVertex.values()) if (tInfo!=null && tInfo.mTetNum>=3) {
                ++rCoordination;
            }
            return rCoordination;
        }
        @Override public double atomicVolume() {
            updateStat_();
            double rAtomicVolume = 0.0;
            // 使用棱锥体积公式进行计算
            for (@Nullable VertexInfo tInfo : mNeighborVertex.values()) if (tInfo != null) {
                rAtomicVolume += tInfo.mArea * tInfo.mDis / 6.0;
            }
            return rAtomicVolume;
        }
        @Override public double cavityRadius() {
            updateStat_();
            double rCavityRadius = 0.0;
            for (Tetrahedron tTet : mNeighborTet) {
                rCavityRadius = Math.max(rCavityRadius, mXYZ.distance(tTet.centerSphere_()));
            }
            return rCavityRadius;
        }
        @Override public int[] index() {
            updateStat_();
            int[] rIndex = new int[mIndexLength];
            // 如果面积过小直接跳过这个点的统计，这里不考虑表面积截断带来的边长截断效应
            for (@Nullable VertexInfo tInfo : mNeighborVertex.values()) if (tInfo!=null && tInfo.mTetNum>=3) {
                int tIndex = tInfo.mTetNum;
                if (tIndex > mIndexLength) {
                    if (!mNoWarning) UT.Code.warning("Voronoi index out of boundary: "+tIndex);
                    tIndex = mIndexLength;
                }
                ++rIndex[tIndex-1];
            }
            return rIndex;
        }
        /** 其他可能有用信息 */
        @Override public int atomIndex() {return mAtomIndex;}
        @Override public double x() {return mXYZ.mX;}
        @Override public double y() {return mXYZ.mY;}
        @Override public double z() {return mXYZ.mZ;}
        @Override public @Unmodifiable Collection<IVertex> neighborVertex() {
            updateStat_();
            return AbstractCollections.map(mNeighborVertex.keySet(), v->v);
        }
        @Override public @Unmodifiable Collection<ITetrahedron> neighborTetrahedron() {
            updateStat_();
            return AbstractCollections.map(mNeighborTet, t->t);
        }
    }
}
