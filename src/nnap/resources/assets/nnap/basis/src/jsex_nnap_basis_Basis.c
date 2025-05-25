#include "jsex_nnap_basis_BASIS.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_jsex_nnap_basis_Basis_forceDot1(JNIEnv *aEnv, jclass aClazz,
        jdoubleArray aXGrad, jint aShift, jint aLength, jdoubleArray aFpPx, jdoubleArray aFpPy, jdoubleArray aFpPz,
        jdoubleArray rFx, jdoubleArray rFy, jdoubleArray rFz, jint aNN) {
    // java array init
#ifdef __cplusplus
    double *tXGrad = (double *)aEnv->GetPrimitiveArrayCritical(aXGrad, NULL);
    double *tFpPx = (double *)aEnv->GetPrimitiveArrayCritical(aFpPx, NULL);
    double *tFpPy = (double *)aEnv->GetPrimitiveArrayCritical(aFpPy, NULL);
    double *tFpPz = (double *)aEnv->GetPrimitiveArrayCritical(aFpPz, NULL);
    double *tFx = (double *)aEnv->GetPrimitiveArrayCritical(rFx, NULL);
    double *tFy = (double *)aEnv->GetPrimitiveArrayCritical(rFy, NULL);
    double *tFz = (double *)aEnv->GetPrimitiveArrayCritical(rFz, NULL);
#else
    double *tXGrad = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aXGrad, NULL);
    double *tFpPx = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPx, NULL);
    double *tFpPy = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPy, NULL);
    double *tFpPz = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aFpPz, NULL);
    double *tFx = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rFx, NULL);
    double *tFy = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rFy, NULL);
    double *tFz = (double *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rFz, NULL);
#endif
    
    double *tXGrad_ = tXGrad + aShift;
    double *tFpPx_ = tFpPx;
    double *tFpPy_ = tFpPy;
    double *tFpPz_ = tFpPz;
    for (jint j = 0; j < aNN; ++j) {
        double rDotX = 0.0, rDotY = 0.0, rDotZ = 0.0;
        for (jint i = 0; i < aLength; ++i) {
            double subXGrad = tXGrad_[i];
            rDotX += subXGrad * tFpPx_[i];
            rDotY += subXGrad * tFpPy_[i];
            rDotZ += subXGrad * tFpPz_[i];
        }
        tFx[j] = rDotX; tFy[j] = rDotY; tFz[j] = rDotZ;
        tFpPx_ += aLength; tFpPy_ += aLength; tFpPz_ += aLength;
    }
    
    // release java array
#ifdef __cplusplus
    aEnv->ReleasePrimitiveArrayCritical(aXGrad, tXGrad, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPx, tFpPx, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPy, tFpPy, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(aFpPz, tFpPz, JNI_ABORT);
    aEnv->ReleasePrimitiveArrayCritical(rFx, tFx, 0);
    aEnv->ReleasePrimitiveArrayCritical(rFy, tFy, 0);
    aEnv->ReleasePrimitiveArrayCritical(rFz, tFz, 0);
#else
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aXGrad, tXGrad, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPx, tFpPx, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPy, tFpPy, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aFpPz, tFpPz, JNI_ABORT);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rFx, tFx, 0);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rFy, tFy, 0);
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rFz, tFz, 0);
#endif
}

#ifdef __cplusplus
}
#endif
