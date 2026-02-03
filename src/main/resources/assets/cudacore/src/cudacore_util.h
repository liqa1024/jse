#ifndef CUDACORE_UTIL_H
#define CUDACORE_UTIL_H

#include <cuda_runtime.h>
#include <stdint.h>

namespace JSE_CUDACORE {

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

static inline jboolean cudaMemcpyH2D(JNIEnv *aEnv, void *aSrc, void *rDest, jint aCount) {
    const cudaError_t tErr = cudaMemcpy(rDest, aSrc, aCount, cudaMemcpyHostToDevice);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
        return JNI_TRUE;
    }
    return JNI_FALSE;
}
static inline jboolean cudaMemcpyD2H(JNIEnv *aEnv, void *aSrc, void *rDest, jint aCount) {
    const cudaError_t tErr = cudaMemcpy(rDest, aSrc, aCount, cudaMemcpyDeviceToHost);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
        return JNI_TRUE;
    }
    return JNI_FALSE;
}
static inline jboolean cudaMemcpyD2D(JNIEnv *aEnv, void *aSrc, void *rDest, jint aCount) {
    const cudaError_t tErr = cudaMemcpy(rDest, aSrc, aCount, cudaMemcpyDeviceToDevice);
    if (tErr != cudaSuccess) {
        throwExceptionCuda(aEnv, cudaGetErrorString(tErr));
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

}

#endif //CUDACORE_UTIL_H