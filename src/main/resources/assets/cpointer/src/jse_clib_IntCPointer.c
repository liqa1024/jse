#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#endif

#include "jniutil.h"
#include "jse_clib_IntCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_jse_clib_IntCPointer_typeSize(JNIEnv *aEnv, jclass aClazz) {
    return (jint)sizeof(int);
}
JNIEXPORT void JNICALL Java_jse_clib_IntCPointer_fill_1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jintArray aJArray, jint aStart, jint aCount) {
    parsejint2intV(aEnv, aJArray, aStart, (int *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_clib_IntCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jintArray rJArray, jint aStart, jint aCount) {
    parseint2jintV(aEnv, rJArray, aStart, (int *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT jint JNICALL Java_jse_clib_IntCPointer_get_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jint) *(int *)(intptr_t)aPtr;
}
JNIEXPORT jint JNICALL Java_jse_clib_IntCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx) {
    return (jint) ((int *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_clib_IntCPointer_set_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aValue) {
    *(int *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_clib_IntCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx, jint aValue) {
    ((int *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_clib_IntCPointer_next_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    int *tPtr = (int *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_clib_IntCPointer_rightShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    int *tPtr = (int *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_clib_IntCPointer_previous_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    int *tPtr = (int *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_clib_IntCPointer_leftShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    int *tPtr = (int *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
