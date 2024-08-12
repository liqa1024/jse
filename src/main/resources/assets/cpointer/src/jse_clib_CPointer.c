#include "jniutil.h"
#include "jse_clib_CPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_clib_CPointer_malloc_1(JNIEnv *aEnv, jclass aClazz, jint aCount) {
    return (jlong)MALLOCN(aCount, 1);
}
JNIEXPORT jlong JNICALL Java_jse_clib_CPointer_calloc_1(JNIEnv *aEnv, jclass aClazz, jint aCount) {
    return (jlong)CALLOC(aCount, 1);
}
JNIEXPORT void JNICALL Java_jse_clib_CPointer_free_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    FREE((void *)(intptr_t)aPtr);
}

#ifdef __cplusplus
}
#endif
