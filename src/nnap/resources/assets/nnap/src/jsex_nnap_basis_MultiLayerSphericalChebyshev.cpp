#include "jsex_nnap_basis_MultiLayerSphericalChebyshev.h"
#include "basis_MultiLayerSphericalChebyshev.hpp"

extern "C" {

JNIEXPORT void JNICALL Java_jsex_nnap_basis_MultiLayerSphericalChebyshev_forwardEmb0(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aInputX, jdoubleArray rOutputEmb, jint aNMax, jdoubleArray aEmbWeights, jdoubleArray aEmbBiases, jintArray aEmbDims, jint aEmbNumber) {
    jdouble *tInputX = (jdouble *)getJArrayBuf(aEnv, aInputX);
    jdouble *tOutputEmb = (jdouble *)getJArrayBuf(aEnv, rOutputEmb);
    jdouble *tEmbWeights = (jdouble *)getJArrayBuf(aEnv, aEmbWeights);
    jdouble *tEmbBiases = (jdouble *)getJArrayBuf(aEnv, aEmbBiases);
    jint *tEmbDims = (jint *)getJArrayBuf(aEnv, aEmbDims);

    jint tEmbCacheSize = 0;
    for (jint l = 0; l < aEmbNumber-1; ++l) {
        tEmbCacheSize += tEmbDims[l];
    }
    jdouble *rEmbCache = MALLOCN_TP(jdouble, tEmbCacheSize);
    JSE_NNAP::calEmbRn(tInputX, aNMax, tEmbWeights, tEmbBiases, tEmbDims, aEmbNumber, rEmbCache, NULL, tOutputEmb);
    FREE(rEmbCache);

    releaseJArrayBuf(aEnv, aInputX, tInputX, JNI_ABORT);
    releaseJArrayBuf(aEnv, rOutputEmb, tOutputEmb, 0);
    releaseJArrayBuf(aEnv, aEmbWeights, tEmbWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEmbBiases, tEmbBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEmbDims, tEmbDims, JNI_ABORT);
}
JNIEXPORT void JNICALL Java_jsex_nnap_basis_MultiLayerSphericalChebyshev_forward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray rFp, jint aShiftFp, jdoubleArray rForwardCache, jint aForwardCacheShift, jboolean aFullCache,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
        jdoubleArray aEmbWeights, jdoubleArray aEmbBiases, jintArray aEmbDims, jint aEmbNumber,
        jdoubleArray aEquFuseWeight, jint aEquFuseSize, jdouble aEquFuseScale,
        jdoubleArray aRFuncScale, jdoubleArray aSystemScale) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tFp = (jdouble *)getJArrayBuf(aEnv, rFp);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, rForwardCache);
    jdouble *tEmbWeights = (jdouble *)getJArrayBuf(aEnv, aEmbWeights);
    jdouble *tEmbBiases = (jdouble *)getJArrayBuf(aEnv, aEmbBiases);
    jint *tEmbDims = (jint *)getJArrayBuf(aEnv, aEmbDims);
    jdouble *tEquFuseWeight = (jdouble *)getJArrayBuf(aEnv, aEquFuseWeight);
    jdouble *tRFuncScale = (jdouble *)getJArrayBuf(aEnv, aRFuncScale);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    JSE_NNAP::calFp(tNlDx, tNlDy, tNlDz, tNlType, aNN, tFp+aShiftFp,
                    tForwardCache+aForwardCacheShift, aFullCache,
                    aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max,
                    tEmbWeights, tEmbBiases, tEmbDims, aEmbNumber,
                    tEquFuseWeight, aEquFuseSize, aEquFuseScale,
                    tRFuncScale, tSystemScale);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, rFp, tFp, 0);
    releaseJArrayBuf(aEnv, rForwardCache, tForwardCache, 0);
    releaseJArrayBuf(aEnv, aEmbWeights, tEmbWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEmbBiases, tEmbBiases, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEmbDims, tEmbDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEquFuseWeight, tEquFuseWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aRFuncScale, tRFuncScale, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_MultiLayerSphericalChebyshev_backward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aGradFp, jint aShiftGradFp, jdoubleArray rGradPara, jint aShiftGradPara,
        jdoubleArray aForwardCache, jint aForwardCacheShift, jdoubleArray rBackwardCache, jint aBackwardCacheShift,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
        jdoubleArray aEmbWeights, jintArray aEmbDims, jint aEmbNumber,
        jdoubleArray aEquFuseWeight, jint aEquFuseSize, jdouble aEquFuseScale,
        jdoubleArray aSystemScale) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tGradFp = (jdouble *)getJArrayBuf(aEnv, aGradFp);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, aForwardCache);
    jdouble *tBackwardCache = (jdouble *)getJArrayBuf(aEnv, rBackwardCache);
    jdouble *tEmbWeights = (jdouble *)getJArrayBuf(aEnv, aEmbWeights);
    jint *tEmbDims = (jint *)getJArrayBuf(aEnv, aEmbDims);
    jdouble *tEquFuseWeight = (jdouble *)getJArrayBuf(aEnv, aEquFuseWeight);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    JSE_NNAP::calBackward(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                          tGradFp+aShiftGradFp, tGradPara+aShiftGradPara,
                          tForwardCache+aForwardCacheShift, tBackwardCache+aBackwardCacheShift,
                          aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max,
                          tEmbWeights, tEmbDims, aEmbNumber,
                          tEquFuseWeight, aEquFuseSize, aEquFuseScale,
                          tSystemScale);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradFp, tGradFp, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aForwardCache, tForwardCache, JNI_ABORT);
    releaseJArrayBuf(aEnv, rBackwardCache, tBackwardCache, 0);
    releaseJArrayBuf(aEnv, aEmbWeights, tEmbWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEmbDims, tEmbDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEquFuseWeight, tEquFuseWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_MultiLayerSphericalChebyshev_forwardForce1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNNGrad, jint aShiftFp, jdoubleArray rFx, jdoubleArray rFy, jdoubleArray rFz,
        jdoubleArray aForwardCache, jint aForwardCacheShift, jdoubleArray rForwardForceCache, jint aForwardForceCacheShift, jboolean aFullCache,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
        jdoubleArray aEmbWeights, jintArray aEmbDims, jint aEmbNumber,
        jdoubleArray aEquFuseWeight, jint aEquFuseSize, jdouble aEquFuseScale,
        jdoubleArray aRFuncScale, jdoubleArray aSystemScale) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tNNGrad = (jdouble *)getJArrayBuf(aEnv, aNNGrad);
    jdouble *tFx = (jdouble *)getJArrayBuf(aEnv, rFx);
    jdouble *tFy = (jdouble *)getJArrayBuf(aEnv, rFy);
    jdouble *tFz = (jdouble *)getJArrayBuf(aEnv, rFz);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, aForwardCache);
    jdouble *tForwardForceCache = (jdouble *)getJArrayBuf(aEnv, rForwardForceCache);
    jdouble *tEmbWeights = (jdouble *)getJArrayBuf(aEnv, aEmbWeights);
    jint *tEmbDims = (jint *)getJArrayBuf(aEnv, aEmbDims);
    jdouble *tEquFuseWeight = (jdouble *)getJArrayBuf(aEnv, aEquFuseWeight);
    jdouble *tRFuncScale = (jdouble *)getJArrayBuf(aEnv, aRFuncScale);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    // JSE_NNAP::calForce(tNlDx, tNlDy, tNlDz, tNlType, aNN,
    //                    tNNGrad+aShiftFp, tFx, tFy, tFz,
    //                    tForwardCache+aForwardCacheShift, tForwardForceCache+aForwardForceCacheShift, aFullCache,
    //                    aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max,
    //                    tRFuseWeight, aRFuseSize, aRFuseScale,
    //                    tEquFuseWeight, aEquFuseSize, aEquFuseScale,
    //                    tRFuncScale, tSystemScale);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNNGrad, tNNGrad, JNI_ABORT);
    releaseJArrayBuf(aEnv, rFx, tFx, 0);
    releaseJArrayBuf(aEnv, rFy, tFy, 0);
    releaseJArrayBuf(aEnv, rFz, tFz, 0);
    releaseJArrayBuf(aEnv, aForwardCache, tForwardCache, JNI_ABORT);
    releaseJArrayBuf(aEnv, rForwardForceCache, tForwardForceCache, 0);
    releaseJArrayBuf(aEnv, aEmbWeights, tEmbWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEmbDims, tEmbDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEquFuseWeight, tEquFuseWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aRFuncScale, tRFuncScale, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_MultiLayerSphericalChebyshev_backwardForce1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNNGrad, jint aShiftNNGrad, jdoubleArray aGradFx, jdoubleArray aGradFy, jdoubleArray aGradFz,
        jdoubleArray rGradNNGrad, jint aShiftGradNNGrad, jdoubleArray rGradPara, jint aShiftGradPara,
        jdoubleArray aForwardCache, jint aForwardCacheShift, jdoubleArray aForwardForceCache, jint aForwardForceCacheShift,
        jdoubleArray rBackwardCache, jint aBackwardCacheShift, jdoubleArray rBackwardForceCache, jint aBackwardForceCacheShift, jboolean aFixBasis,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aLMax, jint aL3Max, jint aL4Max,
        jdoubleArray aEmbWeights, jintArray aEmbDims, jint aEmbNumber,
        jdoubleArray aEquFuseWeight, jint aEquFuseSize, jdouble aEquFuseScale,
        jdoubleArray aSystemScale) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tNNGrad = (jdouble *)getJArrayBuf(aEnv, aNNGrad);
    jdouble *tGradFx = (jdouble *)getJArrayBuf(aEnv, aGradFx);
    jdouble *tGradFy = (jdouble *)getJArrayBuf(aEnv, aGradFy);
    jdouble *tGradFz = (jdouble *)getJArrayBuf(aEnv, aGradFz);
    jdouble *tGradNNGrad = (jdouble *)getJArrayBuf(aEnv, rGradNNGrad);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara); // nullable
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, aForwardCache);
    jdouble *tForwardForceCache = (jdouble *)getJArrayBuf(aEnv, aForwardForceCache);
    jdouble *tBackwardCache = (jdouble *)getJArrayBuf(aEnv, rBackwardCache);
    jdouble *tBackwardForceCache = (jdouble *)getJArrayBuf(aEnv, rBackwardForceCache);
    jdouble *tEmbWeights = (jdouble *)getJArrayBuf(aEnv, aEmbWeights);
    jint *tEmbDims = (jint *)getJArrayBuf(aEnv, aEmbDims);
    jdouble *tEquFuseWeight = (jdouble *)getJArrayBuf(aEnv, aEquFuseWeight);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    // JSE_NNAP::calBackwardForce(tNlDx, tNlDy, tNlDz, tNlType, aNN,
    //                            tNNGrad+aShiftNNGrad, tGradFx, tGradFy, tGradFz,
    //                            tGradNNGrad+aShiftGradNNGrad, tGradPara==NULL?NULL:(tGradPara+aShiftGradPara),
    //                            tForwardCache+aForwardCacheShift, tForwardForceCache+aForwardForceCacheShift,
    //                            tBackwardCache+aBackwardCacheShift, tBackwardForceCache+aBackwardForceCacheShift, aFixBasis,
    //                            aTypeNum, aRCut, aNMax, aLMax, aL3Max, aL4Max,
    //                            tRFuseWeight, aRFuseSize, aRFuseScale,
    //                            tEquFuseWeight, aEquFuseSize, aEquFuseScale,
    //                            tSystemScale);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNNGrad, tNNGrad, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradFx, tGradFx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradFy, tGradFy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradFz, tGradFz, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradNNGrad, tGradNNGrad, 0);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aForwardCache, tForwardCache, JNI_ABORT);
    releaseJArrayBuf(aEnv, aForwardForceCache, tForwardForceCache, JNI_ABORT);
    releaseJArrayBuf(aEnv, rBackwardCache, tBackwardCache, 0);
    releaseJArrayBuf(aEnv, rBackwardForceCache, tBackwardForceCache, 0);
    releaseJArrayBuf(aEnv, aEmbWeights, tEmbWeights, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEmbDims, tEmbDims, JNI_ABORT);
    releaseJArrayBuf(aEnv, aEquFuseWeight, tEquFuseWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

}
