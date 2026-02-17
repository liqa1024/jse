#include "jse_cptr_NestedDoubleCPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_jse_cptr_NestedDoubleCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdoubleArray aJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsejdouble2nesteddoubleV(aEnv, aJArray, aStart, (double **)(intptr_t)rPtr, 0, aRowNum, aColNum);
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedDoubleCPointer_fill2(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jdouble aValue, jlong aRowNum, jlong aColNum) {
    double **tPtr = (double **)(intptr_t)rPtr;
    for (jlong i = 0; i < aRowNum; ++i) {
        double *tRow = tPtr[i];
        if (tRow == NULL) break;
        for (jlong j = 0; j < aColNum; ++j) {
            tRow[j] = (double)aValue;
        }
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedDoubleCPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jdoubleArray rJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsenesteddouble2jdoubleV(aEnv, rJArray, aStart, (const double **)(intptr_t)aPtr, 0, aRowNum, aColNum);
}
JNIEXPORT jdouble JNICALL Java_jse_cptr_NestedDoubleCPointer_getAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aRowNum, jlong aColNum) {
    return (jdouble) ((double **)(intptr_t)aPtr)[aRowNum][aColNum];
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedDoubleCPointer_putAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aRowNum, jlong aColNum, jdouble aValue) {
    ((double **)(intptr_t)aPtr)[aRowNum][aColNum] = aValue;
}

#ifdef __cplusplus
}
#endif
