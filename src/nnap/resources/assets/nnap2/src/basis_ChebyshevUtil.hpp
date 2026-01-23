#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static void mplusChebyFuse_(double *rFp, double *aFuseWeight, int aType, double aFc, double *aRn) {
    double *tFuseWeight = aFuseWeight;
    if (FSTYLE==FSTYLE_LIMITED) {
        tFuseWeight += FSIZE*(aType-1);
    } else {
        tFuseWeight += FSIZE*(NMAX+1)*(aType-1);
    }
    double *tFp = rFp;
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
static inline void mplusChebyFuse(double *rFp, double *aFuseWeight, int aType, double aFc, double *aRn) {
    mplusChebyFuse_<NMAX, FSIZE, FSTYLE, FALSE>(rFp, aFuseWeight, aType, aFc, aRn);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline void mplusChebyExFuse(double *rFp, double *aFuseWeight, int aType, double aFc, double *aRn) {
    mplusChebyFuse_<NMAX, FSIZE, FSTYLE, TRUE>(rFp, aFuseWeight, aType, aFc, aRn);
}


template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static void chebyGradRnFuse_(double *rGradRn, double *aGradFp, double *aFuseWeight, int aType) {
    fill<NMAX+1>(rGradRn, 0.0);
    double *tFuseWeight = aFuseWeight;
    if (FSTYLE==FSTYLE_LIMITED) {
        tFuseWeight += FSIZE*(aType-1);
    } else {
        tFuseWeight += FSIZE*(NMAX+1)*(aType-1);
    }
    double *tGradFp = aGradFp;
    if (EXFLAG) {
        mplus<NMAX+1>(rGradRn, 1.0, tGradFp);
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
static inline void chebyGradRnFuse(double *rGradRn, double *aGradFp, double *aFuseWeight, int aType) {
    chebyGradRnFuse_<NMAX, FSIZE, FSTYLE, FALSE>(rGradRn, aGradFp, aFuseWeight, aType);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline void chebyGradRnExFuse(double *rGradRn, double *aGradFp, double *aFuseWeight, int aType) {
    chebyGradRnFuse_<NMAX, FSIZE, FSTYLE, TRUE>(rGradRn, aGradFp, aFuseWeight, aType);
}

template <int NMAX, int WTFLAG>
static void gradRn2xyz_(int j, double *aGradRn, double *aGradRnWt, double aFc, double *aRn, double aWt,
                        double aFcPx, double aFcPy, double aFcPz, double *aRnPx, double *aRnPy, double *aRnPz,
                        double *rGradNlDx, double *rGradNlDy, double *rGradNlDz) noexcept {
    double tGradFc = 0.0;
    double rGradDxj = 0.0, rGradDyj = 0.0, rGradDzj = 0.0;
    for (int n = 0; n <= NMAX; ++n) {
        double tGradRnn = aGradRn[n];
        if (WTFLAG) tGradRnn += aWt*aGradRnWt[n];
        const double tRnn = aRn[n];
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
static inline void gradRn2xyz(int j, double *aGradRn, double aFc, double *aRn, double aFcPx, double aFcPy, double aFcPz, double *aRnPx, double *aRnPy, double *aRnPz, double *rGradNlDx, double *rGradNlDy, double *rGradNlDz) noexcept {
    gradRn2xyz_<NMAX, FALSE>(j, aGradRn, NULL, aFc, aRn, 0, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}
template <int NMAX>
static inline void gradRnWt2xyz(int j, double *aGradRn, double *aGradRnWt, double aFc, double *aRn, double aWt, double aFcPx, double aFcPy, double aFcPz, double *aRnPx, double *aRnPy, double *aRnPz, double *rGradNlDx, double *rGradNlDy, double *rGradNlDz) noexcept {
    gradRn2xyz_<NMAX, TRUE>(j, aGradRn, aGradRnWt, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H