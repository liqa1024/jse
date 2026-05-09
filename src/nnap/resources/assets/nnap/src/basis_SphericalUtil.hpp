#ifndef BASIS_SPHERICAL_UTIL_H
#define BASIS_SPHERICAL_UTIL_H

#include "basis_SphericalUtil0.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENS_X__ 0
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

template <int L>
static NNAP_DEVICE void realNormalizedLegendreInterLoopSub_(flt_t aX, flt_t *rDest) noexcept {
    flt_t tPlm;
// >>> NNAPGEN REPEAT
    if (L-1==__NNAPGENS_X__) return;
    constexpr flt_t tSHAlm__NNAPGENS_X__ = SH_Alm[L*(L+1)/2 + __NNAPGENS_X__];
    constexpr flt_t tSHBlm__NNAPGENS_X__ = SH_Blm[L*(L+1)/2 + __NNAPGENS_X__];
    tPlm = tSHAlm__NNAPGENS_X__ * (
        aX*rDest[(L-1)*(L-1)+(L-1) + __NNAPGENS_X__] +
        tSHBlm__NNAPGENS_X__*rDest[(L-2)*(L-2)+(L-2) + __NNAPGENS_X__]
    );
    if (__NNAPGENS_X__ == 0) {
        rDest[L*L+L] = tPlm;
    } else {
        rDest[L*L+L + __NNAPGENS_X__] = tPlm;
        rDest[L*L+L - __NNAPGENS_X__] = tPlm;
    }
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static NNAP_DEVICE void realNormalizedLegendreFull(flt_t aX, flt_t aY, flt_t *rDest) noexcept {
    flt_t tPll = 0.28209479177387814347403972578039; // = sqrt(1/(4*PI))
    rDest[0] = tPll;
    if (LMAX == 0) return;
    flt_t tPlm = SQRT3 * aX * tPll;
    rDest[2] = tPlm;
    tPll *= (-SQRT3DIV2 * aY);
    rDest[2+1] = tPll;
    rDest[2-1] = tPll;
    if (LMAX == 1) return;
// >>> NNAPGEN REPEAT
    realNormalizedLegendreInterLoopSub_<__NNAPGENS_X__>(aX, rDest);
    constexpr flt_t tMul1__NNAPGENS_X__ =  SQRT_2LM1P3[__NNAPGENS_X__];
    constexpr flt_t tMul2__NNAPGENS_X__ = -SQRT_1P1D2L[__NNAPGENS_X__];
    tPlm = tMul1__NNAPGENS_X__ * aX * tPll;
    rDest[__NNAPGENS_X__*__NNAPGENS_X__+__NNAPGENS_X__ + (__NNAPGENS_X__-1)] = tPlm;
    rDest[__NNAPGENS_X__*__NNAPGENS_X__+__NNAPGENS_X__ - (__NNAPGENS_X__-1)] = tPlm;
    tPll *= tMul2__NNAPGENS_X__ * aY;
    rDest[__NNAPGENS_X__*__NNAPGENS_X__+__NNAPGENS_X__ + __NNAPGENS_X__] = tPll;
    rDest[__NNAPGENS_X__*__NNAPGENS_X__+__NNAPGENS_X__ - __NNAPGENS_X__] = tPll;
    if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 2..12
}

template <int M, int LMAX>
static NNAP_DEVICE void realSphericalHarmonicsFull4InterLoopSub_(flt_t aSqrt2CosMPhi, flt_t aSqrt2SinMPhi, flt_t *rDest) noexcept {
// >>> NNAPGEN REPEAT
    rDest[(M+__NNAPGENS_X__)*(M+__NNAPGENS_X__) + (M+__NNAPGENS_X__) + M] *= aSqrt2CosMPhi;
    rDest[(M+__NNAPGENS_X__)*(M+__NNAPGENS_X__) + (M+__NNAPGENS_X__) - M] *= aSqrt2SinMPhi;
    if (LMAX==M+__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static NNAP_DEVICE void realSphericalHarmonicsFull4_(flt_t aCosTheta, flt_t aSinTheta, flt_t aCosPhi, flt_t aSinPhi, flt_t *rDest) noexcept {
    // cal real Legendre
    realNormalizedLegendreFull<LMAX>(aCosTheta, aSinTheta, rDest);
    if (LMAX == 0) return;
    // cal sinMPhi & conMPhi
    flt_t rSinMmmPhi = ZERO;
    flt_t rCosMmmPhi = ONE;
    flt_t rSinMPhi = aSinPhi;
    flt_t rCosMPhi = aCosPhi;
    const flt_t tCosPhi2 = rCosMPhi+rCosMPhi;
    flt_t tSinMppPhi, tCosMppPhi;
// >>> NNAPGEN REPEAT
    realSphericalHarmonicsFull4InterLoopSub_<__NNAPGENS_X__, LMAX>(SQRT2*rCosMPhi, SQRT2*rSinMPhi, rDest);
    if (LMAX==__NNAPGENS_X__) return;
    tSinMppPhi = tCosPhi2*rSinMPhi - rSinMmmPhi;
    tCosMppPhi = tCosPhi2*rCosMPhi - rCosMmmPhi;
    rSinMmmPhi = rSinMPhi; rCosMmmPhi = rCosMPhi;
    rSinMPhi = tSinMppPhi; rCosMPhi = tCosMppPhi;
// <<< NNAPGEN REPEAT 1..12
}
template <int LMAX>
static NNAP_DEVICE void calY(flt_t *rY, flt_t aDx, flt_t aDy, flt_t aDz, flt_t aDis) noexcept {
    const flt_t dxy = nnap_hypot(aDx, aDy);
    const flt_t cosTheta = aDz / aDis;
    const flt_t sinTheta = dxy / aDis;
    flt_t cosPhi;
    flt_t sinPhi;
    // avoid nan
    if (numericEqual(dxy, ZERO)) {
        cosPhi = ONE;
        sinPhi = ZERO;
    } else {
        cosPhi = aDx / dxy;
        sinPhi = aDy / dxy;
    }
    realSphericalHarmonicsFull4_<LMAX>(cosTheta, sinTheta, cosPhi, sinPhi, rY);
}

template <int L>
static NNAP_DEVICE void calYPphi_(flt_t *rYPphi, flt_t *aY) noexcept {
    constexpr int tStart = L*L;
    constexpr int tIdx = tStart+L;
    for (int m = -L; m <= L; ++m) {
        rYPphi[tIdx+m] = -((flt_t)m) * aY[tIdx-m];
    }
}
template <int L>
static NNAP_DEVICE void calYPtheta_(flt_t *rYPtheta, flt_t *aY, flt_t aCosPhi, flt_t aSinPhi) noexcept {
    switch(L) {
    case 0: {
        rYPtheta[0] = ZERO;
        return;
    }
    case 1: {
        constexpr flt_t tMul = SQRT_LPM_LMM1[2]*SQRT2_INV;
        rYPtheta[1] = -tMul * aSinPhi*aY[2];
        rYPtheta[2] =  tMul * (aCosPhi*aY[3] + aSinPhi*aY[1]);
        rYPtheta[3] = -tMul * aCosPhi*aY[2];
        return;
    }
    default: {
        constexpr int tStart = L*L;
        constexpr int tIdx = tStart+L;
        constexpr flt_t tMul = SQRT_LPM_LMM1[tIdx]*SQRT2_INV;
        rYPtheta[tIdx] = tMul * (aCosPhi*aY[tIdx+1] + aSinPhi*aY[tIdx-1]);
        rYPtheta[tIdx+1] = -tMul * aCosPhi*aY[tIdx];
        rYPtheta[tIdx-1] = -tMul * aSinPhi*aY[tIdx];
// >>> NNAPGEN REPEAT
        if (__NNAPGENS_X__>1) {
            constexpr flt_t tMul__NNAPGENS_X__ = -((flt_t)0.5)*SQRT_LPM_LMM1[tIdx+__NNAPGENS_X__];
            rYPtheta[tIdx+__NNAPGENS_X__] = tMul__NNAPGENS_X__ * (aCosPhi*aY[tIdx+__NNAPGENS_X__-1] - aSinPhi*aY[tIdx-__NNAPGENS_X__+1]);
            rYPtheta[tIdx-__NNAPGENS_X__] = tMul__NNAPGENS_X__ * (aCosPhi*aY[tIdx-__NNAPGENS_X__+1] + aSinPhi*aY[tIdx+__NNAPGENS_X__-1]);
        }
        if (__NNAPGENS_X__<L) {
            constexpr flt_t tMul__NNAPGENS_X__ = ((flt_t)0.5)*SQRT_LPM1_LMM[tIdx+__NNAPGENS_X__];
            rYPtheta[tIdx+__NNAPGENS_X__] += tMul__NNAPGENS_X__ * (aCosPhi*aY[tIdx+__NNAPGENS_X__+1] + aSinPhi*aY[tIdx-__NNAPGENS_X__-1]);
            rYPtheta[tIdx-__NNAPGENS_X__] += tMul__NNAPGENS_X__ * (aCosPhi*aY[tIdx-__NNAPGENS_X__-1] - aSinPhi*aY[tIdx+__NNAPGENS_X__+1]);
        }
        if (L==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
        return;
    }}
}
template <int LMAX>
static NNAP_DEVICE void calYPthetaPphi_(flt_t *rYPtheta, flt_t *rYPphi, flt_t *aY, flt_t aCosPhi, flt_t aSinPhi) noexcept {
// >>> NNAPGEN REPEAT
    calYPphi_<__NNAPGENS_X__>(rYPphi, aY);
    calYPtheta_<__NNAPGENS_X__>(rYPtheta, aY, aCosPhi, aSinPhi);
    if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static NNAP_DEVICE void calYPthetaPphi(flt_t *rYPtheta, flt_t *rYPphi, flt_t *aY, flt_t aDx, flt_t aDy, flt_t aDz, flt_t aDis,
                                       flt_t &rThetaPx, flt_t &rThetaPy, flt_t &rThetaPz, flt_t &rPhiPx, flt_t &rPhiPy) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    const flt_t dxy = nnap_hypot(aDx, aDy);
    const flt_t cosTheta = aDz / aDis;
    const flt_t sinTheta = dxy / aDis;
    const int dxyCloseZero = numericEqual(dxy, ZERO);
    flt_t cosPhi, sinPhi;
    if (dxyCloseZero) {
        cosPhi = ONE;
        sinPhi = ZERO;
    } else {
        cosPhi = aDx / dxy;
        sinPhi = aDy / dxy;
    }
    calYPthetaPphi_<LMAX>(rYPtheta, rYPphi, aY, cosPhi, sinPhi);
    if (dxyCloseZero) {
        // fix singularity
        fill<tLMAll>(rYPphi, ZERO);
    }
    rThetaPx = -cosTheta*cosPhi/aDis;
    rThetaPy = -cosTheta*sinPhi/aDis;
    rThetaPz =  sinTheta/aDis;
    rPhiPx = dxyCloseZero ? ZERO : ( sinPhi/dxy);
    rPhiPy = dxyCloseZero ? ZERO : (-cosPhi/dxy);
}
static inline NNAP_DEVICE void calthetaPhiPxyz(flt_t aDx, flt_t aDy, flt_t aDz, flt_t aDis,
                                               flt_t &rThetaPx, flt_t &rThetaPy, flt_t &rThetaPz,
                                               flt_t &rPhiPx, flt_t &rPhiPy) noexcept {
    const flt_t dxy = nnap_hypot(aDx, aDy);
    const flt_t cosTheta = aDz / aDis;
    const flt_t sinTheta = dxy / aDis;
    const int dxyCloseZero = numericEqual(dxy, ZERO);
    flt_t cosPhi, sinPhi;
    if (dxyCloseZero) {
        cosPhi = ONE;
        sinPhi = ZERO;
    } else {
        cosPhi = aDx / dxy;
        sinPhi = aDy / dxy;
    }
    rThetaPx = -cosTheta*cosPhi/aDis;
    rThetaPy = -cosTheta*sinPhi/aDis;
    rThetaPz =  sinTheta/aDis;
    rPhiPx = dxyCloseZero ? ZERO : ( sinPhi/dxy);
    rPhiPy = dxyCloseZero ? ZERO : (-cosPhi/dxy);
}


template <int SIZE_NP, int LMAX>
static NNAP_DEVICE void mplusAnlm(flt_t *rAnlm, flt_t *aY, flt_t aFc, flt_t *aRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tAnlm = rAnlm;
    for (int np = 0; np < SIZE_NP; ++np) {
        mplus<tLMAll>(tAnlm, aFc*aRnp[np], aY);
        tAnlm += tLMAll;
    }
}
template <int SIZE_NP, int LMAX>
static NNAP_DEVICE void mplusAnlmWt(flt_t *rAnlm, flt_t *rAnlmWt, flt_t aWt, flt_t *aY, flt_t aFc, flt_t *aRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tAnlm = rAnlm;
    flt_t *tAnlmWt = rAnlmWt;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subRnpFc = aFc*aRnp[np];
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t tRHS = subRnpFc*aY[k];
            tAnlm[k] += tRHS;
            tAnlmWt[k] += aWt*tRHS;
        }
        tAnlm += tLMAll;
        tAnlmWt += tLMAll;
    }
}

template <int SIZE_NP, int LMAX>
static NNAP_DEVICE void backwardMplusAnlm(flt_t *aAGradAnlm, flt_t *aY, flt_t *rAGradY, flt_t aFc, flt_t &rAGradFc, flt_t *aRnp, flt_t *rAGradRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t tAGradFc = ZERO;
    flt_t *tAGradAnlm = aAGradAnlm;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subRnp = aRnp[np];
        const flt_t subFcRnp = aFc*subRnp;
        flt_t tAGradFcRnp = ZERO;
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t subAGradAnlm = tAGradAnlm[k];
            rAGradY[k] += subAGradAnlm*subFcRnp;
            tAGradFcRnp += subAGradAnlm*aY[k];
        }
        tAGradFc += tAGradFcRnp*subRnp;
        rAGradRnp[np] += tAGradFcRnp*aFc;
        tAGradAnlm += tLMAll;
    }
    rAGradFc += tAGradFc;
}
template <int SIZE_NP, int LMAX>
static NNAP_DEVICE void backwardMplusAnlmWt(flt_t *aAGradAnlm, flt_t *aAGradAnlmWt, flt_t aWt, flt_t *aY, flt_t *rAGradY, flt_t aFc, flt_t &rAGradFc, flt_t *aRnp, flt_t *rAGradRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t tAGradFc = ZERO;
    flt_t *tAGradAnlm = aAGradAnlm;
    flt_t *tAGradAnlmWt = aAGradAnlmWt;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subRnp = aRnp[np];
        const flt_t subFcRnp = aFc*subRnp;
        flt_t tAGradFcRnp = ZERO;
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t subAGradAnlm = tAGradAnlm[k] + aWt*tAGradAnlmWt[k];
            rAGradY[k] += subAGradAnlm*subFcRnp;
            tAGradFcRnp += subAGradAnlm*aY[k];
        }
        tAGradFc += tAGradFcRnp*subRnp;
        rAGradRnp[np] += tAGradFcRnp*aFc;
        tAGradAnlm += tLMAll;
        tAGradAnlmWt += tLMAll;
    }
    rAGradFc += tAGradFc;
}

template <int SIZE_NP, int LMAX, int GRAD_RNP>
static NNAP_DEVICE void backwardBackwardMplusAnlm(flt_t *aAGradAnlm, flt_t *rBGradAGradAnlm, flt_t *aY, flt_t *aBGradAGradY, flt_t aFc, flt_t aBGradAGradFc, flt_t *aRnp, flt_t *rBGradRnp, flt_t *aBGradAGradRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tAGradAnlm = aAGradAnlm;
    flt_t *tBGradAGradAnlm = rBGradAGradAnlm;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subRnp = aRnp[np];
        const flt_t subFcRnp = aFc*subRnp;
        const flt_t tBGradAGradFcRnp = aBGradAGradRnp[np]*aFc + aBGradAGradFc*subRnp;
        flt_t subBGradFcRnp = ZERO;
        flt_t tAGradFcRnp = ZERO; // i do not want to cache everything...
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t subY = aY[k];
            const flt_t subBGradAGradY = aBGradAGradY[k];
            tBGradAGradAnlm[k] += subBGradAGradY*subFcRnp + tBGradAGradFcRnp*subY;
            if (GRAD_RNP) {
                const flt_t subGradAnlm = tAGradAnlm[k];
                subBGradFcRnp += subBGradAGradY*subGradAnlm;
                tAGradFcRnp += subGradAnlm*subY;
            }
        }
        if (GRAD_RNP) {
            rBGradRnp[np] += aBGradAGradFc*tAGradFcRnp + subBGradFcRnp*aFc;
            tAGradAnlm += tLMAll;
        }
        tBGradAGradAnlm += tLMAll;
    }
}
template <int SIZE_NP, int LMAX>
static NNAP_DEVICE void backwardBackwardMplusAnlm(flt_t *rBGradAGradAnlm, flt_t *aY, flt_t *aBGradAGradY, flt_t aFc, flt_t aBGradAGradFc, flt_t *aRnp, flt_t *aBGradAGradRnp) noexcept {
    backwardBackwardMplusAnlm<SIZE_NP, LMAX, FALSE>(NULL, rBGradAGradAnlm, aY, aBGradAGradY, aFc, aBGradAGradFc, aRnp, NULL, aBGradAGradRnp);
}
template <int SIZE_NP, int LMAX>
static NNAP_DEVICE void backwardBackwardMplusAnlmWt(flt_t *rBGradAGradAnlm, flt_t *rBGradAGradAnlmWt, flt_t aWt, flt_t *aY, flt_t *aBGradAGradY, flt_t aFc, flt_t aBGradAGradFc, flt_t *aRnp, flt_t *aBGradAGradRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tBGradAGradAnlm = rBGradAGradAnlm;
    flt_t *tBGradAGradAnlmWt = rBGradAGradAnlmWt;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t subRnp = aRnp[np];
        const flt_t subFcRnp = aFc*subRnp;
        const flt_t tBGradAGradFcRnp = aBGradAGradRnp[np]*aFc + aBGradAGradFc*subRnp;
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t tRHS = aBGradAGradY[k]*subFcRnp + tBGradAGradFcRnp*aY[k];
            tBGradAGradAnlm[k] += tRHS;
            tBGradAGradAnlmWt[k] += aWt*tRHS;
        }
        tBGradAGradAnlm += tLMAll;
        tBGradAGradAnlmWt += tLMAll;
    }
}


template <int L>
static inline NNAP_DEVICE void calL2Sub_(flt_t *aAnlm, flt_t *rFp) noexcept {
    constexpr int tLen = L+L+1;
    const flt_t rDot = dot<tLen>(aAnlm + (L*L));
    rFp[L] = (PI4/(flt_t)tLen) * rDot;
}
template <int LMAX>
static NNAP_DEVICE void calSphL2(flt_t *aAnlm, flt_t *rFp) noexcept {
    // l == 0
    const flt_t tAn00 = aAnlm[0];
    rFp[0] = PI4 * tAn00*tAn00;
    if (LMAX == 0) return;
// >>> NNAPGEN REPEAT
    calL2Sub_<__NNAPGENS_X__>(aAnlm, rFp);
    if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int L3IDX, int SUBIDX>
static inline NNAP_DEVICE flt_t calL3SubSub_(flt_t *aAnlm) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr flt_t coeff = L3_COEFF[L3IDX][SUBIDX];
    return coeff * aAnlm[i1]*aAnlm[i2]*aAnlm[i3];
}
template <int L3IDX>
static NNAP_DEVICE flt_t calL3Sub_(flt_t *aAnlm) noexcept {
    flt_t rFp3 = ZERO;
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return rFp3;
    rFp3 += calL3SubSub_<L3IDX, __NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 0..<110
    return rFp3;
}
template <int L3MAX>
static NNAP_DEVICE void calSphL3(flt_t *aAnlm, flt_t *rFp) noexcept {
    if (L3MAX<=1) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX==2) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX==3) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX==4) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX==5) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline NNAP_DEVICE flt_t calL4SubSub_(flt_t *aAnlm) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr flt_t coeff = L4_COEFF[L4IDX][SUBIDX];
    return coeff * aAnlm[i1]*aAnlm[i2]*aAnlm[i3]*aAnlm[i4];
}
template <int L4IDX>
static NNAP_DEVICE flt_t calL4Sub_(flt_t *aAnlm) noexcept {
    flt_t rFp4 = ZERO;
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return rFp4;
    rFp4 += calL4SubSub_<L4IDX, __NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 0..<100
    return rFp4;
}
template <int L4MAX>
static NNAP_DEVICE void calSphL4(flt_t *aAnlm, flt_t *rFp) noexcept {
    if (L4MAX<1) return;
    rFp[0] = calL4Sub_<0>(aAnlm);
    if (L4MAX==1) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL4Sub_<__NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX==2) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL4Sub_<__NNAPGENS_X__>(aAnlm);
// <<< NNAPGEN REPEAT 3..8
}

template <int L>
static inline NNAP_DEVICE void calGradL2Sub_(flt_t *aAnlm, flt_t *rGradAnlm, flt_t aSubGradFp) noexcept {
    constexpr int tLen = L+L+1;
    const flt_t tMul = (TWO*PI4/(flt_t)tLen) * aSubGradFp;
    mplus<tLen>(rGradAnlm + (L*L), tMul, aAnlm + (L*L));
}
template <int LMAX>
static NNAP_DEVICE void calGradSphL2(flt_t *aAnlm, flt_t *rGradAnlm, flt_t *aGradFp) noexcept {
    // l = 0
    rGradAnlm[0] += (TWO*PI4) * aAnlm[0] * aGradFp[0];
    if (LMAX == 0) return;
// >>> NNAPGEN REPEAT
    calGradL2Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
    if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int L3IDX, int SUBIDX>
static inline NNAP_DEVICE void calGradL3SubSub_(flt_t *aAnlm, flt_t *rGradAnlm, flt_t aSubGradFp) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr flt_t coeff = L3_COEFF[L3IDX][SUBIDX];
    const flt_t tMul = coeff * aSubGradFp;
    const flt_t tAnlm1 = aAnlm[i1];
    const flt_t tAnlm2 = aAnlm[i2];
    const flt_t tAnlm3 = aAnlm[i3];
    rGradAnlm[i1] += tMul * tAnlm2*tAnlm3;
    rGradAnlm[i2] += tMul * tAnlm1*tAnlm3;
    rGradAnlm[i3] += tMul * tAnlm1*tAnlm2;
}
template <int L3IDX>
static NNAP_DEVICE void calGradL3Sub_(flt_t *aAnlm, flt_t *rGradAnlm, flt_t aSubGradFp) noexcept {
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return;
    calGradL3SubSub_<L3IDX, __NNAPGENS_X__>(aAnlm, rGradAnlm, aSubGradFp);
// <<< NNAPGEN REPEAT 0..<110
}
template <int L3MAX>
static NNAP_DEVICE void calGradSphL3(flt_t *aAnlm, flt_t *rGradAnlm, flt_t *aGradFp) noexcept {
    if (L3MAX <= 1) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX == 2) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX == 3) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX == 4) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX == 5) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline NNAP_DEVICE void calGradL4SubSub_(flt_t *aAnlm, flt_t *rGradAnlm, flt_t aSubGradFp) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr flt_t coeff = L4_COEFF[L4IDX][SUBIDX];
    const flt_t tMul = coeff * aSubGradFp;
    const flt_t tAnlm1 = aAnlm[i1];
    const flt_t tAnlm2 = aAnlm[i2];
    const flt_t tAnlm3 = aAnlm[i3];
    const flt_t tAnlm4 = aAnlm[i4];
    rGradAnlm[i1] += tMul * tAnlm2*tAnlm3*tAnlm4;
    rGradAnlm[i2] += tMul * tAnlm1*tAnlm3*tAnlm4;
    rGradAnlm[i3] += tMul * tAnlm1*tAnlm2*tAnlm4;
    rGradAnlm[i4] += tMul * tAnlm1*tAnlm2*tAnlm3;
}
template <int L4IDX>
static NNAP_DEVICE void calGradL4Sub_(flt_t *aAnlm, flt_t *rGradAnlm, flt_t aSubGradFp) noexcept {
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return;
    calGradL4SubSub_<L4IDX, __NNAPGENS_X__>(aAnlm, rGradAnlm, aSubGradFp);
// <<< NNAPGEN REPEAT 0..<100
}
template <int L4MAX>
static NNAP_DEVICE void calGradSphL4(flt_t *aAnlm, flt_t *rGradAnlm, flt_t *aGradFp) noexcept {
    if (L4MAX < 1) return;
    calGradL4Sub_<0>(aAnlm, rGradAnlm, aGradFp[0]);
    if (L4MAX == 1) return;
// >>> NNAPGEN REPEAT
    calGradL4Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX == 2) return;
// >>> NNAPGEN REPEAT
    calGradL4Sub_<__NNAPGENS_X__>(aAnlm, rGradAnlm, aGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 3..8
}

template <int L>
static inline NNAP_DEVICE void calGradGradL2Sub_(flt_t *aAnlm, flt_t *aGradGradAnlm, flt_t *rGradGradFp) noexcept {
    constexpr int tLen = L+L+1;
    const flt_t rDot = dot<tLen>(aGradGradAnlm + (L*L), aAnlm + (L*L));
    rGradGradFp[L] += (TWO*PI4/(flt_t)tLen) * rDot;
}
template <int LMAX>
static NNAP_DEVICE void calGradGradSphL2(flt_t *aAnlm, flt_t *aGradGradAnlm, flt_t *rGradGradFp) noexcept {
    // l = 0
    rGradGradFp[0] += (TWO*PI4) * aGradGradAnlm[0] * aAnlm[0];
    if (LMAX == 0) return;
// >>> NNAPGEN REPEAT
    calGradGradL2Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm, rGradGradFp);
    if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int L3IDX, int SUBIDX>
static inline NNAP_DEVICE flt_t calGradGradL3SubSub_(flt_t *aAnlm, flt_t *aGradGradAnlm) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr flt_t coeff = L3_COEFF[L3IDX][SUBIDX];
    const flt_t tAnlm1 = aAnlm[i1];
    const flt_t tAnlm2 = aAnlm[i2];
    const flt_t tAnlm3 = aAnlm[i3];
    return coeff * (
        (aGradGradAnlm[i1] * tAnlm2*tAnlm3) +
        (aGradGradAnlm[i2] * tAnlm1*tAnlm3) +
        (aGradGradAnlm[i3] * tAnlm1*tAnlm2)
    );
}
template <int L3IDX>
static NNAP_DEVICE flt_t calGradGradL3Sub_(flt_t *aAnlm, flt_t *aGradGradAnlm) noexcept {
    flt_t rGradGradFp3 = ZERO;
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return rGradGradFp3;
    rGradGradFp3 += calGradGradL3SubSub_<L3IDX, __NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 0..<110
    return rGradGradFp3;
}
template <int L3MAX>
static NNAP_DEVICE void calGradGradSphL3(flt_t *aAnlm, flt_t *aGradGradAnlm, flt_t *rGradGradFp) noexcept {
    if (L3MAX <= 1) return;
// >>> NNAPGEN REPEAT
    rGradGradFp[__NNAPGENS_X__] += calGradGradL3Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX == 2) return;
// >>> NNAPGEN REPEAT
    rGradGradFp[__NNAPGENS_X__] += calGradGradL3Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX == 3) return;
// >>> NNAPGEN REPEAT
    rGradGradFp[__NNAPGENS_X__] += calGradGradL3Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX == 4) return;
// >>> NNAPGEN REPEAT
    rGradGradFp[__NNAPGENS_X__] += calGradGradL3Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX == 5) return;
// >>> NNAPGEN REPEAT
    rGradGradFp[__NNAPGENS_X__] += calGradGradL3Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline NNAP_DEVICE flt_t calGradGradL4SubSub_(flt_t *aAnlm, flt_t *aGradGradAnlm) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr flt_t coeff = L4_COEFF[L4IDX][SUBIDX];
    const flt_t tAnlm1 = aAnlm[i1];
    const flt_t tAnlm2 = aAnlm[i2];
    const flt_t tAnlm3 = aAnlm[i3];
    const flt_t tAnlm4 = aAnlm[i4];
    return coeff * (
        (aGradGradAnlm[i1] * tAnlm2*tAnlm3*tAnlm4) +
        (aGradGradAnlm[i2] * tAnlm1*tAnlm3*tAnlm4) +
        (aGradGradAnlm[i3] * tAnlm1*tAnlm2*tAnlm4) +
        (aGradGradAnlm[i4] * tAnlm1*tAnlm2*tAnlm3)
    );
}
template <int L4IDX>
static NNAP_DEVICE flt_t calGradGradL4Sub_(flt_t *aAnlm, flt_t *aGradGradAnlm) noexcept {
    flt_t rGradGradFp3 = ZERO;
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return rGradGradFp3;
    rGradGradFp3 += calGradGradL4SubSub_<L4IDX, __NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 0..<100
    return rGradGradFp3;
}
template <int L4MAX>
static NNAP_DEVICE void calGradGradSphL4(flt_t *aAnlm, flt_t *aGradGradAnlm, flt_t *rGradGradFp) noexcept {
    if (L4MAX < 1) return;
    rGradGradFp[0] += calGradGradL4Sub_<0>(aAnlm, aGradGradAnlm);
    if (L4MAX == 1) return;
// >>> NNAPGEN REPEAT
    rGradGradFp[__NNAPGENS_X__] += calGradGradL4Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX == 2) return;
// >>> NNAPGEN REPEAT
    rGradGradFp[__NNAPGENS_X__] += calGradGradL4Sub_<__NNAPGENS_X__>(aAnlm, aGradGradAnlm);
// <<< NNAPGEN REPEAT 3..8
}

template <int LMAX>
static NNAP_DEVICE void calBGradSphL2(flt_t *aAnlm, flt_t *rBGradAnlm, flt_t *aBGradAGradAnlm, flt_t *aAGradFp) noexcept {
    calGradSphL2<LMAX>(aBGradAGradAnlm, rBGradAnlm, aAGradFp);
}
template <int L3IDX, int SUBIDX>
static inline NNAP_DEVICE void calBGradL3SubSub_(flt_t *aAnlm, flt_t *rBGradAnlm, flt_t *aBGradAGradAnlm, flt_t aSubAGradFp) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr flt_t coeff = L3_COEFF[L3IDX][SUBIDX];
    const flt_t tMul = coeff * aSubAGradFp;
    const flt_t tAnlm1 = aAnlm[i1], tBGradAGradAnlm1 = aBGradAGradAnlm[i1];
    const flt_t tAnlm2 = aAnlm[i2], tBGradAGradAnlm2 = aBGradAGradAnlm[i2];
    const flt_t tAnlm3 = aAnlm[i3], tBGradAGradAnlm3 = aBGradAGradAnlm[i3];
    rBGradAnlm[i1] += tMul*(tBGradAGradAnlm2*tAnlm3 + tAnlm2*tBGradAGradAnlm3);
    rBGradAnlm[i2] += tMul*(tBGradAGradAnlm1*tAnlm3 + tAnlm1*tBGradAGradAnlm3);
    rBGradAnlm[i3] += tMul*(tBGradAGradAnlm1*tAnlm2 + tAnlm1*tBGradAGradAnlm2);
}
template <int L3IDX>
static NNAP_DEVICE void calBGradL3Sub_(flt_t *aAnlm, flt_t *rBGradAnlm, flt_t *aBGradAGradAnlm, flt_t aSubAGradFp) noexcept {
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return;
    calBGradL3SubSub_<L3IDX, __NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aSubAGradFp);
// <<< NNAPGEN REPEAT 0..<110
}
template <int L3MAX>
static NNAP_DEVICE void calBGradSphL3(flt_t *aAnlm, flt_t *rBGradAnlm, flt_t *aBGradAGradAnlm, flt_t *aAGradFp) noexcept {
    if (L3MAX <= 1) return;
// >>> NNAPGEN REPEAT
    calBGradL3Sub_<__NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX == 2) return;
// >>> NNAPGEN REPEAT
    calBGradL3Sub_<__NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX == 3) return;
// >>> NNAPGEN REPEAT
    calBGradL3Sub_<__NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX == 4) return;
// >>> NNAPGEN REPEAT
    calBGradL3Sub_<__NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX == 5) return;
// >>> NNAPGEN REPEAT
    calBGradL3Sub_<__NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline NNAP_DEVICE void calBGradL4SubSub_(flt_t *aAnlm, flt_t *rBGradAnlm, flt_t *aBGradAGradAnlm, flt_t aSubAGradFp) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr flt_t coeff = L4_COEFF[L4IDX][SUBIDX];
    const flt_t tMul = coeff * aSubAGradFp;
    const flt_t tAnlm1 = aAnlm[i1], tBGradAGradAnlm1 = aBGradAGradAnlm[i1];
    const flt_t tAnlm2 = aAnlm[i2], tBGradAGradAnlm2 = aBGradAGradAnlm[i2];
    const flt_t tAnlm3 = aAnlm[i3], tBGradAGradAnlm3 = aBGradAGradAnlm[i3];
    const flt_t tAnlm4 = aAnlm[i4], tBGradAGradAnlm4 = aBGradAGradAnlm[i4];
    rBGradAnlm[i1] += tMul * (tBGradAGradAnlm2*tAnlm3*tAnlm4 + tAnlm2*tBGradAGradAnlm3*tAnlm4 + tAnlm2*tAnlm3*tBGradAGradAnlm4);
    rBGradAnlm[i2] += tMul * (tBGradAGradAnlm1*tAnlm3*tAnlm4 + tAnlm1*tBGradAGradAnlm3*tAnlm4 + tAnlm1*tAnlm3*tBGradAGradAnlm4);
    rBGradAnlm[i3] += tMul * (tBGradAGradAnlm1*tAnlm2*tAnlm4 + tAnlm1*tBGradAGradAnlm2*tAnlm4 + tAnlm1*tAnlm2*tBGradAGradAnlm4);
    rBGradAnlm[i4] += tMul * (tBGradAGradAnlm1*tAnlm2*tAnlm3 + tAnlm1*tBGradAGradAnlm2*tAnlm3 + tAnlm1*tAnlm2*tBGradAGradAnlm3);
}
template <int L4IDX>
static NNAP_DEVICE void calBGradL4Sub_(flt_t *aAnlm, flt_t *rBGradAnlm, flt_t *aBGradAGradAnlm, flt_t aSubAGradFp) noexcept {
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) return;
    calBGradL4SubSub_<L4IDX, __NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aSubAGradFp);
// <<< NNAPGEN REPEAT 0..<100
}
template <int L4MAX>
static NNAP_DEVICE void calBGradSphL4(flt_t *aAnlm, flt_t *rBGradAnlm, flt_t *aBGradAGradAnlm, flt_t *aAGradFp) noexcept {
    if (L4MAX < 1) return;
    calBGradL4Sub_<0>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[0]);
    if (L4MAX == 1) return;
// >>> NNAPGEN REPEAT
    calBGradL4Sub_<__NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX == 2) return;
// >>> NNAPGEN REPEAT
    calBGradL4Sub_<__NNAPGENS_X__>(aAnlm, rBGradAnlm, aBGradAGradAnlm, aAGradFp[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 3..8
}

}

#endif //BASIS_SPHERICAL_UTIL_H