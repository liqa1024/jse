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
static constexpr flt_t LN_EPSILON = 1.0e-5;


static inline NNAP_DEVICE NNAP_HOST double nnap_abs(double value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return abs(value);
#else
    return std::abs(value);
#endif
}
static inline NNAP_DEVICE NNAP_HOST float nnap_abs(float value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return abs(value);
#else
    return std::abs(value);
#endif
}

static inline NNAP_DEVICE NNAP_HOST double nnap_sqrt(double value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return sqrt(value);
#else
    return std::sqrt(value);
#endif
}
static inline NNAP_DEVICE NNAP_HOST float nnap_sqrt(float value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return sqrt(value);
#else
    return std::sqrt(value);
#endif
}

static inline NNAP_DEVICE NNAP_HOST double nnap_hypot(double x, double y) noexcept {
#ifdef NNAP_ARCH_CUDA
    return hypot(x, y);
#else
    return std::hypot(x, y);
#endif
}
static inline NNAP_DEVICE NNAP_HOST float nnap_hypot(float x, float y) noexcept {
#ifdef NNAP_ARCH_CUDA
    return hypot(x, y);
#else
    return std::hypot(x, y);
#endif
}

static inline NNAP_DEVICE NNAP_HOST double nnap_exp(double value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return exp(value);
#else
    return std::exp(value);
#endif
}
static inline NNAP_DEVICE NNAP_HOST float nnap_exp(float value) noexcept {
#ifdef NNAP_ARCH_CUDA
    return exp(value);
#else
    return std::exp(value);
#endif
}


static inline NNAP_DEVICE NNAP_HOST flt_t pow2(flt_t value) noexcept {
    return value * value;
}
static inline NNAP_DEVICE NNAP_HOST flt_t pow3(flt_t value) noexcept {
    return value * value * value;
}
static inline NNAP_DEVICE NNAP_HOST flt_t pow4(flt_t value) noexcept {
    const flt_t value2 = value * value;
    return value2 * value2;
}
static inline NNAP_DEVICE NNAP_HOST int numericEqual(double aLHS, double aRHS) noexcept {
    double tNorm = nnap_abs(aLHS) + nnap_abs(aRHS);
    if (tNorm < JSE_DBL_MIN_NORMAL * JSE_EPS_MUL) return TRUE;
    double tDiff = nnap_abs(aLHS - aRHS);
    return (tDiff <= tNorm * JSE_DBL_EPSILON) ? TRUE : FALSE;
}
static inline NNAP_DEVICE NNAP_HOST int numericEqual(float aLHS, float aRHS) noexcept {
    float tNorm = nnap_abs(aLHS) + nnap_abs(aRHS);
    if (tNorm < JSE_FLT_MIN_NORMAL * JSE_EPS_MUL) return TRUE;
    float tDiff = nnap_abs(aLHS - aRHS);
    return (tDiff <= tNorm * JSE_FLT_EPSILON) ? TRUE : FALSE;
}


template <int N>
static inline NNAP_DEVICE NNAP_HOST void fill(flt_t *rArray, flt_t aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] = aValue;
    }
}
template <int N>
static inline NNAP_DEVICE NNAP_HOST void fill(flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] = aArrayR[i];
    }
}

template <int N>
static inline NNAP_DEVICE NNAP_HOST void multiply(flt_t *rArray, flt_t aValue) noexcept {
    for (int i = 0; i < N; ++i) {
        rArray[i] *= aValue;
    }
}
template <int N>
static inline NNAP_DEVICE NNAP_HOST void multiply(flt_t *rArrayL, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] *= aArrayR[i];
    }
}

template <int N>
static inline NNAP_DEVICE NNAP_HOST flt_t dot(flt_t *aArray) noexcept {
    flt_t rDot = ZERO;
    for (int i = 0; i < N; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
template <int N>
static inline NNAP_DEVICE NNAP_HOST flt_t dot(flt_t *aArrayL, flt_t *aArrayR) noexcept {
    flt_t rDot = ZERO;
    for (int i = 0; i < N; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}

template <int N>
static inline NNAP_DEVICE NNAP_HOST void mplus(flt_t *rArrayL, flt_t aMul, flt_t *aArrayR) noexcept {
    for (int i = 0; i < N; ++i) {
        rArrayL[i] += aMul*aArrayR[i];
    }
}

#ifdef NNAP_USE_TABLE
static inline NNAP_DEVICE NNAP_HOST void tableInit(flt_t aX, flt_t &ix, int &il, int &ir) noexcept {
    ix = aX * (flt_t)NNAP_TABLE_SIZE;
    il = (int)ix;
    ir = il + 1;
}
static inline NNAP_DEVICE NNAP_HOST flt_t calRFuncFromTable(flt_t ix, int il, int ir, flt_t *aRFuncTable) noexcept {
    if (il < 0) return aRFuncTable[0];
    if (ir > NNAP_TABLE_SIZE) return aRFuncTable[NNAP_TABLE_SIZE];
    const flt_t fl = aRFuncTable[il];
    const flt_t fr = aRFuncTable[ir];
    return fl + (ix-(flt_t)il) * (fr-fl);
}
#endif

template <int N>
static inline NNAP_DEVICE NNAP_HOST void chebyshevFull(flt_t aX, flt_t aMul, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = aMul;
    if (N == 0) return;
    rDest[1] = aMul*aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}
template <int N>
static inline NNAP_DEVICE NNAP_HOST void chebyshev2Full(flt_t aX, flt_t aMul, flt_t *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = aMul;
    if (N == 0) return;
    rDest[1] = TWO*aMul*aX;
    for (int n = 2; n <= N; ++n) {
        rDest[n] = TWO*aX*rDest[n-1] - rDest[n-2];
    }
}

static inline NNAP_DEVICE NNAP_HOST flt_t calFc(flt_t aDis, flt_t aRCut) noexcept {
    return pow4(ONE - pow2(aDis/aRCut));
}
static inline NNAP_DEVICE NNAP_HOST flt_t calFcGrad(flt_t aDis, flt_t aRCut) noexcept {
    const flt_t fcMul3 = pow3(ONE - pow2(aDis/aRCut));
    return ((flt_t)8.0) * fcMul3 / (aRCut*aRCut);
}

template <int N>
static inline NNAP_DEVICE NNAP_HOST void calRn(flt_t *rRn, flt_t aDis, flt_t aRCut) noexcept {
    const flt_t tX = aDis/aRCut;
    const flt_t tRnX = ONE - (tX+tX);
    chebyshevFull<N>(tRnX, ONE, rRn);
}
template <int N>
static inline NNAP_DEVICE NNAP_HOST void calRnGrad(flt_t *rRnGrad, flt_t aDis, flt_t aRCut) noexcept {
    const flt_t tX = aDis/aRCut;
    const flt_t tCheby2Mul = TWO / (aDis*aRCut);
    const flt_t tRnX = ONE - (tX+tX);
    chebyshev2Full<N-1>(tRnX, tCheby2Mul, rRnGrad+1);
    rRnGrad[0] = ZERO;
    for (int n = 1; n <= N; ++n) {
        rRnGrad[n] *= (flt_t)n;
    }
}

template <int N, int NP>
static inline NNAP_DEVICE NNAP_HOST void calRnp(flt_t *rRnp, flt_t *aRn, flt_t *aRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        rRnp[np] = dot<N+1>(aRn, tWeight);
        tWeight += (N+1);
    }
}
template <int N, int NP, int GRAD_W, int GRAD_R>
static inline NNAP_DEVICE NNAP_HOST void backwardRnp(flt_t *aAGradRnp, flt_t *aRn, flt_t *rAGradRn, flt_t *aRFuseWeight, flt_t *rAGradRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    flt_t *rAGradWeight = rAGradRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        const flt_t subAGradRnp = aAGradRnp[np];
        if (GRAD_W) {
            mplus<N+1>(rAGradWeight, subAGradRnp, aRn);
            rAGradWeight += (N+1);
        }
        if (GRAD_R) {
            mplus<N+1>(rAGradRn, subAGradRnp, tWeight);
            tWeight += (N+1);
        }
    }
}
template <int N, int NP>
static inline NNAP_DEVICE NNAP_HOST void backwardBackwardRnp(flt_t *aAGradRnp, flt_t *rBGradAGradRnp, flt_t *aBGradAGradRn, flt_t *aRFuseWeight, flt_t *rBGradRFuseWeight) noexcept {
    flt_t *tWeight = aRFuseWeight;
    flt_t *rBGradWeight = rBGradRFuseWeight;
    for (int np = 0; np < NP; ++np) {
        const flt_t subAGradRnp = aAGradRnp[np];
        flt_t subBGradAGradRnp = ZERO;
        for (int n = 0; n <= N; ++n) {
            const flt_t subBGradAGradRn = aBGradAGradRn[n];
            subBGradAGradRnp += subBGradAGradRn * tWeight[n];
            rBGradWeight[n] += subAGradRnp * subBGradAGradRn;
        }
        rBGradAGradRnp[np] += subBGradAGradRnp;
        tWeight += (N+1);
        rBGradWeight += (N+1);
    }
}

template <int N>
static inline NNAP_DEVICE NNAP_HOST void layerNorm(flt_t *aX, flt_t *rY, flt_t &rMu, flt_t &rSigma, flt_t *aLNBeta, flt_t *aLNGamma) noexcept {
    // cal mu sigma
    flt_t tSumX = ZERO, tSumX2 = ZERO;
    for (int i = 0; i < N; ++i) {
        const flt_t tX = aX[i];
        tSumX += tX;
        tSumX2 += tX*tX;
    }
    const flt_t tMu = tSumX/N;
    const flt_t tSigma = sqrt(tSumX2/N - tMu*tMu + LN_EPSILON);
    // norm to rX
    for (int i = 0; i < N; ++i) {
        rY[i] = aLNGamma[i]*(aX[i]-tMu)/tSigma + aLNBeta[i];
    }
    rMu = tMu;
    rSigma = tSigma;
}
template <int N, int GRAD_W>
static inline NNAP_DEVICE NNAP_HOST void backwardLayerNorm(flt_t *aX, flt_t *rAGradX, flt_t *aAGradY, flt_t aMu, flt_t aSigma, flt_t &rAGradSigma, flt_t *aLNGamma, flt_t *rAGradLNBeta, flt_t *rAGradLNGamma) noexcept {
    // i do not want to cache everything...
    flt_t tAGradMu = ZERO, tAGradSigma = ZERO;
    for (int i = 0; i < N; ++i) {
        const flt_t subX = aX[i];
        const flt_t subAGradY = aAGradY[i];
        if (GRAD_W) {
            rAGradLNBeta[i] += subAGradY;
            rAGradLNGamma[i] += subAGradY*(subX-aMu)/aSigma;
        }
        const flt_t subAGradNormX = subAGradY*aLNGamma[i];
        tAGradSigma -= subAGradNormX*(subX-aMu)/pow2(aSigma);
        tAGradMu -= subAGradNormX/aSigma;
        rAGradX[i] += subAGradNormX/aSigma;
    }
    const flt_t tAGradSigma2 = ((flt_t)0.5)*tAGradSigma/aSigma;
    tAGradMu += (-TWO)*tAGradSigma2*aMu;
    const flt_t tAGradSumX2 = tAGradSigma2/N;
    const flt_t tAGradSumX = tAGradMu/N;
    for (int i = 0; i < N; ++i) {
        rAGradX[i] += tAGradSumX + TWO*tAGradSumX2*aX[i];
    }
    rAGradSigma = tAGradSigma;
}
template <int N>
static inline NNAP_DEVICE NNAP_HOST void backwardBackwardLayerNorm(flt_t *aX, flt_t *rBGradX, flt_t *aBGradAGradX, flt_t *aAGradY, flt_t *rBGradAGradY, flt_t aMu, flt_t aSigma, flt_t aAGradSigma, flt_t *aLNGamma, flt_t *rBGradLNGamma) noexcept {
    // i do not want to cache everything...
    const flt_t tAGradSigma2 = ((flt_t)0.5)*aAGradSigma/aSigma;
    const flt_t tAGradSumX2 = tAGradSigma2/N;
    flt_t tBGradMu = ZERO, tBGradSigma= ZERO;
    flt_t tBGradAGradSumX = ZERO, tBGradAGradSumX2 = ZERO;
    for (int i = 0; i < N; ++i) {
        const flt_t subBGradAGradX = aBGradAGradX[i];
        tBGradAGradSumX += subBGradAGradX;
        tBGradAGradSumX2 += TWO*subBGradAGradX*aX[i];
        rBGradX[i] += TWO*subBGradAGradX*tAGradSumX2;
    }
    const flt_t tBGradAGradMu = tBGradAGradSumX/N;
    const flt_t tBGradAGradSigma2 = tBGradAGradSumX2/N + (-TWO)*tBGradAGradMu*aMu;
    const flt_t tBGradAGradSigma = ((flt_t)0.5)*tBGradAGradSigma2/aSigma;
    
    tBGradMu += (-TWO)*tBGradAGradMu*tAGradSigma2;
    tBGradSigma += (-(flt_t)0.5)*tBGradAGradSigma2*aAGradSigma/pow2(aSigma);
    
    for (int i = 0; i < N; ++i) {
        const flt_t subGamma = aLNGamma[i];
        const flt_t subX = aX[i];
        const flt_t subBGradAGradX = aBGradAGradX[i];
        const flt_t subAGradY = aAGradY[i];
        const flt_t subBGradAGradNormX = (subBGradAGradX - tBGradAGradMu)/aSigma - tBGradAGradSigma*(subX-aMu)/pow2(aSigma);
        rBGradAGradY[i] += subBGradAGradNormX*subGamma;
        rBGradLNGamma[i] += subBGradAGradNormX*subAGradY;
        
        const flt_t subAGradNormX = subAGradY*subGamma;
        tBGradSigma -= (subBGradAGradX - tBGradAGradMu)*subAGradNormX/pow2(aSigma) - TWO*tBGradAGradSigma*subAGradNormX*(subX-aMu)/pow3(aSigma);
        tBGradMu += tBGradAGradSigma*subAGradNormX/pow2(aSigma);
        rBGradX[i] -= tBGradAGradSigma*subAGradNormX/pow2(aSigma);
    }
    // grad sigma & mu -> grad x
    const flt_t tBGradSigma2 = ((flt_t)0.5)*tBGradSigma/aSigma;
    tBGradMu += (-TWO)*tBGradSigma2*aMu;
    const flt_t tBGradSumX2 = tBGradSigma2/N;
    const flt_t tBGradSumX = tBGradMu/N;
    for (int i = 0; i < N; ++i) {
        rBGradX[i] += tBGradSumX + TWO*tBGradSumX2*aX[i];
    }
}

}

#endif //NNAP_UTIL_H
