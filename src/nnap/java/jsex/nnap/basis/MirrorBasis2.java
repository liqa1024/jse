package jsex.nnap.basis;

import java.util.Map;

/**
 * 基于其他元素基组的一个镜像基组，其对于自身元素和对应的镜像元素种类会进行交换
 * <p>
 * 目前主要用于实现 ising 模型
 * @author liqa
 */
public class MirrorBasis2 extends Basis2 {
    
    private final Basis2 mMirrorBasis;
    private final int mMirrorType;
    public MirrorBasis2(Basis2 aMirrorBasis, int aMirrorType) {
        if (aMirrorBasis == null) throw new NullPointerException();
        if (aMirrorBasis instanceof MirrorBasis2) throw new IllegalArgumentException("MirrorBasis MUST NOT be Mirror");
        mMirrorBasis = aMirrorBasis;
        mMirrorType = aMirrorType;
    }
    public Basis2 mirrorBasis() {return mMirrorBasis;}
    public int mirrorType() {return mMirrorType;}
    
    @Override public double rcut() {return mMirrorBasis.rcut();}
    @Override public int size() {return mMirrorBasis.size();}
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put("[FP MIRROR "+aGenIdx+"]", true); // 标记此分支为 mirror
        updateGenMapInternal(rGenMap, aGenIdx);
    }
    @Override void updateGenMapInternal(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put(aGenIdx+":NNAPGEN_FP_MIRROR_TYPE", mMirrorType);
        // 补充 mirror 基组的内部参数
        mMirrorBasis.updateGenMapInternal(rGenMap, aGenIdx);
    }
    @Override public boolean hasSameGenMap(Basis2 aBasis) {
        if (!(aBasis instanceof MirrorBasis2)) return false;
        MirrorBasis2 tBasis = (MirrorBasis2)aBasis;
        return mMirrorType==tBasis.mMirrorType && mMirrorBasis.hasSameGenMap(tBasis.mMirrorBasis);
    }
    
    @Override public int forwardCacheSize(int aNumNei) {
        return mMirrorBasis.forwardCacheSize(aNumNei);
    }
    @Override public int backwardCacheSize(int aNumNei) {
        return mMirrorBasis.backwardCacheSize(aNumNei);
    }
    @Override public int backwardBackwardCacheSize(int aNumNei) {
        return mMirrorBasis.backwardBackwardCacheSize(aNumNei);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "mirror");
        rSaveTo.put("mirror", mMirrorType);
    }
    @SuppressWarnings({"rawtypes"})
    public static MirrorBasis2 load(Basis2[] aBasis, Map aMap) {
        Object tMirror = aMap.get("mirror");
        if (tMirror==null) throw new IllegalArgumentException("Key `mirror` required for mirror_basis");
        int tMirrorType = ((Number)tMirror).intValue();
        return new MirrorBasis2(aBasis[tMirrorType-1], tMirrorType);
    }
}
