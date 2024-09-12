package jse.atom;

import jse.code.collection.AbstractCollections;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IIntIterator;
import jse.math.MathEX;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static jse.code.CS.ZL_STR;


public abstract class AbstractSettableAtomDataOperation extends AbstractAtomDataOperation implements ISettableAtomDataOperation {
    
    @Override public ISettableAtomData refSlice(ISlice aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    @Override public ISettableAtomData refSlice(List<Integer> aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    @Override public ISettableAtomData refSlice(int[] aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    @Override public ISettableAtomData refSlice(IIndexFilter aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().atoms(), aIndices));}
    
    
    @Override public void map2this(int aMinTypeNum, IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator) {
        final ISettableAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNumber();
        for (int i = 0; i < tAtomNum; ++i) {
            tThis.setAtom(i, aOperator.apply(tThis.atom(i)));
        }
        // 这里不进行 try 包含，因为手动指定了 aMinTypeNum 后才会调用，此时设置失败会希望抛出错误
        if (tThis.atomTypeNumber() < aMinTypeNum) tThis.setAtomTypeNumber(aMinTypeNum);
    }
    
    @Override public void mapType2this(int aMinTypeNum, IUnaryFullOperator<Integer, ? super IAtom> aOperator) {
        final ISettableAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNumber();
        for (int i = 0; i < tAtomNum; ++i) {
            // 保存修改后的原子，现在内部会自动更新种类计数
            ISettableAtom tAtom = tThis.atom(i);
            tAtom.setType(aOperator.apply(tAtom));
        }
        // 这里不进行 try 包含，因为手动指定了 aMinTypeNum 后才会调用，此时设置失败会希望抛出错误
        if (tThis.atomTypeNumber() < aMinTypeNum) tThis.setAtomTypeNumber(aMinTypeNum);
    }
    
    @Override public void mapTypeRandom2this(Random aRandom, IVector aTypeWeights) {
        int tAtomNum = thisAtomData_().atomNumber();
        int tMaxType = aTypeWeights.size();
        for (int i = 0; i < tMaxType; ++i) {
            if (aTypeWeights.get(i) < 0.0) throw new RuntimeException("TypeWeights Must be Positive");
        }
        double tTotWeight = aTypeWeights.sum();
        
        // 获得对应原子种类的 List
        double tRest = 0.0;
        final IntVector.Builder tBuilder = IntVector.builder(tAtomNum+tMaxType);
        for (int tType = 1; tType <= tMaxType; ++tType) {
            // 计算这种种类的粒子数目，超出部分随机处理
            double tLen = (aTypeWeights.get(tType-1) / tTotWeight) * tAtomNum + tRest;
            int tSteps = MathEX.Code.floor2int(tLen);
            if (aRandom.nextDouble() < tLen-tSteps) ++tSteps;
            tRest = tLen-tSteps;
            for (int i = 0; i < tSteps; ++i) tBuilder.add(tType);
        }
        // 简单处理，如果数量不够则添加最后一种种类；理论上应该不可能出现这个情况
        while (tBuilder.size() < tAtomNum) tBuilder.add(tMaxType);
        IIntVector tTypeList = tBuilder.build();
        // 随机打乱这些种类标记
        tTypeList.shuffle(aRandom);
        final IIntIterator it = tTypeList.iterator();
        // 使用 mapType2this 直接设置
        mapType2this(tMaxType, atom -> it.next());
    }
    
    @Override public void perturbXYZGaussian2this(Random aRandom, double aSigma) {
        final ISettableAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNumber();
        for (int i = 0; i < tAtomNum; ++i) {
            ISettableAtom tAtom = tThis.atom(i);
            tAtom.setXYZ(
                tAtom.x() + aRandom.nextGaussian()*aSigma,
                tAtom.y() + aRandom.nextGaussian()*aSigma,
                tAtom.z() + aRandom.nextGaussian()*aSigma
            );
        }
        // 注意周期边界条件的处理
        wrapPBC2this();
    }
    
    @Override public void wrapPBC2this() {
        final ISettableAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNumber();
        final IBox tBox = tThis.box();
        XYZ tBuf = new XYZ();
        for (int i = 0; i < tAtomNum; ++i) {
            ISettableAtom tAtom = tThis.atom(i);
            tBuf.setXYZ(tAtom);
            tBox.wrapPBC(tBuf);
            tAtom.setXYZ(tBuf);
        }
    }
    
    @Override public List<IntVector> clusterAnalyze(double aRCut, boolean aUnwrapByCluster2this) {
        return clusterAnalyze_(aRCut, aUnwrapByCluster2this, true);
    }
    @Override public void unwrapByCluster2this(double aRCut) {
        clusterAnalyze_(aRCut, true, false);
    }
    
    
    /** 用于方便内部使用 */
    private ISettableAtomData refAtomData_(List<? extends ISettableAtom> aAtoms) {
        ISettableAtomData tThis = thisAtomData_();
        @Nullable List<@Nullable String> tSymbols = tThis.symbols();
        return new SettableAtomData(aAtoms, tThis.atomTypeNumber(), tThis.box(), tThis.hasVelocity(), tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    
    /** stuff to override */
    protected abstract ISettableAtomData thisAtomData_();
}
