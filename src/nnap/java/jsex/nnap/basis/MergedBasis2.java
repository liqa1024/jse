package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.NewCollections;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.IDataShell;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import jse.math.vector.ShiftVector;

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
    private final int mSize, mTotParaSize, mTotHyperParaSize, mTotFitParaSize;
    private final int[] mParaSizes, mHyperParaSizes, mFitParaSizes;
    
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
        mParaSizes = new int[mMergedBasis.length];
        mHyperParaSizes = new int[mMergedBasis.length];
        mFitParaSizes = new int[mMergedBasis.length];
        int tTotParaSize = 0, tTotHyperParaSize = 0, tTotFitParaSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSizePara = mMergedBasis[i].parameterSize();
            mParaSizes[i] = tSizePara;
            tTotParaSize += tSizePara;
            int tSizeHyperPara = mMergedBasis[i].hyperParameterSize();
            mHyperParaSizes[i] = tSizeHyperPara;
            tTotHyperParaSize += tSizeHyperPara;
            int tSizeFitPara = mMergedBasis[i].fittableParameterSize();
            mFitParaSizes[i] = tSizeFitPara;
            tTotFitParaSize += tSizeFitPara;
        }
        mTotParaSize = tTotParaSize;
        mTotHyperParaSize = tTotHyperParaSize;
        mTotFitParaSize = tTotFitParaSize;
    }
    public int mergeSize() {
        return mMergedBasis.length;
    }
    
    @Override public void mountParameter(IDoubleOrFloatCPointer aPtr) {
        IDoubleOrFloatCPointer tPtr = aPtr.copy();
        for (int i = 0; i < mMergedBasis.length; ++i) {
            mMergedBasis[i].mountParameter(tPtr);
            tPtr.rightShift(mParaSizes[i]);
        }
    }
    @Override public int parameterSize() {
        return mTotParaSize;
    }
    
    @Override public void mountHyperParameter(IDoubleOrFloatCPointer aPtr) {
        IDoubleOrFloatCPointer tPtr = aPtr.copy();
        for (int i = 0; i < mMergedBasis.length; ++i) {
            mMergedBasis[i].mountHyperParameter(tPtr);
            tPtr.rightShift(mHyperParaSizes[i]);
        }
    }
    @Override public int hyperParameterSize() {
        return mTotHyperParaSize;
    }
    
    @Override public void initParameters() {
        for (MergeableBasis2 tBasis : mMergedBasis) tBasis.initParameters();
    }
    @Override public void mountFittableParameter(IDataShell<double[]> aData) {
        if (Conf.OPERATION_CHECK) {
            if (mTotFitParaSize != aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (mTotFitParaSize > aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        }
        double[] tData = aData.internalData();
        int tShift = aData.internalDataShift();
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSize = mFitParaSizes[i];
            mMergedBasis[i].mountFittableParameter(new ShiftVector(tSize, tShift, tData));
            tShift += tSize;
        }
    }
    @Override public int fittableParameterSize() {
        return mTotFitParaSize;
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
