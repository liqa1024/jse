#include "jniutil.h"
#include <math.h>

#ifndef MATH_UTIL_H
#define MATH_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

static inline jdouble sum_jse(jdouble *aArray, jint aLen) {
    jdouble rSum = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rSum += tBuf[j];
        }
    }
    for (; i < aLen; ++i) {
        rSum += aArray[i];
    }
    return rSum;
}
static inline jdouble prod_jse(jdouble *aArray, jint aLen) {
    jdouble rProd = 1.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rProd *= tBuf[j];
        }
    }
    for (; i < aLen; ++i) {
        rProd *= aArray[i];
    }
    return rProd;
}
static inline jdouble dot_jse(jdouble *aArray, jint aLen) {
    jdouble rDot = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rDot += tBuf[j]*tBuf[j];
        }
    }
    for (; i < aLen; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
static inline jdouble dotAB_jse(jdouble *aArrayL, jdouble *aArrayR, jint aLen) {
    jdouble rDot = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBufL = aArrayL, *tBufR = aArrayR; i < tEnd; i += JSE_BATCH_SIZE, tBufL += JSE_BATCH_SIZE, tBufR += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rDot += tBufL[j]*tBufR[j];
        }
    }
    for (; i < aLen; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}
static inline jdouble norm1_jse(jdouble *aArray, jint aLen) {
    jdouble rNorm = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rNorm += fabs((double)tBuf[j]);
        }
    }
    for (; i < aLen; ++i) {
        rNorm += fabs((double)aArray[i]);
    }
    return rNorm;
}

#ifdef __cplusplus
}
#endif

#endif //MATH_UTIL_H
