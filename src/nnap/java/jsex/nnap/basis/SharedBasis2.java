package jsex.nnap.basis;

import java.util.Map;

/**
 * 基于其他元素基组的共享基组
 * <p>
 * 现在是常用的其他种类默认基组
 * @author liqa
 */
public class SharedBasis2 extends Basis2 {
    
    private final Basis2 mSharedBasis;
    private final int mSharedType;
    public SharedBasis2(Basis2 aSharedBasis, int aSharedType) {
        if (aSharedBasis == null) throw new NullPointerException();
        if (aSharedBasis instanceof SharedBasis2) throw new IllegalArgumentException("SharedBasis MUST NOT be Shared");
        mSharedBasis = aSharedBasis;
        mSharedType = aSharedType;
    }
    public Basis2 sharedBasis() {return mSharedBasis;}
    public int sharedType() {return mSharedType;}
    
    @Override public double rcut() {return mSharedBasis.rcut();}
    @Override public int size() {return mSharedBasis.size();}
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put("[FP SHARE "+aGenIdx+"]", true); // 标记此分支为 share
        rGenMap.put(aGenIdx+":NNAPGEN_FP_SHARED_TYPE", mSharedType);
        // 依旧需要补充 share 的参数
        mSharedBasis.updateGenMap(rGenMap, aGenIdx);
    }
    @Override public boolean hasSameGenMap(Basis2 aBasis) {
        if (!(aBasis instanceof SharedBasis2)) return false;
        SharedBasis2 tBasis = (SharedBasis2)aBasis;
        return mSharedType==tBasis.mSharedType && mSharedBasis.hasSameGenMap(tBasis.mSharedBasis);
    }
    
    @Override public int forwardCacheSize(int aNN, boolean aFullCache) {return mSharedBasis.forwardCacheSize(aNN, aFullCache);}
    @Override public int backwardCacheSize(int aNN, boolean aFullCache) {return mSharedBasis.backwardCacheSize(aNN, aFullCache);}
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "share");
        rSaveTo.put("share", mSharedType);
    }
    @SuppressWarnings({"rawtypes"})
    public static SharedBasis2 load(Basis2[] aBasis, Map aMap) {
        Object tShare = aMap.get("share");
        if (tShare==null) throw new IllegalArgumentException("Key `share` required for shared_basis");
        int tSharedType = ((Number)tShare).intValue();
        return new SharedBasis2(aBasis[tSharedType-1], tSharedType);
    }
}
