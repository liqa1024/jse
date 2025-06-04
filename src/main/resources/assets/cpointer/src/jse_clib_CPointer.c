#include "jniutil.h"
#include "jse_clib_CPointer.h"

#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_clib_CPointer_malloc_1(JNIEnv *aEnv, jclass aClazz, jint aCount, jint aSize) {
    return (jlong)(intptr_t)MALLOCN(aCount, aSize);
}
JNIEXPORT jlong JNICALL Java_jse_clib_CPointer_calloc_1(JNIEnv *aEnv, jclass aClazz, jint aCount, jint aSize) {
    return (jlong)(intptr_t)CALLOC(aCount, aSize);
}
JNIEXPORT void JNICALL Java_jse_clib_CPointer_free_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    FREE((void *)(intptr_t)aPtr);
}
JNIEXPORT void JNICALL Java_jse_clib_CPointer_memcpy_1(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jint aCount) {
    memcpy((void *)(intptr_t)rDest, (void *)(intptr_t)aSrc, aCount);
}

#ifdef __cplusplus
}
#endif
