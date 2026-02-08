#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_N>
static void chebyForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                         flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // init cache
    flt_t rRn[NMAX+1];
    // clear fp first
    fill<SIZE_N>(rFp, ZERO);
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
        // cal fp
        if (WTYPE==WTYPE_FUSE) {
            mplusChebyFuse<NMAX, FSIZE, FSTYLE>(rFp, aFuseWeight, type, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            mplusChebyExFuse<NMAX, FSIZE, FSTYLE>(rFp, aFuseWeight, type, fc, rRn);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplus<NMAX+1>(rFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tFp = rFp + (NMAX+1)*(type-1);
            mplus<NMAX+1>(tFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tFpWt = rFp + (NMAX+1)*type;
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc, rRn);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tFpWt = rFp + (NMAX+1);
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc*wt, rRn);
        }
    }
}

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_N>
static void chebyBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                          flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                          flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // init cache
    flt_t rRn[NMAX+1];
    flt_t rRnPx[NMAX+1], rRnPy[NMAX+1], rRnPz[NMAX+1], rCheby2[NMAX+1];
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = std::sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fcPxyz
        flt_t fcPx, fcPy, fcPz;
        flt_t fc = calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        // cal RnPxyz
        calRnPxyz<NMAX>(rRn, rRnPx, rRnPy, rRnPz, rCheby2, dis, aRCut, dx, dy, dz);
        if (WTYPE==WTYPE_FUSE) {
            flt_t *tGradRn = rCheby2;
            chebyGradRnFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            flt_t *tGradRn = rCheby2;
            chebyGradRnExFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradRn2xyz<NMAX>(j, aGradFp, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tGradRn = aGradFp + (NMAX+1)*(type-1);
            gradRn2xyz<NMAX>(j, tGradRn, fc, rRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tNNGradWt = aGradFp + (NMAX+1)*type;
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, rRn, ONE, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tNNGradWt = aGradFp + (NMAX+1);
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, rRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H