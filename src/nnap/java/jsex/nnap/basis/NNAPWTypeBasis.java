package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.collection.DoubleList;
import org.jetbrains.annotations.Nullable;

class NNAPWTypeBasis {
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_SINGLE = 1, WTYPE_FULL = 2, WTYPE_EXFULL = 3;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("single", WTYPE_SINGLE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .build();
    
    static int getWType_(@Nullable Object aType) {
        if (aType == null) return WTYPE_DEFAULT;
        if (aType instanceof Number) return ((Number)aType).intValue();
        return ALL_WTYPE.get(aType.toString());
    }
    
    static int sizeN_(int aNMax, int aTypeNum, int aWType) {
        switch(aWType) {
        case WTYPE_EXFULL: {
            return aTypeNum>1 ? (aTypeNum+1)*(aNMax+1) : (aNMax+1);
        }
        case WTYPE_FULL: {
            return aTypeNum*(aNMax+1);
        }
        case WTYPE_NONE:
        case WTYPE_SINGLE: {
            return aNMax+1;
        }
        case WTYPE_DEFAULT: {
            return aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        }
        default: {
            throw new IllegalStateException();
        }}
    }
    
    static void validSize_(DoubleList aData, int aSize) {
        aData.ensureCapacity(aSize);
        aData.setInternalDataSize(aSize);
    }
}
