package jsex.nnap.basis;

import jse.code.collection.NewCollections;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用多个基组的合并基组，用于实现自定义的高效基组
 * @author liqa
 */
public class MergedBasis2 extends Basis2 {
    private final MergeableBasis2[] mMergedBasis;
    private final double mRCut;
    private final int mSize, mTotParaSize, mTotHyperParaSize;
    private final @Nullable IVector[] mParas;
    private final IVector[] mHyperParas;
    private final int[] mParaSizes, mHyperParaSizes;
    
    public MergedBasis2(MergeableBasis2... aMergedBasis) {
        if (aMergedBasis ==null || aMergedBasis.length==0) throw new IllegalArgumentException("Merge basis can not be null or empty");
        double tRCut = Double.NEGATIVE_INFINITY;
        int tSize = 0;
        for (MergeableBasis2 tBasis : aMergedBasis) {
            if (!(tBasis instanceof SphericalChebyshev2) && !(tBasis instanceof Chebyshev2)) {
                throw new IllegalArgumentException("MergeBasis should be SphericalChebyshev or Chebyshev");
            }
            tRCut = Math.max(tBasis.rcut(), tRCut);
            tSize += tBasis.size();
        }
        mMergedBasis = aMergedBasis;
        mRCut = tRCut;
        mSize = tSize;
        // init para stuff
        mParas = new IVector[mMergedBasis.length];
        mHyperParas = new IVector[mMergedBasis.length];
        mParaSizes = new int[mMergedBasis.length];
        mHyperParaSizes = new int[mMergedBasis.length];
        int tTotParaSize = 0, tTotHyperParaSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            IVector tPara = mMergedBasis[i].hasParameters() ? mMergedBasis[i].parameters() : null;
            mParas[i] = tPara;
            int tSizePara = mMergedBasis[i].parameterSize();
            mParaSizes[i] = tSizePara;
            tTotParaSize += tSizePara;
            IVector tHyperPara = mMergedBasis[i].hyperParameters();
            mHyperParas[i] = tHyperPara;
            int tSizeHyperPara = mMergedBasis[i].hyperParameterSize();
            mHyperParaSizes[i] = tSizeHyperPara;
            tTotHyperParaSize += tSizeHyperPara;
        }
        mTotParaSize = tTotParaSize;
        mTotHyperParaSize = tTotHyperParaSize;
    }
    public int mergeSize() {
        return mMergedBasis.length;
    }
    
    @Override public IVector hyperParameters() {
        return new RefVector() {
            @Override public double get(int aIdx) {
                int tIdx = aIdx;
                for (int i = 0; i < mMergedBasis.length; ++i) {
                    int tHyperParaSize = mHyperParaSizes[i];
                    if (tIdx < tHyperParaSize) {
                        IVector tHyperPara = mHyperParas[i];
                        return tHyperPara.get(tIdx);
                    }
                    tIdx -= tHyperParaSize;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return mTotHyperParaSize;
            }
        };
    }
    @Override public int hyperParameterSize() {
        return mTotHyperParaSize;
    }
    
    @Override public void initParameters() {
        for (MergeableBasis2 tBasis : mMergedBasis) tBasis.initParameters();
    }
    @Override public IVector parameters() {
        return new RefVector() {
            @Override public double get(int aIdx) {
                int tIdx = aIdx;
                for (int i = 0; i < mMergedBasis.length; ++i) {
                    int tParaSize = mParaSizes[i];
                    if (tIdx < tParaSize) {
                        IVector tPara = mParas[i];
                        assert tPara != null;
                        return tPara.get(tIdx);
                    }
                    tIdx -= tParaSize;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                int tIdx = aIdx;
                for (int i = 0; i < mMergedBasis.length; ++i) {
                    int tParaSize = mParaSizes[i];
                    if (tIdx < tParaSize) {
                        IVector tPara = mParas[i];
                        assert tPara != null;
                        tPara.set(tIdx, aValue);
                        return;
                    }
                    tIdx -= tParaSize;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return mTotParaSize;
            }
        };
    }
    @Override public int parameterSize() {
        return mTotParaSize;
    }
    @Override public boolean hasParameters() {
        for (MergeableBasis2 tBasis : mMergedBasis) {
            if (tBasis.hasParameters()) return true;
        }
        return false;
    }
    
    @Override public double rcut() {return mRCut;}
    @Override public int size() {return mSize;}
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdx) {
        rGenMap.put("[FP MERGE "+aGenIdx+"]", mMergedBasis.length);
        rGenMap.put(aGenIdx+":NNAPGEN_FP_SIZE", size());
        for (int i = 0; i < mMergedBasis.length; ++i) {
            mMergedBasis[i].updateGenMap(rGenMap, aGenIdx, i);
        }
    }
    @Override public boolean hasSameGenMap(Basis2 aBasis) {
        if (!(aBasis instanceof MergedBasis2)) return false;
        MergedBasis2 tBasis = (MergedBasis2)aBasis;
        if (size()!=tBasis.size()) return false;
        if (mMergedBasis.length != tBasis.mMergedBasis.length) return false;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            if (!mMergedBasis[i].hasSameGenMap(tBasis.mMergedBasis[i])) return false;
        }
        return true;
    }
    
    @Override public int forwardCacheSize(int aNN, boolean aFullCache) {
        int rSize = 0;
        for (MergeableBasis2 tBasis : mMergedBasis) {
            rSize += tBasis.forwardCacheSize(aNN, aFullCache);
        }
        return rSize;
    }
    @Override public int backwardCacheSize(int aNN, boolean aFullCache) {
        int rSize = 0;
        for (MergeableBasis2 tBasis : mMergedBasis) {
            rSize += tBasis.backwardCacheSize(aNN, aFullCache);
        }
        return rSize;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "merge");
        List<Map> tMergeBasis = NewCollections.from(mMergedBasis.length, i -> new LinkedHashMap<>());
        for (int i = 0; i < mMergedBasis.length; ++i) {
            mMergedBasis[i].save(tMergeBasis.get(i));
        }
        rSaveTo.put("basis", tMergeBasis);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MergedBasis2 load(int aTypeNum, Map aMap) {
        Object tObj = aMap.get("basis");
        if (tObj == null) throw new IllegalArgumentException("Key `basis` required for merge load");
        List<Map> tList = (List<Map>)tObj;
        MergeableBasis2[] tMergeBasis = new MergeableBasis2[tList.size()];
        for (int i = 0; i < tMergeBasis.length; ++i) {
            Map tMap = tList.get(i);
            Object tType = tMap.get("type");
            if (tType == null) {
                tType = "spherical_chebyshev";
            }
            switch(tType.toString()) {
            case "spherical_chebyshev": {
                tMergeBasis[i] = SphericalChebyshev2.load(aTypeNum, tMap);
                break;
            }
            case "chebyshev": {
                tMergeBasis[i] = Chebyshev2.load(aTypeNum, tMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tType);
            }}
        }
        return new MergedBasis2(tMergeBasis);
    }
}
