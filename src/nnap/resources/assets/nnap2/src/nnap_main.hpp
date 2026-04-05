#ifndef NNAP_MAIN_H
#define NNAP_MAIN_H

#include "basis_Chebyshev.hpp"
#include "basis_SphericalChebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENX_FP_SIZE__ 84
#define __NNAPGENXX_FP_WTYPE__ JSE_NNAP::WTYPE_DEFAULT
#define __NNAPGENXX_FP_SIZE_N__ 12
#define __NNAPGENXX_FP_NMAX__ 5
#define __NNAPGENXX_FP_LMAX__ 6
#define __NNAPGENXX_FP_NORADIAL__ 0
#define __NNAPGENXX_FP_L3MAX__ 0
#define __NNAPGENXX_FP_L4MAX__ 0
#define __NNAPGENXX_FP_FSIZE__ 0
#define __NNAPGENXX_FP_FSTYLE__ JSE_NNAP::FSTYLE_LIMITED
#define __NNAPGENXX_FP_PFFLAG__ 0
#define __NNAPGENXX_FP_PFSIZE__ 0
#define __NNAPGENXX_FP_SIZE__ 84
#define __NNAPGENXX_FP_SIZE_FW__ 0
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
    sphForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_NORADIAL__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
               __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_SIZE_N__, __NNAPGENXX_FP_PFFLAG__, __NNAPGENXX_FP_PFSIZE__, REQUIRE_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, rSubFp,
        REQUIRE_CACHE?(&rSubFpForwardCache):NULL, tSubFpHyperParam[0], tSubFpParam,
        tSubFpParam+__NNAPGENXX_FP_SIZE_FW__, tSubFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyForward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_FSTYLE__, __NNAPGENXX_FP_SIZE_N__, REQUIRE_CACHE>(
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
static NNAP_DEVICE int normedNnForward(int cType, flt_t *rFp, flt_t *aNormParam, flt_t **aNnParam,
                                       flt_t *rNnGradCache, flt_t *rOutEng) noexcept {
    flt_t tNormMuEng = aNormParam[0];
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
    
// >>> NNAPGEN SWITCH
    flt_t *tNormMu = aNormParam + 2;
    flt_t *tNormSigma = tNormMu + __NNAPGENX_NN_SIZE_IN__;
    // norm fp here
    for (int i = 0; i < __NNAPGENX_NN_SIZE_IN__; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    flt_t *tHiddenWeights = aNnParam[cType-1];
    flt_t *tOutputWeights = tHiddenWeights + __NNAPGENX_NN_SIZE_HW__;
    flt_t *tHiddenBiases = tOutputWeights + __NNAPGENX_NN_SIZE_OW__;
    flt_t tOutputBias = tHiddenBiases[__NNAPGENX_NN_SIZE_HB__];
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    flt_t tEng = nnForward<__NNAPGENS_CTYPE_GEN__, CACHE_GRAD>(
        rFp, tHiddenWeights, NULL, tHiddenBiases, NULL,
        tOutputWeights, tOutputBias, CACHE_GRAD?rNnGradCache:NULL
    );
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedHiddenWeights = aNnParam[__NNAPGENX_NN_SHARED_TYPE__-1];
    flt_t *tSharedHiddenBiases = tSharedHiddenWeights + (__NNAPGENX_NN_SIZE_HW__+__NNAPGENX_NN_SIZE_SHW__ + __NNAPGENX_NN_SIZE_OW__);
    flt_t tEng = nnForward<__NNAPGENS_CTYPE_GEN__, CACHE_GRAD>(
        rFp, tHiddenWeights, tSharedHiddenWeights, tHiddenBiases, tSharedHiddenBiases,
        tOutputWeights, tOutputBias, CACHE_GRAD?rNnGradCache:NULL
    );
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    // denorm energy here
    tEng = tEng*tNormSigmaEng + tNormMuEng;
    *rOutEng = tEng;
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    if (flag) return 1;
    return 0;
}

template <int CTYPE_GEN, int REQUIRE_CACHE>
static NNAP_DEVICE int fpBackward(flt_t *aNlDx, flt_t *aNlDy, flt_t *aNlDz, int *aNlType, int aNeiNum, int cType, flt_t *aGradFp,
                                  flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
                                  flt_t **aFpHyperParam, flt_t **aFpParam, flt_t *aFpForwardCache, flt_t *rFpBackwardCache) noexcept {
    int flag = 1;
// >>> NNAPGEN SWITCH
    flt_t *tSubGradFp = aGradFp;
    flt_t *aSubFpForwardCache = aFpForwardCache;
    flt_t *rSubFpBackwardCache = rFpBackwardCache;
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
    sphBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_LMAX__, __NNAPGENXX_FP_NORADIAL__, __NNAPGENXX_FP_L3MAX__, __NNAPGENXX_FP_L4MAX__,
                __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_SIZE_N__, __NNAPGENXX_FP_PFFLAG__, __NNAPGENXX_FP_PFSIZE__, REQUIRE_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        &aSubFpForwardCache, REQUIRE_CACHE?(&rSubFpBackwardCache):NULL, tSubFpHyperParam[0], tSubFpParam,
        tSubFpParam+__NNAPGENXX_FP_SIZE_FW__, tSubFpHyperParam[1]
    );
// --- NNAPGEN PICK: chebyshev
    chebyBackward<__NNAPGENXX_FP_WTYPE__, __NNAPGENXX_FP_NMAX__, __NNAPGENXX_FP_FSIZE__, __NNAPGENXX_FP_FSTYLE__, __NNAPGENXX_FP_SIZE_N__, REQUIRE_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNeiNum, tSubGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        &aSubFpForwardCache, REQUIRE_CACHE?(&rSubFpBackwardCache):NULL, tSubFpHyperParam[0], tSubFpParam
    );
// <<< NNAPGEN PICK [FP USE __NNAPGENS_X__:__NNAPGENOS_X__]
    tSubGradFp += __NNAPGENXX_FP_SIZE__;
    tSubFpHyperParam += __NNAPGENXX_FP_SIZE_HPARAM__;
    tSubFpParam += __NNAPGENXX_FP_SIZE_PARAM__;
// <<< NNAPGEN REPEAT 0..<[FP MERGE __NNAPGENS_X__]
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [FP TYPE]
    if (flag) return 1;
    return 0;
}

template <int CTYPE_GEN>
static NNAP_DEVICE int normedNnBackward(int cType, flt_t *rGradFp, flt_t *aNormParam, flt_t **aNnParam, flt_t *aNnGradCache, flt_t aInGradEng) noexcept {
    flt_t tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    // denorm energy here
    aInGradEng = aInGradEng*tNormSigmaEng;
    flt_t *tHiddenWeights = aNnParam[cType-1];
    flt_t *tOutputWeights = tHiddenWeights + __NNAPGENX_NN_SIZE_HW__;
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    nnBackward<__NNAPGENS_CTYPE_GEN__>(
        aInGradEng, rGradFp, tHiddenWeights, NULL,
        tOutputWeights, aNnGradCache
    );
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedHiddenWeights = aNnParam[__NNAPGENX_NN_SHARED_TYPE__-1];
    nnBackward<__NNAPGENS_CTYPE_GEN__>(
        aInGradEng, rGradFp, tHiddenWeights, tSharedHiddenWeights,
        tOutputWeights, aNnGradCache
    );
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    flt_t *tNormSigma = aNormParam + (2+__NNAPGENX_NN_SIZE_IN__);
    // denorm fp here
    for (int i = 0; i < __NNAPGENX_NN_SIZE_IN__; ++i) {
        rGradFp[i] /= tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    if (flag) return 1;
    return 0;
}

}

#endif //NNAP_MAIN_H