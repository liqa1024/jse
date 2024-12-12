package jsex.nnap.basis;

import jse.atom.IAtomData;
import jse.atom.MonatomicParameterCalculator;
import jse.io.ISavable;
import jse.math.matrix.RowMatrix;
import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    /** @see #rowNumber() */
    default int nrows() {return rowNumber();}
    /** @see #columnNumber() */
    default int ncols() {return columnNumber();}
    /** @return 基组矩阵的行数目 */
    int rowNumber();
    /** @return 基组矩阵的列数目 */
    int columnNumber();
    
    /** @return 基组需要的元素顺序 */
    @Nullable List<String> symbols();
    default boolean hasSymbol() {return symbols()!=null;}
    default IntUnaryOperator typeMap(IAtomData aAtomData) {
        List<String> tSymbols = symbols();
        if (tSymbols == null) throw new UnsupportedOperationException("`typeMap` for Basis without symbols");
        return typeMap_(tSymbols, aAtomData);
    }
    default boolean sameOrder(List<String> aDataSymbols) {
        List<String> tSymbols = symbols();
        if (tSymbols == null) throw new UnsupportedOperationException("`sameOrder` for Basis without symbols");
        return sameOrder_(tSymbols, aDataSymbols);
    }
    default int indexOf(String aSymbol) {
        List<String> tSymbols = symbols();
        if (tSymbols == null) throw new UnsupportedOperationException("`indexOf` for Basis without symbols");
        return indexOf_(tSymbols, aSymbol);
    }
    
    @ApiStatus.Internal
    static IntUnaryOperator typeMap_(List<String> aSymbols, IAtomData aAtomData) {
        if (aSymbols.size() < aAtomData.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of AtomData: " + aAtomData.atomTypeNumber() + ", target: " + aSymbols.size());
        List<String> tAtomDataSymbols = aAtomData.symbols();
        if (tAtomDataSymbols==null || sameOrder_(aSymbols, tAtomDataSymbols)) return type->type;
        final int[] tAtomDataType2newType = new int[tAtomDataSymbols.size()+1];
        for (int i = 0; i < tAtomDataSymbols.size(); ++i) {
            String tElem = tAtomDataSymbols.get(i);
            int idx = indexOf_(aSymbols, tElem);
            if (idx < 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in AtomData");
            tAtomDataType2newType[i+1] = idx+1;
        }
        return type -> tAtomDataType2newType[type];
    }
    @ApiStatus.Internal
    static boolean sameOrder_(List<String> aSymbols, List<String> aDataSymbols) {
        for (int i = 0; i < aDataSymbols.size(); ++i) {
            if (!aDataSymbols.get(i).equals(aSymbols.get(i))) {
                return false;
            }
        }
        return true;
    }
    @ApiStatus.Internal
    static int indexOf_(List<String> aSymbols, String aSymbol) {
        for (int i = 0; i < aSymbols.size(); ++i) {
            if (aSymbol.equals(aSymbols.get(i))) {
                return i;
            }
        }
        return -1;
    }
    
    
    @FunctionalInterface interface IDxyzTypeIterable {void forEachDxyzType(IDxyzTypeDo aDxyzTypeDo);}
    @FunctionalInterface interface IDxyzTypeDo {void run(double aDx, double aDy, double aDz, int aType);}
    
    /**
     * 通用的计算基组的接口，可以自定义任何近邻列表获取器来实现
     * @param aNL 近邻列表遍历器
     * @return 原子描述符行矩阵，可以通过 asVecRow 来转为向量形式方便作为神经网络的输入
     */
    RowMatrix eval(IDxyzTypeIterable aNL);
    /**
     * 基于 {@link MonatomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组功能
     * @param aMPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 原子描述符行矩阵，可以通过 asVecRow 来转为向量形式方便作为神经网络的输入
     */
    default RowMatrix eval(final MonatomicParameterCalculator aMPC, final int aIdx, final IntUnaryOperator aTypeMap) {
        return eval(dxyzTypeDo -> {
            aMPC.nl_().forEachNeighbor(aIdx, rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aMPC.atomType_().get(idx)));
            });
        });
    }
    default RowMatrix eval(MonatomicParameterCalculator aMPC, int aIdx) {return eval(aMPC, aIdx, type->type);}
    /**
     * 简单遍历计算给定原子数据所有基组的实现，此实现适合对相同基组计算大量的原子结构；
     * 由于基组存储了元素排序，因此可以自动修正多个原子结构中元素排序不一致的问题
     * @param aAtomData 原子结构数据
     * @return 原子描述符行矩阵组成的列表
     */
    default List<RowMatrix> evalAll(IAtomData aAtomData) {
        IntUnaryOperator tTypeMap = hasSymbol() ? typeMap(aAtomData) : type->type;
        int tAtomNum = aAtomData.atomNumber();
        List<RowMatrix> rFingerPrints = new ArrayList<>(tAtomNum);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                rFingerPrints.add(eval(tMPC, i, tTypeMap));
            }
        }
        return rFingerPrints;
    }
    
    /**
     * 基组结果对于 {@code xyz} 偏微分的计算结果，主要用于力的计算
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aNL 近邻列表遍历器
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, boolean aCalCross, IDxyzTypeIterable aNL);
    default List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, IDxyzTypeIterable aNL) {return evalPartial(aCalBasis, false, aNL);}
    default List<@NotNull RowMatrix> evalPartial(IDxyzTypeIterable aNL) {return evalPartial(true, aNL);}
    /**
     * 基于 {@link MonatomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组偏导数功能
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aMPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    default List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, boolean aCalCross, final MonatomicParameterCalculator aMPC, final int aIdx, final IntUnaryOperator aTypeMap) {
        return evalPartial(aCalBasis, aCalCross, dxyzTypeDo -> {
            aMPC.nl_().forEachNeighbor(aIdx, rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aMPC.atomType_().get(idx)));
            });
        });
    }
    default List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, MonatomicParameterCalculator aMPC, int aIdx, IntUnaryOperator aTypeMap) {return evalPartial(aCalBasis, false, aMPC, aIdx, aTypeMap);}
    default List<@NotNull RowMatrix> evalPartial(MonatomicParameterCalculator aMPC, int aIdx, IntUnaryOperator aTypeMap) {return evalPartial(true, aMPC, aIdx, aTypeMap);}
    default List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, boolean aCalCross, MonatomicParameterCalculator aMPC, int aIdx) {return evalPartial(aCalBasis, aCalCross, aMPC, aIdx, type->type);}
    default List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, MonatomicParameterCalculator aMPC, int aIdx) {return evalPartial(aCalBasis, false, aMPC, aIdx);}
    default List<@NotNull RowMatrix> evalPartial(MonatomicParameterCalculator aMPC, int aIdx) {return evalPartial(true, aMPC, aIdx);}
    /**
     * 简单遍历计算给定原子数据所有基组偏导的实现，此实现适合对相同基组计算大量的原子结构；
     * 由于基组存储了元素排序，因此可以自动修正多个原子结构中元素排序不一致的问题
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aAtomData 原子结构数据
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    default List<List<RowMatrix>> evalAllPartial(boolean aCalBasis, boolean aCalCross, IAtomData aAtomData) {
        IntUnaryOperator tTypeMap = hasSymbol() ? typeMap(aAtomData) : type->type;
        int tAtomNum = aAtomData.atomNumber();
        List<List<RowMatrix>> rOut = new ArrayList<>(tAtomNum);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                rOut.add(evalPartial(aCalBasis, aCalCross, tMPC, i, tTypeMap));
            }
        }
        return rOut;
    }
    default List<List<RowMatrix>> evalAllPartial(boolean aCalBasis, IAtomData aAtomData) {return evalAllPartial(aCalBasis, false, aAtomData);}
    default List<List<RowMatrix>> evalAllPartial(IAtomData aAtomData) {return evalAllPartial(true, aAtomData);}
    
    @Override default void shutdown() {/**/}
}
