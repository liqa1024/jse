package com.jtool.rareevent.atom;

import com.jtool.atom.IAtom;
import com.jtool.atom.IAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.math.vector.ILogicalVector;

import java.util.ArrayList;
import java.util.List;

import static com.jtool.code.CS.R_NEAREST_MUL;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * <p>
 * 处理有多种成分的合金的团簇计算，对于复杂的合金效果更好，当然会有更高的计算开销（2 ~ 3 倍）
 * @author liqa
 */
public final class MultiTypeClusterSizeCalculator extends AbstractClusterSizeCalculator {
    private final static int TYPE_CAL_THRESHOLD = 100;
    
    private final double mQ6CutoffMul, mRClusterMul;
    private final int mNnn;
    
    /**
     * 构造一个团簇大小计算器，使用
     * {@link MonatomicParameterCalculator#calConnectCountABOOP}
     * 来判断是否是团簇
     * @param aQ6CutoffMul 计算 Q6 所使用的截断半径倍率，默认为 1.5
     * @param aNnn 计算 Q6 所使用的最近邻数目限制，默认不做限制
     * @param aRClusterMul 判断是否两原子是团簇的距离倍率，如果只传一个参数则与 aQ6CutoffMul 相同，否则默认为 1.5
     */
    public MultiTypeClusterSizeCalculator(double aQ6CutoffMul, int aNnn, double aRClusterMul) {mQ6CutoffMul = aQ6CutoffMul; mNnn = aNnn; mRClusterMul = aRClusterMul;}
    public MultiTypeClusterSizeCalculator(double aQ6CutoffMul, int aNnn) {this(aQ6CutoffMul, aNnn, R_NEAREST_MUL);}
    public MultiTypeClusterSizeCalculator(double aRNearestMul) {this(aRNearestMul, -1, aRNearestMul);}
    public MultiTypeClusterSizeCalculator() {this(R_NEAREST_MUL);}
    
    @Override protected double getRClusterMul_() {return mRClusterMul;}
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {
        // 先计算整体的
        ILogicalVector rIsSolid = aMPC.calConnectCountABOOP(6, aMPC.unitLen()*mQ6CutoffMul, mNnn, 0.83).greaterOrEqual(7);
        // 再计算每种种类的，这里手动遍历过滤
        int tTypeNum = aPoint.atomTypeNum();
        List<List<Integer>> tTypeIndices = new ArrayList<>(tTypeNum);
        for (int i = 0; i < tTypeNum; ++i) tTypeIndices.add(new ArrayList<>());
        int tIdx = 0;
        for (IAtom tAtom : aPoint.atoms()) {
            tTypeIndices.get(tAtom.type()-1).add(tIdx);
            ++tIdx;
        }
        // 需要这种种类的原子数超过指定阈值才去计算
        for (List<Integer> tIndices : tTypeIndices) if (tIndices.size() >= TYPE_CAL_THRESHOLD) {
            try (MonatomicParameterCalculator tMPC = aPoint.operation().filterIndices(tIndices).getMonatomicParameterCalculator()) {
                ILogicalVector tIsSolid = tMPC.calConnectCountABOOP(6, tMPC.unitLen()*mQ6CutoffMul, mNnn, 0.83).greaterOrEqual(7);
                // 使用 refSlicer 来合并不同种类的
                rIsSolid.refSlicer().get(tIndices).or2this(tIsSolid);
            }
        }
        return rIsSolid;
    }
}
