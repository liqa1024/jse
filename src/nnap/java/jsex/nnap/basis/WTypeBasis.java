package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.UT;
import jse.math.matrix.ColumnMatrix;
import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

abstract class WTypeBasis extends MergeableBasis {
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_FUSE = 4, WTYPE_RFUSE = 5;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("fuse", WTYPE_FUSE)
        .put("rfuse", WTYPE_RFUSE)
        .build();
    public final static int DEFAULT_FUSE_SIZE = 1;
    
    final int mTypeNum;
    final int mNMax;
    final int mWType;
    final int mSizeN;
    final @Nullable ColumnMatrix mFuseWeight;
    final int mFuseSize;
    
    WTypeBasis(int aTypeNum, int aNMax, int aWType, @Nullable ColumnMatrix aFuseWeight) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax<0 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [0, 20], input: "+aNMax);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 2, 3, 4, 5}, input: "+ aWType);
        if ((aWType==WTYPE_FUSE || aWType==WTYPE_RFUSE) && aFuseWeight==null) throw new IllegalArgumentException("Input fuse_weight MUST NOT be null when wtype=='fuse' or 'rfuse'");
        if ((aWType!=WTYPE_FUSE && aWType!=WTYPE_RFUSE) && aFuseWeight!=null) throw new IllegalArgumentException("Input fuse_weight MUST be null when wtype!='fuse' and 'rfuse'");
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mWType = aWType;
        mFuseWeight = aFuseWeight;
        mSizeN = getSizeN_(mWType, mTypeNum, mNMax, mFuseWeight);
        if (mWType == WTYPE_FUSE) {
            mFuseSize = mFuseWeight.rowNumber();
        } else
        if (mWType == WTYPE_RFUSE) {
            mFuseSize = mSizeN;
        } else {
            mFuseSize = DEFAULT_FUSE_SIZE;
        }
        if (mFuseWeight != null) {
            if (mFuseWeight.columnNumber()!=mTypeNum) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            if (mFuseWeight.rowNumber()==0) throw new IllegalArgumentException("Row number of fuse weight MUST be non-zero");
            if (mWType==WTYPE_RFUSE && mFuseWeight.rowNumber()!=mFuseSize*(mNMax+1)) throw new IllegalArgumentException("Row number of fuse weight mismatch");
        }
    }
    WTypeBasis(int aTypeNum, int aNMax, int aWType) {
        this(aTypeNum, aNMax, aWType, null);
    }
    
    static int getSizeN_(int aWType, int aTypeNum, int aNMax, ColumnMatrix aFuseWeight) {
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
            return aFuseWeight.rowNumber() * (aNMax+1);
        }
        case WTYPE_RFUSE: {
            return aFuseWeight.rowNumber() / (aNMax+1);
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
        if (tOut == null) throw new IllegalArgumentException("Input wtype MUST be in {default, none, full, exfull, fuse, rfuse}, input: "+tType);
        return tOut;
    }
    @SuppressWarnings("rawtypes")
    static @Nullable ColumnMatrix getFuseWeight_(Map aMap, int aWType, int aTypeNum, int aNMax) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_RFUSE) return null;
        Object tFuseWeight = aMap.get("fuse_weight");
        if (tFuseWeight != null) {
            List<?> tRows = (List<?>)tFuseWeight;
            ColumnMatrix tMat = ColumnMatrix.zeros(tRows.size(), aTypeNum);
            tMat.fillWithRows(tRows);
            return tMat;
        }
        if (aWType == WTYPE_FUSE) {
            int tFuseSize = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_FUSE_SIZE, "fuse_size")).intValue();
            return ColumnMatrix.zeros(tFuseSize, aTypeNum);
        }
        // aWType == WTYPE_RFUSE
        int tFuseSize = ((Number)UT.Code.getWithDefault(aMap, aNMax+1, "fuse_size")).intValue();
        return ColumnMatrix.zeros(tFuseSize*(aNMax+1), aTypeNum);
    }
    
    @Override public void initParameters() {
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_RFUSE) return;
        assert mFuseWeight != null;
        mFuseWeight.assignRow(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，对于 fuse 和 rfuse 操作不同
        if (mWType == WTYPE_FUSE) {
            for (IVector tRow : mFuseWeight.rows()) {
                tRow.div2this(tRow.operation().norm1() / mTypeNum);
            }
        } else {
            for (IVector tCol : mFuseWeight.cols()) {
                int tShift = 0;
                for (int np = 0; np < mFuseSize; ++np) {
                    IVector tSubCol = tCol.subVec(tShift, tShift + (mNMax+1));
                    tSubCol.div2this(tSubCol.operation().norm1() / (mNMax+1));
                    tShift += (mNMax+1);
                }
            }
        }
    }
    @Override public IVector parameters() {
        if (mWType!=WTYPE_FUSE && mWType!=WTYPE_RFUSE) return null;
        assert mFuseWeight != null;
        return mFuseWeight.asVecCol();
    }
    @Override public boolean hasParameters() {return mWType==WTYPE_FUSE || mWType==WTYPE_RFUSE;}
}
