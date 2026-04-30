package jsex.nnap.basis;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IAtomData;
import jse.atom.IHasSymbol;
import jse.cache.VectorCache;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.io.ISavable;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * 通用的纯 java 的简单 nnap 基组/描述符实现
 * @author liqa
 */
@ApiStatus.Experimental
public abstract class SimpleBasis implements IHasSymbol, AutoCloseable {
    
    /** @return 基组需要的近邻截断半径 */
    public abstract double rcut();
    /** @return 基组的长度 */
    public abstract int size();
    
    /** @return {@inheritDoc} */
    @Override public int ntypes() {return 1;}
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
    public final boolean isClosed() {
        return mDead;
    }
    @Override public final void close() {
        if (mDead) return;
        mDead = true;
        close_();
    }
    protected void close_() {/**/}
    
    
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
        typeMapCheck(aAPC.ntypes(), aTypeMap);
        // 构造近邻列表缓存
        initCacheNl_();
        final int tTypeNum = ntypes();
        // 缓存情况需要先清空这些
        mNlDx.clear(); mNlDy.clear(); mNlDz.clear();
        mNlType.clear();
        aAPC.nl_().forEachNeighbor(aIdx, rcut(), (dx, dy, dz, idx) -> {
            int type = aTypeMap.applyAsInt(aAPC.types().get(idx));
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
        if (isClosed()) throw new IllegalStateException("This Basis is dead");
        IntUnaryOperator tTypeMap = hasSymbol() ? typeMap(aAtomData) : type->type;
        int tAtomNum = aAtomData.natoms();
        List<Vector> rFps = VectorCache.getVec(size(), tAtomNum);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                eval(tAPC, i, tTypeMap, rFps.get(i));
            }
        }
        return rFps;
    }
}
