package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.Conf;
import jse.code.UT;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.RANDOM;

abstract class WTypeBasis extends MergeableBasis {
    public final static int WTYPE_DEFAULT = 0, WTYPE_NONE = -1, WTYPE_FULL = 2, WTYPE_EXFULL = 3, WTYPE_FUSE = 4, WTYPE_RFUSE = 5, WTYPE_EXFUSE = 6;
    final static BiMap<String, Integer> ALL_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTYPE_DEFAULT)
        .put("none", WTYPE_NONE)
        .put("full", WTYPE_FULL)
        .put("exfull", WTYPE_EXFULL)
        .put("fuse", WTYPE_FUSE)
        .put("rfuse", WTYPE_RFUSE)
        .put("exfuse", WTYPE_EXFUSE)
        .build();
    
    final double mRCut;
    final int mNumTypes;
    final int mNMax;
    final int mWType, mInternalWType;
    final int mTypedWType;
    
    @Nullable Vector mFuseWeight;
    @Nullable Vector mGradFuseWeight = null;
    final int mSizeN;
    final int mFuseSize;
    
    @Nullable Vector mRFuseWeight;
    @Nullable Vector mGradRFuseWeight = null;
    final int mSizeNP;
    final int mRFuseSize;
    final double[] mRFuseScale;
    
    final boolean mLayerNorm, mInternalLayerNorm;
    @Nullable Vector mLayerNormBeta, mLayerNormGamma;
    @Nullable Vector mGradLayerNormBeta = null, mGradLayerNormGamma = null;
    
    IDoubleOrFloatCPointer mInternalRFuseWeight = null;
    IDoubleOrFloatCPointer[] mInternalGradRFuseWeight = null;
    IDoubleOrFloatCPointer mInternalLayerNormBeta = null, mInternalLayerNormGamma = null;
    IDoubleOrFloatCPointer[] mInternalGradLayerNormBeta = null, mInternalGradLayerNormGamma = null;
    
    WTypeBasis(double aRCut, int aNumTypes, int aNMax, int aWType, @Nullable Vector aFuseWeight, @Nullable Vector aRFuseWeight, double @Nullable[] aRFuseScale,
               boolean aLayerNorm, @Nullable Vector aLayerNormBeta, @Nullable Vector aLayerNormGamma) {
        if (aNumTypes <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+ aNumTypes);
        if (aNMax<0 || aNMax>20) throw new IllegalArgumentException("Input nmax MUST be in [0, 20], input: "+aNMax);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 2, 3, 4, 6}, input: "+ aWType);
        if ((aWType==WTYPE_FUSE || aWType==WTYPE_EXFUSE) && aFuseWeight==null) throw new IllegalArgumentException("Input fuse_weight MUST NOT be null when wtype=='fuse' or 'exfuse'");
        if ((aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) && aFuseWeight!=null) throw new IllegalArgumentException("Input fuse_weight MUST be null when wtype!='fuse' and 'exfuse'");
        if ((aWType==WTYPE_RFUSE) && aRFuseWeight==null) throw new IllegalArgumentException("Input rfuse_weight MUST NOT be null when wtype=='rfuse'");
        if ((aWType!=WTYPE_RFUSE) && aRFuseWeight!=null) throw new IllegalArgumentException("Input rfuse_weight MUST be null when wtype!='rfuse'");
        if (!aLayerNorm && (aLayerNormBeta!=null || aLayerNormGamma!=null)) throw new IllegalArgumentException("Input ln_beta & ln_gamma MUST be null when ln==false");
        
        mRCut = aRCut;
        mNumTypes = aNumTypes;
        mNMax = aNMax;
        mWType = aWType;
        mFuseWeight = aFuseWeight;
        mFuseSize = getFuseSize(mWType, mNumTypes, mFuseWeight);
        if (mFuseWeight!=null) {
            if (mFuseWeight.size()!=mNumTypes*mFuseSize) throw new IllegalArgumentException("Size of fuse weight mismatch");
        }
        mSizeN = getSizeN_(mWType, mNumTypes, mNMax, mFuseSize);
        
        mRFuseWeight = aRFuseWeight;
        if (mRFuseWeight==null) {
            mRFuseSize = 0;
        } else {
            mRFuseSize = mRFuseWeight.size()/mSizeN;
        }
        if (mRFuseWeight!=null) {
            if (mRFuseWeight.size()!=mRFuseSize*mSizeN) throw new IllegalArgumentException("Size of rfuse weight mismatch");
        }
        mRFuseScale = aRFuseScale==null ? new double[]{1.0} : aRFuseScale;
        if (mRFuseScale.length!=1) throw new IllegalArgumentException("Size of rfuse scale mismatch");
        mSizeNP = mRFuseWeight==null ? mSizeN : mRFuseSize;
        
        if (mNumTypes==1) {
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
        if (mRFuseWeight==null) {
            mInternalWType = mTypedWType;
        } else {
            mInternalWType = WTYPE_RFUSE;
        }
        
        mLayerNorm = aLayerNorm;
        mInternalLayerNorm = mLayerNorm && aWType==WTYPE_RFUSE;
        if (mInternalLayerNorm) {
            mLayerNormBeta = aLayerNormBeta!=null ? aLayerNormBeta : Vectors.zeros(mSizeNP);
            mLayerNormGamma = aLayerNormGamma!=null ? aLayerNormGamma : Vectors.zeros(mSizeNP);
            if (mLayerNormBeta.size()!=mSizeNP) throw new IllegalArgumentException("Size of ln_beta mismatch");
            if (mLayerNormGamma.size()!=mSizeNP) throw new IllegalArgumentException("Size of ln_gamma mismatch");
        } else {
            mLayerNormBeta = aLayerNormBeta;
            mLayerNormGamma = aLayerNormGamma;
        }
    }
    @Override public void requireGrad(int aNumThreads) {
        if (mInternalGradRFuseWeight !=null) return;
        mInternalGradRFuseWeight = new IDoubleOrFloatCPointer[aNumThreads];
        mInternalGradLayerNormBeta = new IDoubleOrFloatCPointer[aNumThreads];
        mInternalGradLayerNormGamma = new IDoubleOrFloatCPointer[aNumThreads];
    }
    
    @Override public void updateParameters() {
        updateParameters_(true);
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    @Override public void backwardParameter() {
        if (mInternalGradRFuseWeight==null) throw new IllegalStateException("invoke `requireGrad(nthreads)` first.");
        if (mInternalLayerNorm) {
            int tNumThreads = mInternalGradRFuseWeight.length;
            for (int ti = 0; ti < tNumThreads; ++ti) {
                assert mGradLayerNormBeta!=null && mGradLayerNormGamma!=null;
                IDoubleOrFloatCPointer tInternalGradLayerNormBeta = mInternalGradLayerNormBeta[ti];
                IDoubleOrFloatCPointer tInternalGradLayerNormGamma = mInternalGradLayerNormGamma[ti];
                for (int i = 0; i < mSizeNP; ++i) {
                    mGradLayerNormBeta.add(i, tInternalGradLayerNormBeta.getAtD(i));
                    mGradLayerNormGamma.add(i, tInternalGradLayerNormGamma.getAtD(i));
                }
            }
        }
        if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE && mRFuseWeight==null) return;
        for (IDoubleOrFloatCPointer tInternalGradRFuseWeight : mInternalGradRFuseWeight) {
            double tScale = mRFuseScale[0];
            switch(mTypedWType) {
            case WTYPE_RFUSE: {
                assert mGradRFuseWeight != null;
                final int tSize = mSizeN*mSizeNP;
                for (int i = 0; i < tSize; ++i) {
                    mGradRFuseWeight.add(i, tScale * tInternalGradRFuseWeight.getAtD(i));
                }
                break;
            }
            case WTYPE_FUSE: {
                assert mGradFuseWeight != null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int k = 0; k < mFuseSize; ++k) {
                        double rGradWt = 0.0;
                        for (int n = 0; n <= mNMax; ++n) {
                            rGradWt += tInternalGradRFuseWeight.getAtD(((type-1)*mSizeNP + k*(mNMax+1) + n)*(mNMax+1) + n);
                        }
                        mGradFuseWeight.add((type - 1) * mFuseSize + k, tScale * rGradWt);
                    }
                }
                break;
            }
            case WTYPE_EXFUSE: {
                assert mGradFuseWeight != null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int k = 0; k < mFuseSize; ++k) {
                        double rGradWt = 0.0;
                        for (int n = 0; n <= mNMax; ++n) {
                            rGradWt += tInternalGradRFuseWeight.getAtD(((type-1)*mSizeNP + (k+1)*(mNMax+1) + n)*(mNMax+1) + n);
                        }
                        mGradFuseWeight.add((type - 1) * mFuseSize + k, tScale * rGradWt);
                    }
                }
                break;
            }
            default: {
                throw new IllegalStateException();
            }}
        }
    }
    
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    final void updateParameters_(boolean aCheck) {
        if (mInternalLayerNorm) {
            mInternalLayerNormBeta.fillD(mLayerNormBeta);
            mInternalLayerNormGamma.fillD(mLayerNormGamma);
        }
        if (aCheck) {
            if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE && mRFuseWeight==null) return;
        }
        // 简单总是清空旧值
        mInternalRFuseWeight.fillD(0.0, (mSizeNP*mNumTypes)*(mNMax+1));
        switch(mTypedWType) {
        case WTYPE_RFUSE: {
            assert mRFuseWeight!=null;
            mInternalRFuseWeight.fillD(mRFuseWeight);
            int tSize = (mSizeNP*mNumTypes)*(mNMax+1);
            double tScale = mRFuseScale[0];
            for (int i = 0; i < tSize; ++i) {
                mInternalRFuseWeight.putAtD(i, tScale*mInternalRFuseWeight.getAtD(i));
            }
            break;
        }
        case WTYPE_FUSE: {
            assert mFuseWeight!=null;
            for (int type = 1; type <= mNumTypes; ++type) {
                for (int k = 0; k < mFuseSize; ++k) {
                    double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                    for (int n = 0; n <= mNMax; ++n) {
                        mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + k*(mNMax+1) + n)*(mNMax+1) + n, wt);
                    }
                }
            }
            break;
        }
        case WTYPE_EXFUSE: {
            assert mFuseWeight!=null;
            for (int type = 1; type <= mNumTypes; ++type) {
                for (int n = 0; n <= mNMax; ++n) {
                    mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0); // ex term
                }
                for (int k = 0; k < mFuseSize; ++k) {
                    double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                    for (int n = 0; n <= mNMax; ++n) {
                        mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + (k+1)*(mNMax+1) + n)*(mNMax+1) + n, wt);
                    }
                }
            }
            break;
        }
        case WTYPE_FULL: {
            for (int type = 1; type <= mNumTypes; ++type) {
                for (int n = 0; n <= mNMax; ++n) {
                    mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + (type-1)*(mNMax+1) + n)*(mNMax+1) + n, 1.0);
                }
            }
            break;
        }
        case WTYPE_EXFULL: {
            for (int type = 1; type <= mNumTypes; ++type) {
                for (int n = 0; n <= mNMax; ++n) {
                    mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0); // ex term
                    mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + type*(mNMax+1) + n)*(mNMax+1) + n, 1.0);
                }
            }
            break;
        }
        case WTYPE_NONE: {
            for (int type = 1; type <= mNumTypes; ++type) {
                for (int n = 0; n <= mNMax; ++n) {
                    mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0);
                }
            }
            break;
        }
        case WTYPE_DEFAULT: {
            for (int type = 1; type <= mNumTypes; ++type) {
                double wt = ((type&1)==1) ? type : (-type);
                for (int n = 0; n <= mNMax; ++n) {
                    mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0);
                    mInternalRFuseWeight.putAtD(((type-1)*mSizeNP + (mNMax+1) + n)*(mNMax+1) + n, wt);
                }
            }
            break;
        }
        default: {
            throw new IllegalStateException();
        }}
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    final void save_(Map rSaveTo) {
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
        if (mFuseWeight!=null) {
            rSaveTo.put("fuse_size", mFuseSize);
            rSaveTo.put("fuse_weight", mFuseWeight.asList());
        }
        if (mRFuseWeight!=null) {
            rSaveTo.put("rfuse_size", mRFuseSize);
            rSaveTo.put("rfuse_scale", mRFuseScale[0]);
            rSaveTo.put("rfuse_weight", mRFuseWeight.asList());
        }
        if (mLayerNorm) {
            assert mLayerNormBeta!=null && mLayerNormGamma!=null;
            rSaveTo.put("layer_norm", true);
            rSaveTo.put("layer_norm_beta", mLayerNormBeta.asList());
            rSaveTo.put("layer_norm_gamma", mLayerNormGamma.asList());
        }
    }
    static int getFuseSize(int aWType, int aNumTypes, IVector aFuseWeight) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) {
            return 0;
        } else {
            return aFuseWeight.size()/aNumTypes;
        }
    }
    static int getSizeN_(int aWType, int aNumTypes, int aNMax, int aFuseSize) {
        switch(aWType) {
        case WTYPE_EXFULL: {
            return aNumTypes>1 ? (aNumTypes+1)*(aNMax+1) : (aNMax+1);
        }
        case WTYPE_FULL: case WTYPE_RFUSE: {
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
        if (tOut == null) throw new IllegalArgumentException("Input wtype MUST be in {default, none, full, exfull, fuse, rfuse, exfuse}, input: "+tType);
        return tOut;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getLNBeta_(Map aMap) {
        List tLNBeta = (List)UT.Code.get(aMap, "layer_norm_beta", "ln_beta");
        if (tLNBeta == null) return null;
        return Vectors.from(tLNBeta);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getLNGamma_(Map aMap) {
        List tLNGamma = (List)UT.Code.get(aMap, "layer_norm_gamma", "ln_gamma");
        if (tLNGamma == null) return null;
        return Vectors.from(tLNGamma);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getFuseWeight_(Map aMap, int aWType, int aNumTypes) {
        if (aWType!=WTYPE_FUSE && aWType!=WTYPE_EXFUSE) return null;
        Object tFuseSize = UT.Code.get(aMap, "fuse_size");
        Object tFuseWeight = aMap.get("fuse_weight");
        if (tFuseWeight!=null) {
            if (tFuseSize==null) {
                // 如果没有 fuse_size 则是旧版，按列读取
                RowMatrix tMat = Matrices.fromCols((List<?>)tFuseWeight);
                return tMat.asVecRow();
            }
            // 现在是统一为单个向量，使用第一个值判断即可
            Object tRow = ((List<?>)tFuseWeight).get(0);
            if (tRow instanceof List) {
                // 按行读取
                RowMatrix tMat = Matrices.fromRows((List<?>)tFuseWeight);
                if (tMat.ncols()!=((Number)tFuseSize).intValue()) throw new IllegalArgumentException("Column number of fuse weight mismatch");
                return tMat.asVecRow();
            }
            Vector tVec = Vectors.from((List)tFuseWeight);
            if (tVec.size()!=((Number)tFuseSize).intValue()*aNumTypes) throw new IllegalArgumentException("Size of fuse weight mismatch");
            return tVec;
        }
        if (tFuseSize==null) throw new IllegalArgumentException("Key `fuse_weight` or `fuse_size` required for fuse wtype");
        return Vectors.zeros(aNumTypes*((Number)tFuseSize).intValue());
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getRFuseWeight_(Map aMap, int aWType, int aSizeN) {
        if (aWType != WTYPE_RFUSE) return null;
        if (aMap.containsKey("post_fuse_size") || aMap.containsKey("post_fuse_weight")) {
            throw new IllegalArgumentException("Key `post_fuse_size` or `post_fuse_weight` is invalid for wtype=='rfuse'");
        }
        Object tRFuseSize = aMap.get("rfuse_size");
        Object tRFuseWeight = aMap.get("rfuse_weight");
        if (tRFuseWeight!=null) {
            Vector tVec = Vectors.from((List)tRFuseWeight);
            if (tRFuseSize!=null) {
                if (tVec.size()!=((Number)tRFuseSize).intValue()*aSizeN) throw new IllegalArgumentException("Size of rfuse weight mismatch");
            }
            return tVec;
        }
        if (tRFuseSize==null) {
            throw new IllegalArgumentException("Key `rfuse_weight` or `rfuse_size` required for wtype=='rfuse'");
        }
        int tSize = ((Number)tRFuseSize).intValue()*aSizeN;
        return Vector.zeros(tSize);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    static @Nullable Vector getPostFuseWeight_(Map aMap, int aSizeN) {
        Object tFlag = aMap.get("post_fuse");
        if (tFlag!=null && (!(Boolean)tFlag)) return null;
        Object tPostFuseSize = aMap.get("post_fuse_size");
        Object tPostFuseWeight = aMap.get("post_fuse_weight");
        if (tFlag==null && tPostFuseSize==null && tPostFuseWeight==null) return null;
        if (tPostFuseWeight!=null) {
            Vector tVec = Vectors.from((List)tPostFuseWeight);
            if (tPostFuseSize!=null) {
                if (tVec.size()!=((Number)tPostFuseSize).intValue()*aSizeN) throw new IllegalArgumentException("Size of post fuse weight mismatch");
            }
            return tVec;
        }
        throw new IllegalArgumentException("Key `post_fuse_weight` always required for post_fuse, training with post_fuse is invalid now");
    }
    static Vector postFuse2RFuse_(Vector aPostFuseWeight, int aWType, int aNumTypes, int aNMax, int aSizeN, @Nullable Vector aFuseWeight, int aFuseSize) {
        int tSizeNP = aPostFuseWeight.size() / aSizeN;
        Vector rRFuseWeight = Vectors.zeros(aNumTypes*tSizeNP*(aNMax+1));
        int tTypedWType;
        if (aNumTypes ==1) {
            switch(aWType) {
            case WTYPE_EXFULL: case WTYPE_FULL: case WTYPE_NONE: case WTYPE_DEFAULT: {
                tTypedWType = WTYPE_NONE;
                break;
            }
            default: {
                tTypedWType = aWType;
                break;
            }}
        } else {
            tTypedWType = aWType;
        }
        switch(tTypedWType) {
        case WTYPE_FUSE: {
            assert aFuseWeight!=null;
            for (int type = 1; type <= aNumTypes; ++type) {
                for (int np = 0; np < tSizeNP; ++np) {
                    int tShiftL = ((type-1)*tSizeNP + np)*(aNMax+1);
                    for (int k = 0; k < aFuseSize; ++k) {
                        double wt = aFuseWeight.get((type-1)*aFuseSize + k);
                        int tShiftR = np*aSizeN + k*(aNMax+1);
                        for (int n = 0; n <= aNMax; ++n) {
                            rRFuseWeight.add(tShiftL+n, wt*aPostFuseWeight.get(tShiftR+n));
                        }
                    }
                }
            }
            break;
        }
        case WTYPE_EXFUSE: {
            assert aFuseWeight!=null;
            for (int type = 1; type <= aNumTypes; ++type) {
                for (int np = 0; np < tSizeNP; ++np) {
                    int tShiftL = ((type-1)*tSizeNP + np)*(aNMax+1);
                    int tShiftR = np*aSizeN;
                    // ex term
                    for (int n = 0; n <= aNMax; ++n) {
                        rRFuseWeight.set(tShiftL+n, aPostFuseWeight.get(tShiftR+n));
                    }
                    for (int k = 0; k < aFuseSize; ++k) {
                        double wt = aFuseWeight.get((type-1)*aFuseSize + k);
                        tShiftR = np*aSizeN + (k+1)*(aNMax+1);
                        for (int n = 0; n <= aNMax; ++n) {
                            rRFuseWeight.add(tShiftL+n, wt*aPostFuseWeight.get(tShiftR+n));
                        }
                    }
                }
            }
            break;
        }
        case WTYPE_FULL: {
            for (int type = 1; type <= aNumTypes; ++type) {
                for (int np = 0; np < tSizeNP; ++np) {
                    int tShiftL = ((type-1)*tSizeNP + np)*(aNMax+1);
                    int tShiftR = np*aSizeN + (type-1)*(aNMax+1);
                    for (int n = 0; n <= aNMax; ++n) {
                        rRFuseWeight.set(tShiftL+n, aPostFuseWeight.get(tShiftR+n));
                    }
                }
            }
            break;
        }
        case WTYPE_EXFULL: {
            for (int type = 1; type <= aNumTypes; ++type) {
                for (int np = 0; np < tSizeNP; ++np) {
                    int tShiftL = ((type-1)*tSizeNP + np)*(aNMax+1);
                    int tShiftR = np*aSizeN;
                    // ex term
                    for (int n = 0; n <= aNMax; ++n) {
                        rRFuseWeight.set(tShiftL+n, aPostFuseWeight.get(tShiftR+n));
                    }
                    tShiftR = np*aSizeN + type*(aNMax+1);
                    for (int n = 0; n <= aNMax; ++n) {
                        rRFuseWeight.add(tShiftL+n, aPostFuseWeight.get(tShiftR+n));
                    }
                }
            }
            break;
        }
        case WTYPE_NONE: {
            for (int type = 1; type <= aNumTypes; ++type) {
                for (int np = 0; np < tSizeNP; ++np) {
                    int tShiftL = ((type-1)*tSizeNP + np)*(aNMax+1);
                    int tShiftR = np*aSizeN;
                    for (int n = 0; n <= aNMax; ++n) {
                        rRFuseWeight.set(tShiftL+n, aPostFuseWeight.get(tShiftR+n));
                    }
                }
            }
            break;
        }
        case WTYPE_DEFAULT: {
            for (int type = 1; type <= aNumTypes; ++type) {
                double wt = ((type&1)==1) ? type : (-type);
                for (int np = 0; np < tSizeNP; ++np) {
                    int tShiftL = ((type-1)*tSizeNP + np)*(aNMax+1);
                    int tShiftR = np*aSizeN;
                    for (int n = 0; n <= aNMax; ++n) {
                        rRFuseWeight.set(tShiftL+n, aPostFuseWeight.get(tShiftR+n));
                    }
                    tShiftR = np*aSizeN + (aNMax+1);
                    for (int n = 0; n <= aNMax; ++n) {
                        rRFuseWeight.add(tShiftL+n, wt*aPostFuseWeight.get(tShiftR+n));
                    }
                }
            }
            break;
        }
        default: {
            throw new IllegalStateException();
        }}
        return rRFuseWeight;
    }
    
    
    /** @return {@inheritDoc} */
    @Override public abstract int size();
    /** @return {@inheritDoc} */
    @Override public double rcut() {return mRCut;}
    public int ntypes() {return mNumTypes;}
    
    private void initFuseWeight_() {
        if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE) return;
        assert mFuseWeight != null;
        mFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 权重按照种类归一化，注意只有一种的情况下不专门归一化；经验调整，只是可以加速训练
        if (mNumTypes > 1) {
            int tSize = mFuseWeight.size();
            for (int k = 0; k < mFuseSize; ++k) {
                double tNorm1 = 0.0;
                for (int i = k; i < tSize; i+=mFuseSize) {
                    tNorm1 += Math.abs(mFuseWeight.get(i));
                }
                final double fNorm1 = tNorm1/mNumTypes;
                for (int i = k; i < tSize; i+=mFuseSize) {
                    mFuseWeight.update(i, v -> v / fNorm1);
                }
            }
        }
    }
    private void initRFuseWeight_() {
        // 补充对于 RFuseWeight 的初始化
        if (mRFuseWeight == null) return;
        mRFuseWeight.assign(() -> RANDOM.nextDouble(-1, 1));
        // 确保权重归一化，这个可以有效加速训练；经验设定
        for (int np = 0; np < mSizeNP; ++np) {
            double tNorm = 0.0;
            for (int type = 1; type <= mNumTypes; ++type) {
                int tShift = ((type-1)*mSizeNP + np)*(mNMax+1);
                IVector tSubVec = mRFuseWeight.subVec(tShift, tShift + (mNMax+1));
                tNorm += tSubVec.operation().norm1();
            }
            tNorm /= mSizeN;
            for (int type = 1; type <= mNumTypes; ++type) {
                int tShift = ((type-1)*mSizeNP + np)*(mNMax+1);
                IVector tSubVec = mRFuseWeight.subVec(tShift, tShift + (mNMax+1));
                tSubVec.div2this(tNorm);
            }
        }
        // 在这个经验设定下，scale 设置为此值确保输出的基组值数量级一致
        mRFuseScale[0] = MathEX.Fast.sqrt(1.0 / mSizeN);
    }
    private void initLayerNormParam_() {
        if (!mInternalLayerNorm) return;
        assert mLayerNormBeta!=null && mLayerNormGamma!=null;
        // 简单采用固定值初始化
        mLayerNormBeta.fill(0.0);
        mLayerNormGamma.fill(1.0);
    }
    @Override public void initParameters() {
        initFuseWeight_();
        initRFuseWeight_();
        initLayerNormParam_();
        // 参数更新
        updateParameters_(true);
    }
    
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    @Override public void mountCptrParameter(IDoubleOrFloatCPointer aPtr) {
        mInternalRFuseWeight = aPtr.copy();
        if (mInternalLayerNorm) {
            mInternalLayerNormBeta = mInternalRFuseWeight.plus((mSizeNP*mNumTypes)*(mNMax+1));
            mInternalLayerNormGamma = mInternalLayerNormBeta.plus(mSizeNP);
        }
        updateParameters_(false);
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    @Override public void mountGradCptrParameter(int aThreadID, IDoubleOrFloatCPointer aPtr) {
        if (mInternalGradRFuseWeight ==null) throw new IllegalStateException("invoke `requireGrad(nthreads)` first.");
        mInternalGradRFuseWeight[aThreadID] = aPtr.copy();
        if (mInternalLayerNorm) {
            mInternalGradLayerNormBeta[aThreadID] = mInternalGradRFuseWeight[aThreadID].plus((mSizeNP*mNumTypes)*(mNMax+1));
            mInternalGradLayerNormGamma[aThreadID] = mInternalGradLayerNormBeta[aThreadID].plus(mSizeNP);
        }
    }
    @Override public int cptrParameterSize() {
        int tCParaSize = (mSizeNP*mNumTypes)*(mNMax+1);
        if (mInternalLayerNorm) {
            tCParaSize += (mSizeNP+mSizeNP);
        }
        return tCParaSize;
    }
    
    @Override public void mountParameter(Vector aVec) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aVec.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aVec.size()) throw new IllegalArgumentException("data size mismatch");
        }
        int tShift = 0;
        if (mTypedWType==WTYPE_FUSE || mTypedWType==WTYPE_EXFUSE) {
            assert mFuseWeight != null;
            IVector oFuseWeight = mFuseWeight;
            int tSize = oFuseWeight.size();
            mFuseWeight = aVec.subVec(tShift, tShift+tSize);
            mFuseWeight.fill(oFuseWeight);
            tShift += tSize;
        }
        if (mTypedWType==WTYPE_RFUSE) {
            assert mRFuseWeight != null;
            IVector oPostFuseWeight = mRFuseWeight;
            int tSize = oPostFuseWeight.size();
            mRFuseWeight = aVec.subVec(tShift, tShift+tSize);
            mRFuseWeight.fill(oPostFuseWeight);
            tShift += tSize;
        }
        if (mInternalLayerNorm) {
            assert mLayerNormBeta!=null && mLayerNormGamma!=null;
            IVector oLayerNormBeta = mLayerNormBeta;
            int tSize = oLayerNormBeta.size();
            mLayerNormBeta = aVec.subVec(tShift, tShift+tSize);
            mLayerNormBeta.fill(oLayerNormBeta);
            tShift += tSize;
            IVector oLayerNormGamma = mLayerNormGamma;
            tSize = oLayerNormGamma.size();
            mLayerNormGamma = aVec.subVec(tShift, tShift+tSize);
            mLayerNormGamma.fill(oLayerNormGamma);
        }
    }
    @Override public void mountGradParameter(Vector aVec) {
        if (mInternalGradRFuseWeight ==null) throw new IllegalStateException("invoke `requireGrad(nthreads)` first.");
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aVec.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aVec.size()) throw new IllegalArgumentException("data size mismatch");
        }
        int tShift = 0;
        if (mTypedWType==WTYPE_FUSE || mTypedWType==WTYPE_EXFUSE) {
            assert mFuseWeight != null;
            int tSize = mFuseWeight.size();
            mGradFuseWeight = aVec.subVec(tShift, tShift+tSize);
            tShift += tSize;
        }
        if (mTypedWType==WTYPE_RFUSE) {
            assert mRFuseWeight != null;
            int tSize = mRFuseWeight.size();
            mGradRFuseWeight = aVec.subVec(tShift, tShift+tSize);
            tShift += tSize;
        }
        if (mInternalLayerNorm) {
            assert mLayerNormBeta!=null && mLayerNormGamma!=null;
            int tSize = mLayerNormBeta.size();
            mGradLayerNormBeta = aVec.subVec(tShift, tShift+tSize);
            tShift += tSize;
            tSize = mLayerNormGamma.size();
            mGradLayerNormGamma = aVec.subVec(tShift, tShift+tSize);
        }
    }
    @Override public int parameterSize() {
        int tParaSize = 0;
        if (mTypedWType==WTYPE_FUSE || mTypedWType==WTYPE_EXFUSE) {
            assert mFuseWeight != null;
            tParaSize += mFuseWeight.size();
        }
        if (mTypedWType==WTYPE_RFUSE) {
            assert mRFuseWeight != null;
            tParaSize += mRFuseWeight.size();
        }
        if (mInternalLayerNorm) {
            assert mLayerNormBeta!=null && mLayerNormGamma!=null;
            tParaSize += mLayerNormBeta.size();
            tParaSize += mLayerNormGamma.size();
        }
        return tParaSize;
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge) {
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE", size());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_HPARAM", cptrHyperParameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_PARAM", cptrParameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_WTYPE", mInternalWType);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_NMAX", mNMax);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_NP", mSizeNP);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_LN", mInternalLayerNorm?1:0);
    }
    @Override public boolean hasSameGenMap(MergeableBasis aBasis) {
        if (!(aBasis instanceof WTypeBasis)) return false;
        WTypeBasis tBasis = (WTypeBasis)aBasis;
        return size()==tBasis.size() && cptrHyperParameterSize()==tBasis.cptrHyperParameterSize() && cptrParameterSize()==tBasis.cptrParameterSize() &&
            mNumTypes==tBasis.mNumTypes && (mRFuseWeight!=null)==(tBasis.mRFuseWeight!=null) && mWType==tBasis.mWType && mNMax==tBasis.mNMax && mSizeNP==tBasis.mSizeNP && mInternalLayerNorm==tBasis.mInternalLayerNorm;
    }
}
