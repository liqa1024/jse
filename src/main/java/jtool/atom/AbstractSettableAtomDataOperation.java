package jtool.atom;

import jtool.code.collection.AbstractCollections;
import jtool.code.collection.ISlice;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.IUnaryFullOperator;
import jtool.code.iterator.IIntIterator;
import jtool.math.vector.IIntVector;
import jtool.math.vector.IVector;
import jtool.math.vector.IntVector;

import java.util.List;
import java.util.Random;


public abstract class AbstractSettableAtomDataOperation extends AbstractAtomDataOperation implements ISettableAtomDataOperation {
    
    @Override public ISettableAtomData refSlice(ISlice aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().asList(), aIndices));}
    @Override public ISettableAtomData refSlice(List<Integer> aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().asList(), aIndices));}
    @Override public ISettableAtomData refSlice(int[] aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().asList(), aIndices));}
    @Override public ISettableAtomData refSlice(IIndexFilter aIndices) {return refAtomData_(AbstractCollections.slice(thisAtomData_().asList(), aIndices));}
    
    
    @Override public void map2this(int aMinTypeNum, IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator) {
        final ISettableAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNum();
        for (int i = 0; i < tAtomNum; ++i) {
            tThis.setAtom(i, aOperator.apply(tThis.pickAtom(i)));
        }
        // 这里不进行 try 包含，因为手动指定了 aMinTypeNum 后才会调用，此时设置失败会希望抛出错误
        if (tThis.atomTypeNum() < aMinTypeNum) tThis.setAtomTypeNum(aMinTypeNum);
    }
    
    @Override public void mapType2this(int aMinTypeNum, IUnaryFullOperator<Integer, ? super IAtom> aOperator) {
        final ISettableAtomData tThis = thisAtomData_();
        final int tAtomNum = tThis.atomNum();
        for (int i = 0; i < tAtomNum; ++i) {
            // 保存修改后的原子，现在内部会自动更新种类计数
            ISettableAtom tAtom = tThis.pickAtom(i);
            tAtom.setType(aOperator.apply(tAtom));
        }
        // 这里不进行 try 包含，因为手动指定了 aMinTypeNum 后才会调用，此时设置失败会希望抛出错误
        if (tThis.atomTypeNum() < aMinTypeNum) tThis.setAtomTypeNum(aMinTypeNum);
    }
    
    @Override public void mapTypeRandom2this(Random aRandom, IVector aTypeWeights) {
        double tTotWeight = aTypeWeights.sum();
        if (tTotWeight <= 0.0) throw new RuntimeException("TypeWeights Must be Positive");
        
        int tAtomNum = thisAtomData_().atomNum();
        int tMaxType = aTypeWeights.size();
        // 获得对应原子种类的 List
        final IntVector.Builder tBuilder = IntVector.builder(tAtomNum+tMaxType);
        for (int tType = 1; tType <= tMaxType; ++tType) {
            // 计算这种种类的粒子数目
            long tSteps = Math.round((aTypeWeights.get_(tType-1) / tTotWeight) * tAtomNum);
            for (int i = 0; i < tSteps; ++i) tBuilder.add(tType);
        }
        // 简单处理，如果数量不够则添加最后一种种类
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
        final int tAtomNum = tThis.atomNum();
        for (int i = 0; i < tAtomNum; ++i) {
            ISettableAtom tAtom = tThis.pickAtom(i);
            tAtom.setX(tAtom.x() + aRandom.nextGaussian()*aSigma)
                 .setY(tAtom.y() + aRandom.nextGaussian()*aSigma)
                 .setZ(tAtom.z() + aRandom.nextGaussian()*aSigma);
        }
        // 注意周期边界条件的处理
        wrapPBC2this();
    }
    
    @Override public void wrapPBC2this() {
        final ISettableAtomData tThis = thisAtomData_();
        final XYZ tBox = XYZ.toXYZ(tThis.box());
        final int tAtomNum = tThis.atomNum();
        for (int i = 0; i < tAtomNum; ++i) {
            ISettableAtom tAtom = tThis.pickAtom(i);
            double tX = tAtom.x();
            double tY = tAtom.y();
            double tZ = tAtom.z();
            if      (tX <  0.0    ) {tX += tBox.mX; while (tX <  0.0    ) tX += tBox.mX;}
            else if (tX >= tBox.mX) {tX -= tBox.mX; while (tX >= tBox.mX) tX -= tBox.mX;}
            if      (tY <  0.0    ) {tY += tBox.mY; while (tY <  0.0    ) tY += tBox.mY;}
            else if (tY >= tBox.mY) {tY -= tBox.mY; while (tY >= tBox.mY) tY -= tBox.mY;}
            if      (tZ <  0.0    ) {tZ += tBox.mZ; while (tZ <  0.0    ) tZ += tBox.mZ;}
            else if (tZ >= tBox.mZ) {tZ -= tBox.mZ; while (tZ >= tBox.mZ) tZ -= tBox.mZ;}
            tAtom.setX(tX).setY(tY).setZ(tZ);
        }
    }
    
    
    /** 用于方便内部使用 */
    private ISettableAtomData refAtomData_(List<? extends ISettableAtom> aAtoms) {
        ISettableAtomData tThis = thisAtomData_();
        return new SettableAtomData(aAtoms, tThis.atomTypeNum(), tThis.box(), tThis.hasVelocities());
    }
    
    /** stuff to override */
    protected abstract ISettableAtomData thisAtomData_();
}
