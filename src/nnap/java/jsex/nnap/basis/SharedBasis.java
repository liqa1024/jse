package jsex.nnap.basis;

import java.util.Map;

/**
 * 基于其他元素基组的共享基组
 * <p>
 * 现在是常用的其他种类默认基组
 * @author liqa
 */
public class SharedBasis extends Basis {
    
    private final Basis mSharedBasis;
    private final int mSharedType;
    public SharedBasis(Basis aSharedBasis, int aSharedType) {
        if (aSharedBasis == null) throw new NullPointerException();
        if (aSharedBasis instanceof SharedBasis) throw new IllegalArgumentException("SharedBasis MUST NOT be Shared");
        mSharedBasis = aSharedBasis;
        mSharedType = aSharedType;
    }
    public Basis sharedBasis() {return mSharedBasis;}
    public int sharedType() {return mSharedType;}
    
    @Override public double rcutMax() {return mSharedBasis.rcutMax();}
    @Override public int size() {return mSharedBasis.size();}
    
    @Override public int mergeSize() {return mSharedBasis.mergeSize();}
    @Override public double rcut(int aMergeIdx) {return mSharedBasis.rcut(aMergeIdx);}
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put("[FP SHARE "+aGenIdx+"]", true); // 标记此分支为 share
        updateGenMapInternal(rGenMap, aGenIdx);
    }
    @Override void updateGenMapInternal(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put(aGenIdx+":NNAPGEN_FP_SHARED_TYPE", mSharedType);
        // 补充 share 基组的内部参数
        mSharedBasis.updateGenMapInternal(rGenMap, aGenIdx);
    }
    @Override public boolean hasSameGenMap(Basis aBasis) {
        if (!(aBasis instanceof SharedBasis)) return false;
        SharedBasis tBasis = (SharedBasis)aBasis;
        return mSharedType==tBasis.mSharedType && mSharedBasis.hasSameGenMap(tBasis.mSharedBasis);
    }
    
    @Override public int forwardCacheSize(int aNumNei) {
        return mSharedBasis.forwardCacheSize(aNumNei);
    }
    @Override public int backwardCacheSize(int aNumNei) {
        return mSharedBasis.backwardCacheSize(aNumNei);
    }
    @Override public int backwardBackwardCacheSize(int aNumNei) {
        return mSharedBasis.backwardBackwardCacheSize(aNumNei);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "share");
        rSaveTo.put("share", mSharedType);
    }
    @SuppressWarnings({"rawtypes"})
    public static SharedBasis load(Basis[] aBasis, Map aMap) {
        Object tShare = aMap.get("share");
        if (tShare==null) throw new IllegalArgumentException("Key `share` required for shared_basis");
        int tSharedType = ((Number)tShare).intValue();
        return new SharedBasis(aBasis[tSharedType-1], tSharedType);
    }
}
