#include "jse_cptr_Int64CPointer.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_jse_cptr_Int64CPointer_typeSize_1(JNIEnv *aEnv, jclass aClazz) {
    return (jlong)sizeof(int64_t);
}
JNIEXPORT void JNICALL Java_jse_cptr_Int64CPointer_fill0(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jlongArray aJArray, jint aStart, jint aCount) {
    parsejlong2int64_tV(aEnv, aJArray, aStart, (int64_t *)(intptr_t)rPtr, 0, aCount);
}
JNIEXPORT void JNICALL Java_jse_cptr_Int64CPointer_fill1(JNIEnv *aEnv, jclass aClazz, jlong rPtr, jlong aValue, jlong aCount) {
    int64_t *tPtr = (int64_t *)(intptr_t)rPtr;
    for (jlong i = 0; i < aCount; ++i) {
        tPtr[i] = (int64_t)aValue;
    }
}
JNIEXPORT void JNICALL Java_jse_cptr_Int64CPointer_parse2dest_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlongArray rJArray, jint aStart, jint aCount) {
    parseint64_t2jlongV(aEnv, rJArray, aStart, (const int64_t *)(intptr_t)aPtr, 0, aCount);
}
JNIEXPORT jlong JNICALL Java_jse_cptr_Int64CPointer_get_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    return (jlong) *(int64_t *)(intptr_t)aPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_Int64CPointer_getAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx) {
    return (jlong) ((int64_t *)(intptr_t)aPtr)[aIdx];
}
JNIEXPORT void JNICALL Java_jse_cptr_Int64CPointer_set_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aValue) {
    *(int64_t *)(intptr_t)aPtr = aValue;
}
JNIEXPORT void JNICALL Java_jse_cptr_Int64CPointer_putAt_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aIdx, jlong aValue) {
    ((int64_t *)(intptr_t)aPtr)[aIdx] = aValue;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_Int64CPointer_next_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    int64_t *tPtr = (int64_t *)(intptr_t)aPtr;
    ++tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_Int64CPointer_rightShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    int64_t *tPtr = (int64_t *)(intptr_t)aPtr;
    tPtr += aCount;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_Int64CPointer_previous_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr) {
    int64_t *tPtr = (int64_t *)(intptr_t)aPtr;
    --tPtr;
    return (jlong)(intptr_t)tPtr;
}
JNIEXPORT jlong JNICALL Java_jse_cptr_Int64CPointer_leftShift_1(JNIEnv *aEnv, jclass aClazz, jlong aPtr, jlong aCount) {
    int64_t *tPtr = (int64_t *)(intptr_t)aPtr;
    tPtr -= aCount;
    return (jlong)(intptr_t)tPtr;
}

#ifdef __cplusplus
}
#endif
