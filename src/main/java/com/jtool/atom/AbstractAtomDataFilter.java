package com.jtool.atom;

import com.jtool.code.operator.IOperator1;
import com.jtool.math.vector.IVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 一般的过滤器的实现，值拷贝一次并使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存
 * @author liqa
 */
public abstract class AbstractAtomDataFilter implements IAtomDataFilter {
    @Override public IHasAtomData type(int aMinTypeNum, IOperator1<Integer, IAtom> aFilter) {
        IHasAtomData tThis = thisAtomData_();
        final List<IAtom> rAtoms = new ArrayList<>(tThis.atomNum());
        
        int tAtomTypeNum = Math.max(aMinTypeNum, tThis.atomTypeNum());
        for (IAtom oAtom : tThis.atoms()) {
            Atom tAtom = new Atom(oAtom);
            // 更新粒子种类数目
            int tType = aFilter.cal(oAtom);
            if (tType > tAtomTypeNum) tAtomTypeNum = tType;
            tAtom.mType = tType;
            // 保存修改后的原子
            rAtoms.add(tAtom);
        }
        final int fAtomTypeNum = tAtomTypeNum;
        
        return new AbstractAtomData() {
            @Override public List<IAtom> atoms() {return rAtoms;}
            @Override public IHasXYZ boxLo() {return tThis.boxLo();}
            @Override public IHasXYZ boxHi() {return tThis.boxHi();}
            @Override public int atomNum() {return rAtoms.size();}
            @Override public int atomTypeNum() {return fAtomTypeNum;}
        };
    }
    
    
    @Override public IHasAtomData typeWeight(Random aRandom, IVector aTypeWeights) {
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
        // 使用 typeFilter 获取种类修改后的 AtomData
        final AtomicInteger idx = new AtomicInteger();
        return type(tMaxType, atom -> tTypeList.get(idx.getAndIncrement()));
    }
    
    /** stuff to override */
    protected abstract IHasAtomData thisAtomData_();
}
