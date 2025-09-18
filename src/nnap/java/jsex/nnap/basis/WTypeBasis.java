package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.UT;
import jse.math.matrix.ColumnMatrix;
import jse.math.matrix.Matrices;
import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

abstract class WTypeBasis extends MergeableBasis {
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_FUSE = 4;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("fuse", WTYPE_FUSE)
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
        if (aWType==WTYPE_FUSE && aFuseWeight==null) throw new IllegalArgumentException("Input fuse_weight MUST NOT be null when wtype=='fuse'");
        if (aWType!=WTYPE_FUSE && aFuseWeight!=null) throw new IllegalArgumentException("Input fuse_weight MUST be null when wtype!='fuse'");
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mWType = aWType;
        mFuseWeight = aFuseWeight;
        switch(mWType) {
        case WTYPE_EXFULL: {
            mSizeN = mTypeNum>1 ? (mTypeNum+1)*(mNMax+1) : (mNMax+1);
            break;
        }
        case WTYPE_FULL: {
            mSizeN = mTypeNum*(mNMax+1);
            break;
        }
        case WTYPE_NONE: {
            mSizeN = mNMax+1;
            break;
        }
        case WTYPE_DEFAULT: {
            mSizeN = mTypeNum>1 ? (mNMax+mNMax+2) : (mNMax+1);
            break;
        }
        case WTYPE_FUSE: {
            mSizeN = mFuseWeight.rowNumber() * (mNMax+1);
            break;
        }
        default: {
            throw new IllegalStateException();
        }}
        if (mWType == WTYPE_FUSE) {
            mFuseSize = mFuseWeight.rowNumber();
        } else {
            mFuseSize = DEFAULT_FUSE_SIZE;
        }
        if (mFuseWeight != null) {
            if (mFuseWeight.columnNumber()!=mTypeNum) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            if (mFuseWeight.rowNumber()==0) throw new IllegalArgumentException("Row number of fuse weight MUST be non-zero");
        }
    }
    WTypeBasis(int aTypeNum, int aNMax, int aWType) {
        this(aTypeNum, aNMax, aWType, null);
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
        if (aWType!=WTYPE_FUSE) return null;
        Object tFuseWeight = aMap.get("fuse_weight");
        if (tFuseWeight != null) {
            List<?> tRows = (List<?>)tFuseWeight;
            ColumnMatrix tMat = ColumnMatrix.zeros(tRows.size(), aTypeNum);
            tMat.fillWithRows(tRows);
            return tMat;
        }
        int tFuseSize = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_FUSE_SIZE, "fuse_size")).intValue();
        return ColumnMatrix.zeros(tFuseSize, aTypeNum);
    }
    
    @Override public void initParameters() {
        if (mWType!=WTYPE_FUSE) return;
        assert mFuseWeight != null;
        mFuseWeight.assignRow(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这里只对常规 fuse 这么做
        for (IVector tRow : mFuseWeight.rows()) {
            tRow.div2this(tRow.operation().norm1() / mTypeNum);
        }
    }
    @Override public IVector parameters() {
        if (mWType!=WTYPE_FUSE) return null;
        assert mFuseWeight != null;
        return mFuseWeight.asVecCol();
    }
    @Override public boolean hasParameters() {return mWType==WTYPE_FUSE;}
}
