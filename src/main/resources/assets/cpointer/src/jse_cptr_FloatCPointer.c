#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#endif

#include "jniutil.h"
#include "jse_cptr_FloatCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

static inline void parsefloat2jfloatV(JNIEnv *aEnv, jfloatArray rJArray, jsize aJStart, const float *aBuf, jsize aBStart, jsize aLen) {
    if (rJArray==NULL || aBuf==NULL) return;
#ifdef __cplusplus
    // jfloat is always float
    aEnv->SetFloatArrayRegion(rJArray, aJStart, aLen, (aBuf+aBStart));
#else
    // jfloat is always float
    (*aEnv)->SetFloatArrayRegion(aEnv, rJArray, aJStart, aLen, (aBuf+aBStart));
#endif
}
static inline void parsejfloat2floatV(JNIEnv *aEnv, jfloatArray aJArray, jsize aJStart, float *rBuf, jsize aBStart, jsize aLen) {
    if (aJArray==NULL || rBuf==NULL) return;
#ifdef __cplusplus
    // jfloat is always float
    aEnv->GetFloatArrayRegion(aJArray, aJStart, aLen, (rBuf+aBStart));
#else
    // jfloat is always float
    (*aEnv)->GetFloatArrayRegion(aEnv, aJArray, aJStart, aLen, (rBuf+aBStart));
#endif
}

GEN_PARSE_ANY_TO_JANY(float, jdouble)
GEN_PARSE_JANY_TO_ANY(jdouble, float)


JNIEXPORT jint JNICALL Java_jse_cptr_FloatCPointer_typeSize_1(JNIEnv *aEnv, jclass aClazz) {
    return (jint)sizeof(float);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aCount) {
    parsejfloat2floatV(aEnv, aJArray, aStart, (float *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_fillD0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aCount) {
    parsejdouble2floatV(aEnv, aJArray, aStart, (float *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_fill1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloat aValue, jint aCount) {
    float *it = (float *)(intptr_t)rPtr;
    for (jsize i = 0; i < aCount; ++i) {
        *it = (float)aValue;
        ++it;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aCount) {
    parsefloat2jfloatV(aEnv, rJArray, aStart, (const float *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_parse2destD_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aCount) {
    parsefloat2jdoubleV(aEnv, rJArray, aStart, (const float *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT jfloat JNICALL Java_jse_cptr_FloatCPointer_get_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jfloat) *(float *)(intptr_t)aPtr;
}
JNIEXPORT jfloat JNICALL Java_jse_cptr_FloatCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx) {
    return (jfloat) ((float *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_set_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloat aValue) {
    *(float *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_FloatCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aIdx, jfloat aValue) {
    ((float *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_next_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    float *tPtr = (float *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_rightShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    float *tPtr = (float *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_previous_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    float *tPtr = (float *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_FloatCPointer_leftShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aCount) {
    float *tPtr = (float *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
