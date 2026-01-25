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
static void calCnlm(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, double *rCnlm,
                    double *rForwardCache, double aRCut, double *aFuseWeight) noexcept {
    constexpr int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    constexpr int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    double *rRn = NULL, *rY = NULL;
    double *rBnlm = NULL;
    double *rNlRn = NULL, *rNlFc = NULL, *rNlY = NULL;
    double *rNlBnlm = NULL;
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
        double dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        double dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        double fc = calFc(dis, aRCut);
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
            double *tCnlm = rCnlm + tSizeBnlm*(type-1);
            mplusCnlm<NMAX, LMAXMAX>(tCnlm, rY, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            double *tCnlmWt = rCnlm + tSizeBnlm*type;
            mplusCnlmWt<NMAX, LMAXMAX>(rCnlm, tCnlmWt, rY, fc, rRn, 1.0);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : -type;
            double *tCnlmWt = rCnlm + tSizeBnlm;
            mplusCnlmWt<NMAX, LMAXMAX>(rCnlm, tCnlmWt, rY, fc, rRn, wt);
        }
    }
}

template <int WTYPE, int NTYPES, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int PFFLAG, int PFSIZE, int FULL_CACHE>
static void sphForward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, double *rFp,
                       double *rForwardCache, double aRCut, double *aFuseWeight, double *aPostFuseWeight, double aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeN = sphSizeN_(WTYPE, NTYPES, NMAX, FSIZE);
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = tSizeN*tLMAll;
    constexpr int tSizeAnlm = PFFLAG ? (PFSIZE*tLMAll) : 0;
    // init cache
    double *rCnlm = rForwardCache;
    double *rAnlm = rCnlm + tSizeCnlm;
    double *rCacheElse = rAnlm + tSizeAnlm;
    // clear cnlm first
    fill<tSizeCnlm>(rCnlm, 0.0);
    // do cal
    calCnlm<tWType, NMAX, tLMaxMax, FSIZE, FULL_CACHE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rCnlm, rCacheElse, aRCut, aFuseWeight);
    // cnlm -> anlm
    if (PFFLAG) {
        // clear anlm first
        fill<tSizeAnlm>(rAnlm, 0.0);
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
static void backwardCnlm(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, double *aGradCnlm,
                         double *rGradNlDx, double *rGradNlDy, double *rGradNlDz,
                         double *aForwardCache, double *rBackwardCache, double aRCut, double *aFuseWeight) {
    const int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    double *tNlRn = aForwardCache;
    double *tNlFc = tNlRn + aNeiNum*(NMAX+1);
    double *tNlY = tNlFc + aNeiNum;
    double *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    double *rYPx = NULL, *rYPy = NULL, *rYPz = NULL, *rYPtheta = NULL, *rYPphi = NULL;
    double *rGradBnlm = NULL;
    double *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    double *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    double *rNlYPx = NULL, *rNlYPy = NULL, *rNlYPz = NULL;
    double *rNlGradBnlm = NULL;
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
        double dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        double dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn Y
        double fc = tNlFc[j];
        double *tRn = tNlRn + j*(NMAX+1);
        double *tY = tNlY + j*tLMAll;
        // cal fcPxyz
        double fcPx, fcPy, fcPz;
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
            double *tGradBnlm = aGradCnlm + tSizeBnlm*(type-1);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, tGradBnlm, rYPtheta, tY, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            double *tGradCnlmWt = aGradCnlm + tSizeBnlm*type;
            gradCnlmWt2xyz<NMAX, LMAXMAX>(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, 1.0, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : -type;
            double *tGradCnlmWt = aGradCnlm + tSizeBnlm;
            gradCnlmWt2xyz<NMAX, LMAXMAX>(j, aGradCnlm, tGradCnlmWt, rYPtheta, tY, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}
template <int WTYPE, int NTYPES, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int PFFLAG, int PFSIZE, int FULL_CACHE, int CLEAR_CACHE>
static void sphBackward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, double *aGradFp,
                        double *rGradNlDx, double *rGradNlDy, double *rGradNlDz,
                        double *aForwardCache, double *rBackwardCache, double aRCut, double *aFuseWeight,
                        double *aPostFuseWeight, double aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeN = sphSizeN_(WTYPE, NTYPES, NMAX, FSIZE);
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = tSizeN*tLMAll;
    constexpr int tSizeAnlm = PFFLAG ? (PFSIZE*tLMAll) : 0;
    // init cache
    double *tCnlm = aForwardCache;
    double *tAnlm = tCnlm + tSizeCnlm;
    double *tForwardCacheElse = tAnlm + tSizeAnlm;
    double *rGradCnlm = rBackwardCache;
    double *rGradAnlm = rGradCnlm + tSizeCnlm;
    double *rForwardForceCacheElse = rGradAnlm + tSizeAnlm;
    if (CLEAR_CACHE) {
        for (int j = 0; j < aNeiNum; ++j) {
            rGradNlDx[j] = 0.0;
            rGradNlDy[j] = 0.0;
            rGradNlDz[j] = 0.0;
        }
        fill<tSizeAnlm>(rGradAnlm, 0.0);
        fill<tSizeCnlm>(rGradCnlm, 0.0);
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