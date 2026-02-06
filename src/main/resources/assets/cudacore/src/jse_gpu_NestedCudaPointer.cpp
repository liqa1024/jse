#include "jse_gpu_NestedCudaPointer.h"
#include "cudacore_util.h"
#include "jniutil.h"

extern "C" {

JNIEXPORT jint JNICALL Java_jse_gpu_NestedCudaPointer_typeSize_1(JNIEnv *aEnv, jclass aClazz) {
    return (jint)sizeof(void *);
}

}
