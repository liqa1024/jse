#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#endif

#include "jniutil.h"
#include "jse_cptr_AnyCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_jse_cptr_AnyCPointer_typeSize_1(JNIEnv *aEnv, jclass aClazz) {
    return (jint)sizeof(void *);
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_get_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jlong)(intptr_t) *(void **)(intptr_t)aPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx) {
    return (jlong)(intptr_t) ((void **)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_AnyCPointer_set_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aValue) {
    *(void **)(intptr_t)aPtr = (void *)(intptr_t)aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_AnyCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx, jlong aValue) {
    ((void **)(intptr_t)aPtr)[aIdx] = (void *)(intptr_t)aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_next_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    void **tPtr = (void **)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_rightShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    void **tPtr = (void **)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_previous_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    void **tPtr = (void **)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_leftShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    void **tPtr = (void **)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
