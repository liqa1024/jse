package jse.atom;

import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.Random;

import static jse.code.CS.RANDOM;

/**
 * 专门为可以修改的 AtomData 提供一个运算器，
 * 附加一些可以修改自身的操作
 * @author liqa
 */
public interface ISettableAtomDataOperation extends IAtomDataOperation {
    ISettableAtomData refSlice(ISlice aIndices);
    ISettableAtomData refSlice(List<Integer> aIndices);
    ISettableAtomData refSlice(int[] aIndices);
    ISettableAtomData refSlice(IIndexFilter aIndices);
    
    void map2this(int aMinTypeNum, IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator);
    default void map2this(IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator) {map2this(1, aOperator);}
    
    void mapType2this(int aMinTypeNum, IUnaryFullOperator<Integer, ? super IAtom> aOperator);
    default void mapType2this(IUnaryFullOperator<Integer, ? super IAtom> aOperator) {mapType2this(1, aOperator);}
    
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
    
    void wrapPBC2this();
    @VisibleForTesting default void wrap2this() {wrapPBC2this();}
}
