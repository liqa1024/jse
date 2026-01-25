package jsex.nnap.basis;

import jse.code.UT;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

/**
 * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这是 NNAP 中默认使用的原子基组。
 * @author liqa
 */
public class SphericalChebyshev2 extends WTypeBasis2 {
    final static int[] L3NCOLS = {0, 0, 2, 4, 9, 14, 23};
    final static int[] L4NCOLS = {0, 1, 3, 9};
    
    public final static int DEFAULT_NMAX = 5;
    public final static int DEFAULT_LMAX = 6;
    public final static int DEFAULT_L3MAX = 0;
    public final static int DEFAULT_L4MAX = 0;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    final int mLMax, mL3Max, mL4Max;
    final boolean mNoRadial;
    final double mRCut;
    
    final int mSizeL;
    final int mLMaxMax, mLMAll;
    final int mSize;
    
    final Vector mPostFuseWeight;
    final int mPostFuseSize;
    final double[] mPostFuseScale;
    
    private SphericalChebyshev2(int aThisType, int aTypeNum, int aNMax, int aLMax, int aLMaxMax, boolean aNoRadial, int aL3Max, int aL4Max, double aRCut,
                                int aWType, @Nullable RowMatrix aFuseWeight, @Nullable Vector aPostFuseWeight, double @Nullable[] aPostFuseScale) {
        super(aThisType, aTypeNum, aNMax, aWType, aFuseWeight);
        if (aLMaxMax<0 || aLMaxMax>12) throw new IllegalArgumentException("Input lmax MUST be in [0, 12], input: "+aLMaxMax);
        if (aL3Max<0 || aL3Max>6) throw new IllegalArgumentException("Input l3max MUST be in [0, 6], input: "+aL3Max);
        if (aL4Max<0 || aL4Max>3) throw new IllegalArgumentException("Input l4max MUST be in [0, 3], input: "+aL3Max);
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
            mPostFuseSize = mPostFuseWeight.size()/mSizeN;
        }
        if (mPostFuseWeight!=null) {
            if (mPostFuseWeight.size()!=mPostFuseSize*mSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
        }
        mPostFuseScale = aPostFuseScale==null ? new double[]{1.0} : aPostFuseScale;
        if (mPostFuseScale.length!=1) throw new IllegalArgumentException("Size of post fuse scale mismatch");
        mSize = mPostFuseWeight==null ? (mSizeN*mSizeL) : (mPostFuseSize*mSizeL);
    }
    SphericalChebyshev2(int aThisType, int aTypeNum, int aNMax, int aLMax, boolean aNoRadial, int aL3Max, int aL4Max, double aRCut, int aWType, RowMatrix aFuseWeight, Vector aPostFuseWeight, double[] aPostFuseScale) {
        this(aThisType, aTypeNum, aNMax, aLMax, Math.max(Math.max(aLMax, aL3Max), aL4Max), aNoRadial, aL3Max, aL4Max, aRCut, aWType, aFuseWeight, aPostFuseWeight, aPostFuseScale);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        // 目前此功能只是保留兼容，因此只在开启时专门存储
        if (mNoRadial) {
            rSaveTo.put("noradial", true);
        }
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l4max", mL4Max);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
        rSaveTo.put("post_fuse", mPostFuseWeight!=null);
        if (mPostFuseWeight!=null) {
            rSaveTo.put("post_fuse_size", mPostFuseSize);
            rSaveTo.put("post_fuse_scale", mPostFuseScale[0]);
            rSaveTo.put("post_fuse_weight", mPostFuseWeight.asList());
        }
    }
    
    @SuppressWarnings({"rawtypes"})
    public static SphericalChebyshev2 load(int aThisType, int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l3cross")) throw new IllegalArgumentException("no l3cross is invalid now.");
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l4cross")) throw new IllegalArgumentException("no l4cross is invalid now.");
        if (!UT.Code.getWithDefault(aMap, "limited", "fuse_style").equals("limited")) throw new IllegalArgumentException("no limited fuse_style is invalid now.");
        if (aMap.containsKey("rfunc_scales")) throw new IllegalArgumentException("rfunc_scales is invalid now.");
        if (aMap.containsKey("system_scales")) throw new IllegalArgumentException("system_scales is invalid now.");
        int aWType = getWType_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aTypeNum);
        int tFuseSize = getFuseSize(aWType, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aTypeNum, aNMax, tFuseSize);
        Vector aPostFuseWeight = getPostFuseWeight_(aMap, tSizeN);
        double[] aPostFuseScale = aPostFuseWeight==null ? null : new double[1];
        if (aPostFuseWeight!=null) {
            Object tPostFuseScale = aMap.get("post_fuse_scale");
            aPostFuseScale[0] = tPostFuseScale==null ? 1.0 : ((Number)tPostFuseScale).doubleValue();
        }
        return new SphericalChebyshev2(
            aThisType, aTypeNum, aNMax,
            aLMax, (Boolean)UT.Code.getWithDefault(aMap, false, "noradial"),
            aL3Max, aL4Max,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseWeight, aPostFuseWeight, aPostFuseScale
        );
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getPostFuseWeight_(Map aMap, int aSizeN) {
        Object tFlag = aMap.get("post_fuse");
        if (tFlag==null || (!(Boolean)tFlag)) return null;
        Object tPostFuseSize = aMap.get("post_fuse_size");
        Object tPostFuseWeight = aMap.get("post_fuse_weight");
        if (tPostFuseWeight!=null) {
            Vector tVec = Vectors.from((List)tPostFuseWeight);
            if (tPostFuseSize!=null) {
                if (tVec.size()!=((Number)tPostFuseSize).intValue()*aSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
            }
            return tVec;
        }
        if (tPostFuseSize==null) throw new IllegalArgumentException("Key `post_fuse_weight` or `post_fuse_size` required for post_fuse");
        int tSize = ((Number)tPostFuseSize).intValue()*aSizeN;
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
            IVector tSubVec = mPostFuseWeight.subVec(tShift, tShift + mSizeN);
            tSubVec.div2this(tSubVec.operation().norm1() / mSizeN);
            tShift += mSizeN;
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
    @Override public int parameterSize() {
        int tParaSize = super.parameterSize();
        if (mPostFuseWeight == null) return tParaSize;
        return tParaSize + mPostFuseWeight.size();
    }
    @Override public boolean hasParameters() {
        return super.hasParameters() || mPostFuseWeight!=null;
    }
    
    @Override public IVector hyperParameters() {
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx==0) return mPostFuseScale[0];
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {return 1;}
        };
    }
    @Override public int hyperParameterSize() {return 1;}
    
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
    
    @Override public int forwardCacheSize(int aNN, boolean aFullCache) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE) {
            return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll + (mNMax+1)*mLMAll) + mSizeN*mLMAll + tPostSize)
                              : (mNMax+1 + mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize);
        }
        return aFullCache ? (aNN*(mNMax+1 + 1 + mLMAll) + mSizeN*mLMAll + tPostSize)
                          : (mNMax+1 + mLMAll + mSizeN*mLMAll + tPostSize);
    }
    @Override public int backwardCacheSize(int aNN, boolean aFullCache) {
        int tPostSize = mPostFuseWeight==null ? 0 : (mPostFuseSize*mLMAll);
        if (mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE) {
            return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll + (mNMax+1)*mLMAll) + (mNMax+1) + 2*mLMAll + mSizeN*mLMAll + tPostSize)
                              : (4*(mNMax+1) + 5*mLMAll + (mNMax+1)*mLMAll + mSizeN*mLMAll + tPostSize);
        }
        return aFullCache ? (3*aNN*(mNMax+1 + 1 + mLMAll) + (mNMax+1) + 2*mLMAll + mSizeN*mLMAll + tPostSize)
                          : (4*(mNMax+1) + 5*mLMAll + mSizeN*mLMAll + tPostSize);
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap) {
        super.updateGenMap(rGenMap);
        int ti = mThisType-1;
        rGenMap.put("[FP USE "+mThisType+"]", "spherical_chebyshev");
        rGenMap.put(ti+":NNAPGEN_FP_LMAX", mLMax);
        rGenMap.put(ti+":NNAPGEN_FP_NORADIAL", mNoRadial?1:0);
        rGenMap.put(ti+":NNAPGEN_FP_L3MAX", mL3Max);
        rGenMap.put(ti+":NNAPGEN_FP_L4MAX", mL4Max);
        rGenMap.put(ti+":NNAPGEN_FP_PFFLAG", mPostFuseWeight==null?0:1);
        rGenMap.put(ti+":NNAPGEN_FP_PFSIZE", mPostFuseSize);
    }
    @Override public boolean hasSameGenMap(Object aBasis) {
        if (!(aBasis instanceof SphericalChebyshev2)) return false;
        SphericalChebyshev2 tBasis = (SphericalChebyshev2)aBasis;
        return super.hasSameGenMap(aBasis) && mLMax==tBasis.mLMax && mNoRadial==tBasis.mNoRadial && mL3Max==tBasis.mL3Max && mL4Max==tBasis.mL4Max
                                           && (mPostFuseWeight!=null)==(tBasis.mPostFuseWeight!=null) && mPostFuseSize==tBasis.mPostFuseSize;
    }
}
