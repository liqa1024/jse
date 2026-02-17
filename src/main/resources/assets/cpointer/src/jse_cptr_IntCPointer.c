#include "jse_cptr_IntCPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_cptr_IntCPointer_typeSize0(JNIEnv *aEnv, jclass aClazz) {
    return (jlong)sizeof(int);
}
JNIEXPORT void JNICALL Java_jse_cptr_IntCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jintArray aJArray, jint aStart, jint aCount) {
    parsejint2intV(aEnv, aJArray, aStart, (int *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_IntCPointer_fill2(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jint aValue, jlong aCount) {
    int *tPtr = (int *)(intptr_t)rPtr;
    for (jlong i = 0; i < aCount; ++i) {
        tPtr[i] = (int)aValue;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_IntCPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jintArray rJArray, jint aStart, jint aCount) {
    parseint2jintV(aEnv, rJArray, aStart, (const int *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT jint JNICALL Java_jse_cptr_IntCPointer_get0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jint) *(int *)(intptr_t)aPtr;
}
JNIEXPORT jint JNICALL Java_jse_cptr_IntCPointer_getAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx) {
    return (jint) ((int *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_IntCPointer_set0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aValue) {
    *(int *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_IntCPointer_putAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx, jint aValue) {
    ((int *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_IntCPointer_next0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    int *tPtr = (int *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_IntCPointer_rightShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    int *tPtr = (int *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_IntCPointer_previous0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    int *tPtr = (int *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_IntCPointer_leftShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    int *tPtr = (int *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif
