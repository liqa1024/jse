#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

static constexpr int sphSizeN_(int aWType, int aTypeNum, int aNMax, int aFuseSize) noexcept {
    switch(aWType) {
    case WTYPE_EXFULL:  {return (aTypeNum+1)*(aNMax+1);}
    case WTYPE_FULL:    {return aTypeNum*(aNMax+1);}
    case WTYPE_NONE:    {return aNMax+1;}
    case WTYPE_DEFAULT: {return (aNMax+aNMax+2);}
    case WTYPE_FUSE:    {return aFuseSize*(aNMax+1);}
    case WTYPE_EXFUSE:  {return (aFuseSize+1)*(aNMax+1);}
    default:            {return 0;}
    }
}

template <int WTYPE, int NMAX, int LMAXMAX, int FSIZE, int FULL_CACHE>
static void calCnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rCnlm,
                    flt_t *rForwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    constexpr int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    constexpr int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    flt_t *rRn = NULL, *rY = NULL;
    flt_t *rBnlm = NULL;
    flt_t *rNlRn = NULL, *rNlFc = NULL, *rNlY = NULL;
    flt_t *rNlBnlm = NULL;
    if (FULL_CACHE) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNeiNum*(NMAX+1);
        rNlY = rNlFc + aNeiNum;
    } else {
        rRn = rForwardCache;
        rY = rRn + (NMAX+1);
    }
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        if (FULL_CACHE) {
            rNlBnlm = rNlY + aNeiNum*tLMAll;
        } else {
            rBnlm = rY + tLMAll;
        }
    }
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        flt_t fc = calFc(dis, aRCut);
        if (FULL_CACHE) rNlFc[j] = fc;
        // cal Rn
        if (FULL_CACHE) rRn = rNlRn + j*(NMAX+1);
        calRn<NMAX>(rRn, dis, aRCut);
        // cal Y
        if (FULL_CACHE) rY = rNlY + j*tLMAll;
        realSphericalHarmonicsFull4<LMAXMAX>(dx, dy, dz, dis, rY);
        // cal cnlm
        if (WTYPE==WTYPE_FUSE) {
            // cal bnlm
            if (FULL_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
            calBnlm<NMAX, LMAXMAX>(rBnlm, rY, fc, rRn);
            // mplus2cnlm
            mplusCnlmFuse<NMAX, LMAXMAX, FSIZE>(rCnlm, rBnlm, aFuseWeight, type);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            // cal bnlm
            if (FULL_CACHE) rBnlm = rNlBnlm + j*tSizeBnlm;
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

template <int WTYPE, int NTYPES, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int PFFLAG, int PFSIZE, int FULL_CACHE>
static void sphForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                       flt_t *rForwardCache, flt_t aRCut, flt_t *aFuseWeight, flt_t *aPostFuseWeight, flt_t aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeN = sphSizeN_(WTYPE, NTYPES, NMAX, FSIZE);
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = tSizeN*tLMAll;
    constexpr int tSizeAnlm = PFFLAG ? (PFSIZE*tLMAll) : 0;
    // init cache
    flt_t *rCnlm = rForwardCache;
    flt_t *rAnlm = rCnlm + tSizeCnlm;
    flt_t *rCacheElse = rAnlm + tSizeAnlm;
    // clear cnlm first
    fill<tSizeCnlm>(rCnlm, ZERO);
    // do cal
    calCnlm<tWType, NMAX, tLMaxMax, FSIZE, FULL_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rCnlm, rCacheElse, aRCut, aFuseWeight);
    // cnlm -> anlm
    if (PFFLAG) {
        // clear anlm first
        fill<tSizeAnlm>(rAnlm, ZERO);
        mplusAnlm<tSizeN, tLMaxMax, PFSIZE>(rAnlm, rCnlm, aPostFuseWeight);
        // scale anlm here
        multiply<tSizeAnlm>(rAnlm, aPostFuseScale);
    } else {
        rAnlm = rCnlm;
    }
    constexpr int tSizeL2 = NORADIAL?LMAX:(LMAX+1);
    constexpr int tSizeL3 = L3NCOLS[L3MAX];
    constexpr int tSizeNp = PFFLAG ? PFSIZE : tSizeN;
    for (int np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calSphL2<LMAX, NORADIAL>(rAnlm+tShift, rFp+tShiftFp);
        calSphL3<L3MAX>(rAnlm+tShift, rFp+tShiftFp+tSizeL2);
        calSphL4<L4MAX>(rAnlm+tShift, rFp+tShiftFp+tSizeL2+tSizeL3);
    }
}

template <int WTYPE, int NMAX, int LMAXMAX, int FSIZE, int FULL_CACHE>
static void backwardCnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradCnlm,
                         flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                         flt_t *aForwardCache, flt_t *rBackwardCache, flt_t aRCut, flt_t *aFuseWeight) {
    const int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    flt_t *tNlRn = aForwardCache;
    flt_t *tNlFc = tNlRn + aNeiNum*(NMAX+1);
    flt_t *tNlY = tNlFc + aNeiNum;
    flt_t *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    flt_t *rYPx = NULL, *rYPy = NULL, *rYPz = NULL, *rYPtheta = NULL, *rYPphi = NULL;
    flt_t *rGradBnlm = NULL;
    flt_t *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    flt_t *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    flt_t *rNlYPx = NULL, *rNlYPy = NULL, *rNlYPz = NULL;
    flt_t *rNlGradBnlm = NULL;
    if (FULL_CACHE) {
        rNlRnPx = rBackwardCache;
        rNlRnPy = rNlRnPx + aNeiNum*(NMAX+1);
        rNlRnPz = rNlRnPy + aNeiNum*(NMAX+1);
        rNlFcPx = rNlRnPz + aNeiNum*(NMAX+1);
        rNlFcPy = rNlFcPx + aNeiNum;
        rNlFcPz = rNlFcPy + aNeiNum;
        rNlYPx = rNlFcPz + aNeiNum;
        rNlYPy = rNlYPx + aNeiNum*tLMAll;
        rNlYPz = rNlYPy + aNeiNum*tLMAll;
        rYPtheta = rNlYPz + aNeiNum*tLMAll;
        rYPphi = rYPtheta + tLMAll;
        rCheby2 = rYPphi + tLMAll;
    } else {
        rRnPx = rBackwardCache;
        rRnPy = rRnPx + (NMAX+1);
        rRnPz = rRnPy + (NMAX+1);
        rYPx = rRnPz + (NMAX+1);
        rYPy = rYPx + tLMAll;
        rYPz = rYPy + tLMAll;
        rYPtheta = rYPz + tLMAll;
        rYPphi = rYPtheta + tLMAll;
        rCheby2 = rYPphi + tLMAll;
    }
    if (WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
        if (FULL_CACHE) {
            rNlGradBnlm = rCheby2 + (NMAX+1);
        } else {
            rGradBnlm = rCheby2 + (NMAX+1);
        }
    }
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn Y
        flt_t fc = tNlFc[j];
        flt_t *tRn = tNlRn + j*(NMAX+1);
        flt_t *tY = tNlY + j*tLMAll;
        // cal fcPxyz
        flt_t fcPx, fcPy, fcPz;
        calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        if (FULL_CACHE) {
            rNlFcPx[j] = fcPx;
            rNlFcPy[j] = fcPy;
            rNlFcPz[j] = fcPz;
        }
        // cal RnPxyz
        if (FULL_CACHE) {
            rRnPx = rNlRnPx + j*(NMAX+1);
            rRnPy = rNlRnPy + j*(NMAX+1);
            rRnPz = rNlRnPz + j*(NMAX+1);
        }
        calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, dx, dy, dz);
        // cal Ylm
        if (FULL_CACHE) {
            rYPx = rNlYPx + j*tLMAll;
            rYPy = rNlYPy + j*tLMAll;
            rYPz = rNlYPz + j*tLMAll;
        }
        calYPxyz<LMAXMAX>(tY, dx, dy, dz, dis, rYPx, rYPy, rYPz, rYPtheta, rYPphi);
        // cal fxyz
        if (WTYPE==WTYPE_FUSE) {
            if (FULL_CACHE) {
                rGradBnlm = rNlGradBnlm + j*tSizeBnlm;
            }
            calGradBnlmFuse<NMAX, LMAXMAX, FSIZE>(aGradCnlm, rGradBnlm, aFuseWeight, type);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, rGradBnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            if (FULL_CACHE) {
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
template <int WTYPE, int NTYPES, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int PFFLAG, int PFSIZE, int FULL_CACHE, int CLEAR_CACHE>
static void sphBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                        flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                        flt_t *aForwardCache, flt_t *rBackwardCache, flt_t aRCut, flt_t *aFuseWeight,
                        flt_t *aPostFuseWeight, flt_t aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeN = sphSizeN_(WTYPE, NTYPES, NMAX, FSIZE);
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = tSizeN*tLMAll;
    constexpr int tSizeAnlm = PFFLAG ? (PFSIZE*tLMAll) : 0;
    // init cache
    flt_t *tCnlm = aForwardCache;
    flt_t *tAnlm = tCnlm + tSizeCnlm;
    flt_t *tForwardCacheElse = tAnlm + tSizeAnlm;
    flt_t *rGradCnlm = rBackwardCache;
    flt_t *rGradAnlm = rGradCnlm + tSizeCnlm;
    flt_t *rForwardForceCacheElse = rGradAnlm + tSizeAnlm;
    if (CLEAR_CACHE) {
        for (int j = 0; j < aNeiNum; ++j) {
            rGradNlDx[j] = ZERO;
            rGradNlDy[j] = ZERO;
            rGradNlDz[j] = ZERO;
        }
        fill<tSizeAnlm>(rGradAnlm, ZERO);
        fill<tSizeCnlm>(rGradCnlm, ZERO);
    }
    if (!PFFLAG) {
        tAnlm = tCnlm;
        rGradAnlm = rGradCnlm;
    }
    constexpr int tSizeL2 = NORADIAL?LMAX:(LMAX+1);
    constexpr int tSizeL3 = L3NCOLS[L3MAX];
    constexpr int tSizeNp = PFFLAG ? PFSIZE : tSizeN;
    for (int np=0, tShift=0, tShiftFp=0; np<tSizeNp; ++np, tShift+=tLMAll, tShiftFp+=tSizeL) {
        calGradSphL2<LMAX, NORADIAL>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp);
        calGradSphL3<L3MAX>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2);
        calGradSphL4<L4MAX>(tAnlm+tShift, rGradAnlm+tShift, aGradFp+tShiftFp+tSizeL2+tSizeL3);
    }
    if (PFFLAG) {
        // scale anlm here
        multiply<tSizeAnlm>(rGradAnlm, aPostFuseScale);
        // anlm -> cnlm
        mplusGradAnlm<tSizeN, tLMaxMax, PFSIZE>(rGradAnlm, rGradCnlm, aPostFuseWeight);
    }
    backwardCnlm<tWType, NMAX, tLMaxMax, FSIZE, FULL_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rGradCnlm, rGradNlDx, rGradNlDy, rGradNlDz, tForwardCacheElse, rForwardForceCacheElse, aRCut, aFuseWeight);
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H