#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#endif

#include "jniutil.h"
#include "jse_clib_NestedIntCPointer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_jse_clib_NestedIntCPointer_fill_1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jintArray aJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsejint2nestedintV(aEnv, aJArray, aStart, (int **)(intptr_t)rPtr, 0, aRowNum, aColNum);
}
JNIEXPORT void JNICALL Java_jse_clib_NestedIntCPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jintArray rJArray, jint aStart, jint aRowNum, jint aColNum) {
    parsenestedint2jintV(aEnv, rJArray, aStart, (const int **)(intptr_t)aPtr, 0, aRowNum, aColNum);
}
JNIEXPORT jint JNICALL Java_jse_clib_NestedIntCPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aRowNum, jint aColNum) {
    return (jint) ((int **)(intptr_t)aPtr)[aRowNum][aColNum];
}
JNIEXPORT void JNICALL Java_jse_clib_NestedIntCPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jint aRowNum, jint aColNum, jint aValue) {
    ((int **)(intptr_t)aPtr)[aRowNum][aColNum] = aValue;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
