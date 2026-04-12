#ifndef BASIS_SPHERICAL_UTIL_H
#define BASIS_SPHERICAL_UTIL_H

#include "basis_SphericalUtil0.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENS_X__ 0
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

template <int M, int L>
static inline NNAP_DEVICE void realNormalizedLegendreInterLoopSubSub_(flt_t aX, flt_t *rDest) noexcept {
    constexpr flt_t tSHAlm = SH_Alm[L*(L+1)/2 + M];
    constexpr flt_t tSHBlm = SH_Blm[L*(L+1)/2 + M];
    const flt_t tPlm = tSHAlm * (aX*rDest[(L-1)*(L-1)+(L-1) + M] + tSHBlm*rDest[(L-2)*(L-2)+(L-2) + M]);
    if (M == 0) {
        rDest[L*L+L] = tPlm;
    } else {
        rDest[L*L+L + M] = tPlm;
        rDest[L*L+L - M] = tPlm;
    }
}
template <int L>
static inline NNAP_DEVICE void realNormalizedLegendreInterLoopSub_(flt_t aX, flt_t *rDest) noexcept {
// >>> NNAPGEN REPEAT
    if (L-1==__NNAPGENS_X__) {return;} realNormalizedLegendreInterLoopSubSub_<__NNAPGENS_X__, L>(aX, rDest);
// <<< NNAPGEN REPEAT 0..12
}
template <int L>
static inline NNAP_DEVICE void realNormalizedLegendreInterLoop_(flt_t aX, flt_t aY, flt_t *rDest, flt_t &rPll) noexcept {
    realNormalizedLegendreInterLoopSub_<L>(aX, rDest);
    constexpr flt_t tMul1 = SQRT_2LM1P3[L];
    const flt_t tPlm = tMul1 * aX * rPll;
    rDest[L*L+L + (L-1)] = tPlm;
    rDest[L*L+L - (L-1)] = tPlm;
    constexpr flt_t tMul2 = -SQRT_1P1D2L[L];
    rPll *= tMul2 * aY;
    rDest[L*L+L + L] = rPll;
    rDest[L*L+L - L] = rPll;
}
template <int LMAX>
static inline NNAP_DEVICE void realNormalizedLegendreFull(flt_t aX, flt_t aY, flt_t *rDest) noexcept {
    flt_t tPll = 0.28209479177387814347403972578039; // = sqrt(1/(4*PI))
    rDest[0] = tPll;
    if (LMAX == 0) return;
    rDest[2] = SQRT3 * aX * tPll;
    tPll *= (-SQRT3DIV2 * aY);
    rDest[2+1] = tPll;
    rDest[2-1] = tPll;
    if (LMAX == 1) return;
// >>> NNAPGEN REPEAT
    realNormalizedLegendreInterLoop_<__NNAPGENS_X__>(aX, aY, rDest, tPll); if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 2..12
}

template <int M, int L>
static inline NNAP_DEVICE void realSphericalHarmonicsFull4InterLoopSubSub_(flt_t aSqrt2CosMPhi, flt_t aSqrt2SinMPhi, flt_t *rDest) noexcept {
    rDest[L*L+L + M] *= aSqrt2CosMPhi;
    rDest[L*L+L - M] *= aSqrt2SinMPhi;
}
template <int M, int LMAX>
static inline NNAP_DEVICE void realSphericalHarmonicsFull4InterLoopSub_(flt_t aSqrt2CosMPhi, flt_t aSqrt2SinMPhi, flt_t *rDest) noexcept {
// >>> NNAPGEN REPEAT
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+__NNAPGENS_X__>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int M, int LMAX>
static inline NNAP_DEVICE void realSphericalHarmonicsFull4InterLoop_(flt_t aCosPhi2, flt_t &rSinMPhi, flt_t &rSinMmmPhi, flt_t &rCosMPhi, flt_t &rCosMmmPhi, flt_t *rDest) noexcept {
    const flt_t fSqrt2CosMPhi = SQRT2*rCosMPhi;
    const flt_t fSqrt2SinMPhi = SQRT2*rSinMPhi;
    realSphericalHarmonicsFull4InterLoopSub_<M, LMAX>(fSqrt2CosMPhi, fSqrt2SinMPhi, rDest);
    const flt_t tSinMppPhi = aCosPhi2 * rSinMPhi - rSinMmmPhi;
    const flt_t tCosMppPhi = aCosPhi2 * rCosMPhi - rCosMmmPhi;
    rSinMmmPhi = rSinMPhi; rCosMmmPhi = rCosMPhi;
    rSinMPhi = tSinMppPhi; rCosMPhi = tCosMppPhi;
}
template <int LMAX>
static inline NNAP_DEVICE void realSphericalHarmonicsFull4_(flt_t aCosTheta, flt_t aSinTheta, flt_t aCosPhi, flt_t aSinPhi, flt_t *rDest) noexcept {
    // cal real Legendre
    realNormalizedLegendreFull<LMAX>(aCosTheta, aSinTheta, rDest);
    if (LMAX == 0) return;
    // cal sinMPhi & conMPhi
    flt_t rSinMmmPhi = ZERO;
    flt_t rCosMmmPhi = ONE;
    flt_t rSinMPhi = aSinPhi;
    flt_t rCosMPhi = aCosPhi;
    const flt_t tCosPhi2 = rCosMPhi+rCosMPhi;
// >>> NNAPGEN REPEAT
    realSphericalHarmonicsFull4InterLoop_<__NNAPGENS_X__, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int LMAX>
static inline NNAP_DEVICE void calY(flt_t *rY, flt_t aDx, flt_t aDy, flt_t aDz, flt_t aDis) noexcept {
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
static inline NNAP_DEVICE void calYPphi_(flt_t *rYPphi, flt_t *aY) noexcept {
    constexpr int tStart = L*L;
    constexpr int tIdx = tStart+L;
    for (int m = -L; m <= L; ++m) {
        rYPphi[tIdx+m] = -((flt_t)m) * aY[tIdx-m];
    }
}
template <int L>
static inline NNAP_DEVICE void calYPtheta_(flt_t *rYPtheta, flt_t *aY, flt_t aCosPhi, flt_t aSinPhi) noexcept {
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
    calYPphi_<__NNAPGENS_X__>(rYPphi, aY); calYPtheta_<__NNAPGENS_X__>(rYPtheta, aY, aCosPhi, aSinPhi); if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static inline NNAP_DEVICE void calYPthetaPphi(flt_t *rYPtheta, flt_t *rYPphi, flt_t *aY, flt_t aDx, flt_t aDy, flt_t aDz, flt_t aDis,
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


template <int SIZE_NP, int LMAX>
static inline NNAP_DEVICE void mplusAnlm(flt_t *rAnlm, flt_t *aY, flt_t *aRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tAnlm = rAnlm;
    for (int np = 0; np < SIZE_NP; ++np) {
        mplus<tLMAll>(tAnlm, aRnp[np], aY);
        tAnlm += tLMAll;
    }
}
template <int SIZE_NP, int LMAX>
static inline NNAP_DEVICE void mplusAnlmEx(flt_t *rAnlmEx, flt_t *rAnlm, flt_t *aY, flt_t *aRnp) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tAnlmEx = rAnlmEx;
    flt_t *tAnlm = rAnlm;
    for (int np = 0; np < SIZE_NP; ++np) {
        mplusEx<tLMAll>(tAnlmEx, tAnlm, aRnp[np], aY);
        tAnlmEx += tLMAll;
        tAnlm += tLMAll;
    }
}


template <int SIZE_NP, int LMAX>
static inline NNAP_DEVICE void gradAnlm2xyz(int j, flt_t *aGradAnlm, flt_t *rGradY, flt_t *aY, flt_t *aRnp,
                                            flt_t *aRnpGrad, flt_t *aYPtheta, flt_t *aYPphi,
                                            flt_t dx, flt_t dy, flt_t dz, flt_t thetaPx, flt_t thetaPy, flt_t thetaPz, flt_t phiPx, flt_t phiPy,
                                            flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    // clear gradY here
    fill<tLMAll>(rGradY, ZERO);
    flt_t rGradj = ZERO;
    flt_t *tGradAnlm = aGradAnlm;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t tRnpnp = aRnp[np];
        flt_t tGradRnp = ZERO;
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t subGradAnlm = tGradAnlm[k];
            rGradY[k] += tRnpnp * subGradAnlm;
            tGradRnp += aY[k] * subGradAnlm;
        }
        tGradAnlm += tLMAll;
        rGradj += tGradRnp*aRnpGrad[np];
    }
    flt_t rGradThetaj = ZERO, rGradPhij = ZERO;
    for (int k = 0; k < tLMAll; ++k) {
        const flt_t subGradY = rGradY[k];
        rGradThetaj += subGradY*aYPtheta[k];
        rGradPhij += subGradY*aYPphi[k];
    }
    rGradNlDx[j] += rGradj*dx + rGradThetaj*thetaPx + rGradPhij*phiPx;
    rGradNlDy[j] += rGradj*dy + rGradThetaj*thetaPy + rGradPhij*phiPy;
    rGradNlDz[j] += rGradj*dz + rGradThetaj*thetaPz;
}
template <int SIZE_NP, int LMAX>
static inline NNAP_DEVICE void gradAnlmEx2xyz(int j, flt_t *aGradAnlmEx, flt_t *aGradAnlm, flt_t *rGradY, flt_t *aY, flt_t *aRnp,
                                              flt_t *aRnpGrad, flt_t *aYPtheta, flt_t *aYPphi,
                                              flt_t dx, flt_t dy, flt_t dz, flt_t thetaPx, flt_t thetaPy, flt_t thetaPz, flt_t phiPx, flt_t phiPy,
                                              flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    // clear gradY here
    fill<tLMAll>(rGradY, ZERO);
    flt_t rGradj = ZERO;
    flt_t *tGradAnlmEx = aGradAnlmEx;
    flt_t *tGradAnlm = aGradAnlm;
    for (int np = 0; np < SIZE_NP; ++np) {
        const flt_t tRnpnp = aRnp[np];
        flt_t tGradRnp = ZERO;
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t subGradAnlm = tGradAnlm[k] + tGradAnlmEx[k];
            rGradY[k] += tRnpnp * subGradAnlm;
            tGradRnp += aY[k] * subGradAnlm;
        }
        tGradAnlm += tLMAll;
        tGradAnlmEx += tLMAll;
        rGradj += tGradRnp*aRnpGrad[np];
    }
    flt_t rGradThetaj = ZERO, rGradPhij = ZERO;
    for (int k = 0; k < tLMAll; ++k) {
        const flt_t subGradY = rGradY[k];
        rGradThetaj += subGradY*aYPtheta[k];
        rGradPhij += subGradY*aYPphi[k];
    }
    rGradNlDx[j] += rGradj*dx + rGradThetaj*thetaPx + rGradPhij*phiPx;
    rGradNlDy[j] += rGradj*dy + rGradThetaj*thetaPy + rGradPhij*phiPy;
    rGradNlDz[j] += rGradj*dz + rGradThetaj*thetaPz;
}


template <int L>
static inline NNAP_DEVICE void calL2Sub_(flt_t *aCnlm, flt_t *rFp) noexcept {
    constexpr int tLen = L+L+1;
    const flt_t rDot = dot<tLen>(aCnlm + (L*L));
    rFp[L-1] = (PI4/(flt_t)tLen) * rDot;
}
template <int LMAX, int NORADIAL>
static NNAP_DEVICE void calSphL2(flt_t *aCnlm, flt_t *rFp) noexcept {
    // l == 0
    flt_t *tFp = rFp;
    if (!NORADIAL) {
        const flt_t tCn00 = aCnlm[0];
        tFp[0] = PI4 * tCn00*tCn00;
        ++tFp;
    }
    if (LMAX == 0) return;
// >>> NNAPGEN REPEAT
    calL2Sub_<__NNAPGENS_X__>(aCnlm, tFp); if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int L3IDX, int SUBIDX>
static inline NNAP_DEVICE flt_t calL3SubSub_(flt_t *aCnlm) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr flt_t coeff = L3_COEFF[L3IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3];
}
template <int L3IDX>
static NNAP_DEVICE flt_t calL3Sub_(flt_t *aCnlm) noexcept {
    flt_t rFp3 = ZERO;
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, __NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 0..<110
    return rFp3;
}
template <int L3MAX>
static NNAP_DEVICE void calSphL3(flt_t *aCnlm, flt_t *rFp) noexcept {
    if (L3MAX<=1) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX==2) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX==3) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX==4) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX==5) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL3Sub_<__NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline NNAP_DEVICE flt_t calL4SubSub_(flt_t *aCnlm) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr flt_t coeff = L4_COEFF[L4IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3]*aCnlm[i4];
}
template <int L4IDX>
static NNAP_DEVICE flt_t calL4Sub_(flt_t *aCnlm) noexcept {
    flt_t rFp4 = ZERO;
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, __NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 0..<100
    return rFp4;
}
template <int L4MAX>
static NNAP_DEVICE void calSphL4(flt_t *aCnlm, flt_t *rFp) noexcept {
    if (L4MAX<1) return;
    rFp[0] = calL4Sub_<0>(aCnlm);
    if (L4MAX==1) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL4Sub_<__NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX==2) return;
// >>> NNAPGEN REPEAT
    rFp[__NNAPGENS_X__] = calL4Sub_<__NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 3..8
}

template <int L>
static inline NNAP_DEVICE void calGradL2Sub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int tStart = L*L;
    constexpr int tLen = L+L+1;
    constexpr int tEnd = tStart+tLen;
    const flt_t tMul = (TWO*PI4/(flt_t)tLen) * aSubNNGrad;
    for (int i = tStart; i < tEnd; ++i) {
        rGradCnlm[i] += tMul * aCnlm[i];
    }
}
template <int LMAX, int NORADIAL>
static NNAP_DEVICE void calGradSphL2(flt_t *aCnlm, flt_t *rGradCnlm, flt_t *aNNGrad) noexcept {
    // l = 0
    flt_t *tNNGrad = aNNGrad;
    if (!NORADIAL) {
        rGradCnlm[0] += (TWO*PI4) * aCnlm[0] * tNNGrad[0];
        ++tNNGrad;
    }
    if (LMAX == 0) return;
// >>> NNAPGEN REPEAT
    calGradL2Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, tNNGrad[__NNAPGENS_X__-1]); if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
}
template <int L3IDX, int SUBIDX>
static inline NNAP_DEVICE void calGradL3SubSub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr flt_t coeff = L3_COEFF[L3IDX][SUBIDX];
    const flt_t tMul = coeff * aSubNNGrad;
    const flt_t tCnlm1 = aCnlm[i1];
    const flt_t tCnlm2 = aCnlm[i2];
    const flt_t tCnlm3 = aCnlm[i3];
    rGradCnlm[i1] += tMul * tCnlm2*tCnlm3;
    rGradCnlm[i2] += tMul * tCnlm1*tCnlm3;
    rGradCnlm[i3] += tMul * tCnlm1*tCnlm2;
}
template <int L3IDX>
static NNAP_DEVICE void calGradL3Sub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return;} calGradL3SubSub_<L3IDX, __NNAPGENS_X__>(aCnlm, rGradCnlm, aSubNNGrad);
// <<< NNAPGEN REPEAT 0..<110
}
template <int L3MAX>
static NNAP_DEVICE void calGradSphL3(flt_t *aCnlm, flt_t *rGradCnlm, flt_t *aNNGrad) noexcept {
    if (L3MAX <= 1) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, aNNGrad[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 0..1
    if (L3MAX == 2) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, aNNGrad[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 2..3
    if (L3MAX == 3) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, aNNGrad[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 4..8
    if (L3MAX == 4) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, aNNGrad[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 9..13
    if (L3MAX == 5) return;
// >>> NNAPGEN REPEAT
    calGradL3Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, aNNGrad[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 14..22
}
template <int L4IDX, int SUBIDX>
static inline NNAP_DEVICE void calGradL4SubSub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr flt_t coeff = L4_COEFF[L4IDX][SUBIDX];
    const flt_t tMul = coeff * aSubNNGrad;
    const flt_t tCnlm1 = aCnlm[i1];
    const flt_t tCnlm2 = aCnlm[i2];
    const flt_t tCnlm3 = aCnlm[i3];
    const flt_t tCnlm4 = aCnlm[i4];
    rGradCnlm[i1] += tMul * tCnlm2*tCnlm3*tCnlm4;
    rGradCnlm[i2] += tMul * tCnlm1*tCnlm3*tCnlm4;
    rGradCnlm[i3] += tMul * tCnlm1*tCnlm2*tCnlm4;
    rGradCnlm[i4] += tMul * tCnlm1*tCnlm2*tCnlm3;
}
template <int L4IDX>
static NNAP_DEVICE void calGradL4Sub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return;} calGradL4SubSub_<L4IDX, __NNAPGENS_X__>(aCnlm, rGradCnlm, aSubNNGrad);
// <<< NNAPGEN REPEAT 0..<100
}
template <int L4MAX>
static NNAP_DEVICE void calGradSphL4(flt_t *aCnlm, flt_t *rGradCnlm, flt_t *aNNGrad) noexcept {
    if (L4MAX < 1) return;
    calGradL4Sub_<0>(aCnlm, rGradCnlm, aNNGrad[0]);
    if (L4MAX == 1) return;
// >>> NNAPGEN REPEAT
    calGradL4Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, aNNGrad[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 1..2
    if (L4MAX == 2) return;
// >>> NNAPGEN REPEAT
    calGradL4Sub_<__NNAPGENS_X__>(aCnlm, rGradCnlm, aNNGrad[__NNAPGENS_X__]);
// <<< NNAPGEN REPEAT 3..8
}

}

#endif //BASIS_SPHERICAL_UTIL_H