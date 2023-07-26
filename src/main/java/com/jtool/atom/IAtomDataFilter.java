package com.jtool.atom;

import com.jtool.code.operator.IOperator1;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;

import java.util.Random;

import static com.jtool.code.CS.RANDOM;

public interface IAtomDataFilter {
    /**
     * 根据通用的过滤器 aFilter 来过滤 aAtomData，修改粒子的种类
     * @author liqa
     * @param aMinTypeNum 建议最小的种类数目
     * @param aFilter 自定义的过滤器，输入 {@link IAtom}，返回过滤后的 type
     * @return 过滤后的 AtomData
     */
    IHasAtomData type(int aMinTypeNum, IOperator1<Integer, IAtom> aFilter);
    default IHasAtomData type(IOperator1<Integer, IAtom> aFilter) {return type(1, aFilter);}
    
    /**
     * 根据给定的权重来随机修改原子种类，主要用于创建合金的初始结构
     * @author liqa
     */
    IHasAtomData typeWeight(Random aRandom, IVector aTypeWeights);
    default IHasAtomData typeWeight(IVector aTypeWeights) {return typeWeight(RANDOM, aTypeWeights);}
    default IHasAtomData typeWeight(Random aRandom, double... aTypeWeights) {
        // 特殊输入直接抛出错误
        if (aTypeWeights == null || aTypeWeights.length == 0) throw new RuntimeException("TypeWeights Must be not empty");
        return typeWeight(aRandom, Vectors.from(aTypeWeights));
    }
    default IHasAtomData typeWeight(double... aTypeWeights) {return typeWeight(RANDOM, aTypeWeights);}
}
