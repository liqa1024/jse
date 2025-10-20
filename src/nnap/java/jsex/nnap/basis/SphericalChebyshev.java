package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.matrix.ColumnMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

/**
 * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这是 NNAP 中默认使用的原子基组。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * 现在统一通过调用 c 并借助 avx 指令优化来得到最佳的性能
 * <p>
 * References:
 * <a href="https://link.springer.com/article/10.1007/s40843-024-2953-9">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * @author Su Rui, liqa
 */
public class SphericalChebyshev extends WTypeBasis {
    final static int[] L3NCOLS = {0, 0, 2, 4, 9, 14, 23}, L3NCOLS_NOCROSS = {0, 0, 1, 1, 2, 2, 3};
    final static int[] L4NCOLS = {0, 1, 3, 9}, L4NCOLS_NOCROSS = {0, 1, 2, 3};
    
    public final static int DEFAULT_NMAX = 5;
    public final static int DEFAULT_LMAX = 6;
    public final static int DEFAULT_L3MAX = 0;
    public final static int DEFAULT_L4MAX = 0;
    public final static boolean DEFAULT_NORADIAL = false;
    public final static boolean DEFAULT_L3CROSS = true;
    public final static boolean DEFAULT_L4CROSS = true;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    final String @Nullable[] mSymbols;
    final int mLMax, mL3Max, mL4Max;
    final boolean mNoRadial, mL3Cross, mL4Cross;
    final double mRCut;
    
    final int mSizeL, mSize;
    final int mLMaxMax, mLMAll;
    
    final @Nullable Vector mPostFuseWeight;
    final int mPostFuseSize;
    
    SphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, boolean aL3Cross, int aL4Max, boolean aL4Cross, double aRCut,
                       int aWType, @Nullable ColumnMatrix aFuseWeight, @Nullable Vector aPostFuseWeight) {
        super(aTypeNum, aNMax, aWType, aFuseWeight);
        if (aLMax<0 || aLMax>12) throw new IllegalArgumentException("Input lmax MUST be in [0, 12], input: "+aLMax);
        if (aL3Max<0 || aL3Max>6) throw new IllegalArgumentException("Input l3max MUST be in [0, 6], input: "+aL3Max);
        if (aL4Max<0 || aL4Max>3) throw new IllegalArgumentException("Input l4max MUST be in [0, 3], input: "+aL3Max);
        mSymbols = aSymbols;
        mLMax = aLMax;
        mL3Max = aL3Max;
        mL4Max = aL4Max;
        mNoRadial = aNoRadial;
        mL3Cross = aL3Cross;
        mL4Cross = aL4Cross;
        mRCut = aRCut;
        
        mSizeL = (mNoRadial?mLMax:(mLMax+1)) + (mL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[mL3Max] + (mL4Cross?L4NCOLS:L4NCOLS_NOCROSS)[mL4Max];
        mLMaxMax = Math.max(Math.max(mLMax, mL3Max), mL4Max);
        mLMAll = (mLMaxMax+1)*(mLMaxMax+1);
        
        mPostFuseWeight = aPostFuseWeight;
        mPostFuseSize = mPostFuseWeight==null ? 0 : (mPostFuseWeight.size()/mSizeN);
        if (mPostFuseWeight != null) {
            if (mPostFuseWeight.size()!=mPostFuseSize*mSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
        }
        mSize = mPostFuseWeight==null ? (mSizeN*mSizeL) : (mPostFuseSize*mSizeL);
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(String @NotNull[] aSymbols, int aNMax, int aLMax, double aRCut) {
        this(aSymbols, aSymbols.length, aNMax, aLMax, DEFAULT_NORADIAL, DEFAULT_L3MAX, DEFAULT_L3CROSS, DEFAULT_L4MAX, DEFAULT_L4CROSS, aRCut, WTYPE_DEFAULT, null, null);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
        this(null, aTypeNum, aNMax, aLMax, DEFAULT_NORADIAL, DEFAULT_L3MAX, DEFAULT_L3CROSS, DEFAULT_L4MAX, DEFAULT_L4CROSS, aRCut, WTYPE_DEFAULT, null, null);
    }
    
    @Override public SphericalChebyshev threadSafeRef() {
        return new SphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mL4Max, mL4Cross, mRCut, mWType, mFuseWeight, mPostFuseWeight);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("noradial", mNoRadial);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l3cross", mL3Cross);
        rSaveTo.put("l4max", mL4Max);
        rSaveTo.put("l4cross", mL4Cross);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
        if (mPostFuseWeight!=null) {
            rSaveTo.put("post_fuse", true);
            rSaveTo.put("post_fuse_weight", mPostFuseWeight.asList());
        }
    }
    
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aWType = getWType_(aMap);
        ColumnMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aTypeNum, aNMax);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, aFuseWeight);
        Vector aPostFuseWeight = getPostFuseWeight_(aMap, tSizeN);
        return new SphericalChebyshev(
            aSymbols, aTypeNum, aNMax,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_NORADIAL, "noradial"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L4CROSS, "l4cross"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseWeight, aPostFuseWeight
        );
    }
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aWType = getWType_(aMap);
        ColumnMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aTypeNum, aNMax);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, aFuseWeight);
        Vector aPostFuseWeight = getPostFuseWeight_(aMap, tSizeN);
        return new SphericalChebyshev(
            null, aTypeNum, aNMax,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_NORADIAL, "noradial"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L4CROSS, "l4cross"),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseWeight, aPostFuseWeight
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getPostFuseWeight_(Map aMap, int aSizeN) {
        Object tFlag = aMap.get("post_fuse");
        if (tFlag==null || (!(Boolean)tFlag)) return null;
        Object tPostFuseWeight = aMap.get("post_fuse_weight");
        if (tPostFuseWeight != null) {
            return Vectors.from((List)tPostFuseWeight);
        }
        Object tPostFuseSize = aMap.get("post_fuse_size");
        if (tPostFuseSize == null) throw new IllegalArgumentException("Key `post_fuse_weight` or `post_fuse_size` required for post_fuse");
        return Vector.zeros(((Number)tPostFuseSize).intValue() * aSizeN);
    }
    
    @Override public void initParameters() {
        super.initParameters();
        // 补充对于 PostFuseWeight 的初始化
        if (mPostFuseWeight == null) return;
        mPostFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化
        int tShift = 0;
        for (int np = 0; np < mPostFuseSize; ++np) {
            IVector tSubVec = mPostFuseWeight.subVec(tShift, tShift + mSizeN);
            tSubVec.div2this(tSubVec.operation().norm1() / mSizeN);
            tShift += mSizeN;
        }
    }
    @Override public IVector parameters() {
        final IVector tPara = super.parameters();
        if (mPostFuseWeight == null) return tPara;
        // 补充对于 PostFuseWeight 的参数
        if (tPara == null) return mPostFuseWeight;
        final int tParaSize = tPara.size();
        final int tPostParaSize = mPostFuseWeight.size();
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx < tParaSize) {
                    return tPara.get(aIdx);
                }
                aIdx -= tParaSize;
                if (aIdx < tPostParaSize) {
                    return mPostFuseWeight.get(aIdx);
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                if (aIdx < tParaSize) {
                    tPara.set(aIdx, aValue);
                    return;
                }
                aIdx -= tParaSize;
                if (aIdx < tPostParaSize) {
                    mPostFuseWeight.set(aIdx, aValue);
                    return;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return tParaSize+tPostParaSize;
            }
        };
    }
    @Override public boolean hasParameters() {
        return super.hasParameters() || mPostFuseWeight!=null;
    }
    
    
    /** @return {@inheritDoc} */
    @Override public double rcut() {return mRCut;}
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)(lmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)(lmax+1)}
     */
    @Override public int size() {return mSize;}
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return mTypeNum;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    /**
     * {@inheritDoc}
     * @param aType
     * @return {@inheritDoc}
     */
    @Override public @Nullable String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    
    @Override protected int forwardCacheSize_(int aNN, boolean aFullCache) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType == WTYPE_FUSE) {
            return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll + (mNMax+1)*mLMAll) + mSizeN*mLMAll + tPostSize)
                              : (mNMax+1 + mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize);
        }
        if (mWType == WTYPE_RFUSE) {
            return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll + mFuseSize) + mSizeN*mLMAll + tPostSize)
                              : (mNMax+1 + mLMAll + mFuseSize + mSizeN*mLMAll + tPostSize);
        }
        return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll) + mSizeN*mLMAll + tPostSize)
                          : (mNMax+1 + mLMAll + mSizeN*mLMAll + tPostSize);
    }
    @Override protected int backwardCacheSize_(int aNN) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType == WTYPE_FUSE) {
            return mSizeN*mLMAll + tPostSize;
        }
        if (mWType == WTYPE_RFUSE) {
            return aNN*mFuseSize + mSizeN*mLMAll + tPostSize;
        }
        return tPostSize;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType == WTYPE_FUSE) {
            return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll + (mNMax+1)*mLMAll) + (mNMax+1) + 2*mLMAll + mSizeN*mLMAll + tPostSize)
                              : (4*(mNMax+1) + 5*mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize);
        }
        if (mWType == WTYPE_RFUSE) {
            return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll + mFuseSize) + (mNMax+1) + 2*mLMAll + mSizeN*mLMAll + tPostSize)
                              : (4*(mNMax+1) + 5*mLMAll + mFuseSize + mSizeN*mLMAll + tPostSize);
        }
        return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll) + (mNMax+1) + 2*mLMAll + mSizeN*mLMAll + tPostSize)
                          : (4*(mNMax+1) + 5*mLMAll + mSizeN*mLMAll + tPostSize);
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType == WTYPE_FUSE) {
            return mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize;
        }
        if (mWType == WTYPE_RFUSE) {
            return mLMAll + mNMax+1 + mSizeN*mLMAll + tPostSize;
        }
        return mLMAll + mSizeN*mLMAll + tPostSize;
    }
    
    @Override
    protected void forward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleArrayVector rForwardCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算基组
        forward0(aNlDx, aNlDy, aNlDz, aNlType, rFp, rForwardCache, aFullCache);
    }
    @Override
    protected void backward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleArrayVector aForwardCache, DoubleArrayVector rBackwardCache, boolean aKeepCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 如果不是 fuse 直接返回不走 native
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_RFUSE && mPostFuseWeight==null) return;
        // 如果不保留旧值则在这里清空
        if (!aKeepCache) rBackwardCache.fill(0.0);
        
        backward0(aNlDx, aNlDy, aNlDz, aNlType, aGradFp, rGradPara, aForwardCache, rBackwardCache);
    }
    @Override
    protected void forwardForceAccumulate_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleArrayVector aForwardCache, DoubleArrayVector rForwardForceCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算力
        forwardForce0(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache);
    }
    @Override
    protected void backwardForce_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                                  DoubleArrayVector aForwardCache, DoubleArrayVector aForwardForceCache, DoubleArrayVector rBackwardCache, DoubleArrayVector rBackwardForceCache, boolean aKeepCache, boolean aFixBasis) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 如果不保留旧值则在这里清空
        if (!aKeepCache) {
            rBackwardCache.fill(0.0);
            rBackwardForceCache.fill(0.0);
        }
        
        backwardForce0(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aFixBasis);
    }
    
    
    void forward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp, IDataShell<double[]> rForwardCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                 rFp.internalDataWithLengthCheck(mSize), rFp.internalDataShift(),
                 rForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, aFullCache)), rForwardCache.internalDataShift(), aFullCache,
                 mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mL4Max, mL4Cross, mWType,
                 mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                 mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize);
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                        int aL3Max, boolean aL3Cross, int aL4Max, boolean aL4Cross,
                                        int aWType, double[] aFuseWeight, int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize);
    
    void backward0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aGradFp, IDataShell<double[]> rGradPara, IDataShell<double[]> aForwardCache, IDataShell<double[]> rBackwardCache) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = 0;
        if (mFuseWeight!=null) tParaSize += mFuseWeight.internalDataSize();
        if (mPostFuseWeight!=null) tParaSize += mPostFuseWeight.internalDataSize();
        backward1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                  aGradFp.internalDataWithLengthCheck(mSize), aGradFp.internalDataShift(),
                  rGradPara.internalDataWithLengthCheck(tParaSize), rGradPara.internalDataShift(),
                  aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                  rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                  mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mL4Max, mL4Cross, mWType, mFuseSize,
                  mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize);
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                         int aL3Max, boolean aL3Cross, int aL4Max, boolean aL4Cross,
                                         int aWType, int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize);
    
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mL4Max, mL4Cross, mWType,
                      mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                      mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize);
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                             int aL3Max, boolean aL3Cross, int aL4Max, boolean aL4Cross,
                                             int aWType, double[] aFuseWeight, int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize);
    
    void backwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                        IDataShell<double[]> aNNGrad, IDataShell<double[]> aGradFx, IDataShell<double[]> aGradFy, IDataShell<double[]> aGradFz,
                        IDataShell<double[]> rGradNNGrad, @Nullable IDataShell<double[]> rGradPara,
                        IDataShell<double[]> aForwardCache, IDataShell<double[]> aForwardForceCache,
                        IDataShell<double[]> rBackwardCache, IDataShell<double[]> rBackwardForceCache, boolean aFixBasis) {
        int tNN = aNlDx.internalDataSize();
        int tParaSize = 0;
        if (mFuseWeight!=null) tParaSize += mFuseWeight.internalDataSize();
        if (mPostFuseWeight!=null) tParaSize += mPostFuseWeight.internalDataSize();
        boolean tNoPassGradPara = tParaSize==0 || aFixBasis;
        if (!tNoPassGradPara && rGradPara==null) throw new NullPointerException();
        backwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                       aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), aGradFx.internalDataWithLengthCheck(tNN, 0), aGradFy.internalDataWithLengthCheck(tNN, 0), aGradFz.internalDataWithLengthCheck(tNN, 0),
                       rGradNNGrad.internalDataWithLengthCheck(mSize), rGradNNGrad.internalDataShift(),
                       tNoPassGradPara?null:rGradPara.internalDataWithLengthCheck(tParaSize), tNoPassGradPara?0:rGradPara.internalDataShift(),
                       aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                       aForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, true)), aForwardForceCache.internalDataShift(),
                       rBackwardCache.internalDataWithLengthCheck(backwardCacheSize_(tNN)), rBackwardCache.internalDataShift(),
                       rBackwardForceCache.internalDataWithLengthCheck(backwardForceCacheSize_(tNN)), rBackwardForceCache.internalDataShift(), aFixBasis,
                       mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL3Cross, mL4Max, mL4Cross, mWType,
                       mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                       mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize);
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int aBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                              int aL3Max, boolean aL3Cross, int aL4Max, boolean aL4Cross,
                                              int aWType, double[] aFuseWeight, int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize);
}
