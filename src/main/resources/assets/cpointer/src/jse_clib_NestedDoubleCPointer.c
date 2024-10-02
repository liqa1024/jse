#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#endif

#include "jniutil.h"
#include "jse_clib_NestedDoubleCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_jse_clib_NestedDoubleCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsejdouble2nesteddoubleV(aEnv, aJArray, aStart, (double **)(intptr_t)rPtr, 0, aRowNum, aColNum);
}
JNIEXPORT void JNICALL Java_jse_clib_NestedDoubleCPointer_fill1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdouble aValue, jint aRowNum, jint aColNum) {
    double **itt = (double **)(intptr_t)rPtr;
    for (jsize i = 0; i < aRowNum; ++i) {
        double *it = *itt;
        if (it == NULL) break;
        for (jsize j = 0; j < aColNum; ++j) {
            *it = (double)aValue;
            ++it;
        }
        ++itt;
    }
}
JNIEXPORT void JNICALL Java_jse_clib_NestedDoubleCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsenesteddouble2jdoubleV(aEnv, rJArray, aStart, (const double **)(intptr_t)aPtr, 0, aRowNum, aColNum);
}
JNIEXPORT jdouble JNICALL Java_jse_clib_NestedDoubleCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aRowNum, jint aColNum) {
    return (jdouble) ((double **)(intptr_t)aPtr)[aRowNum][aColNum];
}
JNIEXPORT void JNICALL Java_jse_clib_NestedDoubleCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aRowNum, jint aColNum, jdouble aValue) {
    ((double **)(intptr_t)aPtr)[aRowNum][aColNum] = aValue;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
