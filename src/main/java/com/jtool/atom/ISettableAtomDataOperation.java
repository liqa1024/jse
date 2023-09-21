package com.jtool.atom;

import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IOperator1;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.Random;

import static com.jtool.code.CS.RANDOM;

/**
 * 专门为可以修改的 AtomData 提供一个运算器，
 * 附加一些可以修改自身的操作
 * @author liqa
 */
public interface ISettableAtomDataOperation extends IAtomDataOperation {
    ISettableAtomData refSlice(List<Integer> aIndices);
    ISettableAtomData refSlice(int[] aIndices);
    ISettableAtomData refSlice(IIndexFilter aIndices);
    
    void map2this(int aMinTypeNum, IOperator1<? extends IAtom, ? super IAtom> aOperator);
    default void map2this(IOperator1<? extends IAtom, ? super IAtom> aOperator) {map2this(1, aOperator);}
    
    void mapType2this(int aMinTypeNum, IOperator1<Integer, ? super IAtom> aOperator);
    default void mapType2this(IOperator1<Integer, ? super IAtom> aOperator) {mapType2this(1, aOperator);}
    
    void mapTypeRandom2this(Random aRandom, IVector aTypeWeights);
    default void mapTypeRandom2this(IVector aTypeWeights) {mapTypeRandom2this(RANDOM, aTypeWeights);}
    default void mapTypeRandom2this(Random aRandom, double... aTypeWeights) {
        // 特殊输入直接抛出错误
        if (aTypeWeights == null || aTypeWeights.length == 0) throw new RuntimeException("TypeWeights Must be not empty");
        mapTypeRandom2this(aRandom, Vectors.from(aTypeWeights));
    }
    default void mapTypeRandom2this(double... aTypeWeights) {mapTypeRandom2this(RANDOM, aTypeWeights);}
    
    void perturbXYZGaussian2this(Random aRandom, double aSigma);
    default void perturbXYZGaussian2this(double aSigma) {perturbXYZGaussian2this(RANDOM, aSigma);}
    @VisibleForTesting default void perturbXYZ2this(Random aRandom, double aSigma) {perturbXYZGaussian2this(aRandom, aSigma);}
    @VisibleForTesting default void perturbXYZ2this(double aSigma) {perturbXYZGaussian2this(aSigma);}
}
