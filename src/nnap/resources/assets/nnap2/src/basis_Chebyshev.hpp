#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_FP, int FULL_CACHE>
static void chebyForward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNN, double *rFp,
                         double *rCache, double aRCut, double *aFuseWeight) noexcept {
    // init cache
    double *rRn = NULL;
    double *rNlRn = NULL, *rNlFc = NULL;
    if (FULL_CACHE) {
        rNlRn = rCache;
        rNlFc = rNlRn + aNN*(NMAX+1);
    } else {
        rRn = rCache;
    }
    // clear fp first
    fill<SIZE_FP>(rFp, 0.0);
    // loop for neighbor
    for (int j = 0; j < aNN; ++j) {
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
            double *tFp = rFp + (NMAX+1)*(type-1);
            mplus<NMAX+1>(tFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            double *tFpWt = rFp + (NMAX+1)*type;
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc, rRn);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : -type;
            double *tFpWt = rFp + (NMAX+1);
            mplus2<NMAX+1>(rFp, tFpWt, fc, fc*wt, rRn);
        }
    }
}

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int FULL_CACHE, int CLEAR_CACHE>
static void chebyBackward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNN, double *aGradFp,
                          double *rGradNlDx, double *rGradNlDy, double *rGradNlDz,
                          double *rCache, double aRCut, double *aFuseWeight) noexcept {
    if (CLEAR_CACHE) {
        for (int j = 0; j < aNN; ++j) {
            rGradNlDx[j] = 0.0;
            rGradNlDy[j] = 0.0;
            rGradNlDz[j] = 0.0;
        }
    }
    // init cache
    double *tNlRn = rCache;
    double *tNlFc = tNlRn + aNN*(NMAX+1);
    double *rRnPx = NULL, *rRnPy = NULL, *rRnPz = NULL, *rCheby2 = NULL;
    double *rNlRnPx = NULL, *rNlRnPy = NULL, *rNlRnPz = NULL;
    double *rNlFcPx = NULL, *rNlFcPy = NULL, *rNlFcPz = NULL;
    if (FULL_CACHE) {
        rNlRnPx = tNlFc + aNN;
        rNlRnPy = rNlRnPx + aNN*(NMAX+1);
        rNlRnPz = rNlRnPy + aNN*(NMAX+1);
        rNlFcPx = rNlRnPz + aNN*(NMAX+1);
        rNlFcPy = rNlFcPx + aNN;
        rNlFcPz = rNlFcPy + aNN;
        rCheby2 = rNlFcPz + aNN;
    } else {
        rRnPx = tNlFc + aNN;
        rRnPy = rRnPx + (NMAX+1);
        rRnPz = rRnPy + (NMAX+1);
        rCheby2 = rRnPz + (NMAX+1);
    }
    // loop for neighbor
    for (int j = 0; j < aNN; ++j) {
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
        if (WTYPE==WTYPE_FUSE) {
            double *tGradRn = rCheby2;
            chebyGradRnFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            double *tGradRn = rCheby2;
            chebyGradRnExFuse<NMAX, FSIZE, FSTYLE>(tGradRn, aGradFp, aFuseWeight, type);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradRn2xyz<NMAX>(j, aGradFp, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            double *tGradRn = aGradFp + (NMAX+1)*(type-1);
            gradRn2xyz<NMAX>(j, tGradRn, fc, tRn, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            double *tNNGradWt = aGradFp + (NMAX+1)*type;
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, 1.0, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : -type;
            double *tNNGradWt = aGradFp + (NMAX+1);
            gradRnWt2xyz<NMAX>(j, aGradFp, tNNGradWt, fc, tRn, wt, fcPx, fcPy, fcPz, rRnPx, rRnPy, rRnPz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H