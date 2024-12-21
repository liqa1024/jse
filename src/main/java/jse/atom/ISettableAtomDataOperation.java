package jse.atom;

import jse.code.collection.ISlice;
import jse.code.functional.IFilter;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.Random;

import static jse.code.CS.RANDOM;

/**
 * 通用的可以修改的原子数据运算接口，针对可以修改的一般原子数据
 * {@link ISettableAtomData}。相比一般的原子数据运算接口
 * {@link IAtomDataOperation} 增加了许多直接将修改应用到自身的功能
 * @author liqa
 * @see ISettableAtomData#operation()
 * @see IAtomDataOperation
 */
public interface ISettableAtomDataOperation extends IAtomDataOperation {
    /**
     * {@inheritDoc}
     * @param aIndices {@inheritDoc}
     * @return {@inheritDoc}
     * @see ISlice
     */
    ISettableAtomData refSlice(ISlice aIndices);
    /**
     * {@inheritDoc}
     * @param aIndices {@inheritDoc}
     * @return {@inheritDoc}
     */
    ISettableAtomData refSlice(List<Integer> aIndices);
    /**
     * {@inheritDoc}
     * @param aIndices {@inheritDoc}
     * @return {@inheritDoc}
     */
    ISettableAtomData refSlice(int[] aIndices);
    /**
     * {@inheritDoc}
     * @param aIndices {@inheritDoc}
     * @return {@inheritDoc}
     * @see IIndexFilter
     * @see #filter(IFilter)
     */
    ISettableAtomData refSlice(IIndexFilter aIndices);
    
    /** 将 {@link #map(int, IUnaryFullOperator)} 的运算结果直接设置到自身 */
    void map2this(int aMinTypeNum, IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator);
    /** 将 {@link #map(IUnaryFullOperator)} 的运算结果直接设置到自身 */
    default void map2this(IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator) {map2this(1, aOperator);}
    
    /** 将 {@link #mapType(int, IUnaryFullOperator)} 的运算结果直接设置到自身 */
    void mapType2this(int aMinTypeNum, IUnaryFullOperator<Integer, ? super IAtom> aOperator);
    /** 将 {@link #mapType(IUnaryFullOperator)} 的运算结果直接设置到自身 */
    default void mapType2this(IUnaryFullOperator<Integer, ? super IAtom> aOperator) {mapType2this(1, aOperator);}
    
    /** 将 {@link #mapTypeRandom(Random, IVector)} 的运算结果直接设置到自身 */
    void mapTypeRandom2this(Random aRandom, IVector aTypeWeights);
    /** 将 {@link #mapTypeRandom(IVector)} 的运算结果直接设置到自身 */
    default void mapTypeRandom2this(IVector aTypeWeights) {mapTypeRandom2this(RANDOM, aTypeWeights);}
    /** 将 {@link #mapTypeRandom(Random, double...)} 的运算结果直接设置到自身 */
    default void mapTypeRandom2this(Random aRandom, double... aTypeWeights) {
        // 特殊输入直接抛出错误
        if (aTypeWeights == null || aTypeWeights.length == 0) throw new RuntimeException("TypeWeights Must be not empty");
        mapTypeRandom2this(aRandom, Vectors.from(aTypeWeights));
    }
    /** 将 {@link #mapTypeRandom(double...)} 的运算结果直接设置到自身 */
    default void mapTypeRandom2this(double... aTypeWeights) {mapTypeRandom2this(RANDOM, aTypeWeights);}
    
    /** 将 {@link #perturbXYZGaussian(Random, double)} 的运算结果直接设置到自身 */
    void perturbXYZGaussian2this(Random aRandom, double aSigma);
    /** 将 {@link #perturbXYZGaussian(double)} 的运算结果直接设置到自身 */
    default void perturbXYZGaussian2this(double aSigma) {perturbXYZGaussian2this(RANDOM, aSigma);}
    /** 将 {@link #perturbXYZ(Random, double)} 的运算结果直接设置到自身 */
    @VisibleForTesting default void perturbXYZ2this(Random aRandom, double aSigma) {perturbXYZGaussian2this(aRandom, aSigma);}
    /** 将 {@link #perturbXYZ(double)} 的运算结果直接设置到自身 */
    @VisibleForTesting default void perturbXYZ2this(double aSigma) {perturbXYZGaussian2this(aSigma);}
    
    /** 将 {@link #wrapPBC()} 的运算结果直接设置到自身 */
    void wrapPBC2this();
    /** 将 {@link #wrap()} 的运算结果直接设置到自身 */
    @VisibleForTesting default void wrap2this() {wrapPBC2this();}
    
    /**
     * 对原子数据进行团簇分析，获取成团簇的原子索引存储为
     * {@link IntVector}，所有团簇构成一个列表
     * {@code List<IntVector>}
     * <p>
     * 这里直接按照团簇分析的发现顺序排列团簇，因此原则上列表是无序的
     * <p>
     * 通过开启 {@code aUnwrapByCluster2this}
     * 参数，可以顺便将原子按照团簇分析结果放在一起
     *
     * @param aRCut 用于判断团簇链接的截断半径
     * @param aUnwrapByCluster2this 是否在团簇分析过程中，顺便将原子按照团簇
     * unwrap 到一起；默认为 {@code false}
     * @return 每个团簇对应的原子索引列表 {@link IntVector} 组成的列表
     * @see IntVector
     * @see #unwrapByCluster(double)
     * @see #unwrapByCluster2this(double)
     */
    List<IntVector> clusterAnalyze(double aRCut, boolean aUnwrapByCluster2this);
    /** 将 {@link #unwrapByCluster(double)} 的运算结果直接设置到自身 */
    void unwrapByCluster2this(double aRCut);
}
