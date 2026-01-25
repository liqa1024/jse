#ifndef NNAP_UTIL_H
#define NNAP_UTIL_H

#include <cmath>

namespace JSE_NNAP {

// >>> NNAPGEN PICK
// --- NNAPGEN PICK: double
// >>> NNAPGEN REMOVE
/*
// <<< NNAPGEN REMOVE
typedef double flt_t;
// >>> NNAPGEN REMOVE
*/
// <<< NNAPGEN REMOVE
// --- NNAPGEN PICK: single
typedef float flt_t;
// <<< NNAPGEN PICK [PRECISION]

static constexpr int WTYPE_DEFAULT = 0;
static constexpr int WTYPE_NONE    = -1;
static constexpr int WTYPE_SINGLE  = 1; // unused
static constexpr int WTYPE_FULL    = 2;
static constexpr int WTYPE_EXFULL  = 3;
static constexpr int WTYPE_FUSE    = 4;
static constexpr int WTYPE_RFUSE   = 5; // unused
static constexpr int WTYPE_EXFUSE  = 6;

static constexpr int FSTYLE_LIMITED = 0;
static constexpr int FSTYLE_EXTENSIVE = 1;

static constexpr int TRUE = 1;
static constexpr int FALSE = 0;

static constexpr double JSE_DBL_MIN_NORMAL = 2.2250738585072014E-308;
static constexpr float JSE_FLT_MIN_NORMAL = 1.17549435E-38f;
static constexpr int JSE_EPS_MUL = 8;
static constexpr double JSE_DBL_EPSILON = 1.0e-12;
static constexpr float JSE_FLT_EPSILON = 1.0e-5f;

static constexpr flt_t ZERO = 0.0;
static constexpr flt_t ONE = 1.0;
static constexpr flt_t TWO = 2.0;
static constexpr flt_t SQRT2 = 1.4142135623730951;
static constexpr flt_t SQRT2_INV = 0.7071067811865475;
static constexpr flt_t SQRT15_INV = 0.2581988897471611;
static constexpr flt_t SQRT96_INV = 0.10206207261596577;
static constexpr flt_t SQRT3 = 1.7320508075688772;
static constexpr flt_t SQRT3DIV2 = 1.224744871391589;
static constexpr flt_t PI4 = 12.566370614359172;
static constexpr flt_t SQRT_PI4 = 3.5449077018110318;

static constexpr int toInternalWType(int aWType, int aTypeNum) noexcept {
    if (aTypeNum==1) {
        switch(aWType) {
        case WTYPE_EXFULL: case WTYPE_FULL: case WTYPE_NONE: case WTYPE_DEFAULT: {
            return WTYPE_NONE;
        }
        default: {
            return aWType;
        }}
    } else {
        return aWType;
    }
};

static inline flt_t pow2(flt_t value) noexcept {
    return value * value;
}
static inline flt_t pow3(flt_t value) noexcept {
    return value * value * value;
}
static inline flt_t pow4(flt_t value) noexcept {
    const flt_t value2 = value * value;
    return value2 * value2;
}
static inline int numericEqual(double aLHS, double aRHS) noexcept {
    double tNorm = fabs(aLHS) + fabs(aRHS);
    if (tNorm < JSE_DBL_MIN_NORMAL * JSE_EPS_MUL) return 1;
    double tDiff = fabs(aLHS - aRHS);
    return (tDiff <= tNorm * JSE_DBL_EPSILON) ? TRUE : FALSE;
}
static inline int numericEqual(float aLHS, float aRHS) noexcept {
    float tNorm = fabs(aLHS) + fabs(aRHS);
    if (tNorm < JSE_FLT_MIN_NORMAL * JSE_EPS_MUL) return 1;
    float tDiff = fabs(aLHS - aRHS);
    return (tDiff <= tNorm * JSE_FLT_EPSILON) ? TRUE : FALSE;
}

template <int N>
static inline void fill(flt_t *rArray, flt_t aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] = aValue;
    }
}
template <int N>
static inline void fill(flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] = aArrayR[i];
    }
}

template <int N>
static inline void multiply(flt_t *rArray, flt_t aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] *= aValue;
    }
}
template <int N>
static inline void multiply(flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] *= aArrayR[i];
    }
}

template <int N>
static inline flt_t dot(flt_t *aArray) noexcept {
    flt_t rDot = 0.0;
    for (int i = 0; i < N; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
template <int N>
static inline flt_t dot(flt_t *aArrayL, flt_t *aArrayR) noexcept {
    flt_t rDot = 0.0;
    for (int i = 0; i < N; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}

template <int N>
static inline void mplus(flt_t *rArrayL, flt_t aMul, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aMul*aArrayR[i];
    }
}
template <int N>
static inline void mplus(flt_t *rArrayL, flt_t *aArrayMul1, flt_t *aArrayMul2) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aArrayMul1[i]*aArrayMul2[i];
    }
}

template <int N>
static inline void mplus2(flt_t *rArrayL1, flt_t *rArrayL2, flt_t aMul1, flt_t aMul2, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        const flt_t tRHS = aArrayR[i];
        rArrayL1[i] += aMul1*tRHS;
        rArrayL2[i] += aMul2*tRHS;
    }
}

template <int N>
static inline void chebyshevFull(flt_t aX, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = ONE;
    if (N == 0) return;
    rDest[1] = aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}
template <int N>
static inline void chebyshev2Full(flt_t aX, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = ONE;
    if (N == 0) return;
    rDest[1] = TWO*aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}

static inline flt_t calFc(flt_t aDis, flt_t aRCut) noexcept {
    return pow4(ONE - pow2(aDis/aRCut));
}
static inline flt_t calFc(flt_t aDis, flt_t aRCutL, flt_t aRCutR) noexcept {
    const flt_t tX = (aDis-aRCutL)/(aRCutR-aRCutL);
    return pow4(ONE - pow2(tX+tX - ONE));
}
template <int N>
static inline void calRn(flt_t *rRn, flt_t aDis, flt_t aRCut) noexcept {
    flt_t tRnX = aDis/aRCut;
    tRnX = ONE - (tRnX+tRnX);
    chebyshevFull<N>(tRnX, rRn);
}
template <int N>
static inline void calRn(flt_t *rRn, flt_t aDis, flt_t aRCutL, flt_t aRCutR) noexcept {
    flt_t tRnX = (aDis-aRCutL)/(aRCutR-aRCutL);
    tRnX = ONE - (tRnX+tRnX);
    chebyshevFull<N>(tRnX, rRn);
}

static inline void calFcPxyz(flt_t *rFcPx, flt_t *rFcPy, flt_t *rFcPz,
                             flt_t aDis, flt_t aRCut, flt_t aDx, flt_t aDy, flt_t aDz) noexcept {
    flt_t fcMul = ONE - pow2(aDis/aRCut);
    flt_t fcPMul = ((flt_t)8.0) * pow3(fcMul) / (aRCut*aRCut);
    *rFcPx = aDx * fcPMul;
    *rFcPy = aDy * fcPMul;
    *rFcPz = aDz * fcPMul;
}
static inline void calFcPxyz(flt_t *rFcPx, flt_t *rFcPy, flt_t *rFcPz,
                             flt_t aDis, flt_t aRCutL, flt_t aRCutR, flt_t aDx, flt_t aDy, flt_t aDz) noexcept {
    const flt_t tRCutRL = aRCutR-aRCutL;
    const flt_t tX = (aDis-aRCutL)/tRCutRL;
    flt_t fcMul = ONE - pow2(tX+tX - ONE);
    flt_t fcPMul = ((flt_t)16.0) * pow3(fcMul) * (TWO - (aRCutL+aRCutR)/aDis) / (tRCutRL*tRCutRL);
    *rFcPx = aDx * fcPMul;
    *rFcPy = aDy * fcPMul;
    *rFcPz = aDz * fcPMul;
}
template <int N>
static inline void calRnPxyz(flt_t *rRnPx, flt_t *rRnPy, flt_t *rRnPz, flt_t *rCheby2,
                             flt_t aDis, flt_t aRCut, flt_t aDx, flt_t aDy, flt_t aDz) noexcept {
    flt_t tRnX = aDis/aRCut;
    tRnX = ONE - (tRnX+tRnX);
    chebyshev2Full<N-1>(tRnX, rCheby2);
    const flt_t tRnPMul = TWO / (aDis*aRCut);
    rRnPx[0] = ZERO; rRnPy[0] = ZERO; rRnPz[0] = ZERO;
    for (int n = 1; n <= N; ++n) {
        const flt_t tRnP = ((flt_t)n)*tRnPMul*rCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}
template <int N>
static inline void calRnPxyz(flt_t *rRnPx, flt_t *rRnPy, flt_t *rRnPz, flt_t *rCheby2,
                             flt_t aDis, flt_t aRCutL, flt_t aRCutR, flt_t aDx, flt_t aDy, flt_t aDz) noexcept {
    const flt_t tRCutRL = aRCutR-aRCutL;
    flt_t tRnX = (aDis-aRCutL)/tRCutRL;
    tRnX = ONE - (tRnX+tRnX);
    chebyshev2Full<N-1>(tRnX, rCheby2);
    const flt_t tRnPMul = TWO / (aDis*tRCutRL);
    rRnPx[0] = ZERO; rRnPy[0] = ZERO; rRnPz[0] = ZERO;
    for (int n = 1; n <= N; ++n) {
        const flt_t tRnP = ((flt_t)n)*tRnPMul*rCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}

}

#endif //NNAP_UTIL_H
