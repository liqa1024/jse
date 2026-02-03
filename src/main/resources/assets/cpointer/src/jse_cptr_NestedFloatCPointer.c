#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#endif

#include "jniutil.h"
#include "jse_cptr_NestedFloatCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

GEN_PARSE_JANY_TO_NESTED_ANY(jfloat, float)
GEN_PARSE_JANY_TO_NESTED_ANY(jdouble, float)
GEN_PARSE_NESTED_ANY_TO_JANY(float, jfloat)
GEN_PARSE_NESTED_ANY_TO_JANY(float, jdouble)

JNIEXPORT void JNICALL Java_jse_cptr_NestedFloatCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloatArray aJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsejfloat2nestedfloatV(aEnv, aJArray, aStart, (float **)(intptr_t)rPtr, 0, aRowNum, aColNum);
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedFloatCPointer_fillD0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsejdouble2nestedfloatV(aEnv, aJArray, aStart, (float **)(intptr_t)rPtr, 0, aRowNum, aColNum);
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedFloatCPointer_fill1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jfloat aValue, jint aRowNum, jint aColNum) {
    float **itt = (float **)(intptr_t)rPtr;
    for (jsize i = 0; i < aRowNum; ++i) {
        float *it = *itt;
        if (it == NULL) break;
        for (jsize j = 0; j < aColNum; ++j) {
            *it = (float)aValue;
            ++it;
        }
        ++itt;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedFloatCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jfloatArray rJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsenestedfloat2jfloatV(aEnv, rJArray, aStart, (const float **)(intptr_t)aPtr, 0, aRowNum, aColNum);
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedFloatCPointer_parse2destD_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsenestedfloat2jdoubleV(aEnv, rJArray, aStart, (const float **)(intptr_t)aPtr, 0, aRowNum, aColNum);
}
JNIEXPORT jfloat JNICALL Java_jse_cptr_NestedFloatCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aRowNum, jint aColNum) {
    return (jfloat) ((float **)(intptr_t)aPtr)[aRowNum][aColNum];
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedFloatCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aRowNum, jint aColNum, jfloat aValue) {
    ((float **)(intptr_t)aPtr)[aRowNum][aColNum] = aValue;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
