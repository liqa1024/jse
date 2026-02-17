#include "jse_cptr_NestedIntCPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_jse_cptr_NestedIntCPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jintArray aJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsejint2nestedintV(aEnv, aJArray, aStart, (int **)(intptr_t)rPtr, 0, aRowNum, aColNum);
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedIntCPointer_fill2(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jint aValue, jlong aRowNum, jlong aColNum) {
    int **tPtr = (int **)(intptr_t)rPtr;
    for (jlong i = 0; i < aRowNum; ++i) {
        int *tRow = tPtr[i];
        if (tRow == NULL) break;
        for (jlong j = 0; j < aColNum; ++j) {
            tRow[j] = (int)aValue;
        }
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedIntCPointer_parse2dest0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jintArray rJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsenestedint2jintV(aEnv, rJArray, aStart, (const int **)(intptr_t)aPtr, 0, aRowNum, aColNum);
}
JNIEXPORT jint JNICALL Java_jse_cptr_NestedIntCPointer_getAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aRowNum, jlong aColNum) {
    return (jint) ((int **)(intptr_t)aPtr)[aRowNum][aColNum];
}
JNIEXPORT void JNICALL Java_jse_cptr_NestedIntCPointer_putAt0(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aRowNum, jlong aColNum, jint aValue) {
    ((int **)(intptr_t)aPtr)[aRowNum][aColNum] = aValue;
}

#ifdef __cplusplus
}
#endif
