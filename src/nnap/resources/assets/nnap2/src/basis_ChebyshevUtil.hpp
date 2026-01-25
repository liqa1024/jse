#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static void mplusChebyFuse_(flt_t *rFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aRn) {
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
static inline void mplusChebyFuse(flt_t *rFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aRn) {
    mplusChebyFuse_<NMAX, FSIZE, FSTYLE, FALSE>(rFp, aFuseWeight, aType, aFc, aRn);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline void mplusChebyExFuse(flt_t *rFp, flt_t *aFuseWeight, int aType, flt_t aFc, flt_t *aRn) {
    mplusChebyFuse_<NMAX, FSIZE, FSTYLE, TRUE>(rFp, aFuseWeight, aType, aFc, aRn);
}


template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static void chebyGradRnFuse_(flt_t *rGradRn, flt_t *aGradFp, flt_t *aFuseWeight, int aType) {
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
static inline void chebyGradRnFuse(flt_t *rGradRn, flt_t *aGradFp, flt_t *aFuseWeight, int aType) {
    chebyGradRnFuse_<NMAX, FSIZE, FSTYLE, FALSE>(rGradRn, aGradFp, aFuseWeight, aType);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline void chebyGradRnExFuse(flt_t *rGradRn, flt_t *aGradFp, flt_t *aFuseWeight, int aType) {
    chebyGradRnFuse_<NMAX, FSIZE, FSTYLE, TRUE>(rGradRn, aGradFp, aFuseWeight, aType);
}

template <int NMAX, int WTFLAG>
static void gradRn2xyz_(int j, flt_t *aGradRn, flt_t *aGradRnWt, flt_t aFc, flt_t *aRn, flt_t aWt,
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
static inline void gradRn2xyz(int j, flt_t *aGradRn, flt_t aFc, flt_t *aRn, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    gradRn2xyz_<NMAX, FALSE>(j, aGradRn, NULL, aFc, aRn, ZERO, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}
template <int NMAX>
static inline void gradRnWt2xyz(int j, flt_t *aGradRn, flt_t *aGradRnWt, flt_t aFc, flt_t *aRn, flt_t aWt, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    gradRn2xyz_<NMAX, TRUE>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H