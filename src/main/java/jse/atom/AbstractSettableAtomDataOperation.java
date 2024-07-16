package jse.atom;

import jse.code.collection.AbstractCollections;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IIntIterator;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;

import java.util.List;
import java.util.Random;


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
        double tTotWeight = aTypeWeights.sum();
        if (tTotWeight <= 0.0) throw new RuntimeException("TypeWeights Must be Positive");
        
        int tAtomNum = thisAtomData_().atomNumber();
        int tMaxType = aTypeWeights.size();
        // 获得对应原子种类的 List
        final IntVector.Builder tBuilder = IntVector.builder(tAtomNum+tMaxType);
        for (int tType = 1; tType <= tMaxType; ++tType) {
            // 计算这种种类的粒子数目
            long tSteps = Math.round((aTypeWeights.get(tType-1) / tTotWeight) * tAtomNum);
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
        if (tThis.isPrism()) {
            // 斜方情况需要转为 Direct 再 wrap，
            // 完事后再转回 Cartesian
            final IBox tBox = tThis.box();
            XYZ tBuf = new XYZ();
            for (int i = 0; i < tAtomNum; ++i) {
                ISettableAtom tAtom = tThis.atom(i);
                tBuf.setXYZ(tAtom);
                tBox.toDirect(tBuf);
                if      (tBuf.mX <  0.0) {do {++tBuf.mX;} while (tBuf.mX <  0.0);}
                else if (tBuf.mX >= 1.0) {do {--tBuf.mX;} while (tBuf.mX >= 1.0);}
                if      (tBuf.mY <  0.0) {do {++tBuf.mY;} while (tBuf.mY <  0.0);}
                else if (tBuf.mY >= 1.0) {do {--tBuf.mY;} while (tBuf.mY >= 1.0);}
                if      (tBuf.mZ <  0.0) {do {++tBuf.mZ;} while (tBuf.mZ <  0.0);}
                else if (tBuf.mZ >= 1.0) {do {--tBuf.mZ;} while (tBuf.mZ >= 1.0);}
                tBox.toCartesian(tBuf);
                tAtom.setXYZ(tBuf);
            }
        } else {
            final XYZ tBox = XYZ.toXYZ(tThis.box());
            for (int i = 0; i < tAtomNum; ++i) {
                ISettableAtom tAtom = tThis.atom(i);
                double tX = tAtom.x();
                double tY = tAtom.y();
                double tZ = tAtom.z();
                if      (tX <  0.0    ) {do {tX += tBox.mX;} while (tX <  0.0    );}
                else if (tX >= tBox.mX) {do {tX -= tBox.mX;} while (tX >= tBox.mX);}
                if      (tY <  0.0    ) {do {tY += tBox.mY;} while (tY <  0.0    );}
                else if (tY >= tBox.mY) {do {tY -= tBox.mY;} while (tY >= tBox.mY);}
                if      (tZ <  0.0    ) {do {tZ += tBox.mZ;} while (tZ <  0.0    );}
                else if (tZ >= tBox.mZ) {do {tZ -= tBox.mZ;} while (tZ >= tBox.mZ);}
                tAtom.setXYZ(tX, tY, tZ);
            }
        }
    }
    
    
    /** 用于方便内部使用 */
    private ISettableAtomData refAtomData_(List<? extends ISettableAtom> aAtoms) {
        ISettableAtomData tThis = thisAtomData_();
        return new SettableAtomData(aAtoms, tThis.atomTypeNumber(), tThis.box(), tThis.hasVelocity());
    }
    
    /** stuff to override */
    protected abstract ISettableAtomData thisAtomData_();
}
