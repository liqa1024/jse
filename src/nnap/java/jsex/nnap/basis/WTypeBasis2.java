package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.UT;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

abstract class WTypeBasis2 extends MergeableBasis2 {
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_FUSE = 4, WTYPE_RFUSE = 5, WTYPE_EXFUSE = 6;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("fuse", WTYPE_FUSE)
        .put("exfuse", WTYPE_EXFUSE)
        .build();
    
    final double mRCut;
    final int mNumTypes;
    final int mNMax;
    final int mWType, mInternalWType;
    final int mTypedWType;
    
    final @Nullable RowMatrix mFuseWeight;
    final int mSizeN;
    final int mFuseSize;
    
    final Vector mPostFuseWeight;
    final int mPostFuseSize;
    final double[] mPostFuseScale;
    
    final int mSizeNP;
    final RowMatrix mRFuseWeight;
    
    WTypeBasis2(double aRCut, int aNumTypes, int aNMax, int aWType, @Nullable RowMatrix aFuseWeight, @Nullable Vector aPostFuseWeight, double @Nullable[] aPostFuseScale) {
        if (aNumTypes <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+ aNumTypes);
        if (aNMax<0 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [0, 20], input: "+aNMax);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 2, 3, 4, 6}, input: "+ aWType);
        if ((aWType==WTYPE_FUSE || aWType==WTYPE_EXFUSE) && aFuseWeight==null) throw new IllegalArgumentException("Input fuse_weight MUST NOT be null when wtype=='fuse' or 'exfuse'");
        if ((aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) && aFuseWeight!=null) throw new IllegalArgumentException("Input fuse_weight MUST be null when wtype!='fuse' and 'exfuse'");
        
        mRCut = aRCut;
        mNumTypes = aNumTypes;
        mNMax = aNMax;
        mWType = aWType;
        mFuseWeight = aFuseWeight;
        mFuseSize = getFuseSize(mWType, mFuseWeight);
        if (mFuseWeight!=null) {
            if (mFuseWeight.nrows()!= mNumTypes) throw new IllegalArgumentException("Row number of fuse weight mismatch");
            if (mFuseWeight.ncols()==0) throw new IllegalArgumentException("Column number of fuse weight MUST be non-zero");
        }
        mSizeN = getSizeN_(mWType, mNumTypes, mNMax, mFuseSize);
        
        mPostFuseWeight = aPostFuseWeight;
        if (mPostFuseWeight==null) {
            mPostFuseSize = 0;
        } else {
            mPostFuseSize = mPostFuseWeight.size()/mSizeN;
        }
        if (mPostFuseWeight!=null) {
            if (mPostFuseWeight.size()!=mPostFuseSize*mSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
        }
        mPostFuseScale = aPostFuseScale==null ? new double[]{1.0} : aPostFuseScale;
        if (mPostFuseScale.length!=1) throw new IllegalArgumentException("Size of post fuse scale mismatch");
        
        mSizeNP = mPostFuseWeight==null ? mSizeN : mPostFuseSize;
        mRFuseWeight = RowMatrix.zeros(mSizeNP*mNumTypes, mNMax+1);
        
        if (mNumTypes ==1) {
            switch(mWType) {
            case WTYPE_EXFULL: case WTYPE_FULL: case WTYPE_NONE: case WTYPE_DEFAULT: {
                mTypedWType = WTYPE_NONE;
                break;
            }
            default: {
                mTypedWType = aWType;
                break;
            }}
        } else {
            mTypedWType = aWType;
        }
        if (mPostFuseWeight==null) {
            mInternalWType = mTypedWType;
        } else {
            mInternalWType = WTYPE_RFUSE;
        }
        updateRFuseWeight_(false);
    }
    
    final void updateRFuseWeight_(boolean aCheck) {
        if (aCheck) {
            if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE && mPostFuseWeight==null) return;
        }
        // 简单总是清空旧值
        mRFuseWeight.fill(0.0);
        if (mPostFuseWeight==null) {
            switch(mTypedWType) {
            case WTYPE_FUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int k = 0; k < mFuseSize; ++k) {
                        double wt = mFuseWeight.get(type-1, k);
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.set((type-1)*mSizeNP + k*(mNMax+1) + n, n, wt);
                        }
                    }
                }
                return;
            }
            case WTYPE_EXFUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.set((type-1)*mSizeNP + n, n, 1.0); // ex term
                    }
                    for (int k = 0; k < mFuseSize; ++k) {
                        double wt = mFuseWeight.get(type-1, k);
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.set((type-1)*mSizeNP + (k+1)*(mNMax+1) + n, n, wt);
                        }
                    }
                }
                return;
            }
            case WTYPE_FULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.set((type-1)*mSizeNP + (type-1)*(mNMax+1) + n, n, 1.0);
                    }
                }
                return;
            }
            case WTYPE_EXFULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.set((type-1)*mSizeNP + n, n, 1.0); // ex term
                        mRFuseWeight.set((type-1)*mSizeNP + type*(mNMax+1) + n, n, 1.0);
                    }
                }
                return;
            }
            case WTYPE_NONE: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.set((type-1)*mSizeNP + n, n, 1.0);
                    }
                }
                return;
            }
            case WTYPE_DEFAULT: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    double wt = ((type&1)==1) ? type : (-type);
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.set((type-1)*mSizeNP + n, n, 1.0);
                        mRFuseWeight.set((type-1)*mSizeNP + (mNMax+1) + n, n, wt);
                    }
                }
                return;
            }}
        } else {
            switch(mTypedWType) {
            case WTYPE_FUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        IVector tRow = mRFuseWeight.row((type-1)*mSizeNP + np);
                        tRow.fill(0.0);
                        for (int k = 0; k < mFuseSize; ++k) {
                            double wt = mFuseWeight.get(type-1, k);
                            int tShift = np*mSizeN + k*(mNMax+1);
                            tRow.op().mplus2this(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1)), wt);
                        }
                    }
                }
                mRFuseWeight.multiply2this(mPostFuseScale[0]);
                return;
            }
            case WTYPE_EXFUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        IVector tRow = mRFuseWeight.row((type-1)*mSizeNP + np);
                        int tShift = np*mSizeN;
                        tRow.fill(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1))); // ex term
                        for (int k = 0; k < mFuseSize; ++k) {
                            double wt = mFuseWeight.get(type-1, k);
                            tShift = np*mSizeN + (k+1)*(mNMax+1);
                            tRow.op().mplus2this(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1)), wt);
                        }
                    }
                }
                mRFuseWeight.multiply2this(mPostFuseScale[0]);
                return;
            }
            case WTYPE_FULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        IVector tRow = mRFuseWeight.row((type-1)*mSizeNP + np);
                        int tShift = np*mSizeN + (type-1)*(mNMax+1);
                        tRow.fill(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1)));
                    }
                }
                mRFuseWeight.multiply2this(mPostFuseScale[0]);
                return;
            }
            case WTYPE_EXFULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        IVector tRow = mRFuseWeight.row((type-1)*mSizeNP + np);
                        int tShift = np*mSizeN;
                        tRow.fill(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1))); // ex term
                        tShift = np*mSizeN + type*(mNMax+1);
                        tRow.plus2this(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1)));
                    }
                }
                mRFuseWeight.multiply2this(mPostFuseScale[0]);
                return;
            }
            case WTYPE_NONE: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        IVector tRow = mRFuseWeight.row((type-1)*mSizeNP + np);
                        int tShift = np*mSizeN;
                        tRow.fill(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1)));
                    }
                }
                mRFuseWeight.multiply2this(mPostFuseScale[0]);
                return;
            }
            case WTYPE_DEFAULT: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    double wt = ((type&1)==1) ? type : (-type);
                    for (int np = 0; np < mSizeNP; ++np) {
                        IVector tRow = mRFuseWeight.row((type-1)*mSizeNP + np);
                        int tShift = np*mSizeN;
                        tRow.fill(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1)));
                        tShift = np*mSizeN + (mNMax+1);
                        tRow.op().mplus2this(mPostFuseWeight.subVec(tShift, tShift+(mNMax+1)), wt);
                    }
                }
                mRFuseWeight.multiply2this(mPostFuseScale[0]);
                return;
            }}
        }
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    final void save_(Map rSaveTo) {
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asListRows());
        }
        rSaveTo.put("post_fuse", mPostFuseWeight!=null);
        if (mPostFuseWeight!=null) {
            rSaveTo.put("post_fuse_size", mPostFuseSize);
            rSaveTo.put("post_fuse_scale", mPostFuseScale[0]);
            rSaveTo.put("post_fuse_weight", mPostFuseWeight.asList());
        }
    }
    static int getFuseSize(int aWType, RowMatrix aFuseWeight) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) {
            return 0;
        } else {
            return aFuseWeight.ncols();
        }
    }
    static int getSizeN_(int aWType, int aNumTypes, int aNMax, int aFuseSize) {
        switch(aWType) {
        case WTYPE_EXFULL: {
            return aNumTypes>1 ? (aNumTypes+1)*(aNMax+1) : (aNMax+1);
        }
        case WTYPE_FULL: {
            return aNumTypes*(aNMax+1);
        }
        case WTYPE_NONE: {
            return aNMax+1;
        }
        case WTYPE_DEFAULT: {
            return aNumTypes>1 ? (aNMax+aNMax+2) : (aNMax+1);
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
    static @Nullable RowMatrix getFuseWeight_(Map aMap, int aWType, int aNumTypes) {
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
            if (tMat.ncols()!=((Number)tFuseSize).intValue()) throw new IllegalArgumentException("Column number of fuse weight mismatch");
            return tMat;
        }
        if (tFuseSize==null) throw new IllegalArgumentException("Key `fuse_weight` or `fuse_size` required for fuse wtype");
        return RowMatrix.zeros(aNumTypes, ((Number)tFuseSize).intValue());
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getPostFuseWeight_(Map aMap, int aSizeN) {
        Object tFlag = aMap.get("post_fuse");
        if (tFlag==null || (!(Boolean)tFlag)) return null;
        Object tPostFuseSize = aMap.get("post_fuse_size");
        Object tPostFuseWeight = aMap.get("post_fuse_weight");
        if (tPostFuseWeight!=null) {
            Vector tVec = Vectors.from((List)tPostFuseWeight);
            if (tPostFuseSize!=null) {
                if (tVec.size()!=((Number)tPostFuseSize).intValue()*aSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
            }
            return tVec;
        }
        if (tPostFuseSize==null) throw new IllegalArgumentException("Key `post_fuse_weight` or `post_fuse_size` required for post_fuse");
        int tSize = ((Number)tPostFuseSize).intValue()*aSizeN;
        return Vector.zeros(tSize);
    }
    
    /** @return {@inheritDoc} */
    @Override public abstract int size();
    /** @return {@inheritDoc} */
    @Override public double rcut() {return mRCut;}
    public int ntypes() {return mNumTypes;}
    
    private void initFuseWeight_() {
        if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE) return;
        assert mFuseWeight != null;
        mFuseWeight.assignRow(() -> RANDOM.nextDouble(-1, 1));
        // 权重按照种类归一化，注意只有一种的情况下不专门归一化；经验调整，只是可以加速训练
        if (mNumTypes > 1) for (IVector tCol : mFuseWeight.cols()) {
            tCol.div2this(tCol.operation().norm1() / mNumTypes);
        }
    }
    private void initPostFuseWeight_() {
        // 补充对于 PostFuseWeight 的初始化
        if (mPostFuseWeight == null) return;
        mPostFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这个可以有效加速训练；经验设定
        int tShift = 0;
        for (int np = 0; np < mPostFuseSize; ++np) {
            IVector tSubVec = mPostFuseWeight.subVec(tShift, tShift + mSizeN);
            tSubVec.div2this(tSubVec.operation().norm1() / mSizeN);
            tShift += mSizeN;
        }
        // 在这个经验设定下，scale 设置为此值确保输出的基组值数量级一致
        mPostFuseScale[0] = MathEX.Fast.sqrt(1.0 / mSizeN);
    }
    @Override public void initParameters() {
        initFuseWeight_();
        initPostFuseWeight_();
        // 参数更新
        updateRFuseWeight_(true);
    }
    
    @Override public IVector parameters() {
        return mRFuseWeight.asVecRow();
    }
    @Override public int parameterSize() {
        return mRFuseWeight.internalDataSize();
    }
    
    @Override public IVector fittableParameters() {
        final IVector tPara;
        if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE) {
            tPara = null;
        } else {
            assert mFuseWeight != null;
            tPara = mFuseWeight.asVecRow();
        }
        if (mPostFuseWeight==null) return tPara;
        // 补充对于 PostFuseWeight 的参数
        if (tPara == null) return mPostFuseWeight;
        final int tParaSize = tPara.size();
        final int tPostParaSize = mPostFuseWeight.size();
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx < tParaSize) {
                    return tPara.get(aIdx);
                }
                aIdx -= tParaSize;
                if (aIdx < tPostParaSize) {
                    return mPostFuseWeight.get(aIdx);
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                if (aIdx < tParaSize) {
                    tPara.set(aIdx, aValue);
                    return;
                }
                aIdx -= tParaSize;
                if (aIdx < tPostParaSize) {
                    mPostFuseWeight.set(aIdx, aValue);
                    return;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return tParaSize+tPostParaSize;
            }
        };
    }
    @Override public int fittableParameterSize() {
        final int tParaSize;
        if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE) {
            tParaSize = 0;
        } else {
            assert mFuseWeight != null;
            tParaSize = mFuseWeight.internalDataSize();
        }
        if (mPostFuseWeight==null) return tParaSize;
        return tParaSize + mPostFuseWeight.size();
    }
    @Override public boolean hasFittableParameters() {
        return mTypedWType==WTYPE_FUSE || mTypedWType==WTYPE_EXFUSE || mPostFuseWeight!=null;
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge) {
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE", size());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_HPARAM", hyperParameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_PARAM", parameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_WTYPE", mInternalWType);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_NMAX", mNMax);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_NP", mSizeNP);
    }
    @Override public boolean hasSameGenMap(MergeableBasis2 aBasis) {
        if (!(aBasis instanceof WTypeBasis2)) return false;
        WTypeBasis2 tBasis = (WTypeBasis2)aBasis;
        return size()==tBasis.size() && hyperParameterSize()==tBasis.hyperParameterSize() && parameterSize()==tBasis.parameterSize() &&
            mNumTypes==tBasis.mNumTypes && (mPostFuseWeight!=null)==(tBasis.mPostFuseWeight!=null) && mWType==tBasis.mWType && mNMax==tBasis.mNMax && mSizeNP==tBasis.mSizeNP;
    }
}
