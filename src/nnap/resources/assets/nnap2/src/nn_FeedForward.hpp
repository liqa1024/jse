#ifndef NN_FEED_FORWARD_H
#define NN_FEED_FORWARD_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

static inline flt_t silu(flt_t aX) noexcept {
    return aX / (ONE + exp(-aX));
}
static inline flt_t siluGrad(flt_t aX, flt_t *rGrad) noexcept {
    flt_t tSigmoid = ONE / (ONE + exp(-aX));
    *rGrad = tSigmoid * (ONE + aX * (ONE - tSigmoid));
    return aX * tSigmoid;
}
static inline flt_t siluGradGrad(flt_t aX, flt_t *rGrad, flt_t *rGradGrad) noexcept {
    flt_t tSigmoid = ONE / (ONE + exp(-aX));
    *rGrad = tSigmoid * (ONE + aX * (ONE - tSigmoid));
    *rGradGrad = tSigmoid * (ONE - tSigmoid) * (TWO + aX * (ONE - tSigmoid - tSigmoid));
    return aX * tSigmoid;
}


// >>> NNAPGEN REMOVE
#define __NNAPGENXX_NN_IN_SIZE__ 84
#define __NNAPGENXX_NN_OUT_SIZE__ 32
#define __NNAPGENXX_NN_SHARE_SIZE__ 0
#define __NNAPGENX_NN_SIZE_IN__ 84
#define __NNAPGENX_NN_SIZE_HW__ (84*32)
#define __NNAPGENX_NN_SIZE_SHW__ 0
#define __NNAPGENX_NN_SIZE_HB__ 32
#define __NNAPGENX_NN_SIZE_SHB__ 0
#define __NNAPGENX_NN_SIZE_OW__ 32
#define __NNAPGENX_NN_SHARED_TYPE__ 1
// <<< NNAPGEN REMOVE

template <int IN_SIZE, int OUT_SIZE, int CACHE_GRAD>
static void nnForwardLayer(flt_t *aX, flt_t *rY, flt_t *aWeights, flt_t *aBiases, flt_t *rSiLUGrad) noexcept {
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
template <int IN_SIZE, int OUT_SIZE, int SHARE_SIZE, int CACHE_GRAD>
static void nnShareForwardLayer(flt_t *aX, flt_t *rY, flt_t *aWeights, flt_t *aSharedWeights, flt_t *aBiases, flt_t *aSharedBiases, flt_t *rSiLUGrad) noexcept {
    constexpr int tNoShareSize = OUT_SIZE - SHARE_SIZE;
    flt_t *tWeights = aWeights;
    for (int j = 0; j < tNoShareSize; ++j) {
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
    // shared last
    tWeights = aSharedWeights + (tNoShareSize*IN_SIZE);
    for (int j = tNoShareSize; j < OUT_SIZE; ++j) {
        const flt_t tDot = dot<IN_SIZE>(aX, tWeights) + aSharedBiases[j];
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

template <int CTYPE, int CACHE_GRAD>
static flt_t nnForward(flt_t *rLayers, flt_t *aHiddenWeights, flt_t *aSharedHiddenWeights, flt_t *aHiddenBiases, flt_t *aSharedHiddenBiases,
                       flt_t *aOutputWeights, flt_t aOutputBias, flt_t *rSiLUGrad) noexcept {
    flt_t *tX = rLayers;
    flt_t *tWeights = aHiddenWeights;
    flt_t *tBiases = aHiddenBiases;
    flt_t *tSiLUGrad = rSiLUGrad;
    
    flt_t rOut;
// >>> NNAPGEN SWITCH
    flt_t *rY = tX + __NNAPGENX_NN_SIZE_IN__;
    
// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
// >>> NNAPGEN REPEAT
    nnForwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, CACHE_GRAD>(tX, rY, tWeights, tBiases, tSiLUGrad);
    tWeights += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tBiases += __NNAPGENXX_NN_OUT_SIZE__;
    tX = rY;
    rY += __NNAPGENXX_NN_OUT_SIZE__;
    if (CACHE_GRAD) {
        tSiLUGrad += __NNAPGENXX_NN_OUT_SIZE__;
    }
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedWeights = aSharedHiddenWeights;
    flt_t *tSharedBiases = aSharedHiddenBiases;
// >>> NNAPGEN REPEAT
    nnShareForwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, __NNAPGENXX_NN_SHARE_SIZE__, CACHE_GRAD>(tX, rY, tWeights, tSharedWeights, tBiases, tSharedBiases, tSiLUGrad);
    tWeights += __NNAPGENXX_NN_IN_SIZE__*(__NNAPGENXX_NN_OUT_SIZE__-__NNAPGENXX_NN_SHARE_SIZE__);
    tSharedWeights += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tBiases += (__NNAPGENXX_NN_OUT_SIZE__-__NNAPGENXX_NN_SHARE_SIZE__);
    tSharedBiases += __NNAPGENXX_NN_OUT_SIZE__;
    tX = rY;
    rY += __NNAPGENXX_NN_OUT_SIZE__;
    if (CACHE_GRAD) {
        tSiLUGrad += __NNAPGENXX_NN_OUT_SIZE__;
    }
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]
    
    rOut = dot<__NNAPGENX_NN_SIZE_OW__>(tX, aOutputWeights) + aOutputBias;
// <<< NNAPGEN SWITCH (CTYPE) [NN TYPE]
    return rOut;
}


template <int IN_SIZE, int OUT_SIZE>
static void nnBackwardLayer(flt_t *rGradX, flt_t *aGradY, flt_t *aWeights, flt_t *aSiLUGrad) noexcept {
    flt_t *tWeights = aWeights;
    for (int j = 0; j < OUT_SIZE; ++j) {
        flt_t tGradZ =  aSiLUGrad[j] * aGradY[j];
        mplus<IN_SIZE>(rGradX, tGradZ, tWeights);
        tWeights += IN_SIZE;
    }
}
template <int IN_SIZE, int OUT_SIZE, int SHARE_SIZE>
static void nnShareBackwardLayer(flt_t *rGradX, flt_t *aGradY, flt_t *aWeights, flt_t *aSharedWeights, flt_t *aSiLUGrad) noexcept {
    constexpr int tNoShareSize = OUT_SIZE - SHARE_SIZE;
    flt_t *tWeights = aWeights;
    for (int j = 0; j < tNoShareSize; ++j) {
        flt_t tGradZ =  aSiLUGrad[j] * aGradY[j];
        mplus<IN_SIZE>(rGradX, tGradZ, tWeights);
        tWeights += IN_SIZE;
    }
    // shared last
    tWeights = aSharedWeights + (tNoShareSize*IN_SIZE);
    for (int j = tNoShareSize; j < OUT_SIZE; ++j) {
        flt_t tGradZ =  aSiLUGrad[j] * aGradY[j];
        mplus<IN_SIZE>(rGradX, tGradZ, tWeights);
        tWeights += IN_SIZE;
    }
}

template <int CTYPE, int CLEAR_CACHE>
static void nnBackward(flt_t aGradY, flt_t *rGradLayers, flt_t *aHiddenWeights, flt_t *aSharedHiddenWeights, flt_t *aOutputWeight, flt_t *aSiLUGrad) noexcept {
    flt_t *tWeights = aHiddenWeights;
    flt_t *tSiLUGrad = aSiLUGrad;
    flt_t *rGradX = rGradLayers;
    
// >>> NNAPGEN SWITCH

// >>> NNAPGEN PICK
// --- NNAPGEN PICK: feed_forward
    if (CLEAR_CACHE) {
        fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rGradLayers, ZERO);
    }
    // switch to last layer
    tWeights += __NNAPGENX_NN_SIZE_HW__;
    tSiLUGrad += __NNAPGENX_NN_SIZE_HB__;
    rGradX += (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    
    // begin backward
    rGradX -= __NNAPGENX_NN_SIZE_OW__;
    mplus<__NNAPGENX_NN_SIZE_OW__>(rGradX, aGradY, aOutputWeight);
    flt_t *tGradY = rGradX;
    
// >>> NNAPGEN REPEAT
    tWeights -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGrad -= __NNAPGENXX_NN_OUT_SIZE__;
    rGradX -= __NNAPGENXX_NN_IN_SIZE__;
    nnBackwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__>(rGradX, tGradY, tWeights, tSiLUGrad);
    tGradY = rGradX;
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS __NNAPGENS_X__]<..0
// --- NNAPGEN PICK: shared_feed_forward
    flt_t *tSharedWeights = aSharedHiddenWeights;
    if (CLEAR_CACHE) {
        fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__+__NNAPGENX_NN_SIZE_SHB__>(rGradLayers, ZERO);
    }
    // switch to last layer
    tWeights += __NNAPGENX_NN_SIZE_HW__;
    tSharedWeights += (__NNAPGENX_NN_SIZE_HW__+__NNAPGENX_NN_SIZE_SHW__);
    tSiLUGrad += (__NNAPGENX_NN_SIZE_HB__+__NNAPGENX_NN_SIZE_SHB__);
    rGradX += (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__+__NNAPGENX_NN_SIZE_SHB__);
    
    // begin backward
    rGradX -= __NNAPGENX_NN_SIZE_OW__;
    mplus<__NNAPGENX_NN_SIZE_OW__>(rGradX, aGradY, aOutputWeight);
    flt_t *tGradY = rGradX;
    
// >>> NNAPGEN REPEAT
    tWeights -= __NNAPGENXX_NN_IN_SIZE__*(__NNAPGENXX_NN_OUT_SIZE__-__NNAPGENXX_NN_SHARE_SIZE__);
    tSharedWeights -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGrad -= __NNAPGENXX_NN_OUT_SIZE__;
    rGradX -= __NNAPGENXX_NN_IN_SIZE__;
    nnShareBackwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, __NNAPGENXX_NN_SHARE_SIZE__>(rGradX, tGradY, tWeights, tSharedWeights, tSiLUGrad);
    tGradY = rGradX;
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS __NNAPGENS_X__]<..0
// <<< NNAPGEN PICK [NN USE __NNAPGENS_X__]

// <<< NNAPGEN SWITCH (CTYPE) [NN TYPE]
}

}

#endif //NN_FEED_FORWARD_H