#include "jse_gpu_IntCudaPointer.h"
#include "cudacore_util.h"
#include "jniutil.h"

extern "C" {

JNIEXPORT jint JNICALL Java_jse_gpu_IntCudaPointer_typeSize_1(JNIEnv *aEnv, jclass aClazz) {
    return (jint)sizeof(int);
}
JNIEXPORT void JNICALL Java_jse_gpu_IntCudaPointer_fill_1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jintArray aJArray, jint aStart, jint aCount) {
    int *tBuf = MALLOCN_TP(int, aCount);
    parsejint2intV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(int));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_IntCudaPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jintArray rJArray, jint aStart, jint aCount) {
    int *tBuf = MALLOCN_TP(int, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(int))) {
        FREE(tBuf);
        return;
    }
    parseint2jintV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}

}
