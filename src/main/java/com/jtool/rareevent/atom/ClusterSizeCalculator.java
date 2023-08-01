package com.jtool.rareevent.atom;

import com.jtool.atom.IAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.code.filter.IFilter;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.math.MathEX;
import com.jtool.math.vector.ILogicalVector;
import com.jtool.rareevent.IParameterCalculator;

import java.util.List;


/**
 * 一种参数计算机，计算体系中的最大的固体团簇的尺寸
 * @author liqa
 */
public class ClusterSizeCalculator implements IParameterCalculator<IAtomData> {
    public ClusterSizeCalculator() {}
    
    @Override public double lambdaOf(IAtomData aPoint) {
        // 进行类固体判断
        try (final MonatomicParameterCalculator tMPC = aPoint.getMPC()) {
            final ILogicalVector tIsSolid = tMPC.checkSolidQ6();
            // 使用 getClustersBFS 获取所有的团簇
            List<List<Integer>> tClusters = MathEX.Adv.getClustersBFS(IIndexFilter.filter(tIsSolid.size(), tIsSolid), i -> IFilter.filter(tMPC.getNeighborList(i), tIsSolid::get_));
            // 遍历团簇统计 lambda
            double rLambda = 0.0;
            for (List<Integer> subCluster : tClusters) {
                rLambda = Math.max(rLambda, subCluster.size());
            }
            return rLambda;
        }
    }
}
