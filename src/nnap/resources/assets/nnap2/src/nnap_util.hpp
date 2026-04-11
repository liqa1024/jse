#ifndef NNAP_UTIL_H
#define NNAP_UTIL_H

// >>> NNAPGEN PICK
// --- NNAPGEN PICK: cpu
#include <cmath>
#define NNAP_ARCH_CPU
#define NNAP_DEVICE
#define NNAP_HOST
// >>> NNAPGEN REMOVE
/*
// <<< NNAPGEN REMOVE
// --- NNAPGEN PICK: cuda
#define NNAP_ARCH_CUDA
#define NNAP_DEVICE __device__
#define NNAP_HOST __host__
// <<< NNAPGEN PICK [ARCH]
// >>> NNAPGEN REMOVE
*/
// <<< NNAPGEN REMOVE

// >>> NNAPGEN IF
// --- NNAPGEN HAS: [USE TABLE]
#define NNAP_USE_TABLE
#define NNAP_TABLE_SIZE __NNAPGEN_TABLE_SIZE__
// >>> NNAPGEN REMOVE
/*
// <<< NNAPGEN REMOVE
// --- NNAPGEN ELSE:
#define NNAP_TABLE_SIZE -1
// <<< NNAPGEN IF
// >>> NNAPGEN REMOVE
*/
// <<< NNAPGEN REMOVE

// >>> NNAPGEN REMOVE
#define __NNAPGEN_NTYPES__ 2
#define __NNAPGENS_X__ 0
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

// >>> NNAPGEN PICK
// --- NNAPGEN PICK: double
// >>> NNAPGEN REMOVE
/*
// <<< NNAPGEN REMOVE
#define NNAP_PRECISION_DOUBLE
typedef double flt_t;
// >>> NNAPGEN REMOVE
*/
// <<< NNAPGEN REMOVE
// --- NNAPGEN PICK: single
#define NNAP_PRECISION_SINGLE
typedef float flt_t;
// <<< NNAPGEN PICK [PRECISION]

static constexpr int WTYPE_DEFAULT = 0;
static constexpr int WTYPE_NONE    = -1;
static constexpr int WTYPE_SINGLE  = 1; // unused
static constexpr int WTYPE_FULL    = 2;
static constexpr int WTYPE_EXFULL  = 3;
static constexpr int WTYPE_FUSE    = 4;
static constexpr int WTYPE_RFUSE   = 5;
static constexpr int WTYPE_EXFUSE  = 6;

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


static inline NNAP_DEVICE double nnap_abs(double value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return abs(value);
#else
    return std::abs(value);
#endif
}
static inline NNAP_DEVICE float nnap_abs(float value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return abs(value);
#else
    return std::abs(value);
#endif
}

static inline NNAP_DEVICE double nnap_sqrt(double value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return sqrt(value);
#else
    return std::sqrt(value);
#endif
}
static inline NNAP_DEVICE float nnap_sqrt(float value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return sqrt(value);
#else
    return std::sqrt(value);
#endif
}

static inline NNAP_DEVICE double nnap_hypot(double x, double y) noexcept {
#ifdef NNAP_ARCH_CUDA
    return hypot(x, y);
#else
    return std::hypot(x, y);
#endif
}
static inline NNAP_DEVICE float nnap_hypot(float x, float y) noexcept {
#ifdef NNAP_ARCH_CUDA
    return hypot(x, y);
#else
    return std::hypot(x, y);
#endif
}

static inline NNAP_DEVICE double nnap_exp(double value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return exp(value);
#else
    return std::exp(value);
#endif
}
static inline NNAP_DEVICE float nnap_exp(float value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return exp(value);
#else
    return std::exp(value);
#endif
}


static inline NNAP_DEVICE flt_t pow2(flt_t value) noexcept {
    return value * value;
}
static inline NNAP_DEVICE flt_t pow3(flt_t value) noexcept {
    return value * value * value;
}
static inline NNAP_DEVICE flt_t pow4(flt_t value) noexcept {
    const flt_t value2 = value * value;
    return value2 * value2;
}
static inline NNAP_DEVICE int numericEqual(double aLHS, double aRHS) noexcept {
    double tNorm = nnap_abs(aLHS) + nnap_abs(aRHS);
    if (tNorm < JSE_DBL_MIN_NORMAL * JSE_EPS_MUL) return TRUE;
    double tDiff = nnap_abs(aLHS - aRHS);
    return (tDiff <= tNorm * JSE_DBL_EPSILON) ? TRUE : FALSE;
}
static inline NNAP_DEVICE int numericEqual(float aLHS, float aRHS) noexcept {
    float tNorm = nnap_abs(aLHS) + nnap_abs(aRHS);
    if (tNorm < JSE_FLT_MIN_NORMAL * JSE_EPS_MUL) return TRUE;
    float tDiff = nnap_abs(aLHS - aRHS);
    return (tDiff <= tNorm * JSE_FLT_EPSILON) ? TRUE : FALSE;
}


template <int N>
static inline NNAP_DEVICE void fill(flt_t *rArray, flt_t aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] = aValue;
    }
}
template <int N>
static inline NNAP_DEVICE void fill(flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] = aArrayR[i];
    }
}

template <int N>
static inline NNAP_DEVICE void multiply(flt_t *rArray, flt_t aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] *= aValue;
    }
}
template <int N>
static inline NNAP_DEVICE void multiply(flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] *= aArrayR[i];
    }
}

template <int N>
static inline NNAP_DEVICE flt_t dot(flt_t *aArray) noexcept {
    flt_t rDot = ZERO;
    for (int i = 0; i < N; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
template <int N>
static inline NNAP_DEVICE flt_t dot(flt_t *aArrayL, flt_t *aArrayR) noexcept {
    flt_t rDot = ZERO;
    for (int i = 0; i < N; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}
template <int N>
static inline NNAP_DEVICE flt_t dotEx(flt_t *aArrayLEx, flt_t *aArrayL, flt_t *aArrayR) noexcept {
    flt_t rDot = ZERO;
    for (int i = 0; i < N; ++i) {
        rDot += (aArrayLEx[i] + aArrayL[i])*aArrayR[i];
    }
    return rDot;
}

template <int N>
static inline NNAP_DEVICE void plus(flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aArrayR[i];
    }
}
template <int N>
static inline NNAP_DEVICE void plusEx(flt_t *rArrayLEx, flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        const flt_t tRHS = aArrayR[i];
        rArrayLEx[i] += tRHS;
        rArrayL[i] += tRHS;
    }
}

template <int N>
static inline NNAP_DEVICE void mplus(flt_t *rArrayL, flt_t aMul, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aMul*aArrayR[i];
    }
}
template <int N>
static inline NNAP_DEVICE void mplus(flt_t *rArrayL, flt_t *aArrayMul1, flt_t *aArrayMul2) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aArrayMul1[i]*aArrayMul2[i];
    }
}
template <int N>
static inline NNAP_DEVICE void mplusEx(flt_t *rArrayLEx, flt_t *rArrayL, flt_t aMul, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        const flt_t tRHS = aMul*aArrayR[i];
        rArrayLEx[i] += tRHS;
        rArrayL[i] += tRHS;
    }
}

#ifdef NNAP_USE_TABLE
static inline NNAP_DEVICE void tableInit(flt_t aX, flt_t &ix, int &il, int &ir) noexcept {
    ix = aX * (flt_t)NNAP_TABLE_SIZE;
    il = (int)ix;
    ir = il + 1;
}
static inline NNAP_DEVICE flt_t calRFuncFromTable(flt_t ix, int il, int ir, flt_t *aRFuncTable) noexcept {
    if (il < 0) return aRFuncTable[0];
    if (ir > NNAP_TABLE_SIZE) return aRFuncTable[NNAP_TABLE_SIZE];
    const flt_t fl = aRFuncTable[il];
    const flt_t fr = aRFuncTable[ir];
    return fl + (ix-(flt_t)il) * (fr-fl);
}
#endif

template <int N>
static inline NNAP_DEVICE void chebyshevFull(flt_t aX, flt_t aMul, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = aMul;
    if (N == 0) return;
    rDest[1] = aMul*aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}
template <int N>
static inline NNAP_DEVICE void chebyshev2Full(flt_t aX, flt_t aMul, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = aMul;
    if (N == 0) return;
    rDest[1] = TWO*aMul*aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}

template <int N>
static inline NNAP_DEVICE void chebyshevFull(flt_t aX, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = ONE;
    if (N == 0) return;
    rDest[1] = aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}
template <int N>
static inline NNAP_DEVICE void chebyshev2Full(flt_t aX, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = ONE;
    if (N == 0) return;
    rDest[1] = TWO*aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}

static inline NNAP_DEVICE flt_t calFc(flt_t aX) noexcept {
    return pow4(ONE - pow2(aX));
}
static inline NNAP_DEVICE flt_t calFc(flt_t aDis, flt_t aRCut) noexcept {
    return pow4(ONE - pow2(aDis/aRCut));
}
static inline NNAP_DEVICE flt_t calFc(flt_t aDis, flt_t aRCutL, flt_t aRCutR) noexcept {
    const flt_t tX = (aDis-aRCutL)/(aRCutR-aRCutL);
    return pow4(ONE - pow2(tX+tX - ONE));
}

template <int N>
static inline NNAP_DEVICE void calRnFc(flt_t *rRn, flt_t aDis, flt_t aRCut) noexcept {
    const flt_t tX = aDis/aRCut;
    const flt_t fc = calFc(tX);
    const flt_t tRnX = ONE - (tX+tX);
    chebyshevFull<N>(tRnX, fc, rRn);
}
template <int N>
static inline NNAP_DEVICE void calRn(flt_t *rRn, flt_t aDis, flt_t aRCut) noexcept {
    flt_t tRnX = aDis/aRCut;
    tRnX = ONE - (tRnX+tRnX);
    chebyshevFull<N>(tRnX, rRn);
}
template <int N>
static inline NNAP_DEVICE void calRn(flt_t *rRn, flt_t aDis, flt_t aRCutL, flt_t aRCutR) noexcept {
    flt_t tRnX = (aDis-aRCutL)/(aRCutR-aRCutL);
    tRnX = ONE - (tRnX+tRnX);
    chebyshevFull<N>(tRnX, rRn);
}


template <int N, int NP, int MPLUS>
static inline NNAP_DEVICE void calRnp_(flt_t *rRnp, flt_t *aRn, flt_t *aRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        const flt_t tDot = dot<N+1>(aRn, tWeight);
        if (MPLUS) {
            rRnp[np] += tDot;
        } else {
            rRnp[np] = tDot;
        }
        tWeight += (N+1);
    }
}
template <int N, int NP>
static inline NNAP_DEVICE void calRnp(flt_t *rRnp, flt_t *aRn, flt_t *aRFuseWeight) noexcept {
    calRnp_<N, NP, FALSE>(rRnp, aRn, aRFuseWeight);
}
template <int N, int NP>
static inline NNAP_DEVICE void mplusRnp(flt_t *rRnp, flt_t *aRn, flt_t *aRFuseWeight) noexcept {
    calRnp_<N, NP, TRUE>(rRnp, aRn, aRFuseWeight);
}
template <int N, int NP, int MPLUS>
static inline NNAP_DEVICE void calRnp_(flt_t *rRnp, flt_t aFc, flt_t *aRn, flt_t *aRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        const flt_t tDot = dot<N+1>(aRn, tWeight);
        if (MPLUS) {
            rRnp[np] += aFc*tDot;
        } else {
            rRnp[np] = aFc*tDot;
        }
        tWeight += (N+1);
    }
}
template <int N, int NP>
static inline NNAP_DEVICE void calRnp(flt_t *rRnp, flt_t aFc, flt_t *aRn, flt_t *aRFuseWeight) noexcept {
    calRnp_<N, NP, FALSE>(rRnp, aFc, aRn, aRFuseWeight);
}
template <int N, int NP>
static inline NNAP_DEVICE void mplusRnp(flt_t *rRnp, flt_t aFc, flt_t *aRn, flt_t *aRFuseWeight) noexcept {
    calRnp_<N, NP, TRUE>(rRnp, aFc, aRn, aRFuseWeight);
}

static inline NNAP_DEVICE flt_t calFcGrad(flt_t aX, flt_t aRCut, flt_t *rFcGrad) noexcept {
    const flt_t fcMul = ONE - pow2(aX);
    const flt_t fcMul3 = pow3(fcMul);
    *rFcGrad = ((flt_t)8.0) * fcMul3 / (aRCut*aRCut);
    return fcMul*fcMul3;
}
static inline NNAP_DEVICE flt_t calFcGrad(flt_t aDis, flt_t aRCut) noexcept {
    const flt_t fcMul3 = pow3(ONE - pow2(aDis/aRCut));
    return ((flt_t)8.0) * fcMul3 / (aRCut*aRCut);
}
static inline NNAP_DEVICE flt_t calFcPxyz(flt_t *rFcPx, flt_t *rFcPy, flt_t *rFcPz,
                                          flt_t aDis, flt_t aRCut, flt_t aDx, flt_t aDy, flt_t aDz) noexcept {
    const flt_t fcMul = ONE - pow2(aDis/aRCut);
    const flt_t fcMul3 = pow3(fcMul);
    const flt_t fcPMul = ((flt_t)8.0) * fcMul3 / (aRCut*aRCut);
    *rFcPx = aDx * fcPMul;
    *rFcPy = aDy * fcPMul;
    *rFcPz = aDz * fcPMul;
    return fcMul*fcMul3;
}
static inline NNAP_DEVICE flt_t calFcPxyz(flt_t *rFcPx, flt_t *rFcPy, flt_t *rFcPz,
                                          flt_t aDis, flt_t aRCutL, flt_t aRCutR, flt_t aDx, flt_t aDy, flt_t aDz) noexcept {
    const flt_t tRCutRL = aRCutR-aRCutL;
    const flt_t tX = (aDis-aRCutL)/tRCutRL;
    const flt_t fcMul = ONE - pow2(tX+tX - ONE);
    const flt_t fcMul3 = pow3(fcMul);
    const flt_t fcPMul = ((flt_t)16.0) * fcMul3 * (TWO - (aRCutL+aRCutR)/aDis) / (tRCutRL*tRCutRL);
    *rFcPx = aDx * fcPMul;
    *rFcPy = aDy * fcPMul;
    *rFcPz = aDz * fcPMul;
    return fcMul*fcMul3;
}

template <int N>
static inline NNAP_DEVICE void calRnFcGrad(flt_t *rRnGrad, flt_t *rCheby2, flt_t aDis, flt_t aRCut) noexcept {
    const flt_t tX = aDis/aRCut;
    flt_t fcGrad;
    flt_t tCheby2Mul = calFcGrad(tX, aRCut, &fcGrad);
    tCheby2Mul *= TWO / (aDis*aRCut);
    
    const flt_t tRnX = ONE - (tX+tX);
    chebyshevFull<N>(tRnX, fcGrad, rRnGrad);
    chebyshev2Full<N-1>(tRnX, tCheby2Mul, rCheby2);
    for (int n = 1; n <= N; ++n) {
        rRnGrad[n] += ((flt_t)n)*rCheby2[n-1];
    }
}
template <int N>
static inline NNAP_DEVICE void calRnGrad(flt_t *rRnGrad, flt_t *rCheby2, flt_t aDis, flt_t aRCut) noexcept {
    flt_t tRnX = aDis/aRCut;
    tRnX = ONE - (tRnX+tRnX);
    chebyshev2Full<N-1>(tRnX, rCheby2);
    const flt_t tRnPMul = TWO / (aDis*aRCut);
    rRnGrad[0] = ZERO;
    for (int n = 1; n <= N; ++n) {
        rRnGrad[n] = ((flt_t)n)*tRnPMul*rCheby2[n-1];
    }
}
template <int N>
static inline NNAP_DEVICE void calRnPxyz(flt_t *rRnPx, flt_t *rRnPy, flt_t *rRnPz, flt_t *rCheby2,
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
static inline NNAP_DEVICE void calRnPxyz(flt_t *rRnPx, flt_t *rRnPy, flt_t *rRnPz, flt_t *rCheby2,
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

template <int N, int NP>
static inline NNAP_DEVICE void calRnpGrad(flt_t *rRnpGrad, flt_t *aRnGrad, flt_t *aRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        rRnpGrad[np] = dot<N+1>(aRnGrad, tWeight);
        tWeight += (N+1);
    }
}
template <int N, int NP>
static inline NNAP_DEVICE void calRnpGrad(flt_t *rRnpGrad, flt_t aFc, flt_t *aRn, flt_t aFcGrad, flt_t *aRnGrad, flt_t *aRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        flt_t tRnWeight = 0.0, tRnGradWeight = 0.0;
        for (int n = 0; n <= N; ++n) {
            const flt_t tW = tWeight[n];
            tRnWeight += aRn[n] * tW;
            tRnGradWeight += aRnGrad[n] * tW;
        }
        tWeight += (N+1);
        rRnpGrad[np] = aFc*tRnGradWeight + aFcGrad*tRnWeight;
    }
}
template <int N, int NP>
static inline NNAP_DEVICE void calRnpPxyz(flt_t *rRnpPx, flt_t *rRnpPy, flt_t *rRnpPz, flt_t aFc, flt_t *aRn,
                                          flt_t aFcPx, flt_t aFcPy, flt_t aFcPz,
                                          flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz,
                                          flt_t *aRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        flt_t tRnWeight = 0.0, tRnPxWeight = 0.0, tRnPyWeight = 0.0, tRnPzWeight = 0.0;
        for (int n = 0; n <= N; ++n) {
            const flt_t tW = tWeight[n];
            tRnWeight += aRn[n] * tW;
            tRnPxWeight += aRnPx[n] * tW;
            tRnPyWeight += aRnPy[n] * tW;
            tRnPzWeight += aRnPz[n] * tW;
        }
        tWeight += (N+1);
        rRnpPx[np] = aFc*tRnPxWeight + aFcPx*tRnWeight;
        rRnpPy[np] = aFc*tRnPyWeight + aFcPy*tRnWeight;
        rRnpPz[np] = aFc*tRnPzWeight + aFcPz*tRnWeight;
    }
}

}

#endif //NNAP_UTIL_H
