#include "jse_math_operation_ARRAY_Native.h"
#include "math_util.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jdouble JNICALL Java_jse_math_operation_ARRAY_00024Native_sumOfThis_1(JNIEnv *aEnv, jclass aClazz,
    jdoubleArray aThis, jint aShift, jint aLength) {
    // java array init
    jdouble *tThis = (jdouble *)getJArrayBuf(aEnv, aThis);
    
    jdouble tOut = sum_jse(tThis+aShift, aLength);
    
    // release java array
    releaseJArrayBuf(aEnv, aThis, tThis, JNI_ABORT);
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jse_math_operation_ARRAY_00024Native_prodOfThis_1(JNIEnv *aEnv, jclass aClazz,
    jdoubleArray aThis, jint aShift, jint aLength) {
    // java array init
    jdouble *tThis = (jdouble *)getJArrayBuf(aEnv, aThis);
    
    jdouble tOut = prod_jse(tThis+aShift, aLength);
    
    // release java array
    releaseJArrayBuf(aEnv, aThis, tThis, JNI_ABORT);
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jse_math_operation_ARRAY_00024Native_dot_1(JNIEnv *aEnv, jclass aClazz,
    jdoubleArray aDataL, jint aShiftL, jdoubleArray aDataR, jint aShiftR, jint aLength) {
    // java array init
    jdouble *tDataL = (jdouble *)getJArrayBuf(aEnv, aDataL);
    jdouble *tDataR = (jdouble *)getJArrayBuf(aEnv, aDataR);
    
    jdouble tOut = dotAB_jse(tDataL+aShiftL, tDataR+aShiftR, aLength);
    
    // release java array
    releaseJArrayBuf(aEnv, aDataL, tDataL, JNI_ABORT);
    releaseJArrayBuf(aEnv, aDataR, tDataR, JNI_ABORT);
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jse_math_operation_ARRAY_00024Native_dotOfThis_1(JNIEnv *aEnv, jclass aClazz,
    jdoubleArray aThis, jint aShift, jint aLength) {
    // java array init
    jdouble *tThis = (jdouble *)getJArrayBuf(aEnv, aThis);
    
    jdouble tOut = dot_jse(tThis+aShift, aLength);
    
    // release java array
    releaseJArrayBuf(aEnv, aThis, tThis, JNI_ABORT);
    return tOut;
}

JNIEXPORT jdouble JNICALL Java_jse_math_operation_ARRAY_00024Native_norm1OfThis_1(JNIEnv *aEnv, jclass aClazz,
    jdoubleArray aThis, jint aShift, jint aLength) {
    // java array init
    jdouble *tThis = (jdouble *)getJArrayBuf(aEnv, aThis);
    
    jdouble tOut = norm1_jse(tThis+aShift, aLength);
    
    // release java array
    releaseJArrayBuf(aEnv, aThis, tThis, JNI_ABORT);
    return tOut;
}

JNIEXPORT void JNICALL Java_jse_math_operation_ARRAY_00024Native_matmulRC2Dest_1(JNIEnv *aEnv, jclass aClazz,
    jdoubleArray aDataRowL, jint aShiftL, jdoubleArray aDataColR, jint aShiftR,
    jdoubleArray rDestRow, jint rShift, jint aRowNum, jint aColNum, jint aMidNum) {
    // java array init
    jdouble *tDataRowL = (jdouble *)getJArrayBuf(aEnv, aDataRowL);
    jdouble *tDataColR = (jdouble *)getJArrayBuf(aEnv, aDataColR);
    jdouble *tDestRow = (jdouble *)getJArrayBuf(aEnv, rDestRow);
    
    matmulRC_jse(tDataRowL+aShiftL, tDataColR+aShiftR, tDestRow+rShift, aRowNum, aColNum, aMidNum);
    
    // release java array
    releaseJArrayBuf(aEnv, aDataRowL, tDataRowL, JNI_ABORT);
    releaseJArrayBuf(aEnv, aDataColR, tDataColR, JNI_ABORT);
    releaseJArrayBuf(aEnv, rDestRow, tDestRow, 0);
}

#ifdef __cplusplus
}
#endif
