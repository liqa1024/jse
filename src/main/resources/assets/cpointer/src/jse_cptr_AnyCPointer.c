#include "jse_cptr_AnyCPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_typeSize0(JNIEnv *aEnv, jclass aClazz) {
    return (jlong)sizeof(void *);
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_get0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jlong)(intptr_t) *(void **)(intptr_t)aPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_getAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx) {
    return (jlong)(intptr_t) ((void **)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_AnyCPointer_set0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aValue) {
    *(void **)(intptr_t)aPtr = (void *)(intptr_t)aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_AnyCPointer_putAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx, jlong aValue) {
    ((void **)(intptr_t)aPtr)[aIdx] = (void *)(intptr_t)aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_next0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    void **tPtr = (void **)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_rightShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    void **tPtr = (void **)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_previous0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    void **tPtr = (void **)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_AnyCPointer_leftShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    void **tPtr = (void **)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif
