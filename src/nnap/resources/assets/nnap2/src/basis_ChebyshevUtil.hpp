#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int NMAX, int FSIZE, int FSTYLE, int EXFLAG>
static void mplusFpFuse_(double *rFp, double *aFuseWeight, int aType, double aFc, double *aRn) {
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
static inline void mplusFpFuse(double *rFp, double *aFuseWeight, int aType, double aFc, double *aRn) {
    mplusFpFuse_<NMAX, FSIZE, FSTYLE, FALSE>(rFp, aFuseWeight, aType, aFc, aRn);
}
template <int NMAX, int FSIZE, int FSTYLE>
static inline void mplusFpExFuse(double *rFp, double *aFuseWeight, int aType, double aFc, double *aRn) {
    mplusFpFuse_<NMAX, FSIZE, FSTYLE, TRUE>(rFp, aFuseWeight, aType, aFc, aRn);
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H