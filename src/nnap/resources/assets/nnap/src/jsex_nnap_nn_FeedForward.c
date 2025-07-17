#include "jsex_nnap_nn_FeedForward.h"
#include "nnap_util.h"

#ifdef __cplusplus
extern "C" {
#endif

static inline jdouble silu(jdouble aX) {
    return aX / (1.0 + exp(-aX));
}
static inline jdouble siluGrad(jdouble aX, jdouble *rGrad) {
    jdouble tSigmoid = 1.0 / (1.0 + exp(-aX));
    *rGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    return aX * tSigmoid;
}
static inline jdouble siluGradGrad(jdouble aX, jdouble *rGrad, jdouble *rGradGrad) {
    jdouble tSigmoid = 1.0 / (1.0 + exp(-aX));
    *rGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    *rGradGrad = tSigmoid * (1 - tSigmoid) * (2 + aX * (1 - tSigmoid - tSigmoid));
    return aX * tSigmoid;
}

static inline jdouble forward(jdouble *aX, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber,
                              jdouble *aHiddenWeights, jdouble *aHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias,
                              jdouble *rHiddenOutputs, jdouble *rHiddenGrads, jdouble *rHiddenGradGrads) {
    jdouble *tInput = aX;
    jdouble *rOutput = rHiddenOutputs;
    jdouble *rGrad = rHiddenGrads;
    jdouble *rGradGrad = rHiddenGradGrads;
    jdouble *tWeights = aHiddenWeights;
    jdouble *tBiases = aHiddenBiases;
    jint tInSize = aInputDim;
    const jint tEnd = aHiddenNumber - 1;
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        for (jint j = 0; j < tOutSize; ++j) {
            jdouble rDot = dotAB_jse(tInput, tWeights, tInSize) + tBiases[j];
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
            tWeights += tInSize;
        }
        tInput = rOutput;
        rOutput += tOutSize;
        if (rGrad != NULL) rGrad += tOutSize;
        if (rGradGrad != NULL) rGradGrad += tOutSize;
        tBiases += tOutSize;
        tInSize = tOutSize;
    }
    // special optimize for last layer
    jdouble rOut = aOutputBias;
    const jint tOutSize = aHiddenDims[tEnd];
    for (jint j = 0; j < tOutSize; ++j) {
        jdouble rDot = dotAB_jse(tInput, tWeights, tInSize) + tBiases[j];
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
        tWeights += tInSize;
    }
    return rOut;
}
static inline void backward(jdouble aYGrad, jdouble *aX, jdouble *rGradX, jdouble *rGradPara,
                            jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight,
                            jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *rHiddenGrads2, jdouble *rHiddenGrads3) {
    // switch to last layer
    const jint tEnd = aHiddenNumber - 1;
    jdouble *tGrad = aHiddenGrads;
    jdouble *rGrad2 = rHiddenGrads2;
    jdouble *rGrad3 = rHiddenGrads3;
    jdouble *tX = aHiddenOutputs;
    jdouble *rGradWeights = NULL, *rGradBiases = NULL;
    jint tInSize = -1;
    if (rGradPara != NULL) {
        rGradWeights = rGradPara;
        tInSize = aInputDim;
    }
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad += tOutSize;
        rGrad2 += tOutSize;
        rGrad3 += tOutSize;
        tX += tOutSize;
        if (rGradPara != NULL) {
            rGradWeights += tInSize*tOutSize;
            tInSize = tOutSize;
        }
    }
    if (rGradPara != NULL) {
        jint tLastHiddenSize = aHiddenDims[tEnd];
        rGradWeights += tInSize*tLastHiddenSize;
        jdouble *rGradOutWeights = rGradWeights;
        for (jint j = 0; j < tLastHiddenSize; ++j) {
            rGradOutWeights[j] += aYGrad*tX[j];
        }
        rGradBiases = rGradOutWeights+tLastHiddenSize;
        for (jint i = 0; i < aHiddenNumber; ++i) {
            rGradBiases += aHiddenDims[i];
        }
        *rGradBiases += aYGrad;
    }
    // begin backward
    tInSize = aHiddenDims[tEnd];
    for (jint j = 0; j < tInSize; ++j) {
        rGrad3[j] = aYGrad * tGrad[j] * aOutputWeight[j];
    }
    jdouble *tWeights = aHiddenWeightsBackward;
    jdouble *tGrad3Before = rGrad3;
    for (jint i = tEnd-1; i >= 0; --i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad -= tOutSize;
        rGrad2 -= tOutSize;
        rGrad3 -= tOutSize;
        tX -= tOutSize;
        if (rGradPara != NULL) {
            jint tWeightSize = tOutSize*tInSize;
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
            rGradWeights -= tWeightSize;
        }
        for (jint j = 0; j < tOutSize; ++j) {
            rGrad2[j] = dotAB_jse(tGrad3Before, tWeights, tInSize);
            rGrad3[j] = rGrad2[j] * tGrad[j];
            tWeights += tInSize;
        }
        tGrad3Before = rGrad3;
        tInSize = tOutSize;
    }
    // to input layer
    if (rGradPara != NULL) {
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
        for (jint j = 0; j < aInputDim; ++j) {
            rGradX[j] = dotAB_jse(tGrad3Before, tWeights, tInSize);
            tWeights += tInSize;
        }
    }
}
static inline void gradBackward(jdouble *aGradXGrad, jdouble *aX, jdouble *rGradPara,
                                jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight,
                                jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *aHiddenGrads2, jdouble *aHiddenGrads3, jdouble *aHiddenGradGrads,
                                jdouble *rHiddenOutputs2, jdouble *rHiddenGrads4, jdouble *rHiddenGrads5) {
    // ptr init
    jdouble *tGrad = aHiddenGrads;
    jdouble *tGrad2 = aHiddenGrads2;
    jdouble *tGrad3 = aHiddenGrads3;
    jdouble *tGradGrad = aHiddenGradGrads;
    jdouble *tX = aHiddenOutputs;
    jdouble *rX2 = rHiddenOutputs2;
    jdouble *rGrad4 = rHiddenGrads4;
    jdouble *rGrad5 = rHiddenGrads5;
    jdouble *rGradWeights = rGradPara;
    jdouble *rGradBiases = rGradPara;
    jint tColNum = aInputDim;
    for (jint i = 0; i < aHiddenNumber; ++i) {
        jint tHiddenDim = aHiddenDims[i];
        rGradBiases += tColNum * tHiddenDim;
        tColNum = tHiddenDim;
    }
    rGradBiases += aHiddenDims[aHiddenNumber-1];
    /// backward backward
    // diff W0 ij
    jint tInSize = aInputDim;
    jint tOutSize = aHiddenDims[0];
    for (jint i = 0; i < tOutSize; ++i) {
        jdouble tSubGrad3 = tGrad3[i];
        for (jint j = 0; j < tInSize; ++j) {
            rGradWeights[j] += aGradXGrad[j]*tSubGrad3;
        }
        rGradWeights += tInSize;
    }
    // G^0 i
    jdouble *tWeights = aHiddenWeights;
    for (jint i = 0; i < tOutSize; ++i) {
        rGrad5[i] = dotAB_jse(aGradXGrad, tWeights, tInSize);
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
        rGradBiases += tOutSize;
        // diff Wl+1 ij
        tInSize = tOutSize;
        tOutSize = aHiddenDims[l+1];
        for (jint i = 0; i < tOutSize; ++i) {
            jdouble tSubGrad3 = tGrad3[i];
            for (jint j = 0; j < tInSize; ++j) {
                rGradWeights[j] += tSubGrad3 * rGrad5[j];
            }
            rGradWeights += tInSize;
        }
        // G^l+1 ik
        jdouble *rGrad6 = rGrad5 + tInSize;
        for (jint i = 0; i < tOutSize; ++i) {
            rGrad6[i] = dotAB_jse(tWeights, rGrad5, tInSize);
            tWeights += tInSize;
        }
        rGrad4 += tInSize;
        rGrad5 += tInSize;
    }
    // Gend i, Wo i
    tOutSize = aHiddenDims[tEnd];
    for (jint i = 0; i < tOutSize; ++i) {
        rGrad4[i] = aOutputWeight[i] * rGrad5[i];
        rGradWeights[i] += tGrad[i] * rGrad5[i];
    }
    /// backward forward
    // X^end ik
    for (jint i = 0; i < tOutSize; ++i) {
        rX2[i] = tGradGrad[i] * rGrad4[i];
    }
    rGradBiases += tOutSize;
    tWeights = aHiddenWeightsBackward;
    for (int l = tEnd; l > 0; --l) {
        // bl i, Wl ij
        tInSize = aHiddenDims[l-1];
        tX -= tInSize;
        rGradBiases -= tOutSize;
        rGradWeights -= tInSize*tOutSize;
        for (jint i = 0; i < tOutSize; ++i) {
            jdouble tSubX2 = rX2[i];
            rGradBiases[i] += tSubX2;
            for (jint j = 0; j < tInSize; ++j) {
                rGradWeights[j] += tX[j] * tSubX2;
            }
            rGradWeights += tInSize;
        }
        rGradWeights -= tInSize*tOutSize;
        // Xl-1 ik
        jdouble *rX3 = rX2 + tOutSize;
        for (jint i = 0; i < tInSize; ++i) {
            rX3[i] = dotAB_jse(tWeights, rX2, tOutSize);
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
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_FeedForward_forward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias,
        jdoubleArray rHiddenOutputs, jint aShiftOutputs, jdoubleArray rHiddenGrads, jint aShiftGrads) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aHiddenBiases);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    jdouble *tHiddenGrads = rHiddenGrads==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rHiddenGrads);
    
    jdouble tOut = forward(tX+aShiftX, aInputDim, tHiddenDims, aHiddenNumber,
                           tHiddenWeights, tHiddenBiases, tOutputWeight, aOutputBias,
                           tHiddenOutputs+aShiftOutputs, tHiddenGrads==NULL?NULL:(tHiddenGrads+aShiftGrads), NULL);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, rHiddenGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    if (rHiddenGrads!=NULL) releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, 0);
    
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_FeedForward_forwardGrad1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias,
        jdoubleArray rHiddenOutputs, jint aShiftOutputs, jdoubleArray rHiddenGrads, jint aShiftGrads,
        jdoubleArray rHiddenGrads2, jint aShiftGrads2, jdoubleArray rHiddenGrads3, jint aShiftGrads3, jdoubleArray rHiddenGradGrads, jint aShiftGradGrads) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradX = (jdouble *)getJArrayBuf(aEnv, rGradX);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aHiddenBiases);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads);
    jdouble *tHiddenGrads2 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads2);
    jdouble *tHiddenGrads3 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads3);
    jdouble *tHiddenGradGrads = rHiddenGradGrads==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rHiddenGradGrads);
    
    jdouble tOut = forward(tX+aShiftX, aInputDim, tHiddenDims, aHiddenNumber,
                           tHiddenWeights, tHiddenBiases, tOutputWeight, aOutputBias,
                           tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads, tHiddenGradGrads==NULL?NULL:(tHiddenGradGrads+aShiftGradGrads));
    
    backward(1.0, tX+aShiftX, tGradX+aShiftGradX, NULL,
             aInputDim, tHiddenDims, aHiddenNumber, tHiddenWeightsBackward, tOutputWeight,
             tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads,
             tHiddenGrads2+aShiftGrads2, tHiddenGrads3+aShiftGrads3);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradX, tGradX, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    releaseJArrayBuf(aEnv, rHiddenGrads2, tHiddenGrads2, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    releaseJArrayBuf(aEnv, rHiddenGrads3, tHiddenGrads3, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    if (rHiddenGradGrads!=NULL) releaseJArrayBuf(aEnv, rHiddenGradGrads, tHiddenGradGrads, 0);
    
    return tOut;
}

JNIEXPORT void JNICALL Java_jsex_nnap_nn_FeedForward_backward1(JNIEnv *aEnv, jclass aClazz,
        jdouble aYGrad, jdoubleArray aX, jint aShiftX, jdoubleArray rGradPara, jint aShiftGradPara, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeightsBackward, jdoubleArray aOutputWeight,
        jdoubleArray aHiddenOutputs, jint aShiftOutputs, jdoubleArray aHiddenGrads, jint aShiftGrads,
        jdoubleArray rHiddenGrads2, jdoubleArray rHiddenGrads3) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, aHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads);
    jdouble *tHiddenGrads2 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads2);
    jdouble *tHiddenGrads3 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads3);
    
    backward(aYGrad, tX+aShiftX, NULL, tGradPara+aShiftGradPara,
             aInputDim, tHiddenDims, aHiddenNumber, tHiddenWeightsBackward, tOutputWeight,
             tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads, tHiddenGrads2, tHiddenGrads3);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenOutputs, tHiddenOutputs, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads, tHiddenGrads, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenGrads2, tHiddenGrads2, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenGrads3, tHiddenGrads3, JNI_ABORT); // buffer only
}

JNIEXPORT void JNICALL Java_jsex_nnap_nn_FeedForward_gradBackward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aGradXGrad, jint aShiftGradXGrad, jdoubleArray aX, jint aShiftX, jdoubleArray rGradPara, jint aShiftGradPara,
        jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber, jdoubleArray aHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aOutputWeight,
        jdoubleArray aHiddenOutputs, jint aShiftOutputs, jdoubleArray aHiddenGrads, jint aShiftGrads,
        jdoubleArray aHiddenGrads2, jint aShiftGrads2, jdoubleArray aHiddenGrads3, jint aShiftGrads3, jdoubleArray aHiddenGradGrads, jint aShiftGradGrads,
        jdoubleArray rHiddenOutputs2, jdoubleArray rHiddenGrads4, jdoubleArray rHiddenGrads5) {
    // java array init
    jdouble *tGradXGrad = (jdouble *)getJArrayBuf(aEnv, aGradXGrad);
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, aHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads);
    jdouble *tHiddenGrads2 = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads2);
    jdouble *tHiddenGrads3 = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads3);
    jdouble *tHiddenGradGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGradGrads);
    jdouble *tHiddenOutputs2 = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs2);
    jdouble *tHiddenGrads4 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads4);
    jdouble *tHiddenGrads5 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads5);
    
    gradBackward(tGradXGrad+aShiftGradXGrad, tX+aShiftX, tGradPara+aShiftGradPara,
                 aInputDim, tHiddenDims, aHiddenNumber,
                 tHiddenWeights, tHiddenWeightsBackward, tOutputWeight,
                 tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads,
                 tHiddenGrads2+aShiftGrads2, tHiddenGrads3+aShiftGrads3, tHiddenGradGrads+aShiftGradGrads,
                 tHiddenOutputs2, tHiddenGrads4, tHiddenGrads5);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradXGrad, tGradXGrad, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenOutputs, tHiddenOutputs, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads, tHiddenGrads, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads2, tHiddenGrads2, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads3, tHiddenGrads3, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGradGrads, tHiddenGradGrads, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs2, tHiddenOutputs2, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenGrads4, tHiddenGrads4, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenGrads5, tHiddenGrads5, JNI_ABORT); // buffer only
}

#ifdef __cplusplus
}
#endif
