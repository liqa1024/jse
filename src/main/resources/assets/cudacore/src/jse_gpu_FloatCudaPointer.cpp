#include "jse_gpu_FloatCudaPointer.h"
#include "cudacore_util.h"
#include "jniutil.h"

extern "C" {

JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_fill_1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    parsejfloat2floatV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(float));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_fillD_1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    parsejdouble2floatV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(float));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(float))) {
        FREE(tBuf);
        return;
    }
    parsefloat2jfloatV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_FloatCudaPointer_parse2destD_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    float *tBuf = MALLOCN_TP(float, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(float))) {
        FREE(tBuf);
        return;
    }
    parsefloat2jdoubleV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}

}
