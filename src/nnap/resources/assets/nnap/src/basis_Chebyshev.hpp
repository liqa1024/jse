#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <jint NMAX>
static inline void mplusFp(jdouble *rFp, jdouble aFc, jdouble *aRn) noexcept {
    for (jint n = 0; n <= NMAX; ++n) {
        rFp[n] += aFc*aRn[n];
    }
}
template <jint NMAX>
static inline void mplusFpWt(jdouble *rFp, jdouble *rFpWt, jdouble aFc, jdouble *aRn, jdouble aWt) noexcept {
    const jdouble tFcWt = aFc*aWt;
    for (jint n = 0; n <= NMAX; ++n) {
        const jdouble tRnn = aRn[n];
        rFp[n] += aFc*tRnn;
        rFpWt[n] += tFcWt*tRnn;
    }
}

template <jint NMAX, jboolean WT>
static inline void mplusGradNNGrad_(jdouble *rGradNNGrad, jdouble *rGradNNGradWt, jdouble aFc, jdouble *aRn, jdouble aWt,
                                    jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                                    jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    // cal gradNNgradFc
    const jdouble tGradNNGradFc = aFcPx*aGradFx + aFcPy*aGradFy + aFcPz*aGradFz;
    // cal gradNNgrad
    for (jint n = 0; n <= NMAX; ++n) {
        jdouble tGradNNGradRn = aRnPx[n]*aGradFx + aRnPy[n]*aGradFy + aRnPz[n]*aGradFz;
        tGradNNGradRn = aRn[n]*tGradNNGradFc + aFc*tGradNNGradRn;
        rGradNNGrad[n] += tGradNNGradRn;
        if (WT) rGradNNGradWt[n] += aWt*tGradNNGradRn;
    }
}
template <jint NMAX>
static inline void mplusGradNNGrad(jdouble *rGradNNGrad, jdouble aFc, jdouble *aRn, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    mplusGradNNGrad_<NMAX, JNI_FALSE>(rGradNNGrad, NULL, aFc, aRn, 0, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}
template <jint NMAX>
static inline void mplusGradNNGradWt(jdouble *rGradNNGrad, jdouble *rGradNNGradWt, jdouble aFc, jdouble *aRn, jdouble aWt, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) {
    mplusGradNNGrad_<NMAX, JNI_TRUE>(rGradNNGrad, rGradNNGradWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}

template <jint NMAX, jboolean WT>
static inline void gradRn2Fxyz_(jint j, jdouble *aGradRn, jdouble *aGradRnWt, jdouble aFc, jdouble *aRn, jdouble aWt,
                                jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                                jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                                jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    jdouble tGradFc = 0.0;
    jdouble rFxj = 0.0, rFyj = 0.0, rFzj = 0.0;
    for (jint n = 0; n <= NMAX; ++n) {
        jdouble tGradRnn = aGradRn[n];
        if (WT) tGradRnn += aWt*aGradRnWt[n];
        const jdouble tRnn = aRn[n];
        tGradFc += tRnn * tGradRnn;
        tGradRnn *= aFc;
        rFxj += tGradRnn*aRnPx[n];
        rFyj += tGradRnn*aRnPy[n];
        rFzj += tGradRnn*aRnPz[n];
    }
    rFxj += aFcPx*tGradFc;
    rFyj += aFcPy*tGradFc;
    rFzj += aFcPz*tGradFc;
    rFx[j] += rFxj; rFy[j] += rFyj; rFz[j] += rFzj;
}
template <jint NMAX>
static inline void gradRn2Fxyz(jint j, jdouble *aGradRn, jdouble aFc, jdouble *aRn, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    gradRn2Fxyz_<NMAX, JNI_FALSE>(j, aGradRn, NULL, aFc, aRn, 0, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz);
}
template <jint NMAX>
static inline void gradRnWt2Fxyz(jint j, jdouble *aGradRn, jdouble *aGradRnWt, jdouble aFc, jdouble *aRn, jdouble aWt, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    gradRn2Fxyz_<NMAX, JNI_TRUE>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rFx, rFy, rFz);
}

template <jint NMAX, jint WTYPE, jboolean FULL_CACHE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                  jdouble *rFp, jdouble *rForwardCache,
                  jint aTypeNum, jdouble aRCut, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // const init
    jint tSizeFp;
    switch(WTYPE) {
    case WTYPE_EXFULL:  {tSizeFp = (aTypeNum+1)*(NMAX+1); break;}
    case WTYPE_FULL:    {tSizeFp = aTypeNum*(NMAX+1);     break;}
    case WTYPE_NONE:    {tSizeFp = NMAX+1;                break;}
    case WTYPE_DEFAULT: {tSizeFp = (NMAX+NMAX+2);         break;}
    case WTYPE_FUSE:    {tSizeFp = aFuseSize*(NMAX+1);    break;}
    default:            {tSizeFp = 0;                     break;}
    }
    // init cache
    jdouble *rRn = NULL;
    jdouble *rNlRn = NULL, *rNlFc = NULL;
    if (FULL_CACHE) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNN*(NMAX+1);
    } else {
        rRn = rForwardCache;
    }
    // clear fp first
    for (jint i = 0; i < tSizeFp; ++i) {
        rFp[i] = 0.0;
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        jdouble fc = calFc(dis, aRCut);
        if (FULL_CACHE) rNlFc[j] = fc;
        // cal Rn
        if (FULL_CACHE) rRn = rNlRn + j*(NMAX+1);
        calRn<NMAX>(rRn, dis, aRCut);
        // cal fp
        if (WTYPE==WTYPE_FUSE) {
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tFp = rFp;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble fcWt = fc * tFuseWeight[type-1];
                mplusFp<NMAX>(tFp, fcWt, rRn);
                tFp += (NMAX+1);
                tFuseWeight += aTypeNum;
            }
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusFp<NMAX>(rFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tFp = rFp + (NMAX+1)*(type-1);
            mplusFp<NMAX>(tFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tFpWt = rFp + (NMAX+1)*type;
            mplusFpWt<NMAX>(rFp, tFpWt, fc, rRn, 1.0);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tFpWt = rFp + (NMAX+1);
            mplusFpWt<NMAX>(rFp, tFpWt, fc, rRn, wt);
        }
    }
}

template <jint NMAX, jint WTYPE>
static void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                        jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache,
                        jint aTypeNum, jdouble aRCut, jint aFuseSize) noexcept {
    static_assert(WTYPE!=WTYPE_DEFAULT && WTYPE!=WTYPE_NONE && WTYPE!=WTYPE_FULL && WTYPE!=WTYPE_EXFULL, "WTYPE INVALID");
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(NMAX+1);
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(NMAX+1);
        // plus to para
        if (WTYPE==WTYPE_FUSE) {
            jdouble *tGradFp = aGradFp;
            jdouble *tGradPara = rGradPara;
            for (jint k = 0; k < aFuseSize; ++k) {
                tGradPara[type-1] += fc * dot<NMAX+1>(tRn, tGradFp);
                tGradFp += (NMAX+1);
                tGradPara += aTypeNum;
            }
            continue;
        }
    }
}

template <jint NMAX, jint WTYPE, jboolean FULL_CACHE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache,
                     jint aTypeNum, jdouble aRCut, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(NMAX+1);
    jdouble *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    jdouble *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    jdouble *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    if (FULL_CACHE) {
        rNlRnPx = rForwardForceCache;
        rNlRnPy = rNlRnPx + aNN*(NMAX+1);
        rNlRnPz = rNlRnPy + aNN*(NMAX+1);
        rNlFcPx = rNlRnPz + aNN*(NMAX+1);
        rNlFcPy = rNlFcPx + aNN;
        rNlFcPz = rNlFcPy + aNN;
        rCheby2 = rNlFcPz + aNN;
    } else {
        rRnPx = rForwardForceCache;
        rRnPy = rRnPx + (NMAX+1);
        rRnPz = rRnPy + (NMAX+1);
        rCheby2 = rRnPz + (NMAX+1);
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(NMAX+1);
        // cal fcPxyz
        jdouble fcPx, fcPy, fcPz;
        calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        if (FULL_CACHE) {
            rNlFcPx[j] = fcPx;
            rNlFcPy[j] = fcPy;
            rNlFcPz[j] = fcPz;
        }
        // cal RnPxyz
        if (FULL_CACHE) {
            rRnPx = rNlRnPx + j*(NMAX+1);
            rRnPy = rNlRnPy + j*(NMAX+1);
            rRnPz = rNlRnPz + j*(NMAX+1);
        }
        calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, dx, dy, dz);
        if (WTYPE==WTYPE_FUSE) {
            jdouble *tGradRn = rCheby2;
            for (jint n = 0; n <= NMAX; ++n) {
                tGradRn[n] = 0.0;
            }
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tNNGrad = aNNGrad;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble wt = tFuseWeight[type-1];
                mplusFp<NMAX>(tGradRn, wt, tNNGrad);
                tNNGrad += (NMAX+1);
                tFuseWeight += aTypeNum;
            }
            gradRn2Fxyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradRn2Fxyz<NMAX>(j, aNNGrad, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradRn = aNNGrad + (NMAX+1)*(type-1);
            gradRn2Fxyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tNNGradWt = aNNGrad + (NMAX+1)*type;
            gradRnWt2Fxyz<NMAX>(j, aNNGrad, tNNGradWt, fc, tRn, 1.0, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tNNGradWt = aNNGrad + (NMAX+1);
            gradRnWt2Fxyz<NMAX>(j, aNNGrad, tNNGradWt, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        }
    }
}

template <jint NMAX, jint WTYPE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(NMAX+1);
    jdouble *tNlRnPx = aForwardForceCache;
    jdouble *tNlRnPy = tNlRnPx + aNN*(NMAX+1);
    jdouble *tNlRnPz = tNlRnPy + aNN*(NMAX+1);
    jdouble *tNlFcPx = tNlRnPz + aNN*(NMAX+1);
    jdouble *tNlFcPy = tNlFcPx + aNN;
    jdouble *tNlFcPz = tNlFcPy + aNN;
    jdouble *rGradNNGradRn = NULL;
    if (WTYPE==WTYPE_FUSE) {
        rGradNNGradRn = rBackwardForceCache;
    }
    // loop for neighbor
    for (jint j = 0; j < aNN; ++j) {
        // init nl
        jint type = aNlType[j];
        jdouble dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        jdouble dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get gradFxyz
        jdouble tGradFx = aGradFx[j], tGradFy = aGradFy[j], tGradFz = aGradFz[j];
        // get fc Rn
        jdouble fc = tNlFc[j];
        jdouble *tRn = tNlRn + j*(NMAX+1);
        // get fcPxyz RnPxyz
        jdouble fcPx = tNlFcPx[j], fcPy = tNlFcPy[j], fcPz = tNlFcPz[j];
        jdouble *tRnPx = tNlRnPx + j*(NMAX+1);
        jdouble *tRnPy = tNlRnPy + j*(NMAX+1);
        jdouble *tRnPz = tNlRnPz + j*(NMAX+1);
        // mplus to gradNNgrad
        if (WTYPE==WTYPE_FUSE) {
            jdouble tGradNNGradFc = fcPx*tGradFx + fcPy*tGradFy + fcPz*tGradFz;
            for (jint n = 0; n <= NMAX; ++n) {
                jdouble tGradNNGradRn_ = tRnPx[n]*tGradFx + tRnPy[n]*tGradFy + tRnPz[n]*tGradFz;
                rGradNNGradRn[n] = tRn[n]*tGradNNGradFc + fc*tGradNNGradRn_;
            }
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tGradNNGrad = rGradNNGrad;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble wt = tFuseWeight[type-1];
                mplusFp<NMAX>(tGradNNGrad, wt, rGradNNGradRn);
                tGradNNGrad += (NMAX+1);
                tFuseWeight += aTypeNum;
            }
            if (!aFixBasis) {
                jdouble *tGradPara = rGradPara;
                jdouble *tNNGrad = aNNGrad;
                for (jint k = 0; k < aFuseSize; ++k) {
                    tGradPara[type-1] += dot<NMAX+1>(tNNGrad, rGradNNGradRn);
                    tNNGrad += (NMAX+1);
                    tGradPara += aTypeNum;
                }
            }
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusGradNNGrad<NMAX>(rGradNNGrad, fc, tRn, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_FULL) {
            jdouble *tGradNNGrad = rGradNNGrad + (NMAX+1)*(type-1);
            mplusGradNNGrad<NMAX>(tGradNNGrad, fc, tRn, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            jdouble *tGradNNGradWt = rGradNNGrad + (NMAX+1)*type;
            mplusGradNNGradWt<NMAX>(rGradNNGrad, tGradNNGradWt, fc, tRn, 1.0, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tGradNNGradWt = rGradNNGrad + (NMAX+1);
            mplusGradNNGradWt<NMAX>(rGradNNGrad, tGradNNGradWt, fc, tRn, wt, fcPx, fcPy, fcPz, tRnPx, tRnPy, tRnPz, tGradFx, tGradFy, tGradFz);
        }
    }
}


template <jint NMAX, jint WTYPE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                         jint aTypeNum, jdouble aRCut, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aFullCache) {
        calFp<NMAX, WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
    } else {
        calFp<NMAX, WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
    }
}
template <jint WTYPE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                         jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch (aNMax) {
    case 0: {calFp<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 1: {calFp<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 2: {calFp<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 3: {calFp<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 4: {calFp<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 5: {calFp<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 6: {calFp<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 7: {calFp<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 8: {calFp<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 9: {calFp<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 10: {calFp<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 11: {calFp<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 12: {calFp<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 13: {calFp<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 14: {calFp<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 15: {calFp<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 16: {calFp<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 17: {calFp<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 18: {calFp<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 19: {calFp<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 20: {calFp<20, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    default: {return;}
    }
}
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                         jdouble *rFp, jdouble *rForwardCache, jboolean aFullCache,
                         jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_NONE: {
            calFp<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_EXFULL: {
            calFp<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calFp<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calFp<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            calFp<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    }
}


template <jint WTYPE>
static inline void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                               jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache,
                               jint aTypeNum, jdouble aRCut, jint aNMax, jint aFuseSize) noexcept {
    switch (aNMax) {
    case 0: {calBackward<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 1: {calBackward<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 2: {calBackward<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 3: {calBackward<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 4: {calBackward<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 5: {calBackward<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 6: {calBackward<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 7: {calBackward<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 8: {calBackward<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 9: {calBackward<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 10: {calBackward<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 11: {calBackward<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 12: {calBackward<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 13: {calBackward<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 14: {calBackward<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 15: {calBackward<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 16: {calBackward<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 17: {calBackward<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 18: {calBackward<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 19: {calBackward<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    case 20: {calBackward<20, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize); return;}
    default: {return;}
    }
}
static inline void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                               jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache,
                               jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseSize) noexcept {
    if (aWType==WTYPE_FUSE) {
        calBackward<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aNMax, aFuseSize);
    }
}


template <jint NMAX, jint WTYPE>
static inline void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                            jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                            jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                            jint aTypeNum, jdouble aRCut, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aFullCache) {
        calForce<NMAX, WTYPE, JNI_TRUE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
    } else {
        calForce<NMAX, WTYPE, JNI_FALSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
    }
}
template <jint WTYPE>
static inline void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                            jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                            jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                            jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch (aNMax) {
    case 0: {calForce<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 1: {calForce<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 2: {calForce<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 3: {calForce<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 4: {calForce<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 5: {calForce<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 6: {calForce<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 7: {calForce<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 8: {calForce<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 9: {calForce<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 10: {calForce<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 11: {calForce<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 12: {calForce<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 13: {calForce<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 14: {calForce<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 15: {calForce<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 16: {calForce<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 17: {calForce<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 18: {calForce<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 19: {calForce<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 20: {calForce<20, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    default: {return;}
    }
}
static inline void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                            jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                            jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                            jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_NONE: {
            calForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_EXFULL: {
            calForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            calForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    }
}


template <jint WTYPE>
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch (aNMax) {
    case 0: {calBackwardForce<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 1: {calBackwardForce<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 2: {calBackwardForce<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 3: {calBackwardForce<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 4: {calBackwardForce<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 5: {calBackwardForce<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 6: {calBackwardForce<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 7: {calBackwardForce<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 8: {calBackwardForce<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 9: {calBackwardForce<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 10: {calBackwardForce<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 11: {calBackwardForce<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 12: {calBackwardForce<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 13: {calBackwardForce<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 14: {calBackwardForce<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 15: {calBackwardForce<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 16: {calBackwardForce<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 17: {calBackwardForce<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 18: {calBackwardForce<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 19: {calBackwardForce<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    case 20: {calBackwardForce<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aFuseWeight, aFuseSize); return;}
    default: {return;}
    }
}
static void calBackwardForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                             jdouble *aNNGrad, jdouble *aGradFx, jdouble *aGradFy, jdouble *aGradFz,
                             jdouble *rGradNNGrad, jdouble *rGradPara,
                             jdouble *aForwardCache, jdouble *aForwardForceCache,
                             jdouble *rBackwardForceCache, jboolean aFixBasis,
                             jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    if (aTypeNum == 1) {
        switch(aWType) {
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE:
        case WTYPE_FULL:
        case WTYPE_EXFULL:
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    } else {
        switch(aWType) {
        case WTYPE_EXFULL: {
            calBackwardForce<WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FULL: {
            calBackwardForce<WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_NONE: {
            calBackwardForce<WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_DEFAULT: {
            calBackwardForce<WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        case WTYPE_FUSE: {
            calBackwardForce<WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardForceCache, aFixBasis, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
            return;
        }
        default: {
            return;
        }}
    }
}

}

#endif //BASIS_CHEBYSHEV_H