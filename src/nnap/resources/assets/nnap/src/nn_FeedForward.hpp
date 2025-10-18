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
static jdouble forward_(jdouble *aX, jint aInputDim, jint *aHiddenDims, jint *aSharedHiddenDims, jint aHiddenNumber,
                        jdouble *aHiddenWeights, jdouble *aSharedHiddenWeights, jdouble *aHiddenBiases, jdouble *aSharedHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias,
                        jdouble *rHiddenOutputs, jdouble *rHiddenGrads, jdouble *rHiddenGradGrads) noexcept {
    jdouble *tInput = aX;
    jdouble *rOutput = rHiddenOutputs;
    jdouble *rGrad = rHiddenGrads;
    jdouble *rGradGrad = rHiddenGradGrads;
    jdouble *tSharedWeights = NULL, *tSharedBiases = NULL;
    jdouble *tNoSharedWeights = NULL, *tNoSharedBiases = NULL;
    jdouble *tWeights = NULL, *tBiases = NULL;
    jint tInSize = aInputDim;
    if (SHARE) {
        tNoSharedWeights = aHiddenWeights; tNoSharedBiases = aHiddenBiases;
        tSharedWeights = aSharedHiddenWeights; tSharedBiases = aSharedHiddenBiases;
    } else {
        tWeights = aHiddenWeights; tBiases = aHiddenBiases;
    }
    const jint tEnd = aHiddenNumber - 1;
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        jint tNoSharedOutSize = -1;
        if (SHARE) {
            tNoSharedOutSize = tOutSize-aSharedHiddenDims[i];
            // shared last
            tWeights = tNoSharedWeights;
            tBiases = tNoSharedBiases;
        }
        for (jint j = 0; j < tOutSize; ++j) {
            if (SHARE) {
                if (j == tNoSharedOutSize) {
                    tWeights = tSharedWeights + tNoSharedOutSize*tInSize;
                    tBiases = tSharedBiases;
                }
            }
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
        if (SHARE) {
            tNoSharedWeights += tNoSharedOutSize*tInSize;
            tNoSharedBiases += tNoSharedOutSize;
            tSharedWeights += tOutSize*tInSize;
            tSharedBiases += tOutSize;
        } else {
            tBiases += tOutSize;
        }
        tInput = rOutput;
        rOutput += tOutSize;
        if (rGrad != NULL) rGrad += tOutSize;
        if (rGradGrad != NULL) rGradGrad += tOutSize;
        tInSize = tOutSize;
    }
    // special optimize for last layer
    jdouble rOut = aOutputBias;
    const jint tOutSize = aHiddenDims[tEnd];
    jint tNoSharedOutSize = -1;
    if (SHARE) {
        tNoSharedOutSize = tOutSize-aSharedHiddenDims[tEnd];
        // shared last
        tWeights = tNoSharedWeights;
        tBiases = tNoSharedBiases;
    }
    for (jint j = 0; j < tOutSize; ++j) {
        if (SHARE) {
            if (j == tNoSharedOutSize) {
                tWeights = tSharedWeights + tNoSharedOutSize*tInSize;
                tBiases = tSharedBiases;
            }
        }
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
    return forward_<JNI_FALSE>(aX, aInputDim, aHiddenDims, NULL, aHiddenNumber, aHiddenWeights, NULL, aHiddenBiases, NULL, aOutputWeight, aOutputBias, rHiddenOutputs, rHiddenGrads, rHiddenGradGrads);
}
static inline jdouble forward(jdouble *aX, jint aInputDim, jint *aHiddenDims, jint *aSharedHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aSharedHiddenWeights, jdouble *aHiddenBiases, jdouble *aSharedHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias, jdouble *rHiddenOutputs, jdouble *rHiddenGrads, jdouble *rHiddenGradGrads) noexcept {
    return forward_<JNI_TRUE>(aX, aInputDim, aHiddenDims, aSharedHiddenDims, aHiddenNumber, aHiddenWeights, aSharedHiddenWeights, aHiddenBiases, aSharedHiddenBiases, aOutputWeight, aOutputBias, rHiddenOutputs, rHiddenGrads, rHiddenGradGrads);
}

template <jboolean SHARE>
static void backward_(jdouble aYGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara,
                      jint aInputDim, jint *aHiddenDims, jint *aSharedHiddenDims, jint aHiddenNumber,
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
    jint tInSize = -1;
    if (rGradPara != NULL) {
        if (SHARE) {
            rGradSharedWeights = rGradSharedPara;
            rGradNoSharedWeights = rGradPara;
        } else {
            rGradWeights = rGradPara;
        }
        tInSize = aInputDim;
    }
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad += tOutSize;
        rGrad2 += tOutSize;
        rGrad3 += tOutSize;
        tX += tOutSize;
        if (rGradPara != NULL) {
            if (SHARE) {
                const jint tNoSharedOutSize = tOutSize-aSharedHiddenDims[i];
                rGradSharedWeights += tInSize*tOutSize;
                rGradNoSharedWeights += tInSize*tNoSharedOutSize;
            } else {
                rGradWeights += tInSize*tOutSize;
            }
            tInSize = tOutSize;
        }
    }
    if (rGradPara != NULL) {
        jint tLastHiddenSize = aHiddenDims[tEnd];
        if (SHARE) {
            jint tLastNoSharedHiddenSize = tLastHiddenSize-aSharedHiddenDims[tEnd];
            rGradSharedWeights += tInSize*tLastHiddenSize;
            rGradNoSharedWeights += tInSize*tLastNoSharedHiddenSize;
            // output weights
            mplus(aSharedHiddenDims[aHiddenNumber]==1?rGradSharedWeights:rGradNoSharedWeights, aYGrad, tX, tLastHiddenSize);
        } else {
            rGradWeights += tInSize*tLastHiddenSize;
            mplus(rGradWeights, aYGrad, tX, tLastHiddenSize);
        }
        if (SHARE) {
            rGradSharedBiases = rGradSharedWeights+tLastHiddenSize;
            rGradNoSharedBiases = rGradNoSharedWeights+tLastHiddenSize; // output weights, same size
        } else {
            rGradBiases = rGradWeights+tLastHiddenSize;
        }
        for (jint i = 0; i < aHiddenNumber; ++i) {
            if (SHARE) {
                const jint tHiddenDim = aHiddenDims[i];
                const jint tNoSharedHiddenDim = tHiddenDim-aSharedHiddenDims[i];
                rGradSharedBiases += tHiddenDim;
                rGradNoSharedBiases += tNoSharedHiddenDim;
            } else {
                rGradBiases += aHiddenDims[i];
            }
        }
        if (SHARE) {
            // output bias
            *(aSharedHiddenDims[aHiddenNumber]==1?rGradSharedBiases:rGradNoSharedBiases) += aYGrad;
        } else {
            *rGradBiases += aYGrad;
        }
    }
    // begin backward
    tInSize = aHiddenDims[tEnd];
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
        jint tSharedInSize = -1;
        jint tNoSharedInSize = -1;
        if (SHARE) {
            tSharedInSize = aSharedHiddenDims[i+1];
            tNoSharedInSize = tInSize-tSharedInSize;
        }
        if (rGradPara != NULL) {
            jint tWeightSize = tOutSize*tInSize;
            if (SHARE) {
                rGradNoSharedWeights -= tOutSize*tNoSharedInSize;
                rGradNoSharedBiases -= tNoSharedInSize;
                rGradSharedWeights -= tWeightSize;
                rGradSharedBiases -= tInSize;
                // shared last
                rGradWeights = rGradNoSharedWeights;
                rGradBiases = rGradNoSharedBiases;
            } else {
                rGradWeights -= tWeightSize;
                rGradBiases -= tInSize;
            }
            for (jint j = 0; j < tInSize; ++j) {
                if (SHARE) {
                    if (j == tNoSharedInSize) {
                        rGradWeights = rGradSharedWeights + tOutSize*tNoSharedInSize;
                        rGradBiases = rGradSharedBiases;
                    }
                }
                const jdouble tSubGrad3 = tGrad3Before[j];
                rGradBiases[j] += tSubGrad3;
                mplus(rGradWeights, tSubGrad3, tX, tOutSize);
                rGradWeights += tOutSize;
            }
            if (!SHARE) {
                rGradWeights -= tWeightSize;
            }
        }
        for (jint j = 0; j < tOutSize; ++j) {
            if (SHARE) {
                // shared last
                rGrad2[j] = dot(tGrad3Before, tNoSharedWeights, tNoSharedInSize)
                          + dot(tGrad3Before+tNoSharedInSize, tSharedWeights+tNoSharedInSize, tSharedInSize);
                tNoSharedWeights += tNoSharedInSize;
                tSharedWeights += tInSize;
            } else {
                rGrad2[j] = dot(tGrad3Before, tWeights, tInSize);
                tWeights += tInSize;
            }
            rGrad3[j] = rGrad2[j] * tGrad[j];
        }
        tGrad3Before = rGrad3;
        tInSize = tOutSize;
    }
    // to input layer
    jint tSharedInSize = -1;
    jint tNoSharedInSize = -1;
    if (SHARE) {
        tSharedInSize = aSharedHiddenDims[0];
        tNoSharedInSize = tInSize-tSharedInSize;
    }
    if (rGradPara != NULL) {
        if (SHARE) {
            rGradNoSharedWeights -= aInputDim*tNoSharedInSize;
            rGradNoSharedBiases -= tNoSharedInSize;
            rGradSharedWeights -= aInputDim*tInSize;
            rGradSharedBiases -= tInSize;
            // shared last
            rGradWeights = rGradNoSharedWeights;
            rGradBiases = rGradNoSharedBiases;
        } else {
            rGradWeights -= aInputDim*tInSize;
            rGradBiases -= tInSize;
        }
        for (jint j = 0; j < tInSize; ++j) {
            if (SHARE) {
                if (j == tNoSharedInSize) {
                    rGradWeights = rGradSharedWeights + aInputDim*tNoSharedInSize;
                    rGradBiases = rGradSharedBiases;
                }
            }
            const jdouble tSubGrad3 = tGrad3Before[j];
            rGradBiases[j] += tSubGrad3;
            mplus(rGradWeights, tSubGrad3, aX, aInputDim);
            rGradWeights += aInputDim;
        }
    }
    if (rGradX != NULL) {
        for (jint j = 0; j < aInputDim; ++j) {
            if (SHARE) {
                // shared last
                rGradX[j] += dot(tGrad3Before, tNoSharedWeights, tNoSharedInSize)
                           + dot(tGrad3Before+tNoSharedInSize, tSharedWeights+tNoSharedInSize, tSharedInSize);
                tNoSharedWeights += tNoSharedInSize;
                tSharedWeights += tInSize;
            } else {
                rGradX[j] += dot(tGrad3Before, tWeights, tInSize);
            }
            tWeights += tInSize;
        }
    }
}
static inline void backward(jdouble aYGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *rHiddenGrads2, jdouble *rHiddenGrads3) noexcept {
    backward_<JNI_FALSE>(aYGrad, aX, rGradX, rGradPara, NULL, aInputDim, aHiddenDims, NULL, aHiddenNumber, aHiddenWeightsBackward, NULL, aOutputWeight, aHiddenOutputs, aHiddenGrads, rHiddenGrads2, rHiddenGrads3);
}
static inline void backward(jdouble aYGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara, jint aInputDim, jint *aHiddenDims, jint *aSharedHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeightsBackward, jdouble *aSharedHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *rHiddenGrads2, jdouble *rHiddenGrads3) noexcept {
    backward_<JNI_TRUE>(aYGrad, aX, rGradX, rGradPara, rGradSharedPara, aInputDim, aHiddenDims, aSharedHiddenDims, aHiddenNumber, aHiddenWeightsBackward, aSharedHiddenWeightsBackward, aOutputWeight, aHiddenOutputs, aHiddenGrads, rHiddenGrads2, rHiddenGrads3);
}

template <jboolean SHARE>
static void gradBackward_(jdouble *aGradXGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara,
                          jint aInputDim, jint *aHiddenDims, jint *aSharedHiddenDims, jint aHiddenNumber,
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
    jint tColNum = aInputDim;
    if (SHARE) {
        rGradSharedWeights = rGradSharedPara; rGradSharedBiases = rGradSharedPara;
        rGradNoSharedWeights = rGradPara; rGradNoSharedBiases = rGradPara;
    } else {
        rGradWeights = rGradPara; rGradBiases = rGradPara;
    }
    for (jint i = 0; i < aHiddenNumber; ++i) {
        jint tHiddenDim = aHiddenDims[i];
        if (SHARE) {
            jint tNoSharedHiddenDim = tHiddenDim - aSharedHiddenDims[i];
            rGradNoSharedBiases += tColNum*tNoSharedHiddenDim;
            rGradSharedBiases += tColNum*tHiddenDim;
        } else {
            rGradBiases += tColNum*tHiddenDim;
        }
        tColNum = tHiddenDim;
    }
    if (SHARE) {
        rGradNoSharedBiases += aHiddenDims[aHiddenNumber-1]; // output weights, same size
        rGradSharedBiases += aHiddenDims[aHiddenNumber-1];
    } else {
        rGradBiases += aHiddenDims[aHiddenNumber-1];
    }
    /// backward backward
    // diff W0 ij
    jint tInSize = aInputDim;
    jint tOutSize = aHiddenDims[0];
    jint tNoSharedOutSize = -1;
    if (SHARE) {
        tNoSharedOutSize = tOutSize-aSharedHiddenDims[0];
        // shared last
        rGradWeights = rGradNoSharedWeights;
    }
    for (jint i = 0; i < tOutSize; ++i) {
        if (SHARE) {
            if (i == tNoSharedOutSize) {
                rGradWeights = rGradSharedWeights + tNoSharedOutSize*tInSize;
            }
        }
        mplus(rGradWeights, tGrad3[i], aGradXGrad, tInSize);
        rGradWeights += tInSize;
    }
    if (SHARE) {
        rGradNoSharedWeights += tNoSharedOutSize*tInSize;
        rGradSharedWeights += tOutSize*tInSize;
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
        // shared last
        tWeights = tNoSharedWeights;
    }
    for (jint i = 0; i < tOutSize; ++i) {
        if (SHARE) {
            if (i == tNoSharedOutSize) {
                tWeights = tSharedWeights + tNoSharedOutSize*tInSize;
            }
        }
        rGrad5[i] = dot(aGradXGrad, tWeights, tInSize);
        tWeights += tInSize;
    }
    if (SHARE) {
        tNoSharedWeights += tNoSharedOutSize*tInSize;
        tSharedWeights += tOutSize*tInSize;
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
            rGradNoSharedBiases += tNoSharedOutSize;
            rGradSharedBiases += tOutSize;
        } else {
            rGradBiases += tOutSize;
        }
        // diff Wl+1 ij
        tInSize = tOutSize;
        tOutSize = aHiddenDims[l+1];
        if (SHARE) {
            tNoSharedOutSize = tOutSize-aSharedHiddenDims[l+1];
            // shared last
            rGradWeights = rGradNoSharedWeights;
        }
        for (jint i = 0; i < tOutSize; ++i) {
            if (SHARE) {
                if (i == tNoSharedOutSize) {
                    rGradWeights = rGradSharedWeights + tNoSharedOutSize*tInSize;
                }
            }
            mplus(rGradWeights, tGrad3[i], rGrad5, tInSize);
            rGradWeights += tInSize;
        }
        if (SHARE) {
            rGradNoSharedWeights += tNoSharedOutSize*tInSize;
            rGradSharedWeights += tOutSize*tInSize;
        }
        // G^l+1 ik
        if (SHARE) {
            // shared last
            tWeights = tNoSharedWeights;
        }
        jdouble *rGrad6 = rGrad5 + tInSize;
        for (jint i = 0; i < tOutSize; ++i) {
            if (SHARE) {
                if (i == tNoSharedOutSize) {
                    tWeights = tSharedWeights + tNoSharedOutSize*tInSize;
                }
            }
            rGrad6[i] = dot(tWeights, rGrad5, tInSize);
            tWeights += tInSize;
        }
        if (SHARE) {
            tNoSharedWeights += tNoSharedOutSize*tInSize;
            tSharedWeights += tOutSize*tInSize;
        }
        rGrad4 += tInSize;
        rGrad5 += tInSize;
    }
    // Gend i, Wo i
    tOutSize = aHiddenDims[tEnd];
    if (SHARE) {
        tNoSharedOutSize = tOutSize-aSharedHiddenDims[tEnd];
    }
    if (SHARE) {
        rGradWeights = aSharedHiddenDims[aHiddenNumber]==1?rGradSharedWeights:rGradNoSharedWeights; // output weights
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
        rGradNoSharedBiases += tNoSharedOutSize;
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
        tInSize = aHiddenDims[l-1];
        tX -= tInSize;
        jint tSharedOutSize = -1;
        if (SHARE) {
            tSharedOutSize = aSharedHiddenDims[l];
            tNoSharedOutSize = tOutSize-tSharedOutSize;
        }
        if (SHARE) {
            rGradNoSharedWeights -= tNoSharedOutSize*tInSize;
            rGradNoSharedBiases -= tNoSharedOutSize;
            rGradSharedWeights -= tOutSize*tInSize;
            rGradSharedBiases -= tOutSize;
            // shared last
            rGradWeights = rGradNoSharedWeights;
            rGradBiases = rGradNoSharedBiases;
        } else {
            rGradWeights -= tInSize*tOutSize;
            rGradBiases -= tOutSize;
        }
        for (jint i = 0; i < tOutSize; ++i) {
            if (SHARE) {
                if (i == tNoSharedOutSize) {
                    rGradWeights = rGradSharedWeights + tNoSharedOutSize*tInSize;
                    rGradBiases = rGradSharedBiases;
                }
            }
            const jdouble tSubX2 = rX2[i];
            rGradBiases[i] += tSubX2;
            mplus(rGradWeights, tSubX2, tX, tInSize);
            rGradWeights += tInSize;
        }
        if (!SHARE) {
            rGradWeights -= tInSize*tOutSize;
        }
        // Xl-1 ik
        jdouble *rX3 = rX2 + tOutSize;
        for (jint i = 0; i < tInSize; ++i) {
            if (SHARE) {
                // shared last
                rX3[i] = dot(tNoSharedWeights, rX2, tNoSharedOutSize)
                       + dot(tSharedWeights+tNoSharedOutSize, rX2+tNoSharedOutSize, tSharedOutSize);
                tNoSharedWeights += tNoSharedOutSize;
                tSharedWeights += tOutSize;
            } else {
                rX3[i] = dot(tWeights, rX2, tOutSize);
                tWeights += tOutSize;
            }
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
    jint tSharedOutSize = -1;
    if (SHARE) {
        tSharedOutSize = aSharedHiddenDims[0];
        tNoSharedOutSize = tOutSize-tSharedOutSize;
    }
    if (SHARE) {
        rGradNoSharedWeights -= aInputDim*tNoSharedOutSize;
        rGradNoSharedBiases -= tNoSharedOutSize;
        rGradSharedWeights -= aInputDim*tOutSize;
        rGradSharedBiases -= tOutSize;
        // shared last
        rGradWeights = rGradNoSharedWeights;
        rGradBiases = rGradNoSharedBiases;
    } else {
        rGradBiases -= tOutSize;
        rGradWeights -= aInputDim*tOutSize;
    }
    for (jint i = 0; i < tOutSize; ++i) {
        if (SHARE) {
            if (i == tNoSharedOutSize) {
                rGradWeights = rGradSharedWeights + aInputDim*tNoSharedOutSize;
                rGradBiases = rGradSharedBiases;
            }
        }
        const jdouble tSubX2 = rX2[i];
        rGradBiases[i] += tSubX2;
        mplus(rGradWeights, tSubX2, aX, aInputDim);
        rGradWeights += aInputDim;
    }
    if (rGradX != NULL) {
        for (jint j = 0; j < aInputDim; ++j) {
            if (SHARE) {
                // shared last
                rGradX[j] += dot(rX2, tNoSharedWeights, tNoSharedOutSize)
                           + dot(rX2+tNoSharedOutSize, tSharedWeights+tNoSharedOutSize, tSharedOutSize);
                tNoSharedWeights += tNoSharedOutSize;
                tSharedWeights += tOutSize;
            } else {
                rGradX[j] += dot(rX2, tWeights, tOutSize);
                tWeights += tOutSize;
            }
        }
    }
}
static inline void gradBackward(jdouble *aGradXGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *aHiddenGrads2, jdouble *aHiddenGrads3, jdouble *aHiddenGradGrads, jdouble *rHiddenOutputs2, jdouble *rHiddenGrads4, jdouble *rHiddenGrads5) noexcept {
    gradBackward_<JNI_FALSE>(aGradXGrad, aX, rGradX, rGradPara, NULL, aInputDim, aHiddenDims, NULL, aHiddenNumber, aHiddenWeights, NULL, aHiddenWeightsBackward, NULL, aOutputWeight, aHiddenOutputs, aHiddenGrads, aHiddenGrads2, aHiddenGrads3, aHiddenGradGrads, rHiddenOutputs2, rHiddenGrads4, rHiddenGrads5);
}
static inline void gradBackward(jdouble *aGradXGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jdouble *rGradSharedPara, jint aInputDim, jint *aHiddenDims, jint *aSharedHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aSharedHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aSharedHiddenWeightsBackward, jdouble *aOutputWeight, jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *aHiddenGrads2, jdouble *aHiddenGrads3, jdouble *aHiddenGradGrads, jdouble *rHiddenOutputs2, jdouble *rHiddenGrads4, jdouble *rHiddenGrads5) noexcept {
    gradBackward_<JNI_TRUE>(aGradXGrad, aX, rGradX, rGradPara, rGradSharedPara, aInputDim, aHiddenDims, aSharedHiddenDims, aHiddenNumber, aHiddenWeights, aSharedHiddenWeights, aHiddenWeightsBackward, aSharedHiddenWeightsBackward, aOutputWeight, aHiddenOutputs, aHiddenGrads, aHiddenGrads2, aHiddenGrads3, aHiddenGradGrads, rHiddenOutputs2, rHiddenGrads4, rHiddenGrads5);
}

}

#endif //NN_FEED_FORWARD_H