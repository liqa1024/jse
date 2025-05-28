#include <jni.h>
#include <math.h>

#ifndef BASIS_UTIL_H
#define BASIS_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

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

static inline double pow2_jse(double value) {
    return value * value;
}
static inline double pow3_jse(double value) {
    return value * value * value;
}
static inline double pow4_jse(double value) {
    double value2 = value * value;
    return value2 * value2;
}
static inline jboolean numericEqual_jse(double aLHS, double aRHS) {
    double tNorm = fabs(aLHS) + fabs(aRHS);
    if (tNorm < JSE_DBL_MIN_NORMAL * JSE_EPS_MUL) return JNI_TRUE;
    double tDiff = fabs(aLHS - aRHS);
    return (tDiff <= tNorm * JSE_DBL_EPSILON) ? JNI_TRUE : JNI_FALSE;
}

static inline double dot_jse(double *aArray, jint aLen) {
    double rDot = 0.0;
    for (int i = 0; i < aLen; ++i) {
        double tValue = aArray[i];
        rDot += tValue*tValue;
    }
    return rDot;
}
static inline double dotAB_jse(double *aArrayL, double *aArrayR, jint aLen) {
    double rDot = 0.0;
    for (int i = 0; i < aLen; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}

static inline void chebyshevFull(jint aN, double aX, double *rDest) {
    if (aN < 0) return;
    rDest[0] = 1.0;
    if (aN == 0) return;
    rDest[1] = aX;
    for (jint n = 2; n <= aN; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}
static inline void chebyshev2Full(jint aN, double aX, double *rDest) {
    if (aN < 0) return;
    rDest[0] = 1.0;
    if (aN == 0) return;
    rDest[1] = 2.0*aX;
    for (jint n = 2; n <= aN; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}

static inline void calRnPxyz(double *rRnPx, double *rRnPy, double *rRnPz, double *aCheby2, jint aNMax,
                             double aDis, double aRCut, double aWt, double aDx, double aDy, double aDz) {
    const double tRnPMul = 2.0 * aWt / (aDis*aRCut);
    rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
    for (jint n = 1; n <= aNMax; ++n) {
        const double tRnP = n*tRnPMul*aCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}

#ifdef __cplusplus
}
#endif


#endif //BASIS_UTIL_H
