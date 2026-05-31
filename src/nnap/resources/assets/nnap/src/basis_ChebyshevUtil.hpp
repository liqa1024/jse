#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int SIZE_NP>
static inline NNAP_DEVICE NNAP_HOST void mplusFp(flt_t *rFp, flt_t aFc, flt_t *aRnp) noexcept {
    mplus<SIZE_NP>(rFp, aFc, aRnp);
}
template <int SIZE_NP>
static inline NNAP_DEVICE NNAP_HOST void mplusFpWt(flt_t *rFp, flt_t *rFpWt, flt_t aWt, flt_t aFc, flt_t *aRnp) noexcept {
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subFcRnp = aFc*aRnp[np];
        rFp[np] += subFcRnp;
        rFpWt[np] += aWt*subFcRnp;
    }
}

template <int SIZE_NP>
static inline NNAP_DEVICE NNAP_HOST void backwardMplusFp(flt_t *aAGradFp, flt_t aFc, flt_t &rAGradFc, flt_t *aRnp, flt_t *rAGradRnp) noexcept {
    flt_t tAGradFc = ZERO;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subAGradFp = aAGradFp[np];
        tAGradFc += subAGradFp*aRnp[np];
        rAGradRnp[np] += subAGradFp*aFc;
    }
    rAGradFc += tAGradFc;
}
template <int SIZE_NP>
static inline NNAP_DEVICE NNAP_HOST void backwardMplusFpWt(flt_t *aAGradFp, flt_t *aAGradFpWt, flt_t aWt, flt_t aFc, flt_t &rAGradFc, flt_t *aRnp, flt_t *rAGradRnp) noexcept {
    flt_t tAGradFc = ZERO;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subAGradFcRnp = aAGradFp[np] + aWt*aAGradFpWt[np];
        tAGradFc += subAGradFcRnp*aRnp[np];
        rAGradRnp[np] += subAGradFcRnp*aFc;
    }
    rAGradFc += tAGradFc;
}

template <int SIZE_NP, int GRAD_RNP>
static inline NNAP_DEVICE NNAP_HOST void backwardBackwardMplusFp(flt_t *aAGradFp, flt_t *rBGradAGradFp, flt_t aFc, flt_t aBGradAGradFc, flt_t *aRnp, flt_t *rBGradRnp, flt_t *aBGradAGradRnp) noexcept {
    for (int np = 0; np < SIZE_NP; ++np) {
        rBGradAGradFp[np] += aBGradAGradFc*aRnp[np] + aBGradAGradRnp[np]*aFc;
        if (GRAD_RNP) {
            rBGradRnp[np] += aBGradAGradFc*aAGradFp[np];
        }
    }
}
template <int SIZE_NP>
static inline NNAP_DEVICE NNAP_HOST void backwardBackwardMplusFp(flt_t *rBGradAGradFp, flt_t aFc, flt_t aBGradAGradFc, flt_t *aRnp, flt_t *aBGradAGradRnp) noexcept {
    backwardBackwardMplusFp<SIZE_NP, FALSE>(NULL, rBGradAGradFp, aFc, aBGradAGradFc, aRnp, NULL, aBGradAGradRnp);
}
template <int SIZE_NP>
static inline NNAP_DEVICE NNAP_HOST void backwardBackwardMplusFpWt(flt_t *rBGradAGradFp, flt_t *rBGradAGradFpWt, flt_t aWt, flt_t aFc, flt_t aBGradAGradFc, flt_t *aRnp, flt_t *aBGradAGradRnp) noexcept {
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t tRHS = aBGradAGradFc*aRnp[np] + aBGradAGradRnp[np]*aFc;
        rBGradAGradFp[np] += tRHS;
        rBGradAGradFpWt[np] += aWt*tRHS;
    }
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H