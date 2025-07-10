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
            rOut += silu(rDot) * tOutputWeight;
        } else {
            jdouble rGradDot;
            rOut += siluGrad(rDot, &rGradDot) * tOutputWeight;
            tGrad[j] = rGradDot * tOutputWeight;
        }
        tWeights += tInSize;
    }
    return rOut;
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
    
    jdouble *tX_ = tX + aShiftX;
    jdouble tOut = forward(tX_, aInputDim, tHiddenDims, aHiddenNumber,
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
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGrad, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
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
    
    jdouble *tX_ = tX + aShiftX;
    jdouble *tGradX_ = tGradX + aShiftGrad;
    jdouble tOut = forward(tX_, aInputDim, tHiddenDims, aHiddenNumber,
                          tHiddenWeights, tHiddenBiases, tOutputWeight, aOutputBias,
                          tHiddenOutputs, tHiddenGrads);
    // switch to last layer
    const jint tEnd = aHiddenNumber - 1;
    jdouble *tGrad = tHiddenGrads;
    jdouble *tOutput = tHiddenOutputs;
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = tHiddenDims[i];
        tGrad += tOutSize;
        tOutput += tOutSize;
    }
    // begin backward, last layer has been specially optimized
    jdouble *tInput = tGrad;
    jdouble *tWeights = tHiddenWeightsBackward;
    jint tInSize = tHiddenDims[tEnd];
    for (jint i = tEnd-1; i >= 0; --i) {
        const jint tOutSize = tHiddenDims[i];
        tGrad -= tOutSize;
        tOutput -= tOutSize;
        for (jint j = 0; j < tOutSize; ++j) {
            tOutput[j] = dotAB_jse(tInput, tWeights, tInSize) * tGrad[j];
            tWeights += tInSize;
        }
        tInput = tOutput;
        tInSize = tOutSize;
    }
    // to input layer
    for (jint j = 0; j < aInputDim; ++j) {
        tGradX_[j] = dotAB_jse(tInput, tWeights, tInSize);
        tWeights += tInSize;
    }
    
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

#ifdef __cplusplus
}
#endif
