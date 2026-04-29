package jsex.nnap.basis;

import jse.code.UT;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
    final int mSizeL;
    final int mLMaxMax, mLMAll;
    final int mSize;
    
    private SphericalChebyshev2(double aRCut, int aNumTypes, int aNMax, int aLMax, int aLMaxMax, int aL3Max, int aL4Max,
                                int aWType, @Nullable Vector aFuseWeight, @Nullable Vector aPostFuseWeight, double @Nullable[] aPostFuseScale) {
        super(aRCut, aNumTypes, aNMax, aWType, aFuseWeight, aPostFuseWeight, aPostFuseScale);
        if (aLMaxMax<0 || aLMaxMax>12) throw new IllegalArgumentException("Input lmax MUST be in [0, 12], input: "+aLMaxMax);
        if (aL3Max<0 || aL3Max>6) throw new IllegalArgumentException("Input l3max MUST be in [0, 6], input: "+aL3Max);
        if (aL4Max<0 || aL4Max>3) throw new IllegalArgumentException("Input l4max MUST be in [0, 3], input: "+aL3Max);
        mLMax = aLMax;
        mL3Max = aL3Max;
        mL4Max = aL4Max;
        
        mSizeL = (mLMax+1) + L3NCOLS[mL3Max] + L4NCOLS[mL4Max];
        mLMaxMax = aLMaxMax;
        if (mLMaxMax!=Math.max(Math.max(mLMax, mL3Max), mL4Max)) throw new IllegalStateException();
        mLMAll = (mLMaxMax+1)*(mLMaxMax+1);
        
        mSize = mSizeNP*mSizeL;
    }
    SphericalChebyshev2(double aRCut, int aNumTypes, int aNMax, int aLMax, int aL3Max, int aL4Max, int aWType, Vector aFuseWeight, Vector aPostFuseWeight, double[] aPostFuseScale) {
        this(aRCut, aNumTypes, aNMax, aLMax, Math.max(Math.max(aLMax, aL3Max), aL4Max), aL3Max, aL4Max, aWType, aFuseWeight, aPostFuseWeight, aPostFuseScale);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l4max", mL4Max);
        super.save_(rSaveTo);
    }
    
    @SuppressWarnings({"rawtypes"})
    public static SphericalChebyshev2 load(int aNumTypes, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        int aLMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue();
        int aL3Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue();
        int aL4Max = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_L4MAX, "l4max")).intValue();
        if ((Boolean)UT.Code.getWithDefault(aMap, false, "noradial")) throw new IllegalArgumentException("noradial is invalid now.");
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l3cross")) throw new IllegalArgumentException("no l3cross is invalid now.");
        if (!(Boolean)UT.Code.getWithDefault(aMap, true, "l4cross")) throw new IllegalArgumentException("no l4cross is invalid now.");
        if (!UT.Code.getWithDefault(aMap, "limited", "fuse_style").equals("limited")) throw new IllegalArgumentException("no limited fuse_style is invalid now.");
        if (aMap.containsKey("rfunc_scales")) throw new IllegalArgumentException("rfunc_scales is invalid now.");
        if (aMap.containsKey("system_scales")) throw new IllegalArgumentException("system_scales is invalid now.");
        int aWType = getWType_(aMap);
        Vector aFuseWeight = getFuseWeight_(aMap, aWType, aNumTypes);
        int tFuseSize = getFuseSize(aWType, aNumTypes, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aNumTypes, aNMax, tFuseSize);
        Vector aPostFuseWeight = getPostFuseWeight_(aMap, tSizeN);
        double[] aPostFuseScale = aPostFuseWeight==null ? null : new double[1];
        if (aPostFuseWeight!=null) {
            Object tPostFuseScale = aMap.get("post_fuse_scale");
            aPostFuseScale[0] = tPostFuseScale==null ? 1.0 : ((Number)tPostFuseScale).doubleValue();
        }
        return new SphericalChebyshev2(
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aNumTypes, aNMax,
            aLMax, aL3Max, aL4Max,
            aWType, aFuseWeight, aPostFuseWeight, aPostFuseScale
        );
    }
    
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)(lmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)(lmax+1)}
     */
    @Override public int size() {return mSize;}
    
    @Override public int forwardCacheSize(int aNumNei) {
        return aNumNei*(mNMax+1 + mSizeNP + mLMAll) + (mSizeNP*mLMAll);
    }
    @Override public int backwardCacheSize(int aNumNei) {
        return aNumNei*(mNMax+1 + mSizeNP*2 + mLMAll*2) + (mSizeNP*mLMAll);
    }
    @Override public int backwardBackwardCacheSize(int aNumNei) {
        return aNumNei*(mSizeNP) + mSizeNP*mLMAll;
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge) {
        super.updateGenMap(rGenMap, aGenIdxType, aGenIdxMerge);
        rGenMap.put("[FP USE "+aGenIdxType+":"+aGenIdxMerge+"]", "spherical_chebyshev");
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_LMAX", mLMax);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_L3MAX", mL3Max);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_L4MAX", mL4Max);
    }
    @Override public boolean hasSameGenMap(MergeableBasis2 aBasis) {
        if (!(aBasis instanceof SphericalChebyshev2)) return false;
        SphericalChebyshev2 tBasis = (SphericalChebyshev2)aBasis;
        return super.hasSameGenMap(aBasis) && mLMax==tBasis.mLMax && mL3Max==tBasis.mL3Max && mL4Max==tBasis.mL4Max;
    }
}
