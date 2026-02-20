#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static NNAP_DEVICE void mplusChebyFuseBatch_(int bi, int nb,
        flt_t *rBatchFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aBatchRn) {
    flt_t *tFuseWeight = aFuseWeight;
    if (FSTYLE==FSTYLE_LIMITED) {
        tFuseWeight += FSIZE*(aType-1);
    } else {
        tFuseWeight += FSIZE*(NMAX+1)*(aType-1);
    }
    flt_t *tBatchFp = rBatchFp;
    if (EXFLAG) {
        mplusBatch<NMAX+1>(bi, nb, tBatchFp, aFc, aBatchRn);
        tBatchFp += (NMAX+1)*nb;
    }
    for (int k = 0; k < FSIZE; ++k) {
        if (FSTYLE==FSTYLE_LIMITED) {
            mplusBatch<NMAX+1>(bi, nb, tBatchFp, aFc*tFuseWeight[k], aBatchRn);
        } else {
            for (int n = 0; n <= NMAX; ++n) {
                tBatchFp[n*nb + bi] += tFuseWeight[n]*aFc * aBatchRn[n*nb + bi];
            }
            tFuseWeight += (NMAX+1);
        }
        tBatchFp += (NMAX+1)*nb;
    }
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void mplusChebyFuseBatch(int bi, int nb, flt_t *rBatchFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aBatchRn) {
    mplusChebyFuseBatch_<NMAX, FSIZE, FSTYLE, FALSE>(bi, nb, rBatchFp, aFuseWeight, aType, aFc, aBatchRn);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void mplusChebyExFuseBatch(int bi, int nb, flt_t *rBatchFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aBatchRn) {
    mplusChebyFuseBatch_<NMAX, FSIZE, FSTYLE, TRUE>(bi, nb, rBatchFp, aFuseWeight, aType, aFc, aBatchRn);
}

template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static NNAP_DEVICE void mplusChebyFuse_(flt_t *rFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aRn) {
    flt_t *tFuseWeight = aFuseWeight;
    if (FSTYLE==FSTYLE_LIMITED) {
        tFuseWeight += FSIZE*(aType-1);
    } else {
        tFuseWeight += FSIZE*(NMAX+1)*(aType-1);
    }
    flt_t *tFp = rFp;
    if (EXFLAG) {
        mplus<NMAX+1>(tFp, aFc, aRn);
        tFp += (NMAX+1);
    }
    for (int k = 0; k < FSIZE; ++k) {
        if (FSTYLE==FSTYLE_LIMITED) {
            mplus<NMAX+1>(tFp, aFc*tFuseWeight[k], aRn);
        } else {
            for (int n = 0; n <= NMAX; ++n) {
                tFp[n] += tFuseWeight[n]*aFc*aRn[n];
            }
            tFuseWeight += (NMAX+1);
        }
        tFp += (NMAX+1);
    }
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void mplusChebyFuse(flt_t *rFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aRn) {
    mplusChebyFuse_<NMAX, FSIZE, FSTYLE, FALSE>(rFp, aFuseWeight, aType, aFc, aRn);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void mplusChebyExFuse(flt_t *rFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aRn) {
    mplusChebyFuse_<NMAX, FSIZE, FSTYLE, TRUE>(rFp, aFuseWeight, aType, aFc, aRn);
}


template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static NNAP_DEVICE void chebyGradRnFuseBatch_(int bi, int nb,
        flt_t *rBatchGradRn, flt_t *aBatchGradFp, flt_t *aFuseWeight, int aType) {
    fillBatch<NMAX+1>(bi, nb, rBatchGradRn, ZERO);
    flt_t *tFuseWeight = aFuseWeight;
    if (FSTYLE==FSTYLE_LIMITED) {
        tFuseWeight += FSIZE*(aType-1);
    } else {
        tFuseWeight += FSIZE*(NMAX+1)*(aType-1);
    }
    flt_t *tBatchGradFp = aBatchGradFp;
    if (EXFLAG) {
        mplusBatch<NMAX+1>(bi, nb, rBatchGradRn, ONE, tBatchGradFp);
        tBatchGradFp += (NMAX+1)*nb;
    }
    for (int k = 0; k < FSIZE; ++k) {
        if (FSTYLE==FSTYLE_LIMITED) {
            mplusBatch<NMAX+1>(bi, nb, rBatchGradRn, tFuseWeight[k], tBatchGradFp);
        } else {
            for (int n = 0; n <= NMAX; ++n) {
                rBatchGradRn[n*nb + bi] += tFuseWeight[n] * tBatchGradFp[n*nb + bi];
            }
            tFuseWeight += (NMAX+1);
        }
        tBatchGradFp += (NMAX+1)*nb;
    }
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void chebyGradRnFuseBatch(int bi, int nb, flt_t *rBatchGradRn, flt_t *aBatchGradFp, flt_t *aFuseWeight, int aType) {
    chebyGradRnFuseBatch_<NMAX, FSIZE, FSTYLE, FALSE>(bi, nb, rBatchGradRn, aBatchGradFp, aFuseWeight, aType);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void chebyGradRnExFuseBatch(int bi, int nb, flt_t *rBatchGradRn, flt_t *aBatchGradFp, flt_t *aFuseWeight, int aType) {
    chebyGradRnFuseBatch_<NMAX, FSIZE, FSTYLE, TRUE>(bi, nb, rBatchGradRn, aBatchGradFp, aFuseWeight, aType);
}

template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static NNAP_DEVICE void chebyGradRnFuse_(flt_t *rGradRn, flt_t *aGradFp, flt_t *aFuseWeight, int aType) {
    fill<NMAX+1>(rGradRn, ZERO);
    flt_t *tFuseWeight = aFuseWeight;
    if (FSTYLE==FSTYLE_LIMITED) {
        tFuseWeight += FSIZE*(aType-1);
    } else {
        tFuseWeight += FSIZE*(NMAX+1)*(aType-1);
    }
    flt_t *tGradFp = aGradFp;
    if (EXFLAG) {
        mplus<NMAX+1>(rGradRn, ONE, tGradFp);
        tGradFp += (NMAX+1);
    }
    for (int k = 0; k < FSIZE; ++k) {
        if (FSTYLE==FSTYLE_LIMITED) {
            mplus<NMAX+1>(rGradRn, tFuseWeight[k], tGradFp);
        } else {
            for (int n = 0; n <= NMAX; ++n) {
                rGradRn[n] += tFuseWeight[n]*tGradFp[n];
            }
            tFuseWeight += (NMAX+1);
        }
        tGradFp += (NMAX+1);
    }
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void chebyGradRnFuse(flt_t *rGradRn, flt_t *aGradFp, flt_t *aFuseWeight, int aType) {
    chebyGradRnFuse_<NMAX, FSIZE, FSTYLE, FALSE>(rGradRn, aGradFp, aFuseWeight, aType);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline NNAP_DEVICE void chebyGradRnExFuse(flt_t *rGradRn, flt_t *aGradFp, flt_t *aFuseWeight, int aType) {
    chebyGradRnFuse_<NMAX, FSIZE, FSTYLE, TRUE>(rGradRn, aGradFp, aFuseWeight, aType);
}

template <int NMAX, int WTFLAG>
static NNAP_DEVICE void gradRn2xyzBatch_(int bi, int nb,
        int j, flt_t *aBatchGradRn, flt_t *aBatchGradRnWt, flt_t aFc, flt_t *aBatchRn, flt_t aWt,
        flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aBatchRnPx, flt_t *aBatchRnPy, flt_t *aBatchRnPz,
        flt_t *rBatchGradNlDx, flt_t *rBatchGradNlDy, flt_t *rBatchGradNlDz) noexcept {
    flt_t tGradFc = ZERO;
    flt_t rGradDxj = ZERO, rGradDyj = ZERO, rGradDzj = ZERO;
    for (int n = 0; n <= NMAX; ++n) {
        flt_t tGradRnn = aBatchGradRn[n*nb + bi];
        if (WTFLAG) tGradRnn += aWt*aBatchGradRnWt[n*nb + bi];
        const flt_t tRnn = aBatchRn[n*nb + bi];
        tGradFc += tRnn * tGradRnn;
        tGradRnn *= aFc;
        rGradDxj += tGradRnn*aBatchRnPx[n*nb + bi];
        rGradDyj += tGradRnn*aBatchRnPy[n*nb + bi];
        rGradDzj += tGradRnn*aBatchRnPz[n*nb + bi];
    }
    rGradDxj += aFcPx*tGradFc;
    rGradDyj += aFcPy*tGradFc;
    rGradDzj += aFcPz*tGradFc;
    rBatchGradNlDx[j*nb + bi] += rGradDxj;
    rBatchGradNlDy[j*nb + bi] += rGradDyj;
    rBatchGradNlDz[j*nb + bi] += rGradDzj;
}
template <int NMAX>
static inline NNAP_DEVICE void gradRn2xyzBatch(int bi, int nb, int j, flt_t *aBatchGradRn, flt_t aFc, flt_t *aBatchRn, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aBatchRnPx, flt_t *aBatchRnPy, flt_t *aBatchRnPz, flt_t *rBatchGradNlDx, flt_t *rBatchGradNlDy, flt_t *rBatchGradNlDz) noexcept {
    gradRn2xyzBatch_<NMAX, FALSE>(bi, nb, j, aBatchGradRn, NULL, aFc, aBatchRn, ZERO, aFcPx, aFcPy, aFcPz, aBatchRnPx, aBatchRnPy, aBatchRnPz, rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz);
}
template <int NMAX>
static inline NNAP_DEVICE void gradRnWt2xyzBatch(int bi, int nb, int j, flt_t *aBatchGradRn, flt_t *aBatchGradRnWt, flt_t aFc, flt_t *aBatchRn, flt_t aWt, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aBatchRnPx, flt_t *aBatchRnPy, flt_t *aBatchRnPz, flt_t *rBatchGradNlDx, flt_t *rBatchGradNlDy, flt_t *rBatchGradNlDz) noexcept {
    gradRn2xyzBatch_<NMAX, TRUE>(bi, nb, j, aBatchGradRn, aBatchGradRnWt, aFc, aBatchRn, aWt, aFcPx, aFcPy, aFcPz, aBatchRnPx, aBatchRnPy, aBatchRnPz, rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz);
}

template <int NMAX, int WTFLAG>
static NNAP_DEVICE void gradRn2xyz_(int j, flt_t *aGradRn, flt_t *aGradRnWt, flt_t aFc, flt_t *aRn, flt_t aWt,
                                    flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz,
                                    flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    flt_t tGradFc = ZERO;
    flt_t rGradDxj = ZERO, rGradDyj = ZERO, rGradDzj = ZERO;
    for (int n = 0; n <= NMAX; ++n) {
        flt_t tGradRnn = aGradRn[n];
        if (WTFLAG) tGradRnn += aWt*aGradRnWt[n];
        const flt_t tRnn = aRn[n];
        tGradFc += tRnn * tGradRnn;
        tGradRnn *= aFc;
        rGradDxj += tGradRnn*aRnPx[n];
        rGradDyj += tGradRnn*aRnPy[n];
        rGradDzj += tGradRnn*aRnPz[n];
    }
    rGradDxj += aFcPx*tGradFc;
    rGradDyj += aFcPy*tGradFc;
    rGradDzj += aFcPz*tGradFc;
    rGradNlDx[j] += rGradDxj; rGradNlDy[j] += rGradDyj; rGradNlDz[j] += rGradDzj;
}
template <int NMAX>
static inline NNAP_DEVICE void gradRn2xyz(int j, flt_t *aGradRn, flt_t aFc, flt_t *aRn, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    gradRn2xyz_<NMAX, FALSE>(j, aGradRn, NULL, aFc, aRn, ZERO, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}
template <int NMAX>
static inline NNAP_DEVICE void gradRnWt2xyz(int j, flt_t *aGradRn, flt_t *aGradRnWt, flt_t aFc, flt_t *aRn, flt_t aWt, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    gradRn2xyz_<NMAX, TRUE>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H