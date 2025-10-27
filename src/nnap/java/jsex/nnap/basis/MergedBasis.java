package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.math.vector.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jse.code.CS.*;

/**
 * 使用多个基组的合并基组，用于实现自定义的高效基组
 * @author liqa
 */
public class MergedBasis extends Basis {
    private final MergeableBasis[] mMergedBasis;
    private final double mRCut;
    private final int mSize, mTotParaSize, mTypeNum;
    private final @Nullable IVector[] mParas;
    private final int[] mParaSizes;
    private final String @Nullable[] mSymbols;
    private final ShiftVector[] mFpShell, mNNGradShell, mGradNNGradShell, mParaShell;
    private final ShiftVector[] mForwardCacheShell, mBackwardCacheShell, mForwardForceCacheShell, mBackwardForceCacheShell;
    
    public MergedBasis(MergeableBasis... aMergedBasis) {
        if (aMergedBasis ==null || aMergedBasis.length==0) throw new IllegalArgumentException("Merge basis can not be null or empty");
        double tRCut = Double.NEGATIVE_INFINITY;
        int tSize = 0;
        int tTypeNum = -1;
        @Nullable List<String> tSymbols = null;
        Boolean tHasSymbols = null;
        for (Basis tBasis : aMergedBasis) {
            if (!(tBasis instanceof SphericalChebyshev) && !(tBasis instanceof Chebyshev)) {
                throw new IllegalArgumentException("MergeBasis should be SphericalChebyshev or Chebyshev");
            }
            tRCut = Math.max(tBasis.rcut(), tRCut);
            tSize += tBasis.size();
            if (tTypeNum < 0) {
                tTypeNum = tBasis.atomTypeNumber();
            } else {
                if (tTypeNum != tBasis.atomTypeNumber()) throw new IllegalArgumentException("atom type number mismatch");
            }
            if (tHasSymbols == null) {
                tHasSymbols = tBasis.hasSymbol();
                if (tHasSymbols) {
                    tSymbols = tBasis.symbols();
                    if (tSymbols == null) throw new NullPointerException();
                }
            } else {
                if (tHasSymbols != tBasis.hasSymbol()) throw new IllegalArgumentException("symbols mismatch");
                List<String> tSymbols_ = tBasis.symbols();
                if (tHasSymbols) {
                    if (!tSymbols.equals(tSymbols_)) throw new IllegalArgumentException("symbols mismatch");
                } else {
                    if (tSymbols_ != null) throw new IllegalArgumentException("symbols mismatch");
                }
            }
            tTypeNum = Math.min(tBasis.atomTypeNumber(), tTypeNum);
        }
        mMergedBasis = aMergedBasis;
        mRCut = tRCut;
        mSize = tSize;
        mTypeNum = tTypeNum;
        mSymbols = tSymbols==null ? null : tSymbols.toArray(ZL_STR);
        // init para stuff
        mParas = new IVector[mMergedBasis.length];
        mParaSizes = new int[mMergedBasis.length];
        mParaShell = new ShiftVector[mMergedBasis.length];
        int tTotParaSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            IVector tPara = mMergedBasis[i].hasParameters() ? mMergedBasis[i].parameters() : null;
            mParas[i] = tPara;
            int tSizePara = tPara==null ? 0 : tPara.size();
            mParaSizes[i] = tSizePara;
            mParaShell[i] = new ShiftVector(tSizePara, 0, null);
            tTotParaSize += tSizePara;
        }
        mTotParaSize = tTotParaSize;
        // init fp shell
        mFpShell = new ShiftVector[mMergedBasis.length];
        mNNGradShell = new ShiftVector[mMergedBasis.length];
        mGradNNGradShell = new ShiftVector[mMergedBasis.length];
        mForwardCacheShell = new ShiftVector[mMergedBasis.length];
        mBackwardCacheShell = new ShiftVector[mMergedBasis.length];
        mForwardForceCacheShell = new ShiftVector[mMergedBasis.length];
        mBackwardForceCacheShell = new ShiftVector[mMergedBasis.length];
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSizeFp = mMergedBasis[i].size();
            mFpShell[i] = new ShiftVector(tSizeFp, 0, null);
            mNNGradShell[i] = new ShiftVector(tSizeFp, 0, null);
            mGradNNGradShell[i] = new ShiftVector(tSizeFp, 0, null);
            mForwardCacheShell[i] = new ShiftVector(0, 0, null);
            mBackwardCacheShell[i] = new ShiftVector(0, 0, null);
            mForwardForceCacheShell[i] = new ShiftVector(0, 0, null);
            mBackwardForceCacheShell[i] = new ShiftVector(0, 0, null);
        }
    }
    @Override public MergedBasis threadSafeRef() {
        MergeableBasis[] rBasis = new MergeableBasis[mMergedBasis.length];
        for (int i = 0; i < mMergedBasis.length; ++i) {
            rBasis[i] = mMergedBasis[i].threadSafeRef();
        }
        return new MergedBasis(rBasis);
    }
    @Override public void initParameters() {
        for (Basis tBasis : mMergedBasis) tBasis.initParameters();
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
    @Override public boolean hasParameters() {
        for (Basis tBasis : mMergedBasis) {
            if (tBasis.hasParameters()) return true;
        }
        return false;
    }
    
    @Override public double rcut() {return mRCut;}
    @Override public int size() {return mSize;}
    @Override public int atomTypeNumber() {return mTypeNum;}
    @Override public boolean hasSymbol() {return mSymbols != null;}
    @Override public String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    
    @Override protected void shutdown_() {
        for (Basis tBasis : mMergedBasis) {
            tBasis.shutdown();
        }
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
    public static MergedBasis load(String @NotNull[] aSymbols, Map aMap) {
        Object tObj = aMap.get("basis");
        if (tObj == null) throw new IllegalArgumentException("Key `basis` required for merge load");
        List<Map> tList = (List<Map>)tObj;
        MergeableBasis[] tMergeBasis = new MergeableBasis[tList.size()];
        for (int i = 0; i < tMergeBasis.length; ++i) {
            Map tMap = tList.get(i);
            Object tType = tMap.get("type");
            if (tType == null) {
                tType = "spherical_chebyshev";
            }
            switch(tType.toString()) {
            case "spherical_chebyshev": {
                tMergeBasis[i] = SphericalChebyshev.load(aSymbols, tMap);
                break;
            }
            case "equivariant_spherical_chebyshev": case "equ_spherical_chebyshev": {
                tMergeBasis[i] = EquivariantSphericalChebyshev.load(aSymbols, tMap);
                break;
            }
            case "chebyshev": {
                tMergeBasis[i] = Chebyshev.load(aSymbols, tMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tType);
            }}
        }
        return new MergedBasis(tMergeBasis);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MergedBasis load(int aTypeNum, Map aMap) {
        Object tObj = aMap.get("basis");
        if (tObj == null) throw new IllegalArgumentException("Key `basis` required for merge load");
        List<Map> tList = (List<Map>)tObj;
        MergeableBasis[] tMergeBasis = new MergeableBasis[tList.size()];
        for (int i = 0; i < tMergeBasis.length; ++i) {
            Map tMap = tList.get(i);
            Object tType = tMap.get("type");
            if (tType == null) {
                tType = "spherical_chebyshev";
            }
            switch(tType.toString()) {
            case "spherical_chebyshev": {
                tMergeBasis[i] = SphericalChebyshev.load(aTypeNum, tMap);
                break;
            }
            case "equivariant_spherical_chebyshev": case "equ_spherical_chebyshev": {
                tMergeBasis[i] = EquivariantSphericalChebyshev.load(aTypeNum, tMap);
                break;
            }
            case "chebyshev": {
                tMergeBasis[i] = Chebyshev.load(aTypeNum, tMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tType);
            }}
        }
        return new MergedBasis(tMergeBasis);
    }
    
    private int initForwardCacheShell_(int aNN, boolean aFullCache) {
        int rTotSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSize = mMergedBasis[i].forwardCacheSize_(aNN, aFullCache);
            mForwardCacheShell[i].setInternalDataSize(tSize);
            rTotSize += tSize;
        }
        return rTotSize;
    }
    private int initBackwardCacheShell_(int aNN) {
        int rTotSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSize = mMergedBasis[i].backwardCacheSize_(aNN);
            mBackwardCacheShell[i].setInternalDataSize(tSize);
            rTotSize += tSize;
        }
        return rTotSize;
    }
    private int initForwardForceCacheShell_(int aNN, boolean aFullCache) {
        int rTotSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSize = mMergedBasis[i].forwardForceCacheSize_(aNN, aFullCache);
            mForwardForceCacheShell[i].setInternalDataSize(tSize);
            rTotSize += tSize;
        }
        return rTotSize;
    }
    private int initBackwardForceCacheShell_(int aNN) {
        int rTotSize = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            int tSize = mMergedBasis[i].backwardForceCacheSize_(aNN);
            mBackwardForceCacheShell[i].setInternalDataSize(tSize);
            rTotSize += tSize;
        }
        return rTotSize;
    }
    
    @Override
    public final void forward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rForwardCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        if (Conf.OPERATION_CHECK) {
            if (mSize != rFp.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (mSize > rFp.size()) throw new IllegalArgumentException("data size mismatch");
        }
        validCache_(rForwardCache, initForwardCacheShell_(aNlDx.size(), aFullCache));
        int tFpShift = rFp.internalDataShift();
        int tCacheShift = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            tFp.setInternalData(rFp.internalData());
            tFp.setInternalDataShift(tFpShift);
            ShiftVector tForwardCache = mForwardCacheShell[i];
            tForwardCache.setInternalData(rForwardCache.internalData());
            tForwardCache.setInternalDataShift(tCacheShift);
            mMergedBasis[i].forward_(aNlDx, aNlDy, aNlDz, aNlType, tFp, tForwardCache, aFullCache);
            tFpShift += tFp.internalDataSize();
            tCacheShift += tForwardCache.internalDataSize();
        }
    }
    @Override
    public final void backward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleList aForwardCache, DoubleList rBackwardCache, boolean aKeepCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        if (Conf.OPERATION_CHECK) {
            if (mSize != aGradFp.size()) throw new IllegalArgumentException("data size mismatch");
            if (mTotParaSize != rGradPara.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (mSize > aGradFp.size()) throw new IllegalArgumentException("data size mismatch");
            if (mTotParaSize > rGradPara.size()) throw new IllegalArgumentException("data size mismatch");
        }
        initForwardCacheShell_(aNlDx.size(), true);
        validCache_(rBackwardCache, initBackwardCacheShell_(aNlDx.size()));
        int tFpShift = aGradFp.internalDataShift(), tParaShift = rGradPara.internalDataShift();
        int tCacheShift = 0, tBackwardCacheShift = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            ShiftVector tGradFp = mFpShell[i];
            tGradFp.setInternalData(aGradFp.internalData());
            tGradFp.setInternalDataShift(tFpShift);
            ShiftVector tGradPara = mParaShell[i];
            tGradPara.setInternalData(rGradPara.internalData());
            tGradPara.setInternalDataShift(tParaShift);
            ShiftVector tForwardCache = mForwardCacheShell[i];
            tForwardCache.setInternalData(aForwardCache.internalData());
            tForwardCache.setInternalDataShift(tCacheShift);
            ShiftVector tBackwardCache = mBackwardCacheShell[i];
            tBackwardCache.setInternalData(rBackwardCache.internalData());
            tBackwardCache.setInternalDataShift(tBackwardCacheShift);
            mMergedBasis[i].backward_(aNlDx, aNlDy, aNlDz, aNlType, tGradFp, tGradPara, tForwardCache, tBackwardCache, aKeepCache);
            tFpShift += tGradFp.internalDataSize();
            tParaShift += tGradPara.internalDataSize();
            tCacheShift += tForwardCache.internalDataSize();
            tBackwardCacheShift += tBackwardCache.internalDataSize();
        }
    }
    @Override
    public final void forwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleList aForwardCache, DoubleList rForwardForceCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 这里需要手动清空旧值
        MergeableBasis.clearForce_(rFx, rFy, rFz);
        initForwardCacheShell_(aNlDx.size(), true);
        validCache_(rForwardForceCache, initForwardForceCacheShell_(aNlDx.size(), aFullCache));
        int tFpShift = aNNGrad.internalDataShift();
        int tCacheShift = 0, tForceCacheShift = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            ShiftVector tNNGrad = mNNGradShell[i];
            tNNGrad.setInternalData(aNNGrad.internalData());
            tNNGrad.setInternalDataShift(tFpShift);
            ShiftVector tForwardCache = mForwardCacheShell[i];
            tForwardCache.setInternalData(aForwardCache.internalData());
            tForwardCache.setInternalDataShift(tCacheShift);
            ShiftVector tForwardForceCache = mForwardForceCacheShell[i];
            tForwardForceCache.setInternalData(rForwardForceCache.internalData());
            tForwardForceCache.setInternalDataShift(tForceCacheShift);
            mMergedBasis[i].forwardForceAccumulate_(aNlDx, aNlDy, aNlDz, aNlType, tNNGrad, rFx, rFy, rFz, tForwardCache, tForwardForceCache, aFullCache);
            tFpShift += tNNGrad.internalDataSize();
            tCacheShift += tForwardCache.internalDataSize();
            tForceCacheShift += tForwardForceCache.internalDataSize();
        }
    }
    @Override
    public final void backwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                                    DoubleList aForwardCache, DoubleList aForwardForceCache, DoubleList rBackwardCache, DoubleList rBackwardForceCache, boolean aKeepCache, boolean aFixBasis) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        initForwardCacheShell_(aNlDx.size(), true);
        initForwardForceCacheShell_(aNlDx.size(), true);
        validCache_(rBackwardCache, initBackwardCacheShell_(aNlDx.size()));
        validCache_(rBackwardForceCache, initBackwardForceCacheShell_(aNlDx.size()));
        int tGFpShift = aNNGrad.internalDataShift(), tGGFpShift = rGradNNGrad.internalDataShift(), tParaShift = rGradPara==null?0:rGradPara.internalDataShift();
        int tCacheShift = 0, tForceCacheShift = 0, tBackwardCacheShift = 0, tBackwardForceCacheShift = 0;
        for (int i = 0; i < mMergedBasis.length; ++i) {
            ShiftVector tNNGrad = mNNGradShell[i];
            tNNGrad.setInternalData(aNNGrad.internalData());
            tNNGrad.setInternalDataShift(tGFpShift);
            ShiftVector tGradNNGrad = mGradNNGradShell[i];
            tGradNNGrad.setInternalData(rGradNNGrad.internalData());
            tGradNNGrad.setInternalDataShift(tGGFpShift);
            ShiftVector tGradPara = rGradPara==null?null:mParaShell[i];
            if (rGradPara!=null) tGradPara.setInternalData(rGradPara.internalData());
            if (rGradPara!=null) tGradPara.setInternalDataShift(tParaShift);
            ShiftVector tForwardCache = mForwardCacheShell[i];
            tForwardCache.setInternalData(aForwardCache.internalData());
            tForwardCache.setInternalDataShift(tCacheShift);
            ShiftVector tForwardForceCache = mForwardForceCacheShell[i];
            tForwardForceCache.setInternalData(aForwardForceCache.internalData());
            tForwardForceCache.setInternalDataShift(tForceCacheShift);
            ShiftVector tBackwardCache = mBackwardCacheShell[i];
            tBackwardCache.setInternalData(rBackwardCache.internalData());
            tBackwardCache.setInternalDataShift(tBackwardCacheShift);
            ShiftVector tBackwardForceCache = mBackwardForceCacheShell[i];
            tBackwardForceCache.setInternalData(rBackwardForceCache.internalData());
            tBackwardForceCache.setInternalDataShift(tBackwardForceCacheShift);
            mMergedBasis[i].backwardForce_(aNlDx, aNlDy, aNlDz, aNlType, tNNGrad, aGradFx, aGradFy, aGradFz, tGradNNGrad, tGradPara, tForwardCache, tForwardForceCache, tBackwardCache, tBackwardForceCache, aKeepCache, aFixBasis);
            tGFpShift += tNNGrad.internalDataSize();
            tGGFpShift += tGradNNGrad.internalDataSize();
            if (rGradPara!=null) tParaShift += tGradPara.internalDataSize();
            tCacheShift += tForwardCache.internalDataSize();
            tForceCacheShift += tForwardForceCache.internalDataSize();
            tBackwardCacheShift += tBackwardCache.internalDataSize();
            tBackwardForceCacheShift += tBackwardForceCache.internalDataSize();
        }
    }
}
