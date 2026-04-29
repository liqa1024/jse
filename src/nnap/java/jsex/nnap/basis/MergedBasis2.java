package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.NewCollections;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.vector.Vector;

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
    private final int mSize, mTotCParaSize, mTotCHyperParaSize, mTotParaSize;
    private final int[] mCParaSizes, mCHyperParaSizes, mParaSizes;
    
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
        mCParaSizes = new int[mMergedBasis.length];
        mCHyperParaSizes = new int[mMergedBasis.length];
        mParaSizes = new int[mMergedBasis.length];
        int tTotCParaSize = 0, tTotCHyperParaSize = 0, tTotParaSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSizeCPara = mMergedBasis[i].cptrParameterSize();
            mCParaSizes[i] = tSizeCPara;
            tTotCParaSize += tSizeCPara;
            int tSizeCHyperPara = mMergedBasis[i].cptrHyperParameterSize();
            mCHyperParaSizes[i] = tSizeCHyperPara;
            tTotCHyperParaSize += tSizeCHyperPara;
            int tSizePara = mMergedBasis[i].parameterSize();
            mParaSizes[i] = tSizePara;
            tTotParaSize += tSizePara;
        }
        mTotCParaSize = tTotCParaSize;
        mTotCHyperParaSize = tTotCHyperParaSize;
        mTotParaSize = tTotParaSize;
    }
    public int mergeSize() {
        return mMergedBasis.length;
    }
    
    @Override public void mountCptrParameter(IDoubleOrFloatCPointer aPtr) {
        IDoubleOrFloatCPointer tPtr = aPtr.copy();
        for (int i = 0; i < mMergedBasis.length; ++i) {
            mMergedBasis[i].mountCptrParameter(tPtr);
            tPtr.rightShift(mCParaSizes[i]);
        }
    }
    @Override public void mountGradCptrParameter(int aThreadID, IDoubleOrFloatCPointer aPtr) {
        IDoubleOrFloatCPointer tPtr = aPtr.copy();
        for (int i = 0; i < mMergedBasis.length; ++i) {
            mMergedBasis[i].mountGradCptrParameter(aThreadID, tPtr);
            tPtr.rightShift(mCParaSizes[i]);
        }
    }
    @Override public int cptrParameterSize() {
        return mTotCParaSize;
    }
    
    @Override public void mountCptrHyperParameter(IDoubleOrFloatCPointer aPtr) {
        IDoubleOrFloatCPointer tPtr = aPtr.copy();
        for (int i = 0; i < mMergedBasis.length; ++i) {
            mMergedBasis[i].mountCptrHyperParameter(tPtr);
            tPtr.rightShift(mCHyperParaSizes[i]);
        }
    }
    @Override public int cptrHyperParameterSize() {
        return mTotCHyperParaSize;
    }
    
    @Override public void initParameters() {
        for (MergeableBasis2 tBasis : mMergedBasis) tBasis.initParameters();
    }
    @Override public void mountParameter(Vector aVec) {
        if (Conf.OPERATION_CHECK) {
            if (mTotParaSize != aVec.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (mTotParaSize > aVec.size()) throw new IllegalArgumentException("data size mismatch");
        }
        int tShift = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSize = mParaSizes[i];
            mMergedBasis[i].mountParameter(aVec.subVec(tShift, tShift+tSize));
            tShift += tSize;
        }
    }
    @Override public void mountGradParameter(Vector aVec) {
        if (Conf.OPERATION_CHECK) {
            if (mTotParaSize != aVec.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (mTotParaSize > aVec.size()) throw new IllegalArgumentException("data size mismatch");
        }
        int tShift = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSize = mParaSizes[i];
            mMergedBasis[i].mountGradParameter(aVec.subVec(tShift, tShift+tSize));
            tShift += tSize;
        }
    }
    @Override public int parameterSize() {
        return mTotParaSize;
    }
    
    @Override public void requireGrad(int aNumThreads) {
        for (MergeableBasis2 tMergedBasis : mMergedBasis) {
            tMergedBasis.requireGrad(aNumThreads);
        }
    }
    @Override public void updateParameters() {
        for (MergeableBasis2 tMergedBasis : mMergedBasis) {
            tMergedBasis.updateParameters();
        }
    }
    @Override public void backwardParameter() {
        for (MergeableBasis2 tMergedBasis : mMergedBasis) {
            tMergedBasis.backwardParameter();
        }
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
    
    @Override public int forwardCacheSize(int aNumNei) {
        int rSize = 0;
        for (MergeableBasis2 tBasis : mMergedBasis) {
            rSize += tBasis.forwardCacheSize(aNumNei);
        }
        return rSize;
    }
    @Override public int backwardCacheSize(int aNumNei) {
        int rSize = 0;
        for (MergeableBasis2 tBasis : mMergedBasis) {
            rSize += tBasis.backwardCacheSize(aNumNei);
        }
        return rSize;
    }
    @Override public int backwardBackwardCacheSize(int aNumNei) {
        int rSize = 0;
        for (MergeableBasis2 tBasis : mMergedBasis) {
            rSize += tBasis.backwardBackwardCacheSize(aNumNei);
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
            case "spherical_chebyshev": case "sph_cheby": {
                tMergeBasis[i] = SphericalChebyshev2.load(aTypeNum, tMap);
                break;
            }
            case "chebyshev": case "cheby": {
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
