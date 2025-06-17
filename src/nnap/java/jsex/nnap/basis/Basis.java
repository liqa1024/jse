package jsex.nnap.basis;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IAtomData;
import jse.atom.IHasSymbol;
import jse.cache.VectorCache;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.io.ISavable;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jse.parallel.IAutoShutdown;
import jsex.nnap.NNAP;
import jsex.nnap.nn.NeuralNetwork;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * 通用的 nnap 基组/描述符实现
 * <p>
 * 由于内部会缓存近邻列表，因此此类相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
@ApiStatus.Experimental
public abstract class Basis implements IHasSymbol, ISavable, IAutoShutdown {
    static {
        // 依赖 nnap
        NNAP.InitHelper.init();
    }
    
    /** @return 基组需要的近邻截断半径 */
    public abstract double rcut();
    /** @return 基组的长度 */
    public abstract int size();
    
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return 1;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override public boolean hasSymbol() {return false;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类
     */
    @Override public @Nullable String symbol(int aType) {return null;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override public final @Nullable List<@Nullable String> symbols() {return IHasSymbol.super.symbols();}
    
    private boolean mDead = false;
    /** @return 此基组是否已经关闭 */
    public final boolean isShutdown() {return mDead;}
    @Override public final void shutdown() {
        if (mDead) return;
        mDead = true;
        shutdown_();
    }
    protected void shutdown_() {/**/}
    
    
    @FunctionalInterface public interface IDxyzTypeIterable {void forEachDxyzType(IDxyzTypeDo aDxyzTypeDo);}
    @FunctionalInterface public interface IDxyzTypeDo {void run(double aDx, double aDy, double aDz, int aType);}
    
    private DoubleList mNlDx = null, mNlDy = null, mNlDz = null;
    private IntList mNlType = null;
    private Vector mFp = null, mFpGrad = null;
    
    private void initCacheFp_() {
        if (mFp != null) return;
        int tSize = size();
        mFp = Vectors.zeros(tSize);
        mFpGrad = Vectors.zeros(tSize);
    }
    private void initCacheNl_() {
        if (mNlDx != null) return;
        mNlDx = new DoubleList(16);
        mNlDy = new DoubleList(16);
        mNlDz = new DoubleList(16);
        mNlType = new IntList(16);
    }
    
    private void buildNL_(IDxyzTypeIterable aNL) {
        initCacheNl_();
        final int tTypeNum = atomTypeNumber();
        // 缓存情况需要先清空这些
        mNlDx.clear(); mNlDy.clear(); mNlDz.clear();
        mNlType.clear();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            // 现在不再检测距离，因为需要处理合并情况下截断不一致的情况
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            mNlDx.add(dx); mNlDy.add(dy); mNlDz.add(dz);
            mNlType.add(type);
        });
    }
    static void validSize_(DoubleList aData, int aSize) {
        aData.ensureCapacity(aSize);
        aData.setInternalDataSize(aSize);
    }
    
    protected abstract void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, boolean aBufferNl);
    protected abstract void evalPartial_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz);
    protected abstract void evalPartialAndForceDot_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aFpGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz);
    
    /**
     * 内部使用的直接计算能量的接口，现在统一采用外部预先构造的近邻列表，从而可以避免重复遍历近邻
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param aNN 需要进行计算的神经网络
     * @return 计算得到的能量值
     */
    @ApiStatus.Internal
    public final double evalEnergy(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, NeuralNetwork aNN) throws Exception {
        initCacheFp_();
        eval_(aNlDx, aNlDy, aNlDz, aNlType, mFp, false);
        return aNN.forward(mFp);
    }
    
    /**
     * 内部使用的直接计算能量和力的接口，现在统一采用外部预先构造的近邻列表，从而可以避免重复遍历近邻
     * <p>
     * 直接给出能量和力可以避免大量偏导数的缓存，并且简化稀疏偏导的细节处理
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param aNN 需要进行计算的神经网络
     * @param rFx 计算得到的原子对于近邻原子 x 方向的力，要求已经是合适的大小，后续实现不会实际扩容
     * @param rFy 计算得到的原子对于近邻原子 y 方向的力，要求已经是合适的大小，后续实现不会实际扩容
     * @param rFz 计算得到的原子对于近邻原子 z 方向的力，要求已经是合适的大小，后续实现不会实际扩容
     * @return 计算得到的能量值
     */
    @ApiStatus.Internal
    public final double evalEnergyForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, NeuralNetwork aNN, DoubleList rFx, DoubleList rFy, DoubleList rFz) throws Exception {
        initCacheFp_();
        eval_(aNlDx, aNlDy, aNlDz, aNlType, mFp, true);
        double tEng = aNN.backward(mFp, mFpGrad);
        evalPartialAndForceDot_(aNlDx, aNlDy, aNlDz, aNlType, mFpGrad, rFx, rFy, rFz);
        return tEng;
    }
    
    
    /**
     * 内部使用的计算基组接口，现在统一采用外部预先构造的近邻列表，从而可以避免重复遍历近邻
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param rFp 计算输出的原子描述符向量
     */
    @ApiStatus.Internal
    public final void eval(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp) {
        eval_(aNlDx, aNlDy, aNlDz, aNlType, rFp, false);
    }
    /**
     * 通用的计算基组的接口，可以自定义任何近邻列表获取器来实现
     * @param aNL 近邻列表遍历器
     * @param rFp 计算输出的原子描述符向量
     */
    public final void eval(IDxyzTypeIterable aNL, DoubleArrayVector rFp) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        buildNL_(aNL);
        eval(mNlDx, mNlDy, mNlDz, mNlType, rFp);
    }
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @param rFp 计算输出的原子描述符向量
     */
    public final void eval(final AtomicParameterCalculator aAPC, final int aIdx, final IntUnaryOperator aTypeMap, DoubleArrayVector rFp) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        eval(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        }, rFp);
    }
    public final void eval(AtomicParameterCalculator aAPC, int aIdx, DoubleArrayVector rFp) {eval(aAPC, aIdx, type->type, rFp);}
    
    /**
     * 简单遍历计算给定原子数据所有基组的实现，此实现适合对相同基组计算大量的原子结构；
     * 由于基组存储了元素排序，因此可以自动修正多个原子结构中元素排序不一致的问题
     * @param aAtomData 原子结构数据
     * @return 原子描述符向量组成的列表
     */
    public final List<Vector> evalAll(IAtomData aAtomData) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        IntUnaryOperator tTypeMap = hasSymbol() ? typeMap(aAtomData) : type->type;
        int tAtomNum = aAtomData.atomNumber();
        List<Vector> rFps = VectorCache.getVec(size(), tAtomNum);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                eval(tAPC, i, tTypeMap, rFps.get(i));
            }
        }
        return rFps;
    }
    
    /**
     * 内部使用的计算基组以及偏导数接口，现在统一采用外部预先构造的近邻列表，从而可以避免重复遍历近邻
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，要求已经是合适的大小，后续实现不会实际扩容
     * @param rFpPy 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，要求已经是合适的大小，后续实现不会实际扩容
     * @param rFpPz 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，要求已经是合适的大小，后续实现不会实际扩容
     */
    @ApiStatus.Internal
    public final void evalPartial(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        eval_(aNlDx, aNlDy, aNlDz, aNlType, rFp, true);
        evalPartial_(aNlDx, aNlDy, aNlDz, aNlType, rFpPx, rFpPy, rFpPz);
    }
    /**
     * 基组结果对于 {@code xyz} 偏微分的计算结果，主要用于力的计算；会同时计算基组值本身
     * @param aNL 近邻列表遍历器
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPy 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPz 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，会自动清空旧值并根据近邻列表扩容
     */
    public final void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        buildNL_(aNL);
        // 初始化偏导数相关值
        int tSizeAll = mNlDx.size() * (rFp.size() + rFp.internalDataShift());
        validSize_(rFpPx, tSizeAll);
        validSize_(rFpPy, tSizeAll);
        validSize_(rFpPz, tSizeAll);
        evalPartial(mNlDx, mNlDy, mNlDz, mNlType, rFp, rFpPx, rFpPy, rFpPz);
    }
    /**
     * 基于 {@link AtomicParameterCalculator} 的近邻列表实现的通用的计算某个原子的基组偏导数功能
     * @param aAPC 原子结构参数计算器，用来获取近邻列表
     * @param aIdx 需要计算基组的原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @param rFp 计算输出的原子描述符向量
     * @param rFpPx 计算输出的原子描述符向量对于近邻原子坐标 x 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPy 计算输出的原子描述符向量对于近邻原子坐标 y 的偏导数，会自动清空旧值并根据近邻列表扩容
     * @param rFpPz 计算输出的原子描述符向量对于近邻原子坐标 z 的偏导数，会自动清空旧值并根据近邻列表扩容
     */
    public final void evalPartial(AtomicParameterCalculator aAPC, int aIdx, IntUnaryOperator aTypeMap, DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        evalPartial(dxyzTypeDo -> {
            aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
                dxyzTypeDo.run(dx, dy, dz, aTypeMap.applyAsInt(aAPC.atomType_().get(idx)));
            });
        }, rFp, rFpPx, rFpPy, rFpPz);
    }
    public final void evalPartial(AtomicParameterCalculator aAPC, int aIdx, DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        evalPartial(aAPC, aIdx, type->type, rFp, rFpPx, rFpPy, rFpPz);
    }
}
