#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_N>
static NNAP_DEVICE void chebyForwardBatch(int bi, int nb,
        flt_t *aBatchNlDx, flt_t *aBatchNlDy, flt_t *aBatchNlDz, int *aBatchNlType, int aNeiNum, flt_t *rBatchFp,
        flt_t **rBatchForwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // init cache
    flt_t *rBatchRn = *rBatchForwardCache; *rBatchForwardCache += (NMAX+1)*nb;
    // clear fp first
    fillBatch<SIZE_N>(bi, nb, rBatchFp, ZERO);
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        int type = aBatchNlType[j*nb + bi];
        flt_t dx = aBatchNlDx[j*nb + bi];
        flt_t dy = aBatchNlDy[j*nb + bi];
        flt_t dz = aBatchNlDz[j*nb + bi];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fc
        flt_t fc = calFc(dis, aRCut);
        // cal Rn
        calRnBatch<NMAX>(bi, nb, rBatchRn, dis, aRCut);
        // cal fp
        if (WTYPE==WTYPE_FUSE) {
            mplusChebyFuseBatch<NMAX, FSIZE, FSTYLE>(bi, nb, rBatchFp, aFuseWeight, type, fc, rBatchRn);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            mplusChebyExFuseBatch<NMAX, FSIZE, FSTYLE>(bi, nb, rBatchFp, aFuseWeight, type, fc, rBatchRn);
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusBatch<NMAX+1>(bi, nb, rBatchFp, fc, rBatchRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tBatchFp = rBatchFp + (NMAX+1)*(type-1)*nb;
            mplusBatch<NMAX+1>(bi, nb, tBatchFp, fc, rBatchRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tBatchFpWt = rBatchFp + (NMAX+1)*type*nb;
            mplus2Batch<NMAX+1>(bi, nb, rBatchFp, tBatchFpWt, fc, fc, rBatchRn);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tBatchFpWt = rBatchFp + (NMAX+1)*nb;
            mplus2Batch<NMAX+1>(bi, nb, rBatchFp, tBatchFpWt, fc, fc*wt, rBatchRn);
        }
    }
}

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_N>
static NNAP_DEVICE void chebyBackwardBatch(int bi, int nb,
        flt_t *aBatchNlDx, flt_t *aBatchNlDy, flt_t *aBatchNlDz, int *aBatchNlType, int aNeiNum, flt_t *aBatchGradFp,
        flt_t *rBatchGradNlDx, flt_t *rBatchGradNlDy, flt_t *rBatchGradNlDz,
        flt_t **rBatchForwardCache, flt_t **rBatchBackwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // init cache
    flt_t *rBatchRn = *rBatchForwardCache; *rBatchForwardCache += (NMAX+1)*nb;
    flt_t *rBatchRnPx = *rBatchBackwardCache; *rBatchBackwardCache += (NMAX+1)*nb;
    flt_t *rBatchRnPy = *rBatchBackwardCache; *rBatchBackwardCache += (NMAX+1)*nb;
    flt_t *rBatchRnPz = *rBatchBackwardCache; *rBatchBackwardCache += (NMAX+1)*nb;
    flt_t *rBatchCheby2 = *rBatchBackwardCache; *rBatchBackwardCache += (NMAX+1)*nb;
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aBatchNlType[j*nb + bi];
        flt_t dx = aBatchNlDx[j*nb + bi];
        flt_t dy = aBatchNlDy[j*nb + bi];
        flt_t dz = aBatchNlDz[j*nb + bi];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal fcPxyz
        flt_t fcPx, fcPy, fcPz;
        flt_t fc = calFcPxyz(&fcPx, &fcPy, &fcPz, dis, aRCut, dx, dy, dz);
        // cal RnPxyz
        calRnPxyzBatch<NMAX>(bi, nb, rBatchRn, rBatchRnPx, rBatchRnPy, rBatchRnPz, rBatchCheby2, dis, aRCut, dx, dy, dz);
        if (WTYPE==WTYPE_FUSE) {
            flt_t *tBatchGradRn = rBatchCheby2;
            chebyGradRnFuseBatch<NMAX, FSIZE, FSTYLE>(bi, nb, tBatchGradRn, aBatchGradFp, aFuseWeight, type);
            gradRn2xyzBatch<NMAX>(bi, nb,
                j, tBatchGradRn, fc, rBatchRn, fcPx, fcPy, fcPz, rBatchRnPx, rBatchRnPy, rBatchRnPz,
                rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz
            );
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            flt_t *tBatchGradRn = rBatchCheby2;
            chebyGradRnExFuseBatch<NMAX, FSIZE, FSTYLE>(bi, nb, tBatchGradRn, aBatchGradFp, aFuseWeight, type);
            gradRn2xyzBatch<NMAX>(bi, nb,
                j, tBatchGradRn, fc, rBatchRn, fcPx, fcPy, fcPz, rBatchRnPx, rBatchRnPy, rBatchRnPz,
                rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz
            );
        } else
        if (WTYPE==WTYPE_NONE) {
            gradRn2xyzBatch<NMAX>(bi, nb,
                j, aBatchGradFp, fc, rBatchRn, fcPx, fcPy, fcPz, rBatchRnPx, rBatchRnPy, rBatchRnPz,
                rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz
            );
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tBatchGradRn = aBatchGradFp + (NMAX+1)*(type-1)*nb;
            gradRn2xyzBatch<NMAX>(bi, nb,
                j, tBatchGradRn, fc, rBatchRn, fcPx, fcPy, fcPz, rBatchRnPx, rBatchRnPy, rBatchRnPz,
                rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz
            );
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tBatchGradRnWt = aBatchGradFp + (NMAX+1)*type*nb;
            gradRnWt2xyzBatch<NMAX>(bi, nb,
                j, aBatchGradFp, tBatchGradRnWt, fc, rBatchRn, ONE, fcPx, fcPy, fcPz, rBatchRnPx, rBatchRnPy, rBatchRnPz,
                rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz
            );
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tBatchGradRnWt = aBatchGradFp + (NMAX+1)*nb;
            gradRnWt2xyzBatch<NMAX>(bi, nb,
                j, aBatchGradFp, tBatchGradRnWt, fc, rBatchRn, wt, fcPx, fcPy, fcPz, rBatchRnPx, rBatchRnPy, rBatchRnPz,
                rBatchGradNlDx, rBatchGradNlDy, rBatchGradNlDz
            );
        }
    }
}


template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_N, int FULL_CACHE>
static NNAP_DEVICE void chebyForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                                     flt_t **rForwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // init cache
    flt_t *rRn = NULL;
    flt_t *rNlRn = NULL;
    if (FULL_CACHE) {
        rNlRn = *rForwardCache; *rForwardCache += aNeiNum*(NMAX+1);
    } else {
        rRn = *rForwardCache; *rForwardCache += (NMAX+1);
    }
    // clear fp first
    fill<SIZE_N>(rFp, ZERO);
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
        if (FULL_CACHE) rRn = rNlRn + j*(NMAX+1);
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

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_N, int FULL_CACHE>
static NNAP_DEVICE void chebyBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                                      flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                      flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // init cache
    flt_t *tNlRn = *aForwardCache; *aForwardCache += aNeiNum*(NMAX+1);
    flt_t *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    flt_t *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    flt_t *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    if (FULL_CACHE) {
        rNlRnPx = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnPy = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnPz = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlFcPx = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlFcPy = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlFcPz = *rBackwardCache; *rBackwardCache += aNeiNum;
        rCheby2 = *rBackwardCache; *rBackwardCache += (NMAX+1);
    } else {
        rRnPx = *rBackwardCache; *rBackwardCache += (NMAX+1);
        rRnPy = *rBackwardCache; *rBackwardCache += (NMAX+1);
        rRnPz = *rBackwardCache; *rBackwardCache += (NMAX+1);
        rCheby2 = *rBackwardCache; *rBackwardCache += (NMAX+1);
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
        if (WTYPE==WTYPE_FUSE) {
            flt_t *tGradRn = rCheby2;
            chebyGradRnFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            flt_t *tGradRn = rCheby2;
            chebyGradRnExFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradRn2xyz<NMAX>(j, aGradFp, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tGradRn = aGradFp + (NMAX+1)*(type-1);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tNNGradWt = aGradFp + (NMAX+1)*type;
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, ONE, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tNNGradWt = aGradFp + (NMAX+1);
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H