package jsex.nnap.basis;

import jse.cache.VectorCache;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.parallel.ParforThreadPool;
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
    final static int[] L3NCOLS = {0, 0, 2, 4, 9, 14, 23};
    final static int[] L4NCOLS = {0, 1, 3, 9};
    
    public final static int DEFAULT_NMAX = 5;
    public final static int DEFAULT_LMAX = 6;
    public final static int DEFAULT_L3MAX = 0;
    public final static int DEFAULT_L4MAX = 0;
    public final static boolean DEFAULT_NORADIAL = false;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    public final static class Conf {
        /**
         * 是否开启针对多项式的缩放。
         * 目前所有方案效果都不显著且还在实验，因此默认关闭
         */
        public static boolean POLY_SCALE = OS.envZ("JSE_NNAP_POLY_SCALE", false);
    }
    
    final String @Nullable[] mSymbols;
    final int mLMax, mL3Max, mL4Max;
    final boolean mNoRadial;
    final double mRCut;
    
    final int mSizeL;
    final int mLMaxMax, mLMAll;
    int mSize;
    
    final Vector mPostFuseWeight;
    final int mPostFuseSize;
    final double[] mPostFuseScale;
    
    final Vector mRFuncScale;
    final Vector mSystemScale;
    final boolean[] mPolyScale, mAnyScale;
    
    private SphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aLMaxMax, boolean aNoRadial, int aL3Max, int aL4Max, double aRCut,
                               int aWType, int aFuseStyle, @Nullable RowMatrix aFuseWeight, @Nullable Vector aPostFuseWeight, double @Nullable[] aPostFuseScale,
                               @Nullable Vector aRFuncScale, @Nullable Vector aSystemScale, boolean @Nullable[] aPolyScale, boolean @Nullable[] aAnyScale) {
        super(aTypeNum, aNMax, aLMaxMax, aWType, aFuseStyle, aFuseWeight);
        if (aLMaxMax<0 || aLMaxMax>12) throw new IllegalArgumentException("Input lmax MUST be in [0, 12], input: "+aLMaxMax);
        if (aL3Max<0 || aL3Max>6) throw new IllegalArgumentException("Input l3max MUST be in [0, 6], input: "+aL3Max);
        if (aL4Max<0 || aL4Max>3) throw new IllegalArgumentException("Input l4max MUST be in [0, 3], input: "+aL3Max);
        mSymbols = aSymbols;
        mLMax = aLMax;
        mL3Max = aL3Max;
        mL4Max = aL4Max;
        mNoRadial = aNoRadial;
        mRCut = aRCut;
        
        mSizeL = (mNoRadial?mLMax:(mLMax+1)) + L3NCOLS[mL3Max] + L4NCOLS[mL4Max];
        mLMaxMax = aLMaxMax;
        if (mLMaxMax!=Math.max(Math.max(mLMax, mL3Max), mL4Max)) throw new IllegalStateException();
        mLMAll = (mLMaxMax+1)*(mLMaxMax+1);
        
        mPostFuseWeight = aPostFuseWeight;
        if (mPostFuseWeight==null) {
            mPostFuseSize = 0;
        } else {
            if (mFuseStyle==FUSE_STYLE_LIMITED) {
                mPostFuseSize = mPostFuseWeight.size()/mSizeN;
            } else {
                mPostFuseSize = mPostFuseWeight.size()/mSizeN/(mLMaxMax+1);
            }
        }
        if (mPostFuseWeight!=null) {
            if (mFuseStyle==FUSE_STYLE_LIMITED) {
                if (mPostFuseWeight.size()!=mPostFuseSize*mSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
            } else {
                if (mPostFuseWeight.size()!=mPostFuseSize*mSizeN*(mLMaxMax+1)) throw new IllegalArgumentException("Size of post fuse weight mismatch");
            }
        }
        mPostFuseScale = aPostFuseScale==null ? new double[]{1.0} : aPostFuseScale;
        if (mPostFuseScale.length!=1) throw new IllegalArgumentException("Size of post fuse scale mismatch");
        mSize = mPostFuseWeight==null ? (mSizeN*mSizeL) : (mPostFuseSize*mSizeL);
        
        mRFuncScale = aRFuncScale==null ? Vector.ones(mNMax+1) : aRFuncScale;
        mSystemScale = aSystemScale==null ? Vector.ones(mSizeN*(mLMaxMax+1)) : aSystemScale;
        mPolyScale = aPolyScale==null ? new boolean[]{false} : aPolyScale;
        mAnyScale = aAnyScale==null ? new boolean[]{aRFuncScale!=null || aSystemScale!=null || aPolyScale!=null} : aAnyScale;
        
        if (mRFuncScale.size()!=mNMax+1) throw new IllegalArgumentException("Size of rfunc scale mismatch");
        if (mSystemScale.size()!=mSizeN*(mLMaxMax+1)) throw new IllegalArgumentException("Size of system scale mismatch");
        if (mPolyScale.length!=1) throw new IllegalArgumentException("Size of poly scale mismatch");
        if (mAnyScale.length!=1) throw new IllegalArgumentException("Size of any scale mismatch");
    }
    SphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, int aL4Max, double aRCut, int aWType, int aFuseStyle, RowMatrix aFuseWeight, Vector aPostFuseWeight, double[] aPostFuseScale, Vector aRFuncScale, Vector aSystemScale, boolean[] aPolyScale, boolean[] aAnyScale) {
        this(aSymbols, aTypeNum, aNMax, aLMax, Math.max(Math.max(aLMax, aL3Max), aL4Max), aNoRadial, aL3Max, aL4Max, aRCut, aWType, aFuseStyle, aFuseWeight, aPostFuseWeight, aPostFuseScale, aRFuncScale, aSystemScale, aPolyScale, aAnyScale);
    }
    @Override public SphericalChebyshev threadSafeRef() {
        return new SphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mNoRadial, mL3Max, mL4Max, mRCut, mWType, mFuseStyle, mFuseWeight, mPostFuseWeight, mPostFuseScale, mRFuncScale, mSystemScale, mPolyScale, mAnyScale);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("noradial", mNoRadial);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l4max", mL4Max);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        if (mFuseWeight!=null || mPostFuseWeight!=null) {
            rSaveTo.put("fuse_style", ALL_FUSE_STYLE.inverse().get(mFuseStyle));
        }
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
        if (mAnyScale[0]) {
            rSaveTo.put("rfunc_scales", mRFuncScale.asList());
            rSaveTo.put("system_scales", mSystemScale.asList());
            rSaveTo.put("poly_scale", mPolyScale[0]);
        }
        rSaveTo.put("post_fuse", mPostFuseWeight!=null);
        if (mPostFuseWeight!=null) {
            rSaveTo.put("post_fuse_size", mPostFuseSize);
            rSaveTo.put("post_fuse_scale", mPostFuseScale[0]);
            rSaveTo.put("post_fuse_weight", mPostFuseWeight.asList());
        }
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SphericalChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l3cross")) throw new IllegalArgumentException("no l3cross is invalid now.");
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l4cross")) throw new IllegalArgumentException("no l4cross is invalid now.");
        int tLMaxMax = Math.max(Math.max(aLMax, aL3Max), aL4Max);
        int aWType = getWType_(aMap);
        int aFuseStyle = getFuseStyle_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax, tLMaxMax);
        int tFuseSize = getFuseSize(aWType, aFuseStyle, aNMax, tLMaxMax, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, tFuseSize);
        Vector aPostFuseWeight = getPostFuseWeight_(aMap, aFuseStyle, tSizeN, tLMaxMax);
        double[] aPostFuseScale = aPostFuseWeight==null ? null : new double[1];
        if (aPostFuseWeight!=null) {
            Object tPostFuseScale = aMap.get("post_fuse_scale");
            aPostFuseScale[0] = tPostFuseScale==null ? 1.0 : ((Number)tPostFuseScale).doubleValue();
        }
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        Object tPolyScale = UT.Code.get(aMap, "poly_scale");
        boolean[] aPolyScale = tPolyScale==null ? null : new boolean[]{(Boolean)tPolyScale};
        return new SphericalChebyshev(
            aSymbols, aTypeNum, aNMax,
            aLMax, (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_NORADIAL, "noradial"),
            aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight, aPostFuseWeight, aPostFuseScale,
            aRFuncScales, aSystemScale, aPolyScale, null
        );
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SphericalChebyshev load(int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l3cross")) throw new IllegalArgumentException("no l3cross is invalid now.");
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l4cross")) throw new IllegalArgumentException("no l4cross is invalid now.");
        int tLMaxMax = Math.max(Math.max(aLMax, aL3Max), aL4Max);
        int aWType = getWType_(aMap);
        int aFuseStyle = getFuseStyle_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax, tLMaxMax);
        int tFuseSize = getFuseSize(aWType, aFuseStyle, aNMax, tLMaxMax, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, tFuseSize);
        Vector aPostFuseWeight = getPostFuseWeight_(aMap, aFuseStyle, tSizeN, tLMaxMax);
        double[] aPostFuseScale = aPostFuseWeight==null ? null : new double[1];
        if (aPostFuseWeight!=null) {
            Object tPostFuseScale = aMap.get("post_fuse_scale");
            aPostFuseScale[0] = tPostFuseScale==null ? 1.0 : ((Number)tPostFuseScale).doubleValue();
        }
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        Object tPolyScale = UT.Code.get(aMap, "poly_scale");
        boolean[] aPolyScale = tPolyScale==null ? null : new boolean[]{(Boolean)tPolyScale};
        return new SphericalChebyshev(
            null, aTypeNum, aNMax,
            aLMax, (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_NORADIAL, "noradial"),
            aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight, aPostFuseWeight, aPostFuseScale,
            aRFuncScales, aSystemScale, aPolyScale, null
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getPostFuseWeight_(Map aMap, int aFuseStyle, int aSizeN, int aLMaxMax) {
        Object tFlag = aMap.get("post_fuse");
        if (tFlag==null || (!(Boolean)tFlag)) return null;
        Object tPostFuseSize = aMap.get("post_fuse_size");
        Object tPostFuseWeight = aMap.get("post_fuse_weight");
        if (tPostFuseWeight!=null) {
            Vector tVec = Vectors.from((List)tPostFuseWeight);
            if (tPostFuseSize!=null) {
                if (aFuseStyle==FUSE_STYLE_LIMITED) {
                    if (tVec.size()!=((Number)tPostFuseSize).intValue()*aSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
                } else
                if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
                    if (tVec.size()!=((Number)tPostFuseSize).intValue()*aSizeN*(aLMaxMax+1)) throw new IllegalArgumentException("Size of post fuse weight mismatch");
                } else {
                    throw new IllegalStateException();
                }
            }
            return tVec;
        }
        if (tPostFuseSize==null) throw new IllegalArgumentException("Key `post_fuse_weight` or `post_fuse_size` required for post_fuse");
        int tSize;
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            tSize = ((Number)tPostFuseSize).intValue()*aSizeN;
        } else
        if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
            tSize = ((Number)tPostFuseSize).intValue()*aSizeN*(aLMaxMax+1);
        } else {
            throw new IllegalStateException();
        }
        return Vector.zeros(tSize);
    }
    
    @Override public void initParameters() {
        super.initParameters();
        // 补充对于 PostFuseWeight 的初始化
        if (mPostFuseWeight == null) return;
        mPostFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这个可以有效加速训练；经验设定
        int tShift = 0;
        for (int np = 0; np < mPostFuseSize; ++np) {
            if (mFuseStyle==FUSE_STYLE_LIMITED) {
                IVector tSubVec = mPostFuseWeight.subVec(tShift, tShift + mSizeN);
                tSubVec.div2this(tSubVec.operation().norm1() / mSizeN);
                tShift += mSizeN;
            } else {
                // extensive 情况下会先遍历 l，因此归一化需要这样进行
                for (int l = 0; l <= mLMaxMax; ++l) {
                    double tNorm1 = 0.0;
                    for (int n = 0; n < mSizeN; ++n) {
                        tNorm1 += Math.abs(mPostFuseWeight.get(tShift + n*(mLMaxMax+1) + l));
                    }
                    final double tDiv = tNorm1 / mSizeN;
                    for (int n = 0; n < mSizeN; ++n) {
                        mPostFuseWeight.update(tShift + n*(mLMaxMax+1) + l, v -> v/tDiv);
                    }
                }
                tShift += mSizeN*(mLMaxMax+1);
            }
        }
        // 在这个经验设定下，scale 设置为此值确保输出的基组值数量级一致
        mPostFuseScale[0] = MathEX.Fast.sqrt(1.0 / mSizeN);
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
        if (mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE) {
            return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll + (mNMax+1)*mLMAll) + mSizeN*mLMAll + tPostSize + mSize)
                              : (mNMax+1 + mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize + mSize);
        }
        return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll) + mSizeN*mLMAll + tPostSize + mSize)
                          : (mNMax+1 + mLMAll + mSizeN*mLMAll + tPostSize + mSize);
    }
    @Override protected int backwardCacheSize_(int aNN) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE) {
            return mSizeN*mLMAll + tPostSize + mSize;
        }
        return tPostSize + mSize;
    }
    @Override protected int forwardForceCacheSize_(int aNN, boolean aFullCache) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE) {
            return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll + (mNMax+1)*mLMAll) + (mNMax+1) + 2*mLMAll + mSizeN*mLMAll + tPostSize + mSize)
                              : (4*(mNMax+1) + 5*mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize + mSize);
        }
        return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll) + (mNMax+1) + 2*mLMAll + mSizeN*mLMAll + tPostSize + mSize)
                          : (4*(mNMax+1) + 5*mLMAll + mSizeN*mLMAll + tPostSize + mSize);
    }
    @Override protected int backwardForceCacheSize_(int aNN) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE) {
            return mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize + mSize;
        }
        return mLMAll + mSizeN*mLMAll + tPostSize + mSize;
    }
    
    @Override public void initScale(List<DoubleList> aNlDxList, List<DoubleList> aNlDyList, List<DoubleList> aNlDzList, List<IntList> aNlTypeList, ParforThreadPool aPool) {
        mAnyScale[0] = true;
        mPolyScale[0] = Conf.POLY_SCALE;
        // 先初始化径向函数的缩放
        for (int n = 0; n <= mNMax; ++n) {
            final int fn = n;
            double tScale = MathEX.Adv.integral(0.0, 1.0, 1000, r -> Math.abs(MathEX.Func.chebyshev(fn, 1 - 2*r)));
            mRFuncScale.set(fn, 1.0/tScale);
        }
        // 遍历统计系统 scale
        final int tThreadNum = aPool.threadNumber();
        final List<Vector> tScaleTotPar = VectorCache.getZeros(mSizeN*(mLMaxMax+1), tThreadNum);
        final List<Vector> tScalePar = VectorCache.getVec(mSizeN*(mLMaxMax+1), tThreadNum);
        final List<DoubleList> rForwardCachePar = NewCollections.from(tThreadNum, i -> new DoubleList(16));
        final int tSize = aNlDxList.size();
        aPool.parfor(tSize, (i, threadID) -> {
            Vector tScaleTot = tScaleTotPar.get(threadID);
            Vector tScale = tScalePar.get(threadID);
            calSystemScale(aNlDxList.get(i), aNlDyList.get(i), aNlDzList.get(i), aNlTypeList.get(i), tScale, rForwardCachePar.get(threadID));
            tScale.abs2this();
            tScaleTot.plus2this(tScale);
        });
        Vector tScaleTot = tScaleTotPar.get(0);
        for (int i = 1; i < tThreadNum; ++i) tScaleTot.plus2this(tScaleTotPar.get(i));
        tScaleTot.div2this(tSize);
        tScaleTot.operation().ldiv2this(1.0);
        mSystemScale.fill(tScaleTot);
        VectorCache.returnVec(tScalePar);
        VectorCache.returnVec(tScaleTotPar);
    }
    void calSystemScale(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rSystemScale, DoubleList rForwardCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        validCache_(rForwardCache, forwardCacheSize_(aNlDx.size(), false));
        calSystemScale0(aNlDx, aNlDy, aNlDz, aNlType, rSystemScale, rForwardCache);
    }
    void calSystemScale0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rSystemScale, IDataShell<double[]> rForwardCache) {
        int tNN = aNlDx.internalDataSize();
        calSystemScale1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                        rSystemScale.internalDataWithLengthCheck(mSizeN*(mLMaxMax+1)), rSystemScale.internalDataShift(),
                        rForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, false)), rForwardCache.internalDataShift(),
                        mTypeNum, mRCut, mNMax, mLMaxMax, mWType, mFuseSize,
                        mRFuncScale.internalDataWithLengthCheck());
    }
    private static native void calSystemScale1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                               double[] rSystemScale, int aShiftSystemScale, double[] rForwardCache, int aForwardCacheShift,
                                               int aTypeNum, double aRCut, int aNMax, int aLMaxMax, int aWType, int aFuseSize,
                                               double[] aRFuncScale);
    
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
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_EXFUSE && mPostFuseWeight==null) return;
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
                 mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL4Max, mWType, mFuseStyle,
                 mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                 mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize, mPostFuseScale[0],
                 mRFuncScale.internalDataWithLengthCheck(), mSystemScale.internalDataWithLengthCheck(), mPolyScale[0], mAnyScale[0]);
    }
    private static native void forward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN, double[] rFp, int aShiftFp,
                                        double[] rForwardCache, int aForwardCacheShift, boolean aFullCache,
                                        int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                        int aL3Max, int aL4Max, int aWType, int aFuseStyle,
                                        double[] aFuseWeight, int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize, double aPostFuseScale,
                                        double[] aRFuncScale, double[] aSystemScale, boolean aPolyScale, boolean aSphScale);
    
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
                  mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL4Max, mWType,
                  mFuseStyle, mFuseSize,
                  mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize, mPostFuseScale[0],
                  mSystemScale.internalDataWithLengthCheck(), mPolyScale[0], mAnyScale[0]);
    }
    private static native void backward1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                         double[] aGradFp, int aShiftGradFp, double[] rGradPara, int aShiftGradPara,
                                         double[] aForwardCache, int aForwardCacheShift, double[] rBackwardCache, int aBackwardCacheShift,
                                         int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                         int aL3Max, int aL4Max, int aWType, int aFuseStyle,
                                         int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize, double aPostFuseScale,
                                         double[] aSystemScale, boolean aPolyScale, boolean aSphScale);
    
    void forwardForce0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> aNNGrad, IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, IDataShell<double[]> aForwardCache, IDataShell<double[]> rForwardForceCache, boolean aFullCache) {
        int tNN = aNlDx.internalDataSize();
        forwardForce1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                      aNNGrad.internalDataWithLengthCheck(mSize), aNNGrad.internalDataShift(), rFx.internalDataWithLengthCheck(tNN, 0), rFy.internalDataWithLengthCheck(tNN, 0), rFz.internalDataWithLengthCheck(tNN, 0),
                      aForwardCache.internalDataWithLengthCheck(forwardCacheSize_(tNN, true)), aForwardCache.internalDataShift(),
                      rForwardForceCache.internalDataWithLengthCheck(forwardForceCacheSize_(tNN, aFullCache)), rForwardForceCache.internalDataShift(), aFullCache,
                      mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL4Max, mWType, mFuseStyle,
                      mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                      mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize, mPostFuseScale[0],
                      mRFuncScale.internalDataWithLengthCheck(), mSystemScale.internalDataWithLengthCheck(), mPolyScale[0], mAnyScale[0]);
    }
    private static native void forwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                             double[] aNNGrad, int aShiftNNGrad, double[] rFx, double[] rFy, double[] rFz,
                                             double[] aForwardCache, int aForwardCacheShift, double[] rForwardForceCache, int aForwardForceCacheShift, boolean aFullCache,
                                             int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                             int aL3Max, int aL4Max, int aWType, int aFuseStyle,
                                             double[] aFuseWeight, int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize, double aPostFuseScale,
                                             double[] aRFuncScale, double[] aSystemScale, boolean aPolyScale, boolean aSphScale);
    
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
                       mTypeNum, mRCut, mNMax, mLMax, mNoRadial, mL3Max, mL4Max, mWType, mFuseStyle,
                       mFuseWeight==null?null:mFuseWeight.internalDataWithLengthCheck(), mFuseSize,
                       mPostFuseWeight==null?null:mPostFuseWeight.internalDataWithLengthCheck(), mPostFuseSize, mPostFuseScale[0],
                       mSystemScale.internalDataWithLengthCheck(), mPolyScale[0], mAnyScale[0]);
    }
    private static native void backwardForce1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                              double[] aNNGrad, int aShiftNNGrad, double[] aGradFx, double[] aGradFy, double[] aGradFz,
                                              double[] rGradNNGrad, int aShiftGradNNGrad, double[] rGradPara, int aShiftGradPara,
                                              double[] aForwardCache, int aForwardCacheShift, double[] aForwardForceCache, int aForwardForceCacheShift,
                                              double[] rBackwardCache, int aBackwardCacheShift, double[] rBackwardForceCache, int aBackwardForceCacheShift, boolean aFixBasis,
                                              int aTypeNum, double aRCut, int aNMax, int aLMax, boolean aNoRadial,
                                              int aL3Max, int aL4Max, int aWType, int aFuseStyle,
                                              double[] aFuseWeight, int aFuseSize, double[] aPostFuseWeight, int aPostFuseSize, double aPostFuseScale,
                                              double[] aSystemScale, boolean aPolyScale, boolean aSphScale);
}
