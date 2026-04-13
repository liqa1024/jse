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
#define __NNAPGENX_NN_SIZE_HW__ (84*32)
#define __NNAPGENX_NN_SIZE_HB__ 32
#define __NNAPGENX_NN_SIZE_OW__ 32
#define __NNAPGENX_NN_NLAYERS__ 2
#define __NNAPGENOS_X__ 1
// <<< NNAPGEN REMOVE


template <int IN_SIZE, int OUT_SIZE, int CACHE_GRAD>
static NNAP_DEVICE void nnForwardLayer(flt_t *aX, flt_t *rY, flt_t *aWeight, flt_t *aBias, flt_t *rSiLUGrad) noexcept {
    flt_t *tWeight = aWeight;
    for (int j = 0; j < OUT_SIZE; ++j) {
        const flt_t tDot = dot<IN_SIZE>(aX, tWeight) + aBias[j];
        tWeight += IN_SIZE;
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
static NNAP_DEVICE flt_t nnForward(flt_t *rLayers, flt_t *aWeights, flt_t *aBiases, flt_t *rGradCache) noexcept {
    flt_t rOut;
// >>> NNAPGEN SWITCH
    flt_t *tSubX = rLayers;
    flt_t *rSubY = rLayers + __NNAPGENX_NN_SIZE_IN__;
    
    flt_t *tWeight = aWeights;
    flt_t *tBias = aBiases;
    flt_t *rSiLUGrad = rGradCache;
    
// >>> NNAPGEN REPEAT
    nnForwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, CACHE_GRAD>(tSubX, rSubY, tWeight, tBias, rSiLUGrad);
    tWeight += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tBias += __NNAPGENXX_NN_OUT_SIZE__;
    tSubX = rSubY;
    rSubY += __NNAPGENXX_NN_OUT_SIZE__;
    if (CACHE_GRAD) rSiLUGrad += __NNAPGENXX_NN_OUT_SIZE__;
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
    
    rOut = dot<__NNAPGENX_NN_SIZE_OW__>(tSubX, tWeight) + tBias[0];
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    return rOut;
}


template <int IN_SIZE, int OUT_SIZE, int GRAD_PARAM>
static NNAP_DEVICE void nnBackwardLayer(flt_t *aX, flt_t *rGradX, flt_t *aGradY, flt_t *aWeight,
                                        flt_t *rGradWeight, flt_t *rGradBias, flt_t *aSiLUGrad) noexcept {
    flt_t *tWeight = aWeight;
    flt_t *tGradWeight = GRAD_PARAM ? rGradWeight : NULL;
    for (int j = 0; j < OUT_SIZE; ++j) {
        flt_t tGradZ =  aSiLUGrad[j] * aGradY[j];
        mplus<IN_SIZE>(rGradX, tGradZ, tWeight);
        tWeight += IN_SIZE;
        if (GRAD_PARAM) {
            mplus<IN_SIZE>(tGradWeight, tGradZ, aX);
            rGradBias[j] += tGradZ;
            tGradWeight += IN_SIZE;
        }
    }
}

template <int CTYPE_GEN, int GRAD_PARAM>
static NNAP_DEVICE void nnBackward(flt_t aGradY, flt_t *aLayers, flt_t *rGradLayers, flt_t *aWeights,
                                   flt_t *rGradWeights, flt_t *rGradBiases, flt_t *aGradCache) noexcept {
// >>> NNAPGEN SWITCH
    flt_t *rSubGradX = rGradLayers;
    flt_t *tSubGradY = NULL;
    flt_t *tWeight = aWeights;
    flt_t *tSiLUGrad = aGradCache;
    flt_t *tSubX = GRAD_PARAM ? aLayers : NULL;
    flt_t *tGradWeight = GRAD_PARAM ? rGradWeights : NULL;
    flt_t *tGradBias = GRAD_PARAM ? rGradBiases : NULL;
    
    // switch to last layer
    tWeight += __NNAPGENX_NN_SIZE_HW__;
    tSiLUGrad += __NNAPGENX_NN_SIZE_HB__;
    rSubGradX += (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    if (GRAD_PARAM) {
        tGradWeight += __NNAPGENX_NN_SIZE_HW__;
        tGradBias += __NNAPGENX_NN_SIZE_HB__;
        tSubX += (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    }
    
    // begin backward
    rSubGradX -= __NNAPGENX_NN_SIZE_OW__;
    mplus<__NNAPGENX_NN_SIZE_OW__>(rSubGradX, aGradY, tWeight);
    if (GRAD_PARAM) {
        tSubX -= __NNAPGENX_NN_SIZE_OW__;
        // output weight
        mplus<__NNAPGENX_NN_SIZE_OW__>(tGradWeight, aGradY, tSubX);
        // output bias
        tGradBias[0] += aGradY;
    }
// >>> NNAPGEN REPEAT
    tWeight -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGrad -= __NNAPGENXX_NN_OUT_SIZE__;
    tSubGradY = rSubGradX;
    rSubGradX -= __NNAPGENXX_NN_IN_SIZE__;
    if (GRAD_PARAM) {
        tGradWeight -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
        tGradBias -= __NNAPGENXX_NN_OUT_SIZE__;
        tSubX -= __NNAPGENXX_NN_IN_SIZE__;
    }
    nnBackwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, GRAD_PARAM>(
        tSubX, rSubGradX, tSubGradY, tWeight,
        tGradWeight, tGradBias, tSiLUGrad
    );
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS __NNAPGENS_X__]<..0

// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
}

}

#endif //NN_FEED_FORWARD_H