#ifndef BASIS_CHEBYSHEV_H
#define BASIS_CHEBYSHEV_H

#include "basis_ChebyshevUtil.hpp"

namespace JSE_NNAP {

template <int WTYPE, int NMAX, int SIZE_NP, int LN, int REQUIRE_CACHE>
static NNAP_DEVICE void chebyForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *rFp,
                                     flt_t **rForwardCache, flt_t aRCut, flt_t *aParams) noexcept {
    // init cache
    flt_t bRn[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRn = REQUIRE_CACHE ? NULL : bRn;
    flt_t bRnp[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnp = REQUIRE_CACHE ? NULL : bRnp;
    flt_t bRnpLN[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rRnpLN = REQUIRE_CACHE ? NULL : bRnpLN;
    flt_t *rNlFc = NULL, *rNlRn = NULL, *rNlRnp = NULL, *rNlRnpLN = NULL, *rNlMuRnp = NULL, *rNlSigmaRnp = NULL;
    if (REQUIRE_CACHE) {
        rNlFc = *rForwardCache; *rForwardCache += aNeiNum;
        rNlRn = *rForwardCache; *rForwardCache += aNeiNum*(NMAX+1);
        rNlRnp = *rForwardCache; *rForwardCache += aNeiNum*SIZE_NP;
        if (LN) {
            rNlRnpLN = *rForwardCache; *rForwardCache += aNeiNum*SIZE_NP;
            rNlMuRnp = *rForwardCache; *rForwardCache += aNeiNum;
            rNlSigmaRnp = *rForwardCache; *rForwardCache += aNeiNum;
        }
    }
    // clear fp first
    fill<SIZE_NP>(rFp, ZERO);
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // cal Rn, fc
        if (REQUIRE_CACHE) rRn = rNlRn + j*(NMAX+1);
        calRn<NMAX>(rRn, dis, aRCut);
        flt_t fc = calFc(dis, aRCut);
        if (REQUIRE_CACHE) rNlFc[j] = fc;
        // Rn to fp
        if (WTYPE==WTYPE_RFUSE || WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            const int tParamShift = (type-1)*(SIZE_NP*(NMAX+1));
            // cal Rnp
            if (REQUIRE_CACHE) rRnp = rNlRnp + j*SIZE_NP;
            calRnp<NMAX, SIZE_NP>(rRnp, rRn, aParams+tParamShift);
            if (LN) {
                if (REQUIRE_CACHE) rRnpLN = rNlRnpLN + j*SIZE_NP;
                flt_t tMuRnp, tSigmaRnp;
                layerNorm<SIZE_NP>(
                    rRnp, rRnpLN, tMuRnp, tSigmaRnp,
                    aParams+tParamShift+(SIZE_NP*(NMAX+1)),
                    aParams+tParamShift+(SIZE_NP*(NMAX+1)+SIZE_NP)
                );
                if (REQUIRE_CACHE) {
                    rNlMuRnp[j] = tMuRnp;
                    rNlSigmaRnp[j] = tSigmaRnp;
                }
                mplusFp<SIZE_NP>(rFp, fc, rRnpLN);
            } else {
                mplusFp<SIZE_NP>(rFp, fc, rRnp);
            }
        } else
        if (WTYPE==WTYPE_NONE) {
            mplusFp<NMAX+1>(rFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tFp = rFp + (type-1)*(NMAX+1);
            mplusFp<NMAX+1>(tFp, fc, rRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tFpWt = rFp + type*(NMAX+1);
            mplusFpWt<NMAX+1>(rFp, tFpWt, ONE, fc, rRn);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : (-type);
            flt_t *tFpWt = rFp + (NMAX+1);
            mplusFpWt<NMAX+1>(rFp, tFpWt, wt, fc, rRn);
        }
    }
}

template <int WTYPE, int NMAX, int SIZE_NP, int LN, int GRAD_PARAM, int USE_BB, int REQUIRE_CACHE>
static NNAP_DEVICE void chebyBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aAGradFp,
                                      flt_t *rAGradNlDx, flt_t *rAGradNlDy, flt_t *rAGradNlDz,
                                      flt_t **aForwardCache, flt_t **rBackwardCache, flt_t **rBackwardBackwardCache,
                                      flt_t aRCut, flt_t *aParams, flt_t *rAGradParams) noexcept {
    static_assert(!(GRAD_PARAM && REQUIRE_CACHE), "INVALID STATE");
    static_assert(!(USE_BB && REQUIRE_CACHE), "INVALID STATE");
    static_assert(!(!GRAD_PARAM && USE_BB), "INVALID STATE");
    if (GRAD_PARAM) {
        // no param
        if (WTYPE!=WTYPE_RFUSE && WTYPE!=WTYPE_FUSE && WTYPE!=WTYPE_EXFUSE) {
            // aForwardCache shift required
            *aForwardCache += aNeiNum;
            *aForwardCache += aNeiNum*(NMAX+1);
            *aForwardCache += aNeiNum*SIZE_NP;
            if (LN) {
                *aForwardCache += aNeiNum*SIZE_NP;
                *aForwardCache += aNeiNum;
                *aForwardCache += aNeiNum;
            }
            if (USE_BB) {
                // rBackwardBackwardCache shift required
                *rBackwardBackwardCache += aNeiNum*SIZE_NP;
                if (LN) {
                    *rBackwardBackwardCache += aNeiNum*SIZE_NP;
                }
            }
            return;
        }
    }
    // init cache
    flt_t *tNlFc = *aForwardCache; *aForwardCache += aNeiNum;
    flt_t *tNlRn = *aForwardCache; *aForwardCache += aNeiNum*(NMAX+1);
    flt_t *tNlRnp = *aForwardCache; *aForwardCache += aNeiNum*SIZE_NP;
    flt_t *tNlRnpLN = NULL, *tNlMuRnp = NULL, *tNlSigmaRnp = NULL;
    if (LN) {
        tNlRnpLN = *aForwardCache; *aForwardCache += aNeiNum*SIZE_NP;
        tNlMuRnp = *aForwardCache; *aForwardCache += aNeiNum;
        tNlSigmaRnp = *aForwardCache; *aForwardCache += aNeiNum;
    }
    flt_t bRnGrad[REQUIRE_CACHE ? 1 : (NMAX+1)]; flt_t *rRnGrad = REQUIRE_CACHE ? NULL : bRnGrad;
    flt_t bAGradRnp[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rAGradRnp = REQUIRE_CACHE ? NULL : bAGradRnp;
    flt_t bAGradRnpLN[REQUIRE_CACHE ? 1 : SIZE_NP]; flt_t *rAGradRnpLN = REQUIRE_CACHE ? NULL : bAGradRnpLN;
    flt_t *rNlFcGrad = NULL, *rNlRnGrad = NULL, *rNlAGradRnp = NULL, *rNlAGradRnpLN = NULL, *rNlAGradSigmaRnp = NULL;
    if (REQUIRE_CACHE) {
        rNlFcGrad = *rBackwardCache; *rBackwardCache += aNeiNum;
        rNlRnGrad = *rBackwardCache; *rBackwardCache += aNeiNum*(NMAX+1);
        rNlAGradRnp = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
        if (LN) {
            rNlAGradRnpLN = *rBackwardCache; *rBackwardCache += aNeiNum*SIZE_NP;
            rNlAGradSigmaRnp = *rBackwardCache; *rBackwardCache += aNeiNum;
        }
    }
    if (USE_BB) {
        rNlAGradRnp = *rBackwardBackwardCache; *rBackwardBackwardCache += aNeiNum*SIZE_NP;
        if (LN) {
            rNlAGradRnpLN = *rBackwardBackwardCache; *rBackwardBackwardCache += aNeiNum*SIZE_NP;
        }
    }
    flt_t rCheby2[NMAX+1], rAGradRn[NMAX+1];
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get Rn, fc
        flt_t *tRn = tNlRn + j*(NMAX+1);
        flt_t fc = tNlFc[j];
        // gradFp to gradRn & gradFc
        flt_t rAGradFc = ZERO;
        fill<NMAX+1>(rAGradRn, ZERO);
        if (WTYPE==WTYPE_RFUSE || WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            const int tParamShift = (type-1)*(SIZE_NP*(NMAX+1));
            // get Rnp
            flt_t *tRnp = tNlRnp + j*SIZE_NP;
            // cache grad Rnp
            if (REQUIRE_CACHE || USE_BB) rAGradRnp = rNlAGradRnp + j*SIZE_NP;
            if (!USE_BB) fill<SIZE_NP>(rAGradRnp, ZERO);
            if (LN) {
                // get RnpLN
                flt_t *tRnpLN = tNlRnpLN + j*SIZE_NP;
                // cache grad RnpLN
                if (REQUIRE_CACHE || USE_BB) rAGradRnpLN = rNlAGradRnpLN + j*SIZE_NP;
                if (!USE_BB) fill<SIZE_NP>(rAGradRnpLN, ZERO);
                backwardMplusFp<SIZE_NP>(aAGradFp, fc, rAGradFc, tRnpLN, rAGradRnpLN);
                // get mu, sigma
                flt_t tMuRnp = tNlMuRnp[j];
                flt_t tSigmaRnp = tNlSigmaRnp[j];
                flt_t rAGradSigmaRnp;
                backwardLayerNorm<SIZE_NP, GRAD_PARAM>(
                    tRnp, rAGradRnp, rAGradRnpLN, tMuRnp, tSigmaRnp, rAGradSigmaRnp,
                    aParams+tParamShift+(SIZE_NP*(NMAX+1)+SIZE_NP),
                    GRAD_PARAM ? (rAGradParams+tParamShift+(SIZE_NP*(NMAX+1))) : NULL,
                    GRAD_PARAM ? rAGradParams+tParamShift+(SIZE_NP*(NMAX+1)+SIZE_NP) : NULL
                );
                if (REQUIRE_CACHE) rNlAGradSigmaRnp[j] = rAGradSigmaRnp;
            } else {
                backwardMplusFp<SIZE_NP>(aAGradFp, fc, rAGradFc, tRnp, rAGradRnp);
            }
            backwardRnp<NMAX, SIZE_NP, GRAD_PARAM, !GRAD_PARAM>(
                rAGradRnp, tRn, rAGradRn,
                aParams+tParamShift,
                GRAD_PARAM ? (rAGradParams+tParamShift) : NULL
            );
        } else
        if (WTYPE==WTYPE_NONE) {
            backwardMplusFp<NMAX+1>(aAGradFp, fc, rAGradFc, tRn, rAGradRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tAGradFp = aAGradFp + (type-1)*(NMAX+1);
            backwardMplusFp<NMAX+1>(tAGradFp, fc, rAGradFc, tRn, rAGradRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tAGradFpWt = aAGradFp + type*(NMAX+1);
            backwardMplusFpWt<NMAX+1>(aAGradFp, tAGradFpWt, ONE, fc, rAGradFc, tRn, rAGradRn);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : (-type);
            flt_t *tAGradFpWt = aAGradFp + (NMAX+1);
            backwardMplusFpWt<NMAX+1>(aAGradFp, tAGradFpWt, wt, fc, rAGradFc, tRn, rAGradRn);
        }
        // gradRn, gradFc to grad xyz
        if (!GRAD_PARAM) {
            // cal RnGrad, fcGrag
            if (REQUIRE_CACHE) rRnGrad = rNlRnGrad + j*(NMAX+1);
            calRnGrad<NMAX>(rRnGrad, rCheby2, dis, aRCut);
            flt_t fcGrad = calFcGrad(dis, aRCut);
            if (REQUIRE_CACHE) rNlFcGrad[j] = fcGrad;
            flt_t rAGradj = dot<NMAX+1>(rAGradRn, rRnGrad);
            rAGradj += rAGradFc*fcGrad;
            rAGradNlDx[j] += rAGradj*dx;
            rAGradNlDy[j] += rAGradj*dy;
            rAGradNlDz[j] += rAGradj*dz;
        }
    }
}

template <int WTYPE, int NMAX, int SIZE_NP, int LN>
static NNAP_DEVICE void chebyBackwardBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, flt_t *aAGradFp, flt_t *rBGradAGradFp,
                                              flt_t *aBGradAGradNlDx, flt_t *aBGradAGradNlDy, flt_t *aBGradAGradNlDz,
                                              flt_t **aForwardCache, flt_t **aBackwardCache, flt_t **rBackwardBackwardCache,
                                              flt_t aRCut, flt_t *aParams, flt_t *rBGradParams) noexcept {
    // init cache
    flt_t *tNlFc = *aForwardCache; *aForwardCache += aNeiNum;
    flt_t *tNlRn = *aForwardCache; *aForwardCache += aNeiNum*(NMAX+1);
    flt_t *tNlRnp = *aForwardCache; *aForwardCache += aNeiNum*SIZE_NP;
    flt_t *tNlRnpLN = NULL, *tNlMuRnp = NULL, *tNlSigmaRnp = NULL;
    if (LN) {
        tNlRnpLN = *aForwardCache; *aForwardCache += aNeiNum*SIZE_NP;
        tNlMuRnp = *aForwardCache; *aForwardCache += aNeiNum;
        tNlSigmaRnp = *aForwardCache; *aForwardCache += aNeiNum;
    }
    flt_t *tNlFcGrad = *aBackwardCache; *aBackwardCache += aNeiNum;
    flt_t *tNlRnGrad = *aBackwardCache; *aBackwardCache += aNeiNum*(NMAX+1);
    flt_t *tNlAGradRnp = *aBackwardCache; *aBackwardCache += aNeiNum*SIZE_NP;
    flt_t *tNlAGradRnpLN = NULL, *tNlAGradSigmaRnp = NULL;
    if (LN) {
        tNlAGradRnpLN = *aBackwardCache; *aBackwardCache += aNeiNum*SIZE_NP;
        tNlAGradSigmaRnp = *aBackwardCache; *aBackwardCache += aNeiNum;
    }
    flt_t *rNlBGradRnp = *rBackwardBackwardCache; *rBackwardBackwardCache += aNeiNum*SIZE_NP;
    flt_t *rNlBGradRnpLN = NULL;
    if (LN) {
        rNlBGradRnpLN = *rBackwardBackwardCache; *rBackwardBackwardCache += aNeiNum*SIZE_NP;
    }
    flt_t rBGradAGradRn[NMAX+1], rBGradAGradRnp[SIZE_NP], rBGradAGradRnpLN[SIZE_NP];
    // loop for neighbor
    for (int j = 0; j < aNeiNum; ++j) {
        // init nl
        int type = aNlType[j];
        flt_t dx = aNlDx[j], dy = aNlDy[j], dz = aNlDz[j];
        flt_t dis = nnap_sqrt(dx*dx + dy*dy + dz*dz);
        // check rcut for merge
        if (dis >= aRCut) continue;
        // get Rn, fc
        flt_t *tRn = tNlRn + j*(NMAX+1);
        flt_t fc = tNlFc[j];
        // get RnGrad, fcGrad
        flt_t *tRnGrad = tNlRnGrad + j*(NMAX+1);
        flt_t fcGrad = tNlFcGrad[j];
        // grad grad xyz to grad grad fc & Rn
        const flt_t tBGradAGradj = aBGradAGradNlDx[j]*dx + aBGradAGradNlDy[j]*dy + aBGradAGradNlDz[j]*dz;
        fill<NMAX+1>(rBGradAGradRn, ZERO);
        flt_t tBGradAGradFc = tBGradAGradj*fcGrad;
        mplus<NMAX+1>(rBGradAGradRn, tBGradAGradj, tRnGrad);
        // grad grad fc & Rn to grad grad fp
        if (WTYPE==WTYPE_RFUSE || WTYPE==WTYPE_FUSE || WTYPE==WTYPE_EXFUSE) {
            const int tParamShift = (type-1)*(SIZE_NP*(NMAX+1));
            // get gradRnp
            flt_t *tAGradRnp = tNlAGradRnp + j*SIZE_NP;
            fill<SIZE_NP>(rBGradAGradRnp, ZERO);
            backwardBackwardRnp<NMAX, SIZE_NP>(
                tAGradRnp, rBGradAGradRnp, rBGradAGradRn,
                aParams+tParamShift, rBGradParams+tParamShift
            );
            // get Rnp
            flt_t *tRnp = tNlRnp + j*SIZE_NP;
            flt_t *rBGradRnp = rNlBGradRnp + j*SIZE_NP;
            fill<SIZE_NP>(rBGradRnp, ZERO);
            if (LN) {
                // get mu, sigma
                flt_t tMuRnp = tNlMuRnp[j];
                flt_t tSigmaRnp = tNlSigmaRnp[j];
                flt_t tAGradSigmaRnp = tNlAGradSigmaRnp[j];
                // get gradRnpLN
                flt_t *tAGradRnpLN = tNlAGradRnpLN + j*SIZE_NP;
                fill<SIZE_NP>(rBGradAGradRnpLN, ZERO);
                backwardBackwardLayerNorm<SIZE_NP>(
                    tRnp, rBGradRnp, rBGradAGradRnp, tAGradRnpLN, rBGradAGradRnpLN,
                    tMuRnp, tSigmaRnp, tAGradSigmaRnp,
                    aParams+tParamShift+(SIZE_NP*(NMAX+1)+SIZE_NP),
                    rBGradParams+tParamShift+(SIZE_NP*(NMAX+1)+SIZE_NP)
                );
                // get RnpLN
                flt_t *tRnpLN = tNlRnpLN + j*SIZE_NP;
                flt_t *rBGradRnpLN = rNlBGradRnpLN + j*SIZE_NP;
                fill<SIZE_NP>(rBGradRnpLN, ZERO);
                backwardBackwardMplusFp<SIZE_NP, TRUE>(aAGradFp, rBGradAGradFp, fc, tBGradAGradFc, tRnpLN, rBGradRnpLN, rBGradAGradRnpLN);
            } else {
                backwardBackwardMplusFp<SIZE_NP, TRUE>(aAGradFp, rBGradAGradFp, fc, tBGradAGradFc, tRnp, rBGradRnp, rBGradAGradRnp);
            }
        } else
        if (WTYPE==WTYPE_NONE) {
            backwardBackwardMplusFp<NMAX+1>(rBGradAGradFp, fc, tBGradAGradFc, tRn, rBGradAGradRn);
        } else
        if (WTYPE==WTYPE_FULL) {
            flt_t *tBGradAGradFp = rBGradAGradFp + (type-1)*(NMAX+1);
            backwardBackwardMplusFp<NMAX+1>(tBGradAGradFp, fc, tBGradAGradFc, tRn, rBGradAGradRn);
        } else
        if (WTYPE==WTYPE_EXFULL) {
            flt_t *tBGradAGradFpWt = rBGradAGradFp + type*(NMAX+1);
            backwardBackwardMplusFpWt<NMAX+1>(rBGradAGradFp, tBGradAGradFpWt, ONE, fc, tBGradAGradFc, tRn, rBGradAGradRn);
        } else
        if (WTYPE==WTYPE_DEFAULT) {
            double wt = ((type&1)==1) ? type : (-type);
            flt_t *tBGradAGradFpWt = rBGradAGradFp + (NMAX+1);
            backwardBackwardMplusFpWt<NMAX+1>(rBGradAGradFp, tBGradAGradFpWt, wt, fc, tBGradAGradFc, tRn, rBGradAGradRn);
        }
    }
}

}

#endif //BASIS_CHEBYSHEV_H