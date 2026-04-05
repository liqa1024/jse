#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int LMAXMAX, int FSIZE, int REQUIRE_CACHE>
static NNAP_DEVICE void calCnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rCnlm,
                                flt_t **rForwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    constexpr int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    constexpr int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    flt_t bRn[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRn = REQUIRE_CACHE ? NULL : bRn;
    flt_t bY[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rY = REQUIRE_CACHE ? NULL : bY;
    flt_t *rNlRn = NULL, *rNlY = NULL;
    if (REQUIRE_CACHE) {
        rNlRn = *rForwardCache; *rForwardCache += aNeiNum*(NMAX+1);
        rNlY  = *rForwardCache; *rForwardCache += aNeiNum*tLMAll;
    }
    flt_t bBnlm[(WTYPE==WTYPE_FUSE||WTYPE==WTYPE_EXFUSE) ? (REQUIRE_CACHE ? 1 : tSizeBnlm) : 1];
    flt_t *rBnlm = NULL, *rNlBnlm = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        if (REQUIRE_CACHE) {
            rNlBnlm = *rForwardCache; *rForwardCache += aNeiNum*tSizeBnlm;
        } else {
            rBnlm = bBnlm;
        }
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
        // cal cnlm
        if (WTYPE==WTYPE_FUSE) {
            // cal bnlm
            if (REQUIRE_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
            calBnlm<NMAX, LMAXMAX>(rBnlm, rY, fc, rRn);
            // mplus2cnlm
            mplusCnlmFuse<NMAX, LMAXMAX, FSIZE>(rCnlm, rBnlm, aFuseWeight, type);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            // cal bnlm
            if (REQUIRE_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
            calBnlm<NMAX, LMAXMAX>(rBnlm, rY, fc, rRn);
            // mplus2cnlm
            mplusCnlmExFuse<NMAX, LMAXMAX, FSIZE>(rCnlm, rBnlm, aFuseWeight, type);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusCnlm<NMAX, LMAXMAX>(rCnlm, rY, fc, rRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tCnlm = rCnlm + tSizeBnlm*(type-1);
            mplusCnlm<NMAX, LMAXMAX>(tCnlm, rY, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tCnlmWt = rCnlm + tSizeBnlm*type;
            mplusCnlmWt<NMAX, LMAXMAX>(rCnlm, tCnlmWt, rY, fc, rRn, ONE);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tCnlmWt = rCnlm + tSizeBnlm;
            mplusCnlmWt<NMAX, LMAXMAX>(rCnlm, tCnlmWt, rY, fc, rRn, wt);
        }
    }
}

template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int SIZE_N, int PFFLAG, int PFSIZE, int REQUIRE_CACHE>
static NNAP_DEVICE void sphForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                                   flt_t **rForwardCache, flt_t aRCut, flt_t *aFuseWeight, flt_t *aPostFuseWeight, flt_t aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = SIZE_N*tLMAll;
    constexpr int tSizeAnlm = PFSIZE*tLMAll;
    // init cache
    flt_t bCnlm[REQUIRE_CACHE ? 1 : tSizeCnlm] = {0};
    flt_t *rCnlm = NULL;
    if (REQUIRE_CACHE) {
        rCnlm = *rForwardCache; *rForwardCache += tSizeCnlm;
        fill<tSizeCnlm>(rCnlm, ZERO);
    } else {
        rCnlm = bCnlm;
    }
    flt_t bAnlm[PFFLAG ? (REQUIRE_CACHE ? 1 : tSizeAnlm) : 1] = {0};
    flt_t *rAnlm = NULL;
    if (PFFLAG) {
        if (REQUIRE_CACHE) {
            rAnlm = *rForwardCache; *rForwardCache += tSizeAnlm;
            fill<tSizeAnlm>(rAnlm, ZERO);
        } else {
            rAnlm = bAnlm;
        }
    } else {
        rAnlm = rCnlm;
    }
    // do cal
    calCnlm<WTYPE, NMAX, tLMaxMax, FSIZE, REQUIRE_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rCnlm, rForwardCache, aRCut, aFuseWeight);
    // cnlm -> anlm
    if (PFFLAG) {
        mplusAnlm<SIZE_N, tLMaxMax, PFSIZE>(rAnlm, rCnlm, aPostFuseWeight);
        // scale anlm here
        multiply<tSizeAnlm>(rAnlm, aPostFuseScale);
    }
    // anlm -> fp
    constexpr int tSizeL2 = NORADIAL?LMAX:(LMAX+1);
    constexpr int tSizeL3 = L3NCOLS[L3MAX];
    constexpr int tSizeNp = PFFLAG ? PFSIZE : SIZE_N;
    for (int np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calSphL2<LMAX, NORADIAL>(rAnlm+tShift, rFp+tShiftFp);
        calSphL3<L3MAX>(rAnlm+tShift, rFp+tShiftFp+tSizeL2);
        calSphL4<L4MAX>(rAnlm+tShift, rFp+tShiftFp+tSizeL2+tSizeL3);
    }
}

template <int WTYPE, int NMAX, int LMAXMAX, int FSIZE, int REQUIRE_CACHE>
static NNAP_DEVICE void backwardCnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradCnlm,
                                     flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                     flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aFuseWeight) {
    const int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    flt_t *tNlRn = *aForwardCache; *aForwardCache += aNeiNum*(NMAX+1);
    flt_t *tNlY  = *aForwardCache; *aForwardCache += aNeiNum*tLMAll;
    flt_t bRnPx[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPx = REQUIRE_CACHE ? NULL : bRnPx;
    flt_t bRnPy[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPy = REQUIRE_CACHE ? NULL : bRnPy;
    flt_t bRnPz[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnPz = REQUIRE_CACHE ? NULL : bRnPz;
    flt_t rCheby2[NMAX+1];
    flt_t bYPx[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPx = REQUIRE_CACHE ? NULL : bYPx;
    flt_t bYPy[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPy = REQUIRE_CACHE ? NULL : bYPy;
    flt_t bYPz[REQUIRE_CACHE ? 1 : tLMAll]; flt_t *rYPz = REQUIRE_CACHE ? NULL : bYPz;
    flt_t rYPtheta[tLMAll];
    flt_t rYPphi[tLMAll];
    flt_t *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    flt_t *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    flt_t *rNlYPx = NULL, *rNlYPy = NULL, *rNlYPz = NULL;
    if (REQUIRE_CACHE) {
        rNlRnPx = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnPy = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnPz = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlFcPx = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlFcPy = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlFcPz = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlYPx  = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
        rNlYPy  = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
        rNlYPz  = *rBackwardCache; *rBackwardCache += aNeiNum*tLMAll;
    }
    flt_t bGradBnlm[(WTYPE==WTYPE_FUSE||WTYPE==WTYPE_EXFUSE) ? (REQUIRE_CACHE ? 1 : tSizeBnlm) : 1];
    flt_t *rGradBnlm = NULL, *rNlGradBnlm = NULL;
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        if (REQUIRE_CACHE) {
            rNlGradBnlm = *rBackwardCache; *rBackwardCache += aNeiNum*tSizeBnlm;
        } else {
            rGradBnlm = bGradBnlm;
        }
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
        if (WTYPE==WTYPE_FUSE) {
            if (REQUIRE_CACHE) {
                rGradBnlm = rNlGradBnlm + j*tSizeBnlm;
            }
            calGradBnlmFuse<NMAX, LMAXMAX, FSIZE>(aGradCnlm, rGradBnlm, aFuseWeight, type);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, rGradBnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            if (REQUIRE_CACHE) {
                rGradBnlm = rNlGradBnlm + j*tSizeBnlm;
            }
            calGradBnlmExFuse<NMAX, LMAXMAX, FSIZE>(aGradCnlm, rGradBnlm, aFuseWeight, type);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, rGradBnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradBnlm2xyz<NMAX, LMAXMAX>(j, aGradCnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tGradBnlm = aGradCnlm + tSizeBnlm*(type-1);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, tGradBnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tGradCnlmWt = aGradCnlm + tSizeBnlm*type;
            gradCnlmWt2xyz<NMAX, LMAXMAX>(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, ONE, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tGradCnlmWt = aGradCnlm + tSizeBnlm;
            gradCnlmWt2xyz<NMAX, LMAXMAX>(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}
template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int SIZE_N, int PFFLAG, int PFSIZE, int REQUIRE_CACHE>
static NNAP_DEVICE void sphBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                                    flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                    flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aFuseWeight,
                                    flt_t *aPostFuseWeight, flt_t aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = SIZE_N*tLMAll;
    constexpr int tSizeAnlm = PFSIZE*tLMAll;
    // init cache
    flt_t *tCnlm = *aForwardCache; *aForwardCache += tSizeCnlm;
    flt_t bGradCnlm[REQUIRE_CACHE ? 1 : tSizeCnlm] = {0};
    flt_t *rGradCnlm = NULL;
    if (REQUIRE_CACHE) {
        rGradCnlm = *rBackwardCache; *rBackwardCache += tSizeCnlm;
        fill<tSizeCnlm>(rGradCnlm, ZERO);
    } else {
        rGradCnlm = bGradCnlm;
    }
    flt_t *tAnlm = NULL;
    flt_t bGradAnlm[PFFLAG ? (REQUIRE_CACHE ? 1 : tSizeAnlm) : 1] = {0};
    flt_t *rGradAnlm = NULL;
    if (PFFLAG) {
        tAnlm = *aForwardCache; *aForwardCache += tSizeAnlm;
        if (REQUIRE_CACHE) {
            rGradAnlm = *rBackwardCache; *rBackwardCache += tSizeAnlm;
            fill<tSizeAnlm>(rGradAnlm, ZERO);
        } else {
            rGradAnlm = bGradAnlm;
        }
    } else {
        tAnlm = tCnlm;
        rGradAnlm = rGradCnlm;
    }
    // fp -> anlm
    constexpr int tSizeL2 = NORADIAL?LMAX:(LMAX+1);
    constexpr int tSizeL3 = L3NCOLS[L3MAX];
    constexpr int tSizeNp = PFFLAG ? PFSIZE : SIZE_N;
    for (int np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradSphL2<LMAX, NORADIAL>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp);
        calGradSphL3<L3MAX>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2);
        calGradSphL4<L4MAX>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2+tSizeL3);
    }
    if (PFFLAG) {
        // scale anlm here
        multiply<tSizeAnlm>(rGradAnlm, aPostFuseScale);
        // anlm -> cnlm
        mplusGradAnlm<SIZE_N, tLMaxMax, PFSIZE>(rGradAnlm, rGradCnlm, aPostFuseWeight);
    }
    backwardCnlm<WTYPE, NMAX, tLMaxMax, FSIZE, REQUIRE_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rGradCnlm, rGradNlDx, rGradNlDy, rGradNlDz, aForwardCache, rBackwardCache, aRCut, aFuseWeight);
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H