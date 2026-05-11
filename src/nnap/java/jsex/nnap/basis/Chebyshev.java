package jsex.nnap.basis;

import jse.code.UT;
import jse.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 一种仅使用 Chebyshev 多项式将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这不会包含角向序，但是速度可以很快。
 * @author liqa
 */
public class Chebyshev extends WTypeBasis {
    public final static int DEFAULT_NMAX = 5;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    final int mSize;
    
    Chebyshev(double aRCut, int aNumTypes, int aNMax, int aWType, boolean aWeightStandardization,
              @Nullable Vector aFuseWeight, @Nullable Vector aRFuseWeight, double @Nullable[] aRFuseScale) {
        super(aRCut, aNumTypes, aNMax, aWType, aWeightStandardization, aFuseWeight, aRFuseWeight, aRFuseScale);
        mSize = mSizeNP;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "chebyshev");
        super.save_(rSaveTo);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Chebyshev load(int aNumTypes, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        if (!UT.Code.getWithDefault(aMap, "limited", "fuse_style").equals("limited")) throw new IllegalArgumentException("no limited fuse_style is invalid now.");
        if (aMap.containsKey("rfunc_scales")) throw new IllegalArgumentException("rfunc_scales is invalid now.");
        if (aMap.containsKey("system_scales")) throw new IllegalArgumentException("system_scales is invalid now.");
        int aWType = getWType_(aMap);
        Vector aFuseWeight = getFuseWeight_(aMap, aWType, aNumTypes);
        int tFuseSize = getFuseSize(aWType, aNumTypes, aFuseWeight);
        int tSizeN = getSizeN_(aWType, aNumTypes, aNMax, tFuseSize);
        // 先尝试获取 rfuse
        Vector aRFuseWeight = getRFuseWeight_(aMap, aWType, tSizeN);
        double[] aRFuseScale = aRFuseWeight==null ? null : new double[1];
        if (aRFuseWeight != null) {
            Object tRFuseScale = aMap.get("rfuse_scale");
            aRFuseScale[0] = tRFuseScale==null ? 1.0 : ((Number)tRFuseScale).doubleValue();
        }
        // 没有 rfuse 的情况下尝试获取 post_fuse 兼容
        if (aRFuseWeight == null) {
            Vector tPostFuseWeight = getPostFuseWeight_(aMap, tSizeN);
            if (tPostFuseWeight != null) {
                // 简单覆盖
                aRFuseScale = new double[1];
                Object tPostFuseScale = aMap.get("post_fuse_scale");
                aRFuseScale[0] = tPostFuseScale==null ? 1.0 : ((Number)tPostFuseScale).doubleValue();
                // 转换
                aRFuseWeight = postFuse2RFuse_(tPostFuseWeight, aWType, aNumTypes, aNMax, tSizeN, aFuseWeight, tFuseSize);
                aWType = WTYPE_RFUSE;
                aFuseWeight = null;
            }
        }
        return new Chebyshev(
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aNumTypes, aNMax,
            aWType, (Boolean)UT.Code.getWithDefault(aMap, false, "weight_standardization", "ws"),
            aFuseWeight, aRFuseWeight, aRFuseScale
        );
    }
    
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)}
     */
    @Override public int size() {return mSize;}
    
    @Override public int forwardCacheSize(int aNumNei) {
        return aNumNei*(1 + mNMax+1 + mSizeNP);
    }
    @Override public int backwardCacheSize(int aNumNei) {
        return aNumNei*(1 + mNMax+1 + mSizeNP);
    }
    @Override public int backwardBackwardCacheSize(int aNumNei) {
        return aNumNei*mSizeNP;
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge) {
        super.updateGenMap(rGenMap, aGenIdxType, aGenIdxMerge);
        rGenMap.put("[FP USE "+aGenIdxType+":"+aGenIdxMerge+"]", "chebyshev");
    }
    @Override public boolean hasSameGenMap(MergeableBasis aBasis) {
        if (!(aBasis instanceof Chebyshev)) return false;
        return super.hasSameGenMap(aBasis);
    }
}
