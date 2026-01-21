#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int FSIZE, int FSTYLE, int SIZE_FP, int CACHE_LEVEL>
static void chebyForward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNN,
                         double *rFp, double *rForwardCache,
                         double aRCut, double *aFuseWeight) noexcept {
    // init cache
    double *rRn = NULL;
    double *rNlRn = NULL, *rNlFc = NULL;
    if (CACHE_LEVEL>0) {
        rNlRn = rForwardCache;
        rNlFc = rNlRn + aNN*(NMAX+1);
    } else {
        rRn = rForwardCache;
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
        if (CACHE_LEVEL>0) rNlFc[j] = fc;
        // cal Rn
        if (CACHE_LEVEL>0) rRn = rNlRn + j*(NMAX+1);
        calRn<NMAX>(rRn, dis, aRCut);
        // cal fp
        if (WTYPE==WTYPE_FUSE) {
            mplusFpFuse<NMAX, FSIZE, FSTYLE>(rFp, aFuseWeight, type, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFUSE) {
            mplusFpExFuse<NMAX, FSIZE, FSTYLE>(rFp, aFuseWeight, type, fc, rRn);
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

}

#endif //BASIS_CHEBYSHEV_H