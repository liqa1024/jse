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

abstract class WTypeBasis extends MergeableBasis {
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_FUSE = 4, WTYPE_EXFUSE = 6;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("fuse", WTYPE_FUSE)
        .put("exfuse", WTYPE_EXFUSE)
        .build();
    public final static int FUSE_STYLE_LIMITED = 0, FUSE_STYLE_EXTENSIVE = 1;
    final static BiMap<String, Integer> ALL_FUSE_STYLE = ImmutableBiMap.<String, Integer>builder()
        .put("limited", FUSE_STYLE_LIMITED)
        .put("extensive", FUSE_STYLE_EXTENSIVE)
        .build();
    
    final int mTypeNum;
    final int mNMax;
    final int mWType;
    final int mFuseStyle;
    final int mSizeN;
    final @Nullable RowMatrix mFuseWeight;
    final int mFuseSize;
    
    WTypeBasis(int aTypeNum, int aNMax, int aLMaxMax, int aWType, int aFuseStyle, @Nullable RowMatrix aFuseWeight) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax<0 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [0, 20], input: "+aNMax);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 2, 3, 4, 6}, input: "+ aWType);
        if (!ALL_FUSE_STYLE.containsValue(aFuseStyle)) throw new IllegalArgumentException("Input fuse_style MUST be in {0, 1}, input: "+ aFuseStyle);
        if ((aWType==WTYPE_FUSE || aWType==WTYPE_EXFUSE) && aFuseWeight==null) throw new IllegalArgumentException("Input fuse_weight MUST NOT be null when wtype=='fuse' or 'exfuse'");
        if ((aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) && aFuseWeight!=null) throw new IllegalArgumentException("Input fuse_weight MUST be null when wtype!='fuse' and 'exfuse'");
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mWType = aWType;
        mFuseStyle = aFuseStyle;
        mFuseWeight = aFuseWeight;
        mFuseSize = getFuseSize(mWType, mFuseStyle, mNMax, aLMaxMax, mFuseWeight);
        if (mFuseWeight!=null) {
            if (mFuseWeight.rowNumber()!=mTypeNum) throw new IllegalArgumentException("Row number of fuse weight mismatch");
            if (mFuseWeight.columnNumber()==0) throw new IllegalArgumentException("Column number of fuse weight MUST be non-zero");
            if (mFuseStyle==FUSE_STYLE_EXTENSIVE) {
                if (mFuseWeight.columnNumber()!=mFuseSize*(aNMax+1)*(aLMaxMax+1)) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            }
        }
        mSizeN = getSizeN_(mWType, mTypeNum, mNMax, mFuseSize);
    }
    
    static int getFuseSize(int aWType, int aFuseStyle, int aNMax, int aLMaxMax, RowMatrix aFuseWeight) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) {
            return 0;
        } else {
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                return aFuseWeight.columnNumber();
            } else {
                return aFuseWeight.columnNumber()/(aNMax+1)/(aLMaxMax+1);
            }
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
    static int getFuseStyle_(Map aMap) {
        @Nullable Object tStyle = UT.Code.get(aMap, "fuse_style");
        if (tStyle == null) return FUSE_STYLE_LIMITED;
        if (tStyle instanceof Number) return ((Number)tStyle).intValue();
        @Nullable Integer tOut = ALL_FUSE_STYLE.get(tStyle.toString());
        if (tOut == null) throw new IllegalArgumentException("Input wtype MUST be in {limited, extensive}, input: "+tStyle);
        return tOut;
    }
    @SuppressWarnings("rawtypes")
    static @Nullable RowMatrix getFuseWeight_(Map aMap, int aWType, int aFuseStyle, int aTypeNum, int aNMax, int aLMaxMax) {
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
            if (aFuseStyle==FUSE_STYLE_LIMITED) {
                if (tMat.columnNumber()!=((Number)tFuseSize).intValue()) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            } else
            if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
                if (tMat.columnNumber()!=((Number)tFuseSize).intValue()*(aNMax+1)*(aLMaxMax+1)) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            } else {
                throw new IllegalStateException();
            }
            return tMat;
        }
        if (tFuseSize==null) throw new IllegalArgumentException("Key `fuse_weight` or `fuse_size` required for fuse wtype");
        int tColNum;
        if (aFuseStyle==FUSE_STYLE_LIMITED) {
            tColNum = ((Number)tFuseSize).intValue();
        } else
        if (aFuseStyle==FUSE_STYLE_EXTENSIVE) {
            tColNum = ((Number)tFuseSize).intValue()*(aNMax+1)*(aLMaxMax+1);
        } else {
            throw new IllegalStateException();
        }
        return RowMatrix.zeros(aTypeNum, tColNum);
    }
    
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
    @Override public boolean hasParameters() {return mWType==WTYPE_FUSE || mWType==WTYPE_EXFUSE;}
}
