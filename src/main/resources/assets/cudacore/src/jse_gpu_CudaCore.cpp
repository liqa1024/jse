#include "jse_gpu_CudaCore.h"

#include <stdint.h>
#include <cuda_runtime.h>

static inline void throwExceptionCuda(JNIEnv *aEnv, const char *aErrStr) {
    const char *tClazzName = "jse/gpu/CudaException";
    const char *tInitSig = "(Ljava/lang/String;)V";
    // find class runtime due to asm
    jclass tClazz = aEnv->FindClass(tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = aEnv->GetMethodID(tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = aEnv->NewStringUTF(aErrStr);
    jthrowable tException = (jthrowable)aEnv->NewObject(tClazz, tInit, tJErrStr);
    aEnv->Throw(tException);
    aEnv->DeleteLocalRef(tException);
    aEnv->DeleteLocalRef(tJErrStr);
    aEnv->DeleteLocalRef(tClazz);
}

extern "C" {

JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaDeviceSynchronize(JNIEnv *aEnv, jclass aClazz) {
    const cudaError_t tErr = cudaDeviceSynchronize();
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
    }
}

JNIEXPORT jlong JNICALL Java_jse_gpu_CudaCore_cudaMalloc(JNIEnv *aEnv, jclass aClazz, jint aCount) {
    void *tPtr = NULL;
    const cudaError_t tErr = cudaMalloc(&tPtr, aCount);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
        return 0;
    }
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaFree(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    const cudaError_t tErr = cudaFree((void *)(intptr_t)aPtr);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
    }
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaMemcpyH2D(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jint aCount) {
    const cudaError_t tErr = cudaMemcpy((void *)(intptr_t)rDest, (void *)(intptr_t)aSrc, aCount, cudaMemcpyHostToDevice);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
    }
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaMemcpyD2H(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jint aCount) {
    const cudaError_t tErr = cudaMemcpy((void *)(intptr_t)rDest, (void *)(intptr_t)aSrc, aCount, cudaMemcpyDeviceToHost);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
    }
}
JNIEXPORT void JNICALL Java_jse_gpu_CudaCore_cudaMemcpyD2D(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jint aCount) {
    const cudaError_t tErr = cudaMemcpy((void *)(intptr_t)rDest, (void *)(intptr_t)aSrc, aCount, cudaMemcpyDeviceToDevice);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
    }
}

}
