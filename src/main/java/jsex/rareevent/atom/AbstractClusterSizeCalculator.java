package jsex.rareevent.atom;

import jse.atom.IAtomData;
import jse.atom.MonatomicParameterCalculator;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.vector.IIntVector;
import jse.math.vector.ILogicalVector;
import jsex.rareevent.IParameterCalculator;

import java.util.List;

import static jse.code.CS.R_NEAREST_MUL;


/**
 * 抽象的参数计算器，计算体系中的最大的固体团簇的尺寸，可自定义内部使用的固体判断函数
 * <p>
 * 方便起见，统计所有固体数目的逻辑也包含在这个类里，通过重写 countAll 方法来切换逻辑
 * @author liqa
 */
public abstract class AbstractClusterSizeCalculator implements IParameterCalculator<IAtomData> {
    @Override public final double lambdaOf(IAtomData aPoint) {
        // 常量暂存
        final boolean tCountAll = countAll();
        final int tMinClusterSize = minClusterSize();
        // 还是使用 MPC 内部的方法来判断
        try (final MonatomicParameterCalculator tMPC = aPoint.getMonatomicParameterCalculator(nThreads())) {
            final ILogicalVector tIsSolid = getIsSolid_(tMPC, aPoint);
            if (tCountAll && tMinClusterSize<=1) {
                // 如果全部统计且最小团簇大小为 0 则直接求和统计数目即可
                return tIsSolid.count();
            } else {
                // 使用 getClustersDFS 获取所有的团簇（一般来说会比 BFS 更快，当然这个部分不是瓶颈）
                final double tRCluster = getRCluster_(tMPC);
                List<? extends IIntVector> tClusters = MathEX.Adv.getClustersDFS(tIsSolid.size(), AbstractCollections.filterInteger(tIsSolid.size(), tIsSolid), i -> AbstractCollections.filterInteger(tMPC.getNeighborList(i, tRCluster), tIsSolid));
                // 遍历团簇统计 lambda，区分 countAll() 和一般只统计最大的逻辑
                double rLambda = 0.0;
                double rMax = 0.0;
                for (IIntVector subCluster : tClusters) {
                    double tClusterSize = subCluster.size();
                    rMax = Math.max(rMax, tClusterSize);
                    if (tCountAll && tClusterSize >= tMinClusterSize) {
                        rLambda += tClusterSize;
                    }
                }
                return tCountAll ? Math.max(rLambda, rMax) : rMax;
            }
        }
    }
    
    /** stuff to override */
    protected int nThreads() {return 1;}
    protected boolean countAll() {return false;}
    protected int minClusterSize() {return 5;}
    protected double getRCluster_(MonatomicParameterCalculator aMPC) {return aMPC.unitLen()*R_NEAREST_MUL;}
    protected abstract ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint);
}
