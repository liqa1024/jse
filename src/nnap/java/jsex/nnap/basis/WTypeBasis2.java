package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.Conf;
import jse.code.UT;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
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
    
    @Nullable DoubleArrayVector mFuseWeight;
    @Nullable DoubleArrayVector mGradFuseWeight = null;
    final int mSizeN;
    final int mFuseSize;
    
    @Nullable DoubleArrayVector mPostFuseWeight;
    @Nullable DoubleArrayVector mGradPostFuseWeight = null;
    final int mPostFuseSize;
    final double[] mPostFuseScale;
    
    final int mSizeNP;
    IDoubleOrFloatCPointer mRFuseWeight = null;
    IDoubleOrFloatCPointer mGradRFuseWeight = null;
    
    WTypeBasis2(double aRCut, int aNumTypes, int aNMax, int aWType, @Nullable Vector aFuseWeight, @Nullable Vector aPostFuseWeight, double @Nullable[] aPostFuseScale) {
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
        mFuseSize = getFuseSize(mWType, mNumTypes, mFuseWeight);
        if (mFuseWeight!=null) {
            if (mFuseWeight.size()!=mNumTypes*mFuseSize) throw new IllegalArgumentException("Size of fuse weight mismatch");
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
    }
    
    @Override public void updateParameters() {
        updateParameters_(true);
    }
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    @Override public void backwardParameter() {
        if (mGradRFuseWeight==null) throw new IllegalStateException();
        if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE && mPostFuseWeight==null) return;
        double tScale = mPostFuseScale[0];
        if (mPostFuseWeight==null) {
            switch(mTypedWType) {
            case WTYPE_FUSE: {
                assert mGradFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int k = 0; k < mFuseSize; ++k) {
                        double rGradWt = 0.0;
                        for (int n = 0; n <= mNMax; ++n) {
                            rGradWt += mGradRFuseWeight.getAtD(((type-1)*mSizeNP + k*(mNMax+1) + n)*(mNMax+1) + n);
                        }
                        mGradFuseWeight.add((type-1)*mFuseSize + k, tScale*rGradWt);
                    }
                }
                break;
            }
            case WTYPE_EXFUSE: {
                assert mGradFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int k = 0; k < mFuseSize; ++k) {
                        double rGradWt = 0.0;
                        for (int n = 0; n <= mNMax; ++n) {
                            rGradWt += mGradRFuseWeight.getAtD(((type-1)*mSizeNP + (k+1)*(mNMax+1) + n)*(mNMax+1) + n);
                        }
                        mGradFuseWeight.add((type-1)*mFuseSize + k, tScale*rGradWt);
                    }
                }
                break;
            }}
        } else {
            switch(mTypedWType) {
            case WTYPE_FUSE: {
                assert mFuseWeight!=null && mGradFuseWeight!=null && mGradPostFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        for (int k = 0; k < mFuseSize; ++k) {
                            double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                            int tShiftR = np*mSizeN + k*(mNMax+1);
                            double rGradWt = 0.0;
                            for (int n = 0; n <= mNMax; ++n) {
                                double tSubGradRFW = mGradRFuseWeight.getAtD(tShiftL+n);
                                rGradWt += tSubGradRFW*mPostFuseWeight.get(tShiftR+n);
                                mGradPostFuseWeight.add(tShiftR+n, tScale*tSubGradRFW*wt);
                            }
                            mGradFuseWeight.add((type-1)*mFuseSize + k, tScale*rGradWt);
                        }
                    }
                }
                break;
            }
            case WTYPE_EXFUSE: {
                assert mFuseWeight!=null && mGradFuseWeight!=null && mGradPostFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        // ex term
                        for (int n = 0; n <= mNMax; ++n) {
                            mGradPostFuseWeight.add(tShiftR+n, tScale*mGradRFuseWeight.getAtD(tShiftL+n));
                        }
                        for (int k = 0; k < mFuseSize; ++k) {
                            double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                            tShiftR = np*mSizeN + (k+1)*(mNMax+1);
                            double rGradWt = 0.0;
                            for (int n = 0; n <= mNMax; ++n) {
                                double tSubGradRFW = mGradRFuseWeight.getAtD(tShiftL+n);
                                rGradWt += tSubGradRFW*mPostFuseWeight.get(tShiftR+n);
                                mGradPostFuseWeight.add(tShiftR+n, tScale*tSubGradRFW*wt);
                            }
                            mGradFuseWeight.add((type-1)*mFuseSize + k, tScale*rGradWt);
                        }
                    }
                }
                break;
            }
            case WTYPE_FULL: {
                assert mGradPostFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN + (type-1)*(mNMax+1);
                        for (int n = 0; n <= mNMax; ++n) {
                            mGradPostFuseWeight.add(tShiftR+n, tScale*mGradRFuseWeight.getAtD(tShiftL+n));
                        }
                    }
                }
                break;
            }
            case WTYPE_EXFULL: {
                assert mGradPostFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        // ex term
                        for (int n = 0; n <= mNMax; ++n) {
                            mGradPostFuseWeight.add(tShiftR+n, tScale*mGradRFuseWeight.getAtD(tShiftL+n));
                        }
                        tShiftR = np*mSizeN + type*(mNMax+1);
                        for (int n = 0; n <= mNMax; ++n) {
                            mGradPostFuseWeight.add(tShiftR+n, tScale*mGradRFuseWeight.getAtD(tShiftL+n));
                        }
                    }
                }
                break;
            }
            case WTYPE_NONE: {
                assert mGradPostFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        for (int n = 0; n <= mNMax; ++n) {
                            mGradPostFuseWeight.add(tShiftR+n, tScale*mGradRFuseWeight.getAtD(tShiftL+n));
                        }
                    }
                }
                break;
            }
            case WTYPE_DEFAULT: {
                assert mGradPostFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    double wt = ((type&1)==1) ? type : (-type);
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        for (int n = 0; n <= mNMax; ++n) {
                            mGradPostFuseWeight.add(tShiftR+n, tScale*mGradRFuseWeight.getAtD(tShiftL+n));
                        }
                        tShiftR = np*mSizeN + (mNMax+1);
                        for (int n = 0; n <= mNMax; ++n) {
                            mGradPostFuseWeight.add(tShiftR+n, tScale*wt*mGradRFuseWeight.getAtD(tShiftL+n));
                        }
                    }
                }
                break;
            }}
        }
    }
    
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    final void updateParameters_(boolean aCheck) {
        if (aCheck) {
            if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE && mPostFuseWeight==null) return;
        }
        // 简单总是清空旧值
        mRFuseWeight.fillD(0.0, cptrParameterSize());
        if (mPostFuseWeight==null) {
            switch(mTypedWType) {
            case WTYPE_FUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int k = 0; k < mFuseSize; ++k) {
                        double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(((type-1)*mSizeNP + k*(mNMax+1) + n)*(mNMax+1) + n, wt);
                        }
                    }
                }
                break;
            }
            case WTYPE_EXFUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0); // ex term
                    }
                    for (int k = 0; k < mFuseSize; ++k) {
                        double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(((type-1)*mSizeNP + (k+1)*(mNMax+1) + n)*(mNMax+1) + n, wt);
                        }
                    }
                }
                break;
            }
            case WTYPE_FULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.putAtD(((type-1)*mSizeNP + (type-1)*(mNMax+1) + n)*(mNMax+1) + n, 1.0);
                    }
                }
                break;
            }
            case WTYPE_EXFULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0); // ex term
                        mRFuseWeight.putAtD(((type-1)*mSizeNP + type*(mNMax+1) + n)*(mNMax+1) + n, 1.0);
                    }
                }
                break;
            }
            case WTYPE_NONE: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0);
                    }
                }
                break;
            }
            case WTYPE_DEFAULT: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    double wt = ((type&1)==1) ? type : (-type);
                    for (int n = 0; n <= mNMax; ++n) {
                        mRFuseWeight.putAtD(((type-1)*mSizeNP + n)*(mNMax+1) + n, 1.0);
                        mRFuseWeight.putAtD(((type-1)*mSizeNP + (mNMax+1) + n)*(mNMax+1) + n, wt);
                    }
                }
                break;
            }}
        } else {
            switch(mTypedWType) {
            case WTYPE_FUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        for (int k = 0; k < mFuseSize; ++k) {
                            double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                            int tShiftR = np*mSizeN + k*(mNMax+1);
                            for (int n = 0; n <= mNMax; ++n) {
                                mRFuseWeight.putAtD(tShiftL+n, mRFuseWeight.getAtD(tShiftL+n) + wt*mPostFuseWeight.get(tShiftR+n));
                            }
                        }
                    }
                }
                break;
            }
            case WTYPE_EXFUSE: {
                assert mFuseWeight!=null;
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        // ex term
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(tShiftL+n, mPostFuseWeight.get(tShiftR+n));
                        }
                        for (int k = 0; k < mFuseSize; ++k) {
                            double wt = mFuseWeight.get((type-1)*mFuseSize + k);
                            tShiftR = np*mSizeN + (k+1)*(mNMax+1);
                            for (int n = 0; n <= mNMax; ++n) {
                                mRFuseWeight.putAtD(tShiftL+n, mRFuseWeight.getAtD(tShiftL+n) + wt*mPostFuseWeight.get(tShiftR+n));
                            }
                        }
                    }
                }
                break;
            }
            case WTYPE_FULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN + (type-1)*(mNMax+1);
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(tShiftL+n, mPostFuseWeight.get(tShiftR+n));
                        }
                    }
                }
                break;
            }
            case WTYPE_EXFULL: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        // ex term
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(tShiftL+n, mPostFuseWeight.get(tShiftR+n));
                        }
                        tShiftR = np*mSizeN + type*(mNMax+1);
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(tShiftL+n, mRFuseWeight.getAtD(tShiftL+n) + mPostFuseWeight.get(tShiftR+n));
                        }
                    }
                }
                break;
            }
            case WTYPE_NONE: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(tShiftL+n, mPostFuseWeight.get(tShiftR+n));
                        }
                    }
                }
                break;
            }
            case WTYPE_DEFAULT: {
                for (int type = 1; type <= mNumTypes; ++type) {
                    double wt = ((type&1)==1) ? type : (-type);
                    for (int np = 0; np < mSizeNP; ++np) {
                        int tShiftL = ((type-1)*mSizeNP + np)*(mNMax+1);
                        int tShiftR = np*mSizeN;
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(tShiftL+n, mPostFuseWeight.get(tShiftR+n));
                        }
                        tShiftR = np*mSizeN + (mNMax+1);
                        for (int n = 0; n <= mNMax; ++n) {
                            mRFuseWeight.putAtD(tShiftL+n, mRFuseWeight.getAtD(tShiftL+n) + wt*mPostFuseWeight.get(tShiftR+n));
                        }
                    }
                }
                break;
            }}
            int tSize = cptrParameterSize();
            double tScale = mPostFuseScale[0];
            for (int i = 0; i < tSize; ++i) {
                mRFuseWeight.putAtD(i, tScale*mRFuseWeight.getAtD(i));
            }
        }
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
        rSaveTo.put("post_fuse", mPostFuseWeight!=null);
        if (mPostFuseWeight!=null) {
            rSaveTo.put("post_fuse_size", mPostFuseSize);
            rSaveTo.put("post_fuse_scale", mPostFuseScale[0]);
            rSaveTo.put("post_fuse_weight", mPostFuseWeight.asList());
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
        updateParameters_(true);
    }
    
    @Override public void mountCptrParameter(IDoubleOrFloatCPointer aPtr) {
        mRFuseWeight = aPtr.copy();
        updateParameters_(false);
    }
    @Override public void mountGradCptrParameter(IDoubleOrFloatCPointer aPtr) {
        mGradRFuseWeight = aPtr.copy();
    }
    @Override public int cptrParameterSize() {
        return (mSizeNP*mNumTypes) * (mNMax+1);
    }
    
    @Override public void mountParameter(IDataShell<double[]> aData) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        }
        double[] tData = aData.internalData();
        int tShift = aData.internalDataShift();
        if (mTypedWType==WTYPE_FUSE || mTypedWType==WTYPE_EXFUSE) {
            assert mFuseWeight != null;
            IVector oFuseWeight = mFuseWeight;
            int tSize = oFuseWeight.size();
            mFuseWeight = new ShiftVector(tSize, tShift, tData);
            tShift += tSize;
            mFuseWeight.fill(oFuseWeight);
        }
        if (mPostFuseWeight==null) return;
        IVector oPostFuseWeight = mPostFuseWeight;
        int tSize = oPostFuseWeight.size();
        mPostFuseWeight = new ShiftVector(tSize, tShift, tData);
        mPostFuseWeight.fill(oPostFuseWeight);
    }
    @Override public void mountGradParameter(IDataShell<double[]> aData) {
        if (Conf.OPERATION_CHECK) {
            if (parameterSize() != aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (parameterSize() > aData.internalDataSize()) throw new IllegalArgumentException("data size mismatch");
        }
        double[] tData = aData.internalData();
        int tShift = aData.internalDataShift();
        if (mTypedWType==WTYPE_FUSE || mTypedWType==WTYPE_EXFUSE) {
            assert mFuseWeight != null;
            int tSize = mFuseWeight.size();
            mGradFuseWeight = new ShiftVector(tSize, tShift, tData);
            tShift += tSize;
        }
        if (mPostFuseWeight==null) return;
        int tSize = mPostFuseWeight.size();
        mGradPostFuseWeight = new ShiftVector(tSize, tShift, tData);
    }
    @Override public int parameterSize() {
        final int tParaSize;
        if (mTypedWType!=WTYPE_FUSE && mTypedWType!=WTYPE_EXFUSE) {
            tParaSize = 0;
        } else {
            assert mFuseWeight != null;
            tParaSize = mFuseWeight.size();
        }
        if (mPostFuseWeight==null) return tParaSize;
        return tParaSize + mPostFuseWeight.size();
    }
    
    @Override public void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge) {
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE", size());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_HPARAM", cptrHyperParameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_PARAM", cptrParameterSize());
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_WTYPE", mInternalWType);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_NMAX", mNMax);
        rGenMap.put(aGenIdxType+":"+aGenIdxMerge+":NNAPGEN_FP_SIZE_NP", mSizeNP);
    }
    @Override public boolean hasSameGenMap(MergeableBasis2 aBasis) {
        if (!(aBasis instanceof WTypeBasis2)) return false;
        WTypeBasis2 tBasis = (WTypeBasis2)aBasis;
        return size()==tBasis.size() && cptrHyperParameterSize()==tBasis.cptrHyperParameterSize() && cptrParameterSize()==tBasis.cptrParameterSize() &&
            mNumTypes==tBasis.mNumTypes && (mPostFuseWeight!=null)==(tBasis.mPostFuseWeight!=null) && mWType==tBasis.mWType && mNMax==tBasis.mNMax && mSizeNP==tBasis.mSizeNP;
    }
}
