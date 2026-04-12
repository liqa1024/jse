#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int SIZE_NP>
static NNAP_DEVICE void gradFp2xyz(int j, flt_t *aGradFp, flt_t *aRnpGrad,
                                   flt_t aDx, flt_t aDy, flt_t aDz,
                                   flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    const flt_t rGradj = dot<SIZE_NP>(aGradFp, aRnpGrad);
    rGradNlDx[j] += rGradj*aDx; rGradNlDy[j] += rGradj*aDy; rGradNlDz[j] += rGradj*aDz;
}
template <int SIZE_NP>
static NNAP_DEVICE void gradFp2xyzEx(int j, flt_t *aGradFpEx, flt_t *aGradFp, flt_t *aRnpGrad,
                                     flt_t aDx, flt_t aDy, flt_t aDz,
                                     flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    const flt_t rGradj = dotEx<SIZE_NP>(aGradFpEx, aGradFp, aRnpGrad);
    rGradNlDx[j] += rGradj*aDx; rGradNlDy[j] += rGradj*aDy; rGradNlDz[j] += rGradj*aDz;
}
template <int NMAX, int SIZE_K>
static NNAP_DEVICE void gradFp2xyzFuse(int j, flt_t *aGradFp, flt_t *aRnGrad, flt_t *aFuseWeight,
                                      flt_t aDx, flt_t aDy, flt_t aDz,
                                      flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    flt_t *tGradFp = aGradFp;
    flt_t rGradj = ZERO;
    for (int k = 0; k < SIZE_K; ++k) {
        rGradj += aFuseWeight[k] * dot<NMAX+1>(tGradFp, aRnGrad);
        tGradFp += (NMAX+1);
    }
    rGradNlDx[j] += rGradj*aDx; rGradNlDy[j] += rGradj*aDy; rGradNlDz[j] += rGradj*aDz;
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H