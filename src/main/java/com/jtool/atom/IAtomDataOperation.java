package com.jtool.atom;


import com.jtool.code.filter.IFilter;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IOperator1;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.Random;

import static com.jtool.code.CS.RANDOM;

/**
 * 现在改为通用的例子操作运算，命名和其余的 operation 保持类似格式；
 * 默认会返回新的 AtomData
 * @author liqa
 */
public interface IAtomDataOperation {
    /**
     * 根据通用的过滤器 aFilter 来过滤 aAtomData，保留满足 Filter 的原子
     * <p>
     * 这里会直接过滤一次构造完成过滤后的 List
     * @author liqa
     * @param aFilter 自定义的过滤器，输入 {@link IAtom}，返回是否保留
     * @return 新创建的过滤后的 AtomData
     */
    IAtomData filter(IFilter<IAtom> aFilter);
    
    /**
     * 根据原子种类来过滤 aAtomData，只保留选定种类的原子
     * @author liqa
     * @param aType 选择保留的原子种类数
     * @return 新创建的过滤后的 AtomData
     */
    IAtomData filterType(int aType);
    
    /**
     * 直接根据 {@code List<Integer>} 来过滤 aAtomData，只保留选定种类的原子
     * <p>
     * 直接通过输入的 aIndices 进行引用映射，相当于 {@code refSlice}
     * @author liqa
     * @param aIndices 选择保留的原子的下标组成的数组
     * @return 新创建的过滤后的 AtomData
     */
    IAtomData filterIndices(List<Integer> aIndices);
    IAtomData filterIndices(int[] aIndices);
    IAtomData filterIndices(IIndexFilter aIndices);
    
    
    /**
     * 根据通用的原子映射 aOperator 来遍历映射修改原子
     * @author liqa
     * @param aMinTypeNum 建议最小的种类数目
     * @param aOperator 自定义的原子映射运算，输入 {@link IAtom} 输出修改后的 {@link IAtom}
     * @return 新创建的修改后的 AtomData
     */
    IAtomData map(int aMinTypeNum, IOperator1<? extends IAtom, ? super IAtom> aOperator);
    default IAtomData map(IOperator1<? extends IAtom, ? super IAtom> aOperator) {return map(1, aOperator);}
    
    /**
     * 根据特殊的原子种类映射 aOperator 来遍历映射修改原子种类
     * @author liqa
     * @param aMinTypeNum 建议最小的种类数目
     * @param aOperator 自定义的原子映射运算，输入 {@link IAtom} 输出应该分配的种类
     * @return 新创建的修改后的 AtomData
     */
    IAtomData mapType(int aMinTypeNum, IOperator1<Integer, ? super IAtom> aOperator);
    default IAtomData mapType(IOperator1<Integer, ? super IAtom> aOperator) {return mapType(1, aOperator);}
    
    
    /**
     * 根据给定的权重来随机分配原子种类，主要用于创建合金的初始结构
     * @author liqa
     * @param aRandom 可选自定义的随机数生成器
     * @param aTypeWeights 每个种类的权重
     * @return 新创建的修改后的 AtomData
     */
    IAtomData mapTypeRandom(Random aRandom, IVector aTypeWeights);
    default IAtomData mapTypeRandom(IVector aTypeWeights) {return mapTypeRandom(RANDOM, aTypeWeights);}
    default IAtomData mapTypeRandom(Random aRandom, double... aTypeWeights) {
        // 特殊输入直接抛出错误
        if (aTypeWeights == null || aTypeWeights.length == 0) throw new RuntimeException("TypeWeights Must be not empty");
        return mapTypeRandom(aRandom, Vectors.from(aTypeWeights));
    }
    default IAtomData mapTypeRandom(double... aTypeWeights) {return mapTypeRandom(RANDOM, aTypeWeights);}
    
    /**
     * 使用高斯分布来随机扰动原子位置
     * @author liqa
     * @param aRandom 可选自定义的随机数生成器
     * @param aSigma 高斯分布的标准差
     * @return 新创建的扰动后的 AtomData
     */
    IAtomData perturbXYZGaussian(Random aRandom, double aSigma);
    default IAtomData perturbXYZGaussian(double aSigma) {return perturbXYZGaussian(RANDOM, aSigma);}
    @VisibleForTesting default IAtomData perturbXYZ(Random aRandom, double aSigma) {return perturbXYZGaussian(aRandom, aSigma);}
    @VisibleForTesting default IAtomData perturbXYZ(double aSigma) {return perturbXYZGaussian(aSigma);}
}
