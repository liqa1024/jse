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
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_SINGLE = 1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_DENSE = 4;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("single", WTYPE_SINGLE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("dense", WTYPE_DENSE)
        .build();
    public final static int DEFAULT_DENSE_SIZE = 1;
    
    final int mTypeNum;
    final int mWType;
    final @Nullable RowMatrix mDenseWeight;
    
    WTypeBasis(int aTypeNum, int aWType, @Nullable RowMatrix aDenseWeight) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 1, 2, 3, 4}, input: "+ aWType);
        if (aWType==WTYPE_DENSE && aDenseWeight==null) throw new IllegalArgumentException("Input dense_weight MUST NOT be null when wtype=='dense'");
        mTypeNum = aTypeNum;
        mWType = aWType;
        mDenseWeight = aDenseWeight;
        if (mDenseWeight!=null && mDenseWeight.columnNumber()!=mTypeNum) throw new IllegalArgumentException("Column number of dense weight mismatch");
        if (mDenseWeight!=null && mDenseWeight.rowNumber()==0) throw new IllegalArgumentException("Row number of dense weight MUST be non-zero");
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
    static @Nullable RowMatrix getDenseWeight_(Map aMap, int aWType, int aTypeNum) {
        if (aWType != WTYPE_DENSE) return null;
        Object tDenseWeight = aMap.get("dense_weight");
        if (tDenseWeight != null) {
            return Matrices.fromRows((List<?>)tDenseWeight);
        }
        int tDenseSize = ((Number) UT.Code.getWithDefault(aMap, DEFAULT_DENSE_SIZE, "dense_size")).intValue();
        return Matrices.zeros(tDenseSize, aTypeNum);
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
        case WTYPE_DENSE: {
            assert mDenseWeight != null;
            return mDenseWeight.rowNumber() * (aNMax+1);
        }
        default: {
            throw new IllegalStateException();
        }}
    }
    
    @Override public void initParameters() {
        if (mDenseWeight == null) return;
        mDenseWeight.assignRow(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化
        for (IVector tRow : mDenseWeight.rows()) {
            tRow.div2this(tRow.operation().norm1() / mTypeNum);
        }
    }
    @Override public IVector parameters() {
        if (mWType != WTYPE_DENSE) return null;
        assert mDenseWeight != null;
        return mDenseWeight.asVecRow();
    }
    @Override public boolean hasParameters() {return mWType==WTYPE_DENSE;}
}
