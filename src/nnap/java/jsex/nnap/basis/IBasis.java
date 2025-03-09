package jsex.nnap.basis;

import jse.atom.IAtomData;
import jse.atom.AtomicParameterCalculator;
import jse.code.io.ISavable;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * 通用的 nnap 基组/描述符实现
 * @author liqa
 */
@ApiStatus.Experimental
public interface IBasis extends ISavable, IAutoShutdown {
    /** @return 基组需要的近邻截断半径 */
    double rcut();
    /** @return 基组的长度 */
    int size();
    
    /** @return 基组需要的元素顺序 */
    @Nullable List<String> symbols();
    default boolean hasSymbol() {return symbols()!=null;}
    default IntUnaryOperator typeMap(IAtomData aAtomData) {
        List<String> tSymbols = symbols();
        if (tSymbols == null) throw new UnsupportedOperationException("`typeMap` for Basis without symbols");
        return IAtomData.typeMap_(tSymbols, aAtomData);
    }
    default boolean sameOrder(Collection<? extends CharSequence> aSymbolsIn) {
        List<String> tSymbols = symbols();
        if (tSymbols == null) throw new UnsupportedOperationException("`sameOrder` for Basis without symbols");
        return IAtomData.sameSymbolOrder_(tSymbols, aSymbolsIn);
    }
    default int typeOf(String aSymbol) {
        List<String> tSymbols = symbols();
        if (tSymbols == null) throw new UnsupportedOperationException("`typeOf` for Basis without symbols");
        return IAtomData.typeOf_(tSymbols, aSymbol);
    }
    
    @FunctionalInterface interface IDxyzTypeIterable {void forEachDxyzType(IDxyzTypeDo aDxyzTypeDo);}
    @FunctionalInterface interface IDxyzTypeDo {void run(double aDx, double aDy, double aDz, int aType);}
    
    /**
     * 通用的计算基组的接口，可以自定义任何近邻列表获取器来实现
     * @param aNL 近邻列表遍历器
     * @return 原子描述符向量
     */
    Vector eval(IDxyzTypeIterable aNL);
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 原子描述符向量
     */
    default Vector eval(final AtomicParameterCalculator aAPC, final int aIdx, final IntUnaryOperator aTypeMap) {
        return eval(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        });
    }
    default Vector eval(AtomicParameterCalculator aAPC, int aIdx) {return eval(aAPC, aIdx, type->type);}
    /**
     * 简单遍历计算给定原子数据所有基组的实现，此实现适合对相同基组计算大量的原子结构；
     * 由于基组存储了元素排序，因此可以自动修正多个原子结构中元素排序不一致的问题
     * @param aAtomData 原子结构数据
     * @return 原子描述符向量组成的列表
     */
    default List<Vector> evalAll(IAtomData aAtomData) {
        IntUnaryOperator tTypeMap = hasSymbol() ? typeMap(aAtomData) : type->type;
        int tAtomNum = aAtomData.atomNumber();
        List<Vector> rFingerPrints = new ArrayList<>(tAtomNum);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                rFingerPrints.add(eval(tAPC, i, tTypeMap));
            }
        }
        return rFingerPrints;
    }
    
    /**
     * 基组结果对于 {@code xyz} 偏微分的计算结果，主要用于力的计算；会同时计算基组值本身
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aNL 近邻列表遍历器
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    List<@NotNull Vector> evalPartial(boolean aCalCross, IDxyzTypeIterable aNL);
    default List<@NotNull Vector> evalPartial(IDxyzTypeIterable aNL) {return evalPartial(false, aNL);}
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组偏导数功能
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    default List<@NotNull Vector> evalPartial(boolean aCalCross, final AtomicParameterCalculator aAPC, final int aIdx, final IntUnaryOperator aTypeMap) {
        return evalPartial(aCalCross, dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        });
    }
    default List<@NotNull Vector> evalPartial(AtomicParameterCalculator aAPC, int aIdx, IntUnaryOperator aTypeMap) {return evalPartial(false, aAPC, aIdx, aTypeMap);}
    default List<@NotNull Vector> evalPartial(boolean aCalCross, AtomicParameterCalculator aAPC, int aIdx) {return evalPartial(aCalCross, aAPC, aIdx, type->type);}
    default List<@NotNull Vector> evalPartial(AtomicParameterCalculator aAPC, int aIdx) {return evalPartial(false, aAPC, aIdx);}
    /**
     * 简单遍历计算给定原子数据所有基组偏导的实现，会同时计算基组值本身，此实现适合对相同基组计算大量的原子结构；
     * 由于基组存储了元素排序，因此可以自动修正多个原子结构中元素排序不一致的问题
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aAtomData 原子结构数据
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    default List<List<Vector>> evalAllPartial(boolean aCalCross, IAtomData aAtomData) {
        IntUnaryOperator tTypeMap = hasSymbol() ? typeMap(aAtomData) : type->type;
        int tAtomNum = aAtomData.atomNumber();
        List<List<Vector>> rOut = new ArrayList<>(tAtomNum);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                rOut.add(evalPartial(aCalCross, tAPC, i, tTypeMap));
            }
        }
        return rOut;
    }
    default List<List<Vector>> evalAllPartial(IAtomData aAtomData) {return evalAllPartial(false, aAtomData);}
    
    @Override default void shutdown() {/**/}
}
