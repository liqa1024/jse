package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.UT;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 一种仅使用 Chebyshev 多项式将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这不会包含角向序，但是速度可以很快。
 * @author liqa
 */
public class Chebyshev2 extends WTypeBasis2 {
    public final static int DEFAULT_NMAX = 5;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    /** 仅对不影响整体且有部分关键势还在使用的 Chebyshev 保留兼容 */
    public final static int FUSE_STYLE_LIMITED = 0, FUSE_STYLE_EXTENSIVE = 1;
    final static BiMap<String, Integer> ALL_FUSE_STYLE = ImmutableBiMap.<String, Integer>builder()
        .put("limited", FUSE_STYLE_LIMITED)
        .put("extensive", FUSE_STYLE_EXTENSIVE)
        .build();
    
    final int mFuseStyle;
    final double mRCut;
    final int mSize;
    
    Chebyshev2(int aThisType, int aTypeNum, int aNMax, double aRCut, int aWType, int aFuseStyle, @Nullable RowMatrix aFuseWeight) {
        super(aThisType, aTypeNum, aNMax, aWType, aFuseWeight);
        if (!ALL_FUSE_STYLE.containsValue(aFuseStyle)) throw new IllegalArgumentException("Input fuse_style MUST be in {0, 1}, input: "+ aFuseStyle);
        mFuseStyle = aFuseStyle;
        // Chebyshev 对 extensive 保留兼容
        if (mFuseWeight!=null && mFuseStyle==FUSE_STYLE_EXTENSIVE) {
            mFuseSize = mFuseWeight.columnNumber()/(aNMax+1);
            if (mFuseWeight.columnNumber()!=mFuseSize*(aNMax+1)) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            mSizeN = getSizeN_(mWType, mTypeNum, mNMax, mFuseSize);
        }
        mRCut = aRCut;
        mSize = mSizeN;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        if (mFuseWeight!=null && mFuseStyle!=FUSE_STYLE_LIMITED) {
            rSaveTo.put("fuse_style", ALL_FUSE_STYLE.inverse().get(mFuseStyle));
        }
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
    }
    
    @SuppressWarnings({"rawtypes"})
    public static Chebyshev2 load(int aThisType, int aTypeNum, Map aMap) {
        int aNMax = ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue();
        if (aMap.containsKey("rfunc_scales")) throw new IllegalArgumentException("rfunc_scales is invalid now.");
        if (aMap.containsKey("system_scales")) throw new IllegalArgumentException("system_scales is invalid now.");
        int aWType = getWType_(aMap);
        int aFuseStyle = getFuseStyle_(aMap);
        RowMatrix aFuseWeight = getFuseWeight_(aMap, aWType, aFuseStyle, aTypeNum, aNMax);
        return new Chebyshev2(
            aThisType, aTypeNum, aNMax,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            aWType, aFuseStyle, aFuseWeight
        );
    }
    
    @SuppressWarnings({"rawtypes"})
    static int getFuseStyle_(Map aMap) {
        @Nullable Object tStyle = UT.Code.get(aMap, "fuse_style");
        if (tStyle == null) return FUSE_STYLE_LIMITED;
        if (tStyle instanceof Number) return ((Number)tStyle).intValue();
        @Nullable Integer tOut = ALL_FUSE_STYLE.get(tStyle.toString());
        if (tOut == null) throw new IllegalArgumentException("Input wtype MUST be in {limited, extensive}, input: "+tStyle);
        return tOut;
    }
    @SuppressWarnings("rawtypes")
    static @Nullable RowMatrix getFuseWeight_(Map aMap, int aWType, int aFuseStyle, int aTypeNum, int aNMax) {
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
                if (tMat.columnNumber()!=((Number)tFuseSize).intValue()*(aNMax+1)) throw new IllegalArgumentException("Column number of fuse weight mismatch");
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
            tColNum = ((Number)tFuseSize).intValue()*(aNMax+1);
        } else {
            throw new IllegalStateException();
        }
        return RowMatrix.zeros(aTypeNum, tColNum);
    }
    
    /** @return {@inheritDoc} */
    public double rcut() {return mRCut;}
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)(lmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)(lmax+1)}
     */
    @Override public int size() {return mSize;}
    /** @return {@inheritDoc} */
    public int atomTypeNumber() {return mTypeNum;}
    /** @return {@inheritDoc} */
    public int thisType() {return mThisType;}
    
    public int forwardCacheSize(int aNN, int aCacheLevel) {
        return aCacheLevel>0 ? aNN*(mNMax+1 + 1) : (mNMax+1);
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap) {
        super.updateGenMap(rGenMap);
        int ti = mThisType-1;
        rGenMap.put(ti+":NNAPGEN_FP_FSTYLE", mFuseStyle);
    }
}
