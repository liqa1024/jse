#include "jse_gpu_Int64CudaPointer.h"
#include "cudacore_util.h"
#include "jniutil.h"

extern "C" {

JNIEXPORT void JNICALL Java_jse_gpu_Int64CudaPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jlongArray aJArray, jint aStart, jint aCount) {
    int64_t *tBuf = MALLOCN_TP(int64_t, aCount);
    parsejlong2int64_tV(aEnv, aJArray, aStart, tBuf, 0, aCount);
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, tBuf, (void *)(intptr_t)rPtr, aCount*sizeof(int64_t));
    FREE(tBuf);
}
JNIEXPORT void JNICALL Java_jse_gpu_Int64CudaPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlongArray rJArray, jint aStart, jint aCount) {
    int64_t *tBuf = MALLOCN_TP(int64_t, aCount);
    if (JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aPtr, tBuf, aCount*sizeof(int64_t))) {
        FREE(tBuf);
        return;
    }
    parseint64_t2jlongV(aEnv, rJArray, aStart, tBuf, 0, aCount);
    FREE(tBuf);
}

}
