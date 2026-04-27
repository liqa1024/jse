#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int SIZE_NP>
static NNAP_DEVICE void gradFp2nlj(flt_t *aGradFp, flt_t *aRnpGrad, flt_t &rGradj) noexcept {
    rGradj += dot<SIZE_NP>(aGradFp, aRnpGrad);
}
template <int SIZE_NP>
static NNAP_DEVICE void gradFp2nljWt(flt_t *aGradFp, flt_t *aGradFpWt, flt_t aWt, flt_t *aRnpGrad, flt_t &rGradj) noexcept {
    flt_t tGradj = ZERO;
    for (int np = 0; np < SIZE_NP; ++np) {
        tGradj += (aGradFp[np] + aWt*aGradFpWt[np]) * aRnpGrad[np];
    }
    rGradj += tGradj;
}

template <int SIZE_NP>
static NNAP_DEVICE void gradGradNlj2Rnp(flt_t *aGradFp, flt_t *rGradRnpGrad, flt_t aGradGradj) noexcept {
    mplus<SIZE_NP>(rGradRnpGrad, aGradGradj, aGradFp);
}
template <int SIZE_NP>
static NNAP_DEVICE void gradGradNlj2fp(flt_t *rGradGradFp, flt_t *aRnpGrad, flt_t aGradGradj) noexcept {
    mplus<SIZE_NP>(rGradGradFp, aGradGradj, aRnpGrad);
}
template <int SIZE_NP>
static NNAP_DEVICE void gradGradNlj2fpWt(flt_t *rGradGradFp, flt_t *rGradGradFpWt, flt_t aWt, flt_t *aRnpGrad, flt_t aGradGradj) noexcept {
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t tRHS = aGradGradj*aRnpGrad[np];
        rGradGradFp[np] += tRHS;
        rGradGradFpWt[np] += aWt*tRHS;
    }
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H