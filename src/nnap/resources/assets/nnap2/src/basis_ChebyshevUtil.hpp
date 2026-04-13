#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int NMAX, int SIZE_NP>
static inline NNAP_DEVICE void backwardGradFp(flt_t *aGradFp, flt_t *aRn, flt_t *rGradRFuseWeight) noexcept {
    flt_t *rGradWeight = rGradRFuseWeight;
    for (int np = 0; np < SIZE_NP; ++np) {
        mplus<NMAX+1>(rGradWeight, aGradFp[np], aRn);
        rGradWeight += (NMAX+1);
    }
}
template <int SIZE_NP>
static NNAP_DEVICE void gradFp2xyz(int j, flt_t *aGradFp, flt_t *aRnpGrad,
                                   flt_t aDx, flt_t aDy, flt_t aDz,
                                   flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    const flt_t rGradj = dot<SIZE_NP>(aGradFp, aRnpGrad);
    rGradNlDx[j] += rGradj*aDx; rGradNlDy[j] += rGradj*aDy; rGradNlDz[j] += rGradj*aDz;
}
template <int SIZE_NP>
static NNAP_DEVICE void gradFp2xyzWt(int j, flt_t *aGradFp, flt_t *aGradFpWt, flt_t aWt, flt_t *aRnpGrad,
                                     flt_t aDx, flt_t aDy, flt_t aDz,
                                     flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    flt_t rGradj = ZERO;
    for (int np = 0; np < SIZE_NP; ++np) {
        rGradj += (aGradFp[np] + aWt*aGradFpWt[np]) * aRnpGrad[np];
    }
    rGradNlDx[j] += rGradj*aDx; rGradNlDy[j] += rGradj*aDy; rGradNlDz[j] += rGradj*aDz;
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H