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
    private final double mQ6CutoffMul, mRClusterMul;
    private final int mNnn;
    
    /**
     * 构造一个团簇大小计算器，使用
     * {@link MonatomicParameterCalculator#checkSolidQ6(double aQ6Cutoff, int aNnn)}
     * 来判断是否是团簇
     * @param aQ6CutoffMul 计算 Q6 所使用的截断半径倍率，默认为 1.5
     * @param aNnn 计算 Q6 所使用的最近邻数目限制，默认不做限制
     * @param aRClusterMul 判断是否两原子是团簇的距离倍率，如果只传一个参数则与 aQ6CutoffMul 相同，否则默认为 1.5
     */
    public ClusterSizeCalculator(double aQ6CutoffMul, int aNnn, double aRClusterMul) {mQ6CutoffMul = aQ6CutoffMul; mNnn = aNnn; mRClusterMul = aRClusterMul;}
    public ClusterSizeCalculator(double aQ6CutoffMul, int aNnn) {this(aQ6CutoffMul, aNnn, R_NEAREST_MUL);}
    public ClusterSizeCalculator(double aRNearestMul) {this(aRNearestMul, -1, aRNearestMul);}
    public ClusterSizeCalculator() {this(R_NEAREST_MUL);}
    
    @Override public double lambdaOf(IAtomData aPoint) {
        // 进行类固体判断
        try (final MonatomicParameterCalculator tMPC = aPoint.getMonatomicParameterCalculator()) {
            final ILogicalVector tIsSolid = tMPC.checkSolidQ6(tMPC.unitLen()*mQ6CutoffMul, mNnn);
            // 使用 getClustersBFS 获取所有的团簇
            final double tRCluster = tMPC.unitLen()*mRClusterMul;
            List<List<Integer>> tClusters = MathEX.Adv.getClustersBFS(tIsSolid.filter(tIsSolid.size()),i -> tIsSolid.filter(tMPC.getNeighborList(i, tRCluster)));
            // 遍历团簇统计 lambda
            double rLambda = 0.0;
            for (List<Integer> subCluster : tClusters) {
                rLambda = Math.max(rLambda, subCluster.size());
            }
            return rLambda;
        }
    }
}
