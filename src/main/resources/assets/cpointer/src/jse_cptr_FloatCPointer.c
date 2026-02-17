#include "jse_cptr_FloatCPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_typeSize0(JNIEnv *aEnv, jclass aClazz) {
    return (jlong)sizeof(float);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aCount) {
    parsejfloat2floatV(aEnv, aJArray, aStart, (float *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_fillD0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    parsejdouble2floatV(aEnv, aJArray, aStart, (float *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_fillD1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jlong aData, jlong aCount) {
    float *tPtr = (float *)(intptr_t)rPtr;
    double *tData = (double *)(intptr_t)aData;
    for (jlong i = 0; i < aCount; ++i) {
        tPtr[i] = (float)tData[i];
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_fill2(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloat aValue, jlong aCount) {
    float *tPtr = (float *)(intptr_t)rPtr;
    for (jlong i = 0; i < aCount; ++i) {
        tPtr[i] = (float)aValue;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aCount) {
    parsefloat2jfloatV(aEnv, rJArray, aStart, (const float *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_parse2destD0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    parsefloat2jdoubleV(aEnv, rJArray, aStart, (const float *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_parse2destD1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong rDest, jlong aCount) {
    float *tPtr = (float *)(intptr_t)aPtr;
    double *tDest = (double *)(intptr_t)rDest;
    for (jlong i = 0; i < aCount; ++i) {
        tDest[i] = (double)tPtr[i];
    }
}
JNIEXPORT jfloat JNICALL Java_jse_cptr_FloatCPointer_get0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jfloat) *(float *)(intptr_t)aPtr;
}
JNIEXPORT jfloat JNICALL Java_jse_cptr_FloatCPointer_getAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx) {
    return (jfloat) ((float *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_set0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloat aValue) {
    *(float *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_putAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx, jfloat aValue) {
    ((float *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_next0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    float *tPtr = (float *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_rightShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    float *tPtr = (float *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_previous0(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    float *tPtr = (float *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_leftShift0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    float *tPtr = (float *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif
