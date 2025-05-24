package jsex.nnap.basis;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IHasSymbol;
import jse.code.collection.DoubleList;
import jse.code.io.ISavable;
import jse.math.vector.DoubleArrayVector;
import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * 通用的 nnap 基组/描述符实现
 * @author liqa
 */
@ApiStatus.Experimental
public interface IBasis extends IHasSymbol, ISavable, IAutoShutdown {
    /** @return 基组需要的近邻截断半径 */
    double rcut();
    /** @return 基组的长度 */
    int size();
    
    /** @return {@inheritDoc} */
    @Override default int atomTypeNumber() {return 1;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override default boolean hasSymbol() {return false;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类
     */
    @Override default @Nullable String symbol(int aType) {return null;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override default @Nullable List<@Nullable String> symbols() {return IHasSymbol.super.symbols();}
    
    /**
     * 检测此基组是否已经关闭，默认永远为 {@code false}（即使手动调用了
     * {@link #shutdown()}），即默认不会去进行是否关闭的检测；
     * 重写此函数来在调用计算时检测是否关闭
     * @return 此基组是否已经关闭
     */
    default boolean isShutdown() {return false;}
    @Override default void shutdown() {/**/}
    
    @FunctionalInterface interface IDxyzTypeIterable {void forEachDxyzType(IDxyzTypeDo aDxyzTypeDo);}
    @FunctionalInterface interface IDxyzTypeDo {void run(double aDx, double aDy, double aDz, int aType);}
    
    /**
     * 通用的计算基组的接口，可以自定义任何近邻列表获取器来实现
     * @param aNL 近邻列表遍历器
     * @param rFp 计算输出的原子描述符向量
     */
    void eval(IDxyzTypeIterable aNL, DoubleArrayVector rFp);
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @param rFp 计算输出的原子描述符向量
     */
    default void eval(final AtomicParameterCalculator aAPC, final int aIdx, final IntUnaryOperator aTypeMap, DoubleArrayVector rFp) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        eval(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        }, rFp);
    }
    default void eval(AtomicParameterCalculator aAPC, int aIdx, DoubleArrayVector rFp) {eval(aAPC, aIdx, type->type, rFp);}
    
    /**
     * 基组结果对于 {@code xyz} 偏微分的计算结果，主要用于力的计算；会同时计算基组值本身
     * @param aNL 近邻列表遍历器
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于坐标 x 的偏导数
     * @param rFpPy 计算输出的原子描述符向量对于坐标 y 的偏导数
     * @param rFpPz 计算输出的原子描述符向量对于坐标 z 的偏导数
     */
    void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz);
    /**
     * 基组结果对于 {@code xyz} 偏微分的计算结果，主要用于力的计算；会同时计算基组值本身
     * @param aNL 近邻列表遍历器
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于坐标 x 的偏导数
     * @param rFpPy 计算输出的原子描述符向量对于坐标 y 的偏导数
     * @param rFpPz 计算输出的原子描述符向量对于坐标 z 的偏导数
     * @param rFpPxCross 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPyCross 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPzCross 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，会自动清空旧值并根据近邻列表扩容
     */
    void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz, DoubleList rFpPxCross, DoubleList rFpPyCross, DoubleList rFpPzCross);
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组偏导数功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于坐标 x 的偏导数
     * @param rFpPy 计算输出的原子描述符向量对于坐标 y 的偏导数
     * @param rFpPz 计算输出的原子描述符向量对于坐标 z 的偏导数
     */
    default void evalPartial(AtomicParameterCalculator aAPC, int aIdx, IntUnaryOperator aTypeMap, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        evalPartial(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        }, rFp, rFpPx, rFpPy, rFpPz);
    }
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组偏导数功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于坐标 x 的偏导数
     * @param rFpPy 计算输出的原子描述符向量对于坐标 y 的偏导数
     * @param rFpPz 计算输出的原子描述符向量对于坐标 z 的偏导数
     * @param rFpPxCross 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPyCross 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPzCross 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，会自动清空旧值并根据近邻列表扩容
     */
    default void evalPartial(AtomicParameterCalculator aAPC, int aIdx, IntUnaryOperator aTypeMap, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz, DoubleList rFpPxCross, DoubleList rFpPyCross, DoubleList rFpPzCross) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        evalPartial(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        }, rFp, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross);
    }
    default void evalPartial(AtomicParameterCalculator aAPC, int aIdx, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz) {
        evalPartial(aAPC, aIdx, type->type, rFp, rFpPx, rFpPy, rFpPz);
    }
    default void evalPartial(AtomicParameterCalculator aAPC, int aIdx, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz, DoubleList rFpPxCross, DoubleList rFpPyCross, DoubleList rFpPzCross) {
        evalPartial(aAPC, aIdx, type->type, rFp, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross);
    }
}
