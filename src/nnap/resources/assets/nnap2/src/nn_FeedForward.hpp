#ifndef NN_FEED_FORWARD_H
#define NN_FEED_FORWARD_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

static inline NNAP_DEVICE flt_t silu(flt_t aX) noexcept {
    return aX / (ONE + nnap_exp(-aX));
}
static inline NNAP_DEVICE flt_t siluGrad(flt_t aX, flt_t *rGrad) noexcept {
    flt_t tSigmoid = ONE / (ONE + nnap_exp(-aX));
    *rGrad = tSigmoid * (ONE + aX * (ONE - tSigmoid));
    return aX * tSigmoid;
}
static inline NNAP_DEVICE flt_t siluGradGrad(flt_t aX, flt_t *rGrad, flt_t *rGradGrad) noexcept {
    flt_t tSigmoid = ONE / (ONE + nnap_exp(-aX));
    *rGrad = tSigmoid * (ONE + aX * (ONE - tSigmoid));
    *rGradGrad = tSigmoid * (ONE - tSigmoid) * (TWO + aX * (ONE - tSigmoid - tSigmoid));
    return aX * tSigmoid;
}


// >>> NNAPGEN REMOVE
#define __NNAPGENXX_NN_IN_SIZE__ 84
#define __NNAPGENXX_NN_OUT_SIZE__ 32
#define __NNAPGENX_NN_SIZE_IN__ 84
#define __NNAPGENX_NN_SIZE_CACHEG__ 32
#define __NNAPGENX_NN_SIZE_HMAX__ 32
#define __NNAPGENX_NN_SIZE_HW__ (84*32)
#define __NNAPGENX_NN_SIZE_HB__ 32
#define __NNAPGENX_NN_SIZE_OW__ 32
#define __NNAPGENX_NN_NLAYERS__ 2
#define __NNAPGENOS_X__ 1
// <<< NNAPGEN REMOVE


template <int IN_SIZE, int OUT_SIZE, int CACHE_GRAD>
static NNAP_DEVICE void nnForwardLayer(flt_t *aX, flt_t *rY, flt_t *aWeights, flt_t *aBiases, flt_t *rSiLUGrad) noexcept {
    flt_t *tWeights = aWeights;
    for (int j = 0; j < OUT_SIZE; ++j) {
        const flt_t tDot = dot<IN_SIZE>(aX, tWeights) + aBiases[j];
        tWeights += IN_SIZE;
        if (CACHE_GRAD) {
            flt_t rGrad;
            rY[j] = siluGrad(tDot, &rGrad);
            rSiLUGrad[j] = rGrad;
        } else {
            rY[j] = silu(tDot);
        }
    }
}


template <int CTYPE_GEN, int CACHE_GRAD>
static NNAP_DEVICE flt_t nnForward(flt_t *aX, flt_t *aHiddenWeights, flt_t *aHiddenBiases,
                                   flt_t *aOutputWeights, flt_t aOutputBias, flt_t *rGradCache) noexcept {
    flt_t rOut;
// >>> NNAPGEN SWITCH
    flt_t bHidden1[__NNAPGENX_NN_SIZE_HMAX__];
    flt_t bHidden2[__NNAPGENX_NN_SIZE_HMAX__];
    flt_t *tSubX = aX;
    flt_t *rSubY = bHidden2;
    flt_t *tmp_ = NULL;
    
    flt_t *tWeights = aHiddenWeights;
    flt_t *tBiases = aHiddenBiases;
    flt_t *rSiLUGrad = rGradCache;
    
// >>> NNAPGEN REPEAT
    nnForwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, CACHE_GRAD>(tSubX, rSubY, tWeights, tBiases, rSiLUGrad);
    tWeights += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tBiases += __NNAPGENXX_NN_OUT_SIZE__;
    if (__NNAPGENOS_X__==0) {
        tSubX = rSubY;
        rSubY = bHidden1;
    } else {
        // swap x & y
        tmp_ = tSubX;
        tSubX = rSubY;
        rSubY = tmp_;
    }
    if (CACHE_GRAD) {
        rSiLUGrad += __NNAPGENXX_NN_OUT_SIZE__;
    }
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
    
    rOut = dot<__NNAPGENX_NN_SIZE_OW__>(tSubX, aOutputWeights) + aOutputBias;
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    return rOut;
}


template <int IN_SIZE, int OUT_SIZE>
static NNAP_DEVICE void nnBackwardLayer(flt_t *rGradX, flt_t *aGradY, flt_t *aWeights, flt_t *aSiLUGrad) noexcept {
    flt_t *tWeights = aWeights;
    for (int j = 0; j < OUT_SIZE; ++j) {
        flt_t tGradZ =  aSiLUGrad[j] * aGradY[j];
        mplus<IN_SIZE>(rGradX, tGradZ, tWeights);
        tWeights += IN_SIZE;
    }
}

template <int CTYPE_GEN>
static NNAP_DEVICE void nnBackward(flt_t aGradY, flt_t *rGradX, flt_t *aHiddenWeights,
                                   flt_t *aOutputWeight, flt_t *aGradCache) noexcept {
// >>> NNAPGEN SWITCH
    flt_t bHidden1[__NNAPGENX_NN_SIZE_HMAX__] = {0};
    flt_t bHidden2[__NNAPGENX_NN_SIZE_HMAX__] = {0};
    flt_t *rSubGradX = bHidden2;
    flt_t *tSubGradY = NULL;
    flt_t *tmp_ = NULL;
    flt_t *tWeights = aHiddenWeights;
    flt_t *tSiLUGrad = aGradCache;
    
    // switch to last layer
    tWeights += __NNAPGENX_NN_SIZE_HW__;
    tSiLUGrad += __NNAPGENX_NN_SIZE_CACHEG__;
    
    // begin backward
    mplus<__NNAPGENX_NN_SIZE_OW__>(rSubGradX, aGradY, aOutputWeight);
// >>> NNAPGEN REPEAT
    tWeights -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGrad -= __NNAPGENXX_NN_OUT_SIZE__;
    if (__NNAPGENOS_X__==0) {
        tSubGradY = rSubGradX;
        rSubGradX = rGradX;
    } else {
        if (__NNAPGENOS_X__==__NNAPGENX_NN_NLAYERS__-1) {
            tSubGradY = rSubGradX;
            rSubGradX = bHidden1;
        } else {
            // swap x & y
            tmp_ = tSubGradY;
            tSubGradY = rSubGradX;
            rSubGradX = tmp_;
            fill<__NNAPGENXX_NN_IN_SIZE__>(rSubGradX, ZERO);
        }
    }
    nnBackwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__>(rSubGradX, tSubGradY, tWeights, tSiLUGrad);
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS __NNAPGENS_X__]<..0

// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
}

}

#endif //NN_FEED_FORWARD_H