#include "jse_gpu_DoubleCudaPointer.h"
#include "cudacore_util.h"
#include "jniutil.h"

extern "C" {

JNIEXPORT void JNICALL Java_jse_gpu_DoubleCudaPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    double *tBuf = MALLOCN_TP(double, aCount);
    parsejdouble2doubleV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(double));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_DoubleCudaPointer_fillF0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aCount) {
    double *tBuf = MALLOCN_TP(double, aCount);
    parsejfloat2doubleV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(double));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_DoubleCudaPointer_fillF1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jlong aData, jlong aCount) {
    double *tBuf = MALLOCN_TP(double, aCount);
    float *tData = (float *)(intptr_t)aData;
    for (jlong i = 0; i < aCount; ++i) {
        tBuf[i] = (double)tData[i];
    }
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(double));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_DoubleCudaPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    double *tBuf = MALLOCN_TP(double, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(double))) {
        FREE(tBuf);
        return;
    }
    parsedouble2jdoubleV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_DoubleCudaPointer_parse2destF0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aCount) {
    double *tBuf = MALLOCN_TP(double, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(double))) {
        FREE(tBuf);
        return;
    }
    parsedouble2jfloatV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_DoubleCudaPointer_parse2destF1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong rDest, jlong aCount) {
    double *tBuf = MALLOCN_TP(double, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(double))) {
        FREE(tBuf);
        return;
    }
    float *tDest = (float *)(intptr_t)rDest;
    for (jlong i = 0; i < aCount; ++i) {
        tDest[i] = (float)tBuf[i];
    }
    FREE(tBuf);
}

}
