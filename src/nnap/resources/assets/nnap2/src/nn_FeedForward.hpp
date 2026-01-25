#ifndef NN_FEED_FORWARD_H
#define NN_FEED_FORWARD_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

static inline double silu(double aX) noexcept {
    return aX / (1.0 + exp(-aX));
}
static inline double siluGrad(double aX, double *rGrad) noexcept {
    double tSigmoid = 1.0 / (1.0 + exp(-aX));
    *rGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    return aX * tSigmoid;
}
static inline double siluGradGrad(double aX, double *rGrad, double *rGradGrad) noexcept {
    double tSigmoid = 1.0 / (1.0 + exp(-aX));
    *rGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    *rGradGrad = tSigmoid * (1 - tSigmoid) * (2 + aX * (1 - tSigmoid - tSigmoid));
    return aX * tSigmoid;
}


// >>> NNAPGEN REMOVE
#define NNAPGENXX_NN_IN_SIZE_H 10
#define NNAPGENXX_NN_OUT_SIZE_H 32
#define NNAPGENX_NN_SIZE_IN 10
#define NNAPGENX_NN_SIZE_HW (10*32)
#define NNAPGENX_NN_SIZE_HB 32
#define NNAPGENX_NN_SIZE_OW 32
// <<< NNAPGEN REMOVE

template <int IN_SIZE, int OUT_SIZE, int CACHE_GRAD>
static void nnForwardLayer(double *aX, double *rY, double *aWeights, double *aBiases, double *rSiLUGrad) noexcept {
    double *tWeights = aWeights;
    for (int j = 0; j < OUT_SIZE; ++j) {
        const double tDot = dot<IN_SIZE>(aX, tWeights) + aBiases[j];
        tWeights += IN_SIZE;
        if (CACHE_GRAD) {
            double rGrad;
            rY[j] = siluGrad(tDot, &rGrad);
            rSiLUGrad[j] = rGrad;
        } else {
            rY[j] = silu(tDot);
        }
    }
}

template <int CTYPE, int CACHE_GRAD>
static double nnForward(double *rLayers, double *aHiddenWeights, double *aHiddenBiases, double *aOutputWeights, double aOutputBias, double *rSiLUGrad) noexcept {
    double *tX = rLayers;
    double *tWeights = aHiddenWeights;
    double *tBiases = aHiddenBiases;
    double *tSiLUGrad = rSiLUGrad;
    
    double rOut;
// >>> NNAPGEN SWITCH
    double *rY = tX + NNAPGENX_NN_SIZE_IN;
    
// >>> NNAPGEN REPEAT
    nnForwardLayer<NNAPGENXX_NN_IN_SIZE_H, NNAPGENXX_NN_OUT_SIZE_H, CACHE_GRAD>(tX, rY, tWeights, tBiases, tSiLUGrad);
    tWeights += NNAPGENXX_NN_IN_SIZE_H*NNAPGENXX_NN_OUT_SIZE_H;
    tBiases += NNAPGENXX_NN_OUT_SIZE_H;
    tX = rY;
    rY += NNAPGENXX_NN_OUT_SIZE_H;
    if (CACHE_GRAD) {
        tSiLUGrad += NNAPGENXX_NN_OUT_SIZE_H;
    }
// <<< NNAPGEN REPEAT 0..<[NN HIDDEN LAYERS NNAPGENS_CTYPE]
    
    rOut = dot<NNAPGENX_NN_SIZE_OW>(tX, aOutputWeights) + aOutputBias;
// <<< NNAPGEN SWITCH (CTYPE) [NN TYPE]
    return rOut;
}


template <int IN_SIZE, int OUT_SIZE>
static void nnBackwardLayer(double *rGradX, double *aGradY, double *aWeights, double *aSiLUGrad) noexcept {
    double *tWeights = aWeights;
    for (int j = 0; j < OUT_SIZE; ++j) {
        double tGradZ =  aSiLUGrad[j] * aGradY[j];
        mplus<IN_SIZE>(rGradX, tGradZ, tWeights);
        tWeights += IN_SIZE;
    }
}

template <int CTYPE, int CLEAR_CACHE>
static void nnBackward(double aGradY, double *rGradLayers, double *aHiddenWeights, double *aOutputWeight, double *aSiLUGrad) noexcept {
    double *tHiddenWeights = aHiddenWeights;
    double *tSiLUGrad = aSiLUGrad;
    double *rGradX = rGradLayers;
    
// >>> NNAPGEN SWITCH
    if (CLEAR_CACHE) {
        fill<NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB>(rGradLayers, 0.0);
    }
    // switch to last layer
    tHiddenWeights += NNAPGENX_NN_SIZE_HW;
    tSiLUGrad += NNAPGENX_NN_SIZE_HB;
    rGradX += (NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB);
    
    // begin backward
    rGradX -= NNAPGENX_NN_SIZE_OW;
    mplus<NNAPGENX_NN_SIZE_OW>(rGradX, aGradY, aOutputWeight);
    double *tGradY = rGradX;
    
// >>> NNAPGEN REPEAT
    tHiddenWeights -= NNAPGENXX_NN_IN_SIZE_H*NNAPGENXX_NN_OUT_SIZE_H;
    tSiLUGrad -= NNAPGENXX_NN_OUT_SIZE_H;
    rGradX -= NNAPGENXX_NN_IN_SIZE_H;
    nnBackwardLayer<NNAPGENXX_NN_IN_SIZE_H, NNAPGENXX_NN_OUT_SIZE_H>(rGradX, tGradY, tHiddenWeights, tSiLUGrad);
    tGradY = rGradX;
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS NNAPGENS_CTYPE]<..0

// <<< NNAPGEN SWITCH (CTYPE) [NN TYPE]
}

}

#endif //NN_FEED_FORWARD_H