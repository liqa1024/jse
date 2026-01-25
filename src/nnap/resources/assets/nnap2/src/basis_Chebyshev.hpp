#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

static constexpr int chebySizeFp_(int aWType, int aTypeNum, int aNMax, int aFuseSize) noexcept {
    switch(aWType) {
    case WTYPE_EXFULL:  {return (aTypeNum+1)*(aNMax+1);}
    case WTYPE_FULL:    {return aTypeNum*(aNMax+1);}
    case WTYPE_NONE:    {return aNMax+1;}
    case WTYPE_DEFAULT: {return aNMax+aNMax+2;}
    case WTYPE_FUSE:    {return aFuseSize*(aNMax+1);}
    case WTYPE_EXFUSE:  {return (aFuseSize+1)*(aNMax+1);}
    default:            {return 0;}
    }
}

template <int WTYPE, int NTYPES, int NMAX, int FSIZE, int FSTYLE, int FULL_CACHE>
static void chebyForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                         flt_t *rForwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // const init
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    constexpr int tSizeFp = chebySizeFp_(tWType, NTYPES, NMAX, FSIZE);
    // init cache
    flt_t *rRn = NULL;
    flt_t *rNlRn = NULL, *rNlFc = NULL;
    if (FULL_CACHE) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNeiNum*(NMAX+1);
    } else {
        rRn = rForwardCache;
    }
    // clear fp first
    fill<tSizeFp>(rFp, ZERO);
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
        // cal fp
        if (tWType==WTYPE_FUSE) {
            mplusChebyFuse<NMAX, FSIZE, FSTYLE>(rFp, aFuseWeight, type, fc, rRn);
        } else
        if (tWType==WTYPE_EXFUSE) {
            mplusChebyExFuse<NMAX, FSIZE, FSTYLE>(rFp, aFuseWeight, type, fc, rRn);
        } else
        if (tWType==WTYPE_NONE) {
            mplus<NMAX+1>(rFp, fc, rRn);
        } else
        if (tWType==WTYPE_FULL) {
            flt_t *tFp = rFp + (NMAX+1)*(type-1);
            mplus<NMAX+1>(tFp, fc, rRn);
        } else
        if (tWType==WTYPE_EXFULL) {
            flt_t *tFpWt = rFp + (NMAX+1)*type;
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc, rRn);
        } else
        if (tWType==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tFpWt = rFp + (NMAX+1);
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc*wt, rRn);
        }
    }
}

template <int WTYPE, int NTYPES, int NMAX, int FSIZE, int FSTYLE, int FULL_CACHE, int CLEAR_CACHE>
static void chebyBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                          flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                          flt_t *aForwardCache, flt_t *rBackwardCache, flt_t aRCut, flt_t *aFuseWeight) noexcept {
    // const init
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    // init cache
    flt_t *tNlRn = aForwardCache;
    flt_t *tNlFc = tNlRn + aNeiNum*(NMAX+1);
    flt_t *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    flt_t *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    flt_t *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    if (FULL_CACHE) {
        rNlRnPx = rBackwardCache;
        rNlRnPy = rNlRnPx + aNeiNum*(NMAX+1);
        rNlRnPz = rNlRnPy + aNeiNum*(NMAX+1);
        rNlFcPx = rNlRnPz + aNeiNum*(NMAX+1);
        rNlFcPy = rNlFcPx + aNeiNum;
        rNlFcPz = rNlFcPy + aNeiNum;
        rCheby2 = rNlFcPz + aNeiNum;
    } else {
        rRnPx = rBackwardCache;
        rRnPy = rRnPx + (NMAX+1);
        rRnPz = rRnPy + (NMAX+1);
        rCheby2 = rRnPz + (NMAX+1);
    }
    if (CLEAR_CACHE) {
        for (int j = 0; j < aNeiNum; ++j) {
            rGradNlDx[j] = ZERO;
            rGradNlDy[j] = ZERO;
            rGradNlDz[j] = ZERO;
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
        // get fc Rn
        flt_t fc = tNlFc[j];
        flt_t *tRn = tNlRn + j*(NMAX+1);
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
        if (tWType==WTYPE_FUSE) {
            flt_t *tGradRn = rCheby2;
            chebyGradRnFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_EXFUSE) {
            flt_t *tGradRn = rCheby2;
            chebyGradRnExFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_NONE) {
            gradRn2xyz<NMAX>(j, aGradFp, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_FULL) {
            flt_t *tGradRn = aGradFp + (NMAX+1)*(type-1);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_EXFULL) {
            flt_t *tNNGradWt = aGradFp + (NMAX+1)*type;
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, ONE, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_DEFAULT) {
            flt_t wt = ((type&1)==1) ? ((flt_t)type) : -((flt_t)type);
            flt_t *tNNGradWt = aGradFp + (NMAX+1);
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H