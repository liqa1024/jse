#ifndef BASIS_CHEBYSHEV_UTIL_H
#define BASIS_CHEBYSHEV_UTIL_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

template <int NMAX, int EXFLAG>
static NNAP_DEVICE void gradFp2xyz_(int j, flt_t *aGradFpEx, flt_t *aGradFp, flt_t aFc, flt_t *aRn,
                                    flt_t aFcPx, flt_t aFcPy, flt_t aFcPz,
                                    flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz,
                                    flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    flt_t tGradFc = ZERO;
    flt_t rGradDxj = ZERO, rGradDyj = ZERO, rGradDzj = ZERO;
    for (int n = 0; n <= NMAX; ++n) {
        flt_t subGradFp = aGradFp[n];
        if (EXFLAG) subGradFp += aGradFpEx[n];
        const flt_t tRnn = aRn[n];
        tGradFc += tRnn * subGradFp;
        subGradFp *= aFc;
        rGradDxj += subGradFp*aRnPx[n];
        rGradDyj += subGradFp*aRnPy[n];
        rGradDzj += subGradFp*aRnPz[n];
    }
    rGradDxj += aFcPx*tGradFc;
    rGradDyj += aFcPy*tGradFc;
    rGradDzj += aFcPz*tGradFc;
    rGradNlDx[j] += rGradDxj; rGradNlDy[j] += rGradDyj; rGradNlDz[j] += rGradDzj;
}
template <int NMAX>
static inline NNAP_DEVICE void gradFp2xyz(int j, flt_t *aGradFp, flt_t aFc, flt_t *aRn, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    gradFp2xyz_<NMAX, FALSE>(j, NULL, aGradFp, aFc, aRn, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}
template <int NMAX>
static inline NNAP_DEVICE void gradFpEx2xyz(int j, flt_t *aGradFpEx, flt_t *aGradFp, flt_t aFc, flt_t *aRn, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz, flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    gradFp2xyz_<NMAX, TRUE>(j, aGradFpEx, aGradFp, aFc, aRn, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
}

template <int SIZE_NP>
static NNAP_DEVICE void gradFp2xyz(int j, flt_t *aGradFp, flt_t *aRnpPx, flt_t *aRnpPy, flt_t *aRnpPz,
                                   flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    flt_t rGradDxj = ZERO, rGradDyj = ZERO, rGradDzj = ZERO;
    for (int np = 0; np < SIZE_NP; ++np) {
        flt_t subGradFp = aGradFp[np];
        rGradDxj += subGradFp*aRnpPx[np];
        rGradDyj += subGradFp*aRnpPy[np];
        rGradDzj += subGradFp*aRnpPz[np];
    }
    rGradNlDx[j] += rGradDxj; rGradNlDy[j] += rGradDyj; rGradNlDz[j] += rGradDzj;
}

}

#endif //BASIS_CHEBYSHEV_UTIL_H