#ifndef NN_FEED_FORWARD_H
#define NN_FEED_FORWARD_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

static inline NNAP_DEVICE NNAP_HOST flt_t silu(flt_t aX) noexcept {
    return aX / (ONE + nnap_exp(-aX));
}
static inline NNAP_DEVICE NNAP_HOST flt_t siluGrad(flt_t aX, flt_t *rGrad) noexcept {
    flt_t tSigmoid = ONE / (ONE + nnap_exp(-aX));
    *rGrad = tSigmoid * (ONE + aX * (ONE - tSigmoid));
    return aX * tSigmoid;
}
static inline NNAP_DEVICE NNAP_HOST flt_t siluGradGrad(flt_t aX, flt_t *rGrad, flt_t *rGradGrad) noexcept {
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

template <int IN_SIZE, int OUT_SIZE>
static NNAP_DEVICE void nnForwardLayerGpu(
    flt_t *aX, flt_t *rY, flt_t *aWeight, flt_t *aBias, flt_t *rSiLUGrad) noexcept {
    
    flt_t *tWeight = aWeight;
    for (int j = 0; j < OUT_SIZE; ++j) {
        const flt_t tDot = dot<IN_SIZE>(aX, tWeight) + aBias[j];
        tWeight += IN_SIZE;
        flt_t rGrad;
        rY[j] = siluGrad(tDot, &rGrad);
        rSiLUGrad[j] = rGrad;
    }
}

template <int CTYPE_GEN>
static NNAP_DEVICE flt_t nnForwardGpu(
    flt_t *aFp, flt_t *aWeights, flt_t *aBiases, flt_t *rGradCache) noexcept {
    
    flt_t rOut;
// >>> NNAPGEN SWITCH
    flt_t bHiddenLayers[__NNAPGENX_NN_SIZE_HB__];
    flt_t *tSubX = aFp;
    flt_t *rSubY = bHiddenLayers;
    
    flt_t *tWeight = aWeights;
    flt_t *tBias = aBiases;
    flt_t *rSiLUGrad = rGradCache;
    
// >>> NNAPGEN REPEAT
    nnForwardLayerGpu<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__>(
        tSubX, rSubY, tWeight, tBias, rSiLUGrad
    );
    tWeight += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tBias += __NNAPGENXX_NN_OUT_SIZE__;
    rSiLUGrad += __NNAPGENXX_NN_OUT_SIZE__;
    tSubX = rSubY;
    rSubY += __NNAPGENXX_NN_OUT_SIZE__;
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
    
    rOut = dot<__NNAPGENX_NN_SIZE_OW__>(tSubX, tWeight) + tBias[0];
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    return rOut;
}


template <int IN_SIZE, int OUT_SIZE>
static NNAP_DEVICE void nnBackwardLayerGpu(
    flt_t *rAGradX, flt_t *aAGradY, flt_t *aWeight, flt_t *aSiLUGrad) noexcept {
    
    flt_t *tWeight = aWeight;
    for (int j = 0; j < OUT_SIZE; ++j) {
        flt_t tAGradZ = aAGradY[j] * aSiLUGrad[j];
        mplus<IN_SIZE>(rAGradX, tAGradZ, tWeight);
        tWeight += IN_SIZE;
    }
}

template <int CTYPE_GEN>
static NNAP_DEVICE void nnBackwardGpu(
    flt_t aAGradY, flt_t *rAGradFp, flt_t *aWeights, flt_t *aGradCache) noexcept {
    
// >>> NNAPGEN SWITCH
    flt_t bHiddenLayers[__NNAPGENX_NN_SIZE_HB__] = {0};
    flt_t *rSubAGradX = bHiddenLayers;
    flt_t *tSubAGradY = NULL;
    
    flt_t *tWeight = aWeights;
    flt_t *tSiLUGrad = aGradCache;
    
    // switch to last layer
    tWeight += __NNAPGENX_NN_SIZE_HW__;
    tSiLUGrad += __NNAPGENX_NN_SIZE_HB__;
    rSubAGradX += __NNAPGENX_NN_SIZE_HB__;
    
    // begin backward
    rSubAGradX -= __NNAPGENX_NN_SIZE_OW__;
    mplus<__NNAPGENX_NN_SIZE_OW__>(rSubAGradX, aAGradY, tWeight);
// >>> NNAPGEN REPEAT
    tWeight -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGrad -= __NNAPGENXX_NN_OUT_SIZE__;
    tSubAGradY = rSubAGradX;
    if (__NNAPGENOS_X__ == 0) {
        rSubAGradX = rAGradFp;
    } else {
        rSubAGradX -= __NNAPGENXX_NN_IN_SIZE__;
    }
    nnBackwardLayerGpu<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__>(
        rSubAGradX, tSubAGradY, tWeight, tSiLUGrad
    );
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS __NNAPGENS_X__]<..0

// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
}



template <int IN_SIZE, int OUT_SIZE, int CACHE_GRAD, int CACHE_GRADGRAD>
static void nnForwardLayer(flt_t *aX, flt_t *rY, flt_t *aWeight, flt_t *aBias, flt_t *rSiLUGrad, flt_t *rSiLUGradGrad) noexcept {
    flt_t *tWeight = aWeight;
    for (int j = 0; j < OUT_SIZE; ++j) {
        const flt_t tDot = dot<IN_SIZE>(aX, tWeight) + aBias[j];
        tWeight += IN_SIZE;
        if (CACHE_GRADGRAD) {
            flt_t rGrad, rGradGrad;
            rY[j] = siluGradGrad(tDot, &rGrad, &rGradGrad);
            if (CACHE_GRAD) rSiLUGrad[j] = rGrad;
            rSiLUGradGrad[j] = rGradGrad;
        } else
        if (CACHE_GRAD) {
            flt_t rGrad;
            rY[j] = siluGrad(tDot, &rGrad);
            rSiLUGrad[j] = rGrad;
        } else {
            rY[j] = silu(tDot);
        }
    }
}


template <int CTYPE_GEN, int CACHE_GRAD, int CACHE_GRADGRAD>
static flt_t nnForward(flt_t *rLayers, flt_t *aWeights, flt_t *aBiases, flt_t *rGradCache, flt_t *rGradGradCache) noexcept {
    flt_t rOut;
// >>> NNAPGEN SWITCH
    flt_t *tSubX = rLayers;
    flt_t *rSubY = rLayers + __NNAPGENX_NN_SIZE_IN__;
    
    flt_t *tWeight = aWeights;
    flt_t *tBias = aBiases;
    flt_t *rSiLUGrad = rGradCache;
    flt_t *rSiLUGradGrad = rGradGradCache;
    
// >>> NNAPGEN REPEAT
    nnForwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, CACHE_GRAD, CACHE_GRADGRAD>(
        tSubX, rSubY, tWeight, tBias, rSiLUGrad, rSiLUGradGrad
    );
    tWeight += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tBias += __NNAPGENXX_NN_OUT_SIZE__;
    tSubX = rSubY;
    rSubY += __NNAPGENXX_NN_OUT_SIZE__;
    if (CACHE_GRAD) rSiLUGrad += __NNAPGENXX_NN_OUT_SIZE__;
    if (CACHE_GRADGRAD) rSiLUGradGrad += __NNAPGENXX_NN_OUT_SIZE__;
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
    
    rOut = dot<__NNAPGENX_NN_SIZE_OW__>(tSubX, tWeight) + tBias[0];
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
    return rOut;
}


template <int IN_SIZE, int OUT_SIZE, int GRAD_PARAM, int CACHE_Z>
static void nnBackwardLayer(flt_t *aX, flt_t *rAGradX, flt_t *aAGradY, flt_t *rAGradZ,
                            flt_t *aWeight, flt_t *rAGradWeight, flt_t *rAGradBias, flt_t *aSiLUGrad) noexcept {
    flt_t *tWeight = aWeight;
    flt_t *tAGradWeight = GRAD_PARAM ? rAGradWeight : NULL;
    for (int j = 0; j < OUT_SIZE; ++j) {
        flt_t tAGradZ = aAGradY[j] * aSiLUGrad[j];
        if (CACHE_Z) {
            rAGradZ[j] += tAGradZ;
            tAGradZ = rAGradZ[j];
        }
        mplus<IN_SIZE>(rAGradX, tAGradZ, tWeight);
        tWeight += IN_SIZE;
        if (GRAD_PARAM) {
            mplus<IN_SIZE>(tAGradWeight, tAGradZ, aX);
            rAGradBias[j] += tAGradZ;
            tAGradWeight += IN_SIZE;
        }
    }
}

template <int CTYPE_GEN, int GRAD_PARAM, int CACHE_Z>
static void nnBackward(flt_t aAGradY, flt_t *aLayers, flt_t *rAGradLayers, flt_t *rAGradLayersZ,
                       flt_t *aWeights, flt_t *rAGradWeights, flt_t *rAGradBiases, flt_t *aGradCache) noexcept {
// >>> NNAPGEN SWITCH
    flt_t *rSubAGradX = rAGradLayers;
    flt_t *tSubAGradY = NULL;
    flt_t *tWeight = aWeights;
    flt_t *tSiLUGrad = aGradCache;
    flt_t *tSubX = GRAD_PARAM ? aLayers : NULL;
    flt_t *rAGradWeight = GRAD_PARAM ? rAGradWeights : NULL;
    flt_t *rAGradBias = GRAD_PARAM ? rAGradBiases : NULL;
    flt_t *rSubAGradZ = CACHE_Z ? rAGradLayersZ : NULL;
    
    // switch to last layer
    tWeight += __NNAPGENX_NN_SIZE_HW__;
    tSiLUGrad += __NNAPGENX_NN_SIZE_HB__;
    rSubAGradX += (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    if (GRAD_PARAM) {
        rAGradWeight += __NNAPGENX_NN_SIZE_HW__;
        rAGradBias += __NNAPGENX_NN_SIZE_HB__;
        tSubX += (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    }
    if (CACHE_Z) {
        rSubAGradZ += __NNAPGENX_NN_SIZE_HB__;
    }
    
    // begin backward
    rSubAGradX -= __NNAPGENX_NN_SIZE_OW__;
    mplus<__NNAPGENX_NN_SIZE_OW__>(rSubAGradX, aAGradY, tWeight);
    if (GRAD_PARAM) {
        tSubX -= __NNAPGENX_NN_SIZE_OW__;
        // output weight
        mplus<__NNAPGENX_NN_SIZE_OW__>(rAGradWeight, aAGradY, tSubX);
        // output bias
        rAGradBias[0] += aAGradY;
    }
// >>> NNAPGEN REPEAT
    tWeight -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGrad -= __NNAPGENXX_NN_OUT_SIZE__;
    tSubAGradY = rSubAGradX;
    rSubAGradX -= __NNAPGENXX_NN_IN_SIZE__;
    if (GRAD_PARAM) {
        rAGradWeight -= __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
        rAGradBias -= __NNAPGENXX_NN_OUT_SIZE__;
        tSubX -= __NNAPGENXX_NN_IN_SIZE__;
    }
    if (CACHE_Z) {
        rSubAGradZ -= __NNAPGENXX_NN_OUT_SIZE__;
    }
    nnBackwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__, GRAD_PARAM, CACHE_Z>(
        tSubX, rSubAGradX, tSubAGradY, rSubAGradZ,
        tWeight, rAGradWeight, rAGradBias, tSiLUGrad
    );
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS __NNAPGENS_X__]<..0

// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
}


template <int IN_SIZE, int OUT_SIZE>
static void nnBackwardBackwardLayer(flt_t *aBGradAGradX, flt_t *aAGradY, flt_t *rBGradAGradY, flt_t *aAGradZ, flt_t *rBGradZ,
                                    flt_t *aWeight, flt_t *rBGradWeight, flt_t *aSiLUGrad, flt_t *aSiLUGradGrad) noexcept {
    flt_t *tWeight = aWeight;
    flt_t *tBGradWeight = rBGradWeight;
    for (int j = 0; j < OUT_SIZE; ++j) {
        // grad z -> grad weight
        mplus<IN_SIZE>(tBGradWeight, aAGradZ[j], aBGradAGradX);
        // grad z -> grad grad z
        const flt_t tBGradAGradZ = dot<IN_SIZE>(aBGradAGradX, tWeight);
        // grad grad z -> grad grad y
        rBGradAGradY[j] += tBGradAGradZ * aSiLUGrad[j];
        // grad grad z -> grad z
        rBGradZ[j] += tBGradAGradZ * aAGradY[j] * aSiLUGradGrad[j];
        
        tWeight += IN_SIZE;
        tBGradWeight += IN_SIZE;
    }
}

template <int CTYPE_GEN, int GRAD_IN>
static void nnBackwardBackward(flt_t aAGradY, flt_t *rBGradAGradY, flt_t *aAGradLayers, flt_t *rBGradAGradLayers, flt_t *aAGradLayersZ, flt_t *rBGradLayersZ,
                               flt_t *aWeights, flt_t *rBGradWeights, flt_t *aGradCache, flt_t *aGradGradCache) noexcept {
// >>> NNAPGEN SWITCH
    flt_t *tSubAGradY = aAGradLayers + __NNAPGENX_NN_SIZE_IN__;
    flt_t *tSubBGradAGradX = rBGradAGradLayers;
    flt_t *rSubBGradAGradY = rBGradAGradLayers + __NNAPGENX_NN_SIZE_IN__;
    flt_t *tSubAGradZ = aAGradLayersZ;
    flt_t *rSubBGradZ = rBGradLayersZ;
    flt_t *tWeight = aWeights;
    flt_t *rBGradWeight = rBGradWeights;
    flt_t *tSiLUGrad = aGradCache;
    flt_t *tSiLUGradGrad = aGradGradCache;
    
// >>> NNAPGEN REPEAT
    nnBackwardBackwardLayer<__NNAPGENXX_NN_IN_SIZE__, __NNAPGENXX_NN_OUT_SIZE__>(
        tSubBGradAGradX, tSubAGradY, rSubBGradAGradY, tSubAGradZ, rSubBGradZ,
        tWeight, rBGradWeight, tSiLUGrad, tSiLUGradGrad
    );
    tSubBGradAGradX = rSubBGradAGradY;
    rSubBGradAGradY += __NNAPGENXX_NN_OUT_SIZE__;
    tSubAGradY += __NNAPGENXX_NN_OUT_SIZE__;
    tSubAGradZ += __NNAPGENXX_NN_OUT_SIZE__;
    rSubBGradZ += __NNAPGENXX_NN_OUT_SIZE__;
    
    tWeight += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    rBGradWeight += __NNAPGENXX_NN_IN_SIZE__*__NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGrad += __NNAPGENXX_NN_OUT_SIZE__;
    tSiLUGradGrad += __NNAPGENXX_NN_OUT_SIZE__;
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS __NNAPGENS_X__]
    
    mplus<__NNAPGENX_NN_SIZE_OW__>(rBGradWeight, aAGradY, tSubBGradAGradX);
    if (GRAD_IN) {
        *rBGradAGradY = dot<__NNAPGENX_NN_SIZE_OW__>(tSubBGradAGradX, tWeight);
    }
// <<< NNAPGEN SWITCH (CTYPE_GEN) [NN TYPE]
}


}

#endif //NN_FEED_FORWARD_H