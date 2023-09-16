package com.jtool.atom;

import com.jtool.code.collection.AbstractCollections;
import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.code.collection.NewCollections;
import com.jtool.code.filter.IFilter;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IOperator1;
import com.jtool.math.vector.IVector;

import java.util.*;

import static com.jtool.code.UT.Code.toXYZ;


/**
 * 一般的运算器的实现，值拷贝一次并使用 {@code ArrayList<IAtom>} 来存储，尽管这会占据更多的内存
 * @author liqa
 */
public abstract class AbstractAtomDataOperation implements IAtomDataOperation {
    
    @Override public IAtomData filter(IFilter<IAtom> aFilter) {
        IAtomData tThis = thisAtomData_();
        List<IAtom> rAtoms = new ArrayList<>();
        for (IAtom tAtom : tThis.atoms()) if (aFilter.accept(tAtom)) {
            rAtoms.add(tAtom);
        }
        return new AtomData(rAtoms, tThis.atomTypeNum(), tThis.boxLo(), tThis.boxHi(), tThis.hasVelocities());
    }
    @Override public IAtomData filterType(final int aType) {return filter(atom -> atom.type()==aType);}
    
    @Override public IAtomData filterIndices(final List<Integer> aIndices) {
        IAtomData tThis = thisAtomData_();
        final List<IAtom> tThisAtoms = tThis.atoms();
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(int index) {return tThisAtoms.get(aIndices.get(index));}
            @Override public int size() {return aIndices.size();}
        }, tThis.atomTypeNum(), tThis.boxLo(), tThis.boxHi(), tThis.hasVelocities());
    }
    @Override public IAtomData filterIndices(int[] aIndices) {return filterIndices(AbstractCollections.from(aIndices));}
    @Override public IAtomData filterIndices(IIndexFilter aIndices) {return filterIndices(NewCollections.filterIndex(thisAtomData_().atomNum(), aIndices));}
    
    
    @Override public IAtomData map(int aMinTypeNum, IOperator1<? extends IAtom, ? super IAtom> aOperator) {
        IAtomData tThis = thisAtomData_();
        List<IAtom> rAtoms = new ArrayList<>(tThis.atomNum());
        int tAtomTypeNum = Math.max(aMinTypeNum, tThis.atomTypeNum());
        for (IAtom oAtom : tThis.atoms()) {
            IAtom tAtom = aOperator.cal(oAtom);
            // 更新种类数
            int tType = tAtom.type();
            if (tType > tAtomTypeNum) tAtomTypeNum = tType;
            // 保存修改后的原子
            rAtoms.add(tAtom);
        }
        return new AtomData(rAtoms, tAtomTypeNum, tThis.boxLo(), tThis.boxHi(), tThis.hasVelocities());
    }
    
    /** 减少重复代码，用于内部修改原子个别属性 */
    protected static class WrapperAtom implements IAtom {
        protected final IAtom mAtom;
        protected WrapperAtom(IAtom aAtom) {mAtom = aAtom;}
        
        @Override public double x() {return mAtom.x();}
        @Override public double y() {return mAtom.y();}
        @Override public double z() {return mAtom.z();}
        @Override public int id() {return mAtom.id();}
        @Override public int type() {return mAtom.type();}
        
        @Override public double vx() {return mAtom.vx();}
        @Override public double vy() {return mAtom.vy();}
        @Override public double vz() {return mAtom.vz();}
    }
    protected final static class TypeWrapperAtom extends WrapperAtom {
        final int mType;
        TypeWrapperAtom(IAtom aAtom, int aType) {super(aAtom); mType = aType;}
        @Override public int type() {return mType;}
    }
    protected final static class XYZWrapperAtom extends WrapperAtom {
        final double mX, mY, mZ;
        XYZWrapperAtom(IAtom aAtom, double aX, double aY, double aZ) {super(aAtom); mX = aX; mY = aY; mZ = aZ;}
        @Override public double x() {return mX;}
        @Override public double y() {return mY;}
        @Override public double z() {return mZ;}
    }
    
    @Override public IAtomData mapType(int aMinTypeNum, final IOperator1<Integer, ? super IAtom> aOperator) {
        return map(aMinTypeNum, atom -> new TypeWrapperAtom(atom, aOperator.cal(atom)));
    }
    
    @Override public IAtomData perturbXYZGaussian(final Random aRandom, final double aSigma) {
        // 先获取 box
        IAtomData tThis = thisAtomData_();
        final XYZ tBoxLo = toXYZ(tThis.boxLo());
        final XYZ tBoxHi = toXYZ(tThis.boxHi());
        final XYZ tBox = tBoxHi.minus(tBoxLo);
        // 使用 collect 获取种类修改后的 AtomData，注意周期边界条件
        return map(atom -> {
            double tX = atom.x() + aRandom.nextGaussian()*aSigma;
            double tY = atom.y() + aRandom.nextGaussian()*aSigma;
            double tZ = atom.z() + aRandom.nextGaussian()*aSigma;
            if      (tX <  tBoxLo.mX) {tX += tBox.mX; while (tX <  tBoxLo.mX) tX += tBox.mX;}
            else if (tX >= tBoxHi.mX) {tX -= tBox.mX; while (tX >= tBoxHi.mX) tX -= tBox.mX;}
            if      (tY <  tBoxLo.mY) {tY += tBox.mY; while (tY <  tBoxLo.mY) tY += tBox.mY;}
            else if (tY >= tBoxHi.mY) {tY -= tBox.mY; while (tY >= tBoxHi.mY) tY -= tBox.mY;}
            if      (tZ <  tBoxLo.mZ) {tZ += tBox.mZ; while (tZ <  tBoxLo.mZ) tZ += tBox.mZ;}
            else if (tZ >= tBoxHi.mZ) {tZ -= tBox.mZ; while (tZ >= tBoxHi.mZ) tZ -= tBox.mZ;}
            
            return new XYZWrapperAtom(atom, tX, tY, tZ);
        });
    }
    
    @Override public IAtomData mapTypeRandom(Random aRandom, IVector aTypeWeights) {
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
        final Iterator<Integer> it = tTypeList.iterator();
        // 使用 mapType 获取种类修改后的 AtomData
        return mapType(tMaxType, atom -> it.next());
    }
    
    /** stuff to override */
    protected abstract IAtomData thisAtomData_();
}
