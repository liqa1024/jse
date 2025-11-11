#include "jsex_nnap_basis_Chebyshev.h"
#include "basis_Chebyshev.hpp"

extern "C" {

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_calSystemScale1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray rSystemScale, jint aShiftSystemScale, jdoubleArray rForwardCache, jint aForwardCacheShift,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseSize,
        jdoubleArray aRFuncScale, jdoubleArray aRFuncShift) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, rSystemScale);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, rForwardCache);
    jdouble *tRFuncScale = (jdouble *)getJArrayBuf(aEnv, aRFuncScale);
    jdouble *tRFuncShift = (jdouble *)getJArrayBuf(aEnv, aRFuncShift);
    
    // do cal
    JSE_NNAP::calSystemScale(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                             tSystemScale+aShiftSystemScale, tForwardCache+aForwardCacheShift,
                             aTypeNum, aRCut, aNMax, aWType, aFuseSize, tRFuncScale, tRFuncShift);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, rSystemScale, tSystemScale, 0);
    releaseJArrayBuf(aEnv, rForwardCache, tForwardCache, JNI_ABORT); // cache only
    releaseJArrayBuf(aEnv, aRFuncScale, tRFuncScale, JNI_ABORT);
    releaseJArrayBuf(aEnv, aRFuncShift, tRFuncShift, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_forward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray rFp, jint aShiftFp, jdoubleArray rForwardCache, jint aForwardCacheShift, jboolean aFullCache,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdoubleArray aFuseWeight, jint aFuseSize,
        jdoubleArray aRFuncScale, jdoubleArray aRFuncShift, jdoubleArray aSystemScale) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tFp = (jdouble *)getJArrayBuf(aEnv, rFp);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, rForwardCache);
    jdouble *tFuseWeight = (jdouble *)getJArrayBuf(aEnv, aFuseWeight); // nullable
    jdouble *tRFuncScale = (jdouble *)getJArrayBuf(aEnv, aRFuncScale);
    jdouble *tRFuncShift = (jdouble *)getJArrayBuf(aEnv, aRFuncShift);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    // do cal
    JSE_NNAP::calFp(tNlDx, tNlDy, tNlDz, tNlType, aNN, tFp+aShiftFp,
                    tForwardCache+aForwardCacheShift, aFullCache,
                    aTypeNum, aRCut, aNMax, aWType, aFuseStyle, tFuseWeight, aFuseSize,
                    tRFuncScale, tRFuncShift, tSystemScale);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, rFp, tFp, 0);
    releaseJArrayBuf(aEnv, rForwardCache, tForwardCache, 0);
    releaseJArrayBuf(aEnv, aFuseWeight, tFuseWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aRFuncScale, tRFuncScale, JNI_ABORT);
    releaseJArrayBuf(aEnv, aRFuncShift, tRFuncShift, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_backward1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aGradFp, jint aShiftGradFp, jdoubleArray rGradPara, jint aShiftGradPara,
        jdoubleArray aForwardCache, jint aForwardCacheShift, jdoubleArray rBackwardCache, jint aBackwardCacheShift,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jint aFuseSize, jdoubleArray aSystemScale) {
    // java array init
    jdouble *tNlDx = (jdouble *)getJArrayBuf(aEnv, aNlDx);
    jdouble *tNlDy = (jdouble *)getJArrayBuf(aEnv, aNlDy);
    jdouble *tNlDz = (jdouble *)getJArrayBuf(aEnv, aNlDz);
    jint *tNlType = (jint *)getJArrayBuf(aEnv, aNlType);
    jdouble *tGradFp = (jdouble *)getJArrayBuf(aEnv, aGradFp);
    jdouble *tGradPara = (jdouble *)getJArrayBuf(aEnv, rGradPara);
    jdouble *tForwardCache = (jdouble *)getJArrayBuf(aEnv, aForwardCache);
    jdouble *tBackwardCache = (jdouble *)getJArrayBuf(aEnv, rBackwardCache);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    // do cal
    JSE_NNAP::calBackward(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                          tGradFp+aShiftGradFp, tGradPara+aShiftGradPara,
                          tForwardCache+aForwardCacheShift, tBackwardCache+aBackwardCacheShift,
                          aTypeNum, aRCut, aNMax, aWType, aFuseStyle, aFuseSize, tSystemScale);
    
    // release java array
    releaseJArrayBuf(aEnv, aNlDx, tNlDx, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDy, tNlDy, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlDz, tNlDz, JNI_ABORT);
    releaseJArrayBuf(aEnv, aNlType, tNlType, JNI_ABORT);
    releaseJArrayBuf(aEnv, aGradFp, tGradFp, JNI_ABORT);
    releaseJArrayBuf(aEnv, rGradPara, tGradPara, 0);
    releaseJArrayBuf(aEnv, aForwardCache, tForwardCache, JNI_ABORT);
    releaseJArrayBuf(aEnv, rBackwardCache, tBackwardCache, 0);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_forwardForce1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNNGrad, jint aShiftNNGrad, jdoubleArray rFx, jdoubleArray rFy, jdoubleArray rFz,
        jdoubleArray aForwardCache, jint aForwardCacheShift, jdoubleArray rForwardForceCache, jint aForwardForceCacheShift, jboolean aFullCache,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdoubleArray aFuseWeight, jint aFuseSize,
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
    jdouble *tFuseWeight = (jdouble *)getJArrayBuf(aEnv, aFuseWeight); // nullable
    jdouble *tRFuncScale = (jdouble *)getJArrayBuf(aEnv, aRFuncScale);
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    JSE_NNAP::calForce(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                       tNNGrad+aShiftNNGrad, tFx, tFy, tFz,
                       tForwardCache+aForwardCacheShift, tForwardForceCache+aForwardForceCacheShift, aFullCache,
                       aTypeNum, aRCut, aNMax, aWType, aFuseStyle, tFuseWeight, aFuseSize,
                       tRFuncScale, tSystemScale);
    
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
    releaseJArrayBuf(aEnv, aFuseWeight, tFuseWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aRFuncScale, tRFuncScale, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Chebyshev_backwardForce1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aNlDx, jdoubleArray aNlDy, jdoubleArray aNlDz, jintArray aNlType, jint aNN,
        jdoubleArray aNNGrad, jint aShiftNNGrad, jdoubleArray aGradFx, jdoubleArray aGradFy, jdoubleArray aGradFz,
        jdoubleArray rGradNNGrad, jint aShiftGradNNGrad, jdoubleArray rGradPara, jint aShiftGradPara,
        jdoubleArray aForwardCache, jint aForwardCacheShift, jdoubleArray aForwardForceCache, jint aForwardForceCacheShift,
        jdoubleArray rBackwardCache, jint aBackwardCacheShift, jdoubleArray rBackwardForceCache, jint aBackwardForceCacheShift, jboolean aFixBasis,
        jint aTypeNum, jdouble aRCut, jint aNMax, jint aWType, jint aFuseStyle, jdoubleArray aFuseWeight, jint aFuseSize, jdoubleArray aSystemScale) {
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
    jdouble *tFuseWeight = (jdouble *)getJArrayBuf(aEnv, aFuseWeight); // nullable
    jdouble *tSystemScale = (jdouble *)getJArrayBuf(aEnv, aSystemScale);
    
    JSE_NNAP::calBackwardForce(tNlDx, tNlDy, tNlDz, tNlType, aNN,
                               tNNGrad+aShiftNNGrad, tGradFx, tGradFy, tGradFz,
                               tGradNNGrad+aShiftGradNNGrad, tGradPara==NULL?NULL:(tGradPara+aShiftGradPara),
                               tForwardCache+aForwardCacheShift, tForwardForceCache+aForwardForceCacheShift,
                               tBackwardCache+aBackwardCacheShift, tBackwardForceCache+aBackwardForceCacheShift, aFixBasis,
                               aTypeNum, aRCut, aNMax, aWType, aFuseStyle, tFuseWeight, aFuseSize, tSystemScale);
    
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
    releaseJArrayBuf(aEnv, aFuseWeight, tFuseWeight, JNI_ABORT);
    releaseJArrayBuf(aEnv, aSystemScale, tSystemScale, JNI_ABORT);
}

}
