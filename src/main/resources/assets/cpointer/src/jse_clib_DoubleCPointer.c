#include "jniutil.h"
#include "jse_clib_DoubleCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_jse_clib_DoubleCPointer_typeSize(JNIEnv *aEnv, jclass aClazz) {
    return sizeof(double);
}
JNIEXPORT void JNICALL Java_jse_clib_DoubleCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    parsedouble2jdoubleV(aEnv, rJArray, aStart, (double *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT jdouble JNICALL Java_jse_clib_DoubleCPointer_get_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return *(double *)(intptr_t)aPtr;
}
JNIEXPORT jdouble JNICALL Java_jse_clib_DoubleCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx) {
    return ((double *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_clib_DoubleCPointer_set_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdouble aValue) {
    *(double *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_clib_DoubleCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx, jdouble aValue) {
    ((double *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_clib_DoubleCPointer_next_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_clib_DoubleCPointer_rightShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_clib_DoubleCPointer_previous_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_clib_DoubleCPointer_leftShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif
