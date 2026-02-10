#include "jse_cptr_DoubleCPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_typeSize_1(JNIEnv *aEnv, jclass aClazz) {
    return (jlong)sizeof(double);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    parsejdouble2doubleV(aEnv, aJArray, aStart, (double *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fillF0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aCount) {
    parsejfloat2doubleV(aEnv, aJArray, aStart, (double *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fill1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdouble aValue, jlong aCount) {
    double *tPtr = (double *)(intptr_t)rPtr;
    for (jlong i = 0; i < aCount; ++i) {
        tPtr[i] = (double)aValue;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    parsedouble2jdoubleV(aEnv, rJArray, aStart, (const double *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_parse2destF_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aCount) {
    parsedouble2jfloatV(aEnv, rJArray, aStart, (const double *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT jdouble JNICALL Java_jse_cptr_DoubleCPointer_get_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jdouble) *(double *)(intptr_t)aPtr;
}
JNIEXPORT jdouble JNICALL Java_jse_cptr_DoubleCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx) {
    return (jdouble) ((double *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_set_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdouble aValue) {
    *(double *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx, jdouble aValue) {
    ((double *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_next_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_rightShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_previous_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_leftShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif
