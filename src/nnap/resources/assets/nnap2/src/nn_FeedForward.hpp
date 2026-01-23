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

template <int IN_SIZE, int OUT_SIZE>
static void nnForwardHiddenLayer(double *aX, double *aWeights, double *aBiases, double *rOutputs) noexcept {
    double *tWeights = aWeights;
    for (int j = 0; j < OUT_SIZE; ++j) {
        const double tDot = dot<IN_SIZE>(aX, tWeights) + aBiases[j];
        tWeights += IN_SIZE;
        rOutputs[j] = silu(tDot);
    }
}
template <int IN_SIZE, int OUT_SIZE>
static double nnForwardOutputLayer(double *aX, double *aHiddenWeights, double *aHiddenBiases, double *aOutputWeights, double aOutputBias, double *rHiddenOutputs) noexcept {
    double rOut = aOutputBias;
    double *tHiddenWeights = aHiddenWeights;
    for (int j = 0; j < OUT_SIZE; ++j) {
        const double tDot = dot<IN_SIZE>(aX, tHiddenWeights) + aHiddenBiases[j];
        tHiddenWeights += IN_SIZE;
        const double tSiluDot = silu(tDot);
        rHiddenOutputs[j] = tSiluDot; // for backward
        rOut += tSiluDot * aOutputWeights[j];
    }
    return rOut;
}

// >>> NNAPGEN REMOVE
#define NNAPGENXX_NN_IN_SIZE_H 10
#define NNAPGENXX_NN_OUT_SIZE_H 32
#define NNAPGENX_NN_IN_SIZE_O 32
#define NNAPGENX_NN_OUT_SIZE_O 24
// <<< NNAPGEN REMOVE

template <int CTYPE>
static double nnForward(double *aX, double *aHiddenWeights, double *aHiddenBiases, double *aOutputWeights, double aOutputBias, double *rHiddenOutputs) noexcept {
    double *tX = aX;
    double *tHiddenWeights = aHiddenWeights;
    double *tHiddenBiases = aHiddenBiases;
    double *tHiddenOutputs = rHiddenOutputs;
    
    double rOut;
// >>> NNAPGEN SWITCH
// >>> NNAPGEN REPEAT
    nnForwardHiddenLayer<NNAPGENXX_NN_IN_SIZE_H, NNAPGENXX_NN_OUT_SIZE_H>(tX, tHiddenWeights, tHiddenBiases, tHiddenOutputs);
    tHiddenWeights += NNAPGENXX_NN_IN_SIZE_H*NNAPGENXX_NN_OUT_SIZE_H;
    tHiddenBiases += NNAPGENXX_NN_OUT_SIZE_H;
    tX = tHiddenOutputs;
    tHiddenOutputs += NNAPGENXX_NN_OUT_SIZE_H;
// <<< NNAPGEN REPEAT [NN HIDDEN LAYERS NNAPGENS_CTYPE]
    
    rOut = nnForwardOutputLayer<NNAPGENX_NN_IN_SIZE_O, NNAPGENX_NN_OUT_SIZE_O>(tX, tHiddenWeights, tHiddenBiases, aOutputWeights, aOutputBias, tHiddenOutputs);
// <<< NNAPGEN SWITCH (CTYPE) [NN TYPE]
    return rOut;
}

}

#endif //NN_FEED_FORWARD_H