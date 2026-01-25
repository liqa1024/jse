#ifndef NNAP_UTIL_H
#define NNAP_UTIL_H

#include <cmath>

namespace JSE_NNAP {

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
static constexpr int JSE_EPS_MUL = 8;
static constexpr double JSE_DBL_EPSILON = 1.0e-10;

static constexpr double SQRT2 = 1.4142135623730951;
static constexpr double SQRT2_INV = 0.7071067811865475;
static constexpr double SQRT15_INV = 0.2581988897471611;
static constexpr double SQRT96_INV = 0.10206207261596577;
static constexpr double SQRT3 = 1.7320508075688772;
static constexpr double SQRT3DIV2 = 1.224744871391589;
static constexpr double PI4 = 12.566370614359172;
static constexpr double SQRT_PI4 = 3.5449077018110318;

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

static inline double pow2(double value) noexcept {
    return value * value;
}
static inline double pow3(double value) noexcept {
    return value * value * value;
}
static inline double pow4(double value) noexcept {
    const double value2 = value * value;
    return value2 * value2;
}
static inline int numericEqual(double aLHS, double aRHS) noexcept {
    double tNorm = fabs((double)aLHS) + fabs((double)aRHS);
    if (tNorm < JSE_DBL_MIN_NORMAL * JSE_EPS_MUL) return 1;
    double tDiff = fabs((double)(aLHS - aRHS));
    return (tDiff <= tNorm * JSE_DBL_EPSILON) ? TRUE : FALSE;
}

template <int N>
static inline void fill(double *rArray, double aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] = aValue;
    }
}
template <int N>
static inline void fill(double *rArrayL, double *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] = aArrayR[i];
    }
}

template <int N>
static inline void multiply(double *rArray, double aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] *= aValue;
    }
}
template <int N>
static inline void multiply(double *rArrayL, double *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] *= aArrayR[i];
    }
}

template <int N>
static inline double dot(double *aArray) noexcept {
    double rDot = 0.0;
    for (int i = 0; i < N; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
template <int N>
static inline double dot(double *aArrayL, double *aArrayR) noexcept {
    double rDot = 0.0;
    for (int i = 0; i < N; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}

template <int N>
static inline void mplus(double *rArrayL, double aMul, double *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aMul*aArrayR[i];
    }
}
template <int N>
static inline void mplus(double *rArrayL, double *aArrayMul1, double *aArrayMul2) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aArrayMul1[i]*aArrayMul2[i];
    }
}

template <int N>
static inline void mplus2(double *rArrayL1, double *rArrayL2, double aMul1, double aMul2, double *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        const double tRHS = aArrayR[i];
        rArrayL1[i] += aMul1*tRHS;
        rArrayL2[i] += aMul2*tRHS;
    }
}

template <int N>
static inline void chebyshevFull(double aX, double *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = 1.0;
    if (N == 0) return;
    rDest[1] = aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}
template <int N>
static inline void chebyshev2Full(double aX, double *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = 1.0;
    if (N == 0) return;
    rDest[1] = 2.0*aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}

static inline double calFc(double aDis, double aRCut) noexcept {
    return pow4(1.0 - pow2(aDis/aRCut));
}
static inline double calFc(double aDis, double aRCutL, double aRCutR) noexcept {
    const double tX = (aDis-aRCutL)/(aRCutR-aRCutL);
    return pow4(1.0 - pow2(tX+tX - 1.0));
}
template <int N>
static inline void calRn(double *rRn, double aDis, double aRCut) noexcept {
    double tRnX = aDis/aRCut;
    tRnX = 1.0 - (tRnX+tRnX);
    chebyshevFull<N>(tRnX, rRn);
}
template <int N>
static inline void calRn(double *rRn, double aDis, double aRCutL, double aRCutR) noexcept {
    double tRnX = (aDis-aRCutL)/(aRCutR-aRCutL);
    tRnX = 1.0 - (tRnX+tRnX);
    chebyshevFull<N>(tRnX, rRn);
}

static inline void calFcPxyz(double *rFcPx, double *rFcPy, double *rFcPz,
                             double aDis, double aRCut, double aDx, double aDy, double aDz) noexcept {
    double fcMul = 1.0 - pow2(aDis/aRCut);
    double fcPMul = 8.0 * pow3(fcMul) / (aRCut*aRCut);
    *rFcPx = aDx * fcPMul;
    *rFcPy = aDy * fcPMul;
    *rFcPz = aDz * fcPMul;
}
static inline void calFcPxyz(double *rFcPx, double *rFcPy, double *rFcPz,
                             double aDis, double aRCutL, double aRCutR, double aDx, double aDy, double aDz) noexcept {
    const double tRCutRL = aRCutR-aRCutL;
    const double tX = (aDis-aRCutL)/tRCutRL;
    double fcMul = 1.0 - pow2(tX+tX - 1.0);
    double fcPMul = 16.0 * pow3(fcMul) * (2.0 - (aRCutL+aRCutR)/aDis) / (tRCutRL*tRCutRL);
    *rFcPx = aDx * fcPMul;
    *rFcPy = aDy * fcPMul;
    *rFcPz = aDz * fcPMul;
}
template <int N>
static inline void calRnPxyz(double *rRnPx, double *rRnPy, double *rRnPz, double *rCheby2,
                             double aDis, double aRCut, double aDx, double aDy, double aDz) noexcept {
    double tRnX = aDis/aRCut;
    tRnX = 1.0 - (tRnX+tRnX);
    chebyshev2Full<N-1>(tRnX, rCheby2);
    const double tRnPMul = 2.0 / (aDis*aRCut);
    rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
    for (int n = 1; n <= N; ++n) {
        const double tRnP = n*tRnPMul*rCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}
template <int N>
static inline void calRnPxyz(double *rRnPx, double *rRnPy, double *rRnPz, double *rCheby2,
                             double aDis, double aRCutL, double aRCutR, double aDx, double aDy, double aDz) noexcept {
    const double tRCutRL = aRCutR-aRCutL;
    double tRnX = (aDis-aRCutL)/tRCutRL;
    tRnX = 1.0 - (tRnX+tRnX);
    chebyshev2Full<N-1>(tRnX, rCheby2);
    const double tRnPMul = 2.0 / (aDis*tRCutRL);
    rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
    for (int n = 1; n <= N; ++n) {
        const double tRnP = n*tRnPMul*rCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}

}

#endif //NNAP_UTIL_H
