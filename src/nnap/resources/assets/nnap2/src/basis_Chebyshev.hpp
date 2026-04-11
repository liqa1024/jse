#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void chebyForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                                     flt_t **rForwardCache, flt_t aRCut, flt_t *aRFuseWeight) noexcept {
    // init cache
    flt_t bRn[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRn = REQUIRE_CACHE ? NULL : bRn;
    flt_t *rNlRn = NULL;
    if (REQUIRE_CACHE) {
        rNlRn = *rForwardCache; *rForwardCache += aNeiNum*(NMAX+1);
    }
    // clear fp first
    fill<SIZE_NP>(rFp, ZERO);
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        flt_t fc = calFc(dis, aRCut);
        // cal Rn
        if (REQUIRE_CACHE) rRn = rNlRn + j*(NMAX+1);
        calRn<NMAX>(rRn, dis, aRCut);
        // cal fp
        if (WTYPE==WTYPE_NONE) {
            mplus<NMAX+1>(rFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tFp = rFp + (type-1)*(NMAX+1);
            mplus<NMAX+1>(tFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tFp = rFp + type*(NMAX+1);
            mplusEx<NMAX+1>(rFp, tFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            // cal Rnp to fp
            mplusRnp<NMAX, SIZE_NP>(rFp, fc, rRn, aRFuseWeight + (type-1)*(SIZE_NP*(NMAX+1)));
        }
    }
}

template <int WTYPE, int NMAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void chebyBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                                      flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                      flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aRFuseWeight) noexcept {
    // init cache
    flt_t *tNlRn = *aForwardCache; *aForwardCache += aNeiNum*(NMAX+1);
    flt_t bRnPx[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPx = REQUIRE_CACHE ? NULL : bRnPx;
    flt_t bRnPy[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPy = REQUIRE_CACHE ? NULL : bRnPy;
    flt_t bRnPz[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPz = REQUIRE_CACHE ? NULL : bRnPz;
    flt_t rCheby2[NMAX+1];
    flt_t bRnpPx[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpPx = REQUIRE_CACHE ? NULL : bRnpPx;
    flt_t bRnpPy[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpPy = REQUIRE_CACHE ? NULL : bRnpPy;
    flt_t bRnpPz[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpPz = REQUIRE_CACHE ? NULL : bRnpPz;
    flt_t *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    flt_t *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    flt_t *rNlRnpPx = NULL, *rNlRnpPy = NULL, *rNlRnpPz = NULL;
    if (REQUIRE_CACHE) {
        rNlRnPx = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnPy = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnPz = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlFcPx = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlFcPy = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlFcPz = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlRnpPx = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
        rNlRnpPy = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
        rNlRnpPz = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
    }
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get Rn
        flt_t *tRn = tNlRn + j*(NMAX+1);
        // cal fcPxyz
        flt_t fcPx, fcPy, fcPz;
        flt_t fc = calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        if (REQUIRE_CACHE) {
            rNlFcPx[j] = fcPx;
            rNlFcPy[j] = fcPy;
            rNlFcPz[j] = fcPz;
        }
        // cal RnPxyz
        if (REQUIRE_CACHE) {
            rRnPx = rNlRnPx + j*(NMAX+1);
            rRnPy = rNlRnPy + j*(NMAX+1);
            rRnPz = rNlRnPz + j*(NMAX+1);
        }
        calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, dx, dy, dz);
        if (WTYPE==WTYPE_NONE) {
            gradFp2xyz<NMAX>(j, aGradFp, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tGradFp = aGradFp + (type-1)*(NMAX+1);
            gradFp2xyz<NMAX>(j, tGradFp, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tGradFp = aGradFp + type*(NMAX+1);
            gradFpEx2xyz<NMAX>(j, aGradFp, tGradFp, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            // cal RnpPxyz here
            if (REQUIRE_CACHE) {
                rRnpPx = rNlRnpPx + j*SIZE_NP;
                rRnpPy = rNlRnpPy + j*SIZE_NP;
                rRnpPz = rNlRnpPz + j*SIZE_NP;
            }
            calRnpPxyz<NMAX, SIZE_NP>(rRnpPx, rRnpPy, rRnpPz, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, aRFuseWeight + (type-1)*(SIZE_NP*(NMAX+1)));
            gradFp2xyz<SIZE_NP>(j, aGradFp, rRnpPx, rRnpPy, rRnpPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H