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

}

#endif //BASIS_CHEBYSHEV_UTIL_H