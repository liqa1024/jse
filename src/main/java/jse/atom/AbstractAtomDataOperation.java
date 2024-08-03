package jse.atom;

import jse.cache.LogicalVectorCache;
import jse.code.CS;
import jse.code.collection.*;
import jse.code.functional.IFilter;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.math.MathEX;
import jse.math.vector.ILogicalVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jse.code.CS.ZL_STR;


/**
 * 一般的运算器的实现，默认会值拷贝一次并使用 {@code ArrayList<IAtom>} 来存储，尽管这会占据更多的内存
 * @author liqa
 */
public abstract class AbstractAtomDataOperation implements IAtomDataOperation {
    
    @Override public ISettableAtomData filter(IFilter<IAtom> aFilter) {
        IAtomData tThis = thisAtomData_();
        List<IAtom> tFilterAtoms = NewCollections.filter(tThis.atoms(), aFilter);
        ISettableAtomData rAtomData = newSettableAtomData_(tFilterAtoms.size());
        for (int i = 0; i < tFilterAtoms.size(); ++i) rAtomData.setAtom(i, tFilterAtoms.get(i));
        return rAtomData;
    }
    @Override public ISettableAtomData filterType(final int aType) {return filter(atom -> atom.type()==aType);}
    
    @Override public IAtomData refSlice(ISlice aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    @Override public IAtomData refSlice(List<Integer> aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    @Override public IAtomData refSlice(int[] aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    @Override public IAtomData refSlice(IIndexFilter aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    
    
    @Override public ISettableAtomData map(int aMinTypeNum, IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator) {
        final IAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNumber();
        ISettableAtomData rAtomData = newSettableAtomData_(tAtomNum);
        for (int i = 0; i < tAtomNum; ++i) {
            // 保存修改后的原子，现在内部会自动更新种类计数
            rAtomData.setAtom(i, aOperator.apply(tThis.atom(i)));
        }
        // 这里不进行 try 包含，因为目前这里的实例都是支持的，并且手动指定了 aMinTypeNum 后才会调用，此时设置失败会希望抛出错误
        if (rAtomData.atomTypeNumber() < aMinTypeNum) rAtomData.setAtomTypeNumber(aMinTypeNum);
        return rAtomData;
    }
    
    
    @Override public ISettableAtomData mapType(int aMinTypeNum, IUnaryFullOperator<Integer, ? super IAtom> aOperator) {
        ISettableAtomData rAtomData = newSameSettableAtomData_();
        rAtomData.operation().mapType2this(aMinTypeNum, aOperator);
        return rAtomData;
    }
    
    @Override public ISettableAtomData mapTypeRandom(Random aRandom, IVector aTypeWeights) {
        ISettableAtomData rAtomData = newSameSettableAtomData_();
        rAtomData.operation().mapTypeRandom2this(aRandom, aTypeWeights);
        return rAtomData;
    }
    
    @Override public ISettableAtomData perturbXYZGaussian(Random aRandom, double aSigma) {
        ISettableAtomData rAtomData = newSameSettableAtomData_();
        rAtomData.operation().perturbXYZGaussian2this(aRandom, aSigma);
        return rAtomData;
    }
    
    @Override public ISettableAtomData wrapPBC() {
        ISettableAtomData rAtomData = newSameSettableAtomData_();
        rAtomData.operation().wrapPBC2this();
        return rAtomData;
    }
    
    @Override public ISettableAtomData repeat(int aNx, int aNy, int aNz) {
        final IAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNumber();
        final IBox tBox = tThis.box();
        final double tAx = tBox.ax(), tAy = tBox.ay(), tAz = tBox.az();
        final double tBx = tBox.bx(), tBy = tBox.by(), tBz = tBox.bz();
        final double tCx = tBox.cx(), tCy = tBox.cy(), tCz = tBox.cz();
        ISettableAtomData rAtomData = newSettableAtomData_(
            tAtomNum*aNx*aNy*aNz,
            tThis.isPrism() ? new BoxPrism(
                tAx*aNx, tAy*aNx, tAz*aNx,
                tBx*aNy, tBy*aNy, tBz*aNy,
                tCx*aNz, tCy*aNz, tCz*aNz
            ) : new Box(tAx*aNx, tBy*aNy, tCz*aNz)
        );
        for (int idx = 0; idx < tAtomNum; ++idx) {
            IAtom tAtom = tThis.atom(idx);
            double tX  = tAtom.x() , tY  = tAtom.y() , tZ  = tAtom.z() ;
            double tVx = tAtom.vx(), tVy = tAtom.vy(), tVz = tAtom.vz();
            int tID = tAtom.id(); int tType = tAtom.type();
            if (tThis.isPrism()) {
                for (int i = 0; i < aNx; ++i) {
                double tDirAX = tAx*i, tDirAY = tAy*i, tDirAZ = tAz*i;
                for (int j = 0; j < aNy; ++j) {
                double tDirBX = tBx*j, tDirBY = tBy*j, tDirBZ = tBz*j;
                for (int k = 0; k < aNz; ++k) {
                    int tShift = (i + j*aNx + k*aNx*aNy) * tAtomNum;
                    ISettableAtom rAtom = rAtomData.atom(tShift + idx);
                    rAtom.setXYZ(
                        tX + tDirAX + tDirBX + tCx*k,
                        tY + tDirAY + tDirBY + tCy*k,
                        tZ + tDirAZ + tDirBZ + tCz*k
                    ).setID(tShift + tID).setType(tType);
                    if (tAtom.hasVelocity()) rAtom.setVxyz(tVx, tVy, tVz);
                }}}
            } else {
                for (int i = 0; i < aNx; ++i) {
                double tDirX = tAx*i;
                for (int j = 0; j < aNy; ++j) {
                double tDirY = tBy*j;
                for (int k = 0; k < aNz; ++k) {
                    int tShift = (i + j*aNx + k*aNx*aNy) * tAtomNum;
                    ISettableAtom rAtom = rAtomData.atom(tShift + idx);
                    rAtom.setXYZ(
                        tX + tDirX,
                        tY + tDirY,
                        tZ + tCz*k
                    ).setID(tShift + tID).setType(tType);
                    if (tAtom.hasVelocity()) rAtom.setVxyz(tVx, tVy, tVz);
                }}}
            }
        }
        return rAtomData;
    }
    
    @Override public List<? extends ISettableAtomData> slice(int aNx, int aNy, int aNz) {
        final IAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNumber();
        final IBox tBox = tThis.box();
        final IBox rBox = tThis.isPrism() ? new BoxPrism(
            tBox.ax()/aNx, tBox.ay()/aNx, tBox.az()/aNx,
            tBox.bx()/aNy, tBox.by()/aNy, tBox.bz()/aNy,
            tBox.cx()/aNz, tBox.cy()/aNz, tBox.cz()/aNz
        ) : new Box(tBox.x()/aNx, tBox.y()/aNy, tBox.z()/aNz);
        // 先遍历添加对应的 idx，然后根据此来创建 ISettableAtomData
        int tSliceNum = aNx*aNy*aNz;
        List<IntList> rIndices = NewCollections.from(tSliceNum, i -> new IntList());
        XYZ tBuf = new XYZ();
        for (int idx = 0; idx < tAtomNum; ++idx) {
            tBuf.setXYZ(tThis.atom(idx));
            rBox.toDirect(tBuf);
            int i = MathEX.Code.floor2int(tBuf.mX); if (i<0 || i>=aNx) continue;
            int j = MathEX.Code.floor2int(tBuf.mY); if (j<0 || j>=aNy) continue;
            int k = MathEX.Code.floor2int(tBuf.mZ); if (k<0 || k>=aNz) continue;
            rIndices.get(i + j*aNx + k*aNx*aNy).add(idx);
        }
        ISettableAtomData[] rSlice = new ISettableAtomData[tSliceNum];
        for (int i = 0; i < tSliceNum; ++i) {
            IntList subIndices = rIndices.get(i);
            ISettableAtomData subAtomData = newSettableAtomData_(rIndices.get(i).size(), rBox.copy());
            int subAtomNum = subAtomData.atomNumber();
            for (int j = 0; j < subAtomNum; ++j) {
                ISettableAtom subAtom = subAtomData.atom(j);
                IAtom tAtom = tThis.atom(subIndices.get(j));
                if (tThis.isPrism()) {
                    tBuf.setXYZ(tAtom);
                    rBox.toDirect(tBuf);
                    tBuf.mX -= MathEX.Code.floor(tBuf.mX);
                    tBuf.mY -= MathEX.Code.floor(tBuf.mY);
                    tBuf.mZ -= MathEX.Code.floor(tBuf.mZ);
                    rBox.toCartesian(tBuf);
                    subAtom.setXYZ(tBuf).setID(tAtom.id()).setType(tAtom.type());
                } else {
                    subAtom.setXYZ(
                        tAtom.x() % rBox.x(),
                        tAtom.y() % rBox.y(),
                        tAtom.z() % rBox.z()
                    ).setID(tAtom.id()).setType(tAtom.type());
                }
                if (tAtom.hasVelocity()) subAtom.setVxyz(tAtom.vx(), tAtom.vy(), tAtom.vz());
            }
            rSlice[i] = subAtomData;
        }
        return NewCollections.from(rSlice);
    }
    
    
    protected List<IntVector> clusterAnalyze_(double aRCut, final boolean aUnwrapByCluster2this, boolean aOutputIndex) {
        final IAtomData tThis = thisAtomData_();
        assert !aUnwrapByCluster2this || (tThis instanceof ISettableAtomData);
        
        final int tAtomNum = tThis.atomNumber();
        // 使用 mpc 来获取近邻列表
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(tThis)) {
            final List<IntVector> rClusters = aOutputIndex ? new ArrayList<>() : null;
            final ILogicalVector tVisited = LogicalVectorCache.getZeros(tAtomNum);
            // 采用深度有限搜索算法来获取团簇
            final IntDeque tStack = new IntDeque();
            final XYZ tBuf = new XYZ();
            for (int point = 0; point < tAtomNum; ++point) if (!tVisited.get(point)) {
                IntVector.Builder subCluster = aOutputIndex ? IntVector.builder() : null;
//              tStack.clear(); // 由于后面会遍历移除，因此此时 tStack 永远为空
                
                tStack.push(point);
                tVisited.set(point, true);
                
                while (!tStack.isEmpty()) {
                    int currentPoint = tStack.pop();
                    if (aOutputIndex) subCluster.add(currentPoint);
                    
                    if (aUnwrapByCluster2this) tBuf.setXYZ(tThis.atom(currentPoint));
                    tMPC.nl_().forEachNeighbor(currentPoint, aRCut, (x, y, z, neighbor, dx, dy, dz) -> {
                        if (!tVisited.get(neighbor)) {
                            tStack.push(neighbor);
                            tVisited.set(neighbor, true);
                            if (aUnwrapByCluster2this) {
                                ((ISettableAtomData)tThis).atom(neighbor).setXYZ(tBuf.mX+dx, tBuf.mY+dy, tBuf.mZ+dz);
                            }
                        }
                    });
                }
                if (aOutputIndex) rClusters.add(subCluster.build());
            }
            LogicalVectorCache.returnVec(tVisited);
            return rClusters;
        }
    }
    
    @Override public List<IntVector> clusterAnalyze(double aRCut) {
        return clusterAnalyze_(aRCut, false, true);
    }
    @Override public ISettableAtomData unwrapByCluster(double aRCut) {
        ISettableAtomData rAtomData = newSameSettableAtomData_();
        rAtomData.operation().unwrapByCluster2this(aRCut);
        return rAtomData;
    }
    
    /** 用于方便内部使用 */
    private IAtomData refAtomData_(List<? extends IAtom> aAtoms) {
        IAtomData tThis = thisAtomData_();
        @Nullable List<@Nullable String> tSymbols = tThis.symbols();
        return new AtomData(aAtoms, tThis.atomTypeNumber(), tThis.box(), tThis.hasVelocity(), tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    
    /** stuff to override */
    protected abstract IAtomData thisAtomData_();
    protected abstract ISettableAtomData newSameSettableAtomData_();
    protected abstract ISettableAtomData newSettableAtomData_(int aAtomNum);
    protected abstract ISettableAtomData newSettableAtomData_(int aAtomNum, IBox aBox);
}
