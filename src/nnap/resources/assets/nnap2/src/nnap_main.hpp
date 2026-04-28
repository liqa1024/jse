#ifndef NNAP_MAIN_H
#define NNAP_MAIN_H

#include "basis_Chebyshev.hpp"
#include "basis_SphericalChebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENX_FP_SIZE__ 84
#define __NNAPGENXX_FP_WTYPE__ JSE_NNAP::WTYPE_RFUSE
#define __NNAPGENXX_FP_SIZE_NP__ 8
#define __NNAPGENXX_FP_NMAX__ 5
#define __NNAPGENXX_FP_LMAX__ 6
#define __NNAPGENXX_FP_L3MAX__ 0
#define __NNAPGENXX_FP_L4MAX__ 0
#define __NNAPGENXX_FP_SIZE__ 84
#define __NNAPGENXX_FP_SIZE_HPARAM__ 2
#define __NNAPGENXX_FP_SIZE_PARAM__ 0
#define __NNAPGENX_FP_SHARED_TYPE__ 1
#define __NNAPGENS_CTYPE_GEN__ 1
#define __NNAPGENS_ctype__ 1
// <<< NNAPGEN REMOVE


namespace JSE_NNAP {

template <int CTYPE_GEN, int REQUIRE_CACHE>
static NNAP_DEVICE int fpForward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int cType, flt_t *rFp,
                                 flt_t **aFpHyperParam, flt_t **aFpParam, flt_t *rFpForwardCache) noexcept {
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *rSubFp = rFp;
    flt_t *rSubFpForwardCache = rFpForwardCache;
// >>> NNAPGEN IF
// --- NNAPGEN HAS: [FP SHARE __NNAPGENS_X__]
    flt_t *tSubFpHyperParam = aFpHyperParam[__NNAPGENX_FP_SHARED_TYPE__-1];
    flt_t *tSubFpParam = aFpParam[__NNAPGENX_FP_SHARED_TYPE__-1];
// --- NNAPGEN ELSE:
    flt_t *tSubFpHyperParam = aFpHyperParam[cType-1];
    flt_t *tSubFpParam = aFpParam[cType-1];
// <<< NNAPGEN IF
// >>> NNAPGEN REPEAT
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
               __NNAPGENXX_FP_SIZE_NP__, REQUIRE_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rSubFp,
        REQUIRE_CACHE?(&rSubFpForwardCache):NULL, tSubFpHyperParam[0], tSubFpParam
    );
// --- NNAPGEN PICK: chebyshev
    chebyForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_SIZE_NP__, REQUIRE_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rSubFp,
        REQUIRE_CACHE?(&rSubFpForwardCache):NULL, tSubFpHyperParam[0], tSubFpParam
    );
// <<< NNAPGEN PICK [FP USE __NNAPGENS_X__:__NNAPGENOS_X__]
    rSubFp += __NNAPGENXX_FP_SIZE__;
    tSubFpHyperParam += __NNAPGENXX_FP_SIZE_HPARAM__;
    tSubFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
// <<< NNAPGEN REPEAT 0..<[FP MERGE __NNAPGENS_X__]
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [FP TYPE]
    if (flag) return 1;
    return 0;
}

template <int CTYPE_GEN, int CACHE_GRAD>
static NNAP_DEVICE int normedNnForward(int cType, flt_t *rLayers, flt_t *aNormParam, flt_t **aNnParam,
                                       flt_t *rNnGradCache, flt_t *rOut) noexcept {
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *tNormMu = aNormParam;
    flt_t *tNormSigma = tNormMu + __NNAPGENX_NN_SIZE_IN__;
    // norm fp here
    for (int i = 0; i < __NNAPGENX_NN_SIZE_IN__; ++i) {
        rLayers[i] = (rLayers[i] - tNormMu[i]) / tNormSigma[i];
    }
    flt_t *tWeights = aNnParam[cType-1];
    flt_t *tBiases = tWeights + (__NNAPGENX_NN_SIZE_HW__+__NNAPGENX_NN_SIZE_OW__);
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    rOut[0] = nnForward<__NNAPGENS_CTYPE_GEN__, CACHE_GRAD>(
        rLayers, tWeights, tBiases, CACHE_GRAD?rNnGradCache:NULL
    );
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    if (flag) return 1;
    return 0;
}

template <int CTYPE_GEN, int GRAD_PARAM>
static NNAP_DEVICE int normedNnBackward(int cType, flt_t *aLayers, flt_t *rGradLayers, flt_t *aNormParam, flt_t **aNnParam,
                                        flt_t **rGradNnParam, flt_t *aNnGradCache, flt_t aInGrad) noexcept {
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *tWeights = aNnParam[cType-1];
    flt_t *rGradWeights = GRAD_PARAM ? rGradNnParam[cType-1] : NULL;
    flt_t *rGradBiases = GRAD_PARAM ? (rGradWeights + (__NNAPGENX_NN_SIZE_HW__+__NNAPGENX_NN_SIZE_OW__)) : NULL;
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    nnBackward<__NNAPGENS_CTYPE_GEN__, GRAD_PARAM>(
        aInGrad, aLayers, rGradLayers, tWeights,
        rGradWeights, rGradBiases, aNnGradCache
    );
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    flt_t *tNormSigma = aNormParam + __NNAPGENX_NN_SIZE_IN__;
    // denorm fp here
    for (int i = 0; i < __NNAPGENX_NN_SIZE_IN__; ++i) {
        rGradLayers[i] /= tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    if (flag) return 1;
    return 0;
}

template <int CTYPE_GEN, int GRAD_PARAM, int USE_BB, int REQUIRE_CACHE>
static NNAP_DEVICE int fpBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int cType, flt_t *aGradFp,
                                  flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz, flt_t **aFpHyperParam,
                                  flt_t **aFpParam, flt_t **rGradFpParam, flt_t *aFpForwardCache, flt_t *rFpBackwardCache, flt_t *rFpBackwardBackwardCache) noexcept {
    static_assert(!(GRAD_PARAM && REQUIRE_CACHE), "INVALID STATE");
    static_assert(!(USE_BB && REQUIRE_CACHE), "INVALID STATE");
    static_assert(!(!GRAD_PARAM && USE_BB), "INVALID STATE");
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *tSubGradFp = aGradFp;
    flt_t *aSubFpForwardCache = aFpForwardCache;
    flt_t *rSubFpBackwardCache = rFpBackwardCache;
    flt_t *rSubFpBackwardBackwardCache = rFpBackwardBackwardCache;
// >>> NNAPGEN IF
// --- NNAPGEN HAS: [FP SHARE __NNAPGENS_X__]
    flt_t *tSubFpHyperParam = aFpHyperParam[__NNAPGENX_FP_SHARED_TYPE__-1];
    flt_t *tSubFpParam = aFpParam[__NNAPGENX_FP_SHARED_TYPE__-1];
    flt_t *rSubGradFpParam = GRAD_PARAM ? rGradFpParam[__NNAPGENX_FP_SHARED_TYPE__-1] : NULL;
// --- NNAPGEN ELSE:
    flt_t *tSubFpHyperParam = aFpHyperParam[cType-1];
    flt_t *tSubFpParam = aFpParam[cType-1];
    flt_t *rSubGradFpParam = GRAD_PARAM ? rGradFpParam[cType-1] : NULL;
// <<< NNAPGEN IF
// >>> NNAPGEN REPEAT
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: spherical_chebyshev
    sphBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
                __NNAPGENXX_FP_SIZE_NP__, GRAD_PARAM, USE_BB, REQUIRE_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        &aSubFpForwardCache, REQUIRE_CACHE?(&rSubFpBackwardCache):NULL, USE_BB?(&rSubFpBackwardBackwardCache):NULL,
        tSubFpHyperParam[0], tSubFpParam, rSubGradFpParam
    );
// --- NNAPGEN PICK: chebyshev
    chebyBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_SIZE_NP__, GRAD_PARAM, USE_BB, REQUIRE_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        &aSubFpForwardCache, REQUIRE_CACHE?(&rSubFpBackwardCache):NULL, USE_BB?(&rSubFpBackwardBackwardCache):NULL,
        tSubFpHyperParam[0], tSubFpParam, rSubGradFpParam
    );
// <<< NNAPGEN PICK [FP USE __NNAPGENS_X__:__NNAPGENOS_X__]
    tSubGradFp += __NNAPGENXX_FP_SIZE__;
    tSubFpHyperParam += __NNAPGENXX_FP_SIZE_HPARAM__;
    tSubFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
    if (GRAD_PARAM) {
        rSubGradFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
    }
// <<< NNAPGEN REPEAT 0..<[FP MERGE __NNAPGENS_X__]
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [FP TYPE]
    if (flag) return 1;
    return 0;
}

}

#endif //NNAP_MAIN_H