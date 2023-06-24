package com.jtool.rareevent.atom;

import com.jtool.atom.IHasAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.atom.MonatomicParameterCalculator.INeighborListGetter;
import com.jtool.code.UT;
import com.jtool.code.collection.Pair;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.math.MathEX;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.vector.IVector;
import com.jtool.rareevent.IParameterCalculator;

import java.util.ArrayList;
import java.util.List;


/**
 * 一种参数计算机，计算体系中的最大的固体团簇的尺寸
 * @author liqa
 */
public class ClusterSizeCalculator implements IParameterCalculator<IHasAtomData> {
    public ClusterSizeCalculator() {}
    
    @Override public double lambdaOf(IHasAtomData aPoint) {
        // 进行类固体判断以及获取顺便产生的近邻列表，因此需要使用较为内部的接口
        IVector tIsSolid;
        final INeighborListGetter tNeighborList;
        try (MonatomicParameterCalculator tMPC = aPoint.getMPC()) {
            Pair<Pair<IMatrix, IMatrix>, INeighborListGetter> tPair = tMPC.calYlmMeanAndGetNeighborList(6);
            tIsSolid = tMPC.checkSolidYlmMean(tPair.first, tPair.second);
            tNeighborList = tPair.second;
        }
        // 获取所有需要考虑的原子列表
        List<Integer> rSolidList = new ArrayList<>();
        IDoubleIterator it = tIsSolid.iterator();
        int tIdx = 0;
        while (it.hasNext()) {
            if (it.next() == 1.0) rSolidList.add(tIdx);
            ++tIdx;
        }
        // 如果没有需要考虑的则结果为 0.0
        if (rSolidList.isEmpty()) return 0.0;
        // 使用 getClustersBFS 获取所有的团簇
        List<List<Integer>> tClusters = MathEX.Adv.getClustersBFS(rSolidList, i -> UT.Code.filter(tNeighborList.get(i), j -> tIsSolid.get_(j)==1));
        
        // 遍历团簇统计 lambda
        double rLambda = 0.0;
        for (List<Integer> subCluster : tClusters) {
            rLambda = Math.max(rLambda, subCluster.size());
        }
        return rLambda;
    }
}
