package com.jtool.rareevent.atom;

import com.jtool.atom.IAtom;
import com.jtool.atom.IAtomData;
import com.jtool.atom.MonatomicParameterCalculator;
import com.jtool.code.collection.NewCollections;
import com.jtool.math.MathEX;
import com.jtool.math.vector.ILogicalVector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * 一种参数计算器，计算体系中的最大的固体团簇的尺寸
 * <p>
 * 除了会考虑整体，也会只考虑其他种类的来进行判断
 * @author liqa
 */
public class MultiTypeClusterSizeCalculator extends AbstractClusterSizeCalculator {
    private final static double DEFAULT_TYPE_CAL_THRESHOLD = 0.15;
    
    private final ISolidChecker mAllSolidChecker;
    private final ISolidChecker @Nullable[] mTypeSolidCheckers;
    private double mTypeCalThreshold;
    
    /**
     * 构造一个 MultiTypeClusterSizeCalculator
     * @author liqa
     * @param aAllSolidChecker 适用于所有原子的 Checker，传入的 MPC 为所有原子的体系
     * @param aTypeSolidCheckers 适用于某个种类的 Checker，传入的 MPC 为选定的种类的原子的体系；对给定种类位置设为 null 则不会考虑这种原子，直接传入 null 则统一采用 aAllSolidChecker 计算
     */
    public MultiTypeClusterSizeCalculator(ISolidChecker aAllSolidChecker, ISolidChecker @Nullable[] aTypeSolidCheckers) {
        mAllSolidChecker = aAllSolidChecker;
        mTypeSolidCheckers = aTypeSolidCheckers==null || aTypeSolidCheckers.length==0 ? null : aTypeSolidCheckers;
        mTypeCalThreshold = DEFAULT_TYPE_CAL_THRESHOLD;
    }
    public MultiTypeClusterSizeCalculator(ISolidChecker aAllSolidChecker) {this(aAllSolidChecker, (ISolidChecker[])null);}
    /** 兼容 Groovy 的输入 */
    public MultiTypeClusterSizeCalculator(ISolidChecker aAllSolidChecker, Collection<? extends ISolidChecker> aTypeSolidCheckers) {this(aAllSolidChecker, aTypeSolidCheckers.toArray(new ISolidChecker[0]));}
    
    public MultiTypeClusterSizeCalculator setTypeCalThreshold(double aTypeCalThreshold) {mTypeCalThreshold = MathEX.Code.toRange(0.0, 1.0, aTypeCalThreshold); return this;}
    
    
    @Override protected ILogicalVector getIsSolid_(MonatomicParameterCalculator aMPC, IAtomData aPoint) {
        // 常量暂存
        final int tTypeNum = aPoint.atomTypeNum();
        final int tAtomNum = aPoint.atomNum();
        final List<IAtom> tAtoms = aPoint.atoms();
        // 先判断所有的
        ILogicalVector rIsSolid = mAllSolidChecker.checkSolid(aMPC);
        // 手动遍历过滤
        List<List<Integer>> tTypeIndices = NewCollections.from(tTypeNum, i -> new ArrayList<>());
        for (int idx = 0; idx < tAtomNum; ++idx) {
            tTypeIndices.get(tAtoms.get(idx).type()-1).add(idx);
        }
        // 再判断某个种类的
        int tMinCalNum = (int)Math.ceil(tAtomNum * mTypeCalThreshold);
        for (int tTypeMM = 0; tTypeMM < tTypeNum; ++tTypeMM) {
            ISolidChecker subTypeSolidChecker = mTypeSolidCheckers==null ? mAllSolidChecker : mTypeSolidCheckers[tTypeMM];
            if (subTypeSolidChecker!=null && tTypeIndices.get(tTypeMM).size()>=tMinCalNum) {
                try (MonatomicParameterCalculator tMPC = aPoint.operation().refSlice(tTypeIndices.get(tTypeMM)).getMonatomicParameterCalculator()) {
                    ILogicalVector tTypeIsSolid = subTypeSolidChecker.checkSolid(tMPC);
                    // 使用 refSlicer 来合并两者结果
                    rIsSolid.refSlicer().get(tTypeIndices.get(tTypeMM)).or2this(tTypeIsSolid);
                    // 周围中有一半的为 solid 则也要设为 solid
                    for (int idx = 0; idx < tAtomNum; ++idx) if (!rIsSolid.get(idx) && tAtoms.get(idx).type()!=tTypeMM+1) {
                        List<Integer> tNL = tMPC.getNeighborList(tAtoms.get(idx));
                        int rTypeSolidNum = 0;
                        for (int i : tNL) if (tTypeIsSolid.get(i)) ++rTypeSolidNum;
                        if (rTypeSolidNum!=0 && rTypeSolidNum+rTypeSolidNum>=tNL.size()) rIsSolid.set(idx, true);
                    }
                }
            }
        }
        return rIsSolid;
    }
}
