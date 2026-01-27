#ifndef BASIS_SPHERICAL_UTIL_H
#define BASIS_SPHERICAL_UTIL_H

#include "basis_SphericalUtil0.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENS_X__ 0
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

template <int M, int L>
static inline void realNormalizedLegendreInterLoopSubSub_(flt_t aX, flt_t *rDest) noexcept {
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
static inline void realNormalizedLegendreInterLoopSub_(flt_t aX, flt_t *rDest) noexcept {
// >>> NNAPGEN REPEAT
    if (L-1==__NNAPGENS_X__) {return;} realNormalizedLegendreInterLoopSubSub_<__NNAPGENS_X__, L>(aX, rDest);
// <<< NNAPGEN REPEAT 0..12
}
template <int L>
static inline void realNormalizedLegendreInterLoop_(flt_t aX, flt_t aY, flt_t *rDest, flt_t &rPll) noexcept {
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
static inline void realNormalizedLegendreFull(flt_t aX, flt_t aY, flt_t *rDest) noexcept {
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
static inline void realSphericalHarmonicsFull4InterLoopSubSub_(flt_t aSqrt2CosMPhi, flt_t aSqrt2SinMPhi, flt_t *rDest) noexcept {
    rDest[L*L+L + M] *= aSqrt2CosMPhi;
    rDest[L*L+L - M] *= aSqrt2SinMPhi;
}
template <int M, int LMAX>
static inline void realSphericalHarmonicsFull4InterLoopSub_(flt_t aSqrt2CosMPhi, flt_t aSqrt2SinMPhi, flt_t *rDest) noexcept {
// >>> NNAPGEN REPEAT
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+__NNAPGENS_X__>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int M, int LMAX>
static inline void realSphericalHarmonicsFull4InterLoop_(flt_t aCosPhi2, flt_t &rSinMPhi, flt_t &rSinMmmPhi, flt_t &rCosMPhi, flt_t &rCosMmmPhi, flt_t *rDest) noexcept {
    const flt_t fSqrt2CosMPhi = SQRT2*rCosMPhi;
    const flt_t fSqrt2SinMPhi = SQRT2*rSinMPhi;
    realSphericalHarmonicsFull4InterLoopSub_<M, LMAX>(fSqrt2CosMPhi, fSqrt2SinMPhi, rDest);
    const flt_t tSinMppPhi = aCosPhi2 * rSinMPhi - rSinMmmPhi;
    const flt_t tCosMppPhi = aCosPhi2 * rCosMPhi - rCosMmmPhi;
    rSinMmmPhi = rSinMPhi; rCosMmmPhi = rCosMPhi;
    rSinMPhi = tSinMppPhi; rCosMPhi = tCosMppPhi;
}
template <int LMAX>
static inline void realSphericalHarmonicsFull4(flt_t aX, flt_t aY, flt_t aZ, flt_t aDis, flt_t *rDest) noexcept {
    const flt_t tXY = hypot(aX, aY);
    const flt_t tCosTheta = aZ / aDis;
    const flt_t tSinTheta = tXY / aDis;
    flt_t tCosPhi;
    flt_t tSinPhi;
    // avoid nan
    if (numericEqual(tXY, ZERO)) {
        tCosPhi = ONE;
        tSinPhi = ZERO;
    } else {
        tCosPhi = aX / tXY;
        tSinPhi = aY / tXY;
    }
    // cal real Legendre
    realNormalizedLegendreFull<LMAX>(tCosTheta, tSinTheta, rDest);
    if (LMAX == 0) return;
    // cal sinMPhi & conMPhi
    flt_t rSinMmmPhi = ZERO;
    flt_t rCosMmmPhi = ONE;
    flt_t rSinMPhi = tSinPhi;
    flt_t rCosMPhi = tCosPhi;
    const flt_t tCosPhi2 = rCosMPhi+rCosMPhi;
// >>> NNAPGEN REPEAT
    realSphericalHarmonicsFull4InterLoop_<__NNAPGENS_X__, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 1..12
}

template <int L>
static inline void calYPphi(flt_t *rYPphi, flt_t *aY) noexcept {
    constexpr int tStart = L*L;
    constexpr int tIdx = tStart+L;
    for (int m = -L; m <= L; ++m) {
        rYPphi[tIdx+m] = -((flt_t)m) * aY[tIdx-m];
    }
}
template <int L>
static inline void calYPtheta(flt_t aCosPhi, flt_t aSinPhi, flt_t *rYPtheta, flt_t *aY) noexcept {
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
        for (int m = 2; m <= L; ++m) {
            const flt_t tMul2 = -((flt_t)0.5)*SQRT_LPM_LMM1[tIdx+m];
            rYPtheta[tIdx+m] = tMul2 * (aCosPhi*aY[tIdx+m-1] - aSinPhi*aY[tIdx-m+1]);
            rYPtheta[tIdx-m] = tMul2 * (aCosPhi*aY[tIdx-m+1] + aSinPhi*aY[tIdx+m-1]);
        }
        for (int m = 1; m < L; ++m) {
            const flt_t tMul2 = ((flt_t)0.5)*SQRT_LPM1_LMM[tIdx+m];
            rYPtheta[tIdx+m] += tMul2 * (aCosPhi*aY[tIdx+m+1] + aSinPhi*aY[tIdx-m-1]);
            rYPtheta[tIdx-m] += tMul2 * (aCosPhi*aY[tIdx-m-1] - aSinPhi*aY[tIdx+m+1]);
        }
        return;
    }}
}
template <int LMAX>
static void calYPphiPtheta(flt_t *rYPphi, flt_t aCosPhi, flt_t aSinPhi, flt_t *rYPtheta, flt_t *aY) noexcept {
// >>> NNAPGEN REPEAT
    calYPphi<__NNAPGENS_X__>(rYPphi, aY); calYPtheta<__NNAPGENS_X__>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX==__NNAPGENS_X__) return;
// <<< NNAPGEN REPEAT 0..12
}
template <int N>
static inline void convertYPPhiPtheta2YPxyz(flt_t aCosTheta, flt_t aSinTheta, flt_t aCosPhi, flt_t aSinPhi, flt_t aDis, flt_t aDxy, int aDxyCloseZero,
                                            flt_t *rYPx, flt_t *rYPy, flt_t *rYPz, flt_t *aYPtheta, flt_t *aYPphi) noexcept {
    const flt_t thetaPx = -aCosTheta * aCosPhi / aDis;
    const flt_t thetaPy = -aCosTheta * aSinPhi / aDis;
    const flt_t thetaPz =  aSinTheta / aDis;
    const flt_t phiPx = aDxyCloseZero ? ZERO :  aSinPhi / aDxy;
    const flt_t phiPy = aDxyCloseZero ? ZERO : -aCosPhi / aDxy;
    for (int i = 0; i < N; ++i) {
        const flt_t tYPtheta = aYPtheta[i];
        const flt_t tYPphi = aYPphi[i];
        rYPx[i] = tYPtheta*thetaPx + tYPphi*phiPx;
        rYPy[i] = tYPtheta*thetaPy + tYPphi*phiPy;
        rYPz[i] = tYPtheta*thetaPz;
    }
}
template <int LMAX>
static void calYPxyz(flt_t *aY, flt_t aDx, flt_t aDy, flt_t aDz, flt_t aDis,
                     flt_t *rYPx, flt_t *rYPy, flt_t *rYPz, flt_t *rYPtheta, flt_t *rYPphi) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    const flt_t dxy = hypot(aDx, aDy);
    const flt_t cosTheta = aDz / aDis;
    const flt_t sinTheta = dxy / aDis;
    flt_t cosPhi;
    flt_t sinPhi;
    const int dxyCloseZero = numericEqual(dxy, ZERO);
    if (dxyCloseZero) {
        cosPhi = ONE;
        sinPhi = ZERO;
    } else {
        cosPhi = aDx / dxy;
        sinPhi = aDy / dxy;
    }
    calYPphiPtheta<LMAX>(rYPphi, cosPhi, sinPhi, rYPtheta, aY);
    if (dxyCloseZero) {
        // fix singularity
        for (int k = 0; k < tLMAll; ++k) rYPphi[k] = ZERO;
    }
    // conert to Pxyz
    convertYPPhiPtheta2YPxyz<tLMAll>(cosTheta, sinTheta, cosPhi, sinPhi, aDis, dxy, dxyCloseZero,
                                     rYPx, rYPy, rYPz, rYPtheta, rYPphi);
}


template <int NMAX, int LMAX, int MPLUS, int WTFLAG>
static void setormplusCnlm_(flt_t *rCnlm, flt_t *rCnlmWt, flt_t *aY, flt_t aFc, flt_t *aRn, flt_t aWt) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tCnlm = rCnlm;
    flt_t *tCnlmWt = rCnlmWt;
    for (int n = 0; n <= NMAX; ++n) {
        const flt_t tMul = aFc*aRn[n];
        for (int k = 0; k < tLMAll; ++k) {
            const flt_t tValue = tMul*aY[k];
            if (MPLUS) {
                tCnlm[k] += tValue;
                if (WTFLAG) tCnlmWt[k] += aWt*tValue;
            } else {
                tCnlm[k] = tValue;
                if (WTFLAG) tCnlmWt[k] = aWt*tValue;
            }
        }
        tCnlm += tLMAll;
        if (WTFLAG) tCnlmWt += tLMAll;
    }
}
template <int NMAX, int LMAX>
static inline void calBnlm(flt_t *rBnlm, flt_t *aY, flt_t aFc, flt_t *aRn) noexcept {
    setormplusCnlm_<NMAX, LMAX, FALSE, FALSE>(rBnlm, NULL, aY, aFc, aRn, ZERO);
}
template <int NMAX, int LMAX>
static inline void mplusCnlm(flt_t *rCnlm, flt_t *aY, flt_t aFc, flt_t *aRn) noexcept {
    setormplusCnlm_<NMAX, LMAX, TRUE, FALSE>(rCnlm, NULL, aY, aFc, aRn, ZERO);
}
template <int NMAX, int LMAX>
static inline void mplusCnlmWt(flt_t *rCnlm, flt_t *rCnlmWt, flt_t *aY, flt_t aFc, flt_t *aRn, flt_t aWt) noexcept {
    setormplusCnlm_<NMAX, LMAX, TRUE, TRUE>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt);
}

template <int LMAX>
static void mplusLM(flt_t *rAlm, flt_t *aMul, flt_t *aBlm) {
    flt_t *tAlm = rAlm, *tBlm = aBlm;
// >>> NNAPGEN REPEAT
    mplus<2*__NNAPGENS_X__+1>(tAlm, aMul[__NNAPGENS_X__], tBlm); if (LMAX==__NNAPGENS_X__) {return;} tAlm += (2*__NNAPGENS_X__+1); tBlm += (2*__NNAPGENS_X__+1);
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static void dotLM(flt_t *rDot, flt_t *aAlm, flt_t *aBlm) {
    flt_t *tAlm = aAlm, *tBlm = aBlm;
// >>> NNAPGEN REPEAT
    rDot[__NNAPGENS_X__] += dot<2*__NNAPGENS_X__+1>(tAlm, tBlm); if (LMAX==__NNAPGENS_X__) {return;} tAlm += (2*__NNAPGENS_X__+1); tBlm += (2*__NNAPGENS_X__+1);
// <<< NNAPGEN REPEAT 0..12
}
template <int LMAX>
static void multiplyLM(flt_t *rClm, flt_t *aMul) {
    flt_t *tClm = rClm;
// >>> NNAPGEN REPEAT
    multiply<2*__NNAPGENS_X__+1>(tClm, aMul[__NNAPGENS_X__]); if (LMAX==__NNAPGENS_X__) {return;} tClm += (2*__NNAPGENS_X__+1);
// <<< NNAPGEN REPEAT 0..12
}

template <int NMAX, int LMAX, int FSIZE, int EXFLAG>
static void mplusCnlmFuse_(flt_t *rCnlm, flt_t *aBnlm, flt_t *aFuseWeight, int aType) noexcept {
    constexpr int tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    flt_t *tFuseWeight = aFuseWeight + FSIZE*(aType-1);
    flt_t *tCnlm = rCnlm;
    if (EXFLAG) {
        mplus<tSizeBnlm>(tCnlm, ONE, aBnlm);
        tCnlm += tSizeBnlm;
    }
    for (int k = 0; k < FSIZE; ++k) {
        mplus<tSizeBnlm>(tCnlm, tFuseWeight[k], aBnlm);
        tCnlm += tSizeBnlm;
    }
}
template <int NMAX, int LMAX, int FSIZE>
static inline void mplusCnlmFuse(flt_t *rCnlm, flt_t *aBnlm, flt_t *aFuseWeight, int aType) noexcept {
    mplusCnlmFuse_<NMAX, LMAX, FSIZE, FALSE>(rCnlm, aBnlm, aFuseWeight, aType);
}
template <int NMAX, int LMAX, int FSIZE>
static inline void mplusCnlmExFuse(flt_t *rCnlm, flt_t *aBnlm, flt_t *aFuseWeight, int aType) noexcept {
    mplusCnlmFuse_<NMAX, LMAX, FSIZE, TRUE>(rCnlm, aBnlm, aFuseWeight, aType);
}


template <int SIZEN, int LMAX, int PFSIZE>
static void mplusAnlm(flt_t *rAnlm, flt_t *aCnlm, flt_t *aPostFuseWeight) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tAnlm = rAnlm;
    flt_t *tPostFuseWeight = aPostFuseWeight;
    for (int np = 0; np < PFSIZE; ++np) {
        flt_t *tCnlm = aCnlm;
        for (int n = 0; n < SIZEN; ++n) {
            mplus<tLMAll>(tAnlm, tPostFuseWeight[n], tCnlm);
            tCnlm += tLMAll;
        }
        tPostFuseWeight += SIZEN;
        tAnlm += tLMAll;
    }
}



template <int SIZEN, int LMAX, int PFSIZE>
static void mplusGradAnlm(flt_t *aGradAnlm, flt_t *rGradCnlm, flt_t *aPostFuseWeight) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    flt_t *tGradCnlm = rGradCnlm;
    for (int n = 0; n < SIZEN; ++n) {
        flt_t *tGradAnlm = aGradAnlm;
        for (int np = 0; np < PFSIZE; ++np) {
            mplus<tLMAll>(tGradCnlm, aPostFuseWeight[n + np*SIZEN], tGradAnlm);
            tGradAnlm += tLMAll;
        }
        tGradCnlm += tLMAll;
    }
}


template <int NMAX, int LMAX, int FSIZE, int EXFLAG>
static void calGradBnlmFuse_(flt_t *aGradCnlm, flt_t *rGradBnlm, flt_t *aFuseWeight, int aType) noexcept {
    constexpr int tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    fill<tSizeBnlm>(rGradBnlm, ZERO);
    flt_t *tFuseWeight = aFuseWeight + FSIZE*(aType-1);
    flt_t *tGradCnlm = aGradCnlm;
    if (EXFLAG) {
        mplus<tSizeBnlm>(rGradBnlm, ONE, tGradCnlm);
        tGradCnlm += tSizeBnlm;
    }
    for (int k = 0; k < FSIZE; ++k) {
        mplus<tSizeBnlm>(rGradBnlm, tFuseWeight[k], tGradCnlm);
        tGradCnlm += tSizeBnlm;
    }
}
template <int NMAX, int LMAX, int FSIZE>
static inline void calGradBnlmFuse(flt_t *aGradCnlm, flt_t *rGradBnlm, flt_t *aFuseWeight, int aType) noexcept {
    calGradBnlmFuse_<NMAX, LMAX, FSIZE, FALSE>(aGradCnlm, rGradBnlm, aFuseWeight, aType);
}
template <int NMAX, int LMAX, int FSIZE>
static inline void calGradBnlmExFuse(flt_t *aGradCnlm, flt_t *rGradBnlm, flt_t *aFuseWeight, int aType) noexcept {
    calGradBnlmFuse_<NMAX, LMAX, FSIZE, TRUE>(aGradCnlm, rGradBnlm, aFuseWeight, aType);
}

template <int NMAX, int LMAX, int WTFLAG>
static void gradCnlm2xyz_(int j, flt_t *aGradCnlm, flt_t *aGradCnlmWt, flt_t *rGradY,
                          flt_t *aY, flt_t aFc, flt_t *aRn, flt_t aWt,
                          flt_t aFcPx, flt_t aFcPy, flt_t aFcPz,
                          flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz,
                          flt_t *aYPx, flt_t *aYPy, flt_t *aYPz,
                          flt_t *rFx, flt_t *rFy, flt_t *rFz) noexcept {
    constexpr int tLMAll = (LMAX+1)*(LMAX+1);
    // clear gradY here
    fill<tLMAll>(rGradY, ZERO);
    flt_t tGradFc = ZERO;
    flt_t rFxj = ZERO, rFyj = ZERO, rFzj = ZERO;
    flt_t *tGradCnlm = aGradCnlm;
    flt_t *tGradCnlmWt = aGradCnlmWt;
    for (int n = 0; n <= NMAX; ++n) {
        const flt_t tRnn = aRn[n];
        const flt_t tMul = aFc * tRnn;
        flt_t tGradRn = ZERO;
        for (int k = 0; k < tLMAll; ++k) {
            flt_t subGradBnlm = tGradCnlm[k];
            if (WTFLAG) subGradBnlm += aWt*tGradCnlmWt[k];
            rGradY[k] += tMul * subGradBnlm;
            tGradRn += aY[k] * subGradBnlm;
        }
        tGradCnlm += tLMAll;
        if (WTFLAG) tGradCnlmWt += tLMAll;
        
        tGradFc += tRnn * tGradRn;
        tGradRn *= aFc;
        rFxj += tGradRn*aRnPx[n];
        rFyj += tGradRn*aRnPy[n];
        rFzj += tGradRn*aRnPz[n];
    }
    for (int k = 0; k < tLMAll; ++k) {
        const flt_t subGradY = rGradY[k];
        rFxj += subGradY*aYPx[k];
        rFyj += subGradY*aYPy[k];
        rFzj += subGradY*aYPz[k];
    }
    rFxj += aFcPx*tGradFc;
    rFyj += aFcPy*tGradFc;
    rFzj += aFcPz*tGradFc;
    rFx[j] += rFxj; rFy[j] += rFyj; rFz[j] += rFzj;
}
template <int NMAX, int LMAX>
static inline void gradBnlm2xyz(int j, flt_t *aGradBnlm, flt_t *rGradY, flt_t *aY, flt_t aFc, flt_t *aRn, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz,
                                flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *aYPx, flt_t *aYPy, flt_t *aYPz, flt_t *rFx, flt_t *rFy, flt_t *rFz) noexcept {
    gradCnlm2xyz_<NMAX, LMAX, FALSE>(j, aGradBnlm, NULL, rGradY, aY, aFc, aRn, 0, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz);
}
template <int NMAX, int LMAX>
static inline void gradCnlmWt2xyz(int j, flt_t *aGradCnlm, flt_t *aGradCnlmWt, flt_t *rGradY, flt_t *aY, flt_t aFc, flt_t *aRn, flt_t aWt, flt_t aFcPx, flt_t aFcPy, flt_t aFcPz,
                                  flt_t *aRnPx, flt_t *aRnPy, flt_t *aRnPz, flt_t *aYPx, flt_t *aYPy, flt_t *aYPz, flt_t *rFx, flt_t *rFy, flt_t *rFz) noexcept {
    gradCnlm2xyz_<NMAX, LMAX, TRUE>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz);
}



template <int L>
static inline void calL2Sub_(flt_t *aCnlm, flt_t *rFp) noexcept {
    constexpr int tLen = L+L+1;
    const flt_t rDot = dot<tLen>(aCnlm + (L*L));
    rFp[L-1] = (PI4/(flt_t)tLen) * rDot;
}
template <int LMAX, int NORADIAL>
static void calSphL2(flt_t *aCnlm, flt_t *rFp) noexcept {
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
static inline flt_t calL3SubSub_(flt_t *aCnlm) noexcept {
    constexpr int i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr int i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr int i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr flt_t coeff = L3_COEFF[L3IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3];
}
template <int L3IDX>
static flt_t calL3Sub_(flt_t *aCnlm) noexcept {
    flt_t rFp3 = ZERO;
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, __NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 0..<110
    return rFp3;
}
template <int L3MAX>
static void calSphL3(flt_t *aCnlm, flt_t *rFp) noexcept {
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
static inline flt_t calL4SubSub_(flt_t *aCnlm) noexcept {
    constexpr int i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr int i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr int i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr int i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr flt_t coeff = L4_COEFF[L4IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3]*aCnlm[i4];
}
template <int L4IDX>
static flt_t calL4Sub_(flt_t *aCnlm) noexcept {
    flt_t rFp4 = ZERO;
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, __NNAPGENS_X__>(aCnlm);
// <<< NNAPGEN REPEAT 0..<100
    return rFp4;
}
template <int L4MAX>
static void calSphL4(flt_t *aCnlm, flt_t *rFp) noexcept {
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
static inline void calGradL2Sub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int tStart = L*L;
    constexpr int tLen = L+L+1;
    constexpr int tEnd = tStart+tLen;
    const flt_t tMul = (TWO*PI4/(flt_t)tLen) * aSubNNGrad;
    for (int i = tStart; i < tEnd; ++i) {
        rGradCnlm[i] += tMul * aCnlm[i];
    }
}
template <int LMAX, int NORADIAL>
static void calGradSphL2(flt_t *aCnlm, flt_t *rGradCnlm, flt_t *aNNGrad) noexcept {
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
static inline void calGradL3SubSub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
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
static void calGradL3Sub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int tSize = L3_SIZE[L3IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return;} calGradL3SubSub_<L3IDX, __NNAPGENS_X__>(aCnlm, rGradCnlm, aSubNNGrad);
// <<< NNAPGEN REPEAT 0..<110
}
template <int L3MAX>
static void calGradSphL3(flt_t *aCnlm, flt_t *rGradCnlm, flt_t *aNNGrad) noexcept {
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
static inline void calGradL4SubSub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
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
static void calGradL4Sub_(flt_t *aCnlm, flt_t *rGradCnlm, flt_t aSubNNGrad) noexcept {
    constexpr int tSize = L4_SIZE[L4IDX];
// >>> NNAPGEN REPEAT
    if (tSize==__NNAPGENS_X__) {return;} calGradL4SubSub_<L4IDX, __NNAPGENS_X__>(aCnlm, rGradCnlm, aSubNNGrad);
// <<< NNAPGEN REPEAT 0..<100
}
template <int L4MAX>
static void calGradSphL4(flt_t *aCnlm, flt_t *rGradCnlm, flt_t *aNNGrad) noexcept {
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