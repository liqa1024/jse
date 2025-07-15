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
    jdouble *tOutput = rHiddenOutputs;
    jdouble *tGrad = rHiddenGrads;
    jdouble *tGradGrad = rHiddenGradGrads;
    jdouble *tWeights = aHiddenWeights;
    jdouble *tBiases = aHiddenBiases;
    jint tInSize = aInputDim;
    const jint tEnd = aHiddenNumber - 1;
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        for (jint j = 0; j < tOutSize; ++j) {
            jdouble rDot = dotAB_jse(tInput, tWeights, tInSize) + tBiases[j];
            if (tGrad == NULL) {
                tOutput[j] = silu(rDot);
            } else
            if (tGradGrad == NULL) {
                jdouble rGradDot;
                tOutput[j] = siluGrad(rDot, &rGradDot);
                tGrad[j] = rGradDot;
            } else {
                jdouble rGradDot, rGradGradDot;
                tOutput[j] = siluGradGrad(rDot, &rGradDot, &rGradGradDot);
                tGrad[j] = rGradDot;
                tGradGrad[j] = rGradGradDot;
            }
            tWeights += tInSize;
        }
        tInput = tOutput;
        tOutput += tOutSize;
        if (tGrad != NULL) tGrad += tOutSize;
        if (tGradGrad != NULL) tGradGrad += tOutSize;
        tBiases += tOutSize;
        tInSize = tOutSize;
    }
    // special optimize for last layer
    jdouble rOut = aOutputBias;
    const jint tOutSize = aHiddenDims[tEnd];
    for (jint j = 0; j < tOutSize; ++j) {
        jdouble rDot = dotAB_jse(tInput, tWeights, tInSize) + tBiases[j];
        if (tGrad == NULL) {
            tOutput[j] = silu(rDot);
        } else
        if (tGradGrad == NULL) {
            jdouble rGradDot;
            tOutput[j] = siluGrad(rDot, &rGradDot);
            tGrad[j] = rGradDot;
        } else {
            jdouble rGradDot, rGradGradDot;
            tOutput[j] = siluGradGrad(rDot, &rGradDot, &rGradGradDot);
            tGrad[j] = rGradDot;
            tGradGrad[j] = rGradGradDot;
        }
        jdouble tOutputWeight = aOutputWeight[j];
        rOut += tOutput[j] * tOutputWeight;
        tWeights += tInSize;
    }
    return rOut;
}
static inline void backward(jdouble *aX, jdouble *rGradX, jdouble *rGradPara,
                            jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight,
                            jdouble *rHiddenOutputs, jdouble *rHiddenGrads, jdouble *rHiddenGrads2, jdouble *rHiddenGrads3) {
    // switch to last layer
    const jint tEnd = aHiddenNumber - 1;
    jdouble *tGrad = rHiddenGrads;
    jdouble *tGrad2 = rHiddenGrads2!=NULL ? rHiddenGrads2 : rHiddenOutputs;
    jdouble *tGrad3 = rHiddenGrads3!=NULL ? rHiddenGrads3 : rHiddenOutputs;
    jdouble *tX = rHiddenOutputs;
    jdouble *rGradWeights = NULL, *rGradBiases = NULL;
    jint tInSize = -1;
    if (rGradPara != NULL) {
        rGradWeights = rGradPara;
        tInSize = aInputDim;
    }
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad += tOutSize;
        tGrad2 += tOutSize;
        tGrad3 += tOutSize;
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
            rGradOutWeights[j] += tX[j]; // rGradOutWeights is the last output
        }
        rGradBiases = rGradOutWeights+tLastHiddenSize;
        for (jint i = 0; i < aHiddenNumber; ++i) {
            rGradBiases += aHiddenDims[i];
        }
        *rGradBiases += 1.0; // rGradOutBias always 1
    }
    // begin backward
    tInSize = aHiddenDims[tEnd];
    for (jint j = 0; j < tInSize; ++j) {
        tGrad3[j] = tGrad[j] * aOutputWeight[j];
    }
    jdouble *tWeights = aHiddenWeightsBackward;
    jdouble *tGrad3Before = tGrad3;
    for (jint i = tEnd-1; i >= 0; --i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad -= tOutSize;
        tGrad2 -= tOutSize;
        tGrad3 -= tOutSize;
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
            tGrad2[j] = dotAB_jse(tGrad3Before, tWeights, tInSize);
            tGrad3[j] = tGrad2[j] * tGrad[j];
            tWeights += tInSize;
        }
        tGrad3Before = tGrad3;
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
static inline void gradBackward(jdouble *aX, jdouble *rGradXGradPara,
                                jint aInputDim, jint *aHiddenDims, jint aHiddenNumber, jdouble *aHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aOutputWeight,
                                jdouble *aHiddenOutputs, jdouble *aHiddenGrads, jdouble *aHiddenGrads2, jdouble *aHiddenGrads3, jdouble *aHiddenGradGrads,
                                jdouble *rHiddenMatOutputGrads, jdouble *rHiddenMatGradGrads, jdouble *rHiddenMatGradGrads2) {
    // size init
    jint tHiddenWeightsSize = 0;
    jint tHiddenBiasesSize = 0;
    jint tColNum = aInputDim;
    for (jint i = 0; i < aHiddenNumber; ++i) {
        jint tHiddenDim = aHiddenDims[i];
        tHiddenWeightsSize += tColNum * tHiddenDim;
        tHiddenBiasesSize += tHiddenDim;
        tColNum = tHiddenDim;
    }
    jint tOutputWeightSize = aHiddenDims[aHiddenNumber-1];
    jint tParaSize = tHiddenWeightsSize+tOutputWeightSize + tHiddenBiasesSize+1;
    // ptr init
    jdouble *tGrad = aHiddenGrads;
    jdouble *tGrad2 = aHiddenGrads2;
    jdouble *tGrad3 = aHiddenGrads3;
    jdouble *tGradGrad = aHiddenGradGrads;
    jdouble *tX = aHiddenOutputs;
    jdouble *rMatXGrad = rHiddenMatOutputGrads;
    jdouble *rMatGradGrad = rHiddenMatGradGrads;
    jdouble *rMatGradGrad2 = rHiddenMatGradGrads2;
    
    /// backward backward
    // diff W0 ij
    jint tShiftParaWeight = 0;
    jint tShiftParaBias = 0;
    jint tInSize = aInputDim;
    jint tOutSize = aHiddenDims[0];
    for (jint i = 0; i < tOutSize; ++i) {
        jdouble tSubGrad3 = tGrad3[i];
        jint tShift = i * tInSize;
        for (jint j = 0; j < tInSize; ++j) {
            rGradXGradPara[j*tParaSize + tShift + j] += tSubGrad3;
        }
    }
    // G^0 ik
    jdouble *tWeights = aHiddenWeights;
    for (jint i = 0; i < tOutSize; ++i) {
        for (jint k = 0; k < aInputDim; ++k) {
            rMatGradGrad2[i + k*tOutSize] = tWeights[k];
        }
        tWeights += aInputDim;
    }
    const jint tEnd = aHiddenNumber - 1;
    for (jint l = 0; l < tEnd; ++l) {
        // Gl ik, G~l ik
        for (jint k = 0; k < aInputDim; ++k) {
            for (jint i = 0; i < tOutSize; ++i) {
                rMatGradGrad[i] = tGrad2[i] * rMatGradGrad2[i];
                rMatGradGrad2[i] = tGrad[i] * rMatGradGrad2[i];
            }
            rMatGradGrad += tOutSize;
            rMatGradGrad2 += tOutSize;
        }
        rMatGradGrad2 -= tOutSize*aInputDim;
        tGrad += tOutSize;
        tGrad2 += tOutSize;
        tGrad3 += tOutSize;
        tGradGrad += tOutSize;
        tX += tOutSize;
        // diff Wl+1 ij
        tShiftParaWeight += tInSize*tOutSize;
        tShiftParaBias += tOutSize;
        tInSize = tOutSize;
        tOutSize = aHiddenDims[l+1];
        for (jint k = 0; k < aInputDim; ++k) {
            jdouble *tGradXGradWeight = rGradXGradPara + k*tParaSize + tShiftParaWeight;
            for (jint i = 0; i < tOutSize; ++i) {
                jdouble tSubGrad3 = tGrad3[i];
                for (jint j = 0; j < tInSize; ++j) {
                    tGradXGradWeight[j] += tSubGrad3 * rMatGradGrad2[j];
                }
                tGradXGradWeight += tInSize;
            }
            rMatGradGrad2 += tInSize;
        }
        // G^l+1 ik
        jdouble *rMatGradGrad3 = rMatGradGrad2;
        rMatGradGrad2 -= tInSize*aInputDim;
        jdouble *tWeights_ = tWeights;
        for (jint k = 0; k < aInputDim; ++k) {
            for (jint i = 0; i < tOutSize; ++i) {
                rMatGradGrad3[i] = dotAB_jse(tWeights_, rMatGradGrad2, tInSize);
                tWeights_ += tInSize;
            }
            tWeights_ = tWeights;
            rMatGradGrad2 += tInSize;
            rMatGradGrad3 += tOutSize;
        }
        tWeights += tOutSize*tInSize;
    }
    // Gend ik, Wo ik
    tOutSize = aHiddenDims[tEnd];
    for (jint k = 0; k < aInputDim; ++k) {
        jdouble *tGradXGradWeight = rGradXGradPara + k*tParaSize + tHiddenWeightsSize;
        for (jint i = 0; i < tOutSize; ++i) {
            rMatGradGrad[i] = aOutputWeight[i] * rMatGradGrad2[i];
            tGradXGradWeight[i] += tGrad[i] * rMatGradGrad2[i];
        }
        rMatGradGrad += tOutSize;
        rMatGradGrad2 += tOutSize;
    }
    rMatGradGrad -= tOutSize*aInputDim;
    /// backward forward
    // X^end ik
    for (jint k = 0; k < aInputDim; ++k) {
        for (jint i = 0; i < tOutSize; ++i) {
            rMatXGrad[i] = tGradGrad[i] * rMatGradGrad[i];
        }
        rMatXGrad += tOutSize;
        rMatGradGrad += tOutSize;
    }
    rMatXGrad -= tOutSize*aInputDim;
    rMatGradGrad -= tOutSize*aInputDim;
    tWeights = aHiddenWeightsBackward;
    tShiftParaWeight = tHiddenWeightsSize;
    tShiftParaBias = tHiddenWeightsSize+tOutputWeightSize + tHiddenBiasesSize;
    for (int l = tEnd; l > 0; --l) {
        // bl ik, Wl ijk
        tInSize = aHiddenDims[l-1];
        tGrad -= tInSize;
        tGradGrad -= tInSize;
        tX -= tInSize;
        tShiftParaWeight -= tInSize*tOutSize;
        tShiftParaBias -= tOutSize;
        for (jint k = 0; k < aInputDim; ++k) {
            jdouble *tGradXGradWeight = rGradXGradPara + k*tParaSize + tShiftParaWeight;
            jdouble *tGradXGradBias = rGradXGradPara + k*tParaSize + tShiftParaBias;
            for (jint i = 0; i < tOutSize; ++i) {
                jdouble tSubMatXGrad = rMatXGrad[i];
                tGradXGradBias[i] += tSubMatXGrad;
                for (jint j = 0; j < tInSize; ++j) {
                    tGradXGradWeight[j] += tX[j] * tSubMatXGrad;
                }
                tGradXGradWeight += tInSize;
            }
            rMatXGrad += tOutSize;
        }
        // Xl-1 ik
        jdouble *rMatXGrad2 = rMatXGrad;
        rMatXGrad -= tOutSize*aInputDim;
        jdouble *tWeights_ = tWeights;
        for (jint k = 0; k < aInputDim; ++k) {
            for (jint i = 0; i < tInSize; ++i) {
                rMatXGrad2[i] = dotAB_jse(tWeights_, rMatXGrad, tOutSize);
                tWeights_ += tOutSize;
            }
            tWeights_ = tWeights;
            rMatXGrad += tOutSize;
            rMatXGrad2 += tInSize;
        }
        tWeights += tOutSize*tInSize;
        // X^l-1 ik
        tOutSize = tInSize;
        rMatGradGrad -= tOutSize*aInputDim;
        for (jint k = 0; k < aInputDim; ++k) {
            for (jint i = 0; i < tOutSize; ++i) {
                rMatXGrad[i] = tGradGrad[i]*rMatGradGrad[i] + tGrad[i]*rMatXGrad[i];
            }
            rMatXGrad += tOutSize;
            rMatGradGrad += tOutSize;
        }
        rMatXGrad -= tOutSize*aInputDim;
        rMatGradGrad -= tOutSize*aInputDim;
    }
    // b0 ik, W0 ijk
    for (jint k = 0; k < aInputDim; ++k) {
        jdouble *tGradXGradWeight = rGradXGradPara + k*tParaSize;
        jdouble *tGradXGradBias = rGradXGradPara + k*tParaSize + tHiddenWeightsSize+tOutputWeightSize;
        for (jint i = 0; i < tOutSize; ++i) {
            jdouble tSubMatXGrad = rMatXGrad[i];
            tGradXGradBias[i] += tSubMatXGrad;
            for (jint j = 0; j < aInputDim; ++j) {
                tGradXGradWeight[j] += aX[j] * tSubMatXGrad;
            }
            tGradXGradWeight += aInputDim;
        }
        rMatXGrad += tOutSize;
    }
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_FeedForward_forward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias,
        jdoubleArray rHiddenOutputs, jdoubleArray rHiddenGrads) {
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
                           tHiddenOutputs, tHiddenGrads, NULL);
    
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
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX, jdoubleArray rGradPara, jint aShiftGradPara, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias,
        jdoubleArray rHiddenOutputs, jdoubleArray rHiddenGrads, jdoubleArray rHiddenGrads2, jdoubleArray rHiddenGrads3, jdoubleArray rHiddenGradGrads) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradX = (jdouble *)getJArrayBuf(aEnv, rGradX);
    jdouble *tGradPara = rGradPara==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rGradPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aHiddenBiases);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads);
    jdouble *tHiddenGrads2 = rHiddenGrads2==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rHiddenGrads2);
    jdouble *tHiddenGrads3 = rHiddenGrads3==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rHiddenGrads3);
    jdouble *tHiddenGradGrads = rHiddenGradGrads==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rHiddenGradGrads);
    
    jdouble tOut = forward(tX+aShiftX, aInputDim, tHiddenDims, aHiddenNumber,
                           tHiddenWeights, tHiddenBiases, tOutputWeight, aOutputBias,
                           tHiddenOutputs, tHiddenGrads, tHiddenGradGrads);
    
    backward(tX+aShiftX, tGradX+aShiftGradX, tGradPara==NULL?NULL:(tGradPara+aShiftGradPara),
             aInputDim, tHiddenDims, aHiddenNumber, tHiddenWeightsBackward, tOutputWeight,
             tHiddenOutputs, tHiddenGrads, tHiddenGrads2, tHiddenGrads3);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradX, tGradX, 0);
    if (rGradPara!=NULL) releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    if (rHiddenGrads2!=NULL) releaseJArrayBuf(aEnv, rHiddenGrads2, tHiddenGrads2, 0);
    if (rHiddenGrads3!=NULL) releaseJArrayBuf(aEnv, rHiddenGrads3, tHiddenGrads3, 0);
    if (rHiddenGradGrads!=NULL) releaseJArrayBuf(aEnv, rHiddenGradGrads, tHiddenGradGrads, 0);
    
    return tOut;
}

JNIEXPORT void JNICALL Java_jsex_nnap_nn_FeedForward_backward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradPara, jint aShiftGradPara, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeightsBackward, jdoubleArray aOutputWeight,
        jdoubleArray rHiddenOutputs, jdoubleArray rHiddenGrads) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads);
    
    backward(tX+aShiftX, NULL, tGradPara+aShiftGradPara,
             aInputDim, tHiddenDims, aHiddenNumber, tHiddenWeightsBackward, tOutputWeight,
             tHiddenOutputs, tHiddenGrads, NULL, NULL);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, JNI_ABORT); // buffer only
}

JNIEXPORT void JNICALL Java_jsex_nnap_nn_FeedForward_gradBackward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jdoubleArray aGradX, jint aShiftGradX, jdoubleArray rGradXGradPara, jint aShiftGradXGradPara,
        jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber, jdoubleArray aHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aOutputWeight,
        jdoubleArray aHiddenOutputs, jdoubleArray aHiddenGrads, jdoubleArray aHiddenGrads2, jdoubleArray aHiddenGrads3, jdoubleArray aHiddenGradGrads,
        jdoubleArray rHiddenMatOutputGrads, jdoubleArray rHiddenMatGradGrads, jdoubleArray rHiddenMatGradGrads2) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradX = (jdouble *)getJArrayBuf(aEnv, aGradX);
    jdouble *tGradXGradPara = (jdouble *)getJArrayBuf(aEnv, rGradXGradPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, aHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads);
    jdouble *tHiddenGrads2 = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads2);
    jdouble *tHiddenGrads3 = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads3);
    jdouble *tHiddenGradGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGradGrads);
    jdouble *tHiddenMatOutputGrads = (jdouble *)getJArrayBuf(aEnv, rHiddenMatOutputGrads);
    jdouble *tHiddenMatGradGrads = (jdouble *)getJArrayBuf(aEnv, rHiddenMatGradGrads);
    jdouble *tHiddenMatGradGrads2 = (jdouble *)getJArrayBuf(aEnv, rHiddenMatGradGrads2);
    
    gradBackward(tX+aShiftX, tGradXGradPara+aShiftGradXGradPara,
                 aInputDim, tHiddenDims, aHiddenNumber,
                 tHiddenWeights, tHiddenWeightsBackward, tOutputWeight,
                 tHiddenOutputs, tHiddenGrads, tHiddenGrads2, tHiddenGrads3, tHiddenGradGrads,
                 tHiddenMatOutputGrads, tHiddenMatGradGrads, tHiddenMatGradGrads2);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradX, tGradX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradXGradPara, tGradXGradPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenOutputs, tHiddenOutputs, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads, tHiddenGrads, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads2, tHiddenGrads2, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads3, tHiddenGrads3, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGradGrads, tHiddenGradGrads, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenMatOutputGrads, tHiddenMatOutputGrads, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenMatGradGrads, tHiddenMatGradGrads, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenMatGradGrads2, tHiddenMatGradGrads2, JNI_ABORT); // buffer only
}

#ifdef __cplusplus
}
#endif
