package com.jtool.atom;

import com.jtool.math.vector.IVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.jtool.code.UT.Code.toXYZ;


/**
 * 一般的运算器的实现，值拷贝一次并使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存
 * @author liqa
 */
public abstract class AbstractAtomDataOperation implements IAtomDataOperation {
    
    protected final static class AtomSetter implements IAtomSetter {
        private final Atom mAtom;
        public AtomSetter(Atom rAtom) {mAtom = rAtom;}
        @Override public IAtomSetter setX(double aX) {mAtom.mX = aX; return this;}
        @Override public IAtomSetter setY(double aY) {mAtom.mY = aY; return this;}
        @Override public IAtomSetter setZ(double aZ) {mAtom.mZ = aZ; return this;}
        @Override public IAtomSetter setID(int aID) {mAtom.mID = aID; return this;}
        @Override public IAtomSetter setType(int aType) {mAtom.mType = aType; return this;}
    }
    
    @Override public IAtomData mapUpdate(int aMinTypeNum, IAtomUpdater aUpdater) {
        IAtomData tThis = thisAtomData_();
        List<IAtom> rAtoms = new ArrayList<>(tThis.atomNum());
        
        int tAtomTypeNum = Math.max(aMinTypeNum, tThis.atomTypeNum());
        for (IAtom oAtom : tThis.atoms()) {
            Atom tAtom = new Atom(oAtom);
            // 传入 AtomSetter 来更新粒子
            aUpdater.update(oAtom, new AtomSetter(tAtom));
            // 更新种类数
            if (tAtom.mType > tAtomTypeNum) tAtomTypeNum = tAtom.mType;
            // 保存修改后的原子
            rAtoms.add(tAtom);
        }
        
        return new AtomData(rAtoms, tAtomTypeNum, tThis.boxLo(), tThis.boxHi());
    }
    
    
    @Override public IAtomData randomPerturbXYZByGaussian(final Random aRandom, final double aSigma) {
        // 先获取 box
        IAtomData tThis = thisAtomData_();
        final XYZ tBoxLo = toXYZ(tThis.boxLo());
        final XYZ tBoxHi = toXYZ(tThis.boxHi());
        final XYZ tBox = tBoxHi.minus(tBoxLo);
        // 使用 mapUpdate 获取种类修改后的 AtomData，注意周期边界条件
        return mapUpdate((atom, setter) -> {
            double tX = atom.x() + aRandom.nextGaussian()*aSigma;
            double tY = atom.y() + aRandom.nextGaussian()*aSigma;
            double tZ = atom.z() + aRandom.nextGaussian()*aSigma;
            if      (tX <  tBoxLo.mX) {tX += tBox.mX; while (tX <  tBoxLo.mX) tX += tBox.mX;}
            else if (tX >= tBoxHi.mX) {tX -= tBox.mX; while (tX >= tBoxHi.mX) tX -= tBox.mX;}
            if      (tY <  tBoxLo.mY) {tY += tBox.mY; while (tY <  tBoxLo.mY) tY += tBox.mY;}
            else if (tY >= tBoxHi.mY) {tY -= tBox.mY; while (tY >= tBoxHi.mY) tY -= tBox.mY;}
            if      (tZ <  tBoxLo.mZ) {tZ += tBox.mZ; while (tZ <  tBoxLo.mZ) tZ += tBox.mZ;}
            else if (tZ >= tBoxHi.mZ) {tZ -= tBox.mZ; while (tZ >= tBoxHi.mZ) tZ -= tBox.mZ;}
            setter.setX(tX);
            setter.setY(tY);
            setter.setZ(tZ);
        });
    }
    
    @Override public IAtomData randomUpdateTypeByWeight(Random aRandom, IVector aTypeWeights) {
        double tTotWeight = aTypeWeights.sum();
        if (tTotWeight <= 0.0) throw new RuntimeException("TypeWeights Must be Positive");
        
        int tAtomNum = thisAtomData_().atomNum();
        int tMaxType = aTypeWeights.size();
        // 获得对应原子种类的 List
        final List<Integer> tTypeList = new ArrayList<>(tAtomNum+tMaxType);
        for (int tType = 1; tType <= tMaxType; ++tType) {
            // 计算这种种类的粒子数目
            long tSteps = Math.round((aTypeWeights.get_(tType-1) / tTotWeight) * tAtomNum);
            for (int i = 0; i < tSteps; ++i) tTypeList.add(tType);
        }
        // 简单处理，如果数量不够则添加最后一种种类
        while (tTypeList.size() < tAtomNum) tTypeList.add(tMaxType);
        // 随机打乱这些种类标记
        Collections.shuffle(tTypeList, aRandom);
        // 使用 mapUpdate 获取种类修改后的 AtomData
        return mapUpdate(tMaxType, (atom, setter) -> setter.setType(tTypeList.remove(tTypeList.size()-1)));
    }
    
    /** stuff to override */
    protected abstract IAtomData thisAtomData_();
}
