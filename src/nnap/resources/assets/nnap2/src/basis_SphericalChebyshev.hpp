#ifndef BASIS_SPHERICAL_CHEBYSHEV_H
#define BASIS_SPHERICAL_CHEBYSHEV_H

#include "basis_SphericalUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int LMAXMAX, int FSIZE>
static void calCnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rCnlm,
                    flt_t aRCut, flt_t *aFuseWeight) noexcept {
    constexpr int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    constexpr int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    flt_t rRn[NMAX+1], rY[tLMAll];
    flt_t rBnlm[(WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) ? tSizeBnlm : 1];
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = std::sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        flt_t fc = calFc(dis, aRCut);
        // cal Rn
        calRn<NMAX>(rRn, dis, aRCut);
        // cal Y
        realSphericalHarmonicsFull4<LMAXMAX>(dx, dy, dz, dis, rY);
        // cal cnlm
        if (WTYPE==WTYPE_FUSE) {
            // cal bnlm
            calBnlm<NMAX, LMAXMAX>(rBnlm, rY, fc, rRn);
            // mplus2cnlm
            mplusCnlmFuse<NMAX, LMAXMAX, FSIZE>(rCnlm, rBnlm, aFuseWeight, type);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            // cal bnlm
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

template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int SIZE_N, int PFFLAG, int PFSIZE>
static void sphForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                       flt_t **rForwardCache, flt_t aRCut, flt_t *aFuseWeight, flt_t *aPostFuseWeight, flt_t aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = SIZE_N*tLMAll;
    constexpr int tSizeAnlm = PFFLAG ? (PFSIZE*tLMAll) : tSizeCnlm;
    // init cache
    flt_t rCnlm[tSizeCnlm] = {};
    flt_t *rAnlm = *rForwardCache; *rForwardCache += tSizeAnlm;
    // do cal
    calCnlm<WTYPE, NMAX, tLMaxMax, FSIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rCnlm, aRCut, aFuseWeight);
    // cnlm -> anlm
    if (PFFLAG) {
        fill<tSizeAnlm>(rAnlm, ZERO);
        mplusAnlm<SIZE_N, tLMaxMax, PFSIZE>(rAnlm, rCnlm, aPostFuseWeight);
        // scale anlm here
        multiply<tSizeAnlm>(rAnlm, aPostFuseScale);
    } else {
        fill<tSizeCnlm>(rAnlm, rCnlm);
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

template <int WTYPE, int NMAX, int LMAXMAX, int FSIZE>
static void backwardCnlm(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradCnlm,
                         flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                         flt_t aRCut, flt_t *aFuseWeight) {
    const int tLMAll = (LMAXMAX+1)*(LMAXMAX+1);
    const int tSizeBnlm = (NMAX+1)*tLMAll;
    // init cache
    flt_t rRn[NMAX+1], rY[tLMAll];
    flt_t rRnPx[NMAX+1], rRnPy[NMAX+1], rRnPz[NMAX+1], rCheby2[NMAX+1];
    flt_t rYPx[tLMAll], rYPy[tLMAll], rYPz[tLMAll], rYPtheta[tLMAll], rYPphi[tLMAll];
    flt_t rGradBnlm[(WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) ? tSizeBnlm : 1];
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = std::sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc Rn Y
        flt_t fc = calFc(dis, aRCut);
        calRn<NMAX>(rRn, dis, aRCut);
        realSphericalHarmonicsFull4<LMAXMAX>(dx, dy, dz, dis, rY);
        // cal fcPxyz
        flt_t fcPx, fcPy, fcPz;
        calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        // cal RnPxyz
        calRnPxyz<NMAX>(rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, dx, dy, dz);
        // cal YlmPxyz
        calYPxyz<LMAXMAX>(rY, dx, dy, dz, dis, rYPx, rYPy, rYPz, rYPtheta, rYPphi);
        // cal fxyz
        if (WTYPE==WTYPE_FUSE) {
            calGradBnlmFuse<NMAX, LMAXMAX, FSIZE>(aGradCnlm, rGradBnlm, aFuseWeight, type);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, rGradBnlm, rYPtheta, rY, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            calGradBnlmExFuse<NMAX, LMAXMAX, FSIZE>(aGradCnlm, rGradBnlm, aFuseWeight, type);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, rGradBnlm, rYPtheta, rY, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradBnlm2xyz<NMAX, LMAXMAX>(j, aGradCnlm, rYPtheta, rY, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tGradBnlm = aGradCnlm + tSizeBnlm*(type-1);
            gradBnlm2xyz<NMAX, LMAXMAX>(j, tGradBnlm, rYPtheta, rY, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tGradCnlmWt = aGradCnlm + tSizeBnlm*type;
            gradCnlmWt2xyz<NMAX, LMAXMAX>(j, aGradCnlm, tGradCnlmWt, rYPtheta, rY, fc, rRn, ONE, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tGradCnlmWt = aGradCnlm + tSizeBnlm;
            gradCnlmWt2xyz<NMAX, LMAXMAX>(j, aGradCnlm, tGradCnlmWt, rYPtheta, rY, fc, rRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rYPx, rYPy, rYPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}
template <int WTYPE, int NMAX, int LMAX, int NORADIAL, int L3MAX, int L4MAX, int FSIZE, int SIZE_N, int PFFLAG, int PFSIZE>
static void sphBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                        flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                        flt_t **aForwardCache, flt_t aRCut, flt_t *aFuseWeight,
                        flt_t *aPostFuseWeight, flt_t aPostFuseScale) noexcept {
    // const init
    constexpr int tSizeL = (NORADIAL?LMAX:(LMAX+1)) + L3NCOLS[L3MAX] + L4NCOLS[L4MAX];
    constexpr int tLMaxMax = LMAX>L3MAX ? (LMAX>L4MAX?LMAX:L4MAX) : (L3MAX>L4MAX?L3MAX:L4MAX);
    constexpr int tLMAll = (tLMaxMax+1)*(tLMaxMax+1);
    constexpr int tSizeCnlm = SIZE_N*tLMAll;
    constexpr int tSizeAnlm = PFFLAG ? (PFSIZE*tLMAll) : tSizeCnlm;
    // init cache
    flt_t *tAnlm = *aForwardCache; *aForwardCache += tSizeAnlm;
    flt_t rGradCnlm[tSizeCnlm] = {};
    flt_t rGradAnlm[tSizeAnlm] = {};
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
    } else {
        fill<tSizeCnlm>(rGradCnlm, rGradAnlm);
    }
    backwardCnlm<WTYPE, NMAX, tLMaxMax, FSIZE>(aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rGradCnlm, rGradNlDx, rGradNlDy, rGradNlDz, aRCut, aFuseWeight);
}

}

#endif //BASIS_SPHERICAL_CHEBYSHEV_H