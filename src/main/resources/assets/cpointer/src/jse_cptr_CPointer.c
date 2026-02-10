#include "jse_cptr_CPointer.h"
#include "jniutil.h"

#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_cptr_CPointer_malloc_1(JNIEnv *aEnv, jclass aClazz, jlong aCount, jlong aSize) {
    return (jlong)(intptr_t)MALLOCN((size_t)aCount, (size_t)aSize);
}
JNIEXPORT jlong JNICALL Java_jse_cptr_CPointer_calloc_1(JNIEnv *aEnv, jclass aClazz, jlong aCount, jlong aSize) {
    return (jlong)(intptr_t)CALLOC((size_t)aCount, (size_t)aSize);
}
JNIEXPORT void JNICALL Java_jse_cptr_CPointer_free_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    FREE((void *)(intptr_t)aPtr);
}
JNIEXPORT void JNICALL Java_jse_cptr_CPointer_memcpy_1(JNIEnv *aEnv, jclass aClazz, jlong aSrc, jlong rDest, jlong aCount) {
    memcpy((void *)(intptr_t)rDest, (void *)(intptr_t)aSrc, (size_t)aCount);
}

#ifdef __cplusplus
}
#endif
