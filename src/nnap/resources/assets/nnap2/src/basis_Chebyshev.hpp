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
};

template <int WTYPE, int NTYPES, int NMAX, int FSIZE, int FSTYLE, int FULL_CACHE>
static void chebyForward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, double *rFp,
                         double *rForwardCache, double aRCut, double *aFuseWeight) noexcept {
    // const init
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    constexpr int tSizeFp = chebySizeFp_(tWType, NTYPES, NMAX, FSIZE);
    // init cache
    double *rRn = NULL;
    double *rNlRn = NULL, *rNlFc = NULL;
    if (FULL_CACHE) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNeiNum*(NMAX+1);
    } else {
        rRn = rForwardCache;
    }
    // clear fp first
    fill<tSizeFp>(rFp, 0.0);
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
            double *tFp = rFp + (NMAX+1)*(type-1);
            mplus<NMAX+1>(tFp, fc, rRn);
        } else
        if (tWType==WTYPE_EXFULL) {
            double *tFpWt = rFp + (NMAX+1)*type;
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc, rRn);
        } else
        if (tWType==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : -type;
            double *tFpWt = rFp + (NMAX+1);
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc*wt, rRn);
        }
    }
}

template <int WTYPE, int NTYPES, int NMAX, int FSIZE, int FSTYLE, int FULL_CACHE, int CLEAR_CACHE>
static void chebyBackward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNeiNum, double *aGradFp,
                          double *rGradNlDx, double *rGradNlDy, double *rGradNlDz,
                          double *aForwardCache, double *rBackwardCache, double aRCut, double *aFuseWeight) noexcept {
    // const init
    constexpr int tWType = toInternalWType(WTYPE, NTYPES);
    if (CLEAR_CACHE) {
        for (int j = 0; j < aNeiNum; ++j) {
            rGradNlDx[j] = 0.0;
            rGradNlDy[j] = 0.0;
            rGradNlDz[j] = 0.0;
        }
    }
    // init cache
    double *tNlRn = aForwardCache;
    double *tNlFc = tNlRn + aNeiNum*(NMAX+1);
    double *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    double *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    double *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
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
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        double dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        double dis = sqrt((double)(dx*dx + dy*dy + dz*dz));
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get fc Rn
        double fc = tNlFc[j];
        double *tRn = tNlRn + j*(NMAX+1);
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
        if (tWType==WTYPE_FUSE) {
            double *tGradRn = rCheby2;
            chebyGradRnFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_EXFUSE) {
            double *tGradRn = rCheby2;
            chebyGradRnExFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_NONE) {
            gradRn2xyz<NMAX>(j, aGradFp, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_FULL) {
            double *tGradRn = aGradFp + (NMAX+1)*(type-1);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_EXFULL) {
            double *tNNGradWt = aGradFp + (NMAX+1)*type;
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, 1.0, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (tWType==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : -type;
            double *tNNGradWt = aGradFp + (NMAX+1);
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H