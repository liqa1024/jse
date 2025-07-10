#include "jsex_nnap_nn_FeedForward.h"
#include "nnap_util.h"

#ifdef __cplusplus
extern "C" {
#endif

static inline jdouble silu(jdouble aX) {
    return aX / (1.0 + exp(-aX));
}
static inline jdouble siluGrad(jdouble aX, jdouble *aGrad) {
    jdouble tSigmoid = 1.0 / (1.0 + exp(-aX));
    *aGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    return aX * tSigmoid;
}

static inline jdouble forward(jdouble *aX, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber,
                             jdouble *aHiddenWeights, jdouble *aHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias,
                             jdouble *rHiddenOutputs, jdouble *rHiddenGrads) {
    jdouble *tInput = aX;
    jdouble *tOutput = rHiddenOutputs;
    jdouble *tGrad = rHiddenGrads;
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
            } else {
                jdouble rGradDot;
                tOutput[j] = siluGrad(rDot, &rGradDot);
                tGrad[j] = rGradDot;
            }
            tWeights += tInSize;
        }
        tInput = tOutput;
        tOutput += tOutSize;
        if (tGrad != NULL) tGrad += tOutSize;
        tBiases += tOutSize;
        tInSize = tOutSize;
    }
    // special optimize for last layer
    jdouble rOut = aOutputBias;
    const jint tOutSize = aHiddenDims[tEnd];
    for (jint j = 0; j < tOutSize; ++j) {
        jdouble rDot = dotAB_jse(tInput, tWeights, tInSize) + tBiases[j];
        jdouble tOutputWeight = aOutputWeight[j];
        if (tGrad == NULL) {
            tOutput[j] = silu(rDot);
            rOut += tOutput[j] * tOutputWeight;
        } else {
            jdouble rGradDot;
            tOutput[j] = siluGrad(rDot, &rGradDot);
            rOut += tOutput[j] * tOutputWeight;
            tGrad[j] = rGradDot * tOutputWeight;
        }
        tWeights += tInSize;
    }
    return rOut;
}
static inline jdouble backward(jdouble *aX, jdouble *rGradX, jdouble *rGradPara, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber,
                               jdouble *aHiddenWeights, jdouble *aHiddenWeightsBackward, jdouble *aHiddenBiases, jdouble *aOutputWeight, jdouble aOutputBias,
                               jdouble *rHiddenOutputs, jdouble *rHiddenGrads) {
    jdouble tOut = forward(aX, aInputDim, aHiddenDims, aHiddenNumber,
                           aHiddenWeights, aHiddenBiases, aOutputWeight, aOutputBias,
                           rHiddenOutputs, rHiddenGrads);
    // switch to last layer
    const jint tEnd = aHiddenNumber - 1;
    jdouble *tGrad = rHiddenGrads;
    jdouble *tOutput = rHiddenOutputs;
    jdouble *rGradWeights = NULL, *rGradBiases = NULL;
    jint tInSize = -1;
    if (rGradPara != NULL) {
        rGradWeights = rGradPara;
        tInSize = aInputDim;
    }
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad += tOutSize;
        tOutput += tOutSize;
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
            rGradOutWeights[j] = tOutput[j]; // rGradOutWeights is the last output
        }
        rGradBiases = rGradOutWeights+tLastHiddenSize;
        for (jint i = 0; i < aHiddenNumber; ++i) {
            rGradBiases += aHiddenDims[i];
        }
        *rGradBiases = 1.0; // rGradOutBias always 1
    }
    // begin backward, last layer has been specially optimized
    jdouble *tInput = tGrad;
    jdouble *tWeights = aHiddenWeightsBackward;
    tInSize = aHiddenDims[tEnd];
    for (jint i = tEnd-1; i >= 0; --i) {
        const jint tOutSize = aHiddenDims[i];
        tGrad -= tOutSize;
        tOutput -= tOutSize;
        if (rGradPara != NULL) {
            jint tWeightSize = tOutSize*tInSize;
            rGradWeights -= tWeightSize;
            rGradBiases -= tInSize;
            for (jint j = 0; j < tInSize; ++j) {
                jdouble tSubInput = tInput[j];
                rGradBiases[j] = tSubInput;
                for (jint k = 0; k < tOutSize; ++k) {
                    rGradWeights[k] = tSubInput * tOutput[k];
                }
                rGradWeights += tOutSize;
            }
            rGradWeights -= tWeightSize;
        }
        for (jint j = 0; j < tOutSize; ++j) {
            tOutput[j] = dotAB_jse(tInput, tWeights, tInSize) * tGrad[j];
            tWeights += tInSize;
        }
        tInput = tOutput;
        tInSize = tOutSize;
    }
    // to input layer
    if (rGradPara != NULL) {
        rGradWeights -= aInputDim*tInSize;
        rGradBiases -= tInSize;
        for (jint j = 0; j < tInSize; ++j) {
            jdouble tSubInput = tInput[j];
            rGradBiases[j] = tSubInput;
            for (jint k = 0; k < aInputDim; ++k) {
                rGradWeights[k] = tSubInput * aX[k];
            }
            rGradWeights += aInputDim;
        }
    }
    for (jint j = 0; j < aInputDim; ++j) {
        rGradX[j] = dotAB_jse(tInput, tWeights, tInSize);
        tWeights += tInSize;
    }
    
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_FeedForward_forward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias, jdoubleArray rHiddenOutputs) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aHiddenBiases);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    
    jdouble tOut = forward(tX+aShiftX, aInputDim, tHiddenDims, aHiddenNumber,
                           tHiddenWeights, tHiddenBiases, tOutputWeight, aOutputBias,
                           tHiddenOutputs, NULL);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
    
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_FeedForward_backward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias, jdoubleArray rHiddenOutputs, jdoubleArray rHiddenGrads) {
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
    
    jdouble tOut = backward(tX+aShiftX, tGradX+aShiftGradX, NULL, aInputDim, tHiddenDims, aHiddenNumber,
                           tHiddenWeights, tHiddenWeightsBackward, tHiddenBiases, tOutputWeight, aOutputBias,
                           tHiddenOutputs, tHiddenGrads);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradX, tGradX, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, JNI_ABORT); // buffer only
    
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_FeedForward_backwardFull1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX, jdoubleArray rGradPara, jint aShiftGradPara, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias, jdoubleArray rHiddenOutputs, jdoubleArray rHiddenGrads) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradX = (jdouble *)getJArrayBuf(aEnv, rGradX);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aHiddenBiases);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads);
    
    jdouble tOut = backward(tX+aShiftX, tGradX+aShiftGradX, tGradPara+aShiftGradPara, aInputDim, tHiddenDims, aHiddenNumber,
                           tHiddenWeights, tHiddenWeightsBackward, tHiddenBiases, tOutputWeight, aOutputBias,
                           tHiddenOutputs, tHiddenGrads);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradX, tGradX, 0);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, JNI_ABORT); // buffer only
    
    return tOut;
}

#ifdef __cplusplus
}
#endif
