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
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_SINGLE = 1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_FUSE = 4;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("single", WTYPE_SINGLE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("fuse", WTYPE_FUSE)
        .build();
    public final static int DEFAULT_FUSE_SIZE = 1;
    
    final int mTypeNum;
    final int mWType;
    final @Nullable RowMatrix mFuseWeight;
    
    WTypeBasis(int aTypeNum, int aWType, @Nullable RowMatrix aFuseWeight) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 1, 2, 3, 4}, input: "+ aWType);
        if (aWType==WTYPE_FUSE && aFuseWeight==null) throw new IllegalArgumentException("Input fuse_weight MUST NOT be null when wtype=='fuse'");
        mTypeNum = aTypeNum;
        mWType = aWType;
        mFuseWeight = aFuseWeight;
        if (mFuseWeight!=null && mFuseWeight.columnNumber()!=mTypeNum) throw new IllegalArgumentException("Column number of fuse weight mismatch");
        if (mFuseWeight!=null && mFuseWeight.rowNumber()==0) throw new IllegalArgumentException("Row number of fuse weight MUST be non-zero");
    }
    WTypeBasis(int aTypeNum, int aWType) {
        this(aTypeNum, aWType, null);
    }
    
    @SuppressWarnings("rawtypes")
    static int getWType_(Map aMap) {
        @Nullable Object tType = UT.Code.get(aMap, "wtype");
        if (tType == null) return WTYPE_DEFAULT;
        if (tType instanceof Number) return ((Number)tType).intValue();
        return ALL_WTYPE.get(tType.toString());
    }
    @SuppressWarnings("rawtypes")
    static @Nullable RowMatrix getFuseWeight_(Map aMap, int aWType, int aTypeNum) {
        if (aWType != WTYPE_FUSE) return null;
        Object tFuseWeight = aMap.get("fuse_weight");
        if (tFuseWeight != null) {
            return Matrices.fromRows((List<?>)tFuseWeight);
        }
        int tFuseSize = ((Number) UT.Code.getWithDefault(aMap, DEFAULT_FUSE_SIZE, "fuse_size")).intValue();
        return Matrices.zeros(tFuseSize, aTypeNum);
    }
    
    int sizeN_(int aNMax) {
        switch(mWType) {
        case WTYPE_EXFULL: {
            return mTypeNum>1 ? (mTypeNum+1)*(aNMax+1) : (aNMax+1);
        }
        case WTYPE_FULL: {
            return mTypeNum*(aNMax+1);
        }
        case WTYPE_NONE:
        case WTYPE_SINGLE: {
            return aNMax+1;
        }
        case WTYPE_DEFAULT: {
            return mTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        }
        case WTYPE_FUSE: {
            assert mFuseWeight != null;
            return mFuseWeight.rowNumber() * (aNMax+1);
        }
        default: {
            throw new IllegalStateException();
        }}
    }
    
    @Override public void initParameters() {
        if (mFuseWeight == null) return;
        mFuseWeight.assignRow(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化
        for (IVector tRow : mFuseWeight.rows()) {
            tRow.div2this(tRow.operation().norm1() / mTypeNum);
        }
    }
    @Override public IVector parameters() {
        if (mWType != WTYPE_FUSE) return null;
        assert mFuseWeight != null;
        return mFuseWeight.asVecRow();
    }
    @Override public boolean hasParameters() {return mWType==WTYPE_FUSE;}
}
