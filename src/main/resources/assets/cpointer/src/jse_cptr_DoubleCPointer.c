#include "jse_cptr_DoubleCPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_typeSize0(JNIEnv *aEnv, jclass aClazz) {
    return (jlong)sizeof(double);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    parsejdouble2doubleV(aEnv, aJArray, aStart, (double *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fillF0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aCount) {
    parsejfloat2doubleV(aEnv, aJArray, aStart, (double *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fillF1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jlong aData, jlong aCount) {
    double *tPtr = (double *)(intptr_t)rPtr;
    float *tData = (float *)(intptr_t)aData;
    for (jlong i = 0; i < aCount; ++i) {
        tPtr[i] = (double)tData[i];
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_fill2(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdouble aValue, jlong aCount) {
    double *tPtr = (double *)(intptr_t)rPtr;
    for (jlong i = 0; i < aCount; ++i) {
        tPtr[i] = (double)aValue;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    parsedouble2jdoubleV(aEnv, rJArray, aStart, (const double *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_parse2destF0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aCount) {
    parsedouble2jfloatV(aEnv, rJArray, aStart, (const double *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_parse2destF1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong rDest, jlong aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    float *tDest = (float *)(intptr_t)rDest;
    for (jlong i = 0; i < aCount; ++i) {
        tDest[i] = (float)tPtr[i];
    }
}
JNIEXPORT jdouble JNICALL Java_jse_cptr_DoubleCPointer_get0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jdouble) *(double *)(intptr_t)aPtr;
}
JNIEXPORT jdouble JNICALL Java_jse_cptr_DoubleCPointer_getAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx) {
    return (jdouble) ((double *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_set0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdouble aValue) {
    *(double *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_DoubleCPointer_putAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx, jdouble aValue) {
    ((double *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_next0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_rightShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_previous0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    double *tPtr = (double *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_DoubleCPointer_leftShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    double *tPtr = (double *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif
