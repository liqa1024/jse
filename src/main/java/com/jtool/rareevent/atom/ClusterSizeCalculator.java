package com.jtool.rareevent.atom;

import com.jtool.atom.IAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.math.MathEX;
import com.jtool.math.vector.ILogicalVector;
import com.jtool.rareevent.IParameterCalculator;

import java.util.List;

import static com.jtool.code.CS.R_NEAREST_MUL;


/**
 * 一种参数计算机，计算体系中的最大的固体团簇的尺寸
 * @author liqa
 */
public class ClusterSizeCalculator implements IParameterCalculator<IAtomData> {
    private final double mRNearestMul;
    public ClusterSizeCalculator(double aRNearestMul) {mRNearestMul = aRNearestMul;}
    public ClusterSizeCalculator() {this(R_NEAREST_MUL);}
    
    @Override public double lambdaOf(IAtomData aPoint) {
        // 进行类固体判断
        try (final MonatomicParameterCalculator tMPC = aPoint.getMPC()) {
            final double tRNearest = tMPC.unitLen()*mRNearestMul;
            final ILogicalVector tIsSolid = tMPC.checkSolidQ6(tRNearest);
            // 使用 getClustersBFS 获取所有的团簇
            List<List<Integer>> tClusters = MathEX.Adv.getClustersBFS(tIsSolid.filter(tIsSolid.size()),i -> tIsSolid.filter(tMPC.getNeighborList(i, tRNearest)));
            // 遍历团簇统计 lambda
            double rLambda = 0.0;
            for (List<Integer> subCluster : tClusters) {
                rLambda = Math.max(rLambda, subCluster.size());
            }
            return rLambda;
        }
    }
}
