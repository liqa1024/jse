#include "jsex_nnap_nn_FeedForward.h"
#include <math.h>

#ifdef __cplusplus
extern "C" {
#endif

static inline double silu(double aX) {
    return aX / (1.0 + exp(-aX));
}
static inline double siluGrad(double aX, double *aGrad) {
    double tSigmoid = 1.0 / (1.0 + exp(-aX));
    *aGrad = tSigmoid * (1 + aX * (1 - tSigmoid));
    return aX * tSigmoid;
}

static inline double forward(double *aX, jint aInputDim, jint *aHiddenDims, jint aHiddenNumber,
                             double *aHiddenWeights, double *aHiddenBiases, double *aOutputWeight, double aOutputBias,
                             double *rHiddenOutputs, double *rHiddenGrad) {
    double *tInput = aX;
    double *tOutput = rHiddenOutputs;
    double *tGrad = rHiddenGrad;
    double *tWeights = aHiddenWeights;
    double *tBiases = aHiddenBiases;
    jint tInSize = aInputDim;
    const jint tEnd = aHiddenNumber - 1;
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = aHiddenDims[i];
        for (jint j = 0; j < tOutSize; ++j) {
            double rDot = tBiases[j];
            for (jint k = 0; k < tInSize; ++k) {
                rDot += tInput[k] * tWeights[k];
            }
            if (tGrad == NULL) {
                tOutput[j] = silu(rDot);
            } else {
                double rGradDot;
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
    double rOut = aOutputBias;
    const jint tOutSize = aHiddenDims[tEnd];
    for (jint j = 0; j < tOutSize; ++j) {
        double rDot = tBiases[j];
        for (jint k = 0; k < tInSize; ++k) {
            rDot += tInput[k] * tWeights[k];
        }
        double tOutputWeight = aOutputWeight[j];
        if (tGrad == NULL) {
            rOut += silu(rDot) * tOutputWeight;
        } else {
            double rGradDot;
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
#ifdef __cplusplus
    double *tX = (double *)aEnv->GetPrimitiveArrayCritical(aX, NULL);
    jint *tHiddenDims = (jint *)aEnv->GetPrimitiveArrayCritical(aHiddenDims, NULL);
    double *tHiddenWeights = (double *)aEnv->GetPrimitiveArrayCritical(aHiddenWeights, NULL);
    double *tHiddenBiases = (double *)aEnv->GetPrimitiveArrayCritical(aHiddenBiases, NULL);
    double *tOutputWeight = (double *)aEnv->GetPrimitiveArrayCritical(aOutputWeight, NULL);
    double *tHiddenOutputs = (double *)aEnv->GetPrimitiveArrayCritical(rHiddenOutputs, NULL);
#else
    double *tX = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aX, NULL);
    jint *tHiddenDims = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aHiddenDims, NULL);
    double *tHiddenWeights = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aHiddenWeights, NULL);
    double *tHiddenBiases = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aHiddenBiases, NULL);
    double *tOutputWeight = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aOutputWeight, NULL);
    double *tHiddenOutputs = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rHiddenOutputs, NULL);
#endif

    double *tX_ = tX + aShiftX;
    double tOut = forward(tX_, aInputDim, tHiddenDims, aHiddenNumber,
                          tHiddenWeights, tHiddenBiases, tOutputWeight, aOutputBias,
                          tHiddenOutputs, NULL);
    
    // release java array
#ifdef __cplusplus
    aEnv->ReleasePrimitiveArrayCritical(aX, tX, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aHiddenDims, tHiddenDims, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aHiddenWeights, tHiddenWeights, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aHiddenBiases, tHiddenBiases, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aOutputWeight, tOutputWeight, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
#else
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aX, tX, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
#endif

    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_FeedForward_backward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX, jint aInputDim, jintArray aHiddenDims, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias, jdoubleArray rHiddenOutputs, jdoubleArray rHiddenGrads) {
    // java array init
#ifdef __cplusplus
    double *tX = (double *)aEnv->GetPrimitiveArrayCritical(aX, NULL);
    double *tGradX = (double *)aEnv->GetPrimitiveArrayCritical(rGradX, NULL);
    jint *tHiddenDims = (jint *)aEnv->GetPrimitiveArrayCritical(aHiddenDims, NULL);
    double *tHiddenWeights = (double *)aEnv->GetPrimitiveArrayCritical(aHiddenWeights, NULL);
    double *tHiddenBiases = (double *)aEnv->GetPrimitiveArrayCritical(aHiddenBiases, NULL);
    double *tOutputWeight = (double *)aEnv->GetPrimitiveArrayCritical(aOutputWeight, NULL);
    double *tHiddenOutputs = (double *)aEnv->GetPrimitiveArrayCritical(rHiddenOutputs, NULL);
    double *tHiddenGrads = (double *)aEnv->GetPrimitiveArrayCritical(rHiddenGrads, NULL);
#else
    double *tX = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aX, NULL);
    double *tGradX = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rGradX, NULL);
    jint *tHiddenDims = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aHiddenDims, NULL);
    double *tHiddenWeights = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aHiddenWeights, NULL);
    double *tHiddenBiases = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aHiddenBiases, NULL);
    double *tOutputWeight = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aOutputWeight, NULL);
    double *tHiddenOutputs = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rHiddenOutputs, NULL);
    double *tHiddenGrads = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rHiddenGrads, NULL);
#endif

    double *tX_ = tX + aShiftX;
    double *tGradX_ = tGradX + aShiftGradX;
    double tOut = forward(tX_, aInputDim, tHiddenDims, aHiddenNumber,
                          tHiddenWeights, tHiddenBiases, tOutputWeight, aOutputBias,
                          tHiddenOutputs, tHiddenGrads);
    // switch to last layer
    const jint tEnd = aHiddenNumber - 1;
    double *tGrad = tHiddenGrads;
    double *tOutput = tHiddenOutputs;
    double *tWeights = tHiddenWeights;
    jint tInSize = aInputDim;
    for (jint i = 0; i < tEnd; ++i) {
        const jint tOutSize = tHiddenDims[i];
        tGrad += tOutSize;
        tOutput += tOutSize;
        tWeights += (tInSize*tOutSize);
        tInSize = tOutSize;
    }
    tWeights += (tInSize*tHiddenDims[tEnd]);
    double *tInput = tGrad;
    // begin backward, last layer has been specially optimized
    tInSize = tHiddenDims[tEnd];
    for (jint i = tEnd-1; i >= 0; --i) {
        const jint tOutSize = tHiddenDims[i];
        tGrad -= tOutSize;
        tOutput -= tOutSize;
        tWeights -= (tInSize*tOutSize);
        for (jint j = 0; j < tOutSize; ++j) {
            double rDot = 0.0;
            for (jint k = 0; k < tInSize; ++k) {
                rDot += tInput[k] * tWeights[k*tOutSize + j];
            }
            tOutput[j] = rDot * tGrad[j];
        }
        tInput = tOutput;
        tInSize = tOutSize;
    }
    // to input layer
    tWeights = tHiddenWeights;
    for (jint j = 0; j < aInputDim; ++j) {
        double rDot = 0.0;
        for (jint k = 0; k < tInSize; ++k) {
            rDot += tInput[k] * tWeights[k*aInputDim + j];
        }
        tGradX_[j] = rDot;
    }
    
    // release java array
#ifdef __cplusplus
    aEnv->ReleasePrimitiveArrayCritical(aX, tX, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(rGradX, tGradX, 0);
    aEnv->ReleasePrimitiveArrayCritical(aHiddenDims, tHiddenDims, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aHiddenWeights, tHiddenWeights, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aHiddenBiases, tHiddenBiases, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aOutputWeight, tOutputWeight, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
    aEnv->ReleasePrimitiveArrayCritical(rHiddenGrads, tHiddenGrads, JNI_ABORT); // buffer only
#else
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aX, tX, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rGradX, tGradX, 0);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rHiddenOutputs, tHiddenOutputs, JNI_ABORT); // buffer only
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rHiddenGrads, tHiddenGrads, JNI_ABORT); // buffer only
#endif

    return tOut;
}

#ifdef __cplusplus
}
#endif
