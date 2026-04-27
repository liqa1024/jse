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

template <int WTYPE, int NMAX, int SIZE_NP, int GRAD_PARAM, int REQUIRE_CACHE>
static NNAP_DEVICE void chebyBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aAGradFp,
                                      flt_t *rAGradNlDx, flt_t *rAGradNlDy, flt_t *rAGradNlDz,
                                      flt_t **aForwardCache, flt_t **rBackwardCache,
                                      flt_t aRCut, flt_t *aParams, flt_t *rAGradParams) noexcept {
    if (GRAD_PARAM) {
        // no param
        if (WTYPE!=WTYPE_RFUSE && WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE) return;
    }
    // init cache
    flt_t bRnGrad[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnGrad = REQUIRE_CACHE ? NULL : bRnGrad;
    flt_t bRnpGrad[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpGrad = REQUIRE_CACHE ? NULL : bRnpGrad;
    flt_t *rNlRnGrad = NULL, *rNlRnpGrad = NULL;
    if (REQUIRE_CACHE) {
        rNlRnGrad = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlRnpGrad = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
    }
    flt_t rCheby2[NMAX+1];
    flt_t *rRn = rCheby2;
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        if (GRAD_PARAM) {
            // cal Rn
            calRn<NMAX>(rRn, dis, aRCut);
        } else {
            // cal RnGrad
            if (REQUIRE_CACHE) rRnGrad = rNlRnGrad + j*(NMAX+1);
            calRnGrad<NMAX>(rRnGrad, rCheby2, dis, aRCut);
        }
        // RnGrad to grad xyz
        flt_t rAGradj = ZERO;
        if (WTYPE==WTYPE_RFUSE || WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            const int tParamShift = (type-1)*(SIZE_NP*(NMAX+1));
            if (GRAD_PARAM) {
                backwardRnp<NMAX, SIZE_NP>(aAGradFp, rRn, rAGradParams+tParamShift);
            } else {
                // cal RnpGrad here
                if (REQUIRE_CACHE) rRnpGrad = rNlRnpGrad + j*SIZE_NP;
                calRnp<NMAX, SIZE_NP>(rRnpGrad, rRnGrad, aParams+tParamShift);
                gradFp2nlj<SIZE_NP>(aAGradFp, rRnpGrad, rAGradj);
            }
        } else
        if (WTYPE==WTYPE_NONE) {
            gradFp2nlj<NMAX+1>(aAGradFp, rRnGrad, rAGradj);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tAGradFp = aAGradFp + (type-1)*(NMAX+1);
            gradFp2nlj<NMAX+1>(tAGradFp, rRnGrad, rAGradj);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tAGradFpWt = aAGradFp + type*(NMAX+1);
            gradFp2nljWt<NMAX+1>(aAGradFp, tAGradFpWt, ONE, rRnGrad, rAGradj);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : (-type);
            flt_t *tAGradFpWt = aAGradFp + (NMAX+1);
            gradFp2nljWt<NMAX+1>(aAGradFp, tAGradFpWt, wt, rRnGrad, rAGradj);
        }
        if (!GRAD_PARAM) {
            rAGradNlDx[j] += rAGradj*dx;
            rAGradNlDy[j] += rAGradj*dy;
            rAGradNlDz[j] += rAGradj*dz;
        }
    }
}

template <int WTYPE, int NMAX, int SIZE_NP>
static NNAP_DEVICE void chebyBackwardBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aAGradFp, flt_t *rBGradAGradFp,
                                              flt_t *aBGradAGradNlDx, flt_t *aBGradAGradNlDy, flt_t *aBGradAGradNlDz,
                                              flt_t **aForwardCache, flt_t **aBackwardCache,
                                              flt_t aRCut, flt_t *aParams, flt_t *rBGradParams) noexcept {
    // init cache
    flt_t *tNlRnGrad = *aBackwardCache; *aBackwardCache += aNeiNum*(NMAX+1);
    flt_t *tNlRnpGrad = *aBackwardCache; *aBackwardCache += aNeiNum*SIZE_NP;
    flt_t rBGradRnpGrad[SIZE_NP];
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get RnGrad
        flt_t *tRnGrad = tNlRnGrad + j*(NMAX+1);
        // grad grad xyz to grad grad fp
        flt_t tBGradAGradj = aBGradAGradNlDx[j]*dx + aBGradAGradNlDy[j]*dy + aBGradAGradNlDz[j]*dz;
        if (WTYPE==WTYPE_RFUSE || WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            const int tParamShift = (type-1)*(SIZE_NP*(NMAX+1));
            // get RnpGrad
            flt_t *tRnpGrad = tNlRnpGrad + j*SIZE_NP;
            gradGradNlj2fp<SIZE_NP>(rBGradAGradFp, tRnpGrad, tBGradAGradj);
            // grad param stuffs
            fill<SIZE_NP>(rBGradRnpGrad, ZERO);
            gradGradNlj2Rnp<SIZE_NP>(aAGradFp, rBGradRnpGrad, tBGradAGradj);
            backwardRnp<SIZE_NP>(rBGradRnpGrad, tRnGrad, rBGradParams+tParamShift);
        } else
        if (WTYPE==WTYPE_NONE) {
            gradGradNlj2fp<NMAX+1>(rBGradAGradFp, tRnGrad, tBGradAGradj);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tBGradAGradFp = rBGradAGradFp + (type-1)*(NMAX+1);
            gradGradNlj2fp<NMAX+1>(tBGradAGradFp, tRnGrad, tBGradAGradj);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tBGradAGradFpWt = rBGradAGradFp + type*(NMAX+1);
            gradGradNlj2fpWt<NMAX+1>(rBGradAGradFp, tBGradAGradFpWt, ONE, tRnGrad, tBGradAGradj);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : (-type);
            flt_t *tBGradAGradFpWt = rBGradAGradFp + (NMAX+1);
            gradGradNlj2fpWt<NMAX+1>(rBGradAGradFp, tBGradAGradFpWt, wt, tRnGrad, tBGradAGradj);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H