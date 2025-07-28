#include "jse_math_operation_ARRAY_Native.h"
#include "jniutil.h"

#ifdef __cplusplus
extern "C" {
#endif

static inline jdouble dot_jse(jdouble *aArray, jint aLen) {
    jdouble rDot = 0.0;
    for (jint i = 0; i < aLen; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
static inline jdouble dotAB_jse(jdouble *aArrayL, jdouble *aArrayR, jint aLen) {
    jdouble rDot = 0.0;
    for (jint i = 0; i < aLen; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
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

#ifdef __cplusplus
}
#endif
