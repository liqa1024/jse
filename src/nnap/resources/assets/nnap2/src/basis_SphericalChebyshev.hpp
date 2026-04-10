#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int LMAXMAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void calAnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rAnlm,
                                flt_t **rForwardCache, flt_t aRCut, flt_t *aRFuseWeight) noexcept {
    constexpr int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    // init cache
    flt_t bRn[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRn = REQUIRE_CACHE ? NULL : bRn;
    flt_t bRnp[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnp = REQUIRE_CACHE ? NULL : bRnp;
    flt_t bY[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rY = REQUIRE_CACHE ? NULL : bY;
    flt_t *rNlRn = NULL, *rNlRnp = NULL, *rNlY = NULL;
    if (REQUIRE_CACHE) {
        rNlRn = *rForwardCache; *rForwardCache += aNeiNum*(NMAX+1);
        rNlRnp = *rForwardCache; *rForwardCache += aNeiNum*SIZE_NP;
        rNlY  = *rForwardCache; *rForwardCache += aNeiNum*tLMAll;
    }
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
        // cal Y
        if (REQUIRE_CACHE) rY = rNlY + j*tLMAll;
        calY<LMAXMAX>(dx, dy, dz, dis, rY);
        if (WTYPE==WTYPE_NONE) {
            mplusCnlm<NMAX, LMAXMAX>(rAnlm, rY, fc, rRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tCnlm = rAnlm + (type-1)*(NMAX+1)*tLMAll;
            mplusCnlm<NMAX, LMAXMAX>(tCnlm, rY, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tCnlm = rAnlm + type*(NMAX+1)*tLMAll;
            mplusCnlmEx<NMAX, LMAXMAX>(rAnlm, tCnlm, rY, fc, rRn);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            // cal Rnp
            if (REQUIRE_CACHE) rRnp = rNlRnp + j*SIZE_NP;
            calRnp<NMAX, SIZE_NP>(rRnp, fc, rRn, aRFuseWeight + (type-1)*(SIZE_NP*(NMAX+1)));
            // cal anlm
            mplusAnlm<SIZE_NP, LMAXMAX>(rAnlm, rY, rRnp);
        }
    }
}

template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void sphForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                                   flt_t **rForwardCache, flt_t aRCut, flt_t *aRFuseWeight) noexcept {
    // const init
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeAnlm = SIZE_NP*tLMAll;
    // init cache
    flt_t bAnlm[REQUIRE_CACHE ? 1 : tSizeAnlm] = {0};
    flt_t *rAnlm = NULL;
    if (REQUIRE_CACHE) {
        rAnlm = *rForwardCache; *rForwardCache += tSizeAnlm;
        fill<tSizeAnlm>(rAnlm, ZERO);
    } else {
        rAnlm = bAnlm;
    }
    // do cal
    calAnlm<WTYPE, NMAX, tLMaxMax, SIZE_NP, REQUIRE_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rAnlm, rForwardCache, aRCut, aRFuseWeight);
    // anlm -> fp
    constexpr int tSizeL2 = NORADIAL?LMAX:(LMAX+1);
    constexpr int tSizeL3 = L3NCOLS[L3MAX];
    for (int np=0, tShift=0, tShiftFp=0; np<SIZE_NP; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calSphL2<LMAX, NORADIAL>(rAnlm+tShift, rFp+tShiftFp);
        calSphL3<L3MAX>(rAnlm+tShift, rFp+tShiftFp+tSizeL2);
        calSphL4<L4MAX>(rAnlm+tShift, rFp+tShiftFp+tSizeL2+tSizeL3);
    }
}

template <int WTYPE, int NMAX, int LMAXMAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void backwardAnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradAnlm,
                                     flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                     flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aRFuseWeight) {
    const int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    // init cache
    flt_t *tNlRn = *aForwardCache; *aForwardCache += aNeiNum*(NMAX+1);
    flt_t *tNlRnp = *aForwardCache; *aForwardCache += aNeiNum*SIZE_NP;
    flt_t *tNlY  = *aForwardCache; *aForwardCache += aNeiNum*tLMAll;
    flt_t bRnPx[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPx = REQUIRE_CACHE ? NULL : bRnPx;
    flt_t bRnPy[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPy = REQUIRE_CACHE ? NULL : bRnPy;
    flt_t bRnPz[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPz = REQUIRE_CACHE ? NULL : bRnPz;
    flt_t rCheby2[NMAX+1];
    flt_t bRnpPx[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpPx = REQUIRE_CACHE ? NULL : bRnpPx;
    flt_t bRnpPy[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpPy = REQUIRE_CACHE ? NULL : bRnpPy;
    flt_t bRnpPz[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpPz = REQUIRE_CACHE ? NULL : bRnpPz;
    flt_t bYPx[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPx = REQUIRE_CACHE ? NULL : bYPx;
    flt_t bYPy[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPy = REQUIRE_CACHE ? NULL : bYPy;
    flt_t bYPz[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPz = REQUIRE_CACHE ? NULL : bYPz;
    flt_t rYPtheta[tLMAll];
    flt_t rYPphi[tLMAll];
    flt_t *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    flt_t *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    flt_t *rNlRnpPx = NULL, *rNlRnpPy = NULL, *rNlRnpPz = NULL;
    flt_t *rNlYPx = NULL, *rNlYPy = NULL, *rNlYPz = NULL;
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
        rNlYPx  = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
        rNlYPy  = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
        rNlYPz  = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
    }
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get Rn Y
        flt_t *tRn = tNlRn + j*(NMAX+1);
        flt_t *tY = tNlY + j*tLMAll;
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
        // cal YlmPxyz
        if (REQUIRE_CACHE) {
            rYPx = rNlYPx + j*tLMAll;
            rYPy = rNlYPy + j*tLMAll;
            rYPz = rNlYPz + j*tLMAll;
        }
        calYPxyz<LMAXMAX>(dx, dy, dz, dis, tY, rYPx, rYPy, rYPz, rYPtheta, rYPphi);
        // cal fxyz
        if (WTYPE==WTYPE_NONE) {
            gradCnlm2xyz<NMAX, LMAXMAX>(j, aGradAnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tGradCnlm = aGradAnlm + (type-1)*(NMAX+1)*tLMAll;
            gradCnlm2xyz<NMAX, LMAXMAX>(j, tGradCnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tGradCnlm = aGradAnlm + type*(NMAX+1)*tLMAll;
            gradCnlmEx2xyz<NMAX, LMAXMAX>(j, aGradAnlm, tGradCnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_RFUSE) {
            // get Rnp here
            flt_t *tRnp = tNlRnp + j*SIZE_NP;
            // cal RnpPxyz here
            if (REQUIRE_CACHE) {
                rRnpPx = rNlRnpPx + j*SIZE_NP;
                rRnpPy = rNlRnpPy + j*SIZE_NP;
                rRnpPz = rNlRnpPz + j*SIZE_NP;
            }
            calRnpPxyz<NMAX, SIZE_NP>(rRnpPx, rRnpPy, rRnpPz, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, aRFuseWeight + (type-1)*(SIZE_NP*(NMAX+1)));
            gradAnlm2xyz<SIZE_NP, LMAXMAX>(j, aGradAnlm, rYPtheta, tY, tRnp, rRnpPx, rRnpPy, rRnpPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}
template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void sphBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                                    flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                    flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aRFuseWeight) noexcept {
    // const init
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeAnlm = SIZE_NP*tLMAll;
    // init cache
    flt_t *tAnlm = *aForwardCache; *aForwardCache += tSizeAnlm;
    flt_t bGradAnlm[REQUIRE_CACHE ? 1 : tSizeAnlm] = {0};
    flt_t *rGradAnlm = NULL;
    if (REQUIRE_CACHE) {
        rGradAnlm = *rBackwardCache; *rBackwardCache += tSizeAnlm;
        fill<tSizeAnlm>(rGradAnlm, ZERO);
    } else {
        rGradAnlm = bGradAnlm;
    }
    // fp -> anlm
    constexpr int tSizeL2 = NORADIAL?LMAX:(LMAX+1);
    constexpr int tSizeL3 = L3NCOLS[L3MAX];
    for (int np=0, tShift=0, tShiftFp=0; np<SIZE_NP; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradSphL2<LMAX, NORADIAL>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp);
        calGradSphL3<L3MAX>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2);
        calGradSphL4<L4MAX>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2+tSizeL3);
    }
    backwardAnlm<WTYPE, NMAX, tLMaxMax, SIZE_NP, REQUIRE_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rGradAnlm, rGradNlDx, rGradNlDy, rGradNlDz, aForwardCache, rBackwardCache, aRCut, aRFuseWeight);
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H