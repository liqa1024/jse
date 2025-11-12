#ifndef BASIS_SPHERICAL_UTIL_H
#define BASIS_SPHERICAL_UTIL_H

#include "basis_SphericalUtil0.hpp"

namespace JSE_NNAP {

template <jint M, jint L>
static inline void realNormalizedLegendreInterLoopSubSub_(jdouble aX, jdouble *rDest) noexcept {
    constexpr jdouble tSHAlm = SH_Alm[L*(L+1)/2 + M];
    constexpr jdouble tSHBlm = SH_Blm[L*(L+1)/2 + M];
    const jdouble tPlm = tSHAlm * (aX*rDest[(L-1)*(L-1)+(L-1) + M] + tSHBlm*rDest[(L-2)*(L-2)+(L-2) + M]);
    if (M == 0) {
        rDest[L*L+L] = tPlm;
    } else {
        rDest[L*L+L + M] = tPlm;
        rDest[L*L+L - M] = tPlm;
    }
}
template <jint L>
static inline void realNormalizedLegendreInterLoopSub_(jdouble aX, jdouble *rDest) noexcept {
    if (L-1==0) {return;} realNormalizedLegendreInterLoopSubSub_<0, L>(aX, rDest);
    if (L-1==1) {return;} realNormalizedLegendreInterLoopSubSub_<1, L>(aX, rDest);
    if (L-1==2) {return;} realNormalizedLegendreInterLoopSubSub_<2, L>(aX, rDest);
    if (L-1==3) {return;} realNormalizedLegendreInterLoopSubSub_<3, L>(aX, rDest);
    if (L-1==4) {return;} realNormalizedLegendreInterLoopSubSub_<4, L>(aX, rDest);
    if (L-1==5) {return;} realNormalizedLegendreInterLoopSubSub_<5, L>(aX, rDest);
    if (L-1==6) {return;} realNormalizedLegendreInterLoopSubSub_<6, L>(aX, rDest);
    if (L-1==7) {return;} realNormalizedLegendreInterLoopSubSub_<7, L>(aX, rDest);
    if (L-1==8) {return;} realNormalizedLegendreInterLoopSubSub_<8, L>(aX, rDest);
    if (L-1==9) {return;} realNormalizedLegendreInterLoopSubSub_<9, L>(aX, rDest);
    if (L-1==10) {return;} realNormalizedLegendreInterLoopSubSub_<10, L>(aX, rDest);
    if (L-1==11) {return;} realNormalizedLegendreInterLoopSubSub_<11, L>(aX, rDest);
    if (L-1==12) {return;} realNormalizedLegendreInterLoopSubSub_<12, L>(aX, rDest);
}
template <jint L>
static inline void realNormalizedLegendreInterLoop_(jdouble aX, jdouble aY, jdouble *rDest, jdouble &rPll) noexcept {
    realNormalizedLegendreInterLoopSub_<L>(aX, rDest);
    constexpr jdouble tMul1 = SQRT_2LM1P3[L];
    const jdouble tPlm = tMul1 * aX * rPll;
    rDest[L*L+L + (L-1)] = tPlm;
    rDest[L*L+L - (L-1)] = tPlm;
    constexpr jdouble tMul2 = -SQRT_1P1D2L[L];
    rPll *= tMul2 * aY;
    rDest[L*L+L + L] = rPll;
    rDest[L*L+L - L] = rPll;
}
template <jint LMAX>
static inline void realNormalizedLegendreFull(jdouble aX, jdouble aY, jdouble *rDest) noexcept {
    jdouble tPll = 0.28209479177387814347403972578039; // = sqrt(1/(4*PI))
    rDest[0] = tPll;
    if (LMAX == 0) return;
    rDest[2] = SQRT3 * aX * tPll;
    tPll *= (-SQRT3DIV2 * aY);
    rDest[2+1] = tPll;
    rDest[2-1] = tPll;
    if (LMAX == 1) return;
    realNormalizedLegendreInterLoop_<2>(aX, aY, rDest, tPll); if (LMAX == 2) return;
    realNormalizedLegendreInterLoop_<3>(aX, aY, rDest, tPll); if (LMAX == 3) return;
    realNormalizedLegendreInterLoop_<4>(aX, aY, rDest, tPll); if (LMAX == 4) return;
    realNormalizedLegendreInterLoop_<5>(aX, aY, rDest, tPll); if (LMAX == 5) return;
    realNormalizedLegendreInterLoop_<6>(aX, aY, rDest, tPll); if (LMAX == 6) return;
    realNormalizedLegendreInterLoop_<7>(aX, aY, rDest, tPll); if (LMAX == 7) return;
    realNormalizedLegendreInterLoop_<8>(aX, aY, rDest, tPll); if (LMAX == 8) return;
    realNormalizedLegendreInterLoop_<9>(aX, aY, rDest, tPll); if (LMAX == 9) return;
    realNormalizedLegendreInterLoop_<10>(aX, aY, rDest, tPll); if (LMAX == 10) return;
    realNormalizedLegendreInterLoop_<11>(aX, aY, rDest, tPll); if (LMAX == 11) return;
    realNormalizedLegendreInterLoop_<12>(aX, aY, rDest, tPll);
}

template <jint M, jint L>
static inline void realSphericalHarmonicsFull4InterLoopSubSub_(jdouble aSqrt2CosMPhi, jdouble aSqrt2SinMPhi, jdouble *rDest) noexcept {
    rDest[L*L+L + M] *= aSqrt2CosMPhi;
    rDest[L*L+L - M] *= aSqrt2SinMPhi;
}
template <jint M, jint LMAX>
static inline void realSphericalHarmonicsFull4InterLoopSub_(jdouble aSqrt2CosMPhi, jdouble aSqrt2SinMPhi, jdouble *rDest) noexcept {
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+0>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+0) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+1>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+1) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+2>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+2) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+3>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+3) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+4>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+4) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+5>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+5) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+6>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+6) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+7>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+7) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+8>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+8) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+9>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+9) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+10>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+10) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+11>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest); if (LMAX==M+11) return;
    realSphericalHarmonicsFull4InterLoopSubSub_<M, M+12>(aSqrt2CosMPhi, aSqrt2SinMPhi, rDest);
}
template <jint M, jint LMAX>
static inline void realSphericalHarmonicsFull4InterLoop_(jdouble aCosPhi2, jdouble &rSinMPhi, jdouble &rSinMmmPhi, jdouble &rCosMPhi, jdouble &rCosMmmPhi, jdouble *rDest) noexcept {
    const jdouble fSqrt2CosMPhi = SQRT2*rCosMPhi;
    const jdouble fSqrt2SinMPhi = SQRT2*rSinMPhi;
    realSphericalHarmonicsFull4InterLoopSub_<M, LMAX>(fSqrt2CosMPhi, fSqrt2SinMPhi, rDest);
    const jdouble tSinMppPhi = aCosPhi2 * rSinMPhi - rSinMmmPhi;
    const jdouble tCosMppPhi = aCosPhi2 * rCosMPhi - rCosMmmPhi;
    rSinMmmPhi = rSinMPhi; rCosMmmPhi = rCosMPhi;
    rSinMPhi = tSinMppPhi; rCosMPhi = tCosMppPhi;
}
template <jint LMAX>
static inline void realSphericalHarmonicsFull4(jdouble aX, jdouble aY, jdouble aZ, jdouble aDis, jdouble *rDest) noexcept {
    const jdouble tXY = hypot(aX, aY);
    const jdouble tCosTheta = aZ / aDis;
    const jdouble tSinTheta = tXY / aDis;
    jdouble tCosPhi;
    jdouble tSinPhi;
    // avoid nan
    if (numericEqual(tXY, 0.0)) {
        tCosPhi = 1.0;
        tSinPhi = 0.0;
    } else {
        tCosPhi = aX / tXY;
        tSinPhi = aY / tXY;
    }
    // cal real Legendre
    realNormalizedLegendreFull<LMAX>(tCosTheta, tSinTheta, rDest);
    if (LMAX == 0) return;
    // cal sinMPhi & conMPhi
    jdouble rSinMmmPhi = 0.0;
    jdouble rCosMmmPhi = 1.0;
    jdouble rSinMPhi = tSinPhi;
    jdouble rCosMPhi = tCosPhi;
    const jdouble tCosPhi2 = rCosMPhi+rCosMPhi;
    realSphericalHarmonicsFull4InterLoop_<1, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 1) return;
    realSphericalHarmonicsFull4InterLoop_<2, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 2) return;
    realSphericalHarmonicsFull4InterLoop_<3, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 3) return;
    realSphericalHarmonicsFull4InterLoop_<4, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 4) return;
    realSphericalHarmonicsFull4InterLoop_<5, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 5) return;
    realSphericalHarmonicsFull4InterLoop_<6, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 6) return;
    realSphericalHarmonicsFull4InterLoop_<7, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 7) return;
    realSphericalHarmonicsFull4InterLoop_<8, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 8) return;
    realSphericalHarmonicsFull4InterLoop_<9, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 9) return;
    realSphericalHarmonicsFull4InterLoop_<10, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 10) return;
    realSphericalHarmonicsFull4InterLoop_<11, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest); if (LMAX == 11) return;
    realSphericalHarmonicsFull4InterLoop_<12, LMAX>(tCosPhi2, rSinMPhi, rSinMmmPhi, rCosMPhi, rCosMmmPhi, rDest);
}
static void realSphericalHarmonicsFull4(jint aLMax, jdouble aX, jdouble aY, jdouble aZ, jdouble aDis, jdouble *rDest) noexcept {
    switch (aLMax) {
    case 0: {realSphericalHarmonicsFull4<0>(aX, aY, aZ, aDis, rDest); return;}
    case 1: {realSphericalHarmonicsFull4<1>(aX, aY, aZ, aDis, rDest); return;}
    case 2: {realSphericalHarmonicsFull4<2>(aX, aY, aZ, aDis, rDest); return;}
    case 3: {realSphericalHarmonicsFull4<3>(aX, aY, aZ, aDis, rDest); return;}
    case 4: {realSphericalHarmonicsFull4<4>(aX, aY, aZ, aDis, rDest); return;}
    case 5: {realSphericalHarmonicsFull4<5>(aX, aY, aZ, aDis, rDest); return;}
    case 6: {realSphericalHarmonicsFull4<6>(aX, aY, aZ, aDis, rDest); return;}
    case 7: {realSphericalHarmonicsFull4<7>(aX, aY, aZ, aDis, rDest); return;}
    case 8: {realSphericalHarmonicsFull4<8>(aX, aY, aZ, aDis, rDest); return;}
    case 9: {realSphericalHarmonicsFull4<9>(aX, aY, aZ, aDis, rDest); return;}
    case 10: {realSphericalHarmonicsFull4<10>(aX, aY, aZ, aDis, rDest); return;}
    case 11: {realSphericalHarmonicsFull4<11>(aX, aY, aZ, aDis, rDest); return;}
    case 12: {realSphericalHarmonicsFull4<12>(aX, aY, aZ, aDis, rDest); return;}
    default: {return;}
    }
}

template <jint L>
static inline void calYPphi(jdouble *rYPphi, jdouble *aY) noexcept {
    constexpr jint tStart = L*L;
    constexpr jint tIdx = tStart+L;
    for (jint m = -L; m <= L; ++m) {
        rYPphi[tIdx+m] = -m * aY[tIdx-m];
    }
}
template <jint L>
static inline void calYPtheta(jdouble aCosPhi, jdouble aSinPhi, jdouble *rYPtheta, jdouble *aY) noexcept {
    switch(L) {
    case 0: {
        rYPtheta[0] = 0.0;
        return;
    }
    case 1: {
        constexpr jdouble tMul = SQRT_LPM_LMM1[2]*SQRT2_INV;
        rYPtheta[1] = -tMul * aSinPhi*aY[2];
        rYPtheta[2] =  tMul * (aCosPhi*aY[3] + aSinPhi*aY[1]);
        rYPtheta[3] = -tMul * aCosPhi*aY[2];
        return;
    }
    default: {
        constexpr jint tStart = L*L;
        constexpr jint tIdx = tStart+L;
        constexpr jdouble tMul = SQRT_LPM_LMM1[tIdx]*SQRT2_INV;
        rYPtheta[tIdx] = tMul * (aCosPhi*aY[tIdx+1] + aSinPhi*aY[tIdx-1]);
        rYPtheta[tIdx+1] = -tMul * aCosPhi*aY[tIdx];
        rYPtheta[tIdx-1] = -tMul * aSinPhi*aY[tIdx];
        for (jint m = 2; m <= L; ++m) {
            const jdouble tMul2 = -0.5*SQRT_LPM_LMM1[tIdx+m];
            rYPtheta[tIdx+m] = tMul2 * (aCosPhi*aY[tIdx+m-1] - aSinPhi*aY[tIdx-m+1]);
            rYPtheta[tIdx-m] = tMul2 * (aCosPhi*aY[tIdx-m+1] + aSinPhi*aY[tIdx+m-1]);
        }
        for (jint m = 1; m < L; ++m) {
            const jdouble tMul2 = 0.5*SQRT_LPM1_LMM[tIdx+m];
            rYPtheta[tIdx+m] += tMul2 * (aCosPhi*aY[tIdx+m+1] + aSinPhi*aY[tIdx-m-1]);
            rYPtheta[tIdx-m] += tMul2 * (aCosPhi*aY[tIdx-m-1] - aSinPhi*aY[tIdx+m+1]);
        }
        return;
    }}
}
template <jint LMAX>
static void calYPphiPtheta(jdouble *rYPphi, jdouble aCosPhi, jdouble aSinPhi, jdouble *rYPtheta, jdouble *aY) noexcept {
    calYPphi<0>(rYPphi, aY); calYPtheta<0>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 0) return;
    calYPphi<1>(rYPphi, aY); calYPtheta<1>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 1) return;
    calYPphi<2>(rYPphi, aY); calYPtheta<2>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 2) return;
    calYPphi<3>(rYPphi, aY); calYPtheta<3>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 3) return;
    calYPphi<4>(rYPphi, aY); calYPtheta<4>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 4) return;
    calYPphi<5>(rYPphi, aY); calYPtheta<5>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 5) return;
    calYPphi<6>(rYPphi, aY); calYPtheta<6>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 6) return;
    calYPphi<7>(rYPphi, aY); calYPtheta<7>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 7) return;
    calYPphi<8>(rYPphi, aY); calYPtheta<8>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 8) return;
    calYPphi<9>(rYPphi, aY); calYPtheta<9>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 9) return;
    calYPphi<10>(rYPphi, aY); calYPtheta<10>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 10) return;
    calYPphi<11>(rYPphi, aY); calYPtheta<11>(aCosPhi, aSinPhi, rYPtheta, aY); if (LMAX == 11) return;
    calYPphi<12>(rYPphi, aY); calYPtheta<12>(aCosPhi, aSinPhi, rYPtheta, aY);
}
template <jint N>
static inline void convertYPPhiPtheta2YPxyz(jdouble aCosTheta, jdouble aSinTheta, jdouble aCosPhi, jdouble aSinPhi, jdouble aDis, jdouble aDxy, jboolean aDxyCloseZero,
                                            jdouble *rYPx, jdouble *rYPy, jdouble *rYPz, jdouble *aYPtheta, jdouble *aYPphi) noexcept {
    const jdouble thetaPx = -aCosTheta * aCosPhi / aDis;
    const jdouble thetaPy = -aCosTheta * aSinPhi / aDis;
    const jdouble thetaPz =  aSinTheta / aDis;
    const jdouble phiPx = aDxyCloseZero ? 0.0 :  aSinPhi / aDxy;
    const jdouble phiPy = aDxyCloseZero ? 0.0 : -aCosPhi / aDxy;
    for (jint i = 0; i < N; ++i) {
        const jdouble tYPtheta = aYPtheta[i];
        const jdouble tYPphi = aYPphi[i];
        rYPx[i] = tYPtheta*thetaPx + tYPphi*phiPx;
        rYPy[i] = tYPtheta*thetaPy + tYPphi*phiPy;
        rYPz[i] = tYPtheta*thetaPz;
    }
}
template <jint LMAX>
static void calYPxyz(jdouble *aY, jdouble aDx, jdouble aDy, jdouble aDz, jdouble aDis,
                     jdouble *rYPx, jdouble *rYPy, jdouble *rYPz, jdouble *rYPtheta, jdouble *rYPphi) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    const jdouble dxy = hypot(aDx, aDy);
    const jdouble cosTheta = aDz / aDis;
    const jdouble sinTheta = dxy / aDis;
    jdouble cosPhi;
    jdouble sinPhi;
    const jboolean dxyCloseZero = numericEqual(dxy, 0.0);
    if (dxyCloseZero) {
        cosPhi = 1.0;
        sinPhi = 0.0;
    } else {
        cosPhi = aDx / dxy;
        sinPhi = aDy / dxy;
    }
    calYPphiPtheta<LMAX>(rYPphi, cosPhi, sinPhi, rYPtheta, aY);
    if (dxyCloseZero) {
        // fix singularity
        for (jint k = 0; k < tLMAll; ++k) rYPphi[k] = 0.0;
    }
    // conert to Pxyz
    convertYPPhiPtheta2YPxyz<tLMAll>(cosTheta, sinTheta, cosPhi, sinPhi, aDis, dxy, dxyCloseZero,
                                     rYPx, rYPy, rYPz, rYPtheta, rYPphi);
}
static void calYPxyz(jint aLMax, jdouble *aY, jdouble aDx, jdouble aDy, jdouble aDz, jdouble aDis,
                     jdouble *rYPx, jdouble *rYPy, jdouble *rYPz, jdouble *rYPtheta, jdouble *rYPphi) noexcept {
    switch (aLMax) {
    case 0: {calYPxyz<0>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 1: {calYPxyz<1>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 2: {calYPxyz<2>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 3: {calYPxyz<3>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 4: {calYPxyz<4>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 5: {calYPxyz<5>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 6: {calYPxyz<6>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 7: {calYPxyz<7>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 8: {calYPxyz<8>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 9: {calYPxyz<9>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 10: {calYPxyz<10>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 11: {calYPxyz<11>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    case 12: {calYPxyz<12>(aY, aDx, aDy, aDz, aDis, rYPx, rYPy, rYPz, rYPtheta, rYPphi); return;}
    default: {return;}
    }
}



template <jint NMAX, jint LMAX, jboolean MPLUS, jboolean WT>
static void setormplusCnlm_(jdouble *rCnlm, jdouble *rCnlmWt, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    jdouble *tCnlm = rCnlm;
    jdouble *tCnlmWt = rCnlmWt;
    for (jint n = 0; n <= NMAX; ++n) {
        const jdouble tMul = aFc*aRn[n];
        for (jint k = 0; k < tLMAll; ++k) {
            const jdouble tValue = tMul*aY[k];
            if (MPLUS) {
                tCnlm[k] += tValue;
                if (WT) tCnlmWt[k] += aWt*tValue;
            } else {
                tCnlm[k] = tValue;
                if (WT) tCnlmWt[k] = aWt*tValue;
            }
        }
        tCnlm += tLMAll;
        if (WT) tCnlmWt += tLMAll;
    }
}
template <jint LMAX, jboolean MPLUS, jboolean WT>
static void setormplusCnlm_(jdouble *rCnlm, jdouble *rCnlmWt, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax) noexcept {
    switch (aNMax) {
    case 0: {setormplusCnlm_<0, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 1: {setormplusCnlm_<1, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 2: {setormplusCnlm_<2, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 3: {setormplusCnlm_<3, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 4: {setormplusCnlm_<4, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 5: {setormplusCnlm_<5, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 6: {setormplusCnlm_<6, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 7: {setormplusCnlm_<7, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 8: {setormplusCnlm_<8, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 9: {setormplusCnlm_<9, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 10: {setormplusCnlm_<10, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 11: {setormplusCnlm_<11, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 12: {setormplusCnlm_<12, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 13: {setormplusCnlm_<13, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 14: {setormplusCnlm_<14, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 15: {setormplusCnlm_<15, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 16: {setormplusCnlm_<16, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 17: {setormplusCnlm_<17, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 18: {setormplusCnlm_<18, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 19: {setormplusCnlm_<19, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    case 20: {setormplusCnlm_<20, LMAX, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt); return;}
    default: {return;}
    }
}
template <jboolean MPLUS, jboolean WT>
static void setormplusCnlm_(jdouble *rCnlm, jdouble *rCnlmWt, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {setormplusCnlm_<0, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 1: {setormplusCnlm_<1, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 2: {setormplusCnlm_<2, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 3: {setormplusCnlm_<3, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 4: {setormplusCnlm_<4, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 5: {setormplusCnlm_<5, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 6: {setormplusCnlm_<6, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 7: {setormplusCnlm_<7, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 8: {setormplusCnlm_<8, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 9: {setormplusCnlm_<9, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 10: {setormplusCnlm_<10, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 11: {setormplusCnlm_<11, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    case 12: {setormplusCnlm_<12, MPLUS, WT>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax); return;}
    default: {return;}
    }
}
static inline void calBnlm(jdouble *rBnlm, jdouble *aY, jdouble aFc, jdouble *aRn, jint aNMax, jint aLMax) noexcept {
    setormplusCnlm_<JNI_FALSE, JNI_FALSE>(rBnlm, NULL, aY, aFc, aRn, 0, aNMax, aLMax);
}
static inline void mplusCnlm(jdouble *rCnlm, jdouble *aY, jdouble aFc, jdouble *aRn, jint aNMax, jint aLMax) noexcept {
    setormplusCnlm_<JNI_TRUE, JNI_FALSE>(rCnlm, NULL, aY, aFc, aRn, 0, aNMax, aLMax);
}
static inline void mplusCnlmWt(jdouble *rCnlm, jdouble *rCnlmWt, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jint aLMax) noexcept {
    setormplusCnlm_<JNI_TRUE, JNI_TRUE>(rCnlm, rCnlmWt, aY, aFc, aRn, aWt, aNMax, aLMax);
}

template <jint LMAX>
static void mplusLM(jdouble *rAlm, jdouble *aMul, jdouble *aBlm) {
    jdouble *tAlm = rAlm, *tBlm = aBlm;
    mplus<2*0+1>(tAlm, aMul[0], tBlm); if (LMAX==0) {return;} tAlm += (2*0+1); tBlm += (2*0+1);
    mplus<2*1+1>(tAlm, aMul[1], tBlm); if (LMAX==1) {return;} tAlm += (2*1+1); tBlm += (2*1+1);
    mplus<2*2+1>(tAlm, aMul[2], tBlm); if (LMAX==2) {return;} tAlm += (2*2+1); tBlm += (2*2+1);
    mplus<2*3+1>(tAlm, aMul[3], tBlm); if (LMAX==3) {return;} tAlm += (2*3+1); tBlm += (2*3+1);
    mplus<2*4+1>(tAlm, aMul[4], tBlm); if (LMAX==4) {return;} tAlm += (2*4+1); tBlm += (2*4+1);
    mplus<2*5+1>(tAlm, aMul[5], tBlm); if (LMAX==5) {return;} tAlm += (2*5+1); tBlm += (2*5+1);
    mplus<2*6+1>(tAlm, aMul[6], tBlm); if (LMAX==6) {return;} tAlm += (2*6+1); tBlm += (2*6+1);
    mplus<2*7+1>(tAlm, aMul[7], tBlm); if (LMAX==7) {return;} tAlm += (2*7+1); tBlm += (2*7+1);
    mplus<2*8+1>(tAlm, aMul[8], tBlm); if (LMAX==8) {return;} tAlm += (2*8+1); tBlm += (2*8+1);
    mplus<2*9+1>(tAlm, aMul[9], tBlm); if (LMAX==9) {return;} tAlm += (2*9+1); tBlm += (2*9+1);
    mplus<2*10+1>(tAlm, aMul[10], tBlm); if (LMAX==10) {return;} tAlm += (2*10+1); tBlm += (2*10+1);
    mplus<2*11+1>(tAlm, aMul[11], tBlm); if (LMAX==11) {return;} tAlm += (2*11+1); tBlm += (2*11+1);
    mplus<2*12+1>(tAlm, aMul[12], tBlm); if (LMAX==12) {return;} tAlm += (2*12+1); tBlm += (2*12+1);
}
template <jint LMAX>
static void dotLM(jdouble *rDot, jdouble *aAlm, jdouble *aBlm) {
    jdouble *tAlm = aAlm, *tBlm = aBlm;
    rDot[0] += dot<2*0+1>(tAlm, tBlm); if (LMAX==0) {return;} tAlm += (2*0+1); tBlm += (2*0+1);
    rDot[1] += dot<2*1+1>(tAlm, tBlm); if (LMAX==1) {return;} tAlm += (2*1+1); tBlm += (2*1+1);
    rDot[2] += dot<2*2+1>(tAlm, tBlm); if (LMAX==2) {return;} tAlm += (2*2+1); tBlm += (2*2+1);
    rDot[3] += dot<2*3+1>(tAlm, tBlm); if (LMAX==3) {return;} tAlm += (2*3+1); tBlm += (2*3+1);
    rDot[4] += dot<2*4+1>(tAlm, tBlm); if (LMAX==4) {return;} tAlm += (2*4+1); tBlm += (2*4+1);
    rDot[5] += dot<2*5+1>(tAlm, tBlm); if (LMAX==5) {return;} tAlm += (2*5+1); tBlm += (2*5+1);
    rDot[6] += dot<2*6+1>(tAlm, tBlm); if (LMAX==6) {return;} tAlm += (2*6+1); tBlm += (2*6+1);
    rDot[7] += dot<2*7+1>(tAlm, tBlm); if (LMAX==7) {return;} tAlm += (2*7+1); tBlm += (2*7+1);
    rDot[8] += dot<2*8+1>(tAlm, tBlm); if (LMAX==8) {return;} tAlm += (2*8+1); tBlm += (2*8+1);
    rDot[9] += dot<2*9+1>(tAlm, tBlm); if (LMAX==9) {return;} tAlm += (2*9+1); tBlm += (2*9+1);
    rDot[10] += dot<2*10+1>(tAlm, tBlm); if (LMAX==10) {return;} tAlm += (2*10+1); tBlm += (2*10+1);
    rDot[11] += dot<2*11+1>(tAlm, tBlm); if (LMAX==11) {return;} tAlm += (2*11+1); tBlm += (2*11+1);
    rDot[12] += dot<2*12+1>(tAlm, tBlm); if (LMAX==12) {return;} tAlm += (2*12+1); tBlm += (2*12+1);
}

template <jint FSTYLE, jint NMAX, jint LMAX>
static void mplusCnlmFuse_(jboolean aExFlag, jdouble *rCnlm, jdouble *aBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    constexpr jint tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    jdouble *tFuseWeight = aFuseWeight;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tFuseWeight += aFuseSize*(aType-1);
    } else {
        tFuseWeight += aFuseSize*(NMAX+1)*(LMAX+1)*(aType-1);
    }
    jdouble *tCnlm = rCnlm;
    if (aExFlag) {
        mplus<tSizeBnlm>(tCnlm, 1.0, aBnlm);
        tCnlm += tSizeBnlm;
    }
    for (jint k = 0; k < aFuseSize; ++k) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            mplus<tSizeBnlm>(tCnlm, tFuseWeight[k], aBnlm);
            tCnlm += tSizeBnlm;
        } else {
            jdouble *tBnlm = aBnlm;
            for (jint n = 0; n <= NMAX; ++n) {
                mplusLM<LMAX>(tCnlm, tFuseWeight, tBnlm);
                tFuseWeight += (LMAX+1);
                tCnlm += tLMAll;
                tBnlm += tLMAll;
            }
        }
    }
}
template <jint FSTYLE, jint LMAX>
static void mplusCnlmFuse_(jboolean aExFlag, jdouble *rCnlm, jdouble *aBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax) noexcept {
    switch (aNMax) {
    case 0: {mplusCnlmFuse_<FSTYLE, 0, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 1: {mplusCnlmFuse_<FSTYLE, 1, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 2: {mplusCnlmFuse_<FSTYLE, 2, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 3: {mplusCnlmFuse_<FSTYLE, 3, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 4: {mplusCnlmFuse_<FSTYLE, 4, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 5: {mplusCnlmFuse_<FSTYLE, 5, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 6: {mplusCnlmFuse_<FSTYLE, 6, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 7: {mplusCnlmFuse_<FSTYLE, 7, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 8: {mplusCnlmFuse_<FSTYLE, 8, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 9: {mplusCnlmFuse_<FSTYLE, 9, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 10: {mplusCnlmFuse_<FSTYLE, 10, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 11: {mplusCnlmFuse_<FSTYLE, 11, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 12: {mplusCnlmFuse_<FSTYLE, 12, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 13: {mplusCnlmFuse_<FSTYLE, 13, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 14: {mplusCnlmFuse_<FSTYLE, 14, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 15: {mplusCnlmFuse_<FSTYLE, 15, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 16: {mplusCnlmFuse_<FSTYLE, 16, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 17: {mplusCnlmFuse_<FSTYLE, 17, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 18: {mplusCnlmFuse_<FSTYLE, 18, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 19: {mplusCnlmFuse_<FSTYLE, 19, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 20: {mplusCnlmFuse_<FSTYLE, 20, LMAX>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize); return;}
    default: {return;}
    }
}
template <jint FSTYLE>
static void mplusCnlmFuse_(jboolean aExFlag, jdouble *rCnlm, jdouble *aBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {mplusCnlmFuse_<FSTYLE, 0>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 1: {mplusCnlmFuse_<FSTYLE, 1>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 2: {mplusCnlmFuse_<FSTYLE, 2>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 3: {mplusCnlmFuse_<FSTYLE, 3>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 4: {mplusCnlmFuse_<FSTYLE, 4>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 5: {mplusCnlmFuse_<FSTYLE, 5>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 6: {mplusCnlmFuse_<FSTYLE, 6>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 7: {mplusCnlmFuse_<FSTYLE, 7>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 8: {mplusCnlmFuse_<FSTYLE, 8>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 9: {mplusCnlmFuse_<FSTYLE, 9>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 10: {mplusCnlmFuse_<FSTYLE, 10>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 11: {mplusCnlmFuse_<FSTYLE, 11>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 12: {mplusCnlmFuse_<FSTYLE, 12>(aExFlag, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    default: {return;}
    }
}
template <jint FSTYLE>
static inline void mplusCnlmFuse(jdouble *rCnlm, jdouble *aBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax, jint aLMax) noexcept {
    mplusCnlmFuse_<FSTYLE>(JNI_FALSE, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax, aLMax);
}
template <jint FSTYLE>
static inline void mplusCnlmExFuse(jdouble *rCnlm, jdouble *aBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax, jint aLMax) noexcept {
    mplusCnlmFuse_<FSTYLE>(JNI_TRUE, rCnlm, aBnlm, aFuseWeight, aType, aFuseSize, aNMax, aLMax);
}


template <jint FSTYLE, jint LMAX>
static void mplusAnlm(jdouble *rAnlm, jdouble *aCnlm, jdouble *aPostFuseWeight, jint aPostFuseSize, jint aSizeN) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    jdouble *tAnlm = rAnlm;
    jdouble *tPostFuseWeight = aPostFuseWeight;
    for (jint np = 0; np < aPostFuseSize; ++np) {
        jdouble *tCnlm = aCnlm;
        for (jint n = 0; n < aSizeN; ++n) {
            if (FSTYLE==FUSE_STYLE_LIMITED) {
                mplus<tLMAll>(tAnlm, tPostFuseWeight[n], tCnlm);
            } else {
                mplusLM<LMAX>(tAnlm, tPostFuseWeight, tCnlm);
                tPostFuseWeight += (LMAX+1);
            }
            tCnlm += tLMAll;
        }
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tPostFuseWeight += aSizeN;
        }
        tAnlm += tLMAll;
    }
}
template <jint FSTYLE>
static void mplusAnlm(jdouble *rAnlm, jdouble *aCnlm, jdouble *aPostFuseWeight, jint aPostFuseSize, jint aSizeN, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {mplusAnlm<FSTYLE, 0>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 1: {mplusAnlm<FSTYLE, 1>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 2: {mplusAnlm<FSTYLE, 2>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 3: {mplusAnlm<FSTYLE, 3>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 4: {mplusAnlm<FSTYLE, 4>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 5: {mplusAnlm<FSTYLE, 5>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 6: {mplusAnlm<FSTYLE, 6>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 7: {mplusAnlm<FSTYLE, 7>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 8: {mplusAnlm<FSTYLE, 8>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 9: {mplusAnlm<FSTYLE, 9>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 10: {mplusAnlm<FSTYLE, 10>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 11: {mplusAnlm<FSTYLE, 11>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 12: {mplusAnlm<FSTYLE, 12>(rAnlm, aCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    default: {return;}
    }
}


template <jint FSTYLE, jint NMAX, jint LMAX>
static void mplusGradParaFuse(jdouble *aGradCnlm, jdouble *aBnlm, jdouble *rGradPara, jint aType, jint aFuseSize) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    constexpr jint tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    jdouble *tGradPara = rGradPara;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tGradPara += aFuseSize*(aType-1);
    } else {
        tGradPara += aFuseSize*(NMAX+1)*(LMAX+1)*(aType-1);
    }
    jdouble *tGradCnlm = aGradCnlm;
    for (jint k = 0; k < aFuseSize; ++k) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tGradPara[k] += dot<tSizeBnlm>(aBnlm, tGradCnlm);
            tGradCnlm += tSizeBnlm;
        } else {
            jdouble *tBnlm = aBnlm;
            for (jint n = 0; n <= NMAX; ++n) {
                dotLM<LMAX>(tGradPara, tBnlm, tGradCnlm);
                tGradPara += (LMAX+1);
                tBnlm += tLMAll;
                tGradCnlm += tLMAll;
            }
        }
    }
}
template <jint FSTYLE, jint LMAX>
static void mplusGradParaFuse(jdouble *aGradCnlm, jdouble *aBnlm, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax) noexcept {
    switch (aNMax) {
    case 0: {mplusGradParaFuse<FSTYLE, 0, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 1: {mplusGradParaFuse<FSTYLE, 1, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 2: {mplusGradParaFuse<FSTYLE, 2, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 3: {mplusGradParaFuse<FSTYLE, 3, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 4: {mplusGradParaFuse<FSTYLE, 4, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 5: {mplusGradParaFuse<FSTYLE, 5, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 6: {mplusGradParaFuse<FSTYLE, 6, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 7: {mplusGradParaFuse<FSTYLE, 7, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 8: {mplusGradParaFuse<FSTYLE, 8, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 9: {mplusGradParaFuse<FSTYLE, 9, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 10: {mplusGradParaFuse<FSTYLE, 10, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 11: {mplusGradParaFuse<FSTYLE, 11, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 12: {mplusGradParaFuse<FSTYLE, 12, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 13: {mplusGradParaFuse<FSTYLE, 13, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 14: {mplusGradParaFuse<FSTYLE, 14, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 15: {mplusGradParaFuse<FSTYLE, 15, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 16: {mplusGradParaFuse<FSTYLE, 16, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 17: {mplusGradParaFuse<FSTYLE, 17, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 18: {mplusGradParaFuse<FSTYLE, 18, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 19: {mplusGradParaFuse<FSTYLE, 19, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    case 20: {mplusGradParaFuse<FSTYLE, 20, LMAX>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize); return;}
    default: {return;}
    }
}
template <jint FSTYLE>
static void mplusGradParaFuse(jdouble *aGradCnlm, jdouble *aBnlm, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {mplusGradParaFuse<FSTYLE, 0>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 1: {mplusGradParaFuse<FSTYLE, 1>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 2: {mplusGradParaFuse<FSTYLE, 2>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 3: {mplusGradParaFuse<FSTYLE, 3>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 4: {mplusGradParaFuse<FSTYLE, 4>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 5: {mplusGradParaFuse<FSTYLE, 5>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 6: {mplusGradParaFuse<FSTYLE, 6>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 7: {mplusGradParaFuse<FSTYLE, 7>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 8: {mplusGradParaFuse<FSTYLE, 8>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 9: {mplusGradParaFuse<FSTYLE, 9>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 10: {mplusGradParaFuse<FSTYLE, 10>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 11: {mplusGradParaFuse<FSTYLE, 11>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    case 12: {mplusGradParaFuse<FSTYLE, 12>(aGradCnlm, aBnlm, rGradPara, aType, aFuseSize, aNMax); return;}
    default: {return;}
    }
}

template <jint FSTYLE, jint LMAX>
static void mplusGradParaPostFuse(jdouble *aGradAnlm, jdouble *aCnlm, jdouble *rGradPara, jint aPostFuseSize, jint aSizeN) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    jdouble *tGradAnlm = aGradAnlm;
    jdouble *tGradPara = rGradPara;
    for (jint np = 0; np < aPostFuseSize; ++np) {
        jdouble *tCnlm = aCnlm;
        for (jint n = 0; n < aSizeN; ++n) {
            if (FSTYLE==FUSE_STYLE_LIMITED) {
                tGradPara[n] += dot<tLMAll>(tGradAnlm, tCnlm);
            } else {
                dotLM<LMAX>(tGradPara, tGradAnlm, tCnlm);
                tGradPara += (LMAX+1);
            }
            tCnlm += tLMAll;
        }
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tGradPara += aSizeN;
        }
        tGradAnlm += tLMAll;
    }
}
template <jint FSTYLE>
static void mplusGradParaPostFuse(jdouble *aGradAnlm, jdouble *aCnlm, jdouble *rGradPara, jint aPostFuseSize, jint aSizeN, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {mplusGradParaPostFuse<FSTYLE, 0>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 1: {mplusGradParaPostFuse<FSTYLE, 1>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 2: {mplusGradParaPostFuse<FSTYLE, 2>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 3: {mplusGradParaPostFuse<FSTYLE, 3>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 4: {mplusGradParaPostFuse<FSTYLE, 4>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 5: {mplusGradParaPostFuse<FSTYLE, 5>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 6: {mplusGradParaPostFuse<FSTYLE, 6>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 7: {mplusGradParaPostFuse<FSTYLE, 7>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 8: {mplusGradParaPostFuse<FSTYLE, 8>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 9: {mplusGradParaPostFuse<FSTYLE, 9>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 10: {mplusGradParaPostFuse<FSTYLE, 10>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 11: {mplusGradParaPostFuse<FSTYLE, 11>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    case 12: {mplusGradParaPostFuse<FSTYLE, 12>(aGradAnlm, aCnlm, rGradPara, aPostFuseSize, aSizeN); return;}
    default: {return;}
    }
}

template <jint FSTYLE, jint LMAX>
static void mplusGradAnlm(jdouble *aGradAnlm, jdouble *rGradCnlm, jdouble *aPostFuseWeight, jint aPostFuseSize, jint aSizeN) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    jdouble *tGradCnlm = rGradCnlm;
    for (jint n = 0; n < aSizeN; ++n) {
        jdouble *tGradAnlm = aGradAnlm;
        for (jint np = 0; np < aPostFuseSize; ++np) {
            if (FSTYLE==FUSE_STYLE_LIMITED) {
                mplus<tLMAll>(tGradCnlm, aPostFuseWeight[n + np*aSizeN], tGradAnlm);
            } else {
                const jint tShift = (n + np*aSizeN)*(LMAX+1);
                mplusLM<LMAX>(tGradCnlm, aPostFuseWeight+tShift, tGradAnlm);
            }
            tGradAnlm += tLMAll;
        }
        tGradCnlm += tLMAll;
    }
}
template <jint FSTYLE>
static void mplusGradAnlm(jdouble *aGradAnlm, jdouble *rGradCnlm, jdouble *aPostFuseWeight, jint aPostFuseSize, jint aSizeN, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {mplusGradAnlm<FSTYLE, 0>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 1: {mplusGradAnlm<FSTYLE, 1>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 2: {mplusGradAnlm<FSTYLE, 2>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 3: {mplusGradAnlm<FSTYLE, 3>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 4: {mplusGradAnlm<FSTYLE, 4>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 5: {mplusGradAnlm<FSTYLE, 5>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 6: {mplusGradAnlm<FSTYLE, 6>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 7: {mplusGradAnlm<FSTYLE, 7>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 8: {mplusGradAnlm<FSTYLE, 8>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 9: {mplusGradAnlm<FSTYLE, 9>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 10: {mplusGradAnlm<FSTYLE, 10>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 11: {mplusGradAnlm<FSTYLE, 11>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    case 12: {mplusGradAnlm<FSTYLE, 12>(aGradAnlm, rGradCnlm, aPostFuseWeight, aPostFuseSize, aSizeN); return;}
    default: {return;}
    }
}


template <jint FSTYLE, jint NMAX, jint LMAX>
static void calGradBnlmFuse_(jboolean aExFlag, jdouble *aGradCnlm, jdouble *rGradBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    constexpr jint tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    fill<tSizeBnlm>(rGradBnlm, 0.0);
    jdouble *tFuseWeight = aFuseWeight;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tFuseWeight += aFuseSize*(aType-1);
    } else {
        tFuseWeight += aFuseSize*(NMAX+1)*(LMAX+1)*(aType-1);
    }
    jdouble *tGradCnlm = aGradCnlm;
    if (aExFlag) {
        mplus<tSizeBnlm>(rGradBnlm, 1.0, tGradCnlm);
        tGradCnlm += tSizeBnlm;
    }
    for (jint k = 0; k < aFuseSize; ++k) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            mplus<tSizeBnlm>(rGradBnlm, tFuseWeight[k], tGradCnlm);
            tGradCnlm += tSizeBnlm;
        } else {
            jdouble *tGradBnlm = rGradBnlm;
            for (jint n = 0; n <= NMAX; ++n) {
                mplusLM<LMAX>(tGradBnlm, tFuseWeight, tGradCnlm);
                tFuseWeight += (LMAX+1);
                tGradBnlm += tLMAll;
                tGradCnlm += tLMAll;
            }
        }
    }
}
template <jint FSTYLE, jint LMAX>
static void calGradBnlmFuse_(jboolean aExFlag, jdouble *aGradCnlm, jdouble *rGradBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax) noexcept {
    switch (aNMax) {
    case 0: {calGradBnlmFuse_<FSTYLE, 0, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 1: {calGradBnlmFuse_<FSTYLE, 1, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 2: {calGradBnlmFuse_<FSTYLE, 2, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 3: {calGradBnlmFuse_<FSTYLE, 3, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 4: {calGradBnlmFuse_<FSTYLE, 4, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 5: {calGradBnlmFuse_<FSTYLE, 5, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 6: {calGradBnlmFuse_<FSTYLE, 6, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 7: {calGradBnlmFuse_<FSTYLE, 7, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 8: {calGradBnlmFuse_<FSTYLE, 8, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 9: {calGradBnlmFuse_<FSTYLE, 9, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 10: {calGradBnlmFuse_<FSTYLE, 10, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 11: {calGradBnlmFuse_<FSTYLE, 11, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 12: {calGradBnlmFuse_<FSTYLE, 12, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 13: {calGradBnlmFuse_<FSTYLE, 13, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 14: {calGradBnlmFuse_<FSTYLE, 14, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 15: {calGradBnlmFuse_<FSTYLE, 15, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 16: {calGradBnlmFuse_<FSTYLE, 16, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 17: {calGradBnlmFuse_<FSTYLE, 17, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 18: {calGradBnlmFuse_<FSTYLE, 18, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 19: {calGradBnlmFuse_<FSTYLE, 19, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    case 20: {calGradBnlmFuse_<FSTYLE, 20, LMAX>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize); return;}
    default: {return;}
    }
}
template <jint FSTYLE>
static void calGradBnlmFuse_(jboolean aExFlag, jdouble *aGradCnlm, jdouble *rGradBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {calGradBnlmFuse_<FSTYLE, 0>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 1: {calGradBnlmFuse_<FSTYLE, 1>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 2: {calGradBnlmFuse_<FSTYLE, 2>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 3: {calGradBnlmFuse_<FSTYLE, 3>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 4: {calGradBnlmFuse_<FSTYLE, 4>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 5: {calGradBnlmFuse_<FSTYLE, 5>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 6: {calGradBnlmFuse_<FSTYLE, 6>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 7: {calGradBnlmFuse_<FSTYLE, 7>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 8: {calGradBnlmFuse_<FSTYLE, 8>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 9: {calGradBnlmFuse_<FSTYLE, 9>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 10: {calGradBnlmFuse_<FSTYLE, 10>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 11: {calGradBnlmFuse_<FSTYLE, 11>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    case 12: {calGradBnlmFuse_<FSTYLE, 12>(aExFlag, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax); return;}
    default: {return;}
    }
}
template <jint FSTYLE>
static inline void calGradBnlmFuse(jdouble *aGradCnlm, jdouble *rGradBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax, jint aLMax) noexcept {
    calGradBnlmFuse_<FSTYLE>(JNI_FALSE, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax, aLMax);
}
template <jint FSTYLE>
static inline void calGradBnlmExFuse(jdouble *aGradCnlm, jdouble *rGradBnlm, jdouble *aFuseWeight, jint aType, jint aFuseSize, jint aNMax, jint aLMax) noexcept {
    calGradBnlmFuse_<FSTYLE>(JNI_TRUE, aGradCnlm, rGradBnlm, aFuseWeight, aType, aFuseSize, aNMax, aLMax);
}

template <jint LMAX, jboolean WT>
static void gradCnlm2Fxyz_(jint j, jdouble *aGradCnlm, jdouble *aGradCnlmWt, jdouble *rGradY,
                           jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax,
                           jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                           jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                           jdouble *aYPx, jdouble *aYPy, jdouble *aYPz,
                           jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    // clear gradY here
    fill<tLMAll>(rGradY, 0.0);
    jdouble tGradFc = 0.0;
    jdouble rFxj = 0.0, rFyj = 0.0, rFzj = 0.0;
    jdouble *tGradCnlm = aGradCnlm;
    jdouble *tGradCnlmWt = aGradCnlmWt;
    for (jint n = 0; n <= aNMax; ++n) {
        const jdouble tRnn = aRn[n];
        const jdouble tMul = aFc * tRnn;
        jdouble tGradRn = 0.0;
        for (jint k = 0; k < tLMAll; ++k) {
            jdouble subGradBnlm = tGradCnlm[k];
            if (WT) subGradBnlm += aWt*tGradCnlmWt[k];
            rGradY[k] += tMul * subGradBnlm;
            tGradRn += aY[k] * subGradBnlm;
        }
        tGradCnlm += tLMAll;
        if (WT) tGradCnlmWt += tLMAll;
        
        tGradFc += tRnn * tGradRn;
        tGradRn *= aFc;
        rFxj += tGradRn*aRnPx[n];
        rFyj += tGradRn*aRnPy[n];
        rFzj += tGradRn*aRnPz[n];
    }
    for (jint k = 0; k < tLMAll; ++k) {
        const jdouble subGradY = rGradY[k];
        rFxj += subGradY*aYPx[k];
        rFyj += subGradY*aYPy[k];
        rFzj += subGradY*aYPz[k];
    }
    rFxj += aFcPx*tGradFc;
    rFyj += aFcPy*tGradFc;
    rFzj += aFcPz*tGradFc;
    rFx[j] += rFxj; rFy[j] += rFyj; rFz[j] += rFzj;
}
template <jboolean WT>
static void gradCnlm2Fxyz_(jint j, jdouble *aGradCnlm, jdouble *aGradCnlmWt, jdouble *rGradY,
                           jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jint aLMax,
                           jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz,
                           jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    switch (aLMax) {
    case 0: {gradCnlm2Fxyz_<0, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 1: {gradCnlm2Fxyz_<1, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 2: {gradCnlm2Fxyz_<2, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 3: {gradCnlm2Fxyz_<3, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 4: {gradCnlm2Fxyz_<4, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 5: {gradCnlm2Fxyz_<5, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 6: {gradCnlm2Fxyz_<6, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 7: {gradCnlm2Fxyz_<7, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 8: {gradCnlm2Fxyz_<8, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 9: {gradCnlm2Fxyz_<9, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 10: {gradCnlm2Fxyz_<10, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 11: {gradCnlm2Fxyz_<11, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    case 12: {gradCnlm2Fxyz_<12, WT>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz); return;}
    default: {return;}
    }
}
static inline void gradBnlm2Fxyz(jint j, jdouble *aGradBnlm, jdouble *rGradY, jdouble *aY, jdouble aFc, jdouble *aRn, jint aNMax, jint aLMax, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                                 jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    gradCnlm2Fxyz_<JNI_FALSE>(j, aGradBnlm, NULL, rGradY, aY, aFc, aRn, 0, aNMax, aLMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz);
}
static inline void gradCnlmWt2Fxyz(jint j, jdouble *aGradCnlm, jdouble *aGradCnlmWt, jdouble *rGradY, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jint aLMax, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                                   jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rFx, jdouble *rFy, jdouble *rFz) noexcept {
    gradCnlm2Fxyz_<JNI_TRUE>(j, aGradCnlm, aGradCnlmWt, rGradY, aY, aFc, aRn, aWt, aNMax, aLMax, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aYPx, aYPy, aYPz, rFx, rFy, rFz);
}


template <jint FSTYLE, jint NMAX, jint LMAX>
static void mplusGradNNGradCnlmFuse_(jboolean aExFlag, jdouble *aNNGradCnlm, jdouble *rGradNNGradCnlm, jdouble *aGradNNGradBnlm, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jboolean aFixBasis) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    constexpr jint tSizeBnlm = (NMAX+1)*(LMAX+1)*(LMAX+1);
    jdouble *tFuseWeight = aFuseWeight;
    if (FSTYLE==FUSE_STYLE_LIMITED) {
        tFuseWeight += aFuseSize*(aType-1);
    } else {
        tFuseWeight += aFuseSize*(NMAX+1)*(LMAX+1)*(aType-1);
    }
    jdouble *tGradNNGradCnlm = rGradNNGradCnlm;
    if (aExFlag) {
        mplus<tSizeBnlm>(tGradNNGradCnlm, 1.0, aGradNNGradBnlm);
        tGradNNGradCnlm += tSizeBnlm;
    }
    for (jint k = 0; k < aFuseSize; ++k) {
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            mplus<tSizeBnlm>(tGradNNGradCnlm, tFuseWeight[k], aGradNNGradBnlm);
            tGradNNGradCnlm += tSizeBnlm;
        } else {
            jdouble *tGradNNGradBnlm = aGradNNGradBnlm;
            for (jint n = 0; n <= NMAX; ++n) {
                mplusLM<LMAX>(tGradNNGradCnlm, tFuseWeight, tGradNNGradBnlm);
                tFuseWeight += (LMAX+1);
                tGradNNGradBnlm += tLMAll;
                tGradNNGradCnlm += tLMAll;
            }
        }
    }
    if (!aFixBasis) {
        jdouble *tGradPara = rGradPara;
        if (FSTYLE==FUSE_STYLE_LIMITED) {
            tGradPara += aFuseSize*(aType-1);
        } else {
            tGradPara += aFuseSize*(NMAX+1)*(LMAX+1)*(aType-1);
        }
        jdouble *tNNGradCnlm = aNNGradCnlm;
        if (aExFlag) {
            tNNGradCnlm += tSizeBnlm;
        }
        for (jint k = 0; k < aFuseSize; ++k) {
            if (FSTYLE==FUSE_STYLE_LIMITED) {
                tGradPara[k] += dot<tSizeBnlm>(tNNGradCnlm, aGradNNGradBnlm);
                tNNGradCnlm += tSizeBnlm;
            } else {
                jdouble *tGradNNGradBnlm = aGradNNGradBnlm;
                for (jint n = 0; n <= NMAX; ++n) {
                    dotLM<LMAX>(tGradPara, tNNGradCnlm, tGradNNGradBnlm);
                    tGradPara += (LMAX+1);
                    tGradNNGradBnlm += tLMAll;
                    tNNGradCnlm += tLMAll;
                }
            }
        }
    }
}
template <jint FSTYLE, jint LMAX>
static void mplusGradNNGradCnlmFuse_(jboolean aExFlag, jdouble *aNNGradCnlm, jdouble *rGradNNGradCnlm, jdouble *aGradNNGradBnlm, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax, jboolean aFixBasis) noexcept {
    switch (aNMax) {
    case 0: {mplusGradNNGradCnlmFuse_<FSTYLE, 0, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 1: {mplusGradNNGradCnlmFuse_<FSTYLE, 1, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 2: {mplusGradNNGradCnlmFuse_<FSTYLE, 2, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 3: {mplusGradNNGradCnlmFuse_<FSTYLE, 3, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 4: {mplusGradNNGradCnlmFuse_<FSTYLE, 4, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 5: {mplusGradNNGradCnlmFuse_<FSTYLE, 5, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 6: {mplusGradNNGradCnlmFuse_<FSTYLE, 6, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 7: {mplusGradNNGradCnlmFuse_<FSTYLE, 7, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 8: {mplusGradNNGradCnlmFuse_<FSTYLE, 8, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 9: {mplusGradNNGradCnlmFuse_<FSTYLE, 9, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 10: {mplusGradNNGradCnlmFuse_<FSTYLE, 10, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 11: {mplusGradNNGradCnlmFuse_<FSTYLE, 11, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 12: {mplusGradNNGradCnlmFuse_<FSTYLE, 12, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 13: {mplusGradNNGradCnlmFuse_<FSTYLE, 13, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 14: {mplusGradNNGradCnlmFuse_<FSTYLE, 14, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 15: {mplusGradNNGradCnlmFuse_<FSTYLE, 15, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 16: {mplusGradNNGradCnlmFuse_<FSTYLE, 16, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 17: {mplusGradNNGradCnlmFuse_<FSTYLE, 17, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 18: {mplusGradNNGradCnlmFuse_<FSTYLE, 18, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 19: {mplusGradNNGradCnlmFuse_<FSTYLE, 19, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    case 20: {mplusGradNNGradCnlmFuse_<FSTYLE, 20, LMAX>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aFixBasis); return;}
    default: {return;}
    }
}
template <jint FSTYLE>
static void mplusGradNNGradCnlmFuse_(jboolean aExFlag, jdouble *aNNGradCnlm, jdouble *rGradNNGradCnlm, jdouble *aGradNNGradBnlm, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax, jint aLMax, jboolean aFixBasis) noexcept {
    switch (aLMax) {
    case 0: {mplusGradNNGradCnlmFuse_<FSTYLE, 0>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 1: {mplusGradNNGradCnlmFuse_<FSTYLE, 1>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 2: {mplusGradNNGradCnlmFuse_<FSTYLE, 2>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 3: {mplusGradNNGradCnlmFuse_<FSTYLE, 3>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 4: {mplusGradNNGradCnlmFuse_<FSTYLE, 4>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 5: {mplusGradNNGradCnlmFuse_<FSTYLE, 5>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 6: {mplusGradNNGradCnlmFuse_<FSTYLE, 6>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 7: {mplusGradNNGradCnlmFuse_<FSTYLE, 7>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 8: {mplusGradNNGradCnlmFuse_<FSTYLE, 8>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 9: {mplusGradNNGradCnlmFuse_<FSTYLE, 9>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 10: {mplusGradNNGradCnlmFuse_<FSTYLE, 10>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 11: {mplusGradNNGradCnlmFuse_<FSTYLE, 11>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    case 12: {mplusGradNNGradCnlmFuse_<FSTYLE, 12>(aExFlag, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aFixBasis); return;}
    default: {return;}
    }
}
template <jint FSTYLE>
static inline void mplusGradNNGradCnlmFuse(jdouble *aNNGradCnlm, jdouble *rGradNNGradCnlm, jdouble *aGradNNGradBnlm, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax, jint aLMax, jboolean aFixBasis) noexcept {
    mplusGradNNGradCnlmFuse_<FSTYLE>(JNI_FALSE, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aLMax, aFixBasis);
}
template <jint FSTYLE>
static inline void mplusGradNNGradCnlmExFuse(jdouble *aNNGradCnlm, jdouble *rGradNNGradCnlm, jdouble *aGradNNGradBnlm, jdouble *aFuseWeight, jdouble *rGradPara, jint aType, jint aFuseSize, jint aNMax, jint aLMax, jboolean aFixBasis) noexcept {
    mplusGradNNGradCnlmFuse_<FSTYLE>(JNI_TRUE, aNNGradCnlm, rGradNNGradCnlm, aGradNNGradBnlm, aFuseWeight, rGradPara, aType, aFuseSize, aNMax, aLMax, aFixBasis);
}

template <jint LMAX, jboolean MPLUS, jboolean WT>
static void setormplusGradNNGradCnlm_(jdouble *rGradNNGradCnlm, jdouble *rGradNNGradCnlmWt, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax,
                                      jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rGradNNGradY, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                                      jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) noexcept {
    constexpr jint tLMAll = (LMAX+1)*(LMAX+1);
    for (jint k = 0; k < tLMAll; ++k) {
        rGradNNGradY[k] = aYPx[k]*aGradFx + aYPy[k]*aGradFy + aYPz[k]*aGradFz;
    }
    const jdouble tGradNNGradFc = aFcPx*aGradFx + aFcPy*aGradFy + aFcPz*aGradFz;
    jdouble *tGradNNGradCnlm = rGradNNGradCnlm;
    jdouble *tGradNNGradCnlmWt = rGradNNGradCnlmWt;
    for (jint n = 0; n <= aNMax; ++n) {
        jdouble tGradNNGradRn = aRnPx[n]*aGradFx + aRnPy[n]*aGradFy + aRnPz[n]*aGradFz;
        jdouble tRnn = aRn[n];
        tGradNNGradRn = tRnn*tGradNNGradFc + aFc*tGradNNGradRn;
        tRnn *= aFc;
        for (jint k = 0; k < tLMAll; ++k) {
            const jdouble tValue = aY[k]*tGradNNGradRn + tRnn*rGradNNGradY[k];
            if (MPLUS) {
                tGradNNGradCnlm[k] += tValue;
                if (WT) tGradNNGradCnlmWt[k] += aWt*tValue;
            } else {
                tGradNNGradCnlm[k] = tValue;
                if (WT) tGradNNGradCnlmWt[k] = aWt*tValue;
            }
        }
        tGradNNGradCnlm += tLMAll;
        if (WT) tGradNNGradCnlmWt += tLMAll;
    }
}
template <jboolean MPLUS, jboolean WT>
static void setormplusGradNNGradCnlm_(jdouble *rGradNNGradCnlm, jdouble *rGradNNGradCnlmWt, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jint aLMax,
                                      jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rGradNNGradY, jdouble aFcPx, jdouble aFcPy, jdouble aFcPz,
                                      jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) noexcept {
    switch (aLMax) {
    case 0: {setormplusGradNNGradCnlm_<0, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 1: {setormplusGradNNGradCnlm_<1, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 2: {setormplusGradNNGradCnlm_<2, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 3: {setormplusGradNNGradCnlm_<3, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 4: {setormplusGradNNGradCnlm_<4, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 5: {setormplusGradNNGradCnlm_<5, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 6: {setormplusGradNNGradCnlm_<6, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 7: {setormplusGradNNGradCnlm_<7, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 8: {setormplusGradNNGradCnlm_<8, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 9: {setormplusGradNNGradCnlm_<9, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 10: {setormplusGradNNGradCnlm_<10, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 11: {setormplusGradNNGradCnlm_<11, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    case 12: {setormplusGradNNGradCnlm_<12, MPLUS, WT>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz); return;}
    default: {return;}
    }
}
static inline void calGradNNGradBnlm(jdouble *rGradNNGradBnlm, jdouble *aY, jdouble aFc, jdouble *aRn, jint aNMax, jint aLMax, jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rGradNNGradY,
                                     jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) noexcept {
    setormplusGradNNGradCnlm_<JNI_FALSE, JNI_FALSE>(rGradNNGradBnlm, NULL, aY, aFc, aRn, 0, aNMax, aLMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}
static inline void mplusGradNNGradCnlm(jdouble *rGradNNGradCnlm, jdouble *aY, jdouble aFc, jdouble *aRn, jint aNMax, jint aLMax, jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rGradNNGradY,
                                       jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) noexcept {
    setormplusGradNNGradCnlm_<JNI_TRUE, JNI_FALSE>(rGradNNGradCnlm, NULL, aY, aFc, aRn, 0, aNMax, aLMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}
static inline void mplusGradNNGradCnlmWt(jdouble *rGradNNGradCnlm, jdouble *rGradNNGradCnlmWt, jdouble *aY, jdouble aFc, jdouble *aRn, jdouble aWt, jint aNMax, jint aLMax, jdouble *aYPx, jdouble *aYPy, jdouble *aYPz, jdouble *rGradNNGradY,
                                         jdouble aFcPx, jdouble aFcPy, jdouble aFcPz, jdouble *aRnPx, jdouble *aRnPy, jdouble *aRnPz, jdouble aGradFx, jdouble aGradFy, jdouble aGradFz) noexcept {
    setormplusGradNNGradCnlm_<JNI_TRUE, JNI_TRUE>(rGradNNGradCnlm, rGradNNGradCnlmWt, aY, aFc, aRn, aWt, aNMax, aLMax, aYPx, aYPy, aYPz, rGradNNGradY, aFcPx, aFcPy, aFcPz, aRnPx, aRnPy, aRnPz, aGradFx, aGradFy, aGradFz);
}



template <jint L>
static inline void calL2Sub_(jdouble *aCnlm, jdouble *rFp) noexcept {
    constexpr jint tLen = L+L+1;
    const jdouble rDot = dot<tLen>(aCnlm + (L*L));
    rFp[L-1] = (PI4/(jdouble)tLen) * rDot;
}
template <jint LMAX, jboolean NO_RADIAL>
static void calL2_(jdouble *aCnlm, jdouble *rFp) noexcept {
    // l == 0
    jdouble *tFp = rFp;
    if (!NO_RADIAL) {
        const jdouble tCnl0 = aCnlm[0];
        tFp[0] = PI4 * (tCnl0*tCnl0);
        ++tFp;
    }
    if (LMAX == 0) return;
    calL2Sub_<1>(aCnlm, tFp); if (LMAX == 1) return;
    calL2Sub_<2>(aCnlm, tFp); if (LMAX == 2) return;
    calL2Sub_<3>(aCnlm, tFp); if (LMAX == 3) return;
    calL2Sub_<4>(aCnlm, tFp); if (LMAX == 4) return;
    calL2Sub_<5>(aCnlm, tFp); if (LMAX == 5) return;
    calL2Sub_<6>(aCnlm, tFp); if (LMAX == 6) return;
    calL2Sub_<7>(aCnlm, tFp); if (LMAX == 7) return;
    calL2Sub_<8>(aCnlm, tFp); if (LMAX == 8) return;
    calL2Sub_<9>(aCnlm, tFp); if (LMAX == 9) return;
    calL2Sub_<10>(aCnlm, tFp); if (LMAX == 10) return;
    calL2Sub_<11>(aCnlm, tFp); if (LMAX == 11) return;
    calL2Sub_<12>(aCnlm, tFp);
}
template <jboolean NO_RADIAL>
static void calL2_(jdouble *aCnlm, jdouble *rFp, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {calL2_<0, NO_RADIAL>(aCnlm, rFp); return;}
    case 1: {calL2_<1, NO_RADIAL>(aCnlm, rFp); return;}
    case 2: {calL2_<2, NO_RADIAL>(aCnlm, rFp); return;}
    case 3: {calL2_<3, NO_RADIAL>(aCnlm, rFp); return;}
    case 4: {calL2_<4, NO_RADIAL>(aCnlm, rFp); return;}
    case 5: {calL2_<5, NO_RADIAL>(aCnlm, rFp); return;}
    case 6: {calL2_<6, NO_RADIAL>(aCnlm, rFp); return;}
    case 7: {calL2_<7, NO_RADIAL>(aCnlm, rFp); return;}
    case 8: {calL2_<8, NO_RADIAL>(aCnlm, rFp); return;}
    case 9: {calL2_<9, NO_RADIAL>(aCnlm, rFp); return;}
    case 10: {calL2_<10, NO_RADIAL>(aCnlm, rFp); return;}
    case 11: {calL2_<11, NO_RADIAL>(aCnlm, rFp); return;}
    case 12: {calL2_<12, NO_RADIAL>(aCnlm, rFp); return;}
    default: {return;}
    }
}
static void calL2_(jdouble *aCnlm, jdouble *rFp, jint aLMax, jboolean aNoRadial) noexcept {
    if (aNoRadial) {
        calL2_<JNI_TRUE>(aCnlm, rFp, aLMax);
    } else {
        calL2_<JNI_FALSE>(aCnlm, rFp, aLMax);
    }
}
template <jint L3IDX, jint SUBIDX>
static inline jdouble calL3SubSub_(jdouble *aCnlm) noexcept {
    constexpr jint i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr jint i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr jint i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr jdouble coeff = L3_COEFF[L3IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3];
}
template <jint L3IDX>
static jdouble calL3Sub_(jdouble *aCnlm) noexcept {
    jdouble rFp3 = 0.0;
    constexpr jint tSize = L3_SIZE[L3IDX];
    if (tSize==0) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 0>(aCnlm);
    if (tSize==1) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 1>(aCnlm);
    if (tSize==2) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 2>(aCnlm);
    if (tSize==3) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 3>(aCnlm);
    if (tSize==4) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 4>(aCnlm);
    if (tSize==5) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 5>(aCnlm);
    if (tSize==6) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 6>(aCnlm);
    if (tSize==7) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 7>(aCnlm);
    if (tSize==8) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 8>(aCnlm);
    if (tSize==9) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 9>(aCnlm);
    if (tSize==10) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 10>(aCnlm);
    if (tSize==11) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 11>(aCnlm);
    if (tSize==12) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 12>(aCnlm);
    if (tSize==13) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 13>(aCnlm);
    if (tSize==14) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 14>(aCnlm);
    if (tSize==15) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 15>(aCnlm);
    if (tSize==16) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 16>(aCnlm);
    if (tSize==17) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 17>(aCnlm);
    if (tSize==18) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 18>(aCnlm);
    if (tSize==19) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 19>(aCnlm);
    if (tSize==20) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 20>(aCnlm);
    if (tSize==21) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 21>(aCnlm);
    if (tSize==22) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 22>(aCnlm);
    if (tSize==23) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 23>(aCnlm);
    if (tSize==24) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 24>(aCnlm);
    if (tSize==25) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 25>(aCnlm);
    if (tSize==26) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 26>(aCnlm);
    if (tSize==27) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 27>(aCnlm);
    if (tSize==28) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 28>(aCnlm);
    if (tSize==29) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 29>(aCnlm);
    if (tSize==30) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 30>(aCnlm);
    if (tSize==31) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 31>(aCnlm);
    if (tSize==32) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 32>(aCnlm);
    if (tSize==33) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 33>(aCnlm);
    if (tSize==34) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 34>(aCnlm);
    if (tSize==35) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 35>(aCnlm);
    if (tSize==36) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 36>(aCnlm);
    if (tSize==37) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 37>(aCnlm);
    if (tSize==38) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 38>(aCnlm);
    if (tSize==39) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 39>(aCnlm);
    if (tSize==40) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 40>(aCnlm);
    if (tSize==41) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 41>(aCnlm);
    if (tSize==42) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 42>(aCnlm);
    if (tSize==43) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 43>(aCnlm);
    if (tSize==44) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 44>(aCnlm);
    if (tSize==45) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 45>(aCnlm);
    if (tSize==46) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 46>(aCnlm);
    if (tSize==47) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 47>(aCnlm);
    if (tSize==48) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 48>(aCnlm);
    if (tSize==49) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 49>(aCnlm);
    if (tSize==50) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 50>(aCnlm);
    if (tSize==51) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 51>(aCnlm);
    if (tSize==52) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 52>(aCnlm);
    if (tSize==53) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 53>(aCnlm);
    if (tSize==54) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 54>(aCnlm);
    if (tSize==55) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 55>(aCnlm);
    if (tSize==56) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 56>(aCnlm);
    if (tSize==57) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 57>(aCnlm);
    if (tSize==58) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 58>(aCnlm);
    if (tSize==59) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 59>(aCnlm);
    if (tSize==60) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 60>(aCnlm);
    if (tSize==61) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 61>(aCnlm);
    if (tSize==62) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 62>(aCnlm);
    if (tSize==63) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 63>(aCnlm);
    if (tSize==64) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 64>(aCnlm);
    if (tSize==65) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 65>(aCnlm);
    if (tSize==66) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 66>(aCnlm);
    if (tSize==67) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 67>(aCnlm);
    if (tSize==68) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 68>(aCnlm);
    if (tSize==69) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 69>(aCnlm);
    if (tSize==70) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 70>(aCnlm);
    if (tSize==71) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 71>(aCnlm);
    if (tSize==72) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 72>(aCnlm);
    if (tSize==73) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 73>(aCnlm);
    if (tSize==74) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 74>(aCnlm);
    if (tSize==75) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 75>(aCnlm);
    if (tSize==76) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 76>(aCnlm);
    if (tSize==77) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 77>(aCnlm);
    if (tSize==78) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 78>(aCnlm);
    if (tSize==79) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 79>(aCnlm);
    if (tSize==80) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 80>(aCnlm);
    if (tSize==81) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 81>(aCnlm);
    if (tSize==82) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 82>(aCnlm);
    if (tSize==83) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 83>(aCnlm);
    if (tSize==84) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 84>(aCnlm);
    if (tSize==85) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 85>(aCnlm);
    if (tSize==86) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 86>(aCnlm);
    if (tSize==87) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 87>(aCnlm);
    if (tSize==88) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 88>(aCnlm);
    if (tSize==89) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 89>(aCnlm);
    if (tSize==90) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 90>(aCnlm);
    if (tSize==91) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 91>(aCnlm);
    if (tSize==92) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 92>(aCnlm);
    if (tSize==93) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 93>(aCnlm);
    if (tSize==94) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 94>(aCnlm);
    if (tSize==95) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 95>(aCnlm);
    if (tSize==96) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 96>(aCnlm);
    if (tSize==97) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 97>(aCnlm);
    if (tSize==98) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 98>(aCnlm);
    if (tSize==99) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 99>(aCnlm);
    if (tSize==100) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 100>(aCnlm);
    if (tSize==101) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 101>(aCnlm);
    if (tSize==102) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 102>(aCnlm);
    if (tSize==103) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 103>(aCnlm);
    if (tSize==104) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 104>(aCnlm);
    if (tSize==105) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 105>(aCnlm);
    if (tSize==106) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 106>(aCnlm);
    if (tSize==107) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 107>(aCnlm);
    if (tSize==108) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 108>(aCnlm);
    if (tSize==109) {return rFp3;} rFp3 += calL3SubSub_<L3IDX, 109>(aCnlm);
    return rFp3;
}
template <jint L3MAX>
static void calL3_(jdouble *aCnlm, jdouble *rFp) noexcept {
    if (L3MAX <= 1) return;
    *rFp = calL3Sub_<0>(aCnlm); ++rFp;
    *rFp = calL3Sub_<1>(aCnlm); ++rFp;
    if (L3MAX == 2) return;
    *rFp = calL3Sub_<2>(aCnlm); ++rFp;
    *rFp = calL3Sub_<3>(aCnlm); ++rFp;
    if (L3MAX == 3) return;
    *rFp = calL3Sub_<4>(aCnlm); ++rFp;
    *rFp = calL3Sub_<5>(aCnlm); ++rFp;
    *rFp = calL3Sub_<6>(aCnlm); ++rFp;
    *rFp = calL3Sub_<7>(aCnlm); ++rFp;
    *rFp = calL3Sub_<8>(aCnlm); ++rFp;
    if (L3MAX == 4) return;
    *rFp = calL3Sub_<9>(aCnlm); ++rFp;
    *rFp = calL3Sub_<10>(aCnlm); ++rFp;
    *rFp = calL3Sub_<11>(aCnlm); ++rFp;
    *rFp = calL3Sub_<12>(aCnlm); ++rFp;
    *rFp = calL3Sub_<13>(aCnlm); ++rFp;
    if (L3MAX == 5) return;
    *rFp = calL3Sub_<14>(aCnlm); ++rFp;
    *rFp = calL3Sub_<15>(aCnlm); ++rFp;
    *rFp = calL3Sub_<16>(aCnlm); ++rFp;
    *rFp = calL3Sub_<17>(aCnlm); ++rFp;
    *rFp = calL3Sub_<18>(aCnlm); ++rFp;
    *rFp = calL3Sub_<19>(aCnlm); ++rFp;
    *rFp = calL3Sub_<20>(aCnlm); ++rFp;
    *rFp = calL3Sub_<21>(aCnlm); ++rFp;
    *rFp = calL3Sub_<22>(aCnlm); ++rFp;
}
static void calL3_(jdouble *aCnlm, jdouble *rFp, jint aL3Max) noexcept {
    switch (aL3Max) {
    case 0: case 1: {calL3_<0>(aCnlm, rFp); return;}
    case 2: {calL3_<2>(aCnlm, rFp); return;}
    case 3: {calL3_<3>(aCnlm, rFp); return;}
    case 4: {calL3_<4>(aCnlm, rFp); return;}
    case 5: {calL3_<5>(aCnlm, rFp); return;}
    case 6: {calL3_<6>(aCnlm, rFp); return;}
    default: {return;}
    }
}
template <jint L4IDX, jint SUBIDX>
static inline jdouble calL4SubSub_(jdouble *aCnlm) noexcept {
    constexpr jint i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr jint i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr jint i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr jint i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr jdouble coeff = L4_COEFF[L4IDX][SUBIDX];
    return coeff * aCnlm[i1]*aCnlm[i2]*aCnlm[i3]*aCnlm[i4];
}
template <jint L4IDX>
static jdouble calL4Sub_(jdouble *aCnlm) noexcept {
    jdouble rFp4 = 0.0;
    constexpr jint tSize = L4_SIZE[L4IDX];
    if (tSize==0) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 0>(aCnlm);
    if (tSize==1) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 1>(aCnlm);
    if (tSize==2) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 2>(aCnlm);
    if (tSize==3) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 3>(aCnlm);
    if (tSize==4) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 4>(aCnlm);
    if (tSize==5) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 5>(aCnlm);
    if (tSize==6) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 6>(aCnlm);
    if (tSize==7) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 7>(aCnlm);
    if (tSize==8) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 8>(aCnlm);
    if (tSize==9) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 9>(aCnlm);
    if (tSize==10) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 10>(aCnlm);
    if (tSize==11) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 11>(aCnlm);
    if (tSize==12) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 12>(aCnlm);
    if (tSize==13) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 13>(aCnlm);
    if (tSize==14) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 14>(aCnlm);
    if (tSize==15) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 15>(aCnlm);
    if (tSize==16) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 16>(aCnlm);
    if (tSize==17) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 17>(aCnlm);
    if (tSize==18) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 18>(aCnlm);
    if (tSize==19) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 19>(aCnlm);
    if (tSize==20) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 20>(aCnlm);
    if (tSize==21) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 21>(aCnlm);
    if (tSize==22) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 22>(aCnlm);
    if (tSize==23) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 23>(aCnlm);
    if (tSize==24) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 24>(aCnlm);
    if (tSize==25) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 25>(aCnlm);
    if (tSize==26) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 26>(aCnlm);
    if (tSize==27) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 27>(aCnlm);
    if (tSize==28) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 28>(aCnlm);
    if (tSize==29) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 29>(aCnlm);
    if (tSize==30) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 30>(aCnlm);
    if (tSize==31) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 31>(aCnlm);
    if (tSize==32) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 32>(aCnlm);
    if (tSize==33) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 33>(aCnlm);
    if (tSize==34) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 34>(aCnlm);
    if (tSize==35) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 35>(aCnlm);
    if (tSize==36) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 36>(aCnlm);
    if (tSize==37) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 37>(aCnlm);
    if (tSize==38) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 38>(aCnlm);
    if (tSize==39) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 39>(aCnlm);
    if (tSize==40) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 40>(aCnlm);
    if (tSize==41) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 41>(aCnlm);
    if (tSize==42) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 42>(aCnlm);
    if (tSize==43) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 43>(aCnlm);
    if (tSize==44) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 44>(aCnlm);
    if (tSize==45) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 45>(aCnlm);
    if (tSize==46) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 46>(aCnlm);
    if (tSize==47) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 47>(aCnlm);
    if (tSize==48) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 48>(aCnlm);
    if (tSize==49) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 49>(aCnlm);
    if (tSize==50) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 50>(aCnlm);
    if (tSize==51) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 51>(aCnlm);
    if (tSize==52) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 52>(aCnlm);
    if (tSize==53) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 53>(aCnlm);
    if (tSize==54) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 54>(aCnlm);
    if (tSize==55) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 55>(aCnlm);
    if (tSize==56) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 56>(aCnlm);
    if (tSize==57) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 57>(aCnlm);
    if (tSize==58) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 58>(aCnlm);
    if (tSize==59) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 59>(aCnlm);
    if (tSize==60) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 60>(aCnlm);
    if (tSize==61) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 61>(aCnlm);
    if (tSize==62) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 62>(aCnlm);
    if (tSize==63) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 63>(aCnlm);
    if (tSize==64) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 64>(aCnlm);
    if (tSize==65) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 65>(aCnlm);
    if (tSize==66) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 66>(aCnlm);
    if (tSize==67) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 67>(aCnlm);
    if (tSize==68) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 68>(aCnlm);
    if (tSize==69) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 69>(aCnlm);
    if (tSize==70) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 70>(aCnlm);
    if (tSize==71) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 71>(aCnlm);
    if (tSize==72) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 72>(aCnlm);
    if (tSize==73) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 73>(aCnlm);
    if (tSize==74) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 74>(aCnlm);
    if (tSize==75) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 75>(aCnlm);
    if (tSize==76) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 76>(aCnlm);
    if (tSize==77) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 77>(aCnlm);
    if (tSize==78) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 78>(aCnlm);
    if (tSize==79) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 79>(aCnlm);
    if (tSize==80) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 80>(aCnlm);
    if (tSize==81) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 81>(aCnlm);
    if (tSize==82) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 82>(aCnlm);
    if (tSize==83) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 83>(aCnlm);
    if (tSize==84) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 84>(aCnlm);
    if (tSize==85) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 85>(aCnlm);
    if (tSize==86) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 86>(aCnlm);
    if (tSize==87) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 87>(aCnlm);
    if (tSize==88) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 88>(aCnlm);
    if (tSize==89) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 89>(aCnlm);
    if (tSize==90) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 90>(aCnlm);
    if (tSize==91) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 91>(aCnlm);
    if (tSize==92) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 92>(aCnlm);
    if (tSize==93) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 93>(aCnlm);
    if (tSize==94) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 94>(aCnlm);
    if (tSize==95) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 95>(aCnlm);
    if (tSize==96) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 96>(aCnlm);
    if (tSize==97) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 97>(aCnlm);
    if (tSize==98) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 98>(aCnlm);
    if (tSize==99) {return rFp4;} rFp4 += calL4SubSub_<L4IDX, 99>(aCnlm);
    return rFp4;
}
template <jint L4MAX>
static void calL4_(jdouble *aCnlm, jdouble *rFp) noexcept {
    if (L4MAX < 1) return;
    *rFp = calL4Sub_<0>(aCnlm); ++rFp;
    if (L4MAX == 1) return;
    *rFp = calL4Sub_<1>(aCnlm); ++rFp;
    *rFp = calL4Sub_<2>(aCnlm); ++rFp;
    if (L4MAX == 2) return;
    *rFp = calL4Sub_<3>(aCnlm); ++rFp;
    *rFp = calL4Sub_<4>(aCnlm); ++rFp;
    *rFp = calL4Sub_<5>(aCnlm); ++rFp;
    *rFp = calL4Sub_<6>(aCnlm); ++rFp;
    *rFp = calL4Sub_<7>(aCnlm); ++rFp;
    *rFp = calL4Sub_<8>(aCnlm); ++rFp;
}
static void calL4_(jdouble *aCnlm, jdouble *rFp, jint aL4Max) noexcept {
    switch (aL4Max) {
    case 0: {calL4_<0>(aCnlm, rFp); return;}
    case 1: {calL4_<1>(aCnlm, rFp); return;}
    case 2: {calL4_<2>(aCnlm, rFp); return;}
    case 3: {calL4_<3>(aCnlm, rFp); return;}
    default: {return;}
    }
}

template <jint L>
static inline void calGradL2Sub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad) noexcept {
    constexpr jint tStart = L*L;
    constexpr jint tLen = L+L+1;
    constexpr jint tEnd = tStart+tLen;
    constexpr jdouble tCoeff = 2.0 * PI4/(jdouble)tLen;
    const jdouble tMul = tCoeff * aNNGrad[L-1];
    for (jint i = tStart; i < tEnd; ++i) {
        rGradCnlm[i] += tMul * aCnlm[i];
    }
}
template <jint LMAX, jboolean NO_RADIAL>
static void calGradL2_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad) noexcept {
    // l = 0
    jdouble *tNNGrad = aNNGrad;
    if (!NO_RADIAL) {
        rGradCnlm[0] += (PI4+PI4) * tNNGrad[0] * aCnlm[0];
        ++tNNGrad;
    }
    if (LMAX == 0) return;
    calGradL2Sub_<1>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 1) return;
    calGradL2Sub_<2>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 2) return;
    calGradL2Sub_<3>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 3) return;
    calGradL2Sub_<4>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 4) return;
    calGradL2Sub_<5>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 5) return;
    calGradL2Sub_<6>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 6) return;
    calGradL2Sub_<7>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 7) return;
    calGradL2Sub_<8>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 8) return;
    calGradL2Sub_<9>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 9) return;
    calGradL2Sub_<10>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 10) return;
    calGradL2Sub_<11>(aCnlm, rGradCnlm, tNNGrad); if (LMAX == 11) return;
    calGradL2Sub_<12>(aCnlm, rGradCnlm, tNNGrad);
}
template <jboolean NO_RADIAL>
static void calGradL2_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {calGradL2_<0, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 1: {calGradL2_<1, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 2: {calGradL2_<2, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 3: {calGradL2_<3, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 4: {calGradL2_<4, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 5: {calGradL2_<5, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 6: {calGradL2_<6, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 7: {calGradL2_<7, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 8: {calGradL2_<8, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 9: {calGradL2_<9, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 10: {calGradL2_<10, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 11: {calGradL2_<11, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 12: {calGradL2_<12, NO_RADIAL>(aCnlm, rGradCnlm, aNNGrad); return;}
    default: {return;}
    }
}
static void calGradL2_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad, jint aLMax, jboolean aNoRadial) noexcept {
    if (aNoRadial) {
        calGradL2_<JNI_TRUE>(aCnlm, rGradCnlm, aNNGrad, aLMax);
    } else {
        calGradL2_<JNI_FALSE>(aCnlm, rGradCnlm, aNNGrad, aLMax);
    }
}
template <jint L3IDX, jint SUBIDX>
static inline void calGradL3SubSub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr jint i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr jint i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr jdouble coeff = L3_COEFF[L3IDX][SUBIDX];
    const jdouble tMul = coeff * aSubNNGrad;
    const jdouble tCnlm1 = aCnlm[i1];
    const jdouble tCnlm2 = aCnlm[i2];
    const jdouble tCnlm3 = aCnlm[i3];
    rGradCnlm[i1] += tMul * tCnlm2*tCnlm3;
    rGradCnlm[i2] += tMul * tCnlm1*tCnlm3;
    rGradCnlm[i3] += tMul * tCnlm1*tCnlm2;
}
template <jint L3IDX>
static void calGradL3Sub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint tSize = L3_SIZE[L3IDX];
    if (tSize==0) {return;} calGradL3SubSub_<L3IDX, 0>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==1) {return;} calGradL3SubSub_<L3IDX, 1>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==2) {return;} calGradL3SubSub_<L3IDX, 2>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==3) {return;} calGradL3SubSub_<L3IDX, 3>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==4) {return;} calGradL3SubSub_<L3IDX, 4>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==5) {return;} calGradL3SubSub_<L3IDX, 5>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==6) {return;} calGradL3SubSub_<L3IDX, 6>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==7) {return;} calGradL3SubSub_<L3IDX, 7>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==8) {return;} calGradL3SubSub_<L3IDX, 8>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==9) {return;} calGradL3SubSub_<L3IDX, 9>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==10) {return;} calGradL3SubSub_<L3IDX, 10>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==11) {return;} calGradL3SubSub_<L3IDX, 11>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==12) {return;} calGradL3SubSub_<L3IDX, 12>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==13) {return;} calGradL3SubSub_<L3IDX, 13>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==14) {return;} calGradL3SubSub_<L3IDX, 14>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==15) {return;} calGradL3SubSub_<L3IDX, 15>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==16) {return;} calGradL3SubSub_<L3IDX, 16>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==17) {return;} calGradL3SubSub_<L3IDX, 17>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==18) {return;} calGradL3SubSub_<L3IDX, 18>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==19) {return;} calGradL3SubSub_<L3IDX, 19>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==20) {return;} calGradL3SubSub_<L3IDX, 20>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==21) {return;} calGradL3SubSub_<L3IDX, 21>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==22) {return;} calGradL3SubSub_<L3IDX, 22>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==23) {return;} calGradL3SubSub_<L3IDX, 23>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==24) {return;} calGradL3SubSub_<L3IDX, 24>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==25) {return;} calGradL3SubSub_<L3IDX, 25>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==26) {return;} calGradL3SubSub_<L3IDX, 26>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==27) {return;} calGradL3SubSub_<L3IDX, 27>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==28) {return;} calGradL3SubSub_<L3IDX, 28>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==29) {return;} calGradL3SubSub_<L3IDX, 29>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==30) {return;} calGradL3SubSub_<L3IDX, 30>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==31) {return;} calGradL3SubSub_<L3IDX, 31>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==32) {return;} calGradL3SubSub_<L3IDX, 32>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==33) {return;} calGradL3SubSub_<L3IDX, 33>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==34) {return;} calGradL3SubSub_<L3IDX, 34>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==35) {return;} calGradL3SubSub_<L3IDX, 35>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==36) {return;} calGradL3SubSub_<L3IDX, 36>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==37) {return;} calGradL3SubSub_<L3IDX, 37>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==38) {return;} calGradL3SubSub_<L3IDX, 38>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==39) {return;} calGradL3SubSub_<L3IDX, 39>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==40) {return;} calGradL3SubSub_<L3IDX, 40>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==41) {return;} calGradL3SubSub_<L3IDX, 41>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==42) {return;} calGradL3SubSub_<L3IDX, 42>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==43) {return;} calGradL3SubSub_<L3IDX, 43>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==44) {return;} calGradL3SubSub_<L3IDX, 44>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==45) {return;} calGradL3SubSub_<L3IDX, 45>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==46) {return;} calGradL3SubSub_<L3IDX, 46>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==47) {return;} calGradL3SubSub_<L3IDX, 47>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==48) {return;} calGradL3SubSub_<L3IDX, 48>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==49) {return;} calGradL3SubSub_<L3IDX, 49>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==50) {return;} calGradL3SubSub_<L3IDX, 50>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==51) {return;} calGradL3SubSub_<L3IDX, 51>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==52) {return;} calGradL3SubSub_<L3IDX, 52>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==53) {return;} calGradL3SubSub_<L3IDX, 53>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==54) {return;} calGradL3SubSub_<L3IDX, 54>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==55) {return;} calGradL3SubSub_<L3IDX, 55>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==56) {return;} calGradL3SubSub_<L3IDX, 56>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==57) {return;} calGradL3SubSub_<L3IDX, 57>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==58) {return;} calGradL3SubSub_<L3IDX, 58>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==59) {return;} calGradL3SubSub_<L3IDX, 59>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==60) {return;} calGradL3SubSub_<L3IDX, 60>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==61) {return;} calGradL3SubSub_<L3IDX, 61>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==62) {return;} calGradL3SubSub_<L3IDX, 62>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==63) {return;} calGradL3SubSub_<L3IDX, 63>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==64) {return;} calGradL3SubSub_<L3IDX, 64>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==65) {return;} calGradL3SubSub_<L3IDX, 65>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==66) {return;} calGradL3SubSub_<L3IDX, 66>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==67) {return;} calGradL3SubSub_<L3IDX, 67>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==68) {return;} calGradL3SubSub_<L3IDX, 68>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==69) {return;} calGradL3SubSub_<L3IDX, 69>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==70) {return;} calGradL3SubSub_<L3IDX, 70>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==71) {return;} calGradL3SubSub_<L3IDX, 71>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==72) {return;} calGradL3SubSub_<L3IDX, 72>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==73) {return;} calGradL3SubSub_<L3IDX, 73>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==74) {return;} calGradL3SubSub_<L3IDX, 74>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==75) {return;} calGradL3SubSub_<L3IDX, 75>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==76) {return;} calGradL3SubSub_<L3IDX, 76>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==77) {return;} calGradL3SubSub_<L3IDX, 77>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==78) {return;} calGradL3SubSub_<L3IDX, 78>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==79) {return;} calGradL3SubSub_<L3IDX, 79>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==80) {return;} calGradL3SubSub_<L3IDX, 80>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==81) {return;} calGradL3SubSub_<L3IDX, 81>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==82) {return;} calGradL3SubSub_<L3IDX, 82>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==83) {return;} calGradL3SubSub_<L3IDX, 83>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==84) {return;} calGradL3SubSub_<L3IDX, 84>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==85) {return;} calGradL3SubSub_<L3IDX, 85>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==86) {return;} calGradL3SubSub_<L3IDX, 86>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==87) {return;} calGradL3SubSub_<L3IDX, 87>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==88) {return;} calGradL3SubSub_<L3IDX, 88>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==89) {return;} calGradL3SubSub_<L3IDX, 89>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==90) {return;} calGradL3SubSub_<L3IDX, 90>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==91) {return;} calGradL3SubSub_<L3IDX, 91>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==92) {return;} calGradL3SubSub_<L3IDX, 92>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==93) {return;} calGradL3SubSub_<L3IDX, 93>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==94) {return;} calGradL3SubSub_<L3IDX, 94>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==95) {return;} calGradL3SubSub_<L3IDX, 95>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==96) {return;} calGradL3SubSub_<L3IDX, 96>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==97) {return;} calGradL3SubSub_<L3IDX, 97>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==98) {return;} calGradL3SubSub_<L3IDX, 98>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==99) {return;} calGradL3SubSub_<L3IDX, 99>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==100) {return;} calGradL3SubSub_<L3IDX, 100>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==101) {return;} calGradL3SubSub_<L3IDX, 101>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==102) {return;} calGradL3SubSub_<L3IDX, 102>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==103) {return;} calGradL3SubSub_<L3IDX, 103>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==104) {return;} calGradL3SubSub_<L3IDX, 104>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==105) {return;} calGradL3SubSub_<L3IDX, 105>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==106) {return;} calGradL3SubSub_<L3IDX, 106>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==107) {return;} calGradL3SubSub_<L3IDX, 107>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==108) {return;} calGradL3SubSub_<L3IDX, 108>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==109) {return;} calGradL3SubSub_<L3IDX, 109>(aCnlm, rGradCnlm, aSubNNGrad);
}
template <jint L3MAX>
static void calGradL3_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad) noexcept {
    if (L3MAX <= 1) return;
    calGradL3Sub_<0>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<1>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 2) return;
    calGradL3Sub_<2>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<3>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 3) return;
    calGradL3Sub_<4>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<5>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<6>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<7>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<8>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 4) return;
    calGradL3Sub_<9>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<10>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<11>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<12>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<13>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 5) return;
    calGradL3Sub_<14>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<15>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<16>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<17>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<18>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<19>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<20>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<21>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL3Sub_<22>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
}
static void calGradL3_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad, jint aL3Max) noexcept {
    switch (aL3Max) {
    case 0: case 1: {calGradL3_<0>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 2: {calGradL3_<2>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 3: {calGradL3_<3>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 4: {calGradL3_<4>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 5: {calGradL3_<5>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 6: {calGradL3_<6>(aCnlm, rGradCnlm, aNNGrad); return;}
    default: {return;}
    }
}
template <jint L4IDX, jint SUBIDX>
static inline void calGradL4SubSub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr jint i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr jint i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr jint i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr jdouble coeff = L4_COEFF[L4IDX][SUBIDX];
    const jdouble tMul = coeff * aSubNNGrad;
    const jdouble tCnlm1 = aCnlm[i1];
    const jdouble tCnlm2 = aCnlm[i2];
    const jdouble tCnlm3 = aCnlm[i3];
    const jdouble tCnlm4 = aCnlm[i4];
    rGradCnlm[i1] += tMul * tCnlm2*tCnlm3*tCnlm4;
    rGradCnlm[i2] += tMul * tCnlm1*tCnlm3*tCnlm4;
    rGradCnlm[i3] += tMul * tCnlm1*tCnlm2*tCnlm4;
    rGradCnlm[i4] += tMul * tCnlm1*tCnlm2*tCnlm3;
}
template <jint L4IDX>
static void calGradL4Sub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint tSize = L4_SIZE[L4IDX];
    if (tSize==0) {return;} calGradL4SubSub_<L4IDX, 0>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==1) {return;} calGradL4SubSub_<L4IDX, 1>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==2) {return;} calGradL4SubSub_<L4IDX, 2>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==3) {return;} calGradL4SubSub_<L4IDX, 3>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==4) {return;} calGradL4SubSub_<L4IDX, 4>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==5) {return;} calGradL4SubSub_<L4IDX, 5>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==6) {return;} calGradL4SubSub_<L4IDX, 6>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==7) {return;} calGradL4SubSub_<L4IDX, 7>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==8) {return;} calGradL4SubSub_<L4IDX, 8>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==9) {return;} calGradL4SubSub_<L4IDX, 9>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==10) {return;} calGradL4SubSub_<L4IDX, 10>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==11) {return;} calGradL4SubSub_<L4IDX, 11>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==12) {return;} calGradL4SubSub_<L4IDX, 12>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==13) {return;} calGradL4SubSub_<L4IDX, 13>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==14) {return;} calGradL4SubSub_<L4IDX, 14>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==15) {return;} calGradL4SubSub_<L4IDX, 15>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==16) {return;} calGradL4SubSub_<L4IDX, 16>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==17) {return;} calGradL4SubSub_<L4IDX, 17>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==18) {return;} calGradL4SubSub_<L4IDX, 18>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==19) {return;} calGradL4SubSub_<L4IDX, 19>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==20) {return;} calGradL4SubSub_<L4IDX, 20>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==21) {return;} calGradL4SubSub_<L4IDX, 21>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==22) {return;} calGradL4SubSub_<L4IDX, 22>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==23) {return;} calGradL4SubSub_<L4IDX, 23>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==24) {return;} calGradL4SubSub_<L4IDX, 24>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==25) {return;} calGradL4SubSub_<L4IDX, 25>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==26) {return;} calGradL4SubSub_<L4IDX, 26>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==27) {return;} calGradL4SubSub_<L4IDX, 27>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==28) {return;} calGradL4SubSub_<L4IDX, 28>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==29) {return;} calGradL4SubSub_<L4IDX, 29>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==30) {return;} calGradL4SubSub_<L4IDX, 30>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==31) {return;} calGradL4SubSub_<L4IDX, 31>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==32) {return;} calGradL4SubSub_<L4IDX, 32>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==33) {return;} calGradL4SubSub_<L4IDX, 33>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==34) {return;} calGradL4SubSub_<L4IDX, 34>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==35) {return;} calGradL4SubSub_<L4IDX, 35>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==36) {return;} calGradL4SubSub_<L4IDX, 36>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==37) {return;} calGradL4SubSub_<L4IDX, 37>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==38) {return;} calGradL4SubSub_<L4IDX, 38>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==39) {return;} calGradL4SubSub_<L4IDX, 39>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==40) {return;} calGradL4SubSub_<L4IDX, 40>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==41) {return;} calGradL4SubSub_<L4IDX, 41>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==42) {return;} calGradL4SubSub_<L4IDX, 42>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==43) {return;} calGradL4SubSub_<L4IDX, 43>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==44) {return;} calGradL4SubSub_<L4IDX, 44>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==45) {return;} calGradL4SubSub_<L4IDX, 45>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==46) {return;} calGradL4SubSub_<L4IDX, 46>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==47) {return;} calGradL4SubSub_<L4IDX, 47>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==48) {return;} calGradL4SubSub_<L4IDX, 48>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==49) {return;} calGradL4SubSub_<L4IDX, 49>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==50) {return;} calGradL4SubSub_<L4IDX, 50>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==51) {return;} calGradL4SubSub_<L4IDX, 51>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==52) {return;} calGradL4SubSub_<L4IDX, 52>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==53) {return;} calGradL4SubSub_<L4IDX, 53>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==54) {return;} calGradL4SubSub_<L4IDX, 54>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==55) {return;} calGradL4SubSub_<L4IDX, 55>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==56) {return;} calGradL4SubSub_<L4IDX, 56>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==57) {return;} calGradL4SubSub_<L4IDX, 57>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==58) {return;} calGradL4SubSub_<L4IDX, 58>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==59) {return;} calGradL4SubSub_<L4IDX, 59>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==60) {return;} calGradL4SubSub_<L4IDX, 60>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==61) {return;} calGradL4SubSub_<L4IDX, 61>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==62) {return;} calGradL4SubSub_<L4IDX, 62>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==63) {return;} calGradL4SubSub_<L4IDX, 63>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==64) {return;} calGradL4SubSub_<L4IDX, 64>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==65) {return;} calGradL4SubSub_<L4IDX, 65>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==66) {return;} calGradL4SubSub_<L4IDX, 66>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==67) {return;} calGradL4SubSub_<L4IDX, 67>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==68) {return;} calGradL4SubSub_<L4IDX, 68>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==69) {return;} calGradL4SubSub_<L4IDX, 69>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==70) {return;} calGradL4SubSub_<L4IDX, 70>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==71) {return;} calGradL4SubSub_<L4IDX, 71>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==72) {return;} calGradL4SubSub_<L4IDX, 72>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==73) {return;} calGradL4SubSub_<L4IDX, 73>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==74) {return;} calGradL4SubSub_<L4IDX, 74>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==75) {return;} calGradL4SubSub_<L4IDX, 75>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==76) {return;} calGradL4SubSub_<L4IDX, 76>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==77) {return;} calGradL4SubSub_<L4IDX, 77>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==78) {return;} calGradL4SubSub_<L4IDX, 78>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==79) {return;} calGradL4SubSub_<L4IDX, 79>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==80) {return;} calGradL4SubSub_<L4IDX, 80>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==81) {return;} calGradL4SubSub_<L4IDX, 81>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==82) {return;} calGradL4SubSub_<L4IDX, 82>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==83) {return;} calGradL4SubSub_<L4IDX, 83>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==84) {return;} calGradL4SubSub_<L4IDX, 84>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==85) {return;} calGradL4SubSub_<L4IDX, 85>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==86) {return;} calGradL4SubSub_<L4IDX, 86>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==87) {return;} calGradL4SubSub_<L4IDX, 87>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==88) {return;} calGradL4SubSub_<L4IDX, 88>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==89) {return;} calGradL4SubSub_<L4IDX, 89>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==90) {return;} calGradL4SubSub_<L4IDX, 90>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==91) {return;} calGradL4SubSub_<L4IDX, 91>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==92) {return;} calGradL4SubSub_<L4IDX, 92>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==93) {return;} calGradL4SubSub_<L4IDX, 93>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==94) {return;} calGradL4SubSub_<L4IDX, 94>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==95) {return;} calGradL4SubSub_<L4IDX, 95>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==96) {return;} calGradL4SubSub_<L4IDX, 96>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==97) {return;} calGradL4SubSub_<L4IDX, 97>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==98) {return;} calGradL4SubSub_<L4IDX, 98>(aCnlm, rGradCnlm, aSubNNGrad);
    if (tSize==99) {return;} calGradL4SubSub_<L4IDX, 99>(aCnlm, rGradCnlm, aSubNNGrad);
}
template <jint L4MAX>
static void calGradL4_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad) noexcept {
    if (L4MAX < 1) return;
    calGradL4Sub_<0>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    if (L4MAX == 1) return;
    calGradL4Sub_<1>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL4Sub_<2>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    if (L4MAX == 2) return;
    calGradL4Sub_<3>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL4Sub_<4>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL4Sub_<5>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL4Sub_<6>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL4Sub_<7>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
    calGradL4Sub_<8>(aCnlm, rGradCnlm, *aNNGrad); ++aNNGrad;
}
static void calGradL4_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aNNGrad, jint aL4Max) noexcept {
    switch (aL4Max) {
    case 0: {calGradL4_<0>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 1: {calGradL4_<1>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 2: {calGradL4_<2>(aCnlm, rGradCnlm, aNNGrad); return;}
    case 3: {calGradL4_<3>(aCnlm, rGradCnlm, aNNGrad); return;}
    default: {return;}
    }
}

template <jint L>
static inline void calGradNNGradL2Sub_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad) noexcept {
    constexpr jint tStart = L*L;
    constexpr jint tLen = L+L+1;
    const jdouble rDot = dot<tLen>(aCnlm+tStart, aGradNNGradCnlm+tStart);
    rGradNNGrad[L-1] = (2.0 * PI4/(jdouble)tLen) * rDot;
}
template <jint LMAX, jboolean NO_RADIAL>
static void calGradNNGradL2_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad) noexcept {
    // l = 0
    jdouble *tGradNNGrad = rGradNNGrad;
    if (!NO_RADIAL) {
        tGradNNGrad[0] += (PI4+PI4) * aCnlm[0] * aGradNNGradCnlm[0];
        ++tGradNNGrad;
    }
    if (LMAX == 0) return;
    calGradNNGradL2Sub_<1>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 1) return;
    calGradNNGradL2Sub_<2>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 2) return;
    calGradNNGradL2Sub_<3>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 3) return;
    calGradNNGradL2Sub_<4>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 4) return;
    calGradNNGradL2Sub_<5>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 5) return;
    calGradNNGradL2Sub_<6>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 6) return;
    calGradNNGradL2Sub_<7>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 7) return;
    calGradNNGradL2Sub_<8>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 8) return;
    calGradNNGradL2Sub_<9>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 9) return;
    calGradNNGradL2Sub_<10>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 10) return;
    calGradNNGradL2Sub_<11>(aCnlm, aGradNNGradCnlm, tGradNNGrad); if (LMAX == 11) return;
    calGradNNGradL2Sub_<12>(aCnlm, aGradNNGradCnlm, tGradNNGrad);
}
template <jboolean NO_RADIAL>
static void calGradNNGradL2_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad, jint aLMax) noexcept {
    switch (aLMax) {
    case 0: {calGradNNGradL2_<0, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 1: {calGradNNGradL2_<1, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 2: {calGradNNGradL2_<2, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 3: {calGradNNGradL2_<3, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 4: {calGradNNGradL2_<4, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 5: {calGradNNGradL2_<5, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 6: {calGradNNGradL2_<6, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 7: {calGradNNGradL2_<7, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 8: {calGradNNGradL2_<8, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 9: {calGradNNGradL2_<9, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 10: {calGradNNGradL2_<10, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 11: {calGradNNGradL2_<11, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 12: {calGradNNGradL2_<12, NO_RADIAL>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    default: {return;}
    }
}
static void calGradNNGradL2_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad, jint aLMax, jboolean aNoRadial) noexcept {
    if (aNoRadial) {
        calGradNNGradL2_<JNI_TRUE>(aCnlm, aGradNNGradCnlm, rGradNNGrad, aLMax);
    } else {
        calGradNNGradL2_<JNI_FALSE>(aCnlm, aGradNNGradCnlm, rGradNNGrad, aLMax);
    }
}
template <jint L3IDX, jint SUBIDX>
static inline jdouble calGradNNGradL3SubSub_(jdouble *aCnlm, jdouble *aGradNNGradCnlm) noexcept {
    jdouble rGGFp3 = 0.0;
    constexpr jint i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr jint i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr jint i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr jdouble coeff = L3_COEFF[L3IDX][SUBIDX];
    const jdouble tCnlm1 = aCnlm[i1];
    const jdouble tCnlm2 = aCnlm[i2];
    const jdouble tCnlm3 = aCnlm[i3];
    rGGFp3 += aGradNNGradCnlm[i1]*tCnlm2*tCnlm3;
    rGGFp3 += tCnlm1*aGradNNGradCnlm[i2]*tCnlm3;
    rGGFp3 += tCnlm1*tCnlm2*aGradNNGradCnlm[i3];
    return coeff*rGGFp3;
}
template <jint L3IDX>
static jdouble calGradNNGradL3Sub_(jdouble *aCnlm, jdouble *aGradNNGradCnlm) noexcept {
    jdouble rGGFp3 = 0.0;
    constexpr jint tSize = L3_SIZE[L3IDX];
    if (tSize==0) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 0>(aCnlm, aGradNNGradCnlm);
    if (tSize==1) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 1>(aCnlm, aGradNNGradCnlm);
    if (tSize==2) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 2>(aCnlm, aGradNNGradCnlm);
    if (tSize==3) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 3>(aCnlm, aGradNNGradCnlm);
    if (tSize==4) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 4>(aCnlm, aGradNNGradCnlm);
    if (tSize==5) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 5>(aCnlm, aGradNNGradCnlm);
    if (tSize==6) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 6>(aCnlm, aGradNNGradCnlm);
    if (tSize==7) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 7>(aCnlm, aGradNNGradCnlm);
    if (tSize==8) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 8>(aCnlm, aGradNNGradCnlm);
    if (tSize==9) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 9>(aCnlm, aGradNNGradCnlm);
    if (tSize==10) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 10>(aCnlm, aGradNNGradCnlm);
    if (tSize==11) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 11>(aCnlm, aGradNNGradCnlm);
    if (tSize==12) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 12>(aCnlm, aGradNNGradCnlm);
    if (tSize==13) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 13>(aCnlm, aGradNNGradCnlm);
    if (tSize==14) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 14>(aCnlm, aGradNNGradCnlm);
    if (tSize==15) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 15>(aCnlm, aGradNNGradCnlm);
    if (tSize==16) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 16>(aCnlm, aGradNNGradCnlm);
    if (tSize==17) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 17>(aCnlm, aGradNNGradCnlm);
    if (tSize==18) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 18>(aCnlm, aGradNNGradCnlm);
    if (tSize==19) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 19>(aCnlm, aGradNNGradCnlm);
    if (tSize==20) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 20>(aCnlm, aGradNNGradCnlm);
    if (tSize==21) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 21>(aCnlm, aGradNNGradCnlm);
    if (tSize==22) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 22>(aCnlm, aGradNNGradCnlm);
    if (tSize==23) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 23>(aCnlm, aGradNNGradCnlm);
    if (tSize==24) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 24>(aCnlm, aGradNNGradCnlm);
    if (tSize==25) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 25>(aCnlm, aGradNNGradCnlm);
    if (tSize==26) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 26>(aCnlm, aGradNNGradCnlm);
    if (tSize==27) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 27>(aCnlm, aGradNNGradCnlm);
    if (tSize==28) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 28>(aCnlm, aGradNNGradCnlm);
    if (tSize==29) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 29>(aCnlm, aGradNNGradCnlm);
    if (tSize==30) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 30>(aCnlm, aGradNNGradCnlm);
    if (tSize==31) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 31>(aCnlm, aGradNNGradCnlm);
    if (tSize==32) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 32>(aCnlm, aGradNNGradCnlm);
    if (tSize==33) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 33>(aCnlm, aGradNNGradCnlm);
    if (tSize==34) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 34>(aCnlm, aGradNNGradCnlm);
    if (tSize==35) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 35>(aCnlm, aGradNNGradCnlm);
    if (tSize==36) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 36>(aCnlm, aGradNNGradCnlm);
    if (tSize==37) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 37>(aCnlm, aGradNNGradCnlm);
    if (tSize==38) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 38>(aCnlm, aGradNNGradCnlm);
    if (tSize==39) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 39>(aCnlm, aGradNNGradCnlm);
    if (tSize==40) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 40>(aCnlm, aGradNNGradCnlm);
    if (tSize==41) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 41>(aCnlm, aGradNNGradCnlm);
    if (tSize==42) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 42>(aCnlm, aGradNNGradCnlm);
    if (tSize==43) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 43>(aCnlm, aGradNNGradCnlm);
    if (tSize==44) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 44>(aCnlm, aGradNNGradCnlm);
    if (tSize==45) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 45>(aCnlm, aGradNNGradCnlm);
    if (tSize==46) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 46>(aCnlm, aGradNNGradCnlm);
    if (tSize==47) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 47>(aCnlm, aGradNNGradCnlm);
    if (tSize==48) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 48>(aCnlm, aGradNNGradCnlm);
    if (tSize==49) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 49>(aCnlm, aGradNNGradCnlm);
    if (tSize==50) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 50>(aCnlm, aGradNNGradCnlm);
    if (tSize==51) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 51>(aCnlm, aGradNNGradCnlm);
    if (tSize==52) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 52>(aCnlm, aGradNNGradCnlm);
    if (tSize==53) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 53>(aCnlm, aGradNNGradCnlm);
    if (tSize==54) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 54>(aCnlm, aGradNNGradCnlm);
    if (tSize==55) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 55>(aCnlm, aGradNNGradCnlm);
    if (tSize==56) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 56>(aCnlm, aGradNNGradCnlm);
    if (tSize==57) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 57>(aCnlm, aGradNNGradCnlm);
    if (tSize==58) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 58>(aCnlm, aGradNNGradCnlm);
    if (tSize==59) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 59>(aCnlm, aGradNNGradCnlm);
    if (tSize==60) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 60>(aCnlm, aGradNNGradCnlm);
    if (tSize==61) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 61>(aCnlm, aGradNNGradCnlm);
    if (tSize==62) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 62>(aCnlm, aGradNNGradCnlm);
    if (tSize==63) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 63>(aCnlm, aGradNNGradCnlm);
    if (tSize==64) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 64>(aCnlm, aGradNNGradCnlm);
    if (tSize==65) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 65>(aCnlm, aGradNNGradCnlm);
    if (tSize==66) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 66>(aCnlm, aGradNNGradCnlm);
    if (tSize==67) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 67>(aCnlm, aGradNNGradCnlm);
    if (tSize==68) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 68>(aCnlm, aGradNNGradCnlm);
    if (tSize==69) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 69>(aCnlm, aGradNNGradCnlm);
    if (tSize==70) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 70>(aCnlm, aGradNNGradCnlm);
    if (tSize==71) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 71>(aCnlm, aGradNNGradCnlm);
    if (tSize==72) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 72>(aCnlm, aGradNNGradCnlm);
    if (tSize==73) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 73>(aCnlm, aGradNNGradCnlm);
    if (tSize==74) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 74>(aCnlm, aGradNNGradCnlm);
    if (tSize==75) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 75>(aCnlm, aGradNNGradCnlm);
    if (tSize==76) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 76>(aCnlm, aGradNNGradCnlm);
    if (tSize==77) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 77>(aCnlm, aGradNNGradCnlm);
    if (tSize==78) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 78>(aCnlm, aGradNNGradCnlm);
    if (tSize==79) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 79>(aCnlm, aGradNNGradCnlm);
    if (tSize==80) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 80>(aCnlm, aGradNNGradCnlm);
    if (tSize==81) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 81>(aCnlm, aGradNNGradCnlm);
    if (tSize==82) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 82>(aCnlm, aGradNNGradCnlm);
    if (tSize==83) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 83>(aCnlm, aGradNNGradCnlm);
    if (tSize==84) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 84>(aCnlm, aGradNNGradCnlm);
    if (tSize==85) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 85>(aCnlm, aGradNNGradCnlm);
    if (tSize==86) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 86>(aCnlm, aGradNNGradCnlm);
    if (tSize==87) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 87>(aCnlm, aGradNNGradCnlm);
    if (tSize==88) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 88>(aCnlm, aGradNNGradCnlm);
    if (tSize==89) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 89>(aCnlm, aGradNNGradCnlm);
    if (tSize==90) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 90>(aCnlm, aGradNNGradCnlm);
    if (tSize==91) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 91>(aCnlm, aGradNNGradCnlm);
    if (tSize==92) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 92>(aCnlm, aGradNNGradCnlm);
    if (tSize==93) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 93>(aCnlm, aGradNNGradCnlm);
    if (tSize==94) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 94>(aCnlm, aGradNNGradCnlm);
    if (tSize==95) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 95>(aCnlm, aGradNNGradCnlm);
    if (tSize==96) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 96>(aCnlm, aGradNNGradCnlm);
    if (tSize==97) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 97>(aCnlm, aGradNNGradCnlm);
    if (tSize==98) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 98>(aCnlm, aGradNNGradCnlm);
    if (tSize==99) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 99>(aCnlm, aGradNNGradCnlm);
    if (tSize==100) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 100>(aCnlm, aGradNNGradCnlm);
    if (tSize==101) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 101>(aCnlm, aGradNNGradCnlm);
    if (tSize==102) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 102>(aCnlm, aGradNNGradCnlm);
    if (tSize==103) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 103>(aCnlm, aGradNNGradCnlm);
    if (tSize==104) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 104>(aCnlm, aGradNNGradCnlm);
    if (tSize==105) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 105>(aCnlm, aGradNNGradCnlm);
    if (tSize==106) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 106>(aCnlm, aGradNNGradCnlm);
    if (tSize==107) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 107>(aCnlm, aGradNNGradCnlm);
    if (tSize==108) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 108>(aCnlm, aGradNNGradCnlm);
    if (tSize==109) {return rGGFp3;} rGGFp3 += calGradNNGradL3SubSub_<L3IDX, 109>(aCnlm, aGradNNGradCnlm);
    return rGGFp3;
}
template <jint L3MAX>
static void calGradNNGradL3_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad) noexcept {
    if (L3MAX <= 1) return;
    *rGradNNGrad += calGradNNGradL3Sub_<0>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<1>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    if (L3MAX == 2) return;
    *rGradNNGrad += calGradNNGradL3Sub_<2>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<3>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    if (L3MAX == 3) return;
    *rGradNNGrad += calGradNNGradL3Sub_<4>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<5>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<6>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<7>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<8>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    if (L3MAX == 4) return;
    *rGradNNGrad += calGradNNGradL3Sub_<9>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<10>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<11>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<12>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<13>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    if (L3MAX == 5) return;
    *rGradNNGrad += calGradNNGradL3Sub_<14>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<15>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<16>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<17>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<18>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<19>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<20>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<21>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL3Sub_<22>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
}
static void calGradNNGradL3_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad, jint aL3Max) noexcept {
    switch (aL3Max) {
    case 0: case 1: {calGradNNGradL3_<0>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 2: {calGradNNGradL3_<2>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 3: {calGradNNGradL3_<3>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 4: {calGradNNGradL3_<4>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 5: {calGradNNGradL3_<5>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 6: {calGradNNGradL3_<6>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    default: {return;}
    }
}
template <jint L4IDX, jint SUBIDX>
static inline jdouble calGradNNGradL4SubSub_(jdouble *aCnlm, jdouble *aGradNNGradCnlm) noexcept {
    jdouble rGGFp4 = 0.0;
    constexpr jint i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr jint i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr jint i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr jint i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr jdouble coeff = L4_COEFF[L4IDX][SUBIDX];
    const jdouble tCnlm1 = aCnlm[i1];
    const jdouble tCnlm2 = aCnlm[i2];
    const jdouble tCnlm3 = aCnlm[i3];
    const jdouble tCnlm4 = aCnlm[i4];
    rGGFp4 += aGradNNGradCnlm[i1]*tCnlm2*tCnlm3*tCnlm4;
    rGGFp4 += tCnlm1*aGradNNGradCnlm[i2]*tCnlm3*tCnlm4;
    rGGFp4 += tCnlm1*tCnlm2*aGradNNGradCnlm[i3]*tCnlm4;
    rGGFp4 += tCnlm1*tCnlm2*tCnlm3*aGradNNGradCnlm[i4];
    return coeff*rGGFp4;
}
template <jint L4IDX>
static jdouble calGradNNGradL4Sub_(jdouble *aCnlm, jdouble *aGradNNGradCnlm) noexcept {
    jdouble rGGFp4 = 0.0;
    constexpr jint tSize = L4_SIZE[L4IDX];
    if (tSize==0) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 0>(aCnlm, aGradNNGradCnlm);
    if (tSize==1) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 1>(aCnlm, aGradNNGradCnlm);
    if (tSize==2) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 2>(aCnlm, aGradNNGradCnlm);
    if (tSize==3) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 3>(aCnlm, aGradNNGradCnlm);
    if (tSize==4) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 4>(aCnlm, aGradNNGradCnlm);
    if (tSize==5) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 5>(aCnlm, aGradNNGradCnlm);
    if (tSize==6) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 6>(aCnlm, aGradNNGradCnlm);
    if (tSize==7) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 7>(aCnlm, aGradNNGradCnlm);
    if (tSize==8) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 8>(aCnlm, aGradNNGradCnlm);
    if (tSize==9) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 9>(aCnlm, aGradNNGradCnlm);
    if (tSize==10) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 10>(aCnlm, aGradNNGradCnlm);
    if (tSize==11) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 11>(aCnlm, aGradNNGradCnlm);
    if (tSize==12) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 12>(aCnlm, aGradNNGradCnlm);
    if (tSize==13) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 13>(aCnlm, aGradNNGradCnlm);
    if (tSize==14) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 14>(aCnlm, aGradNNGradCnlm);
    if (tSize==15) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 15>(aCnlm, aGradNNGradCnlm);
    if (tSize==16) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 16>(aCnlm, aGradNNGradCnlm);
    if (tSize==17) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 17>(aCnlm, aGradNNGradCnlm);
    if (tSize==18) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 18>(aCnlm, aGradNNGradCnlm);
    if (tSize==19) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 19>(aCnlm, aGradNNGradCnlm);
    if (tSize==20) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 20>(aCnlm, aGradNNGradCnlm);
    if (tSize==21) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 21>(aCnlm, aGradNNGradCnlm);
    if (tSize==22) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 22>(aCnlm, aGradNNGradCnlm);
    if (tSize==23) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 23>(aCnlm, aGradNNGradCnlm);
    if (tSize==24) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 24>(aCnlm, aGradNNGradCnlm);
    if (tSize==25) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 25>(aCnlm, aGradNNGradCnlm);
    if (tSize==26) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 26>(aCnlm, aGradNNGradCnlm);
    if (tSize==27) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 27>(aCnlm, aGradNNGradCnlm);
    if (tSize==28) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 28>(aCnlm, aGradNNGradCnlm);
    if (tSize==29) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 29>(aCnlm, aGradNNGradCnlm);
    if (tSize==30) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 30>(aCnlm, aGradNNGradCnlm);
    if (tSize==31) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 31>(aCnlm, aGradNNGradCnlm);
    if (tSize==32) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 32>(aCnlm, aGradNNGradCnlm);
    if (tSize==33) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 33>(aCnlm, aGradNNGradCnlm);
    if (tSize==34) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 34>(aCnlm, aGradNNGradCnlm);
    if (tSize==35) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 35>(aCnlm, aGradNNGradCnlm);
    if (tSize==36) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 36>(aCnlm, aGradNNGradCnlm);
    if (tSize==37) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 37>(aCnlm, aGradNNGradCnlm);
    if (tSize==38) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 38>(aCnlm, aGradNNGradCnlm);
    if (tSize==39) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 39>(aCnlm, aGradNNGradCnlm);
    if (tSize==40) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 40>(aCnlm, aGradNNGradCnlm);
    if (tSize==41) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 41>(aCnlm, aGradNNGradCnlm);
    if (tSize==42) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 42>(aCnlm, aGradNNGradCnlm);
    if (tSize==43) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 43>(aCnlm, aGradNNGradCnlm);
    if (tSize==44) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 44>(aCnlm, aGradNNGradCnlm);
    if (tSize==45) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 45>(aCnlm, aGradNNGradCnlm);
    if (tSize==46) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 46>(aCnlm, aGradNNGradCnlm);
    if (tSize==47) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 47>(aCnlm, aGradNNGradCnlm);
    if (tSize==48) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 48>(aCnlm, aGradNNGradCnlm);
    if (tSize==49) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 49>(aCnlm, aGradNNGradCnlm);
    if (tSize==50) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 50>(aCnlm, aGradNNGradCnlm);
    if (tSize==51) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 51>(aCnlm, aGradNNGradCnlm);
    if (tSize==52) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 52>(aCnlm, aGradNNGradCnlm);
    if (tSize==53) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 53>(aCnlm, aGradNNGradCnlm);
    if (tSize==54) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 54>(aCnlm, aGradNNGradCnlm);
    if (tSize==55) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 55>(aCnlm, aGradNNGradCnlm);
    if (tSize==56) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 56>(aCnlm, aGradNNGradCnlm);
    if (tSize==57) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 57>(aCnlm, aGradNNGradCnlm);
    if (tSize==58) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 58>(aCnlm, aGradNNGradCnlm);
    if (tSize==59) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 59>(aCnlm, aGradNNGradCnlm);
    if (tSize==60) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 60>(aCnlm, aGradNNGradCnlm);
    if (tSize==61) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 61>(aCnlm, aGradNNGradCnlm);
    if (tSize==62) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 62>(aCnlm, aGradNNGradCnlm);
    if (tSize==63) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 63>(aCnlm, aGradNNGradCnlm);
    if (tSize==64) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 64>(aCnlm, aGradNNGradCnlm);
    if (tSize==65) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 65>(aCnlm, aGradNNGradCnlm);
    if (tSize==66) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 66>(aCnlm, aGradNNGradCnlm);
    if (tSize==67) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 67>(aCnlm, aGradNNGradCnlm);
    if (tSize==68) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 68>(aCnlm, aGradNNGradCnlm);
    if (tSize==69) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 69>(aCnlm, aGradNNGradCnlm);
    if (tSize==70) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 70>(aCnlm, aGradNNGradCnlm);
    if (tSize==71) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 71>(aCnlm, aGradNNGradCnlm);
    if (tSize==72) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 72>(aCnlm, aGradNNGradCnlm);
    if (tSize==73) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 73>(aCnlm, aGradNNGradCnlm);
    if (tSize==74) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 74>(aCnlm, aGradNNGradCnlm);
    if (tSize==75) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 75>(aCnlm, aGradNNGradCnlm);
    if (tSize==76) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 76>(aCnlm, aGradNNGradCnlm);
    if (tSize==77) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 77>(aCnlm, aGradNNGradCnlm);
    if (tSize==78) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 78>(aCnlm, aGradNNGradCnlm);
    if (tSize==79) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 79>(aCnlm, aGradNNGradCnlm);
    if (tSize==80) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 80>(aCnlm, aGradNNGradCnlm);
    if (tSize==81) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 81>(aCnlm, aGradNNGradCnlm);
    if (tSize==82) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 82>(aCnlm, aGradNNGradCnlm);
    if (tSize==83) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 83>(aCnlm, aGradNNGradCnlm);
    if (tSize==84) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 84>(aCnlm, aGradNNGradCnlm);
    if (tSize==85) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 85>(aCnlm, aGradNNGradCnlm);
    if (tSize==86) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 86>(aCnlm, aGradNNGradCnlm);
    if (tSize==87) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 87>(aCnlm, aGradNNGradCnlm);
    if (tSize==88) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 88>(aCnlm, aGradNNGradCnlm);
    if (tSize==89) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 89>(aCnlm, aGradNNGradCnlm);
    if (tSize==90) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 90>(aCnlm, aGradNNGradCnlm);
    if (tSize==91) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 91>(aCnlm, aGradNNGradCnlm);
    if (tSize==92) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 92>(aCnlm, aGradNNGradCnlm);
    if (tSize==93) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 93>(aCnlm, aGradNNGradCnlm);
    if (tSize==94) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 94>(aCnlm, aGradNNGradCnlm);
    if (tSize==95) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 95>(aCnlm, aGradNNGradCnlm);
    if (tSize==96) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 96>(aCnlm, aGradNNGradCnlm);
    if (tSize==97) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 97>(aCnlm, aGradNNGradCnlm);
    if (tSize==98) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 98>(aCnlm, aGradNNGradCnlm);
    if (tSize==99) {return rGGFp4;} rGGFp4 += calGradNNGradL4SubSub_<L4IDX, 99>(aCnlm, aGradNNGradCnlm);
    return rGGFp4;
}
template <jint L4MAX>
static void calGradNNGradL4_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad) noexcept {
    if (L4MAX < 1) return;
    *rGradNNGrad += calGradNNGradL4Sub_<0>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    if (L4MAX == 1) return;
    *rGradNNGrad += calGradNNGradL4Sub_<1>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL4Sub_<2>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    if (L4MAX == 2) return;
    *rGradNNGrad += calGradNNGradL4Sub_<3>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL4Sub_<4>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL4Sub_<5>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL4Sub_<6>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL4Sub_<7>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
    *rGradNNGrad += calGradNNGradL4Sub_<8>(aCnlm, aGradNNGradCnlm); ++rGradNNGrad;
}
static void calGradNNGradL4_(jdouble *aCnlm, jdouble *aGradNNGradCnlm, jdouble *rGradNNGrad, jint aL4Max) noexcept {
    switch (aL4Max) {
    case 0: {calGradNNGradL4_<0>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 1: {calGradNNGradL4_<1>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 2: {calGradNNGradL4_<2>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    case 3: {calGradNNGradL4_<3>(aCnlm, aGradNNGradCnlm, rGradNNGrad); return;}
    default: {return;}
    }
}

static void calGradCnlmL2_(jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble *aNNGrad, jint aLMax, jboolean aNoRadial) noexcept {
    calGradL2_(aGradNNGradCnlm, rGradCnlm, aNNGrad, aLMax, aNoRadial);
}
template <jint L3IDX, jint SUBIDX>
static inline void calGradCnlmL3SubSub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint i1 = L3_INDEX[L3IDX][SUBIDX][0];
    constexpr jint i2 = L3_INDEX[L3IDX][SUBIDX][1];
    constexpr jint i3 = L3_INDEX[L3IDX][SUBIDX][2];
    constexpr jdouble coeff = L3_COEFF[L3IDX][SUBIDX];
    const jdouble tMul = coeff * aSubNNGrad;
    const jdouble tCnlm1 = aCnlm[i1], tGGCnlm1 = aGradNNGradCnlm[i1];
    const jdouble tCnlm2 = aCnlm[i2], tGGCnlm2 = aGradNNGradCnlm[i2];
    const jdouble tCnlm3 = aCnlm[i3], tGGCnlm3 = aGradNNGradCnlm[i3];
    rGradCnlm[i1] += tMul * (tGGCnlm2*tCnlm3 + tCnlm2*tGGCnlm3);
    rGradCnlm[i2] += tMul * (tGGCnlm1*tCnlm3 + tCnlm1*tGGCnlm3);
    rGradCnlm[i3] += tMul * (tGGCnlm1*tCnlm2 + tCnlm1*tGGCnlm2);
}
template <jint L3IDX>
static void calGradCnlmL3Sub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint tSize = L3_SIZE[L3IDX];
    if (tSize==0) {return;} calGradCnlmL3SubSub_<L3IDX, 0>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==1) {return;} calGradCnlmL3SubSub_<L3IDX, 1>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==2) {return;} calGradCnlmL3SubSub_<L3IDX, 2>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==3) {return;} calGradCnlmL3SubSub_<L3IDX, 3>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==4) {return;} calGradCnlmL3SubSub_<L3IDX, 4>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==5) {return;} calGradCnlmL3SubSub_<L3IDX, 5>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==6) {return;} calGradCnlmL3SubSub_<L3IDX, 6>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==7) {return;} calGradCnlmL3SubSub_<L3IDX, 7>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==8) {return;} calGradCnlmL3SubSub_<L3IDX, 8>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==9) {return;} calGradCnlmL3SubSub_<L3IDX, 9>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==10) {return;} calGradCnlmL3SubSub_<L3IDX, 10>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==11) {return;} calGradCnlmL3SubSub_<L3IDX, 11>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==12) {return;} calGradCnlmL3SubSub_<L3IDX, 12>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==13) {return;} calGradCnlmL3SubSub_<L3IDX, 13>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==14) {return;} calGradCnlmL3SubSub_<L3IDX, 14>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==15) {return;} calGradCnlmL3SubSub_<L3IDX, 15>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==16) {return;} calGradCnlmL3SubSub_<L3IDX, 16>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==17) {return;} calGradCnlmL3SubSub_<L3IDX, 17>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==18) {return;} calGradCnlmL3SubSub_<L3IDX, 18>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==19) {return;} calGradCnlmL3SubSub_<L3IDX, 19>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==20) {return;} calGradCnlmL3SubSub_<L3IDX, 20>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==21) {return;} calGradCnlmL3SubSub_<L3IDX, 21>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==22) {return;} calGradCnlmL3SubSub_<L3IDX, 22>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==23) {return;} calGradCnlmL3SubSub_<L3IDX, 23>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==24) {return;} calGradCnlmL3SubSub_<L3IDX, 24>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==25) {return;} calGradCnlmL3SubSub_<L3IDX, 25>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==26) {return;} calGradCnlmL3SubSub_<L3IDX, 26>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==27) {return;} calGradCnlmL3SubSub_<L3IDX, 27>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==28) {return;} calGradCnlmL3SubSub_<L3IDX, 28>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==29) {return;} calGradCnlmL3SubSub_<L3IDX, 29>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==30) {return;} calGradCnlmL3SubSub_<L3IDX, 30>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==31) {return;} calGradCnlmL3SubSub_<L3IDX, 31>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==32) {return;} calGradCnlmL3SubSub_<L3IDX, 32>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==33) {return;} calGradCnlmL3SubSub_<L3IDX, 33>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==34) {return;} calGradCnlmL3SubSub_<L3IDX, 34>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==35) {return;} calGradCnlmL3SubSub_<L3IDX, 35>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==36) {return;} calGradCnlmL3SubSub_<L3IDX, 36>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==37) {return;} calGradCnlmL3SubSub_<L3IDX, 37>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==38) {return;} calGradCnlmL3SubSub_<L3IDX, 38>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==39) {return;} calGradCnlmL3SubSub_<L3IDX, 39>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==40) {return;} calGradCnlmL3SubSub_<L3IDX, 40>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==41) {return;} calGradCnlmL3SubSub_<L3IDX, 41>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==42) {return;} calGradCnlmL3SubSub_<L3IDX, 42>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==43) {return;} calGradCnlmL3SubSub_<L3IDX, 43>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==44) {return;} calGradCnlmL3SubSub_<L3IDX, 44>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==45) {return;} calGradCnlmL3SubSub_<L3IDX, 45>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==46) {return;} calGradCnlmL3SubSub_<L3IDX, 46>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==47) {return;} calGradCnlmL3SubSub_<L3IDX, 47>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==48) {return;} calGradCnlmL3SubSub_<L3IDX, 48>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==49) {return;} calGradCnlmL3SubSub_<L3IDX, 49>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==50) {return;} calGradCnlmL3SubSub_<L3IDX, 50>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==51) {return;} calGradCnlmL3SubSub_<L3IDX, 51>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==52) {return;} calGradCnlmL3SubSub_<L3IDX, 52>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==53) {return;} calGradCnlmL3SubSub_<L3IDX, 53>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==54) {return;} calGradCnlmL3SubSub_<L3IDX, 54>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==55) {return;} calGradCnlmL3SubSub_<L3IDX, 55>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==56) {return;} calGradCnlmL3SubSub_<L3IDX, 56>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==57) {return;} calGradCnlmL3SubSub_<L3IDX, 57>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==58) {return;} calGradCnlmL3SubSub_<L3IDX, 58>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==59) {return;} calGradCnlmL3SubSub_<L3IDX, 59>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==60) {return;} calGradCnlmL3SubSub_<L3IDX, 60>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==61) {return;} calGradCnlmL3SubSub_<L3IDX, 61>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==62) {return;} calGradCnlmL3SubSub_<L3IDX, 62>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==63) {return;} calGradCnlmL3SubSub_<L3IDX, 63>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==64) {return;} calGradCnlmL3SubSub_<L3IDX, 64>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==65) {return;} calGradCnlmL3SubSub_<L3IDX, 65>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==66) {return;} calGradCnlmL3SubSub_<L3IDX, 66>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==67) {return;} calGradCnlmL3SubSub_<L3IDX, 67>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==68) {return;} calGradCnlmL3SubSub_<L3IDX, 68>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==69) {return;} calGradCnlmL3SubSub_<L3IDX, 69>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==70) {return;} calGradCnlmL3SubSub_<L3IDX, 70>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==71) {return;} calGradCnlmL3SubSub_<L3IDX, 71>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==72) {return;} calGradCnlmL3SubSub_<L3IDX, 72>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==73) {return;} calGradCnlmL3SubSub_<L3IDX, 73>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==74) {return;} calGradCnlmL3SubSub_<L3IDX, 74>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==75) {return;} calGradCnlmL3SubSub_<L3IDX, 75>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==76) {return;} calGradCnlmL3SubSub_<L3IDX, 76>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==77) {return;} calGradCnlmL3SubSub_<L3IDX, 77>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==78) {return;} calGradCnlmL3SubSub_<L3IDX, 78>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==79) {return;} calGradCnlmL3SubSub_<L3IDX, 79>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==80) {return;} calGradCnlmL3SubSub_<L3IDX, 80>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==81) {return;} calGradCnlmL3SubSub_<L3IDX, 81>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==82) {return;} calGradCnlmL3SubSub_<L3IDX, 82>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==83) {return;} calGradCnlmL3SubSub_<L3IDX, 83>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==84) {return;} calGradCnlmL3SubSub_<L3IDX, 84>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==85) {return;} calGradCnlmL3SubSub_<L3IDX, 85>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==86) {return;} calGradCnlmL3SubSub_<L3IDX, 86>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==87) {return;} calGradCnlmL3SubSub_<L3IDX, 87>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==88) {return;} calGradCnlmL3SubSub_<L3IDX, 88>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==89) {return;} calGradCnlmL3SubSub_<L3IDX, 89>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==90) {return;} calGradCnlmL3SubSub_<L3IDX, 90>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==91) {return;} calGradCnlmL3SubSub_<L3IDX, 91>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==92) {return;} calGradCnlmL3SubSub_<L3IDX, 92>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==93) {return;} calGradCnlmL3SubSub_<L3IDX, 93>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==94) {return;} calGradCnlmL3SubSub_<L3IDX, 94>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==95) {return;} calGradCnlmL3SubSub_<L3IDX, 95>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==96) {return;} calGradCnlmL3SubSub_<L3IDX, 96>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==97) {return;} calGradCnlmL3SubSub_<L3IDX, 97>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==98) {return;} calGradCnlmL3SubSub_<L3IDX, 98>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==99) {return;} calGradCnlmL3SubSub_<L3IDX, 99>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==100) {return;} calGradCnlmL3SubSub_<L3IDX, 100>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==101) {return;} calGradCnlmL3SubSub_<L3IDX, 101>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==102) {return;} calGradCnlmL3SubSub_<L3IDX, 102>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==103) {return;} calGradCnlmL3SubSub_<L3IDX, 103>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==104) {return;} calGradCnlmL3SubSub_<L3IDX, 104>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==105) {return;} calGradCnlmL3SubSub_<L3IDX, 105>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==106) {return;} calGradCnlmL3SubSub_<L3IDX, 106>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==107) {return;} calGradCnlmL3SubSub_<L3IDX, 107>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==108) {return;} calGradCnlmL3SubSub_<L3IDX, 108>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==109) {return;} calGradCnlmL3SubSub_<L3IDX, 109>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
}
template <jint L3MAX>
static void calGradCnlmL3_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble *aNNGrad) noexcept {
    if (L3MAX <= 1) return;
    calGradCnlmL3Sub_<0>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<1>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 2) return;
    calGradCnlmL3Sub_<2>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<3>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 3) return;
    calGradCnlmL3Sub_<4>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<5>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<6>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<7>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<8>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 4) return;
    calGradCnlmL3Sub_<9>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<10>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<11>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<12>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<13>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    if (L3MAX == 5) return;
    calGradCnlmL3Sub_<14>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<15>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<16>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<17>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<18>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<19>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<20>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<21>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL3Sub_<22>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
}
static void calGradCnlmL3_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble *aNNGrad, jint aL3Max) noexcept {
    switch (aL3Max) {
    case 0: case 1: {calGradCnlmL3_<0>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 2: {calGradCnlmL3_<2>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 3: {calGradCnlmL3_<3>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 4: {calGradCnlmL3_<4>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 5: {calGradCnlmL3_<5>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 6: {calGradCnlmL3_<6>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    default: {return;}
    }
}

template <jint L4IDX, jint SUBIDX>
static inline void calGradCnlmL4SubSub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint i1 = L4_INDEX[L4IDX][SUBIDX][0];
    constexpr jint i2 = L4_INDEX[L4IDX][SUBIDX][1];
    constexpr jint i3 = L4_INDEX[L4IDX][SUBIDX][2];
    constexpr jint i4 = L4_INDEX[L4IDX][SUBIDX][3];
    constexpr jdouble coeff = L4_COEFF[L4IDX][SUBIDX];
    const jdouble tMul = coeff * aSubNNGrad;
    const jdouble tCnlm1 = aCnlm[i1], tGGCnlm1 = aGradNNGradCnlm[i1];
    const jdouble tCnlm2 = aCnlm[i2], tGGCnlm2 = aGradNNGradCnlm[i2];
    const jdouble tCnlm3 = aCnlm[i3], tGGCnlm3 = aGradNNGradCnlm[i3];
    const jdouble tCnlm4 = aCnlm[i4], tGGCnlm4 = aGradNNGradCnlm[i4];
    rGradCnlm[i1] += tMul * (tGGCnlm2*tCnlm3*tCnlm4 + tCnlm2*tGGCnlm3*tCnlm4 + tCnlm2*tCnlm3*tGGCnlm4);
    rGradCnlm[i2] += tMul * (tGGCnlm1*tCnlm3*tCnlm4 + tCnlm1*tGGCnlm3*tCnlm4 + tCnlm1*tCnlm3*tGGCnlm4);
    rGradCnlm[i3] += tMul * (tGGCnlm1*tCnlm2*tCnlm4 + tCnlm1*tGGCnlm2*tCnlm4 + tCnlm1*tCnlm2*tGGCnlm4);
    rGradCnlm[i4] += tMul * (tGGCnlm1*tCnlm2*tCnlm3 + tCnlm1*tGGCnlm2*tCnlm3 + tCnlm1*tCnlm2*tGGCnlm3);
}
template <jint L4IDX>
static void calGradCnlmL4Sub_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble aSubNNGrad) noexcept {
    constexpr jint tSize = L4_SIZE[L4IDX];
    if (tSize==0) {return;} calGradCnlmL4SubSub_<L4IDX, 0>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==1) {return;} calGradCnlmL4SubSub_<L4IDX, 1>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==2) {return;} calGradCnlmL4SubSub_<L4IDX, 2>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==3) {return;} calGradCnlmL4SubSub_<L4IDX, 3>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==4) {return;} calGradCnlmL4SubSub_<L4IDX, 4>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==5) {return;} calGradCnlmL4SubSub_<L4IDX, 5>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==6) {return;} calGradCnlmL4SubSub_<L4IDX, 6>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==7) {return;} calGradCnlmL4SubSub_<L4IDX, 7>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==8) {return;} calGradCnlmL4SubSub_<L4IDX, 8>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==9) {return;} calGradCnlmL4SubSub_<L4IDX, 9>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==10) {return;} calGradCnlmL4SubSub_<L4IDX, 10>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==11) {return;} calGradCnlmL4SubSub_<L4IDX, 11>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==12) {return;} calGradCnlmL4SubSub_<L4IDX, 12>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==13) {return;} calGradCnlmL4SubSub_<L4IDX, 13>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==14) {return;} calGradCnlmL4SubSub_<L4IDX, 14>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==15) {return;} calGradCnlmL4SubSub_<L4IDX, 15>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==16) {return;} calGradCnlmL4SubSub_<L4IDX, 16>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==17) {return;} calGradCnlmL4SubSub_<L4IDX, 17>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==18) {return;} calGradCnlmL4SubSub_<L4IDX, 18>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==19) {return;} calGradCnlmL4SubSub_<L4IDX, 19>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==20) {return;} calGradCnlmL4SubSub_<L4IDX, 20>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==21) {return;} calGradCnlmL4SubSub_<L4IDX, 21>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==22) {return;} calGradCnlmL4SubSub_<L4IDX, 22>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==23) {return;} calGradCnlmL4SubSub_<L4IDX, 23>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==24) {return;} calGradCnlmL4SubSub_<L4IDX, 24>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==25) {return;} calGradCnlmL4SubSub_<L4IDX, 25>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==26) {return;} calGradCnlmL4SubSub_<L4IDX, 26>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==27) {return;} calGradCnlmL4SubSub_<L4IDX, 27>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==28) {return;} calGradCnlmL4SubSub_<L4IDX, 28>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==29) {return;} calGradCnlmL4SubSub_<L4IDX, 29>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==30) {return;} calGradCnlmL4SubSub_<L4IDX, 30>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==31) {return;} calGradCnlmL4SubSub_<L4IDX, 31>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==32) {return;} calGradCnlmL4SubSub_<L4IDX, 32>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==33) {return;} calGradCnlmL4SubSub_<L4IDX, 33>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==34) {return;} calGradCnlmL4SubSub_<L4IDX, 34>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==35) {return;} calGradCnlmL4SubSub_<L4IDX, 35>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==36) {return;} calGradCnlmL4SubSub_<L4IDX, 36>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==37) {return;} calGradCnlmL4SubSub_<L4IDX, 37>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==38) {return;} calGradCnlmL4SubSub_<L4IDX, 38>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==39) {return;} calGradCnlmL4SubSub_<L4IDX, 39>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==40) {return;} calGradCnlmL4SubSub_<L4IDX, 40>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==41) {return;} calGradCnlmL4SubSub_<L4IDX, 41>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==42) {return;} calGradCnlmL4SubSub_<L4IDX, 42>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==43) {return;} calGradCnlmL4SubSub_<L4IDX, 43>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==44) {return;} calGradCnlmL4SubSub_<L4IDX, 44>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==45) {return;} calGradCnlmL4SubSub_<L4IDX, 45>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==46) {return;} calGradCnlmL4SubSub_<L4IDX, 46>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==47) {return;} calGradCnlmL4SubSub_<L4IDX, 47>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==48) {return;} calGradCnlmL4SubSub_<L4IDX, 48>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==49) {return;} calGradCnlmL4SubSub_<L4IDX, 49>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==50) {return;} calGradCnlmL4SubSub_<L4IDX, 50>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==51) {return;} calGradCnlmL4SubSub_<L4IDX, 51>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==52) {return;} calGradCnlmL4SubSub_<L4IDX, 52>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==53) {return;} calGradCnlmL4SubSub_<L4IDX, 53>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==54) {return;} calGradCnlmL4SubSub_<L4IDX, 54>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==55) {return;} calGradCnlmL4SubSub_<L4IDX, 55>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==56) {return;} calGradCnlmL4SubSub_<L4IDX, 56>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==57) {return;} calGradCnlmL4SubSub_<L4IDX, 57>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==58) {return;} calGradCnlmL4SubSub_<L4IDX, 58>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==59) {return;} calGradCnlmL4SubSub_<L4IDX, 59>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==60) {return;} calGradCnlmL4SubSub_<L4IDX, 60>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==61) {return;} calGradCnlmL4SubSub_<L4IDX, 61>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==62) {return;} calGradCnlmL4SubSub_<L4IDX, 62>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==63) {return;} calGradCnlmL4SubSub_<L4IDX, 63>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==64) {return;} calGradCnlmL4SubSub_<L4IDX, 64>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==65) {return;} calGradCnlmL4SubSub_<L4IDX, 65>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==66) {return;} calGradCnlmL4SubSub_<L4IDX, 66>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==67) {return;} calGradCnlmL4SubSub_<L4IDX, 67>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==68) {return;} calGradCnlmL4SubSub_<L4IDX, 68>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==69) {return;} calGradCnlmL4SubSub_<L4IDX, 69>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==70) {return;} calGradCnlmL4SubSub_<L4IDX, 70>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==71) {return;} calGradCnlmL4SubSub_<L4IDX, 71>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==72) {return;} calGradCnlmL4SubSub_<L4IDX, 72>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==73) {return;} calGradCnlmL4SubSub_<L4IDX, 73>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==74) {return;} calGradCnlmL4SubSub_<L4IDX, 74>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==75) {return;} calGradCnlmL4SubSub_<L4IDX, 75>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==76) {return;} calGradCnlmL4SubSub_<L4IDX, 76>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==77) {return;} calGradCnlmL4SubSub_<L4IDX, 77>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==78) {return;} calGradCnlmL4SubSub_<L4IDX, 78>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==79) {return;} calGradCnlmL4SubSub_<L4IDX, 79>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==80) {return;} calGradCnlmL4SubSub_<L4IDX, 80>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==81) {return;} calGradCnlmL4SubSub_<L4IDX, 81>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==82) {return;} calGradCnlmL4SubSub_<L4IDX, 82>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==83) {return;} calGradCnlmL4SubSub_<L4IDX, 83>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==84) {return;} calGradCnlmL4SubSub_<L4IDX, 84>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==85) {return;} calGradCnlmL4SubSub_<L4IDX, 85>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==86) {return;} calGradCnlmL4SubSub_<L4IDX, 86>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==87) {return;} calGradCnlmL4SubSub_<L4IDX, 87>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==88) {return;} calGradCnlmL4SubSub_<L4IDX, 88>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==89) {return;} calGradCnlmL4SubSub_<L4IDX, 89>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==90) {return;} calGradCnlmL4SubSub_<L4IDX, 90>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==91) {return;} calGradCnlmL4SubSub_<L4IDX, 91>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==92) {return;} calGradCnlmL4SubSub_<L4IDX, 92>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==93) {return;} calGradCnlmL4SubSub_<L4IDX, 93>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==94) {return;} calGradCnlmL4SubSub_<L4IDX, 94>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==95) {return;} calGradCnlmL4SubSub_<L4IDX, 95>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==96) {return;} calGradCnlmL4SubSub_<L4IDX, 96>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==97) {return;} calGradCnlmL4SubSub_<L4IDX, 97>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==98) {return;} calGradCnlmL4SubSub_<L4IDX, 98>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
    if (tSize==99) {return;} calGradCnlmL4SubSub_<L4IDX, 99>(aCnlm, rGradCnlm, aGradNNGradCnlm, aSubNNGrad);
}
template <jint L4MAX>
static void calGradCnlmL4_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble *aNNGrad) noexcept {
    if (L4MAX < 1) return;
    calGradCnlmL4Sub_<0>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    if (L4MAX == 1) return;
    calGradCnlmL4Sub_<1>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL4Sub_<2>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    if (L4MAX == 2) return;
    calGradCnlmL4Sub_<3>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL4Sub_<4>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL4Sub_<5>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL4Sub_<6>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL4Sub_<7>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
    calGradCnlmL4Sub_<8>(aCnlm, rGradCnlm, aGradNNGradCnlm, *aNNGrad); ++aNNGrad;
}
static void calGradCnlmL4_(jdouble *aCnlm, jdouble *rGradCnlm, jdouble *aGradNNGradCnlm, jdouble *aNNGrad, jint aL4Max) noexcept {
    switch (aL4Max) {
    case 0: {calGradCnlmL4_<0>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 1: {calGradCnlmL4_<1>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 2: {calGradCnlmL4_<2>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    case 3: {calGradCnlmL4_<3>(aCnlm, rGradCnlm, aGradNNGradCnlm, aNNGrad); return;}
    default: {return;}
    }
}

}

#endif //BASIS_SPHERICAL_UTIL_H