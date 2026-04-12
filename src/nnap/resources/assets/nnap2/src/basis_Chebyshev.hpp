#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void chebyForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                                     flt_t **rForwardCache, flt_t aRCut, flt_t *aParams) noexcept {
    // init cache
    flt_t rRn[NMAX+1];
    // clear fp first
    fill<SIZE_NP>(rFp, ZERO);
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal Rn
        calRn<NMAX>(rRn, dis, aRCut);
        // Rn to fp
        if (WTYPE==WTYPE_RFUSE || WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            mplusRnp<NMAX, SIZE_NP>(rFp, rRn, aParams + (type-1)*(SIZE_NP*(NMAX+1)));
        } else
        if (WTYPE==WTYPE_NONE) {
            plus<NMAX+1>(rFp, rRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tFp = rFp + (type-1)*(NMAX+1);
            plus<NMAX+1>(tFp, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tFpWt = rFp + type*(NMAX+1);
            plusWt<NMAX+1>(rFp, tFpWt, ONE, rRn);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : (-type);
            flt_t *tFpWt = rFp + (NMAX+1);
            plusWt<NMAX+1>(rFp, tFpWt, wt, rRn);
        }
    }
}

template <int WTYPE, int NMAX, int SIZE_NP, int REQUIRE_CACHE>
static NNAP_DEVICE void chebyBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aGradFp,
                                      flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                      flt_t **aForwardCache, flt_t **rBackwardCache, flt_t aRCut, flt_t *aParams) noexcept {
    // init cache
    flt_t bRnGrad[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnGrad = REQUIRE_CACHE ? NULL : bRnGrad;
    flt_t rCheby2[NMAX+1];
    flt_t bRnpGrad[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpGrad = REQUIRE_CACHE ? NULL : bRnpGrad;
    flt_t *rNlRnGrad = NULL, *rNlRnpGrad = NULL;
    if (REQUIRE_CACHE) {
        rNlRnGrad = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnpGrad = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
    }
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal RnGrad
        if (REQUIRE_CACHE) rRnGrad = rNlRnGrad + j*(NMAX+1);
        calRnGrad<NMAX>(rRnGrad, rCheby2, dis, aRCut);
        // RnGrad to grad xyz
        if (WTYPE==WTYPE_RFUSE || WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            // cal RnpGrad here
            if (REQUIRE_CACHE) rRnpGrad = rNlRnpGrad + j*SIZE_NP;
            calRnpGrad<NMAX, SIZE_NP>(rRnpGrad, rRnGrad, aParams + (type-1)*(SIZE_NP*(NMAX+1)));
            gradFp2xyz<SIZE_NP>(j, aGradFp, rRnpGrad, dx, dy, dz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradFp2xyz<NMAX+1>(j, aGradFp, rRnGrad, dx, dy, dz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tGradFp = aGradFp + (type-1)*(NMAX+1);
            gradFp2xyz<NMAX+1>(j, tGradFp, rRnGrad, dx, dy, dz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tGradFpWt = aGradFp + type*(NMAX+1);
            gradFp2xyzWt<NMAX+1>(j, aGradFp, tGradFpWt, ONE, rRnGrad, dx, dy, dz, rGradNlDx, rGradNlDy, rGradNlDz);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : (-type);
            flt_t *tGradFpWt = aGradFp + (NMAX+1);
            gradFp2xyzWt<NMAX+1>(j, aGradFp, tGradFpWt, wt, rRnGrad, dx, dy, dz, rGradNlDx, rGradNlDy, rGradNlDz);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H