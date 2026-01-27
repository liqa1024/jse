package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.UT;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

abstract class WTypeBasis2 extends MergeableBasis2 {
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_FUSE = 4, WTYPE_EXFUSE = 6;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("fuse", WTYPE_FUSE)
        .put("exfuse", WTYPE_EXFUSE)
        .build();
    
    final int mTypeNum;
    final int mNMax;
    final int mWType;
    final @Nullable RowMatrix mFuseWeight;
    int mSizeN;
    int mFuseSize;
    
    WTypeBasis2(int aTypeNum, int aNMax, int aWType, @Nullable RowMatrix aFuseWeight) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax<0 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [0, 20], input: "+aNMax);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 2, 3, 4, 6}, input: "+ aWType);
        if ((aWType==WTYPE_FUSE || aWType==WTYPE_EXFUSE) && aFuseWeight==null) throw new IllegalArgumentException("Input fuse_weight MUST NOT be null when wtype=='fuse' or 'exfuse'");
        if ((aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) && aFuseWeight!=null) throw new IllegalArgumentException("Input fuse_weight MUST be null when wtype!='fuse' and 'exfuse'");
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mWType = aWType;
        mFuseWeight = aFuseWeight;
        mFuseSize = getFuseSize(mWType, mFuseWeight);
        if (mFuseWeight!=null) {
            if (mFuseWeight.rowNumber()!=mTypeNum) throw new IllegalArgumentException("Row number of fuse weight mismatch");
            if (mFuseWeight.columnNumber()==0) throw new IllegalArgumentException("Column number of fuse weight MUST be non-zero");
        }
        mSizeN = getSizeN_(mWType, mTypeNum, mNMax, mFuseSize);
    }
    
    static int getFuseSize(int aWType, RowMatrix aFuseWeight) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) {
            return 0;
        } else {
            return aFuseWeight.columnNumber();
        }
    }
    static int getSizeN_(int aWType, int aTypeNum, int aNMax, int aFuseSize) {
        switch(aWType) {
        case WTYPE_EXFULL: {
            return aTypeNum>1 ? (aTypeNum+1)*(aNMax+1) : (aNMax+1);
        }
        case WTYPE_FULL: {
            return aTypeNum*(aNMax+1);
        }
        case WTYPE_NONE: {
            return aNMax+1;
        }
        case WTYPE_DEFAULT: {
            return aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        }
        case WTYPE_FUSE: {
            return aFuseSize * (aNMax+1);
        }
        case WTYPE_EXFUSE: {
            return (aFuseSize+1) * (aNMax+1);
        }
        default: {
            throw new IllegalStateException();
        }}
    }
    @SuppressWarnings("rawtypes")
    static int getWType_(Map aMap) {
        @Nullable Object tType = UT.Code.get(aMap, "wtype");
        if (tType == null) return WTYPE_DEFAULT;
        if (tType instanceof Number) return ((Number)tType).intValue();
        @Nullable Integer tOut = ALL_WTYPE.get(tType.toString());
        if (tOut == null) throw new IllegalArgumentException("Input fuse_style MUST be in {default, none, full, exfull, fuse, exfuse}, input: "+tType);
        return tOut;
    }
    @SuppressWarnings("rawtypes")
    static @Nullable RowMatrix getFuseWeight_(Map aMap, int aWType, int aTypeNum) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) return null;
        Object tFuseSize = UT.Code.get(aMap, "fuse_size");
        Object tFuseWeight = aMap.get("fuse_weight");
        if (tFuseWeight!=null) {
            if (tFuseSize==null) {
                // 如果没有 fuse_size 则是旧版，按列读取
                return Matrices.fromCols((List<?>)tFuseWeight);
            }
            // 否则按行读取
            RowMatrix tMat = Matrices.fromRows((List<?>)tFuseWeight);
            if (tMat.columnNumber()!=((Number)tFuseSize).intValue()) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            return tMat;
        }
        if (tFuseSize==null) throw new IllegalArgumentException("Key `fuse_weight` or `fuse_size` required for fuse wtype");
        return RowMatrix.zeros(aTypeNum, ((Number)tFuseSize).intValue());
    }
    
    @Override public abstract int size();
    public int atomTypeNumber() {return mTypeNum;}
    
    @Override public void initParameters() {
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_EXFUSE) return;
        assert mFuseWeight != null;
        mFuseWeight.assignRow(() -> RANDOM.nextDouble(-1, 1));
        // 权重按照种类归一化，注意只有一种的情况下不专门归一化；经验调整，只是可以加速训练
        if (mTypeNum > 1) for (IVector tCol : mFuseWeight.cols()) {
            tCol.div2this(tCol.operation().norm1() / mTypeNum);
        }
    }
    @Override public IVector parameters() {
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_EXFUSE) return null;
        assert mFuseWeight != null;
        return mFuseWeight.asVecRow();
    }
    @Override public int parameterSize() {
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_EXFUSE) return 0;
        assert mFuseWeight != null;
        return mFuseWeight.internalDataSize();
    }
    @Override public boolean hasParameters() {return mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE;}
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge) {
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE", size());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_HPARAM", hyperParameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_PARAM", parameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_FW", mFuseWeight==null?0:mFuseWeight.internalDataSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_NTYPES", mTypeNum);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_WTYPE", mWType);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_NMAX", mNMax);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_FSIZE", mFuseSize);
    }
    @Override public boolean hasSameGenMap(MergeableBasis2 aBasis) {
        if (!(aBasis instanceof WTypeBasis2)) return false;
        WTypeBasis2 tBasis = (WTypeBasis2)aBasis;
        return mTypeNum==tBasis.mTypeNum && size()==tBasis.size() && mWType==tBasis.mWType && mNMax==tBasis.mNMax && mFuseSize==tBasis.mFuseSize;
    }
}
