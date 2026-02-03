#include "jse_gpu_CudaCore.h"
#include "cudacore_util.h"

extern "C" {

JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaDeviceSynchronize(JNIEnv *aEnv, jclass aClazz) {
    const cudaError_t tErr = cudaDeviceSynchronize();
    if (tErr != cudaSuccess) {
        JSE_CUDACORE::throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
    }
}

JNIEXPORT jlong JNICALL Java_jse_gpu_CudaCore_cudaMalloc(JNIEnv *aEnv, jclass aClazz, jint aCount) {
    void *tPtr = NULL;
    const cudaError_t tErr = cudaMalloc(&tPtr, aCount);
    if (tErr != cudaSuccess) {
        JSE_CUDACORE::throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
        return 0;
    }
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaFree(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    const cudaError_t tErr = cudaFree((void *)(intptr_t)aPtr);
    if (tErr != cudaSuccess) {
        JSE_CUDACORE::throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
    }
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaMemcpyH2D(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jint aCount) {
    JSE_CUDACORE::cudaMemcpyH2D(aEnv, (void *)(intptr_t)aSrc, (void *)(intptr_t)rDest, aCount);
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaMemcpyD2H(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jint aCount) {
    JSE_CUDACORE::cudaMemcpyD2H(aEnv, (void *)(intptr_t)aSrc, (void *)(intptr_t)rDest, aCount);
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaMemcpyD2D(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jint aCount) {
    JSE_CUDACORE::cudaMemcpyD2D(aEnv, (void *)(intptr_t)aSrc, (void *)(intptr_t)rDest, aCount);
}

}
