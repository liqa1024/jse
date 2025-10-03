#ifndef NN_FEED_FORWARD_H
#define NN_FEED_FORWARD_H

#include "nnap_util.hpp"

namespace JSE_NNAP {

static inline jdouble silu(jdouble aX) noexcept {
    return aX / (1.0 + exp(-aX));
}
static inline jdouble siluGrad(jdouble aX, jdouble *rGrad) noexcept {
    jdouble tSigmoid = 1.0 / (1.0 + exp(-aX));
    *rGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    return aX * tSigmoid;
}
static inline jdouble siluGradGrad(jdouble aX, jdouble *rGrad, jdouble *rGradGrad) noexcept {
    jdouble tSigmoid = 1.0 / (1.0 + exp(-aX));
    *rGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    *rGradGrad = tSigmoid * (1 - tSigmoid) * (2 + aX * (1 - tSigmoid - tSigmoid));
    return aX * tSigmoid;
}

template <jboolean SHARE>
static jdouble forward_(jdouble *aX, jint aInputDim, jint aSharedInputDim, jint *aHiddenDims, jboolean *aSharedFlags, jint aHiddenNumber,
                        jdouble *aHiddenWeights, jdouble *aSharedHiddenWeights, jdouble *aHiddenBiases, jdouble *aSharedHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias,
                        jdouble *rHiddenOutputs, jdouble *rHiddenGrads, jdouble *rHiddenGradGrads) noexcept {
    jdouble *tInput = aX;
    jdouble *rOutput = rHiddenOutputs;
    jdouble *rGrad = rHiddenGrads;
    jdouble *rGradGrad = rHiddenGradGrads;
    jdouble *tSharedWeights = NULL, *tSharedBiases = NULL;
    jdouble *tNoSharedWeights = NULL, *tNoSharedBiases = NULL;
    jdouble *tWeights = NULL, *tBiases = NULL;
    jint tInSize = aInputDim, tSharedInSize = aSharedInputDim;
    if (SHARE) {
        tNoSharedWeights = aHiddenWeights; tNoSharedBiases = aHiddenBiases;
        tSharedWeights = aSharedHiddenWeights; tSharedBiases = aSharedHiddenBiases;
    } else {
        tWeights = aHiddenWeights; tBiases = aHiddenBiases;
    }
    const jint tEnd = aHiddenNumber - 1;
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        if (SHARE) {
            if (aSharedFlags[i]) {
                tWeights = tSharedWeights;
                tBiases = tSharedBiases;
            } else {
                tWeights = tNoSharedWeights;
                tBiases = tNoSharedBiases;
                tNoSharedWeights += tOutSize*tInSize;
                tNoSharedBiases += tOutSize;
            }
            tSharedWeights += tOutSize*tSharedInSize;
            tSharedBiases += tOutSize;
        }
        for (jint j = 0; j < tOutSize; ++j) {
            jdouble rDot = dot(tInput, tWeights, tInSize) + tBiases[j];
            tWeights += tInSize;
            if (rGrad == NULL) {
                rOutput[j] = silu(rDot);
            } else
            if (rGradGrad == NULL) {
                jdouble rGradDot;
                rOutput[j] = siluGrad(rDot, &rGradDot);
                rGrad[j] = rGradDot;
            } else {
                jdouble rGradDot, rGradGradDot;
                rOutput[j] = siluGradGrad(rDot, &rGradDot, &rGradGradDot);
                rGrad[j] = rGradDot;
                rGradGrad[j] = rGradGradDot;
            }
        }
        if (!SHARE) {
            tBiases += tOutSize;
        }
        tInput = rOutput;
        rOutput += tOutSize;
        if (rGrad != NULL) rGrad += tOutSize;
        if (rGradGrad != NULL) rGradGrad += tOutSize;
        tSharedInSize = tInSize = tOutSize;
    }
    // special optimize for last layer
    jdouble rOut = aOutputBias;
    const jint tOutSize = aHiddenDims[tEnd];
    if (SHARE) {
        if (aSharedFlags[tEnd]) {
            tWeights = tSharedWeights;
            tBiases = tSharedBiases;
        } else {
            tWeights = tNoSharedWeights;
            tBiases = tNoSharedBiases;
        }
    }
    for (jint j = 0; j < tOutSize; ++j) {
        jdouble rDot = dot(tInput, tWeights, tInSize) + tBiases[j];
        tWeights += tInSize;
        if (rGrad == NULL) {
            rOutput[j] = silu(rDot);
        } else
        if (rGradGrad == NULL) {
            jdouble rGradDot;
            rOutput[j] = siluGrad(rDot, &rGradDot);
            rGrad[j] = rGradDot;
        } else {
            jdouble rGradDot, rGradGradDot;
            rOutput[j] = siluGradGrad(rDot, &rGradDot, &rGradGradDot);
            rGrad[j] = rGradDot;
            rGradGrad[j] = rGradGradDot;
        }
        jdouble tOutputWeight = aOutputWeight[j];
        rOut += rOutput[j] * tOutputWeight;
    }
    return rOut;
}
static inline jdouble forward(jdouble *aX, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias, jdouble *rHiddenOutputs, jdouble *rHiddenGrads, jdouble *rHiddenGradGrads) noexcept {
    return forward_<JNI_FALSE>(aX, aInputDim, -1, aHiddenDims, NULL, aHiddenNumber, aHiddenWeights, NULL, aHiddenBiases, NULL, aOutputWeight, aOutputBias, rHiddenOutputs, rHiddenGrads, rHiddenGradGrads);
}
static inline jdouble forward(jdouble *aX, jint aInputDim, jint aSharedInputDim, jint *aHiddenDims, jboolean *aSharedFlags, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aSharedHiddenWeights, jdouble *aHiddenBiases, jdouble *aSharedHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias, jdouble *rHiddenOutputs, jdouble *rHiddenGrads, jdouble *rHiddenGradGrads) noexcept {
    return forward_<JNI_TRUE>(aX, aInputDim, aSharedInputDim, aHiddenDims, aSharedFlags, aHiddenNumber, aHiddenWeights, aSharedHiddenWeights, aHiddenBiases, aSharedHiddenBiases, aOutputWeight, aOutputBias, rHiddenOutputs, rHiddenGrads, rHiddenGradGrads);
}

template <jboolean SHARE>
static void backward_(jdouble aYGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara,
                      jint aInputDim, jint aSharedInputDim, jint *aHiddenDims, jboolean *aSharedFlags, jint aHiddenNumber,
                      jdouble *aHiddenWeightsBackward, jdouble *aSharedHiddenWeightsBackward, jdouble *aOutputWeight,
                      jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *rHiddenGrads2, jdouble *rHiddenGrads3) noexcept {
    // switch to last layer
    const jint tEnd = aHiddenNumber - 1;
    jdouble *tGrad = aHiddenGrads;
    jdouble *rGrad2 = rHiddenGrads2;
    jdouble *rGrad3 = rHiddenGrads3;
    jdouble *tX = aHiddenOutputs;
    jdouble *rGradSharedWeights = NULL, *rGradSharedBiases = NULL;
    jdouble *rGradNoSharedWeights = NULL, *rGradNoSharedBiases = NULL;
    jdouble *rGradWeights = NULL, *rGradBiases = NULL;
    jint tInSize = -1, tSharedInSize = -1;
    if (rGradPara != NULL) {
        if (SHARE) {
            rGradSharedWeights = rGradSharedPara;
            rGradNoSharedWeights = rGradPara;
        } else {
            rGradWeights = rGradPara;
        }
        tInSize = aInputDim;
        tSharedInSize = aSharedInputDim;
    }
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad += tOutSize;
        rGrad2 += tOutSize;
        rGrad3 += tOutSize;
        tX += tOutSize;
        if (rGradPara != NULL) {
            if (SHARE) {
                rGradSharedWeights += tSharedInSize*tOutSize;
                if (!aSharedFlags[i]) {
                    rGradNoSharedWeights += tInSize*tOutSize;
                }
            } else {
                rGradWeights += tInSize*tOutSize;
            }
            tSharedInSize = tInSize = tOutSize;
        }
    }
    if (rGradPara != NULL) {
        jint tLastHiddenSize = aHiddenDims[tEnd];
        jdouble *rGradOutWeights;
        if (SHARE) {
            rGradSharedWeights += tSharedInSize*tLastHiddenSize;
            if (!aSharedFlags[tEnd]) {
                rGradNoSharedWeights += tInSize*tLastHiddenSize;
            }
            rGradOutWeights = rGradNoSharedWeights; // output weights
        } else {
            rGradWeights += tInSize*tLastHiddenSize;
            rGradOutWeights = rGradWeights;
        }
        for (jint j = 0; j < tLastHiddenSize; ++j) {
            rGradOutWeights[j] += aYGrad*tX[j];
        }
        if (SHARE) {
            rGradSharedBiases = rGradSharedWeights+tLastHiddenSize;
            rGradNoSharedBiases = rGradNoSharedWeights+tLastHiddenSize;
        } else {
            rGradBiases = rGradWeights+tLastHiddenSize;
        }
        for (jint i = 0; i < aHiddenNumber; ++i) {
            if (SHARE) {
                rGradSharedBiases += aHiddenDims[i];
                if (!aSharedFlags[i]) {
                    rGradNoSharedBiases += aHiddenDims[i];
                }
            } else {
                rGradBiases += aHiddenDims[i];
            }
        }
        if (SHARE) {
            *rGradNoSharedBiases += aYGrad;
        } else {
            *rGradBiases += aYGrad;
        }
    }
    // begin backward
    tSharedInSize = tInSize = aHiddenDims[tEnd];
    for (jint j = 0; j < tInSize; ++j) {
        rGrad3[j] = aYGrad * tGrad[j] * aOutputWeight[j];
    }
    jdouble *tSharedWeights = NULL;
    jdouble *tNoSharedWeights = NULL;
    jdouble *tWeights = NULL;
    if (SHARE) {
        tSharedWeights = aSharedHiddenWeightsBackward;
        tNoSharedWeights = aHiddenWeightsBackward;
    } else {
        tWeights = aHiddenWeightsBackward;
    }
    jdouble *tGrad3Before = rGrad3;
    for (jint i = tEnd-1; i >= 0; --i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad -= tOutSize;
        rGrad2 -= tOutSize;
        rGrad3 -= tOutSize;
        tX -= tOutSize;
        if (rGradPara != NULL) {
            jint tWeightSize = tOutSize*tInSize;
            if (SHARE) {
                if (aSharedFlags[i+1]) {
                    rGradWeights = rGradSharedWeights;
                    rGradBiases = rGradSharedBiases;
                } else {
                    rGradWeights = rGradNoSharedWeights;
                    rGradBiases = rGradNoSharedBiases;
                    rGradNoSharedWeights -= tWeightSize;
                    rGradNoSharedBiases -= tInSize;
                }
                rGradSharedWeights -= tOutSize*tSharedInSize;
                rGradSharedBiases -= tSharedInSize;
            }
            rGradWeights -= tWeightSize;
            rGradBiases -= tInSize;
            for (jint j = 0; j < tInSize; ++j) {
                jdouble tSubGrad3 = tGrad3Before[j];
                rGradBiases[j] += tSubGrad3;
                for (jint k = 0; k < tOutSize; ++k) {
                    rGradWeights[k] += tSubGrad3 * tX[k];
                }
                rGradWeights += tOutSize;
            }
            if (!SHARE) {
                rGradWeights -= tWeightSize;
            }
        }
        if (SHARE) {
            if (aSharedFlags[i+1]) {
                tWeights = tSharedWeights;
            } else {
                tWeights = tNoSharedWeights;
                tNoSharedWeights += tOutSize*tInSize;
            }
            tSharedWeights += tOutSize*tSharedInSize;
        }
        for (jint j = 0; j < tOutSize; ++j) {
            rGrad2[j] = dot(tGrad3Before, tWeights, tInSize);
            rGrad3[j] = rGrad2[j] * tGrad[j];
            tWeights += tInSize;
        }
        tGrad3Before = rGrad3;
        tSharedInSize = tInSize = tOutSize;
    }
    // to input layer
    if (rGradPara != NULL) {
        if (SHARE) {
            if (aSharedFlags[0]) {
                rGradWeights = rGradSharedWeights;
                rGradBiases = rGradSharedBiases;
            } else {
                rGradWeights = rGradNoSharedWeights;
                rGradBiases = rGradNoSharedBiases;
            }
        }
        rGradWeights -= aInputDim*tInSize;
        rGradBiases -= tInSize;
        for (jint j = 0; j < tInSize; ++j) {
            jdouble tSubGrad3 = tGrad3Before[j];
            rGradBiases[j] += tSubGrad3;
            for (jint k = 0; k < aInputDim; ++k) {
                rGradWeights[k] += tSubGrad3 * aX[k];
            }
            rGradWeights += aInputDim;
        }
    }
    if (rGradX != NULL) {
        if (SHARE) {
            if (aSharedFlags[0]) {
                tWeights = tSharedWeights;
            } else {
                tWeights = tNoSharedWeights;
            }
        }
        for (jint j = 0; j < aInputDim; ++j) {
            rGradX[j] += dot(tGrad3Before, tWeights, tInSize);
            tWeights += tInSize;
        }
    }
}
static inline void backward(jdouble aYGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *rHiddenGrads2, jdouble *rHiddenGrads3) noexcept {
    backward_<JNI_FALSE>(aYGrad, aX, rGradX, rGradPara, NULL, aInputDim, -1, aHiddenDims, NULL, aHiddenNumber, aHiddenWeightsBackward, NULL, aOutputWeight, aHiddenOutputs, aHiddenGrads, rHiddenGrads2, rHiddenGrads3);
}
static inline void backward(jdouble aYGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara, jint aInputDim, jint aSharedInputDim, jint *aHiddenDims, jboolean *aSharedFlags, jint aHiddenNumber, jdouble *aHiddenWeightsBackward, jdouble *aSharedHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *rHiddenGrads2, jdouble *rHiddenGrads3) noexcept {
    backward_<JNI_TRUE>(aYGrad, aX, rGradX, rGradPara, rGradSharedPara, aInputDim, aSharedInputDim, aHiddenDims, aSharedFlags, aHiddenNumber, aHiddenWeightsBackward, aSharedHiddenWeightsBackward, aOutputWeight, aHiddenOutputs, aHiddenGrads, rHiddenGrads2, rHiddenGrads3);
}

template <jboolean SHARE>
static void gradBackward_(jdouble *aGradXGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara,
                          jint aInputDim, jint aSharedInputDim, jint *aHiddenDims, jboolean *aSharedFlags, jint aHiddenNumber,
                          jdouble *aHiddenWeights, jdouble *aSharedHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aSharedHiddenWeightsBackward, jdouble *aOutputWeight,
                          jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *aHiddenGrads2, jdouble *aHiddenGrads3, jdouble *aHiddenGradGrads,
                          jdouble *rHiddenOutputs2, jdouble *rHiddenGrads4, jdouble *rHiddenGrads5) noexcept {
    // ptr init
    jdouble *tGrad = aHiddenGrads;
    jdouble *tGrad2 = aHiddenGrads2;
    jdouble *tGrad3 = aHiddenGrads3;
    jdouble *tGradGrad = aHiddenGradGrads;
    jdouble *tX = aHiddenOutputs;
    jdouble *rX2 = rHiddenOutputs2;
    jdouble *rGrad4 = rHiddenGrads4;
    jdouble *rGrad5 = rHiddenGrads5;
    jdouble *rGradSharedWeights = NULL, *rGradSharedBiases = NULL;
    jdouble *rGradNoSharedWeights = NULL, *rGradNoSharedBiases = NULL;
    jdouble *rGradWeights = NULL, *rGradBiases = NULL;
    jint tColNum = aInputDim, tSharedColNum = aSharedInputDim;
    if (SHARE) {
        rGradSharedWeights = rGradSharedPara; rGradSharedBiases = rGradSharedPara;
        rGradNoSharedWeights = rGradPara; rGradNoSharedBiases = rGradPara;
    } else {
        rGradWeights = rGradPara; rGradBiases = rGradPara;
    }
    for (jint i = 0; i < aHiddenNumber; ++i) {
        jint tHiddenDim = aHiddenDims[i];
        if (SHARE) {
            if (!aSharedFlags[i]) {
                rGradNoSharedBiases += tColNum*tHiddenDim;
            }
            rGradSharedBiases += tSharedColNum*tHiddenDim;
        } else {
            rGradBiases += tColNum*tHiddenDim;
        }
        tSharedColNum = tColNum = tHiddenDim;
    }
    if (SHARE) {
        rGradNoSharedBiases += aHiddenDims[aHiddenNumber-1];
        rGradSharedBiases += aHiddenDims[aHiddenNumber-1];
    } else {
        rGradBiases += aHiddenDims[aHiddenNumber-1];
    }
    /// backward backward
    // diff W0 ij
    jint tInSize = aInputDim, tSharedInSize = aSharedInputDim;
    jint tOutSize = aHiddenDims[0];
    if (SHARE) {
        if (aSharedFlags[0]) {
            rGradWeights = rGradSharedWeights;
        } else {
            rGradWeights = rGradNoSharedWeights;
            rGradNoSharedWeights += tOutSize*tInSize;
        }
        rGradSharedWeights += tOutSize*tSharedInSize;
    }
    for (jint i = 0; i < tOutSize; ++i) {
        jdouble tSubGrad3 = tGrad3[i];
        for (jint j = 0; j < tInSize; ++j) {
            rGradWeights[j] += aGradXGrad[j]*tSubGrad3;
        }
        rGradWeights += tInSize;
    }
    // G^0 i
    jdouble *tSharedWeights = NULL;
    jdouble *tNoSharedWeights = NULL;
    jdouble *tWeights = NULL;
    if (SHARE) {
        tSharedWeights = aSharedHiddenWeights;
        tNoSharedWeights = aHiddenWeights;
    } else {
        tWeights = aHiddenWeights;
    }
    if (SHARE) {
        if (aSharedFlags[0]) {
            tWeights = tSharedWeights;
        } else {
            tWeights = tNoSharedWeights;
            tNoSharedWeights += tOutSize*tInSize;
        }
        tSharedWeights += tOutSize*tSharedInSize;
    }
    for (jint i = 0; i < tOutSize; ++i) {
        rGrad5[i] = dot(aGradXGrad, tWeights, tInSize);
        tWeights += tInSize;
    }
    const jint tEnd = aHiddenNumber - 1;
    for (jint l = 0; l < tEnd; ++l) {
        // Gl i, G~l i
        for (jint i = 0; i < tOutSize; ++i) {
            rGrad4[i] = tGrad2[i] * rGrad5[i];
            rGrad5[i] = tGrad[i] * rGrad5[i];
        }
        tGrad += tOutSize;
        tGrad2 += tOutSize;
        tGrad3 += tOutSize;
        tGradGrad += tOutSize;
        tX += tOutSize;
        if (SHARE) {
            if (!aSharedFlags[l]) {
                rGradNoSharedBiases += tOutSize;
            }
            rGradSharedBiases += tOutSize;
        } else {
            rGradBiases += tOutSize;
        }
        // diff Wl+1 ij
        tSharedInSize = tInSize = tOutSize;
        tOutSize = aHiddenDims[l+1];
        if (SHARE) {
            if (aSharedFlags[l+1]) {
                rGradWeights = rGradSharedWeights;
            } else {
                rGradWeights = rGradNoSharedWeights;
                rGradNoSharedWeights += tOutSize*tInSize;
            }
            rGradSharedWeights += tOutSize*tSharedInSize;
        }
        for (jint i = 0; i < tOutSize; ++i) {
            jdouble tSubGrad3 = tGrad3[i];
            for (jint j = 0; j < tInSize; ++j) {
                rGradWeights[j] += tSubGrad3 * rGrad5[j];
            }
            rGradWeights += tInSize;
        }
        // G^l+1 ik
        if (SHARE) {
            if (aSharedFlags[l+1]) {
                tWeights = tSharedWeights;
            } else {
                tWeights = tNoSharedWeights;
                tNoSharedWeights += tOutSize*tInSize;
            }
            tSharedWeights += tOutSize*tSharedInSize;
        }
        jdouble *rGrad6 = rGrad5 + tInSize;
        for (jint i = 0; i < tOutSize; ++i) {
            rGrad6[i] = dot(tWeights, rGrad5, tInSize);
            tWeights += tInSize;
        }
        rGrad4 += tInSize;
        rGrad5 += tInSize;
    }
    // Gend i, Wo i
    tOutSize = aHiddenDims[tEnd];
    if (SHARE) {
        rGradWeights = rGradNoSharedWeights; // output weights
    }
    for (jint i = 0; i < tOutSize; ++i) {
        rGrad4[i] = aOutputWeight[i] * rGrad5[i];
        rGradWeights[i] += tGrad[i] * rGrad5[i];
    }
    /// backward forward
    // X^end ik
    for (jint i = 0; i < tOutSize; ++i) {
        rX2[i] = tGradGrad[i] * rGrad4[i];
    }
    if (SHARE) {
        if (!aSharedFlags[tEnd]) {
            rGradNoSharedBiases += tOutSize;
        }
        rGradSharedBiases += tOutSize;
    } else {
        rGradBiases += tOutSize;
    }
    if (SHARE) {
        tSharedWeights = aSharedHiddenWeightsBackward;
        tNoSharedWeights = aHiddenWeightsBackward;
    } else {
        tWeights = aHiddenWeightsBackward;
    }
    for (jint l = tEnd; l > 0; --l) {
        // bl i, Wl ij
        tSharedInSize = tInSize = aHiddenDims[l-1];
        tX -= tInSize;
        if (SHARE) {
            if (aSharedFlags[l]) {
                rGradWeights = rGradSharedWeights;
                rGradBiases = rGradSharedBiases;
            } else {
                rGradWeights = rGradNoSharedWeights;
                rGradBiases = rGradNoSharedBiases;
                rGradNoSharedWeights -= tInSize*tOutSize;
                rGradNoSharedBiases -= tOutSize;
            }
            rGradSharedWeights -= tOutSize*tSharedInSize;
            rGradSharedBiases -= tOutSize;
        }
        rGradWeights -= tInSize*tOutSize;
        rGradBiases -= tOutSize;
        for (jint i = 0; i < tOutSize; ++i) {
            jdouble tSubX2 = rX2[i];
            rGradBiases[i] += tSubX2;
            for (jint j = 0; j < tInSize; ++j) {
                rGradWeights[j] += tX[j] * tSubX2;
            }
            rGradWeights += tInSize;
        }
        if (!SHARE) {
            rGradWeights -= tInSize*tOutSize;
        }
        // Xl-1 ik
        if (SHARE) {
            if (aSharedFlags[l]) {
                tWeights = tSharedWeights;
            } else {
                tWeights = tNoSharedWeights;
                tNoSharedWeights += tOutSize*tInSize;
            }
            tSharedWeights += tOutSize*tSharedInSize;
        }
        jdouble *rX3 = rX2 + tOutSize;
        for (jint i = 0; i < tInSize; ++i) {
            rX3[i] = dot(tWeights, rX2, tOutSize);
            tWeights += tOutSize;
        }
        rX2 += tOutSize;
        // X^l-1 ik
        tOutSize = tInSize;
        tGrad -= tInSize;
        tGradGrad -= tInSize;
        rGrad4 -= tOutSize;
        for (jint i = 0; i < tOutSize; ++i) {
            rX2[i] = tGradGrad[i]*rGrad4[i] + tGrad[i]*rX2[i];
        }
    }
    // b0 i, W0 ij
    if (SHARE) {
        if (aSharedFlags[0]) {
            rGradWeights = rGradSharedWeights;
            rGradBiases = rGradSharedBiases;
        } else {
            rGradWeights = rGradNoSharedWeights;
            rGradBiases = rGradNoSharedBiases;
        }
    }
    rGradBiases -= tOutSize;
    rGradWeights -= aInputDim*tOutSize;
    for (jint i = 0; i < tOutSize; ++i) {
        jdouble tSubX2 = rX2[i];
        rGradBiases[i] += tSubX2;
        for (jint j = 0; j < aInputDim; ++j) {
            rGradWeights[j] += aX[j]*tSubX2;
        }
        rGradWeights += aInputDim;
    }
    if (rGradX != NULL) {
        if (SHARE) {
            if (aSharedFlags[0]) {
                tWeights = tSharedWeights;
            } else {
                tWeights = tNoSharedWeights;
            }
        }
        for (jint j = 0; j < aInputDim; ++j) {
            rGradX[j] += dot(rX2, tWeights, tInSize);
            tWeights += tInSize;
        }
    }
}
static inline void gradBackward(jdouble *aGradXGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *aHiddenGrads2, jdouble *aHiddenGrads3, jdouble *aHiddenGradGrads, jdouble *rHiddenOutputs2, jdouble *rHiddenGrads4, jdouble *rHiddenGrads5) noexcept {
    gradBackward_<JNI_FALSE>(aGradXGrad, aX, rGradX, rGradPara, NULL, aInputDim, -1, aHiddenDims, NULL, aHiddenNumber, aHiddenWeights, NULL, aHiddenWeightsBackward, NULL, aOutputWeight, aHiddenOutputs, aHiddenGrads, aHiddenGrads2, aHiddenGrads3, aHiddenGradGrads, rHiddenOutputs2, rHiddenGrads4, rHiddenGrads5);
}
static inline void gradBackward(jdouble *aGradXGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara, jint aInputDim, jint aSharedInputDim, jint *aHiddenDims, jboolean *aSharedFlags, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aSharedHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aSharedHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *aHiddenGrads2, jdouble *aHiddenGrads3, jdouble *aHiddenGradGrads, jdouble *rHiddenOutputs2, jdouble *rHiddenGrads4, jdouble *rHiddenGrads5) noexcept {
    gradBackward_<JNI_TRUE>(aGradXGrad, aX, rGradX, rGradPara, rGradSharedPara, aInputDim, aSharedInputDim, aHiddenDims, aSharedFlags, aHiddenNumber, aHiddenWeights, aSharedHiddenWeights, aHiddenWeightsBackward, aSharedHiddenWeightsBackward, aOutputWeight, aHiddenOutputs, aHiddenGrads, aHiddenGrads2, aHiddenGrads3, aHiddenGradGrads, rHiddenOutputs2, rHiddenGrads4, rHiddenGrads5);
}

}

#endif //NN_FEED_FORWARD_H