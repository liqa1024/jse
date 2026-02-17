#include "jse_gpu_FloatCudaPointer.h"
#include "cudacore_util.h"
#include "jniutil.h"

extern "C" {

JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    parsejfloat2floatV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(float));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_fillD0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    parsejdouble2floatV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(float));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_fillD1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jlong aData, jlong aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    double *tData = (double *)(intptr_t)aData;
    for (jlong i = 0; i < aCount; ++i) {
        tBuf[i] = (float)tData[i];
    }
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(float));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(float))) {
        FREE(tBuf);
        return;
    }
    parsefloat2jfloatV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_parse2destD0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(float))) {
        FREE(tBuf);
        return;
    }
    parsefloat2jdoubleV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_parse2destD1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong rDest, jlong aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(float))) {
        FREE(tBuf);
        return;
    }
    double *tDest = (double *)(intptr_t)rDest;
    for (jlong i = 0; i < aCount; ++i) {
        tDest[i] = (double)tBuf[i];
    }
    FREE(tBuf);
}

}
