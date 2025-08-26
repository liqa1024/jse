#ifndef NNAP_UTIL_H
#define NNAP_UTIL_H

#include "jniutil.h"
#include <cmath>

namespace JSE_NNAP {

#undef JSE_DBL_MIN_NORMAL
#define JSE_DBL_MIN_NORMAL (2.2250738585072014E-308)
#undef JSE_EPS_MUL
#define JSE_EPS_MUL (8)
#undef JSE_DBL_EPSILON
#define JSE_DBL_EPSILON (1.0e-10)

#undef SQRT2
#define SQRT2 (1.4142135623730951)
#undef SQRT2_INV
#define SQRT2_INV (0.7071067811865475)
#undef SQRT3
#define SQRT3 (1.7320508075688772)
#undef SQRT3DIV2
#define SQRT3DIV2 (1.224744871391589)

#undef PI4
#define PI4 (12.566370614359172)

static inline jdouble pow2(jdouble value) {
    return value * value;
}
static inline jdouble pow3(jdouble value) {
    return value * value * value;
}
static inline jdouble pow4(jdouble value) {
    jdouble value2 = value * value;
    return value2 * value2;
}
static inline jboolean numericEqual(jdouble aLHS, jdouble aRHS) {
    jdouble tNorm = fabs((double)aLHS) + fabs((double)aRHS);
    if (tNorm < JSE_DBL_MIN_NORMAL * JSE_EPS_MUL) return JNI_TRUE;
    jdouble tDiff = fabs((double)(aLHS - aRHS));
    return (tDiff <= tNorm * JSE_DBL_EPSILON) ? JNI_TRUE : JNI_FALSE;
}

template <jint N>
static inline jdouble dot(jdouble *aArray) {
    jdouble rDot = 0.0;
    for (jint i = 0; i < N; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
static inline jdouble dot(jdouble *aArray, jint aLen) {
    jdouble rDot = 0.0;
    for (jint i = 0; i < aLen; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
template <jint N>
static inline jdouble dot(jdouble *aArrayL, jdouble *aArrayR) {
    jdouble rDot = 0.0;
    for (jint i = 0; i < N; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}
static inline jdouble dot(jdouble *aArrayL, jdouble *aArrayR, jint aLen) {
    jdouble rDot = 0.0;
    for (jint i = 0; i < aLen; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}

template <jint N>
static inline void chebyshevFull(jdouble aX, jdouble *rDest) {
    if (N < 0) return;
    rDest[0] = 1.0;
    if (N == 0) return;
    rDest[1] = aX;
    for (jint n = 2; n <= N; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}
template <jint N>
static inline void chebyshev2Full(jdouble aX, jdouble *rDest) {
    if (N < 0) return;
    rDest[0] = 1.0;
    if (N == 0) return;
    rDest[1] = 2.0*aX;
    for (jint n = 2; n <= N; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}

template <jint N>
static inline void calRnPxyz(jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *aCheby2,
                             jdouble aDis, jdouble aRCut, jdouble aWt, jdouble aDx, jdouble aDy, jdouble aDz) {
    const jdouble tRnPMul = 2.0 * aWt / (aDis*aRCut);
    rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
    for (jint n = 1; n <= N; ++n) {
        const jdouble tRnP = n*tRnPMul*aCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}

}

#endif //NNAP_UTIL_H
