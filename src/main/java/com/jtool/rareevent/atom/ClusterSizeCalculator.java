package com.jtool.rareevent.atom;

import com.jtool.atom.IAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.math.MathEX;
import com.jtool.math.vector.ILogicalVector;
import com.jtool.rareevent.IParameterCalculator;

import java.util.List;

import static com.jtool.code.CS.R_NEAREST_MUL;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * @author liqa
 */
public final class ClusterSizeCalculator extends AbstractClusterSizeCalculator {
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
    
    @Override protected double getRClusterMul_() {return mRClusterMul;}
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {return aMPC.checkSolidQ6(aMPC.unitLen()*mQ6CutoffMul, mNnn);}
}
