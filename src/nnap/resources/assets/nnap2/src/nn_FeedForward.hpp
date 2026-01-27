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
#define __NNAPGENXX_NN_IN_SIZE_H__ 84
#define __NNAPGENXX_NN_OUT_SIZE_H__ 32
#define __NNAPGENX_NN_SIZE_IN__ 84
#define __NNAPGENX_NN_SIZE_HW__ (84*32)
#define __NNAPGENX_NN_SIZE_HB__ 32
#define __NNAPGENX_NN_SIZE_OW__ 32
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

template <int CTYPE, int CACHE_GRAD>
static flt_t nnForward(flt_t *rLayers, flt_t *aHiddenWeights, flt_t *aHiddenBiases, flt_t *aOutputWeights, flt_t aOutputBias, flt_t *rSiLUGrad) noexcept {
    flt_t *tX = rLayers;
    flt_t *tWeights = aHiddenWeights;
    flt_t *tBiases = aHiddenBiases;
    flt_t *tSiLUGrad = rSiLUGrad;
    
    flt_t rOut;
// >>> NNAPGEN SWITCH
    flt_t *rY = tX + __NNAPGENX_NN_SIZE_IN__;
    
// >>> NNAPGEN REPEAT
    nnForwardLayer<__NNAPGENXX_NN_IN_SIZE_H__, __NNAPGENXX_NN_OUT_SIZE_H__, CACHE_GRAD>(tX, rY, tWeights, tBiases, tSiLUGrad);
    tWeights += __NNAPGENXX_NN_IN_SIZE_H__*__NNAPGENXX_NN_OUT_SIZE_H__;
    tBiases += __NNAPGENXX_NN_OUT_SIZE_H__;
    tX = rY;
    rY += __NNAPGENXX_NN_OUT_SIZE_H__;
    if (CACHE_GRAD) {
        tSiLUGrad += __NNAPGENXX_NN_OUT_SIZE_H__;
    }
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
    
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

template <int CTYPE, int CLEAR_CACHE>
static void nnBackward(flt_t aGradY, flt_t *rGradLayers, flt_t *aHiddenWeights, flt_t *aOutputWeight, flt_t *aSiLUGrad) noexcept {
    flt_t *tHiddenWeights = aHiddenWeights;
    flt_t *tSiLUGrad = aSiLUGrad;
    flt_t *rGradX = rGradLayers;
    
// >>> NNAPGEN SWITCH
    if (CLEAR_CACHE) {
        fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rGradLayers, ZERO);
    }
    // switch to last layer
    tHiddenWeights += __NNAPGENX_NN_SIZE_HW__;
    tSiLUGrad += __NNAPGENX_NN_SIZE_HB__;
    rGradX += (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    
    // begin backward
    rGradX -= __NNAPGENX_NN_SIZE_OW__;
    mplus<__NNAPGENX_NN_SIZE_OW__>(rGradX, aGradY, aOutputWeight);
    flt_t *tGradY = rGradX;
    
// >>> NNAPGEN REPEAT
    tHiddenWeights -= __NNAPGENXX_NN_IN_SIZE_H__*__NNAPGENXX_NN_OUT_SIZE_H__;
    tSiLUGrad -= __NNAPGENXX_NN_OUT_SIZE_H__;
    rGradX -= __NNAPGENXX_NN_IN_SIZE_H__;
    nnBackwardLayer<__NNAPGENXX_NN_IN_SIZE_H__, __NNAPGENXX_NN_OUT_SIZE_H__>(rGradX, tGradY, tHiddenWeights, tSiLUGrad);
    tGradY = rGradX;
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS __NNAPGENS_X__]<..0

// <<< NNAPGEN SWITCH (CTYPE) [NN TYPE]
}

}

#endif //NN_FEED_FORWARD_H