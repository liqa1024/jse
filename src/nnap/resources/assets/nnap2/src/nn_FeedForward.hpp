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
#define NNAPGENXX_NN_IN_SIZE_H 84
#define NNAPGENXX_NN_OUT_SIZE_H 32
#define NNAPGENX_NN_SIZE_IN 84
#define NNAPGENX_NN_SIZE_HW (84*32)
#define NNAPGENX_NN_SIZE_HB 32
#define NNAPGENX_NN_SIZE_OW 32
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
    flt_t *rY = tX + NNAPGENX_NN_SIZE_IN;
    
// >>> NNAPGEN REPEAT
    nnForwardLayer<NNAPGENXX_NN_IN_SIZE_H, NNAPGENXX_NN_OUT_SIZE_H, CACHE_GRAD>(tX, rY, tWeights, tBiases, tSiLUGrad);
    tWeights += NNAPGENXX_NN_IN_SIZE_H*NNAPGENXX_NN_OUT_SIZE_H;
    tBiases += NNAPGENXX_NN_OUT_SIZE_H;
    tX = rY;
    rY += NNAPGENXX_NN_OUT_SIZE_H;
    if (CACHE_GRAD) {
        tSiLUGrad += NNAPGENXX_NN_OUT_SIZE_H;
    }
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS NNAPGENS_X]
    
    rOut = dot<NNAPGENX_NN_SIZE_OW>(tX, aOutputWeights) + aOutputBias;
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
        fill<NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB>(rGradLayers, ZERO);
    }
    // switch to last layer
    tHiddenWeights += NNAPGENX_NN_SIZE_HW;
    tSiLUGrad += NNAPGENX_NN_SIZE_HB;
    rGradX += (NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB);
    
    // begin backward
    rGradX -= NNAPGENX_NN_SIZE_OW;
    mplus<NNAPGENX_NN_SIZE_OW>(rGradX, aGradY, aOutputWeight);
    flt_t *tGradY = rGradX;
    
// >>> NNAPGEN REPEAT
    tHiddenWeights -= NNAPGENXX_NN_IN_SIZE_H*NNAPGENXX_NN_OUT_SIZE_H;
    tSiLUGrad -= NNAPGENXX_NN_OUT_SIZE_H;
    rGradX -= NNAPGENXX_NN_IN_SIZE_H;
    nnBackwardLayer<NNAPGENXX_NN_IN_SIZE_H, NNAPGENXX_NN_OUT_SIZE_H>(rGradX, tGradY, tHiddenWeights, tSiLUGrad);
    tGradY = rGradX;
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS NNAPGENS_X]<..0

// <<< NNAPGEN SWITCH (CTYPE) [NN TYPE]
}

}

#endif //NN_FEED_FORWARD_H