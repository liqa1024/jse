#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#endif

#include "jniutil.h"
#include "jse_cptr_DoubleCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_jse_cptr_DoubleCPointer_typeSize_1(JNIEnv *aEnv, jclass aClazz) {
    return (jint)sizeof(double);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    parsejdouble2doubleV(aEnv, aJArray, aStart, (double *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fill1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdouble aValue, jint aCount) {
    double *it = (double *)(intptr_t)rPtr;
    for (jsize i = 0; i < aCount; ++i) {
        *it = (double)aValue;
        ++it;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    parsedouble2jdoubleV(aEnv, rJArray, aStart, (const double *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT jdouble JNICALL Java_jse_cptr_DoubleCPointer_get_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jdouble) *(double *)(intptr_t)aPtr;
}
JNIEXPORT jdouble JNICALL Java_jse_cptr_DoubleCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx) {
    return (jdouble) ((double *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_set_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdouble aValue) {
    *(double *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx, jdouble aValue) {
    ((double *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_next_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_rightShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_previous_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_leftShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
