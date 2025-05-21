#include "jsex_nnap_basis_BASIS.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_jsex_nnap_basis_BASIS_forceDot1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aXGrad, jdoubleArray aFpPx, jdoubleArray aFpPy, jdoubleArray aFpPz, jint aShift, jint aLength,
        jdoubleArray aFpPxCross, jdoubleArray aFpPyCross, jdoubleArray aFpPzCross, jdoubleArray rFx, jdoubleArray rFy, jdoubleArray rFz, jint aNN) {
    // java array init
#ifdef __cplusplus
    double *tXGrad = (double *)aEnv->GetPrimitiveArrayCritical(aXGrad, NULL);
    double *tFpPx = (double *)aEnv->GetPrimitiveArrayCritical(aFpPx, NULL);
    double *tFpPy = (double *)aEnv->GetPrimitiveArrayCritical(aFpPy, NULL);
    double *tFpPz = (double *)aEnv->GetPrimitiveArrayCritical(aFpPz, NULL);
    double *tFpPxCross = (double *)aEnv->GetPrimitiveArrayCritical(aFpPxCross, NULL);
    double *tFpPyCross = (double *)aEnv->GetPrimitiveArrayCritical(aFpPyCross, NULL);
    double *tFpPzCross = (double *)aEnv->GetPrimitiveArrayCritical(aFpPzCross, NULL);
    double *tFx = (double *)aEnv->GetPrimitiveArrayCritical(rFx, NULL);
    double *tFy = (double *)aEnv->GetPrimitiveArrayCritical(rFy, NULL);
    double *tFz = (double *)aEnv->GetPrimitiveArrayCritical(rFz, NULL);
#else
    double *tXGrad = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aXGrad, NULL);
    double *tFpPx = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPx, NULL);
    double *tFpPy = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPy, NULL);
    double *tFpPz = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPz, NULL);
    double *tFpPxCross = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPxCross, NULL);
    double *tFpPyCross = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPyCross, NULL);
    double *tFpPzCross = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPzCross, NULL);
    double *tFx = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rFx, NULL);
    double *tFy = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rFy, NULL);
    double *tFz = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rFz, NULL);
#endif
    
    double *tXGrad_ = tXGrad + aShift;
    double rDotX = 0.0, rDotY = 0.0, rDotZ = 0.0;
    for (jint i = 0; i < aLength; ++i) {
        double subXGrad = tXGrad_[i];
        rDotX += subXGrad * tFpPx[i];
        rDotY += subXGrad * tFpPy[i];
        rDotZ += subXGrad * tFpPz[i];
    }
    tFx[0] = rDotX; tFy[0] = rDotY; tFz[0] = rDotZ;
    
    double *tFpPx_ = tFpPxCross;
    double *tFpPy_ = tFpPyCross;
    double *tFpPz_ = tFpPzCross;
    for (jint j = 0; j < aNN; ++j) {
        rDotX = 0.0; rDotY = 0.0; rDotZ = 0.0;
        for (jint i = 0; i < aLength; ++i) {
            double subXGrad = tXGrad_[i];
            rDotX += subXGrad * tFpPx_[i];
            rDotY += subXGrad * tFpPy_[i];
            rDotZ += subXGrad * tFpPz_[i];
        }
        tFx[j+1] = rDotX; tFy[j+1] = rDotY; tFz[j+1] = rDotZ;
        tFpPx_ += aLength; tFpPy_ += aLength; tFpPz_ += aLength;
    }
    
    // release java array
#ifdef __cplusplus
    aEnv->ReleasePrimitiveArrayCritical(aXGrad, tXGrad, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPx, tFpPx, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPy, tFpPy, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPz, tFpPz, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPxCross, tFpPxCross, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPyCross, tFpPyCross, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPzCross, tFpPzCross, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(rFx, tFx, 0);
    aEnv->ReleasePrimitiveArrayCritical(rFy, tFy, 0);
    aEnv->ReleasePrimitiveArrayCritical(rFz, tFz, 0);
#else
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aXGrad, tXGrad, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPx, tFpPx, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPy, tFpPy, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPz, tFpPz, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPxCross, tFpPxCross, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPyCross, tFpPyCross, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPzCross, tFpPzCross, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rFx, tFx, 0);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rFy, tFy, 0);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rFz, tFz, 0);
#endif
}

#ifdef __cplusplus
}
#endif
