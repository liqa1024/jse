#include "jsex_nnap_basis_Chebyshev.h"
#include "nnap_util.hpp"

template <jint NMAX>
static inline void mplusFp(jdouble *rFp, jdouble aFc, jdouble *aRn) noexcept {
    for (jint n = 0; n <= NMAX; ++n) {
        rFp[n] += aFc*aRn[n];
    }
}
template <jint NMAX>
static inline void mplusFpWt(jdouble *rFp, jdouble *rFpWt, jdouble aFc, jdouble *aRn, jdouble aWt) noexcept {
    jdouble tFcWt = aFc*aWt;
    for (jint n = 0; n <= NMAX; ++n) {
        jdouble tRnn = aRn[n];
        rFp[n] += aFc*tRnn;
        rFpWt[n] += tFcWt*tRnn;
    }
}

template <jint NMAX>
static inline void gradRn2Fxyz(jint j, jdouble *aGradRn, jdouble aFc, jdouble *aRn,
                               jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                               jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                               jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    jdouble tGradFc = 0.0;
    jdouble rFxj = 0.0, rFyj = 0.0, rFzj = 0.0;
    for (jint n = 0; n <= NMAX; ++n) {
        jdouble tGradRnn = aGradRn[n];
        jdouble tRnn = aRn[n];
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
static inline void gradRnWt2Fxyz(jint j, jdouble *aGradRn, jdouble *aGradRnWt, jdouble aFc, jdouble *aRn, jdouble aWt,
                                 jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                                 jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                                 jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    jdouble tGradFc = 0.0;
    jdouble rFxj = 0.0, rFyj = 0.0, rFzj = 0.0;
    for (jint n = 0; n <= NMAX; ++n) {
        jdouble tGradRnn = aGradRn[n] + aWt*aGradRnWt[n];
        jdouble tRnn = aRn[n];
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

template <jint NMAX, jint WTYPE>
static void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN, jdouble *rFp,
                  jdouble *rForwardCache, jboolean aFullCache,
                  jint aTypeNum, jdouble aRCut, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // const init
    jint tSizeFp;
    switch(WTYPE) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        tSizeFp = (aTypeNum==1) ? (NMAX+1) : (aTypeNum+1)*(NMAX+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        tSizeFp = aTypeNum*(NMAX+1);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE: {
        tSizeFp = NMAX+1;
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        tSizeFp = (aTypeNum==1) ? (NMAX+1) : (NMAX+NMAX+2);
        break;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FUSE: {
        tSizeFp = aFuseSize*(NMAX+1);
        break;
    }
    default: {
        tSizeFp = 0;
        break;
    }}
    // init cache
    jdouble *rRn = NULL;
    jdouble *rNlRn = NULL, *rNlFc = NULL;
    if (aFullCache) {
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
        jdouble fc = JSE_NNAP::calFc(dis, aRCut);
        if (aFullCache) rNlFc[j] = fc;
        // cal Rn
        if (aFullCache) rRn = rNlRn + j*(NMAX+1);
        JSE_NNAP::calRn<NMAX>(rRn, dis, aRCut);
        // cal fp
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FUSE) {
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tFp = rFp;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble fcWt = fc * tFuseWeight[type-1];
                mplusFp<NMAX>(tFp, fcWt, rRn);
                tFp += (NMAX+1);
                tFuseWeight += aTypeNum;
            }
        } else
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE || aTypeNum==1) {
            mplusFp<NMAX>(rFp, fc, rRn);
        } else
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
            jdouble *tFp = rFp + (NMAX+1)*(type-1);
            mplusFp<NMAX>(tFp, fc, rRn);
        } else
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_EXFULL) {
            jdouble *tFpWt = rFp + (NMAX+1)*type;
            mplusFpWt<NMAX>(rFp, tFpWt, fc, rRn, 1.0);
        } else
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT) {
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
    static_assert(WTYPE!=jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT &&
                  WTYPE!=jsex_nnap_basis_Chebyshev_WTYPE_NONE &&
                  WTYPE!=jsex_nnap_basis_Chebyshev_WTYPE_FULL &&
                  WTYPE!=jsex_nnap_basis_Chebyshev_WTYPE_EXFULL, "WTYPE INVALID");
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
        if (WTYPE == jsex_nnap_basis_Chebyshev_WTYPE_FUSE) {
            jdouble *tGradFp = aGradFp;
            jdouble *tGradPara = rGradPara;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble tGradPara_ = 0.0;
                for (jint n = 0; n <= NMAX; ++n) {
                    tGradPara_ += fc*tRn[n]*tGradFp[n];
                }
                tGradPara[type-1] += tGradPara_;
                tGradFp += (NMAX+1);
                tGradPara += aTypeNum;
            }
            continue;
        }
    }
}

template <jint NMAX, jint WTYPE>
static void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                     jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                     jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                     jint aTypeNum, jdouble aRCut, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    // init cache
    jdouble *tNlRn = aForwardCache;
    jdouble *tNlFc = tNlRn + aNN*(NMAX+1);
    jdouble *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    jdouble *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    jdouble *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    if (aFullCache) {
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
        JSE_NNAP::calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        if (aFullCache) {
            rNlFcPx[j] = fcPx;
            rNlFcPy[j] = fcPy;
            rNlFcPz[j] = fcPz;
        }
        // cal RnPxyz
        if (aFullCache) {
            rRnPx = rNlRnPx + j*(NMAX+1);
            rRnPy = rNlRnPy + j*(NMAX+1);
            rRnPz = rNlRnPz + j*(NMAX+1);
        }
        JSE_NNAP::calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, dx, dy, dz);
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_FUSE) {
            jdouble *tGradRn = rCheby2;
            for (jint n = 0; n <= NMAX; ++n) {
                tGradRn[n] = 0.0;
            }
            jdouble *tFuseWeight = aFuseWeight;
            jdouble *tNNGrad = aNNGrad;
            for (jint k = 0; k < aFuseSize; ++k) {
                jdouble wt = tFuseWeight[type-1];
                for (jint n = 0; n <= NMAX; ++n) {
                    tGradRn[n] += wt*tNNGrad[n];
                }
                tNNGrad += (NMAX+1);
                tFuseWeight += aTypeNum;
            }
            gradRn2Fxyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_NONE || aTypeNum==1) {
            gradRn2Fxyz<NMAX>(j, aNNGrad, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_FULL) {
            jdouble *tGradRn = aNNGrad + (NMAX+1)*(type-1);
            gradRn2Fxyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_EXFULL) {
            jdouble *tNNGradWt = aNNGrad + (NMAX+1)*type;
            gradRnWt2Fxyz<NMAX>(j, aNNGrad, tNNGradWt, fc, tRn, 1.0, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else
        if (WTYPE==jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT) {
            jdouble wt = ((type&1)==1) ? type : -type;
            jdouble *tNNGradWt = aNNGrad + (NMAX+1);
            gradRnWt2Fxyz<NMAX>(j, aNNGrad, tNNGradWt, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rFx, rFy, rFz);
        } else {
            continue;
        }
    }
}

template <jint WTYPE>
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN, jdouble *rFp,
                         jdouble *rForwardCache, jboolean aFullCache,
                         jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch (aNMax) {
    case 0: {
        calFp<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 1: {
        calFp<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 2: {
        calFp<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 3: {
        calFp<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 4: {
        calFp<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 5: {
        calFp<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 6: {
        calFp<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 7: {
        calFp<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 8: {
        calFp<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 9: {
        calFp<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 10: {
        calFp<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 11: {
        calFp<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 12: {
        calFp<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 13: {
        calFp<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 14: {
        calFp<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 15: {
        calFp<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 16: {
        calFp<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 17: {
        calFp<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 18: {
        calFp<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 19: {
        calFp<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 20: {
        calFp<20, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    default: {
        return;
    }}
}
static inline void calFp(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN, jdouble *rFp,
                         jdouble *rForwardCache, jboolean aFullCache,
                         jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FUSE: {
        calFp<jsex_nnap_basis_Chebyshev_WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp, rForwardCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    default: {
        return;
    }}
}


template <jint WTYPE>
static inline void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                               jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache,
                               jint aTypeNum, jdouble aRCut, jint aNMax, jint aFuseSize) noexcept {
    switch (aNMax) {
    case 0: {
        calBackward<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 1: {
        calBackward<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 2: {
        calBackward<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 3: {
        calBackward<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 4: {
        calBackward<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 5: {
        calBackward<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 6: {
        calBackward<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 7: {
        calBackward<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 8: {
        calBackward<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 9: {
        calBackward<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 10: {
        calBackward<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 11: {
        calBackward<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 12: {
        calBackward<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 13: {
        calBackward<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 14: {
        calBackward<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 15: {
        calBackward<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 16: {
        calBackward<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 17: {
        calBackward<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 18: {
        calBackward<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 19: {
        calBackward<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    case 20: {
        calBackward<20, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aFuseSize);
        return;
    }
    default: {
        return;
    }}
}
static inline void calBackward(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                               jdouble *aGradFp, jdouble *rGradPara, jdouble *aForwardCache,
                               jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseSize) noexcept {
    if (aWType == jsex_nnap_basis_Chebyshev_WTYPE_FUSE) {
        calBackward<jsex_nnap_basis_Chebyshev_WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aGradFp, rGradPara, aForwardCache, aTypeNum, aRCut, aNMax, aFuseSize);
    }
}


template <jint WTYPE>
static inline void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                            jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                            jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                            jint aTypeNum, jdouble aRCut, jint aNMax, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch (aNMax) {
    case 0: {
        calForce<0, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 1: {
        calForce<1, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 2: {
        calForce<2, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 3: {
        calForce<3, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 4: {
        calForce<4, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 5: {
        calForce<5, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 6: {
        calForce<6, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 7: {
        calForce<7, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 8: {
        calForce<8, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 9: {
        calForce<9, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 10: {
        calForce<10, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 11: {
        calForce<11, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 12: {
        calForce<12, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 13: {
        calForce<13, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 14: {
        calForce<14, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 15: {
        calForce<15, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 16: {
        calForce<16, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 17: {
        calForce<17, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 18: {
        calForce<18, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 19: {
        calForce<19, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    case 20: {
        calForce<20, WTYPE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aFuseWeight, aFuseSize);
        return;
    }
    default: {
        return;
    }}
}
static inline void calForce(jdouble *aNlDx, jdouble *aNlDy, jdouble *aNlDz, jint *aNlType, jint aNN,
                            jdouble *aNNGrad, jdouble *rFx, jdouble *rFy, jdouble *rFz,
                            jdouble *aForwardCache, jdouble *rForwardForceCache, jboolean aFullCache,
                            jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jdouble *aFuseWeight, jint aFuseSize) noexcept {
    switch(aWType) {
    case jsex_nnap_basis_Chebyshev_WTYPE_EXFULL: {
        calForce<jsex_nnap_basis_Chebyshev_WTYPE_EXFULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FULL: {
        calForce<jsex_nnap_basis_Chebyshev_WTYPE_FULL>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_NONE: {
        calForce<jsex_nnap_basis_Chebyshev_WTYPE_NONE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT: {
        calForce<jsex_nnap_basis_Chebyshev_WTYPE_DEFAULT>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    case jsex_nnap_basis_Chebyshev_WTYPE_FUSE: {
        calForce<jsex_nnap_basis_Chebyshev_WTYPE_FUSE>(aNlDx, aNlDy, aNlDz, aNlType, aNN, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache, aTypeNum, aRCut, aNMax, aFuseWeight, aFuseSize);
        return;
    }
    default: {
        return;
    }}
}



extern "C" {

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_forward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray rFp, jint aShiftFp, jdoubleArray rForwardCache, jint aForwardCacheShift, jboolean aFullCache,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jdoubleArray aFuseWeight, jint aFuseSize) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tFp = (jdouble *)getJArrayBuf(aEnv, rFp);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, rForwardCache);
    jdouble *tFuseWeight = aFuseWeight==NULL ? NULL : (jdouble *)getJArrayBuf(aEnv, aFuseWeight);
    
    // do cal
    calFp(tNlDx, tNlDy, tNlDz, tNlType, aNN, tFp+aShiftFp,
          tForwardCache+aForwardCacheShift, aFullCache,
          aTypeNum, aRCut, aNMax, aWType, tFuseWeight, aFuseSize);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, rFp, tFp, 0);
    releaseJArrayBuf(aEnv, rForwardCache, tForwardCache, 0);
    if (aFuseWeight!=NULL) releaseJArrayBuf(aEnv, aFuseWeight, tFuseWeight, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_backward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aGradFp, jint aShiftGradFp, jdoubleArray rGradPara, jint aShiftGradPara,
        jdoubleArray aForwardCache, jint aForwardCacheShift,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseSize) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tGradFp = (jdouble *)getJArrayBuf(aEnv, aGradFp);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, aForwardCache);
    
    // do cal
    calBackward(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                tGradFp+aShiftGradFp, tGradPara+aShiftGradPara,
                tForwardCache+aForwardCacheShift,
                aTypeNum, aRCut, aNMax, aWType, aFuseSize);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradFp, tGradFp, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aForwardCache, tForwardCache, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_forwardForce1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNNGrad, jint aShiftFp, jdoubleArray rFx, jdoubleArray rFy, jdoubleArray rFz,
        jdoubleArray aForwardCache, jint aForwardCacheShift, jdoubleArray rForwardForceCache, jint aForwardForceCacheShift, jboolean aFullCache,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jdoubleArray aFuseWeight, jint aFuseSize) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tNNGrad = (jdouble *)getJArrayBuf(aEnv, aNNGrad);
    jdouble *tFx = (jdouble *)getJArrayBuf(aEnv, rFx);
    jdouble *tFy = (jdouble *)getJArrayBuf(aEnv, rFy);
    jdouble *tFz = (jdouble *)getJArrayBuf(aEnv, rFz);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, aForwardCache);
    jdouble *tForwardForceCache = (jdouble *)getJArrayBuf(aEnv, rForwardForceCache);
    jdouble *tFuseWeight = aFuseWeight==NULL ? NULL : (jdouble *)getJArrayBuf(aEnv, aFuseWeight);
    
    calForce(tNlDx, tNlDy, tNlDz, tNlType, aNN,
             tNNGrad+aShiftFp, tFx, tFy, tFz,
             tForwardCache+aForwardCacheShift, tForwardForceCache+aForwardForceCacheShift, aFullCache,
             aTypeNum, aRCut, aNMax, aWType, tFuseWeight, aFuseSize);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNNGrad, tNNGrad, JNI_ABORT);
    releaseJArrayBuf(aEnv, rFx, tFx, 0);
    releaseJArrayBuf(aEnv, rFy, tFy, 0);
    releaseJArrayBuf(aEnv, rFz, tFz, 0);
    releaseJArrayBuf(aEnv, aForwardCache, tForwardCache, JNI_ABORT);
    releaseJArrayBuf(aEnv, rForwardForceCache, tForwardForceCache, 0);
    if (aFuseWeight!=NULL) releaseJArrayBuf(aEnv, aFuseWeight, tFuseWeight, JNI_ABORT);
}

}
