#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int LMAXMAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void calAnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rAnlm,
                                flt_t **rForwardCache, flt_t aRCut, flt_t *aParams) noexcept {
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
        // cal Y
        if (REQUIRE_CACHE) rY = rNlY + j*tLMAll;
        calY<LMAXMAX>(rY, dx, dy, dz, dis);
        if (WTYPE==WTYPE_RFUSE) {
#ifdef NNAP_USE_TABLE
            flt_t ix; int il, ir;
            tableInit(dis/aRCut, ix, il, ir);
            // cal Rnp
            if (REQUIRE_CACHE) rRnp = rNlRnp + j*SIZE_NP;
            flt_t *tTable = aParams + (type-1)*SIZE_NP*(NNAP_TABLE_SIZE+1);
            for (int np = 0; np < SIZE_NP; ++np) {
                rRnp[np] = calRFuncFromTable(ix, il, ir, tTable);
                tTable += (NNAP_TABLE_SIZE+1);
            }
#else
            // cal Rn
            if (REQUIRE_CACHE) rRn = rNlRn + j*(NMAX+1);
            calRn<NMAX>(rRn, dis, aRCut);
            // cal Rnp
            if (REQUIRE_CACHE) rRnp = rNlRnp + j*SIZE_NP;
            calRnp<NMAX, SIZE_NP>(rRnp, rRn, aParams + (type-1)*(SIZE_NP*(NMAX+1)));
#endif
            // cal anlm
            mplusAnlm<SIZE_NP, LMAXMAX>(rAnlm, rY, rRnp);
        } else {
            // cal Rn
            if (REQUIRE_CACHE) rRn = rNlRn + j*(NMAX+1);
            calRn<NMAX>(rRn, dis, aRCut);
            if (WTYPE==WTYPE_NONE) {
                mplusAnlm<NMAX+1, LMAXMAX>(rAnlm, rY, rRn);
            } else
            if (WTYPE==WTYPE_FULL) {
                flt_t *tCnlm = rAnlm + (type-1)*(NMAX+1)*tLMAll;
                mplusAnlm<NMAX+1, LMAXMAX>(tCnlm, rY, rRn);
            } else
            if (WTYPE==WTYPE_EXFULL) {
                flt_t *tCnlm = rAnlm + type*(NMAX+1)*tLMAll;
                mplusAnlmEx<NMAX+1, LMAXMAX>(rAnlm, tCnlm, rY, rRn);
            }
        }
    }
}

template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void sphForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                                   flt_t **rForwardCache, flt_t aRCut, flt_t *aParams) noexcept {
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
    calAnlm<WTYPE, NMAX, tLMaxMax, SIZE_NP, REQUIRE_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rAnlm, rForwardCache, aRCut, aParams);
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
                                     flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aParams) {
    const int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    // init cache
    flt_t *tNlRn = *aForwardCache; *aForwardCache += aNeiNum*(NMAX+1);
    flt_t *tNlRnp = *aForwardCache; *aForwardCache += aNeiNum*SIZE_NP;
    flt_t *tNlY  = *aForwardCache; *aForwardCache += aNeiNum*tLMAll;
    flt_t bRnGrad[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnGrad = REQUIRE_CACHE ? NULL : bRnGrad;
    flt_t rCheby2[NMAX+1];
    flt_t bRnpGrad[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpGrad = REQUIRE_CACHE ? NULL : bRnpGrad;
    flt_t bYPtheta[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPtheta = REQUIRE_CACHE ? NULL : bYPtheta;
    flt_t bYPphi[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPphi = REQUIRE_CACHE ? NULL : bYPphi;
    flt_t rGradY[tLMAll];
    flt_t *rNlRnGrad = NULL, *rNlRnpGrad = NULL, *rNlYPtheta = NULL, *rNlYPphi = NULL;
    if (REQUIRE_CACHE) {
        rNlRnGrad = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnpGrad = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
        rNlYPtheta = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
        rNlYPphi = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
    }
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get Y
        flt_t *tY = tNlY + j*tLMAll;
        // cal YlmPthetaPphi
        if (REQUIRE_CACHE) {
            rYPtheta = rNlYPtheta + j*tLMAll;
            rYPphi = rNlYPphi + j*tLMAll;
        }
        flt_t thetaPx, thetaPy, thetaPz, phiPx, phiPy;
        calYPthetaPphi<LMAXMAX>(
            rYPtheta, rYPphi, tY, dx, dy, dz, dis,
            thetaPx, thetaPy, thetaPz, phiPx, phiPy
        );
        if (WTYPE==WTYPE_RFUSE) {
#ifdef NNAP_USE_TABLE
            flt_t ix; int il, ir;
            tableInit(dis/aRCut, ix, il, ir);
            flt_t *tParams = aParams + __NNAPGEN_NTYPES__*SIZE_NP*(NNAP_TABLE_SIZE+1);
            // get Rnp here
            flt_t *tRnp = tNlRnp + j*SIZE_NP;
            // cal RnpGrad here
            if (REQUIRE_CACHE) rRnpGrad = rNlRnpGrad + j*SIZE_NP;
            flt_t *tTable = tParams + (type-1)*SIZE_NP*(NNAP_TABLE_SIZE+1);
            for (int np = 0; np < SIZE_NP; ++np) {
                rRnpGrad[np] = calRFuncFromTable(ix, il, ir, tTable);
                tTable += (NNAP_TABLE_SIZE+1);
            }
#else
            // get Rn
            flt_t *tRn = tNlRn + j*(NMAX+1);
            // cal RnGrad
            if (REQUIRE_CACHE) rRnGrad = rNlRnGrad + j*(NMAX+1);
            calRnGrad<NMAX>(rRnGrad, rCheby2, dis, aRCut);
            // get Rnp here
            flt_t *tRnp = tNlRnp + j*SIZE_NP;
            // cal RnpGrad here
            if (REQUIRE_CACHE) rRnpGrad = rNlRnpGrad + j*SIZE_NP;
            calRnpGrad<NMAX, SIZE_NP>(rRnpGrad, rRnGrad, aParams + (type-1)*(SIZE_NP*(NMAX+1)));
#endif
            gradAnlm2xyz<SIZE_NP, LMAXMAX>(j,
                aGradAnlm, rGradY, tY, tRnp, rRnpGrad, rYPtheta, rYPphi,
                dx, dy, dz, thetaPx, thetaPy, thetaPz, phiPx, phiPy, rGradNlDx, rGradNlDy, rGradNlDz
            );
        } else {
            // get Rn
            flt_t *tRn = tNlRn + j*(NMAX+1);
            // cal RnGrad
            if (REQUIRE_CACHE) rRnGrad = rNlRnGrad + j*(NMAX+1);
            calRnGrad<NMAX>(rRnGrad, rCheby2, dis, aRCut);
            // cal fxyz
            if (WTYPE==WTYPE_NONE) {
                gradAnlm2xyz<NMAX+1, LMAXMAX>(j,
                    aGradAnlm, rGradY, tY, tRn, rRnGrad, rYPtheta, rYPphi,
                    dx, dy, dz, thetaPx, thetaPy, thetaPz, phiPx, phiPy, rGradNlDx, rGradNlDy, rGradNlDz
                );
            } else
            if (WTYPE==WTYPE_FULL) {
                flt_t *tGradCnlm = aGradAnlm + (type-1)*(NMAX+1)*tLMAll;
                gradAnlm2xyz<NMAX+1, LMAXMAX>(j,
                    tGradCnlm, rGradY, tY, tRn, rRnGrad, rYPtheta, rYPphi,
                    dx, dy, dz, thetaPx, thetaPy, thetaPz, phiPx, phiPy, rGradNlDx, rGradNlDy, rGradNlDz
                );
            } else
            if (WTYPE==WTYPE_EXFULL) {
                flt_t *tGradCnlm = aGradAnlm + type*(NMAX+1)*tLMAll;
                gradAnlmEx2xyz<NMAX+1, LMAXMAX>(j,
                    aGradAnlm, tGradCnlm, rGradY, tY, tRn, rRnGrad, rYPtheta, rYPphi,
                    dx, dy, dz, thetaPx, thetaPy, thetaPz, phiPx, phiPy, rGradNlDx, rGradNlDy, rGradNlDz
                );
            }
        }
    }
}
template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void sphBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                                    flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                    flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aParams) noexcept {
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
    backwardAnlm<WTYPE, NMAX, tLMaxMax, SIZE_NP, REQUIRE_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rGradAnlm, rGradNlDx, rGradNlDy, rGradNlDz, aForwardCache, rBackwardCache, aRCut, aParams);
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H