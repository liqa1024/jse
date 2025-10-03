#include "jsex_nnap_nn_SharedFeedForward.h"
#include "nn_FeedForward.hpp"

extern "C" {

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_SharedFeedForward_forward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jint aInputDim, jint aSharedInputDim, jintArray aHiddenDims, jbooleanArray aSharedFlags, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aSharedHiddenWeights, jdoubleArray aHiddenBiases, jdoubleArray aSharedHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias,
        jdoubleArray rHiddenOutputs, jint aShiftOutputs, jdoubleArray rHiddenGrads, jint aShiftGrads) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jboolean *tSharedFlags = (jboolean *)getJArrayBuf(aEnv, aSharedFlags);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tSharedHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenWeights);
    jdouble *tHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aHiddenBiases);
    jdouble *tSharedHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenBiases);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    jdouble *tHiddenGrads = rHiddenGrads==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rHiddenGrads);
    
    jdouble tOut = JSE_NNAP::forward(tX+aShiftX, aInputDim, aSharedInputDim, tHiddenDims, tSharedFlags, aHiddenNumber,
                                     tHiddenWeights, tSharedHiddenWeights, tHiddenBiases, tSharedHiddenBiases, tOutputWeight, aOutputBias,
                                     tHiddenOutputs+aShiftOutputs, tHiddenGrads==NULL?NULL:(tHiddenGrads+aShiftGrads), NULL);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedFlags, tSharedFlags, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenWeights, tSharedHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenBiases, tSharedHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, rHiddenGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    if (rHiddenGrads!=NULL) releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, 0);
    
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jsex_nnap_nn_SharedFeedForward_forwardGrad1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX,
        jint aInputDim, jint aSharedInputDim, jintArray aHiddenDims, jbooleanArray aSharedFlags, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aSharedHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aSharedHiddenWeightsBackward,
        jdoubleArray aHiddenBiases, jdoubleArray aSharedHiddenBiases, jdoubleArray aOutputWeight, jdouble aOutputBias,
        jdoubleArray rHiddenOutputs, jint aShiftOutputs, jdoubleArray rHiddenGrads, jint aShiftGrads,
        jdoubleArray rHiddenGrads2, jint aShiftGrads2, jdoubleArray rHiddenGrads3, jint aShiftGrads3, jdoubleArray rHiddenGradGrads, jint aShiftGradGrads) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradX = (jdouble *)getJArrayBuf(aEnv, rGradX);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jboolean *tSharedFlags = (jboolean *)getJArrayBuf(aEnv, aSharedFlags);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tSharedHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenWeights);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tSharedHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenWeightsBackward);
    jdouble *tHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aHiddenBiases);
    jdouble *tSharedHiddenBiases = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenBiases);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads);
    jdouble *tHiddenGrads2 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads2);
    jdouble *tHiddenGrads3 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads3);
    jdouble *tHiddenGradGrads = rHiddenGradGrads==NULL?NULL:(jdouble *)getJArrayBuf(aEnv, rHiddenGradGrads);
    
    jdouble tOut = JSE_NNAP::forward(tX+aShiftX, aInputDim, aSharedInputDim, tHiddenDims, tSharedFlags, aHiddenNumber,
                                     tHiddenWeights, tSharedHiddenWeights, tHiddenBiases, tSharedHiddenBiases, tOutputWeight, aOutputBias,
                                     tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads, tHiddenGradGrads==NULL?NULL:(tHiddenGradGrads+aShiftGradGrads));
    
    jdouble *tGradX_ = tGradX + aShiftGradX;
    for (jint i = 0; i < aInputDim; ++i) {
        tGradX_[i] = 0.0;
    }
    JSE_NNAP::backward(1.0, tX+aShiftX, tGradX_, NULL, NULL,
                       aInputDim, aSharedInputDim, tHiddenDims, tSharedFlags, aHiddenNumber,
                       tHiddenWeightsBackward, tSharedHiddenWeightsBackward, tOutputWeight,
                       tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads,
                       tHiddenGrads2+aShiftGrads2, tHiddenGrads3+aShiftGrads3);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradX, tGradX, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedFlags, tSharedFlags, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenWeights, tSharedHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenWeightsBackward, tSharedHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenBiases, tHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenBiases, tSharedHiddenBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenOutputs, tHiddenOutputs, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    releaseJArrayBuf(aEnv, rHiddenGrads, tHiddenGrads, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    releaseJArrayBuf(aEnv, rHiddenGrads2, tHiddenGrads2, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    releaseJArrayBuf(aEnv, rHiddenGrads3, tHiddenGrads3, rHiddenGradGrads==NULL?JNI_ABORT:0); // buffer only for no grad
    if (rHiddenGradGrads!=NULL) releaseJArrayBuf(aEnv, rHiddenGradGrads, tHiddenGradGrads, 0);
    
    return tOut;
}

JNIEXPORT void JNICALL Java_jsex_nnap_nn_SharedFeedForward_backward1(JNIEnv *aEnv, jclass aClazz,
        jdouble aYGrad, jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX,
        jdoubleArray rGradPara, jint aShiftGradPara, jdoubleArray rGradSharedPara, jint aShiftGradSharedPara,
        jint aInputDim, jint aSharedInputDim, jintArray aHiddenDims, jbooleanArray aSharedFlags, jint aHiddenNumber,
        jdoubleArray aHiddenWeightsBackward, jdoubleArray aSharedHiddenWeightsBackward, jdoubleArray aOutputWeight,
        jdoubleArray aHiddenOutputs, jint aShiftOutputs, jdoubleArray aHiddenGrads, jint aShiftGrads,
        jdoubleArray rHiddenGrads2, jdoubleArray rHiddenGrads3) {
    // java array init
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradX = rGradX==NULL ? NULL : (jdouble *)getJArrayBuf(aEnv, rGradX);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jdouble *tGradSharedPara = (jdouble *)getJArrayBuf(aEnv, rGradSharedPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jboolean *tSharedFlags = (jboolean *)getJArrayBuf(aEnv, aSharedFlags);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tSharedHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenWeightsBackward);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, aHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads);
    jdouble *tHiddenGrads2 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads2);
    jdouble *tHiddenGrads3 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads3);
    
    JSE_NNAP::backward(aYGrad, tX+aShiftX, tGradX==NULL?NULL:tGradX+aShiftGradX, tGradPara+aShiftGradPara, tGradSharedPara+aShiftGradSharedPara,
                       aInputDim, aSharedInputDim, tHiddenDims, tSharedFlags, aHiddenNumber,
                       tHiddenWeightsBackward, tSharedHiddenWeightsBackward, tOutputWeight,
                       tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads, tHiddenGrads2, tHiddenGrads3);
    
    // release java array
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    if (rGradX!=NULL) releaseJArrayBuf(aEnv, rGradX, tGradX, 0);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, rGradSharedPara, tGradSharedPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedFlags, tSharedFlags, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenWeightsBackward, tSharedHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aOutputWeight, tOutputWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenOutputs, tHiddenOutputs, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenGrads, tHiddenGrads, JNI_ABORT);
    releaseJArrayBuf(aEnv, rHiddenGrads2, tHiddenGrads2, JNI_ABORT); // buffer only
    releaseJArrayBuf(aEnv, rHiddenGrads3, tHiddenGrads3, JNI_ABORT); // buffer only
}

JNIEXPORT void JNICALL Java_jsex_nnap_nn_SharedFeedForward_gradBackward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aGradXGrad, jint aShiftGradXGrad, jdoubleArray aX, jint aShiftX, jdoubleArray rGradX, jint aShiftGradX,
        jdoubleArray rGradPara, jint aShiftGradPara, jdoubleArray rGradSharedPara, jint aShiftGradSharedPara,
        jint aInputDim, jint aSharedInputDim, jintArray aHiddenDims, jbooleanArray aSharedFlags, jint aHiddenNumber,
        jdoubleArray aHiddenWeights, jdoubleArray aSharedHiddenWeights, jdoubleArray aHiddenWeightsBackward, jdoubleArray aSharedHiddenWeightsBackward, jdoubleArray aOutputWeight,
        jdoubleArray aHiddenOutputs, jint aShiftOutputs, jdoubleArray aHiddenGrads, jint aShiftGrads,
        jdoubleArray aHiddenGrads2, jint aShiftGrads2, jdoubleArray aHiddenGrads3, jint aShiftGrads3, jdoubleArray aHiddenGradGrads, jint aShiftGradGrads,
        jdoubleArray rHiddenOutputs2, jdoubleArray rHiddenGrads4, jdoubleArray rHiddenGrads5) {
    // java array init
    jdouble *tGradXGrad = (jdouble *)getJArrayBuf(aEnv, aGradXGrad);
    jdouble *tX = (jdouble *)getJArrayBuf(aEnv, aX);
    jdouble *tGradX = rGradX==NULL ? NULL : (jdouble *)getJArrayBuf(aEnv, rGradX);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jdouble *tGradSharedPara = (jdouble *)getJArrayBuf(aEnv, rGradSharedPara);
    jint *tHiddenDims = (jint *)getJArrayBuf(aEnv, aHiddenDims);
    jboolean *tSharedFlags = (jboolean *)getJArrayBuf(aEnv, aSharedFlags);
    jdouble *tHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aHiddenWeights);
    jdouble *tSharedHiddenWeights = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenWeights);
    jdouble *tHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aHiddenWeightsBackward);
    jdouble *tSharedHiddenWeightsBackward = (jdouble *)getJArrayBuf(aEnv, aSharedHiddenWeightsBackward);
    jdouble *tOutputWeight = (jdouble *)getJArrayBuf(aEnv, aOutputWeight);
    jdouble *tHiddenOutputs = (jdouble *)getJArrayBuf(aEnv, aHiddenOutputs);
    jdouble *tHiddenGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads);
    jdouble *tHiddenGrads2 = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads2);
    jdouble *tHiddenGrads3 = (jdouble *)getJArrayBuf(aEnv, aHiddenGrads3);
    jdouble *tHiddenGradGrads = (jdouble *)getJArrayBuf(aEnv, aHiddenGradGrads);
    jdouble *tHiddenOutputs2 = (jdouble *)getJArrayBuf(aEnv, rHiddenOutputs2);
    jdouble *tHiddenGrads4 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads4);
    jdouble *tHiddenGrads5 = (jdouble *)getJArrayBuf(aEnv, rHiddenGrads5);
    
    JSE_NNAP::gradBackward(tGradXGrad+aShiftGradXGrad, tX+aShiftX, tGradX==NULL?NULL:tGradX+aShiftGradX, tGradPara+aShiftGradPara, tGradSharedPara+aShiftGradSharedPara,
                           aInputDim, aSharedInputDim, tHiddenDims, tSharedFlags, aHiddenNumber,
                           tHiddenWeights, tSharedHiddenWeights, tHiddenWeightsBackward, tSharedHiddenWeightsBackward, tOutputWeight,
                           tHiddenOutputs+aShiftOutputs, tHiddenGrads+aShiftGrads,
                           tHiddenGrads2+aShiftGrads2, tHiddenGrads3+aShiftGrads3, tHiddenGradGrads+aShiftGradGrads,
                           tHiddenOutputs2, tHiddenGrads4, tHiddenGrads5);
    
    // release java array
    releaseJArrayBuf(aEnv, aGradXGrad, tGradXGrad, JNI_ABORT);
    releaseJArrayBuf(aEnv, aX, tX, JNI_ABORT);
    if (rGradX!=NULL) releaseJArrayBuf(aEnv, rGradX, tGradX, 0);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, rGradSharedPara, tGradSharedPara, 0);
    releaseJArrayBuf(aEnv, aHiddenDims, tHiddenDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedFlags, tSharedFlags, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeights, tHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenWeights, tSharedHiddenWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aHiddenWeightsBackward, tHiddenWeightsBackward, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSharedHiddenWeightsBackward, tSharedHiddenWeightsBackward, JNI_ABORT);
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

}
