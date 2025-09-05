package jsex.nnap.basis;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IAtomData;
import jse.atom.IHasSymbol;
import jse.cache.VectorCache;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.io.ISavable;
import jse.math.vector.*;
import jse.parallel.IAutoShutdown;
import jsex.nnap.NNAP;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * 通用的 nnap 基组/描述符实现
 * <p>
 * 由于内部会缓存近邻列表，因此此类相同实例线程不安全，而不同实例之间线程安全；
 * 可以通过 {@link #threadSafeRef()} 来创建一个线程安全的引用（拷贝内部缓存）
 *
 * @author liqa
 */
@ApiStatus.Experimental
public abstract class Basis implements IHasSymbol, ISavable, IAutoShutdown {
    static {
        // 依赖 nnap
        NNAP.InitHelper.init();
    }
    
    /** 提供直接加载完整基组的通用接口 */
    @SuppressWarnings("rawtypes")
    public static Basis[] load(String @Nullable[] aSymbols, List aData) {
        final int tTypeNum = aData.size();
        if (aSymbols!=null && aSymbols.length!=tTypeNum) throw new IllegalArgumentException("Input size of symbols and data list mismatch");
        Basis[] rBasis = new Basis[tTypeNum];
        for (int i = 0; i < tTypeNum; ++i) {
            Map tBasisMap = (Map)aData.get(i);
            Object tBasisType = tBasisMap.get("type");
            if (tBasisType == null) {
                tBasisType = "spherical_chebyshev";
            }
            switch(tBasisType.toString()) {
            case "mirror": {
                break; // mirror 情况延迟初始化
            }
            case "spherical_chebyshev": {
                rBasis[i] = aSymbols==null ? SphericalChebyshev.load(tTypeNum, tBasisMap)
                                           : SphericalChebyshev.load(aSymbols, tBasisMap);
                break;
            }
            case "chebyshev": {
                rBasis[i] = aSymbols==null ? Chebyshev.load(tTypeNum, tBasisMap)
                                           : Chebyshev.load(aSymbols, tBasisMap);
                break;
            }
            case "merge": {
                rBasis[i] = aSymbols==null ? Merge.load(tTypeNum, tBasisMap)
                                           : Merge.load(aSymbols, tBasisMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
            }}
        }
        for (int i = 0; i < tTypeNum; ++i) {
            Map tBasisMap = (Map)aData.get(i);
            Object tBasisType = tBasisMap.get("type");
            if (!tBasisType.equals("mirror")) continue;
            Object tMirror = tBasisMap.get("mirror");
            if (tMirror == null) throw new IllegalArgumentException("Key `mirror` required for basis mirror");
            int tMirrorType = ((Number)tMirror).intValue();
            rBasis[i] = new Mirror(rBasis[tMirrorType-1], tMirrorType, i+1);
        }
        return rBasis;
    }
    @SuppressWarnings("rawtypes")
    public static Basis[] load(List aData) {
        return load(null, aData);
    }
    
    /** @return 线程安全的引用对象，保证读取调用是线程安全的 */
    public abstract Basis threadSafeRef();
    /** 随机初始化内部可能存在的可拟合参数 */
    public void initParameters() {/**/}
    
    /** @return 内部可能存在的可拟合参数组成的向量 */
    public @Nullable IVector parameters() {return null;}
    /** @return 是否确实存在可拟合的参数 */
    public boolean hasParameters() {return false;}
    
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
    
    
    /**
     * 单纯的向前传播获取输出基组，并可选输出先前传播过程中的缓存值
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param rFp 计算输出的原子描述符向量
     * @param rForwardCache 计算向前传播过程中需要使用的缓存，会自动扩容到合适长度
     * @param aFullCache 是否进行完整缓存，以供反向传播使用
     */
    @ApiStatus.Internal
    public abstract void forward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rForwardCache, boolean aFullCache);
    
    /**
     * 单纯的反向传播计算关于可拟合参量梯度，默认没有实现因为一般不存在可拟合参数。
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param aGradFp 输入已有的 (loss) 关于基组的梯度
     * @param rGradPara 计算输出的 (loss) 关于可拟合参数的梯度
     * @param aForwardCache 需要的向前传播的完整缓存值
     * @param rBackwardCache 计算反向传播过程中需要使用的缓存，会自动扩容到合适长度
     * @param aKeepCache 标记是否保留输入的 {@code rBackwardCache} 旧值，在某些情况需要这些值来实现带有依赖的反向传播
     */
    @ApiStatus.Internal
    public void backward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleList aForwardCache, DoubleList rBackwardCache, boolean aKeepCache) {/**/}
    
    /**
     * 结合了反向传播的向前传播计算力，并可选输出传播过程中的缓存值
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param aNNGrad 神经网络输出关于基组的梯度
     * @param rFx 计算得到的原子对于近邻原子 x 方向的力，要求已经是合适的大小
     * @param rFy 计算得到的原子对于近邻原子 y 方向的力，要求已经是合适的大小
     * @param rFz 计算得到的原子对于近邻原子 z 方向的力，要求已经是合适的大小
     * @param aForwardCache 需要的向前传播的完整缓存值
     * @param rForwardForceCache 计算输出的力向前传播过程中的缓存值，会自动扩容到合适长度
     * @param aFullCache 是否进行完整缓存，以供反向传播使用
     */
    @ApiStatus.Internal
    public abstract void forwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleList aForwardCache, DoubleList rForwardForceCache, boolean aFullCache);
    
    /**
     * 计算力的反向传播版本，用于拟合力时反向传播 loss 的梯度
     * @param aNlDx 由近邻原子的 dx 组成的列表
     * @param aNlDy 由近邻原子的 dy 组成的列表
     * @param aNlDz 由近邻原子的 dz 组成的列表
     * @param aNlType 由近邻原子的 type 组成的列表
     * @param aNNGrad 神经网络输出关于基组的梯度
     * @param aGradFx (loss) 关于近邻原子 x 方向力的梯度
     * @param aGradFy (loss) 关于近邻原子 y 方向力的梯度
     * @param aGradFz (loss) 关于近邻原子 z 方向力的梯度
     * @param rGradNNGrad 反向传播计算得到的 (loss) 关于网络梯度的梯度
     * @param rGradPara 可选计算输出的 (loss) 关于可拟合参数的梯度
     * @param aForwardCache 需要的向前传播的完整缓存值
     * @param aForwardForceCache 需要的力向前传播的完整缓存值
     * @param rBackwardCache 可选计算输出的对于 backward 缓存使用的修改，部分基组会存在此依赖项，会自动扩容到合适长度
     * @param rBackwardForceCache 计算力反向传播过程中需要使用的缓存，会自动扩容到合适长度
     * @param aKeepCache 标记是否保留输入的 {@code rBackwardForceCache} 旧值
     * @param aFixBasis 标记是否固定基组，当固定基组时不会进行考虑基组可变的反向传播分支（rGradPara）
     */
    @ApiStatus.Internal
    public abstract void backwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                                       DoubleList aForwardCache, DoubleList aForwardForceCache, DoubleList rBackwardCache, DoubleList rBackwardForceCache, boolean aKeepCache, boolean aFixBasis);
    
    
    private DoubleList mNlDx = null, mNlDy = null, mNlDz = null;
    private IntList mNlType = null;
    private final DoubleList mForwardCache = new DoubleList(128);
    
    private void initCacheNl_() {
        if (mNlDx != null) return;
        mNlDx = new DoubleList(16);
        mNlDy = new DoubleList(16);
        mNlDz = new DoubleList(16);
        mNlType = new IntList(16);
    }
    static void validCache_(DoubleList rCache, int aSize) {
        rCache.ensureCapacity(aSize);
        rCache.setInternalDataSize(aSize);
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
        forward(aNlDx, aNlDy, aNlDz, aNlType, rFp, mForwardCache, false);
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
        // 构造近邻列表缓存
        initCacheNl_();
        final int tTypeNum = atomTypeNumber();
        // 缓存情况需要先清空这些
        mNlDx.clear(); mNlDy.clear(); mNlDz.clear();
        mNlType.clear();
        aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
            int type = aTypeMap.applyAsInt(aAPC.atomType_().get(idx));
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            mNlDx.add(dx); mNlDy.add(dy); mNlDz.add(dz);
            mNlType.add(type);
        });
        eval(mNlDx, mNlDy, mNlDz, mNlType, rFp);
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
}
