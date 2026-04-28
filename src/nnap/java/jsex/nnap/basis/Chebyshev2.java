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
public class Chebyshev2 extends WTypeBasis2 {
    public final static int DEFAULT_NMAX = 5;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    final int mSize;
    
    Chebyshev2(double aRCut, int aNumTypes, int aNMax, int aWType, @Nullable Vector aFuseWeight, @Nullable Vector aPostFuseWeight, double @Nullable[] aPostFuseScale) {
        super(aRCut, aNumTypes, aNMax, aWType, aFuseWeight, aPostFuseWeight, aPostFuseScale);
        mSize = mSizeNP;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "chebyshev");
        super.save_(rSaveTo);
    }
    
    @SuppressWarnings({"rawtypes"})
    public static Chebyshev2 load(int aNumTypes, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
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
        return new Chebyshev2(
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aNumTypes, aNMax,
            aWType, aFuseWeight, aPostFuseWeight, aPostFuseScale
        );
    }
    
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)}
     */
    @Override public int size() {return mSize;}
    
    @Override public int forwardCacheSize(int aNumNei) {
        return 0;
    }
    @Override public int backwardCacheSize(int aNumNei) {
        return aNumNei*(mNMax+1 + mSizeNP);
    }
    @Override public int backwardBackwardCacheSize(int aNumNei) {
        return 0;
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge) {
        super.updateGenMap(rGenMap, aGenIdxType, aGenIdxMerge);
        rGenMap.put("[FP USE "+aGenIdxType+":"+aGenIdxMerge+"]", "chebyshev");
    }
    @Override public boolean hasSameGenMap(MergeableBasis2 aBasis) {
        if (!(aBasis instanceof Chebyshev2)) return false;
        return super.hasSameGenMap(aBasis);
    }
}
